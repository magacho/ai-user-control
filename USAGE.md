# AI User Control - Guia de Uso

## 🚀 Quick Start

### 1. Configure suas credenciais

Copie o arquivo de exemplo e configure suas credenciais:

```bash
cp .env.example .env
# Edite o arquivo .env com suas credenciais reais
```

**Variáveis importantes:**
```bash
# Exportar automaticamente na inicialização
AI_CONTROL_EXPORT_ON_STARTUP=true

# Diretório de saída dos CSVs
AI_CONTROL_EXPORT_OUTPUT_DIR=./output

# Gerar CSV consolidado (todas as ferramentas em um arquivo)
AI_CONTROL_EXPORT_CONSOLIDATED=false

# Habilitar integrações
AI_CONTROL_CLAUDE_ENABLED=true
AI_CONTROL_CLAUDE_TOKEN=sk-ant-admin-xxxxxxxxxxxxxxxxxxxxx

AI_CONTROL_GITHUB_ENABLED=true
AI_CONTROL_GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxxx
AI_CONTROL_GITHUB_ORG=your-org

AI_CONTROL_CURSOR_ENABLED=true
AI_CONTROL_CURSOR_TOKEN=key_xxxxxxxxxxxxxxxxxxxxx
```

### 2. Execute a aplicação

```bash
java -jar target/ai-user-control-0.0.1-SNAPSHOT.jar
```

### 3. Verifique os arquivos gerados

Os arquivos CSV serão criados no diretório `./output/`:

```
output/
├── claude-users-20260205-143000.csv
├── github-copilot-users-20260205-143000.csv
└── cursor-users-20260205-143000.csv
```

## 📄 Formato dos CSVs

Cada arquivo CSV contém as seguintes colunas:

```csv
tool,email,name,status,last_activity_at,collected_at
claude,user@example.com,John Doe,active,2026-02-05T14:30:45,2026-02-05T14:30:00
```

**Colunas:**
- `tool`: Nome da ferramenta (claude, github-copilot, cursor)
- `email`: Email do usuário
- `name`: Nome completo do usuário
- `status`: Status do usuário (active, inactive)
- `last_activity_at`: Data/hora da última atividade
- `collected_at`: Data/hora da coleta dos dados

## 🔧 Configurações Avançadas

### Desabilitar execução automática

Se você não quer que a coleta execute automaticamente ao iniciar:

```bash
export AI_CONTROL_EXPORT_ON_STARTUP=false
java -jar target/ai-user-control-0.0.1-SNAPSHOT.jar
```

### Gerar CSV consolidado

Para gerar um único CSV com dados de todas as ferramentas:

```bash
export AI_CONTROL_EXPORT_CONSOLIDATED=true
java -jar target/ai-user-control-0.0.1-SNAPSHOT.jar
```

Isso criará um arquivo adicional:
```
output/all-users-consolidated-20260205-143000.csv
```

### Personalizar diretório de saída

```bash
export AI_CONTROL_EXPORT_OUTPUT_DIR=/var/data/ai-control/exports
java -jar target/ai-user-control-0.0.1-SNAPSHOT.jar
```

## 🏗️ Build do Projeto

```bash
# Build completo com testes
mvn clean install

# Build sem testes (mais rápido)
mvn clean package -DskipTests

# Executar apenas testes
mvn test

# Executar testes de integração
mvn verify -Pintegration-tests
```

## 📊 Exemplo de Output

```
================================================================================
Starting AI User Control data collection and export
================================================================================
2026-02-05 14:30:00 - Collecting users from Claude Code...
2026-02-05 14:30:01 - Successfully collected 45 users from Claude Code
2026-02-05 14:30:01 - Collecting users from GitHub Copilot...
2026-02-05 14:30:02 - Successfully collected 38 users from GitHub Copilot
2026-02-05 14:30:02 - Collecting users from Cursor...
2026-02-05 14:30:03 - Successfully collected 52 users from Cursor
2026-02-05 14:30:03 - User collection completed. Total users collected: 135 from 3 integrations
2026-02-05 14:30:03 - Total users collected: 135
2026-02-05 14:30:03 -
2026-02-05 14:30:03 - Exporting to CSV files...
2026-02-05 14:30:03 - Created: claude-users-20260205-143003.csv (45 users, 3842 bytes)
2026-02-05 14:30:03 - Created: github-copilot-users-20260205-143003.csv (38 users, 3156 bytes)
2026-02-05 14:30:03 - Created: cursor-users-20260205-143003.csv (52 users, 4389 bytes)
2026-02-05 14:30:03 - CSV export completed. Generated 3 files
2026-02-05 14:30:03 -
2026-02-05 14:30:03 - CSV files generated:
2026-02-05 14:30:03 -   - /home/user/ai-user-control/output/claude-users-20260205-143003.csv
2026-02-05 14:30:03 -   - /home/user/ai-user-control/output/github-copilot-users-20260205-143003.csv
2026-02-05 14:30:03 -   - /home/user/ai-user-control/output/cursor-users-20260205-143003.csv
2026-02-05 14:30:03 -
================================================================================
Export completed successfully!
================================================================================
```

## 🐛 Troubleshooting

### Nenhum usuário coletado

Verifique:
1. As integrações estão habilitadas? (`AI_CONTROL_*_ENABLED=true`)
2. Os tokens estão corretos?
3. Você tem permissão para acessar as APIs?

### Erro de autenticação

- **Claude**: Certifique-se de usar uma Admin API key (`sk-ant-admin-...`)
- **GitHub**: Token precisa dos scopes `read:org` e `manage_billing:copilot`
- **Cursor**: Verifique o token no dashboard do Cursor

### Diretório de output não existe

O diretório será criado automaticamente. Se houver erro de permissão, verifique:
```bash
mkdir -p ./output
chmod 755 ./output
```

## 📚 Mais Informações

- [README.md](README.md) - Documentação completa do projeto
- [Integracoes.md](Integracoes.md) - Detalhes sobre cada integração
- [INTEGRATION_TESTS.md](INTEGRATION_TESTS.md) - Como executar testes de integração
