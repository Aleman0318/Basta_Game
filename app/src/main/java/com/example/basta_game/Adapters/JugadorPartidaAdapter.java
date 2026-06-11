package com.example.basta_game.Adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
        // Asegúrate de tener tu XML item_jugador_partida alineado con tu diseño
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_jugador_partida, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Jugador jugador = listaJugadores.get(position);
        holder.txtNombre.setText(jugador.getNombre());
        // Aquí asumiríamos que tu modelo Jugador tiene un método getPuntos() y getEstadoPartida()
        // holder.txtPuntos.setText(jugador.getPuntos() + " ⭐");
    }

    @Override
    public int getItemCount() { return listaJugadores.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtPuntos, txtEstado;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Reemplaza con los IDs correctos de tu item_jugador_partida.xml
            txtNombre = itemView.findViewById(R.id.txtNombreJugador);
        }
    }
}