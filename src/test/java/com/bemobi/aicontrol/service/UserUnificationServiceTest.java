package com.bemobi.aicontrol.service;

import com.bemobi.aicontrol.integration.common.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserUnificationServiceTest {

    private UserUnificationService service;

    @BeforeEach
    void setUp() {
        service = new UserUnificationService();
    }

    @Test
    void unify_singleToolUser_returnsToolsCountOne() {
        Map<String, List<UserData>> data = new HashMap<>();
        data.put("claude", List.of(
                createUserData("joao@emp.com", "Joao", "active")
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(1);
        UnifiedUser user = result.get(0);
        assertThat(user.toolsCount()).isEqualTo(1);
        assertThat(user.usesClaude()).isTrue();
        assertThat(user.usesCopilot()).isFalse();
        assertThat(user.usesCursor()).isFalse();
        assertThat(user.email()).isEqualTo("joao@emp.com");
        assertThat(user.name()).isEqualTo("Joao");
    }

    @Test
    void unify_twoToolsSameEmail_returnsToolsCountTwo() {
        Map<String, List<UserData>> data = new HashMap<>();
        data.put("claude", List.of(
                createUserData("joao@emp.com", "Joao Claude", "active")
        ));
        data.put("github-copilot", List.of(
                createUserData("joao@emp.com", "Joao GitHub", "active")
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(1);
        UnifiedUser user = result.get(0);
        assertThat(user.toolsCount()).isEqualTo(2);
        assertThat(user.usesClaude()).isTrue();
        assertThat(user.usesCopilot()).isTrue();
        assertThat(user.usesCursor()).isFalse();
    }

    @Test
    void unify_threeToolsSameEmail_returnsToolsCountThree() {
        Map<String, List<UserData>> data = new HashMap<>();
        data.put("claude", List.of(
                createUserData("joao@emp.com", "Joao", "active")
        ));
        data.put("github-copilot", List.of(
                createUserData("joao@emp.com", "Joao", "active")
        ));
        data.put("cursor", List.of(
                createUserData("joao@emp.com", "Joao", "active")
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(1);
        UnifiedUser user = result.get(0);
        assertThat(user.toolsCount()).isEqualTo(3);
        assertThat(user.usesClaude()).isTrue();
        assertThat(user.usesCopilot()).isTrue();
        assertThat(user.usesCursor()).isTrue();
    }

    @Test
    void unify_caseInsensitiveEmail_unifiesSameUser() {
        Map<String, List<UserData>> data = new HashMap<>();
        data.put("claude", List.of(
                createUserData("Joao@Emp.com", "Joao", "active")
        ));
        data.put("github-copilot", List.of(
                createUserData("joao@emp.com", "Joao", "active")
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).toolsCount()).isEqualTo(2);
    }

    @Test
    void unify_semUsrGithub_doesNotUnifyBetweenEachOther() {
        Map<String, List<UserData>> data = new HashMap<>();
        data.put("github-copilot", List.of(
                createUserData("[sem-usr-github]", "loginA", "active"),
                createUserData("[sem-usr-github]", "loginB", "active")
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(u -> u.toolsCount() == 1);
    }

    @Test
    void unify_namePriority_githubOverClaudeOverCursor() {
        Map<String, List<UserData>> data = new HashMap<>();
        data.put("cursor", List.of(
                createUserData("joao@emp.com", "Joao Cursor", "active")
        ));
        data.put("claude", List.of(
                createUserData("joao@emp.com", "Joao Claude", "active")
        ));
        data.put("github-copilot", List.of(
                createUserData("joao@emp.com", "Joao GitHub", "active")
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Joao GitHub");
    }

    @Test
    void unify_namePriority_claudeOverCursor() {
        Map<String, List<UserData>> data = new HashMap<>();
        data.put("cursor", List.of(
                createUserData("joao@emp.com", "Joao Cursor", "active")
        ));
        data.put("claude", List.of(
                createUserData("joao@emp.com", "Joao Claude", "active")
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Joao Claude");
    }

    @Test
    void unify_sortsByToolsCountDescThenEmailAsc() {
        Map<String, List<UserData>> data = new HashMap<>();
        data.put("claude", List.of(
                createUserData("alice@emp.com", "Alice", "active"),
                createUserData("bob@emp.com", "Bob", "active"),
                createUserData("charlie@emp.com", "Charlie", "active")
        ));
        data.put("github-copilot", List.of(
                createUserData("charlie@emp.com", "Charlie", "active"),
                createUserData("bob@emp.com", "Bob", "active")
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(3);
        // bob e charlie têm toolsCount=2, alice tem 1
        assertThat(result.get(0).email()).isEqualTo("bob@emp.com");
        assertThat(result.get(0).toolsCount()).isEqualTo(2);
        assertThat(result.get(1).email()).isEqualTo("charlie@emp.com");
        assertThat(result.get(1).toolsCount()).isEqualTo(2);
        assertThat(result.get(2).email()).isEqualTo("alice@emp.com");
        assertThat(result.get(2).toolsCount()).isEqualTo(1);
    }

    @Test
    void unify_emptyMap_returnsEmptyList() {
        List<UnifiedUser> result = service.unify(Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void unify_nullMap_returnsEmptyList() {
        List<UnifiedUser> result = service.unify(null);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSummary_returnsCorrectFormat() {
        Map<String, List<UserData>> rawData = new HashMap<>();
        rawData.put("claude", List.of(
                createUserData("a@e.com", "A", "active"),
                createUserData("b@e.com", "B", "active")
        ));
        rawData.put("github-copilot", List.of(
                createUserData("a@e.com", "A", "active"),
                createUserData("c@e.com", "C", "active")
        ));
        rawData.put("cursor", List.of(
                createUserData("a@e.com", "A", "active")
        ));

        List<UnifiedUser> unified = service.unify(rawData);
        String summary = service.buildSummary(unified, rawData);

        assertThat(summary).contains("=== Resumo da Coleta ===");
        assertThat(summary).contains("Total de usuários únicos: 3");
        assertThat(summary).contains("Claude Code: 2 ativos");
        assertThat(summary).contains("GitHub Copilot: 2 ativos");
        assertThat(summary).contains("Cursor: 1 ativos");
        assertThat(summary).contains("Usam 2+ ferramentas: 1");
        assertThat(summary).contains("Usam todas (3): 1");
        assertThat(summary).contains("Sem email resolvido: 0");
    }

    @Test
    void unify_preservesLastActivityAndStatus() {
        LocalDateTime claudeActivity = LocalDateTime.of(2026, 2, 5, 14, 30, 0);
        LocalDateTime copilotActivity = LocalDateTime.of(2026, 2, 6, 10, 0, 0);

        Map<String, List<UserData>> data = new HashMap<>();
        data.put("claude", List.of(
                new UserData("joao@emp.com", "Joao", "active", claudeActivity, null, null)
        ));
        data.put("github-copilot", List.of(
                new UserData("joao@emp.com", "Joao", "inactive", copilotActivity, null, null)
        ));

        List<UnifiedUser> result = service.unify(data);

        assertThat(result).hasSize(1);
        UnifiedUser user = result.get(0);
        assertThat(user.claudeLastActivity()).isEqualTo("2026-02-05T14:30:00");
        assertThat(user.copilotLastActivity()).isEqualTo("2026-02-06T10:00:00");
        assertThat(user.cursorLastActivity()).isEmpty();
        assertThat(user.claudeStatus()).isEqualTo("active");
        assertThat(user.copilotStatus()).isEqualTo("inactive");
        assertThat(user.cursorStatus()).isEmpty();
    }

    private UserData createUserData(String email, String name, String status) {
        return new UserData(email, name, status, LocalDateTime.now(), null, null);
    }
}
