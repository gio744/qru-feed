# QRU Android — protocolo de atualização segura (26/10/2026)

## Estado verificado
- PR #75: navegação proposta CTB | Normas | Q | Situações | Novidades, ainda em rascunho.
- Commit verificado: `74d2a406a14a719313674565ccb37f4e70d6fc56`.
- CI: Android UI e Android Build concluídos com sucesso; testes físicos ainda pendentes.
- Artefato de desenvolvimento: `qru-transito-debug-apk` (workflow 40242901053).
- `android/app/build.gradle.kts`: `versionCode=2`, `versionName=0.2.7-indice`.
- Identificador normal: `br.com.qru.transito`; com `-PqruParallelTest`: `br.com.qru.testeapp`.
- A configuração `parallelTest` usa `android/test-signing/qru-debug.jks` exclusivamente para testes.

## Bloqueios antes de instalar por cima do app existente
1. Identificar package name, versionCode e certificado SHA-256 do APK **instalado**.
2. Identificar package name, versionCode e certificado SHA-256 do APK **candidato**.
3. Exigir mesmo package name, certificado compatível e versionCode não inferior. Para distribuição de nova versão, incrementar versionCode.
4. Se os identificadores forem diferentes, instalar lado a lado; não presumir migração de dados.
5. Não desinstalar, limpar dados nem trocar assinatura para contornar erro de atualização.
6. Verificar backup/exportação suportada antes de qualquer migração; `allowBackup=false` no Manifest impede presumir backup automático.
7. Testar no aparelho físico: inicialização, busca exata, fichas individuais, PDFs offline/zoom/rolagem, novidades, navegação, retorno e persistência.
8. Testar novo ícone QRU prata fosca em densidades Android e fundo claro/escuro, incluindo adaptive icon e recorte.
9. Confirmar que os PDFs oficiais e hashes são preservados e que conteúdo jurídico pendente não é apresentado como validado.
10. Só liberar APK após relatório de resultados e aprovação explícita.

## Inspeção técnica local (sem alterar aparelho)
- `adb shell pm list packages | grep qru` para identificar pacotes.
- `adb shell dumpsys package br.com.qru.testeapp` (ou `br.com.qru.transito`) para dados da instalação.
- `apksigner verify --print-certs candidato.apk` para inspecionar assinatura do candidato.
- `aapt dump badging candidato.apk` para identificar package e versionCode.

Este protocolo não afirma que o APK candidato já seja compatível com o aplicativo instalado. Não contém credenciais nem certificados privados.
