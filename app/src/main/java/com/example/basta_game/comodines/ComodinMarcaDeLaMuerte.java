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

    @Override public void activar(String nombreJugador,ComodinCallback callback) {
        final int semilla=new Random().nextInt(6), delay=new Random().nextInt(3000);
        final String uso="marca_"+salaRef.push().getKey();
        descontarInventario(ID_COMODIN,callback,() -> salaRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData sala) {
                if(sala.getValue()==null) return com.google.firebase.database.Transaction.success(sala);
                return com.example.basta_game.utils.SeleccionMinijuego.publicar(sala,rondaActual,miUid,uidVictima,nombreJugador,nombreVictima,semilla,delay,uso)
                        ? com.google.firebase.database.Transaction.success(sala) : com.google.firebase.database.Transaction.abort();
            }
            @Override public void onComplete(com.google.firebase.database.DatabaseError e,boolean ok,com.google.firebase.database.DataSnapshot sala) {
                if(e==null && ok) callback.onSuccess("SILENCIO");else callback.onError("No se pudo publicar el duelo: revisa si ya hay otro activo");
            }
        },false));
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
