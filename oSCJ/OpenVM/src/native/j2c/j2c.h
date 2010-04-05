#include "autodefs.h"
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef RTEMS_BUILD
#include <strings.h>
#endif
#include <unistd.h>
#include <fcntl.h>
#include <pthread.h>
#include <math.h>

#include "ctypes.h"
#include "ovm_heap_exports.h"

#include "native_helpers.h"

#if defined(PRECISE_PTRSTACK)

// Double the stack size, so that there is room for
// J2cContext.localState.auxStack
//#define CONTEXT_SIZE (4 * 56 * 1024)
//#define CONTEXT_SIZE (4 * 100 * 1024)

// still not enough for dacapo (ptr stack size)
//#define CONTEXT_SIZE (4 * 200 * 1024)

#define CONTEXT_SIZE (4 * 256 * 1024)

// Only half the context is used for the real call stack

#define PTRSTACKSIZE CONTEXT_SIZE/2

#elif defined(PRECISE_SAFE_POINTS)

typedef HEADER *AppExn_t;
#define APP_THROW j2c_throw

// Double the stack size, so that there is room for
// J2cContext.localState.auxStack
#define CONTEXT_SIZE (4 * 56 * 1024)
// Only half the context is used for the real call stack
#define BAKSIZE CONTEXT_SIZE/2

#include "precise.h"

#else
// The size of a thread context and stack (we add an unreadable gaurd
// page at the bottom).
#define CONTEXT_SIZE (56 * 1024)
#endif

#define THROW(x) throw x

extern double _j2c_NaN;
extern double _j2c_nzero;
extern float _j2c_NaNf;
extern float _j2c_nzerof;

typedef e_ovm_core_domain_Code Code;
typedef e_ovm_core_domain_Oop Oop;
typedef e_ovm_core_execution_Context Context;

typedef e_ovm_core_execution_InvocationMessage InvocationMessage;

typedef e_s3_core_domain_S3Blueprint S3Blueprint;
typedef e_s3_core_domain_S3Blueprint_Array S3Blueprint_Array;
typedef e_s3_core_domain_S3Blueprint_Scalar S3Blueprint_Scalar;
typedef e_s3_core_domain_S3Blueprint_Primitive S3Blueprint_Primitive;

typedef e_s3_core_execution_S3CoreServicesAccess CSA;

typedef struct J2cContext NativeContext;
typedef void *NativeCodeFragment;

//inline jboolean subtype_of_scalar(S3Blueprint *sub, S3Blueprint_Scalar *zuper) {
static inline jboolean subtype_of_scalar(HEADER *subh, HEADER *zuperh) {
  S3Blueprint *sub = (S3Blueprint*)subh;
  S3Blueprint_Scalar *zuper = (S3Blueprint_Scalar *)zuperh;
  
#ifndef ARRAYLETS
  return (  ((jbyte*) (sub->ovm_typeDisplay_1->values)) [ ((S3Blueprint *)zuper)->ovm_typeBucket_1]
	  == ((S3Blueprint*)zuper)->ovm_typeBucketID_1);
#else
  // FIXME: get rid of the dereference... 
  return (  (* (jbyte**) (sub->ovm_typeDisplay_1->values)) [ ((S3Blueprint *)zuper)->ovm_typeBucket_1]
	  == ((S3Blueprint*)zuper)->ovm_typeBucketID_1);
#endif	  
}

/*
 * Variations:  If zuper's element type is know statically not to be
 * Object, Cloneable, or Serializable, we know the actual depth
 * statically, and can maybe specialize for length == 1
 */
//inline jboolean subtype_of_array(S3Blueprint *sub, S3Blueprint *zuper) {

static inline jboolean subtype_of_array(HEADER *subh, HEADER *zuperh) {
  S3Blueprint *sub = (S3Blueprint *)subh;
  S3Blueprint *zuper = (S3Blueprint *)zuperh;
				 
  while (sub->ovm_arrayDepth_1 && zuper->ovm_arrayDepth_1) {
    sub = (S3Blueprint *) (((S3Blueprint_Array *) sub)->ovm_componentBlueprint_1);
    zuper = (S3Blueprint *) (((S3Blueprint_Array *) zuper)->ovm_componentBlueprint_1);
  }
  if (zuper->ovm_arrayDepth_1)
    return J_FALSE;
  else
    return subtype_of_scalar(subh, zuperh);
}

