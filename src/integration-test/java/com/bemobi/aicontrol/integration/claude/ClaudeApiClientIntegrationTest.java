package com.bemobi.aicontrol.integration.claude;

import com.bemobi.aicontrol.integration.BaseIntegrationTest;
import com.bemobi.aicontrol.integration.claude.ClaudeApiClient;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for Claude Code Admin API client with real API calls.
 *
 * Required environment variables:
 * - AI_CONTROL_CLAUDE_ENABLED=true
 * - AI_CONTROL_CLAUDE_TOKEN=sk-ant-admin-xxx (Admin API Key, NOT regular API key)
 *
 * Note: Regular API keys (sk-ant-api03-...) will NOT work.
 * You must use an Admin API key (sk-ant-admin-...).
 *
 * Run with: mvn verify -P integration-tests -DskipTests -Dit.test=ClaudeApiClientIntegrationTest
 */
class ClaudeApiClientIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiClientIntegrationTest.class);

    @Autowired(required = false)
    private ClaudeApiClient claudeClient;

    @BeforeEach
    void checkCredentials() {
        // Skip test if credentials are not configured
        assumeTrue(isConfigured("AI_CONTROL_CLAUDE_TOKEN"),
            "Skipping test: AI_CONTROL_CLAUDE_TOKEN not configured");
        assumeTrue(claudeClient != null,
            "Skipping test: ClaudeApiClient bean not available (check AI_CONTROL_CLAUDE_ENABLED)");

        // Verify it's an Admin API key (starts with sk-ant-admin)
        String token = System.getenv("AI_CONTROL_CLAUDE_TOKEN");
        assumeTrue(token != null && token.startsWith("sk-ant-admin"),
            "Skipping test: Must use Admin API key (sk-ant-admin...), not regular API key (sk-ant-api03...)");
    }

    @Test
    void testRealApiConnection() {
        log.info("=== Testing REAL Claude Code API Connection ===");

        ConnectionTestResult result = claudeClient.testConnection();

        log.info("Tool: {}", result.getToolName());
        log.info("Success: {}", result.isSuccess());
        log.info("Message: {}", result.getMessage());

        assertTrue(result.isSuccess(),
            "Connection test should succeed with valid credentials");
        assertEquals("claude-code", result.getToolName());
    }

    @Test
    void testRealFetchUsers() throws ApiClientException {
        log.info("=== Fetching REAL Users from Claude Code ===");

        List<UserData> users = claudeClient.fetchUsers();

        assertNotNull(users, "User list should not be null");
        log.info("Total users fetched: {}", users.size());
        log.info("==========================================");

        // If there are users, validate structure and log all users verbosely
        if (!users.isEmpty()) {
            // Validate first user structure
            UserData firstUser = users.get(0);
            assertNotNull(firstUser.getEmail(), "Email should not be null");
            assertNotNull(firstUser.getName(), "Name should not be null");
            // Status may be null for some users in the Admin API

            // Log ALL users verbosely
            for (int i = 0; i < users.size(); i++) {
                UserData user = users.get(i);
                log.info("User #{}: ", i + 1);
                log.info("  Email: {}", user.getEmail());
                log.info("  Name: {}", user.getName());
                log.info("  Status: {}", user.getStatus() != null ? user.getStatus() : "null");
                log.info("  Last Activity: {}", user.getLastActivityAt() != null ? user.getLastActivityAt() : "null");

                if (user.getAdditionalMetrics() != null && !user.getAdditionalMetrics().isEmpty()) {
                    log.info("  Additional Metrics:");
                    user.getAdditionalMetrics().forEach((key, value) ->
                        log.info("    {}: {}", key, value));
                } else {
                    log.info("  Additional Metrics: none");
                }
                log.info("------------------------------------------");
            }
            log.info("==========================================");
        } else {
            log.info("No users found in organization (this is valid if org is empty)");
        }
    }

    @Test
    void testClientMetadata() {
        log.info("=== Testing Client Metadata ===");

        assertEquals("claude-code", claudeClient.getToolName());
        assertEquals("Claude Code", claudeClient.getDisplayName());
        assertTrue(claudeClient.isEnabled());

        log.debug("Tool Name: {}", claudeClient.getToolName());
        log.debug("Display Name: {}", claudeClient.getDisplayName());
        log.debug("Enabled: {}", claudeClient.isEnabled());
    }
}
