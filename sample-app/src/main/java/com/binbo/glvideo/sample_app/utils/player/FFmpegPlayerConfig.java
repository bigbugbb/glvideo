package com.binbo.glvideo.sample_app.utils.player;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.SparseArray;

import com.arthenica.smartexception.java.Exceptions;

import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

public class FFmpegPlayerConfig {

    static class SAFProtocolUrl {
        private final Integer safId;
        private final Uri uri;
        private final String openMode;
        private final ContentResolver contentResolver;
        private ParcelFileDescriptor parcelFileDescriptor;

        public SAFProtocolUrl(final Integer safId, final Uri uri, final String openMode, final ContentResolver contentResolver) {
            this.safId = safId;
            this.uri = uri;
            this.openMode = openMode;
            this.contentResolver = contentResolver;
        }

        public Integer getSafId() {
            return safId;
        }

        public Uri getUri() {
            return uri;
        }

        public String getOpenMode() {
            return openMode;
        }

        public ContentResolver getContentResolver() {
            return contentResolver;
        }

        public void setParcelFileDescriptor(final ParcelFileDescriptor parcelFileDescriptor) {
            this.parcelFileDescriptor = parcelFileDescriptor;
        }

        public ParcelFileDescriptor getParcelFileDescriptor() {
            return parcelFileDescriptor;
        }
    }

    /**
     * The tag used for logging.
     */
    static final String TAG = "ffmpeg-kit";

    /**
     * Prefix of named pipes created by ffmpeg kit.
     */
    static final String FFMPEG_KIT_NAMED_PIPE_PREFIX = "fk_pipe_";

    /**
     * Generates ids for named ffmpeg kit pipes and saf protocol urls.
     */
    private static final AtomicInteger uniqueIdGenerator;

    private static Level activeLogLevel;

    /* Global callbacks */
    private static LogCallback globalLogCallback;
    private static StatisticsCallback globalStatisticsCallback;
    private static final SparseArray<SAFProtocolUrl> safIdMap;
    private static final SparseArray<SAFProtocolUrl> safFileDescriptorMap;

    static {

        Exceptions.registerRootPackage("com.binbo.glvideo");

        android.util.Log.i(TAG, "Loading ffmpeg-player.");

        uniqueIdGenerator = new AtomicInteger(1);

        /* NATIVE LOG LEVEL IS RECEIVED ONLY ON STARTUP */
        activeLogLevel = Level.from(Level.AV_LOG_DEBUG.getValue());

        globalLogCallback = null;
        globalStatisticsCallback = null;

        safIdMap = new SparseArray<>();
        safFileDescriptorMap = new SparseArray<>();

        android.util.Log.i(TAG, String.format("Loaded ffmpeg-player-%s-%s.", getVersion(), getFFmpegVersion()));
    }

    /**
     * Default constructor hidden.
     */
    private FFmpegPlayerConfig() {
    }

    /**
     * <p>Enables log and statistics redirection.
     *
     * <p>When redirection is enabled FFmpeg/FFprobe logs are redirected to Logcat and sessions
     * collect log and statistics entries for the executions. It is possible to define global or
     * session specific log/statistics callbacks as well.
     *
     * <p>Note that redirection is enabled by default. If you do not want to use its functionality
     * please use {@link #disableRedirection()} to disable it.
     */
    public static void enableRedirection() {
        enableNativeRedirection();
    }

    /**
     * <p>Disables log and statistics redirection.
     *
     * <p>When redirection is disabled logs are printed to stderr, all logs and statistics
     * callbacks are disabled and <code>FFprobe</code>'s <code>getMediaInformation</code> methods
     * do not work.
     */
    public static void disableRedirection() {
        disableNativeRedirection();
    }

