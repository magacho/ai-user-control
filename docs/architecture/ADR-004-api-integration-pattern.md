# ADR-004: Padrão de Integração com APIs Externas

**Status:** Accepted
**Date:** 2026-01-22
**Deciders:** @Architect, @Dev, @DevOps

## Context

O sistema precisa integrar com múltiplas APIs externas (Anthropic, GitHub, Cursor) para coletar dados de usuários. Cada API tem suas particularidades: autenticação, rate limits, formatos de resposta e tratamento de erros.

### Desafios de Integração
- **Múltiplas APIs:** 3 ferramentas, cada uma com sua API
- **Rate Limiting:** Cada API tem limites diferentes
- **Resiliência:** Falha em uma API não deve afetar outras
- **Retry Logic:** Necessidade de retentar em caso de erros transitórios
- **Testabilidade:** Testes sem dependência de APIs reais
- **Extensibilidade:** Facilitar adição de novas ferramentas

## Decision

**Adotamos o padrão Strategy com interface comum `ToolApiClient` e implementações específicas para cada ferramenta.**

### Core Interface

```java
public interface ToolApiClient {

    /**
     * Nome identificador da ferramenta (claude-code, github-copilot, cursor)
     */
    String getToolName();

    /**
     * Nome para exibição (Claude Code, GitHub Copilot, Cursor)
     */
    String getDisplayName();

    /**
     * Busca lista de usuários ativos na ferramenta
     * @throws ApiClientException em caso de erro na comunicação
     */
    List<UserData> fetchUsers() throws ApiClientException;

    /**
     * Testa conectividade com a API
     */
    ConnectionTestResult testConnection();

    /**
     * Verifica se o cliente está habilitado
     */
    boolean isEnabled();
}
```

### Implementation Pattern

```java
@Component
@ConditionalOnProperty(prefix = "ai-control.api.claude", name = "enabled", havingValue = "true")
public class ClaudeApiClient implements ToolApiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiClient.class);

    private final WebClient webClient;
    private final ClaudeApiProperties properties;

    public ClaudeApiClient(WebClient.Builder webClientBuilder, ClaudeApiProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getBaseUrl())
            .defaultHeader("X-API-Key", properties.getToken())
            .defaultHeader("Anthropic-Version", "2023-06-01")
            .build();
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

            return mapToUserData(response);

        } catch (WebClientException e) {
            log.error("Error fetching users from Claude Code: {}", e.getMessage(), e);
            throw new ApiClientException("Failed to fetch users from Claude Code", e);
        }
    }

    @Override
    public String getToolName() {
        return "claude-code";
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }
}
```

## Architectural Diagram

```mermaid
graph TB
    subgraph "Service Layer"
        CS[CollectionService]
    end

    subgraph "Strategy Interface"
        TAC[ToolApiClient Interface]
    end

    subgraph "Concrete Strategies"
        CAC[ClaudeApiClient]
        GAC[GitHubCopilotApiClient]
        CUC[CursorCsvClient]
    end

    subgraph "External Systems"
        ANT[Anthropic API]
        GH[GitHub API]
        CSV[CSV Files]
    end

    CS -->|uses| TAC
    TAC <|.. CAC
    TAC <|.. GAC
    TAC <|.. CUC

    CAC --> ANT
    GAC --> GH
    CUC --> CSV

    style TAC fill:#e1f5ff
    style CAC fill:#c8e6c9
    style GAC fill:#c8e6c9
    style CUC fill:#c8e6c9
```

## Key Patterns and Practices

### 1. Retry with Exponential Backoff

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

**Benefícios:**
- Resiliência contra erros transitórios
- Respeita rate limits (429 Too Many Requests)
- Backoff evita sobrecarregar APIs
- Configurável por ferramenta

### 2. Conditional Bean Loading

```java
@Component
@ConditionalOnProperty(
    prefix = "ai-control.api.claude",
    name = "enabled",
    havingValue = "true"
)
public class ClaudeApiClient implements ToolApiClient {
    // Implementation
}
```

