package com.labirinto.tiltmaze.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.labirinto.tiltmaze.elementos.BuracoNegro;
import com.labirinto.tiltmaze.elementos.ParticulaChao;
import com.labirinto.tiltmaze.elementos.Vagalume;
import com.labirinto.tiltmaze.engine.Bolinha;
import com.labirinto.tiltmaze.engine.ControladorSensores;
import com.labirinto.tiltmaze.engine.MotorFisico;
import com.labirinto.tiltmaze.maze.CelulaLabirinto;
import com.labirinto.tiltmaze.maze.GeradorLabirinto;
import com.labirinto.tiltmaze.maze.Labirinto;
import com.labirinto.tiltmaze.util.GerenciadorSom;
import com.labirinto.tiltmaze.util.GerenciadorVibracao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

// SurfaceView principal do jogo com todas as mecânicas das 15 fases:
// - Fases 1 a 5: Mapas crescendo progressivamente
// - Fases 5 a 15: Fog of War com raio de luz diminuindo a cada fase
// - Fases 5 a 15: Vagalumes que voam suavemente e iluminam os caminhos
// - Fases 8 a 15: Paredes móveis com aviso âmbar que reconfiguram o labirinto sem trancar
// - Fases 10 a 15: Buracos negros temporários que teleportam a bolinha pro spawn se cair neles
public class VisaoJogo extends SurfaceView implements SurfaceHolder.Callback, MotorFisico.ListenerColisao {

    public interface ListenerEstadoJogo {
        void onLevelCompleted(int level);
    }

    private ThreadJogo threadJogo;
    private final GeradorLabirinto geradorLabirinto = new GeradorLabirinto();
    private final MotorFisico motorFisico = new MotorFisico();
    private final Bolinha bolinha = new Bolinha();
    private final Random random = new Random();

    private ControladorSensores controladorSensores;
    private GerenciadorVibracao gerenciadorVibracao;
    private GerenciadorSom gerenciadorSom;
    private ListenerEstadoJogo listenerEstadoJogo;

    private Labirinto labirintoAtual;
    private int faseAtual = 1;
    private boolean faseVencida = false;

    // Coordenadas calculadas pra centralizar o labirinto na tela do smartphone
    private float originX, originY;
    private float cellWidth, cellHeight;
    private float wallThickness;
    private final RectF boundsLabirinto = new RectF();

    // Tempo acumulado pra animações visuais
    private float pulseTime = 0f;

    // --- Mecânicas dinâmicas ---
    // Vagalumes (Fases 5+)
    private final List<Vagalume> vagalumes = new ArrayList<>();
    private float timerSpawnVagalume = 0f;

    // Paredes Móveis (Fases 8+)
    private float timerReconfiguracaoParede = 0f;
    private float timerAlertaParede = 0f;
    private boolean aguardandoReconfiguracao = false;

    // Buracos Negros (Fases 10+)
    private final List<BuracoNegro> buracosNegros = new ArrayList<>();
    private float timerSpawnBuraco = 0f;

    // Tintas pré-alocadas (Zero GC no loop de desenho!)
    private final Paint paintBg = new Paint();
    private final Paint paintMazeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintMazeBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintWall = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintWallAlert = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBall = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBallGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTarget = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTargetRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStartRing = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Tintas para Fog of War, Vagalumes e Buracos
    private final Paint paintFogDarkness = new Paint();
    private final Paint paintLightCutout = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintFireflyDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintFireflyCore = new Paint(Paint.ANTI_ALIAS_FLAG);    // Núcleo branco quente
    private final Paint paintFireflyGlow = new Paint(Paint.ANTI_ALIAS_FLAG);    // Halo bioluminescente pulsante
    private final Paint paintFireflyWings = new Paint(Paint.ANTI_ALIAS_FLAG);   // Asinhas translúcidas
    private final Paint paintFireflyTrail = new Paint(Paint.ANTI_ALIAS_FLAG);   // Partículas douradas do rastro

    // Vórtice 100% cinza monocromático (Fases 10+)
    private final Paint paintBlackHoleCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintVortexCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintVortexSingularity = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintVortexSpiral1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintVortexSpiral2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintVortexSpiral3 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectVortex = new RectF();

    // Raio de energia da bolinha azul até o vagalume ao clicar
    private boolean raioAtivo = false;
    private float raioTimer = 0f;
    private float raioStartX, raioStartY;
    private float raioEndX, raioEndY;
    private final Path pathRaio = new Path();
    private final Paint paintRaioGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintRaioCore = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Partículas de poeira luminosa caídas no chão (Zero GC via pool!)
    private static final int MAX_PARTICULAS_CHAO = 64;
    private final ParticulaChao[] particulasChao = new ParticulaChao[MAX_PARTICULAS_CHAO];
    private final Paint paintParticulaChao = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintParticulaChaoGlow = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Lock de sincronização pra física não colidir com troca de fase na UI thread
    private final Object lockFisica = new Object();

    // Matrizes e shaders pro Fog of War com bordas esfumaçadas / blur (Zero GC no loop!)
    private final Matrix matrixLuzBolinha = new Matrix();
    private final Matrix matrixLuzElemento = new Matrix();
    private final Paint paintLuzSuaveBolinha = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLuzSuaveElemento = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RadialGradient shaderLuzBolinha;
    private RadialGradient shaderLuzElemento;

    // Handler pra avisar a Activity na thread principal quando ganhar a fase
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VisaoJogo(Context context) {
        super(context);
        init();
    }

