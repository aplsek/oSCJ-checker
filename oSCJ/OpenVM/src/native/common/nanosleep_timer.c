
#include "config.h"

#ifdef RTEMS_BUILD
#include <bsp.h>
#include <stdio.h>
#include "eventmanager.h"
#include "jtypes.h"

#else


#include "util.h"
#include "timer.h"
#include "eventmanager.h"
#include "signalmanager.h"
#include "fdutils.h"
#include "native_helpers.h"
#include <signal.h>
#include <time.h>
#include <sys/time.h>
#include <stdlib.h>
#include <errno.h>

#endif

#if HAVE_PTHREAD
#include <pthread.h>
#endif

#if HAVE_PTHREAD || defined(RTEMS_BUILD)

static jlong period;

static void *timerThread(void *arg) {
    struct timespec req;
    for (;;) {
	req.tv_sec=period/1000000000;
	req.tv_nsec=period%1000000000;
	nanosleep(&req,NULL);
	SIGNAL_EVENT_FROM_THREAD();
    }
}


#if defined(RTEMS_BUILD)
rtems_id timerTaskId;
rtems_name timerTaskName;

rtems_task timerTask( rtems_task_argument unused ) {
  timerThread(NULL);
}
#endif
  
jint initNanosleepTimer(jlong period_) {
    
    period=period_;
#if HAVE_PTHREAD
    pthread_t t;    
    pthread_create(&t,NULL,timerThread,NULL);
#else
  /* RTEMS */

  rtems_task_priority old_pri;
  rtems_mode old_mode;
  rtems_status_code status;
  
  status = rtems_task_set_priority( RTEMS_SELF, 2, &old_pri);
  if (status != RTEMS_SUCCESSFUL) {
    fprintf(stderr, "rtems_task_set_priority failed with code %d\n", status);
    abort();
  }

  status = rtems_task_mode( RTEMS_PREEMPT, RTEMS_PREEMPT_MASK, &old_mode);
  if (status != RTEMS_SUCCESSFUL) {
    fprintf(stderr, "rtems_task_set_mode failed with code %d\n", status);
    abort();
  }

  
  timerTaskName = rtems_build_name( 'T', 'I', 'M', 'E');
  
  status = rtems_task_create( timerTaskName, 1, RTEMS_MINIMUM_STACK_SIZE * 2, 
    RTEMS_DEFAULT_MODES, RTEMS_DEFAULT_ATTRIBUTES, &timerTaskId );
  if (status != RTEMS_SUCCESSFUL) {
    fprintf(stderr, "rtems_task_create failed with code %d\n", status);
    abort();
  }

  status = rtems_task_start( timerTaskId, timerTask, 0 );
  if (status != RTEMS_SUCCESSFUL) {
    fprintf(stderr, "rtems_task_start failed with code %d\n", status);
    abort();
  }
#endif
    return 1;
}


#else
/* nanosleep timer not supported */

jint initNanosleepTimer(jlong period) { return 0; }

#endif
