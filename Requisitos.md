# Documento de Requisitos

## 1. Informações do Projeto

- **Nome do Projeto:** ai-user-control
- **Versão:** 0.0.1
- **Data:** 22/01/2026
- **Autor(es):** Flavio Magacho <flavio@magacho.com>
- **Stakeholders:** 

## 2. Visão Geral

### 2.1 Objetivo do Projeto
Este projeto tem por objetivo coletar e consolidar informações de uso de ferramentas de IA para desenvolvimento de software (Claude Code, GitHub Copilot e Cursor), permitindo uma gestão eficiente de usuários, visibilidade sobre padrões de uso e controle de custos operacionais.

O sistema fornecerá uma ferramenta CLI baseada em Spring Boot com capacidade de gerar relatórios automatizados que auxiliem na tomada de decisões sobre licenciamento, alocação de recursos e otimização de investimentos em ferramentas de IA.

### 2.2 Escopo

**Dentro do Escopo:**
- Coleta automatizada de dados de usuários ativos nas ferramentas: Claude Code, GitHub Copilot e Cursor
- Unificação e correlação de usuários entre diferentes plataformas (usando email como chave principal)
- Geração de relatórios sobre utilização e distribuição de usuários
- Interface CLI para execução e agendamento de coletas
- Armazenamento histórico de dados para análise temporal
- Documentação completa de desenvolvimento e uso

**Fora do Escopo (nesta versão):**
- Interface web/dashboard visual
- Integração com outras ferramentas de IA além das três mencionadas
- Controle de acesso e permissionamento de usuários
- Faturamento e cobrança automatizada

### 2.3 Contexto
A organização utiliza múltiplas ferramentas de IA para auxiliar desenvolvedores (Cursor, Claude Code, GitHub Copilot), cada uma com sua própria base de usuários e sistema de licenciamento.

**Desafios Atuais:**
- Falta de visibilidade sobre quais usuários estão utilizando cada ferramenta
- Impossibilidade de identificar usuários duplicados em múltiplas plataformas
- Ausência de dados consolidados sobre frequência e padrões de uso
- Dificuldade em justificar e otimizar gastos com licenças
- Impossibilidade de realizar análises históricas de adoção

**Solução Proposta:**
Sistema CLI que integra com as APIs das ferramentas, coleta dados periodicamente, normaliza e consolida informações em uma base local, e gera relatórios customizáveis para apoiar decisões de gestão. 

## 3. Requisitos Funcionais

### RF-001: Integração com APIs das Ferramentas de IA
- **Descrição:** O sistema deve integrar-se com as APIs oficiais das ferramentas Claude Code, GitHub Copilot e Cursor para coletar informações de usuários ativos e seus respectivos dados de uso.
- **Prioridade:** Crítica
- **Critérios de Aceitação:**
    - Integração funcional com a API do Claude Code (Anthropic)
    - Integração funcional com a API do GitHub Copilot
    - Integração funcional com a API do Cursor
    - Extração de dados mínimos: email, nome do usuário, status (ativo/inativo), data da última atividade
    - Tratamento de erros de autenticação e rate limiting
    - Logs detalhados de cada coleta realizada
    - Capacidade de executar coletas de forma independente por ferramenta
- **Dependências:** Credenciais de API e tokens de acesso para cada plataforma

### RF-002: Normalização e Armazenamento de Dados
- **Descrição:** O sistema deve normalizar os dados coletados de diferentes fontes em um formato padronizado e armazená-los em uma base de dados local para análise e geração de relatórios.
- **Prioridade:** Crítica
- **Critérios de Aceitação:**
  - Dados normalizados em schema único independente da fonte
  - Email como chave primária de identificação de usuário
  - Armazenamento de timestamp de coleta para rastreabilidade
  - Manutenção de histórico de coletas anteriores
  - Capacidade de identificar mudanças entre coletas (novos usuários, remoções, alterações)
  - Validação de integridade dos dados antes do armazenamento

### RF-003: Unificação e Correlação de Usuários
- **Descrição:** O sistema deve identificar e correlacionar usuários que aparecem em múltiplas ferramentas usando o email como chave de unificação.
- **Prioridade:** Alta
- **Critérios de Aceitação:**
  - Algoritmo de matching baseado em email (case-insensitive)
  - Identificação de usuários únicos vs. usuários duplicados em múltiplas plataformas
  - Matriz de presença mostrando em quais ferramentas cada usuário está ativo
  - Detecção de inconsistências (emails diferentes para mesmo usuário, caso existam)

### RF-004: Geração de Relatórios Consolidados
- **Descrição:** O sistema deve gerar relatórios em múltiplos formatos apresentando análises sobre distribuição e uso das ferramentas.
- **Prioridade:** Alta
- **Critérios de Aceitação:**
  - Relatório de total de usuários por ferramenta
  - Relatório de usuários em múltiplas ferramentas (overlap)
  - Relatório de usuários exclusivos de cada ferramenta
  - Relatório de evolução temporal (se houver histórico)
  - Formatos de saída: TXT, CSV, JSON, MD (Markdown)
  - Possibilidade de filtrar relatórios por data/período

### RF-005: Interface CLI (Command Line Interface)
- **Descrição:** O sistema deve fornecer uma interface de linha de comando intuitiva para execução de comandos de coleta, análise e geração de relatórios.
- **Prioridade:** Alta
- **Critérios de Aceitação:**
  - Comando para coletar dados de todas as ferramentas: `ai-control collect`
  - Comando para coletar dados de ferramenta específica: `ai-control collect --tool <nome>`
  - Comando para gerar relatórios: `ai-control report --type <tipo> --format <formato>`
  - Comando para listar histórico de coletas: `ai-control history`
  - Help detalhado para cada comando: `ai-control --help`
  - Outputs informativos e progress indicators durante operações longas
  - Códigos de retorno apropriados para integração com scripts

### RF-006: Configuração e Gerenciamento de Credenciais
- **Descrição:** O sistema deve permitir configuração segura de credenciais de API e parâmetros de execução usando mecanismos do Spring Boot.
- **Prioridade:** Alta
- **Critérios de Aceitação:**
  - Configuração via `application.yml` ou variáveis de ambiente
  - Suporte a Spring Profiles (dev, test, prod)
  - Propriedades tipadas usando `@ConfigurationProperties`
  - Comando para configurar credenciais: `config set <chave> <valor>`
  - Suporte a variáveis de ambiente para credenciais sensíveis
  - Validação de credenciais antes da primeira coleta usando Bean Validation
  - Comando para testar conectividade: `test-connection --tool <nome>`
  - Proteção de propriedades sensíveis nos logs (Spring Boot masking)

