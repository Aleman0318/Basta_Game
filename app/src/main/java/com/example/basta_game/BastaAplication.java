package com.example.basta_game;

import android.app.Application;
import com.example.basta_game.utils.Sounds;

public class BastaAplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        android.app.ActivityManager memoria=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
        if(memoria!=null && (memoria.isLowRamDevice() || memoria.getMemoryClass()<=128))
            com.bumptech.glide.Glide.get(this).setMemoryCategory(com.bumptech.glide.MemoryCategory.LOW);
        Sounds.init(this);
        com.example.basta_game.utils.NotificationHelper.inicializar(this);
        com.example.basta_game.utils.ConexionJuego.inicializar(this);
    }
}
