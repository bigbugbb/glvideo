package com.binbo.glvideo.core.media.utils;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageHelper {
    public static String TAG = "ImageHelper";

    // 获取到YuvImage对象 然后存文件
//    public static void useYuvImgSaveFile(ImageProxy imageProxy, boolean outputYOnly) {
//        final int width = imageProxy.getWidth();
//        final int height = imageProxy.getHeight();
//        Log.d(TAG, "宽高: " + width + ", " + height);
//
//        YuvImage yuvImage = ImageHelper.toYuvImage(imageProxy);
//        File file = FileToolUtils.getFile(FileUseCase.TEST_ONLY, "z_" + System.currentTimeMillis() + ".png");
//        saveYuvToFile(file, width, height, yuvImage);
//        Log.d(TAG, "rustfisher.com 存储了" + file);
//
//        if (outputYOnly) { // 仅仅作为功能演示
//            YuvImage yImage = ImageHelper.toYOnlyYuvImage(imageProxy);
//            File yFile = FileToolUtils.getFile(FileUseCase.TEST_ONLY, "y_" + System.currentTimeMillis() + ".png");
//            saveYuvToFile(yFile, width, height, yImage);
//            Log.d(TAG, "rustfisher.com 存储了" + yFile);
//        }
//    }

    // 仅作为示例使用
    public static YuvImage toYOnlyYuvImage(ImageProxy imageProxy) {
        if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }
        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
        byte[] nv21 = new byte[width * height * 3 / 2];
        int index = 0;
        int yRowStride = imageProxy.getPlanes()[0].getRowStride();
        int yPixelStride = imageProxy.getPlanes()[0].getPixelStride();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                nv21[index++] = yBuffer.get(y * yRowStride + x * yPixelStride);
            }
        }
        return new YuvImage(nv21, ImageFormat.NV21, width, height, null);
    }

    public static YuvImage toYuvImage(ImageProxy image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }
        int width = image.getWidth();
        int height = image.getHeight();

        // 拿到YUV数据
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        byte[] nv21 = new byte[width * height * 3 / 2]; // 转换后的数据
        int index = 0;

        // 复制Y的数据
        int yRowStride = image.getPlanes()[0].getRowStride();
        int yPixelStride = image.getPlanes()[0].getPixelStride();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                nv21[index++] = yBuffer.get(y * yRowStride + x * yPixelStride);
            }
        }

        // 复制U/V数据
        int uvRowStride = image.getPlanes()[1].getRowStride();
        int uvPixelStride = image.getPlanes()[1].getPixelStride();
        int uvWidth = width / 2;
        int uvHeight = height / 2;

        for (int y = 0; y < uvHeight; ++y) {
            for (int x = 0; x < uvWidth; ++x) {
                int bufferIndex = (y * uvRowStride) + (x * uvPixelStride);
                nv21[index++] = vBuffer.get(bufferIndex);
                nv21[index++] = uBuffer.get(bufferIndex);
            }
        }
        return new YuvImage(nv21, ImageFormat.NV21, width, height, null);
    }

    public static void saveYuvToFile(File file, int width, int height, YuvImage yuvImage) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
