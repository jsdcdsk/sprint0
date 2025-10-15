package com.example.primero;

// ------------------------------------------------------------------
//  MainActivity con control robusto del escaneo BLE.
//  Cambios clave:
//   1) Bandera global `escaneando` (volatile) para cortar callbacks.
//   2) Antes de iniciar cualquier escaneo -> detener el anterior.
//   3) Guardas en callbacks y en subida para ignorar eventos tras detener.
//   4) detenerBusquedaDispositivosBTLE() usa try/catch y "soft stop" aunque
//      BLUETOOTH_SCAN falte en Android 12+.
// ------------------------------------------------------------------

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// ------------------------------------------------------------------
// ------------------------------------------------------------------

public class MainActivity extends AppCompatActivity {

    // --------------------------------------------------------------
    // Etiquetas y códigos
    // --------------------------------------------------------------
    private static final String ETIQUETA_LOG = "basedato";
    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    // --------------------------------------------------------------
    // BLE + API
    // --------------------------------------------------------------
    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo = null;

    // Bandera de control de flujo:
    // - true  -> aceptamos resultados y subimos datos
    // - false -> ignoramos cualquier callback pendiente ("soft stop")
    private volatile boolean escaneando = false;

    private ApiService api;

    // --------------------------------------------------------------
    // Escaneo de TODOS los dispositivos (sin filtros)
    // --------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): empieza");

