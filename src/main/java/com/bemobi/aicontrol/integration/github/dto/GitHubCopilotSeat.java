package com.bemobi.aicontrol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * DTO representing a Copilot seat from GitHub API.
 */
public class GitHubCopilotSeat {

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("pending_cancellation_date")
    private String pendingCancellationDate;

    @JsonProperty("last_activity_at")
    private OffsetDateTime lastActivityAt;

    @JsonProperty("last_activity_editor")
    private String lastActivityEditor;

    private GitHubUser assignee;

    @JsonProperty("assigning_team")
    private GitHubTeam assigningTeam;

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPendingCancellationDate() {
        return pendingCancellationDate;
    }

    public void setPendingCancellationDate(String pendingCancellationDate) {
        this.pendingCancellationDate = pendingCancellationDate;
    }

    public OffsetDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public String getLastActivityEditor() {
        return lastActivityEditor;
    }

    public void setLastActivityEditor(String lastActivityEditor) {
        this.lastActivityEditor = lastActivityEditor;
    }

    public GitHubUser getAssignee() {
        return assignee;
    }

    public void setAssignee(GitHubUser assignee) {
        this.assignee = assignee;
    }

    public GitHubTeam getAssigningTeam() {
        return assigningTeam;
    }

    public void setAssigningTeam(GitHubTeam assigningTeam) {
        this.assigningTeam = assigningTeam;
    }
}
