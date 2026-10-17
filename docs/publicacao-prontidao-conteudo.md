# Bloqueio de ativação de conteúdo pendente

A criação de um candidato e a ativação são etapas diferentes. Um pacote assinado com `ready: false` pode ser recebido para conferência. A ativação exige agora `ready: true` no pacote e em cada ficha, além das verificações já existentes de assinatura, estrutura, revisão de impacto e regressão.

Somente o booleano JSON `true` é aceito. Valores ausentes, strings, números, itens inválidos ou lista vazia bloqueiam a ativação. Candidatos antigos sem payload também são bloqueados. A declaração de prontidão não equivale a validação jurídica automática e não substitui os revisores.

## Registro das tentativas

O payload é lido na mesma transação que bloqueia a linha da versão. A avaliação de prontidão é incluída em `activation_checks.content_readiness` no snapshot do dossiê. Uma tentativa com pendências devolve HTTP 409 depois de persistir o dossiê e o evento de auditoria. A versão ativa anterior não é substituída nesse caso.

O diagnóstico registra a quantidade total de fichas pendentes e até 20 exemplos por índice e stable_key. Não inclui credenciais nem assinatura. Nenhum campo do pacote é editado, e nenhuma aprovação é deduzida apenas da presença de fonte, artigo ou código.

## Validação de 17/10/2026

- 32 testes locais aprovados: booleanos estritos, campos ausentes, payload malformado, lista vazia, aprovação parcial, candidato antigo, todos os demais gates passando com conteúdo pendente, manutenção das outras exigências, ativação positiva, recebimento de candidato pendente e snapshot do dossiê.
- Sintaxe dos módulos Python verificada.
- O candidato real de 418 referências foi submetido somente à função local: resultado bloqueado, 418 itens pendentes; arquivo permaneceu idêntico.
- Os testes de ativação usam conexão e cursor simulados. Não foi executada ativação nem gravação no PostgreSQL de produção.
- O workflow `Content publication gates` executa a suíte específica em PRs que alterem esses módulos.

Para executar: na pasta `backend`, instalar `requirements.txt` e executar `python -m pytest tests/test_release_readiness_gate.py -q`.

Pacotes pendentes recebidos anteriormente continuam candidatos para conferência. Para liberar conteúdo, concluir a revisão, gerar um novo pacote com declarações coerentes e nova assinatura, registrar as demais aprovações e só então solicitar a ativação. Não alterar o JSON já assinado.
