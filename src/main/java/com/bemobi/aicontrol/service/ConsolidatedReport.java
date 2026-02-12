package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import java.time.LocalDateTime;
import java.util.List;

public record ConsolidatedReport(
    String period,
    LocalDateTime generatedAt,
    List<UnifiedUsageRecord> usageRecords,
    List<UnifiedSpendingRecord> spendingRecords,
    ReportSummary summary
) { }