### RF-007: Agendamento e Automação
- **Descrição:** O sistema deve suportar execução automatizada e agendada de coletas.
- **Prioridade:** Média
- **Critérios de Aceitação:**
  - Documentação de como agendar via cron/systemd
  - Flag `--silent` para execução sem interação (modo batch)
  - Geração automática de relatório após cada coleta (configurável)
  - Notificação de erros via logs ou saída padrão
  
## 4. Requisitos Não Funcionais

### RNF-001: Performance
- **Descrição:** O sistema deve executar coletas e gerar relatórios de forma eficiente, minimizando tempo de resposta e uso de recursos.
- **Métricas:**
  - Coleta completa de todas as ferramentas em < 30 segundos (para até 1000 usuários)
  - Geração de relatórios em < 5 segundos
  - Uso de memória < 512MB durante execução
  - Tamanho do banco de dados local otimizado (< 100MB para 1000 usuários/ano)
  - Tempo de resposta de comandos CLI < 2 segundos

### RNF-002: Segurança
- **Descrição:** O sistema deve proteger credenciais de API e dados sensíveis dos usuários, seguindo boas práticas de segurança.
- **Métricas:**
  - Credenciais armazenadas em arquivo com permissões 600 (somente leitura pelo owner)
  - Suporte a credenciais via variáveis de ambiente (não hard-coded)
  - Dados em repouso não contêm senhas ou tokens em plain text
  - Comunicação com APIs usando TLS/HTTPS exclusivamente
  - Logs não devem expor credenciais ou dados sensíveis
  - Conformidade com princípio de privilégio mínimo nas APIs

### RNF-003: Usabilidade
- **Descrição:** A interface CLI deve ser intuitiva e seguir convenções comuns de ferramentas de linha de comando.
- **Métricas:**
  - Documentação de help acessível via `--help` em todos os comandos
  - Mensagens de erro claras e acionáveis
  - Progress indicators para operações > 3 segundos
  - Comandos seguem padrão verbo-substantivo (ex: `collect`, `report`)
  - Tempo médio para primeiro uso bem-sucedido < 10 minutos (incluindo configuração)
  - README com quick start guide e exemplos práticos

### RNF-004: Confiabilidade
- **Descrição:** O sistema deve ser robusto, com tratamento adequado de erros e capacidade de recuperação.
- **Métricas:**
  - Disponibilidade: 99% de sucesso em coletas agendadas
  - Retry automático em caso de falhas de rede (até 3 tentativas)
  - Falha em uma API não impede coleta das demais
  - Validação de dados antes de persistência (0% de dados corrompidos)
  - Logs detalhados de todas as operações para troubleshooting
  - Backup automático antes de operações destrutivas

### RNF-005: Manutenibilidade
- **Descrição:** O código deve ser bem estruturado, documentado e fácil de manter e estender.
- **Métricas:**
  - Cobertura de testes > 80%
  - Código Java sem uso de Lombok (explícito para melhor compreensão)
  - Documentação de arquitetura e decisões técnicas (ADRs)
  - Separação clara de responsabilidades (camadas: API client, Data, CLI, Reports)
  - Logs estruturados com níveis apropriados (DEBUG, INFO, WARN, ERROR)
  - Versionamento semântico para releases
  - Tempo médio para adicionar nova ferramenta de IA < 4 horas de desenvolvimento

### RNF-006: Portabilidade
- **Descrição:** O sistema deve ser executável em diferentes ambientes com mínima configuração.
- **Métricas:**
  - Compatível com Linux, macOS e Windows
  - Empacotamento como JAR executável standalone
  - Dependências externas documentadas e minimizadas
  - Containerização via Docker disponível
  - Java 11+ como requisito mínimo

### RNF-007: Observabilidade
- **Descrição:** O sistema deve fornecer visibilidade sobre sua operação e estado.
- **Métricas:**
  - Logs em formato estruturado (JSON ou padrão consistente)
  - Níveis de log configuráveis via parâmetro ou variável de ambiente
  - Métricas de execução: tempo de coleta, registros processados, erros ocorridos
  - Status de conectividade com cada API verificável via comando
  - Histórico de execuções mantido no banco de dados

## 5. Regras de Negócio

### RN-001: Identificação Única de Usuários
- **Descrição:** Usuários são considerados idênticos se possuírem o mesmo endereço de email (case-insensitive), independentemente da ferramenta.
- **Aplicação:**
  - O email deve ser normalizado (lowercase) antes de comparações
  - Emails em formato inválido devem ser registrados mas marcados como "inválidos" para revisão manual
  - Se um usuário aparecer com emails diferentes em ferramentas diferentes, deve ser tratado como usuários distintos

### RN-002: Prioridade de Dados em Conflito
- **Descrição:** Quando um mesmo usuário (mesmo email) possui dados conflitantes entre ferramentas, deve-se seguir ordem de prioridade.
- **Aplicação:**
  - Ordem de prioridade para nome: 1) GitHub Copilot, 2) Claude Code, 3) Cursor
  - O timestamp mais recente sempre prevalece para data de última atividade
  - Todos os conflitos devem ser registrados em log para auditoria

### RN-003: Retenção de Dados Históricos
- **Descrição:** O sistema deve manter histórico de todas as coletas realizadas por período mínimo de 12 meses.
- **Aplicação:**
  - Dados anteriores a 12 meses podem ser arquivados ou removidos
  - Relatórios de tendência histórica usam apenas dados disponíveis
  - Limpeza de dados deve ser explícita via comando `ai-control cleanup --older-than <período>`

### RN-004: Status de Usuário
- **Descrição:** Um usuário é considerado "ativo" se teve atividade nos últimos 30 dias em pelo menos uma ferramenta.
- **Aplicação:**
  - Usuários sem atividade recente são marcados como "inativos" nos relatórios
  - A definição de "atividade recente" pode ser configurada (padrão: 30 dias)
  - Usuários inativos em todas as ferramentas devem ser destacados em relatórios

