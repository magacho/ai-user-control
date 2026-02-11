package com.bemobi.aicontrol.integration.claude;

import com.bemobi.aicontrol.integration.ToolApiClient;
import com.bemobi.aicontrol.integration.claude.dto.ClaudeMember;
import com.bemobi.aicontrol.integration.claude.dto.ClaudeMembersResponse;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for integrating with Claude Code (Anthropic Admin API).
 *
 * This client fetches organization users from the Anthropic Admin API.
 * Requires an Admin API key (sk-ant-admin-...) with appropriate permissions.
 *
 * API Documentation: https://docs.anthropic.com/en/api/administration-api
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.claude", name = "enabled", havingValue = "true")
public class ClaudeApiClient implements ToolApiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiClient.class);

    private final WebClient webClient;
    private final ClaudeApiProperties properties;
    private final ObjectMapper objectMapper;

    public ClaudeApiClient(WebClient.Builder webClientBuilder,
                          ClaudeApiProperties properties,
                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        // Only create WebClient if properties are configured
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl() : "https://api.anthropic.com";
        String token = properties.getToken() != null ? properties.getToken() : "";

        this.webClient = webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("X-API-Key", token)
            .defaultHeader("Anthropic-Version", "2023-06-01")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public String getToolName() {
        return "claude-code";
    }

    @Override
    public String getDisplayName() {
        return "Claude Code";
    }

    @Override
    public List<UserData> fetchUsers() throws ApiClientException {
        log.info("Fetching users from Claude Code Admin API");

        try {
            ClaudeMembersResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/organizations/users")
                    .queryParam("limit", 100)
                    .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(ClaudeMembersResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                    .doBeforeRetry(signal ->
                        log.warn("Rate limit hit, retrying request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null || response.data() == null) {
                throw new ApiClientException("Empty response from Claude Admin API");
            }

            log.info("Successfully fetched {} users from Claude Code", response.data().size());

            return response.data().stream()
                .map(this::mapToUserData)
                .collect(Collectors.toList());

        } catch (WebClientException e) {
            log.error("Error fetching users from Claude Code: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch users from Claude Code", e);
        }
    }

    @Override
    public ConnectionTestResult testConnection() {
        try {
            fetchUsers();
            return ConnectionTestResult.success(getToolName());
        } catch (Exception e) {
            return ConnectionTestResult.failure(getToolName(), e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private UserData mapToUserData(ClaudeMember member) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("role", member.role());
        metrics.put("joined_at", member.joinedAt());
        metrics.put("member_id", member.id());

        return new UserData(
                member.email() != null ? member.email().toLowerCase() : null,
                member.name(),
                member.status(),
                member.lastActiveAt(),
                metrics,
                toRawJson(member)
        );
    }

    private String toRawJson(Object dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize rawJson", e);
            return null;
        }
    }

    private Mono<? extends Throwable> handle4xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("4xx error from Claude API: {} - {}", response.statusCode(), body);
                return Mono.error(new ApiClientException("Client error: " + body));
            });
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("5xx error from Claude API: {} - {}", response.statusCode(), body);
                return Mono.error(new ApiClientException("Server error: " + body));
            });
    }
}
