package com.example.basta_game.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.*;
import android.provider.Settings;

/** One local signal per user/match/round; never changes the shared deadline. */
public final class VibracionRonda {
    private static final int DURACION_MS = 250;
    private VibracionRonda() { }
    public static boolean esMomento(long restanteMs, boolean emitido) {
        return !emitido && restanteMs > 0 && restanteMs <= 10000;
    }

    /** True means the signal was claimed, including devices/settings without haptics. */
    public static synchronized boolean avisar(Context contexto, String uid, String partida, int ronda, long restanteMs) {
        if (contexto == null || uid == null || partida == null || partida.isEmpty() || ronda <= 0 || !esMomento(restanteMs, false)) return false;
        SharedPreferences avisos = contexto.getSharedPreferences("vibracionFinalRonda", Context.MODE_PRIVATE);
        String clave = uid + ":" + partida + ":" + ronda;
        if (!esMomento(restanteMs, avisos.getBoolean(clave, false))) return false;
        avisos.edit().putBoolean(clave, true).apply();
        try {
            if (Build.VERSION.SDK_INT < 33 && Settings.System.getInt(contexto.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0) return true;
            Vibrator vibrador = (Vibrator) contexto.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrador == null || !vibrador.hasVibrator()) return true;
            if (Build.VERSION.SDK_INT >= 33) {
                vibrador.vibrate(VibrationEffect.createOneShot(DURACION_MS, VibrationEffect.DEFAULT_AMPLITUDE),
                        new VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build());
            } else {
                AudioAttributes atributos = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build();
                if (Build.VERSION.SDK_INT >= 26) vibrador.vibrate(VibrationEffect.createOneShot(DURACION_MS, VibrationEffect.DEFAULT_AMPLITUDE), atributos);
                else vibrador.vibrate(DURACION_MS, atributos);
            }
        } catch (SecurityException | UnsupportedOperationException error) {
            android.util.Log.w("VIBRACION", "Vibracion no disponible en este dispositivo", error);
        }
        return true;
    }
}
