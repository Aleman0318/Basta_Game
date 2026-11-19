package com.example.basta_game.utils;

/** UI throttle only; currency and inventory still require a database transaction. */
public final class ControlCompra {
    private boolean pendiente;
    private long disponibleDesde;
    public synchronized boolean iniciar(long ahora) {
        if(pendiente || ahora<disponibleDesde) return false;
        pendiente=true; return true;
    }
    public synchronized void terminar(boolean confirmada,long ahora) {
        pendiente=false; disponibleDesde=ahora;
    }
    public synchronized long restante(long ahora) { return Math.max(0,disponibleDesde-ahora); }
}
