package com.example.basta_game.Adapters;

import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Map;

public class CategoriaPartidaAdapter extends RecyclerView.Adapter<CategoriaPartidaAdapter.ViewHolder> {

    private List<Categoria> listaCategorias;
    private Map<String, String> respuestasJugador;

    public CategoriaPartidaAdapter(List<Categoria> listaCategorias, Map<String, String> respuestasJugador) {
        this.listaCategorias = listaCategorias;
        this.respuestasJugador = respuestasJugador;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_categoria_partida, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categoria cat = listaCategorias.get(position);

        holder.txtNombre.setText(cat.getNombre());
        holder.imgIcono.setImageResource(cat.getIconoId());

        if (holder.textWatcher != null) {
            holder.etRespuesta.removeTextChangedListener(holder.textWatcher);
        }

        String respuestaGuardada = respuestasJugador.get(cat.getNombre());
        holder.etRespuesta.setText(respuestaGuardada != null ? respuestaGuardada : "");

        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                respuestasJugador.put(cat.getNombre(), s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        holder.etRespuesta.addTextChangedListener(holder.textWatcher);
    }

    @Override
    public int getItemCount() {
        return listaCategorias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre;
        ImageView imgIcono;
        EditText etRespuesta;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombre = itemView.findViewById(R.id.txtNombreCategoriaPartida);
            imgIcono = itemView.findViewById(R.id.imgIconoCategoriaPartida);
            etRespuesta = itemView.findViewById(R.id.etRespuestaCategoriaPartida);
        }
    }
}