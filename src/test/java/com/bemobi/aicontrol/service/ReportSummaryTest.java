package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.ToolType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportSummaryTest {

    @Test
    void testSummaryCreation() {
        // Given
        BigDecimal totalCost = BigDecimal.valueOf(10.50);
        Long inputTokens = 5000L;
        Long outputTokens = 2000L;
        Integer userCount = 3;
        Map<ToolType, BigDecimal> costByTool = Map.of(
            ToolType.CLAUDE, BigDecimal.valueOf(5.00),
            ToolType.CURSOR, BigDecimal.valueOf(3.50),
            ToolType.GITHUB_COPILOT, BigDecimal.valueOf(2.00)
        );

        // When
        ReportSummary summary = new ReportSummary(
            totalCost,
            inputTokens,
            outputTokens,
            userCount,
            costByTool
        );

        // Then
        assertNotNull(summary);
        assertEquals(totalCost, summary.totalCostUsd());
        assertEquals(inputTokens, summary.totalInputTokens());
        assertEquals(outputTokens, summary.totalOutputTokens());
        assertEquals(userCount, summary.userCount());
        assertEquals(3, summary.costByTool().size());
        assertEquals(BigDecimal.valueOf(5.00), summary.costByTool().get(ToolType.CLAUDE));
    }

    @Test
    void testSummaryWithZeroValues() {
        // Given
        ReportSummary summary = new ReportSummary(
            BigDecimal.ZERO,
            0L,
            0L,
            0,
            Map.of()
        );

        // Then
        assertEquals(BigDecimal.ZERO, summary.totalCostUsd());
        assertEquals(0L, summary.totalInputTokens());
        assertEquals(0L, summary.totalOutputTokens());
        assertEquals(0, summary.userCount());
        assertTrue(summary.costByTool().isEmpty());
    }

    @Test
    void testSummaryWithNullTokens() {
        // Given - some collectors might not provide token data
        ReportSummary summary = new ReportSummary(
            BigDecimal.valueOf(5.00),
            null,
            null,
            1,
            Map.of(ToolType.GITHUB_COPILOT, BigDecimal.valueOf(5.00))
        );

        // Then
        assertNull(summary.totalInputTokens());
        assertNull(summary.totalOutputTokens());
        assertEquals(BigDecimal.valueOf(5.00), summary.totalCostUsd());
    }

    @Test
    void testCostByToolMapStructure() {
        // Given
        Map<ToolType, BigDecimal> costByTool = new HashMap<>();
        costByTool.put(ToolType.CLAUDE, BigDecimal.valueOf(10.00));
        costByTool.put(ToolType.CURSOR, BigDecimal.valueOf(5.50));

        ReportSummary summary = new ReportSummary(
            BigDecimal.valueOf(15.50),
            1000L,
            500L,
            2,
            costByTool
        );

        // Then
        assertEquals(2, summary.costByTool().size());
        assertTrue(summary.costByTool().containsKey(ToolType.CLAUDE));
        assertTrue(summary.costByTool().containsKey(ToolType.CURSOR));
        assertFalse(summary.costByTool().containsKey(ToolType.GITHUB_COPILOT));
    }

    @Test
    void testRecordEquality() {
        // Given
        Map<ToolType, BigDecimal> costMap = Map.of(ToolType.CLAUDE, BigDecimal.ONE);

        ReportSummary summary1 = new ReportSummary(
            BigDecimal.ONE,
            100L,
            50L,
            1,
            costMap
        );

        ReportSummary summary2 = new ReportSummary(
            BigDecimal.ONE,
            100L,
            50L,
            1,
            costMap
        );

        // Then
        assertEquals(summary1, summary2);
        assertEquals(summary1.hashCode(), summary2.hashCode());
    }
}
