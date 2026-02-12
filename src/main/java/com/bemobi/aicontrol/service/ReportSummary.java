package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.ToolType;
import java.math.BigDecimal;
import java.util.Map;

public record ReportSummary(
    BigDecimal totalCostUsd,
    Long totalInputTokens,
    Long totalOutputTokens,
    Integer userCount,
    Map<ToolType, BigDecimal> costByTool
) { }
