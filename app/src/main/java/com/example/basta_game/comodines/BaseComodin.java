package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public abstract class BaseComodin {

    protected String idSala;
    protected int rondaActual;
    protected String miUid;
    protected DatabaseReference salaRef;
    protected DatabaseReference usuarioRef;

    public interface ComodinCallback {
        void onSuccess(String mensajeNotificacion);
        void onError(String mensajeError);
    }

    public BaseComodin(String idSala, int rondaActual) {
        this.idSala = idSala;
        this.rondaActual = rondaActual;
        this.miUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.salaRef = FirebaseDatabase.getInstance().getReference("salas").child(idSala);
        this.usuarioRef = FirebaseDatabase.getInstance().getReference("usuarios").child(miUid);
    }

    public abstract void activar(String nombreJugador, ComodinCallback callback);

    // SOLUCIÓN BUG 1: Usamos SingleValueEvent para evitar que la caché vacía aborte la operación.
    protected void descontarInventario(String idComodin, ComodinCallback callback, Runnable onDescontadoExito) {
        if(!com.example.basta_game.utils.ConexionJuego.disponible()){callback.onError("Necesitas conexion para usar comodines");return;}
        final long conexion=com.example.basta_game.utils.ConexionJuego.version();
        salaRef.get().addOnSuccessListener(sala -> {
            if(!com.example.basta_game.utils.ConexionJuego.vigente(conexion)){callback.onError("La conexion cambio; vuelve a intentarlo");return;}
            Object actual = sala.child("rondaActual").getValue();
            Object maximo = sala.child("cantidadRondas").getValue();
            if (!sala.child("jugadores").hasChild(miUid) || !(actual instanceof Number) ||
                    ((Number) actual).intValue() != rondaActual || !(maximo instanceof Number) ||
                    (!com.example.basta_game.utils.FaseComodin.deResultados(idComodin) &&
                            sala.child("respuestas/ronda_" + rondaActual).hasChild(miUid)) ||
                    !com.example.basta_game.utils.FaseComodin.disponible(idComodin,
                            sala.child("fase").getValue(String.class), rondaActual, ((Number) maximo).intValue())) {
                callback.onError("Este comodin no esta disponible en la fase actual");
                return;
            }
            descontarInventarioVerificado(idComodin, callback, onDescontadoExito);
        }).addOnFailureListener(error -> callback.onError("No se pudo verificar la fase de la partida"));
    }

    private void descontarInventarioVerificado(String idComodin, ComodinCallback callback, Runnable onDescontadoExito) {
        final long conexion=com.example.basta_game.utils.ConexionJuego.version();
        if(com.example.basta_game.utils.AliasCambioFlujo.es(idComodin)) {
            usuarioRef.child("inventarioComodines").runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @Override public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData datos) {
                    if(!com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return com.google.firebase.database.Transaction.abort();
                    if(datos.getValue()==null) return com.google.firebase.database.Transaction.success(datos);
                    return com.example.basta_game.utils.AliasCambioFlujo.descontar(datos) ? com.google.firebase.database.Transaction.success(datos) : com.google.firebase.database.Transaction.abort();
                }
                @Override public void onComplete(DatabaseError error,boolean committed,DataSnapshot datos) {
                    if(error!=null) callback.onError("Error al descontar el comodín.");
                    else if(committed && datos!=null && datos.exists()) onDescontadoExito.run();
                    else callback.onError("No tienes este comodín disponible en tu inventario.");
                }
            },false);
            return;
        }
        DatabaseReference cantidadRef = usuarioRef.child("inventarioComodines")
                .child(idComodin).child("cantidad");
        com.example.basta_game.utils.ConsumoInventario.descontar(cantidadRef,
                (descontado, mensaje) -> {
                    if (descontado) onDescontadoExito.run();
                    else callback.onError(mensaje);
                });
    }
}