    /**
     * <p>Log redirection method called by the native library.
     *
     * @param sessionId  id of the session that generated this log, 0 for logs that do not belong
     *                   to a specific session
     * @param levelValue log level as defined in {@link Level}
     * @param logMessage redirected log message data
     */
    private static void log(final long sessionId, final int levelValue, final byte[] logMessage) {
        final Level level = Level.from(levelValue);
        final String text = new String(logMessage);
        final Log log = new Log(sessionId, level, text);

        // AV_LOG_STDERR logs are always redirected
        if ((activeLogLevel == Level.AV_LOG_QUIET && levelValue != Level.AV_LOG_STDERR.getValue()) || levelValue > activeLogLevel.getValue()) {
            // LOG NEITHER PRINTED NOR FORWARDED
            return;
        }

        final LogCallback globalLogCallbackFunction = globalLogCallback;
        if (globalLogCallbackFunction != null) {
            try {
                // NOTIFY GLOBAL CALLBACK DEFINED
                globalLogCallbackFunction.apply(log);
            } catch (final Exception e) {
                android.util.Log.e(TAG, String.format("Exception thrown inside global log callback.%s", Exceptions.getStackTraceString(e)));
            }
        }

        // PRINT LOGS
        switch (level) {
            case AV_LOG_QUIET: {
                // PRINT NO OUTPUT
            }
            break;
            case AV_LOG_TRACE:
            case AV_LOG_DEBUG: {
                android.util.Log.d(TAG, text);
            }
            break;
            case AV_LOG_INFO: {
                android.util.Log.i(TAG, text);
            }
            break;
            case AV_LOG_WARNING: {
                android.util.Log.w(TAG, text);
            }
            break;
            case AV_LOG_ERROR:
            case AV_LOG_FATAL:
            case AV_LOG_PANIC: {
                android.util.Log.e(TAG, text);
            }
            break;
            case AV_LOG_STDERR:
            case AV_LOG_VERBOSE:
            default: {
                android.util.Log.v(TAG, text);
            }
            break;
        }
    }

    /**
     * <p>Statistics redirection method called by the native library.
     *
     * @param sessionId        id of the session that generated this statistics, 0 by default
     * @param videoFrameNumber frame number for videos
     * @param videoFps         frames per second value for videos
     * @param videoQuality     quality of the video stream
     * @param size             size in bytes
     * @param time             processed duration in milliseconds
     * @param bitrate          output bit rate in kbits/s
     * @param speed            processing speed = processed duration / operation duration
     */
    private static void statistics(final long sessionId, final int videoFrameNumber,
                                   final float videoFps, final float videoQuality, final long size,
                                   final double time, final double bitrate, final double speed) {
        final Statistics statistics = new Statistics(sessionId, videoFrameNumber, videoFps, videoQuality, size, time, bitrate, speed);

        final StatisticsCallback globalStatisticsCallbackFunction = globalStatisticsCallback;
        if (globalStatisticsCallbackFunction != null) {
            try {
                // NOTIFY GLOBAL CALLBACK IF DEFINED
                globalStatisticsCallbackFunction.apply(statistics);
            } catch (final Exception e) {
                android.util.Log.e(TAG, String.format("Exception thrown inside global statistics callback.%s", Exceptions.getStackTraceString(e)));
            }
        }
    }

    /**
     * <p>Returns the version of FFmpeg bundled within <code>FFmpegKit</code> library.
     *
     * @return the version of FFmpeg
     */
    public static String getFFmpegVersion() {
        return getNativeFFmpegVersion();
    }

    /**
     * <p>Returns FFmpegPlayer library version.
     *
     * @return FFmpegPlayer version
     */
    public static String getVersion() {
        return getNativeVersion();
    }

    /**
     * <p>Prints the given string to Logcat using the given priority. If string provided is bigger
     * than the Logcat buffer, the string is printed in multiple lines.
     *
     * @param logPriority one of {@link android.util.Log#VERBOSE},
     *                    {@link android.util.Log#DEBUG},
     *                    {@link android.util.Log#INFO},
     *                    {@link android.util.Log#WARN},
     *                    {@link android.util.Log#ERROR},
     *                    {@link android.util.Log#ASSERT}
     * @param string      string to be printed
     */
    public static void printToLogcat(final int logPriority, final String string) {
        final int LOGGER_ENTRY_MAX_LEN = 4 * 1000;

        String remainingString = string;
        do {
            if (remainingString.length() <= LOGGER_ENTRY_MAX_LEN) {
                android.util.Log.println(logPriority, TAG, remainingString);
                remainingString = "";
            } else {
                final int index = remainingString.substring(0, LOGGER_ENTRY_MAX_LEN).lastIndexOf('\n');
                if (index < 0) {
                    android.util.Log.println(logPriority, TAG, remainingString.substring(0, LOGGER_ENTRY_MAX_LEN));
                    remainingString = remainingString.substring(LOGGER_ENTRY_MAX_LEN);
                } else {
                    android.util.Log.println(logPriority, TAG, remainingString.substring(0, index));
                    remainingString = remainingString.substring(index);
                }
            }
        } while (remainingString.length() > 0);
    }

