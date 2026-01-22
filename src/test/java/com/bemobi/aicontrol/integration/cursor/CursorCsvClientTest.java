package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ConnectionTestResult;
import com.bemobi.aicontrol.integration.common.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CursorCsvClientTest {

    @Mock
    private CursorApiProperties properties;

    private CursorCsvClient client;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getImportMode()).thenReturn("csv");

        client = new CursorCsvClient(properties);
    }

    @Test
    void testGetToolName() {
        assertEquals("cursor", client.getToolName());
    }

    @Test
    void testGetDisplayName() {
        assertEquals("Cursor", client.getDisplayName());
    }

    @Test
    void testIsEnabled() {
        assertTrue(client.isEnabled());
    }

    @Test
    void testFetchUsers_ThrowsUnsupportedOperation() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            client.fetchUsers();
        });

        assertTrue(exception.getMessage().contains("CSV import"));
    }

    @Test
    void testImportFromCsv_Success() throws ApiClientException, IOException {
        // Create test CSV file
        Path csvFile = tempDir.resolve("test-users.csv");
        Files.writeString(csvFile,
            "email,name,status,last_active,joined_at\n" +
            "test@example.com,Test User,active,2026-01-20,2025-06-01\n" +
            "user2@example.com,User Two,inactive,2026-01-15,2025-05-10\n"
        );

        // Execute
        List<UserData> users = client.importFromCsv(csvFile.toString());

        // Verify
        assertNotNull(users);
        assertEquals(2, users.size());

        UserData user1 = users.get(0);
        assertEquals("test@example.com", user1.getEmail());
        assertEquals("Test User", user1.getName());
        assertEquals("active", user1.getStatus());
        assertNotNull(user1.getLastActivityAt());
        assertEquals(LocalDate.of(2026, 1, 20).atStartOfDay(), user1.getLastActivityAt());

        UserData user2 = users.get(1);
        assertEquals("user2@example.com", user2.getEmail());
        assertEquals("User Two", user2.getName());
        assertEquals("inactive", user2.getStatus());
    }

    @Test
    void testImportFromCsv_MinimalFields() throws ApiClientException, IOException {
        // Create test CSV file with only required fields
        Path csvFile = tempDir.resolve("minimal.csv");
        Files.writeString(csvFile,
            "email,name\n" +
            "test@example.com,Test User\n"
        );

        // Execute
        List<UserData> users = client.importFromCsv(csvFile.toString());

        // Verify
        assertNotNull(users);
        assertEquals(1, users.size());

        UserData user = users.get(0);
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getName());
        assertEquals("active", user.getStatus()); // Default value
        assertNull(user.getLastActivityAt());
    }

    @Test
    void testImportFromCsv_FileNotFound() {
        String nonExistentFile = tempDir.resolve("nonexistent.csv").toString();

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.importFromCsv(nonExistentFile);
        });

        assertTrue(exception.getMessage().contains("CSV file not found"));
    }

    @Test
    void testImportFromCsv_MissingEmail() throws IOException {
        // Create test CSV file without email
        Path csvFile = tempDir.resolve("no-email.csv");
        Files.writeString(csvFile,
            "name,status\n" +
            "Test User,active\n"
        );

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.importFromCsv(csvFile.toString());
        });

        assertTrue(exception.getMessage().contains("Failed to parse CSV"));
    }

    @Test
    void testImportFromCsv_EmptyEmail() throws IOException {
        // Create test CSV file with empty email
        Path csvFile = tempDir.resolve("empty-email.csv");
        Files.writeString(csvFile,
            "email,name\n" +
            ",Test User\n"
        );

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.importFromCsv(csvFile.toString());
        });

        assertTrue(exception.getMessage().contains("Failed to parse CSV"));
    }

    @Test
    void testFindLatestCsvFile() throws IOException, ApiClientException, InterruptedException {
        when(properties.getCsvPath()).thenReturn(tempDir.toString());

        // Create multiple CSV files
        Path file1 = tempDir.resolve("file1.csv");
        Path file2 = tempDir.resolve("file2.csv");
        Path file3 = tempDir.resolve("file3.csv");

        Files.writeString(file1, "email,name\n");
        Thread.sleep(10); // Ensure different timestamps
        Files.writeString(file2, "email,name\n");
        Thread.sleep(10);
        Files.writeString(file3, "email,name\n");

        // Execute
        String latestFile = client.findLatestCsvFile();

        // Verify - should return the most recent file
        assertTrue(latestFile.endsWith("file3.csv"));
    }

    @Test
    void testFindLatestCsvFile_NoFiles() {
        when(properties.getCsvPath()).thenReturn(tempDir.toString());

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.findLatestCsvFile();
        });

        assertTrue(exception.getMessage().contains("No CSV files found"));
    }

    @Test
    void testFindLatestCsvFile_DirectoryNotFound() {
        when(properties.getCsvPath()).thenReturn("/nonexistent/directory");

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.findLatestCsvFile();
        });

        assertTrue(exception.getMessage().contains("CSV import directory not found"));
    }

    @Test
    void testFindLatestCsvFile_NullPath() {
        when(properties.getCsvPath()).thenReturn(null);

        ApiClientException exception = assertThrows(ApiClientException.class, () -> {
            client.findLatestCsvFile();
        });

        assertTrue(exception.getMessage().contains("CSV path not configured"));
    }

    @Test
    void testTestConnection_Success() {
        when(properties.getCsvPath()).thenReturn(tempDir.toString());

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("cursor", result.getToolName());
        assertTrue(result.getMessage().contains("accessible"));
    }

    @Test
    void testTestConnection_DirectoryNotFound() {
        when(properties.getCsvPath()).thenReturn("/nonexistent/directory");

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("cursor", result.getToolName());
    }

    @Test
    void testTestConnection_NullPath() {
        when(properties.getCsvPath()).thenReturn(null);

        ConnectionTestResult result = client.testConnection();

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not configured"));
    }
}
