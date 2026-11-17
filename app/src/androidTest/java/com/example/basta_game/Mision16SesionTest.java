package com.example.basta_game;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.basta_game.Fragments.PartidaFragment;
import com.example.basta_game.utils.*;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

/** AuthRunner + demo-basta-auth only. Local fixture rules are not production rules. */
public class Mision16SesionTest {
    private final Instrumentation i=InstrumentationRegistry.getInstrumentation();
    private void esperar(java.util.function.BooleanSupplier condicion)throws Exception {
        long fin=System.currentTimeMillis()+20000;
        while(!condicion.getAsBoolean() && System.currentTimeMillis()<fin)Thread.sleep(100);
        assertTrue("No se recupero la fase/salida",condicion.getAsBoolean());
    }
    private boolean revision(SalaActivity a) {
        java.util.concurrent.atomic.AtomicBoolean visible=new java.util.concurrent.atomic.AtomicBoolean();
        i.runOnMainSync(() -> {
            androidx.fragment.app.Fragment f=a.getSupportFragmentManager().findFragmentById(R.id.sala_fragment_container);
            if(f instanceof PartidaFragment)try {
                java.lang.reflect.Field campo=PartidaFragment.class.getDeclaredField("dialogValidacionActual");campo.setAccessible(true);
                Dialog dialog=(Dialog)campo.get(f);visible.set(dialog!=null && dialog.isShowing());
            }catch(Exception e){throw new AssertionError(e);}
        });return visible.get();
    }
    private Object campo(Object objeto,String nombre)throws Exception {
        java.lang.reflect.Field f=objeto.getClass().getDeclaredField(nombre);f.setAccessible(true);return f.get(objeto);
    }
    private void transporte(FirebaseDatabase db)throws Exception {
        Object repo=campo(db,"repo"),conexion=campo(repo,"connection"),info=campo(db,"repoInfo");
        System.out.println("M16 transporte: host="+campo(info,"host")+", interno="+campo(info,"internalHost")+", cache="+campo(conexion,"cachedHost")+", estado="+campo(conexion,"connectionState")+", interrupciones="+campo(conexion,"interruptReasons"));
    }
    @Test public void revisionSeRecuperaTrasRecreacionYSalidaOfflineEsInmediata()throws Exception {
        assertEquals("com.example.basta_game.authtest",i.getTargetContext().getPackageName());
        FirebaseAuth auth=FirebaseAuth.getInstance();
        Tasks.await(auth.createUserWithEmailAndPassword("m16-"+System.currentTimeMillis()+"@ues.edu.sv","SoloEmulador-123"),15,TimeUnit.SECONDS);
        String uid=auth.getCurrentUser().getUid(),id="m16-"+UUID.randomUUID();
        FirebaseDatabase db=FirebaseDatabase.getInstance();DatabaseReference sala=db.getReference("salas").child(id);
        Map<String,Object> datos=new HashMap<>();
        datos.put("idPartida","m16");datos.put("codigoSala","ABC123");datos.put("hostUID","rival");datos.put("estadoPartida","En juego");datos.put("fase","jugando");
        datos.put("rondaActual",2);datos.put("cantidadRondas",3);datos.put("tiempoRonda",1);datos.put("inicioRonda",System.currentTimeMillis()-2000);
        datos.put("inicioRondaDe",2);datos.put("inicioRondaListoDe",2);datos.put("letraActual","A");datos.put("categoriasActivas",Arrays.asList("Animal","Fruta"));
        Map<String,Object> j=new HashMap<>();
        for(String u:Arrays.asList(uid,"rival")){Map<String,Object> jugador=new HashMap<>();jugador.put("uid",u);jugador.put("nombre","Demo "+u);jugador.put("urlAvatar","local");jugador.put("estaConectado",true);jugador.put("esHost",u.equals("rival"));j.put(u,jugador);}
        datos.put("jugadores",j);datos.put("respuestas",Collections.singletonMap("ronda_2",Collections.singletonMap("rival",new HashMap<String,String>(){{put("Animal","Abeja");put("Fruta","Almendra");}})));
        Tasks.await(sala.setValue(datos),15,TimeUnit.SECONDS);
        assertSame("Todas las referencias deben usar el mismo emulador",db,FirebaseDatabase.getInstance());
        Application app=(Application)i.getTargetContext().getApplicationContext();
        i.runOnMainSync(() -> {NotificationHelper.inicializar(app);ConexionJuego.inicializar(app);Sounds.init(app);});
        Bundle b=new Bundle();b.putString("IDSALA",id);b.putString("ID_PARTIDA","m16");b.putString("CODIGO_SALA","ABC123");b.putInt("RONDA_ACTUAL",2);b.putInt("RONDAS",3);b.putInt("TIEMPO",1);b.putString("LETRA","A");b.putStringArrayList("CATEGORIAS",new ArrayList<>(Arrays.asList("Animal","Fruta")));
        Instrumentation.ActivityMonitor home=i.addMonitor(HomeActivity.class.getName(),null,true);
        SalaActivity a=(SalaActivity)i.startActivitySync(new Intent(i.getTargetContext(),SalaActivity.class).putExtra("DATOS_SALA",b).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Activity retorno=null;
        i.waitForIdleSync();
        i.runOnMainSync(() -> System.out.println("M16 inicio: extras="+a.getIntent().hasExtra("DATOS_SALA")+", fragment="+a.getSupportFragmentManager().findFragmentById(R.id.sala_fragment_container)+", uidPresente="+(auth.getCurrentUser()!=null)));
        try {
            esperar(() -> revision(a));
            assertTrue(Tasks.await(sala.child("revisionCategorias/ronda_2/reloj").get(),10,TimeUnit.SECONDS).exists());
            Instrumentation.ActivityMonitor recreada=i.addMonitor(SalaActivity.class.getName(),null,false);
            i.runOnMainSync(a::recreate);SalaActivity actual=(SalaActivity)recreada.waitForActivityWithTimeout(10000);i.removeMonitor(recreada);assertNotNull(actual);
            esperar(() -> revision(actual));
            transporte(db);
            i.runOnMainSync(db::goOffline);Thread.sleep(300);
            long inicio=android.os.SystemClock.elapsedRealtime();i.runOnMainSync(actual::mandarHomePorDesconexion);
            assertTrue("Salida sin esperar Firebase",actual.isFinishing());assertTrue(android.os.SystemClock.elapsedRealtime()-inicio<1500);
            assertEquals(1,home.getHits());assertFalse(i.getTargetContext().getSharedPreferences("salidasPendientes",0).getAll().isEmpty());
            System.out.println("M16 ticket="+i.getTargetContext().getSharedPreferences("salidasPendientes",0).getAll());
            // Home is intercepted: keep a harmless foreground Activity, as real navigation would.
            retorno=i.startActivitySync(new Intent(i.getTargetContext(),RegistrarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            assertSame(db,FirebaseDatabase.getInstance());
            i.runOnMainSync(db::goOnline);
            Thread.sleep(500);i.runOnMainSync(() -> SalidasPendientes.reconciliar(app));
            long fin=System.currentTimeMillis()+15000;boolean retirado=false;
            while(System.currentTimeMillis()<fin){DataSnapshot s=Tasks.await(sala.get(),10,TimeUnit.SECONDS);if(!s.child("jugadores").hasChild(uid)){retirado=true;break;}Thread.sleep(100);}
            assertTrue("Abandono reconciliado sin fantasma",retirado);
        } catch(AssertionError error) {
            i.runOnMainSync(() -> System.out.println("M16 fallo: fragment="+a.getSupportFragmentManager().findFragmentById(R.id.sala_fragment_container)+", salida="+a.haSalidoDeSala()+", disponible="+ConexionJuego.disponible()));
            System.out.println("M16 fixture="+Tasks.await(sala.get(),10,TimeUnit.SECONDS).getValue());
            transporte(db);
            throw error;
        } finally {
            i.runOnMainSync(() -> {db.goOnline();a.finish();});i.removeMonitor(home);
            try {Tasks.await(sala.removeValue(),3,TimeUnit.SECONDS);} catch(Exception limpieza) {System.out.println("M16 limpieza local pendiente: "+limpieza.getClass().getSimpleName());}
            if(retorno!=null){Activity pantalla=retorno;i.runOnMainSync(pantalla::finish);}
            auth.signOut();
        }
    }
}
