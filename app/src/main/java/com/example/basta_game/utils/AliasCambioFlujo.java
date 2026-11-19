package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import java.util.Map;

/** Both persisted IDs are the same existing power; never migrate or manufacture units. */
public final class AliasCambioFlujo {
    private AliasCambioFlujo() { }
    public static String canonico(String id) { return "cambio_flujo".equals(id) ? "uno_reversa" : id; }
    public static boolean es(String id) { return "uno_reversa".equals(canonico(id)); }
    public static int cantidad(Object valor) {
        if (valor instanceof String) try { valor=Long.parseLong((String)valor); } catch(NumberFormatException e) { return 0; }
        if (!(valor instanceof Number)) return 0;
        Number n=(Number)valor;
        return n.doubleValue()==n.longValue() && n.longValue()>0 && n.longValue()<=Integer.MAX_VALUE ? n.intValue() : 0;
    }
    public static long total(Map<?,?> inventario) {
        long total=0;
        for(String id:new String[]{"uno_reversa","cambio_flujo"}) {
            Object item=inventario.get(id);
            if(item instanceof Map) total+=cantidad(((Map<?,?>)item).get("cantidad"));
        }
        return total;
    }
    public static boolean descontar(MutableData inventario) {
        for(String id:new String[]{"cambio_flujo","uno_reversa"}) {
            MutableData campo=inventario.child(id).child("cantidad");
            int cantidad=cantidad(campo.getValue());
            if(cantidad>0) {campo.setValue(cantidad-1);return true;}
        }
        return false;
    }
}
