package com.bemobi.aicontrol;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Diagnostic tool for Google Workspace configuration
 */
public class DiagnoseGoogleWorkspace {
    public static void main(String[] args) {
        String credentials = System.getenv("AI_CONTROL_WORKSPACE_CREDENTIALS");
        String adminEmail = System.getenv("AI_CONTROL_WORKSPACE_ADMIN_EMAIL");
        String domain = System.getenv("AI_CONTROL_WORKSPACE_DOMAIN");

        System.out.println("=".repeat(60));
        System.out.println("Google Workspace Configuration Diagnosis");
        System.out.println("=".repeat(60));

        // Step 1: Load credentials
        System.out.println("\n[1/5] Loading credentials from: " + credentials);
        try (InputStream is = new FileInputStream(Path.of(credentials).toFile())) {
            GoogleCredentials creds = GoogleCredentials.fromStream(is);
            System.out.println("✅ Credentials loaded successfully");

            if (creds instanceof ServiceAccountCredentials serviceAccount) {
                System.out.println("✅ Type: Service Account");
                System.out.println("   Service Account Email: " + serviceAccount.getClientEmail());
                System.out.println("   Project ID: " + serviceAccount.getProjectId());
                System.out.println("   Client ID: " + serviceAccount.getClientId());
            } else {
                System.out.println("❌ ERROR: Not a Service Account!");
                System.out.println("   Type: " + creds.getClass().getSimpleName());
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("❌ Failed to load credentials: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Step 2: Load and scope credentials
        System.out.println("\n[2/5] Applying scopes");
        try (InputStream is = new FileInputStream(Path.of(credentials).toFile())) {
            GoogleCredentials creds = GoogleCredentials.fromStream(is);
            ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) creds;

            GoogleCredentials scoped = serviceAccount
                .createScoped(Collections.singleton(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY));
            System.out.println("✅ Scopes applied: " + DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY);
        } catch (Exception e) {
            System.out.println("❌ Failed to apply scopes: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Step 3: Create delegated credentials
        System.out.println("\n[3/5] Creating delegated credentials");
        System.out.println("   Admin Email: " + adminEmail);
        System.out.println("   Domain: " + domain);
        try (InputStream is = new FileInputStream(Path.of(credentials).toFile())) {
            GoogleCredentials creds = GoogleCredentials.fromStream(is);
            ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) creds;

            GoogleCredentials scoped = serviceAccount
                .createScoped(Collections.singleton(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY));

            GoogleCredentials delegated = scoped.createDelegated(adminEmail);
            System.out.println("✅ Delegated credentials created");
        } catch (Exception e) {
            System.out.println("❌ Failed to create delegated credentials: " + e.getMessage());
            System.out.println("\nPossible causes:");
            System.out.println("1. Service Account does not have Domain-Wide Delegation enabled");
            System.out.println("2. Admin email is incorrect or not an admin user");
            System.out.println("3. Service Account Client ID not added to Domain-Wide Delegation in Google Admin");
            e.printStackTrace();
            System.exit(1);
        }

        // Step 4: Build Directory service
        System.out.println("\n[4/5] Building Directory service");
        Directory directory = null;
        try (InputStream is = new FileInputStream(Path.of(credentials).toFile())) {
            GoogleCredentials creds = GoogleCredentials.fromStream(is);
            ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) creds;

            GoogleCredentials delegated = serviceAccount
                .createScoped(Collections.singleton(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY))
                .createDelegated(adminEmail);

            directory = new Directory.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(delegated))
                    .setApplicationName("ai-user-control")
                    .build();
            System.out.println("✅ Directory service built");
        } catch (Exception e) {
            System.out.println("❌ Failed to build Directory service: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Step 5: Test API call
        System.out.println("\n[5/5] Testing API call (list users)");
        try {
            var result = directory.users().list()
                .setDomain(domain)
                .setMaxResults(1)
                .execute();

            System.out.println("✅ API call successful!");
            if (result.getUsers() != null && !result.getUsers().isEmpty()) {
                System.out.println("   Found users in domain");
                System.out.println("   Sample user: " + result.getUsers().get(0).getPrimaryEmail());
            } else {
                System.out.println("   No users found (empty domain?)");
            }
        } catch (Exception e) {
            System.out.println("❌ API call failed: " + e.getMessage());
            System.out.println("\nError details:");
            System.out.println("This error typically means:");
            System.out.println("1. Domain-Wide Delegation is not properly configured");
            System.out.println("2. The Service Account Client ID is not authorized in Google Admin Console");
            System.out.println("3. The scope " + DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY + " is not authorized");
            System.out.println("\nTo fix:");
            System.out.println("1. Go to: https://admin.google.com");
            System.out.println("2. Navigate to: Security → API Controls → Domain-wide Delegation");
            System.out.println("3. Click 'Add new'");
            System.out.println("4. Add the Client ID from the service account");
            System.out.println("5. Add scope: " + DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY);
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ ALL CHECKS PASSED!");
        System.out.println("Google Workspace is properly configured");
        System.out.println("=".repeat(60));
    }
}
