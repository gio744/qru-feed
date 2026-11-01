# QRU V1 — Matriz de homologação (01/11/2026)

Documento de acompanhamento na branch de desenvolvimento. Não altera código, não aprova merge e não substitui `docs/QA_RELEASE_GATE_2026-10-27.md`.

## Evidências conhecidas
- PR #80 informa Android Build run 40814262456 com conclusão `success` e artefato `qru-transito-debug-apk`. Conferir SHA do commit e disponibilidade do artefato antes de utilizar.
- A compilação bem-sucedida não comprova funcionamento no aparelho.
- PRs #72, #75, #77, #78 e #80 exigem revisão de integração.
- Estado inicial de todos os testes manuais abaixo: **NÃO EXECUTADO / SEM EVIDÊNCIA**.

## Matriz obrigatória
| ID | Verificação | Critério de aceite | Estado |
| --- | --- | --- | --- |
| UI-01 | Rodapé CTB / Normas / Q / Situações / Novidades | Navegação correta, retorno à home, sem duplicações | Pendente |
| UI-02 | Campo superior de busca e Q central | Busca acessível, Q identificado, sem banner não aprovado | Pendente |
| UI-03 | Contraste e dimensões do ícone | Legível no Android físico | Pendente |
| BUS-01 | 230 V e XVIII | Busca exata, incisos sem mistura e ordenação jurídica | Pendente |
| BUS-02 | 165, 165-A, 252 III, 244 X, 231 VIII | Resultados relevantes e fichas corretas | Pendente |
| JUR-01 | Fichas operacionais | Fontes oficiais, vigência e revisão jurídica confirmadas | Pendente |
| PDF-01 | Catálogo e ordem | Ordenação consistente e indicação de disponibilidade | Pendente |
| PDF-02 | 1.026–1.029 e demais PDFs embarcados | SHA-256 válido e abertura sem rede | Pendente |
| VID-01 | Catálogo de vídeos | Sem entradas fictícias, inválidas ou mídia não confiável | Pendente |
| VID-02 | Downloads | Identificação individual, concorrência e reprodução testadas | Pendente |
| APK-01 | Instalação e atualização | Abre, atualiza sem perda indevida de dados e reinicia | Pendente |
| APK-02 | Testes instrumentados | Execução verde na branch consolidada | Pendente |
| SEC-01 | Segredos e assinatura | Sem credenciais no código; artefato de teste identificado | Pendente |

## Ordem de execução
1. Conferir diffs e conflitos entre PRs #72, #75, #77 e #80, inclusive `QruApp.kt` e recursos do ícone.
2. Executar testes automatizados na branch consolidada, registrar URL do workflow, SHA e resultado.
3. Instalar APK em aparelho Android e preencher cada linha com evidência reproduzível.
4. Revisar conteúdo jurídico e documentos oficiais.
5. Só então avaliar merge e liberação.

**Bloqueio de publicação:** qualquer item crítico pendente impede declarar a V1 homologada.

