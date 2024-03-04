// ffmpeg is written in C, It links different than C++ due to name mangling
#include <pthread.h>
#include <stdatomic.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "ffmpeg_player.h"
#include "config.h"
#include "libavcodec/jni.h"
#include "libavutil/bprint.h"
#include "libavutil/file.h"
#include "libavutil/mem.h"

# define LogType 1

/** Callback data structure */
struct CallbackData {
    int type;                 // 1 (log callback) or 2 (statistics callback)
    long sessionId;           // session identifier

    int logLevel;             // log level
    AVBPrint logData;         // log data

    int statisticsFrameNumber;        // statistics frame number
    float statisticsFps;              // statistics fps
    float statisticsQuality;          // statistics quality
    int64_t statisticsSize;           // statistics size
    double statisticsTime;            // statistics time
    double statisticsBitrate;         // statistics bitrate
    double statisticsSpeed;           // statistics speed

    struct CallbackData *next;
};

/** Redirection control variables */
static pthread_mutex_t lockMutex;
static pthread_mutex_t monitorMutex;
static pthread_cond_t monitorCondition;

pthread_t callbackThread;
int redirectionEnabled;

struct CallbackData *callbackDataHead;
struct CallbackData *callbackDataTail;

/** Global reference to the virtual machine running */
static JavaVM *globalVm;

/** Global reference of Config class in Java */
static jclass configClass;

/** Global reference of log redirection method in Java */
static jmethodID logMethod;

/** Global reference of safOpen method in Java */
static jmethodID safOpenMethod;

/** Global reference of safClose method in Java */
static jmethodID safCloseMethod;

/** Global reference of String class in Java */
static jclass stringClass;

/** Global reference of String constructor in Java */
static jmethodID stringConstructor;

/** Full name of the Config class */
const char *configClassName = "com/binbo/glvideo/sample_app/utils/player/FFmpegPlayerConfig";

/** Full name of String class */
const char *stringClassName = "java/lang/String";

/** Fields that control the handling of SIGNALs */
volatile int handleSIGQUIT = 1;
volatile int handleSIGINT = 1;
volatile int handleSIGTERM = 1;
volatile int handleSIGXCPU = 1;
volatile int handleSIGPIPE = 1;

/** Holds the id of the current session */
__thread long globalSessionId = 0;

/** Holds the default log level */
int configuredLogLevel = AV_LOG_INFO;

/** Prototypes of native functions defined by Config class. */
JNINativeMethod configMethods[] = {
    {"enableNativeRedirection", "()V", (void*) Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_enableNativeRedirection},
    {"disableNativeRedirection", "()V", (void*) Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_disableNativeRedirection},
    {"setNativeLogLevel", "(I)V", (void*) Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_setNativeLogLevel},
    {"getNativeLogLevel", "()I", (void*) Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeLogLevel},
    {"getNativeFFmpegVersion", "()Ljava/lang/String;", (void*) Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeFFmpegVersion},
    {"getNativeVersion", "()Ljava/lang/String;", (void*) Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeVersion},
    {"setNativeEnvironmentVariable", "(Ljava/lang/String;Ljava/lang/String;)I", (void*) Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_setNativeEnvironmentVariable},
    {"ignoreNativeSignal", "(I)V", (void*) Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_ignoreNativeSignal},
};

static const char *avutil_log_get_level_str(int level) {
    switch (level) {
        case AV_LOG_STDERR:
            return "stderr";
        case AV_LOG_QUIET:
            return "quiet";
        case AV_LOG_DEBUG:
            return "debug";
        case AV_LOG_VERBOSE:
            return "verbose";
        case AV_LOG_INFO:
            return "info";
        case AV_LOG_WARNING:
            return "warning";
        case AV_LOG_ERROR:
            return "error";
        case AV_LOG_FATAL:
            return "fatal";
        case AV_LOG_PANIC:
            return "panic";
        default:
            return "";
    }
}

