# Contexto do Projeto
- **Stack**: Java 21, Spring Boot 3, PostgreSQL.
- **Gerenciador**: Maven (\`./mvnw\`).
- **Arquitetura**: MVC, Service Layer, Repository Pattern.

# Comandos Essenciais
- **Build**: \`./mvnw clean install\`
- **Testes**: \`./mvnw test\`
- **Run**: \`./mvnw spring-boot:run\`

# ⚙️ Configuração de Workflow (Comente/Descomente para trocar)

## Opção A: Stack Atlassian (Jira + Bitbucket)
# Leia estas regras para usar Jira e Bitbucket:
- Gestão: @docs/workflow/tracking-github.md
- Repo: @docs/workflow/repo-github.md

## Opção B: Stack GitHub (Issues + Repo)
# Leia estas regras para usar GitHub total (Requer 'gh' CLI instalado):
#- Gestão: @docs/workflow/tracking-github.md
#- Repo: @docs/workflow/repo-github.md

# 🛡️ Padrões Técnicos (Sempre Ativos)
- Java/Spring: @docs/tech/java-standards.md
- **CRÍTICO**: Não use Lombok. Use Logs Estruturados.

# Instruções Globais
- Inicie tarefas complexas sempre com \`Plan Mode\`.
- TDD é obrigatório.
- Antes de push, execute testes locais.
