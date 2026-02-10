package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.ToolApiClient;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.github.dto.GitHubCopilotSeat;
import com.bemobi.aicontrol.integration.github.dto.GitHubCopilotSeatsResponse;
import com.bemobi.aicontrol.integration.github.dto.GitHubUser;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public GitHubCopilotApiClient(WebClient.Builder webClientBuilder,
                                 GitHubApiProperties properties) {
        this.properties = properties;

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
                    .filter(throwable -> throwable instanceof WebClientResponseException &&
                           ((WebClientResponseException) throwable).getStatusCode().is5xxServerError())
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
        UserData userData = new UserData();

        GitHubUser assignee = seat.assignee();
        if (assignee != null) {
            // Tentar obter email real do perfil público do usuário
            String email = null;
            String emailType = "generated";

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

            // Fallback para email gerado se não encontrou email público
            if (email == null || email.isEmpty()) {
                email = assignee.login() + "@github.local";
                log.info("Email not available for user {}, using generated email", assignee.login());
            }

            userData.setEmail(email.toLowerCase());
            userData.setName(assignee.name() != null ? assignee.name() : assignee.login());

            // Métricas adicionais
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("last_activity_editor", seat.lastActivityEditor());
            metrics.put("created_at", seat.createdAt());
            metrics.put("updated_at", seat.updatedAt());
            metrics.put("github_login", assignee.login());
            metrics.put("github_id", assignee.id());
            metrics.put("email_type", emailType);
            userData.setAdditionalMetrics(metrics);
        }

        userData.setStatus("active"); // Todos os seats são ativos

        // Converter OffsetDateTime para LocalDateTime
        if (seat.lastActivityAt() != null) {
            userData.setLastActivityAt(seat.lastActivityAt().toLocalDateTime());
        }

        return userData;
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
}
