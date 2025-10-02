<?php
// api/db.php
$host   = '127.0.0.1';              // 或 localhost，看哪个在你服务器正常
$dbname = 'wjin_datogas';
$user   = 'wjin';
$pass   = 'JINWEI666';

$dsn = "mysql:host=$host;dbname=$dbname;charset=utf8mb4"; // 如需端口: ;port=3306
$options = [
  PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
  PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
  PDO::ATTR_EMULATE_PREPARES   => false,
];

try {
  $pdo = new PDO($dsn, $user, $pass, $options);
} catch (Exception $e) {
  http_response_code(500);
  header('Content-Type: application/json; charset=utf-8');
  echo json_encode(["ok"=>false, "error"=>"DB_CONNECT_FAILED"]);
  exit;
}
