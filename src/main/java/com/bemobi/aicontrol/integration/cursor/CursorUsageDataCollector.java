package com.bemobi.aicontrol.integration.cursor;

import com.bemobi.aicontrol.integration.common.ApiClientException;
import com.bemobi.aicontrol.integration.common.ToolType;
import com.bemobi.aicontrol.integration.common.UnifiedSpendingRecord;
import com.bemobi.aicontrol.integration.common.UnifiedUsageRecord;
import com.bemobi.aicontrol.integration.common.UsageDataCollector;
import com.bemobi.aicontrol.integration.cursor.dto.DailyUsageRecord;
import com.bemobi.aicontrol.integration.cursor.dto.DailyUsageResponse;
import com.bemobi.aicontrol.integration.cursor.dto.SpendingDataResponse;
import com.bemobi.aicontrol.integration.cursor.dto.SpendingRecord;
import com.bemobi.aicontrol.integration.cursor.dto.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementação do UsageDataCollector para Cursor.
 *
 * <p>Coleta dados de uso e spending da Cursor Admin API e converte para o formato unificado.
 * Consolida tokens de múltiplos modelos e preserva detalhes originais em rawMetadata.</p>
 */
@Component
@ConditionalOnProperty(prefix = "ai-control.api.cursor", name = "enabled", havingValue = "true")
public class CursorUsageDataCollector implements UsageDataCollector {

