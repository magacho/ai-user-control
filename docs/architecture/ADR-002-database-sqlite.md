# ADR-002: Escolha do SQLite como Banco de Dados

**Status:** Accepted
**Date:** 2026-01-22
**Deciders:** @Architect, @Dev, @DevOps

## Context

O sistema precisa armazenar dados coletados das APIs, incluindo usuários, ferramentas, snapshots de coletas e histórico. Os dados são principalmente relacionais com necessidade de consultas JOIN, agregações e histórico temporal.

### Requisitos de Persistência
- Armazenar usuários e suas associações com ferramentas
- Manter histórico de coletas (12 meses - RN-003)
- Suportar consultas complexas (JOINs, agregações)
- Volume estimado: ~1000 usuários, ~365 coletas/ano
- Simplicidade operacional (sem servidor dedicado)
- Portabilidade (backup via cópia de arquivo)
- Deploy simples (aplicação CLI standalone)

### Características dos Dados
- Leitura > Escrita (coletas periódicas, relatórios frequentes)
- Sem necessidade de concorrência alta
- Volume moderado (~100MB/ano)
- Relacionamentos claros (User ↔ Tool via Snapshot)

## Decision

**Escolhemos SQLite como banco de dados relacional embarcado.**

### Configuração
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${AI_CONTROL_DB_PATH:${user.home}/.ai-control/database.db}
    driver-class-name: org.sqlite.JDBC

  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate
    show-sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Dependências Maven
```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.0.0</version>
</dependency>

<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-community-dialects</artifactId>
</dependency>
```

## Consequences

### Positive
- ✅ **Zero Configuração:** Sem servidor de BD para instalar/configurar
- ✅ **Portabilidade:** Banco é um único arquivo, fácil backup/restore
- ✅ **Simplicidade:** Deploy trivial (aplicação standalone)
- ✅ **Performance:** Rápido para leituras (principal caso de uso)
- ✅ **ACID Compliant:** Transações confiáveis
- ✅ **SQL Completo:** Suporta JOINs, agregações, índices
- ✅ **Footprint Pequeno:** ~1MB de biblioteca
- ✅ **Sem Custo Operacional:** Sem servidor para manter
- ✅ **Backup Simples:** `cp database.db backup.db`
- ✅ **Desenvolvimento Local:** Mesmo BD em dev/prod

### Negative
- ⚠️ **Concorrência Limitada:** Um writer por vez (não é problema para CLI)
- ⚠️ **Escalabilidade Limitada:** Não adequado para milhões de registros
- ⚠️ **Sem Replicação Nativa:** Backup manual necessário
- ⚠️ **Sem Usuários/Permissões:** Segurança via filesystem
- ⚠️ **Dialeto Hibernate:** Precisa de `hibernate-community-dialects`
- ⚠️ **Tipos Limitados:** Sem JSONB nativo (usar TEXT + conversão)

## Alternatives Considered

### 1. PostgreSQL
**Descrição:** Banco relacional robusto e completo
**Rejeitado porque:**
- Requer servidor dedicado (complexidade operacional)
- Overkill para volume de dados esperado
- Dificulta deploy standalone da aplicação CLI
- Backup mais complexo
- Configuração inicial mais trabalhosa

**Quando reconsiderar:**
- Volume > 10GB de dados
- Necessidade de múltiplos writers concorrentes
- Requisito de replicação automática

### 2. H2 Database
**Descrição:** Banco embarcado em memória/arquivo
**Rejeitado porque:**
- Menos maduro que SQLite
- Modo file tem quirks de compatibilidade
- SQLite tem melhor suporte da comunidade Hibernate
- H2 é mais usado para testes, não produção

**Vantagens sobre SQLite:**
- Modo in-memory mais rápido
- Melhor compatibilidade SQL padrão

### 3. MongoDB (NoSQL)
**Descrição:** Banco de documentos
**Rejeitado porque:**
- Dados são fortemente relacionais
- Necessidade de JOINs complexos
- Requer servidor dedicado
- Over-engineering para o caso de uso
- Team tem mais experiência com SQL

## Implementation Notes

