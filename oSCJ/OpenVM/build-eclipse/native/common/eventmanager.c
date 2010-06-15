/*
 * eventmanager.c -- C-land code for the event manager.  this here code
 *                   helps facilitate our fast poll check that happens once
 *                   every couple instructions.  it also helps the VM
 *                   block for events whenever it needs to do so.
 * by Filip and David.
 */
#include "config.h"
#ifdef RTEMS_BUILD
#include <bsp.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include <mem.h>
#include <signal.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>
#include "util.h"
#include "fdutils.h"
#include "signalmanager.h"
#include "native_helpers.h"
#include <sys/time.h>

#if HAVE_PTHREAD
#include <pthread.h>
#endif

#include <stdbool.h>

#define IN_EVENTMANAGER_C

#include "eventmanager.h"

eventUnion_t eventUnion = {
    ._.notSignaled = 1,
    ._.notEnabled = 1
};

jshort eventCount;

/** the pointers */
volatile VOID_FPTR signal_event = NULL;
volatile VOID_FPTR signal_event_from_thread = NULL;

volatile VOID_FPTR engine_signal_event = NULL;
volatile VOID_FPTR engine_signal_event_from_thread = NULL;
volatile VOID_FPTR engine_events_enabled = NULL;
volatile VOID_FPTR engine_events_disabled = NULL;

static jlong lastPeriodCount;
static jlong period=1000000000; /* set very large default period so that if
				   the event manager is initialized before
				   the timer manager, things won't just blow
				   up. */

static jint prof_timer_trace_buf_size=0,prof_timer_trace_n;
static jlong *prof_timer_trace_buf;

static jshort maxCount;

void initPollcheckTimer(jshort _maxCount,
			jlong _period) {
    maxCount=_maxCount;
    period=_period;
}

static void clearTimerTraceBuf(void) {
    if (prof_timer_trace_n) {
	FILE *flout=fopen("timer_pc_trace.txt","a");
	if (flout!=NULL) {
	    jint i;
	    for (i=0;i<prof_timer_trace_n;++i) {
		fprintf(flout,"%lld\n",
			prof_timer_trace_buf[i]);
	    }
	}
	prof_timer_trace_n=0;
    }
}

void initPollcheckTimerProf(jint trace_buf_size) {
    prof_timer_trace_buf_size=trace_buf_size;
    if ( (prof_timer_trace_buf=(jlong*)malloc(trace_buf_size*sizeof(jlong))) == NULL) {
      fprintf(stderr,"Cannot allocate prof_timer_trace_buf\n");
      abort();
    }
    prof_timer_trace_n=0;
    atexit(clearTimerTraceBuf);
}

jboolean eventPollcheckTimer() {
    eventCount=maxCount;
    jlong curTime=getCurrentTime();
    if (prof_timer_trace_buf_size) {
	prof_timer_trace_buf[prof_timer_trace_n++]=curTime;
	if (prof_timer_trace_n==prof_timer_trace_buf_size) {
	    clearTimerTraceBuf();
	}
    }
    jlong curPeriodCount=curTime/period;
    if (curPeriodCount-lastPeriodCount) {
	lastPeriodCount=curPeriodCount;
	eventUnion._.notSignaled=0;
    }
    return eventPollcheck();
}

/** a standard and simple signal event implementation */
static void simple_signal_event(void) {
    SIGNAL_EVENT_FOR_POLL_CHECK();
}

/** a signal event implementation that indicates to the user that something
    is broken */
static void broken_signal_event(void) {
    printf("broken_signal_event(void) called\n");
    abort();
}

void makeSignalEventSimple() {
    signal_event=simple_signal_event;
}

void makeSignalEventBroken() {
    signal_event=broken_signal_event;
}

void makeSignalEventFromThreadSimple() {
    signal_event_from_thread=simple_signal_event;
}

void makeSignalEventFromThreadBroken() {
    signal_event_from_thread=broken_signal_event;
}

#ifdef RTEMS_BUILD

static rtems_id wakeup_sem;
static rtems_id status_sem;
static bool sleeping;
static bool waitingInited;

static void thread_signal_event(void) {
    rtems_semaphore_obtain(
	status_sem,
	RTEMS_DEFAULT_OPTIONS,
	RTEMS_NO_TIMEOUT);
    SIGNAL_EVENT_FOR_POLL_CHECK();
    if (sleeping) {
	rtems_semaphore_release(wakeup_sem);
	sleeping=false;
    }
    rtems_semaphore_release(status_sem);
}

