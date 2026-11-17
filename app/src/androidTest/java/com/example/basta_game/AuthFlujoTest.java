package com.example.basta_game;
import android.app.*;import android.content.*;import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;import static org.junit.Assert.*;
public class AuthFlujoTest {
 private final Instrumentation i=InstrumentationRegistry.getInstrumentation();
 private Activity abrir(Class<?> tipo){return i.startActivitySync(new Intent(i.getTargetContext(),tipo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
 private void texto(Activity a,int id,String v){i.runOnMainSync(()->((TextView)a.findViewById(id)).setText(v));}
 private void pulsar(Activity a,int id){i.runOnMainSync(()->{assertTrue(a.findViewById(id).isEnabled());});androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(id)).perform(androidx.test.espresso.action.ViewActions.click());}
 private void esperar(java.util.function.BooleanSupplier listo)throws Exception{long fin=System.currentTimeMillis()+20000;while(!listo.getAsBoolean()&&System.currentTimeMillis()<fin)Thread.sleep(100);assertTrue("No llegó confirmación",listo.getAsBoolean());}
 @Test public void registroLoginErrorYReintentoSinDependerDeRTDB()throws Exception{
  assertFalse(com.example.basta_game.utils.ConexionJuego.disponible());FirebaseAuth auth=FirebaseAuth.getInstance();auth.signOut();
  Instrumentation.ActivityMonitor home=i.addMonitor(HomeActivity.class.getName(),null,true);
  String email="demo"+System.currentTimeMillis()+"@ues.edu.sv";String clave="SoloEmulador-123";
  Activity r=abrir(RegistrarActivity.class);texto(r,R.id.etNombre,"Demo local");texto(r,R.id.etCorreoReg,email);texto(r,R.id.etPasswordReg,clave);pulsar(r,R.id.btnRegistrar);
  esperar(()->home.getHits()==1);assertNotNull(auth.getCurrentUser());java.net.HttpURLConnection consulta=(java.net.HttpURLConnection)new java.net.URL("http://10.0.2.2:9000/usuarios/"+auth.getCurrentUser().getUid()+"/nombre.json?ns=demo-basta-auth-default-rtdb").openConnection();consulta.setRequestProperty("Authorization","Bearer owner");consulta.setConnectTimeout(5000);consulta.setReadTimeout(5000);
  try(java.io.BufferedReader lectura=new java.io.BufferedReader(new java.io.InputStreamReader(consulta.getInputStream()))){assertEquals("\"Demo local\"",lectura.readLine());}finally{consulta.disconnect();}assertTrue(r.findViewById(R.id.btnRegistrar).isEnabled());i.runOnMainSync(r::finish);auth.signOut();
  com.google.firebase.database.FirebaseDatabase.getInstance().goOffline();
  Activity l=abrir(LoginActivity.class);texto(l,R.id.etUsuario,email);texto(l,R.id.etPassword,"Incorrecta-123");pulsar(l,R.id.btnEntrar);esperar(()->l.findViewById(R.id.btnEntrar).isEnabled());assertNull(auth.getCurrentUser());assertEquals(1,home.getHits());
  i.getTargetContext().startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();
  i.getTargetContext().startActivity(new Intent(i.getTargetContext(),LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));i.waitForIdleSync();
  texto(l,R.id.etUsuario,email);texto(l,R.id.etPassword,clave);pulsar(l,R.id.btnEntrar);esperar(()->home.getHits()==2);assertNotNull(auth.getCurrentUser());assertTrue(l.findViewById(R.id.btnEntrar).isEnabled());com.google.firebase.database.FirebaseDatabase.getInstance().goOnline();i.runOnMainSync(l::finish);auth.signOut();
  Activity duplicado=abrir(RegistrarActivity.class);texto(duplicado,R.id.etNombre,"Demo local");texto(duplicado,R.id.etCorreoReg,email);texto(duplicado,R.id.etPasswordReg,clave);pulsar(duplicado,R.id.btnRegistrar);esperar(()->duplicado.findViewById(R.id.btnRegistrar).isEnabled());assertEquals(2,home.getHits());assertNull(auth.getCurrentUser());i.runOnMainSync(duplicado::finish);i.removeMonitor(home);
 }
}
