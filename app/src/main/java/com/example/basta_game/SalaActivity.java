package com.example.basta_game;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.basta_game.Fragments.LobbyFragment;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SalaActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private String idSala;
    private String codigoSala;
    private boolean esHost;
    private DatabaseReference nodoJugadorSala;
    private ValueEventListener listenerDesconexion;

    private boolean salida = false;

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

        auth = FirebaseAuth.getInstance();

        Bundle bundle = getIntent().getBundleExtra("DATOS_SALA");

        if (bundle != null) {
            idSala = bundle.getString("IDSALA", "");
            esHost = bundle.getBoolean("ES_ANFITRION");
            codigoSala = bundle.getString("CODIGO_SALA");
            mostrarLobby(bundle);
        }

        if (VerificacionesAuth.VerificarInicioSesion(auth) && idSala != null && !idSala.isEmpty()) {
            String uid = auth.getCurrentUser().getUid();

            nodoJugadorSala = FirebaseDatabase.getInstance()
                    .getReference("salas")
                    .child(idSala)
                    .child("jugadores")
                    .child(uid);

            VerificarSiUsuarioSeDesconecto();
        }
    }

    public void mostrarLobby(Bundle datosSala) {
        LobbyFragment lobby = new LobbyFragment();
        lobby.setArguments(datosSala);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.sala_fragment_container, lobby)
                .commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!salida){
            Log.d("ONRESUME", "Entró al onPause");

            if (VerificacionesAuth.VerificarInicioSesion(auth) && nodoJugadorSala != null && !isFinishing()) {
                nodoJugadorSala.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            nodoJugadorSala.child("estaConectado").setValue(false)
                                    .addOnSuccessListener(unused -> Log.d("ONPAUSE", "Jugador marcado como desconectado"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d("ONPAUSE", "Error: " + error.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!salida){
            Log.d("ONRESUME", "Entró al onResume");

            if (VerificacionesAuth.VerificarInicioSesion(auth) && nodoJugadorSala != null) {
                nodoJugadorSala.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            nodoJugadorSala.child("estaConectado").setValue(true)
                                    .addOnSuccessListener(unused -> Log.d("ONRESUME", "Jugador marcado como conectado"));
                        } else {
                            mandarHomePorDesconexion();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d("ONRESUME", "Error: " + error.getMessage());
                    }
                });
            }
        }
    }

    public void VerificarSiUsuarioSeDesconecto() {
        Log.d("DESCONECTADO", "Mi nodo jugador desapareció. salida=" + salida + " esHost=" + esHost + " idSala=" + idSala);
        //para evitar que la app EXPLOTE si el nodo esta vacio
        if (nodoJugadorSala == null) {
            return;
        }

        listenerDesconexion = nodoJugadorSala.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    mandarHomePorDesconexion();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("DESCONECTADO", "Error: " + error.getMessage());
            }
        });
    }

    public void mandarHomePorDesconexion() {
        if(salida){
            return;
        }

        salida = true;

        if (nodoJugadorSala != null && listenerDesconexion != null) {
            nodoJugadorSala.removeEventListener(listenerDesconexion);
            listenerDesconexion = null;
        }

        if(esHost && VerificacionesAuth.VerificarInicioSesion(auth)){
            DatabaseReference nodoSalaEliminada = FirebaseDatabase.getInstance().getReference("salas").child(idSala);
            nodoSalaEliminada.removeValue().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    DatabaseReference nodoCodigoSala = FirebaseDatabase.getInstance().getReference("codigosSalas").child(codigoSala);
                    nodoCodigoSala.removeValue().addOnCompleteListener(task2 ->{
                        if(task2.isSuccessful()){
                            Intent intent = new Intent(SalaActivity.this, HomeActivity.class);
                            intent.putExtra("DESCONEXION", false);
                            intent.putExtra("CREADOR_DESCONEXION", true);
                            startActivity(intent);
                            finish();
                        }
                    });
                }
            });
        }else{
            String uidUsuarioEliminar = auth.getCurrentUser().getUid();
            DatabaseReference nodoSalaEliminada = FirebaseDatabase.getInstance().getReference("salas").child(idSala).child("jugadores").child(uidUsuarioEliminar);
            nodoSalaEliminada.removeValue().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    Intent intent = new Intent(SalaActivity.this, HomeActivity.class);
                    intent.putExtra("DESCONEXION", true);
                    intent.putExtra("CREADOR_DESCONEXION", false);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (nodoJugadorSala != null && listenerDesconexion != null) {
            nodoJugadorSala.removeEventListener(listenerDesconexion);
        }
    }
}