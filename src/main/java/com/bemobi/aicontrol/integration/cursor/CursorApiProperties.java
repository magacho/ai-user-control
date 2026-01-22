package com.bemobi.aicontrol.integration.cursor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Cursor (Admin API) integration.
 *
 * Requires an Admin API key from Cursor team settings.
 */
@ConfigurationProperties(prefix = "ai-control.api.cursor")
@Validated
public class CursorApiProperties {

    private boolean enabled = false;

    private String baseUrl;

    private String token;

    private int timeout = 30000;

    private int retryAttempts = 3;

    /**
     * @deprecated CSV import mode is deprecated. Use Admin API instead.
     */
    @Deprecated
    private String csvPath;

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

    /**
     * @deprecated Use Admin API instead of CSV import
     */
    @Deprecated
    public String getCsvPath() {
        return csvPath;
    }

    /**
     * @deprecated Use Admin API instead of CSV import
     */
    @Deprecated
    public void setCsvPath(String csvPath) {
        this.csvPath = csvPath;
    }
}
