package com.example.basta_game.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.Models.Jugador;
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

        if (jugador.isEstaListo()) {
            holder.tvEstado.setText("LISTO");
            holder.tvEstado.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvEstado.setText("ESPERANDO...");
            holder.tvEstado.setTextColor(Color.parseColor("#F97316"));
        }
    }

    @Override
    public int getItemCount() { return listaJugadores.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvEstado;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreJugador);
            tvEstado = itemView.findViewById(R.id.tvEstadoJugador);
        }
    }
}