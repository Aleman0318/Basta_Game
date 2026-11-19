package com.example.basta_game.utils;

public final class PlazoRonda {
    private PlazoRonda() { }
    public static long inicioRespuestas(long inicio,int ronda,long preparacionMs) {
        return inicio+(ronda==1 ? Math.max(0,preparacionMs) : 0);
    }
    public static boolean lecturaAplicable(Long anterior,Long actual) {
        return anterior==null || (actual!=null && actual>=anterior);
    }
    public static boolean bastaDisponible(long inicioRespuestas,long fin,long ahora) {
        return inicioRespuestas>0 && ahora-inicioRespuestas>=30000 && fin-ahora>10000;
    }
    public static boolean aceptarBasta(com.google.firebase.database.MutableData sala,String partida,int ronda,String uid,long ahora) {
        if(!VotacionRevision.rondaVigente(sala,partida,ronda) || uid==null || !sala.child("jugadores").hasChild(uid) || !PresenciaSala.conectada(sala,uid) ||
                !"jugando".equals(sala.child("fase").getValue()) || Boolean.TRUE.equals(sala.child("bastaPresionado").getValue())) return false;
        Object marca=sala.child("inicioRonda").getValue(),duracion=sala.child("tiempoRonda").getValue();
        if(!(marca instanceof Number) || !(duracion instanceof Number)) return false;
        long inicio=inicioRespuestas(((Number)marca).longValue(),ronda,RelojRevision.numero(sala.child("preparacionInicialMs").getValue(),0));
        if(!bastaDisponible(inicio,calcularFin(inicio,((Number)duracion).intValue(),null),ahora)) return false;
        sala.child("bastaPresionado").setValue(true);
        sala.child("bastaPresionadoPor").setValue(uid);
        sala.child("bastaTimeStamp").setValue(com.google.firebase.database.ServerValue.TIMESTAMP);
        return true;
    }

    public static long calcularFin(long inicioRonda, int segundos, Long bastaTimestamp) {
        long fin = inicioRonda + segundos * 1000L;
        return bastaTimestamp == null ? fin : bastaTimestamp + 10000L;
    }

    public static long restante(long fin, long horaLocal, long offsetServidor) {
        return Math.max(0L, fin - (horaLocal + offsetServidor));
    }
}
