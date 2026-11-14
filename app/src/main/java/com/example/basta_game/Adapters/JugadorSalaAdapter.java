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

import com.bumptech.glide.Glide;
import com.example.basta_game.R;
import com.example.basta_game.models.JugadorSala;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class JugadorSalaAdapter extends RecyclerView.Adapter<JugadorSalaAdapter.ViewHolder> {

    private List<JugadorSala> listaJugadores;

    public JugadorSalaAdapter(List<JugadorSala> listaJugadores) {
        this.listaJugadores = listaJugadores;
    }

    @NonNull
    @Override
    public JugadorSalaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_jugador, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JugadorSalaAdapter.ViewHolder holder, int position) {
        JugadorSala jugador = listaJugadores.get(position);

        holder.tvNombre.setText(com.example.basta_game.utils.IdentidadJugador.nombreValido(jugador.getNombre())
                ? jugador.getNombre() : "Cargando perfil...");

        Glide.with(holder.itemView.getContext())
                .load(jugador.getUrlAvatar())
                .placeholder(R.drawable.outline_person_24)
                .error(R.drawable.outline_person_24)
                .into(holder.imgAvatar);

        if (!jugador.isEstaConectado()) {
            aplicarEstadoDesconectado(holder);
        } else {
            if (jugador.isEstaListo()) {
                aplicarEstadoListo(holder);
            } else {
                aplicarEstadoEsperando(holder);
            }
        }
    }

    private void aplicarEstadoListo(ViewHolder holder) {
        holder.cardJugadorLobby.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#F6FFF6"))
        );
        holder.cardJugadorLobby.setStrokeColor(
                ColorStateList.valueOf(Color.parseColor("#BDE8B8"))
        );

        holder.cardEstado.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#2AA514"))
        );

        holder.cardEstadoPill.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#E9FBE8"))
        );
        holder.cardEstadoPill.setStrokeColor(
                ColorStateList.valueOf(Color.parseColor("#74D66A"))
        );

        holder.tvIconoEstado.setText("●");
        holder.tvIconoEstado.setTextColor(Color.parseColor("#2AA514"));

        holder.tvEstadoJugador.setText("Listo");
        holder.tvEstadoJugador.setTextColor(Color.parseColor("#2AA514"));

        holder.tvSubtitulo.setText("Preparado para jugar");
        holder.tvSubtitulo.setTextColor(Color.parseColor("#4B8B4B"));
    }

    private void aplicarEstadoEsperando(ViewHolder holder) {
        holder.cardJugadorLobby.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#FCFDFF"))
        );
        holder.cardJugadorLobby.setStrokeColor(
                ColorStateList.valueOf(Color.parseColor("#BFD0FF"))
        );

        holder.cardEstado.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#0017ED"))
        );

        holder.cardEstadoPill.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#EEF4FF"))
        );
        holder.cardEstadoPill.setStrokeColor(
                ColorStateList.valueOf(Color.parseColor("#BFD0FF"))
        );

        holder.tvIconoEstado.setText("●");
        holder.tvIconoEstado.setTextColor(Color.parseColor("#0017ED"));

        holder.tvEstadoJugador.setText("Esperando");
        holder.tvEstadoJugador.setTextColor(Color.parseColor("#0017ED"));

        holder.tvSubtitulo.setText("Aún no está listo");
        holder.tvSubtitulo.setTextColor(Color.parseColor("#7596A7"));
    }

    private void aplicarEstadoDesconectado(ViewHolder holder) {
        holder.cardJugadorLobby.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#F8FAFC"))
        );
        holder.cardJugadorLobby.setStrokeColor(
                ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
        );

        holder.cardEstado.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        );

        holder.cardEstadoPill.setCardBackgroundColor(
                ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
        );
        holder.cardEstadoPill.setStrokeColor(
                ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
        );

        holder.tvIconoEstado.setText("●");
        holder.tvIconoEstado.setTextColor(Color.parseColor("#64748B"));

        holder.tvEstadoJugador.setText("Reconectando...");
        holder.tvEstadoJugador.setTextColor(Color.parseColor("#64748B"));

        holder.tvSubtitulo.setText("Conservando su lugar");
        holder.tvSubtitulo.setTextColor(Color.parseColor("#94A3B8"));
    }

    @Override
    public int getItemCount() {
        return listaJugadores.size();
    }

    public void actualizarLista(List<JugadorSala> nuevaLista) {
        this.listaJugadores = nuevaLista;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvNombre;
        TextView tvSubtitulo;
        TextView tvEstadoJugador;
        TextView tvIconoEstado;

        MaterialCardView cardJugadorLobby;
        MaterialCardView cardEstado;
        MaterialCardView cardEstadoPill;

        ImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardJugadorLobby = itemView.findViewById(R.id.cardJugadorLobby);
            tvNombre = itemView.findViewById(R.id.tvNombreJugador);
            tvSubtitulo = itemView.findViewById(R.id.tvSubtituloJugador);

            cardEstado = itemView.findViewById(R.id.cardEstadoJugador);
            cardEstadoPill = itemView.findViewById(R.id.cardEstadoPill);

            tvEstadoJugador = itemView.findViewById(R.id.tvEstadoJugador);
            tvIconoEstado = itemView.findViewById(R.id.tvIconoEstadoJugador);

            imgAvatar = itemView.findViewById(R.id.ivAvatarJugador);
        }
    }
}
