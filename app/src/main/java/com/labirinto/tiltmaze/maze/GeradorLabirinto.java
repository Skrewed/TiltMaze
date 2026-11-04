package com.labirinto.tiltmaze.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

// Gerador procedural de labirintos usando DFS (Busca em Profundidade com Pilha).
// Totalmente procedural: cada vez que abre o app ou inicia a fase, cria um mapa novo!
// A partir da fase 5, sorteia início e fim em pontos aleatórios, garantindo que a saída verde
// fique bem distante do spawn do jogador através de cálculo de passos (BFS).
public class GeradorLabirinto {

    private final Random random;

    public GeradorLabirinto() {
        this.random = new Random();
    }

    // Construtor com seed fixa pra testes unitários
    public GeradorLabirinto(long seed) {
        this.random = new Random(seed);
    }

    // Escalonamento do tamanho do mapa até a fase 5 (depois trava no 9x16 que é o tamanho ideal pra tela)
    public Labirinto generateForLevel(int level) {
        int cols;
        int rows;

        switch (level) {
            case 1:
                cols = 5;
                rows = 8;
                break;
            case 2:
                cols = 6;
                rows = 10;
                break;
            case 3:
                cols = 7;
                rows = 12;
                break;
            case 4:
                cols = 8;
                rows = 14;
                break;
            default: // Fase 5 até a 15
                cols = 9;
                rows = 16;
                break;
        }

        return generate(cols, rows, level);
    }

    public Labirinto generate(int cols, int rows) {
        return generate(cols, rows, 1);
    }

    // Gera o labirinto do zero quebrando paredes via DFS
    public Labirinto generate(int cols, int rows, int level) {
        cols = Math.max(3, cols);
        rows = Math.max(3, rows);

        int startCol = 0;
        int startRow = 0;

        // A partir da fase 5, o ponto de início (spawn) é aleatório no mapa
        if (level >= 5) {
            startCol = random.nextInt(cols);
            startRow = random.nextInt(rows);
        }

        Labirinto maze = new Labirinto(cols, rows, startCol, startRow, cols - 1, rows - 1);
        Deque<CelulaLabirinto> stack = new ArrayDeque<>();

        CelulaLabirinto current = maze.getCell(startCol, startRow);
        current.visited = true;
        stack.push(current);

        // Algoritmo DFS (Recursive Backtracking) iterativo com pilha
        while (!stack.isEmpty()) {
            current = stack.peek();
            List<CelulaLabirinto> unvisitedNeighbors = getUnvisitedNeighbors(maze, current);

            if (!unvisitedNeighbors.isEmpty()) {
                CelulaLabirinto next = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                current.removeWallBetween(next);
                next.visited = true;
                stack.push(next);
            } else {
                stack.pop();
            }
        }

        // A partir da fase 5: sorteia a saída (goal verde) bem afastada do início
        if (level >= 5) {
            posicionarGoalVerdeAfastado(maze, startCol, startRow);
        } else {
            maze.setEndCol(cols - 1);
            maze.setEndRow(rows - 1);
        }

        return maze;
    }

    // Roda um BFS a partir do início pra achar as células mais distantes em número de passos
    private void posicionarGoalVerdeAfastado(Labirinto maze, int startCol, int startRow) {
        int cols = maze.getCols();
        int rows = maze.getRows();

        int[][] distancias = new int[cols][rows];
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                distancias[c][r] = -1;
            }
        }

        Deque<CelulaLabirinto> fila = new ArrayDeque<>();
        distancias[startCol][startRow] = 0;
        fila.add(maze.getCell(startCol, startRow));

        int maxDist = 0;

        while (!fila.isEmpty()) {
            CelulaLabirinto atual = fila.poll();
            int c = atual.col;
            int r = atual.row;
            int d = distancias[c][r];

            if (d > maxDist) {
                maxDist = d;
            }

            // Vizinhos acessíveis pelas paredes abertas
            if (!atual.topWall && r > 0 && distancias[c][r - 1] == -1) {
                distancias[c][r - 1] = d + 1;
                fila.add(maze.getCell(c, r - 1));
            }
            if (!atual.rightWall && c < cols - 1 && distancias[c + 1][r] == -1) {
                distancias[c + 1][r] = d + 1;
                fila.add(maze.getCell(c + 1, r));
            }
            if (!atual.bottomWall && r < rows - 1 && distancias[c][r + 1] == -1) {
                distancias[c][r + 1] = d + 1;
                fila.add(maze.getCell(c, r + 1));
            }
            if (!atual.leftWall && c > 0 && distancias[c - 1][r] == -1) {
                distancias[c - 1][r] = d + 1;
                fila.add(maze.getCell(c - 1, r));
            }
        }

        // Filtra todas as células que têm distância >= 70% da distância máxima do labirinto
        // e garante que a saída verde esteja fisicamente bem afastada do início (Manhattan >= 5)
        List<CelulaLabirinto> candidatasLonge = new ArrayList<>();
        int threshold = (int) (maxDist * 0.70f);

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                int manhattan = Math.abs(c - startCol) + Math.abs(r - startRow);
                if (distancias[c][r] >= threshold && manhattan >= 5) {
                    candidatasLonge.add(maze.getCell(c, r));
                }
            }
        }

        // Fallback de segurança: se nenhuma atingir o manhattan 5, aceita as de maior distância de passos
        if (candidatasLonge.isEmpty()) {
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    if (distancias[c][r] >= threshold && (c != startCol || r != startRow)) {
                        candidatasLonge.add(maze.getCell(c, r));
                    }
                }
            }
        }

        if (!candidatasLonge.isEmpty()) {
            // Sorteia uma das candidatas mais distantes pro buraco verde da vitória
            CelulaLabirinto escolhida = candidatasLonge.get(random.nextInt(candidatasLonge.size()));
            maze.setEndCol(escolhida.col);
            maze.setEndRow(escolhida.row);
        } else {
            maze.setEndCol(cols - 1);
            maze.setEndRow(rows - 1);
        }
    }

    private List<CelulaLabirinto> getUnvisitedNeighbors(Labirinto maze, CelulaLabirinto cell) {
        List<CelulaLabirinto> neighbors = new ArrayList<>(4);
        int c = cell.col;
        int r = cell.row;

        CelulaLabirinto top = maze.getCell(c, r - 1);
        if (top != null && !top.visited) neighbors.add(top);

        CelulaLabirinto right = maze.getCell(c + 1, r);
        if (right != null && !right.visited) neighbors.add(right);

        CelulaLabirinto bottom = maze.getCell(c, r + 1);
        if (bottom != null && !bottom.visited) neighbors.add(bottom);

        CelulaLabirinto left = maze.getCell(c - 1, r);
        if (left != null && !left.visited) neighbors.add(left);

        return neighbors;
    }
}
