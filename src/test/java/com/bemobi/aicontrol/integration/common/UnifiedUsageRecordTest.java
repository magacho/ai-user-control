package com.bemobi.aicontrol.integration.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedUsageRecordTest {

    @Test
    void testConstructionWithAllFieldsNull() {
        UnifiedUsageRecord record = new UnifiedUsageRecord(
                "test@example.com",
                ToolType.CLAUDE,
                LocalDate.of(2026, 2, 12),
                null, // inputTokens
                null, // outputTokens
                null, // cacheReadTokens
                null, // linesSuggested
                null, // linesAccepted
                null, // acceptanceRate
                null  // rawMetadata
        );

        assertEquals("test@example.com", record.email());
        assertEquals(ToolType.CLAUDE, record.tool());
        assertEquals(LocalDate.of(2026, 2, 12), record.date());
        assertNull(record.inputTokens());
        assertNull(record.outputTokens());
        assertNull(record.cacheReadTokens());
        assertNull(record.linesSuggested());
        assertNull(record.linesAccepted());
        assertNull(record.acceptanceRate());
        assertNotNull(record.rawMetadata());
        assertTrue(record.rawMetadata().isEmpty());
    }

    @Test
    void testConstructionWithAllFieldsPopulated() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", "claude-sonnet-4.5");
        metadata.put("requestId", "req-12345");

        UnifiedUsageRecord record = new UnifiedUsageRecord(
                "user@company.com",
                ToolType.CURSOR,
                LocalDate.of(2026, 2, 10),
                1000L,  // inputTokens
                500L,   // outputTokens
                200L,   // cacheReadTokens
                150,    // linesSuggested
                120,    // linesAccepted
                0.8,    // acceptanceRate
                metadata
        );

        assertEquals("user@company.com", record.email());
        assertEquals(ToolType.CURSOR, record.tool());
        assertEquals(LocalDate.of(2026, 2, 10), record.date());
        assertEquals(1000L, record.inputTokens());
        assertEquals(500L, record.outputTokens());
        assertEquals(200L, record.cacheReadTokens());
        assertEquals(150, record.linesSuggested());
        assertEquals(120, record.linesAccepted());
        assertEquals(0.8, record.acceptanceRate());
        assertEquals(2, record.rawMetadata().size());
        assertEquals("claude-sonnet-4.5", record.rawMetadata().get("model"));
        assertEquals("req-12345", record.rawMetadata().get("requestId"));
    }

    @Test
    void testRawMetadataImmutability() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        UnifiedUsageRecord record = new UnifiedUsageRecord(
                "test@test.com",
                ToolType.GITHUB_COPILOT,
                LocalDate.now(),
                null, null, null, 100, 80, 0.8,
                metadata
        );

        // Attempt to modify original map should not affect record
        metadata.put("key2", "value2");
        assertEquals(1, record.rawMetadata().size());
        assertFalse(record.rawMetadata().containsKey("key2"));

        // Attempt to modify record's map should throw exception
        assertThrows(UnsupportedOperationException.class, () ->
                record.rawMetadata().put("key3", "value3")
        );
    }

    @Test
    void testDefensiveCopyOfRawMetadata() {
        Map<String, Object> originalMetadata = new HashMap<>();
        originalMetadata.put("original", "data");

        UnifiedUsageRecord record = new UnifiedUsageRecord(
                "test@test.com",
                ToolType.CLAUDE,
                LocalDate.now(),
                100L, 50L, null, null, null, null,
                originalMetadata
        );

        // Modify original after construction
        originalMetadata.put("modified", "after");

        // Record should not be affected
        assertEquals(1, record.rawMetadata().size());
        assertTrue(record.rawMetadata().containsKey("original"));
        assertFalse(record.rawMetadata().containsKey("modified"));
    }

    @Test
    void testClaudeUsagePattern() {
        // Claude typically has tokens but not lines
        UnifiedUsageRecord record = new UnifiedUsageRecord(
                "claude-workspace-id",
                ToolType.CLAUDE,
                LocalDate.of(2026, 2, 12),
                5000L,  // inputTokens
                2000L,  // outputTokens
                1000L,  // cacheReadTokens
                null,   // linesSuggested
                null,   // linesAccepted
                null,   // acceptanceRate
                Map.of("model", "claude-3-opus")
        );

        assertNotNull(record.inputTokens());
        assertNotNull(record.outputTokens());
        assertNotNull(record.cacheReadTokens());
        assertNull(record.linesSuggested());
        assertNull(record.linesAccepted());
        assertNull(record.acceptanceRate());
    }

    @Test
    void testGitHubCopilotUsagePattern() {
        // GitHub Copilot typically has lines but not tokens
        UnifiedUsageRecord record = new UnifiedUsageRecord(
                "developer@company.com",
                ToolType.GITHUB_COPILOT,
                LocalDate.of(2026, 2, 12),
                null,   // inputTokens
                null,   // outputTokens
                null,   // cacheReadTokens
                200,    // linesSuggested
                150,    // linesAccepted
                0.75,   // acceptanceRate
                Map.of("language", "java")
        );

        assertNull(record.inputTokens());
        assertNull(record.outputTokens());
        assertNull(record.cacheReadTokens());
        assertNotNull(record.linesSuggested());
        assertNotNull(record.linesAccepted());
        assertNotNull(record.acceptanceRate());
    }

    @Test
    void testCursorCompleteUsagePattern() {
        // Cursor exposes all metrics
        UnifiedUsageRecord record = new UnifiedUsageRecord(
                "cursor-user@company.com",
                ToolType.CURSOR,
                LocalDate.of(2026, 2, 12),
                3000L,  // inputTokens
                1500L,  // outputTokens
                500L,   // cacheReadTokens
                180,    // linesSuggested
                160,    // linesAccepted
                0.89,   // acceptanceRate
                Map.of("sessionId", "sess-xyz")
        );

        assertNotNull(record.inputTokens());
        assertNotNull(record.outputTokens());
        assertNotNull(record.cacheReadTokens());
        assertNotNull(record.linesSuggested());
        assertNotNull(record.linesAccepted());
        assertNotNull(record.acceptanceRate());
    }
}
