# Especificação de Integrações - AI User Control

Este documento detalha as especificações técnicas para integração com cada ferramenta de IA monitorada pelo sistema.

## Visão Geral

O sistema integra-se com três ferramentas de IA:
1. **Claude Code** (Anthropic API)
2. **GitHub Copilot** (GitHub API)
3. **Cursor** (API própria ou alternativas)

Cada integração é implementada como um componente Spring independente que implementa a interface `ToolApiClient`.

---

## Arquitetura de Integração

### Interface Base (ToolApiClient)

```java
public interface ToolApiClient {

    /**
     * Nome identificador da ferramenta
     * @return nome da ferramenta (claude-code, github-copilot, cursor)
     */
    String getToolName();

    /**
     * Nome para exibição
     * @return nome para exibição (Claude Code, GitHub Copilot, Cursor)
     */
    String getDisplayName();

    /**
     * Busca lista de usuários ativos na ferramenta
     * @return lista de dados de usuários
     * @throws ApiClientException em caso de erro na comunicação
     */
    List<UserData> fetchUsers() throws ApiClientException;

    /**
     * Testa conectividade com a API
     * @return resultado do teste de conexão
     */
    ConnectionTestResult testConnection();

    /**
     * Verifica se o cliente está habilitado
     * @return true se habilitado
     */
    boolean isEnabled();
}
```

### DTO Base (UserData)

```java
public class UserData {
    private String email;
    private String name;
    private String status;
    private LocalDateTime lastActivityAt;
    private Map<String, Object> additionalMetrics;
    private String rawJson; // Dados brutos para debug

    // Getters e Setters explícitos (sem Lombok)
}
```

---

## 1. Integração Claude Code (Anthropic API)

### 1.1 Informações Gerais

- **API Base URL:** `https://api.anthropic.com`
- **Documentação:** https://docs.anthropic.com/api
- **Autenticação:** API Key via header `X-API-Key`
- **Rate Limits:** 100 requests/minuto por organização

### 1.2 Obtenção de Credenciais

1. Acessar https://console.anthropic.com/
2. Criar/selecionar organização
3. Navegar para Settings → API Keys
4. Gerar novo API key com permissões de leitura
5. Copiar Organization ID (encontrado em Settings)

### 1.3 Configuração no Sistema

**application.yml:**
```yaml
ai-control:
  api:
    claude:
      enabled: true
      base-url: https://api.anthropic.com
      token: ${AI_CONTROL_CLAUDE_TOKEN}
      organization-id: ${AI_CONTROL_CLAUDE_ORG_ID}
      timeout: 30000
      retry-attempts: 3
```

**Variáveis de Ambiente:**
```bash
export AI_CONTROL_CLAUDE_TOKEN="sk-ant-api03-xxx"
export AI_CONTROL_CLAUDE_ORG_ID="org_xxx"
```

### 1.4 Endpoints Utilizados

#### GET /v1/organizations/{org_id}/members

**Request:**
```http
GET /v1/organizations/org_xxx/members HTTP/1.1
Host: api.anthropic.com
X-API-Key: sk-ant-api03-xxx
Content-Type: application/json
Anthropic-Version: 2023-06-01
```

**Response (200 OK):**
```json
{
  "object": "list",
  "data": [
    {
      "id": "user_xxx",
      "object": "organization_member",
      "email": "john.doe@example.com",
      "name": "John Doe",
      "role": "member",
      "status": "active",
      "joined_at": "2025-06-01T00:00:00Z",
      "last_active_at": "2026-01-22T10:30:00Z"
    },
    {
      "id": "user_yyy",
      "object": "organization_member",
      "email": "jane.smith@example.com",
      "name": "Jane Smith",
      "role": "admin",
      "status": "active",
      "joined_at": "2025-05-15T00:00:00Z",
      "last_active_at": "2026-01-21T18:45:00Z"
    }
  ],
  "has_more": false,
  "first_id": "user_xxx",
  "last_id": "user_yyy"
}
```

### 1.5 Tratamento de Erros

