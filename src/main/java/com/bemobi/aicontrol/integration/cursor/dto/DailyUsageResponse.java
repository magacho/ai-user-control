package com.bemobi.aicontrol.integration.cursor.dto;

import java.util.List;

/**
 * Response DTO para endpoint GET /teams/daily-usage-data da Cursor Admin API.
 *
 * @param data lista de registros de uso diário por usuário
 */
public record DailyUsageResponse(
        List<DailyUsageRecord> data
) {}
