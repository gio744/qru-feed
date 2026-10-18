# QRU? — QUAL A SITUAÇÃO

Aplicativo Android de consulta de referências de trânsito, com catálogo e documentos embarcados para leitura sem internet. Estado documentado em 18/10/2026.

## Recursos implementados

- Busca automática durante a digitação, por voz, artigo, código e termos práticos. A coordenação de requisições impede que uma consulta antiga substitua a mais recente.
- 418 referências pesquisáveis no catálogo embarcado. A busca de “pneu” prioriza 672-61; consultas por artigo e inciso priorizam a correspondência exata.
- Fichas individuais com páginas delimitadas para 417 referências. A 551-71 permanece sem intervalo individual confirmado; sua investigação está na [issue 36](https://github.com/gio744/qru-feed/issues/36).
- Biblioteca offline com dez PDFs: o manual original e nove resoluções. A biblioteca permite abrir o documento completo; a ficha pesquisada usa somente o intervalo associado à referência.
- PDFs com rolagem contínua, pinça, controles de zoom, salto para página, primeira/última e retomada da leitura do documento completo. A ficha não substitui a posição salva do manual.
- CTB com parte geral e crimes em leitura interna. O catálogo de infrações permanece na busca.
- Descrição de ocorrência e análise guiada de habilitação, ambas em conferência.
- Novidade inicial por publicação, com “Não mostrar esta novidade novamente” marcada por padrão. O conteúdo permanece na aba Novidades. A preferência é local; limpar dados ou desinstalar elimina esse histórico.
- Situações com atalhos de consulta. Vídeos ainda não estão disponíveis.

A disponibilidade das fontes offline é definida pelo manifesto em `android/app/src/main/assets/norms/pdf-catalog.json`. Os intervalos individuais estão em `reference-page-ranges.json`. A seleção exige correspondência de código, fonte e hash; não deve usar páginas aproximadas ou de outra publicação.

## Limites de conteúdo e publicação

As **418 referências continuam com validação operacional pendente**. Cobertura de busca, fonte oficial e intervalo de PDF não significam aprovação jurídica. Os documentos são publicações originais, não uma consolidação automática de alterações posteriores.

O aplicativo consulta o catálogo embarcado quando não existe versão ativa no banco local. Uma versão assinada ativa é consultada sem misturar automaticamente o índice editorial. O recebimento de candidato pelo publicador não ativa conteúdo no aplicativo.

A novidade atual é embarcada no APK; não representa monitoramento normativo automático. Configuração do servidor e código de sincronização não comprovam, por si, publicação de pacote ativo. Não registrar senhas, hashes de autenticação ou chaves privadas neste repositório.

## Compilar

Na raiz:

```bash
gradle -p android -PqruParallelTest testDebugUnitTest assembleDebug
```

A opção `qruParallelTest` usa `br.com.qru.testeapp` e o certificado exclusivamente de desenvolvimento em `android/test-signing/qru-debug.jks`, mantendo compatibilidade entre atualizações de teste. Sem a propriedade, o identificador permanece `br.com.qru.transito`.

Java/Kotlin 17, Gradle 8.9, SDK 35 e Android mínimo API 28. Os PDFs são obtidos no build e conferidos por SHA-256. Room exporta o esquema em `android/app/schemas`.

## Validação Android

O workflow **QRU Android Build** executa testes JVM e compila o APK. O workflow **QRU Android UI** executa testes instrumentados em emulador API 29 / Pixel 2, com relatórios e logcat preservados.

Com um emulador descartável conectado:

```bash
bash scripts/run_android_ui_tests.sh
```

O script desliga Wi-Fi e dados móveis desse emulador; o computador continua conectado para obter dependências de compilação. A classe `ReaderAndNewsUiTest` cobre PDF, novidade dispensada após recriação de Activity e o fluxo busca → ficha → retorno.

Os resultados do CI devem ser conferidos por commit. Esses cenários não medem FPS ou consumo de memória, não simulam encerramento do processo e não substituem a validação em aparelhos físicos de fabricantes diferentes.

## Servidor

Backend FastAPI/PostgreSQL com migrações, autenticação administrativa e verificação de pacotes assinados. O publicador está na rota `/publicador`; a configuração usa variáveis de ambiente. A senha digitada no login e o hash de senha configurado no servidor têm funções distintas.

O Dockerfile usa a raiz como contexto:

```bash
docker build -f backend/Dockerfile .
```

É necessário PostgreSQL persistente, migrações, segredo JWT e chave pública válidos. Consulte os arquivos de configuração e os resultados atuais de execução antes de declarar o servidor pronto. Documentos antigos em `docs/` registram etapas históricas e podem descrever pendências já superadas.