/*
 * If invoke_native returned a (C++) jvalue instead of a jlong, none
 * of this would be needed.  We must be careful to convert the return
 * value to an appropriately sized integer before we stuff it into any
 * unions
 */
#define jlong_to_jwide(name)						\
static inline name to_##name##_jlong(jlong l) {					\
  jwide ret;								\
  ret.jw_jlong = (jlong) l;						\
  return ret.jw_##name;							\
}
#define jlong_to_jnarrow(name)						\
static inline name to_##name##_jlong(jlong l) {					\
  jnarrow ret;								\
  ret.jn_jint = (jint) l;						\
  return ret.jn_##name;							\
}
#define jlong_to_itype(name)						\
static inline name to_##name##_jlong(jlong l) { return (name) l; }

jlong_to_itype(jboolean);
jlong_to_itype(jbyte);
jlong_to_itype(jchar);
jlong_to_itype(jshort);
jlong_to_itype(jint);
jlong_to_jnarrow(jfloat);
jlong_to_jnarrow(jref);
jlong_to_itype(jlong);
jlong_to_jwide(jdouble);

#define jvalue_ctor(T) 							\
static inline jvalue to_jvalue_##T(T v) {					\
  jvalue ret;								\
  ret.jv_##T = v;							\
  return ret;								\
}

#define jwide_ctor(T)							\
jvalue_ctor(T)								\
static inline jwide to_jwide_##T(T v) {					\
  jwide ret;								\
  ret.jw_##T = v;							\
  return ret;								\
}

#define jnarrow_ctor(T)							\
jvalue_ctor(T)								\
static inline jnarrow to_jnarrow_##T(T v) {					\
  jnarrow ret;								\
  ret.jn_##T = v;							\
  return ret;								\
}


// I wonder why we need to have "jboolean" and "jn_jbool" 
// at the same time...
static inline jnarrow to_jnarrow_jboolean(jboolean v) {
  jnarrow ret;
  ret.jn_jbool = v;
  return ret;
}  
//jnarrow_ctor(jboolean);

jnarrow_ctor(jbyte);
jnarrow_ctor(jchar);
jnarrow_ctor(jshort);
jnarrow_ctor(jint);
jnarrow_ctor(jfloat);
jnarrow_ctor(jref);
jwide_ctor(jlong);
jwide_ctor(jdouble);

#ifdef PRECISE_HENDERSON
struct GCFrame {
  GCFrame *next;
  HEADER arrayHeader;
  int length;
};
typedef struct GCFrame GCFrame;
#elif defined(PRECISE_PTRSTACK)
typedef char GCFrame;		// FIXME: shouldn't this be HEADER *?
#endif

// #undef OVM_X86 /* for testing ucontext crap */

// aix/linux have slightly different calling conventions
#if defined(OVM_PPC)
# if defined(OSX_BUILD)
#   define DARWIN_PPC
# elif defined(LINUX_BUILD)
#   define LINUX_PPC
# endif
#endif

#if defined(OVM_ARM)
# if defined(LINUX_BUILD)
#  define LINUX_ARM
# endif
#endif

#if defined(OVM_SPARC)
# if defined(LINUX_BUILD)
#  define LINUX_SPARC
# elif defined(SOLARIS_BUILD)
#   define SOLARIS_SPARC
# elif defined(RTEMS_BUILD)
#   define RTEMS_SPARC
# endif
#endif

#if defined(OVM_X86)
# if defined(OSX_BUILD)
#   define DARWIN_X86
# elif defined(LINUX_BUILD)
#   define LINUX_X86
# elif defined(SOLARIS_BUILD)
#   define SOLARIS_X86
# elif defined(NETBSD_BUILD)
#   define NETBSD_X86
# elif defined(RTEMS_BUILD)
#   define RTEMS_X86
# endif
#endif

