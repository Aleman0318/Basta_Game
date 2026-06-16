package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.R;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {

    public HistorialAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        JugadorHistorialAdapter adaptadorJugadores = new JugadorHistorialAdapter();
        holder.rvJugadoresPartida.setAdapter(adaptadorJugadores);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        RecyclerView rvJugadoresPartida;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rvJugadoresPartida = itemView.findViewById(R.id.rvJugadoresPartidaHistorial);
        }
    }
}