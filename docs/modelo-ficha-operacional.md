# Modelo editorial de ficha operacional

O modelo `OperationalReference` separa **quando autuar** (`whenApplicable`), **quando não autuar / exceções** (`exceptions`), observações e medidas administrativas. Cada afirmação requer uma URL HTTPS e um localizador da fonte. Medidas podem registrar condições, prazo fundamentado e dependência de sistema.

## Condições estruturais para publicação

- Código de enquadramento no formato `000-00`.
- Aplicabilidade e avaliação de exceções presentes, com texto e evidências completas.
- Revisor identificado, data ISO válida que não esteja no futuro, situação `APPROVED` e nenhuma pendência em aberto.
- Medidas diferentes de `NONE` acompanhadas de condições; prazos positivos e fundamentados; dependências de sistema preenchidas quando declaradas.
- A aprovação editorial **não** decorre automaticamente de preencher os campos: exige decisão explícita do responsável técnico.

`editorialIssues()` verifica apenas **completude estrutural**. Não valida se a norma está vigente, se o conteúdo jurídico está correto, se o localizador corresponde ao texto legal, nem a competência de quem revisou. Os testes utilizam textos fictícios, não orientações de fiscalização.

## Interface Android e situação atual

A tela de referência já apresenta as seções **Quando autuar**, **Quando não autuar / exceções** e **Medidas administrativas**, com avisos de conteúdo pendente. A consulta à publicação oficial e a abertura do trecho individual de PDF offline continuam disponíveis quando houver mapeamento.

**Limitação importante:** o modelo editorial Kotlin ainda não está conectado ao catálogo das 418 referências, ao banco de dados, ao publicador ou ao mecanismo de ativação. A interface mostra seções e avisos, **não** os dados de `OperationalReference`. Portanto, nenhuma das 418 referências deve ser descrita como ficha operacional integralmente revisada por causa dessas alterações.

## Próximas etapas

1. Definir formato versionado de pacote editorial, com origem, revisão e vigência.
2. Implementar importação em modo rascunho, com validação de esquema e rastreabilidade.
3. Vincular somente pacotes aprovados à ficha individual, preservando o aviso de pendência nos demais.
4. Criar testes de não publicação para dados incompletos, fontes inválidas, conflitos de vigência e versões antigas.
5. Submeter o conteúdo jurídico das primeiras fichas à conferência técnica, sem aprovação automática.

Validação técnica: compilar Android e executar testes JVM no CI a cada alteração. A aprovação do CI não equivale a revisão jurídica.
