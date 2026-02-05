package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.ToolApiClient;
import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCollectionServiceTest {

    @Mock
    private ToolApiClient claudeClient;

    @Mock
    private ToolApiClient githubClient;

    @Mock
    private ToolApiClient cursorClient;

    private UserCollectionService service;

    @BeforeEach
    void setUp() {
        List<ToolApiClient> clients = Arrays.asList(claudeClient, githubClient, cursorClient);
        service = new UserCollectionService(clients);

        // Setup default mock responses with lenient stubbing
        lenient().when(claudeClient.getToolName()).thenReturn("claude");
        lenient().when(claudeClient.getDisplayName()).thenReturn("Claude Code");

        lenient().when(githubClient.getToolName()).thenReturn("github-copilot");
        lenient().when(githubClient.getDisplayName()).thenReturn("GitHub Copilot");

        lenient().when(cursorClient.getToolName()).thenReturn("cursor");
        lenient().when(cursorClient.getDisplayName()).thenReturn("Cursor");
    }

    @Test
    void testCollectAllUsers_AllEnabled_Success() throws ApiClientException {
        // Arrange
        when(claudeClient.isEnabled()).thenReturn(true);
        when(githubClient.isEnabled()).thenReturn(true);
        when(cursorClient.isEnabled()).thenReturn(true);

        UserData claudeUser = createUserData("claude@example.com", "Claude User", "claude");
        UserData githubUser = createUserData("github@example.com", "GitHub User", "github-copilot");
        UserData cursorUser = createUserData("cursor@example.com", "Cursor User", "cursor");

        when(claudeClient.fetchUsers()).thenReturn(Arrays.asList(claudeUser));
        when(githubClient.fetchUsers()).thenReturn(Arrays.asList(githubUser));
        when(cursorClient.fetchUsers()).thenReturn(Arrays.asList(cursorUser));

        // Act
        Map<String, List<UserData>> results = service.collectAllUsers();

        // Assert
        assertThat(results).hasSize(3);
        assertThat(results.get("claude")).hasSize(1);
        assertThat(results.get("github-copilot")).hasSize(1);
        assertThat(results.get("cursor")).hasSize(1);

        verify(claudeClient).fetchUsers();
        verify(githubClient).fetchUsers();
        verify(cursorClient).fetchUsers();
    }

    @Test
    void testCollectAllUsers_OneDisabled() throws ApiClientException {
        // Arrange
        when(claudeClient.isEnabled()).thenReturn(true);
        when(githubClient.isEnabled()).thenReturn(false);
        when(cursorClient.isEnabled()).thenReturn(true);

        UserData claudeUser = createUserData("claude@example.com", "Claude User", "claude");
        UserData cursorUser = createUserData("cursor@example.com", "Cursor User", "cursor");

        when(claudeClient.fetchUsers()).thenReturn(Arrays.asList(claudeUser));
        when(cursorClient.fetchUsers()).thenReturn(Arrays.asList(cursorUser));

        // Act
        Map<String, List<UserData>> results = service.collectAllUsers();

        // Assert
        assertThat(results).hasSize(3);
        assertThat(results.get("claude")).hasSize(1);
        assertThat(results.get("github-copilot")).isEmpty();
        assertThat(results.get("cursor")).hasSize(1);

        verify(claudeClient).fetchUsers();
        verify(githubClient, never()).fetchUsers();
        verify(cursorClient).fetchUsers();
    }

    @Test
    void testCollectAllUsers_OneClientThrowsException() throws ApiClientException {
        // Arrange
        when(claudeClient.isEnabled()).thenReturn(true);
        when(githubClient.isEnabled()).thenReturn(true);
        when(cursorClient.isEnabled()).thenReturn(true);

        UserData claudeUser = createUserData("claude@example.com", "Claude User", "claude");
        UserData cursorUser = createUserData("cursor@example.com", "Cursor User", "cursor");

        when(claudeClient.fetchUsers()).thenReturn(Arrays.asList(claudeUser));
        when(githubClient.fetchUsers()).thenThrow(new ApiClientException("API Error", new RuntimeException()));
        when(cursorClient.fetchUsers()).thenReturn(Arrays.asList(cursorUser));

        // Act
        Map<String, List<UserData>> results = service.collectAllUsers();

        // Assert
        assertThat(results).hasSize(3);
        assertThat(results.get("claude")).hasSize(1);
        assertThat(results.get("github-copilot")).isEmpty();
        assertThat(results.get("cursor")).hasSize(1);
    }

    @Test
    void testCollectFromTool_Claude_Success() throws ApiClientException {
        // Arrange
        when(claudeClient.isEnabled()).thenReturn(true);
        UserData claudeUser = createUserData("claude@example.com", "Claude User", "claude");
        when(claudeClient.fetchUsers()).thenReturn(Arrays.asList(claudeUser));

        // Act
        List<UserData> results = service.collectFromTool("claude");

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("claude@example.com");
        verify(claudeClient).fetchUsers();
    }

    @Test
    void testCollectFromTool_Disabled() throws ApiClientException {
        // Arrange
        when(claudeClient.isEnabled()).thenReturn(false);

        // Act
        List<UserData> results = service.collectFromTool("claude");

        // Assert
        assertThat(results).isEmpty();
        verify(claudeClient, never()).fetchUsers();
    }

    @Test
    void testCollectFromTool_NotFound() {
        // Act
        List<UserData> results = service.collectFromTool("unknown-tool");

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void testCollectFromClaude() throws ApiClientException {
        // Arrange
        when(claudeClient.isEnabled()).thenReturn(true);
        UserData claudeUser = createUserData("claude@example.com", "Claude User", "claude");
        when(claudeClient.fetchUsers()).thenReturn(Arrays.asList(claudeUser));

        // Act
        List<UserData> results = service.collectFromClaude();

        // Assert
        assertThat(results).hasSize(1);
        verify(claudeClient).fetchUsers();
    }

    @Test
    void testCollectFromGitHub() throws ApiClientException {
        // Arrange
        when(githubClient.isEnabled()).thenReturn(true);
        UserData githubUser = createUserData("github@example.com", "GitHub User", "github-copilot");
        when(githubClient.fetchUsers()).thenReturn(Arrays.asList(githubUser));

        // Act
        List<UserData> results = service.collectFromGitHub();

        // Assert
        assertThat(results).hasSize(1);
        verify(githubClient).fetchUsers();
    }

    @Test
    void testCollectFromCursor() throws ApiClientException {
        // Arrange
        when(cursorClient.isEnabled()).thenReturn(true);
        UserData cursorUser = createUserData("cursor@example.com", "Cursor User", "cursor");
        when(cursorClient.fetchUsers()).thenReturn(Arrays.asList(cursorUser));

        // Act
        List<UserData> results = service.collectFromCursor();

        // Assert
        assertThat(results).hasSize(1);
        verify(cursorClient).fetchUsers();
    }

    @Test
    void testGetApiClients() {
        // Act
        List<ToolApiClient> clients = service.getApiClients();

        // Assert
        assertThat(clients).hasSize(3);
        assertThat(clients).containsExactly(claudeClient, githubClient, cursorClient);
    }

    @Test
    void testCollectAllUsers_MultipleUsersPerTool() throws ApiClientException {
        // Arrange
        when(claudeClient.isEnabled()).thenReturn(true);
        when(githubClient.isEnabled()).thenReturn(true);
        when(cursorClient.isEnabled()).thenReturn(true);

        UserData claude1 = createUserData("claude1@example.com", "User 1", "claude");
        UserData claude2 = createUserData("claude2@example.com", "User 2", "claude");
        UserData github1 = createUserData("github1@example.com", "User 3", "github-copilot");

        when(claudeClient.fetchUsers()).thenReturn(Arrays.asList(claude1, claude2));
        when(githubClient.fetchUsers()).thenReturn(Arrays.asList(github1));
        when(cursorClient.fetchUsers()).thenReturn(Arrays.asList());

        // Act
        Map<String, List<UserData>> results = service.collectAllUsers();

        // Assert
        assertThat(results.get("claude")).hasSize(2);
        assertThat(results.get("github-copilot")).hasSize(1);
        assertThat(results.get("cursor")).isEmpty();
    }

    private UserData createUserData(String email, String name, String tool) {
        UserData userData = new UserData();
        userData.setEmail(email);
        userData.setName(name);
        userData.setStatus("active");
        userData.setLastActivityAt(LocalDateTime.now());
        return userData;
    }
}
