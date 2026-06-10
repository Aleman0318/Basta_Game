package com.example.basta_game;

import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        if(auth != null){
            Toast.makeText(this, "Firebase Auth inicializado", Toast.LENGTH_SHORT).show();
            String email = "Diegolingo@gmail.com";
            String password = "Diego12345";

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                       if(task.isSuccessful()){
                           Toast.makeText(this, "Usuario creado con éxito", Toast.LENGTH_SHORT).show();
                       }
                       else{
                           Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                       }
                    });
        }



    }
}