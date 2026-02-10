package com.bemobi.aicontrol.integration.github.dto;

/**
 * DTO representing a GitHub team.
 */
public record GitHubTeam(
        String name,
        String slug
) {}
