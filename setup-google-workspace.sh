#!/bin/bash

# Script Simples: Configurar Google Workspace
# Foca apenas nas credenciais do Google Workspace

clear
echo "================================================"
echo "  Configuração Google Workspace"
echo "================================================"
echo ""

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Criar diretório seguro para credenciais
CREDS_DIR="$HOME/.ai-control"
mkdir -p "$CREDS_DIR"
chmod 700 "$CREDS_DIR"

echo "Este script vai configurar apenas o Google Workspace."
echo ""

# Passo 1: Arquivo JSON
echo -e "${YELLOW}1️⃣  Arquivo JSON da Service Account${NC}"
echo ""
echo "Onde está o arquivo JSON baixado do Google Cloud Console?"
echo "(Pressione Enter para usar: ~/Downloads/service-account-key.json)"
echo ""
read -p "Caminho: " JSON_PATH

# Usar padrão se vazio
if [ -z "$JSON_PATH" ]; then
    JSON_PATH="$HOME/Downloads/service-account-key.json"
fi

# Expandir ~
JSON_PATH="${JSON_PATH/#\~/$HOME}"

# Verificar se existe
if [ ! -f "$JSON_PATH" ]; then
    echo -e "${RED}❌ Arquivo não encontrado: $JSON_PATH${NC}"
    echo ""
    echo "Baixe o arquivo JSON da Service Account:"
    echo "1. Acesse https://console.cloud.google.com/"
    echo "2. IAM & Admin → Service Accounts"
    echo "3. Clique na service account → Keys → Add Key → Create new key → JSON"
    exit 1
fi

# Verificar se é JSON válido
if ! python3 -m json.tool "$JSON_PATH" > /dev/null 2>&1 && ! cat "$JSON_PATH" | jq . > /dev/null 2>&1; then
    echo -e "${RED}❌ Arquivo não é um JSON válido${NC}"
    exit 1
fi

# Copiar para local seguro
TARGET_FILE="$CREDS_DIR/google-workspace-key.json"
cp "$JSON_PATH" "$TARGET_FILE"
chmod 600 "$TARGET_FILE"
echo -e "${GREEN}✓${NC} Arquivo copiado para: $TARGET_FILE"
echo ""

# Passo 2: Domínio
echo -e "${YELLOW}2️⃣  Domínio do Google Workspace${NC}"
echo ""
echo "Qual o domínio da sua empresa?"
echo "Exemplo: suaempresa.com"
echo ""
read -p "Domínio: " DOMAIN

if [ -z "$DOMAIN" ]; then
    echo -e "${RED}❌ Domínio é obrigatório${NC}"
    exit 1
fi

echo ""

# Passo 3: Email Admin
echo -e "${YELLOW}3️⃣  Email do Super Admin${NC}"
echo ""
echo "Email de um Super Admin do Google Workspace?"
echo "Exemplo: admin@$DOMAIN"
echo ""
read -p "Email: " ADMIN_EMAIL

if [ -z "$ADMIN_EMAIL" ]; then
    echo -e "${RED}❌ Email do admin é obrigatório${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}4️⃣  Salvando configuração...${NC}"
echo ""

# Criar arquivo .env se não existir
ENV_FILE=".env"
if [ ! -f "$ENV_FILE" ]; then
    touch "$ENV_FILE"
fi

# Remover configurações antigas do Workspace (se existirem)
sed -i.bak '/AI_CONTROL_WORKSPACE/d' "$ENV_FILE" 2>/dev/null || true

# Adicionar configurações do Google Workspace
cat >> "$ENV_FILE" << EOF

# ================================================
# Google Workspace Configuration
# Configurado em: $(date '+%Y-%m-%d %H:%M:%S')
# ================================================
export AI_CONTROL_WORKSPACE_ENABLED=true
export AI_CONTROL_WORKSPACE_CREDENTIALS="$TARGET_FILE"
export AI_CONTROL_WORKSPACE_DOMAIN="$DOMAIN"
export AI_CONTROL_WORKSPACE_ADMIN_EMAIL="$ADMIN_EMAIL"
export AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA=custom
export AI_CONTROL_WORKSPACE_GIT_FIELD=git_name
EOF

echo -e "${GREEN}✓${NC} Configuração salva em $ENV_FILE"
echo ""

# Carregar variáveis
source "$ENV_FILE"

# Resumo
echo "================================================"
echo -e "${GREEN}✅ Configuração concluída!${NC}"
echo "================================================"
echo ""
echo "📋 Configurado:"
echo "   • Credenciais: $TARGET_FILE"
echo "   • Domínio: $DOMAIN"
echo "   • Admin: $ADMIN_EMAIL"
echo ""
echo "📝 Próximos passos:"
echo ""
echo "1. Carregue as variáveis de ambiente:"
echo -e "   ${YELLOW}source .env${NC}"
echo ""
echo "2. Configure o custom schema no Workspace Admin Console:"
echo "   https://admin.google.com/"
echo "   → Directory → Users → Manage custom attributes"
echo "   → Adicione campo 'git_name' na categoria 'custom'"
echo ""
echo "3. Preencha o campo 'git_name' para cada usuário:"
echo "   → Use o login exato do GitHub (ex: johndoe)"
echo ""
echo "4. Teste a configuração:"
echo -e "   ${YELLOW}./test-integration.sh${NC}"
echo ""
