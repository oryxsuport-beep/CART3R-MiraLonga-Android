# CART3R Mira Longa V4

Aplicativo Android auxiliar Kotlin. Inspirado no vídeo de exemplo fornecido pelo usuário: **linhas vermelhas da bola-alvo até as seis caçapas**, mais guias geométricas para uma tabela. Não modifica o aplicativo do 8 Ball Pool, não lê o jogo e não faz bypass de anticheat.

## Funcionalidades
- Guias de seis caçapas (vermelho forte);
- Até seis rotas geométricas de uma tabela (vermelho transparente);
- Direção de mira longa e previsão idealizada entre duas bolas;
- Calibração manual da mesa, taco, bola-alvo e raio da bola;
- Controles para ligar/desligar as linhas e os rebotes;
- Sobreposição travável que deixa os toques passarem ao app abaixo.

## Como usar no Android
1. Na aba GitHub Actions, abra o último build, baixe o artefato `CART3R-MiraLonga-v4-debug` e extraia `app-debug.apk`.
2. Instale o arquivo no Android. Este build usa ID de pacote próprio da V4, podendo coexistir com V3.
3. Abra o app e permita **Aparecer sobre outros aplicativos**.
4. Toque em **Iniciar mira longa**; em **Calibrar mesa**, ajuste as bordas.
5. Em **Colocar bola**, arraste o ponto azul até a bola que quer mirar.
6. Em **Ajustar mira**, coloque o ponto branco na posição da branca e o outro ponto na direção da tacada.
7. Trave os ajustes. Ligue ou desligue **Seis caçapas**, **Tabelas extras**, e **Rebotes**.

## Limitações
As linhas da caçapa **não indicam acerto automático**; são referências geométricas. Os rebotes assumem ângulo de incidência igual ao ângulo de reflexão. Colisões são modeladas como duas bolas de mesma massa e colisão elástica ideal. Não há detecção automática de bolas, análise do vídeo em tempo real, cálculo de pockets com tamanho real, spin, efeito do taco, atrito e calibração da física do servidor do jogo. Não é equivalente ao mod exibido no vídeo e não promete tacadas perfeitas.

Ao testar sobreposições em jogos, verifique as regras do serviço; seu uso pode resultar em bloqueio.
