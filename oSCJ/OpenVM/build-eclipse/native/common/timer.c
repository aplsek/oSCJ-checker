/** 
 * This is the native code that supports the timer manager implementation
 *
 */

#include "config.h"

#ifdef RTEMS_BUILD
#include <bsp.h>
#endif

#include "util.h"
#include "timer.h"
#include "eventmanager.h"
#include "signalmanager.h"
#include "fdutils.h"
#include "native_helpers.h"
#ifndef RTEMS_BUILD
#include <signal.h>
#endif
#include <time.h>
#include <sys/time.h>
#include <stdlib.h>
#include <errno.h>

#define CORE_DUMP_PROTECT
jlong baseTime = 0;

/**
NOTES: 

On normal Linux and OSX systems the timer granularity is limited to a 10ms
interrupt rate. This means that RTSJ timers, periodic thread release and
simple sleeps can only update every 10ms - therefore periods, for example, must
be >> 10ms. (Apparently with 2.6 kernels this will improve to 1ms.)

On Timesys Linux RT, with the licensed extensions we can achieve timer
interrupt rates of 1us by using the POSIX RT timers and linking with the
resource kernel (rk) library.

POSIX RT Timers do not work very well (irregular interrupt rates at less
than the requested rate) or normal Linux systems with a 2.4 kernel. Supposedly
they are fixed in 2.6 but that has not been tested.
Additionally they require linking with libpthread, which can cause problems 
with simplejit and/or j2c on some systems depending on the version of 
LinuxThreads being used. Use with NPTL should be okay but the various working 
permutations have not been enumerated so we keep it simple and don't use the
POSIX timers unless on the licenced Timesys Linux RT with the rk library.

When POSIX RT timers are not used we use setitimer to install a timer "alarm"
handler. Note that neither setitimer nor the POSIX RT timers generate an error
if you ask for an interrupt rate faster than that supported by the system. So
we check if the requested rate is less than the supported rate and print a 
warning if so. It may be better to abort the OVM startup with an error in this
case so that the user is clearly aware that the system can't support the
interrupt rate they requested.

To link with the Timesys rk library you need to ensure that librk.a is either 
in the default link-time path used by ld (/usr/lib only!) or else is specified
on the LIBRARY_PATH environment variable. This must occur at both configure 
time (so that configure will produce makefiles that will use the POSIX RT 
timers) and at build time so that the library can actually be statically linked.
David Holmes - Jul 26, 2004
*/

/* For local experimentation */
/*
#define USE_POSIX_RT_TIMER
*/

#if defined(USE_POSIX_RT_TIMER) && !(_POSIX_TIMERS > 0)
#error "Can't use POSIX timers when they aren't supported on this platform"
#endif


/**
 * The actual interrupt count - read from Java code
 */
volatile sig_atomic_t interruptCount;

static jlong multiplier, count, numInterruptions, numOverruns;
static jlong lastTime, interSum, interN;

static void profileInterArrivalTime() {
    jlong thisTime=getCurrentTime();
    if (lastTime) {
	interSum+=thisTime-lastTime;
	interN++;
    }
    lastTime=thisTime;
}


/**
 * Returns address of interruptCount for Java code to access directly
 */
volatile void* getInterruptCountAddress() {
    return &interruptCount;
}

// for debugging - testing code needs to make sure an interrupt is pending
// to test pollchecks
void generateTimerInterrupt() {
	SIGNAL_EVENT();
	interruptCount++;
}

/**
 * The currently configured timer period. This should be a constant once
 * set.
 */
jlong currentPeriodNanos;

/** local prototypes */
static void setupTimer(jlong nanos);

/* rounds the base time to whole period, aimed at improving accuracy of triggering periodic tasks */
static void setupBaseTime(jlong roundTo) {

  if (roundTo < 1000000000L ) {
  	roundTo = 1000000000L;  // 1 second
  }

  baseTime = 0;
  jlong now = getCurrentTime();
  baseTime = now - (now % roundTo);
  printf("Timer base time set to %lld\n", baseTime );
}

/**
 * Implementation of TimerManagerImpl.Helper.getPeriod
 */
jlong getPeriod() {
    return currentPeriodNanos;
}


