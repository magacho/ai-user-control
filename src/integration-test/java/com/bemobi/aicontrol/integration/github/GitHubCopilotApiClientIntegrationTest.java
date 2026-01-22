package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.BaseIntegrationTest;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.github.GitHubCopilotApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for GitHub Copilot API client with real API calls.
 *
 * Required environment variables:
 * - AI_CONTROL_GITHUB_ENABLED=true
 * - AI_CONTROL_GITHUB_TOKEN=ghp_xxx
 * - AI_CONTROL_GITHUB_ORG=your-organization
 *
 * Run with: mvn verify -P integration-tests
 */
class GitHubCopilotApiClientIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(GitHubCopilotApiClientIntegrationTest.class);

    @Autowired(required = false)
    private GitHubCopilotApiClient githubClient;

    @BeforeEach
    void checkCredentials() {
        // Skip test if credentials are not configured
        assumeTrue(isConfigured("AI_CONTROL_GITHUB_TOKEN"),
            "Skipping test: AI_CONTROL_GITHUB_TOKEN not configured");
        assumeTrue(isConfigured("AI_CONTROL_GITHUB_ORG"),
            "Skipping test: AI_CONTROL_GITHUB_ORG not configured");
        assumeTrue(githubClient != null,
            "Skipping test: GitHubCopilotApiClient bean not available (check AI_CONTROL_GITHUB_ENABLED)");
    }

    @Test
    void testRealApiConnection() {
        log.info("=== Testing REAL GitHub Copilot API Connection ===");

        ConnectionTestResult result = githubClient.testConnection();

        log.info("Tool: {}", result.getToolName());
        log.info("Success: {}", result.isSuccess());
        log.info("Message: {}", result.getMessage());

        assertTrue(result.isSuccess(),
            "Connection test should succeed with valid credentials");
        assertEquals("github-copilot", result.getToolName());
    }

    @Test
    void testRealFetchUsers() throws ApiClientException {
        log.info("=== Fetching REAL Copilot Seats from GitHub ===");

        List<UserData> users = githubClient.fetchUsers();

        assertNotNull(users, "User list should not be null");
        log.info("Total Copilot seats fetched: {}", users.size());

        // If there are users, validate structure
        if (!users.isEmpty()) {
            UserData firstUser = users.get(0);

            assertNotNull(firstUser.getEmail(), "Email should not be null");
            assertNotNull(firstUser.getName(), "Name should not be null");
            assertNotNull(firstUser.getStatus(), "Status should not be null");
            assertEquals("active", firstUser.getStatus(), "All seats should be active");

            log.debug("Sample seat:");
            log.debug("  Email: {}", firstUser.getEmail());
            log.debug("  Name: {}", firstUser.getName());
            log.debug("  Status: {}", firstUser.getStatus());
            log.debug("  Last Activity: {}", firstUser.getLastActivityAt());

            if (firstUser.getAdditionalMetrics() != null && !firstUser.getAdditionalMetrics().isEmpty()) {
                log.debug("  Additional Metrics:");
                firstUser.getAdditionalMetrics().forEach((key, value) ->
                    log.debug("    {}: {}", key, value));

                // Validate GitHub-specific metrics
                assertTrue(firstUser.getAdditionalMetrics().containsKey("github_login"),
                    "Should have github_login metric");
                assertTrue(firstUser.getAdditionalMetrics().containsKey("last_activity_editor"),
                    "Should have last_activity_editor metric");
            }

            // Check for fallback email pattern
            if (firstUser.getEmail().endsWith("@github.local")) {
                log.info("Note: Using fallback email (public email not available)");
            }
        } else {
            log.info("No Copilot seats found (organization may not have Copilot enabled)");
        }
    }

    @Test
    void testClientMetadata() {
        log.info("=== Testing Client Metadata ===");

        assertEquals("github-copilot", githubClient.getToolName());
        assertEquals("GitHub Copilot", githubClient.getDisplayName());
        assertTrue(githubClient.isEnabled());

        log.debug("Tool Name: {}", githubClient.getToolName());
        log.debug("Display Name: {}", githubClient.getDisplayName());
        log.debug("Enabled: {}", githubClient.isEnabled());
    }
}
