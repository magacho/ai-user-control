# ADR-001: Escolha do Spring Boot como Framework Base

**Status:** Accepted
**Date:** 2026-01-22
**Deciders:** @Architect, @Dev, @DevOps

## Context

O projeto AI User Control necessita de um framework robusto para implementar uma aplicação CLI em Java que colete dados de APIs externas, processe informações, persista em banco de dados e gere relatórios. A escolha do framework impacta diretamente a produtividade, manutenibilidade e facilidade de testes.

### Requisitos Técnicos
- Aplicação CLI (não web)
- Integração com múltiplas APIs REST
- Persistência de dados
- Sistema de configuração flexível
- Logging estruturado
- Facilidade de testes
- Suporte a agendamento e automação

## Decision

**Escolhemos Spring Boot 3.5.10 com Java 21 LTS como framework base do projeto.**

### Componentes Principais
- **Spring Boot Starter** - Core framework
- **Spring Shell** - Interface CLI (modo nao-interativo)
- **Spring WebFlux** - Cliente HTTP reativo (WebClient)
- **Apache Commons CSV** - Exportacao de dados

### Configuracao
```yaml
spring:
  application:
    name: ai-user-control
  main:
    web-application-type: none
  shell:
    interactive:
      enabled: false
    noninteractive:
      enabled: true
```

## Consequences

### Positive
- ✅ **Ecossistema Maduro:** Spring Boot é amplamente adotado com vasta documentação
- ✅ **Injeção de Dependências:** Gerenciamento automático de beans e dependências
- ✅ **Spring Shell:** CLI nao-interativa para execucao automatizada
- ✅ **Configuração Centralizada:** `application.yml` + profiles + `@ConfigurationProperties`
- ✅ **Testing Framework:** Suporte completo a testes unitários e de integração
- ✅ **Observabilidade:** Actuator fornece métricas e health checks prontos
- ✅ **WebClient Reativo:** Cliente HTTP moderno com retry e backoff
- ✅ **Comunidade Ativa:** Facil encontrar solucoes e boas praticas

### Negative
- ⚠️ **Tamanho do JAR:** Spring Boot gera JARs maiores (~40-60MB)
- ⚠️ **Tempo de Startup:** ~2-3 segundos (aceitável para CLI)
- ⚠️ **Overhead de Memória:** ~200-300MB RAM (mais que CLI puro)
- ⚠️ **Curva de Aprendizado:** Requer conhecimento do ecossistema Spring
- ⚠️ **Complexidade:** Pode ser "over-engineering" para CLIs muito simples

## Alternatives Considered

### 1. Picocli (CLI puro)
**Descrição:** Framework leve focado apenas em CLI
**Rejeitado porque:**
- Não fornece injeção de dependências nativa
- Sem suporte integrado para JPA/persistência
- Requer integração manual de componentes (HTTP client, logging, etc)
- Mais código boilerplate necessário
- Menos suporte a configuração centralizada

### 2. Quarkus
**Descrição:** Framework cloud-native otimizado para GraalVM
**Rejeitado porque:**
- Mais adequado para microservices e aplicações web
- Menor maturidade que Spring Boot
- Comunidade menor
- CLI não é o foco principal do framework
- Team tem mais experiência com Spring

### 3. Micronaut
**Descrição:** Framework moderno com compilação em tempo de build
**Rejeitado porque:**
- Menor adoção no mercado
- Documentação menos abrangente
- Menos integrações prontas
- Team tem mais experiência com Spring

## Implementation Notes

### Estrutura do Projeto
```
ai-user-control/
├── pom.xml
├── src/main/
│   ├── java/com/bemobi/aicontrol/
│   │   ├── AiUserControlApplication.java    # @SpringBootApplication
│   │   ├── config/                          # @Configuration
│   │   ├── integration/                     # API Clients (Strategy Pattern)
│   │   │   ├── ToolApiClient.java           # Interface base
│   │   │   ├── common/                      # DTOs comuns (UserData, etc.)
│   │   │   ├── claude/                      # Anthropic Admin API
│   │   │   ├── github/                      # GitHub Copilot API
│   │   │   ├── cursor/                      # Cursor Admin API + CSV
│   │   │   └── google/                      # Google Workspace (email)
│   │   ├── service/                         # @Service (logica de negocio)
│   │   └── runner/                          # CommandLineRunner
│   └── resources/
│       └── application.yml
```

### Application Main Class
```java
@SpringBootApplication
@EnableConfigurationProperties
public class AiUserControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiUserControlApplication.class, args);
    }
}
```

### Exemplo de Configuração Tipada
```java
@Configuration
@ConfigurationProperties(prefix = "ai-control")
@Validated
public class AiControlProperties {
    private ApiConfig api;
    private CollectionConfig collection;
    private ReportsConfig reports;

    // Getters e Setters explícitos (sem Lombok)
}
```

### Build e Execucao
```bash
# Build
mvn clean install

# Executar (coleta e exporta CSVs automaticamente)
mvn spring-boot:run
```

## Related ADRs
- ADR-002: Escolha do Banco de Dados (SQLite) — **Superseded**
- ADR-003: Arquitetura em Camadas — **Superseded**
- ADR-004: Padrao de Integracao com APIs Externas

## References
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Shell Documentation: https://spring.io/projects/spring-shell
- Requisitos.md - Seção 9.2 (Dependências Java)

---

> *Generated by Claude Code - @Architect*
