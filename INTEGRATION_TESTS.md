# Guia de Testes de Integração

Este documento explica como executar testes de integração com chamadas reais às APIs externas.

## Visão Geral

Os testes de integração fazem **chamadas reais** às APIs de:
- Claude Code (Anthropic API)
- GitHub Copilot
- Cursor (CSV import)

Estes testes são **separados** dos testes unitários e executam apenas quando credenciais válidas estão configuradas.

## Estrutura

```
src/
├── test/                    # Testes unitários (mocks)
└── integration-test/        # Testes de integração (APIs reais)
    ├── java/
    │   └── com/bemobi/aicontrol/integration/
    │       ├── BaseIntegrationTest.java
    │       ├── claude/
    │       │   └── ClaudeApiClientIntegrationTest.java
    │       ├── github/
    │       │   └── GitHubCopilotApiClientIntegrationTest.java
    │       └── cursor/
    │           └── CursorCsvClientIntegrationTest.java
    └── resources/
        └── application-integration-test.yml
```

## Configuração

### 1. Criar arquivo de configuração

Copie o arquivo de exemplo e preencha com suas credenciais:

```bash
cp .env.example .env
```

Edite `.env` com suas credenciais reais:

```bash
# Claude Code
export AI_CONTROL_CLAUDE_ENABLED=true
export AI_CONTROL_CLAUDE_TOKEN="sk-ant-api03-xxx"
export AI_CONTROL_CLAUDE_ORG_ID="org_xxx"

# GitHub Copilot
export AI_CONTROL_GITHUB_ENABLED=true
export AI_CONTROL_GITHUB_TOKEN="ghp_xxx"
export AI_CONTROL_GITHUB_ORG="your-organization"

# Cursor
export AI_CONTROL_CURSOR_ENABLED=true
export AI_CONTROL_CURSOR_CSV_PATH="$HOME/.ai-control/imports/cursor"
```

### 2. Carregar variáveis de ambiente

```bash
source .env
```

### 3. Preparar dados do Cursor (opcional)

Se você quiser testar a integração com Cursor:

```bash
# Criar diretório
mkdir -p ~/.ai-control/imports/cursor

# Criar um CSV de exemplo
cat > ~/.ai-control/imports/cursor/cursor-users.csv << 'EOF'
email,name,status,last_active,joined_at
john.doe@example.com,John Doe,active,2026-01-21,2025-06-01
jane.smith@example.com,Jane Smith,active,2026-01-20,2025-05-15
EOF
```

## Executando os Testes

### Opção 1: Script Automático (Recomendado)

```bash
./run-integration-tests.sh
```

Este script:
- Verifica se as variáveis de ambiente estão configuradas
- Exibe quais integrações estão habilitadas
- Executa todos os testes de integração

### Opção 2: Maven Direto

```bash
# Executar todos os testes de integração
mvn verify -P integration-tests

# Executar apenas Claude Code
mvn verify -P integration-tests -Dit.test=ClaudeApiClientIntegrationTest

# Executar apenas GitHub Copilot
mvn verify -P integration-tests -Dit.test=GitHubCopilotApiClientIntegrationTest

# Executar apenas Cursor
mvn verify -P integration-tests -Dit.test=CursorCsvClientIntegrationTest
```

### Opção 3: Pela IDE (IntelliJ/Eclipse)

1. Configurar variáveis de ambiente na configuração de execução
2. Executar a classe de teste específica
3. Verificar que o perfil `integration-test` está ativo

## Comportamento dos Testes

### Testes são Ignorados Automaticamente

Os testes verificam se as credenciais estão configuradas antes de executar. Se não estiverem, o teste é **ignorado (skipped)** ao invés de falhar.

Exemplo:
```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 2
```

Significa que 2 testes foram ignorados porque as credenciais não estavam configuradas.

### Output Detalhado

Os testes exibem informações sobre as chamadas reais:

```
=== Testing REAL Claude Code API Connection ===
Tool: claude-code
Success: true
Message: Connection successful

=== Fetching REAL Users from Claude Code ===
Total users fetched: 15

Sample user:
  Email: john.doe@example.com
  Name: John Doe
  Status: active
  Last Activity: 2026-01-21T14:30:00
  Additional Metrics:
    role: member
    joined_at: 2025-06-01T00:00:00
    member_id: user_123
```

