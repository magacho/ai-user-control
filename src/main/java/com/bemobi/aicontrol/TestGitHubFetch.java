package com.bemobi.aicontrol;

import com.bemobi.aicontrol.integration.github.GitHubCopilotApiClient;
import com.bemobi.aicontrol.integration.github.GitHubApiProperties;
import com.bemobi.aicontrol.integration.common.UserData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Quick test to check GitHub user fetching
 */
public class TestGitHubFetch {
    public static void main(String[] args) {
        try {
            // Read from environment variables
            String token = System.getenv("AI_CONTROL_GITHUB_TOKEN");
            String org = System.getenv("AI_CONTROL_GITHUB_ORG");

            if (token == null || token.isEmpty()) {
                System.err.println("❌ AI_CONTROL_GITHUB_TOKEN not set!");
                System.exit(1);
            }

            if (org == null || org.isEmpty()) {
                System.err.println("❌ AI_CONTROL_GITHUB_ORG not set!");
                System.exit(1);
            }

            System.out.println("=".repeat(60));
            System.out.println("Testing GitHub Copilot User Fetch");
            System.out.println("Organization: " + org);
            System.out.println("=".repeat(60));

            // Configure properties
            GitHubApiProperties props = new GitHubApiProperties();
            props.setEnabled(true);
            props.setBaseUrl("https://api.github.com");
            props.setToken(token);
            props.setOrganization(org);
            props.setTimeout(30000);
            props.setRetryAttempts(3);

            // Create client
            GitHubCopilotApiClient client = new GitHubCopilotApiClient(
                WebClient.builder(),
                props,
                null, // No Google Workspace for this test
                new ObjectMapper()
            );

            // Fetch users
            System.out.println("\n📡 Fetching users from GitHub Copilot API...\n");
            List<UserData> users = client.fetchUsers();

            // Display results
            System.out.println("✅ Total users fetched: " + users.size());
            System.out.println("\nUser List:");
            System.out.println("-".repeat(60));

            int unresolvedCount = 0;
            for (int i = 0; i < users.size(); i++) {
                UserData user = users.get(i);
                System.out.printf("%d. Email: %s\n", i + 1, user.email());
                System.out.printf("   Name: %s\n", user.name());
                System.out.printf("   Status: %s\n", user.status());

                if (user.additionalMetrics() != null) {
                    Object emailType = user.additionalMetrics().get("email_type");
                    Object githubLogin = user.additionalMetrics().get("github_login");
                    System.out.printf("   GitHub Login: %s\n", githubLogin);
                    System.out.printf("   Email Type: %s\n", emailType);

                    if ("not_found".equals(emailType)) {
                        unresolvedCount++;
                    }
                }
                System.out.println("-".repeat(60));
            }

            // Summary
            System.out.println("\n📊 Summary:");
            System.out.println("Total users: " + users.size());
            System.out.println("Unresolved users ([SEM-USR-GITHUB]): " + unresolvedCount);
            System.out.println("Resolved users: " + (users.size() - unresolvedCount));

            if (users.isEmpty()) {
                System.out.println("\n⚠️  WARNING: No users found!");
                System.out.println("Possible reasons:");
                System.out.println("1. No GitHub Copilot seats assigned in organization");
                System.out.println("2. Invalid token or insufficient permissions");
                System.out.println("3. Organization name is incorrect");
            }

        } catch (Exception e) {
            System.err.println("\n❌ Error occurred:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
