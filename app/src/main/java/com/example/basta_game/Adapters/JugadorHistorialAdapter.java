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
import com.google.android.material.card.MaterialCardView;

public class JugadorHistorialAdapter extends RecyclerView.Adapter<JugadorHistorialAdapter.ViewHolder> {

    //temporal pa la prueba visual
    public JugadorHistorialAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_jugador_historial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        //  datos ficticios pa visual
        holder.txtNombretarjetaJugador.setText("Jugador " + (position + 1));

        switch (position) {
            case 0:
                holder.tarjetaJugadorIcono.setVisibility(View.VISIBLE);
                holder.txtPosiciontarjetaJugador.setVisibility(View.GONE);

                holder.tarjetaJugadorEsqueleto.setStrokeColor(Color.parseColor("#2AA514"));
                holder.txtPuntostarjetaJugador.setText("740");
                break;

            case 1:
                holder.tarjetaJugadorIcono.setVisibility(View.GONE);
                holder.txtPosiciontarjetaJugador.setVisibility(View.VISIBLE);
                holder.txtPosiciontarjetaJugador.setText("2°");

                holder.tarjetaJugadorEsqueleto.setStrokeColor(Color.parseColor("#2795A9"));
                holder.txtPuntostarjetaJugador.setText("541");
                break;

            case 2:
                holder.tarjetaJugadorIcono.setVisibility(View.GONE);
                holder.txtPosiciontarjetaJugador.setVisibility(View.VISIBLE);
                holder.txtPosiciontarjetaJugador.setText("3°");

                holder.tarjetaJugadorEsqueleto.setStrokeColor(Color.parseColor("#FF5F00"));
                holder.txtPuntostarjetaJugador.setText("333");
                break;

            case 3:
                holder.tarjetaJugadorIcono.setVisibility(View.GONE);
                holder.txtPosiciontarjetaJugador.setVisibility(View.VISIBLE);
                holder.txtPosiciontarjetaJugador.setText("4°");

                holder.tarjetaJugadorEsqueleto.setStrokeColor(Color.parseColor("#ff2341"));
                holder.txtPuntostarjetaJugador.setText("331");
                break;

            default:
                holder.tarjetaJugadorIcono.setVisibility(View.GONE);
                holder.txtPosiciontarjetaJugador.setVisibility(View.VISIBLE);
                holder.txtPosiciontarjetaJugador.setText((position + 1) + "°");

                holder.tarjetaJugadorEsqueleto.setStrokeColor(Color.parseColor("#8E1600"));
                holder.txtPuntostarjetaJugador.setText("111");
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 8;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView tarjetaJugadorEsqueleto, tarjetaJugadorPuntos;
        ImageView tarjetaJugadorIcono, tarjetaJugadorAvatar;
        TextView txtPosiciontarjetaJugador, txtNombretarjetaJugador, txtPuntostarjetaJugador;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tarjetaJugadorEsqueleto = itemView.findViewById(R.id.tarjetaJugadorEsqueleto);
            tarjetaJugadorPuntos = itemView.findViewById(R.id.tarjetaJugadorPuntos);
            tarjetaJugadorIcono = itemView.findViewById(R.id.tarjetaJugadorIcono);
            tarjetaJugadorAvatar = itemView.findViewById(R.id.tarjetaJugadorAvatar);
            txtPosiciontarjetaJugador = itemView.findViewById(R.id.txtPosiciontarjetaJugador);
            txtNombretarjetaJugador = itemView.findViewById(R.id.txtNombretarjetaJugador);
            txtPuntostarjetaJugador = itemView.findViewById(R.id.txtPuntostarjetaJugador);
        }
    }
}