### RN-005: Tratamento de Falhas de Coleta
- **Descrição:** Falhas na coleta de uma ferramenta não devem impedir a coleta das demais ou invalidar dados já coletados.
- **Aplicação:**
  - Cada ferramenta é coletada de forma independente
  - Falhas são registradas em log com detalhes do erro
  - Relatórios devem indicar claramente quais ferramentas tiveram coleta bem-sucedida
  - Dados parciais são válidos e podem ser utilizados

### RN-006: Limite de Rate Limiting
- **Descrição:** O sistema deve respeitar limites de taxa (rate limits) das APIs e implementar backoff exponencial.
- **Aplicação:**
  - Intervalo mínimo entre requisições: 1 segundo
  - Em caso de HTTP 429 (Too Many Requests), esperar tempo indicado no header Retry-After
  - Máximo de 3 tentativas com backoff exponencial (1s, 2s, 4s)
  - Após 3 falhas consecutivas, abortar coleta daquela ferramenta

### RN-007: Privacidade e Dados Sensíveis
- **Descrição:** O sistema coleta apenas dados necessários para análise de uso, sem armazenar código ou conteúdo de trabalho dos usuários.
- **Aplicação:**
  - Dados coletados limitados a: email, nome, status, timestamps
  - Nenhum código-fonte ou conteúdo de conversas deve ser armazenado
  - Dados de uso agregados (quantidade de requisições, uso de features) são permitidos se disponíveis via API
  - Relatórios não devem expor informações pessoais sem consentimento

### RN-008: Atualização vs. Inserção de Dados
- **Descrição:** Em cada coleta, dados existentes de um usuário devem ser atualizados, não duplicados.
- **Aplicação:**
  - Usar email + ferramenta como chave composta para identificação
  - Operações de INSERT vs UPDATE baseadas na existência prévia do registro
  - Manter data da primeira coleta e data da última atualização
  - Histórico de mudanças opcional (configurável)

## 6. Casos de Uso

### UC-001: Configurar Sistema pela Primeira Vez
- **Ator:** Administrador de TI
- **Pré-condições:**
  - Sistema instalado (Java 11+ disponível)
  - Acesso às credenciais de API das ferramentas
- **Fluxo Principal:**
    1. Administrador executa `ai-control config init`
    2. Sistema cria arquivo de configuração com template
    3. Administrador edita arquivo e insere credenciais de API
    4. Administrador executa `ai-control test-connection --all`
    5. Sistema valida credenciais de cada ferramenta
    6. Sistema confirma que todas as conexões estão funcionais
- **Fluxos Alternativos:**
    - **FA-001:** Se credenciais inválidas
      - Sistema exibe mensagem de erro específica da ferramenta
      - Administrador corrige credenciais e repete passo 4
- **Pós-condições:**
  - Sistema configurado e pronto para coletar dados
  - Arquivo de configuração criado com permissões adequadas (600)

### UC-002: Coletar Dados de Todas as Ferramentas
- **Ator:** Administrador de TI ou Agendador (cron/systemd)
- **Pré-condições:**
  - Sistema configurado com credenciais válidas
  - Conectividade com internet
- **Fluxo Principal:**
    1. Ator executa `ai-control collect`
    2. Sistema inicia coleta do Claude Code
    3. Sistema inicia coleta do GitHub Copilot
    4. Sistema inicia coleta do Cursor
    5. Sistema normaliza dados coletados
    6. Sistema persiste dados no banco local
    7. Sistema exibe resumo: X usuários em Claude Code, Y em Copilot, Z em Cursor
    8. Sistema registra coleta no histórico
- **Fluxos Alternativos:**
    - **FA-001:** Falha em uma das APIs
      - Sistema registra erro em log
      - Sistema continua com coleta das demais ferramentas
      - Sistema exibe warning ao final indicando falha parcial
    - **FA-002:** Modo silencioso (`--silent`)
      - Sistema executa sem output interativo
      - Sistema registra apenas em log
- **Pós-condições:**
  - Dados atualizados no banco local
  - Timestamp da última coleta registrado
  - Logs de execução disponíveis

### UC-003: Gerar Relatório de Usuários por Ferramenta
- **Ator:** Administrador de TI ou Gestor
- **Pré-condições:**
  - Pelo menos uma coleta bem-sucedida realizada
- **Fluxo Principal:**
    1. Ator executa `ai-control report --type summary --format md`
    2. Sistema consulta dados mais recentes no banco
    3. Sistema calcula totais por ferramenta
    4. Sistema identifica usuários em múltiplas plataformas
    5. Sistema gera relatório em formato Markdown
    6. Sistema exibe relatório no terminal
    7. Sistema salva relatório em arquivo `reports/summary-YYYY-MM-DD.md`
- **Fluxos Alternativos:**
    - **FA-001:** Formato CSV solicitado
      - Sistema gera relatório em CSV
      - Arquivo salvo como `reports/summary-YYYY-MM-DD.csv`
    - **FA-002:** Período específico (`--from YYYY-MM-DD --to YYYY-MM-DD`)
      - Sistema filtra dados do período solicitado
      - Relatório indica período de análise
- **Pós-condições:**
  - Relatório gerado e disponível em arquivo
  - Administrador tem visão consolidada dos dados

### UC-004: Identificar Usuários Duplicados em Múltiplas Ferramentas
- **Ator:** Gestor de Licenças
- **Pré-condições:**
  - Dados coletados de pelo menos duas ferramentas
- **Fluxo Principal:**
    1. Gestor executa `ai-control report --type overlap --format csv`
    2. Sistema identifica emails presentes em mais de uma ferramenta
    3. Sistema agrupa usuários por email
    4. Sistema lista ferramentas onde cada usuário está presente
    5. Sistema calcula estatísticas: % de overlap entre ferramentas
    6. Sistema gera relatório CSV com colunas: Email, Nome, Ferramentas
    7. Sistema salva relatório em `reports/overlap-YYYY-MM-DD.csv`
- **Fluxos Alternativos:**
    - **FA-001:** Apenas duas ferramentas específicas
      - Gestor executa `ai-control report --type overlap --tools claude-code,copilot`
      - Sistema analisa apenas as ferramentas especificadas
- **Pós-condições:**
  - Gestor identifica redundâncias de licenciamento
  - Dados disponíveis para decisões de otimização de custos

### UC-005: Consultar Histórico de Coletas
- **Ator:** Administrador de TI
- **Pré-condições:**
  - Pelo menos uma coleta realizada