    public VisaoJogo(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        motorFisico.setCollisionListener(this);
        gerenciadorVibracao = new GerenciadorVibracao(getContext());
        gerenciadorSom = new GerenciadorSom();

        paintBg.setColor(Color.parseColor("#121316"));

        paintMazeBg.setColor(Color.parseColor("#16181D"));
        paintMazeBg.setStyle(Paint.Style.FILL);

        paintMazeBorder.setColor(Color.parseColor("#2C313C"));
        paintMazeBorder.setStyle(Paint.Style.STROKE);
        paintMazeBorder.setStrokeWidth(6f);

        paintWall.setColor(Color.parseColor("#313642"));
        paintWall.setStyle(Paint.Style.STROKE);
        paintWall.setStrokeCap(Paint.Cap.ROUND);

        // Parede que tá prestes a se mover (pisca com cor neutra elegante: platina/slate)
        paintWallAlert.setColor(Color.parseColor("#CFD8DC"));
        paintWallAlert.setStyle(Paint.Style.STROKE);
        paintWallAlert.setStrokeCap(Paint.Cap.ROUND);

        paintBall.setColor(Color.parseColor("#00E5FF"));
        paintBall.setStyle(Paint.Style.FILL);

        paintBallGlow.setColor(Color.parseColor("#4D00E5FF"));
        paintBallGlow.setStyle(Paint.Style.FILL);

        paintTarget.setColor(Color.parseColor("#00E676"));
        paintTarget.setStyle(Paint.Style.FILL);

        paintTargetRing.setColor(Color.parseColor("#6600E676"));
        paintTargetRing.setStyle(Paint.Style.STROKE);
        paintTargetRing.setStrokeWidth(4f);

        paintStartRing.setColor(Color.parseColor("#3F4452"));
        paintStartRing.setStyle(Paint.Style.STROKE);
        paintStartRing.setStrokeWidth(3f);

        // Fog of War: camada escura cobrindo o labirinto
        paintFogDarkness.setColor(Color.parseColor("#F5121316"));
        paintLightCutout.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Shaders radiais unitários (raio 1.0f) com queda gradual de transparência:
        // No modo DST_OUT, a opacidade do pincel apaga a escuridão do fog.
        // Centro (alpha 255): 100% transparente (luz máxima).
        // Borda (45% a 100% do raio): transição gradual até alpha 0 (efeito esfumaçado / blur suave).
        shaderLuzBolinha = new RadialGradient(
                0f, 0f, 1.0f,
                new int[]{
                        Color.argb(255, 0, 0, 0),
                        Color.argb(255, 0, 0, 0),
                        Color.argb(160, 0, 0, 0),
                        Color.argb(0, 0, 0, 0)
                },
                new float[]{0f, 0.45f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP
        );
        paintLuzSuaveBolinha.setShader(shaderLuzBolinha);
        paintLuzSuaveBolinha.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        shaderLuzElemento = new RadialGradient(
                0f, 0f, 1.0f,
                new int[]{
                        Color.argb(230, 0, 0, 0),
                        Color.argb(120, 0, 0, 0),
                        Color.argb(0, 0, 0, 0)
                },
                new float[]{0f, 0.40f, 1.0f},
                Shader.TileMode.CLAMP
        );
        paintLuzSuaveElemento.setShader(shaderLuzElemento);
        paintLuzSuaveElemento.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        // Vagalumes mais bonitinhos com brilho bioluminescente, asas e rastro de fada
        paintFireflyDot.setColor(Color.parseColor("#EEFF41"));
        paintFireflyDot.setStyle(Paint.Style.FILL);

        paintFireflyCore.setColor(Color.parseColor("#FFFFF0"));
        paintFireflyCore.setStyle(Paint.Style.FILL);

        paintFireflyGlow.setColor(Color.parseColor("#EEFF41"));
        paintFireflyGlow.setStyle(Paint.Style.FILL);

        paintFireflyWings.setColor(Color.parseColor("#B3FFFFFF"));
        paintFireflyWings.setStyle(Paint.Style.FILL);

        paintFireflyTrail.setColor(Color.parseColor("#FFEE55"));
        paintFireflyTrail.setStyle(Paint.Style.FILL);

        // Vórtice 100% cinza monocromático (Fases 10+)
        paintBlackHoleCore.setColor(Color.parseColor("#121316"));
        paintBlackHoleCore.setStyle(Paint.Style.FILL);

        paintVortexCenter.setColor(Color.parseColor("#1F2228"));
        paintVortexCenter.setStyle(Paint.Style.FILL);

        paintVortexSingularity.setColor(Color.parseColor("#08090A"));
        paintVortexSingularity.setStyle(Paint.Style.FILL);

        paintVortexSpiral1.setColor(Color.parseColor("#788290"));
        paintVortexSpiral1.setStyle(Paint.Style.STROKE);
        paintVortexSpiral1.setStrokeWidth(3.5f);
        paintVortexSpiral1.setStrokeCap(Paint.Cap.ROUND);

        paintVortexSpiral2.setColor(Color.parseColor("#A4B0BE"));
        paintVortexSpiral2.setStyle(Paint.Style.STROKE);
        paintVortexSpiral2.setStrokeWidth(2.8f);
        paintVortexSpiral2.setStrokeCap(Paint.Cap.ROUND);

        paintVortexSpiral3.setColor(Color.parseColor("#D1D8E0"));
        paintVortexSpiral3.setStyle(Paint.Style.STROKE);
        paintVortexSpiral3.setStrokeWidth(2.0f);
        paintVortexSpiral3.setStrokeCap(Paint.Cap.ROUND);

        // Raio de choque elétrico
        paintRaioGlow.setColor(Color.parseColor("#9900E5FF"));
        paintRaioGlow.setStyle(Paint.Style.STROKE);
        paintRaioGlow.setStrokeWidth(9f);
        paintRaioGlow.setStrokeCap(Paint.Cap.ROUND);
        paintRaioGlow.setStrokeJoin(Paint.Join.ROUND);

        paintRaioCore.setColor(Color.parseColor("#FFFFFF"));
        paintRaioCore.setStyle(Paint.Style.STROKE);
        paintRaioCore.setStrokeWidth(3f);
        paintRaioCore.setStrokeCap(Paint.Cap.ROUND);
        paintRaioCore.setStrokeJoin(Paint.Join.ROUND);

        // Pool de partículas no chão
        for (int i = 0; i < MAX_PARTICULAS_CHAO; i++) {
            particulasChao[i] = new ParticulaChao();
        }

        paintParticulaChao.setColor(Color.parseColor("#FFEE55"));
        paintParticulaChao.setStyle(Paint.Style.FILL);

        paintParticulaChaoGlow.setColor(Color.parseColor("#55EEFF41"));
        paintParticulaChaoGlow.setStyle(Paint.Style.FILL);
    }

    public void setSensorHandler(ControladorSensores controladorSensores) {
        this.controladorSensores = controladorSensores;
    }

    public void setGameStateListener(ListenerEstadoJogo listener) {
        this.listenerEstadoJogo = listener;
    }

    public void setInvertX(boolean invertX) {
        motorFisico.setInvertX(invertX);
    }

    public void setInvertY(boolean invertY) {
        motorFisico.setInvertY(invertY);
    }

    public boolean isInvertX() {
        return motorFisico.isInvertX();
    }

    public boolean isInvertY() {
        return motorFisico.isInvertY();
    }

    // Começa uma nova fase com sincronização pra thread de física não pegar dados pela metade
    public void startLevel(int level) {
        synchronized (lockFisica) {
            this.faseVencida = true; // Trava a física enquanto constrói o novo mapa
            this.faseAtual = level;
            this.labirintoAtual = geradorLabirinto.generateForLevel(level);

            // Reseta listas das mecânicas especiais
            vagalumes.clear();
            buracosNegros.clear();
            timerSpawnVagalume = 0f;
            timerSpawnBuraco = 0f;
            timerReconfiguracaoParede = 0f;
            timerAlertaParede = 0f;
            aguardandoReconfiguracao = false;
            raioAtivo = false;
            raioTimer = 0f;
            for (int i = 0; i < MAX_PARTICULAS_CHAO; i++) {
                if (particulasChao[i] != null) particulasChao[i].ativa = false;
            }

            recalculateLayout(getWidth(), getHeight());
            resetBallToStart();

            // Só libera a física DEPOIS que a bolinha já foi colocada no spawn
            this.faseVencida = false;
        }
    }

    // Reinicia a mesma fase
    public void restartCurrentLevel() {
        synchronized (lockFisica) {
            this.faseVencida = true;
            vagalumes.clear();
            buracosNegros.clear();
            timerSpawnVagalume = 0f;
            timerSpawnBuraco = 0f;
            timerReconfiguracaoParede = 0f;
            aguardandoReconfiguracao = false;
            raioAtivo = false;
            raioTimer = 0f;
            for (int i = 0; i < MAX_PARTICULAS_CHAO; i++) {
                if (particulasChao[i] != null) particulasChao[i].ativa = false;
            }
            if (labirintoAtual != null) {
                labirintoAtual.limparAlertas();
            }
            resetBallToStart();
            this.faseVencida = false;
        }
    }

    // Coloca a bolinha no spawn do labirinto atual
    private void resetBallToStart() {
        if (labirintoAtual == null) return;
        float startX = originX + (labirintoAtual.getStartCol() + 0.5f) * cellWidth;
        float startY = originY + (labirintoAtual.getStartRow() + 0.5f) * cellHeight;
        float radius = Math.min(cellWidth, cellHeight) * 0.28f;
        bolinha.reset(startX, startY, radius);
    }

    private void recalculateLayout(int w, int h) {
        if (w <= 0 || h <= 0 || labirintoAtual == null) return;

        float margin = 24f;
        float availableWidth = w - (margin * 2);
        float availableHeight = h - (margin * 2);

        int cols = labirintoAtual.getCols();
        int rows = labirintoAtual.getRows();

        float cellSize = Math.min(availableWidth / cols, availableHeight / rows);
        cellWidth = cellSize;
        cellHeight = cellSize;

        float mazePixelWidth = cols * cellSize;
        float mazePixelHeight = rows * cellSize;

        originX = (w - mazePixelWidth) / 2f;
        originY = (h - mazePixelHeight) / 2f;

        boundsLabirinto.set(originX, originY, originX + mazePixelWidth, originY + mazePixelHeight);
        wallThickness = Math.max(5f, cellSize * 0.12f);
        paintWall.setStrokeWidth(wallThickness);
        paintWallAlert.setStrokeWidth(wallThickness * 1.3f);
        paintMazeBorder.setStrokeWidth(wallThickness * 1.2f);
    }

    // Calcula o raio da luz da bolinha de acordo com a fase atual
    // Diminui progressivamente da fase 5 até a 15
    public float calcularRaioLuzBolinha() {
        if (faseAtual < 5) return 9999f; // Fases 1 a 4 têm visão total
        // Fase 5 começa com raio grande (~3.4 células) e cai até ~1.3 célula na fase 15
        float multiplicador = Math.max(1.3f, 3.4f - (faseAtual - 5) * 0.21f);
        return cellWidth * multiplicador;
    }

    // Loop de atualização da física e elementos vivos
    public void updatePhysics(float dt) {
        synchronized (lockFisica) {
            if (faseVencida || labirintoAtual == null) return;

            pulseTime += dt;

            float roll = 0f;
            float pitch = 0f;
            if (controladorSensores != null) {
                roll = controladorSensores.getRoll();
                pitch = controladorSensores.getPitch();
            }

            // 1. Atualiza a física da bolinha (e colisões com paredes e buracos negros)
            motorFisico.update(bolinha, labirintoAtual, buracosNegros, roll, pitch, dt,
                    originX, originY, cellWidth, cellHeight, wallThickness);

            // Timer do raio elétrico disparado pela bolinha
            if (raioAtivo) {
                raioTimer -= dt;
                if (raioTimer <= 0f) {
                    raioAtivo = false;
                }
            }

            // Atualiza partículas que caíram no chão iluminando o mapa com contenção suave
            for (int i = 0; i < MAX_PARTICULAS_CHAO; i++) {
                if (particulasChao[i].ativa) {
                    particulasChao[i].update(dt, boundsLabirinto.left + 8f, boundsLabirinto.top + 8f,
                            boundsLabirinto.right - 8f, boundsLabirinto.bottom - 8f);
                }
            }

            // 2. Vagalumes (Fases 5 a 15)
            if (faseAtual >= 5) {
                timerSpawnVagalume += dt;
                // Mantém de 2 a 4 vagalumes ativos voando pela tela
                if (timerSpawnVagalume > 1.8f && vagalumes.size() < 4) {
                    timerSpawnVagalume = 0f;
                    float spawnX = boundsLabirinto.left + random.nextFloat() * boundsLabirinto.width();
                    float spawnY = boundsLabirinto.top + random.nextFloat() * boundsLabirinto.height();
                    float raioLuzVagalume = cellWidth * (1.3f + random.nextFloat() * 0.4f);
                    vagalumes.add(new Vagalume(spawnX, spawnY, raioLuzVagalume));
                }

                Iterator<Vagalume> itV = vagalumes.iterator();
                while (itV.hasNext()) {
                    Vagalume v = itV.next();
                    v.update(dt, boundsLabirinto.left + 10f, boundsLabirinto.top + 10f,
                            boundsLabirinto.right - 10f, boundsLabirinto.bottom - 10f);
                    if (v.isMorto()) {
                        itV.remove();
                    }
                }
            }

            // 3. Paredes Dinâmicas com Auto-reconfiguração (Fases 8 a 15)
            if (faseAtual >= 8) {
                // Frequência e velocidade de alternância aumentam conforme os níveis avançam:
                // Intervalo entre trocas cai de 6.0s (fase 8) para até 2.4s (fase 15)
                float intervaloReconfig = Math.max(2.4f, 6.0f - (faseAtual - 8) * 0.50f);
                // Duração do aviso piscando fica mais rápida e desafiadora (1.4s até 0.65s)
                float duracaoAlerta = Math.max(0.65f, 1.4f - (faseAtual - 8) * 0.11f);
                // Muda mais paredes por vez conforme o nível avança (de 3 até 6 paredes)
                int qtdParedes = Math.min(6, 3 + (faseAtual - 8) / 2);

                if (!aguardandoReconfiguracao) {
                    timerReconfiguracaoParede += dt;
                    if (timerReconfiguracaoParede >= intervaloReconfig) {
                        timerReconfiguracaoParede = 0f;
                        aguardandoReconfiguracao = true;
                        timerAlertaParede = duracaoAlerta;
                        labirintoAtual.avisarProximaReconfiguracao(qtdParedes);
                    }
                } else {
                    timerAlertaParede -= dt;
                    if (timerAlertaParede <= 0f) {
                        aguardandoReconfiguracao = false;
                        // Descobre a célula atual da bolinha pra garantir que não vai trancá-la
                        int ballCol = Math.max(0, Math.min(labirintoAtual.getCols() - 1, (int) ((bolinha.x - originX) / cellWidth)));
                        int ballRow = Math.max(0, Math.min(labirintoAtual.getRows() - 1, (int) ((bolinha.y - originY) / cellHeight)));
                        labirintoAtual.executarReconfiguracao(ballCol, ballRow);
                    }
                }
            }

            // 4. Buracos Negros Móveis (Fases 10 a 15)
            if (faseAtual >= 10) {
                timerSpawnBuraco += dt;
                // Spawna um buraco a cada 3.5 segundos até o limite de 3 buracos simultâneos
                if (timerSpawnBuraco >= 3.5f && buracosNegros.size() < 3) {
                    timerSpawnBuraco = 0f;
                    spawnaBuracoNegroAleatorio();
                }

                Iterator<BuracoNegro> itB = buracosNegros.iterator();
                while (itB.hasNext()) {
                    BuracoNegro b = itB.next();
                    b.update(dt);
                    if (b.isMorto()) {
                        itB.remove();
                    }
                }
            }
        }
    }

    // Sorteia uma célula livre (longe da bolinha atual e longe do goal) pra nascer o buraco
    private void spawnaBuracoNegroAleatorio() {
        if (labirintoAtual == null) return;
        int cols = labirintoAtual.getCols();
        int rows = labirintoAtual.getRows();

        int ballCol = Math.max(0, Math.min(cols - 1, (int) ((bolinha.x - originX) / cellWidth)));
        int ballRow = Math.max(0, Math.min(rows - 1, (int) ((bolinha.y - originY) / cellHeight)));

        for (int tentativa = 0; tentativa < 25; tentativa++) {
            int c = random.nextInt(cols);
            int r = random.nextInt(rows);

            // Nunca nasce na célula onde a bolinha está nem na saída verde
            if ((c == ballCol && r == ballRow) ||
                (c == labirintoAtual.getEndCol() && r == labirintoAtual.getEndRow()) ||
                (c == labirintoAtual.getStartCol() && r == labirintoAtual.getStartRow())) {
                continue;
            }

            // Não nasce colado em outro buraco já existente
            boolean muitoPerto = false;
            for (BuracoNegro existente : buracosNegros) {
                if (Math.abs(existente.col - c) <= 1 && Math.abs(existente.row - r) <= 1) {
                    muitoPerto = true;
                    break;
                }
            }
            if (muitoPerto) continue;

            float cx = originX + (c + 0.5f) * cellWidth;
            float cy = originY + (r + 0.5f) * cellHeight;
            float raioMax = Math.min(cellWidth, cellHeight) * 0.38f;
            float duracaoAtivo = 4.0f + random.nextFloat() * 2.0f; // Fica ativo entre 4 e 6 segundos

            buracosNegros.add(new BuracoNegro(c, r, cx, cy, raioMax, duracaoAtivo));
            break;
        }
    }

    // Desenha tudo na tela protegida por lock pra não ler labirinto durante geração/troca de fase
    public void drawGame(Canvas canvas) {
        synchronized (lockFisica) {
            // 1. Fundo geral escuro
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paintBg);

            if (labirintoAtual == null) return;

        // 2. Fundo interno do labirinto
        canvas.drawRoundRect(boundsLabirinto, 16f, 16f, paintMazeBg);

        // 3. Ponto de partida
        float startX = originX + (labirintoAtual.getStartCol() + 0.5f) * cellWidth;
        float startY = originY + (labirintoAtual.getStartRow() + 0.5f) * cellHeight;
        canvas.drawCircle(startX, startY, bolinha.radius * 1.4f, paintStartRing);

        // 4. Buracos Negros / Vórtices Cinzas (Fases 10+) - 100% monocromático cinza
        for (BuracoNegro b : buracosNegros) {
            float r = b.getRaioAtual();
            if (r > 0f) {
                float rot = (b.getTempoAtual() * 180f) % 360f;

                // Fundo do vórtice (cinza escuro profundo)
                canvas.drawCircle(b.centerX, b.centerY, r * 1.18f, paintVortexCenter);
                canvas.drawCircle(b.centerX, b.centerY, r * 0.95f, paintBlackHoleCore);

                // Espirais e anéis concêntricos girando (100% tons neutros de cinza)
                // Braço externo (cinza médio)
                rectVortex.set(b.centerX - r * 1.05f, b.centerY - r * 1.05f,
                        b.centerX + r * 1.05f, b.centerY + r * 1.05f);
                canvas.drawArc(rectVortex, rot, 80f, false, paintVortexSpiral1);
                canvas.drawArc(rectVortex, rot + 180f, 80f, false, paintVortexSpiral1);

                // Braço intermediário (cinza claro prateado)
                rectVortex.set(b.centerX - r * 0.72f, b.centerY - r * 0.72f,
                        b.centerX + r * 0.72f, b.centerY + r * 0.72f);
                canvas.drawArc(rectVortex, -rot * 1.3f, 100f, false, paintVortexSpiral2);
                canvas.drawArc(rectVortex, -rot * 1.3f + 180f, 100f, false, paintVortexSpiral2);

                // Braço interno veloz (cinza platina brilhante)
                rectVortex.set(b.centerX - r * 0.42f, b.centerY - r * 0.42f,
                        b.centerX + r * 0.42f, b.centerY + r * 0.42f);
                canvas.drawArc(rectVortex, rot * 2.0f + 90f, 120f, false, paintVortexSpiral3);
                canvas.drawArc(rectVortex, rot * 2.0f + 270f, 120f, false, paintVortexSpiral3);

                // Singularidade central
                canvas.drawCircle(b.centerX, b.centerY, r * 0.22f, paintVortexSingularity);
            }
        }

        // Partículas luminosas caídas no chão deixadas por vagalumes abatidos
        for (int i = 0; i < MAX_PARTICULAS_CHAO; i++) {
            ParticulaChao p = particulasChao[i];
            if (p.ativa) {
                float intens = p.getIntensidade();
                if (intens > 0f) {
                    paintParticulaChaoGlow.setAlpha((int) (90 * intens));
                    canvas.drawCircle(p.x, p.y, p.raio * 2.2f, paintParticulaChaoGlow);

                    paintParticulaChao.setAlpha((int) (255 * intens));
                    canvas.drawCircle(p.x, p.y, p.raio, paintParticulaChao);
                }
            }
        }

        // 5. Saída verde (Goal)
        float endX = originX + (labirintoAtual.getEndCol() + 0.5f) * cellWidth;
        float endY = originY + (labirintoAtual.getEndRow() + 0.5f) * cellHeight;
        float targetRadius = Math.min(cellWidth, cellHeight) * 0.32f;
        float pulseScale = 1.0f + 0.22f * (float) Math.sin(pulseTime * 5.0);
        canvas.drawCircle(endX, endY, targetRadius * pulseScale, paintTargetRing);
        canvas.drawCircle(endX, endY, targetRadius * 0.65f, paintTarget);

        // 6. Paredes internas do labirinto
        int cols = labirintoAtual.getCols();
        int rows = labirintoAtual.getRows();

        // Piscar neutro das paredes em alerta (Fases 8+) - mais rápido nos níveis mais altos
        float freqPiscar = 10f + (faseAtual - 8) * 2.5f;
        boolean alertaPiscar = ((int) (pulseTime * freqPiscar) % 2 == 0);

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                CelulaLabirinto cell = labirintoAtual.getCell(c, r);
                if (cell == null) continue;

                float x0 = originX + c * cellWidth;
                float x1 = x0 + cellWidth;
                float y0 = originY + r * cellHeight;
                float y1 = y0 + cellHeight;

                // Parede Superior
                if (cell.topWall) {
                    canvas.drawLine(x0, y0, x1, y0, (cell.topWallAlert && alertaPiscar) ? paintWallAlert : paintWall);
                } else if (cell.topWallAlert && alertaPiscar) {
                    // Parede aberta que tá prestes a fechar: avisa piscando
                    canvas.drawLine(x0, y0, x1, y0, paintWallAlert);
                }

                // Parede Direita
                if (cell.rightWall) {
                    canvas.drawLine(x1, y0, x1, y1, (cell.rightWallAlert && alertaPiscar) ? paintWallAlert : paintWall);
                } else if (cell.rightWallAlert && alertaPiscar) {
                    canvas.drawLine(x1, y0, x1, y1, paintWallAlert);
                }

                // Parede Inferior
                if (cell.bottomWall) {
                    canvas.drawLine(x0, y1, x1, y1, (cell.bottomWallAlert && alertaPiscar) ? paintWallAlert : paintWall);
                } else if (cell.bottomWallAlert && alertaPiscar) {
                    canvas.drawLine(x0, y1, x1, y1, paintWallAlert);
                }

                // Parede Esquerda
                if (cell.leftWall) {
                    canvas.drawLine(x0, y0, x0, y1, (cell.leftWallAlert && alertaPiscar) ? paintWallAlert : paintWall);
                } else if (cell.leftWallAlert && alertaPiscar) {
                    canvas.drawLine(x0, y0, x0, y1, paintWallAlert);
                }
            }
        }

