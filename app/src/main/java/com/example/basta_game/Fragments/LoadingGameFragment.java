package com.example.basta_game.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.airbnb.lottie.LottieAnimationView;

import com.example.basta_game.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jspecify.annotations.NonNull;


public class LoadingGameFragment extends Fragment {

    private LottieAnimationView lottieLoading;
    private DatabaseReference estadoRef;
    private ValueEventListener estadoListener;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_loading_game,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        lottieLoading = view.findViewById(R.id.lottieLoading);
        lottieLoading.setSpeed(0.6f);

        Bundle datos = getArguments();

        if (datos == null) {
            return;
        }

        String salaID = datos.getString("idSala");

        if (salaID == null) {
            return;
        }

        estadoRef = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(salaID)
                .child("estadoPartida");

        estadoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String estado = snapshot.getValue(String.class);

                if ("En juego".equals(estado)) {

                    PartidaFragment partida =
                            new PartidaFragment();

                    partida.setArguments(datos);

                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(
                                    R.id.sala_fragment_container,
                                    partida
                            )
                            .commit();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        estadoRef.addValueEventListener(estadoListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (estadoRef != null &&
                estadoListener != null) {

            estadoRef.removeEventListener(
                    estadoListener
            );
        }
    }
}
