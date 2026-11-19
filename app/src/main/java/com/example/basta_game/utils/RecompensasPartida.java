package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import java.util.Map;

public final class RecompensasPartida {
    private RecompensasPartida() { }

    public static boolean aplicar(MutableData usuario, String idPartida, Map<String, Object> historial) {
        if (!idValido(idPartida) || historial == null || !idPartida.equals(historial.get("idPartida"))) {
            throw new IllegalArgumentException("Identificador de partida invalido");
        }
        if (usuario.getValue() == null) return false;
        if (usuario.child("recompensasAplicadas").child(idPartida).getValue() != null) return false;
        if (!(historial.get("miUid") instanceof String) ||
                !historial.get("miUid").equals(usuario.child("uid").getValue())) {
            throw new IllegalArgumentException("El historial no corresponde al usuario");
        }
        if (numero(historial.get("monedas")) < 0 || numero(historial.get("miPosicion")) < 1) {
            throw new IllegalArgumentException("Recompensa invalida");
        }
        long monedas = Math.addExact(numero(usuario.child("monedas").getValue()), numero(historial.get("monedas")));
        long jugadas = Math.addExact(numero(usuario.child("partidasJugadas").getValue()), 1);
        long puntos = Math.addExact(numero(usuario.child("puntosTotalesHistoricos").getValue()), numero(historial.get("miTotal")));
        long ganadas = Math.addExact(numero(usuario.child("partidasGanadas").getValue()),
                numero(historial.get("miPosicion")) == 1 ? 1 : 0);
        usuario.child("historialPartidas").child(idPartida).setValue(historial);
        usuario.child("monedas").setValue(monedas);
        usuario.child("partidasJugadas").setValue(jugadas);
        usuario.child("puntosTotalesHistoricos").setValue(puntos);
        usuario.child("partidasGanadas").setValue(ganadas);
        // La marca sobrevive al recorte del historial para impedir pagos repetidos.
        usuario.child("recompensasAplicadas").child(idPartida).setValue(true);
        return true;
    }

    public static boolean idValido(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]+");
    }

    private static long numero(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }
}
