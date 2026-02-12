package com.bemobi.aicontrol.integration.cursor.dto;

import java.math.BigDecimal;

/**
 * DTO representando dados de spending individual de um membro no Cursor.
 *
 * @param email email do usuário
 * @param name nome do usuário
 * @param spending custo em USD
 */
public record SpendingRecord(
        String email,
        String name,
        BigDecimal spending
) {}
