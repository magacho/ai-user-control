package com.bemobi.aicontrol.integration.common;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO unificado para dados de spending/custo de ferramentas de IA.
 *
 * <p>Consolida informações de gastos de diferentes plataformas.</p>
 *
 * <h3>Disponibilidade por ferramenta:</h3>
 * <ul>
 *   <li><b>Claude</b>: cost report disponível (custo em cents, convertido para USD)</li>
 *   <li><b>GitHub Copilot</b>: não expõe custos via API (retorna lista vazia)</li>
 *   <li><b>Cursor</b>: spending data disponível per-user</li>
 * </ul>
 *
 * @param email identificador do usuário (pode ser workspace-id ou api-key para Claude)
 * @param tool ferramenta de origem
 * @param period período de referência (formato: "YYYY-MM-DD" ou "YYYY-MM")
 * @param costUsd custo em dólares americanos
 * @param currency moeda original (default: "USD")
 * @param rawMetadata detalhes adicionais específicos da ferramenta — imutável após construção
 */
public record UnifiedSpendingRecord(
        String email,
        ToolType tool,
        String period,
        BigDecimal costUsd,
        String currency,
        Map<String, Object> rawMetadata
) {
    public UnifiedSpendingRecord {
        currency = currency == null ? "USD" : currency;
        rawMetadata = rawMetadata == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(rawMetadata));
    }
}
