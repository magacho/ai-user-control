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

### 1. Claude Code (Anthropic Admin API)

#### Obtenção de Credenciais

**Importante:** Você precisa de uma **Admin API Key**, não uma chave API regular!

1. Acesse https://console.anthropic.com/
2. Navegue para **Settings → API Keys**
3. Procure pela seção **Admin API Keys** (não "API Keys")
4. Clique em **Create Admin Key**
5. Copie a chave que começa com `sk-ant-admin-...`

**Requisitos:**
- Apenas membros com role **admin** podem criar Admin API Keys
- Admin Keys começam com `sk-ant-admin-...`
- Chaves regulares (`sk-ant-api03-...`) **não funcionam** para gerenciamento de organização

#### Configuração

**Via variáveis de ambiente:**
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

- Busca automática de usuários da organização via Admin API
- Suporte a paginação (até 100 usuários por requisição)
- Retry automático em caso de rate limit (429)
- Timeout configurável (padrão: 30s)
- Logs estruturados de todas as operações
- API Documentation: https://docs.anthropic.com/en/api/administration-api

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

### 3. Cursor (Admin API)

#### Obtenção de Credenciais

1. Acesse **Cursor Settings → Teams → Admin API**
2. Clique em **"Create API Key"**
3. Copie a chave que começa com `cur_...`
4. **Importante:** Apenas administradores do team podem criar API keys

#### Configuração

**Via variáveis de ambiente:**
```bash
export AI_CONTROL_CURSOR_ENABLED=true
export AI_CONTROL_CURSOR_TOKEN="cur_xxx"
```

**Via application.yml:**
```yaml
ai-control:
  api:
    cursor:
      enabled: true
      token: ${AI_CONTROL_CURSOR_TOKEN}
```

#### Features

- Busca automática de membros do team via Admin API
- Retry automático em caso de rate limit (429)
- Timeout configurável (padrão: 30s)
- Logs estruturados de todas as operações
- API Documentation: https://cursor.com/docs/account/teams/admin-api

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

### Exemplo: Buscar usuários do Cursor

```java
@Autowired
private CursorApiClient cursorClient;

public void fetchCursorUsers() {
    try {
        List<UserData> users = cursorClient.fetchUsers();
        users.forEach(user -> {
            log.info("Email: {}", user.getEmail());
            log.info("Name: {}", user.getName());
            log.info("Role: {}", user.getAdditionalMetrics().get("role"));
            log.info("---");
        });
    } catch (ApiClientException e) {
        log.error("Erro ao buscar usuários: {}", e.getMessage());
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
| `Not Found` (Claude) | Usando API key regular ao invés de Admin key | Criar e usar Admin API key (sk-ant-admin-...) |
| `Unauthorized` (Claude) | Admin key sem permissões | Verificar que você tem role admin na organização |
| `Rate limit exceeded` | Muitas requisições | Aguardar reset (retry automático) |
| `CSV file not found` | Arquivo não existe | Verificar caminho do CSV |
| `Email is required` | CSV sem campo email | Adicionar coluna `email` |

## Testes

### Testes Unitários

O projeto possui **45 testes unitários** com cobertura >80%:

```bash
# Executar todos os testes unitários
mvn test

# Executar testes de uma classe específica
mvn test -Dtest=ClaudeApiClientTest

# Gerar relatório de cobertura
mvn jacoco:report
# Relatório em: target/site/jacoco/index.html
```

### Testes de Integração (APIs Reais)

O projeto também inclui **testes de integração** que fazem chamadas reais às APIs:

```bash
# Configurar credenciais (copie e edite .env.example)
cp .env.example .env
source .env

# Executar testes de integração
mvn verify -P integration-tests

# Ou usar o script auxiliar
./run-integration-tests.sh
```

**Características:**
- ✅ Fazem chamadas reais às APIs (sem mocks)
- ✅ Validam que a integração funciona de verdade
- ✅ Ignoram testes automaticamente se credenciais não configuradas
- ✅ Exibem output detalhado dos dados reais retornados
- ✅ Baixo risco (apenas operações de leitura)

Para mais informações, veja [INTEGRATION_TESTS.md](INTEGRATION_TESTS.md)

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
