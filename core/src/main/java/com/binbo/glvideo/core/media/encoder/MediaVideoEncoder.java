package com.binbo.glvideo.core.media.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.binbo.glvideo.core.media.BaseMediaEncoder;
import com.binbo.glvideo.core.media.config.EncoderSurfaceRenderConfig;
import com.binbo.glvideo.core.media.config.MediaEncoderConfig;
import com.binbo.glvideo.core.media.muxer.MediaMuxerManager;
import com.binbo.glvideo.core.media.recorder.RecorderGLRender;
import com.binbo.glvideo.core.media.recorder.TextureToRecord;

import java.io.IOException;

public class MediaVideoEncoder extends BaseMediaEncoder {

    private static final String TAG = MediaVideoEncoder.class.getSimpleName();

    private static final String MIME_TYPE = "video/avc";

    //
    private static final float BPP = 0.25f;

    private RecorderGLRender mRender;

    // 由MediaCodec创建的输入surface
    private Surface mMediaCodecSurface;

    /**
     * 构造方法,父类中，开启了该线程
     *
     * @param mediaMuxerManager
     * @param mediaEncoderListener
     * @param config
     */
    public MediaVideoEncoder(
            final MediaMuxerManager mediaMuxerManager,
            final MediaEncoderListener mediaEncoderListener,
            final MediaEncoderConfig config) {
        super(mediaMuxerManager, mediaEncoderListener, config);
        Log.i(TAG, "MediaVideoEncoderRunable: ");

        /**
         * 开启了一个看不到的绘制线程
         */
        mRender = RecorderGLRender.createHandler(TAG);
    }

    /**
     * 运行在GLThread
     *
     * @param textureToRecord 要渲染的纹理
     * @param textureSize     纹理的尺寸
     * @return
     */
    public boolean frameAvailableSoon(TextureToRecord textureToRecord, final Size textureSize) {
        Log.d(TAG, "---frameAvailableSoon---");
        boolean result;
        if (result = super.frameAvailableSoon()) {
            result = mRender.draw(textureToRecord, textureSize);
        }
        return result;
    }


    /**
     * 开始录制前的准备(目前由XMediaMuxerManager在主线程调用)
     *
     * @throws IOException
     */
    @Override
    public void prepare() throws IOException {
        Log.d(TAG, "---prepare---");
        //
        mTrackIndex = -1;
        //
        mMuxerStarted = mIsEndOfStream = false;

        //-----------------MediaFormat-----------------------
        // mediaCodec采用的是H.264编码
        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mConfig.getWidth(), mConfig.getHeight());
        // 数据来源自surface
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 视频码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.getVideoBitRate());
        // fps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mConfig.getFrameRate());
        // 设置关键帧的时间
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mConfig.getKeyFrameInterval());

        //-----------------Encoder-----------------------
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mMediaCodecSurface = mMediaCodec.createInputSurface();
        //
        mMediaCodec.start();
        //
        Log.i(TAG, "prepare finishing");
        if (mMediaEncoderListener.get() != null) {
            try {
                mMediaEncoderListener.get().onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    /**
     * 运行在GLThread中
     *
     * @param config 用于surface模式，设置encoder内部RecorderGLRender的参数
     */
    public void setupSurfaceRender(final EncoderSurfaceRenderConfig config) {
        if (getRecorderGLRender().getEglContext() != config.getEglContext()) {
            config.setWidth(mConfig.getWidth());
            config.setHeight(mConfig.getHeight());
            config.setFrameRate(mConfig.getFrameRate());
            mRender.setup(mMediaCodecSurface, config);
        }
    }

    @Override
    public void release() {
        Log.i(TAG, "release:");
        if (mMediaCodecSurface != null) {
            mMediaCodecSurface.release();
            mMediaCodecSurface = null;
        }
        if (mRender != null) {
            mRender.release();
            mRender = null;
        }
        super.release();
    }

    /**
     * 码率
     *
     * @return
     */
    private int calcBitRate() {
        //final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        final int bitrate = 800000;
        return bitrate;
    }

    @Override
    public void signalEndOfInputStream() {
        Log.d(TAG, "sending EOS to encoder");
        // 停止录制
        mMediaCodec.signalEndOfInputStream();
        //
        mIsEndOfStream = true;
    }

    public RecorderGLRender getRecorderGLRender() {
        return mRender;
    }
}
