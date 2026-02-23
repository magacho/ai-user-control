package com.bemobi.aicontrol.integration.claude;

import com.bemobi.aicontrol.integration.claude.dto.ClaudeCodeRecord;
import com.bemobi.aicontrol.integration.claude.dto.ClaudeCodeUsageReport;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.common.UsageDataCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collector for Claude Code usage and spending data.
 *
 * <p>Converts Claude Code usage reports into unified records.</p>
 *
 * <p><b>Note:</b> Uses Claude Code endpoint which provides per-user statistics
 * with email addresses.</p>
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.claude", name = "enabled", havingValue = "true")
public class ClaudeUsageDataCollector implements UsageDataCollector {

    private static final Logger log = LoggerFactory.getLogger(ClaudeUsageDataCollector.class);

    private final ClaudeApiClient claudeApiClient;

    public ClaudeUsageDataCollector(ClaudeApiClient claudeApiClient) {
        this.claudeApiClient = claudeApiClient;
    }

    @Override
    public List<UnifiedUsageRecord> collectUsageData(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("Collecting usage data from Claude Code: {} to {}", startDate, endDate);

        // Claude Code endpoint uses starting_at parameter
        ClaudeCodeUsageReport response = claudeApiClient.fetchClaudeCodeUsageReport(startDate);

        if (response.data() == null || response.data().isEmpty()) {
            log.info("No usage data found for period starting {}", startDate);
            return List.of();
        }

        List<UnifiedUsageRecord> records = new ArrayList<>();
        for (ClaudeCodeRecord record : response.data()) {
            records.add(convertClaudeCodeToUsageRecord(record));
        }

        log.info("Converted {} Claude Code records to unified usage records", records.size());
        return records;
    }

    @Override
    public List<UnifiedSpendingRecord> collectSpendingData(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("Collecting spending data from Claude Code: {} to {}", startDate, endDate);

        // Claude Code endpoint uses starting_at parameter
        ClaudeCodeUsageReport response = claudeApiClient.fetchClaudeCodeUsageReport(startDate);

        if (response.data() == null || response.data().isEmpty()) {
            log.info("No spending data found for period starting {}", startDate);
            return List.of();
        }

        List<UnifiedSpendingRecord> records = new ArrayList<>();
        for (ClaudeCodeRecord record : response.data()) {
            UnifiedSpendingRecord spendingRecord = convertClaudeCodeToSpendingRecord(record);
            if (spendingRecord != null) {
                records.add(spendingRecord);
            }
        }

        log.info("Converted {} Claude Code records to unified spending records", records.size());
        return records;
    }

    @Override
    public ToolType getToolType() {
        return ToolType.CLAUDE;
    }

