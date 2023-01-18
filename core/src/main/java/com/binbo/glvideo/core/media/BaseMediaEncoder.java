package com.binbo.glvideo.core.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.binbo.glvideo.core.media.config.MediaEncoderConfig;
import com.binbo.glvideo.core.media.muxer.MediaMuxerManager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * 视频与音频录制的基类
 */
public abstract class BaseMediaEncoder implements Runnable {

    private static final String TAG = BaseMediaEncoder.class.getSimpleName();

    // 10[msec]
    protected static final int TIMEOUT_USEC = 10000;

    /**
     *
     */
    public interface MediaEncoderListener {
        void onPrepared(BaseMediaEncoder encoder);

        void onStopped(BaseMediaEncoder encoder);
    }

    // 同步锁
    protected final Object mSync = new Object();
    // Flag that indicate this encoder is capturing now.
    // 是否正在进行录制的状态记录
    protected volatile boolean mIsCapturing;
    // Flag that indicate the frame data will be available soon.
    // 可用数据帧数量
    private int mRequestDrainEncoderCount;
    // Flag to request stop capturing
    // 结束录制的标识
    protected volatile boolean mRequestStop;
    // Flag that indicate encoder received EOS(End Of Stream)
    // 结束录制标识
    protected boolean mIsEndOfStream;
    //Flag the indicate the muxer is running
    // muxer结束标识
    protected boolean mMuxerStarted;
    //Track Number
    protected int mTrackIndex;

    /**
     * -----------------------------
     */
    // MediaCodec instance for encoding
    protected MediaCodec mMediaCodec;
    // BufferInfo instance for dequeuing
    private MediaCodec.BufferInfo mBufferInfo;

    /**
     * ----------------------------
     */
    // MediaMuxerWarapper instance
    protected MediaMuxerManager mMediaMuxerManager;
    //
    protected WeakReference<MediaEncoderListener> mMediaEncoderListener;

    protected final MediaEncoderConfig mConfig;


    /**
     * 构造方法
     *
     * @param mediaMuxerManager
     * @param mediaEncoderListener
     */
    public BaseMediaEncoder(final MediaMuxerManager mediaMuxerManager, final MediaEncoderListener mediaEncoderListener, final MediaEncoderConfig config) {
        Log.d(TAG, "---BaseMediaEncoderRunnable---");
        if (mediaEncoderListener == null) {
            throw new NullPointerException("MediaEncoderListener is null");
        }
        if (mediaMuxerManager == null) {
            throw new NullPointerException("MediaMuxerWrapper is null");
        }
        //
        this.mMediaMuxerManager = mediaMuxerManager;
        this.mMediaEncoderListener = new WeakReference<>(mediaEncoderListener);
        //
        //
        this.mMediaMuxerManager.addEncoder(BaseMediaEncoder.this);

        this.mConfig = config;

        //
        Log.d(TAG, "---BaseMediaEncoderRunnable synchronized (mSync) before begin---");
        synchronized (mSync) {
            Log.d(TAG, "---BaseMediaEncoderRunnable synchronized (mSync) begin---");
            // create BufferInfo here for effectiveness(to reduce GC)
            mBufferInfo = new MediaCodec.BufferInfo();
            // wait for starting thread
            new Thread(this, getClass().getSimpleName()).start();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "---BaseMediaEncoderRunable synchronized (mSync) end---");
    }

    public String getEncoderName() {
        return mConfig.getEncoderName();
    }

