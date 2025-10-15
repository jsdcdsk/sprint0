<?php
// api/logica.php
// ------------------------------------------------------------
// Capa de lógica de negocio: valida los datos recibidos,
// aplica reglas básicas y gestiona la inserción en la BD.
// ------------------------------------------------------------

function guardarMedicion($pdo, $data) {

  // 1️⃣ Validación de campos obligatorios
  $required = ["nombre","mac","rssi","uuid","major","minor","txPower","timestamp"];
  foreach ($required as $k) {
    if (!isset($data[$k])) {
      return ["ok"=>false, "error"=>"MISSING_FIELD:$k"];
    }
  }

  try {
    // 2️⃣ Preparar la consulta SQL con parámetros nombrados
    $stmt = $pdo->prepare("
      INSERT INTO dispositivos
        (nombre, mac, rssi, uuid, major, minor, txPower, timestampMs)
      VALUES
        (:nombre, :mac, :rssi, :uuid, :major, :minor, :txPower, :timestampMs)
    ");

    // 3️⃣ Ejecutar la inserción con conversión de tipos
    $stmt->execute([
      ":nombre"      => substr($data["nombre"], 0, 100),
      ":mac"         => substr($data["mac"], 0, 17),
      ":rssi"        => (int)$data["rssi"],
      ":uuid"        => substr($data["uuid"], 0, 36),
      ":major"       => (int)$data["major"],
      ":minor"       => (int)$data["minor"],
      ":txPower"     => (int)$data["txPower"],
      ":timestampMs" => (int)$data["timestamp"]
    ]);

    // 4️⃣ Devolver respuesta de éxito
    return [
      "ok" => true,
      "id" => $pdo->lastInsertId(),
      "message" => "Dispositivo insertado correctamente"
    ];

  } catch (Exception $e) {
    // 5️⃣ Captura de errores de base de datos
    return [
      "ok" => false,
      "error" => "DB_INSERT_FAILED",
      "details" => $e->getMessage()
    ];
  }
}
