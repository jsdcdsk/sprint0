package com.example.primero;

import com.example.primero.fake.FakeApiService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** 工厂：根据开关返回 真/假 API */
public class ApiProvider {
    // 一键切换：true = 用假数据；false = 用真实后端
    public static final boolean USE_FAKE = false;

    private static ApiService instance;

    public static ApiService get() {
        if (instance != null) return instance;

        if (USE_FAKE) {
            instance = new FakeApiService();
        } else {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://wjin.upv.edu.es/servidor/api/") // 改成你的
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            instance = retrofit.create(ApiService.class);
        }
        return instance;
    }
}
