package com.bemobi.aicontrol.integration.github.dto;

/**
 * DTO representing a GitHub team.
 */
public class GitHubTeam {

    private String name;
    private String slug;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
