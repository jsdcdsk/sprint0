# Proyecto de Biometría – IoT con iBeacon, Android y Servidor Web

Este proyecto integra **un emisor BLE (iBeacon)** en Arduino/nRF52, **una app Android** que escanea y sube datos, **un servidor PHP/MySQL** que persiste la información y un **cliente web** para visualizarla. Es ideal como plantilla docente para telemetría sencilla (p. ej. CO₂ y temperatura) vía BLE → Android → REST → Base de datos.

## 📦 Estructura del repositorio

```
.
├── arduino/                  # Emisor BLE (nRF52/Arduino)
│   ├── HolaMundoIBeacon.ino
│   ├── EmisoraBLE.h
│   ├── ServicioEnEmisora.h
│   ├── Publicador.h
│   ├── Medidor.h
│   ├── LED.h
│   └── PuertoSerie.h
│
├── telefono/                 # App Android (Java)
│   ├── MainActivity.java
│   ├── Device.java
│   ├── TramaIBeacon.java
│   ├── ApiService.java
│   ├── ApiProvider.java
│   ├── ApiResponse.java
│   ├── Utilidades.java
│   └── fake/                 # Lógica fake (opcional para pruebas)
│       ├── FakeApiService.java
│       └── SimpleCall.java
│
├── servidor/                 # PHP + MySQL
│   ├── dispositivos.php      # Listado/consulta
│   ├── api/
│   │   ├── db.php
│   │   ├── insert_device.php
│   │   └── logica.php
│   ├── logs/                 # (opcional) ficheros de log
│   └── api.md                # Documentación de la API
│
└── cliente/
    └── webcliente.html       # Cliente HTML con fallback FAKE
```

> Notas de trazabilidad:
> – El **cliente web** implementa forzado/auto-fallback FAKE y parsing tolerante a dos formatos de respuesta JSON.
> – En **Arduino**, `Publicador.h` codifica el tipo de medición en `major` y el valor en `minor`.
> – La emisora BLE se gestiona con `EmisoraBLE.h`.
> – `Medidor.h` y `LED.h` contienen utilidades de sensor simulado y LED.
> – La **API** está documentada en `api.md` (endpoints, ejemplos cURL, esquema SQL).

---

## 🚀 Arduino/nRF52 (Emisor BLE)

* `Publicador.h`: codifica CO₂, temperatura o ruido en los campos `major`/`minor` y publica periódicamente.
* `Medidor.h`: simula sensores (valores fijos por defecto).
* `LED.h`: gestión de parpadeo LED según estado.
* `PuertoSerie.h`: salida de depuración.
* `EmisoraBLE.h`: configuración BLE (UUID, intervalos, TX power).
* `ServicioEnEmisora.h`: define características GATT opcionales.

**Codificación:**

* `major = (tipo << 8) + contador` donde `tipo` es:

  * `11` → CO₂
  * `12` → Temperatura
  * `13` → Ruido
* `minor` = valor medido (p. ej. ppm o °C)

El contador se reinicia en 0–255, por eso el valor total de `major` puede parecer “grande” (no es decimal directo).

---

## 📱 App Android (Java)

* **MainActivity.java**: gestión de permisos BLE, escaneo y control de estado (`escaneando`).
* **TramaIBeacon.java**: parseo de la trama iBeacon recibida.
* **Device.java**: modelo de datos del beacon detectado.
* **ApiService / ApiProvider**: cliente HTTP (Retrofit) que envía los datos al servidor.
* **FakeApiService.java**: simulación local para pruebas sin servidor.

**Flujo:**

1. Escanea beacons cercanos.
2. Parsea UUID, major, minor, RSSI.
3. Llama a `insert_device.php` (POST JSON).

---

## 🌐 Servidor PHP + MySQL

* `db.php` → conexión a base de datos.
* `insert_device.php` → recibe JSON y lo inserta (usa `PDO`).
* `dispositivos.php` → devuelve registros en JSON.
* `logica.php` → validación y transformaciones.

**Ejemplo SQL:**

```sql
CREATE TABLE dispositivos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  uuid VARCHAR(64) NOT NULL,
  major INT NOT NULL,
  minor INT NOT NULL,
  rssi INT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**Ejemplo cURL:**

```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"uuid":"E2C56DB5-DFFB-48D2-B060-D0F5A71096E0","major":11,"minor":500,"rssi":-53}' \
  https://<host>/api/insert_device.php
```

---

## 💻 Cliente Web (webcliente.html)

* Usa **Bootstrap 5** y `fetch()` para obtener datos.
* Soporta **dos formatos JSON**: array directo o `{ ok:true, rows:[...] }`.
* Fallback FAKE automático (si fetch falla o JSON inválido).
* Controles de interfaz:

  * **Refrescar** (actualiza datos)
  * **Forzar Real** / **Forzar Fake**
  * **Banner** amarillo de advertencia si está en modo FAKE
* Parámetro de URL: `?modo=real` o `?modo=fake`.

**Configuración:**
Editar la constante `ENDPOINT` dentro del script:

```js
const ENDPOINT = "https://tuservidor.com/servidor/dispositivos.php";
```

---

## 🚀 Puesta en marcha

### 1) Arduino

* Instala **Arduino IDE** y BSP Adafruit nRF52.
* Abre `HolaMundoIBeacon.ino`, selecciona la placa y sube.
* Verifica anuncios BLE con *nRF Connect*.

### 2) Servidor

* Instala **XAMPP/LAMP**.
* Crea base de datos y tabla `dispositivos`.
* Ajusta credenciales en `db.php`.
* Copia `servidor/` a `/htdocs/` o `/var/www/html/`.

### 3) Android

* Abre en Android Studio.
* Configura la URL base de la API.
* Concede permisos BLE/ubicación.
* Ejecuta en un móvil real.

### 4) Web

* Abre `cliente/webcliente.html`.
* Verifica carga de datos reales o modo FAKE.

---

## 🔍 Solución de problemas

| Problema                    | Causa / Solución                                                                              |
| --------------------------- | --------------------------------------------------------------------------------------------- |
| El `id` no empieza por 1    | Es autoincremental. Si hubo filas, reinicia con `ALTER TABLE dispositivos AUTO_INCREMENT=1;`. |
| `major` tiene valores altos | Contiene tipo (byte alto) + contador (byte bajo). No es decimal simple.                       |
| Web muestra banner amarillo | Fallback FAKE activo (timeout, CORS o JSON inválido). Revisa `ENDPOINT`.                      |
| BLE no se detiene bien      | Usa bandera `escaneando` y detén antes de reiniciar.                                          |

---

## 📝 Licencia

Proyecto distribuido bajo **MIT License** con fines educativos.

---

## 👤 Autores

Inspirado en **Jordi Bataller i Mascarell** y adaptado para un proyecto académico IoT.
