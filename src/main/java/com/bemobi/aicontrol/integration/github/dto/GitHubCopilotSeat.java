package com.bemobi.aicontrol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * DTO representing a Copilot seat from GitHub API.
 */
public class GitHubCopilotSeat {

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("pending_cancellation_date")
    private LocalDateTime pendingCancellationDate;

    @JsonProperty("last_activity_at")
    private LocalDateTime lastActivityAt;

    @JsonProperty("last_activity_editor")
    private String lastActivityEditor;

    private GitHubUser assignee;

    @JsonProperty("assigning_team")
    private GitHubTeam assigningTeam;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getPendingCancellationDate() {
        return pendingCancellationDate;
    }

    public void setPendingCancellationDate(LocalDateTime pendingCancellationDate) {
        this.pendingCancellationDate = pendingCancellationDate;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
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
