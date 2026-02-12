package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.ToolType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UserUsageRow(
    String email,
    ToolType tool,
    LocalDate lastUsage,
    Long inputTokens,
    Long outputTokens,
    Long cacheReadTokens,
    Integer linesSuggested,
    Integer linesAccepted,
    Double acceptanceRate,
    BigDecimal costUsd
) { }
