package com.labirinto.tiltmaze.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

// Cuida da vibração do celular quando bate na parede ou quando vence a fase.
// Usa a API nova do Android se der, ou a antiga se for aparelho mais velho.
public class GerenciadorVibracao {

    private final Vibrator vibrator;
    private long lastWallVibeTime = 0;

    public GerenciadorVibracao(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    // Dá uma tremidinha rápida quando bate na parede
    public void vibrateWallHit(float intensity) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (intensity < 100f) return; // Se foi só um toquezinho de nada, nem vibra

        // Cooldown de 130ms pra não descarregar a bateria do cara vibrando sem parar
        long now = System.currentTimeMillis();
        if (now - lastWallVibeTime < 130) {
            return;
        }
        lastWallVibeTime = now;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int amplitude = Math.min(255, Math.max(30, (int) (intensity * 0.4f)));
            try {
                // Vibração de 12ms bem seca e rápida
                vibrator.vibrate(VibrationEffect.createOneShot(12, amplitude));
            } catch (Exception ignored) {}
        } else {
            vibrator.vibrate(12);
        }
    }

    // Vibração mais forte e dramática quando cai no buraco negro
    public void vibrateBlackHole() {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] timings = {0, 80, 50, 140};
            int[] amplitudes = {0, 200, 0, 255};
            try {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
            } catch (Exception ignored) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            vibrator.vibrate(new long[]{0, 80, 50, 140}, -1);
        }
    }

    // Vibração tátil seca e imediata do choque elétrico (clique no vagalume)
    public void vibrateZap() {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                vibrator.vibrate(VibrationEffect.createOneShot(22, 220));
            } catch (Exception ignored) {
                vibrator.vibrate(22);
            }
        } else {
            vibrator.vibrate(22);
        }
    }

    // Padrão de vibração comemorativo quando chega no objetivo final
    public void vibrateWin() {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] timings = {0, 45, 60, 90};
            int[] amplitudes = {0, 180, 0, 255};
            try {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
            } catch (Exception ignored) {
                vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            vibrator.vibrate(new long[]{0, 50, 60, 100}, -1);
        }
    }
}
