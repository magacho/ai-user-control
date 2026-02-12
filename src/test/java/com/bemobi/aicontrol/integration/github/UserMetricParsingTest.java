package com.bemobi.aicontrol.integration.github;

import com.bemobi.aicontrol.integration.github.dto.UserMetric;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parsing NDJSON (Newline Delimited JSON) from GitHub Copilot Metrics API.
 */
class UserMetricParsingTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testParseSingleLineNdjson() throws Exception {
        String ndjson = "{\"user_name\":\"john.doe\",\"user_email\":\"john@example.com\",\"date\":\"2026-02-10\",\"user_initiated_interaction_count\":50,\"code_generation_activity_count\":40,\"code_acceptance_activity_count\":30,\"loc_suggested_to_add_sum\":500,\"loc_added_sum\":400,\"loc_deleted_sum\":100}";

        UserMetric metric = objectMapper.readValue(ndjson, UserMetric.class);

        assertNotNull(metric);
        assertEquals("john.doe", metric.userName());
        assertEquals("john@example.com", metric.userEmail());
        assertEquals("2026-02-10", metric.date());
        assertEquals(50, metric.userInitiatedInteractionCount());
        assertEquals(40, metric.codeGenerationActivityCount());
        assertEquals(30, metric.codeAcceptanceActivityCount());
        assertEquals(500, metric.locSuggestedToAddSum());
        assertEquals(400, metric.locAddedSum());
        assertEquals(100, metric.locDeletedSum());
    }

    @Test
    void testParseMultiLineNdjson() throws Exception {
        String ndjson = """
                {"user_name":"john.doe","user_email":"john@example.com","date":"2026-02-10","user_initiated_interaction_count":50,"code_generation_activity_count":40,"code_acceptance_activity_count":30,"loc_suggested_to_add_sum":500,"loc_added_sum":400,"loc_deleted_sum":100}
                {"user_name":"jane.smith","user_email":"jane@example.com","date":"2026-02-10","user_initiated_interaction_count":60,"code_generation_activity_count":45,"code_acceptance_activity_count":35,"loc_suggested_to_add_sum":600,"loc_added_sum":500,"loc_deleted_sum":150}
                {"user_name":"bob.jones","user_email":"bob@example.com","date":"2026-02-10","user_initiated_interaction_count":30,"code_generation_activity_count":25,"code_acceptance_activity_count":20,"loc_suggested_to_add_sum":300,"loc_added_sum":250,"loc_deleted_sum":50}
                """;

        List<UserMetric> metrics = parseNdjson(ndjson);

        assertEquals(3, metrics.size());

        UserMetric firstMetric = metrics.get(0);
        assertEquals("john.doe", firstMetric.userName());
        assertEquals("john@example.com", firstMetric.userEmail());
        assertEquals(50, firstMetric.userInitiatedInteractionCount());

        UserMetric secondMetric = metrics.get(1);
        assertEquals("jane.smith", secondMetric.userName());
        assertEquals("jane@example.com", secondMetric.userEmail());
        assertEquals(60, secondMetric.userInitiatedInteractionCount());

        UserMetric thirdMetric = metrics.get(2);
        assertEquals("bob.jones", thirdMetric.userName());
        assertEquals("bob@example.com", thirdMetric.userEmail());
        assertEquals(30, thirdMetric.userInitiatedInteractionCount());
    }

    @Test
    void testParseEmptyNdjson() throws Exception {
        String ndjson = "";

        List<UserMetric> metrics = parseNdjson(ndjson);

        assertNotNull(metrics);
        assertTrue(metrics.isEmpty());
    }

    @Test
    void testParseNdjsonWithBlankLines() throws Exception {
        String ndjson = """
                {"user_name":"john.doe","user_email":"john@example.com","date":"2026-02-10","user_initiated_interaction_count":50,"code_generation_activity_count":40,"code_acceptance_activity_count":30,"loc_suggested_to_add_sum":500,"loc_added_sum":400,"loc_deleted_sum":100}

                {"user_name":"jane.smith","user_email":"jane@example.com","date":"2026-02-10","user_initiated_interaction_count":60,"code_generation_activity_count":45,"code_acceptance_activity_count":35,"loc_suggested_to_add_sum":600,"loc_added_sum":500,"loc_deleted_sum":150}
                """;

        List<UserMetric> metrics = parseNdjson(ndjson);

        assertEquals(2, metrics.size());
        assertEquals("john.doe", metrics.get(0).userName());
        assertEquals("jane.smith", metrics.get(1).userName());
    }

    @Test
    void testParseNdjsonWithNullFields() throws Exception {
        String ndjson = "{\"user_name\":\"john.doe\",\"user_email\":null,\"date\":\"2026-02-10\",\"user_initiated_interaction_count\":50,\"code_generation_activity_count\":40,\"code_acceptance_activity_count\":30,\"loc_suggested_to_add_sum\":500,\"loc_added_sum\":400,\"loc_deleted_sum\":100}";

        UserMetric metric = objectMapper.readValue(ndjson, UserMetric.class);

        assertNotNull(metric);
        assertEquals("john.doe", metric.userName());
        assertNull(metric.userEmail());
        assertEquals("2026-02-10", metric.date());
    }

    @Test
    void testParseNdjsonWithZeroValues() throws Exception {
        String ndjson = "{\"user_name\":\"john.doe\",\"user_email\":\"john@example.com\",\"date\":\"2026-02-10\",\"user_initiated_interaction_count\":0,\"code_generation_activity_count\":0,\"code_acceptance_activity_count\":0,\"loc_suggested_to_add_sum\":0,\"loc_added_sum\":0,\"loc_deleted_sum\":0}";

        UserMetric metric = objectMapper.readValue(ndjson, UserMetric.class);

        assertNotNull(metric);
        assertEquals("john.doe", metric.userName());
        assertEquals(0, metric.userInitiatedInteractionCount());
        assertEquals(0, metric.locSuggestedToAddSum());
        assertEquals(0, metric.locAddedSum());
    }

    /**
     * Helper method to parse NDJSON string into list of UserMetric.
     * This simulates what will be implemented in GitHubCopilotApiClient.
     */
    private List<UserMetric> parseNdjson(String ndjson) throws Exception {
        List<UserMetric> metrics = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(ndjson));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                UserMetric metric = objectMapper.readValue(line, UserMetric.class);
                metrics.add(metric);
            }
        }
        return metrics;
    }
}
