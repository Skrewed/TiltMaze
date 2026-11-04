package com.labirinto.tiltmaze.maze;

// Célula básica da grade do labirinto.
// Cada quadradinho tem 4 paredes (cima, direita, baixo, esquerda).
// A partir da fase 8, as paredes ganham flags de alerta pra avisar quando vão se mover!
public class CelulaLabirinto {
    // Posição da célula na matriz (coluna e linha)
    public final int col;
    public final int row;

    // No começo todas as paredes tão de pé (true)
    public boolean topWall = true;
    public boolean rightWall = true;
    public boolean bottomWall = true;
    public boolean leftWall = true;

    // Avisos visuais de alerta (pisca em âmbar) quando a parede vai abrir ou fechar
    public boolean topWallAlert = false;
    public boolean rightWallAlert = false;
    public boolean bottomWallAlert = false;
    public boolean leftWallAlert = false;

    // Pra saber se a busca do algoritmo já passou por aqui
    public boolean visited = false;

    public CelulaLabirinto(int col, int row) {
        this.col = col;
        this.row = row;
    }

    // Derruba a parede entre essa célula e a vizinha pra abrir caminho
    public void removeWallBetween(CelulaLabirinto other) {
        int diffCol = this.col - other.col;
        int diffRow = this.row - other.row;

        if (diffCol == 1) { // other tá na esquerda
            this.leftWall = false;
            other.rightWall = false;
        } else if (diffCol == -1) { // other tá na direita
            this.rightWall = false;
            other.leftWall = false;
        } else if (diffRow == 1) { // other tá em cima
            this.topWall = false;
            other.bottomWall = false;
        } else if (diffRow == -1) { // other tá embaixo
            this.bottomWall = false;
            other.topWall = false;
        }
    }

    // Alterna o estado de uma parede e da vizinha correspondente
    public void toggleWallWithNeighbor(String direcao, CelulaLabirinto vizinho) {
        switch (direcao) {
            case "RIGHT":
                this.rightWall = !this.rightWall;
                this.rightWallAlert = false;
                if (vizinho != null) {
                    vizinho.leftWall = this.rightWall;
                    vizinho.leftWallAlert = false;
                }
                break;
            case "BOTTOM":
                this.bottomWall = !this.bottomWall;
                this.bottomWallAlert = false;
                if (vizinho != null) {
                    vizinho.topWall = this.bottomWall;
                    vizinho.topWallAlert = false;
                }
                break;
        }
    }
}
