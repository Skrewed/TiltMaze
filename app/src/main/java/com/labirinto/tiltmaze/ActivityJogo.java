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

// Tela principal da partida (GameActivity).
// Gerencia os sensores, o cronômetro, a HUD superior e o diálogo de vitória.
public class ActivityJogo extends AppCompatActivity implements VisaoJogo.ListenerEstadoJogo {

    // Constantes pra salvar no SharedPreferences as escolhas de inversão de eixo
    private static final String PREFS_NAME = "tilt_maze_prefs";
    private static final String KEY_INVERT_Y = "invert_y";
    private static final String KEY_INVERT_X = "invert_x";

    private VisaoJogo visaoJogo;
    private ControladorSensores controladorSensores;

    private TextView tvHudLevel;
    private TextView tvHudTimer;
    private FrameLayout overlayVictory;
    private TextView tvWinTime;

    private int faseAtual = 1;
    private long tempoInicioFase = 0;
    private boolean isTimerRunning = false;

    // Handler pra atualizar o texto do cronômetro a cada 100 milissegundos
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

        // Amarração dos componentes do layout XML
        visaoJogo = findViewById(R.id.gameView);
        tvHudLevel = findViewById(R.id.tvHudLevel);
        tvHudTimer = findViewById(R.id.tvHudTimer);
        overlayVictory = findViewById(R.id.overlayVictory);
        tvWinTime = findViewById(R.id.tvWinTime);

        Button btnHudMenu = findViewById(R.id.btnHudMenu);
        Button btnHudCalibrate = findViewById(R.id.btnHudCalibrate);
        Button btnHudAxes = findViewById(R.id.btnHudAxes);
        Button btnHudRestart = findViewById(R.id.btnHudRestart);
        Button btnNextLevel = findViewById(R.id.btnNextLevel);
        Button btnOverlayMenu = findViewById(R.id.btnOverlayMenu);

        // Carrega as configurações que o usuário salvou antes
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean savedInvertY = prefs.getBoolean(KEY_INVERT_Y, false);
        boolean savedInvertX = prefs.getBoolean(KEY_INVERT_X, false);
        visaoJogo.setInvertY(savedInvertY);
        visaoJogo.setInvertX(savedInvertX);

        // Inicia o giroscópio e passa pro SurfaceView
        controladorSensores = new ControladorSensores(this);
        visaoJogo.setSensorHandler(controladorSensores);
        visaoJogo.setGameStateListener(this);

        if (!controladorSensores.isSensorAvailable()) {
            Toast.makeText(this, R.string.sensor_warning, Toast.LENGTH_LONG).show();
        }

        // Botão de voltar pro menu inicial
        btnHudMenu.setOnClickListener(v -> finish());

        // Botão de calibrar a posição que o jogador tá segurando o celular
        btnHudCalibrate.setOnClickListener(v -> {
            controladorSensores.calibrate();
            Toast.makeText(ActivityJogo.this, R.string.toast_calibrated, Toast.LENGTH_SHORT).show();
        });

        // Botão pra abrir o diálogo de inverter eixos
        btnHudAxes.setOnClickListener(v -> showAxesDialog());

        // Botão de reiniciar a fase atual
        btnHudRestart.setOnClickListener(v -> restartLevel());

        // Botões do diálogo quando o cara ganha a fase
        btnNextLevel.setOnClickListener(v -> {
            faseAtual++;
            startLevel(faseAtual);
        });

        btnOverlayMenu.setOnClickListener(v -> finish());

        // Começa na fase 1
        startLevel(faseAtual);
    }

    // Abre um AlertDialog com checkbox pra inverter os eixos se o cara quiser
    private void showAxesDialog() {
        visaoJogo.pauseGame();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean currentInvertY = prefs.getBoolean(KEY_INVERT_Y, false);
        boolean currentInvertX = prefs.getBoolean(KEY_INVERT_X, false);

        String[] options = new String[]{
                getString(R.string.invert_pitch),
                getString(R.string.invert_roll)
        };
        boolean[] checkedItems = new boolean[]{currentInvertY, currentInvertX};

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_axes_title)
                .setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton(R.string.btn_close, (dialog, which) -> {
                    boolean newInvertY = checkedItems[0];
                    boolean newInvertX = checkedItems[1];

                    // Grava as preferências no banco local do app (SharedPreferences)
                    prefs.edit()
                            .putBoolean(KEY_INVERT_Y, newInvertY)
                            .putBoolean(KEY_INVERT_X, newInvertX)
                            .apply();

                    visaoJogo.setInvertY(newInvertY);
                    visaoJogo.setInvertX(newInvertX);
                })
                .setOnDismissListener(dialog -> {
                    if (overlayVictory.getVisibility() != View.VISIBLE) {
                        visaoJogo.resumeGame();
                    }
                })
                .show();
    }

    // Inicia uma nova fase e reseta o cronômetro
    private void startLevel(int level) {
        overlayVictory.setVisibility(View.GONE);
        tvHudLevel.setText(getString(R.string.hud_level, level));

        tempoInicioFase = System.currentTimeMillis();
        isTimerRunning = true;
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);

        visaoJogo.startLevel(level);
        visaoJogo.resumeGame();
    }

    // Reinicia a fase atual
    private void restartLevel() {
        overlayVictory.setVisibility(View.GONE);
        tempoInicioFase = System.currentTimeMillis();
        isTimerRunning = true;
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);

        visaoJogo.restartCurrentLevel();
        visaoJogo.resumeGame();
    }

    // Chamado pelo motor de jogo quando a bolinha atinge a saída
    @Override
    public void onLevelCompleted(int level) {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);

        long tempoFinal = System.currentTimeMillis() - tempoInicioFase;
        String formatado = formatTime(tempoFinal);

        tvWinTime.setText(getString(R.string.dialog_win_message, formatado));
        overlayVictory.setVisibility(View.VISIBLE);
    }

    // Formata milissegundos para string bonita MM:SS.D
    private String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long tenths = (millis % 1000) / 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths);
    }

    // Ciclo de vida: quando volta pro app, religa os sensores
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

    // Ciclo de vida: quando o app vai pro segundo plano, desliga o sensor pra economizar bateria
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
