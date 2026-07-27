package com.example.basta_game.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.EventoComodin;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ResumenComodinesAdapter extends RecyclerView.Adapter<ResumenComodinesAdapter.ViewHolder> {

    private List<EventoComodin> listaEventos;

    public ResumenComodinesAdapter(List<EventoComodin> listaEventos) {
        this.listaEventos = listaEventos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resumen_comodin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventoComodin evento = listaEventos.get(position);

        holder.txtTitulo.setText(evento.getTitulo());
        holder.txtDescripcion.setText(evento.getDescripcion());
        holder.imgIcono.setImageResource(evento.getIconoResId());

        // Coloreamos los bordes dependiendo si fue un beneficio o un castigo
        if (evento.isEsBeneficio()) {
            holder.cardView.setStrokeColor(Color.parseColor("#BDE8B8")); // Verde suave
            holder.cardView.setCardBackgroundColor(Color.parseColor("#F1FFF0"));
        } else {
            holder.cardView.setStrokeColor(Color.parseColor("#FFC4C4")); // Rojo suave
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF1F1"));
        }
    }

    @Override
    public int getItemCount() {
        return listaEventos != null ? listaEventos.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView imgIcono;
        TextView txtTitulo;
        TextView txtDescripcion;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardEventoComodin);
            imgIcono = itemView.findViewById(R.id.imgIconoComodinResumen);
            txtTitulo = itemView.findViewById(R.id.txtTituloEvento);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcionEvento);
        }
    }
}