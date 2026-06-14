package com.example.basta_game.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.example.basta_game.R;

public class Sounds {
    private static SoundPool soundPool;

    public static int celebracionSound;
    public static int perdedorSound;
    public static int notificacionesSound;

    private static boolean initialized = false;

    public Sounds() {
    }

    public static void init(Context context) {

        if (initialized) return;

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attributes)
                .build();

        celebracionSound = soundPool.load(context, R.raw.celebracion, 1);
        perdedorSound = soundPool.load(context, R.raw.perder, 1);
        notificacionesSound = soundPool.load(context, R.raw.notificaciones, 1);

        initialized = true;
    }

    public static void playCelebracion() {
        if (soundPool != null) {
            soundPool.play(celebracionSound, 1f, 1f, 1, 0, 1f);
        }
    }
    public static void playPerdedor() {
        if (soundPool != null) {
            soundPool.play(perdedorSound, 1f, 1f, 1, 0, 1f);
        }
    }
    public static void playNotificaciones(){
        if (soundPool != null){
            soundPool.play(notificacionesSound, 1f,1f, 1, 0, 1f);
        }
    }
    public static void release() {
        if(soundPool != null){
            soundPool.release();
            soundPool = null;
            initialized = false;
        }
    }
}
