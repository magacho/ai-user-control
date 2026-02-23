package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a single cost result within a time bucket.
 *
 * <p><b>Note:</b> amount is a decimal string in cents (e.g., "123.45" = $1.23)</p>
 */
public record CostResult(
        String amount,
        String currency,
        @JsonProperty("workspace_id") String workspaceId,
        @JsonProperty("cost_type") String costType,
        String description,
        String model,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("service_tier") String serviceTier,
        @JsonProperty("context_window") String contextWindow,
        @JsonProperty("inference_geo") String inferenceGeo,
        String speed
) {}
