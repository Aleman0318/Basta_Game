package com.example.basta_game.Fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import com.example.basta_game.Adapters.JugadorSalaAdapter;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LobbyFragment extends Fragment {

    private boolean esAnfitrion = false;
    private boolean estoyListo = false;
    private View notificationLobby;

    private TextView txtCodigoSala, tvValorCategoriasLobby, tvValorRondasLobby, tvValorDuracionLobby, tvValorJugadoresLobby;

    private FirebaseAuth auth;

    private RecyclerView rvJugadoresLobby;

    private String idSalaMetodo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lobby, container, false);

        ArrayList<String> categoriasStr = new ArrayList<>();
        int tiempoStr = 60;
        int jugadoresStr = 4;
        int rondasStr = 5;
        boolean esCreador = false;
        String codigoSala = "";
        String idSala = "";
        Boolean esAnexo = false;

        if (getArguments() != null) {
            esAnfitrion = getArguments().getBoolean("ES_ANFITRION", false);
            categoriasStr = getArguments().getStringArrayList("CATEGORIAS");
            tiempoStr = getArguments().getInt("TIEMPO", 60);
            jugadoresStr = getArguments().getInt("JUGADORES", 4);
            rondasStr = getArguments().getInt("RONDAS", 5);
            esCreador = getArguments().getBoolean("CREADOR", false);
            codigoSala = getArguments().getString("CODIGO_SALA", "Esperando...");
            idSala = getArguments().getString("IDSALA");
            esAnexo = getArguments().getBoolean("ANEXO", false);
        }



        String tiempoFormateado = String.format("%d:%02d min", tiempoStr / 60, tiempoStr % 60);

        String categoriasFormateadas = android.text.TextUtils.join(", ", categoriasStr);

        idSalaMetodo = idSala;

        tvValorCategoriasLobby = view.findViewById(R.id.tvValorCategoriasLobby);
        tvValorDuracionLobby = view.findViewById(R.id.tvValorDuracionLobby);
        tvValorJugadoresLobby = view.findViewById(R.id.tvValorJugadoresLobby);
        tvValorRondasLobby = view.findViewById(R.id.tvValorRondasLobby);

        tvValorCategoriasLobby.setText(categoriasFormateadas);
        tvValorRondasLobby.setText(String.valueOf(rondasStr));
        tvValorJugadoresLobby.setText("Máximo " + String.valueOf(jugadoresStr));
        tvValorDuracionLobby.setText(tiempoFormateado + " por ronda");



        rvJugadoresLobby = view.findViewById(R.id.rvJugadoresLobby);
        rvJugadoresLobby.setLayoutManager(new LinearLayoutManager(getContext()));

        ObtenerJugadoresYLlenarLobby(idSala);

        ImageView btnRegresar = view.findViewById(R.id.btnRegresar);
        AppCompatButton btnToggleListo = view.findViewById(R.id.btnToggleListo);
        AppCompatButton btnComenzarPartida = view.findViewById(R.id.btnComenzarPartida);
        notificationLobby = view.findViewById(R.id.notificationLobby);
        txtCodigoSala = view.findViewById(R.id.tvidSalaLobby);

        txtCodigoSala.setText(codigoSala);

        //confirmamos valor cuando cargamos la pantalla
        if (estoyListo) {
            btnToggleListo.setText("NO ESTOY LISTO");
            btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
        } else {
            btnToggleListo.setText("ESTOY LISTO");
            btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

        if(esCreador){
            NotificationHelper.show(
                    notificationLobby,
                    "Sala creada Correctamente",
                    NotificationHelper.SUCCESS
            );
        }

        if(esAnexo){
            NotificationHelper.show(
                    notificationLobby,
                    "Te has unido a la sala correctamente",
                    NotificationHelper.SUCCESS
            );
        }

        if (esAnfitrion) {
            btnComenzarPartida.setVisibility(View.VISIBLE);
        } else {
            btnComenzarPartida.setVisibility(View.GONE);
        }

        btnRegresar.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnToggleListo.setOnClickListener(v -> {
            estoyListo = !estoyListo;
            if (estoyListo) {
                if(auth != null && VerificacionesAuth.VerificarInicioSesion(auth)){
                    String uid = auth.getCurrentUser().getUid();

                    DatabaseReference nodoSala = FirebaseDatabase.getInstance().getReference("salas").child(idSalaMetodo).child("jugadores").child(uid);
                    nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            JugadorSala jugador = snapshot.getValue(JugadorSala.class);
                            if(jugador != null){
                                jugador.setEstaListo(true);
                                nodoSala.setValue(jugador).addOnCompleteListener(taskEstado ->{
                                   if(taskEstado.isSuccessful()){
                                       btnToggleListo.setText("NO ESTOY LISTO");
                                       btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
                                   }else{
                                       NotificationHelper.show(
                                               notificationLobby,
                                               "Hubo un error al cambiar de estado",
                                               NotificationHelper.ERROR
                                       );
                                   }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

            } else {
                if(auth != null && VerificacionesAuth.VerificarInicioSesion(auth)){
                    String uid = auth.getCurrentUser().getUid();

                    DatabaseReference nodoSala = FirebaseDatabase.getInstance().getReference("salas").child(idSalaMetodo).child("jugadores").child(uid);
                    nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            JugadorSala jugador = snapshot.getValue(JugadorSala.class);
                            if(jugador != null){
                                jugador.setEstaListo(false);
                                nodoSala.setValue(jugador).addOnCompleteListener(taskEstado ->{
                                    if(taskEstado.isSuccessful()){
                                        btnToggleListo.setText("ESTOY LISTO");
                                        btnToggleListo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                                    }else{
                                        NotificationHelper.show(
                                                notificationLobby,
                                                "Hubo un error al cambiar de estado",
                                                NotificationHelper.ERROR
                                        );
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }
        });

        return view;
    }

    public void ObtenerJugadoresYLlenarLobby(String idSala){
        auth = FirebaseAuth.getInstance();
        if(VerificacionesAuth.VerificarInicioSesion(auth)){
            DatabaseReference nodoSalas = FirebaseDatabase.getInstance().getReference("salas").child(idSala).child("jugadores");
            nodoSalas.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ArrayList<JugadorSala> jugadoresEnSala = new ArrayList<>();

                    for (DataSnapshot hijo : snapshot.getChildren()) {
                        JugadorSala jugador = hijo.getValue(JugadorSala.class);

                        if (jugador != null) {
                            jugadoresEnSala.add(jugador);
                        }
                    }

                    if(!jugadoresEnSala.isEmpty()){
                        JugadorSalaAdapter adaptador = new JugadorSalaAdapter(jugadoresEnSala);
                        rvJugadoresLobby.setAdapter(adaptador);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
}