- **Fluxo Principal:**
    1. Administrador executa `ai-control history`
    2. Sistema lista todas as coletas realizadas
    3. Para cada coleta, sistema exibe: data/hora, ferramentas coletadas, status, registros processados
    4. Sistema exibe total de coletas bem-sucedidas vs. falhas
- **Fluxos Alternativos:**
    - **FA-001:** Detalhes de coleta específica
      - Administrador executa `ai-control history --id <coleta-id>`
      - Sistema exibe logs detalhados daquela coleta
- **Pós-condições:**
  - Administrador tem visibilidade do histórico de operações
  - Possível identificar padrões de falhas

### UC-006: Agendar Coletas Periódicas
- **Ator:** Administrador de TI
- **Pré-condições:**
  - Sistema configurado
  - Acesso ao cron ou systemd do sistema operacional
- **Fluxo Principal:**
    1. Administrador decide frequência de coleta (ex: diária às 2h)
    2. Administrador cria entrada no cron: `0 2 * * * /usr/local/bin/ai-control collect --silent`
    3. Sistema executa automaticamente no horário agendado
    4. Sistema registra execução em log
    5. (Opcional) Sistema envia email/notificação em caso de falha
- **Fluxos Alternativos:**
    - **FA-001:** Uso de systemd timer
      - Administrador cria service e timer units
      - Sistema gerencia execução via systemd
- **Pós-condições:**
  - Coletas automáticas configuradas
  - Dados sempre atualizados sem intervenção manual

## 7. Modelo de Dados

### Diagrama de Relacionamentos
```
CollectionRun (1) ----< (N) UserToolSnapshot
                              |
User (1) ----< (N) UserToolSnapshot >---- (1) Tool
```

### Entidade: User
Representa um usuário único identificado pelo email.

**Atributos:**
- `id` (BIGINT, PK, AUTO_INCREMENT): Identificador único interno
- `email` (VARCHAR(255), UNIQUE, NOT NULL): Email normalizado (lowercase)
- `primary_name` (VARCHAR(255)): Nome prioritário do usuário (conforme RN-002)
- `first_seen_at` (TIMESTAMP, NOT NULL): Data da primeira vez que o usuário foi detectado
- `last_seen_at` (TIMESTAMP, NOT NULL): Data da última vez que o usuário foi visto em qualquer ferramenta
- `is_active` (BOOLEAN): Flag indicando se usuário está ativo (conforme RN-004)
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Timestamp de criação do registro
- `updated_at` (TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP): Timestamp de última atualização

**Índices:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `email`

**Regras:**
- Email deve ser único e normalizado (lowercase)
- `is_active` calculado baseado em `last_seen_at` (últimos 30 dias)

---

### Entidade: Tool
Representa uma ferramenta de IA monitorada pelo sistema.

**Atributos:**
- `id` (INT, PK, AUTO_INCREMENT): Identificador único
- `name` (VARCHAR(100), UNIQUE, NOT NULL): Nome da ferramenta (claude-code, github-copilot, cursor)
- `display_name` (VARCHAR(100)): Nome para exibição (Claude Code, GitHub Copilot, Cursor)
- `api_endpoint` (VARCHAR(255)): URL base da API
- `is_enabled` (BOOLEAN, DEFAULT TRUE): Se a ferramenta está ativa para coleta
- `created_at` (TIMESTAMP): Timestamp de criação

**Índices:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `name`

**Dados Iniciais:**
1. name='claude-code', display_name='Claude Code'
2. name='github-copilot', display_name='GitHub Copilot'
3. name='cursor', display_name='Cursor'

---

### Entidade: UserToolSnapshot
Representa o estado de um usuário em uma ferramenta específica em um momento no tempo.

**Atributos:**
- `id` (BIGINT, PK, AUTO_INCREMENT): Identificador único
- `user_id` (BIGINT, FK -> User.id, NOT NULL): Referência ao usuário
- `tool_id` (INT, FK -> Tool.id, NOT NULL): Referência à ferramenta
- `collection_run_id` (BIGINT, FK -> CollectionRun.id, NOT NULL): Referência à coleta
- `name_in_tool` (VARCHAR(255)): Nome do usuário conforme registrado na ferramenta
- `status` (VARCHAR(50)): Status do usuário (active, inactive, suspended, etc.)
- `last_activity_at` (TIMESTAMP): Data da última atividade na ferramenta (se disponível via API)
- `usage_metrics` (JSON, NULLABLE): Métricas de uso adicionais (formato flexível)
- `raw_data` (TEXT, NULLABLE): Dados brutos da API para debug (opcional)
- `created_at` (TIMESTAMP): Timestamp da criação do snapshot

**Índices:**
- PRIMARY KEY: `id`
- INDEX: `user_id`
- INDEX: `tool_id`
- INDEX: `collection_run_id`
- UNIQUE INDEX: `user_id, tool_id, collection_run_id` (um snapshot por usuário/ferramenta/coleta)

**Regras:**
- Cada combinação de user_id + tool_id + collection_run_id deve ser única
- `usage_metrics` pode conter dados como: request_count, last_request_at, features_used, etc.

---

### Entidade: CollectionRun
Representa uma execução de coleta de dados.

**Atributos:**
- `id` (BIGINT, PK, AUTO_INCREMENT): Identificador único
- `started_at` (TIMESTAMP, NOT NULL): Início da coleta
- `completed_at` (TIMESTAMP, NULLABLE): Fim da coleta (NULL se ainda em execução ou falhou)
- `status` (VARCHAR(50), NOT NULL): Status (running, completed, partial_failure, failed)
- `tools_collected` (VARCHAR(255)): Lista de ferramentas coletadas (claude-code,copilot,cursor)
- `tools_failed` (VARCHAR(255), NULLABLE): Lista de ferramentas que falharam
- `total_users_processed` (INT): Total de usuários processados
- `total_snapshots_created` (INT): Total de snapshots criados
- `error_message` (TEXT, NULLABLE): Mensagem de erro se houver falha
- `execution_time_ms` (BIGINT): Tempo de execução em milissegundos
- `triggered_by` (VARCHAR(100)): Como foi iniciada (manual, cron, systemd, etc.)
- `created_at` (TIMESTAMP): Timestamp de criação

**Índices:**
- PRIMARY KEY: `id`
- INDEX: `started_at` (para queries de histórico)
- INDEX: `status`

**Regras:**
- `status` transitions: running -> completed/partial_failure/failed
- `completed_at` deve ser NULL enquanto `status` = running
- `execution_time_ms` = completed_at - started_at

