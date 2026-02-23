package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the actor (user or API key) in Claude Code usage.
 */
public record ClaudeCodeActor(
        @JsonProperty("email_address") String emailAddress,
        @JsonProperty("api_key_name") String apiKeyName,
        String type
) {}
