package com.bemobi.aicontrol.service;

import java.math.BigDecimal;
import java.util.Set;
import com.bemobi.aicontrol.integration.common.ToolType;

public record MultiToolUserRow(
    String email,
    Set<ToolType> tools,
    int toolCount,
    boolean usesClaude,
    boolean usesGitHub,
    boolean usesCursor,
    Long totalTokens,
    Integer linesSuggested,
    Integer linesAccepted,
    BigDecimal totalCost
) { }