---

### Entidade: Configuration
Armazena configurações do sistema (alternativa a arquivo de configuração).

**Atributos:**
- `key` (VARCHAR(100), PK): Chave da configuração
- `value` (TEXT): Valor da configuração
- `is_sensitive` (BOOLEAN, DEFAULT FALSE): Se é dado sensível (credencial)
- `description` (TEXT): Descrição da configuração
- `updated_at` (TIMESTAMP): Última atualização

**Índices:**
- PRIMARY KEY: `key`

**Exemplos:**
- key='claude-code.api.token', value='sk-ant-...', is_sensitive=true
- key='collection.retention_days', value='365', is_sensitive=false
- key='report.default_format', value='markdown', is_sensitive=false

---

### Relacionamentos

1. **User ↔ UserToolSnapshot** (1:N)
   - Um usuário pode ter múltiplos snapshots (histórico em diferentes coletas)
   - FK: UserToolSnapshot.user_id -> User.id

2. **Tool ↔ UserToolSnapshot** (1:N)
   - Uma ferramenta tem múltiplos snapshots de usuários
   - FK: UserToolSnapshot.tool_id -> Tool.id

3. **CollectionRun ↔ UserToolSnapshot** (1:N)
   - Uma coleta gera múltiplos snapshots
   - FK: UserToolSnapshot.collection_run_id -> CollectionRun.id

---

### Views Úteis

**view_current_user_tools:**
Mostra o estado atual (última coleta) de cada usuário em cada ferramenta.
```sql
SELECT u.email, u.primary_name, t.display_name as tool,
       uts.status, uts.last_activity_at
FROM User u
JOIN UserToolSnapshot uts ON u.id = uts.user_id
JOIN Tool t ON uts.tool_id = t.id
JOIN CollectionRun cr ON uts.collection_run_id = cr.id
WHERE cr.id = (SELECT MAX(id) FROM CollectionRun WHERE status = 'completed')
```

**view_user_overlap:**
Mostra usuários presentes em múltiplas ferramentas.
```sql
SELECT u.email, u.primary_name,
       COUNT(DISTINCT uts.tool_id) as tool_count,
       GROUP_CONCAT(t.display_name) as tools
FROM User u
JOIN UserToolSnapshot uts ON u.id = uts.user_id
JOIN Tool t ON uts.tool_id = t.id
WHERE uts.collection_run_id = (SELECT MAX(id) FROM CollectionRun WHERE status = 'completed')
GROUP BY u.id
HAVING tool_count > 1
```

## 8. Interfaces

### 8.1 Interface de Linha de Comando (CLI)

O sistema é operado via Spring Shell, que oferece dois modos de operação:

**Modo Interativo (Shell):**
```bash
java -jar ai-control.jar
ai-control:> help
ai-control:> collect --all
ai-control:> report --type summary
ai-control:> exit
```

**Modo Non-Interactive (Comando Único):**
```bash
java -jar ai-control.jar <comando> [opções] [argumentos]
java -jar ai-control.jar collect --all
java -jar ai-control.jar report --type summary --format csv
```

**Vantagens do Spring Shell:**
- Auto-complete com TAB
- Histórico de comandos (setas ↑ ↓)
- Help integrado e contextual
- Validação automática de parâmetros
- Integração nativa com Spring Boot

#### Comandos Disponíveis

**1. Configuração e Setup**
```bash
# Inicializar configuração
ai-control config init

# Definir valores de configuração
ai-control config set <chave> <valor>
ai-control config set claude-code.api.token sk-ant-xxx
ai-control config set github.api.token ghp_xxx

# Listar configurações (oculta valores sensíveis)
ai-control config list

# Testar conectividade
ai-control test-connection --all
ai-control test-connection --tool claude-code
```

**2. Coleta de Dados**
```bash
# Coletar de todas as ferramentas
ai-control collect

# Coletar de ferramenta específica
ai-control collect --tool claude-code
ai-control collect --tool github-copilot
ai-control collect --tool cursor

# Coletar múltiplas ferramentas específicas
ai-control collect --tools claude-code,copilot

# Modo silencioso (para cron)
ai-control collect --silent

# Modo verbose (debug)
ai-control collect --verbose
```

**3. Geração de Relatórios**
```bash
# Relatório resumo (padrão)
ai-control report --type summary
ai-control report --type summary --format md
ai-control report --type summary --format csv
ai-control report --type summary --format json

# Relatório de overlap (usuários em múltiplas ferramentas)
ai-control report --type overlap --format csv

# Relatório de ferramenta específica
ai-control report --type tool-detail --tool claude-code

# Relatório com período específico
ai-control report --type summary --from 2026-01-01 --to 2026-01-22

# Salvar relatório em arquivo específico
ai-control report --type summary --output /path/to/report.md
```

**4. Histórico e Auditoria**
```bash
# Listar histórico de coletas
ai-control history

# Listar últimas N coletas
ai-control history --limit 10

# Detalhes de coleta específica
ai-control history --id 42

# Limpar dados antigos
ai-control cleanup --older-than 365d
```

**5. Utilitários**
```bash
# Ajuda geral
ai-control --help
ai-control -h

# Ajuda de comando específico
ai-control collect --help

# Versão do sistema
ai-control --version
ai-control -v

# Validar integridade do banco de dados
ai-control db validate

# Exportar/importar configuração
ai-control config export --output config.json
ai-control config import --input config.json
```

#### Códigos de Saída
- `0`: Sucesso
- `1`: Erro geral
- `2`: Erro de configuração (credenciais inválidas, arquivo não encontrado)
- `3`: Erro de conectividade (falha ao acessar APIs)
- `4`: Erro de validação (dados inválidos)
- `5`: Falha parcial (algumas ferramentas falharam, outras sucederam)

#### Variáveis de Ambiente
```bash
# Credenciais (alternativa ao arquivo de config)
export AI_CONTROL_CLAUDE_TOKEN="sk-ant-xxx"
export AI_CONTROL_GITHUB_TOKEN="ghp_xxx"
export AI_CONTROL_CURSOR_TOKEN="cur_xxx"

# Configurações gerais
export AI_CONTROL_CONFIG_PATH="/etc/ai-control/config.json"
export AI_CONTROL_DB_PATH="/var/lib/ai-control/database.db"
export AI_CONTROL_LOG_LEVEL="INFO"  # DEBUG, INFO, WARN, ERROR
export AI_CONTROL_LOG_FILE="/var/log/ai-control/app.log"

# Spring Boot profiles
export SPRING_PROFILES_ACTIVE="prod"  # dev, test, prod
```

