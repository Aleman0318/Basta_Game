package com.example.basta_game;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.basta_game.Fragments.PartidaFragment;
import java.util.ArrayList; // No olvides importar esto

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

        // ==========================================
        // ¡AQUÍ ESTÁ LA SOLUCIÓN!
        // Agregamos categorías simuladas para la prueba
        // ==========================================
        ArrayList<String> categoriasPrueba = new ArrayList<>();
        categoriasPrueba.add("Nombre");
        categoriasPrueba.add("Color");
        categoriasPrueba.add("Fruta");
        categoriasPrueba.add("País");
        categoriasPrueba.add("Animal");
        categoriasPrueba.add("Cosa");
        categoriasPrueba.add("Apellido");

        bundle.putStringArrayList("CATEGORIAS", categoriasPrueba);

        partidaFragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main, partidaFragment)
                .commit();
    }
}