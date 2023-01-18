package com.binbo.glvideo.core.camera.face;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.lang.ref.WeakReference;


public class FaceTracker {
    static {
        System.loadLibrary("native-lib");
    }

    private final static int MESSAGE_ID_DETECT_FACE = 100;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private WeakReference<FaceTrackObserver> mObserver;

    private long mSelf;
    //结果
    public volatile Face mFace;

    public FaceTracker(String model, String seeta, int width, int height) {
        mSelf = create(model, seeta);
        mHandlerThread = new HandlerThread("track");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // 子线程 耗时再久 也不会对其他地方 (如：opengl绘制线程) 产生影响
                synchronized (FaceTracker.this) {
                    switch (msg.what) {
                        case MESSAGE_ID_DETECT_FACE:
                            // 定位 线程中检测
                            mFace = detector(mSelf, (byte[]) msg.obj, msg.arg1, width, height);
                            if (mFace != null) {
                                if (mObserver != null && mObserver.get() != null) {
                                    mObserver.get().onFaceDetected(mFace);
                                }
                            }
                            break;
                    }
                }
            }
        };
    }

    public void setFaceTrackObserver(FaceTrackObserver observer) {
        mObserver = new WeakReference<>(observer);
    }

    public void startTrack() {
        start(mSelf);
    }

    public void stopTrack() {
        synchronized (this) {
            mHandlerThread.quitSafely();
            mHandler.removeCallbacksAndMessages(null);
            stop(mSelf);
            mSelf = 0;
        }
    }

    public void detector(byte[] data, int lensFacing) {
        mHandler.removeMessages(MESSAGE_ID_DETECT_FACE);
        Message message = mHandler.obtainMessage(MESSAGE_ID_DETECT_FACE);
        message.obj = data;
        message.arg1 = lensFacing;
        mHandler.sendMessage(message);
    }

    // 传入模型文件， 创建人脸识别追踪器和人眼定位器
    private native long create(String model, String seeta);

    // 开始追踪
    private native void start(long self);

    // 停止追踪
    private native void stop(long self);

    // 检测人脸
    private native Face detector(long self, byte[] data, int cameraId, int width, int height);
}
