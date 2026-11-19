package com.example.basta_game.Adapters;

import android.text.Editable;
import android.text.Selection;
import android.text.InputFilter;
import android.text.Spanned;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.google.android.material.card.MaterialCardView;
import com.example.basta_game.utils.EspejoCategorias;
import com.example.basta_game.utils.ConexionJuego;
import com.example.basta_game.utils.TextoEspejo;
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

import java.util.ArrayList;
import java.util.HashMap;
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

    // Decreto del Silencio: se anuncia a todos, pero no modifica el texto escrito.
    private final java.util.Set<String> categoriasDecreto = new java.util.HashSet<>();
    private String vocalDecreto;

    public void mostrarDecreto(java.util.Collection<String> categorias, String vocal) {
        categoriasDecreto.clear();
        if(categorias!=null) categoriasDecreto.addAll(categorias);
        vocalDecreto=vocal;
        actualizarVistasEspejo();
        // Reaplicar la marca a todas las posiciones, incluso fuera de pantalla.
        // Payload evita reiniciar los EditText o mover el cursor de escritura.
        if (listaVisible != null) listaVisible.post(() -> {
            if (listaVisible != null && getItemCount() > 0)
                notifyItemRangeChanged(0, getItemCount(), "marcaDecreto");
        });
    }

    private boolean espejoActivo;
    private String espejoIdUso;
    private long espejoFinMs;
    private long espejoFinMonotonoMs;
    private final java.util.Set<String> categoriasEspejo = new java.util.HashSet<>();
    private final java.util.Set<String> espejosFinalizados = new java.util.HashSet<>();
    private final Handler relojEspejo = new Handler(Looper.getMainLooper());
    private final Runnable ticEspejo = new Runnable() {
        @Override public void run() {
            if (!espejoActivo) return;
            if (SystemClock.elapsedRealtime() >= espejoFinMonotonoMs ||
                    ConexionJuego.ahoraServidor() >= espejoFinMs) {
                finalizarEspejo();
            } else {
                actualizarVistasEspejo();
                relojEspejo.postDelayed(this, 200L);
            }
        }
    };
    private RecyclerView listaVisible;
    private boolean aplicandoEspejo;

    /** El efecto solo selecciona campos una vez por ataque, nunca en cada refresco de Firebase. */
    public boolean iniciarEspejo(String idUso, long finMs) {
        if (idUso == null || espejosFinalizados.contains(idUso)) return false;
        if (espejoActivo && idUso.equals(espejoIdUso)) return true;
        if (finMs <= ConexionJuego.ahoraServidor()) return false;
        if (espejoActivo) finalizarEspejo();
        espejoIdUso = idUso;
        espejoFinMs = finMs;
        // Tiempo monotónico: no se congela por cambios del reloj o del offset de Firebase.
        espejoFinMonotonoMs = SystemClock.elapsedRealtime() +
                Math.max(0L, finMs - ConexionJuego.ahoraServidor());
        String enFoco = null;
        if (listaVisible != null) for (int i=0;i<listaVisible.getChildCount();i++) {
            RecyclerView.ViewHolder vh = listaVisible.getChildViewHolder(listaVisible.getChildAt(i));
            if (vh instanceof ViewHolder && ((ViewHolder)vh).etRespuesta.hasFocus()) {
                enFoco = ((ViewHolder)vh).nombreCategoria;
                break;
            }
        }
        categoriasEspejo.clear();
        List<String> nombres = new ArrayList<>();
        for (Categoria c : listaCategorias) nombres.add(c.getNombre());
        // No volver a sortear el mismo ataque al rotar o reabrir la pantalla.
        android.content.SharedPreferences preferencias = listaVisible == null ? null :
                listaVisible.getContext().getSharedPreferences("espejoCategoriasD8", android.content.Context.MODE_PRIVATE);
        String clave = "uso:" + idUso;
        if (preferencias != null && preferencias.contains(clave)) {
            java.util.Set<String> guardadas = preferencias.getStringSet(clave, java.util.Collections.emptySet());
            if (guardadas != null) for (String categoria : guardadas)
                if (nombres.contains(categoria)) categoriasEspejo.add(categoria);
        } else {
            categoriasEspejo.addAll(EspejoCategorias.elegir(nombres, respuestasJugador, enFoco, idUso));
            if (preferencias != null) preferencias.edit().putStringSet(clave,
                    new java.util.HashSet<>(categoriasEspejo)).apply();
        }
        // Transformar una sola vez el texto REAL al recibir el ataque.
        // Lo que se ve y lo que se guardara en Firebase es exactamente lo mismo.
        espejoActivo = true;
        if (preferencias == null || !preferencias.getBoolean("invertido:"+idUso,false)) {
            for (String nombre : categoriasEspejo) {
                String previo = respuestasJugador.get(nombre);
                respuestasJugador.put(nombre, TextoEspejo.invertir(previo));
            }
            if (listaVisible != null) for (int i=0;i<listaVisible.getChildCount();i++) {
                RecyclerView.ViewHolder vh = listaVisible.getChildViewHolder(listaVisible.getChildAt(i));
                if (!(vh instanceof ViewHolder)) continue;
                ViewHolder h=(ViewHolder)vh;
                if (!categoriasEspejo.contains(h.nombreCategoria)) continue;
                aplicandoEspejo=true;
                try {
                    h.etRespuesta.setText(respuestasJugador.get(h.nombreCategoria));
                    h.etRespuesta.setSelection(Math.min(1,h.etRespuesta.length()));
                } finally { aplicandoEspejo=false; }
            }
            if (preferencias != null) preferencias.edit().putBoolean("invertido:"+idUso,true).apply();
        }
        actualizarVistasEspejo();
        relojEspejo.removeCallbacks(ticEspejo);
        relojEspejo.post(ticEspejo);
        return true;
    }

    /** Al terminar SOLO libera la escritura. No invierte, borra ni reescribe respuestas. */
    public void finalizarEspejo() {
        if (!espejoActivo) return;
        espejoActivo = false;
        relojEspejo.removeCallbacks(ticEspejo);
        if (espejoIdUso != null) espejosFinalizados.add(espejoIdUso);
        categoriasEspejo.clear();
        actualizarVistasEspejo();
        // Un EditText normal y editable, con el texto exactamente como quedo.
        if (listaVisible != null) for(int i=0;i<listaVisible.getChildCount();i++) {
            RecyclerView.ViewHolder vh=listaVisible.getChildViewHolder(listaVisible.getChildAt(i));
            if (vh instanceof ViewHolder) {
                EditText campo=((ViewHolder)vh).etRespuesta;
                if (campo.hasFocus()) campo.setSelection(campo.length());
            }
        }
    }

    private void actualizarVistasEspejo() {
        if (listaVisible == null) return;
        final long ahora = ConexionJuego.ahoraServidor();
        for (int i=0;i<listaVisible.getChildCount();i++) {
            RecyclerView.ViewHolder vh = listaVisible.getChildViewHolder(listaVisible.getChildAt(i));
            if (vh instanceof ViewHolder) actualizarReflejo((ViewHolder)vh, ahora);
        }
    }

    @Override public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        listaVisible = recyclerView;
        if (espejoActivo) {
            relojEspejo.removeCallbacks(ticEspejo);
            relojEspejo.post(ticEspejo);
        }
    }

    @Override public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        // El reloj sigue corriendo durante cambios de vista; el efecto no debe quedar en 2 s.
        listaVisible = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    private void actualizarReflejo(ViewHolder holder) {
        actualizarReflejo(holder, ConexionJuego.ahoraServidor());
    }

    private void actualizarReflejo(ViewHolder holder, long ahora) {
        boolean afectado = espejoActivo && categoriasEspejo.contains(holder.nombreCategoria);
        // Texto real, visible y editable: ningun TextView puede tapar el cursor.
        holder.etRespuesta.setTextColor(holder.colorOriginal);
        holder.etRespuesta.setHint(afectado ? "🪞 Escribe al revés" : "Escribe tu respuesta");
        holder.txtEspejoVisual.setVisibility(View.GONE);
        boolean prohibida = false;
        if (vocalDecreto != null && holder.nombreCategoria != null) {
            for (String nombre : categoriasDecreto) {
                if (nombre != null && nombre.trim().equalsIgnoreCase(holder.nombreCategoria.trim())) {
                    prohibida = true;
                    break;
                }
            }
        }
        holder.txtDecreto.setVisibility(prohibida ? View.VISIBLE : View.GONE);
        if(prohibida) holder.txtDecreto.setText("🚫 Vocal " + vocalDecreto + " prohibida · 0 pts y multa de 5");
        holder.tarjetaCategoria.setStrokeColor(Color.parseColor(prohibida ? "#E58E29" : afectado ? "#8F4CE8" : "#BFD0FF"));
        float dp = holder.itemView.getResources().getDisplayMetrics().density;
        holder.tarjetaCategoria.setStrokeWidth(Math.round(dp * (afectado ? 3f : prohibida ? 2.5f : 1.5f)));
        holder.txtCuentaEspejo.setVisibility(afectado ? View.VISIBLE : View.GONE);
        // La marca de Decreto prevalece sobre el borde de Espejo: las dos marcas
        // siempre quedan reconocibles cuando ambos efectos afectan el mismo campo.
        if (afectado) {
            long restante = Math.max(0, Math.min(espejoFinMs - ahora,
                    espejoFinMonotonoMs - SystemClock.elapsedRealtime()));
            holder.txtCuentaEspejo.setText("🪞 " + ((restante + 999L) / 1000L) + " s");
        }
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
        this.listaCategorias = new ArrayList<>(listaCategorias);
        this.respuestasJugador = respuestasJugador;
        this.focusListener = focusListener;
    }

    /**
     * Cambia SOLO el orden visible. Las respuestas siguen asociadas al nombre
     * original de cada categoría, nunca a su posición en RecyclerView.
     */
    public void reordenarPorNombres(List<String> nombresEnOrden) {
        if (nombresEnOrden == null || nombresEnOrden.size() != listaCategorias.size()) return;
        Map<String, Categoria> porNombre = new HashMap<>();
        for (Categoria categoria : listaCategorias) {
            if (porNombre.put(categoria.getNombre(), categoria) != null) return;
        }
        List<Categoria> nuevoOrden = new ArrayList<>();
        for (String nombre : nombresEnOrden) {
            Categoria categoria = porNombre.remove(nombre);
            if (categoria == null) return;
            nuevoOrden.add(categoria);
        }
        if (!porNombre.isEmpty()) return;
        boolean distinto = false;
        for (int i = 0; i < nuevoOrden.size(); i++) {
            if (nuevoOrden.get(i) != listaCategorias.get(i)) { distinto = true; break; }
        }
        if (!distinto) return;
        listaCategorias.clear();
        listaCategorias.addAll(nuevoOrden);
        notifyDataSetChanged();
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

        holder.nombreCategoria = cat.getNombre();
        holder.txtNombre.setText(cat.getNombre());
        holder.imgIcono.setImageResource(cat.getIconoId());

        if (holder.textWatcher != null) {
            holder.etRespuesta.removeTextChangedListener(holder.textWatcher);
        }

        String respuestaGuardada = respuestasJugador.get(cat.getNombre());
        String texto = respuestaGuardada != null ? respuestaGuardada : "";
        if (!holder.etRespuesta.getText().toString().equals(texto)) holder.etRespuesta.setText(texto);

        holder.textWatcher = new TextWatcher() {
            int inicio, agregados;
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {
                inicio=start; agregados=count;
                respuestasJugador.put(cat.getNombre(),s.toString());
            }
            @Override public void afterTextChanged(Editable s) {
                if (aplicandoEspejo) return;
                if (espejoActivo && categoriasEspejo.contains(cat.getNombre()) &&
                        cat.getNombre().equals(holder.nombreCategoria)) {
                    // Cada caracter nuevo se escribe delante del anterior.
                    // Si pega una palabra completa, invertimos el bloque una sola vez.
                    if (agregados>0 && inicio>=0 && inicio+agregados<=s.length()) {
                        String insertado=s.subSequence(inicio,inicio+agregados).toString();
                        aplicandoEspejo=true;
                        try {
                            s.delete(inicio,inicio+agregados);
                            s.insert(0,TextoEspejo.invertir(insertado));
                        } finally { aplicandoEspejo=false; }
                    }
                    // Posicion 1: escribir siguiente letra antepone; retroceso borra
                    // la ultima letra insertada. Nunca se bloquea el EditText.
                    int cursor=Math.min(1,s.length());
                    if (holder.etRespuesta.getSelectionStart()!=cursor) Selection.setSelection(s,cursor);
                }
                respuestasJugador.put(cat.getNombre(),s.toString());
            }
        };

        holder.etRespuesta.addTextChangedListener(holder.textWatcher);
        actualizarReflejo(holder);
        if (espejoActivo && categoriasEspejo.contains(cat.getNombre()) && !holder.etRespuesta.hasFocus())
            holder.etRespuesta.setSelection(Math.min(1,holder.etRespuesta.length()));

        holder.etRespuesta.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                avisarYSubirCampo(v);
            }
        });

        holder.etRespuesta.setOnClickListener(this::avisarYSubirCampo);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position,
                                 @NonNull java.util.List<Object> payloads) {
        if (payloads.contains("marcaDecreto")) {
            actualizarReflejo(holder);
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
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
        TextView txtEspejoVisual;
        TextView txtCuentaEspejo;
        TextView txtDecreto;
        MaterialCardView tarjetaCategoria;
        String nombreCategoria;
        int colorOriginal;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombre = itemView.findViewById(R.id.txtNombreCategoriaPartida);
            imgIcono = itemView.findViewById(R.id.imgIconoCategoriaPartida);
            etRespuesta = itemView.findViewById(R.id.etRespuestaCategoriaPartida);
            txtEspejoVisual = itemView.findViewById(R.id.txtEspejoVistaCategoriaPartida);
            txtCuentaEspejo = itemView.findViewById(R.id.txtCuentaEspejoCategoriaPartida);
            txtDecreto = itemView.findViewById(R.id.txtDecretoCategoriaPartida);
            tarjetaCategoria = itemView.findViewById(R.id.tarjetaCategoriaPartida);
            colorOriginal = etRespuesta.getCurrentTextColor();

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