#### Exemplo de Implementação de Comando (Spring Shell)

```java
@ShellComponent
@ShellCommandGroup("Data Collection")
public class CollectCommands {

    private final CollectionService collectionService;

    public CollectCommands(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @ShellMethod(value = "Collect data from all AI tools", key = "collect")
    public String collect(
        @ShellOption(defaultValue = "false", help = "Silent mode") boolean silent,
        @ShellOption(defaultValue = "false", help = "Verbose mode") boolean verbose,
        @ShellOption(defaultValue = "", help = "Specific tool to collect") String tool
    ) {
        try {
            CollectionResult result = collectionService.collectAll(silent, verbose, tool);
            return formatCollectionResult(result);
        } catch (Exception e) {
            return "Error during collection: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Test connection to AI tool APIs", key = "test-connection")
    public String testConnection(
        @ShellOption(defaultValue = "false", help = "Test all tools") boolean all,
        @ShellOption(defaultValue = "", help = "Specific tool to test") String tool
    ) {
        // Implementation
        return "Connection test results...";
    }
}
```

**Exemplo de Service Layer (Spring):**
```java
@Service
public class CollectionService {

    private final UserRepository userRepository;
    private final ToolRepository toolRepository;
    private final CollectionRunRepository collectionRunRepository;
    private final UserToolSnapshotRepository snapshotRepository;
    private final List<ToolApiClient> apiClients;

    public CollectionService(
            UserRepository userRepository,
            ToolRepository toolRepository,
            CollectionRunRepository collectionRunRepository,
            UserToolSnapshotRepository snapshotRepository,
            List<ToolApiClient> apiClients) {
        this.userRepository = userRepository;
        this.toolRepository = toolRepository;
        this.collectionRunRepository = collectionRunRepository;
        this.snapshotRepository = snapshotRepository;
        this.apiClients = apiClients;
    }

    @Transactional
    public CollectionResult collectAll(boolean silent, boolean verbose, String toolFilter) {
        CollectionRun run = createCollectionRun();

        try {
            for (ToolApiClient client : apiClients) {
                if (shouldCollect(client, toolFilter)) {
                    collectFromTool(client, run, verbose);
                }
            }

            run.setStatus("completed");
            run.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            run.setStatus("failed");
            run.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            collectionRunRepository.save(run);
        }

        return buildResult(run);
    }

    private void collectFromTool(ToolApiClient client, CollectionRun run, boolean verbose) {
        // Implementation
    }
}
```

**Exemplo de API Client (usando WebClient):**
```java
@Component
public class ClaudeApiClient implements ToolApiClient {

    private final WebClient webClient;
    private final AiControlProperties properties;

    public ClaudeApiClient(WebClient.Builder webClientBuilder,
                          AiControlProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getApi().getClaude().getBaseUrl())
            .defaultHeader("X-API-Key", properties.getApi().getClaude().getToken())
            .build();
    }

    @Override
    public List<UserData> fetchUsers() {
        return webClient.get()
            .uri("/v1/organizations/{orgId}/users", "org_123")
            .retrieve()
            .bodyToMono(ClaudeUsersResponse.class)
            .map(response -> response.getUsers())
            .block();
    }

    @Override
    public String getToolName() {
        return "claude-code";
    }
}
```

---

### 8.2 APIs e Integrações Externas

O sistema integra-se com APIs de três ferramentas de IA. Abaixo estão os requisitos e endpoints esperados.

#### 1. Claude Code (Anthropic API)

**Autenticação:**
- Header: `X-API-Key: <token>`
- Token obtido em: https://console.anthropic.com/

**Endpoints Utilizados:**
```
GET /v1/organizations/{org_id}/users
```

**Response Esperado:**
```json
{
  "users": [
    {
      "id": "user_xxx",
      "email": "user@example.com",
      "name": "John Doe",
      "status": "active",
      "last_active_at": "2026-01-22T10:30:00Z",
      "created_at": "2025-06-01T00:00:00Z"
    }
  ],
  "total": 42
}
```

**Rate Limits:**
- 100 requests/minuto
- Header de resposta: `X-RateLimit-Remaining`

**Tratamento de Erros:**
- 401: Token inválido
- 403: Sem permissão para acessar organização
- 429: Rate limit excedido (retry após header `Retry-After`)

---

#### 2. GitHub Copilot (GitHub API)

**Autenticação:**
- Header: `Authorization: Bearer <token>`
- Token (PAT) obtido em: https://github.com/settings/tokens
- Scopes necessários: `read:org`, `read:user`

**Endpoints Utilizados:**
```
GET /orgs/{org}/copilot/billing/seats
```

**Response Esperado:**
```json
{
  "total_seats": 50,
  "seats": [
    {
      "assignee": {
        "login": "johndoe",
        "email": "john@example.com",
        "name": "John Doe"
      },
      "created_at": "2025-05-01T00:00:00Z",
      "last_activity_at": "2026-01-20T14:22:00Z",
      "last_activity_editor": "vscode"
    }
  ]
}
```

**Rate Limits:**
- 5000 requests/hora (autenticado)
- Headers de resposta: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

**Tratamento de Erros:**
- 401: Token inválido ou expirado
- 403: Sem permissão para acessar organização
- 404: Organização não encontrada ou sem Copilot
- 429: Rate limit excedido

---

#### 3. Cursor API

**Nota:** Cursor pode não ter API pública oficial. Alternativas:
- **Opção A:** Uso de dashboard web scraping (não recomendado - frágil)
- **Opção B:** Integração via SSO logs se disponível
- **Opção C:** Export manual de CSV e import via comando CLI

**Autenticação (se API existir):**
- Header: `Authorization: Bearer <token>`
- Token obtido no dashboard administrativo

**Endpoint Hipotético:**
```
GET /api/v1/team/members
```

**Response Esperado:**
```json
{
  "members": [
    {
      "id": "mem_xxx",
      "email": "user@example.com",
      "name": "John Doe",
      "status": "active",
      "last_seen": "2026-01-21T18:45:00Z",
      "joined_at": "2025-07-01T00:00:00Z"
    }
  ]
}
```