#if defined(LINUX_X86) || defined(DARWIN_PPC) || defined(LINUX_PPC) || defined(LINUX_ARM) || defined(DARWIN_X86) || defined(LINUX_SPARC) || defined(SOLARIS_X86) || defined(NETBSD_X86) || defined(SOLARIS_SPARC) || defined(RTEMS_SPARC) || defined(RTEMS_X86)
# define THREAD_SETJMP
#elif defined(HAVE_SWAPCONTEXT)
  /* The thread-launching asm for ppc-linux seems to work, but unless
   * we use swapcontext, certain library functions such as malloc can
   * only be called from the main thread.
   */
  // Note: ucontext calls are not implemented on Linux/ARM
# define THREAD_UCONTEXT
#else
# error "No threading support for this system"
#endif

#ifdef THREAD_SETJMP
# include <setjmp.h>
#else
# include <ucontext.h>
#endif

struct J2cContext {
// let the jmp_buf/ucontext stay at the beginning of
// the structure, just in case it prefers to be
// 8-byte aligned
#ifdef THREAD_SETJMP
  jmp_buf buf;
#else
  ucontext_t uc;
#endif

  // Should include NativeContext, use the VarArea &c?
  Context *ovmContext; // Java pointer

  void *stackBase;
  void *stackTop;

  Code *code; // Java pointer
  InvocationMessage *args; // Java pointer

#if defined(PRECISE_HENDERSON)
  GCFrame *gcTop;
#elif defined(PRECISE_PTRSTACK)
  char *gcTop;
  char ptrStack[PTRSTACKSIZE];
#elif defined(PRECISE_SAFE_POINTS)
  accurate::LocalState localState;
  // true if we need to call localState.gc() on wakeup
  int shouldWalkStack;
#endif

  void *topFrame;
  void *bottomFrame;

/*
  J2cContext(Context *oc): ovmContext(oc) {
#ifdef PRECISE_HENDERSON
    gcTop = 0;
#elif defined(PRECISE_PTRSTACK)
    gcTop = 0;
#elif defined(PRECISE_SAFE_POINTS)
    shouldWalkStack = 0;
#endif
  }
*/  
};
typedef struct J2cContext J2cContext;

extern J2cContext* J2cContext_ini(J2cContext *jc, Context *oc);

extern J2cContext *currentContext;

extern int **initializedBlueprints;

// Force pointer dereference.  This check could be made safer by
// adding the keyword volatile, but gcc already treats asm statements
// with no outputs specially.
#define NULLCHECK(p) ({ asm ("" : : "r"(HEADER_BLUEPRINT((HEADER*)p))); p; })

//extern "C" {
#include "engine.h"
#include "md.h"
#include "native_calls.h"
#include "eventmanager.h"
#include "timer.h"

  // A function to call in place of native/boot-time/otherwise undefined
  // methods. What should the return type of j2cFail be, exactly?
//  extern jint j2cFail(...) __attribute__((noreturn));
  extern jint j2cFail(void) __attribute__((noreturn));

  extern jint j2c_signalEvent() __attribute__((noinline));

  //extern jboolean is_subtype_of(S3Blueprint *, S3Blueprint *);
  extern jboolean is_subtype_of(HEADER *, HEADER *);

  static inline jboolean array_store_valid(HEADER *arr, HEADER *o) {
    S3Blueprint_Array *abp = (S3Blueprint_Array *) HEADER_BLUEPRINT(arr);
    return (!o ||
	    is_subtype_of((HEADER *)HEADER_BLUEPRINT(o), (HEADER *)((S3Blueprint *)(abp->ovm_componentBlueprint_1))));
  }
