# 🌀 Labirinto por Inclinação (Tilt Maze)

[![Platform](https://img.shields.io/badge/Plataforma-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Linguagem-Java%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.0%2B-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue?style=for-the-badge)](https://developer.android.com/about/dashboards)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-33%20%2F%2034-blue?style=for-the-badge)](https://developer.android.com/)
[![License](https://img.shields.io/badge/Licen%C3%A7a-MIT-green?style=for-the-badge)](LICENSE)

> Um jogo nativo Android desenvolvido em **Java puro**, com estética **clean, moderna e minimalista**. O jogador guia uma esfera física através de um labirinto gerado proceduralmente inclinando o smartphone, com respostas físicas realistas de gravidade, atrito, colisões e feedback háptico.

---

## ✨ Destaques & Funcionalidades

- **🎯 Controle por Giroscópio & Fusão de Sensores:**
  - Leitura de alta precisão via `Sensor.TYPE_ROTATION_VECTOR` com matriz de rotação (`SensorManager.getRotationMatrixFromVector`), eliminando o desvio cumulativo (*gyro drift*).
  - Filtro passa-baixa exponencial (*Low-Pass Filter*) para amortecer ruídos e trepidações.
  - Fallback automático para `Sensor.TYPE_GRAVITY` e `Sensor.TYPE_ACCELEROMETER`.
  - **Botão "Calibrar"**: permite definir a posição neutra de repouso em qualquer ângulo que o usuário estiver segurando o aparelho (sentado, deitado ou reclinado).
  - **Ajuste de Eixos**: menu para inverter os eixos vertical e horizontal de forma independente, com persistência via `SharedPreferences`.

- **⚡ Motor Físico Próprio em Java:**
  - Simulação com integração de tempo contínuo (*fixed delta-time* com sub-passos).
  - Atrito cinético de rolamento e aceleração gravitacional por inclinação.
  - **Resolução de Colisão Inelástica em Repouso**: limiar de velocidade para corte de restituição elástica, eliminando micro-quiques infinitos e ruídos contínuos quando a esfera repousa na borda.
  - Detecção precisa de vitória ao alcançar o alvo final.

- **🧩 Geração Procedural de Labirintos:**
  - Algoritmo de *Recursive Backtracking* (DFS) iterativo baseado em pilha.
  - **Garantia matemática**: labirintos perfeitos (sem ciclos, sem regiões inacessíveis e com solução garantida do início ao fim).
  - Dificuldade progressiva com ampliação da grade a cada nova fase concluída.

- **🎨 UI & UX Minimalista (Clean Dark Aesthetic):**
  - Paleta *OLED Charcoal* (`#121316`) com destaques em Ciano Elétrico (`#00E5FF`) e Menta Esmeralda (`#00E676`).
  - Menu inicial direto e sem distrações com apenas os botões **"INICIAR"** e **"SAIR"**.
  - HUD no jogo com cronômetro em milissegundos, indicador de fase, calibração rápida e reinício.
  - Zero alocações de memória dentro do loop de renderização (`SurfaceView`), garantindo taxa de quadros estável (60 a 120 FPS).

- **🔊 Áudio Procedural em Tempo Real & Hápticos:**
  - **Sintetizador Procedural (`AudioTrack` em modo `MODE_STATIC`)**: gera tons senoidais de colisão e arpeggios de vitória diretamente em PCM de 16 bits, sem depender de bibliotecas externas ou arquivos de áudio pesados (.mp3/.wav).
  - **Vibração Tátil (`VibrationEffect`)**: micropulsos hápticos proporcionais à intensidade de cada choque contra as paredes.

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
│       │   │   ├── ActivityPrincipal.java   # Menu Principal minimalista (Iniciar / Sair)
│       │   │   ├── ActivityJogo.java        # Ciclo de vida da partida, sensores e HUD
│       │   │   ├── view/
│       │   │   │   ├── VisaoJogo.java       # SurfaceView de alta taxa de quadros
│       │   │   │   └── ThreadJogo.java      # Loop de simulação desacoplado da UI
│       │   │   ├── engine/
│       │   │   │   ├── MotorFisico.java     # Dinâmica de movimento, colisões e repouso
│       │   │   │   ├── ControladorSensores.java # Fusão de sensores, calibração e filtro
│       │   │   │   └── Bolinha.java         # Entidade da esfera (x, y, vx, vy, raio)
│       │   │   ├── maze/
│       │   │   │   ├── Labirinto.java       # Grade de células e coordenadas
│       │   │   │   ├── CelulaLabirinto.java # Célula individual com paredes N, S, L, O
│       │   │   │   └── GeradorLabirinto.java# Gerador procedural DFS com níveis
│       │   │   └── util/
│       │   │       ├── GerenciadorVibracao.java # Feedback tátil calibrado
│       │   │       └── GerenciadorSom.java  # Sintetizador procedural PCM em tempo real
│       │   └── res/
│       │       ├── layout/                  # Layouts XML modernos (ConstraintLayout)
│       │       ├── values/                  # Strings (pt-BR), cores e temas Dark
│       │       └── drawable/                # Ícones vetoriais, gradientes e seletores
│       └── test/java/com/labirinto/tiltmaze/
│           ├── GeradorLabirintoTest.java    # Teste de solvabilidade e inundação BFS
│           └── MotorFisicoTest.java         # Teste de dinâmica, eixos e choque único
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
2. Clique em **File ➔ Open** e selecione a pasta raiz deste repositório.
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

O projeto inclui suíte completa de testes unitários para o motor físico e a geração procedural:

```powershell
.\gradlew.bat test
```

| Teste | Descrição |
| :--- | :--- |
| `testMazeDimensionsAndBounds` | Valida inicialização correta de limites e coordenadas do labirinto |
| `testAllCellsReachableFromStart` | Garante que **100% das células são alcançáveis** via busca em largura (BFS) sem áreas isoladas |
| `testLevelScaling` | Valida a progressão do tamanho da grade conforme o nível avança |
| `testTiltRollRightAcceleratesBallRight` | Valida aceleração horizontal positiva ao inclinar para a direita |
| `testTiltRollLeftAcceleratesBallLeft` | Valida aceleração horizontal negativa ao inclinar para a esquerda |
| `testPitchForwardAcceleratesBallUp` | Valida aceleração vertical para frente/topo ($-Y$) ao inclinar o celular para frente |
| `testPitchBackwardAcceleratesBallDown` | Valida aceleração vertical para trás/base ($+Y$) ao inclinar para trás |
| `testInvertAxesToggles` | Valida a inversão independente de eixos |
| `testRestingOnWallDoesNotProduceInfiniteHits` | Garante que a esfera em repouso contra a parede não produz sons contínuos |
| `testWallCollisionConstrainsPosition` | Garante que a esfera nunca penetra paredes |
| `testGoalDetection` | Valida o disparo da vitória ao atingir a célula final |

---

## 🛠️ Tecnologias Utilizadas

- **Java 17**
- **Android SDK (compileSdk 33 / minSdk 24 / targetSdk 33)**
- **AndroidX & Material Components**
- **Android Hardware Sensor API (`SensorManager`, `SensorEventListener`)**
- **Android AudioTrack API (Áudio PCM estático em tempo real)**
- **Android Vibrator / VibrationEffect**
- **JUnit 4**

---

## 📄 Licença

Este projeto é distribuído sob a licença **MIT**. Consulte o arquivo `LICENSE` para mais detalhes.
