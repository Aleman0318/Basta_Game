package com.example.basta_game.utils;

import java.util.ArrayDeque;

/** State belongs to the application, never to a fragment or a View. */
public final class ColaAvisos {
    public static final class Aviso {
        public final String clave, texto;
        public final int tipo;
        Aviso(String clave, String texto, int tipo) { this.clave=clave; this.texto=texto; this.tipo=tipo; }
    }
    private final ArrayDeque<Aviso> cola=new ArrayDeque<>();
    private Aviso actual;
    private long vence;

    public void agregar(String clave, String texto, int tipo, long ahora) {
        actual(ahora);
        if (actual != null && clave != null && clave.equals(actual.clave)) {
            actual=new Aviso(clave,texto,tipo); vence=ahora+3000; return;
        }
        if (actual != null && actual.texto.equals(texto) && actual.tipo==tipo) return;
        if (clave != null) cola.removeIf(a -> clave.equals(a.clave));
        for (Aviso a:cola) if(a.texto.equals(texto) && a.tipo==tipo) return;
        Aviso nuevo=new Aviso(clave,texto,tipo);
        if(actual==null) { actual=nuevo; vence=ahora+3000; } else cola.offer(nuevo);
    }
    public Aviso actual(long ahora) {
        if(actual!=null && ahora>=vence) actual=null;
        if(actual==null && !cola.isEmpty()) { actual=cola.poll(); vence=ahora+3000; }
        return actual;
    }
    public long restante(long ahora) { return actual(ahora)==null ? 0 : Math.max(0,vence-ahora); }
    public int pendientes() { return cola.size()+(actual==null ? 0 : 1); }
}
