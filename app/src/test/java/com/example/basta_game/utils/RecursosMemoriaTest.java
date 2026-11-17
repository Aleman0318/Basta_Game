package com.example.basta_game.utils;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecursosMemoriaTest {
    @Test public void bitmapsCriticosSonAcotadosYNodpiSinDecodificarlos() throws Exception {
        File raiz=new File(System.getProperty("user.dir"));
        if(!new File(raiz,"app/src/main/res").exists()) raiz=raiz.getParentFile();
        long bytes=0;
        for(String nombre:new String[]{"ic_notificacion_succes","ic_notificacion_error","ic_notificacion_warning","ic_notificacion_info","bg_fondocuaderno","logo_basta","ic_nombre","ic_mujer","ic_frutas2","ic_paleta2","ic_cosa","ic_animal3","ic_pais2","ic_pelicula","ic_cancion","btn_atras1","ic_crono","ic_siluetas"}) {
            File res=new File(raiz,"app/src/main/res");
            assertFalse(new File(res,"drawable/"+nombre+".png").exists());
            try(DataInputStream in=new DataInputStream(new FileInputStream(new File(res,"drawable-nodpi/"+nombre+".png")))) {
                assertEquals(0x89504e470d0a1a0aL,in.readLong());assertEquals(13,in.readInt());assertEquals(0x49484452,in.readInt());
                int w=in.readInt(),h=in.readInt();assertTrue(w>0 && h>0);
                int max=nombre.startsWith("ic_notificacion") ? 144 : nombre.equals("bg_fondocuaderno") ? 1280 : nombre.equals("logo_basta") ? 960 : nombre.equals("btn_atras1") || nombre.equals("ic_crono") || nombre.equals("ic_siluetas") ? 256 : 192;
                assertTrue(nombre,w<=max && h<=max);bytes+=(long)w*h*4;
            }
        }
        assertTrue("Presupuesto teorico ARGB8888, no medida de heap Android",bytes<9*1024*1024);
    }
}