### Estrutura de Diretórios
```
~/.ai-control/
├── database.db          # Banco principal
├── database.db-journal  # WAL do SQLite
├── logs/                # Logs da aplicação
└── reports/             # Relatórios gerados
```

### Schema Overview
```sql
-- Tabela principal de usuários
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    primary_name VARCHAR(255),
    first_seen_at TIMESTAMP,
    last_seen_at TIMESTAMP,
    is_active BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Ferramentas monitoradas
CREATE TABLE tools (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    is_enabled BOOLEAN DEFAULT 1
);

-- Snapshots de usuário/ferramenta em cada coleta
CREATE TABLE user_tool_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    tool_id INTEGER NOT NULL,
    collection_run_id INTEGER NOT NULL,
    name_in_tool VARCHAR(255),
    status VARCHAR(50),
    last_activity_at TIMESTAMP,
    usage_metrics TEXT,  -- JSON serializado
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (tool_id) REFERENCES tools(id),
    FOREIGN KEY (collection_run_id) REFERENCES collection_runs(id)
);

-- Execuções de coleta
CREATE TABLE collection_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    tools_collected VARCHAR(255),
    tools_failed VARCHAR(255),
    total_users_processed INTEGER,
    execution_time_ms INTEGER
);
```

### Migrations com Flyway

**V001__initial_schema.sql:**
```sql
-- Criar tabelas principais
-- Criar índices
-- Inserir dados iniciais (3 tools)
INSERT INTO tools (name, display_name, is_enabled) VALUES
    ('claude-code', 'Claude Code', 1),
    ('github-copilot', 'GitHub Copilot', 1),
    ('cursor', 'Cursor', 1);
```

### Backup e Restore

**Backup:**
```bash
# Via CLI
java -jar ai-control.jar db backup --output /backup/db-$(date +%Y%m%d).db

# Manual
cp ~/.ai-control/database.db /backup/
```

**Restore:**
```bash
# Via CLI
java -jar ai-control.jar db restore --input /backup/db-20260122.db

# Manual
cp /backup/db-20260122.db ~/.ai-control/database.db
```

### Performance Optimization

**Índices Recomendados:**
```sql
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_last_seen ON users(last_seen_at);
CREATE INDEX idx_snapshots_user ON user_tool_snapshots(user_id);
CREATE INDEX idx_snapshots_tool ON user_tool_snapshots(tool_id);
CREATE INDEX idx_snapshots_run ON user_tool_snapshots(collection_run_id);
CREATE INDEX idx_runs_started ON collection_runs(started_at);
```

**Configurações SQLite:**
```sql
PRAGMA journal_mode = WAL;           -- Write-Ahead Logging
PRAGMA synchronous = NORMAL;         -- Balance safety/performance
PRAGMA cache_size = -64000;          -- 64MB cache
PRAGMA temp_store = MEMORY;          -- Temp tables in memory
```

### Tamanho Estimado

Para 1000 usuários ao longo de 1 ano:
- Users: ~1000 registros × 500 bytes = 500KB
- Tools: 3 registros = 1KB
- UserToolSnapshots: ~1000 users × 2.5 tools × 365 days = ~900K registros × 300 bytes = ~270MB
- CollectionRuns: 365 registros = 50KB

**Total estimado:** ~270MB/ano (bem dentro das capacidades do SQLite)

## Monitoring and Maintenance

### Verificação de Integridade
```bash
java -jar ai-control.jar db validate
```

### Cleanup de Dados Antigos
```bash
# Remover dados > 12 meses (conforme RN-003)
java -jar ai-control.jar cleanup --older-than 365d
```

### Vacuum (otimização)
```sql
VACUUM;  -- Executar periodicamente para recuperar espaço
```

## Related ADRs
- ADR-001: Escolha do Spring Boot como Framework
- ADR-003: Arquitetura em Camadas (Repository Layer)

## References
- SQLite Documentation: https://www.sqlite.org/docs.html
- Hibernate Community Dialects: https://github.com/hibernate/hibernate-orm
- Requisitos.md - Seção 7 (Modelo de Dados)
- RN-003: Retenção de Dados Históricos

---

> *Generated by Claude Code - @Architect*
