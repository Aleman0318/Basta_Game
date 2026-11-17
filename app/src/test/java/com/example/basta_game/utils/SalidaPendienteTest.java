package com.example.basta_game.utils;
import org.junit.Test;
import static org.junit.Assert.*;
import com.google.firebase.database.*;
public class SalidaPendienteTest {
 @Test public void ticketNoRetiraPartidaNuevaNiOtraSesion(){
  MutableData s=MutableDataLocal.desde(new java.util.HashMap<>());s.child("idPartida").setValue("p");s.child("jugadores/u/nombre").setValue("Jugador");s.child("sesiones/u/s/conectada").setValue(false);
  assertFalse(TransferenciaHost.abandonarSesion(s,"u","otra","s"));s.child("sesiones/u/nueva/conectada").setValue(true);assertFalse(TransferenciaHost.abandonarSesion(s,"u","p","s"));assertTrue(s.child("jugadores").hasChild("u"));
  s.child("sesiones/u/nueva").setValue(null);assertTrue(TransferenciaHost.abandonarSesion(s,"u","p","s"));assertFalse(TransferenciaHost.abandonarSesion(s,"u","p","s"));
 }
}
