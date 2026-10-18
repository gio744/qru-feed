# Lista de busca sob demanda

A tela principal utilizava uma coluna com rolagem e criava um cartão para cada resultado da consulta, inclusive fora da área visível. A mudança utiliza LazyColumn com identidade estável por referência e tipo comum para os cartões. Cabeçalho e ferramentas permanecem em itens próprios, com a mesma sequência e espaçamento.

Não altera consulta, normalização, pontuação, ordenação, catálogo ou texto dos resultados. A meta é reduzir a composição de cartões fora da tela quando há muitas correspondências. Não há medição de ganho em milissegundos neste bloco.

Validação: compilação, suíte JVM existente (incluindo busca) e verificação do APK no CI. Rolagem, foco do teclado, preservação de posição entre abas e fluidez no aparelho precisam de confirmação na próxima instalação.
