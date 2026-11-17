package com.example.basta_game.utils;

/** UI policy only: transport recovery is not proof that the Internet is absent. */
public final class EstadoConexion {
    private EstadoConexion(){}
    public static boolean accionesDisponibles(boolean transporte,boolean reloj,boolean red,boolean enSala,boolean salaLista){
        return transporte && reloj && red && (!enSala || salaLista);
    }
    public static boolean bloquear(boolean red,boolean enSala,boolean error,long esperandoMs){
        return !red;
    }
}
