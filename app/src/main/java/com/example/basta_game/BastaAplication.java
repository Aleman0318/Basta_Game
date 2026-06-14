package com.example.basta_game;

import android.app.Application;
import com.example.basta_game.utils.Sounds;

public class BastaAplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Sounds.init(this);
    }
}
