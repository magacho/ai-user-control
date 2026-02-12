package com.bemobi.aicontrol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a single user's metrics from GitHub Copilot Metrics API.
 *
 * <p>This data comes from the NDJSON response of the Metrics API endpoint:
 * GET /orgs/{org}/copilot/metrics/reports/users-1-day</p>
 */
public record UserMetric(
        @JsonProperty("user_name") String userName,
        @JsonProperty("user_email") String userEmail,
        @JsonProperty("date") String date,
        @JsonProperty("user_initiated_interaction_count") Integer userInitiatedInteractionCount,
        @JsonProperty("code_generation_activity_count") Integer codeGenerationActivityCount,
        @JsonProperty("code_acceptance_activity_count") Integer codeAcceptanceActivityCount,
        @JsonProperty("loc_suggested_to_add_sum") Integer locSuggestedToAddSum,
        @JsonProperty("loc_added_sum") Integer locAddedSum,
        @JsonProperty("loc_deleted_sum") Integer locDeletedSum
) {}
