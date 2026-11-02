# QRU V1 — Plano de conciliação segura (02/11/2026)

## Base verificada
- Branch isolada: `integration/qru-v1-safe-reconcile-20261102`
- Commit de origem: `f750d390eb1e345c309b56184770c536176afba1`
- Android Build, Android UI e Release Smoke: success nesse commit.
- PR #66: mergeable=false (dirty); PR #80: draft, mergeable=true, mas NÃO mesclar automaticamente.
- Comparação da branch ativa com a candidata antiga: 211 commits à frente e 102 atrás (consulta de 02/11/2026).

## Regras de preservação
1. `QruApp.kt`: cinco abas, busca texto/voz, navegação sem "Mais" duplicado; manter aparência aprovada.
2. `Theme.kt`: conservar grafite/navy e Q metálico; não aplicar paleta em lote.
3. `CatalogSearch.kt`: manter prioridade jurídica e isolamento de artigo/inciso; testar 230 V, 230 XVIII e "pneu".
4. `OfflineNormLibrary.kt` e `OfflineDocumentSource.kt`: manter biblioteca interna, ordenação e correspondência única de PDF.
5. `pdf-catalog.json`: 16 entradas na origem; verificar integridade, duplicidades, vigência e PDFs offline.
6. Studio: não apresentar vídeos inexistentes como publicados; manter validações de HTTPS e SHA-256.

## Portões obrigatórios antes de publicar
- Resolver conflitos por arquivo e revisar o diff; não fazer merge cego de PR #66 ou #80.
- Executar Build, UI e Release Smoke no SHA final da branch consolidada.
- Validar instalação, atualização, navegação, pesquisa e PDFs sem rede em aparelho físico.
- Conferir conteúdo jurídico e vigência em fontes oficiais.
- Registrar resultados na matriz `docs/QRU_V1_VALIDATION_MATRIX_2026-11-01.md`.
- Não declarar homologação ou produção até cumprir todos os itens críticos.

Este documento é um plano de trabalho, não um atestado de homologação.
