package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.BaseIntegrationTest;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.integration.github.GitHubCopilotApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        System.out.println("\n=== Testing REAL GitHub Copilot API Connection ===");

        ConnectionTestResult result = githubClient.testConnection();

        System.out.println("Tool: " + result.getToolName());
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Message: " + result.getMessage());

        assertTrue(result.isSuccess(),
            "Connection test should succeed with valid credentials");
        assertEquals("github-copilot", result.getToolName());
    }

    @Test
    void testRealFetchUsers() throws ApiClientException {
        System.out.println("\n=== Fetching REAL Copilot Seats from GitHub ===");

        List<UserData> users = githubClient.fetchUsers();

        assertNotNull(users, "User list should not be null");
        System.out.println("Total Copilot seats fetched: " + users.size());

        // If there are users, validate structure
        if (!users.isEmpty()) {
            UserData firstUser = users.get(0);

            assertNotNull(firstUser.getEmail(), "Email should not be null");
            assertNotNull(firstUser.getName(), "Name should not be null");
            assertNotNull(firstUser.getStatus(), "Status should not be null");
            assertEquals("active", firstUser.getStatus(), "All seats should be active");

            System.out.println("\nSample seat:");
            System.out.println("  Email: " + firstUser.getEmail());
            System.out.println("  Name: " + firstUser.getName());
            System.out.println("  Status: " + firstUser.getStatus());
            System.out.println("  Last Activity: " + firstUser.getLastActivityAt());

            if (firstUser.getAdditionalMetrics() != null && !firstUser.getAdditionalMetrics().isEmpty()) {
                System.out.println("  Additional Metrics:");
                firstUser.getAdditionalMetrics().forEach((key, value) ->
                    System.out.println("    " + key + ": " + value));

                // Validate GitHub-specific metrics
                assertTrue(firstUser.getAdditionalMetrics().containsKey("github_login"),
                    "Should have github_login metric");
                assertTrue(firstUser.getAdditionalMetrics().containsKey("last_activity_editor"),
                    "Should have last_activity_editor metric");
            }

            // Check for fallback email pattern
            if (firstUser.getEmail().endsWith("@github.local")) {
                System.out.println("  Note: Using fallback email (public email not available)");
            }
        } else {
            System.out.println("No Copilot seats found (organization may not have Copilot enabled)");
        }
    }

    @Test
    void testClientMetadata() {
        System.out.println("\n=== Testing Client Metadata ===");

        assertEquals("github-copilot", githubClient.getToolName());
        assertEquals("GitHub Copilot", githubClient.getDisplayName());
        assertTrue(githubClient.isEnabled());

        System.out.println("Tool Name: " + githubClient.getToolName());
        System.out.println("Display Name: " + githubClient.getDisplayName());
        System.out.println("Enabled: " + githubClient.isEnabled());
    }
}
