package com.bemobi.aicontrol.integration.claude;

import com.bemobi.aicontrol.integration.BaseIntegrationTest;
import com.bemobi.aicontrol.integration.claude.ClaudeApiClient;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for Claude Code API client with real API calls.
 *
 * Required environment variables:
 * - AI_CONTROL_CLAUDE_ENABLED=true
 * - AI_CONTROL_CLAUDE_TOKEN=sk-ant-api03-xxx
 * - AI_CONTROL_CLAUDE_ORG_ID=org_xxx
 *
 * Run with: mvn verify -P integration-tests
 */
class ClaudeApiClientIntegrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private ClaudeApiClient claudeClient;

    @BeforeEach
    void checkCredentials() {
        // Skip test if credentials are not configured
        assumeTrue(isConfigured("AI_CONTROL_CLAUDE_TOKEN"),
            "Skipping test: AI_CONTROL_CLAUDE_TOKEN not configured");
        assumeTrue(isConfigured("AI_CONTROL_CLAUDE_ORG_ID"),
            "Skipping test: AI_CONTROL_CLAUDE_ORG_ID not configured");
        assumeTrue(claudeClient != null,
            "Skipping test: ClaudeApiClient bean not available (check AI_CONTROL_CLAUDE_ENABLED)");
    }

    @Test
    void testRealApiConnection() {
        System.out.println("\n=== Testing REAL Claude Code API Connection ===");

        ConnectionTestResult result = claudeClient.testConnection();

        System.out.println("Tool: " + result.getToolName());
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Message: " + result.getMessage());

        assertTrue(result.isSuccess(),
            "Connection test should succeed with valid credentials");
        assertEquals("claude-code", result.getToolName());
    }

    @Test
    void testRealFetchUsers() throws ApiClientException {
        System.out.println("\n=== Fetching REAL Users from Claude Code ===");

        List<UserData> users = claudeClient.fetchUsers();

        assertNotNull(users, "User list should not be null");
        System.out.println("Total users fetched: " + users.size());

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
        } else {
            System.out.println("No users found in organization (this is valid if org is empty)");
        }
    }

    @Test
    void testClientMetadata() {
        System.out.println("\n=== Testing Client Metadata ===");

        assertEquals("claude-code", claudeClient.getToolName());
        assertEquals("Claude Code", claudeClient.getDisplayName());
        assertTrue(claudeClient.isEnabled());

        System.out.println("Tool Name: " + claudeClient.getToolName());
        System.out.println("Display Name: " + claudeClient.getDisplayName());
        System.out.println("Enabled: " + claudeClient.isEnabled());
    }
}
