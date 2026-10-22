#include <jni.h>
#include <string>
#include <vector>
#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include "ncnn-android-vulkan/x86_64/include/ncnn/net.h"
#include "ncnn-android-vulkan/x86_64/include/ncnn/gpu.h"
#include "ncnn-android-vulkan/x86_64/include/ncnn/cpu.h"

#define TAG "NCNNDetector"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static ncnn::Net rtmdet;
static bool has_gpu = false;
static bool is_model_loaded = false;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_sdart_app_NCNNDetector_init(JNIEnv* env, jobject thiz, jobject assetManager, jboolean use_gpu) {
    // If the model is already loaded in this process, skip initialization completely
    // to avoid fatal Vulkan crashes or OpenMP thread leaks during reload.
    if (is_model_loaded) {
        LOGD("NCNN: Model already loaded, skipping init to prevent crash.");
        return JNI_TRUE;
    }
    
    // Global settings
    ncnn::Option opt;
    opt.lightmode = true;
    
    // Für Vulkan-GPU-Inferenz macht die CPU nur Pre/Post-Processing.
    // Ein Thread und Powersave-Mode (Little-Cores) reichen hierfür völlig aus 
    // und sparen massiv Energie, während die GPU die schwere Arbeit übernimmt.
    opt.num_threads = 1; 
    ncnn::set_cpu_powersave(1); // 0=all, 1=little cores, 2=big cores
    
    // Enable fp16 
    opt.use_fp16_packed = true;
    opt.use_fp16_storage = true;
    opt.use_fp16_arithmetic = true;

    if (use_gpu) {
        int gpu_count = ncnn::get_gpu_count();
        if (gpu_count > 0) {
            if (!has_gpu) {
                ncnn::create_gpu_instance();
                has_gpu = true;
                LOGD("Vulkan GPU enabled (count: %d) - Created new instance", gpu_count);
            } else {
                LOGD("Vulkan GPU enabled (count: %d) - Reusing existing instance", gpu_count);
            }
            opt.use_vulkan_compute = true;
        } else {
            LOGE("No Vulkan GPU found, using CPU");
        }
    }
    
    // Apply options to the net BEFORE loading the model!
    rtmdet.opt = opt;
    if (use_gpu && ncnn::get_gpu_count() > 0) {
        rtmdet.set_vulkan_device(0);
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    
    int ret_param = rtmdet.load_param(mgr, "rtmdet_tiny.param");
    int ret_bin = rtmdet.load_model(mgr, "rtmdet_tiny.bin");

    if (ret_param != 0 || ret_bin != 0) {
        LOGE("Failed to load model from assets");
        return JNI_FALSE;
    }

    is_model_loaded = true;
    LOGD("NCNN model loaded successfully!");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_sdart_app_NCNNDetector_detect(JNIEnv* env, jobject thiz, jobject bitmap) {
    LOGD("NCNN: Entering detect method...");
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap not RGBA_8888");
        return env->NewFloatArray(0);
    }

    int width = info.width;
    int height = info.height;
    LOGD("NCNN: Converting bitmap of size %dx%d...", width, height);
    
    // RTMDet models (from MMDeploy) expect BGR color space and Mean/Std Normalization.
    // We convert from RGBA to BGR
    ncnn::Mat in = ncnn::Mat::from_android_bitmap(env, bitmap, ncnn::Mat::PIXEL_RGBA2BGR);

    LOGD("NCNN: Preprocessing input tensor (BGR mode with Mean/Std)...");
    
    // RTMDet BGR Mean: [103.53, 116.28, 123.675]
    // RTMDet BGR Std:  [57.375, 57.12, 58.395] (norm is 1 / std)
    const float mean_vals[3] = {103.53f, 116.28f, 123.675f};
    const float norm_vals[3] = {1.0f / 57.375f, 1.0f / 57.12f, 1.0f / 58.395f};
    in.substract_mean_normalize(mean_vals, norm_vals);

    LOGD("NCNN: Creating extractor...");
    ncnn::Extractor ex = rtmdet.create_extractor();
    ex.input("input", in);

    LOGD("NCNN: Starting extraction for boxes...");
    ncnn::Mat boxes;
    int ret1 = ex.extract("boxes", boxes);
    LOGD("NCNN: Extraction for boxes finished (ret: %d)", ret1);
    
    LOGD("NCNN: Starting extraction for scores...");
    ncnn::Mat scores;
    int ret2 = ex.extract("scores", scores);
    LOGD("NCNN: Extraction for scores finished (ret: %d)", ret2);
    
    if (ret1 != 0 || ret2 != 0 || boxes.empty() || scores.empty()) {
        LOGE("Inference failed or empty output");
        return env->NewFloatArray(0);
    }

    // boxes might have arbitrary dimensions (e.g. c=8400, h=1, w=4) depending on how onnx2ncnn parsed it.
    // To be safe against NCNN channel alignment padding, we manually flatten the data.
    std::vector<float> flat_boxes;
    flat_boxes.reserve(boxes.c * boxes.h * boxes.w);
    for (int q = 0; q < boxes.c; q++) {
        const float* ptr = boxes.channel(q);
        for (int i = 0; i < boxes.h * boxes.w; i++) {
            flat_boxes.push_back(ptr[i]);
        }
    }

    std::vector<float> flat_scores;
    flat_scores.reserve(scores.c * scores.h * scores.w);
    for (int q = 0; q < scores.c; q++) {
        const float* ptr = scores.channel(q);
        for (int i = 0; i < scores.h * scores.w; i++) {
            flat_scores.push_back(ptr[i]);
        }
    }

    int num_dets = flat_boxes.size() / 4;
    int num_classes = 0;
    if (num_dets > 0) {
        num_classes = flat_scores.size() / num_dets;
    }

    LOGD("NCNN: boxes dims=%d, w=%d, h=%d, c=%d -> flat size=%d", boxes.dims, boxes.w, boxes.h, boxes.c, (int)flat_boxes.size());
    LOGD("NCNN: scores dims=%d, w=%d, h=%d, c=%d -> flat size=%d", scores.dims, scores.w, scores.h, scores.c, (int)flat_scores.size());
    LOGD("NCNN: Flattened -> num_dets=%d, num_classes=%d", num_dets, num_classes);

    std::vector<float> result;
    result.reserve(100 * 6); // estimate

    if (num_dets > 0 && num_classes > 0) {
        int valid_count = 0;
        for (int i = 0; i < num_dets; i++) {
            const float* box_ptr = flat_boxes.data() + i * 4;
            const float* score_ptr = flat_scores.data() + i * num_classes;
            
            // Find max class
            float max_score = -1.0f;
            int max_class = -1;
            for (int c = 0; c < num_classes; c++) {
                if (score_ptr[c] > max_score) {
                    max_score = score_ptr[c];
                    max_class = c;
                }
            }
            
            if (max_score > 0.1f) {
                float x1 = box_ptr[0];
                float y1 = box_ptr[1];
                float x2 = box_ptr[2];
                float y2 = box_ptr[3];
                
                // Push in order: x1, y1, x2, y2, conf, class
                result.push_back(x1);
                result.push_back(y1);
                result.push_back(x2);
                result.push_back(y2);
                result.push_back(max_score);
                result.push_back((float)max_class);
                valid_count++;
            }
        }
        LOGD("NCNN Inference extracted %d valid detections (out of %d raw)", valid_count, num_dets);
    } else {
        LOGE("NCNN Inference failed: num_dets or num_classes is 0");
    }

    jfloatArray jOutput = env->NewFloatArray(result.size());
    env->SetFloatArrayRegion(jOutput, 0, result.size(), result.data());
    return jOutput;
}

extern "C" JNIEXPORT void JNICALL
Java_com_sdart_app_NCNNDetector_destroy(JNIEnv* env, jobject thiz) {
    // DO NOT clear the network (rtmdet.clear()).
    // The GPU instance and model weights are kept alive globally.
    // Re-creating them later in the same process causes fatal Vulkan/OpenMP crashes.
    LOGD("NCNN: Detector resources released, but model is safely kept in memory.");
}
