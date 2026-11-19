package com.example.basta_game;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.basta_game.Adapters.ComodinPartidaAdapter;
import com.example.basta_game.Fragments.*;
import com.example.basta_game.models.*;
import com.example.basta_game.utils.*;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Native views in the Internet-free .responsive APK; no purchases or real accounts. */
public class Mision17InterfazTest {
    private final Instrumentation i=InstrumentationRegistry.getInstrumentation();
    private Context contexto(int ancho,float fuente) {
        Context c=i.getTargetContext();Configuration cfg=new Configuration(c.getResources().getConfiguration());
        cfg.densityDpi=160;cfg.screenWidthDp=ancho;cfg.screenHeightDp=700;cfg.smallestScreenWidthDp=ancho;cfg.fontScale=fuente;
        return new ContextThemeWrapper(c.createConfigurationContext(cfg),R.style.Theme_Basta_Game);
    }
    private void medir(View v,int ancho,int alto) {
        v.measure(View.MeasureSpec.makeMeasureSpec(ancho,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(alto,View.MeasureSpec.EXACTLY));
        v.layout(0,0,ancho,alto);
    }
    @Test public void ajustesPequenosConservanLimitesYAbcLegible() {
        i.runOnMainSync(() -> {
            for(int w:new int[]{320,360,600})for(float f:new float[]{1f,1.3f,2f}) {
                Context c=contexto(w,f);View revision=LayoutInflater.from(c).inflate(R.layout.item_tarjeta_revision,null);medir(revision,w,600);
                TextView abc=revision.findViewById(R.id.txtInsigniaAbc);View pill=revision.findViewById(R.id.cardInsigniaAbc);
                assertEquals("ABC",abc.getText().toString());assertTrue(pill.getWidth()>pill.getHeight());
                assertTrue(abc.getWidth()-abc.getCompoundPaddingLeft()-abc.getCompoundPaddingRight()>=abc.getPaint().measureText("ABC"));
                assertEquals(1,abc.getLineCount());assertEquals(0,abc.getLayout().getEllipsisCount(0));
                assertTrue(abc.getLayout().getHeight()<=abc.getHeight()-abc.getCompoundPaddingTop()-abc.getCompoundPaddingBottom());
                View carril=LayoutInflater.from(c).inflate(R.layout.item_carril_tienda,null);medir(carril,w,700);
                View subtitulo=carril.findViewById(R.id.txtSubtituloCategoria);assertEquals(16,((ViewGroup.MarginLayoutParams)subtitulo.getLayoutParams()).bottomMargin);
                assertEquals(70,((View)subtitulo.getParent()).getHeight());
                View partida=LayoutInflater.from(c).inflate(R.layout.fragment_partida,null);MaterialButton boton=partida.findViewById(R.id.btnComodines);
                assertEquals(22,boton.getIconSize());assertEquals(112,boton.getLayoutParams().width);assertEquals(48,boton.getLayoutParams().height);assertEquals("Comodines",boton.getText().toString());
            }
        });
    }
    @Test public void inventarioMuestraTodosLosTiposVendidosYProntoNoActiva() {
        i.runOnMainSync(() -> {
            try {
                TiendaFragment tienda=new TiendaFragment();java.lang.reflect.Method preparar=TiendaFragment.class.getDeclaredMethod("prepararCatalogoComodines");preparar.setAccessible(true);preparar.invoke(tienda);
                java.lang.reflect.Field campo=TiendaFragment.class.getDeclaredField("listaCategorias");campo.setAccessible(true);
                List<ComodinTienda> productos=new ArrayList<>();for(CategoriaTienda c:(List<CategoriaTienda>)campo.get(tienda))productos.addAll(c.getComodines());
                assertEquals(12,productos.size());
                java.lang.reflect.Method catalogo=PartidaFragment.class.getDeclaredMethod("obtenerCatalogoComodinesPartida");catalogo.setAccessible(true);
                Map<String,Object> inventario=new LinkedHashMap<>();for(int n=0;n<productos.size();n++)inventario.put(productos.get(n).getId(),Collections.singletonMap("cantidad",n+1));
                List<ComodinPartida> items=InventarioComodines.listar(inventario,(List<ComodinPartida>)catalogo.invoke(new PartidaFragment()),R.drawable.ic_comodines);
                assertEquals(productos.size(),items.size());Map<String,ComodinPartida> porId=new HashMap<>();for(ComodinPartida item:items)porId.put(item.getId(),item);
                Context c=contexto(320,1.3f);RecyclerView padre=new RecyclerView(c);padre.setLayoutManager(new LinearLayoutManager(c));
                AtomicInteger acciones=new AtomicInteger();ComodinPartidaAdapter adapter=new ComodinPartidaAdapter(items,x -> acciones.incrementAndGet(),x -> true);
                int pendientes=0;
                for(int n=0;n<productos.size();n++) {
                    ComodinTienda producto=productos.get(n);ComodinPartida item=porId.get(AliasCambioFlujo.canonico(producto.getId()));
                    assertNotNull(producto.getId(),item);assertEquals(n+1,item.getCantidad());assertEquals(producto.getIconoResId(),item.getIconoResId());assertEquals(producto.getNombre(),item.getNombre());
                }
                for(int n=0;n<items.size();n++) {
                    ComodinPartidaAdapter.ViewHolder h=adapter.createViewHolder(padre,0);adapter.bindViewHolder(h,n);
                    if(!items.get(n).isImplementado()) {pendientes++;TextView usar=h.itemView.findViewById(R.id.btnUsarComodinPartidaItem);assertEquals("PRONTO",usar.getText().toString());assertFalse(usar.isEnabled());usar.performClick();}
                }
                assertEquals(7,pendientes);assertEquals(0,acciones.get());
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
    }
    @Test public void vibracionSeRecuerdaTrasRecrearContextoYNoRequiereHardware() {
        i.runOnMainSync(() -> {
            Context c=new ContextWrapper(i.getTargetContext()) {
                @Override public Object getSystemService(String nombre) {return Context.VIBRATOR_SERVICE.equals(nombre) ? null : super.getSystemService(nombre);}
            };
            String partida="m17-"+UUID.randomUUID();
            assertFalse(VibracionRonda.avisar(c,"local",partida,1,10001));
            assertTrue(VibracionRonda.avisar(c,"local",partida,1,10000));
            assertFalse(VibracionRonda.avisar(c,"local",partida,1,9000));
            Context recreado=new ContextWrapper(i.getTargetContext());
            assertFalse(VibracionRonda.avisar(recreado,"local",partida,1,8000));
            assertTrue(VibracionRonda.avisar(c,"local",partida,2,10000));
            assertFalse(VibracionRonda.avisar(c,"local",partida,2,0));
        });
    }
    private Object privado(String nombre) {
        try {java.lang.reflect.Field f=NotificationHelper.class.getDeclaredField(nombre);f.setAccessible(true);return f.get(null);}catch(Exception e){throw new AssertionError(e);}
    }
    @Test public void notificacionArribaDelDialogoSinRobarFocoNiPulsaciones() throws Exception {
        assertEquals("com.example.basta_game.responsive",i.getTargetContext().getPackageName());
        i.runOnMainSync(() -> NotificationHelper.inicializar((Application)i.getTargetContext().getApplicationContext()));
        Activity a=i.startActivitySync(new Intent(i.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Dialog[] compra={null};AtomicInteger compras=new AtomicInteger();
        try {
            i.runOnMainSync(() -> {
                compra[0]=CustomDialogHelper.show(a,"Confirmar compra","¿Quieres comprar Congelador por 250 monedas?",false,
                    new CustomDialogHelper.DialogButton("Cancelar",Color.GRAY,Color.WHITE,true,null),
                    new CustomDialogHelper.DialogButton("Comprar",Color.BLUE,Color.WHITE,true,compras::incrementAndGet));
                NotificationHelper.showGlobal("Aviso local de compra",NotificationHelper.SUCCESS);
            });
            Thread.sleep(400);i.waitForIdleSync();
            i.runOnMainSync(() -> {
                Dialog ventana=(Dialog)privado("ventanaNotificacion");View card=(View)privado("card");assertNotNull(ventana);assertTrue(ventana.isShowing());
                int flags=ventana.getWindow().getAttributes().flags;
                assertTrue((flags&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0);assertTrue((flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0);
                assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION,ventana.getWindow().getAttributes().type);
                Rect aviso=new Rect(),mensaje=new Rect();assertTrue(card.getGlobalVisibleRect(aviso));assertTrue(compra[0].findViewById(R.id.txtDialogMessage).getGlobalVisibleRect(mensaje));
                int[] p=new int[2],q=new int[2];card.getLocationOnScreen(p);compra[0].findViewById(R.id.txtDialogMessage).getLocationOnScreen(q);
                androidx.core.view.WindowInsetsCompat insets=androidx.core.view.ViewCompat.getRootWindowInsets(a.getWindow().getDecorView());
                assertTrue(p[1]>=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top);
                assertTrue("Aviso no tapa mensaje de compra",p[1]+card.getHeight()<q[1]);
                assertTrue("Dialogo de compra conserva foco",compra[0].getWindow().getDecorView().hasWindowFocus());
            });
            Bitmap captura=i.getUiAutomation().takeScreenshot();
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(i.getTargetContext().getExternalFilesDir("responsive"),"m17-notificacion-dialogo.png"))){captura.compress(Bitmap.CompressFormat.PNG,100,out);}finally{captura.recycle();}
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Comprar")).inRoot(androidx.test.espresso.matcher.RootMatchers.withDecorView(org.hamcrest.Matchers.is(compra[0].getWindow().getDecorView()))).perform(androidx.test.espresso.action.ViewActions.click());
            assertEquals(1,compras.get());
            Thread.sleep(3100);i.waitForIdleSync();i.runOnMainSync(() -> assertNull(privado("ventanaNotificacion")));
        } finally {i.runOnMainSync(() -> {if(compra[0]!=null)compra[0].dismiss();a.finish();});}
        i.waitForIdleSync();
    }
}
