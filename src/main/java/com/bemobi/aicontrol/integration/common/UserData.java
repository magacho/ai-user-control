package com.bemobi.aicontrol.integration.common;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO base para dados de usuário coletados de ferramentas de IA.
 *
 * Este DTO normaliza informações de diferentes plataformas em um formato comum.
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
        if (additionalMetrics == null) {
            additionalMetrics = Map.of();
        }
    }
}
