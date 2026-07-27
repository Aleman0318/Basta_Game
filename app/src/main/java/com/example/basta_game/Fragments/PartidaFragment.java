package com.example.basta_game.Fragments;

import android.app.Dialog;
import android.graphics.Color;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.basta_game.Adapters.VictimaAdapter;
import com.example.basta_game.comodines.BaseComodin;
import com.example.basta_game.comodines.ComodinCambioDeFlujo;
import com.example.basta_game.comodines.ComodinDobleONada;
import com.example.basta_game.comodines.ComodinMarcaDeLaMuerte;
import com.example.basta_game.comodines.ComodinRuletaCaotica;
import com.example.basta_game.models.ApiService;
import com.example.basta_game.models.ItemRespuesta;
import com.example.basta_game.models.ResultadoApi;
import com.example.basta_game.models.RetrofitClient;
import com.example.basta_game.models.ValidacionRequest;
import com.example.basta_game.models.ValidacionResponse;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.basta_game.Adapters.CategoriaPartidaAdapter;
import com.example.basta_game.Adapters.ComodinPartidaAdapter;
import com.example.basta_game.Adapters.JugadorPartidaAdapter;
import com.example.basta_game.Adapters.PuntuacionAdapter;
import com.example.basta_game.Adapters.TarjetasValidacionAdapter;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.models.ComodinPartida;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.PuntuacionJugador;
import com.example.basta_game.models.RespuestaValidacion;
import com.example.basta_game.utils.Animations;
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

    // false = modo prueba sin API.
