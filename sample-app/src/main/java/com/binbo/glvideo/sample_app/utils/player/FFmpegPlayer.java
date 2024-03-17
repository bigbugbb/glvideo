package com.binbo.glvideo.sample_app.utils.player;

import com.arthenica.smartexception.java.Exceptions;

public class FFmpegPlayer {

    /**
     * The tag used for logging.
     */
    static final String TAG = "ffmpeg-kit";

    static {
        Exceptions.registerRootPackage("com.binbo.glvideo");
    }

    /**
     * Default constructor hidden.
     */
    private FFmpegPlayer() {
    }

    public native static int createPlayer(final String path);

    public native static int destroyPlayer();

    public native static int openPlayer(final String path, final double offset);

    public native static int closePlayer();

    public native static int play();

    public native static int seek(final double offset);

    public native static int pause();
}
