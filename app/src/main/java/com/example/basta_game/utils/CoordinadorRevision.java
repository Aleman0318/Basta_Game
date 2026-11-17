package com.example.basta_game.utils;

import android.os.*;
import android.widget.TextView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.basta_game.Adapters.TarjetasValidacionAdapter;
import com.google.firebase.database.*;
import java.util.function.*;

/** Owns one visible review. Ticks paint cached state; writes only at readiness/deadline boundaries. */
public final class CoordinadorRevision {
    private final DatabaseReference sala, offset, conexion;
    private final String partida,uid;
    private final int ronda;
    private final TarjetasValidacionAdapter adapter;
    private final ViewPager2 pager;
    private final Consumer<String> texto;
    private final Consumer<Boolean> habilitarVoto;
    private final Predicate<String> dialogoListo;
    private final Consumer<DataSnapshot> resolver;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=this::actualizar;
    private DataSnapshot ultimo;
    private ValueEventListener listener, listenerOffset, listenerConexion;
    private volatile boolean conectado, relojDisponible;
    private volatile long baseServidor,baseMonotona;
    private volatile boolean cerrado;
    private boolean escribiendo;
    private long proximaEscritura,proximaResolucion;
    private long ultimoAviso=-30000;

    public CoordinadorRevision(DatabaseReference sala,String partida,int ronda,String uid,
            TarjetasValidacionAdapter adapter,ViewPager2 pager,Consumer<String> texto,
            Consumer<Boolean> habilitarVoto,Predicate<String> dialogoListo,Consumer<DataSnapshot> resolver) {
        this.sala=sala; this.partida=partida; this.ronda=ronda; this.uid=uid; this.adapter=adapter;
        this.pager=pager; this.texto=texto; this.habilitarVoto=habilitarVoto;
        this.dialogoListo=dialogoListo; this.resolver=resolver;
        offset=FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        conexion=FirebaseDatabase.getInstance().getReference(".info/connected");
    }
    public long ahora() { return baseServidor+(SystemClock.elapsedRealtime()-baseMonotona); }
    public boolean disponible() { return !cerrado && conectado && relojDisponible; }
    public int indiceActual() { return ultimo==null ? -1 : (int)RelojRevision.numero(ultimo.child(RelojRevision.ruta(ronda)+"/indiceActual").getValue(),-1); }
    public long turnoActual() { return ultimo==null ? -1 : RelojRevision.numero(ultimo.child(RelojRevision.ruta(ronda)+"/reloj/turno").getValue(),-1); }
    public DataSnapshot votoParaCerrar(String id) {
        if(!disponible() || ultimo==null || id==null) return null;
        DataSnapshot r=ultimo.child(RelojRevision.ruta(ronda)+"/reloj");
        DataSnapshot voto=ultimo.child("votacionActiva/ronda_"+ronda);
        return "votos".equals(r.child("modo").getValue()) && id.equals(r.child("idVotacion").getValue()) &&
                r.child("inicio").getValue() instanceof Number && (ahora()>=RelojRevision.finTurno(r.child("inicio").getValue(),r.child("duracionMs").getValue(),false) || RelojRevision.todosVotaron(r.child("elegibles").getValue(),ultimo.child("jugadores").getValue(),voto.child("votos").getValue())) &&
                id.equals(voto.child("idVotacion").getValue()) && Boolean.TRUE.equals(voto.child("activa").getValue()) ? voto : null;
    }
    public void iniciar() {
        listenerOffset=offset.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) {
                Object valor=s.getValue(); relojDisponible=valor instanceof Number;
                if(relojDisponible) { baseServidor=System.currentTimeMillis()+((Number)valor).longValue(); baseMonotona=SystemClock.elapsedRealtime(); }
                actualizar();
            }
            @Override public void onCancelled(DatabaseError e) { relojDisponible=false; actualizar(); }
        });
        listenerConexion=conexion.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) { conectado=Boolean.TRUE.equals(s.getValue()); actualizar(); }
            @Override public void onCancelled(DatabaseError e) { conectado=false; actualizar(); }
        });
        listener=sala.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot s) { ultimo=s; actualizar(); }
            @Override public void onCancelled(DatabaseError e) { ultimo=null; texto.accept("No se pudo sincronizar la revision"); habilitarVoto.accept(false); }
        });
    }
    public void actualizar() {
        handler.removeCallbacks(tick);
        if(cerrado) return;
        handler.postDelayed(tick,250);
        if(ultimo==null || partida==null || !partida.equals(ultimo.child("idPartida").getValue()) ||
                RelojRevision.numero(ultimo.child("rondaActual").getValue(),-1)!=ronda) { habilitarVoto.accept(false); return; }
        DataSnapshot revision=ultimo.child(RelojRevision.ruta(ronda)), r=revision.child("reloj");
        int indice=(int)RelojRevision.numero(revision.child("indiceActual").getValue(),0);
        if(indice<0 || indice>=adapter.getItemCount()) return;
        boolean completa=Boolean.TRUE.equals(revision.child("completa").getValue());
        long tiempo=ahora(), inicio=RelojRevision.numero(r.child("inicio").getValue(),Long.MAX_VALUE);
        boolean propuestas="propuestas".equals(r.child("modo").getValue());
        boolean revisando="revision".equals(r.child("modo").getValue());
        boolean resultado="resultado".equals(r.child("modo").getValue());
        boolean vacia=revisando && RelojRevision.categoriaVacia(ultimo.child("jugadores").getValue(),
                ultimo.child("respuestas/ronda_"+ronda).getValue(),r.child("categoria").getValue(String.class));
        long fin=RelojRevision.finTurno(r.child("inicio").getValue(),r.child("duracionMs").getValue(),vacia);
        boolean activa=disponible() && tiempo>=inicio && tiempo<fin;
        for(int i=0;i<adapter.getItemCount();i++) adapter.estadoCompartido(i,
                !completa && i==indice && activa && propuestas,
                Boolean.TRUE.equals(revision.child("cerradas").child(String.valueOf(i)).getValue()) || (i==indice && resultado),completa);
        if(!completa && pager.getScrollState()==androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE && pager.getCurrentItem()!=indice) pager.setCurrentItem(indice,true);
        DataSnapshot voto=ultimo.child("votacionActiva/ronda_"+ronda);
        boolean hayVoto=Boolean.TRUE.equals(voto.child("activa").getValue());
        habilitarVoto.accept(activa && !propuestas && hayVoto && !voto.child("votos").hasChild(uid));
        String etiqueta=completa ? "Revision terminada" : !conectado ? "Reconectando..." : !relojDisponible ? "Sincronizando..." :
                inicio==Long.MAX_VALUE ? "Preparando respuestas..." : vacia ? "Respuestas vacías" :
                        Math.max(0,(fin-tiempo+999)/1000)+"\n"+(propuestas ? "Elige una respuesta para votar" : revisando ? "¡Revisa las respuestas de tus rivales!" : resultado ? "Puntuaciones de la categor\u00eda" : "Tiempo para votar");
        texto.accept(etiqueta);
        if(completa || !disponible()) return;
        long turno=RelojRevision.numero(r.child("turno").getValue(),-1);
        boolean listo=revisando ? adapter.categoriaPreparada(indice) : dialogoListo.test(r.child("idVotacion").getValue(String.class));
        if(r.exists() && r.child("inicio").getValue()==null && !r.child("listos").hasChild(uid) && listo) {
            escribir(datos -> RelojRevision.preparado(datos,partida,ronda,indice,turno,uid)); return;
        }
        // Deterministic clock transitions may be completed by any active member, not only a foreground host.
        if(!ultimo.child("jugadores").hasChild(uid) || Boolean.FALSE.equals(ultimo.child("jugadores").child(uid).child("estaConectado").getValue())) return;
        if(hayVoto && !propuestas && r.child("inicio").getValue() instanceof Number && (tiempo>=fin || RelojRevision.todosVotaron(r.child("elegibles").getValue(),ultimo.child("jugadores").getValue(),voto.child("votos").getValue())) &&
                SystemClock.elapsedRealtime()>=proximaResolucion) {
            proximaResolucion=SystemClock.elapsedRealtime()+1500; resolver.accept(voto); return;
        }
        boolean todos=true;
        for(DataSnapshot jugador:ultimo.child("jugadores").getChildren())
            if(!Boolean.FALSE.equals(jugador.child("estaConectado").getValue()) && !r.child("listos").hasChild(jugador.getKey())) todos=false;
        Object desde=r.child("preparandoDesde").getValue();
        boolean esperaAgotada=desde instanceof Number && tiempo-((Number)desde).longValue()>=RelojRevision.ESPERA_MS;
        if(!r.exists() || (hayVoto && propuestas) || (r.child("inicio").getValue()==null && (todos || esperaAgotada)) ||
                ((revisando || resultado || propuestas) && !hayVoto && tiempo>=fin)) escribir(datos -> RelojRevision.coordinar(datos,partida,ronda,uid,ahora()));
    }
    private interface Cambio { boolean aplicar(MutableData datos); }
    private void escribir(Cambio cambio) {
        if(escribiendo || SystemClock.elapsedRealtime()<proximaEscritura || !disponible()) return;
        escribiendo=true;
        sala.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData datos) {
                if(!disponible()) return Transaction.abort();
                if(datos.getValue()==null) return Transaction.success(datos);
                return cambio.aplicar(datos) ? Transaction.success(datos) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean committed,DataSnapshot s) {
                escribiendo=false;
                if(e!=null && !cerrado) { proximaEscritura=SystemClock.elapsedRealtime()+3000;
                    if(SystemClock.elapsedRealtime()-ultimoAviso>30000){ultimoAviso=SystemClock.elapsedRealtime();
                    NotificationHelper.showGlobal(e.getCode()==DatabaseError.PERMISSION_DENIED ? "No hay permiso para sincronizar esta sala" : "No se pudo confirmar el turno. Reintentaremos al recuperar la conexion",NotificationHelper.WARNING);} }
            }
        },false);
    }
    public void cerrar() {
        cerrado=true; handler.removeCallbacks(tick);
        if(listener!=null) sala.removeEventListener(listener);
        if(listenerOffset!=null) offset.removeEventListener(listenerOffset);
        if(listenerConexion!=null) conexion.removeEventListener(listenerConexion);
    }
}
