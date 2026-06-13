package com.example.basta_game.Adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.basta_game.R;
import java.util.List;

public class TarjetasValidacionAdapter extends RecyclerView.Adapter<TarjetasValidacionAdapter.ViewHolder> {

    private List<String> categorias;
    private ViewPager2 viewPager2;
    private Runnable onUltimaCategoriaRevelada;
    private boolean[] categoriasReveladas;

    private ValidacionAdapter[] subAdaptadores;

    public TarjetasValidacionAdapter(List<String> categorias, ViewPager2 viewPager2, Runnable onUltimaCategoriaRevelada) {
        this.categorias = categorias;
        this.viewPager2 = viewPager2;
        this.onUltimaCategoriaRevelada = onUltimaCategoriaRevelada;
        this.categoriasReveladas = new boolean[categorias.size()];
        this.subAdaptadores = new ValidacionAdapter[categorias.size()];

        for(int i = 0; i < categorias.size(); i++) {
            subAdaptadores[i] = new ValidacionAdapter(4);
        }

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                iniciarAnimacionEnPagina(position);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tarjeta_revision, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtNombreCategoria.setText(categorias.get(position));

        holder.rvRespuestas.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.rvRespuestas.setAdapter(subAdaptadores[position]);

        if (categoriasReveladas[position]) {
            subAdaptadores[position].revelarTodos();
            if (position == categorias.size() - 1) {
                holder.txtDeslizar.setText("¡Revisión terminada!");
                holder.txtDeslizar.setVisibility(View.VISIBLE);
            } else {
                holder.txtDeslizar.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.txtDeslizar.setVisibility(View.INVISIBLE);
        }
    }

    private void iniciarAnimacionEnPagina(int position) {
        if (categoriasReveladas[position]) return;

        ValidacionAdapter adaptadorJugadores = subAdaptadores[position];
        viewPager2.setUserInputEnabled(false);

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable revelarRunnable = new Runnable() {
            @Override
            public void run() {
                if (viewPager2.getCurrentItem() != position) return;

                if (!adaptadorJugadores.estanTodosRevelados()) {
                    adaptadorJugadores.revelarSiguiente();
                    handler.postDelayed(this, 2000);
                } else {
                    categoriasReveladas[position] = true;

                    if (position == categorias.size() - 1) {
                        viewPager2.setUserInputEnabled(true);
                        notifyItemChanged(position);
                        if (onUltimaCategoriaRevelada != null) onUltimaCategoriaRevelada.run();
                    } else {
                        viewPager2.setCurrentItem(position + 1, true);
                    }
                }
            }
        };
        handler.postDelayed(revelarRunnable, 1000);
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombreCategoria, txtDeslizar;
        RecyclerView rvRespuestas;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombreCategoria = itemView.findViewById(R.id.txtNombreCategoriaTarjeta);
            txtDeslizar = itemView.findViewById(R.id.txtDeslizarTarjeta);
            rvRespuestas = itemView.findViewById(R.id.rvRespuestasTarjeta);
        }
    }
}