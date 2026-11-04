package com.labirinto.tiltmaze.util;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

// Sintetizador de áudio feito na mão com onda senoidal via AudioTrack.
// Assim a gente não precisa colocar arquivo .mp3 ou .wav na pasta assets, o app fica levinho!
public class GerenciadorSom {

    private static final int SAMPLE_RATE = 22050;
    private static final long HIT_SOUND_COOLDOWN_MS = 140; // Mínimo de 140ms entre os barulhos de batida
    private static final float MIN_SOUND_INTENSITY = 100f; // Ignora se for só a bolinha encostando devagar

    private long lastHitSoundTime = 0;
    private AudioTrack hitTrack = null;
    private final Object hitLock = new Object();

    public GerenciadorSom() {
        initHitTrack();
    }

    // Pré-carrega o som da batida na memória uma vez só pra não engasgar o jogo
    private void initHitTrack() {
        try {
            int durationMs = 24;
            int numSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
            short[] buffer = new short[numSamples];
            double freq = 130.0; // Frequência mais grave, tipo um "pop" de madeira/pedra

            // Gera a onda senoidal com fade-out pra não estalar o alto-falante
            for (int i = 0; i < numSamples; i++) {
                double t = (double) i / SAMPLE_RATE;
                double envelope = Math.pow(1.0 - ((double) i / numSamples), 1.8);
                double sample = Math.sin(2.0 * Math.PI * freq * t) * envelope;
                buffer[i] = (short) (sample * 14000);
            }

            hitTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(buffer.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();

            hitTrack.write(buffer, 0, buffer.length);
        } catch (Exception ignored) {
            hitTrack = null;
        }
    }

    // Toca o barulho da batida quando a bolinha tromba na parede
    public void playWallHitSound(float intensity) {
        if (intensity < MIN_SOUND_INTENSITY) return;

        long now = System.currentTimeMillis();
        synchronized (hitLock) {
            // Se tocou faz menos de 140ms, descarta pra não sobrepor som
            if (now - lastHitSoundTime < HIT_SOUND_COOLDOWN_MS) {
                return;
            }
            lastHitSoundTime = now;

            if (hitTrack != null) {
                try {
                    // Dá reload no buffer estático e toca na hora sem recriar thread
                    hitTrack.stop();
                    hitTrack.reloadStaticData();
                    hitTrack.play();
                } catch (Exception e) {
                    initHitTrack();
                    if (hitTrack != null) {
                        try {
                            hitTrack.play();
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    // Toca duas notas musicais quando vence a fase (Dó -> Mi)
    public void playWinSound() {
        new Thread(() -> {
            try {
                int durationMs = 260;
                int numSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
                short[] buffer = new short[numSamples];

                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // Primeira metade toca C5 (523Hz), segunda metade toca E5 (659Hz)
                    double freq = (i < numSamples / 2) ? 523.25 : 659.25;
                    double envelope = 1.0 - ((double) i / numSamples);
                    double sample = Math.sin(2.0 * Math.PI * freq * t) * envelope;
                    buffer[i] = (short) (sample * 18000);
                }

                AudioTrack winTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(buffer.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();

                winTrack.write(buffer, 0, buffer.length);
                winTrack.play();
                Thread.sleep(durationMs + 40);
                winTrack.release(); // Libera memória depois que tocou
            } catch (Exception ignored) {}
        }).start();
    }

    // Toca som descendente de quando a bolinha é sugada pelo buraco negro (Fases 10+)
    public void playBlackHoleSound() {
        new Thread(() -> {
            try {
                int durationMs = 280;
                int numSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
                short[] buffer = new short[numSamples];

                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // Frequência vai caindo de 320Hz até 80Hz (efeito de queda/sucção)
                    double freq = 320.0 - (240.0 * ((double) i / numSamples));
                    double envelope = 1.0 - Math.pow((double) i / numSamples, 2.0);
                    double sample = Math.sin(2.0 * Math.PI * freq * t) * envelope;
                    buffer[i] = (short) (sample * 16000);
                }

                AudioTrack holeTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(buffer.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();

                holeTrack.write(buffer, 0, buffer.length);
                holeTrack.play();
                Thread.sleep(durationMs + 40);
                holeTrack.release();
            } catch (Exception ignored) {}
        }).start();
    }

    // Libera a memória do áudio quando fechar a tela
    public void release() {
        synchronized (hitLock) {
            if (hitTrack != null) {
                try {
                    hitTrack.stop();
                    hitTrack.release();
                } catch (Exception ignored) {}
                hitTrack = null;
            }
        }
    }
}
