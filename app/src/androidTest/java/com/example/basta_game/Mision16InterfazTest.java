package com.example.basta_game;

import android.content.*;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.basta_game.Adapters.ComodinTiendaAdapter;
import com.example.basta_game.models.ComodinTienda;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Isolated native views; never buys, joins or contacts production. */
public class Mision16InterfazTest {
    private Context contexto(int ancho,float fuente) {
        Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration c=new Configuration(base.getResources().getConfiguration());
        c.densityDpi=160;c.screenWidthDp=ancho;c.screenHeightDp=900;c.smallestScreenWidthDp=ancho;c.fontScale=fuente;
        return new ContextThemeWrapper(base.createConfigurationContext(c),R.style.Theme_Basta_Game);
    }
    @Test public void tarjetasUniformesConTextoCompletoYCompraAlineada() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for(int ancho:new int[]{320,360,600}) for(float fuente:new float[]{1f,1.3f}) {
                Context c=contexto(ancho,fuente);RecyclerView padre=new RecyclerView(c);
                padre.setLayoutManager(new LinearLayoutManager(c,RecyclerView.HORIZONTAL,false));
                padre.measure(View.MeasureSpec.makeMeasureSpec(ancho,1073741824),View.MeasureSpec.makeMeasureSpec(900,1073741824));padre.layout(0,0,ancho,900);
                List<ComodinTienda> datos=Arrays.asList(
                    new ComodinTienda("a","Congelador","Bloquea BASTA por unos segundos.","control","ataque",R.drawable.ic_comodin_congelar,12345,"monedas",""),
                    new ComodinTienda("b","Marca de la Muerte legendaria","Reta a otro jugador a un duelo. El resultado aplica los puntos reales de la partida. Esta descripcion larga tambien debe verse completa con fuentes aumentadas.","control","ataque",R.drawable.ic_comodin_marcadelamuerte,12345,"monedas",""));
                ComodinTiendaAdapter a=new ComodinTiendaAdapter(datos,x->{},()->false);
                a.actualizarInventario(new HashMap<String,Integer>(){{put("a",12345);put("b",12345);}});
                int alto=-1,compraY=-1;
                for(int n=0;n<datos.size();n++) {
                    ComodinTiendaAdapter.ViewHolder h=a.createViewHolder(padre,0);a.bindViewHolder(h,n);View v=h.itemView;
                    v.measure(View.MeasureSpec.makeMeasureSpec(v.getLayoutParams().width,1073741824),View.MeasureSpec.makeMeasureSpec(v.getLayoutParams().height,1073741824));v.layout(0,0,v.getMeasuredWidth(),v.getMeasuredHeight());
                    if(alto<0)alto=v.getHeight();assertEquals(alto,v.getHeight());
                    TextView descripcion=v.findViewById(R.id.txtDescripcionComodinItem);assertEquals(3,descripcion.getMinLines());
                    assertEquals(descripcion.getText().length(),descripcion.getLayout().getLineEnd(descripcion.getLineCount()-1));
                    assertTrue(descripcion.getLayout().getHeight()<=descripcion.getHeight()-descripcion.getCompoundPaddingTop()-descripcion.getCompoundPaddingBottom());
                    View compra=v.findViewById(R.id.btnComprarComodinItem);android.graphics.Rect r=new android.graphics.Rect();compra.getDrawingRect(r);((ViewGroup)v).offsetDescendantRectToMyCoords(compra,r);
                    assertTrue(r.bottom<=v.getHeight());assertTrue(r.right<=v.getWidth());assertTrue(compra.getHeight()>=48);
                    if(compraY<0)compraY=r.top;assertEquals(compraY,r.top);
                    try(java.io.FileOutputStream salida=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir("responsive"),"m16-tienda-"+ancho+"-"+fuente+"-"+n+".png"))) {
                        android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(v.getWidth(),v.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                        v.draw(new android.graphics.Canvas(b));b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,salida);b.recycle();
                    } catch(java.io.IOException e){throw new AssertionError(e);}
                }
                ComodinTiendaAdapter.ViewHolder reciclada=a.createViewHolder(padre,0);a.bindViewHolder(reciclada,0);
                a.setListaComodines(Collections.singletonList(datos.get(0)));a.bindViewHolder(reciclada,0);
                assertTrue(reciclada.itemView.getLayoutParams().height<=alto);
                a.setListaComodines(datos);a.bindViewHolder(reciclada,0);
                assertEquals("Altura recalculada al reciclar categorias",alto,reciclada.itemView.getLayoutParams().height);
            }
        });
    }
    @Test public void iconoComodinesAumentaSinCambiarCapsula() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c=contexto(320,1f);View v=LayoutInflater.from(c).inflate(R.layout.fragment_partida,null);
            MaterialButton b=v.findViewById(R.id.btnComodines);assertEquals(20,b.getIconSize());
            assertEquals(48,b.getLayoutParams().height);assertEquals("Comodines",b.getText().toString());
        });
    }
    @Test public void avisoTiendaSolicitaAudioOriginalSinRepetirRafaga() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.media.AudioManager audio=(android.media.AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        org.junit.Assume.assumeTrue("No se altera el modo silencio ni el volumen",audio.getRingerMode()==android.media.AudioManager.RINGER_MODE_NORMAL && audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)>0);
        java.util.concurrent.CountDownLatch cargado=new java.util.concurrent.CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            com.example.basta_game.utils.Sounds.init(c);
            try {
                java.lang.reflect.Field f=com.example.basta_game.utils.Sounds.class.getDeclaredField("soundPool");f.setAccessible(true);
                ((android.media.SoundPool)f.get(null)).setOnLoadCompleteListener((pool,id,estado) -> {
                    if(id==com.example.basta_game.utils.Sounds.notificacionesSound && estado==0)cargado.countDown();
                });
            } catch(Exception e){throw new AssertionError(e);}
        });
        try {
            assertTrue("Audio original decodificado",cargado.await(5,java.util.concurrent.TimeUnit.SECONDS));
            com.example.basta_game.utils.NotificationHelper.avisoTienda("Aviso de tienda local",com.example.basta_game.utils.NotificationHelper.WARNING);
            long fin=android.os.SystemClock.elapsedRealtime()+2000;boolean activo=false;
            while(android.os.SystemClock.elapsedRealtime()<fin){if(audio.isMusicActive()){activo=true;break;}Thread.sleep(20);}
            assertTrue("AudioTrack activo tras el aviso de tienda",activo);
            java.lang.reflect.Field f=com.example.basta_game.utils.NotificationHelper.class.getDeclaredField("ultimoSonidoTienda");f.setAccessible(true);
            long primero=f.getLong(null);
            com.example.basta_game.utils.NotificationHelper.avisoTienda("Otro aviso de tienda local",com.example.basta_game.utils.NotificationHelper.WARNING);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();assertEquals(primero,f.getLong(null));
        } finally {InstrumentationRegistry.getInstrumentation().runOnMainSync(com.example.basta_game.utils.Sounds::release);}
    }
}
