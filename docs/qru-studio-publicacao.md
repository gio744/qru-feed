# QRU Studio — fluxo editorial e distribuição

## Estado de implementação (2026-10-22)

O aplicativo contém um catálogo local vazio em `android/app/src/main/assets/studio/videos.json`. O leitor valida metadados, URL HTTPS do arquivo original sob domínio QRU e SHA-256. Há download via Android DownloadManager, reprodução offline por aplicativo compatível, exclusão manual e verificação de integridade. **Nenhum vídeo foi publicado e não há integração automática com redes sociais.**

## Um vídeo, quatro destinos

1. **Pauta:** selecionar dúvida a partir de buscas agregadas e tendências externas, sem identificar usuários.
2. **Roteiro:** redigir situação, enquadramento, condições de aplicação e exceções, distinguindo hipótese de constatação real.
3. **Revisão jurídica:** registrar nome do revisor, data, fontes oficiais vigentes, versões e aprovação expressa. Sem aprovação, não incluir no catálogo.
4. **Produção:** gerar vídeo vertical 9:16 para Shorts/Reels/TikTok; versão mais longa, quando necessário, para YouTube e QRU. Informar claramente quando houver simulação ou apresentadora gerada por IA.
5. **Hospedagem QRU:** guardar MP4 próprio/licenciado em infraestrutura QRU, calcular SHA-256 **do arquivo final exato** e verificar download em dispositivo.
6. **Publicação:** adicionar item aprovado a `studio/videos.json` com `id`, `title`, `description`, `legalReference`, `reviewedBy`, `reviewedAt`, `videoUrl`, `durationSeconds`, `sha256` e `youtubeUrl` opcional. O catálogo não aceita arquivos de terceiros como fonte de download.
7. **Distribuição:** publicar cópias adequadas no YouTube, Instagram e TikTok apenas por contas autorizadas e respeitando regras de cada plataforma. Upload automático exige APIs/permissões próprias e ainda não está implementado.
8. **Revisão contínua:** alteração normativa relevante exige retirar ou substituir o vídeo e publicar novo hash. Vídeos locais antigos devem ser invalidados na próxima sincronização; a sincronização remota ainda não foi implementada.

## Checklist de aceite antes de divulgar

- [ ] Base legal conferida em fonte oficial e revisor identificado
- [ ] Roteiro aprovado e sem afirmações operacionais ambíguas
- [ ] Identificação de simulação/IA visível quando aplicável
- [ ] Direitos de imagem, voz, música e cenas regularizados
- [ ] Vídeo original hospedado em domínio QRU, com SHA-256 correto
- [ ] Teste de download, reprodução offline e exclusão em Android real
- [ ] Links do YouTube, Instagram e TikTok conferidos
- [ ] Processo documentado para retirar conteúdo desatualizado

## Limitações técnicas conhecidas

- A detecção de download finalizado ainda depende de atualização manual da tela.
- A reprodução offline utiliza um player externo do Android; não há player embutido.
- Não há armazenamento automático de transcrição, publicação remota, atualização de catálogo ou upload multicanal.
- A verificação SHA-256 durante composição da interface poderá causar lentidão em vídeos grandes; mover para processamento assíncrono antes do lançamento.
- O botão de download depende de infraestrutura de mídia e catálogo efetivamente aprovado.
- Não há vídeos publicados neste repositório.
