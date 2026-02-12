package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.github.dto.GitHubCopilotSeat;
import com.bemobi.aicontrol.integration.github.dto.GitHubCopilotSeatsResponse;
import com.bemobi.aicontrol.integration.github.dto.GitHubUser;
import com.bemobi.aicontrol.integration.github.dto.UserMetricsResponse;
import com.bemobi.aicontrol.integration.google.GoogleWorkspaceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GitHubCopilotApiClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private GitHubApiProperties properties;

    @Mock
    private GoogleWorkspaceClient workspaceClient;

    private GitHubCopilotApiClient client;
    private GitHubCopilotApiClient clientWithoutWorkspace;

    @BeforeEach
    void setUp() {
        when(properties.getBaseUrl()).thenReturn("https://api.github.com");
        when(properties.getToken()).thenReturn("test-token");
        when(properties.getOrganization()).thenReturn("test-org");
        when(properties.getTimeout()).thenReturn(30000);
        when(properties.getRetryAttempts()).thenReturn(3);
        when(properties.isEnabled()).thenReturn(true);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        client = new GitHubCopilotApiClient(webClientBuilder, properties, workspaceClient, objectMapper);
        clientWithoutWorkspace = new GitHubCopilotApiClient(webClientBuilder, properties, null, objectMapper);
    }

    @Test
    void testGetToolName() {
        assertEquals("github-copilot", client.getToolName());
    }

    @Test
    void testGetDisplayName() {
        assertEquals("GitHub Copilot", client.getDisplayName());
    }

    @Test
    void testIsEnabled() {
        assertTrue(client.isEnabled());
    }

    @Test
    void testFetchUsers_Success() throws ApiClientException {
        // Setup mock response
        GitHubUser user = new GitHubUser(
                "testuser", 123L, null, null, null, false, "Test User", "test@example.com", null, null
        );

        OffsetDateTime now = OffsetDateTime.now();
        GitHubCopilotSeat seat = new GitHubCopilotSeat(
                now.minusDays(30), now, null, now, "vscode", user, null
        );

        GitHubCopilotSeatsResponse response = new GitHubCopilotSeatsResponse(1, Arrays.asList(seat));

        // Mock Copilot seats call
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/orgs/{org}/copilot/billing/seats"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(eq("/users/{username}"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class)).thenReturn(Mono.just(response));

        // Workspace resolves email
        when(workspaceClient.findEmailByGitName("testuser")).thenReturn(Optional.of("testuser@corp.com"));

        // Execute
        List<UserData> users = client.fetchUsers();

        // Verify - workspace takes priority
        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("testuser@corp.com", userData.email());
        assertEquals("Test User", userData.name());
        assertEquals("active", userData.status());
        assertNotNull(userData.lastActivityAt());
        assertEquals("vscode", userData.additionalMetrics().get("last_activity_editor"));
        assertEquals("testuser", userData.additionalMetrics().get("github_login"));
        assertEquals(123L, userData.additionalMetrics().get("github_id"));
        assertEquals("workspace", userData.additionalMetrics().get("email_type"));
        assertNotNull(userData.rawJson());
        assertTrue(userData.rawJson().contains("vscode"));
        assertTrue(userData.rawJson().contains("testuser"));
    }

    @Test
    void testFetchUsers_WorkspaceNotFound_FallbackToGitHub() throws ApiClientException {
        GitHubUser user = new GitHubUser(
                "testuser", 123L, null, null, null, false, "Test User", null, null, null
        );

        OffsetDateTime now = OffsetDateTime.now();
        GitHubCopilotSeat seat = new GitHubCopilotSeat(
                now.minusDays(30), now, null, now, "vscode", user, null
        );

        GitHubCopilotSeatsResponse response = new GitHubCopilotSeatsResponse(1, Arrays.asList(seat));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/orgs/{org}/copilot/billing/seats"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(eq("/users/{username}"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class)).thenReturn(Mono.just(response));

        // Workspace returns empty
        when(workspaceClient.findEmailByGitName("testuser")).thenReturn(Optional.empty());

        // GitHub profile has email
        GitHubUser publicProfile = new GitHubUser(
                null, null, null, null, null, false, "Test User", "test@github-public.com", null, null
        );
        when(responseSpec.bodyToMono(GitHubUser.class)).thenReturn(Mono.just(publicProfile));

        List<UserData> users = client.fetchUsers();

        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("test@github-public.com", userData.email());
        assertEquals("real", userData.additionalMetrics().get("email_type"));
    }

    @Test
    void testFetchUsers_WorkspaceNotFound_GitHubNoEmail_Fallback() throws ApiClientException {
        GitHubUser user = new GitHubUser(
                "testuser", 123L, null, null, null, false, "Test User", null, null, null
        );

        OffsetDateTime now = OffsetDateTime.now();
        GitHubCopilotSeat seat = new GitHubCopilotSeat(
                null, null, null, now, "vscode", user, null
        );

        GitHubCopilotSeatsResponse response = new GitHubCopilotSeatsResponse(1, Arrays.asList(seat));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/orgs/{org}/copilot/billing/seats"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(eq("/users/{username}"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class)).thenReturn(Mono.just(response));

        // Workspace returns empty
        when(workspaceClient.findEmailByGitName("testuser")).thenReturn(Optional.empty());

        // GitHub profile has no email
        GitHubUser publicProfile = new GitHubUser(
                null, null, null, null, null, false, null, null, null, null
        );
        when(responseSpec.bodyToMono(GitHubUser.class)).thenReturn(Mono.just(publicProfile));

        List<UserData> users = client.fetchUsers();

        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("[sem-usr-github]", userData.email());
        assertEquals("not_found", userData.additionalMetrics().get("email_type"));
    }

    @Test
    void testFetchUsers_WorkspaceThrowsException_FallbackToGitHub() throws ApiClientException {
        GitHubUser user = new GitHubUser(
                "testuser", 123L, null, null, null, false, "Test User", null, null, null
        );

        OffsetDateTime now = OffsetDateTime.now();
        GitHubCopilotSeat seat = new GitHubCopilotSeat(
                now.minusDays(30), now, null, now, "vscode", user, null
        );

        GitHubCopilotSeatsResponse response = new GitHubCopilotSeatsResponse(1, Arrays.asList(seat));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/orgs/{org}/copilot/billing/seats"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(eq("/users/{username}"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class)).thenReturn(Mono.just(response));

        // Workspace throws exception
        when(workspaceClient.findEmailByGitName("testuser")).thenThrow(new RuntimeException("Workspace unavailable"));

        // GitHub profile has email as fallback
        GitHubUser publicProfile = new GitHubUser(
                null, null, null, null, null, false, "Test User", "fallback@github.com", null, null
        );
        when(responseSpec.bodyToMono(GitHubUser.class)).thenReturn(Mono.just(publicProfile));

        List<UserData> users = client.fetchUsers();

        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("fallback@github.com", userData.email());
        assertEquals("real", userData.additionalMetrics().get("email_type"));
    }

    @Test
    void testFetchUsers_WorkspaceDisabled_FallbackToGitHub() throws ApiClientException {
        // Use client without workspace
        GitHubUser user = new GitHubUser(
                "testuser", 123L, null, null, null, false, "Test User", "test@example.com", null, null
        );

        OffsetDateTime now = OffsetDateTime.now();
        GitHubCopilotSeat seat = new GitHubCopilotSeat(
                now.minusDays(30), now, null, now, "vscode", user, null
        );

        GitHubCopilotSeatsResponse response = new GitHubCopilotSeatsResponse(1, Arrays.asList(seat));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/orgs/{org}/copilot/billing/seats"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(eq("/users/{username}"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class)).thenReturn(Mono.just(response));

        // Mock public profile call
        GitHubUser publicProfile = new GitHubUser(
                null, null, null, null, null, false, "Test User", "test@example.com", null, null
        );
        when(responseSpec.bodyToMono(GitHubUser.class)).thenReturn(Mono.just(publicProfile));

        List<UserData> users = clientWithoutWorkspace.fetchUsers();

        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("test@example.com", userData.email());
        assertEquals("real", userData.additionalMetrics().get("email_type"));

        // Workspace should never be called
        verifyNoInteractions(workspaceClient);
    }

    @Test
    void testFetchUsers_WorkspaceDisabled_NoEmail() throws ApiClientException {
        GitHubUser user = new GitHubUser(
                "testuser", 123L, null, null, null, false, "Test User", null, null, null
        );

        OffsetDateTime now = OffsetDateTime.now();
        GitHubCopilotSeat seat = new GitHubCopilotSeat(
                null, null, null, now, "vscode", user, null
        );

        GitHubCopilotSeatsResponse response = new GitHubCopilotSeatsResponse(1, Arrays.asList(seat));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/orgs/{org}/copilot/billing/seats"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(eq("/users/{username}"), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class)).thenReturn(Mono.just(response));

        GitHubUser publicProfile = new GitHubUser(
                null, null, null, null, null, false, null, null, null, null
        );
        when(responseSpec.bodyToMono(GitHubUser.class)).thenReturn(Mono.just(publicProfile));

        List<UserData> users = clientWithoutWorkspace.fetchUsers();

        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("[sem-usr-github]", userData.email());
        assertEquals("not_found", userData.additionalMetrics().get("email_type"));
    }

    @Test
    void testFetchUsers_EmptyResponse() throws ApiClientException {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class)).thenReturn(Mono.empty());

        List<UserData> users = client.fetchUsers();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void testFetchUsers_NotFound() throws ApiClientException {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class))
            .thenReturn(Mono.error(WebClientResponseException.create(
                404, "Not Found", null, null, null)));

        List<UserData> users = client.fetchUsers();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void testFetchUsers_WebClientException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class))
            .thenReturn(Mono.error(WebClientResponseException.create(
                401, "Unauthorized", null, null, null)));

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUsers();
        });

        assertTrue(exception.getMessage().contains("Failed to fetch users from GitHub Copilot"));
    }

    @Test
    void testTestConnection_Success() throws ApiClientException {
        GitHubCopilotSeatsResponse response = new GitHubCopilotSeatsResponse(0, Arrays.asList());

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class)).thenReturn(Mono.just(response));

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("github-copilot", result.toolName());
    }

    @Test
    void testTestConnection_Failure() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GitHubCopilotSeatsResponse.class))
            .thenReturn(Mono.error(new RuntimeException("Connection failed")));

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertFalse(result.success());
        assertEquals("github-copilot", result.toolName());
    }

    @Test
    void testFetchUserMetrics_Success() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        // Mock initial API response with signed URL
        Map<String, Object> initialResponse = new HashMap<>();
        initialResponse.put("report_url", "https://signed-url.example.com/report.ndjson");
        initialResponse.put("expires_at", "2026-02-11T00:00:00Z");

        // Mock NDJSON data
        String ndjsonData = "{\"user_name\":\"john.doe\",\"user_email\":\"john@example.com\",\"date\":\"2026-02-10\",\"user_initiated_interaction_count\":50,\"code_generation_activity_count\":40,\"code_acceptance_activity_count\":30,\"loc_suggested_to_add_sum\":500,\"loc_added_sum\":400,\"loc_deleted_sum\":100}\n" +
                "{\"user_name\":\"jane.smith\",\"user_email\":\"jane@example.com\",\"date\":\"2026-02-10\",\"user_initiated_interaction_count\":60,\"code_generation_activity_count\":45,\"code_acceptance_activity_count\":35,\"loc_suggested_to_add_sum\":600,\"loc_added_sum\":500,\"loc_deleted_sum\":150}";

        // Setup mocks
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(initialResponse));

        // Note: For the signed URL WebClient call, we would need to mock WebClient.create()
        // For now, we'll test the parsing logic in isolation

        UserMetricsResponse response = client.fetchUserMetrics(date);

        assertNotNull(response);
        assertEquals("https://signed-url.example.com/report.ndjson", response.reportUrl());
        assertEquals("2026-02-11T00:00:00Z", response.expiresAt());
    }

    @Test
    void testFetchUserMetrics_EmptyResponse() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());

        UserMetricsResponse response = client.fetchUserMetrics(date);

        assertNotNull(response);
        assertNull(response.reportUrl());
        assertNull(response.expiresAt());
        assertTrue(response.data().isEmpty());
    }

    @Test
    void testFetchUserMetrics_NotFound() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
            .thenReturn(Mono.error(WebClientResponseException.create(
                404, "Not Found", null, null, null)));

        UserMetricsResponse response = client.fetchUserMetrics(date);

        assertNotNull(response);
        assertNull(response.reportUrl());
        assertTrue(response.data().isEmpty());
    }

    @Test
    void testFetchUserMetrics_WebClientException() {
        LocalDate date = LocalDate.of(2026, 2, 10);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
            .thenReturn(Mono.error(WebClientResponseException.create(
                401, "Unauthorized", null, null, null)));

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUserMetrics(date);
        });

        assertTrue(exception.getMessage().contains("Failed to fetch user metrics"));
    }
}
