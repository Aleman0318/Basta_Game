package com.example.basta_game.comodines;
import com.google.firebase.database.*;
import java.util.Random;
public class ComodinRuletaCaotica extends BaseComodin {
    public static final String ID_COMODIN="ruleta_caotica";
    public ComodinRuletaCaotica(String idSala,int ronda){super(idSala,ronda);}
    @Override public void activar(String nombre,ComodinCallback callback) {
        Random azar=new Random();final int efecto=azar.nextInt(5);final double a=azar.nextDouble(),b=azar.nextDouble();
        final String uso="ruleta_"+salaRef.push().getKey();
        final long conexion=com.example.basta_game.utils.ConexionJuego.version();
        descontarInventario(ID_COMODIN,callback,() -> salaRef.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData sala) {
                if(!com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return Transaction.abort();
                if(sala.getValue()==null) return Transaction.success(sala);
                return com.example.basta_game.utils.EfectoRuleta.aplicar(sala,rondaActual,miUid,nombre,uso,efecto,a,b) ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean ok,DataSnapshot sala) {
                if(e==null && sala!=null && sala.child("comodinesResultados/ronda_"+rondaActual).hasChild(uso)) callback.onSuccess("SILENCIO");
                else callback.onError("No se pudo confirmar la ruleta. Revisa tu conexion");
            }
        },false));
    }
}