package com.example.basta_game.Adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import androidx.viewpager2.widget.ViewPager2;
import com.example.basta_game.R;
import com.example.basta_game.models.RespuestaValidacion;
import java.util.*;

public class TarjetasValidacionAdapter extends RecyclerView.Adapter<TarjetasValidacionAdapter.ViewHolder> {
    public interface ControlAnimacionCallback { boolean puedeContinuar(); }
    public interface OnAvanzarCategoriaListener { void onAvanzar(int indice); }
    private final List<String> categorias;
    private final ValidacionAdapter[] adapters;
    private final boolean[] cerradas;
    private final Runnable alTerminar;
    private final ViewPager2 pager;
    private boolean terminada;
    public TarjetasValidacionAdapter(List<String> categorias,Map<String,List<RespuestaValidacion>> respuestas,
            ViewPager2 pager,Runnable alTerminar,ValidacionAdapter.OnSolicitarVotacionListener votar,
            ControlAnimacionCallback control,boolean anfitrion,OnAvanzarCategoriaListener avanzar) {
        this.categorias=categorias; this.pager=pager; this.alTerminar=alTerminar;
        adapters=new ValidacionAdapter[categorias.size()]; cerradas=new boolean[categorias.size()];
        for(int i=0;i<adapters.length;i++) {
            List<RespuestaValidacion> lista=respuestas.get(categorias.get(i));
            adapters[i]=new ValidacionAdapter(lista==null ? Collections.emptyList() : lista,votar);
            adapters[i].revelarTodos();
        }
    }
    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p,int type) {
        return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_tarjeta_revision,p,false));
    }
    @Override public void onBindViewHolder(@NonNull ViewHolder h,int i) {
        h.titulo.setText(categorias.get(i));
        h.lista.setAdapter(adapters[i]);
        h.estado.setText(cerradas[i] ? "Categoria revisada" : "Desliza para ver todas las respuestas");
    }
    public boolean categoriaPreparada(int indice) { return indice>=0 && indice<adapters.length && adapters[indice].preparada(); }
    public void estadoCompartido(int indice,boolean disponible,boolean cerrada,boolean completa) {
        if(indice<0 || indice>=adapters.length) return;
        boolean cambio=cerradas[indice]!=cerrada; cerradas[indice]=cerrada;
        adapters[indice].estadoCompartido(disponible,cerrada);
        if(cambio) notifyItemChanged(indice);
        if(completa && !terminada) { terminada=true; pager.setUserInputEnabled(true); if(alTerminar!=null) alTerminar.run(); }
    }
    public void iniciarAnimacionEnPaginaPublica(int i) { /* Reveal is immediate; advancement belongs to Firebase. */ }
    public void actualizarControlAvance(boolean permitido) { }
    public boolean revisionFinalizada() { return terminada; }
    public void actualizarValidaciones() { for(ValidacionAdapter a:adapters) a.actualizarValidaciones(); }
    public void pausarTemporizadoresPorVotacion(String clave) { for(int i=0;i<adapters.length;i++) adapters[i].estadoCompartido(false,cerradas[i]); }
    public void reiniciarTemporizadoresPausadosPorVotacion() { actualizarValidaciones(); }
    public void actualizarCooldown(long inicio,long duracion,long ahora) { }
    @Override public int getItemCount() { return categorias.size(); }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titulo,estado; final RecyclerView lista;
        public ViewHolder(View v) { super(v); titulo=v.findViewById(R.id.txtNombreCategoriaTarjeta);
            estado=v.findViewById(R.id.txtDeslizarTarjeta); lista=v.findViewById(R.id.rvRespuestasTarjeta); lista.setLayoutManager(new LinearLayoutManager(v.getContext())); lista.setItemAnimator(null); lista.setHasFixedSize(false); }
    }
}
