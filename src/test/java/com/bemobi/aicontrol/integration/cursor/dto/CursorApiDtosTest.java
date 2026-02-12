package com.bemobi.aicontrol.integration.cursor.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para DTOs da Cursor API (spending e usage).
 */
class CursorApiDtosTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Deve deserializar SpendingDataResponse com snake_case")
    void shouldDeserializeSpendingDataResponse() throws Exception {
        String json = """
            {
                "data": [
                    {
                        "email": "user1@example.com",
                        "name": "User One",
                        "spending": 12.50
                    },
                    {
                        "email": "user2@example.com",
                        "name": "User Two",
                        "spending": 25.75
                    }
                ]
            }
            """;

        SpendingDataResponse response = objectMapper.readValue(json, SpendingDataResponse.class);

        assertNotNull(response);
        assertNotNull(response.data());
        assertEquals(2, response.data().size());

        SpendingRecord record1 = response.data().get(0);
        assertEquals("user1@example.com", record1.email());
        assertEquals("User One", record1.name());
        assertEquals(new BigDecimal("12.50"), record1.spending());

        SpendingRecord record2 = response.data().get(1);
        assertEquals("user2@example.com", record2.email());
        assertEquals("User Two", record2.name());
        assertEquals(new BigDecimal("25.75"), record2.spending());
    }

    @Test
    @DisplayName("Deve deserializar DailyUsageResponse com snake_case")
    void shouldDeserializeDailyUsageResponse() throws Exception {
        String json = """
            {
                "data": [
                    {
                        "email": "user1@example.com",
                        "date": "2024-01-15",
                        "lines_added": 150,
                        "lines_deleted": 50,
                        "acceptance_rate": 0.85,
                        "request_types": {
                            "chat": 10,
                            "autocomplete": 25
                        },
                        "most_used_models": ["gpt-4", "claude-3"],
                        "token_usage": [
                            {
                                "input_tokens": 1000,
                                "output_tokens": 500,
                                "cache_read_tokens": 200,
                                "cache_write_tokens": 100,
                                "model": "gpt-4"
                            },
                            {
                                "input_tokens": 800,
                                "output_tokens": 400,
                                "cache_read_tokens": 150,
                                "cache_write_tokens": 75,
                                "model": "claude-3"
                            }
                        ]
                    }
                ]
            }
            """;

        DailyUsageResponse response = objectMapper.readValue(json, DailyUsageResponse.class);

        assertNotNull(response);
        assertNotNull(response.data());
        assertEquals(1, response.data().size());

        DailyUsageRecord record = response.data().get(0);
        assertEquals("user1@example.com", record.email());
        assertEquals("2024-01-15", record.date());
        assertEquals(150, record.linesAdded());
        assertEquals(50, record.linesDeleted());
        assertEquals(0.85, record.acceptanceRate());

        assertNotNull(record.requestTypes());
        assertEquals(10, record.requestTypes().get("chat"));
        assertEquals(25, record.requestTypes().get("autocomplete"));

        assertNotNull(record.mostUsedModels());
        assertEquals(2, record.mostUsedModels().size());
        assertTrue(record.mostUsedModels().contains("gpt-4"));
        assertTrue(record.mostUsedModels().contains("claude-3"));

        assertNotNull(record.tokenUsage());
        assertEquals(2, record.tokenUsage().size());

        TokenUsage token1 = record.tokenUsage().get(0);
        assertEquals(1000L, token1.inputTokens());
        assertEquals(500L, token1.outputTokens());
        assertEquals(200L, token1.cacheReadTokens());
        assertEquals(100L, token1.cacheWriteTokens());
        assertEquals("gpt-4", token1.model());

        TokenUsage token2 = record.tokenUsage().get(1);
        assertEquals(800L, token2.inputTokens());
        assertEquals(400L, token2.outputTokens());
        assertEquals(150L, token2.cacheReadTokens());
        assertEquals(75L, token2.cacheWriteTokens());
        assertEquals("claude-3", token2.model());
    }

    @Test
    @DisplayName("Deve aceitar valores nulos em campos opcionais de DailyUsageRecord")
    void shouldAcceptNullValuesInOptionalFields() throws Exception {
        String json = """
            {
                "data": [
                    {
                        "email": "user1@example.com",
                        "date": "2024-01-15",
                        "lines_added": null,
                        "lines_deleted": null,
                        "acceptance_rate": null,
                        "request_types": null,
                        "most_used_models": null,
                        "token_usage": []
                    }
                ]
            }
            """;

        DailyUsageResponse response = objectMapper.readValue(json, DailyUsageResponse.class);

        assertNotNull(response);
        DailyUsageRecord record = response.data().get(0);
        assertEquals("user1@example.com", record.email());
        assertEquals("2024-01-15", record.date());
        assertNull(record.linesAdded());
        assertNull(record.linesDeleted());
        assertNull(record.acceptanceRate());
        assertNull(record.requestTypes());
        assertNull(record.mostUsedModels());
        assertNotNull(record.tokenUsage());
        assertTrue(record.tokenUsage().isEmpty());
    }

    @Test
    @DisplayName("Deve aceitar valores nulos em campos opcionais de TokenUsage")
    void shouldAcceptNullValuesInTokenUsage() throws Exception {
        String json = """
            {
                "data": [
                    {
                        "email": "user1@example.com",
                        "date": "2024-01-15",
                        "lines_added": 100,
                        "lines_deleted": 50,
                        "acceptance_rate": 0.75,
                        "request_types": {},
                        "most_used_models": [],
                        "token_usage": [
                            {
                                "input_tokens": 500,
                                "output_tokens": null,
                                "cache_read_tokens": null,
                                "cache_write_tokens": null,
                                "model": "gpt-4"
                            }
                        ]
                    }
                ]
            }
            """;

        DailyUsageResponse response = objectMapper.readValue(json, DailyUsageResponse.class);

        assertNotNull(response);
        DailyUsageRecord record = response.data().get(0);
        TokenUsage tokenUsage = record.tokenUsage().get(0);

        assertEquals(500L, tokenUsage.inputTokens());
        assertNull(tokenUsage.outputTokens());
        assertNull(tokenUsage.cacheReadTokens());
        assertNull(tokenUsage.cacheWriteTokens());
        assertEquals("gpt-4", tokenUsage.model());
    }
}