| Status Code | Significado | Ação |
|-------------|-------------|------|
| 200 | Sucesso | Processar dados |
| 401 | Token inválido ou expirado | Logar erro, falhar coleta |
| 403 | Sem permissão para acessar organização | Logar erro, falhar coleta |
| 404 | Organização não encontrada | Logar erro, falhar coleta |
| 429 | Rate limit excedido | Retry com backoff (header `Retry-After`) |
| 500/502/503 | Erro no servidor | Retry com backoff exponencial |

**Response de Erro (401):**
```json
{
  "error": {
    "type": "authentication_error",
    "message": "Invalid API key"
  }
}
```

### 1.6 Implementação Spring Boot

**Classe de Configuração:**
```java
@ConfigurationProperties(prefix = "ai-control.api.claude")
@Validated
public class ClaudeApiProperties {

    private boolean enabled = true;

    @NotBlank(message = "Claude API base URL is required")
    private String baseUrl;

    @NotBlank(message = "Claude API token is required")
    private String token;

    @NotBlank(message = "Claude organization ID is required")
    private String organizationId;

    private int timeout = 30000;

    private int retryAttempts = 3;

    // Getters e Setters explícitos

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
}
```

**Cliente API:**
```java
@Component
@ConditionalOnProperty(prefix = "ai-control.api.claude", name = "enabled", havingValue = "true")
public class ClaudeApiClient implements ToolApiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiClient.class);

    private final WebClient webClient;
    private final ClaudeApiProperties properties;

    public ClaudeApiClient(WebClient.Builder webClientBuilder,
                          ClaudeApiProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getBaseUrl())
            .defaultHeader("X-API-Key", properties.getToken())
            .defaultHeader("Anthropic-Version", "2023-06-01")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public String getToolName() {
        return "claude-code";
    }

    @Override
    public String getDisplayName() {
        return "Claude Code";
    }

    @Override
    public List<UserData> fetchUsers() throws ApiClientException {
        log.info("Fetching users from Claude Code API");

        try {
            ClaudeMembersResponse response = webClient.get()
                .uri("/v1/organizations/{orgId}/members", properties.getOrganizationId())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(ClaudeMembersResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null || response.getData() == null) {
                throw new ApiClientException("Empty response from Claude API");
            }

            log.info("Successfully fetched {} users from Claude Code", response.getData().size());

            return response.getData().stream()
                .map(this::mapToUserData)
                .collect(Collectors.toList());

        } catch (WebClientException e) {
            log.error("Error fetching users from Claude Code: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch users from Claude Code", e);
        }
    }

    @Override
    public ConnectionTestResult testConnection() {
        try {
            fetchUsers();
            return ConnectionTestResult.success(getToolName());
        } catch (Exception e) {
            return ConnectionTestResult.failure(getToolName(), e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private UserData mapToUserData(ClaudeMember member) {
        UserData userData = new UserData();
        userData.setEmail(member.getEmail().toLowerCase());
        userData.setName(member.getName());
        userData.setStatus(member.getStatus());
        userData.setLastActivityAt(member.getLastActiveAt());

        // Adicionar métricas adicionais
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("role", member.getRole());
        metrics.put("joined_at", member.getJoinedAt());
        userData.setAdditionalMetrics(metrics);

        return userData;
    }

    private Mono<? extends Throwable> handle4xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("4xx error from Claude API: {} - {}", response.statusCode(), body);
                return Mono.error(new ApiClientException("Client error: " + body));
            });
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("5xx error from Claude API: {} - {}", response.statusCode(), body);
                return Mono.error(new ApiClientException("Server error: " + body));
            });
    }
}
```

**DTOs:**
```java
public class ClaudeMembersResponse {
    private String object;
    private List<ClaudeMember> data;
    private boolean hasMore;
    private String firstId;
    private String lastId;

    // Getters e Setters
}

public class ClaudeMember {
    private String id;
    private String object;
    private String email;
    private String name;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;

    // Getters e Setters
}
```

### 1.7 Testes Unitários

