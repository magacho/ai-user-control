package com.bemobi.aicontrol.integration.cursor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representando uso de tokens por modelo no Cursor.
 *
 * @param inputTokens tokens de entrada consumidos (nullable)
 * @param outputTokens tokens de saída gerados (nullable)
 * @param cacheReadTokens tokens de cache lidos (nullable)
 * @param cacheWriteTokens tokens de cache escritos (nullable)
 * @param model nome do modelo utilizado
 */
public record TokenUsage(
        @JsonProperty("input_tokens") Long inputTokens,
        @JsonProperty("output_tokens") Long outputTokens,
        @JsonProperty("cache_read_tokens") Long cacheReadTokens,
        @JsonProperty("cache_write_tokens") Long cacheWriteTokens,
        String model
) {}
