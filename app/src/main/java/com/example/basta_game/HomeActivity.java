package com.example.basta_game;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.basta_game.Fragments.HistorialFragment;
import com.example.basta_game.Fragments.JugarFragment;
import com.example.basta_game.Fragments.PerfilFragment;
import com.example.basta_game.Fragments.TiendaFragment;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private View notificationHome;
    private FirebaseAuth auth;

    private Fragment fragmentJugar = new JugarFragment();
    private Fragment fragmentTienda = new TiendaFragment();
    private Fragment fragmentHistorial = new HistorialFragment();
    private Fragment fragmentPerfil = new PerfilFragment();
    private final FragmentManager fm = getSupportFragmentManager();

    private Fragment fragmentActivo = fragmentJugar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            fragmentJugar = fm.findFragmentByTag("jugar");
            fragmentTienda = fm.findFragmentByTag("tienda");
            fragmentHistorial = fm.findFragmentByTag("historial");
            fragmentPerfil = fm.findFragmentByTag("perfil");
            for (Fragment fragment : fm.getFragments()) {
                if (!fragment.isHidden()) fragmentActivo = fragment;
            }
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        notificationHome = findViewById(R.id.notificationHome);
        auth = FirebaseAuth.getInstance();

        if (getIntent().getExtras() != null) {
            boolean seDesconecto = getIntent().getExtras().getBoolean("DESCONEXION");
            boolean seDesconectoSiendoHost = getIntent().getExtras().getBoolean("CREADOR_DESCONEXION");

            if (seDesconecto && !seDesconectoSiendoHost) {
                NotificationHelper.show(
                        notificationHome,
                        "Has abandonado la partida",
                        NotificationHelper.WARNING
                );
                Sounds.playNotificaciones();
            } else if (seDesconectoSiendoHost && !seDesconecto) {
                NotificationHelper.show(
                        notificationHome,
                        "Se ha eliminado la sala, volviendo a home",
                        NotificationHelper.WARNING
                );
                Sounds.playNotificaciones();
            }
        } else {
            if (VerificacionesAuth.VerificarInicioSesion(auth)) {
                VerificacionesAuth.CargarYMostrarUsuarioLogueado(notificationHome, auth);
            }
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setItemIconTintList(null);

        if (savedInstanceState == null) {
            fm.beginTransaction().add(R.id.fragment_container, fragmentPerfil, "perfil").hide(fragmentPerfil).commit();
            fm.beginTransaction().add(R.id.fragment_container, fragmentHistorial, "historial").hide(fragmentHistorial).commit();
            fm.beginTransaction().add(R.id.fragment_container, fragmentTienda, "tienda").hide(fragmentTienda).commit();
            fm.beginTransaction().add(R.id.fragment_container, fragmentJugar, "jugar").commit();

            bottomNavigationView.setSelectedItemId(R.id.nav_jugar);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_jugar) {
                fm.beginTransaction().hide(fragmentActivo).show(fragmentJugar).commit();
                fragmentActivo = fragmentJugar;
                return true;
            } else if (id == R.id.nav_tienda) {
                fm.beginTransaction().hide(fragmentActivo).show(fragmentTienda).commit();
                fragmentActivo = fragmentTienda;
                return true;
            } else if (id == R.id.nav_historial) {
                fm.beginTransaction().hide(fragmentActivo).show(fragmentHistorial).commit();
                fragmentActivo = fragmentHistorial;
                return true;
            } else if (id == R.id.nav_perfil) {
                fm.beginTransaction().hide(fragmentActivo).show(fragmentPerfil).commit();
                fragmentActivo = fragmentPerfil;
                return true;
            }

            return false;
        });
    }
}
