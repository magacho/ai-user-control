package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.github.dto.UserMetric;
import com.bemobi.aicontrol.integration.github.dto.UserMetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubCopilotUsageDataCollectorTest {

    @Mock
    private GitHubCopilotApiClient apiClient;

    private GitHubCopilotUsageDataCollector collector;

    @BeforeEach
    void setUp() {
        collector = new GitHubCopilotUsageDataCollector(apiClient);
    }

    @Test
    void testGetToolType() {
        assertEquals(ToolType.GITHUB_COPILOT, collector.getToolType());
    }

    @Test
    void testCollectSpendingData_ReturnsEmptyList() throws ApiClientException {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 5);

        List<UnifiedSpendingRecord> result = collector.collectSpendingData(startDate, endDate);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(apiClient);
    }

    @Test
    void testCollectUsageData_SingleDay_Success() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        UserMetric metric1 = new UserMetric(
                "john.doe",
                "john@example.com",
                "2026-02-10",
                50,
                40,
                30,
                500,
                400,
                100
        );

        UserMetric metric2 = new UserMetric(
                "jane.smith",
                "jane@example.com",
                "2026-02-10",
                60,
                45,
                35,
                600,
                500,
                150
        );

        UserMetricsResponse response = new UserMetricsResponse(
                "https://example.com/report",
                "2026-02-11T00:00:00Z",
                Arrays.asList(metric1, metric2)
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify first record
        UnifiedUsageRecord record1 = results.get(0);
        assertEquals("john@example.com", record1.email());
        assertEquals(ToolType.GITHUB_COPILOT, record1.tool());
        assertEquals(date, record1.date());
        assertNull(record1.inputTokens()); // GitHub doesn't expose tokens
        assertNull(record1.outputTokens());
        assertNull(record1.cacheReadTokens());
        assertEquals(500, record1.linesSuggested());
        assertEquals(400, record1.linesAccepted());
        assertEquals(0.8, record1.acceptanceRate(), 0.001); // 400/500 = 0.8
        assertNotNull(record1.rawMetadata());
        assertEquals("john.doe", record1.rawMetadata().get("user_name"));

        // Verify second record
        UnifiedUsageRecord record2 = results.get(1);
        assertEquals("jane@example.com", record2.email());
        assertEquals(ToolType.GITHUB_COPILOT, record2.tool());
        assertEquals(600, record2.linesSuggested());
        assertEquals(500, record2.linesAccepted());
        assertEquals(0.833, record2.acceptanceRate(), 0.001); // 500/600 = 0.833...

        verify(apiClient, times(1)).fetchUserMetrics(date);
    }

    @Test
    void testCollectUsageData_MultipleDay_Success() throws ApiClientException {
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 12);

        UserMetric metric1 = new UserMetric(
                "john.doe",
                "john@example.com",
                "2026-02-10",
                50, 40, 30, 500, 400, 100
        );

        UserMetric metric2 = new UserMetric(
                "john.doe",
                "john@example.com",
                "2026-02-11",
                55, 42, 32, 550, 420, 110
        );

        UserMetric metric3 = new UserMetric(
                "john.doe",
                "john@example.com",
                "2026-02-12",
                60, 45, 35, 600, 450, 120
        );

        when(apiClient.fetchUserMetrics(LocalDate.of(2026, 2, 10)))
                .thenReturn(new UserMetricsResponse("url1", "exp1", Arrays.asList(metric1)));
        when(apiClient.fetchUserMetrics(LocalDate.of(2026, 2, 11)))
                .thenReturn(new UserMetricsResponse("url2", "exp2", Arrays.asList(metric2)));
        when(apiClient.fetchUserMetrics(LocalDate.of(2026, 2, 12)))
                .thenReturn(new UserMetricsResponse("url3", "exp3", Arrays.asList(metric3)));

        List<UnifiedUsageRecord> results = collector.collectUsageData(startDate, endDate);

        assertNotNull(results);
        assertEquals(3, results.size());

        assertEquals(LocalDate.of(2026, 2, 10), results.get(0).date());
        assertEquals(LocalDate.of(2026, 2, 11), results.get(1).date());
        assertEquals(LocalDate.of(2026, 2, 12), results.get(2).date());

        verify(apiClient, times(3)).fetchUserMetrics(any(LocalDate.class));
    }

    @Test
    void testCollectUsageData_EmptyResponse() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        UserMetricsResponse response = new UserMetricsResponse(
                "https://example.com/report",
                "2026-02-11T00:00:00Z",
                Collections.emptyList()
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(apiClient, times(1)).fetchUserMetrics(date);
    }

    @Test
    void testCollectUsageData_ApiClientException_ContinuesWithNextDate() throws ApiClientException {
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 12);

        UserMetric metric = new UserMetric(
                "john.doe",
                "john@example.com",
                "2026-02-12",
                50, 40, 30, 500, 400, 100
        );

        // Day 1: throws exception
        when(apiClient.fetchUserMetrics(LocalDate.of(2026, 2, 10)))
                .thenThrow(new ApiClientException("API Error"));

        // Day 2: returns empty
        when(apiClient.fetchUserMetrics(LocalDate.of(2026, 2, 11)))
                .thenReturn(new UserMetricsResponse("url", "exp", Collections.emptyList()));

        // Day 3: returns data
        when(apiClient.fetchUserMetrics(LocalDate.of(2026, 2, 12)))
                .thenReturn(new UserMetricsResponse("url", "exp", Arrays.asList(metric)));

        List<UnifiedUsageRecord> results = collector.collectUsageData(startDate, endDate);

        // Should continue despite error on day 1
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(LocalDate.of(2026, 2, 12), results.get(0).date());

        verify(apiClient, times(3)).fetchUserMetrics(any(LocalDate.class));
    }

    @Test
    void testAcceptanceRate_Calculation() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        // Test different acceptance rates
        UserMetric metric1 = new UserMetric(
                "user1", "user1@example.com", "2026-02-10",
                50, 40, 30, 1000, 800, 100 // 80% acceptance
        );

        UserMetric metric2 = new UserMetric(
                "user2", "user2@example.com", "2026-02-10",
                50, 40, 30, 500, 500, 100 // 100% acceptance
        );

        UserMetric metric3 = new UserMetric(
                "user3", "user3@example.com", "2026-02-10",
                50, 40, 30, 1000, 0, 100 // 0% acceptance
        );

        UserMetricsResponse response = new UserMetricsResponse(
                "url", "exp", Arrays.asList(metric1, metric2, metric3)
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertEquals(3, results.size());
        assertEquals(0.8, results.get(0).acceptanceRate(), 0.001);
        assertEquals(1.0, results.get(1).acceptanceRate(), 0.001);
        assertEquals(0.0, results.get(2).acceptanceRate(), 0.001);
    }

    @Test
    void testAcceptanceRate_ZeroSuggested_ReturnsNull() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        UserMetric metric = new UserMetric(
                "user1", "user1@example.com", "2026-02-10",
                50, 40, 30, 0, 100, 100 // 0 suggested
        );

        UserMetricsResponse response = new UserMetricsResponse(
                "url", "exp", Arrays.asList(metric)
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertEquals(1, results.size());
        assertNull(results.get(0).acceptanceRate());
    }

    @Test
    void testAcceptanceRate_NullValues_ReturnsNull() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        UserMetric metric = new UserMetric(
                "user1", "user1@example.com", "2026-02-10",
                50, 40, 30, null, null, 100
        );

        UserMetricsResponse response = new UserMetricsResponse(
                "url", "exp", Arrays.asList(metric)
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertEquals(1, results.size());
        assertNull(results.get(0).acceptanceRate());
        assertNull(results.get(0).linesSuggested());
        assertNull(results.get(0).linesAccepted());
    }

    @Test
    void testEmailNormalization_NullEmail() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        UserMetric metric = new UserMetric(
                "john.doe", null, "2026-02-10",
                50, 40, 30, 500, 400, 100
        );

        UserMetricsResponse response = new UserMetricsResponse(
                "url", "exp", Arrays.asList(metric)
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertEquals(1, results.size());
        assertEquals("[sem-usr-github]", results.get(0).email());
    }

    @Test
    void testEmailNormalization_EmptyEmail() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        UserMetric metric = new UserMetric(
                "john.doe", "  ", "2026-02-10",
                50, 40, 30, 500, 400, 100
        );

        UserMetricsResponse response = new UserMetricsResponse(
                "url", "exp", Arrays.asList(metric)
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertEquals(1, results.size());
        assertEquals("[sem-usr-github]", results.get(0).email());
    }

    @Test
    void testEmailNormalization_LowerCase() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        UserMetric metric = new UserMetric(
                "john.doe", "John@Example.COM", "2026-02-10",
                50, 40, 30, 500, 400, 100
        );

        UserMetricsResponse response = new UserMetricsResponse(
                "url", "exp", Arrays.asList(metric)
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertEquals(1, results.size());
        assertEquals("john@example.com", results.get(0).email());
    }

    @Test
    void testRawMetadata_ContainsGitHubSpecificFields() throws ApiClientException {
        LocalDate date = LocalDate.of(2026, 2, 10);

        UserMetric metric = new UserMetric(
                "john.doe",
                "john@example.com",
                "2026-02-10",
                50,
                40,
                30,
                500,
                400,
                100
        );

        UserMetricsResponse response = new UserMetricsResponse(
                "url", "exp", Arrays.asList(metric)
        );

        when(apiClient.fetchUserMetrics(date)).thenReturn(response);

        List<UnifiedUsageRecord> results = collector.collectUsageData(date, date);

        assertEquals(1, results.size());
        UnifiedUsageRecord record = results.get(0);

        assertNotNull(record.rawMetadata());
        assertEquals("john.doe", record.rawMetadata().get("user_name"));
        assertEquals("john@example.com", record.rawMetadata().get("user_email"));
        assertEquals("2026-02-10", record.rawMetadata().get("date"));
        assertEquals(50, record.rawMetadata().get("user_initiated_interaction_count"));
        assertEquals(40, record.rawMetadata().get("code_generation_activity_count"));
        assertEquals(30, record.rawMetadata().get("code_acceptance_activity_count"));
        assertEquals(100, record.rawMetadata().get("loc_deleted_sum"));
    }
}
