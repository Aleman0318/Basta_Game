package com.example.basta_game.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.Adapters.HistorialPartidaAdapter;
import com.example.basta_game.R;
import com.example.basta_game.models.HistorialPartida;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;

public class HistorialFragment extends Fragment {

    private TextView txtMonedasHistorial;
    private TextView txtEmptyHistorial;
    private RecyclerView rvHistorialPartidas;

    private HistorialPartidaAdapter adapter;
    private final ArrayList<HistorialPartida> listaHistorial = new ArrayList<>();

    private DatabaseReference usuarioRef;
    private ValueEventListener listenerUsuario;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        txtMonedasHistorial = view.findViewById(R.id.txtMonedasHistorial);
        txtEmptyHistorial = view.findViewById(R.id.txtEmptyHistorial);
        rvHistorialPartidas = view.findViewById(R.id.rvHistorialPartidas);

        rvHistorialPartidas.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistorialPartidaAdapter(listaHistorial);
        rvHistorialPartidas.setAdapter(adapter);

        cargarHistorialUsuario();

        return view;
    }

    private void cargarHistorialUsuario() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            mostrarVacio("Inicia sesión para ver tu historial");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usuarioRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uid);

        listenerUsuario = usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int monedas = leerEntero(snapshot.child("monedas"), 0);
                txtMonedasHistorial.setText(String.valueOf(monedas));

                listaHistorial.clear();

                DataSnapshot historialSnap = snapshot.child("historialPartidas");

                for (DataSnapshot partidaSnap : historialSnap.getChildren()) {
                    HistorialPartida partida = leerPartida(partidaSnap);

                    if (partida != null) {
                        listaHistorial.add(partida);
                    }
                }

                Collections.sort(listaHistorial, (a, b) -> Long.compare(b.getFecha(), a.getFecha()));

                while (listaHistorial.size() > 5) {
                    listaHistorial.remove(listaHistorial.size() - 1);
                }

                adapter.notifyDataSetChanged();

                if (listaHistorial.isEmpty()) {
                    mostrarVacio("Todavía no tienes partidas guardadas");
                } else {
                    txtEmptyHistorial.setVisibility(View.GONE);
                    rvHistorialPartidas.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mostrarVacio("No se pudo cargar el historial");
            }
        });
    }

    private HistorialPartida leerPartida(DataSnapshot partidaSnap) {
        String idPartida = partidaSnap.child("idPartida").getValue(String.class);

        if (idPartida == null || idPartida.trim().isEmpty()) {
            idPartida = partidaSnap.getKey();
        }

        if (idPartida == null) {
            return null;
        }

        HistorialPartida partida = new HistorialPartida();

        partida.setIdPartida(idPartida);
        partida.setIdSala(partidaSnap.child("idSala").getValue(String.class));
        partida.setCodigoSala(partidaSnap.child("codigoSala").getValue(String.class));
        partida.setFecha(leerLong(partidaSnap.child("fecha"), 0L));
        partida.setCantidadRondas(leerEntero(partidaSnap.child("cantidadRondas"), 0));
        partida.setJugadoresCantidad(leerEntero(partidaSnap.child("jugadoresCantidad"), 0));
        partida.setMonedasEntregadas(Boolean.TRUE.equals(partidaSnap.child("monedasEntregadas").getValue(Boolean.class)));

        partida.setMiUid(partidaSnap.child("miUid").getValue(String.class));
        partida.setMiNombre(partidaSnap.child("miNombre").getValue(String.class));
        partida.setMiPosicion(leerEntero(partidaSnap.child("miPosicion"), 0));
        partida.setMiTotal(leerEntero(partidaSnap.child("miTotal"), 0));
        partida.setMonedas(leerEntero(partidaSnap.child("monedas"), 0));

        partida.setGanadorUid(partidaSnap.child("ganadorUid").getValue(String.class));
        partida.setGanadorNombre(partidaSnap.child("ganadorNombre").getValue(String.class));
        partida.setPuntajeGanador(leerEntero(partidaSnap.child("puntajeGanador"), 0));

        ArrayList<HistorialPartida.TopJugador> top3 = new ArrayList<>();

        for (DataSnapshot topSnap : partidaSnap.child("top3").getChildren()) {
            String uid = topSnap.child("uid").getValue(String.class);
            String nombre = topSnap.child("nombre").getValue(String.class);
            String avatarUrl = topSnap.child("avatarUrl").getValue(String.class);
            int posicion = leerEntero(topSnap.child("posicion"), 0);
            int total = leerEntero(topSnap.child("total"), 0);

            if (nombre == null || nombre.trim().isEmpty()) {
                nombre = "Jugador";
            }

            top3.add(new HistorialPartida.TopJugador(uid, nombre, avatarUrl, posicion, total));
        }

        Collections.sort(top3, (a, b) -> Integer.compare(a.getPosicion(), b.getPosicion()));
        partida.setTop3(top3);

        return partida;
    }

    private void mostrarVacio(String mensaje) {
        txtEmptyHistorial.setText(mensaje);
        txtEmptyHistorial.setVisibility(View.VISIBLE);
        rvHistorialPartidas.setVisibility(View.GONE);
    }

    private int leerEntero(DataSnapshot snapshot, int valorDefault) {
        Object valor = snapshot.getValue();

        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }

        if (valor instanceof String) {
            try {
                return Integer.parseInt((String) valor);
            } catch (NumberFormatException e) {
                return valorDefault;
            }
        }

        return valorDefault;
    }

    private long leerLong(DataSnapshot snapshot, long valorDefault) {
        Object valor = snapshot.getValue();

        if (valor instanceof Number) {
            return ((Number) valor).longValue();
        }

        if (valor instanceof String) {
            try {
                return Long.parseLong((String) valor);
            } catch (NumberFormatException e) {
                return valorDefault;
            }
        }

        return valorDefault;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (usuarioRef != null && listenerUsuario != null) {
            usuarioRef.removeEventListener(listenerUsuario);
            listenerUsuario = null;
            usuarioRef = null;
        }
    }
}
