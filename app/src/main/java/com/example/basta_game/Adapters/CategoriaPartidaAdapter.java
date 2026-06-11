package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.R;
import java.util.List;

public class CategoriaPartidaAdapter extends RecyclerView.Adapter<CategoriaPartidaAdapter.ViewHolder> {
    private List<Categoria> listaCategorias;

    public CategoriaPartidaAdapter(List<Categoria> listaCategorias) {
        this.listaCategorias = listaCategorias;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Asegúrate de tener tu XML item_categoria_partida alineado con el diseño
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_categoria_partida, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categoria cat = listaCategorias.get(position);
        holder.txtNombre.setText(cat.getNombre());
        holder.imgIcono.setImageResource(cat.getIconoId());
    }

    @Override
    public int getItemCount() { return listaCategorias.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre;
        ImageView imgIcono;
        EditText etRespuesta;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Reemplaza con los IDs de tu XML item_categoria_partida
            txtNombre = itemView.findViewById(R.id.txtNombreCategoriaPartida);
            imgIcono = itemView.findViewById(R.id.imgIconoCategoriaPartida);
        }
    }
}