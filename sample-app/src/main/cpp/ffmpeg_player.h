#ifndef GLVIDEO_FFMPEG_PLAYER_H
#define GLVIDEO_FFMPEG_PLAYER_H

#include <jni.h>
#include <android/log.h>

#include "libavutil/log.h"
#include "libavutil/ffversion.h"

/** Library version string */
#define FFMPEG_KIT_VERSION "6.0"

/** Defines tag used for Android logging. */
#define LIB_NAME "sample_player"

/**
 * Defines logs printed to stderr by ffmpeg. They are not filtered and always redirected.
 */
#define AV_LOG_STDERR    -16

/** Verbose Android logging macro. */
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LIB_NAME, __VA_ARGS__)

/** Debug Android logging macro. */
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LIB_NAME, __VA_ARGS__)

/** Info Android logging macro. */
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LIB_NAME, __VA_ARGS__)

/** Warn Android logging macro. */
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LIB_NAME, __VA_ARGS__)

/** Error Android logging macro. */
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LIB_NAME, __VA_ARGS__)

/**
 * Sets log level.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param level log level
 */
JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_setNativeLogLevel(JNIEnv *env, jclass object, jint level);

/**
 * Returns current log level.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 */
JNIEXPORT jint JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeLogLevel(JNIEnv *env, jclass object);

/**
 * Disables log redirection.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 */
JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_enableNativeRedirection(JNIEnv *env, jclass object);

/**
 * Disables log redirection.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 */
JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_disableNativeRedirection(JNIEnv *env, jclass object);

/**
 * Returns FFmpeg version bundled within the library natively.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return FFmpeg version string
 */
JNIEXPORT jstring JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeFFmpegVersion(JNIEnv *env, jclass object);

/**
 * Returns FFmpegKit library version natively.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return FFmpegKit version string
 */
JNIEXPORT jstring JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeVersion(JNIEnv *env, jclass object);

/**
 * Sets an environment variable natively
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param variableName environment variable name
 * @param variableValue environment variable value
 * @return zero on success, non-zero on error
 */
JNIEXPORT int JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_setNativeEnvironmentVariable(JNIEnv *env, jclass object, jstring variableName, jstring variableValue);

/**
 * Registers a new ignored signal. Ignored signals are not handled by the library.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param signum signal number
 */
JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_ignoreNativeSignal(JNIEnv *env, jclass object, jint signum);

/**
 * Create the player powered by FFmpeg.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param path the path of the media from local file or network
 * @return zero on success, non-zero on error
 */
JNIEXPORT int JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_createPlayer(JNIEnv *env, jclass object, jstring path);

/**
 * Create the player powered by FFmpeg.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return zero on success, non-zero on error
 */
JNIEXPORT int JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_destroyPlayer(JNIEnv *env, jclass object);

/**
 * Create the player powered by FFmpeg.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param path the path of the media from local file or network
 * @param offset the offset in the media
 * @return zero on success, non-zero on error
 */
JNIEXPORT int JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_openPlayer(JNIEnv *env, jclass clazz, jstring path, jdouble offset);

/**
 * Close the player opened.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return zero on success, non-zero on error
 */
JNIEXPORT jint JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_closePlayer(JNIEnv *env, jclass clazz);

/**
 * Start playing.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return zero on success, non-zero on error
 */
JNIEXPORT jint JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_play(JNIEnv *env, jclass clazz);

/**
 * Seek to a time offset.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param offset the offset in the media
 * @return zero on success, non-zero on error
 */
JNIEXPORT jint JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_seek(JNIEnv *env, jclass clazz, jdouble offset);

/**
 * Pause playing.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return zero on success, non-zero on error
 */
JNIEXPORT jint JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_pause(JNIEnv *env, jclass clazz);

#endif /* GLVIDEO_FFMPEG_PLAYER_H */