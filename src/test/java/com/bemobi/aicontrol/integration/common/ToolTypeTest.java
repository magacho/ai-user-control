package com.bemobi.aicontrol.integration.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolTypeTest {

    @Test
    void testClaudeGetId() {
        assertEquals("claude-code", ToolType.CLAUDE.getId());
    }

    @Test
    void testClaudeGetDisplayName() {
        assertEquals("Claude Code", ToolType.CLAUDE.getDisplayName());
    }

    @Test
    void testGithubCopilotGetId() {
        assertEquals("github-copilot", ToolType.GITHUB_COPILOT.getId());
    }

    @Test
    void testGithubCopilotGetDisplayName() {
        assertEquals("GitHub Copilot", ToolType.GITHUB_COPILOT.getDisplayName());
    }

    @Test
    void testCursorGetId() {
        assertEquals("cursor", ToolType.CURSOR.getId());
    }

    @Test
    void testCursorGetDisplayName() {
        assertEquals("Cursor", ToolType.CURSOR.getDisplayName());
    }

    @Test
    void testValueOfClaude() {
        assertEquals(ToolType.CLAUDE, ToolType.valueOf("CLAUDE"));
    }

    @Test
    void testValueOfGithubCopilot() {
        assertEquals(ToolType.GITHUB_COPILOT, ToolType.valueOf("GITHUB_COPILOT"));
    }

    @Test
    void testValueOfCursor() {
        assertEquals(ToolType.CURSOR, ToolType.valueOf("CURSOR"));
    }

    @Test
    void testAllValuesPresent() {
        ToolType[] values = ToolType.values();
        assertEquals(3, values.length);
        assertNotNull(values[0]);
        assertNotNull(values[1]);
        assertNotNull(values[2]);
    }
}
