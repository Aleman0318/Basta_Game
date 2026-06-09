package com.example.basta_game.fragments;

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
import com.example.basta_game.adapters.CategoriaAdapter;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.R;
import java.util.ArrayList;
import java.util.List;

public class Crear_salaFragment extends Fragment {

    private int contadorJugadores = 4;
    private ArrayList<String> categoriasActivas = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crear_sala, container, false);

        RecyclerView rvCategorias = view.findViewById(R.id.rvCategorias);
        rvCategorias.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<Categoria> misCategorias = new ArrayList<>();
        misCategorias.add(new Categoria("Nombre", R.drawable.outline_person_24));
        misCategorias.add(new Categoria("Color", R.drawable.outline_person_24));
        misCategorias.add(new Categoria("Fruta", R.drawable.outline_person_24));
        misCategorias.add(new Categoria("País", R.drawable.outline_person_24));
        misCategorias.add(new Categoria("Animal", R.drawable.outline_person_24));
        misCategorias.add(new Categoria("Cosa", R.drawable.outline_person_24));
        misCategorias.add(new Categoria("Apellido", R.drawable.outline_person_24));
        misCategorias.add(new Categoria("Objeto o pastel", R.drawable.outline_person_24));

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

        tvTiempoActual.setText(sbTiempo.getProgress() + "s");

        sbTiempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvTiempoActual.setText(progress + "s");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
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
                String nombresSeleccionados = android.text.TextUtils.join(", ", categoriasActivas);

                if (categoriasActivas.isEmpty()) {
                    Toast.makeText(getContext(), "¡Debes seleccionar al menos una categoría!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Categorías: " + nombresSeleccionados, Toast.LENGTH_LONG).show();
                }
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