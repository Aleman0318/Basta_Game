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
