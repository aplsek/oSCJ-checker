/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This header file contains declarations for the primitive types
 * used by the interpreter that correspond to Java types as well
 * as declarations of the structures needed to represent stack frames
 * and code fragments.
 *
 * @author Jan Vitek
 * @author James Liang
 **/
#ifndef _TYPES_H
#define _TYPES_H

#include "cjtypes.h"
//extern "C" {
#include "util.h"
//}
typedef signed int cint;
typedef signed int bint;
typedef signed int zint;
typedef signed int sint;
#include "cstructs.h"
typedef struct HEADER *jref;

typedef unsigned char byte;

/**
 * union jnarrow
 *
 * This union represents any of the 4 byte (or smaller) types
 * in java as a single union.
 **/
typedef union jnarrow {
  bint		jn_jbool;
  bint		jn_jbyte;
  cint		jn_jchar; 
  sint		jn_jshort;
  jint		jn_jint;
  jfloat	jn_jfloat;
  jref		jn_jref;
} jnarrow;

/**
 * union jwide
 *
 * This union represents either of the two 8 byte (wide)
 * java types.
 **/
typedef union {
  jlong jw_jlong;
  jdouble jw_jdouble;
} jwide;

/**
 * union cjvalue
 *
 * This union represents all ava values
 */
typedef union {
  bint		jv_jboolean;
  bint		jv_jbyte;
  cint		jv_jchar; 
  sint		jv_jshort;
  jint		jv_jint;
  jfloat	jv_jfloat;
  jref		jv_jref;
  jlong		jv_jlong;
  jdouble	jv_jdouble;
} jvalue;

#define null_byte ((byte*)0)
#define null_code_fragment ((CodeFragment*)0)


// Mappings to hide some of the ugliness...
#if 0
// all gone...
typedef e_s3_core_repository_S3ByteCodeFragment CodeFragment;
typedef e_s3_core_repository_S3NativeCodeFragment NativeCodeFragment;
typedef e_Array<CodeFragment> arr_CodeFragment;
typedef e_Array<NativeCodeFragment> arr_NativeCodeFragment
#endif
typedef struct e_s3_core_domain_S3Blueprint Blueprint;

typedef struct e_java_lang_Object Object;

// typedef e_s3_core_domain_S3ByteCode ByteCode;

// (already in j2c.h)
// typedef struct e_ovm_core_execution_InvocationMessage InvocationMessage;

typedef struct e_ovm_core_execution_ReturnMessage ReturnMessage;
typedef struct e_ovm_core_execution_ValueUnion ValueUnion;

typedef struct e_java_lang_StackTraceElement StackTraceElement;

#define NULL_REFERENCE 0



#endif // TYPES_H
