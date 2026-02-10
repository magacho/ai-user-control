package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.UserData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for exporting user data to CSV files.
 *
 * This service generates CSV files for each AI tool integration with collected user data,
 * using Apache Commons CSV for formatting.
 */
@Service
public class CsvExportService {

    private static final Logger log = LoggerFactory.getLogger(CsvExportService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter CSV_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String[] CSV_HEADERS = {
            "tool", "email", "name", "status", "last_activity_at", "collected_at", "email_type"
    };

    private final String outputDirectory;

    /**
     * Constructs the CsvExportService with configurable output directory.
     *
     * @param outputDirectory Directory where CSV files will be saved
     */
    public CsvExportService(
            @Value("${ai-control.export.output-directory:./output}") String outputDirectory) {
        this.outputDirectory = outputDirectory;
        log.info("CsvExportService initialized with output directory: {}", outputDirectory);
    }

    /**
     * Exports user data to CSV files, one file per tool.
     *
     * @param userData Map with tool names as keys and lists of UserData as values
     * @return List of paths to the generated CSV files
     * @throws IOException if there's an error writing the CSV files
     */
    public List<Path> exportToCsv(Map<String, List<UserData>> userData) throws IOException {
        log.info("Starting CSV export for {} tools", userData.size());
        ensureOutputDirectoryExists();

        List<Path> generatedFiles = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        LocalDateTime collectedAt = LocalDateTime.now();

        for (Map.Entry<String, List<UserData>> entry : userData.entrySet()) {
            String toolName = entry.getKey();
            List<UserData> users = entry.getValue();

            if (users.isEmpty()) {
                log.info("Skipping CSV export for {} - no users to export", toolName);
                continue;
            }

            Path csvFile = generateCsvFile(toolName, users, timestamp, collectedAt);
            generatedFiles.add(csvFile);
        }

        log.info("CSV export completed. Generated {} files", generatedFiles.size());
        return generatedFiles;
    }

    /**
     * Exports all user data to a single consolidated CSV file.
     *
     * @param userData Map with tool names as keys and lists of UserData as values
     * @return Path to the consolidated CSV file
     * @throws IOException if there's an error writing the CSV file
     */
    public Path exportToConsolidatedCsv(Map<String, List<UserData>> userData) throws IOException {
        log.info("Starting consolidated CSV export");
        ensureOutputDirectoryExists();

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        LocalDateTime collectedAt = LocalDateTime.now();
        String filename = String.format("all-users-consolidated-%s.csv", timestamp);
        Path outputPath = Paths.get(outputDirectory, filename);

        int totalUsers = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            for (Map.Entry<String, List<UserData>> entry : userData.entrySet()) {
                String toolName = entry.getKey();
                for (UserData user : entry.getValue()) {
                    writeUserRecord(csvPrinter, toolName, user, collectedAt);
                    totalUsers++;
                }
            }
        }

        long fileSize = Files.size(outputPath);
        log.info("Created consolidated CSV: {} ({} users, {} bytes)", filename, totalUsers, fileSize);

        return outputPath;
    }

    /**
     * Generates a CSV file for a specific tool's user data.
     *
     * @param toolName Tool name (used in filename)
     * @param users List of users to export
     * @param timestamp Timestamp string for filename
     * @param collectedAt Collection timestamp for CSV records
     * @return Path to the generated CSV file
     * @throws IOException if there's an error writing the file
     */
    private Path generateCsvFile(String toolName, List<UserData> users, String timestamp, LocalDateTime collectedAt)
            throws IOException {
        String filename = String.format("%s-users-%s.csv", toolName, timestamp);
        Path outputPath = Paths.get(outputDirectory, filename);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            for (UserData user : users) {
                writeUserRecord(csvPrinter, toolName, user, collectedAt);
            }
        }

        long fileSize = Files.size(outputPath);
        log.info("Created: {} ({} users, {} bytes)", filename, users.size(), fileSize);

        return outputPath;
    }

    /**
     * Writes a single user record to the CSV printer.
     *
     * @param csvPrinter CSV printer instance
     * @param toolName Tool name for this record
     * @param user UserData to write
     * @param collectedAt Collection timestamp
     * @throws IOException if there's an error writing the record
     */
    private void writeUserRecord(CSVPrinter csvPrinter, String toolName, UserData user, LocalDateTime collectedAt)
            throws IOException {
        // Get email_type from additional metrics (for GitHub Copilot)
        String emailType = "";
        if (user.additionalMetrics() != null && user.additionalMetrics().containsKey("email_type")) {
            emailType = user.additionalMetrics().get("email_type").toString();
        }

        csvPrinter.printRecord(
                toolName,
                user.email() != null ? user.email() : "",
                user.name() != null ? user.name() : "",
                user.status() != null ? user.status() : "",
                user.lastActivityAt() != null ? user.lastActivityAt().format(CSV_DATETIME_FORMATTER) : "",
                collectedAt.format(CSV_DATETIME_FORMATTER),
                emailType
        );
    }

    /**
     * Ensures the output directory exists, creating it if necessary.
     *
     * @throws IOException if the directory cannot be created
     */
    private void ensureOutputDirectoryExists() throws IOException {
        Path dirPath = Paths.get(outputDirectory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("Created output directory: {}", outputDirectory);
        }
    }

    /**
     * Gets the configured output directory.
     *
     * @return Output directory path
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }
}
