// ffmpeg is written in C, It links different than C++ due to name mangling
#include <pthread.h>
#include <stdatomic.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "ffmpeg_player.h"
#include "player_interface.h"
#include "config.h"
#include "libavcodec/jni.h"
#include "libavutil/bprint.h"
#include "libavutil/file.h"
#include "libavutil/mem.h"

/** Global reference to the virtual machine running */
static JavaVM *globalVm;

/** Global reference of Config class in Java */
static jclass playerClass;

/** Global reference of String class in Java */
static jclass stringClass;

/** Global reference of IllegalArgumentException class in Java */
static jclass exceptionClass;

/** Global reference of String constructor in Java */
static jmethodID stringConstructor;

/** Full name of the Config class */
const char *playerClassName = "com/binbo/glvideo/sample_app/utils/player/FFmpegPlayer";

/** Full name of String class */
const char *stringClassName = "java/lang/String";

/** Full name of IllegalArgumentException class */
const char *exceptionClassName = "java/lang/IllegalArgumentException";

/** Prototypes of native functions defined by Config class. */
JNINativeMethod playerMethods[] = {
    {"createPlayer", "(Ljava/lang/String;)I", FFmpegPlayer_createPlayer},
    {"destroyPlayer", "()I", FFmpegPlayer_destroyPlayer},
    {"openPlayer", "(Ljava/lang/String;D)I", FFmpegPlayer_openPlayer},
    {"closePlayer", "()I", FFmpegPlayer_closePlayer},
    {"play", "()I", FFmpegPlayer_play},
    {"seek", "(D)I", FFmpegPlayer_seek},
    {"pause", "()I", FFmpegPlayer_pause}
};

static void onAudioDecoderCreated() {

}

static void onVideoDecoderCreated() {

}

static void onFrameSizeUpdated() {

}

static void onFrameAvailable(AVFrame* pFrame) {
    int width = pFrame->width;
    int height = pFrame->height;
    uint8_t** data = pFrame->data;
    int* linesize = pFrame->linesize;

    LOGD("onFrameAvailable");
}

static void onPlaybackCompleted() {

}

static void onPlayerError() {

}

static void onSeekPosition() {

}

static void onReadIndex() {

}

static void onPlayerOpened() {

}

static void onPlayerClosed() {

}

static void onFrameCaptured() {

}

static int onPlayerCallback(int nType, void* pUserData, void* pReserved) {
    switch (nType) {
        case CALLBACK_CREATE_AUDIO_SERVICE:
            onAudioDecoderCreated();
            break;
        case CALLBACK_CREATE_VIDEO_SERVICE:
            onVideoDecoderCreated();
            break;
        case CALLBACK_UPDATE_FRAME_SIZE:
            onFrameSizeUpdated();
            break;
        case CALLBACK_FRAME_AVAILABLE:
            onFrameAvailable(pUserData);
            break;
        case CALLBACK_PLAYBACK_COMPLETED:
            onPlaybackCompleted();
            break;
        case CALLBACK_ERROR:
            onPlayerError();
            break;
        case CALLBACK_BEGIN_BUFFERING:
            break;
        case CALLBACK_ON_BUFFERING:
            break;
        case CALLBACK_END_BUFFERING:
            break;
        case CALLBACK_SEEK_POSITION:
            onSeekPosition();
            break;
        case CALLBACK_READ_INDEX:
            onReadIndex();
            break;
        case CALLBACK_GET_DOWNLOAD_SPEED:
            break;
        case CALLBACK_OPEN_COMPLETED:
            onPlayerOpened();
            break;
        case CALLBACK_CLOSE_FINISHED:
            onPlayerClosed();
            break;
        case CALLBACK_FRAME_CAPTURED:
            onFrameCaptured();
            break;
    }
}

static int registerPlayerCallbacks() {
    SetCallback(CALLBACK_CREATE_AUDIO_SERVICE, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_CREATE_VIDEO_SERVICE, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_UPDATE_FRAME_SIZE, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_FRAME_AVAILABLE, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_PLAYBACK_COMPLETED, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_ERROR, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_BEGIN_BUFFERING, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_ON_BUFFERING, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_END_BUFFERING, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_SEEK_POSITION, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_READ_INDEX, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_GET_DOWNLOAD_SPEED, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_OPEN_COMPLETED, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_CLOSE_FINISHED, onPlayerCallback, NULL, NULL);
    SetCallback(CALLBACK_FRAME_CAPTURED, onPlayerCallback, NULL, NULL);
}

