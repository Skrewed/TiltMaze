package com.labirinto.tiltmaze.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.labirinto.tiltmaze.engine.Bolinha;
import com.labirinto.tiltmaze.engine.ControladorSensores;
import com.labirinto.tiltmaze.engine.MotorFisico;
import com.labirinto.tiltmaze.maze.CelulaLabirinto;
import com.labirinto.tiltmaze.maze.GeradorLabirinto;
import com.labirinto.tiltmaze.maze.Labirinto;
import com.labirinto.tiltmaze.util.GerenciadorSom;
import com.labirinto.tiltmaze.util.GerenciadorVibracao;

// Nossa SurfaceView customizada que desenha todo o labirinto e a bolinha.
// Dica do professor: nunca crie "new Paint()" dentro do onDraw/drawGame senão o Garbage Collector trava tudo!
public class VisaoJogo extends SurfaceView implements SurfaceHolder.Callback, MotorFisico.ListenerColisao {

    public interface ListenerEstadoJogo {
        void onLevelCompleted(int level);
    }

    private ThreadJogo threadJogo;
    private final GeradorLabirinto geradorLabirinto = new GeradorLabirinto();
    private final MotorFisico motorFisico = new MotorFisico();
    private final Bolinha bolinha = new Bolinha();

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

    // Tempo acumulado pra fazer a animação do alvo pulsando
    private float pulseTime = 0f;

    // Todas as "canetas" (Paints) pré-alocadas na memória pra não dar lag
    private final Paint paintBg = new Paint();
    private final Paint paintMazeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintMazeBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintWall = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBall = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBallGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTarget = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTargetRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStartRing = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Handler pra avisar a Activity na thread principal quando o cara ganhar a fase
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VisaoJogo(Context context) {
        super(context);
        init();
    }

