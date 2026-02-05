package com.bemobi.aicontrol.integration;

import com.bemobi.aicontrol.integration.common.UserData;
import com.bemobi.aicontrol.service.CsvExportService;
import com.bemobi.aicontrol.service.UserCollectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to validate the complete export flow.
 */
@SpringBootTest
class ExportIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testCompleteExportFlow() throws IOException {
        // Create test services
        CsvExportService csvExportService = new CsvExportService(tempDir.toString());

        // Create mock data
        Map<String, List<UserData>> testData = new HashMap<>();
        testData.put("claude", Arrays.asList(createTestUser("claude@test.com", "Claude Test")));
        testData.put("github-copilot", Arrays.asList(createTestUser("github@test.com", "GitHub Test")));
        testData.put("cursor", Arrays.asList(createTestUser("cursor@test.com", "Cursor Test")));

        // Export to CSV
        List<Path> generatedFiles = csvExportService.exportToCsv(testData);

        // Verify files were created
        assertThat(generatedFiles).hasSize(3);
        assertThat(generatedFiles).allMatch(Files::exists);

        // Verify CSV content
        for (Path file : generatedFiles) {
            List<String> lines = Files.readAllLines(file);
            assertThat(lines.size()).isGreaterThan(1); // Header + at least 1 data row
            assertThat(lines.get(0)).contains("tool,email,name,status");
        }

        System.out.println("✅ Export integration test passed!");
        System.out.println("Generated files:");
        for (Path file : generatedFiles) {
            System.out.println("  - " + file.getFileName());
        }
    }

    private UserData createTestUser(String email, String name) {
        UserData user = new UserData();
        user.setEmail(email);
        user.setName(name);
        user.setStatus("active");
        user.setLastActivityAt(LocalDateTime.now());
        return user;
    }
}
