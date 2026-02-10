package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.BaseIntegrationTest;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.cursor.CursorCsvClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for Cursor CSV client with real CSV files.
 *
 * Required environment variables:
 * - AI_CONTROL_CURSOR_ENABLED=true
 * - AI_CONTROL_CURSOR_CSV_PATH=/path/to/csv/directory
 *
 * The directory should contain at least one CSV file with the expected format:
 * email,name,status,last_active,joined_at
 *
 * Run with: mvn verify -P integration-tests
 */
class CursorCsvClientIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CursorCsvClientIntegrationTest.class);

    @Autowired(required = false)
    private CursorCsvClient cursorClient;

    @BeforeEach
    void checkConfiguration() {
        // Skip test if not configured
        assumeTrue(isConfigured("AI_CONTROL_CURSOR_CSV_PATH"),
            "Skipping test: AI_CONTROL_CURSOR_CSV_PATH not configured");
        assumeTrue(cursorClient != null,
            "Skipping test: CursorCsvClient bean not available (check AI_CONTROL_CURSOR_ENABLED)");

        // Check if directory exists
        String csvPath = getEnv("AI_CONTROL_CURSOR_CSV_PATH");
        Path path = Paths.get(csvPath);
        assumeTrue(Files.exists(path),
            "Skipping test: CSV directory does not exist: " + csvPath);
        assumeTrue(Files.isDirectory(path),
            "Skipping test: CSV path is not a directory: " + csvPath);
    }

    @Test
    void testRealDirectoryConnection() {
        log.info("=== Testing REAL Cursor CSV Directory Access ===");

        ConnectionTestResult result = cursorClient.testConnection();

        log.info("Tool: {}", result.toolName());
        log.info("Success: {}", result.success());
        log.info("Message: {}", result.message());

        assertTrue(result.success(),
            "Connection test should succeed with configured directory");
        assertEquals("cursor", result.toolName());
    }

    @Test
    void testRealFindLatestCsvFile() {
        log.info("=== Finding Latest CSV File ===");

        try {
            String latestFile = cursorClient.findLatestCsvFile();

            assertNotNull(latestFile, "Latest file should not be null");
            assertTrue(latestFile.endsWith(".csv"), "File should be a CSV");
            assertTrue(Files.exists(Paths.get(latestFile)), "File should exist");

            log.info("Latest CSV file: {}", latestFile);
            log.debug("File size: {} bytes", Files.size(Paths.get(latestFile)));
            log.debug("Last modified: {}", Files.getLastModifiedTime(Paths.get(latestFile)));

        } catch (ApiClientException e) {
            fail("Should find at least one CSV file in configured directory", e);
        } catch (IOException e) {
            fail("Error accessing file system", e);
        }
    }

    @Test
    void testRealImportFromLatestCsv() {
        log.info("=== Importing Users from Latest CSV ===");

        try {
            // Find latest CSV
            String csvFile = cursorClient.findLatestCsvFile();
            log.info("Using file: {}", csvFile);

            // Import users
            List<UserData> users = cursorClient.importFromCsv(csvFile);

            assertNotNull(users, "User list should not be null");
            log.info("Total users imported: {}", users.size());

            // If there are users, validate structure
            if (!users.isEmpty()) {
                UserData firstUser = users.get(0);

                assertNotNull(firstUser.getEmail(), "Email should not be null");
                assertNotNull(firstUser.getName(), "Name should not be null");
                assertNotNull(firstUser.getStatus(), "Status should not be null");

                log.debug("Sample user:");
                log.debug("  Email: {}", firstUser.getEmail());
                log.debug("  Name: {}", firstUser.getName());
                log.debug("  Status: {}", firstUser.getStatus());
                log.debug("  Last Activity: {}", firstUser.getLastActivityAt());

                if (firstUser.getAdditionalMetrics() != null && !firstUser.getAdditionalMetrics().isEmpty()) {
                    log.debug("  Additional Metrics:");
                    firstUser.getAdditionalMetrics().forEach((key, value) ->
                        log.debug("    {}: {}", key, value));
                }

                // Validate email format
                assertTrue(firstUser.getEmail().contains("@"),
                    "Email should contain @");
                assertTrue(firstUser.getEmail().equals(firstUser.getEmail().toLowerCase()),
                    "Email should be lowercase");

            } else {
                log.info("CSV file is empty (no users)");
            }

        } catch (ApiClientException e) {
            fail("Import should succeed with valid CSV file", e);
        }
    }

    @Test
    void testClientMetadata() {
        log.info("=== Testing Client Metadata ===");

        assertEquals("cursor", cursorClient.getToolName());
        assertEquals("Cursor", cursorClient.getDisplayName());
        assertTrue(cursorClient.isEnabled());

        log.debug("Tool Name: {}", cursorClient.getToolName());
        log.debug("Display Name: {}", cursorClient.getDisplayName());
        log.debug("Enabled: {}", cursorClient.isEnabled());
    }

    @Test
    void testUnsupportedFetchUsers() {
        log.info("=== Testing Unsupported fetchUsers() Method ===");

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> cursorClient.fetchUsers()
        );

        assertTrue(exception.getMessage().contains("CSV import"),
            "Should mention CSV import in error message");

        log.debug("Expected exception: {}", exception.getMessage());
    }
}
