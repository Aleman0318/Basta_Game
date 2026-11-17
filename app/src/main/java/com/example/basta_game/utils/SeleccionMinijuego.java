package com.example.basta_game.utils;
import com.google.firebase.database.*;
import java.util.*;

public final class SeleccionMinijuego {
    private SeleccionMinijuego() { }
    public static boolean elegirPPT(MutableData juego,String uid,String eleccion) {
        if(!"jugando".equals(juego.child("estado").getValue()) || !"piedra_papel_tijera".equals(juego.child("tipoJuego").getValue()) ||
                !("piedra".equals(eleccion) || "papel".equals(eleccion) || "tijera".equals(eleccion))) return false;
        String lado=uid!=null && uid.equals(juego.child("uidAtacante").getValue()) ? "eleccionA" : uid!=null && uid.equals(juego.child("uidVictima").getValue()) ? "eleccionV" : null;
        if(lado==null) return false;
        MutableData dato=juego.child("datosJuego").child(lado);if(dato.getValue()!=null && !"".equals(dato.getValue())) return false;
        dato.setValue(eleccion);return true;
    }
    public static String elegir(String anterior,int semilla) {
        List<String> juegos=new ArrayList<>(Arrays.asList("tres_en_raya","piedra_papel_tijera","duelo_reflejos"));
        juegos.remove(anterior);return juegos.get(Math.floorMod(semilla,juegos.size()));
    }
    public static boolean publicar(MutableData sala,int ronda,String a,String v,String nombreA,String nombreV,int semilla,int delay,String uso) {
        MutableData juego=sala.child("minijuegoActual");
        if(!"revision_puntuaciones".equals(sala.child("fase").getValue()) || RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=ronda || !sala.child("jugadores").hasChild(a) ||
                !sala.child("jugadores").hasChild(v) || (Boolean.TRUE.equals(juego.child("activo").getValue()) && !"terminado".equals(juego.child("estado").getValue()))) return false;
        String tipo=elegir(sala.child("ultimoMinijuego").getValue(String.class),semilla);
        juego.setValue(null);juego.child("activo").setValue(true);juego.child("estado").setValue("anuncio");
        juego.child("tipoJuego").setValue(tipo);juego.child("uidAtacante").setValue(a);juego.child("uidVictima").setValue(v);
        juego.child("nombreAtacante").setValue(nombreA);juego.child("nombreVictima").setValue(nombreV);juego.child("ganadorUid").setValue("");
        juego.child("anuncioDesde").setValue(ServerValue.TIMESTAMP);sala.child("ultimoMinijuego").setValue(tipo);
        MutableData datos=juego.child("datosJuego");datos.child("turnoDe").setValue(a);
        if("tres_en_raya".equals(tipo)) {for(int i=0;i<9;i++) datos.child("c"+i).setValue("");datos.child("histA").setValue("");datos.child("histV").setValue("");}
        else if("piedra_papel_tijera".equals(tipo)) {datos.child("eleccionA").setValue("");datos.child("eleccionV").setValue("");datos.child("puntosA").setValue(0);datos.child("puntosV").setValue(0);}
        else {datos.child("reaccionA").setValue(0);datos.child("reaccionV").setValue(0);datos.child("delayTiroteo").setValue(2000+Math.floorMod(delay,3000));}
        MutableData registro=sala.child("comodinesUsados/ronda_"+ronda).child(uso);
        registro.child("tipo").setValue("marca_de_la_muerte");registro.child("uid").setValue(a);registro.child("nombreJugador").setValue(nombreA);registro.child("timestamp").setValue(ServerValue.TIMESTAMP);
        return true;
    }
}
