# Física e detecção: origem e limites

Este projeto é um **app auxiliar independente**. Os gráficos e o script `fisica_8ball.py` recebidos foram usados como referência conceitual, não como prova de que os parâmetros correspondem ao 8 Ball Pool.

## Parâmetros do arquivo de referência

| Grandeza | Referência | Aplicação na V6 |
| --- | ---: | --- |
| Restituição bola–bola | 0,95 | Normal do impacto entre bolas |
| Atrito bola–bola | 0,06 | Impulso tangencial limitado / throw aproximado |
| Restituição de tabela | 0,75 | Componente normal dos rebotes |
| Atrito de deslizamento com pano | 0,20 | Módulo analítico de spin/rolamento, ainda não integrado à mira ao vivo |
| Resistência ao rolamento | 0,010 | Módulo analítico de spin/rolamento, ainda não integrado à mira ao vivo |
| Offset vertical | h/R | Entrada matemática para draw, stun e follow (sem estimativa automática) |

### Colisão
Usamos duas massas iguais, normal `n`, velocidade incidente `v` e restituição `e=0,95`. Impulso normal por unidade de massa: `Jn/m=(1+e)(v·n)/2`. O impulso tangencial é limitado por `min(|v·t|/7, μbb·Jn/m)` com sentido de atrito. A direção pós-impacto da branca e da bola-alvo é calculada a partir desses componentes. Não há simulação completa do giro após a colisão.

### Tabela
O vetor incidente tem componente tangencial mantida e normal invertida com fator `e=0,75`. Isso muda a direção de saída em comparação com espelhamento perfeito. Este coeficiente é **uma escolha inicial ajustável no código**, não uma medição das tabelas do jogo.

### Spin
O arquivo original inclui `k=2,5h/R` e, para stun, a transição para rolamento `v_rol=5/7 v0`. A classe `SpinPhysics` reproduz essa relação e tem testes, mas a V6 **não consegue estimar força e spin a partir de um quadro estático**: portanto, a linha ao vivo não promete prever draw/follow corretamente.

## Detecção automática beta
A V6 usa `MediaProjection` **com consentimento do Android em cada sessão**. Os pixels são processados no próprio celular, não gravados nem transmitidos. O detector:
1. Usa a mesa calibrada manualmente e o raio escolhido (6 ou 9).
2. Estima a cor predominante do pano.
3. Busca componentes conexos com formato e área aproximados de bolas.
4. Tenta identificar a branca pela aparência clara e atualizar as posições das demais bolas.
5. Mantém os ajustes manuais caso não encontre candidatos confiáveis.

A ferramenta pode falhar com bolas escuras, sobreposições, telas protegidas, zoom, mesa colorida, bolas encostadas, rotação da tela, marcações luminosas ou tamanho incorreto do raio. Não lê memória, APK, coordenadas do servidor nem física interna do jogo. Não contém bypass de anticheat.

**Uso responsável:** verifique as regras do jogo sobre sobreposição e ferramentas auxiliares.
