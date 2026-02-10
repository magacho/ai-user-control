package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.BaseIntegrationTest;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for Cursor Admin API client with real API calls.
 *
 * Required environment variables:
 * - AI_CONTROL_CURSOR_ENABLED=true
 * - AI_CONTROL_CURSOR_TOKEN=cur_***
 *
 * Run with: mvn verify -P integration-tests -DskipTests -Dit.test=CursorApiClientIntegrationTest
 */
class CursorApiClientIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CursorApiClientIntegrationTest.class);

    @Autowired(required = false)
    private CursorApiClient cursorClient;

    @BeforeEach
    void checkCredentials() {
        // Skip test if credentials are not configured
        assumeTrue(isConfigured("AI_CONTROL_CURSOR_TOKEN"),
            "Skipping test: AI_CONTROL_CURSOR_TOKEN not configured");
        assumeTrue(cursorClient != null,
            "Skipping test: CursorApiClient bean not available (check AI_CONTROL_CURSOR_ENABLED)");
    }

    @Test
    void testRealApiConnection() {
        log.info("=== Testing REAL Cursor Admin API Connection ===");

        ConnectionTestResult result = cursorClient.testConnection();

        log.info("Tool: {}", result.toolName());
        log.info("Success: {}", result.success());
        log.info("Message: {}", result.message());

        assertTrue(result.success(),
            "Connection test should succeed with valid credentials");
        assertEquals("cursor", result.toolName());
    }

    @Test
    void testRealFetchUsers() throws ApiClientException {
        log.info("=== Fetching REAL Team Members from Cursor ===");

        List<UserData> users = cursorClient.fetchUsers();

        assertNotNull(users, "User list should not be null");
        log.info("Total users fetched: {}", users.size());
        log.info("==========================================");

        // If there are users, validate structure and log all users verbosely
        if (!users.isEmpty()) {
            // Validate first user structure
            UserData firstUser = users.get(0);
            assertNotNull(firstUser.email(), "Email should not be null");
            assertNotNull(firstUser.name(), "Name should not be null");

            // Log ALL users verbosely
            for (int i = 0; i < users.size(); i++) {
                UserData user = users.get(i);
                log.info("User #{}: ", i + 1);
                log.info("  Email: {}", user.email());
                log.info("  Name: {}", user.name());
                log.info("  Status: {}", user.status() != null ? user.status() : "active");
                log.info("  Last Activity: {}", user.lastActivityAt() != null ? user.lastActivityAt() : "null");

                if (user.additionalMetrics() != null && !user.additionalMetrics().isEmpty()) {
                    log.info("  Additional Metrics:");
                    user.additionalMetrics().forEach((key, value) ->
                        log.info("    {}: {}", key, value));
                } else {
                    log.info("  Additional Metrics: none");
                }
                log.info("------------------------------------------");
            }
            log.info("==========================================");
        } else {
            log.info("No users found in team (this is valid if team is empty)");
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
}
