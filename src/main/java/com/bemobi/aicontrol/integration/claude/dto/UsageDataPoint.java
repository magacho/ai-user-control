package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * DTO representing a single data point in Claude API usage report.
 *
 * <p>Represents token usage data for a specific timestamp and API key/workspace.</p>
 */
public record UsageDataPoint(
        LocalDateTime timestamp,
        @JsonProperty("workspace_id") String workspaceId,
        @JsonProperty("api_key_id") String apiKeyId,
        String model,
        @JsonProperty("input_tokens") Long inputTokens,
        @JsonProperty("output_tokens") Long outputTokens,
        @JsonProperty("cache_creation_input_tokens") Long cacheCreationInputTokens,
        @JsonProperty("cache_read_input_tokens") Long cacheReadInputTokens
) {}
