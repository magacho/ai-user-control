package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for cache creation tokens breakdown.
 */
public record CacheCreation(
        @JsonProperty("ephemeral_5m_input_tokens") Long ephemeral5mInputTokens,
        @JsonProperty("ephemeral_1h_input_tokens") Long ephemeral1hInputTokens
) {}
