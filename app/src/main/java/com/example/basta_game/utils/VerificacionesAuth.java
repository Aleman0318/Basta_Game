package com.example.basta_game.utils;

import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.basta_game.HomeActivity;
import com.example.basta_game.LoginActivity;
import com.example.basta_game.models.Usuario;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public  class VerificacionesAuth {
    private static Usuario usuarioComprobado;

    public static Usuario CargarYMostrarUsuarioLogueado(View vista, FirebaseAuth auth){

            String uid = auth.getCurrentUser().getUid();

            DatabaseReference db = FirebaseDatabase.getInstance().getReference("usuarios");
            db.child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Usuario userTemporal = snapshot.getValue(Usuario.class);

                            if(userTemporal != null){
                                 usuarioComprobado = userTemporal;
                                 String nombreUsuarioNotificacion = usuarioComprobado.getNombre();

                                if(!nombreUsuarioNotificacion.isEmpty()){
                                    NotificationHelper.show(
                                            vista,
                                            "¡Bienvenido " + nombreUsuarioNotificacion + "!",
                                            NotificationHelper.SUCCESS
                                    );
                                }
                                else{
                                    Log.d("NOMBRE_DEBUG", "NO SE CARGO CORRECTAMENTE EL NOMBRE");
                                }
                            }
                            else{
                                Log.d("LOGIN_DEBUG", "la instancia usuario esta null");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            return usuarioComprobado;
    }

    public static boolean VerificarInicioSesion(FirebaseAuth authentication){
        if(authentication != null && authentication.getCurrentUser() != null){
            return true;
        }

        return false;
    }

}
