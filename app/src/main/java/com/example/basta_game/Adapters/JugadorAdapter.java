package com.example.basta_game.Adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import java.util.List;

public class JugadorAdapter extends RecyclerView.Adapter<JugadorAdapter.ViewHolder> {
    private List<Jugador> listaJugadores;

    public JugadorAdapter(List<Jugador> listaJugadores) {
        this.listaJugadores = listaJugadores;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_jugador, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Jugador jugador = listaJugadores.get(position);
        holder.tvNombre.setText(jugador.getNombre());

        // Lógica de colores para el círculo
        if (jugador.isEstaListo()) {
            // Verde si está listo
            holder.cardEstado.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            // Rojo si no está listo
            holder.cardEstado.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F44336")));
        }
    }

    @Override
    public int getItemCount() {
        return listaJugadores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        MaterialCardView cardEstado; // Cambiamos de TextView a MaterialCardView

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreJugador);
            cardEstado = itemView.findViewById(R.id.cardEstadoJugador); // Conectamos el nuevo círculo
        }
    }
}