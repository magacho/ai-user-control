# AI User Control

Sistema para coletar e consolidar informações de uso de ferramentas de IA para desenvolvimento (Claude Code, GitHub Copilot e Cursor).

## Stack Técnica

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring WebFlux** (WebClient reativo)
- **Apache Commons CSV**
- **Maven**

## Estrutura do Projeto

```
src/
├── main/java/com/bemobi/aicontrol/
│   ├── integration/
│   │   ├── ToolApiClient.java          # Interface base
│   │   ├── claude/                     # Integração Claude Code
│   │   ├── github/                     # Integração GitHub Copilot
│   │   ├── cursor/                     # Integração Cursor (CSV)
│   │   └── common/                     # DTOs comuns
│   └── config/                         # Configurações Spring
└── test/                               # Testes unitários e de integração
```

## Configuração

### Pré-requisitos

- Java 17+
- Maven 3.6+

### Build

```bash
mvn clean install
```

### Executar Testes

```bash
mvn test
```

## Integrações

### 1. Claude Code (Anthropic API)

#### Obtenção de Credenciais

1. Acesse https://console.anthropic.com/
2. Navegue para **Settings → API Keys**
3. Clique em **Create API Key**
4. Copie o **API Key** e o **Organization ID**

#### Configuração

**Via variáveis de ambiente:**
```bash
export AI_CONTROL_CLAUDE_ENABLED=true
export AI_CONTROL_CLAUDE_TOKEN="sk-ant-api03-xxx"
export AI_CONTROL_CLAUDE_ORG_ID="org_xxx"
```

**Via application.yml:**
```yaml
ai-control:
  api:
    claude:
      enabled: true
      token: ${AI_CONTROL_CLAUDE_TOKEN}
      organization-id: ${AI_CONTROL_CLAUDE_ORG_ID}
```

#### Features

- Busca automática de membros da organização
- Retry automático em caso de rate limit (429)
- Timeout configurável (padrão: 30s)
- Logs estruturados de todas as operações

---

### 2. GitHub Copilot

#### Obtenção de Credenciais

1. Acesse https://github.com/settings/tokens
2. Clique em **Generate new token (classic)**
3. Selecione os scopes:
   - `read:org` - Leitura de informações da organização
   - `manage_billing:copilot` - Acesso aos dados de billing do Copilot
4. Gere e copie o token
5. Identifique o nome da sua organização GitHub

#### Configuração

**Via variáveis de ambiente:**
```bash
export AI_CONTROL_GITHUB_ENABLED=true
export AI_CONTROL_GITHUB_TOKEN="ghp_xxx"
export AI_CONTROL_GITHUB_ORG="my-organization"
```

**Via application.yml:**
```yaml
ai-control:
  api:
    github:
      enabled: true
      token: ${AI_CONTROL_GITHUB_TOKEN}
      organization: ${AI_CONTROL_GITHUB_ORG}
```

#### Features

- Busca de seats do Copilot
- Fallback de email para usuários sem email público (`login@github.local`)
- Detecção e log de rate limits
- Retry automático em erros 5xx
- Tratamento de organizações sem Copilot (404)

---

### 3. Cursor (CSV Import)

> **Nota:** Cursor não possui API pública. A integração é feita via import manual de CSV.

#### Como exportar dados do Cursor

1. Acesse o dashboard administrativo do Cursor
2. Exporte a lista de usuários em formato CSV
3. Salve o arquivo no diretório configurado

#### Formato CSV Esperado

```csv
email,name,status,last_active,joined_at
john.doe@example.com,John Doe,active,2026-01-21,2025-06-01
jane.smith@example.com,Jane Smith,active,2026-01-20,2025-05-15
```

**Campos:**
- `email` *(obrigatório)*: Email do usuário
- `name` *(obrigatório)*: Nome completo
- `status` *(opcional)*: `active` ou `inactive` (default: `active`)
- `last_active` *(opcional)*: Data da última atividade (formato: YYYY-MM-DD)
- `joined_at` *(opcional)*: Data de entrada (formato: YYYY-MM-DD)

#### Configuração

**Via variáveis de ambiente:**
```bash
export AI_CONTROL_CURSOR_ENABLED=true
export AI_CONTROL_CURSOR_CSV_PATH="$HOME/.ai-control/imports/cursor"
```

**Via application.yml:**
```yaml
ai-control:
  api:
    cursor:
      enabled: true
      csv-path: ${AI_CONTROL_CURSOR_CSV_PATH}
```

#### Criar diretório de import

```bash
mkdir -p ~/.ai-control/imports/cursor
```

## Uso Programático

