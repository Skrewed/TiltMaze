package com.labirinto.tiltmaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

// Menu Principal do jogo (MainActivity).
// Direto ao ponto: botão Iniciar pra abrir a partida e botão Sair pra fechar o app.
public class ActivityPrincipal extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnExit = findViewById(R.id.btnExit);

        // Quando clica em Iniciar, abre a tela do jogo com animação de fade
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(ActivityPrincipal.this, ActivityJogo.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Quando clica em Sair, mata todas as activities e fecha o aplicativo
        btnExit.setOnClickListener(v -> {
            finishAffinity();
        });
    }
}
