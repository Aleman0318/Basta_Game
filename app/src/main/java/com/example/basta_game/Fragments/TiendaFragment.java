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
    private android.app.Dialog dialogoCompra;

    private FirebaseAuth auth;
    private DatabaseReference usuarioRef;
    private ValueEventListener listenerUsuario;
    private com.example.basta_game.utils.ComprasViewModel compras;

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

    @Override public void onViewCreated(@NonNull View view, Bundle state) {
        super.onViewCreated(view,state);
        compras=new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.example.basta_game.utils.ComprasViewModel.class);
        compras.enCurso.observe(getViewLifecycleOwner(), ocupado -> {
            if(adapter!=null) adapter.compraEnCurso(Boolean.TRUE.equals(ocupado));
        });
        compras.confirmado.observe(getViewLifecycleOwner(), datos -> {
            if(datos!=null && auth.getCurrentUser()!=null && auth.getCurrentUser().getUid().equals(datos.getKey())) pintarUsuario(datos);
        });
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
                "Maldice dos categorías con una vocal prohibida. Quien la utilice perderá puntos.",
                "Control", "efecto_global", R.drawable.ic_comodin_decreto_silencio, 350, "monedas",
                "<b>Efecto:</b> Invalida automáticamente las palabras con la <font color='#304EDB'><b>vocal prohibida</b></font>.\n" +
                        "<b>Usuarios:</b> Afecta a <font color='#304EDB'><b>toda la sala</b></font> incluyéndote, solo en dos categorías distintas.\n" +
                        "<b>Fracaso:</b> Si usas la vocal (también acentuada), tu respuesta vale <font color='#304EDB'><b>0 puntos</b></font> y recibes una multa única de 5 puntos por categoría, sin saldo negativo. Vacías no infringen. No hay bono por cumplir. Se activa antes de BASTA, con al menos 20 segundos libres, y el aviso debe llegar a cada jugador para poder sancionarlo."));

        listaControl.add(new ComodinTienda("inversion_letras", "El Espejo",
                "¡Revuelve la mente de un rival! Invierte lo que escriba en hasta 3 categorías aleatorias durante 30 segundos, aunque estén vacías. ¡Después tendrá que corregir las respuestas por su cuenta!",
                "Control", "ataque", R.drawable.ic_comodin_espejo, 1650, "monedas",
                "<b>Objetivo:</b> Elige a un <font color='#304EDB'><b>rival</b></font> y ataca hasta <b>3 categorías en posiciones separadas</b> entre todas las categorías disponibles, incluso si todavía están vacías.\n" +
                        "<b>Confusión:</b> Durante <font color='#7442B7'><b>30 segundos</b></font> todo lo que escriba en las categorías afectadas se verá invertido (Gato → otaG); un borde morado y un reloj indican las afectadas.\n" +
                        "<b>Consecuencia:</b> Cuando el reloj termine podrá corregirlas, pero <b>las palabras no se arreglan solas</b>: si deja 'otaG', esa será la respuesta evaluada.\n" +
                        "<b>Límite:</b> Se activa antes de BASTA. Si alguien pulsa BASTA, la confusión termina pero las palabras invertidas permanecen para corregirlas en los últimos 10 s."));

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
                "Riesgo", "estrategia", R.drawable.ic_comodin_oraculo_ciego, 500, "monedas",
                "<b>Requisito:</b> Tu palabra debe ser <font color='#304EDB'><b>única y válida</b></font>.\n" +
                        "<b>Recompensa:</b> Duplicas únicamente el puntaje normal de <font color='#304EDB'><b>esa categoría</b></font>.\n" +
                        "<b>Castigo:</b> Si está repetida, pierdes un <font color='#304EDB'><b>20%</b></font> de tu ronda total."));

        listaRiesgo.add(new ComodinTienda("impuesto_revol", "Impuesto Revolucionario",
                "Roba 5 puntos temporales a un rival. Si al final le ganas, te los quedas.",
                "Riesgo", "ataque", R.drawable.ic_comodin_impuesto, 400, "monedas",
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
                "Caótico", "efecto_global", R.drawable.ic_comodin_viento, 600, "monedas",
                "<b>Efecto:</b> Las categorías cambiarán de lugar en <font color='#304EDB'><b>todas las pantallas</b></font>.\n" +
                        "<b>Duración:</b> <font color='#304EDB'><b>10 segundos desde su activación</b></font>, sin superar el cierre de respuestas.\n" +
                        "<b>Riesgo:</b> Puede que escribas un país en el espacio de <font color='#304EDB'><b>nombre</b></font>."));

        listaCategorias.add(new CategoriaTienda("Caóticas", "Siembra el caos y sorprende a todos.", R.drawable.bg_contenedor_caotico, listaCaoticas));


        // ==========================================
        // 4. CATEGORÍA: LEGENDARIAS
        // ==========================================
        List<ComodinTienda> listaLegendarias = new ArrayList<>();

        listaLegendarias.add(new ComodinTienda("escudo_dorado", "Escudo Dorado",
                "Equipa un escudo secreto para toda la ronda: solo se revela cuando bloquea un ataque.",
                "Premium", "defensa", R.drawable.ic_comodin_escudo_dorado, 10, "gemas",
                "<b>Activación manual:</b> Consume una unidad mientras respondes. Te protege de ataques dirigidos <font color='#304EDB'><b>agresivos</b></font>.\n" +
                        "<b>Secreto:</b> Nadie sabrá que lo tienes equipado hasta que bloquee un ataque.\n" +
                        "<b>Duración:</b> Protección hasta terminar los resultados de esta ronda.\n" +
                        "<b>Defensa:</b> Bloquea Espejo, Cambio de Flujo y Marca de la Muerte. <font color='#304EDB'><b>¡PARRY!</b></font> El atacante también pierde su unidad cuando es bloqueado. No detiene efectos globales."));

        listaLegendarias.add(new ComodinTienda("marca_de_la_muerte", "Marca de la Muerte",
                "Reta a un rival a un duelo. Si ganas, robas el 50 % de sus puntos actuales.",
                "Premium", "ataque", R.drawable.ic_comodin_marcadelamuerte, 15, "gemas",
                "<b>Objetivo:</b> Tú eliges a qué <font color='#304EDB'><b>rival</b></font> vas a marcar.\n" +
                        "<b>Evento:</b> Se activará un <font color='#304EDB'><b>duelo aleatorio</b></font> (TER, PPT o Tiroteo).\n" +
                        "<b>Éxito:</b> Si ganas el minijuego, le <font color='#304EDB'><b>robarás</b></font> el 50 % de sus puntos actuales, redondeado hacia abajo (100 → 50 robados). Si pierdes, conserva la consecuencia existente."));

        listaLegendarias.add(new ComodinTienda("chismoso_suerte", "Chismoso de la Suerte",
                "Te marca con amarillo si alguien escribió tu misma palabra, pero te congela 5s.",
                "Premium", "estrategia", R.drawable.ic_comodin_chismoso, 12, "gemas",
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
        if(usuarioRef==null) return;
        listenerUsuario=usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(getView()!=null) pintarUsuario(snapshot);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                NotificationHelper.avisoTienda("No se pudo actualizar el inventario",NotificationHelper.WARNING);
            }
        });
    }
    private void pintarUsuario(DataSnapshot snapshot) {
        txtMonedasTienda.setText(String.valueOf(leerEntero(snapshot.child("monedas"),0)));
        txtGemasTienda.setText(String.valueOf(leerEntero(snapshot.child("gemas"),0)));
        Map<String,Integer> cantidades=new HashMap<>();
        for(DataSnapshot item:snapshot.child("inventarioComodines").getChildren())
            if(item.getKey()!=null) cantidades.put(item.getKey(),leerEntero(item.child("cantidad"),0));
        if(adapter!=null) adapter.actualizarInventario(cantidades);
    }
    private void comprarComodin(ComodinTienda comodin) {
        if(!com.example.basta_game.utils.CompraComodin.disponible(comodin.getId())) {
            NotificationHelper.avisoTienda("Chismoso de la Suerte está en preparación; no se puede comprar todavía.",NotificationHelper.INFO);return;
        }
        if(compras==null || com.example.basta_game.utils.ComprasViewModel.confirmacionAbierta ||
                Boolean.TRUE.equals(compras.enCurso.getValue())) return;
        com.example.basta_game.utils.ComprasViewModel.confirmacionAbierta=true;
        String moneda="gemas".equals(comodin.getTipoMoneda()) ? "gemas" : "monedas";
        android.app.Dialog dialogo=com.example.basta_game.utils.CustomDialogHelper.show(requireContext(),
                "Confirmar compra","¿Quieres comprar "+comodin.getNombre()+" por "+comodin.getPrecio()+" "+moneda+"?",true,
                new com.example.basta_game.utils.CustomDialogHelper.DialogButton("Cancelar",android.graphics.Color.GRAY,android.graphics.Color.WHITE,true,null),
                new com.example.basta_game.utils.CustomDialogHelper.DialogButton("Comprar",android.graphics.Color.rgb(11,76,227),android.graphics.Color.WHITE,true,
                        () -> compras.comprar(usuarioRef,comodin)));
        dialogoCompra=dialogo;
        dialogo.setOnDismissListener(d -> {dialogoCompra=null;com.example.basta_game.utils.ComprasViewModel.confirmacionAbierta=false;});
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
        if(dialogoCompra!=null) dialogoCompra.dismiss();
        super.onDestroyView();
        if (usuarioRef != null && listenerUsuario != null) {
            usuarioRef.removeEventListener(listenerUsuario);
            listenerUsuario = null;
        }
    }
}
