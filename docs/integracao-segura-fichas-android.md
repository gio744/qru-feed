# QRU — integração segura de fichas editoriais na interface Android

## Constatação no código atual
A tela `ReferenceDialog` em `android/app/src/main/java/br/com/qru/transito/ui/QruApp.kt` **não** lê automaticamente os JSONs de `content/operational-cards/`.

A interface lê exclusivamente os assets:
- `operational/approved-fiches.json`
- `operational/approved-fiches.sha256`

A liberação depende também de `legalRepository.isActiveReference(result)` e da seleção pelo código do resultado. A aprovação é verificada por `OperationalFichePackage.approvedByCodeWithDigest`.

## Critérios obrigatórios para a integração
1. Não copiar diretamente uma ficha com status `pendente` ou `em_revisao` para o pacote aprovado.
2. Criar transformação explícita e revisada entre o schema editorial (`quandoAutuar`, `quandoNaoAutuar`, etc.) e o modelo Android (`whenApplicable`, `exceptions`, `measures`), preservando evidências por afirmação.
3. Exigir revisão jurídica, fontes oficiais e digest correspondente antes de gerar os assets.
4. Preservar a verificação de referência ativa e o bloqueio por digest.
5. Testar casos: código válido e aprovado; código inexistente; código inativo; digest incorreto; conteúdo pendente.
6. Só depois publicar APK de teste e conferir as fichas no dispositivo.

## Observação de distribuição
O workflow atual usa `-PqruParallelTest` e altera o `applicationId` para `br.com.qru.testeapp`. Portanto, o APK de CI é uma versão paralela de testes, não necessariamente uma atualização direta do pacote `br.com.qru.transito`.

## Situação
A ficha editorial 165-A está pendente. Nenhuma integração automática foi autorizada por este documento.
