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

    private com.example.basta_game.utils.ListaContenido rvCategorias;
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
    private android.widget.RadioButton rbSalaPublica;

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

    private java.util.Map<String,Object> crearAnfitrionPendiente(String uid,String nombre,String foto) {
        java.util.Map<String,Object> j=new java.util.HashMap<>();
        j.put("uid",uid);j.put("nombre",nombre);j.put("urlAvatar",foto);j.put("puntajePartida",0);
        j.put("esHost",true);j.put("estaListo",false);j.put("estaConectado",false);
        j.put("desconectadoDesde",com.google.firebase.database.ServerValue.TIMESTAMP);return j;
    }
    private void ajustarDistribucion(View vista) {
        if(!(vista instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup grupo=(android.view.ViewGroup)vista;
        boolean estrecho=getResources().getConfiguration().screenWidthDp<300 || (getResources().getConfiguration().screenWidthDp<360 && getResources().getConfiguration().fontScale>1.8f);
        if(estrecho && grupo instanceof android.widget.LinearLayout && grupo.getChildCount()==2 &&
                grupo.getChildAt(0) instanceof com.google.android.material.card.MaterialCardView &&
                grupo.getChildAt(1) instanceof com.google.android.material.card.MaterialCardView) {
            ((android.widget.LinearLayout)grupo).setOrientation(android.widget.LinearLayout.VERTICAL);
            for(int j=0;j<2;j++) {
                android.widget.LinearLayout.LayoutParams lp=(android.widget.LinearLayout.LayoutParams)grupo.getChildAt(j).getLayoutParams();
                lp.width=android.view.ViewGroup.LayoutParams.MATCH_PARENT;lp.weight=0;
                lp.topMargin=Math.round(8*getResources().getDisplayMetrics().density);grupo.getChildAt(j).setLayoutParams(lp);
            }
        }
        for(int j=0;j<grupo.getChildCount();j++) ajustarDistribucion(grupo.getChildAt(j));
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
        rbSalaPublica = view.findViewById(R.id.rbSalaPublica);
        ajustarDistribucion(view);

        Animations.animacionAparicion(btnCrearSalaFinal);
    }

    private void configurarCategorias() {
        rvCategorias.setColumnas(getResources().getConfiguration().screenWidthDp<360 || getResources().getConfiguration().fontScale>1.25f ? 1 : 2);

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
        rvCategorias.post(() -> rvCategorias.requestLayout());
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
        if (contadorRondas < 27) {
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
        if(!com.example.basta_game.utils.ConexionJuego.disponible()) return;

        Log.d("DEBUG_SALA", "Entrando a funcion sala");

        auth = FirebaseAuth.getInstance();

        if (auth != null && VerificacionesAuth.VerificarInicioSesion(auth)) {

            if (!validarConfiguracionSala()) {
                return;
            }

            Log.d("DEBUG_SALA", "Sesion validada con metodo verificarsesion");

            if(!com.example.basta_game.utils.CargaBoton.iniciar(btnCrearSalaFinal))return;
        final Object operacion=com.example.basta_game.utils.CargaBoton.token(btnCrearSalaFinal);
            String usuarioUID = auth.getCurrentUser().getUid();

            DatabaseReference db = FirebaseDatabase
                    .getInstance()
                    .getReference("usuarios")
                    .child(usuarioUID);

            db.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if(!isAdded() || getView()==null || !com.example.basta_game.utils.CargaBoton.activa(btnCrearSalaFinal,operacion))return;
                    Usuario user = snapshot.getValue(Usuario.class);

                    if (user == null || !com.example.basta_game.utils.IdentidadJugador.nombreValido(user.getNombre())) {
                        NotificationHelper.show(
                                notificationCrearSala,
                                "¡No se pudo obtener el usuario!",
                                NotificationHelper.ERROR
                        );
                        Sounds.playNotificaciones();
                        com.example.basta_game.utils.CargaBoton.terminar(btnCrearSalaFinal,operacion);
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

                    java.util.Map<String,Object> datos=new java.util.HashMap<>();
                    final boolean esPublica = rbSalaPublica != null && rbSalaPublica.isChecked();
                    datos.put("publica", esPublica);
                    datos.put("idSala",idSalaString);datos.put("codigoSala",codigoSala);
                    datos.put("hostUID",usuarioUID);datos.put("hostNombre",nombreUsuario);
                    datos.put("estadoPartida",estadoSala);datos.put("letraActual",letraElegida);
                    datos.put("rondaActual",rondaActual);datos.put("cantidadRondas",rondasMaximas);
                    datos.put("fechaCreacion",com.google.firebase.database.ServerValue.TIMESTAMP);
                    datos.put("maxJugadores",jugadoresMaximos);datos.put("tiempoRonda",tiempoRonda);
                    datos.put("categoriasActivas",categoriasSeleccionadas);
                    datos.put("jugadores",java.util.Collections.singletonMap(usuarioUID,crearAnfitrionPendiente(usuarioUID,nombreUsuario,urlAvatar)));
                    btnCrearSalaFinal.setEnabled(false);
                    com.example.basta_game.utils.CreacionSala.crear(FirebaseDatabase.getInstance().getReference("codigosSalas").child(codigoSala),
                            salaConID,datos,ok -> {
                                if(!isAdded() || getView()==null) return;
                                if(!com.example.basta_game.utils.CargaBoton.terminar(btnCrearSalaFinal,operacion))return;
                                if (ok) {
                                    if (esPublica) {
                                        java.util.Map<String, Object> ficha = new java.util.HashMap<>();
                                        ficha.put("idSala", idSalaString);
                                        ficha.put("codigoSala", codigoSala);
                                        ficha.put("hostNombre", nombreUsuario);
                                        ficha.put("maxJugadores", jugadoresMaximos);
                                        ficha.put("fechaCreacion", com.google.firebase.database.ServerValue.TIMESTAMP);
                                        FirebaseDatabase.getInstance().getReference("salasPublicas")
                                                .child(idSalaString).setValue(ficha).addOnCompleteListener(registro -> {
                                                    if (!isAdded()) return;
                                                    if (!registro.isSuccessful()) {
                                                        NotificationHelper.showGlobal("Sala creada, pero no se pudo publicar en el listado. Revisa permisos de Firebase.", NotificationHelper.WARNING);
                                                    }
                                                    abrirLobbyComoAnfitrion(categoriasSeleccionadas);
                                                });
                                    } else abrirLobbyComoAnfitrion(categoriasSeleccionadas);
                                } else NotificationHelper.show(notificationCrearSala,"No se pudo reservar o crear la sala. Reintenta con otro codigo",NotificationHelper.ERROR);
                            });
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    com.example.basta_game.utils.CargaBoton.terminar(btnCrearSalaFinal,operacion);
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
