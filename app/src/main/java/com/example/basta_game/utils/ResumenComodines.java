package com.example.basta_game.utils;

import com.example.basta_game.R;
import com.example.basta_game.models.EventoComodin;
import java.util.*;

public final class ResumenComodines {
    private ResumenComodines() { }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> mapa(Object valor) {
        return valor instanceof Map ? (Map<String,Object>) valor : Collections.emptyMap();
    }

    private static String texto(Object valor, String defecto) {
        return valor instanceof String && !((String) valor).trim().isEmpty() ? (String) valor : defecto;
    }

    public static List<EventoComodin> crear(Map<String,Object> sala, int ronda, boolean finalPartida) {
        List<EventoComodin> lista = new ArrayList<>();
        for (int numero = finalPartida ? 1 : ronda; numero <= ronda; numero++) {
            String clave = "ronda_" + numero;
            Map<String,Object> resultados = mapa(mapa(sala.get("comodinesResultados")).get(clave));
            Map<String,Object> usos = mapa(mapa(sala.get("comodinesUsados")).get(clave));
            Map<String,Object> congelador = mapa(mapa(mapa(sala.get("comodinesActivos")).get(clave)).get("congelador"));
            String prefijo = "Ronda " + numero + " · ";
            for (String id : new TreeSet<>(resultados.keySet())) {
                Map<String,Object> resultado = mapa(resultados.get(id));
                if (resultado.containsKey("doble_o_nada")) {
                    Map<String,Object> doble = mapa(resultado.get("doble_o_nada"));
                    String nombre = texto(mapa(mapa(sala.get("jugadores")).get(id)).get("nombre"), "Un jugador");
                    String estado = texto(doble.get("resultado"), "");
                    if ("duplicado".equals(estado) || "castigo".equals(estado)) {
                        lista.add(new EventoComodin(prefijo + nombre + " · Doble o Nada",
                                doble.get("puntosBase") instanceof Number && doble.get("puntosFinales") instanceof Number
                                        ? "Puntos: " + doble.get("puntosBase") + " → " + doble.get("puntosFinales")
                                        : "duplicado".equals(estado) ? "Duplicó los puntos de la ronda" : "Castigo registrado; importe aplicado no disponible",
                                R.drawable.ic_comodin_dobleonada, "duplicado".equals(estado)));
                    } else lista.add(new EventoComodin(prefijo + nombre + " · Doble o Nada",
                            "Uso registrado; resultado no disponible", R.drawable.ic_comodin_dobleonada));
                } else if (id.startsWith("flujo_")) {
                    boolean castigo = "castigo".equals(resultado.get("resultado"));
                    lista.add(new EventoComodin(prefijo + "Cambio de Flujo", castigo
                            ? "Castigo registrado; importe aplicado no disponible" : "Intercambio de puntuación registrado",
                            R.drawable.ic_comodin_cambiodeflujo, !castigo));
                } else if ("ruleta_caotica".equals(resultado.get("tipo"))) {
                    lista.add(new EventoComodin(prefijo + texto(resultado.get("nombreAtacante"), "Un jugador") + " · Ruleta Caótica",
                            "Efecto registrado: " + texto(resultado.get("efecto"), "no disponible"), R.drawable.ic_comodin_ruletacaotica));
                }
            }
            for (String id : new TreeSet<>(usos.keySet())) {
                Map<String,Object> uso = mapa(usos.get(id));
                String tipo = texto(uso.get("tipo"), "Comodín");
                String uid = texto(uso.get("uid"), "");
                if (resultados.containsKey(id) || ("doble_o_nada".equals(tipo) &&
                        mapa(resultados.get(uid)).containsKey("doble_o_nada"))) continue;
                String nombre = texto(uso.get("nombreJugador"), "Un jugador");
                String descripcion = "Uso registrado; resultado no disponible";
                int icono = R.drawable.ic_comodines;
                String titulo = tipo;
                if ("congelador".equals(tipo)) {
                    titulo = "Congelador"; icono = R.drawable.ic_comodin_congelar;
                    Object duracion = uso.get("duracionMs");
                    if (!(duracion instanceof Number) && id.equals(congelador.get("idUso"))) duracion = congelador.get("duracionMs");
                    descripcion = "Bloqueó el botón BASTA" + (duracion instanceof Number
                            ? "; duración registrada: " + ((Number) duracion).longValue() / 1000.0 + " s"
                            : "; duración no registrada para este uso");
                } else if ("doble_o_nada".equals(tipo)) {
                    titulo = "Doble o Nada"; icono = R.drawable.ic_comodin_dobleonada;
                } else if ("marca_de_la_muerte".equals(tipo)) {
                    titulo = "Marca de la Muerte"; icono = R.drawable.ic_comodin_marcadelamuerte;
                    descripcion = "Inició un duelo; el resultado histórico no está registrado en este uso";
                }
                lista.add(new EventoComodin(prefijo + nombre + " · " + titulo, descripcion, icono));
            }
        }
        return lista;
    }
}
