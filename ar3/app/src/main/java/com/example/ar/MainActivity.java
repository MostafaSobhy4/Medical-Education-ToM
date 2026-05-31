package com.example.ar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private GLView glView;
    private Button btnJaundice, btnRash;
    private SurfaceTexture glSurfaceTexture;
    private Surface cameraSurface;
    private ExecutorService executor;
    private FaceLandmarker faceLandmarker;

    // MediaPipe Face Mesh Landmark Indices
    private static final int LEFT_CHEEK  = 234;
    private static final int RIGHT_CHEEK = 454;
    private static final int NOSE_TIP    = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glView      = findViewById(R.id.glView);
        btnJaundice = findViewById(R.id.btnJaundice);
        btnRash     = findViewById(R.id.btnLupus);

        btnJaundice.setOnClickListener(v -> glView.setFilterMode(1));
        btnRash.setOnClickListener(v     -> glView.setFilterMode(3));

        executor = Executors.newSingleThreadExecutor();

        glView.setGLReadyCallback(surfaceTexture -> {
            glSurfaceTexture = surfaceTexture;
            runOnUiThread(this::checkPermission);
        });
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            init();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code,
                                           @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (code == 100 && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED)
            init();
        else
            Toast.makeText(this, "Camera permission needed", Toast.LENGTH_LONG).show();
    }

    private void init() {
        initFaceLandmarker();
        startCamera();
    }

    // ── MediaPipe Face Landmarker ─────────────────────────────────────────────

    private void initFaceLandmarker() {
        try {
            BaseOptions base = BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build();

            FaceLandmarker.FaceLandmarkerOptions opts =
                    FaceLandmarker.FaceLandmarkerOptions.builder()
                            .setBaseOptions(base)
                            .setRunningMode(RunningMode.LIVE_STREAM)
                            .setNumFaces(1)
                            .setResultListener(this::onFaceResult)
                            .setErrorListener(e -> e.printStackTrace())
                            .build();

            faceLandmarker = FaceLandmarker.createFromOptions(this, opts);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "FaceLandmarker init failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void onFaceResult(FaceLandmarkerResult result, MPImage input) {
        if (result == null || result.faceLandmarks().isEmpty()) return;

        List<NormalizedLandmark> lm = result.faceLandmarks().get(0);
        if (lm.size() <= RIGHT_CHEEK) return;

        NormalizedLandmark leftCheek  = lm.get(LEFT_CHEEK);
        NormalizedLandmark rightCheek = lm.get(RIGHT_CHEEK);
        NormalizedLandmark nose       = lm.get(NOSE_TIP);

        // Flip X for front camera (mirror)
        float lcX = 1f - leftCheek.x();
        float lcY = leftCheek.y();
        float rcX = 1f - rightCheek.x();
        float rcY = rightCheek.y();
        float nX  = 1f - nose.x();
        float nY  = nose.y();

        glView.updateFaceLandmarks(lcX, lcY, rcX, rcY, nX, nY);
    }

    // ── CameraX ───────────────────────────────────────────────────────────────

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                provider.unbindAll();

                cameraSurface = new Surface(glSurfaceTexture);

                // ── Preview: maximum supported resolution for front camera ──
                // 4K UHD (3840×2160) — CameraX will fall back gracefully to the
                // highest resolution the sensor actually supports (typically
                // 1080p or 1440p on most front cameras).
                Preview preview = new Preview.Builder()
                        .setTargetResolution(new Size(2160, 3840))
                        .build();
                preview.setSurfaceProvider(request ->
                        request.provideSurface(
                                cameraSurface,
                                ContextCompat.getMainExecutor(this),
                                SurfaceRequest.Result::getResultCode));

                // ── Analysis: 1080p for landmark detection accuracy ──────────
                // Higher than before (was 720p) → finer landmark placement,
                // less jitter, and sharper rash boundaries on dense displays.
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1080, 1920))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(executor, this::analyzeFrame);

                provider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview, analysis);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Camera error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeFrame(ImageProxy proxy) {
        if (faceLandmarker == null) {
            proxy.close();
            return;
        }
        try {
            android.graphics.Bitmap bmp = proxy.toBitmap();
            MPImage mp = new BitmapImageBuilder(bmp).build();
            faceLandmarker.detectAsync(mp, proxy.getImageInfo().getTimestamp());
            bmp.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            proxy.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
        if (faceLandmarker != null) faceLandmarker.close();
        if (cameraSurface != null) cameraSurface.release();
    }
}