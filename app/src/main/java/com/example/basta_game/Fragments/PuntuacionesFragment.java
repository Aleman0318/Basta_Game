package com.example.basta_game.Fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.Adapters.PuntuacionAdapter;
import com.example.basta_game.HomeActivity;
import com.example.basta_game.R;
import com.example.basta_game.models.PuntuacionJugador;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PuntuacionesFragment extends Fragment {

    private RecyclerView rvTablaPuntuaciones;
    private MaterialButton btnVolverInicio;
    private LinearLayout contenedorEncabezadosRondas;
    private TextView txtTituloResultados;
    private TextView txtGanadorResultados;

    private String idSala;
    private int cantidadRondas = 5;
    private boolean esFinal = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_puntuaciones, container, false);

        rvTablaPuntuaciones = view.findViewById(R.id.rvTablaPuntuaciones);
        btnVolverInicio = view.findViewById(R.id.btnVolverInicio);
        contenedorEncabezadosRondas = view.findViewById(R.id.contenedorEncabezadosRondas);
        txtTituloResultados = view.findViewById(R.id.txtTituloResultados);
        txtGanadorResultados = view.findViewById(R.id.txtGanadorResultados);

        if (getArguments() != null) {
            idSala = getArguments().getString("IDSALA");
            cantidadRondas = getArguments().getInt("CANTIDAD_RONDAS", 5);
            esFinal = getArguments().getBoolean("ES_FINAL", true);
        }

        if (cantidadRondas < 1) {
            cantidadRondas = 1;
        }

        if (cantidadRondas > 8) {
            cantidadRondas = 8;
        }

        rvTablaPuntuaciones.setLayoutManager(new LinearLayoutManager(getContext()));

        configurarEncabezados();

        if (esFinal) {
            txtTituloResultados.setText("RESULTADOS FINALES");
            btnVolverInicio.setText("VOLVER AL INICIO");
        } else {
            txtTituloResultados.setText("PUNTUACIONES DE RONDA");
            btnVolverInicio.setText("LISTO");
        }

        cargarPuntuaciones();

        btnVolverInicio.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void configurarEncabezados() {
        contenedorEncabezadosRondas.removeAllViews();

        for (int i = 1; i <= cantidadRondas; i++) {
            TextView textView = new TextView(requireContext());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(60),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            textView.setLayoutParams(params);
            textView.setText("R" + i);
            textView.setGravity(Gravity.CENTER);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextSize(16);
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blanco));

            contenedorEncabezadosRondas.addView(textView);
        }
    }

    private void cargarPuntuaciones() {
        if (idSala == null || idSala.trim().isEmpty()) {
            txtGanadorResultados.setText("No se encontró la sala.");
            return;
        }

        DatabaseReference nodoSala = FirebaseDatabase.getInstance()
                .getReference("salas")
                .child(idSala);

        nodoSala.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotSala) {
                List<PuntuacionJugador> lista = new ArrayList<>();

                DataSnapshot snapshotJugadores = snapshotSala.child("jugadores");
                DataSnapshot snapshotPuntajesRondas = snapshotSala.child("puntajesRondas");

                for (DataSnapshot jugadorSnap : snapshotJugadores.getChildren()) {
                    String uid = jugadorSnap.getKey();

                    if (uid == null) {
                        continue;
                    }

                    String nombre = jugadorSnap.child("nombre").getValue(String.class);

                    if (nombre == null || nombre.trim().isEmpty()) {
                        nombre = "Jugador";
                    }

                    List<Integer> puntajes = new ArrayList<>();
                    int total = 0;

                    for (int ronda = 1; ronda <= cantidadRondas; ronda++) {
                        Integer puntos = snapshotPuntajesRondas
                                .child("ronda_" + ronda)
                                .child(uid)
                                .getValue(Integer.class);

                        puntajes.add(puntos);

                        if (puntos != null) {
                            total += puntos;
                        }
                    }

                    lista.add(new PuntuacionJugador(0, uid, nombre, puntajes, total));
                }

                Collections.sort(lista, (a, b) -> Integer.compare(b.getTotal(), a.getTotal()));

                for (int i = 0; i < lista.size(); i++) {
                    lista.get(i).setPosicion(i + 1);
                }

                PuntuacionAdapter adapter = new PuntuacionAdapter(lista, cantidadRondas);
                rvTablaPuntuaciones.setAdapter(adapter);

                txtGanadorResultados.setText(obtenerTextoGanador(lista));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtGanadorResultados.setText("Error cargando puntuaciones.");
            }
        });
    }

    private String obtenerTextoGanador(List<PuntuacionJugador> lista) {
        if (lista == null || lista.isEmpty()) {
            return "Sin puntuaciones todavía.";
        }

        int mayorPuntaje = lista.get(0).getTotal();

        if (mayorPuntaje <= 0) {
            return "Aún no hay puntos acumulados.";
        }

        List<String> ganadores = new ArrayList<>();

        for (PuntuacionJugador jugador : lista) {
            if (jugador.getTotal() == mayorPuntaje) {
                ganadores.add(jugador.getNombre());
            }
        }

        if (ganadores.size() == 1) {
            return "Ganador: " + ganadores.get(0) + " con " + mayorPuntaje + " puntos";
        }

        return "Empate: " + String.join(", ", ganadores) + " con " + mayorPuntaje + " puntos";
    }

    private int dp(int valor) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(valor * density);
    }
}