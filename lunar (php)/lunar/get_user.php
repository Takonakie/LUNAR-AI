<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    // In a real-world scenario, you'd get the user ID from the session or a token
    // For this example, we'll just get the first user
    $sql = "SELECT id, username, email, photo_uri FROM users LIMIT 1";
    $result = $conn->query($sql);

    if ($user = $result->fetch_assoc()) {
        echo json_encode([
            "success" => true,
            "user" => [
                "id" => $user['id'],
                "username" => $user['username'],
                "email" => $user['email'],
                "photo_uri" => $user['photo_uri']
            ]
        ]);
    } else {
        echo json_encode(["success" => false, "message" => "User not found"]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Invalid request method"]);
}

$conn->close();
?>

