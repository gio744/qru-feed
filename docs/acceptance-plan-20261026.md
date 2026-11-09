# QRU — critérios de consolidação para a próxima entrega (26/10/2026)

## Decisão de produto
Não exigir instalação ou download enquanto o usuário estiver em computador de terceiros. Continuar por PRs isolados, com CI obrigatório e sem mesclar rascunhos automaticamente.

## Navegação canônica proposta no PR #75
- Busca superior acessível imediatamente, com foco na consulta operacional.
- Rodapé: CTB | Normas | Q | Situações | Novidades.
- Q central: atalho de ações rápidas/área Mais, sem duplicar campo de busca.
- Não acrescentar banner normativo na página inicial.
- Manter categorias da área Mais explicitamente marcadas como em preparação quando ainda não funcionais.
- Não trocar a identidade visual principal por tema experimental antes de revisão em dispositivo.

## Critérios de aceitação
1. Busca exata por artigo/inciso/código prevalece sobre similaridade textual; consultas por termos práticos permanecem úteis.
2. CTB e Crimes abrem leitura interna, sem redirecionamento externo inesperado.
3. Normas distinguem vigente, parcialmente revogada, revogada e situação não verificada; nunca inferir vigência apenas por número ou presença do PDF.
4. Referências oficiais exibem origem e data de conferência, quando verificadas.
5. Fichas ainda não revisadas não recebem selo de aprovação jurídica.
6. Situações não exibem vídeos fictícios; links e downloads somente quando efetivamente disponíveis.
7. Novidades diferenciam conteúdo embarcado de atualizações recebidas do servidor; sem alegar tempo real sem sincronização comprovada.
8. Testes de navegação, build e testes unitários devem passar no commit final, além de inspeção em aparelho físico antes de lançamento.
9. Não alterar preços, assinaturas ou permissões comerciais nesta etapa.
10. Novo ícone QRU prata fosca exige recursos adaptive e teste em diferentes launchers/densidades; não confundir protótipo com arte final incorporada.

## Pendências de conteúdo documentadas
- README em 18/10 relata 418 referências pesquisáveis, todas com validação operacional pendente.
- Ficha 551-71 sem intervalo individual confirmado (issue #36).
- Vídeos ainda não disponíveis no app.
- Atualizações normativas automáticas e publicador não devem ser declarados ativos apenas com base no código.

## Ordem de execução
1. Consolidar PR #75 após testes no commit mais recente.
2. Revisar busca e fichas com amostra operacional, corrigindo inconsistências comprovadas.
3. Validar vigência e fontes das normas.
4. Integrar identidade visual final e ícone.
5. Testar instalação/atualização no dispositivo do usuário quando disponível.
