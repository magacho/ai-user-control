package com.bemobi.aicontrol;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.Schema;
import com.google.api.services.directory.model.Schemas;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.Users;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * List all custom schemas available in Google Workspace
 */
public class ListGoogleWorkspaceSchemas {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Google Workspace Custom Schemas Inspector");
        System.out.println("=".repeat(80));
        System.out.println();

        String credentialsPath = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
        String domain = System.getenv("AI_CONTROL_WORKSPACE_DOMAIN");
        String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");
        String customerId = "my_customer"; // Special value for Directory API

        try {
            // Build Directory service
            System.out.println("[1/3] Connecting to Google Workspace...");
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

            // List all custom schemas
            System.out.println("[2/3] Fetching custom schemas...");
            try {
                Schemas schemas = directory.schemas().list(customerId).execute();
                List<Schema> schemaList = schemas.getSchemas();

                if (schemaList == null || schemaList.isEmpty()) {
                    System.out.println("   ⚠️  No custom schemas found!");
                    System.out.println();
                    System.out.println("💡 You need to create a custom schema in Google Workspace:");
                    System.out.println("   1. Go to: https://admin.google.com/ac/customschema");
                    System.out.println("   2. Create a new schema (e.g., 'custom' or 'developer')");
                    System.out.println("   3. Add a text field (e.g., 'git_name')");
                    System.out.println("   4. Update the .env file with the correct schema and field names");
                } else {
                    System.out.println("   ✅ Found " + schemaList.size() + " custom schema(s)");
                    System.out.println();

                    for (Schema schema : schemaList) {
                        System.out.println("📋 Schema: " + schema.getSchemaName());
                        System.out.println("   Display Name: " + schema.getDisplayName());
                        System.out.println("   Schema ID: " + schema.getSchemaId());

                        if (schema.getFields() != null && !schema.getFields().isEmpty()) {
                            System.out.println("   Fields:");
                            for (Object fieldObj : schema.getFields()) {
                                if (fieldObj instanceof Map) {
                                    Map<?, ?> field = (Map<?, ?>) fieldObj;
                                    String fieldName = (String) field.get("fieldName");
                                    String fieldType = (String) field.get("fieldType");
                                    System.out.println("      • " + fieldName + " (" + fieldType + ")");
                                }
                            }
                        } else {
                            System.out.println("   Fields: (none)");
                        }
                        System.out.println();
                    }
                }
            } catch (Exception e) {
                System.err.println("   ❌ Failed to fetch schemas: " + e.getMessage());
                System.err.println();
                System.err.println("💡 This might mean:");
                System.err.println("   • You need the 'admin.directory.userschema.readonly' scope");
                System.err.println("   • Or custom schemas don't exist yet");
            }
            System.out.println();

            // Try to list one user with custom fields to see what's available
            System.out.println("[3/3] Fetching a sample user with custom fields...");
            try {
                Users result = directory.users().list()
                        .setDomain(domain)
                        .setMaxResults(1)
                        .setProjection("full")
                        .execute();

                if (result.getUsers() != null && !result.getUsers().isEmpty()) {
                    User user = result.getUsers().get(0);
                    System.out.println("   Sample user: " + user.getPrimaryEmail());

                    if (user.getCustomSchemas() != null && !user.getCustomSchemas().isEmpty()) {
                        System.out.println("   Custom fields found:");
                        for (Map.Entry<String, Map<String, Object>> entry : user.getCustomSchemas().entrySet()) {
                            System.out.println();
                            System.out.println("   📦 Schema: " + entry.getKey());
                            Map<String, Object> fields = entry.getValue();
                            for (Map.Entry<String, Object> field : fields.entrySet()) {
                                System.out.println("      • " + field.getKey() + " = " + field.getValue());
                            }
                        }
                    } else {
                        System.out.println("   ⚠️  No custom fields set for this user");
                    }
                } else {
                    System.out.println("   ⚠️  No users found in domain");
                }
            } catch (Exception e) {
                System.err.println("   ❌ Failed to fetch user: " + e.getMessage());
            }

            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("✅ Inspection complete!");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
