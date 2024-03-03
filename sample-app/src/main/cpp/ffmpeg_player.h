#ifndef SAMPLE_PLAYER_H
#define SAMPLE_PLAYER_H

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
 * Enables log and statistics redirection.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 */
JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_enableNativeRedirection(JNIEnv *env, jclass object);

/**
 * Disables log and statistics redirection.
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

#endif /* SAMPLE_PLAYER_H */