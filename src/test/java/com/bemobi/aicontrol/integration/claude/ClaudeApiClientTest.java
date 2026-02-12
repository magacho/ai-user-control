package com.bemobi.aicontrol.integration.claude;

import com.bemobi.aicontrol.integration.claude.dto.ClaudeMember;
import com.bemobi.aicontrol.integration.claude.dto.ClaudeMembersResponse;
import com.bemobi.aicontrol.integration.claude.dto.CostDataPoint;
import com.bemobi.aicontrol.integration.claude.dto.CostReportResponse;
import com.bemobi.aicontrol.integration.claude.dto.UsageDataPoint;
import com.bemobi.aicontrol.integration.claude.dto.UsageReportResponse;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaudeApiClientTest {

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
    private ClaudeApiProperties properties;

    private ClaudeApiClient client;

    @BeforeEach
    void setUp() {
        when(properties.getBaseUrl()).thenReturn("https://api.anthropic.com");
        when(properties.getToken()).thenReturn("test-token");
        // organizationId is no longer required for Admin API
        when(properties.getTimeout()).thenReturn(30000);
        when(properties.getRetryAttempts()).thenReturn(3);
        when(properties.isEnabled()).thenReturn(true);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        client = new ClaudeApiClient(webClientBuilder, properties, objectMapper);
    }

    @Test
    void testGetToolName() {
        assertEquals("claude-code", client.getToolName());
    }

    @Test
    void testGetDisplayName() {
        assertEquals("Claude Code", client.getDisplayName());
    }

    @Test
    void testIsEnabled() {
        assertTrue(client.isEnabled());
    }

    @Test
    void testFetchUsers_Success() throws ApiClientException {
        // Setup mock response
        LocalDateTime joinedAt = LocalDateTime.now().minusDays(30);
        LocalDateTime lastActiveAt = LocalDateTime.now();
        ClaudeMember member = new ClaudeMember(
                "user_123", null, "test@example.com", "Test User",
                "member", "active", joinedAt, lastActiveAt
        );

        ClaudeMembersResponse response = new ClaudeMembersResponse(
                null, Arrays.asList(member), false, null, null
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClaudeMembersResponse.class)).thenReturn(Mono.just(response));

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
        assertEquals("member", userData.additionalMetrics().get("role"));
        assertEquals("user_123", userData.additionalMetrics().get("member_id"));
        assertNotNull(userData.rawJson());
        assertTrue(userData.rawJson().contains("test@example.com"));
        assertTrue(userData.rawJson().contains("user_123"));
    }

    @Test
    void testFetchUsers_EmptyResponse() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClaudeMembersResponse.class)).thenReturn(Mono.empty());

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUsers();
        });

        assertTrue(exception.getMessage().contains("Empty response"));
    }

    @Test
    void testFetchUsers_WebClientException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClaudeMembersResponse.class))
            .thenReturn(Mono.error(WebClientResponseException.create(
                500, "Internal Server Error", null, null, null)));

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUsers();
        });

        assertTrue(exception.getMessage().contains("Failed to fetch users from Claude Code"));
    }

    @Test
    void testTestConnection_Success() throws ApiClientException {
        // Setup successful fetch
        ClaudeMembersResponse response = new ClaudeMembersResponse(
                null, Arrays.asList(), false, null, null
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClaudeMembersResponse.class)).thenReturn(Mono.just(response));

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("claude-code", result.toolName());
    }

    @Test
    void testTestConnection_Failure() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ClaudeMembersResponse.class))
            .thenReturn(Mono.error(new RuntimeException("Connection failed")));

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertFalse(result.success());
        assertEquals("claude-code", result.toolName());
    }

    @Test
    void testFetchUsageReport_Success() throws ApiClientException {
        // Setup mock response
        LocalDateTime timestamp = LocalDateTime.now();
        UsageDataPoint dataPoint = new UsageDataPoint(
                timestamp,
                "workspace-123",
                "api-key-456",
                "claude-3-opus-20240229",
                1000L,
                500L,
                100L,
                200L
        );

        UsageReportResponse response = new UsageReportResponse(
                "1d",
                Arrays.asList(dataPoint)
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UsageReportResponse.class)).thenReturn(Mono.just(response));

        // Execute
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        UsageReportResponse result = client.fetchUsageReport(startDate, endDate, "1d");

        // Verify
        assertNotNull(result);
        assertEquals("1d", result.bucketWidth());
        assertNotNull(result.data());
        assertEquals(1, result.data().size());
        assertEquals("workspace-123", result.data().get(0).workspaceId());
        assertEquals(1000L, result.data().get(0).inputTokens());
    }

    @Test
    void testFetchUsageReport_EmptyResponse() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UsageReportResponse.class)).thenReturn(Mono.empty());

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUsageReport(startDate, endDate, "1d");
        });

        assertTrue(exception.getMessage().contains("Empty response"));
    }

    @Test
    void testFetchUsageReport_WebClientException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UsageReportResponse.class))
            .thenReturn(Mono.error(WebClientResponseException.create(
                500, "Internal Server Error", null, null, null)));

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchUsageReport(startDate, endDate, "1d");
        });

        assertTrue(exception.getMessage().contains("Failed to fetch usage report"));
    }

    @Test
    void testFetchCostReport_Success() throws ApiClientException {
        // Setup mock response
        LocalDateTime timestamp = LocalDateTime.now();
        CostDataPoint dataPoint = new CostDataPoint(
                timestamp,
                "workspace-123",
                5000L,
                "api_usage",
                "API usage costs"
        );

        CostReportResponse response = new CostReportResponse(
                Arrays.asList(dataPoint)
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CostReportResponse.class)).thenReturn(Mono.just(response));

        // Execute
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        CostReportResponse result = client.fetchCostReport(startDate, endDate);

        // Verify
        assertNotNull(result);
        assertNotNull(result.data());
        assertEquals(1, result.data().size());
        assertEquals("workspace-123", result.data().get(0).workspaceId());
        assertEquals(5000L, result.data().get(0).cost());
    }

    @Test
    void testFetchCostReport_EmptyResponse() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CostReportResponse.class)).thenReturn(Mono.empty());

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchCostReport(startDate, endDate);
        });

        assertTrue(exception.getMessage().contains("Empty response"));
    }

    @Test
    void testFetchCostReport_WebClientException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CostReportResponse.class))
            .thenReturn(Mono.error(WebClientResponseException.create(
                500, "Internal Server Error", null, null, null)));

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.fetchCostReport(startDate, endDate);
        });

        assertTrue(exception.getMessage().contains("Failed to fetch cost report"));
    }
}
