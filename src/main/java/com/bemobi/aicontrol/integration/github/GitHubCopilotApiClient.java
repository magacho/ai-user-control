package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.ToolApiClient;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.github.dto.GitHubCopilotSeat;
import com.bemobi.aicontrol.integration.github.dto.GitHubCopilotSeatsResponse;
import com.bemobi.aicontrol.integration.github.dto.GitHubUser;
import com.bemobi.aicontrol.integration.github.dto.UserMetric;
import com.bemobi.aicontrol.integration.github.dto.UserMetricsResponse;
import com.bemobi.aicontrol.integration.google.GoogleWorkspaceClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Client for integrating with GitHub Copilot API.
 *
 * This client fetches Copilot seat assignments from the GitHub API,
 * handling authentication, rate limiting, and error responses.
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.github", name = "enabled", havingValue = "true")
public class GitHubCopilotApiClient implements ToolApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubCopilotApiClient.class);

    private final WebClient webClient;
    private final GitHubApiProperties properties;
    private final GoogleWorkspaceClient workspaceClient;
    private final ObjectMapper objectMapper;

    public GitHubCopilotApiClient(WebClient.Builder webClientBuilder,
                                 GitHubApiProperties properties,
                                 @Autowired(required = false) GoogleWorkspaceClient workspaceClient,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.workspaceClient = workspaceClient;
        this.objectMapper = objectMapper;

        // Only create WebClient if properties are configured
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl() : "https://api.github.com";
        String token = properties.getToken() != null ? properties.getToken() : "";

        this.webClient = webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + token)
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
    }

    @Override
    public String getToolName() {
        return "github-copilot";
    }

    @Override
    public String getDisplayName() {
        return "GitHub Copilot";
    }

    @Override
    public List<UserData> fetchUsers() throws ApiClientException {
        log.info("Fetching users from GitHub Copilot API");

        try {
            GitHubCopilotSeatsResponse response = webClient.get()
                .uri("/orgs/{org}/copilot/billing/seats", properties.getOrganization())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(GitHubCopilotSeatsResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException
                            && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError())
                    .doBeforeRetry(signal ->
                        log.warn("Server error, retrying request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null || response.seats() == null) {
                log.warn("Empty response from GitHub Copilot API");
                return Collections.emptyList();
            }

            log.info("Successfully fetched {} Copilot seats from GitHub", response.totalSeats());
            log.info("Fetching public emails for {} users (this may take a moment)", response.totalSeats());

            return response.seats().stream()
                .map(this::mapToUserData)
                .collect(Collectors.toList());

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Organization '{}' not found or doesn't have Copilot", properties.getOrganization());
            return Collections.emptyList();
        } catch (WebClientException e) {
            log.error("Error fetching users from GitHub Copilot: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch users from GitHub Copilot", e);
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

    private UserData mapToUserData(GitHubCopilotSeat seat) {
        String email = null;
        String name = null;
        Map<String, Object> metrics = new HashMap<>();

        GitHubUser assignee = seat.assignee();
        if (assignee != null) {
            String emailType = "not_found";

            // Priority 1: Google Workspace lookup (if enabled)
            if (workspaceClient != null) {
                try {
                    Optional<String> workspaceEmail = workspaceClient.findEmailByGitName(assignee.login());
                    if (workspaceEmail.isPresent()) {
                        email = workspaceEmail.get();
                        emailType = "workspace";
                        log.debug("Workspace resolved email for user {}: {}", assignee.login(), email);
                    }
                } catch (Exception e) {
                    log.debug("Workspace lookup failed for user {}: {}", assignee.login(), e.getMessage());
                }
            }

            // Priority 2: GitHub public profile
            if (email == null || email.isEmpty()) {
                try {
                    GitHubUser publicProfile = fetchUserPublicProfile(assignee.login());
                    if (publicProfile != null && publicProfile.email() != null && !publicProfile.email().isEmpty()) {
                        email = publicProfile.email();
                        emailType = "real";
                        log.debug("Found public email for user {}: {}", assignee.login(), email);
                    }
                } catch (Exception e) {
                    log.debug("Could not fetch public profile for user {}: {}", assignee.login(), e.getMessage());
                }
            }

            // Priority 3: Fallback - no email found
            if (email == null || email.isEmpty()) {
                email = "[SEM-USR-GITHUB]";
                emailType = "not_found";
                log.info("Email not available for user {}, marking as unresolved", assignee.login());
            }

            email = email.toLowerCase();
            name = assignee.name() != null ? assignee.name() : assignee.login();

            metrics.put("last_activity_editor", seat.lastActivityEditor());
            metrics.put("created_at", seat.createdAt());
            metrics.put("updated_at", seat.updatedAt());
            metrics.put("github_login", assignee.login());
            metrics.put("github_id", assignee.id());
            metrics.put("email_type", emailType);
        }

        return new UserData(
                email,
                name,
                "active",
                seat.lastActivityAt() != null ? seat.lastActivityAt().toLocalDateTime() : null,
                metrics,
                toRawJson(seat)
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

    /**
     * Fetch user's public profile to get their public email address.
     * Returns null if profile cannot be fetched or user doesn't have public email.
     */
    private GitHubUser fetchUserPublicProfile(String username) {
        try {
            return webClient.get()
                .uri("/users/{username}", username)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.empty())
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.empty())
                .bodyToMono(GitHubUser.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(throwable -> {
                    log.debug("Error fetching public profile for {}: {}", username, throwable.getMessage());
                    return Mono.empty();
                })
                .block();
        } catch (Exception e) {
            log.debug("Failed to fetch public profile for {}: {}", username, e.getMessage());
            return null;
        }
    }

    private Mono<? extends Throwable> handle4xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("4xx error from GitHub API: {} - {}", response.statusCode(), body);

                // Check rate limit headers
                response.headers().header("X-RateLimit-Remaining").stream().findFirst()
                    .ifPresent(remaining -> {
                        if ("0".equals(remaining)) {
                            String reset = response.headers().header("X-RateLimit-Reset")
                                .stream().findFirst().orElse("unknown");
                            log.error("GitHub API rate limit exceeded. Reset at: {}", reset);
                        }
                    });

                return Mono.error(new ApiClientException("Client error: " + body));
            });
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("5xx error from GitHub API: {} - {}", response.statusCode(), body);
                return Mono.error(new ApiClientException("Server error: " + body));
            });
    }

    /**
     * Fetches user metrics for a specific date from the GitHub Copilot Metrics API.
     *
     * <p>This method uses the new Metrics API endpoint:
     * GET /orgs/{org}/copilot/metrics/reports/users-1-day?day=YYYY-MM-DD</p>
     *
     * <p>The API returns a signed URL that must be accessed to download the actual
     * metrics data in NDJSON format (one JSON object per line, one per user).</p>
     *
     * @param date the date for which to fetch metrics (YYYY-MM-DD format)
     * @return UserMetricsResponse containing the parsed metrics data
     * @throws ApiClientException if the API call fails or parsing fails
     */
    public UserMetricsResponse fetchUserMetrics(LocalDate date) throws ApiClientException {
        log.info("Fetching user metrics from GitHub Copilot API for date: {}", date);

        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            // Step 1: Get the signed URL from the metrics API
            Map<String, Object> initialResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/orgs/{org}/copilot/metrics/reports/users-1-day")
                    .queryParam("day", dateStr)
                    .build(properties.getOrganization()))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException
                            && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError())
                    .doBeforeRetry(signal ->
                        log.warn("Server error, retrying metrics request. Attempt: {}", signal.totalRetries() + 1)))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (initialResponse == null) {
                log.warn("Empty response from GitHub Copilot Metrics API");
                return new UserMetricsResponse(null, null, Collections.emptyList());
            }

            String reportUrl = (String) initialResponse.get("report_url");
            String expiresAt = (String) initialResponse.get("expires_at");

            if (reportUrl == null || reportUrl.isEmpty()) {
                log.warn("No report URL in metrics API response");
                return new UserMetricsResponse(reportUrl, expiresAt, Collections.emptyList());
            }

            log.debug("Fetching metrics data from signed URL: {}", reportUrl);

            // Step 2: Download NDJSON data from the signed URL
            String ndjsonData = WebClient.create()
                .get()
                .uri(reportUrl)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(String.class)
                .block(Duration.ofMillis(properties.getTimeout()));

            if (ndjsonData == null || ndjsonData.isEmpty()) {
                log.warn("Empty NDJSON data from signed URL");
                return new UserMetricsResponse(reportUrl, expiresAt, Collections.emptyList());
            }

            // Step 3: Parse NDJSON (one JSON object per line)
            List<UserMetric> metrics = parseNdjson(ndjsonData);
            log.info("Successfully parsed {} user metrics for date {}", metrics.size(), date);

            return new UserMetricsResponse(reportUrl, expiresAt, metrics);

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Metrics not found for organization '{}' on date {}", properties.getOrganization(), date);
            return new UserMetricsResponse(null, null, Collections.emptyList());
        } catch (WebClientException e) {
            log.error("Error fetching user metrics from GitHub Copilot: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch user metrics from GitHub Copilot", e);
        } catch (Exception e) {
            log.error("Error parsing metrics data: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to parse metrics data", e);
        }
    }

    /**
     * Parses NDJSON (Newline Delimited JSON) into a list of UserMetric objects.
     *
     * @param ndjson the NDJSON string to parse
     * @return list of parsed UserMetric objects
     * @throws Exception if parsing fails
     */
    private List<UserMetric> parseNdjson(String ndjson) throws Exception {
        List<UserMetric> metrics = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(ndjson));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                UserMetric metric = objectMapper.readValue(line, UserMetric.class);
                metrics.add(metric);
            }
        }
        return metrics;
    }
}
