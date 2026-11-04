package com.labirinto.tiltmaze.elementos;

// Buraco negro que aparece do nada no mapa a partir da fase 10.
// Fica aberto por alguns segundos. Se a bolinha cair dentro, teleporta de volta pro começo da fase!
// Depois de um tempo ele fecha e some, nascendo em outro lugar aleatório.
public class BuracoNegro {

    public enum Estado {
        SURGINDO, // Abrindo com animação (0.8s)
        ATIVO,    // Aberto e perigoso (4 a 5s)
        SUMINDO   // Fechando suavemente (0.8s)
    }

    public final int col;
    public final int row;
    public float centerX;
    public float centerY;
    public float raioMaximo;

    private float tempoAtual = 0f;
    private final float tempoSurgir = 0.8f;
    private final float tempoAtivo;
    private final float tempoSumir = 0.8f;

    private Estado estado = Estado.SURGINDO;
    private boolean morto = false;

    public BuracoNegro(int col, int row, float centerX, float centerY, float raioMaximo, float duracaoAtivo) {
        this.col = col;
        this.row = row;
        this.centerX = centerX;
        this.centerY = centerY;
        this.raioMaximo = raioMaximo;
        this.tempoAtivo = duracaoAtivo;
    }

    // Atualiza o ciclo de vida do buraco a cada frame
    public void update(float dt) {
        tempoAtual += dt;

        if (tempoAtual < tempoSurgir) {
            estado = Estado.SURGINDO;
        } else if (tempoAtual < tempoSurgir + tempoAtivo) {
            estado = Estado.ATIVO;
        } else if (tempoAtual < tempoSurgir + tempoAtivo + tempoSumir) {
            estado = Estado.SUMINDO;
        } else {
            morto = true;
        }
    }

    // Raio atual animado: cresce quando nasce e diminui quando fecha
    public float getRaioAtual() {
        if (estado == Estado.SURGINDO) {
            float progresso = tempoAtual / tempoSurgir;
            return raioMaximo * Math.min(1.0f, progresso);
        } else if (estado == Estado.ATIVO) {
            // Leve pulsação senoidal no tamanho pra parecer um vórtice vivo
            float pulso = 1.0f + 0.08f * (float) Math.sin(tempoAtual * 8.0);
            return raioMaximo * pulso;
        } else if (estado == Estado.SUMINDO) {
            float tempoNoSumir = tempoAtual - (tempoSurgir + tempoAtivo);
            float progresso = 1.0f - (tempoNoSumir / tempoSumir);
            return raioMaximo * Math.max(0f, progresso);
        }
        return 0f;
    }

    // Retorna true se a bolinha chegou muito perto e caiu no buraco
    public boolean checarCaptura(float ballX, float ballY, float ballRadius) {
        if (estado != Estado.ATIVO) return false;

        float dx = ballX - centerX;
        float dy = ballY - centerY;
        float distSq = dx * dx + dy * dy;
        float raioCaptura = raioMaximo * 0.75f;

        // Se o centro da bolinha entrou na boca do buraco, já era!
        return distSq <= (raioCaptura * raioCaptura);
    }

    public Estado getEstado() {
        return estado;
    }

    public boolean isMorto() {
        return morto;
    }
}
