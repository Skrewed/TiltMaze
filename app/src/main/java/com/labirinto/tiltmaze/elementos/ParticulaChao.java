package com.labirinto.tiltmaze.elementos;

// Partícula que cai no chão do labirinto quando o vagalume é atingido pelo raio da bolinha.
// Fica no chão iluminando o labirinto no escuro e vai enfraquecendo aos poucos até sumir!
// Dica de SI: reciclada em pool pra não ficar alocando memória na heap durante o jogo.
public class ParticulaChao {

    public float x;
    public float y;
    public float vx;
    public float vy;
    public float vida;
    public float duracao;
    public float raio;
    public float raioLuz;
    public boolean ativa = false;

    // Atualiza a posição da partícula com limites de contenção na tela
    public void update(float dt, float minX, float minY, float maxX, float maxY) {
        if (!ativa) return;

        vida += dt;
        if (vida >= duracao) {
            ativa = false;
            return;
        }

        // Nos primeiros 0.85 segundos a partícula viaja longe e assenta suavemente no piso
        if (vida < 0.85f) {
            x += vx * dt;
            y += vy * dt;
            float amortecimento = Math.max(0f, 1f - 3.2f * dt);
            vx *= amortecimento;
            vy *= amortecimento;

            // Não deixa a partícula voar pra fora do tabuleiro
            if (x < minX) {
                x = minX;
                vx = -vx * 0.4f;
            } else if (x > maxX) {
                x = maxX;
                vx = -vx * 0.4f;
            }
            if (y < minY) {
                y = minY;
                vy = -vy * 0.4f;
            } else if (y > maxY) {
                y = maxY;
                vy = -vy * 0.4f;
            }
        }
    }

    // Sobrecarga simples pra testes unitários
    public void update(float dt) {
        update(dt, -Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    // Intensidade de luz e brilho: brilha forte no vôo e pouso, depois esmaece gradualmente
    public float getIntensidade() {
        if (!ativa) return 0f;
        float progresso = vida / duracao;
        if (progresso < 0.20f) {
            return 1.0f; // Brilho total enquanto assenta no chão
        }
        // Decaimento suave até 0
        float t = (progresso - 0.20f) / 0.80f;
        return Math.max(0f, 1.0f - t);
    }
}
