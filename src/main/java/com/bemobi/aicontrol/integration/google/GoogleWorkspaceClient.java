package com.bemobi.aicontrol.integration.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.Users;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Client for resolving corporate email addresses from GitHub logins
 * using Google Workspace Admin Directory API.
 *
 * Uses a custom schema field (e.g., custom.git_name) to match GitHub logins
 * to Workspace user profiles.
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.google-workspace", name = "enabled", havingValue = "true")
public class GoogleWorkspaceClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleWorkspaceClient.class);
    private static final Pattern VALID_GIT_LOGIN = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$");

    private final Directory directory;
    private final GoogleWorkspaceProperties properties;
    private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();

    public GoogleWorkspaceClient(GoogleWorkspaceProperties properties) throws IOException, GeneralSecurityException {
        this.properties = properties;
        this.directory = buildDirectoryService(properties);
        log.info("Google Workspace client initialized for domain: {}", properties.getDomain());
    }

    /**
     * Finds the corporate email address for a given GitHub login.
     *
     * @param gitLogin the GitHub username to look up
     * @return the corporate email if found, empty otherwise
     */
    public Optional<String> findEmailByGitName(String gitLogin) {
        if (gitLogin == null || gitLogin.isBlank()) {
            return Optional.empty();
        }
        return cache.computeIfAbsent(gitLogin, this::lookupEmail);
    }

    private Optional<String> lookupEmail(String gitLogin) {
        if (gitLogin == null || gitLogin.isBlank() || !VALID_GIT_LOGIN.matcher(gitLogin).matches()) {
            log.debug("Workspace: skipping invalid git login '{}'", gitLogin);
            return Optional.empty();
        }

        try {
            String query = properties.getCustomSchema() + "." + properties.getGitNameField() + "='" + gitLogin + "'";

            Users result = directory.users().list()
                    .setDomain(properties.getDomain())
                    .setQuery(query)
                    .setProjection("custom")
                    .setCustomFieldMask(properties.getCustomSchema())
                    .setMaxResults(1)
                    .execute();

            List<User> users = result.getUsers();
            if (users != null && !users.isEmpty()) {
                String email = users.get(0).getPrimaryEmail();
                log.debug("Workspace resolved {} -> {}", gitLogin, email);
                return Optional.ofNullable(email);
            }

            log.debug("Workspace: no match for git_name '{}'", gitLogin);
            return Optional.empty();

        } catch (IOException e) {
            log.warn("Workspace lookup failed for '{}': {}", gitLogin, e.getMessage());
            return Optional.empty();
        }
    }

    Directory buildDirectoryService(GoogleWorkspaceProperties props) throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = loadCredentials(props.getCredentials());

        if (!(credentials instanceof ServiceAccountCredentials serviceCredentials)) {
            throw new IOException("Expected ServiceAccountCredentials but got: " + credentials.getClass().getSimpleName()
                    + ". Ensure the credentials file is for a service account.");
        }

        GoogleCredentials delegated = serviceCredentials
                .createScoped(Collections.singleton(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY))
                .createDelegated(props.getAdminEmail());

        return new Directory.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(delegated))
                .setApplicationName("ai-user-control")
                .build();
    }

    private GoogleCredentials loadCredentials(String credentials) throws IOException {
        if (credentials == null || credentials.isBlank()) {
            throw new IOException("Google Workspace credentials not configured");
        }

        Path path = Path.of(credentials);
        if (Files.exists(path)) {
            log.debug("Loading Workspace credentials from file: {}", path);
            try (InputStream is = new FileInputStream(path.toFile())) {
                return GoogleCredentials.fromStream(is);
            }
        }

        log.debug("Loading Workspace credentials from inline JSON");
        try (InputStream is = new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8))) {
            return GoogleCredentials.fromStream(is);
        }
    }

    /**
     * Clears the in-memory cache.
     */
    public void clearCache() {
        cache.clear();
    }
}
