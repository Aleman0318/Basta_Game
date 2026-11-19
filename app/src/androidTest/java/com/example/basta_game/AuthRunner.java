package com.example.basta_game;
import android.app.Application;
import android.content.Context;
import androidx.test.runner.AndroidJUnitRunner;
import com.google.firebase.*;
public class AuthRunner extends AndroidJUnitRunner {
 @Override public Application newApplication(ClassLoader c,String n,Context ctx)throws InstantiationException,IllegalAccessException,ClassNotFoundException{return super.newApplication(c,Application.class.getName(),ctx);}
 @Override public void onStart(){
  Context c=getTargetContext();if(!c.getPackageName().equals("com.example.basta_game.authtest"))throw new IllegalStateException("Solo perfil aislado");
  // SDK 22 mutates RepoInfo when applying useEmulator; keep its cache key local from the start.
  FirebaseApp.initializeApp(c,new FirebaseOptions.Builder().setProjectId("demo-basta-auth").setStorageBucket("demo-basta-auth.appspot.com").setApplicationId("1:123456789:android:demobasta").setApiKey("AIzaSyDemoBastaAuthenticationEmulatorOnly").setDatabaseUrl("http://10.0.2.2:9000?ns=demo-basta-auth-default-rtdb").build());
  com.google.firebase.storage.FirebaseStorage.getInstance().useEmulator("10.0.2.2",9199);
  com.google.firebase.storage.FirebaseStorage.getInstance().setMaxOperationRetryTimeMillis(1000);
  com.google.firebase.auth.FirebaseAuth.getInstance().useEmulator("10.0.2.2",9099);com.google.firebase.database.FirebaseDatabase.getInstance().useEmulator("10.0.2.2",9000);super.onStart();
 }
}