    public VisaoJogo(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // Configura as cores do tema Dark Minimalist e os estilos de traço
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
        paintWall.setStrokeCap(Paint.Cap.ROUND); // Pontas redondinhas nas paredes pra ficar bonito

        paintBall.setColor(Color.parseColor("#00E5FF")); // Ciano elétrico da bolinha
        paintBall.setStyle(Paint.Style.FILL);

        paintBallGlow.setColor(Color.parseColor("#4D00E5FF")); // Brilho em volta da bolinha
        paintBallGlow.setStyle(Paint.Style.FILL);

        paintTarget.setColor(Color.parseColor("#00E676")); // Verde da saída
        paintTarget.setStyle(Paint.Style.FILL);

        paintTargetRing.setColor(Color.parseColor("#6600E676"));
        paintTargetRing.setStyle(Paint.Style.STROKE);
        paintTargetRing.setStrokeWidth(4f);

        paintStartRing.setColor(Color.parseColor("#3F4452"));
        paintStartRing.setStyle(Paint.Style.STROKE);
        paintStartRing.setStrokeWidth(3f);
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

    // Começa uma fase nova: gera outro labirinto maior e reposiciona a bolinha
    public void startLevel(int level) {
        this.faseAtual = level;
        this.faseVencida = false;
        this.labirintoAtual = geradorLabirinto.generateForLevel(level);

        recalculateLayout(getWidth(), getHeight());
        resetBallToStart();
    }

    // Reinicia a mesma fase se o jogador quiser tentar um tempo melhor
    public void restartCurrentLevel() {
        this.faseVencida = false;
        resetBallToStart();
    }

    // Coloca a bolinha no meio da célula inicial (canto superior esquerdo)
    private void resetBallToStart() {
        if (labirintoAtual == null) return;
        float startX = originX + (labirintoAtual.getStartCol() + 0.5f) * cellWidth;
        float startY = originY + (labirintoAtual.getStartRow() + 0.5f) * cellHeight;
        float radius = Math.min(cellWidth, cellHeight) * 0.28f;
        bolinha.reset(startX, startY, radius);
    }

    // Faz a conta geométrica pra centralizar o labirinto em qualquer tamanho de tela de smartphone
    private void recalculateLayout(int w, int h) {
        if (w <= 0 || h <= 0 || labirintoAtual == null) return;

        float margin = 24f;
        float availableWidth = w - (margin * 2);
        float availableHeight = h - (margin * 2);

        int cols = labirintoAtual.getCols();
        int rows = labirintoAtual.getRows();

        // Quadrados com proporção 1:1 perfeita
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
        paintMazeBorder.setStrokeWidth(wallThickness * 1.2f);
    }

    // Atualiza a física da simulação
    public void updatePhysics(float dt) {
        if (faseVencida || labirintoAtual == null) return;

        pulseTime += dt;

        float roll = 0f;
        float pitch = 0f;
        if (controladorSensores != null) {
            roll = controladorSensores.getRoll();
            pitch = controladorSensores.getPitch();
        }

        motorFisico.update(bolinha, labirintoAtual, roll, pitch, dt,
                originX, originY, cellWidth, cellHeight, wallThickness);
    }

    // Desenha tudo na tela através do Canvas
    public void drawGame(Canvas canvas) {
        // 1. Fundo geral escuro
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paintBg);

        if (labirintoAtual == null) return;

        // 2. Fundo interno do labirinto
        canvas.drawRoundRect(boundsLabirinto, 16f, 16f, paintMazeBg);

        // 3. Marcador do início (onde a bolinha nasceu)
        float startX = originX + (labirintoAtual.getStartCol() + 0.5f) * cellWidth;
        float startY = originY + (labirintoAtual.getStartRow() + 0.5f) * cellHeight;
        canvas.drawCircle(startX, startY, bolinha.radius * 1.4f, paintStartRing);

        // 4. Buraco da saída com efeito visual pulsando
        float endX = originX + (labirintoAtual.getEndCol() + 0.5f) * cellWidth;
        float endY = originY + (labirintoAtual.getEndRow() + 0.5f) * cellHeight;
        float targetRadius = Math.min(cellWidth, cellHeight) * 0.32f;
        float pulseScale = 1.0f + 0.22f * (float) Math.sin(pulseTime * 5.0);
        canvas.drawCircle(endX, endY, targetRadius * pulseScale, paintTargetRing);
        canvas.drawCircle(endX, endY, targetRadius * 0.65f, paintTarget);

        // 5. Paredes internas
        int cols = labirintoAtual.getCols();
        int rows = labirintoAtual.getRows();

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                CelulaLabirinto cell = labirintoAtual.getCell(c, r);
                if (cell == null) continue;

                float x0 = originX + c * cellWidth;
                float x1 = x0 + cellWidth;
                float y0 = originY + r * cellHeight;
                float y1 = y0 + cellHeight;

                if (cell.topWall) {
                    canvas.drawLine(x0, y0, x1, y0, paintWall);
                }
                if (cell.rightWall) {
                    canvas.drawLine(x1, y0, x1, y1, paintWall);
                }
                if (cell.bottomWall) {
                    canvas.drawLine(x0, y1, x1, y1, paintWall);
                }
                if (cell.leftWall) {
                    canvas.drawLine(x0, y0, x0, y1, paintWall);
                }
            }
        }

        // 6. Moldura ao redor do labirinto
        canvas.drawRoundRect(boundsLabirinto, 16f, 16f, paintMazeBorder);

        // 7. A bolinha azul estilosa com aura de luz
        canvas.drawCircle(bolinha.x, bolinha.y, bolinha.radius * 1.5f, paintBallGlow);
        canvas.drawCircle(bolinha.x, bolinha.y, bolinha.radius, paintBall);
    }

    // Quando o motor físico avisa que deu batida forte
    @Override
    public void onWallHit(float intensity) {
        gerenciadorVibracao.vibrateWallHit(intensity);
        gerenciadorSom.playWallHitSound(intensity);
    }

    // Quando a bolinha cai no buraco da meta
    @Override
    public void onGoalReached() {
        if (faseVencida) return;
        faseVencida = true;

        bolinha.stop();
        gerenciadorVibracao.vibrateWin();
        gerenciadorSom.playWinSound();

        // Avisa a Activity pra mostrar o painel de vitória
        mainHandler.post(() -> {
            if (listenerEstadoJogo != null) {
                listenerEstadoJogo.onLevelCompleted(faseAtual);
            }
        });
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        recalculateLayout(getWidth(), getHeight());
        if (labirintoAtual == null) {
            startLevel(faseAtual);
        } else {
            resetBallToStart();
        }

        // Dá start na thread de renderização
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
