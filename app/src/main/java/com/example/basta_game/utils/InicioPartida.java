package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import java.util.Map;
import com.google.firebase.database.ServerValue;

public final class InicioPartida {
    private InicioPartida() { }
    public static boolean registrarEntrada(MutableData sala,String partida,int ronda,String uid) {
        if(!VotacionRevision.rondaVigente(sala,partida,ronda) || uid==null || !sala.child("jugadores").hasChild(uid) ||
                !PresenciaSala.conectada(sala,uid) || !"jugando".equals(sala.child("fase").getValue())) return false;
        MutableData entrada=sala.child("entradaPartida/ronda_"+ronda).child(uid);
        MutableData listo=sala.child("jugadores").child(uid).child("estaListo");
        if(Boolean.TRUE.equals(entrada.child("dentro").getValue()) && Boolean.FALSE.equals(listo.getValue())) return false;
        if(!Boolean.TRUE.equals(entrada.child("dentro").getValue())) {
            entrada.child("dentro").setValue(true);entrada.child("timestamp").setValue(ServerValue.TIMESTAMP);
        }
        listo.setValue(false);return true;
    }
    public static boolean marcarListo(MutableData sala,String uid,boolean listo) {
        if(!IngresoSala.enLobby(sala) || uid==null || !sala.child("jugadores").hasChild(uid) ||
                Boolean.TRUE.equals(sala.child("expulsados").child(uid).getValue()) || !PresenciaSala.conectada(sala,uid)) return false;
        sala.child("jugadores").child(uid).child("estaListo").setValue(listo);return true;
    }
    public static boolean enviarRespuestas(MutableData sala,String partida,int ronda,String uid,Map<String,String> respuestas) {
        MutableData destino=sala.child("respuestas/ronda_"+ronda).child(uid);
        if(!VotacionRevision.rondaVigente(sala,partida,ronda) || !PresenciaSala.conectada(sala,uid) ||
                !"jugando".equals(sala.child("fase").getValue()) || sala.child(RelojRevision.ruta(ronda)+"/reloj").getValue()!=null || destino.getValue()!=null) return false;
        destino.setValue(respuestas);return true;
    }

    public static boolean publicarResumen(MutableData sala, String uidHost, String idPartida, int rondaCerrada) {
        Object ronda = sala.child("rondaActual").getValue();
        Object maximo = sala.child("cantidadRondas").getValue();
        if (idPartida == null || !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                uidHost == null || !uidHost.equals(sala.child("hostUID").getValue(String.class)) ||
                !(ronda instanceof Number) || ((Number) ronda).intValue() != rondaCerrada ||
                !(maximo instanceof Number) || sala.child("jugadores").getChildrenCount() <= 1 ||
                "resultados_finales".equals(sala.child("fase").getValue(String.class))) return false;
        sala.child("resumenRondaListo/ronda_" + rondaCerrada).setValue(true);
        sala.child("fase").setValue(rondaCerrada >= ((Number) maximo).intValue()
                ? "resultados_finales" : "revision_puntuaciones");
        return true;
    }

    public static boolean aplicar(MutableData sala, String idPartida, String uidHost,
                                  Map<String, Object> cambios) {
        if (!RecompensasPartida.idValido(idPartida) || uidHost == null || sala.getValue() == null) return false;
        String estado = sala.child("estadoPartida").getValue(String.class);
        if (!"En espera".equals(estado) && !"Esperando jugadores".equals(estado)) return false;
        if (!uidHost.equals(sala.child("hostUID").getValue(String.class))) return false;
        if (sala.child("jugadores").getChildrenCount()<2) return false;
        for (MutableData jugador : sala.child("jugadores").getChildren()) {
            if (Boolean.TRUE.equals(sala.child("expulsados").child(jugador.getKey()).getValue()) || !Boolean.TRUE.equals(jugador.child("estaListo").getValue(Boolean.class)) ||
                    !Boolean.TRUE.equals(jugador.child("estaConectado").getValue()) || !PresenciaSala.conectada(sala,jugador.getKey())) return false;
        }
        for (Map.Entry<String, Object> cambio : cambios.entrySet()) {
            sala.child(cambio.getKey()).setValue(cambio.getValue());
        }
        sala.child("idPartida").setValue(idPartida);
        sala.child("preparacionInicialMs").setValue(10000);
        if(sala.child("letrasPartida").hasChildren()) sala.child("letraActual").setValue(sala.child("letrasPartida/0").getValue());
        fijarInicio(sala,1);
        for (MutableData jugador : sala.child("jugadores").getChildren()) {
            jugador.child("puntajePartida").setValue(0);
            jugador.child("estaListo").setValue(false);
        }
        return true;
    }

    public static boolean avanzar(MutableData sala, String uidHost, String idPartida, int rondaCerrada,
                                  Map<String, Object> cambios) {
        if (sala.getValue() == null || uidHost == null ||
                idPartida == null || !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                !uidHost.equals(sala.child("hostUID").getValue(String.class)) ||
                !"revision_puntuaciones".equals(sala.child("fase").getValue(String.class))) return false;
        Object ronda = sala.child("rondaActual").getValue();
        if (!(ronda instanceof Number) || ((Number) ronda).intValue() != rondaCerrada) return false;
        for (MutableData jugador : sala.child("jugadores").getChildren()) {
            if(Boolean.FALSE.equals(jugador.child("estaConectado").getValue())) continue;
            if (!Boolean.TRUE.equals(sala.child("revisionLista/ronda_" + rondaCerrada)
                    .child(jugador.getKey()).getValue(Boolean.class))) return false;
        }
        for (Map.Entry<String, Object> cambio : cambios.entrySet()) sala.child(cambio.getKey()).setValue(cambio.getValue());
        if(sala.child("letrasPartida").hasChildren()) sala.child("letraActual").setValue(sala.child("letrasPartida/"+rondaCerrada).getValue());
        fijarInicio(sala,rondaCerrada+1);
        return true;
    }
    private static void fijarInicio(MutableData sala,int ronda) {
        sala.child("inicioRonda").setValue(ServerValue.TIMESTAMP);
        sala.child("inicioRondaDe").setValue(ronda);
        sala.child("inicioRondaListoDe").setValue(ronda);
    }
    public static boolean confirmarInicio(MutableData sala,String partida,int ronda,String uid) {
        if(!VotacionRevision.rondaVigente(sala,partida,ronda) || uid==null || !sala.child("jugadores").hasChild(uid) ||
                !"jugando".equals(sala.child("fase").getValue()) ||
                numero(sala.child("inicioRondaListoDe").getValue())==ronda) return false;
        if(sala.child("inicioRonda").getValue()==null || numero(sala.child("inicioRondaDe").getValue())!=ronda) fijarInicio(sala,ronda);
        else sala.child("inicioRondaListoDe").setValue(ronda);
        return true;
    }
    private static int numero(Object v) { return v instanceof Number ? ((Number)v).intValue() : -1; }
    public static boolean expulsarEnEspera(MutableData sala,String host,String objetivo) {
        String estado=sala.child("estadoPartida").getValue(String.class);
        if(!IngresoSala.enLobby(sala) || host==null || !host.equals(sala.child("hostUID").getValue()) || objetivo==null || host.equals(objetivo) ||
                !("En espera".equals(estado) || "Esperando jugadores".equals(estado)) || !sala.child("jugadores").hasChild(objetivo)) return false;
        sala.child("expulsados").child(objetivo).setValue(true);
        return TransferenciaHost.abandonar(sala,objetivo);
    }
}
