package com.example.basta_game.utils;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.example.basta_game.R;

public class Animations {

    public Animations() {
    }

    public static void animacionLanzada(View view) {

        Animation animation = AnimationUtils.loadAnimation(
                view.getContext(), R.anim.texto_lanzado);
        view.startAnimation(animation);
    }
    public static void animacionAparicion(View view) {

        Animation animation = AnimationUtils.loadAnimation(
                view.getContext(), R.anim.aparicion);
        view.startAnimation(animation);
    }
    public static  void animacionZoom(View view){

        Animation animation = AnimationUtils.loadAnimation(
                view.getContext(), R.anim.zoom);
        view.startAnimation(animation);
    }
}
