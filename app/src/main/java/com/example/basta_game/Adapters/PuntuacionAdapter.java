package com.example.basta_game.Adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.R;
import com.example.basta_game.models.PuntuacionJugador;

import java.util.List;

public class PuntuacionAdapter extends RecyclerView.Adapter<PuntuacionAdapter.ViewHolder> {

    private List<PuntuacionJugador> listaPuntuaciones;
    private int cantidadRondas;

    public PuntuacionAdapter(List<PuntuacionJugador> listaPuntuaciones, int cantidadRondas) {
        this.listaPuntuaciones = listaPuntuaciones;
        this.cantidadRondas = cantidadRondas;
    }

    @NonNull
    @Override
    public PuntuacionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fila_puntuacion, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PuntuacionAdapter.ViewHolder holder, int position) {
        PuntuacionJugador jugador = listaPuntuaciones.get(position);

        holder.txtPosicion.setText(String.valueOf(jugador.getPosicion()));
        holder.txtNombre.setText(jugador.getNombre());
        holder.txtTotal.setText(String.valueOf(jugador.getTotal()));

        holder.contenedorPuntosRondas.removeAllViews();

        List<Integer> puntajes = jugador.getPuntajesRondas();

        for (int i = 0; i < cantidadRondas; i++) {
            String textoPuntos = "-";

            if (puntajes != null && i < puntajes.size() && puntajes.get(i) != null) {
                textoPuntos = String.valueOf(puntajes.get(i));
            }

            TextView txtPuntos = crearCeldaPuntos(holder.itemView.getContext(), textoPuntos);
            holder.contenedorPuntosRondas.addView(txtPuntos);
        }
    }

    private TextView crearCeldaPuntos(Context context, String texto) {
        TextView textView = new TextView(context);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(context, 60),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        textView.setLayoutParams(params);
        textView.setText(texto);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(16);
        textView.setTextColor(ContextCompat.getColor(context, R.color.colorContenidoInputs));
        textView.setSingleLine(true);

        return textView;
    }

    private int dp(Context context, int valor) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(valor * density);
    }

    @Override
    public int getItemCount() {
        return listaPuntuaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtPosicion;
        TextView txtNombre;
        TextView txtTotal;
        LinearLayout contenedorPuntosRondas;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtPosicion = itemView.findViewById(R.id.txtPosicion);
            txtNombre = itemView.findViewById(R.id.txtNombreJugadorTabla);
            txtTotal = itemView.findViewById(R.id.txtTotalPuntos);
            contenedorPuntosRondas = itemView.findViewById(R.id.contenedorPuntosRondas);
        }
    }
}