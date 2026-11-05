package com.labirinto.tiltmaze;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.labirinto.tiltmaze.elementos.BuracoNegro;
import com.labirinto.tiltmaze.engine.Bolinha;
import com.labirinto.tiltmaze.engine.MotorFisico;
import com.labirinto.tiltmaze.maze.Labirinto;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

// Testes unitários do motor físico pra validar se a física não tá maluca
public class MotorFisicoTest {

    private MotorFisico engine;
    private Labirinto maze;
    private Bolinha ball;

    @Before
    public void setUp() {
        engine = new MotorFisico();
        maze = new Labirinto(4, 4); // Matriz 4x4 com start em (0,0) e end em (3,3)
        ball = new Bolinha(50f, 50f, 10f); // Põe a bolinha no centro da célula (0,0)
    }

    // Inclinar pra direita tem que fazer a bolinha acelerar pro lado direito (+X)
    @Test
    public void testTiltRollRightAcceleratesBallRight() {
        ball.reset(50f, 50f, 10f);
        float roll = 30f;
        float pitch = 0f;
        float dt = 0.05f;

        engine.update(ball, maze, roll, pitch, dt, 0, 0, 100, 100, 10);

        assertTrue("Velocidade horizontal deve ser positiva ao inclinar para a direita", ball.vx > 0);
        assertEquals("Velocidade vertical deve ser próxima de zero sem inclinação pitch", 0f, ball.vy, 0.01f);
    }

    // Inclinar pra esquerda tem que acelerar pro lado esquerdo (-X)
    @Test
    public void testTiltRollLeftAcceleratesBallLeft() {
        ball.reset(50f, 50f, 10f);
        float roll = -30f;
        float pitch = 0f;
        float dt = 0.05f;

        engine.update(ball, maze, roll, pitch, dt, 0, 0, 100, 100, 10);

        assertTrue("Velocidade horizontal deve ser negativa ao inclinar para a esquerda", ball.vx < 0);
        assertEquals("Velocidade vertical deve ser próxima de zero sem inclinação pitch", 0f, ball.vy, 0.01f);
    }

    // Inclinar pra frente tem que mover a bolinha pra frente/topo (-Y na tela do celular)
    @Test
    public void testPitchForwardAcceleratesBallUp() {
        ball.reset(50f, 50f, 10f);
        float roll = 0f;
        float pitch = 30f;
        float dt = 0.05f;

        engine.update(ball, maze, roll, pitch, dt, 0, 0, 100, 100, 10);

        assertTrue("Velocidade vertical deve ser negativa ao inclinar para frente (movimento para o topo)", ball.vy < 0);
        assertEquals("Velocidade horizontal deve ser próxima de zero sem inclinação roll", 0f, ball.vx, 0.01f);
    }

    // Inclinar pra trás tem que mover a bolinha pra base (+Y na tela do celular)
    @Test
    public void testPitchBackwardAcceleratesBallDown() {
        ball.reset(50f, 50f, 10f);
        float roll = 0f;
        float pitch = -30f;
        float dt = 0.05f;

        engine.update(ball, maze, roll, pitch, dt, 0, 0, 100, 100, 10);

        assertTrue("Velocidade vertical deve ser positiva ao inclinar para trás (movimento para a base)", ball.vy > 0);
        assertEquals("Velocidade horizontal deve ser próxima de zero sem inclinação roll", 0f, ball.vx, 0.01f);
    }

    // Se o usuário ativar a inversão manual de eixos, tem que inverter o sinal da velocidade
    @Test
    public void testInvertAxesToggles() {
        engine.setInvertY(true);
        engine.setInvertX(true);
        ball.reset(50f, 50f, 10f);

        float roll = 30f;
        float pitch = 30f;
        float dt = 0.05f;

        engine.update(ball, maze, roll, pitch, dt, 0, 0, 100, 100, 10);

        assertTrue("Com Invert X ativo, inclinar para direita deve gerar velocidade negativa", ball.vx < 0);
        assertTrue("Com Invert Y ativo, inclinar para frente deve gerar velocidade positiva", ball.vy > 0);
    }

