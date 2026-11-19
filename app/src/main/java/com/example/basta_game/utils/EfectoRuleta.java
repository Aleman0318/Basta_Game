package com.example.basta_game.utils;
import com.google.firebase.database.*;
import java.util.*;

public final class EfectoRuleta {
    private EfectoRuleta(){}
    public static boolean aplicar(MutableData sala,int ronda,String uid,String nombre,String uso,int efecto,double a,double b) {
        String key="ronda_"+ronda;
        if(sala.getValue()==null || efecto<0 || efecto>4 || !"revision_puntuaciones".equals(sala.child("fase").getValue()) ||
                RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=ronda || !PresenciaSala.conectada(sala,uid) ||
                sala.child("comodinesResultados/"+key).hasChild(uso)) return false;
        if(efecto==2) return PuntosSaltarines.aplicar(sala,ronda,uid,nombre,uso,a,b);
        List<String> jugadores=new ArrayList<>();
        for(MutableData j:sala.child("jugadores").getChildren()) if(PresenciaSala.conectada(sala,j.getKey())) jugadores.add(j.getKey());
        if(jugadores.size()<2) return false;
        MutableData puntos=sala.child("puntajesRondas/"+key);
        jugadores.sort((x,y) -> {int c=Long.compare(RelojRevision.numero(puntos.child(y).getValue(),0),RelojRevision.numero(puntos.child(x).getValue(),0));return c==0 ? x.compareTo(y):c;});
        Map<String,Object> cambios=new HashMap<>();String titulo,mensaje;
        if(efecto==0) {
            long total=0;for(String j:jugadores) total+=RelojRevision.numero(puntos.child(j).getValue(),0);
            long media=total/jugadores.size(),resto=total%jugadores.size();
            List<String> reparto=new ArrayList<>(jugadores);Collections.sort(reparto);
            for(int i=0;i<reparto.size();i++) cambios.put(reparto.get(i),media+(i<resto?1:0));
            titulo="El Comunismo";mensaje="¡Atención, camaradas! ¡La ruleta ha declarado que la riqueza no debe quedarse en unas solas manos! ¡Es hora de redistribuir los puntos!\n\nEfecto aplicado: Todos los puntos han sido repartidos por igual entre los jugadores activos, sin importar su posición en la clasificación."
                    +(resto>0 ? " Los "+resto+" puntos indivisibles se reparten de uno en uno por orden de identificador, sin perder puntos." : "");
        } else if(efecto==1) {
            for(int i=0;i<jugadores.size();i++) cambios.put(jugadores.get(i),RelojRevision.numero(puntos.child(jugadores.get((i+1)%jugadores.size())).getValue(),0));
            titulo="Carrusel de puntos";mensaje="¡La tabla dio una vuelta! Cada participante activo recibio los puntos del siguiente.";
        } else if(efecto==3) {
            String primero=jugadores.get(0),ultimo=jugadores.get(jugadores.size()-1);
            long saldo=RelojRevision.numero(puntos.child(primero).getValue(),0),impuesto=Math.max(0,(long)(saldo*.30));
            cambios.put(primero,saldo-impuesto);cambios.put(ultimo,RelojRevision.numero(puntos.child(ultimo).getValue(),0)+impuesto);
            titulo="Robin Hood";mensaje="¡Robin Hood movio "+impuesto+" puntos del primer lugar al ultimo!";
        } else {
            for(String j:jugadores) {
                long saldo=RelojRevision.numero(puntos.child(j).getValue(),0);
                if(saldo<0) {cambios.put(j,saldo);continue;}
                long invertido=Long.parseLong(new StringBuilder(String.valueOf(saldo)).reverse().toString());
                if(invertido>Integer.MAX_VALUE) return false;cambios.put(j,invertido);
            }
            titulo="Numeros locos";mensaje="¡Los digitos de los puntos de la ronda dieron la vuelta!";
        }
        StringBuilder detalle=new StringBuilder();
        for(String j:jugadores){
            String jugador=sala.child("jugadores").child(j).child("nombre").getValue(String.class);
            long antes=RelojRevision.numero(puntos.child(j).getValue(),0), despues=((Number)cambios.get(j)).longValue(), diferencia=despues-antes;
            detalle.append(jugador==null?"Participante":jugador).append(diferencia>=0?" ganó ":" perdió ").append(Math.abs(diferencia)).append(" puntos. ");
        }
        if(efecto!=0) mensaje=mensaje+" "+detalle.append("¡La ruleta hizo de las suyas!");
        for(Map.Entry<String,Object> c:cambios.entrySet()) puntos.child(c.getKey()).setValue(c.getValue());
        MutableData r=sala.child("comodinesResultados/"+key).child(uso);
        r.child("tipo").setValue("ruleta_caotica");r.child("atacante").setValue(uid);r.child("nombreAtacante").setValue(nombre);
        r.child("efecto").setValue(efecto);r.child("mensaje").setValue(mensaje);r.child("timestamp").setValue(ServerValue.TIMESTAMP);
        MutableData evento=sala.child("eventoCaosGlobal/"+key);
        evento.child("idUso").setValue(uso);evento.child("titulo").setValue(titulo);evento.child("mensaje").setValue(mensaje);
        evento.child("esExito").setValue(true);evento.child("uidAtacante").setValue("RULETA");evento.child("uidVictima").setValue("↔");evento.child("timestamp").setValue(ServerValue.TIMESTAMP);
        return true;
    }
}
