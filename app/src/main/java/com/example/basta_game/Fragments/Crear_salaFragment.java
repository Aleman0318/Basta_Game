package com.example.basta_game.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.ArrayList;
import java.util.List;

//imports firebase
import com.example.basta_game.models.CodigoSalaSearch;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.Sala;
import com.example.basta_game.models.Usuario;
import com.example.basta_game.utils.CodigoSala;
import com.example.basta_game.utils.LetraSala;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Crear_salaFragment extends Fragment {

    private int contadorJugadores = 2;
    private int contadorRondas = 1;

    private final ArrayList<String> categoriasActivas = new ArrayList<>();

    private RecyclerView rvCategorias;
    private SeekBar sbTiempo;

    private TextView tvTiempoActual;
    private TextView tvCantRondas;
    private TextView tvCantJugadores;

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

    private String estadoSala;

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
    }

    private void configurarCategorias() {
        rvCategorias.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<Categoria> misCategorias = new ArrayList<>();
        misCategorias.add(new Categoria("Nombre", R.drawable.ic_nombre));
        misCategorias.add(new Categoria("Color", R.drawable.ic_paleta2));
        misCategorias.add(new Categoria("Fruta", R.drawable.ic_frutas2));
        misCategorias.add(new Categoria("País", R.drawable.ic_pais2));
        misCategorias.add(new Categoria("Animal", R.drawable.ic_animal3));
        misCategorias.add(new Categoria("Cosa", R.drawable.ic_cosa));
        misCategorias.add(new Categoria("Apellido", R.drawable.ic_mujer));

        categoriasActivas.clear();

        for (Categoria categoria : misCategorias) {
            categoriasActivas.add(categoria.getNombre());
        }

        CategoriaAdapter adaptador = new CategoriaAdapter(misCategorias, new CategoriaAdapter.OnCategoriaClickListener() {
            @Override
            public void onCategoriaToggle(Categoria categoria, boolean estaHabilitada) {
                actualizarCategoriaActiva(categoria, estaHabilitada);
            }
        });

        rvCategorias.setAdapter(adaptador);
    }

    private void actualizarCategoriaActiva(Categoria categoria, boolean estaHabilitada) {
        String nombreCategoria = categoria.getNombre();

        if (estaHabilitada) {
            if (!categoriasActivas.contains(nombreCategoria)) {
                categoriasActivas.add(nombreCategoria);
            }
        } else {
            categoriasActivas.remove(nombreCategoria);
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

    private int ObtenerRondasMaximas(){
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

    private int ObtenerJugadoresMaximos(){
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

        if(auth != null && VerificacionesAuth.VerificarInicioSesion(auth)){
            if (!validarConfiguracionSala()) {
                return;
            }
            Log.d("DEBUG_SALA", "Sesion validada con metodo verificarsesion");
            String usuarioUID = auth.getCurrentUser().getUid();
            DatabaseReference db = FirebaseDatabase.getInstance().getReference("usuarios").child(usuarioUID);

            db.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Usuario user = snapshot.getValue(Usuario.class);
                    String nombreUsuario = user.getNombre();
                    String urlAvatar = user.getFotoPerfilUrl();
                    String estadoSala = "En Espera";
                    String letraElegida = LetraSala.ElegirLetraAlAzar();
                    String codigoSala = CodigoSala.GenerarCodigoSala();
                    codigoSalaLobby = codigoSala;
                    int rondaActual  = 1;
                    long fechaCreacion = System.currentTimeMillis();
                    int jugadoresMaximos = ObtenerJugadoresMaximos();
                    int rondasMaximas = ObtenerRondasMaximas();
                    int tiempoRonda = obtenerTiempoSeleccionado();
                    ArrayList<String> categoriasSeleccionadas = ObtenerCategoriasActivas();

                    DatabaseReference nodoSala = FirebaseDatabase.getInstance().getReference("salas");
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
                            categoriasActivas
                    );

                    salaConID.setValue(sala).addOnCompleteListener(taskSala -> {
                       if(taskSala.isSuccessful()){

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
                                   .setValue(jugador).addOnCompleteListener(task -> {
                                       if(task.isSuccessful()){

                                           salaConID.child("jugadores").child(usuarioUID).onDisconnect().removeValue();

                                           Log.d("DEBUG_SALA", "Jugador creado correctamente");

                                           CodigoSalaSearch codigo = new CodigoSalaSearch(
                                                   codigoSala,
                                                   idSalaString,
                                                   true
                                           );

                                            DatabaseReference nodoCodigoSala = FirebaseDatabase.getInstance().getReference("codigosSalas");
                                            nodoCodigoSala.child(codigoSala).setValue(codigo).addOnCompleteListener(taskCodigo ->{
                                                if(taskCodigo.isSuccessful()){
                                                    Log.d("DEBUG_SALA", "codigo almacenado en realtime correctamente");
                                                    Log.d("JUGADOR_SALA", "jugador sala cargado correctamente en la realtime");
                                                    abrirLobbyComoAnfitrion(categoriasActivas);
                                                }
                                                else{
                                                    Log.d("JUGADOR_SALA", "no se pudo crear el nodo codigo sala");
                                                }
                                            });
                                       }
                                       else{
                                           Log.d("JUGADOR_SALA", "no se pudo cargar el jugador a la realtime");
                                       }
                                   });
                       }
                       else {
                           Log.d("DEBUG_SALA", "no se pudo crear la sala");
                           NotificationHelper.show(
                                   notificationCrearSala,
                                   "¡No se pudo crear la sala!",
                                   NotificationHelper.ERROR
                           );
                       }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }else{
            Log.d("DEBUG_SALA", "usuario no verificado");
        }
    }

    private boolean validarConfiguracionSala() {
        if (categoriasActivas.isEmpty()) {
            NotificationHelper.show(
                notificationCrearSala,
        "¡Debes seleccionar al menos una categoría!",
                 NotificationHelper.WARNING
            );
            return false;
        }

        return true;
    }

    private ArrayList<String> ObtenerCategoriasActivas(){
        return categoriasActivas;
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
        bundle.putString("CODIGO_SALA",codigoSalaLobby);
        bundle.putString("IDSALA", idSalaLobby);
        bundle.putBoolean("ANEXO", true); //indica si se unio a la partida

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