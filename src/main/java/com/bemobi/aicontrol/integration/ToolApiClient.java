package com.bemobi.aicontrol.integration;

import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;

import java.util.List;

/**
 * Base interface for all AI tool API clients.
 *
 * Each integration (Claude Code, GitHub Copilot, Cursor) implements this interface
 * to provide a unified way to collect user data from different platforms.
 */
public interface ToolApiClient {

    /**
     * Nome identificador da ferramenta
     * @return nome da ferramenta (claude-code, github-copilot, cursor)
     */
    String getToolName();

    /**
     * Nome para exibição
     * @return nome para exibição (Claude Code, GitHub Copilot, Cursor)
     */
    String getDisplayName();

    /**
     * Busca lista de usuários ativos na ferramenta
     * @return lista de dados de usuários
     * @throws com.bemobi.aicontrol.integration.common.ApiClientException em caso de erro na comunicação
     */
    List<UserData> fetchUsers() throws com.bemobi.aicontrol.integration.common.ApiClientException;

    /**
     * Testa conectividade com a API
     * @return resultado do teste de conexão
     */
    ConnectionTestResult testConnection();

    /**
     * Verifica se o cliente está habilitado
     * @return true se habilitado
     */
    boolean isEnabled();
}
