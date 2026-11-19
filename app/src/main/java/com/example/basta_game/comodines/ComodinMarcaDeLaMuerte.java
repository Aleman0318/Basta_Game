package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Random;

public class ComodinMarcaDeLaMuerte extends BaseComodin {

    public static final String ID_COMODIN = "marca_de_la_muerte";

    // Variables exclusivas de este comodín premium
    private String uidVictima;
    private String nombreVictima;

    public ComodinMarcaDeLaMuerte(String idSala, int rondaActual, String uidVictima, String nombreVictima) {
        super(idSala, rondaActual);
        this.uidVictima = uidVictima;
        this.nombreVictima = nombreVictima;
    }

    @Override public void activar(String nombreJugador, ComodinCallback callback) {
        if (uidVictima == null || uidVictima.equals(miUid)) {
            callback.onError("Selecciona un rival valido para el duelo");
            return;
        }
        // Fast preflight: no charge if somebody has already selected Next Round.
        // The root transaction below repeats this check to resolve simultaneous taps.
        salaRef.get().addOnSuccessListener(sala -> {
            if (!"revision_puntuaciones".equals(sala.child("fase").getValue(String.class)) ||
                    com.example.basta_game.utils.RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=rondaActual ||
                    Boolean.TRUE.equals(sala.child("minijuegoActual/activo").getValue(Boolean.class))) {
                callback.onError("El duelo no esta disponible en este momento");
                return;
            }
            for (com.google.firebase.database.DataSnapshot listo : sala.child("revisionLista/ronda_" + rondaActual).getChildren()) {
                if (Boolean.TRUE.equals(listo.getValue(Boolean.class))) {
                    callback.onError("Alguien ya pidio la siguiente ronda. No se puede iniciar otro duelo");
                    return;
                }
            }
            final int semilla=new Random().nextInt(6), delay=new Random().nextInt(3000);
            final String uso="marca_"+salaRef.push().getKey();
            descontarInventario(ID_COMODIN, callback, () -> salaRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @Override public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData salaActual) {
                    if(salaActual.getValue()==null) return com.google.firebase.database.Transaction.abort();
                    return com.example.basta_game.utils.SeleccionMinijuego.publicar(
                            salaActual,rondaActual,miUid,uidVictima,nombreJugador,nombreVictima,semilla,delay,uso)
                            ? com.google.firebase.database.Transaction.success(salaActual)
                            : com.google.firebase.database.Transaction.abort();
                }
                @Override public void onComplete(com.google.firebase.database.DatabaseError error,boolean committed,
                                                 com.google.firebase.database.DataSnapshot salaActual) {
                    if(error==null && committed) {
                        callback.onSuccess("SILENCIO");
                        return;
                    }
                    // Ready could have arrived just after inventory was discounted.
                    // Restore exactly this discounted unit, via another atomic counter transaction.
                    usuarioRef.child("inventarioComodines").child(ID_COMODIN).child("cantidad")
                            .runTransaction(new com.google.firebase.database.Transaction.Handler() {
                                @Override public com.google.firebase.database.Transaction.Result doTransaction(
                                        com.google.firebase.database.MutableData cantidad) {
                                    Object valorUnidades = cantidad.getValue();
                                    Number unidades = valorUnidades instanceof Number ? (Number) valorUnidades : null;
                                    if (unidades==null || unidades.longValue()<0 || unidades.longValue()>=Integer.MAX_VALUE)
                                        return com.google.firebase.database.Transaction.abort();
                                    cantidad.setValue(unidades.intValue()+1);
                                    return com.google.firebase.database.Transaction.success(cantidad);
                                }
                                @Override public void onComplete(com.google.firebase.database.DatabaseError e,boolean devuelto,
                                                                 com.google.firebase.database.DataSnapshot dato) {
                                    callback.onError(devuelto && e==null
                                            ? "Ya comenzaron a continuar o hay otro duelo. No se gasto tu comodin."
                                            : "No se inicio el duelo. Comprueba tu inventario antes de reintentar.");
                                }
                            },false);
                }
            },false));
        }).addOnFailureListener(error -> callback.onError("No se pudo comprobar si la ronda sigue disponible"));
    }
    /**
     * Da formato bonito al nombre del juego para el texto de notificación
     */
    private String formatearNombreJuego(String juego) {
        switch (juego) {
            case "piedra_papel_tijera": return "Piedra, Papel o Tijera";
            case "tres_en_raya": return "Tres en Raya Infinito";
            case "duelo_reflejos": return "Duelo de Reflejos";
            default: return "Muerte Súbita";
        }
    }
}