#define NANOS_PER_SEC (1000L*1000*1000)
#define MICROS_PER_SEC (1000L*1000)
#define NANOS_PER_MICRO 1000L

#if REPORT_TIMER_INTERRUPT_RATE==YES
  #define NSTAMPS 200
  static int idx = 0;

  #if defined(USE_POSIX_RT_TIMER) || (_POSIX_TIMERS > 0)
    /* we have clock_gettime available */
    static struct timespec timestamps[NSTAMPS];

    #define LOG_TIMESTAMP() do { \
      if (idx < NSTAMPS) \
        clock_gettime(CLOCK_REALTIME, &timestamps[idx++]); \
      } while(0)

    static void dumpTimestamps() {
        jlong lastTime, now;
        int i;
        for(i = 1; i < NSTAMPS; i++) {
            lastTime = timestamps[i-1].tv_sec * NANOS_PER_SEC + timestamps[i-1].tv_nsec;
            now = timestamps[i].tv_sec * NANOS_PER_SEC + timestamps[i].tv_nsec;
            
            printf("Time between interrupts = %lld, absolute arrival (now) is %lld, arrival relative to VM base (start) time is %lld\n", (now - lastTime), now, now-baseTime );
        }
    }

  #else /* not POSIX TIMER */
    static struct timeval timestamps[NSTAMPS];

    #define LOG_TIMESTAMP() do { \
       if (idx < NSTAMPS) \
         gettimeofday(&timestamps[idx++], NULL); \
       } while(0)

    static void dumpTimestamps() {
        jlong lastTime, now;
        int i;
        for(i = 1; i < NSTAMPS; i++) {
            lastTime = timestamps[i-1].tv_sec * NANOS_PER_SEC + timestamps[i-1].tv_usec * NANOS_PER_MICRO;
            now = timestamps[i].tv_sec * NANOS_PER_SEC + timestamps[i].tv_usec * NANOS_PER_MICRO;
            
            printf("Time between interrupts = %lld\n", (now - lastTime) );
        }
    }
  #endif
#else /* no REPORT_TIMER_INTERRUPT_RATE */
  #define LOG_TIMESTAMP()
#endif



#if defined(RTEMS_BUILD)

static jlong getTimerRes() {
    rtems_interval ticks_per_second;
    rtems_clock_get(
	RTEMS_CLOCK_GET_TICKS_PER_SECOND,
	&ticks_per_second );
    return NANOS_PER_SEC/ticks_per_second;
}

static rtems_interval period_ticks;

void create_ratemon(rtems_id *ratemon);

static rtems_task timer_task_main(rtems_task_argument _) {
    rtems_id ratemon;
    create_ratemon(&ratemon);
    for (;;) {
	rtems_rate_monotonic_period(
	    ratemon,
	    period_ticks);
	
	SIGNAL_EVENT_FROM_THREAD();
	
	profileInterArrivalTime();
	
	numInterruptions++;
	count++;
	while (count>=multiplier) {
	    interruptCount++;
	    count-=multiplier;
	}
	
	LOG_TIMESTAMP();
    }
}

static rtems_id timer_task;

/**
 * Helper function to actually do the timer setup
 */
static void setupTimer(jlong nanos) {
    rtems_task_priority myprio;
    rtems_interval ticks_per_second;
    
    if (rtems_task_set_priority(
	    RTEMS_SELF,
	    RTEMS_CURRENT_PRIORITY,
	    &myprio)!=0) {
	printf("could not get priority!\n");
	exit(1);
    }
    
    printf("my priority = %d\n",myprio);
    printf("will create timer with priority = %d\n",myprio-1);
    
    if (rtems_clock_get(
	    RTEMS_CLOCK_GET_TICKS_PER_SECOND,
	    &ticks_per_second)!=0) {
	printf("could not get ticks per second!\n");
	exit(1);
    }
    
    printf("ticks_per_second = %d\n",ticks_per_second);
    
    period_ticks=nanos*ticks_per_second/NANOS_PER_SEC;
    
    printf("period_ticks = %d\n",period_ticks);
    
    if (rtems_task_create(
	    rtems_build_name('t','i','m','e'),
	    myprio-1,
	    RTEMS_MINIMUM_STACK_SIZE,
	    RTEMS_NO_PREEMPT | RTEMS_NO_TIMESLICE | RTEMS_NO_ASR | RTEMS_INTERRUPT_LEVEL(0),
	    RTEMS_LOCAL | RTEMS_PRIORITY | RTEMS_FLOATING_POINT,
	    &timer_task)!=0) {
	printf("could not create timer task!\n");
	exit(1);
    }
    
    if (rtems_task_start(
	    timer_task,
	    timer_task_main,
	    0)!=0) {
	printf("could not start timer task!\n");
	exit(1);
    }
}