    /**
     * the method to indicate frame data is soon available or already available
     *
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
//        Log.d(TAG, "---frameAvailableSoon---");
//        Log.d(TAG, "---mSync before begin---");
        synchronized (mSync) {
//            Log.d(TAG, "---mSync begin---");
            if (!mIsCapturing || mRequestStop) {
//                Log.d(TAG, "mIsCapturing: " + mIsCapturing);
//                Log.d(TAG, "mRequestStop: " + mRequestStop);
//                Log.d(TAG, "return false");
                return false;
            }
            mRequestDrainEncoderCount++;
//            Log.d(TAG, "mRequestDrainEncoderCount: " + mRequestDrainEncoderCount);
            mSync.notifyAll();
        }
//        Log.d(TAG, "---mSync end---");
//        Log.d(TAG, "return true");
        return true;
    }

    /**
     * encoding loop on private thread
     */
    @Override
    public void run() {
        Log.d(TAG, "---run---");
        Log.d(TAG, "---run synchronized (mSync) before begin---");
        // 线程开启
        synchronized (mSync) {
            Log.d(TAG, "---run synchronized (mSync) begin---");
            //
            mRequestStop = false;
            mRequestDrainEncoderCount = 0;
            //
            mSync.notify();
        }
        Log.d(TAG, "---run synchronized (mSync) end---");
        // 线程开启
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrainEncoderFlag;
        while (isRunning) {
            //
            Log.d(TAG, "---run2 synchronized (mSync) before begin---");
            synchronized (mSync) {
                Log.d(TAG, "---run2 synchronized (mSync) begin---");
                localRequestStop = mRequestStop;
                localRequestDrainEncoderFlag = (mRequestDrainEncoderCount > 0);
                if (localRequestDrainEncoderFlag) {
                    mRequestDrainEncoderCount--;
                }
            }
            Log.d(TAG, "---run2 synchronized (mSync) end---");
            // 停止编码时，调用
            if (localRequestStop) {
                drainEncoder();
                // request stop recording
                signalEndOfInputStream();
                // process output data again for EOS signal
                drainEncoder();
                // release all related objects
                release();
                break;
            }
            // 需要编码
            if (localRequestDrainEncoderFlag) {
                drainEncoder();
            } else {
                // ------线程进入等待状态---------
                Log.d(TAG, "---run3 synchronized (mSync) before begin---");
                synchronized (mSync) {
                    Log.d(TAG, "---run3 synchronized (mSync) begin---");
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                Log.d(TAG, "---run3 synchronized (mSync) end---");
            }
        } // end of while

        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }


    /**
     * 目前在主线程被调用
     *
     * @throws IOException
     */
    public abstract void prepare() throws IOException;

    /**
     * 目前主线程调用
     */
    public void startRecording() {
        Log.d(TAG, "---startRecording synchronized (mSync) before begin---");
        synchronized (mSync) {
            Log.d(TAG, "---startRecording synchronized (mSync) begin---");
            // 正在录制标识
            mIsCapturing = true;
            // 停止标识 置false
            mRequestStop = false;
            //
            mSync.notifyAll();
        }
        Log.d(TAG, "---startRecording synchronized (mSync) end---");
    }


    /**
     * 停止录制(目前在主线程调用)
     */
    public void stopRecording() {
        Log.d(TAG, "---stopRecording synchronized (mSync) before begin---");
        synchronized (mSync) {
            Log.d(TAG, "---stopRecording synchronized (mSync) begin---");
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;
            mSync.notifyAll();
        }
        Log.d(TAG, "---stopRecording synchronized (mSync) end---");
    }


    /**
     * Release all releated objects
     */
    public void release() {
        // 设置标识 停止
        mIsCapturing = false;
        // ------释放mediacodec--------
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        // ----------释放muxer-----------
        if (mMuxerStarted) {
            if (mMediaMuxerManager != null) {
                try {
                    mMediaMuxerManager.stop();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // mBufferInfo置空
        mBufferInfo = null;

        // 回调停止
        if (mMediaEncoderListener.get() != null) {
            try {
                mMediaEncoderListener.get().onStopped(BaseMediaEncoder.this);
                mMediaEncoderListener.clear();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 停止录制
     */
    public void signalEndOfInputStream() {
        encode(null, 0, getPTSUs());
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     *
     * @param buffer
     * @param length             　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        //
        if (!mIsCapturing) {
            return;
        }
        //
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        //
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                //
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
                if (length <= 0) {
                    mIsEndOfStream = true;
                    //
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            }
        }
    }


    /**
     * mEncoder从缓冲区取数据，然后交给mMuxer编码
     */
    protected void drainEncoder() {
        if (mMediaCodec == null) {
            return;
        }

        //
        int count = 0;

        if (mMediaMuxerManager == null) {
            return;
        }

        //拿到输出缓冲区,用于取到编码后的数据
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();

        LOOP:
        while (mIsCapturing) {
            //拿到输出缓冲区的索引
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!mIsEndOfStream) {
                    if (++count > 5) {
                        // out of while
                        break LOOP;
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // this should not come when encoding
                // 拿到输出缓冲区,用于取到编码后的数据
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                // get output format from codec and pass them to muxer
                final MediaFormat format = mMediaCodec.getOutputFormat();
                //
                mTrackIndex = mMediaMuxerManager.addTrack(format);
                //
                mMuxerStarted = true;
                //
                if (!mMediaMuxerManager.start()) {
                    // we should wait until muxer is ready
                    synchronized (mMediaMuxerManager) {
                        while (!mMediaMuxerManager.isStarted())
                            try {
                                mMediaMuxerManager.wait(100);
                            } catch (final InterruptedException e) {
                                break LOOP;
                            }
                    }
                }
            } else if (encoderStatus < 0) {
                // unexpected status

            } else {
                // 获取解码后的数据
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    // this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                //
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                //
                if (mBufferInfo.size != 0) {
                    // encoded data is ready, clear waiting counter
                    count = 0;
                    if (!mMuxerStarted) {
                        // muxer is not ready...this will programing failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    // 编码
                    mMediaMuxerManager.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                //
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    mIsCapturing = false;
                    break;      // out of while
                }
            }
        }
    }

    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    protected long getPTSUs() {
//        long result = System.nanoTime() / 1000L;
//        if (result < prevOutputPTSUs)
//            result = (prevOutputPTSUs - result) + result;
//        return result;
        if (prevOutputPTSUs == 0) {
            return System.nanoTime() / 1000L;
        }
        return prevOutputPTSUs + 1000000 / mConfig.getFrameRate();
    }

}
