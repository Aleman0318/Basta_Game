package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.basta_game.R;

public class ValidacionAdapter extends RecyclerView.Adapter<ValidacionAdapter.ViewHolder> {

    private final int cantidadTotalElementos;
    private int elementosRevelados = 0;

    public ValidacionAdapter(int cantidadTotalElementos) {
        this.cantidadTotalElementos = cantidadTotalElementos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_respuestas, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        View btnValidar = holder.itemView.findViewById(R.id.btnValidarAccion);
        TextView txtPuntos = holder.itemView.findViewById(R.id.txtPuntuacionValidacion);

        if (position < elementosRevelados) {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.txtUsuario.setText("Jugador " + (position + 1));
            holder.txtRespuesta.setText("Respuesta " + (position + 1));

            if (position == elementosRevelados - 1 && elementosRevelados <= cantidadTotalElementos) {
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
        return cantidadTotalElementos;
    }

    public void revelarSiguiente() {
        if (elementosRevelados <= cantidadTotalElementos) {
            elementosRevelados++;
            notifyDataSetChanged();
        }
    }

    public boolean estanTodosRevelados() {
        return elementosRevelados > cantidadTotalElementos;
    }

    public void revelarTodos() {
        elementosRevelados = cantidadTotalElementos + 1;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUsuario, txtRespuesta;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUsuario = itemView.findViewById(R.id.txtUsuarioValidacion);
            txtRespuesta = itemView.findViewById(R.id.txtRespuestaValidacion);
        }
    }
}