package com.example.basta_game.Fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.basta_game.Adapters.HistorialAdapter;
import com.example.basta_game.R;

public class RankingFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ranking, container, false);

        RecyclerView rvHistorial = view.findViewById(R.id.rvHistorial);

        //tarjetas vertical
        rvHistorial.setLayoutManager(new LinearLayoutManager(getContext()));

        // Se asigna el adaptador de prueba
        HistorialAdapter adapter = new HistorialAdapter();
        rvHistorial.setAdapter(adapter);

        return view;
    }
}