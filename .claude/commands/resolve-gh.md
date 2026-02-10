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
