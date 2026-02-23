package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.ToolApiClient;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.cursor.dto.CursorTeamMember;
import com.bemobi.aicontrol.integration.cursor.dto.CursorTeamMembersResponse;
import com.bemobi.aicontrol.integration.cursor.dto.DailyUsageResponse;
import com.bemobi.aicontrol.integration.cursor.dto.SpendingDataResponse;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for integrating with Cursor (Admin API).
 *
 * This client fetches team members from the Cursor Admin API.
 * Requires an Admin API key with appropriate permissions.
 *
 * API Documentation: https://cursor.com/docs/account/teams/admin-api
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.cursor", name = "enabled", havingValue = "true")
public class CursorApiClient implements ToolApiClient {

    private static final Logger log = LoggerFactory.getLogger(CursorApiClient.class);

    private final WebClient webClient;
    private final CursorApiProperties properties;
    private final ObjectMapper objectMapper;

    public CursorApiClient(WebClient.Builder webClientBuilder,
                          CursorApiProperties properties,
                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        // Only create WebClient if properties are configured
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl() : "https://api.cursor.com";
        String token = properties.getToken() != null ? properties.getToken() : "";

        // Cursor API uses Basic Auth with API key as username (password is empty)
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((token + ":").getBytes());

        this.webClient = webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", authHeader)
            .defaultHeader("Content-Type", "application/json")
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer for large responses
            .build();
    }

    @Override
    public String getToolName() {
        return "cursor";
    }

    @Override
    public String getDisplayName() {
        return "Cursor";
    }

    @Override
    public List<UserData> fetchUsers() throws ApiClientException {
        log.info("Fetching users from Cursor Admin API");

        try {
            CursorTeamMembersResponse response = webClient.get()
                .uri("/teams/members")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(CursorTeamMembersResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                    .doBeforeRetry(signal ->
                        log.warn("Rate limit hit, retrying request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null || response.teamMembers() == null) {
                throw new ApiClientException("Empty response from Cursor Admin API");
            }

            log.info("Successfully fetched {} users from Cursor", response.teamMembers().size());

            return response.teamMembers().stream()
                .map(this::mapToUserData)
                .collect(Collectors.toList());

        } catch (WebClientException e) {
            log.error("Error fetching users from Cursor: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch users from Cursor", e);
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

    private UserData mapToUserData(CursorTeamMember member) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("role", member.role());
        metrics.put("user_id", member.userId());

        return new UserData(
                member.email() != null ? member.email().toLowerCase() : null,
                member.name(),
                "active",
                null,
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
                log.error("4xx error from Cursor Admin API: {} - {}", response.statusCode(), body);
                return Mono.error(new ApiClientException("Client error: " + body));
            });
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("5xx error from Cursor Admin API: {} - {}", response.statusCode(), body);
                return Mono.error(new ApiClientException("Server error: " + body));
            });
    }

    /**
     * Fetch spending data from Cursor Admin API.
     *
     * <p>Retrieves spending information for all team members.</p>
     *
     * <p>Note: API expects POST with date range in body.</p>
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return spending data response containing per-user spending information
     * @throws ApiClientException if the API request fails
     */
    public SpendingDataResponse fetchSpendingData(
            java.time.LocalDate startDate, java.time.LocalDate endDate) throws ApiClientException {
        log.info("Fetching spending data from Cursor Admin API: {} to {}", startDate, endDate);

        // Cursor API uses date strings in ISO format (YYYY-MM-DD)
        Map<String, String> requestBody = Map.of(
            "startDate", startDate.toString(),
            "endDate", endDate.toString()
        );

        try {
            SpendingDataResponse response = webClient.post()
                .uri("/teams/spending-data")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(SpendingDataResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                    .doBeforeRetry(signal ->
                        log.warn("Rate limit hit, retrying request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null || response.data() == null) {
                throw new ApiClientException("Empty response from Cursor Admin API (spending-data)");
            }

            log.info("Successfully fetched spending data for {} users from Cursor", response.data().size());

            return response;

        } catch (WebClientException e) {
            log.error("Error fetching spending data from Cursor: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch spending data from Cursor", e);
        }
    }

    /**
     * Fetch daily usage data from Cursor Admin API.
     *
     * <p>Retrieves daily usage metrics for all team members, including token usage,
     * lines added/deleted, acceptance rates, and model usage statistics.</p>
     *
     * <p>Note: API expects POST with date range in body.</p>
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return daily usage response containing per-user daily metrics
     * @throws ApiClientException if the API request fails
     */
    public DailyUsageResponse fetchDailyUsage(
            java.time.LocalDate startDate, java.time.LocalDate endDate) throws ApiClientException {
        log.info("Fetching daily usage data from Cursor Admin API: {} to {}", startDate, endDate);

        // Cursor API uses date strings in ISO format (YYYY-MM-DD)
        Map<String, String> requestBody = Map.of(
            "startDate", startDate.toString(),
            "endDate", endDate.toString()
        );

        try {
            DailyUsageResponse response = webClient.post()
                .uri("/teams/daily-usage-data")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(DailyUsageResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                    .doBeforeRetry(signal ->
                        log.warn("Rate limit hit, retrying request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null || response.data() == null) {
                throw new ApiClientException("Empty response from Cursor Admin API (daily-usage-data)");
            }

            log.info("Successfully fetched daily usage data for {} records from Cursor", response.data().size());

            return response;

        } catch (WebClientException e) {
            log.error("Error fetching daily usage data from Cursor: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch daily usage data from Cursor", e);
        }
    }
}
