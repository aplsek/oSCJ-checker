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

#include "util.h"
#include "jtypes.h"
typedef signed int cint;
typedef signed int bint;
typedef signed int zint;
typedef signed int sint;
#include "structs.h"

typedef unsigned char byte;

#define null_byte ((byte*)0)
#define null_code_fragment ((CodeFragment*)0)



typedef struct Array {
   struct java_lang_Object _parent_;
   int length;
} Array;

struct arr_signed_char {
   struct java_lang_Object _parent_;
   int length;
  signed char values[0];
}; /* Krysztof, we need this - J */


struct arr_unsigned_short {
  struct java_lang_Object _parent_;
  int length;
  unsigned short values[0];
}; 

struct arr_jint_255 {
  struct arr_jint header;
  jint values_[255];
}; /* yes, we need this - C */


// Mappings to hide some of the ugliness...
typedef struct s3_core_domain_S3Blueprint Blueprint;

typedef struct ovm_core_execution_CoreServicesAccess CoreServicesAccess;

typedef struct java_lang_Object Object;

typedef struct s3_core_domain_S3ByteCode ByteCode;
typedef struct ovm_core_execution_InvocationMessage InvocationMessage;
typedef struct ovm_core_execution_ReturnMessage ReturnMessage;
typedef struct ovm_core_execution_ValueUnion ValueUnion;

typedef struct java_lang_StackTraceElement StackTraceElement;

#define NULL_REFERENCE (jref)0



#endif // TYPES_H
