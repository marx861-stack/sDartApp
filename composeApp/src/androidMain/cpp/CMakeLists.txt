cmake_minimum_required(VERSION 3.10.2)
project("ml_ncnn")

# Set ncnn path
set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED CONFIG)

# Add our JNI source file
add_library(
        ml_ncnn
        SHARED
        ncnn_detector.cpp)

# Link Vulkan, log, jnigraphics, and ncnn
target_link_libraries(
        ml_ncnn
        ncnn
        vulkan
        log
        jnigraphics)
