package com.example.basta_game.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.basta_game.R;
import com.example.basta_game.models.RespuestaValidacion;
import com.example.basta_game.utils.Animations;

import java.util.List;

public class ValidacionAdapter extends RecyclerView.Adapter<ValidacionAdapter.ViewHolder> {

    public interface OnSolicitarVotacionListener {
        void onSolicitarVotacion(RespuestaValidacion respuesta);
    }

    private final List<RespuestaValidacion> respuestas;
    private int elementosRevelados = 0;
    private OnSolicitarVotacionListener listener;

    public ValidacionAdapter(
            List<RespuestaValidacion> respuestas,
            OnSolicitarVotacionListener listener
    ) {
        this.respuestas = respuestas;
        this.listener = listener;
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

        btnValidar.setOnClickListener(null);

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

            boolean respuestaVacia =
                    respuesta.getRespuesta() == null ||
                            respuesta.getRespuesta().trim().isEmpty() ||
                            respuesta.getRespuesta().equalsIgnoreCase("Sin respuesta");

            btnValidar.setEnabled(!respuestaVacia);
            btnValidar.setAlpha(respuestaVacia ? 0.4f : 1f);

            btnValidar.setOnClickListener(v -> {
                if (!respuestaVacia && listener != null) {
                    listener.onSolicitarVotacion(respuesta);
                }
            });

            if (position == elementosRevelados - 1 && elementosRevelados <= respuestas.size()) {
                btnValidar.setVisibility(View.VISIBLE);
                txtPuntos.setVisibility(View.GONE);
                holder.txtRespuesta.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
                holder.txtRespuesta.setBackgroundResource(R.drawable.bg_input_juego);

                Animations.animacionLanzada(holder.txtUsuario);
                Animations.animacionLanzada(holder.txtRespuesta);
                Animations.animacionLanzada(holder.imgAvatar);
                Animations.animacionLanzada(btnValidar);

            } else {
                if (respuesta.isValidada()) {

                    int colorOutline;

                    if (respuesta.getPuntos() == 10) {
                        colorOutline = Color.parseColor("#4CAF50");
                    } else if (respuesta.getPuntos() == 5) {
                        colorOutline = Color.parseColor("#FFB300");
                    } else {
                        colorOutline = Color.parseColor("#F44336");
                    }

                    holder.txtRespuesta.setTextColor(colorOutline);

                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setColor(Color.WHITE);
                    gd.setCornerRadius(40f);
                    gd.setStroke(4, colorOutline);
                    holder.txtRespuesta.setBackground(gd);

                    if (!respuesta.isMostrarPuntaje()) {
                        btnValidar.setVisibility(View.VISIBLE);
                        txtPuntos.setVisibility(View.GONE);

                        if (!respuesta.isTimerIniciado()) {
                            respuesta.setTimerIniciado(true);

                            holder.itemView.postDelayed(() -> {
                                respuesta.setMostrarPuntaje(true);
                                notifyItemChanged(position);
                            }, 5000);
                        }

                    } else {
                        btnValidar.setVisibility(View.GONE);
                        txtPuntos.setVisibility(View.VISIBLE);
                        txtPuntos.setText("+" + respuesta.getPuntos());
                        txtPuntos.setTextColor(colorOutline);
                    }

                } else {
                    holder.txtRespuesta.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
                    holder.txtRespuesta.setBackgroundResource(R.drawable.bg_input_juego);
                    btnValidar.setVisibility(View.VISIBLE);
                    txtPuntos.setVisibility(View.GONE);
                }
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