# PR #66 — plano de conciliação segura

Data: 2026-11-02. Branch de trabalho: integration/pr66-safe-reconciliation-20261102.

## Bases verificadas
- main: a7dfdcb99e43a2f3e069b7f4cd54afe70185ed8f
- desenvolvimento: 8b961bf3980433f748223458b1801bd70fc5ba09
- A comparação reversa registra 14 commits exclusivos da main e 16 caminhos alterados.

## Regras de preservação
1. Não sobrescrever QruApp.kt integralmente: reconciliar a interface, busca sem hífen, fichas e navegação.
2. No catálogo, preservar os 16 documentos do desenvolvimento, inclusive 969/2022 e 1.030/2026, e o textIndex de 1.031/2026. Conferir títulos, vigência e hashes antes de publicar.
3. Preservar os testes de catálogo, PDF offline e build Android de ambas as linhas.
4. Não alterar a branch main nem distribuir APK antes de build, UI e release smoke aprovados.

## Arquivos para revisão manual
- android/app/src/main/java/br/com/qru/transito/ui/QruApp.kt
- android/app/src/main/assets/norms/pdf-catalog.json
- android/app/src/test/java/br/com/qru/transito/NormPdfCatalogTest.kt
- android/app/src/test/java/br/com/qru/transito/ui/OfflineDocumentSourceTest.kt
- .github/workflows/android-build.yml
- .github/workflows/contran-pdf-audit.yml
- .github/workflows/contran-pdf-staging.yml

## Pendente
Realizar conciliação de código e histórico, executar testes e verificar mergeability do novo PR. Este documento não representa conflitos resolvidos.
