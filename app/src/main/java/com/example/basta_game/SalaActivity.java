package com.example.basta_game;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.basta_game.Fragments.LobbyFragment;
import com.example.basta_game.models.JugadorSala;
import com.example.basta_game.models.Usuario;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SalaActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sala);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Bundle bundle = getIntent().getBundleExtra("DATOS_SALA");

        if(bundle != null){
            mostrarLobby(bundle);
        }

    }

    public void mostrarLobby(Bundle datosSala){
        LobbyFragment lobby = new LobbyFragment();
        lobby.setArguments(datosSala);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.sala_fragment_container, lobby)
                .addToBackStack(null)
                .commit();
    }

    //controlando si el usuario sale de la pantalla


    @Override
    protected void onPause() {
        super.onPause();
        Log.d("ONPAUSE", "ENTRO AL ON PAUSE");
      if(!isFinishing()){
          try {
              auth = FirebaseAuth.getInstance();

              if(VerificacionesAuth.VerificarInicioSesion(auth)){
                  String uidUsuario = auth.getCurrentUser().getUid();

                  Bundle bundle = getIntent().getBundleExtra("DATOS_SALA");
                  if(bundle != null){
                      String idSala = bundle.getString("IDSALA");

                      DatabaseReference nodoSalas = FirebaseDatabase.getInstance().getReference("salas").child(idSala).child("jugadores").child(uidUsuario);
                      nodoSalas.addListenerForSingleValueEvent(new ValueEventListener() {
                          @Override
                          public void onDataChange(@NonNull DataSnapshot snapshot) {
                              JugadorSala jugador = snapshot.getValue(JugadorSala.class);
                              if(jugador != null){
                                  jugador.setEstaConectado(false);
                                  nodoSalas.setValue(jugador).addOnCompleteListener(task -> {
                                      if(task.isSuccessful()){
                                          Log.d("ONPAUSE", "se cambio el estado del jugador");
                                      }
                                  });
                              }
                              else{
                                  Toast.makeText(SalaActivity.this, "Snapshot esta vacio", Toast.LENGTH_SHORT).show();
                                  Log.d("ONPAUSE", "EL SNAPSHOT ESTA VACIO");
                              }
                          }

                          @Override
                          public void onCancelled(@NonNull DatabaseError error) {

                          }
                      });
                  }
              }else{
                  Log.d("ONPAUSE", "NO ESTA AUTENTICADO");
              }
          }catch (Exception e){
              Log.d("ONPAUSE", "ocurrio un error: " + e.getMessage());
          }
      }else{
          Log.d("ONPAUSE", "NO SE EJECUTO EL ON FINISHING");
      }
    }

    @Override
    protected void onResume() {
        super.onResume();
        auth = FirebaseAuth.getInstance();

        if(VerificacionesAuth.VerificarInicioSesion(auth)){
            String uidUsuario = auth.getCurrentUser().getUid();

            Bundle bundle = getIntent().getBundleExtra("DATOS_SALA");
            if(bundle != null){
                String idSala = bundle.getString("IDSALA");

                DatabaseReference nodoSalas = FirebaseDatabase.getInstance().getReference("salas").child(idSala).child("jugadores").child(uidUsuario);
                nodoSalas.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        JugadorSala jugador = snapshot.getValue(JugadorSala.class);
                        jugador.setEstaConectado(true);
                        nodoSalas.setValue(jugador).addOnCompleteListener(task -> {
                            if(task.isSuccessful()){
                                Log.d("ONPAUSE", "se cambio el estado del jugador");
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