<?php
// api/insert_device.php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');          // 纯 App 调用，不涉及浏览器 CORS，可留
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type, X-API-KEY');

require_once __DIR__ . '/db.php';

function apilog($msg){
  $file = __DIR__ . '/../logs/api_insert.log'; // ../ 表示到 api 的上级目录
  @file_put_contents($file, date('c')." ".$msg."\n", FILE_APPEND);
}

apilog("METHOD=".$_SERVER['REQUEST_METHOD']);
$headers = function_exists('getallheaders') ? json_encode(getallheaders(), JSON_UNESCAPED_UNICODE) : '{}';
apilog("HEADERS=".$headers);
$raw = file_get_contents('php://input');
apilog("RAW=".$raw);

// 简单鉴权（可换成更安全方式）
$apiKey = "YOUR_SECURE_RANDOM_KEY";
if (!isset($_SERVER['HTTP_X_API_KEY']) || $_SERVER['HTTP_X_API_KEY'] !== $apiKey) {
  http_response_code(401);
  echo json_encode(["ok"=>false, "error"=>"UNAUTHORIZED"]);
  exit;
}

$raw = file_get_contents("php://input");
$data = json_decode($raw, true);
if (!$data) {
  http_response_code(400);
  echo json_encode(["ok"=>false, "error"=>"INVALID_JSON"]);
  exit;
}

$required = ["nombre","mac","rssi","uuid","major","minor","txPower","timestamp"];
foreach ($required as $k) {
  if (!isset($data[$k])) {
    http_response_code(422);
    echo json_encode(["ok"=>false, "error"=>"MISSING_FIELD:$k"]);
    exit;
  }
}

try {
  $stmt = $pdo->prepare("
    INSERT INTO dispositivos
      (nombre, mac, rssi, uuid, major, minor, txPower, timestampMs)
    VALUES
      (:nombre, :mac, :rssi, :uuid, :major, :minor, :txPower, :timestampMs)
  ");
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

  echo json_encode(["ok"=>true, "id"=>$pdo->lastInsertId()]);
} catch (Exception $e) {
  http_response_code(500);
  echo json_encode(["ok"=>false, "error"=>"DB_INSERT_FAILED"]);
}
