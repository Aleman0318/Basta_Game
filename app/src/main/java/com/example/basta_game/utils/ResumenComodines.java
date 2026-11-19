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

    private static String relatoDoble(String nombre,String id,String estado,Map<String,Object> datos) {
        String frase="duplicado".equals(estado)
                ? (Math.floorMod(id.hashCode(),2)==0 ? " aposto y la fortuna sonrio. " : " arriesgo sus puntos y duplico la apuesta. ")
                : (Math.floorMod(id.hashCode(),2)==0 ? " aposto todo, pero la suerte se fue. " : " arriesgo sus puntos y la apuesta fallo. ");
        Object base=datos.get("puntosBase"), fin=datos.get("puntosFinales");
        if(base instanceof Number && fin instanceof Number) {
            long cambio=((Number)fin).longValue()-((Number)base).longValue();
            return nombre+frase+(cambio<0 ? "Perdió "+(-cambio)+" puntos." : cambio>0 ? "Ganó "+cambio+" puntos." : "Sus puntos no cambiaron.")+" ("+base+" -> "+fin+")";
        }
        return nombre+frase+("duplicado".equals(estado) ? "Duplico los puntos de la ronda." : "Castigo registrado; importe no disponible.");
    }

    public static List<EventoComodin> crear(Map<String,Object> sala, int ronda, boolean finalPartida) {
        List<EventoComodin> lista = new ArrayList<>();
        for (int numero = finalPartida ? 1 : ronda; numero <= ronda; numero++) {
            String clave = "ronda_" + numero;
            Map<String,Object> resultados = mapa(mapa(sala.get("comodinesResultados")).get(clave));
            Map<String,Object> usos = mapa(mapa(sala.get("comodinesUsados")).get(clave));
            Map<String,Object> congelador = mapa(mapa(mapa(sala.get("comodinesActivos")).get(clave)).get("congelador"));
            String prefijo = "Ronda " + numero + " · ";
            // Decreto del Silencio: anuncio y multas realmente aplicadas al cerrar la ronda.
            Map<String,Object> decreto=mapa(mapa(sala.get("decretoSilencio")).get(clave));
            if(Objects.equals(sala.get("idPartida"),decreto.get("idPartida")) && decreto.get("idUso")!=null) {
                String vocal=texto(decreto.get("vocal"),"?");
                StringBuilder categorias=new StringBuilder();
                Object campos=decreto.get("categorias");
                if(campos instanceof List) for(Object campo:(List<?>)campos) {
                    if(categorias.length()>0) categorias.append(" y ");
                    categorias.append(campo);
                }
                StringBuilder multas=new StringBuilder();
                for(Map.Entry<String,Object> jugador:mapa(decreto.get("resultados")).entrySet()) {
                    Map<String,Object> info=mapa(jugador.getValue());
                    Object aplicado=info.get("multaAplicada");
                    if(!(aplicado instanceof Number) || ((Number)aplicado).intValue()<=0) continue;
                    String nombre=texto(mapa(mapa(sala.get("jugadores")).get(jugador.getKey())).get("nombre"),"Un jugador");
                    if(multas.length()>0) multas.append("; ");
                    multas.append(nombre).append(" -").append(((Number)aplicado).intValue()).append(" pts");
                }
                lista.add(new EventoComodin(prefijo+"Decreto del Silencio",
                        "La vocal "+vocal+" quedó prohibida en "+categorias+". Las palabras infractoras no recibieron puntos. "+
                                (multas.length()>0 ? "Multas aplicadas: "+multas+"." : "No hubo multas aplicadas."),
                        R.drawable.ic_comodin_decreto_silencio));
            }
            // Viento del Caos se registra en efectosVisuales, no en comodinesUsados.
            // Solo incluir eventos que pertenecen a esta partida, no a una anterior.
            Map<String,Object> viento = mapa(mapa(mapa(sala.get("efectosVisuales"))
                    .get("vientoCaos")).get(clave));
            if (viento.containsKey("idUso") && Objects.equals(sala.get("idPartida"), viento.get("idPartida"))) {
                String invocador = texto(viento.get("nombreJugador"), "Un jugador");
                lista.add(new EventoComodin(prefijo + invocador + " · Viento del Caos",
                        invocador + " desató un torbellino: todas las categorías cambiaron "
                                + "de posición, pero las respuestas se mantuvieron en su categoría correcta.",
                        R.drawable.ic_comodin_viento));
            }
            // El Espejo se guarda por victima en efectosVisuales/espejo/ronda_N.
            Map<String,Object> espejos = mapa(mapa(mapa(sala.get("efectosVisuales"))
                    .get("espejo")).get(clave));
            for (Object valor : espejos.values()) {
                Map<String,Object> evento = mapa(valor);
                if (!evento.containsKey("idUso") || !Objects.equals(
                        sala.get("idPartida"), evento.get("idPartida"))) continue;
                String atacante = texto(evento.get("atacanteNombre"),"Un jugador");
                String victima = texto(evento.get("victimaNombre"),"un rival");
                lista.add(new EventoComodin(prefijo + atacante + " · El Espejo",
                        atacante + " atacó a " + victima +
                                ": hasta 3 categorías elegidas al azar se invirtieron durante 30 segundos. " +
                                "Las respuestas invertidas quedan así hasta que el rival las corrija.", R.drawable.ic_comodin_espejo));
            }
            Map<String,Object> poderes=mapa(mapa(sala.get("poderesRonda")).get(clave));
            if(Objects.equals(sala.get("idPartida"),poderes.get("idPartida")) && poderes.get("idPartida")!=null) {
                for(Map.Entry<String,Object> registroEscudo:mapa(poderes.get("escudos")).entrySet()) {
                    Map<String,Object> escudo=mapa(registroEscudo.getValue());
                    if(!escudo.containsKey("idUso")) continue;
                    String nombre=texto(escudo.get("nombreJugador"),"Un jugador");
                    List<String> ataquesDetenidos=new ArrayList<>();
                    Map<String,Object> bloqueos=mapa(poderes.get("bloqueos"));
                    for(Object registro : bloqueos.values()) {
                        Map<String,Object> bloqueo=mapa(registro);
                        if(!Objects.equals(escudo.get("idPartida"),poderes.get("idPartida"))) continue;
                        if(!Objects.equals(registroEscudo.getKey(), texto(bloqueo.get("uidVictima"),""))) continue;
                        String tipo=texto(bloqueo.get("tipo"),"");
                        String ataque="inversion_letras".equals(tipo)?"El Espejo":
                                "marca_de_la_muerte".equals(tipo)?"Marca de la Muerte":
                                        "uno_reversa".equals(tipo)?"Cambio de Flujo":"un ataque";
                        ataquesDetenidos.add(ataque+" de "+texto(bloqueo.get("nombreAtacante"),"un rival"));
                    }
                    int usosAntiguos=0;
                    for(Object intento:mapa(escudo.get("intentos")).values())
                        if(Boolean.TRUE.equals(intento)) usosAntiguos++;
                    String descripcion;
                    if(!ataquesDetenidos.isEmpty()) {
                        StringBuilder detalles=new StringBuilder();
                        for(String ataque:ataquesDetenidos) {
                            if(detalles.length()>0) detalles.append("; ");
                            detalles.append(ataque);
                        }
                        descripcion="¡PARRY! "+nombre+" bloqueó: "+detalles+". Los atacantes gastaron sus unidades.";
                    }
                    else if(usosAntiguos>0)
                        descripcion=nombre+" bloqueó "+usosAntiguos+" ataques. Ambos lados gastaron sus comodines.";
                    else descripcion=nombre+" equipó su Escudo Dorado en secreto. Nadie intentó atacarlo esta ronda.";
                    lista.add(new EventoComodin(prefijo+nombre+" · Escudo Dorado",descripcion,R.drawable.ic_comodin_escudo_dorado));
                }
            }
            for (String id : new TreeSet<>(resultados.keySet())) {
                Map<String,Object> resultado = mapa(resultados.get(id));
                if (resultado.containsKey("doble_o_nada")) {
                    Map<String,Object> doble = mapa(resultado.get("doble_o_nada"));
                    String nombre = texto(mapa(mapa(sala.get("jugadores")).get(id)).get("nombre"), "Un jugador");
                    String estado = texto(doble.get("resultado"), "");
                    if ("duplicado".equals(estado) || "castigo".equals(estado)) {
                        lista.add(new EventoComodin(prefijo + nombre + " · Doble o Nada",
                                relatoDoble(nombre,id+numero,estado,doble),
                                R.drawable.ic_comodin_dobleonada, "duplicado".equals(estado)));
                    } else lista.add(new EventoComodin(prefijo + nombre + " · Doble o Nada",
                            "Uso registrado; resultado no disponible", R.drawable.ic_comodin_dobleonada));
                } else if (id.startsWith("flujo_")) {
                    boolean castigo = "castigo".equals(resultado.get("resultado"));
                    lista.add(new EventoComodin(prefijo + "Cambio de Flujo", castigo
                            ? "Castigo registrado; importe aplicado no disponible" : "Intercambio de puntuación registrado",
                            R.drawable.ic_comodin_cambiodeflujo, !castigo));
                } else if ("marca_de_la_muerte".equals(resultado.get("tipo"))) {
                    lista.add(new EventoComodin(prefijo + "Marca de la Muerte", texto(resultado.get("mensaje"),"Duelo resuelto"),
                            R.drawable.ic_comodin_marcadelamuerte, texto(resultado.get("atacante"),"").equals(resultado.get("ganadorUid"))));
                } else if ("ruleta_caotica".equals(resultado.get("tipo"))) {
                    lista.add(new EventoComodin(prefijo + texto(resultado.get("nombreAtacante"), "Un jugador") + " · Ruleta Caótica",
                            texto(resultado.get("mensaje"),"Efecto registrado: " + texto(resultado.get("efecto"), "no disponible")), R.drawable.ic_comodin_ruletacaotica));
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
                    descripcion = nombre + (Math.floorMod(id.hashCode(),2)==0 ? " puso la ronda sobre hielo: bloqueo BASTA" : " activo Congelador y freno el boton BASTA") + (duracion instanceof Number
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
