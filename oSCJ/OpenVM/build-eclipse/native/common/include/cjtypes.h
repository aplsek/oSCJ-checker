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

typedef float						jfloat;
typedef double __attribute__((aligned(8)))		jdouble;
typedef unsigned short					jchar;
typedef signed char					jbyte;
typedef signed short					jshort;

typedef int						jint;
typedef unsigned int					jsize;
typedef unsigned int    				unsignedjint;
typedef long long __attribute__((aligned(8)))		jlong;
typedef unsigned long long __attribute__((aligned(8)))  unsignedjlong;
typedef	jbyte						jboolean;

typedef unsignedjint					jword;

#endif
