package com.bemobi.aicontrol.integration.cursor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a team member from Cursor Admin API.
 */
public record CursorTeamMember(
        String name,
        String email,
        String role,
        @JsonProperty("user_id") String userId
) {}
