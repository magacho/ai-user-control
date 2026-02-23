package com.bemobi.aicontrol.integration.claude.dto;

/**
 * DTO representing estimated cost.
 */
public record ClaudeCodeEstimatedCost(
        Integer amount,
        String currency
) {}
