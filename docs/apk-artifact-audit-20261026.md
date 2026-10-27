# Auditoria de artefato Android — 26/10/2026

Artefato do workflow QRU Android Build, run 40242901053, commit 74d2a406a14a719313674565ccb37f4e70d6fc56.

- Arquivo: app-debug.apk
- Tamanho: 104068678 bytes
- SHA-256 calculado: b2fd07353fde7bb0c1714d63332092f06abf83b34d797d65a93dd0d06c7884d2
- SHA-256 em app-debug.apk.sha256: idêntico
- SHA-256 em app-debug.apk.record.json: idêntico
- Registro de integridade do workflow: PASS
- Registro de teste em aparelho: NOT_YET_DEVICE_TESTED
- Estrutura APK Sig Block 42: encontrada (isso **não** comprova validação criptográfica).
- Verificação com apksigner: NÃO EXECUTADA (ferramenta indisponível no ambiente de inspeção).
- Compatibilidade com app instalado: NÃO VERIFICADA (necessário pacote e certificado do dispositivo).
- Decisão: não liberar como atualização instalada; não solicitar desinstalação.

Próximos critérios: `apksigner verify --verbose --print-certs app-debug.apk`; identificar package/versionCode com `aapt`; comparar com instalação existente; validar no dispositivo; conferir recursos de ícone adaptive.
