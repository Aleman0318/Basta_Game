package com.example.basta_game.Adapters;

import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.ComodinTienda;
import com.google.android.material.card.MaterialCardView;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComodinTiendaAdapter extends RecyclerView.Adapter<ComodinTiendaAdapter.ViewHolder> {

    private List<ComodinTienda> listaComodines;
    private OnComprarClickListener listener;
    private Map<String, Integer> inventarioActual;

    private Set<Integer> itemsExpandidos = new HashSet<>();

    public interface OnComprarClickListener {
        void onComprarClick(ComodinTienda comodin);
    }

    public ComodinTiendaAdapter(List<ComodinTienda> listaComodines, OnComprarClickListener listener) {
        this.listaComodines = listaComodines;
        this.listener = listener;
    }

    public void actualizarInventario(Map<String, Integer> nuevoInventario) {
        this.inventarioActual = nuevoInventario;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comodin_tienda, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComodinTienda comodin = listaComodines.get(position);

        holder.txtNombre.setText(comodin.getNombre());
        holder.txtDescripcion.setText(comodin.getDescripcion());
        holder.txtPrecio.setText(String.valueOf(comodin.getPrecio()));
        holder.imgIcono.setImageResource(comodin.getIconoResId());

        if (comodin.getTipoMoneda().equalsIgnoreCase("gemas")) {
            holder.imgTipoMoneda.setImageResource(R.drawable.ic_gemas);
            holder.txtPrecio.setTextColor(Color.parseColor("#1273A8"));
        } else {
            holder.imgTipoMoneda.setImageResource(R.drawable.ic_monedas);
            holder.txtPrecio.setTextColor(Color.parseColor("#9A6400"));
        }

        int cantidad = 0;
        if (inventarioActual != null && inventarioActual.containsKey(comodin.getId())) {
            cantidad = inventarioActual.get(comodin.getId());
        }
        holder.txtCantidad.setText("TIENES: " + cantidad);

        int resIdHeader;
        String colorBorde;
        String colorFondoIcono;

        switch (comodin.getRareza().toLowerCase()) {
            case "control":
                resIdHeader = R.drawable.bg_rareza_control;
                colorBorde = "#1273A8";
                colorFondoIcono = "#E1F5FE";
                break;
            case "riesgo":
                resIdHeader = R.drawable.bg_rareza_riesgo;
                colorBorde = "#E65100";
                colorFondoIcono = "#FFF3E0";
                break;
            case "caótico":
            case "caotico":
            case "troll":
                resIdHeader = R.drawable.bg_rareza_caotico;
                colorBorde = "#4A148C";
                colorFondoIcono = "#F3E5F5";
                break;
            case "premium":
            case "legendario":
                resIdHeader = R.drawable.bg_rareza_premium;
                colorBorde = "#FF8F00";
                colorFondoIcono = "#FFF8E1";
                break;
            default:
                resIdHeader = R.drawable.bg_rareza_control;
                colorBorde = "#17235F";
                colorFondoIcono = "#F0F5FF";
                break;
        }

        holder.imgHeader.setImageResource(resIdHeader);
        holder.cardPadre.setStrokeColor(Color.parseColor(colorBorde));
        holder.cardIcono.setStrokeColor(Color.parseColor(colorBorde));
        holder.cardIcono.setCardBackgroundColor(Color.parseColor(colorFondoIcono));
        holder.cardInventario.setStrokeColor(Color.parseColor(colorBorde));
        holder.txtCantidad.setTextColor(Color.parseColor(colorBorde));

        holder.rowRegla1.setVisibility(View.GONE);
        holder.rowRegla2.setVisibility(View.GONE);
        holder.rowRegla3.setVisibility(View.GONE);
        holder.divRegla1.setVisibility(View.GONE);
        holder.divRegla2.setVisibility(View.GONE);

        if (comodin.getEstadisticas() != null) {
            String[] reglas = comodin.getEstadisticas().split("\n");

            if (reglas.length > 0 && !reglas[0].trim().isEmpty()) {
                holder.rowRegla1.setVisibility(View.VISIBLE);
                holder.txtRegla1.setText(Html.fromHtml(reglas[0]));
                holder.imgRegla1.setImageResource(obtenerIconoRegla(reglas[0]));
            }
            if (reglas.length > 1 && !reglas[1].trim().isEmpty()) {
                holder.divRegla1.setVisibility(View.VISIBLE);
                holder.rowRegla2.setVisibility(View.VISIBLE);
                holder.txtRegla2.setText(Html.fromHtml(reglas[1]));
                holder.imgRegla2.setImageResource(obtenerIconoRegla(reglas[1]));
            }
            if (reglas.length > 2 && !reglas[2].trim().isEmpty()) {
                holder.divRegla2.setVisibility(View.VISIBLE);
                holder.rowRegla3.setVisibility(View.VISIBLE);
                holder.txtRegla3.setText(Html.fromHtml(reglas[2]));
                holder.imgRegla3.setImageResource(obtenerIconoRegla(reglas[2]));
            }
        }

        boolean isExpanded = itemsExpandidos.contains(position);
        holder.layoutEstadisticas.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.txtFlecha.animate().rotation(isExpanded ? 180f : 0f).setDuration(250).start();

        holder.btnDesplegar.setOnClickListener(v -> {
            if (isExpanded) {
                itemsExpandidos.remove(position);
            } else {
                itemsExpandidos.add(position);
            }
            notifyItemChanged(position);
        });

        holder.btnComprar.setOnClickListener(v -> {
            v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(250).setInterpolator(new OvershootInterpolator()).start();
                if (listener != null) listener.onComprarClick(comodin);
            }).start();
        });
    }

    /**
     * DETECTOR DE CONTEXTO ACTUALIZADO
     * Lee la regla y decide qué icono de tu diseñadora usar.
     */
    private int obtenerIconoRegla(String textoHTML) {
        String texto = textoHTML.toLowerCase();

        // 1. Reloj: Tiempo, duración, límites
        if (texto.contains("duración") || texto.contains("segundos") || texto.contains("límite")) {
            return R.drawable.ic_reloj_comodines;
        }
        // 2. Efecto: Eventos y descripciones de qué hace
        else if (texto.contains("efecto") || texto.contains("evento")) {
            return R.drawable.ic_efecto_comodines;
        }
        // 3. Trofeo: Recompensas, primeros lugares, éxito
        else if (texto.contains("top") || texto.contains("recompensa") || texto.contains("1er lugar")) {
            return R.drawable.ic_trofeo_comodines;
        }
        // 4. Usuarios: Multijugador, rivales, todos
        else if (texto.contains("jug.") || texto.contains("todos") || texto.contains("rival") || texto.contains("rivales")) {
            return R.drawable.ic_usuarios_comodines;
        }
        // 5. Espadas: Fracasos, perder puntos, ataques
        else if (texto.contains("fracaso") || texto.contains("perder") || texto.contains("robar")) {
            return R.drawable.ic_espadas_comodines;
        }
        // 6. Escudo: Porcentajes, riesgos, protección pasiva
        else if (texto.contains("%") || texto.contains("protege") || texto.contains("riesgo") || texto.contains("pasiva") || texto.contains("requisito")) {
            return R.drawable.ic_escudo_comodines;
        }
        // 7. Fallback (Por defecto)
        else {
            return R.drawable.ic_dinero_comodines;
        }
    }

    @Override
    public int getItemCount() {
        return listaComodines != null ? listaComodines.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardPadre, cardIcono, cardInventario, layoutEstadisticas;
        ImageView imgHeader, imgIcono, imgTipoMoneda, btnComprar;
        LinearLayout btnDesplegar, layoutPrecio;
        TextView txtNombre, txtDescripcion, txtCantidad, txtPrecio, txtFlecha;

        LinearLayout rowRegla1, rowRegla2, rowRegla3;
        ImageView imgRegla1, imgRegla2, imgRegla3;
        TextView txtRegla1, txtRegla2, txtRegla3;
        View divRegla1, divRegla2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPadre = itemView.findViewById(R.id.cardComodinTiendaItem);
            cardIcono = itemView.findViewById(R.id.cardIconoComodinItem);
            cardInventario = itemView.findViewById(R.id.cardInventarioItem);
            layoutEstadisticas = itemView.findViewById(R.id.layoutEstadisticasOcultas);

            imgHeader = itemView.findViewById(R.id.imgHeaderRareza);
            txtNombre = itemView.findViewById(R.id.txtNombreComodinItem);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcionComodinItem);

            btnDesplegar = itemView.findViewById(R.id.btnDesplegarReglas);
            txtFlecha = itemView.findViewById(R.id.txtFlechaDesplegable);
            layoutPrecio = itemView.findViewById(R.id.layoutPrecio);

            txtCantidad = itemView.findViewById(R.id.txtCantidadInventarioItem);
            txtPrecio = itemView.findViewById(R.id.txtPrecioComodinItem);
            imgIcono = itemView.findViewById(R.id.imgIconoComodinItem);
            imgTipoMoneda = itemView.findViewById(R.id.imgTipoMonedaComodinItem);
            btnComprar = itemView.findViewById(R.id.btnComprarComodinItem);

            rowRegla1 = itemView.findViewById(R.id.rowRegla1);
            rowRegla2 = itemView.findViewById(R.id.rowRegla2);
            rowRegla3 = itemView.findViewById(R.id.rowRegla3);

            imgRegla1 = itemView.findViewById(R.id.imgRegla1);
            imgRegla2 = itemView.findViewById(R.id.imgRegla2);
            imgRegla3 = itemView.findViewById(R.id.imgRegla3);

            txtRegla1 = itemView.findViewById(R.id.txtRegla1);
            txtRegla2 = itemView.findViewById(R.id.txtRegla2);
            txtRegla3 = itemView.findViewById(R.id.txtRegla3);

            divRegla1 = itemView.findViewById(R.id.divRegla1);
            divRegla2 = itemView.findViewById(R.id.divRegla2);
        }
    }
}