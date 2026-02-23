package com.bemobi.aicontrol.command;

import com.bemobi.aicontrol.service.ConsolidatedReport;
import com.bemobi.aicontrol.service.UnifiedSpendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Command to generate consolidated XLSX report.
 *
 * Enable with: -Dgenerate.report=true
 */
@Component
@ConditionalOnProperty(name = "generate.report", havingValue = "true")
public class ReportCommand implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReportCommand.class);

    private final UnifiedSpendingService spendingService;

    public ReportCommand(UnifiedSpendingService spendingService) {
        this.spendingService = spendingService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Gerando Relatório Consolidado ===");

        // Período: últimos 30 dias
        LocalDate endDate = LocalDate.now().minusDays(1); // Yesterday (data is processed daily)
        LocalDate startDate = endDate.minusDays(29); // 30 days total

        log.info("Período: {} até {}", startDate, endDate);
        log.info("Coletando dados das APIs...");

        // Gerar relatório
        ConsolidatedReport report = spendingService.generateSpendingReport(startDate, endDate);

        log.info("=== Resumo da Coleta ===");
        log.info("Registros de uso: {}", report.usageRecords().size());
        log.info("Registros de custo: {}", report.spendingRecords().size());
        log.info("Usuários únicos: {}", report.summary().userCount());
        log.info("Custo total: ${}", report.summary().totalCostUsd());

        // Exportar para XLSX
        String timestamp = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path outputPath = Paths.get("output/consolidated-report-" + timestamp + ".xlsx");

        log.info("=== Gerando XLSX ===");
        Path xlsxFile = spendingService.exportToXlsx(report, outputPath);

        System.out.println("\n✅ Relatório gerado com sucesso!");
        System.out.println("📄 Arquivo: " + xlsxFile.toAbsolutePath());
        System.out.println("\n📊 Abas consolidadas (agregado do período):");
        System.out.println("  - Aba 1: Volumes de Uso");
        System.out.println("  - Aba 2: GitHub Não Cadastrados");
        System.out.println("  - Aba 3: Usuários Multi-Tool");
        System.out.println("\n🔍 Abas de debug (snapshots):");
        System.out.println("  - Aba 4: Claude - Dados Brutos");
        System.out.println("  - Aba 5: GitHub - Dados Brutos (seats)");
        System.out.println("  - Aba 6: Cursor - Snapshot (último dia)");
        System.out.println();
    }
}
