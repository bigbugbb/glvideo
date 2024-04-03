#pragma once

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
 * Create the player powered by FFmpeg.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param path the path of the media from local file or network
 * @return zero on success, non-zero on error
 */
JNIEXPORT int JNICALL FFmpegPlayer_createPlayer(JNIEnv *env, jclass object, jstring path);

/**
 * Create the player powered by FFmpeg.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return zero on success, non-zero on error
 */
JNIEXPORT int JNICALL FFmpegPlayer_destroyPlayer(JNIEnv *env, jclass object);

/**
 * Create the player powered by FFmpeg.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param path the path of the media from local file or network
 * @param offset the offset in the media
 * @return zero on success, non-zero on error
 */
JNIEXPORT int JNICALL FFmpegPlayer_openPlayer(JNIEnv *env, jclass clazz, jstring path, jdouble offset);

/**
 * Close the player opened.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return zero on success, non-zero on error
 */
JNIEXPORT jint JNICALL FFmpegPlayer_closePlayer(JNIEnv *env, jclass clazz);

/**
 * Start playing.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return zero on success, non-zero on error
 */
JNIEXPORT jint JNICALL FFmpegPlayer_play(JNIEnv *env, jclass clazz);

/**
 * Seek to a time offset.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @param offset the offset in the media
 * @return zero on success, non-zero on error
 */
JNIEXPORT jint JNICALL FFmpegPlayer_seek(JNIEnv *env, jclass clazz, jdouble offset);

/**
 * Pause playing.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return zero on success, non-zero on error
 */
JNIEXPORT jint JNICALL FFmpegPlayer_pause(JNIEnv *env, jclass clazz);

