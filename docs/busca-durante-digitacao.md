# Busca durante a digitação

Alterar o campo agenda a consulta após 180 ms sem nova alteração. Digitar novamente cancela a consulta anterior; limpar o campo cancela e limpa o resultado. Toque na lupa, ação do teclado e pesquisa por voz executam imediatamente, sem a espera.

Cada consulta recebe uma geração. Somente a geração atual publica resultado ou erro, inclusive quando um mecanismo de busca não coopera com o cancelamento. Mensagens anteriores são apagadas ao iniciar nova consulta. A busca continua no repositório existente, fora da thread de interface.

Não altera indexação, normalização, ranking ou catálogo. Os testes controlam a espera sem depender de temporizadores reais e incluem resposta e erro tardios, limpeza, nova tentativa e execução imediata.

Validação exigida: compilação Android, testes JVM e integridade do APK. Digitação, voz e foco do teclado precisam de confirmação visual no aparelho na próxima instalação.
