package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.basta_game.R;
import com.example.basta_game.models.HistorialPartida;
import java.util.List;
import java.util.Locale;

public class HistorialPartidaAdapter extends RecyclerView.Adapter<HistorialPartidaAdapter.ViewHolder> {

    private final List<HistorialPartida> lista;

    public HistorialPartidaAdapter(List<HistorialPartida> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_partida, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistorialPartida partida = lista.get(position);

        String codigo = partida.getCodigoSala();
        if (codigo == null || codigo.trim().isEmpty()) {
            codigo = "Sala";
        }

        holder.txtCodigoSala.setText(codigo);
        holder.txtTiempoHace.setText(formatearTiempoRelativo(partida.getFecha()));
        holder.txtRondas.setText(partida.getCantidadRondas() + " rondas");
        holder.txtJugadores.setText(partida.getJugadoresCantidad() + " jugadores");

        if (partida.getMiPosicion() == 1) {
            holder.txtResultado.setText("VICTORIA");
        } else {
            holder.txtResultado.setText("RESULTADO");
        }

        holder.txtMiPuestoPrincipal.setText("#" + partida.getMiPosicion());
        holder.txtMiPuntos.setText(partida.getMiTotal() + " pts");

        if (partida.getMonedas() > 0) {
            holder.txtMonedas.setText("+" + partida.getMonedas());
        } else {
            holder.txtMonedas.setText("+0");
        }

        ocultarTop(holder.layoutTop1);
        ocultarTop(holder.layoutTop2);
        ocultarTop(holder.layoutTop3);

        for (HistorialPartida.TopJugador jugador : partida.getTop3()) {
            if (jugador.getPosicion() == 1) {
                mostrarTopJugador(holder.layoutTop1, holder.imgTop1, holder.txtNombreTop1, holder.txtPuntosTop1, jugador);
            } else if (jugador.getPosicion() == 2) {
                mostrarTopJugador(holder.layoutTop2, holder.imgTop2, holder.txtNombreTop2, holder.txtPuntosTop2, jugador);
            } else if (jugador.getPosicion() == 3) {
                mostrarTopJugador(holder.layoutTop3, holder.imgTop3, holder.txtNombreTop3, holder.txtPuntosTop3, jugador);
            }
        }

        if (partida.getMiPosicion() > 3) {
            holder.layoutMiExtra.setVisibility(View.VISIBLE);

            String miNombre = partida.getMiNombre();
            if (miNombre == null || miNombre.trim().isEmpty()) {
                miNombre = "Tú";
            }

            holder.txtMiExtra.setText("Tu puesto: #" + partida.getMiPosicion() + "  " + miNombre);
        } else {
            holder.layoutMiExtra.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    private void mostrarTopJugador(
            LinearLayout layout,
            ImageView imagen,
            TextView nombre,
            TextView puntos,
            HistorialPartida.TopJugador jugador
    ) {
        layout.setVisibility(View.VISIBLE);

        String nombreJugador = jugador.getNombre();
        if (nombreJugador == null || nombreJugador.trim().isEmpty()) {
            nombreJugador = "Jugador";
        }

        nombre.setText(nombreJugador);
        puntos.setText(jugador.getTotal() + " pts");

        String avatarUrl = jugador.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Glide.with(imagen.getContext())
                    .load(avatarUrl)
                    .dontAnimate()
                    .placeholder(R.drawable.outline_person_24)
                    .error(R.drawable.outline_person_24)
                    .into(imagen);
        } else {
            imagen.setImageResource(R.drawable.outline_person_24);
        }
    }

    private void ocultarTop(LinearLayout layout) {
        layout.setVisibility(View.INVISIBLE);
    }

    private String formatearTiempoRelativo(long fecha) {
        if (fecha <= 0) {
            return "Hace poco";
        }

        long diferenciaMs = System.currentTimeMillis() - fecha;

        if (diferenciaMs < 0) {
            diferenciaMs = 0;
        }

        long segundos = diferenciaMs / 1000;
        long minutos = segundos / 60;
        long horas = minutos / 60;
        long dias = horas / 24;

        if (segundos < 60) {
            return "Hace " + segundos + "s";
        }

        if (minutos < 60) {
            return String.format(Locale.ROOT, "Hace %dm", minutos);
        }

        if (horas < 24) {
            return String.format(Locale.ROOT, "Hace %dh", horas);
        }

        return String.format(Locale.ROOT, "Hace %dd", dias);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCodigoSala, txtTiempoHace, txtResultado, txtRondas, txtJugadores;
        TextView txtMiPuestoPrincipal, txtMiPuntos, txtMonedas;
        TextView txtNombreTop1, txtNombreTop2, txtNombreTop3;
        TextView txtPuntosTop1, txtPuntosTop2, txtPuntosTop3;
        TextView txtMiExtra;
        ImageView imgTop1, imgTop2, imgTop3;
        LinearLayout layoutTop1, layoutTop2, layoutTop3, layoutMiExtra;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtCodigoSala = itemView.findViewById(R.id.txtCodigoSalaHistorialItem);
            txtTiempoHace = itemView.findViewById(R.id.txtTiempoHaceHistorialItem);
            txtResultado = itemView.findViewById(R.id.txtResultadoHistorialItem);
            txtRondas = itemView.findViewById(R.id.txtRondasHistorialItem);
            txtJugadores = itemView.findViewById(R.id.txtJugadoresHistorialItem);

            txtMiPuestoPrincipal = itemView.findViewById(R.id.txtMiPuestoPrincipalHistorialItem);
            txtMiPuntos = itemView.findViewById(R.id.txtMiPuntosHistorialItem);
            txtMonedas = itemView.findViewById(R.id.txtMonedasHistorialItem);

            layoutTop1 = itemView.findViewById(R.id.layoutTop1HistorialItem);
            layoutTop2 = itemView.findViewById(R.id.layoutTop2HistorialItem);
            layoutTop3 = itemView.findViewById(R.id.layoutTop3HistorialItem);

            imgTop1 = itemView.findViewById(R.id.imgTop1HistorialItem);
            imgTop2 = itemView.findViewById(R.id.imgTop2HistorialItem);
            imgTop3 = itemView.findViewById(R.id.imgTop3HistorialItem);

            txtNombreTop1 = itemView.findViewById(R.id.txtNombreTop1HistorialItem);
            txtNombreTop2 = itemView.findViewById(R.id.txtNombreTop2HistorialItem);
            txtNombreTop3 = itemView.findViewById(R.id.txtNombreTop3HistorialItem);

            txtPuntosTop1 = itemView.findViewById(R.id.txtPuntosTop1HistorialItem);
            txtPuntosTop2 = itemView.findViewById(R.id.txtPuntosTop2HistorialItem);
            txtPuntosTop3 = itemView.findViewById(R.id.txtPuntosTop3HistorialItem);

            layoutMiExtra = itemView.findViewById(R.id.layoutMiExtraHistorialItem);
            txtMiExtra = itemView.findViewById(R.id.txtMiExtraHistorialItem);
        }
    }
}
