package com.example.basta_game;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.basta_game.Fragments.PartidaFragment;

public class SalaActivity extends AppCompatActivity {

    private String codigoSala;
    private boolean esAnfitrion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sala);

        // DATOS DE PRUEBA SOLO PARA VER EL DISEÑO
        codigoSala = "TEST1";
        esAnfitrion = true;

        mostrarPartidaDePrueba();
    }

    private void mostrarPartidaDePrueba() {
        PartidaFragment partidaFragment = new PartidaFragment();

        Bundle bundle = new Bundle();
        bundle.putString("CODIGO_SALA", codigoSala);
        bundle.putBoolean("ES_ANFITRION", esAnfitrion);

        partidaFragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.sala_fragment_container, partidaFragment)
                .commit();
    }
}