package com.bemobi.aicontrol;

import com.bemobi.aicontrol.integration.github.GitHubCopilotApiClient;
import com.bemobi.aicontrol.integration.github.GitHubApiProperties;
import com.bemobi.aicontrol.integration.google.GoogleWorkspaceClient;
import com.bemobi.aicontrol.integration.google.GoogleWorkspaceProperties;
import com.bemobi.aicontrol.integration.common.UserData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.model.User;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Diagnostic tool to understand git_name matching issues
 */
public class DiagnoseGitNameMatching {
    public static void main(String[] args) {
        try {
            System.out.println("=".repeat(80));
            System.out.println("Git Name Matching Diagnostic Tool");
            System.out.println("=".repeat(80));
            System.out.println();

            // Step 1: Fetch GitHub users
            System.out.println("[1/4] Fetching users from GitHub Copilot...");
            GitHubApiProperties githubProps = new GitHubApiProperties();
            githubProps.setEnabled(true);
            githubProps.setBaseUrl("https://api.github.com");
            githubProps.setToken(System.getenv("AI_CONTROL_GITHUB_TOKEN"));
            githubProps.setOrganization(System.getenv("AI_CONTROL_GITHUB_ORG"));
            githubProps.setTimeout(30000);
            githubProps.setRetryAttempts(3);

            GitHubCopilotApiClient githubClient = new GitHubCopilotApiClient(
                WebClient.builder(),
                githubProps,
                null, // Don't use Workspace yet
                new ObjectMapper()
            );

            List<UserData> githubUsers = githubClient.fetchUsers();
            System.out.println("   ✅ Found " + githubUsers.size() + " GitHub Copilot users");

            // Extract GitHub logins
            List<String> githubLogins = githubUsers.stream()
                .map(u -> (String) u.additionalMetrics().get("github_login"))
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

            System.out.println("   GitHub logins: " + githubLogins.size());
            System.out.println();

            // Step 2: Initialize Google Workspace
            System.out.println("[2/4] Initializing Google Workspace client...");
            GoogleWorkspaceProperties workspaceProps = new GoogleWorkspaceProperties();
            workspaceProps.setEnabled(true);
            workspaceProps.setCredentials(System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS"));
            workspaceProps.setDomain(System.getenv("AI_CONTROL_WORKSPACE_DOMAIN"));
            workspaceProps.setAdminEmail(System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL"));
            workspaceProps.setCustomSchema(System.getenv("AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA"));
            workspaceProps.setGitNameField(System.getenv("AI_CONTROL_WORKSPACE_GIT_FIELD"));
            workspaceProps.setTimeout(30000);

            GoogleWorkspaceClient workspaceClient = new GoogleWorkspaceClient(workspaceProps);
            System.out.println("   ✅ Google Workspace initialized");
            System.out.println("   Custom field: " + workspaceProps.getCustomSchema() + "." + workspaceProps.getGitNameField());
            System.out.println();

            // Step 3: Test each GitHub login against Google Workspace
            System.out.println("[3/4] Testing each GitHub login against Google Workspace...");

            Map<String, String> workspaceGitNames = new HashMap<>();
            Set<String> testedLogins = new HashSet<>();

            for (String githubLogin : githubLogins) {
                Optional<String> email = workspaceClient.findEmailByGitName(githubLogin);
                if (email.isPresent()) {
                    workspaceGitNames.put(githubLogin.toLowerCase(), email.get());
                }
                testedLogins.add(githubLogin);
            }

            System.out.println("   ✅ Tested " + testedLogins.size() + " GitHub logins");
            System.out.println("   ✅ Found " + workspaceGitNames.size() + " matches in Workspace");
            System.out.println();

            // Step 4: Compare and analyze
            System.out.println("[4/4] Analyzing matches...");
            System.out.println("=".repeat(80));
            System.out.println();

            int matched = 0;
            int notMatched = 0;

            List<String> notFoundLogins = new ArrayList<>();
            List<String> matchedLogins = new ArrayList<>();

            for (String githubLogin : githubLogins) {
                String normalizedLogin = githubLogin.toLowerCase();
                String workspaceEmail = workspaceGitNames.get(normalizedLogin);

                if (workspaceEmail != null) {
                    matched++;
                    matchedLogins.add(githubLogin);
                    System.out.println("✅ " + String.format("%-30s", githubLogin) + " -> " + workspaceEmail);
                } else {
                    notMatched++;
                    notFoundLogins.add(githubLogin);
                    System.out.println("❌ " + String.format("%-30s", githubLogin) + " -> NOT FOUND");
                }
            }

            // Summary
            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("📊 SUMMARY");
            System.out.println("=".repeat(80));
            System.out.println();
            System.out.println("GitHub Copilot users: " + githubLogins.size());
            System.out.println("Google Workspace users with git_name: " + workspaceGitNames.size());
            System.out.println();
            System.out.println("✅ Matched: " + matched);
            System.out.println("❌ Not matched: " + notMatched);
            System.out.println();

            if (!notFoundLogins.isEmpty()) {
                System.out.println("Users NOT FOUND in Google Workspace:");
                System.out.println("-".repeat(80));
                for (String login : notFoundLogins) {
                    System.out.println("  • " + login);
                }
                System.out.println();
                System.out.println("💡 Action needed:");
                System.out.println("   1. Check if these users exist in Google Workspace");
                System.out.println("   2. Add the 'git_name' custom field with their GitHub login");
                System.out.println("   3. Or check if the git_name value is different (typo, case, etc)");
            }

            // Show what git_names exist in Workspace but not in GitHub
            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("📋 ALL GIT_NAMES IN GOOGLE WORKSPACE");
            System.out.println("=".repeat(80));

            List<Map.Entry<String, String>> sortedWorkspace = new ArrayList<>(workspaceGitNames.entrySet());
            sortedWorkspace.sort(Map.Entry.comparingByKey());

            for (Map.Entry<String, String> entry : sortedWorkspace) {
                boolean inGitHub = githubLogins.stream()
                    .anyMatch(login -> login.equalsIgnoreCase(entry.getKey()));
                String marker = inGitHub ? "✅" : "⚠️";
                System.out.println(marker + " " + String.format("%-30s", entry.getKey()) + " (" + entry.getValue() + ")");
            }

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
