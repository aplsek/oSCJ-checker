/*
 * TestJNI900.c
 *
 *  Created on: Apr 28, 2009
 *      Author: leizhao
 */
#include"s3scj_tck_TestJNI900.h"
/*
 * Class:     s3scj_tck_TestJNI900_MyMission
 * Method:    testObjInfo
 * Signature: (Ljava/lang/Object;)Z
 */
JNIEXPORT jboolean JNICALL
Java_s3scj_tck_TestJNI900_testObjInfo(JNIEnv *env,
		jobject this, jobject o) {
	jclass C;
	
	(*env)->GetVersion(env);

	if (!(*env)->IsInstanceOf(env, o, (*env)->GetObjectClass(env, o)))
		return JNI_FALSE;

	if (!(*env)->IsSameObject(env, o, o))
		return JNI_FALSE;

	C = (*env)->GetObjectClass(env, o);
	if (!(*env)->IsAssignableFrom(env, C, (*env)->GetSuperclass(env, C)))
		return JNI_FALSE;

	return JNI_TRUE;
}

/**
 * s == "jnitest"
 */
JNIEXPORT jboolean JNICALL
Java_s3scj_tck_TestJNI900_testString(JNIEnv *env,
		jobject this, jstring s) {

	const jbyte *targetStr = "jnitest";
	const jchar *argStr;
	const int strLen = 7;
	jboolean isCopy;
	jbyte buf;
	int i;

	if ((*env)->GetStringLength(env, s) != strLen)
		return JNI_FALSE;

	argStr = (*env)->GetStringChars(env, s, &isCopy);

	for (i = 0; i < strLen; i++) {
		if (targetStr[i] != argStr[i])
			return JNI_FALSE;
	}

	(*env)->ReleaseStringChars(env, s, argStr);

	if ((*env)->GetStringUTFLength(env, s) != strLen)
		return JNI_FALSE;

	argStr = (*env)->GetStringUTFChars(env, s, &isCopy);

	(*env)->ReleaseStringUTFChars(env, s, argStr);

	(*env)->GetStringRegion(env, s, 0, 1, &buf);

	if (buf != targetStr[0])
		return JNI_FALSE;

	(*env)->GetStringUTFRegion(env, s, 0, 1, &buf);
	if (buf != targetStr[0])
		return JNI_FALSE;

	return JNI_TRUE;
}

/**
 * Initially, iArray = [1,0,0,0,0]; aArray = [a,null,null,null,null]
 * Finally, they should be [1,2,3,4,5]; [a,null,a,null,null]
 */
JNIEXPORT jboolean JNICALL
Java_s3scj_tck_TestJNI900_testArray(JNIEnv *env,
		jobject this, jintArray intArray, jobjectArray objArray, jobject o) {
	jboolean isCopy;
	int *array;
	int buf;

	if ((*env)->GetArrayLength(env, intArray) != 5)
		return JNI_FALSE;

	if (!(*env)->IsSameObject(env,
			(*env)->GetObjectArrayElement(env, objArray, 0), o))
		return JNI_FALSE;

	(*env)->SetObjectArrayElement(env, objArray, 2, o);

	if ((*env)->NewIntArray(env, 5) == NULL)
		return JNI_FALSE;

	(*env)->GetIntArrayRegion(env, intArray, 0, 1, &buf);

	if (buf != 1)
		return JNI_FALSE;

	buf = 2;

	(*env)->SetIntArrayRegion(env, intArray, 1, 1, &buf);

	array = (*env)->GetIntArrayElements(env, intArray, &isCopy);

	array[2] = 3;
	array[3] = 4;
	array[4] = 5;

	(*env)->ReleaseIntArrayElements(env, intArray, array, 0);

	return JNI_TRUE;
}

/**
 *
 */
JNIEXPORT jboolean JNICALL
Java_s3scj_tck_TestJNI900_testIO(JNIEnv *env,
		jobject this, jobject buffer) {
			
	void *buf = (void *) malloc(sizeof(int));
	jlong capacity = 1;

	if ((*env)->NewDirectByteBuffer(env, &buf, capacity) == NULL) {
	//	printf("NewDirectByteBuffer fails\n");
		return JNI_FALSE;
	}
	free(buf);

	if ((*env)->GetDirectBufferAddress(env, buffer) == NULL){
	//	printf("GetDirectBufferAddress fails, buffer address[%p]\n",buffer);
		return JNI_FALSE;
	}
	if ((*env)->GetDirectBufferCapacity(env, buffer) == -1){
	//	printf("GetDirectBufferCapacity fails\n");
		return JNI_FALSE;
	}
	
	return JNI_TRUE;	
}
