# PR #83 — Critérios de conciliação (2026-11-02)

## Estado verificado
- Build Android #41489642765 aprovado no commit cbedbb8 (anterior aos quatro arquivos recuperados).
- Branch de integração: e94ca27, com os quatro arquivos exclusivos da main restaurados.
- PR #83 ainda apresenta conflitos (mergeable_state=dirty). Não fazer merge automático.

## Arquivos de maior risco
- `android/app/src/main/java/br/com/qru/transito/ui/QruApp.kt`: preservar navegação moderna aprovada, sem reintroduzir aba Mais ou substituir os cinco ícones.
- `android/app/src/main/assets/norms/pdf-catalog.json`: preservar entradas adicionais e hashes; não eliminar documentos para resolver conflito.
- `android/app/src/main/java/br/com/qru/transito/ui/OfflineDocumentSource.kt` e `OfflineNormLibrary.kt`: preservar leitura interna e diferenciação de documentos realmente disponíveis offline.
- `android/app/src/main/java/br/com/qru/transito/domain/usecase/CatalogSearch.kt`: preservar normalização de códigos sem hífen e ranking.
- `android/app/src/main/java/br/com/qru/transito/data/repository/OperationalFichePackage.kt`: preservar bloqueio editorial de conteúdo não aprovado.
- `.github/workflows/android-build.yml`: manter validações de PDF, integridade e testes que passaram na execução #41489642765.

## Critério de aceitação
1. Inspecionar cada divergência funcional da main antes de optar pela implementação da integração.
2. Não resolver conflitos de código compartilhado por substituição em massa ou merge artificial de histórico.
3. Executar novamente build e testes Android após a resolução, no commit exato a integrar.
4. Verificar navegação, busca sem hífen, fichas, biblioteca offline, referências e identidade visual.
5. Só retirar o rascunho do PR #83 quando o GitHub indicar ausência de conflitos e checks aprovados.

Este registro é uma auditoria de integração, não uma declaração de merge concluído.
