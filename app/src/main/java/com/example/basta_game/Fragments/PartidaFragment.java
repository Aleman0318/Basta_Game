package com.example.basta_game.Fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.basta_game.HomeActivity;
import com.example.basta_game.models.ApiService;
import com.example.basta_game.models.ItemRespuesta;
import com.example.basta_game.models.ResultadoApi;
import com.example.basta_game.models.RetrofitClient;
import com.example.basta_game.models.ValidacionRequest;
import com.example.basta_game.models.ValidacionResponse;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.basta_game.Adapters.CategoriaPartidaAdapter;
import com.example.basta_game.Adapters.JugadorPartidaAdapter;
import com.example.basta_game.Adapters.PuntuacionAdapter;
import com.example.basta_game.Adapters.TarjetasValidacionAdapter;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.PuntuacionJugador;
import com.example.basta_game.models.RespuestaValidacion;
import com.example.basta_game.models.Sala;
import com.example.basta_game.utils.CustomDialogHelper;
import com.example.basta_game.utils.LetraSala;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.example.basta_game.utils.VerificacionesAuth;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PartidaFragment extends Fragment {

    private static final int MAX_VOTACIONES_DECISIVAS_POR_RONDA = 2;

    private String codigoSala;
    private int tiempoSeconds;
    private String idSala;
    private int jugadoresMaximos;
    private boolean esAnfitrion;
    private int rondasMaximas;
    private boolean esCreador;
    private boolean esAnexo;
    private int rondaActual;
    private boolean bastaActivado = false;

    private MaterialButton btnAbandonar;

    private ArrayList<String> listaNombresCategorias;

    private DatabaseReference salaRef;

    private boolean sonidoFinalReproducido = false;
    private boolean bastaSonidoReproducido = false;
    private boolean sonidoResultadoRondaReproducido = false;

    private CountDownTimer cronometroRonda;

    private TextView txtCodigoSala;
    private TextView txtRondaActual;
    private TextView txtEstadoPartida;
    private TextView txtTiempoRestante;
    private String letraElegida;
    private TextView txtInstruccionLetra;

    private ImageView imgLetraActual;

    private AppCompatButton btnBasta;

    private RecyclerView rvJugadoresPartida;
    private RecyclerView rvCategoriasJuego;

    private HashMap<String, String> respuestasJugador = new HashMap<>();

    private boolean modalValidacionMostrado = false;
    private boolean respuestasGuardadas = false;
    private boolean cierreRondaSolicitado = false;

    private ValueEventListener listenerRespuestasValidacion;
    private DatabaseReference nodoRespuestasRondaRef;

    private ValueEventListener listenerCambiosRonda;
    private ValueEventListener listenerBasta;

    private ValueEventListener listenerRevisionLista;
    private DatabaseReference nodoRevisionListaRef;

    private ValueEventListener listenerVotacionActiva;

    private Dialog dialogValidacionActual;
    private Dialog dialogPuntuacionesActual;
    private Dialog dialogVotacionActual;

    private String idVotacionModalActual = null;
    private boolean resolviendoVotacion = false;
    private boolean hayVotacionActivaEnRevision = false;

    private FirebaseAuth auth;

    private View notificationPartida;

    private Long ultimoBastaTimestampNotificado = null;
    private int ultimaRondaBastaNotificada = -1;
    private String faseActual = "jugando";

    private interface PuntuacionesCallback {
        void onCargadas(List<PuntuacionJugador> lista);
    }

    private interface PuntosVotacionCallback {
        void onPuntosCalculados(int puntos);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partida, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        obtenerArgumentos();
        inicializarVistas(view);
        configurarFirebase();
        cargarDatosIniciales();

        escucharCambiosDeRonda();

        setearCronometroGlobal();
        escucharBastaParaCronometro();
        escucharVotacionActiva();
        configurarBotones();

        ObtenerEstadoPartida(auth, idSala);
        ObtenerJugadoresYLlenarPartida(idSala);
        CambiarEstadoPartida(auth, idSala);
        cambiarEstadoUsuarios();
    }

    private void obtenerArgumentos() {
        if (getArguments() != null) {
            codigoSala = getArguments().getString("CODIGO_SALA");
            esAnfitrion = getArguments().getBoolean("ES_ANFITRION", false);
            listaNombresCategorias = getArguments().getStringArrayList("CATEGORIAS");
            tiempoSeconds = getArguments().getInt("TIEMPO", 60);
            jugadoresMaximos = getArguments().getInt("JUGADORES", 4);
            rondasMaximas = getArguments().getInt("RONDAS", 3);
            esCreador = getArguments().getBoolean("CREADOR", false);
            idSala = getArguments().getString("IDSALA");
            esAnexo = getArguments().getBoolean("ANEXO", false);
            rondaActual = getArguments().getInt("RONDA_ACTUAL", 1);
            letraElegida = getArguments().getString("LETRA", "A");
        }
    }

    private void inicializarVistas(View view) {
        txtCodigoSala = view.findViewById(R.id.txtCodigoSala);
        txtRondaActual = view.findViewById(R.id.txtRondaActual);
        txtEstadoPartida = view.findViewById(R.id.txtEstadoPartida);
        txtTiempoRestante = view.findViewById(R.id.txtTiempoRestante);
        txtInstruccionLetra = view.findViewById(R.id.txtInstruccionLetra);
        btnAbandonar = view.findViewById(R.id.btnSalida);
        imgLetraActual = view.findViewById(R.id.imgLetraActual);
        btnBasta = view.findViewById(R.id.btnBasta);
        rvJugadoresPartida = view.findViewById(R.id.rvJugadoresPartida);
        rvCategoriasJuego = view.findViewById(R.id.rvCategoriasJuego);
        notificationPartida = view.findViewById(R.id.notificationPartida);
    }

    private void configurarFirebase() {
        salaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala != null ? idSala : "TEST");
    }

    private void cargarDatosIniciales() {
        txtCodigoSala.setText(codigoSala);
        txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
        txtTiempoRestante.setText(formatearTiempo(tiempoSeconds));
        mostrarLetra(letraElegida);

        rvJugadoresPartida.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        rvCategoriasJuego.setLayoutManager(new LinearLayoutManager(getContext()));

        cargarCategoriasJuego();
    }

    private void cargarCategoriasJuego() {
        List<Categoria> listaCategorias = new ArrayList<>();

        if (listaNombresCategorias != null && !listaNombresCategorias.isEmpty()) {
            for (String nombreCat : listaNombresCategorias) {
                int iconoId = obtenerIconoParaCategoria(nombreCat);
                listaCategorias.add(new Categoria(nombreCat, iconoId));
            }
        } else {
            listaCategorias.add(new Categoria("Sin categorías", R.drawable.outline_person_24));
        }

        rvCategoriasJuego.setAdapter(new CategoriaPartidaAdapter(listaCategorias, respuestasJugador));
    }

    private void configurarBotones() {
        btnBasta.setOnClickListener(v -> {
            if (bastaActivado) {
                return;
            }

            DatabaseReference nodoBasta = FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .child("bastaPresionado");

            nodoBasta.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Boolean valorActual = currentData.getValue(Boolean.class);

                    if (valorActual != null && valorActual) {
                        return Transaction.abort();
                    }

                    currentData.setValue(true);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                    if (error != null) {
                        Log.d("BASTA", "Error en transaction: " + error.getMessage());
                        return;
                    }

                    if (committed) {
                        bastaActivado = true;

                        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                                .getReference("salas")
                                .child(idSala);

                        Map<String, Object> datosBasta = new HashMap<>();
                        datosBasta.put("bastaPresionadoPor", auth.getCurrentUser().getUid());
                        datosBasta.put("bastaTimeStamp", ServerValue.TIMESTAMP);

                        nodoSala.updateChildren(datosBasta).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("BASTA", "BASTA activado correctamente");
                            } else {
                                Log.d("BASTA", "Error guardando datos de BASTA");
                            }
                        });
                    } else {
                        Log.d("BASTA", "Otro jugador ya presionó BASTA primero");
                    }
                }
            });
        });

        btnAbandonar.setOnClickListener(view -> {
            CustomDialogHelper.show(
                    requireContext(),
                    "¿Deseas salir?",
                    "Si sales de la sala abandonarás la partida y no podras volver",
                    true,
                    new CustomDialogHelper.DialogButton(
                            "No",
                            Color.parseColor("#888888"),
                            Color.WHITE,
                            true,
                            null
                    ),
                    new CustomDialogHelper.DialogButton(
                            "Sí, salir",
                            Color.parseColor("#E53935"),
                            Color.WHITE,
                            true,
                            () -> ((SalaActivity) requireActivity()).mandarHomePorDesconexion()
                    )
            );
        });
    }

    private boolean esFaseJugando() {
        return faseActual == null ||
                faseActual.equalsIgnoreCase("jugando") ||
                faseActual.equalsIgnoreCase("En juego");
    }

    private boolean rondaSigueSiendoActual(int rondaEsperada) {
        return isAdded() && rondaActual == rondaEsperada;
    }

    private void escucharCambiosDeRonda() {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        listenerCambiosRonda = nodoSala.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }

                String faseFirebase = snapshot.child("fase").getValue(String.class);

                if (faseFirebase != null) {
                    faseActual = faseFirebase;
                }

                String estado = snapshot.child("estadoPartida").getValue(String.class);
                int rondaFirebase = leerEntero(snapshot.child("rondaActual"), rondaActual);

                if (estado != null &&
                        estado.equalsIgnoreCase("Esperando jugadores") &&
                        rondaFirebase == 0) {

                    volverAlInicio();
                    return;
                }

                int cantidadRondasFirebase = leerEntero(snapshot.child("cantidadRondas"), rondasMaximas);
                int tiempoRondaFirebase = leerEntero(snapshot.child("tiempoRonda"), tiempoSeconds);

                String letraFirebase = snapshot.child("letraActual").getValue(String.class);

                if (letraFirebase == null || letraFirebase.trim().isEmpty()) {
                    letraFirebase = letraElegida;
                }

                boolean cambioRonda = rondaFirebase != rondaActual;
                boolean cambioLetra = letraElegida == null ||
                        !letraFirebase.equalsIgnoreCase(letraElegida);
                boolean cambioTiempo = tiempoRondaFirebase != tiempoSeconds;

                rondasMaximas = cantidadRondasFirebase;
                tiempoSeconds = tiempoRondaFirebase;

                if (cambioRonda || cambioLetra || cambioTiempo) {
                    rondaActual = rondaFirebase;
                    letraElegida = letraFirebase;

                    reiniciarPartidaParaNuevaRonda();
                } else {
                    txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
                    mostrarLetra(letraElegida);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("RONDA", "Error escuchando cambios de ronda: " + error.getMessage());
            }
        });
    }

    private void reiniciarPartidaParaNuevaRonda() {
        if (!isAdded()) {
            return;
        }

        limpiarListenerRevision();

        cierreRondaSolicitado = false;
        respuestasGuardadas = false;
        modalValidacionMostrado = false;
        bastaActivado = false;
        sonidoFinalReproducido = false;
        bastaSonidoReproducido = false;
        sonidoResultadoRondaReproducido = false;

        faseActual = "jugando";
        ultimoBastaTimestampNotificado = null;
        ultimaRondaBastaNotificada = -1;

        idVotacionModalActual = null;
        resolviendoVotacion = false;
        hayVotacionActivaEnRevision = false;

        respuestasJugador.clear();

        if (cronometroRonda != null) {
            cronometroRonda.cancel();
            cronometroRonda = null;
        }

        if (nodoRespuestasRondaRef != null && listenerRespuestasValidacion != null) {
            nodoRespuestasRondaRef.removeEventListener(listenerRespuestasValidacion);
            listenerRespuestasValidacion = null;
            nodoRespuestasRondaRef = null;
        }

        if (dialogValidacionActual != null && dialogValidacionActual.isShowing()) {
            dialogValidacionActual.dismiss();
            dialogValidacionActual = null;
        }

        if (dialogPuntuacionesActual != null && dialogPuntuacionesActual.isShowing()) {
            dialogPuntuacionesActual.dismiss();
            dialogPuntuacionesActual = null;
        }

        if (dialogVotacionActual != null && dialogVotacionActual.isShowing()) {
            dialogVotacionActual.dismiss();
            dialogVotacionActual = null;
        }

        txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
        txtTiempoRestante.setText(formatearTiempo(tiempoSeconds));
        mostrarLetra(letraElegida);

        btnBasta.setEnabled(true);
        btnBasta.setAlpha(1f);

        cargarCategoriasJuego();
        setearCronometroGlobal();

        Log.d("RONDA", "Nueva ronda cargada: " + rondaActual + " letra: " + letraElegida);
    }

    private int leerEntero(DataSnapshot snapshot, int valorDefault) {
        Object valor = snapshot.getValue();

        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }

        if (valor instanceof String) {
            try {
                return Integer.parseInt((String) valor);
            } catch (NumberFormatException e) {
                return valorDefault;
            }
        }

        return valorDefault;
    }

    private void mostrarLetra(String letra) {
        if (letra == null || letra.trim().isEmpty()) {
            letra = "A";
        }

        txtInstruccionLetra.setText("Escribe respuestas que empiecen con la letra " + letra);

        String nombreDrawable = "letra_" + letra.toLowerCase(Locale.ROOT);
        int idDrawable = getResources().getIdentifier(
                nombreDrawable,
                "drawable",
                requireContext().getPackageName()
        );

        if (idDrawable != 0) {
            imgLetraActual.setImageResource(idDrawable);
        }
    }

    private int obtenerIconoParaCategoria(String nombre) {
        switch (nombre.toLowerCase(Locale.ROOT)) {
            case "animal": return R.drawable.ic_animal3;
            case "color": return R.drawable.ic_paleta2;
            case "fruta": return R.drawable.ic_frutas2;
            case "país": return R.drawable.ic_pais2;
            case "pais": return R.drawable.ic_pais2;
            case "cosa": return R.drawable.ic_cosa;
            case "nombre": return R.drawable.ic_nombre;
            case "apellido": return R.drawable.ic_mujer;
            default: return R.drawable.perfil_menuu;
        }
    }

    public void CambiarEstadoPartida(FirebaseAuth auth, String idSala) {
        if (VerificacionesAuth.VerificarInicioSesion(auth)) {
            DatabaseReference nodoEstado = FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .child("estadoPartida");

            nodoEstado.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String estadoActual = snapshot.getValue(String.class);

                    if (estadoActual != null && estadoActual.equalsIgnoreCase("Esperando jugadores")) {
                        return;
                    }

                    nodoEstado.setValue("En juego").addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("ESTADO", "Se cambió el estado de la partida correctamente");
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    public void setearCronometroGlobal() {
        final int rondaDelCronometro = rondaActual;

        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                if (!rondaSigueSiendoActual(rondaDelCronometro)) {
                    return;
                }

                String faseFirebase = snapshotSala.child("fase").getValue(String.class);

                if (faseFirebase != null) {
                    faseActual = faseFirebase;
                }

                if (!esFaseJugando()) {
                    Log.d("TIEMPO", "No se inicia cronómetro porque faseActual = " + faseActual);
                    return;
                }

                int rondaFirebase = leerEntero(snapshotSala.child("rondaActual"), rondaDelCronometro);
                int inicioRondaDe = leerEntero(snapshotSala.child("inicioRondaDe"), rondaFirebase);
                Long inicioRonda = snapshotSala.child("inicioRonda").getValue(Long.class);

                if (rondaFirebase != rondaDelCronometro ||
                        inicioRondaDe != rondaDelCronometro ||
                        inicioRonda == null) {

                    Log.d("TIEMPO", "Esperando inicioRonda correcto para ronda " + rondaDelCronometro);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (rondaSigueSiendoActual(rondaDelCronometro)) {
                            setearCronometroGlobal();
                        }
                    }, 500);

                    return;
                }

                DatabaseReference offsetRef = FirebaseDatabase.getInstance()
                        .getReference(".info/serverTimeOffset");

                offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot offsetSnapshot) {
                        Long offset = offsetSnapshot.getValue(Long.class);

                        if (offset == null) {
                            offset = 0L;
                        }

                        long horaActualServidor = System.currentTimeMillis() + offset;
                        long duracionRondaMs = tiempoSeconds * 1000L;
                        long tiempoPasadoMs = horaActualServidor - inicioRonda;
                        long tiempoRestanteMs = duracionRondaMs - tiempoPasadoMs;

                        if (!rondaSigueSiendoActual(rondaDelCronometro) || !esFaseJugando()) {
                            return;
                        }

                        if (tiempoRestanteMs <= 0) {
                            txtTiempoRestante.setText("00:00");
                            guardarRespuestasJugador();
                            return;
                        }

                        if (cronometroRonda != null) {
                            cronometroRonda.cancel();
                        }

                        cronometroRonda = new CountDownTimer(tiempoRestanteMs, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                if (!rondaSigueSiendoActual(rondaDelCronometro)) {
                                    cancel();
                                    return;
                                }

                                long segundosRestantes = millisUntilFinished / 1000;

                                if (segundosRestantes == 5 && !sonidoFinalReproducido) {
                                    sonidoFinalReproducido = true;
                                    Sounds.conteoFinalPlay();
                                }

                                txtTiempoRestante.setText(formatearTiempo(segundosRestantes));
                            }

                            @Override
                            public void onFinish() {
                                if (!rondaSigueSiendoActual(rondaDelCronometro) || !esFaseJugando()) {
                                    return;
                                }

                                txtTiempoRestante.setText("00:00");
                                guardarRespuestasJugador();
                            }
                        };

                        cronometroRonda.start();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (cronometroRonda != null) {
            cronometroRonda.cancel();
            cronometroRonda = null;
        }

        if (nodoRespuestasRondaRef != null && listenerRespuestasValidacion != null) {
            nodoRespuestasRondaRef.removeEventListener(listenerRespuestasValidacion);
            listenerRespuestasValidacion = null;
        }

        if (listenerCambiosRonda != null) {
            FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .removeEventListener(listenerCambiosRonda);

            listenerCambiosRonda = null;
        }

        if (listenerBasta != null) {
            FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .removeEventListener(listenerBasta);

            listenerBasta = null;
        }

        if (listenerVotacionActiva != null) {
            FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .child("votacionActiva")
                    .removeEventListener(listenerVotacionActiva);

            listenerVotacionActiva = null;
        }

        limpiarListenerRevision();

        if (dialogValidacionActual != null && dialogValidacionActual.isShowing()) {
            dialogValidacionActual.dismiss();
            dialogValidacionActual = null;
        }

        if (dialogPuntuacionesActual != null && dialogPuntuacionesActual.isShowing()) {
            dialogPuntuacionesActual.dismiss();
            dialogPuntuacionesActual = null;
        }

        if (dialogVotacionActual != null && dialogVotacionActual.isShowing()) {
            dialogVotacionActual.dismiss();
            dialogVotacionActual = null;
        }
    }

    private String formatearTiempo(long totalSegundos) {
        if (totalSegundos < 0) {
            totalSegundos = 0;
        }

        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;

        return String.format(Locale.ROOT, "%02d:%02d", minutos, segundos);
    }

    public void ObtenerEstadoPartida(FirebaseAuth auth, String salaID) {
        if (VerificacionesAuth.VerificarInicioSesion(auth)) {
            DatabaseReference nodoEstado = FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(salaID);

            nodoEstado.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Sala sala = snapshot.getValue(Sala.class);

                    if (sala != null) {
                        txtEstadoPartida.setText("● " + sala.getEstadoPartida());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    public void ObtenerJugadoresYLlenarPartida(String idSala) {
        auth = FirebaseAuth.getInstance();

        if (VerificacionesAuth.VerificarInicioSesion(auth)) {
            DatabaseReference nodoSalas = FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .child("jugadores");

            nodoSalas.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ArrayList<Jugador> jugadoresActivos = new ArrayList<>();

                    for (DataSnapshot hijo : snapshot.getChildren()) {
                        JugadorSala jugador = hijo.getValue(JugadorSala.class);

                        if (jugador != null) {
                            jugadoresActivos.add(
                                    new Jugador(
                                            jugador.getNombre(),
                                            jugador.isEstaListo(),
                                            jugador.getUrlAvatar(),
                                            jugador.isEsHost()
                                    )
                            );
                        }
                    }

                    if (!jugadoresActivos.isEmpty()) {
                        JugadorPartidaAdapter adaptador = new JugadorPartidaAdapter(jugadoresActivos);
                        rvJugadoresPartida.setAdapter(adaptador);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    private void cambiarEstadoUsuarios() {
        if (VerificacionesAuth.VerificarInicioSesion(auth)) {
            DatabaseReference nodoEstadoUsuario = FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .child("jugadores")
                    .child(auth.getCurrentUser().getUid())
                    .child("estaListo");

            nodoEstadoUsuario.setValue(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("CAMBIAR", "ESTADO DE USUARIO CAMBIADO A DESACTIVADO");
                }
            });
        }
    }

    private void guardarRespuestasJugador() {
        if (!esFaseJugando()) {
            Log.d("RESPUESTAS", "No se guardan respuestas porque la fase actual no es jugando: " + faseActual);
            return;
        }

        if (respuestasGuardadas) {
            return;
        }

        respuestasGuardadas = true;

        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        final int rondaGuardada = rondaActual;
        String uid = auth.getCurrentUser().getUid();

        HashMap<String, String> respuestasFinales = new HashMap<>();

        if (listaNombresCategorias != null) {
            for (String categoria : listaNombresCategorias) {
                String respuesta = respuestasJugador.get(categoria);

                if (respuesta == null) {
                    respuesta = "";
                }

                respuestasFinales.put(categoria, respuesta.trim());
            }
        }

        DatabaseReference nodoRespuestaJugador = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("respuestas")
                .child("ronda_" + rondaGuardada)
                .child(uid);

        nodoRespuestaJugador.setValue(respuestasFinales).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("RESPUESTAS", "Respuestas guardadas correctamente en ronda " + rondaGuardada);
                esperarRespuestasYMostrarModal(rondaGuardada);
            } else {
                Log.d("RESPUESTAS", "Error al guardar respuestas");
                respuestasGuardadas = false;
            }
        });
    }

    private void validarRespuestasConApi(
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            int rondaDeEstaRevision
    ) {
        if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
            return;
        }

        List<ItemRespuesta> respuestasApi = new ArrayList<>();

        for (String categoria : listaNombresCategorias) {
            String respuesta = respuestasJugador.get(categoria);

            if (respuesta == null) {
                respuesta = "";
            }

            String categoriaApi = mapearCategoriaParaApi(categoria);
            respuestasApi.add(new ItemRespuesta(categoriaApi, respuesta.trim()));
        }

        ValidacionRequest request = new ValidacionRequest(letraElegida, respuestasApi);
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.validarRonda(request).enqueue(new retrofit2.Callback<ValidacionResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ValidacionResponse> call, retrofit2.Response<ValidacionResponse> response) {
                if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<ResultadoApi> resultados = response.body().getResultados();

                    HashMap<String, Object> validacionesJugador = new HashMap<>();

                    for (ResultadoApi resultado : resultados) {
                        String categoriaOriginal = mapearCategoriaDesdeApi(resultado.getCategoria());

                        for (String cat : listaNombresCategorias) {
                            if (cat.equalsIgnoreCase(categoriaOriginal)) {
                                categoriaOriginal = cat;
                                break;
                            }
                        }

                        String miRespuesta = respuestasJugador.get(categoriaOriginal);

                        if (miRespuesta == null) {
                            miRespuesta = "";
                        }

                        int repeticiones = 0;
                        List<RespuestaValidacion> todasLasRespuestas = respuestasPorCategoria.get(categoriaOriginal);

                        if (todasLasRespuestas != null) {
                            for (RespuestaValidacion rv : todasLasRespuestas) {
                                if (rv.getRespuesta() != null &&
                                        rv.getRespuesta().trim().equalsIgnoreCase(miRespuesta.trim())) {
                                    repeticiones++;
                                }
                            }
                        }

                        int puntosAsignados = 0;

                        if (resultado.isCorrecta()) {
                            puntosAsignados = (repeticiones > 1) ? 5 : 10;
                        }

                        HashMap<String, Object> datosCat = new HashMap<>();
                        datosCat.put("correcta", resultado.isCorrecta());
                        datosCat.put("puntos", puntosAsignados);

                        validacionesJugador.put(categoriaOriginal, datosCat);
                    }

                    DatabaseReference nodoMisValidaciones = FirebaseDatabase.getInstance()
                            .getReference("salas")
                            .child(idSala)
                            .child("validaciones")
                            .child("ronda_" + rondaDeEstaRevision)
                            .child(auth.getCurrentUser().getUid());

                    nodoMisValidaciones.setValue(validacionesJugador).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("VALIDACION", "Resultados API subidos en ronda " + rondaDeEstaRevision);
                        }
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ValidacionResponse> call, Throwable t) {
                Log.e("VALIDACION_API", "Error API: " + t.getMessage());
            }
        });
    }

    private void esperarRespuestasYMostrarModal(int rondaDeEstaRevision) {
        nodoRespuestasRondaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("respuestas")
                .child("ronda_" + rondaDeEstaRevision);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                return;
            }

            if (!modalValidacionMostrado && isAdded() && esFaseJugando()) {
                Log.d("VALIDACION", "Forzando apertura del modal por Timeout de jugadores lentos");
                modalValidacionMostrado = true;
                cargarRespuestasValidacionYMostrarModal(rondaDeEstaRevision);
            }
        }, 5000);

        listenerRespuestasValidacion = nodoRespuestasRondaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotRespuestas) {
                if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                long cantidadRespuestas = snapshotRespuestas.getChildrenCount();

                DatabaseReference nodoJugadores = FirebaseDatabase.getInstance()
                        .getReference("salas")
                        .child(idSala)
                        .child("jugadores");

                nodoJugadores.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                        if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                            return;
                        }

                        long cantidadJugadores = snapshotJugadores.getChildrenCount();

                        if (cantidadJugadores > 0 && cantidadRespuestas >= cantidadJugadores) {
                            if (modalValidacionMostrado) {
                                return;
                            }

                            modalValidacionMostrado = true;

                            if (!isAdded()) {
                                return;
                            }

                            cargarRespuestasValidacionYMostrarModal(rondaDeEstaRevision);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void cargarRespuestasValidacionYMostrarModal(int rondaDeEstaRevision) {
        if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
            return;
        }

        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                DataSnapshot snapshotJugadores = snapshotSala.child("jugadores");
                DataSnapshot snapshotRespuestas = snapshotSala
                        .child("respuestas")
                        .child("ronda_" + rondaDeEstaRevision);

                HashMap<String, List<RespuestaValidacion>> respuestasPorCategoria = new HashMap<>();

                if (listaNombresCategorias == null || listaNombresCategorias.isEmpty()) {
                    Log.d("VALIDACION", "No hay categorías para validar");
                    return;
                }

                for (String categoria : listaNombresCategorias) {
                    respuestasPorCategoria.put(categoria, new ArrayList<>());
                }

                for (DataSnapshot jugadorSnapshot : snapshotJugadores.getChildren()) {
                    JugadorSala jugador = jugadorSnapshot.getValue(JugadorSala.class);
                    String uidJugador = jugadorSnapshot.getKey();

                    if (jugador != null && uidJugador != null) {
                        String nombreJugador = jugador.getNombre();
                        String fotoJugador = jugador.getUrlAvatar();

                        for (String categoria : listaNombresCategorias) {
                            String respuesta = snapshotRespuestas
                                    .child(uidJugador)
                                    .child(categoria)
                                    .getValue(String.class);

                            if (respuesta == null || respuesta.trim().isEmpty()) {
                                respuesta = "Sin respuesta";
                            }

                            List<RespuestaValidacion> lista = respuestasPorCategoria.get(categoria);

                            if (lista != null) {
                                RespuestaValidacion rv = new RespuestaValidacion(
                                        uidJugador,
                                        nombreJugador,
                                        respuesta,
                                        fotoJugador
                                );

                                rv.setCategoria(categoria);
                                lista.add(rv);
                            }
                        }
                    }
                }

                if (!isAdded()) {
                    return;
                }

                validarRespuestasConApi(respuestasPorCategoria, rondaDeEstaRevision);

                mostrarModalValidacionCarrusel(
                        listaNombresCategorias,
                        respuestasPorCategoria,
                        rondaDeEstaRevision
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("VALIDACION", "Error cargando respuestas: " + error.getMessage());
            }
        });
    }

    private void escucharBastaParaCronometro() {
        DatabaseReference nodoBastaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        listenerBasta = nodoBastaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!esFaseJugando()) {
                    return;
                }

                Long bastaTimeStamp = snapshot.child("bastaTimeStamp").getValue(Long.class);
                String uidBasta = snapshot.child("bastaPresionadoPor").getValue(String.class);

                if (bastaTimeStamp == null || uidBasta == null) {
                    return;
                }

                boolean yaNotificado =
                        ultimoBastaTimestampNotificado != null &&
                                ultimoBastaTimestampNotificado.equals(bastaTimeStamp) &&
                                ultimaRondaBastaNotificada == rondaActual;

                if (yaNotificado) {
                    return;
                }

                ultimoBastaTimestampNotificado = bastaTimeStamp;
                ultimaRondaBastaNotificada = rondaActual;

                Sounds.bastaPlay();

                String nombreJugadorBasta = snapshot
                        .child("jugadores")
                        .child(uidBasta)
                        .child("nombre")
                        .getValue(String.class);

                if (nombreJugadorBasta == null) {
                    nombreJugadorBasta = "Un jugador";
                }

                NotificationHelper.show(
                        notificationPartida,
                        "¡" + nombreJugadorBasta + " presionó BASTA!",
                        NotificationHelper.SUCCESS
                );
                recalcularCronometroConBasta(bastaTimeStamp);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("BASTA", "Error escuchando bastaTimeStamp: " + error.getMessage());
            }
        });
    }

    private void recalcularCronometroConBasta(long bastaTimeStamp) {
        final int rondaDelBasta = rondaActual;

        if (!esFaseJugando()) {
            return;
        }

        DatabaseReference offsetRef = FirebaseDatabase.getInstance()
                .getReference(".info/serverTimeOffset");

        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot offsetSnapshot) {
                if (!rondaSigueSiendoActual(rondaDelBasta) || !esFaseJugando()) {
                    return;
                }

                Long offset = offsetSnapshot.getValue(Long.class);

                if (offset == null) {
                    offset = 0L;
                }

                long offsetFinal = offset;

                DatabaseReference nodoInicioRonda = FirebaseDatabase.getInstance()
                        .getReference("salas")
                        .child(idSala);

                nodoInicioRonda.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                        if (!rondaSigueSiendoActual(rondaDelBasta) || !esFaseJugando()) {
                            return;
                        }

                        Long inicioRonda = snapshotSala.child("inicioRonda").getValue(Long.class);
                        int inicioRondaDe = leerEntero(snapshotSala.child("inicioRondaDe"), rondaDelBasta);

                        if (inicioRonda == null || inicioRondaDe != rondaDelBasta) {
                            return;
                        }

                        long horaActualServidor = System.currentTimeMillis() + offsetFinal;

                        long finOriginal = inicioRonda + (tiempoSeconds * 1000L);
                        long finBasta = bastaTimeStamp + 10000L;

                        long finReal = Math.min(finOriginal, finBasta);

                        long tiempoRestanteMs = finReal - horaActualServidor;

                        if (tiempoRestanteMs <= 0) {
                            txtTiempoRestante.setText("00:00");
                            guardarRespuestasJugador();
                            return;
                        }

                        if (cronometroRonda != null) {
                            cronometroRonda.cancel();
                        }

                        cronometroRonda = new CountDownTimer(tiempoRestanteMs, 1000) {

                            @Override
                            public void onTick(long millisUntilFinished) {
                                if (!rondaSigueSiendoActual(rondaDelBasta)) {
                                    cancel();
                                    return;
                                }

                                long segundosRestantes = (millisUntilFinished + 999) / 1000;

                                if (segundosRestantes == 5 && !sonidoFinalReproducido) {
                                    sonidoFinalReproducido = true;
                                    Sounds.conteoFinalPlay();
                                }

                                txtTiempoRestante.setText(formatearTiempo(segundosRestantes));
                            }

                            @Override
                            public void onFinish() {
                                if (!rondaSigueSiendoActual(rondaDelBasta) || !esFaseJugando()) {
                                    return;
                                }

                                txtTiempoRestante.setText("00:00");
                                guardarRespuestasJugador();
                            }
                        };

                        cronometroRonda.start();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void mostrarModalValidacionCarrusel(
            List<String> categorias,
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            int rondaDeEstaRevision
    ) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialogo_respuestas);

        dialogValidacionActual = dialog;

        dialog.setOnDismissListener(d -> {
            if (dialogValidacionActual == dialog) {
                dialogValidacionActual = null;
            }
        });

        ViewPager2 vpCategorias = dialog.findViewById(R.id.vpCategoriasValidacion);
        AppCompatButton btnSalir = dialog.findViewById(R.id.btnSalirRevision);

        btnSalir.setVisibility(View.GONE);

        final boolean[] accionPreparada = {false};

        Runnable accionMostrarBotonSalir = () -> {
            if (accionPreparada[0]) {
                return;
            }

            accionPreparada[0] = true;

            btnSalir.setVisibility(View.VISIBLE);
            btnSalir.setText("CALCULANDO...");
            btnSalir.setEnabled(false);

            cerrarRondaComoHost();
            esperarResumenRondaYActivarBoton(btnSalir, dialog, rondaDeEstaRevision);
        };

        TarjetasValidacionAdapter adapter = new TarjetasValidacionAdapter(
                categorias,
                respuestasPorCategoria,
                vpCategorias,
                accionMostrarBotonSalir,
                respuesta -> solicitarVotacionRespuesta(respuesta, rondaDeEstaRevision),
                () -> !hayVotacionActivaEnRevision
        );

        vpCategorias.setAdapter(adapter);
        vpCategorias.setOffscreenPageLimit(1);

        dialog.setCancelable(false);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        escucharValidacionesGlobales(respuestasPorCategoria, adapter, rondaDeEstaRevision);
    }

    private void esperarResumenRondaYActivarBoton(AppCompatButton btnSalir, Dialog dialog, int rondaCerrada) {
        DatabaseReference resumenRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("resumenRondaListo")
                .child("ronda_" + rondaCerrada);

        resumenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean listo = snapshot.getValue(Boolean.class);

                if (listo != null && listo) {
                    resumenRef.removeEventListener(this);

                    if (!isAdded()) {
                        return;
                    }

                    reproducirSonidoResultadoRonda(rondaCerrada);

                    btnSalir.setEnabled(true);
                    btnSalir.setText("VER PUNTUACIONES");

                    btnSalir.setOnClickListener(v -> {
                        dialog.dismiss();

                        boolean esFinal = rondaCerrada >= rondasMaximas;
                        mostrarModalPuntuaciones(rondaCerrada, esFinal);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void reproducirSonidoResultadoRonda(int rondaCerrada) {
        if (sonidoResultadoRondaReproducido) {
            return;
        }

        sonidoResultadoRondaReproducido = true;

        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        String miUid = auth.getCurrentUser().getUid();

        DatabaseReference puntajesRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("puntajesRondas")
                .child("ronda_" + rondaCerrada);

        puntajesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int maxPuntos = -1;
                int misPuntos = 0;

                for (DataSnapshot jugadorSnap : snapshot.getChildren()) {
                    String uid = jugadorSnap.getKey();
                    int puntos = leerEntero(jugadorSnap, 0);

                    if (puntos > maxPuntos) {
                        maxPuntos = puntos;
                    }

                    if (uid != null && uid.equals(miUid)) {
                        misPuntos = puntos;
                    }
                }

                if (misPuntos >= 10 && maxPuntos > 0 && misPuntos == maxPuntos) {
                    Sounds.playCelebracion();
                } else {
                    Sounds.playPerdedor();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Sounds.playPerdedor();
            }
        });
    }

    private void escucharValidacionesGlobales(
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            TarjetasValidacionAdapter adapter,
            int rondaDeEstaRevision
    ) {
        DatabaseReference nodoValidaciones = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("validaciones")
                .child("ronda_" + rondaDeEstaRevision);

        nodoValidaciones.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                for (DataSnapshot jugadorValidacion : snapshot.getChildren()) {
                    String uid = jugadorValidacion.getKey();

                    for (DataSnapshot categoriaSnap : jugadorValidacion.getChildren()) {
                        String categoria = categoriaSnap.getKey();
                        Boolean correcta = categoriaSnap.child("correcta").getValue(Boolean.class);
                        Integer puntos = categoriaSnap.child("puntos").getValue(Integer.class);

                        if (correcta != null && puntos != null && uid != null && categoria != null) {
                            List<RespuestaValidacion> lista = null;

                            for (String key : respuestasPorCategoria.keySet()) {
                                if (key.equalsIgnoreCase(categoria)) {
                                    lista = respuestasPorCategoria.get(key);
                                    break;
                                }
                            }

                            if (lista != null) {
                                for (RespuestaValidacion rv : lista) {
                                    if (rv.getUidJugador().equals(uid)) {
                                        rv.setValidada(true);
                                        rv.setCorrecta(correcta);
                                        rv.setPuntos(puntos);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (adapter != null) {
                    adapter.actualizarValidaciones();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("VALIDACIONES", "Error escuchando validaciones", error.toException());
            }
        });
    }

    private void solicitarVotacionRespuesta(RespuestaValidacion respuesta, int rondaDeEstaRevision) {
        if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
            return;
        }

        if (respuesta == null || respuesta.getRespuesta() == null ||
                respuesta.getRespuesta().trim().isEmpty() ||
                respuesta.getRespuesta().equalsIgnoreCase("Sin respuesta")) {

            NotificationHelper.show(
                    notificationPartida,
                    "No se puede votar una respuesta vacía",
                    NotificationHelper.SUCCESS
            );
            return;
        }

        if (!respuesta.isValidada()) {
            NotificationHelper.show(
                    notificationPartida,
                    "Espera a que el sistema valide esa respuesta",
                    NotificationHelper.SUCCESS
            );
            return;
        }

        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                Boolean resumenListo = snapshotSala
                        .child("resumenRondaListo")
                        .child("ronda_" + rondaDeEstaRevision)
                        .getValue(Boolean.class);

                if (resumenListo != null && resumenListo) {
                    NotificationHelper.show(
                            notificationPartida,
                            "La revisión de esta ronda ya terminó",
                            NotificationHelper.SUCCESS
                    );
                    return;
                }

                int usadas = leerEntero(
                        snapshotSala.child("votacionesUsadas").child("ronda_" + rondaDeEstaRevision),
                        0
                );

                if (usadas >= MAX_VOTACIONES_DECISIVAS_POR_RONDA) {
                    NotificationHelper.show(
                            notificationPartida,
                            "Ya se usaron las 2 votaciones de esta ronda",
                            NotificationHelper.SUCCESS
                    );
                    return;
                }

                Boolean activa = snapshotSala
                        .child("votacionActiva")
                        .child("ronda_" + rondaDeEstaRevision)
                        .child("activa")
                        .getValue(Boolean.class);

                if (activa != null && activa) {
                    NotificationHelper.show(
                            notificationPartida,
                            "Ya hay una votación activa",
                            NotificationHelper.SUCCESS
                    );
                    return;
                }

                crearVotacionActiva(respuesta, rondaDeEstaRevision);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void crearVotacionActiva(RespuestaValidacion respuesta, int rondaDeEstaRevision) {
        String idVotacion = "voto_" + rondaDeEstaRevision + "_" +
                respuesta.getUidJugador() + "_" +
                System.currentTimeMillis();

        DatabaseReference votacionRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("votacionActiva")
                .child("ronda_" + rondaDeEstaRevision);

        HashMap<String, Object> datos = new HashMap<>();
        datos.put("activa", true);
        datos.put("resuelta", false);
        datos.put("idVotacion", idVotacion);
        datos.put("ronda", rondaDeEstaRevision);
        datos.put("uidObjetivo", respuesta.getUidJugador());
        datos.put("nombreJugador", respuesta.getNombreJugador());
        datos.put("categoria", respuesta.getCategoria());
        datos.put("respuesta", respuesta.getRespuesta());
        datos.put("creadaPor", auth.getCurrentUser().getUid());
        datos.put("timestamp", ServerValue.TIMESTAMP);

        votacionRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Boolean activa = currentData.child("activa").getValue(Boolean.class);

                if (activa != null && activa) {
                    return Transaction.abort();
                }

                currentData.setValue(datos);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    Log.d("VOTACION", "Error creando votación: " + error.getMessage());
                    return;
                }

                if (!committed) {
                    NotificationHelper.show(
                            notificationPartida,
                            "Otro jugador inició una votación primero",
                            NotificationHelper.SUCCESS
                    );
                }
            }
        });
    }

    private void escucharVotacionActiva() {
        DatabaseReference votacionesRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("votacionActiva");

        listenerVotacionActiva = votacionesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }

                DataSnapshot votacionRonda = snapshot.child("ronda_" + rondaActual);

                Boolean activa = votacionRonda.child("activa").getValue(Boolean.class);
                String idVotacion = votacionRonda.child("idVotacion").getValue(String.class);

                if (activa == null || !activa || idVotacion == null) {
                    hayVotacionActivaEnRevision = false;

                    if (dialogVotacionActual != null && dialogVotacionActual.isShowing()) {
                        dialogVotacionActual.dismiss();
                    }

                    dialogVotacionActual = null;
                    idVotacionModalActual = null;
                    resolviendoVotacion = false;
                    return;
                }

                hayVotacionActivaEnRevision = true;

                if (idVotacion.equals(idVotacionModalActual)) {
                    return;
                }

                idVotacionModalActual = idVotacion;
                mostrarDialogoVotacion(rondaActual, idVotacion);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void mostrarDialogoVotacion(int rondaVotacion, String idVotacion) {
        if (!isAdded()) {
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        View vista = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_votacion_palabra, null);

        dialog.setContentView(vista);
        dialog.setCancelable(false);

        dialogVotacionActual = dialog;

        TextView txtPalabra = vista.findViewById(R.id.txtPalabraAEvaluar);
        TextView txtJugador = vista.findViewById(R.id.txtJugadorEvaluado);
        TextView txtVotosNo = vista.findViewById(R.id.txtVotosNo);
        TextView txtVotosSi = vista.findViewById(R.id.txtVotosSi);
        TextView txtVotosAbstenerse = vista.findViewById(R.id.txtVotosAbstenerse);
        TextView txtEstado = vista.findViewById(R.id.txtEstadoVotoUsuario);

        MaterialButton btnNo = vista.findViewById(R.id.btnVotoNo);
        MaterialButton btnSi = vista.findViewById(R.id.btnVotoSi);
        MaterialButton btnAbstenerse = vista.findViewById(R.id.btnVotoAbstenerse);

        DatabaseReference votacionRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("votacionActiva")
                .child("ronda_" + rondaVotacion);

        String miUid = auth.getCurrentUser().getUid();

        btnNo.setOnClickListener(v -> registrarVoto(rondaVotacion, miUid, "no"));
        btnSi.setOnClickListener(v -> registrarVoto(rondaVotacion, miUid, "si"));
        btnAbstenerse.setOnClickListener(v -> registrarVoto(rondaVotacion, miUid, "abstenerse"));

        ValueEventListener listenerDialogo = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean activa = snapshot.child("activa").getValue(Boolean.class);

                if (activa == null || !activa) {
                    votacionRef.removeEventListener(this);

                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    return;
                }

                String palabra = snapshot.child("respuesta").getValue(String.class);
                String nombreJugador = snapshot.child("nombreJugador").getValue(String.class);

                if (palabra == null) {
                    palabra = "";
                }

                if (nombreJugador == null) {
                    nombreJugador = "Jugador";
                }

                txtPalabra.setText(palabra);
                txtJugador.setText("Respuesta de " + nombreJugador);

                int votosNo = 0;
                int votosSi = 0;
                int votosAbs = 0;

                DataSnapshot votosSnap = snapshot.child("votos");

                for (DataSnapshot votoSnap : votosSnap.getChildren()) {
                    String voto = votoSnap.getValue(String.class);

                    if ("si".equalsIgnoreCase(voto)) {
                        votosSi++;
                    } else if ("no".equalsIgnoreCase(voto)) {
                        votosNo++;
                    } else if ("abstenerse".equalsIgnoreCase(voto)) {
                        votosAbs++;
                    }
                }

                txtVotosNo.setText("No: " + votosNo);
                txtVotosSi.setText("Sí: " + votosSi);
                txtVotosAbstenerse.setText("Abs: " + votosAbs);

                String miVoto = snapshot.child("votos").child(miUid).getValue(String.class);

                if (miVoto != null) {
                    btnNo.setEnabled(false);
                    btnSi.setEnabled(false);
                    btnAbstenerse.setEnabled(false);

                    txtEstado.setText("Tu voto: " + textoVotoBonito(miVoto));
                } else {
                    txtEstado.setText("Elige tu voto");
                }

                revisarSiTodosVotaronYResolver(rondaVotacion, snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        votacionRef.addValueEventListener(listenerDialogo);

        dialog.setOnDismissListener(d -> votacionRef.removeEventListener(listenerDialogo));

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private String textoVotoBonito(String voto) {
        if ("si".equalsIgnoreCase(voto)) {
            return "Sí";
        }

        if ("no".equalsIgnoreCase(voto)) {
            return "No";
        }

        return "Abstenerse";
    }

    private void registrarVoto(int rondaVotacion, String uid, String voto) {
        FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("votacionActiva")
                .child("ronda_" + rondaVotacion)
                .child("votos")
                .child(uid)
                .setValue(voto);
    }

    private void revisarSiTodosVotaronYResolver(int rondaVotacion, DataSnapshot snapshotVotacion) {
        if (!esAnfitrion && !esCreador) {
            return;
        }

        if (resolviendoVotacion) {
            return;
        }

        Boolean resuelta = snapshotVotacion.child("resuelta").getValue(Boolean.class);

        if (resuelta != null && resuelta) {
            return;
        }

        DatabaseReference jugadoresRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("jugadores");

        jugadoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                long cantidadJugadores = snapshotJugadores.getChildrenCount();
                long cantidadVotos = snapshotVotacion.child("votos").getChildrenCount();

                if (cantidadJugadores > 0 && cantidadVotos >= cantidadJugadores) {
                    resolviendoVotacion = true;
                    resolverVotacion(rondaVotacion, snapshotVotacion);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void resolverVotacion(int rondaVotacion, DataSnapshot snapshotVotacion) {
        int votosSi = 0;
        int votosNo = 0;
        int votosAbs = 0;

        for (DataSnapshot votoSnap : snapshotVotacion.child("votos").getChildren()) {
            String voto = votoSnap.getValue(String.class);

            if ("si".equalsIgnoreCase(voto)) {
                votosSi++;
            } else if ("no".equalsIgnoreCase(voto)) {
                votosNo++;
            } else {
                votosAbs++;
            }
        }

        String resultado;

        if (votosAbs >= votosSi && votosAbs >= votosNo) {
            resultado = "abstencion";
        } else if (votosSi == votosNo) {
            resultado = "abstencion";
        } else if (votosSi > votosNo) {
            resultado = "si";
        } else {
            resultado = "no";
        }

        if ("abstencion".equals(resultado)) {
            cerrarVotacionSinConsumir(rondaVotacion, snapshotVotacion, resultado);
            return;
        }

        boolean correctaFinal = "si".equals(resultado);

        String uidObjetivo = snapshotVotacion.child("uidObjetivo").getValue(String.class);
        String categoria = snapshotVotacion.child("categoria").getValue(String.class);
        String respuesta = snapshotVotacion.child("respuesta").getValue(String.class);

        if (uidObjetivo == null || categoria == null || respuesta == null) {
            cerrarVotacionSinConsumir(rondaVotacion, snapshotVotacion, "error");
            return;
        }

        if (correctaFinal) {
            calcularPuntosPorRespuestaVotada(rondaVotacion, categoria, respuesta, puntosCalculados -> {
                aplicarResultadoVotacion(
                        rondaVotacion,
                        snapshotVotacion,
                        uidObjetivo,
                        categoria,
                        true,
                        puntosCalculados,
                        resultado
                );
            });
        } else {
            aplicarResultadoVotacion(
                    rondaVotacion,
                    snapshotVotacion,
                    uidObjetivo,
                    categoria,
                    false,
                    0,
                    resultado
            );
        }
    }

    private void calcularPuntosPorRespuestaVotada(
            int rondaVotacion,
            String categoria,
            String respuestaEvaluada,
            PuntosVotacionCallback callback
    ) {
        if (respuestaEvaluada == null ||
                respuestaEvaluada.trim().isEmpty() ||
                respuestaEvaluada.equalsIgnoreCase("Sin respuesta")) {

            callback.onPuntosCalculados(0);
            return;
        }

        DatabaseReference respuestasRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("respuestas")
                .child("ronda_" + rondaVotacion);

        respuestasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotRespuestas) {
                int repeticiones = 0;

                for (DataSnapshot jugadorSnap : snapshotRespuestas.getChildren()) {
                    String respuesta = jugadorSnap.child(categoria).getValue(String.class);

                    if (respuesta != null &&
                            respuesta.trim().equalsIgnoreCase(respuestaEvaluada.trim())) {
                        repeticiones++;
                    }
                }

                int puntos = repeticiones > 1 ? 5 : 10;
                callback.onPuntosCalculados(puntos);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onPuntosCalculados(10);
            }
        });
    }

    private void aplicarResultadoVotacion(
            int rondaVotacion,
            DataSnapshot snapshotVotacion,
            String uidObjetivo,
            String categoria,
            boolean correctaFinal,
            int puntosFinales,
            String resultado
    ) {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        String idVotacion = snapshotVotacion.child("idVotacion").getValue(String.class);

        if (idVotacion == null) {
            idVotacion = "votacion_" + System.currentTimeMillis();
        }

        HashMap<String, Object> actualizaciones = new HashMap<>();

        actualizaciones.put(
                "validaciones/ronda_" + rondaVotacion + "/" + uidObjetivo + "/" + categoria + "/correcta",
                correctaFinal
        );

        actualizaciones.put(
                "validaciones/ronda_" + rondaVotacion + "/" + uidObjetivo + "/" + categoria + "/puntos",
                puntosFinales
        );

        actualizaciones.put(
                "validaciones/ronda_" + rondaVotacion + "/" + uidObjetivo + "/" + categoria + "/modificadaPorVotacion",
                true
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/resultado",
                resultado
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/puntosFinales",
                puntosFinales
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/correctaFinal",
                correctaFinal
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/uidObjetivo",
                uidObjetivo
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/categoria",
                categoria
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/respuesta",
                snapshotVotacion.child("respuesta").getValue(String.class)
        );

        for (DataSnapshot votoSnap : snapshotVotacion.child("votos").getChildren()) {
            String uidVotante = votoSnap.getKey();
            String voto = votoSnap.getValue(String.class);

            if (uidVotante != null && voto != null) {
                actualizaciones.put(
                        "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/votos/" + uidVotante,
                        voto
                );
            }
        }

        actualizaciones.put(
                "votacionesUsadas/ronda_" + rondaVotacion,
                ServerValue.increment(1)
        );

        actualizaciones.put("votacionActiva/ronda_" + rondaVotacion, null);

        nodoSala.updateChildren(actualizaciones).addOnCompleteListener(task -> {
            resolviendoVotacion = false;
            hayVotacionActivaEnRevision = false;
        });
    }

    private void cerrarVotacionSinConsumir(
            int rondaVotacion,
            DataSnapshot snapshotVotacion,
            String resultado
    ) {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        String idVotacion = snapshotVotacion.child("idVotacion").getValue(String.class);

        if (idVotacion == null) {
            idVotacion = "votacion_" + System.currentTimeMillis();
        }

        HashMap<String, Object> actualizaciones = new HashMap<>();

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/resultado",
                resultado
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/consumioVotacion",
                false
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/motivo",
                "Ganó abstención o hubo empate"
        );

        for (DataSnapshot votoSnap : snapshotVotacion.child("votos").getChildren()) {
            String uidVotante = votoSnap.getKey();
            String voto = votoSnap.getValue(String.class);

            if (uidVotante != null && voto != null) {
                actualizaciones.put(
                        "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/votos/" + uidVotante,
                        voto
                );
            }
        }

        actualizaciones.put("votacionActiva/ronda_" + rondaVotacion, null);

        nodoSala.updateChildren(actualizaciones).addOnCompleteListener(task -> {
            resolviendoVotacion = false;
            hayVotacionActivaEnRevision = false;
        });
    }

    private void cerrarRondaComoHost() {
        if (!esAnfitrion && !esCreador) {
            return;
        }

        if (cierreRondaSolicitado) {
            return;
        }

        cierreRondaSolicitado = true;

        final int rondaCerrada = rondaActual;

        DatabaseReference rondaProcesadaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("rondasProcesadas")
                .child("ronda_" + rondaCerrada);

        rondaProcesadaRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Boolean yaProcesada = currentData.getValue(Boolean.class);

                if (yaProcesada != null && yaProcesada) {
                    return Transaction.abort();
                }

                currentData.setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    Log.d("RONDA", "Error cerrando ronda: " + error.getMessage());
                    return;
                }

                if (committed) {
                    Log.d("RONDA", "Ronda " + rondaCerrada + " cerrada por el host");
                    esperarValidacionesCompletasYPrepararResumen(rondaCerrada);
                } else {
                    Log.d("RONDA", "La ronda ya había sido procesada");
                }
            }
        });
    }

    private void esperarValidacionesCompletasYPrepararResumen(int rondaCerrada) {
        DatabaseReference validacionesRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("validaciones")
                .child("ronda_" + rondaCerrada);

        final boolean[] yaSumo = {false};
        final ValueEventListener[] listenerHolder = new ValueEventListener[1];

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotValidaciones) {
                DatabaseReference jugadoresRef = FirebaseDatabase.getInstance()
                        .getReference("salas")
                        .child(idSala)
                        .child("jugadores");

                jugadoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                        long cantidadJugadores = snapshotJugadores.getChildrenCount();
                        long cantidadValidaciones = snapshotValidaciones.getChildrenCount();

                        if (!yaSumo[0] && cantidadJugadores > 0 && cantidadValidaciones >= cantidadJugadores) {
                            yaSumo[0] = true;

                            if (listenerHolder[0] != null) {
                                validacionesRef.removeEventListener(listenerHolder[0]);
                            }

                            esperarQueNoHayaVotacionActivaYSumar(rondaCerrada);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        listenerHolder[0] = listener;
        validacionesRef.addValueEventListener(listener);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!yaSumo[0]) {
                yaSumo[0] = true;

                if (listenerHolder[0] != null) {
                    validacionesRef.removeEventListener(listenerHolder[0]);
                }

                esperarQueNoHayaVotacionActivaYSumar(rondaCerrada);
            }
        }, 8000);
    }

    private void esperarQueNoHayaVotacionActivaYSumar(int rondaCerrada) {
        DatabaseReference votacionRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("votacionActiva")
                .child("ronda_" + rondaCerrada);

        votacionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean activa = snapshot.child("activa").getValue(Boolean.class);

                if (activa == null || !activa) {
                    votacionRef.removeEventListener(this);
                    sumarPuntajesDesdeFirebaseYPrepararResumen(rondaCerrada);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void sumarPuntajesDesdeFirebaseYPrepararResumen(int rondaCerrada) {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                HashMap<String, Integer> puntosPorJugador = new HashMap<>();

                DataSnapshot snapshotJugadores = snapshotSala.child("jugadores");
                DataSnapshot snapshotValidaciones = snapshotSala
                        .child("validaciones")
                        .child("ronda_" + rondaCerrada);

                for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                    String uidJugador = jugadorSnap.getKey();

                    if (uidJugador != null) {
                        puntosPorJugador.put(uidJugador, 0);
                    }
                }

                for (DataSnapshot jugadorValidacionSnap : snapshotValidaciones.getChildren()) {
                    String uidJugador = jugadorValidacionSnap.getKey();

                    if (uidJugador == null) {
                        continue;
                    }

                    int totalRonda = 0;

                    for (DataSnapshot categoriaSnap : jugadorValidacionSnap.getChildren()) {
                        totalRonda += leerEntero(categoriaSnap.child("puntos"), 0);
                    }

                    puntosPorJugador.put(uidJugador, totalRonda);
                }

                guardarPuntajesDeRonda(puntosPorJugador, rondaCerrada);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("PUNTAJES", "Error leyendo sala: " + error.getMessage());
            }
        });
    }

    private void guardarPuntajesDeRonda(HashMap<String, Integer> puntosPorJugador, int rondaCerrada) {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        HashMap<String, Object> actualizaciones = new HashMap<>();

        for (Map.Entry<String, Integer> entry : puntosPorJugador.entrySet()) {
            actualizaciones.put(
                    "puntajesRondas/ronda_" + rondaCerrada + "/" + entry.getKey(),
                    entry.getValue()
            );
        }

        nodoSala.updateChildren(actualizaciones).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                actualizarPuntajesAcumulados(puntosPorJugador, rondaCerrada);
            } else {
                marcarResumenRondaListo(rondaCerrada);
            }
        });
    }

    private void actualizarPuntajesAcumulados(
            HashMap<String, Integer> puntosPorJugador,
            int rondaCerrada
    ) {
        if (puntosPorJugador.isEmpty()) {
            marcarResumenRondaListo(rondaCerrada);
            return;
        }

        AtomicInteger pendientes = new AtomicInteger(puntosPorJugador.size());

        for (Map.Entry<String, Integer> entry : puntosPorJugador.entrySet()) {
            String uidJugador = entry.getKey();
            int puntosGanados = entry.getValue();

            DatabaseReference puntajeRef = FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .child("jugadores")
                    .child(uidJugador)
                    .child("puntajePartida");

            puntajeRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Object valorActual = currentData.getValue();

                    int puntajeActual = 0;

                    if (valorActual instanceof Number) {
                        puntajeActual = ((Number) valorActual).intValue();
                    }

                    currentData.setValue(puntajeActual + puntosGanados);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                    if (error != null) {
                        Log.d("PUNTAJES", "Error sumando puntaje: " + error.getMessage());
                    }

                    if (pendientes.decrementAndGet() == 0) {
                        marcarResumenRondaListo(rondaCerrada);
                    }
                }
            });
        }
    }

    private void marcarResumenRondaListo(int rondaCerrada) {
        HashMap<String, Object> actualizaciones = new HashMap<>();

        actualizaciones.put("resumenRondaListo/ronda_" + rondaCerrada, true);

        if (rondaCerrada >= rondasMaximas) {
            actualizaciones.put("fase", "resultados_finales");
        } else {
            actualizaciones.put("fase", "revision_puntuaciones");
        }

        FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .updateChildren(actualizaciones);
    }

    private void mostrarModalPuntuaciones(int rondaCerrada, boolean esFinal) {
        Dialog dialog = new Dialog(requireContext());

        View vista = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_puntuaciones, null);

        dialog.setContentView(vista);
        dialog.setCancelable(false);

        dialogPuntuacionesActual = dialog;

        TextView txtTitulo = vista.findViewById(R.id.txtTituloResultados);
        TextView txtGanador = vista.findViewById(R.id.txtGanadorResultados);
        RecyclerView rvTabla = vista.findViewById(R.id.rvTablaPuntuaciones);
        LinearLayout contenedorEncabezados = vista.findViewById(R.id.contenedorEncabezadosRondas);
        MaterialButton btnListo = vista.findViewById(R.id.btnVolverInicio);

        rvTabla.setLayoutManager(new LinearLayoutManager(requireContext()));

        configurarEncabezadosPuntuacion(contenedorEncabezados, rondasMaximas);

        if (esFinal) {
            txtTitulo.setText("RESULTADOS FINALES");
            btnListo.setText("SALIR DE PARTIDA");
        } else {
            txtTitulo.setText("PUNTUACIONES - RONDA " + rondaCerrada);
            btnListo.setText("LISTO PARA SIGUIENTE RONDA");
        }

        btnListo.setEnabled(false);
        btnListo.setText("CARGANDO...");

        cargarPuntuacionesDesdeFirebase(rondasMaximas, lista -> {
            if (!isAdded()) {
                return;
            }

            PuntuacionAdapter adapter = new PuntuacionAdapter(lista, rondasMaximas);
            rvTabla.setAdapter(adapter);

            txtGanador.setText(obtenerTextoGanador(lista));

            btnListo.setEnabled(true);

            if (esFinal) {
                btnListo.setText("SALIR DE PARTIDA");
            } else {
                btnListo.setText("LISTO PARA SIGUIENTE RONDA");
            }
        });

        btnListo.setOnClickListener(v -> {
            marcarRevisionLista(rondaCerrada);

            btnListo.setEnabled(false);

            if (esFinal) {
                btnListo.setText("ESPERANDO A LOS DEMÁS...");
            } else {
                btnListo.setText("ESPERANDO JUGADORES...");
            }
        });

        escucharJugadoresListosRevision(rondaCerrada, esFinal);

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void configurarEncabezadosPuntuacion(LinearLayout contenedor, int cantidadRondas) {
        contenedor.removeAllViews();

        int rondas = cantidadRondas;

        if (rondas < 1) {
            rondas = 1;
        }

        if (rondas > 8) {
            rondas = 8;
        }

        for (int i = 1; i <= rondas; i++) {
            TextView textView = new TextView(requireContext());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(60),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            textView.setLayoutParams(params);
            textView.setText("R" + i);
            textView.setGravity(Gravity.CENTER);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextSize(16);
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blanco));

            contenedor.addView(textView);
        }
    }

    private void cargarPuntuacionesDesdeFirebase(int cantidadRondas, PuntuacionesCallback callback) {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                List<PuntuacionJugador> lista = new ArrayList<>();

                DataSnapshot snapshotJugadores = snapshotSala.child("jugadores");
                DataSnapshot snapshotPuntajesRondas = snapshotSala.child("puntajesRondas");

                for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                    String uid = jugadorSnap.getKey();

                    if (uid == null) {
                        continue;
                    }

                    String nombre = jugadorSnap.child("nombre").getValue(String.class);

                    if (nombre == null || nombre.trim().isEmpty()) {
                        nombre = "Jugador";
                    }

                    List<Integer> puntajes = new ArrayList<>();
                    int total = 0;

                    for (int ronda = 1; ronda <= cantidadRondas; ronda++) {
                        Integer puntos = snapshotPuntajesRondas
                                .child("ronda_" + ronda)
                                .child(uid)
                                .getValue(Integer.class);

                        puntajes.add(puntos);

                        if (puntos != null) {
                            total += puntos;
                        }
                    }

                    lista.add(new PuntuacionJugador(0, uid, nombre, puntajes, total));
                }

                Collections.sort(lista, (a, b) -> Integer.compare(b.getTotal(), a.getTotal()));

                for (int i = 0; i < lista.size(); i++) {
                    lista.get(i).setPosicion(i + 1);
                }

                callback.onCargadas(lista);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onCargadas(new ArrayList<>());
            }
        });
    }

    private String obtenerTextoGanador(List<PuntuacionJugador> lista) {
        if (lista == null || lista.isEmpty()) {
            return "Sin puntuaciones todavía.";
        }

        int mayorPuntaje = lista.get(0).getTotal();

        if (mayorPuntaje <= 0) {
            return "Aún no hay puntos acumulados.";
        }

        List<String> ganadores = new ArrayList<>();

        for (PuntuacionJugador jugador : lista) {
            if (jugador.getTotal() == mayorPuntaje) {
                ganadores.add(jugador.getNombre());
            }
        }

        if (ganadores.size() == 1) {
            return "Ganador: " + ganadores.get(0) + " con " + mayorPuntaje + " puntos";
        }

        return "Empate: " + unirNombres(ganadores) + " con " + mayorPuntaje + " puntos";
    }

    private String unirNombres(List<String> nombres) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < nombres.size(); i++) {
            builder.append(nombres.get(i));

            if (i < nombres.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    private void marcarRevisionLista(int rondaCerrada) {
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("revisionLista")
                .child("ronda_" + rondaCerrada)
                .child(uid)
                .setValue(true);
    }

    private void escucharJugadoresListosRevision(int rondaCerrada, boolean esFinal) {
        limpiarListenerRevision();

        nodoRevisionListaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("revisionLista")
                .child("ronda_" + rondaCerrada);

        listenerRevisionLista = nodoRevisionListaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotRevision) {
                DatabaseReference jugadoresRef = FirebaseDatabase.getInstance()
                        .getReference("salas")
                        .child(idSala)
                        .child("jugadores");

                jugadoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                        long cantidadJugadores = snapshotJugadores.getChildrenCount();
                        long cantidadListos = snapshotRevision.getChildrenCount();

                        if (cantidadJugadores > 0 && cantidadListos >= cantidadJugadores) {
                            if (esAnfitrion || esCreador) {
                                limpiarListenerRevision();
                                iniciarNuevaRondaOFinalizar(rondaCerrada);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void limpiarListenerRevision() {
        if (nodoRevisionListaRef != null && listenerRevisionLista != null) {
            nodoRevisionListaRef.removeEventListener(listenerRevisionLista);
            listenerRevisionLista = null;
            nodoRevisionListaRef = null;
        }
    }

    private void iniciarNuevaRondaOFinalizar(int rondaCerrada) {
        if (rondaCerrada >= rondasMaximas) {
            resetearSalaParaLobby();
        } else {
            iniciarSiguienteRonda(rondaCerrada);
        }
    }

    private void iniciarSiguienteRonda(int rondaCerrada) {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        int siguienteRonda = rondaCerrada + 1;
        String nuevaLetra = LetraSala.ElegirLetraAlAzar(letraElegida);

        HashMap<String, Object> actualizaciones = new HashMap<>();

        actualizaciones.put("rondaActual", siguienteRonda);
        actualizaciones.put("letraActual", nuevaLetra);
        actualizaciones.put("inicioRonda", ServerValue.TIMESTAMP);
        actualizaciones.put("inicioRondaDe", siguienteRonda);
        actualizaciones.put("estadoPartida", "En juego");
        actualizaciones.put("fase", "jugando");

        actualizaciones.put("bastaPresionado", false);
        actualizaciones.put("bastaPresionadoPor", null);
        actualizaciones.put("bastaTimeStamp", null);

        actualizaciones.put("cambioRondaTimestamp", ServerValue.TIMESTAMP);

        nodoSala.updateChildren(actualizaciones).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("RONDA", "Nueva ronda iniciada: " + siguienteRonda + " letra: " + nuevaLetra);
            } else {
                Log.d("RONDA", "Error iniciando nueva ronda");
            }
        });
    }

    private void resetearSalaParaLobby() {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        String nuevaLetra = LetraSala.ElegirLetraAlAzar(letraElegida);

        HashMap<String, Object> actualizaciones = new HashMap<>();

        actualizaciones.put("estadoPartida", "Esperando jugadores");
        actualizaciones.put("fase", "lobby");
        actualizaciones.put("rondaActual", 0);
        actualizaciones.put("letraActual", nuevaLetra);
        actualizaciones.put("inicioRonda", null);
        actualizaciones.put("inicioRondaDe", null);

        actualizaciones.put("bastaPresionado", false);
        actualizaciones.put("bastaPresionadoPor", null);
        actualizaciones.put("bastaTimeStamp", null);

        actualizaciones.put("respuestas", null);
        actualizaciones.put("validaciones", null);
        actualizaciones.put("rondasProcesadas", null);
        actualizaciones.put("revisionLista", null);
        actualizaciones.put("resumenRondaListo", null);
        actualizaciones.put("puntajesRondas", null);

        actualizaciones.put("votacionActiva", null);
        actualizaciones.put("votacionesUsadas", null);
        actualizaciones.put("historialVotaciones", null);

        actualizaciones.put("finPartida", ServerValue.TIMESTAMP);

        nodoSala.child("jugadores").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                    String uid = jugadorSnap.getKey();

                    if (uid != null) {
                        actualizaciones.put("jugadores/" + uid + "/estaListo", false);
                        actualizaciones.put("jugadores/" + uid + "/puntajePartida", 0);
                    }
                }

                nodoSala.updateChildren(actualizaciones).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FINAL", "Sala reseteada correctamente");
                    } else {
                        Log.d("FINAL", "Error reseteando sala");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private int dp(int valor) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(valor * density);
    }

    private void volverAlInicio() {
        if (!isAdded()) {
            return;
        }

        Intent intent = new Intent(requireContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String mapearCategoriaParaApi(String categoriaApp) {
        if (categoriaApp == null) return "";

        switch (categoriaApp.toLowerCase(Locale.ROOT)) {
            case "animal": return "animales";
            case "color": return "colores";
            case "fruta": return "frutas";
            case "país":
            case "pais": return "paises";
            case "cosa": return "cosas";
            case "nombre": return "nombre";
            case "apellido": return "apellido";
            default: return categoriaApp.toLowerCase(Locale.ROOT);
        }
    }

    private String mapearCategoriaDesdeApi(String categoriaApi) {
        if (categoriaApi == null) return "";

        switch (categoriaApi.toLowerCase(Locale.ROOT)) {
            case "animales": return "Animal";
            case "colores": return "Color";
            case "frutas": return "Fruta";
            case "paises": return "País";
            case "cosas": return "Cosa";
            case "nombre": return "Nombre";
            case "apellido": return "Apellido";
            default: return categoriaApi;
        }
    }
}