package com.example.basta_game.Adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.basta_game.R;
import com.example.basta_game.models.RespuestaValidacion;
import com.example.basta_game.utils.EstadoRevision;
import java.util.List;

public class ValidacionAdapter extends RecyclerView.Adapter<ValidacionAdapter.ViewHolder> {
    public interface OnSolicitarVotacionListener { void onSolicitarVotacion(RespuestaValidacion respuesta); }
    private final List<RespuestaValidacion> respuestas;
    private final OnSolicitarVotacionListener listener;
    private int elementosRevelados;
    private boolean habilitada, cerrada;
    public ValidacionAdapter(List<RespuestaValidacion> respuestas,OnSolicitarVotacionListener listener) {
        this.respuestas=respuestas; this.listener=listener;
    }
    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_respuestas,parent,false));
    }
    @Override public void onBindViewHolder(@NonNull ViewHolder h,int position) {
        RespuestaValidacion r=respuestas.get(position);
        h.itemView.setVisibility(position<elementosRevelados ? View.VISIBLE : View.GONE);
        h.txtUsuario.setText(r.getNombreJugador());
        h.txtRespuesta.setText(EstadoRevision.vacia(r.getRespuesta()) ? "Sin respuesta" : r.getRespuesta());
        Glide.with(h.itemView.getContext()).load(r.getFotoPerfilUrl()).placeholder(R.drawable.ic_siluetas).error(R.drawable.ic_siluetas).override(192,192).into(h.imgAvatar);
        h.btnValidar.setOnClickListener(null);
        boolean definitiva=EstadoRevision.definitiva(r.getRespuesta(),r.isValidada(),r.getPuntos(),r.isVotacionResuelta(),r.isMostrarPuntaje()) || (cerrada && r.isValidada());
        if(definitiva) {
            if(EstadoRevision.vacia(r.getRespuesta())) r.setPuntos(0);
            r.setMostrarPuntaje(true);
            h.btnValidar.setVisibility(View.GONE); h.btnValidar.setEnabled(false);
            h.txtPuntos.setVisibility(View.VISIBLE); h.txtPuntos.setText("+"+r.getPuntos());
            int color=Color.parseColor(r.getPuntos()==10 ? "#2E7D32" : r.getPuntos()==5 ? "#8A6200" : "#C62828");
            float dp=h.itemView.getResources().getDisplayMetrics().density;
            GradientDrawable fondo=new GradientDrawable(); fondo.setCornerRadius(15*dp);
            fondo.setColor(Color.parseColor(r.getPuntos()==10 ? "#E9FBE8" : r.getPuntos()==5 ? "#FFF7D6" : "#FFF1F1"));
            fondo.setStroke(Math.round(2*dp),color,7*dp,4*dp); h.txtPuntos.setBackground(fondo); h.txtPuntos.setTextColor(color);
            h.txtRespuesta.setTextColor(color);
        } else {
            h.txtPuntos.setVisibility(View.GONE); h.btnValidar.setVisibility(View.VISIBLE);
            boolean puede=habilitada && r.isValidada();
            h.btnValidar.setEnabled(puede); h.btnValidar.setAlpha(puede ? 1f : .5f);
            h.txtBotonVotar.setText(!r.isValidada() ? "VALIDANDO..." : puede ? "VOTAR" : "ESPERANDO TURNO");
            h.txtRespuesta.setTextColor(androidx.core.content.ContextCompat.getColor(h.itemView.getContext(),R.color.colorPrimary));
            h.btnValidar.setOnClickListener(v -> {
                if(habilitada && !cerrada && r.isValidada() && !r.isMostrarPuntaje() && !r.isVotacionResuelta()) {
                    v.setEnabled(false); if(listener!=null) listener.onSolicitarVotacion(r);
                }
            });
        }
    }
    public void estadoCompartido(boolean disponible,boolean finalizada) {
        if(habilitada==disponible && cerrada==finalizada) return;
        habilitada=disponible; cerrada=finalizada; actualizarValidaciones();
    }
    @Override public void onViewRecycled(@NonNull ViewHolder h){Glide.with(h.itemView.getContext()).clear(h.imgAvatar);super.onViewRecycled(h);}
    public boolean preparada() {
        for(RespuestaValidacion r:respuestas) if(!r.isValidada() && !EstadoRevision.vacia(r.getRespuesta())) return false;
        return true;
    }
    public void actualizarValidaciones() { if(!respuestas.isEmpty()) notifyItemRangeChanged(0,respuestas.size()); }
    public void revelarTodos() { elementosRevelados=respuestas.size(); notifyDataSetChanged(); }
    public void revelarSiguiente() { if(elementosRevelados<respuestas.size()) notifyItemChanged(elementosRevelados++); }
    public boolean estanTodosRevelados() { return elementosRevelados==respuestas.size(); }
    public boolean todasLasRespuestasFinalizadas() { return cerrada && preparada(); }
    @Override public int getItemCount() { return respuestas.size(); }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtUsuario,txtRespuesta,txtPuntos,txtBotonVotar;
        final ImageView imgAvatar; final View btnValidar;
        public ViewHolder(View v) {
            super(v); txtUsuario=v.findViewById(R.id.txtUsuarioValidacion); txtRespuesta=v.findViewById(R.id.txtRespuestaValidacion);
            txtPuntos=v.findViewById(R.id.txtPuntuacionValidacion); txtBotonVotar=v.findViewById(R.id.txtBotonVotarValidacion);
            imgAvatar=v.findViewById(R.id.imgAvatarValidacion); btnValidar=v.findViewById(R.id.btnValidarAccion);
        }
    }
}