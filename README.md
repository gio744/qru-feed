# QRU Trânsito — integração do feed, 05/10/2026

Pacote consolidado a partir de P1.4 (backend) e P2.9 (Android).

## Correções verificadas
- Corrigidos escapes literais de quebra de linha que impediam importar a API e instalar suas dependências.
- Registrada a rota GET /legal/releases/current/package na aplicação FastAPI.
- Distribuição usa os mesmos bytes canônicos da assinatura Ed25519.
- Pacote com impressão digital divergente ou assinatura inválida é bloqueado.
- Teste legado de assinatura substituído por teste compatível com a implementação Ed25519 atual.

## Verificação executada
Python compileall: passou.
pytest backend/tests: 11 passaram; 1 teste de ambiente PostgreSQL foi pulado.
Os testes da rota usam banco simulado e chaves efêmeras; não comprovam hospedagem nem integração com PostgreSQL real.

## Executar backend local
Instale backend/requirements.txt e configure as variáveis QRU_ conforme backend/.env.example.
A partir de backend: python3 -m uvicorn app.main:app --host 127.0.0.1 --port 8000
Aplique as migrações em um PostgreSQL de teste antes de usar os endpoints de escrita.

## Pendências concretas
- Provisionar PostgreSQL e hospedar a API em HTTPS.
- Configurar chave pública Ed25519 no backend (formato raw base64) e Android (formato X.509 base64), correspondentes à mesma chave.
- Substituir https://api.example.invalid/ em android/app/src/main/res/values/qru_config.xml pelo endereço real.
- Validar criação, revisão, ativação e consumo de uma versão no banco real.
- Compilar APK e executar testes em aparelho.
- Revisar juridicamente as fichas antes de publicá-las; este pacote não adiciona conteúdo normativo validado.

Nenhum APK, hospedagem ou publicação na Play Store foi produzido nesta etapa.

## Continuação — teste completo preparado
O teste PostgreSQL agora cria usuário técnico, assina pacote vazio, cria candidato,
comprova bloqueio sem revisão, registra revisão e regressão, ativa, baixa e confere
bytes/cabeçalhos, consulta dossiês e bloqueia reativação e acesso sem autenticação.
Execute no banco descartável: docker compose -f docker-compose.test.yml up --abort-on-container-exit --exit-code-from tests
O teste exige QRU_ENV=test e banco qru_test; não deve ser apontado para produção.
Também corrigido o Compose e adicionada espera pela saúde do banco.
Nesta execução: 11 testes passaram; o fluxo PostgreSQL foi pulado porque o servidor não está disponível.

## Continuação — coerência da versão do feed
- Candidatos com version diferente de content_version no pacote assinado são rejeitados pelo gate estrutural.
- Distribuição também bloqueia divergência em registros já armazenados.
- Metadados e download usam a mesma ordenação por ativação, criação e ID para desempate determinístico.
- Verificação desta etapa: 14 testes passaram; 1 integração PostgreSQL pulada por ausência de servidor. Sintaxe Python verificada. APK e teste em aparelho continuam pendentes.