**Alternativa - Import Manual:**
```bash
# Se API não disponível, permitir import de CSV
ai-control import --tool cursor --file cursor-users.csv
```

Formato CSV esperado:
```csv
email,name,status,last_active
john@example.com,John Doe,active,2026-01-21
```

---

### 8.3 Persistência de Dados

**Banco de Dados:**
- SQLite para simplicidade e portabilidade
- Arquivo padrão: `~/.ai-control/database.db`
- Versionamento de schema via migrations

**ORM e Acesso a Dados:**
- **Spring Data JPA** para abstração de acesso a dados
- **Hibernate** como provider JPA (com dialect customizado para SQLite)
- **Repositories** seguindo padrão Spring Data

**Exemplo de Configuração Tipada (@ConfigurationProperties):**
```java
@Configuration
@ConfigurationProperties(prefix = "ai-control")
@Validated
public class AiControlProperties {

    private ApiConfig api = new ApiConfig();
    private CollectionConfig collection = new CollectionConfig();
    private ReportsConfig reports = new ReportsConfig();

    // Getters e Setters

    public ApiConfig getApi() {
        return api;
    }

    public void setApi(ApiConfig api) {
        this.api = api;
    }

    public CollectionConfig getCollection() {
        return collection;
    }

    public void setCollection(CollectionConfig collection) {
        this.collection = collection;
    }

    public ReportsConfig getReports() {
        return reports;
    }

    public void setReports(ReportsConfig reports) {
        this.reports = reports;
    }

    public static class ApiConfig {
        private ClaudeConfig claude = new ClaudeConfig();
        private GitHubConfig github = new GitHubConfig();
        private CursorConfig cursor = new CursorConfig();

        // Getters e Setters
    }

    public static class ClaudeConfig {
        @NotBlank(message = "Claude API base URL is required")
        private String baseUrl;

        @NotBlank(message = "Claude API token is required")
        private String token;

        private int timeout = 30000;

        // Getters e Setters
    }

    // Classes GitHubConfig, CursorConfig, CollectionConfig, ReportsConfig similares...
}
```

**Exemplo de Repository (Spring Data JPA):**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByIsActiveTrue();

    @Query("SELECT u FROM User u WHERE u.lastSeenAt >= :since")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);
}

@Repository
public interface UserToolSnapshotRepository extends JpaRepository<UserToolSnapshot, Long> {

    List<UserToolSnapshot> findByCollectionRunId(Long collectionRunId);

    @Query("SELECT uts FROM UserToolSnapshot uts " +
           "WHERE uts.userId = :userId AND uts.collectionRunId = :runId")
    List<UserToolSnapshot> findByUserAndRun(@Param("userId") Long userId,
                                             @Param("runId") Long runId);
}
```

**Configuração Principal (application.yml):**
```yaml
spring:
  application:
    name: ai-user-control

  datasource:
    url: jdbc:sqlite:${AI_CONTROL_DB_PATH:${user.home}/.ai-control/database.db}
    driver-class-name: org.sqlite.JDBC

  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate  # Nunca usar 'update' em produção
    show-sql: false
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  shell:
    interactive:
      enabled: true
    noninteractive:
      enabled: true
    script:
      enabled: false

logging:
  level:
    root: INFO
    com.bemobi.aicontrol: ${AI_CONTROL_LOG_LEVEL:INFO}
    org.hibernate.SQL: ${AI_CONTROL_SQL_LOG:WARN}
  file:
    name: ${AI_CONTROL_LOG_FILE:${user.home}/.ai-control/logs/application.log}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%level] [%logger{36}] - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%level] [%logger{36}] - %msg%n"

# Configurações customizadas da aplicação
ai-control:
  api:
    claude:
      base-url: https://api.anthropic.com
      token: ${AI_CONTROL_CLAUDE_TOKEN:}
      timeout: 30000
    github:
      base-url: https://api.github.com
      token: ${AI_CONTROL_GITHUB_TOKEN:}
      org: ${AI_CONTROL_GITHUB_ORG:}
      timeout: 30000
    cursor:
      base-url: https://api.cursor.sh
      token: ${AI_CONTROL_CURSOR_TOKEN:}
      timeout: 30000

  collection:
    retention-days: 365
    retry-attempts: 3
    retry-backoff-ms: 1000
    active-user-days: 30

  reports:
    default-format: markdown
    output-dir: ${user.home}/.ai-control/reports
```

**Perfil de Produção (application-prod.yml):**
```yaml
spring:
  jpa:
    show-sql: false

logging:
  level:
    root: WARN
    com.bemobi.aicontrol: INFO

ai-control:
  reports:
    output-dir: /var/lib/ai-control/reports
```

**Schema Migrations:**
- Ferramenta: **Flyway** (integração nativa com Spring Boot)
- Migrations em: `src/main/resources/db/migration/`
- Formato: `V001__initial_schema.sql`, `V002__add_usage_metrics.sql`
- Execução automática no startup da aplicação

**Backup:**
```bash
# Backup manual
ai-control db backup --output /path/to/backup.db

