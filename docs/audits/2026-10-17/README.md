# QRU — auditoria de identidade das 418 referências

Data: 17/10/2026. Escopo: correspondência de código e artigo nos documentos examinados. **Não é validação operacional nem declaração de vigência atual das 418 fichas.**

## Resultado consolidado

| Verificação | Resultado |
| --- | ---: |
| Referências examinadas | 418 |
| Correspondências em cabeçalhos do manual-base | 411 |
| Identidades adicionais confirmadas visualmente nos anexos | 6 |
| Identidade adicional confirmada na tabela SENATRAN (551-71) | 1 |
| Total com evidência de identidade nos documentos examinados | 418 |
| Referências cuja fonte aponta ao manual-base | 401 |
| Divergências de página nessas 401 referências | 0 |
| Referências cuja fonte aponta a resoluções posteriores | 16 |
| Referência ainda ligada à minuta de consulta pública | 1 |
| Fichas aprovadas para uso operacional neste bloco | 0 |

O resultado anterior de 417 correspondências registradas no candidato foi reexaminado, em vez de simplesmente repetido. O manual foi baixado novamente e tem a mesma impressão digital registrada na conferência anterior. Seis PDFs de resoluções posteriores e a tabela SENATRAN também foram obtidos e identificados por SHA-256. Os dados por referência estão em `identity-audit.json`.

## Evidência complementar

A ficha 551-71 não foi localizada como cabeçalho próprio no texto extraído do manual-base de 830 páginas. A página física 11 da tabela da Portaria SENATRAN 354/2022 relaciona código 5517, desdobramento 1, descrição de estacionamento em viadutos e artigo 181, XIV. Isso comprova a identidade histórica sem depender da minuta; não substitui a revisão do texto integral da ficha.

Os cabeçalhos de 779-00, 780-30, 781-10 e 782-00 foram conferidos visualmente nas páginas 4–7 da Resolução 1009/2024. Os de 757-91 e 757-92 foram conferidos visualmente nas páginas 17 e 19 da Resolução 1031/2026. A redação do amparo de 780-30 usa “165-B c/c o parágrafo único”; o candidato abrevia como “165-B, parágrafo único”. O reconhecimento automático estrito não é suficiente para essa equivalência, que foi registrada separadamente.

## Pendências concretas

1. Substituir a fonte de consulta pública de 551-71 por fonte normativa adequada em uma futura versão do conteúdo; manter a pendência da ficha integral.
2. Concluir a revisão das instruções, exceções, medidas e alterações posteriores. Código e artigo conferidos não equivalem a ficha pronta.
3. Concluir a conferência visual de todas as fontes posteriores; a leitura automática dos anexos teve limitações e não foi usada para presumir aprovação.
4. Conferir no app a transição de 757-90 para 757-91/757-92. O candidato registra fim de 757-90 em 16/10/2026 e início dos desdobramentos em 17/10/2026; o art. 16, I, da Resolução 1031/2026 fixa 60 dias após a publicação para a alteração do anexo. A pesquisa ainda contém a expressão editorial “vigência futura”, a revisar sem alterar identificadores persistentes.
5. Revisar datas de conferência e períodos antes de assinar uma nova versão. Não modificar o JSON assinado recebido pelo publicador.

## Integridade e validação técnica

O arquivo candidato permaneceu idêntico byte a byte durante a auditoria: SHA-256 `452d77e5b63ca9dab3addd9858c985bae246eb0c87df68628e9bb96a702e1753`. Todos os campos `ready` do candidato permanecem falsos. Não houve alteração de assinatura, ativação no servidor ou substituição do pacote recebido.

O comparador reproduzível está em `scripts/audit_reference_identity.py`. Recebe o candidato e PDFs/textos extraídos; pode receber manifesto de OCR como argumento opcional. A inspeção visual deste relatório é uma etapa separada e explicitamente registrada no JSON. Seis testes passaram, cobrindo citações cruzadas, inciso incorreto, página física, PDFs distintos, desdobramento em coluna e sufixos de artigo. Extração parcial da tabela não significa código inexistente.

## Fontes examinadas

- https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/Resolucao10032023.pdf — SHA-256 `8e1c0e18924680565fff17585acda791d88607d46193227175be18d37950640d`
- https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/Resolucao10092024.pdf — SHA-256 `2c19112a82bf6357dc8710dd5c00401b721a70022e7dd8386a264814e261a9a1`
- https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/Resolucao10122024.pdf — SHA-256 `d6ff6c688bdc5126ca8644f3dfe80181c36f8b310ae53808c1102abc1e71715b`
- https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/resolucao10222026c.pdf — SHA-256 `2547703494199728cee582c9f5717d83599c8ed610494f5c3942cea23779a7d2`
- https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/resolucao10232026c.pdf — SHA-256 `24f8d7ad7cd934041db4b1f1d7a2c9c97880f1a525378cf150285831c6c3f89d`
- https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/resolucao10312026.pdf — SHA-256 `9f6d8d8c6e7b7996fce21326a229c9b262d45178907fff7bd9dd159ca5ba9b5a`
- https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/mbvt20222.pdf — SHA-256 `5214bc8cfa3c8d1b5acde7c34c3c27f2d06ea1ba1589f5bbfab5ae041e06b1b2`
- https://www.gov.br/transportes/pt-br/assuntos/transito/arquivos-senatran/portarias/2022/Portaria3542022AnexoN3.pdf — SHA-256 `83ecb895fa2e95878bcb8818b67e896dd7768bede507a162b5851c51f7334d7e`
