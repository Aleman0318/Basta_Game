package com.example.basta_game;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//import de firebase
import com.example.basta_game.Models.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

//imports notificacion personalizada
import android.view.View;
import com.example.basta_game.utils.NotificationHelper;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrarActivity extends AppCompatActivity {

    private TextInputEditText etNombre;
    private TextInputEditText etCorreoReg;
    private TextInputEditText etPasswordReg;
    private AppCompatButton btnRegistrar;
    
    //instancias de firebase
    private FirebaseAuth auth;
    private DatabaseReference db;
    

    //notificacion personalziada
    private View notificationRegister;

    private TextView tvIrLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registrar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etNombre = findViewById(R.id.etNombre);
        etCorreoReg = findViewById(R.id.etCorreoReg);
        etPasswordReg = findViewById(R.id.etPasswordReg);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        tvIrLogin = findViewById(R.id.tvIrLogin);
        notificationRegister = findViewById(R.id.notificationRegister);
        
        auth = FirebaseAuth.getInstance();

        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String emailUser = String.valueOf(etCorreoReg.getText());
                String passUser = String.valueOf(etPasswordReg.getText());
                String nombre = String.valueOf(etNombre.getText());

                if(nombre.isEmpty() || emailUser.isEmpty() || passUser.isEmpty()){
                    NotificationHelper.show(
                            notificationRegister,
                            "Debes llenar todos los campos primero",
                            NotificationHelper.WARNING
                    );

                    Log.d("REGISTRO_FIREBASE", "Pasando filtro de no vacios");

                    if(nombre.isEmpty()){
                        etNombre.setError("Debes llenar este campo");
                    }

                    if(emailUser.isEmpty()){
                        etCorreoReg.setError("Debes llenar este campo");
                    }

                    if (passUser.isEmpty()) {
                        etPasswordReg.setError("Debes llenar este campo");
                    }
                }
                else {

                    Log.d("REGISTRO_FIREBASE", "Ignorando filtro de no vacios (tienen datos)");

                    if(auth!=null){
                        //creamos el usuario
                        auth.createUserWithEmailAndPassword(emailUser, passUser)
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful()){
                                        NotificationHelper.show(
                                                notificationRegister,
                                                "Usuario registrado correctamente",
                                                NotificationHelper.SUCCESS
                                        );

                                        String uid = auth.getCurrentUser().getUid();

                                        Usuario user = new Usuario(
                                                uid,
                                                nombre,
                                                emailUser,
                                                0,
                                                0,
                                                0,
                                                0,
                                                "avatar_01",
                                                System.currentTimeMillis()
                                        );

                                        DatabaseReference usuarioRef = FirebaseDatabase.getInstance().getReference("usuarios");

                                        usuarioRef.child(uid).setValue(user)
                                                .addOnCompleteListener(taskGuardar->{
                                                    if(taskGuardar.isSuccessful()){
                                                        Log.d("REGISTRO_FIREBASE","usuario añadido correctamente a realtime");
                                                    }
                                                    else {
                                                        Log.d("REGISTRO_FIREBASE","ocurrio un error al guardar el perfil el realtime");
                                                    }
                                                });

                                    }
                                    else {
                                        NotificationHelper.show(
                                                notificationRegister,
                                                "¡Ocurrio un error al registrar el usuario!",
                                                NotificationHelper.ERROR
                                        );
                                    }
                                });
                    }else{
                       Log.d("REGISTRO_FIREBASE", "Ocurrio un error por que auth esta vacio");
                    }
                }
            }
        });

        tvIrLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });
        
        
    }
}