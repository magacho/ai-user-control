package com.bemobi.aicontrol;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.Users;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;

/**
 * List users and show their custom fields
 */
public class ListAnyUserWithCustomFields {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Google Workspace - List Users with Custom Fields");
        System.out.println("=".repeat(80));
        System.out.println();

        String credentialsPath = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
        String domain = System.getenv("AI_CONTROL_WORKSPACE_DOMAIN");
        String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");

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

            // List users with custom fields
            System.out.println("[2/2] Fetching users from domain: " + domain);
            Users result = directory.users().list()
                    .setDomain(domain)
                    .setMaxResults(10)
                    .setProjection("custom")
                    .execute();

            if (result.getUsers() == null || result.getUsers().isEmpty()) {
                System.out.println("   ⚠️  No users found in domain!");
                System.exit(1);
            }

            System.out.println("   ✅ Found " + result.getUsers().size() + " user(s)");
            System.out.println();
            System.out.println("=".repeat(80));

            boolean foundCustomFields = false;

            for (User user : result.getUsers()) {
                System.out.println();
                System.out.println("👤 User: " + user.getPrimaryEmail());
                System.out.println("   Name: " + (user.getName() != null ? user.getName().getFullName() : "N/A"));

                if (user.getCustomSchemas() != null && !user.getCustomSchemas().isEmpty()) {
                    foundCustomFields = true;
                    System.out.println("   ✅ Custom Fields:");

                    for (Map.Entry<String, Map<String, Object>> schemaEntry : user.getCustomSchemas().entrySet()) {
                        String schemaName = schemaEntry.getKey();
                        Map<String, Object> fields = schemaEntry.getValue();

                        System.out.println();
                        System.out.println("      📦 Schema: '" + schemaName + "'");

                        for (Map.Entry<String, Object> field : fields.entrySet()) {
                            String fieldName = field.getKey();
                            Object fieldValue = field.getValue();
                            System.out.println("         • " + fieldName + " = " + fieldValue);
                        }
                    }
                } else {
                    System.out.println("   ⚠️  No custom fields");
                }
                System.out.println("   " + "-".repeat(76));
            }

            System.out.println();
            System.out.println("=".repeat(80));

            if (foundCustomFields) {
                System.out.println("✅ FOUND CUSTOM FIELDS!");
                System.out.println();
                System.out.println("💡 Update your .env based on the schema and field names above:");
                System.out.println("   AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA=<schema_name>");
                System.out.println("   AI_CONTROL_WORKSPACE_GIT_FIELD=<field_name_for_github_username>");
            } else {
                System.out.println("⚠️  NO CUSTOM FIELDS FOUND!");
                System.out.println();
                System.out.println("💡 You need to:");
                System.out.println("   1. Go to: https://admin.google.com/ac/customschema");
                System.out.println("   2. Create a custom schema (e.g., 'Developer')");
                System.out.println("   3. Add a text field (e.g., 'git_name')");
                System.out.println("   4. Edit users and add their GitHub usernames");
            }

            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
