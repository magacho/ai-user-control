package com.bemobi.aicontrol.integration.cursor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO representando dados de uso diário de um membro no Cursor.
 *
 * @param email email do usuário
 * @param date data no formato YYYY-MM-DD
 * @param linesAdded linhas de código adicionadas (nullable)
 * @param linesDeleted linhas de código deletadas (nullable)
 * @param acceptanceRate taxa de aceitação 0.0-1.0 (nullable)
 * @param requestTypes mapa de tipos de requisição e contagens (nullable)
 * @param mostUsedModels lista de modelos mais utilizados (nullable)
 * @param tokenUsage lista de uso de tokens por modelo
 */
public record DailyUsageRecord(
        String email,
        String date,
        @JsonProperty("lines_added") Integer linesAdded,
        @JsonProperty("lines_deleted") Integer linesDeleted,
        @JsonProperty("acceptance_rate") Double acceptanceRate,
        @JsonProperty("request_types") Map<String, Integer> requestTypes,
        @JsonProperty("most_used_models") List<String> mostUsedModels,
        @JsonProperty("token_usage") List<TokenUsage> tokenUsage
) {}
