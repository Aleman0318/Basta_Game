package com.example.basta_game.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.basta_game.Adapters.ComodinTiendaAdapter;
import com.example.basta_game.R;
import com.example.basta_game.models.ComodinTienda;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.Sounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TiendaFragment extends Fragment {

    private TextView txtMonedasTienda, txtGemasTienda;
    private RecyclerView rvComodinesTienda;
    private View notificationTienda;

    private FirebaseAuth auth;
    private DatabaseReference usuarioRef;
    private ValueEventListener listenerUsuario;

    private ComodinTiendaAdapter adapter;
    private final List<ComodinTienda> listaComodines = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tienda, container, false);

        auth = FirebaseAuth.getInstance();

        txtMonedasTienda = view.findViewById(R.id.txtMonedasTienda);
        txtGemasTienda = view.findViewById(R.id.txtGemasTienda);
        rvComodinesTienda = view.findViewById(R.id.rvComodinesTienda);
        notificationTienda = view.findViewById(R.id.notificationTienda);

        notificationTienda.bringToFront();
        notificationTienda.setTranslationZ(40f);

        prepararCatalogoComodines();
        configurarRecycler();
        prepararFirebaseUsuario();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        prepararFirebaseUsuario();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            prepararFirebaseUsuario();
        }
    }

    private void prepararFirebaseUsuario() {
        if (auth == null) auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        DatabaseReference nuevaRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(auth.getCurrentUser().getUid());

        if (usuarioRef != null && listenerUsuario != null) {
            usuarioRef.removeEventListener(listenerUsuario);
            listenerUsuario = null;
        }

        usuarioRef = nuevaRef;
        escucharUsuario();
    }

    private void prepararCatalogoComodines() {
        listaComodines.clear();

        // 1. CAMBIO DE FLUJO (Íconos dinámicos: Escudo, Usuarios, Trofeo)
        listaComodines.add(new ComodinTienda(
                "uno_reversa",
                "Cambio de Flujo",
                "Intercambia tu puntaje con un jugador elegido aleatoriamente en la mesa. ¡Un arma de doble filo!",
                "<b>1 vs 1:</b> Tienes <font color='#304EDB'><b>65%</b></font> de riesgo de auto-sabotearte.\n" +
                        "<b>Si vas ganando:</b> En partidas de <font color='#304EDB'><b>3+ jug.</b></font> tienes 70% de riesgo de perder 20 pts.\n" +
                        "<b>Si vas último:</b> Tienes la ventaja, es casi seguro que le robes al <font color='#304EDB'><b>Top 3</b></font>.",
                "Caótico", "monedas", 1000, R.drawable.ic_comodin_cambiodeflujo, false
        ));

        // 2. DOBLE O NADA (Íconos dinámicos: Escudo, Trofeo, Espadas)
        listaComodines.add(new ComodinTienda(
                "doble_o_nada",
                "Doble o Nada",
                "Apuéstalo todo por tus conocimientos. Duplica tus puntos... o sufre las consecuencias.",
                "<b>Requisito:</b> Necesitas tener correctas al menos el <font color='#304EDB'><b>80%</b></font> de las categorías.\n" +
                        "<b>Recompensa:</b> Si lo logras, tu puntaje en la ronda se multiplica <font color='#304EDB'><b>x2</b></font>.\n" +
                        "<b>Fracaso:</b> Si fallas la meta, se te restarán <font color='#304EDB'><b>15 puntos</b></font> fijos.",
                "Riesgo", "monedas", 450, R.drawable.ic_comodin_dobleonada, false
        ));

        // 3. CONGELADOR (Íconos dinámicos: Efecto, Reloj, Reloj)
        listaComodines.add(new ComodinTienda(
                "congelador",
                "Congelador",
                "Congela el botón BASTA para todos, dándote tiempo de terminar de escribir a gusto.",
                "<b>Efecto:</b> Bloquea por completo el botón BASTA de <font color='#304EDB'><b>todos los rivales</b></font>.\n" +
                        "<b>Duración:</b> El congelamiento dura exactamente <font color='#304EDB'><b>8 segundos</b></font>.\n" +
                        "<b>Límite:</b> Inválido si el contador está en los últimos <font color='#304EDB'><b>10 segundos</b></font>.",
                "Control", "monedas", 250, R.drawable.ic_comodin_congelar, false
        ));

        // 4. RULETA CAÓTICA (Íconos dinámicos: Efecto, Usuarios, Trofeo)
        listaComodines.add(new ComodinTienda(
                "ruleta_caotica",
                "Ruleta Caótica",
                "Gira la ruleta y desata un evento aleatorio que afectará a toda la sala sin excepción.",
                "<b>Evento:</b> Lanza 1 de 5 locuras globales con <font color='#304EDB'><b>20%</b></font> de probabilidad cada una.\n" +
                        "<b>Riesgo:</b> <font color='#304EDB'><b>Todos</b></font> pueden perder sus puntos o intercambiarlos.\n" +
                        "<b>Ventaja:</b> Existe la posibilidad de robarle muchísimos puntos al <font color='#304EDB'><b>1er lugar</b></font>.",
                "Troll", "monedas", 350, R.drawable.ic_comodin_ruletacaotica, false
        ));

        // 5. ESCUDO DORADO (Íconos dinámicos: Escudo, Reloj, Usuarios)
        listaComodines.add(new ComodinTienda(
                "escudo_dorado",
                "Escudo Dorado",
                "Te protege automáticamente de cualquier comodín enemigo lanzado hacia ti.",
                "<b>Pasiva:</b> Te protege automáticamente del daño de comodines <font color='#304EDB'><b>agresivos</b></font>.\n" +
                        "<b>Duración:</b> Te otorga inmunidad absoluta para <font color='#304EDB'><b>1 solo ataque</b></font> en la partida.\n" +
                        "<b>Defensa:</b> Invalida por completo el efecto lanzado por el <font color='#304EDB'><b>rival</b></font>.",
                "Premium", "gemas", 10, R.drawable.ic_comodin_escudo, true
        ));

        // 6. MARCA DE LA MUERTE (El nuevo Comodín Premium)
        listaComodines.add(new ComodinTienda(
                "marca_de_la_muerte",
                "Marca de la Muerte",
                "Selecciona a tu víctima, arrástrala a un minijuego a muerte y róbale su puntaje a la vista de todos.",
                "<b>Objetivo:</b> Tú eliges a qué <font color='#304EDB'><b>rival</b></font> vas a marcar.\n" +
                        "<b>Evento:</b> Se activará un <font color='#304EDB'><b>duelo aleatorio</b></font> (Piedra/Papel/Tijera, Tres en Raya o Reflejos).\n" +
                        "<b>Éxito:</b> Si ganas el minijuego, le <font color='#304EDB'><b>robarás</b></font> todos sus puntos.",
                "Premium", "gemas", 15, R.drawable.ic_comodin_marcadelamuerte, true // Usa el ícono de espadas de tu diseñadora
        ));
    }

    private void configurarRecycler() {
        adapter = new ComodinTiendaAdapter(listaComodines, this::comprarComodin);
        rvComodinesTienda.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComodinesTienda.setAdapter(adapter);
    }

    private void escucharUsuario() {
        if (usuarioRef == null) return;

        listenerUsuario = usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int monedas = leerEntero(snapshot.child("monedas"), 0);
                int gemas = leerEntero(snapshot.child("gemas"), 0);

                txtMonedasTienda.setText(String.valueOf(monedas));
                txtGemasTienda.setText(String.valueOf(gemas));

                Map<String, Integer> cantidades = new HashMap<>();
                DataSnapshot inventarioSnap = snapshot.child("inventarioComodines");

                for (DataSnapshot comodinSnap : inventarioSnap.getChildren()) {
                    String id = comodinSnap.getKey();
                    int cantidad = leerEntero(comodinSnap.child("cantidad"), 0);
                    if (id != null) cantidades.put(id, cantidad);
                }
                if (adapter != null) adapter.actualizarInventario(cantidades);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void comprarComodin(ComodinTienda comodin) {
        if (usuarioRef == null || comodin == null) return;

        usuarioRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String campoSaldo = comodin.getTipoMoneda().equalsIgnoreCase("gemas") ? "gemas" : "monedas";
                int saldoActual = leerEnteroMutable(currentData.child(campoSaldo), 0);

                if (saldoActual < comodin.getPrecio()) return Transaction.abort();

                int cantidadActual = leerEnteroMutable(currentData.child("inventarioComodines").child(comodin.getId()).child("cantidad"), 0);

                currentData.child(campoSaldo).setValue(saldoActual - comodin.getPrecio());
                currentData.child("inventarioComodines").child(comodin.getId()).child("cantidad").setValue(cantidadActual + 1);
                currentData.child("inventarioComodines").child(comodin.getId()).child("nombre").setValue(comodin.getNombre());
                currentData.child("inventarioComodines").child(comodin.getId()).child("tipoMoneda").setValue(comodin.getTipoMoneda());

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    NotificationHelper.show(notificationTienda, "Error al comprar comodín", NotificationHelper.ERROR);
                    Sounds.playNotificaciones();
                    return;
                }
                if (committed) {
                    NotificationHelper.show(notificationTienda, "Compraste " + comodin.getNombre(), NotificationHelper.SUCCESS);
                    Sounds.playNotificaciones();
                } else {
                    String moneda = comodin.getTipoMoneda().equalsIgnoreCase("gemas") ? "gemas" : "monedas";
                    NotificationHelper.show(notificationTienda, "No tienes suficientes " + moneda, NotificationHelper.WARNING);
                    Sounds.playNotificaciones();
                }
            }
        });
    }

    private int leerEntero(DataSnapshot snapshot, int valorDefault) {
        Object valor = snapshot.getValue();
        if (valor instanceof Number) return ((Number) valor).intValue();
        if (valor instanceof String) {
            try { return Integer.parseInt((String) valor); } catch (NumberFormatException e) { return valorDefault; }
        }
        return valorDefault;
    }

    private int leerEnteroMutable(MutableData data, int valorDefault) {
        Object valor = data.getValue();
        if (valor instanceof Number) return ((Number) valor).intValue();
        if (valor instanceof String) {
            try { return Integer.parseInt((String) valor); } catch (NumberFormatException e) { return valorDefault; }
        }
        return valorDefault;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usuarioRef != null && listenerUsuario != null) {
            usuarioRef.removeEventListener(listenerUsuario);
            listenerUsuario = null;
        }
    }
}