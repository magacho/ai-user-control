package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.common.UsageDataCollector;
import com.bemobi.aicontrol.integration.google.GoogleWorkspaceClient;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for consolidating usage and spending data from all AI tool collectors
 * and generating XLSX reports with multiple sheets.
 *
 * <p>This service coordinates data collection from Claude, GitHub Copilot, and Cursor collectors,
 * consolidates the results, and generates a comprehensive XLSX report with 3 sheets:
 * <ul>
 *   <li>Volumes de Uso: User usage metrics by tool</li>
 *   <li>GitHub Não Cadastrados: GitHub users without Google Workspace email</li>
 *   <li>Multi-Tool Users: Users using multiple AI tools</li>
 * </ul>
 */
@Service
public class UnifiedSpendingService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedSpendingService.class);

    // Sheet 1: Usage Volumes
    private static final String[] USAGE_HEADERS = {
        "Email", "Ferramenta", "Último Uso",
        "Tokens Entrada", "Tokens Saída", "Tokens Cache",
        "Linhas Sugeridas", "Linhas Aceitas", "Taxa Aceitação (%)",
        "Custo (USD)"
    };

    // Sheet 2: GitHub Unregistered
    private static final String[] GITHUB_UNREGISTERED_HEADERS = {
        "GitHub Login", "GitHub Email", "Último Uso",
        "Linhas Sugeridas", "Linhas Aceitas"
    };

    // Sheet 3: Multi-Tool Users
    private static final String[] MULTI_TOOL_HEADERS = {
        "Email", "Ferramentas", "Quantidade",
        "Usa Claude", "Usa GitHub Copilot", "Usa Cursor",
        "Tokens Total", "Linhas Sugeridas", "Linhas Aceitas", "Custo Total (USD)"
    };

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final List<UsageDataCollector> collectors;
    private final GoogleWorkspaceClient workspaceClient;
    private final String outputDirectory;

    /**
     * Constructs the UnifiedSpendingService with all available collectors.
     *
     * @param collectors List of UsageDataCollector implementations injected by Spring
     * @param workspaceClient Google Workspace client for email validation (optional)
     * @param outputDirectory Output directory for XLSX files
     */
    public UnifiedSpendingService(
        List<UsageDataCollector> collectors,
        @Autowired(required = false) GoogleWorkspaceClient workspaceClient,
        @Value("${ai-control.export.output-directory:./output}") String outputDirectory
    ) {
        this.collectors = collectors;
        this.workspaceClient = workspaceClient;
        this.outputDirectory = outputDirectory;
        log.info("UnifiedSpendingService initialized with {} collectors", collectors.size());
    }

    /**
     * Generates a consolidated spending report for the specified date range.
     *
     * <p>Collects usage and spending data from all available collectors, handling failures gracefully.
     * If a collector fails, continues with remaining collectors and returns partial data.
     *
     * @param startDate Start date of the report period (inclusive)
     * @param endDate End date of the report period (inclusive)
     * @return ConsolidatedReport containing all collected data and summary
     */
    public ConsolidatedReport generateSpendingReport(LocalDate startDate, LocalDate endDate) {
        log.info("Starting spending report generation for period {  } to {}", startDate, endDate);

        List<UnifiedUsageRecord> allUsageRecords = new ArrayList<>();
        List<UnifiedSpendingRecord> allSpendingRecords = new ArrayList<>();

        for (UsageDataCollector collector : collectors) {
            String toolId = collector.getToolType().getId();
            MDC.put("toolName", toolId);

            try {
                log.info("Collecting data from {} collector...", toolId);

                // Collect usage data
                try {
                    List<UnifiedUsageRecord> usageRecords = collector.collectUsageData(startDate, endDate);
                    allUsageRecords.addAll(usageRecords);
                    log.info("Collected {} usage records from {}", usageRecords.size(), toolId);
                } catch (Exception e) {
                    log.error("Error collecting usage data from {}: {}", toolId, e.getMessage(), e);
                }

                // Collect spending data
                try {
                    List<UnifiedSpendingRecord> spendingRecords = collector.collectSpendingData(startDate, endDate);
                    allSpendingRecords.addAll(spendingRecords);
                    log.info("Collected {} spending records from {}", spendingRecords.size(), toolId);
                } catch (Exception e) {
                    log.error("Error collecting spending data from {}: {}", toolId, e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("Unexpected error processing collector {}: {}", toolId, e.getMessage(), e);
            } finally {
                MDC.remove("toolName");
            }
        }

        ReportSummary summary = calculateSummary(allUsageRecords, allSpendingRecords);

        String period = startDate.format(DateTimeFormatter.ISO_DATE)
                + " to " + endDate.format(DateTimeFormatter.ISO_DATE);

        log.info("Report generation completed. Usage records: {}, Spending records: {}, Total cost: ${}",
            allUsageRecords.size(), allSpendingRecords.size(), summary.totalCostUsd());

        return new ConsolidatedReport(
            period,
            LocalDateTime.now(),
            allUsageRecords,
            allSpendingRecords,
            summary
        );
    }

    /**
     * Exports the consolidated report to an XLSX file with 6 sheets.
     *
     * <p><strong>Consolidated Sheets (aggregated by user):</strong></p>
     * <ol>
     *   <li><strong>Volumes de Uso</strong> - Aggregated usage by user and tool (period totals)</li>
     *   <li><strong>GitHub Não Cadastrados</strong> - Unregistered GitHub users (no corporate email)</li>
     *   <li><strong>Usuários Multi-Tool</strong> - Users with multiple tools (aggregated metrics)</li>
     * </ol>
     *
     * <p><strong>Raw Data Sheets (debugging - snapshots):</strong></p>
     * <ol start="4">
     *   <li><strong>Claude - Dados Brutos</strong> - Raw Claude records (all records from API)</li>
     *   <li><strong>GitHub - Dados Brutos</strong> - Raw GitHub Copilot seats (snapshot)</li>
     *   <li><strong>Cursor - Dados Brutos</strong> - Raw Cursor records (LAST DAY ONLY - snapshot)</li>
     * </ol>
     *
     * <p><strong>Note:</strong> Cursor raw data shows only the last day to avoid showing 30 days × N users.
     * The "Volumes de Uso" sheet already shows aggregated period data.</p>
     *
     * @param report ConsolidatedReport to export
     * @param outputPath Path where the XLSX file will be saved
     * @return Path to the generated XLSX file
     * @throws IOException if file writing fails
     */
    public Path exportToXlsx(ConsolidatedReport report, Path outputPath) throws IOException {
        log.info("Starting XLSX export to {}", outputPath);

        // Consolidated sheets
        List<UserUsageRow> usageRows = buildUserUsageRows(
            report.usageRecords(),
            report.spendingRecords()
        );

        List<GitHubUnregisteredRow> githubUnregisteredRows =
            buildGitHubUnregisteredRows(report.usageRecords());

        List<MultiToolUserRow> multiToolRows =
            buildMultiToolUserRows(report.usageRecords(), report.spendingRecords());

        // Raw data sheets (for debugging)
        // Claude: show all records (usually snapshot-based from API)
        List<UnifiedUsageRecord> claudeRawRecords = report.usageRecords().stream()
            .filter(r -> r.tool() == ToolType.CLAUDE)
            .toList();

        // GitHub: show all records (usually snapshot-based from seats API)
        List<UnifiedUsageRecord> githubRawRecords = report.usageRecords().stream()
            .filter(r -> r.tool() == ToolType.GITHUB_COPILOT)
            .toList();

        // Cursor: show ONLY last day (snapshot) to avoid 30 days × N users records
        // The consolidated sheet already shows aggregated data for the period
        LocalDate lastDate = report.usageRecords().stream()
            .filter(r -> r.tool() == ToolType.CURSOR)
            .map(UnifiedUsageRecord::date)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());

        List<UnifiedUsageRecord> cursorRawRecords = report.usageRecords().stream()
            .filter(r -> r.tool() == ToolType.CURSOR)
            .filter(r -> r.date().equals(lastDate))
            .toList();

        log.debug("Cursor raw data filtered to last date: {} ({} records)",
            lastDate, cursorRawRecords.size());

        writeXlsxFile(outputPath, usageRows, githubUnregisteredRows, multiToolRows,
            claudeRawRecords, githubRawRecords, cursorRawRecords);

        log.info("XLSX export completed. File: {}, Usage: {}, Unregistered: {}, Multi-tool: {},"
                + " Raw: Claude={}, GitHub={}, Cursor={}",
                outputPath, usageRows.size(), githubUnregisteredRows.size(), multiToolRows.size(),
                claudeRawRecords.size(), githubRawRecords.size(), cursorRawRecords.size());

        return outputPath;
    }

    /**
     * Calculates summary statistics from usage and spending records.
     */
    private ReportSummary calculateSummary(
        List<UnifiedUsageRecord> usageRecords,
        List<UnifiedSpendingRecord> spendingRecords
    ) {
        BigDecimal totalCost = spendingRecords.stream()
            .map(UnifiedSpendingRecord::costUsd)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalInputTokens = usageRecords.stream()
            .map(UnifiedUsageRecord::inputTokens)
            .filter(Objects::nonNull)
            .reduce(0L, Long::sum);

        Long totalOutputTokens = usageRecords.stream()
            .map(UnifiedUsageRecord::outputTokens)
            .filter(Objects::nonNull)
            .reduce(0L, Long::sum);

        Integer userCount = (int) usageRecords.stream()
            .map(UnifiedUsageRecord::email)
            .distinct()
            .count();

        Map<ToolType, BigDecimal> costByTool = spendingRecords.stream()
            .filter(r -> r.costUsd() != null)
            .collect(Collectors.groupingBy(
                UnifiedSpendingRecord::tool,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    UnifiedSpendingRecord::costUsd,
                    BigDecimal::add
                )
            ));

        return new ReportSummary(
            totalCost,
            totalInputTokens,
            totalOutputTokens,
            userCount,
            costByTool
        );
    }

    /**
     * Builds user usage rows for Sheet 1 by merging usage and spending data.
     */
    private List<UserUsageRow> buildUserUsageRows(
        List<UnifiedUsageRecord> usageRecords,
        List<UnifiedSpendingRecord> spendingRecords
    ) {
        // Create spending lookup map: (email, tool) -> cost
        Map<String, BigDecimal> spendingMap = spendingRecords.stream()
            .collect(Collectors.toMap(
                r -> r.email() + ":" + r.tool().getId(),
                UnifiedSpendingRecord::costUsd,
                BigDecimal::add
            ));

        // Group usage records by (email, tool)
        Map<String, List<UnifiedUsageRecord>> groupedRecords = usageRecords.stream()
            .collect(Collectors.groupingBy(r -> r.email() + ":" + r.tool().getId()));

        List<UserUsageRow> rows = new ArrayList<>();

        for (Map.Entry<String, List<UnifiedUsageRecord>> entry : groupedRecords.entrySet()) {
            List<UnifiedUsageRecord> records = entry.getValue();
            UnifiedUsageRecord first = records.get(0);

            // Find last usage date
            LocalDate lastUsage = records.stream()
                .map(UnifiedUsageRecord::date)
                .max(LocalDate::compareTo)
                .orElse(null);

            // Sum tokens and lines
            Long inputTokens = records.stream()
                .map(UnifiedUsageRecord::inputTokens)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
            if (inputTokens == 0L && records.stream().anyMatch(r -> r.inputTokens() != null)) {
                inputTokens = null; // Keep as null if all are null
            }

            Long outputTokens = records.stream()
                .map(UnifiedUsageRecord::outputTokens)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
            if (outputTokens == 0L && records.stream().noneMatch(r -> r.outputTokens() != null)) {
                outputTokens = null;
            }

            Long cacheTokens = records.stream()
                .map(UnifiedUsageRecord::cacheReadTokens)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
            if (cacheTokens == 0L && records.stream().noneMatch(r -> r.cacheReadTokens() != null)) {
                cacheTokens = null;
            }

            Integer linesSuggested = records.stream()
                .map(UnifiedUsageRecord::linesSuggested)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
            if (linesSuggested == 0 && records.stream().noneMatch(r -> r.linesSuggested() != null)) {
                linesSuggested = null;
            }

            Integer linesAccepted = records.stream()
                .map(UnifiedUsageRecord::linesAccepted)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
            if (linesAccepted == 0 && records.stream().noneMatch(r -> r.linesAccepted() != null)) {
                linesAccepted = null;
            }

            // Calculate acceptance rate
            Double acceptanceRate = null;
            if (linesSuggested != null && linesSuggested > 0 && linesAccepted != null) {
                acceptanceRate = (linesAccepted.doubleValue() / linesSuggested.doubleValue()) * 100.0;
            }

            BigDecimal cost = spendingMap.get(entry.getKey());

            rows.add(new UserUsageRow(
                first.email(),
                first.tool(),
                lastUsage,
                inputTokens,
                outputTokens,
                cacheTokens,
                linesSuggested,
                linesAccepted,
                acceptanceRate,
                cost
            ));
        }

        // Sort by email then tool (handle nulls)
        rows.sort(Comparator.comparing(UserUsageRow::email, Comparator.nullsLast(String::compareTo))
            .thenComparing(r -> r.tool().getId()));

        return rows;
    }

    /**
     * Builds GitHub unregistered rows for Sheet 2.
     * Only includes GitHub users without Google Workspace email.
     */
    private List<GitHubUnregisteredRow> buildGitHubUnregisteredRows(
        List<UnifiedUsageRecord> usageRecords
    ) {
        if (workspaceClient == null) {
            log.warn("GoogleWorkspaceClient not available, skipping GitHub unregistered check");
            return List.of();
        }

        List<GitHubUnregisteredRow> rows = new ArrayList<>();

        // Filter only GitHub Copilot records
        List<UnifiedUsageRecord> githubRecords = usageRecords.stream()
            .filter(r -> r.tool() == ToolType.GITHUB_COPILOT)
            .toList();

        // Group by email (GitHub login)
        Map<String, List<UnifiedUsageRecord>> byUser = githubRecords.stream()
            .collect(Collectors.groupingBy(UnifiedUsageRecord::email));

        for (Map.Entry<String, List<UnifiedUsageRecord>> entry : byUser.entrySet()) {
            String githubLogin = entry.getKey();
            List<UnifiedUsageRecord> records = entry.getValue();

            // Extract gitHubLogin from rawMetadata if available
            String actualGitLogin = records.stream()
                .map(UnifiedUsageRecord::rawMetadata)
                .filter(m -> m.containsKey("gitHubLogin"))
                .map(m -> (String) m.get("gitHubLogin"))
                .findFirst()
                .orElse(githubLogin);

            // Check if user exists in Google Workspace
            Optional<String> workspaceEmail = workspaceClient.findEmailByGitName(actualGitLogin);

            if (workspaceEmail.isEmpty()) {
                // User not found in Workspace - add to unregistered list
                LocalDate lastUsage = records.stream()
                    .map(UnifiedUsageRecord::date)
                    .max(LocalDate::compareTo)
                    .orElse(null);

                Integer linesSuggested = records.stream()
                    .map(UnifiedUsageRecord::linesSuggested)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);

                Integer linesAccepted = records.stream()
                    .map(UnifiedUsageRecord::linesAccepted)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);

                rows.add(new GitHubUnregisteredRow(
                    actualGitLogin,
                    githubLogin,
                    lastUsage,
                    linesSuggested > 0 ? linesSuggested : null,
                    linesAccepted > 0 ? linesAccepted : null
                ));
            }
        }

        return rows;
    }

    /**
     * Builds multi-tool user rows for Sheet 3.
     * Only includes users using 2 or more tools with aggregated metrics.
     */
    private List<MultiToolUserRow> buildMultiToolUserRows(
            List<UnifiedUsageRecord> usageRecords,
            List<UnifiedSpendingRecord> spendingRecords) {

        // Group tools by user
        Map<String, Set<ToolType>> userTools = new HashMap<>();
        Map<String, List<UnifiedUsageRecord>> userRecords = new HashMap<>();

        for (UnifiedUsageRecord record : usageRecords) {
            userTools.computeIfAbsent(record.email(), k -> new HashSet<>())
                .add(record.tool());
            userRecords.computeIfAbsent(record.email(), k -> new ArrayList<>())
                .add(record);
        }

        // Group spending by user
        Map<String, BigDecimal> userCosts = spendingRecords.stream()
            .collect(Collectors.groupingBy(
                UnifiedSpendingRecord::email,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    UnifiedSpendingRecord::costUsd,
                    BigDecimal::add
                )
            ));

        List<MultiToolUserRow> rows = new ArrayList<>();

        for (Map.Entry<String, Set<ToolType>> entry : userTools.entrySet()) {
            Set<ToolType> tools = entry.getValue();

            if (tools.size() > 1) {
                String email = entry.getKey();
                List<UnifiedUsageRecord> records = userRecords.get(email);

                // Aggregate metrics across all tools
                Long totalInputTokens = records.stream()
                    .map(UnifiedUsageRecord::inputTokens)
                    .filter(Objects::nonNull)
                    .reduce(0L, Long::sum);

                Long totalOutputTokens = records.stream()
                    .map(UnifiedUsageRecord::outputTokens)
                    .filter(Objects::nonNull)
                    .reduce(0L, Long::sum);

                Long totalCacheTokens = records.stream()
                    .map(UnifiedUsageRecord::cacheReadTokens)
                    .filter(Objects::nonNull)
                    .reduce(0L, Long::sum);

                Long totalTokens = totalInputTokens + totalOutputTokens + totalCacheTokens;

                Integer linesSuggested = records.stream()
                    .map(UnifiedUsageRecord::linesSuggested)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);

                Integer linesAccepted = records.stream()
                    .map(UnifiedUsageRecord::linesAccepted)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);

                BigDecimal totalCost = userCosts.getOrDefault(email, BigDecimal.ZERO);

                rows.add(new MultiToolUserRow(
                    email,
                    tools,
                    tools.size(),
                    tools.contains(ToolType.CLAUDE),
                    tools.contains(ToolType.GITHUB_COPILOT),
                    tools.contains(ToolType.CURSOR),
                    totalTokens > 0 ? totalTokens : null,
                    linesSuggested > 0 ? linesSuggested : null,
                    linesAccepted > 0 ? linesAccepted : null,
                    totalCost.compareTo(BigDecimal.ZERO) > 0 ? totalCost : null
                ));
            }
        }

        // Sort by tool count (desc) then email (asc)
        rows.sort(Comparator.comparing(MultiToolUserRow::toolCount).reversed()
            .thenComparing(MultiToolUserRow::email));

        return rows;
    }

    /**
     * Writes the XLSX file with 6 sheets (3 consolidated + 3 raw data for debugging).
     */
    private void writeXlsxFile(
        Path outputPath,
        List<UserUsageRow> usageRows,
        List<GitHubUnregisteredRow> githubRows,
        List<MultiToolUserRow> multiToolRows,
        List<UnifiedUsageRecord> claudeRawRecords,
        List<UnifiedUsageRecord> githubRawRecords,
        List<UnifiedUsageRecord> cursorRawRecords
    ) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Consolidated sheets
            writeUsageSheet(workbook, usageRows);
            writeGitHubUnregisteredSheet(workbook, githubRows);
            writeMultiToolSheet(workbook, multiToolRows);

            // Raw data sheets (for debugging)
            writeRawDataSheet(workbook, "Claude - Dados Brutos", claudeRawRecords);
            writeRawDataSheet(workbook, "GitHub - Dados Brutos", githubRawRecords);
            writeRawDataSheet(workbook, "Cursor - Snapshot", cursorRawRecords);

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fileOut);
            }
        }
    }

    private void writeUsageSheet(Workbook workbook, List<UserUsageRow> rows) {
        Sheet sheet = workbook.createSheet("Volumes de Uso");

        // Create header style
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle percentStyle = createPercentStyle(workbook);

        // Write headers
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < USAGE_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(USAGE_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Write data
        int rowNum = 1;
        for (UserUsageRow row : rows) {
            Row dataRow = sheet.createRow(rowNum++);

            createCell(dataRow, 0, row.email(), null);
            createCell(dataRow, 1, row.tool().getDisplayName(), null);
            createCell(dataRow, 2, row.lastUsage(), dateStyle);
            createCell(dataRow, 3, row.inputTokens(), numberStyle);
            createCell(dataRow, 4, row.outputTokens(), numberStyle);
            createCell(dataRow, 5, row.cacheReadTokens(), numberStyle);
            createCell(dataRow, 6, row.linesSuggested(), numberStyle);
            createCell(dataRow, 7, row.linesAccepted(), numberStyle);
            createCell(dataRow, 8, row.acceptanceRate(), percentStyle);
            createCell(dataRow, 9, row.costUsd(), currencyStyle);
        }

        // Auto-size columns
        for (int i = 0; i < USAGE_HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeGitHubUnregisteredSheet(Workbook workbook, List<GitHubUnregisteredRow> rows) {
        Sheet sheet = workbook.createSheet("GitHub Não Cadastrados");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);

        // Write headers
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < GITHUB_UNREGISTERED_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(GITHUB_UNREGISTERED_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Write data
        int rowNum = 1;
        for (GitHubUnregisteredRow row : rows) {
            Row dataRow = sheet.createRow(rowNum++);

            createCell(dataRow, 0, row.gitHubLogin(), null);
            createCell(dataRow, 1, row.gitHubEmail(), null);
            createCell(dataRow, 2, row.lastUsage(), dateStyle);
            createCell(dataRow, 3, row.linesSuggested(), numberStyle);
            createCell(dataRow, 4, row.linesAccepted(), numberStyle);
        }

        // Auto-size columns
        for (int i = 0; i < GITHUB_UNREGISTERED_HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeMultiToolSheet(Workbook workbook, List<MultiToolUserRow> rows) {
        Sheet sheet = workbook.createSheet("Usuários Multi-Tool");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        // Write headers
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < MULTI_TOOL_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(MULTI_TOOL_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Write data
        int rowNum = 1;
        for (MultiToolUserRow row : rows) {
            Row dataRow = sheet.createRow(rowNum++);

            createCell(dataRow, 0, row.email(), null);

            String toolsStr = row.tools().stream()
                .map(ToolType::getDisplayName)
                .sorted()
                .collect(Collectors.joining(", "));
            createCell(dataRow, 1, toolsStr, null);

            createCell(dataRow, 2, row.toolCount(), numberStyle);
            createCell(dataRow, 3, row.usesClaude() ? "Sim" : "Não", null);
            createCell(dataRow, 4, row.usesGitHub() ? "Sim" : "Não", null);
            createCell(dataRow, 5, row.usesCursor() ? "Sim" : "Não", null);
            createCell(dataRow, 6, row.totalTokens(), numberStyle);
            createCell(dataRow, 7, row.linesSuggested(), numberStyle);
            createCell(dataRow, 8, row.linesAccepted(), numberStyle);
            createCell(dataRow, 9, row.totalCost(), currencyStyle);
        }

        // Auto-size columns
        for (int i = 0; i < MULTI_TOOL_HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Writes a raw data sheet for debugging purposes.
     * Shows the original data as received from each API.
     */
    private void writeRawDataSheet(Workbook workbook, String sheetName, List<UnifiedUsageRecord> records) {
        Sheet sheet = workbook.createSheet(sheetName);

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);

        // Headers
        String[] headers = {
            "Email", "Data", "Tokens Entrada", "Tokens Saída", "Tokens Cache",
            "Linhas Sugeridas", "Linhas Aceitas", "Taxa Aceitação (%)",
            "GitHub Login", "Metadata"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Write data
        int rowNum = 1;
        for (UnifiedUsageRecord record : records) {
            Row dataRow = sheet.createRow(rowNum++);

            createCell(dataRow, 0, record.email(), null);
            createCell(dataRow, 1, record.date(), dateStyle);
            createCell(dataRow, 2, record.inputTokens(), numberStyle);
            createCell(dataRow, 3, record.outputTokens(), numberStyle);
            createCell(dataRow, 4, record.cacheReadTokens(), numberStyle);
            createCell(dataRow, 5, record.linesSuggested(), numberStyle);
            createCell(dataRow, 6, record.linesAccepted(), numberStyle);
            createCell(dataRow, 7, record.acceptanceRate(), numberStyle);

            // Extract GitHub login from metadata if available
            String githubLogin = "";
            if (record.rawMetadata() != null && record.rawMetadata().containsKey("gitHubLogin")) {
                githubLogin = String.valueOf(record.rawMetadata().get("gitHubLogin"));
            }
            createCell(dataRow, 8, githubLogin, null);

            // Serialize metadata for debugging
            String metadata = "";
            if (record.rawMetadata() != null && !record.rawMetadata().isEmpty()) {
                metadata = record.rawMetadata().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; "));
            }
            createCell(dataRow, 9, metadata, null);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Helper methods for cell creation and styling

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        return style;
    }

    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);

        if (value == null) {
            cell.setBlank();
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof LocalDate) {
            cell.setCellValue(((LocalDate) value).toString());
        }

        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}