static void tearDownTimer() {
    /* meh */
}

#elif defined(USE_POSIX_RT_TIMER)
/* Our real-time timer */
static timer_t timer;

/**
 * Signal handling code for use with POSIX RT timer
 */
static void timer_interrupt_handler(jint sig,
				    siginfo_t *info,
				    void *ctx) {
    unsigned overrun;

    SIGNAL_EVENT();
#ifdef DEBUG
    if (sig != SIGALRM){
        printf("Wrong sig sent to handler: expected %d got %d\n", 
               SIGALRM, sig);
        abort();
    }
    if (info->si_signo != SIGALRM){
        printf("Wrong signo sent to handler: expected %d got %d\n", 
               SIGALRM, info->si_signo);
        abort();
    }
    if (info->si_code != SI_TIMER){
        printf("Wrong code sent to handler\n");
        abort();
    }
#endif

    /* account for the possibility that timer interrupts are firing faster
       than the signal system can deliver them.
    */
    
    profileInterArrivalTime();

    overrun=timer_getoverrun(timer);
    if (overrun>0) {
	numOverruns++;
    }
    numInterruptions++;
    count+=1 + overrun;
    while (count>=multiplier) {
	interruptCount++;
	count-=multiplier;
    }

    LOG_TIMESTAMP();
}

#ifdef CORE_DUMP_PROTECT
void coreDumpProtectHandler(int signum) {
  fprintf(stderr, "OVM SIGSEGV HANDLER: Process %d got signal %d\n", getpid(), signum);
  RESET_EVENTS();
  signal(SIGALRM, SIG_IGN);
  signal(signum, SIG_DFL);
  kill(getpid(), signum);
}
#endif

/**
 * Helper function to actually do the timer setup
 */
static void setupTimer(jlong nanos) {
    struct itimerspec ts;
    struct sigevent timer_ev;
    
    if (1) printf("WORKING WITH POSIX RT TIMERS\n");
    timer_ev.sigev_notify = SIGEV_SIGNAL;
    timer_ev.sigev_signo = SIGALRM;
    EXIT_ERRNO(timer_create(CLOCK_REALTIME, &timer_ev, &timer), "timer_create failed");
    
    long psecs  = nanos / NANOS_PER_SEC;
    long pnanos = nanos % NANOS_PER_SEC;

    ts.it_interval.tv_sec = psecs;
    ts.it_interval.tv_nsec = pnanos;
    
    ts.it_value.tv_sec= baseTime / NANOS_PER_SEC;
    ts.it_value.tv_nsec = baseTime % NANOS_PER_SEC;

//  this worked .. but the timer can start quite late 
//
//    ts.it_value.tv_nsec = NANOS_PER_SEC - (getCurrentTime()%NANOS_PER_SEC);
    
    /* note that we are starting the timer in the past ; my experiments suggest it does not matter,
       and it simplifies our code ; I didn't find out that it would be illegal in the doc */
       
    if (1) printf("Timer set for: %ld secs, %lld nsecs\n", psecs, pnanos);
    EXIT_ERRNO(timer_settime(timer, TIMER_ABSTIME, &ts, NULL), "timer_settime failed");
    
#ifdef CORE_DUMP_PROTECT
    signal(SIGSEGV, coreDumpProtectHandler);
    signal(SIGBUS, coreDumpProtectHandler);    
#endif    
}

static void tearDownTimer() {
    EXIT_ERRNO(timer_delete(timer), "timer_delete failed");
}

static jlong getTimerRes() {
    struct timespec res;
    clock_getres(CLOCK_REALTIME, &res);
    printf("POSIX RT getTimerRes - clock timer resolution is %ld ns\n",
    	((jlong)res.tv_sec * NANOS_PER_SEC) + res.tv_nsec);
    return ((jlong)res.tv_sec * NANOS_PER_SEC) + res.tv_nsec;
}


