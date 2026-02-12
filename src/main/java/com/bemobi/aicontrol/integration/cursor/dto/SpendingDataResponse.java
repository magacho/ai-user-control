package com.bemobi.aicontrol.integration.cursor.dto;

import java.util.List;

/**
 * Response DTO para endpoint GET /teams/spending-data da Cursor Admin API.
 *
 * @param data lista de registros de spending por usuário
 */
public record SpendingDataResponse(
        List<SpendingRecord> data
) {}
