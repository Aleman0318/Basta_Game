package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import java.util.Map;
import java.util.Objects;

public final class VotacionRevision {
    private VotacionRevision() { }

    static boolean rondaVigente(MutableData sala, String partida, int ronda) {
        Object actual = sala.child("rondaActual").getValue();
        return partida != null && partida.equals(sala.child("idPartida").getValue(String.class)) &&
                actual instanceof Number && ((Number)actual).intValue() == ronda &&
                !"resultados_finales".equals(sala.child("fase").getValue(String.class)) &&
                !Boolean.TRUE.equals(sala.child("resumenRondaListo/ronda_" + ronda).getValue());
    }

    private static boolean activa(MutableData sala, String partida, int ronda, String id) {
        MutableData voto = sala.child("votacionActiva/ronda_" + ronda);
        return rondaVigente(sala,partida,ronda) && id != null && id.equals(voto.child("idVotacion").getValue()) &&
                Boolean.TRUE.equals(voto.child("activa").getValue()) && !Boolean.TRUE.equals(voto.child("resuelta").getValue());
    }

    public static boolean crear(MutableData sala, String partida, int ronda, String uid, Map<String,Object> datos) {
        return crear(sala,partida,ronda,uid,datos,Long.MIN_VALUE);
    }
    public static boolean crear(MutableData sala, String partida, int ronda, String uid, Map<String,Object> datos,long ahora) {
        MutableData voto = sala.child("votacionActiva/ronda_" + ronda);
        Object clave = datos.get("claveVotacion");
        Object objetivo = datos.get("uidObjetivo");
        Object categoria = datos.get("categoria");
        if (!rondaVigente(sala,partida,ronda) || uid == null || !sala.child("jugadores").hasChild(uid) ||
                !(objetivo instanceof String) || !sala.child("jugadores").hasChild((String)objetivo) ||
                !(datos.get("respuesta") instanceof String) || EstadoRevision.vacia((String)datos.get("respuesta")) ||
                !(categoria instanceof String) || !(clave instanceof String) ||
                Boolean.TRUE.equals(voto.child("activa").getValue()) ||
                Boolean.TRUE.equals(sala.child("resultadosVotacionPalabras/ronda_" + ronda)
                        .child((String)clave).child("resuelta").getValue())) return false;
        Object puntos = sala.child("validaciones/ronda_" + ronda).child((String)objetivo)
                .child((String)categoria).child("puntos").getValue();
        if (puntos instanceof Number && ((Number)puntos).intValue() == 5) return false;
        String letra=sala.child("letraActual").getValue(String.class);
        if(letra!=null && !ValidacionBasica.correcta(letra,(String)datos.get("respuesta"))) return false;
        if(sala.child(RelojRevision.ruta(ronda)+"/reloj").hasChildren() &&
                RelojRevision.numero(datos.get("turnoRevision"),-1)!=RelojRevision.numero(sala.child(RelojRevision.ruta(ronda)+"/reloj/turno").getValue(),-2)) return false;
        if(!RelojRevision.prepararVoto(sala,ronda,(String)datos.get("idVotacion"),ahora,(String)categoria)) return false;
        voto.setValue(datos);
        return true;
    }

    public static boolean registrar(MutableData sala, String partida, int ronda, String id, String uid, String valor) {
        return registrar(sala,partida,ronda,id,uid,valor,Long.MIN_VALUE);
    }
    public static boolean registrar(MutableData sala, String partida, int ronda, String id, String uid, String valor,long ahora) {
        if (!activa(sala,partida,ronda,id) || uid == null || !sala.child("jugadores").hasChild(uid) ||
                !("si".equals(valor) || "no".equals(valor) || "abstenerse".equals(valor))) return false;
        if(sala.child(RelojRevision.ruta(ronda)+"/reloj").hasChildren() &&
                (!RelojRevision.abierto(sala,ronda,ahora,"votos") ||
                        !id.equals(sala.child(RelojRevision.ruta(ronda)+"/reloj/idVotacion").getValue()))) return false;
        MutableData votos = sala.child("votacionActiva/ronda_" + ronda + "/votos");
        if (votos.hasChild(uid)) return false;
        MutableData electores=sala.child(RelojRevision.ruta(ronda)+"/reloj/elegibles");
        if(electores.hasChildren() && !electores.hasChild(uid)) return false;
        votos.child(uid).setValue(valor);
        return true;
    }

