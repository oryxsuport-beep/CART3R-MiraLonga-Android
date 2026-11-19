# CART3R Mira Longa V3 (Android)

Aplicativo Android independente em Kotlin para prever **trajetórias aproximadas de bilhar** sem modificar nenhum jogo.

## Funções
- Mira longa ajustável manualmente
- Posicionamento manual de bola branca e bola-alvo
- Simulação geométrica da colisão de duas bolas de mesma massa
- Caminhos pós-colisão em amarelo (branca) e azul (bola-alvo)
- Calibração das quatro bordas da mesa
- Até 3 rebotes ideais e 5 tamanhos de raio da bola
- Botões para travar a sobreposição e liberar toques

## Como usar
1. Execute o build em Actions > Android - Compilar APK. Baixe o artifact CART3R-MiraLonga-v3-debug.
2. Extraia o ZIP e instale app-debug.apk.
3. Abra o aplicativo e autorize a permissão "Aparecer sobre outros apps".
4. Inicie a mira. Calibre as bordas da mesa no botão "Calibrar mesa".
5. Em "Ajustar mira", arraste os 2 marcadores para a branca e a direção da tacada.
6. Em "Colocar bola", toque ou arraste até a posição da bola-alvo.
7. Ajuste o tamanho em "Raio" até combinar com a bola exibida.
8. Selecione 0 a 3 rebotes, conforme necessário.
9. Trave os marcadores e retorne ao jogo; a linha não captura nem controla o jogo.

## Modelo e limites
A aproximação usa colisão elástica de duas massas iguais sem rotação, interseção raio-círculo e reflexão ideal nas tabelas. Não considera força, atrito real, efeito/spin, caçapas, comportamento da rede, colisões com múltiplas bolas nem os parâmetros secretos do motor do 8 Ball Pool. Não há garantia de precisão no jogo real.

Nenhum APK do jogo original, bypass, injeção de código, acesso à memória ou captura de tela está incluído. Sobreposições podem violar regras de jogos online; use somente onde permitido.

## Desenvolvimento
O GitHub Actions executa testes JUnit e compila o APK debug no branch main.
