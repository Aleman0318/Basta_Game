package com.example.basta_game.Adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.Categoria;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CategoriaAdapter extends RecyclerView.Adapter<CategoriaAdapter.ViewHolder> {

    private List<Categoria> listaCategorias;
    private OnCategoriaClickListener listener;

    public interface OnCategoriaClickListener {
        void onCategoriaToggle(Categoria categoria, boolean estaHabilitada);
    }

    public CategoriaAdapter(List<Categoria> listaCategorias, OnCategoriaClickListener listener) {
        this.listaCategorias = listaCategorias;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoriaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_categoria, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriaAdapter.ViewHolder holder, int position) {
        Categoria categoria = listaCategorias.get(position);

        holder.tvNombre.setText(categoria.getNombre());
        holder.ivIcono.setImageResource(categoria.getIconoId());

        aplicarEstadoVisual(holder, categoria.isHabilitada());

        holder.itemView.setOnClickListener(v -> {
            boolean nuevoEstado = !categoria.isHabilitada();
            categoria.setHabilitada(nuevoEstado);

            aplicarEstadoVisual(holder, nuevoEstado);

            if (listener != null) {
                listener.onCategoriaToggle(categoria, nuevoEstado);
            }
        });
    }

    private void aplicarEstadoVisual(ViewHolder holder, boolean habilitada) {
        if (habilitada) {
            holder.cardView.setCardBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#EEF4FF"))
            );
            holder.cardView.setStrokeColor(
                    ColorStateList.valueOf(Color.parseColor("#0017ED"))
            );
            holder.cardView.setStrokeWidth(3);

            holder.cardIcono.setCardBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            );
            holder.cardIcono.setStrokeColor(
                    ColorStateList.valueOf(Color.parseColor("#0017ED"))
            );

            holder.tvNombre.setTextColor(Color.parseColor("#0017ED"));
            holder.tvEstado.setText("Seleccionada");
            holder.tvEstado.setTextColor(Color.parseColor("#0017ED"));

            holder.itemView.setAlpha(1f);

        } else {
            holder.cardView.setCardBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#FCFDFF"))
            );
            holder.cardView.setStrokeColor(
                    ColorStateList.valueOf(Color.parseColor("#D6E0FF"))
            );
            holder.cardView.setStrokeWidth(2);

            holder.cardIcono.setCardBackgroundColor(
                    ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            );
            holder.cardIcono.setStrokeColor(
                    ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
            );

            holder.tvNombre.setTextColor(Color.parseColor("#1E293B"));
            holder.tvEstado.setText("Deseleccionada");
            holder.tvEstado.setTextColor(Color.parseColor("#9E9E9E"));

            holder.itemView.setAlpha(0.88f);
        }
    }

    @Override
    public int getItemCount() {
        return listaCategorias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvEstado;
        ImageView ivIcono;
        MaterialCardView cardView;
        MaterialCardView cardIcono;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.cardCategoria);
            cardIcono = itemView.findViewById(R.id.cardIconoCategoria);
            tvNombre = itemView.findViewById(R.id.tvNombreCategoria);
            tvEstado = itemView.findViewById(R.id.tvEstadoCategoria);
            ivIcono = itemView.findViewById(R.id.ivIconoCategoria);
        }
    }
}