package com.example.primero;

import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Response;

import static org.junit.Assert.*;

public class InsertDeviceTest {

    @Test
    public void testInsertDevice() throws Exception {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://wjin.upv.edu.es/servidor/api/") // 改成你服务器真实路径
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService api = retrofit.create(ApiService.class);

        Device device = new Device();     // 无参构造器默认存在
        device.nombre    = "GTI-3A";
        device.mac       = "AA:BB:CC:DD:EE:FF";
        device.rssi      = -53;
        device.uuid      = "EPSG-GTI-PROY-3A";
        device.major     = 2816;          // (11 << 8)
        device.minor     = 523;
        device.txPower   = 4;
        device.timestamp = System.currentTimeMillis();

        Response<ApiResponse> response = api.insertDevice(device).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().isOk());

        System.out.println("✅ Inserción correcta, id=" + response.body().getId());
    }
}
