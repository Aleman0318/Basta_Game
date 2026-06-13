package com.example.basta_game.Fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.viewpager2.widget.ViewPager2;
import com.example.basta_game.Adapters.TarjetasValidacionAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.Adapters.CategoriaPartidaAdapter;
import com.example.basta_game.Adapters.JugadorPartidaAdapter;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PartidaFragment extends Fragment {

    private String codigoSala;
    private boolean esAnfitrion;
    private ArrayList<String> listaNombresCategorias;

    private DatabaseReference salaRef;

    private TextView txtCodigoSala;
    private TextView txtRondaActual;
    private TextView txtEstadoPartida;
    private TextView txtTiempoRestante;
    private TextView txtInstruccionLetra;

    private ImageView imgLetraActual;

    private AppCompatButton btnBasta;

    // Declaramos los RecyclerViews
    private RecyclerView rvJugadoresPartida;
    private RecyclerView rvCategoriasJuego;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partida, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        obtenerArgumentos();
        inicializarVistas(view);
        configurarFirebase();
        cargarDatosIniciales();
        configurarBotones();
    }

    private void obtenerArgumentos() {
        if (getArguments() != null) {
            codigoSala = getArguments().getString("CODIGO_SALA");
            esAnfitrion = getArguments().getBoolean("ES_ANFITRION", false);
            listaNombresCategorias = getArguments().getStringArrayList("CATEGORIAS");
        }
    }

    private void inicializarVistas(View view) {
        txtCodigoSala = view.findViewById(R.id.txtCodigoSala);
        txtRondaActual = view.findViewById(R.id.txtRondaActual);
        txtEstadoPartida = view.findViewById(R.id.txtEstadoPartida);
        txtTiempoRestante = view.findViewById(R.id.txtTiempoRestante);
        txtInstruccionLetra = view.findViewById(R.id.txtInstruccionLetra);

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
        txtCodigoSala.setText("Sala " + codigoSala);
        txtEstadoPartida.setText("● En juego");

        // 1. CONFIGURAR JUGADORES (Arriba)
        rvJugadoresPartida.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        List<Jugador> listaJugadores = new ArrayList<>();
        listaJugadores.add(new Jugador("Marlon", false));
        listaJugadores.add(new Jugador("Diego", true));
        listaJugadores.add(new Jugador("Diana", true));
        listaJugadores.add(new Jugador("Steven", true));
        rvJugadoresPartida.setAdapter(new JugadorPartidaAdapter(listaJugadores));

        // 2. CONFIGURAR CATEGORÍAS (Centro - 1 Columna con Scroll)
        // ¡Aquí está el cambio! De GridLayoutManager(..., 2) a LinearLayoutManager normal
        rvCategoriasJuego.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Categoria> listaCategorias = new ArrayList<>();

        // Verificamos si nos pasaron categorías desde el Lobby
        if (listaNombresCategorias != null && !listaNombresCategorias.isEmpty()) {
            for (String nombreCat : listaNombresCategorias) {
                int iconoId = obtenerIconoParaCategoria(nombreCat);
                listaCategorias.add(new Categoria(nombreCat, iconoId));
            }
        } else {
            // Un fallback por si entras a la pantalla sin datos (solo para pruebas)
            listaCategorias.add(new Categoria("Sin categorías", R.drawable.outline_person_24));
        }
        rvCategoriasJuego.setAdapter(new CategoriaPartidaAdapter(listaCategorias));
    }

    private void configurarBotones() {
        btnBasta.setOnClickListener(v -> {
            // Más adelante: guardar respuestas, cambiar estado de ronda...

            List<String> categoriasPrueba = new ArrayList<>(); //cat de prueba
            categoriasPrueba.add("Fruta");
            categoriasPrueba.add("Animal");
            categoriasPrueba.add("Color");

            mostrarModalValidacionCarrusel(categoriasPrueba);
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
}