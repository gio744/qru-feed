# QRU V1 — Auditoria de conciliação (29/10/2026)

## Base de trabalho
Branch: `integration/qru-v1-final-reconciliation-20261029`
Origem: `release/qru-v1-scope-freeze-20261027`.
PR #75: `feature/qru-home-approved-20261025`.
Não confundir aprovação de CI com homologação em aparelho.

## Evidências
- Comparação GitHub: branches divergentes, 35 commits exclusivos na release e 20 exclusivos na PR #75.
- `Theme.kt`: blob idêntico nas duas branches (`5372b9045a9b8a84980cd8d9cb2a4892829152a3`). Não substituir.
- `CatalogSearch.kt`: release acrescenta ordenação numérica por artigo/inciso, restrição de inciso exato e tratamento de código em consulta numérica. Preservar a release e validar regressões.
- `QruApp.kt`: mudanças sobrepostas; conciliação manual necessária. Preservar navegação CTB | Normas | Q/Mais | Situações | Novidades, busca acessível no topo e menu, e funções de Studio.
- Run Android Build 40808419790: success, artefato `qru-transito-debug-apk`; teste físico não comprovado.
- Run Android UI 40808419797: success.
- PR #77 continua em rascunho e não mesclada.

## Gate antes de qualquer merge
- [ ] Conferir linha a linha o `QruApp.kt` da PR #75 e desta branch; transportar somente diferenças funcionais aprovadas e ainda ausentes.
- [ ] Verificar launcher/ícone e recursos exclusivos da PR #75.
- [ ] Executar regressões de busca: pneu -> 230 XVIII; artigo/inciso exatos; código de infração; ordenação legal.
- [ ] Conferir Studio: apenas vídeos reais, download, verificação de integridade e uso offline.
- [ ] Auditar fontes oficiais e situação normativa (vigente, parcial, revogada, não verificada).
- [ ] Rodar build, testes unitários, testes UI e registrar SHA256 do APK consolidado.
- [ ] Instalar e atualizar em aparelho físico, verificar navegação e funcionamento offline.
- [ ] Somente após esses gates, avaliar merge/publicação.

**Estado:** branch de trabalho criada; esta auditoria não representa mesclagem de código nem novo teste.
