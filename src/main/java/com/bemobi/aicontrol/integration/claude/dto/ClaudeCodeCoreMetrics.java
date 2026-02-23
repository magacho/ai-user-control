package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing core productivity metrics for Claude Code.
 */
public record ClaudeCodeCoreMetrics(
        @JsonProperty("commits_by_claude_code") Integer commitsByClaudeCode,
        @JsonProperty("lines_of_code") ClaudeCodeLinesOfCode linesOfCode,
        @JsonProperty("num_sessions") Integer numSessions,
        @JsonProperty("pull_requests_by_claude_code") Integer pullRequestsByClaudeCode
) {}
