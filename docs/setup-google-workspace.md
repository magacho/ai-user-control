# Guia: Configuração do Google Workspace para Resolução de Email

Este guia detalha como obter e configurar as credenciais necessárias para que o
`GoogleWorkspaceClient` resolva logins do GitHub em emails corporativos.

## Visão Geral

O fluxo de autenticação usa uma **Service Account** com **Domain-Wide Delegation**,
que permite consultar o diretório de usuários do Google Workspace sem interação do usuário.

```
Service Account (JSON key)
  → Domain-Wide Delegation (autorizada no Admin Console)
    → Admin SDK Directory API
      → Consulta: custom.git_name = "github_login"
        → Retorna: email corporativo
```

## Pré-requisitos

- Acesso ao [Google Cloud Console](https://console.cloud.google.com/) com permissão para criar projetos e service accounts
- Acesso ao [Google Workspace Admin Console](https://admin.google.com/) com perfil de **Super Admin**
- Custom schema `git_name` já configurado nos perfis de usuário do Workspace (veja [Etapa 5](#etapa-5-configurar-custom-schema-no-workspace))

---

## Etapa 1: Criar um Projeto no Google Cloud

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/)
2. Clique em **Select a project** → **New Project**
3. Nomeie o projeto (ex: `ai-user-control`) e clique em **Create**
4. Certifique-se de que o projeto está selecionado no seletor do topo

## Etapa 2: Habilitar a Admin SDK API

1. No menu lateral, vá em **APIs & Services** → **Library**
2. Pesquise por **Admin SDK API**
3. Clique no resultado e em **Enable**

> Sem esta API habilitada, as chamadas ao diretório retornarão erro 403.

## Etapa 3: Criar a Service Account

1. Vá em **IAM & Admin** → **Service Accounts**
2. Clique em **Create Service Account**
3. Preencha:
   - **Name:** `ai-user-control-workspace`
   - **ID:** será gerado automaticamente (ex: `ai-user-control-workspace@project-id.iam.gserviceaccount.com`)
   - **Description:** `Service account para resolução de email via Workspace`
4. Clique em **Create and Continue**
5. Pule as etapas de permissão IAM (não são necessárias para esta integração) e clique em **Done**

### Gerar a chave JSON

1. Na lista de service accounts, clique na que acabou de criar
2. Vá na aba **Keys**
3. Clique em **Add Key** → **Create new key**
4. Selecione **JSON** e clique em **Create**
5. O arquivo `.json` será baixado automaticamente — **guarde-o em local seguro**

> **IMPORTANTE:** Este arquivo contém credenciais sensíveis. Nunca o commite no repositório.

## Etapa 4: Configurar Domain-Wide Delegation

### No Google Cloud Console

1. Na página da Service Account, clique em **Edit** (ícone de lápis)
2. Expanda **Show advanced settings**
3. Marque **Enable Google Workspace Domain-wide Delegation**
4. Preencha o **OAuth consent screen** se solicitado (apenas o nome do app é obrigatório)
5. Salve e copie o **Client ID** numérico (ex: `123456789012345678901`)

### No Google Workspace Admin Console

1. Acesse [admin.google.com](https://admin.google.com/)
2. Vá em **Security** → **Access and data control** → **API controls**
3. Na seção **Domain-wide delegation**, clique em **Manage Domain Wide Delegation**
4. Clique em **Add new**
5. Preencha:
   - **Client ID:** o ID numérico copiado acima
   - **OAuth Scopes:** `https://www.googleapis.com/auth/admin.directory.user.readonly`
6. Clique em **Authorize**

> Use **apenas** o escopo `admin.directory.user.readonly` (princípio do menor privilégio).

## Etapa 5: Configurar Custom Schema no Workspace

O `GoogleWorkspaceClient` busca um campo customizado no perfil do usuário para associar
o login do GitHub ao email corporativo.

### Criar o custom schema (se ainda não existir)

1. No Admin Console, vá em **Directory** → **Users**
2. Clique em **More options** → **Manage custom attributes**
3. Clique em **Add Custom Attribute**
4. Preencha:
   - **Category:** `custom` (ou o nome que preferir — ajuste `custom-schema` na config)
   - **Custom fields:**
     - **Name:** `git_name`
     - **Info type:** Text
     - **Visibility:** Visible to admin and user
     - **No. of values:** Single Value
5. Clique em **Add**

### Preencher o campo para cada usuário

1. No Admin Console, vá ao perfil de um usuário
2. Clique em **User information** → seção do custom schema
3. Preencha o campo `git_name` com o **login exato do GitHub** (case-sensitive)
4. Salve

> Para preenchimento em massa, use a [API de atualização de usuários](https://developers.google.com/admin-sdk/directory/v1/guides/manage-users)
> ou a importação via CSV do Admin Console.

## Etapa 6: Configurar a Aplicação

### Variáveis de ambiente

```bash
# Habilitar integração
export AI_CONTROL_WORKSPACE_ENABLED=true

# Credenciais: caminho para o arquivo JSON OU o conteúdo JSON inline
export AI_CONTROL_WORKSPACE_CREDENTIALS=/path/to/service-account-key.json

# Domínio do Google Workspace
export AI_CONTROL_WORKSPACE_DOMAIN=suaempresa.com

# Email de um admin do Workspace (necessário para delegação)
export AI_CONTROL_WORKSPACE_ADMIN_EMAIL=admin@suaempresa.com

# (Opcionais - valores padrão mostrados)
export AI_CONTROL_WORKSPACE_CUSTOM_SCHEMA=custom
export AI_CONTROL_WORKSPACE_GIT_FIELD=git_name
```

### Credenciais inline (alternativa)

Se preferir não usar um arquivo, passe o JSON inteiro como variável de ambiente:

```bash
export AI_CONTROL_WORKSPACE_CREDENTIALS='{"type":"service_account","project_id":"...","private_key":"..."}'
```

> Útil em ambientes de CI/CD ou containers onde montar arquivos é inconveniente.

## Verificação

### Testar a conexão

Após configurar, execute a aplicação e verifique os logs de inicialização:

```
INFO  c.b.a.i.g.GoogleWorkspaceClient -- Google Workspace client initialized for domain: suaempresa.com
```

### Testar a resolução de email

Execute o collect do GitHub Copilot e verifique nos logs:

```
DEBUG c.b.a.i.g.GitHubCopilotApiClient -- Workspace resolved email for user johndoe: john@suaempresa.com
```

Nos dados exportados, o campo `email_type` será `workspace` para usuários resolvidos via Workspace.

### Executar testes de integração

```bash
export AI_CONTROL_WORKSPACE_TEST_GIT_LOGIN=um_login_github_valido
mvn verify -P integration-tests
```

## Troubleshooting

| Sintoma | Causa provável | Solução |
|---------|---------------|---------|
| `403 Not Authorized to access this resource/api` | Domain-Wide Delegation não configurada ou escopo incorreto | Verifique o Client ID e o escopo no Admin Console |
| `400 Invalid Input` | `admin-email` não é um admin válido | Use o email de um Super Admin ou Admin com acesso ao diretório |
| `GoogleJsonResponseException: 403` | Admin SDK API não habilitada | Habilite a API no Cloud Console (Etapa 2) |
| Lookup retorna vazio para todos | Custom schema ou campo com nome diferente | Verifique `custom-schema` e `git-name-field` na configuração |
| `IOException: Google Workspace credentials not configured` | Variável `AI_CONTROL_WORKSPACE_CREDENTIALS` vazia | Defina o caminho do arquivo ou o JSON inline |

## Referências

- [Admin SDK Directory API - Overview](https://developers.google.com/admin-sdk/directory/v1/guides)
- [Service Account Delegation](https://developers.google.com/identity/protocols/oauth2/service-account#delegatingauthority)
- [Managing Custom User Attributes](https://support.google.com/a/answer/6208725)
