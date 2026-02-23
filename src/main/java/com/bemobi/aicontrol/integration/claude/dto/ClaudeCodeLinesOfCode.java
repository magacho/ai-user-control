package com.bemobi.aicontrol.integration.claude.dto;

/**
 * DTO representing lines of code statistics.
 */
public record ClaudeCodeLinesOfCode(
        Integer added,
        Integer removed
) {}