static void sigusr1_handler(jint sig,siginfo_t info,void *ctx) {
    /* do nothing */
}

void makeSignalEventFromThreadProper() {
    rtems_task_priority myprio;
    
    printk("making signal_event_from_thread proper\n");
    
    if (rtems_task_set_priority(
	    RTEMS_SELF,
	    RTEMS_CURRENT_PRIORITY,
	    &myprio)!=0) {
	printf("could not get priority!\n");
	exit(1);
    }

    if (rtems_semaphore_create(
	    rtems_build_name('w','a','k','e'),
	    0,
	    RTEMS_DEFAULT_ATTRIBUTES,
	    RTEMS_NO_PRIORITY,
	    &wakeup_sem)!=0) {
	printf("could not create semaphore!\n");
	exit(1);
    }
    
    printf("myprio is %d, ceiling priority for stat semaphore is %d\n",
      myprio, myprio-1);
    if (rtems_semaphore_create(
	    rtems_build_name('s','t','a','t'),
	    1,
	    RTEMS_DEFAULT_ATTRIBUTES,
	    myprio-1,
	    &status_sem)!=0) {
	printf("could not create semaphore!\n");
	exit(1);
    }
    
    signal_event_from_thread=thread_signal_event;
    
    waitingInited=true;
}

#elif HAVE_PTHREAD

static pthread_t mainThread;

static void thread_signal_event(void) {
    SIGNAL_EVENT_FOR_POLL_CHECK();
    pthread_kill(mainThread,SIGUSR1);
}

static void sigusr1_handler(jint sig,siginfo_t info,void *ctx) {
    /* do nothing */
}

void makeSignalEventFromThreadProper() {
    if (registerSignalHandler(SIGUSR1,sigusr1_handler)<0) {
	fprintf(stderr,"Cannot register SIGUSR1 handler because %s\n",strerror(errno));
	abort();
    }
    mainThread=pthread_self();
    signal_event_from_thread=thread_signal_event;
}

#else

void makeSignalEventFromThreadProper() {
    signal_event_from_thread=broken_signal_event;
}

#endif

/**
 * Blocks the current thread until an event has occurred.
 */
void waitForEvents() {
#ifdef RTEMS_BUILD
    if (!waitingInited) {
	printf("FATAL: waiting is not inited\n");
	abort();
    }
    for (;;) {
	bool done=false;
	rtems_semaphore_obtain(
	    status_sem,
	    RTEMS_DEFAULT_OPTIONS,
	    RTEMS_NO_TIMEOUT);
	if (CHECK_EVENT_SIGNALED()) {
	    done=true;
	} else {
	    sleeping=true;
	}
	rtems_semaphore_release(status_sem);
	if (done) {
	    break;
	}
	rtems_semaphore_obtain(
	    wakeup_sem,
	    RTEMS_DEFAULT_OPTIONS,
	    RTEMS_NO_TIMEOUT);
    }
    RESET_EVENTS();
#else
    sigset_t fullSet,oldSet,emptySet;
    sigfillset(&fullSet);
    sigemptyset(&emptySet);

    /* First we mask all signals */
#if HAVE_PTHREAD
    EXIT_ERRNO(pthread_sigmask(SIG_BLOCK,&fullSet,&oldSet),
               "pthread_sigmask failed");
#else
    EXIT_ERRNO(sigprocmask(SIG_BLOCK,&fullSet,&oldSet),
               "sigprocmask failed");
#endif

    /* next we check if there is an event pending */
    while (!CHECK_EVENT_SIGNALED()) {
        /* enable all signals while we suspend */
        sigsuspend(&emptySet);
    }
    /* clear the flag so that it is ready for next time */
    RESET_EVENTS();

    /* restore the original signal mask */
#if HAVE_PTHREAD
    EXIT_ERRNO(pthread_sigmask(SIG_SETMASK,&oldSet,NULL),
               "pthread_sigmask failed");
#else
    EXIT_ERRNO(sigprocmask(SIG_SETMASK,&oldSet,NULL),
               "sigprocmask failed");
#endif
#endif
}

static unsigned prof_histo_size=0;
static unsigned prof_trace_buf_size=0;

static unsigned *prof_histo=NULL;
static unsigned prof_overflow=0;

static unsigned long long *prof_trace_stamp_buf=NULL;
static unsigned *prof_trace_ltncy_buf=NULL;
static unsigned prof_trace_n=0;

static unsigned long long prof_last;
static jboolean prof_last_valid=0;

