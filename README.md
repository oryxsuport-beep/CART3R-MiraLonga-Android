# CART3R Mira Longa V5 — Simulador auxiliar para Android

Aplicativo independente Kotlin com linhas de referência ajustáveis, inspirado nos vídeos do usuário. **Não modifica o APK do 8 Ball Pool**, não acessa memória ou dados do jogo, e não possui bypass de anticheat.

## Melhorias da V5
- **Toque simultâneo no jogo**: o painel é minimizável, a camada com as linhas fica sempre não-interativa, e só os pequenos pontos vermelhos de ajuste recebem toques.
- **Raio menor**: só 9 ou 6 (medidas em dp), por padrão 9, como no vídeo.
- **Bolas extras**: adicione até 7 bolas de colisão e arraste cada marcador; apague a última quando quiser.
- **Modelo de contato**: escolhe o primeiro contato ao longo da trajetória, inclusive depois de um ou mais rebotes, calcula direções aproximadas após o primeiro choque e mostra a segunda colisão entre bolas quando presente.
- **Linhas vermelhas** para as seis caçapas e guias de uma tabela, ajustáveis manualmente.
- **Até 3 rebotes**, calibração das quatro bordas, controle de mira e bola-alvo.

## Instalação
No GitHub Actions, abra a última execução de **Android - Compilar APK**, faça download de `CART3R-MiraLonga-v5-debug`, extraia o ZIP e instale `app-debug.apk` no Android. A V5 usa identificador de pacote próprio e pode coexistir com versões anteriores.

## Usar sem bloquear o jogo
1. Abra o aplicativo; autorize a permissão **Aparecer sobre outros apps**.
2. Toque em **Iniciar mira longa**.
3. Na barra flutuante, toque em **Calibrar 4 bordas**: aparecem somente quatro marcadores arrastáveis. Fora deles, o toque passa para o jogo.
4. Toque em **Mover mira / branca** e ajuste os dois marcadores.
5. Toque em **Mover bola-alvo** para posicionar a bola. Toque em **Adicionar colisão** para criar bolas extras; use **Remover última colisão** para desfazer.
6. Ajuste o raio entre **9 e 6** e a quantidade de rebotes. O botão de cabeçalho minimiza o painel.
7. Toque em **Liberar toques** para remover marcadores de edição e continuar jogando. O painel permanece disponível.

## Física e precisão
A física simulada é **aproximada**, com colisões elásticas ideais de massas iguais e reflexão especular nas tabelas. O aplicativo não mede força, atrito real, spin, condições das tabelas, tamanhos físicos das caçapas ou a física exata usada pelo jogo. Não reconhece nem segue as bolas automaticamente. Ajuste a mesa e as bolas com cuidado; as linhas para caçapas são referências geométricas, não garantia de acerto. Os controles podem variar em aparelhos e versões do Android. O jogo pode restringir sobreposições, e o uso pode contrariar suas regras.

## Desenvolvedores
`.github/workflows/build-apk.yml` executa testes JUnit e compila `app-debug.apk`. A aplicação não solicita acesso à internet, mídia, acessibilidade, root ou permissões do jogo.
