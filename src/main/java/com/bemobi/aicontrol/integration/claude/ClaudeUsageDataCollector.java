package com.bemobi.aicontrol.integration.claude;

import com.bemobi.aicontrol.integration.claude.dto.CostDataPoint;
import com.bemobi.aicontrol.integration.claude.dto.CostReportResponse;
import com.bemobi.aicontrol.integration.claude.dto.UsageDataPoint;
import com.bemobi.aicontrol.integration.claude.dto.UsageReportResponse;
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
 * Collector for Claude API usage and spending data.
 *
 * <p>Converts Claude-specific usage and cost reports into unified records.</p>
 *
 * <p><b>Note:</b> Claude API does not provide per-user granularity.
 * We use workspace_id or api_key_id as the "email" field in unified records.</p>
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.claude", name = "enabled", havingValue = "true")
public class ClaudeUsageDataCollector implements UsageDataCollector {

    private static final Logger log = LoggerFactory.getLogger(ClaudeUsageDataCollector.class);
    private static final String DEFAULT_BUCKET_WIDTH = "1d";

    private final ClaudeApiClient claudeApiClient;

    public ClaudeUsageDataCollector(ClaudeApiClient claudeApiClient) {
        this.claudeApiClient = claudeApiClient;
    }

    @Override
    public List<UnifiedUsageRecord> collectUsageData(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("Collecting usage data from Claude: {} to {}", startDate, endDate);

        UsageReportResponse response = claudeApiClient.fetchUsageReport(
                startDate, endDate, DEFAULT_BUCKET_WIDTH);

        if (response.data() == null || response.data().isEmpty()) {
            log.info("No usage data found for period {} to {}", startDate, endDate);
            return List.of();
        }

        List<UnifiedUsageRecord> records = new ArrayList<>();
        for (UsageDataPoint dataPoint : response.data()) {
            records.add(convertToUnifiedUsageRecord(dataPoint));
        }

        log.info("Converted {} usage data points to unified records", records.size());
        return records;
    }

    @Override
    public List<UnifiedSpendingRecord> collectSpendingData(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("Collecting spending data from Claude: {} to {}", startDate, endDate);

        CostReportResponse response = claudeApiClient.fetchCostReport(startDate, endDate);

        if (response.data() == null || response.data().isEmpty()) {
            log.info("No spending data found for period {} to {}", startDate, endDate);
            return List.of();
        }

        List<UnifiedSpendingRecord> records = new ArrayList<>();
        for (CostDataPoint dataPoint : response.data()) {
            records.add(convertToUnifiedSpendingRecord(dataPoint));
        }

        log.info("Converted {} spending data points to unified records", records.size());
        return records;
    }

    @Override
    public ToolType getToolType() {
        return ToolType.CLAUDE;
    }

    /**
     * Converts Claude UsageDataPoint to UnifiedUsageRecord.
     *
     * <p>Uses workspaceId or apiKeyId as the "email" identifier since Claude
     * doesn't provide per-user granularity.</p>
     */
    private UnifiedUsageRecord convertToUnifiedUsageRecord(UsageDataPoint dataPoint) {
        String identifier = dataPoint.workspaceId() != null
                ? dataPoint.workspaceId()
                : dataPoint.apiKeyId();

        Map<String, Object> rawMetadata = new HashMap<>();
        rawMetadata.put("workspace_id", dataPoint.workspaceId());
        rawMetadata.put("api_key_id", dataPoint.apiKeyId());
        rawMetadata.put("model", dataPoint.model());
        rawMetadata.put("cache_creation_input_tokens", dataPoint.cacheCreationInputTokens());

        return new UnifiedUsageRecord(
                identifier,
                ToolType.CLAUDE,
                dataPoint.timestamp().toLocalDate(),
                dataPoint.inputTokens(),
                dataPoint.outputTokens(),
                dataPoint.cacheReadInputTokens(),
                null, // linesSuggested - not applicable for Claude
                null, // linesAccepted - not applicable for Claude
                null, // acceptanceRate - not applicable for Claude
                rawMetadata
        );
    }

    /**
     * Converts Claude CostDataPoint to UnifiedSpendingRecord.
     *
     * <p>Cost from Claude API is in cents, so we divide by 100 to get USD.</p>
     */
    private UnifiedSpendingRecord convertToUnifiedSpendingRecord(CostDataPoint dataPoint) {
        String identifier = dataPoint.workspaceId() != null
                ? dataPoint.workspaceId()
                : "unknown";

        // Convert cents to USD
        BigDecimal costUsd = dataPoint.cost() != null
                ? BigDecimal.valueOf(dataPoint.cost())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Use date from timestamp for period
        String period = dataPoint.timestamp().toLocalDate().toString();

        Map<String, Object> rawMetadata = new HashMap<>();
        rawMetadata.put("workspace_id", dataPoint.workspaceId());
        rawMetadata.put("cost_type", dataPoint.costType());
        rawMetadata.put("description", dataPoint.description());
        rawMetadata.put("timestamp", dataPoint.timestamp().toString());

        return new UnifiedSpendingRecord(
                identifier,
                ToolType.CLAUDE,
                period,
                costUsd,
                "USD",
                rawMetadata
        );
    }
}