        // 7. Borda externa do labirinto
        canvas.drawRoundRect(boundsLabirinto, 16f, 16f, paintMazeBorder);

        // 8. Bolinha do jogador
        canvas.drawCircle(bolinha.x, bolinha.y, bolinha.radius * 1.5f, paintBallGlow);
        canvas.drawCircle(bolinha.x, bolinha.y, bolinha.radius, paintBall);

        // 9. Fog of War (Fases 5 a 15) com bordas esfumaçadas / blur
        // Usa shaders com modo DST_OUT pras bordas ficarem totalmente esfumaçadas e suaves
        if (faseAtual >= 5) {
            float raioLuz = calcularRaioLuzBolinha();

            // Salva camada pro corte com gradiente suave (DST_OUT)
            int saveCount = canvas.saveLayer(boundsLabirinto.left, boundsLabirinto.top,
                    boundsLabirinto.right, boundsLabirinto.bottom, null);

            // Escuridão cobrindo o tabuleiro
            canvas.drawRect(boundsLabirinto, paintFogDarkness);

            // Furo de luz em volta da bolinha com borda esfumaçada / blur
            matrixLuzBolinha.reset();
            matrixLuzBolinha.setScale(raioLuz, raioLuz);
            matrixLuzBolinha.postTranslate(bolinha.x, bolinha.y);
            shaderLuzBolinha.setLocalMatrix(matrixLuzBolinha);
            canvas.drawCircle(bolinha.x, bolinha.y, raioLuz, paintLuzSuaveBolinha);

            // Furos de luz suaves e esfumaçados onde os vagalumes e seu rastro passam
            for (Vagalume v : vagalumes) {
                float op = v.getOpacidade();
                if (op > 0.08f) {
                    float raioV = v.raioLuz * op * v.getFatorPulso();
                    matrixLuzElemento.reset();
                    matrixLuzElemento.setScale(raioV, raioV);
                    matrixLuzElemento.postTranslate(v.x, v.y);
                    shaderLuzElemento.setLocalMatrix(matrixLuzElemento);
                    canvas.drawCircle(v.x, v.y, raioV, paintLuzSuaveElemento);

                    // Cada partícula no rastro também abre um furinho de luz esfumaçado por onde passou!
                    for (Vagalume.Particula p : v.getParticulas()) {
                        if (p.ativa) {
                            float pOp = (1f - p.vida / p.duracao) * op;
                            if (pOp > 0.15f) {
                                float raioP = p.raio * 4.0f * pOp;
                                matrixLuzElemento.reset();
                                matrixLuzElemento.setScale(raioP, raioP);
                                matrixLuzElemento.postTranslate(p.x, p.y);
                                shaderLuzElemento.setLocalMatrix(matrixLuzElemento);
                                canvas.drawCircle(p.x, p.y, raioP, paintLuzSuaveElemento);
                            }
                        }
                    }
                }
            }

            // Luz suave permanente da saída verde (serve como bússola no escuro)
            float raioSaida = targetRadius * 2.2f;
            matrixLuzElemento.reset();
            matrixLuzElemento.setScale(raioSaida, raioSaida);
            matrixLuzElemento.postTranslate(endX, endY);
            shaderLuzElemento.setLocalMatrix(matrixLuzElemento);
            canvas.drawCircle(endX, endY, raioSaida, paintLuzSuaveElemento);

            // Furos de luz suaves das partículas no chão (vão enfraquecendo aos poucos até voltar ao escuro)
            for (int i = 0; i < MAX_PARTICULAS_CHAO; i++) {
                ParticulaChao p = particulasChao[i];
                if (p.ativa) {
                    float intens = p.getIntensidade();
                    if (intens > 0.04f) {
                        float rLuzP = p.raioLuz * (0.35f + 0.65f * intens);
                        matrixLuzElemento.reset();
                        matrixLuzElemento.setScale(rLuzP, rLuzP);
                        matrixLuzElemento.postTranslate(p.x, p.y);
                        shaderLuzElemento.setLocalMatrix(matrixLuzElemento);
                        canvas.drawCircle(p.x, p.y, rLuzP, paintLuzSuaveElemento);
                    }
                }
            }

            // Flash de luz do raio elétrico rasgando o nevoeiro
            if (raioAtivo) {
                float midX = (raioStartX + raioEndX) * 0.5f;
                float midY = (raioStartY + raioEndY) * 0.5f;
                float rRaio = cellWidth * 1.8f;
                matrixLuzElemento.reset();
                matrixLuzElemento.setScale(rRaio, rRaio);
                matrixLuzElemento.postTranslate(midX, midY);
                shaderLuzElemento.setLocalMatrix(matrixLuzElemento);
                canvas.drawCircle(midX, midY, rRaio, paintLuzSuaveElemento);
            }

            canvas.restoreToCount(saveCount);
        }

