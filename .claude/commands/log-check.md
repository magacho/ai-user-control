---
description: Verifica se o arquivo atual segue o padrão de logs estruturados.
---
# Verificação de Logs

Analise o código atual focando APENAS em observabilidade:
1. Verifica se existe \`System.out.print\` (se sim, mande corrigir).
2. Verifica se está usando SLF4J.
3. Verifica se os logs de erro incluem contexto (variáveis chave) e não apenas a mensagem de erro.
4. Sugira melhorias para incluir MDC se for um ponto de entrada (Controller/Listener).