## O que os Testes Validam

### Claude Code
- ✅ Conexão com a API
- ✅ Autenticação funciona
- ✅ Busca de membros da organização
- ✅ Estrutura dos dados retornados
- ✅ Métricas adicionais

### GitHub Copilot
- ✅ Conexão com a API
- ✅ Autenticação funciona
- ✅ Busca de seats do Copilot
- ✅ Fallback de email (quando não disponível)
- ✅ Métricas específicas do GitHub
- ✅ Tratamento de organizações sem Copilot

### Cursor (CSV)
- ✅ Acesso ao diretório de CSVs
- ✅ Busca do arquivo mais recente
- ✅ Import de dados do CSV
- ✅ Validação de campos obrigatórios
- ✅ Parsing de datas
- ✅ Normalização de emails

## Troubleshooting

### Erro: "Connection test failed"

**Causa**: Credenciais inválidas ou expiradas

**Solução**:
1. Verificar se as variáveis de ambiente estão corretas
2. Verificar se o token não expirou
3. Verificar se o Organization ID está correto

### Erro: "Rate limit exceeded"

**Causa**: Muitas requisições em curto período

**Solução**:
1. Aguardar alguns minutos
2. Os testes têm retry automático, mas pode precisar aguardar o reset
3. Verificar logs para o horário de reset

### Erro: "Organization not found"

**Causa**: Organization ID/nome incorreto

**Solução**:
1. Claude: Verificar Organization ID em https://console.anthropic.com/
2. GitHub: Verificar nome exato da organização

### Erro: "CSV directory not found"

**Causa**: Diretório não existe ou caminho incorreto

**Solução**:
```bash
mkdir -p ~/.ai-control/imports/cursor
export AI_CONTROL_CURSOR_CSV_PATH="$HOME/.ai-control/imports/cursor"
```

### Todos os testes são ignorados (skipped)

**Causa**: Nenhuma credencial configurada

**Solução**:
1. Configurar variáveis de ambiente
2. Executar `source .env`
3. Verificar com `echo $AI_CONTROL_CLAUDE_TOKEN`

## Segurança

⚠️ **IMPORTANTE**: Nunca commitar credenciais reais!

- O arquivo `.env` está no `.gitignore`
- Use `.env.example` como template
- Credenciais devem ser mantidas apenas localmente
- Para CI/CD, use secrets do GitHub Actions

## Diferenças entre Testes Unitários e de Integração

| Aspecto | Testes Unitários | Testes de Integração |
|---------|------------------|----------------------|
| Localização | `src/test/` | `src/integration-test/` |
| APIs | Mockadas | Reais |
| Credenciais | Não necessárias | Obrigatórias |
| Velocidade | Rápido (~8s) | Lento (~30s+) |
| Execução | `mvn test` | `mvn verify -P integration-tests` |
| CI/CD | Sempre | Opcional |

## Executar Apenas Testes Unitários

Para executar **apenas** os testes unitários (comportamento padrão):

```bash
mvn test
```

Isso NÃO executa os testes de integração.

## Cobertura de Código

Os testes de integração também contribuem para a cobertura de código:

```bash
# Gerar relatório incluindo integração
mvn verify -P integration-tests
mvn jacoco:report

# Ver relatório
open target/site/jacoco/index.html
```

## CI/CD

Exemplo de configuração para GitHub Actions:

```yaml
name: Integration Tests

on:
  workflow_dispatch:  # Manual execution
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run Integration Tests
        env:
          AI_CONTROL_CLAUDE_ENABLED: true
          AI_CONTROL_CLAUDE_TOKEN: ${{ secrets.CLAUDE_TOKEN }}
          AI_CONTROL_CLAUDE_ORG_ID: ${{ secrets.CLAUDE_ORG_ID }}
          AI_CONTROL_GITHUB_ENABLED: true
          AI_CONTROL_GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          AI_CONTROL_GITHUB_ORG: ${{ secrets.GITHUB_ORG }}
        run: mvn verify -P integration-tests
```

## Próximos Passos

Após validar as integrações reais, você pode:

1. Adicionar mais cenários de teste
2. Testar casos de erro específicos
3. Validar rate limits
4. Testar retry e backoff
5. Adicionar métricas de performance

---

> 🤖 *Generated by Claude Code*
