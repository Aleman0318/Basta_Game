package com.example.basta_game.utils;
import java.io.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.*;
import static org.junit.Assert.*;

public class ResponsiveRecursosTest {
    private File res(){File f=new File("src/main/res");return f.exists()?f:new File("app/src/main/res");}
    @Test public void imagenesSinEscaladoDeDensidadYDimensionesAcotadas() throws Exception {
        assertEquals(0,new File(res(),"drawable").listFiles((d,n)->n.endsWith(".png")).length);
        long total=0;int n=0;
        for(File f:new File(res(),"drawable-nodpi").listFiles((d,s)->s.endsWith(".png"))) {
            try(DataInputStream in=new DataInputStream(new FileInputStream(f))) {
                assertEquals(0x89504e470d0a1a0aL,in.readLong());in.readInt();assertEquals(0x49484452,in.readInt());
                int w=in.readInt(),h=in.readInt();assertTrue(f.getName(),w>0 && h>0 && w<=1280 && h<=1280);
                if(f.getName().startsWith("letra_")) assertTrue(f.getName(),w<=384 && h<=384);
                total+=(long)w*h*4;n++;
            }
        }
        assertTrue(n>=100);assertTrue("Pixeles teoricos, no heap real",total<64L*1024*1024);
    }
    @Test public void listasPequenasNoAnidanUnSegundoRecyclerVertical() throws Exception {
        for(String nombre:new String[]{"fragment_crear_sala"}) {
            Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(res(),"layout/"+nombre+".xml"));
            assertEquals(1,doc.getElementsByTagName("com.example.basta_game.utils.ListaContenido").getLength());
            assertEquals(0,doc.getElementsByTagName("androidx.recyclerview.widget.RecyclerView").getLength());
            assertEquals(1,doc.getElementsByTagName("ScrollView").getLength());
        }
    }
    @Test public void categoriaNoImponeAnchoFijoAlGrid() throws Exception {
        Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(res(),"layout/item_categoria.xml"));
        NodeList nodos=doc.getElementsByTagName("*");
        for(int i=0;i<nodos.getLength();i++) assertNotEquals("150dp",((Element)nodos.item(i)).getAttribute("android:layout_width"));
    }
    @Test public void espaciadosDePantallasCriticasTienenFallbackApi25() throws Exception {
        for(String nombre:new String[]{"item_jugador_partida","fragment_partida","item_categoria","item_categoria_partida","item_tarjeta_revision","fragment_crear_sala","item_respuestas","dialogo_resultado_flujo"}) {
            Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(res(),"layout/"+nombre+".xml"));
            NodeList nodos=doc.getElementsByTagName("*");
            for(int i=0;i<nodos.getLength();i++) {
                Element e=(Element)nodos.item(i);
                for(String[] a:new String[][]{{"paddingHorizontal","paddingStart","paddingEnd"},{"paddingVertical","paddingTop","paddingBottom"},{"layout_marginHorizontal","layout_marginStart","layout_marginEnd"},{"layout_marginVertical","layout_marginTop","layout_marginBottom"}})
                    if(e.hasAttribute("android:"+a[0])) {assertTrue(nombre,e.hasAttribute("android:"+a[1]));assertTrue(nombre,e.hasAttribute("android:"+a[2]));}
            }
        }
    }
}
