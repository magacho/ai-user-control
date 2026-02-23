package com.bemobi.aicontrol;

import com.bemobi.aicontrol.integration.google.GoogleWorkspaceClient;
import com.bemobi.aicontrol.integration.google.GoogleWorkspaceProperties;

import java.util.Optional;

/**
 * Quick test for Google Workspace integration
 */
public class TestGoogleWorkspace {
    public static void main(String[] args) {
        try {
            String credentials = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
            String domain = System.getenv("AI_CONTROL_WORKSPACE_DOMAIN");
            String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");
            String customSchema = System.getenv("AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA");
            String gitField = System.getenv("AI_CONTROL_WORKSPACE_GIT_FIELD");

            System.out.println("=".repeat(60));
            System.out.println("Testing Google Workspace Integration");
            System.out.println("=".repeat(60));
            System.out.println("Credentials file: " + credentials);
            System.out.println("Domain: " + domain);
            System.out.println("Admin email: " + adminEmail);
            System.out.println("Custom schema: " + customSchema);
            System.out.println("Git field: " + gitField);
            System.out.println("=".repeat(60));

            GoogleWorkspaceProperties props = new GoogleWorkspaceProperties();
            props.setEnabled(true);
            props.setCredentials(credentials);
            props.setDomain(domain);
            props.setAdminEmail(adminEmail);
            props.setCustomSchema(customSchema);
            props.setGitNameField(gitField);
            props.setTimeout(30000);

            GoogleWorkspaceClient client = new GoogleWorkspaceClient(props);

            // Test with some GitHub logins from the previous test
            String[] testLogins = {
                "natenho",
                "jpgsaraceni",
                "faleirothiago",
                "eirisma-bemobi",
                "thaisfaustino-bemobi"
            };

            System.out.println("\n📡 Testing email resolution for GitHub logins:\n");

            int resolved = 0;
            for (String login : testLogins) {
                Optional<String> email = client.findEmailByGitName(login);
                if (email.isPresent()) {
                    System.out.println("✅ " + login + " -> " + email.get());
                    resolved++;
                } else {
                    System.out.println("❌ " + login + " -> NOT FOUND");
                }
            }

            System.out.println("\n📊 Summary:");
            System.out.println("Total tested: " + testLogins.length);
            System.out.println("Resolved: " + resolved);
            System.out.println("Not found: " + (testLogins.length - resolved));

        } catch (Exception e) {
            System.err.println("\n❌ Error occurred:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
