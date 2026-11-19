package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;

/** D4: congelador global con 2 usos maximos por partida.
 * Nunca activar si los 8 s invaden los ultimos 10 s de la ronda.
 */
public final class ActivacionCongelador {
    private ActivacionCongelador() { }
    public static final int MAX_USOS_PARTIDA=2;
    public static final long MARGEN_FINAL_MS=10000L;

    public static boolean disponible(MutableData sala,String idPartida,int ronda,String uid,long ahoraServidor,int duracionMs) {
        if(sala.getValue()==null || idPartida==null || uid==null ||
                !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                !"jugando".equals(sala.child("fase").getValue(String.class)) ||
                !sala.child("jugadores").hasChild(uid) ||
                !PresenciaSala.conectada(sala,uid) ||
                Boolean.TRUE.equals(sala.child("bastaPresionado").getValue()) ||
                !(sala.child("rondaActual").getValue() instanceof Number) ||
                ((Number)sala.child("rondaActual").getValue()).intValue()!=ronda) return false;
        Object usos=sala.child("usosCongeladorPartida").getValue();
        if(usos instanceof Number && ((Number)usos).intValue()>=MAX_USOS_PARTIDA) return false;
        Object marca=sala.child("inicioRonda").getValue();
        Object duracion=sala.child("tiempoRonda").getValue();
        if(!(marca instanceof Number) || !(duracion instanceof Number) ||
                !(sala.child("inicioRondaListoDe").getValue() instanceof Number) ||
                ((Number)sala.child("inicioRondaListoDe").getValue()).intValue()!=ronda) return false;
        long inicio=PlazoRonda.inicioRespuestas(((Number)marca).longValue(),ronda,
                RelojRevision.numero(sala.child("preparacionInicialMs").getValue(),0));
        long fin=PlazoRonda.calcularFin(inicio,((Number)duracion).intValue(),null);
        if(ahoraServidor < inicio || fin-ahoraServidor <= duracionMs+MARGEN_FINAL_MS) return false;
        MutableData previo=sala.child("comodinesActivos/ronda_"+ronda+"/congelador");
        Object anterior=previo.child("inicio").getValue();
        Object tiempo=previo.child("duracionMs").getValue();
        if(Boolean.TRUE.equals(previo.child("activo").getValue()) &&
                anterior instanceof Number && tiempo instanceof Number &&
                ((Number)anterior).longValue()+((Number)tiempo).longValue()>ahoraServidor) return false;
        return true;
    }

    // Lectura previa para no descontar inventario cuando ya se agotaron los usos.
    // La transacción de activar vuelve a validar porque esta lectura puede cambiar.
    public static boolean disponible(com.google.firebase.database.DataSnapshot sala,String idPartida,int ronda,
                                     String uid,long ahoraServidor,int duracionMs) {
        if(!sala.exists() || idPartida==null || uid==null ||
                !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                !"jugando".equals(sala.child("fase").getValue(String.class)) ||
                !sala.child("jugadores").hasChild(uid) ||
                Boolean.FALSE.equals(sala.child("jugadores").child(uid).child("estaConectado").getValue()) ||
                Boolean.TRUE.equals(sala.child("bastaPresionado").getValue()) ||
                !(sala.child("rondaActual").getValue() instanceof Number) ||
                ((Number)sala.child("rondaActual").getValue()).intValue()!=ronda) return false;
        Object usos=sala.child("usosCongeladorPartida").getValue();
        if(usos instanceof Number && ((Number)usos).intValue()>=MAX_USOS_PARTIDA) return false;
        Object marca=sala.child("inicioRonda").getValue();
        Object duracion=sala.child("tiempoRonda").getValue();
        if(!(marca instanceof Number) || !(duracion instanceof Number)) return false;
        long preparacion=sala.child("preparacionInicialMs").getValue() instanceof Number
                ? ((Number)sala.child("preparacionInicialMs").getValue()).longValue():0;
        long inicio=PlazoRonda.inicioRespuestas(((Number)marca).longValue(),ronda,preparacion);
        long fin=PlazoRonda.calcularFin(inicio,((Number)duracion).intValue(),null);
        if(ahoraServidor<inicio || fin-ahoraServidor<=duracionMs+MARGEN_FINAL_MS) return false;
        com.google.firebase.database.DataSnapshot previo=sala.child("comodinesActivos/ronda_"+ronda+"/congelador");
        Object anterior=previo.child("inicio").getValue(),tiempo=previo.child("duracionMs").getValue();
        return !Boolean.TRUE.equals(previo.child("activo").getValue()) ||
                !(anterior instanceof Number) || !(tiempo instanceof Number) ||
                ((Number)anterior).longValue()+((Number)tiempo).longValue()<=ahoraServidor;
    }