// true  = modo real con cache + API.
    private static final boolean USAR_API_VALIDACION = false;

    // En modo sin API, una respuesta debe tener mínimo 3 letras reales.
    private static final int MIN_LETRAS_RESPUESTA_LOCAL = 3;
    private static final String NODO_USUARIOS = "usuarios";
    private static final String NODO_HISTORIAL_PARTIDAS = "historialPartidas";

    private static final int MONEDAS_PRIMER_LUGAR = 100;
    private static final int MONEDAS_SEGUNDO_LUGAR = 70;
    private static final int MONEDAS_TERCER_LUGAR = 45;
    private static final int MONEDAS_CUARTO_LUGAR = 25;
    private static final int MONEDAS_PARTICIPACION = 15;

    private ValueEventListener listenerEventoCaos;
    private boolean historialGuardandose = false;
    private boolean miHistorialFinalGuardado = false;

    private String codigoSala;
    private int tiempoSeconds;
    private String idSala;
    private int jugadoresMaximos;
    private boolean esAnfitrion;
    private int rondasMaximas;
    private boolean esCreador;
    private int rondaActual;
    private boolean bastaActivado = false;

    private MaterialButton btnAbandonar;

    private ArrayList<String> listaNombresCategorias;

    private DatabaseReference salaRef;

    private boolean sonidoFinalReproducido = false;
    private boolean bastaSonidoReproducido = false;
    private boolean sonidoResultadoRondaReproducido = false;

    private CountDownTimer cronometroRonda;
    private int versionCronometro = 0;

    private TextView txtCodigoSala;
    private TextView txtRondaActual;
    private TextView txtEstadoPartida;
    private TextView txtTiempoRestante;
    private String letraElegida;
    private TextView txtInstruccionLetra;

    private ImageView imgLetraActual;

    // --- VARIABLES DE COOLDOWN ---
    private java.util.HashSet<String> comodinesUsadosEstaRonda = new java.util.HashSet<>();

    private AppCompatButton btnBasta;
    private MaterialButton btnComodines;

    private ScrollView scrollPartida;
    private RecyclerView rvJugadoresPartida;
    private RecyclerView rvCategoriasJuego;

    private ValueEventListener listenerMiRolHost;
    private DatabaseReference nodoMiJugadorPartidaRef;
    private boolean yaVolvioAlLobby = false;

    private HashMap<String, String> respuestasJugador = new HashMap<>();

    private boolean modalValidacionMostrado = false;
    private boolean respuestasGuardadas = false;
    private boolean cierreRondaSolicitado = false;

    private ValueEventListener listenerRespuestasValidacion;
    private DatabaseReference nodoRespuestasRondaRef;

    private ValueEventListener listenerPuntuacionesEnVivo;
    private ValueEventListener listenerEstadoPartida;
    private ValueEventListener listenerFase;
    private ValueEventListener listenerRondaActual;
    private ValueEventListener listenerLetraActual;
    private ValueEventListener listenerCantidadRondas;
    private ValueEventListener listenerTiempoRonda;

    private ValueEventListener listenerBasta;
    private DatabaseReference nodoBastaTimeStampRef;

    private Dialog dialogAnuncioMinijuego;

    private ValueEventListener listenerJugadoresPartida;
    private DatabaseReference nodoJugadoresPartidaRef;

    private ValueEventListener listenerRevisionLista;
    private DatabaseReference nodoRevisionListaRef;

    private ValueEventListener listenerVotacionActiva;
    private DatabaseReference nodoVotacionActivaRef;

    // --- VARIABLES PARA EL MINIJUEGO (MARCA DE LA MUERTE) ---
    private ValueEventListener listenerMinijuego;
    private DatabaseReference nodoMinijuegoRef;
    private Dialog dialogMinijuegoActual;

    private ValueEventListener listenerValidacionesGlobales;
    private DatabaseReference nodoValidacionesGlobalesRef;

    private ValueEventListener listenerResultadosVotacionPalabras;
    private DatabaseReference nodoResultadosVotacionPalabrasRef;

    private ValueEventListener listenerCooldownVotacion;
    private DatabaseReference nodoCooldownVotacionRef;

    private ValueEventListener listenerRevisionCategorias;
    private DatabaseReference nodoRevisionCategoriasRef;

    private Dialog dialogValidacionActual;
    private Dialog dialogPuntuacionesActual;
    private Dialog dialogVotacionActual;
    private Dialog dialogComodinesActual;

    private TarjetasValidacionAdapter adapterValidacionActual;

    private String idVotacionModalActual = null;
    private boolean resolviendoVotacion = false;
    private boolean hayVotacionActivaEnRevision = false;
    private int cantidadJugadoresEnSala = 0;

    private ValueEventListener listenerEsperandoLobbyFinal;
    private DatabaseReference nodoEsperandoLobbyFinalRef;

    private FirebaseAuth auth;

    private View notificationPartida;

    private Long ultimoBastaTimestampNotificado = null;
    private int ultimaRondaBastaNotificada = -1;
    private String faseActual = "jugando";
    private String estadoPartidaActual = "En juego";

    private static final String COMODIN_CONGELADOR = "congelador";
    private static final String COMODIN_DOBLE_O_NADA = "doble_o_nada";
    private static final String COMODIN_UNO_REVERSA = "uno_reversa";
    private static final int DURACION_CONGELADOR_MS = 8000;
    private static final long COOLDOWN_VOTACION_MS = 5000;
    private static final double PORCENTAJE_MINIMO_DOBLE_O_NADA = 0.80;
    private static final int PUNTOS_MAXIMOS_POR_CATEGORIA = 10;
    private static final int CASTIGO_DOBLE_O_NADA = 15;
    private static final long ESPERA_MAXIMA_INICIO_RONDA_MS = 8000;

    private Handler handlerComodines = new Handler(Looper.getMainLooper());
    private Runnable runnableDesbloquearBasta;
    private boolean bastaBloqueadoPorComodin = false;
    private String ultimoCongeladorProcesado = null;

    private boolean reparacionRondaCeroSolicitada = false;
    private final Handler handlerInicioRonda = new Handler(Looper.getMainLooper());

    private ValueEventListener listenerCongeladorActivo;
    private DatabaseReference nodoCongeladorActivoRef;

    private interface PuntuacionesCallback {
        void onCargadas(List<PuntuacionJugador> lista);
    }

    private interface OperacionTerminadaCallback {
        void onTermino();
    }

    private interface PuntosVotacionCallback {
        void onPuntosCalculados(int puntos);
    }

    private interface ResultadoComodinCallback {
        void onResultado(boolean exito);
    }

    private interface NombreJugadorCallback {
        void onNombre(String nombre);
    }

    private interface HoraServidorCallback {
        void onHora(long horaServidor);
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
        marcarEntradaPartidaActual();

        prepararEntradaNuevaPartida();

        cargarDatosIniciales();

        if (rondaActual > 0) {
            resetearEstadosParaPartidaActiva();
        }

        escucharMinijuegoActivo();

        escucharCambiosDeRonda();
        escucharMiRolHostEnPartida();

        setearCronometroGlobal();
        escucharBastaParaCronometro();
        escucharVotacionActiva();
        escucharCongeladorActivo();
        configurarBotones();
        ObtenerJugadoresYLlenarPartida(idSala);
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
        btnComodines = view.findViewById(R.id.btnComodines);
        scrollPartida = view.findViewById(R.id.scrollPartida);
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
        rvCategoriasJuego.setNestedScrollingEnabled(false);

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

        rvCategoriasJuego.setAdapter(new CategoriaPartidaAdapter(
                listaCategorias,
                respuestasJugador,
                this::subirCampoRespuestaAlTeclado
        ));
    }


    private void subirCampoRespuestaAlTeclado(View campoEnfocado) {
        if (scrollPartida == null || campoEnfocado == null || !isAdded()) {
            return;
        }

        scrollPartida.postDelayed(() -> {
            if (scrollPartida == null || campoEnfocado == null || !isAdded()) {
                return;
            }

            int[] posicionScroll = new int[2];
            int[] posicionCampo = new int[2];

            scrollPartida.getLocationOnScreen(posicionScroll);
            campoEnfocado.getLocationOnScreen(posicionCampo);

            int campoDentroDelScroll = posicionCampo[1] - posicionScroll[1];
            int margenSuperiorSeguro = dp(135);

            int nuevoScrollY = scrollPartida.getScrollY() + campoDentroDelScroll - margenSuperiorSeguro;

            if (nuevoScrollY < 0) {
                nuevoScrollY = 0;
            }

            scrollPartida.smoothScrollTo(0, nuevoScrollY);
        }, 260);
    }

    private void configurarBotones() {
        if (btnComodines != null) {
            // Le pasamos FALSE porque este botón se presiona DURANTE el juego, no al final.
            btnComodines.setOnClickListener(v -> mostrarDialogoComodines(false));
        }

        btnBasta.setOnClickListener(v -> {
            if (bastaActivado ||
                    respuestasGuardadas ||
                    modalValidacionMostrado ||
                    cierreRondaSolicitado ||
                    !esFaseJugando()) {
                return;
            }

            if (bastaBloqueadoPorComodin) {
                NotificationHelper.show(
                        notificationPartida,
                        "BASTA está congelado por un comodín",
                        NotificationHelper.WARNING
                );
                return;
            }

            bloquearBotonBastaPorBasta();

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
                        desbloquearBotonBastaSiCorresponde();
                        return;
                    }

                    if (committed) {
                        bastaActivado = true;
                        bloquearBotonBastaPorBasta();

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
                        desbloquearBotonBastaSiCorresponde();
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

    private void marcarEntradaPartidaActual() {
        if (salaRef == null || !VerificacionesAuth.VerificarInicioSesion(auth) || rondaActual <= 0) {
            return;
        }

        String miUid = auth.getCurrentUser().getUid();

        HashMap<String, Object> datosEntrada = new HashMap<>();
        datosEntrada.put("entradaPartida/ronda_" + rondaActual + "/" + miUid + "/dentro", true);
        datosEntrada.put("entradaPartida/ronda_" + rondaActual + "/" + miUid + "/timestamp", ServerValue.TIMESTAMP);

        salaRef.updateChildren(datosEntrada);
    }

    private void bloquearBotonBastaPorBasta() {
        if (btnBasta == null) {
            return;
        }

        btnBasta.setEnabled(false);
        btnBasta.setClickable(false);
        btnBasta.setAlpha(0.45f);
        btnBasta.setText("FINALIZANDO...");
    }

    private void desbloquearBotonBastaSiCorresponde() {
        if (btnBasta == null) {
            return;
        }

        if (bastaActivado || bastaBloqueadoPorComodin || respuestasGuardadas || modalValidacionMostrado || cierreRondaSolicitado) {
            return;
        }

        btnBasta.setEnabled(true);
        btnBasta.setClickable(true);
        btnBasta.setAlpha(1f);
        btnBasta.setText("¡BASTA!");
    }

    private void resetearEstadosParaPartidaActiva() {
        respuestasGuardadas = false;
        modalValidacionMostrado = false;
        cierreRondaSolicitado = false;
        bastaActivado = false;

        sonidoFinalReproducido = false;
        bastaSonidoReproducido = false;
        sonidoResultadoRondaReproducido = false;

        idVotacionModalActual = null;
        resolviendoVotacion = false;
        hayVotacionActivaEnRevision = false;

        bastaBloqueadoPorComodin = false;
        ultimoCongeladorProcesado = null;

        faseActual = "jugando";
        estadoPartidaActual = "En juego";

        if (handlerComodines != null && runnableDesbloquearBasta != null) {
            handlerComodines.removeCallbacks(runnableDesbloquearBasta);
            runnableDesbloquearBasta = null;
        }

        desbloquearBotonBastaSiCorresponde();
    }

    private boolean esFaseJugando() {
        return rondaActual > 0 &&
                (
                        faseActual == null ||
                                faseActual.equalsIgnoreCase("jugando") ||
                                faseActual.equalsIgnoreCase("En juego") ||
                                "En juego".equalsIgnoreCase(estadoPartidaActual)
                );
    }

    private boolean rondaSigueSiendoActual(int rondaEsperada) {
        return isAdded() && rondaActual == rondaEsperada;
    }

    private void escucharCambiosDeRonda() {
        if (salaRef == null) {
            return;
        }

        listenerEstadoPartida = salaRef.child("estadoPartida").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String estado = snapshot.getValue(String.class);

                if (estado != null) {
                    estadoPartidaActual = estado;
                    txtEstadoPartida.setText("● " + estado);
                }

                if ("Esperando jugadores".equalsIgnoreCase(estadoPartidaActual) && rondaActual == 0) {
                    volverAlLobbyDespuesDePartida();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        listenerFase = salaRef.child("fase").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fase = snapshot.getValue(String.class);

                if (fase != null) {
                    faseActual = fase;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        listenerCantidadRondas = salaRef.child("cantidadRondas").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rondasMaximas = leerEntero(snapshot, rondasMaximas);

                if (isAdded()) {
                    txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        listenerTiempoRonda = salaRef.child("tiempoRonda").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tiempoSeconds = leerEntero(snapshot, tiempoSeconds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        listenerLetraActual = salaRef.child("letraActual").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String letra = snapshot.getValue(String.class);

                if (letra != null && !letra.trim().isEmpty()) {
                    letraElegida = letra;

                    if (isAdded()) {
                        mostrarLetra(letraElegida);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        listenerRondaActual = salaRef.child("rondaActual").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }

                int rondaFirebase = leerEntero(snapshot, rondaActual);

                if ("Esperando jugadores".equalsIgnoreCase(estadoPartidaActual) && rondaFirebase == 0) {
                    rondaActual = 0;
                    volverAlLobbyDespuesDePartida();
                    return;
                }

                if (rondaFirebase != rondaActual && rondaFirebase > 0) {
                    rondaActual = rondaFirebase;

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded()) {
                            reiniciarPartidaParaNuevaRonda();
                        }
                    }, 250);
                    return;
                }

                txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("RONDA", "Error escuchando rondaActual: " + error.getMessage());
            }
        });
    }


    private void reiniciarPartidaParaNuevaRonda() {

        comodinesUsadosEstaRonda.clear();

        escucharMinijuegoActivo();
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
        estadoPartidaActual = "En juego";
        yaVolvioAlLobby = false;
        ultimoBastaTimestampNotificado = null;
        ultimaRondaBastaNotificada = -1;

        idVotacionModalActual = null;
        resolviendoVotacion = false;
        hayVotacionActivaEnRevision = false;
        bastaBloqueadoPorComodin = false;
        ultimoCongeladorProcesado = null;

        if (handlerComodines != null && runnableDesbloquearBasta != null) {
            handlerComodines.removeCallbacks(runnableDesbloquearBasta);
            runnableDesbloquearBasta = null;
        }

        respuestasJugador.clear();

        if (cronometroRonda != null) {
            cronometroRonda.cancel();
            cronometroRonda = null;
        }

        if (handlerInicioRonda != null) {
            handlerInicioRonda.removeCallbacksAndMessages(null);
        }

        if (nodoRespuestasRondaRef != null && listenerRespuestasValidacion != null) {
            nodoRespuestasRondaRef.removeEventListener(listenerRespuestasValidacion);
            listenerRespuestasValidacion = null;
            nodoRespuestasRondaRef = null;
        }

        limpiarListenerValidacionesGlobales();
        limpiarListenerResultadosVotacionPalabras();
        limpiarListenerCooldownVotacion();
        limpiarListenerRevisionCategorias();
        limpiarListenerVotacionActiva();
        limpiarListenerCongeladorActivo();

        if (handlerComodines != null && runnableDesbloquearBasta != null) {
            handlerComodines.removeCallbacks(runnableDesbloquearBasta);
            runnableDesbloquearBasta = null;
        }

        if (dialogComodinesActual != null && dialogComodinesActual.isShowing()) {
            dialogComodinesActual.dismiss();
            dialogComodinesActual = null;
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

        desbloquearBotonBastaSiCorresponde();

        cargarCategoriasJuego();
        marcarEntradaPartidaActual();
        setearCronometroGlobal();
        escucharVotacionActiva();
        escucharCongeladorActivo();

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
            case "pelicula": return R.drawable.ic_pelicula;
            case "película": return R.drawable.ic_pelicula;
            case "cancion": return R.drawable.ic_cancion;
            case "canción": return R.drawable.ic_cancion;
            default: return R.drawable.perfil_menuu;
        }
    }
    public void setearCronometroGlobal() {
        if (salaRef == null || !isAdded()) {
            return;
        }

        final int rondaSolicitada = rondaActual > 0 ? rondaActual : 1;

        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                if (!isAdded() || yaVolvioAlLobby) {
                    return;
                }

                String estadoFirebase = snapshotSala.child("estadoPartida").getValue(String.class);
                String faseFirebase = snapshotSala.child("fase").getValue(String.class);

                if (estadoFirebase != null) {
                    estadoPartidaActual = estadoFirebase;
                    txtEstadoPartida.setText("● " + estadoFirebase);
                }

                if (faseFirebase != null) {
                    faseActual = faseFirebase;
                }

                int rondaFirebase = leerEntero(snapshotSala.child("rondaActual"), rondaSolicitada);

                boolean firebaseDiceJugando =
                        "jugando".equalsIgnoreCase(faseFirebase) ||
                                "En juego".equalsIgnoreCase(estadoFirebase);

                if (rondaFirebase <= 0 && firebaseDiceJugando) {
                    Log.d("TIEMPO", "Sala dice En juego pero rondaFirebase=0. Reparando...");
                    repararRondaCeroSiHaceFalta("setearCronometroGlobal");
                    return;
                }

                boolean estaEnLobby =
                        rondaFirebase <= 0 ||
                                "lobby".equalsIgnoreCase(faseFirebase) ||
                                "Esperando jugadores".equalsIgnoreCase(estadoFirebase);

                if (estaEnLobby) {
                    Log.d("TIEMPO", "La sala todavía está en lobby o sin ronda activa. rondaFirebase=" + rondaFirebase);
                    return;
                }

                rondaActual = rondaFirebase;
                txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
                marcarEntradaPartidaActual();

                int tiempoFirebase = leerEntero(snapshotSala.child("tiempoRonda"), tiempoSeconds);

                if (tiempoFirebase > 0) {
                    tiempoSeconds = tiempoFirebase;
                }

                Long inicioRonda = snapshotSala.child("inicioRonda").getValue(Long.class);
                int inicioRondaDe = leerEntero(snapshotSala.child("inicioRondaDe"), -1);
                int inicioRondaListoDe = leerEntero(snapshotSala.child("inicioRondaListoDe"), -1);
                Boolean bastaPresionadoFirebase = snapshotSala.child("bastaPresionado").getValue(Boolean.class);

                if (inicioRonda == null || inicioRondaDe != rondaFirebase || inicioRondaListoDe != rondaFirebase) {
                    Log.d("TIEMPO", "inicioRonda todavía no está confirmado. rondaFirebase=" +
                            rondaFirebase + " inicioRondaDe=" + inicioRondaDe +
                            " inicioRondaListoDe=" + inicioRondaListoDe);

                    prepararInicioRondaSiHaceFalta(rondaFirebase);
                    reintentarCronometro(rondaFirebase);
                    return;
                }

                faseActual = "jugando";
                estadoPartidaActual = "En juego";
                txtEstadoPartida.setText("● En juego");

                if (Boolean.TRUE.equals(bastaPresionadoFirebase) || bastaActivado) {
                    bastaActivado = true;
                    bloquearBotonBastaPorBasta();
                } else {
                    desbloquearBotonBastaSiCorresponde();
                }

                final int rondaDelCronometro = rondaFirebase;

                FirebaseDatabase.getInstance()
                        .getReference(".info/serverTimeOffset")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot offsetSnapshot) {
                                if (!isAdded() || yaVolvioAlLobby) {
                                    return;
                                }

                                Long offset = offsetSnapshot.getValue(Long.class);

                                if (offset == null) {
                                    offset = 0L;
                                }

                                long horaActualServidor = System.currentTimeMillis() + offset;
                                long duracionRondaMs = tiempoSeconds * 1000L;
                                long tiempoPasadoMs = horaActualServidor - inicioRonda;

                                if (tiempoPasadoMs < 0) {
                                    tiempoPasadoMs = 0;
                                }

                                long tiempoRestanteMs = duracionRondaMs - tiempoPasadoMs;

                                Log.d("TIEMPO", "Iniciando cronómetro. ronda=" + rondaDelCronometro +
                                        " tiempoRestanteMs=" + tiempoRestanteMs +
                                        " tiempoSeconds=" + tiempoSeconds);

                                if (tiempoRestanteMs <= 0) {
                                    txtTiempoRestante.setText("00:00");
                                    guardarRespuestasJugador();
                                    return;
                                }

                                iniciarCronometro(tiempoRestanteMs, rondaDelCronometro);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.d("TIEMPO", "No se pudo leer offset. Usando tiempo local.");
                                iniciarCronometro(tiempoSeconds * 1000L, rondaDelCronometro);
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("TIEMPO", "Error leyendo sala para cronómetro: " + error.getMessage());
            }
        });
    }

    private void prepararInicioRondaSiHaceFalta(int rondaParaIniciar) {
        if (salaRef == null || rondaParaIniciar <= 0) {
            return;
        }

        if (!esAnfitrion && !esCreador) {
            return;
        }

        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                if (!isAdded() || yaVolvioAlLobby) {
                    return;
                }

                int inicioRondaListoDe = leerEntero(snapshotSala.child("inicioRondaListoDe"), -1);

                if (inicioRondaListoDe == rondaParaIniciar) {
                    return;
                }

                long cantidadJugadores = snapshotSala.child("jugadores").getChildrenCount();
                long jugadoresDentro = snapshotSala
                        .child("entradaPartida")
                        .child("ronda_" + rondaParaIniciar)
                        .getChildrenCount();

                long esperaDesde = leerLong(
                        snapshotSala
                                .child("esperaInicioRonda")
                                .child("ronda_" + rondaParaIniciar)
                                .child("desde"),
                        0L
                );

                long ahora = System.currentTimeMillis();

                if (esperaDesde <= 0) {
                    HashMap<String, Object> espera = new HashMap<>();
                    espera.put("esperaInicioRonda/ronda_" + rondaParaIniciar + "/desde", ServerValue.TIMESTAMP);
                    espera.put("esperaInicioRonda/ronda_" + rondaParaIniciar + "/jugadoresEsperados", cantidadJugadores);
                    espera.put("esperaInicioRonda/ronda_" + rondaParaIniciar + "/jugadoresDentro", jugadoresDentro);

                    salaRef.updateChildren(espera);
                    return;
                }

                boolean todosEntraron = cantidadJugadores > 0 && jugadoresDentro >= cantidadJugadores;
                boolean esperaAgotada = (ahora - esperaDesde) >= ESPERA_MAXIMA_INICIO_RONDA_MS;

                if (!todosEntraron && !esperaAgotada) {
                    HashMap<String, Object> progreso = new HashMap<>();
                    progreso.put("esperaInicioRonda/ronda_" + rondaParaIniciar + "/jugadoresEsperados", cantidadJugadores);
                    progreso.put("esperaInicioRonda/ronda_" + rondaParaIniciar + "/jugadoresDentro", jugadoresDentro);
                    salaRef.updateChildren(progreso);
                    return;
                }

                HashMap<String, Object> actualizaciones = new HashMap<>();

                actualizaciones.put("estadoPartida", "En juego");
                actualizaciones.put("fase", "jugando");
                actualizaciones.put("rondaActual", rondaParaIniciar);
                actualizaciones.put("inicioRonda", ServerValue.TIMESTAMP);
                actualizaciones.put("inicioRondaDe", rondaParaIniciar);
                actualizaciones.put("inicioRondaListoDe", rondaParaIniciar);
                actualizaciones.put("bastaPresionado", false);
                actualizaciones.put("bastaPresionadoPor", null);
                actualizaciones.put("bastaTimeStamp", null);
                actualizaciones.put("cambioRondaTimestamp", ServerValue.TIMESTAMP);
                actualizaciones.put("esperaInicioRonda/ronda_" + rondaParaIniciar + "/iniciada", true);

                salaRef.updateChildren(actualizaciones).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("TIEMPO", "Host confirmó inicio exacto para ronda " + rondaParaIniciar);
                    } else {
                        Log.d("TIEMPO", "No se pudo confirmar inicio de ronda");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void reintentarCronometro(int rondaEsperada) {
        handlerInicioRonda.postDelayed(() -> {
            if (!isAdded() || yaVolvioAlLobby) {
                return;
            }

            Log.d("TIEMPO", "Reintentando cronómetro para ronda " + rondaEsperada);
            setearCronometroGlobal();
        }, 700);
    }

    private void iniciarCronometro(long tiempoRestanteMs, int rondaDelCronometro) {
        if (!isAdded() || tiempoRestanteMs <= 0) {
            return;
        }

        if (cronometroRonda != null) {
            cronometroRonda.cancel();
        }

        rondaActual = rondaDelCronometro;
        final int versionActual = ++versionCronometro;

        txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
        txtTiempoRestante.setText(formatearTiempo((tiempoRestanteMs + 999) / 1000));

        cronometroRonda = new CountDownTimer(tiempoRestanteMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isAdded() || yaVolvioAlLobby || versionActual != versionCronometro) {
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
                if (!isAdded() || yaVolvioAlLobby || versionActual != versionCronometro) {
                    return;
                }

                faseActual = "jugando";
                estadoPartidaActual = "En juego";

                txtTiempoRestante.setText("00:00");
                guardarRespuestasJugador();
            }
        };

        cronometroRonda.start();
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
            nodoRespuestasRondaRef = null;
        }

        limpiarListenerValidacionesGlobales();
        limpiarListenerResultadosVotacionPalabras();
        limpiarListenerCooldownVotacion();
        limpiarListenerRevisionCategorias();
        limpiarListenerVotacionActiva();
        limpiarListenerRevision();
        limpiarListenerEsperandoLobbyFinal();
        limpiarListenerCongeladorActivo();
        limpiarListenerMinijuego();


        if (dialogMinijuegoActual != null && dialogMinijuegoActual.isShowing()) {
            dialogMinijuegoActual.dismiss();
            dialogMinijuegoActual = null;
        }

        if (handlerComodines != null && runnableDesbloquearBasta != null) {
            handlerComodines.removeCallbacks(runnableDesbloquearBasta);
            runnableDesbloquearBasta = null;
        }

        if (salaRef != null) {
            if (listenerEstadoPartida != null) {
                salaRef.child("estadoPartida").removeEventListener(listenerEstadoPartida);
                listenerEstadoPartida = null;
            }
            if (listenerFase != null) {
                salaRef.child("fase").removeEventListener(listenerFase);
                listenerFase = null;
            }
            if (listenerRondaActual != null) {
                salaRef.child("rondaActual").removeEventListener(listenerRondaActual);
                listenerRondaActual = null;
            }
            if (listenerLetraActual != null) {
                salaRef.child("letraActual").removeEventListener(listenerLetraActual);
                listenerLetraActual = null;
            }
            if (listenerCantidadRondas != null) {
                salaRef.child("cantidadRondas").removeEventListener(listenerCantidadRondas);
                listenerCantidadRondas = null;
            }
            if (listenerTiempoRonda != null) {
                salaRef.child("tiempoRonda").removeEventListener(listenerTiempoRonda);
                listenerTiempoRonda = null;
            }
        }

        if (nodoBastaTimeStampRef != null && listenerBasta != null) {
            nodoBastaTimeStampRef.removeEventListener(listenerBasta);
            listenerBasta = null;
            nodoBastaTimeStampRef = null;
        }

        if (nodoJugadoresPartidaRef != null && listenerJugadoresPartida != null) {
            nodoJugadoresPartidaRef.removeEventListener(listenerJugadoresPartida);
            listenerJugadoresPartida = null;
            nodoJugadoresPartidaRef = null;
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

        if (nodoMiJugadorPartidaRef != null && listenerMiRolHost != null) {
            nodoMiJugadorPartidaRef.removeEventListener(listenerMiRolHost);
            listenerMiRolHost = null;
            nodoMiJugadorPartidaRef = null;
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

    public void ObtenerJugadoresYLlenarPartida(String idSala) {
        auth = FirebaseAuth.getInstance();

        if (!VerificacionesAuth.VerificarInicioSesion(auth) || salaRef == null) {
            return;
        }

        nodoJugadoresPartidaRef = salaRef.child("jugadores");

        listenerJugadoresPartida = nodoJugadoresPartidaRef.addValueEventListener(new ValueEventListener() {
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

                cantidadJugadoresEnSala = jugadoresActivos.size();

                JugadorPartidaAdapter adaptador = new JugadorPartidaAdapter(jugadoresActivos);
                rvJugadoresPartida.setAdapter(adaptador);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
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
        if (!rondaSigueSiendoActual(rondaDeEstaRevision) || listaNombresCategorias == null) {
            return;
        }

        if (!USAR_API_VALIDACION) {
            HashMap<String, Boolean> correctasLocales = new HashMap<>();

            for (String categoriaOriginal : listaNombresCategorias) {
                String respuesta = respuestasJugador.get(categoriaOriginal);
                correctasLocales.put(categoriaOriginal, validarRespuestaLocalmente(letraElegida, respuesta));
            }

            guardarValidacionesJugador(correctasLocales, respuestasPorCategoria, rondaDeEstaRevision);
            return;
        }

        HashMap<String, Boolean> correctasPorCategoria = new HashMap<>();
        List<ItemRespuesta> respuestasPendientesApi = new ArrayList<>();
        HashMap<String, String> categoriaApiAOriginal = new HashMap<>();
        HashMap<String, String> respuestaPorCategoriaApi = new HashMap<>();
        AtomicInteger pendientesCache = new AtomicInteger(listaNombresCategorias.size());

        for (String categoriaOriginal : listaNombresCategorias) {
            String respuesta = respuestasJugador.get(categoriaOriginal);

            if (respuesta == null || respuesta.trim().isEmpty()) {
                correctasPorCategoria.put(categoriaOriginal, false);

                if (pendientesCache.decrementAndGet() == 0) {
                    finalizarValidacionConCacheYApi(correctasPorCategoria, respuestasPendientesApi, categoriaApiAOriginal, respuestaPorCategoriaApi, respuestasPorCategoria, rondaDeEstaRevision);
                }

                continue;
            }

            String categoriaApi = mapearCategoriaParaApi(categoriaOriginal);
            categoriaApiAOriginal.put(categoriaApi, categoriaOriginal);

            DatabaseReference cacheRef = obtenerRefCacheValidacion(letraElegida, categoriaApi, respuesta);

            cacheRef.child("correcta").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshotCache) {
                    Boolean correctaCache = snapshotCache.getValue(Boolean.class);

                    if (correctaCache != null) {
                        correctasPorCategoria.put(categoriaOriginal, correctaCache);
                    } else {
                        respuestasPendientesApi.add(new ItemRespuesta(categoriaApi, respuesta.trim()));
                        respuestaPorCategoriaApi.put(categoriaApi, respuesta.trim());
                    }

                    if (pendientesCache.decrementAndGet() == 0) {
                        finalizarValidacionConCacheYApi(correctasPorCategoria, respuestasPendientesApi, categoriaApiAOriginal, respuestaPorCategoriaApi, respuestasPorCategoria, rondaDeEstaRevision);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    respuestasPendientesApi.add(new ItemRespuesta(categoriaApi, respuesta.trim()));
                    respuestaPorCategoriaApi.put(categoriaApi, respuesta.trim());

                    if (pendientesCache.decrementAndGet() == 0) {
                        finalizarValidacionConCacheYApi(correctasPorCategoria, respuestasPendientesApi, categoriaApiAOriginal, respuestaPorCategoriaApi, respuestasPorCategoria, rondaDeEstaRevision);
                    }
                }
            });
        }
    }

    private void finalizarValidacionConCacheYApi(
            HashMap<String, Boolean> correctasPorCategoria,
            List<ItemRespuesta> respuestasPendientesApi,
            HashMap<String, String> categoriaApiAOriginal,
            HashMap<String, String> respuestaPorCategoriaApi,
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            int rondaDeEstaRevision
    ) {
        if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
            return;
        }

        if (respuestasPendientesApi.isEmpty()) {
            guardarValidacionesJugador(correctasPorCategoria, respuestasPorCategoria, rondaDeEstaRevision);
            return;
        }

        ValidacionRequest request = new ValidacionRequest(letraElegida, respuestasPendientesApi);
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.validarRonda(request).enqueue(new retrofit2.Callback<ValidacionResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ValidacionResponse> call, retrofit2.Response<ValidacionResponse> response) {
                if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<ResultadoApi> resultados = response.body().getResultados();

                    for (ResultadoApi resultado : resultados) {
                        String categoriaApi = resultado.getCategoria();
                        String categoriaOriginal = categoriaApiAOriginal.get(categoriaApi);

                        if (categoriaOriginal == null) {
                            categoriaOriginal = mapearCategoriaDesdeApi(categoriaApi);
                        }

                        boolean correcta = resultado.isCorrecta();
                        correctasPorCategoria.put(categoriaOriginal, correcta);

                        String respuestaOriginal = respuestaPorCategoriaApi.get(categoriaApi);

                        if (respuestaOriginal != null && !respuestaOriginal.trim().isEmpty()) {
                            HashMap<String, Object> datosCache = new HashMap<>();
                            datosCache.put("correcta", correcta);
                            datosCache.put("actualizadoEn", ServerValue.TIMESTAMP);

                            obtenerRefCacheValidacion(letraElegida, categoriaApi, respuestaOriginal)
                                    .updateChildren(datosCache);
                        }
                    }
                }

                for (String categoriaApi : respuestaPorCategoriaApi.keySet()) {
                    String categoriaOriginal = categoriaApiAOriginal.get(categoriaApi);

                    if (categoriaOriginal != null && !correctasPorCategoria.containsKey(categoriaOriginal)) {
                        correctasPorCategoria.put(categoriaOriginal, false);
                    }
                }

                guardarValidacionesJugador(correctasPorCategoria, respuestasPorCategoria, rondaDeEstaRevision);
            }

            @Override
            public void onFailure(retrofit2.Call<ValidacionResponse> call, Throwable t) {
                Log.e("VALIDACION_API", "Error API: " + t.getMessage());

                for (String categoriaApi : respuestaPorCategoriaApi.keySet()) {
                    String categoriaOriginal = categoriaApiAOriginal.get(categoriaApi);

                    if (categoriaOriginal != null) {
                        correctasPorCategoria.put(categoriaOriginal, false);
                    }
                }

                guardarValidacionesJugador(correctasPorCategoria, respuestasPorCategoria, rondaDeEstaRevision);
            }
        });
    }

    private void guardarValidacionesJugador(
            HashMap<String, Boolean> correctasPorCategoria,
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            int rondaDeEstaRevision
    ) {
        if (!rondaSigueSiendoActual(rondaDeEstaRevision) || !VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        HashMap<String, Object> validacionesJugador = new HashMap<>();

        for (String categoriaOriginal : listaNombresCategorias) {
            String miRespuesta = respuestasJugador.get(categoriaOriginal);

            if (miRespuesta == null) {
                miRespuesta = "";
            }

            boolean correcta = correctasPorCategoria.containsKey(categoriaOriginal) && correctasPorCategoria.get(categoriaOriginal);
            int repeticiones = contarRepeticionesRespuesta(categoriaOriginal, miRespuesta, respuestasPorCategoria);
            int puntosAsignados = correcta ? (repeticiones > 1 ? 5 : 10) : 0;

            HashMap<String, Object> datosCat = new HashMap<>();
            datosCat.put("correcta", correcta);
            datosCat.put("puntos", puntosAsignados);

            validacionesJugador.put(categoriaOriginal, datosCat);
        }

        salaRef.child("validaciones")
                .child("ronda_" + rondaDeEstaRevision)
                .child(auth.getCurrentUser().getUid())
                .setValue(validacionesJugador)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("VALIDACION", "Validaciones guardadas en ronda " + rondaDeEstaRevision);
                    }
                });
    }

    private int contarRepeticionesRespuesta(
            String categoriaOriginal,
            String miRespuesta,
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria
    ) {
        if (miRespuesta == null || miRespuesta.trim().isEmpty()) {
            return 0;
        }

        int repeticiones = 0;
        List<RespuestaValidacion> todasLasRespuestas = respuestasPorCategoria.get(categoriaOriginal);

        if (todasLasRespuestas != null) {
            for (RespuestaValidacion rv : todasLasRespuestas) {
                if (rv.getRespuesta() != null && rv.getRespuesta().trim().equalsIgnoreCase(miRespuesta.trim())) {
                    repeticiones++;
                }
            }
        }

        return repeticiones;
    }

    private boolean validarRespuestaLocalmente(String letra, String respuesta) {
        if (respuesta == null || respuesta.trim().isEmpty()) {
            return false;
        }

        if (respuesta.trim().equalsIgnoreCase("Sin respuesta")) {
            return false;
        }

        String letraNormalizada = normalizarTextoBasico(letra);
        String respuestaNormalizada = normalizarTextoBasico(respuesta);

        if (letraNormalizada.isEmpty() || respuestaNormalizada.isEmpty()) {
            return false;
        }

        int cantidadLetras = contarLetrasRespuesta(respuestaNormalizada);

        if (cantidadLetras < MIN_LETRAS_RESPUESTA_LOCAL) {
            return false;
        }

        return respuestaNormalizada.startsWith(letraNormalizada.substring(0, 1));
    }

    private int contarLetrasRespuesta(String texto) {
        if (texto == null) {
            return 0;
        }

        int contador = 0;

        for (int i = 0; i < texto.length(); i++) {
            if (Character.isLetter(texto.charAt(i))) {
                contador++;
            }
        }

        return contador;
    }

    private boolean votacionesLimitadas() {
        return USAR_API_VALIDACION;
    }

    private String normalizarTextoBasico(String texto) {
        if (texto == null) {
            return "";
        }

        return texto.trim()
                .toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ü", "u")
                .replace("ñ", "n");
    }

    private DatabaseReference obtenerRefCacheValidacion(String letra, String categoriaApi, String respuesta) {
        return FirebaseDatabase.getInstance()
                .getReference("validaciones_cache")
                .child(normalizarClaveFirebase(letra))
                .child(normalizarClaveFirebase(categoriaApi))
                .child(normalizarClaveFirebase(respuesta));
    }

    private String normalizarClaveFirebase(String texto) {
        if (texto == null) {
            return "_";
        }

        String clave = texto.trim().toLowerCase(Locale.ROOT);
        clave = clave.replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
                .replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("/", "_");

        if (clave.isEmpty()) {
            return "_";
        }

        return clave;
    }


    private void esperarRespuestasYMostrarModal(int rondaDeEstaRevision) {
        nodoRespuestasRondaRef = salaRef
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
                int jugadoresEsperados = Math.max(cantidadJugadoresEnSala, 1);

                if (cantidadRespuestas >= jugadoresEsperados) {
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


    private void cargarRespuestasValidacionYMostrarModal(int rondaDeEstaRevision) {
        if (!rondaSigueSiendoActual(rondaDeEstaRevision) || salaRef == null) {
            return;
        }

        salaRef.child("jugadores").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                salaRef.child("respuestas")
                        .child("ronda_" + rondaDeEstaRevision)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshotRespuestas) {
                                if (!rondaSigueSiendoActual(rondaDeEstaRevision)) {
                                    return;
                                }

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
                                                rv.setClaveVotacion(
                                                        calcularClaveVotacion(
                                                                rondaDeEstaRevision,
                                                                uidJugador,
                                                                categoria,
                                                                respuesta
                                                        )
                                                );
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
    private void escucharBastaParaCronometro() {
        if (salaRef == null) {
            return;
        }

        if (nodoBastaTimeStampRef != null && listenerBasta != null) {
            nodoBastaTimeStampRef.removeEventListener(listenerBasta);
            listenerBasta = null;
            nodoBastaTimeStampRef = null;
        }

        nodoBastaTimeStampRef = salaRef.child("bastaTimeStamp");

        listenerBasta = nodoBastaTimeStampRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || yaVolvioAlLobby || rondaActual <= 0) {
                    return;
                }

                Long bastaTimeStamp = snapshot.getValue(Long.class);

                if (bastaTimeStamp == null) {
                    return;
                }

                boolean bastaYaProcesadoEnEstaRonda =
                        ultimoBastaTimestampNotificado != null &&
                                ultimaRondaBastaNotificada == rondaActual;

                if (bastaYaProcesadoEnEstaRonda) {
                    return;
                }

                ultimoBastaTimestampNotificado = bastaTimeStamp;
                ultimaRondaBastaNotificada = rondaActual;

                bastaActivado = true;
                bloquearBotonBastaPorBasta();

                faseActual = "jugando";
                estadoPartidaActual = "En juego";

                if (!bastaSonidoReproducido) {
                    bastaSonidoReproducido = true;
                    Sounds.bastaPlay();
                }

                salaRef.child("bastaPresionadoPor").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot uidSnapshot) {
                        String uidBasta = uidSnapshot.getValue(String.class);

                        if (uidBasta == null) {
                            uidBasta = "";
                        }

                        String finalUidBasta = uidBasta;

                        salaRef.child("jugadores")
                                .child(finalUidBasta)
                                .child("nombre")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot nombreSnapshot) {
                                        String nombreJugadorBasta = nombreSnapshot.getValue(String.class);

                                        if (nombreJugadorBasta == null || nombreJugadorBasta.trim().isEmpty()) {
                                            nombreJugadorBasta = "Un jugador";
                                        }

                                        NotificationHelper.show(
                                                notificationPartida,
                                                "¡" + nombreJugadorBasta + " presionó BASTA!",
                                                NotificationHelper.SUCCESS
                                        );
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) { }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });

                recalcularCronometroConBasta(bastaTimeStamp);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("BASTA", "Error escuchando bastaTimeStamp: " + error.getMessage());
            }
        });
    }
    private void recalcularCronometroConBasta(long bastaTimeStamp) {
        if (!isAdded() || salaRef == null || yaVolvioAlLobby || rondaActual <= 0) {
            return;
        }

        final int rondaDelBasta = rondaActual;

        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                if (!rondaSigueSiendoActual(rondaDelBasta) || yaVolvioAlLobby) {
                    return;
                }

                Long inicioRonda = snapshotSala.child("inicioRonda").getValue(Long.class);
                int inicioRondaDe = leerEntero(snapshotSala.child("inicioRondaDe"), -1);

                if (inicioRonda == null || inicioRondaDe != rondaDelBasta) {
                    Log.d("BASTA", "BASTA ignorado porque no coincide con la ronda actual");
                    return;
                }

                faseActual = "jugando";
                estadoPartidaActual = "En juego";

                FirebaseDatabase.getInstance()
                        .getReference(".info/serverTimeOffset")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot offsetSnapshot) {
                                if (!rondaSigueSiendoActual(rondaDelBasta) || yaVolvioAlLobby) {
                                    return;
                                }

                                Long offset = offsetSnapshot.getValue(Long.class);

                                if (offset == null) {
                                    offset = 0L;
                                }

                                long horaActualServidor = System.currentTimeMillis() + offset;
                                long finOriginal = inicioRonda + (tiempoSeconds * 1000L);
                                long finBasta = bastaTimeStamp + 10000L;
                                long finReal = Math.min(finOriginal, finBasta);
                                long tiempoRestanteMs = finReal - horaActualServidor;

                                if (tiempoRestanteMs <= 0) {
                                    txtTiempoRestante.setText("00:00");
                                    guardarRespuestasJugador();
                                    return;
                                }

                                iniciarCronometro(tiempoRestanteMs, rondaDelBasta);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) { }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }



    private void mostrarDialogoComodines(boolean esFaseFinal) {
        if (!isAdded() || !VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        // Si estamos en la tabla de puntuaciones final, abrimos directo el diálogo saltando validaciones de juego.
        if (esFaseFinal) {
            mostrarDialogoComodinesSeguro(true);
            return;
        }

        // --- LÓGICA PARA CUANDO ESTÁN EN PLENA PARTIDA (esFaseFinal = false) ---
        if (respuestasGuardadas || modalValidacionMostrado || cierreRondaSolicitado) {
            NotificationHelper.show(
                    notificationPartida,
                    "Solo puedes usar comodines mientras estás respondiendo",
                    NotificationHelper.WARNING
            );
            return;
        }

        if (!esFaseJugando()) {
            if (salaRef == null) {
                NotificationHelper.show(
                        notificationPartida,
                        "Solo puedes usar comodines mientras estás respondiendo",
                        NotificationHelper.WARNING
                );
                return;
            }

            salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String faseFirebase = snapshot.child("fase").getValue(String.class);
                    String estadoFirebase = snapshot.child("estadoPartida").getValue(String.class);
                    int rondaFirebase = leerEntero(snapshot.child("rondaActual"), rondaActual);

                    if (faseFirebase != null) {
                        faseActual = faseFirebase;
                    }

                    if (estadoFirebase != null) {
                        estadoPartidaActual = estadoFirebase;
                    }

                    if (rondaFirebase > 0) {
                        rondaActual = rondaFirebase;
                    }

                    boolean jugandoFirebase = rondaFirebase > 0 &&
                            ("jugando".equalsIgnoreCase(faseActual) ||
                                    "En juego".equalsIgnoreCase(estadoPartidaActual));

                    if (jugandoFirebase && !respuestasGuardadas && !modalValidacionMostrado) {
                        resetearEstadosParaPartidaActiva();
                        mostrarDialogoComodinesSeguro(false);
                    } else {
                        NotificationHelper.show(
                                notificationPartida,
                                "Solo puedes usar comodines mientras estás respondiendo",
                                NotificationHelper.WARNING
                        );
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    NotificationHelper.show(
                            notificationPartida,
                            "No se pudo verificar el estado de la partida",
                            NotificationHelper.ERROR
                    );
                }
            });

            return;
        }

        mostrarDialogoComodinesSeguro(false);
    }

    private void mostrarDialogoComodinesSeguro(boolean esFaseFinal) {
        Dialog dialog = new Dialog(requireContext());
        View vista = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_comodines_partida, null);

        dialog.setContentView(vista);
        dialog.setCancelable(true);
        dialogComodinesActual = dialog;

        RecyclerView rvComodines = vista.findViewById(R.id.rvComodinesPartida);
        TextView txtComodinesVacios = vista.findViewById(R.id.txtComodinesVaciosPartida);
        MaterialButton btnCerrar = vista.findViewById(R.id.btnCerrarComodinesPartida);

        List<ComodinPartida> comodinesInventario = new ArrayList<>();

        ComodinPartidaAdapter adapterComodines = new ComodinPartidaAdapter(
                comodinesInventario,
                comodin -> ejecutarComodinDesdeDialogo(comodin, dialog)
        );

        rvComodines.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComodines.setAdapter(adapterComodines);

        txtComodinesVacios.setVisibility(View.VISIBLE);
        txtComodinesVacios.setText("Cargando tus comodines...");

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        String miUid = auth.getCurrentUser().getUid();

        FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(miUid)
                .child("inventarioComodines")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        comodinesInventario.clear();

                        for (ComodinPartida comodinCatalogo : obtenerCatalogoComodinesPartida()) {

                            // === FILTRO MÁGICO DE FASES ===
                            boolean esComodinDeCaosFinal = comodinCatalogo.getId().equals(COMODIN_UNO_REVERSA) ||
                                    comodinCatalogo.getId().equals("ruleta_caotica") ||
                                    comodinCatalogo.getId().equals("marca_de_la_muerte");

                            // Si estamos en la tabla final, ignoramos los de juego (Doble o nada, congelador)
                            if (esFaseFinal && !esComodinDeCaosFinal) continue;

                            // Si estamos jugando normal, ignoramos los del final (Cambio de flujo)
                            if (!esFaseFinal && esComodinDeCaosFinal) continue;
                            // ==============================

                            int cantidad = leerEntero(
                                    snapshot.child(comodinCatalogo.getId()).child("cantidad"),
                                    0
                            );

                            if (cantidad > 0) {
                                comodinesInventario.add(new ComodinPartida(
                                        comodinCatalogo.getId(),
                                        comodinCatalogo.getNombre(),
                                        comodinCatalogo.getDescripcion(),
                                        comodinCatalogo.getRareza(),
                                        comodinCatalogo.getIconoResId(),
                                        cantidad,
                                        comodinCatalogo.isImplementado(),
                                        comodinCatalogo.isPremium()
                                ));
                            }
                        }

                        adapterComodines.notifyDataSetChanged();

                        if (comodinesInventario.isEmpty()) {
                            txtComodinesVacios.setVisibility(View.VISIBLE);
                            if (esFaseFinal) {
                                txtComodinesVacios.setText("No tienes comodines de fase final en tu inventario.");
                            } else {
                                txtComodinesVacios.setText("No tienes comodines de juego en tu inventario.");
                            }
                        } else {
                            txtComodinesVacios.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        txtComodinesVacios.setVisibility(View.VISIBLE);
                        txtComodinesVacios.setText("No se pudo cargar tu inventario.");

                        NotificationHelper.show(
                                notificationPartida,
                                "No se pudo cargar tu inventario",
                                NotificationHelper.ERROR
                        );
                    }
                });

        dialog.setOnDismissListener(d -> {
            if (dialogComodinesActual == dialog) {
                dialogComodinesActual = null;
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private List<ComodinPartida> obtenerCatalogoComodinesPartida() {
        List<ComodinPartida> catalogo = new ArrayList<>();

        catalogo.add(new ComodinPartida(
                COMODIN_CONGELADOR,
                "Congelador",
                "Bloquea el botón BASTA por unos segundos.",
                "Control",
                R.drawable.ic_comodin_congelar,
                0,
                true,
                false
        ));

        catalogo.add(new ComodinPartida(
                COMODIN_DOBLE_O_NADA,
                "Doble o Nada",
                "Si alcanzas el 80% de la ronda, duplicas tus puntos. Si fallas, pierdes 15 puntos.",
                "Riesgo",
                R.drawable.ic_comodin_dobleonada,
                0,
                true,
                false
        ));

        catalogo.add(new ComodinPartida(
                COMODIN_UNO_REVERSA,
                "Cambio de Flujo",
                "Intercambia tu puntaje con un jugador aleatorio. Puede salvarte o destruirte.",
                "Caótico",
                R.drawable.ic_comodin_cambiodeflujo,
                0,
                true,
                false
        ));

        catalogo.add(new ComodinPartida(
                "ruleta_caotica",
                "Ruleta Caótica",
                "Activa una sorpresa aleatoria: puede beneficiar o arruinar a todos.",
                "Troll",
                R.drawable.ic_comodin_ruletacaotica,
                0,
                true,
                false
        ));

        catalogo.add(new ComodinPartida(
                "escudo_dorado",
                "Escudo Dorado",
                "Te protege de un comodín enemigo una vez durante la partida.",
                "Premium",
                R.drawable.ic_comodin_escudo,
                0,
                false,
                true
        ));


        catalogo.add(new ComodinPartida(
                "marca_de_la_muerte",
                "Marca de la Muerte",
                "Reta a un jugador a un duelo a muerte por sus puntos.",
                "Premium",
                R.drawable.ic_comodin_marcadelamuerte,
                0,
                true, // ¡IMPORTANTE! Debe estar en 'true' para que el botón diga "USAR" y no "PRONTO"
                true  // Es premium (cuesta gemas)
        ));

        return catalogo;
    }

    private void ejecutarComodinDesdeDialogo(ComodinPartida comodin, Dialog dialog) {
        if (comodin == null) return;

        if (!comodin.isImplementado()) {
            NotificationHelper.show(notificationPartida, comodin.getNombre() + " estará disponible pronto", NotificationHelper.INFO);
            Sounds.playNotificaciones();
            return;
        }

        // 👇 NUEVO: VERIFICACIÓN DE COOLDOWN PARA COMODINES FINALES 👇
        if (comodin.getId().equals("marca_de_la_muerte") ||
                comodin.getId().equals(COMODIN_UNO_REVERSA) ||
                comodin.getId().equals("ruleta_caotica")) {

            if (comodinesUsadosEstaRonda.contains(comodin.getId())) {
                NotificationHelper.show(notificationPartida, "Ya usaste este comodín. ¡Espera a la siguiente ronda!", NotificationHelper.WARNING);
                Sounds.playNotificaciones();
                return; // Lo rebotamos y no cerramos el diálogo
            }
        }
        // 👆 FIN DE LO NUEVO 👆

        dialog.dismiss(); // Cerramos el modal de inventario

        obtenerMiNombreEnSala(nombre -> {
            BaseComodin comodinEjecutor = null;

            switch (comodin.getId()) {
                case COMODIN_DOBLE_O_NADA:
                    comodinEjecutor = new ComodinDobleONada(idSala, rondaActual);
                    break;
                case COMODIN_UNO_REVERSA:
                    comodinEjecutor = new ComodinCambioDeFlujo(idSala, rondaActual);
                    break;
                case "ruleta_caotica":
                    comodinEjecutor = new ComodinRuletaCaotica(idSala, rondaActual);
                    break;
                case COMODIN_CONGELADOR:
                    usarCongelador();
                    return;
                // 👇 ¡NUEVA INTERCEPCIÓN AQUÍ! 👇
                case "marca_de_la_muerte":
                    mostrarDialogoSeleccionVictima(nombre);
                    return; // Salimos de aquí, porque el comodín se activará DESPUÉS de elegir víctima
            }

            // EJECUCIÓN LIMPIA PARA LOS DEMÁS COMODINES
            if (comodinEjecutor != null) {
                comodinEjecutor.activar(nombre, new BaseComodin.ComodinCallback() {
                    @Override
                    public void onSuccess(String mensajeNotificacion) {
                        comodinesUsadosEstaRonda.add(comodin.getId());

                        if (!mensajeNotificacion.equals("SILENCIO")) {
                            NotificationHelper.show(notificationPartida, mensajeNotificacion, NotificationHelper.SUCCESS);
                            Sounds.playNotificaciones();
                        }
                    }

                    @Override
                    public void onError(String mensajeError) {
                        NotificationHelper.show(notificationPartida, mensajeError, NotificationHelper.WARNING);
                        Sounds.playNotificaciones();
                    }
                });
            }
        });
    }

    private void mostrarDialogoSeleccionVictima(String miNombre) {
        if (!isAdded()) return;

        Dialog dialogVictima = new Dialog(requireContext());
        dialogVictima.setContentView(R.layout.dialog_seleccionar_victima);
        dialogVictima.setCancelable(false);

        RecyclerView rvVictimas = dialogVictima.findViewById(R.id.rvVictimas);
        MaterialButton btnCancelar = dialogVictima.findViewById(R.id.btnCancelarVictima);

        btnCancelar.setOnClickListener(v -> dialogVictima.dismiss());

        String miUid = auth.getCurrentUser().getUid();

        // Leemos los jugadores de la sala desde Firebase
        salaRef.child("jugadores").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<VictimaAdapter.JugadorMini> rivales = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String uid = snap.getKey();
                    String nombre = snap.child("nombre").getValue(String.class);

                    // Añadimos a todos a la lista, EXCEPTO a nosotros mismos
                    if (uid != null && !uid.equals(miUid) && nombre != null) {
                        rivales.add(new VictimaAdapter.JugadorMini(uid, nombre));
                    }
                }

                VictimaAdapter adapter = new VictimaAdapter(rivales, victima -> {
                    dialogVictima.dismiss(); // Cerramos el diálogo de selección

                    // ¡AQUÍ SE DESATA LA MARCA DE LA MUERTE!
                    ComodinMarcaDeLaMuerte comodin = new ComodinMarcaDeLaMuerte(idSala, rondaActual, victima.uid, victima.nombre);

                    comodin.activar(miNombre, new BaseComodin.ComodinCallback() {
                        @Override
                        public void onSuccess(String mensaje) {
                            // El comodín se activó en Firebase.
                            // El nuevo Listener que haremos en la Fase 2 detectará la creación del minijuego.
                            comodinesUsadosEstaRonda.add("marca_de_la_muerte");
                        }

                        @Override
                        public void onError(String error) {
                            NotificationHelper.show(notificationPartida, error, NotificationHelper.ERROR);
                        }
                    });
                });

                rvVictimas.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvVictimas.setAdapter(adapter);

                dialogVictima.show();
                if (dialogVictima.getWindow() != null) {
                    dialogVictima.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                    dialogVictima.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                NotificationHelper.show(notificationPartida, "Error al cargar jugadores", NotificationHelper.ERROR);
            }
        });
    }

    private void usarCongelador() {
        if (!puedeUsarComodinAhora()) {
            return;
        }

        salaRef.child("bastaPresionado").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean bastaPresionado = snapshot.getValue(Boolean.class);

                if (Boolean.TRUE.equals(bastaPresionado)) {
                    NotificationHelper.show(
                            notificationPartida,
                            "Ya presionaron BASTA en esta ronda",
                            NotificationHelper.WARNING
                    );
                    return;
                }

                descontarComodinSiHay(COMODIN_CONGELADOR, exito -> {
                    if (!exito) {
                        NotificationHelper.show(
                                notificationPartida,
                                "No tienes Congelador disponible",
                                NotificationHelper.WARNING
                        );
                        return;
                    }

                    obtenerMiNombreEnSala(nombre -> activarCongeladorEnSala(nombre));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void activarCongeladorEnSala(String nombreJugador) {
        String miUid = auth.getCurrentUser().getUid();
        String idUso = "congelador_" + rondaActual + "_" + miUid + "_" + System.currentTimeMillis();

        HashMap<String, Object> actualizaciones = new HashMap<>();

        String rutaCongelador = "comodinesActivos/ronda_" + rondaActual + "/congelador";
        String rutaUso = "comodinesUsados/ronda_" + rondaActual + "/" + idUso;

        actualizaciones.put(rutaCongelador + "/activo", true);
        actualizaciones.put(rutaCongelador + "/idUso", idUso);
        actualizaciones.put(rutaCongelador + "/tipo", COMODIN_CONGELADOR);
        actualizaciones.put(rutaCongelador + "/usadoPor", miUid);
        actualizaciones.put(rutaCongelador + "/nombreJugador", nombreJugador);
        actualizaciones.put(rutaCongelador + "/inicio", ServerValue.TIMESTAMP);
        actualizaciones.put(rutaCongelador + "/duracionMs", DURACION_CONGELADOR_MS);

        actualizaciones.put(rutaUso + "/tipo", COMODIN_CONGELADOR);
        actualizaciones.put(rutaUso + "/uid", miUid);
        actualizaciones.put(rutaUso + "/nombreJugador", nombreJugador);
        actualizaciones.put(rutaUso + "/timestamp", ServerValue.TIMESTAMP);

        salaRef.updateChildren(actualizaciones).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                NotificationHelper.show(
                        notificationPartida,
                        "Activaste Congelador: BASTA queda bloqueado por 8 segundos",
                        NotificationHelper.SUCCESS
                );
                Sounds.playNotificaciones();
            } else {
                NotificationHelper.show(
                        notificationPartida,
                        "No se pudo activar el Congelador",
                        NotificationHelper.ERROR
                );
            }
        });
    }

    private void mostrarResumenComodines(int rondaCerrada, boolean esFinal) {
        if (!isAdded()) return;

        // 1. Primero obtenemos los nombres de los jugadores para que el chisme tenga sentido
        salaRef.child("jugadores").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                HashMap<String, String> mapaNombres = new HashMap<>();
                for (DataSnapshot j : snapshotJugadores.getChildren()) {
                    mapaNombres.put(j.getKey(), j.child("nombre").getValue(String.class));
                }

                // 2. Ahora sí, leemos los resultados de los comodines de la ronda
                salaRef.child("comodinesResultados").child("ronda_" + rondaCerrada)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshotResultados) {
                                List<com.example.basta_game.models.EventoComodin> listaEventos = new ArrayList<>();

                                if (snapshotResultados.exists() && snapshotResultados.getChildrenCount() > 0) {
                                    // Procesamos qué pasó
                                    for (DataSnapshot snap : snapshotResultados.getChildren()) {
                                        String key = snap.getKey();

                                        if (key != null && key.startsWith("flujo_")) {
                                            // Fue un Cambio de Flujo
                                            String nombreAtacante = snap.child("nombreAtacante").getValue(String.class);
                                            String victimaUid = snap.child("victima").getValue(String.class);
                                            String resultado = snap.child("resultado").getValue(String.class);

                                            if ("castigo".equals(resultado)) {
                                                listaEventos.add(new com.example.basta_game.models.EventoComodin(nombreAtacante + " usó Cambio de Flujo", "¡Le salió mal el azar y perdió 5 puntos!", R.drawable.ic_comodin_cambiodeflujo, false));
                                            } else {
                                                String nombreVictima = mapaNombres.containsKey(victimaUid) ? mapaNombres.get(victimaUid) : "alguien";
                                                listaEventos.add(new com.example.basta_game.models.EventoComodin(nombreAtacante + " usó Cambio de Flujo", "Robó e intercambió su puntaje con " + nombreVictima, R.drawable.ic_comodin_cambiodeflujo, true));
                                            }
                                        } else if (key != null) {
                                            // Posible Doble o Nada (la llave es el UID del jugador)
                                            if (snap.hasChild("doble_o_nada")) {
                                                String resultado = snap.child("doble_o_nada").child("resultado").getValue(String.class);
                                                String nombreJugador = mapaNombres.containsKey(key) ? mapaNombres.get(key) : "Un jugador";

                                                if ("duplicado".equals(resultado)) {
                                                    listaEventos.add(new com.example.basta_game.models.EventoComodin(nombreJugador + " usó Doble o Nada", "¡Logró el 80% de aciertos y duplicó sus puntos!", R.drawable.ic_comodin_dobleonada, true));
                                                } else {
                                                    listaEventos.add(new com.example.basta_game.models.EventoComodin(nombreJugador + " usó Doble o Nada", "No alcanzó la meta y fue castigado con -15 pts.", R.drawable.ic_comodin_dobleonada, false));
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Mostramos el diálogo
                                if (!listaEventos.isEmpty()) {
                                    Dialog dialogResumen = new Dialog(requireContext());
                                    View vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialogo_resumen_comodines, null);
                                    dialogResumen.setContentView(vista);
                                    dialogResumen.setCancelable(false);

                                    MaterialButton btnEntendido = vista.findViewById(R.id.btnEntendidoResumen);
                                    TextView txtNadie = vista.findViewById(R.id.txtNadieUsoComodines);
                                    RecyclerView rvEventos = vista.findViewById(R.id.rvEventosComodines);

                                    txtNadie.setVisibility(View.GONE);
                                    rvEventos.setVisibility(View.VISIBLE);

                                    rvEventos.setLayoutManager(new LinearLayoutManager(requireContext()));
                                    rvEventos.setAdapter(new com.example.basta_game.Adapters.ResumenComodinesAdapter(listaEventos));

                                    btnEntendido.setOnClickListener(v -> {
                                        dialogResumen.dismiss();
                                        mostrarModalPuntuaciones(rondaCerrada, esFinal);
                                    });

                                    dialogResumen.show();
                                    if (dialogResumen.getWindow() != null) {
                                        dialogResumen.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                        dialogResumen.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    }
                                } else {
                                    // Nadie usó nada, saltamos directo a las puntuaciones
                                    mostrarModalPuntuaciones(rondaCerrada, esFinal);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                mostrarModalPuntuaciones(rondaCerrada, esFinal);
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mostrarModalPuntuaciones(rondaCerrada, esFinal);
            }
        });
    }

    private boolean puedeUsarComodinAhora() {
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return false;
        }

        if (salaRef == null || rondaActual <= 0 || !esFaseJugando()) {
            NotificationHelper.show(
                    notificationPartida,
                    "Solo puedes usar comodines durante una ronda activa",
                    NotificationHelper.WARNING
            );
            return false;
        }

        if (respuestasGuardadas || modalValidacionMostrado || cierreRondaSolicitado) {
            NotificationHelper.show(
                    notificationPartida,
                    "Ya no puedes usar comodines en esta ronda",
                    NotificationHelper.WARNING
            );
            return false;
        }

        return true;
    }

    private void descontarComodinSiHay(String idComodin, ResultadoComodinCallback callback) {
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            if (callback != null) {
                callback.onResultado(false);
            }
            return;
        }

        String miUid = auth.getCurrentUser().getUid();

        DatabaseReference cantidadRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(miUid)
                .child("inventarioComodines")
                .child(idComodin)
                .child("cantidad");

        cantidadRef.keepSynced(true);

        cantidadRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int cantidadLeida = leerEntero(snapshot, 0);

                if (cantidadLeida <= 0) {
                    if (callback != null) {
                        callback.onResultado(false);
                    }
                    return;
                }

                cantidadRef.runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        int cantidadActual = leerEnteroMutable(currentData.getValue(), 0);

                        if (cantidadActual <= 0) {
                            return Transaction.abort();
                        }

                        currentData.setValue(cantidadActual - 1);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                        if (callback != null) {
                            callback.onResultado(error == null && committed);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onResultado(false);
                }
            }
        });
    }





    private int leerEnteroMutable(Object valor, int valorDefault) {
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

    private void obtenerMiNombreEnSala(NombreJugadorCallback callback) {
        String miUid = auth.getCurrentUser().getUid();

        salaRef.child("jugadores")
                .child(miUid)
                .child("nombre")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String nombre = snapshot.getValue(String.class);

                        if (nombre == null || nombre.trim().isEmpty()) {
                            nombre = "Un jugador";
                        }

                        callback.onNombre(nombre);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onNombre("Un jugador");
                    }
                });
    }

    private void escucharCongeladorActivo() {
        if (salaRef == null || rondaActual <= 0) {
            return;
        }

        limpiarListenerCongeladorActivo();

        nodoCongeladorActivoRef = salaRef
                .child("comodinesActivos")
                .child("ronda_" + rondaActual)
                .child("congelador");

        listenerCongeladorActivo = nodoCongeladorActivoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || yaVolvioAlLobby) {
                    return;
                }

                Boolean activo = snapshot.child("activo").getValue(Boolean.class);
                Long inicio = snapshot.child("inicio").getValue(Long.class);
                int duracionMs = leerEntero(snapshot.child("duracionMs"), DURACION_CONGELADOR_MS);
                String nombreJugador = snapshot.child("nombreJugador").getValue(String.class);
                String idUso = snapshot.child("idUso").getValue(String.class);

                if (!Boolean.TRUE.equals(activo) || inicio == null) {
                    return;
                }

                FirebaseDatabase.getInstance()
                        .getReference(".info/serverTimeOffset")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot offsetSnapshot) {
                                Long offset = offsetSnapshot.getValue(Long.class);

                                if (offset == null) {
                                    offset = 0L;
                                }

                                long horaActualServidor = System.currentTimeMillis() + offset;
                                long restanteMs = (inicio + duracionMs) - horaActualServidor;

                                if (restanteMs > 0) {
                                    bloquearBastaPorCongelador(restanteMs, nombreJugador, idUso);
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

    private void bloquearBastaPorCongelador(long restanteMs, String nombreJugador, String idUso) {
        if (!isAdded()) {
            return;
        }

        bastaBloqueadoPorComodin = true;

        if (runnableDesbloquearBasta != null) {
            handlerComodines.removeCallbacks(runnableDesbloquearBasta);
            runnableDesbloquearBasta = null;
        }

        btnBasta.setEnabled(false);
        btnBasta.setClickable(false);
        btnBasta.setAlpha(0.45f);
        btnBasta.setText("CONGELADO");

        boolean esNuevoCongelador = idUso != null && !idUso.equals(ultimoCongeladorProcesado);

        if (esNuevoCongelador) {
            ultimoCongeladorProcesado = idUso;

            if (nombreJugador == null || nombreJugador.trim().isEmpty()) {
                nombreJugador = "Un jugador";
            }

            NotificationHelper.show(
                    notificationPartida,
                    nombreJugador + " usó Congelador. BASTA queda bloqueado por unos segundos",
                    NotificationHelper.INFO
            );
        }

        runnableDesbloquearBasta = () -> {
            bastaBloqueadoPorComodin = false;
            runnableDesbloquearBasta = null;

            if (!bastaActivado && esFaseJugando() && isAdded()) {
                desbloquearBotonBastaSiCorresponde();
            }
        };

        handlerComodines.postDelayed(runnableDesbloquearBasta, restanteMs);
    }

    private void limpiarListenerCongeladorActivo() {
        if (nodoCongeladorActivoRef != null && listenerCongeladorActivo != null) {
            nodoCongeladorActivoRef.removeEventListener(listenerCongeladorActivo);
            listenerCongeladorActivo = null;
            nodoCongeladorActivoRef = null;
        }
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

            adapterValidacionActual = null;

            limpiarListenerRevisionCategorias();
            limpiarListenerResultadosVotacionPalabras();
            limpiarListenerCooldownVotacion();
        });

        ViewPager2 vpCategorias = dialog.findViewById(R.id.vpCategoriasValidacion);
        AppCompatButton btnSalir = dialog.findViewById(R.id.btnSalirRevision);

        btnSalir.setVisibility(View.GONE);
        vpCategorias.setUserInputEnabled(false);

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
                () -> !hayVotacionActivaEnRevision,
                esAnfitrion || esCreador,
                siguienteIndice -> avanzarCategoriaRevision(rondaDeEstaRevision, siguienteIndice)
        );

        adapterValidacionActual = adapter;

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
        escucharResultadosVotacionesPalabras(respuestasPorCategoria, adapter, rondaDeEstaRevision);
        escucharCooldownVotaciones(adapter, rondaDeEstaRevision);
        prepararRevisionCategoriasSiHaceFalta(rondaDeEstaRevision);
        escucharIndiceRevisionCategoria(vpCategorias, adapter, rondaDeEstaRevision);
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

                        // ¡AQUÍ ESTÁ LA MAGIA QUE FALTABA!
                        // En lugar de llamar a mostrarModalPuntuaciones,
                        // llamamos primero a mostrarResumenComodines.
                        mostrarResumenComodines(rondaCerrada, esFinal);
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
        limpiarListenerValidacionesGlobales();

        nodoValidacionesGlobalesRef = salaRef.child("validaciones").child("ronda_" + rondaDeEstaRevision);

        listenerValidacionesGlobales = nodoValidacionesGlobalesRef.addValueEventListener(new ValueEventListener() {
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

    private void limpiarListenerValidacionesGlobales() {
        if (nodoValidacionesGlobalesRef != null && listenerValidacionesGlobales != null) {
            nodoValidacionesGlobalesRef.removeEventListener(listenerValidacionesGlobales);
            listenerValidacionesGlobales = null;
            nodoValidacionesGlobalesRef = null;
        }
    }


    private void prepararEntradaNuevaPartida() {
        yaVolvioAlLobby = false;

        respuestasGuardadas = false;
        modalValidacionMostrado = false;
        cierreRondaSolicitado = false;
        bastaActivado = false;

        historialGuardandose = false;
        miHistorialFinalGuardado = false;

        sonidoFinalReproducido = false;
        bastaSonidoReproducido = false;
        sonidoResultadoRondaReproducido = false;

        idVotacionModalActual = null;
        resolviendoVotacion = false;
        hayVotacionActivaEnRevision = false;

        bastaBloqueadoPorComodin = false;
        ultimoCongeladorProcesado = null;

        faseActual = "jugando";
        estadoPartidaActual = "En juego";

        versionCronometro++;

        respuestasJugador.clear();

        if (handlerComodines != null && runnableDesbloquearBasta != null) {
            handlerComodines.removeCallbacks(runnableDesbloquearBasta);
            runnableDesbloquearBasta = null;
        }

        desbloquearBotonBastaSiCorresponde();
    }

    private void prepararRevisionCategoriasSiHaceFalta(int rondaDeEstaRevision) {
        if (salaRef == null || (!esAnfitrion && !esCreador)) {
            return;
        }

        DatabaseReference ref = salaRef
                .child("revisionCategorias")
                .child("ronda_" + rondaDeEstaRevision);

        ref.child("indiceActual").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Object valor = currentData.getValue();

                if (valor != null) {
                    return Transaction.abort();
                }

                currentData.setValue(0);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) { }
        });
    }

    private void escucharIndiceRevisionCategoria(
            ViewPager2 vpCategorias,
            TarjetasValidacionAdapter adapter,
            int rondaDeEstaRevision
    ) {
        limpiarListenerRevisionCategorias();

        nodoRevisionCategoriasRef = salaRef
                .child("revisionCategorias")
                .child("ronda_" + rondaDeEstaRevision)
                .child("indiceActual");

        listenerRevisionCategorias = nodoRevisionCategoriasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                int indice = leerEntero(snapshot, 0);

                if (indice < 0 || adapter == null || indice >= adapter.getItemCount()) {
                    return;
                }

                if (vpCategorias.getCurrentItem() != indice) {
                    vpCategorias.setCurrentItem(indice, true);
                }

                adapter.iniciarAnimacionEnPaginaPublica(indice);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void avanzarCategoriaRevision(int rondaDeEstaRevision, int siguienteIndice) {
        if (salaRef == null || (!esAnfitrion && !esCreador)) {
            return;
        }

        salaRef.child("revisionCategorias")
                .child("ronda_" + rondaDeEstaRevision)
                .child("indiceActual")
                .setValue(siguienteIndice);
    }

    private void limpiarListenerRevisionCategorias() {
        if (nodoRevisionCategoriasRef != null && listenerRevisionCategorias != null) {
            nodoRevisionCategoriasRef.removeEventListener(listenerRevisionCategorias);
            listenerRevisionCategorias = null;
            nodoRevisionCategoriasRef = null;
        }
    }

    private void escucharResultadosVotacionesPalabras(
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            TarjetasValidacionAdapter adapter,
            int rondaDeEstaRevision
    ) {
        limpiarListenerResultadosVotacionPalabras();

        nodoResultadosVotacionPalabrasRef = salaRef
                .child("resultadosVotacionPalabras")
                .child("ronda_" + rondaDeEstaRevision);

        listenerResultadosVotacionPalabras = nodoResultadosVotacionPalabrasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                for (DataSnapshot resultadoSnap : snapshot.getChildren()) {
                    String clave = resultadoSnap.getKey();

                    if (clave == null) {
                        continue;
                    }

                    Boolean resuelta = resultadoSnap.child("resuelta").getValue(Boolean.class);
                    String resultado = resultadoSnap.child("resultado").getValue(String.class);
                    Boolean correctaFinal = resultadoSnap.child("correctaFinal").getValue(Boolean.class);
                    int puntosFinales = leerEntero(resultadoSnap.child("puntosFinales"), -1);

                    RespuestaValidacion respuesta = buscarRespuestaPorClave(respuestasPorCategoria, clave);

                    if (respuesta != null && Boolean.TRUE.equals(resuelta)) {
                        respuesta.setVotacionResuelta(true);
                        respuesta.setResultadoVotacion(resultado == null ? "resuelta" : resultado);

                        if (correctaFinal != null && puntosFinales >= 0) {
                            respuesta.setCorrecta(correctaFinal);
                            respuesta.setPuntos(puntosFinales);
                            respuesta.setValidada(true);
                        }

                        respuesta.setMostrarPuntaje(true);
                        respuesta.setTimerIniciado(false);
                    }
                }

                if (adapter != null) {
                    adapter.actualizarValidaciones();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void limpiarListenerResultadosVotacionPalabras() {
        if (nodoResultadosVotacionPalabrasRef != null && listenerResultadosVotacionPalabras != null) {
            nodoResultadosVotacionPalabrasRef.removeEventListener(listenerResultadosVotacionPalabras);
            listenerResultadosVotacionPalabras = null;
            nodoResultadosVotacionPalabrasRef = null;
        }
    }

    private void escucharCooldownVotaciones(
            TarjetasValidacionAdapter adapter,
            int rondaDeEstaRevision
    ) {
        limpiarListenerCooldownVotacion();

        nodoCooldownVotacionRef = salaRef
                .child("cooldownVotacion")
                .child("ronda_" + rondaDeEstaRevision);

        listenerCooldownVotacion = nodoCooldownVotacionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !rondaSigueSiendoActual(rondaDeEstaRevision)) {
                    return;
                }

                long iniciadoEn = leerLong(snapshot.child("iniciadoEn"), 0L);
                long duracionMs = leerLong(snapshot.child("duracionMs"), COOLDOWN_VOTACION_MS);

                obtenerHoraServidor(horaServidor -> {
                    if (adapter != null) {
                        adapter.actualizarCooldown(iniciadoEn, duracionMs, horaServidor);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void limpiarListenerCooldownVotacion() {
        if (nodoCooldownVotacionRef != null && listenerCooldownVotacion != null) {
            nodoCooldownVotacionRef.removeEventListener(listenerCooldownVotacion);
            listenerCooldownVotacion = null;
            nodoCooldownVotacionRef = null;
        }
    }

    private RespuestaValidacion buscarRespuestaPorClave(
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            String clave
    ) {
        if (respuestasPorCategoria == null || clave == null) {
            return null;
        }

        for (List<RespuestaValidacion> lista : respuestasPorCategoria.values()) {
            if (lista == null) {
                continue;
            }

            for (RespuestaValidacion respuesta : lista) {
                if (clave.equals(respuesta.getClaveVotacion())) {
                    return respuesta;
                }
            }
        }

        return null;
    }

    private String calcularClaveVotacion(
            int ronda,
            String uidJugador,
            String categoria,
            String respuesta
    ) {
        return "r" + ronda + "_" +
                normalizarTextoClave(uidJugador) + "_" +
                normalizarTextoClave(categoria) + "_" +
                normalizarTextoClave(respuesta);
    }

    private String normalizarTextoClave(String texto) {
        if (texto == null) {
            return "_";
        }

        String clave = texto.trim().toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ü", "u")
                .replace("ñ", "n")
                .replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("/", "_")
                .replace(" ", "_");

        if (clave.isEmpty()) {
            return "_";
        }

        return clave;
    }

    private void obtenerHoraServidor(HoraServidorCallback callback) {
        FirebaseDatabase.getInstance()
                .getReference(".info/serverTimeOffset")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long offset = snapshot.getValue(Long.class);

                        if (offset == null) {
                            offset = 0L;
                        }

                        callback.onHora(System.currentTimeMillis() + offset);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onHora(System.currentTimeMillis());
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
                    NotificationHelper.WARNING
            );
            return;
        }

        if (respuesta.isVotacionResuelta()) {
            NotificationHelper.show(
                    notificationPartida,
                    "Esa palabra ya fue votada",
                    NotificationHelper.INFO
            );
            return;
        }

        if (!respuesta.isValidada()) {
            NotificationHelper.show(
                    notificationPartida,
                    "Espera a que el sistema valide esa respuesta",
                    NotificationHelper.WARNING
            );
            return;
        }

        String claveVotacion = respuesta.getClaveVotacion();

        if (claveVotacion == null || claveVotacion.trim().isEmpty()) {
            claveVotacion = calcularClaveVotacion(
                    rondaDeEstaRevision,
                    respuesta.getUidJugador(),
                    respuesta.getCategoria(),
                    respuesta.getRespuesta()
            );
            respuesta.setClaveVotacion(claveVotacion);
        }

        final String claveFinal = claveVotacion;

        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        obtenerHoraServidor(horaServidor -> nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
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
                            NotificationHelper.WARNING
                    );
                    return;
                }

                Boolean yaResuelta = snapshotSala
                        .child("resultadosVotacionPalabras")
                        .child("ronda_" + rondaDeEstaRevision)
                        .child(claveFinal)
                        .child("resuelta")
                        .getValue(Boolean.class);

                if (Boolean.TRUE.equals(yaResuelta)) {
                    respuesta.setVotacionResuelta(true);
                    respuesta.setMostrarPuntaje(true);
                    NotificationHelper.show(
                            notificationPartida,
                            "Esa palabra ya fue votada",
                            NotificationHelper.INFO
                    );
                    return;
                }

                if (votacionesLimitadas()) {
                    int usadas = leerEntero(
                            snapshotSala.child("votacionesUsadas").child("ronda_" + rondaDeEstaRevision),
                            0
                    );

                    if (usadas >= MAX_VOTACIONES_DECISIVAS_POR_RONDA) {
                        NotificationHelper.show(
                                notificationPartida,
                                "Ya se usaron las 2 votaciones de esta ronda",
                                NotificationHelper.WARNING
                        );
                        return;
                    }
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
                            NotificationHelper.WARNING
                    );
                    return;
                }

                crearVotacionActiva(respuesta, rondaDeEstaRevision);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        }));
    }



    private void crearVotacionActiva(RespuestaValidacion respuesta, int rondaDeEstaRevision) {
        String claveVotacion = respuesta.getClaveVotacion();

        if (claveVotacion == null || claveVotacion.trim().isEmpty()) {
            claveVotacion = calcularClaveVotacion(
                    rondaDeEstaRevision,
                    respuesta.getUidJugador(),
                    respuesta.getCategoria(),
                    respuesta.getRespuesta()
            );
            respuesta.setClaveVotacion(claveVotacion);
        }

        String idVotacion = "voto_" + rondaDeEstaRevision + "_" +
                respuesta.getUidJugador() + "_" + System.currentTimeMillis();

        DatabaseReference votacionRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("votacionActiva")
                .child("ronda_" + rondaDeEstaRevision);

        HashMap<String, Object> datos = new HashMap<>();
        datos.put("activa", true);
        datos.put("resuelta", false);
        datos.put("idVotacion", idVotacion);
        datos.put("claveVotacion", claveVotacion);
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
                            NotificationHelper.WARNING
                    );
                }
            }
        });
    }


    // ---------------- LOGICA DE MINIJUEGOS (FASE 2 Y 3) ----------------

    private void escucharMinijuegoActivo() {
        if (salaRef == null || !isAdded()) return;

        limpiarListenerMinijuego();

        nodoMinijuegoRef = salaRef.child("minijuegoActual");
        listenerMinijuego = nodoMinijuegoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Boolean activo = snapshot.child("activo").getValue(Boolean.class);

                // Si no hay minijuego activo, cerramos todo
                if (activo == null || !activo) {
                    if (dialogMinijuegoActual != null && dialogMinijuegoActual.isShowing()) {
                        dialogMinijuegoActual.dismiss();
                        dialogMinijuegoActual = null;
                    }
                    if (dialogAnuncioMinijuego != null && dialogAnuncioMinijuego.isShowing()) {
                        dialogAnuncioMinijuego.dismiss();
                        dialogAnuncioMinijuego = null;
                    }
                    return;
                }

                String tipoJuego = snapshot.child("tipoJuego").getValue(String.class);
                String uidAtacante = snapshot.child("uidAtacante").getValue(String.class);
                String uidVictima = snapshot.child("uidVictima").getValue(String.class);
                String estado = snapshot.child("estado").getValue(String.class);

                if (tipoJuego == null || uidAtacante == null || uidVictima == null) return;

                String miUid = auth.getCurrentUser().getUid();
                String miRol = "espectador";
                if (miUid.equals(uidAtacante)) miRol = "atacante";
                else if (miUid.equals(uidVictima)) miRol = "victima";

                // LA MÁQUINA DE ESTADOS
                if ("anuncio".equals(estado)) {
                    // Estamos en el pre-show
                    if (dialogMinijuegoActual != null && dialogMinijuegoActual.isShowing()) {
                        dialogMinijuegoActual.dismiss();
                    }
                    mostrarPantallaAnuncioMinijuego(tipoJuego, miRol, snapshot);
                } else {
                    // Ya estamos en la batalla (jugando, revelando, terminado)
                    if (dialogAnuncioMinijuego != null && dialogAnuncioMinijuego.isShowing()) {
                        dialogAnuncioMinijuego.dismiss();
                        dialogAnuncioMinijuego = null;
                    }
                    abrirPantallaMinijuego(tipoJuego, miRol, uidAtacante, uidVictima, estado, snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- NUEVO: LA SECUENCIA DE ANUNCIO ÉPICA ---
    private void mostrarPantallaAnuncioMinijuego(String tipoJuego, String miRol, DataSnapshot snapshot) {
        if (!isAdded()) return;

        // Evitamos reiniciar la animación si el diálogo ya está abierto
        if (dialogAnuncioMinijuego != null && dialogAnuncioMinijuego.isShowing()) return;

        dialogAnuncioMinijuego = new Dialog(requireContext());
        dialogAnuncioMinijuego.setContentView(R.layout.dialog_anuncio_minijuego);
        dialogAnuncioMinijuego.setCancelable(false);
        if (dialogAnuncioMinijuego.getWindow() != null) {
            dialogAnuncioMinijuego.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialogAnuncioMinijuego.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialogAnuncioMinijuego.show();

        TextView txtTitulo = dialogAnuncioMinijuego.findViewById(R.id.txtTituloAnuncio);
        TextView txtMensaje = dialogAnuncioMinijuego.findViewById(R.id.txtMensajeAnuncio);
        TextView txtAccion = dialogAnuncioMinijuego.findViewById(R.id.txtAccionAnuncio);
        TextView txtContador = dialogAnuncioMinijuego.findViewById(R.id.txtContadorAnuncio);

        String nombreAtacante = snapshot.child("nombreAtacante").getValue(String.class);
        String nombreVictima = snapshot.child("nombreVictima").getValue(String.class);

        // Textos personalizados según quién eres
        if ("victima".equals(miRol)) {
            txtTitulo.setText("¡TE HAN MARCADO!");
            txtMensaje.setText(android.text.Html.fromHtml("<b><font color='#304EDB'>" + nombreAtacante + "</font></b> te retó usando Marca de la Muerte. ¡Gana el minijuego para defender tus puntos!"));
        } else if ("atacante".equals(miRol)) {
            txtTitulo.setText("¡ATRACO INICIADO!");
            txtMensaje.setText(android.text.Html.fromHtml("Has retado a <b><font color='#E53935'>" + nombreVictima + "</font></b>. ¡Prepárate para robarle todo!"));
        } else {
            txtTitulo.setText("¡DUELO A MUERTE!");
            txtMensaje.setText(android.text.Html.fromHtml("<b><font color='#304EDB'>" + nombreAtacante + "</font></b> retó a <b><font color='#E53935'>" + nombreVictima + "</font></b>. ¡Vaya tensión!"));
        }

        // LA ANIMACIÓN DE SUSPENSO EN TODOS LOS TELÉFONOS (Secuencia de 5 segundos)
        Handler handlerSecuencia = new Handler(Looper.getMainLooper());

        // Seg 0: "Eligiendo minijuego..." (Ya está por defecto en el XML)

        // Seg 2: Revela el juego con letras épicas
        handlerSecuencia.postDelayed(() -> {
            if (dialogAnuncioMinijuego != null && dialogAnuncioMinijuego.isShowing()) {
                String nombreJuego = "piedra_papel_tijera".equals(tipoJuego) ? "PIEDRA, PAPEL O TIJERA" : "MINIJUEGO MISTERIOSO";

                // Usamos HTML para que "Duelo Seleccionado" sea negro y el nombre del juego sea Azul y más grande
                String textoEpico = "<font color='#14213D'>Duelo seleccionado:</font><br><b><font color='#304EDB'>" + nombreJuego + "</font></b>";
                txtAccion.setText(android.text.Html.fromHtml(textoEpico));
            }
        }, 2000);

        // Seg 3.5: Empieza cuenta regresiva visual
        handlerSecuencia.postDelayed(() -> {
            if (dialogAnuncioMinijuego != null && dialogAnuncioMinijuego.isShowing()) {
                txtAccion.setText("¡Abriendo la arena en...!");
                txtAccion.setTextColor(android.graphics.Color.parseColor("#E53935")); // Lo pintamos rojo de urgencia
                txtContador.setVisibility(View.VISIBLE);
                txtContador.setText("3");

                // AQUÍ PONES TU NUEVO AUDIO
                Sounds.playCombate();
            }
        }, 3500);

        handlerSecuencia.postDelayed(() -> {
            if (dialogAnuncioMinijuego != null) txtContador.setText("2");
        }, 4500);

        handlerSecuencia.postDelayed(() -> {
            if (dialogAnuncioMinijuego != null) txtContador.setText("1");
        }, 5500);

        // Seg 6.5: ¡EL ATACANTE ABRE LAS PUERTAS DE LA ARENA!
        handlerSecuencia.postDelayed(() -> {
            if ("atacante".equals(miRol) && salaRef != null) {
                // Solo el atacante le avisa a Firebase para que no haya duplicados
                salaRef.child("minijuegoActual").child("estado").setValue("jugando");
            }
        }, 6500);
    }

    private void limpiarListenerMinijuego() {
        if (nodoMinijuegoRef != null && listenerMinijuego != null) {
            nodoMinijuegoRef.removeEventListener(listenerMinijuego);
            listenerMinijuego = null;
            nodoMinijuegoRef = null;
        }
    }

    // Cascarón para la Fase 3: Aquí diseñaremos la arena
    // ---------------- LOGICA DE MINIJUEGOS (FASE 3) ----------------

    private void abrirPantallaMinijuego(String tipoJuego, String miRol, String uidAtacante, String uidVictima, String estado, DataSnapshot snapshotCompleto) {
        if (!isAdded()) return;

        // Por ahora solo manejamos Piedra, Papel o Tijera
        if (!"piedra_papel_tijera".equals(tipoJuego)) return;

        // Si el diálogo no existe o se cerró, lo creamos
        if (dialogMinijuegoActual == null || !dialogMinijuegoActual.isShowing()) {
            dialogMinijuegoActual = new Dialog(requireContext());
            dialogMinijuegoActual.setContentView(R.layout.dialog_minijuego_ppt);
            dialogMinijuegoActual.setCancelable(false); // ¡Nadie escapa de un duelo a muerte!

            if (dialogMinijuegoActual.getWindow() != null) {
                dialogMinijuegoActual.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialogMinijuegoActual.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialogMinijuegoActual.show();

            // --- CARGAR AVATARES Y NOMBRES REALES UNA SOLA VEZ AL ABRIR ---
            TextView txtNombreA = dialogMinijuegoActual.findViewById(R.id.txtNombreA);
            TextView txtNombreV = dialogMinijuegoActual.findViewById(R.id.txtNombreV);
            ImageView imgAvatarA = dialogMinijuegoActual.findViewById(R.id.imgAvatarA);
            ImageView imgAvatarV = dialogMinijuegoActual.findViewById(R.id.imgAvatarV);

            DatabaseReference refJugadores = FirebaseDatabase.getInstance().getReference("salas").child(idSala).child("jugadores");

            // Atacante
            refJugadores.child(uidAtacante).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
                    if(!isAdded() || dialogMinijuegoActual == null) return;
                    String nombre = snap.child("nombre").getValue(String.class);
                    String urlAvatar = snap.child("urlAvatar").getValue(String.class);
                    if(urlAvatar == null) urlAvatar = snap.child("fotoPerfilUrl").getValue(String.class); // Fallback por si acaso

                    txtNombreA.setText(nombre != null ? nombre : "Atacante");
                    if(urlAvatar != null && !urlAvatar.isEmpty()) {
                        com.bumptech.glide.Glide.with(requireContext()).load(urlAvatar).into(imgAvatarA);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            // Víctima
            refJugadores.child(uidVictima).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
                    if(!isAdded() || dialogMinijuegoActual == null) return;
                    String nombre = snap.child("nombre").getValue(String.class);
                    String urlAvatar = snap.child("urlAvatar").getValue(String.class);
                    if(urlAvatar == null) urlAvatar = snap.child("fotoPerfilUrl").getValue(String.class); // Fallback

                    txtNombreV.setText(nombre != null ? nombre : "Víctima");
                    if(urlAvatar != null && !urlAvatar.isEmpty()) {
                        com.bumptech.glide.Glide.with(requireContext()).load(urlAvatar).into(imgAvatarV);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // --- VINCULAMOS LAS DEMÁS VISTAS DEL XML ---
        TextView txtPuntosA = dialogMinijuegoActual.findViewById(R.id.txtPuntosA);
        TextView txtPuntosV = dialogMinijuegoActual.findViewById(R.id.txtPuntosV);
        ImageView imgEleccionA = dialogMinijuegoActual.findViewById(R.id.imgEleccionA);
        ImageView imgEleccionV = dialogMinijuegoActual.findViewById(R.id.imgEleccionV);
        TextView txtEstadoPPT = dialogMinijuegoActual.findViewById(R.id.txtEstadoPPT);
        LinearLayout layoutControles = dialogMinijuegoActual.findViewById(R.id.layoutControlesPPT);
        ImageView btnPiedra = dialogMinijuegoActual.findViewById(R.id.btnPiedra);
        ImageView btnPapel = dialogMinijuegoActual.findViewById(R.id.btnPapel);
        ImageView btnTijera = dialogMinijuegoActual.findViewById(R.id.btnTijera);

        // --- CARGAMOS LOS DATOS EN TIEMPO REAL DESDE FIREBASE ---
        DataSnapshot datos = snapshotCompleto.child("datosJuego");
        int ptsA = leerEntero(datos.child("puntosA"), 0);
        int ptsV = leerEntero(datos.child("puntosV"), 0);
        String eleccionA = datos.child("eleccionA").getValue(String.class);
        String eleccionV = datos.child("eleccionV").getValue(String.class);

        if (eleccionA == null) eleccionA = "";
        if (eleccionV == null) eleccionV = "";

        txtPuntosA.setText(String.valueOf(ptsA));
        txtPuntosV.setText(String.valueOf(ptsV));

        // --- MÁQUINA DE ESTADOS VISUALES ---
        if ("terminado".equals(estado)) {
            layoutControles.setVisibility(View.GONE);
            String ganador = snapshotCompleto.child("ganadorUid").getValue(String.class);

            if (auth.getCurrentUser().getUid().equals(ganador)) {
                txtEstadoPPT.setText("¡GANASTE EL DUELO!");
                txtEstadoPPT.setTextColor(android.graphics.Color.parseColor("#2AA514"));
                Sounds.playCelebracion();
            } else {
                txtEstadoPPT.setText("¡EL DUELO HA TERMINADO!");
                txtEstadoPPT.setTextColor(android.graphics.Color.parseColor("#E53935"));
            }

            setearIconoEleccion(imgEleccionA, eleccionA);
            setearIconoEleccion(imgEleccionV, eleccionV);
            return; // Detenemos aquí
        }

        if ("revelando".equals(estado)) {
            // Ambos eligieron, mostramos el choque
            layoutControles.setVisibility(View.GONE);
            txtEstadoPPT.setText("¡CHOQUE DE PODER!");
            txtEstadoPPT.setTextColor(android.graphics.Color.parseColor("#E65100"));
            setearIconoEleccion(imgEleccionA, eleccionA);
            setearIconoEleccion(imgEleccionV, eleccionV);
            return;
        }

        // Si estamos aquí, el estado es "jugando" o "preparando"
        txtEstadoPPT.setTextColor(android.graphics.Color.parseColor("#14213D"));
        setearIconoOculto(imgEleccionA, eleccionA, "atacante".equals(miRol));
        setearIconoOculto(imgEleccionV, eleccionV, "victima".equals(miRol));

        if ("espectador".equals(miRol)) {
            layoutControles.setVisibility(View.GONE);
            txtEstadoPPT.setText("Viendo el duelo a muerte...");
        } else {
            // Soy el Atacante o la Víctima
            String miEleccion = "atacante".equals(miRol) ? eleccionA : eleccionV;

            if (miEleccion.isEmpty()) {
                layoutControles.setVisibility(View.VISIBLE);
                txtEstadoPPT.setText("Elige tu arma...");

                // Configurar clicks a Firebase
                btnPiedra.setOnClickListener(v -> enviarEleccionPPT("piedra", miRol));
                btnPapel.setOnClickListener(v -> enviarEleccionPPT("papel", miRol));
                btnTijera.setOnClickListener(v -> enviarEleccionPPT("tijera", miRol));
            } else {
                layoutControles.setVisibility(View.GONE);
                txtEstadoPPT.setText("Arma lista. Esperando al rival...");
            }
        }

        // --- EL ATACANTE ES EL ÁRBITRO ---
        if ("atacante".equals(miRol) && !eleccionA.isEmpty() && !eleccionV.isEmpty() && !"revelando".equals(estado) && !"terminado".equals(estado)) {
            resolverRondaPPT(eleccionA, eleccionV, ptsA, ptsV, uidAtacante, uidVictima);
        }
    }

    // --- MÉTODOS AYUDANTES PARA EL MINIJUEGO PPT ---

    private void enviarEleccionPPT(String eleccion, String miRol) {
        String nodo = "atacante".equals(miRol) ? "eleccionA" : "eleccionV";
        salaRef.child("minijuegoActual").child("datosJuego").child(nodo).setValue(eleccion);
    }

    private void setearIconoEleccion(ImageView img, String eleccion) {
        switch(eleccion) {
            case "piedra": img.setImageResource(R.drawable.ic_piedra); break;
            case "papel": img.setImageResource(R.drawable.ic_papel); break;
            case "tijera": img.setImageResource(R.drawable.ic_tijeras); break;
            default: img.setImageResource(R.drawable.ic_siluetas); // Cargando...
        }
    }

    private void setearIconoOculto(ImageView img, String eleccion, boolean esMio) {
        if (eleccion.isEmpty()) {
            img.setImageResource(R.drawable.ic_interrogacion); // Todavía no elige
        } else {
            if (esMio) {
                setearIconoEleccion(img, eleccion); // Yo sí puedo ver mi propia arma
            } else {
                img.setImageResource(R.drawable.ic_candados); // Arma del rival oculta (Candado)
            }
        }
    }

    private void resolverRondaPPT(String eleccionA, String eleccionV, int ptsA, int ptsV, String uidAtacante, String uidVictima) {
        // Bloqueamos el estado a "revelando" para que la UI muestre las armas chocando
        salaRef.child("minijuegoActual").child("estado").setValue("revelando");

        // Lógica clásica de Piedra, Papel o Tijera
        int ganadorRonda = 0; // 0 Empate, 1 Atacante, 2 Víctima
        if (!eleccionA.equals(eleccionV)) {
            if ((eleccionA.equals("piedra") && eleccionV.equals("tijera")) ||
                    (eleccionA.equals("papel") && eleccionV.equals("piedra")) ||
                    (eleccionA.equals("tijera") && eleccionV.equals("papel"))) {
                ganadorRonda = 1;
            } else {
                ganadorRonda = 2;
            }
        }

        final int finalPtsA = ptsA + (ganadorRonda == 1 ? 1 : 0);
        final int finalPtsV = ptsV + (ganadorRonda == 2 ? 1 : 0);

        // Subimos los puntos parciales de esta ronda (ej. 1 a 0)
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("datosJuego/puntosA", finalPtsA);
        updates.put("datosJuego/puntosV", finalPtsV);
        salaRef.child("minijuegoActual").updateChildren(updates);

        // Esperamos 3 segundos para que todos vean la animación del choque
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded() || salaRef == null) return;

            if (finalPtsA >= 2 || finalPtsV >= 2) {
                // ==========================================
                // ALGUIEN LLEGÓ A 2 VICTORIAS (FIN DEL JUEGO)
                // ==========================================
                HashMap<String, Object> nextUpdates = new HashMap<>();
                nextUpdates.put("estado", "terminado");

                String ganadorRealUid = finalPtsA >= 2 ? uidAtacante : uidVictima;
                boolean atacanteGano = ganadorRealUid.equals(uidAtacante);

                nextUpdates.put("ganadorUid", ganadorRealUid);
                salaRef.child("minijuegoActual").updateChildren(nextUpdates);

                // Esperamos 3.5s viendo al ganador, cerramos el diálogo y ejecutamos el robo
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // --- FASE 4 BLINDADA: EL ROBO DE PUNTOS Y EL CHISME ---
                    salaRef.child("puntajesRondas").child("ronda_" + rondaActual)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshotPuntos) {
                                    int puntosRobados = leerEntero(snapshotPuntos.child(uidVictima), 0);
                                    int misPuntos = leerEntero(snapshotPuntos.child(uidAtacante), 0);

                                    HashMap<String, Object> actualizacionesFinales = new HashMap<>();
                                    actualizacionesFinales.put("minijuegoActual", null); // Destruye arena

                                    String idUso = "marca_resultado_" + rondaActual + "_" + uidAtacante + "_" + System.currentTimeMillis();
                                    String tituloGlobal;
                                    String mensajeGlobal;
                                    boolean esExitoGlobal;

                                    TextView txtNombreA = dialogMinijuegoActual != null ? dialogMinijuegoActual.findViewById(R.id.txtNombreA) : null;
                                    TextView txtNombreV = dialogMinijuegoActual != null ? dialogMinijuegoActual.findViewById(R.id.txtNombreV) : null;
                                    String nombreA = (txtNombreA != null) ? txtNombreA.getText().toString() : "El Atacante";
                                    String nombreV = (txtNombreV != null) ? txtNombreV.getText().toString() : "La Víctima";

                                    if (atacanteGano) {
                                        actualizacionesFinales.put("puntajesRondas/ronda_" + rondaActual + "/" + uidVictima, 0);
                                        actualizacionesFinales.put("puntajesRondas/ronda_" + rondaActual + "/" + uidAtacante, misPuntos + puntosRobados);
                                        tituloGlobal = "¡ATRACO PERFECTO!";
                                        mensajeGlobal = "<b><font color='#2AA514'>" + nombreA.toUpperCase() + "</font></b> ganó el duelo a muerte y le robó <b><font color='#E53935'>" + puntosRobados + " puntos</font></b> a <b>" + nombreV + "</b>.";
                                        esExitoGlobal = true;
                                    } else {
                                        tituloGlobal = "¡DEFENSA LEGENDARIA!";
                                        mensajeGlobal = "<b><font color='#2AA514'>" + nombreV.toUpperCase() + "</font></b> humilló a <b><font color='#E53935'>" + nombreA + "</font></b> en el duelo y protegió sus puntos.";
                                        esExitoGlobal = false;
                                    }

                                    actualizacionesFinales.put("eventoCaosGlobal/ronda_" + rondaActual + "/idUso", idUso);
                                    actualizacionesFinales.put("eventoCaosGlobal/ronda_" + rondaActual + "/titulo", tituloGlobal);
                                    actualizacionesFinales.put("eventoCaosGlobal/ronda_" + rondaActual + "/mensaje", mensajeGlobal);
                                    actualizacionesFinales.put("eventoCaosGlobal/ronda_" + rondaActual + "/esExito", esExitoGlobal);
                                    actualizacionesFinales.put("eventoCaosGlobal/ronda_" + rondaActual + "/timestamp", ServerValue.TIMESTAMP);
                                    actualizacionesFinales.put("eventoCaosGlobal/ronda_" + rondaActual + "/uidAtacante", uidAtacante);
                                    actualizacionesFinales.put("eventoCaosGlobal/ronda_" + rondaActual + "/uidVictima", uidVictima);

                                    salaRef.updateChildren(actualizacionesFinales).addOnCompleteListener(task -> {
                                        // SALVAVIDAS: Forzamos el cierre de la pantalla aquí mismo
                                        if (dialogMinijuegoActual != null && dialogMinijuegoActual.isShowing()) {
                                            dialogMinijuegoActual.dismiss();
                                            dialogMinijuegoActual = null;
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    salaRef.child("minijuegoActual").removeValue();
                                    if (dialogMinijuegoActual != null && dialogMinijuegoActual.isShowing()) {
                                        dialogMinijuegoActual.dismiss();
                                        dialogMinijuegoActual = null;
                                    }
                                }
                            });
                }, 3500);

            } else {
                // ==========================================
                // ¡AQUÍ ESTABA EL ERROR! EL ELSE PERDIDO
                // ==========================================
                // Nadie ha ganado el mejor de 3 todavía. Limpiamos las armas para el siguiente Round.
                HashMap<String, Object> nextUpdates = new HashMap<>();
                nextUpdates.put("estado", "jugando");
                nextUpdates.put("datosJuego/eleccionA", "");
                nextUpdates.put("datosJuego/eleccionV", "");
                salaRef.child("minijuegoActual").updateChildren(nextUpdates);
            }
        }, 3000);
    }

    private void escucharVotacionActiva() {
        if (salaRef == null) {
            return;
        }

        limpiarListenerVotacionActiva();

        nodoVotacionActivaRef = salaRef.child("votacionActiva").child("ronda_" + rondaActual);

        listenerVotacionActiva = nodoVotacionActivaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot votacionRonda) {
                if (!isAdded()) {
                    return;
                }

                Boolean activa = votacionRonda.child("activa").getValue(Boolean.class);
                String idVotacion = votacionRonda.child("idVotacion").getValue(String.class);

                if (activa == null || !activa || idVotacion == null) {
                    hayVotacionActivaEnRevision = false;

                    if (adapterValidacionActual != null) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (adapterValidacionActual != null) {
                                adapterValidacionActual.reiniciarTemporizadoresPausadosPorVotacion();
                            }
                        }, 300);
                    }

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

                String claveVotacion = votacionRonda.child("claveVotacion").getValue(String.class);

                if (adapterValidacionActual != null) {
                    adapterValidacionActual.pausarTemporizadoresPorVotacion(claveVotacion);
                }

                idVotacionModalActual = idVotacion;
                mostrarDialogoVotacion(rondaActual, idVotacion);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void limpiarListenerVotacionActiva() {
        if (nodoVotacionActivaRef != null && listenerVotacionActiva != null) {
            nodoVotacionActivaRef.removeEventListener(listenerVotacionActiva);
            listenerVotacionActiva = null;
            nodoVotacionActivaRef = null;
        }
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

    private void repararRondaCeroSiHaceFalta(String origen) {
        if (!isAdded() || salaRef == null || yaVolvioAlLobby) {
            return;
        }

        Log.d("TIEMPO", "Reparando ronda 0 desde: " + origen);

        rondaActual = 1;
        faseActual = "jugando";
        estadoPartidaActual = "En juego";

        txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
        txtEstadoPartida.setText("● En juego");

        desbloquearBotonBastaSiCorresponde();

        if ((esAnfitrion || esCreador) && !reparacionRondaCeroSolicitada) {
            reparacionRondaCeroSolicitada = true;
            prepararInicioRondaSiHaceFalta(1);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && !yaVolvioAlLobby) {
                setearCronometroGlobal();
            }
        }, 600);
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

        String respuestaTexto = snapshotVotacion.child("respuesta").getValue(String.class);
        String claveVotacion = snapshotVotacion.child("claveVotacion").getValue(String.class);

        if (claveVotacion == null || claveVotacion.trim().isEmpty()) {
            claveVotacion = calcularClaveVotacion(rondaVotacion, uidObjetivo, categoria, respuestaTexto);
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
                respuestaTexto
        );

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/claveVotacion",
                claveVotacion
        );

        String rutaResultado = "resultadosVotacionPalabras/ronda_" + rondaVotacion + "/" + claveVotacion;
        actualizaciones.put(rutaResultado + "/resuelta", true);
        actualizaciones.put(rutaResultado + "/resultado", resultado);
        actualizaciones.put(rutaResultado + "/correctaFinal", correctaFinal);
        actualizaciones.put(rutaResultado + "/puntosFinales", puntosFinales);
        actualizaciones.put(rutaResultado + "/uidObjetivo", uidObjetivo);
        actualizaciones.put(rutaResultado + "/categoria", categoria);
        actualizaciones.put(rutaResultado + "/respuesta", respuestaTexto);
        actualizaciones.put(rutaResultado + "/idVotacion", idVotacion);
        actualizaciones.put(rutaResultado + "/timestamp", ServerValue.TIMESTAMP);

        for (DataSnapshot votoSnap : snapshotVotacion.child("votos").getChildren()) {
            String uidVotante = votoSnap.getKey();
            String voto = votoSnap.getValue(String.class);

            if (uidVotante != null && voto != null) {
                actualizaciones.put(
                        "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/votos/" + uidVotante,
                        voto
                );

                actualizaciones.put(rutaResultado + "/votos/" + uidVotante, voto);
            }
        }

        if (votacionesLimitadas()) {
            actualizaciones.put(
                    "votacionesUsadas/ronda_" + rondaVotacion,
                    ServerValue.increment(1)
            );

            actualizaciones.put(
                    "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/consumioVotacion",
                    true
            );
        } else {
            actualizaciones.put(
                    "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/consumioVotacion",
                    false
            );

            actualizaciones.put(
                    "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/modo",
                    "sin_api"
            );
        }

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

        String uidObjetivo = snapshotVotacion.child("uidObjetivo").getValue(String.class);
        String categoria = snapshotVotacion.child("categoria").getValue(String.class);
        String respuestaTexto = snapshotVotacion.child("respuesta").getValue(String.class);
        String claveVotacion = snapshotVotacion.child("claveVotacion").getValue(String.class);

        if (claveVotacion == null || claveVotacion.trim().isEmpty()) {
            claveVotacion = calcularClaveVotacion(rondaVotacion, uidObjetivo, categoria, respuestaTexto);
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

        actualizaciones.put(
                "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/claveVotacion",
                claveVotacion
        );

        String rutaResultado = "resultadosVotacionPalabras/ronda_" + rondaVotacion + "/" + claveVotacion;
        actualizaciones.put(rutaResultado + "/resuelta", true);
        actualizaciones.put(rutaResultado + "/resultado", resultado);
        actualizaciones.put(rutaResultado + "/uidObjetivo", uidObjetivo);
        actualizaciones.put(rutaResultado + "/categoria", categoria);
        actualizaciones.put(rutaResultado + "/respuesta", respuestaTexto);
        actualizaciones.put(rutaResultado + "/idVotacion", idVotacion);
        actualizaciones.put(rutaResultado + "/timestamp", ServerValue.TIMESTAMP);

        for (DataSnapshot votoSnap : snapshotVotacion.child("votos").getChildren()) {
            String uidVotante = votoSnap.getKey();
            String voto = votoSnap.getValue(String.class);

            if (uidVotante != null && voto != null) {
                actualizaciones.put(
                        "historialVotaciones/ronda_" + rondaVotacion + "/" + idVotacion + "/votos/" + uidVotante,
                        voto
                );

                actualizaciones.put(rutaResultado + "/votos/" + uidVotante, voto);
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
                HashMap<String, Object> paqueteDeSubida = new HashMap<>(); // <-- Unificamos todo aquí

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
                    if (uidJugador == null) continue;

                    int totalRondaBase = 0;
                    int categoriasEvaluadas = 0;

                    for (DataSnapshot categoriaSnap : jugadorValidacionSnap.getChildren()) {
                        totalRondaBase += leerEntero(categoriaSnap.child("puntos"), 0);
                        categoriasEvaluadas++;
                    }

                    if (categoriasEvaluadas <= 0 && listaNombresCategorias != null) {
                        categoriasEvaluadas = listaNombresCategorias.size();
                    }

                    if (categoriasEvaluadas <= 0) categoriasEvaluadas = 1;

                    int totalRondaFinal = totalRondaBase;

                    Boolean dobleONadaActivo = snapshotSala
                            .child("comodinesPendientes")
                            .child("ronda_" + rondaCerrada)
                            .child(uidJugador)
                            .child("doble_o_nada")
                            .getValue(Boolean.class);

                    if (Boolean.TRUE.equals(dobleONadaActivo)) {
                        int puntosMaximos = categoriasEvaluadas * PUNTOS_MAXIMOS_POR_CATEGORIA;
                        int minimoNecesario = calcularMinimoDobleONada(categoriasEvaluadas);

                        if (totalRondaBase >= minimoNecesario) {
                            totalRondaFinal = totalRondaBase * 2;
                            paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/resultado", "duplicado");
                        } else {
                            totalRondaFinal = totalRondaBase - CASTIGO_DOBLE_O_NADA;
                            paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/resultado", "castigo");
                        }

                        paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/activo", true);
                        paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/puntosBase", totalRondaBase);
                        paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/puntosFinales", totalRondaFinal);
                        paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/categoriasEvaluadas", categoriasEvaluadas);
                        paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/puntosMaximos", puntosMaximos);
                        paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/minimoNecesario", minimoNecesario);
                        paqueteDeSubida.put("comodinesResultados/ronda_" + rondaCerrada + "/" + uidJugador + "/doble_o_nada/castigo", CASTIGO_DOBLE_O_NADA);
                    }

                    puntosPorJugador.put(uidJugador, totalRondaFinal);
                }

                // SOLUCIÓN BUG 2: Agregamos los puntajes al MISMO paquete de los comodines.
                // Así nos aseguramos de que todo se suba en el mismo milisegundo.
                for (Map.Entry<String, Integer> entry : puntosPorJugador.entrySet()) {
                    paqueteDeSubida.put("puntajesRondas/ronda_" + rondaCerrada + "/" + entry.getKey(), entry.getValue());
                }

                // Mandamos un solo updateChildren maestro
                nodoSala.updateChildren(paqueteDeSubida).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        actualizarPuntajesAcumulados(puntosPorJugador, rondaCerrada);
                    } else {
                        marcarResumenRondaListo(rondaCerrada);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("PUNTAJES", "Error leyendo sala: " + error.getMessage());
            }
        });
    }

    private int calcularMinimoDobleONada(int cantidadCategorias) {
        int categoriasSeguras = Math.max(1, cantidadCategorias);
        int puntosMaximos = categoriasSeguras * PUNTOS_MAXIMOS_POR_CATEGORIA;

        return (int) Math.ceil(puntosMaximos * PORCENTAJE_MINIMO_DOBLE_O_NADA);
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
        MaterialButton btnListo = vista.findViewById(R.id.btnVolverInicio);

        // NUEVO: Mapear el botón de comodines finales
        MaterialButton btnUsarComodinFinal = vista.findViewById(R.id.btnUsarComodinFinal);

        rvTabla.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (esFinal) {
            txtTitulo.setText("RESULTADOS FINALES");
            btnListo.setText("VOLVER AL LOBBY");
        } else {
            txtTitulo.setText("PUNTUACIONES - RONDA " + rondaCerrada);
            btnListo.setText("LISTO PARA SIGUIENTE RONDA");
        }

        btnListo.setEnabled(false);
        btnListo.setText("CARGANDO...");

        // NUEVO: Inicializamos el adapter vacío y lo pegamos al RecyclerView antes de consultar Firebase
        PuntuacionAdapter adapter = new PuntuacionAdapter(new ArrayList<>(), rondasMaximas);
        rvTabla.setAdapter(adapter);

        cargarPuntuacionesDesdeFirebase(rondasMaximas, lista -> {
            if (!isAdded()) {
                return;
            }

            // NUEVO: Usamos el método de actualización suave para que no parpadee si roban puntos
            adapter.actualizarListaConAnimacion(lista);

            txtGanador.setText(obtenerTextoGanador(lista));

            if (esFinal) {
                guardarMiHistorialUsuarioFinal(rondaCerrada);
            }

            btnListo.setEnabled(true);

            if (esFinal) {
                btnListo.setText("VOLVER AL LOBBY");
            } else {
                btnListo.setText("LISTO PARA SIGUIENTE RONDA");
            }
        });

        // NUEVO: Lógica del botón "USAR COMODÍN"
        if (esFinal) {
            btnUsarComodinFinal.setVisibility(View.GONE); // No hay comodines al finalizar el juego
        } else {
            btnUsarComodinFinal.setVisibility(View.VISIBLE);
            btnUsarComodinFinal.setOnClickListener(v -> {
                // Le pasamos "true" para indicarle al diálogo que estamos en fase final
                mostrarDialogoComodines(true);
            });
        }

        if (esFinal) {
            btnListo.setOnClickListener(v -> {
                btnListo.setEnabled(false);
                btnListo.setText("VOLVIENDO AL LOBBY...");
                solicitarVolverLobbyFinal(dialog);
            });

        } else {
            btnListo.setOnClickListener(v -> {
                marcarRevisionLista(rondaCerrada);

                btnListo.setEnabled(false);
                btnListo.setText("ESPERANDO JUGADORES...");
            });

            escucharJugadoresListosRevision(rondaCerrada, false);
        }

        // --- NUEVO: LISTENER GLOBAL PARA COMODINES DE CAOS EN TIEMPO REAL ---
        if (listenerEventoCaos != null) {
            salaRef.child("eventoCaosGlobal").child("ronda_" + rondaCerrada).removeEventListener(listenerEventoCaos);
        }

        final String[] ultimoEventoMostrado = {null};

        listenerEventoCaos = salaRef.child("eventoCaosGlobal").child("ronda_" + rondaCerrada)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        String idUso = snapshot.child("idUso").getValue(String.class);
                        if (idUso == null || idUso.equals(ultimoEventoMostrado[0])) return;

                        ultimoEventoMostrado[0] = idUso;

                        String titulo = snapshot.child("titulo").getValue(String.class);
                        String mensaje = snapshot.child("mensaje").getValue(String.class);
                        Boolean esExito = snapshot.child("esExito").getValue(Boolean.class);

                        // ---> ¡AGREGA ESTAS DOS LECTURAS! <---
                        String uidAtacante = snapshot.child("uidAtacante").getValue(String.class);
                        String uidVictima = snapshot.child("uidVictima").getValue(String.class);

                        if (titulo != null && mensaje != null && esExito != null) {
                            // Actualizamos la llamada para pasarle los UIDs
                            mostrarDialogoAnimadoFlujo(titulo, mensaje, esExito, uidAtacante, uidVictima);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
        // --------------------------------------------------------------------

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void mostrarDialogoAnimadoFlujo(String titulo, String mensaje, boolean esExito, String uidAtacante, String uidVictima) {
        if (!isAdded()) return;

        Dialog dialog = new Dialog(requireContext());
        View vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialogo_resultado_flujo, null);
        dialog.setContentView(vista);
        dialog.setCancelable(false);

        ImageView imgIcono = vista.findViewById(R.id.imgIconoResultado);
        TextView txtTitulo = vista.findViewById(R.id.txtTituloResultado);
        TextView txtMensaje = vista.findViewById(R.id.txtMensajeResultado);
        MaterialButton btnCerrar = vista.findViewById(R.id.btnCerrarResultado);
        LottieAnimationView lottieConfeti = vista.findViewById(R.id.lottieConfeti);

        txtTitulo.setText(android.text.Html.fromHtml(titulo));
        txtMensaje.setText(android.text.Html.fromHtml(mensaje));

        if (esExito) {
            imgIcono.setImageResource(R.drawable.ic_comodin_cambiodeflujo);
            txtTitulo.setTextColor(Color.parseColor("#2AA514"));
            btnCerrar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2AA514")));
            Sounds.playCelebracion();
            lottieConfeti.setVisibility(View.VISIBLE); // Mostramos el Lottie
            Animations.lanzarConfeti(lottieConfeti);
        } else {
            imgIcono.setImageResource(R.drawable.ic_comodin_cambiodeflujo);
            txtTitulo.setTextColor(Color.parseColor("#E53935"));
            btnCerrar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E53935")));
            Sounds.playPerdedor();
            lottieConfeti.setVisibility(View.GONE); // Ocultamos el Lottie para que no deje el punto azul
        }

        Animations.animacionZoom(vista.findViewById(R.id.imgIconoResultado));

        btnCerrar.setOnClickListener(v -> {
            dialog.dismiss(); // Cerramos el chisme

            if (dialogPuntuacionesActual != null && dialogPuntuacionesActual.isShowing()) {
                RecyclerView rvTabla = dialogPuntuacionesActual.findViewById(R.id.rvTablaPuntuaciones);

                if (rvTabla != null && rvTabla.getAdapter() instanceof PuntuacionAdapter) {
                    PuntuacionAdapter adapter = (PuntuacionAdapter) rvTabla.getAdapter();

                    // NUEVO: Verificamos si es la Ruleta Caótica
                    if ("RULETA".equals(uidAtacante)) {
                        // Animamos a todos con el símbolo (que viene guardado en uidVictima)
                        String colorAnimacion = esExito ? "#FF9800" : "#E53935";
                        adapter.animarATodos(rvTabla, uidVictima, colorAnimacion);
                    }
                    // Lógica original del Cambio de Flujo
                    else if (esExito && uidAtacante != null && uidVictima != null) {
                        adapter.animarPuntaje(rvTabla, uidAtacante, "🔄", "#FF9800");
                        if (!uidAtacante.equals(uidVictima)) {
                            adapter.animarPuntaje(rvTabla, uidVictima, "🔄", "#FF9800");
                        }
                    } else if (!esExito && uidAtacante != null) {
                        adapter.animarPuntaje(rvTabla, uidAtacante, "-5", "#E53935");
                    }
                }
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void cargarPuntuacionesDesdeFirebase(int cantidadRondas, PuntuacionesCallback callback) {
        if (salaRef == null) {
            callback.onCargadas(new ArrayList<>());
            return;
        }

        salaRef.child("jugadores").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {

                // Limpiamos el listener anterior para evitar duplicados
                if (listenerPuntuacionesEnVivo != null) {
                    salaRef.child("puntajesRondas").removeEventListener(listenerPuntuacionesEnVivo);
                }

                // AHORA ESCUCHAMOS EN VIVO LOS CAMBIOS DE LA TABLA
                listenerPuntuacionesEnVivo = salaRef.child("puntajesRondas").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotPuntajesRondas) {
                        List<PuntuacionJugador> lista = new ArrayList<>();

                        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                            String uid = jugadorSnap.getKey();
                            if (uid == null) continue;

                            String nombre = jugadorSnap.child("nombre").getValue(String.class);
                            if (nombre == null || nombre.trim().isEmpty()) nombre = "Jugador";

                            List<Integer> puntajes = new ArrayList<>();
                            int total = 0;

                            for (int ronda = 1; ronda <= cantidadRondas; ronda++) {
                                Integer puntos = snapshotPuntajesRondas.child("ronda_" + ronda).child(uid).getValue(Integer.class);
                                puntajes.add(puntos);
                                if (puntos != null) total += puntos;
                            }
                            lista.add(new PuntuacionJugador(0, uid, nombre, puntajes, total));
                        }

                        // Ordenar y asignar posiciones
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
            resetearSalaParaLobbyYVolver();
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
        actualizaciones.put("inicioRonda", null);
        actualizaciones.put("inicioRondaDe", null);
        actualizaciones.put("inicioRondaListoDe", null);
        actualizaciones.put("inicioRondaListoDe", null);
        actualizaciones.put("entradaPartida/ronda_" + siguienteRonda, null);
        actualizaciones.put("esperaInicioRonda/ronda_" + siguienteRonda, null);
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
        resetearSalaParaLobbyYVolver();
    }

    private void escucharMiRolHostEnPartida() {
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        String miUid = auth.getCurrentUser().getUid();

        nodoMiJugadorPartidaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("jugadores")
                .child(miUid);

        listenerMiRolHost = nodoMiJugadorPartidaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }

                if (!snapshot.exists()) {
                    return;
                }

                Boolean hostFirebase = snapshot.child("esHost").getValue(Boolean.class);

                boolean nuevoValorHost = Boolean.TRUE.equals(hostFirebase);

                if (esAnfitrion != nuevoValorHost) {
                    esAnfitrion = nuevoValorHost;

                    if (getArguments() != null) {
                        getArguments().putBoolean("ES_ANFITRION", esAnfitrion);
                    }

                    Log.d("HOST_PARTIDA", "Rol de host actualizado en partida: " + esAnfitrion);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("HOST_PARTIDA", "Error escuchando rol host: " + error.getMessage());
            }
        });
    }

    private void solicitarVolverLobbyFinal(Dialog dialog) {
        if (salaRef == null) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            volverAlLobbyDespuesDePartida();
            return;
        }

        DatabaseReference candadoResetRef = salaRef.child("reseteoFinalEnProceso");

        candadoResetRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Boolean enProceso = currentData.getValue(Boolean.class);

                if (enProceso != null && enProceso) {
                    return Transaction.abort();
                }

                currentData.setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    Log.d("FINAL", "Error solicitando regreso al lobby: " + error.getMessage());
                    esperarSalaReseteadaParaVolverAlLobby(dialog);
                    return;
                }

                if (committed) {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    resetearSalaParaLobbyYVolver();
                } else {
                    esperarSalaReseteadaParaVolverAlLobby(dialog);
                }
            }
        });
    }

    private void esperarSalaReseteadaParaVolverAlLobby(Dialog dialog) {
        if (salaRef == null) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            volverAlLobbyDespuesDePartida();
            return;
        }

        limpiarListenerEsperandoLobbyFinal();

        nodoEsperandoLobbyFinalRef = salaRef;

        listenerEsperandoLobbyFinal = nodoEsperandoLobbyFinalRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }

                String estado = snapshot.child("estadoPartida").getValue(String.class);
                String fase = snapshot.child("fase").getValue(String.class);
                int rondaFirebase = leerEntero(snapshot.child("rondaActual"), rondaActual);

                boolean salaYaEstaEnLobby = rondaFirebase == 0 &&
                        ("lobby".equalsIgnoreCase(fase) ||
                                "Esperando jugadores".equalsIgnoreCase(estado));

                if (salaYaEstaEnLobby) {
                    limpiarListenerEsperandoLobbyFinal();

                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    volverAlLobbyDespuesDePartida();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                limpiarListenerEsperandoLobbyFinal();

                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }

                volverAlLobbyDespuesDePartida();
            }
        });
    }

    private void limpiarListenerEsperandoLobbyFinal() {
        if (nodoEsperandoLobbyFinalRef != null && listenerEsperandoLobbyFinal != null) {
            nodoEsperandoLobbyFinalRef.removeEventListener(listenerEsperandoLobbyFinal);
            listenerEsperandoLobbyFinal = null;
            nodoEsperandoLobbyFinalRef = null;
        }
    }

    private int obtenerRondaFinalParaHistorial() {
        if (rondasMaximas > 0) {
            return rondasMaximas;
        }

        if (rondaActual > 0) {
            return rondaActual;
        }

        return 1;
    }


    private void guardarMiHistorialUsuarioFinal(int rondaFinal) {
        if (miHistorialFinalGuardado) {
            return;
        }

        if (salaRef == null || !VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        miHistorialFinalGuardado = true;

        String miUid = auth.getCurrentUser().getUid();
        int rondaFinalSegura = Math.max(1, rondaFinal);

        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                try {
                    DataSnapshot snapshotJugadores = snapshotSala.child("jugadores");
                    DataSnapshot snapshotPuntajesRondas = snapshotSala.child("puntajesRondas");

                    ArrayList<RegistroJugadorHistorial> tabla = new ArrayList<>();

                    for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                        String uid = jugadorSnap.getKey();

                        if (uid == null) {
                            continue;
                        }

                        String nombre = jugadorSnap.child("nombre").getValue(String.class);

                        if (nombre == null || nombre.trim().isEmpty()) {
                            nombre = "Jugador";
                        }

                        String avatarUrl = jugadorSnap.child("urlAvatar").getValue(String.class);

                        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                            avatarUrl = jugadorSnap.child("fotoPerfilUrl").getValue(String.class);
                        }

                        if (avatarUrl == null) {
                            avatarUrl = "";
                        }

                        ArrayList<Integer> puntajes = new ArrayList<>();
                        int total = 0;

                        for (int ronda = 1; ronda <= rondaFinalSegura; ronda++) {
                            int puntosRonda = leerEntero(
                                    snapshotPuntajesRondas
                                            .child("ronda_" + ronda)
                                            .child(uid),
                                    0
                            );

                            puntajes.add(puntosRonda);
                            total += puntosRonda;
                        }

                        tabla.add(new RegistroJugadorHistorial(uid, nombre, avatarUrl, total, puntajes));
                    }

                    Collections.sort(tabla, (a, b) -> Integer.compare(b.total, a.total));

                    boolean puedeDarMonedas = tabla.size() >= 3 && rondaFinalSegura >= 2;

                    RegistroJugadorHistorial miRegistro = null;

                    for (int i = 0; i < tabla.size(); i++) {
                        RegistroJugadorHistorial jugador = tabla.get(i);
                        jugador.posicion = i + 1;
                        jugador.monedas = calcularMonedasPorPuesto(jugador.posicion, puedeDarMonedas);

                        if (jugador.uid.equals(miUid)) {
                            miRegistro = jugador;
                        }
                    }

                    if (miRegistro == null) {
                        Log.d("HISTORIAL", "No se encontró mi usuario en la tabla final");
                        return;
                    }

                    guardarMiHistorialEnUsuario(tabla, miRegistro, rondaFinalSegura, puedeDarMonedas);

                } catch (Exception e) {
                    Log.d("HISTORIAL", "Error guardando mi historial: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("HISTORIAL", "Error leyendo sala para mi historial: " + error.getMessage());
            }
        });
    }

    private void guardarMiHistorialEnUsuario(
            ArrayList<RegistroJugadorHistorial> tabla,
            RegistroJugadorHistorial miRegistro,
            int rondaFinal,
            boolean puedeDarMonedas
    ) {
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        String miUid = auth.getCurrentUser().getUid();

        DatabaseReference usuarioRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(miUid);

        String idPartidaGenerada = usuarioRef.child("historialPartidas").push().getKey();

        if (idPartidaGenerada == null) {
            idPartidaGenerada = "partida_" + System.currentTimeMillis();
        }

        final String idPartida = idPartidaGenerada;

        String rutaPartida = "historialPartidas/" + idPartida;

        HashMap<String, Object> actualizaciones = new HashMap<>();

        actualizaciones.put(rutaPartida + "/idPartida", idPartida);
        actualizaciones.put(rutaPartida + "/idSala", idSala);
        actualizaciones.put(rutaPartida + "/codigoSala", codigoSala);
        actualizaciones.put(rutaPartida + "/fecha", ServerValue.TIMESTAMP);
        actualizaciones.put(rutaPartida + "/cantidadRondas", rondaFinal);
        actualizaciones.put(rutaPartida + "/jugadoresCantidad", tabla.size());
        actualizaciones.put(rutaPartida + "/monedasEntregadas", puedeDarMonedas);

        actualizaciones.put(rutaPartida + "/miUid", miRegistro.uid);
        actualizaciones.put(rutaPartida + "/miNombre", miRegistro.nombre);
        actualizaciones.put(rutaPartida + "/miPosicion", miRegistro.posicion);
        actualizaciones.put(rutaPartida + "/miTotal", miRegistro.total);
        actualizaciones.put(rutaPartida + "/monedas", miRegistro.monedas);

        if (!tabla.isEmpty()) {
            actualizaciones.put(rutaPartida + "/ganadorUid", tabla.get(0).uid);
            actualizaciones.put(rutaPartida + "/ganadorNombre", tabla.get(0).nombre);
            actualizaciones.put(rutaPartida + "/puntajeGanador", tabla.get(0).total);
        }

        int limiteTop = Math.min(3, tabla.size());

        for (int i = 0; i < limiteTop; i++) {
            RegistroJugadorHistorial jugador = tabla.get(i);
            String rutaTop = rutaPartida + "/top3/puesto_" + jugador.posicion;

            actualizaciones.put(rutaTop + "/uid", jugador.uid);
            actualizaciones.put(rutaTop + "/nombre", jugador.nombre);
            actualizaciones.put(rutaTop + "/avatarUrl", jugador.avatarUrl == null ? "" : jugador.avatarUrl);
            actualizaciones.put(rutaTop + "/posicion", jugador.posicion);
            actualizaciones.put(rutaTop + "/total", jugador.total);
        }

        actualizaciones.put("partidasJugadas", ServerValue.increment(1));
        actualizaciones.put("puntosTotalesHistoricos", ServerValue.increment(miRegistro.total));

        if (miRegistro.posicion == 1) {
            actualizaciones.put("partidasGanadas", ServerValue.increment(1));
        }

        if (miRegistro.monedas > 0) {
            actualizaciones.put("monedas", ServerValue.increment(miRegistro.monedas));
        }

        usuarioRef.updateChildren(actualizaciones).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("HISTORIAL", "Mi historial fue guardado: " + idPartida);
                limitarHistorialUsuarioA5(usuarioRef);
            } else {
                Log.d("HISTORIAL", "No se pudo guardar mi historial. Revisa las rules de usuarios/" + miUid);
            }
        });
    }

    private void limitarHistorialUsuarioA5(DatabaseReference usuarioRef) {
        usuarioRef.child("historialPartidas")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<HistorialClaveFecha> partidas = new ArrayList<>();

                        for (DataSnapshot partidaSnap : snapshot.getChildren()) {
                            String key = partidaSnap.getKey();
                            long fecha = leerLong(partidaSnap.child("fecha"), 0L);

                            if (key != null) {
                                partidas.add(new HistorialClaveFecha(key, fecha));
                            }
                        }

                        Collections.sort(partidas, (a, b) -> Long.compare(b.fecha, a.fecha));

                        HashMap<String, Object> borrados = new HashMap<>();

                        for (int i = 5; i < partidas.size(); i++) {
                            borrados.put("historialPartidas/" + partidas.get(i).key, null);
                        }

                        if (!borrados.isEmpty()) {
                            usuarioRef.updateChildren(borrados);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private long leerLong(DataSnapshot snapshot, long valorDefault) {
        Object valor = snapshot.getValue();

        if (valor instanceof Number) {
            return ((Number) valor).longValue();
        }

        if (valor instanceof String) {
            try {
                return Long.parseLong((String) valor);
            } catch (NumberFormatException e) {
                return valorDefault;
            }
        }

        return valorDefault;
    }

    private void guardarHistorialYRecompensasSiHaceFalta(
            int rondaFinal,
            OperacionTerminadaCallback callback
    ) {
        if (salaRef == null) {
            if (callback != null) {
                callback.onTermino();
            }
            return;
        }

        if (historialGuardandose) {
            Log.d("HISTORIAL", "Ya se está guardando historial. Continuando reset para no bloquear.");
            if (callback != null) {
                callback.onTermino();
            }
            return;
        }

        historialGuardandose = true;

        final boolean[] procesoTerminado = {false};
        Handler handler = new Handler(Looper.getMainLooper());

        OperacionTerminadaCallback callbackSeguro = () -> {
            if (procesoTerminado[0]) {
                return;
            }

            procesoTerminado[0] = true;
            historialGuardandose = false;

            if (callback != null) {
                callback.onTermino();
            }
        };

        handler.postDelayed(() -> {
            if (!procesoTerminado[0]) {
                Log.d("HISTORIAL", "Timeout guardando historial. Se continúa con el regreso al lobby.");
                callbackSeguro.onTermino();
            }
        }, 7000);

        DatabaseReference candadoHistorialRef = salaRef.child("historialGuardado");

        candadoHistorialRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Boolean yaGuardado = currentData.getValue(Boolean.class);

                if (yaGuardado != null && yaGuardado) {
                    return Transaction.abort();
                }

                currentData.setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (procesoTerminado[0]) {
                    return;
                }

                if (error != null) {
                    Log.d("HISTORIAL", "Error creando candado: " + error.getMessage());
                    callbackSeguro.onTermino();
                    return;
                }

                if (!committed) {
                    Log.d("HISTORIAL", "La partida ya tenía historial guardado o alguien más lo está guardando.");
                    callbackSeguro.onTermino();
                    return;
                }

                construirYGuardarHistorialPartida(rondaFinal, callbackSeguro);
            }
        });
    }

    private void construirYGuardarHistorialPartida(
            int rondaFinal,
            OperacionTerminadaCallback callback
    ) {
        if (salaRef == null) {
            if (callback != null) {
                callback.onTermino();
            }
            return;
        }

        int rondaFinalSegura = Math.max(1, rondaFinal);

        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                try {
                    DataSnapshot snapshotJugadores = snapshotSala.child("jugadores");
                    DataSnapshot snapshotPuntajesRondas = snapshotSala.child("puntajesRondas");

                    ArrayList<RegistroJugadorHistorial> tabla = new ArrayList<>();

                    for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                        String uid = jugadorSnap.getKey();

                        if (uid == null) {
                            continue;
                        }

                        String nombre = jugadorSnap.child("nombre").getValue(String.class);

                        if (nombre == null || nombre.trim().isEmpty()) {
                            nombre = "Jugador";
                        }

                        ArrayList<Integer> puntajes = new ArrayList<>();
                        int total = 0;

                        for (int ronda = 1; ronda <= rondaFinalSegura; ronda++) {
                            int puntosRonda = leerEntero(
                                    snapshotPuntajesRondas
                                            .child("ronda_" + ronda)
                                            .child(uid),
                                    0
                            );

                            puntajes.add(puntosRonda);
                            total += puntosRonda;
                        }

                        tabla.add(new RegistroJugadorHistorial(uid, nombre, "", total, puntajes));
                    }

                    Collections.sort(tabla, (a, b) -> Integer.compare(b.total, a.total));

                    boolean puedeDarMonedas = tabla.size() >= 3 && rondaFinalSegura >= 2;

                    for (int i = 0; i < tabla.size(); i++) {
                        RegistroJugadorHistorial jugador = tabla.get(i);
                        jugador.posicion = i + 1;
                        jugador.monedas = calcularMonedasPorPuesto(jugador.posicion, puedeDarMonedas);
                    }

                    guardarHistorialEnFirebase(tabla, rondaFinalSegura, puedeDarMonedas, callback);

                } catch (Exception e) {
                    Log.d("HISTORIAL", "Error armando historial: " + e.getMessage());

                    if (callback != null) {
                        callback.onTermino();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("HISTORIAL", "Error leyendo sala: " + error.getMessage());

                if (callback != null) {
                    callback.onTermino();
                }
            }
        });
    }

    private void guardarHistorialEnFirebase(
            ArrayList<RegistroJugadorHistorial> tabla,
            int rondaFinal,
            boolean puedeDarMonedas,
            OperacionTerminadaCallback callback
    ) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        String idPartida = rootRef
                .child(NODO_HISTORIAL_PARTIDAS)
                .push()
                .getKey();

        if (idPartida == null) {
            if (callback != null) {
                callback.onTermino();
            }
            return;
        }

        HashMap<String, Object> datosHistorial = new HashMap<>();
        HashMap<String, Object> tablaFinal = new HashMap<>();

        datosHistorial.put("idPartida", idPartida);
        datosHistorial.put("idSala", idSala);
        datosHistorial.put("codigoSala", codigoSala);
        datosHistorial.put("fecha", ServerValue.TIMESTAMP);
        datosHistorial.put("cantidadRondas", rondaFinal);
        datosHistorial.put("jugadoresCantidad", tabla.size());
        datosHistorial.put("categorias", listaNombresCategorias);
        datosHistorial.put("monedasEntregadas", puedeDarMonedas);

        if (!tabla.isEmpty()) {
            datosHistorial.put("ganadorUid", tabla.get(0).uid);
            datosHistorial.put("ganadorNombre", tabla.get(0).nombre);
            datosHistorial.put("puntajeGanador", tabla.get(0).total);
        }

        for (RegistroJugadorHistorial jugador : tabla) {
            HashMap<String, Object> datosJugador = new HashMap<>();
            datosJugador.put("uid", jugador.uid);
            datosJugador.put("nombre", jugador.nombre);
            datosJugador.put("posicion", jugador.posicion);
            datosJugador.put("total", jugador.total);
            datosJugador.put("monedas", jugador.monedas);
            datosJugador.put("puntajesRondas", jugador.puntajesRondas);

            tablaFinal.put(jugador.uid, datosJugador);
        }

        datosHistorial.put("tablaFinal", tablaFinal);

        rootRef.child(NODO_HISTORIAL_PARTIDAS)
                .child(idPartida)
                .setValue(datosHistorial)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("HISTORIAL", "Historial global guardado: " + idPartida);
                    } else {
                        Log.d("HISTORIAL", "No se pudo guardar historial global. Se continuará igual.");
                    }

                    guardarCopiasUsuariosYMonedas(tabla, idPartida, rondaFinal, callback);
                });
    }

    private void guardarCopiasUsuariosYMonedas(
            ArrayList<RegistroJugadorHistorial> tabla,
            String idPartida,
            int rondaFinal,
            OperacionTerminadaCallback callback
    ) {
        if (tabla == null || tabla.isEmpty()) {
            if (callback != null) {
                callback.onTermino();
            }
            return;
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        AtomicInteger pendientes = new AtomicInteger(tabla.size());
        String ganadorNombre = tabla.get(0).nombre;

        for (RegistroJugadorHistorial jugador : tabla) {
            HashMap<String, Object> actualizacionesUsuario = new HashMap<>();

            String rutaHistorialUsuario = "historialPartidas/" + idPartida;

            actualizacionesUsuario.put(rutaHistorialUsuario + "/idPartida", idPartida);
            actualizacionesUsuario.put(rutaHistorialUsuario + "/codigoSala", codigoSala);
            actualizacionesUsuario.put(rutaHistorialUsuario + "/fecha", ServerValue.TIMESTAMP);
            actualizacionesUsuario.put(rutaHistorialUsuario + "/posicion", jugador.posicion);
            actualizacionesUsuario.put(rutaHistorialUsuario + "/total", jugador.total);
            actualizacionesUsuario.put(rutaHistorialUsuario + "/monedas", jugador.monedas);
            actualizacionesUsuario.put(rutaHistorialUsuario + "/ganadorNombre", ganadorNombre);
            actualizacionesUsuario.put(rutaHistorialUsuario + "/cantidadRondas", rondaFinal);

            actualizacionesUsuario.put("partidasJugadas", ServerValue.increment(1));
            actualizacionesUsuario.put("puntosTotalesHistoricos", ServerValue.increment(jugador.total));

            if (jugador.posicion == 1) {
                actualizacionesUsuario.put("partidasGanadas", ServerValue.increment(1));
            }

            if (jugador.monedas > 0) {
                actualizacionesUsuario.put("monedas", ServerValue.increment(jugador.monedas));
            }

            rootRef.child(NODO_USUARIOS)
                    .child(jugador.uid)
                    .updateChildren(actualizacionesUsuario)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.d("HISTORIAL", "No se pudo guardar copia de usuario/recompensa para " + jugador.uid);
                        }

                        if (pendientes.decrementAndGet() == 0) {
                            if (callback != null) {
                                callback.onTermino();
                            }
                        }
                    });
        }
    }

    private int calcularMonedasPorPuesto(int posicion, boolean puedeDarMonedas) {
        if (!puedeDarMonedas) {
            return 0;
        }

        switch (posicion) {
            case 1:
                return MONEDAS_PRIMER_LUGAR;
            case 2:
                return MONEDAS_SEGUNDO_LUGAR;
            case 3:
                return MONEDAS_TERCER_LUGAR;
            case 4:
                return MONEDAS_CUARTO_LUGAR;
            default:
                return MONEDAS_PARTICIPACION;
        }
    }

    private void resetearSalaParaLobbyYVolver() {
        resetearSalaParaLobbyYVolverDespuesDeHistorial();
    }

    private void resetearSalaParaLobbyYVolverDespuesDeHistorial() {
        if (salaRef == null) {
            volverAlLobbyDespuesDePartida();
            return;
        }

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
        actualizaciones.put("resultadosVotacionPalabras", null);
        actualizaciones.put("cooldownVotacion", null);
        actualizaciones.put("revisionCategorias", null);
        actualizaciones.put("comodinesPendientes", null);
        actualizaciones.put("comodinesActivos", null);
        actualizaciones.put("comodinesUsados", null);
        actualizaciones.put("comodinesResultados", null);
        actualizaciones.put("reseteoFinalEnProceso", null);
        actualizaciones.put("cambioRondaTimestamp", null);
        actualizaciones.put("historialGuardado", null);
        actualizaciones.put("entradaPartida", null);
        actualizaciones.put("esperaInicioRonda", null);

        actualizaciones.put("finPartida", ServerValue.TIMESTAMP);

        salaRef.child("jugadores").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                    String uid = jugadorSnap.getKey();

                    if (uid != null) {
                        actualizaciones.put("jugadores/" + uid + "/estaListo", false);
                        actualizaciones.put("jugadores/" + uid + "/puntajePartida", 0);
                    }
                }

                salaRef.updateChildren(actualizaciones).addOnCompleteListener(task -> {
                    letraElegida = nuevaLetra;
                    volverAlLobbyDespuesDePartida();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                volverAlLobbyDespuesDePartida();
            }
        });
    }

    private void volverAlLobbyDespuesDePartida() {
        if (!isAdded() || yaVolvioAlLobby) {
            return;
        }

        yaVolvioAlLobby = true;

        versionCronometro++;

        respuestasGuardadas = false;
        modalValidacionMostrado = false;
        cierreRondaSolicitado = false;
        bastaActivado = false;

        faseActual = "lobby";
        estadoPartidaActual = "Esperando jugadores";

        if (handlerComodines != null && runnableDesbloquearBasta != null) {
            handlerComodines.removeCallbacks(runnableDesbloquearBasta);
            runnableDesbloquearBasta = null;
        }

        if (cronometroRonda != null) {
            cronometroRonda.cancel();
            cronometroRonda = null;
        }

        Bundle datosLobby = new Bundle();
        datosLobby.putString("CODIGO_SALA", codigoSala);
        datosLobby.putBoolean("ES_ANFITRION", esAnfitrion);
        datosLobby.putStringArrayList("CATEGORIAS", listaNombresCategorias);
        datosLobby.putInt("TIEMPO", tiempoSeconds);
        datosLobby.putInt("JUGADORES", jugadoresMaximos);
        datosLobby.putInt("RONDAS", rondasMaximas);
        datosLobby.putBoolean("CREADOR", false);
        datosLobby.putString("IDSALA", idSala);
        datosLobby.putBoolean("ANEXO", false);
        datosLobby.putInt("RONDA_ACTUAL", 0);
        datosLobby.putString("LETRA", letraElegida);

        LobbyFragment lobby = new LobbyFragment();
        lobby.setArguments(datosLobby);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.sala_fragment_container, lobby)
                .commitAllowingStateLoss();
    }


    private int dp(int valor) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(valor * density);
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
            case "pelicula":
            case "película": return "peliculas";
            case "cancion":
            case "canción": return "canciones";
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
            case "peliculas": return "Pelicula";
            case "películas": return "Pelicula";
            case "canciones": return "Cancion";
            default: return categoriaApi;
        }
    }

    private static class HistorialClaveFecha {
        String key;
        long fecha;

        HistorialClaveFecha(String key, long fecha) {
            this.key = key;
            this.fecha = fecha;
        }
    }

    private static class RegistroJugadorHistorial {
        String uid;
        String nombre;
        String avatarUrl;
        int total;
        int posicion;
        int monedas;
        ArrayList<Integer> puntajesRondas;

        RegistroJugadorHistorial(
                String uid,
                String nombre,
                String avatarUrl,
                int total,
                ArrayList<Integer> puntajesRondas
        ) {
            this.uid = uid;
            this.nombre = nombre;
            this.avatarUrl = avatarUrl;
            this.total = total;
            this.puntajesRondas = puntajesRondas;
        }
    }
}