package com.example.alpacasclassmobilenetv2.helpers;

import static java.lang.Math.min;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;


import com.example.alpacasclassmobilenetv2.ml.AlpacasModelLite;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class ClassifierHelper {

    public static int imageSize = 512;

    private final String TAG = "classifier";

    private static AlpacasModelLite _model;

    private final static String[] classes = new String[] { "Alpaca", "Not alpaca" };

    public void setupModel(Context context) {
        Model.Options options;
        options = new Model.Options.Builder().setNumThreads(4).build();
        try {
            _model = AlpacasModelLite.newInstance(context, options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearModel(Context context) {
        _model.close();
        _model = null;
    }

    public static Map<String, Double> classifyFrames(Context context, Bitmap bitmap) {
        Map<String, Double> result = new HashMap<>();

        String label = "";
        double probability;

        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
        ByteBuffer bytebuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
        bytebuffer.order(ByteOrder.nativeOrder());


        int[] intValues = new int[imageSize * imageSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for(int i = 0; i < imageSize; i ++){
            for(int j = 0; j < imageSize; j++){
                int val = intValues[pixel++]; // RGB
                bytebuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                bytebuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                bytebuffer.putFloat((val & 0xFF) * (1.f / 1));
            }
        }

        inputFeature0.loadBuffer(bytebuffer);

        AlpacasModelLite.Outputs outputs = _model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        float[] confidences = outputFeature0.getFloatArray();

        if (confidences[0] >= 0.5) {
            label = classes[1];
            probability = (confidences[0] - 0.5) * 2;

        } else {
            label = classes[0];
            probability = (0.5 - confidences[0]) * 2;
        }
        probability = (double) Math.round(probability * 10000) / 10000;

        result.put(label, probability);
        return result;
    }

    public static Map<String, Double> classifyImage(Context context, Bitmap bitmap) {

        Map<String, Double> result = new HashMap<>();

        String label = "";
        double probability;

        Model.Options options;
        options = new Model.Options.Builder().setNumThreads(4).build();
        try {
            _model = AlpacasModelLite.newInstance(context, options);
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
            ByteBuffer bytebuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            bytebuffer.order(ByteOrder.nativeOrder());


            int[] intValues = new int[imageSize * imageSize];
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            int pixel = 0;
            for(int i = 0; i < imageSize; i ++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    bytebuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    bytebuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    bytebuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }

            inputFeature0.loadBuffer(bytebuffer);

            AlpacasModelLite.Outputs outputs = _model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

            if (confidences[0] >= 0.5) {
                label = classes[1];
            } else {
                label = classes[0];
            }
//            probability = (double) Math.round(probability * 10000) / 10000;
            probability = Math.round(confidences[0] * 100) / 100.;

            result.put(label, probability);
            _model.close();

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Bitmap preprocessBitmap(Context context, Uri uri, Bitmap bp) {

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(getDriveFilePath(uri, context));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int rotationInDegrees = exifToDegrees(rotation);
        Matrix matrix = new Matrix();
        if (rotation != 0) {
            matrix.preRotate(rotationInDegrees);
        }
        int width = bp.getWidth();
        int height = bp.getHeight();

        Bitmap adjustedBitmap = Bitmap.createBitmap(bp, 0, 0, width, height, matrix, true);

        return Bitmap.createScaledBitmap(adjustedBitmap, 512, 512, true);
    }

    public static Bitmap preprocessBitmap(Bitmap bp) {

        Matrix matrix = new Matrix();
        matrix.preRotate(90);

        int width = bp.getWidth();
        int height = bp.getHeight();

        Bitmap adjustedBitmap = Bitmap.createBitmap(bp, 0, 0, width, height, matrix, true);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(adjustedBitmap, 512, 512, true);

        return resizedBitmap;
    }


    public static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    public static String getDriveFilePath(Uri uri, Context context) {
        Uri returnUri = uri;
        Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getCacheDir(), name);

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            Log.e("userInt", e.getMessage());
        }

        return file.getPath();

    }

    public static Bitmap loadBitmap(Uri uri, Context context) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


}
