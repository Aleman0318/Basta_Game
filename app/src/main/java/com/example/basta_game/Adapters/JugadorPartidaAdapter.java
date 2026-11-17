package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.basta_game.models.Jugador;
import com.example.basta_game.R;
import java.util.List;

public class JugadorPartidaAdapter extends RecyclerView.Adapter<JugadorPartidaAdapter.ViewHolder> {
    private List<Jugador> listaJugadores;

    public JugadorPartidaAdapter(List<Jugador> listaJugadores) {
        this.listaJugadores = listaJugadores;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_jugador_partida, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Jugador jugador = listaJugadores.get(position);
        holder.txtNombre.setText(com.example.basta_game.utils.IdentidadJugador.nombreValido(jugador.getNombre())
                ? jugador.getNombre() : "Cargando perfil...");

        Glide.with(holder.itemView.getContext())
                .load(jugador.getUrlPerfil())
                .placeholder(R.drawable.outline_person_24)
                .error(R.drawable.outline_person_24)
                .override(192,192).into(holder.imgAvatar);

        if(jugador.isEsHost()){
            holder.imgCoronaHost.setVisibility(View.VISIBLE);
        }else{
            holder.imgCoronaHost.setVisibility(View.INVISIBLE);
        }

        if (jugador.isEstaListo()) {
            holder.txtEstado.setText("Listo");
        } else {
            holder.txtEstado.setText("Escribiendo...");
        }
    }

    @Override
    public int getItemCount() { return listaJugadores.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtEstado;
        ImageView imgAvatar, imgCoronaHost;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreJugador);
            txtEstado = itemView.findViewById(R.id.txtEstadoJugador);
            imgAvatar = itemView.findViewById(R.id.imgAvatarJugadorPartida);
            imgCoronaHost = itemView.findViewById(R.id.imgCoronaHost);
        }
    }
}
