package com.bemobi.aicontrol.integration.cursor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Cursor integration.
 *
 * Since Cursor doesn't have a public API, this integration uses CSV import mode.
 */
@ConfigurationProperties(prefix = "ai-control.api.cursor")
@Validated
public class CursorApiProperties {

    private boolean enabled = false;

    private String importMode = "csv";

    private String csvPath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getImportMode() {
        return importMode;
    }

    public void setImportMode(String importMode) {
        this.importMode = importMode;
    }

    public String getCsvPath() {
        return csvPath;
    }

    public void setCsvPath(String csvPath) {
        this.csvPath = csvPath;
    }
}
