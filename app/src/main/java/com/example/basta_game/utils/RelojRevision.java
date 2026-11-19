package com.example.basta_game.utils;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import java.util.Map;

/** One persisted clock: proposal window and decisive ballot use successive turns. */
public final class RelojRevision {
    public static final long DURACION_MS=5000, ESPERA_MS=8000;
    private RelojRevision() { }
    public static boolean guardarIniciales(MutableData sala,String partida,int ronda,String uid,Map<String,Object> valores) {
        if(!VotacionRevision.rondaVigente(sala,partida,ronda) || uid==null || !sala.child("jugadores").hasChild(uid)) return false;
        boolean cambio=false;
        MutableData validaciones=sala.child("validaciones/ronda_"+ronda).child(uid);
        for(Map.Entry<String,Object> entrada:valores.entrySet()) {
            if(!validaciones.hasChild(entrada.getKey())) {
                validaciones.child(entrada.getKey()).setValue(entrada.getValue()); cambio=true;
            }
        }
        return cambio;
    }
    public static String ruta(int ronda) { return "revisionCategorias/ronda_"+ronda; }
    public static long numero(Object v,long defecto) { return v instanceof Number ? ((Number)v).longValue() : defecto; }
    public static long fin(Object inicio) { return inicio instanceof Number ? ((Number)inicio).longValue()+DURACION_MS : Long.MAX_VALUE; }
    public static long fin(Object inicio,boolean vacia) { return inicio instanceof Number ? ((Number)inicio).longValue()+(vacia ? 2000 : DURACION_MS) : Long.MAX_VALUE; }
    public static long finTurno(Object inicio,Object duracion,boolean vacia) {
        long ms=numero(duracion,DURACION_MS);
        if(ms!=3500 && ms!=3000 && ms!=5000 && ms!=10000) ms=DURACION_MS;
        return inicio instanceof Number ? ((Number)inicio).longValue()+(vacia ? 2000 : ms) : Long.MAX_VALUE;
    }
    @SuppressWarnings("unchecked")
    public static boolean categoriaVacia(Object participantes,Object respuestas,String categoria) {
        if(!(participantes instanceof Map) || !(respuestas instanceof Map) || ((Map<?,?>)participantes).isEmpty()) return false;
        for(Object uid:((Map<?,?>)participantes).keySet()) {
            Object categorias=((Map<?,?>)respuestas).get(uid);
            if(!(categorias instanceof Map) || !((Map<?,?>)categorias).containsKey(categoria)) return false;
            Object palabra=((Map<?,?>)categorias).get(categoria);
            if(!(palabra instanceof String) || !EstadoRevision.vacia((String)palabra)) return false;
        }
        return true;
    }
    public static boolean abierto(MutableData sala,int ronda,long ahora,String modo) {
        MutableData reloj=sala.child(ruta(ronda)+"/reloj");
        return java.util.Objects.equals(sala.child("idPartida").getValue(),reloj.child("idPartida").getValue()) &&
                modo.equals(reloj.child("modo").getValue()) && reloj.child("inicio").getValue() instanceof Number &&
                ahora>=numero(reloj.child("inicio").getValue(),Long.MAX_VALUE) && ahora<finTurno(reloj.child("inicio").getValue(),reloj.child("duracionMs").getValue(),false);
    }
    private static void preparar(MutableData sala,int ronda,int indice,String categoria,long turno,String modo,String id) {
        MutableData r=sala.child(ruta(ronda)+"/reloj");
        r.setValue(null);
        r.child("idPartida").setValue(sala.child("idPartida").getValue());
        r.child("indice").setValue(indice); r.child("categoria").setValue(categoria);
        r.child("turno").setValue(turno); r.child("modo").setValue(modo);
        r.child("idVotacion").setValue(id);
        r.child("preparandoDesde").setValue(ServerValue.TIMESTAMP);
        int elegibles=0; for(MutableData j:sala.child("jugadores").getChildren()) if(!Boolean.FALSE.equals(j.child("estaConectado").getValue())) elegibles++;
                r.child("duracionMs").setValue("revision".equals(modo) ? 3500 : "resultado".equals(modo) ? 3000 : elegibles>5 ? 10000 : 5000);
        if("votos".equals(modo) || "resultado".equals(modo) || "propuestas".equals(modo)) r.child("inicio").setValue(ServerValue.TIMESTAMP);
        if("votos".equals(modo)) for(MutableData j:sala.child("jugadores").getChildren())
            if(!Boolean.FALSE.equals(j.child("estaConectado").getValue())) r.child("elegibles").child(j.getKey()).setValue(true);
    }
    public static boolean prepararVoto(MutableData sala,int ronda,String id,long ahora,String categoria) {
        MutableData r=sala.child(ruta(ronda)+"/reloj");
        if(Boolean.TRUE.equals(sala.child(ruta(ronda)+"/completa").getValue()) ||
                Boolean.TRUE.equals(sala.child(ruta(ronda)+"/cerradas").child(String.valueOf(numero(r.child("indice").getValue(),-1))).getValue())) return false;
        if(!r.hasChildren()) return true; // Legacy rooms keep their existing fields/behaviour.
        if(!abierto(sala,ronda,ahora,"propuestas") || !categoria.equals(r.child("categoria").getValue())) return false;
        Object seleccionInicio=r.child("inicio").getValue(), seleccionDuracion=r.child("duracionMs").getValue();
        preparar(sala,ronda,(int)numero(r.child("indice").getValue(),0),categoria,
                numero(r.child("turno").getValue(),0)+1,"votos",id);
        r.child("seleccionInicio").setValue(seleccionInicio);
        r.child("seleccionDuracionMs").setValue(seleccionDuracion);
        return true;
    }
    public static boolean preparado(MutableData sala,String partida,int ronda,int indice,long turno,String uid) {
        if(!VotacionRevision.rondaVigente(sala,partida,ronda) || uid==null || !sala.child("jugadores").hasChild(uid)) return false;
        MutableData r=sala.child(ruta(ronda)+"/reloj");
        if(!partida.equals(r.child("idPartida").getValue()) || numero(r.child("indice").getValue(),-1)!=indice || numero(r.child("turno").getValue(),-1)!=turno ||
                r.child("inicio").getValue()!=null || Boolean.TRUE.equals(r.child("listos").child(uid).getValue())) return false;
        r.child("listos").child(uid).setValue(true); return true;
    }
    public static boolean coordinar(MutableData sala,String partida,int ronda,String host,long ahora) {
        if(!VotacionRevision.rondaVigente(sala,partida,ronda) || host==null || !sala.child("jugadores").hasChild(host) ||
                Boolean.FALSE.equals(sala.child("jugadores").child(host).child("estaConectado").getValue())) return false;
        MutableData revision=sala.child(ruta(ronda));
        if(Boolean.TRUE.equals(revision.child("completa").getValue())) return false;
        int indice=(int)numero(revision.child("indiceActual").getValue(),0);
        long total=sala.child("categoriasActivas").getChildrenCount();
        if(indice<0 || indice>=total) return false;
        String categoria=sala.child("categoriasActivas").child(String.valueOf(indice)).getValue(String.class);
        MutableData r=revision.child("reloj");
        MutableData voto=sala.child("votacionActiva/ronda_"+ronda);
        if(!r.hasChildren()) {
            revision.child("indiceActual").setValue(indice);
            boolean vacia=categoriaVacia(sala.child("jugadores").getValue(),sala.child("respuestas/ronda_"+ronda).getValue(),categoria);
            boolean recibidas=true;
            for(MutableData j:sala.child("jugadores").getChildren())
                if(!sala.child("respuestas/ronda_"+ronda).child(j.getKey()).hasChild(categoria)) recibidas=false;
            if(recibidas) {
                completarValoraciones(sala,ronda,categoria);
                DecretoSilencio.evaluar(sala,partida,ronda,categoria);
            }
            // El resultado inmediato (p. ej., respuestas repetidas) también
            // debe aplicar Decreto antes de que se muestren los puntos.
            boolean resultadoDirecto=recibidas && !vacia && !hayCandidatas(sala,ronda,categoria);
            if (resultadoDirecto) DecretoSilencio.aplicarAlResultado(sala,partida,ronda,categoria);
            preparar(sala,ronda,indice,categoria,1,resultadoDirecto ? "resultado" : "revision",null); return true;
        }
        if(Boolean.TRUE.equals(voto.child("activa").getValue()) && !"votos".equals(r.child("modo").getValue())) {
            String id=voto.child("idVotacion").getValue(String.class);
            if(id==null) return false;
            preparar(sala,ronda,indice,categoria,numero(r.child("turno").getValue(),0)+1,"votos",id); return true;
        }
        if(r.child("inicio").getValue()==null) {
            boolean todos=true; int activos=0;
            for(MutableData j:sala.child("jugadores").getChildren()) {
                if(Boolean.FALSE.equals(j.child("estaConectado").getValue())) continue;
                activos++;
                if(!Boolean.TRUE.equals(r.child("listos").child(j.getKey()).getValue())) todos=false;
            }
            Object desde=r.child("preparandoDesde").getValue();
            if(activos>0 && (todos || (desde instanceof Number && ahora-numero(desde,ahora)>=ESPERA_MS))) {
                r.child("inicio").setValue(ServerValue.TIMESTAMP); return true;
            }
            return false;
        }
        String modo=r.child("modo").getValue(String.class);
        boolean vacia="revision".equals(modo) && categoriaVacia(sala.child("jugadores").getValue(),sala.child("respuestas/ronda_"+ronda).getValue(),categoria);
        if(!Boolean.TRUE.equals(voto.child("activa").getValue()) &&
                ahora>=finTurno(r.child("inicio").getValue(),r.child("duracionMs").getValue(),vacia)) {
            completarValoraciones(sala,ronda,categoria);
            DecretoSilencio.evaluar(sala,partida,ronda,categoria);
            if(vacia || "resultado".equals(modo)) {
                revision.child("cerradas").child(String.valueOf(indice)).setValue(true);
                if(indice+1>=total) revision.child("completa").setValue(true);
                else {revision.child("indiceActual").setValue(indice+1);revision.child("reloj").setValue(null);}
            } else {
                String siguiente="revision".equals(modo) && hayCandidatas(sala,ronda,categoria) ? "propuestas" : "resultado";
                if ("resultado".equals(siguiente)) DecretoSilencio.aplicarAlResultado(sala,partida,ronda,categoria);
                preparar(sala,ronda,indice,categoria,numero(r.child("turno").getValue(),0)+1,siguiente,null);
            }
            return true;
        }
        return false;
    }
    private static boolean hayCandidatas(MutableData sala,int ronda,String categoria) {
        // Decreto no cancela las votaciones, solamente limita la puntuación.
        for(MutableData jugador:sala.child("jugadores").getChildren()) {
            String uid=jugador.getKey();
            String palabra=sala.child("respuestas/ronda_"+ronda).child(uid).child(categoria).getValue(String.class);
            long puntos=numero(sala.child("validaciones/ronda_"+ronda).child(uid).child(categoria).child("puntos").getValue(),0);
            boolean decreto=DecretoSilencio.categoriaAfectada(sala,
                    sala.child("idPartida").getValue(String.class),ronda,categoria);
            // Una palabra con vocal prohibida puede ser votada aunque su puntuación
            // preliminar sea 0; una palabra normalmente inválida NO puede votarse.
            if(ValidacionBasica.correcta(sala.child("letraActual").getValue(String.class),palabra) &&
                    puntos!=5 && (puntos!=0 || decreto) && !Boolean.TRUE.equals(sala.child("resultadosVotacionPalabras/ronda_"+ronda)
                    .child(ClaveRevision.de(ronda,uid,categoria,palabra)).child("resuelta").getValue())) return true;
        }
        return false;
    }
    public static boolean todosVotaron(Object elegibles,Object miembros,Object votos) {
        if(!(elegibles instanceof Map) || !(miembros instanceof Map)) return false;
        Map<?,?> v=votos instanceof Map ? (Map<?,?>)votos : java.util.Collections.emptyMap();int requeridos=0;
        for(Object uid:((Map<?,?>)elegibles).keySet()) {
            Object j=((Map<?,?>)miembros).get(uid);if(!(j instanceof Map) || Boolean.FALSE.equals(((Map<?,?>)j).get("estaConectado"))) continue;
            requeridos++;if(!v.containsKey(uid)) return false;
        }
        return requeridos>0;
    }
    private static void completarValoraciones(MutableData sala,int ronda,String categoria) {
        for(MutableData jugador:sala.child("jugadores").getChildren()) {
            MutableData valor=sala.child("validaciones/ronda_"+ronda).child(jugador.getKey()).child(categoria);
            String palabra=sala.child("respuestas/ronda_"+ronda).child(jugador.getKey()).child(categoria).getValue(String.class);
            // Nunca admitir +10 heredados o tardíos para casillas vacías.
            if(EstadoRevision.vacia(palabra)) {
                valor.child("correcta").setValue(false);
                valor.child("puntos").setValue(0L);
                continue;
            }
            if(valor.getValue()!=null) continue;
            boolean correcta=ValidacionBasica.correcta(sala.child("letraActual").getValue(String.class),palabra);
            int repeticiones=0;
            if(correcta) for(MutableData otro:sala.child("jugadores").getChildren()) {
                String otra=sala.child("respuestas/ronda_"+ronda).child(otro.getKey()).child(categoria).getValue(String.class);
                if(otra!=null && otra.trim().equalsIgnoreCase(palabra.trim())) repeticiones++;
            }
            valor.child("correcta").setValue(correcta);
            valor.child("puntos").setValue(correcta ? (repeticiones>1 ? 5 : 10) : 0);
        }
    }
    public static boolean puedeCerrarVoto(MutableData sala,int ronda,String id,long ahora) {
        MutableData r=sala.child(ruta(ronda)+"/reloj");
        return !r.hasChildren() || ("votos".equals(r.child("modo").getValue()) && id.equals(r.child("idVotacion").getValue()) &&
                r.child("inicio").getValue() instanceof Number && (ahora>=finTurno(r.child("inicio").getValue(),r.child("duracionMs").getValue(),false) || todosVotaron(r.child("elegibles").getValue(),sala.child("jugadores").getValue(),sala.child("votacionActiva/ronda_"+ronda+"/votos").getValue())));
    }
    public static void siguientePropuesta(MutableData sala,int ronda,String id,boolean conVotos) {
        MutableData r=sala.child(ruta(ronda)+"/reloj");
        if(r.hasChildren() && id.equals(r.child("idVotacion").getValue())) {
            Object inicio=r.child("seleccionInicio").getValue(), duracion=r.child("seleccionDuracionMs").getValue();
            String categoria=r.child("categoria").getValue(String.class);
            boolean candidatas=hayCandidatas(sala,ronda,categoria);
            if(!candidatas) DecretoSilencio.aplicarAlResultado(sala,sala.child("idPartida").getValue(String.class),ronda,categoria);
            preparar(sala,ronda,(int)numero(r.child("indice").getValue(),0),categoria,
                    numero(r.child("turno").getValue(),0)+1,candidatas ? "propuestas" : "resultado",null);
            if(candidatas && !conVotos && inicio instanceof Number) {
                r.child("inicio").setValue(inicio);r.child("duracionMs").setValue(duracion);
            }
        }
    }
}
