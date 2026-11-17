package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.ComodinPartida;

import java.util.List;

public class ComodinPartidaAdapter extends RecyclerView.Adapter<ComodinPartidaAdapter.ViewHolder> {

    public interface OnUsarComodinListener {
        void onUsar(ComodinPartida comodin);
    }

    private final List<ComodinPartida> listaComodines;
    private final OnUsarComodinListener listener;
    private final java.util.function.Predicate<ComodinPartida> disponible;

    public ComodinPartidaAdapter(
            List<ComodinPartida> listaComodines,
            OnUsarComodinListener listener,
            java.util.function.Predicate<ComodinPartida> disponible
    ) {
        this.listaComodines = listaComodines;
        this.listener = listener;
        this.disponible = disponible;
    }

    @NonNull
    @Override
    public ComodinPartidaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comodin_partida, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComodinPartidaAdapter.ViewHolder holder, int position) {
        ComodinPartida comodin = listaComodines.get(position);

        holder.imgIcono.setImageResource(comodin.getIconoResId());
        holder.txtNombre.setText(comodin.getNombre());
        holder.txtDescripcion.setText(comodin.getDescripcion());
        holder.txtCantidad.setText("Tienes: " + comodin.getCantidad());
        holder.txtRareza.setText(comodin.getRareza());
        boolean resultados = com.example.basta_game.utils.FaseComodin.deResultados(comodin.getId());
        String tituloGrupo=resultados ? "★ EN RESULTADOS ★" : "★ DURANTE LA PARTIDA ★";
        android.text.SpannableString titulo=new android.text.SpannableString(tituloGrupo);
        titulo.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#FFD54F")),0,1,0);
        titulo.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#FFD54F")),titulo.length()-1,titulo.length(),0);
        holder.txtGrupo.setText(titulo);
        holder.txtGrupo.setVisibility(position == 0 || resultados !=
                com.example.basta_game.utils.FaseComodin.deResultados(listaComodines.get(position - 1).getId())
                ? View.VISIBLE : View.GONE);
        holder.txtCantidad.setText("Tienes: " + comodin.getCantidad());
        holder.txtDisponibilidad.setText(resultados ? "Entre rondas · Solo si queda otra ronda" : "Mientras respondes");
        int acento=android.graphics.Color.parseColor(comodin.getRareza().equals("Control") ? "#159AD6" : comodin.isPremium() ? "#C99316" : "#8E4DCD");
        int suave=android.graphics.Color.parseColor(comodin.getRareza().equals("Control") ? "#DDF4FF" : comodin.isPremium() ? "#FFF1B5" : "#EBDFFF");
        float dp=holder.itemView.getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable badge=new android.graphics.drawable.GradientDrawable();
        badge.setColor(suave); badge.setCornerRadius(14*dp); badge.setStroke(Math.round(2*dp),acento);
        holder.txtRareza.setBackground(badge);
        android.graphics.drawable.GradientDrawable fondo=new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{suave,android.graphics.Color.parseColor("#FFFDF6"),android.graphics.Color.WHITE});
        fondo.setCornerRadius(20*dp); holder.itemView.findViewById(R.id.cuerpoComodin).setBackground(fondo);

        boolean habilitado=comodin.isImplementado() && disponible.test(comodin);
        holder.imgIcono.setAlpha(habilitado ? 1f : .65f);

        if (comodin.isImplementado() && disponible.test(comodin)) {
            // Validamos si el botón soporta texto (por si usaste la imagen del diseñador en el XML)
            if (holder.btnUsar instanceof TextView) {
                ((TextView) holder.btnUsar).setText("USAR");
            }
            holder.btnUsar.setEnabled(true);
            holder.btnUsar.setAlpha(1f);
        } else {
            if (holder.btnUsar instanceof TextView) {
                ((TextView) holder.btnUsar).setText(comodin.isImplementado() ? "NO DISPONIBLE" : "PRONTO");
            }
            holder.btnUsar.setEnabled(false); // Deshabilitamos el clic para los no implementados
            holder.btnUsar.setAlpha(1f);
        }
        if(holder.btnUsar instanceof TextView) {
            ((TextView)holder.btnUsar).setTextColor(habilitado ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#283062"));
            ((TextView)holder.btnUsar).setTextSize(habilitado ? 17 : 14);
            ((TextView)holder.btnUsar).setShadowLayer(habilitado ? 2*dp : 0,0,dp,android.graphics.Color.parseColor("#101443"));
        }

        holder.btnUsar.setOnClickListener(v -> {
            if (listener != null && comodin.isImplementado() && disponible.test(comodin)) {
                listener.onUsar(comodin);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaComodines.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        View cardComodin; // <--- Cambiado a View genérico para evitar errores
        ImageView imgIcono;
        TextView txtNombre;
        TextView txtDescripcion;
        TextView txtCantidad;
        TextView txtRareza;
        TextView txtGrupo;
        TextView txtDisponibilidad;
        View btnUsar; // <--- Cambiado a View genérico

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardComodin = itemView.findViewById(R.id.cardComodinPartidaItem);
            imgIcono = itemView.findViewById(R.id.imgIconoComodinPartidaItem);
            txtNombre = itemView.findViewById(R.id.txtNombreComodinPartidaItem);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcionComodinPartidaItem);
            txtCantidad = itemView.findViewById(R.id.txtCantidadComodinPartidaItem);
            txtRareza = itemView.findViewById(R.id.txtRarezaComodinPartidaItem);
            txtGrupo = itemView.findViewById(R.id.txtGrupoComodin);
            txtDisponibilidad = itemView.findViewById(R.id.txtDisponibilidadComodin);
            btnUsar = itemView.findViewById(R.id.btnUsarComodinPartidaItem);
        }
    }
}
