package com.example.basta_game.Fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.basta_game.R;
import com.example.basta_game.utils.Animations;
import com.google.android.material.card.MaterialCardView;

public class JugarFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_jugar, container, false);

        MaterialCardView cardCrearSala = view.findViewById(R.id.cardCrearSala);
        MaterialCardView cardUnirseSala = view.findViewById(R.id.cardUnirseSala);

        Animations.animacionAparicion(cardCrearSala);
        Animations.animacionAparicion(cardUnirseSala);

        cardCrearSala.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Crear_salaFragment crearSalaFragment = new Crear_salaFragment();
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, crearSalaFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        cardUnirseSala.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Unirse_salaFragment unirseSalaFragment = new Unirse_salaFragment();

                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, unirseSalaFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        return view;
    }
}