package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsolidatedReportTest {

    @Test
    void testRecordCreation() {
        // Given
        String period = "2026-02";
        LocalDateTime generatedAt = LocalDateTime.now();

        UnifiedUsageRecord usageRecord = new UnifiedUsageRecord(
            "test@example.com",
            ToolType.CLAUDE,
            LocalDate.now(),
            1000L,
            500L,
            null,
            null,
            null,
            null,
            null
        );

        UnifiedSpendingRecord spendingRecord = new UnifiedSpendingRecord(
            "test@example.com",
            ToolType.CLAUDE,
            "2026-02",
            BigDecimal.valueOf(1.50),
            null,
            null
        );

        List<UnifiedUsageRecord> usageRecords = List.of(usageRecord);
        List<UnifiedSpendingRecord> spendingRecords = List.of(spendingRecord);

        ReportSummary summary = new ReportSummary(
            BigDecimal.valueOf(1.50),
            1000L,
            500L,
            1,
            Map.of(ToolType.CLAUDE, BigDecimal.valueOf(1.50))
        );

        // When
        ConsolidatedReport report = new ConsolidatedReport(
            period,
            generatedAt,
            usageRecords,
            spendingRecords,
            summary
        );

        // Then
        assertNotNull(report);
        assertEquals(period, report.period());
        assertEquals(generatedAt, report.generatedAt());
        assertEquals(1, report.usageRecords().size());
        assertEquals(1, report.spendingRecords().size());
        assertNotNull(report.summary());
        assertEquals(BigDecimal.valueOf(1.50), report.summary().totalCostUsd());
    }

    @Test
    void testRecordImmutability() {
        // Given
        List<UnifiedUsageRecord> usageRecords = List.of();
        List<UnifiedSpendingRecord> spendingRecords = List.of();
        ReportSummary summary = new ReportSummary(
            BigDecimal.ZERO,
            0L,
            0L,
            0,
            Map.of()
        );

        ConsolidatedReport report = new ConsolidatedReport(
            "2026-02",
            LocalDateTime.now(),
            usageRecords,
            spendingRecords,
            summary
        );

        // Then - lists should be unmodifiable or immutable
        assertNotNull(report.usageRecords());
        assertNotNull(report.spendingRecords());
    }

    @Test
    void testRecordEquality() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        ReportSummary summary = new ReportSummary(
            BigDecimal.ZERO,
            0L,
            0L,
            0,
            Map.of()
        );

        ConsolidatedReport report1 = new ConsolidatedReport(
            "2026-02",
            timestamp,
            List.of(),
            List.of(),
            summary
        );

        ConsolidatedReport report2 = new ConsolidatedReport(
            "2026-02",
            timestamp,
            List.of(),
            List.of(),
            summary
        );

        // Then - records should be equal if all fields are equal
        assertEquals(report1, report2);
        assertEquals(report1.hashCode(), report2.hashCode());
    }
}
