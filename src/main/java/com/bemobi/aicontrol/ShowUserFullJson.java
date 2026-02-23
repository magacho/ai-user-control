package com.bemobi.aicontrol;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.User;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileInputStream;
import java.util.Collections;

/**
 * Show full JSON for a specific user
 */
public class ShowUserFullJson {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Google Workspace User - Full JSON");
        System.out.println("=".repeat(80));
        System.out.println();

        String credentialsPath = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
        String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");
        String targetUser = "flavio.magacho@bemobi.com";

        System.out.println("🔍 Fetching user: " + targetUser);
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

            System.out.println("   ✅ Connected");
            System.out.println();

            // Fetch user with ALL fields
            System.out.println("[2/2] Fetching user data...");
            User user = directory.users().get(targetUser)
                    .setProjection("custom")  // Get custom fields
                    .execute();

            System.out.println("   ✅ User found!");
            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("📄 FULL USER JSON:");
            System.out.println("=".repeat(80));
            System.out.println();

            // Convert to JSON and pretty print
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(user);

            System.out.println(json);

            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println();

            // Highlight custom schemas if they exist
            if (user.getCustomSchemas() != null && !user.getCustomSchemas().isEmpty()) {
                System.out.println("✅ CUSTOM SCHEMAS FOUND:");
                System.out.println();
                for (var entry : user.getCustomSchemas().entrySet()) {
                    System.out.println("Schema: " + entry.getKey());
                    System.out.println("Fields: " + entry.getValue());
                    System.out.println();
                }
            } else {
                System.out.println("⚠️  No custom schemas found for this user");
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
