package com.example.ar;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

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

        Bitmap resizedBitmap =
                Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        ByteBuffer inputBuffer =
                ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);

        inputBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];

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

        float[][][][] output =
                new float[1][INPUT_SIZE][INPUT_SIZE][1];

        interpreter.run(inputBuffer, output);

        Bitmap result =
                resizedBitmap.copy(Bitmap.Config.ARGB_8888, true);

        for (int y = 0; y < INPUT_SIZE; y++) {

            for (int x = 0; x < INPUT_SIZE; x++) {

                float confidence = output[0][y][x][0];

                if (confidence > 0.5f) {

                    int pixel = result.getPixel(x, y);

                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    r = Math.min(255, r + 40);
                    g = Math.min(255, g + 40);

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