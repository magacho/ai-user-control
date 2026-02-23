package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing token usage and cost breakdown by model.
 */
public record ClaudeCodeModelBreakdown(
        @JsonProperty("estimated_cost") ClaudeCodeEstimatedCost estimatedCost,
        String model,
        ClaudeCodeTokens tokens
) {}
