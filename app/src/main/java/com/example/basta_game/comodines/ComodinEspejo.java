package com.example.basta_game.comodines;

import androidx.annotation.NonNull;
import com.example.basta_game.utils.ConexionJuego;
import com.example.basta_game.utils.PlazoRonda;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import java.util.UUID;

/** Invierte las respuestas de hasta tres categorias durante 30 s; el rival debe corregirlas. */
public class ComodinEspejo extends BaseComodin {
    public static final String ID = "inversion_letras";
    private static final long DURACION_MS = 30000L;
    private final String idPartida, uidVictima, nombreVictima;
    public ComodinEspejo(String sala, String partida, int ronda, String uidVictima, String nombreVictima) {
        super(sala, ronda);
        this.idPartida=partida;
        this.uidVictima=uidVictima;
        this.nombreVictima=nombreVictima;
    }
    private boolean valido(DataSnapshot sala, long ahora) {
        if (idPartida == null || uidVictima == null || miUid.equals(uidVictima) ||
                !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                !"jugando".equals(sala.child("fase").getValue(String.class)) ||
                Boolean.TRUE.equals(sala.child("bastaPresionado").getValue(Boolean.class)) ||
                !sala.child("jugadores").hasChild(miUid) || !sala.child("jugadores").hasChild(uidVictima) ||
                sala.child("respuestas/ronda_"+rondaActual).hasChild(miUid)) return false;
        Object ronda = sala.child("rondaActual").getValue(), inicio = sala.child("inicioRonda").getValue();
        Object tiempo = sala.child("tiempoRonda").getValue();
        if (!(ronda instanceof Number) || ((Number)ronda).intValue()!=rondaActual ||
                !(inicio instanceof Number) || !(tiempo instanceof Number)) return false;
        long comienzo = PlazoRonda.inicioRespuestas(((Number)inicio).longValue(),rondaActual,
                com.example.basta_game.utils.RelojRevision.numero(
                    sala.child("preparacionInicialMs").getValue(),0));
        long fin = PlazoRonda.calcularFin(comienzo,((Number)tiempo).intValue(),null);
        return ahora < fin-DURACION_MS-500;
    }
    @Override public void activar(String atacanteNombre, ComodinCallback callback) {
        if (!ConexionJuego.disponible()) {callback.onError("Necesitas conexión para atacar");return;}
        final long version = ConexionJuego.version();
        final long ahora = ConexionJuego.ahoraServidor();
        salaRef.get().addOnCompleteListener(lectura -> {
            if (!lectura.isSuccessful() || lectura.getResult()==null ||
                    !ConexionJuego.vigente(version) || !valido(lectura.getResult(), ahora)) {
                callback.onError("El Espejo ya no puede activarse en esta ronda");return;
            }
            final DatabaseReference unidades = usuarioRef.child("inventarioComodines").child(ID).child("cantidad");
            com.example.basta_game.utils.ConsumoInventario.descontar(unidades, (descontado, mensaje) -> {
                if (!descontado) { callback.onError(mensaje); return; }
                    final long ahoraActivacion = ConexionJuego.ahoraServidor();
                    final String idUso=UUID.randomUUID().toString();
                    salaRef.runTransaction(new Transaction.Handler() {
                        @Override public Transaction.Result doTransaction(MutableData sala) {
                            if (sala.getValue()==null ||
                                    !idPartida.equals(sala.child("idPartida").getValue(String.class)) ||
                                    !"jugando".equals(sala.child("fase").getValue(String.class)) ||
                                    Boolean.TRUE.equals(sala.child("bastaPresionado").getValue(Boolean.class)) ||
                                    !sala.child("jugadores").hasChild(miUid) || !sala.child("jugadores").hasChild(uidVictima) ||
                                    sala.child("respuestas/ronda_"+rondaActual).hasChild(miUid) ||
                                    idPartida.equals(sala.child("efectosVisuales/espejo/ronda_"+rondaActual+"/"+uidVictima+"/idPartida").getValue())) return Transaction.abort();
                            Object n=sala.child("rondaActual").getValue(),tiempo=sala.child("tiempoRonda").getValue(),inicio=sala.child("inicioRonda").getValue();
                            if (!(n instanceof Number) || ((Number)n).intValue()!=rondaActual ||
                                    !(tiempo instanceof Number) || !(inicio instanceof Number)) return Transaction.abort();
                            long comienzo=PlazoRonda.inicioRespuestas(((Number)inicio).longValue(),rondaActual,
                                    com.example.basta_game.utils.RelojRevision.numero(
                                        sala.child("preparacionInicialMs").getValue(),0));
                            if (ahoraActivacion >= PlazoRonda.calcularFin(comienzo,((Number)tiempo).intValue(),null)-DURACION_MS-500)
                                return Transaction.abort();
                            if (com.example.basta_game.utils.DefensaEscudoDorado.registrarBloqueo(
                                    sala, idPartida, rondaActual, uidVictima, miUid,
                                    atacanteNombre, nombreVictima, idUso, ID)) {
                                return Transaction.success(sala);
                            }
                            String ruta="efectosVisuales/espejo/ronda_"+rondaActual+"/"+uidVictima;
                            sala.child(ruta+"/idUso").setValue(idUso);
                            sala.child(ruta+"/idPartida").setValue(idPartida);
                            sala.child(ruta+"/uidAtacante").setValue(miUid);
                            sala.child(ruta+"/atacanteNombre").setValue(atacanteNombre);
                            sala.child(ruta+"/victimaUid").setValue(uidVictima);
                            sala.child(ruta+"/victimaNombre").setValue(nombreVictima);
                            sala.child(ruta+"/finMs").setValue(ahoraActivacion+DURACION_MS);
                            return Transaction.success(sala);
                        }
                        @Override public void onComplete(DatabaseError error2, boolean ok2, DataSnapshot dato) {
                            if (error2==null && ok2) {callback.onSuccess("ESPEJO");return;}
                            unidades.runTransaction(new Transaction.Handler() {
                                @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData dato) {
                                    Object cantidad = dato.getValue();
                                    if (!(cantidad instanceof Number)) return Transaction.abort();
                                    dato.setValue(((Number)cantidad).longValue()+1);
                                    return Transaction.success(dato);
                                }
                                @Override public void onComplete(DatabaseError e, boolean devuelto, DataSnapshot snap) {
                                    callback.onError(devuelto ? "No se activó El Espejo; recuperaste tu unidad" :
                                            "No se pudo confirmar El Espejo. Comprueba tu inventario");
                                }
                            },false);
                        }
                    },false);
            });
        });
    }
}
