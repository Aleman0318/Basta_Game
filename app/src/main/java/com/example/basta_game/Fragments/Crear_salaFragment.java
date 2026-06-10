package com.example.basta_game.Fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatButton;
import com.example.basta_game.Adapters.CategoriaAdapter;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.R;
import java.util.ArrayList;
import java.util.List;

public class Crear_salaFragment extends Fragment {

    private int contadorJugadores = 4;
    private int contadorRondas = 1;
    private ArrayList<String> categoriasActivas = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crear_sala, container, false);

        RecyclerView rvCategorias = view.findViewById(R.id.rvCategorias);
        rvCategorias.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<Categoria> misCategorias = new ArrayList<>();
        misCategorias.add(new Categoria("Nombre", R.drawable.ic_nombre));
        misCategorias.add(new Categoria("Color", R.drawable.ic_paleta));
        misCategorias.add(new Categoria("Fruta", R.drawable.ic_fruta));
        misCategorias.add(new Categoria("País", R.drawable.ic_pais));
        misCategorias.add(new Categoria("Animal", R.drawable.ic_animal));
        misCategorias.add(new Categoria("Cosa", R.drawable.ic_cosa));
        misCategorias.add(new Categoria("Apellido", R.drawable.ic_apellido));
        misCategorias.add(new Categoria("Objeto o pastel", R.drawable.outline_person_24));

        categoriasActivas.clear();

        for (Categoria cat : misCategorias) {
            categoriasActivas.add(cat.getNombre());
        }

        CategoriaAdapter adaptador = new CategoriaAdapter(misCategorias, new CategoriaAdapter.OnCategoriaClickListener() {
            @Override
            public void onCategoriaToggle(Categoria categoria, boolean estaHabilitada) {
                if (estaHabilitada) {
                    if (!categoriasActivas.contains(categoria.getNombre())) {
                        categoriasActivas.add(categoria.getNombre());
                    }
                } else {
                    categoriasActivas.remove(categoria.getNombre());
                }
            }
        });
        rvCategorias.setAdapter(adaptador);

        SeekBar sbTiempo = view.findViewById(R.id.sbTiempo);
        final TextView tvTiempoActual = view.findViewById(R.id.tvTiempoActual);

        int segundosIniciales = 60 + (sbTiempo.getProgress() * 10);
        tvTiempoActual.setText(String.format("%d:%02d min", segundosIniciales / 60, segundosIniciales % 60));

        sbTiempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int totalSegundos = 60 + (progress * 10);

                String tiempoFormateado = String.format("%d:%02d min", totalSegundos / 60, totalSegundos % 60);
                tvTiempoActual.setText(tiempoFormateado);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AppCompatButton btnRestarRonda = view.findViewById(R.id.btnRestarRonda);
        AppCompatButton btnSumarRonda = view.findViewById(R.id.btnSumarRonda);
        final TextView tvCantRondas = view.findViewById(R.id.tvCantRondas);

        tvCantRondas.setText(String.valueOf(contadorRondas));

        btnRestarRonda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contadorRondas > 1) {
                    contadorRondas--;
                    tvCantRondas.setText(String.valueOf(contadorRondas));
                }
            }
        });

        btnSumarRonda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contadorRondas < 26) {
                    contadorRondas++;
                    tvCantRondas.setText(String.valueOf(contadorRondas));
                }
            }
        });

        AppCompatButton btnRestar = view.findViewById(R.id.btnRestarJugador);
        AppCompatButton btnSumar = view.findViewById(R.id.btnSumarJugador);
        final TextView tvCantJugadores = view.findViewById(R.id.tvCantJugadores);

        tvCantJugadores.setText(String.valueOf(contadorJugadores));

        btnRestar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contadorJugadores > 2) {
                    contadorJugadores--;
                    tvCantJugadores.setText(String.valueOf(contadorJugadores));
                }
            }
        });

        btnSumar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contadorJugadores < 8) {
                    contadorJugadores++;
                    tvCantJugadores.setText(String.valueOf(contadorJugadores));
                }
            }
        });

        AppCompatButton btnCrearSalaFinal = view.findViewById(R.id.btnCrearSalaFinal);

        btnCrearSalaFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (categoriasActivas.isEmpty()) {
                    Toast.makeText(getContext(), "¡Debes seleccionar al menos una categoría!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String nombresSeleccionados = android.text.TextUtils.join(", ", categoriasActivas);

                LobbyFragment lobby = new LobbyFragment();
                Bundle bundle = new Bundle();

                bundle.putBoolean("ES_ANFITRION", true);
                bundle.putString("CATEGORIAS", nombresSeleccionados);

                bundle.putInt("TIEMPO", 60 + (sbTiempo.getProgress() * 10));
                bundle.putInt("JUGADORES", contadorJugadores);
                bundle.putInt("RONDAS", contadorRondas);

                lobby.setArguments(bundle);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, lobby)
                        .addToBackStack(null)
                        .commit();
            }
        });

        android.widget.ImageView btnRegresar = view.findViewById(R.id.btnRegresar);
        btnRegresar.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
    }
}