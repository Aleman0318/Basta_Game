package com.example.basta_game.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.HomeActivity;
import com.example.basta_game.R;
import com.example.basta_game.Adapters.PuntuacionAdapter;
import com.example.basta_game.models.PuntuacionDummy;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class PuntuacionesFragment extends Fragment {

    private RecyclerView rvTablaPuntuaciones;
    private MaterialButton btnVolverInicio;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_puntuaciones, container, false);

        rvTablaPuntuaciones = view.findViewById(R.id.rvTablaPuntuaciones);
        btnVolverInicio = view.findViewById(R.id.btnVolverInicio);

        rvTablaPuntuaciones.setLayoutManager(new LinearLayoutManager(getContext()));

        List<PuntuacionDummy> datosPrueba = new ArrayList<>();
        datosPrueba.add(new PuntuacionDummy(1, "JugadorDaigo", 45, 50, 30, 60, 10));
        datosPrueba.add(new PuntuacionDummy(2, "Aleman0318", 40, 45, 30, 50, 20));
        datosPrueba.add(new PuntuacionDummy(3, "DianaEspinal", 50, 30, 40, 20, 30));
        datosPrueba.add(new PuntuacionDummy(4, "StevenGT", 30, 30, 30, 30, 40));
        datosPrueba.add(new PuntuacionDummy(5, "Marlon123", 20, 10, 50, 40, 30));
        datosPrueba.add(new PuntuacionDummy(6, "JugadorX", 10, 20, 10, 30, 50));
        datosPrueba.add(new PuntuacionDummy(6, "Ariana", 10, 20, 10, 30, 50));
        datosPrueba.add(new PuntuacionDummy(6, "Ange", 10, 20, 10, 30, 50));

        PuntuacionAdapter adapter = new PuntuacionAdapter(datosPrueba);
        rvTablaPuntuaciones.setAdapter(adapter);

        btnVolverInicio.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}