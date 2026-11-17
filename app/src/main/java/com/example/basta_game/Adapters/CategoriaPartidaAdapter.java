package com.example.basta_game.Adapters;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.R;
import com.example.basta_game.models.Categoria;

import java.util.List;
import java.util.Map;

public class CategoriaPartidaAdapter extends RecyclerView.Adapter<CategoriaPartidaAdapter.ViewHolder> {
    public static void configurarScrollInterno(RecyclerView lista) {
        lista.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(lista.getContext()));
        lista.setNestedScrollingEnabled(false);
        lista.setPreserveFocusAfterLayout(true);
        lista.setItemAnimator(null);
    }

    public interface OnCampoRespuestaFocusListener {
        void onCampoRespuestaFocus(View campo);
    }

    private final List<Categoria> listaCategorias;
    private final Map<String, String> respuestasJugador;
    private final OnCampoRespuestaFocusListener focusListener;

    public CategoriaPartidaAdapter(List<Categoria> listaCategorias, Map<String, String> respuestasJugador) {
        this(listaCategorias, respuestasJugador, null);
    }

    public CategoriaPartidaAdapter(
            List<Categoria> listaCategorias,
            Map<String, String> respuestasJugador,
            OnCampoRespuestaFocusListener focusListener
    ) {
        this.listaCategorias = listaCategorias;
        this.respuestasJugador = respuestasJugador;
        this.focusListener = focusListener;
    }

    @NonNull
    @Override
    public CategoriaPartidaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_categoria_partida, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriaPartidaAdapter.ViewHolder holder, int position) {
        Categoria cat = listaCategorias.get(position);

        holder.txtNombre.setText(cat.getNombre());
        holder.imgIcono.setImageResource(cat.getIconoId());

        if (holder.textWatcher != null) {
            holder.etRespuesta.removeTextChangedListener(holder.textWatcher);
        }

        String respuestaGuardada = respuestasJugador.get(cat.getNombre());
        String texto = respuestaGuardada != null ? respuestaGuardada : "";
        if (!holder.etRespuesta.getText().toString().equals(texto)) holder.etRespuesta.setText(texto);

        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                respuestasJugador.put(cat.getNombre(), s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        holder.etRespuesta.addTextChangedListener(holder.textWatcher);

        holder.etRespuesta.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                avisarYSubirCampo(v);
            }
        });

        holder.etRespuesta.setOnClickListener(this::avisarYSubirCampo);
    }

    private void avisarYSubirCampo(View campo) {
        if (focusListener != null) {
            focusListener.onCampoRespuestaFocus(campo);
            return;
        }
        campo.post(() -> {
            if (!campo.hasFocus() || !campo.isAttachedToWindow()) return;
            int margen = Math.round(12 * campo.getResources().getDisplayMetrics().density);
            campo.requestRectangleOnScreen(new android.graphics.Rect(0, -margen,
                    campo.getWidth(), campo.getHeight() + margen), false);
        });
    }
    @Override
    public int getItemCount() {
        return listaCategorias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre;
        ImageView imgIcono;
        EditText etRespuesta;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombre = itemView.findViewById(R.id.txtNombreCategoriaPartida);
            imgIcono = itemView.findViewById(R.id.imgIconoCategoriaPartida);
            etRespuesta = itemView.findViewById(R.id.etRespuestaCategoriaPartida);

            InputFilter filtroBloquearNumeros = new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    for (int i = start; i < end; i++) {
                        if (Character.isDigit(source.charAt(i))) {
                            return source.toString().replaceAll("[0-9]", "");
                        }
                    }

                    return null;
                }
            };

            etRespuesta.setFilters(new InputFilter[]{filtroBloquearNumeros});
        }
    }
}
