package com.example.basta_game;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.basta_game.models.Usuario;
import com.example.basta_game.utils.NotificationHelper;
import com.example.basta_game.utils.VerificacionesAuth;
import com.google.android.material.textfield.TextInputEditText;

//imports firebase
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etCorreoUsuario;
    private TextInputEditText etPassword;
    private AppCompatButton btnEntrar;
    private TextView tvIrRegistro;

    //instancias de firebase
    private FirebaseAuth auth;
    private DatabaseReference db;

    private View notificationLogin;

    private String nombreUsuario;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etCorreoUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        btnEntrar = findViewById(R.id.btnEntrar);
        tvIrRegistro = findViewById(R.id.tvIrRegistro);
        notificationLogin = findViewById(R.id.LoginNotification);
        auth = FirebaseAuth.getInstance();


        if(VerificacionesAuth.VerificarInicioSesion(auth)){
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class)
                    .putExtra("usuarioLogueado", auth.getCurrentUser());
            startActivity(intent);
        };



        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String usuarioIngresado = etCorreoUsuario.getText().toString();
                String passwordIngresada = etPassword.getText().toString();

                if(usuarioIngresado.isEmpty() || passwordIngresada.isEmpty()){
                    NotificationHelper.show(
                            notificationLogin,
                            "Debes llenar todos los campos primero",
                            NotificationHelper.WARNING
                    );
                }else {

                    auth.signInWithEmailAndPassword(usuarioIngresado, passwordIngresada)
                            .addOnCompleteListener(taskLoguearse ->{
                               Exception exception = taskLoguearse.getException();

                               if(auth != null && taskLoguearse.isSuccessful() && auth.getCurrentUser() != null){
                                   Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                   startActivity(intent);
                               }
                               else if(exception instanceof FirebaseAuthInvalidCredentialsException){
                                    NotificationHelper.show(
                                            notificationLogin,
                                            "Las credenciales son invalidas",
                                            NotificationHelper.ERROR
                                    );
                                    etCorreoUsuario.setText("");
                                    etPassword.setText("");
                                }else if (exception instanceof FirebaseNetworkException) {
                                    NotificationHelper.show(
                                            notificationLogin,
                                            "Revisa tu conexión a internet",
                                            NotificationHelper.ERROR
                                    );
                                    etCorreoUsuario.setText("");
                                    etPassword.setText("");
                                }
                            });
                }

            }
        });

        tvIrRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrarActivity.class);
                startActivity(intent);
            }
        });
    }
}