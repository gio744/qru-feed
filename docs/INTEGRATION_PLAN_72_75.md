# Plano de integração PR #72 + PR #75

Branch de trabalho: `integration/qru-android-release-gate-20261027` (base `main`).

## Arquivos sobrepostos verificados
| Arquivo | main (blob) | PR #72 (blob) | PR #75 (blob) |
|---|---|---|---|
| `ui/QruApp.kt` | `a083250` | `0aef088` | `77dc838` |
| `domain/usecase/CatalogSearch.kt` | `6fec349` | `18d0cc9` | `36b1798` |
| `ui/theme/Theme.kt` | `5f4a63c` | `6879072` | `5372b90` |

## Estratégia de conciliação
1. Preservar a versão de maior cobertura funcional da PR #72 como referência para Studio, fichas e catálogo, sem sobrescrever alterações posteriores da main inadvertidamente.
2. Incorporar a navegação aprovada da PR #75: CTB, Normas, Q/Mais, Situações e Novidades; busca no topo.
3. Revisar manualmente diferenças de `CatalogSearch.kt`, incluindo filtros de inciso e ranking; ampliar testes de regressão.
4. Reconciliar `Theme.kt` conforme identidade aprovada; não incluir o experimento visual da PR #73.
5. Revisar os demais arquivos e recursos exclusivos de cada PR, garantindo ícone, FileProvider e testes.
6. Executar build Android, testes e verificação do APK em dispositivo antes de merge.

**Situação:** análise estrutural concluída; nenhuma mesclagem de código ou teste de build foi executado. Não publicar APK até cumprir o gate.