## Evidências automatizadas confirmadas em 01/11/2026
- Commit testado: `bfa66a1dfab4a9f23e705cf9810ebb2e3a95aedc` (branch `feature/operational-accordion-distinctive`).
- [Android Build #41305707261](https://github.com/gio744/qru-feed/actions/runs/41305707261): **success**.
- [Android UI #41305707313](https://github.com/gio744/qru-feed/actions/runs/41305707313): **success**.
- [Release Smoke #41305707256](https://github.com/gio744/qru-feed/actions/runs/41305707256): **success**.
- A suíte inclui regressão para pesquisa integral do art. 230, isolamento do artigo e ordenação jurídica dos incisos. A execução verde não equivale à inspeção visual em dispositivo físico.
- Comparação com a branch candidata `integration/qru-v1-final-reconciliation-20261029`: histórico **divergente** (180 commits à frente / 102 atrás, no momento da consulta); PR #80 continua **draft**. Não integrar por merge automático sem conciliação por arquivo.
- A branch de desenvolvimento possui 16 PDFs no catálogo, enquanto a candidata possui 14; preservar as referências 969/2022 e 1.030/2026 na consolidação.
- O catálogo Studio contém `items: []`; nenhum vídeo deve ser apresentado como publicado antes de existir mídia válida.

**Interpretação da matriz:** APK-02 possui evidência automatizada **na branch de desenvolvimento**, mas continua pendente **na branch consolidada**; BUS-01 possui cobertura automatizada, mas não foi homologado manualmente; todos os demais estados permanecem pendentes até evidência específica. Não confundir CI verde com liberação para publicação.

## Conciliação documental — biblioteca e links (01/11/2026)
- `OfflineDocumentSource.kt` na branch de desenvolvimento exige correspondência **única** de URL oficial com PDF embarcado; a candidata usa `firstOrNull`, que pode selecionar arbitrariamente um documento em catálogo duplicado. **Preservar a proteção de unicidade**.
- `OfflineNormLibrary.kt` na branch de desenvolvimento é autocontida, sem callback `openSource`; a candidata ainda aceita esse callback. **Preservar a navegação interna** solicitada para a biblioteca.
- A listagem da branch de desenvolvimento organiza por ano e número em ordem decrescente, exibe contagem, agrupamento anual e alerta de vigência. **Não substituir** pelo componente simplificado da candidata.
- Esta decisão é de integração por arquivo, não comprovação de publicação nem homologação jurídica.

## Conciliação da home e navegação — inspeção de código (01/11/2026)
- A branch ativa usa **cinco abas** `CTB / Normas / QRU / Situações / Novidades`, com `BUSCA` no centro, sem `MAIS` duplicado. A candidata de integração ainda declara seis valores de `QruTab` e utiliza `MAIS` como centro. **Não transplantar a enumeração ou a barra inferior da candidata**.
- A home ativa (`SearchHome`) inclui pesquisa por texto/voz, análise guiada de habilitação e quatro atalhos personalizáveis; a candidata possui `QruOperationalHome` separado, com transição `homeVisible` e busca. A conciliação exige comparação funcional e visual antes de importar qualquer parte da home candidata.
- A home ativa já utiliza cabeçalho azul em gradiente e marca QRU. A candidata possui Q central com tratamento metálico mais elaborado; avaliar **somente** eventual transferência de acabamento visual em mudança isolada, acompanhada de testes, sem alterar rotas ou comportamento.
- O menu de ajuda foi atualizado no commit `89f55f93`. O conjunto Android Build, Android UI e Release Smoke para esse commit terminou em **success** (runs 41306340360, 41306340568, 41306340425).
- UI-01 e UI-02 permanecem **pendentes de inspeção em aparelho**; a inspeção de código e CI não substituem homologação visual.

## Evidência do refinamento metálico do Q — 01/11/2026
- Commit de implementação visual: [`ff1340ae21740e4b161943a610d35d3d641acbe1`](https://github.com/gio744/qru-feed/commit/ff1340ae21740e4b161943a610d35d3d641acbe1), branch `feature/operational-accordion-distinctive`.
- [Release Smoke #41306874560](https://github.com/gio744/qru-feed/actions/runs/41306874560): **success**.
- [Android Build #41306874498](https://github.com/gio744/qru-feed/actions/runs/41306874498): **success**.
- [Android UI #41306874753](https://github.com/gio744/qru-feed/actions/runs/41306874753): **success** (concluído em 01/11/2026 às 12:36:09 UTC).
- **Conclusão restrita:** o refinamento do Q passou nas três rotinas automatizadas no mesmo SHA. Não há evidência de inspeção visual em aparelho, instalação/atualização física, verificação jurídica ou homologação da branch consolidada. Permanecem os gates manuais e a PR #80 em draft.
