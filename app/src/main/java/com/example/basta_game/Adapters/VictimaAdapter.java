package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;

import java.util.List;

public class VictimaAdapter extends RecyclerView.Adapter<VictimaAdapter.ViewHolder> {

    // Usaremos una clase sencilla temporal para recibir datos.
    // Si ya tienes un modelo "Jugador", puedes cambiar "JugadorMini" por el tuyo.
    public static class JugadorMini {
        public String uid;
        public String nombre;
        public JugadorMini(String uid, String nombre) { this.uid = uid; this.nombre = nombre; }
    }

    private List<JugadorMini> listaJugadores;
    private OnVictimaClickListener listener;

    public interface OnVictimaClickListener {
        void onVictimaClick(JugadorMini victima);
    }

    public VictimaAdapter(List<JugadorMini> listaJugadores, OnVictimaClickListener listener) {
        this.listaJugadores = listaJugadores;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_victima, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JugadorMini jugador = listaJugadores.get(position);
        holder.txtNombre.setText(jugador.nombre);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onVictimaClick(jugador);
        });
    }

    @Override
    public int getItemCount() {
        return listaJugadores != null ? listaJugadores.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreVictimaa);
        }
    }
}