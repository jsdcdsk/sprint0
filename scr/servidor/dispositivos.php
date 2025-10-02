<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=utf-8");

header('Content-Type: application/json; charset=utf-8');

require_once __DIR__ . '/api/db.php';

try {
    $stmt = $pdo->query("SELECT * FROM dispositivos ORDER BY id DESC LIMIT 200");
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "ok" => true,
        "rows" => $rows
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "ok" => false,
        "error" => $e->getMessage()
    ]);
}
