package com.bemobi.aicontrol.integration.cursor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for Cursor Admin API team members endpoint.
 */
public record CursorTeamMembersResponse(
        @JsonProperty("teamMembers") List<CursorTeamMember> teamMembers
) {}
