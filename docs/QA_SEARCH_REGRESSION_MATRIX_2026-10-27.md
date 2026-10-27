# QRU — Matriz de regressão de busca (27/10/2026)

Status: plano de testes, NÃO resultado de execução.

## Casos obrigatórios
| Consulta | Verificação exigida |
|---|---|
| `230 V` | Apresentar somente o desdobramento V como correspondência exata prioritária; não confundir com XVIII. |
| `230 XVIII` | Priorizar o inciso XVIII; não misturar com V. |
| `pneu` | Priorizar ficha relacionada ao art. 230 XVIII, sujeito à validação do enquadramento e do MBFT. |
| `roda` | Retornar resultados pertinentes a roda, sem supor infração sem constatação. |
| `165` | Não tratar `165-A` como correspondência exata de `165`. |
| `165-A` | Priorizar ficha específica de recusa, sem equiparar automaticamente a `165`. |
| `252 III` | Priorizar inciso III, sem confundir com outros incisos. |
| `244 X` | Priorizar inciso X. |
| `231 VIII` | Priorizar inciso VIII. |
| `CNH B elétrico` | Buscar regra pertinente com fonte, condições e data de vigência verificadas. |
| consulta vazia | Não exibir conclusão jurídica automática. |
| termo inexistente | Exibir ausência de resultado, sem inventar infração. |

## Regras de aceite
1. A correspondência exata de artigo + inciso precede correspondências parciais.
2. O parser diferencia hífen de artigo (`165-A`) e separador de inciso (`230 XVIII`).
3. Toda ficha operacional exibe fundamento, data de revisão e status de vigência verificado; falta de confirmação deve ficar explícita.
4. Busca por voz passa pelo mesmo normalizador e ranking da busca textual.
5. Testes automatizados devem comprovar o ranking e a ausência de colisões; testes manuais devem confirmar navegação e abertura da ficha.
6. Qualquer falha nos casos `165`/`165-A`, `230 V`/`230 XVIII` ou na indicação de vigência bloqueia a liberação.

## Evidências necessárias
Registrar commit, execução CI, resultado PASS/FAIL por caso e versão do APK. Este documento não atesta testes executados.