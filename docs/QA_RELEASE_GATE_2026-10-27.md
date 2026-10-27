# QRU TRÂNSITO — Critérios de liberação Android

Data: 2026-10-27

## Regra de liberação
Não publicar APK nem mesclar branches experimentais sem compilação Android bem-sucedida, testes automatizados relevantes e inspeção manual em aparelho.

## Interface
- [ ] Confirmar navegação CTB | Normas | Q (Mais) | Situações | Novidades, conforme PR #75.
- [ ] Garantir busca no campo superior e ausência de banner normativo não aprovado na tela inicial.
- [ ] Verificar contraste, tamanho e alinhamento do ícone Q em Android real.
- [ ] Não incorporar tema experimental PR #73 sem aprovação visual.

## Pesquisa e fichas
- [ ] Pesquisar 230 V e 230 XVIII por artigo, inciso e termos livres, sem misturar desdobramentos.
- [ ] Pesquisar 165, 165-A, 252 III, 244 X e 231 VIII, conferindo ordenação e vínculos com fichas.
- [ ] Conferir campos 'quando autuar' e 'quando não autuar', fontes e data de revisão.
- [ ] Validar referências legais e vigência antes de disponibilizar conteúdo operacional.

## Normas offline
- [ ] Confirmar integridade SHA-256 dos PDFs incluídos no pacote, especialmente 1.026–1.029.
- [ ] Testar abertura sem internet, fallback e indicação explícita de documento não disponível.
- [ ] Distinguir normas vigentes, revogadas e parcialmente revogadas, sem inferir vigência apenas do número.

## Studio e vídeos
- [ ] Testar downloads simultâneos e atualização individual por identificador.
- [ ] Testar reprodução e abertura de links externos, além de funcionamento offline quando houver download.

## Segurança e entrega
- [ ] Não registrar senhas, tokens ou chaves no repositório.
- [ ] Revisar PR #72 e PR #75 quanto a conflitos, checks e escopo antes de mesclar.
- [ ] Produzir APK de teste assinado adequadamente e registrar versão/commit e resultados.
- [ ] Fazer smoke test em aparelho Android: iniciar, pesquisar, abrir ficha, abrir norma, navegar, assistir vídeo e reiniciar.

**Estado:** checklist criado; itens não devem ser considerados aprovados sem evidência dos testes.

## Evidência automatizada — 27/10/2026
- GitHub Actions execução #604: https://github.com/gio744/qru-feed/actions/runs/40506068905 — **success**.
- Commit validado: `93a5ae0c58e8df16186f104d9ad3522f021de439`.
- `Run unit tests independently of PDF downloads`: **success**; inclui regressões de `165`/`165-A`.
- `Assemble APK with verified official PDFs`: **success**, com verificação SHA-256 obrigatória mantida.
- Artefato de testes `qru-transito-debug-apk`, ID `12734718382` (não é versão de produção).
- **Não inferir revisão jurídica ou teste em aparelho a partir do CI.** Permanecem pendentes a revisão editorial de vigência e o smoke test Android, inclusive funcionamento offline, navegação e vídeos.
- Divergências de hashes observadas em execuções anteriores devem ser investigadas como possível variação do arquivo upstream/cache; o sucesso de um build não demonstra, por si só, equivalência jurídica entre versões publicadas.

## QRU Studio — validação de integração incremental
- Execução #609: https://github.com/gio744/qru-feed/actions/runs/40506895442 — **success**.
- Testes unitários e montagem Android com PDFs verificados: **success**; upload do APK de teste: **success**.
- Componentes incorporados da PR #72: `QruVideoCatalog`, `QruStudioIntegrity`, `studio/videos.json` e seus dois testes.
- O catálogo inicial contém zero vídeos publicados; isso evita anunciar vídeos ainda não submetidos à revisão editorial.
- **Pendente:** conectar catálogo à UI, downloads e reprodução; verificar permissões, FileProvider e testes em aparelho antes de liberar Studio ao público.

## QRU Studio — interface Situações
- Execução #610: https://github.com/gio744/qru-feed/actions/runs/40507484274 — **success**.
- Commit validado: `73922198b668740cf346c50fec7146ca2a458502`.
- Testes unitários, APK com PDFs verificados e upload de artefato: **success**.
- A tela Situações carrega `studio/videos.json` pelo `QruVideoCatalog.published`; mostra mensagem editorial quando vazio e cards para vídeos aprovados.
- **Não aprovado ainda:** download offline, reprodução em aparelho, revisão de segurança de URLs e teste funcional da tela. O sucesso do build não comprova esses itens.
