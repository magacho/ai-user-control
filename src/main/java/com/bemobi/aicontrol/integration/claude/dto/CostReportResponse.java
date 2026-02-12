package com.bemobi.aicontrol.integration.claude.dto;

import java.util.List;

/**
 * Response DTO for Claude API cost report endpoint.
 *
 * <p>Contains cost data for the organization across a time period.</p>
 */
public record CostReportResponse(
        List<CostDataPoint> data
) {}
