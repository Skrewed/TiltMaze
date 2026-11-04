# 🌀 Labirinto por Inclinação (Tilt Maze)

[![Platform](https://img.shields.io/badge/Plataforma-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Linguagem-Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.0%2B-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue?style=for-the-badge)](https://developer.android.com/about/dashboards)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-33%20%2F%2034-blue?style=for-the-badge)](https://developer.android.com/)
[![License](https://img.shields.io/badge/Licen%C3%A7a-MIT-green?style=for-the-badge)](LICENSE)

> Um jogo nativo Android desenvolvido em **Java puro**, com estética **clean, moderna e minimalista**. O jogador guia uma esfera física através de uma campanha de **15 fases progressivas** com labirintos gerados proceduralmente inclinando o smartphone, com respostas físicas realistas de gravidade, atrito, colisões, névoa de guerra (*fog of war*), vagalumes dinâmicos, paredes vivas e buracos negros.

---

## ✨ Destaques & Campanha de 15 Fases

- **🎯 Controle por Giroscópio & Fusão de Sensores:**
  - Leitura de alta precisão via `Sensor.TYPE_ROTATION_VECTOR` com matriz de rotação (`SensorManager.getRotationMatrixFromVector`), eliminando o desvio cumulativo (*gyro drift*).
  - Filtro passa-baixa exponencial (*Low-Pass Filter*) de resposta imediata (`ALPHA = 0.45f`) com zona morta refinada para 0.4°, garantindo movimentação instantânea sem atrasos.
  - Fallback automático para `Sensor.TYPE_GRAVITY` e `Sensor.TYPE_ACCELEROMETER`.

- **⚡ Motor Físico Turbinado em Java:**
  - Simulação ágil e de alta resposta (`TILT_ACCELERATION = 3800f`, `FRICTION = 2.4f`), tornando a bolinha veloz e altamente responsiva a qualquer leve inclinação do aparelho.
  - Integração contínua de sub-passos com resolução de colisão inelástica em repouso.
  - Sincronização concorrente thread-safe (`lockFisica`) e guarda de tempo mínimo, eliminando falhas de vitória prematura na virada de fase.
  - Detecção precisa de vitória e colisões com vórtices e alvos.

- **🗺️ Progressão de Dificuldade (15 Fases):**
  - **Fases 1 a 4 (Aquecimento & Escalonamento)**:
    - O mapa cresce progressivamente: 5x8, 6x10, 7x12 e 8x14.
    - Visão clara do tabuleiro inteiro para o jogador se habituar ao controle fino da física.
  - **Fase 5 em diante (Mapas Procedurais & Névoa de Guerra - Fog of War com Blur)**:
    - O tamanho do labirinto estabiliza na proporção ideal de tela de 9x16.
    - A cada nova abertura ou rodada, **um novo mapa 100% procedural é gerado**. O ponto de partida (spawn) e o buraco verde (goal) são sorteados aleatoriamente, com garantia algorítmica de grande distância entre si (BFS).
    - **Fog of War com Efeito Esfumaçado (Blur)**: escuridão cobrindo o tabuleiro com abertura de luz de bordas suaves e esfumaçadas em gradiente radial (`PorterDuff.Mode.DST_OUT`), simulando uma tocha/lanterna atmosférica realista.
    - O raio da luz **diminui gradativamente a cada fase**, caindo de ~3.4 células na fase 5 até ~1.3 célula na fase 15.
  - **Fase 5 a 15 (Vagalumes Bioluminescentes & Rastro de Partículas)**:
    - Vagalumes super charmosos cruzam a tela com vôo fluido e orgânico baseado em curvas harmônicas senoidais.
    - Cada vagalume possui corpo bioluminescente pulsante, núcleo incandescente e asinhas translúcidas que batem em alta frequência.
    - Conforme voam, soltam um rastro contínuo de **partículas douradas cintilantes (fairy dust)** gerenciadas por uma pool de memória com Zero GC, que iluminam suavemente o caminho e as paredes do labirinto no escuro.
  - **Fase 8 em diante (Labirinto Vivo - Paredes Móveis)**:
    - O labirinto se reconfigura sozinho durante o jogo!
    - Paredes internas começam a pulsar em cor âmbar de aviso e, após 2.5 segundos, alternam seu estado (abrem ou fecham).
    - **Garantia matemática de solvabilidade**: o algoritmo BFS testa a rota antes de confirmar a mudança, impedindo que a bolinha seja encurralada sem saída.
  - **Fase 10 em diante (Buracos Negros Aleatórios)**:
    - Vórtices negros surgem e desaparecem periodicamente em posições aleatórias do mapa.
    - Se a bolinha for sugada por um buraco negro ativo, ela sofre aceleração de sucção, vibração tátil dupla, efeito sonoro descendente e é **teleportada de volta ao ponto inicial da fase**.
  - **Fase 15 (Grande Vitória)**:
    - Completar a 15ª fase exibe a tela de vitória suprema com arpeggio sonoro e estatísticas completas da campanha.

- **🎨 UI & UX Minimalista (Clean Dark Aesthetic):**
  - Paleta *OLED Charcoal* (`#121316`) com destaques em Ciano Elétrico (`#00E5FF`), Âmbar (`#FF9100`) e Menta Esmeralda (`#00E676`).
  - Menu inicial direto e sem distrações com apenas os botões **"INICIAR"** e **"SAIR"**.
  - HUD no jogo limpo e sem poluição visual, exibindo apenas o botão **"MENU"**, indicador de fase e cronômetro central, e o botão **"REINICIAR"**.
  - Renderização via camadas (`saveLayer` + `PorterDuff.Mode.DST_OUT`) a 60-120 FPS com Zero GC.

- **🔊 Áudio Procedural em Tempo Real & Hápticos:**
  - **Sintetizador Procedural (`AudioTrack`)**: gera tons senoidais de choque de parede, arpeggios harmônicos de vitória e *whoosh* descendente de queda no buraco negro em PCM de 16 bits puro, sem necessidade de arquivos externos de áudio.
  - **Vibração Tátil (`VibrationEffect`)**: micropulsos proporcionais à velocidade do impacto contra paredes e padrão duplo com pausa na captura por buracos negros.

---

## 🏛️ Arquitetura do Projeto

```
c:\Users\Misharin\Desktop\Java\
├── app/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/labirinto/tiltmaze/
│       │   │   ├── ActivityPrincipal.java       # Menu Principal minimalista (Iniciar / Sair)
│       │   │   ├── ActivityJogo.java            # Ciclo de vida da partida, sensores e HUD
│       │   │   ├── view/
│       │   │   │   ├── VisaoJogo.java           # SurfaceView: renderização, Fog of War e luzes
│       │   │   │   └── ThreadJogo.java          # Loop de simulação desacoplado da UI
│       │   │   ├── elementos/
│       │   │   │   ├── Vagalume.java            # Entidade de vagalume com física orgânica e luz
│       │   │   │   └── BuracoNegro.java         # Vórtice dinâmico com ciclo de vida e sucção
│       │   │   ├── engine/
│       │   │   │   ├── MotorFisico.java         # Dinâmica de movimento, colisões e repouso
│       │   │   │   ├── ControladorSensores.java # Fusão de sensores, calibração e filtro
│       │   │   │   └── Bolinha.java             # Entidade da esfera (x, y, vx, vy, raio)
│       │   │   ├── maze/
│       │   │   │   ├── Labirinto.java           # Grade de células, solvabilidade BFS e paredes móveis
│       │   │   │   ├── CelulaLabirinto.java     # Célula individual com paredes e alertas
│       │   │   │   └── GeradorLabirinto.java    # Gerador procedural DFS com distanciamento BFS
│       │   │   └── util/
│       │   │       ├── GerenciadorVibracao.java # Feedback tátil calibrado
│       │   │       └── GerenciadorSom.java      # Sintetizador procedural PCM em tempo real
│       │   └── res/
│       │       ├── layout/                      # Layouts XML modernos (ConstraintLayout)
│       │       ├── values/                      # Strings (pt-BR), cores e temas Dark
│       │       └── drawable/                    # Ícones vetoriais, gradientes e seletores
│       └── test/java/com/labirinto/tiltmaze/
│           ├── GeradorLabirintoTest.java        # Teste de BFS, escalonamento e solvabilidade dinâmica
│           ├── MotorFisicoTest.java             # Teste de dinâmica, eixos, buracos negros e raio de luz
│           └── VagalumeTest.java                # Teste de vôo orgânico, ciclo de vida e partículas do rastro
├── build.gradle
├── settings.gradle
└── gradlew.bat
```

---

## 🚀 Como Executar

### Pré-requisitos
- **Android Studio** (compatível com Android Studio Quail 3 / Hedgehog / Iguana / Ladybug).
- **JDK 17** (OpenJDK ou Oracle JDK).
- Dispositivo físico Android (recomendado para usar o giroscópio real) ou Emulador Android com a aba *Virtual Sensors* ativada.

### Opção 1: Via Android Studio
1. Abra o **Android Studio**.
2. Clique em **File ➔ Open** e selecione a pasta raiz deste repositório (`C:\Users\Misharin\Desktop\Java` ou `C:\Users\Misharin\Desktop\TiltMaze`).
3. Aguarde o Gradle sincronizar as dependências automaticamente.
4. Conecte seu smartphone via USB com a **Depuração USB** habilitada.
5. Clique no botão **Run (▶)** na barra superior.

### Opção 2: Via Linha de Comando (Gradle)
Para compilar o APK de desenvolvimento:
```powershell
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```
O APK gerado estará disponível em:
`app/build/outputs/apk/debug/app-debug.apk`

Para instalar diretamente no aparelho conectado via `adb`:
```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Testes Automatizados

O projeto inclui suíte completa de testes unitários para o motor físico, a geração procedural e as novas mecânicas:

```powershell
.\gradlew.bat test
```

| Teste | Descrição |
| :--- | :--- |
| `testMazeDimensionsAndBounds` | Valida inicialização correta de limites e coordenadas do labirinto |
| `testAllCellsReachableFromStart` | Garante que **100% das células são alcançáveis** via busca em largura (BFS) sem áreas isoladas |
| `testLevelScalingUpToStage5` | Valida escalonamento progressivo de 5x8 até 9x16 nas 15 fases |
| `testRandomStartAndDistantGoalStage5AndAbove` | Garante posições aleatórias de spawn e saída com grande distância Manhattan/passos a partir da fase 5 |
| `testReconfigurationPreservesSolvability` | Valida que a movimentação dinâmica de paredes na fase 8+ **nunca quebra a solução do labirinto** |
| `testTiltRollRightAcceleratesBallRight` | Valida aceleração horizontal positiva ao inclinar para a direita |
| `testTiltRollLeftAcceleratesBallLeft` | Valida aceleração horizontal negativa ao inclinar para a esquerda |
| `testPitchForwardAcceleratesBallUp` | Valida aceleração vertical para frente/topo ($-Y$) ao inclinar o celular para frente |
| `testPitchBackwardAcceleratesBallDown` | Valida aceleração vertical para trás/base ($+Y$) ao inclinar para trás |
| `testInvertAxesToggles` | Valida a inversão independente de eixos |
| `testRestingOnWallDoesNotProduceInfiniteHits` | Garante que a esfera em repouso contra a parede não produz sons contínuos |
| `testWallCollisionConstrainsPosition` | Garante que a esfera nunca penetra paredes |
| `testGoalDetection` | Valida o disparo da vitória ao atingir a célula final |
| `testBlackHoleCapturesBallAndResetsToStart` | Garante que o buraco negro ativo teleporta a esfera de volta ao spawn e zera a velocidade |
| `testInactiveBlackHoleDoesNotCapture` | Garante que buracos em transição inicial não capturam prematuramente |
| `testShrinkingFogOfWarRadiusFormula` | Valida o encolhimento progressivo do raio de luz da bolinha entre as fases 5 e 15 |
| `testVagalumeMovementAndFluidFlight` | Valida a dinâmica suave de vôo em curvas harmônicas e limites de tela |
| `testParticleTrailSpawnsAndRecycles` | Garante nascimento e reciclagem sem vazamentos da pool de partículas do rastro |
| `testPulseAndWingBeat` | Valida pulso luminoso senoidal do abdômen e batimento das asinhas |
| `testLifeCycleAndOpacity` | Valida fade-in, sustentação e término de ciclo de vida do vagalume |

---

## 🛠️ Tecnologias Utilizadas

- **Java 17**
- **Android SDK (compileSdk 33 / minSdk 24 / targetSdk 33)**
- **AndroidX & Material Components**
- **Android Hardware Sensor API (`SensorManager`, `SensorEventListener`)**
- **Android Canvas & Hardware Accelerated Graphics (`SurfaceView`, `PorterDuff.Mode.CLEAR`)**
- **Android AudioTrack API (Áudio PCM estático em tempo real)**
- **Android Vibrator / VibrationEffect**
- **JUnit 4**

---

## 📄 Licença

Este projeto é distribuído sob a licença **MIT**. Consulte o arquivo `LICENSE` para mais detalhes.
