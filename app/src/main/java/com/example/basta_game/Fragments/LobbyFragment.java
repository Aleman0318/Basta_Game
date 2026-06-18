package com.example.basta_game.Fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;

import com.example.basta_game.HomeActivity;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import com.example.basta_game.Adapters.JugadorSalaAdapter;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.Sala;
import com.example.basta_game.utils.Animations;
import com.example.basta_game.utils.CustomDialogHelper;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
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
    private Button btnComenzarPartida;

    private Bundle bundleDatosPartida;
    private Boolean yaEntroAPartida = false;

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


        bundleDatosPartida = getArguments();

        auth = FirebaseAuth.getInstance();

        if (bundleDatosPartida != null) {
            esAnfitrion = bundleDatosPartida.getBoolean("ES_ANFITRION", false);
            categoriasStr = bundleDatosPartida.getStringArrayList("CATEGORIAS");
            tiempoStr = bundleDatosPartida.getInt("TIEMPO", 60);
            jugadoresStr = bundleDatosPartida.getInt("JUGADORES", 4);
            rondasStr = bundleDatosPartida.getInt("RONDAS", 5);
            esCreador = bundleDatosPartida.getBoolean("CREADOR", false);
            codigoSala = bundleDatosPartida.getString("CODIGO_SALA", "Esperando...");
            idSala = bundleDatosPartida.getString("IDSALA");

            esAnexo = bundleDatosPartida.getBoolean("ANEXO", false);

            ObtenerEstadoPartida(auth, idSala);
        }


        String tiempoFormateado = String.format("%d:%02d min", tiempoStr / 60, tiempoStr % 60);

        String categoriasFormateadas = android.text.TextUtils.join(", ", categoriasStr);

        idSalaMetodo = idSala;

        tvValorCategoriasLobby = view.findViewById(R.id.tvValorCategoriasLobby);
        tvValorDuracionLobby = view.findViewById(R.id.tvValorDuracionLobby);
        tvValorJugadoresLobby = view.findViewById(R.id.tvValorJugadoresLobby);
        tvValorRondasLobby = view.findViewById(R.id.tvValorRondasLobby);
        btnComenzarPartida = view.findViewById(R.id.btnComenzarPartida);

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

        Animations.animacionAparicion(btnComenzarPartida);
        Animations.animacionAparicion(btnToggleListo);

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
            Sounds.playNotificaciones();
        }

        if(esAnexo){
            NotificationHelper.show(
                    notificationLobby,
                    "Te has unido a la sala correctamente",
                    NotificationHelper.SUCCESS
            );
            Sounds.playNotificaciones();
        }

        if (esAnfitrion) {
            btnComenzarPartida.setVisibility(View.VISIBLE);
        } else {
            btnComenzarPartida.setVisibility(View.GONE);
        }

        btnRegresar.setOnClickListener(v -> {
            CustomDialogHelper.show(
                    requireContext(),
                    "¿Deseas salir?",
                    "Si sales de la sala abandonarás el lobby actual.",
                    true,
                    new CustomDialogHelper.DialogButton(
                            "No",
                            Color.parseColor("#888888"),
                            Color.WHITE,
                            true,
                            null
                    ),
                    new CustomDialogHelper.DialogButton(
                            "Sí, salir",
                            Color.parseColor("#E53935"),
                            Color.WHITE,
                            true,
                            () -> ((SalaActivity) requireActivity()).mandarHomePorDesconexion()
                    )
            );
        });

        btnComenzarPartida.setOnClickListener(v -> {
            if(VerificacionesAuth.VerificarInicioSesion(auth)){
                DatabaseReference nodoJugadores = FirebaseDatabase.getInstance().getReference("salas").child(idSalaMetodo).child("jugadores");
                nodoJugadores.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int contadorListos = 0;
                        ArrayList<JugadorSala> jugadores = new ArrayList<>();
                        for (DataSnapshot hijo: snapshot.getChildren()) {
                            JugadorSala player = hijo.getValue(JugadorSala.class);
                            if(hijo.exists()){
                                if(player.isEstaListo()){
                                    jugadores.add(player);
                                }
                                contadorListos ++;
                            }
                        }

                        if(jugadores.size() < contadorListos){
                            NotificationHelper.show(
                                    notificationLobby,
                                    "Todos los jugadores deben estar listos",
                                    NotificationHelper.ERROR
                            );
                        }else{


                                DatabaseReference nodoEstado = FirebaseDatabase.getInstance()
                                        .getReference("salas")
                                        .child(idSalaMetodo)
                                        .child("estadoPartida");
                            //abrimos la partida e insertamos el tiempo de inicio de la ronda
                            DatabaseReference nodoEstado = FirebaseDatabase.getInstance()
                                    .getReference("salas")
                                    .child(idSalaMetodo)
                                    .child("estadoPartida");

                                nodoEstado.setValue("En juego");
                            nodoEstado.setValue("En juego");

                            DatabaseReference nodoSala = FirebaseDatabase.getInstance().getReference("salas").child(idSalaMetodo).child("bastaPresionado");
                            nodoSala.setValue(false);

                            DatabaseReference nodoTimeStamp = FirebaseDatabase.getInstance().getReference("salas").child(idSalaMetodo).child("bastaTimeStamp");
                            nodoTimeStamp.setValue(null);

                            DatabaseReference nodoTiempoInicio = FirebaseDatabase.getInstance()
                                    .getReference("salas").child(idSalaMetodo)
                                    .child("inicioRonda");

                            nodoTiempoInicio.setValue(ServerValue.TIMESTAMP);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        });


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
                                       Sounds.playNotificaciones();
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
                                        Sounds.playNotificaciones();
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

    public void ObtenerEstadoPartida(FirebaseAuth auth, String salaID){
        if(VerificacionesAuth.VerificarInicioSesion(auth)){
            DatabaseReference nodoEstado = FirebaseDatabase.getInstance().getReference("salas").child(salaID).child("estadoPartida");
            nodoEstado.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(yaEntroAPartida){
                        Log.d("OBTENER","ENTRO AL RETURN DE YA ENTRO A PARTIDA");
                        return;
                    }else{
                        Log.d("OBTENER","NO ENTRO AL RETURN DE YA ENTRO A PARTIDA");
                        String estado = snapshot.getValue(String.class);
                        Log.d("OBTENER","EL VALOR DE LA SALA ES: " + estado);
                        if("En juego".equals(estado)){
                            Log.d("OBTENER","ENTRO AL IF DE CREAR PARTIDA");
                            PartidaFragment partida = new PartidaFragment();
                            partida.setArguments(bundleDatosPartida);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.sala_fragment_container, partida)
                                    .commit();
                            yaEntroAPartida = true;
                            Log.d("OBTENER","SE CREO LA PARTIDA A PARTIDA");
                        }else{
                            Log.d("OBTENER","NO ENTRO AL IF DE CREAR PARTIDA PARTIDA");
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }


}