# 🚀 Guia Rápido - Primeiro Teste

## 📋 Pré-requisitos

Antes de começar, você precisa ter em mãos:

### 1. **Claude** (Anthropic)
- ✅ Token: `sk-ant-admin-xxx` (obtido em https://console.anthropic.com/)
- ✅ Org ID: `org_xxx` (opcional, encontrado nas configurações)

### 2. **GitHub Copilot**
- ✅ Token: `ghp_xxx` (obtido em https://github.com/settings/tokens)
  - Scopes necessários: `read:org`, `manage_billing:copilot`
- ✅ Nome da organização GitHub

### 3. **Google Workspace** (opcional, mas recomendado)
- ✅ Arquivo JSON da Service Account (baixado do Google Cloud Console)
- ✅ Domínio da empresa (ex: `suaempresa.com`)
- ✅ Email de um admin (ex: `admin@suaempresa.com`)

---

## ⚡ Setup em 3 Passos

### Passo 1: Configure as APIs básicas (Claude + GitHub)

```bash
# Criar arquivo de configuração
cat > .env << 'EOF'
# Claude
export AI_CONTROL_CLAUDE_ENABLED=true
export AI_CONTROL_CLAUDE_TOKEN="COLE_SEU_TOKEN_AQUI"

# GitHub Copilot
export AI_CONTROL_GITHUB_ENABLED=true
export AI_CONTROL_GITHUB_TOKEN="COLE_SEU_TOKEN_AQUI"
export AI_CONTROL_GITHUB_ORG="NOME_DA_SUA_ORG"

# Cursor (desabilitado por enquanto)
export AI_CONTROL_CURSOR_ENABLED=false
EOF

# Edite o arquivo e cole seus tokens reais
nano .env

# Carregue as variáveis
source .env
```

### Passo 2: Configure Google Workspace (opcional)

```bash
# Execute o script interativo
./setup-workspace.sh

# O script vai perguntar:
# 1. Onde está o arquivo JSON (ex: ~/Downloads/service-account-key.json)
# 2. Domínio do Workspace (ex: suaempresa.com)
# 3. Email do admin (ex: admin@suaempresa.com)

# Depois, carregue novamente
source .env
```

### Passo 3: Teste a configuração

```bash
# Executar validação completa
./test-integration.sh

# Deve mostrar:
# ✓ APIs configuradas
# ✓ Testes unitários passando (15 testes)
# ✓ Projeto compilado
```

---

## 🎯 Testando a Coleta de Dados

### Coletar dados das APIs

```bash
# Compilar e executar
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 21.0.9-tem
./mvnw spring-boot:run
```

Isso vai:
1. ✅ Conectar com Claude API
2. ✅ Conectar com GitHub Copilot API
3. ✅ Coletar dados de usuários
4. ✅ Gerar arquivos CSV em `./output/`

### Verificar os arquivos gerados

```bash
ls -lh output/

# Você deve ver arquivos como:
# - claude-users-YYYYMMDD-HHMMSS.csv
# - github-copilot-users-YYYYMMDD-HHMMSS.csv
# - consolidated-users-YYYYMMDD-HHMMSS.csv
```

---

## 📊 Gerar Relatório XLSX (Issue #29)

O relatório consolidado com 3 abas ainda não tem comando CLI (Issue #8 pendente), mas você pode testar via código:

```java
// Exemplo de uso do UnifiedSpendingService
LocalDate start = LocalDate.of(2026, 2, 1);
LocalDate end = LocalDate.of(2026, 2, 28);

ConsolidatedReport report = unifiedSpendingService.generateSpendingReport(start, end);
Path xlsxFile = unifiedSpendingService.exportToXlsx(report, Paths.get("output/report.xlsx"));

System.out.println("Relatório gerado: " + xlsxFile);
```

---

## 🔍 Troubleshooting

### Erro: "Claude API 401 Unauthorized"
- ✅ Verifique se o token começa com `sk-ant-admin-` (não `sk-ant-api03-`)
- ✅ Confirme que o token tem permissões de **Admin API**

### Erro: "GitHub 403 Forbidden"
- ✅ Verifique se o token tem os scopes: `read:org` e `manage_billing:copilot`
- ✅ Confirme que você tem acesso à organização especificada

### Erro: "Google Workspace 403"
- ✅ Verifique se Domain-Wide Delegation está configurado corretamente
- ✅ Confirme o escopo: `https://www.googleapis.com/auth/admin.directory.user.readonly`
- ✅ Admin SDK API está habilitada no Google Cloud Console?

### Erro: "Java version 21 not supported"
- ✅ Use SDKMAN para ativar Java 21:
  ```bash
  source ~/.sdkman/bin/sdkman-init.sh
  sdk use java 21.0.9-tem
  ```

---

## 📚 Documentação Completa

- **Integrações detalhadas:** `Integracoes.md`
- **Google Workspace:** `docs/setup-google-workspace.md`
- **Testes de integração:** `INTEGRATION_TESTS.md`
- **Uso geral:** `USAGE.md`

---

## ✅ Checklist de Validação

Marque conforme for testando:

- [ ] Claude API configurada e testada
- [ ] GitHub Copilot API configurada e testada
- [ ] Google Workspace configurado (opcional)
- [ ] Testes unitários passando (15 testes)
- [ ] Coleta de dados funcionando
- [ ] Arquivos CSV gerados em `./output/`
- [ ] Relatório XLSX testado (quando CLI estiver pronto)

---

**🎉 Pronto! Agora você tem um sistema completo de monitoramento de uso de ferramentas de IA.**

Para dúvidas ou problemas, consulte:
- GitHub Issues: https://github.com/magacho/ai-user-control/issues
- Documentação: arquivos `.md` na raiz e em `docs/`