        // 10. Desenho dos Vagalumes e seu rastro de partículas cintilantes (Zero GC!)
        for (Vagalume v : vagalumes) {
            float op = v.getOpacidade();
            if (op <= 0f) continue;

            int alphaGeral = (int) (255 * op);

            // a) Rastro de partículas (fagulhas cintilantes de poeira mágica)
            for (Vagalume.Particula p : v.getParticulas()) {
                if (p.ativa) {
                    float percVida = p.vida / p.duracao;
                    float alphaP = (1f - percVida) * op;
                    paintFireflyTrail.setAlpha((int) (240 * alphaP));

                    // Partícula vai encolhendo suavemente até sumir
                    float rParticula = p.raio * (1f - 0.45f * percVida);
                    canvas.drawCircle(p.x, p.y, rParticula, paintFireflyTrail);
                }
            }

            // b) Halo pulsante bioluminescente em volta do abdômen
            float pulso = v.getFatorPulso();
            paintFireflyGlow.setAlpha((int) (85 * op * (pulso * 0.9f)));
            canvas.drawCircle(v.x, v.y, 11f * pulso, paintFireflyGlow);

            paintFireflyGlow.setAlpha((int) (210 * op));
            canvas.drawCircle(v.x, v.y, 4.8f * pulso, paintFireflyGlow);

            // c) Núcleo quente branco incandescente (lâmpada natural)
            paintFireflyCore.setAlpha(alphaGeral);
            canvas.drawCircle(v.x, v.y, 2.2f, paintFireflyCore);

            // d) Asinhas batendo em alta frequência
            float ang = v.getAngulo();
            float batimento = v.getBatimentoAsa(); // -1 a 1
            float asaDist = 3.8f;
            float asaOffset = batimento * 2.6f;

            // Vetores ortogonais à direção do vôo pras duas asinhas
            float perpX = -(float) Math.sin(ang);
            float perpY = (float) Math.cos(ang);

            paintFireflyWings.setAlpha((int) (160 * op));
            // Asa esquerda
            canvas.drawCircle(v.x + perpX * asaDist, v.y + perpY * asaDist + asaOffset, 2.3f, paintFireflyWings);
            // Asa direita
            canvas.drawCircle(v.x - perpX * asaDist, v.y - perpY * asaDist - asaOffset, 2.3f, paintFireflyWings);
        }

