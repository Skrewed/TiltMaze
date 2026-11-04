package com.labirinto.tiltmaze.engine;

import com.labirinto.tiltmaze.elementos.BuracoNegro;
import com.labirinto.tiltmaze.maze.CelulaLabirinto;
import com.labirinto.tiltmaze.maze.Labirinto;

import java.util.List;

// Nosso motorzinho de física 2D feito em Java.
// Calcula a velocidade da bolinha com a gravidade da inclinação, atrito no chão,
// bate nas paredes, checa se caiu em buracos negros e se chegou na meta de vitória.
public class MotorFisico {

    // Interface pra avisar a tela quando a bolinha bater, cair no buraco ou ganhar
    public interface ListenerColisao {
        void onWallHit(float intensity);
        void onGoalReached();
        default void onBlackHoleCaptured() {} // Novo: quando a bolinha cai no buraco negro
    }

    private static final float TILT_ACCELERATION = 3800f; // Força turbinada: bolinha acelera rápido com qualquer inclinação!
    private static final float FRICTION = 2.4f;          // Atrito ligeiramente menor pro rolamento ficar bem ágil e responsivo
    private static final float RESTITUTION = 0.35f;       // Coeficiente de quique (elasticidade)

    private static final float RESTITUTION_THRESHOLD = 75f;  // Se tiver muito devagar, não quica (repouso)
    private static final float MIN_IMPACT_VELOCITY = 120f;   // Só faz barulho de batida se for choque real

    private boolean invertX = false;
    private boolean invertY = false;
    private boolean isRestingOnWall = false;

    private ListenerColisao listener;

    public void setCollisionListener(ListenerColisao listener) {
        this.listener = listener;
    }

    public void setInvertX(boolean invertX) {
        this.invertX = invertX;
    }

    public void setInvertY(boolean invertY) {
        this.invertY = invertY;
    }

    public boolean isInvertX() {
        return invertX;
    }

    public boolean isInvertY() {
        return invertY;
    }

    public void update(Bolinha ball, Labirinto maze, float tiltRoll, float tiltPitch, float dt,
                       float originX, float originY, float cellWidth, float cellHeight, float wallThickness) {
        update(ball, maze, null, tiltRoll, tiltPitch, dt, originX, originY, cellWidth, cellHeight, wallThickness);
    }

    // Roda a cada frame
    public void update(Bolinha ball, Labirinto maze, List<BuracoNegro> buracos, float tiltRoll, float tiltPitch, float dt,
                       float originX, float originY, float cellWidth, float cellHeight, float wallThickness) {
        if (maze == null || ball == null || dt <= 0f) return;

        // 1. Converte os ângulos de graus pra radianos
        float rollRad = (float) Math.toRadians(tiltRoll);
        float pitchRad = (float) Math.toRadians(tiltPitch);

        float ax = TILT_ACCELERATION * (float) Math.sin(rollRad);
        float ay = -TILT_ACCELERATION * (float) Math.sin(pitchRad);

        if (invertX) ax = -ax;
        if (invertY) ay = -ay;

        // 2. Física básica (Euler): V = V0 + a*t
        ball.vx += ax * dt;
        ball.vy += ay * dt;

        float damping = Math.max(0f, 1f - FRICTION * dt);
        ball.vx *= damping;
        ball.vy *= damping;

        float maxSpeed = Math.max(cellWidth, cellHeight) * 16f; // Velocidade máxima mais alta pros reflexos do jogador
        float currentSpeed = (float) Math.hypot(ball.vx, ball.vy);
        if (currentSpeed > maxSpeed) {
            float scale = maxSpeed / currentSpeed;
            ball.vx *= scale;
            ball.vy *= scale;
        }

        // 3. Sub-passos de integração
        int subSteps = 3;
        float subDt = dt / subSteps;
        float maxHitIntensity = 0f;
        boolean inContactAny = false;

        for (int step = 0; step < subSteps; step++) {
            ball.x += ball.vx * subDt;
            ball.y += ball.vy * subDt;

            float hitIntensity = resolveCollisions(ball, maze, originX, originY, cellWidth, cellHeight, wallThickness);
            if (hitIntensity > 0f) {
                if (hitIntensity > maxHitIntensity) {
                    maxHitIntensity = hitIntensity;
                }
            }
            if (checkIsTouchingAnyWall(ball, maze, originX, originY, cellWidth, cellHeight, wallThickness)) {
                inContactAny = true;
            }
        }

        if (maxHitIntensity >= MIN_IMPACT_VELOCITY) {
            if (!isRestingOnWall && listener != null) {
                listener.onWallHit(maxHitIntensity);
            }
            isRestingOnWall = true;
        } else if (!inContactAny) {
            isRestingOnWall = false;
        }

        // 4. Checa colisão com buracos negros ativos (Fases 10+)
        if (buracos != null && !buracos.isEmpty()) {
            for (BuracoNegro buraco : buracos) {
                if (buraco.checarCaptura(ball.x, ball.y, ball.radius)) {
                    // Caiu no buraco negro! Para a bolinha e joga pro início da fase
                    ball.stop();
                    float startX = originX + (maze.getStartCol() + 0.5f) * cellWidth;
                    float startY = originY + (maze.getStartRow() + 0.5f) * cellHeight;
                    ball.reset(startX, startY, ball.radius);

                    if (listener != null) {
                        listener.onBlackHoleCaptured();
                    }
                    return; // Retorna imediatamente pra não colidir com a saída no mesmo frame
                }
            }
        }

        // 5. Checa se o centro da bolinha alcançou a meta de saída verde
        float goalCenterX = originX + (maze.getEndCol() + 0.5f) * cellWidth;
        float goalCenterY = originY + (maze.getEndRow() + 0.5f) * cellHeight;
        float distToGoal = (float) Math.hypot(ball.x - goalCenterX, ball.y - goalCenterY);

        if (distToGoal <= Math.min(cellWidth, cellHeight) * 0.38f) {
            if (listener != null) {
                listener.onGoalReached();
            }
        }
    }

