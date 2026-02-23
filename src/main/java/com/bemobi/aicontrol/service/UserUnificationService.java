package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsável por unificar dados de usuários coletados de múltiplas ferramentas de IA.
 *
 * Agrupa por email (case-insensitive) e produz uma lista de {@link UnifiedUser}
 * com uma entrada por usuário, contendo flags de presença por ferramenta.
 */
@Service
public class UserUnificationService {

    private static final Logger log = LoggerFactory.getLogger(UserUnificationService.class);
    private static final DateTimeFormatter CSV_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String NO_GITHUB_USER_PREFIX = "[sem-usr-github]";

    /**
     * Unifica dados de usuários de múltiplas ferramentas em uma lista de usuários únicos.
     *
     * @param dataByTool Map com nome da ferramenta como chave e lista de UserData como valor
     * @return Lista de UnifiedUser ordenada por toolsCount DESC, email ASC
     */
    public List<UnifiedUser> unify(Map<String, List<UserData>> dataByTool) {
        if (dataByTool == null || dataByTool.isEmpty()) {
            return List.of();
        }

        Map<String, UserBuilder> buildersByKey = new LinkedHashMap<>();

        for (Map.Entry<String, List<UserData>> entry : dataByTool.entrySet()) {
            String toolName = entry.getKey();
            List<UserData> users = entry.getValue();

            for (UserData user : users) {
                String key = resolveKey(user, toolName);
                UserBuilder builder = buildersByKey.computeIfAbsent(key, k -> new UserBuilder(resolveEmail(user)));
                builder.applyTool(toolName, user);
            }
        }

        List<UnifiedUser> result = new ArrayList<>();
        for (UserBuilder builder : buildersByKey.values()) {
            result.add(builder.build());
        }

        result.sort(Comparator
                .comparingInt(UnifiedUser::toolsCount).reversed()
                .thenComparing(UnifiedUser::email, String.CASE_INSENSITIVE_ORDER));

        log.info("Unified {} tool entries into {} unique users", countTotalEntries(dataByTool), result.size());
        return result;
    }

    /**
     * Gera um resumo textual da coleta de dados.
     *
     * @param users     Lista de usuários unificados
     * @param rawData   Dados brutos por ferramenta
     * @return Texto formatado com o resumo
     */
    public String buildSummary(List<UnifiedUser> users, Map<String, List<UserData>> rawData) {
        int total = users.size();
        long claudeCount = countByTool(rawData, "claude");
        long copilotCount = countByTool(rawData, "github-copilot");
        long cursorCount = countByTool(rawData, "cursor");
        long multiTool = users.stream().filter(u -> u.toolsCount() >= 2).count();
        long allThree = users.stream().filter(u -> u.toolsCount() == 3).count();
        long noEmail = users.stream().filter(u -> u.email().startsWith(NO_GITHUB_USER_PREFIX)).count();

        String multiToolPct = total > 0 ? String.valueOf(Math.round((double) multiTool / total * 100)) : "0";
        String allThreePct = total > 0 ? String.valueOf(Math.round((double) allThree / total * 100)) : "0";

        StringBuilder sb = new StringBuilder();
        sb.append("=== Resumo da Coleta ===\n");
        sb.append(String.format("Total de usuários únicos: %d%n", total));
        sb.append(String.format("Claude Code: %d ativos%n", claudeCount));
        sb.append(String.format("GitHub Copilot: %d ativos%n", copilotCount));
        sb.append(String.format("Cursor: %d ativos%n", cursorCount));
        sb.append(String.format("Usam 2+ ferramentas: %d (%s%%)%n", multiTool, multiToolPct));
        sb.append(String.format("Usam todas (3): %d (%s%%)%n", allThree, allThreePct));
        sb.append(String.format("Sem email resolvido: %d", noEmail));

        return sb.toString();
    }

    private String resolveKey(UserData user, String toolName) {
        String email = user.email() != null ? user.email() : "";
        if (email.startsWith(NO_GITHUB_USER_PREFIX)) {
            String login = extractGithubLogin(user);
            return NO_GITHUB_USER_PREFIX + "#" + login;
        }
        return email.toLowerCase().trim();
    }

    private String resolveEmail(UserData user) {
        return user.email() != null ? user.email() : "";
    }

    private String extractGithubLogin(UserData user) {
        String email = user.email() != null ? user.email() : "";
        if (email.startsWith(NO_GITHUB_USER_PREFIX)) {
            // O email pode conter informação do login após o prefixo
            // Formato esperado: [sem-usr-github] ou o name pode ser o login
            String name = user.name() != null ? user.name() : "";
            return name.isEmpty() ? email : name;
        }
        return email;
    }

    private long countByTool(Map<String, List<UserData>> rawData, String toolName) {
        List<UserData> users = rawData.get(toolName);
        return users != null ? users.size() : 0;
    }

    private int countTotalEntries(Map<String, List<UserData>> dataByTool) {
        return dataByTool.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Builder interno para acumular dados de um usuário ao longo de múltiplas ferramentas.
     */
    private static class UserBuilder {
        private final String email;
        private String name;
        private boolean usesClaude;
        private boolean usesCopilot;
        private boolean usesCursor;
        private String claudeLastActivity = "";
        private String copilotLastActivity = "";
        private String cursorLastActivity = "";
        private String claudeStatus = "";
        private String copilotStatus = "";
        private String cursorStatus = "";
        private String emailType = "";

        // Prioridade de nome: github-copilot > claude > cursor
        private int namePriority = -1;

        UserBuilder(String email) {
            this.email = email;
        }

        void applyTool(String toolName, UserData user) {
            String lastActivity = user.lastActivityAt() != null
                    ? user.lastActivityAt().format(CSV_DATETIME_FORMATTER)
                    : "";
            String status = user.status() != null ? user.status() : "";

            switch (toolName) {
                case "claude" -> {
                    usesClaude = true;
                    claudeLastActivity = lastActivity;
                    claudeStatus = status;
                    applyName(user.name(), 1); // prioridade média
                }
                case "github-copilot" -> {
                    usesCopilot = true;
                    copilotLastActivity = lastActivity;
                    copilotStatus = status;
                    applyName(user.name(), 2); // prioridade alta
                }
                case "cursor" -> {
                    usesCursor = true;
                    cursorLastActivity = lastActivity;
                    cursorStatus = status;
                    applyName(user.name(), 0); // prioridade baixa
                }
                default -> log.warn("Unknown tool: {}", toolName);
            }

            // Captura email_type do additionalMetrics
            if (user.additionalMetrics() != null && user.additionalMetrics().containsKey("email_type")) {
                String type = user.additionalMetrics().get("email_type").toString();
                if (!type.isEmpty()) {
                    emailType = type;
                }
            }
        }

        private void applyName(String candidateName, int priority) {
            if (candidateName != null && !candidateName.isEmpty() && priority > namePriority) {
                this.name = candidateName;
                this.namePriority = priority;
            }
        }

        UnifiedUser build() {
            int toolsCount = (usesClaude ? 1 : 0) + (usesCopilot ? 1 : 0) + (usesCursor ? 1 : 0);
            return new UnifiedUser(
                    email,
                    name != null ? name : "",
                    toolsCount,
                    usesClaude,
                    usesCopilot,
                    usesCursor,
                    claudeLastActivity,
                    copilotLastActivity,
                    cursorLastActivity,
                    claudeStatus,
                    copilotStatus,
                    cursorStatus,
                    emailType
            );
        }
    }
}
