package com.example.basta_game.Fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.example.basta_game.Adapters.JugadorPartidaAdapter;
import com.example.basta_game.Adapters.TarjetasValidacionAdapter;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.RespuestaValidacion;
import com.example.basta_game.models.Sala;
import com.example.basta_game.utils.CustomDialogHelper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PartidaFragment extends Fragment {

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

    private ValueEventListener listenerRespuestasValidacion;
    private DatabaseReference nodoRespuestasRondaRef;

    private FirebaseAuth auth;

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
        setearCronometroGlobal();
        escucharBastaParaCronometro();
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
            letraElegida = getArguments().getString("LETRA", "a");
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
    }

    private void configurarFirebase() {
        salaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(codigoSala != null ? codigoSala : "TEST");
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
                                Log.d("BASTA", "BASTA activado correctamente por: " + auth.getCurrentUser().getUid());
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

        btnAbandonar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
    }

    private void mostrarLetra(String letra) {
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
            case "cosa": return R.drawable.ic_cosa;
            case "nombre": return R.drawable.ic_nombre;
            case "apellido": return R.drawable.ic_mujer;
            default: return R.drawable.perfil_menuu;
        }
    }

    public void CambiarEstadoPartida(FirebaseAuth auth, String idSala) {
        if (VerificacionesAuth.VerificarInicioSesion(auth)) {
            DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .child("estadoPartida");

            nodoSala.setValue("En juego").addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("ESTADO", "se cambio el estado de la partida correctamente");
                }
            });
        }
    }

    public void setearCronometroGlobal() {
        DatabaseReference offsetRef = FirebaseDatabase.getInstance()
                .getReference(".info/serverTimeOffset");

        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot offsetSnapshot) {
                Long offset = offsetSnapshot.getValue(Long.class);

                if (offset == null) {
                    offset = 0L;
                }

                long offsetFinal = offset;

                DatabaseReference nodoTiempoInicioRonda = FirebaseDatabase.getInstance()
                        .getReference("salas")
                        .child(idSala)
                        .child("inicioRonda");

                nodoTiempoInicioRonda.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long inicioRonda = snapshot.getValue(Long.class);

                        if (inicioRonda != null) {
                            long horaActualServidor = System.currentTimeMillis() + offsetFinal;

                            long duracionRondaMs = tiempoSeconds * 1000L;
                            long tiempoPasadoMs = horaActualServidor - inicioRonda;
                            long tiempoRestanteMs = duracionRondaMs - tiempoPasadoMs;

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
                                    long segundosRestantes = millisUntilFinished / 1000;
                                    if(segundosRestantes == 5 && !sonidoFinalReproducido){
                                        sonidoFinalReproducido = true;
                                        Sounds.conteoFinalPlay();
                                    }
                                    txtTiempoRestante.setText(formatearTiempo(segundosRestantes));
                                }

                                @Override
                                public void onFinish() {
                                    txtTiempoRestante.setText("00:00");
                                    guardarRespuestasJugador();
                                }
                            };

                            cronometroRonda.start();
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
        if (respuestasGuardadas) {
            return;
        }

        respuestasGuardadas = true;

        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

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
                .child("ronda_" + rondaActual)
                .child(uid);

        nodoRespuestaJugador.setValue(respuestasFinales).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("RESPUESTAS", "Respuestas guardadas correctamente");
                esperarRespuestasYMostrarModal();
            } else {
                Log.d("RESPUESTAS", "Error al guardar respuestas");
                respuestasGuardadas = false;
            }
        });
    }

    // Le agregamos el parámetro del Mapa para poder comparar palabras
    private void validarRespuestasConApi(Map<String, List<RespuestaValidacion>> respuestasPorCategoria) {
        List<ItemRespuesta> respuestasApi = new ArrayList<>();

        for (String categoria : listaNombresCategorias) {
            String respuesta = respuestasJugador.get(categoria);
            if(respuesta == null){
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
                if(response.isSuccessful() && response.body() != null){
                    List<ResultadoApi> resultados = response.body().getResultados();

                    HashMap<String, Object> validacionesJugador = new HashMap<>();

                    for(ResultadoApi resultado : resultados){

                        String categoriaOriginal = mapearCategoriaDesdeApi(resultado.getCategoria());
                        for (String cat : listaNombresCategorias) {
                            if (cat.equalsIgnoreCase(categoriaOriginal)) {
                                categoriaOriginal = cat;
                                break;
                            }
                        }

                        // LÓGICA DE EMPATE (DUPLICADOS)
                        String miRespuesta = respuestasJugador.get(categoriaOriginal);
                        if (miRespuesta == null) miRespuesta = "";

                        int repeticiones = 0;
                        List<RespuestaValidacion> todasLasRespuestas = respuestasPorCategoria.get(categoriaOriginal);

                        if (todasLasRespuestas != null) {
                            for (RespuestaValidacion rv : todasLasRespuestas) {
                                if (rv.getRespuesta() != null &&
                                        rv.getRespuesta().trim().equalsIgnoreCase(miRespuesta.trim())) {
                                    repeticiones++; // Contamos cuántos escribieron exactamente lo mismo
                                }
                            }
                        }

                        int puntosAsignados = 0;
                        if (resultado.isCorrecta()) {
                            // Si es correcta y hay más de 1 que puso lo mismo, son 5 puntos. Sino, 10.
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
                            .child("ronda_" + rondaActual)
                            .child(auth.getCurrentUser().getUid());

                    nodoMisValidaciones.setValue(validacionesJugador).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("VALIDACION", "Resultados de la API subidos a Firebase correctamente.");
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

    private void esperarRespuestasYMostrarModal() {
        nodoRespuestasRondaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("respuestas")
                .child("ronda_" + rondaActual);

        // NUEVO: SALVAVIDAS (FAIL-SAFE)
        // Si pasan 5 segundos y no todos enviaron respuestas, abrimos el modal a la fuerza.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!modalValidacionMostrado && isAdded()) {
                Log.d("VALIDACION", "Forzando apertura del modal por Timeout de jugadores lentos");
                modalValidacionMostrado = true;
                cargarRespuestasValidacionYMostrarModal();
            }
        }, 5000);

        listenerRespuestasValidacion = nodoRespuestasRondaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotRespuestas) {
                long cantidadRespuestas = snapshotRespuestas.getChildrenCount();

                DatabaseReference nodoJugadores = FirebaseDatabase.getInstance()
                        .getReference("salas")
                        .child(idSala)
                        .child("jugadores");

                nodoJugadores.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotJugadores) {
                        long cantidadJugadores = snapshotJugadores.getChildrenCount();

                        if (cantidadJugadores > 0 && cantidadRespuestas >= cantidadJugadores) {
                            if (modalValidacionMostrado) {
                                return;
                            }

                            modalValidacionMostrado = true;

                            if (!isAdded()) {
                                return;
                            }

                            cargarRespuestasValidacionYMostrarModal();
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

    private void cargarRespuestasValidacionYMostrarModal() {
        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                DataSnapshot snapshotJugadores = snapshotSala.child("jugadores");
                DataSnapshot snapshotRespuestas = snapshotSala
                        .child("respuestas")
                        .child("ronda_" + rondaActual);

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
                                lista.add(new RespuestaValidacion(uidJugador, nombreJugador, respuesta, fotoJugador));
                            }
                        }
                    }
                }

                if (!isAdded()) {
                    return;
                }

                validarRespuestasConApi(respuestasPorCategoria);
                mostrarModalValidacionCarrusel(listaNombresCategorias, respuestasPorCategoria);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("VALIDACION", "Error cargando respuestas: " + error.getMessage());
            }
        });
    }

    private boolean bastaSonidoReproducido = false;

    private void escucharBastaParaCronometro() {
        DatabaseReference nodoBastaTimestamp = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("bastaTimeStamp");

        nodoBastaTimestamp.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long bastaTimeStamp = snapshot.getValue(Long.class);

                if (bastaTimeStamp != null) {

                    if (!bastaSonidoReproducido) {
                        bastaSonidoReproducido = true;
                        Sounds.bastaPlay();
                    }

                    recalcularCronometroConBasta(bastaTimeStamp);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("BASTA", "Error escuchando bastaTimeStamp: " + error.getMessage());
            }
        });
    }

    private void recalcularCronometroConBasta(long bastaTimeStamp) {
        DatabaseReference offsetRef = FirebaseDatabase.getInstance()
                .getReference(".info/serverTimeOffset");

        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot offsetSnapshot) {
                Long offset = offsetSnapshot.getValue(Long.class);

                if (offset == null) {
                    offset = 0L;
                }

                long offsetFinal = offset;

                DatabaseReference nodoInicioRonda = FirebaseDatabase.getInstance()
                        .getReference("salas")
                        .child(idSala)
                        .child("inicioRonda");

                nodoInicioRonda.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long inicioRonda = snapshot.getValue(Long.class);

                        if (inicioRonda == null) {
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

                                long segundosRestantes = (millisUntilFinished + 999) / 1000;

                                if (segundosRestantes == 5 && !sonidoFinalReproducido) {
                                    sonidoFinalReproducido = true;
                                    Sounds.conteoFinalPlay();
                                }

                                txtTiempoRestante.setText(formatearTiempo(segundosRestantes));
                            }

                            @Override
                            public void onFinish() {
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
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria
    ) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialogo_respuestas);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        ViewPager2 vpCategorias = dialog.findViewById(R.id.vpCategoriasValidacion);
        androidx.appcompat.widget.AppCompatButton btnSalir = dialog.findViewById(R.id.btnSalirRevision);

        btnSalir.setOnClickListener(v -> dialog.dismiss());

        Runnable accionMostrarBotonSalir = () -> {
            btnSalir.setVisibility(View.VISIBLE);
            determinarGanadorRondaYReproducirSonido(respuestasPorCategoria);
        };

        TarjetasValidacionAdapter adapter = new TarjetasValidacionAdapter(
                categorias,
                respuestasPorCategoria,
                vpCategorias,
                accionMostrarBotonSalir
        );

        vpCategorias.setAdapter(adapter);
        vpCategorias.setOffscreenPageLimit(1);

        dialog.setCancelable(false);
        dialog.show();

        escucharValidacionesGlobales(respuestasPorCategoria, adapter);
    }

    private void escucharValidacionesGlobales(
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            TarjetasValidacionAdapter adapter) {

        DatabaseReference nodoValidaciones = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("validaciones")
                .child("ronda_" + rondaActual);

        nodoValidaciones.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot jugadorValidacion : snapshot.getChildren()) {
                    String uid = jugadorValidacion.getKey();

                    for (DataSnapshot categoriaSnap : jugadorValidacion.getChildren()) {
                        String categoria = categoriaSnap.getKey();
                        Boolean correcta = categoriaSnap.child("correcta").getValue(Boolean.class);
                        Integer puntos = categoriaSnap.child("puntos").getValue(Integer.class);

                        if (correcta != null && puntos != null && uid != null && categoria != null) {

                            // Búsqueda segura ignorando mayúsculas/minúsculas
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
                if(adapter != null) {
                    adapter.actualizarValidaciones();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("VALIDACIONES", "Error escuchando validaciones", error.toException());
            }
        });
    }

    private void determinarGanadorRondaYReproducirSonido(Map<String, List<RespuestaValidacion>> respuestasPorCategoria) {
        HashMap<String, Integer> puntajesTotales = new HashMap<>();

        for (List<RespuestaValidacion> lista : respuestasPorCategoria.values()) {
            for (RespuestaValidacion rv : lista) {
                String uid = rv.getUidJugador();
                int puntosActuales = puntajesTotales.containsKey(uid) ? puntajesTotales.get(uid) : 0;
                puntajesTotales.put(uid, puntosActuales + rv.getPuntos());
            }
        }

        int maxPuntos = -1;
        for (int puntos : puntajesTotales.values()) {
            if (puntos > maxPuntos) {
                maxPuntos = puntos;
            }
        }

        String miUid = auth.getCurrentUser().getUid();
        int misPuntos = puntajesTotales.containsKey(miUid) ? puntajesTotales.get(miUid) : 0;

        // Ganas si tienes el puntaje máximo Y lograste anotar al menos 10 puntos en la partida
        if (maxPuntos > 0 && misPuntos == maxPuntos) {
            Sounds.playCelebracion();
        } else {
            Sounds.playPerdedor();
        }
    }

    // Traduce de la App a la API (Singular -> Plural)
    private String mapearCategoriaParaApi(String categoriaApp) {
        if (categoriaApp == null) return "";
        switch (categoriaApp.toLowerCase(Locale.ROOT)) {
            case "animal": return "animales";
            case "color": return "colores";
            case "fruta": return "frutas";
            case "país":
            case "pais": return "paises"; // Le quita la tilde y lo hace plural
            case "cosa": return "cosas";
            case "nombre": return "nombre";
            case "apellido": return "apellido";
            default: return categoriaApp.toLowerCase(Locale.ROOT);
        }
    }

    // Traduce de la API a la App (Plural -> Singular)
    private String mapearCategoriaDesdeApi(String categoriaApi) {
        if (categoriaApi == null) return "";
        switch (categoriaApi.toLowerCase(Locale.ROOT)) {
            case "animales": return "Animal";
            case "colores": return "Color";
            case "frutas": return "Fruta";
            case "paises": return "País"; // Le devuelve la tilde
            case "cosas": return "Cosa";
            case "nombre": return "Nombre";
            case "apellido": return "Apellido";
            default: return categoriaApi;
        }
    }
}