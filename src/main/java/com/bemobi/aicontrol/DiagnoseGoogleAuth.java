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
 * Diagnose Google Workspace authentication issues
 */
public class DiagnoseGoogleAuth {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Google Workspace Authentication Diagnostic");
        System.out.println("=".repeat(80));
        System.out.println();

        String credentialsPath = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
        String domain = System.getenv("AI_CONTROL_WORKSPACE_DOMAIN");
        String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");
        String customSchema = System.getenv("AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA");
        String gitField = System.getenv("AI_CONTROL_WORKSPACE_GIT_FIELD");

        System.out.println("📋 Configuration:");
        System.out.println("   Credentials: " + credentialsPath);
        System.out.println("   Domain: " + domain);
        System.out.println("   Admin Email: " + adminEmail);
        System.out.println("   Custom Schema: " + customSchema);
        System.out.println("   Git Field: " + gitField);
        System.out.println();

        try {
            // Step 1: Load credentials
            System.out.println("[1/4] Loading service account credentials...");
            GoogleCredentials baseCredentials;
            try (FileInputStream fis = new FileInputStream(credentialsPath)) {
                baseCredentials = GoogleCredentials.fromStream(fis);
            }

            if (!(baseCredentials instanceof ServiceAccountCredentials)) {
                System.err.println("   ❌ Not a service account!");
                System.exit(1);
            }

            ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) baseCredentials;

            System.out.println("   ✅ Service account loaded");
            System.out.println("   Service Account Email: " + serviceAccount.getClientEmail());
            System.out.println("   Project ID: " + serviceAccount.getProjectId());
            System.out.println();

            // Step 2: Add scopes
            System.out.println("[2/4] Adding Directory API scopes...");
            GoogleCredentials scopedCredentials = serviceAccount
                    .createScoped(Collections.singleton(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY));
            System.out.println("   ✅ Scopes added: ADMIN_DIRECTORY_USER_READONLY");
            System.out.println();

            // Step 3: Create delegated credentials
            System.out.println("[3/4] Creating delegated credentials for: " + adminEmail);
            GoogleCredentials delegatedCredentials;
            try {
                delegatedCredentials = scopedCredentials.createDelegated(adminEmail);
                System.out.println("   ✅ Delegation created successfully");
            } catch (Exception e) {
                System.err.println("   ❌ Failed to create delegation:");
                System.err.println("   Error: " + e.getMessage());
                System.err.println();
                System.err.println("💡 Possible causes:");
                System.err.println("   1. Domain-wide delegation not enabled for this service account");
                System.err.println("   2. The service account client ID not authorized in Workspace Admin");
                System.err.println("   3. The admin email '" + adminEmail + "' doesn't exist or lacks permissions");
                System.err.println();
                System.err.println("🔧 To fix:");
                System.err.println("   1. Go to: https://admin.google.com/ac/owl/domainwidedelegation");
                System.err.println("   2. Add the Client ID: " + serviceAccount.getClientId());
                System.err.println("   3. Add scope: https://www.googleapis.com/auth/admin.directory.user.readonly");
                throw e;
            }
            System.out.println();

            // Step 4: Test Directory API access
            System.out.println("[4/4] Testing Directory API access...");
            Directory directory = new Directory.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(delegatedCredentials))
                    .setApplicationName("ai-user-control-diagnostic")
                    .build();

            // Try to list users (limited to 1)
            System.out.println("   Fetching 1 user from domain: " + domain);
            Users result = directory.users().list()
                    .setDomain(domain)
                    .setMaxResults(1)
                    .execute();

            if (result.getUsers() != null && !result.getUsers().isEmpty()) {
                System.out.println("   ✅ Successfully retrieved user: " + result.getUsers().get(0).getPrimaryEmail());
            } else {
                System.out.println("   ⚠️  No users returned (domain might be empty)");
            }
            System.out.println();

            // Step 5: Test custom field query
            System.out.println("[5/5] Testing custom field query...");
            String testGitName = "natenho";
            String query = customSchema + "." + gitField + "='" + testGitName + "'";
            System.out.println("   Query: " + query);

            try {
                Users customResult = directory.users().list()
                        .setDomain(domain)
                        .setQuery(query)
                        .setProjection("custom")
                        .setCustomFieldMask(customSchema)
                        .setMaxResults(1)
                        .execute();

                if (customResult.getUsers() != null && !customResult.getUsers().isEmpty()) {
                    System.out.println("   ✅ Found user: " + customResult.getUsers().get(0).getPrimaryEmail());
                } else {
                    System.out.println("   ⚠️  No users found with git_name = '" + testGitName + "'");
                }
            } catch (Exception e) {
                System.err.println("   ❌ Custom field query failed: " + e.getMessage());
                System.err.println();
                System.err.println("💡 This might mean:");
                System.err.println("   1. The custom schema '" + customSchema + "' doesn't exist");
                System.err.println("   2. The field '" + gitField + "' doesn't exist in that schema");
                System.err.println("   3. Permissions issue accessing custom fields");
            }
            System.out.println();

            System.out.println("=".repeat(80));
            System.out.println("✅ ALL CHECKS PASSED - Integration should work!");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println();
            System.err.println("=".repeat(80));
            System.err.println("❌ DIAGNOSTIC FAILED");
            System.err.println("=".repeat(80));
            System.err.println();
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
