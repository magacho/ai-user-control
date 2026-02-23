package com.bemobi.aicontrol;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.Users;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileInputStream;
import java.util.Collections;

/**
 * Test different query formats to find the correct one
 */
public class TestDifferentQueryFormats {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Testing Different Query Formats for Google Workspace");
        System.out.println("=".repeat(80));
        System.out.println();

        String credentialsPath = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
        String domain = System.getenv("AI_CONTROL_WORKSPACE_DOMAIN");
        String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");

        String testGitName = "natenho"; // Test value

        try {
            // Build Directory service
            System.out.println("[1/2] Connecting to Google Workspace...");
            GoogleCredentials baseCredentials;
            try (FileInputStream fis = new FileInputStream(credentialsPath)) {
                baseCredentials = GoogleCredentials.fromStream(fis);
            }

            ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) baseCredentials;
            GoogleCredentials delegatedCredentials = serviceAccount
                    .createScoped(Collections.singleton(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY))
                    .createDelegated(adminEmail);

            Directory directory = new Directory.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(delegatedCredentials))
                    .setApplicationName("ai-user-control")
                    .build();

            System.out.println("   ✅ Connected successfully");
            System.out.println("   Admin Email: " + adminEmail);
            System.out.println("   Domain: " + domain);
            System.out.println();

            // Test different query formats
            System.out.println("[2/2] Testing different query formats for: " + testGitName);
            System.out.println("=".repeat(80));

            String[] queryFormats = {
                "git_name='" + testGitName + "'",                    // Without schema
                "custom.git_name='" + testGitName + "'",             // With 'custom' schema
                "Developer.git_name='" + testGitName + "'",          // With 'Developer' schema
                "git_name:" + testGitName,                           // Colon syntax
                "custom.git_name:" + testGitName,                    // Colon with schema
            };

            for (int i = 0; i < queryFormats.length; i++) {
                String query = queryFormats[i];
                System.out.println();
                System.out.println("Test " + (i + 1) + "/" + queryFormats.length + ": " + query);

                try {
                    Users result = directory.users().list()
                            .setDomain(domain)
                            .setQuery(query)
                            .setMaxResults(1)
                            .execute();

                    if (result.getUsers() != null && !result.getUsers().isEmpty()) {
                        System.out.println("   ✅ SUCCESS! Found: " + result.getUsers().get(0).getPrimaryEmail());
                        System.out.println();
                        System.out.println("=".repeat(80));
                        System.out.println("🎉 WORKING QUERY FORMAT: " + query);
                        System.out.println("=".repeat(80));
                        System.out.println();
                        System.out.println("💡 Update your .env:");

                        if (query.contains(".")) {
                            String schema = query.substring(0, query.indexOf("."));
                            System.out.println("   AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA=" + schema);
                            System.out.println("   AI_CONTROL_WORKSPACE_GIT_FIELD=git_name");
                        } else {
                            System.out.println("   AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA=");
                            System.out.println("   AI_CONTROL_WORKSPACE_GIT_FIELD=git_name");
                        }

                        return; // Exit on first success
                    } else {
                        System.out.println("   ⚠️  Query worked but no results found");
                    }

                } catch (Exception e) {
                    System.out.println("   ❌ Failed: " + e.getMessage());
                    if (e.getMessage().contains("Invalid Input")) {
                        System.out.println("      (Invalid field format)");
                    }
                }
            }

            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("❌ None of the query formats worked!");
            System.out.println("=".repeat(80));
            System.out.println();
            System.out.println("💡 Possible reasons:");
            System.out.println("   1. The custom field doesn't exist in Google Workspace");
            System.out.println("   2. The field has a different name");
            System.out.println("   3. No user has the git_name field set to '" + testGitName + "'");
            System.out.println();
            System.out.println("🔧 Next steps:");
            System.out.println("   1. Go to: https://admin.google.com/ac/customschema");
            System.out.println("   2. Check if custom schemas exist and what they're named");
            System.out.println("   3. Create a schema and field if they don't exist");
            System.out.println("   4. Add git_name values to user profiles");

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
