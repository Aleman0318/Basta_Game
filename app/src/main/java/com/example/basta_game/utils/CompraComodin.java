package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;

public final class CompraComodin {
    private CompraComodin() { }
    public static boolean disponible(String id) { return !"chismoso_suerte".equals(id); }

    public static boolean aplicar(MutableData usuario, String id, String nombre, String moneda, int precio) {
        if (!disponible(id) || usuario.getValue() == null || !RecompensasPartida.idValido(id) || nombre == null || precio <= 0 ||
                !("gemas".equals(moneda) || "monedas".equals(moneda))) return false;
        long saldo = entero(usuario.child(moneda).getValue());
        MutableData item = usuario.child("inventarioComodines").child(id);
        long cantidad = item.child("cantidad").getValue() == null ? 0 : entero(item.child("cantidad").getValue());
        if (saldo < precio || saldo > Integer.MAX_VALUE || cantidad < 0 || cantidad >= Integer.MAX_VALUE) return false;
        usuario.child(moneda).setValue(saldo - precio);
        item.child("cantidad").setValue(cantidad + 1);
        item.child("nombre").setValue(nombre);
        item.child("tipoMoneda").setValue(moneda);
        return true;
    }

    private static long entero(Object valor) {
        if (valor instanceof Number) {
            Number n = (Number)valor;
            return n.doubleValue() == n.longValue() ? n.longValue() : -1;
        }
        if (valor instanceof String) try { return Long.parseLong((String)valor); } catch (NumberFormatException ignored) { }
        return -1;
    }
}
