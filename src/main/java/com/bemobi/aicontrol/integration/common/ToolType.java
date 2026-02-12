package com.bemobi.aicontrol.integration.common;

/**
 * Tipos de ferramentas de IA suportadas.
 */
public enum ToolType {
    /**
     * Claude Code (Anthropic)
     */
    CLAUDE("claude-code", "Claude Code"),

    /**
     * GitHub Copilot
     */
    GITHUB_COPILOT("github-copilot", "GitHub Copilot"),

    /**
     * Cursor
     */
    CURSOR("cursor", "Cursor");

    private final String id;
    private final String displayName;

    ToolType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Identificador único da ferramenta.
     * @return id (ex: "claude-code")
     */
    public String getId() {
        return id;
    }

    /**
     * Nome para exibição.
     * @return display name (ex: "Claude Code")
     */
    public String getDisplayName() {
        return displayName;
    }
}
