package com.binbo.glvideo.core.media.recorder;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.binbo.glvideo.core.media.config.EncoderSurfaceRenderConfig;
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * RenderRunnable
 */
public final class RecorderGLRender implements Runnable {

    private static final String TAG = RecorderGLRender.class.getSimpleName();

    private final Object mSync = new Object();

    private FrameDrawer mFrameDrawer;
    // 纹理id
    private BlockingDeque<TextureToRecord> mTextures = new LinkedBlockingDeque<>(36);

    private Size mTextureSize;
    /**
     * 由MediaCodec创建的输入surface
     */
    private Object mRecorderSurface;

    private EncoderSurfaceRenderConfig mConfig;

    private RecorderEGLManager mRecorderEglManager;

    private boolean mRequestSetEglContext;

    // 是否需要释放资源
    private boolean mRequestRelease;

    // 需要绘制的次数
    private int mRequestDraw;

    /**
     * 创建线程,开启这个Runable
     *
     * @param name
     * @return
     */
    public static RecorderGLRender createHandler(final String name) {

        final RecorderGLRender handler = new RecorderGLRender();
        synchronized (handler.mSync) {
            new Thread(handler, !TextUtils.isEmpty(name) ? name : TAG).start();
            try {
                handler.mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
        return handler;
    }

    private RecorderGLRender() {
    }

    /**
     * 开始录制时，调用该方法,设置一些数据
     *
     * @param recorderSurface
     * @param config
     */
    public void setup(final Object recorderSurface, final EncoderSurfaceRenderConfig config) {
        //
        if (!(recorderSurface instanceof Surface) && !(recorderSurface instanceof SurfaceTexture) && !(recorderSurface instanceof SurfaceHolder)) {
            throw new RuntimeException("unsupported window type:" + recorderSurface);
        }
        //
        synchronized (mSync) {
            // 释放资源
            if (mRequestRelease) {
                return;
            }
            //
            mRecorderSurface = recorderSurface;
            mConfig = config;
            //
            mRequestSetEglContext = true;
            //
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public EGLContext getEglContext() {
        if (mConfig != null) {
            return mConfig.getEglContext();
        }
        return EGL14.EGL_NO_CONTEXT;
    }

    /**
     * 运行在GLThread
     *
     * @param textureToRecord textureId and pts
     */
    public boolean draw(final TextureToRecord textureToRecord, final Size textureSize) {
        boolean result;
        synchronized (mSync) {
            // 释放资源
            if (mRequestRelease) {
                return false;
            }
            //
            result = mTextures.offerLast(textureToRecord);
            if (result) {
                mTextureSize = textureSize;
                mRequestDraw++;
            }
            mSync.notifyAll();
        }
        return result;
    }

    /**
     * 释放资源
     */
    public void release() {
        synchronized (mSync) {
            if (mRequestRelease) {
                return;
            }
            mRequestRelease = true;
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        synchronized (mSync) {
            mRequestSetEglContext = mRequestRelease = false;
            mRequestDraw = 0;
            mSync.notifyAll();
        }
        boolean localRequestDraw;
        int frames = 0;
        // 无限循环
        for (; ; ) {
            //
            synchronized (mSync) {
                // 是否需要释放资源
                if (mRequestRelease) {
                    break;
                }
                //
                if (mRequestSetEglContext) {
                    mRequestSetEglContext = false;
                    internalPrepare();
                }
                localRequestDraw = mRequestDraw > 0;
                if (localRequestDraw) {
                    mRequestDraw--;
                }
            }
            if (localRequestDraw) {
                if (mRecorderEglManager != null && !mTextures.isEmpty()) {
                    try {
                        TextureToRecord textureToRecord = mTextures.pollFirst();
                        // 清屏颜色为黑色
                        GLES20.glClearColor(0f, 0f, 0f, 0f);
                        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

                        mFrameDrawer.setFrameSize(mTextureSize.getWidth(), mTextureSize.getHeight());
                        mFrameDrawer.setViewportSize(mConfig.getWidth(), mConfig.getHeight());
                        mFrameDrawer.setTextureID(textureToRecord.getTextureId());
                        mFrameDrawer.draw();

//                        Bitmap bitmap = OpenGLUtils.savePixels(0, 0, mConfig.getWidth(), mConfig.getHeight());
//                        bitmap.recycle();

                        long pts = frames++ * (1000000000L / mConfig.getFrameRate());
                        if (textureToRecord.getPts() != -1) {
                            pts = textureToRecord.getPts();
                        }
//                    LogUtil.d(TAG, String.format("setPresentationTime i=%d pts=%ld", i, pts));
                        mRecorderEglManager.setPresentationTime(pts);
                        mRecorderEglManager.swapMyEGLBuffers();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                //--------进入等待状态-----------
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
        synchronized (mSync) {
            mRequestRelease = true;
            releaseEGL();
            mSync.notifyAll();
        }
    }

    private void internalPrepare() {
        //
        releaseEGL();
        //
        EGLContext eglContext = getEglContext();
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.d(TAG, "shit");
        }
        mRecorderEglManager = new RecorderEGLManager(eglContext, mRecorderSurface);
        mFrameDrawer = new FrameDrawer();
        mFrameDrawer.onWorldCreated();
        //
        mRecorderSurface = null;
        mSync.notifyAll();
    }

    /**
     *
     */
    private void releaseEGL() {
        if (mFrameDrawer != null) {
            mFrameDrawer.release();
            mFrameDrawer = null;
        }
        if (mRecorderEglManager != null) {
            mRecorderEglManager.release();
            mRecorderEglManager = null;
        }
    }

}
