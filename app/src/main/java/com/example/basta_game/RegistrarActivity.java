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
                String nombre = com.example.basta_game.utils.NombresUnicos.normalizarPresentacion(
                        String.valueOf(etNombre.getText()));

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

                    if (!com.example.basta_game.utils.NombresUnicos.valido(nombre)) {
                        etNombre.setError("Nombre de 2 a 24 caracteres, letras o números");
                        return;
                    }
                    if (!CorreoInstitucional.esValido(emailUser)) {
                        NotificationHelper.show(notificationRegister,
                                "Correo inválido. Debes utilizar tu correo institucional @ues.edu.sv",
                                NotificationHelper.WARNING);
                        Sounds.playNotificaciones();
                        return;
                    }
                    if(auth!=null){
                        //creamos el usuario
                        if(!com.example.basta_game.utils.CargaBoton.iniciar(btnRegistrar))return;
        final Object operacion=com.example.basta_game.utils.CargaBoton.token(btnRegistrar);
                        android.util.Log.d("AuthFlujo","Solicitando autenticación");
                    auth.createUserWithEmailAndPassword(emailUser, passUser)
                                .addOnCompleteListener(task -> {
                                    android.util.Log.d("AuthFlujo","Registro Auth finalizó: exito="+task.isSuccessful());
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

                                        DatabaseReference nombres = FirebaseDatabase.getInstance()
                                                .getReference("nombresVisibles")
                                                .child(com.example.basta_game.utils.NombresUnicos.clave(nombre));
                                        // Reserva exclusiva: incluso dos registros simultáneos
                                        // no pueden tomar el mismo nombre ignorando mayúsculas.
                                        nombres.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                                            @androidx.annotation.NonNull
                                            @Override public com.google.firebase.database.Transaction.Result doTransaction(
                                                    @androidx.annotation.NonNull com.google.firebase.database.MutableData valor) {
                                                String existente = valor.getValue(String.class);
                                                if (existente != null && !uid.equals(existente))
                                                    return com.google.firebase.database.Transaction.abort();
                                                valor.setValue(uid);
                                                return com.google.firebase.database.Transaction.success(valor);
                                            }
                                            @Override public void onComplete(com.google.firebase.database.DatabaseError error,
                                                    boolean reservado, com.google.firebase.database.DataSnapshot dato) {
                                                if (!reservado || error != null) {
                                                    com.example.basta_game.utils.CargaBoton.terminar(btnRegistrar,operacion);
                                                    etNombre.setError(error == null ? "Nombre ocupado; elige otro" :
                                                            "No se pudo reservar el nombre en Firebase");
                                                    // La cuenta Auth ya se creó. Eliminarla si es posible
                                                    // para permitir reintentar el registro sin duplicados.
                                                    if (auth.getCurrentUser() != null && uid.equals(auth.getCurrentUser().getUid())) {
                                                        auth.getCurrentUser().delete().addOnFailureListener(e -> {
                                                            if (!isFinishing()) NotificationHelper.show(notificationRegister,
                                                                    "No se pudo limpiar la cuenta provisional; inicia sesión para recuperarla",
                                                                    NotificationHelper.WARNING);
                                                        });
                                                    }
                                                    return;
                                                }
                                                DatabaseReference usuarioRef = FirebaseDatabase.getInstance().getReference("usuarios");
                                                usuarioRef.child(uid).setValue(user).addOnCompleteListener(guardado -> {
                                                    com.example.basta_game.utils.CargaBoton.terminar(btnRegistrar,operacion);
                                                    if (guardado.isSuccessful() && !isFinishing() && !isDestroyed()) {
                                                        NotificationHelper.show(notificationRegister,
                                                                "Usuario registrado correctamente",NotificationHelper.SUCCESS);
                                                        Sounds.playNotificaciones();
                                                        startActivity(new Intent(RegistrarActivity.this,HomeActivity.class));
                                                    } else {
                                                        com.example.basta_game.utils.NombresUnicos.soltarReservaAnterior(uid,nombre);
                                                        if (!isFinishing()) NotificationHelper.show(notificationRegister,
                                                                "No se pudo guardar el perfil. Verifica tu conexión",
                                                                NotificationHelper.ERROR);
                                                    }
                                                });
                                            }
                                        }, false);

                                    }
                                    else {
                                        com.example.basta_game.utils.CargaBoton.terminar(btnRegistrar,operacion);
                                        NotificationHelper.show(
                                                notificationRegister,
                                                task.getException()!=null ? task.getException().getLocalizedMessage() : "No se pudo registrar la cuenta",
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
