package com.bemobi.aicontrol;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.User;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Inspect custom fields for a specific user
 */
public class InspectUserCustomFields {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Google Workspace User Custom Fields Inspector");
        System.out.println("=".repeat(80));
        System.out.println();

        String credentialsPath = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
        String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");

        // Use the admin email as the test user (you can change this)
        String testUserEmail = adminEmail;

        System.out.println("📧 Will inspect user: " + testUserEmail);
        System.out.println();

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
            System.out.println();

            // Fetch the user with custom fields
            System.out.println("[2/2] Fetching user with custom fields...");
            User user = directory.users().get(testUserEmail)
                    .setProjection("custom")
                    .execute();

            System.out.println("   ✅ User found: " + user.getPrimaryEmail());
            System.out.println("   Name: " + user.getName().getFullName());
            System.out.println();

            // Check custom schemas
            if (user.getCustomSchemas() != null && !user.getCustomSchemas().isEmpty()) {
                System.out.println("✅ CUSTOM FIELDS FOUND:");
                System.out.println("=".repeat(80));

                for (Map.Entry<String, Map<String, Object>> schemaEntry : user.getCustomSchemas().entrySet()) {
                    String schemaName = schemaEntry.getKey();
                    Map<String, Object> fields = schemaEntry.getValue();

                    System.out.println();
                    System.out.println("📦 Schema Name: '" + schemaName + "'");
                    System.out.println("   Fields:");

                    for (Map.Entry<String, Object> field : fields.entrySet()) {
                        String fieldName = field.getKey();
                        Object fieldValue = field.getValue();
                        System.out.println("      • " + fieldName + " = " + fieldValue);
                        System.out.println("        Use in .env: " + schemaName + "." + fieldName);
                    }
                }

                System.out.println();
                System.out.println("=".repeat(80));
                System.out.println("💡 Update your .env file:");
                System.out.println("   AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA=<schema_name_from_above>");
                System.out.println("   AI_CONTROL_WORKSPACE_GIT_FIELD=<field_name_from_above>");

            } else {
                System.out.println("⚠️  NO CUSTOM FIELDS FOUND for this user!");
                System.out.println();
                System.out.println("💡 You need to:");
                System.out.println("   1. Create a custom schema in Google Workspace Admin");
                System.out.println("   2. Go to: https://admin.google.com/ac/customschema");
                System.out.println("   3. Create a schema (e.g., 'Developer' or 'custom')");
                System.out.println("   4. Add a text field (e.g., 'git_name')");
                System.out.println("   5. Edit a user and fill in their GitHub username in that field");
                System.out.println();
                System.out.println("   Then test again with a user that has the field filled in.");
            }

            System.out.println();
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
