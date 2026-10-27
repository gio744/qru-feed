# QRU TRÂNSITO — Congelamento do escopo V1 (27/10/2026)

## Objetivo
Entregar uma versão Android funcional, verificável e tecnicamente confiável. Esta branch é uma base de estabilização, não uma declaração de prontidão comercial.

## Escopo obrigatório
- Busca operacional por artigo, inciso, código e expressão comum; testes de regressão do ranking.
- Fichas e referências com indicação de revisão e fonte; nenhuma ficha pendente deve ser apresentada como juridicamente homologada.
- Leitura interna do CTB e Crimes e acesso a Normas com situação normativa distinguindo vigente, parcialmente revogada, revogada e não verificada.
- Navegação aprovada: CTB | Normas | Q/Mais | Situações | Novidades, busca superior.
- QRU Studio: mostrar somente vídeos reais disponíveis; downloads e integridade precisam ser testados.
- Identidade visual aprovada e ícone após verificação em dispositivo.

## Fora do escopo V1
- Novos planos comerciais, permissões e precificação.
- Novas categorias, redes sociais, publicação automática e recursos não testados.
- Alegações de sincronização em tempo real ou cobertura jurídica completa sem evidências.

## Gate obrigatório
1. Conciliação manual das alterações sobrepostas da PR #75 com esta branch, sem substituição integral de QruApp.kt, CatalogSearch.kt ou Theme.kt.
2. Compilação Android e testes unitários no commit consolidado.
3. Inspeção de navegação e regressão de busca.
4. Auditoria das fontes e vigências normativas, sinalizando conteúdo não verificado.
5. Teste de instalação/atualização, downloads e experiência no aparelho físico.
6. Somente então avaliar merge e disponibilização da versão.

## Evidência atual
O workflow Android da branch de origem concluiu com sucesso em 27/10/2026 (run 40548302198). Isso não valida automaticamente esta branch após futuras alterações, nem os testes físicos.
