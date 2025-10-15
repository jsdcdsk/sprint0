package com.example.primero.fake;

import com.example.primero.ApiService;
import com.example.primero.Device;
import com.example.primero.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;   // ← 加上这个

import retrofit2.Call;

/**
 * 假逻辑（Fake）版本的 ApiService
 * 不访问服务器，不发送 HTTP，只返回内存中的虚拟数据。
 */
@retrofit2.internal.EverythingIsNonNull
public class FakeApiService implements ApiService {

    // 模拟的“数据库”
    private final List<Device> inMemory = new ArrayList<>();

    public FakeApiService() {
        // 初始化一些虚拟数据
        inMemory.add(make("Beacon A", "AA:BB:CC:DD:EE:01", -60, "EPSG-GTI-PROY-3A", (11 << 8) + 1, 500, -53));
        inMemory.add(make("Beacon B", "AA:BB:CC:DD:EE:02", -65, "EPSG-GTI-PROY-3A", (12 << 8) + 2, 10, -53));
    }

    private Device make(String nombre, String mac, int rssi, String uuid, int major, int minor, int txPower) {
        Device d = new Device();
        d.nombre = nombre;
        d.mac = mac;
        d.rssi = rssi;
        d.uuid = uuid;
        d.major = major;
        d.minor = minor;
        d.txPower = txPower;
        d.timestamp = System.currentTimeMillis();
        return d;
    }

    // 只需要实现 insertDevice，因为接口只有这一个方法
    @Override
    public Call<ApiResponse> insertDevice(Device d) {
        // 模拟“插入数据库”
        if (d == null) d = new Device();
        if (d.timestamp == 0) d.timestamp = System.currentTimeMillis();
        inMemory.add(d);

        // 构造一个“成功”的响应
        ApiResponse resp = new ApiResponse();

        // 优先尝试调用 setter；若没有，再用反射给 public 字段赋值（兼容多种 ApiResponse 结构）
        try {
            // 1) 尝试 setter
            try {
                ApiResponse.class.getMethod("setOk", boolean.class).invoke(resp, true);
                ApiResponse.class.getMethod("setMessage", String.class).invoke(resp, "Fake insert ok（未真正连接服务器）");
            } catch (NoSuchMethodException e) {
                // 2) 无 setter，则尝试直接改字段
                Field fOk = resp.getClass().getDeclaredField("ok");
                fOk.setAccessible(true);
                fOk.set(resp, Boolean.TRUE);

                Field fMsg = resp.getClass().getDeclaredField("message");
                fMsg.setAccessible(true);
                fMsg.set(resp, "Fake insert ok（未真正连接服务器）");
            }
        } catch (Throwable ignore) {
            // 如果既没有 setter 也没有字段，就什么都不做——UI 端只要收到 200/成功即可
        }

        return new SimpleCall<>(resp);
    }
}
