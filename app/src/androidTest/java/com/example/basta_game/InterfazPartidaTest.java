package com.example.basta_game;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.basta_game.Adapters.CategoriaPartidaAdapter;
import com.example.basta_game.Adapters.ValidacionAdapter;
import com.example.basta_game.models.Categoria;
import com.example.basta_game.models.RespuestaValidacion;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Local views only: no Activity navigation, Auth or Database calls. IME still needs a device check. */
@RunWith(AndroidJUnit4.class)
public class InterfazPartidaTest {
    @Test public void compraPendienteBloqueaToquesYRebindMuestraInventarioConfirmado() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c=contexto();
            java.util.concurrent.atomic.AtomicBoolean ocupado=new java.util.concurrent.atomic.AtomicBoolean(true);
            com.example.basta_game.models.ComodinTienda item=new com.example.basta_game.models.ComodinTienda(
                    "congelar","Congelador","Bloquea BASTA","Control","ataque",R.drawable.ic_siluetas,10,"monedas","");
            com.example.basta_game.Adapters.ComodinTiendaAdapter adapter=new com.example.basta_game.Adapters.ComodinTiendaAdapter(
                    Collections.singletonList(item),v -> fail("No debe comprar mientras esta ocupado"),ocupado::get);
            RecyclerView parent=new RecyclerView(c);
            com.example.basta_game.Adapters.ComodinTiendaAdapter.ViewHolder holder=adapter.createViewHolder(parent,0);
            adapter.bindViewHolder(holder,0);
            View boton=holder.itemView.findViewById(R.id.btnComprarComodinItem);
            assertFalse(boton.isEnabled()); boton.performClick();
            ocupado.set(false); adapter.actualizarInventario(Collections.singletonMap("congelar",7));
            adapter.bindViewHolder(holder,0);
            assertTrue(boton.isEnabled());
            assertEquals("TIENES: 7",((TextView)holder.itemView.findViewById(R.id.txtCantidadInventarioItem)).getText().toString());
        });
    }

    @Test public void hojaTieneScrollInternoYBastaPermaneceFueraDeLaLista() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c=contexto();
            for(int ancho:new int[]{320,800}) {
                View pantalla=android.view.LayoutInflater.from(c).inflate(R.layout.fragment_partida,new FrameLayout(c),false);
                RecyclerView lista=pantalla.findViewById(R.id.rvCategoriasJuego);
                CategoriaPartidaAdapter.configurarScrollInterno(lista);
                Map<String,String> respuestas=new HashMap<>();
                java.util.List<Categoria> categorias=new ArrayList<>();
                for(int i=0;i<20;i++) categorias.add(new Categoria("Categoria "+i,R.drawable.ic_siluetas));
                lista.setAdapter(new CategoriaPartidaAdapter(categorias,respuestas));
                int w=Math.round(ancho*c.getResources().getDisplayMetrics().density);
                int h=Math.round(640*c.getResources().getDisplayMetrics().density);
                pantalla.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
                pantalla.layout(0,0,w,h);
                View basta=pantalla.findViewById(R.id.btnBasta);
                android.graphics.Rect antes=new android.graphics.Rect();basta.getDrawingRect(antes);
                ((android.view.ViewGroup)pantalla).offsetDescendantRectToMyCoords(basta,antes);
                assertTrue(lista.getLayoutManager().canScrollVertically());
                assertFalse(lista.isVerticalScrollBarEnabled());assertTrue(lista.getHeight()>0);
                lista.scrollToPosition(19); lista.measure(View.MeasureSpec.makeMeasureSpec(lista.getWidth(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(lista.getHeight(),View.MeasureSpec.EXACTLY));
                lista.layout(lista.getLeft(),lista.getTop(),lista.getRight(),lista.getBottom());
                RecyclerView.ViewHolder holder=lista.findViewHolderForAdapterPosition(19);
                assertNotNull(holder);
                EditText campo=holder.itemView.findViewById(R.id.etRespuestaCategoriaPartida);
                campo.requestFocus();campo.setText("Armadillo");
                assertEquals("Armadillo",respuestas.get("Categoria 19"));
                android.graphics.Rect despues=new android.graphics.Rect();basta.getDrawingRect(despues);
                ((android.view.ViewGroup)pantalla).offsetDescendantRectToMyCoords(basta,despues);
                assertEquals(antes,despues);assertTrue(lista.getPreserveFocusAfterLayout());
            }
        });
    }
    private Context contexto() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new ContextThemeWrapper(app, app.getApplicationInfo().theme);
    }

    @Test public void tarjetaCabeEnTelefonosYTablets() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context contexto = contexto();
            float densidad = contexto.getResources().getDisplayMetrics().density;
            for (int ancho : new int[]{240,320,360,600,800}) {
                View fila = android.view.LayoutInflater.from(contexto).inflate(R.layout.item_respuestas, new FrameLayout(contexto), false);
                ((TextView) fila.findViewById(R.id.txtRespuestaValidacion)).setText("POLLO RANCIO CON UNA RESPUESTA MUY LARGA");
                int pixels = Math.round(ancho * densidad);
                fila.measure(View.MeasureSpec.makeMeasureSpec(pixels,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                fila.layout(0,0,pixels,fila.getMeasuredHeight());
                View palabra = fila.findViewById(R.id.txtRespuestaValidacion);
                View votar = fila.findViewById(R.id.btnValidarAccion);
                android.graphics.Rect rect = new android.graphics.Rect();
                votar.getDrawingRect(rect);
                ((android.view.ViewGroup)fila).offsetDescendantRectToMyCoords(votar,rect);
                assertTrue(rect.left >= 0 && rect.right <= pixels);
                assertTrue(votar.getMeasuredHeight() >= Math.round(48 * densidad));
                assertTrue(palabra.getMeasuredWidth() > 0);
                assertTrue(votar.getMeasuredWidth() >= palabra.getMeasuredWidth());
            }
        });
    }

    @Test public void resultadosReemplazanBotonSinVotoNuevo() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context contexto = contexto();
            for (int puntos : new int[]{0,5,10}) {
                RespuestaValidacion respuesta = new RespuestaValidacion("u","Diego",puntos == 0 ? "" : "Manzana",null);
                respuesta.setValidada(true); respuesta.setPuntos(puntos);
                respuesta.setVotacionResuelta(puntos == 10);
                ValidacionAdapter adapter = new ValidacionAdapter(Collections.singletonList(respuesta),r -> fail("Resultado resuelto no votable"));
                adapter.revelarTodos();
                RecyclerView parent = new RecyclerView(contexto);
                ValidacionAdapter.ViewHolder holder = adapter.createViewHolder(parent,0);
                adapter.bindViewHolder(holder,0);
                assertEquals(View.GONE,holder.itemView.findViewById(R.id.btnValidarAccion).getVisibility());
                assertEquals("+" + puntos,((TextView)holder.itemView.findViewById(R.id.txtPuntuacionValidacion)).getText().toString());
                adapter.onDetachedFromRecyclerView(parent);
            }
        });
    }

    @Test public void rebindDeCampoConservaRespuestaEspaciosYCursor() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context contexto = contexto();
            Map<String,String> respuestas = new HashMap<>();
            CategoriaPartidaAdapter adapter = new CategoriaPartidaAdapter(
                    Collections.singletonList(new Categoria("Animal",R.drawable.ic_siluetas)),respuestas);
            RecyclerView parent = new RecyclerView(contexto);
            CategoriaPartidaAdapter.ViewHolder holder = adapter.createViewHolder(parent,0);
            adapter.bindViewHolder(holder,0);
            EditText campo = holder.itemView.findViewById(R.id.etRespuestaCategoriaPartida);
            campo.setText("Pollo rancio "); campo.setSelection(3);
            adapter.bindViewHolder(holder,0);
            assertEquals("Pollo rancio ",campo.getText().toString());
            assertEquals("Pollo rancio ",respuestas.get("Animal"));
            assertEquals(3,campo.getSelectionStart());
        });
    }
}