```java
@ExtendWith(MockitoExtension.class)
class ClaudeApiClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ClaudeApiProperties properties;

    private ClaudeApiClient client;

    @BeforeEach
    void setUp() {
        when(properties.getBaseUrl()).thenReturn("https://api.anthropic.com");
        when(properties.getToken()).thenReturn("test-token");
        when(properties.getOrganizationId()).thenReturn("org_test");
        when(properties.getTimeout()).thenReturn(30000);
        when(properties.getRetryAttempts()).thenReturn(3);

        // Setup WebClient mock...

        client = new ClaudeApiClient(webClientBuilder, properties);
    }

    @Test
    void testFetchUsers_Success() {
        // Test implementation
    }

    @Test
    void testFetchUsers_Unauthorized() {
        // Test implementation
    }

    @Test
    void testFetchUsers_RateLimitWithRetry() {
        // Test implementation
    }
}
```

---

## 2. Integração GitHub Copilot

### 2.1 Informações Gerais

- **API Base URL:** `https://api.github.com`
- **Documentação:** https://docs.github.com/en/rest/copilot
- **Autenticação:** Personal Access Token (PAT) ou GitHub App
- **Rate Limits:** 5000 requests/hora (autenticado)
- **Scopes Necessários:** `read:org`, `manage_billing:copilot`

### 2.2 Obtenção de Credenciais

1. Acessar https://github.com/settings/tokens
2. Clicar em "Generate new token (classic)"
3. Selecionar scopes:
   - `read:org` - Leitura de informações da organização
   - `manage_billing:copilot` - Acesso aos dados de billing do Copilot
4. Gerar token e copiar
5. Identificar nome da organização GitHub

### 2.3 Configuração no Sistema

**application.yml:**
```yaml
ai-control:
  api:
    github:
      enabled: true
      base-url: https://api.github.com
      token: ${AI_CONTROL_GITHUB_TOKEN}
      organization: ${AI_CONTROL_GITHUB_ORG}
      timeout: 30000
      retry-attempts: 3
```

**Variáveis de Ambiente:**
```bash
export AI_CONTROL_GITHUB_TOKEN="ghp_xxx"
export AI_CONTROL_GITHUB_ORG="my-organization"
```

### 2.4 Endpoints Utilizados

#### GET /orgs/{org}/copilot/billing/seats

**Request:**
```http
GET /orgs/my-organization/copilot/billing/seats HTTP/1.1
Host: api.github.com
Authorization: Bearer ghp_xxx
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
```

**Response (200 OK):**
```json
{
  "total_seats": 50,
  "seats": [
    {
      "created_at": "2025-05-01T00:00:00Z",
      "updated_at": "2026-01-22T10:30:00Z",
      "pending_cancellation_date": null,
      "last_activity_at": "2026-01-20T14:22:00Z",
      "last_activity_editor": "vscode",
      "assignee": {
        "login": "johndoe",
        "id": 12345,
        "node_id": "MDQ6VXNlcjEyMzQ1",
        "avatar_url": "https://avatars.githubusercontent.com/u/12345?v=4",
        "type": "User",
        "site_admin": false,
        "name": "John Doe",
        "email": "john.doe@example.com",
        "twitter_username": null,
        "blog": null
      },
      "assigning_team": null
    }
  ]
}
```

### 2.5 Tratamento de Erros

| Status Code | Significado | Ação |
|-------------|-------------|------|
| 200 | Sucesso | Processar dados |
| 401 | Token inválido ou expirado | Logar erro, falhar coleta |
| 403 | Sem permissão ou rate limit | Verificar `X-RateLimit-Remaining` |
| 404 | Organização não encontrada ou sem Copilot | Logar aviso, retornar lista vazia |
| 500/502/503 | Erro no servidor | Retry com backoff |

**Headers de Rate Limit:**
```
X-RateLimit-Limit: 5000
X-RateLimit-Remaining: 4999
X-RateLimit-Reset: 1674763261
X-RateLimit-Used: 1
X-RateLimit-Resource: core
```

### 2.6 Implementação Spring Boot

**Classe de Configuração:**
```java
@ConfigurationProperties(prefix = "ai-control.api.github")
@Validated
public class GitHubApiProperties {

    private boolean enabled = true;

    @NotBlank(message = "GitHub API base URL is required")
    private String baseUrl;

    @NotBlank(message = "GitHub API token is required")
    private String token;

    @NotBlank(message = "GitHub organization is required")
    private String organization;

    private int timeout = 30000;

    private int retryAttempts = 3;

    // Getters e Setters explícitos

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
}
```