    /**
     * Converts ClaudeCodeRecord to UnifiedUsageRecord.
     *
     * <p>Aggregates token usage from model_breakdown and extracts email from actor.</p>
     */
    private UnifiedUsageRecord convertClaudeCodeToUsageRecord(ClaudeCodeRecord record) {
        // Extract email from actor
        String email = record.actor() != null && record.actor().emailAddress() != null
                ? record.actor().emailAddress()
                : "unknown";

        // Parse date (supports both YYYY-MM-DD and ISO 8601 formats)
        LocalDate date = null;
        if (record.date() != null) {
            try {
                // Try ISO 8601 format first (2026-02-05T00:00:00Z)
                if (record.date().length() > 10) {
                    date = LocalDate.parse(record.date().substring(0, 10));
                } else {
                    // Simple YYYY-MM-DD format
                    date = LocalDate.parse(record.date());
                }
            } catch (Exception e) {
                log.warn("Failed to parse date '{}': {}", record.date(), e.getMessage());
            }
        }

        // Aggregate tokens from model_breakdown
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        long totalCacheReadTokens = 0;

        if (record.modelBreakdown() != null) {
            for (var breakdown : record.modelBreakdown()) {
                if (breakdown.tokens() != null) {
                    if (breakdown.tokens().input() != null) {
                        totalInputTokens += breakdown.tokens().input();
                    }
                    if (breakdown.tokens().output() != null) {
                        totalOutputTokens += breakdown.tokens().output();
                    }
                    if (breakdown.tokens().cacheRead() != null) {
                        totalCacheReadTokens += breakdown.tokens().cacheRead();
                    }
                }
            }
        }

        Map<String, Object> rawMetadata = new HashMap<>();
        rawMetadata.put("organization_id", record.organizationId());
        rawMetadata.put("actor", record.actor());
        rawMetadata.put("customer_type", record.customerType());
        rawMetadata.put("terminal_type", record.terminalType());
        rawMetadata.put("subscription_type", record.subscriptionType());
        if (record.coreMetrics() != null) {
            rawMetadata.put("commits_by_claude_code", record.coreMetrics().commitsByClaudeCode());
            rawMetadata.put("num_sessions", record.coreMetrics().numSessions());
            rawMetadata.put("pull_requests_by_claude_code", record.coreMetrics().pullRequestsByClaudeCode());
            if (record.coreMetrics().linesOfCode() != null) {
                rawMetadata.put("lines_added", record.coreMetrics().linesOfCode().added());
                rawMetadata.put("lines_removed", record.coreMetrics().linesOfCode().removed());
            }
        }

        return new UnifiedUsageRecord(
                email,
                ToolType.CLAUDE,
                date,
                totalInputTokens > 0 ? totalInputTokens : null,
                totalOutputTokens > 0 ? totalOutputTokens : null,
                totalCacheReadTokens > 0 ? totalCacheReadTokens : null,
                null, // linesSuggested - not applicable for Claude Code
                null, // linesAccepted - not applicable for Claude Code
                null, // acceptanceRate - not applicable for Claude Code
                rawMetadata
        );
    }

    /**
     * Converts ClaudeCodeRecord to UnifiedSpendingRecord.
     *
     * <p>Aggregates estimated costs from model_breakdown. Amounts are in cents.</p>
     */
    private UnifiedSpendingRecord convertClaudeCodeToSpendingRecord(ClaudeCodeRecord record) {
        // Extract email from actor
        String email = record.actor() != null && record.actor().emailAddress() != null
                ? record.actor().emailAddress()
                : "unknown";

        // Parse date (supports both YYYY-MM-DD and ISO 8601 formats)
        String period = "unknown";
        if (record.date() != null) {
            try {
                // Extract YYYY-MM-DD from ISO 8601 format if needed
                period = record.date().length() > 10
                        ? record.date().substring(0, 10)
                        : record.date();
            } catch (Exception e) {
                log.warn("Failed to extract date from '{}': {}", record.date(), e.getMessage());
            }
        }

        // Aggregate costs from model_breakdown
        BigDecimal totalCost = BigDecimal.ZERO;

        if (record.modelBreakdown() != null) {
            for (var breakdown : record.modelBreakdown()) {
                if (breakdown.estimatedCost() != null && breakdown.estimatedCost().amount() != null) {
                    try {
                        // Amount is in cents, divide by 100 to get USD
                        BigDecimal cost = new BigDecimal(breakdown.estimatedCost().amount())
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        totalCost = totalCost.add(cost);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse cost amount '{}' for model {}",
                                breakdown.estimatedCost().amount(), breakdown.model());
                    }
                }
            }
        }

        // Only create spending record if there's actual cost
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        Map<String, Object> rawMetadata = new HashMap<>();
        rawMetadata.put("organization_id", record.organizationId());
        rawMetadata.put("actor", record.actor());
        rawMetadata.put("customer_type", record.customerType());
        rawMetadata.put("model_breakdown", record.modelBreakdown());

        return new UnifiedSpendingRecord(
                email,
                ToolType.CLAUDE,
                period,
                totalCost,
                "USD",
                rawMetadata
        );
    }
}