        // 11. Raio de energia da bolinha azul até o vagalume
        if (raioAtivo) {
            canvas.drawPath(pathRaio, paintRaioGlow);
            canvas.drawPath(pathRaio, paintRaioCore);
        }
    }
}

    // Dispara o raio elétrico da bolinha azul até a posição do vagalume clicado
    private void dispararRaioPara(float targetX, float targetY) {
        raioStartX = bolinha.x;
        raioStartY = bolinha.y;
        raioEndX = targetX;
        raioEndY = targetY;
        raioAtivo = true;
        raioTimer = 0.18f;

        pathRaio.reset();
        pathRaio.moveTo(raioStartX, raioStartY);

        float dx = raioEndX - raioStartX;
        float dy = raioEndY - raioStartY;
        float dist = (float) Math.hypot(dx, dy);

        float perpX = -dy / (dist > 0.001f ? dist : 1f);
        float perpY = dx / (dist > 0.001f ? dist : 1f);

        int segmentos = Math.max(4, Math.min(8, (int) (dist / 35f)));
        for (int i = 1; i < segmentos; i++) {
            float t = (float) i / segmentos;
            float segX = raioStartX + dx * t;
            float segY = raioStartY + dy * t;
            float jitter = (random.nextFloat() - 0.5f) * 26f * (1f - Math.abs(t - 0.5f) * 0.7f);
            pathRaio.lineTo(segX + perpX * jitter, segY + perpY * jitter);
        }
        pathRaio.lineTo(raioEndX, raioEndY);
    }

    // Solta partículas que caem no chão e se espalham amplamente pelo labirinto (sem empelotar em blob)
    private void spawnParticulasChao(float x, float y) {
        int qtd = 18 + random.nextInt(6); // 18 a 23 partículas dispersas
        int adicionadas = 0;

        for (int i = 0; i < MAX_PARTICULAS_CHAO && adicionadas < qtd; i++) {
            ParticulaChao p = particulasChao[i];
            if (!p.ativa) {
                // Leve dispersão no ponto de origem pra não partirem todas do mesmo pixel exato
                float burstAng = (float) (random.nextFloat() * 2 * Math.PI);
                float burstDist = 3f + random.nextFloat() * 12f;
                p.x = x + (float) Math.cos(burstAng) * burstDist;
                p.y = y + (float) Math.sin(burstAng) * burstDist;

                // 3 faixas de velocidade pra criar uma dispersão rica: algumas ficam perto, outras voam longe
                float angulo = (float) (random.nextFloat() * 2 * Math.PI);
                float faixa = random.nextFloat();
                float vel;
                if (faixa < 0.30f) {
                    // Núcleo interno: embers suaves que assentam mais perto
                    vel = cellWidth * (0.9f + random.nextFloat() * 0.7f);
                    p.raio = 2.4f + random.nextFloat() * 1.4f;
                    p.raioLuz = cellWidth * (0.40f + random.nextFloat() * 0.25f);
                } else if (faixa < 0.75f) {
                    // Corpo médio: espalham-se pela largura do corredor
                    vel = cellWidth * (1.8f + random.nextFloat() * 1.0f);
                    p.raio = 1.8f + random.nextFloat() * 1.2f;
                    p.raioLuz = cellWidth * (0.55f + random.nextFloat() * 0.30f);
                } else {
                    // Estilhaços rápidos: disparam em direção às curvas e paredes
                    vel = cellWidth * (2.9f + random.nextFloat() * 1.3f);
                    p.raio = 1.4f + random.nextFloat() * 1.0f;
                    p.raioLuz = cellWidth * (0.75f + random.nextFloat() * 0.35f);
                }

                p.vx = (float) Math.cos(angulo) * vel;
                p.vy = (float) Math.sin(angulo) * vel;
                p.vida = 0f;
                // Duração individual variável pra não apagarem todas de uma vez de forma brusca
                p.duracao = 5.5f + random.nextFloat() * 3.5f; // Ficam no chão entre 5.5s e 9.0s
                p.ativa = true;
                adicionadas++;
            }
        }
    }

    // Detecta toque do usuário nos vagalumes pra disparar o raio da bolinha azul
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();

            synchronized (lockFisica) {
                if (faseVencida || labirintoAtual == null || faseAtual < 5) {
                    return super.onTouchEvent(event);
                }

                Vagalume vagalumeAlvo = null;
                float menorDistSq = Float.MAX_VALUE;
                float touchRadius = Math.max(55f, cellWidth * 0.85f);
                float maxDistSq = touchRadius * touchRadius;

                for (Vagalume v : vagalumes) {
                    if (!v.isMorto() && v.getOpacidade() > 0.15f) {
                        float dx = v.x - touchX;
                        float dy = v.y - touchY;
                        float distSq = dx * dx + dy * dy;
                        if (distSq <= maxDistSq && distSq < menorDistSq) {
                            menorDistSq = distSq;
                            vagalumeAlvo = v;
                        }
                    }
                }

                if (vagalumeAlvo != null) {
                    // 1. Dispara o raio da bolinha azul até o vagalume
                    dispararRaioPara(vagalumeAlvo.x, vagalumeAlvo.y);

                    // 2. Vagalume morre na hora
                    float deadX = vagalumeAlvo.x;
                    float deadY = vagalumeAlvo.y;
                    vagalumeAlvo.matar();

                    // 3. Partículas caem no chão e iluminam a área
                    spawnParticulasChao(deadX, deadY);

                    // 4. Efeitos sonoro e tátil
                    gerenciadorSom.playElectricZapSound();
                    gerenciadorVibracao.vibrateZap();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onWallHit(float intensity) {
        gerenciadorVibracao.vibrateWallHit(intensity);
        gerenciadorSom.playWallHitSound(intensity);
    }

    @Override
    public void onBlackHoleCaptured() {
        // Feedback dramático quando cai no buraco negro: vibra forte e toca o whoosh de queda
        gerenciadorVibracao.vibrateBlackHole();
        gerenciadorSom.playBlackHoleSound();
    }

    @Override
    public void onGoalReached() {
        synchronized (lockFisica) {
            if (faseVencida) return;
            faseVencida = true;

            bolinha.stop();
            gerenciadorVibracao.vibrateWin();
            gerenciadorSom.playWinSound();

            mainHandler.post(() -> {
                if (listenerEstadoJogo != null) {
                    listenerEstadoJogo.onLevelCompleted(faseAtual);
                }
            });
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        recalculateLayout(getWidth(), getHeight());
        if (labirintoAtual == null) {
            startLevel(faseAtual);
        } else {
            resetBallToStart();
        }

        threadJogo = new ThreadJogo(holder, this);
        threadJogo.setRunning(true);
        threadJogo.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        recalculateLayout(width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        boolean retry = true;
        if (threadJogo != null) {
            threadJogo.setRunning(false);
            while (retry) {
                try {
                    threadJogo.join();
                    retry = false;
                } catch (InterruptedException ignored) {}
            }
            threadJogo = null;
        }
        if (gerenciadorSom != null) {
            gerenciadorSom.release();
        }
    }

    public void pauseGame() {
        if (threadJogo != null) {
            threadJogo.setPaused(true);
        }
    }

    public void resumeGame() {
        if (threadJogo != null) {
            threadJogo.setPaused(false);
        }
    }
}
