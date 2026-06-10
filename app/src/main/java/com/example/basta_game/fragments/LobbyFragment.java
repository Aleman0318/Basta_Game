package com.example.basta_game.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import com.example.basta_game.Models.Jugador;
import com.example.basta_game.R;
import com.example.basta_game.adapters.JugadorAdapter;
import java.util.ArrayList;
import java.util.List;

public class LobbyFragment extends Fragment {

    private boolean esAnfitrion = false;
    private boolean estoyListo = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lobby, container, false);

        String categoriasStr = "Cargando...";
        int tiempoStr = 60;
        int jugadoresStr = 4;

        if (getArguments() != null) {
            esAnfitrion = getArguments().getBoolean("ES_ANFITRION", false);
            categoriasStr = getArguments().getString("CATEGORIAS", "Pendiente...");
            tiempoStr = getArguments().getInt("TIEMPO", 60);
            jugadoresStr = getArguments().getInt("JUGADORES", 4);
        }

        TextView tvLobbyReglas = view.findViewById(R.id.tvLobbyReglas);
        tvLobbyReglas.setText("• Categorías: " + categoriasStr + "\n• Tiempo: " + tiempoStr + "s por ronda\n• Jugadores: " + jugadoresStr);

        RecyclerView rvJugadoresLobby = view.findViewById(R.id.rvJugadoresLobby);
        rvJugadoresLobby.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Jugador> listaFicticia = new ArrayList<>();
        listaFicticia.add(new Jugador("Diego", false));
        listaFicticia.add(new Jugador("Carlos_99", true));
        listaFicticia.add(new Jugador("AnaBanana", false));

        JugadorAdapter adaptador = new JugadorAdapter(listaFicticia);
        rvJugadoresLobby.setAdapter(adaptador);

        ImageView btnRegresar = view.findViewById(R.id.btnRegresar);
        AppCompatButton btnToggleListo = view.findViewById(R.id.btnToggleListo);
        AppCompatButton btnComenzarPartida = view.findViewById(R.id.btnComenzarPartida);

        if (esAnfitrion) {
            btnComenzarPartida.setVisibility(View.VISIBLE);
        } else {
            btnComenzarPartida.setVisibility(View.GONE);
        }

        btnRegresar.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnToggleListo.setOnClickListener(v -> {
            estoyListo = !estoyListo;
            if (estoyListo) {
                btnToggleListo.setText("Estoy listo");
                btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            } else {
                btnToggleListo.setText("No estoy listo");
                btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
            }
        });

        return view;
    }
}