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
    private String partidaPresencia;
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

    private volatile boolean salida = false;
    private volatile boolean conectadoServidor = false;
    private volatile long offsetServidor = 0L;
    private volatile boolean offsetDisponible = false;
    private DatabaseReference conexionRef, offsetRef;
    private ValueEventListener listenerConexion, listenerOffset;
    private DataSnapshot ultimosJugadores;
    private final Map<String, String> nombresJugadores = new HashMap<>();
    private boolean primeraLista = true;
    private boolean modalSalirMostrado = false;
    private int ultimaAlturaIme=-1;
    private DatabaseReference sesionActual;
    private ValueEventListener listenerSesiones;
    private ValueEventListener listenerIdPartida;
    private DataSnapshot estadoSesiones;
    private final java.util.Set<String> sesionesPropias=new java.util.HashSet<>();
    private boolean registrandoSesion;
    private volatile int versionConexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null) {
            java.util.ArrayList<String> propias=savedInstanceState.getStringArrayList("sesionesPropias");
            if(propias!=null) sesionesPropias.addAll(propias);
            partidaPresencia=savedInstanceState.getString("partidaPresencia");
        }

        EdgeToEdge.enable(this);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

        setContentView(R.layout.activity_sala);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean cambioTeclado=ultimaAlturaIme!=teclado.bottom;
            ultimaAlturaIme=teclado.bottom;

            // Edge-to-edge needs IME insets to expose the scroll viewport above the keyboard.
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    Math.max(systemBars.bottom, teclado.bottom)
            );

            View chrome=findViewById(R.id.chromePartida);
            if(chrome!=null) chrome.setVisibility(teclado.bottom>0 ? View.GONE : View.VISIBLE);
            View campo = getCurrentFocus();
            if (cambioTeclado && teclado.bottom > 0 && campo instanceof android.widget.EditText) {
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
            if(partidaPresencia==null) partidaPresencia=bundle.getString("ID_PARTIDA");

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

    public boolean haSalidoDeSala() { return salida; }

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
        listenerSesiones=salaRef.child("sesiones").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot datos) { estadoSesiones=datos; sincronizarPresencia(); }
            @Override public void onCancelled(DatabaseError e) { Log.w("PRESENCIA","No se pudieron leer sesiones",e.toException()); }
        });
        listenerIdPartida=salaRef.child("idPartida").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot datos) { partidaPresencia=datos.getValue(String.class); }
            @Override public void onCancelled(DatabaseError error) { Log.w("PRESENCIA","No se pudo leer la partida",error.toException()); }
        });
        listenerConexion = conexionRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean nueva=Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if(nueva && !conectadoServidor) {
                    versionConexion++; registrandoSesion=false;
                    cerrarSesionActual();
                }
                conectadoServidor = nueva;
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

    public void reintentarConexionSala() { verificarSiSigueEnSalaYReconectar(); }

    private void verificarSiSigueEnSalaYReconectar() {
        if(salida || !conectadoServidor || !usuarioValido() || nodoJugadorSala==null) return;
        String uid=auth.getCurrentUser().getUid();
        final int recuperacion=versionConexion;
        salaRef.get().addOnSuccessListener(sala -> {
            if(salida || !conectadoServidor || recuperacion!=versionConexion || isDestroyed()) return;
            DataSnapshot jugador=sala.child("jugadores").child(uid);
            if(!jugador.exists() || !com.example.basta_game.utils.IngresoSala.reservaVigente(jugador.child("estaConectado").getValue(),jugador.child("desconectadoDesde").getValue(),sala.child("sesiones").child(uid).getValue(),offsetDisponible ? System.currentTimeMillis()+offsetServidor : Long.MIN_VALUE,TIEMPO_GRACIA_DESCONEXION_MS) || Boolean.TRUE.equals(sala.child("expulsados").child(uid).getValue())) {comprobarSalidaDeSala();return;}
            partidaPresencia=sala.child("idPartida").getValue(String.class);
            actualizarHostLocal(Boolean.TRUE.equals(jugador.child("esHost").getValue()));
            recuperarIdentidadSiIncompleta(jugador);registrarSesion();
            Fragment f=getSupportFragmentManager().findFragmentById(R.id.sala_fragment_container);
            if(f instanceof PartidaFragment) ((PartidaFragment)f).recuperarEstadoConectado();
            com.example.basta_game.utils.ConexionJuego.salaSincronizada(this);
        }).addOnFailureListener(e -> {
            if(recuperacion!=versionConexion || salida || isDestroyed()) return;
            Log.w("PRESENCIA","No se pudo recuperar la sala actual",e);
            com.example.basta_game.utils.ConexionJuego.errorSala(this,e);
        });
    }
    private void sincronizarPresencia() {
        if(salida || !conectadoServidor) return;
        salaRef.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData datos) {
                if(datos.getValue()==null) return Transaction.success(datos);
                boolean cambio=com.example.basta_game.utils.PresenciaSala.actualizar(datos);
                cambio=TransferenciaHost.reconciliar(datos) || cambio;
                return cambio ? Transaction.success(datos) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean committed,DataSnapshot datos) {
                if(e!=null) Log.w("PRESENCIA","No se pudo confirmar presencia",e.toException());
            }
        },false);
    }
    private void cerrarSesionActual() {
        if(sesionActual==null) return;
        DatabaseReference antigua=sesionActual; sesionActual=null;
        // Do not queue a fresh disconnect timestamp that would extend the grace on reconnection.
        if(!conectadoServidor) return;
        java.util.Map<String,Object> cambios=new HashMap<>();
        cambios.put("conectada",false); cambios.put("desconectadoDesde",ServerValue.TIMESTAMP);
        antigua.updateChildren(cambios).addOnFailureListener(e -> Log.w("PRESENCIA","Cierre de sesion pendiente",e));
    }
    private void registrarSesion() {
        if(sesionActual!=null || registrandoSesion || salida || !conectadoServidor || isDestroyed()) return;
        registrandoSesion=true;
        final int version=versionConexion;
        final DatabaseReference nueva=salaRef.child("sesiones").child(auth.getCurrentUser().getUid()).push();
        sesionesPropias.add(nueva.getKey());
        // Cancel only this connection's old player-level hook before switching protocols.
        nodoJugadorSala.onDisconnect().cancel().addOnCompleteListener(cancelacion -> {
            if(!cancelacion.isSuccessful() || version!=versionConexion || salida || isDestroyed() || !conectadoServidor) {if(version==versionConexion)registrandoSesion=false;return;}
            HashMap<String,Object> desconexion=new HashMap<>();
            desconexion.put("conectada",false); desconexion.put("desconectadoDesde",ServerValue.TIMESTAMP);
            nueva.onDisconnect().updateChildren(desconexion).addOnCompleteListener(registro -> {
                if(!registro.isSuccessful() || version!=versionConexion || salida || isDestroyed() || !conectadoServidor) {if(version==versionConexion)registrandoSesion=false;return;}
                final String uid=auth.getCurrentUser().getUid();
                salaRef.runTransaction(new Transaction.Handler() {
                    @Override public Transaction.Result doTransaction(MutableData datos) {
                        if(version!=versionConexion || salida || isDestroyed() || !conectadoServidor || !offsetDisponible) return Transaction.abort();
                        if(datos.getValue()==null) return Transaction.success(datos);
                        MutableData jugador=datos.child("jugadores").child(uid);
                        if(!jugador.hasChildren() || Boolean.TRUE.equals(datos.child("expulsados").child(uid).getValue()) ||
                                !com.example.basta_game.utils.IngresoSala.reservaVigente(jugador.child("estaConectado").getValue(),
                                jugador.child("desconectadoDesde").getValue(),datos.child("sesiones").child(uid).getValue(),
                                System.currentTimeMillis()+offsetServidor,TIEMPO_GRACIA_DESCONEXION_MS)) return Transaction.abort();
                        datos.child("sesiones").child(uid).child(nueva.getKey()).child("conectada").setValue(true);
                        com.example.basta_game.utils.PresenciaSala.actualizar(datos);
                        TransferenciaHost.reconciliar(datos);
                        return Transaction.success(datos);
                    }
                    @Override public void onComplete(DatabaseError error,boolean ok,DataSnapshot datos) {
                        if(version!=versionConexion || salida || isDestroyed()) return;
                        registrandoSesion=false;
                        if(error!=null) {Log.w("PRESENCIA","No se pudo anunciar sesion",error.toException());return;}
                        if(ok && datos!=null && Boolean.TRUE.equals(datos.child("sesiones").child(uid).child(nueva.getKey()).child("conectada").getValue())) sesionActual=nueva;
                        else verificarSiSigueEnSalaYReconectar();
                    }
                },false);
            });
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
                    comprobarSalidaDeSala();
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
                    if(Boolean.FALSE.equals(jugador.child("estaConectado").getValue()) && Boolean.TRUE.equals(jugador.child("estaListo").getValue())) {
                        jugadoresRef.child(jugador.getKey()).runTransaction(new Transaction.Handler() {
                            @Override public Transaction.Result doTransaction(MutableData datos) {
                                if(!Boolean.FALSE.equals(datos.child("estaConectado").getValue()) || !Boolean.TRUE.equals(datos.child("estaListo").getValue())) return Transaction.abort();
                                datos.child("estaListo").setValue(false); return Transaction.success(datos);
                            }
                            @Override public void onComplete(DatabaseError e,boolean committed,DataSnapshot datos) {
                                if(e!=null) Log.w("PRESENCIA","No se pudo limpiar preparacion desconectada",e.toException());
                            }
                        },false);
                    }
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
        salirLocalSinConexion();
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
                    if (expirar && !salida) handlerDesconexion.postDelayed(() -> programarLimpieza(), 3000);
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

    private void comprobarSalidaDeSala() {
        salaRef.child("expulsados").child(auth.getCurrentUser().getUid()).get().addOnCompleteListener(t -> {
            if(salida) return;
            boolean expulsado=t.isSuccessful() && Boolean.TRUE.equals(t.getResult().getValue());
            getIntent().putExtra("EXPULSADO",expulsado);irHome(true,false);
        });
    }
    public void salirLocalSinConexion() {
        if(salida) return;
        if(usuarioValido() && !sesionesPropias.isEmpty()) {
            java.util.Set<String> conocidas=new java.util.HashSet<>();
            if(estadoSesiones!=null) for(DataSnapshot s:estadoSesiones.child(auth.getCurrentUser().getUid()).getChildren()) conocidas.add(s.getKey());
            conocidas.addAll(sesionesPropias);
            String sesion=sesionActual!=null ? sesionActual.getKey() : sesionesPropias.iterator().next();
            com.example.basta_game.utils.SalidasPendientes.guardar(this,idSala,auth.getCurrentUser().getUid(),partidaPresencia,sesion,conocidas,sesionesPropias);
        }
        // Navigation is local; reconciliation must never hold this Activity hostage.
        salida=true; versionConexion++;
        com.example.basta_game.utils.Sounds.detenerTodo();
        if(com.example.basta_game.utils.ConexionJuego.transporteDisponible()) com.example.basta_game.utils.SalidasPendientes.reconciliar(getApplicationContext());
        irHome(false,false);
    }
    private void irHome(boolean desconexion, boolean creadorDesconexion) {
        salida = true;
        cerrarSesionActual();
        cancelarTiempoDeGracia();
        if (conectadoServidor && nodoJugadorSala != null) {
            nodoJugadorSala.onDisconnect().cancel().addOnFailureListener(e -> Log.w("PRESENCIA", "Cancelacion pendiente", e));
            salaRef.child("minijuegoActual").onDisconnect().cancel();
        }
        limpiarListeners();

        Intent intent = new Intent(SalaActivity.this, HomeActivity.class);
        intent.putExtra("DESCONEXION", desconexion);
        intent.putExtra("MOTIVO_SALIDA", desconexion ? "Tu lugar en la sala ya no esta disponible" : (!com.example.basta_game.utils.ConexionJuego.transporteDisponible() ? "Saliste de la partida. Confirmaremos tu salida al reconectar" : "Abandonaste la sala"));
        if(getIntent().getBooleanExtra("EXPULSADO",false)) intent.putExtra("MOTIVO_SALIDA","Has sido expulsado de la sala");
        intent.putExtra("CREADOR_DESCONEXION", creadorDesconexion);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void limpiarListeners() {
        if (conexionRef != null && listenerConexion != null) conexionRef.removeEventListener(listenerConexion);
        if (offsetRef != null && listenerOffset != null) offsetRef.removeEventListener(listenerOffset);
        if(listenerSesiones!=null) salaRef.child("sesiones").removeEventListener(listenerSesiones);
        if(listenerIdPartida!=null) salaRef.child("idPartida").removeEventListener(listenerIdPartida);
        listenerIdPartida=null;
        listenerSesiones=null;
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
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        estado.putStringArrayList("sesionesPropias",new java.util.ArrayList<>(sesionesPropias));
        estado.putString("partidaPresencia",partidaPresencia);
        super.onSaveInstanceState(estado);
    }

    @Override
    protected void onDestroy() {
        cerrarSesionActual();
        super.onDestroy();

        cancelarTiempoDeGracia();
        limpiarListeners();
    }
}
