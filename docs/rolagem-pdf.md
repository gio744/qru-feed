# Rolagem contínua dos PDFs

O leitor apresenta páginas numa lista vertical contínua. O usuário desliza para atravessar a divisão entre páginas e mantém anterior/próxima, primeira/última, anexos, salto por número e ampliação. A página indicada no cabeçalho acompanha a primeira página visível.

As imagens são criadas apenas para os itens próximos da tela. A renderização é serializada por leitor e executada fora da interface; cada renderizador/página é fechado após o uso. Não pré-renderiza o manual inteiro. Páginas fora da composição podem ter suas imagens liberadas pelo gerenciamento de memória.

A cópia local é verificada por SHA-256 antes de abrir. Há nova tentativa tanto para a abertura do documento quanto para falha isolada de página. A posição salva exige que a página atual tenha sido renderizada. Links com página explícita continuam tendo precedência na abertura.

Validação: compilação Android, suíte JVM existente de fontes e posições e verificação do APK no CI. Rolagem por gesto, ampliação horizontal, consumo de memória e retomada em aparelho real ainda exigem confirmação após a instalação. Não foram medidos FPS ou memória neste bloco.
