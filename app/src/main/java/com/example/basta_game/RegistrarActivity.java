package com.example.basta_game;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputEditText;

public class RegistrarActivity extends AppCompatActivity {

    private TextInputEditText etNombre;
    private TextInputEditText etCorreoReg;
    private TextInputEditText etPasswordReg;
    private AppCompatButton btnRegistrar;
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

        tvIrLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });
    }
}