package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;

/** Score publication and completion receipt share one transaction and round identity. */
public final class CierreRevision {
    private CierreRevision(){}
    public static boolean aplicar(MutableData sala,String partida,int ronda,String uid) {
        String key="ronda_"+ronda;
        if(!VotacionRevision.rondaVigente(sala,partida,ronda) || !sala.child("jugadores").hasChild(uid) ||
                !PresenciaSala.conectada(sala,uid) || !Boolean.TRUE.equals(sala.child(RelojRevision.ruta(ronda)+"/completa").getValue()) ||
                Boolean.TRUE.equals(sala.child("votacionActiva/"+key+"/activa").getValue())) return false;
        MutableData rendimiento=sala.child("rendimientoRondas/"+key);
        if(partida.equals(rendimiento.child("idPartida").getValue())) return false;
        rendimiento.setValue(null);rendimiento.child("idPartida").setValue(partida);
        for(MutableData jugador:sala.child("jugadores").getChildren()) {
            String id=jugador.getKey();int base=0,categorias=0;
            // El Decreto se liquida una sola vez en el cierre atomico de Firebase:
            // invalidar palabras primero, calcular Doble o Nada y finalmente multa.
            int multaDecreto=DecretoSilencio.invalidarEnCierre(sala,partida,ronda,id);
            for(MutableData valor:sala.child("validaciones/"+key).child(id).getChildren()) {base+=(int)RelojRevision.numero(valor.child("puntos").getValue(),0);categorias++;}
            if(categorias==0) categorias=Math.max(1,(int)sala.child("categoriasActivas").getChildrenCount());
            rendimiento.child("puntos").child(id).setValue(base);
            rendimiento.child("respuestasConfirmadas").child(id).setValue(sala.child("respuestas/"+key).hasChild(id));
            int total=base;
            if(Boolean.TRUE.equals(sala.child("comodinesPendientes/"+key).child(id).child("doble_o_nada").getValue())) {
                int minimo=(int)Math.ceil(categorias*10*.8);boolean exito=base>=minimo;total=exito ? base*2 : base-15;
                MutableData r=sala.child("comodinesResultados/"+key).child(id).child("doble_o_nada");
                r.child("resultado").setValue(exito ? "duplicado":"castigo");r.child("activo").setValue(true);
                r.child("puntosBase").setValue(base);r.child("puntosFinales").setValue(total);r.child("categoriasEvaluadas").setValue(categorias);
                r.child("puntosMaximos").setValue(categorias*10);r.child("minimoNecesario").setValue(minimo);r.child("castigo").setValue(15);
            }
            total=DecretoSilencio.aplicarMultaEnCierre(sala,partida,ronda,id,total,multaDecreto);
            sala.child("puntajesRondas/"+key).child(id).setValue(total);
            jugador.child("puntajePartida").setValue(RelojRevision.numero(jugador.child("puntajePartida").getValue(),0)+total);
        }
        sala.child("rondasProcesadas/"+key).setValue(true);
        rendimiento.child("cerrado").setValue(true);
        ImpuestoRevolucionario.liquidar(sala,partida,ronda);
        sala.child("resumenRondaListo/"+key).setValue(true);
        sala.child("fase").setValue(ronda>=RelojRevision.numero(sala.child("cantidadRondas").getValue(),ronda) ? "resultados_finales" : "revision_puntuaciones");
        return true;
    }
}