static void avutil_log_format_line(void *avcl, int level, const char *fmt, va_list vl, AVBPrint part[4], int *print_prefix) {
    int flags = av_log_get_flags();
    AVClass* avc = avcl ? *(AVClass **) avcl : NULL;
    av_bprint_init(part+0, 0, 1);
    av_bprint_init(part+1, 0, 1);
    av_bprint_init(part+2, 0, 1);
    av_bprint_init(part+3, 0, 65536);

    if (*print_prefix && avc) {
        if (avc->parent_log_context_offset) {
            AVClass** parent = *(AVClass ***) (((uint8_t *) avcl) +
                                               avc->parent_log_context_offset);
            if (parent && *parent) {
                av_bprintf(part+0, "[%s @ %p] ",
                           (*parent)->item_name(parent), parent);
            }
        }
        av_bprintf(part+1, "[%s @ %p] ",
                   avc->item_name(avcl), avcl);
    }

    if (*print_prefix && (level > AV_LOG_QUIET) && (flags & AV_LOG_PRINT_LEVEL))
        av_bprintf(part+2, "[%s] ", avutil_log_get_level_str(level));

    av_vbprintf(part+3, fmt, vl);

    if(*part[0].str || *part[1].str || *part[2].str || *part[3].str) {
        char lastc = part[3].len && part[3].len <= part[3].size ? part[3].str[part[3].len - 1] : 0;
        *print_prefix = lastc == '\n' || lastc == '\r';
    }
}

static void avutil_log_sanitize(uint8_t *line) {
    while(*line){
        if(*line < 0x08 || (*line > 0x0D && *line < 0x20))
            *line='?';
        line++;
    }
}

void mutexInit() {
    pthread_mutexattr_t attributes;
    pthread_mutexattr_init(&attributes);
    pthread_mutexattr_settype(&attributes, PTHREAD_MUTEX_RECURSIVE_NP);

    pthread_mutex_init(&lockMutex, &attributes);
    pthread_mutexattr_destroy(&attributes);
}

void monitorInit() {
    pthread_mutexattr_t attributes;
    pthread_mutexattr_init(&attributes);
    pthread_mutexattr_settype(&attributes, PTHREAD_MUTEX_RECURSIVE_NP);

    pthread_condattr_t cattributes;
    pthread_condattr_init(&cattributes);
    pthread_condattr_setpshared(&cattributes, PTHREAD_PROCESS_PRIVATE);

    pthread_mutex_init(&monitorMutex, &attributes);
    pthread_mutexattr_destroy(&attributes);

    pthread_cond_init(&monitorCondition, &cattributes);
    pthread_condattr_destroy(&cattributes);
}

void mutexUnInit() {
    pthread_mutex_destroy(&lockMutex);
}

void monitorUnInit() {
    pthread_mutex_destroy(&monitorMutex);
    pthread_cond_destroy(&monitorCondition);
}

void mutexLock() {
    pthread_mutex_lock(&lockMutex);
}

void mutexUnlock() {
    pthread_mutex_unlock(&lockMutex);
}

void monitorWait(int milliSeconds) {
    struct timeval tp;
    struct timespec ts;
    int rc;

    rc = gettimeofday(&tp, NULL);
    if (rc) {
        return;
    }

    ts.tv_sec  = tp.tv_sec;
    ts.tv_nsec = tp.tv_usec * 1000;
    ts.tv_sec += milliSeconds / 1000;
    ts.tv_nsec += (milliSeconds % 1000)*1000000;
    ts.tv_sec += ts.tv_nsec / 1000000000L;
    ts.tv_nsec = ts.tv_nsec % 1000000000L;

    pthread_mutex_lock(&monitorMutex);
    pthread_cond_timedwait(&monitorCondition, &monitorMutex, &ts);
    pthread_mutex_unlock(&monitorMutex);
}

