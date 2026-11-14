package com.example.basta_game.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.Adapters.CategoriaAdapter;
import com.example.basta_game.R;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.models.CodigoSalaSearch;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.Sala;
import com.example.basta_game.models.Usuario;
import com.example.basta_game.utils.Animations;
import com.example.basta_game.utils.CodigoSala;
import com.example.basta_game.utils.LetraSala;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Crear_salaFragment extends Fragment {

    private int contadorJugadores = 2;
    private int contadorRondas = 1;

    private final ArrayList<String> categoriasActivas = new ArrayList<>();
    private final ArrayList<Categoria> listaCategoriasBase = new ArrayList<>();

    private RecyclerView rvCategorias;
    private SeekBar sbTiempo;

    private TextView tvTiempoActual;
    private TextView tvCantRondas;
    private TextView tvCantJugadores;

    private String letraElegidabundle;

    private AppCompatButton btnRestarRonda;
    private AppCompatButton btnSumarRonda;
    private AppCompatButton btnRestarJugador;
    private AppCompatButton btnSumarJugador;
    private AppCompatButton btnCrearSalaFinal;

    private ImageView btnRegresar;

    private FirebaseAuth auth;

    private View notificationCrearSala;

    private String codigoSalaLobby;
    private String idSalaLobby;
    private String estadoPartida;

    private int rondaActual;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_crear_sala, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inicializarVistas(view);
        configurarCategorias();
        configurarTiempo();
        configurarRondas();
        configurarJugadores();
        configurarBotonCrearSala();
        configurarBotonRegresar();
    }

    private void inicializarVistas(View view) {
        rvCategorias = view.findViewById(R.id.rvCategorias);

        sbTiempo = view.findViewById(R.id.sbTiempo);
        tvTiempoActual = view.findViewById(R.id.tvTiempoActual);

        btnRestarRonda = view.findViewById(R.id.btnRestarRonda);
        btnSumarRonda = view.findViewById(R.id.btnSumarRonda);
        tvCantRondas = view.findViewById(R.id.tvCantRondas);

        btnRestarJugador = view.findViewById(R.id.btnRestarJugador);
        btnSumarJugador = view.findViewById(R.id.btnSumarJugador);
        tvCantJugadores = view.findViewById(R.id.tvCantJugadores);

        btnCrearSalaFinal = view.findViewById(R.id.btnCrearSalaFinal);
        btnRegresar = view.findViewById(R.id.btnRegresar);

        notificationCrearSala = view.findViewById(R.id.notificationCrearSala);

        Animations.animacionAparicion(btnCrearSalaFinal);
    }

    private void configurarCategorias() {
        rvCategorias.setLayoutManager(new GridLayoutManager(getContext(), 2));

        listaCategoriasBase.clear();

        listaCategoriasBase.add(new Categoria("Nombre", R.drawable.ic_nombre));
        listaCategoriasBase.add(new Categoria("Apellido", R.drawable.ic_mujer));
        listaCategoriasBase.add(new Categoria("Fruta", R.drawable.ic_frutas2));
        listaCategoriasBase.add(new Categoria("Color", R.drawable.ic_paleta2));
        listaCategoriasBase.add(new Categoria("Cosa", R.drawable.ic_cosa));
        listaCategoriasBase.add(new Categoria("Animal", R.drawable.ic_animal3));
        listaCategoriasBase.add(new Categoria("País", R.drawable.ic_pais2));
        listaCategoriasBase.add(new Categoria("Pelicula", R.drawable.ic_pelicula));
        listaCategoriasBase.add(new Categoria("Cancion", R.drawable.ic_cancion));

        for (Categoria categoria : listaCategoriasBase) {
            categoria.setHabilitada(true);
        }

        actualizarCategoriasActivasEnOrden();

        CategoriaAdapter adaptador = new CategoriaAdapter(listaCategoriasBase, new CategoriaAdapter.OnCategoriaClickListener() {
            @Override
            public void onCategoriaToggle(Categoria categoria, boolean estaHabilitada) {
                actualizarCategoriasActivasEnOrden();
            }
        });

        rvCategorias.setAdapter(adaptador);
    }

    private void actualizarCategoriasActivasEnOrden() {
        categoriasActivas.clear();

        for (Categoria categoria : listaCategoriasBase) {
            if (categoria.isHabilitada()) {
                categoriasActivas.add(categoria.getNombre());
            }
        }
    }

    private void configurarTiempo() {
        actualizarTextoTiempo();

        sbTiempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                actualizarTextoTiempo();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void actualizarTextoTiempo() {
        int totalSegundos = obtenerTiempoSeleccionado();
        String tiempoFormateado = String.format("%d:%02d min", totalSegundos / 60, totalSegundos % 60);
        tvTiempoActual.setText(tiempoFormateado);
    }

    private int obtenerTiempoSeleccionado() {
        return 60 + (sbTiempo.getProgress() * 10);
    }

    private void configurarRondas() {
        tvCantRondas.setText(String.valueOf(contadorRondas));

        btnRestarRonda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restarRonda();
            }
        });

        btnSumarRonda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sumarRonda();
            }
        });
    }

    private void restarRonda() {
        if (contadorRondas > 1) {
            contadorRondas--;
            tvCantRondas.setText(String.valueOf(contadorRondas));
        }
    }

    private void sumarRonda() {
        if (contadorRondas < 26) {
            contadorRondas++;
            tvCantRondas.setText(String.valueOf(contadorRondas));
        }
    }

    private int ObtenerRondasMaximas() {
        return Integer.parseInt(tvCantRondas.getText().toString());
    }

    private void configurarJugadores() {
        tvCantJugadores.setText(String.valueOf(contadorJugadores));

        btnRestarJugador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restarJugador();
            }
        });

        btnSumarJugador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sumarJugador();
            }
        });
    }

    private void restarJugador() {
        if (contadorJugadores > 2) {
            contadorJugadores--;
            tvCantJugadores.setText(String.valueOf(contadorJugadores));
        }
    }

    private void sumarJugador() {
        if (contadorJugadores < 8) {
            contadorJugadores++;
            tvCantJugadores.setText(String.valueOf(contadorJugadores));
        }
    }

    private int ObtenerJugadoresMaximos() {
        return Integer.parseInt(tvCantJugadores.getText().toString());
    }

    private void configurarBotonCrearSala() {
        btnCrearSalaFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                crearSala();
            }
        });
    }

    private void crearSala() {

        Log.d("DEBUG_SALA", "Entrando a funcion sala");

        auth = FirebaseAuth.getInstance();

        if (auth != null && VerificacionesAuth.VerificarInicioSesion(auth)) {

            if (!validarConfiguracionSala()) {
                return;
            }

            Log.d("DEBUG_SALA", "Sesion validada con metodo verificarsesion");

            String usuarioUID = auth.getCurrentUser().getUid();

            DatabaseReference db = FirebaseDatabase
                    .getInstance()
                    .getReference("usuarios")
                    .child(usuarioUID);

            db.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    Usuario user = snapshot.getValue(Usuario.class);

                    if (user == null || !com.example.basta_game.utils.IdentidadJugador.nombreValido(user.getNombre())) {
                        NotificationHelper.show(
                                notificationCrearSala,
                                "¡No se pudo obtener el usuario!",
                                NotificationHelper.ERROR
                        );
                        Sounds.playNotificaciones();
                        return;
                    }

                    String nombreUsuario = user.getNombre();
                    String urlAvatar = user.getFotoPerfilUrl();

                    String estadoSala = "En espera";
                    estadoPartida = estadoSala;

                    String letraElegida = LetraSala.ElegirLetraAlAzar();
                    letraElegidabundle = letraElegida;

                    String codigoSala = CodigoSala.GenerarCodigoSala();
                    codigoSalaLobby = codigoSala;

                    rondaActual = 1;

                    long fechaCreacion = System.currentTimeMillis();

                    int jugadoresMaximos = ObtenerJugadoresMaximos();
                    int rondasMaximas = ObtenerRondasMaximas();
                    int tiempoRonda = obtenerTiempoSeleccionado();

                    ArrayList<String> categoriasSeleccionadas = ObtenerCategoriasActivas();

                    DatabaseReference nodoSala = FirebaseDatabase
                            .getInstance()
                            .getReference("salas");

                    DatabaseReference salaConID = nodoSala.push();

                    String idSalaString = salaConID.getKey();
                    idSalaLobby = idSalaString;

                    Sala sala = new Sala(
                            idSalaString,
                            codigoSala,
                            usuarioUID,
                            nombreUsuario,
                            estadoSala,
                            letraElegida,
                            rondaActual,
                            rondasMaximas,
                            fechaCreacion,
                            jugadoresMaximos,
                            tiempoRonda,
                            categoriasSeleccionadas
                    );

                    salaConID.setValue(sala).addOnCompleteListener(taskSala -> {
                        if (taskSala.isSuccessful()) {

                            Log.d("DEBUG_SALA", "Sala creada correctamente");

                            JugadorSala jugador = new JugadorSala(
                                    usuarioUID,
                                    nombreUsuario,
                                    0,
                                    false,
                                    true,
                                    true,
                                    urlAvatar
                            );

                            salaConID.child("jugadores")
                                    .child(usuarioUID)
                                    .setValue(jugador)
                                    .addOnCompleteListener(task -> {

                                        if (task.isSuccessful()) {



                                            Log.d("DEBUG_SALA", "Jugador creado correctamente");

                                            CodigoSalaSearch codigo = new CodigoSalaSearch(
                                                    codigoSala,
                                                    idSalaString,
                                                    true
                                            );

                                            DatabaseReference nodoCodigoSala = FirebaseDatabase
                                                    .getInstance()
                                                    .getReference("codigosSalas");

                                            nodoCodigoSala.child(codigoSala)
                                                    .setValue(codigo)
                                                    .addOnCompleteListener(taskCodigo -> {

                                                        if (taskCodigo.isSuccessful()) {

                                                            Log.d("DEBUG_SALA", "codigo almacenado en realtime correctamente");
                                                            Log.d("JUGADOR_SALA", "jugador sala cargado correctamente en la realtime");

                                                            abrirLobbyComoAnfitrion(categoriasSeleccionadas);

                                                        } else {

                                                            Log.d("JUGADOR_SALA", "no se pudo crear el nodo codigo sala");

                                                            NotificationHelper.show(
                                                                    notificationCrearSala,
                                                                    "¡No se pudo guardar el código de la sala!",
                                                                    NotificationHelper.ERROR
                                                            );
                                                            Sounds.playNotificaciones();
                                                        }
                                                    });

                                        } else {

                                            Log.d("JUGADOR_SALA", "no se pudo cargar el jugador a la realtime");

                                            NotificationHelper.show(
                                                    notificationCrearSala,
                                                    "¡No se pudo agregar el jugador!",
                                                    NotificationHelper.ERROR
                                            );
                                            Sounds.playNotificaciones();
                                        }
                                    });

                        } else {

                            Log.d("DEBUG_SALA", "no se pudo crear la sala");

                            NotificationHelper.show(
                                    notificationCrearSala,
                                    "¡No se pudo crear la sala!",
                                    NotificationHelper.ERROR
                            );
                            Sounds.playNotificaciones();
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d("DEBUG_SALA", "Error leyendo usuario: " + error.getMessage());

                    NotificationHelper.show(
                            notificationCrearSala,
                            "¡Error leyendo los datos del usuario!",
                            NotificationHelper.ERROR
                    );
                    Sounds.playNotificaciones();
                }
            });

        } else {
            Log.d("DEBUG_SALA", "usuario no verificado");

            NotificationHelper.show(
                    notificationCrearSala,
                    "¡Debes iniciar sesión!",
                    NotificationHelper.WARNING
            );
            Sounds.playNotificaciones();
        }
    }

    private boolean validarConfiguracionSala() {
        ArrayList<String> categoriasSeleccionadas = ObtenerCategoriasActivas();

        if (categoriasSeleccionadas.isEmpty()) {
            NotificationHelper.show(
                    notificationCrearSala,
                    "¡Debes seleccionar al menos una categoría!",
                    NotificationHelper.WARNING
            );
            Sounds.playNotificaciones();
            return false;
        }

        return true;
    }

    private ArrayList<String> ObtenerCategoriasActivas() {
        actualizarCategoriasActivasEnOrden();
        return new ArrayList<>(categoriasActivas);
    }

    private void abrirLobbyComoAnfitrion(ArrayList<String> categoriasSeleccionadas) {
        Intent intent = new Intent(requireContext(), SalaActivity.class);
        Bundle bundle = new Bundle();

        bundle.putBoolean("ES_ANFITRION", true);
        bundle.putStringArrayList("CATEGORIAS", categoriasSeleccionadas);
        bundle.putInt("TIEMPO", obtenerTiempoSeleccionado());
        bundle.putInt("JUGADORES", contadorJugadores);
        bundle.putInt("RONDAS", contadorRondas);
        bundle.putBoolean("CREADOR", true);
        bundle.putString("CODIGO_SALA", codigoSalaLobby);
        bundle.putString("IDSALA", idSalaLobby);
        bundle.putBoolean("ANEXO", false);
        bundle.putString("ESTADO_SALA", estadoPartida);
        bundle.putInt("RONDA_ACTUAL", rondaActual);
        bundle.putString("LETRA", letraElegidabundle);

        intent.putExtra("DATOS_SALA", bundle);

        startActivity(intent);

        JugarFragment jFragment = new JugarFragment();

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, jFragment)
                .addToBackStack(null)
                .commit();
    }

    private void configurarBotonRegresar() {
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getParentFragmentManager().popBackStack();
            }
        });
    }
}
