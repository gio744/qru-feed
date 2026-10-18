# Retomada de leitura dos PDFs

Abrir um documento pela biblioteca retoma a última página renderizada com sucesso. A posição é local, indexada pelo SHA-256 da publicação, para não ser aplicada automaticamente a bytes diferentes de uma norma atualizada. Falha ao renderizar não grava a página como lida.

Uma abertura com página explícita, como o botão Ler no QRU de uma ficha, tem precedência sobre a posição salva. O usuário continua dispondo de primeira/última página, salto por número e ampliação. Página salva fora dos limites ou preferência inválida volta ao início.

A mudança não altera o conteúdo, a aprovação das referências ou o backend. Testes JVM cobrem a precedência e os limites. Persistência entre reinícios, navegação e renderização precisam de confirmação no aparelho após a próxima instalação.