void monitorNotify() {
    pthread_mutex_lock(&monitorMutex);
    pthread_cond_signal(&monitorCondition);
    pthread_mutex_unlock(&monitorMutex);
}

/**
 * Adds log data to the end of callback data list.
 *
 * @param level log level
 * @param data log data
 */
void logCallbackDataAdd(int level, AVBPrint *data) {

    // CREATE DATA STRUCT FIRST
    struct CallbackData *newData = (struct CallbackData*)av_malloc(sizeof(struct CallbackData));
    newData->type = LogType;
    newData->sessionId = globalSessionId;
    newData->logLevel = level;
    av_bprint_init(&newData->logData, 0, AV_BPRINT_SIZE_UNLIMITED);
    av_bprintf(&newData->logData, "%s", data->str);
    newData->next = NULL;

    mutexLock();

    // INSERT IT TO THE END OF QUEUE
    if (callbackDataTail == NULL) {
        callbackDataTail = newData;

        if (callbackDataHead != NULL) {
            LOGE("Dangling callback data head detected. This can cause memory leak.");
        } else {
            callbackDataHead = newData;
        }
    } else {
        struct CallbackData *oldTail = callbackDataTail;
        oldTail->next = newData;

        callbackDataTail = newData;
    }

    mutexUnlock();

    monitorNotify();
}

/**
 * Removes head of callback data list.
 */
struct CallbackData *callbackDataRemove() {
    struct CallbackData *currentData;

    mutexLock();

    if (callbackDataHead == NULL) {
        currentData = NULL;
    } else {
        currentData = callbackDataHead;

        struct CallbackData *nextHead = currentData->next;
        if (nextHead == NULL) {
            if (callbackDataHead != callbackDataTail) {
                LOGE("Head and tail callback data pointers do not match for single callback data element. This can cause memory leak.");
            } else {
                callbackDataTail = NULL;
            }
            callbackDataHead = NULL;

        } else {
            callbackDataHead = nextHead;
        }
    }

    mutexUnlock();

    return currentData;
}

/**
 * Callback function for FFmpeg logs.
 *
 * @param ptr pointer to AVClass struct
 * @param level log level
 * @param format format string
 * @param vargs arguments
 */
void ffmpegkit_log_callback_function(void *ptr, int level, const char* format, va_list vargs) {
    AVBPrint fullLine;
    AVBPrint part[4];
    int print_prefix = 1;

    if (level >= 0) {
        level &= 0xff;
    }
    int activeLogLevel = av_log_get_level();

    // AV_LOG_STDERR logs are always redirected
    if ((activeLogLevel == AV_LOG_QUIET && level != AV_LOG_STDERR) || (level > activeLogLevel)) {
        return;
    }

    av_bprint_init(&fullLine, 0, AV_BPRINT_SIZE_UNLIMITED);

    avutil_log_format_line(ptr, level, format, vargs, part, &print_prefix);
    avutil_log_sanitize(part[0].str);
    avutil_log_sanitize(part[1].str);
    avutil_log_sanitize(part[2].str);
    avutil_log_sanitize(part[3].str);

    // COMBINE ALL 4 LOG PARTS
    av_bprintf(&fullLine, "%s%s%s%s", part[0].str, part[1].str, part[2].str, part[3].str);

    if (fullLine.len > 0) {
        logCallbackDataAdd(level, &fullLine);
    }

    av_bprint_finalize(part, NULL);
    av_bprint_finalize(part+1, NULL);
    av_bprint_finalize(part+2, NULL);
    av_bprint_finalize(part+3, NULL);
    av_bprint_finalize(&fullLine, NULL);
}

/**
 * Forwards callback messages to Java classes.
 */
