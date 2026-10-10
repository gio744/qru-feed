# Conexão ao servidor

O app já inclui um índice offline. Para receber pacotes assinados, ainda é preciso informar uma API HTTPS real e a chave pública correspondente ao servidor. Não configure conteúdo em revisão como se fosse uma versão juridicamente validada.

## Preparação existente

- Container: construir a partir da raiz com `docker build -f backend/Dockerfile .`.
- Configuração: usar as variáveis de `backend/.env.production.example` no provedor escolhido.
- Banco: PostgreSQL persistente; aplicar migrações com `python migrate.py` antes de iniciar a API.
- API: `/health` confere a disponibilidade do banco; `/legal/releases/current/package` fornece o pacote ativo assinado.
- Autenticação administrativa: exige usuários/roles e JWT do emissor configurado. A rota de token de desenvolvimento fica desabilitada em produção.
- Android: `scripts/configure_android_feed.py` recebe URL HTTPS e chave pública raw Base64, convertendo-a para X.509 Base64. Reconstruir o APK após configurar.

## Dados ainda necessários

Endereço do repositório atual e do provedor/servidor que será usado. A API não foi hospedada nesta etapa. O Dockerfile foi preparado; não foi executado aqui, pois Docker não está disponível. O teste de integração PostgreSQL continua dependente de um banco descartável.
