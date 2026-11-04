package com.labirinto.tiltmaze.view;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

// Thread separada da UI pra rodar o loop do jogo a 60-90 FPS sem travar os botões da tela.
// Quem já tentou rodar animação pesada na UI thread sabe a dor de cabeça que dá (ANR).
public class ThreadJogo extends Thread {

    private final SurfaceHolder surfaceHolder;
    private final VisaoJogo visaoJogo;

    // Controlam se o loop tá rodando e se tá pausado
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;

    // Meta de tempo por frame pra tentar cravar ~90 FPS
    private static final long TARGET_FRAME_TIME_NS = 1_000_000_000L / 90;

    public ThreadJogo(SurfaceHolder surfaceHolder, VisaoJogo visaoJogo) {
        this.surfaceHolder = surfaceHolder;
        this.visaoJogo = visaoJogo;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();

        while (isRunning) {
            long now = System.nanoTime();
            // dt é o delta-time em segundos entre o frame anterior e esse
            float dt = (now - lastTime) / 1_000_000_000.0f;
            lastTime = now;

            // Se o usuário minimizou o app ou deu um lag monstro, trava dt em 0.1s pra bolinha não teletransportar
            if (dt > 0.1f) {
                dt = 0.1f;
            }

            // Atualiza a física da bolinha se não tiver pausado
            if (!isPaused) {
                visaoJogo.updatePhysics(dt);
            }

            Canvas canvas = null;
            try {
                // Trava o Canvas pra gente poder desenhar com exclusividade
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        visaoJogo.drawGame(canvas);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                // Destrava e manda o Canvas pra tela do celular
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }

            // Dorme um tiquinho de nanossegundos só pra não queimar processador além da conta
            long elapsed = System.nanoTime() - now;
            long sleepNs = TARGET_FRAME_TIME_NS - elapsed;
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
