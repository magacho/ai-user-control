# AI User Control

Sistema para coletar e consolidar informacoes de uso de ferramentas de IA para desenvolvimento (Claude Code, GitHub Copilot e Cursor), gerando CSVs unificados por usuario.

## Stack Tecnica

- **Java 21**
- **Spring Boot 3.5.10**
- **Spring WebFlux** (WebClient reativo)
- **Apache Commons CSV**
- **Google Workspace Admin SDK** (resolucao de email)
- **Maven**

## Estrutura do Projeto

```
src/
├── main/java/com/bemobi/aicontrol/
│   ├── AiUserControlApplication.java
│   ├── config/
│   │   └── ApiClientConfiguration.java       # Configuracao Spring
│   ├── integration/
│   │   ├── ToolApiClient.java                 # Interface base (Strategy Pattern)
│   │   ├── common/
│   │   │   ├── UserData.java                  # DTO unificado (record)
│   │   │   ├── ApiClientException.java
│   │   │   └── ConnectionTestResult.java
│   │   ├── claude/                            # Anthropic Admin API
│   │   ├── github/                            # GitHub Copilot API
│   │   ├── cursor/                            # Cursor Admin API + CSV legado
│   │   └── google/                            # Google Workspace (resolucao email)
│   ├── service/
│   │   ├── UserCollectionService.java         # Orquestra coleta de todas as APIs
│   │   ├── UserUnificationService.java        # Unifica usuarios por email
│   │   ├── UnifiedUser.java                   # Record do usuario unificado
│   │   └── CsvExportService.java              # Exportacao para CSV
│   └── runner/
│       └── DataCollectionRunner.java          # CommandLineRunner (startup)
└── test/                                      # 107+ testes unitarios
```

## Configuracao

### Pre-requisitos

- Java 21+
- Maven 3.6+

### Build

```bash
mvn clean install
```

### Executar Testes

```bash
mvn test
```

### Executar

```bash
mvn spring-boot:run
```

## Integracoes

### 1. Claude Code (Anthropic Admin API)

#### Obtencao de Credenciais

**Importante:** Voce precisa de uma **Admin API Key**, nao uma chave API regular!

1. Acesse https://console.anthropic.com/
2. Navegue para **Settings → API Keys**
3. Procure pela secao **Admin API Keys** (nao "API Keys")
4. Clique em **Create Admin Key**
5. Copie a chave que comeca com `sk-ant-admin-...`

**Requisitos:**
- Apenas membros com role **admin** podem criar Admin API Keys
- Admin Keys comecam com `sk-ant-admin-...`
- Chaves regulares (`sk-ant-api03-...`) **nao funcionam** para gerenciamento de organizacao

#### Configuracao

**Via variaveis de ambiente:**
```bash
export AI_CONTROL_CLAUDE_ENABLED=true
export AI_CONTROL_CLAUDE_TOKEN="sk-ant-admin-xxx"
```

**Via application.yml:**
```yaml
ai-control:
  api:
    claude:
      enabled: true
      token: ${AI_CONTROL_CLAUDE_TOKEN}
```

#### Features

- Busca automatica de usuarios da organizacao via Admin API
- Suporte a paginacao (ate 100 usuarios por requisicao)
- Retry automatico em caso de rate limit (429)
- Timeout configuravel (padrao: 30s)
- Logs estruturados de todas as operacoes
- API Documentation: https://docs.anthropic.com/en/api/administration-api

---

### 2. GitHub Copilot

#### Obtencao de Credenciais

1. Acesse https://github.com/settings/tokens
2. Clique em **Generate new token (classic)**
3. Selecione os scopes:
   - `read:org` - Leitura de informacoes da organizacao
   - `manage_billing:copilot` - Acesso aos dados de billing do Copilot
4. Gere e copie o token
5. Identifique o nome da sua organizacao GitHub

#### Configuracao

**Via variaveis de ambiente:**
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
- Fallback de email para usuarios sem email publico (`login@github.local`)
- Deteccao e log de rate limits
- Retry automatico em erros 5xx
- Tratamento de organizacoes sem Copilot (404)

---

### 3. Cursor (Admin API + CSV legado)

O Cursor suporta dois metodos de coleta: **Admin API** (primario) e **importacao CSV** (legado).

#### Obtencao de Credenciais (Admin API)

1. Acesse **Cursor Settings → Teams → Admin API**
2. Clique em **"Create API Key"**
3. Copie a chave que comeca com `cur_...`
4. **Importante:** Apenas administradores do team podem criar API keys

#### Configuracao

