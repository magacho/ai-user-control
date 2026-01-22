package com.bemobi.aicontrol.integration.github;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for GitHub Copilot API integration.
 */
@ConfigurationProperties(prefix = "ai-control.api.github")
@Validated
public class GitHubApiProperties {

    private boolean enabled = true;

    @NotBlank(message = "GitHub API base URL is required")
    private String baseUrl;

    @NotBlank(message = "GitHub API token is required")
    private String token;

    @NotBlank(message = "GitHub organization is required")
    private String organization;

    private int timeout = 30000;

    private int retryAttempts = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
}
