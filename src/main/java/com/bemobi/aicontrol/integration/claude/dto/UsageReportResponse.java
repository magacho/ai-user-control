package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for Claude API usage report endpoint.
 *
 * <p>Contains aggregated usage data for the organization across a time period.</p>
 */
public record UsageReportResponse(
        @JsonProperty("bucket_width") String bucketWidth,
        List<UsageDataPoint> data
) {}