**Via variaveis de ambiente:**
```bash
export AI_CONTROL_CURSOR_ENABLED=true
export AI_CONTROL_CURSOR_TOKEN="cur_xxx"

# CSV legado (opcional)
export AI_CONTROL_CURSOR_CSV_ENABLED=false
export AI_CONTROL_CURSOR_CSV_PATH="./cursor-exports"
```

**Via application.yml:**
```yaml
ai-control:
  api:
    cursor:
      enabled: true
      token: ${AI_CONTROL_CURSOR_TOKEN}
      csv-import:
        enabled: false  # legado
        csv-path: ./cursor-exports
```

#### Features

- Busca automatica de membros do team via Admin API
- Metricas adicionais: `role`, `user_id`
- Retry automatico em caso de rate limit (429)
- Timeout configuravel (padrao: 30s)
- Importacao CSV como fallback legado
- API Documentation: https://cursor.com/docs/account/teams/admin-api

---

### 4. Google Workspace (Resolucao de Email)

Integracao opcional que resolve logins do GitHub em emails corporativos via Google Workspace Admin Directory API.

#### Configuracao

**Via variaveis de ambiente:**
```bash
export AI_CONTROL_WORKSPACE_ENABLED=true
export AI_CONTROL_WORKSPACE_CREDENTIALS="/path/to/service-account.json"
export AI_CONTROL_WORKSPACE_DOMAIN="empresa.com"
export AI_CONTROL_WORKSPACE_ADMIN_EMAIL="admin@empresa.com"
```

**Via application.yml:**
```yaml
ai-control:
  api:
    google-workspace:
      enabled: true
      credentials: ${AI_CONTROL_WORKSPACE_CREDENTIALS}
      domain: ${AI_CONTROL_WORKSPACE_DOMAIN}
      admin-email: ${AI_CONTROL_WORKSPACE_ADMIN_EMAIL}
      custom-schema: custom
      git-name-field: git_name
```

Para detalhes completos de configuracao, veja [docs/setup-google-workspace.md](docs/setup-google-workspace.md).

## CSV Unificado

O sistema gera um **CSV unificado** que consolida todos os usuarios em uma unica linha por email, independente de quantas ferramentas o usuario utiliza.

### Como funciona

1. **Coleta**: `UserCollectionService` busca usuarios de todas as integracoes habilitadas
2. **Unificacao**: `UserUnificationService` agrupa por email (case-insensitive) e mescla dados
3. **Exportacao**: `CsvExportService` gera CSVs individuais por ferramenta + CSV unificado

### Campos do CSV Unificado (`UnifiedUser`)

| Campo | Descricao |
|-------|-----------|
| `email` | Email do usuario (chave de unificacao) |
| `name` | Nome (prioridade: GitHub > Claude > Cursor) |
| `toolsCount` | Quantidade de ferramentas utilizadas |
| `usesClaude` | Usa Claude Code? |
| `usesCopilot` | Usa GitHub Copilot? |
| `usesCursor` | Usa Cursor? |
| `claudeLastActivity` | Ultima atividade no Claude |
| `copilotLastActivity` | Ultima atividade no Copilot |
| `cursorLastActivity` | Ultima atividade no Cursor |
| `claudeStatus` | Status no Claude |
| `copilotStatus` | Status no Copilot |
| `cursorStatus` | Status no Cursor |
| `emailType` | Tipo de email (corporate, personal, github.local) |

### Configuracao de exportacao

```yaml
ai-control:
  export:
    on-startup: true                    # Exportar ao iniciar
    output-directory: ./output          # Diretorio de saida
    consolidated: true                  # Gerar CSV unificado
```

## Uso Programatico

### Exemplo: Buscar usuarios do Claude Code

```java
@Autowired
private ClaudeApiClient claudeClient;

public void fetchClaudeUsers() {
    try {
        List<UserData> users = claudeClient.fetchUsers();
        users.forEach(user -> {
            System.out.println("Email: " + user.email());
            System.out.println("Name: " + user.name());
            System.out.println("Status: " + user.status());
            System.out.println("---");
        });
    } catch (ApiClientException e) {
        log.error("Erro ao buscar usuarios: {}", e.getMessage());
    }
}
```

### Exemplo: Buscar usuarios do Cursor

```java
@Autowired
private CursorApiClient cursorClient;

public void fetchCursorUsers() {
    try {
        List<UserData> users = cursorClient.fetchUsers();
        users.forEach(user -> {
            log.info("Email: {}", user.email());
            log.info("Name: {}", user.name());
            log.info("Role: {}", user.additionalMetrics().get("role"));
            log.info("---");
        });
    } catch (ApiClientException e) {
        log.error("Erro ao buscar usuarios: {}", e.getMessage());
    }
}
```

