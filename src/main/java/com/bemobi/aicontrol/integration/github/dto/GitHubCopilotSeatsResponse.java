package com.bemobi.aicontrol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for GitHub Copilot seats endpoint.
 */
public record GitHubCopilotSeatsResponse(
        @JsonProperty("total_seats") int totalSeats,
        List<GitHubCopilotSeat> seats
) {}
