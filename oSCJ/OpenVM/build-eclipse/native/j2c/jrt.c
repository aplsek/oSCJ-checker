#include "config.h"

#ifdef RTEMS_BUILD
//#include <bsp.h>
#include "networkconfig.h"
#endif

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <assert.h>
#ifndef RTEMS_BUILD
#include <sys/mman.h>
#endif
#include <errno.h>
#include <stdarg.h>
#ifndef RTEMS_BUILD
#include <sys/io.h>
#endif

// RAPITA SUPPORT
//#include "rpt.h"


#ifndef RTEMS_BUILD
// Xenomai
#include <string.h>
//#include <signal.h>
#include <execinfo.h>
#endif

#ifdef OVM_XENOMAI
#include <native/task.h>
#include <rtdk.h>
#endif

// on Linux, ucontext.h must be included with __USE_GNU (which must be
// turned on by _GNU_SOURCE) to support register access constants like
// REG_EIP if ucontext is included recursively before, we are screwed

#ifdef LINUX_BUILD
  #ifndef _GNU_SOURCE
    #ifdef _SYS_UCONTEXT_H
      #error "ucontext.h included without _GNU_SOURCE, please fix"
    #endif
    #define _GNU_SOURCE
    #undef _FEATURES_H
    #include <ucontext.h>
    #undef _GNU_SOURCE
  #else 
    #include <ucontext.h>
  #endif
#elif !defined(RTEMS_BUILD)
 #include <ucontext.h>
#endif 

#include "fdutils.h"
#include "j2c.h"



//extern "C" {
#include "engine.h"
#include "signalmanager.h"
#include "nativeScheduling.h"
#include "jvm_constants/throwables.h"
//}
#include "ffi.h"

#undef TRACE_THREADS
/* #define TRACE_THREADS */
#ifdef TRACE_THREADS
# define CTX(stuff...) ubprintf(stuff)
#else
# define CTX(stuff...) 
#endif

#ifdef CEXCEPTIONS
HEADER *cur_exc = 0;
int cur_exc_dom = 0;
#ifdef CEXCEPTIONS_COUNT
int n_exc_thrown = 0;
#endif
#endif

int stored_line_number = 0;

#define VERBOSE_DEBUG

// typedef e_java_lang_Object Object;
// typedef e_ovm_core_domain_Oop Oop;
// typedef e_ovm_core_execution_ValueUnion ValueUnion;
typedef e_ovm_core_services_memory_VM_1Address VM_Address;

typedef e_s3_services_j2c_J2cCodeFragment J2cCodeFragment;

// extern e_Array<jint> cf_ranges;
// extern e_Array<J2cCodeFragment *> cf_frags;

static float bits2float(int x) {
  union {
    int i;
    float f;
  } u;

  u.i = x;
  return u.f;
}

static double bits2double(long long x) {
  union {
    long long l;
    struct {
     int x;
     int y;
    } swap;
    double d;
  } u;

  u.l = x;
#if defined(OVM_ARM) && !defined(WORDS_BIGENDIAN)
  int z=u.swap.x;
  u.swap.x=u.swap.y;
  u.swap.y=z;
#endif
  return u.d;
}

double _j2c_NaN;
double _j2c_nzero;
float _j2c_NaNf;
float _j2c_nzerof;

int **initializedBlueprints;
J2cContext *currentContext;
J2cContext *mainContext;
int process_argc;
char **process_argv;
extern void boot();

#ifdef OVM_XENOMAI
extern int exit_process_code;
extern RT_TASK main_ovm_task;
#endif

typedef struct pcRange {
  size_t start;
  size_t end;
} pcRange;

//extern e_Array<pcRange> j2c_method_ranges;
typedef struct e_Array_pcRange {
  e_java_lang_Object _parent_;
  int length;
  pcRange values[1];
} e_Array_pcRange;
extern e_Array_pcRange j2c_method_ranges;

//#ifndef ARRAYLETS
# define MR_VALUES j2c_method_ranges.values
//#else
//# define MR_VALUES (*(pcRange **)j2c_method_ranges.values)
//#endif

//extern e_Array<e_s3_services_j2c_J2cCodeFragment *> j2c_method_pointers;
extern e_Array j2c_method_pointers;

static void compute_range_hints();
#ifdef PRECISE_BARRIER_PROF
extern e_Array<pcRange> barrier_prof_ranges;
#ifdef ARRAYLETS
#error Arraylets not supported with barrier profiling.
#endif
#endif
static void init_barrier_prof();

int pgsize;

//inline void *operator new(size_t sz, void *p) { return p; }

ffi_type *codeToType(int code) {
  switch (code) {
  case 'L':
  case '[':
    return &ffi_type_pointer;
  case 'J':
    return &ffi_type_sint64;
  case 'I':
    return &ffi_type_sint32;
  case 'S':
    return &ffi_type_sint16;
  case 'C':
    return &ffi_type_uint16;
  case 'B':
  case 'Z':
    return &ffi_type_sint8;
  case 'D':
    return &ffi_type_double;
  case 'F':
    return &ffi_type_float;
  case 'V':
    return &ffi_type_void;
  default:
    assert(!"unknown type code");
  }
}

