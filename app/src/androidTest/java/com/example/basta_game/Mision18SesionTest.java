package com.example.basta_game;

import android.app.*;
import android.content.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.Tasks;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class Mision18SesionTest {
    @Test public void atrasConfirmaCancelarConservaSesionYConfirmarLimpiaPila() throws Exception {
        android.app.Instrumentation i=InstrumentationRegistry.getInstrumentation();assertEquals("com.example.basta_game.authtest",i.getTargetContext().getPackageName());
        FirebaseAuth auth=FirebaseAuth.getInstance();Tasks.await(auth.createUserWithEmailAndPassword("m18-"+System.currentTimeMillis()+"@ues.edu.sv","SoloEmulador-123"),15,TimeUnit.SECONDS);
        String uid=auth.getCurrentUser().getUid();Map<String,Object> perfil=new HashMap<>();perfil.put("nombre","Demo local");perfil.put("correo","demo@ues.edu.sv");perfil.put("monedas",0);perfil.put("gemas",0);
        Tasks.await(FirebaseDatabase.getInstance().getReference("usuarios").child(uid).setValue(perfil),15,TimeUnit.SECONDS);
        i.runOnMainSync(() -> {com.example.basta_game.utils.NotificationHelper.inicializar((Application)i.getTargetContext().getApplicationContext());com.example.basta_game.utils.Sounds.init(i.getTargetContext());});
        HomeActivity a=(HomeActivity)i.startActivitySync(new Intent(i.getTargetContext(),HomeActivity.class).putExtra("DESCONEXION",false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        android.app.Instrumentation.ActivityMonitor login=i.addMonitor(LoginActivity.class.getName(),null,false);Activity destino=null;
        try {
            java.lang.reflect.Field campo=HomeActivity.class.getDeclaredField("confirmacionSalida");campo.setAccessible(true);
            i.runOnMainSync(() -> a.getOnBackPressedDispatcher().onBackPressed());i.waitForIdleSync();Dialog d=(Dialog)campo.get(a);assertTrue(d.isShowing());
            i.runOnMainSync(() -> a.getOnBackPressedDispatcher().onBackPressed());assertSame(d,campo.get(a));
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Cancelar")).inRoot(androidx.test.espresso.matcher.RootMatchers.withDecorView(org.hamcrest.Matchers.is(d.getWindow().getDecorView()))).perform(androidx.test.espresso.action.ViewActions.click());
            assertFalse(d.isShowing());assertEquals(uid,auth.getCurrentUser().getUid());assertFalse(a.isFinishing());
            i.runOnMainSync(() -> a.getOnBackPressedDispatcher().onBackPressed());i.waitForIdleSync();Dialog segundo=(Dialog)campo.get(a);
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Cerrar sesión")).inRoot(androidx.test.espresso.matcher.RootMatchers.withDecorView(org.hamcrest.Matchers.is(segundo.getWindow().getDecorView()))).perform(androidx.test.espresso.action.ViewActions.click());
            destino=i.waitForMonitorWithTimeout(login,10000);assertNotNull(destino);assertNull(auth.getCurrentUser());assertTrue(a.isFinishing());
            assertTrue((destino.getIntent().getFlags()&Intent.FLAG_ACTIVITY_CLEAR_TASK)!=0);
        } finally {Activity fin=destino;i.runOnMainSync(() -> {a.finish();if(fin!=null)fin.finish();});i.removeMonitor(login);auth.signOut();}
    }
}