    private boolean checkIsTouchingAnyWall(Bolinha ball, Labirinto maze, float originX, float originY,
                                          float cellWidth, float cellHeight, float wallThickness) {
        float halfWall = wallThickness / 2f;
        float touchThreshold = 1.0f;

        float boardLeft = originX + halfWall;
        float boardTop = originY + halfWall;
        float boardRight = originX + maze.getCols() * cellWidth - halfWall;
        float boardBottom = originY + maze.getRows() * cellHeight - halfWall;

        if (ball.x - ball.radius <= boardLeft + touchThreshold) return true;
        if (ball.x + ball.radius >= boardRight - touchThreshold) return true;
        if (ball.y - ball.radius <= boardTop + touchThreshold) return true;
        if (ball.y + ball.radius >= boardBottom - touchThreshold) return true;

        float effectiveRadius = ball.radius + halfWall;
        int minCol = Math.max(0, (int) Math.floor((ball.x - effectiveRadius - originX) / cellWidth));
        int maxCol = Math.min(maze.getCols() - 1, (int) Math.floor((ball.x + effectiveRadius - originX) / cellWidth));
        int minRow = Math.max(0, (int) Math.floor((ball.y - effectiveRadius - originY) / cellHeight));
        int maxRow = Math.min(maze.getRows() - 1, (int) Math.floor((ball.y + effectiveRadius - originY) / cellHeight));

        for (int c = minCol; c <= maxCol; c++) {
            for (int r = minRow; r <= maxRow; r++) {
                CelulaLabirinto cell = maze.getCell(c, r);
                if (cell == null) continue;

                float x0 = originX + c * cellWidth;
                float x1 = x0 + cellWidth;
                float y0 = originY + r * cellHeight;
                float y1 = y0 + cellHeight;

                if (cell.topWall && distPointToSegmentSq(ball.x, ball.y, x0, y0, x1, y0) <= (effectiveRadius + touchThreshold) * (effectiveRadius + touchThreshold)) return true;
                if (cell.rightWall && distPointToSegmentSq(ball.x, ball.y, x1, y0, x1, y1) <= (effectiveRadius + touchThreshold) * (effectiveRadius + touchThreshold)) return true;
                if (cell.bottomWall && distPointToSegmentSq(ball.x, ball.y, x0, y1, x1, y1) <= (effectiveRadius + touchThreshold) * (effectiveRadius + touchThreshold)) return true;
                if (cell.leftWall && distPointToSegmentSq(ball.x, ball.y, x0, y0, x0, y1) <= (effectiveRadius + touchThreshold) * (effectiveRadius + touchThreshold)) return true;
            }
        }

        return false;
    }

    private float distPointToSegmentSq(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSq = dx * dx + dy * dy;
        if (lenSq == 0f) return (px - x1) * (px - x1) + (py - y1) * (py - y1);
        float t = Math.max(0f, Math.min(1f, ((px - x1) * dx + (py - y1) * dy) / lenSq));
        float cx = x1 + t * dx;
        float cy = y1 + t * dy;
        return (px - cx) * (px - cx) + (py - cy) * (py - cy);
    }