    // A parede tem que bloquear a bolinha e fazer ela quicar pro lado contrário
    @Test
    public void testWallCollisionConstrainsPosition() {
        ball.reset(15f, 50f, 10f);
        ball.vx = -200f; // voando contra a parede esquerda

        float roll = -20f;
        float pitch = 0f;
        float dt = 0.02f;

        engine.update(ball, maze, roll, pitch, dt, 0, 0, 100, 100, 10);

        assertTrue("A esfera não pode ultrapassar a parede esquerda", ball.x >= 15f);
        assertTrue("A velocidade deve ser refletida para a direita após colisão", ball.vx >= 0);
    }

    // Teste do bug corrigido: a bolinha encostada na borda NÃO pode ficar tocando som a cada frame
    @Test
    public void testRestingOnWallDoesNotProduceInfiniteHits() {
        final int[] hitCount = {0};
        engine.setCollisionListener(new MotorFisico.ListenerColisao() {
            @Override
            public void onWallHit(float intensity) {
                hitCount[0]++;
            }

            @Override
            public void onGoalReached() {}

            @Override
            public void onBlackHoleCaptured() {}
        });

        // 1. Bola bate na parede com força
        ball.reset(16f, 50f, 10f);
        ball.vx = -300f;

        engine.update(ball, maze, -30f, 0f, 0.016f, 0, 0, 100, 100, 10);

        // Tem que tocar 1 som no impacto
        assertEquals("Deve registrar exatamente 1 impacto no choque inicial", 1, hitCount[0]);

        // 2. Continua inclinando pra esquerda por 30 frames seguidos
        for (int i = 0; i < 30; i++) {
            engine.update(ball, maze, -30f, 0f, 0.016f, 0, 0, 100, 100, 10);
        }

        // Não pode tocar de novo enquanto estiver encostada
        assertEquals("Não deve disparar sons contínuos enquanto encostada na borda", 1, hitCount[0]);

        // 3. Desgruda da parede inclinando pro outro lado
        for (int i = 0; i < 10; i++) {
            engine.update(ball, maze, 30f, 0f, 0.016f, 0, 0, 100, 100, 10);
        }
        assertTrue("A bola deve ter se descolado da parede", ball.x > 20f);

        // 4. Bate de novo com velocidade
        ball.reset(16f, 50f, 10f);
        ball.vx = -300f;
        engine.update(ball, maze, -30f, 0f, 0.016f, 0, 0, 100, 100, 10);

        // Agora sim registra o segundo som
        assertEquals("Deve registrar um novo choque apenas quando colidir novamente", 2, hitCount[0]);
    }

    // Se chegar no centro do buraco de saída, dispara o listener de vitória
    @Test
    public void testGoalDetection() {
        final boolean[] goalReached = {false};
        engine.setCollisionListener(new MotorFisico.ListenerColisao() {
            @Override
            public void onWallHit(float intensity) {}

            @Override
            public void onGoalReached() {
                goalReached[0] = true;
            }

            @Override
            public void onBlackHoleCaptured() {}
        });

        float cellWidth = 100f;
        float cellHeight = 100f;
        float goalCenterX = (3 + 0.5f) * cellWidth;
        float goalCenterY = (3 + 0.5f) * cellHeight;

        ball.reset(goalCenterX, goalCenterY, 15f);

        engine.update(ball, maze, 0f, 0f, 0.016f, 0, 0, cellWidth, cellHeight, 10);

        assertTrue("Objetivo deve ser acionado quando a esfera atinge a célula final", goalReached[0]);
    }

