package com.example.basta_game.utils;

import com.google.firebase.database.*;
import java.util.*;

/** Local verified kernel. Activation stays PRONTO until inventory+effect have a trusted coordinator. */
public final class DecretoSilencio {
    private DecretoSilencio() { }
    public static List<String> elegirCategorias(List<String> categorias,long semilla) {
        List<String> unicas=new ArrayList<>(new LinkedHashSet<>(categorias));
        if(unicas.size()<2) return Collections.emptyList();
        Collections.shuffle(unicas,new Random(semilla));return new ArrayList<>(unicas.subList(0,2));
    }
    public static boolean infringe(String respuesta,String vocal) {
        if(EstadoRevision.vacia(respuesta) || vocal==null || !"AEIOU".contains(vocal) || vocal.length()!=1) return false;
        return sinAcentos(respuesta).contains(vocal);
    }
    private static String sinAcentos(String texto) {
        return java.text.Normalizer.normalize(texto,java.text.Normalizer.Form.NFC).toUpperCase(Locale.ROOT)
                .replace('Á','A').replace('É','E').replace('Í','I').replace('Ó','O').replace('Ú','U').replace('Ü','U');
    }
    public static String elegirVocal(String letra,int semilla) {
        String opciones="AEIOU",oficial=letra==null?"":sinAcentos(letra.trim());
        if(oficial.length()==1 && opciones.contains(oficial)) opciones=opciones.replace(oficial,"");
        return String.valueOf(opciones.charAt(Math.floorMod(semilla,opciones.length())));
    }
    public static boolean preparar(MutableData sala,String partida,int ronda,String uid,String uso,int semilla,long ahora) {
        if(partida==null || uso==null || uso.isEmpty() || !Objects.equals(partida,sala.child("idPartida").getValue()) || !"jugando".equals(sala.child("fase").getValue()) ||
                RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=ronda || !sala.child("jugadores").hasChild(uid) ||
                Boolean.TRUE.equals(sala.child("expulsados").child(uid).getValue()) || !PresenciaSala.conectada(sala,uid) ||
                sala.child("respuestas/ronda_"+ronda).getChildrenCount()>0 ||
                Boolean.TRUE.equals(sala.child("bastaPresionado").getValue())) return false;
        long inicio=RelojRevision.numero(sala.child("inicioRonda").getValue(),0);
        if(inicio<=0) return false;
        long desde=PlazoRonda.inicioRespuestas(inicio,ronda,RelojRevision.numero(sala.child("preparacionInicialMs").getValue(),0));
        Object basta=sala.child("bastaTimeStamp").getValue();
        long fin=PlazoRonda.calcularFin(desde,(int)RelojRevision.numero(sala.child("tiempoRonda").getValue(),0),basta instanceof Number?((Number)basta).longValue():null);
        if(ahora<desde || fin-ahora<20000) return false;
        List<String> categorias=new ArrayList<>();for(MutableData c:sala.child("categoriasActivas").getChildren()) if(c.getValue() instanceof String) categorias.add((String)c.getValue());
        List<String> seleccion=elegirCategorias(categorias,semilla);if(seleccion.size()!=2) return false;
        String letra=sala.child("letraActual").getValue(String.class);if(letra==null || letra.trim().isEmpty()) return false;
        MutableData d=sala.child("decretoSilencio/ronda_"+ronda);
        if(d.getValue()!=null && partida.equals(d.child("idPartida").getValue())) return false;
        d.setValue(null);
        d.child("idUso").setValue(uso);d.child("idPartida").setValue(partida);d.child("vocal").setValue(elegirVocal(letra,semilla));
        d.child("categorias").setValue(seleccion);d.child("activadoEn").setValue(ahora);return true;
    }
    /** La selección permanece anclada a partida/ronda; nunca a la posición del adaptador. */
    public static boolean categoriaAfectada(MutableData sala,String partida,int ronda,String categoria) {
        MutableData d=sala.child("decretoSilencio/ronda_"+ronda);
        if(partida==null || categoria==null || !partida.equals(d.child("idPartida").getValue())) return false;
        for(MutableData c:d.child("categorias").getChildren()) if(categoria.equals(c.getValue())) return true;
        return false;
    }
    public static boolean evaluar(MutableData sala,String partida,int ronda,String categoria) {
        MutableData d=sala.child("decretoSilencio/ronda_"+ronda);
        if(!Objects.equals(partida,sala.child("idPartida").getValue()) || !Objects.equals(partida,d.child("idPartida").getValue()) ||
                RelojRevision.numero(sala.child("rondaActual").getValue(),-1)!=ronda ||
                // Al empezar el reloj de revisión, algunas salas aún están en fase "jugando".
                (!("jugando".equals(sala.child("fase").getValue()) ||
                   "revision_puntuaciones".equals(sala.child("fase").getValue()))) ||
                d.child("evaluaciones").hasChild(categoria)) return false;
        if(!categoriaAfectada(sala,partida,ronda,categoria)) return false;
        for(MutableData j:sala.child("jugadores").getChildren()) {
            String uid=j.getKey();boolean enterado=Boolean.TRUE.equals(d.child("recibido").child(uid).getValue());
            String respuesta=sala.child("respuestas/ronda_"+ronda).child(uid).child(categoria).getValue(String.class);
            boolean viola=enterado && infringe(respuesta,d.child("vocal").getValue(String.class));
            MutableData r=d.child("evaluaciones").child(categoria).child(uid);
            r.child("recibido").setValue(enterado);r.child("infraccion").setValue(viola);
            r.child("multa").setValue(viola?5:0);
            // No alterar puntajes durante la votación: solo registrar la infracción.
            // Se aplicará al pasar a los 3 segundos del resultado.

        }
        return true;
    }

