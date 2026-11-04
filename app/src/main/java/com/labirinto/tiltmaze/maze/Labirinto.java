package com.labirinto.tiltmaze.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

// Essa classe guarda a matriz inteira do labirinto.
// É uma matriz 2D de CelulaLabirinto com início (startCol, startRow) e saída (endCol, endRow).
// Agora com suporte a paredes móveis que se reconfiguram a partir da fase 8 sem travar o jogo!
public class Labirinto {
    private final int cols;
    private final int rows;
    private final CelulaLabirinto[][] cells;

    private int startCol;
    private int startRow;
    private int endCol;
    private int endRow;

    // Guarda qual parede tá em alerta pra mudar (piscando antes de abrir/fechar)
    public static class ParedeAlerta {
        public int col;
        public int row;
        public String direcao; // "RIGHT" ou "BOTTOM"
        public boolean vaiFechar; // true se tava aberta e vai fechar, false se tava fechada e vai abrir

        public ParedeAlerta(int col, int row, String direcao, boolean vaiFechar) {
            this.col = col;
            this.row = row;
            this.direcao = direcao;
            this.vaiFechar = vaiFechar;
        }
    }

    private final List<ParedeAlerta> paredesEmAlerta = new ArrayList<>();
    private final Random random = new Random();

    public Labirinto(int cols, int rows) {
        this(cols, rows, 0, 0, cols - 1, rows - 1);
    }

    public Labirinto(int cols, int rows, int startCol, int startRow, int endCol, int endRow) {
        this.cols = cols;
        this.rows = rows;
        this.startCol = startCol;
        this.startRow = startRow;
        this.endCol = endCol;
        this.endRow = endRow;
        this.cells = new CelulaLabirinto[cols][rows];

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                cells[c][r] = new CelulaLabirinto(c, r);
            }
        }
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public CelulaLabirinto getCell(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return null;
        }
        return cells[col][row];
    }

    public int getStartCol() {
        return startCol;
    }

    public void setStartCol(int startCol) {
        this.startCol = startCol;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getEndCol() {
        return endCol;
    }

    public void setEndCol(int endCol) {
        this.endCol = endCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    public List<ParedeAlerta> getParedesEmAlerta() {
        return paredesEmAlerta;
    }

    // Busca em Largura (BFS) pra checar se existe caminho livre entre duas células quaisquer.
    // Dica de SI: roda em fração de milissegundo numa matriz 9x16, garantindo que o jogo nunca fica travado!
    public boolean isSolvable(int sCol, int sRow, int tCol, int tRow) {
        if (sCol < 0 || sCol >= cols || sRow < 0 || sRow >= rows) return false;
        if (tCol < 0 || tCol >= cols || tRow < 0 || tRow >= rows) return false;
        if (sCol == tCol && sRow == tRow) return true;

        boolean[][] visitados = new boolean[cols][rows];
        Deque<CelulaLabirinto> fila = new ArrayDeque<>();

        CelulaLabirinto inicio = getCell(sCol, sRow);
        visitados[sCol][sRow] = true;
        fila.add(inicio);

        while (!fila.isEmpty()) {
            CelulaLabirinto atual = fila.poll();
            int c = atual.col;
            int r = atual.row;

            if (c == tCol && r == tRow) {
                return true;
            }

            // Cima
            if (!atual.topWall && r > 0 && !visitados[c][r - 1]) {
                visitados[c][r - 1] = true;
                fila.add(getCell(c, r - 1));
            }
            // Direita
            if (!atual.rightWall && c < cols - 1 && !visitados[c + 1][r]) {
                visitados[c + 1][r] = true;
                fila.add(getCell(c + 1, r));
            }
            // Baixo
            if (!atual.bottomWall && r < rows - 1 && !visitados[c][r + 1]) {
                visitados[c][r + 1] = true;
                fila.add(getCell(c, r + 1));
            }
            // Esquerda
            if (!atual.leftWall && c > 0 && !visitados[c - 1][r]) {
                visitados[c - 1][r] = true;
                fila.add(getCell(c - 1, r));
            }
        }

        return false;
    }

    // A partir da fase 8: escolhe uma parede interna pra avisar que vai abrir/fechar
    public void avisarProximaReconfiguracao() {
        limparAlertas();

        // Tenta achar até 2 paredes internas aleatórias
        for (int tentativa = 0; tentativa < 20 && paredesEmAlerta.size() < 2; tentativa++) {
            int c = random.nextInt(cols);
            int r = random.nextInt(rows);
            boolean ehDireita = random.nextBoolean();

            if (ehDireita && c < cols - 1) { // Parede interna direita
                CelulaLabirinto cell = getCell(c, r);
                CelulaLabirinto vizinho = getCell(c + 1, r);
                if (cell != null && vizinho != null) {
                    boolean vaiFechar = !cell.rightWall;
                    cell.rightWallAlert = true;
                    vizinho.leftWallAlert = true;
                    paredesEmAlerta.add(new ParedeAlerta(c, r, "RIGHT", vaiFechar));
                }
            } else if (!ehDireita && r < rows - 1) { // Parede interna de baixo
                CelulaLabirinto cell = getCell(c, r);
                CelulaLabirinto vizinho = getCell(c, r + 1);
                if (cell != null && vizinho != null) {
                    boolean vaiFechar = !cell.bottomWall;
                    cell.bottomWallAlert = true;
                    vizinho.topWallAlert = true;
                    paredesEmAlerta.add(new ParedeAlerta(c, r, "BOTTOM", vaiFechar));
                }
            }
        }
    }

    // Executa a troca das paredes que estavam em alerta, testando antes se não vai isolar a bolinha da saída
    public void executarReconfiguracao(int ballCol, int ballRow) {
        for (ParedeAlerta alerta : paredesEmAlerta) {
            CelulaLabirinto cell = getCell(alerta.col, alerta.row);
            CelulaLabirinto vizinho = (alerta.direcao.equals("RIGHT"))
                    ? getCell(alerta.col + 1, alerta.row)
                    : getCell(alerta.col, alerta.row + 1);

            if (cell != null && vizinho != null) {
                // Alterna temporariamente pra ver se o labirinto continua com saída
                cell.toggleWallWithNeighbor(alerta.direcao, vizinho);

                // Testa se a bolinha ainda consegue chegar na meta
                if (!isSolvable(ballCol, ballRow, endCol, endRow)) {
                    // Se trancou o caminho, desfaz na hora! (Garantia matemática de solvabilidade)
                    cell.toggleWallWithNeighbor(alerta.direcao, vizinho);
                }
            }
        }

        limparAlertas();
    }

    public void limparAlertas() {
        for (ParedeAlerta alerta : paredesEmAlerta) {
            CelulaLabirinto cell = getCell(alerta.col, alerta.row);
            if (cell != null) {
                cell.topWallAlert = false;
                cell.rightWallAlert = false;
                cell.bottomWallAlert = false;
                cell.leftWallAlert = false;
            }
            CelulaLabirinto vizinho = (alerta.direcao.equals("RIGHT"))
                    ? getCell(alerta.col + 1, alerta.row)
                    : getCell(alerta.col, alerta.row + 1);
            if (vizinho != null) {
                vizinho.topWallAlert = false;
                vizinho.rightWallAlert = false;
                vizinho.bottomWallAlert = false;
                vizinho.leftWallAlert = false;
            }
        }
        paredesEmAlerta.clear();
    }
}
