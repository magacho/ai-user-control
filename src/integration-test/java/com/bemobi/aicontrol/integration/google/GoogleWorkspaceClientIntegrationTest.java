package com.bemobi.aicontrol.integration.google;

import com.bemobi.aicontrol.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for Google Workspace client with real API calls.
 *
 * Required environment variables:
 * - AI_CONTROL_WORKSPACE_ENABLED=true
 * - AI_CONTROL_WORKSPACE_CREDENTIALS=path or inline JSON
 * - AI_CONTROL_WORKSPACE_DOMAIN=your-domain.com
 * - AI_CONTROL_WORKSPACE_ADMIN_EMAIL=admin@your-domain.com
 *
 * Run with: mvn verify -P integration-tests
 */
class GoogleWorkspaceClientIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(GoogleWorkspaceClientIntegrationTest.class);

    @Autowired(required = false)
    private GoogleWorkspaceClient workspaceClient;

    @BeforeEach
    void checkCredentials() {
        assumeTrue(isConfigured("AI_CONTROL_WORKSPACE_CREDENTIALS"),
            "Skipping test: AI_CONTROL_WORKSPACE_CREDENTIALS not configured");
        assumeTrue(isConfigured("AI_CONTROL_WORKSPACE_DOMAIN"),
            "Skipping test: AI_CONTROL_WORKSPACE_DOMAIN not configured");
        assumeTrue(workspaceClient != null,
            "Skipping test: GoogleWorkspaceClient bean not available (check AI_CONTROL_WORKSPACE_ENABLED)");
    }

    @Test
    void testFindEmailByGitName_existingUser() {
        log.info("=== Testing Workspace email resolution ===");

        // This test requires a real user with git_name set in Workspace
        String testGitLogin = System.getenv("AI_CONTROL_WORKSPACE_TEST_GIT_LOGIN");
        assumeTrue(testGitLogin != null && !testGitLogin.isBlank(),
            "Skipping test: AI_CONTROL_WORKSPACE_TEST_GIT_LOGIN not configured");

        Optional<String> email = workspaceClient.findEmailByGitName(testGitLogin);

        log.info("Lookup for '{}': {}", testGitLogin, email.orElse("(not found)"));

        assertTrue(email.isPresent(), "Should find email for configured test user");
        assertFalse(email.get().isBlank(), "Email should not be blank");
    }

    @Test
    void testFindEmailByGitName_nonExistentUser() {
        log.info("=== Testing Workspace lookup for non-existent user ===");

        Optional<String> email = workspaceClient.findEmailByGitName("nonexistent_user_xyz_12345");

        log.info("Lookup for non-existent user: {}", email.orElse("(not found)"));

        assertFalse(email.isPresent(), "Should not find email for non-existent user");
    }
}
