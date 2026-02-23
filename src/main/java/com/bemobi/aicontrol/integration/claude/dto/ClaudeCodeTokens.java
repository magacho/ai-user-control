package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing token usage breakdown.
 */
public record ClaudeCodeTokens(
        Integer input,
        Integer output,
        @JsonProperty("cache_creation") Integer cacheCreation,
        @JsonProperty("cache_read") Integer cacheRead
) {}
