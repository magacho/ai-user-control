package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.github.dto.GitHubCopilotSeat;
import com.bemobi.aicontrol.integration.github.dto.GitHubCopilotSeatsResponse;
import com.bemobi.aicontrol.integration.github.dto.GitHubUser;
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

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

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

    private GitHubCopilotApiClient client;

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

        client = new GitHubCopilotApiClient(webClientBuilder, properties);
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

        // Mock public profile call - user has public email
        GitHubUser publicProfile = new GitHubUser(
                null, null, null, null, null, false, "Test User", "test@example.com", null, null
        );
        when(responseSpec.bodyToMono(GitHubUser.class)).thenReturn(Mono.just(publicProfile));

        // Execute
        List<UserData> users = client.fetchUsers();

        // Verify
        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("test@example.com", userData.email());
        assertEquals("Test User", userData.name());
        assertEquals("active", userData.status());
        assertNotNull(userData.lastActivityAt());
        assertEquals("vscode", userData.additionalMetrics().get("last_activity_editor"));
        assertEquals("testuser", userData.additionalMetrics().get("github_login"));
        assertEquals(123L, userData.additionalMetrics().get("github_id"));
        assertEquals("real", userData.additionalMetrics().get("email_type"));
    }

    @Test
    void testFetchUsers_NoEmail() throws ApiClientException {
        // Setup mock response without email
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

        // Mock public profile call - user has no public email
        GitHubUser publicProfile = new GitHubUser(
                null, null, null, null, null, false, null, null, null, null
        );
        when(responseSpec.bodyToMono(GitHubUser.class)).thenReturn(Mono.just(publicProfile));

        // Execute
        List<UserData> users = client.fetchUsers();

        // Verify
        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("testuser@github.local", userData.email()); // Fallback email
        assertEquals("Test User", userData.name());
        assertEquals("generated", userData.additionalMetrics().get("email_type"));
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
}