    /**
     * <p>Sets an environment variable.
     *
     * @param variableName  environment variable name
     * @param variableValue environment variable value
     * @return zero on success, non-zero on error
     */
    public static int setEnvironmentVariable(final String variableName, final String variableValue) {
        return setNativeEnvironmentVariable(variableName, variableValue);
    }

    /**
     * <p>Registers a new ignored signal. Ignored signals are not handled by <code>FFmpegKit</code>
     * library.
     *
     * @param signal signal to be ignored
     */
    public static void ignoreSignal(final Signal signal) {
        ignoreNativeSignal(signal.getValue());
    }

    /**
     * <p>Sets a global callback to redirect FFmpeg/FFprobe logs.
     *
     * @param logCallback log callback or null to disable a previously defined callback
     */
    public static void enableLogCallback(final LogCallback logCallback) {
        globalLogCallback = logCallback;
    }

    /**
     * <p>Sets a global callback to redirect FFmpeg statistics.
     *
     * @param statisticsCallback statistics callback or null to disable a previously
     *                           defined callback
     */
    public static void enableStatisticsCallback(final StatisticsCallback statisticsCallback) {
        globalStatisticsCallback = statisticsCallback;
    }

    /**
     * Returns the current log level.
     *
     * @return current log level
     */
    public static Level getLogLevel() {
        return activeLogLevel;
    }

    /**
     * Sets the log level.
     *
     * @param level new log level
     */
    public static void setLogLevel(final Level level) {
        if (level != null) {
            activeLogLevel = level;
            setNativeLogLevel(level.getValue());
        }
    }