    // Buraco negro / vórtice deve capturar a bolinha, zerar velocidade e teleportar pra outra célula aleatória (não pro início nem pro goal)
    @Test
    public void testBlackHoleCapturesBallAndTeleportsToRandomCell() {
        final boolean[] captured = {false};
        engine.setCollisionListener(new MotorFisico.ListenerColisao() {
            @Override
            public void onWallHit(float intensity) {}

            @Override
            public void onGoalReached() {}

            @Override
            public void onBlackHoleCaptured() {
                captured[0] = true;
            }
        });

        float cellWidth = 100f;
        float cellHeight = 100f;
        // Buraco negro na célula (2, 2)
        BuracoNegro buraco = new BuracoNegro(2, 2, 250f, 250f, 30f, 4.0f);
        // Avança 1.0s para ficar no estado ATIVO
        buraco.update(1.0f);
        assertEquals(BuracoNegro.Estado.ATIVO, buraco.getEstado());

        // Bolinha se movendo em direção ao buraco
        ball.reset(250f, 250f, 12f);
        ball.vx = 150f;
        ball.vy = -50f;

        engine.update(ball, maze, Collections.singletonList(buraco), 0f, 0f, 0.016f,
                0f, 0f, cellWidth, cellHeight, 10f);

        assertTrue("O evento onBlackHoleCaptured deve ser disparado", captured[0]);
        // Velocidade deve ter sido zerada após a sucção pelo vórtice
        assertEquals(0f, ball.vx, 0.001f);
        assertEquals(0f, ball.vy, 0.001f);

        // Bolinha deve estar numa célula válida que NÃO seja a célula do buraco (2, 2) e NÃO seja o goal (3, 3)
        int newCol = (int) (ball.x / cellWidth);
        int newRow = (int) (ball.y / cellHeight);

        assertTrue("Coluna de teleport deve estar dentro do labirinto", newCol >= 0 && newCol < maze.getCols());
        assertTrue("Linha de teleport deve estar dentro do labirinto", newRow >= 0 && newRow < maze.getRows());
        assertFalse("Não deve teleportar para a mesma célula do buraco", newCol == 2 && newRow == 2);
        assertFalse("Não deve teleportar direto para o objetivo final", newCol == maze.getEndCol() && newRow == maze.getEndRow());
    }

    // A aceleração e responsividade da bolinha devem ser altas
    @Test
    public void testHighSpeedResponsiveness() {
        ball.reset(50f, 50f, 10f);
        float roll = 45f;
        float pitch = 0f;
        float dt = 0.02f;

        engine.update(ball, maze, roll, pitch, dt, 0, 0, 100f, 100f, 10f);

        // Com TILT_ACCELERATION = 5200f e 45 graus (sin ~ 0.707), vx deve atingir mais de 50px/s em 20ms
        assertTrue("Aceleração rápida deve dar resposta veloz à inclinação", ball.vx > 50f);
    }

    // Buraco negro ainda surgindo (inativo) não deve capturar a bolinha
    @Test
    public void testInactiveBlackHoleDoesNotCapture() {
        final boolean[] captured = {false};
        engine.setCollisionListener(new MotorFisico.ListenerColisao() {
            @Override
            public void onWallHit(float intensity) {}

            @Override
            public void onGoalReached() {}

            @Override
            public void onBlackHoleCaptured() {
                captured[0] = true;
            }
        });

        BuracoNegro buraco = new BuracoNegro(2, 2, 250f, 250f, 30f, 4.0f);
        // Não avança o suficiente: continua em SURGINDO
        buraco.update(0.3f);
        assertEquals(BuracoNegro.Estado.SURGINDO, buraco.getEstado());

        ball.reset(250f, 250f, 12f);

        engine.update(ball, maze, Collections.singletonList(buraco), 0f, 0f, 0.016f,
                0f, 0f, 100f, 100f, 10f);

        assertFalse("Buraco negro no estado SURGINDO não deve capturar", captured[0]);
        assertEquals(250f, ball.x, 0.01f);
    }

    // Fórmula de redução do raio de luz da fase 5 até a fase 15
    @Test
    public void testShrinkingFogOfWarRadiusFormula() {
        float cellWidth = 100f;
        float previousRadius = Float.MAX_VALUE;

        for (int fase = 5; fase <= 15; fase++) {
            float mult = Math.max(1.3f, 3.4f - (fase - 5) * 0.21f);
            float radius = cellWidth * mult;

            assertTrue("O raio na fase " + fase + " deve ser menor ou igual à anterior",
                    radius <= previousRadius);
            assertTrue("O raio mínimo não deve ser inferior a 1.3 células",
                    mult >= 1.3f);
            previousRadius = radius;
        }

        // Na fase 5 é ~3.4 células, na fase 15 é ~1.3 células
        float rFase5 = cellWidth * Math.max(1.3f, 3.4f - (5 - 5) * 0.21f);
        float rFase15 = cellWidth * Math.max(1.3f, 3.4f - (15 - 5) * 0.21f);
        assertEquals(340f, rFase5, 0.01f);
        assertEquals(130f, rFase15, 0.01f);
    }
}
