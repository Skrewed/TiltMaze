package com.labirinto.tiltmaze;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.labirinto.tiltmaze.elementos.ParticulaChao;
import com.labirinto.tiltmaze.elementos.Vagalume;

import org.junit.Before;
import org.junit.Test;

// Testes unitários do vagalume e do rastro de partículas
public class VagalumeTest {

    private Vagalume vagalume;

    @Before
    public void setUp() {
        vagalume = new Vagalume(200f, 200f, 60f);
    }

    // O vagalume deve se mover continuamente em curvas fluidas
    @Test
    public void testVagalumeMovementAndFluidFlight() {
        float xInicial = vagalume.x;
        float yInicial = vagalume.y;

        // Atualiza por alguns quadros
        for (int i = 0; i < 20; i++) {
            vagalume.update(0.016f, 0f, 0f, 500f, 500f);
        }

        assertTrue("A posição X do vagalume deve ter mudado no vôo", Math.abs(vagalume.x - xInicial) > 0.01f);
        assertTrue("A posição Y do vagalume deve ter mudado no vôo", Math.abs(vagalume.y - yInicial) > 0.01f);
        assertTrue("O vagalume deve respeitar os limites mínimos", vagalume.x >= 0f && vagalume.y >= 0f);
        assertTrue("O vagalume deve respeitar os limites máximos", vagalume.x <= 500f && vagalume.y <= 500f);
    }

    // O rastro de partículas deve nascer e se manter ativo sem estourar a pool pré-alocada
    @Test
    public void testParticleTrailSpawnsAndRecycles() {
        Vagalume.Particula[] particulas = vagalume.getParticulas();
        assertNotNull(particulas);
        assertEquals(24, particulas.length);

        // Inicialmente nenhuma partícula ativa
        int ativasInicio = 0;
        for (Vagalume.Particula p : particulas) {
            if (p.ativa) ativasInicio++;
        }
        assertEquals(0, ativasInicio);

        // Voa por 0.5 segundo: deve soltar partículas no caminho
        for (int i = 0; i < 30; i++) {
            vagalume.update(0.016f, 0f, 0f, 500f, 500f);
        }

        int ativasDepois = 0;
        for (Vagalume.Particula p : particulas) {
            if (p.ativa) ativasDepois++;
        }
        assertTrue("O vagalume deve soltar partículas ativas no rastro", ativasDepois > 0);
    }

    // O pulso de luz do abdômen e o batimento de asa devem oscilar de forma senoidal suave
    @Test
    public void testPulseAndWingBeat() {
        for (int i = 0; i < 60; i++) {
            vagalume.update(0.016f, 0f, 0f, 500f, 500f);

            float pulso = vagalume.getFatorPulso();
            assertTrue("O pulso deve estar entre 0.7 e 1.3", pulso >= 0.7f && pulso <= 1.3f);

            float asa = vagalume.getBatimentoAsa();
            assertTrue("O batimento de asa deve estar normalizado entre -1 e 1", asa >= -1.0f && asa <= 1.0f);
        }
    }

    // Ciclo de vida com fade-in, pleno e fade-out antes de morrer
    @Test
    public void testLifeCycleAndOpacity() {
        // Logo ao nascer, opacidade começa em 0 e sobe
        float opInicio = vagalume.getOpacidade();
        assertTrue("Opacidade inicial deve ser pequena (fade-in)", opInicio < 0.2f);
        assertFalse("Vagalume recém-nascido não deve estar morto", vagalume.isMorto());

        // Voa por 2 segundos: deve atingir opacidade total (1.0f)
        for (int i = 0; i < 125; i++) {
            vagalume.update(0.016f, 0f, 0f, 500f, 500f);
        }
        assertEquals(1.0f, vagalume.getOpacidade(), 0.01f);

        // Voa por mais 8 segundos (superando o limite de 6-9s de vida)
        for (int i = 0; i < 500; i++) {
            vagalume.update(0.016f, 0f, 0f, 500f, 500f);
        }
        assertTrue("Após o tempo total de vida, o vagalume deve ser marcado como morto", vagalume.isMorto());
    }

    // Ao tomar o raio elétrico (clique), o vagalume deve morrer imediatamente
    @Test
    public void testMatarVagalume() {
        assertFalse("Vagalume deve começar vivo", vagalume.isMorto());
        vagalume.matar();
        assertTrue("Vagalume deve morrer imediatamente ao chamar matar()", vagalume.isMorto());
    }

    // Partículas caídas no chão devem se dispersar, assentar e diminuir a intensidade de luz até sumir
    @Test
    public void testParticulaChaoDispersalAndFade() {
        ParticulaChao p = new ParticulaChao();
        p.x = 100f;
        p.y = 100f;
        p.vx = 50f;
        p.vy = 50f;
        p.vida = 0f;
        p.duracao = 6.0f;
        p.raio = 3f;
        p.raioLuz = 80f;
        p.ativa = true;

        // No início, intensidade deve ser 1.0 (brilho máximo)
        assertEquals(1.0f, p.getIntensidade(), 0.001f);

        // Atualiza nos primeiros 0.2 segundos (fase de dispersão e assentamento)
        p.update(0.2f);
        assertTrue("Partícula deve ter se movido", p.x > 100f && p.y > 100f);
        assertEquals(1.0f, p.getIntensidade(), 0.001f);

        // Avança até a metade da vida (3.0s)
        p.update(2.8f);
        assertTrue("Intensidade deve estar diminuindo", p.getIntensidade() < 1.0f && p.getIntensidade() > 0f);

        // Avança até o fim da vida (6.0s)
        p.update(3.1f);
        assertFalse("Partícula deve desativar após atingir a duração total", p.ativa);
        assertEquals(0f, p.getIntensidade(), 0.001f);
    }

    // A partícula deve viajar uma distância significativa para não formar aglomerado ('blob')
    @Test
    public void testWideScatterDispersalDistance() {
        ParticulaChao p = new ParticulaChao();
        p.x = 200f;
        p.y = 200f;
        p.vx = 260f; // Velocidade típica de dispersão média
        p.vy = 0f;
        p.vida = 0f;
        p.duracao = 7.0f;
        p.ativa = true;

        // Atualiza por 0.85 segundos (fase de dispersão completa)
        for (int i = 0; i < 53; i++) {
            p.update(0.016f);
        }

        float distanciaPercorrida = p.x - 200f;
        // Deve viajar bem mais que os antigos 5-15 pixels, espalhando-se amplamente pelo corredor
        assertTrue("Partícula deve se dispersar por pelo menos 55 pixels", distanciaPercorrida > 55f);
    }

    // A partícula deve respeitar a contenção das bordas do labirinto
    @Test
    public void testParticleBoundaryConstrainment() {
        ParticulaChao p = new ParticulaChao();
        p.x = 480f;
        p.y = 200f;
        p.vx = 400f; // Disparando contra a borda direita (maxX = 500f)
        p.vy = 0f;
        p.vida = 0f;
        p.duracao = 7.0f;
        p.ativa = true;

        for (int i = 0; i < 30; i++) {
            p.update(0.016f, 0f, 0f, 500f, 500f);
        }

        assertTrue("A partícula não deve ultrapassar o limite máximo X", p.x <= 500f);
    }
}
