package com.example.basta_game.utils;
import com.google.firebase.database.*;
import java.util.*;

public final class PuntosSaltarines {
    private PuntosSaltarines() { }
    public static boolean aplicar(MutableData sala,int ronda,String atacante,String nombre,String uso,double semillaA,double semillaB) {
        MutableData registro=sala.child("comodinesResultados/ronda_"+ronda).child(uso);
        if(registro.getValue()!=null || RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=ronda ||
                !sala.child("jugadores").hasChild(atacante) || !"revision_puntuaciones".equals(sala.child("fase").getValue()) ||
                semillaA<0 || semillaA>=1 || semillaB<0 || semillaB>=1) return false;
        List<String> activos=new ArrayList<>();
        for(MutableData j:sala.child("jugadores").getChildren()) if(PresenciaSala.conectada(sala,j.getKey())) activos.add(j.getKey());
        Collections.sort(activos);if(activos.size()<2) return false;
        String a=activos.remove((int)(semillaA*activos.size())), b=activos.get((int)(semillaB*activos.size()));
        MutableData puntos=sala.child("puntajesRondas/ronda_"+ronda);
        long de=RelojRevision.numero(puntos.child(a).getValue(),0), para=RelojRevision.numero(puntos.child(b).getValue(),0);
        if(de<0 || para<0 || para>Integer.MAX_VALUE-5) return false;
        long cantidad=Math.min(5,de);puntos.child(a).setValue(de-cantidad);puntos.child(b).setValue(para+cantidad);
        String nombreA=sala.child("jugadores").child(a).child("nombre").getValue(String.class), nombreB=sala.child("jugadores").child(b).child("nombre").getValue(String.class);
        String mensaje="¡Puntos Saltarines! "+(nombreA==null ? "Un jugador" : nombreA)+" paso "+cantidad+" puntos a "+(nombreB==null ? "su rival" : nombreB)+". ¡Menudo salto!";
        registro.child("tipo").setValue("ruleta_caotica");registro.child("nombreAtacante").setValue(nombre);registro.child("efecto").setValue("puntos_saltarines");
        registro.child("uidOrigen").setValue(a);registro.child("uidDestino").setValue(b);registro.child("cantidad").setValue(cantidad);registro.child("mensaje").setValue(mensaje);registro.child("timestamp").setValue(ServerValue.TIMESTAMP);
        MutableData evento=sala.child("eventoCaosGlobal/ronda_"+ronda);
        evento.child("idUso").setValue(uso);evento.child("titulo").setValue("¡Puntos Saltarines!");evento.child("mensaje").setValue(mensaje);
        evento.child("esExito").setValue(true);evento.child("uidAtacante").setValue("RULETA");evento.child("uidVictima").setValue("↗");evento.child("timestamp").setValue(ServerValue.TIMESTAMP);
        return true;
    }
}
