package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for Claude API members list endpoint.
 */
public record ClaudeMembersResponse(
        String object,
        List<ClaudeMember> data,
        @JsonProperty("has_more") boolean hasMore,
        @JsonProperty("first_id") String firstId,
        @JsonProperty("last_id") String lastId
) {}
