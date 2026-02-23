package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a single usage result within a time bucket.
 */
public record UsageResult(
        @JsonProperty("workspace_id") String workspaceId,
        @JsonProperty("api_key_id") String apiKeyId,
        String model,
        @JsonProperty("uncached_input_tokens") Long uncachedInputTokens,
        @JsonProperty("output_tokens") Long outputTokens,
        @JsonProperty("cache_read_input_tokens") Long cacheReadInputTokens,
        @JsonProperty("cache_creation") CacheCreation cacheCreation
) {}
