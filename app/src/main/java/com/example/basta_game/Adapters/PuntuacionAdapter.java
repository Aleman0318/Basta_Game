package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.R;
import com.example.basta_game.models.PuntuacionDummy;
import java.util.List;

public class PuntuacionAdapter extends RecyclerView.Adapter<PuntuacionAdapter.ViewHolder> {

    private List<PuntuacionDummy> listaPuntuaciones;

    public PuntuacionAdapter(List<PuntuacionDummy> listaPuntuaciones) {
        this.listaPuntuaciones = listaPuntuaciones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fila_puntuacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PuntuacionDummy jugador = listaPuntuaciones.get(position);

        holder.txtPosicion.setText(String.valueOf(jugador.posicion));
        holder.txtNombre.setText(jugador.nombre);
        holder.txtR1.setText(String.valueOf(jugador.r1));
        holder.txtR2.setText(String.valueOf(jugador.r2));
        holder.txtR3.setText(String.valueOf(jugador.r3));
        holder.txtR4.setText(String.valueOf(jugador.r4));
        holder.txtR5.setText(String.valueOf(jugador.r5));
        holder.txtTotal.setText(String.valueOf(jugador.total));
    }

    @Override
    public int getItemCount() {
        return listaPuntuaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtPosicion, txtNombre, txtR1, txtR2, txtR3, txtR4, txtR5, txtTotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPosicion = itemView.findViewById(R.id.txtPosicion);
            txtNombre = itemView.findViewById(R.id.txtNombreJugadorTabla);
            txtR1 = itemView.findViewById(R.id.txtR1);
            txtR2 = itemView.findViewById(R.id.txtR2);
            txtR3 = itemView.findViewById(R.id.txtR3);
            txtR4 = itemView.findViewById(R.id.txtR4);
            txtR5 = itemView.findViewById(R.id.txtR5);
            txtTotal = itemView.findViewById(R.id.txtTotalPuntos);
        }
    }
}