**Cliente API:**
```java
@Component
@ConditionalOnProperty(prefix = "ai-control.api.github", name = "enabled", havingValue = "true")
public class GitHubCopilotApiClient implements ToolApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubCopilotApiClient.class);

    private final WebClient webClient;
    private final GitHubApiProperties properties;

    public GitHubCopilotApiClient(WebClient.Builder webClientBuilder,
                                 GitHubApiProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + properties.getToken())
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
    }

    @Override
    public String getToolName() {
        return "github-copilot";
    }

    @Override
    public String getDisplayName() {
        return "GitHub Copilot";
    }

    @Override
    public List<UserData> fetchUsers() throws ApiClientException {
        log.info("Fetching users from GitHub Copilot API");

        try {
            GitHubCopilotSeatsResponse response = webClient.get()
                .uri("/orgs/{org}/copilot/billing/seats", properties.getOrganization())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xxError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
                .bodyToMono(GitHubCopilotSeatsResponse.class)
                .retryWhen(Retry.backoff(properties.getRetryAttempts(), Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof WebClientResponseException &&
                           ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()))
                .block(Duration.ofMillis(properties.getTimeout()));

            if (response == null || response.getSeats() == null) {
                log.warn("Empty response from GitHub Copilot API");
                return Collections.emptyList();
            }

            log.info("Successfully fetched {} Copilot seats from GitHub", response.getTotalSeats());

            return response.getSeats().stream()
                .map(this::mapToUserData)
                .collect(Collectors.toList());

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Organization '{}' not found or doesn't have Copilot", properties.getOrganization());
            return Collections.emptyList();
        } catch (WebClientException e) {
            log.error("Error fetching users from GitHub Copilot: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch users from GitHub Copilot", e);
        }
    }

    @Override
    public ConnectionTestResult testConnection() {
        try {
            fetchUsers();
            return ConnectionTestResult.success(getToolName());
        } catch (Exception e) {
            return ConnectionTestResult.failure(getToolName(), e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private UserData mapToUserData(GitHubCopilotSeat seat) {
        UserData userData = new UserData();

        GitHubUser assignee = seat.getAssignee();
        if (assignee != null) {
            // Email pode não estar disponível via API pública
            String email = assignee.getEmail();
            if (email == null || email.isEmpty()) {
                // Usar login como fallback
                email = assignee.getLogin() + "@github.local";
                log.warn("Email not available for user {}, using generated email", assignee.getLogin());
            }

            userData.setEmail(email.toLowerCase());
            userData.setName(assignee.getName() != null ? assignee.getName() : assignee.getLogin());
        }

        userData.setStatus("active"); // Todos os seats são ativos
        userData.setLastActivityAt(seat.getLastActivityAt());

        // Métricas adicionais
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("last_activity_editor", seat.getLastActivityEditor());
        metrics.put("created_at", seat.getCreatedAt());
        metrics.put("updated_at", seat.getUpdatedAt());
        if (assignee != null) {
            metrics.put("github_login", assignee.getLogin());
            metrics.put("github_id", assignee.getId());
        }
        userData.setAdditionalMetrics(metrics);

        return userData;
    }

    private Mono<? extends Throwable> handle4xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("4xx error from GitHub API: {} - {}", response.statusCode(), body);

                // Check rate limit headers
                response.headers().header("X-RateLimit-Remaining").stream().findFirst()
                    .ifPresent(remaining -> {
                        if ("0".equals(remaining)) {
                            String reset = response.headers().header("X-RateLimit-Reset")
                                .stream().findFirst().orElse("unknown");
                            log.error("GitHub API rate limit exceeded. Reset at: {}", reset);
                        }
                    });

                return Mono.error(new ApiClientException("Client error: " + body));
            });
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("5xx error from GitHub API: {} - {}", response.statusCode(), body);
                return Mono.error(new ApiClientException("Server error: " + body));
            });
    }
}
```