//jlong j2cInvoke(Code *code, InvocationMessage *args) {
jlong j2cInvoke(HEADER *codeh, HEADER *argsh) {
  Code *code = (Code *)codeh;
  InvocationMessage *args = (InvocationMessage *)argsh;
  
  J2cCodeFragment *cc = (J2cCodeFragment *) code;
  ffi_cif cif;

  // Should this be an assert, or a check that we perform even with 
  // ndebug?
  assert(HEADER_BLUEPRINT(cc) == ((e_s3_core_domain_S3Blueprint*)&bp_e_s3_services_j2c_J2cCodeFragment));
  
  if (cc->ovm_index == -1) {
    fprintf(stderr, "reflective call to `unreachable' method \"%.*s\"\n",
	    ((e_java_lang_String *)cc->ovm_cname)->ovm_count,
#ifndef ARRAYLETS	    
	    ((e_java_lang_String *)cc->ovm_cname)->ovm_data->values + 
#else
	    (* (char **) (((e_java_lang_String *)cc->ovm_cname)->ovm_data->values)) + 
#endif	    
	    ((e_java_lang_String *)cc->ovm_cname)->ovm_offset);
    abort();
  }
  int nargs = 1 + args->ovm_inArgs->length;
  ffi_type *at[nargs];
  ffi_type *rt = codeToType(cc->ovm_rtype);
  at[0] = codeToType('L');
  int i;
  for (i = 1; i < nargs; i++) {
#ifndef ARRAYLETS  
    at[i] = codeToType(((e_ovm_core_execution_ValueUnion **)args->ovm_inArgs->values)[i-1]->ovm_tag);
#else
    at[i] = codeToType((*(e_ovm_core_execution_ValueUnion ***)args->ovm_inArgs->values)[i-1]->ovm_tag);    
#endif    
  }
  
  ffi_status pstatus = ffi_prep_cif(&cif, FFI_DEFAULT_ABI, nargs, rt, at);
  assert(pstatus == FFI_OK);

  void (*fn) () = (void (*)()) MR_VALUES [cc->ovm_index].start;

  void *av[nargs];
  typedef union {
    jbyte b;
    jchar c;
    jshort s;
  } narrow;
  narrow av_narrow[nargs];

  av[0] = &args->ovm_receiver;
  for (i = 1; i < nargs; i++) {
#ifndef ARRAYLETS  
    ValueUnion *ai = ((e_ovm_core_execution_ValueUnion **)args->ovm_inArgs->values)[i - 1];
#else
    ValueUnion *ai = (*(e_ovm_core_execution_ValueUnion ***)args->ovm_inArgs->values)[i - 1];
#endif    
    switch (ai->ovm_tag) {
    case 'L':
    case '[':
      av[i] = (void *) &ai->ovm_reference;
      break;
    case 'I':
    case 'F':
      av[i] = (void *) &ai->ovm_primitive;
      break;
    case 'D':
    case 'J':
      av[i] = &ai->ovm_widePrimitive;
      break;
    case 'Z':
    case 'B':
      av_narrow[i].b = ai->ovm_primitive;
      av[i] = (void *) &av_narrow[i].b;
      break;
    case 'S':
      av_narrow[i].s = ai->ovm_primitive;
      av[i] = (void *) &av_narrow[i].s;
      break;
    case 'C':
      av_narrow[i].c = ai->ovm_primitive;
      av[i] = (void *) &av_narrow[i].c;
      break;
    }
  }
  jvalue ret;
  HEADER *ex;
#ifdef COUNTER_EXCEPTIONS
  ffi_call(&cif, fn, &ret, av);
  if (CHECK_EXCEPTION()) {
      accurate::counterClearException();
      throwWildcardException(getCurrentException());
  }
  return ret.jv_jlong;

#elif defined CEXCEPTIONS

  cur_exc=(HEADER *)0;
  ffi_call(&cif, fn, &ret, av);
  
#if defined(PRECISE_SAFE_POINTS) && defined(PRECISE_THUNK)
  if (cur_exc && (cur_exc!=(HEADER *)&accurate::GCException_singleton)) {
#elif defined(PRECISE_SAFE_POINTS) && !defined(PRECISE_THUNK)
  if (accurate::gAuxStackHeight&ACC_EXCBIT)  {
#else
  if (cur_exc) {
#endif
    HEADER *caught_exception = cur_exc;
    cur_exc = 0;
#if defined(PRECISE_SAFE_POINTS) && !defined(PRECISE_THUNK)
    accurate::counterClearException();
#endif    

    /* this will just set the exception, the code
      will however continue executing at the following
      return statement

      for this to work, there must be no method call between 
      the call below and the return of this function */
    throwWildcardException(caught_exception);
    return 0;
  }
  return ret.jv_jlong;

#else
  int troubles=0;
  // FIXME: make this work with counter exceptions
  try { ffi_call(&cif, fn, &ret, av); }
  // Calling throwWildcardException from the handler
  // causes a rethrow of the original exception.
  // Jumping out of a catch block causes troubles on
  // GCC/ARM. So a flag is used instead.
  catch (HEADER *ex1) { ex = ex1; troubles=1; }

  if (troubles)
      throwWildcardException(ex);

  return ret.jv_jlong;
#endif
}

//jint j2cFail(...) {
jint j2cFail(void) {
  assert(!"abstract/boot-time/native method called\n");
  exit(-1);
}

sigset_t j2cNormalMask;

void startContext(J2cContext *);

#if defined(PRECISE_SAFE_POINTS)
extern void userGC(accurate::AuxStack *);
#endif

static void reallyStartTheDamnedThread() {
  J2cContext *ctx = currentContext;
  Code *code=ctx->code;
  InvocationMessage *args=ctx->args;
  ctx->code=0;
  ctx->args=0;
#ifdef PRECISE_PTRSTACK
  ctx->gcTop=ctx->ptrStack;
#endif
  //assertAddressValid(code);
  //assertAddressValid(args);
  j2cInvoke((HEADER *)code, (HEADER *)args);
}

void startContext(J2cContext *ctx) {
#ifndef THREAD_UCONTEXT
  // Initially wake up on our own stack
  if (!setjmp(ctx->buf))
    // Return to newNativeContext on the parent thread's stack
    longjmp(currentContext->buf, 1);
#endif
  CTX("%p starting\n", ctx);
  currentContext = ctx;
  ctx->bottomFrame = getCurrentActivation(0);
#if defined(PRECISE_SAFE_POINTS)
  ctx->localState.run(reallyStartTheDamnedThread, userGC);
#else
  reallyStartTheDamnedThread();
#endif

  write(2, "unexpected return from thread's initial method", 46);
  abort();
}

#ifdef RTEMS_BUILD
#define MEM2CTX(mem) 							\
  ((void *) (((char *) mem) + CONTEXT_SIZE - ((sizeof(J2cContext)+7)&~7) ))
#else
#define MEM2CTX(mem) 							\
  ((void *) (((char *) mem) + CONTEXT_SIZE + pgsize - ((sizeof(J2cContext)+7)&~7) ))
#endif

#ifdef RTEMS_BUILD
#define CTX2MEM(ctx) 							\
  (((char *) ctx) - CONTEXT_SIZE + ((sizeof(J2cContext)+7)&~7) )
#else
#define CTX2MEM(ctx) 							\
  (((char *) ctx) - CONTEXT_SIZE - pgsize + ((sizeof(J2cContext)+7)&~7) )
#endif

inline J2cContext* J2cContext_init(J2cContext *jc, Context *oc) {
#ifdef PRECISE_HENDERSON
    jc->gcTop = 0;
#elif defined(PRECISE_PTRSTACK)
    jc->gcTop = 0;
#elif defined(PRECISE_SAFE_POINTS)
    jc->shouldWalkStack = 0;
#endif
    jc->ovmContext = oc;  
    return jc;  
}


//int newNativeContext(Context *appCtx) {
int newNativeContext(HEADER *appCtxh) {
  Context *appCtx = (Context *)appCtxh;

  CTX("in newNativeContext, current thread %p has stack %p - %p\n", currentContext,
    currentContext->stackTop, currentContext->stackBase);

  CTX("in newNativeContext, main thread %p has stack %p - %p\n", mainContext,
    mainContext->stackTop, mainContext->stackBase);
    
#ifdef RTEMS_BUILD
  void *mem = malloc(CONTEXT_SIZE);

  if (mem==NULL) {
    write(2,"malloc failed while trying to allocate new context\n",51);
    abort();
  }
    
  // Place context at the end of the allocation,
  // the macros will keep it 8-byte aligned
  J2cContext *ctx;
  void *_ctx = MEM2CTX(mem);
  
  ctx = J2cContext_init(_ctx,appCtx);

  // Stack starts just below the context
  ctx->stackBase = (void *) (((size_t) ctx) - 8 );
  // keep stack base 8-byte aligned as well

  ctx->stackTop = (void *) (((char *) mem));
  CTX("thread %p has stack %p - %p\n", ctx, ctx->stackTop, ctx->stackBase);
#else
  // allocate stack-gaurd page
  void *mem = mmap(0, CONTEXT_SIZE + pgsize,
		   PROT_READ|PROT_WRITE|PROT_EXEC,
		   MAP_PRIVATE|MAP_ANON, -1, 0);

  if (mem==MAP_FAILED) {
    perror("mmap failed while trying to allocate new context:");
    abort();
  }

  // Place context at the end of the allocation,
  // the macros will keep it 8-byte aligned
  J2cContext *ctx;
  void *_ctx = MEM2CTX(mem);
  
//  ctx = new(_ctx) J2cContext(appCtx);
  ctx = J2cContext_init(_ctx,appCtx);

  // Stack starts just below the context and ends just above the gaurd page
  ctx->stackBase = (void *) (((size_t) ctx) - 8 );
  // keep stack base 8-byte aligned as well

  ctx->stackTop = (void *) (((char *) mem) + pgsize);
  CTX("thread %p has stack %p - %p\n", ctx, ctx->stackTop, ctx->stackBase);
  mprotect((void *) mem, pgsize, PROT_NONE);
#endif

#ifdef THREAD_UCONTEXT
  getcontext(&ctx->uc);
  // in which direction does the stack grow?  What is expected here?
  ctx->uc.uc_stack.ss_sp = ctx->stackTop;
  ctx->uc.uc_stack.ss_flags = 0;
  ctx->uc.uc_stack.ss_size = CONTEXT_SIZE - sizeof(*ctx);
  ctx->uc.uc_link = 0;
  makecontext(&ctx->uc, (void (*)()) startContext, 1, ctx);
#else
#define START(callee_save1, callee_save2, start_asm) {			\
    /* setjmp is going to return 1 long after this frame has died.	\
     * Only use one variable, ctx1, stored in a callee-save register.	\
     */									\
    register void *stackBase asm (#callee_save1) = ctx->stackBase;	\
    register J2cContext *ctx1 asm (#callee_save2) = ctx;		\
									\
    if (!setjmp(currentContext->buf))					\
      {									\
	/* FIXME: need underscore? factor into x86-specific header? */	\
	asm(start_asm  : : "r"(stackBase), "r"(ctx1));			\
	write(2, "unexpected return from startContext\n", 36);		\
	abort();							\
      }									\
  }
    
# if defined(OVM_X86)
#  ifdef ASM_NEEDS_UNDERSCORE
#    define startContextSym "_startContext"
#   else
#     define startContextSym "startContext"
#  endif
  START(edi, esi,
	"andl $-16, %0\n"
	"movl %0, %%esp\n"
	"movl %0, %%ebp\n"
	"subl $12, %%esp\n"
	"pushl %1\n"
	"call " startContextSym "\n"
	);
# elif defined(OVM_PPC)
  /*
   * Here's what I know:
   * r3-r12 are argument registers
   * r13    is the first callee-save register
   * r1     is the stack pointer
   *
   * bl places the return address in a special reg `lr'
   * gcc's prologue saves it at 8(r1) (in the caller's frame), and
   * stores the caller's stack pointer at 0(r1) (in the callee's
   * frame).
   *
   * We need to allocate some space in the bottom frame. We need
   * enough space to hold the frame information and spill the three
   * arguments to write.  I guess that would be 5 words, but it should
   * be rounded up to 8 to satisfy any alignment requirements.
   * (It looks like gcc aligns r1 to quadword boundaries.)
   *
   * I don't understand the PIC magic to get the function's base
   * address into r31
   */
#  ifdef OSX_BUILD
  START(r16, r15,
	"addi r1, %0, -32\n"
	"stw  r1, 0(r1)\n"
	"mr   r3, %1\n" 
	"bl   _startContext");
#  else
  /* The thread-launching asm for ppc-linux seems to work, but unless
   * we use swapcontext, certain library functions such as malloc can
   * only be called from the main thread.
   */
  START(%r16, %r15,
	"addi %%r1, %0, -32\n"
	"stw  %%r1, 0(%%r1)\n"
	"mr   %%r3, %1\n" 
	"bl   startContext");
#  endif
# elif defined (OVM_ARM)
#  if defined(ARM_THUMB)
    START(r4,r5,
	  "mov   sp,%0\n"
	  "mov   r0,%1\n"
	  "push  {r7, lr}\n"
	  "sub   sp, sp, #32\n"
	  "add   r7, sp, #0\n"
	  "bl    startContext");
#  else
    START(r4,r5,
	  "mov   sp,%0\n"
	  "mov   r0,%1\n"
          "mov   ip, sp\n"
	  "stmfd sp!, {fp, ip, lr, pc}\n"
	  "sub   fp, ip, #4\n"
	  "sub   sp, sp, #32\n"
	  "bl    startContext");
#  endif
# elif defined (OVM_SPARC)

// The final "mov" is in the delay slot.
// 64 bytes must always be available to save in and local regs.
// as I'm moving the stack, and that interferes with the register
// windows flushing, I also save explicitly ret addr and fp.
// That *should* be enough for stack traversal. Even though
// it is not really necessary, I save all local and in regs

// ta 3 is ta ST_FLUSH_WINDOWS

  START(%l0,%l1,
	  "ta 3\n"

	  "sub  %0,128,%0\n"
	  "mov  %0,%%sp\n"

	  "st   %%l0,[%%sp+ 0]\n"
	  "st   %%l1,[%%sp+ 4]\n"
	  "st   %%l2,[%%sp+ 8]\n"
	  "st   %%l3,[%%sp+12]\n"

	  "st   %%l4,[%%sp+16]\n"
	  "st   %%l5,[%%sp+20]\n"
	  "st   %%l6,[%%sp+24]\n"
	  "st   %%l7,[%%sp+28]\n"

	  "st   %%i0,[%%sp+32]\n"
	  "st   %%i1,[%%sp+36]\n"
	  "st   %%i2,[%%sp+40]\n"
	  "st   %%i3,[%%sp+44]\n"

	  "st   %%i4,[%%sp+48]\n"
	  "st   %%i5,[%%sp+52]\n"
	  "st   %%i6,[%%sp+56]\n"
	  "st   %%i7,[%%sp+60]\n"

	  "call startContext\n"
	  "mov  %1,%%o0")

# else
#  error "Don't know how to start threads on this system"
# endif
#endif
  return (int) ctx;
}

void destroyNativeContext(int _ctx) {
  J2cContext *ctx = (J2cContext *) _ctx;
  if (ctx != mainContext) {
#ifndef NDEBUG
      ctx->ovmContext = 211; // debug
#endif
    
#ifdef RTEMS_BUILD  
  free(CTX2MEM(ctx));
#else
  munmap(CTX2MEM(ctx), CONTEXT_SIZE + pgsize);    
#endif  
  }
}

void run(int processorHandle, int _ctx) {
 top: {
 
  J2cContext *ctx = (J2cContext *) _ctx;
  assert(processorHandle == 0);
  J2cContext *me = currentContext;

  if (ctx == me) {
    CTX("skipping switch %p -> %p\n", ctx, ctx);
  } else {
    // topFrame is run(), top java frame's PC available here
    me->topFrame = getCurrentActivation(0);


    CTX("swap %p -> %p\n", me, ctx);

#ifdef THREAD_SETJMP
    if (setjmp(me->buf)) {
      currentContext = me;
    } else {
      longjmp(ctx->buf, 1);
    }
#else
    swapcontext(&me->uc, &ctx->uc);
    currentContext = me;
#endif

    CTX("%p waking up\n", me);
  }
  }

#if defined(PRECISE_SAFE_POINTS)
  me->localState.makeCurrent();
  if (me->shouldWalkStack) {
      CTX("should walk stack!\n");
      me->shouldWalkStack = 0;
      me->localState.gc();

#if defined(CEXCEPTIONS)
#if defined(PRECISE_THUNK)
      if (cur_exc) {
        CTX("gc threw an exception.\n");
        return ;
      }
#else
      if (accurate::gAuxStackHeight>=ACC_SPECIAL_THRESHOLD) {
        CTX("gc special return (exception)");
        return ;
      }
#endif

#else /* not CEXCEPTIONS */
      CHCKRET();
#endif      

      _ctx = nextThreadToWalk();
      goto top;
  } else {
      CTX("not walking any stacks today.\n");
  }
#endif
}

//jint makeActivation(jint _ctx, Code *cf, InvocationMessage *invocation)
jint makeActivation(jint _ctx, HEADER *cfh, HEADER *invocationh)
{
  Code *cf = (Code *)cfh;
  InvocationMessage *invocation = (InvocationMessage *)invocationh;
  
  J2cContext  *ctx = (J2cContext *) _ctx;
  /*
  if (ctx->code)
    {
      fprintf(stderr, "makeActivation called after thread start\n");
      abort();
    }
  */
  CTX("in makeActivation\n");
  //assertAddressValid(cf);
  //assertAddressValid(invocation);
  ctx->code = cf;
  ctx->args = invocation;
  return 0;
}

void launchContext(void *ctx) {
  assert(!"not supported");
}

#if defined(BOOTBASE)
static ImageFormat *image = (ImageFormat *) BOOTBASE;
#elif defined(FORCE_IMAGE_LOCATION) && FORCE_IMAGE_LOCATION!=0
extern int OVMImage;
static ImageFormat *image = (ImageFormat *) FORCE_IMAGE_LOCATION;
#else
static ImageFormat *image;
#endif

void *getImageBaseAddress() {
//  ??? return (void*) ((char*)image->data + 4); /* first word is null */
  return (void*) (image->data); 
}

void *getImageEndAddress() {
  int off = image->usedMemory - 4; /* last word is magic number */
  return (void *) ((char *)image->data + off);
}

const int signalsReservedByExecEngine[] = {
  /* add comma-separated list of signals that J2c uses for aborting or
   * regular execution here, but before the 0.  see interpreter/main.c
   * for an example. */
#ifdef PRECISE_SAFE_POINTS
  PRECISE_RESERVED_SIGNAL,
#endif
  0
};

jlong getNow() {
// This code is similar to getCurrentTime()
// in src/native/common/native_helpers.c
#if _POSIX_TIMERS > 0 || defined(RTEMS_BUILD)
    struct timespec tm;
#ifdef RTEMS_BUILD
    int status = 0;
    _TOD_Get(&tm);
#else
    int status = clock_gettime(CLOCK_REALTIME, &tm);
#endif
    jlong nanos_per_sec = 1000L * 1000 * 1000;
    jlong now = tm.tv_sec * nanos_per_sec + tm.tv_nsec;
    if (status != 0) {
        ASSERT(status == -1 && errno == EINVAL, "unexpected error from clock_getttime");
        return status;
    }
    return now;
#else
     struct timeval now;
     jlong micros_per_sec = 1000000;
     jlong nowNanos = 0;
     gettimeofday(&now, NULL);
/*      printf("Using gettimeofday()\n");  */
     nowNanos = (now.tv_usec + now.tv_sec * micros_per_sec) * 1000;
     return nowNanos;
#endif
}

#if defined(SOLARIS_BUILD)
// on solaris, mmap totally ignores the supplied address,
// unless MAP_FIXED is used. OTOH, MAP_FIXED destroys
// previous existing mappings. The very convoluted code
// below detects whether the address space requested is
// actually free, recycling the code at
// http://www.winehq.com/hypermail/wine-devel/2000/11/0202.html

#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <alloca.h>
#include <stdio.h>
#include <string.h>
#include <signal.h>
#include <procfs.h>

static int is_mapped_test (uintptr_t vaddr, size_t size,
			   const prmap_t *asmap, int n)
{
  int i = 0, j = n;

  while (i < j)
  {
    int m = (i + j) / 2;
    const prmap_t *o = &asmap[m];

    if ((uintptr_t) o->pr_vaddr >= vaddr + size)
      j = m;
    else if ((uintptr_t) o->pr_vaddr + o->pr_size <= vaddr)
      i = m + 1;
    else
      return 1;
  }

  return 0;
}


static void *safe_mmap (void *addr, size_t len, int prot, int flags,
			int fildes, off_t off)
{
  if (flags & MAP_FIXED)
    return mmap (addr, len, prot, flags, fildes, off);
  else
  {
    int stat = 0;
    pid_t pid;
    int fd;
    struct stat sb;
    prmap_t *asmap;
    void *actual_addr;

    fd = open ("/proc/self/rmap", O_RDONLY);
    assert (fd != -1);
    if ((pid = vfork ()) == -1)
    {
      perror ("is_mapped: vfork");
      abort ();
    }
    else if (pid == 0)
    {
      fstat (fd, &sb);
      asmap = (prmap_t *) alloca (sb.st_size);
      read (fd, asmap, sb.st_size);
      if (is_mapped_test ((uintptr_t) addr, len, asmap,
			  sb.st_size / sizeof (prmap_t)))
	_exit (EADDRINUSE);
      else if ((actual_addr = mmap (addr, len, prot, flags | MAP_FIXED,
				    fildes, off)) == (void *) -1)
	_exit (errno);
      else if (actual_addr != addr)
      {
	munmap (actual_addr, len);
	kill (getpid (), SIGKILL);
      }
      else
      {
	_exit (0);
      }
    }
    else if (waitpid (pid,  &stat, WNOHANG) != pid)
    {
      perror ("is_mapped: waitpid");
      abort ();
    }
    close (fd);
    if (!WIFEXITED (stat))
      return mmap (addr, len, prot, flags, fildes, off);
    else if (WEXITSTATUS (stat) == 0)
      return addr;
    else if (WEXITSTATUS (stat) == EADDRINUSE)
      return mmap (addr, len, prot, flags, fildes, off);
    else
    {
      errno = WEXITSTATUS (stat);
      return (void *) -1;
    }
  }
}
#endif

#ifdef OVM_XENOMAI
RT_TASK task;

int real_argc;
char **real_argv;

void task_body (void *cookie) {

    /* Ask Xenomai to warn us upon switches to secondary mode. */
//    rt_task_set_mode(0, T_WARNSW, NULL);

    /* A real-time task always starts in primary mode. */

//  fprintf(stderr, "About to call real main from OVM Xenomai task...\n");
  fflush(stderr);
    
    real_main(real_argc,real_argv);
    /* the real main in OVM does not return */
}

void warn_upon_switch(int sig __attribute__((unused))) {

    void *bt[32];
    int nentries;

    /* Dump a backtrace of the frame which caused the switch to
       secondary mode: */
    nentries = backtrace(bt,sizeof(bt) / sizeof(bt[0]));
    backtrace_symbols_fd(bt,nentries,fileno(stdout));
    
    /*
    This only makes sense when the switches are rare... very rare
    
    fprintf(stderr,"Switching back to primary mode.\n");
    rt_task_set_mode(0, T_PRIMARY, NULL);
    */
}

int main(int argc, char **argv) {

  int err;

  real_argc = argc;
  real_argv = argv;
  
  /* this should be called automatically from a library constructor, but with current
     linking in OVM, it is not */
//  __rt_print_init();
  
    signal(SIGXCPU, warn_upon_switch);
    mlockall(MCL_CURRENT|MCL_FUTURE);
  
    rt_print_auto_init(1);

    err = rt_print_init(4096, "ovmtask");
    if (err) {
      fprintf(stderr,"Failed to alllocate RT print buffer, code %d (%s)\n", err, strerror(-err) );
      exit(2);
    }
  
//  err = rt_task_create(&task,"ovmtask",0,11,T_FPU|T_JOINABLE);  

//  this normally works, but in case there is no blocking calls (i.e. a
//  simple serial port polling application), it locks up the system via
//  starving Linux

//  err = rt_task_create(&task,"ovmtask",0,11,T_FPU);  

  // for a reason unclear to me, 0 does not starve Linux
  
  err = rt_task_create(&task,"ovmtask",0,0,T_FPU);  

  if (err) {
    fprintf(stderr,"Failed to create Xenomai OVM task, code %d (%s)\n", err, strerror(-err) );
    exit(2);
  }
  
  // this task will only sleep and wait for the end ; thus make sure it gets to the sleep before OVM ends
  err = rt_task_shadow( &main_ovm_task, NULL, 99, 0 );

  if (err) {
    fprintf(stderr,"Shadowing bootstrap task failed, code %d (%s)\n", err, strerror(-err));
    exit(2);
  }  


//  fprintf(stderr, "Running Xenomai OVM task...\n");
  fflush(stderr);
  
  err = rt_task_start(&task,&task_body,NULL);

  if (err) {
    fprintf(stderr,"Running Xenomai OVM task failed, code %d (%s)\n", err, strerror(-err) );
    rt_print_cleanup();    
    exit(2);
  }

  err = rt_task_suspend( &main_ovm_task );
  if (err) {
    fprintf(stderr,"Bootstrap task failed to suspend/resume, code %d (%s)\n", err, strerror(-err));
  }
  
  if (exit_process_code==-1) {
    fprintf(stderr, "BUG: Bootstrap task was resumed too early, switching to active waiting !\n");
  }
  
  /* very nasty workaround */
  while(exit_process_code==-211) {
    sleep(1);
  }

/*  
  err =  rt_task_join(&task);

  if (err) {
    fprintf(stderr,"Joining Xenomai OVM task failed, code %d\n",err);
    exit(2);
  }
*/

/*
  main_ovm_task = rt_task_self();
  err = rt_task_suspend(main_ovm_task);
  if (err) {
    perror("Failed to suspend bootstrap OVM task, rt_task_suspend");
    fprintf(stderr, "rt_task_suspend returned %d.\n", err);
    exit(2);
  }  
*/  
  fprintf(stderr, "Xenomai task finished.\n");
  return exit_process_code;
}

#endif

#ifdef LOAD_FS_IMAGE

#include <rtems/untar.h>

extern char _binary_FilesystemImage_start;
extern char _binary_FilesystemImage_end;

rtems_status_code load_filesystem_image() {

    rtems_status_code status;

    void * embedded_start = &_binary_FilesystemImage_start;
    void * embedded_end = &_binary_FilesystemImage_end;
    size_t embedded_size = ((char *)embedded_end) - ((char *)embedded_start) + 1;

    
   
    fprintf(stderr,"about to load fs-image which is at 0x%x, it's size is %d\n",
      embedded_start, embedded_size
    );
    status = Untar_FromMemory( embedded_start, embedded_size );
    
    if (status) {
        fprintf(stderr, "Unpacking of the file system image has failed with code %d\n", status);
    }
    
    return status;
}

#endif

#define CMDLINE_BUFFER_LEN	4096
char cmdline_buffer[CMDLINE_BUFFER_LEN];


#if defined(OVM_XENOMAI) || defined(RTEMS_BUILD)

int real_main(int argc, char **argv) {

#else
// no Xenomai
int main(int argc, char **argv) {
#endif

  int i,bytesleft,alen;
  char *aptr;

//  if (mlockall(MCL_CURRENT|MCL_FUTURE)!=0) {
//    perror("mlockall failed: ");
//    abort();
//  }
  
  process_argc = argc;
  process_argv = argv;
  
  if ((process_argc == 2) && (!strcmp(process_argv[1],"-stdin-cmdline"))) {
  
    // initial handshake
    for(;;) {
      fprintf(stdout, "OVM-WAITING-FOR-CMDLINE\n");
      fgets(cmdline_buffer, CMDLINE_BUFFER_LEN, stdin);
      if (!strncmp(cmdline_buffer,"CMDLINE-FOLLOWS",15)) {
        break;
      }
    }
    
    // number of arguments
    fgets(cmdline_buffer, CMDLINE_BUFFER_LEN, stdin);
    process_argc = atoi(cmdline_buffer) + 1;
    process_argv = (char **)malloc( (process_argc+1) * sizeof(char *) );
    if (process_argv==NULL) {
      fprintf(stderr, "Cannot allocate array for process arguments read from standard input.\n");
      abort();
    }

    process_argv[0]="ovm-stdin-cmdline";
    process_argv[process_argc]=NULL;
    
    // giving arguments one per line
    
    bytesleft = CMDLINE_BUFFER_LEN;
    aptr = cmdline_buffer;
    
    for(i=1;i<process_argc;i++) {

      aptr[0]=0;
      fgets(aptr, bytesleft, stdin);

      alen = strlen(aptr);
      
      if (alen>1) {
        aptr[alen-1]=0;
        alen--;
      } else {
        fprintf(stderr, "Argument did not end with newline.\n");
        abort();
      }

      bytesleft -= (alen+1);
      if (bytesleft<0) {
        fprintf(stderr, "Run out of buffer space from command line read from standard input.\n");
        abort();
      }

      process_argv[i] = aptr;
      aptr = aptr + alen + 1;
    }
  }
  
  if (1) {
    fprintf(stderr,"Starting OVM with %d command line arguments:\n", process_argc);
    for(i=0;i<process_argc;i++) {
      fprintf(stderr, "  Arg %d: %s\n", i, process_argv[i]);
    }
  }

#ifdef VERBOSE_DEBUG
  printf("starting ovm!\n");
#endif

#ifdef LOAD_FS_IMAGE
#ifdef VERBOSE_DEBUG
  printf("loading FS image\n");
#endif      
    load_filesystem_image();
#endif    

  
#if defined(OVM_X86) && (defined(LINUX_BUILD))

  // set IO privilege level on x86 to allow port access, if possible
  int res = iopl(3);
  
  if (!res) {
//    fprintf(stderr, "I/O privilege level set to 3\n");
  } else {
    fprintf(stderr, "WARNING: Could not set I/O privilege level - port access will cause SEGFAULT.\n");
  }

#endif

// cheap workaround to a minor qemu deficiency
// remove the ifndef when debug is complete.         FIXME
#if 0
  long long now=getNow();
  printf("Initial nanoseconds from the Epoch: %lld  (0x%016llX)\n",now,now);
  fflush(stdout);
#endif
  
  _j2c_NaN =   bits2double(0x7ff8000000000000LL);
  _j2c_nzero = bits2double(0x8000000000000000LL);
  _j2c_NaNf =   bits2float(0x7fc00000);
  _j2c_nzerof = bits2float(0x80000000);
  
  pgsize = getpagesize();

  
#if defined(RTEMS_BUILD)

#if defined(FORCE_IMAGE_LOCATION) && FORCE_IMAGE_LOCATION!=0

  
  /* image location already set */
  
  fprintf(stderr,"Image expected at 0x%x,and it is at 0x%x. Please update your hacks and try again.\n",
	    FORCE_IMAGE_LOCATION, &OVMImage);
  
  if (&OVMImage != ((int *)FORCE_IMAGE_LOCATION)) {
    fprintf(stderr,"Image expected at 0x%x, but it is at 0x%x. Please update your hacks and try again.\n",
	    FORCE_IMAGE_LOCATION, &OVMImage);
    exit(1);
  }
  
  
  
#ifdef VERBOSE_DEBUG  
  unsigned char *imgBytes = (unsigned char *)&OVMImage;
  fprintf(stderr,"Trying to read first 4 bytes of image:\n");
  fprintf(stderr,"image[0] at 0x%x = 0x%x\n", imgBytes, imgBytes[0]);
  fprintf(stderr,"image[1] at 0x%x = 0x%x\n", imgBytes+1, imgBytes[1]);
  fprintf(stderr,"image[2] at 0x%x = 0x%x\n", imgBytes+2, imgBytes[2]);
  fprintf(stderr,"image[3] at 0x%x = 0x%x\n", imgBytes+3, imgBytes[3]);
#endif  
  
#else
#error "When using RTEMS you must use FORCE_IMAGE_LOCATION (use the --with-image-location option in configure)"
#endif

#elif !defined(BOOTBASE)
  if (process_argc < 2) {
    fprintf(stderr, "usage: %s <image> arguments\n", argv[0]);
    exit(1);
  }
  int fd = open(process_argv[1], O_RDONLY);
  if (fd < 0) {
    perror("opening image file");
  }
  process_argv++;
  process_argc--;
  ImageFormat tmpImage;
  read(fd, &tmpImage, sizeof(tmpImage));
  lseek(fd, 0, SEEK_SET);
  int length = lseek(fd, 0, SEEK_END);
  /* Round up request to a multiple of page size.  The remainder of
   * the last page will be zero-filled.  Not rounding up may cause
   * the OS to round down.
   */
  length = (length + pgsize - 1) & ~(pgsize - 1);

#if defined(SOLARIS_BUILD)
  void *_image = safe_mmap((void *) tmpImage.baseAddress,
		      length,
		      PROT_READ|PROT_WRITE|PROT_EXEC,
		      MAP_PRIVATE,
		      fd,
		      0);
#else

  void *_image = mmap((void *) tmpImage.baseAddress,
		      length,
		      PROT_READ|PROT_WRITE|PROT_EXEC,
		      MAP_PRIVATE,
		      fd,
		      0);
		      
#endif
  if (_image != (void *) tmpImage.baseAddress) {
    perror("could not map image to base address");
    exit(1);
  }

  image = (ImageFormat *) _image;
  if (image->mainObject != &mainObject
      || image->coreServicesAccess != &coreServicesAccess
      || image->bootContext != &bootContext) {
    fputs("Image version does not agree with compiled code\n", stderr);
    exit(-1);
  }
#endif /* BOOTBASE */


  if (process_argc > 1 && !strcmp(process_argv[1], "-dump-method-sizes")) {
    int nRanges = (j2c_method_pointers.length);
    pcRange *range = &MR_VALUES[0];
    e_s3_services_j2c_J2cCodeFragment **method =
//#ifndef ARRAYLETS    
      &(((e_s3_services_j2c_J2cCodeFragment **)j2c_method_pointers.values)[0]);
//#else
//      &((*(e_s3_services_j2c_J2cCodeFragment ***)j2c_method_pointers.values)[0]);      
//#endif    

    int i;
    for (i = 0; i < nRanges; i++)
      printf("%5d\t%s\n",
	     (range[i].end - range[i].start + 31) & -32,
#ifndef ARRAYLETS	     
	     ( ((e_java_lang_String *)method[i]->ovm_cname)->ovm_data->values
#else
	     ( ( *(char **)((e_java_lang_String *)method[i]->ovm_cname)->ovm_data->values) 
#endif	     
	      + ((e_java_lang_String *)method[i]->ovm_cname)->ovm_offset));
    exit(0);
  }
  
fprintf(stderr, "[DBG] initializedBlueprints - malloc starting.\n");

	initializedBlueprints = (int **) malloc(3 * sizeof(int));

fprintf(stderr, "[DBG] initializedBlueprints - malloc ok.!\n");

  if (initializedBlueprints == NULL) {
    fprintf(stderr,"malloc failed while trying to allocate initializedBlueprints - 3 pointers to integer...\n");
    abort();
  }
    

  initializedBlueprints[0] = 0;
  
  fprintf(stderr, "[DBG] set to 0, ok.!\n");
  
  //int abc = ((e_s3_core_domain_S3TypeContext *) ( (( e_s3_core_domain_S3Domain*)&u1)->ovm_context_1))->ovm_blueprintCount;
  fprintf(stderr, "[DBG] my test.!\n");
  //fprintf(stderr, "[DBG] blueprint count is %d!\n", abc);
   //fprintf(stderr, "[DBG] contex 1 : %d.!\n", (( e_s3_core_domain_S3Domain*)&u1)->ovm_context_1));
  
  
  initializedBlueprints[1] =
    (int *) malloc(((e_s3_core_domain_S3TypeContext *) ( (( e_s3_core_domain_S3Domain*)&u1)->ovm_context_1))->ovm_blueprintCount/8 + 1);

    fprintf(stderr, "[DBG] blueprin 1, ok.!\n");
    
  
  if (initializedBlueprints[1] == NULL) {
    fprintf(stderr,"malloc failed while trying to allocate initializedBlueprints[1]\n");
    abort();
  }
      
      fprintf(stderr, "[DBG] memset blueprin 1....!\n");
      
  memset(initializedBlueprints[1], 0, ((e_s3_core_domain_S3TypeContext *)( (( e_s3_core_domain_S3Domain *)&u1)->ovm_context_1))->ovm_blueprintCount/8+1);
  
  fprintf(stderr, "[DBG] memset blueprin 1.... OK!\n");
  
  
  
  
  initializedBlueprints[2] =
    (int *) malloc(((e_s3_core_domain_S3TypeContext *)u1.ovm_appContext_1)->ovm_blueprintCount/8+1);
  if (initializedBlueprints[2] == NULL) {
    fprintf(stderr,"malloc failed while trying to allocate initializedBlueprints[2]\n");
    abort();
  }
      
  fprintf(stderr, "[DBG] init blueprin 2.... OK!\n");
    
    
    
//      fprintf(stderr, "[DBG] init Blueprints 1 ok.!\n");
//    
//int abc = (((e_s3_core_domain_S3TypeContext *) ( (( e_s3_core_domain_S3Domain*)&u1)->ovm_context_1))->ovm_blueprintCount);
//
  //fprintf(stderr, "[DBG] init abc ok  abs is:%d.!\n", abc);
//int def = (((e_s3_core_domain_S3TypeContext *) ( (( e_s3_core_domain_S3Domain*)&u1)->ovm_context_1))->ovm_blueprintCount/8 + 1);
//
//fprintf(stderr, "[DBG] initializedBlueprints[1] - count1 = %d\n", abc);
//fprintf(stderr, "[DBG] initializedBlueprints[1] - count2 = %d\n", def);
//fprintf(stderr, "[DBG] initializedBlueprints[1] - addr   = %d\n", initializedBlueprints[1]);
    
 // if (initializedBlueprints[1] == NULL) {
 //   fprintf(stderr,"malloc failed while trying to allocate initializedBlueprints[1]\n");
 //   abort();
 // }
  //
 //      
  //memset(initializedBlueprints[1], 0, ((e_s3_core_domain_S3TypeContext *)( (( e_s3_core_domain_S3Domain *)&u1)->ovm_context_1))->ovm_blueprintCount/8 + 1);
  //initializedBlueprints[2] =
  //  (int *) malloc(((e_s3_core_domain_S3TypeContext *)u1.ovm_appContext_1)->ovm_blueprintCount/8 + 1);
//
 //   
//fprintf(stderr, "[DBG] initializedBlueprints[2] - count1 = %d\n", abc);
//fprintf(stderr, "[DBG] initializedBlueprints[2] - count2 = %d\n", def);
//fprintf(stderr, "[DBG] initializedBlueprints[2] - addr  = %d\n", initializedBlueprints[2]);
 // 
  //fprintf(stderr, " [DBG] initializedBlueprints - after malloc.!\n");

  //  if (initializedBlueprints[2] == NULL) {
  //  fprintf(stderr,"malloc failed while trying to allocate initializedBlueprints[2]\n");
  //  abort();
  //}
      
      
      
      fprintf(stderr, "[DBG] setting the memory - blueprint 2.!\n");
   
      
      
      
      
      
      
  memset(initializedBlueprints[2], 0,
	 ((e_s3_core_domain_S3TypeContext *)u1.ovm_appContext_1)->ovm_blueprintCount/8 + 1);

	fprintf(stderr, "[DBG] memset OK.!\n"); 
	 
  EXIT_ERRNO(initSignalHandling(),"initSignalHandling");

  compute_range_hints();

  init_barrier_prof();
  
#if defined(PRECISE_SAFE_POINTS) && !defined(PRECISE_THUNK)
  accurate::initCounterSignalEvent();
#endif

#if !defined(OVM_XENOMAI) && !defined(RTEMS_BUILD)
  initializeNativeScheduling();
#endif

  static contextInitialized = 0;
  static J2cContext c;
  if (!contextInitialized) {
    J2cContext_init(&c,(e_ovm_core_execution_Context *)&bootContext); 
    contextInitialized = 1;
  }
  
  currentContext = mainContext = &c;
  mainContext->bottomFrame = getCurrentActivation(0);
  mainContext->stackBase = (void *) getCurrentActivation(0);
  
  // FIXME: this does not work
#if 0
  // we probably could leave this undefined, but... this seems to help debuggability
  
  mainContext->stackTop = CTX2MEM( ((size_t)(mainContext->stackBase)) + 8 ); // passing it where the context would normally be
  
  // normal context created by newNativeContext, x86 - stack grows down to numerically lower addresses
  /*
    (numerically higher addresses)
    -----------
    - ovmContext
      (includes ptr stack)
      
    -> J2cContext context pointer
    
    -> stackBase pointer
      ... stack  ...
    -> stackTop pointer
  */
  
  // main context created here
  /*
    ovmContext (main context) is anywhere in the memory, 
    not adjacent to the main context's stack
  */
  
  // and this should crash the system if there is not enough stack available to us
  
  ubprintf("Setting main context stack size. If this crashes, consider using ulimit -s...");
  *((char *)(mainContext->stackTop)) = *((char *)(mainContext->stackBase)); // touch also the stack base
  *((char *)(mainContext->stackTop)) = 0; // if this crashed, increase OS stack size (ulimit -s)
  ubprintf("\tOK, did not crash.\n");
  
#endif
  
  ((e_ovm_core_execution_Context *)&bootContext)->ovm_nativeContextHandle_1 = (jint) mainContext;

#ifdef PRECISE_PTRSTACK
  c.gcTop=c.ptrStack;
#endif

#ifdef OVM_XENOMAI
// enable this when catching switches to secondary domain  
//  rt_task_set_mode(0, T_PRIMARY|T_WARNSW, NULL);

  rt_task_set_mode(0, T_PRIMARY, NULL);
#endif  

#ifdef VERBOSE_DEBUG
  printf("booting\n");
#endif
  
  
#if defined(PRECISE_SAFE_POINTS)
  mainContext->localState.run(boot, userGC);
#else
  boot();
#endif
}

#ifdef RTEMS_BUILD

#ifndef OVM_APP_ARG0
#define OVM_APP_ARG0 NULL
#endif
 
#ifndef OVM_APP_ARG1
#define OVM_APP_ARG1 NULL
#endif

#ifndef OVM_APP_ARG2
#define OVM_APP_ARG2 NULL
#endif

#ifndef OVM_APP_ARG3
#define OVM_APP_ARG3 NULL
#endif


char *dummy_argv[] = { "rtems-ovm", OVM_APP_ARG0, OVM_APP_ARG1, OVM_APP_ARG2, OVM_APP_ARG3, NULL }; 

rtems_task ovm_task_main(rtems_task_argument _) {
  int cnt = 0;
  
  while( (dummy_argv[cnt] != NULL) && (dummy_argv[cnt][0] != 0)  ) {
    cnt++;
  }
  dummy_argv[cnt] = NULL;
  
  real_main( cnt, dummy_argv);
  exit(0);
}

#endif

inline static int is_array(S3Blueprint* bpt) {
    return bpt->ovm_arrayDepth_1 > 0;
}

static S3Blueprint* strip_array_blueprint(S3Blueprint* bpt, int howManyTimes) {
    int i;
    assert(howManyTimes <= bpt->ovm_arrayDepth_1);
    for (i = 0; i < howManyTimes; i++) {
	bpt = (S3Blueprint *)
	  ((e_s3_core_domain_S3Blueprint_Array*)bpt)->ovm_componentBlueprint_1;
    }
    return bpt;
}

//jboolean is_subtype_of(S3Blueprint* client, S3Blueprint* provider) {
jboolean is_subtype_of(HEADER* clienth, HEADER* providerh) {

	//printf("\nrapitat out\n");
	//(RAPITA_OUT =(133 + 0x8000) & 0xffff);


    S3Blueprint *client = (S3Blueprint*)clienth;
    S3Blueprint *provider = (S3Blueprint*)providerh;

    if (client == provider) { /* equality should work for primitives */
	return 1;
    }
    assert(client->ovm_typeDisplay_1);

    // if client is primitive, typeDisplay_.length == 0
    if (provider->ovm_typeBucket_1 >= client->ovm_typeDisplay_1->length)
      return 0;
    
#ifndef ARRAYLETS    
    if (client->ovm_typeDisplay_1->values[provider->ovm_typeBucket_1] 
#else
    if ( ( *(char **)client->ovm_typeDisplay_1->values) [provider->ovm_typeBucket_1] 
#endif    
	== provider->ovm_typeBucketID_1) {
	return 1;
    } 
    /*	printf("failed display[%d] = %d != %d\n",
 	provider->typeBucket_,
 	client->typeDisplay_->values[provider->typeBucket_]
 	provider->typeBucketID_);
    */
    
    if (!is_array(provider)) {
	return 0; /* the test was subsumed */ 
    } else { /* provider is an array, test doesn't matter */
	if (!is_array(client)) {
	    return 0;
	} else {
	    int cdepth = client->ovm_arrayDepth_1;
	    int pdepth = provider->ovm_arrayDepth_1;
	    if (pdepth > cdepth) { 
		return 0;
	    } else { /* pdepth <= cdepth */
		return is_subtype_of((HEADER *)strip_array_blueprint(client, pdepth),
				     (HEADER *)strip_array_blueprint(provider, pdepth));
	    }
	}
    } 
}

void did_init(jint ctx, jint bp) {
  // Repeatedly do CSA calls during startup so that the test in
  // needs_init will be fast later on.
  if (!((CSA *) ( ((e_s3_core_domain_S3Domain*)&u1)->ovm_myCSA))->ovm_classInitializationEnabled)
    return;
  int w = bp >> 5;
  int b = 1 << (bp & 31);
  initializedBlueprints[ctx][w] |= b;
}

int stacktrace(NativeContext *_) {
  abort();
  return 0;
}

static size_t range_offset;	// first java bytecode pc
static size_t range_size;	// last java bytecode pc - range_offset

/*
 * range_hints is essentially a hashtable that tells us the first
 * method that a pc may be in.  RANGE_HASH(pc - range_offset) gives us
 * the higher-order bits of the difference between pc and the start of
 * the first java method, and
 * range_hints[RANGE_HASH(pc - range_offset) give us the index of the
 * first java method that shares higher-order bits with pc.
 *
 * The number 7 was chosen based on `gen-ovm -opt=run' on an x86.
 * In a few places there where long sequences of methods 16 bytes
 * apart, and it was more common to see methods 32 or 48 bytes apart.
 * shifting 7 means that we should never have to search more than 8 or
 * 9 entries in the range table, and the average will be very close to
 * 1.  It uses quite a bit of memory: 3% of range_size.
 */
#define RANGE_HASH(X) ((X) >> 7)
static int *range_hints;

static void compute_range_hints() {
  range_offset = MR_VALUES [0].start;
  range_size = MR_VALUES [j2c_method_pointers.length-1].end
		- range_offset;

  int n_hints = RANGE_HASH(range_size) + 1;
  //range_hints = new int[n_hints];
  if ( (range_hints = (int *)malloc(n_hints*sizeof(int))) == NULL) {
    fprintf(stderr, "Cannot allocate range_hints\n");
    abort();
  }
#if 0
  fprintf(stderr, "allocating %d range hints\n", n_hints);
#endif

  int ridx = 0;
  int hidx;
  for (hidx = 0; hidx < n_hints; hidx++) {
    while (ridx < j2c_method_pointers.length 
	   && RANGE_HASH( MR_VALUES [ridx].end - range_offset) < hidx)
      ridx++;
    range_hints[hidx] = ridx;
  }
}

#ifdef PRECISE_BARRIER_PROF
static size_t bp_range_offset;
static size_t bp_range_size;

static int *bp_range_hints;

static unsigned long long bp_total=0;
static unsigned long long bp_inbarrier=0;

// FIXME: this currently is only meant for x86
void bp_timer_handler(jint sig, siginfo_t *info, void *ctx) {
    bp_total++;
    
    struct ucontext *uctx=(struct ucontext*)ctx;
    size_t pc = (size_t) uctx->uc_mcontext.gregs[REG_EIP];
    size_t delta = pc - bp_range_offset;
    if (delta >= bp_range_size) {
	return;
    }
    int idx = bp_range_hints[RANGE_HASH(delta)];
    while (barrier_prof_ranges.values[idx].end <= pc) {
    while ( (*(pcRange **)barrier_prof_ranges.values) [idx].end <= pc) {
	idx++;
    }
    if (barrier_prof_ranges.values[idx].start <= pc) {
    if ( (*(pcRange **)barrier_prof_ranges.values) [idx].start <= pc) {
	bp_inbarrier++;
    }
}

void bp_exit_handler() {
    fSBE(stderr,1);
    fprintf(stderr,
	    "Barrier Prof: total = %llu, in barrier = %llu\n",
	    bp_total,
	    bp_inbarrier);
}
#endif

static void init_barrier_prof() {
#ifdef PRECISE_BARRIER_PROF
#warning "Doing barrier profiling!"
  bp_range_offset = barrier_prof_ranges.values[0].start;
  bp_range_size = (barrier_prof_ranges.values[barrier_prof_ranges.length-1].end
		   - bp_range_offset);

  int n_hints = RANGE_HASH(bp_range_size) + 1;
  // bp_range_hints = new int[n_hints];
  if ((bp_range_hints = (int *)malloc(n_hints*sizeof(int)))==NULL) {
    fprintf(stderr,"Cannot allocate bp_range_hints\n");
    abort();
  }
  
#if 1
  fprintf(stderr, "allocating %d barrier prof range hints\n", n_hints);
#endif

  int ridx = 0;
  for (int hidx = 0; hidx < n_hints; hidx++) {
    while (ridx < barrier_prof_ranges.length 
	   && RANGE_HASH(barrier_prof_ranges.values[ridx].end
			 - bp_range_offset) < hidx)
      ridx++;
    bp_range_hints[hidx] = ridx;
  }
  
  // register SIGALRM signal handler.  we rely on the code in timer.c to
  // register a sufficiently high frequency timer.  It think that it currently
  // uses a 100Hz timer, which should be good enough.
// this is done in timer.c again - maybe it was for a reason here, too, but
// I disabled it because of Xenomai  
//  ABORT_ERRNO(registerSignalHandler(SIGALRM,bp_timer_handler),
//	      "registerSignalHandler");
//
  
  atexit(bp_exit_handler);
#endif
}
  
void* j2c_getCode(void *_pc) {
  size_t pc = (size_t) _pc;
  size_t delta = pc - range_offset;
  if (delta >= range_size) {
#if 0
      fprintf(stderr, "getCode(%p) => <null>, out of range (delta = %d, range size = %d)\n", _pc, delta, range_size);
#endif
    return 0;
  }
  int idx = range_hints[RANGE_HASH(delta)];
  while (MR_VALUES [idx].end <= pc)
    idx++;
  J2cCodeFragment *ret = (MR_VALUES [idx].start <= pc
			  ? ((e_s3_services_j2c_J2cCodeFragment **)j2c_method_pointers.values)[idx]
			  : 0);
#if 0
  fprintf(stderr, "getCode(%p) => %s@%p idx = %d, distance = %d\n",
	  _pc,
	  (ret
#ifndef ARRAYLETS	  
	   ? ((char *) (((e_java_lang_String *)ret->ovm_cname)->ovm_data->values
#else
	   ? ( (*(char **) (((e_java_lang_String *)ret->ovm_cname)->ovm_data->values)
#endif	   
			+ ((e_java_lang_String *)ret->ovm_cname)->ovm_offset))
	   : "<null>"),
	  ret,
	  idx,
	  idx - range_hints[RANGE_HASH(delta)] );
#endif
  return ret;
}

void *j2c_topFrame(jint nc) {
  return ((J2cContext *) nc)->topFrame;
}

void *j2c_bottomFrame(jint nc) {
  return ((J2cContext *) nc)->bottomFrame;
}

// analogous to BCdead
#define RTdead { assert(!"unsupported operation"); exit(-1); }

void *getOVMContextLoc(jint nc) {
  return (void *) &((J2cContext *) nc)->ovmContext;
}

void *getInvocationArgsLoc(jint nc) {
  return (void *) &((J2cContext *) nc)->args;
}

void *getEngineSpecificPtrLoc(jint nc) {
  return getInvocationArgsLoc(nc);
}

#ifdef PRECISE_PTRSTACK
void *getPtrStackBase(jint nc) {
    return ((J2cContext*)nc)->ptrStack;
}
void *getPtrStackTop(jint nc) {
    return ((J2cContext*)nc)->gcTop;
}
void *getPtrStackLimit(jint nc) {
    return ((J2cContext*)nc)->ptrStack+PTRSTACKSIZE;
}
#else
void *getPtrStackBase(jint nc) RTdead
void *getPtrStackTop(jint nc) RTdead
void *getPtrStackLimit(jint nc) RTdead
#endif

#ifdef PRECISE_HENDERSON
jint getNextGcRecord(int _gcFrame) {
  return (jint) ((GCFrame *) _gcFrame)->next;
}

jint getGcRecord(int nc, void *callee) {
  J2cContext *ctx = (J2cContext *) nc;
  GCFrame *fr = ctx->gcTop;

  if (ctx != currentContext && callee == ctx->topFrame)
    return (jint) fr;

  while (fr && (unsigned int) fr < (unsigned int) callee)
    fr = fr->next;
  return (jint) fr;
}
  
//e_Array<Oop *> *getLocalReferences(jint gcFrame) {
e_Array *getLocalReferences(jint gcFrame) {
//  return (e_Array<Oop *> *) &((GCFrame *) gcFrame)->arrayHeader;
  return (e_Array*) &((GCFrame *) gcFrame)->arrayHeader;
}
#else
jint getNextGcRecord(jint _) RTdead
jint getGcRecord(jint _, void * __) RTdead
//e_Array<Oop *> *getLocalReferences(jint frameHandle) RTdead
e_Array *getLocalReferences(jint frameHandle) RTdead

void *getStackBase(jint nc) {
  J2cContext *ctx = (J2cContext *) nc;
  return ctx->stackBase;
}

void *getStackTop(jint nc) {
  J2cContext *ctx = (J2cContext *) nc;
  if (ctx == currentContext)
    // skip invoke_native() => getStackTop()
    return callerLocalsEnd(getCurrentActivation(2));
  else
#ifdef THREAD_UCONTEXT
    return (void *) UC_REG(ctx->uc, UC_FP);
#else
    return (void *) JB_REG(ctx->buf, JB_FP);
#endif
}

void *getSavedRegisters(jint _ctx) {
  J2cContext *ctx = (J2cContext *) _ctx;
#ifdef THREAD_UCONTEXT
  return (void *) &UC_REG(ctx->uc, 0);
#else
  return ctx->buf;
#endif
}

jint getSavedRegisterSize() {
#ifdef THREAD_UCONTEXT
  return UC_SIZE;
#else
  return sizeof(jmp_buf);
#endif
}
#endif

#if defined(PRECISE_SAFE_POINTS)
void *getAuxStackTop(int nc) {
  accurate::AuxStack *aux = &((J2cContext *) nc)->localState.auxStack;
  return aux->empty() ? getAuxStackBottom(nc) : aux->top();
}
void *getAuxStackBottom(int nc) {
  return ((J2cContext *) nc)->localState.auxStack.stack + AUXSIZE;
}
void walkStack(int nc) {
  ((J2cContext *) nc)->shouldWalkStack = 1;
}
jboolean localStateIsRunning(int nc) {
  return ((J2cContext*)nc)->localState.running;
}
jint getAuxHeaderSize() {
  return sizeof(accurate::AuxFrame);
}
// This callback serves no purpose whatsoever
void userGC(accurate::AuxStack *_) {
}
#else
void *getAuxStackTop(int nc) RTdead
void *getAuxStackBottom(int nc) RTdead
void walkStack(int nc) RTdead
jint getAuxHeaderSize() RTdead
#endif

// Should not be here!
void C(jbyte *dummy, ...) { }

jint getStoredLineNumber() {
  return stored_line_number;
}
