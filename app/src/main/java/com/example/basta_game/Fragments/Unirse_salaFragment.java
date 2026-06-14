package com.example.basta_game.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatButton;
import com.example.basta_game.R;
import com.example.basta_game.SalaActivity;
import com.example.basta_game.models.CodigoSalaSearch;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.Sala;
import com.example.basta_game.models.Usuario;
import com.example.basta_game.utils.Animations;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Unirse_salaFragment extends Fragment {

    private EditText etCodigoSala;
    private FirebaseAuth auth;

    private CodigoSalaSearch codigoElegidoFiltrado;
    private View unirseSalaNotification;

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

        etCodigoSala = view.findViewById(R.id.etSalaId);
        unirseSalaNotification = view.findViewById(R.id.notificacionUnirseSala);
        auth = FirebaseAuth.getInstance();

        AppCompatButton btnUnirseSalaFinal = view.findViewById(R.id.btnUnirseSalaFinal);

        Animations.animacionAparicion(etCodigoSala);
        Animations.animacionAparicion(btnUnirseSalaFinal);

        btnUnirseSalaFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(auth != null && VerificacionesAuth.VerificarInicioSesion(auth)){
                    String codigoSalaIngresado = etCodigoSala.getText().toString();

                    DatabaseReference nodoSalas = FirebaseDatabase.getInstance().getReference("codigosSalas");
                    nodoSalas.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String codigoSalaUsuario = etCodigoSala.getText().toString();
                            boolean encontrado = false;
                            for (DataSnapshot hijo: snapshot.getChildren()) {
                                if(hijo!=null){
                                    CodigoSalaSearch codigoElegido = hijo.getValue(CodigoSalaSearch.class);
                                    if(codigoElegido.getCodigoSala().equals(codigoSalaUsuario) && codigoElegido.getEstaActiva()){
                                        encontrado = true;
                                        codigoElegidoFiltrado = codigoElegido;

                                    }
                                }
                            }

                            if(!encontrado){
                                NotificationHelper.show(
                                        unirseSalaNotification,
                                        "No se encontro la sala requerida",
                                        NotificationHelper.ERROR
                                );
                            }else{
                                DatabaseReference nodoSala = FirebaseDatabase.getInstance().getReference("salas").child(codigoElegidoFiltrado.getIdSala());
                                nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        Sala sala = snapshot.getValue(Sala.class);

                                        DatabaseReference nodoJugadoresSala = FirebaseDatabase.getInstance().getReference("salas").child(codigoElegidoFiltrado.getIdSala()).child("jugadores");
                                        nodoJugadoresSala.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                ArrayList<JugadorSala> jugadoresEnSala = new ArrayList<>();

                                                for (DataSnapshot jugadorSnapshot : snapshot.getChildren()) {
                                                    JugadorSala jugadorSala = jugadorSnapshot.getValue(JugadorSala.class);

                                                    if (jugadorSala != null) {
                                                        jugadoresEnSala.add(jugadorSala);
                                                    }
                                                }

                                                if (jugadoresEnSala.size() < sala.getMaxJugadores()) {
                                                    String uidUsuario = auth.getCurrentUser().getUid();
                                                    DatabaseReference nodoUsuarios = FirebaseDatabase.getInstance().getReference("usuarios").child(uidUsuario);
                                                    nodoUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            Usuario user = snapshot.getValue(Usuario.class);

                                                            JugadorSala jugador = new JugadorSala(
                                                                    user.getUid(),
                                                                    user.getNombre(),
                                                                    0,
                                                                    false,
                                                                    false,
                                                                    true,
                                                                    user.getFotoPerfilUrl()
                                                            );

                                                            DatabaseReference nodoJugadorSala = FirebaseDatabase.getInstance().getReference("salas").child(codigoElegidoFiltrado.getIdSala()).child("jugadores").child(user.getUid());
                                                            nodoJugadorSala.setValue(jugador).addOnCompleteListener(taskJugadorSala -> {
                                                                if(taskJugadorSala.isSuccessful()){

                                                                    nodoJugadorSala.onDisconnect().removeValue();

                                                                    Intent intent = new Intent(requireContext(), SalaActivity.class);
                                                                    Bundle bundle = new Bundle();

                                                                    bundle.putBoolean("ES_ANFITRION", false);
                                                                    bundle.putStringArrayList("CATEGORIAS", sala.getCategoriasActivas());
                                                                    bundle.putInt("TIEMPO", sala.getTiempoRonda());
                                                                    bundle.putInt("JUGADORES", sala.getMaxJugadores());
                                                                    bundle.putInt("RONDAS", sala.getCantidadRondas());
                                                                    bundle.putBoolean("CREADOR", false);
                                                                    bundle.putBoolean("ANEXO", true);
                                                                    bundle.putString("CODIGO_SALA", sala.getCodigoSala());
                                                                    bundle.putString("IDSALA", sala.getIdSala());

                                                                    intent.putExtra("DATOS_SALA", bundle);

                                                                    startActivity(intent);
                                                                }
                                                            });
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                                } else {
                                                    NotificationHelper.show(
                                                            unirseSalaNotification,
                                                            "La sala estÃ¡ llena",
                                                            NotificationHelper.ERROR
                                                    );
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

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
}