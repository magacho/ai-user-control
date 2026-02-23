package com.bemobi.aicontrol.integration.claude;

import com.bemobi.aicontrol.integration.ToolApiClient;
import com.bemobi.aicontrol.integration.claude.dto.ClaudeCodeUsageReport;
import com.bemobi.aicontrol.integration.claude.dto.ClaudeMember;
import com.bemobi.aicontrol.integration.claude.dto.ClaudeMembersResponse;
import com.bemobi.aicontrol.integration.claude.dto.CostReportResponse;
import com.bemobi.aicontrol.integration.claude.dto.UsageReportResponse;
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
import java.time.LocalDate;
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

    /**
     * Fetches usage report from Claude Admin API.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param bucketWidth aggregation bucket width (e.g., "1h", "1d")
     * @return usage report response with aggregated token usage data
     * @throws ApiClientException if API call fails
     */
    public UsageReportResponse fetchUsageReport(LocalDate startDate, LocalDate endDate, String bucketWidth)
            throws ApiClientException {
        log.info("Fetching usage report from Claude API: {} to {}, bucket: {}",
                startDate, endDate, bucketWidth);

        try {
            UsageReportResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/organizations/usage_report/messages")
                    .queryParam("start_date", startDate.toString())
                    .queryParam("end_date", endDate.toString())
                    .queryParam("bucket_width", bucketWidth)
                    .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(UsageReportResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                    .doBeforeRetry(signal ->
                        log.warn("Rate limit hit, retrying request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null) {
                throw new ApiClientException("Empty response from Claude usage report API");
            }

            log.info("Successfully fetched usage report with {} data points",
                    response.data() != null ? response.data().size() : 0);

            return response;

        } catch (WebClientException e) {
            log.error("Error fetching usage report from Claude: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch usage report from Claude", e);
        }
    }

    /**
     * Fetches cost report from Claude Admin API.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return cost report response with spending data in cents
     * @throws ApiClientException if API call fails
     */
    public CostReportResponse fetchCostReport(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("Fetching cost report from Claude API: {} to {}", startDate, endDate);

        try {
            CostReportResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/organizations/cost_report")
                    .queryParam("start_date", startDate.toString())
                    .queryParam("end_date", endDate.toString())
                    .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(CostReportResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                    .doBeforeRetry(signal ->
                        log.warn("Rate limit hit, retrying request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null) {
                throw new ApiClientException("Empty response from Claude cost report API");
            }

            log.info("Successfully fetched cost report with {} data points",
                    response.data() != null ? response.data().size() : 0);

            return response;

        } catch (WebClientException e) {
            log.error("Error fetching cost report from Claude: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch cost report from Claude", e);
        }
    }

    /**
     * Fetches Claude Code usage report from Claude Admin API.
     * This endpoint provides per-user statistics with email addresses.
     *
     * @param startingAt starting date (YYYY-MM-DD format)
     * @return Claude Code usage report with per-user data
     * @throws ApiClientException if API call fails
     */
    public ClaudeCodeUsageReport fetchClaudeCodeUsageReport(LocalDate startingAt)
            throws ApiClientException {
        log.info("Fetching Claude Code usage report from Claude API: starting_at={}", startingAt);

        try {
            ClaudeCodeUsageReport response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/organizations/usage_report/claude_code")
                    .queryParam("starting_at", startingAt.toString())
                    .queryParam("limit", 100)
                    .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(ClaudeCodeUsageReport.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                    .doBeforeRetry(signal ->
                        log.warn("Rate limit hit, retrying request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null) {
                throw new ApiClientException("Empty response from Claude Code usage report API");
            }

            log.info("Successfully fetched Claude Code usage report with {} records, hasMore={}",
                    response.data() != null ? response.data().size() : 0,
                    response.hasMore());

            return response;

        } catch (WebClientException e) {
            log.error("Error fetching Claude Code usage report: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch Claude Code usage report", e);
        }
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
