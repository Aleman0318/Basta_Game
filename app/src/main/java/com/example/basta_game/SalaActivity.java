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
import com.example.basta_game.utils.CustomDialogHelper;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SalaActivity extends AppCompatActivity {

    private static final long TIEMPO_GRACIA_DESCONEXION_MS = 10000;

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
    private boolean expulsadoPorTiempo = false;
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

            // No usamos el alto del teclado como padding del contenedor principal.
            // Si se suma el IME aquí, el fragment puede quedar raro y el scroll manual se vuelve incómodo.
            // Con adjustResize + el ScrollView del fragment, la pantalla se comporta tipo WhatsApp.
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

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
            mostrarLobby(crearBundleActualizado());
            configurarBotonAtrasTelefono();
            escucharMiNodoJugador();
            escucharJugadoresParaLimpieza();
            marcarJugadorConectado();
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
    protected void onPause() {
        super.onPause();

        if (salida || isFinishing() || isChangingConfigurations()) {
            return;
        }

        Log.d("SALA_ACTIVITY", "onPause: usuario salió temporalmente");

        marcarJugadorDesconectadoTemporalmente();
        iniciarTiempoDeGracia();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (expulsadoPorTiempo) {
            irHome(true, false);
            return;
        }

        if (salida) {
            return;
        }

        Log.d("SALA_ACTIVITY", "onResume: usuario volvió");

        cancelarTiempoDeGracia();
        verificarSiSigueEnSalaYReconectar();
    }

    private void iniciarTiempoDeGracia() {
        cancelarTiempoDeGracia();

        tareaExpulsionPorAusencia = () -> {
            if (salida || isFinishing()) {
                return;
            }

            Log.d("SALA_ACTIVITY", "Pasaron 10 segundos. Expulsando jugador por ausencia.");

            expulsadoPorTiempo = true;
            salida = true;

            abandonarSalaEnFirebase(() -> {
                Log.d("SALA_ACTIVITY", "Jugador expulsado por ausencia");
            });
        };

        handlerDesconexion.postDelayed(
                tareaExpulsionPorAusencia,
                TIEMPO_GRACIA_DESCONEXION_MS
        );
    }

    private void cancelarTiempoDeGracia() {
        if (tareaExpulsionPorAusencia != null) {
            handlerDesconexion.removeCallbacks(tareaExpulsionPorAusencia);
            tareaExpulsionPorAusencia = null;
        }
    }

    private void marcarJugadorConectado() {
        if (!usuarioValido() || nodoJugadorSala == null) {
            return;
        }

        HashMap<String, Object> actualizaciones = new HashMap<>();
        actualizaciones.put("estaConectado", true);
        actualizaciones.put("desconectadoDesde", null);

        nodoJugadorSala.updateChildren(actualizaciones)
                .addOnSuccessListener(unused -> Log.d("SALA_ACTIVITY", "Jugador conectado"));
    }

    private void marcarJugadorDesconectadoTemporalmente() {
        if (!usuarioValido() || nodoJugadorSala == null) {
            return;
        }

        HashMap<String, Object> actualizaciones = new HashMap<>();
        actualizaciones.put("estaConectado", false);
        actualizaciones.put("desconectadoDesde", ServerValue.TIMESTAMP);

        nodoJugadorSala.updateChildren(actualizaciones)
                .addOnSuccessListener(unused -> Log.d("SALA_ACTIVITY", "Jugador desconectado temporalmente"));
    }

    private void verificarSiSigueEnSalaYReconectar() {
        if (!usuarioValido() || nodoJugadorSala == null) {
            irHome(true, false);
            return;
        }

        nodoJugadorSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    irHome(true, false);
                    return;
                }

                marcarJugadorConectado();

                Boolean hostFirebase = snapshot.child("esHost").getValue(Boolean.class);
                actualizarHostLocal(Boolean.TRUE.equals(hostFirebase));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("SALA_ACTIVITY", "Error reconectando: " + error.getMessage());
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

    private void escucharJugadoresParaLimpieza() {
        if (jugadoresRef == null) {
            return;
        }

        listenerJugadoresSala = jugadoresRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (salida) {
                    return;
                }

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    eliminarSalaCompleta(() -> irHome(true, false));
                    return;
                }

                asignarHostSiLaSalaNoTiene(snapshot);
                limpiarJugadoresDesconectadosVencidos(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("SALA_ACTIVITY", "Error escuchando jugadores: " + error.getMessage());
            }
        });
    }

    private void actualizarHostLocal(boolean nuevoEsHost) {
        if (esHost == nuevoEsHost) {
            return;
        }

        esHost = nuevoEsHost;

        if (datosSalaOriginal != null) {
            datosSalaOriginal.putBoolean("ES_ANFITRION", esHost);
        }

        actualizarHostEnFragmentActual(esHost);

        Fragment fragmentActual = getSupportFragmentManager()
                .findFragmentById(R.id.sala_fragment_container);

        if (fragmentActual instanceof LobbyFragment && esHost) {
            mostrarLobby(crearBundleActualizado());
        }

        Log.d("SALA_ACTIVITY", "Host actualizado localmente: " + esHost);
    }

    private void actualizarHostEnFragmentActual(boolean nuevoEsHost) {
        Fragment fragmentActual = getSupportFragmentManager()
                .findFragmentById(R.id.sala_fragment_container);

        if (fragmentActual == null) {
            return;
        }

        try {
            Field campoHost = fragmentActual.getClass().getDeclaredField("esAnfitrion");
            campoHost.setAccessible(true);
            campoHost.setBoolean(fragmentActual, nuevoEsHost);
        } catch (Exception ignored) {

        }
    }

    private void asignarHostSiLaSalaNoTiene(DataSnapshot snapshotJugadores) {
        if (!soyResponsableDeLimpieza(snapshotJugadores)) {
            return;
        }

        boolean hayHost = false;

        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
            Boolean host = jugadorSnap.child("esHost").getValue(Boolean.class);

            if (Boolean.TRUE.equals(host)) {
                hayHost = true;
                break;
            }
        }

        if (hayHost) {
            return;
        }

        ArrayList<DataSnapshot> jugadoresDisponibles = obtenerJugadoresDisponibles(snapshotJugadores);

        if (jugadoresDisponibles.isEmpty()) {
            eliminarSalaCompleta(null);
            return;
        }

        DataSnapshot nuevoHostSnap = jugadoresDisponibles.get(
                new Random().nextInt(jugadoresDisponibles.size())
        );

        String nuevoHostUid = nuevoHostSnap.getKey();

        if (nuevoHostUid == null) {
            return;
        }

        asignarNuevoHost(snapshotJugadores, nuevoHostUid);
    }

    private void limpiarJugadoresDesconectadosVencidos(DataSnapshot snapshotJugadores) {
        if (!soyResponsableDeLimpieza(snapshotJugadores)) {
            return;
        }

        long ahora = System.currentTimeMillis();

        ArrayList<String> jugadoresExpirados = new ArrayList<>();

        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
            String uid = jugadorSnap.getKey();

            if (uid == null) {
                continue;
            }

            Boolean conectado = jugadorSnap.child("estaConectado").getValue(Boolean.class);
            Long desconectadoDesde = jugadorSnap.child("desconectadoDesde").getValue(Long.class);

            if (Boolean.FALSE.equals(conectado) &&
                    desconectadoDesde != null &&
                    ahora - desconectadoDesde >= TIEMPO_GRACIA_DESCONEXION_MS) {

                jugadoresExpirados.add(uid);
            }
        }

        if (jugadoresExpirados.isEmpty()) {
            return;
        }

        removerJugadoresExpirados(snapshotJugadores, jugadoresExpirados);
    }

    private void removerJugadoresExpirados(
            DataSnapshot snapshotJugadores,
            ArrayList<String> jugadoresExpirados
    ) {
        if (salaRef == null) {
            return;
        }

        HashMap<String, Object> actualizaciones = new HashMap<>();

        boolean hostFueExpulsado = false;
        ArrayList<DataSnapshot> jugadoresRestantes = new ArrayList<>();

        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
            String uid = jugadorSnap.getKey();

            if (uid == null) {
                continue;
            }

            if (jugadoresExpirados.contains(uid)) {
                actualizaciones.put("jugadores/" + uid, null);

                Boolean eraHost = jugadorSnap.child("esHost").getValue(Boolean.class);

                if (Boolean.TRUE.equals(eraHost)) {
                    hostFueExpulsado = true;
                }

            } else {
                jugadoresRestantes.add(jugadorSnap);
            }
        }

        if (jugadoresRestantes.isEmpty()) {
            eliminarSalaCompleta(null);
            return;
        }

        if (hostFueExpulsado) {
            DataSnapshot nuevoHostSnap = jugadoresRestantes.get(
                    new Random().nextInt(jugadoresRestantes.size())
            );

            String nuevoHostUid = nuevoHostSnap.getKey();

            if (nuevoHostUid != null) {
                for (DataSnapshot jugadorSnap : jugadoresRestantes) {
                    String uid = jugadorSnap.getKey();

                    if (uid != null) {
                        actualizaciones.put("jugadores/" + uid + "/esHost", false);
                    }
                }

                actualizaciones.put("jugadores/" + nuevoHostUid + "/esHost", true);
                actualizaciones.put("hostUID", nuevoHostUid);
                actualizaciones.put("hostUid", nuevoHostUid);
                actualizaciones.put("hostNombre", leerNombreJugador(nuevoHostSnap));
            }
        }

        salaRef.updateChildren(actualizaciones);
    }

    private boolean soyResponsableDeLimpieza(DataSnapshot snapshotJugadores) {
        if (!usuarioValido()) {
            return false;
        }

        String miUid = auth.getCurrentUser().getUid();

        if (esHost) {
            return true;
        }

        String primerUidConectado = null;
        String primerUidCualquiera = null;

        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
            String uid = jugadorSnap.getKey();

            if (uid == null) {
                continue;
            }

            if (primerUidCualquiera == null) {
                primerUidCualquiera = uid;
            }

            Boolean conectado = jugadorSnap.child("estaConectado").getValue(Boolean.class);

            if (Boolean.TRUE.equals(conectado)) {
                primerUidConectado = uid;
                break;
            }
        }

        if (primerUidConectado != null) {
            return miUid.equals(primerUidConectado);
        }

        return miUid.equals(primerUidCualquiera);
    }

    private ArrayList<DataSnapshot> obtenerJugadoresDisponibles(DataSnapshot snapshotJugadores) {
        ArrayList<DataSnapshot> jugadores = new ArrayList<>();

        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
            Boolean conectado = jugadorSnap.child("estaConectado").getValue(Boolean.class);

            if (Boolean.TRUE.equals(conectado)) {
                jugadores.add(jugadorSnap);
            }
        }

        if (!jugadores.isEmpty()) {
            return jugadores;
        }

        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
            jugadores.add(jugadorSnap);
        }

        return jugadores;
    }

    private void asignarNuevoHost(DataSnapshot snapshotJugadores, String nuevoHostUid) {
        if (salaRef == null) {
            return;
        }

        HashMap<String, Object> actualizaciones = new HashMap<>();

        String nombreNuevoHost = "Jugador";

        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
            String uid = jugadorSnap.getKey();

            if (uid != null) {
                actualizaciones.put("jugadores/" + uid + "/esHost", false);
            }

            if (uid != null && uid.equals(nuevoHostUid)) {
                nombreNuevoHost = leerNombreJugador(jugadorSnap);
            }
        }

        actualizaciones.put("jugadores/" + nuevoHostUid + "/esHost", true);
        actualizaciones.put("hostUID", nuevoHostUid);
        actualizaciones.put("hostUid", nuevoHostUid);
        actualizaciones.put("hostNombre", nombreNuevoHost);

        salaRef.updateChildren(actualizaciones);
    }

    public void mandarHomePorDesconexion() {
        if (salida) {
            return;
        }

        salida = true;
        cancelarTiempoDeGracia();

        abandonarSalaEnFirebase(() -> irHome(false, false));
    }

    private void abandonarSalaEnFirebase(Runnable despues) {
        if (!usuarioValido() || salaRef == null || jugadoresRef == null) {
            ejecutarDespues(despues);
            return;
        }

        String miUid = auth.getCurrentUser().getUid();

        jugadoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                if (!snapshotJugadores.exists()) {
                    eliminarSalaCompleta(despues);
                    return;
                }

                DataSnapshot miJugadorSnap = snapshotJugadores.child(miUid);
                boolean yoEraHost = esHost;

                Boolean hostFirebase = miJugadorSnap.child("esHost").getValue(Boolean.class);

                if (Boolean.TRUE.equals(hostFirebase)) {
                    yoEraHost = true;
                }

                ArrayList<DataSnapshot> jugadoresRestantes = new ArrayList<>();

                for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                    String uid = jugadorSnap.getKey();

                    if (uid != null && !uid.equals(miUid)) {
                        jugadoresRestantes.add(jugadorSnap);
                    }
                }

                if (jugadoresRestantes.isEmpty()) {
                    eliminarSalaCompleta(despues);
                    return;
                }

                HashMap<String, Object> actualizaciones = new HashMap<>();
                actualizaciones.put("jugadores/" + miUid, null);

                if (yoEraHost) {
                    DataSnapshot nuevoHostSnap = jugadoresRestantes.get(
                            new Random().nextInt(jugadoresRestantes.size())
                    );

                    String nuevoHostUid = nuevoHostSnap.getKey();

                    if (nuevoHostUid != null) {
                        for (DataSnapshot jugadorSnap : jugadoresRestantes) {
                            String uid = jugadorSnap.getKey();

                            if (uid != null) {
                                actualizaciones.put("jugadores/" + uid + "/esHost", false);
                            }
                        }

                        actualizaciones.put("jugadores/" + nuevoHostUid + "/esHost", true);
                        actualizaciones.put("hostUID", nuevoHostUid);
                        actualizaciones.put("hostUid", nuevoHostUid);
                        actualizaciones.put("hostNombre", leerNombreJugador(nuevoHostSnap));
                    }
                }

                salaRef.updateChildren(actualizaciones)
                        .addOnCompleteListener(task -> ejecutarDespues(despues));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ejecutarDespues(despues);
            }
        });
    }

    private void eliminarSalaCompleta(Runnable despues) {
        if (salaRef == null) {
            ejecutarDespues(despues);
            return;
        }

        salaRef.removeValue().addOnCompleteListener(task -> {
            if (codigoSala != null && !codigoSala.trim().isEmpty()) {
                FirebaseDatabase.getInstance()
                        .getReference("codigosSalas")
                        .child(codigoSala)
                        .removeValue()
                        .addOnCompleteListener(task2 -> ejecutarDespues(despues));
            } else {
                ejecutarDespues(despues);
            }
        });
    }

    private void ejecutarDespues(Runnable despues) {
        if (despues != null) {
            despues.run();
        }
    }

    private String leerNombreJugador(DataSnapshot jugadorSnap) {
        String nombre = jugadorSnap.child("nombre").getValue(String.class);

        if (nombre == null || nombre.trim().isEmpty()) {
            nombre = "Jugador";
        }

        return nombre;
    }

    private boolean usuarioValido() {
        return auth != null &&
                VerificacionesAuth.VerificarInicioSesion(auth) &&
                auth.getCurrentUser() != null;
    }

    private void irHome(boolean desconexion, boolean creadorDesconexion) {
        salida = true;
        cancelarTiempoDeGracia();
        limpiarListeners();

        Intent intent = new Intent(SalaActivity.this, HomeActivity.class);
        intent.putExtra("DESCONEXION", desconexion);
        intent.putExtra("CREADOR_DESCONEXION", creadorDesconexion);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void limpiarListeners() {
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