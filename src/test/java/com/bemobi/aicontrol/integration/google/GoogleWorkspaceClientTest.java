package com.bemobi.aicontrol.integration.google;

import com.google.api.services.directory.Directory;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleWorkspaceClientTest {

    @Mock
    private Directory directory;

    @Mock
    private Directory.Users usersResource;

    @Mock
    private Directory.Users.List listRequest;

    private GoogleWorkspaceProperties properties;
    private GoogleWorkspaceClient client;

    @BeforeEach
    void setUp() throws Exception {
        properties = new GoogleWorkspaceProperties();
        properties.setEnabled(true);
        properties.setDomain("example.com");
        properties.setAdminEmail("admin@example.com");
        properties.setCustomSchema("custom");
        properties.setGitNameField("git_name");

        // Create client bypassing real Google auth by injecting mock Directory
        client = createClientWithMockDirectory();
    }

    @Test
    void testFindEmailByGitName_matchFound() throws IOException {
        User user = new User();
        user.setPrimaryEmail("john@example.com");

        Users usersResult = new Users();
        usersResult.setUsers(List.of(user));

        setupListMock(usersResult);

        Optional<String> result = client.findEmailByGitName("johndoe");

        assertTrue(result.isPresent());
        assertEquals("john@example.com", result.get());
    }

    @Test
    void testFindEmailByGitName_noMatch() throws IOException {
        Users usersResult = new Users();
        usersResult.setUsers(null);

        setupListMock(usersResult);

        Optional<String> result = client.findEmailByGitName("unknown_user");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindEmailByGitName_emptyUserList() throws IOException {
        Users usersResult = new Users();
        usersResult.setUsers(Collections.emptyList());

        setupListMock(usersResult);

        Optional<String> result = client.findEmailByGitName("unknown_user");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindEmailByGitName_apiError() throws IOException {
        when(directory.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(listRequest);
        when(listRequest.setDomain(anyString())).thenReturn(listRequest);
        when(listRequest.setQuery(anyString())).thenReturn(listRequest);
        when(listRequest.setProjection(anyString())).thenReturn(listRequest);
        when(listRequest.setCustomFieldMask(anyString())).thenReturn(listRequest);
        when(listRequest.setMaxResults(anyInt())).thenReturn(listRequest);
        when(listRequest.execute()).thenThrow(new IOException("API error"));

        Optional<String> result = client.findEmailByGitName("error_user");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindEmailByGitName_cacheHit() throws IOException {
        User user = new User();
        user.setPrimaryEmail("cached@example.com");

        Users usersResult = new Users();
        usersResult.setUsers(List.of(user));

        setupListMock(usersResult);

        // First call - hits API
        Optional<String> first = client.findEmailByGitName("cacheduser");
        assertEquals("cached@example.com", first.get());

        // Second call - should use cache (no additional API call)
        Optional<String> second = client.findEmailByGitName("cacheduser");
        assertEquals("cached@example.com", second.get());

        // Verify the API was only called once
        verify(listRequest, times(1)).execute();
    }

    @Test
    void testClearCache() throws IOException {
        User user = new User();
        user.setPrimaryEmail("cached@example.com");

        Users usersResult = new Users();
        usersResult.setUsers(List.of(user));

        setupListMock(usersResult);

        // First call
        client.findEmailByGitName("testuser");

        // Clear cache
        client.clearCache();

        // Second call should hit API again
        client.findEmailByGitName("testuser");

        verify(listRequest, times(2)).execute();
    }

    private GoogleWorkspaceClient createClientWithMockDirectory() throws Exception {
        // Use a test subclass approach to avoid real Google auth
        GoogleWorkspaceClient testClient = new GoogleWorkspaceClient(properties) {
            @Override
            Directory buildDirectoryService(GoogleWorkspaceProperties props) {
                return directory;
            }
        };
        return testClient;
    }

    private void setupListMock(Users usersResult) throws IOException {
        when(directory.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(listRequest);
        when(listRequest.setDomain(anyString())).thenReturn(listRequest);
        when(listRequest.setQuery(anyString())).thenReturn(listRequest);
        when(listRequest.setProjection(anyString())).thenReturn(listRequest);
        when(listRequest.setCustomFieldMask(anyString())).thenReturn(listRequest);
        when(listRequest.setMaxResults(anyInt())).thenReturn(listRequest);
        when(listRequest.execute()).thenReturn(usersResult);
    }
}
