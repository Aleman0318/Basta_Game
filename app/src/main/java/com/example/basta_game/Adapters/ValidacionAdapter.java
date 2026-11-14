package com.example.basta_game.Adapters;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.basta_game.R;
import com.example.basta_game.utils.EstadoRevision;
import com.example.basta_game.models.RespuestaValidacion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ValidacionAdapter extends RecyclerView.Adapter<ValidacionAdapter.ViewHolder> {

    private static final long TIEMPO_BOTON_VOTAR_MS = 5000;
    private static final String PAYLOAD_CONTADOR = "payload_contador";
    private static final String PAYLOAD_ESTADO = "payload_estado";

    public interface OnSolicitarVotacionListener {
        void onSolicitarVotacion(RespuestaValidacion respuesta);
    }

    private final List<RespuestaValidacion> respuestas;
    private int elementosRevelados = 0;
    private final OnSolicitarVotacionListener listener;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Map<String, Runnable> temporizadores = new HashMap<>();
    private final Map<String, Long> tiemposInicio = new HashMap<>();

    private final HashSet<String> clavesPausadasPorVotacion = new HashSet<>();
    private final HashSet<String> clavesReveladas = new HashSet<>();
    private String claveRespuestaEnVotacion = null;

    public ValidacionAdapter(
            List<RespuestaValidacion> respuestas,
            OnSolicitarVotacionListener listener
    ) {
        this.respuestas = respuestas;
        this.listener = listener;
        setHasStableIds(false);
    }

    @NonNull
    @Override
    public ValidacionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_respuestas, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ValidacionAdapter.ViewHolder holder, int position) {
        bindCompleto(holder, position);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ValidacionAdapter.ViewHolder holder,
            int position,
            @NonNull List<Object> payloads
    ) {
        if (payloads.isEmpty()) {
            bindCompleto(holder, position);
            return;
        }

        if (position >= respuestas.size() || position >= elementosRevelados) {
            return;
        }

        RespuestaValidacion respuesta = respuestas.get(position);

        if (payloads.contains(PAYLOAD_CONTADOR) || payloads.contains(PAYLOAD_ESTADO)) {
            configurarEstadoVisualRespuesta(holder, respuesta);
        } else {
            bindCompleto(holder, position);
        }
    }

    private void bindCompleto(@NonNull ValidacionAdapter.ViewHolder holder, int position) {
        holder.itemView.clearAnimation();
        holder.btnValidar.clearAnimation();
        holder.txtRespuesta.clearAnimation();
        holder.txtUsuario.clearAnimation();
        holder.imgAvatar.clearAnimation();

        if (position >= elementosRevelados) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setAlpha(1f);
            return;
        }

        holder.itemView.setVisibility(View.VISIBLE);
        holder.itemView.setAlpha(1f);

        RespuestaValidacion respuesta = respuestas.get(position);

        holder.btnValidar.setOnClickListener(null);
        holder.btnValidar.setClickable(false);
        holder.btnValidar.setEnabled(false);

        holder.txtUsuario.setText(respuesta.getNombreJugador());

        if (esRespuestaVacia(respuesta)) {
            holder.txtRespuesta.setText("Sin respuesta");
        } else {
            holder.txtRespuesta.setText(respuesta.getRespuesta());
        }

        Glide.with(holder.itemView.getContext())
                .load(respuesta.getFotoPerfilUrl())
                .placeholder(R.drawable.ic_siluetas)
                .error(R.drawable.ic_siluetas)
                .into(holder.imgAvatar);

        configurarEstadoVisualRespuesta(holder, respuesta);
    }

    private void configurarEstadoVisualRespuesta(ViewHolder holder, RespuestaValidacion respuesta) {
        holder.btnValidar.clearAnimation();
        holder.txtPuntos.clearAnimation();

        holder.btnValidar.setOnClickListener(null);
        holder.btnValidar.setClickable(false);
        holder.btnValidar.setEnabled(false);

        if (esRespuestaVacia(respuesta)) {
            respuesta.setPuntos(0);
            respuesta.setMostrarPuntaje(true);
            mostrarPuntaje(holder, respuesta, obtenerColorPorPuntos(0));
            return;
        }
        if (!respuesta.isValidada()) {
            configurarRespuestaSinValidar(holder);
            return;
        }

        configurarRespuestaValidada(holder, respuesta);
    }

    private void configurarRespuestaSinValidar(ViewHolder holder) {
        holder.txtRespuesta.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary)
        );

        holder.txtRespuesta.setBackgroundResource(R.drawable.bg_input_juego);

        mostrarBotonVotar(holder);

        holder.btnValidar.setEnabled(false);
        holder.btnValidar.setClickable(false);
        holder.btnValidar.setAlpha(0.45f);

        setTextoBotonVotar(holder, "...");
    }

    private void configurarRespuestaValidada(ViewHolder holder, RespuestaValidacion respuesta) {
        int colorOutline = obtenerColorPorPuntos(respuesta.getPuntos());

        holder.txtRespuesta.setTextColor(colorOutline);
        aplicarBordeRespuesta(holder.txtRespuesta, colorOutline);

        if (esRespuestaVacia(respuesta)) {
            respuesta.setPuntos(0);
            respuesta.setMostrarPuntaje(true);
            respuesta.setTimerIniciado(false);
            cancelarTemporizadorPuntaje(respuesta);
            mostrarPuntaje(holder, respuesta, obtenerColorPorPuntos(0));
            return;
        }

        if (EstadoRevision.definitiva(respuesta.getRespuesta(), respuesta.isValidada(), respuesta.getPuntos(),
                respuesta.isVotacionResuelta(), respuesta.isMostrarPuntaje())) {
            respuesta.setMostrarPuntaje(true);
            respuesta.setTimerIniciado(false);
            cancelarTemporizadorPuntaje(respuesta);
            mostrarPuntaje(holder, respuesta, colorOutline);
            return;
        }

        if (respuesta.isMostrarPuntaje()) {
            cancelarTemporizadorPuntaje(respuesta);
            mostrarPuntaje(holder, respuesta, colorOutline);
            return;
        }

        mostrarBotonVotar(holder);
        asegurarTemporizadorActivo(respuesta);

        holder.btnValidar.setEnabled(true);
        holder.btnValidar.setClickable(true);
        holder.btnValidar.setAlpha(1f);

        actualizarTextoContador(holder, respuesta);

        holder.btnValidar.setOnClickListener(v -> {
            if (listener != null && respuesta.isValidada() && !respuesta.isMostrarPuntaje()) {
                pausarTemporizadorDeRespuesta(respuesta);
                listener.onSolicitarVotacion(respuesta);
            }
        });
    }

    private void asegurarTemporizadorActivo(RespuestaValidacion respuesta) {
        if (respuesta == null ||
                respuesta.isMostrarPuntaje() ||
                respuesta.isVotacionResuelta() ||
                esRespuestaVacia(respuesta)) {
            return;
        }

        String clave = obtenerClaveRespuesta(respuesta);

        // Importante: el contador solo puede iniciar cuando ESA respuesta ya fue revelada.
        // Así evitamos que respuestas de categorías futuras gasten sus 5 segundos ocultas.
        if (!clavesReveladas.contains(clave)) {
            return;
        }

        if (!respuesta.isTimerIniciado() || !tiemposInicio.containsKey(clave)) {
            iniciarTemporizadorPuntaje(respuesta);
        }
    }

    private void mostrarBotonVotar(ViewHolder holder) {
        holder.txtPuntos.setVisibility(View.GONE);
        holder.txtPuntos.setAlpha(1f);

        holder.btnValidar.setVisibility(View.VISIBLE);
        holder.btnValidar.setAlpha(1f);
    }

    private void mostrarPuntaje(ViewHolder holder, RespuestaValidacion respuesta, int colorOutline) {
        holder.btnValidar.clearAnimation();
        holder.btnValidar.setOnClickListener(null);
        holder.btnValidar.setEnabled(false);
        holder.btnValidar.setClickable(false);
        holder.btnValidar.setAlpha(0f);
        holder.btnValidar.setVisibility(View.GONE);

        holder.txtPuntos.setVisibility(View.VISIBLE);
        holder.txtPuntos.setAlpha(1f);
        holder.txtPuntos.bringToFront();
        holder.txtPuntos.setText("+" + respuesta.getPuntos());
        holder.txtPuntos.setTextColor(colorOutline);
        android.graphics.drawable.GradientDrawable fondo = new android.graphics.drawable.GradientDrawable();
        float dp = holder.itemView.getResources().getDisplayMetrics().density;
        fondo.setCornerRadius(15 * dp);
        fondo.setColor(respuesta.getPuntos() == 10 ? Color.parseColor("#E9FBE8")
                : respuesta.getPuntos() == 5 ? Color.parseColor("#FFF7D6") : Color.parseColor("#FFF1F1"));
        fondo.setStroke(Math.round(2 * dp), colorOutline, 7 * dp, 4 * dp);
        holder.txtPuntos.setBackground(fondo);
    }

    private int obtenerColorPorPuntos(int puntos) {
        if (puntos == 10) {
            return Color.parseColor("#2E7D32");
        }

        if (puntos == 5) {
            return Color.parseColor("#8A6200");
        }

        return Color.parseColor("#C62828");
    }

    private void aplicarBordeRespuesta(TextView txtRespuesta, int colorOutline) {
        android.graphics.drawable.GradientDrawable gd =
                new android.graphics.drawable.GradientDrawable();

        gd.setColor(Color.WHITE);
        gd.setCornerRadius(40f);
        gd.setStroke(4, colorOutline);

        txtRespuesta.setBackground(gd);
    }

    private void iniciarTemporizadorPuntaje(RespuestaValidacion respuesta) {
        if (respuesta == null || (respuesta.isValidada() && respuesta.getPuntos() == 5) ||
                respuesta.isMostrarPuntaje() ||
                respuesta.isVotacionResuelta() ||
                esRespuestaVacia(respuesta)) {
            return;
        }

        String clave = obtenerClaveRespuesta(respuesta);
        cancelarTemporizadorPuntaje(respuesta);

        respuesta.setTimerIniciado(true);
        respuesta.setMostrarPuntaje(false);

        long inicio = System.currentTimeMillis();
        tiemposInicio.put(clave, inicio);
        final long[] lastSeg = {-1}; // Control de parpadeos
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Long inicioGuardado = tiemposInicio.get(clave);
                if (inicioGuardado == null) return;

                if (respuesta.isMostrarPuntaje() || respuesta.isVotacionResuelta() || esRespuestaVacia(respuesta)) {
                    cancelarTemporizadorPuntaje(respuesta);
                    notificarEstadoDeRespuesta(respuesta);
                    return;
                }

                long pasado = System.currentTimeMillis() - inicioGuardado;
                long restante = TIEMPO_BOTON_VOTAR_MS - pasado;

                if (restante <= 0) {
                    respuesta.setMostrarPuntaje(true);
                    respuesta.setTimerIniciado(false);
                    temporizadores.remove(clave);
                    tiemposInicio.remove(clave);
                    notificarEstadoDeRespuesta(respuesta);
                    return;
                }

                long segTotal = (long) Math.ceil(restante / 1000.0);
                if (segTotal != lastSeg[0]) {
                    lastSeg[0] = segTotal;
                    notificarContadorRespuesta(respuesta);
                }

                // Bucle de 50ms (Precisión máxima espejo a la Web)
                handler.postDelayed(this, 50);
            }
        };
        temporizadores.put(clave, runnable);
        handler.post(runnable);
    }

    private void pausarTemporizadorDeRespuesta(RespuestaValidacion respuesta) {
        cancelarTemporizadorPuntaje(respuesta);
        respuesta.setTimerIniciado(false);
        notificarEstadoDeRespuesta(respuesta);
    }

    private void cancelarTemporizadorPuntaje(RespuestaValidacion respuesta) {
        if (respuesta == null) {
            return;
        }

        String clave = obtenerClaveRespuesta(respuesta);
        Runnable anterior = temporizadores.get(clave);

        if (anterior != null) {
            handler.removeCallbacks(anterior);
            temporizadores.remove(clave);
        }

        tiemposInicio.remove(clave);
    }

    private void actualizarTextoContador(ViewHolder holder, RespuestaValidacion respuesta) {
        String clave = obtenerClaveRespuesta(respuesta);
        Long inicio = tiemposInicio.get(clave);

        if (inicio == null) {
            setTextoBotonVotar(holder, "VOTAR 5s");
            return;
        }

        long pasado = System.currentTimeMillis() - inicio;
        long restante = TIEMPO_BOTON_VOTAR_MS - pasado;

        if (restante <= 0) {
            setTextoBotonVotar(holder, "VOTAR");
            return;
        }

        int segundos = (int) Math.ceil(restante / 1000.0);
        setTextoBotonVotar(holder, "VOTAR " + segundos + "s");
    }

    private void notificarContadorRespuesta(RespuestaValidacion respuesta) {
        int index = respuestas.indexOf(respuesta);

        if (index >= 0) {
            notifyItemChanged(index, PAYLOAD_CONTADOR);
        }
    }

    private void notificarEstadoDeRespuesta(RespuestaValidacion respuesta) {
        int index = respuestas.indexOf(respuesta);

        if (index >= 0) {
            notifyItemChanged(index, PAYLOAD_ESTADO);
        } else {
            notifyDataSetChanged();
        }
    }

    public void actualizarValidaciones() {
        for (int i = 0; i < respuestas.size(); i++) {
            notifyItemChanged(i, PAYLOAD_ESTADO);
        }
    }

    public void pausarTemporizadoresPorVotacion(String claveVotacionActual) {
        clavesPausadasPorVotacion.clear();
        claveRespuestaEnVotacion = claveVotacionActual;

        for (RespuestaValidacion respuesta : respuestas) {
            if (respuesta == null) {
                continue;
            }

            String clave = obtenerClaveRespuesta(respuesta);

            if (respuesta.isValidada()
                    && !respuesta.isMostrarPuntaje()
                    && !respuesta.isVotacionResuelta()
                    && !esRespuestaVacia(respuesta)
                    && clavesReveladas.contains(clave)
                    && (claveVotacionActual == null || !claveVotacionActual.equals(clave))) {

                clavesPausadasPorVotacion.add(clave);
                pausarTemporizadorDeRespuesta(respuesta);
            }
        }
    }

    public void reiniciarTemporizadoresPausadosPorVotacion() {
        for (RespuestaValidacion respuesta : respuestas) {
            if (respuesta == null) {
                continue;
            }

            String clave = obtenerClaveRespuesta(respuesta);

            if (claveRespuestaEnVotacion != null && claveRespuestaEnVotacion.equals(clave)) {
                continue;
            }

            if (clavesPausadasPorVotacion.contains(clave)
                    && clavesReveladas.contains(clave)
                    && respuesta.isValidada()
                    && !respuesta.isMostrarPuntaje()
                    && !respuesta.isVotacionResuelta()
                    && !esRespuestaVacia(respuesta)) {

                respuesta.setMostrarPuntaje(false);
                respuesta.setTimerIniciado(false);
                iniciarTemporizadorPuntaje(respuesta);
            }
        }

        clavesPausadasPorVotacion.clear();
        claveRespuestaEnVotacion = null;

        for (int i = 0; i < respuestas.size(); i++) {
            notifyItemChanged(i, PAYLOAD_ESTADO);
        }
    }

    public void actualizarCooldown(long iniciadoEn, long duracionMs, long horaServidor) {
        // Ya no usamos cooldown global después de votar.
    }

    private String obtenerClaveRespuesta(RespuestaValidacion respuesta) {
        if (respuesta.getClaveVotacion() != null && !respuesta.getClaveVotacion().trim().isEmpty()) {
            return respuesta.getClaveVotacion();
        }

        String uid = respuesta.getUidJugador() == null ? "_" : respuesta.getUidJugador();
        String categoria = respuesta.getCategoria() == null ? "_" : respuesta.getCategoria();
        String texto = respuesta.getRespuesta() == null ? "_" : respuesta.getRespuesta();

        return uid + "_" + categoria + "_" + texto;
    }

    private boolean esRespuestaVacia(RespuestaValidacion respuesta) {
        return respuesta.getRespuesta() == null ||
                respuesta.getRespuesta().trim().isEmpty() ||
                respuesta.getRespuesta().equalsIgnoreCase("Sin respuesta");
    }

    private void setTextoBotonVotar(ViewHolder holder, String texto) {
        if (holder.txtBotonVotar != null) {
            holder.txtBotonVotar.setText(texto);
        }
    }

    @Override
    public int getItemCount() {
        return respuestas.size();
    }

    public void revelarSiguiente() {
        if (elementosRevelados < respuestas.size()) {
            int nuevoIndice = elementosRevelados;
            RespuestaValidacion respuesta = respuestas.get(nuevoIndice);
            elementosRevelados++;

            if (respuesta != null) {
                String clave = obtenerClaveRespuesta(respuesta);
                clavesReveladas.add(clave);

                cancelarTemporizadorPuntaje(respuesta);

                if (esRespuestaVacia(respuesta)) {
                    respuesta.setPuntos(0);
                    respuesta.setMostrarPuntaje(true);
                    respuesta.setTimerIniciado(false);
                } else if (!respuesta.isVotacionResuelta()) {
                    // Cada respuesta inicia limpia cuando aparece, no cuando se validó ni cuando apareció otra.
                    respuesta.setMostrarPuntaje(false);
                    respuesta.setTimerIniciado(false);
                }
            }

            notifyItemChanged(nuevoIndice);
        }
    }

    public boolean estanTodosRevelados() {
        return elementosRevelados >= respuestas.size();
    }

    public boolean todasLasRespuestasFinalizadas() {
        if (elementosRevelados < respuestas.size()) {
            return false;
        }

        for (RespuestaValidacion respuesta : respuestas) {
            if (respuesta == null) {
                continue;
            }

            if (!respuesta.isValidada()) {
                return false;
            }

            if (esRespuestaVacia(respuesta)) {
                respuesta.setPuntos(0);
                respuesta.setMostrarPuntaje(true);
                continue;
            }

            if (!respuesta.isMostrarPuntaje()) {
                return false;
            }
        }

        return true;
    }

    public void revelarTodos() {
        elementosRevelados = respuestas.size();
        clavesReveladas.clear();

        for (RespuestaValidacion respuesta : respuestas) {
            if (respuesta != null) {
                clavesReveladas.add(obtenerClaveRespuesta(respuesta));
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        handler.removeCallbacksAndMessages(null);

        temporizadores.clear();
        tiemposInicio.clear();
        clavesPausadasPorVotacion.clear();
        clavesReveladas.clear();
        claveRespuestaEnVotacion = null;

        for (RespuestaValidacion respuesta : respuestas) {
            if (respuesta != null && !respuesta.isMostrarPuntaje()) {
                respuesta.setTimerIniciado(false);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUsuario;
        TextView txtRespuesta;
        TextView txtPuntos;
        TextView txtBotonVotar;
        ImageView imgAvatar;
        View btnValidar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtUsuario = itemView.findViewById(R.id.txtUsuarioValidacion);
            txtRespuesta = itemView.findViewById(R.id.txtRespuestaValidacion);
            imgAvatar = itemView.findViewById(R.id.imgAvatarValidacion);
            btnValidar = itemView.findViewById(R.id.btnValidarAccion);
            txtPuntos = itemView.findViewById(R.id.txtPuntuacionValidacion);
            txtBotonVotar = itemView.findViewById(R.id.txtBotonVotarValidacion);
        }
    }
}
