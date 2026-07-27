package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.ComodinPartida;

import java.util.List;

public class ComodinPartidaAdapter extends RecyclerView.Adapter<ComodinPartidaAdapter.ViewHolder> {

    public interface OnUsarComodinListener {
        void onUsar(ComodinPartida comodin);
    }

    private final List<ComodinPartida> listaComodines;
    private final OnUsarComodinListener listener;

    public ComodinPartidaAdapter(
            List<ComodinPartida> listaComodines,
            OnUsarComodinListener listener
    ) {
        this.listaComodines = listaComodines;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ComodinPartidaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comodin_partida, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComodinPartidaAdapter.ViewHolder holder, int position) {
        ComodinPartida comodin = listaComodines.get(position);

        holder.imgIcono.setImageResource(comodin.getIconoResId());
        holder.txtNombre.setText(comodin.getNombre());
        holder.txtDescripcion.setText(comodin.getDescripcion());
        holder.txtCantidad.setText("Tienes: " + comodin.getCantidad());
        holder.txtRareza.setText(comodin.getRareza());

        // Ya no usamos .setCardBackgroundColor() ni .setStrokeColor() porque
        // al usar el nuevo diseño de carta, el fondo ya está en la imagen 9-patch.

        if (comodin.isImplementado()) {
            // Validamos si el botón soporta texto (por si usaste la imagen del diseñador en el XML)
            if (holder.btnUsar instanceof TextView) {
                ((TextView) holder.btnUsar).setText("USAR");
            }
            holder.btnUsar.setEnabled(true);
            holder.btnUsar.setAlpha(1f);
        } else {
            if (holder.btnUsar instanceof TextView) {
                ((TextView) holder.btnUsar).setText("PRONTO");
            }
            holder.btnUsar.setEnabled(false); // Deshabilitamos el clic para los no implementados
            holder.btnUsar.setAlpha(0.6f);    // Lo hacemos más transparente
        }

        holder.btnUsar.setOnClickListener(v -> {
            if (listener != null && comodin.isImplementado()) {
                listener.onUsar(comodin);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaComodines.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        View cardComodin; // <--- Cambiado a View genérico para evitar errores
        ImageView imgIcono;
        TextView txtNombre;
        TextView txtDescripcion;
        TextView txtCantidad;
        TextView txtRareza;
        View btnUsar; // <--- Cambiado a View genérico

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardComodin = itemView.findViewById(R.id.cardComodinPartidaItem);
            imgIcono = itemView.findViewById(R.id.imgIconoComodinPartidaItem);
            txtNombre = itemView.findViewById(R.id.txtNombreComodinPartidaItem);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcionComodinPartidaItem);
            txtCantidad = itemView.findViewById(R.id.txtCantidadComodinPartidaItem);
            txtRareza = itemView.findViewById(R.id.txtRarezaComodinPartidaItem);
            btnUsar = itemView.findViewById(R.id.btnUsarComodinPartidaItem);
        }
    }
}