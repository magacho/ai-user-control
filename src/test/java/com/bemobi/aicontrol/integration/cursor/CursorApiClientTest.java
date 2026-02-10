package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.cursor.dto.CursorTeamMember;
import com.bemobi.aicontrol.integration.cursor.dto.CursorTeamMembersResponse;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CursorApiClientTest {

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
    private CursorApiProperties properties;

    private CursorApiClient client;

    @BeforeEach
    void setUp() {
        when(properties.getBaseUrl()).thenReturn("https://api.cursor.com");
        when(properties.getToken()).thenReturn("test-token");
        when(properties.getTimeout()).thenReturn(30000);
        when(properties.getRetryAttempts()).thenReturn(3);
        when(properties.isEnabled()).thenReturn(true);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        client = new CursorApiClient(webClientBuilder, properties);
    }

    @Test
    void testGetToolName() {
        assertEquals("cursor", client.getToolName());
    }

    @Test
    void testGetDisplayName() {
        assertEquals("Cursor", client.getDisplayName());
    }

    @Test
    void testIsEnabled() {
        assertTrue(client.isEnabled());
    }

    @Test
    void testFetchUsers_Success() throws ApiClientException {
        // Setup mock response
        CursorTeamMember member = new CursorTeamMember("Test User", "test@example.com", "admin", "user_123");

        CursorTeamMembersResponse response = new CursorTeamMembersResponse(Arrays.asList(member));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CursorTeamMembersResponse.class)).thenReturn(Mono.just(response));

        // Execute
        List<UserData> users = client.fetchUsers();

        // Verify
        assertNotNull(users);
        assertEquals(1, users.size());

        UserData userData = users.get(0);
        assertEquals("test@example.com", userData.email());
        assertEquals("Test User", userData.name());
        assertEquals("active", userData.status());
        assertEquals("admin", userData.additionalMetrics().get("role"));
        assertEquals("user_123", userData.additionalMetrics().get("user_id"));
    }

    @Test
    void testFetchUsers_EmptyResponse() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CursorTeamMembersResponse.class)).thenReturn(Mono.empty());

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUsers();
        });

        assertTrue(exception.getMessage().contains("Empty response"));
    }

    @Test
    void testFetchUsers_NullTeamMembers() {
        CursorTeamMembersResponse response = new CursorTeamMembersResponse(null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CursorTeamMembersResponse.class)).thenReturn(Mono.just(response));

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUsers();
        });

        assertTrue(exception.getMessage().contains("Empty response"));
    }

    @Test
    void testFetchUsers_WebClientException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CursorTeamMembersResponse.class))
            .thenReturn(Mono.error(WebClientResponseException.create(
                500, "Internal Server Error", null, null, null)));

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUsers();
        });

        assertTrue(exception.getMessage().contains("Failed to fetch users from Cursor"));
    }

    @Test
    void testFetchUsers_EmailNormalization() throws ApiClientException {
        // Test that emails are normalized to lowercase
        CursorTeamMember member = new CursorTeamMember("Test User", "Test.User@EXAMPLE.COM", "member", "user_123");

        CursorTeamMembersResponse response = new CursorTeamMembersResponse(Arrays.asList(member));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CursorTeamMembersResponse.class)).thenReturn(Mono.just(response));

        List<UserData> users = client.fetchUsers();

        assertEquals("test.user@example.com", users.get(0).email());
    }

    @Test
    void testFetchUsers_MultipleMembers() throws ApiClientException {
        // Test with multiple team members
        CursorTeamMember member1 = new CursorTeamMember("User One", "user1@example.com", "admin", "user_1");
        CursorTeamMember member2 = new CursorTeamMember("User Two", "user2@example.com", "member", "user_2");

        CursorTeamMembersResponse response = new CursorTeamMembersResponse(Arrays.asList(member1, member2));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CursorTeamMembersResponse.class)).thenReturn(Mono.just(response));

        List<UserData> users = client.fetchUsers();

        assertEquals(2, users.size());
        assertEquals("user1@example.com", users.get(0).email());
        assertEquals("user2@example.com", users.get(1).email());
        assertEquals("admin", users.get(0).additionalMetrics().get("role"));
        assertEquals("member", users.get(1).additionalMetrics().get("role"));
    }

    @Test
    void testTestConnection_Success() throws ApiClientException {
        // Setup successful fetch
        CursorTeamMembersResponse response = new CursorTeamMembersResponse(Arrays.asList());

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CursorTeamMembersResponse.class)).thenReturn(Mono.just(response));

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("cursor", result.toolName());
    }

    @Test
    void testTestConnection_Failure() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CursorTeamMembersResponse.class))
            .thenReturn(Mono.error(new RuntimeException("Connection failed")));

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertFalse(result.success());
        assertEquals("cursor", result.toolName());
    }
}
