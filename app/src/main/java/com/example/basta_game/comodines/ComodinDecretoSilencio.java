package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.example.basta_game.utils.ConexionJuego;
import com.example.basta_game.utils.DecretoSilencio;
import com.example.basta_game.utils.PlazoRonda;
import com.example.basta_game.utils.RelojRevision;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import java.util.UUID;

/** Dos categorias con una vocal prohibida para toda la sala en esta ronda. */
public class ComodinDecretoSilencio extends BaseComodin {
    public static final String ID = "decreto_silencio";
    private final String partida;

    public ComodinDecretoSilencio(String sala, String partida, int ronda) {
        super(sala, ronda);
        this.partida = partida;
    }

    private boolean prevalidar(DataSnapshot sala, long ahora) {
        if (partida == null || !partida.equals(sala.child("idPartida").getValue(String.class)) ||
                !"jugando".equals(sala.child("fase").getValue(String.class)) ||
                !sala.child("jugadores").hasChild(miUid) ||
                sala.child("respuestas/ronda_"+rondaActual).getChildrenCount()>0 ||
                sala.child("decretoSilencio/ronda_"+rondaActual).exists() ||
                sala.child("categoriasActivas").getChildrenCount()<2 ||
                Boolean.TRUE.equals(sala.child("bastaPresionado").getValue(Boolean.class))) return false;
        Object n=sala.child("rondaActual").getValue();
        Object desde=sala.child("inicioRonda").getValue();
        Object tiempo=sala.child("tiempoRonda").getValue();
        if(!(n instanceof Number) || ((Number)n).intValue()!=rondaActual ||
                !(desde instanceof Number) || !(tiempo instanceof Number)) return false;
        long inicio=PlazoRonda.inicioRespuestas(((Number)desde).longValue(),rondaActual,
                RelojRevision.numero(sala.child("preparacionInicialMs").getValue(),0));
        long fin=PlazoRonda.calcularFin(inicio,((Number)tiempo).intValue(),null);
        return ahora>=inicio && fin-ahora>=20000L;
    }

    @Override public void activar(String atacanteNombre, ComodinCallback callback) {
        if (!ConexionJuego.disponible()) {callback.onError("Necesitas conexión para usar el Decreto");return;}
        final long version=ConexionJuego.version();
        salaRef.get().addOnCompleteListener(lectura -> {
            if(!lectura.isSuccessful() || lectura.getResult()==null || !ConexionJuego.vigente(version)) {
                callback.onError("No pudimos verificar la ronda; vuelve a intentar");return;
            }
            if(!prevalidar(lectura.getResult(),ConexionJuego.ahoraServidor())) {
                callback.onError("El Decreto requiere 2 categorías y 20 segundos libres, sin otro Decreto activo");return;
            }
            DatabaseReference unidades=usuarioRef.child("inventarioComodines").child(ID).child("cantidad");
            com.example.basta_game.utils.ConsumoInventario.descontar(unidades,(descontado,mensaje) -> {
                if(!descontado) {callback.onError(mensaje);return;}
                final String uso=UUID.randomUUID().toString();
                final long ahora=ConexionJuego.ahoraServidor();
                final int semilla=uso.hashCode();
                salaRef.runTransaction(new Transaction.Handler() {
                    @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                        return DecretoSilencio.preparar(sala,partida,rondaActual,miUid,uso,semilla,ahora)
                                ? Transaction.success(sala) : Transaction.abort();
                    }
                    @Override public void onComplete(DatabaseError error,boolean committed,DataSnapshot sala) {
                        if(error==null && committed) {callback.onSuccess("SILENCIO");return;}
                        // El efecto no fue confirmado: devolver la unidad descontada.
                        unidades.runTransaction(new Transaction.Handler() {
                            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData dato) {
                                Object v=dato.getValue();
                                if(!(v instanceof Number)) return Transaction.abort();
                                dato.setValue(((Number)v).longValue()+1);
                                return Transaction.success(dato);
                            }
                            @Override public void onComplete(DatabaseError e,boolean devuelto,DataSnapshot s) {
                                callback.onError(devuelto ? "Decreto no aplicado; recuperaste tu unidad" :
                                        "No se confirmó el Decreto. Comprueba tu inventario");
                            }
                        },false);
                    }
                },false);
            });
        });
    }
}
