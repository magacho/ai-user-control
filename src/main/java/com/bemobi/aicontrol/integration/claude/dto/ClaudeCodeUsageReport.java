package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for Claude Code usage report endpoint.
 *
 * <p>Contains per-user usage data for Claude Code with email addresses.</p>
 */
public record ClaudeCodeUsageReport(
        List<ClaudeCodeRecord> data,
        @JsonProperty("has_more") Boolean hasMore,
        @JsonProperty("next_page") String nextPage
) {}
