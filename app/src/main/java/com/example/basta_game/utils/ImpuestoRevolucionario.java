package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.DataSnapshot;
import java.util.Objects;

/** Must run inside the coordinator's transaction; never edits earlier round scores. */
public final class ImpuestoRevolucionario {
    private ImpuestoRevolucionario() { }
    public static long disponible(MutableData sala,String uid,int ronda) {
        long base=0;
        for(int n=1;n<ronda;n++) base+=RelojRevision.numero(sala.child("puntajesRondas/ronda_"+n).child(uid).getValue(),0);
        for(MutableData prestamo:sala.child("prestamosImpuesto").getChildren()) {
            if(!Objects.equals(sala.child("idPartida").getValue(),prestamo.child("idPartida").getValue()))continue;
            long cantidad=RelojRevision.numero(prestamo.child("cantidad").getValue(),0);
            String estado=prestamo.child("estado").getValue(String.class);
            if("devuelto".equals(estado))continue;
            if(uid.equals(prestamo.child("rival").getValue()))base-=cantidad;
            if("conservado".equals(estado) && uid.equals(prestamo.child("usuario").getValue()))base+=cantidad;
        }
        return Math.max(0,base);
    }
    public static boolean prestar(MutableData sala,String partida,int ronda,String usuario,String rival,String uso) {
        if(partida==null || usuario==null || rival==null || uso==null || usuario.equals(rival) ||
                !partida.equals(sala.child("idPartida").getValue()) || RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=ronda ||
                !sala.child("jugadores").hasChild(usuario) || !sala.child("jugadores").hasChild(rival) ||
                !"jugando".equals(sala.child("fase").getValue()) || sala.child("prestamosImpuesto").hasChild(uso))return false;
        for(MutableData p:sala.child("prestamosImpuesto").getChildren())
            if(partida.equals(p.child("idPartida").getValue()) && usuario.equals(p.child("usuario").getValue()) &&
                    RelojRevision.numero(p.child("ronda").getValue(),-1)==ronda)return false;
        long cantidad=Math.min(5,disponible(sala,rival,ronda));
        if(cantidad<=0)return false;
        MutableData p=sala.child("prestamosImpuesto").child(uso);
        p.child("idPartida").setValue(partida);p.child("ronda").setValue(ronda);p.child("usuario").setValue(usuario);
        p.child("rival").setValue(rival);p.child("cantidad").setValue(cantidad);p.child("estado").setValue("pendiente");return true;
    }
    public static long ajusteVisible(MutableData sala,String uid) {
        long ajuste=0;
        for(MutableData p:sala.child("prestamosImpuesto").getChildren()) {
            if(!Objects.equals(sala.child("idPartida").getValue(),p.child("idPartida").getValue()) || "devuelto".equals(p.child("estado").getValue()))continue;
            long cantidad=RelojRevision.numero(p.child("cantidad").getValue(),0);
            if(uid.equals(p.child("usuario").getValue()))ajuste+=cantidad;
            if(uid.equals(p.child("rival").getValue()))ajuste-=cantidad;
        }
        return ajuste;
    }
    public static long ajusteVisible(DataSnapshot sala,String uid) {
        long ajuste=0;
        for(DataSnapshot p:sala.child("prestamosImpuesto").getChildren()) {
            if(!Objects.equals(sala.child("idPartida").getValue(),p.child("idPartida").getValue()) ||
                    "devuelto".equals(p.child("estado").getValue()))continue;
            long cantidad=RelojRevision.numero(p.child("cantidad").getValue(),0);
            if(uid.equals(p.child("usuario").getValue()))ajuste+=cantidad;
            if(uid.equals(p.child("rival").getValue()))ajuste-=cantidad;
        }
        return ajuste;
    }
    public static void liquidar(MutableData sala,String partida,int ronda) {
        if(!Objects.equals(partida,sala.child("idPartida").getValue()) || RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=ronda)return;
        MutableData captura=sala.child("rendimientoRondas/ronda_"+ronda);
        if(!Objects.equals(partida,captura.child("idPartida").getValue()) || !Boolean.TRUE.equals(captura.child("cerrado").getValue()))return;
        for(MutableData p:sala.child("prestamosImpuesto").getChildren()) {
            if(!Objects.equals(partida,p.child("idPartida").getValue()) || RelojRevision.numero(p.child("ronda").getValue(),-1)!=ronda || !"pendiente".equals(p.child("estado").getValue()))continue;
            String u=p.child("usuario").getValue(String.class),v=p.child("rival").getValue(String.class);
            Object a=captura.child("puntos").child(u).getValue(),b=captura.child("puntos").child(v).getValue();
            boolean seguros=a instanceof Number && b instanceof Number && Boolean.TRUE.equals(captura.child("respuestasConfirmadas").child(u).getValue()) && Boolean.TRUE.equals(captura.child("respuestasConfirmadas").child(v).getValue());
            if(!seguros){p.child("estado").setValue("devuelto");p.child("contabilizado").setValue(true);p.child("motivo").setValue("rendimiento_no_confirmado");continue;}
            if(!resolver(sala,partida,p.getKey(),((Number)a).longValue(),((Number)b).longValue()))continue;
            if("conservado".equals(p.child("estado").getValue())) {
                long cantidad=RelojRevision.numero(p.child("cantidad").getValue(),0);
                for(String id:new String[]{u,v}) {
                    long delta=id.equals(u)?cantidad:-cantidad;
                    MutableData acumulado=sala.child("jugadores").child(id).child("puntajePartida");
                    acumulado.setValue(RelojRevision.numero(acumulado.getValue(),0)+delta);
                }
            }
            p.child("contabilizado").setValue(true);
        }
    }
    public static void cancelarPorSalida(MutableData sala,String uid) {
        for(MutableData p:sala.child("prestamosImpuesto").getChildren())
            if(Objects.equals(sala.child("idPartida").getValue(),p.child("idPartida").getValue()) && "pendiente".equals(p.child("estado").getValue()) &&
                    (uid.equals(p.child("usuario").getValue()) || uid.equals(p.child("rival").getValue()))) {
                p.child("estado").setValue("devuelto");p.child("contabilizado").setValue(true);p.child("motivo").setValue("participante_retirado");
            }
    }
    /** rendimiento is the immutable round score before transfers, supplied by settlement. */
    static boolean resolver(MutableData sala,String partida,String uso,long rendimientoUsuario,long rendimientoRival) {
        MutableData p=sala.child("prestamosImpuesto").child(uso);
        if(!Objects.equals(partida,sala.child("idPartida").getValue()) || !Objects.equals(partida,p.child("idPartida").getValue()) ||
                !"pendiente".equals(p.child("estado").getValue()) || rendimientoUsuario<0 || rendimientoRival<0)return false;
        String usuario=p.child("usuario").getValue(String.class),rival=p.child("rival").getValue(String.class);
        boolean presentes=sala.child("jugadores").hasChild(usuario) && sala.child("jugadores").hasChild(rival);
        p.child("estado").setValue(presentes && rendimientoUsuario>rendimientoRival?"conservado":"devuelto");
        p.child("rendimientoUsuario").setValue(rendimientoUsuario);p.child("rendimientoRival").setValue(rendimientoRival);return true;
    }
}
