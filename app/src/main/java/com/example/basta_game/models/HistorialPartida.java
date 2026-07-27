package com.example.basta_game.models;

import java.util.ArrayList;

public class HistorialPartida {

    private String idPartida;
    private String idSala;
    private String codigoSala;
    private long fecha;
    private int cantidadRondas;
    private int jugadoresCantidad;
    private boolean monedasEntregadas;

    private String miUid;
    private String miNombre;
    private int miPosicion;
    private int miTotal;
    private int monedas;

    private String ganadorUid;
    private String ganadorNombre;
    private int puntajeGanador;

    private ArrayList<TopJugador> top3 = new ArrayList<>();

    public HistorialPartida() {
    }

    public String getIdPartida() {
        return idPartida;
    }

    public void setIdPartida(String idPartida) {
        this.idPartida = idPartida;
    }

    public String getIdSala() {
        return idSala;
    }

    public void setIdSala(String idSala) {
        this.idSala = idSala;
    }

    public String getCodigoSala() {
        return codigoSala;
    }

    public void setCodigoSala(String codigoSala) {
        this.codigoSala = codigoSala;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public int getCantidadRondas() {
        return cantidadRondas;
    }

    public void setCantidadRondas(int cantidadRondas) {
        this.cantidadRondas = cantidadRondas;
    }

    public int getJugadoresCantidad() {
        return jugadoresCantidad;
    }

    public void setJugadoresCantidad(int jugadoresCantidad) {
        this.jugadoresCantidad = jugadoresCantidad;
    }

    public boolean isMonedasEntregadas() {
        return monedasEntregadas;
    }

    public void setMonedasEntregadas(boolean monedasEntregadas) {
        this.monedasEntregadas = monedasEntregadas;
    }

    public String getMiUid() {
        return miUid;
    }

    public void setMiUid(String miUid) {
        this.miUid = miUid;
    }

    public String getMiNombre() {
        return miNombre;
    }

    public void setMiNombre(String miNombre) {
        this.miNombre = miNombre;
    }

    public int getMiPosicion() {
        return miPosicion;
    }

    public void setMiPosicion(int miPosicion) {
        this.miPosicion = miPosicion;
    }

    public int getMiTotal() {
        return miTotal;
    }

    public void setMiTotal(int miTotal) {
        this.miTotal = miTotal;
    }

    public int getMonedas() {
        return monedas;
    }

    public void setMonedas(int monedas) {
        this.monedas = monedas;
    }

    public String getGanadorUid() {
        return ganadorUid;
    }

    public void setGanadorUid(String ganadorUid) {
        this.ganadorUid = ganadorUid;
    }

    public String getGanadorNombre() {
        return ganadorNombre;
    }

    public void setGanadorNombre(String ganadorNombre) {
        this.ganadorNombre = ganadorNombre;
    }

    public int getPuntajeGanador() {
        return puntajeGanador;
    }

    public void setPuntajeGanador(int puntajeGanador) {
        this.puntajeGanador = puntajeGanador;
    }

    public ArrayList<TopJugador> getTop3() {
        return top3;
    }

    public void setTop3(ArrayList<TopJugador> top3) {
        this.top3 = top3;
    }

    public static class TopJugador {
        private String uid;
        private String nombre;
        private String avatarUrl;
        private int posicion;
        private int total;

        public TopJugador() {
        }

        public TopJugador(String uid, String nombre, String avatarUrl, int posicion, int total) {
            this.uid = uid;
            this.nombre = nombre;
            this.avatarUrl = avatarUrl;
            this.posicion = posicion;
            this.total = total;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public int getPosicion() {
            return posicion;
        }

        public void setPosicion(int posicion) {
            this.posicion = posicion;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }
}
