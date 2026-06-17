package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.basta_game.R;
import com.example.basta_game.models.RespuestaValidacion;

import java.util.List;

public class ValidacionAdapter extends RecyclerView.Adapter<ValidacionAdapter.ViewHolder> {

    private final List<RespuestaValidacion> respuestas;
    private int elementosRevelados = 0;

    public ValidacionAdapter(List<RespuestaValidacion> respuestas) {
        this.respuestas = respuestas;
    }

    @NonNull
    @Override
    public ValidacionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_respuestas, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ValidacionAdapter.ViewHolder holder, int position) {
        View btnValidar = holder.itemView.findViewById(R.id.btnValidarAccion);
        TextView txtPuntos = holder.itemView.findViewById(R.id.txtPuntuacionValidacion);

        if (position < elementosRevelados) {
            holder.itemView.setVisibility(View.VISIBLE);

            RespuestaValidacion respuesta = respuestas.get(position);

            holder.txtUsuario.setText(respuesta.getNombreJugador());

            if (respuesta.getRespuesta() == null || respuesta.getRespuesta().trim().isEmpty()) {
                holder.txtRespuesta.setText("Sin respuesta");
            } else {
                holder.txtRespuesta.setText(respuesta.getRespuesta());
            }

            Glide.with(holder.itemView.getContext())
                    .load(respuesta.getFotoPerfilUrl())
                    .placeholder(R.drawable.ic_siluetas)
                    .error(R.drawable.ic_siluetas)
                    .into(holder.imgAvatar);

            if (position == elementosRevelados - 1 && elementosRevelados <= respuestas.size()) {
                btnValidar.setVisibility(View.VISIBLE);
                txtPuntos.setVisibility(View.GONE);
            } else {
                btnValidar.setVisibility(View.GONE);
                txtPuntos.setVisibility(View.VISIBLE);
            }

        } else {
            holder.itemView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return respuestas.size();
    }

    public void revelarSiguiente() {
        if (elementosRevelados <= respuestas.size()) {
            elementosRevelados++;
            notifyDataSetChanged();
        }
    }

    public boolean estanTodosRevelados() {
        return elementosRevelados > respuestas.size();
    }

    public void revelarTodos() {
        elementosRevelados = respuestas.size() + 1;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUsuario, txtRespuesta;
        ImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUsuario = itemView.findViewById(R.id.txtUsuarioValidacion);
            txtRespuesta = itemView.findViewById(R.id.txtRespuestaValidacion);
            imgAvatar = itemView.findViewById(R.id.imgAvatarValidacion);
        }
    }
}