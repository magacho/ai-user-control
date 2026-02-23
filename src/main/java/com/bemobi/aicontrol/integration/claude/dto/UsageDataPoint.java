package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representing a time bucket in Claude API usage report (new format).
 *
 * <p>Each bucket contains a time range and a list of usage results grouped by the specified dimensions.</p>
 */
public record UsageDataPoint(
        @JsonProperty("starting_at") String startingAt,
        @JsonProperty("ending_at") String endingAt,
        List<UsageResult> results
) {}
