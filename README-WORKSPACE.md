# 🔐 Setup Google Workspace - Guia Simples

## O que você precisa ter em mãos:

1. **Arquivo JSON da Service Account**
   - Baixado do Google Cloud Console
   - Exemplo: `~/Downloads/service-account-key.json`

2. **Domínio da empresa**
   - Exemplo: `bemobi.com`

3. **Email de um Super Admin**
   - Exemplo: `admin@bemobi.com`

---

## 🚀 Como usar (1 comando)

```bash
./setup-google-workspace.sh
```

O script vai perguntar:

```
1️⃣  Arquivo JSON da Service Account
   Caminho: ~/Downloads/service-account-key.json

2️⃣  Domínio do Google Workspace
   Domínio: bemobi.com

3️⃣  Email do Super Admin
   Email: admin@bemobi.com
```

**Pronto!** ✅

---

## 📁 O que o script faz:

1. ✅ Copia o JSON para `~/.ai-control/google-workspace-key.json` (seguro)
2. ✅ Adiciona configurações no arquivo `.env`
3. ✅ Define permissões corretas (600)
4. ✅ Mostra próximos passos

---

## 📋 Depois de rodar o script:

### 1. Carregar variáveis

```bash
source .env
```

### 2. Configurar custom schema no Workspace

**Acesse:** https://admin.google.com/

**Passos:**
- Directory → Users
- More options → **Manage custom attributes**
- **Add Custom Attribute**:
  - Category: `custom`
  - Name: `git_name`
  - Type: `Text`
  - Visibility: `Visible to admin and user`

### 3. Preencher campo para usuários

Para cada usuário no Workspace:
- Abrir perfil do usuário
- Seção **User information** → campo `git_name`
- Preencher com o **login do GitHub** (ex: `johndoe`)

### 4. Testar

```bash
./test-integration.sh
```

---

## ❓ Troubleshooting

### "Arquivo não encontrado"
- Certifique-se que baixou o JSON do Google Cloud Console
- Caminho completo, exemplo: `/home/user/Downloads/arquivo.json`

### "Arquivo não é um JSON válido"
- Abra o arquivo e verifique se é um JSON válido
- Deve começar com `{"type":"service_account",...}`

### Script pergunta o caminho mas você não sabe
- Procure no diretório Downloads:
  ```bash
  ls ~/Downloads/*.json
  ```

---

## 🔑 Como pegar o arquivo JSON (lembrete rápido)

1. **Google Cloud Console**: https://console.cloud.google.com/
2. **IAM & Admin** → **Service Accounts**
3. Clicar na service account criada
4. Aba **Keys**
5. **Add Key** → **Create new key** → **JSON** → **Create**
6. 💾 Arquivo baixado!

---

## 📝 Arquivo .env gerado

Depois de rodar o script, seu `.env` terá:

```bash
# ================================================
# Google Workspace Configuration
# Configurado em: 2026-02-12 15:30:00
# ================================================
export AI_CONTROL_WORKSPACE_ENABLED=true
export AI_CONTROL_WORKSPACE_CREDENTIALS="/home/user/.ai-control/google-workspace-key.json"
export AI_CONTROL_WORKSPACE_DOMAIN="bemobi.com"
export AI_CONTROL_WORKSPACE_ADMIN_EMAIL="admin@bemobi.com"
export AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA=custom
export AI_CONTROL_WORKSPACE_GIT_FIELD=git_name
```

---

## ✅ Validação

Para testar se está funcionando:

```bash
# Carregar variáveis
source .env

# Executar teste
./test-integration.sh
```

Deve aparecer:
```
✓ Google Workspace configurado
✓ 15 testes unitários passando
✓ Projeto compilado
```

---

**🎉 Pronto! Google Workspace configurado.**
