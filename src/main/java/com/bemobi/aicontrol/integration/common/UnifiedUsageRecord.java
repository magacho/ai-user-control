package com.bemobi.aicontrol.integration.common;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO unificado para dados de uso (usage) de ferramentas de IA.
 *
 * <p>Consolida métricas de diferentes plataformas em um formato comum.</p>
 *
 * <h3>Campos Nullable (por ferramenta):</h3>
 * <ul>
 *   <li><b>Claude</b>: inputTokens, outputTokens, cacheReadTokens ✓ | linesSuggested, linesAccepted ✗</li>
 *   <li><b>GitHub Copilot</b>: inputTokens, outputTokens ✗ | linesSuggested, linesAccepted ✓</li>
 *   <li><b>Cursor</b>: todos os campos ✓</li>
 * </ul>
 *
 * @param email identificador do usuário (pode ser workspace-id ou api-key para Claude)
 * @param tool ferramenta de origem
 * @param date data de referência
 * @param inputTokens tokens de entrada consumidos (nullable)
 * @param outputTokens tokens de saída gerados (nullable)
 * @param cacheReadTokens tokens de cache lidos (nullable)
 * @param linesSuggested linhas de código sugeridas (nullable)
 * @param linesAccepted linhas de código aceitas (nullable)
 * @param acceptanceRate taxa de aceitação 0.0-1.0 (nullable, calculada quando possível)
 * @param rawMetadata campos específicos da ferramenta — imutável após construção
 */
public record UnifiedUsageRecord(
        String email,
        ToolType tool,
        LocalDate date,
        Long inputTokens,
        Long outputTokens,
        Long cacheReadTokens,
        Integer linesSuggested,
        Integer linesAccepted,
        Double acceptanceRate,
        Map<String, Object> rawMetadata
) {
    public UnifiedUsageRecord {
        rawMetadata = rawMetadata == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(rawMetadata));
    }
}
