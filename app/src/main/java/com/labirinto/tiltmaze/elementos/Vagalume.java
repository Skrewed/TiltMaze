package com.labirinto.tiltmaze.elementos;

import java.util.Random;

// Classe que simula o vagalume voando pelo labirinto.
// Agora muito mais bonitinho: tem corpinho bioluminescente, asinhas batendo,
// brilho pulsante e solta um rastro de partículas cintilantes (fairy dust) por onde voa!
// Dica de SI: pra não ficar criando e destruindo objeto todo frame (o que engasgaria o Garbage Collector),
// a gente usa uma pool fixa de partículas recicladas dentro de cada vagalume. Zero GC!
public class Vagalume {

    private static final Random random = new Random();

    // Entidade de partícula reciclada pro rastro cintilante
    public static class Particula {
        public float x;
        public float y;
        public float vx;
        public float vy;
        public float vida;
        public float duracao;
        public float raio;
        public boolean ativa;
    }

    public float x;
    public float y;
    private float vx;
    private float vy;

    private float angulo;
    private float velocidadeAngular;

    private float tempoVida;
    private final float duracaoTotal;
    public float raioLuz;

    // Frequência do pulso de luz do abdômen e das asinhas
    private final float fasePulso;
    private final float velocidadeVoo;

    // Pool de partículas pré-alocadas pra fazer o rastro brilhante sem alocações
    private static final int MAX_PARTICULAS = 24;
    private final Particula[] particulas = new Particula[MAX_PARTICULAS];
    private float timerSpawnParticula = 0f;

    public Vagalume(float startX, float startY, float baseRaioLuz) {
        this.x = startX;
        this.y = startY;
        this.raioLuz = baseRaioLuz * (1.15f + random.nextFloat() * 0.35f);

        // Inicializa a pool de partículas
        for (int i = 0; i < MAX_PARTICULAS; i++) {
            particulas[i] = new Particula();
            particulas[i].ativa = false;
        }

        // Direção inicial e velocidade orgânica
        this.angulo = (float) (random.nextFloat() * 2 * Math.PI);
        this.velocidadeVoo = 110f + random.nextFloat() * 40f;
        this.velocidadeAngular = 0f;

        // Variação pro pulso de cada vagalume ser único
        this.fasePulso = random.nextFloat() * 10f;

        // Dura entre 6 e 9 segundos
        this.duracaoTotal = 6.0f + random.nextFloat() * 3.0f;
        this.tempoVida = 0f;
    }

    // Atualiza o vôo suave e as partículas a cada frame
    public void update(float dt, float minX, float minY, float maxX, float maxY) {
        tempoVida += dt;

        // 1. Curva de vôo suave usando harmônicos combinados (seno + cosseno)
        // Evita giros bruscos e produz trajetórias em 'S' e voltinhas elegantes no ar
        float curvaNatural = (float) (Math.sin(tempoVida * 2.2 + fasePulso) * 2.2 + Math.cos(tempoVida * 1.1) * 1.4);
        velocidadeAngular += (curvaNatural - velocidadeAngular) * 4.0f * dt;
        angulo += velocidadeAngular * dt;

        // Variação sutil na velocidade (acelera nas retas, desacelera nas curvas)
        float velAtual = velocidadeVoo * (0.88f + 0.22f * (float) Math.sin(tempoVida * 3.2));
        vx = (float) Math.cos(angulo) * velAtual;
        vy = (float) Math.sin(angulo) * velAtual;

        // Leve flutuação vertical senoidal (sustentação de asas de inseto)
        float flutuacaoY = (float) Math.sin(tempoVida * 8.0) * 9f;

        x += vx * dt;
        y += (vy + flutuacaoY) * dt;

        // 2. Curva suave pra longe das bordas (steering behavior pra não bater de frente)
        float margem = 35f;
        if (x < minX + margem) {
            angulo += 3.5f * dt;
            if (x < minX) x = minX;
        } else if (x > maxX - margem) {
            angulo -= 3.5f * dt;
            if (x > maxX) x = maxX;
        }

        if (y < minY + margem) {
            angulo += 3.5f * dt;
            if (y < minY) y = minY;
        } else if (y > maxY - margem) {
            angulo -= 3.5f * dt;
            if (y > maxY) y = maxY;
        }

        // 3. Atualiza as partículas já ativas no rastro
        for (int i = 0; i < MAX_PARTICULAS; i++) {
            Particula p = particulas[i];
            if (p.ativa) {
                p.vida += dt;
                if (p.vida >= p.duracao) {
                    p.ativa = false;
                } else {
                    p.x += p.vx * dt;
                    p.y += p.vy * dt;
                    // Atrito aerodinâmico suave
                    p.vx *= (1f - 1.8f * dt);
                    p.vy *= (1f - 1.8f * dt);
                }
            }
        }

        // 4. Spawna nova partícula brilhante no rastro
        timerSpawnParticula += dt;
        if (timerSpawnParticula >= 0.045f) { // ~22 partículas por segundo
            timerSpawnParticula = 0f;
            soltarParticulaRastro();
        }
    }

    private void soltarParticulaRastro() {
        for (int i = 0; i < MAX_PARTICULAS; i++) {
            Particula p = particulas[i];
            if (!p.ativa) {
                p.ativa = true;
                // Nasce logo atrás do vagalume com leve dispersão natural
                float recuo = 3.5f;
                p.x = x - (float) Math.cos(angulo) * recuo + (random.nextFloat() - 0.5f) * 4f;
                p.y = y - (float) Math.sin(angulo) * recuo + (random.nextFloat() - 0.5f) * 4f;

                // Deriva ligeiramente pra trás e sobe de leve como fagulha dourada
                p.vx = -vx * 0.12f + (random.nextFloat() - 0.5f) * 12f;
                p.vy = -vy * 0.12f + (random.nextFloat() - 0.5f) * 12f - 7f;

                p.vida = 0f;
                p.duracao = 0.50f + random.nextFloat() * 0.35f;
                p.raio = 1.7f + random.nextFloat() * 1.5f;
                break;
            }
        }
    }

    // Escala do pulso de luz do abdômen (pisca entre 0.85 e 1.25)
    public float getFatorPulso() {
        return 1.0f + 0.22f * (float) Math.sin(tempoVida * 10.0 + fasePulso);
    }

    // Batimento das asinhas (-1 a 1 em alta frequência)
    public float getBatimentoAsa() {
        return (float) Math.sin(tempoVida * 38.0 + fasePulso);
    }

    public float getAngulo() {
        return angulo;
    }

    public Particula[] getParticulas() {
        return particulas;
    }

    // Opacidade com fade-in suave no início e fade-out no fim da vida
    public float getOpacidade() {
        if (tempoVida < 0.8f) {
            return tempoVida / 0.8f;
        }
        float restante = duracaoTotal - tempoVida;
        if (restante < 0.8f) {
            return Math.max(0f, restante / 0.8f);
        }
        return 1.0f;
    }

    // Mata o vagalume na hora quando atingido pelo raio da bolinha azul
    public void matar() {
        this.tempoVida = duracaoTotal;
    }

    public boolean isMorto() {
        return tempoVida >= duracaoTotal;
    }
}
