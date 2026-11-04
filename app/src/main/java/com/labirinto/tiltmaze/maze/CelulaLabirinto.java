package com.labirinto.tiltmaze.maze;

// Célula básica da grade do labirinto.
// Cada quadradinho tem 4 paredes (cima, direita, baixo, esquerda).
public class CelulaLabirinto {
    // Posição da célula na matriz (coluna e linha)
    public final int col;
    public final int row;

    // No começo todas as paredes tão de pé (true)
    public boolean topWall = true;
    public boolean rightWall = true;
    public boolean bottomWall = true;
    public boolean leftWall = true;

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

        // Se a outra tá na esquerda, abre minha parede esquerda e a direita dela
        if (diffCol == 1) {
            this.leftWall = false;
            other.rightWall = false;
        } else if (diffCol == -1) { // Se tá na direita
            this.rightWall = false;
            other.leftWall = false;
        } else if (diffRow == 1) { // Se tá em cima
            this.topWall = false;
            other.bottomWall = false;
        } else if (diffRow == -1) { // Se tá embaixo
            this.bottomWall = false;
            other.topWall = false;
        }
    }
}