    private static final Logger log = LoggerFactory.getLogger(CursorUsageDataCollector.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final CursorApiClient cursorApiClient;

    public CursorUsageDataCollector(CursorApiClient cursorApiClient) {
        this.cursorApiClient = cursorApiClient;
    }

    @Override
    public List<UnifiedUsageRecord> collectUsageData(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("Collecting usage data from Cursor for period {} to {}", startDate, endDate);

        DailyUsageResponse response = cursorApiClient.fetchDailyUsage();

        List<UnifiedUsageRecord> records = response.data().stream()
            .filter(record -> isWithinDateRange(record.date(), startDate, endDate))
            .map(this::convertToUnifiedUsageRecord)
            .collect(Collectors.toList());

        log.info("Collected {} usage records from Cursor", records.size());

        return records;
    }

    @Override
    public List<UnifiedSpendingRecord> collectSpendingData(LocalDate startDate, LocalDate endDate)
            throws ApiClientException {
        log.info("Collecting spending data from Cursor for period {} to {}", startDate, endDate);

        SpendingDataResponse response = cursorApiClient.fetchSpendingData();

        // Cursor spending data doesn't have date granularity, so we use the period as string
        String period = startDate.equals(endDate)
            ? startDate.format(DATE_FORMATTER)
            : startDate.format(DATE_FORMATTER) + "_" + endDate.format(DATE_FORMATTER);

        List<UnifiedSpendingRecord> records = response.data().stream()
            .map(record -> convertToUnifiedSpendingRecord(record, period))
            .collect(Collectors.toList());

        log.info("Collected {} spending records from Cursor", records.size());

        return records;
    }

    @Override
    public ToolType getToolType() {
        return ToolType.CURSOR;
    }

    /**
     * Converte DailyUsageRecord para UnifiedUsageRecord.
     *
     * <p>Consolida tokens de múltiplos modelos somando os totais e preserva
     * detalhes de uso por modelo em rawMetadata.</p>
     */
    private UnifiedUsageRecord convertToUnifiedUsageRecord(DailyUsageRecord record) {
        // Consolidar tokens de múltiplos modelos
        TokenTotals totals = consolidateTokens(record.tokenUsage());

        // Preparar rawMetadata com detalhes originais
        Map<String, Object> rawMetadata = new HashMap<>();
        if (record.requestTypes() != null) {
            rawMetadata.put("request_types", record.requestTypes());
        }
        if (record.mostUsedModels() != null && !record.mostUsedModels().isEmpty()) {
            rawMetadata.put("most_used_models", record.mostUsedModels());
        }
        if (record.tokenUsage() != null && !record.tokenUsage().isEmpty()) {
            rawMetadata.put("token_usage_by_model", record.tokenUsage());
        }

        // Calcular linhas sugeridas e aceitas
        Integer linesSuggested = calculateLinesSuggested(record);
        Integer linesAccepted = record.linesAdded();

        LocalDate date = record.date() != null
            ? LocalDate.parse(record.date(), DATE_FORMATTER)
            : null;

        return new UnifiedUsageRecord(
            record.email() != null ? record.email().toLowerCase() : null,
            ToolType.CURSOR,
            date,
            totals.inputTokens(),
            totals.outputTokens(),
            totals.cacheReadTokens(),
            linesSuggested,
            linesAccepted,
            record.acceptanceRate(),
            rawMetadata
        );
    }

    /**
     * Converte SpendingRecord para UnifiedSpendingRecord.
     */
    private UnifiedSpendingRecord convertToUnifiedSpendingRecord(SpendingRecord record, String period) {
        Map<String, Object> rawMetadata = new HashMap<>();
        if (record.name() != null) {
            rawMetadata.put("name", record.name());
        }

        return new UnifiedSpendingRecord(
            record.email() != null ? record.email().toLowerCase() : null,
            ToolType.CURSOR,
            period,
            record.spending() != null ? record.spending() : BigDecimal.ZERO,
            "USD",
            rawMetadata
        );
    }

    /**
     * Consolida tokens de múltiplos modelos.
     *
     * <p>Soma inputTokens, outputTokens, cacheReadTokens de todos os modelos.
     * Retorna null para cada campo se nenhum modelo forneceu dados para aquele campo.</p>
     */
    private TokenTotals consolidateTokens(List<TokenUsage> tokenUsageList) {
        if (tokenUsageList == null || tokenUsageList.isEmpty()) {
            return new TokenTotals(null, null, null);
        }

        long inputTotal = 0;
        long outputTotal = 0;
        long cacheReadTotal = 0;
        boolean hasInput = false;
        boolean hasOutput = false;
        boolean hasCacheRead = false;

        for (TokenUsage usage : tokenUsageList) {
            if (usage.inputTokens() != null) {
                inputTotal += usage.inputTokens();
                hasInput = true;
            }
            if (usage.outputTokens() != null) {
                outputTotal += usage.outputTokens();
                hasOutput = true;
            }
            if (usage.cacheReadTokens() != null) {
                cacheReadTotal += usage.cacheReadTokens();
                hasCacheRead = true;
            }
            // Note: cacheWriteTokens não está em UnifiedUsageRecord, então não consolidamos
        }

        return new TokenTotals(
            hasInput ? inputTotal : null,
            hasOutput ? outputTotal : null,
            hasCacheRead ? cacheReadTotal : null
        );
    }

    /**
     * Calcula linhas sugeridas aproximadamente como linesAdded + linesDeleted.
     *
     * <p>Cursor fornece linesAdded e linesDeleted. Podemos considerar que linhas sugeridas
     * incluem tanto as adicionadas quanto as deletadas (mudanças totais).</p>
     */
    private Integer calculateLinesSuggested(DailyUsageRecord record) {
        Integer added = record.linesAdded();
        Integer deleted = record.linesDeleted();

        if (added == null && deleted == null) {
            return null;
        }

        int total = 0;
        if (added != null) {
            total += added;
        }
        if (deleted != null) {
            total += deleted;
        }

        return total;
    }

    /**
     * Verifica se uma data está dentro do intervalo especificado.
     */
    private boolean isWithinDateRange(String dateStr, LocalDate startDate, LocalDate endDate) {
        if (dateStr == null) {
            return false;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return !date.isBefore(startDate) && !date.isAfter(endDate);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr, e);
            return false;
        }
    }

    /**
     * Record auxiliar para consolidação de tokens.
     */
    private record TokenTotals(Long inputTokens, Long outputTokens, Long cacheReadTokens) {}
}
