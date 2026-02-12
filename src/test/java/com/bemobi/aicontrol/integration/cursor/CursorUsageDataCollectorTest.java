package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.cursor.dto.DailyUsageRecord;
import com.bemobi.aicontrol.integration.cursor.dto.DailyUsageResponse;
import com.bemobi.aicontrol.integration.cursor.dto.SpendingDataResponse;
import com.bemobi.aicontrol.integration.cursor.dto.SpendingRecord;
import com.bemobi.aicontrol.integration.cursor.dto.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CursorUsageDataCollector.
 */
@ExtendWith(MockitoExtension.class)
class CursorUsageDataCollectorTest {

    @Mock
    private CursorApiClient cursorApiClient;

    private CursorUsageDataCollector collector;

    @BeforeEach
    void setUp() {
        collector = new CursorUsageDataCollector(cursorApiClient);
    }

    @Test
    @DisplayName("Deve retornar ToolType.CURSOR")
    void shouldReturnCursorToolType() {
        assertEquals(ToolType.CURSOR, collector.getToolType());
    }

    @Test
    @DisplayName("Deve coletar e converter dados de usage corretamente")
    void shouldCollectAndConvertUsageData() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 16);

        TokenUsage token1 = new TokenUsage(1000L, 500L, 200L, 100L, "gpt-4");
        TokenUsage token2 = new TokenUsage(800L, 400L, 150L, 75L, "claude-3");

        DailyUsageRecord record1 = new DailyUsageRecord(
            "user1@example.com",
            "2024-01-15",
            150,
            50,
            0.85,
            Map.of("chat", 10, "autocomplete", 25),
            List.of("gpt-4", "claude-3"),
            List.of(token1, token2)
        );

        DailyUsageRecord record2 = new DailyUsageRecord(
            "user2@example.com",
            "2024-01-16",
            100,
            30,
            0.75,
            Map.of("chat", 5),
            List.of("gpt-4"),
            List.of(token1)
        );

        DailyUsageResponse response = new DailyUsageResponse(List.of(record1, record2));

        when(cursorApiClient.fetchDailyUsage()).thenReturn(response);

        // Act
        List<UnifiedUsageRecord> results = collector.collectUsageData(startDate, endDate);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        // Verificar primeiro registro
        UnifiedUsageRecord unified1 = results.get(0);
        assertEquals("user1@example.com", unified1.email());
        assertEquals(ToolType.CURSOR, unified1.tool());
        assertEquals(LocalDate.of(2024, 1, 15), unified1.date());
        assertEquals(1800L, unified1.inputTokens()); // 1000 + 800
        assertEquals(900L, unified1.outputTokens()); // 500 + 400
        assertEquals(350L, unified1.cacheReadTokens()); // 200 + 150
        assertEquals(200, unified1.linesSuggested()); // 150 + 50
        assertEquals(150, unified1.linesAccepted()); // linesAdded
        assertEquals(0.85, unified1.acceptanceRate());

        // Verificar rawMetadata
        assertNotNull(unified1.rawMetadata());
        assertTrue(unified1.rawMetadata().containsKey("request_types"));
        assertTrue(unified1.rawMetadata().containsKey("most_used_models"));
        assertTrue(unified1.rawMetadata().containsKey("token_usage_by_model"));

        // Verificar segundo registro
        UnifiedUsageRecord unified2 = results.get(1);
        assertEquals("user2@example.com", unified2.email());
        assertEquals(LocalDate.of(2024, 1, 16), unified2.date());
        assertEquals(1000L, unified2.inputTokens());
        assertEquals(500L, unified2.outputTokens());
        assertEquals(200L, unified2.cacheReadTokens());
        assertEquals(130, unified2.linesSuggested()); // 100 + 30
        assertEquals(100, unified2.linesAccepted());

        verify(cursorApiClient).fetchDailyUsage();
    }

    @Test
    @DisplayName("Deve filtrar registros fora do período especificado")
    void shouldFilterRecordsOutsideDateRange() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 15);

        DailyUsageRecord record1 = new DailyUsageRecord(
            "user1@example.com",
            "2024-01-15",
            150,
            50,
            0.85,
            null,
            null,
            List.of()
        );

        DailyUsageRecord record2 = new DailyUsageRecord(
            "user2@example.com",
            "2024-01-16", // Fora do período
            100,
            30,
            0.75,
            null,
            null,
            List.of()
        );

        DailyUsageResponse response = new DailyUsageResponse(List.of(record1, record2));

        when(cursorApiClient.fetchDailyUsage()).thenReturn(response);

        // Act
        List<UnifiedUsageRecord> results = collector.collectUsageData(startDate, endDate);

        // Assert
        assertEquals(1, results.size());
        assertEquals("user1@example.com", results.get(0).email());
    }

    @Test
    @DisplayName("Deve lidar com campos nulos em DailyUsageRecord")
    void shouldHandleNullFieldsInDailyUsageRecord() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 15);

        DailyUsageRecord record = new DailyUsageRecord(
            "user1@example.com",
            "2024-01-15",
            null,
            null,
            null,
            null,
            null,
            List.of()
        );

        DailyUsageResponse response = new DailyUsageResponse(List.of(record));

        when(cursorApiClient.fetchDailyUsage()).thenReturn(response);

        // Act
        List<UnifiedUsageRecord> results = collector.collectUsageData(startDate, endDate);

        // Assert
        assertEquals(1, results.size());
        UnifiedUsageRecord unified = results.get(0);
        assertNull(unified.inputTokens());
        assertNull(unified.outputTokens());
        assertNull(unified.cacheReadTokens());
        assertNull(unified.linesSuggested());
        assertNull(unified.linesAccepted());
        assertNull(unified.acceptanceRate());
    }

    @Test
    @DisplayName("Deve consolidar tokens de múltiplos modelos corretamente")
    void shouldConsolidateTokensFromMultipleModels() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 15);

        TokenUsage token1 = new TokenUsage(1000L, 500L, 200L, 100L, "gpt-4");
        TokenUsage token2 = new TokenUsage(800L, 400L, null, 75L, "claude-3");
        TokenUsage token3 = new TokenUsage(600L, null, 100L, null, "gpt-3.5");

        DailyUsageRecord record = new DailyUsageRecord(
            "user1@example.com",
            "2024-01-15",
            150,
            50,
            0.85,
            null,
            null,
            List.of(token1, token2, token3)
        );

        DailyUsageResponse response = new DailyUsageResponse(List.of(record));

        when(cursorApiClient.fetchDailyUsage()).thenReturn(response);

        // Act
        List<UnifiedUsageRecord> results = collector.collectUsageData(startDate, endDate);

        // Assert
        assertEquals(1, results.size());
        UnifiedUsageRecord unified = results.get(0);
        assertEquals(2400L, unified.inputTokens()); // 1000 + 800 + 600
        assertEquals(900L, unified.outputTokens()); // 500 + 400 (token3 não tem)
        assertEquals(300L, unified.cacheReadTokens()); // 200 + 100 (token2 não tem)
    }

    @Test
    @DisplayName("Deve coletar e converter dados de spending corretamente")
    void shouldCollectAndConvertSpendingData() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        SpendingRecord record1 = new SpendingRecord(
            "user1@example.com",
            "User One",
            new BigDecimal("12.50")
        );

        SpendingRecord record2 = new SpendingRecord(
            "user2@example.com",
            "User Two",
            new BigDecimal("25.75")
        );

        SpendingDataResponse response = new SpendingDataResponse(List.of(record1, record2));

        when(cursorApiClient.fetchSpendingData()).thenReturn(response);

        // Act
        List<UnifiedSpendingRecord> results = collector.collectSpendingData(startDate, endDate);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        UnifiedSpendingRecord unified1 = results.get(0);
        assertEquals("user1@example.com", unified1.email());
        assertEquals(ToolType.CURSOR, unified1.tool());
        assertEquals("2024-01-01_2024-01-31", unified1.period());
        assertEquals(new BigDecimal("12.50"), unified1.costUsd());
        assertEquals("USD", unified1.currency());
        assertTrue(unified1.rawMetadata().containsKey("name"));
        assertEquals("User One", unified1.rawMetadata().get("name"));

        UnifiedSpendingRecord unified2 = results.get(1);
        assertEquals("user2@example.com", unified2.email());
        assertEquals(new BigDecimal("25.75"), unified2.costUsd());
        assertEquals("User Two", unified2.rawMetadata().get("name"));

        verify(cursorApiClient).fetchSpendingData();
    }

    @Test
    @DisplayName("Deve usar data única como período quando startDate == endDate")
    void shouldUseSingleDateAsPeriod() throws ApiClientException {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);

        SpendingRecord record = new SpendingRecord(
            "user1@example.com",
            "User One",
            new BigDecimal("10.00")
        );

        SpendingDataResponse response = new SpendingDataResponse(List.of(record));

        when(cursorApiClient.fetchSpendingData()).thenReturn(response);

        // Act
        List<UnifiedSpendingRecord> results = collector.collectSpendingData(date, date);

        // Assert
        assertEquals(1, results.size());
        assertEquals("2024-01-15", results.get(0).period());
    }

    @Test
    @DisplayName("Deve lidar com spending null ou zero")
    void shouldHandleNullOrZeroSpending() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        SpendingRecord record = new SpendingRecord(
            "user1@example.com",
            "User One",
            null
        );

        SpendingDataResponse response = new SpendingDataResponse(List.of(record));

        when(cursorApiClient.fetchSpendingData()).thenReturn(response);

        // Act
        List<UnifiedSpendingRecord> results = collector.collectSpendingData(startDate, endDate);

        // Assert
        assertEquals(1, results.size());
        assertEquals(BigDecimal.ZERO, results.get(0).costUsd());
    }

    @Test
    @DisplayName("Deve converter email para lowercase")
    void shouldConvertEmailToLowercase() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 15);

        DailyUsageRecord usageRecord = new DailyUsageRecord(
            "User1@EXAMPLE.COM",
            "2024-01-15",
            100,
            50,
            0.85,
            null,
            null,
            List.of()
        );

        SpendingRecord spendingRecord = new SpendingRecord(
            "User2@EXAMPLE.COM",
            "User Two",
            new BigDecimal("10.00")
        );

        when(cursorApiClient.fetchDailyUsage()).thenReturn(new DailyUsageResponse(List.of(usageRecord)));
        when(cursorApiClient.fetchSpendingData()).thenReturn(new SpendingDataResponse(List.of(spendingRecord)));

        // Act
        List<UnifiedUsageRecord> usageResults = collector.collectUsageData(startDate, endDate);
        List<UnifiedSpendingRecord> spendingResults = collector.collectSpendingData(startDate, endDate);

        // Assert
        assertEquals("user1@example.com", usageResults.get(0).email());
        assertEquals("user2@example.com", spendingResults.get(0).email());
    }

    @Test
    @DisplayName("Deve propagar ApiClientException ao falhar fetchDailyUsage")
    void shouldPropagateApiClientExceptionFromFetchDailyUsage() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 15);

        when(cursorApiClient.fetchDailyUsage()).thenThrow(new ApiClientException("API Error"));

        // Act & Assert
        assertThrows(ApiClientException.class, () -> {
            collector.collectUsageData(startDate, endDate);
        });
    }

    @Test
    @DisplayName("Deve propagar ApiClientException ao falhar fetchSpendingData")
    void shouldPropagateApiClientExceptionFromFetchSpendingData() throws ApiClientException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 1, 15);

        when(cursorApiClient.fetchSpendingData()).thenThrow(new ApiClientException("API Error"));

        // Act & Assert
        assertThrows(ApiClientException.class, () -> {
            collector.collectSpendingData(startDate, endDate);
        });
    }
}