    private float resolveCollisions(Bolinha ball, Labirinto maze, float originX, float originY,
                                    float cellWidth, float cellHeight, float wallThickness) {
        float halfWall = wallThickness / 2f;
        float effectiveRadius = ball.radius + halfWall;

        float boardLeft = originX + halfWall;
        float boardTop = originY + halfWall;
        float boardRight = originX + maze.getCols() * cellWidth - halfWall;
        float boardBottom = originY + maze.getRows() * cellHeight - halfWall;

        float maxImpact = 0f;

        if (ball.x - ball.radius < boardLeft) {
            ball.x = boardLeft + ball.radius;
            if (ball.vx < 0) {
                float hitVel = Math.abs(ball.vx);
                if (hitVel > RESTITUTION_THRESHOLD) {
                    maxImpact = Math.max(maxImpact, hitVel);
                    ball.vx = -ball.vx * RESTITUTION;
                } else {
                    ball.vx = 0f;
                }
            }
        }
        if (ball.x + ball.radius > boardRight) {
            ball.x = boardRight - ball.radius;
            if (ball.vx > 0) {
                float hitVel = Math.abs(ball.vx);
                if (hitVel > RESTITUTION_THRESHOLD) {
                    maxImpact = Math.max(maxImpact, hitVel);
                    ball.vx = -ball.vx * RESTITUTION;
                } else {
                    ball.vx = 0f;
                }
            }
        }
        if (ball.y - ball.radius < boardTop) {
            ball.y = boardTop + ball.radius;
            if (ball.vy < 0) {
                float hitVel = Math.abs(ball.vy);
                if (hitVel > RESTITUTION_THRESHOLD) {
                    maxImpact = Math.max(maxImpact, hitVel);
                    ball.vy = -ball.vy * RESTITUTION;
                } else {
                    ball.vy = 0f;
                }
            }
        }
        if (ball.y + ball.radius > boardBottom) {
            ball.y = boardBottom - ball.radius;
            if (ball.vy > 0) {
                float hitVel = Math.abs(ball.vy);
                if (hitVel > RESTITUTION_THRESHOLD) {
                    maxImpact = Math.max(maxImpact, hitVel);
                    ball.vy = -ball.vy * RESTITUTION;
                } else {
                    ball.vy = 0f;
                }
            }
        }

        int minCol = Math.max(0, (int) Math.floor((ball.x - effectiveRadius - originX) / cellWidth));
        int maxCol = Math.min(maze.getCols() - 1, (int) Math.floor((ball.x + effectiveRadius - originX) / cellWidth));
        int minRow = Math.max(0, (int) Math.floor((ball.y - effectiveRadius - originY) / cellHeight));
        int maxRow = Math.min(maze.getRows() - 1, (int) Math.floor((ball.y + effectiveRadius - originY) / cellHeight));

        for (int c = minCol; c <= maxCol; c++) {
            for (int r = minRow; r <= maxRow; r++) {
                CelulaLabirinto cell = maze.getCell(c, r);
                if (cell == null) continue;

                float x0 = originX + c * cellWidth;
                float x1 = x0 + cellWidth;
                float y0 = originY + r * cellHeight;
                float y1 = y0 + cellHeight;

                if (cell.topWall) {
                    float impact = checkSegmentCollision(ball, x0, y0, x1, y0, effectiveRadius);
                    if (impact > maxImpact) maxImpact = impact;
                }
                if (cell.rightWall) {
                    float impact = checkSegmentCollision(ball, x1, y0, x1, y1, effectiveRadius);
                    if (impact > maxImpact) maxImpact = impact;
                }
                if (cell.bottomWall) {
                    float impact = checkSegmentCollision(ball, x0, y1, x1, y1, effectiveRadius);
                    if (impact > maxImpact) maxImpact = impact;
                }
                if (cell.leftWall) {
                    float impact = checkSegmentCollision(ball, x0, y0, x0, y1, effectiveRadius);
                    if (impact > maxImpact) maxImpact = impact;
                }
            }
        }

        return maxImpact;
    }

    private float checkSegmentCollision(Bolinha ball, float x1, float y1, float x2, float y2, float effectiveRadius) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSq = dx * dx + dy * dy;
        if (lenSq == 0f) return 0f;

        float t = Math.max(0f, Math.min(1f, ((ball.x - x1) * dx + (ball.y - y1) * dy) / lenSq));

        float closestX = x1 + t * dx;
        float closestY = y1 + t * dy;

        float distSq = (ball.x - closestX) * (ball.x - closestX) + (ball.y - closestY) * (ball.y - closestY);

        if (distSq < effectiveRadius * effectiveRadius) {
            float dist = (float) Math.sqrt(distSq);
            float overlap = effectiveRadius - dist;

            float nx = (dist > 0.0001f) ? (ball.x - closestX) / dist : 0f;
            float ny = (dist > 0.0001f) ? (ball.y - closestY) / dist : -1f;

            ball.x += nx * overlap;
            ball.y += ny * overlap;

            float velAlongNormal = ball.vx * nx + ball.vy * ny;
            if (velAlongNormal < 0f) {
                float hitVel = Math.abs(velAlongNormal);
                if (hitVel > RESTITUTION_THRESHOLD) {
                    ball.vx -= (1f + RESTITUTION) * velAlongNormal * nx;
                    ball.vy -= (1f + RESTITUTION) * velAlongNormal * ny;
                    return hitVel;
                } else {
                    ball.vx -= velAlongNormal * nx;
                    ball.vy -= velAlongNormal * ny;
                    return 0f;
                }
            }
        }

        return 0f;
    }
}
