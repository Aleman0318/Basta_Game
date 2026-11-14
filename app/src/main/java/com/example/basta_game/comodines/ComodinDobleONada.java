package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;

public class ComodinDobleONada extends BaseComodin {

    public static final String ID_COMODIN = "doble_o_nada";

    public ComodinDobleONada(String idSala, int rondaActual) {
        super(idSala, rondaActual);
    }

    @Override
    public void activar(String nombreJugador, ComodinCallback callback) {
        // 1. Verificamos si ya está activo
        salaRef.child("comodinesPendientes")
                .child("ronda_" + rondaActual)
                .child(miUid)
                .child(ID_COMODIN)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                            callback.onError("Ya tienes Doble o Nada activo en esta ronda");
                            return;
                        }

                        // 2. Descontamos del inventario
                        descontarInventario(ID_COMODIN, callback, () -> {
                            // 3. Activamos en la sala
                            aplicarEnSala(nombreJugador, callback);
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Error al verificar servidor");
                    }
                });
    }

    private void aplicarEnSala(String nombreJugador, ComodinCallback callback) {
        String idUso = "doble_" + rondaActual + "_" + miUid + "_" + System.currentTimeMillis();
        HashMap<String, Object> actualizaciones = new HashMap<>();

        actualizaciones.put("comodinesPendientes/ronda_" + rondaActual + "/" + miUid + "/" + ID_COMODIN, true);
        actualizaciones.put("comodinesUsados/ronda_" + rondaActual + "/" + idUso + "/tipo", ID_COMODIN);
        actualizaciones.put("comodinesUsados/ronda_" + rondaActual + "/" + idUso + "/uid", miUid);
        actualizaciones.put("comodinesUsados/ronda_" + rondaActual + "/" + idUso + "/nombreJugador", nombreJugador);
        actualizaciones.put("comodinesUsados/ronda_" + rondaActual + "/" + idUso + "/timestamp", ServerValue.TIMESTAMP);

        salaRef.updateChildren(actualizaciones).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess("Doble o Nada activo: necesitas 80% de aciertos o perderás 15 puntos");
            } else {
                callback.onError("No se pudo activar Doble o Nada en la sala");
            }
        });
    }

    // BONUS: Podemos mudar aquí la matemática final para que el Fragment no haga los cálculos
    public static int calcularPuntajeFinal(int puntosBase, int categoriasEvaluadas) {
        int puntosMaximos = categoriasEvaluadas * 10;
        int minimoNecesario = (int) Math.ceil(puntosMaximos * 0.80);

        if (puntosBase >= minimoNecesario) {
            return puntosBase * 2;
        } else {
            return puntosBase - 15;
        }
    }
}