### Exemplo: Testar conexao

```java
@Autowired
private List<ToolApiClient> allClients;

public void testAllConnections() {
    allClients.forEach(client -> {
        ConnectionTestResult result = client.testConnection();

        if (result.isSuccess()) {
            System.out.println(client.getDisplayName() + ": " + result.getMessage());
        } else {
            System.out.println(client.getDisplayName() + ": " + result.getMessage());
        }
    });
}
```

## DTO Unificado (UserData)

Todas as integracoes retornam uma lista de `UserData` (record imutavel):

```java
public record UserData(
        String email,
        String name,
        String status,
        LocalDateTime lastActivityAt,
        Map<String, Object> additionalMetrics,
        String rawJson
) {}
```

### Metricas Adicionais por Ferramenta

**Claude Code:**
- `role`: Papel na organizacao (member, admin)
- `joined_at`: Data de entrada
- `member_id`: ID do membro

**GitHub Copilot:**
- `last_activity_editor`: Editor usado (vscode, jetbrains, etc.)
- `created_at`: Data de criacao do seat
- `updated_at`: Data de atualizacao
- `github_login`: Login do GitHub
- `github_id`: ID do GitHub

**Cursor:**
- `role`: Papel no team (member, owner, admin)
- `user_id`: ID do usuario no Cursor

## Tratamento de Erros

Todas as integracoes lancam `ApiClientException` em caso de erro:

```java
try {
    List<UserData> users = client.fetchUsers();
} catch (ApiClientException e) {
    log.error("Erro: {}", e.getMessage(), e);
}
```

### Erros Comuns

| Erro | Causa | Solucao |
|------|-------|---------|
| `Invalid API key` | Token invalido/expirado | Verificar e regenerar token |
| `Not Found` (Claude) | Usando API key regular ao inves de Admin key | Criar e usar Admin API key (sk-ant-admin-...) |
| `Unauthorized` (Claude) | Admin key sem permissoes | Verificar que voce tem role admin na organizacao |
| `Rate limit exceeded` | Muitas requisicoes | Aguardar reset (retry automatico) |
| `CSV file not found` | Arquivo nao existe | Verificar caminho do CSV |
| `Email is required` | CSV sem campo email | Adicionar coluna `email` |

## Testes

### Testes Unitarios

O projeto possui **107 testes unitarios** com cobertura >80%:

```bash
# Executar todos os testes unitarios
mvn test

# Executar testes de uma classe especifica
mvn test -Dtest=ClaudeApiClientTest

# Gerar relatorio de cobertura
mvn jacoco:report
# Relatorio em: target/site/jacoco/index.html
```

### Testes de Integracao (APIs Reais)

O projeto tambem inclui **testes de integracao** que fazem chamadas reais as APIs:

```bash
# Configurar credenciais (copie e edite .env.example)
cp .env.example .env
source .env

# Executar testes de integracao
mvn verify -P integration-tests

# Ou usar o script auxiliar
./run-integration-tests.sh
```

**Caracteristicas:**
- Fazem chamadas reais as APIs (sem mocks)
- Validam que a integracao funciona de verdade
- Ignoram testes automaticamente se credenciais nao configuradas
- Exibem output detalhado dos dados reais retornados
- Baixo risco (apenas operacoes de leitura)

Para mais informacoes, veja [INTEGRATION_TESTS.md](INTEGRATION_TESTS.md)

## Logs

O sistema utiliza SLF4J + Logback com suporte a MDC (contexto estruturado):

```
2026-01-22 14:30:00 - Fetching users from Claude Code API
2026-01-22 14:30:01 - Successfully fetched 15 users from Claude Code
```

### Configurar nivel de log

**application.yml:**
```yaml
logging:
  level:
    root: INFO
    com.bemobi.aicontrol: DEBUG
```

Em producao, o perfil `prod` gera logs em formato JSON via `logback-spring.xml`.

## Contribuindo

Este projeto segue Conventional Commits:

```
feat(auth): adiciona login (#123)
fix(cursor): corrige parsing de CSV (#456)
```

Mencione o numero da issue em commits e PRs para rastreamento automatico.

## Roadmap

- [ ] Docker compose para ambiente local
- [ ] Testes de integracao com WireMock
- [ ] Interface web para visualizacao de dados

## Licenca

Este projeto e de uso interno da Bemobi.

---

> *Generated by Claude Code*
