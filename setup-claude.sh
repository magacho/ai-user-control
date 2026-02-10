#!/bin/bash

echo "Criando estrutura de pastas estendida..."
mkdir -p .claude/commands
mkdir -p docs/workflow
mkdir -p docs/tech

# 1. Cria o CLAUDE.md com "Chaves de Troca"
echo "Criando CLAUDE.md Mestre..."
cat << 'EOF' > CLAUDE.md
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
- Gestão: @docs/workflow/tracking-jira.md
- Repo: @docs/workflow/repo-bitbucket.md

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
EOF

# 2. Regras do Jira (Já existentes)
echo "Criando docs/workflow/tracking-jira.md..."
cat << 'EOF' > docs/workflow/tracking-jira.md
# Diretrizes de Gestão (Jira)
- **Fonte da Verdade**: Os Critérios de Aceite no ticket Jira são a lei.
- **Formato**: Trabalhe sempre vinculado a uma chave (ex: \`PROJ-123\`).
- **Ambíguidade**: Se o ticket for vago, gere perguntas para eu postar no Jira antes de codar.
EOF

# 3. Regras do Bitbucket (Já existentes)
echo "Criando docs/workflow/repo-bitbucket.md..."
cat << 'EOF' > docs/workflow/repo-bitbucket.md
# Diretrizes de Repositório (Bitbucket)
- **Branches**: \`feature/[JIRA-ID]-[slug]\` ou \`bugfix/[JIRA-ID]-[slug]\`.
- **Commits**: \`[JIRA-ID] Descrição imperativa\`.
- **PRs**: Como não há CLI nativa configurada, após o push, mostre a URL do console para criar o PR.
EOF

# 4. NOVAS Regras do GitHub Issues
echo "Criando docs/workflow/tracking-github.md..."
cat << 'EOF' > docs/workflow/tracking-github.md
# Diretrizes de Gestão (GitHub Issues)
- **Integração**: Use a ferramenta CLI \`gh\` para ler issues.
  - Exemplo: \`gh issue view 123\` para ler requisitos.
- **Vínculo**: Mencione o número da issue (#123) em commits e PRs para rastreamento automático.
- **Fechamento**: Se o PR resolve a issue, use "Closes #123" na descrição do PR.
EOF

# 5. NOVAS Regras do GitHub Repo
echo "Criando docs/workflow/repo-github.md..."
cat << 'EOF' > docs/workflow/repo-github.md
# Diretrizes de Repositório (GitHub)
- **Ferramentas**: Use a CLI \`gh\` para todas as operações de Pull Request.
- **Branches**: \`feat/issue-123-descricao\` ou \`fix/issue-123-descricao\`.
- **Commits**: \`feat(auth): adiciona login (#123)\` (Use Conventional Commits se possível).
- **Automação de PR**:
  - Ao terminar, use \`gh pr create --fill\` ou gere uma descrição baseada no diff.
  - Solicite minha revisão antes de dar merge.
EOF

# 6. Padrões Java (Mantido igual - Sem Lombok)
echo "Criando docs/tech/java-standards.md..."
cat << 'EOF' > docs/tech/java-standards.md
# Padrões Técnicos Java & Spring Boot

## 🚫 Proibições (Estritas)
- **NÃO USE LOMBOK**. Nada de \`@Data\`, \`@Builder\`, etc.
  - Use \`record\` para DTOs.
  - Gere Getters/Setters manualmente para Entidades.

## 📝 Logs Estruturados
- Use SLF4J + Logback.
- **MDC**: Injete contexto (userId, transactionId) no MDC no início da requisição.
- Logs devem ser legíveis como JSON em produção.

## 🧪 Testes
- JUnit 5 e Mockito.
- Evite carregar o contexto inteiro do Spring (\`@SpringBootTest\`) em testes unitários.
EOF

# 7. Comando: Resolver Issue GitHub (Automação extra)
echo "Criando comando .claude/commands/resolve-gh.md..."
cat << 'EOF' > .claude/commands/resolve-gh.md
---
description: Resolve uma GitHub Issue de ponta a ponta usando 'gh'.
---
# Resolver Issue GitHub: $ARGUMENTS

1. **Leitura**: Use \`gh issue view $ARGUMENTS\` para entender o problema.
2. **Setup**: Certifique-se de estar na branch \`main\` atualizada e crie uma branch \`fix/issue-$ARGUMENTS\`.
3. **Planejamento**: Entre em **Plan Mode** e analise os arquivos afetados.
4. **Execução**:
   - Siga as regras de Java em @docs/tech/java-standards.md.
   - Aplique TDD.
5. **Entrega**:
   - Rode os testes.
   - Faça commit referenciando a issue (\`#$ARGUMENTS\`).
   - Use \`gh pr create\` para abrir o PR automaticamente.
EOF