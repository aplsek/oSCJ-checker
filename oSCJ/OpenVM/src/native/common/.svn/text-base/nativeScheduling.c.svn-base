/*
 *
 * $Header: /p/sss/cvs/OpenVM/src/native/common/nativeScheduling.c,v 1.5 2006/10/15 06:25:41 cunei Exp $
 *
 */
#include "config.h"
#include "nativeScheduling.h"
#include "util.h"
#include <unistd.h>
#if defined(HAS_SCHED_H)
# include <sched.h>
#endif
#include <assert.h>
#include <errno.h>

/**
 * Defines all of the support routines for interacting with native threads
 * within the OVM. This includes initialisation of the native scheduling
 * system (assuming there is one) as well as implementations for use by
 * the interpreter and direct native calls for use by the higher-level
 * threading services in OVM.
 * We try to set up the priority scheduler if one exists.
 */

/** The minimum priority supported by the configured native scheduler. */
static jint MIN_PRIORITY = 0;

/** The maximum priority supported by the configured native scheduler. */
static jint MAX_PRIORITY = 0;

jint getMinPriority() { return MIN_PRIORITY; }
jint getMaxPriority() { return MAX_PRIORITY; }

/* useful helper macro for returning errno after a failed operation */

/* #define DEBUGCHECK */
#ifdef  DEBUGCHECK
  #define DEBUGPRINT(func) \
     printf("checking: "#func "\n")
#else
  #define DEBUGPRINT(func)
#endif 

/* this macro checks if a system/library calls succeeds, and if not it
   causes the return value of that call to be returned to the caller of
   the function using the macro.
*/
#define CHECK(func) do { \
    int status = 0; \
    DEBUGPRINT(func); \
    status = (func); \
    if (status != 0) { \
        return status; \
    } \
} while(0)

#ifdef HAVE_ERROR_H
  #include <error.h>
  #define ERROR_EXIT(msg) error(1, errno, msg)
#elif defined HAVE_ERR_H || defined OSX_BUILD
  #include <err.h>
  #define ERROR_EXIT(msg) err(1, msg)
#else 
  #define ERROR_EXIT(msg) do { fprintf(stderr,msg); fprintf(stderr,"\n"); exit(1); } while(0)
#endif


static void initPriorityRangeForPolicy(int policy) {
#if _POSIX_PRIORITY_SCHEDULING > 0
  MIN_PRIORITY = sched_get_priority_min(policy);
  if (MIN_PRIORITY == -1) {
      ERROR_EXIT("get min priority failed");
  }

  MAX_PRIORITY = sched_get_priority_max(policy);
  if (MAX_PRIORITY == -1) {
      ERROR_EXIT("get max priority failed");
  }
#else
# warning _POSIX_PRIORITY_SCHEDULING not supported
  printf("# WARNING: Priority scheduling is not supported on this platform\n");
#endif
}

/**
 * Initialises the available priority range and sets the current thread to
 * a mid-range priority using SCHED_FIFO.
 * If priority scheduling is not supported then a message is printed.
 * This method should be called once during the startup of the main 
 * interpreter program. It will terminate the process upon error.
 *
 */
void initializeNativeScheduling() {
#if _POSIX_PRIORITY_SCHEDULING > 0
    struct sched_param schedParams;
    int policy = SCHED_FIFO;
    int status;
    
    /* try SCHED_FIFO by default. If we don't have permission then we'll retry
       SCHED_OTHER
    */
    initPriorityRangeForPolicy(SCHED_FIFO);
    
    schedParams.sched_priority = (MIN_PRIORITY + MAX_PRIORITY) / 2;
    
    status = sched_setscheduler(0, SCHED_FIFO, &schedParams);
    if (status != -1) {
        /* sanity check by reading back */
        policy = sched_getscheduler(0);
        ASSERT(policy == SCHED_FIFO, "incorrect scheduling policy returned");
        printf("# Native scheduler configured for priority scheduling using SCHED_FIFO\n"
               "#   Minimum priority = %d\n"
               "#   Maximum priority = %d\n"
               "#   Main thread priority = %d\n",
               MIN_PRIORITY, MAX_PRIORITY, schedParams.sched_priority);
	return;
    }
    if (errno != EPERM) {
	EXIT_ERRNO(status, "set sched params failed");
    }

    initPriorityRangeForPolicy(SCHED_OTHER);
    schedParams.sched_priority = (MIN_PRIORITY + MAX_PRIORITY) / 2;
    status = sched_setscheduler(0, SCHED_OTHER, &schedParams);
    if (status != -1 ) {
	printf("# WARNING: Insufficient permissions to use priority scheduling - using SCHED_OTHER\n"
	       "#   Minimum priority = %d\n"
	       "#   Maximum priority = %d\n"
	       "#   Main thread priority = %d\n",
	       MIN_PRIORITY, MAX_PRIORITY, schedParams.sched_priority);
	return;
    }
    if (errno != EPERM) {
	EXIT_ERRNO(status, "set scheduler SCHED_OTHER failed");
    }

    // Under Solaris, cron tasks will run with different privileges. Try once more before giving up.
    if (schedParams.sched_priority+2<=MAX_PRIORITY) {
	schedParams.sched_priority+=2;
    } else {
	schedParams.sched_priority=MAX_PRIORITY;
    }
    status = sched_setscheduler(0, SCHED_OTHER, &schedParams);
    if (status != -1 ) {
	printf("# WARNING: Insufficient permissions to use priority scheduling - using SCHED_OTHER\n"
	       "#   Minimum priority = %d\n"
	       "#   Maximum priority = %d\n"
	       "#   Main thread priority = %d\n",
	       MIN_PRIORITY, MAX_PRIORITY, schedParams.sched_priority);
	return;
    }

    EXIT_ERRNO(status, "set scheduler SCHED_OTHER failed");

#else
#warning _POSIX_PRIORITY_SCHEDULING not supported
    printf("# WARNING: Priority scheduling is not supported on this platform\n");
#endif
}


/**
 * Sets the current process scheduling priority to the given priority value.
 * Returns zero on success and errno on error. If priority scheduling is
 * not supported then ENOSYS is returned
 */
jint setPriority(jint priority) {
#if _POSIX_PRIORITY_SCHEDULING > 0
  struct sched_param schedParams;
  int policy = -1;
  // get current scheduoing policy
  policy = sched_getscheduler(0);
  if (policy == -1)
      return errno;
  schedParams.sched_priority = priority;
  return (sched_setscheduler(0, policy, &schedParams) == -1 ? errno : 0);
#else
    return ENOSYS;
#endif
}
