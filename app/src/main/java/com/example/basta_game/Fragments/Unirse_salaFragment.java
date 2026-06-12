package com.example.basta_game.Fragments;

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
import com.example.basta_game.models.CodigoSalaSearch;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.Sala;
import com.example.basta_game.models.Usuario;
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
        auth = FirebaseAuth.getInstance();

        AppCompatButton btnUnirseSalaFinal = view.findViewById(R.id.btnUnirseSalaFinal);
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
                            for (DataSnapshot hijo: snapshot.getChildren()) {
                                if(hijo!=null){
                                    CodigoSalaSearch codigoElegido = hijo.getValue(CodigoSalaSearch.class);
                                    if(codigoElegido.getCodigoSala().equals(codigoSalaUsuario) && codigoElegido.getEstaActiva()){
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
                                                       true
                                               );

                                               DatabaseReference nodoJugadorSala = FirebaseDatabase.getInstance().getReference("salas").child(codigoElegido.getIdSala()).child("jugadores").child(user.getUid());
                                               nodoJugadorSala.setValue(jugador).addOnCompleteListener(taskJugadorSala -> {
                                                   if(taskJugadorSala.isSuccessful()){
                                                        DatabaseReference nodoSalas = FirebaseDatabase.getInstance().getReference("salas").child(codigoElegido.getIdSala());
                                                        nodoSalas.addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                Sala sala = snapshot.getValue(Sala.class);

                                                                LobbyFragment lobby = new LobbyFragment();
                                                                Bundle bundle = new Bundle();

                                                                bundle.putBoolean("ES_ANFITRION", false);
                                                                bundle.putStringArrayList("CATEGORIAS", sala.getCategoriasActivas());
                                                                bundle.putInt("TIEMPO", sala.getTiempoRonda());
                                                                bundle.putInt("JUGADORES", sala.getMaxJugadores());
                                                                bundle.putInt("RONDAS", sala.getCantidadRondas());
                                                                bundle.putBoolean("CREADOR", false);
                                                                bundle.putBoolean("ANEXO", true); //indica si se unio a la partida
                                                                bundle.putString("CODIGO_SALA", sala.getCodigoSala());
                                                                bundle.putString("IDSALA", sala.getIdSala());

                                                                lobby.setArguments(bundle);

                                                                getParentFragmentManager().beginTransaction()
                                                                        .replace(R.id.fragment_container, lobby)
                                                                        .addToBackStack(null)
                                                                        .commit();

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });
                                                   }
                                               });
                                           }

                                           @Override
                                           public void onCancelled(@NonNull DatabaseError error) {

                                           }
                                       });
                                    }

                                }
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