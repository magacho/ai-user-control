package com.bemobi.aicontrol.runner;

import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.service.CsvExportService;
import com.bemobi.aicontrol.service.UnifiedUser;
import com.bemobi.aicontrol.service.UserCollectionService;
import com.bemobi.aicontrol.service.UserUnificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runner that automatically collects user data from all AI tools and exports to CSV files.
 *
 * This component runs on application startup and performs the following steps:
 * 1. Collects user data from all enabled AI tool integrations
 * 2. Exports the collected data to CSV files (one per tool)
 * 3. Optionally exports a consolidated CSV with all users
 *
 * Can be disabled via configuration: ai-control.export.on-startup=false
 */
@Component
@ConditionalOnProperty(
        name = "ai-control.export.on-startup",
        havingValue = "true",
        matchIfMissing = true
)
public class DataCollectionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataCollectionRunner.class);

    private final UserCollectionService collectionService;
    private final CsvExportService csvExportService;
    private final UserUnificationService unificationService;
    private final boolean exportConsolidated;

    /**
     * Constructs the DataCollectionRunner with required services.
     *
     * @param collectionService Service for collecting user data
     * @param csvExportService Service for exporting to CSV
     * @param unificationService Service for unifying users across tools
     * @param exportConsolidated Whether to generate consolidated CSV
     */
    public DataCollectionRunner(
            UserCollectionService collectionService,
            CsvExportService csvExportService,
            UserUnificationService unificationService,
            @Value("${ai-control.export.consolidated:false}") boolean exportConsolidated) {
        this.collectionService = collectionService;
        this.csvExportService = csvExportService;
        this.unificationService = unificationService;
        this.exportConsolidated = exportConsolidated;
    }

    @Override
    public void run(String... args) {
        MDC.put("executionId", UUID.randomUUID().toString().substring(0, 8));
        try {
            executeCollection();
        } finally {
            MDC.clear();
        }
    }

    private void executeCollection() {
        log.info("=".repeat(80));
        log.info("Starting AI User Control data collection and export");
        log.info("=".repeat(80));

        try {
            // Step 1: Collect data from all integrations
            Map<String, List<UserData>> userData = collectionService.collectAllUsers();

            // Count total users
            int totalUsers = userData.values().stream()
                    .mapToInt(List::size)
                    .sum();

            if (totalUsers == 0) {
                log.warn("No users collected from any integration. Check your configuration and credentials.");
                log.info("=".repeat(80));
                return;
            }

            log.info("Total users collected: {}", totalUsers);
            log.info("");

            // Step 2: Export to CSV files
            log.info("Exporting to CSV files...");
            List<Path> generatedFiles = csvExportService.exportToCsv(userData);

            log.info("");
            log.info("CSV files generated:");
            for (Path file : generatedFiles) {
                log.info("  - {}", file.toAbsolutePath());
            }

            // Step 3: Optionally export consolidated CSV
            if (exportConsolidated) {
                log.info("");
                log.info("Generating consolidated CSV...");
                Path consolidatedFile = csvExportService.exportToConsolidatedCsv(userData);
                log.info("Consolidated CSV: {}", consolidatedFile.toAbsolutePath());
            }

            // Step 4: Generate unified CSV (always)
            log.info("");
            log.info("Generating unified CSV...");
            List<UnifiedUser> unified = unificationService.unify(userData);
            Path unifiedCsv = csvExportService.exportToUnifiedCsv(unified);
            log.info("Unified CSV: {}", unifiedCsv.toAbsolutePath());

            String summary = unificationService.buildSummary(unified, userData);
            log.info("");
            log.info(summary);

            log.info("");
            log.info("=".repeat(80));
            log.info("Export completed successfully!");
            log.info("=".repeat(80));

        } catch (IOException e) {
            log.error("=".repeat(80));
            log.error("ERROR: Failed to export CSV files", e);
            log.error("=".repeat(80));
            throw new RuntimeException("CSV export failed", e);
        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("ERROR: Unexpected error during data collection", e);
            log.error("=".repeat(80));
            throw new RuntimeException("Data collection failed", e);
        }
    }
}
