package com.labirinto.tiltmaze.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

// Gerador de labirinto usando DFS (Busca em Profundidade / Recursive Backtracking).
// Aprendido na aula de Estrutura de Dados: usando uma Pilha pra não dar StackOverflow no Java.
// Garante que o labirinto não tem salas presas e sempre tem um caminho que dá pra ganhar.
public class GeradorLabirinto {

    private final Random random;

    public GeradorLabirinto() {
        this.random = new Random();
    }

    // Construtor com seed pra gente poder testar nos testes unitários
    public GeradorLabirinto(long seed) {
        this.random = new Random(seed);
    }

    // Conforme passa de fase, aumenta o tamanho da tela pra ficar mais difícil
    public Labirinto generateForLevel(int level) {
        int cols = Math.min(10, 4 + level);
        int rows = Math.min(18, 7 + level * 2);
        return generate(cols, rows);
    }

    // Cria o labirinto do zero quebrando as paredes
    public Labirinto generate(int cols, int rows) {
        cols = Math.max(3, cols);
        rows = Math.max(3, rows);

        Labirinto maze = new Labirinto(cols, rows);
        Deque<CelulaLabirinto> stack = new ArrayDeque<>();

        // Começa na célula (0,0)
        CelulaLabirinto current = maze.getCell(maze.getStartCol(), maze.getStartRow());
        current.visited = true;
        stack.push(current);

        // Enquanto tiver célula na pilha pra explorar
        while (!stack.isEmpty()) {
            current = stack.peek();
            List<CelulaLabirinto> unvisitedNeighbors = getUnvisitedNeighbors(maze, current);

            if (!unvisitedNeighbors.isEmpty()) {
                // Sorteia um vizinho que ainda não foi visitado
                CelulaLabirinto next = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                // Derruba a parede entre o atual e ele
                current.removeWallBetween(next);
                next.visited = true;
                // Empilha o vizinho pra continuar a busca a partir dele
                stack.push(next);
            } else {
                // Se não tem pra onde ir, desempilha (backtracking)
                stack.pop();
            }
        }

        return maze;
    }

    // Procura os vizinhos ortogonais (cima, direita, baixo, esquerda) que ainda não foram visitados
    private List<CelulaLabirinto> getUnvisitedNeighbors(Labirinto maze, CelulaLabirinto cell) {
        List<CelulaLabirinto> neighbors = new ArrayList<>(4);
        int c = cell.col;
        int r = cell.row;

        // Vizinho de cima
        CelulaLabirinto top = maze.getCell(c, r - 1);
        if (top != null && !top.visited) neighbors.add(top);

        // Vizinho da direita
        CelulaLabirinto right = maze.getCell(c + 1, r);
        if (right != null && !right.visited) neighbors.add(right);

        // Vizinho de baixo
        CelulaLabirinto bottom = maze.getCell(c, r + 1);
        if (bottom != null && !bottom.visited) neighbors.add(bottom);

        // Vizinho da esquerda
        CelulaLabirinto left = maze.getCell(c - 1, r);
        if (left != null && !left.visited) neighbors.add(left);

        return neighbors;
    }
}
