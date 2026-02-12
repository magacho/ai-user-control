package com.bemobi.aicontrol.integration.common;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface base para coleta de dados de usage e spending de ferramentas de IA.
 *
 * <p>Cada integração (Claude, GitHub Copilot, Cursor) deve implementar esta interface
 * para fornecer dados unificados de uso e gastos.</p>
 *
 * <p>Campos nullable nos records unificados acomodam diferenças entre ferramentas:
 * - GitHub Copilot não expõe tokens
 * - Claude não tem granularidade per-user nativa
 * - Cursor expõe dados mais completos per-user</p>
 */
public interface UsageDataCollector {

    /**
     * Coleta dados de uso (tokens, linhas, interações) no período especificado.
     *
     * @param startDate data inicial (inclusive)
     * @param endDate data final (inclusive)
     * @return lista de registros unificados de uso
     * @throws ApiClientException em caso de erro na comunicação
     */
    List<UnifiedUsageRecord> collectUsageData(LocalDate startDate, LocalDate endDate)
        throws ApiClientException;

    /**
     * Coleta dados de spending/custo no período especificado.
     *
     * @param startDate data inicial (inclusive)
     * @param endDate data final (inclusive)
     * @return lista de registros unificados de spending
     * @throws ApiClientException em caso de erro na comunicação
     */
    List<UnifiedSpendingRecord> collectSpendingData(LocalDate startDate, LocalDate endDate)
        throws ApiClientException;

    /**
     * Nome identificador da ferramenta.
     * @return tool type (CLAUDE, GITHUB_COPILOT, CURSOR)
     */
    ToolType getToolType();
}
