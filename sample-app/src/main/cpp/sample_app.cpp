#include <jni.h>


/**
 * Called when 'sample_app' native library is loaded.
 *
 * @param vm pointer to the running virtual machine
 * @param reserved reserved
 * @return JNI version needed by 'ffmpegkit' library
 */
extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1; // JNI version not supported.
    }

    // Here you can register native methods if not using the automatic registration
    // through naming convention (Java_packageName_ClassName_methodName).

    return JNI_VERSION_1_6; // Must return the required JNI version.
}