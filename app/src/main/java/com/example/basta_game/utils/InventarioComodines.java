package com.example.basta_game.utils;

import com.example.basta_game.models.ComodinPartida;
import java.util.*;

/** Displays owned entries; availability and consumption remain in their existing guards. */
public final class InventarioComodines {
    private InventarioComodines() { }

    public static List<ComodinPartida> listar(Object datos, List<ComodinPartida> catalogo, int iconoGenerico) {
        List<ComodinPartida> resultado = new ArrayList<>();
        if (!(datos instanceof Map)) return resultado;
        Map<String, ComodinPartida> tipos = new HashMap<>();
        for (ComodinPartida tipo : catalogo) tipos.put(tipo.getId(), tipo);
        if(!tipos.containsKey("uno_reversa") && tipos.containsKey("cambio_flujo")) tipos.put("uno_reversa",tipos.get("cambio_flujo"));
        long cantidadCambio=AliasCambioFlujo.total((Map<?,?>)datos);
        boolean agruparCambio=cantidadCambio<=Integer.MAX_VALUE;
        Set<String> orden = new LinkedHashSet<>();
        for (ComodinPartida tipo : catalogo) orden.add(tipo.getId());
        orden.add("uno_reversa");
        for (Object id : ((Map<?, ?>) datos).keySet()) if (id instanceof String) orden.add((String) id);
        for (String id : orden) {
            if(agruparCambio && "cambio_flujo".equals(id)) continue;
            Object valor = ((Map<?, ?>) datos).get(id);
            if(agruparCambio && "uno_reversa".equals(id) && !(valor instanceof Map)) valor=((Map<?,?>)datos).get("cambio_flujo");
            if (!(valor instanceof Map)) continue;
            Map<?, ?> item = (Map<?, ?>) valor;
            int cantidad = agruparCambio && "uno_reversa".equals(id) ? (int)cantidadCambio : AliasCambioFlujo.cantidad(item.get("cantidad"));
            if (cantidad <= 0) continue;
            ComodinPartida tipo = tipos.get(id);
            Object nombre = item.get("nombre");
            resultado.add(tipo != null ? new ComodinPartida(id, tipo.getNombre(), tipo.getDescripcion(),
                    tipo.getRareza(), tipo.getIconoResId(), cantidad, tipo.isImplementado(), tipo.isPremium()) :
                    new ComodinPartida(id, nombre instanceof String && !((String) nombre).trim().isEmpty() ? (String) nombre : id,
                            "Este comodín estará disponible pronto.", "Próximamente", iconoGenerico, cantidad, false, false));
        }
        return resultado;
    }

}
