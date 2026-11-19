package com.example.basta_game.utils;

import com.google.firebase.database.*;

/** One room transaction settles the captured duel, never a later arena. */
public final class ResultadoMarca {
    private ResultadoMarca() { }
    public static boolean aplicar(MutableData sala,String partida,int ronda,String uso,String ejecutor) {
        MutableData juego=sala.child("minijuegoActual");
        if(partida==null || uso==null || !partida.equals(sala.child("idPartida").getValue()) ||
                !partida.equals(juego.child("idPartida").getValue()) || !uso.equals(juego.child("idUso").getValue()) ||
                RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=ronda ||
                RelojRevision.numero(juego.child("ronda").getValue(),-1)!=ronda ||
                !"revision_puntuaciones".equals(sala.child("fase").getValue()) ||
                !sala.child("jugadores").hasChild(ejecutor) || !"terminado".equals(juego.child("estado").getValue())) return false;
        String a=juego.child("uidAtacante").getValue(String.class),v=juego.child("uidVictima").getValue(String.class);
        String ganador=juego.child("ganadorUid").getValue(String.class);
        if(a==null || v==null || a.equals(v) || (!a.equals(ganador) && !v.equals(ganador))) return false;
        MutableData registro=sala.child("comodinesResultados/ronda_"+ronda).child(uso);
        if(registro.getValue()!=null) return false;
        MutableData puntos=sala.child("puntajesRondas/ronda_"+ronda);
        long de=RelojRevision.numero(puntos.child(v).getValue(),0),para=RelojRevision.numero(puntos.child(a).getValue(),0);
        // Doble o Nada puede dejar totales negativos. No se roban puntos de una deuda,
        // pero el duelo DEBE poder liquidarse para liberar el paso a la siguiente ronda.
        long cantidad=a.equals(ganador) ? Math.max(0,de)/2 : 0;
        if(para>Integer.MAX_VALUE-cantidad) return false;
        if(a.equals(ganador)) {puntos.child(v).setValue(de-cantidad);puntos.child(a).setValue(para+cantidad);}
        String nombreA=juego.child("nombreAtacante").getValue(String.class),nombreV=juego.child("nombreVictima").getValue(String.class);
        if(nombreA==null) nombreA="El atacante";if(nombreV==null) nombreV="La víctima";
        String mensaje=a.equals(ganador) ? nombreA+" ganó el duelo y robó "+cantidad+" puntos a "+nombreV+" (50 %, redondeado hacia abajo)."
                : nombreV+" ganó el duelo y defendió sus puntos. "+nombreA+" no consiguió robarlos.";
        registro.child("tipo").setValue("marca_de_la_muerte");registro.child("atacante").setValue(a);registro.child("victima").setValue(v);
        registro.child("ganadorUid").setValue(ganador);registro.child("cantidad").setValue(cantidad);registro.child("mensaje").setValue(mensaje);
        registro.child("timestamp").setValue(ServerValue.TIMESTAMP);
        MutableData evento=sala.child("eventoCaosGlobal/ronda_"+ronda);
        evento.child("idUso").setValue(uso);evento.child("titulo").setValue(a.equals(ganador)?"¡ATRACO A MEDIAS!":"¡DEFENSA LEGENDARIA!");
        evento.child("mensaje").setValue(mensaje);evento.child("esExito").setValue(a.equals(ganador));
        evento.child("uidAtacante").setValue(a);evento.child("uidVictima").setValue(v);evento.child("timestamp").setValue(ServerValue.TIMESTAMP);
        juego.setValue(null);return true;
    }
}
