package com.example.primero.fake;

import java.io.IOException;

import okhttp3.Request;
import okio.Timeout;                 // 注意是 okio.Timeout
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 可选：消除注解覆盖告警
@retrofit2.internal.EverythingIsNonNull
public class SimpleCall<T> implements Call<T> {
    private final T data;

    public SimpleCall(T data) {
        this.data = data;
    }

    @Override
    public Response<T> execute() throws IOException {
        return Response.success(data);
    }

    @Override
    public void enqueue(Callback<T> callback) {
        callback.onResponse(this, Response.success(data));
    }

    @Override
    public boolean isExecuted() { return false; }

    @Override
    public void cancel() { }

    @Override
    public boolean isCanceled() { return false; }

    @Override
    public Call<T> clone() { return new SimpleCall<>(data); }

    @Override
    public Request request() {
        return new Request.Builder().url("http://fake.local/").build();
    }

    @Override
    public Timeout timeout() {        // 返回 okio.Timeout
        return new Timeout();
    }
}
