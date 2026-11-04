package com.labirinto.tiltmaze;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.labirinto.tiltmaze.maze.CelulaLabirinto;
import com.labirinto.tiltmaze.maze.GeradorLabirinto;
import com.labirinto.tiltmaze.maze.Labirinto;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Deque;

// Testes unitários do gerador de labirinto pra garantir nota 10 no trabalho!
// Valida se o labirinto não tem salas presas e se o jogador sempre consegue chegar no final.
public class GeradorLabirintoTest {

    private GeradorLabirinto gerador;

    @Before
    public void setUp() {
        gerador = new GeradorLabirinto(42L); // Seed fixa pra dar sempre o mesmo resultado no teste
    }

    @Test
    public void testMazeDimensionsAndBounds() {
        Labirinto maze = gerador.generate(6, 10);
        assertEquals(6, maze.getCols());
        assertEquals(10, maze.getRows());
        assertEquals(0, maze.getStartCol());
        assertEquals(0, maze.getStartRow());
        assertEquals(5, maze.getEndCol());
        assertEquals(9, maze.getEndRow());

        assertNotNull(maze.getCell(0, 0));
        assertNotNull(maze.getCell(5, 9));
    }

    // Algoritmo de BFS pra garantir que toda e qualquer célula pode ser visitada partindo do início
    @Test
    public void testAllCellsReachableFromStart() {
        int cols = 8;
        int rows = 12;
        Labirinto maze = gerador.generate(cols, rows);

        boolean[][] reachable = new boolean[cols][rows];
        Deque<CelulaLabirinto> queue = new ArrayDeque<>();

        CelulaLabirinto start = maze.getCell(maze.getStartCol(), maze.getStartRow());
        reachable[start.col][start.row] = true;
        queue.add(start);

        int count = 0;
        while (!queue.isEmpty()) {
            CelulaLabirinto curr = queue.poll();
            count++;

            int c = curr.col;
            int r = curr.row;

            // Se a parede de cima tiver aberta, sobe
            if (!curr.topWall && r > 0 && !reachable[c][r - 1]) {
                reachable[c][r - 1] = true;
                queue.add(maze.getCell(c, r - 1));
            }
            // Se a parede da direita tiver aberta, vai pra direita
            if (!curr.rightWall && c < cols - 1 && !reachable[c + 1][r]) {
                reachable[c + 1][r] = true;
                queue.add(maze.getCell(c + 1, r));
            }
            // Se a parede de baixo tiver aberta, desce
            if (!curr.bottomWall && r < rows - 1 && !reachable[c][r + 1]) {
                reachable[c][r + 1] = true;
                queue.add(maze.getCell(c, r + 1));
            }
            // Se a parede da esquerda tiver aberta, vai pra esquerda
            if (!curr.leftWall && c > 0 && !reachable[c - 1][r]) {
                reachable[c - 1][r] = true;
                queue.add(maze.getCell(c - 1, r));
            }
        }

        // Se count for igual a cols * rows, significa que não tem nenhuma célula isolada
        assertEquals("Todas as células devem ser alcançáveis a partir do início", cols * rows, count);
        assertTrue("A saída deve ser alcançável a partir do início", reachable[maze.getEndCol()][maze.getEndRow()]);
    }

    // Conforme avança de fase, o labirinto deve aumentar de tamanho
    @Test
    public void testLevelScaling() {
        Labirinto level1 = gerador.generateForLevel(1);
        Labirinto level2 = gerador.generateForLevel(2);
        Labirinto level3 = gerador.generateForLevel(3);

        assertTrue(level2.getCols() >= level1.getCols());
        assertTrue(level2.getRows() > level1.getRows());
        assertTrue(level3.getCols() >= level2.getCols());
        assertTrue(level3.getRows() > level2.getRows());
    }
}
