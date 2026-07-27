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
        holder.etRespuesta.setText(respuestaGuardada != null ? respuestaGuardada : "");

        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                respuestasJugador.put(cat.getNombre(), s.toString().trim());
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
        }

        // Esto no bloquea el scroll manual. Solo ayuda a que el campo enfocado quede visible
        // cuando aparece el teclado, parecido a una caja de mensaje que sube.
        campo.postDelayed(() -> desplazarHastaCampo(campo), 280);
        campo.postDelayed(() -> desplazarHastaCampo(campo), 650);
    }

    private void desplazarHastaCampo(View campo) {
        ScrollView scrollView = buscarScrollView(campo);
        NestedScrollView nestedScrollView = buscarNestedScrollView(campo);

        View item = buscarItemCategoria(campo);

        if (item == null) {
            item = campo;
        }

        int margenSuperior = dp(campo, 120);

        if (scrollView != null) {
            int destino = calcularDestinoScroll(scrollView, item, margenSuperior);
            scrollView.smoothScrollTo(0, destino);
            return;
        }

        if (nestedScrollView != null) {
            int destino = calcularDestinoScroll(nestedScrollView, item, margenSuperior);
            nestedScrollView.smoothScrollTo(0, destino);
        }
    }

    private int calcularDestinoScroll(View contenedorScroll, View objetivo, int margenSuperior) {
        int[] locScroll = new int[2];
        int[] locObjetivo = new int[2];

        contenedorScroll.getLocationOnScreen(locScroll);
        objetivo.getLocationOnScreen(locObjetivo);

        int scrollActual = 0;

        if (contenedorScroll instanceof ScrollView) {
            scrollActual = ((ScrollView) contenedorScroll).getScrollY();
        } else if (contenedorScroll instanceof NestedScrollView) {
            scrollActual = ((NestedScrollView) contenedorScroll).getScrollY();
        }

        int destino = scrollActual + (locObjetivo[1] - locScroll[1]) - margenSuperior;
        return Math.max(0, destino);
    }

    private View buscarItemCategoria(View vista) {
        View actual = vista;

        while (actual != null) {
            ViewParent parent = actual.getParent();

            if (parent instanceof RecyclerView) {
                return actual;
            }

            if (parent instanceof View) {
                actual = (View) parent;
            } else {
                return vista;
            }
        }

        return vista;
    }

    private ScrollView buscarScrollView(View vista) {
        ViewParent parent = vista.getParent();

        while (parent != null) {
            if (parent instanceof ScrollView) {
                return (ScrollView) parent;
            }

            parent = parent.getParent();
        }

        return null;
    }

    private NestedScrollView buscarNestedScrollView(View vista) {
        ViewParent parent = vista.getParent();

        while (parent != null) {
            if (parent instanceof NestedScrollView) {
                return (NestedScrollView) parent;
            }

            parent = parent.getParent();
        }

        return null;
    }

    private int dp(View view, int valor) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(valor * density);
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
