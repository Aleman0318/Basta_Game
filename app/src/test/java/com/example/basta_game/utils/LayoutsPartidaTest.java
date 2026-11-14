package com.example.basta_game.utils;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.*;
import static org.junit.Assert.*;

public class LayoutsPartidaTest {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private Document layout(String nombre) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        File archivo = new File("src/main/res/layout/" + nombre + ".xml");
        if (!archivo.exists()) archivo = new File("app/src/main/res/layout/" + nombre + ".xml");
        return factory.newDocumentBuilder().parse(archivo);
    }
    private Element porId(Document doc,String id) {
        NodeList nodos=doc.getElementsByTagName("*");
        for (int i=0;i<nodos.getLength();i++) {
            Element e=(Element)nodos.item(i);
            if (("@+id/"+id).equals(e.getAttributeNS(ANDROID,"id"))) return e;
        }
        throw new AssertionError("Falta " + id);
    }
    @Test public void revisionNoExigeScrollHorizontalNiAnchoMinimoFijo() throws Exception {
        Document doc=layout("item_tarjeta_revision");
        assertEquals(0,doc.getElementsByTagName("HorizontalScrollView").getLength());
        Element lista=porId(doc,"rvRespuestasTarjeta");
        assertEquals("match_parent",lista.getAttributeNS(ANDROID,"layout_width"));
        assertEquals("",lista.getAttributeNS(ANDROID,"minWidth"));
        assertTrue(doc.getElementsByTagName("ScrollView").getLength()>0);
    }
    @Test public void votarYPuntosEstanDebajoDePalabraConAnchoDisponible() throws Exception {
        Document doc=layout("item_respuestas");
        Element palabra=porId(doc,"txtRespuestaValidacion");
        Element votar=porId(doc,"btnValidarAccion");
        assertEquals("match_parent",votar.getAttributeNS(ANDROID,"layout_width"));
        assertEquals("48dp",votar.getAttributeNS(ANDROID,"minHeight"));
        assertSame(palabra.getParentNode(),votar.getParentNode().getParentNode());
        assertEquals("vertical",((Element)palabra.getParentNode()).getAttributeNS(ANDROID,"orientation"));
        assertEquals(votar.getParentNode(),porId(doc,"txtPuntuacionValidacion").getParentNode());
    }
    @Test public void listasDeComodinesTienenAltoAcotadoParaPoderDesplazarse() throws Exception {
        for (String[] caso:new String[][]{{"dialogo_comodines_partida","rvComodinesPartida"},
                {"dialogo_resumen_comodines","rvEventosComodines"}}) {
            Element lista=porId(layout(caso[0]),caso[1]);
            assertEquals("0dp",lista.getAttributeNS(ANDROID,"layout_height"));
            assertEquals("1",lista.getAttributeNS(ANDROID,"layout_weight"));
        }
    }
}