**Benefícios:**
- Cliente só é criado se habilitado
- Permite desabilitar ferramentas via configuração
- Facilita testes (desabilitar APIs reais)

### 3. Typed Configuration Properties

```java
@ConfigurationProperties(prefix = "ai-control.api.claude")
@Validated
public class ClaudeApiProperties {

    private boolean enabled = true;

    @NotBlank(message = "Claude API base URL is required")
    private String baseUrl;

    @NotBlank(message = "Claude API token is required")
    private String token;

    private int timeout = 30000;
    private int retryAttempts = 3;

    // Getters e Setters explícitos
}
```

**Benefícios:**
- Type-safe configuration
- Validação com Bean Validation
- IDE autocomplete
- Documentação implícita via código

### 4. Error Handling Strategy

```java
private Mono<? extends Throwable> handle4xxError(ClientResponse response) {
    return response.bodyToMono(String.class)
        .flatMap(body -> {
            log.error("4xx error from API: {} - {}", response.statusCode(), body);

            // Extrair detalhes do erro
            ErrorDetails details = parseErrorResponse(body);

            // Tratamento específico por status
            return switch (response.statusCode().value()) {
                case 401 -> Mono.error(new AuthenticationException("Invalid API key"));
                case 403 -> Mono.error(new AuthorizationException("Forbidden"));
                case 429 -> Mono.error(new RateLimitException("Rate limit exceeded"));
                default -> Mono.error(new ApiClientException("Client error: " + body));
            };
        });
}
```

**Benefícios:**
- Erros tipados e específicos
- Logs detalhados para troubleshooting
- Possibilidade de retry seletivo
- Informações úteis para o usuário

### 5. Data Normalization

```java
private UserData mapToUserData(ClaudeMember member) {
    UserData userData = new UserData();

    // Email normalizado (lowercase)
    userData.setEmail(member.getEmail().toLowerCase().trim());

    // Nome
    userData.setName(member.getName());

    // Status
    userData.setStatus(normalizeStatus(member.getStatus()));

    // Timestamps
    userData.setLastActivityAt(member.getLastActiveAt());

    // Métricas adicionais (específicas da ferramenta)
    Map<String, Object> metrics = new HashMap<>();
    metrics.put("role", member.getRole());
    metrics.put("joined_at", member.getJoinedAt());
    userData.setAdditionalMetrics(metrics);

    return userData;
}
```

**Benefícios:**
- Dados consistentes independente da fonte
- Email normalizado (RN-001)
- Métricas específicas preservadas
- Facilita unificação posterior

## Consequences

### Positive
- ✅ **Extensibilidade:** Adicionar nova ferramenta = implementar interface
- ✅ **Testabilidade:** Fácil mockar `ToolApiClient` nos testes
- ✅ **Resiliência:** Retry automático e tratamento de erros robusto
- ✅ **Isolamento:** Falha em uma API não afeta outras
- ✅ **Manutenibilidade:** Lógica de integração isolada
- ✅ **Configurabilidade:** Cada API tem sua configuração
- ✅ **Desacoplamento:** Service layer não conhece detalhes de APIs
- ✅ **Type Safety:** DTOs tipados para cada API
- ✅ **Observabilidade:** Logs estruturados em todas as operações

### Negative
- ⚠️ **Boilerplate:** Cada API requer client, properties, DTOs
- ⚠️ **Complexity:** Mais código que chamada direta
- ⚠️ **Testing Overhead:** Precisa mockar múltiplas camadas

## Service Integration Pattern

