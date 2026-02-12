package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * DTO representing a single data point in Claude API cost report.
 *
 * <p>Represents cost data for a specific timestamp and workspace.</p>
 * <p><b>Note:</b> Cost is in cents. Divide by 100 to get USD.</p>
 */
public record CostDataPoint(
        LocalDateTime timestamp,
        @JsonProperty("workspace_id") String workspaceId,
        Long cost,
        @JsonProperty("cost_type") String costType,
        String description
) {}
