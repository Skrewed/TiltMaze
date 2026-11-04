package com.labirinto.tiltmaze.maze;

// Essa classe guarda a matriz inteira do labirinto.
// É uma matriz 2D de CelulaLabirinto com início (0,0) e saída lá no final.
public class Labirinto {
    private final int cols;
    private final int rows;
    private final CelulaLabirinto[][] cells;

    // Aonde a bolinha começa e onde tem que chegar pra vencer
    private final int startCol;
    private final int startRow;
    private final int endCol;
    private final int endRow;

    public Labirinto(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.cells = new CelulaLabirinto[cols][rows];

        // Preenche a matriz inteira com células novas
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                cells[c][r] = new CelulaLabirinto(c, r);
            }
        }

        // Padrão do jogo: nasce no canto superior esquerdo (0,0)
        // e a saída fica no canto inferior direito
        this.startCol = 0;
        this.startRow = 0;
        this.endCol = cols - 1;
        this.endRow = rows - 1;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    // Pega a célula na posição x,y verificando se não tá fora da matriz pra não dar ArrayIndexOutOfBounds
    public CelulaLabirinto getCell(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return null;
        }
        return cells[col][row];
    }

    public int getStartCol() {
        return startCol;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getEndCol() {
        return endCol;
    }

    public int getEndRow() {
        return endRow;
    }
}
