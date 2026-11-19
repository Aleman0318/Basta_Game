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
    private static android.app.Dialog ventanaNotificacion;
    private static WeakReference<ViewGroup> anclaNotificacion=new WeakReference<>(null);
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
    private static boolean descarteEnCurso = false;
    private static boolean arrastrandoAviso = false;
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
    /** La ventana no toma foco; únicamente su tarjeta recibe el gesto hacia arriba. */
    private static void prepararDeslizamiento(MaterialCardView tarjeta) {
        final float densidad = tarjeta.getResources().getDisplayMetrics().density;
        final int umbral = Math.round(46f * densidad);
        final int sensibilidad = android.view.ViewConfiguration.get(tarjeta.getContext()).getScaledTouchSlop();
        tarjeta.setOnTouchListener(new View.OnTouchListener() {
            private float yInicial;
            private boolean moviendo;
            private ColaAvisos.Aviso tocado;
            @Override public boolean onTouch(View vista, android.view.MotionEvent evento) {
                switch (evento.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        if (descarteEnCurso) return true;
                        tocado = dibujado;
                        yInicial = evento.getRawY();
                        moviendo = false;
                        arrastrandoAviso = true;
                        vista.animate().cancel();
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (!arrastrandoAviso || tocado == null) return true;
                        float delta = evento.getRawY() - yInicial;
                        if (delta < -sensibilidad) moviendo = true;
                        if (moviendo) {
                            vista.setTranslationY(Math.min(0f, delta));
                            vista.setAlpha(Math.max(0.30f,
                                    1f + Math.min(0f, delta) / Math.max(1, vista.getHeight())));
                        }
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        if (!arrastrandoAviso) return true;
                        arrastrandoAviso = false;
                        float desplazamiento = evento.getRawY() - yInicial;
                        if (moviendo && desplazamiento <= -umbral && tocado == dibujado) {
                            descartarAviso(tocado);
                        } else {
                            vista.animate().translationY(0f).alpha(1f).setDuration(140).start();
                        }
                        return true;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        arrastrandoAviso = false;
                        vista.animate().translationY(0f).alpha(1f).setDuration(140).start();
                        return true;
                    default: return true;
                }
            }
        });
    }

    private static void descartarAviso(ColaAvisos.Aviso esperado) {
        if (descarteEnCurso) return;
        if (!avisos.descartar(esperado)) {
            // El mensaje se actualizó mientras se arrastraba (por ejemplo, compras).
            // No descartar el aviso actualizado ni dejar la tarjeta desplazada.
            if (card != null) card.animate().translationY(0f).alpha(1f).setDuration(140).start();
            render();
            return;
        }
        descarteEnCurso = true;
        handler.removeCallbacks(actualizar);
        final MaterialCardView objetivo = card;
        if (objetivo == null) { descarteEnCurso = false; render(); return; }
        objetivo.animate().cancel();
        objetivo.animate().translationY(-Math.max(objetivo.getHeight(), 90))
                .alpha(0f).setDuration(150).withEndAction(() -> {
                    if (card == objetivo) quitarHost();
                    descarteEnCurso = false;
                    render();
                }).start();
    }

    private static void quitarHost() {
        arrastrandoAviso = false;
        descarteEnCurso = false;
        if(ventanaNotificacion!=null) { ventanaNotificacion.dismiss(); ventanaNotificacion=null; }
        if(card!=null) {
            card.animate().cancel();
            if(card.getParent() instanceof ViewGroup) ((ViewGroup)card.getParent()).removeView(card);
        }
        card=null; dibujado=null; anclaNotificacion.clear();
    }
    private static void render() {
        if (descarteEnCurso) return;
        handler.removeCallbacks(actualizar);
        long ahora=SystemClock.elapsedRealtime();
        // No reanimar la tarjeta mientras el dedo la esté arrastrando.
        if (arrastrandoAviso) {
            handler.postDelayed(actualizar, 160);
            return;
        }
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
                if(card!=null && anclaNotificacion.get()!=root) quitarHost();
                if(card==null) {
                    FrameLayout contenido=new FrameLayout(a);
                    contenido.setClipChildren(false);
                    card=(MaterialCardView)LayoutInflater.from(a).inflate(R.layout.component_notification_card,contenido,false);
                    int margen=Math.round(16*a.getResources().getDisplayMetrics().density);
                    FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP);
                    contenido.addView(card,params);
                    card.setVisibility(View.VISIBLE);
                    card.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
                    prepararDeslizamiento(card);
                    // A dialog's content starts at its centered frame, not at the screen's top.
                    // App-owned window without overlay permission or focus; only the card is touchable.
                    ventanaNotificacion=new android.app.Dialog(a);
                    ventanaNotificacion.setContentView(contenido);
                    ventanaNotificacion.setCancelable(false);
                    Window ventana=ventanaNotificacion.getWindow();
                    ventana.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                    ventana.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    // No bloquea la pantalla: solo la pequeña ventana del aviso intercepta gestos.
                    // El resto de la app y el teclado conservan sus toques (NOT_TOUCH_MODAL).
                    ventana.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
                    ventana.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    ventana.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                    ventana.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                    WindowManager.LayoutParams atributos=ventana.getAttributes();atributos.y=margen;ventana.setAttributes(atributos);
                    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(ventana,true);
                    try {
                        ventanaNotificacion.show();
                        int ancho=a.getWindow().getDecorView().getWidth();
                        if(ancho<=0) ancho=a.getResources().getDisplayMetrics().widthPixels;
                        ventana.setLayout(Math.max(1,ancho-2*margen),ViewGroup.LayoutParams.WRAP_CONTENT);
                        anclaNotificacion=new WeakReference<>(root);
                    } catch(WindowManager.BadTokenException error) {
                        android.util.Log.w("NOTIFICACION","La pantalla dejo de estar disponible",error);
                        quitarHost();return;
                    }
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
