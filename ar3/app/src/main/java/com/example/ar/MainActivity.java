package com.example.ar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Bundle;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.nio.ByteBuffer;

import com.google.common.util.concurrent.ListenableFuture;

import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter;
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private GLView glView;
    private SurfaceTexture pendingSurfaceTexture;

    private ExecutorService analysisExecutor;

    private ImageSegmenter segmenter;


    private Bitmap latestFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glView = findViewById(R.id.glView);
        glView.getRenderer().setFilterMode(1);

        glView.getRenderer().setGLReadyCallback(surfaceTexture -> {
            runOnUiThread(() -> {
                pendingSurfaceTexture = surfaceTexture;
                startCamera();
            });
        });

        analysisExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        }

        // ===================== MEDIA PIPE SETUP =====================

        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("selfie_segmenter.tflite")
                .build();

        ImageSegmenter.ImageSegmenterOptions options =
                ImageSegmenter.ImageSegmenterOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setRunningMode(RunningMode.LIVE_STREAM)
                        .setOutputCategoryMask(true)
                        .setOutputConfidenceMasks(false)
                        .setResultListener(this::onSegmentationResult)
                        .setErrorListener(e -> android.util.Log.e("MP", e.getMessage()))
                        .build();

        segmenter = ImageSegmenter.createFromOptions(this, options);
    }

    // ============================================================
    // CAMERA
    // ============================================================

    private void startCamera() {

        if (pendingSurfaceTexture == null) return;

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {

                ProcessCameraProvider cameraProvider = future.get();

                pendingSurfaceTexture.setDefaultBufferSize(1920, 1080);
                Surface surface = new Surface(pendingSurfaceTexture);

                Preview preview = new Preview.Builder().build();

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build();

                imageAnalysis.setAnalyzer(
                        analysisExecutor,
                        this::analyzeFrame
                );

                preview.setSurfaceProvider(request ->
                        request.provideSurface(
                                surface,
                                ContextCompat.getMainExecutor(this),
                                result -> {}
                        )
                );

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                );

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    // ============================================================
    // FAST ANALYSIS (NO BITMAP)
    // ============================================================

    private void analyzeFrame(ImageProxy image) {

        try {

            Bitmap bitmap = image.toBitmap();

            latestFrame = bitmap; // ✅ STORE FRAME FOR SKIN MASK

            MPImage mpImage =
                    new BitmapImageBuilder(bitmap).build();

            segmenter.segmentAsync(
                    mpImage,
                    System.currentTimeMillis()
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            image.close();
        }
    }

    // ============================================================
    // SEGMENTATION RESULT
    // ============================================================

    private float[] computeSkinMask(Bitmap frame) {

        int w = frame.getWidth();
        int h = frame.getHeight();

        int[] pixels = new int[w * h];
        frame.getPixels(pixels, 0, w, 0, 0, w, h);

        float[] skin = new float[w * h];

        for (int i = 0; i < pixels.length; i++) {

            int c = pixels[i];

            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            // Improved YCrCb skin heuristic (MUCH more stable than HSV)
            float y  = 0.299f * r + 0.587f * g + 0.114f * b;
            float cr = (r - y) * 0.713f;
            float cb = (b - y) * 0.564f;

            boolean isSkin =
                    (r > 95) &&
                            (g > 40) &&
                            (b > 20) &&
                            (Math.max(r, Math.max(g, b)) -
                                    Math.min(r, Math.min(g, b)) > 15) &&
                            (Math.abs(r - g) > 15) &&
                            (r > g) &&
                            (r > b);

            skin[i] = isSkin ? 0.7f : 0.0f;
        }

        return skin;
    }

    private float[] previousMask;

    private void onSegmentationResult(ImageSegmenterResult result, MPImage input) {

        if (result == null || result.categoryMask().isEmpty()) return;
        if (latestFrame == null) return;

        MPImage mask = result.categoryMask().get();

        int width = mask.getWidth();
        int height = mask.getHeight();
        int size = width * height;

        ByteBuffer buffer =
                com.google.mediapipe.framework.image.ByteBufferExtractor.extract(mask);

        buffer.rewind();

        float[] personMask = new float[size];

        for (int i = 0; i < size; i++) {

            personMask[i] =
                    ((buffer.get() & 0xFF) > 128)
                            ? 1.0f
                            : 0.0f;
        }

        // ---------------------------------
        // OPTIONAL SKIN MASK
        // ---------------------------------

        Bitmap scaledFrame = Bitmap.createScaledBitmap(
                latestFrame,
                width,
                height,
                true
        );

        float[] skinMask = computeSkinMask(scaledFrame);

        // ---------------------------------
        // COMBINE MASKS
        // ---------------------------------

        float[] combined = new float[size];

        for (int i = 0; i < size; i++) {

            float p = personMask[i];
            float s = skinMask[i];

            combined[i] = (0.85f * p) + (0.15f * s);

            combined[i] =
                    combined[i] > 0.45f
                            ? 1f
                            : 0f;
        }

        // ---------------------------------
        // TEMPORAL SMOOTHING
        // ---------------------------------

        if (previousMask == null || previousMask.length != size) {
            previousMask = new float[size];
        }

        float[] smoothed = new float[size];

        for (int i = 0; i < size; i++) {

            float prev = previousMask[i];
            float current = combined[i];

            float value =
                    (0.75f * current) +
                            (0.25f * prev);

            smoothed[i] = value;
            previousMask[i] = value;
        }

        // ---------------------------------
        // BLUR
        // ---------------------------------

        float[] blurred = new float[size];

        for (int y = 1; y < height - 1; y++) {

            for (int x = 1; x < width - 1; x++) {

                int idx = y * width + x;

                float sum = 0f;

                for (int ky = -1; ky <= 1; ky++) {

                    for (int kx = -1; kx <= 1; kx++) {

                        int ni =
                                (y + ky) * width +
                                        (x + kx);

                        sum += smoothed[ni];
                    }
                }

                blurred[idx] = sum / 9f;
            }
        }

        // ---------------------------------
        // FINAL PIXELS
        // ---------------------------------

        int[] pixels = new int[size];

        for (int i = 0; i < size; i++) {

            int v = (int)(blurred[i] * 255f);

            pixels[i] =
                    (0xFF << 24) |
                            (v << 16) |
                            (v << 8) |
                            v;
        }

        // ---------------------------------
        // DEBUG
        // ---------------------------------

        int active = 0;

        for (float v : combined) {

            if (v > 0.5f) {
                active++;
            }
        }

        android.util.Log.d(
                "MASK_STATS",
                "active pixels = " + active + " / " + size
        );

        // ---------------------------------
        // CREATE MASK BITMAP
        // ---------------------------------

        Bitmap rawMask = Bitmap.createBitmap(
                pixels,
                width,
                height,
                Bitmap.Config.ARGB_8888
        );

        // ---------------------------------
        // FLIP VERTICALLY
        // ---------------------------------

        android.graphics.Matrix matrix =
                new android.graphics.Matrix();

        matrix.postScale(1f, -1f);
        matrix.postTranslate(0f, height);

        Bitmap maskBitmap = Bitmap.createBitmap(
                rawMask,
                0,
                0,
                width,
                height,
                matrix,
                true
        );

        // ---------------------------------
        // SEND TO OPENGL
        // ---------------------------------

        glView.queueEvent(() ->
                glView.getRenderer().updateMask(maskBitmap)
        );
    }

    // ============================================================
    // PERMISSION CALLBACK
    // ============================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (pendingSurfaceTexture != null) {
                startCamera();
            }
        }
    }
}