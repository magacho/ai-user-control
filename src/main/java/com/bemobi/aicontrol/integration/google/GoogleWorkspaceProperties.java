package com.bemobi.aicontrol.integration.google;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Google Workspace integration.
 *
 * Used to resolve corporate email addresses from GitHub logins
 * via a custom schema field in Google Workspace user profiles.
 */
@ConfigurationProperties(prefix = "ai-control.api.google-workspace")
@Validated
public class GoogleWorkspaceProperties {

    private boolean enabled = false;

    private String credentials;

    private String domain;

    private String adminEmail;

    private String customSchema = "custom";

    private String gitNameField = "git_name";

    private int timeout = 30000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getCustomSchema() {
        return customSchema;
    }

    public void setCustomSchema(String customSchema) {
        this.customSchema = customSchema;
    }

    public String getGitNameField() {
        return gitNameField;
    }

    public void setGitNameField(String gitNameField) {
        this.gitNameField = gitNameField;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
