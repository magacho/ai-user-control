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
