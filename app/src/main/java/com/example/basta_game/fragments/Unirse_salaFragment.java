package com.example.basta_game.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatButton;
import com.example.basta_game.R;

public class Unirse_salaFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unirse_sala, container, false);

        ImageView btnRegresar = view.findViewById(R.id.btnRegresar);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getParentFragmentManager().popBackStack();
            }
        });

        AppCompatButton btnUnirseSalaFinal = view.findViewById(R.id.btnUnirseSalaFinal);
        btnUnirseSalaFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LobbyFragment lobby = new LobbyFragment();
                Bundle bundle = new Bundle();

                bundle.putBoolean("ES_ANFITRION", false);
                lobby.setArguments(bundle);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, lobby)
                        .addToBackStack(null)
                        .commit();
            }
        });

        return view;
    }
}