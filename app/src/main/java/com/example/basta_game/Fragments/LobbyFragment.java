package com.example.basta_game.Fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.basta_game.Adapters.JugadorSalaAdapter;
import com.example.basta_game.R;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.utils.Animations;
import com.example.basta_game.utils.CustomDialogHelper;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class LobbyFragment extends Fragment {

    private boolean esAnfitrion = false;

    private HashMap<String, JugadorSala> jugadoresMemoria = new HashMap<>();
    private boolean primerCargaJugadores = true;
    private boolean estoyListo = false;
    private View notificationLobby;

    private TextView txtCodigoSala;
    private TextView tvValorCategoriasLobby;
    private TextView tvValorRondasLobby;
    private TextView tvValorDuracionLobby;
    private TextView tvValorJugadoresLobby;

    private LinearLayout contenedorCategoriasLobby;

    private FirebaseAuth auth;
    private RecyclerView rvJugadoresLobby;

    private String idSalaMetodo;
    private Button btnComenzarPartida;

    private Bundle bundleDatosPartida;
    private boolean yaEntroAPartida = false;

    private DatabaseReference salaRef;
    private DatabaseReference nodoEstadoPartidaRef;
    private DatabaseReference nodoJugadoresLobbyRef;

    private ValueEventListener listenerEstadoPartida;
    private ValueEventListener listenerJugadoresLobby;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_lobby, container, false);

        ArrayList<String> categoriasStr = new ArrayList<>();
        int tiempoStr = 60;
        int jugadoresStr = 4;
        int rondasStr = 5;
        boolean esCreador = false;
        String codigoSala = "";
        String idSala = "";
        boolean esAnexo = false;

        bundleDatosPartida = getArguments();
        auth = FirebaseAuth.getInstance();

        if (bundleDatosPartida != null) {
            esAnfitrion = bundleDatosPartida.getBoolean("ES_ANFITRION", false);

            ArrayList<String> categoriasBundle = bundleDatosPartida.getStringArrayList("CATEGORIAS");

            if (categoriasBundle != null) {
                categoriasStr = categoriasBundle;
            }

            tiempoStr = bundleDatosPartida.getInt("TIEMPO", 60);
            jugadoresStr = bundleDatosPartida.getInt("JUGADORES", 4);
            rondasStr = bundleDatosPartida.getInt("RONDAS", 5);
            esCreador = bundleDatosPartida.getBoolean("CREADOR", false);
            codigoSala = bundleDatosPartida.getString("CODIGO_SALA", "Esperando...");
            idSala = bundleDatosPartida.getString("IDSALA", "");
            esAnexo = bundleDatosPartida.getBoolean("ANEXO", false);
        }

        idSalaMetodo = idSala;

        if (idSalaMetodo != null && !idSalaMetodo.trim().isEmpty()) {
            salaRef = FirebaseDatabase
                    .getInstance()
                    .getReference("salas")
                    .child(idSalaMetodo);
        }

        String tiempoFormateado = String.format("%d:%02d", tiempoStr / 60, tiempoStr % 60);
        String categoriasFormateadas = TextUtils.join(", ", categoriasStr);

        tvValorCategoriasLobby = view.findViewById(R.id.tvValorCategoriasLobby);
        tvValorDuracionLobby = view.findViewById(R.id.tvValorDuracionLobby);
        tvValorJugadoresLobby = view.findViewById(R.id.tvValorJugadoresLobby);
        tvValorRondasLobby = view.findViewById(R.id.tvValorRondasLobby);
        contenedorCategoriasLobby = view.findViewById(R.id.contenedorCategoriasLobby);
        btnComenzarPartida = view.findViewById(R.id.btnComenzarPartida);

        tvValorCategoriasLobby.setText(categoriasFormateadas);
        mostrarCategoriasComoTarjetas(categoriasStr);

        tvValorRondasLobby.setText(String.valueOf(rondasStr));
        tvValorJugadoresLobby.setText(String.valueOf(jugadoresStr));
        tvValorDuracionLobby.setText(tiempoFormateado);

        rvJugadoresLobby = view.findViewById(R.id.rvJugadoresLobby);
        rvJugadoresLobby.setLayoutManager(new LinearLayoutManager(getContext()));

        ImageView btnRegresar = view.findViewById(R.id.btnRegresar);
        AppCompatButton btnToggleListo = view.findViewById(R.id.btnToggleListo);
        AppCompatButton btnComenzarPartidaVista = view.findViewById(R.id.btnComenzarPartida);
        notificationLobby = view.findViewById(R.id.notificationLobby);
        txtCodigoSala = view.findViewById(R.id.tvidSalaLobby);

        txtCodigoSala.setText(codigoSala);

        Animations.animacionAparicion(btnComenzarPartidaVista);
        Animations.animacionAparicion(btnToggleListo);

        if (estoyListo) {
            btnToggleListo.setText("NO ESTOY LISTO");
            btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
        } else {
            btnToggleListo.setText("ESTOY LISTO");
            btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

        if (esCreador) {
            NotificationHelper.show(
                    notificationLobby,
                    "Sala creada Correctamente",
                    NotificationHelper.SUCCESS
            );
            Sounds.playNotificaciones();
        }

        if (esAnexo) {
            NotificationHelper.show(
                    notificationLobby,
                    "Te has unido a la sala correctamente",
                    NotificationHelper.SUCCESS
            );
            Sounds.playNotificaciones();
        }

        if (esAnfitrion) {
            btnComenzarPartidaVista.setVisibility(View.VISIBLE);
        } else {
            btnComenzarPartidaVista.setVisibility(View.GONE);
        }

        ObtenerJugadoresYLlenarLobby(idSalaMetodo);
        ObtenerEstadoPartida(auth, idSalaMetodo);

        btnRegresar.setOnClickListener(v -> {
            CustomDialogHelper.show(
                    requireContext(),
                    "¿Deseas salir?",
                    "Si sales de la sala abandonarás el lobby actual.",
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

        btnComenzarPartidaVista.setOnClickListener(v -> validarJugadoresEIniciarPartida());

        btnToggleListo.setOnClickListener(v -> {
            estoyListo = !estoyListo;

            if (estoyListo) {
                cambiarEstadoListoJugador(true, btnToggleListo);
            } else {
                cambiarEstadoListoJugador(false, btnToggleListo);
            }
        });

        return view;
    }

    private void validarJugadoresEIniciarPartida() {
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        if (idSalaMetodo == null || idSalaMetodo.trim().isEmpty()) {
            return;
        }

        DatabaseReference nodoJugadores = FirebaseDatabase
                .getInstance()
                .getReference("salas")
                .child(idSalaMetodo)
                .child("jugadores");

        nodoJugadores.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int contadorJugadores = 0;
                int contadorListos = 0;

                for (DataSnapshot hijo : snapshot.getChildren()) {
                    JugadorSala player = hijo.getValue(JugadorSala.class);

                    if (hijo.exists()) {
                        contadorJugadores++;

                        if (player != null && player.isEstaListo()) {
                            contadorListos++;
                        }
                    }
                }

                if (contadorJugadores <= 0) {
                    NotificationHelper.show(
                            notificationLobby,
                            "No hay jugadores en la sala",
                            NotificationHelper.ERROR
                    );
                    Sounds.playNotificaciones();
                    return;
                }

                if (contadorListos < contadorJugadores) {
                    NotificationHelper.show(
                            notificationLobby,
                            "Todos los jugadores deben estar listos",
                            NotificationHelper.ERROR
                    );
                    Sounds.playNotificaciones();
                    return;
                }

                iniciarPartidaDesdeLobby(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                NotificationHelper.show(
                        notificationLobby,
                        "No se pudo revisar la sala",
                        NotificationHelper.ERROR
                );
                Sounds.playNotificaciones();
            }
        });
    }

    private void iniciarPartidaDesdeLobby(DataSnapshot snapshotJugadores) {
        if (salaRef == null) {
            return;
        }

        if (btnComenzarPartida != null) {
            btnComenzarPartida.setEnabled(false);
        }

        HashMap<String, Object> actualizaciones = new HashMap<>();

        actualizaciones.put("estadoPartida", "En juego");
        actualizaciones.put("fase", "jugando");
        actualizaciones.put("rondaActual", 1);
        actualizaciones.put("inicioRonda", ServerValue.TIMESTAMP);
        actualizaciones.put("inicioRondaDe", 1);

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

        actualizaciones.put("comodinesPendientes", null);
        actualizaciones.put("comodinesActivos", null);
        actualizaciones.put("comodinesUsados", null);

        actualizaciones.put("revisionCategorias", null);
        actualizaciones.put("resultadosVotacionPalabras", null);
        actualizaciones.put("cooldownVotacion", null);

        actualizaciones.put("reseteoFinalEnProceso", null);
        actualizaciones.put("historialGuardado", null);
        actualizaciones.put("cambioRondaTimestamp", ServerValue.TIMESTAMP);

        for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
            String uid = jugadorSnap.getKey();

            if (uid != null) {
                actualizaciones.put("jugadores/" + uid + "/puntajePartida", 0);
                actualizaciones.put("jugadores/" + uid + "/estaListo", false);
            }
        }

        salaRef.updateChildren(actualizaciones).addOnCompleteListener(task -> {
            if (!isAdded()) {
                return;
            }

            if (task.isSuccessful()) {
                if (bundleDatosPartida != null) {
                    bundleDatosPartida.putInt("RONDA_ACTUAL", 1);
                    bundleDatosPartida.putBoolean("CREADOR", esAnfitrion);
                    bundleDatosPartida.putBoolean("ANEXO", false);
                }

                Log.d("LOBBY", "Partida iniciada correctamente con rondaActual=1");

            } else {
                if (btnComenzarPartida != null) {
                    btnComenzarPartida.setEnabled(true);
                }

                NotificationHelper.show(
                        notificationLobby,
                        "No se pudo iniciar la partida",
                        NotificationHelper.ERROR
                );

                Sounds.playNotificaciones();
            }
        });
    }

    private void cambiarEstadoListoJugador(boolean listo, AppCompatButton btnToggleListo) {
        if (auth != null && VerificacionesAuth.VerificarInicioSesion(auth)) {

            String uid = auth.getCurrentUser().getUid();

            DatabaseReference nodoSala = FirebaseDatabase
                    .getInstance()
                    .getReference("salas")
                    .child(idSalaMetodo)
                    .child("jugadores")
                    .child(uid);

            nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    JugadorSala jugador = snapshot.getValue(JugadorSala.class);

                    if (jugador != null) {
                        jugador.setEstaListo(listo);

                        nodoSala.setValue(jugador).addOnCompleteListener(taskEstado -> {
                            if (taskEstado.isSuccessful()) {

                                if (listo) {
                                    btnToggleListo.setText("NO ESTOY LISTO");
                                    btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
                                } else {
                                    btnToggleListo.setText("ESTOY LISTO");
                                    btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                                }

                            } else {
                                NotificationHelper.show(
                                        notificationLobby,
                                        "Hubo un error al cambiar de estado",
                                        NotificationHelper.ERROR
                                );
                                Sounds.playNotificaciones();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void mostrarCategoriasComoTarjetas(ArrayList<String> categorias) {
        contenedorCategoriasLobby.removeAllViews();

        if (categorias == null || categorias.isEmpty()) {
            TextView vacio = new TextView(requireContext());
            vacio.setText("Sin categorías seleccionadas");
            vacio.setTextColor(Color.parseColor("#9E9E9E"));
            vacio.setTextSize(13);
            vacio.setGravity(Gravity.CENTER);
            vacio.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_regular));
            contenedorCategoriasLobby.addView(vacio);
            return;
        }

        LinearLayout filaActual = null;

        for (int i = 0; i < categorias.size(); i++) {

            if (i % 2 == 0) {
                filaActual = new LinearLayout(requireContext());
                filaActual.setOrientation(LinearLayout.HORIZONTAL);
                filaActual.setGravity(Gravity.CENTER);

                LinearLayout.LayoutParams paramsFila = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                paramsFila.setMargins(0, dp(4), 0, dp(4));
                filaActual.setLayoutParams(paramsFila);
                contenedorCategoriasLobby.addView(filaActual);
            }

            MaterialCardView tarjeta = crearTarjetaCategoriaLobby(categorias.get(i));

            LinearLayout.LayoutParams paramsTarjeta = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );

            paramsTarjeta.setMargins(dp(4), dp(4), dp(4), dp(4));

            if (filaActual != null) {
                filaActual.addView(tarjeta, paramsTarjeta);
            }
        }

        if (categorias.size() % 2 != 0 && contenedorCategoriasLobby.getChildCount() > 0) {
            LinearLayout ultimaFila = (LinearLayout) contenedorCategoriasLobby.getChildAt(
                    contenedorCategoriasLobby.getChildCount() - 1
            );

            View espacioVacio = new View(requireContext());

            LinearLayout.LayoutParams paramsEspacio = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );

            paramsEspacio.setMargins(dp(4), dp(4), dp(4), dp(4));
            ultimaFila.addView(espacioVacio, paramsEspacio);
        }
    }

    private MaterialCardView crearTarjetaCategoriaLobby(String nombreCategoria) {
        MaterialCardView card = new MaterialCardView(requireContext());

        card.setRadius(dp(18));
        card.setCardElevation(dp(3));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(Color.parseColor("#BFD0FF"));
        card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EEF4FF")));

        LinearLayout contenido = new LinearLayout(requireContext());
        contenido.setOrientation(LinearLayout.HORIZONTAL);
        contenido.setGravity(Gravity.CENTER_VERTICAL);
        contenido.setPadding(dp(10), dp(9), dp(10), dp(9));

        MaterialCardView cardIcono = new MaterialCardView(requireContext());
        cardIcono.setRadius(dp(14));
        cardIcono.setCardElevation(0);
        cardIcono.setStrokeWidth(dp(1));
        cardIcono.setStrokeColor(Color.parseColor("#0017ED"));
        cardIcono.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));

        LinearLayout.LayoutParams paramsCardIcono = new LinearLayout.LayoutParams(dp(40), dp(40));

        ImageView icono = new ImageView(requireContext());
        icono.setImageResource(obtenerIconoCategoria(nombreCategoria));
        icono.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icono.setPadding(dp(7), dp(7), dp(7), dp(7));

        cardIcono.addView(icono);

        TextView texto = new TextView(requireContext());
        texto.setText(nombreCategoria);
        texto.setTextColor(Color.parseColor("#0017ED"));
        texto.setTextSize(16);
        texto.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.lilitaone_regular));
        texto.setSingleLine(true);
        texto.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams paramsTexto = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );

        paramsTexto.setMargins(dp(8), 0, 0, 0);

        contenido.addView(cardIcono, paramsCardIcono);
        contenido.addView(texto, paramsTexto);

        card.addView(contenido);

        return card;
    }

    private int obtenerIconoCategoria(String categoria) {
        if (categoria == null) {
            return R.drawable.outline_person_24;
        }

        switch (categoria) {
            case "Nombre":
                return R.drawable.ic_nombre;
            case "Apellido":
                return R.drawable.ic_mujer;
            case "Fruta":
                return R.drawable.ic_frutas2;
            case "Color":
                return R.drawable.ic_paleta2;
            case "Cosa":
                return R.drawable.ic_cosa;
            case "Animal":
                return R.drawable.ic_animal3;
            case "País":
                return R.drawable.ic_pais2;
            case "Pais":
                return R.drawable.ic_pais2;
            case "Pelicula":
                return R.drawable.ic_pelicula;
            case "Película":
                return R.drawable.ic_pelicula;
            case "Cancion":
                return R.drawable.ic_cancion;
            case "Canción":
                return R.drawable.ic_cancion;
            default:
                return R.drawable.outline_person_24;
        }
    }

    private int dp(int valor) {
        return (int) (valor * getResources().getDisplayMetrics().density);
    }

    public void ObtenerJugadoresYLlenarLobby(String idSala) {
        auth = FirebaseAuth.getInstance();

        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        if (idSala == null || idSala.trim().isEmpty()) {
            return;
        }

        nodoJugadoresLobbyRef = FirebaseDatabase
                .getInstance()
                .getReference("salas")
                .child(idSala)
                .child("jugadores");

        limpiarListenerJugadoresLobby();

        listenerJugadoresLobby = nodoJugadoresLobbyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<JugadorSala> jugadoresEnSala = new ArrayList<>();
                HashMap<String, JugadorSala> nuevosJugadores = new HashMap<>();

                for (DataSnapshot hijo : snapshot.getChildren()) {
                    JugadorSala jugador = hijo.getValue(JugadorSala.class);
                    if (jugador != null) {
                        jugadoresEnSala.add(jugador);
                        nuevosJugadores.put(hijo.getKey(), jugador);
                    }
                }

                if (!primerCargaJugadores) {
                    for (String uid : nuevosJugadores.keySet()) {
                        if (!jugadoresMemoria.containsKey(uid) && !uid.equals(auth.getCurrentUser().getUid())) {
                            NotificationHelper.show(notificationLobby, nuevosJugadores.get(uid).getNombre() + " entró a la sala", NotificationHelper.INFO);
                            Sounds.playNotificaciones();
                        }
                    }
                    for (String uid : jugadoresMemoria.keySet()) {
                        if (!nuevosJugadores.containsKey(uid)) {
                            NotificationHelper.show(notificationLobby, jugadoresMemoria.get(uid).getNombre() + " abandonó la sala", NotificationHelper.WARNING);
                        }
                    }
                }
                primerCargaJugadores = false;
                jugadoresMemoria.clear();
                jugadoresMemoria.putAll(nuevosJugadores);

                if (rvJugadoresLobby == null) return;
                JugadorSalaAdapter adaptador = new JugadorSalaAdapter(jugadoresEnSala);
                rvJugadoresLobby.setAdapter(adaptador);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void ObtenerEstadoPartida(FirebaseAuth auth, String salaID) {
        if (!VerificacionesAuth.VerificarInicioSesion(auth)) {
            return;
        }

        if (salaID == null || salaID.trim().isEmpty()) {
            return;
        }

        nodoEstadoPartidaRef = FirebaseDatabase
                .getInstance()
                .getReference("salas")
                .child(salaID);

        limpiarListenerEstadoPartida();

        listenerEstadoPartida = nodoEstadoPartidaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }

                if (yaEntroAPartida) {
                    Log.d("OBTENER", "ENTRO AL RETURN DE YA ENTRO A PARTIDA");
                    return;
                }

                String estado = snapshot.child("estadoPartida").getValue(String.class);
                String fase = snapshot.child("fase").getValue(String.class);

                int rondaFirebase = leerEntero(snapshot.child("rondaActual"), 0);
                Long inicioRonda = snapshot.child("inicioRonda").getValue(Long.class);
                int inicioRondaDe = leerEntero(snapshot.child("inicioRondaDe"), -1);

                Log.d("OBTENER", "EL VALOR DE LA SALA ES: " + estado);
                Log.d("OBTENER", "fase=" + fase + " ronda=" + rondaFirebase + " inicioRondaDe=" + inicioRondaDe);

                boolean salaListaParaPartida =
                        "En juego".equalsIgnoreCase(estado) &&
                                "jugando".equalsIgnoreCase(fase) &&
                                rondaFirebase > 0 &&
                                inicioRonda != null &&
                                inicioRondaDe == rondaFirebase;

                if (!salaListaParaPartida) {
                    Log.d("OBTENER", "Sala aún no está lista para abrir partida");
                    return;
                }

                yaEntroAPartida = true;

                if (bundleDatosPartida != null) {
                    bundleDatosPartida.putInt("RONDA_ACTUAL", rondaFirebase);
                    bundleDatosPartida.putBoolean("CREADOR", esAnfitrion);
                    bundleDatosPartida.putBoolean("ANEXO", false);
                }

                LoadingPartidaFragment loading = new LoadingPartidaFragment();
                loading.setArguments(bundleDatosPartida);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.sala_fragment_container, loading)
                        .commitAllowingStateLoss();

                Log.d("OBTENER", "SE CREO LA PARTIDA A PARTIDA");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("OBTENER", "Error leyendo estado de partida: " + error.getMessage());
            }
        });
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

    private void limpiarListenerEstadoPartida() {
        if (nodoEstadoPartidaRef != null && listenerEstadoPartida != null) {
            nodoEstadoPartidaRef.removeEventListener(listenerEstadoPartida);
            listenerEstadoPartida = null;
        }
    }

    private void limpiarListenerJugadoresLobby() {
        if (nodoJugadoresLobbyRef != null && listenerJugadoresLobby != null) {
            nodoJugadoresLobbyRef.removeEventListener(listenerJugadoresLobby);
            listenerJugadoresLobby = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        limpiarListenerEstadoPartida();
        limpiarListenerJugadoresLobby();

        rvJugadoresLobby = null;
        notificationLobby = null;
        txtCodigoSala = null;
        btnComenzarPartida = null;
    }
}