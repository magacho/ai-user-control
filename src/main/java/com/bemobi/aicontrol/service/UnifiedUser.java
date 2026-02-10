package com.bemobi.aicontrol.service;

/**
 * Record imutável representando um usuário unificado entre múltiplas ferramentas de IA.
 *
 * Cada instância consolida dados de até 3 ferramentas (Claude, Copilot, Cursor)
 * em uma única linha, com flags de presença e informações de atividade por ferramenta.
 */
public record UnifiedUser(
        String email,
        String name,
        int toolsCount,
        boolean usesClaude,
        boolean usesCopilot,
        boolean usesCursor,
        String claudeLastActivity,
        String copilotLastActivity,
        String cursorLastActivity,
        String claudeStatus,
        String copilotStatus,
        String cursorStatus,
        String emailType
) {
}
