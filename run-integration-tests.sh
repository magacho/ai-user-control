#!/bin/bash

# Script para executar testes de integração com APIs reais
# Este script configura as variáveis de ambiente necessárias e executa os testes

set -e

echo "=========================================="
echo "AI User Control - Integration Tests"
echo "=========================================="
echo ""

# Função para verificar se uma variável está definida
check_var() {
    if [ -z "${!1}" ]; then
        echo "⚠️  $1 não está definida"
        return 1
    else
        echo "✅ $1 está configurada"
        return 0
    fi
}

# Verificar configurações
echo "Verificando configurações..."
echo ""

# Claude Code
echo "Claude Code (Anthropic API):"
check_var "AI_CONTROL_CLAUDE_ENABLED" || true
check_var "AI_CONTROL_CLAUDE_TOKEN" || true
check_var "AI_CONTROL_CLAUDE_ORG_ID" || true
echo ""

# GitHub Copilot
echo "GitHub Copilot:"
check_var "AI_CONTROL_GITHUB_ENABLED" || true
check_var "AI_CONTROL_GITHUB_TOKEN" || true
check_var "AI_CONTROL_GITHUB_ORG" || true
echo ""

# Cursor
echo "Cursor (CSV):"
check_var "AI_CONTROL_CURSOR_ENABLED" || true
check_var "AI_CONTROL_CURSOR_CSV_PATH" || true
echo ""

echo "=========================================="
echo "Executando Testes de Integração..."
echo "=========================================="
echo ""

# Executar testes de integração
mvn verify -P integration-tests

echo ""
echo "=========================================="
echo "Testes de Integração Concluídos!"
echo "=========================================="
