# Zoom por pinça no PDF

O leitor aceita gesto com dois dedos para ampliar/reduzir de 1× a 4×. Um dedo continua destinado à rolagem vertical ou horizontal. Depois de iniciar uma pinça, o gesto é consumido até soltar os dedos, evitando que o final da pinça vire uma rolagem involuntária.

Os controles inferiores oferecem menos, escala atual (toque para ajustar à largura), mais e voltar. A mesma regra de limites é usada por botões e pinça. Amostras inválidas do gesto não alteram a escala. O gesto não troca páginas nem remove os limites da ficha individual.

Validação: quatro testes da escala e seus limites, compilação, suíte JVM e integridade do APK no CI. Reconhecimento da pinça, deslocamento horizontal e fluidez ainda precisam de confirmação no aparelho; os testes JVM não simulam toque Android.