void *callbackThreadFunction() {
    JNIEnv *env;
    jint getEnvRc = (*globalVm)->GetEnv(globalVm, (void**) &env, JNI_VERSION_1_6);
    if (getEnvRc != JNI_OK) {
        if (getEnvRc != JNI_EDETACHED) {
            LOGE("Callback thread failed to GetEnv for class %s with rc %d.\n", configClassName, getEnvRc);
            return NULL;
        }

        if ((*globalVm)->AttachCurrentThread(globalVm, &env, NULL) != 0) {
            LOGE("Callback thread failed to AttachCurrentThread for class %s.\n", configClassName);
            return NULL;
        }
    }

    LOGD("Async callback block started.\n");

    while (redirectionEnabled) {

        struct CallbackData *callbackData = callbackDataRemove();
        if (callbackData != NULL) {
            if (callbackData->type == LogType) {

                // LOG CALLBACK
                int size = callbackData->logData.len;

                jbyteArray byteArray = (jbyteArray) (*env)->NewByteArray(env, size);
                (*env)->SetByteArrayRegion(env, byteArray, 0, size, callbackData->logData.str);
                (*env)->CallStaticVoidMethod(env, configClass, logMethod, (jlong) callbackData->sessionId, callbackData->logLevel, byteArray);
                (*env)->DeleteLocalRef(env, byteArray);

                // CLEAN LOG DATA
                av_bprint_finalize(&callbackData->logData, NULL);
            }

            // CLEAN STRUCT
            callbackData->next = NULL;
            av_free(callbackData);

        } else {
            monitorWait(100);
        }
    }

    (*globalVm)->DetachCurrentThread(globalVm);

    LOGD("Async callback block stopped.\n");

    return NULL;
}

/**
 * Used by saf protocol; is expected to be called from a Java thread, therefore we don't need attach/detach
 */
int saf_open(int safId) {
    JNIEnv *env = NULL;
    (*globalVm)->GetEnv(globalVm, (void**) &env, JNI_VERSION_1_6);
    return (*env)->CallStaticIntMethod(env, configClass, safOpenMethod, safId);
}

/**
 * Used by saf protocol; is expected to be called from a Java thread, therefore we don't need attach/detach
 */
int saf_close(int fd) {
    JNIEnv *env = NULL;
    (*globalVm)->GetEnv(globalVm, (void**) &env, JNI_VERSION_1_6);
    return (*env)->CallStaticIntMethod(env, configClass, safCloseMethod, fd);
}

/**
 * Used by JNI methods to enable redirection.
 */
