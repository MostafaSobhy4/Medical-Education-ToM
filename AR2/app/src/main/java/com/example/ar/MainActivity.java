package com.example.ar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;


import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;


public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageView overlayView;

    private SegmentationHelper segmentationHelper;

    private final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);

        try {
            segmentationHelper = new SegmentationHelper(this);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("MODEL FAILED");
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE
            );
        }
    }

    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {

            try {

                ProcessCameraProvider cameraProvider =
                        cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(256, 256))
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .build();

                imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        image -> processImage(image)
                );

                CameraSelector cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            }
            catch (ExecutionException | InterruptedException e) {

                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(ImageProxy image) {
        Log.d("CAMERA", "Frame received");

        Bitmap bitmap = imageProxyToBitmap(image);

        if (bitmap != null && segmentationHelper != null) {

            Bitmap result =
                    segmentationHelper.applyJaundiceFilter(bitmap);

            overlayView.setImageBitmap(result);
        }

        image.close();
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {

        try {

            ByteBuffer yBuffer =
                    image.getPlanes()[0].getBuffer();

            ByteBuffer uBuffer =
                    image.getPlanes()[1].getBuffer();

            ByteBuffer vBuffer =
                    image.getPlanes()[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(
                    nv21,
                    ImageFormat.NV21,
                    image.getWidth(),
                    image.getHeight(),
                    null
            );

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            yuvImage.compressToJpeg(
                    new Rect(
                            0,
                            0,
                            image.getWidth(),
                            image.getHeight()
                    ),
                    100,
                    out
            );

            byte[] imageBytes = out.toByteArray();

            Bitmap bitmap =
                    BitmapFactory.decodeByteArray(
                            imageBytes,
                            0,
                            imageBytes.length
                    );

            return bitmap;

        }
        catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startCamera();
        }
    }
}