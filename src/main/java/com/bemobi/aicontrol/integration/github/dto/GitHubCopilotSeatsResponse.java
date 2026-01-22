package com.bemobi.aicontrol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for GitHub Copilot seats endpoint.
 */
public class GitHubCopilotSeatsResponse {

    @JsonProperty("total_seats")
    private int totalSeats;

    private List<GitHubCopilotSeat> seats;

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public List<GitHubCopilotSeat> getSeats() {
        return seats;
    }

    public void setSeats(List<GitHubCopilotSeat> seats) {
        this.seats = seats;
    }
}
