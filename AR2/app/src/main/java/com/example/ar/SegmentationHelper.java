package com.example.ar;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import android.util.Log;

public class SegmentationHelper {

    private Interpreter interpreter;

    private final int INPUT_SIZE = 256;

    public SegmentationHelper(Context context) throws IOException {

        Interpreter.Options options = new Interpreter.Options();

        interpreter = new Interpreter(
                loadModelFile(context),
                options
        );
    }

    private ByteBuffer loadModelFile(Context context) throws IOException {

        FileInputStream inputStream =
                new FileInputStream(
                        context.getAssets().openFd("selfie_segmenter.tflite").getFileDescriptor()
                );

        FileChannel fileChannel = inputStream.getChannel();

        long startOffset =
                context.getAssets().openFd("selfie_segmenter.tflite").getStartOffset();

        long declaredLength =
                context.getAssets().openFd("selfie_segmenter.tflite").getDeclaredLength();

        return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
        );
    }

    public Bitmap applyJaundiceFilter(Bitmap bitmap) {
        Log.d("SEGMENTATION", "Function started");

        Bitmap resizedBitmap =
                Bitmap.createScaledBitmap(
                        bitmap,
                        INPUT_SIZE,
                        INPUT_SIZE,
                        true
                );

        Bitmap result =
                resizedBitmap.copy(Bitmap.Config.ARGB_8888, true);

        ByteBuffer inputBuffer =
                ByteBuffer.allocateDirect(
                        4 * INPUT_SIZE * INPUT_SIZE * 3
                );

        inputBuffer.order(ByteOrder.nativeOrder());

        int[] pixels =
                new int[INPUT_SIZE * INPUT_SIZE];

        resizedBitmap.getPixels(
                pixels,
                0,
                INPUT_SIZE,
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
        );

        for (int pixel : pixels) {

            float r = ((pixel >> 16) & 0xFF) / 255f;
            float g = ((pixel >> 8) & 0xFF) / 255f;
            float b = (pixel & 0xFF) / 255f;

            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }

        inputBuffer.rewind();

        float[][][][] output =
                new float[1][256][256][1];

        interpreter.run(inputBuffer, output);

        float min = 999f;
        float max = -999f;

        for (int y = 0; y < 256; y++) {

            for (int x = 0; x < 256; x++) {

                float value = output[0][y][x][0];

                if (value < min) min = value;
                if (value > max) max = value;
            }
        }

        Log.d("SEGMENTATION", "MASK MIN = " + min);
        Log.d("SEGMENTATION", "MASK MAX = " + max);

        for (int y = 0; y < INPUT_SIZE; y++) {

            for (int x = 0; x < INPUT_SIZE; x++) {

                float confidence =
                        output[0][y][x][0];

                if (confidence > 0.1f) {

                    int pixel = result.getPixel(x, y);

                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    r = Math.min(255, r + 80);
                    g = Math.min(255, g + 80);

                    result.setPixel(
                            x,
                            y,
                            0xFF000000 | (r << 16) | (g << 8) | b
                    );
                }
            }
        }

        return result;
    }
}