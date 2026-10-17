# Modelo editorial de ficha operacional

O modelo `OperationalReference` separa aplicabilidade, exceções, observações e medidas. Cada afirmação aponta para URL e localização na fonte. Cada medida pode registrar condições, prazo fundamentado e dependência de sistema.

`editorialIssues()` verifica completude estrutural, não validade jurídica, autenticidade da fonte ou competência do revisor. A revisão começa pendente. Preencher os campos não a aprova; declarar aprovação também não elimina pendências abertas ou evidências ausentes.

Este bloco prepara a estrutura Kotlin e seus testes. Não conecta esse modelo à busca, ao banco, ao formato dos pacotes ou à interface. Não importa os rascunhos documentais, não altera as 418 referências e não modifica o controle de ativação do backend. Integração e apresentação dependem de uma mudança posterior com conteúdo validado.

Validação exigida: compilação Android e testes JVM no CI. Os testes usam textos fictícios e não constituem fichas jurídicas.
