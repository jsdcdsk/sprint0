# Visión General

Este documento describe la **API del servidor independiente** (PHP) en
tu proyecto --- principalmente:

-   `POST /insert_device.php`: recibe los datos enviados por un
    dispositivo (iBeacon/sensor) y los guarda en la base de datos.
-   `GET /dispositivos.php`: consulta los dispositivos/mediciones
    guardados para que el cliente los muestre.

> Este documento incluye **ejemplos de peticiones/respuestas,
> descripción de campos, códigos de estado, formato de errores** y
> **pautas de implementación**. Se puede colocar en `doc/api.md` o
> integrarlo en `README.md`.

------------------------------------------------------------------------

## General

-   **Base URL**: según el despliegue real (por ejemplo
    `https://<tu-dominio-o-IP>/api/` o el directorio raíz del
    proyecto).\
-   **Codificación**: UTF-8.\
-   **Autenticación**: actualmente no habilitada (si es necesario, se
    puede añadir Token o autenticación básica).\
-   **Content-Type**:
    -   Se recomienda `application/json`.\
    -   También puede usarse `application/x-www-form-urlencoded` si PHP
        lee `$_POST`.\
-   **Zona horaria**: se sugiere UTC o la local, con formato explícito
    en las respuestas.\
-   **CORS**: habilitar encabezados si se accede desde otros orígenes.

------------------------------------------------------------------------

## Modelo de Datos (sugerencia)

-   `id`: clave primaria autoincremental.\
-   `uuid`: UUID del dispositivo/beacon (string).\
-   `major`: valor major del iBeacon o código del tipo de medición
    (11=CO2, 12=Temperatura).\
-   `minor`: valor minor o valor medido (ej. 500=ppm de CO2,
    10=temperatura).\
-   `rssi`: intensidad de señal (opcional).\
-   `created_at`: marca de tiempo del servidor.

------------------------------------------------------------------------

## POST /insert_device.php

Recibe datos de un dispositivo o gateway y los guarda en la base de
datos.

### Petición (JSON)

**Headers**

    Content-Type: application/json

**Body**

``` json
{
  "uuid": "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
  "major": 11,
  "minor": 500,
  "rssi": -53
}
```

### Respuesta exitosa

``` json
{ "status": "ok", "id": 25 }
```

### Respuesta de error

``` json
{ "status": "error", "code": "VALIDATION_ERROR", "message": "field 'uuid' is required" }
```

### Ejemplo cURL

``` bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"uuid":"E2C56DB5-DFFB-48D2-B060-D0F5A71096E0","major":11,"minor":500,"rssi":-53}' \
  https://<host>/insert_device.php
```

------------------------------------------------------------------------

## GET /dispositivos.php

Consulta los registros guardados.

### Parámetros opcionales

-   `limit`, `offset` (paginación)\
-   `uuid` (filtro por dispositivo)\
-   `major` (filtro por tipo de medición)\
-   `since`, `until` (rango de fechas)

### Respuesta JSON (ejemplo)

``` json
[
  { "id": 1, "uuid": "E2C56DB5...", "major": 11, "minor": 500, "rssi": -53, "created_at": "2025-10-03 10:00:00" },
  { "id": 2, "uuid": "E2C56DB5...", "major": 12, "minor": 10,  "rssi": -60, "created_at": "2025-10-03 10:05:00" }
]
```

------------------------------------------------------------------------

## Errores y Códigos de Estado

  --------------------------------------------------------------------------
  Caso         HTTP         `code`               Descripción
  ------------ ------------ -------------------- ---------------------------
  Éxito        200          *(ninguno)*          `status: ok`

  Faltan       400          `VALIDATION_ERROR`   Campos ausentes o inválidos
  parámetros                                     

  No           404          `NOT_FOUND`          Recurso no existe
  encontrado                                     

  Error del    500          `DB_ERROR`           Error de base de datos u
  servidor                                       otro fallo
  --------------------------------------------------------------------------

------------------------------------------------------------------------

## Seguridad y Buenas Prácticas

-   Validar entradas (`uuid`, `major`, `minor`, `rssi`).\
-   Usar consultas preparadas con PDO (`db.php`).\
-   Proteger con HTTPS en producción.\
-   (Opcional) añadir autenticación o limitación de peticiones.

------------------------------------------------------------------------

## Esquema de Base de Datos (ejemplo)

``` sql
CREATE TABLE dispositivos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  uuid VARCHAR(64) NOT NULL,
  major INT NOT NULL,
  minor INT NOT NULL,
  rssi INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

------------------------------------------------------------------------

## Implementación en PHP (resumen)

**insert_device.php**\
1. Leer JSON (`php://input`) o `$_POST`.\
2. Validar campos.\
3. Insertar con PDO (`db.php`).\
4. Responder con `status: ok` + `id`.

**dispositivos.php**\
1. Leer parámetros de filtro.\
2. Ejecutar SELECT con límites.\
3. `echo json_encode($rows);` con header
`Content-Type: application/json`.

------------------------------------------------------------------------

## CORS (si aplica)

``` php
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
```

------------------------------------------------------------------------

## Lista de Verificación

-   [ ] `insert_device.php` acepta JSON/form-data y guarda en BD\
-   [ ] Manejo uniforme de errores en JSON\
-   [ ] `dispositivos.php` devuelve JSON correctamente\
-   [ ] Ejemplos de uso documentados en README
