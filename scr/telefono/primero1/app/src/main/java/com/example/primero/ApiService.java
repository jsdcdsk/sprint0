package com.example.primero;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers({
            "Content-Type: application/json",
            "X-API-KEY: YOUR_SECURE_RANDOM_KEY"     // 与 PHP 保持一致
    })
    @POST("insert_device.php")
    Call<ApiResponse> insertDevice(@Body Device d);
}