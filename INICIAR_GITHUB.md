# Primeiro teste automático do QRU

1. Crie um repositório privado chamado qru-transito no GitHub.
2. Envie o conteúdo desta pasta à raiz do repositório. A pasta .github deve estar incluída; não envie só o ZIP e não coloque os arquivos dentro de outra pasta.
3. Em Actions, execute QRU Backend PostgreSQL e QRU Android Build por Run workflow. Os fluxos também executam quando seus respectivos arquivos são enviados.
4. Aguarde os dois resultados. O teste de banco usa conteúdo técnico vazio e um banco descartável.
5. Se Android passar, abra a execução e baixe qru-transito-debug-apk em Artifacts. Extraia o ZIP: o arquivo instalável é app-debug.apk.
6. Se falhar, baixe qru-build-diagnostics ou qru-postgres-test-log e forneça o resultado para correção.

O APK é uma versão de teste, não uma versão da Play Store. O endereço da API e a chave de produção ainda são placeholders. O banco do teste não é hospedagem persistente. Não envie senhas ou chaves privadas ao repositório.
