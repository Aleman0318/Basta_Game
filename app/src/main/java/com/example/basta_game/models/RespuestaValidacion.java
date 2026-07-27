package com.example.basta_game.models;

public class RespuestaValidacion {

    private String uidJugador;
    private String nombreJugador;
    private String respuesta;
    private String fotoPerfilUrl;

    private boolean validada = false;
    private boolean correcta = false;
    private int puntos = 0;
    private String categoria;

    private boolean timerIniciado = false;
    private boolean mostrarPuntaje = false;

    private String claveVotacion;
    private boolean votacionResuelta = false;
    private String resultadoVotacion;

    public RespuestaValidacion(String uidJugador, String nombreJugador, String respuesta, String fotoPerfilUrl) {
        this.uidJugador = uidJugador;
        this.nombreJugador = nombreJugador;
        this.respuesta = respuesta;
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getUidJugador() { return uidJugador; }
    public String getNombreJugador() { return nombreJugador; }
    public String getRespuesta() { return respuesta; }
    public String getFotoPerfilUrl() { return fotoPerfilUrl; }

    public boolean isValidada() { return validada; }
    public void setValidada(boolean validada) { this.validada = validada; }

    public boolean isCorrecta() { return correcta; }
    public void setCorrecta(boolean correcta) { this.correcta = correcta; }

    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }

    public boolean isTimerIniciado() { return timerIniciado; }
    public void setTimerIniciado(boolean timerIniciado) { this.timerIniciado = timerIniciado; }

    public boolean isMostrarPuntaje() { return mostrarPuntaje; }
    public void setMostrarPuntaje(boolean mostrarPuntaje) { this.mostrarPuntaje = mostrarPuntaje; }

    public String getClaveVotacion() { return claveVotacion; }
    public void setClaveVotacion(String claveVotacion) { this.claveVotacion = claveVotacion; }

    public boolean isVotacionResuelta() { return votacionResuelta; }
    public void setVotacionResuelta(boolean votacionResuelta) { this.votacionResuelta = votacionResuelta; }

    public String getResultadoVotacion() { return resultadoVotacion; }
    public void setResultadoVotacion(String resultadoVotacion) { this.resultadoVotacion = resultadoVotacion; }
}
