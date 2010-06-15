#include <assert.h>
#include <stdarg.h>
#include "native_calls.h"

// find all native call declarations
#include "types.h"
#include <fcntl.h>
#include <math.h>
#include <unistd.h>
#ifndef RTEMS_BUILD
#include <sys/mman.h>
#include <strings.h>
#endif
#include "blockingio.h"
#include "clock.h"
#include "engine.h"
#include "eventmanager.h"
#include "sigiomanager.h"
#include "waitmanager.h"
#include "mem.h"
#include "native_interface.h"
#include "native_helpers.h"
#include "nativeScheduling.h"
#include "timer.h"
#include "systemproperties.h"
#include "signalmonitor.h"
#include "doselect.h"
#include "nanosleep_timer.h"
#include "smp_timer.h"
#include <sched.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <stdio.h>

#ifdef CEXCEPTIONS_COUNT
// WARNING: this must match what is in j2c.h
extern int n_exc_thrown;
#endif

/* Note the angle quotes.  They ensure that we start searching from
 * the start of the include path
 */
#include <app_native_calls.h>

static inline profileNativeBegin(int code);
static inline profileNativeEnd(int code);

#ifdef RTEMS_BUILD
static jint mmap(void* _,...) {
    abort();
}
static jint munmap(void*_,...) {
    abort();
}
#undef pwrite
#define pwrite pwrite_fail
static jint pwrite(jint _,...) {
    abort();
}
#undef pread
#define pread pread_fail
static jint pread(jint _,...) {
    abort();
}
#define WCOREDUMP(x) (0)
#undef socketpair
#define socketpair socketpair_fail
static jint socketpair(jint _,...) {
    return -1;
}
#endif

#define TRACE_INVOKE_NATIVE 0

jlong invoke_native(int code, ...)
{
  if (TRACE_INVOKE_NATIVE) {
    int __stackloc;
    printf("invoke_native on %d with stack at %p\n",code,&__stackloc);
  }
#define POP()     (va_arg(ap, jvalue))
#define POP_W()   (va_arg(ap, jwide))
#define PUSH_P(V) do { if (TRACE_INVOKE_NATIVE) printf("that worked\n"); return ((jvalue) V).jint; } while (0)
#define PUSH_R(V) do { if (TRACE_INVOKE_NATIVE) printf("that worked\n"); return ((jvalue) V).jint; } while (0)
#define PUSH_W(V) do { if (TRACE_INVOKE_NATIVE) printf("that worked\n"); return ((jwide) V).jlong; } while (0)
#define DONE_HOOK 0

  va_list ap;
  va_start(ap, code);
#include "native_calls.gen"
  
  if (TRACE_INVOKE_NATIVE) printf("that worked\n");
  return 0;
}

static int nativeHisto[NNATIVES];
static jlong nativeTimeSum[NNATIVES];
static jlong nativeTimeMax[NNATIVES];
static jlong before;

static inline profileNativeBegin(int code) {
    if (code<NNATIVES) {
	nativeHisto[code]++;
    }
    before=getCurrentTimeR();
}

static inline profileNativeEnd(int code) {
    jlong latency=getCurrentTimeR()-before;
    nativeTimeSum[code]+=latency;
    if (latency>nativeTimeMax[code]) {
	nativeTimeMax[code]=latency;
    }
}

void dumpNativeProfileHisto(void) {
#ifdef PROFILING_NATIVES
    int i;
    for (i=0;i<NNATIVES;++i) {
	printf("nativeHisto: %u %u %llu %llu\n",
	       i,nativeHisto[i],
	       nativeHisto[i]?nativeTimeSum[i]/nativeHisto[i]:(jlong)0,
	       nativeTimeMax[i]);
    }
#endif
}

void dumpCExceptionsStat(void) {

#ifdef CEXCEPTIONS_COUNT

	fprintf(stderr,"# Number of exceptions thrown: %d\n", n_exc_thrown);

#endif

}
