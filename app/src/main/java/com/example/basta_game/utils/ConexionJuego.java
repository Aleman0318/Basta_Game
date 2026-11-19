package com.example.basta_game.utils;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.view.*;
import android.widget.*;
import com.example.basta_game.SalaActivity;
import com.google.firebase.database.*;
import java.lang.ref.WeakReference;

/** One process-wide connection source. Android network and RTDB transport are distinct. */
public final class ConexionJuego implements Application.ActivityLifecycleCallbacks {
    private static ConexionJuego instancia;
    private static volatile boolean disponible, offsetListo;
    private static volatile long version, offset;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private WeakReference<Activity> actual=new WeakReference<>(null);
    private ConnectivityManager redes;
    private Dialog aviso;
    private TextView banner,detalle;
    private Button reintentar;
    private boolean comprobando;
    private volatile boolean redValidada;
    private volatile boolean salaLista;
    private String error;
    private long desde, intento;
    public static long ahoraServidor(){return System.currentTimeMillis()+offset;}
    public static boolean disponible(){return instancia!=null && EstadoConexion.accionesDisponibles(disponible,offsetListo,instancia.redValidada,instancia.actual.get() instanceof SalaActivity,instancia.salaLista);}
    public static boolean transporteDisponible(){return instancia!=null && disponible && instancia.redValidada;}
    public static long version(){return version;}
    public static boolean vigente(long v){return disponible() && v==version;}
    private boolean hayRed(){Network n=redes.getActiveNetwork();NetworkCapabilities c=n==null?null:redes.getNetworkCapabilities(n);return c!=null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);}
    public static void inicializar(Application app){
        if(instancia!=null)return;
        ConexionJuego c=new ConexionJuego();instancia=c;app.registerActivityLifecycleCallbacks(c);
        c.redes=(ConnectivityManager)app.getSystemService(Application.CONNECTIVITY_SERVICE);
        c.redValidada=c.hayRed();
        c.redes.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
            @Override public void onLost(Network n){c.handler.post(() -> {c.redValidada=c.hayRed();if(!c.redValidada){if(disponible)version++;disponible=false;c.salaLista=false;}c.mostrar();});}
            @Override public void onAvailable(Network n){c.handler.post(() -> {c.redValidada=c.hayRed();c.mostrar();});}
            @Override public void onCapabilitiesChanged(Network n,NetworkCapabilities caps){c.handler.post(() -> {c.redValidada=c.hayRed();c.mostrar();});}
        });
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset").addValueEventListener(new ValueEventListener(){
            public void onDataChange(DataSnapshot s){if(s.getValue() instanceof Number){offset=((Number)s.getValue()).longValue();offsetListo=true;c.mostrar();}}
            public void onCancelled(DatabaseError e){c.error=e.getCode()==DatabaseError.PERMISSION_DENIED?"Firebase rechazó el acceso. Revisa tu sesión o los permisos.":e.getMessage();c.mostrar();}
        });
        FirebaseDatabase.getInstance().getReference(".info/connected").addValueEventListener(new ValueEventListener(){
            public void onDataChange(DataSnapshot s){boolean nueva=Boolean.TRUE.equals(s.getValue());if(disponible!=nueva)version++;disponible=nueva;if(!nueva)c.salaLista=false;if(nueva){c.error=null;SalidasPendientes.reconciliar(app);}c.mostrar();}
            public void onCancelled(DatabaseError e){disponible=false;version++;c.error="No se pudo comprobar Firebase: "+e.getMessage();c.mostrar();}
        });
    }
    private final Runnable persistente=this::mostrar;
    private void cerrar(){if(aviso!=null){aviso.dismiss();aviso=null;}detalle=null;reintentar=null;}
    private void quitarBanner(){if(banner!=null){ViewParent p=banner.getParent();if(p instanceof android.view.ViewGroup)((android.view.ViewGroup)p).removeView(banner);banner=null;}}
        public static void salaSincronizada(Activity a){if(instancia!=null && instancia.actual.get()==a){instancia.salaLista=true;instancia.mostrar();}}
    public static void errorSala(Activity a,Exception e){
        if(instancia==null || instancia.actual.get()!=a)return;
        instancia.error=e.getMessage()!=null && e.getMessage().toLowerCase(java.util.Locale.ROOT).contains("permission")?"No se pudo acceder a tu sala. Revisa tu sesión o los permisos.":"Todavía no se pudo recuperar la sala. Intenta nuevamente.";
        if(instancia.detalle!=null)instancia.detalle.setText(instancia.error);instancia.mostrar();
    }
    public static void superponer(){if(instancia!=null)instancia.handler.post(instancia::mostrar);}
    private void mostrar(){
        Activity a=actual.get();
        if(a==null || a.isFinishing() || a.isDestroyed()){cerrar();quitarBanner();return;}
        if(disponible() && comprobando)return;
        if(disponible()){desde=0;error=null;comprobando=false;intento++;handler.removeCallbacks(persistente);cerrar();quitarBanner();return;}
        if(a instanceof com.example.basta_game.SplashActivity)return;
        if(desde==0)desde=SystemClock.elapsedRealtime();
        boolean red=hayRed(),sala=a instanceof SalaActivity;
        if(red){
            cerrar();
            if(banner==null){
                // D4: aviso pulsable DENTRO del área táctil del teléfono, no debajo de la barra de estado.
                // Colocarlo sobre la barra de navegación impedía pulsar "Toca para comprobar".
                banner=new TextView(a);
                banner.setText("Conectando…");
                banner.setTextColor(Color.rgb(13,32,105));
                banner.setBackgroundColor(Color.rgb(255,247,214));
                float densidad=a.getResources().getDisplayMetrics().density;
                int pad=Math.round(13*densidad);
                banner.setPadding(pad,pad,pad,pad);
                banner.setGravity(Gravity.CENTER);
                banner.setTextSize(13);
                banner.setMinHeight(Math.round(48*densidad));
                banner.setElevation(7*densidad);
                android.widget.FrameLayout.LayoutParams ubicacion=new android.widget.FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.BOTTOM);
                ubicacion.setMargins(Math.round(12*densidad),0,Math.round(12*densidad),Math.round(80*densidad));
                ((android.view.ViewGroup)a.getWindow().getDecorView()).addView(banner,ubicacion);
            }
            handler.removeCallbacks(persistente);if(banner!=null){
                String mensaje=error;
                if(mensaje==null) mensaje=disponible && sala ? "Recuperando tu lugar en la sala…" : "Conectando con Firebase…";
                banner.setText(mensaje+" · Toca para comprobar");
                banner.setOnClickListener(v -> comprobar());
            }return;
        }
        quitarBanner();
        if(aviso!=null && aviso.isShowing())return;
        float dp=a.getResources().getDisplayMetrics().density;
        LinearLayout caja=new LinearLayout(a);caja.setOrientation(1);int pad=Math.round(24*dp);caja.setPadding(pad,pad,pad,pad);
        GradientDrawable fondo=new GradientDrawable();fondo.setColor(Color.rgb(255,251,237));fondo.setCornerRadius(24*dp);fondo.setStroke(Math.round(3*dp),Color.rgb(13,32,105));caja.setBackground(fondo);
        ImageView icono=new ImageView(a);icono.setImageResource(com.example.basta_game.R.drawable.ic_wifi_desconectado);caja.addView(icono,new LinearLayout.LayoutParams(-1,Math.round(56*dp)));
        texto(caja,"¡Sin conexión a internet!",25,true);
        texto(caja,"Necesitas conectarte para continuar jugando",17,false);
        detalle=texto(caja,"Estamos esperando que vuelva la red",14,false);
        reintentar=new Button(a);reintentar.setText("Reintentar");caja.addView(reintentar);reintentar.setOnClickListener(v -> comprobar());
        Button salir=new Button(a);salir.setText(sala?"Salir de la sala":"Cerrar");caja.addView(salir);
        salir.setOnClickListener(v -> {cerrar();if(a instanceof SalaActivity)((SalaActivity)a).salirLocalSinConexion();else a.finishAffinity();});
        ScrollView contenido=new ScrollView(a);contenido.addView(caja);aviso=new Dialog(a);aviso.setContentView(contenido);aviso.setCancelable(false);aviso.show();
    }
    private void comprobar(){
        if(comprobando)return;
        comprobando=true;long id=++intento;
        if(banner!=null)banner.setText("Comprobando…");
        if(reintentar!=null){reintentar.setText("Comprobando…");CargaBoton.mostrar(reintentar,true);}
        handler.postDelayed(() -> terminar(id,"Todavía no hay conexión. Intenta nuevamente"),8000);
        if(!hayRed()){terminar(id,"Todavía no hay conexión. Intenta nuevamente");return;}
        FirebaseDatabase.getInstance().goOnline();
        Activity a=actual.get();if(a instanceof SalaActivity)((SalaActivity)a).reintentarConexionSala();
        com.google.firebase.auth.FirebaseUser u=com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if(u==null)comprobarBase(id);else u.getIdToken(false).addOnCompleteListener(t -> {if(id!=intento)return;if(t.isSuccessful())comprobarBase(id);else terminar(id,t.getException() instanceof com.google.firebase.FirebaseNetworkException?"No se pudo contactar al servicio. Intenta nuevamente":"No se pudo validar tu sesión. Reintenta cuando vuelva la red.");});
    }
    private void comprobarBase(long id){
        FirebaseDatabase.getInstance().getReference(".info/connected").get().addOnCompleteListener(t -> {
            if(id!=intento)return;
            if(t.isSuccessful() && Boolean.TRUE.equals(t.getResult().getValue()) && transporteDisponible()) {
                comprobando=false; intento++;
                Activity actividad=actual.get();
                if(actividad instanceof SalaActivity && !disponible()) {
                    ((SalaActivity)actividad).reintentarConexionSala();
                    error=null; mostrar(); // Conectado, pero el asiento de sala sigue recuperándose.
                } else mostrar();
            } else terminar(id,"Firebase todavía se está reconectando. Intenta nuevamente");
        });
    }
    private void terminar(long id,String mensaje){if(id!=intento)return;comprobando=false;intento++;if(reintentar!=null){CargaBoton.mostrar(reintentar,false);reintentar.setText("Reintentar");}if(detalle!=null)detalle.setText(mensaje);if(banner!=null)banner.setText(mensaje+" · Toca para comprobar");}
    private TextView texto(LinearLayout caja,String valor,int sp,boolean titulo){TextView t=new TextView(caja.getContext());t.setText(valor);t.setTextSize(sp);t.setTextColor(Color.rgb(13,32,105));t.setGravity(17);t.setPadding(0,8,0,16);t.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(caja.getContext(),titulo?com.example.basta_game.R.font.lilitaone_regular:com.example.basta_game.R.font.manrope_regular));caja.addView(t);return t;}
    @Override public void onActivityResumed(Activity a){cerrar();quitarBanner();actual=new WeakReference<>(a);desde=0;if(transporteDisponible())SalidasPendientes.reconciliar(a.getApplicationContext());mostrar();}
    @Override public void onActivityPaused(Activity a){if(actual.get()==a){intento++;comprobando=false;handler.removeCallbacks(persistente);cerrar();quitarBanner();actual.clear();}}
    @Override public void onActivityDestroyed(Activity a){if(actual.get()==a){cerrar();quitarBanner();actual.clear();}}
    @Override public void onActivityCreated(Activity a,Bundle b){}
    @Override public void onActivityStarted(Activity a){}
    @Override public void onActivityStopped(Activity a){}
    @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
}
