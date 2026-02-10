package com.bemobi.aicontrol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a GitHub user.
 */
public record GitHubUser(
        String login,
        Long id,
        @JsonProperty("node_id") String nodeId,
        @JsonProperty("avatar_url") String avatarUrl,
        String type,
        @JsonProperty("site_admin") boolean siteAdmin,
        String name,
        String email,
        @JsonProperty("twitter_username") String twitterUsername,
        String blog
) {}