```java
@Service
public class CollectionService {

    private final List<ToolApiClient> apiClients;  // Spring injeta todos

    public CollectionService(List<ToolApiClient> apiClients) {
        this.apiClients = apiClients;
    }

    @Transactional
    public CollectionResult collectAll(boolean silent, boolean verbose, String filter) {
        CollectionRun run = createRun();

        for (ToolApiClient client : apiClients) {
            if (!client.isEnabled()) {
                log.debug("Skipping disabled tool: {}", client.getToolName());
                continue;
            }

            if (shouldCollect(client, filter)) {
                try {
                    List<UserData> users = client.fetchUsers();
                    processUsers(users, client.getToolName(), run);
                    recordSuccess(client.getToolName());

                } catch (ApiClientException e) {
                    log.error("Failed to collect from {}: {}",
                        client.getToolName(), e.getMessage());
                    recordFailure(client.getToolName(), e);
                }
            }
        }

        return finalizeRun(run);
    }
}
```

**Benefícios:**
- Service desconhece quantos/quais clientes existem
- Spring injeta automaticamente todos os `ToolApiClient` beans
- Adicionar nova ferramenta = zero mudanças no service
- Tratamento uniforme de falhas

## Testing Strategy

### Unit Tests (Mock WebClient)

```java
@ExtendWith(MockitoExtension.class)
class ClaudeApiClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ClaudeApiProperties properties;

    private ClaudeApiClient client;

    @Test
    void testFetchUsers_Success() {
        // Mock WebClient responses
        // Assert correct mapping
    }

    @Test
    void testFetchUsers_RateLimitWithRetry() {
        // Mock 429 response
        // Verify retry behavior
    }
}
```

### Integration Tests (WireMock)

```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class ClaudeApiClientIntegrationTest {

    @Autowired
    private ClaudeApiClient client;

    @Test
    void testFetchUsers_RealFlow() {
        stubFor(get(urlEqualTo("/v1/organizations/org_test/members"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(jsonResponse)));

        List<UserData> users = client.fetchUsers();

        assertThat(users).hasSize(2);
    }
}
```

## Rate Limiting Strategy

### Configuração por Ferramenta

**Claude Code:** 100 req/min
```java
retryAttempts: 3
backoffInitial: 1s
backoffMax: 10s
```

**GitHub Copilot:** 5000 req/hora
```java
retryAttempts: 3
backoffInitial: 1s
backoffMax: 30s
checkRateLimitHeaders: true
```

### Headers Monitoring

```java
private void checkRateLimitHeaders(ClientResponse response) {
    String remaining = response.headers()
        .header("X-RateLimit-Remaining")
        .stream().findFirst().orElse("unknown");

    if ("0".equals(remaining)) {
        String reset = response.headers()
            .header("X-RateLimit-Reset")
            .stream().findFirst().orElse("unknown");

        log.warn("Rate limit reached. Reset at: {}", reset);
    }

    if (!"unknown".equals(remaining) && Integer.parseInt(remaining) < 100) {
        log.warn("Low rate limit remaining: {}", remaining);
    }
}
```

## Future Enhancements

### Circuit Breaker Pattern (optional)
```java
@CircuitBreaker(name = "claude-api", fallbackMethod = "fetchUsersFallback")
public List<UserData> fetchUsers() {
    // Implementation
}

private List<UserData> fetchUsersFallback(Exception e) {
    log.error("Circuit breaker activated for Claude API");
    return Collections.emptyList();
}
```

### Metrics Collection (optional)
```java
@Timed(value = "api.client.fetch.time", extraTags = {"tool", "claude-code"})
@Counted(value = "api.client.fetch.count", extraTags = {"tool", "claude-code"})
public List<UserData> fetchUsers() {
    // Implementation with metrics
}
```

## Related ADRs
- ADR-001: Escolha do Spring Boot (habilita WebClient, @ConditionalOnProperty)
- ADR-003: Arquitetura em Camadas (Integration Layer)

## References
- Spring WebClient: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html
- Reactor Retry: https://projectreactor.io/docs/core/release/reference/#extra-retry
- Integracoes.md - Especificação completa de cada integração
- RN-005: Tratamento de Falhas de Coleta
- RN-006: Limite de Rate Limiting

---

> *Generated by Claude Code - @Architect*
