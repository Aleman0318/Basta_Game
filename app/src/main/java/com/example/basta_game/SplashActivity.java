package com.example.basta_game;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.example.basta_game.models.ApiService;
import com.example.basta_game.models.DiccionarioResponse;
import com.example.basta_game.models.Palabra;
import com.example.basta_game.models.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {
    public ImageView img_logo_splash;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        img_logo_splash = findViewById(R.id.img_logo_splash);

        Animation animacion = AnimationUtils.loadAnimation(this, R.anim.aparecer_logo);
        animacion.setDuration(1500);
        img_logo_splash.startAnimation(animacion);

        new Handler(Looper.getMainLooper()).postDelayed(()-> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out );

            finish();
        }, 3000);

        ApiService apiService = RetrofitClient
                .getClient()
                        .create(ApiService.class);
        Call<DiccionarioResponse> call = apiService.obtenerPalabras();

        call.enqueue(new Callback<DiccionarioResponse>() {
            @Override
            public void onResponse(Call<DiccionarioResponse> call, Response<DiccionarioResponse> response) {

                if (response.isSuccessful() && response.body() != null){

                    List<Palabra> palabras = response.body().getData();
                    Log.d("API", "Cantidad: " + palabras.size());

                    for (Palabra p : palabras){
                        Log.d("API", p.getPalabra() + " - " +
                                p.getCategoria());
                    }
                }
            }

            @Override
            public void onFailure(Call<DiccionarioResponse> call, Throwable throwable) {
                Log.e("API", throwable.getMessage());
            }
        });
    }

}