package com.example.deteccinderostros;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 111;
    private static final int REQUEST_GALLERY = 222;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView mImageView;
    private TextView txtResults;
    private Bitmap mSelectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtresults);

        Button btGallery = findViewById(R.id.btGallery);
        Button btCamera = findViewById(R.id.btCamera);
        Button btText = findViewById(R.id.btText);   // OCR (lo puedes implementar después)
        Button btFace = findViewById(R.id.btFace);   // Detección de rostros

        // Abrir galería
        btGallery.setOnClickListener(v -> abrirGaleria());

        // Abrir cámara
        btCamera.setOnClickListener(v -> abrirCamera());

        // Botón OCR (aquí puedes poner tu lógica de reconocimiento de texto con ML Kit)
        btText.setOnClickListener(v -> Toast.makeText(this, "OCR no implementado aún", Toast.LENGTH_SHORT).show());

        // Botón detección de rostros
        btFace.setOnClickListener(v -> {
            if (mSelectedImage != null) {
                detectarRostros(mSelectedImage);
            } else {
                Toast.makeText(this, "Primero selecciona una imagen", Toast.LENGTH_SHORT).show();
            }
        });

        // Solicitar permiso de cámara si no se ha concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        }
    }

    // Método para abrir la galería
    private void abrirGaleria() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    // Método para abrir la cámara
    private void abrirCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    // Manejar el resultado de la imagen seleccionada o tomada
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            try {
                if (requestCode == REQUEST_CAMERA) {
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == REQUEST_GALLERY) {
                    Uri imageUri = data.getData();
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                }
                mImageView.setImageBitmap(mSelectedImage);
                txtResults.setText("Imagen cargada");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para realizar la detección de rostros
    private void detectarRostros(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Opciones de detección de rostros
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        txtResults.setText("No hay rostros");
                    } else {
                        txtResults.setText("Hay " + faces.size() + " rostro(s)");

                        // Dibujar rectángulos alrededor de los rostros detectados
                        BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
                        Bitmap mutableBitmap = drawable.getBitmap()
                                .copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(5);
                        paint.setStyle(Paint.Style.STROKE);

                        for (Face face : faces) {
                            canvas.drawRect(face.getBoundingBox(), paint);
                        }

                        mImageView.setImageBitmap(mutableBitmap);
                    }
                })
                .addOnFailureListener(e -> txtResults.setText("Error al procesar la imagen"));
    }

    // Manejo de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
