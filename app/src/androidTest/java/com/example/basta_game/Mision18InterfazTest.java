package com.example.basta_game;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class Mision18InterfazTest {
    private final Instrumentation i=InstrumentationRegistry.getInstrumentation();
    private final int[] iconos={R.drawable.ic_comodin_decreto_silencio,R.drawable.ic_comodin_espejo,R.drawable.ic_comodin_oraculo_ciego,
            R.drawable.ic_comodin_impuesto,R.drawable.ic_comodin_viento,R.drawable.ic_comodin_escudo_dorado,R.drawable.ic_comodin_chismoso};
    @Test public void chismosoProntoNoCompraAunqueSeFuerceUnaPulsacion() {
        i.runOnMainSync(() -> {
            Context c=new ContextThemeWrapper(i.getTargetContext(),R.style.Theme_Basta_Game);
            androidx.recyclerview.widget.RecyclerView padre=new androidx.recyclerview.widget.RecyclerView(c);
            padre.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(c));
            java.util.concurrent.atomic.AtomicInteger compras=new java.util.concurrent.atomic.AtomicInteger();
            com.example.basta_game.models.ComodinTienda item=new com.example.basta_game.models.ComodinTienda("chismoso_suerte","Chismoso de la Suerte","En preparación","Premium","estrategia",R.drawable.ic_comodin_chismoso,12,"gemas","");
            com.example.basta_game.Adapters.ComodinTiendaAdapter adapter=new com.example.basta_game.Adapters.ComodinTiendaAdapter(java.util.Collections.singletonList(item),x -> compras.incrementAndGet(),() -> false);
            com.example.basta_game.Adapters.ComodinTiendaAdapter.ViewHolder h=adapter.createViewHolder(padre,0);adapter.bindViewHolder(h,0);
            View boton=h.itemView.findViewById(R.id.btnComprarComodinItem);assertFalse(boton.isEnabled());
            assertEquals("PRONTO",((TextView)h.itemView.findViewById(R.id.txtComprarComodin)).getText().toString());
            boton.setEnabled(true);boton.performClick();assertEquals(0,compras.get());
        });
    }
    @Test public void sieteIconos384SinEscaladoGigante() {
        for(int icono:iconos) {
            Bitmap b=BitmapFactory.decodeResource(i.getTargetContext().getResources(),icono);
            assertNotNull(b);assertEquals(384,b.getWidth());assertEquals(384,b.getHeight());assertTrue(b.getAllocationByteCount()<=384*384*4);b.recycle();
        }
        assertEquals(7,new java.util.HashSet<Integer>(){{for(int x:iconos)add(x);}}.size());
    }
    @Test public void cerrarSesionSiempreVisibleEnPerfilEstrecho() {
        i.runOnMainSync(() -> {
            for(int ancho:new int[]{320,360,600})for(float fuente:new float[]{1,1.3f,2}) {
                Configuration cfg=new Configuration(i.getTargetContext().getResources().getConfiguration());cfg.densityDpi=160;cfg.screenWidthDp=ancho;cfg.fontScale=fuente;
                Context c=new ContextThemeWrapper(i.getTargetContext().createConfigurationContext(cfg),R.style.Theme_Basta_Game);
                View perfil=LayoutInflater.from(c).inflate(R.layout.fragment_perfil,null);
                perfil.measure(View.MeasureSpec.makeMeasureSpec(ancho,1073741824),View.MeasureSpec.makeMeasureSpec(480,1073741824));perfil.layout(0,0,ancho,480);
                TextView boton=perfil.findViewById(R.id.btnCerrarSesion);assertEquals(perfil,boton.getParent());
                assertTrue(boton.getTop()>=0);assertTrue(boton.getBottom()<=480);assertTrue(boton.getWidth()>=48);assertTrue(boton.getHeight()>=48);
                assertEquals(1,boton.getLineCount());assertTrue(boton.getLayout().getHeight()<=boton.getHeight()-boton.getCompoundPaddingTop()-boton.getCompoundPaddingBottom());
                assertTrue(boton.getLayout().getLineWidth(0)<=boton.getWidth()-boton.getCompoundPaddingLeft()-boton.getCompoundPaddingRight());
                assertNotNull(perfil.findViewById(R.id.tvPartidasJugadas));assertNotNull(perfil.findViewById(R.id.ivAvatarActual));
            }
        });
    }
    @Test public void capturaDeIconosEnTarjetasPequenas() throws Exception {
        assertEquals("com.example.basta_game.responsive",i.getTargetContext().getPackageName());
        Activity a=i.startActivitySync(new Intent(i.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Dialog[] d={null};
        try {
            i.runOnMainSync(() -> {
                LinearLayout lista=new LinearLayout(a);lista.setOrientation(LinearLayout.VERTICAL);lista.setPadding(24,16,24,16);lista.setBackgroundColor(0xFFFFFDF6);
                String[] nombres={"Decreto del Silencio","El Espejo","Oráculo Ciego","Impuesto Revolucionario","Viento del Caos","Escudo Dorado","Chismoso de la Suerte"};
                float densidad=a.getResources().getDisplayMetrics().density;
                for(int n=0;n<iconos.length;n++) {LinearLayout fila=new LinearLayout(a);fila.setGravity(Gravity.CENTER_VERTICAL);
                    ImageView imagen=new ImageView(a);imagen.setImageResource(iconos[n]);fila.addView(imagen,new LinearLayout.LayoutParams((int)(56*densidad),(int)(56*densidad)));
                    TextView texto=new TextView(a);texto.setText(nombres[n]);texto.setTextSize(16);texto.setTextColor(0xFF14215B);fila.addView(texto);lista.addView(fila);}
                d[0]=new Dialog(a);d[0].setContentView(lista);d[0].show();d[0].getWindow().setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.92),-2);
            });i.waitForIdleSync();Thread.sleep(300);
            Bitmap b=i.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(i.getTargetContext().getExternalFilesDir("responsive"),"m18-iconos.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}
        } finally{i.runOnMainSync(() -> {if(d[0]!=null)d[0].dismiss();a.finish();});}
    }
}
