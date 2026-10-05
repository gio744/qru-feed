# QRU Trânsito — Smoke Test de aparelho

Registrar aparelho, versão Android, versão do APK e SHA-256.

## Instalação e inicialização
- [ ] APK instala sem erro.
- [ ] QRU abre sem tela preta/crash.
- [ ] Identidade preta/roxa é exibida.
- [ ] Navegação inferior mostra Busca, CTB, Normas, Situações e Novidades.

## Busca
- [ ] Campo único aceita texto.
- [ ] Consulta vazia não produz conclusão jurídica.
- [ ] Base vazia mostra mensagem de expansão, sem inventar resultado.
- [ ] Resultado local exibe versão/jurisdição.

## Voz
- [ ] Ícone do microfone está integrado à busca.
- [ ] Permissão RECORD_AUDIO é solicitada somente quando necessária.
- [ ] Reconhecimento preenche a busca.
- [ ] Negar permissão não derruba o app.

## Modo Ocorrência
- [ ] Campo aceita narrativa.
- [ ] Interface deixa claro que narrativa localiza hipóteses, não confirma fatos.

## Offline
- [ ] App abre sem internet.
- [ ] Base previamente ativa continua consultável.
- [ ] Falha de atualização não apaga a base local.

## Atualização jurídica
- [ ] Pacote com fingerprint inválido é rejeitado.
- [ ] Pacote com assinatura inválida é rejeitado.
- [ ] Pacote válido só ativa após validações.
- [ ] Last-known-good permanece disponível após falha.

## Resultado
Classificação: PASS / FAIL / BLOCKED.
Anexar captura/log somente quando necessário para reproduzir falha.
