package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.common.UsageDataCollector;
import com.bemobi.aicontrol.integration.google.GoogleWorkspaceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedSpendingServiceTest {

    @Mock
    private UsageDataCollector claudeCollector;

    @Mock
    private UsageDataCollector githubCollector;

    @Mock
    private UsageDataCollector cursorCollector;

    @Mock
    private GoogleWorkspaceClient workspaceClient;

    @TempDir
    Path tempDir;

    private UnifiedSpendingService service;

    @BeforeEach
    void setUp() {
        List<UsageDataCollector> collectors = Arrays.asList(
            claudeCollector,
            githubCollector,
            cursorCollector
        );
        service = new UnifiedSpendingService(collectors, workspaceClient, tempDir.toString());

        // Setup default mock responses with lenient stubbing
        lenient().when(claudeCollector.getToolType()).thenReturn(ToolType.CLAUDE);
        lenient().when(githubCollector.getToolType()).thenReturn(ToolType.GITHUB_COPILOT);
        lenient().when(cursorCollector.getToolType()).thenReturn(ToolType.CURSOR);
    }

    @Test
    void testGenerateReportWithAllCollectors() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);

        // Claude data
        UnifiedUsageRecord claudeUsage = new UnifiedUsageRecord(
            "workspace-123",
            ToolType.CLAUDE,
            LocalDate.of(2026, 2, 5),
            5000L,
            2000L,
            1000L,
            null,
            null,
            null,
            Map.of()
        );

        UnifiedSpendingRecord claudeSpending = new UnifiedSpendingRecord(
            "workspace-123",
            ToolType.CLAUDE,
            "2026-02",
            BigDecimal.valueOf(1.25),
            "USD",
            Map.of()
        );

        when(claudeCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of(claudeUsage));
        when(claudeCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of(claudeSpending));

        // GitHub data
        UnifiedUsageRecord githubUsage = new UnifiedUsageRecord(
            "user@example.com",
            ToolType.GITHUB_COPILOT,
            LocalDate.of(2026, 2, 8),
            null,
            null,
            null,
            200,
            150,
            0.75,
            Map.of("gitHubLogin", "testuser")
        );

        when(githubCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of(githubUsage));
        when(githubCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        // Cursor data
        UnifiedUsageRecord cursorUsage = new UnifiedUsageRecord(
            "user@example.com",
            ToolType.CURSOR,
            LocalDate.of(2026, 2, 7),
            3000L,
            1500L,
            500L,
            180,
            160,
            0.889,
            Map.of()
        );

        UnifiedSpendingRecord cursorSpending = new UnifiedSpendingRecord(
            "user@example.com",
            ToolType.CURSOR,
            "2026-02",
            BigDecimal.valueOf(0.45),
            "USD",
            Map.of()
        );

        when(cursorCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of(cursorUsage));
        when(cursorCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of(cursorSpending));

        // Act
        ConsolidatedReport report = service.generateSpendingReport(startDate, endDate);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.usageRecords()).hasSize(3);
        assertThat(report.spendingRecords()).hasSize(2);

        ReportSummary summary = report.summary();
        assertThat(summary.totalCostUsd()).isEqualByComparingTo(BigDecimal.valueOf(1.70));
        assertThat(summary.totalInputTokens()).isEqualTo(8000L);
        assertThat(summary.totalOutputTokens()).isEqualTo(3500L);
        assertThat(summary.userCount()).isEqualTo(2);
        assertThat(summary.costByTool()).hasSize(2);
        assertThat(summary.costByTool().get(ToolType.CLAUDE))
            .isEqualByComparingTo(BigDecimal.valueOf(1.25));
        assertThat(summary.costByTool().get(ToolType.CURSOR))
            .isEqualByComparingTo(BigDecimal.valueOf(0.45));

        verify(claudeCollector).collectUsageData(startDate, endDate);
        verify(claudeCollector).collectSpendingData(startDate, endDate);
        verify(githubCollector).collectUsageData(startDate, endDate);
        verify(githubCollector).collectSpendingData(startDate, endDate);
        verify(cursorCollector).collectUsageData(startDate, endDate);
        verify(cursorCollector).collectSpendingData(startDate, endDate);
    }

    @Test
    void testGenerateReportWithPartialFailure() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);

        // Claude works
        UnifiedUsageRecord claudeUsage = new UnifiedUsageRecord(
            "workspace-123",
            ToolType.CLAUDE,
            LocalDate.of(2026, 2, 5),
            5000L,
            2000L,
            null,
            null,
            null,
            null,
            Map.of()
        );

        when(claudeCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of(claudeUsage));
        when(claudeCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        // GitHub fails
        when(githubCollector.collectUsageData(startDate, endDate))
            .thenThrow(new RuntimeException("GitHub API error"));
        when(githubCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        // Cursor works
        UnifiedUsageRecord cursorUsage = new UnifiedUsageRecord(
            "user@example.com",
            ToolType.CURSOR,
            LocalDate.of(2026, 2, 7),
            3000L,
            1500L,
            null,
            null,
            null,
            null,
            Map.of()
        );

        when(cursorCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of(cursorUsage));
        when(cursorCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        // Act
        ConsolidatedReport report = service.generateSpendingReport(startDate, endDate);

        // Assert - should continue with other collectors despite failure
        assertThat(report).isNotNull();
        assertThat(report.usageRecords()).hasSize(2); // Only Claude and Cursor
        assertThat(report.summary().userCount()).isEqualTo(2);
    }

    @Test
    void testGenerateReportEmptyPeriod() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);

        when(claudeCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(claudeCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        when(githubCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(githubCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        when(cursorCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(cursorCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        // Act
        ConsolidatedReport report = service.generateSpendingReport(startDate, endDate);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.usageRecords()).isEmpty();
        assertThat(report.spendingRecords()).isEmpty();
        assertThat(report.summary().totalCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.summary().userCount()).isEqualTo(0);
    }

    @Test
    void testCalculateSummaryCorrectly() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);

        List<UnifiedUsageRecord> usageRecords = Arrays.asList(
            new UnifiedUsageRecord(
                "user1@example.com",
                ToolType.CLAUDE,
                LocalDate.now(),
                1000L,
                500L,
                null,
                null,
                null,
                null,
                Map.of()
            ),
            new UnifiedUsageRecord(
                "user2@example.com",
                ToolType.CURSOR,
                LocalDate.now(),
                2000L,
                1000L,
                null,
                null,
                null,
                null,
                Map.of()
            ),
            new UnifiedUsageRecord(
                "user1@example.com",
                ToolType.CURSOR,
                LocalDate.now(),
                500L,
                250L,
                null,
                null,
                null,
                null,
                Map.of()
            )
        );

        List<UnifiedSpendingRecord> spendingRecords = Arrays.asList(
            new UnifiedSpendingRecord(
                "user1@example.com",
                ToolType.CLAUDE,
                "2026-02",
                BigDecimal.valueOf(10.00),
                "USD",
                Map.of()
            ),
            new UnifiedSpendingRecord(
                "user2@example.com",
                ToolType.CURSOR,
                "2026-02",
                BigDecimal.valueOf(5.50),
                "USD",
                Map.of()
            )
        );

        when(claudeCollector.collectUsageData(startDate, endDate))
            .thenReturn(usageRecords.subList(0, 1));
        when(claudeCollector.collectSpendingData(startDate, endDate))
            .thenReturn(spendingRecords.subList(0, 1));

        when(githubCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(githubCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        when(cursorCollector.collectUsageData(startDate, endDate))
            .thenReturn(usageRecords.subList(1, 3));
        when(cursorCollector.collectSpendingData(startDate, endDate))
            .thenReturn(spendingRecords.subList(1, 2));

        // Act
        ConsolidatedReport report = service.generateSpendingReport(startDate, endDate);

        // Assert
        ReportSummary summary = report.summary();
        assertThat(summary.totalCostUsd()).isEqualByComparingTo(BigDecimal.valueOf(15.50));
        assertThat(summary.totalInputTokens()).isEqualTo(3500L);
        assertThat(summary.totalOutputTokens()).isEqualTo(1750L);
        assertThat(summary.userCount()).isEqualTo(2); // user1 and user2
        assertThat(summary.costByTool()).hasSize(2);
    }

    @Test
    void testExportToXlsxBasicReport() throws IOException, ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);

        UnifiedUsageRecord usage = new UnifiedUsageRecord(
            "user@example.com",
            ToolType.CLAUDE,
            LocalDate.of(2026, 2, 5),
            1000L,
            500L,
            null,
            null,
            null,
            null,
            Map.of()
        );

        UnifiedSpendingRecord spending = new UnifiedSpendingRecord(
            "user@example.com",
            ToolType.CLAUDE,
            "2026-02",
            BigDecimal.valueOf(1.50),
            "USD",
            Map.of()
        );

        when(claudeCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of(usage));
        when(claudeCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of(spending));

        when(githubCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(githubCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        when(cursorCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(cursorCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        ConsolidatedReport report = service.generateSpendingReport(startDate, endDate);

        Path outputPath = tempDir.resolve("test-report.xlsx");

        // Act
        Path result = service.exportToXlsx(report, outputPath);

        // Assert
        assertThat(result).isEqualTo(outputPath);
        assertThat(Files.exists(outputPath)).isTrue();
        assertThat(Files.size(outputPath)).isGreaterThan(0);
    }

    @Test
    void testExportToXlsxWithNulls() throws IOException, ApiClientException {
        // Arrange - data with null fields
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);

        UnifiedUsageRecord usage = new UnifiedUsageRecord(
            "user@example.com",
            ToolType.GITHUB_COPILOT,
            LocalDate.of(2026, 2, 5),
            null, // No input tokens
            null, // No output tokens
            null,
            100,
            75,
            null,
            Map.of()
        );

        when(claudeCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(claudeCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        when(githubCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of(usage));
        when(githubCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of()); // No spending data

        when(cursorCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(cursorCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        ConsolidatedReport report = service.generateSpendingReport(startDate, endDate);

        Path outputPath = tempDir.resolve("test-nulls.xlsx");

        // Act
        Path result = service.exportToXlsx(report, outputPath);

        // Assert - should handle nulls gracefully
        assertThat(result).isEqualTo(outputPath);
        assertThat(Files.exists(outputPath)).isTrue();
        assertThat(Files.size(outputPath)).isGreaterThan(0);
    }

    @Test
    void testExportToXlsxFileMetadata() throws IOException, ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);

        when(claudeCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(claudeCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        when(githubCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(githubCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        when(cursorCollector.collectUsageData(startDate, endDate))
            .thenReturn(List.of());
        when(cursorCollector.collectSpendingData(startDate, endDate))
            .thenReturn(List.of());

        ConsolidatedReport report = service.generateSpendingReport(startDate, endDate);

        Path outputPath = tempDir.resolve("test-metadata.xlsx");

        // Act
        Path result = service.exportToXlsx(report, outputPath);

        // Assert
        assertThat(result).isEqualTo(outputPath);
        assertThat(Files.exists(outputPath)).isTrue();
        assertThat(Files.isRegularFile(outputPath)).isTrue();
        assertThat(outputPath.getFileName().toString()).endsWith(".xlsx");
    }
}
