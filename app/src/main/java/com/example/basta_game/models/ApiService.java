package com.example.basta_game.models;

import retrofit2.Call;
import retrofit2.http.GET;
public interface ApiService {
    @GET("palabras/ver")
    Call<DiccionarioResponse> obtenerPalabras();
}
