package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import java.util.List;

public class PuntajePartidaAdapter extends RecyclerView.Adapter<PuntajePartidaAdapter.ViewHolder> {
    private List<Jugador> listaJugadores;

    public PuntajePartidaAdapter(List<Jugador> listaJugadores) {
        this.listaJugadores = listaJugadores;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_puntaje_partida, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Jugador jugador = listaJugadores.get(position);

        holder.txtPosicion.setText(String.valueOf(position + 1));
        holder.txtNombre.setText(jugador.getNombre());
        // Ficticio, asume que le asignamos una cantidad base para el diseño
        holder.txtPuntos.setText((40 - (position * 5)) + " ⭐");
    }

    @Override
    public int getItemCount() { return listaJugadores.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtPosicion, txtNombre, txtPuntos;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPosicion = itemView.findViewById(R.id.txtPosicionJugador);
            txtNombre = itemView.findViewById(R.id.txtNombrePuntaje);
            txtPuntos = itemView.findViewById(R.id.txtPuntosJugador);
        }
    }
}