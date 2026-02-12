package com.bemobi.aicontrol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representing the response from GitHub Copilot Metrics API.
 *
 * <p>The API returns a signed URL that must be accessed to download the actual
 * metrics data in NDJSON format (one JSON object per line).</p>
 *
 * <p>Endpoint: GET /orgs/{org}/copilot/metrics/reports/users-1-day?date=YYYY-MM-DD</p>
 */
public record UserMetricsResponse(
        @JsonProperty("report_url") String reportUrl,
        @JsonProperty("expires_at") String expiresAt,
        List<UserMetric> data
) {
    /**
     * Constructor for creating response with parsed data.
     */
    public UserMetricsResponse(String reportUrl, String expiresAt, List<UserMetric> data) {
        this.reportUrl = reportUrl;
        this.expiresAt = expiresAt;
        this.data = data != null ? data : List.of();
    }

    /**
     * Constructor for initial API response (before NDJSON parsing).
     */
    public UserMetricsResponse(String reportUrl, String expiresAt) {
        this(reportUrl, expiresAt, List.of());
    }
}
