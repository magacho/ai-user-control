package com.bemobi.aicontrol.integration.claude;

import com.bemobi.aicontrol.integration.claude.dto.CostDataPoint;
import com.bemobi.aicontrol.integration.claude.dto.CostReportResponse;
import com.bemobi.aicontrol.integration.claude.dto.UsageDataPoint;
import com.bemobi.aicontrol.integration.claude.dto.UsageReportResponse;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaudeUsageDataCollectorTest {

    @Mock
    private ClaudeApiClient claudeApiClient;

    private ClaudeUsageDataCollector collector;

    @BeforeEach
    void setUp() {
        collector = new ClaudeUsageDataCollector(claudeApiClient);
    }

    @Test
    void testGetToolType() {
        assertEquals(ToolType.CLAUDE, collector.getToolType());
    }

    @Test
    void testCollectUsageData_Success() throws ApiClientException {
        // Setup mock data
        LocalDateTime timestamp = LocalDateTime.now();
        UsageDataPoint dataPoint1 = new UsageDataPoint(
                timestamp,
                "workspace-123",
                "api-key-456",
                "claude-3-opus-20240229",
                1000L,
                500L,
                100L,
                200L
        );

        UsageDataPoint dataPoint2 = new UsageDataPoint(
                timestamp.plusHours(1),
                "workspace-123",
                null,
                "claude-3-sonnet-20240229",
                2000L,
                800L,
                150L,
                300L
        );

        UsageReportResponse response = new UsageReportResponse(
                "1d",
                Arrays.asList(dataPoint1, dataPoint2)
        );

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchUsageReport(startDate, endDate, "1d"))
                .thenReturn(response);

        // Execute
        List<UnifiedUsageRecord> records = collector.collectUsageData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertEquals(2, records.size());

        UnifiedUsageRecord record1 = records.get(0);
        assertEquals("workspace-123", record1.email());
        assertEquals(ToolType.CLAUDE, record1.tool());
        assertEquals(timestamp.toLocalDate(), record1.date());
        assertEquals(1000L, record1.inputTokens());
        assertEquals(500L, record1.outputTokens());
        assertEquals(200L, record1.cacheReadTokens());
        assertNull(record1.linesSuggested());
        assertNull(record1.linesAccepted());
        assertNull(record1.acceptanceRate());
        assertNotNull(record1.rawMetadata());
        assertEquals("workspace-123", record1.rawMetadata().get("workspace_id"));
        assertEquals("api-key-456", record1.rawMetadata().get("api_key_id"));
        assertEquals("claude-3-opus-20240229", record1.rawMetadata().get("model"));

        UnifiedUsageRecord record2 = records.get(1);
        assertEquals("workspace-123", record2.email());
        assertEquals(2000L, record2.inputTokens());
        assertEquals(800L, record2.outputTokens());
    }

    @Test
    void testCollectUsageData_EmptyResponse() throws ApiClientException {
        // Setup mock empty data
        UsageReportResponse response = new UsageReportResponse("1d", List.of());

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchUsageReport(startDate, endDate, "1d"))
                .thenReturn(response);

        // Execute
        List<UnifiedUsageRecord> records = collector.collectUsageData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    void testCollectUsageData_NullData() throws ApiClientException {
        // Setup mock null data
        UsageReportResponse response = new UsageReportResponse("1d", null);

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchUsageReport(startDate, endDate, "1d"))
                .thenReturn(response);

        // Execute
        List<UnifiedUsageRecord> records = collector.collectUsageData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    void testCollectUsageData_ApiKeyIdFallback() throws ApiClientException {
        // Setup mock data with only apiKeyId (no workspaceId)
        LocalDateTime timestamp = LocalDateTime.now();
        UsageDataPoint dataPoint = new UsageDataPoint(
                timestamp,
                null,
                "api-key-789",
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

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchUsageReport(startDate, endDate, "1d"))
                .thenReturn(response);

        // Execute
        List<UnifiedUsageRecord> records = collector.collectUsageData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("api-key-789", records.get(0).email());
    }

    @Test
    void testCollectSpendingData_Success() throws ApiClientException {
        // Setup mock data
        LocalDateTime timestamp = LocalDateTime.now();
        CostDataPoint dataPoint1 = new CostDataPoint(
                timestamp,
                "workspace-123",
                5000L,
                "api_usage",
                "API usage costs"
        );

        CostDataPoint dataPoint2 = new CostDataPoint(
                timestamp.plusDays(1),
                "workspace-456",
                3500L,
                "api_usage",
                "API usage costs"
        );

        CostReportResponse response = new CostReportResponse(
                Arrays.asList(dataPoint1, dataPoint2)
        );

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchCostReport(startDate, endDate))
                .thenReturn(response);

        // Execute
        List<UnifiedSpendingRecord> records = collector.collectSpendingData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertEquals(2, records.size());

        UnifiedSpendingRecord record1 = records.get(0);
        assertEquals("workspace-123", record1.email());
        assertEquals(ToolType.CLAUDE, record1.tool());
        assertEquals(timestamp.toLocalDate().toString(), record1.period());
        assertEquals(new BigDecimal("50.0000"), record1.costUsd());
        assertEquals("USD", record1.currency());
        assertNotNull(record1.rawMetadata());
        assertEquals("workspace-123", record1.rawMetadata().get("workspace_id"));
        assertEquals("api_usage", record1.rawMetadata().get("cost_type"));
        assertEquals("API usage costs", record1.rawMetadata().get("description"));

        UnifiedSpendingRecord record2 = records.get(1);
        assertEquals("workspace-456", record2.email());
        assertEquals(new BigDecimal("35.0000"), record2.costUsd());
    }

    @Test
    void testCollectSpendingData_EmptyResponse() throws ApiClientException {
        // Setup mock empty data
        CostReportResponse response = new CostReportResponse(List.of());

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchCostReport(startDate, endDate))
                .thenReturn(response);

        // Execute
        List<UnifiedSpendingRecord> records = collector.collectSpendingData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    void testCollectSpendingData_NullData() throws ApiClientException {
        // Setup mock null data
        CostReportResponse response = new CostReportResponse(null);

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchCostReport(startDate, endDate))
                .thenReturn(response);

        // Execute
        List<UnifiedSpendingRecord> records = collector.collectSpendingData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    void testCollectSpendingData_NullWorkspaceId() throws ApiClientException {
        // Setup mock data with null workspaceId
        LocalDateTime timestamp = LocalDateTime.now();
        CostDataPoint dataPoint = new CostDataPoint(
                timestamp,
                null,
                5000L,
                "api_usage",
                "API usage costs"
        );

        CostReportResponse response = new CostReportResponse(
                Arrays.asList(dataPoint)
        );

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchCostReport(startDate, endDate))
                .thenReturn(response);

        // Execute
        List<UnifiedSpendingRecord> records = collector.collectSpendingData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("unknown", records.get(0).email());
    }

    @Test
    void testCollectSpendingData_NullCost() throws ApiClientException {
        // Setup mock data with null cost
        LocalDateTime timestamp = LocalDateTime.now();
        CostDataPoint dataPoint = new CostDataPoint(
                timestamp,
                "workspace-123",
                null,
                "api_usage",
                "API usage costs"
        );

        CostReportResponse response = new CostReportResponse(
                Arrays.asList(dataPoint)
        );

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchCostReport(startDate, endDate))
                .thenReturn(response);

        // Execute
        List<UnifiedSpendingRecord> records = collector.collectSpendingData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals(BigDecimal.ZERO, records.get(0).costUsd());
    }

    @Test
    void testCollectSpendingData_CostConversion() throws ApiClientException {
        // Test various cost conversions from cents to USD
        LocalDateTime timestamp = LocalDateTime.now();

        // Test case: 1234 cents = 12.34 USD
        CostDataPoint dataPoint = new CostDataPoint(
                timestamp,
                "workspace-123",
                1234L,
                "api_usage",
                "Test"
        );

        CostReportResponse response = new CostReportResponse(
                Arrays.asList(dataPoint)
        );

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(claudeApiClient.fetchCostReport(startDate, endDate))
                .thenReturn(response);

        // Execute
        List<UnifiedSpendingRecord> records = collector.collectSpendingData(startDate, endDate);

        // Verify
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals(new BigDecimal("12.3400"), records.get(0).costUsd());
    }
}
