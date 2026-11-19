package com.example.basta_game.utils;

import android.graphics.drawable.Drawable;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.graphics.drawable.Animatable;

/** Native indeterminate drawable; no bitmap or additional dependency. */
public final class CargaBoton {
    private CargaBoton(){}
    private static final android.os.Handler reloj=new android.os.Handler(android.os.Looper.getMainLooper());
    public static boolean iniciar(TextView b){
        if(b.getTag(com.example.basta_game.R.id.carga_boton)!=null)return false;
        Runnable limite=() -> {terminar(b);NotificationHelper.showGlobal("La operación está tardando. Comprueba tu conexión antes de reintentar",NotificationHelper.WARNING);};
        android.view.View.OnAttachStateChangeListener ciclo=new android.view.View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(android.view.View v){}
            public void onViewDetachedFromWindow(android.view.View v){terminar(b,limite);b.removeOnAttachStateChangeListener(this);}
        };
        b.setTag(com.example.basta_game.R.id.carga_ciclo,ciclo);b.addOnAttachStateChangeListener(ciclo);
        b.setTag(com.example.basta_game.R.id.carga_boton,limite);mostrar(b,true);reloj.postDelayed(limite,30000);return true;
    }
    public static Object token(TextView b){return b.getTag(com.example.basta_game.R.id.carga_boton);}
    public static boolean activa(TextView b,Object token){return token!=null && token(b)==token;}
    public static boolean terminar(TextView b,Object token){return activa(b,token) && terminar(b);}
    public static boolean activa(TextView b){return b.getTag(com.example.basta_game.R.id.carga_boton)!=null;}
    public static boolean terminar(TextView b){
        Object r=b.getTag(com.example.basta_game.R.id.carga_boton);if(!(r instanceof Runnable))return false;
        Object ciclo=b.getTag(com.example.basta_game.R.id.carga_ciclo);if(ciclo instanceof android.view.View.OnAttachStateChangeListener)b.removeOnAttachStateChangeListener((android.view.View.OnAttachStateChangeListener)ciclo);b.setTag(com.example.basta_game.R.id.carga_ciclo,null);
        reloj.removeCallbacks((Runnable)r);b.setTag(com.example.basta_game.R.id.carga_boton,null);mostrar(b,false);return true;
    }
    public static void mostrar(TextView boton,boolean cargando){
        Object guardado=boton.getTag(com.example.basta_game.R.id.carga_visual);
        Drawable actual=boton.getForeground();if(actual instanceof android.graphics.drawable.LayerDrawable){Drawable contenido=((android.graphics.drawable.LayerDrawable)actual).getDrawable(0);if(contenido instanceof Animatable)((Animatable)contenido).stop();}if(actual instanceof Animatable)((Animatable)actual).stop();
        if(!cargando){if(guardado instanceof Drawable[])boton.setForeground(((Drawable[])guardado)[0]);boton.setTag(com.example.basta_game.R.id.carga_visual,null);boton.setEnabled(true);return;}
        if(guardado==null)boton.setTag(com.example.basta_game.R.id.carga_visual,new Drawable[]{actual});
        Drawable d=new ProgressBar(boton.getContext()).getIndeterminateDrawable().mutate();float densidad=boton.getResources().getDisplayMetrics().density;int px=Math.round(18*densidad);d.setTint(boton.getCurrentTextColor());
        android.graphics.drawable.LayerDrawable capa=new android.graphics.drawable.LayerDrawable(new Drawable[]{d});capa.setLayerSize(0,px,px);capa.setLayerGravity(0,android.view.Gravity.END|android.view.Gravity.CENTER_VERTICAL);capa.setLayerInsetRight(0,Math.round(12*densidad));boton.setForeground(capa);boton.setEnabled(false);
        if(d instanceof Animatable)((Animatable)d).start();
    }
}