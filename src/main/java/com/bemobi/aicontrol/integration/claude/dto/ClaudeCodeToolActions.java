package com.bemobi.aicontrol.integration.claude.dto;

/**
 * DTO representing tool action statistics.
 */
public record ClaudeCodeToolActions(
        Integer accepted,
        Integer rejected
) {}