### Exemplo: Buscar usuários do Claude Code

```java
@Autowired
private ClaudeApiClient claudeClient;

public void fetchClaudeUsers() {
    try {
        List<UserData> users = claudeClient.fetchUsers();
        users.forEach(user -> {
            System.out.println("Email: " + user.getEmail());
            System.out.println("Name: " + user.getName());
            System.out.println("Status: " + user.getStatus());
            System.out.println("---");
        });
    } catch (ApiClientException e) {
        log.error("Erro ao buscar usuários: {}", e.getMessage());
    }
}
```

### Exemplo: Import CSV do Cursor

```java
@Autowired
private CursorCsvClient cursorClient;

public void importCursorUsers() {
    try {
        // Buscar arquivo CSV mais recente
        String csvFile = cursorClient.findLatestCsvFile();

        // Importar usuários
        List<UserData> users = cursorClient.importFromCsv(csvFile);

        System.out.println("Importados " + users.size() + " usuários do Cursor");
    } catch (ApiClientException e) {
        log.error("Erro ao importar CSV: {}", e.getMessage());
    }
}
```

### Exemplo: Testar conexão

```java
@Autowired
private List<ToolApiClient> allClients;

public void testAllConnections() {
    allClients.forEach(client -> {
        ConnectionTestResult result = client.testConnection();

        if (result.isSuccess()) {
            System.out.println("✅ " + client.getDisplayName() + ": " + result.getMessage());
        } else {
            System.out.println("❌ " + client.getDisplayName() + ": " + result.getMessage());
        }
    });
}
```

## DTO Unificado (UserData)

Todas as integrações retornam uma lista de `UserData`:

```java
public class UserData {
    private String email;                      // Email (chave primária)
    private String name;                       // Nome completo
    private String status;                     // Status (active/inactive)
    private LocalDateTime lastActivityAt;      // Última atividade
    private Map<String, Object> additionalMetrics; // Métricas específicas
    private String rawJson;                    // Dados brutos (debug)
}
```

### Métricas Adicionais por Ferramenta

**Claude Code:**
- `role`: Papel na organização (member, admin)
- `joined_at`: Data de entrada
- `member_id`: ID do membro

**GitHub Copilot:**
- `last_activity_editor`: Editor usado (vscode, jetbrains, etc.)
- `created_at`: Data de criação do seat
- `updated_at`: Data de atualização
- `github_login`: Login do GitHub
- `github_id`: ID do GitHub

**Cursor:**
- `joined_at`: Data de entrada (se disponível no CSV)

## Tratamento de Erros

Todas as integrações lançam `ApiClientException` em caso de erro:

```java
try {
    List<UserData> users = client.fetchUsers();
} catch (ApiClientException e) {
    // Tratar erro
    log.error("Erro: {}", e.getMessage(), e);
}
```

### Erros Comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `Invalid API key` | Token inválido/expirado | Verificar e regenerar token |
| `Organization not found` | Org ID incorreto | Verificar ID da organização |
| `Rate limit exceeded` | Muitas requisições | Aguardar reset (retry automático) |
| `CSV file not found` | Arquivo não existe | Verificar caminho do CSV |
| `Email is required` | CSV sem campo email | Adicionar coluna `email` |

## Testes

O projeto possui **45 testes unitários** com cobertura >80%:

```bash
# Executar todos os testes
mvn test

# Executar testes de uma classe específica
mvn test -Dtest=ClaudeApiClientTest

# Gerar relatório de cobertura
mvn jacoco:report
# Relatório em: target/site/jacoco/index.html
```

## Logs

O sistema utiliza SLF4J com padrão estruturado:

```
2026-01-22 14:30:00 - Fetching users from Claude Code API
2026-01-22 14:30:01 - Successfully fetched 15 users from Claude Code
```

### Configurar nível de log

**application.yml:**
```yaml
logging:
  level:
    root: INFO
    com.bemobi.aicontrol: DEBUG
```

## Contribuindo

Este projeto segue o protocolo de rastreabilidade definido em `CLAUDE.md`:

- Todos os commits devem incluir trailers:
  ```
  Co-authored-by: Claude Agent <claude@ai.bot>
  X-Agent: [NomeAgente]
  ```

- Issues e PRs devem ter a label `ai-generated`

## Roadmap

- [ ] Interface CLI com Spring Shell
- [ ] Persistência em banco de dados
- [ ] Comandos de coleta e relatórios
- [ ] Testes de integração com WireMock
- [ ] Documentação de APIs externas
- [ ] Docker compose para ambiente local

## Licença

Este projeto é de uso interno da Bemobi.

---

> 🤖 *Generated by Claude Code*