static void enableNativeRedirection() {
    mutexLock();

    if (redirectionEnabled != 0) {
        mutexUnlock();
        return;
    }
    redirectionEnabled = 1;

    mutexUnlock();

    int rc = pthread_create(&callbackThread, 0, callbackThreadFunction, 0);
    if (rc != 0) {
        LOGE("Failed to create callback thread (rc=%d).\n", rc);
        return;
    }

    av_log_set_callback(ffmpegkit_log_callback_function);
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
        LOGE("OnLoad failed to GetEnv for class %s.\n", configClassName);
        return JNI_FALSE;
    }

    jclass localConfigClass = (*env)->FindClass(env, configClassName);
    if (localConfigClass == NULL) {
        LOGE("OnLoad failed to FindClass %s.\n", configClassName);
        return JNI_FALSE;
    }

    if ((*env)->RegisterNatives(env, localConfigClass, configMethods, sizeof(configMethods) / sizeof(JNINativeMethod)) < 0) {
        LOGE("OnLoad failed to RegisterNatives for class %s.\n", configClassName);
        return JNI_FALSE;
    }

    jclass localStringClass = (*env)->FindClass(env, stringClassName);
    if (localStringClass == NULL) {
        LOGE("OnLoad failed to FindClass %s.\n", stringClassName);
        return JNI_FALSE;
    }

    (*env)->GetJavaVM(env, &globalVm);

    logMethod = (*env)->GetStaticMethodID(env, localConfigClass, "log", "(JI[B)V");
    if (logMethod == NULL) {
        LOGE("OnLoad thread failed to GetStaticMethodID for %s.\n", "log");
        return JNI_FALSE;
    }

    safOpenMethod = (*env)->GetStaticMethodID(env, localConfigClass, "safOpen", "(I)I");
    if (safOpenMethod == NULL) {
        LOGE("OnLoad thread failed to GetStaticMethodID for %s.\n", "safOpen");
        return JNI_FALSE;
    }

    safCloseMethod = (*env)->GetStaticMethodID(env, localConfigClass, "safClose", "(I)I");
    if (safCloseMethod == NULL) {
        LOGE("OnLoad thread failed to GetStaticMethodID for %s.\n", "safClose");
        return JNI_FALSE;
    }

    stringConstructor = (*env)->GetMethodID(env, localStringClass, "<init>", "([BLjava/lang/String;)V");
    if (stringConstructor == NULL) {
        LOGE("OnLoad thread failed to GetMethodID for %s.\n", "<init>");
        return JNI_FALSE;
    }

    av_jni_set_java_vm(vm, NULL);

    configClass = (jclass) ((*env)->NewGlobalRef(env, localConfigClass));
    stringClass = (jclass) ((*env)->NewGlobalRef(env, localStringClass));

    callbackDataHead = NULL;
    callbackDataTail = NULL;

    mutexInit();
    monitorInit();

    redirectionEnabled = 0;

    av_set_saf_open(saf_open);
    av_set_saf_close(saf_close);

    enableNativeRedirection();

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_setNativeLogLevel(JNIEnv *env, jclass object, jint level) {
    configuredLogLevel = level;
}

JNIEXPORT jint JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeLogLevel(JNIEnv *env, jclass object) {
    return configuredLogLevel;
}

JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_enableNativeRedirection(JNIEnv *env, jclass object) {
    enableNativeRedirection();
}

JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_disableNativeRedirection(JNIEnv *env, jclass object) {
    mutexLock();

    if (redirectionEnabled != 1) {
        mutexUnlock();
        return;
    }
    redirectionEnabled = 0;

    mutexUnlock();

    av_log_set_callback(av_log_default_callback);

    monitorNotify();
}

/**
 * Returns FFmpeg version bundled within the library natively.
 *
 * @param env pointer to native method interface
 * @param object reference to the class on which this method is invoked
 * @return FFmpeg version string
 */
JNIEXPORT jstring JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeFFmpegVersion(JNIEnv *env, jclass object) {
    return (*env)->NewStringUTF(env, FFMPEG_VERSION);
}

JNIEXPORT jstring JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_getNativeVersion(JNIEnv *env, jclass object) {
    return (*env)->NewStringUTF(env, FFMPEG_KIT_VERSION);
}

JNIEXPORT int JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_setNativeEnvironmentVariable(JNIEnv *env, jclass object, jstring variableName, jstring variableValue) {
    const char *variableNameString = (*env)->GetStringUTFChars(env, variableName, 0);
    const char *variableValueString = (*env)->GetStringUTFChars(env, variableValue, 0);

    int rc = setenv(variableNameString, variableValueString, 1);

    (*env)->ReleaseStringUTFChars(env, variableName, variableNameString);
    (*env)->ReleaseStringUTFChars(env, variableValue, variableValueString);
    return rc;
}

JNIEXPORT void JNICALL Java_com_binbo_glvideo_sample_1app_utils_player_FFmpegPlayerConfig_ignoreNativeSignal(JNIEnv *env, jclass object, jint signum) {
    if (signum == SIGQUIT) {
        handleSIGQUIT = 0;
    } else if (signum == SIGINT) {
        handleSIGINT = 0;
    } else if (signum == SIGTERM) {
        handleSIGTERM = 0;
    } else if (signum == SIGXCPU) {
        handleSIGXCPU = 0;
    } else if (signum == SIGPIPE) {
        handleSIGPIPE = 0;
    }
}
