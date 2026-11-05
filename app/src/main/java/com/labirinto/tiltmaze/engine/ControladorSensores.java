package com.labirinto.tiltmaze.engine;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

// Aqui a gente cuida dos sensores do celular (Giroscópio / Acelerômetro).
// Pega o ângulo que o usuário tá inclinando o celular e passa pro motor físico.
public class ControladorSensores implements SensorEventListener {

    private final SensorManager sensorManager;
    private Sensor activeSensor;
    private boolean isRotationVector = false;

    // Matrizes pro cálculo da orientação 3D do aparelho
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    // Valores já filtrados pra não dar solavanco na bolinha
    private float filteredPitch = 0f;
    private float filteredRoll = 0f;

    // Quando o usuário clica em "Calibrar", a gente salva o ângulo atual aqui pra subtrair depois
    private float offsetPitch = 0f;
    private float offsetRoll = 0f;

    // Fator do filtro passa-baixa ultra responsivo: bolinha responde no mesmo instante sem atraso perceptível
    private static final float ALPHA = 0.65f;

    public ControladorSensores(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            // Primeiro tenta pegar o Rotation Vector, porque ele funde giroscópio + acelerômetro sem dar drift
            activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (activeSensor != null) {
                isRotationVector = true;
            } else {
                // Se o celular for mais humilde, tenta gravidade ou acelerômetro normal
                activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
                if (activeSensor == null) {
                    activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                }
            }
        }
    }

    // Verifica se o celular tem algum sensor que dê pra usar
    public boolean isSensorAvailable() {
        return activeSensor != null;
    }

    // Começa a escutar os eventos do sensor quando o jogo entra na tela
    public void start() {
        if (sensorManager != null && activeSensor != null) {
            sensorManager.registerListener(this, activeSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    // Para de escutar o sensor pra economizar bateria quando o jogo pausa ou vai pro fundo
    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // Pega a posição que o cara tá segurando o celular agora e diz que isso é o "ponto zero"
    public void calibrate() {
        this.offsetPitch = this.filteredPitch + this.offsetPitch;
        this.offsetRoll = this.filteredRoll + this.offsetRoll;
    }

    // Chamado pelo Android toda vez que o sensor detecta movimento
    @Override
    public void onSensorChanged(SensorEvent event) {
        float rawPitch;
        float rawRoll;

        if (isRotationVector) {
            // Transforma o vetor de rotação numa matriz e depois em ângulos de Euler
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            // Em modo retrato:
            // orientationAngles[1] é Pitch (frente/trás) em radianos
            // orientationAngles[2] é Roll (esquerda/direita) em radianos
            rawPitch = (float) Math.toDegrees(orientationAngles[1]);
            rawRoll = (float) Math.toDegrees(orientationAngles[2]);
        } else {
            // Leitura direta do acelerômetro caso não tenha giroscópio de fusão
            rawRoll = -event.values[0] * 5.0f;
            rawPitch = -event.values[1] * 5.0f;
        }

        // Aplica o desconto da calibração feita pelo usuário
        float diffPitch = rawPitch - offsetPitch;
        float diffRoll = rawRoll - offsetRoll;

        // Zona morta mínima de 0.2 grau: qualquer toquezinho de inclinação já move a bolinha
        if (Math.abs(diffPitch) < 0.2f) diffPitch = 0f;
        if (Math.abs(diffRoll) < 0.2f) diffRoll = 0f;

        // Trava em no máximo 60 graus pra não disparar a velocidade se virar o celular de ponta cabeça
        diffPitch = Math.max(-60f, Math.min(60f, diffPitch));
        diffRoll = Math.max(-60f, Math.min(60f, diffRoll));

        // Aplica a média ponderada (filtro passa-baixa) pra ficar um movimento bem fluido
        filteredPitch = filteredPitch + ALPHA * (diffPitch - filteredPitch);
        filteredRoll = filteredRoll + ALPHA * (diffRoll - filteredRoll);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Não precisa mexer aqui
    }

    public float getPitch() {
        return filteredPitch;
    }

    public float getRoll() {
        return filteredRoll;
    }
}
