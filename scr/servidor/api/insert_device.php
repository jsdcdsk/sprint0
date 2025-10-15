<?php
// api/insert_device.php
// ------------------------------------------------------------
// Capa de API: recibe la solicitud HTTP, valida cabeceras,
// convierte JSON y llama a la lógica de negocio.
// ------------------------------------------------------------

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');          
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type, X-API-KEY');

require_once __DIR__ . '/db.php';      // Capa de acceso a datos
require_once __DIR__ . '/logica.php';  // Capa de lógica de negocio

// ------------------------------------------------------------
// Función auxiliar para registrar logs en el servidor
// ------------------------------------------------------------
function apilog($msg){
  $file = __DIR__ . '/../logs/api_insert.log';
  @file_put_contents($file, date('c')." ".$msg."\n", FILE_APPEND);
}

// Registrar datos básicos de la solicitud
apilog("METHOD=".$_SERVER['REQUEST_METHOD']);
$raw = file_get_contents('php://input');
apilog("RAW=".$raw);

// ------------------------------------------------------------
// Autenticación sencilla mediante API Key
// ------------------------------------------------------------
$apiKey = "YOUR_SECURE_RANDOM_KEY"; // Sustituir por una clave real
if (!isset($_SERVER['HTTP_X_API_KEY']) || $_SERVER['HTTP_X_API_KEY'] !== $apiKey) {
  http_response_code(401);
  echo json_encode(["ok"=>false, "error"=>"UNAUTHORIZED"]);
  exit;
}

// ------------------------------------------------------------
// Conversión del cuerpo JSON a array PHP
// ------------------------------------------------------------
$data = json_decode($raw, true);
if (!$data) {
  http_response_code(400);
  echo json_encode(["ok"=>false, "error"=>"INVALID_JSON"]);
  exit;
}

// ------------------------------------------------------------
// Llamada a la capa de lógica de negocio
// ------------------------------------------------------------
$resultado = guardarMedicion($pdo, $data);

// ------------------------------------------------------------
// Respuesta final al cliente Android
// ------------------------------------------------------------
if ($resultado["ok"]) {
  http_response_code(201);
} else {
  http_response_code(422);
}
echo json_encode($resultado);