/**
 * Called when 'ffmpeg_player' native library is loaded.
 *
 * @param vm pointer to the running virtual machine
 * @param reserved reserved
 * @return JNI version needed by 'ffmpegkit' library
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void**)(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("OnLoad failed to GetEnv for class %s.\n", playerClassName);
        return JNI_FALSE;
    }

    jclass localPlayerClass = (*env)->FindClass(env, playerClassName);
    if (localPlayerClass == NULL) {
        LOGE("OnLoad failed to FindClass %s.\n", playerClassName);
        return JNI_FALSE;
    }

    if ((*env)->RegisterNatives(env, localPlayerClass, playerMethods, sizeof(playerMethods) / sizeof(JNINativeMethod)) < 0) {
        LOGE("OnLoad failed to RegisterNatives for class %s.\n", playerClassName);
        return JNI_FALSE;
    }

    jclass localStringClass = (*env)->FindClass(env, stringClassName);
    if (localStringClass == NULL) {
        LOGE("OnLoad failed to FindClass %s.\n", stringClassName);
        return JNI_FALSE;
    }

    jclass localExceptionClass = (*env)->FindClass(env, exceptionClassName);
    if (localExceptionClass == NULL) {
        LOGE("OnLoad failed to FindClass %s.\n", exceptionClassName);
        return JNI_FALSE;
    }

    (*env)->GetJavaVM(env, &globalVm);

    stringConstructor = (*env)->GetMethodID(env, localStringClass, "<init>", "([BLjava/lang/String;)V");
    if (stringConstructor == NULL) {
        LOGE("OnLoad thread failed to GetMethodID for %s.\n", "<init>");
        return JNI_FALSE;
    }

    playerClass = (jclass) ((*env)->NewGlobalRef(env, localPlayerClass));
    stringClass = (jclass) ((*env)->NewGlobalRef(env, localStringClass));
    exceptionClass = (jclass) ((*env)->NewGlobalRef(env, localExceptionClass));

    return JNI_VERSION_1_6;
}

JNIEXPORT jint JNICALL FFmpegPlayer_createPlayer(JNIEnv *env, jclass object, jstring path) {
    if (path == NULL) {
        return -1;
    }

    const char* szPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (szPath == NULL) {
        char buffer[256];
        snprintf(buffer, sizeof(buffer), "Invalid path: %s", szPath);
        (*env)->ThrowNew(env, exceptionClass, buffer);
        return -1;
    }

    jint result = CreatePlayer(szPath);

    registerPlayerCallbacks();

    return result;
}

JNIEXPORT jint JNICALL FFmpegPlayer_destroyPlayer(JNIEnv *env, jclass object) {
    return DestroyPlayer();
}

JNIEXPORT jint JNICALL FFmpegPlayer_openPlayer(JNIEnv *env, jclass clazz, jstring path, jdouble offset) {
    if (path == NULL) {
        return -1;
    }

    const char* szPath = (*env)->GetStringUTFChars(env, path, NULL);
    if (szPath == NULL) {
        char buffer[256];
        snprintf(buffer, sizeof(buffer), "Invalid path: %s", szPath);
        (*env)->ThrowNew(env, exceptionClass, buffer);
        return -1;
    }

    jint result = Open(szPath, offset, 0);
    return result;
}

JNIEXPORT jint JNICALL FFmpegPlayer_closePlayer(JNIEnv *env, jclass clazz) {
    return Close();
}

JNIEXPORT jint JNICALL FFmpegPlayer_play(JNIEnv *env, jclass clazz) {
    return Play();
}

JNIEXPORT jint JNICALL FFmpegPlayer_seek(JNIEnv *env, jclass clazz, jdouble offset) {
    return Seek(offset);
}

JNIEXPORT jint JNICALL FFmpegPlayer_pause(JNIEnv *env, jclass clazz) {
    return Pause();
}