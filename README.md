# CART3R Mira Longa V6 — Android

App auxiliar de previsão **aproximada** de bilhar em Kotlin. Não altera o 8 Ball Pool, não contém bypass nem executa ações dentro do jogo.

## Novidade: detecção automática experimental
A V6 pode detectar possíveis bolas numa imagem da tela, usando o sistema de autorização `MediaProjection` do Android. Isso **exige consentimento em cada sessão** e funciona somente na tela compartilhada e na mesa previamente calibrada. Escolha **tela inteira** na autorização; capturar apenas um aplicativo pode alterar o enquadramento e dificultar a correspondência com a sobreposição.

O processamento da imagem ocorre localmente; nenhuma imagem é salva, enviada a servidores ou compartilhada com terceiros pelo aplicativo. Se a imagem não revelar posições confiáveis, use os controles manuais. Alguns jogos podem impedir captura ou bloquear sobreposições.

## Como instalar e usar
1. Em GitHub Actions, abra o último build e baixe o artefato `CART3R-MiraLonga-v6-debug`.
2. Extraia `app-debug.apk` e instale no Android 8.0 ou superior. A V6 usa pacote próprio, podendo coexistir com a V5.
3. Abra o app, autorize **Aparecer sobre outros apps**, toque em **Iniciar linhas manuais**.
4. Na barra flutuante, calibre as bordas da mesa e selecione raio 6 ou 9.
5. Toque em **Detecção automática: INICIAR** (ou botão 3 do app), autorize a captura de tela.
6. Abra o jogo e observe o contador de candidatas detectadas. Se falhar, interrompa a captura e continue posicionando bolas manualmente.
7. Para encerrar a captura, use **Encerrar detecção automática** ou o controle de captura do próprio Android.

## Mira, colisões e interação
- A camada de desenho é transparente aos toques; somente o painel e as bolinhas de ajuste recebem interação.
- Coloque manualmente a bola-alvo e até sete obstáculos; em modo automático, até 15 candidatas podem ser mostradas.
- Exibe até 3 rebotes, trajetórias aproximadas de até 2 choques e linhas vermelhas das seis caçapas.
- O ângulo do rebote usa restituição estimada da tabela; choque bola-bola inclui restituição e atrito tangencial.
- As trajetórias continuam **estimativas simplificadas**; não são medições da física do jogo, nem incluem a força e o spin em tempo real.

## Física: fontes do usuário
Os gráficos e o script Python fornecidos para análise incluem coeficientes de restituição bola-bola = 0,95, tabela = 0,75 e atrito bola-bola = 0,06. Esses valores são hipóteses do simulador e não foram confirmados como constantes do 8 Ball Pool. O arquivo `docs/FISICA_E_DETECCAO_PTBR.md` explica quais equações foram utilizadas e o que ainda não é previsto.

## Privacidade
A detecção de bolas usa apenas frames temporários na memória. O app não requer acesso à internet, acessibilidade, root ou dados privados do jogo. O Android poderá interromper a projeção ao bloquear a tela; a captura nunca é invisível.

## Compilação
O workflow `.github/workflows/build-apk.yml` executa os testes e gera APK debug (não assinado para distribuição na Play Store). Não foi validado em dispositivo físico. Para reportar bugs, envie vídeo e versão do Android.
