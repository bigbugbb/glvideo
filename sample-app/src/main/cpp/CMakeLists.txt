
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("ffmpeg_player" C CXX)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.

# Recursively match all .cpp files in the specified directory
file(GLOB_RECURSE PLAYER_SOURCES "player/*.cpp")

# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        ffmpeg_player.c
        ${PLAYER_SOURCES})

# Specifies a path to native header files.
#include_directories(include/)

# Assuming FFmpeg libraries are stored in a directory structure like
# project_root/ffmpeg/{ABI}/lib where {ABI} could be arm64-v8a, armeabi-v7a, etc.
# And FFmpeg headers are in project_root/ffmpeg/include

set(ENABLE_NEON_CHECK true)
set(MY_LIBS_DIR ${PROJECT_SOURCE_DIR}/../../../../libs)
message("Value of MY_LIBS_DIR: ${MY_LIBS_DIR}")

# Helper macro to create imported target for FFmpeg libraries
macro(create_ffmpeg_lib name)
    add_library(${name} SHARED IMPORTED)
    if(ANDROID_ABI STREQUAL "armeabi-v7a" AND ENABLE_NEON_CHECK)
        include_directories("${MY_LIBS_DIR}/ffmpeg-kit/${ANDROID_ABI}/include_neon")
        set_target_properties(${name} PROPERTIES IMPORTED_LOCATION ${MY_LIBS_DIR}/ffmpeg-kit/${ANDROID_ABI}/lib${name}_neon.so)
    else()
        include_directories("${MY_LIBS_DIR}/ffmpeg-kit/${ANDROID_ABI}/include")
        set_target_properties(${name} PROPERTIES IMPORTED_LOCATION ${MY_LIBS_DIR}/ffmpeg-kit/${ANDROID_ABI}/lib${name}.so)
    endif()
endmacro(create_ffmpeg_lib)

# Use the macro to create imported targets for each FFmpeg library
create_ffmpeg_lib(avcodec)
create_ffmpeg_lib(avfilter)
create_ffmpeg_lib(avformat)
create_ffmpeg_lib(avutil)
create_ffmpeg_lib(swresample)
create_ffmpeg_lib(swscale)

# Link the native library with the log library and FFmpeg libraries
target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        avcodec avfilter avformat avutil swresample swscale)
