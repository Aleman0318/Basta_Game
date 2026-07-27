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
        DatabaseReference cantidadRef = usuarioRef.child("inventarioComodines").child(idComodin).child("cantidad");

        cantidadRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer cantidadActual = snapshot.getValue(Integer.class);

                if (cantidadActual != null && cantidadActual > 0) {
                    cantidadRef.setValue(cantidadActual - 1).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            onDescontadoExito.run();
                        } else {
                            callback.onError("Error al descontar el comodín.");
                        }
                    });
                } else {
                    callback.onError("No tienes este comodín disponible en tu inventario.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al conectar con el servidor.");
            }
        });
    }
}