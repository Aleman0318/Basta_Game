package com.example.basta_game.Fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import com.example.basta_game.utils.PlazoRonda;
import com.example.basta_game.utils.RecompensasPartida;
import com.example.basta_game.utils.InicioPartida;
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
import com.example.basta_game.comodines.ComodinVientoDelCaos;
import com.example.basta_game.comodines.ComodinEspejo;
import com.example.basta_game.comodines.ComodinDecretoSilencio;
import com.example.basta_game.utils.VientoDelCaosOrden;
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
    private static final String NODO_USUARIOS = "usuarios";
    private static final String NODO_HISTORIAL_PARTIDAS = "historialPartidas";

    private static final int MONEDAS_PRIMER_LUGAR = 100;
    private static final int MONEDAS_SEGUNDO_LUGAR = 70;
    private static final int MONEDAS_TERCER_LUGAR = 45;
    private static final int MONEDAS_CUARTO_LUGAR = 25;
    private static final int MONEDAS_PARTICIPACION = 15;

    private ValueEventListener listenerEventoCaos;
    private ValueEventListener listenerVientoDelCaos;
    private DatabaseReference nodoVientoDelCaos;
    private boolean vientoActivacionPendiente = false;
    private boolean espejoActivacionPendiente = false;
    private boolean decretoActivacionPendiente = false;
    private ValueEventListener listenerDecreto;
    private DatabaseReference nodoDecreto;
    private DatabaseReference nodoEvaluacionDecreto;
    private ValueEventListener listenerEvaluacionDecreto;
    private final java.util.Set<String> categoriasDecretoActual = new java.util.HashSet<>();
    private String vocalDecretoActual;
    private ValueEventListener listenerEspejo;
    private DatabaseReference nodoEspejo;
    private Dialog dialogSeleccionEspejo;
    private final Handler handlerEspejo = new Handler(Looper.getMainLooper());
    private Runnable finEspejoPendiente;
    private boolean espejoActivoLocal = false;
    private boolean historialGuardandose = false;
    private boolean miHistorialFinalGuardado = false;
    private String idPartidaActual;
    private boolean recompensaEnProceso = false;

    private String codigoSala;
    private int tiempoSeconds;
    private String idSala;
    private int jugadoresMaximos;
    private boolean esAnfitrion;
    private int rondasMaximas;
    private boolean esCreador; // Metadato historico; no concede autoridad.
    private boolean cierrePorAbandonoMostrado = false;
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
    private TextView txtDecretoResumenPartida;

    private ImageView imgLetraActual;

    // --- VARIABLES DE COOLDOWN ---
    private java.util.HashSet<String> comodinesUsadosEstaRonda = new java.util.HashSet<>();

    private AppCompatButton btnBasta;
    private long inicioRespuestasActual;
    private boolean solicitudBastaPendiente;
    private MaterialButton btnComodines;

    private View scrollPartida;
    private RecyclerView rvJugadoresPartida;
    private RecyclerView rvCategoriasJuego;

    private ValueEventListener listenerMiRolHost;
    private DatabaseReference nodoMiJugadorPartidaRef;
    private boolean yaVolvioAlLobby = false;
    private boolean avisoCierrePorAbandono;

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

    private CountDownTimer timerTurnoTER;

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
    private Dialog dialogResumenActual;
    private boolean resumenEnCarga;
    private Dialog dialogComodinesActual;

    private TarjetasValidacionAdapter adapterValidacionActual;

    private String idVotacionModalActual = null;
    private boolean resolviendoVotacion = false;
    private com.example.basta_game.utils.CoordinadorRevision coordinadorRevision;
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
    private boolean congeladorActivandose = false;

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

        if (savedInstanceState != null && idPartidaActual != null &&
                idPartidaActual.equals(savedInstanceState.getString("borradorPartida")) &&
                rondaActual == savedInstanceState.getInt("borradorRonda", -1)) {
            Bundle borrador = savedInstanceState.getBundle("borradorRespuestas");
            if (borrador != null) for (String categoria : borrador.keySet()) {
                respuestasJugador.put(categoria, borrador.getString(categoria, ""));
            }
        }

        cargarDatosIniciales();

        if (rondaActual > 0) {
            resetearEstadosParaPartidaActiva();
        }

        escucharMinijuegoActivo();

        escucharCambiosDeRonda();
        escucharVientoDelCaos();
        escucharEspejo();
        escucharDecreto();
        escucharDefensas();
        escucharMiRolHostEnPartida();

        setearCronometroGlobal();
        escucharBastaParaCronometro();
        escucharVotacionActiva();
        escucharCongeladorActivo();
        configurarBotones();
        ObtenerJugadoresYLlenarPartida(idSala);
        cambiarEstadoUsuarios();
        escucharCierreRespuestas();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString("borradorPartida", idPartidaActual);
        estado.putInt("borradorRonda", rondaActual);
        Bundle respuestas = new Bundle();
        for (Map.Entry<String,String> entrada : respuestasJugador.entrySet()) respuestas.putString(entrada.getKey(), entrada.getValue());
        estado.putBundle("borradorRespuestas", respuestas);
        if (getArguments() != null) getArguments().putInt("RONDA_ACTUAL", rondaActual);
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
            idPartidaActual = getArguments().getString("ID_PARTIDA");
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
        txtDecretoResumenPartida = view.findViewById(R.id.txtDecretoResumenPartida);
        btnAbandonar = view.findViewById(R.id.btnSalida);
        imgLetraActual = view.findViewById(R.id.imgLetraActual);
        btnBasta = view.findViewById(R.id.btnBasta);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(btnBasta,12,26,1,android.util.TypedValue.COMPLEX_UNIT_SP);
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
        com.example.basta_game.utils.InvitacionSala.colorear(txtCodigoSala,codigoSala);
        txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
        txtTiempoRestante.setText(formatearTiempo(tiempoSeconds));
        mostrarLetra(letraElegida);

        rvJugadoresPartida.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        CategoriaPartidaAdapter.configurarScrollInterno(rvCategoriasJuego);

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

        CategoriaPartidaAdapter adaptador = new CategoriaPartidaAdapter(
                listaCategorias,
                respuestasJugador,
                this::subirCampoRespuestaAlTeclado
        );
        // Recupera la restriccion compartida sin reiniciar el teclado.
        adaptador.mostrarDecreto(categoriasDecretoActual,vocalDecretoActual);
        // El Espejo se activa al recibir el evento remoto con su identificador y finMs.
        rvCategoriasJuego.setAdapter(adaptador);
    }


    /** Escucha el orden de categorías de ESTA partida y ronda únicamente. */
    private void escucharVientoDelCaos() {
        limpiarListenerVientoDelCaos();
        if (salaRef == null || rondaActual <= 0) return;
        nodoVientoDelCaos = salaRef.child("efectosVisuales").child("vientoCaos")
                .child("ronda_" + rondaActual);
        listenerVientoDelCaos = nodoVientoDelCaos.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot evento) {
                if (!isAdded() || !evento.exists() || !"jugando".equals(faseActual) ||
                        idPartidaActual == null ||
                        !idPartidaActual.equals(evento.child("idPartida").getValue(String.class))) return;
                Object valor = evento.child("semilla").getValue();
                if (!(valor instanceof Number) || listaNombresCategorias == null ||
                        listaNombresCategorias.size() < 2) return;
                RecyclerView.Adapter<?> base = rvCategoriasJuego.getAdapter();
                if (!(base instanceof CategoriaPartidaAdapter)) return;
                List<String> orden = VientoDelCaosOrden.generar(
                        listaNombresCategorias, ((Number) valor).longValue());
                ((CategoriaPartidaAdapter) base).reordenarPorNombres(orden);
                // Cada participante ve el anuncio al recibir el evento compartido.
                // Una marca local por uso evita repetirlo al recrear la pantalla.
                String idUso = evento.child("idUso").getValue(String.class);
                if (idUso != null && !idUso.isEmpty() && getContext() != null) {
                    String clave = idSala + ":" + idPartidaActual + ":" + idUso;
                    android.content.SharedPreferences avisos = requireContext()
                            .getSharedPreferences("vientoCaosAvisos", android.content.Context.MODE_PRIVATE);
                    if (!avisos.getBoolean(clave, false)) {
                        avisos.edit().putBoolean(clave, true).apply();
                        String nombre = evento.child("nombreJugador").getValue(String.class);
                        if (nombre == null || nombre.trim().isEmpty()) nombre = "Un jugador";
                        NotificationHelper.showGlobal("¡" + nombre + " desató Viento del Caos! "
                                + "Todas las categorías cambiaron de lugar.", NotificationHelper.INFO);
                        Sounds.playNotificaciones();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.w("VIENTO", "No se pudo recibir el orden visual", error.toException());
            }
        });
    }

    private void limpiarListenerVientoDelCaos() {
        if (nodoVientoDelCaos != null && listenerVientoDelCaos != null) {
            nodoVientoDelCaos.removeEventListener(listenerVientoDelCaos);
        }
        nodoVientoDelCaos = null;
        listenerVientoDelCaos = null;
    }

    /** Un evento por objetivo: todos ven el anuncio, solo la victima ve sus campos reflejados. */
    // Escudo Dorado: equipamiento secreto; solo PARRY se anuncia a todos.
    private DatabaseReference nodoDefensas;
    private ValueEventListener listenerDefensas;
    private boolean escudoEquipandose = false;

    private void activarEscudoDorado(String nombre) {
        if (escudoEquipandose || !puedeUsarComodinAhora() || bastaActivado) return;
        escudoEquipandose = true;
        new com.example.basta_game.comodines.ComodinEscudoDorado(idSala, idPartidaActual, rondaActual)
                .activar(nombre, new BaseComodin.ComodinCallback() {
                    @Override public void onSuccess(String aviso) {
                        escudoEquipandose = false;
                        comodinesUsadosEstaRonda.add("escudo_dorado");
                        if (isAdded()) NotificationHelper.show(notificationPartida,
                                "¡Escudo Dorado equipado! Te protege hasta acabar la revisión de esta ronda",
                                NotificationHelper.SUCCESS);
                    }
                    @Override public void onError(String mensaje) {
                        escudoEquipandose = false;
                        if (isAdded()) NotificationHelper.show(notificationPartida, mensaje, NotificationHelper.WARNING);
                    }
                });
    }

    private void escucharDefensas() {
        limpiarListenerDefensas();
        if (salaRef == null || idPartidaActual == null || rondaActual <= 0) return;
        final int rondaEscuchada = rondaActual;
        final String partidaEscuchada = idPartidaActual;
        nodoDefensas = salaRef.child("poderesRonda").child("ronda_" + rondaEscuchada);
        listenerDefensas = nodoDefensas.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot datos) {
                if (!isAdded() || getView() == null || rondaActual != rondaEscuchada ||
                        !partidaEscuchada.equals(idPartidaActual) ||
                        !partidaEscuchada.equals(datos.child("idPartida").getValue(String.class))) return;
                android.content.SharedPreferences avisos = requireContext().getSharedPreferences(
                        "escudosAvisos", android.content.Context.MODE_PRIVATE);
                // Escudo oculto: nadie recibe aviso cuando un rival se equipa.
                // Solo el propietario ve su confirmacion local.
                for (DataSnapshot bloqueo : datos.child("bloqueos").getChildren()) {
                    String uso = bloqueo.getKey();
                    if (uso == null) continue;
                    String clave = idSala + ":" + partidaEscuchada + ":parry:" + uso;
                    if (avisos.getBoolean(clave, false)) continue;
                    avisos.edit().putBoolean(clave, true).apply();
                    String a = bloqueo.child("nombreAtacante").getValue(String.class);
                    String v = bloqueo.child("nombreVictima").getValue(String.class);
                    String mensaje = (v == null ? "Un jugador" : v) +
                            " se defendió correctamente de " + (a == null ? "un rival" : a);
                    Sounds.playParry();
                    animarParry(mensaje);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.w("ESCUDO_DORADO", "No se pudo escuchar la defensa", error.toException());
            }
        });
    }

    private void animarParry(String mensaje) {
        // Se mantiene la tarjeta y animacion PARRY originales. Solo cambia
        // la ventana en la que se dibuja si hay un dialogo sobre la partida.
        View raiz = getView();
        Dialog dialogoVisible = null;
        if (dialogPuntuacionesActual != null && dialogPuntuacionesActual.isShowing()) {
            dialogoVisible = dialogPuntuacionesActual;
        } else if (dialogValidacionActual != null && dialogValidacionActual.isShowing()) {
            dialogoVisible = dialogValidacionActual;
        }
        if (dialogoVisible != null && dialogoVisible.getWindow() != null) {
            raiz = dialogoVisible.getWindow().getDecorView();
        }
        if (!(raiz instanceof ViewGroup)) return;
        final View destino = raiz;
        // ViewOverlay no captura el teclado ni intercepta botones.
        destino.post(() -> {
            if (!isAdded() || !destino.isAttachedToWindow() || destino.getWidth() < 1) return;
            LinearLayout tarjeta = new LinearLayout(requireContext());
            tarjeta.setOrientation(LinearLayout.VERTICAL);
            tarjeta.setGravity(Gravity.CENTER);
            tarjeta.setPadding(dp(20), dp(18), dp(20), dp(20));
            // Papel crema y borde violeta: misma familia de color que los dialogs de Basta.
            android.graphics.drawable.GradientDrawable fondo = new android.graphics.drawable.GradientDrawable();
            fondo.setColor(Color.parseColor("#FFFBF5"));
            fondo.setCornerRadius(dp(24));
            fondo.setStroke(dp(3), Color.parseColor("#9463DF"));
            tarjeta.setBackground(fondo);
            tarjeta.setElevation(dp(12));
            TextView decoracion = new TextView(requireContext());
            decoracion.setText("✦  🛡️  ✦");
            decoracion.setTextColor(Color.parseColor("#9463DF"));
            decoracion.setTextSize(19f);
            decoracion.setGravity(Gravity.CENTER);
            tarjeta.addView(decoracion);
            TextView titulo = new TextView(requireContext());
            titulo.setText("¡PARRY!");
            titulo.setTextColor(Color.parseColor("#6749B9"));
            titulo.setTextSize(32f);
            titulo.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(
                    requireContext(), R.font.lilitaone_regular));
            titulo.setGravity(Gravity.CENTER);
            TextView subtitulo = new TextView(requireContext());
            subtitulo.setText(mensaje);
            subtitulo.setTextColor(Color.parseColor("#26304A"));
            subtitulo.setTextSize(16f);
            subtitulo.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(
                    requireContext(), R.font.manrope_regular));
            subtitulo.setPadding(dp(5), dp(5), dp(5), 0);
            subtitulo.setGravity(Gravity.CENTER);
            tarjeta.addView(titulo);
            tarjeta.addView(subtitulo);
            int ancho = Math.max(dp(140), destino.getWidth() - dp(44));
            tarjeta.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int alto = tarjeta.getMeasuredHeight();
            int x = (destino.getWidth() - ancho) / 2;
            int y = Math.max(dp(50), (destino.getHeight() - alto) / 2);
            tarjeta.layout(x, y, x + ancho, y + alto);
            ((ViewGroup) destino).getOverlay().add(tarjeta);
            tarjeta.setScaleX(.8f); tarjeta.setScaleY(.8f); tarjeta.setAlpha(0f);
            tarjeta.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280)
                    .withEndAction(() -> tarjeta.animate().alpha(0f).translationY(-dp(20))
                            .setStartDelay(3000).setDuration(400)
                            .withEndAction(() -> ((ViewGroup) destino).getOverlay().remove(tarjeta)).start()).start();
        });
    }

    private void limpiarListenerDefensas() {
        if (nodoDefensas != null && listenerDefensas != null) nodoDefensas.removeEventListener(listenerDefensas);
        nodoDefensas = null;
        listenerDefensas = null;
    }

    private void escucharDecreto() {
        limpiarListenerDecreto();
        if (salaRef == null || rondaActual <= 0) return;
        final int rondaEscuchada=rondaActual;
        nodoDecreto=salaRef.child("decretoSilencio").child("ronda_"+rondaEscuchada);
        listenerDecreto=nodoDecreto.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot evento) {
                if(!isAdded() || getView()==null || auth==null || auth.getCurrentUser()==null ||
                        idPartidaActual==null || rondaEscuchada!=rondaActual) return;
                categoriasDecretoActual.clear();
                vocalDecretoActual=null;
                String uso=evento.child("idUso").getValue(String.class);
                if(idPartidaActual.equals(evento.child("idPartida").getValue(String.class)) && uso!=null) {
                    vocalDecretoActual=evento.child("vocal").getValue(String.class);
                    for(DataSnapshot item:evento.child("categorias").getChildren()) {
                        String nombre=item.getValue(String.class);
                        if(nombre!=null) categoriasDecretoActual.add(nombre);
                    }
                    String uid=auth.getCurrentUser().getUid();
                    android.content.SharedPreferences avisos=requireContext().getSharedPreferences(
                            "decretoAvisos",android.content.Context.MODE_PRIVATE);
                    String clave=idSala+":"+idPartidaActual+":"+uso;
                    if(!avisos.getBoolean(clave,false)) {
                        avisos.edit().putBoolean(clave,true).apply();
                        NotificationHelper.showGlobal("¡DECRETO DEL SILENCIO! Vocal "+vocalDecretoActual+
                                " prohibida en "+android.text.TextUtils.join(" y ",categoriasDecretoActual)+
                                ". Usarla invalida tu respuesta y resta 5 puntos.",NotificationHelper.WARNING);
                        Sounds.playNotificaciones();
                    }
                    // El jugador confirma haber recibido el aviso. Sin confirmacion
                    // no hay sanciones retroactivas por problemas de conexion.
                    if(!Boolean.TRUE.equals(evento.child("recibido").child(uid).getValue(Boolean.class)))
                        nodoDecreto.child("recibido").child(uid).setValue(true);
                }
                if (txtDecretoResumenPartida != null) {
                    boolean activa=vocalDecretoActual!=null && !categoriasDecretoActual.isEmpty();
                    txtDecretoResumenPartida.setVisibility(activa ? View.VISIBLE : View.GONE);
                    if (activa) {
                        // Orden de la sala, no del HashSet; el banner refleja las dos.
                        java.util.List<String> ordenadas=new java.util.ArrayList<>();
                        if (listaNombresCategorias!=null) for(String c:listaNombresCategorias)
                            if(categoriasDecretoActual.contains(c)) ordenadas.add(c);
                        for(String c:categoriasDecretoActual)
                            if(!ordenadas.contains(c)) ordenadas.add(c);
                        txtDecretoResumenPartida.setText("🚫 DECRETO: Vocal " + vocalDecretoActual +
                                " prohibida en " + android.text.TextUtils.join(" y ",ordenadas));
                    }
                }
                if(rvCategoriasJuego!=null && rvCategoriasJuego.getAdapter() instanceof CategoriaPartidaAdapter)
                    ((CategoriaPartidaAdapter)rvCategoriasJuego.getAdapter()).mostrarDecreto(
                            categoriasDecretoActual,vocalDecretoActual);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.w("DECRETO","No se pudo escuchar el Decreto",error.toException());
            }
        });
    }

    private void limpiarListenerDecreto() {
        if(nodoDecreto!=null && listenerDecreto!=null) nodoDecreto.removeEventListener(listenerDecreto);
        nodoDecreto=null;
        listenerDecreto=null;
        categoriasDecretoActual.clear();
        vocalDecretoActual=null;
        if (txtDecretoResumenPartida != null) txtDecretoResumenPartida.setVisibility(View.GONE);
    }

    private void escucharEspejo() {
        limpiarListenerEspejo();
        if (salaRef == null || rondaActual <= 0) return;
        nodoEspejo = salaRef.child("efectosVisuales").child("espejo").child("ronda_" + rondaActual);
        listenerEspejo = nodoEspejo.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot datos) {
                if (!isAdded() || getView() == null || !"jugando".equals(faseActual) ||
                        idPartidaActual == null || auth == null || auth.getCurrentUser() == null) return;
                String miUso = null;
                long finPropio = 0L;
                long ahora = com.example.basta_game.utils.ConexionJuego.ahoraServidor();
                for (DataSnapshot evento : datos.getChildren()) {
                    if (!idPartidaActual.equals(evento.child("idPartida").getValue(String.class))) continue;
                    String idUso = evento.child("idUso").getValue(String.class);
                    String victimaUid = evento.child("victimaUid").getValue(String.class);
                    String victimaNombre = evento.child("victimaNombre").getValue(String.class);
                    String atacanteNombre = evento.child("atacanteNombre").getValue(String.class);
                    Object fin = evento.child("finMs").getValue();
                    if (!(fin instanceof Number) || ((Number)fin).longValue() <= ahora) continue;
                    if (auth.getCurrentUser().getUid().equals(victimaUid)) {
                        miUso = idUso;
                        finPropio = ((Number)fin).longValue();
                    }
                    if (idUso != null && !idUso.isEmpty()) {
                        android.content.SharedPreferences avisos = requireContext().getSharedPreferences(
                                "espejoAvisos", android.content.Context.MODE_PRIVATE);
                        String clave = idSala + ":" + idPartidaActual + ":" + idUso;
                        if (!avisos.getBoolean(clave, false)) {
                            avisos.edit().putBoolean(clave, true).apply();
                            NotificationHelper.showGlobal("¡" + (atacanteNombre == null ? "Alguien" : atacanteNombre) +
                                    " usó El Espejo contra " + (victimaNombre == null ? "un rival" : victimaNombre) +
                                    "! Hasta 3 categorías aleatorias quedarán invertidas.", NotificationHelper.INFO);
                            Sounds.playNotificaciones();
                        }
                    }
                }
                if (rvCategoriasJuego != null && rvCategoriasJuego.getAdapter() instanceof CategoriaPartidaAdapter) {
                    CategoriaPartidaAdapter adaptador = (CategoriaPartidaAdapter) rvCategoriasJuego.getAdapter();
                    if (miUso != null) {
                        espejoActivoLocal = adaptador.iniciarEspejo(miUso, finPropio);
                        // BASTA ofrece 10 s finales de correccion: corta el efecto
                        // pero NO restaura las palabras ya invertidas.
                        if (bastaActivado) aplicarEspejoLocal(false);
                    } else if (espejoActivoLocal) {
                        adaptador.finalizarEspejo();
                        espejoActivoLocal = false;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.w("ESPEJO", "No se pudo recibir el evento", error.toException());
            }
        });
    }

    private void aplicarEspejoLocal(boolean activo) {
        if (!activo && rvCategoriasJuego != null &&
                rvCategoriasJuego.getAdapter() instanceof CategoriaPartidaAdapter) {
            ((CategoriaPartidaAdapter)rvCategoriasJuego.getAdapter()).finalizarEspejo();
        }
        espejoActivoLocal = activo;
    }

    private void limpiarListenerEspejo() {
        if (nodoEspejo != null && listenerEspejo != null) nodoEspejo.removeEventListener(listenerEspejo);
        nodoEspejo = null;
        listenerEspejo = null;
        if (finEspejoPendiente != null) handlerEspejo.removeCallbacks(finEspejoPendiente);
        finEspejoPendiente = null;
        cerrarSelectorEspejo();
        aplicarEspejoLocal(false);
    }

    private void cerrarSelectorEspejo() {
        if (dialogSeleccionEspejo != null) {
            if (dialogSeleccionEspejo.isShowing()) dialogSeleccionEspejo.dismiss();
            dialogSeleccionEspejo = null;
        }
    }

    private void mostrarDialogoSeleccionEspejo(String miNombre) {
        if (!isAdded() || auth.getCurrentUser() == null || !esFaseJugando() || bastaActivado) return;
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_seleccionar_victima);
        dialog.setCancelable(true);
        TextView titulo = dialog.findViewById(R.id.tvTituloVictima);
        TextView mensaje = dialog.findViewById(R.id.tvMensajeVictima);
        if (titulo != null) {titulo.setText("¡EL ESPEJO!"); titulo.setTextColor(Color.parseColor("#6749B9"));}
        if (mensaje != null) mensaje.setText("Elige a quién atacar. El Espejo afecta hasta 3 categorías al azar, ¡aunque estén vacías! Invierte lo que escriba durante 30 s o hasta BASTA. ¡El rival deberá corregirlo!");
        MaterialButton cancelar = dialog.findViewById(R.id.btnCancelarVictima);
        cancelar.setOnClickListener(v -> dialog.dismiss());
        String uid = auth.getCurrentUser().getUid();
        dialogSeleccionEspejo = dialog;
        dialog.setOnDismissListener(d -> { if (dialogSeleccionEspejo == dialog) dialogSeleccionEspejo = null; });
        salaRef.child("jugadores").get().addOnCompleteListener(lectura -> {
            if (!isAdded() || dialogSeleccionEspejo != dialog || !esFaseJugando() || bastaActivado ||
                    !lectura.isSuccessful() || lectura.getResult() == null) {
                cerrarSelectorEspejo();
                return;
            }
            List<VictimaAdapter.JugadorMini> rivales = new ArrayList<>();
            for (DataSnapshot jugador : lectura.getResult().getChildren()) {
                String otroUid = jugador.getKey();
                String otroNombre = jugador.child("nombre").getValue(String.class);
                if (otroUid != null && !uid.equals(otroUid) && otroNombre != null)
                    rivales.add(new VictimaAdapter.JugadorMini(otroUid, otroNombre));
            }
            RecyclerView lista = dialog.findViewById(R.id.rvVictimas);
            lista.setLayoutManager(new LinearLayoutManager(requireContext()));
            lista.setAdapter(new VictimaAdapter(rivales, victima -> {
                cerrarSelectorEspejo();
                if (espejoActivacionPendiente || bastaActivado || !esFaseJugando()) return;
                espejoActivacionPendiente = true;
                new ComodinEspejo(idSala, idPartidaActual, rondaActual, victima.uid, victima.nombre)
                        .activar(miNombre, new BaseComodin.ComodinCallback() {
                            @Override public void onSuccess(String texto) {
                                espejoActivacionPendiente = false;
                                comodinesUsadosEstaRonda.add("inversion_letras");
                                // El aviso a todos lo maneja el listener compartido.
                            }
                            @Override public void onError(String error) {
                                espejoActivacionPendiente = false;
                                if (isAdded()) NotificationHelper.show(notificationPartida,error,NotificationHelper.WARNING);
                            }
                        });
            }));
            if (!dialog.isShowing()) dialog.show();
            com.example.basta_game.utils.NotificationHelper.acompanar(dialog);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });
    }

    private void subirCampoRespuestaAlTeclado(View campoEnfocado) {
        if (campoEnfocado == null) return;
        campoEnfocado.post(() -> {
            if (!isAdded() || !campoEnfocado.hasFocus() || !campoEnfocado.isAttachedToWindow()) return;
            campoEnfocado.requestRectangleOnScreen(new android.graphics.Rect(0, -dp(12),
                    campoEnfocado.getWidth(), campoEnfocado.getHeight() + dp(12)), false);
        });
    }
    private void configurarBotones() {
        if (btnComodines != null) {
            // Le pasamos FALSE porque este botón se presiona DURANTE el juego, no al final.
            btnComodines.setOnClickListener(v -> mostrarDialogoComodines(false));
        }

        btnBasta.setOnClickListener(v -> {
            if (!PlazoRonda.bastaDisponible(inicioRespuestasActual,finDelPlazo,com.example.basta_game.utils.ConexionJuego.ahoraServidor()) || bastaActivado ||
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

            solicitudBastaPendiente=true;
            bloquearBotonBastaPorBasta();

            final String partida=idPartidaActual, uid=auth.getCurrentUser().getUid();
            final int ronda=rondaActual;
            FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot offset) {
                    if(!(offset.getValue() instanceof Number)) {solicitudBastaPendiente=false;desbloquearBotonBastaSiCorresponde();return;}
                    final long desfase=((Number)offset.getValue()).longValue();
                    salaRef.runTransaction(new Transaction.Handler() {
                        @Override public Transaction.Result doTransaction(MutableData sala) {
                            if(sala.getValue()==null) return Transaction.success(sala);
                            return PlazoRonda.aceptarBasta(sala,partida,ronda,uid,System.currentTimeMillis()+desfase)
                                    ? Transaction.success(sala) : Transaction.abort();
                        }
                        @Override public void onComplete(DatabaseError error,boolean committed,DataSnapshot sala) {
                            solicitudBastaPendiente=false;
                            if(!isAdded() || getView()==null) return;
                            if(error!=null) NotificationHelper.show(notificationPartida,"No se pudo confirmar BASTA. Reintenta al conectar",NotificationHelper.WARNING);
                            if(committed && sala!=null && Boolean.TRUE.equals(sala.child("bastaPresionado").getValue())) {
                                bastaActivado=true;bloquearBotonBastaPorBasta();setearCronometroGlobal();
                            } else desbloquearBotonBastaSiCorresponde();
                        }
                    },false);
                }
                @Override public void onCancelled(DatabaseError e) {solicitudBastaPendiente=false;desbloquearBotonBastaSiCorresponde();}
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

        final String miUid=auth.getCurrentUser().getUid(),partida=idPartidaActual;
        final int ronda=rondaActual;
        final long conexion=com.example.basta_game.utils.ConexionJuego.version();
        final SalaActivity actividad=(SalaActivity)requireActivity();
        salaRef.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData datos) {
                if(actividad.haSalidoDeSala() || !com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return Transaction.abort();
                if(datos.getValue()==null) return Transaction.success(datos);
                return InicioPartida.registrarEntrada(datos,partida,ronda,miUid) ? Transaction.success(datos) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error,boolean ok,DataSnapshot datos) {
                if(error!=null) Log.w("ENTRADA_PARTIDA","No se pudo confirmar la entrada",error.toException());
            }
        },false);
    }

    private void bloquearBotonBastaPorBasta() {
        if (btnBasta == null) {
            return;
        }

        btnBasta.setEnabled(false);
        btnBasta.setClickable(false);
        btnBasta.setAlpha(0.45f);
        btnBasta.setText("FINALIZANDO");
        btnBasta.setTextSize(17);
    }

    private void desbloquearBotonBastaSiCorresponde() {
        if (btnBasta == null) {
            return;
        }

        if (solicitudBastaPendiente || bastaActivado || bastaBloqueadoPorComodin || respuestasGuardadas || modalValidacionMostrado || cierreRondaSolicitado) {
            return;
        }

        boolean habilitado=PlazoRonda.bastaDisponible(inicioRespuestasActual,finDelPlazo,com.example.basta_game.utils.ConexionJuego.ahoraServidor());
        if(btnBasta.isEnabled()==habilitado && "¡BASTA!".contentEquals(btnBasta.getText()))return;
        btnBasta.setEnabled(habilitado);
        btnBasta.setClickable(habilitado);
        btnBasta.setAlpha(habilitado ? 1f : 0.45f);
        btnBasta.setText("¡BASTA!");btnBasta.setTextSize(26);
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
        return rondaActual > 0 && ("jugando".equalsIgnoreCase(faseActual) ||
                "En juego".equalsIgnoreCase(faseActual) ||
                (faseActual == null && "En juego".equalsIgnoreCase(estadoPartidaActual)));
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
                    if (!"jugando".equals(fase)) {
                        cerrarSelectorEspejo();
                        aplicarEspejoLocal(false);
                    }
                    if (dialogComodinesActual != null) {
                        RecyclerView lista = dialogComodinesActual.findViewById(R.id.rvComodinesPartida);
                        if (lista != null && lista.getAdapter() != null) lista.getAdapter().notifyDataSetChanged();
                    }
                    if ("resultados_finales".equals(fase)) comprobarFinConUnJugador();
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
        vientoActivacionPendiente = false;
        espejoActivacionPendiente = false;
        decretoActivacionPendiente = false;
        categoriasDecretoActual.clear();
        vocalDecretoActual = null;
        escudoEquipandose = false;
        congeladorActivandose = false;
        cerrarSelectorEspejo();
        limpiarListenerVientoDelCaos();
        limpiarListenerEspejo();
        limpiarListenerDecreto();
        limpiarListenerDefensas();

        // Limpiamos el chisme global viejo para que no se repita en la siguiente ronda
        if (salaRef != null) {
            salaRef.child("eventoCaosGlobal").child("ronda_" + rondaActual).removeValue();
        }

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
        escucharVientoDelCaos();
        escucharEspejo();
        escucharDecreto();
        escucharDefensas();
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

        txtInstruccionLetra.setText("Empieza con " + letra);

        String nombreDrawable = "letra_" + letra.toLowerCase(Locale.ROOT);
        int idDrawable = getResources().getIdentifier(
                nombreDrawable,
                "drawable",
                requireContext().getPackageName()
        );

        TextView sinRecurso=getView().findViewById(R.id.txtLetraSinRecurso);
        sinRecurso.setText(letra);sinRecurso.setVisibility(idDrawable==0 ? View.VISIBLE : View.GONE);
        imgLetraActual.setVisibility(idDrawable==0 ? View.GONE : View.VISIBLE);
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
    public void recuperarEstadoConectado() {
        if(!isAdded() || getView()==null || yaVolvioAlLobby) return;
        if(coordinadorRevision!=null) coordinadorRevision.actualizar();
        if(!modalValidacionMostrado && dialogPuntuacionesActual==null && dialogResumenActual==null) {
            respuestasGuardadas=false;setearCronometroGlobal();
        }
    }    public void setearCronometroGlobal() {
        if (salaRef == null || !isAdded() || getView() == null) {
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

                if (!com.example.basta_game.utils.FaseJuego.permiteResponder(faseFirebase, estadoFirebase, rondaFirebase)) {
                    respuestasGuardadas = true;
                    btnBasta.setEnabled(false);
                    if ("resultados_finales".equals(faseFirebase) && snapshotSala.child("jugadores").getChildrenCount() == 1) {
                        comprobarFinConUnJugador();
                        return;
                    }
                    if (("revision_puntuaciones".equals(faseFirebase) || "resultados_finales".equals(faseFirebase)) &&
                            dialogPuntuacionesActual == null && dialogResumenActual == null && !cierrePorAbandonoMostrado) {
                        rondaActual = rondaFirebase;
                        modalValidacionMostrado = true;
                        mostrarResumenComodines(rondaFirebase, "resultados_finales".equals(faseFirebase));
                    }
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
                final int duracionRonda = tiempoSeconds;
                final long inicioRespuestas=PlazoRonda.inicioRespuestas(inicioRonda,rondaFirebase,leerLong(snapshotSala.child("preparacionInicialMs"),0));

                inicioRespuestasActual=inicioRespuestas;
                FirebaseDatabase.getInstance()
                        .getReference(".info/serverTimeOffset")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot offsetSnapshot) {
                                if (!rondaSigueSiendoActual(rondaDelCronometro) || yaVolvioAlLobby) {
                                    return;
                                }

                                Long offset = offsetSnapshot.getValue(Long.class);

                                if (offset == null) {
                                    offset = 0L;
                                }

                                Long bastaTimestamp = snapshotSala.child("bastaTimeStamp").getValue(Long.class);
                                long fin = PlazoRonda.calcularFin(inicioRespuestas, duracionRonda, bastaTimestamp);
                                iniciarCronometro(fin, rondaDelCronometro, offset,bastaTimestamp);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.d("TIEMPO", "No se pudo leer offset. Conservando plazo con hora local.");
                                Long bastaTimestamp = snapshotSala.child("bastaTimeStamp").getValue(Long.class);
                                iniciarCronometro(PlazoRonda.calcularFin(inicioRespuestas, duracionRonda, bastaTimestamp),
                                        rondaDelCronometro, 0L,bastaTimestamp);
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
        if(salaRef==null || !VerificacionesAuth.VerificarInicioSesion(auth)) return;
        final String uid=auth.getCurrentUser().getUid(), partida=idPartidaActual;
        salaRef.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData datos) {
                if(datos.getValue()==null) return Transaction.success(datos);
                return InicioPartida.confirmarInicio(datos,partida,rondaParaIniciar,uid)
                        ? Transaction.success(datos) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean committed,DataSnapshot datos) {
                if(e!=null) Log.w("TIEMPO","No se pudo confirmar el plazo compartido",e.toException());
            }
        },false);
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

    private Handler handlerCronometro;
    private Runnable runnableCronometro;

    private int rondaDelPlazo = -1;
    private long finDelPlazo;
    private Long marcaBastaDelPlazo;

    private void iniciarCronometro(long finCompartido, int rondaDelCronometro, long offsetServidor,Long marcaBasta) {
        if (!rondaSigueSiendoActual(rondaDelCronometro) || getView() == null || yaVolvioAlLobby || respuestasGuardadas) {
            return;
        }

        // Una lectura anterior a BASTA nunca debe alargar el plazo ya recibido.
        if (rondaDelPlazo == rondaDelCronometro) {
            if(!PlazoRonda.lecturaAplicable(marcaBastaDelPlazo,marcaBasta)) return;
        }
        marcaBastaDelPlazo=marcaBasta;
        rondaDelPlazo = rondaDelCronometro;
        finDelPlazo = finCompartido;
        final long finRealPartida = finCompartido;

        if (cronometroRonda != null) {
            cronometroRonda.cancel();
            cronometroRonda = null;
        }
        if (handlerCronometro != null && runnableCronometro != null) {
            handlerCronometro.removeCallbacks(runnableCronometro);
        }

        final int versionActual = ++versionCronometro;
        txtRondaActual.setText("Ronda " + rondaActual + "/" + rondasMaximas);
        handlerCronometro = new Handler(Looper.getMainLooper());
        final long[] lastSegTotal = {-1};
        runnableCronometro = new Runnable() {
            @Override
            public void run() {
                if (!rondaSigueSiendoActual(rondaDelCronometro) || getView() == null || yaVolvioAlLobby ||
                        versionActual != versionCronometro) return;

                desbloquearBotonBastaSiCorresponde();
                long restanteMs = PlazoRonda.restante(finRealPartida, System.currentTimeMillis(), offsetServidor);
                if (restanteMs <= 0) {
                    txtTiempoRestante.setText("00:00");
                    guardarRespuestasJugador();
                    return;
                }
                long segTotal = (long) Math.ceil(restanteMs / 1000.0);
                if (segTotal != lastSegTotal[0]) {
                    lastSegTotal[0] = segTotal;
                    txtTiempoRestante.setText(formatearTiempo(segTotal));
                    if (auth.getCurrentUser() != null) com.example.basta_game.utils.VibracionRonda.avisar(
                            requireContext(), auth.getCurrentUser().getUid(), idPartidaActual, rondaDelCronometro, restanteMs);
                    if (segTotal == 5 && !sonidoFinalReproducido) {
                        sonidoFinalReproducido = true;
                        Sounds.conteoFinalPlay();
                    }
                }
                handlerCronometro.postDelayed(this, 50);
            }
        };
        handlerCronometro.post(runnableCronometro);
    }

    @Override
    public void onDestroyView() {
        if(listenerPuntuacionesEnVivo!=null && salaRef!=null) salaRef.removeEventListener(listenerPuntuacionesEnVivo);
        listenerPuntuacionesEnVivo=null;
        handlerCierreRespuestas.removeCallbacksAndMessages(null);
        if (listenerCierreRespuestas != null) salaRef.removeEventListener(listenerCierreRespuestas);
        listenerCierreRespuestas = null;
        estadoCierreRespuestas = null;
        if(dialogRuletaActual!=null){dialogRuletaActual.dismiss();dialogRuletaActual=null;}
        handlerAnuncio.removeCallbacksAndMessages(null);
        if (dialogResumenActual != null) { dialogResumenActual.dismiss(); dialogResumenActual = null; }
        super.onDestroyView();
        versionCronometro++;
        rondaDelPlazo = -1;
        handlerInicioRonda.removeCallbacksAndMessages(null);
        if (handlerCronometro != null && runnableCronometro != null) {
            handlerCronometro.removeCallbacks(runnableCronometro);
        }

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
        limpiarListenerVientoDelCaos();
        limpiarListenerEspejo();
        limpiarListenerDecreto();
        limpiarListenerDefensas();

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

                boolean cambioParticipantes = cantidadJugadoresEnSala != jugadoresActivos.size();
                cantidadJugadoresEnSala = jugadoresActivos.size();
                if (cambioParticipantes) retomarResponsabilidades();
                if (cantidadJugadoresEnSala == 1) comprobarFinConUnJugador();

                JugadorPartidaAdapter adaptador = new JugadorPartidaAdapter(jugadoresActivos);
                rvJugadoresPartida.setAdapter(adaptador);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }


    private void cambiarEstadoUsuarios() {
        marcarEntradaPartidaActual();
    }

    private void guardarRespuestasJugador() {
        if(!com.example.basta_game.utils.ConexionJuego.disponible()) return;
        if (!esFaseJugando()) {
            Log.d("RESPUESTAS", "No se guardan respuestas porque la fase actual no es jugando: " + faseActual);
            return;
        }

        if (respuestasGuardadas) {
            return;
        }

        // Si la ronda termina durante la confusion, consolidar la inversion
        // antes de copiar las respuestas a Firebase.
        aplicarEspejoLocal(false);
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

        final String partida=idPartidaActual;final long conexion=com.example.basta_game.utils.ConexionJuego.version();
        salaRef.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData s) {
                if(!com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return Transaction.abort();
                if(s.getValue()==null) return Transaction.success(s);
                return InicioPartida.enviarRespuestas(s,partida,rondaGuardada,uid,respuestasFinales) ? Transaction.success(s) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean ok,DataSnapshot s) {
                if(!isAdded() || !rondaSigueSiendoActual(rondaGuardada)) return;
                if(e==null) handlerCierreRespuestas.post(reintentarCierreRespuestas);
                else respuestasGuardadas=false;
            }
        },false);
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

        final String partida = idPartidaActual;
        final String uid = auth.getCurrentUser().getUid();
        salaRef.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData datos) {
                if(datos.getValue()==null) return Transaction.success(datos);
                return com.example.basta_game.utils.RelojRevision.guardarIniciales(
                        datos,partida,rondaDeEstaRevision,uid,validacionesJugador)
                        ? Transaction.success(datos) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error,boolean committed,DataSnapshot datos) {
                if(error!=null) Log.w("VALIDACION","No se guardaron validaciones iniciales",error.toException());
            }
        },false);
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
        return com.example.basta_game.utils.ValidacionBasica.correcta(letra,respuesta);
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


    private final Handler handlerCierreRespuestas = new Handler(Looper.getMainLooper());
    private final Runnable reintentarCierreRespuestas = this::recuperarCierreRespuestas;
    private ValueEventListener listenerCierreRespuestas;
    private DataSnapshot estadoCierreRespuestas;
    private boolean publicandoCierreRespuestas;

    private void escucharCierreRespuestas() {
        listenerCierreRespuestas = salaRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot sala) {
                estadoCierreRespuestas = sala;
                handlerCierreRespuestas.removeCallbacks(reintentarCierreRespuestas);
                handlerCierreRespuestas.post(reintentarCierreRespuestas);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                estadoCierreRespuestas = null;
                Log.w("CIERRE_RESPUESTAS", "No se pudo observar el cierre compartido", error.toException());
            }
        });
    }

    private void recuperarCierreRespuestas() {
        handlerCierreRespuestas.removeCallbacks(reintentarCierreRespuestas);
        DataSnapshot sala = estadoCierreRespuestas;
        if (!isAdded() || getView() == null || requireActivity().isFinishing() ||
                ((SalaActivity)requireActivity()).haSalidoDeSala() || yaVolvioAlLobby || sala == null || auth.getCurrentUser() == null ||
                idPartidaActual == null || !idPartidaActual.equals(sala.child("idPartida").getValue(String.class)) ||
                leerEntero(sala.child("rondaActual"), -1) != rondaActual ||
                !"jugando".equals(sala.child("fase").getValue(String.class))) return;
        // Replay the durable acknowledgement, independent of this client's answer callback.
        if (sala.child(com.example.basta_game.utils.RelojRevision.ruta(rondaActual) + "/reloj").exists()) {
            respuestasGuardadas = true;
            btnBasta.setEnabled(false);
            versionCronometro++;
            if (!modalValidacionMostrado) {
                modalValidacionMostrado = true;
                cargarRespuestasValidacionYMostrarModal(rondaActual);
            }
            if (dialogValidacionActual == null) handlerCierreRespuestas.postDelayed(reintentarCierreRespuestas, 1000);
            return;
        }
        Long inicio = sala.child("inicioRonda").getValue(Long.class);
        if (inicio == null || !com.example.basta_game.utils.ConexionJuego.disponible()) {
            handlerCierreRespuestas.postDelayed(reintentarCierreRespuestas, 1000);
            return;
        }
        long preparacion = com.example.basta_game.utils.RelojRevision.numero(sala.child("preparacionInicialMs").getValue(), 0);
        long fin = PlazoRonda.calcularFin(PlazoRonda.inicioRespuestas(inicio, rondaActual, preparacion),
                leerEntero(sala.child("tiempoRonda"), tiempoSeconds), sala.child("bastaTimeStamp").getValue(Long.class));
        long ahora = com.example.basta_game.utils.ConexionJuego.ahoraServidor();
        handlerCierreRespuestas.postDelayed(reintentarCierreRespuestas, Math.max(500, fin - ahora));
        if (ahora < fin) return;
        guardarRespuestasJugador();
        if (publicandoCierreRespuestas) return;
        publicandoCierreRespuestas = true;
        final String partida = idPartidaActual, uid = auth.getCurrentUser().getUid();
        final int ronda = rondaActual;
        final long conexion = com.example.basta_game.utils.ConexionJuego.version();
        salaRef.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData datos) {
                if (!com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return Transaction.abort();
                if (datos.getValue() == null) return Transaction.success(datos);
                return com.example.basta_game.utils.CierreRespuestas.aplicar(datos, partida, ronda, uid,
                        com.example.basta_game.utils.ConexionJuego.ahoraServidor()) ? Transaction.success(datos) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot datos) {
                publicandoCierreRespuestas = false;
                if (error != null) {
                    Log.w("CIERRE_RESPUESTAS", "Cierre pendiente de sincronizacion", error.toException());
                    handlerCierreRespuestas.removeCallbacks(reintentarCierreRespuestas);
                    if (isAdded() && getView() != null) handlerCierreRespuestas.postDelayed(reintentarCierreRespuestas, 3000);
                }
            }
        }, false);
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
                                    reintentarAperturaRevision();
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
                                reintentarAperturaRevision();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { reintentarAperturaRevision(); }
        });
    }
    private void reintentarAperturaRevision() {
        modalValidacionMostrado=false;
        handlerCierreRespuestas.removeCallbacks(reintentarCierreRespuestas);
        if(isAdded() && getView()!=null) handlerCierreRespuestas.postDelayed(reintentarCierreRespuestas,3000);
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
                if (snapshot.getValue() != null) {
                    // Al pulsarse BASTA finaliza la confusion, conservando
                    // las palabras invertidas para corregirlas en los 10 s finales.
                    cerrarSelectorEspejo();
                    aplicarEspejoLocal(false);
                }
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

                setearCronometroGlobal();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("BASTA", "Error escuchando bastaTimeStamp: " + error.getMessage());
            }
        });
    }
    private void mostrarDialogoComodines(boolean esFaseFinal) {
        if (!isAdded() || !VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        // Si estamos en la tabla de puntuaciones final, abrimos directo el diálogo saltando validaciones de juego.
        if (esFaseFinal) {
            if (dialogPuntuacionesActual == null || !dialogPuntuacionesActual.isShowing()) return;
            MaterialButton continuar = dialogPuntuacionesActual.findViewById(R.id.btnVolverInicio);
            if (continuar != null && Boolean.TRUE.equals(continuar.getTag())) {
                NotificationHelper.show(notificationPartida,
                        "Ya estás listo para la siguiente ronda", NotificationHelper.WARNING);
                return;
            }
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
        View btnCerrar = vista.findViewById(R.id.btnCerrarComodinesPartida);

        List<ComodinPartida> comodinesInventario = new ArrayList<>();

        ComodinPartidaAdapter adapterComodines = new ComodinPartidaAdapter(
                comodinesInventario,
                comodin -> ejecutarComodinDesdeDialogo(comodin, dialog),
                comodin -> com.example.basta_game.utils.FaseComodin.disponible(comodin.getId(), faseActual, rondaActual, rondasMaximas)
                        && (com.example.basta_game.utils.FaseComodin.deResultados(comodin.getId())
                        || (!respuestasGuardadas && !modalValidacionMostrado && !cierreRondaSolicitado))
        );

        rvComodines.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComodines.setAdapter(adapterComodines);

        vista.findViewById(R.id.estadoVacioComodines).setVisibility(View.VISIBLE);
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

                        comodinesInventario.addAll(com.example.basta_game.utils.InventarioComodines.listar(
                                snapshot.getValue(), obtenerCatalogoComodinesPartida(), R.drawable.ic_comodines));

                        comodinesInventario.sort(java.util.Comparator.comparing(c -> com.example.basta_game.utils.FaseComodin.deResultados(c.getId())));
                        adapterComodines.notifyDataSetChanged();

                        if (comodinesInventario.isEmpty()) {
                            vista.findViewById(R.id.estadoVacioComodines).setVisibility(View.VISIBLE);
                            if (esFaseFinal) {
                                txtComodinesVacios.setText("¡Tu mochila está vacía! Consigue comodines en la tienda.");
                            } else {
                                txtComodinesVacios.setText("¡Tu mochila está vacía! Consigue comodines en la tienda.");
                            }
                        } else {
                            vista.findViewById(R.id.estadoVacioComodines).setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        vista.findViewById(R.id.estadoVacioComodines).setVisibility(View.VISIBLE);
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
        com.example.basta_game.utils.NotificationHelper.acompanar(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
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
                "Actívalo para detener ataques dirigidos hasta terminar la revisión. ¡PARRY! Ambos gastan su comodín.",
                "Premium",
                R.drawable.ic_comodin_escudo_dorado,
                0,
                true,
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

        catalogo.add(new ComodinPartida("decreto_silencio", "Decreto del Silencio", "Prohíbe una vocal en dos categorías aleatorias. Las palabras que la incluyan valen 0 y reciben multa de 5 puntos.", "Control", R.drawable.ic_comodin_decreto_silencio, 0, true, false));
        catalogo.add(new ComodinPartida("inversion_letras", "El Espejo", "Ataca hasta 3 categorías aleatorias, vacías o escritas, durante 30 s o hasta BASTA. ¡Las palabras quedan invertidas hasta que las corrijan!", "Control", R.drawable.ic_comodin_espejo, 0, true, false));
        catalogo.add(new ComodinPartida("oraculo_ciego", "Oráculo Ciego", "Apuesta a que una de tus categorías será única. Si aciertas ganas más, si fallas te maldicen.", "Riesgo", R.drawable.ic_comodin_oraculo_ciego, 0, false, false));
        catalogo.add(new ComodinPartida("impuesto_revol", "Impuesto Revolucionario", "Roba 5 puntos temporales a un rival. Si al final le ganas, te los quedas.", "Riesgo", R.drawable.ic_comodin_impuesto, 0, false, false));
        catalogo.add(new ComodinPartida("viento_caos", "Viento del Caos", "Desordena las categorías visibles de todos los jugadores durante la ronda, sin mezclar respuestas.", "Caótico", R.drawable.ic_comodin_viento, 0, true, false));
        catalogo.add(new ComodinPartida("chismoso_suerte", "Chismoso de la Suerte", "Te marca con amarillo si alguien escribió tu misma palabra, pero te congela 5s.", "Premium", R.drawable.ic_comodin_chismoso, 0, false, true));
        catalogo.add(new ComodinPartida("cambio_flujo", "Cambio de Flujo", "Intercambia tu puntaje con un jugador elegido aleatoriamente en la mesa.", "Caótico", R.drawable.ic_comodin_cambiodeflujo, 0, true, false));
        return catalogo;
    }

    private void ejecutarComodinDesdeDialogo(ComodinPartida comodin, Dialog dialog) {
        if (comodin == null) return;
        if (!com.example.basta_game.utils.FaseComodin.disponible(comodin.getId(), faseActual, rondaActual, rondasMaximas)) {
            mostrarAlertaCooldown("Este comodin no esta disponible en la fase actual");
            return;
        }

        if (!com.example.basta_game.utils.FaseComodin.deResultados(comodin.getId()) && !puedeUsarComodinAhora()) return;
        if (!comodin.isImplementado()) {
            NotificationHelper.show(notificationPartida, comodin.getNombre() + " estará disponible pronto", NotificationHelper.INFO);
            Sounds.playNotificaciones();
            return;
        }

        // 👇 NUEVO: VERIFICACIÓN DE COOLDOWN PARA COMODINES FINALES 👇
        if (comodin.getId().equals("marca_de_la_muerte") ||
                com.example.basta_game.utils.AliasCambioFlujo.es(comodin.getId()) ||
                comodin.getId().equals("ruleta_caotica") ||
                comodin.getId().equals("viento_caos") ||
                comodin.getId().equals("inversion_letras") ||
                comodin.getId().equals("decreto_silencio") ||
                comodin.getId().equals("escudo_dorado")) {

            if (comodinesUsadosEstaRonda.contains(com.example.basta_game.utils.AliasCambioFlujo.canonico(comodin.getId()))) {

                // Disparamos la alerta. ¡Garantizado que aparece encima del inventario!
                mostrarAlertaCooldown("Ya usaste este comodín en esta ronda. ¡Espera a la siguiente!");

                return; // Rebotamos la acción para no gastar el comodín ni cerrar el inventario
            }
        }
        // 👆 FIN DE LO NUEVO 👆

        if ("escudo_dorado".equals(comodin.getId()) && escudoEquipandose) {
            mostrarAlertaCooldown("Ya estás equipando Escudo Dorado");
            return;
        }
        if ("viento_caos".equals(comodin.getId()) && vientoActivacionPendiente) {
            mostrarAlertaCooldown("Viento del Caos ya se está activando");
            return;
        }

        if ("decreto_silencio".equals(comodin.getId()) && decretoActivacionPendiente) {
            mostrarAlertaCooldown("El Decreto ya se está activando");
            return;
        }

        if ("inversion_letras".equals(comodin.getId()) && espejoActivacionPendiente) {
            mostrarAlertaCooldown("Ya estás activando El Espejo");
            return;
        }
        dialog.dismiss(); // Cerramos el modal de inventario

        obtenerMiNombreEnSala(nombre -> {
            BaseComodin comodinEjecutor = null;

            switch (comodin.getId()) {
                case COMODIN_DOBLE_O_NADA:
                    comodinEjecutor = new ComodinDobleONada(idSala, rondaActual);
                    break;
                case COMODIN_UNO_REVERSA:
                case "cambio_flujo":
                    comodinEjecutor = new ComodinCambioDeFlujo(idSala, rondaActual);
                    break;
                case "ruleta_caotica":
                    comodinEjecutor = new ComodinRuletaCaotica(idSala, rondaActual);
                    break;
                case "viento_caos":
                    vientoActivacionPendiente = true;
                    new ComodinVientoDelCaos(idSala, idPartidaActual, rondaActual)
                            .activar(nombre, new BaseComodin.ComodinCallback() {
                                @Override public void onSuccess(String mensaje) {
                                    vientoActivacionPendiente = false;
                                    comodinesUsadosEstaRonda.add("viento_caos");
                                    // El anuncio para TODOS, incluido quien lo usó,
                                    // llega desde el listener compartido del evento.
                                }
                                @Override public void onError(String mensaje) {
                                    vientoActivacionPendiente = false;
                                    if (isAdded()) NotificationHelper.show(notificationPartida, mensaje, NotificationHelper.WARNING);
                                }
                            });
                    return;
                case "decreto_silencio":
                    decretoActivacionPendiente=true;
                    new ComodinDecretoSilencio(idSala,idPartidaActual,rondaActual)
                            .activar(nombre,new BaseComodin.ComodinCallback() {
                                @Override public void onSuccess(String mensaje) {
                                    decretoActivacionPendiente=false;
                                    comodinesUsadosEstaRonda.add("decreto_silencio");
                                    // Cada telefono anuncia una sola vez desde el evento remoto.
                                }
                                @Override public void onError(String mensaje) {
                                    decretoActivacionPendiente=false;
                                    if(isAdded()) NotificationHelper.show(notificationPartida,mensaje,NotificationHelper.WARNING);
                                }
                            });
                    return;
                case "inversion_letras":
                    mostrarDialogoSeleccionEspejo(nombre);
                    return;
                case "escudo_dorado":
                    activarEscudoDorado(nombre);
                    return;
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
                        comodinesUsadosEstaRonda.add(com.example.basta_game.utils.AliasCambioFlujo.canonico(comodin.getId()));

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

    private void mostrarAlertaCooldown(String mensaje) {
        if (!isAdded() || getContext() == null) return;

        // Creamos un diálogo nativo, Android automáticamente lo pondrá encima del inventario
        Dialog dialogAlerta = new Dialog(getContext());
        dialogAlerta.setContentView(R.layout.dialog_alerta_flotante);
        dialogAlerta.setCancelable(true);

        // Fondo transparente para que se vean los bordes curvos y la elevación de tu tarjeta
        if (dialogAlerta.getWindow() != null) {
            dialogAlerta.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialogAlerta.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView txtMensaje = dialogAlerta.findViewById(R.id.txtMensajeAlertaFlotante);
        android.widget.Button btnEntendido = dialogAlerta.findViewById(R.id.btnEntendidoAlerta);

        if (txtMensaje != null && mensaje != null) {
            txtMensaje.setText(mensaje);
        }

        if (btnEntendido != null) {
            btnEntendido.setOnClickListener(v -> dialogAlerta.dismiss());
        }

        dialogAlerta.show();
        com.example.basta_game.utils.NotificationHelper.acompanar(dialogAlerta);
        Sounds.playNotificaciones();
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
        com.example.basta_game.utils.NotificationHelper.acompanar(dialogVictima);
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
        if (!puedeUsarComodinAhora() || congeladorActivandose || auth.getCurrentUser()==null) return;
        congeladorActivandose=true;
        final String uid=auth.getCurrentUser().getUid();
        final String partida=idPartidaActual;
        final int ronda=rondaActual;
        salaRef.get().addOnCompleteListener(comprobacion -> {
            if (!isAdded() || getView()==null) { congeladorActivandose=false; return; }
            if (!comprobacion.isSuccessful() || comprobacion.getResult()==null ||
                    !com.example.basta_game.utils.ActivacionCongelador.disponibleSnapshot(
                        comprobacion.getResult(),
                        partida,ronda,uid,com.example.basta_game.utils.ConexionJuego.ahoraServidor(),DURACION_CONGELADOR_MS)) {
                congeladorActivandose=false;
                NotificationHelper.show(notificationPartida,
                        "Congelador: máximo 2 usos por partida y no puede llegar a los últimos 10 segundos",
                        NotificationHelper.WARNING);
                return;
            }
            descontarComodinSiHay(COMODIN_CONGELADOR, exito -> {
                if (!exito) {
                    congeladorActivandose=false;
                    NotificationHelper.show(notificationPartida,"No tienes Congelador disponible",NotificationHelper.WARNING);
                    return;
                }
                obtenerMiNombreEnSala(nombre -> activarCongeladorEnSala(nombre,partida,ronda,uid));
            });
        });
    }

    private void activarCongeladorEnSala(String nombre,String partida,int ronda,String uid) {
        final String idUso="congelador_"+ronda+"_"+uid+"_"+java.util.UUID.randomUUID();
        salaRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                return com.example.basta_game.utils.ActivacionCongelador.activar(
                        sala,partida,ronda,uid,nombre,idUso,
                        com.example.basta_game.utils.ConexionJuego.ahoraServidor(),DURACION_CONGELADOR_MS)
                        ? Transaction.success(sala):Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error,boolean committed,DataSnapshot resultado) {
                congeladorActivandose=false;
                if(error==null && committed) {
                    if(isAdded()) {
                        NotificationHelper.show(notificationPartida,
                                "Congelador activado: BASTA bloqueado 8 segundos",NotificationHelper.SUCCESS);
                        Sounds.playNotificaciones();
                    }
                    return;
                }
                // La reserva de sala no se confirmó: devolver unidad consumida.
                DatabaseReference cantidad=FirebaseDatabase.getInstance().getReference("usuarios")
                        .child(uid).child("inventarioComodines").child(COMODIN_CONGELADOR).child("cantidad");
                cantidad.runTransaction(new Transaction.Handler() {
                    @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData dato) {
                        Object valor=dato.getValue();
                        if(!(valor instanceof Number)) return Transaction.abort();
                        dato.setValue(((Number)valor).longValue()+1);
                        return Transaction.success(dato);
                    }
                    @Override public void onComplete(DatabaseError error2,boolean devuelto,DataSnapshot actualizado) {
                        if(isAdded()) NotificationHelper.show(notificationPartida,
                                devuelto && error2==null
                                ? "No se activó Congelador; recuperaste tu unidad"
                                : "No se confirmó Congelador; revisa tu inventario",
                                NotificationHelper.WARNING);
                    }
                },false);
            }
        },false);
    }

    private void mostrarResumenComodines(int rondaCerrada, boolean esFinal) {
        if (!isAdded() || salaRef == null || resumenEnCarga || dialogResumenActual != null ||
                (dialogPuntuacionesActual != null && dialogPuntuacionesActual.isShowing())) return;
        resumenEnCarga = true;
        final String partida = idPartidaActual;
        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot sala) {
                resumenEnCarga = false;
                if (!isAdded() || getView() == null || partida == null ||
                        !partida.equals(sala.child("idPartida").getValue(String.class)) ||
                        leerEntero(sala.child("rondaActual"), -1) != rondaCerrada) return;
                @SuppressWarnings("unchecked") java.util.Map<String,Object> datos =
                        (java.util.Map<String,Object>) sala.getValue();
                List<com.example.basta_game.models.EventoComodin> eventos =
                        com.example.basta_game.utils.ResumenComodines.crear(datos, rondaCerrada, esFinal);
                if (eventos.isEmpty()) { mostrarModalPuntuaciones(rondaCerrada, esFinal); return; }
                Dialog dialog = new Dialog(requireContext());
                dialogResumenActual = dialog;
                dialog.setContentView(R.layout.dialogo_resumen_comodines);
                dialog.setCancelable(false);
                RecyclerView lista = dialog.findViewById(R.id.rvEventosComodines);
                lista.setLayoutManager(new LinearLayoutManager(requireContext()));
                lista.setAdapter(new com.example.basta_game.Adapters.ResumenComodinesAdapter(eventos));
                MaterialButton entendido = dialog.findViewById(R.id.btnEntendidoResumen);
                entendido.setOnClickListener(v -> {
                    dialog.dismiss();
                    dialogResumenActual = null;
                    if (isAdded() && getView() != null) mostrarModalPuntuaciones(rondaCerrada, esFinal);
                });
                dialog.show();
        com.example.basta_game.utils.NotificationHelper.acompanar(dialog);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                resumenEnCarga = false;
                Log.e("RESUMEN_COMODINES", error.getMessage());
                if (isAdded() && getView() != null) mostrarModalPuntuaciones(rondaCerrada, esFinal);
            }
        });
    }
    private boolean puedeUsarComodinAhora() {
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) return false;
        if (salaRef == null || rondaActual <= 0 || !esFaseJugando()) {
            NotificationHelper.show(notificationPartida, "Solo puedes usar comodines durante una ronda activa", NotificationHelper.WARNING);
            return false;
        }
        if (respuestasGuardadas || modalValidacionMostrado || cierreRondaSolicitado) {
            NotificationHelper.show(notificationPartida, "Ya no puedes usar comodines en esta ronda", NotificationHelper.WARNING);
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

        com.example.basta_game.utils.ConsumoInventario.descontar(cantidadRef,
                (descontado, mensaje) -> {
                    if (callback != null) callback.onResultado(descontado);
                    if (!descontado && isAdded() && mensaje != null &&
                            !mensaje.startsWith("No tienes unidades")) {
                        NotificationHelper.show(notificationPartida, mensaje, NotificationHelper.WARNING);
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
        btnBasta.setTextSize(17);

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
        dialog.setCancelable(false);

        dialog.setOnDismissListener(d -> {
            if (dialogValidacionActual == dialog) {
                dialogValidacionActual = null;
            }

            adapterValidacionActual = null;

            limpiarListenerRevisionCategorias();
            limpiarListenerResultadosVotacionPalabras();
            limpiarListenerCooldownVotacion();
            limpiarListenerEvaluacionDecreto();
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
                esAnfitrion,
                siguienteIndice -> { }
        );

        adapterValidacionActual = adapter;

        vpCategorias.setAdapter(adapter);
        vpCategorias.setOffscreenPageLimit(1);

        dialog.setCancelable(false);
        dialog.show();
        com.example.basta_game.utils.NotificationHelper.acompanar(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        escucharValidacionesGlobales(respuestasPorCategoria, adapter, rondaDeEstaRevision);
        escucharResultadosVotacionesPalabras(respuestasPorCategoria, adapter, rondaDeEstaRevision);
        escucharEvaluacionDecreto(respuestasPorCategoria, adapter, rondaDeEstaRevision);
        escucharCooldownVotaciones(adapter, rondaDeEstaRevision);

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

                    // Agrega esta línea justo arriba de btnSalir.setEnabled(true);
                    ViewPager2 vpCategorias = dialog.findViewById(R.id.vpCategoriasValidacion);
                    if (vpCategorias != null) { vpCategorias.setUserInputEnabled(true); }

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
        cierrePorAbandonoMostrado = false;

        respuestasGuardadas = false;
        modalValidacionMostrado = false;
        cierreRondaSolicitado = false;
        bastaActivado = false;

        historialGuardandose = false;
        miHistorialFinalGuardado = false;
        recompensaEnProceso = false;

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

    private void pintarRelojVotacion(TextView contador,String texto) {
        android.text.SpannableString t=new android.text.SpannableString(texto);
        int separador=texto.indexOf('\n');
        if(separador>0) {
            t.setSpan(new android.text.style.AbsoluteSizeSpan(42,true),0,separador,0);
            t.setSpan(new android.text.style.AbsoluteSizeSpan(15,true),separador+1,t.length(),0);
        }
        contador.setText(t);
    }

    private void escucharIndiceRevisionCategoria(ViewPager2 pager,TarjetasValidacionAdapter adapter,int ronda) {
        limpiarListenerRevisionCategorias();
        if(!VerificacionesAuth.VerificarInicioSesion(auth)) return;
        coordinadorRevision=new com.example.basta_game.utils.CoordinadorRevision(salaRef,idPartidaActual,ronda,
                auth.getCurrentUser().getUid(),adapter,pager,texto -> {
                    if(dialogValidacionActual!=null) {
                        TextView contador=dialogValidacionActual.findViewById(R.id.txtRelojGeneralRevision);
                        if(contador!=null) pintarRelojVotacion(contador,texto);
                    }
                    if(dialogVotacionActual!=null) {
                        TextView contador=dialogVotacionActual.findViewById(R.id.txtTiempoVotacion);
                        if(contador!=null) pintarRelojVotacion(contador,texto);
                    }
                }, habilitado -> {
                    if(dialogVotacionActual!=null) for(int id:new int[]{R.id.btnVotoNo,R.id.btnVotoSi,R.id.btnVotoAbstenerse}) {
                        View boton=dialogVotacionActual.findViewById(id); if(boton!=null) boton.setEnabled(habilitado);
                    }
                }, id -> id!=null && id.equals(idVotacionModalActual) && dialogVotacionActual!=null && dialogVotacionActual.isShowing(),
                voto -> revisarSiTodosVotaronYResolver(ronda,voto));
        coordinadorRevision.iniciar();
    }
    private void limpiarListenerRevisionCategorias() {
        if(coordinadorRevision!=null) { coordinadorRevision.cerrar(); coordinadorRevision=null; }
        if(nodoRevisionCategoriasRef!=null && listenerRevisionCategorias!=null) nodoRevisionCategoriasRef.removeEventListener(listenerRevisionCategorias);
        nodoRevisionCategoriasRef=null; listenerRevisionCategorias=null;
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
                            // Una votación válida no exonera la vocal prohibida.
                            boolean multada=respuesta.isDecretoMultada();
                            respuesta.setCorrecta(multada ? false : correctaFinal);
                            respuesta.setPuntos(multada ? 0 : puntosFinales);
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

    /** Decreto: se muestra la multa durante la categoría sin bloquear la votación.
     * El descuento real del total ocurre una sola vez al cerrar la ronda.
     */
    private void escucharEvaluacionDecreto(
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            TarjetasValidacionAdapter adapter, int rondaDeEstaRevision) {
        limpiarListenerEvaluacionDecreto();
        nodoEvaluacionDecreto=salaRef.child("decretoSilencio").child("ronda_"+rondaDeEstaRevision);
        listenerEvaluacionDecreto=nodoEvaluacionDecreto.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot evento) {
                if(!isAdded() || !rondaSigueSiendoActual(rondaDeEstaRevision) ||
                        idPartidaActual==null || !idPartidaActual.equals(evento.child("idPartida").getValue(String.class))) return;
                String vocal=evento.child("vocal").getValue(String.class);
                for(DataSnapshot categoria:evento.child("evaluaciones").getChildren()) {
                    String nombre=categoria.getKey();
                    List<RespuestaValidacion> lista=respuestasPorCategoria.get(nombre);
                    if(lista==null) continue;
                    for(RespuestaValidacion respuesta:lista) {
                        DataSnapshot evaluacion=categoria.child(respuesta.getUidJugador());
                        if(!evaluacion.child("infraccion").exists()) continue;
                        boolean infraccion=Boolean.TRUE.equals(evaluacion.child("infraccion").getValue(Boolean.class));
                        respuesta.establecerDecreto(infraccion,vocal);
                    }
                }
                adapter.actualizarValidaciones();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.w("DECRETO", "No se pudieron mostrar las multas en revisión",error.toException());
            }
        });
    }

    private void limpiarListenerEvaluacionDecreto() {
        if(nodoEvaluacionDecreto!=null && listenerEvaluacionDecreto!=null)
            nodoEvaluacionDecreto.removeEventListener(listenerEvaluacionDecreto);
        nodoEvaluacionDecreto=null;
        listenerEvaluacionDecreto=null;
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
        return com.example.basta_game.utils.ClaveRevision.de(ronda,uidJugador,categoria,respuesta);
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
        final long turnoSolicitado=coordinadorRevision==null ? -1 : coordinadorRevision.turnoActual();
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

        if (respuesta.isVotacionResuelta() || respuesta.isMostrarPuntaje() || (respuesta.isValidada() && respuesta.getPuntos() == 5)) {
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

                crearVotacionActiva(respuesta, rondaDeEstaRevision, turnoSolicitado);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        }));
    }



    private void crearVotacionActiva(RespuestaValidacion respuesta, int rondaDeEstaRevision, long turnoSolicitado) {
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
        datos.put("turnoRevision",turnoSolicitado);
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

        final String partida = idPartidaActual;
        final String uid = auth.getCurrentUser().getUid();
        final com.example.basta_game.utils.CoordinadorRevision reloj=coordinadorRevision;
        final long conexion=com.example.basta_game.utils.ConexionJuego.version();
        salaRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if(reloj==null || !reloj.disponible() || !com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return Transaction.abort();
                if (currentData.getValue() == null) return Transaction.success(currentData);
                return com.example.basta_game.utils.VotacionRevision.crear(currentData,partida,rondaDeEstaRevision,uid,datos,reloj.ahora())
                        ? Transaction.success(currentData) : Transaction.abort();
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
                            "No se puede abrir esa votación: revisa el estado actual",
                            NotificationHelper.WARNING
                    );
                }
            }
        });
    }


    // ---------------- LOGICA DE MINIJUEGOS (FASE 2 Y 3) ----------------

    private final java.util.Set<String> cierresMarcaProgramados = new java.util.HashSet<>();
    private void programarResultadoMarca(DataSnapshot juego) {
        final String uso=juego.child("idUso").getValue(String.class),partida=juego.child("idPartida").getValue(String.class);
        final int ronda=(int)com.example.basta_game.utils.RelojRevision.numero(juego.child("ronda").getValue(),-1);
        if(uso==null || partida==null || ronda<1 || !cierresMarcaProgramados.add(uso)) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if(!isAdded() || salaRef==null || auth.getCurrentUser()==null) {cierresMarcaProgramados.remove(uso);return;}
            final String uid=auth.getCurrentUser().getUid();
            salaRef.runTransaction(new Transaction.Handler() {
                @Override public Transaction.Result doTransaction(MutableData sala) {
                    if(sala.getValue()==null) return Transaction.success(sala);
                    return com.example.basta_game.utils.ResultadoMarca.aplicar(sala,partida,ronda,uso,uid) ? Transaction.success(sala) : Transaction.abort();
                }
                @Override public void onComplete(DatabaseError error,boolean ok,DataSnapshot sala) {
                    cierresMarcaProgramados.remove(uso);
                    if(error!=null && isAdded()) NotificationHelper.showGlobal("No se pudo confirmar el resultado del duelo. Revisa la conexión.",NotificationHelper.WARNING);
                }
            },false);
        },3500);
    }
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
                    if (timerTurnoTER != null) timerTurnoTER.cancel();
                    if (dialogMinijuegoActual != null && dialogMinijuegoActual.isShowing()) dialogMinijuegoActual.dismiss();
                    if (dialogAnuncioMinijuego != null && dialogAnuncioMinijuego.isShowing()) dialogAnuncioMinijuego.dismiss();
                    dialogMinijuegoActual = null;
                    dialogAnuncioMinijuego = null;
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

                // ==========================================================
                // 🔥 NUEVO: SISTEMA ANTI-ABANDONO (RAGEQUIT)
                // ==========================================================
                if (!"terminado".equals(estado) && !"espectador".equals(miRol)) {
                    // Dejamos la instrucción en Firebase: "Si me desconecto, gana el otro"
                    HashMap<String, Object> castigoDesc = new HashMap<>();
                    castigoDesc.put("estado", "terminado");
                    castigoDesc.put("ganadorUid", "atacante".equals(miRol) ? uidVictima : uidAtacante);
                    castigoDesc.put("porDesconexion", true);
                    nodoMinijuegoRef.onDisconnect().updateChildren(castigoDesc);
                } else if ("terminado".equals(estado)) {
                    // Si terminó de forma normal o ya estamos procesando, cancelamos la bomba
                    nodoMinijuegoRef.onDisconnect().cancel();
                    if (timerTurnoTER != null) timerTurnoTER.cancel();
                }

                if ("terminado".equals(estado)) programarResultadoMarca(snapshot);
                if ("terminado".equals(estado)) {
                    Boolean porDesconexion = snapshot.child("porDesconexion").getValue(Boolean.class);
                    if (porDesconexion != null && porDesconexion) {
                        // Cerramos las pantallas porque alguien huyó
                        if (dialogAnuncioMinijuego != null) dialogAnuncioMinijuego.dismiss();
                        if (dialogMinijuegoActual != null) dialogMinijuegoActual.dismiss();

                        return; // Frenamos la carga normal de la arena
                    }
                }
                // ==========================================================

                // LA MÁQUINA DE ESTADOS
                // LA MÁQUINA DE ESTADOS
                if ("anuncio".equals(estado)) {
                    if (dialogMinijuegoActual != null && dialogMinijuegoActual.isShowing()) dialogMinijuegoActual.dismiss();
                    mostrarPantallaAnuncioMinijuego(tipoJuego, miRol, snapshot);
                } else {
                    if (dialogAnuncioMinijuego != null && dialogAnuncioMinijuego.isShowing()) dialogAnuncioMinijuego.dismiss();

                    // --- ENRUTADOR DE MINIJUEGOS ---
                    if ("piedra_papel_tijera".equals(tipoJuego)) {
                        abrirPantallaMinijuego(tipoJuego, miRol, uidAtacante, uidVictima, estado, snapshot);
                    } else if ("tres_en_raya".equals(tipoJuego)) {
                        abrirPantallaMinijuegoTER(miRol, uidAtacante, uidVictima, estado, snapshot);
                    } else if ("duelo_reflejos".equals(tipoJuego)) {
                        // 👇 NUEVO 👇
                        abrirPantallaMinijuegoTiroteo(miRol, uidAtacante, uidVictima, estado, snapshot);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }



    // --- NUEVO: LA SECUENCIA DE ANUNCIO ÉPICA ---
    private final Handler handlerAnuncio=new Handler(Looper.getMainLooper());
    private void mostrarPantallaAnuncioMinijuego(String tipoJuego, String miRol, DataSnapshot snapshot) {
        if (!isAdded()) return;

        if (dialogAnuncioMinijuego != null && dialogAnuncioMinijuego.isShowing()) return;

        dialogAnuncioMinijuego = new Dialog(requireContext());
        dialogAnuncioMinijuego.setContentView(R.layout.dialog_anuncio_minijuego);
        dialogAnuncioMinijuego.setCancelable(false);
        if (dialogAnuncioMinijuego.getWindow() != null) {
            dialogAnuncioMinijuego.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialogAnuncioMinijuego.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialogAnuncioMinijuego.show();
        com.example.basta_game.utils.NotificationHelper.acompanar(dialogAnuncioMinijuego);

        TextView txtTitulo = dialogAnuncioMinijuego.findViewById(R.id.txtTituloAnuncio);
        TextView txtMensaje = dialogAnuncioMinijuego.findViewById(R.id.txtMensajeAnuncio);
        TextView txtAccion = dialogAnuncioMinijuego.findViewById(R.id.txtAccionAnuncio);
        TextView txtContador = dialogAnuncioMinijuego.findViewById(R.id.txtContadorAnuncio);

        String nombreAtacante = snapshot.child("nombreAtacante").getValue(String.class);
        String nombreVictima = snapshot.child("nombreVictima").getValue(String.class);

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

        txtTitulo.setText("espectador".equals(miRol) ? "MODO ESPECTADOR" : "¡DUELO A MUERTE!");
        txtTitulo.setTextSize(22);txtMensaje.setTextSize(18);txtAccion.setTextSize(18);
        txtMensaje.setText(android.text.Html.fromHtml("<b>"+android.text.TextUtils.htmlEncode(nombreAtacante==null ? "Atacante" : nombreAtacante)+"</b> reta a <b>"+android.text.TextUtils.htmlEncode(nombreVictima==null ? "Victima" : nombreVictima)+"</b>."));
        String juego="tres_en_raya".equals(tipoJuego) ? "Tres en Raya: forma una linea de tres; la cuarta ficha retira la primera."
                : "piedra_papel_tijera".equals(tipoJuego) ? "Piedra, Papel o Tijera: gana dos rondas." : "Tiroteo: toca el arma cuando se ponga verde.";
        txtAccion.setText(juego);txtAccion.setTypeface(txtAccion.getTypeface(),android.graphics.Typeface.BOLD);
        final long desde=leerLong(snapshot.child("anuncioDesde"),0);
        final Dialog anuncio=dialogAnuncioMinijuego;
        handlerAnuncio.removeCallbacksAndMessages(null);
        obtenerHoraServidor(hora -> {
            final long base=android.os.SystemClock.elapsedRealtime();
            final boolean[] sonido={false};
            Runnable tick=new Runnable() {
                @Override public void run() {
                    if(!isAdded() || getView()==null || dialogAnuncioMinijuego!=anuncio || !anuncio.isShowing() || desde<=0) return;
                    long restante=desde+9000-(hora+android.os.SystemClock.elapsedRealtime()-base);
                    if(restante>0) {
                        txtContador.setVisibility(restante<=3000 ? View.VISIBLE : View.GONE);
                        if(restante<=3000) {txtContador.setText(String.valueOf((restante+999)/1000));if(!sonido[0]) {sonido[0]=true;Sounds.playCombate();}}
                        handlerAnuncio.postDelayed(this,100);return;
                    }
                    salaRef.child("minijuegoActual").runTransaction(new Transaction.Handler() {
                        @Override public Transaction.Result doTransaction(MutableData d) {
                            if(!"anuncio".equals(d.child("estado").getValue()) || com.example.basta_game.utils.RelojRevision.numero(d.child("anuncioDesde").getValue(),-1)!=desde) return Transaction.abort();
                            d.child("estado").setValue("jugando");return Transaction.success(d);
                        }
                        @Override public void onComplete(DatabaseError e,boolean ok,DataSnapshot d) {
                            if(e!=null) NotificationHelper.showGlobal("No se pudo sincronizar el inicio del duelo",NotificationHelper.WARNING);
                        }
                    },false);
                }
            };tick.run();
        });
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
            if(dialogMinijuegoActual.findViewById(R.id.txtModoEspectador)!=null) dialogMinijuegoActual.findViewById(R.id.txtModoEspectador).setVisibility("espectador".equals(miRol) ? View.VISIBLE : View.GONE);
        com.example.basta_game.utils.NotificationHelper.acompanar(dialogMinijuegoActual);

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
        if(!VerificacionesAuth.VerificarInicioSesion(auth)) return;
        final String uid=auth.getCurrentUser().getUid();
        salaRef.child("minijuegoActual").runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData juego) {
                if(juego.getValue()==null) return Transaction.success(juego);
                return com.example.basta_game.utils.SeleccionMinijuego.elegirPPT(juego,uid,eleccion) ? Transaction.success(juego) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean ok,DataSnapshot juego) {
                if(e!=null) Log.w("DUELO","No se pudo confirmar eleccion",e.toException());
            }
        },false);
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

        btnNo.setEnabled(false); btnSi.setEnabled(false); btnAbstenerse.setEnabled(false);
        btnNo.setOnClickListener(v -> registrarVoto(rondaVotacion, idVotacion, miUid, "no"));
        btnSi.setOnClickListener(v -> registrarVoto(rondaVotacion, idVotacion, miUid, "si"));
        btnAbstenerse.setOnClickListener(v -> registrarVoto(rondaVotacion, idVotacion, miUid, "abstenerse"));

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

                    btnNo.setVisibility(View.GONE);btnSi.setVisibility(View.GONE);btnAbstenerse.setVisibility(View.GONE);
                    txtEstado.setText("¡Has realizado tu voto!\nTu elección: " + ("si".equals(miVoto) ? "Válida" : "no".equals(miVoto) ? "Inválida" : "Abstencion"));
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
        com.example.basta_game.utils.NotificationHelper.acompanar(dialog);
        if(coordinadorRevision!=null) coordinadorRevision.actualizar();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            dialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setGravity(android.view.Gravity.BOTTOM);
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

        if ((esAnfitrion) && !reparacionRondaCeroSolicitada) {
            reparacionRondaCeroSolicitada = true;
            prepararInicioRondaSiHaceFalta(1);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && !yaVolvioAlLobby) {
                setearCronometroGlobal();
            }
        }, 600);
    }

    private void registrarVoto(int rondaVotacion, String idVotacion, String uid, String voto) {
        final String partida = idPartidaActual;
        final com.example.basta_game.utils.CoordinadorRevision reloj=coordinadorRevision;
        final long conexion=com.example.basta_game.utils.ConexionJuego.version();
        salaRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                if(reloj==null || !reloj.disponible() || !com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return Transaction.abort();
                if (sala.getValue() == null) return Transaction.success(sala);
                return com.example.basta_game.utils.VotacionRevision.registrar(sala,partida,rondaVotacion,idVotacion,uid,voto,reloj.ahora())
                        ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot sala) {
                if (error != null && isAdded()) NotificationHelper.show(notificationPartida,
                        "No se pudo confirmar el voto. Reintenta al conectar", NotificationHelper.WARNING);
            }
        }, false);
    }
    private void revisarSiTodosVotaronYResolver(int rondaVotacion,DataSnapshot voto) {
        if(resolviendoVotacion || coordinadorRevision==null) return;
        DataSnapshot actual=coordinadorRevision.votoParaCerrar(voto.child("idVotacion").getValue(String.class));
        if(actual==null) return;
        resolviendoVotacion=true;
        resolverVotacion(rondaVotacion,actual);
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

        String resultado=com.example.basta_game.utils.ResultadoVotos.resolver(votosSi,votosNo);

        if ("abstencion".equals(resultado) || "empate".equals(resultado)) {
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

        confirmarCierreVotacion(rondaVotacion, snapshotVotacion, actualizaciones);
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
                "Sin votos decisivos; se conserva la validacion previa"
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

        confirmarCierreVotacion(rondaVotacion, snapshotVotacion, actualizaciones);
    }



    private void confirmarCierreVotacion(int ronda, DataSnapshot voto, Map<String,Object> cambios) {
        final String partida = idPartidaActual;
        final String id = voto.child("idVotacion").getValue(String.class);
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) { resolviendoVotacion = false; return; }
        final String host = auth.getCurrentUser().getUid();
        final com.example.basta_game.utils.CoordinadorRevision reloj=coordinadorRevision;
        final long conexion=com.example.basta_game.utils.ConexionJuego.version();
        salaRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                if(reloj==null || !reloj.disponible() || !com.example.basta_game.utils.ConexionJuego.vigente(conexion)) return Transaction.abort();
                if (sala.getValue() == null) return Transaction.success(sala);
                return com.example.basta_game.utils.VotacionRevision.resolver(sala,partida,ronda,id,host,
                        voto.child("votos").getValue(),cambios,reloj.ahora()) ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot sala) {
                resolviendoVotacion = false;
                if (error != null) { Log.e("CIERRE_VOTACION",error.getMessage()); return; }
                DataSnapshot actual = sala == null ? null : sala.child("votacionActiva/ronda_" + ronda);
                hayVotacionActivaEnRevision = actual != null && Boolean.TRUE.equals(actual.child("activa").getValue());
                if (!committed && hayVotacionActivaEnRevision && isAdded() && host.equals(sala.child("hostUID").getValue()) &&
                        partida != null && partida.equals(sala.child("idPartida").getValue()) &&
                        leerEntero(sala.child("rondaActual"),-1) == ronda &&
                        !Boolean.TRUE.equals(sala.child("resumenRondaListo/ronda_" + ronda).getValue()) &&
                        !"resultados_finales".equals(sala.child("fase").getValue())) {
                    revisarSiTodosVotaronYResolver(ronda,actual);
                }
            }
        }, false);
    }

    private void cerrarRondaComoHost() {
        if(cierreRondaSolicitado || auth.getCurrentUser()==null || !com.example.basta_game.utils.ConexionJuego.disponible()) return;
        cierreRondaSolicitado=true;final int ronda=rondaActual;final String partida=idPartidaActual,uid=auth.getCurrentUser().getUid();
        salaRef.runTransaction(new Transaction.Handler() {
            @Override public Transaction.Result doTransaction(MutableData sala) {
                if(!com.example.basta_game.utils.ConexionJuego.disponible()) return Transaction.abort();
                if(sala.getValue()==null) return Transaction.success(sala);
                return com.example.basta_game.utils.CierreRevision.aplicar(sala,partida,ronda,uid) ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError e,boolean ok,DataSnapshot s) {
                cierreRondaSolicitado=false;
                if(e!=null) NotificationHelper.showGlobal("No se pudo confirmar el resumen. Reintentaremos al conectar",NotificationHelper.WARNING);
            }
        },false);
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
                if (!esAnfitrion || !rondaSigueSiendoActual(rondaCerrada)) return;
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
                    if (uidJugador == null || !snapshotJugadores.hasChild(uidJugador)) continue;

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
                    .child(uidJugador);

            puntajeRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    // No recrear el nodo de alguien que salio mientras se calculaba la ronda.
                    if (currentData.getValue() == null) return Transaction.success(currentData);
                    Object valorActual = currentData.child("puntajePartida").getValue();

                    int puntajeActual = 0;

                    if (valorActual instanceof Number) {
                        puntajeActual = ((Number) valorActual).intValue();
                    }

                    currentData.child("puntajePartida").setValue(puntajeActual + puntosGanados);
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
        if (!esAnfitrion || cierrePorAbandonoMostrado || !VerificacionesAuth.VerificarInicioSesion(auth)) return;
        final String uid = auth.getCurrentUser().getUid();
        final String partida = idPartidaActual;
        salaRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                if (sala.getValue() == null) return Transaction.success(sala);
                return InicioPartida.publicarResumen(sala, uid, partida, rondaCerrada)
                        ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot sala) {
                if (error != null) Log.e("RESUMEN", error.getMessage());
            }
        }, false);
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
        TextView txtPendientes = vista.findViewById(R.id.txtPendientesRevision);
        txtPendientes.setVisibility(esFinal ? View.GONE : View.VISIBLE);

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

        // Guardamos el estado local del boton: las actualizaciones EN VIVO de puntos
        // no deben borrar el "ESPERANDO JUGADORES" tras pulsar continuar.
        final boolean[] continuarSolicitado = {false};

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

            if (!continuarSolicitado[0] && !Boolean.TRUE.equals(btnListo.getTag())) {
                btnListo.setEnabled(true);
                btnListo.setText(esFinal
                        ? "VOLVER AL LOBBY" : "LISTO PARA SIGUIENTE RONDA");
            }
        });

        // NUEVO: Lógica del botón "USAR COMODÍN"
        if (esFinal) {
            btnUsarComodinFinal.setVisibility(View.GONE); // No hay comodines al finalizar el juego
        } else {
            btnUsarComodinFinal.setVisibility(View.VISIBLE);
            btnUsarComodinFinal.setOnClickListener(v -> {
                // No permitir iniciar poderes una vez confirmada la siguiente ronda.
                if (continuarSolicitado[0] || Boolean.TRUE.equals(btnListo.getTag())) {
                    NotificationHelper.show(notificationPartida,
                            "Ya confirmaste siguiente ronda. No puedes usar comodines.", NotificationHelper.WARNING);
                    return;
                }
                mostrarDialogoComodines(true);
            });
        }

        if (esFinal) {
            btnListo.setOnClickListener(v -> {
                if (!miHistorialFinalGuardado && RecompensasPartida.idValido(idPartidaActual)) {
                    CustomDialogHelper.show(requireContext(), "Recompensa pendiente",
                            recompensaEnProceso ? "El guardado sigue pendiente de confirmacion. Si sales, puede no completarse."
                                    : "No se pudo confirmar la recompensa. Puedes reintentar antes de salir.", true,
                            new CustomDialogHelper.DialogButton("Reintentar", Color.BLUE, Color.WHITE, true,
                                    () -> guardarMiHistorialUsuarioFinal(rondaCerrada)),
                            new CustomDialogHelper.DialogButton("Volver al lobby", Color.GRAY, Color.WHITE, true,
                                    () -> solicitarVolverLobbyFinal(dialog)));
                    return;
                }
                btnListo.setEnabled(false);
                btnListo.setText("VOLVIENDO AL LOBBY...");
                solicitarVolverLobbyFinal(dialog);
            });

        } else {
            btnListo.setOnClickListener(v -> {
                // Cambiar el estado ANTES de escribir en Firebase: la escucha del
                // ranking puede refrescarse inmediatamente durante la transacción.
                continuarSolicitado[0] = true;
                btnListo.setTag(Boolean.TRUE);
                btnListo.setEnabled(false);
                btnListo.setText("ESPERANDO JUGADORES...");
                btnUsarComodinFinal.setEnabled(false);
                btnUsarComodinFinal.setVisibility(View.GONE);
                if (dialogComodinesActual != null && dialogComodinesActual.isShowing()) {
                    dialogComodinesActual.dismiss();
                }
                marcarRevisionLista(rondaCerrada, btnListo,
                        () -> {
                            continuarSolicitado[0] = false;
                            btnListo.setTag(Boolean.FALSE);
                            btnUsarComodinFinal.setEnabled(true);
                            btnUsarComodinFinal.setVisibility(View.VISIBLE);
                        });
                escucharJugadoresListosRevision(rondaCerrada, false);
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
                            if("RULETA".equals(uidAtacante)) mostrarCuentaRuleta(titulo,mensaje,esExito,uidAtacante,uidVictima,leerLong(snapshot.child("timestamp"),0),idUso);
                            else mostrarDialogoAnimadoFlujo(titulo, mensaje, esExito, uidAtacante, uidVictima);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
        // --------------------------------------------------------------------

        dialog.show();
        com.example.basta_game.utils.NotificationHelper.acompanar(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private Dialog dialogRuletaActual;
    private void mostrarCuentaRuleta(String titulo,String mensaje,boolean exito,String atacante,String victima,long desde,String uso) {
        if(!isAdded() || requireContext().getSharedPreferences("ruletaVista",0).getBoolean(idSala+uso,false)) return;
        obtenerHoraServidor(ahora -> {
            if(!isAdded() || getView()==null) return;
            if(dialogRuletaActual!=null) dialogRuletaActual.dismiss();
            Dialog d=new Dialog(requireContext());View v=LayoutInflater.from(requireContext()).inflate(R.layout.dialogo_resultado_flujo,null);
            d.setContentView(v);d.setCancelable(false);dialogRuletaActual=d;
            TextView t=v.findViewById(R.id.txtTituloResultado),m=v.findViewById(R.id.txtMensajeResultado);
            View cerrar=v.findViewById(R.id.btnCerrarResultado);cerrar.setVisibility(View.GONE);
            v.findViewById(R.id.lottieConfeti).setVisibility(View.GONE);
            ImageView icono=v.findViewById(R.id.imgIconoResultado);icono.setImageResource(R.drawable.ic_comodin_ruletacaotica);
            Handler h=new Handler(Looper.getMainLooper());long base=android.os.SystemClock.elapsedRealtime();
            Runnable tick=new Runnable() {public void run() {
                if(!isAdded() || getView()==null || !d.isShowing()) return;
                long restante=desde+5000-(ahora+android.os.SystemClock.elapsedRealtime()-base);
                if(restante<=0) {
                    requireContext().getSharedPreferences("ruletaVista",0).edit().putBoolean(idSala+uso,true).apply();
                    d.dismiss();mostrarDialogoAnimadoFlujo(titulo,mensaje,exito,atacante,victima);return;
                }
                t.setTextSize(20);t.setSingleLine(false);t.setEllipsize(null);t.setText("¡RULETA CAÓTICA!\n"+((restante+999)/1000));m.setText("Eligiendo un efecto al azar...");
                icono.animate().rotationBy(180).setDuration(450).start();h.postDelayed(this,500);
            }};
            d.setOnDismissListener(x -> {h.removeCallbacksAndMessages(null);icono.animate().cancel();if(dialogRuletaActual==d)dialogRuletaActual=null;});
            d.show();
            if(d.getWindow()!=null) d.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels-dp(24),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(t,14,20,1,android.util.TypedValue.COMPLEX_UNIT_SP);
            com.example.basta_game.utils.NotificationHelper.acompanar(d);h.post(tick);
        });
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
        com.example.basta_game.utils.NotificationHelper.acompanar(dialog);

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
                    salaRef.removeEventListener(listenerPuntuacionesEnVivo);
                }

                // AHORA ESCUCHAMOS EN VIVO LOS CAMBIOS DE LA TABLA
                listenerPuntuacionesEnVivo = salaRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot estadoSala) {
                        DataSnapshot snapshotPuntajesRondas=estadoSala.child("puntajesRondas");
                        List<PuntuacionJugador> lista = new ArrayList<>();

                        for (DataSnapshot jugadorSnap : estadoSala.child("jugadores").getChildren()) {
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
                            total+=(int)com.example.basta_game.utils.ImpuestoRevolucionario.ajusteVisible(estadoSala,uid);
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

    private void marcarRevisionLista(int rondaCerrada, MaterialButton boton, Runnable alFallar) {
        if (!VerificacionesAuth.VerificarInicioSesion(auth) || salaRef == null) return;
        final String uid = auth.getCurrentUser().getUid();
        final String partidaEsperada = idPartidaActual;
        salaRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                return com.example.basta_game.utils.InicioPartida.marcarListoRevision(
                        sala, partidaEsperada, rondaCerrada, uid)
                        ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot sala) {
                if (!isAdded() || getView() == null || !rondaSigueSiendoActual(rondaCerrada)) return;
                if (error != null || !committed) {
                    alFallar.run();
                    boton.setEnabled(true);
                    boton.setTag(Boolean.FALSE);
                    boton.setText("LISTO PARA SIGUIENTE RONDA");
                    boolean duelo = sala != null && Boolean.TRUE.equals(
                            sala.child("minijuegoActual/activo").getValue(Boolean.class));
                    NotificationHelper.show(notificationPartida,
                            duelo ? "Hay un duelo de Marca de la Muerte. Espera que termine para continuar."
                                    : "No se pudo confirmar tu preparacion. Reintenta al conectar", NotificationHelper.WARNING);
                }
            }
        }, false);
    }
    private void escucharJugadoresListosRevision(int rondaCerrada, boolean esFinal) {
        limpiarListenerRevision();

        // Observe readiness, membership and authority from the same room snapshot.
        nodoRevisionListaRef = salaRef;
        final String partidaEsperada = idPartidaActual;
        listenerRevisionLista = nodoRevisionListaRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot sala) {
                if (!isAdded() || yaVolvioAlLobby || partidaEsperada == null ||
                        !partidaEsperada.equals(sala.child("idPartida").getValue(String.class)) ||
                        leerEntero(sala.child("rondaActual"), -1) != rondaCerrada ||
                        !"revision_puntuaciones".equals(sala.child("fase").getValue(String.class))) return;
                DataSnapshot jugadores = sala.child("jugadores");
                // Mostrar quién falta, usando exactamente las mismas condiciones que
                // emplea el avance de ronda (no contar jugadores desconectados).
                if (dialogPuntuacionesActual != null && dialogPuntuacionesActual.isShowing()) {
                    TextView pendientes = dialogPuntuacionesActual.findViewById(R.id.txtPendientesRevision);
                    if (pendientes != null) {
                        java.util.ArrayList<String> nombresPendientes = new java.util.ArrayList<>();
                        int listos = 0;
                        int total = 0;
                        for (DataSnapshot jugador : jugadores.getChildren()) {
                            if (Boolean.FALSE.equals(jugador.child("estaConectado").getValue(Boolean.class))) continue;
                            total++;
                            String id = jugador.getKey();
                            if (id != null && Boolean.TRUE.equals(sala.child("revisionLista/ronda_" + rondaCerrada)
                                    .child(id).getValue(Boolean.class))) {
                                listos++;
                            } else {
                                String nombre = jugador.child("nombre").getValue(String.class);
                                nombresPendientes.add(nombre == null || nombre.trim().isEmpty() ? "Jugador" : nombre);
                            }
                        }
                        if (nombresPendientes.isEmpty()) {
                            pendientes.setText("¡Todos listos! Preparando la siguiente ronda...");
                        } else {
                            pendientes.setText("Listos " + listos + "/" + total + " · Faltan: "
                                    + android.text.TextUtils.join(", ", nombresPendientes));
                        }
                    }
                }
                if (dialogPuntuacionesActual != null && dialogPuntuacionesActual.isShowing()
                        && auth.getCurrentUser() != null) {
                    String uidPropio = auth.getCurrentUser().getUid();
                    boolean listo = Boolean.TRUE.equals(sala.child("revisionLista/ronda_" + rondaCerrada)
                            .child(uidPropio).getValue(Boolean.class));
                    if (listo) {
                        MaterialButton boton = dialogPuntuacionesActual.findViewById(R.id.btnVolverInicio);
                        if (boton != null) {
                            boton.setTag(Boolean.TRUE);
                            boton.setEnabled(false);
                            boton.setText("ESPERANDO JUGADORES...");
                            MaterialButton comodines = dialogPuntuacionesActual.findViewById(R.id.btnUsarComodinFinal);
                            if (comodines != null) {
                                comodines.setEnabled(false);
                                comodines.setVisibility(View.GONE);
                            }
                        }
                    }
                }
                if (jugadores.getChildrenCount() <= 1 || cierrePorAbandonoMostrado) return;
                for (DataSnapshot jugador : jugadores.getChildren()) {
                    if(Boolean.FALSE.equals(jugador.child("estaConectado").getValue())) continue;
                    if (!Boolean.TRUE.equals(sala.child("revisionLista/ronda_" + rondaCerrada)
                            .child(jugador.getKey()).getValue(Boolean.class))) return;
                }
                if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(
                        sala.child("hostUID").getValue(String.class))) iniciarNuevaRondaOFinalizar(rondaCerrada);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("REVISION_LISTA", error.getMessage());
                if (isAdded()) NotificationHelper.show(notificationPartida,
                        "No se pudo sincronizar la espera. Revisa tu conexion", NotificationHelper.ERROR);
            }
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

        final String partidaEsperada = idPartidaActual;
        final String uidHost = auth.getCurrentUser().getUid();
        nodoSala.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                if (sala.getValue() == null) return Transaction.success(sala);
                return InicioPartida.avanzar(sala, uidHost, partidaEsperada, rondaCerrada, actualizaciones)
                        ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot sala) {
                if (error != null) {
                    Log.e("RONDA", error.getMessage());
                    if (isAdded() && dialogPuntuacionesActual != null && rondaSigueSiendoActual(rondaCerrada)) {
                        MaterialButton boton = dialogPuntuacionesActual.findViewById(R.id.btnVolverInicio);
                        if (boton != null) {
                            boton.setEnabled(true);
                            boton.setText("REINTENTAR CONTINUAR");
                        }
                        NotificationHelper.show(notificationPartida,
                                "No se pudo confirmar el avance. Reintenta al conectar", NotificationHelper.ERROR);
                    }
                }
            }
        }, false);
    }

    private void resetearSalaParaLobby() {
        resetearSalaParaLobbyYVolver();
    }

    public void actualizarAnfitrion(boolean actual) {
        boolean adquirido = actual && !esAnfitrion;
        esAnfitrion = actual;
        if (getArguments() != null) getArguments().putBoolean("ES_ANFITRION", actual);
        if (adapterValidacionActual != null) adapterValidacionActual.actualizarControlAvance(actual);
        if (adquirido) retomarResponsabilidades();
    }

    private void retomarResponsabilidades() {
        if (!esAnfitrion || salaRef == null || !isAdded() || getView() == null) return;
        final int rondaEsperada = rondaActual;
        final String partidaEsperada = idPartidaActual;
        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot sala) {
                if (!esAnfitrion || !rondaSigueSiendoActual(rondaEsperada) || getView() == null ||
                        (partidaEsperada != null && !partidaEsperada.equals(sala.child("idPartida").getValue(String.class)))) return;
                if ("jugando".equals(sala.child("fase").getValue(String.class)) &&
                        leerEntero(sala.child("inicioRondaListoDe"), -1) != rondaActual) {
                    prepararInicioRondaSiHaceFalta(rondaActual);
                }
                if (adapterValidacionActual != null && adapterValidacionActual.revisionFinalizada()) {
                    cerrarRondaComoHost();
                }
                DataSnapshot votacion = sala.child("votacionActiva/ronda_" + rondaActual);
                if (Boolean.TRUE.equals(votacion.child("activa").getValue(Boolean.class))) {
                    revisarSiTodosVotaronYResolver(rondaActual, votacion);
                }
                if (dialogPuntuacionesActual != null &&
                        "revision_puntuaciones".equals(sala.child("fase").getValue(String.class))) {
                    escucharJugadoresListosRevision(rondaActual, false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { Log.e("HOST", error.getMessage()); }
        });
    }

    private void comprobarFinConUnJugador() {
        if (cierrePorAbandonoMostrado || salaRef == null || !isAdded() || getView() == null) return;
        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot sala) {
                if (cierrePorAbandonoMostrado || !isAdded() || getView() == null ||
                        sala.child("jugadores").getChildrenCount() != 1 ||
                        !"resultados_finales".equals(sala.child("fase").getValue(String.class))) return;
                if (idPartidaActual != null && !idPartidaActual.equals(sala.child("idPartida").getValue(String.class))) return;
                cierrePorAbandonoMostrado = true;
                rondaActual = leerEntero(sala.child("rondaActual"), rondaActual);
                faseActual = "resultados_finales";
                versionCronometro++;
                if (handlerCronometro != null) handlerCronometro.removeCallbacksAndMessages(null);
                if (cronometroRonda != null) cronometroRonda.cancel();
                limpiarListenerRevision();
                if (dialogValidacionActual != null) dialogValidacionActual.dismiss();
                if (dialogPuntuacionesActual != null) dialogPuntuacionesActual.dismiss();
                rondasMaximas = leerEntero(sala.child("cantidadRondas"), rondaActual);
                mostrarModalPuntuaciones(rondaActual, true);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { Log.e("CIERRE", error.getMessage()); }
        });
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

                actualizarAnfitrion(nuevoValorHost);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("HOST_PARTIDA", "Error escuchando rol host: " + error.getMessage());
            }
        });
    }

    private void solicitarVolverLobbyFinal(Dialog dialog) {
        if (salaRef == null || auth.getCurrentUser() == null) {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            volverAlLobbyDespuesDePartida();
            return;
        }
        final String uid=auth.getCurrentUser().getUid();
        final String partida=idPartidaActual;
        salaRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData sala) {
                return com.example.basta_game.utils.SalidaFinal.registrar(sala,partida,uid,
                        LetraSala.ElegirLetraAlAzar(letraElegida))
                        ? Transaction.success(sala) : Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error,boolean committed,DataSnapshot sala) {
                if (!isAdded()) return;
                if (error!=null || !committed) {
                    if (dialog != null && dialog.isShowing()) {
                        MaterialButton volver = dialog.findViewById(R.id.btnVolverInicio);
                        if (volver != null) {
                            volver.setEnabled(true);
                            volver.setText("VOLVER AL LOBBY");
                        }
                    }
                    NotificationHelper.show(notificationPartida,
                            "No se pudo confirmar tu salida de resultados. Reintenta al conectar.",
                            NotificationHelper.WARNING);
                    return;
                }
                if (dialog != null && dialog.isShowing()) dialog.dismiss();
                // Solo este jugador cambia de pantalla. Los demás siguen viendo sus puntos.
                // SalidaFinal reinicia los datos compartidos únicamente si todos ya salieron.
                volverAlLobbyDespuesDePartida();
            }
        },false);
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
        if (!isAdded() || yaVolvioAlLobby || (getActivity() instanceof SalaActivity && ((SalaActivity)getActivity()).haSalidoDeSala())) return;
        if (miHistorialFinalGuardado || recompensaEnProceso) {
            return;
        }

        if (salaRef == null || !VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        if (!RecompensasPartida.idValido(idPartidaActual)) {
            informarErrorRecompensa("Partida antigua sin identificador: no se entregaran recompensas automaticas");
            return;
        }
        recompensaEnProceso = true;

        String miUid = auth.getCurrentUser().getUid();
        int rondaFinalSegura = Math.max(1, rondaFinal);

        salaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                if(!isAdded() || getView()==null || yaVolvioAlLobby ||
                        (getActivity() instanceof SalaActivity && ((SalaActivity)getActivity()).haSalidoDeSala())) {
                    recompensaEnProceso=false; return;
                }
                try {
                    if (!idPartidaActual.equals(snapshotSala.child("idPartida").getValue(String.class)) ||
                            leerEntero(snapshotSala.child("rondaActual"), 0) != rondaFinalSegura ||
                            !"resultados_finales".equals(snapshotSala.child("fase").getValue(String.class))) {
                        informarErrorRecompensa("Los resultados de esta partida ya no estan disponibles");
                        return;
                    }
                    DataSnapshot snapshotJugadores = snapshotSala.child("jugadores");
                    if(snapshotJugadores.getChildrenCount()==1 && snapshotJugadores.hasChild(miUid) && !avisoCierrePorAbandono) {
                        avisoCierrePorAbandono=true;
                        NotificationHelper.showGlobal("Los demas participantes salieron. La partida termino con tus puntos confirmados",NotificationHelper.INFO);
                    }
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

                        total+=(int)com.example.basta_game.utils.ImpuestoRevolucionario.ajusteVisible(snapshotSala,uid);
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
                        informarErrorRecompensa("No se encontro mi usuario en la tabla final");
                        return;
                    }

                    guardarMiHistorialEnUsuario(tabla, miRegistro, rondaFinalSegura, puedeDarMonedas);

                } catch (Exception e) {
                    informarErrorRecompensa("No se pudo preparar la recompensa: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                informarErrorRecompensa("No se pudo leer la partida: " + error.getMessage());
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
            informarErrorRecompensa("La sesion ya no esta disponible");
            return;
        }

        String miUid = auth.getCurrentUser().getUid();

        DatabaseReference usuarioRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(miUid);

        final String idPartida = idPartidaActual;

        HashMap<String, Object> historial = new HashMap<>();

        historial.put("idPartida", idPartida);
        historial.put("idSala", idSala);
        historial.put("codigoSala", codigoSala);
        historial.put("fecha", ServerValue.TIMESTAMP);
        historial.put("cantidadRondas", rondaFinal);
        historial.put("jugadoresCantidad", tabla.size());
        historial.put("monedasEntregadas", puedeDarMonedas);

        historial.put("miUid", miRegistro.uid);
        historial.put("miNombre", miRegistro.nombre);
        historial.put("miPosicion", miRegistro.posicion);
        historial.put("miTotal", miRegistro.total);
        historial.put("monedas", miRegistro.monedas);

        if (!tabla.isEmpty()) {
            historial.put("ganadorUid", tabla.get(0).uid);
            historial.put("ganadorNombre", tabla.get(0).nombre);
            historial.put("puntajeGanador", tabla.get(0).total);
        }

        HashMap<String, Object> top3 = new HashMap<>();
        for (int i = 0; i < Math.min(3, tabla.size()); i++) {
            RegistroJugadorHistorial jugador = tabla.get(i);
            HashMap<String, Object> puesto = new HashMap<>();
            puesto.put("uid", jugador.uid);
            puesto.put("nombre", jugador.nombre);
            puesto.put("avatarUrl", jugador.avatarUrl == null ? "" : jugador.avatarUrl);
            puesto.put("posicion", jugador.posicion);
            puesto.put("total", jugador.total);
            top3.put("puesto_" + jugador.posicion, puesto);
        }
        historial.put("top3", top3);

        // El payload queda fijo antes del primer intento; no hay efectos externos en doTransaction.
        usuarioRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData usuario) {
                if (usuario.getValue() == null) return Transaction.success(usuario);
                try {
                    return RecompensasPartida.aplicar(usuario, idPartida, historial)
                            ? Transaction.success(usuario) : Transaction.abort();
                } catch (RuntimeException error) {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot usuario) {
                boolean aplicada = usuario != null && Boolean.TRUE.equals(usuario
                        .child("recompensasAplicadas").child(idPartida).getValue());
                if (error == null && aplicada) {
                    recompensaEnProceso = false;
                    miHistorialFinalGuardado = true;
                    Log.d("HISTORIAL", "Recompensa confirmada o ya aplicada: " + idPartida);
                    limitarHistorialUsuarioA5(usuarioRef);
                } else {
                    informarErrorRecompensa(error == null ? "No se pudo aplicar la recompensa" : error.getMessage());
                }
            }
        }, false);
    }

    private void informarErrorRecompensa(String mensaje) {
        recompensaEnProceso = false;
        miHistorialFinalGuardado = false;
        Log.e("HISTORIAL", mensaje);
        if (isAdded() && getView() != null) {
            NotificationHelper.show(notificationPartida, mensaje + ". Puedes reintentar desde resultados.",
                    NotificationHelper.WARNING);
        }
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

                        total+=(int)com.example.basta_game.utils.ImpuestoRevolucionario.ajusteVisible(snapshotSala,uid);
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
        actualizaciones.put("poderesRonda", null);
        actualizaciones.put("efectosVisuales", null);
        actualizaciones.put("eventoCaosGlobal", null);
        actualizaciones.put("minijuegoActual", null);
        actualizaciones.put("ultimoMinijuego", null);
        actualizaciones.put("prestamosImpuesto", null);
        actualizaciones.put("rendimientoRondas", null);
        actualizaciones.put("decretoSilencio", null);
        actualizaciones.put("usosCongeladorPartida", 0);
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

    // =========================================================================
    // ================ LÓGICA DE TRES EN RAYA INFINITO (TER) ==================
    // =========================================================================

    private void abrirPantallaMinijuegoTER(String miRol, String uidAtacante, String uidVictima, String estado, DataSnapshot snapshotCompleto) {
        if (!isAdded()) return;

        if (dialogMinijuegoActual == null || !dialogMinijuegoActual.isShowing()) {
            dialogMinijuegoActual = new Dialog(requireContext());
            dialogMinijuegoActual.setContentView(R.layout.dialog_minijuego_ter);
            dialogMinijuegoActual.setCancelable(false);

            if (dialogMinijuegoActual.getWindow() != null) {
                dialogMinijuegoActual.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialogMinijuegoActual.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialogMinijuegoActual.show();
            if(dialogMinijuegoActual.findViewById(R.id.txtModoEspectador)!=null) dialogMinijuegoActual.findViewById(R.id.txtModoEspectador).setVisibility("espectador".equals(miRol) ? View.VISIBLE : View.GONE);
        com.example.basta_game.utils.NotificationHelper.acompanar(dialogMinijuegoActual);

            TextView txtNombreA = dialogMinijuegoActual.findViewById(R.id.txtNombreA_TER);
            TextView txtNombreV = dialogMinijuegoActual.findViewById(R.id.txtNombreV_TER);
            String nombreA = snapshotCompleto.child("nombreAtacante").getValue(String.class);
            String nombreV = snapshotCompleto.child("nombreVictima").getValue(String.class);

            if (txtNombreA != null) txtNombreA.setText(nombreA != null ? nombreA : "Atacante");
            if (txtNombreV != null) txtNombreV.setText(nombreV != null ? nombreV : "Víctima");
        }

        DataSnapshot datos = snapshotCompleto.child("datosJuego");
        String turnoDe = datos.child("turnoDe").getValue(String.class);

        // Extraemos los historiales para saber qué pieza se borrará pronto
        String histA = datos.child("histA").getValue(String.class);
        String histV = datos.child("histV").getValue(String.class);
        if (histA == null) histA = "";
        if (histV == null) histV = "";

        String[] arrA = histA.isEmpty() ? new String[0] : histA.split("-");
        String[] arrV = histV.isEmpty() ? new String[0] : histV.split("-");

        // Identificamos las piezas más viejas (si ya tienen 3 puestas)
        String oldestA = arrA.length == 3 ? arrA[0] : "-1";
        String oldestV = arrV.length == 3 ? arrV[0] : "-1";

        TextView txtTurno = dialogMinijuegoActual.findViewById(R.id.txtTurnoTER);
        boolean esMiTurno = auth.getCurrentUser().getUid().equals(turnoDe) && !"espectador".equals(miRol);
        String simboloMio = "atacante".equals(miRol) ? "X" : "O";
        String miHistorial = "atacante".equals(miRol) ? histA : histV;

        // --- ESTADOS DE CABECERA ---
        // --- ESTADOS DE CABECERA ---
        if ("terminado".equals(estado)) {
            // ... (Lo que ya tienes de 'terminado' se queda igual) ...
            if (timerTurnoTER != null) timerTurnoTER.cancel();
            String ganadorUid = snapshotCompleto.child("ganadorUid").getValue(String.class);
            if (ganadorUid != null) {
                if (auth.getCurrentUser().getUid().equals(ganadorUid)) {
                    txtTurno.setText("¡GANASTE EL DUELO!");
                    txtTurno.setTextColor(android.graphics.Color.parseColor("#2AA514"));
                } else if (!"espectador".equals(miRol)){
                    txtTurno.setText("¡PERDISTE!");
                    txtTurno.setTextColor(android.graphics.Color.parseColor("#E53935"));
                } else {
                    txtTurno.setText("Duelo Finalizado");
                    txtTurno.setTextColor(android.graphics.Color.parseColor("#E65100"));
                }
            }
        } else {
            // ⏱️ LIMPIAMOS EL TIMER ANTERIOR
            if (timerTurnoTER != null) {
                timerTurnoTER.cancel();
                timerTurnoTER = null;
            }

            if ("espectador".equals(miRol)) {
                txtTurno.setText(turnoDe.equals(uidAtacante) ? "Turno de: Atacante (X)" : "Turno de: Víctima (O)");
            } else {
                if (esMiTurno) {
                    txtTurno.setTextColor(android.graphics.Color.parseColor("#2AA514"));

                    // ⏱️ INICIAMOS EL TIMER DE 10 SEGUNDOS (Solo en el celular del que le toca)
                    timerTurnoTER = new CountDownTimer(10000, 1000) {
                        @Override
                        public void onTick(long millis) {
                            if (txtTurno != null) txtTurno.setText("¡ES TU TURNO! (" + (millis / 1000) + "s)");
                        }
                        @Override
                        public void onFinish() {
                            if (txtTurno != null) txtTurno.setText("¡TIEMPO AGOTADO!");

                            // SOLUCIÓN: Reconstruimos un tablero local leyendo de 'datos'
                            // para saber cuáles están vacías sin dar error de símbolo
                            String[] tableroLocal = new String[9];
                            java.util.ArrayList<Integer> vacias = new java.util.ArrayList<>();

                            for (int i = 0; i < 9; i++) {
                                String val = datos.child("c" + i).getValue(String.class);
                                if (val == null) val = "";
                                tableroLocal[i] = val;

                                if (val.isEmpty()) vacias.add(i);
                            }

                            if (!vacias.isEmpty()) {
                                int posAleatoria = vacias.get(new java.util.Random().nextInt(vacias.size()));
                                jugarTurnoTER(posAleatoria, simboloMio, miRol, uidAtacante, uidVictima, tableroLocal, miHistorial);
                            }
                        }
                    }.start();

                } else {
                    txtTurno.setText("Esperando jugada del rival...");
                    txtTurno.setTextColor(android.graphics.Color.parseColor("#E65100"));
                }
            }
        }

        // --- DIBUJAR TABLERO INFINITO ---
        String[] tablero = new String[9];
        for (int i = 0; i < 9; i++) {
            String valor = datos.child("c" + i).getValue(String.class);
            if (valor == null) valor = "";
            tablero[i] = valor;

            int resId = getResources().getIdentifier("celda" + i, "id", requireContext().getPackageName());
            TextView celda = dialogMinijuegoActual.findViewById(resId);

            if (celda != null) {
                celda.setText(valor);

                // Pintar X azul y O rojo
                if (valor.equals("X")) celda.setTextColor(android.graphics.Color.parseColor("#304EDB"));
                else if (valor.equals("O")) celda.setTextColor(android.graphics.Color.parseColor("#E53935"));

                // ¡MAGIA VISUAL! Hacemos fantasmal la pieza que está a punto de desaparecer
                if (String.valueOf(i).equals(oldestA) || String.valueOf(i).equals(oldestV)) {
                    celda.setAlpha(0.3f); // Se ve desvanecida
                } else {
                    celda.setAlpha(1.0f); // Se ve normal
                }

                final int posicion = i;

                // Solo habilitar el clic si es mi turno, el juego no ha terminado y la celda está vacía
                if (!"terminado".equals(estado) && esMiTurno && valor.isEmpty()) {
                    celda.setOnClickListener(v -> jugarTurnoTER(posicion, simboloMio, miRol, uidAtacante, uidVictima, tablero, miHistorial));
                } else {
                    celda.setOnClickListener(null);
                }
            }
        }

        // Si el juego ya terminó en Firebase, le pintamos las celdas ganadoras al jugador que estaba esperando
        if ("terminado".equals(estado)) {
            String ganadorData = verificarGanadorTER(tablero);
            if (ganadorData != null) {
                String[] partes = ganadorData.split("_");
                pintarCeldasGanadorasTER(Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]));
            }
        }
    }

    private void jugarTurnoTER(int posicion, String simbolo, String miRol, String uidAtacante, String uidVictima, String[] tableroActual, String miHistorial) {
        HashMap<String, Object> updates = new HashMap<>();

        // 1. Procesar el historial como una lista
        java.util.ArrayList<String> listaHist = new java.util.ArrayList<>();
        if (!miHistorial.isEmpty()) {
            listaHist.addAll(java.util.Arrays.asList(miHistorial.split("-")));
        }

        // Agregamos la nueva jugada al historial
        listaHist.add(String.valueOf(posicion));

        // 2. Si ya hay más de 3 piezas, ¡La más vieja explota!
        if (listaHist.size() > 3) {
            String viejo = listaHist.remove(0); // Quitamos la posición más antigua
            int posVieja = Integer.parseInt(viejo);
            tableroActual[posVieja] = ""; // Se borra del tablero local
            updates.put("datosJuego/c" + posVieja, ""); // Se borra de Firebase
        }

        // 3. Guardar la nueva jugada
        tableroActual[posicion] = simbolo;
        updates.put("datosJuego/c" + posicion, simbolo);

        // Guardar el nuevo historial
        String nuevoHistorial = android.text.TextUtils.join("-", listaHist);
        if ("atacante".equals(miRol)) {
            updates.put("datosJuego/histA", nuevoHistorial);
        } else {
            updates.put("datosJuego/histV", nuevoHistorial);
        }

        // 4. Verificar si con esta jugada alguien hizo línea de 3
        String ganadorData = verificarGanadorTER(tableroActual);

        if (ganadorData != null) {
            String[] partes = ganadorData.split("_");
            String ganador = partes[0];

            // ¡PINTAMOS DE DORADO INMEDIATAMENTE!
            pintarCeldasGanadorasTER(Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]));

            // ¡EL JUEGO TERMINÓ!
            updates.put("estado", "terminado");
            String ganadorUid = ganador.equals("X") ? uidAtacante : uidVictima;
            updates.put("ganadorUid", ganadorUid);
            salaRef.child("minijuegoActual").updateChildren(updates);



        } else {
            // EL JUEGO SIGUE, CAMBIO DE TURNO
            String siguienteTurno = "atacante".equals(miRol) ? uidVictima : uidAtacante;
            updates.put("datosJuego/turnoDe", siguienteTurno);
            salaRef.child("minijuegoActual").updateChildren(updates);
        }
    }

    private String verificarGanadorTER(String[] tablero) {
        int[][] combinaciones = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // Horizontales
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // Verticales
                {0, 4, 8}, {2, 4, 6}             // Diagonales
        };

        for (int[] pos : combinaciones) {
            if (!tablero[pos[0]].isEmpty() &&
                    tablero[pos[0]].equals(tablero[pos[1]]) &&
                    tablero[pos[0]].equals(tablero[pos[2]])) {

                // Retornamos el ganador Y las posiciones exactas separadas por "_"
                return tablero[pos[0]] + "_" + pos[0] + "_" + pos[1] + "_" + pos[2];
            }
        }
        return null;
    }

    private void pintarCeldasGanadorasTER(int c1, int c2, int c3) {
        if (dialogMinijuegoActual == null || !isAdded()) return;

        int[] celdas = {c1, c2, c3};
        for (int i : celdas) {
            int resId = getResources().getIdentifier("celda" + i, "id", requireContext().getPackageName());
            TextView celda = dialogMinijuegoActual.findViewById(resId);

            if (celda != null) {
                // Fondo dorado épico y letra blanca para resaltar
                celda.setBackgroundColor(android.graphics.Color.parseColor("#FFD700"));
                celda.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                celda.setAlpha(1.0f); // Le quitamos lo fantasma por si era la ficha más vieja
            }
        }
    }



    // =========================================================================
    // ==================== LÓGICA DE DUELO DE REFLEJOS ========================
    // =========================================================================

    private void abrirPantallaMinijuegoTiroteo(String miRol, String uidAtacante, String uidVictima, String estado, DataSnapshot snapshotCompleto) {
        if (!isAdded()) return;

        DataSnapshot datos = snapshotCompleto.child("datosJuego");
        long delayTiroteo = leerLong(datos.child("delayTiroteo"), 3000L);
        long reaccionA = leerLong(datos.child("reaccionA"), 0L);
        long reaccionV = leerLong(datos.child("reaccionV"), 0L);

        // CREACIÓN DE LA ARENA (Solo se ejecuta 1 vez cuando se abre)
        if (dialogMinijuegoActual == null || !dialogMinijuegoActual.isShowing()) {
            dialogMinijuegoActual = new Dialog(requireContext());
            dialogMinijuegoActual.setContentView(R.layout.dialog_minijuego_tiroteo);
            dialogMinijuegoActual.setCancelable(false);

            if (dialogMinijuegoActual.getWindow() != null) {
                dialogMinijuegoActual.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialogMinijuegoActual.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialogMinijuegoActual.show();
            if(dialogMinijuegoActual.findViewById(R.id.txtModoEspectador)!=null) dialogMinijuegoActual.findViewById(R.id.txtModoEspectador).setVisibility("espectador".equals(miRol) ? View.VISIBLE : View.GONE);
        com.example.basta_game.utils.NotificationHelper.acompanar(dialogMinijuegoActual);

            // Seteamos Nombres
            TextView txtNombreA = dialogMinijuegoActual.findViewById(R.id.txtNombreA_Tiro);
            TextView txtNombreV = dialogMinijuegoActual.findViewById(R.id.txtNombreV_Tiro);
            String nombreA = snapshotCompleto.child("nombreAtacante").getValue(String.class);
            String nombreV = snapshotCompleto.child("nombreVictima").getValue(String.class);
            if (txtNombreA != null) txtNombreA.setText(nombreA != null ? nombreA : "Atacante");
            if (txtNombreV != null) txtNombreV.setText(nombreV != null ? nombreV : "Víctima");

            // --- LÓGICA DE CRONÓMETRO INTERNO (Solo para los que juegan) ---
            if (!"espectador".equals(miRol) && !"terminado".equals(estado)) {
                com.google.android.material.card.MaterialCardView btnDisparo = dialogMinijuegoActual.findViewById(R.id.btnAreaDisparo);
                TextView txtEstadoTiro = dialogMinijuegoActual.findViewById(R.id.txtEstadoTiroteo);

                final boolean[] estadoLocal = {false, false}; // [0: yaDisparó, 1: estaVerde]
                final long[] tiempoVerdeLocal = {0L};

                // Acción de disparar
                btnDisparo.setOnClickListener(v -> {
                    if (estadoLocal[0]) return; // Si ya disparaste, no haces nada
                    estadoLocal[0] = true;

                    long miReaccion = 99999; // Foul por defecto

                    if (estadoLocal[1]) {
                        // Disparó bien, calculamos los milisegundos reales
                        miReaccion = System.currentTimeMillis() - tiempoVerdeLocal[0];
                    } else {
                        // Disparó antes de tiempo
                        txtEstadoTiro.setText("¡FALSO INICIO!");
                        btnDisparo.setCardBackgroundColor(android.graphics.Color.parseColor("#E53935")); // Rojo Foul
                    }

                    // Subimos el tiempo a Firebase
                    String nodo = "atacante".equals(miRol) ? "reaccionA" : "reaccionV";
                    salaRef.child("minijuegoActual").child("datosJuego").child(nodo).setValue(miReaccion);
                });

                // El Timer invisible que cambia el color a verde
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (dialogMinijuegoActual != null && dialogMinijuegoActual.isShowing() && !estadoLocal[0] && !"terminado".equals(estado)) {
                        estadoLocal[1] = true;
                        tiempoVerdeLocal[0] = System.currentTimeMillis(); // ¡Aquí inicia el reloj de reflejos!
                        btnDisparo.setCardBackgroundColor(android.graphics.Color.parseColor("#2AA514")); // Verde
                        txtEstadoTiro.setText("¡DISPARA!");

                        // Quitamos el conteo de 5s para que sea una sorpresa
                        Sounds.playDisparo(); // Un simple ¡Pum! cuando se pone verde
                    }
                }, delayTiroteo);
            }
        }

        // --- ACTUALIZAR VISTAS EN TIEMPO REAL ---
        TextView txtReaccionA = dialogMinijuegoActual.findViewById(R.id.txtReaccionA);
        TextView txtReaccionV = dialogMinijuegoActual.findViewById(R.id.txtReaccionV);
        com.google.android.material.card.MaterialCardView btnDisparo = dialogMinijuegoActual.findViewById(R.id.btnAreaDisparo);
        TextView txtEstadoTiro = dialogMinijuegoActual.findViewById(R.id.txtEstadoTiroteo);

        if (reaccionA > 0) {
            txtReaccionA.setText(reaccionA == 99999 ? "Presiono Antes" : reaccionA + " ms");
            txtReaccionA.setTextColor(reaccionA == 99999 ? android.graphics.Color.RED : android.graphics.Color.parseColor("#5E6C84"));
        }
        if (reaccionV > 0) {
            txtReaccionV.setText(reaccionV == 99999 ? "Presiono Antes" : reaccionV + " ms");
            txtReaccionV.setTextColor(reaccionV == 99999 ? android.graphics.Color.RED : android.graphics.Color.parseColor("#5E6C84"));
        }

        // --- ÁRBITRO: EL ATACANTE REVISA SI YA HAY DOS TIEMPOS ---
        if ("atacante".equals(miRol) && reaccionA > 0 && reaccionV > 0 && !"terminado".equals(estado)) {

            salaRef.child("minijuegoActual").child("estado").setValue("terminado");

            String ganadorUid;
            if (reaccionA == 99999 && reaccionV == 99999) {
                ganadorUid = uidVictima; // Si ambos hacen Foul, la víctima sobrevive por default
            } else if (reaccionA < reaccionV) {
                ganadorUid = uidAtacante;
            } else if (reaccionV < reaccionA) {
                ganadorUid = uidVictima;
            } else {
                ganadorUid = uidVictima; // Empate perfecto (rarísimo), el defensor gana
            }

            salaRef.child("minijuegoActual").child("ganadorUid").setValue(ganadorUid);

            boolean atacanteGano = ganadorUid.equals(uidAtacante);

        }

        // --- ESTADOS DE CABECERA Y COLOR FINAL ---
        if ("terminado".equals(estado)) {
            if (btnDisparo != null) btnDisparo.setOnClickListener(null); // Bloquear

            String ganadorUid = snapshotCompleto.child("ganadorUid").getValue(String.class);

            if (ganadorUid != null) {
                if (auth.getCurrentUser().getUid().equals(ganadorUid)) {
                    txtEstadoTiro.setText("¡EL MÁS RÁPIDO!");
                    btnDisparo.setCardBackgroundColor(android.graphics.Color.parseColor("#FFD700")); // Dorado
                    Sounds.playCelebracion();
                } else if (!"espectador".equals(miRol)) {
                    txtEstadoTiro.setText("¡FUISTE MUY LENTO!");
                    btnDisparo.setCardBackgroundColor(android.graphics.Color.parseColor("#E53935")); // Rojo
                    Sounds.playPerdedor();
                } else {
                    txtEstadoTiro.setText("¡DUELO TERMINADO!");
                    btnDisparo.setCardBackgroundColor(android.graphics.Color.parseColor("#5E6C84")); // Gris
                }
            }
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
