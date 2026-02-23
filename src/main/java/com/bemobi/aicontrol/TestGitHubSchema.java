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
 * Test GitHub schema query
 */
public class TestGitHubSchema {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Testing GitHub Schema Query");
        System.out.println("=".repeat(80));
        System.out.println();

        String credentialsPath = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
        String domain = System.getenv("AI_CONTROL_WORKSPACE_DOMAIN");
        String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");
        String schema = System.getenv("AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA");
        String field = System.getenv("AI_CONTROL_WORKSPACE_GIT_FIELD");

        String testValue = "magacho"; // Try with the user's own value

        try {
            // Build Directory service
            System.out.println("Connecting...");
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

            System.out.println("✅ Connected");
            System.out.println();
            System.out.println("Schema: " + schema);
            System.out.println("Field: " + field);
            System.out.println("Test value: " + testValue);
            System.out.println();

            // Test with GitHub schema
            String[] queries = {
                schema + "." + field + "='" + testValue + "'",
                schema + "." + field + ":" + testValue,
            };

            for (String query : queries) {
                System.out.println("Testing query: " + query);
                try {
                    Users result = directory.users().list()
                            .setDomain(domain)
                            .setQuery(query)
                            .setProjection("custom")
                            .setCustomFieldMask(schema)
                            .setMaxResults(5)
                            .execute();

                    if (result.getUsers() != null && !result.getUsers().isEmpty()) {
                        System.out.println("   ✅ SUCCESS! Found " + result.getUsers().size() + " user(s):");
                        result.getUsers().forEach(u -> {
                            System.out.println("      • " + u.getPrimaryEmail());
                            if (u.getCustomSchemas() != null) {
                                System.out.println("        Custom: " + u.getCustomSchemas());
                            }
                        });
                    } else {
                        System.out.println("   ⚠️  Query worked but no results");
                    }
                } catch (Exception e) {
                    System.out.println("   ❌ Failed: " + e.getMessage());
                }
                System.out.println();
            }

            System.out.println("=".repeat(80));
            System.out.println("💡 NEXT STEPS:");
            System.out.println("=".repeat(80));
            System.out.println();
            System.out.println("If all queries failed, it means:");
            System.out.println("1. The schema '" + schema + "' doesn't exist, OR");
            System.out.println("2. The field '" + field + "' doesn't exist in that schema, OR");
            System.out.println("3. No user has this field filled with value '" + testValue + "'");
            System.out.println();
            System.out.println("To fix:");
            System.out.println("1. Go to: https://admin.google.com/ac/customschema");
            System.out.println("2. Verify the schema '" + schema + "' exists");
            System.out.println("3. Verify it has a field named '" + field + "'");
            System.out.println("4. Edit a user and fill in the field with their GitHub username");

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