        // 1) Antes de iniciar, garantizamos no dejar otro escaneo vivo
        detenerBusquedaDispositivosBTLE();

        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): instalamos scan callback");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                // Corte duro: si ya hemos “parado”, no procesar más resultados
                if (!escaneando) return;

                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): onScanResult()");
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                if (!escaneando) return;
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): onBatchScanResults()");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): onScanFailed() code=" + errorCode);
            }
        };

        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): empezamos a escanear");

        // 2) Iniciar el escaneo (sin filtros). Tras llamar, marcamos escaneando=true.
        if (this.elEscanner != null) {
            try {
                this.elEscanner.startScan(this.callbackDelEscaneo);
                this.escaneando = true;
            } catch (SecurityException se) {
                Log.w(ETIQUETA_LOG, "startScan SecurityException (revise permisos en Android 12+)", se);
                this.escaneando = false;
                this.callbackDelEscaneo = null;
            }
        }
    } // ()

    // --------------------------------------------------------------
    // Muestra/parsea la información de un dispositivo BTLE
    // (solo LOGs y decode de iBeacon)
    // --------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {

        BluetoothDevice bluetoothDevice = resultado.getDevice();
        byte[] bytes = resultado.getScanRecord() != null ? resultado.getScanRecord().getBytes() : new byte[0];
        int rssi = resultado.getRssi();
        String nombre = "DESCONOCIDO"; // valor por defecto si no hay permiso

        // Android 12+ requiere BLUETOOTH_CONNECT para getName()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                nombre = bluetoothDevice.getName();
            } else {
                Log.w(ETIQUETA_LOG, "⚠️ Sin permiso BLUETOOTH_CONNECT para leer nombre");
            }
        } else {
            nombre = bluetoothDevice.getName();
        }

        Log.d(ETIQUETA_LOG, " ****************************************************");
        Log.d(ETIQUETA_LOG, " ****** DISPOSITIVO DETECTADO BTLE ****************** ");
        Log.d(ETIQUETA_LOG, " ****************************************************");
        Log.d(ETIQUETA_LOG, " nombre = " + nombre);
        Log.d(ETIQUETA_LOG, " toString = " + bluetoothDevice);

        Log.d(ETIQUETA_LOG, " dirección = " + bluetoothDevice.getAddress());
        Log.d(ETIQUETA_LOG, " rssi = " + rssi);

        Log.d(ETIQUETA_LOG, " bytes(" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));

        TramaIBeacon tib = new TramaIBeacon(bytes);

        Log.d(ETIQUETA_LOG, " ----------------------------------------------------");
        Log.d(ETIQUETA_LOG, " prefijo  = " + Utilidades.bytesToHexString(tib.getPrefijo()));
        Log.d(ETIQUETA_LOG, "          advFlags = " + Utilidades.bytesToHexString(tib.getAdvFlags()));
        Log.d(ETIQUETA_LOG, "          advHeader = " + Utilidades.bytesToHexString(tib.getAdvHeader()));
        Log.d(ETIQUETA_LOG, "          companyID = " + Utilidades.bytesToHexString(tib.getCompanyID()));
        Log.d(ETIQUETA_LOG, "          iBeacon type = " + Integer.toHexString(tib.getiBeaconType()));
        Log.d(ETIQUETA_LOG, "          iBeacon length 0x = " + Integer.toHexString(tib.getiBeaconLength()) + " ( "
                + tib.getiBeaconLength() + " ) ");
        Log.d(ETIQUETA_LOG, " uuid  = " + Utilidades.bytesToHexString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, " uuid  (ascii)= " + Utilidades.bytesToString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, " major = " + Utilidades.bytesToHexString(tib.getMajor()) + " ( "
                + Utilidades.bytesToInt(tib.getMajor()) + " ) ");
        Log.d(ETIQUETA_LOG, " minor = " + Utilidades.bytesToHexString(tib.getMinor()) + " ( "
                + Utilidades.bytesToInt(tib.getMinor()) + " ) ");
        Log.d(ETIQUETA_LOG, " txPower = " + Integer.toHexString(tib.getTxPower()) + " ( " + tib.getTxPower() + " )");
        Log.d(ETIQUETA_LOG, " ****************************************************");
    } // ()

    // --------------------------------------------------------------
    // Escaneo con filtro por nombre de dispositivo
    // --------------------------------------------------------------
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): empieza");

        // 1) Antes de iniciar, parar cualquier escaneo previo
        detenerBusquedaDispositivosBTLE();

        Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): instalamos scan callback");

        if (elEscanner == null) return;

        // Android 12+: comprobar/solicitar permiso de SCAN
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(ETIQUETA_LOG, "No se tiene permiso BLUETOOTH_SCAN, solicitando...");
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN},
                        CODIGO_PETICION_PERMISOS
                );
                return;
            }
        }

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                if (!escaneando) return; // corte duro para callbacks rezagados

                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): onScanResult()");
                mostrarInformacionDispositivoBTLE(resultado);

                byte[] bytes = resultado.getScanRecord() != null ? resultado.getScanRecord().getBytes() : new byte[0];
                TramaIBeacon tib = new TramaIBeacon(bytes);

                subirDatosAMySQL(resultado, tib);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                if (!escaneando) return;
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): onBatchScanResults()");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): onScanFailed() code=" + errorCode);
            }
        };

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setDeviceName(dispositivoBuscado).build());

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // descubrir más rápido
                .build();

        try {
            this.elEscanner.startScan(filters, settings, this.callbackDelEscaneo);
            this.escaneando = true;
        } catch (SecurityException se) {
            Log.w(ETIQUETA_LOG, "startScan con filtros SecurityException (Android 12+)", se);
            this.escaneando = false;
            this.callbackDelEscaneo = null;
        }

        Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): escaneando: " + dispositivoBuscado);
    } // ()

    // --------------------------------------------------------------
    // Detener el escaneo BTLE de forma robusta
    // --------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {
        // Nota: aunque no tengamos callback o permisos, hacemos "soft stop" para
        // que no se procese nada más (escaneando=false)
        if (this.callbackDelEscaneo == null && !this.escaneando) {
            return;
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Intentar pedir permiso; incluso si no lo conceden, haremos soft stop igualmente
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.BLUETOOTH_SCAN},
                            CODIGO_PETICION_PERMISOS
                    );
                }
            }

            if (this.elEscanner != null && this.callbackDelEscaneo != null) {
                this.elEscanner.stopScan(this.callbackDelEscaneo);
            }
        } catch (SecurityException se) {
            Log.w(ETIQUETA_LOG, "stopScan SecurityException (Android 12+), aplicando soft stop", se);
        } catch (Exception e) {
            Log.w(ETIQUETA_LOG, "stopScan Exception, aplicando soft stop", e);
        } finally {
            // Soft stop: independientemente de si stopScan tuvo éxito, cortamos callbacks/altas
            this.escaneando = false;
            this.callbackDelEscaneo = null;
        }
    } // ()

    // --------------------------------------------------------------
    // Botones (UI)
    // --------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "boton buscar dispositivos BTLE Pulsado");
        this.buscarTodosLosDispositivosBTLE();
    } // ()

    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "boton nuestro dispositivo BTLE Pulsado");
        // Ejemplo de nombre a filtrar (ajústalo a tu beacon):
        this.buscarEsteDispositivoBTLE("GTI-Mery");
    } // ()

    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "boton detener busqueda dispositivos BTLE Pulsado");
        this.detenerBusquedaDispositivosBTLE();
    } // ()

    // --------------------------------------------------------------
    // Inicialización Bluetooth + permisos básicos
    // --------------------------------------------------------------
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): obtenemos adaptador BT");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): habilitamos adaptador BT");
        if (bta != null && !bta.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, CODIGO_PETICION_PERMISOS);
        }

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): habilitado = " + (bta != null && bta.isEnabled()));
        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): estado = " + (bta != null ? bta.getState() : -1));

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): obtenemos escaner btle");
        this.elEscanner = (bta != null) ? bta.getBluetoothLeScanner() : null;

        if (this.elEscanner == null) {
            Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): ¡NO hemos obtenido escaner btle!");
        }

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): voy a pedir permisos (si no los tuviera)");

        // Permisos base para versiones < Android 12 (a partir de 12 hay permisos nuevos específicos)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    CODIGO_PETICION_PERMISOS
            );
        } else {
            Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): ya tengo permisos base");
        }
    } // ()

    // --------------------------------------------------------------
    // ciclo de vida
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = ApiProvider.get();  // ApiProvider.USE_FAKE = true 时走 Fake；false 走真


        Log.d(ETIQUETA_LOG, "onCreate(): empieza");
        inicializarBlueTooth();
        Log.d(ETIQUETA_LOG, "onCreate(): termina");
    } // onCreate()

    // --------------------------------------------------------------
    // Subida a MySQL (con doble verificación de `escaneando`)
    // --------------------------------------------------------------
    private void subirDatosAMySQL(ScanResult resultado, TramaIBeacon tib) {
        // Si se detuvo recientemente, evitamos subir (doble seguro)
        if (!escaneando) return;

        BluetoothDevice device = resultado.getDevice();

        String nombre = "DESCONOCIDO";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                if (device.getName() != null && !device.getName().isEmpty()) nombre = device.getName();
            }
        } else if (device.getName() != null && !device.getName().isEmpty()) {
            nombre = device.getName();
        }

        Device d = new Device();
        d.nombre = nombre;
        d.mac = device.getAddress();
        d.rssi = resultado.getRssi();
        d.uuid = Utilidades.bytesToHexString(tib.getUUID());
        d.major = Utilidades.bytesToInt(tib.getMajor());
        d.minor = Utilidades.bytesToInt(tib.getMinor());
        d.txPower = tib.getTxPower() & 0xFF;
        d.timestamp = System.currentTimeMillis();

        // Verificación rápida de estado antes de llamar a la red
        if (!escaneando) return;

        api.insertDevice(d).enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse> call,
                                   retrofit2.Response<ApiResponse> response) {
                if (!escaneando) return; // si ya se detuvo, ignorar
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    Log.d(ETIQUETA_LOG, "✅ Subido a MySQL, id=" + response.body().id);
                } else {
                    Log.e(ETIQUETA_LOG, "❌ Fallo API: " + (response.body() != null ? response.body().error : ("HTTP " + response.code())));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse> call, Throwable t) {
                if (!escaneando) return; // si ya se detuvo, ignorar
                Log.e(ETIQUETA_LOG, "❌ Error de red: " + t.getMessage());
            }
        });
    }

    // --------------------------------------------------------------
    // Resultado de permisos
    // --------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PETICION_PERMISOS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(ETIQUETA_LOG, "onRequestPermissionResult(): permisos concedidos");
            } else {
                Log.d(ETIQUETA_LOG, "onRequestPermissionResult(): permisos NO concedidos");
            }
        }
    } // ()
} // class
// --------------------------------------------------------------
// --------------------------------------------------------------
