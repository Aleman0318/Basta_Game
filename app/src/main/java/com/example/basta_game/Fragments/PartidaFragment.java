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
            case "animal":
                return R.drawable.ic_animal3;
            case "color":
                return R.drawable.ic_paleta2;
            case "fruta":
                return R.drawable.ic_frutas2;
            case "país":
                return R.drawable.ic_pais2;
            case "cosa":
                return R.drawable.ic_cosa;
            case "nombre":
                return R.drawable.ic_nombre;
            case "apellido":
                return R.drawable.ic_mujer;
            default:
                return R.drawable.perfil_menuu;
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
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
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
                public void onCancelled(@NonNull DatabaseError error) {

                }
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
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
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
    }

    private void cambiarEstadoUsuarios() {
        Log.d("CAMBIAR", "Voy a poner estaListo=false en sala=" + idSala + " uid=" + auth.getCurrentUser().getUid());

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

    private void esperarRespuestasYMostrarModal() {
        nodoRespuestasRondaRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala)
                .child("respuestas")
                .child("ronda_" + rondaActual);

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
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
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
                                lista.add(
                                        new RespuestaValidacion(
                                                nombreJugador,
                                                respuesta,
                                                fotoJugador
                                        )
                                );
                            }
                        }
                    }
                }

                if (!isAdded()) {
                    return;
                }

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
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}