#define ARRAY_STORE_INVALID(a, o) !array_store_valid(a, o)

  static inline jint needs_init(jint ctx, jint bp) {
    int w = bp >> 5;
    int b = 1 << (bp & 31);
    return (!(initializedBlueprints[ctx][w] & b));
  }
  extern void did_init(jint ctx, jint bp);

  // INVOKE_SYSTEM support
  
  // FIXME: should return void *
  static inline jint getContext(int processorHandle) {
    assert(processorHandle == 0);
    return (jint) currentContext->ovmContext;
  }
  
  // FIXME: should return void *
  static inline jint getNativeContext(int processorHandle) {
    assert(processorHandle == 0);
    return (jint) currentContext;
  }
  
  // FIXME: should return void *
  static inline jint nativeContextToContext(int nc) {
    if (nc==0) return 0;
#ifndef NDEBUG
  if ( ((J2cContext *)nc)->ovmContext == 0 ) {
    ubprintf("ERROR: NON-NULL native context has NULL ovmContext\n");
    abort();
  }
  if ( ((J2cContext *)nc)->ovmContext == 211 ) {
    ubprintf("ERROR: NON-NULL native context is a destroyed ovmContext!!\n");
    abort();
  }  
#endif    
    return (jint) (((J2cContext *)nc)->ovmContext);
  }
  
  //extern jlong j2cInvoke(Code *, InvocationMessage *);
  extern jlong j2cInvoke(HEADER *, HEADER *);
  //extern jint newNativeContext(Context* ovm_context);
  extern jint newNativeContext(HEADER* ovm_context);
  extern void destroyNativeContext(jint nc);
  extern void run(jint processor, jint nc);
 // extern jint makeActivation(jint nc, Code *, InvocationMessage *);
  extern jint makeActivation(jint nc, HEADER *, HEADER *);

  // GC support:
  extern void *getOVMContextLoc(jint nc);
  extern void *getEngineSpecificPtrLoc(jint nc);

  // precise:
  extern void *getInvocationArgsLoc(jint nc);
//  extern e_Array<Oop*> *getLocalReferences(jint gcFrame);
  extern e_Array *getLocalReferences(jint gcFrame);
  extern jint getGcRecord(jint nc, void *frameHandle);
  extern jint getNextGcRecord(jint gcFrame);

  extern void *getPtrStackBase(jint nc);
  extern void *getPtrStackTop(jint nc);
  extern void *getPtrStackLimit(jint nc);

  // conservative:
  extern void *getStackBase(jint nc);
  extern void *getStackTop(jint nc);
  extern jboolean stackGrowsUp();
  extern void *getSavedRegisters(jint nc);
  extern jint getSavedRegisterSize();

  // precise the new way:
  extern void *getAuxStackTop(int nc);
  extern void *getAuxStackBottom(int nc);
  // check that the thread has actually started running
  extern jboolean localStateIsRunning(int nc);
  // Ensure that a thread calls Accurate::LocalState::gc() the
  // next time it runs
  extern void walkStack(int nc);
  extern jint getAuxHeaderSize();
  // gives you the pointers to the pointers to the Code and InvocationMessage
  extern void *pointerToCodePointer(int nc);
  extern void *pointerToArgsPointer(int nc);
  
  // stack trace support:
  extern void *j2c_getCode(void *pc);
  extern void *j2c_bottomFrame(jint nc);
  extern void *j2c_topFrame(jint nc);

  // Generated functions with well-known names.

  extern jint domainForBlueprint(S3Blueprint *bp);

  // Note that j2cThrow's signature must be compatible with
  // CoreServicesAccess.processThrowable()
  extern e_java_lang_Error *j2c_throw(HEADER *)
#if ( defined COUNTER_EXCEPTIONS || defined CEXCEPTIONS )
      ;
#else
      __attribute__((noreturn));
#endif
  extern void boot();  
  extern void throwWildcardException(HEADER *);
  extern void generateThrowable(int);
  extern void assertAddressValid(void *);

  extern HEADER *getCurrentException();

  // Only used in the new precise GC stuff, called after a thread's
  // AuxStack has been computed.  The java method returns the
  // nativeContextHandle we should switch to next.
  extern int nextThreadToWalk();

  // Not currently used.
  extern e_s3_services_j2c_J2cCodeFragment *codeFromPC(jint pc);

  // generateThrowable uses the following variable, which is set and
  // maintained by C++ code
  extern sigset_t j2cNormalMask;
// } (extern "C")

/**
 * An engine-specific primitive operation.  Force the calling
 * method to save all call-preserved registers.
 */