    /** Lectura preliminar para UX; la decisión definitiva sigue en la transacción. */
    public static boolean disponibleSnapshot(com.google.firebase.database.DataSnapshot sala,
            String partida,int ronda,String uid,long ahora,int duracionMs) {
        if(sala==null || !sala.exists() || partida==null || uid==null ||
                !partida.equals(sala.child("idPartida").getValue(String.class)) ||
                !"jugando".equals(sala.child("fase").getValue(String.class)) ||
                !sala.child("jugadores").hasChild(uid) ||
                Boolean.FALSE.equals(sala.child("jugadores").child(uid).child("estaConectado").getValue(Boolean.class)) ||
                Boolean.TRUE.equals(sala.child("bastaPresionado").getValue(Boolean.class))) return false;
        Object rondaObj=sala.child("rondaActual").getValue();
        if(!(rondaObj instanceof Number) || ((Number)rondaObj).intValue()!=ronda) return false;
        Object usos=sala.child("usosCongeladorPartida").getValue();
        if(usos instanceof Number && ((Number)usos).intValue()>=MAX_USOS_PARTIDA) return false;
        Object marca=sala.child("inicioRonda").getValue();
        Object duracion=sala.child("tiempoRonda").getValue();
        Object listo=sala.child("inicioRondaListoDe").getValue();
        if(!(marca instanceof Number) || !(duracion instanceof Number) ||
                !(listo instanceof Number) || ((Number)listo).intValue()!=ronda) return false;
        long preparacion=RelojRevision.numero(sala.child("preparacionInicialMs").getValue(),0);
        long inicio=PlazoRonda.inicioRespuestas(((Number)marca).longValue(),ronda,preparacion);
        long fin=PlazoRonda.calcularFin(inicio,((Number)duracion).intValue(),null);
        if(ahora<inicio || fin-ahora<=duracionMs+MARGEN_FINAL_MS) return false;
        com.google.firebase.database.DataSnapshot previo=sala.child("comodinesActivos/ronda_"+ronda+"/congelador");
        Object anterior=previo.child("inicio").getValue();
        Object tiempo=previo.child("duracionMs").getValue();
        return !Boolean.TRUE.equals(previo.child("activo").getValue(Boolean.class)) ||
                !(anterior instanceof Number) || !(tiempo instanceof Number) ||
                ((Number)anterior).longValue()+((Number)tiempo).longValue()<=ahora;
    }

    public static boolean activar(MutableData sala,String partida,int ronda,String uid,String nombre,
                                  String idUso,long ahoraServidor,int duracionMs) {
        if(!disponible(sala,partida,ronda,uid,ahoraServidor,duracionMs) || idUso==null) return false;
        String ruta="comodinesActivos/ronda_"+ronda+"/congelador";
        sala.child(ruta+"/activo").setValue(true);
        sala.child(ruta+"/idUso").setValue(idUso);
        sala.child(ruta+"/tipo").setValue("congelador");
        sala.child(ruta+"/usadoPor").setValue(uid);
        sala.child(ruta+"/nombreJugador").setValue(nombre);
        sala.child(ruta+"/inicio").setValue(ServerValue.TIMESTAMP);
        sala.child(ruta+"/duracionMs").setValue(duracionMs);
        String registro="comodinesUsados/ronda_"+ronda+"/"+idUso;
        sala.child(registro+"/tipo").setValue("congelador");
        sala.child(registro+"/uid").setValue(uid);
        sala.child(registro+"/nombreJugador").setValue(nombre);
        sala.child(registro+"/timestamp").setValue(ServerValue.TIMESTAMP);
        Object previo=sala.child("usosCongeladorPartida").getValue();
        int usos=previo instanceof Number?((Number)previo).intValue():0;
        sala.child("usosCongeladorPartida").setValue(usos+1);
        return true;
    }
}