static jboolean prof_skip_pa=1;

static unsigned long long getCurTime() {
    struct timeval t;
    gettimeofday(&t,NULL);
    return t.tv_sec*1000000+t.tv_usec;
}

static void recordHistogram(void) {
    FILE *fl;
    fl=fopen("event_pc_histogram.txt","r");
    if (fl!=NULL) {
	unsigned i;
	for (i=0;i<prof_histo_size;++i) {
	    unsigned tmp=0;
	    fscanf(fl,"%u\n",&tmp); /* if this read fails, tmp will still
				       be 0, so it's all good */
	    prof_histo[i]+=tmp;
	}
	fclose(fl);
    }
    fl=fopen("event_pc_overflow.txt","r");
    if (fl!=NULL) {
	unsigned tmp=0;
	fscanf(fl,"%u",&tmp); /* if this read fails, see above */
	prof_overflow+=tmp;
    }
    fl=fopen("event_pc_histogram.txt","w");
    if (fl!=NULL) {
	unsigned i;
	for (i=0;i<prof_histo_size;++i) {
	    fprintf(fl,"%u\n",prof_histo[i]);
	}
	fclose(fl);
    }
    fl=fopen("event_pc_overflow.txt","w");
    if (fl!=NULL) {
	fprintf(fl,"%u\n",prof_overflow);
	fclose(fl);
    }
}

static void clearTraceBuf(void) {
    if (prof_trace_n) {
	FILE *flout=fopen("event_pc_trace.txt","a");
	if (flout!=NULL) {
	    unsigned i;
	    for (i=0;i<prof_trace_n;++i) {
		fprintf(flout,"%llu %u\n",
			prof_trace_stamp_buf[i],
			prof_trace_ltncy_buf[i]);
	    }
	    fclose(flout);
	}
	prof_trace_n=0;
    }
}

void startProfilingEvents(jint histo_size,
			  jint trace_buf_size,
			  jboolean skip_pa) {
    prof_skip_pa=skip_pa;
    prof_histo_size=histo_size;
    prof_trace_buf_size=trace_buf_size;
    if (prof_histo_size) {
	if ( (prof_histo=(unsigned*)malloc(sizeof(unsigned)*prof_histo_size))==NULL) {
	  fprintf(stderr, "Cannot allocated prof_histo\n");
	  abort();
	}
	bzero(prof_histo,sizeof(unsigned)*prof_histo_size);
	atexit(recordHistogram);
    }
    if (prof_trace_buf_size) {
	if ( (prof_trace_stamp_buf=(unsigned long long*)malloc(sizeof(unsigned long long)*prof_trace_buf_size)) ==NULL) {
	  fprintf(stderr, "Cannot allocated prof_trace_stamp_buf\n");
	  abort();
	}
	if ( (prof_trace_ltncy_buf=(unsigned*) malloc(sizeof(unsigned)*prof_trace_buf_size)) == NULL) {
	  fprintf(stderr, "Cannot allocate prof_trace_ltncy_buf\n");
	  abort();
	}
	atexit(clearTraceBuf);
    }
}

void profileEventsBegin(void) {
    if ((prof_histo!=NULL || prof_trace_stamp_buf!=NULL)
	&& !prof_last_valid) {
	prof_last=getCurTime();
	prof_last_valid=1;
    }
}

void profileEventsReset(void) {
    prof_last_valid=0;
}

void profileEventsResetOnSetEnabled(jboolean e) {
    if (prof_skip_pa) {
	prof_last_valid=0;
    }
}

void profileEventsEnd(void) {
    if ((prof_histo!=NULL || prof_trace_stamp_buf!=NULL)
	&& prof_last_valid) {
	prof_last_valid=0;
	unsigned long long cur_time=getCurTime();
	unsigned latency=cur_time-prof_last;
	if (prof_histo!=NULL) {
	    if (latency<prof_histo_size) {
		prof_histo[latency]++;
	    } else {
		int e=fSBE(stdout,1);
		printf("PC PROFILE OVERFLOW!\n");
		fSBE(stdout,e);
		prof_overflow++;
	    }
	}
	if (prof_trace_stamp_buf!=NULL) {
	    prof_trace_stamp_buf[prof_trace_n]=cur_time;
	    prof_trace_ltncy_buf[prof_trace_n]=latency;
	    prof_trace_n++;
	    if (prof_trace_n>=prof_trace_buf_size) {
		clearTraceBuf();
	    }
	}
    }
}




