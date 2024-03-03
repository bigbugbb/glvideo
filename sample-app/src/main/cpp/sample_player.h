#ifndef SAMPLE_PLAYER_H
#define SAMPLE_PLAYER_H

#include <jni.h>
#include <android/log.h>

extern "C" {
    #include "libavutil/log.h"
    #include "libavutil/ffversion.h"
}

/** Library version string */
#define FFMPEG_KIT_VERSION "6.0"

/** Defines tag used for Android logging. */
#define LIB_NAME "sample_player"

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

#endif /* SAMPLE_PLAYER_H */