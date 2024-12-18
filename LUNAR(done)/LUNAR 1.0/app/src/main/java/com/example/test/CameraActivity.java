package com.example.test;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private ImageView objectImage;
    private TextView labelText;
    private Button captureImgBtn;
    private ImageLabeler imageLabeler;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera); // Pastikan ini adalah layout yang benar

        objectImage = findViewById(R.id.objectImage);
        labelText = findViewById(R.id.labelText);
        captureImgBtn = findViewById(R.id.captureImgBtn);

        checkCameraPermission();

        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = extras != null ? (Bitmap) extras.get("data") : null;

                        if (imageBitmap != null) {
                            objectImage.setImageBitmap(imageBitmap);
                            labelImage(imageBitmap);  // Proses pelabelan gambar
                        } else {
                            labelText.setText("Unable to capture image");
                        }
                    }
                }
        );

        captureImgBtn.setOnClickListener(view -> {
            Intent clickPicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (clickPicture.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(clickPicture);
            }
        });
    }

    private void labelImage(Bitmap bitmap) {
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

        imageLabeler.process(inputImage)
                .addOnSuccessListener(this::displayLabel)
                .addOnFailureListener(e -> {
                    labelText.setText("Error: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    private void displayLabel(@NonNull List<ImageLabel> labels) {
        if (!labels.isEmpty()) {
            ImageLabel mostConfidentLabel = labels.get(0);
            labelText.setText(mostConfidentLabel.getText());
        } else {
            labelText.setText("No labels found");
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.CAMERA},
                    1
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan, lanjutkan
            } else {
                labelText.setText("Camera permission is required to use this feature.");
            }
        }
    }
}
