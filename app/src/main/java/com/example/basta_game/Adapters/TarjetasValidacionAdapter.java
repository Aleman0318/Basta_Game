package com.example.basta_game.Adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.basta_game.R;
import com.example.basta_game.models.RespuestaValidacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TarjetasValidacionAdapter extends RecyclerView.Adapter<TarjetasValidacionAdapter.ViewHolder> {

    private static final long TIEMPO_ENTRE_RESPUESTAS_MS = 2000;
    private static final long TIEMPO_ANTES_DE_REVISAR_AVANCE_MS = 700;
    private static final String PAYLOAD_TEXTO_ESTADO = "payload_texto_estado";

    public interface ControlAnimacionCallback {
        boolean puedeContinuar();
    }

    public interface OnAvanzarCategoriaListener {
        void onAvanzar(int siguienteIndice);
    }

    private final List<String> categorias;
    private final Map<String, List<RespuestaValidacion>> respuestasPorCategoria;
    private final ViewPager2 viewPager2;
    private final Runnable onUltimaCategoriaRevelada;
    private final boolean[] categoriasFinalizadas;
    private final boolean[] animacionesIniciadas;
    private final boolean[] avanceSolicitado;
    private final boolean[] mensajePreparandoMostrado;

    private final ValidacionAdapter[] subAdaptadores;
    private final ValidacionAdapter.OnSolicitarVotacionListener listenerVotacion;
    private final ControlAnimacionCallback controlAnimacionCallback;
    private final boolean puedeControlarAvance;
    private final OnAvanzarCategoriaListener onAvanzarCategoriaListener;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public TarjetasValidacionAdapter(
            List<String> categorias,
            Map<String, List<RespuestaValidacion>> respuestasPorCategoria,
            ViewPager2 viewPager2,
            Runnable onUltimaCategoriaRevelada,
            ValidacionAdapter.OnSolicitarVotacionListener listenerVotacion,
            ControlAnimacionCallback controlAnimacionCallback,
            boolean puedeControlarAvance,
            OnAvanzarCategoriaListener onAvanzarCategoriaListener
    ) {
        this.categorias = categorias;
        this.respuestasPorCategoria = respuestasPorCategoria;
        this.viewPager2 = viewPager2;
        this.onUltimaCategoriaRevelada = onUltimaCategoriaRevelada;
        this.listenerVotacion = listenerVotacion;
        this.controlAnimacionCallback = controlAnimacionCallback;
        this.puedeControlarAvance = puedeControlarAvance;
        this.onAvanzarCategoriaListener = onAvanzarCategoriaListener;

        this.categoriasFinalizadas = new boolean[categorias.size()];
        this.animacionesIniciadas = new boolean[categorias.size()];
        this.avanceSolicitado = new boolean[categorias.size()];
        this.mensajePreparandoMostrado = new boolean[categorias.size()];
        this.subAdaptadores = new ValidacionAdapter[categorias.size()];

        for (int i = 0; i < categorias.size(); i++) {
            String categoria = categorias.get(i);
            List<RespuestaValidacion> respuestas = respuestasPorCategoria.get(categoria);

            if (respuestas == null) {
                respuestas = new ArrayList<>();
            }

            subAdaptadores[i] = new ValidacionAdapter(respuestas, listenerVotacion);
        }

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                iniciarAnimacionEnPaginaPublica(position);
            }
        });
    }

    @NonNull
    @Override
    public TarjetasValidacionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarjeta_revision, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TarjetasValidacionAdapter.ViewHolder holder, int position) {
        bindCompleto(holder, position);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TarjetasValidacionAdapter.ViewHolder holder,
            int position,
            @NonNull List<Object> payloads
    ) {
        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_TEXTO_ESTADO)) {
            configurarTextoEstado(holder, position);
            return;
        }

        bindCompleto(holder, position);
    }

    private void bindCompleto(@NonNull TarjetasValidacionAdapter.ViewHolder holder, int position) {
        holder.itemView.clearAnimation();
        holder.txtNombreCategoria.setText(categorias.get(position));

        if (holder.rvRespuestas.getLayoutManager() == null) {
            holder.rvRespuestas.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        }

        holder.rvRespuestas.setItemAnimator(null);
        holder.rvRespuestas.setHasFixedSize(false);

        if (holder.rvRespuestas.getAdapter() != subAdaptadores[position]) {
            holder.rvRespuestas.setAdapter(subAdaptadores[position]);
        }

        configurarTextoEstado(holder, position);
    }

    private void configurarTextoEstado(@NonNull TarjetasValidacionAdapter.ViewHolder holder, int position) {
        if (position < 0 || position >= categorias.size()) {
            return;
        }

        if (categoriasFinalizadas[position]) {
            if (position == categorias.size() - 1) {
                holder.txtDeslizar.setText("¡Revisión terminada!");
            } else {
                holder.txtDeslizar.setText("Categoría revisada");
            }

            holder.txtDeslizar.setVisibility(View.VISIBLE);
        } else if (subAdaptadores[position].estanTodosRevelados()) {
            holder.txtDeslizar.setText("Preparando siguiente categoría...");
            holder.txtDeslizar.setVisibility(View.VISIBLE);
        } else {
            holder.txtDeslizar.setText("Revisando juntos...");
            holder.txtDeslizar.setVisibility(View.VISIBLE);
        }
    }

    private boolean puedeContinuarAnimacion() {
        return controlAnimacionCallback == null || controlAnimacionCallback.puedeContinuar();
    }

    public void iniciarAnimacionEnPaginaPublica(int position) {
        if (position < 0 || position >= categorias.size()) {
            return;
        }

        if (categoriasFinalizadas[position] || animacionesIniciadas[position]) {
            return;
        }

        animacionesIniciadas[position] = true;
        iniciarAnimacionEnPagina(position);
    }

    private void iniciarAnimacionEnPagina(int position) {
        ValidacionAdapter adaptadorJugadores = subAdaptadores[position];
        viewPager2.setUserInputEnabled(false);

        Runnable revisarRunnable = new Runnable() {
            @Override
            public void run() {
                if (viewPager2.getCurrentItem() != position) {
                    animacionesIniciadas[position] = false;
                    return;
                }

                if (!puedeContinuarAnimacion()) {
                    handler.postDelayed(this, 500);
                    return;
                }

                if (!adaptadorJugadores.estanTodosRevelados()) {
                    adaptadorJugadores.revelarSiguiente();
                    handler.postDelayed(this, TIEMPO_ENTRE_RESPUESTAS_MS);
                    return;
                }

                if (!mensajePreparandoMostrado[position]) {
                    mensajePreparandoMostrado[position] = true;
                    notifyItemChanged(position, PAYLOAD_TEXTO_ESTADO);
                }

                if (!adaptadorJugadores.todasLasRespuestasFinalizadas()) {
                    handler.postDelayed(this, 500);
                    return;
                }

                categoriasFinalizadas[position] = true;
                notifyItemChanged(position, PAYLOAD_TEXTO_ESTADO);

                handler.postDelayed(() -> avanzarDespuesDeCategoria(position), TIEMPO_ANTES_DE_REVISAR_AVANCE_MS);
            }
        };

        handler.postDelayed(revisarRunnable, 600);
    }

    private void avanzarDespuesDeCategoria(int position) {
        if (position < 0 || position >= categorias.size()) {
            return;
        }

        if (!puedeContinuarAnimacion()) {
            handler.postDelayed(() -> avanzarDespuesDeCategoria(position), 500);
            return;
        }

        if (avanceSolicitado[position]) {
            return;
        }

        avanceSolicitado[position] = true;

        if (position == categorias.size() - 1) {
            viewPager2.setUserInputEnabled(true);

            if (onUltimaCategoriaRevelada != null) {
                onUltimaCategoriaRevelada.run();
            }

            return;
        }

        int siguienteIndice = position + 1;

        if (puedeControlarAvance && onAvanzarCategoriaListener != null) {
            onAvanzarCategoriaListener.onAvanzar(siguienteIndice);
        }
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }

    public void actualizarValidaciones() {
        for (ValidacionAdapter adapter : subAdaptadores) {
            if (adapter != null) {
                adapter.actualizarValidaciones();
            }
        }
    }

    public void pausarTemporizadoresPorVotacion(String claveVotacionActual) {
        for (ValidacionAdapter adapter : subAdaptadores) {
            if (adapter != null) {
                adapter.pausarTemporizadoresPorVotacion(claveVotacionActual);
            }
        }
    }

    public void reiniciarTemporizadoresPausadosPorVotacion() {
        for (ValidacionAdapter adapter : subAdaptadores) {
            if (adapter != null) {
                adapter.reiniciarTemporizadoresPausadosPorVotacion();
            }
        }
    }

    public void actualizarCooldown(long iniciadoEn, long duracionMs, long horaServidor) {
        // Ya no usamos cooldown global.
        // Este método queda para que PartidaFragment compile aunque todavía lo llame.
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        handler.removeCallbacksAndMessages(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombreCategoria, txtDeslizar;
        RecyclerView rvRespuestas;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombreCategoria = itemView.findViewById(R.id.txtNombreCategoriaTarjeta);
            txtDeslizar = itemView.findViewById(R.id.txtDeslizarTarjeta);
            rvRespuestas = itemView.findViewById(R.id.rvRespuestasTarjeta);
        }
    }
}
