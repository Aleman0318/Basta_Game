package com.example.basta_game;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.basta_game.Adapters.*;
import com.example.basta_game.models.*;
import com.example.basta_game.utils.ListaContenido;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ResponsiveLayoutsTest {
    @org.junit.Before public void soloPerfilAislado(){org.junit.Assume.assumeTrue("Usar -PlayoutsAislados",InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName().equals("com.example.basta_game.responsive"));}
    private Context contexto(int w,int h,float fuente) {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("Only the isolated, Internet-free application is allowed","com.example.basta_game.responsive",c.getPackageName());
        Configuration cfg=new Configuration(c.getResources().getConfiguration());cfg.densityDpi=160;cfg.screenWidthDp=w;cfg.screenHeightDp=h;cfg.smallestScreenWidthDp=Math.min(w,h);cfg.fontScale=fuente;
        return new android.view.ContextThemeWrapper(c.createConfigurationContext(cfg),R.style.Theme_Basta_Game);
    }
    private void medir(View v,int w,int h){v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);}
    private void guardar(View v,String nombre) {
        File carpeta=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("responsive");
        Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);
        try(FileOutputStream out=new FileOutputStream(new File(carpeta,nombre+".png"))) {Canvas c=new Canvas(b);c.drawColor(Color.WHITE);v.draw(c);b.compress(Bitmap.CompressFormat.PNG,100,out);}catch(IOException e){throw new AssertionError(e);}finally{b.recycle();}
    }
    private ScrollView scroll(View v){if(v instanceof ScrollView)return (ScrollView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){ScrollView s=scroll(((ViewGroup)v).getChildAt(i));if(s!=null)return s;}return null;}
    private List<Categoria> categorias(){List<Categoria> l=new ArrayList<>();for(String n:new String[]{"Nombre","Apellido","Fruta","Color","Cosa","Animal","País","Película","Canción"})l.add(new Categoria(n,R.drawable.ic_nombre));return l;}

    @Test public void revisionListaAcotadaDosCincoYOchoSinMoverEncabezado() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for(int cantidad:new int[]{2,5,8})for(int w:new int[]{320,600})for(float fuente:new float[]{1f,1.3f}) {
                int h=w==320?488:820;Context c=contexto(w,h,fuente);
                View v=LayoutInflater.from(c).inflate(R.layout.item_tarjeta_revision,null);
                TextView titulo=v.findViewById(R.id.txtNombreCategoriaTarjeta);titulo.setText("Película / Canción");
                RecyclerView lista=v.findViewById(R.id.rvRespuestasTarjeta);lista.setLayoutManager(new LinearLayoutManager(c));lista.setItemAnimator(null);
                List<RespuestaValidacion> datos=new ArrayList<>();
                for(int i=0;i<cantidad;i++){RespuestaValidacion r=new RespuestaValidacion("demo"+i,"Jugador "+(i+1),"Océanos "+(i+1),null);r.setValidada(true);r.setPuntos(10);datos.add(r);}
                AtomicInteger clicks=new AtomicInteger();ValidacionAdapter a=new ValidacionAdapter(datos,r -> clicks.incrementAndGet());a.revelarTodos();lista.setAdapter(a);
                for(int fase=0;fase<3;fase++){
                    if(fase==2)for(RespuestaValidacion r:datos){r.setVotacionResuelta(true);r.setMostrarPuntaje(true);}
                    a.estadoCompartido(fase==1,fase==2);a.actualizarValidaciones();medir(v,w,h);
                    assertNull(scroll(v));assertTrue(lista.getHeight()>80);int y=titulo.getTop();
                    lista.scrollToPosition(cantidad-1);medir(v,w,h);
                    RecyclerView.ViewHolder ultimo=lista.findViewHolderForAdapterPosition(cantidad-1);assertNotNull(ultimo);
                    assertFalse(((TextView)ultimo.itemView.findViewById(R.id.txtRespuestaValidacion)).getText().toString().isEmpty());
                    assertEquals(y,titulo.getTop());assertTrue(lista.getBottom()<v.getHeight());
                    if(fase==1){assertTrue(ultimo.itemView.findViewById(R.id.btnValidarAccion).performClick());assertEquals(1,clicks.get());}
                    if(fase==2)assertEquals(View.VISIBLE,ultimo.itemView.findViewById(R.id.txtPuntuacionValidacion).getVisibility());
                    if(cantidad==2 && w==600)assertFalse(lista.canScrollVertically(-1)||lista.canScrollVertically(1));
                    guardar(v,"m12-revision-"+cantidad+"-"+w+"-"+fuente+"-fase"+fase);
                }
                lista.setAdapter(null);
            }
        });
    }
    @Test public void cargaBotonDescartaFinalizacionViejaYConservaControlDeDoblePulsacion(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TextView b=new androidx.appcompat.widget.AppCompatButton(contexto(320,568,1));b.setText("Comprar");
            assertTrue(com.example.basta_game.utils.CargaBoton.iniciar(b));Object uno=com.example.basta_game.utils.CargaBoton.token(b);
            assertFalse(b.isEnabled());assertNotNull(b.getForeground());assertFalse(com.example.basta_game.utils.CargaBoton.iniciar(b));
            assertTrue(com.example.basta_game.utils.CargaBoton.terminar(b,uno));assertTrue(b.isEnabled());assertNull(b.getForeground());
            assertTrue(com.example.basta_game.utils.CargaBoton.iniciar(b));Object dos=com.example.basta_game.utils.CargaBoton.token(b);
            assertFalse(com.example.basta_game.utils.CargaBoton.terminar(b,uno));assertFalse(b.isEnabled());assertTrue(com.example.basta_game.utils.CargaBoton.terminar(b,dos));
        });
    }
    @Test public void tiendaM14DescripcionCompletaYCompraVisible(){
      InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
        for(int w:new int[]{320,360,600})for(float f:new float[]{1f,1.3f}){
          Context c=contexto(w,900,f);android.widget.FrameLayout padre=new android.widget.FrameLayout(c);medir(padre,w,900);
          ComodinTienda dato=new ComodinTienda("demo","Marca de la Muerte legendaria","Reta a un jugador a un duelo. Si ganas, recibes los puntos indicados; si pierdes, se aplica la penalización descrita en sus estadísticas.","control","efecto_global",R.drawable.ic_comodin_dobleonada,12345,"gemas","Datos locales");
          ComodinTiendaAdapter a=new ComodinTiendaAdapter(Arrays.asList(dato,dato,dato),v->{},()->false);a.actualizarInventario(Collections.singletonMap("demo",12345));
          RecyclerView carril=new RecyclerView(c);carril.setLayoutManager(new LinearLayoutManager(c,RecyclerView.HORIZONTAL,false));carril.setAdapter(a);padre.addView(carril,new android.widget.FrameLayout.LayoutParams(-1,-2));medir(padre,w,900);View v=carril.findViewHolderForAdapterPosition(0).itemView;assertNotNull(carril.findViewHolderForAdapterPosition(1));assertTrue(carril.canScrollHorizontally(1));
          for(int id:new int[]{R.id.txtNombreComodinItem,R.id.txtDescripcionComodinItem}){TextView t=v.findViewById(id);assertEquals(t.getText().length(),t.getLayout().getLineEnd(t.getLineCount()-1));for(int j=0;j<t.getLineCount();j++)assertEquals(0,t.getLayout().getEllipsisCount(j));}
          View compra=v.findViewById(R.id.btnComprarComodinItem);android.graphics.Rect r=new android.graphics.Rect();compra.getDrawingRect(r);((ViewGroup)v).offsetDescendantRectToMyCoords(compra,r);assertTrue(r.right<=v.getWidth());assertTrue(r.bottom<=v.getHeight());assertTrue(compra.getHeight()>=48);
          TextView comprar=v.findViewById(R.id.txtComprarComodin);assertEquals(1,comprar.getLineCount());assertTrue(comprar.getWidth()>=comprar.getPaint().measureText(comprar.getText().toString()));
          TextView inventario=v.findViewById(R.id.txtCantidadInventarioItem);assertTrue(inventario.getWidth()>=inventario.getPaint().measureText(inventario.getText().toString()));
          guardar(padre,"m14-tienda-"+w+"-"+f);
        }
      });
    }
    @Test public void unirseM14BotonAlcanzableConAlturaReducida(){
      InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
        for(int w:new int[]{320,360,600})for(float f:new float[]{1f,1.3f}){
          Context c=contexto(w,568,f);FrameLayout ventana=new FrameLayout(c);View v=LayoutInflater.from(c).inflate(R.layout.fragment_unirse_sala,ventana,false);ventana.addView(v,new FrameLayout.LayoutParams(-1,-1));medir(ventana,w,300);ScrollView sc=(ScrollView)v;
          TextView codigo=v.findViewById(R.id.etSalaId);codigo.setText("AB1234");medir(ventana,w,300);assertTrue(codigo.getLayout().getLineBottom(0)<=codigo.getHeight()-codigo.getCompoundPaddingTop()-codigo.getCompoundPaddingBottom());View boton=v.findViewById(R.id.btnUnirseSalaFinal);sc.scrollTo(0,sc.getChildAt(0).getHeight());assertTrue("Scroll real al boton",sc.getScrollY()>0);dato("m14-unirse-medidas-"+w+"-"+f,"scroll="+sc.getScrollY()+"; child="+sc.getChildAt(0).getHeight()+"; ventana="+sc.getHeight());android.graphics.Rect r=new android.graphics.Rect();boton.getDrawingRect(r);ventana.offsetDescendantRectToMyCoords(boton,r);assertTrue(r.top>=0);assertTrue(r.bottom<=ventana.getHeight());assertTrue(boton.getHeight()>=48);guardar(ventana,"m14-unirse-"+w+"-"+f);
        }
      });
    }
    @Test public void crearSalaNueveCategoriasYBotonFinalSonAlcanzables() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for(int w:new int[]{320,360,600}) for(float fuente:new float[]{1f,1.3f}) {
                int h=w==600?900:568;Context c=contexto(w,h,fuente);View v=LayoutInflater.from(c).inflate(R.layout.fragment_crear_sala,null);
                ListaContenido lista=v.findViewById(R.id.rvCategorias);lista.setColumnas(w<360?1:2);
                ((TextView)v.findViewById(R.id.tvCantRondas)).setText("27");((TextView)v.findViewById(R.id.tvCantJugadores)).setText("8");
                List<Categoria> datos=categorias();AtomicInteger clicks=new AtomicInteger();lista.setAdapter(new CategoriaAdapter(datos,(cat,activa) -> clicks.incrementAndGet()));
                medir(v,w,h);int total=0;for(int i=0;i<lista.getChildCount();i++)total+=((ViewGroup)lista.getChildAt(i)).getChildCount();assertEquals(9,total);
                for(int i=0;i<lista.getChildCount();i++)for(int j=0;j<((ViewGroup)lista.getChildAt(i)).getChildCount();j++){TextView n=((ViewGroup)lista.getChildAt(i)).getChildAt(j).findViewById(R.id.tvNombreCategoria);assertEquals(n.getText().toString(),0,n.getLayout().getEllipsisCount(0));}
                                TextView numero=v.findViewById(R.id.tvCantRondas);
                dato("contador-"+w,"ancho="+numero.getWidth()+"; padding="+numero.getCompoundPaddingLeft()+","+numero.getCompoundPaddingRight()+"; texto="+numero.getText()+"; anchoTexto="+numero.getPaint().measureText(numero.getText().toString())+"; alto="+numero.getHeight());
                                for(String valor:new String[]{"1","8","27"}){numero.setText(valor);medir(v,w,h);assertEquals(1,numero.getLineCount());assertTrue(numero.getWidth()>=numero.getPaint().measureText(valor));}numero.setText("27");medir(v,w,h);
                assertEquals(1,numero.getLineCount());assertTrue("Numero completo, ancho="+numero.getWidth(),numero.getWidth()-numero.getCompoundPaddingLeft()-numero.getCompoundPaddingRight()>=numero.getPaint().measureText(numero.getText().toString()));
                ViewGroup ultima=(ViewGroup)lista.getChildAt(lista.getChildCount()-1);assertTrue(ultima.getChildAt(0).getHeight()>48);ultima.getChildAt(0).performClick();assertEquals(1,clicks.get());
                guardar(v,"crear-"+w+"-inicio");ScrollView s=scroll(v);s.scrollTo(0,s.getChildAt(0).getHeight());assertTrue(s.getScrollY()>0);guardar(v,"crear-"+w+"-final");lista.setAdapter(null);
            }
        });
    }
    @Test public void partidaCompactaConservaAreaEditableYScrollInterno() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for(int w:new int[]{320,360,600}) {
                int h=w==600?900:568;View v=LayoutInflater.from(contexto(w,h,1.3f)).inflate(R.layout.fragment_partida,null);
                RecyclerView jugadores=v.findViewById(R.id.rvJugadoresPartida);jugadores.setLayoutManager(new LinearLayoutManager(v.getContext(),RecyclerView.HORIZONTAL,false));
                jugadores.setAdapter(new JugadorPartidaAdapter(Arrays.asList(new Jugador("Marlon",false,null,true),new Jugador("Diana",false,null,false))));
                RecyclerView hoja=v.findViewById(R.id.rvCategoriasJuego);CategoriaPartidaAdapter.configurarScrollInterno(hoja);hoja.setAdapter(new CategoriaPartidaAdapter(categorias(),new HashMap<>()));
                                medir(v,w,h);
                for(int id:new int[]{R.id.txtCodigoSala,R.id.txtRondaActual,R.id.txtEstadoPartida}){TextView chip=v.findViewById(id);assertTrue("Texto completo en chip",chip.getLayout().getLineBottom(chip.getLineCount()-1)<=chip.getHeight()-chip.getCompoundPaddingTop()-chip.getCompoundPaddingBottom());}
                guardar(v,"partida-"+w+"-medicion");dato("partida-"+w+"-medidas","hoja="+hoja.getHeight()+"px; pantalla="+w+"x"+h+"; densidad=160; fuente=1.0");assertTrue("Hoja util, alto="+hoja.getHeight(),hoja.getHeight()>=100);assertEquals(View.VISIBLE,v.findViewById(R.id.btnBasta).getVisibility());
                                TextView basta=v.findViewById(R.id.btnBasta);
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(basta,12,26,1,android.util.TypedValue.COMPLEX_UNIT_SP);
                for(String estado:new String[]{"¡BASTA!","CONGELADO","FINALIZANDO"}){basta.setText(estado);medir(v,w,h);assertEquals(estado.length(),basta.getLayout().getLineEnd(basta.getLineCount()-1));for(int linea=0;linea<basta.getLineCount();linea++)assertEquals(0,basta.getLayout().getEllipsisCount(linea));guardar(v,"m12-basta-"+w+"-"+estado);}
                guardar(v,"partida-"+w+"-inicio");hoja.scrollBy(0,10000);assertEquals(8,((LinearLayoutManager)hoja.getLayoutManager()).findLastVisibleItemPosition());guardar(v,"partida-"+w+"-ultima");
                hoja.setAdapter(null);jugadores.setAdapter(null);
            }
        });
    }
    @Test public void tituloRuletaCompletoYDecodificacionCriticaAcotada() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context c=contexto(320,568,1.3f);View v=LayoutInflater.from(c).inflate(R.layout.dialogo_resultado_flujo,null);
            v.findViewById(R.id.lottieConfeti).setVisibility(View.GONE);v.findViewById(R.id.btnCerrarResultado).setVisibility(View.GONE);
            TextView t=v.findViewById(R.id.txtTituloResultado);t.setTextSize(20);t.setSingleLine(false);t.setEllipsize(null);t.setText("¡RULETA CAÓTICA!\n5");
            ((ImageView)v.findViewById(R.id.imgIconoResultado)).setImageResource(R.drawable.ic_comodin_ruletacaotica);
            ((TextView)v.findViewById(R.id.txtMensajeResultado)).setText("Eligiendo un efecto al azar...");v.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(568,View.MeasureSpec.AT_MOST));v.layout(0,0,320,v.getMeasuredHeight());
            assertEquals(t.getText().length(),t.getLayout().getLineEnd(t.getLayout().getLineCount()-1));for(int i=0;i<t.getLineCount();i++)assertEquals(0,t.getLayout().getEllipsisCount(i));guardar(v,"ruleta-320");
            long bytes=0;List<Bitmap> bitmaps=new ArrayList<>();try {
                for(int id:new int[]{R.drawable.bg_fondocuaderno,R.drawable.logo_basta,R.drawable.letra_m,R.drawable.marco_rojo,R.drawable.ic_corona_host,R.drawable.ic_comodines,R.drawable.ic_crono}){Bitmap b=BitmapFactory.decodeResource(c.getResources(),id);assertNotNull(b);bitmaps.add(b);bytes+=b.getAllocationByteCount();}
                assertTrue("Memoria bitmap real de estos recursos: "+bytes,bytes<10L*1024*1024);
                dato("memoria-escena","bytesBitmap="+bytes+"; API="+android.os.Build.VERSION.SDK_INT+"; heapMax="+Runtime.getRuntime().maxMemory());
            } finally {for(Bitmap b:bitmaps)b.recycle();}
        });
    }
    private void dato(String nombre,String valor){try(FileOutputStream o=new FileOutputStream(new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("responsive"),nombre+".txt"))){o.write(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(IOException e){throw new AssertionError(e);}}
}
