package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.CategoriaTienda;

import java.util.List;
import java.util.Map;

public class CategoriaTiendaAdapter extends RecyclerView.Adapter<CategoriaTiendaAdapter.CategoriaViewHolder> {

    private List<CategoriaTienda> listaCategorias;
    private ComodinTiendaAdapter.OnComprarClickListener listener;
    private Map<String, Integer> inventarioGlobal;
    private boolean enCompra;

    public void compraEnCurso(boolean valor) {
        enCompra=valor;
        if(listaCategorias!=null) notifyItemRangeChanged(0,listaCategorias.size(),"ACTUALIZAR_INVENTARIO");
    }

    public CategoriaTiendaAdapter(List<CategoriaTienda> listaCategorias, ComodinTiendaAdapter.OnComprarClickListener listener) {
        this.listaCategorias = listaCategorias;
        this.listener = listener;
    }

    public void actualizarInventario(Map<String, Integer> nuevoInventario) {
        this.inventarioGlobal = nuevoInventario;
        // EL SECRETO: En lugar de recargar todo, mandamos un mensaje (Payload)
        if (listaCategorias != null && !listaCategorias.isEmpty()) {
            notifyItemRangeChanged(0, listaCategorias.size(), "ACTUALIZAR_INVENTARIO");
        }
    }

    @NonNull
    @Override
    public CategoriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carril_tienda, parent, false);
        return new CategoriaViewHolder(view);
    }

    // NUEVO MÉTODO: Atrapa el mensaje y evita recargar la vista entera (SALVA EL SCROLL)
    @Override
    public void onBindViewHolder(@NonNull CategoriaViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("ACTUALIZAR_INVENTARIO")) {
            // Solo avisamos a las tarjetas que actualicen sus números
            ComodinTiendaAdapter adapterHorizontal = (ComodinTiendaAdapter) holder.rvCarrilHorizontal.getAdapter();
            if (adapterHorizontal != null) {
                adapterHorizontal.actualizarInventario(inventarioGlobal);
            }
        } else {
            // Carga normal
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriaViewHolder holder, int position) {
        CategoriaTienda categoria = listaCategorias.get(position);

        holder.imgFondoCategoria.setImageResource(categoria.getFondoResId());
        holder.txtSubtituloCategoria.setText(categoria.getSubtitulo());

        ComodinTiendaAdapter adapterHorizontal = (ComodinTiendaAdapter) holder.rvCarrilHorizontal.getAdapter();

        if (adapterHorizontal == null) {
            adapterHorizontal = new ComodinTiendaAdapter(categoria.getComodines(), listener, () -> enCompra);
            LinearLayoutManager layoutManager = new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
            holder.rvCarrilHorizontal.setLayoutManager(layoutManager);
            holder.rvCarrilHorizontal.setHasFixedSize(false);
            holder.rvCarrilHorizontal.setItemViewCacheSize(2);
            holder.rvCarrilHorizontal.setAdapter(adapterHorizontal);
        } else {
            adapterHorizontal.setListaComodines(categoria.getComodines());
        }

        if (inventarioGlobal != null) {
            adapterHorizontal.actualizarInventario(inventarioGlobal);
        }
    }

    @Override
    public int getItemCount() {
        return listaCategorias != null ? listaCategorias.size() : 0;
    }

    public static class CategoriaViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFondoCategoria;
        TextView txtSubtituloCategoria;
        RecyclerView rvCarrilHorizontal;

        public CategoriaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFondoCategoria = itemView.findViewById(R.id.imgFondoCategoria);
            txtSubtituloCategoria = itemView.findViewById(R.id.txtSubtituloCategoria);
            rvCarrilHorizontal = itemView.findViewById(R.id.rvCarrilHorizontal);
        }
    }
}
