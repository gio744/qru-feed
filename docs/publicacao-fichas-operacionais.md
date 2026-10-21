# Publicação controlada de fichas operacionais — QRU

## Estado atual
O arquivo `android/app/src/main/assets/operational/approved-fiches.json` contém `schemaVersion: 1` e `items: []`. **Nenhuma ficha foi juridicamente aprovada.** O arquivo `approved-fiches.sha256` fixa os bytes esperados; alterá-lo juntamente com o pacote não constitui assinatura independente.

## Fluxo editorial obrigatório
1. Criar a ficha como **rascunho**, com código de infração exatamente igual ao catálogo ativo. Nunca preencher campos jurídicos por inferência de um PDF ou de um resultado de busca.
2. Para cada orientação em `whenApplicable`, `exceptions`, `observations`, `measures` e respectivos prazos, registrar texto, URL oficial HTTPS e localizador preciso (artigo, inciso, parágrafo, item do MBFT ou página).
3. Conferir vigência, revogações, redação consolidada, competência do órgão, medida administrativa e exceções. Para pontos controvertidos, registrar pendência em `unresolvedIssues`; a ficha permanece indisponível.
4. Revisão humana por responsável identificado, com data ISO `YYYY-MM-DD`. Apenas depois da aprovação documentada definir `review.status=APPROVED`.
5. Conferir ausência de códigos duplicados e compatibilidade do código com o **catálogo efetivamente ativo**, incluindo sua versão. O código não pode ser liberado só porque apareceu em uma pesquisa anterior.
6. Gerar o JSON do pacote, recalcular SHA-256 dos **bytes finais**, atualizar o arquivo de digest e executar testes automatizados e compilação Android.
7. Revisar a diferença no GitHub e aprovar a alteração antes de integrar à `main`. Disponibilizar APK apenas depois de compilação bem-sucedida.

## Critérios de bloqueio
- Fonte não oficial, incompleta ou sem localizador;
- Texto sem revisão humana ou com questão jurídica não resolvida;
- Código ausente/inativo, duplicado ou incompatível com a versão ativa do catálogo;
- Pacote alterado sem digest correspondente, esquema inesperado ou erro de leitura;
- Compilação/testes falhos.

## Verificações implementadas
- `LegalRepository.isActiveReference` compara chave estável, código, versão, status de revisão, fonte e artigo com o registro ativo. Sem release no banco, consulta o catálogo embutido; com release ativo, consulta exclusivamente a base ativa.
- O parser rejeita pacote com hash divergente, esquema desconhecido, campos inesperados, códigos duplicados, revisão incompleta ou fonte fora da lista oficial autorizada.
- A ausência de ficha aprovada mantém a interface em modo informativo, sem afirmar que existe orientação operacional revisada.

## Checklist de revisão antes da integração
- [ ] Confirmar que todos os workflows do commit de destino concluíram com sucesso, sem jobs pendentes.
- [ ] Revisar alterações do PR, inclusive dependências, origem e SHA-256 dos PDFs.
- [ ] Confirmar que `approved-fiches.json` continua vazio até haver revisão jurídica humana documentada.
- [ ] Testar abertura de ficha individual, pesquisa por código e artigo, e resultado antigo após troca de release.
- [ ] Confirmar que a atualização não será anunciada como publicada antes de integração, distribuição e instalação.

## Limites atuais a resolver
- A interface verifica o resultado selecionado contra o catálogo/release efetivamente ativo antes de liberar uma ficha. O `setOf(code)` passado ao parser apenas restringe a extração à ficha selecionada; **isoladamente não comprova elegibilidade**. A verificação em `LegalRepository.isActiveReference` é obrigatória e deve continuar protegida contra regressões.
- O hash e o pacote residem no mesmo aplicativo: o SHA-256 detecta divergência acidental, mas não autentica o publicador.
- O pacote inicial não contém fichas aprovadas; não anunciar orientações operacionais como revisadas.
- O build baixa PDFs oficiais e pode falhar por indisponibilidade externa. As tentativas de repetição não dispensam hash válido.
