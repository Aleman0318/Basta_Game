package com.example.basta_game.Adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

import java.util.List;
import java.util.Map;

public class ComodinTiendaAdapter extends RecyclerView.Adapter<ComodinTiendaAdapter.ViewHolder> {

    private List<ComodinTienda> listaComodines;
    private OnComprarClickListener listener;
    private Map<String, Integer> inventarioActual;

    public interface OnComprarClickListener {
        void onComprarClick(ComodinTienda comodin);
    }

    public ComodinTiendaAdapter(List<ComodinTienda> listaComodines, OnComprarClickListener listener) {
        this.listaComodines = listaComodines;
        this.listener = listener;
    }

    // EL SECRETO 2: La tarjeta avisa que solo va a cambiar "La cantidad"
    public void actualizarInventario(Map<String, Integer> nuevoInventario) {
        this.inventarioActual = nuevoInventario;
        if (listaComodines != null && !listaComodines.isEmpty()) {
            notifyItemRangeChanged(0, listaComodines.size(), "ACTUALIZAR_CANTIDAD");
        }
    }

    public void setListaComodines(List<ComodinTienda> nuevaLista) {
        this.listaComodines = nuevaLista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comodin_tienda, parent, false);
        return new ViewHolder(view);
    }

    // NUEVO MÉTODO: Cambia únicamente el texto de "TIENES" y NADA MÁS.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("ACTUALIZAR_CANTIDAD")) {
            ComodinTienda comodin = listaComodines.get(position);
            int cantidad = 0;
            if (inventarioActual != null && inventarioActual.containsKey(comodin.getId())) {
                cantidad = inventarioActual.get(comodin.getId());
            }
            holder.txtCantidad.setText("TIENES: " + cantidad);
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
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

        // 1. COLOR DE BORDE Y FONDO LIVIANO SEGÚN CATEGORÍA
        String colorBorde;
        String colorFondoLiviano;
        switch (comodin.getRareza().toLowerCase()) {
            case "control":
                colorBorde = "#1976D2"; colorFondoLiviano = "#E3F2FD"; break;
            case "riesgo":
                colorBorde = "#E65100"; colorFondoLiviano = "#FFF3E0"; break;
            case "caótico":
            case "caotico":
            case "troll":
                colorBorde = "#8E24AA"; colorFondoLiviano = "#F3E5F5"; break;
            case "premium":
            case "legendarias":
                colorBorde = "#388E3C"; colorFondoLiviano = "#E8F5E9"; break;
            default:
                colorBorde = "#0B4CE3"; colorFondoLiviano = "#F0F5FF"; break;
        }

        holder.cardPadre.setStrokeColor(Color.parseColor(colorBorde));

        holder.cardInventario.setStrokeColor(Color.parseColor(colorBorde));
        holder.cardInventario.setCardBackgroundColor(Color.parseColor(colorFondoLiviano));
        holder.txtCantidad.setTextColor(Color.parseColor(colorBorde));

        holder.cardIcono.setStrokeColor(Color.parseColor(colorBorde));
        holder.cardIcono.setCardBackgroundColor(Color.parseColor(colorFondoLiviano));

        if(holder.imgIconoInventario != null) {
            holder.imgIconoInventario.setColorFilter(Color.parseColor(colorBorde));
        }

        // 2. ASIGNAR ETIQUETA TÁCTICA
        String colorFondoIcono;
        switch (comodin.getEtiquetaTactiva().toLowerCase()) {
            case "ataque":
                holder.imgEtiquetaTactiva.setImageResource(R.drawable.bg_tag_ataque);
                colorFondoIcono = "#FFF3E0";
                break;
            case "defensa":
                holder.imgEtiquetaTactiva.setImageResource(R.drawable.bg_tag_defensa);
                colorFondoIcono = "#E3F2FD";
                break;
            case "efecto_global":
            case "global":
                holder.imgEtiquetaTactiva.setImageResource(R.drawable.bg_tag_global);
                colorFondoIcono = "#F3E5F5";
                break;
            case "estrategia":
            default:
                holder.imgEtiquetaTactiva.setImageResource(R.drawable.bg_tag_estrategia);
                colorFondoIcono = "#E8F5E9";
                break;
        }

        holder.btnDesplegar.setOnClickListener(v -> {
            mostrarModalEstadisticas(v.getContext(), comodin, colorFondoLiviano, colorBorde);
        });

        holder.btnComprar.setOnClickListener(v -> {
            v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(250).setInterpolator(new OvershootInterpolator()).start();
                if (listener != null) listener.onComprarClick(comodin);
            }).start();
        });
    }

    private void mostrarModalEstadisticas(Context context, ComodinTienda comodin, String colorFondo, String colorTema) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_estadisticas_comodin);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        View headerFondo = dialog.findViewById(R.id.headerFondoModal);
        ImageView btnCerrar = dialog.findViewById(R.id.btnCerrarModal);
        ImageView imgIcono = dialog.findViewById(R.id.imgIconoModal);
        TextView txtNombre = dialog.findViewById(R.id.txtNombreModal);
        TextView txtDesc = dialog.findViewById(R.id.txtDescModal);
        LinearLayout contenedorReglas = dialog.findViewById(R.id.contenedorReglasModal);

        headerFondo.setBackgroundColor(Color.parseColor(colorFondo));
        imgIcono.setImageResource(comodin.getIconoResId());
        txtNombre.setText(comodin.getNombre());
        txtDesc.setText(comodin.getDescripcion());

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        if (comodin.getEstadisticas() != null) {
            String[] reglas = comodin.getEstadisticas().split("\n");
            for (String regla : reglas) {
                if (regla.trim().isEmpty()) continue;
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(32, 16, 32, 16);

                ImageView icon = new ImageView(context);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(90, 90);
                icon.setLayoutParams(iconParams);
                icon.setImageResource(obtenerIconoRegla(regla));

                TextView text = new TextView(context);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                textParams.setMargins(32, 0, 0, 0);
                text.setLayoutParams(textParams);
                text.setText(Html.fromHtml(regla));
                text.setTextColor(Color.parseColor("#5E6C84"));
                text.setTextSize(13f);

                row.addView(icon);
                row.addView(text);

                View divider = new View(context);
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
                divParams.setMargins(48, 8, 48, 8);
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(Color.parseColor("#D6E0FF"));

                contenedorReglas.addView(row);
                contenedorReglas.addView(divider);
            }
        }
        dialog.show();
    }

    private int obtenerIconoRegla(String textoHTML) {
        String texto = textoHTML.toLowerCase();
        if (texto.contains("duración") || texto.contains("segundos") || texto.contains("límite")) return R.drawable.ic_reloj_comodines;
        if (texto.contains("efecto") || texto.contains("evento")) return R.drawable.ic_efecto_comodines;
        if (texto.contains("top") || texto.contains("recompensa") || texto.contains("1er lugar") || texto.contains("éxito")) return R.drawable.ic_trofeo_comodines;
        if (texto.contains("jug.") || texto.contains("todos") || texto.contains("rival") || texto.contains("rivales") || texto.contains("objetivo")) return R.drawable.ic_usuarios_comodines;
        if (texto.contains("fracaso") || texto.contains("perder") || texto.contains("robar") || texto.contains("castigo")) return R.drawable.ic_espadas_comodines;
        if (texto.contains("riesgo") || texto.contains("requisito") || texto.contains("seguro") || texto.contains("defensa") || texto.contains("pasiva")) return R.drawable.ic_escudo_comodines;
        return R.drawable.ic_dinero_comodines;
    }

    @Override
    public int getItemCount() {
        return listaComodines != null ? listaComodines.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardPadre, cardIcono, cardInventario, btnComprar;
        LinearLayout btnDesplegar, layoutPrecio;
        ImageView imgEtiquetaTactiva, imgIcono, imgIconoInventario, imgTipoMoneda;
        TextView txtNombre, txtDescripcion, txtCantidad, txtPrecio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPadre = itemView.findViewById(R.id.cardComodinTiendaItem);
            imgEtiquetaTactiva = itemView.findViewById(R.id.imgEtiquetaTactiva);
            cardIcono = itemView.findViewById(R.id.cardIconoComodinItem);
            imgIcono = itemView.findViewById(R.id.imgIconoComodinItem);
            txtNombre = itemView.findViewById(R.id.txtNombreComodinItem);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcionComodinItem);
            btnDesplegar = itemView.findViewById(R.id.btnDesplegarReglas);
            cardInventario = itemView.findViewById(R.id.cardInventarioItem);
            imgIconoInventario = itemView.findViewById(R.id.imgIconoInventario);
            txtCantidad = itemView.findViewById(R.id.txtCantidadInventarioItem);
            btnComprar = itemView.findViewById(R.id.btnComprarComodinItem);
            layoutPrecio = itemView.findViewById(R.id.layoutPrecio);
            imgTipoMoneda = itemView.findViewById(R.id.imgTipoMonedaComodinItem);
            txtPrecio = itemView.findViewById(R.id.txtPrecioComodinItem);
        }
    }
}