# QRU Android — conciliação isolada dos gates (04/11/2026)

## Objetivo
Integrar, em branch isolada derivada de main, as proteções de build das branches main e feature/operational-accordion-distinctive sem alterar UI, fichas ou catálogo de PDFs nesta etapa.

## Evidência comparada
- main .github/workflows/android-build.yml: audit_reference_ranges.py; validate_operational_cards.py; restauração SHA-pinned dos PDFs 1026–1029; cache verificado; build e registro APK.
- feature .github/workflows/android-build.yml: validate_operational_drafts.py + testes; verify_apk_norms.py + testes; register_apk.py + testes.
- main android-ui.yml: execução instrumentada em emulador e relatórios.
- feature android-ui.yml: cache PDF verificado e gatilho push na branch de desenvolvimento.

## Condições antes de modificar workflow
1. Conferir scripts e dependências de ambas as branches e copiar apenas os necessários.
2. Não substituir catálogo nem conteúdo jurídico por versões divergentes.
3. Conservar permissions: actions: read para recuperar staging via gh.
4. Conservar restauração SHA-pinned, verificação do cache e registro APK.
5. Incluir validação de drafts e verificação de PDFs dentro do APK, com seus testes.
6. Executar build e UI em SHA idêntico, registrar URLs e resultados.
7. Não declarar homologação antes dos testes físicos e revisão jurídica.

## Estado
- [x] Branch de integração criada a partir de main.
- [x] Comparação dos dois workflows documentada.
- [ ] Inspecionar scripts necessários na branch feature.
- [ ] Integrar scripts e workflows com revisão de diferenças.
- [ ] Executar CI Android Build e Android UI na branch.
- [ ] Validar instalação, navegação, biblioteca offline e busca em dispositivo.
- [ ] Revisar fichas 165/165-A e vigência normativa.

**Não fazer merge automático nem publicar APK nesta fase.**
