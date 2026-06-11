package com.example.basta_game.Adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.JugadorSala;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class JugadorSalaAdapter extends RecyclerView.Adapter<JugadorSalaAdapter.ViewHolder> {

    private List<JugadorSala> listaJugadores;

    public JugadorSalaAdapter(List<JugadorSala> listaJugadores) {
        this.listaJugadores = listaJugadores;
    }

    @NonNull
    @Override
    public JugadorSalaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_jugador, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JugadorSalaAdapter.ViewHolder holder, int position) {
        JugadorSala jugador = listaJugadores.get(position);

        holder.tvNombre.setText(jugador.getNombre());

        // Verde si está listo, rojo si no está listo
        if (jugador.isEstaListo()) {
            holder.cardEstado.setCardBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            );

            holder.tvEstadoJugador.setText("Listo");

        } else {
            holder.cardEstado.setCardBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#F44336"))
            );

            holder.tvEstadoJugador.setText("En Espera");
        }
    }

    @Override
    public int getItemCount() {
        return listaJugadores.size();
    }

    public void actualizarLista(List<JugadorSala> nuevaLista) {
        this.listaJugadores = nuevaLista;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvNombre;

        TextView tvEstadoJugador;
        MaterialCardView cardEstado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvNombre = itemView.findViewById(R.id.tvNombreJugador);
            cardEstado = itemView.findViewById(R.id.cardEstadoJugador);
            tvEstadoJugador = itemView.findViewById(R.id.tvEstadoJugador);
        }
    }
}