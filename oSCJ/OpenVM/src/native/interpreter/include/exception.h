/**
 * Exception handling.
 **/

#ifndef EXCEPTION_H
#define EXCEPTION_H


#include "frame.h"
#include "instruction_names.h"
#include "util.h"
#include "types.h"
#include "interpreter_defs.h"
#include "globals.h"
#include "debugging.h"
#include "jvm_constants/throwables.h"

#include <string.h>

#define THROW_EXCEPTION(type,meta)					\
do {									\
   PUSH_R((struct java_lang_Object *)csaTHIS);				\
   PUSH_P((jint)type);							\
   PUSH_P((jint)meta);							\
   INVOKE_CSA(generateThrowable, 3, 0);					\
} while(0)



#define CHECK_NONNULL(l) if ( NULL_REFERENCE == (l) ) \
  THROW_EXCEPTION(NULL_POINTER_EXCEPTION,NULL); else ;

#define ARRAY_STORE_INVALID(arr, val) ({				 \
  struct s3_core_domain_S3Blueprint_Array *abp				 \
    = (struct s3_core_domain_S3Blueprint_Array *) HEADER_BLUEPRINT(arr); \
  val && !is_subtype_of(HEADER_BLUEPRINT(val), abp->componentBlueprint_); \
})

#endif