# Restore
ai-control db restore --input /path/to/backup.db
```

---

### 8.4 Logging e Observabilidade

**Framework de Logging:**
- **SLF4J** como facade (abstração)
- **Logback** como implementação (incluído no Spring Boot)
- Configuração via `application.yml` ou `logback-spring.xml`

**Formato de Logs (padrão):**
```
[TIMESTAMP] [LEVEL] [COMPONENT] Message
[2026-01-22 10:30:15] [INFO] [ClaudeCodeClient] Starting user collection
[2026-01-22 10:30:18] [ERROR] [GitHubClient] Rate limit exceeded, retrying in 60s
```

**Exemplo de Uso no Código:**
```java
@Service
@Slf4j  // Se usar Lombok para logs (opcional)
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    public void collectData() {
        log.info("Starting data collection");
        log.debug("Collection configuration: {}", config);

        try {
            // logic
            log.info("Collection completed successfully. Users processed: {}", count);
        } catch (Exception e) {
            log.error("Collection failed: {}", e.getMessage(), e);
        }
    }
}
```

**Logs Estruturados (JSON - via Logstash Encoder):**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```json
{
  "timestamp": "2026-01-22T10:30:15.123Z",
  "level": "INFO",
  "logger": "com.bemobi.aicontrol.service.CollectionService",
  "message": "Starting user collection",
  "thread": "main",
  "context": {
    "collection_run_id": 42,
    "tool": "claude-code"
  }
}
```

**Destinos de Log:**
- Console (stdout/stderr) - padrão do Spring Boot
- Arquivo: `${user.home}/.ai-control/logs/application.log`
- Rotação automática via Logback (diária ou por tamanho)
- Syslog (opcional, configurável)

**Spring Boot Actuator (Opcional):**
- Endpoint `/actuator/health` para health check
- Endpoint `/actuator/metrics` para métricas
- Endpoint `/actuator/loggers` para alterar nível de log em runtime

---

## 9. Dependências e Requisitos de Ambiente

### 9.1 Requisitos de Sistema
- **Java:** 17 LTS ou superior (requerido pelo Spring Boot 3.x)
- **Sistema Operacional:** Linux, macOS, Windows (com WSL)
- **Memória:** Mínimo 512MB RAM (recomendado 1GB para Spring Boot)
- **Disco:** Mínimo 150MB para aplicação + dados
- **Rede:** Acesso HTTPS às APIs (portas 443)

### 9.2 Dependências Java (Spring Boot Ecosystem)

**Framework Base:**
- **Spring Boot:** 3.2.x ou superior
- **Java Version:** 17 LTS (compatível com Spring Boot 3.x)

**Starters e Módulos Spring:**
- **spring-boot-starter:** Core do Spring Boot
- **spring-boot-starter-data-jpa:** Para persistência e ORM
- **spring-shell-starter:** Framework CLI interativo do Spring
- **spring-boot-starter-validation:** Validação de dados
- **spring-boot-starter-actuator:** Métricas e health checks (opcional)
- **spring-boot-starter-logging:** SLF4J + Logback (incluído por padrão)

**Bibliotecas Adicionais:**
- **HTTP Client:** Spring WebClient (reativo) ou RestTemplate
- **JSON:** Jackson (incluído no Spring Boot)
- **Database:** SQLite JDBC driver (`org.xerial:sqlite-jdbc`)
- **Database Dialect:** `org.hibernate.community:hibernate-community-dialects` (para SQLite)
- **Testing:**
  - JUnit 5 (Jupiter)
  - Spring Boot Test (`spring-boot-starter-test`)
  - Mockito
  - AssertJ
  - TestContainers (opcional, para testes de integração)

**Utilitários:**
- **Lombok:** ❌ NÃO usar (conforme CLAUDE.md - código explícito preferido)
- **MapStruct:** Para mapeamento entre entidades e DTOs (opcional)
- **Apache Commons Lang3:** Utilitários de string e validação

### 9.3 Build e Distribuição

**Build Tool:**
- **Maven**: Com Spring Boot Maven Plugin (padrão do projeto)

**Estrutura do Projeto (Maven):**
```
ai-user-control/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/aicontrol/
│   │   │       ├── AiUserControlApplication.java (main)
│   │   │       ├── command/           (Spring Shell commands)
│   │   │       ├── service/           (business logic)
│   │   │       ├── repository/        (Spring Data JPA)
│   │   │       ├── entity/            (JPA entities)
│   │   │       ├── client/            (API clients)
│   │   │       ├── config/            (Spring configuration)
│   │   │       └── util/              (utilities)
│   │   └── resources/
│   │       ├── application.yml        (Spring Boot config)
│   │       ├── application-prod.yml
│   │       ├── db/migration/          (Flyway migrations)
│   │       └── banner.txt             (CLI banner)
│   └── test/
│       ├── java/
│       └── resources/
├── Dockerfile
└── README.md
```

**Empacotamento:**
- **Spring Boot Executable JAR:** JAR com todas as dependências embarcadas
- Gerado via: `mvn clean package`
- Resultado: `target/ai-user-control-0.0.1.jar`

**Comandos Maven Principais:**
```bash
# Build completo (compile + test + package)
mvn clean package

# Build sem executar testes
mvn clean package -DskipTests

# Executar testes
mvn test

# Executar aplicação em modo dev
mvn spring-boot:run

# Instalar no repositório local
mvn clean install
```

**Distribuição:**

1. **JAR Executável Direto:**
   ```bash
   java -jar ai-user-control-0.0.1.jar
   ```

2. **Script Wrapper** (`/usr/local/bin/ai-control`):
   ```bash
   #!/bin/bash
   java -jar /opt/ai-control/ai-user-control.jar "$@"
   ```

3. **Spring Boot CLI Mode:**
   ```bash
   # Modo interativo (shell)
   java -jar ai-control.jar

   # Modo comando único (non-interactive)
   java -jar ai-control.jar collect --all
   java -jar ai-control.jar report --type summary
   ```

4. **Container Docker:**
   ```dockerfile
   FROM eclipse-temurin:17-jre-alpine
   WORKDIR /app
   COPY target/ai-user-control.jar app.jar
   ENTRYPOINT ["java", "-jar", "app.jar"]
   CMD ["--help"]
   ```

   Uso:
   ```bash
   docker build -t ai-control:latest .
   docker run -v ~/.ai-control:/root/.ai-control ai-control:latest collect
   ```

5. **Native Image (opcional - GraalVM):**
   - Compilação nativa com Spring Native
   - Startup ultra-rápido
   - Binário executável sem JVM

**Exemplo de pom.xml:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
        <relativePath/>
    </parent>

    <groupId>com.bemobi</groupId>
    <artifactId>ai-user-control</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>AI User Control</name>
    <description>AI Tools User Management and Analytics</description>

    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Shell -->
        <dependency>
            <groupId>org.springframework.shell</groupId>
            <artifactId>spring-shell-starter</artifactId>
            <version>3.2.1</version>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.45.0.0</version>
        </dependency>

        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-community-dialects</artifactId>
        </dependency>

        <!-- Flyway for migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- HTTP Client -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <executable>true</executable>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 10. Glossário

- **Overlap:** Usuários que aparecem em múltiplas ferramentas simultaneamente
- **Snapshot:** Estado de um usuário em uma ferramenta em um ponto no tempo específico
- **Collection Run:** Execução completa de coleta de dados de uma ou mais ferramentas
- **Active User:** Usuário com atividade registrada nos últimos 30 dias (configurável)
- **Rate Limiting:** Limitação de número de requisições às APIs em período de tempo
- **Retry com Backoff:** Tentativa automática de reexecutar operação após falha, com intervalos crescentes

---

> *Generated by Claude Code*
