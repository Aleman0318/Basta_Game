package com.example.basta_game.utils;

import com.google.firebase.database.*;
import java.util.Map;
import java.util.function.Consumer;

/** Reserve first, publish the complete room, then expose it to joiners. */
public final class CreacionSala {
    private CreacionSala() { }
    public static void crear(DatabaseReference codigo,DatabaseReference sala,Map<String,Object> datos,Consumer<Boolean> completar) {
        if(!ConexionJuego.disponible()){completar.accept(false);return;}
        final long conexion=ConexionJuego.version();
        String id=sala.getKey(), valor=codigo.getKey();
        codigo.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData d) {
                return ConexionJuego.vigente(conexion) && ReservaCodigo.reservar(d,valor,id) ? Transaction.success(d) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean committed,DataSnapshot d) {
                if(e!=null || !committed) { completar.accept(false);return; }
                if(!ConexionJuego.vigente(conexion)){liberar(codigo,sala,datos,completar);return;}
                sala.setValue(datos).addOnCompleteListener(escritura -> {
                    if(!escritura.isSuccessful()) { liberar(codigo,sala,datos,completar);return; }
                    codigo.runTransaction(new Transaction.Handler() {
                        @Override public Transaction.Result doTransaction(MutableData c) {
                            if(c.getValue()==null) return Transaction.success(c);
                            return ReservaCodigo.activar(c,id) ? Transaction.success(c) : Transaction.abort();
                        }
                        @Override public void onComplete(DatabaseError error,boolean ok,DataSnapshot c) {
                            if(error==null && ok && c!=null && id.equals(c.child("idSala").getValue()) &&
                                    Boolean.TRUE.equals(c.child("estaActiva").getValue())) completar.accept(true);
                            else liberar(codigo,sala,datos,completar);
                        }
                    },false);
                });
            }
        },false);
    }
    private static void liberar(DatabaseReference codigo,DatabaseReference sala,Map<String,Object> datos,Consumer<Boolean> completar) {
        String id=sala.getKey(), host=(String)datos.get("hostUID");
        codigo.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData c) {
                if(c.getValue()==null) return Transaction.success(c);
                return ReservaCodigo.liberar(c,id) ? Transaction.success(c) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean ok,DataSnapshot d) {
                if(e!=null || !ok || d==null || d.exists()) {completar.accept(false);return;}
                sala.runTransaction(new Transaction.Handler() {
                    @Override public Transaction.Result doTransaction(MutableData s) {
                        if(s.getValue()==null) return Transaction.success(s);
                        return ReservaCodigo.borrarSalaParcial(s,id,host) ? Transaction.success(s) : Transaction.abort();
                    }
                    @Override public void onComplete(DatabaseError error,boolean committed,DataSnapshot s) {completar.accept(false);}
                },false);
            }
        },false);
    }
}
