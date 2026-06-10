package com.example.basta_game;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.example.basta_game.Fragments.JugarFragment;
import com.example.basta_game.Fragments.PerfilFragment;
import com.example.basta_game.Fragments.RankingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;

        });


        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setItemIconTintList(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragmentSeleccionado = null;
            int id = item.getItemId();

            if (id == R.id.nav_jugar) {
                fragmentSeleccionado = new JugarFragment();
            } else if (id == R.id.nav_ranking) {
                fragmentSeleccionado = new RankingFragment();
            } else if (id == R.id.nav_perfil) {
                fragmentSeleccionado = new PerfilFragment();
            }

            if (fragmentSeleccionado != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragmentSeleccionado)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_jugar);
        }
    }
}