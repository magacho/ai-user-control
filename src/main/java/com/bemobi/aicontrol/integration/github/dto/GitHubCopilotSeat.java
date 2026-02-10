package com.bemobi.aicontrol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * DTO representing a Copilot seat from GitHub API.
 */
public record GitHubCopilotSeat(
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("pending_cancellation_date") String pendingCancellationDate,
        @JsonProperty("last_activity_at") OffsetDateTime lastActivityAt,
        @JsonProperty("last_activity_editor") String lastActivityEditor,
        GitHubUser assignee,
        @JsonProperty("assigning_team") GitHubTeam assigningTeam
) {}
