# QRU Trânsito — índice offline, 10/10/2026

O app inclui 418 referências pesquisáveis sem internet. A pesquisa aceita artigo, código com ou sem hífen, palavras com acento e descrições de situações. Os resultados abrem os documentos oficiais registrados no catálogo.

## Conteúdo disponível

- Busca: referências locais, pesquisa por voz e pesquisa a partir da descrição da ocorrência.
- CTB: índice por artigo/código e link ao CTB integral oficial.
- Normas: acesso às fontes oficiais do CONTRAN e do MBFT.
- Situações: pesquisas por termos práticos; vídeos ainda não disponíveis.
- Novidades: estado e alterações do app; não representa monitoramento normativo automático.

As 418 referências vieram de QRU_Base_Operacional_v1.json, versão 19. Todas têm validação final pendente. A carga inclui título, código, artigo, termos de busca, fonte e metadados editoriais. Não publica orientações de autuação como validadas. Períodos registrados no catálogo aparecem nas referências, quando disponíveis. O índice não comprova vigência ou correção jurídica de cada ficha.

## Compilar a versão de teste instalada em paralelo

Na pasta android: `gradle -PqruParallelTest testDebugUnitTest assembleDebug`.

Essa opção usa br.com.qru.testeapp e o certificado de debug em android/test-signing/qru-debug.jks, mantendo compatibilidade entre atualizações de teste. O certificado é exclusivamente de desenvolvimento. Sem a propriedade, o identificador principal permanece br.com.qru.transito.

Java/Kotlin: alvo 17; Gradle 8.9; Android SDK 35; Android mínimo API 28. Room exporta o esquema em android/app/schemas. O app usa o índice embarcado quando não existe versão ativa no banco local. Uma versão assinada ativa é consultada sem misturar automaticamente o índice editorial.

## Verificações

Ver docs/indice-offline-2026-10-10.json e docs/android-build-indice-2026-10-10.log. Testes de busca e assinatura fazem parte da suíte Android. A instalação e o funcionamento desta atualização no aparelho ainda precisam ser confirmados pelo usuário.

Servidor: 17 testes unitários passaram; o teste PostgreSQL permanece pulado sem banco disponível. Sintaxe Python e conversão raw → X.509 da chave pública verificadas. O container de produção foi preparado, mas não executado neste ambiente.

## Conexão online ainda pendente

A API ainda não foi hospedada. Os recursos Android continuam com endereço/chave pública de exemplo. O app não agenda atualizações para esse endereço. Para conectar: informar API HTTPS e chave pública do servidor, usar scripts/configure_android_feed.py e reconstruir o APK. Ver docs/servidor-proximo-marco.md.

O Dockerfile usa a raiz do projeto como contexto: `docker build -f backend/Dockerfile .`. É necessário PostgreSQL persistente, migrações, segredo JWT e chave pública válidos. Criação/revisão/ativação no banco real e publicação de conteúdo validado continuam pendentes. A autenticação administrativa exige configuração; o endpoint de token de desenvolvimento não funciona em produção.