**DTOs:**
```java
public class GitHubCopilotSeatsResponse {
    private int totalSeats;
    private List<GitHubCopilotSeat> seats;

    // Getters e Setters

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public List<GitHubCopilotSeat> getSeats() {
        return seats;
    }

    public void setSeats(List<GitHubCopilotSeat> seats) {
        this.seats = seats;
    }
}

public class GitHubCopilotSeat {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime pendingCancellationDate;
    private LocalDateTime lastActivityAt;
    private String lastActivityEditor;
    private GitHubUser assignee;
    private GitHubTeam assigningTeam;

    // Getters e Setters

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getPendingCancellationDate() {
        return pendingCancellationDate;
    }

    public void setPendingCancellationDate(LocalDateTime pendingCancellationDate) {
        this.pendingCancellationDate = pendingCancellationDate;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public String getLastActivityEditor() {
        return lastActivityEditor;
    }

    public void setLastActivityEditor(String lastActivityEditor) {
        this.lastActivityEditor = lastActivityEditor;
    }

    public GitHubUser getAssignee() {
        return assignee;
    }

    public void setAssignee(GitHubUser assignee) {
        this.assignee = assignee;
    }

    public GitHubTeam getAssigningTeam() {
        return assigningTeam;
    }

    public void setAssigningTeam(GitHubTeam assigningTeam) {
        this.assigningTeam = assigningTeam;
    }
}

public class GitHubUser {
    private String login;
    private Long id;
    private String nodeId;
    private String avatarUrl;
    private String type;
    private boolean siteAdmin;
    private String name;
    private String email;
    private String twitterUsername;
    private String blog;

    // Getters e Setters completos
}

public class GitHubTeam {
    private String name;
    private String slug;

    // Getters e Setters
}
```

### 2.7 Paginação (se necessário)

Se a organização tiver mais de 100 seats, será necessário implementar paginação:

```java
private List<UserData> fetchAllUsers() {
    List<UserData> allUsers = new ArrayList<>();
    int page = 1;
    int perPage = 100;
    boolean hasMore = true;

    while (hasMore) {
        GitHubCopilotSeatsResponse response = fetchPage(page, perPage);
        allUsers.addAll(mapToUserData(response));

        // GitHub usa Link header para paginação
        // Ou verificar se response.getSeats().size() < perPage
        hasMore = response.getSeats().size() == perPage;
        page++;
    }

    return allUsers;
}
```

---

## 3. Integração Cursor

### 3.1 Informações Gerais

- **Status da API:** Cursor não possui API pública oficial documentada (até Janeiro 2026)
- **Alternativas de Integração:**
  1. API privada (se disponível para clientes enterprise)
  2. Dashboard web scraping (frágil, não recomendado)
  3. Export manual de CSV
  4. Integração via SSO logs (se usar SSO corporativo)

### 3.2 Abordagem Recomendada: Import Manual CSV

Dado que Cursor não tem API pública, a abordagem mais viável é:

1. Administrador exporta lista de usuários do dashboard Cursor manualmente
2. Sistema importa arquivo CSV via comando CLI
3. Dados são normalizados e armazenados junto com outras ferramentas

### 3.3 Configuração no Sistema

**application.yml:**
```yaml
ai-control:
  api:
    cursor:
      enabled: true
      import-mode: csv  # csv, api (futuro)
      csv-path: ${AI_CONTROL_CURSOR_CSV_PATH:${user.home}/.ai-control/imports/cursor}
```

### 3.4 Formato CSV Esperado

**cursor-users.csv:**
```csv
email,name,status,last_active,joined_at
john.doe@example.com,John Doe,active,2026-01-21,2025-06-01
jane.smith@example.com,Jane Smith,active,2026-01-20,2025-05-15
bob.jones@example.com,Bob Jones,inactive,2025-12-15,2025-04-10
```

**Campos:**
- `email` (obrigatório): Email do usuário
- `name` (obrigatório): Nome completo
- `status` (opcional): active/inactive (default: active)
- `last_active` (opcional): Data da última atividade (formato: YYYY-MM-DD)
- `joined_at` (opcional): Data de entrada (formato: YYYY-MM-DD)

### 3.5 Comando CLI de Import

```bash
# Import de arquivo CSV específico
ai-control import cursor --file /path/to/cursor-users.csv

# Import do diretório padrão (busca arquivo mais recente)
ai-control import cursor

# Import com dry-run (validação sem persistir)
ai-control import cursor --file cursor.csv --dry-run
```

### 3.6 Implementação Spring Boot

