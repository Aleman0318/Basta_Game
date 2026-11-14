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
    private final java.util.function.Predicate<ComodinPartida> disponible;

    public ComodinPartidaAdapter(
            List<ComodinPartida> listaComodines,
            OnUsarComodinListener listener,
            java.util.function.Predicate<ComodinPartida> disponible
    ) {
        this.listaComodines = listaComodines;
        this.listener = listener;
        this.disponible = disponible;
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
        boolean resultados = com.example.basta_game.utils.FaseComodin.deResultados(comodin.getId());
        holder.txtGrupo.setText(resultados ? "COMODINES DE RESULTADOS" : "COMODINES DE PARTIDA");
        holder.txtGrupo.setVisibility(position == 0 || resultados !=
                com.example.basta_game.utils.FaseComodin.deResultados(listaComodines.get(position - 1).getId())
                ? View.VISIBLE : View.GONE);
        holder.txtCantidad.setText("Tienes: " + comodin.getCantidad() + (resultados
                ? " · Entre rondas, en puntuaciones" : " · Mientras respondes"));

        // Ya no usamos .setCardBackgroundColor() ni .setStrokeColor() porque
        // al usar el nuevo diseño de carta, el fondo ya está en la imagen 9-patch.

        if (comodin.isImplementado() && disponible.test(comodin)) {
            // Validamos si el botón soporta texto (por si usaste la imagen del diseñador en el XML)
            if (holder.btnUsar instanceof TextView) {
                ((TextView) holder.btnUsar).setText("USAR");
            }
            holder.btnUsar.setEnabled(true);
            holder.btnUsar.setAlpha(1f);
        } else {
            if (holder.btnUsar instanceof TextView) {
                ((TextView) holder.btnUsar).setText(comodin.isImplementado() ? "NO DISPONIBLE" : "PRONTO");
            }
            holder.btnUsar.setEnabled(false); // Deshabilitamos el clic para los no implementados
            holder.btnUsar.setAlpha(0.6f);    // Lo hacemos más transparente
        }

        holder.btnUsar.setOnClickListener(v -> {
            if (listener != null && comodin.isImplementado() && disponible.test(comodin)) {
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
        TextView txtGrupo;
        View btnUsar; // <--- Cambiado a View genérico

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardComodin = itemView.findViewById(R.id.cardComodinPartidaItem);
            imgIcono = itemView.findViewById(R.id.imgIconoComodinPartidaItem);
            txtNombre = itemView.findViewById(R.id.txtNombreComodinPartidaItem);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcionComodinPartidaItem);
            txtCantidad = itemView.findViewById(R.id.txtCantidadComodinPartidaItem);
            txtRareza = itemView.findViewById(R.id.txtRarezaComodinPartidaItem);
            txtGrupo = itemView.findViewById(R.id.txtGrupoComodin);
            btnUsar = itemView.findViewById(R.id.btnUsarComodinPartidaItem);
        }
    }
}
