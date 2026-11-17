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
    public static int loadingPartidaSound;
    private static int loadingStream;
    public static void detenerTodo(){detenerLoadingPartida();if(soundPool!=null)soundPool.autoPause();}
    public static void detenerLoadingPartida(){if(soundPool!=null && loadingStream!=0) soundPool.stop(loadingStream);loadingStream=0;}
    public static int conteoFinalSound;
    public static int bastaSound;
    public static int conteoCombate;
    public static int disparoSound;

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
        loadingPartidaSound = soundPool.load(context, R.raw.loadingpartida, 1);
        conteoFinalSound = soundPool.load(context, R.raw.conteo_final, 1);
        bastaSound = soundPool.load(context, R.raw.bastaaa, 1);
        conteoCombate = soundPool.load(context, R.raw.cuenta_combate, 1);
        disparoSound = soundPool.load(context, R.raw.disparo, 1);

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
    public static void playLoadingPartida(){
        if (soundPool != null){
            detenerLoadingPartida();loadingStream=soundPool.play(loadingPartidaSound, 1f, 1f, 1, 0,1f);
        }
    }
    public static void conteoFinalPlay(){
        if (soundPool != null){
            soundPool.play(conteoFinalSound, 1f, 1f, 1, 0, 1);
        }
    }
    public static void bastaPlay(){
        if (soundPool != null){
            soundPool.play(bastaSound, 1f, 1f, 1,0,1f);
        }
    }

    public static void playCombate() {
        if (soundPool != null) {
            soundPool.play(conteoCombate, 1f, 1f, 1, 0, 1f);
        }
    }

    public static void playDisparo() {
        if (soundPool != null) {
            soundPool.play(disparoSound, 1f, 1f, 1, 0, 1f);
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
