/*
 * This is a minimal jni.h for OVM so that we can write native code in a JNI
 * compliant style even though OVM does not yet support JNI.
 *
 */
#ifndef _OVM_JNI_H_
#define _OVM_JNI_H_

/* jni_md.h contains the machine-dependent typedefs for jbyte, jint
   and jlong etc*/

#include <jni_md.h>

typedef void JNIEnv;

/* OVM invoke_native looks for a method names OVM_* not Java_ so we do a simple
   define to change the javah generated headers if they exist
*/
#define Java_ OVM_

/*
 * jboolean constants
 */

#define JNI_FALSE 0
#define JNI_TRUE 1

/*
 * possible return values for JNI functions.
 */

#define JNI_OK           0                 /* success */
#define JNI_ERR          (-1)              /* unknown error */
#define JNI_EDETACHED    (-2)              /* thread detached from the VM */
#define JNI_EVERSION     (-3)              /* JNI version error */
#define JNI_ENOMEM       (-4)              /* not enough memory */
#define JNI_EEXIST       (-5)              /* VM already created */
#define JNI_EINVAL       (-6)              /* invalid arguments */

#define JNI_VERSION_1_1 0x00010001
#define JNI_VERSION_1_2 0x00010002

#endif /* !_OVM_JNI_H_ */


