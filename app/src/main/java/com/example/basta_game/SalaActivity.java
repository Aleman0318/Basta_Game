package com.example.basta_game;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.basta_game.Fragments.LobbyFragment;
import com.example.basta_game.Fragments.PartidaFragment;
import com.example.basta_game.utils.TransferenciaHost;
import com.example.basta_game.utils.IdentidadJugador;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.android.gms.tasks.Tasks;
import android.view.View;
import android.widget.Toast;
import com.example.basta_game.utils.CustomDialogHelper;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SalaActivity extends AppCompatActivity {

    private static final long TIEMPO_GRACIA_DESCONEXION_MS = 90000;

    private FirebaseAuth auth;

    private String idSala = "";
    private String codigoSala = "";
    private boolean esHost = false;

    private Bundle datosSalaOriginal;

    private DatabaseReference salaRef;
    private DatabaseReference jugadoresRef;
    private DatabaseReference nodoJugadorSala;

    private ValueEventListener listenerMiJugador;
    private ValueEventListener listenerJugadoresSala;

    private final Handler handlerDesconexion = new Handler(Looper.getMainLooper());
    private Runnable tareaExpulsionPorAusencia;

    private boolean salida = false;
    private boolean conectadoServidor = false;
    private long offsetServidor = 0L;
    private boolean offsetDisponible = false;
    private DatabaseReference conexionRef, offsetRef;
    private ValueEventListener listenerConexion, listenerOffset;
    private DataSnapshot ultimosJugadores;
    private final Map<String, String> nombresJugadores = new HashMap<>();
    private boolean primeraLista = true;
    private boolean modalSalirMostrado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

        setContentView(R.layout.activity_sala);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());

            // Edge-to-edge needs IME insets to expose the scroll viewport above the keyboard.
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    Math.max(systemBars.bottom, teclado.bottom)
            );

            View campo = getCurrentFocus();
            if (teclado.bottom > 0 && campo instanceof android.widget.EditText) {
                campo.post(() -> {
                    if (!campo.hasFocus() || !campo.isAttachedToWindow()) return;
                    int margen = Math.round(12 * getResources().getDisplayMetrics().density);
                    campo.requestRectangleOnScreen(new android.graphics.Rect(0, -margen,
                            campo.getWidth(), campo.getHeight() + margen), false);
                });
            }

            return insets;
        });

        auth = FirebaseAuth.getInstance();

        Bundle bundle = getIntent().getBundleExtra("DATOS_SALA");

        if (bundle != null) {
            datosSalaOriginal = new Bundle(bundle);

            idSala = bundle.getString("IDSALA", "");
            codigoSala = bundle.getString("CODIGO_SALA", "");
            esHost = bundle.getBoolean("ES_ANFITRION", false);

            configurarReferenciasFirebase();
            if (getSupportFragmentManager().findFragmentById(R.id.sala_fragment_container) == null) {
                mostrarLobby(crearBundleActualizado());
            }
            configurarBotonAtrasTelefono();
            escucharMiNodoJugador();
            escucharJugadoresParaLimpieza();
            escucharConexion();
        }
    }

    private void configurarReferenciasFirebase() {
        if (idSala == null || idSala.trim().isEmpty()) {
            return;
        }

        salaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        jugadoresRef = salaRef.child("jugadores");

        if (VerificacionesAuth.VerificarInicioSesion(auth)) {
            String uid = auth.getCurrentUser().getUid();
            nodoJugadorSala = jugadoresRef.child(uid);
        }
    }

    public void mostrarLobby(Bundle datosSala) {
        LobbyFragment lobby = new LobbyFragment();
        lobby.setArguments(datosSala);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.sala_fragment_container, lobby)
                .commitAllowingStateLoss();
    }

    private Bundle crearBundleActualizado() {
        Bundle bundle;

        if (datosSalaOriginal != null) {
            bundle = new Bundle(datosSalaOriginal);
        } else {
            bundle = new Bundle();
        }

        bundle.putString("IDSALA", idSala);
        bundle.putString("CODIGO_SALA", codigoSala);
        bundle.putBoolean("ES_ANFITRION", esHost);

        return bundle;
    }

    private void configurarBotonAtrasTelefono() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoAbandonarSala();
            }
        });
    }

    private void mostrarDialogoAbandonarSala() {
        if (modalSalirMostrado || salida) {
            return;
        }

        modalSalirMostrado = true;

        CustomDialogHelper.show(
                this,
                "¿Deseas salir?",
                "Si sales de la sala abandonarás la partida actual.",
                true,
                new CustomDialogHelper.DialogButton(
                        "No",
                        Color.parseColor("#888888"),
                        Color.WHITE,
                        true,
                        () -> modalSalirMostrado = false
                ),
                new CustomDialogHelper.DialogButton(
                        "Sí, salir",
                        Color.parseColor("#E53935"),
                        Color.WHITE,
                        true,
                        () -> {
                            modalSalirMostrado = false;
                            mandarHomePorDesconexion();
                        }
                )
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!salida && conectadoServidor) verificarSiSigueEnSalaYReconectar();
    }

    private void escucharConexion() {
        conexionRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        listenerOffset = offsetRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long offset = snapshot.getValue(Long.class);
                offsetDisponible = offset != null;
                offsetServidor = offset == null ? 0L : offset;
                programarLimpieza();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PRESENCIA", error.getMessage());
            }
        });
        listenerConexion = conexionRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                conectadoServidor = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (conectadoServidor && !salida) verificarSiSigueEnSalaYReconectar();
                programarLimpieza();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                conectadoServidor = false;
                cancelarTiempoDeGracia();
            }
        });
    }

    private void programarLimpieza() {
        cancelarTiempoDeGracia();
        if (salida || !conectadoServidor || !offsetDisponible || ultimosJugadores == null) return;
        long ahora = System.currentTimeMillis() + offsetServidor;
        long proxima = Long.MAX_VALUE;
        for (DataSnapshot jugador : ultimosJugadores.getChildren()) {
            Long desde = jugador.child("desconectadoDesde").getValue(Long.class);
            if (Boolean.FALSE.equals(jugador.child("estaConectado").getValue(Boolean.class)) && desde != null) {
                proxima = Math.min(proxima, Math.max(1L, desde + TIEMPO_GRACIA_DESCONEXION_MS - ahora));
            }
        }
        if (proxima != Long.MAX_VALUE) {
            tareaExpulsionPorAusencia = () -> actualizarSala(null, true, null);
            handlerDesconexion.postDelayed(tareaExpulsionPorAusencia, proxima);
        }
    }

    private void cancelarTiempoDeGracia() {
        if (tareaExpulsionPorAusencia != null) {
            handlerDesconexion.removeCallbacks(tareaExpulsionPorAusencia);
            tareaExpulsionPorAusencia = null;
        }
    }

    private void verificarSiSigueEnSalaYReconectar() {
        if (salida || !conectadoServidor || !usuarioValido() || nodoJugadorSala == null) return;
        nodoJugadorSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (salida || !conectadoServidor) return;
                if (!snapshot.exists()) {
                    irHome(true, false);
                    return;
                }
                actualizarHostLocal(Boolean.TRUE.equals(snapshot.child("esHost").getValue(Boolean.class)));
                recuperarIdentidadSiIncompleta(snapshot);
                HashMap<String, Object> desconexion = new HashMap<>();
                desconexion.put("estaConectado", false);
                desconexion.put("desconectadoDesde", ServerValue.TIMESTAMP);
                // Registrar en el servidor antes de anunciar presencia; nunca restaurar esHost.
                nodoJugadorSala.onDisconnect().updateChildren(desconexion).addOnCompleteListener(registro -> {
                    if (!registro.isSuccessful() || salida || !conectadoServidor) return;
                    nodoJugadorSala.runTransaction(new Transaction.Handler() {
                        @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData jugador) {
                            if (jugador.getValue() != null) {
                                jugador.child("estaConectado").setValue(true);
                                jugador.child("desconectadoDesde").setValue(null);
                            }
                            return Transaction.success(jugador);
                        }
                        @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot jugador) {
                            if (error != null) Log.e("PRESENCIA", error.getMessage());
                            else if (!salida && conectadoServidor) actualizarSala(null, false, null);
                        }
                    }, false);
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PRESENCIA", error.getMessage());
            }
        });
    }

    private void escucharMiNodoJugador() {
        if (!usuarioValido() || nodoJugadorSala == null) {
            return;
        }

        listenerMiJugador = nodoJugadorSala.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (salida) {
                    return;
                }

                if (!snapshot.exists()) {
                    if (!conectadoServidor) return;
                    Log.d("SALA_ACTIVITY", "Mi nodo jugador desapareció");
                    irHome(true, false);
                    return;
                }

                Boolean hostFirebase = snapshot.child("esHost").getValue(Boolean.class);
                actualizarHostLocal(Boolean.TRUE.equals(hostFirebase));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("SALA_ACTIVITY", "Error escuchando jugador: " + error.getMessage());
            }
        });
    }

    private void recuperarIdentidadSiIncompleta(DataSnapshot jugador) {
        if (IdentidadJugador.nombreValido(jugador.child("nombre").getValue(String.class)) &&
                jugador.child("urlAvatar").getValue(String.class) != null) return;
        if (!usuarioValido()) return;
        final String uid = auth.getCurrentUser().getUid();
        if (!IdentidadJugador.nombreValido(jugador.child("nombre").getValue(String.class))) {
            notificarSalida("Cargando perfil...");
        }
        FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot perfil) {
                        if (salida || !usuarioValido() || !uid.equals(auth.getCurrentUser().getUid())) return;
                        String nombre = perfil.child("nombre").getValue(String.class);
                        String foto = perfil.child("fotoPerfilUrl").getValue(String.class);
                        if (!IdentidadJugador.nombreValido(nombre)) {
                            notificarSalida("Perfil pendiente. Reintenta al recuperar la conexion");
                            return;
                        }
                        nodoJugadorSala.runTransaction(new Transaction.Handler() {
                            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData actual) {
                                // An expired membership must never be recreated by a late profile callback.
                                if (actual.getValue() == null) return Transaction.success(actual);
                                IdentidadJugador.aplicar(actual, uid, nombre, foto);
                                return Transaction.success(actual);
                            }
                            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot actual) {
                                if (error != null) Log.e("PERFIL_SALA", error.getMessage());
                            }
                        }, false);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        if (!salida) notificarSalida("No se pudo cargar el perfil. Revisa tu conexion");
                    }
                });
    }

    private void escucharJugadoresParaLimpieza() {
        if (jugadoresRef == null) return;
        listenerJugadoresSala = jugadoresRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (salida) return;
                Map<String, String> actuales = new HashMap<>();
                int coronas = 0;
                for (DataSnapshot jugador : snapshot.getChildren()) {
                    actuales.put(jugador.getKey(), leerNombreJugador(jugador));
                    if (Boolean.TRUE.equals(jugador.child("esHost").getValue(Boolean.class))) coronas++;
                }
                if (!primeraLista) {
                    for (DataSnapshot jugador : snapshot.getChildren()) {
                        if (Boolean.FALSE.equals(jugador.child("estaConectado").getValue(Boolean.class)) &&
                                !Boolean.FALSE.equals(ultimosJugadores.child(jugador.getKey())
                                        .child("estaConectado").getValue(Boolean.class))) {
                            notificarSalida(leerNombreJugador(jugador) + " esta reconectando. Conserva su lugar");
                        }
                    }
                    for (String uid : nombresJugadores.keySet()) {
                        if (!actuales.containsKey(uid)) {
                            DataSnapshot anterior = ultimosJugadores.child(uid);
                            boolean temporal = Boolean.FALSE.equals(anterior.child("estaConectado").getValue(Boolean.class));
                            notificarSalida(nombresJugadores.get(uid) + (temporal
                                    ? " se ha desconectado de la partida" : " ha abandonado la partida"));
                        }
                    }
                }
                primeraLista = false;
                nombresJugadores.clear();
                nombresJugadores.putAll(actuales);
                ultimosJugadores = snapshot;
                if (conectadoServidor && (coronas != 1 || snapshot.getChildrenCount() == 1)) {
                    actualizarSala(null, false, null);
                }
                programarLimpieza();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SALA_ACTIVITY", error.getMessage());
            }
        });
    }

    private void notificarSalida(String mensaje) {
        Log.i("SALIDA_SALA", mensaje);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.sala_fragment_container);
        View aviso = fragment == null || fragment.getView() == null ? null
                : fragment.getView().findViewById(R.id.notificationPartida);
        if (aviso == null && fragment != null && fragment.getView() != null) {
            aviso = fragment.getView().findViewById(R.id.notificationLobby);
        }
        if (aviso != null) NotificationHelper.show(aviso, mensaje, NotificationHelper.WARNING);
        else Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        Sounds.playNotificaciones();
    }

    private void actualizarHostLocal(boolean nuevoEsHost) {
        esHost = nuevoEsHost;
        if (datosSalaOriginal != null) datosSalaOriginal.putBoolean("ES_ANFITRION", esHost);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.sala_fragment_container);
        if (fragment != null && fragment.getArguments() != null) {
            fragment.getArguments().putBoolean("ES_ANFITRION", esHost);
        }
        if (fragment instanceof PartidaFragment) ((PartidaFragment) fragment).actualizarAnfitrion(esHost);
        if (fragment instanceof LobbyFragment) ((LobbyFragment) fragment).actualizarAnfitrion(esHost);
    }

    // Nombre publico conservado para los botones existentes; es un abandono explicito.
    public void mandarHomePorDesconexion() {
        if (salida || !usuarioValido() || nodoJugadorSala == null) return;
        if (!conectadoServidor) {
            notificarSalida("Se necesita conexion para confirmar el abandono");
            return;
        }
        salida = true;
        cancelarTiempoDeGracia();
        actualizarHostLocal(false);
        Tasks.whenAll(nodoJugadorSala.onDisconnect().cancel(),
                salaRef.child("minijuegoActual").onDisconnect().cancel()).addOnCompleteListener(cancelacion -> {
            if (!cancelacion.isSuccessful()) {
                salida = false;
                notificarSalida("No se pudo confirmar la salida. Reintenta al conectar");
                verificarSiSigueEnSalaYReconectar();
                return;
            }
            actualizarSala(auth.getCurrentUser().getUid(), false, () -> irHome(false, false));
        });
    }

    private void actualizarSala(String uidSaliente, boolean expirar, Runnable despues) {
        if (!conectadoServidor || salaRef == null || !usuarioValido()) {
            if (uidSaliente != null) {
                salida = false;
                notificarSalida("La salida no se confirmo: vuelve a conectar y reintenta");
            }
            return;
        }
        if (expirar && !offsetDisponible) return;
        final long ahoraServidor = System.currentTimeMillis() + offsetServidor;
        salaRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                if (sala.getValue() == null) return Transaction.success(sala);
                boolean cambio;
                if (uidSaliente != null) cambio = TransferenciaHost.abandonar(sala, uidSaliente);
                else {
                    cambio = expirar && TransferenciaHost.expirar(sala, ahoraServidor, TIEMPO_GRACIA_DESCONEXION_MS);
                    cambio = TransferenciaHost.reconciliar(sala) || cambio;
                }
                return cambio ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot sala) {
                if (error != null) {
                    Log.e("HOST", error.getMessage());
                    if (uidSaliente != null) {
                        salida = false;
                        notificarSalida("No se pudo confirmar la salida. Reintenta");
                        verificarSiSigueEnSalaYReconectar();
                    }
                    return;
                }
                if (sala != null && !sala.exists()) limpiarCodigoSinSala();
                if (uidSaliente != null && (committed || (sala != null && !sala.child("jugadores").hasChild(uidSaliente)))) {
                    Log.i("SALIDA_SALA", "Abandono confirmado: " + uidSaliente);
                    if (despues != null) despues.run();
                }
            }
        }, false);
    }

    private void limpiarCodigoSinSala() {
        if (codigoSala == null || codigoSala.trim().isEmpty()) return;
        FirebaseDatabase.getInstance().getReference("codigosSalas").child(codigoSala)
                .runTransaction(new Transaction.Handler() {
                    @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData codigo) {
                        if (codigo.getValue() == null) return Transaction.success(codigo);
                        if (!idSala.equals(codigo.child("idSala").getValue(String.class))) return Transaction.abort();
                        codigo.setValue(null);
                        return Transaction.success(codigo);
                    }
                    @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot codigo) {
                        if (error != null) Log.e("SALIDA_SALA", "Indice pendiente: " + error.getMessage());
                    }
                }, false);
    }

    private String leerNombreJugador(DataSnapshot jugador) {
        String nombre = jugador.child("nombre").getValue(String.class);
        return nombre == null || nombre.trim().isEmpty() ? "Jugador" : nombre;
    }

    private boolean usuarioValido() {
        return auth != null &&
                VerificacionesAuth.VerificarInicioSesion(auth) &&
                auth.getCurrentUser() != null;
    }

    private void irHome(boolean desconexion, boolean creadorDesconexion) {
        salida = true;
        cancelarTiempoDeGracia();
        if (conectadoServidor && nodoJugadorSala != null) {
            nodoJugadorSala.onDisconnect().cancel().addOnFailureListener(e -> Log.w("PRESENCIA", "Cancelacion pendiente", e));
            salaRef.child("minijuegoActual").onDisconnect().cancel();
        }
        limpiarListeners();

        Intent intent = new Intent(SalaActivity.this, HomeActivity.class);
        intent.putExtra("DESCONEXION", desconexion);
        intent.putExtra("CREADOR_DESCONEXION", creadorDesconexion);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void limpiarListeners() {
        if (conexionRef != null && listenerConexion != null) conexionRef.removeEventListener(listenerConexion);
        if (offsetRef != null && listenerOffset != null) offsetRef.removeEventListener(listenerOffset);
        listenerConexion = null;
        listenerOffset = null;
        if (nodoJugadorSala != null && listenerMiJugador != null) {
            nodoJugadorSala.removeEventListener(listenerMiJugador);
            listenerMiJugador = null;
        }

        if (jugadoresRef != null && listenerJugadoresSala != null) {
            jugadoresRef.removeEventListener(listenerJugadoresSala);
            listenerJugadoresSala = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cancelarTiempoDeGracia();
        limpiarListeners();
    }
}
