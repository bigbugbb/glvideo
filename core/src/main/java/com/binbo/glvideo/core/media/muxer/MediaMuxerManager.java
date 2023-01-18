package com.binbo.glvideo.core.media.muxer;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;

import com.binbo.glvideo.core.media.BaseMediaEncoder;
import com.binbo.glvideo.core.media.encoder.MediaAudioEncoder;
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 把音轨和视频轨道合成封装为新的视频
 */
public class MediaMuxerManager {

    private static final String TAG = MediaMuxerManager.class.getSimpleName();

    // 输出文件路径
    private String mOutputPath;
    // 把音轨和视频轨道合成封装为新的视频
    private final MediaMuxer mMediaMuxer;
    //
    private int mEncoderCount, mStartedCount;
    private boolean mIsStarted;
    private BaseMediaEncoder mVideoEncoder, mAudioEncoder;

    /**
     * Constructor
     *
     * @param ext extension of output file
     * @throws IOException
     */
    public MediaMuxerManager(Context context, File targetDir, String filename, String ext) throws IOException {
        if (TextUtils.isEmpty(ext)) {
            ext = ".mp4";
        }
        try {
            // 输出文件路径
            mOutputPath = getCaptureFile(targetDir, filename, ext);
        } catch (final NullPointerException e) {
            throw new RuntimeException("This app has no permission of writing external storage");
        }
        // 编码器
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        //
        mEncoderCount = mStartedCount = 0;
        //
        mIsStarted = false;
    }

    /**
     * 目前在主线程被调用
     *
     * @throws IOException
     */
    public void prepare() throws IOException {
        if (mVideoEncoder != null)
            mVideoEncoder.prepare();
        if (mAudioEncoder != null)
            mAudioEncoder.prepare();
    }

    /**
     * 目前主线程调用
     */
    public void startRecording() {
        if (mVideoEncoder != null)
            mVideoEncoder.startRecording();
        if (mAudioEncoder != null)
            mAudioEncoder.startRecording();
    }

    public void stopRecording() {
        if (mVideoEncoder != null)
            mVideoEncoder.stopRecording();
        mVideoEncoder = null;
        if (mAudioEncoder != null)
            mAudioEncoder.stopRecording();
        mAudioEncoder = null;
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }


    /**
     * assign encoder to this calss. this is called from encoder.
     *
     * @param encoder instance of MediaVideoEncoderRunable or MediaAudioEncoderRunable
     */
    public void addEncoder(final BaseMediaEncoder encoder) {
        if (encoder instanceof MediaVideoEncoder) {
            if (mVideoEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaAudioEncoder) {
            if (mAudioEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mAudioEncoder = encoder;
        } else {
            throw new IllegalArgumentException("unsupported encoder");
        }
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    /**
     * request start recording from encoder
     *
     * @return true when muxer is ready to write
     */
    /*package*/
    public synchronized boolean start() {
        mStartedCount++;
        if ((mEncoderCount > 0) && (mStartedCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
        }
        return mIsStarted;
    }

    /**
     * request stop recording from encoder when encoder received EOS
     */
    /*package*/
    public synchronized void stop() {
        mStartedCount--;
        if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsStarted = false;
        }
    }

    /**
     * retrieve current assigned video encoder
     */
    public synchronized BaseMediaEncoder getVideoEncoder() {
        return mVideoEncoder;
    }

    /**
     * retrieve current assigned audio encoder
     */
    public synchronized BaseMediaEncoder getAudioEncoder() {
        return mAudioEncoder;
    }

    /**
     * assign encoder to muxer
     *
     * @param format
     * @return minus value indicate error
     */
    public synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted) {
            throw new IllegalStateException("muxer already started");
        }
        final int trackIx = mMediaMuxer.addTrack(format);

        return trackIx;
    }

    /**
     * write encoded data to muxer
     * 写入数据
     *
     * @param trackIndex 轨道
     * @param byteBuf    buffer数据
     * @param bufferInfo
     */
    public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mStartedCount > 0) {
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
        }
    }

    /**
     * generate output file
     * 获取输出文件路径
     *
     * @param ext .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public static final String getCaptureFile(final File targetDir, final String filename, final String ext) {
        /**
         * 生成路径 /sdcard/Android/data/包名/file/name_card/video/time.mp4
         */
        StringBuffer sb = new StringBuffer();
        // 文件夹路径
        sb.append(targetDir);
        sb.append(File.separator);
        // 以时间命名
//        sb.append(getFormatCurrTime());
        sb.append(filename);
        // 扩展名.mp4
        sb.append(ext);
        return sb.toString();
    }

    public static String getFormatCurrTime() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }
}
