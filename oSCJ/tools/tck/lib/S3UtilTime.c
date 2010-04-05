#include<time.h>
#include"s3scj_bench_util_Time.h"

/*
 * Class:     s3scj_bench_util_time_Time
 * Method:    getNanosecondsNative
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_s3scj_bench_util_Time_getNanosecondsNative(JNIEnv *env, jclass clazz) {
    struct timespec tp;
    clock_gettime(CLOCK_MONOTONIC, &tp); 
    return ((jlong)tp.tv_sec)*1000000000+tp.tv_nsec;
}