#undef SAVE_REGISTERS
#if defined(DARWIN_X86)
# define SAVE_REGISTERS() asm __volatile__ ("" : : : "esi", "edi")
#elif defined(LINUX_X86) || defined(SOLARIS_X86) || defined(NETBSD_X86) || defined(RTEMS_X86) // no other OS forces use of PIC
# define SAVE_REGISTERS() asm __volatile__ ("" : : : "ebx", "esi", "edi")
#elif defined(LINUX_PPC)
# define SAVE_REGISTERS() asm __volatile__ ("" : : :			\
			      "%r14", "%r15", "%r16", "%r17", "%r18",	\
			      "%r19", "%r20", "%r21", "%r22", "%r23",	\
			      "%r24", "%r25", "%r26", "%r27", "%r28",	\
			      "%r29", "%r30", "%r31")
#elif defined(DARWIN_PPC)
# if __GNUC__ >= 4
/* If r13 does not play a dedicated, and undisclosed, role, it is
 * callee-save.
 */
# define SAVE_REGISTERS() asm __volatile__ ("" : : :			\
			      "r14", "r15", "r16", "r17", "r18",	\
			      "r19", "r20", "r21", "r22", "r23",	\
			      "r24", "r25", "r26", "r27", "r28",	\
			      "r29", "r30", "r13")
# else
#  define SAVE_REGISTERS() asm __volatile__ ("" : : :			\
			       "r14", "r15", "r16", "r17", "r18",	\
			       "r19", "r20", "r21", "r22", "r23",	\
			       "r24", "r25", "r26", "r27", "r28",	\
			       "r29", "r30", "r31", "r13")
# endif
#elif defined (LINUX_ARM)
# if defined(ARM_THUMB)
// in thumb mode r7 is used as a frame pointer
#  define SAVE_REGISTERS() asm __volatile__ ("" : : :			\
			       "r4", "r5", "r6",                        \
                               "r8", "r9", "r10", "r11")
# else
// in standard mode r11 is used as a frame pointer
#  define SAVE_REGISTERS() asm __volatile__ ("" : : :			\
			       "r4", "r5", "r6", "r7",                  \
                               "r8", "r9", "r10")
/* no pointer can be in fp regs on the ARM */
/*			       "f4", "f5", "f6", "f7", */
/* even though they are callee-save, these registers corresponding to
   various ARM extensions are not used by the general allocator on the
   GCC ARM, so they can't contain ptrs.  */
/*			       "mvf4", "mvf5", "mvf6", "mvf7", "mvf8",	\
			       "mvf9", "mvf10", "mvf11", "mvf12",       \
			       "s16", "s17", "s18", "s19", "s20",       \
                               "s21", "s22", "s23", "s24", "s25",       \
                               "s26", "s27", "s28", "s29", "s30", "s31"
*/
# endif
#elif defined (LINUX_SPARC) || defined (SOLARIS_SPARC) || defined(RTEMS_SPARC)
// ta 3 used to force flushing register windows
# define SAVE_REGISTERS() asm __volatile__ ("ta 3" : : :		\
			       "%l0", "%l1", "%l2", "%l3",              \
			       "%l4", "%l5", "%l6", "%l7",              \
			       "%i0", "%i1", "%i2", "%i3",              \
			       "%i4", "%i5")
// % i6 is fp, %i7 stores the ret addr (-8). They are both technically
// call-preserved, but not included in SAVE_REGISTERS as they are
// not available for use by the register allocator.

#else
# warning "SAVE_REGISTERS not implemented, optimized programs should not GC"
# define SAVE_REGISTERS()
#endif

#ifdef PRECISE_SAFE_POINTS
# warning "Disabling SAVE_REGISTERS because it _may_ interfere with the counting approach."
# undef SAVE_REGISTERS
# define SAVE_REGISTERS()
#endif

#ifdef CEXCEPTIONS
extern HEADER *cur_exc;
extern int cur_exc_dom;
#ifdef CEXCEPTIONS_COUNT
extern int n_exc_thrown;
#endif
#endif

// debugging support
extern int stored_line_number;

// GCC static branch prediction macros
// http://kerneltrap.org/node/4705

#define likely(x)       __builtin_expect((x),1)
#define unlikely(x)     __builtin_expect((x),0)
