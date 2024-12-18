<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    // In a real-world scenario, you'd get the user ID from the session or a token
    // For this example, we'll just update the first user
    $username = $conn->real_escape_string($_POST['username']);
    $email = $conn->real_escape_string($_POST['email']);
    $photo_uri = isset($_POST['photo_uri']) ? $conn->real_escape_string($_POST['photo_uri']) : null;

    $sql = "UPDATE users SET username = ?, email = ?, photo_uri = ? WHERE id = 1";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("sss", $username, $email, $photo_uri);
    
    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "User updated successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Error: " . $stmt->error]);
    }
    
    $stmt->close();
} else {
    echo json_encode(["success" => false, "message" => "Invalid request method"]);
}

$conn->close();
?>

