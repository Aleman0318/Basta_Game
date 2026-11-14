package com.example.basta_game.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.PuntuacionJugador;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PuntuacionAdapter extends RecyclerView.Adapter<PuntuacionAdapter.ViewHolder> {

    private List<PuntuacionJugador> listaJugadores;
    private int cantidadRondas;
    private Context context;

    public PuntuacionAdapter(List<PuntuacionJugador> listaJugadores, int cantidadRondas) {
        // Hacemos una copia para que DiffUtil funcione correctamente
        this.listaJugadores = new ArrayList<>(listaJugadores);
        this.cantidadRondas = cantidadRondas;
    }

    // MÉTODO ESTRELLA: Actualiza la lista sin parpadeos
    public void actualizarListaConAnimacion(List<PuntuacionJugador> nuevaLista) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return listaJugadores.size();
            }

            @Override
            public int getNewListSize() {
                return nuevaLista.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                // Comparamos por UID para saber si es el mismo jugador
                return listaJugadores.get(oldItemPosition).getUid().equals(nuevaLista.get(newItemPosition).getUid());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                // Comparamos los puntos totales y la posición para ver si cambiaron
                PuntuacionJugador viejo = listaJugadores.get(oldItemPosition);
                PuntuacionJugador nuevo = nuevaLista.get(newItemPosition);
                return viejo.getTotal() == nuevo.getTotal() && viejo.getPosicion() == nuevo.getPosicion();
            }
        });

        this.listaJugadores.clear();
        this.listaJugadores.addAll(nuevaLista);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PuntuacionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_fila_puntuacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PuntuacionAdapter.ViewHolder holder, int position) {
        PuntuacionJugador jugador = listaJugadores.get(position);

        holder.txtPosicion.setText(jugador.getPosicion() + "°");
        holder.txtNombreJugador.setText(jugador.getNombre());
        holder.txtTotalPuntos.setText(String.valueOf(jugador.getTotal()));

        holder.contenedorPuntosRondas.removeAllViews();
        List<Integer> puntajes = jugador.getPuntajesRondas();

        for (int i = 0; i < cantidadRondas; i++) {
            Integer puntos = null;
            if (puntajes != null && i < puntajes.size()) {
                puntos = puntajes.get(i);
            }
            holder.contenedorPuntosRondas.addView(crearFilaRonda(context, i + 1, puntos));
        }
    }

    // MÉTODO PARA LANZAR LA ANIMACIÓN (Lo llamaremos desde el Fragment)
    public void animarPuntaje(RecyclerView recyclerView, String uidJugador, String textoAnimado, String colorHex) {
        for (int i = 0; i < listaJugadores.size(); i++) {
            if (listaJugadores.get(i).getUid().equals(uidJugador)) {
                ViewHolder holder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null) {
                    TextView txtAnimacion = holder.txtAnimacionCambio;
                    txtAnimacion.setText(textoAnimado);
                    txtAnimacion.setTextColor(Color.parseColor(colorHex));
                    txtAnimacion.setVisibility(View.VISIBLE);

                    // Resetear posición
                    txtAnimacion.setAlpha(1f);
                    txtAnimacion.setTranslationY(0f);

                    // Animar hacia arriba y desvanecer
                    txtAnimacion.animate()
                            .translationY(-100f)
                            .alpha(0f)
                            .setDuration(1200)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> txtAnimacion.setVisibility(View.GONE))
                            .start();
                }
                break;
            }
        }
    }

    // MÉTODO PARA ANIMAR A TODOS A LA VEZ (Para la Ruleta Caótica)
    public void animarATodos(RecyclerView recyclerView, String textoAnimado, String colorHex) {
        for (int i = 0; i < listaJugadores.size(); i++) {
            ViewHolder holder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null) {
                TextView txtAnimacion = holder.txtAnimacionCambio;
                txtAnimacion.setText(textoAnimado);
                txtAnimacion.setTextColor(Color.parseColor(colorHex));
                txtAnimacion.setVisibility(View.VISIBLE);

                txtAnimacion.setAlpha(1f);
                txtAnimacion.setTranslationY(0f);

                txtAnimacion.animate()
                        .translationY(-100f)
                        .alpha(0f)
                        .setDuration(1200)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> txtAnimacion.setVisibility(View.GONE))
                        .start();
            }
        }
    }

    private MaterialCardView crearFilaRonda(Context context, int numeroRonda, Integer puntos) {
        // [Este método se queda exactamente igual al original que me compartiste]
        MaterialCardView card = new MaterialCardView(context);
        LinearLayout.LayoutParams paramsCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 42)
        );
        paramsCard.setMargins(0, 0, 0, dp(context, 7));
        card.setLayoutParams(paramsCard);
        card.setRadius(dp(context, 14));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(context, 1));

        if (puntos == null) {
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F8FAFC")));
            card.setStrokeColor(Color.parseColor("#CBD5E1"));
        } else if (puntos >= 10) {
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F1FFF0")));
            card.setStrokeColor(Color.parseColor("#BDE8B8"));
        } else if (puntos > 0) {
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF7D6")));
            card.setStrokeColor(Color.parseColor("#FFD530"));
        } else {
            card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF1F1")));
            card.setStrokeColor(Color.parseColor("#FFC4C4"));
        }

        LinearLayout fila = new LinearLayout(context);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(dp(context, 12), 0, dp(context, 12), 0);

        TextView txtRonda = new TextView(context);
        txtRonda.setText("Ronda " + numeroRonda);
        txtRonda.setTextColor(Color.parseColor("#1E293B"));
        txtRonda.setTextSize(13);
        txtRonda.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout.LayoutParams paramsRonda = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

        TextView txtPuntos = new TextView(context);
        if (puntos == null) {
            txtPuntos.setText("Sin jugar");
            txtPuntos.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            txtPuntos.setText(puntos + " pts");
            if (puntos >= 10) {
                txtPuntos.setTextColor(Color.parseColor("#2AA514"));
            } else if (puntos > 0) {
                txtPuntos.setTextColor(Color.parseColor("#F97316"));
            } else {
                txtPuntos.setTextColor(Color.parseColor("#DC080C"));
            }
        }

        txtPuntos.setTextSize(14);
        txtPuntos.setGravity(Gravity.CENTER);
        txtPuntos.setTypeface(Typeface.DEFAULT_BOLD);

        fila.addView(txtRonda, paramsRonda);
        fila.addView(txtPuntos);
        card.addView(fila);

        return card;
    }

    @Override
    public int getItemCount() {
        return listaJugadores != null ? listaJugadores.size() : 0;
    }

    private int dp(Context context, int valor) {
        return Math.round(valor * context.getResources().getDisplayMetrics().density);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtPosicion;
        TextView txtNombreJugador;
        TextView txtTotalPuntos;
        LinearLayout contenedorPuntosRondas;
        TextView txtAnimacionCambio; // Agregado

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPosicion = itemView.findViewById(R.id.txtPosicion);
            txtNombreJugador = itemView.findViewById(R.id.txtNombreJugadorTabla);
            txtTotalPuntos = itemView.findViewById(R.id.txtTotalPuntos);
            contenedorPuntosRondas = itemView.findViewById(R.id.contenedorPuntosRondas);
            txtAnimacionCambio = itemView.findViewById(R.id.txtAnimacionCambio); // Mapeado
        }
    }
}