**Cliente CSV:**
```java
@Component
@ConditionalOnProperty(prefix = "ai-control.api.cursor", name = "enabled", havingValue = "true")
public class CursorCsvClient implements ToolApiClient {

    private static final Logger log = LoggerFactory.getLogger(CursorCsvClient.class);

    private final CursorApiProperties properties;

    public CursorCsvClient(CursorApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getToolName() {
        return "cursor";
    }

    @Override
    public String getDisplayName() {
        return "Cursor";
    }

    @Override
    public List<UserData> fetchUsers() throws ApiClientException {
        throw new UnsupportedOperationException(
            "Cursor integration uses CSV import. Use 'import cursor' command instead."
        );
    }

    /**
     * Import users from CSV file
     */
    public List<UserData> importFromCsv(String filePath) throws ApiClientException {
        log.info("Importing Cursor users from CSV: {}", filePath);

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new ApiClientException("CSV file not found: " + filePath);
        }

        List<UserData> users = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim());

            for (CSVRecord record : csvParser) {
                UserData userData = parseCsvRecord(record);
                users.add(userData);
            }

            log.info("Successfully imported {} users from Cursor CSV", users.size());
            return users;

        } catch (IOException e) {
            log.error("Error reading CSV file: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to read CSV file", e);
        } catch (Exception e) {
            log.error("Error parsing CSV: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to parse CSV file", e);
        }
    }

    /**
     * Find most recent CSV file in default directory
     */
    public String findLatestCsvFile() throws ApiClientException {
        Path csvDir = Paths.get(properties.getCsvPath());

        if (!Files.exists(csvDir)) {
            throw new ApiClientException("CSV import directory not found: " + csvDir);
        }

        try (Stream<Path> files = Files.list(csvDir)) {
            return files
                .filter(path -> path.toString().endsWith(".csv"))
                .max(Comparator.comparingLong(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (IOException e) {
                        return 0;
                    }
                }))
                .map(Path::toString)
                .orElseThrow(() -> new ApiClientException("No CSV files found in " + csvDir));

        } catch (IOException e) {
            throw new ApiClientException("Error searching for CSV files", e);
        }
    }

    @Override
    public ConnectionTestResult testConnection() {
        try {
            Path csvDir = Paths.get(properties.getCsvPath());
            if (Files.exists(csvDir) && Files.isDirectory(csvDir)) {
                return ConnectionTestResult.success(getToolName(),
                    "CSV import directory is accessible");
            } else {
                return ConnectionTestResult.failure(getToolName(),
                    "CSV import directory not found or not accessible");
            }
        } catch (Exception e) {
            return ConnectionTestResult.failure(getToolName(), e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private UserData parseCsvRecord(CSVRecord record) {
        UserData userData = new UserData();

        // Email (obrigatório)
        String email = record.get("email");
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required on line " + record.getRecordNumber());
        }
        userData.setEmail(email.toLowerCase().trim());

        // Name (obrigatório)
        String name = record.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required on line " + record.getRecordNumber());
        }
        userData.setName(name.trim());

        // Status (opcional, default: active)
        String status = record.isMapped("status") ? record.get("status") : "active";
        userData.setStatus(status != null && !status.isEmpty() ? status : "active");

        // Last Active (opcional)
        if (record.isMapped("last_active")) {
            String lastActive = record.get("last_active");
            if (lastActive != null && !lastActive.isEmpty()) {
                userData.setLastActivityAt(LocalDate.parse(lastActive).atStartOfDay());
            }
        }

        // Additional metrics
        Map<String, Object> metrics = new HashMap<>();
        if (record.isMapped("joined_at")) {
            String joinedAt = record.get("joined_at");
            if (joinedAt != null && !joinedAt.isEmpty()) {
                metrics.put("joined_at", LocalDate.parse(joinedAt).atStartOfDay());
            }
        }
        userData.setAdditionalMetrics(metrics);

        return userData;
    }
}
```