    static String extractExtensionFromSafDisplayName(final String safDisplayName) {
        String rawExtension = safDisplayName;
        if (safDisplayName.lastIndexOf(".") >= 0) {
            rawExtension = safDisplayName.substring(safDisplayName.lastIndexOf("."));
        }
        try {
            // workaround for https://issuetracker.google.com/issues/162440528: ANDROID_CREATE_DOCUMENT generating file names like "transcode.mp3 (2)"
            return new StringTokenizer(rawExtension, " .").nextToken();
        } catch (final Exception e) {
            android.util.Log.w(TAG, String.format("Failed to extract extension from saf display name: %s.%s", safDisplayName, Exceptions.getStackTraceString(e)));
            return "raw";
        }
    }

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * SAF protocol url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level 19+. On older API levels it returns an empty url.
     *
     * @param context  application context
     * @param uri      SAF uri
     * @param openMode file mode to use as defined in {@link ContentProvider#openFile ContentProvider.openFile}
     * @return input/output url that can be passed to FFmpegKit or FFprobeKit
     */
    public static String getSafParameter(final Context context, final Uri uri, final String openMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            android.util.Log.i(TAG, String.format("getSafParameter is not supported on API Level %d", Build.VERSION.SDK_INT));
            return "";
        }

        String displayName = "unknown";
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
            }
        } catch (final Throwable t) {
            android.util.Log.e(TAG, String.format("Failed to get %s column for %s.%s", DocumentsContract.Document.COLUMN_DISPLAY_NAME, uri.toString(), Exceptions.getStackTraceString(t)));
            throw t;
        }

        final int safId = uniqueIdGenerator.getAndIncrement();
        safIdMap.put(safId, new SAFProtocolUrl(safId, uri, openMode, context.getContentResolver()));

        return "saf:" + safId + "." + extractExtensionFromSafDisplayName(displayName);
    }

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * SAF protocol url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level &ge; 19. On older API levels it returns an empty url.
     *
     * @param context application context
     * @param uri     SAF uri
     * @return input url that can be passed to FFmpegKit or FFprobeKit
     */
    public static String getSafParameterForRead(final Context context, final Uri uri) {
        return getSafParameter(context, uri, "r");
    }

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * SAF protocol url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level &ge; 19. On older API levels it returns an empty url.
     *
     * @param context application context
     * @param uri     SAF uri
     * @return output url that can be passed to FFmpegKit or FFprobeKit
     */
    public static String getSafParameterForWrite(final Context context, final Uri uri) {
        return getSafParameter(context, uri, "w");
    }

    /**
     * Called from native library to open an SAF protocol url.
     *
     * @param safId SAF id part of an SAF protocol url
     * @return file descriptor created for this SAF id or 0 if an error occurs
     */
    private static int safOpen(final int safId) {
        try {
            SAFProtocolUrl safUrl = safIdMap.get(safId);
            if (safUrl != null) {
                final ParcelFileDescriptor parcelFileDescriptor = safUrl.getContentResolver().openFileDescriptor(safUrl.getUri(), safUrl.getOpenMode());
                safUrl.setParcelFileDescriptor(parcelFileDescriptor);
                final int fd = parcelFileDescriptor.getFd();
                safFileDescriptorMap.put(fd, safUrl);
                return fd;
            } else {
                android.util.Log.e(TAG, String.format("SAF id %d not found.", safId));
            }
        } catch (final Throwable t) {
            android.util.Log.e(TAG, String.format("Failed to open SAF id: %d.%s", safId, Exceptions.getStackTraceString(t)));
        }

        return 0;
    }

    /**
     * Called from native library to close a file descriptor created for a SAF protocol url.
     *
     * @param fileDescriptor file descriptor that belongs to a SAF protocol url
     * @return 1 if the given file descriptor is closed successfully, 0 if an error occurs
     */
    private static int safClose(final int fileDescriptor) {
        try {
            final SAFProtocolUrl safProtocolUrl = safFileDescriptorMap.get(fileDescriptor);
            if (safProtocolUrl != null) {
                ParcelFileDescriptor parcelFileDescriptor = safProtocolUrl.getParcelFileDescriptor();
                if (parcelFileDescriptor != null) {
                    safFileDescriptorMap.delete(fileDescriptor);
                    safIdMap.delete(safProtocolUrl.getSafId());
                    parcelFileDescriptor.close();
                    return 1;
                } else {
                    android.util.Log.e(TAG, String.format("ParcelFileDescriptor for SAF fd %d not found.", fileDescriptor));
                }
            } else {
                android.util.Log.e(TAG, String.format("SAF fd %d not found.", fileDescriptor));
            }
        } catch (final Throwable t) {
            android.util.Log.e(TAG, String.format("Failed to close SAF fd: %d.%s", fileDescriptor, Exceptions.getStackTraceString(t)));
        }

        return 0;
    }

    /**
     * <p>Enables redirection natively.
     */
    private static native void enableNativeRedirection();

    /**
     * <p>Disables redirection natively.
     */
    private static native void disableNativeRedirection();

    /**
     * Returns native log level.
     *
     * @return log level
     */
    static native int getNativeLogLevel();

    /**
     * Sets native log level
     *
     * @param level log level
     */
    private static native void setNativeLogLevel(int level);

    /**
     * <p>Returns FFmpeg version bundled within the library natively.
     *
     * @return FFmpeg version
     */
    private native static String getNativeFFmpegVersion();

    /**
     * <p>Returns FFmpegKit library version natively.
     *
     * @return FFmpegKit version
     */
    private native static String getNativeVersion();

    /**
     * <p>Sets an environment variable natively.
     *
     * @param variableName  environment variable name
     * @param variableValue environment variable value
     * @return zero on success, non-zero on error
     */
    private native static int setNativeEnvironmentVariable(final String variableName, final String variableValue);

    /**
     * <p>Registers a new ignored signal natively. Ignored signals are not handled by
     * <code>FFmpegKit</code> library.
     *
     * @param signum signal number
     */
    private native static void ignoreNativeSignal(final int signum);

    public native static int createPlayer(final String path);

    public native static int destroyPlayer();
}