#else /* not USE_POSIX_RT_TIMER */


/**
 * Signal handling code for use with setitimer
 */
static void timer_interrupt_handler(jint sig, siginfo_t *info, void *ctx) {
    SIGNAL_EVENT();
#ifdef DEBUG
    if (sig != SIGALRM){
        printf("Wrong sig sent to handler: expected %d got %d\n", 
               SIGALRM, sig);
        abort();
    }
#endif

    count++;
    while (count>=multiplier) {
	interruptCount++;
	count-=multiplier;
    }
    numInterruptions++;
    
    profileInterArrivalTime();

/*     if (eventsEnabled) putchar('@'); */
/*     else putchar('%'); */

    LOG_TIMESTAMP();
}

static void setupTimer(jlong nanos) {
    struct itimerval iv;
    long secs, usecs;
    if (0) printf("WORKING WITH SETITIMER\n");
    secs  = nanos / NANOS_PER_SEC;
    nanos = nanos % NANOS_PER_SEC;
    usecs = nanos / NANOS_PER_MICRO;
    if (usecs == 0 && secs == 0) {
        /* presumably nanos < 1 usec */
        usecs = 1;
    }
    iv.it_interval.tv_sec = secs;
    iv.it_interval.tv_usec = usecs;
    iv.it_value.tv_sec = secs;
    iv.it_value.tv_usec = usecs;
    if (0) printf("timer set for: %ld secs, %ld usecs\n", secs, usecs);
    EXIT_ERRNO(setitimer(ITIMER_REAL,&iv,NULL), "setitimer failed");
}

static void tearDownTimer() {
    struct itimerval iv;
    iv.it_interval.tv_sec = 0;
    iv.it_interval.tv_usec = 0;
    iv.it_value.tv_sec = 0;
    iv.it_value.tv_usec = 0;
    EXIT_ERRNO(setitimer(ITIMER_REAL,&iv,NULL), "setitimer failed");
}

static jlong getTimerRes() {
    /* normal UNIX/Linux timer interrupt rate - no way to verify ??? :( */
    printf("Regular UNIX - hardwired 10ms - getTimerRes()\n");
    return 10LL * 1000 * 1000;
}


#endif /* USE_POSIX_RT_TIMER */


static jboolean handlerInstalled = 0;


/**
 * The code for configuring the timer and installing our signal handler
 *
 */
void configureTimer(jlong nanos,jlong multiplier_) {
    jlong timerRes = getTimerRes();
    
    multiplier=multiplier_;

    /* check args as its easy to pass in an overflowed 32-bit value instead of
       a 64-bit value
    */
    if (nanos < 0) {
        fSBE(stderr,1);
        fprintf(stderr,"negative timer period received - make sure it is being passed as a 64-bit value\n");
        exit(-1);
    }

    if (timerRes > nanos)
        fprintf(stderr, "\nWARNING: requested timer resolution not available -"
                "requested %lld ns, available %lld ns\n", nanos, timerRes);

    /* adjust the current period to handle underlying timer resolution*/
    currentPeriodNanos = timerRes * 
        (nanos/timerRes + (nanos%timerRes > 0 ? 1 : 0));

#ifndef RTEMS_BUILD
    if (!handlerInstalled) {
/*         printf("REGISTERING TIMER SIGNAL HANDLER\n"); */
        /* set up the signal handler */
        ABORT_ERRNO(registerSignalHandler(SIGALRM,timer_interrupt_handler),
                    "registerSignalHandler");
        handlerInstalled = 1;
    }
#endif
    setupBaseTime(currentPeriodNanos);
    setupTimer(currentPeriodNanos);
}

/* stop the timer and remove the handler */
void stopTimer() {
    tearDownTimer();
#ifndef RTEMS_BUILD
    removeSignalHandler(timer_interrupt_handler);
#endif
#if REPORT_TIMER_INTERRUPT_RATE==YES
    dumpTimestamps();
#endif
}

void printTimerStats() {
    ubprintf("timer: I had %u interruptions, of which %u had overruns; the mean inter-arrival time was %u.\n",
	     (unsigned)numInterruptions,(unsigned)numOverruns,(unsigned)(interSum/interN));
}

