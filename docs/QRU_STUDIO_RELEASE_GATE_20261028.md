# QRU Studio — critérios de liberação (28/10/2026)

## Situação verificada
- O catálogo embarcado `android/app/src/main/assets/studio/videos.json` contém `schemaVersion: 1` e `items: []`.
- `QruVideoCatalog.published` filtra entradas editoriais incompletas e fontes de mídia não confiáveis.
- O teste `QruStudioCatalogContractTest` exercita o parser real; sua presença no repositório **não comprova** execução bem-sucedida.
- O workflow `android-build.yml` prevê testes JVM, compilação e publicação de APK de depuração apenas quando o job for bem-sucedido.

## Critérios antes de exibir vídeos na página inicial
1. Vídeo real produzido, revisado tecnicamente e aprovado para publicação.
2. ID único, título, descrição, referência legal, revisor e data.
3. URL HTTPS de mídia hospedada em domínio autorizado, duração e SHA-256 real do MP4.
4. URL YouTube opcional, usada somente para abrir o vídeo na plataforma; não tratá-la como fonte de download.
5. Testar reprodução, download, hash do arquivo recebido, modo offline, cancelamento e downloads simultâneos.
6. Não criar vídeos de demonstração fictícios como se estivessem publicados.
7. Confirmar CI verde no commit exato, APK instalável e teste físico antes de mesclar.

## Pendências de integração
- PR #72 apresenta divergência/conflito com `main`; reconciliar antes de integrar.
- PR #75 requer validação de navegação e interface aprovada.
- PR #78 adiciona testes e deve permanecer em rascunho até os checks serem confirmados.

Nenhuma alteração em assinaturas, preços ou publicação comercial está autorizada por este documento.
