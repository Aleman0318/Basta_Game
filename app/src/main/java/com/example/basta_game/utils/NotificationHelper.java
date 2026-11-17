package com.example.basta_game.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.basta_game.R;
import com.google.android.material.card.MaterialCardView;
import java.lang.ref.WeakReference;

public class NotificationHelper {
    public static final int SUCCESS=1, ERROR=2, WARNING=3, INFO=4;
    private static final Handler handler=new Handler(Looper.getMainLooper());
    private static final ColaAvisos avisos=new ColaAvisos();
    private static ResumenCompras compras=new ResumenCompras();
    private static WeakReference<Activity> actividad=new WeakReference<>(null);
    private static MaterialCardView card;
    private static final java.util.List<WeakReference<android.app.Dialog>> dialogos=new java.util.ArrayList<>();
    public static void acompanar(android.app.Dialog dialogo) {
        ConexionJuego.superponer();
        if(dialogo==null || dialogo.getWindow()==null) return;
        dialogos.add(new WeakReference<>(dialogo));
        dialogo.getWindow().getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { handler.post(actualizar); }
            @Override public void onViewDetachedFromWindow(View v) { handler.post(actualizar); }
        });
        render();
    }
    private static ViewGroup hostVisible(Activity a) {
        for(int i=dialogos.size()-1;i>=0;i--) {
            android.app.Dialog d=dialogos.get(i).get();
            if(d==null || !d.isShowing()) { dialogos.remove(i); continue; }
            android.content.Context c=d.getContext();
            while(c instanceof android.content.ContextWrapper && !(c instanceof Activity)) c=((android.content.ContextWrapper)c).getBaseContext();
            if(c==a && d.getWindow()!=null) return d.getWindow().getDecorView().findViewById(android.R.id.content);
        }
        return a.findViewById(android.R.id.content);
    }
    private static ColaAvisos.Aviso dibujado;
    private static String uidCompras;
    private static long ultimaCompra=Long.MIN_VALUE;
    private static long ultimoSonidoTienda=Long.MIN_VALUE;
    private static final Runnable actualizar=NotificationHelper::render;

    public static void inicializar(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) {
                if(actividad.get()!=a) quitarHost();
                actividad=new WeakReference<>(a); render();
            }
            @Override public void onActivityPaused(Activity a) {
                if(actividad.get()==a) { quitarHost(); actividad.clear(); }
            }
            @Override public void onActivityDestroyed(Activity a) {
                if(actividad.get()==a) { quitarHost(); actividad.clear(); }
            }
            @Override public void onActivityCreated(Activity a,Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a,Bundle b) { }
        });
    }
    public static void show(View origen,String mensaje,int tipo) { showGlobal(mensaje,tipo); }
    public static void showGlobal(String mensaje,int tipo) { enviar(null,mensaje,tipo); }
    public static void avisoTienda(String mensaje,int tipo) {
        handler.post(() -> { enviar(null,mensaje,tipo); sonarTienda(); });
    }
    private static void sonarTienda() {
        long ahora=SystemClock.elapsedRealtime();
        if(ultimoSonidoTienda==Long.MIN_VALUE || ahora-ultimoSonidoTienda>=4000) {
            ultimoSonidoTienda=ahora; Sounds.playNotificaciones();
        }
    }
    public static void compra(String uid,String nombre) {
        handler.post(() -> {
            long ahora=SystemClock.elapsedRealtime();
            boolean nuevo= !uid.equals(uidCompras) || ultimaCompra==Long.MIN_VALUE || ahora-ultimaCompra>4000;
            if(!uid.equals(uidCompras)) { compras=new ResumenCompras(); uidCompras=uid; }
            ultimaCompra=ahora;
            enviar("compras:"+uid,compras.agregar(nombre,ahora),SUCCESS);
            if(nuevo) sonarTienda();
        });
    }
    private static void enviar(String clave,String mensaje,int tipo) {
        if(mensaje==null || mensaje.trim().isEmpty()) return;
        handler.post(() -> { avisos.agregar(clave,mensaje,tipo,SystemClock.elapsedRealtime()); render(); });
    }
    private static void quitarHost() {
        if(card!=null) {
            card.animate().cancel();
            if(card.getParent() instanceof ViewGroup) ((ViewGroup)card.getParent()).removeView(card);
        }
        card=null; dibujado=null;
    }
    private static void render() {
        handler.removeCallbacks(actualizar);
        long ahora=SystemClock.elapsedRealtime();
        ColaAvisos.Aviso aviso=avisos.actual(ahora);
        if(aviso==null) {
            if(card!=null) card.animate().alpha(0).translationY(-25).setDuration(220).withEndAction(() -> {
                if(card!=null && avisos.actual(SystemClock.elapsedRealtime())==null) quitarHost();
            }).start();
            dibujado=null; return;
        }
        Activity a=actividad.get();
        if(a!=null && !a.isFinishing() && !a.isDestroyed()) {
            ViewGroup root=hostVisible(a);
            if(root!=null) {
                if(card!=null && card.getParent()!=root) quitarHost();
                if(card==null) {
                    card=(MaterialCardView)LayoutInflater.from(a).inflate(R.layout.component_notification_card,root,false);
                    int margen=Math.round(16*a.getResources().getDisplayMetrics().density);
                    FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP);
                    int arriba=0;
                    WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(root);
                    if(insets!=null) arriba=insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                    params.setMargins(margen,arriba+margen,margen,0); root.addView(card,params);
                    card.setTranslationZ(100); card.setElevation(100);
                }
                card.animate().cancel(); aplicarDatosNotificacion(card,aviso.texto,aviso.tipo);
                TextView mensaje=card.findViewById(R.id.txtNotificationMessage);
                if(mensaje!=null) mensaje.setMaxLines(aviso.clave!=null && aviso.clave.startsWith("compras:") ? 12 : 4);
                card.bringToFront(); card.setVisibility(View.VISIBLE);
                if(dibujado==null) { card.setAlpha(0); card.setTranslationY(-25); }
                card.animate().alpha(1).translationY(0).setDuration(220).start();
                dibujado=aviso;
            }
        }
        handler.postDelayed(actualizar,Math.max(1,avisos.restante(ahora)));
    }

    private static void aplicarDatosNotificacion(MaterialCardView card, String message, int type) {
        View stripe = card.findViewById(R.id.viewNotificationStripe);
        MaterialCardView iconCard = card.findViewById(R.id.cardNotificationIcon);
        ImageView imgIcon = card.findViewById(R.id.img_iconNotificacion);
        TextView txtTitle = card.findViewById(R.id.txtNotificationTitle);
        TextView txtMessage = card.findViewById(R.id.txtNotificationMessage);

        if (txtMessage != null) {
            txtMessage.setText(message);
        }

        int mainColor;
        int softColor;
        int icon;
        String title;

        switch (type) {
            case SUCCESS:
                mainColor = Color.parseColor("#1E9B4B");
                softColor = Color.parseColor("#E8F8EE");
                icon = R.drawable.ic_notificacion_succes;
                title = "¡Excelente!";
                break;

            case ERROR:
                mainColor = Color.parseColor("#E63946");
                softColor = Color.parseColor("#FFECEF");
                icon = R.drawable.ic_notificacion_error;
                title = "Algo salió mal";
                break;

            case WARNING:
                mainColor = Color.parseColor("#FFB703");
                softColor = Color.parseColor("#FFF4CC");
                icon = R.drawable.ic_notificacion_warning;
                title = "Aviso del juego";
                break;

            case INFO:
            default:
                mainColor = Color.parseColor("#0B4DFF");
                softColor = Color.parseColor("#EAF2FF");
                icon = R.drawable.ic_notificacion_info;
                title = "Información";
                break;
        }

        card.setCardBackgroundColor(Color.parseColor("#FFFDF6"));
        card.setStrokeColor(mainColor);

        if (stripe != null) {
            stripe.setBackgroundColor(mainColor);
        }

        if (iconCard != null) {
            iconCard.setCardBackgroundColor(softColor);
            iconCard.setStrokeColor(mainColor);
        }

        if (imgIcon != null) {
            imgIcon.setImageResource(icon);
        }

        if (txtTitle != null) {
            txtTitle.setText(title);
            txtTitle.setTextColor(mainColor);
        }
    }
}
