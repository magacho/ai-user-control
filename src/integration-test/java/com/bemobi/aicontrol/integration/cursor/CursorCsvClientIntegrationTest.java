package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.BaseIntegrationTest;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.cursor.CursorCsvClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        System.out.println("\n=== Testing REAL Cursor CSV Directory Access ===");

        ConnectionTestResult result = cursorClient.testConnection();

        System.out.println("Tool: " + result.getToolName());
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Message: " + result.getMessage());

        assertTrue(result.isSuccess(),
            "Connection test should succeed with configured directory");
        assertEquals("cursor", result.getToolName());
    }

    @Test
    void testRealFindLatestCsvFile() {
        System.out.println("\n=== Finding Latest CSV File ===");

        try {
            String latestFile = cursorClient.findLatestCsvFile();

            assertNotNull(latestFile, "Latest file should not be null");
            assertTrue(latestFile.endsWith(".csv"), "File should be a CSV");
            assertTrue(Files.exists(Paths.get(latestFile)), "File should exist");

            System.out.println("Latest CSV file: " + latestFile);
            System.out.println("File size: " + Files.size(Paths.get(latestFile)) + " bytes");
            System.out.println("Last modified: " + Files.getLastModifiedTime(Paths.get(latestFile)));

        } catch (ApiClientException e) {
            fail("Should find at least one CSV file in configured directory", e);
        } catch (IOException e) {
            fail("Error accessing file system", e);
        }
    }

    @Test
    void testRealImportFromLatestCsv() {
        System.out.println("\n=== Importing Users from Latest CSV ===");

        try {
            // Find latest CSV
            String csvFile = cursorClient.findLatestCsvFile();
            System.out.println("Using file: " + csvFile);

            // Import users
            List<UserData> users = cursorClient.importFromCsv(csvFile);

            assertNotNull(users, "User list should not be null");
            System.out.println("Total users imported: " + users.size());

            // If there are users, validate structure
            if (!users.isEmpty()) {
                UserData firstUser = users.get(0);

                assertNotNull(firstUser.getEmail(), "Email should not be null");
                assertNotNull(firstUser.getName(), "Name should not be null");
                assertNotNull(firstUser.getStatus(), "Status should not be null");

                System.out.println("\nSample user:");
                System.out.println("  Email: " + firstUser.getEmail());
                System.out.println("  Name: " + firstUser.getName());
                System.out.println("  Status: " + firstUser.getStatus());
                System.out.println("  Last Activity: " + firstUser.getLastActivityAt());

                if (firstUser.getAdditionalMetrics() != null && !firstUser.getAdditionalMetrics().isEmpty()) {
                    System.out.println("  Additional Metrics:");
                    firstUser.getAdditionalMetrics().forEach((key, value) ->
                        System.out.println("    " + key + ": " + value));
                }

                // Validate email format
                assertTrue(firstUser.getEmail().contains("@"),
                    "Email should contain @");
                assertTrue(firstUser.getEmail().equals(firstUser.getEmail().toLowerCase()),
                    "Email should be lowercase");

            } else {
                System.out.println("CSV file is empty (no users)");
            }

        } catch (ApiClientException e) {
            fail("Import should succeed with valid CSV file", e);
        }
    }

    @Test
    void testClientMetadata() {
        System.out.println("\n=== Testing Client Metadata ===");

        assertEquals("cursor", cursorClient.getToolName());
        assertEquals("Cursor", cursorClient.getDisplayName());
        assertTrue(cursorClient.isEnabled());

        System.out.println("Tool Name: " + cursorClient.getToolName());
        System.out.println("Display Name: " + cursorClient.getDisplayName());
        System.out.println("Enabled: " + cursorClient.isEnabled());
    }

    @Test
    void testUnsupportedFetchUsers() {
        System.out.println("\n=== Testing Unsupported fetchUsers() Method ===");

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> cursorClient.fetchUsers()
        );

        assertTrue(exception.getMessage().contains("CSV import"),
            "Should mention CSV import in error message");

        System.out.println("Expected exception: " + exception.getMessage());
    }
}
