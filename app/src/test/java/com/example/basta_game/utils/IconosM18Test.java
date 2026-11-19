package com.example.basta_game.utils;
import org.junit.Test;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.Assert.*;

public class IconosM18Test {
    @Test public void tiendaEInventarioCoincidenConElMapeoAprobado() throws Exception {
        String[][] esperados={{"decreto_silencio","decreto_silencio"},{"inversion_letras","espejo"},{"oraculo_ciego","oraculo_ciego"},
                {"impuesto_revol","impuesto"},{"viento_caos","viento"},{"escudo_dorado","escudo_dorado"},{"chismoso_suerte","chismoso"}};
        for(String clase:Arrays.asList("TiendaFragment","PartidaFragment")) {
            String fuente=new String(Files.readAllBytes(Paths.get("src/main/java/com/example/basta_game/Fragments/"+clase+".java")),java.nio.charset.StandardCharsets.UTF_8);
            for(String[] e:esperados) {
                java.util.regex.Matcher m=java.util.regex.Pattern.compile("new Comodin\\w+\\(\\s*\""+e[0]+"\"[\\s\\S]*?\\)\\)").matcher(fuente);
                assertTrue(clase+" "+e[0],m.find());assertTrue(clase+" "+e[0],m.group().contains("R.drawable.ic_comodin_"+e[1]+","));
            }
        }
    }
    @Test public void sietePngDistintos384SinOriginalesEmpaquetados() throws Exception {
        Set<String> hashes=new HashSet<>();
        for(String nombre:Arrays.asList("decreto_silencio","espejo","oraculo_ciego","impuesto","viento","escudo_dorado","chismoso")) {
            byte[] b=Files.readAllBytes(Paths.get("src/main/res/drawable-nodpi/ic_comodin_"+nombre+".png"));
            assertEquals(384,ByteBuffer.wrap(b,16,4).getInt());assertEquals(384,ByteBuffer.wrap(b,20,4).getInt());
            hashes.add(Arrays.toString(MessageDigest.getInstance("SHA-256").digest(b)));
        }
        assertEquals(7,hashes.size());
        try(java.util.stream.Stream<Path> recursos=Files.walk(Paths.get("src/main/res"))) {
            assertFalse(recursos.anyMatch(p -> p.toString().contains("originales_1254px")));
        }
    }
}
