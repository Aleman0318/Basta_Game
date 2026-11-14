package com.example.basta_game;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.basta_game.models.Usuario;
import com.example.basta_game.utils.Sounds;
import com.example.basta_game.utils.CorreoInstitucional;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.example.basta_game.utils.NotificationHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrarActivity extends AppCompatActivity {

    private TextInputEditText etNombre;
    private TextInputEditText etCorreoReg;
    private TextInputEditText etPasswordReg;
    private AppCompatButton btnRegistrar;

    private FirebaseAuth auth;
    private DatabaseReference db;

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
                    Sounds.playNotificaciones();

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

                    if (!CorreoInstitucional.esValido(emailUser)) {
                        NotificationHelper.show(notificationRegister,
                                "Correo inválido. Debes utilizar tu correo institucional @ues.edu.sv",
                                NotificationHelper.WARNING);
                        Sounds.playNotificaciones();
                        return;
                    }
                    if(auth!=null){
                        //creamos el usuario
                        auth.createUserWithEmailAndPassword(emailUser, passUser)
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful()){
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
                                                System.currentTimeMillis(),
                                                ""
                                        );

                                        NotificationHelper.show(
                                                notificationRegister,
                                                "Usuario registrado correctamente",
                                                NotificationHelper.SUCCESS
                                        );
                                        Sounds.playNotificaciones();

                                        DatabaseReference usuarioRef = FirebaseDatabase.getInstance().getReference("usuarios");

                                        usuarioRef.child(uid).setValue(user)
                                                .addOnCompleteListener(taskGuardar->{
                                                    if(taskGuardar.isSuccessful()){
                                                        Log.d("REGISTRO_FIREBASE","usuario añadido correctamente a realtime");
                                                        Intent intent = new Intent(RegistrarActivity.this, HomeActivity.class);
                                                        startActivity(intent);
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
                                        Sounds.playNotificaciones();
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
