package com.example.basta_game.Fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import com.example.basta_game.Adapters.JugadorSalaAdapter;
import com.example.basta_game.Adapters.TarjetasValidacionAdapter;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.Sala;
import com.example.basta_game.utils.CustomDialogHelper;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private MaterialButton btnAbandonar;

    private ArrayList<String> listaNombresCategorias;

    private DatabaseReference salaRef;

    private TextView txtCodigoSala;
    private TextView txtRondaActual;
    private TextView txtEstadoPartida;
    private TextView txtTiempoRestante;
    private String letraElegida;
    private TextView txtInstruccionLetra;

    private ImageView imgLetraActual;

    private AppCompatButton btnBasta;

    // Declaramos los RecyclerViews
    private RecyclerView rvJugadoresPartida;
    private RecyclerView rvCategoriasJuego;

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

        // Enlazamos los RecyclerViews
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
        mostrarLetra(letraElegida);

        rvJugadoresPartida.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));


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
        rvCategoriasJuego.setAdapter(new CategoriaPartidaAdapter(listaCategorias));
    }

    private void configurarBotones() {
        btnBasta.setOnClickListener(v -> {
            List<String> categoriasPrueba = new ArrayList<>();
            categoriasPrueba.add("Fruta");
            categoriasPrueba.add("Animal");
            categoriasPrueba.add("Color");

           mostrarModalValidacionCarrusel(categoriasPrueba);
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
        int idDrawable = getResources().getIdentifier(nombreDrawable, "drawable", requireContext().getPackageName());

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

    public void CambiarEstadoPartida(FirebaseAuth auth, String idSala){

        if(VerificacionesAuth.VerificarInicioSesion(auth)){
            DatabaseReference nodoSala = FirebaseDatabase.getInstance().getReference("salas").child(idSala).child("estadoPartida");
            nodoSala.setValue("En juego").addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    Log.d("ESTADO", "se cambio el estado de la partida correctamente");
                }
            });
        }
    }



    public void ObtenerEstadoPartida(FirebaseAuth auth, String salaID){
        if(VerificacionesAuth.VerificarInicioSesion(auth)){
            DatabaseReference nodoEstado = FirebaseDatabase.getInstance().getReference("salas").child(salaID);
            nodoEstado.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Sala sala = snapshot.getValue(Sala.class);
                    if(sala != null){
                        txtEstadoPartida.setText("● " + sala.getEstadoPartida());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    public void ObtenerJugadoresYLlenarPartida(String idSala){
        auth = FirebaseAuth.getInstance();
        if(VerificacionesAuth.VerificarInicioSesion(auth)){
            DatabaseReference nodoSalas = FirebaseDatabase.getInstance().getReference("salas").child(idSala).child("jugadores");
            nodoSalas.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ArrayList<Jugador> jugadoresActivos = new ArrayList<>();

                    for (DataSnapshot hijo : snapshot.getChildren()) {
                        JugadorSala jugador = hijo.getValue(JugadorSala.class);

                        if (jugador != null) {
                            jugadoresActivos.add(new Jugador(jugador.getNombre(), jugador.isEstaListo(), jugador.getUrlAvatar()));
                        }
                    }

                    if(!jugadoresActivos.isEmpty()){
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

    private void mostrarModalValidacionCarrusel(List<String> categorias) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialogo_respuestas);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ViewPager2 vpCategorias = dialog.findViewById(R.id.vpCategoriasValidacion);
        androidx.appcompat.widget.AppCompatButton btnSalir = dialog.findViewById(R.id.btnSalirRevision);

        btnSalir.setOnClickListener(v -> dialog.dismiss());

        Runnable accionMostrarBotonSalir = () -> {
            btnSalir.setVisibility(View.VISIBLE);
        };

        TarjetasValidacionAdapter adapter = new TarjetasValidacionAdapter(categorias, vpCategorias, accionMostrarBotonSalir);
        vpCategorias.setAdapter(adapter);

        vpCategorias.setOffscreenPageLimit(1);

        dialog.setCancelable(false);
        dialog.show();
    }

    private void cambiarEstadoUsuarios(){
        Log.d("CAMBIAR", "Voy a poner estaListo=false en sala=" + idSala + " uid=" + auth.getCurrentUser().getUid());
        if(VerificacionesAuth.VerificarInicioSesion(auth)){
            DatabaseReference nodoEstadoUsuario = FirebaseDatabase.getInstance().getReference("salas").child(idSala).child("jugadores").child(auth.getCurrentUser().getUid()).child("estaListo");
            nodoEstadoUsuario.setValue(false).addOnCompleteListener(task -> {
               if(task.isSuccessful()){
                   Log.d("CAMBIAR", "ESTADO DE USUARIO CAMBIADO A DESACTIVADO");
               }
            });
        }
    }

}