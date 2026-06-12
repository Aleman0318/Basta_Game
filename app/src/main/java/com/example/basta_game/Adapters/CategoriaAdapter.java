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
import com.example.basta_game.models.Categoria;
import com.example.basta_game.R;
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_categoria, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categoria categoria = listaCategorias.get(position);
        holder.tvNombre.setText(categoria.getNombre());

        // esto asigna la imagen correspondiente proveniente de el list de categoria
        holder.ivIcono.setImageResource(categoria.getIconoId());
        if (categoria.isHabilitada()) {
            holder.cardView.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3B4B7D")));
        } else {
            holder.cardView.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C2C0C0")));
        }

        holder.itemView.setOnClickListener(v -> {
            boolean nuevoEstado = !categoria.isHabilitada();
            categoria.setHabilitada(nuevoEstado);

            notifyItemChanged(holder.getAdapterPosition());

            if (listener != null) {
                listener.onCategoriaToggle(categoria, nuevoEstado);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaCategorias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        ImageView ivIcono;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreCategoria);
            ivIcono = itemView.findViewById(R.id.ivIconoCategoria);
            cardView = (MaterialCardView) itemView;
        }
    }
}