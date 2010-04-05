/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This header contains some function declarations and macros
 * needed to compile the C source code generated by the
 * interpreter generator.
 *
 * @author Krzysztof Palacz
 * @author James Liang
 **/

#include "interpreter_defs.h"
#include <netinet/in.h>


inline 
signed short get_stream_signed_short(byte* pc, int offset) {
     unsigned short retVal = ntohs(*((unsigned short*)(pc + offset)));
     //     return *((signed short *)&retVal);
     return (signed short)retVal;
}

inline 
signed int get_stream_signed_int(byte* pc, int offset) {
     unsigned long retVal = ntohl(*((unsigned long*)(pc + offset)));
     //     return *((signed int *)&retVal);
     return (signed int)retVal;
}


inline 
Blueprint* get_current_blueprint(NativeContext* ctx) {
  return (Blueprint*)ctx->current_frame->current_bp;
}

inline 
jref get_constant(NativeContext* ctx, int index) {
  struct arr_java_lang_Object *cp = ctx->current_frame->current_cp_values;
  ASSERT(index < cp->length, "invalid CP index");
  return cp->values[index];
}

inline 
jref get_constant_bp_resolved_instance_methodref(NativeContext* ctx, int index) {
  struct arr_java_lang_Object *cp = ctx->current_frame->current_cp_values;
  ASSERT(index < cp->length, "invalid CP index");
  return ((struct ovm_core_domain_ConstantResolvedInstanceMethodref*)
	  cp->values[index])
      ->staticDefinerBlueprint;
}

inline 
jref get_constant_shst_resolved_static_methodref(NativeContext* ctx, int index) {
  struct arr_java_lang_Object *cp = ctx->current_frame->current_cp_values;
  ASSERT(index < cp->length, "invalid CP index");
  return ((struct ovm_core_domain_ConstantResolvedStaticMethodref*)
	  cp->values[index])
      ->sharedState;
}

inline 
jref get_constant_shst_resolved_static_fieldref(NativeContext* ctx, int index) {
  struct arr_java_lang_Object *cp = ctx->current_frame->current_cp_values;
  ASSERT(index < cp->length, "invalid CP index");
  return ((struct ovm_core_domain_ConstantResolvedStaticFieldref*)
	  cp->values[index])
      ->sharedState;
}

inline 
jbyte get_constant_tag(NativeContext* ctx, int index) {
  struct arr_jbyte *tags = ctx->current_frame->current_cp_tags;
  ASSERT(index < tags->length, "invalid CP index");
  return tags->values[index];
}


#if CONVERT_ENDIAN
inline jlong get_constant_wide(NativeContext* ctx, int index) {
  fprintf(stderr,
          "FATAL: LDC2_W should not be encountered at runtime in OVM.\n");
  abort();
  /* old implementation: */
  return (jlong) (((jlong)(jint)get_constant(ctx, index)) << 32) | ((unsigned long)(jint)get_constant(ctx, index + 1));
}
#else
inline jlong get_constant_wide(NativeContext* ctx, int index) {
  fprintf(stderr,
          "FATAL: LDC2_W should not be encountered at runtime in OVM.\n");
  abort();
  /* old implementation: */
  return (((jlong)(jint)get_constant(ctx, index + 1)) << 32) | ((unsigned long)(jint)get_constant(ctx, index));
}
#endif


/* we use unions to do all type-punning to ensure we keep the compiler
   happy and don't fall foul of any strict-aliasing rule breaches.
*/

union floatHolder {
    float f;
    int i;
};

inline float ntohf(float f) {
    union floatHolder fh;
    fh.f = f;
    fh.i = ntohl(fh.i);
  return fh.f;
}

union doubleHolder {
    double d;
    struct {
     int x;
     int y;
    } swap;
    long long l;
};

inline double ntohd(double d) {
    union doubleHolder dh;
    dh.d = d;
#if defined(OVM_ARM) && !defined(WORDS_BIGENDIAN)
    int z=dh.swap.x;
    dh.swap.x=dh.swap.y;
    dh.swap.y=z;
#endif
    dh.l = ntohll(dh.l);
  return dh.d;
}
