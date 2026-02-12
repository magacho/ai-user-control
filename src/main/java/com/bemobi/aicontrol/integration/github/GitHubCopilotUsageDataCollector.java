package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.common.UsageDataCollector;
import com.bemobi.aicontrol.integration.github.dto.UserMetric;
import com.bemobi.aicontrol.integration.github.dto.UserMetricsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collector for GitHub Copilot usage data.
 *
 * <p>Implements the UsageDataCollector interface to provide unified access to GitHub Copilot
 * usage metrics. GitHub Copilot does not expose token counts or direct cost data via API,
 * so those fields are left as null or empty.</p>
 *
 * <p>Metrics are collected from the GitHub Copilot Metrics API:
 * GET /orgs/{org}/copilot/metrics/reports/users-1-day</p>
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.github", name = "enabled", havingValue = "true")
public class GitHubCopilotUsageDataCollector implements UsageDataCollector {

    private static final Logger log = LoggerFactory.getLogger(GitHubCopilotUsageDataCollector.class);

    private final GitHubCopilotApiClient apiClient;

    public GitHubCopilotUsageDataCollector(GitHubCopilotApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<UnifiedUsageRecord> collectUsageData(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("Collecting GitHub Copilot usage data from {} to {}", startDate, endDate);

        List<UnifiedUsageRecord> allRecords = new ArrayList<>();

        // GitHub Copilot API returns data per day, so we need to iterate through each day
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            try {
                UserMetricsResponse response = apiClient.fetchUserMetrics(currentDate);

                if (response.data() != null && !response.data().isEmpty()) {
                    for (UserMetric metric : response.data()) {
                        UnifiedUsageRecord record = convertToUnifiedUsageRecord(metric, currentDate);
                        allRecords.add(record);
                    }
                    log.debug("Collected {} user records for date {}", response.data().size(), currentDate);
                } else {
                    log.debug("No metrics data available for date {}", currentDate);
                }
            } catch (ApiClientException e) {
                log.warn("Failed to fetch metrics for date {}: {}", currentDate, e.getMessage());
                // Continue with next date instead of failing completely
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("Total GitHub Copilot usage records collected: {}", allRecords.size());
        return allRecords;
    }

    @Override
    public List<UnifiedSpendingRecord> collectSpendingData(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("GitHub Copilot does not expose spending data via API, returning empty list");
        // GitHub Copilot API does not provide direct cost/spending information
        return Collections.emptyList();
    }

    @Override
    public ToolType getToolType() {
        return ToolType.GITHUB_COPILOT;
    }

    /**
     * Converts a GitHub Copilot UserMetric to a UnifiedUsageRecord.
     *
     * <p>Note that GitHub Copilot does not expose token counts, so those fields are null.
     * The acceptance rate is calculated from lines of code metrics.</p>
     *
     * @param metric the GitHub Copilot user metric
     * @param date the date for the metric
     * @return unified usage record
     */
    private UnifiedUsageRecord convertToUnifiedUsageRecord(UserMetric metric, LocalDate date) {
        String email = normalizeEmail(metric.userEmail());

        // Calculate acceptance rate: locAddedSum / locSuggestedToAddSum
        Double acceptanceRate = calculateAcceptanceRate(
                metric.locAddedSum(),
                metric.locSuggestedToAddSum()
        );

        // Build raw metadata with GitHub-specific fields
        Map<String, Object> rawMetadata = new HashMap<>();
        rawMetadata.put("user_name", metric.userName());
        rawMetadata.put("user_email", metric.userEmail());
        rawMetadata.put("date", metric.date());
        rawMetadata.put("user_initiated_interaction_count", metric.userInitiatedInteractionCount());
        rawMetadata.put("code_generation_activity_count", metric.codeGenerationActivityCount());
        rawMetadata.put("code_acceptance_activity_count", metric.codeAcceptanceActivityCount());
        rawMetadata.put("loc_deleted_sum", metric.locDeletedSum());

        return new UnifiedUsageRecord(
                email,
                ToolType.GITHUB_COPILOT,
                date,
                null, // inputTokens - GitHub Copilot does not expose token counts
                null, // outputTokens - GitHub Copilot does not expose token counts
                null, // cacheReadTokens - GitHub Copilot does not expose token counts
                metric.locSuggestedToAddSum(),
                metric.locAddedSum(),
                acceptanceRate,
                rawMetadata
        );
    }

    /**
     * Normalizes email to lowercase, or returns a placeholder if email is null/empty.
     */
    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "[SEM-USR-GITHUB]";
        }
        return email.toLowerCase().trim();
    }

    /**
     * Calculates acceptance rate from lines added vs lines suggested.
     *
     * @param linesAdded lines of code actually added
     * @param linesSuggested lines of code suggested
     * @return acceptance rate (0.0 to 1.0), or null if calculation is not possible
     */
    private Double calculateAcceptanceRate(Integer linesAdded, Integer linesSuggested) {
        if (linesAdded == null || linesSuggested == null || linesSuggested == 0) {
            return null;
        }

        // Ensure we don't divide by zero and handle edge cases
        if (linesSuggested == 0) {
            return 0.0;
        }

        double rate = (double) linesAdded / (double) linesSuggested;

        // Cap at 1.0 (100%) in case added > suggested (shouldn't happen normally)
        return Math.min(rate, 1.0);
    }
}
