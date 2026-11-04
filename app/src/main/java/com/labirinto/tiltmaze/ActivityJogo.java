package com.labirinto.tiltmaze;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.labirinto.tiltmaze.engine.ControladorSensores;
import com.labirinto.tiltmaze.view.VisaoJogo;

import java.util.Locale;

// Tela principal da partida (ActivityJogo).
// Gerencia a campanha completa de 15 fases, os sensores, o cronômetro, a HUD superior
// e os diálogos de vitória de fase e do jogo zerado.
public class ActivityJogo extends AppCompatActivity implements VisaoJogo.ListenerEstadoJogo {

    public static final int TOTAL_FASES = 15; // O jogo terá exatamente 15 fases

    private static final String PREFS_NAME = "tilt_maze_prefs";
    private static final String KEY_INVERT_Y = "invert_y";
    private static final String KEY_INVERT_X = "invert_x";

    private VisaoJogo visaoJogo;
    private ControladorSensores controladorSensores;

    private TextView tvHudLevel;
    private TextView tvHudTimer;
    private FrameLayout overlayVictory;
    private TextView tvWinTitle;
    private TextView tvWinTime;
    private Button btnNextLevel;

    private int faseAtual = 1;
    private long tempoInicioFase = 0;
    private boolean isTimerRunning = false;

    // Handler pra atualizar o cronômetro na UI a cada 100ms
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTimerRunning) {
                long decorrido = System.currentTimeMillis() - tempoInicioFase;
                tvHudTimer.setText(formatTime(decorrido));
                timerHandler.postDelayed(this, 100);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        visaoJogo = findViewById(R.id.gameView);
        tvHudLevel = findViewById(R.id.tvHudLevel);
        tvHudTimer = findViewById(R.id.tvHudTimer);
        overlayVictory = findViewById(R.id.overlayVictory);
        tvWinTitle = findViewById(R.id.tvWinTitle);
        tvWinTime = findViewById(R.id.tvWinTime);

        Button btnHudMenu = findViewById(R.id.btnHudMenu);
        Button btnHudRestart = findViewById(R.id.btnHudRestart);
        btnNextLevel = findViewById(R.id.btnNextLevel);
        Button btnOverlayMenu = findViewById(R.id.btnOverlayMenu);

        // Carrega as preferências salvas
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean savedInvertY = prefs.getBoolean(KEY_INVERT_Y, false);
        boolean savedInvertX = prefs.getBoolean(KEY_INVERT_X, false);
        visaoJogo.setInvertY(savedInvertY);
        visaoJogo.setInvertX(savedInvertX);

        // Inicializa giroscópio e sensores
        controladorSensores = new ControladorSensores(this);
        visaoJogo.setSensorHandler(controladorSensores);
        visaoJogo.setGameStateListener(this);

        if (!controladorSensores.isSensorAvailable()) {
            Toast.makeText(this, R.string.sensor_warning, Toast.LENGTH_LONG).show();
        }

        // Botões do HUD minimalista
        btnHudMenu.setOnClickListener(v -> finish());
        btnHudRestart.setOnClickListener(v -> restartLevel());
        btnOverlayMenu.setOnClickListener(v -> finish());

        // Inicia na fase 1
        startLevel(faseAtual);
    }

    private void startLevel(int level) {
        // Pausa o loop da física pra não dar race condition na troca de mapa
        visaoJogo.pauseGame();
        overlayVictory.setVisibility(View.GONE);
        tvHudLevel.setText(getString(R.string.hud_level, level));

        // Inicializa a fase e posiciona a bolinha no spawn
        visaoJogo.startLevel(level);

        tempoInicioFase = System.currentTimeMillis();
        isTimerRunning = true;
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);

        // Retoma a física com tudo pronto
        visaoJogo.resumeGame();
    }

    private void restartLevel() {
        visaoJogo.pauseGame();
        overlayVictory.setVisibility(View.GONE);

        visaoJogo.restartCurrentLevel();

        tempoInicioFase = System.currentTimeMillis();
        isTimerRunning = true;
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);

        visaoJogo.resumeGame();
    }

    // Chamado quando a bolinha atinge o objetivo
    @Override
    public void onLevelCompleted(int level) {
        long tempoFinal = System.currentTimeMillis() - tempoInicioFase;

        // Trava anti-bug: se completou em menos de 350ms, é resquício da fase anterior na troca de mapa. Ignora!
        if (tempoFinal < 350) {
            return;
        }

        // Pausa a física pra bolinha não ficar rodando debaixo do diálogo de vitória
        visaoJogo.pauseGame();
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        String formatado = formatTime(tempoFinal);

        // Se chegou e venceu a 15ª fase, Zerou o Jogo!
        if (level >= TOTAL_FASES) {
            tvWinTitle.setText(R.string.game_completed_title);
            tvWinTime.setText(getString(R.string.game_completed_message, formatado));
            btnNextLevel.setText(R.string.btn_play_again);
            btnNextLevel.setOnClickListener(v -> {
                faseAtual = 1;
                startLevel(faseAtual);
            });
        } else {
            tvWinTitle.setText(R.string.dialog_win_title);
            tvWinTime.setText(getString(R.string.dialog_win_message, formatado));
            btnNextLevel.setText(R.string.btn_next_level);
            btnNextLevel.setOnClickListener(v -> {
                faseAtual++;
                startLevel(faseAtual);
            });
        }

        overlayVictory.setVisibility(View.VISIBLE);
    }

    private String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long tenths = (millis % 1000) / 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controladorSensores != null) {
            controladorSensores.start();
        }
        if (visaoJogo != null && overlayVictory.getVisibility() != View.VISIBLE) {
            visaoJogo.resumeGame();
            if (!isTimerRunning) {
                isTimerRunning = true;
                timerHandler.post(timerRunnable);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (controladorSensores != null) {
            controladorSensores.stop();
        }
        if (visaoJogo != null) {
            visaoJogo.pauseGame();
        }
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }
}
