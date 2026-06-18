package com.example.basta_game.models;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @GET("palabras/ver")
    Call<DiccionarioResponse> obtenerPalabras();

    @POST("api/validar-ronda")
    Call<ValidacionResponse> validarRonda(
            @Body ValidacionRequest request
    );
}