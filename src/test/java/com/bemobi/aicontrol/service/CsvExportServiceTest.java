package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    @TempDir
    Path tempDir;

    private CsvExportService service;

    @BeforeEach
    void setUp() {
        service = new CsvExportService(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup is handled by @TempDir
    }

    @Test
    void testExportToCsv_SingleTool_Success() throws IOException {
        // Arrange
        Map<String, List<UserData>> userData = new HashMap<>();
        List<UserData> claudeUsers = Arrays.asList(
                createUserData("user1@example.com", "User One", "active"),
                createUserData("user2@example.com", "User Two", "inactive")
        );
        userData.put("claude", claudeUsers);

        // Act
        List<Path> generatedFiles = service.exportToCsv(userData);

        // Assert
        assertThat(generatedFiles).hasSize(1);
        Path csvFile = generatedFiles.get(0);
        assertThat(csvFile).exists();
        assertThat(csvFile.getFileName().toString()).startsWith("claude-users-");
        assertThat(csvFile.getFileName().toString()).endsWith(".csv");

        List<String> lines = Files.readAllLines(csvFile);
        assertThat(lines).hasSize(3); // Header + 2 users
        assertThat(lines.get(0)).isEqualTo("tool,email,name,status,last_activity_at,collected_at,email_type");
        assertThat(lines.get(1)).contains("claude", "user1@example.com", "User One", "active");
        assertThat(lines.get(2)).contains("claude", "user2@example.com", "User Two", "inactive");
    }

    @Test
    void testExportToCsv_MultipleTools_Success() throws IOException {
        // Arrange
        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", Arrays.asList(createUserData("claude@example.com", "Claude User", "active")));
        userData.put("github-copilot", Arrays.asList(createUserData("github@example.com", "GitHub User", "active")));
        userData.put("cursor", Arrays.asList(createUserData("cursor@example.com", "Cursor User", "active")));

        // Act
        List<Path> generatedFiles = service.exportToCsv(userData);

        // Assert
        assertThat(generatedFiles).hasSize(3);
        assertThat(generatedFiles).allMatch(Files::exists);

        Set<String> filenames = new HashSet<>();
        for (Path file : generatedFiles) {
            filenames.add(file.getFileName().toString());
        }

        assertThat(filenames).anyMatch(name -> name.startsWith("claude-users-"));
        assertThat(filenames).anyMatch(name -> name.startsWith("github-copilot-users-"));
        assertThat(filenames).anyMatch(name -> name.startsWith("cursor-users-"));
    }

    @Test
    void testExportToCsv_EmptyUserList_SkipsFile() throws IOException {
        // Arrange
        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", Arrays.asList(createUserData("user@example.com", "User", "active")));
        userData.put("github-copilot", new ArrayList<>()); // Empty list

        // Act
        List<Path> generatedFiles = service.exportToCsv(userData);

        // Assert
        assertThat(generatedFiles).hasSize(1);
        assertThat(generatedFiles.get(0).getFileName().toString()).startsWith("claude-users-");
    }

    @Test
    void testExportToCsv_NullValues_HandledGracefully() throws IOException {
        // Arrange
        UserData userWithNulls = new UserData("user@example.com", null, null, null, null, null);
        // name, status, lastActivityAt are null

        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", Arrays.asList(userWithNulls));

        // Act
        List<Path> generatedFiles = service.exportToCsv(userData);

        // Assert
        assertThat(generatedFiles).hasSize(1);
        List<String> lines = Files.readAllLines(generatedFiles.get(0));
        assertThat(lines).hasSize(2); // Header + 1 user
        assertThat(lines.get(1)).contains("user@example.com");
    }

    @Test
    void testExportToConsolidatedCsv_MultipleTools_Success() throws IOException {
        // Arrange
        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", Arrays.asList(
                createUserData("claude1@example.com", "Claude User 1", "active"),
                createUserData("claude2@example.com", "Claude User 2", "active")
        ));
        userData.put("github-copilot", Arrays.asList(
                createUserData("github@example.com", "GitHub User", "active")
        ));

        // Act
        Path consolidatedFile = service.exportToConsolidatedCsv(userData);

        // Assert
        assertThat(consolidatedFile).exists();
        assertThat(consolidatedFile.getFileName().toString()).startsWith("all-users-consolidated-");
        assertThat(consolidatedFile.getFileName().toString()).endsWith(".csv");

        List<String> lines = Files.readAllLines(consolidatedFile);
        assertThat(lines).hasSize(4); // Header + 3 users
        assertThat(lines.get(0)).isEqualTo("tool,email,name,status,last_activity_at,collected_at,email_type");

        String content = String.join("\n", lines);
        assertThat(content).contains("claude", "claude1@example.com");
        assertThat(content).contains("claude", "claude2@example.com");
        assertThat(content).contains("github-copilot", "github@example.com");
    }

    @Test
    void testExportToConsolidatedCsv_EmptyData_CreatesEmptyFile() throws IOException {
        // Arrange
        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", new ArrayList<>());
        userData.put("github-copilot", new ArrayList<>());

        // Act
        Path consolidatedFile = service.exportToConsolidatedCsv(userData);

        // Assert
        assertThat(consolidatedFile).exists();
        List<String> lines = Files.readAllLines(consolidatedFile);
        assertThat(lines).hasSize(1); // Only header
        assertThat(lines.get(0)).isEqualTo("tool,email,name,status,last_activity_at,collected_at,email_type");
    }

    @Test
    void testCsvFormat_CorrectHeaders() throws IOException {
        // Arrange
        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", Arrays.asList(createUserData("user@example.com", "User", "active")));

        // Act
        List<Path> generatedFiles = service.exportToCsv(userData);

        // Assert
        List<String> lines = Files.readAllLines(generatedFiles.get(0));
        String header = lines.get(0);
        assertThat(header).isEqualTo("tool,email,name,status,last_activity_at,collected_at,email_type");
    }

    @Test
    void testCsvFormat_TimestampFormat() throws IOException {
        // Arrange
        UserData user = new UserData("user@example.com", "User", "active",
                LocalDateTime.of(2026, 2, 5, 14, 30, 45), null, null);

        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", Arrays.asList(user));

        // Act
        List<Path> generatedFiles = service.exportToCsv(userData);

        // Assert
        List<String> lines = Files.readAllLines(generatedFiles.get(0));
        assertThat(lines.get(1)).contains("2026-02-05T14:30:45");
    }

    @Test
    void testGetOutputDirectory() {
        // Assert
        assertThat(service.getOutputDirectory()).isEqualTo(tempDir.toString());
    }

    @Test
    void testOutputDirectory_CreatedIfNotExists() throws IOException {
        // Arrange
        Path newDir = tempDir.resolve("new-output-dir");
        assertThat(newDir).doesNotExist();

        CsvExportService newService = new CsvExportService(newDir.toString());
        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", Arrays.asList(createUserData("user@example.com", "User", "active")));

        // Act
        newService.exportToCsv(userData);

        // Assert
        assertThat(newDir).exists();
    }

    @Test
    void testFileNaming_ContainsTimestamp() throws IOException {
        // Arrange
        Map<String, List<UserData>> userData = new HashMap<>();
        userData.put("claude", Arrays.asList(createUserData("user@example.com", "User", "active")));

        // Act
        List<Path> generatedFiles = service.exportToCsv(userData);

        // Assert
        String filename = generatedFiles.get(0).getFileName().toString();
        // Format: claude-users-yyyyMMdd-HHmmss.csv
        assertThat(filename).matches("claude-users-\\d{8}-\\d{6}\\.csv");
    }

    @Test
    void testExportToCsv_MultipleUsers_AllIncluded() throws IOException {
        // Arrange
        Map<String, List<UserData>> userData = new HashMap<>();
        List<UserData> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            users.add(createUserData("user" + i + "@example.com", "User " + i, "active"));
        }
        userData.put("claude", users);

        // Act
        List<Path> generatedFiles = service.exportToCsv(userData);

        // Assert
        List<String> lines = Files.readAllLines(generatedFiles.get(0));
        assertThat(lines).hasSize(11); // Header + 10 users
    }

    @Test
    void testExportToUnifiedCsv_CorrectHeaders() throws IOException {
        // Arrange
        List<UnifiedUser> users = List.of(
                new UnifiedUser("user@example.com", "User", 2, true, true, false,
                        "2026-02-05T14:30:00", "2026-02-06T10:00:00", "",
                        "active", "active", "", "corporate")
        );

        // Act
        Path csvFile = service.exportToUnifiedCsv(users);

        // Assert
        assertThat(csvFile).exists();
        assertThat(csvFile.getFileName().toString()).startsWith("users-unified-");
        assertThat(csvFile.getFileName().toString()).endsWith(".csv");

        List<String> lines = Files.readAllLines(csvFile);
        assertThat(lines).hasSize(2); // Header + 1 user
        assertThat(lines.get(0)).isEqualTo(
                "email,name,tools_count,uses_claude,uses_copilot,uses_cursor,"
                        + "claude_last_activity,copilot_last_activity,cursor_last_activity,"
                        + "claude_status,copilot_status,cursor_status,email_type"
        );
    }

    @Test
    void testExportToUnifiedCsv_BooleanValues() throws IOException {
        // Arrange
        List<UnifiedUser> users = List.of(
                new UnifiedUser("user@example.com", "User", 2, true, false, true,
                        "", "", "",
                        "", "", "", "")
        );

        // Act
        Path csvFile = service.exportToUnifiedCsv(users);

        // Assert
        List<String> lines = Files.readAllLines(csvFile);
        assertThat(lines.get(1)).contains("true,false,true");
    }

    private UserData createUserData(String email, String name, String status) {
        return new UserData(email, name, status, LocalDateTime.now(), null, null);
    }
}
