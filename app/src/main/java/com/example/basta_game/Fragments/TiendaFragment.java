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

import com.example.basta_game.Adapters.CategoriaTiendaAdapter;
import com.example.basta_game.Adapters.ComodinTiendaAdapter;
import com.example.basta_game.R;
import com.example.basta_game.models.CategoriaTienda;
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

    // AHORA USAMOS EL ADAPTER PADRE
    private CategoriaTiendaAdapter adapter;
    private final List<CategoriaTienda> listaCategorias = new ArrayList<>();

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
        listaCategorias.clear();

        // ==========================================
        // 1. CATEGORÍA: CONTROL
        // ==========================================
        List<ComodinTienda> listaControl = new ArrayList<>();

        listaControl.add(new ComodinTienda("congelador", "Congelador",
                "Congela el botón BASTA para todos, dándote tiempo de terminar de escribir a gusto.",
                "Control", "defensa", R.drawable.ic_comodin_congelar, 250, "monedas",
                "<b>Efecto:</b> Bloquea el botón BASTA de <font color='#304EDB'><b>todos los rivales</b></font>.\n" +
                        "<b>Duración:</b> El efecto dura exactamente <font color='#304EDB'><b>8 segundos</b></font>.\n" +
                        "<b>Límite:</b> Inválido si el contador está en los últimos <font color='#304EDB'><b>10 segundos</b></font>."));

        listaControl.add(new ComodinTienda("decreto_silencio", "Decreto del Silencio",
                "Bloquea una vocal al azar. Nadie en la sala podrá escribir respuestas con esa letra.",
                "Control", "efecto_global", R.drawable.ic_comodin_ruletacaotica, 350, "monedas",
                "<b>Efecto:</b> Invalida automáticamente las palabras con la <font color='#304EDB'><b>vocal prohibida</b></font>.\n" +
                        "<b>Usuarios:</b> Afecta a <font color='#304EDB'><b>toda la sala</b></font> incluyéndote a ti.\n" +
                        "<b>Fracaso:</b> Si usas la letra, tu respuesta vale <font color='#304EDB'><b>0 puntos</b></font>."));

        listaControl.add(new ComodinTienda("inversion_letras", "El Espejo",
                "Las respuestas de tu rival se escribirán al revés (ej. otaG en vez de Gato).",
                "Control", "ataque", R.drawable.ic_comodin_cambiodeflujo, 300, "monedas",
                "<b>Efecto:</b> Retrasa a tu <font color='#304EDB'><b>rival</b></font> obligándolo a borrar y corregir.\n" +
                        "<b>Duración:</b> La interferencia dura <font color='#304EDB'><b>15 segundos</b></font>.\n" +
                        "<b>Límite:</b> No borra puntos, solo pone a prueba la <font color='#304EDB'><b>velocidad</b></font>."));

        listaCategorias.add(new CategoriaTienda("Control", "Maneja el tiempo y protege tus puntos.", R.drawable.bg_contenedor_control, listaControl));


        // ==========================================
        // 2. CATEGORÍA: RIESGO
        // ==========================================
        List<ComodinTienda> listaRiesgo = new ArrayList<>();

        listaRiesgo.add(new ComodinTienda("doble_o_nada", "Doble o Nada",
                "Apuéstalo todo por tus conocimientos. Duplica tus puntos... o sufre las consecuencias.",
                "Riesgo", "estrategia", R.drawable.ic_comodin_dobleonada, 450, "monedas",
                "<b>Requisito:</b> Tener correctas al menos el <font color='#304EDB'><b>80%</b></font> de tus categorías.\n" +
                        "<b>Recompensa:</b> Tu puntaje en la ronda se multiplica <font color='#304EDB'><b>x2</b></font>.\n" +
                        "<b>Fracaso:</b> Si fallas la meta, se te restan <font color='#304EDB'><b>15 puntos</b></font>."));

        listaRiesgo.add(new ComodinTienda("oraculo_ciego", "Oráculo Ciego",
                "Apuesta a que una de tus categorías será única. Si aciertas ganas más, si fallas te maldicen.",
                "Riesgo", "estrategia", R.drawable.ic_comodin_dobleonada, 500, "monedas",
                "<b>Requisito:</b> Tu palabra debe ser <font color='#304EDB'><b>única y válida</b></font>.\n" +
                        "<b>Recompensa:</b> Obtienes un bono multiplicador en <font color='#304EDB'><b>esa categoría</b></font>.\n" +
                        "<b>Castigo:</b> Si está repetida, pierdes un <font color='#304EDB'><b>20%</b></font> de tu ronda total."));

        listaRiesgo.add(new ComodinTienda("impuesto_revol", "Impuesto Revolucionario",
                "Roba 5 puntos temporales a un rival. Si al final le ganas, te los quedas.",
                "Riesgo", "ataque", R.drawable.ic_comodin_ruletacaotica, 400, "monedas",
                "<b>Efecto:</b> Absorbes 5 puntos de un <font color='#304EDB'><b>rival</b></font> al iniciar.\n" +
                        "<b>Recompensa:</b> Si superas su puntaje al final, el robo se hace <font color='#304EDB'><b>permanente</b></font>.\n" +
                        "<b>Castigo:</b> Si él te gana, devuelves los 5 pts más una <font color='#304EDB'><b>multa de 5 pts</b></font>."));

        listaCategorias.add(new CategoriaTienda("Riesgo", "Estrategias arriesgadas para cambiarlo todo.", R.drawable.bg_contenedor_riesgo, listaRiesgo));


        // ==========================================
        // 3. CATEGORÍA: CAÓTICAS
        // ==========================================
        List<ComodinTienda> listaCaoticas = new ArrayList<>();

        listaCaoticas.add(new ComodinTienda("cambio_flujo", "Cambio de Flujo",
                "Intercambia tu puntaje con un jugador elegido aleatoriamente en la mesa.",
                "Caótico", "efecto_global", R.drawable.ic_comodin_cambiodeflujo, 1000, "monedas",
                "<b>Riesgo:</b> Tienes <font color='#304EDB'><b>65%</b></font> de auto-sabotearte en 1vs1.\n" +
                        "<b>Si vas ganando:</b> 70% de riesgo de <font color='#304EDB'><b>perder pts</b></font>.\n" +
                        "<b>Si vas último:</b> Casi seguro le robas al <font color='#304EDB'><b>Top 3</b></font>."));

        listaCaoticas.add(new ComodinTienda("ruleta_caotica", "Ruleta Caótica",
                "Gira la ruleta y desata un evento aleatorio que afectará a toda la sala.",
                "Caótico", "efecto_global", R.drawable.ic_comodin_ruletacaotica, 350, "monedas",
                "<b>Evento:</b> Lanza 1 de 5 locuras con <font color='#304EDB'><b>20%</b></font> de probabilidad cada una.\n" +
                        "<b>Usuarios:</b> <font color='#304EDB'><b>Todos</b></font> pueden perder sus puntos o intercambiarlos.\n" +
                        "<b>Ventaja:</b> Posibilidad de robarle mucho al <font color='#304EDB'><b>1er lugar</b></font>."));

        listaCaoticas.add(new ComodinTienda("viento_caos", "Viento del Caos",
                "Desordena visualmente las casillas del tablero de todos los jugadores.",
                "Caótico", "efecto_global", R.drawable.ic_comodin_cambiodeflujo, 600, "monedas",
                "<b>Efecto:</b> Las categorías cambiarán de lugar en <font color='#304EDB'><b>todas las pantallas</b></font>.\n" +
                        "<b>Duración:</b> Activo durante los primeros <font color='#304EDB'><b>10 segundos</b></font>.\n" +
                        "<b>Riesgo:</b> Puede que escribas un país en el espacio de <font color='#304EDB'><b>nombre</b></font>."));

        listaCategorias.add(new CategoriaTienda("Caóticas", "Siembra el caos y sorprende a todos.", R.drawable.bg_contenedor_caotico, listaCaoticas));


        // ==========================================
        // 4. CATEGORÍA: LEGENDARIAS
        // ==========================================
        List<ComodinTienda> listaLegendarias = new ArrayList<>();

        listaLegendarias.add(new ComodinTienda("escudo_dorado", "Escudo Dorado",
                "Te protege automáticamente de cualquier comodín enemigo lanzado hacia ti.",
                "Premium", "defensa", R.drawable.ic_comodin_escudo, 10, "gemas",
                "<b>Pasiva:</b> Te protege del daño de comodines <font color='#304EDB'><b>agresivos</b></font>.\n" +
                        "<b>Duración:</b> Inmunidad para <font color='#304EDB'><b>1 solo ataque</b></font> en la partida.\n" +
                        "<b>Defensa:</b> Invalida por completo el efecto lanzado por el <font color='#304EDB'><b>rival</b></font>."));

        listaLegendarias.add(new ComodinTienda("marca_de_la_muerte", "Marca de la Muerte",
                "Selecciona a tu víctima, arrástrala a un minijuego a muerte y róbale su puntaje.",
                "Premium", "ataque", R.drawable.ic_comodin_marcadelamuerte, 15, "gemas",
                "<b>Objetivo:</b> Tú eliges a qué <font color='#304EDB'><b>rival</b></font> vas a marcar.\n" +
                        "<b>Evento:</b> Se activará un <font color='#304EDB'><b>duelo aleatorio</b></font> (TER, PPT o Tiroteo).\n" +
                        "<b>Éxito:</b> Si ganas el minijuego, le <font color='#304EDB'><b>robarás</b></font> sus puntos."));

        listaLegendarias.add(new ComodinTienda("chismoso_suerte", "Chismoso de la Suerte",
                "Te marca con amarillo si alguien escribió tu misma palabra, pero te congela 5s.",
                "Premium", "estrategia", R.drawable.ic_comodin_escudo, 12, "gemas",
                "<b>Efecto:</b> Descubre si tu palabra está <font color='#304EDB'><b>repetida</b></font> en la sala.\n" +
                        "<b>Castigo:</b> Te bloquea la edición de esa celda por <font color='#304EDB'><b>5 segundos</b></font>.\n" +
                        "<b>Recompensa:</b> Te da tiempo para pensar un sinónimo <font color='#304EDB'><b>único</b></font>."));

        listaCategorias.add(new CategoriaTienda("Legendarias", "Ventajas únicas y poderes ocultos.", R.drawable.bg_contenedor_premium, listaLegendarias));
    }

    private void configurarRecycler() {
        adapter = new CategoriaTiendaAdapter(listaCategorias, this::comprarComodin);
        rvComodinesTienda.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Esto le da un extra de fluidez al scroll vertical principal
        rvComodinesTienda.setHasFixedSize(true);
        rvComodinesTienda.setItemViewCacheSize(10);
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