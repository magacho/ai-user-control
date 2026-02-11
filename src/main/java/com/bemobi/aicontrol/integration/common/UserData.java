package com.bemobi.aicontrol.integration.common;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO base para dados de usuário coletados de ferramentas de IA.
 *
 * <p>Este DTO normaliza informações de diferentes plataformas em um formato comum.</p>
 *
 * @param additionalMetrics métricas específicas por ferramenta — imutável após construção.
 *                          Chaves variam conforme a origem (ex: "role", "email_type", "github_login").
 * @param rawJson JSON bruto da resposta da API de origem, para debug/auditoria.
 */
public record UserData(
        String email,
        String name,
        String status,
        LocalDateTime lastActivityAt,
        Map<String, Object> additionalMetrics,
        String rawJson
) {
    public UserData {
        additionalMetrics = additionalMetrics == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(additionalMetrics));
    }
}
