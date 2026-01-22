package com.bemobi.aicontrol.integration.cursor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a team member from Cursor Admin API.
 */
public class CursorTeamMember {

    private String name;
    private String email;
    private String role;

    @JsonProperty("user_id")
    private String userId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
