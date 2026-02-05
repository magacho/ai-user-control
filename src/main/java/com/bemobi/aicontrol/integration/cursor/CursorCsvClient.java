package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.ToolApiClient;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Client for Cursor integration via CSV import.
 *
 * Since Cursor doesn't have a public API, this client imports user data
 * from CSV files exported from the Cursor admin dashboard.
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.cursor.csv-import", name = "enabled", havingValue = "true")
public class CursorCsvClient implements ToolApiClient {

    private static final Logger log = LoggerFactory.getLogger(CursorCsvClient.class);

    private final CursorApiProperties properties;

    public CursorCsvClient(CursorApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getToolName() {
        return "cursor";
    }

    @Override
    public String getDisplayName() {
        return "Cursor";
    }

    @Override
    public List<UserData> fetchUsers() throws ApiClientException {
        throw new UnsupportedOperationException(
            "Cursor integration uses CSV import. Use 'importFromCsv' method instead."
        );
    }

    /**
     * Import users from CSV file
     */
    public List<UserData> importFromCsv(String filePath) throws ApiClientException {
        log.info("Importing Cursor users from CSV: {}", filePath);

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new ApiClientException("CSV file not found: " + filePath);
        }

        List<UserData> users = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim());

            for (CSVRecord record : csvParser) {
                UserData userData = parseCsvRecord(record);
                users.add(userData);
            }

            log.info("Successfully imported {} users from Cursor CSV", users.size());
            return users;

        } catch (IOException e) {
            log.error("Error reading CSV file: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to read CSV file", e);
        } catch (Exception e) {
            log.error("Error parsing CSV: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to parse CSV file", e);
        }
    }

    /**
     * Find most recent CSV file in default directory
     */
    public String findLatestCsvFile() throws ApiClientException {
        if (properties.getCsvPath() == null || properties.getCsvPath().isEmpty()) {
            throw new ApiClientException("CSV path not configured");
        }

        Path csvDir = Paths.get(properties.getCsvPath());

        if (!Files.exists(csvDir)) {
            throw new ApiClientException("CSV import directory not found: " + csvDir);
        }

        try (Stream<Path> files = Files.list(csvDir)) {
            return files
                .filter(path -> path.toString().endsWith(".csv"))
                .max(Comparator.comparingLong(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (IOException e) {
                        return 0;
                    }
                }))
                .map(Path::toString)
                .orElseThrow(() -> new ApiClientException("No CSV files found in " + csvDir));

        } catch (IOException e) {
            throw new ApiClientException("Error searching for CSV files", e);
        }
    }

    @Override
    public ConnectionTestResult testConnection() {
        try {
            if (properties.getCsvPath() == null || properties.getCsvPath().isEmpty()) {
                return ConnectionTestResult.failure(getToolName(),
                    "CSV path not configured");
            }

            Path csvDir = Paths.get(properties.getCsvPath());
            if (Files.exists(csvDir) && Files.isDirectory(csvDir)) {
                return ConnectionTestResult.success(getToolName(),
                    "CSV import directory is accessible");
            } else {
                return ConnectionTestResult.failure(getToolName(),
                    "CSV import directory not found or not accessible");
            }
        } catch (Exception e) {
            return ConnectionTestResult.failure(getToolName(), e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private UserData parseCsvRecord(CSVRecord record) {
        UserData userData = new UserData();

        // Email (obrigatório)
        String email = record.get("email");
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required on line " + record.getRecordNumber());
        }
        userData.setEmail(email.toLowerCase().trim());

        // Name (obrigatório)
        String name = record.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required on line " + record.getRecordNumber());
        }
        userData.setName(name.trim());

        // Status (opcional, default: active)
        String status = record.isMapped("status") ? record.get("status") : "active";
        userData.setStatus(status != null && !status.isEmpty() ? status : "active");

        // Last Active (opcional)
        if (record.isMapped("last_active")) {
            String lastActive = record.get("last_active");
            if (lastActive != null && !lastActive.isEmpty()) {
                userData.setLastActivityAt(LocalDate.parse(lastActive).atStartOfDay());
            }
        }

        // Additional metrics
        Map<String, Object> metrics = new HashMap<>();
        if (record.isMapped("joined_at")) {
            String joinedAt = record.get("joined_at");
            if (joinedAt != null && !joinedAt.isEmpty()) {
                metrics.put("joined_at", LocalDate.parse(joinedAt).atStartOfDay());
            }
        }
        userData.setAdditionalMetrics(metrics);

        return userData;
    }
}
