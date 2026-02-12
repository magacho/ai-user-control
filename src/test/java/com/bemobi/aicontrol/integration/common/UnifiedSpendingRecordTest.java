package com.bemobi.aicontrol.integration.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedSpendingRecordTest {

    @Test
    void testConstructionWithDefaultCurrency() {
        UnifiedSpendingRecord record = new UnifiedSpendingRecord(
                "test@example.com",
                ToolType.CLAUDE,
                "2026-02",
                new BigDecimal("150.50"),
                null,  // currency should default to "USD"
                null   // rawMetadata
        );

        assertEquals("test@example.com", record.email());
        assertEquals(ToolType.CLAUDE, record.tool());
        assertEquals("2026-02", record.period());
        assertEquals(new BigDecimal("150.50"), record.costUsd());
        assertEquals("USD", record.currency());
        assertNotNull(record.rawMetadata());
        assertTrue(record.rawMetadata().isEmpty());
    }

    @Test
    void testConstructionWithExplicitCurrency() {
        UnifiedSpendingRecord record = new UnifiedSpendingRecord(
                "user@company.com",
                ToolType.CURSOR,
                "2026-02-12",
                new BigDecimal("75.25"),
                "BRL",
                Map.of("converted_from", "USD")
        );

        assertEquals("user@company.com", record.email());
        assertEquals(ToolType.CURSOR, record.tool());
        assertEquals("2026-02-12", record.period());
        assertEquals(new BigDecimal("75.25"), record.costUsd());
        assertEquals("BRL", record.currency());
        assertEquals(1, record.rawMetadata().size());
    }

    @Test
    void testBigDecimalPrecision() {
        BigDecimal cost = new BigDecimal("0.00123456789");
        UnifiedSpendingRecord record = new UnifiedSpendingRecord(
                "test@test.com",
                ToolType.GITHUB_COPILOT,
                "2026-02",
                cost,
                "USD",
                null
        );

        assertEquals(cost, record.costUsd());
        assertEquals(new BigDecimal("0.00123456789"), record.costUsd());
    }

    @Test
    void testRawMetadataImmutability() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoice_id", "INV-12345");

        UnifiedSpendingRecord record = new UnifiedSpendingRecord(
                "test@test.com",
                ToolType.CLAUDE,
                "2026-02",
                new BigDecimal("100.00"),
                "USD",
                metadata
        );

        // Attempt to modify original map should not affect record
        metadata.put("additional_field", "value");
        assertEquals(1, record.rawMetadata().size());
        assertFalse(record.rawMetadata().containsKey("additional_field"));

        // Attempt to modify record's map should throw exception
        assertThrows(UnsupportedOperationException.class, () ->
                record.rawMetadata().put("new_field", "value")
        );
    }

    @Test
    void testDefensiveCopyOfRawMetadata() {
        Map<String, Object> originalMetadata = new HashMap<>();
        originalMetadata.put("original_key", "original_value");

        UnifiedSpendingRecord record = new UnifiedSpendingRecord(
                "test@test.com",
                ToolType.CURSOR,
                "2026-02",
                new BigDecimal("50.00"),
                "USD",
                originalMetadata
        );

        // Modify original after construction
        originalMetadata.put("modified_key", "modified_value");

        // Record should not be affected
        assertEquals(1, record.rawMetadata().size());
        assertTrue(record.rawMetadata().containsKey("original_key"));
        assertFalse(record.rawMetadata().containsKey("modified_key"));
    }

    @Test
    void testClaudeSpendingPattern() {
        // Claude provides cost reports in cents, converted to USD
        UnifiedSpendingRecord record = new UnifiedSpendingRecord(
                "claude-workspace-id",
                ToolType.CLAUDE,
                "2026-02",
                new BigDecimal("125.50"),
                "USD",
                Map.of(
                        "original_cents", 12550,
                        "workspace_id", "ws-abc123"
                )
        );

        assertEquals(ToolType.CLAUDE, record.tool());
        assertEquals("USD", record.currency());
        assertNotNull(record.costUsd());
        assertEquals(2, record.rawMetadata().size());
    }

    @Test
    void testGitHubCopilotNoSpending() {
        // GitHub Copilot doesn't expose cost via API
        // This would typically result in empty list, but if created, cost would be zero or special value
        UnifiedSpendingRecord record = new UnifiedSpendingRecord(
                "developer@company.com",
                ToolType.GITHUB_COPILOT,
                "2026-02",
                BigDecimal.ZERO,
                "USD",
                Map.of("note", "GitHub Copilot does not expose cost data")
        );

        assertEquals(ToolType.GITHUB_COPILOT, record.tool());
        assertEquals(BigDecimal.ZERO, record.costUsd());
    }

    @Test
    void testCursorSpendingPattern() {
        // Cursor provides per-user spending data
        UnifiedSpendingRecord record = new UnifiedSpendingRecord(
                "cursor-user@company.com",
                ToolType.CURSOR,
                "2026-02-12",
                new BigDecimal("45.75"),
                "USD",
                Map.of(
                        "user_id", "user-xyz",
                        "subscription_type", "pro"
                )
        );

        assertEquals(ToolType.CURSOR, record.tool());
        assertEquals("cursor-user@company.com", record.email());
        assertEquals(new BigDecimal("45.75"), record.costUsd());
    }

    @Test
    void testPeriodFormats() {
        // Test daily format
        UnifiedSpendingRecord daily = new UnifiedSpendingRecord(
                "test@test.com",
                ToolType.CLAUDE,
                "2026-02-12",
                new BigDecimal("10.00"),
                "USD",
                null
        );
        assertEquals("2026-02-12", daily.period());

        // Test monthly format
        UnifiedSpendingRecord monthly = new UnifiedSpendingRecord(
                "test@test.com",
                ToolType.CLAUDE,
                "2026-02",
                new BigDecimal("300.00"),
                "USD",
                null
        );
        assertEquals("2026-02", monthly.period());
    }
}
