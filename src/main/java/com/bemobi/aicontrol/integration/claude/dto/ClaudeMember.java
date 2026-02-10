package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * DTO representing a member from Claude API.
 */
public record ClaudeMember(
        String id,
        String object,
        String email,
        String name,
        String role,
        String status,
        @JsonProperty("joined_at") LocalDateTime joinedAt,
        @JsonProperty("last_active_at") LocalDateTime lastActiveAt
) {}
