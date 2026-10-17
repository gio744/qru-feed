# Fontes das referências dentro do QRU

O botão **Ler no QRU** abre a publicação original incluída no aplicativo, diretamente na página física indicada pela referência. A opção de consultar a publicação online continua disponível.

## Cobertura deste bloco

- Manual-base MBFT de 2022: 830 páginas, 17.077.053 bytes, SHA-256 `5214bc8cfa3c8d1b5acde7c34c3c27f2d06ea1ba1589f5bbfab5ae041e06b1b2`.
- Nove resoluções completas incluídas, preservando as cópias e seus hashes. As novas cópias são a Resolução 913/2022 (4 páginas; 88.876 bytes; SHA-256 `3542071abf17e71c2e858712bd2e53b0348c59d129aa70ae4f79198e07622637`) e a Resolução 1.025/2026 (14 páginas; 256.961 bytes; SHA-256 `fa6ffc0346400e0ac5760bc663e0d6efc5e466dfc5819701f772db1754c09f9c`).
- 417 das 418 fontes do candidato correspondem exatamente a um desses dez documentos. Todas as páginas indicadas estão dentro dos limites registrados.
- 551-71 continua com abertura externa porque a fonte registrada é uma minuta. Não substituímos essa fonte silenciosamente pelo manual nem removemos sua pendência de conferência.

O manual aparece na categoria **Manuais**, identificado como publicação original de 2022. As nove resoluções permanecem na categoria **Resoluções**, com contagem derivada do catálogo. A cópia da Resolução 913/2022 é a publicação original: não incorpora silenciosamente retificações ou alterações posteriores. Incluir uma fonte offline não altera a prontidão ou a vigência de qualquer ficha.

## Correspondência de documentos

A resolução local exige HTTPS, mesmo host e caminho exato do catálogo incluído. Aceita apenas página inteira positiva e dentro do documento. Rejeita host diferente, credenciais na URL, porta não padrão, consulta ambígua, fragmento não reconhecido e página fora dos limites. Não troca uma resolução posterior pelo manual-base. Quando a fonte não corresponde a uma cópia local conhecida, a consulta externa permanece disponível.

O leitor recebe a página inicial e mantém os controles existentes de primeira/última página, salto por número, navegação e ampliação. Renderiza uma página por vez. O PDF adicional é obtido na compilação e só é incluído se seus bytes corresponderem ao SHA-256 fixado; o aplicativo instalado não precisa baixar esse arquivo.

## Validação

- Cobertura local das 418 URLs e limites de página conferidos contra o catálogo do candidato.
- Hash do manual baixado novamente corresponde à conferência documental de 17/10/2026.
- Testes JVM adicionados para página inicial/final, URL exata, isolamento entre publicações, URLs ambíguas, limites e catálogo incluído.
- Compilação Android e suíte JVM devem passar no CI antes da integração.
- Navegação visual, renderização e desempenho no aparelho ainda precisam ser confirmados após instalar esta versão.

As fontes são publicações originais. A revisão de alterações, vigência e conteúdo operacional continua pendente. Nenhum pacote foi ativado por esta mudança.