    public static boolean resolver(MutableData sala, String partida, int ronda, String id, String host,
                                   Object votosEsperados, Map<String,Object> cambios) {
        return resolver(sala,partida,ronda,id,host,votosEsperados,cambios,Long.MIN_VALUE);
    }
    public static boolean resolver(MutableData sala, String partida, int ronda, String id, String host,
                                   Object votosEsperados, Map<String,Object> cambios,long ahora) {
        boolean compartida=sala.child(RelojRevision.ruta(ronda)+"/reloj").hasChildren();
        if (!activa(sala,partida,ronda,id) || host == null || (!compartida && !host.equals(sala.child("hostUID").getValue())) ||
                !sala.child("jugadores").hasChild(host) ||
                (compartida && Boolean.FALSE.equals(sala.child("jugadores").child(host).child("estaConectado").getValue())) ||
                !Objects.equals(votosEsperados,sala.child("votacionActiva/ronda_" + ronda + "/votos").getValue())) return false;
        if(!RelojRevision.puedeCerrarVoto(sala,ronda,id,ahora)) return false;
        MutableData voto=sala.child("votacionActiva/ronda_"+ronda);
        String objetivo=voto.child("uidObjetivo").getValue(String.class), categoria=voto.child("categoria").getValue(String.class), clave=voto.child("claveVotacion").getValue(String.class);
        Object original=objetivo==null || categoria==null ? null : sala.child("validaciones/ronda_"+ronda).child(objetivo).child(categoria).getValue();
        String palabraOriginal=voto.child("respuesta").getValue(String.class);
        long si=0,no=0;
        boolean conVotos=voto.child("votos").hasChildren();
        Object elegibles=sala.child(RelojRevision.ruta(ronda)+"/reloj/elegibles").getValue();
        Object votosOriginales=voto.child("votos").getValue();
        for(MutableData v:voto.child("votos").getChildren()) {if("si".equals(v.getValue())) si++;if("no".equals(v.getValue())) no++;}
        for (Map.Entry<String,Object> cambio : cambios.entrySet()) sala.child(cambio.getKey()).setValue(cambio.getValue());
        if(compartida && si==no && original instanceof Map && clave!=null) {
            sala.child("validaciones/ronda_"+ronda).child(objetivo).child(categoria).setValue(original);
            MutableData resultado=sala.child("resultadosVotacionPalabras/ronda_"+ronda).child(clave);
            resultado.child("puntosFinales").setValue(((Map<?,?>)original).get("puntos"));
            resultado.child("correctaFinal").setValue(((Map<?,?>)original).get("correcta"));
        }
        if(clave!=null) sala.child("resultadosVotacionPalabras/ronda_"+ronda).child(clave).child("resuelta").setValue(true);
        if(clave!=null && elegibles instanceof Map) {
            int[] conteo=ConteoVotos.contar(elegibles,votosOriginales);
            MutableData resultado=sala.child("resultadosVotacionPalabras/ronda_"+ronda).child(clave);
            resultado.child("validos").setValue(conteo[0]);resultado.child("invalidos").setValue(conteo[1]);resultado.child("abstenciones").setValue(conteo[2]);
        }
        String letra=sala.child("letraActual").getValue(String.class);
        if(clave!=null && objetivo!=null && categoria!=null && letra!=null && !ValidacionBasica.correcta(letra,palabraOriginal)) {
            MutableData valor=sala.child("validaciones/ronda_"+ronda).child(objetivo).child(categoria);
            valor.child("puntos").setValue(0);valor.child("correcta").setValue(false);
            MutableData resultado=sala.child("resultadosVotacionPalabras/ronda_"+ronda).child(clave);
            resultado.child("puntosFinales").setValue(0);resultado.child("correctaFinal").setValue(false);
        }
        // El voto decide si la palabra es válida, pero NO puede perdonar una
        // infracción de Decreto: siempre 0 y la multa se cobra al cerrar ronda.
        if(objetivo!=null && categoria!=null && DecretoSilencio.categoriaAfectada(sala,partida,ronda,categoria)) {
            String vocal=sala.child("decretoSilencio/ronda_"+ronda+"/vocal").getValue(String.class);
            boolean advertido=Boolean.TRUE.equals(sala.child("decretoSilencio/ronda_"+ronda+
                    "/recibido/"+objetivo).getValue());
            if(advertido && DecretoSilencio.infringe(palabraOriginal,vocal)) {
                MutableData invalidacion=sala.child("validaciones/ronda_"+ronda).child(objetivo).child(categoria);
                invalidacion.child("puntos").setValue(0L);
                invalidacion.child("correcta").setValue(false);
                invalidacion.child("invalidadaPorDecreto").setValue(true);
                if(clave!=null) {
                    MutableData resultado=sala.child("resultadosVotacionPalabras/ronda_"+ronda).child(clave);
                    resultado.child("puntosFinales").setValue(0L);
                    resultado.child("correctaFinal").setValue(false);
                }
            }
        }
        RelojRevision.siguientePropuesta(sala,ronda,id,conVotos);
        return true;
    }
}
