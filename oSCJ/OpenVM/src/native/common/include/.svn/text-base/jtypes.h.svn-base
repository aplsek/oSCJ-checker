/*
 * jtypes.h
 * Java types.
 *
 * Copyright (c) 1996, 1997
 *	Transvirtual Technologies, Inc.  All rights reserved.
 *
 * See the file "license.terms" for information on usage and redistribution 
 * of this file. 
 */

#ifndef __jtypes_h
#define __jtypes_h

#define	SIZEOF_VOIDP 4

// C99 dictates that int has 32 bits and long long has 64
typedef float		jfloat;
typedef double __attribute__((aligned(8))) jdouble;
typedef void *          jref;
typedef unsigned short	jchar;
typedef signed char	jbyte;
typedef signed short	jshort;

typedef int		jint;
typedef unsigned int	jsize;
typedef unsigned int    unsignedjint;
typedef long long __attribute__((aligned(8))) jlong;
typedef unsigned long long __attribute__((aligned(8))) unsignedjlong;
typedef	jbyte		jboolean;

typedef	jchar		unicode;
typedef	jbyte		jbool;		/* Must deprecate */
typedef	jref		jobject;
typedef	jref		jclass;
typedef	jref		jstring;
typedef	jref		jarray;
typedef	jref		jthrowable;

typedef	jref		jobjectArray;
typedef	jref		jbooleanArray;
typedef	jref		jbyteArray;
typedef	jref		jcharArray;
typedef	jref		jshortArray;
typedef	jref		jintArray;
typedef	jref		jlongArray;
typedef	jref		jfloatArray;
typedef	jref		jdoubleArray;

/* An integer type big enough for a pointer or a 32-bit int/float. */
#if SIZEOF_VOIDP <= 4
typedef jint		jword;
#elif SIZEOF_VOIDP <= 8
typedef jlong		jword;
#else
#error "void * bigger than java long?"
#endif

/**
 * union value
 *
 * This union represents any of the 4 byte types
 * in java as a single union. Smaller types are held as jint
 * and cast to/from accordingly. We can't put the smaller types in
 * the union unless we always read/write the union value via the correct
 * member, but we can't do that because we don't always know the type.
 * On big-endian machines sub-word values in the union are stored at the
 * most-significant end of the word, not the least-significant as would
 * be the case witn a cast. Hence we must uses casts all the time.
 **/
typedef union jvalue {
  jint		jint;
  jfloat	jfloat;
  jref		jref;
  struct java_lang_Object * jobj;
} jvalue;

/**
 * union jwide
 *
 * This union represents either of the two 8 byte (wide)
 * java types.
 *
 * Support for swapping the two halves of a long was added.
 *
 **/
typedef union {
  jlong jlong;
  jdouble jdouble;
  struct {
   jint x;
   jint y;
  } swap;
} jwide;

#endif
