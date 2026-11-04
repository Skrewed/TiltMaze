package com.labirinto.tiltmaze.engine;

// Classe simples pra guardar os dados da bolinha que o jogador controla.
// Guarda posição (x,y), velocidade (vx,vy) e o tamanho do raio dela.
public class Bolinha {
    public float x;
    public float y;
    public float vx; // Velocidade horizontal
    public float vy; // Velocidade vertical
    public float radius;

    public Bolinha() {
        this(0, 0, 10);
    }

    public Bolinha(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.vx = 0f;
        this.vy = 0f;
        this.radius = radius;
    }

    // Quando começa o jogo ou reinicia a fase, coloca a bolinha de volta no ponto de partida
    public void reset(float startX, float startY, float newRadius) {
        this.x = startX;
        this.y = startY;
        this.vx = 0f;
        this.vy = 0f;
        this.radius = newRadius;
    }

    // Zera a velocidade quando ela chega na meta de vitória
    public void stop() {
        this.vx = 0f;
        this.vy = 0f;
    }
}
