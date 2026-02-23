package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a single Claude Code usage record.
 */
public record ClaudeCodeRecord(
        ClaudeCodeActor actor,
        @JsonProperty("core_metrics") ClaudeCodeCoreMetrics coreMetrics,
        @JsonProperty("customer_type") String customerType,
        String date,
        @JsonProperty("model_breakdown") List<ClaudeCodeModelBreakdown> modelBreakdown,
        @JsonProperty("organization_id") String organizationId,
        @JsonProperty("terminal_type") String terminalType,
        @JsonProperty("tool_actions") Map<String, ClaudeCodeToolActions> toolActions,
        @JsonProperty("subscription_type") String subscriptionType
) {}
