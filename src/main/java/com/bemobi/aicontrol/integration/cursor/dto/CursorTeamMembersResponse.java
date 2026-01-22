package com.bemobi.aicontrol.integration.cursor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for Cursor Admin API team members endpoint.
 */
public class CursorTeamMembersResponse {

    @JsonProperty("teamMembers")
    private List<CursorTeamMember> teamMembers;

    public List<CursorTeamMember> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<CursorTeamMember> teamMembers) {
        this.teamMembers = teamMembers;
    }
}