**Comando Spring Shell para Import:**
```java
@ShellComponent
@ShellCommandGroup("Data Import")
public class ImportCommands {

    private final CursorCsvClient cursorClient;
    private final ImportService importService;

    public ImportCommands(CursorCsvClient cursorClient, ImportService importService) {
        this.cursorClient = cursorClient;
        this.importService = importService;
    }

    @ShellMethod(value = "Import Cursor users from CSV file", key = "import cursor")
    public String importCursor(
        @ShellOption(defaultValue = "", help = "Path to CSV file") String file,
        @ShellOption(defaultValue = "false", help = "Dry run mode") boolean dryRun
    ) {
        try {
            String csvFile = file.isEmpty() ? cursorClient.findLatestCsvFile() : file;

            List<UserData> users = cursorClient.importFromCsv(csvFile);

            if (dryRun) {
                return String.format("Dry run: Would import %d users from %s",
                    users.size(), csvFile);
            }

            ImportResult result = importService.importUsers("cursor", users);

            return String.format("Successfully imported %d users from Cursor\n" +
                "New users: %d\n" +
                "Updated users: %d\n" +
                "Errors: %d",
                result.getTotal(),
                result.getNewUsers(),
                result.getUpdatedUsers(),
                result.getErrors());

        } catch (Exception e) {
            return "Error importing Cursor users: " + e.getMessage();
        }
    }
}
```

### 3.7 Futuro: API Integration (se disponível)

Se Cursor lançar API pública no futuro, a estrutura seria similar às outras integrações:

```java
@Component
@ConditionalOnProperty(
    prefix = "ai-control.api.cursor",
    name = "import-mode",
    havingValue = "api"
)
public class CursorApiClient implements ToolApiClient {
    // Implementação similar a ClaudeApiClient e GitHubCopilotApiClient
}
```

---

## 4. Diagrama de Sequência - Fluxo de Coleta

```
┌─────────┐        ┌──────────────┐      ┌─────────────┐      ┌──────────┐      ┌──────────┐
│   CLI   │        │CollectionSvc │      │ToolApiClient│      │ API REST │      │   DB     │
└────┬────┘        └──────┬───────┘      └──────┬──────┘      └────┬─────┘      └────┬─────┘
     │                    │                      │                   │                 │
     │ collect --all      │                      │                   │                 │
     ├───────────────────>│                      │                   │                 │
     │                    │                      │                   │                 │
     │                    │ createCollectionRun()│                   │                 │
     │                    ├──────────────────────┼───────────────────┼────────────────>│
     │                    │                      │                   │                 │
     │                    │ foreach client       │                   │                 │
     │                    │                      │                   │                 │
     │                    │ fetchUsers()         │                   │                 │
     │                    ├─────────────────────>│                   │                 │
     │                    │                      │                   │                 │
     │                    │                      │ GET /api/users    │                 │
     │                    │                      ├──────────────────>│                 │
     │                    │                      │                   │                 │
     │                    │                      │ 200 OK + data     │                 │
     │                    │                      │<──────────────────┤                 │
     │                    │                      │                   │                 │
     │                    │ List<UserData>       │                   │                 │
     │                    │<─────────────────────┤                   │                 │
     │                    │                      │                   │                 │
     │                    │ normalizeAndStore()  │                   │                 │
     │                    ├──────────────────────┼───────────────────┼────────────────>│
     │                    │                      │                   │                 │
     │                    │ completeRun()        │                   │                 │
     │                    ├──────────────────────┼───────────────────┼────────────────>│
     │                    │                      │                   │                 │
     │   Result           │                      │                   │                 │
     │<───────────────────┤                      │                   │                 │
     │                    │                      │                   │                 │
```

---

## 5. Tratamento de Casos Especiais

### 5.1 Email Não Disponível (GitHub)

Quando o email não está disponível via API do GitHub:

```java
String email = assignee.getEmail();
if (email == null || email.isEmpty()) {
    // Estratégia 1: Usar login + domínio fake
    email = assignee.getLogin() + "@github.local";

    // Estratégia 2: Tentar buscar via API adicional
    // GET /users/{username} pode retornar email público

    // Marcar como "email não verificado" para revisão manual
    userData.setEmailVerified(false);
}
```

### 5.2 Usuários Duplicados Entre Ferramentas

Identificação e merge de usuários com mesmo email:

```java
@Service
public class UserMergeService {

    public User findOrCreateUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseGet(() -> createNewUser(email));
    }

    public void updateUserFromSnapshot(User user, UserToolSnapshot snapshot) {
        // Atualizar last_seen_at se mais recente
        if (snapshot.getLastActivityAt() != null &&
            (user.getLastSeenAt() == null ||
             snapshot.getLastActivityAt().isAfter(user.getLastSeenAt()))) {
            user.setLastSeenAt(snapshot.getLastActivityAt());
        }

        // Aplicar prioridade de nome (RN-002)
        updateNameByPriority(user, snapshot);
    }
}
```

### 5.3 Timeout e Retry

Configuração de retry com backoff exponencial:

```java
.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
    .maxBackoff(Duration.ofSeconds(10))
    .filter(throwable ->
        throwable instanceof WebClientResponseException.TooManyRequests ||
        throwable instanceof WebClientResponseException.ServiceUnavailable)
    .doBeforeRetry(signal ->
        log.warn("Retrying request. Attempt: {}", signal.totalRetries() + 1))
)
```

### 5.4 Coleta Parcial com Falhas

Se uma API falha, as outras devem continuar:

```java
public CollectionResult collectAll() {
    CollectionRun run = createRun();
    List<String> successfulTools = new ArrayList<>();
    List<String> failedTools = new ArrayList<>();

    for (ToolApiClient client : clients) {
        try {
            collectFromTool(client, run);
            successfulTools.add(client.getToolName());
        } catch (Exception e) {
            log.error("Failed to collect from {}: {}",
                client.getToolName(), e.getMessage());
            failedTools.add(client.getToolName());
        }
    }

    run.setToolsCollected(String.join(",", successfulTools));
    run.setToolsFailed(String.join(",", failedTools));
    run.setStatus(failedTools.isEmpty() ? "completed" : "partial_failure");

    return buildResult(run);
}
```

---

## 6. Testes de Integração

### 6.1 Mock de APIs Externas (WireMock)

```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class ClaudeApiClientIntegrationTest {

    @Autowired
    private ClaudeApiClient client;

    @Test
    void testFetchUsers_Success() {
        stubFor(get(urlEqualTo("/v1/organizations/org_test/members"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "object": "list",
                      "data": [
                        {
                          "email": "test@example.com",
                          "name": "Test User",
                          "status": "active",
                          "last_active_at": "2026-01-22T10:00:00Z"
                        }
                      ]
                    }
                    """)));

        List<UserData> users = client.fetchUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testFetchUsers_RateLimit() {
        stubFor(get(urlEqualTo("/v1/organizations/org_test/members"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Retry-After", "2")
                .withBody("{\"error\": {\"type\": \"rate_limit_error\"}}")));

        assertThatThrownBy(() -> client.fetchUsers())
            .isInstanceOf(ApiClientException.class);
    }
}
```

---

## 7. Checklist de Implementação

### Claude Code Integration
- [ ] Criar `ClaudeApiProperties`
- [ ] Implementar `ClaudeApiClient`
- [ ] Criar DTOs (`ClaudeMembersResponse`, `ClaudeMember`)
- [ ] Adicionar testes unitários
- [ ] Adicionar testes de integração (WireMock)
- [ ] Documentar obtenção de credenciais
- [ ] Testar com API real

### GitHub Copilot Integration
- [ ] Criar `GitHubApiProperties`
- [ ] Implementar `GitHubCopilotApiClient`
- [ ] Criar DTOs (`GitHubCopilotSeatsResponse`, etc.)
- [ ] Implementar tratamento de rate limit
- [ ] Implementar paginação (se necessário)
- [ ] Adicionar testes unitários
- [ ] Adicionar testes de integração
- [ ] Testar com API real
- [ ] Tratar caso de email não disponível

### Cursor Integration
- [ ] Criar `CursorApiProperties`
- [ ] Implementar `CursorCsvClient`
- [ ] Implementar comando `import cursor`
- [ ] Criar `ImportService`
- [ ] Validação de CSV
- [ ] Adicionar testes com CSV de exemplo
- [ ] Documentar formato CSV
- [ ] Criar template CSV de exemplo

### Geral
- [ ] Interface `ToolApiClient`
- [ ] DTO base `UserData`
- [ ] Exception `ApiClientException`
- [ ] `ConnectionTestResult`
- [ ] Configuração centralizada
- [ ] Documentação completa

---

> *Generated by Claude Code*