    /** Aplica las infracciones evaluadas al publicar el resultado de la categoría. */
    public static void aplicarAlResultado(MutableData sala,String partida,int ronda,String categoria) {
        if (!categoriaAfectada(sala,partida,ronda,categoria)) return;
        MutableData evaluaciones=sala.child("decretoSilencio/ronda_"+ronda+"/evaluaciones").child(categoria);
        for (MutableData evaluacion:evaluaciones.getChildren()) {
            if (!Boolean.TRUE.equals(evaluacion.child("infraccion").getValue())) continue;
            MutableData v=sala.child("validaciones/ronda_"+ronda).child(evaluacion.getKey()).child(categoria);
            v.child("puntos").setValue(0L);
            v.child("correcta").setValue(false);
            v.child("invalidadaPorDecreto").setValue(true);
        }
    }

    /** Evalua en la transaccion de cierre. Nunca altera respuestas almacenadas.
     * Devuelve la multa nominal: el cierre aplica hasta 5 puntos por infraccion,
     * sin permitir que el Decreto cree un saldo negativo adicional.
     */
    public static int invalidarEnCierre(MutableData sala,String partida,int ronda,String uid) {
        MutableData d=sala.child("decretoSilencio/ronda_"+ronda);
        if(partida==null || !partida.equals(d.child("idPartida").getValue()) ||
                !Boolean.TRUE.equals(d.child("recibido").child(uid).getValue())) return 0;
        String vocal=d.child("vocal").getValue(String.class);
        int multa=0;
        for(MutableData categoria:d.child("categorias").getChildren()) {
            String nombre=categoria.getValue(String.class);
            if(nombre==null) continue;
            String respuesta=sala.child("respuestas/ronda_"+ronda).child(uid).child(nombre).getValue(String.class);
            MutableData resultado=d.child("evaluaciones").child(nombre).child(uid);
            Object recibioEnRevision=resultado.child("recibido").getValue();
            boolean enterado=recibioEnRevision instanceof Boolean ? (Boolean)recibioEnRevision :
                    Boolean.TRUE.equals(d.child("recibido").child(uid).getValue());
            boolean infraccion=enterado && infringe(respuesta,vocal);
            resultado.child("infraccion").setValue(infraccion);
            resultado.child("multa").setValue(infraccion?5:0);
            resultado.child("recibido").setValue(enterado);
            if(infraccion) {
                MutableData validacion=sala.child("validaciones/ronda_"+ronda).child(uid).child(nombre);
                validacion.child("puntos").setValue(0L);
                validacion.child("correcta").setValue(false);
                validacion.child("invalidadaPorDecreto").setValue(true);
                multa+=5;
            }
        }
        return multa;
    }

    /** Registra cuanto se descontó de verdad sin penalizar saldos negativos previos. */
    public static int aplicarMultaEnCierre(MutableData sala,String partida,int ronda,String uid,int subtotal,int nominal) {
        if(nominal<=0) return subtotal;
        int restante=Math.max(0,subtotal);
        MutableData d=sala.child("decretoSilencio/ronda_"+ronda);
        for(MutableData categoria:d.child("categorias").getChildren()) {
            String nombre=categoria.getValue(String.class);
            if(nombre==null) continue;
            MutableData resultado=d.child("evaluaciones").child(nombre).child(uid);
            if(!Boolean.TRUE.equals(resultado.child("infraccion").getValue())) continue;
            int aplicado=Math.min(5,restante);
            restante-=aplicado;
            resultado.child("multaAplicadaReal").setValue(aplicado);
        }
        d.child("resultados").child(uid).child("multaAplicada").setValue(Math.max(0,subtotal)-restante);
        return subtotal>=0 ? restante : subtotal;
    }
}
