/*
Hardware interrupts monitor
*/

#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>

#include "intmonitor.h"
#include "fdutils.h"
#include "util.h"
#include "atomicops.h"
#include "eventmanager.h"

/* old PC hardware has IRQ0 - IRQ15  (and IRQ 2 is a cascade, so not used) */
#define MAX_INTERRUPT_INDEX	15

//#define SIMULATED_INTERRUPTS


#if defined(SIMULATED_INTERRUPTS)
  #error "The simulated interrupts are presently not supported - not even compilable.
#elif defined(OVM_XENOMAI)
  #define XENOMAI_INTERRUPTS
#else
  #define NO_INTERRUPTS
#endif

#if defined(XENOMAI_INTERRUPTS) || defined(SIMULATED_INTERRUPTS)

#ifdef XENOMAI_INTERRUPTS
#include <native/task.h>
#include <native/intr.h>
#include <native/cond.h>
#include <native/mutex.h>
#include <sys/mman.h>
#endif

static int pendingInterruptIndex;

static int monitoredInterruptsMask[MAX_INTERRUPT_INDEX + 1];

#ifdef SIMULATED_INTERRUPTS

/*
  0 - not pending
  otherwise -  pending
*/  
static int pendingInterruptsMask[MAX_INTERRUPT_INDEX + 1];


/*
  generate some interrupts - for testing
*/  

/* only for testing */
static int interruptDisabled[MAX_INTERRUPT_INDEX + 1];
static int stopSimulator = 0;

void* interrupt_simulator(void* dummy) {
  int i;
  int r;
  
  srand(211);
  
  for(;;) {
    if (stopSimulator) return 0;
    usleep( 1000000 + (5*rand())%5000 );
    if (stopSimulator) return 0;    
    
    /* lets generate a random interrupt */
    for(i=0;i<MAX_INTERRUPT_INDEX;i++) {
      if (!interruptDisabled[i] && monitoredInterruptsMask[i]) {
        r = rand()%100;
        if ( r > 75 ) {
          /* generate this one */
           
         interruptDisabled[i] = 1;   
         pendingInterruptsMask[i] = 1;
         atomic_get_and_inc_int(&totalPendingInterrupts);
         SIGNAL_EVENT();
//         ubprintf("Simulator created interrupt 0x%x...\n",i);
         break ;
        }
      }
    }
  }      
}

static  pthread_t simulator_thread;

void start_interrupt_simulator(void) {

  int rc;

  rc=pthread_create(&simulator_thread, NULL, interrupt_simulator, NULL);
  if (rc) {
    fprintf(stderr, "start_interrupt_simulator, pthread_create: %d (%s)\n", rc, strerror(-rc));
    exit(-1);
  }
//  ubprintf("Interrupt simulator started...\n");  
}

void stop_interrupt_simulator(void) {

  int rc;

//  ubprintf("Stopping interrupt simulator thread...\n");  
  stopSimulator = 1;
  
  rc = pthread_join(simulator_thread, NULL);
  if (rc) {
    fprintf(stderr, "stop_interrupt_simulator, pthread_join: %d (%s)\n", rc, strerror(-rc));
  }  
  
}

#else 

// real interrupts (Xenomai)

static RT_INTR intr_desc[MAX_INTERRUPT_INDEX + 1];
static RT_TASK handler_desc[MAX_INTERRUPT_INDEX + 1];

// synchronization primitives for interrupt disabling/enabling
// - need a binary semaphore 
// 	- "enable" (nonblocking)
//	- "disable" (nonblocking)
//	- "wait for enabled, atomically disable" (blocking)
// - starts enabled

typedef struct {
  RT_COND cond;  
  RT_MUTEX mutex; /* protects access to enabled */
  int enabled;
  int dirty; /* disabled after last call to wait_and_disable */
} INTR_SYNC;

int intr_sync_create(INTR_SYNC * intr_sync) {

  int res;
  
  res = rt_cond_create( &intr_sync->cond, NULL);
  if (res < 0) {
    fprintf(stderr, "intr_sync_create, rt_cond_create: %d (%s)\n", res, strerror(-res));    
    return res;
  }
  
  res = rt_mutex_create( &intr_sync->mutex, NULL );
  if (res < 0) {
    fprintf(stderr, "intr_sync_create, rt_mutex_create: %d (%s)\n", res, strerror(-res));    
    return res;
  }  
  
  intr_sync->enabled = 1;
  intr_sync->dirty = 0;
  
  return res;

}  

int intr_sync_delete(INTR_SYNC * intr_sync) {

  int res1, res2 ;
  
  res1 = rt_mutex_delete( &intr_sync-> mutex );
  if (res1<0) {
    fprintf(stderr, "intr_sync_create, rt_mutex_delete: %d (%s)\n", res1, strerror(-res1));  
  }
  
  res2 = rt_cond_delete( &intr_sync -> cond );
  if (res2<0) {
    fprintf(stderr, "intr_sync_create, rt_cond_delete: %d (%s)\n", res2, strerror(-res2));
  }
  
  if ( (res1==0) && (res2==0) ) {
    return 0;
  } else {
    return -1;
  }
}  

int intr_sync_set_enabled(INTR_SYNC * intr_sync, int enabled) {

  int res;
  int ret = 0;
  
  if (enabled == 0) {
    intr_sync->dirty = 1;
  }
  
  if (intr_sync->enabled == enabled) {
    return 0;
  }      
  
  do {
    res = rt_mutex_acquire( &intr_sync->mutex, TM_INFINITE );
  } while (res == -EINTR);
  
  if (res < 0) {
    fprintf(stderr, "intr_sync_set_enable, rt_mutex_acquire: %d (%s)\n", res, strerror(-res));    
    return res;
  }
  
  if (intr_sync->enabled == enabled) {
    ret = 0;
    goto unlock_and_return;
  }
 
  intr_sync->enabled = enabled;
  
  res = rt_cond_signal( &intr_sync->cond );
  if (res < 0) {
    ret = res;
    fprintf(stderr, "intr_sync_set_enable, rt_cond_signal: %d (%s)\n", res, strerror(-res));      
    goto unlock_and_return;
  }
  
unlock_and_return:

  res = rt_mutex_release( &intr_sync-> mutex );
  if (res < 0) {
    fprintf(stderr, "intr_sync_set_enable, rt_mutex_release: %d (%s)\n", res, strerror(-res));    
    return res;
  }
  
  return ret;  
}  

int intr_sync_enable(INTR_SYNC * intr_sync) {

  return intr_sync_set_enabled( intr_sync, 1 );
}

int intr_sync_disable(INTR_SYNC * intr_sync) {

  return intr_sync_set_enabled( intr_sync, 0 );
}
  
int intr_sync_wait_and_disable(INTR_SYNC * intr_sync) {

  int res;
  int ret = 0;
  
  do {
    res = rt_mutex_acquire( &intr_sync->mutex, TM_INFINITE );
  } while (res == -EINTR);
  
  if (res < 0) {
    fprintf(stderr, "intr_sync_wait_and_disable, rt_mutex_acquire: %d (%s)\n", res, strerror(-res));
    return res;
  }
  
  while( ! intr_sync -> enabled ) {

    res = rt_cond_wait( &intr_sync -> cond, &intr_sync -> mutex, TM_INFINITE );
    if (res < 0) {
      ret = res;
      fprintf(stderr, "intr_sync_wait_and_disable, rt_cond_wait: %d (%s)\n", res, strerror(-res));      
      break;
    } 
  }
    
  if (!ret) {
    /* no error */
    intr_sync -> enabled = 0; /* disabling */
    intr_sync -> dirty = 0;
  }

  res = rt_mutex_release( &intr_sync -> mutex );
  if (res < 0) {
      fprintf(stderr, "intr_sync_wait_and_disable, rt_mutex_release: %d (%s)\n", res, strerror(-res));  
    return res;
  }
    
  return ret;  
}    

/*
-----------------------------------------------------
*/

static INTR_SYNC local_interrupts;
static INTR_SYNC hardware_interrupt[MAX_INTERRUPT_INDEX + 1];

void handler_task (void *cookie) {

  int interruptIndex = (int) cookie;
  int pending;
  int res;
  
  for(;;) {
  
    res = rt_intr_wait(&intr_desc[interruptIndex], TM_INFINITE);
    if (res < 0) {
      if (res == -EINTR) continue ;
      if (res == -EIDRM) return;
      fprintf(stderr, "handler_task, rt_intr_wait: %d (%s)\n", res, strerror(-res));
    }

    pending = res;
    
//    ubprintf("X-Intr: %d (%d times)\n", interruptIndex, pending);
    while(pending) {

/*
This complex synchronization is here to avoid receiving interrupts after
disabled at user level. It's functioning depends on the Java code and
pollchecks, too. Note, that we have "interruption" points at user calls to
disable interrupts.
*/
      res = intr_sync_wait_and_disable( &hardware_interrupt[interruptIndex] );

      if (res < 0) {	
        fprintf(stderr, "handler_task, intr_sync_wait_and_disable: %d (%s)\n", res, strerror(-res));     
      }
      
      /* now we own the interrupt_line for our IRQ */    

      res = intr_sync_wait_and_disable( &local_interrupts );
    
      if (res < 0) {	
        fprintf(stderr, "handler_task, intr_sync_wait_and_disable: %d (%s)\n", res, strerror(-res));     
      }     

      /* now we also own the local CPU interrupts, so we can enable the IRQ line again */
      
      res = intr_sync_enable( &hardware_interrupt[interruptIndex] );

      if (res < 0) {	
        fprintf(stderr, "handler_task, intr_sync_enable: %d (%s)\n", res, strerror(-res));     
      }      


      /* let's check that the user did not call disableInterrupt in between  */
      
      if (hardware_interrupt[interruptIndex].dirty) {
        /* too bad, user called disableInterrupt, we have to wait again */
        
        res = intr_sync_enable( &local_interrupts );
        if (res < 0) {	
          fprintf(stderr, "handler_task, intr_sync_enable: %d (%s)\n", res, strerror(-res));     
        }      
        
        continue;
      }
      
      
      /* and dispatch the interrupt */
      ASSERT( pendingInterruptIndex == -1, "Some interrupt not acknowledged");  

      pendingInterruptIndex = interruptIndex;
      SIGNAL_EVENT();    

      pending --;
      
    }
  }

}

#endif


/*
----------------------------------------------------------------------------------------
Interface for the VM
----------------------------------------------------------------------------------------
*/

void initInterruptMonitor(void) {

  int res;

//  ubprintf("Interrupt monitor initializing...\n");
#ifdef SIMULATED_INTERRUPTS
  start_interrupt_simulator();
#endif

  res = intr_sync_create(&local_interrupts);
  if (res<0) {
    fprintf(stderr, "initInterruptMonitor, intr_sync_create: %d (%s)\n", res, strerror(-res));
  }
  
  pendingInterruptIndex = -1;
    
}  

void shutdownInterruptMonitor(void) {
  
  int res,i;

//  ubprintf("Interrupt monitor shutting down...\n");
#ifdef SIMULATED_INTERRUPTS
  stop_interrupt_simulator();
#endif    

  for( i = 0; i < MAX_INTERRUPT_INDEX ; i++ ) {

    if (!monitoredInterruptsMask[i]) {
      continue;
    }
    
    stopMonitoringInterrupt(i);
  }    
  
  res = intr_sync_delete(&local_interrupts);
  if (res<0) {
    fprintf(stderr, "shutdownInterruptMonitor, intr_sync_delete: %d (%s)\n", res, strerror(-res));
  }
  
}


/*
Returns the list of pending hardware interrupts and length of the list.

input: 
  arrayLength
  pendingInterrupts - allocated array of length arrayLength

output:
  return value - number of pending interrupts
  pendingInterrupts - filled in with pending interrupts indexes, only
    not zeroed, no trailing number
*/    

// TODO: get rid of this

jint getPendingInterrupts( jint pendingInterrupts[], jint arrayLength ) {

  int store_i;
  
  store_i = 0;
  
  if (pendingInterruptIndex==-1) {
    return 0;
  }
  
  pendingInterrupts[store_i++] = pendingInterruptIndex;
  
  return store_i;
}

jint getPendingInterrupt(void) {

  return pendingInterruptIndex;
}  

/*
something like acknowledge in Xenomai...
though, we have a problem here - this is only logically an acknowledge, but 
it does not disable interrupts

Xenomai disables interrupts for some type of interrupts (level, ...) and not
for the others (edge,...).

Thus, we do not really provide a transparent semantics. If level interrupts
were piled at Xenomai level, they would be delivered, even with the
interrupt masked at PIC level. Ideally, we should here "disableInterrupt"
for the types of interrupts that Xenomai would disable at acknowledge.

*/
void interruptServingStarted( jint interruptIndex ) {
  
  ASSERT( interruptIndex < MAX_INTERRUPT_INDEX, "interruptServingStarted: interrupt index out of range" );
//  ASSERT( pendingInterruptIndex == interruptIndex, "interruptServingStarted: serving wrong interrupt");

#ifndef NDEBUG
  if (pendingInterruptIndex != interruptIndex) {
    fprintf(stderr, "interruptServingStarted, serving wrong interrupt - pending %d, serving %d\n",
      pendingInterruptIndex, interruptIndex);
  }
#endif  
    
  pendingInterruptIndex = -1;
  
}

void enableLocalInterrupts(void) {

  int res;

  ASSERT( pendingInterruptIndex == -1, "enableLocalInterrupts: called while an interrupt is pending");

  res = intr_sync_enable( &local_interrupts );
  if (res < 0) {
    fprintf (stderr, "enableLocalInterrupts, intr_sync_enable %d (%s)\n", res, strerror(-res) );
  }

  return;
}  
  
void disableLocalInterrupts(void) {

  int res;

  res = intr_sync_disable( &local_interrupts );
  if (res < 0) {
    fprintf (stderr, "disableLocalInterrupts, intr_sync_disable %d (%s)\n", res, strerror(-res) );
  }
  
  return;  
}

void enableInterrupt( int interruptIndex ) {

  int res;

  ASSERT( interruptIndex < MAX_INTERRUPT_INDEX, "enableInterrupt: interrupt index out of range" );
  
  res = intr_sync_enable( &hardware_interrupt[ interruptIndex ] );
  if (res < 0) {
    fprintf (stderr, "enableInterrupt, intr_sync_enable: %d (%s)\n", res, strerror(-res) );
  }

  res = rt_intr_enable( &intr_desc[ interruptIndex ] );
  if (res < 0) {
    fprintf (stderr, "enableInterrupt, rt_intr_enable: %d (%s)\n", res, strerror(-res) );
  }


  return;
  
}

void disableInterrupt( int interruptIndex ) {

  int res;

  ASSERT( interruptIndex < MAX_INTERRUPT_INDEX, "disableInterrupt: interrupt index out of range" );

  res = intr_sync_disable( &hardware_interrupt[ interruptIndex ] );
  if (res < 0) {
    fprintf (stderr, "disableInterrupt, intr_sync_disable %d (%s)\n", res, strerror(-res) );
  }

  res = rt_intr_disable( &intr_desc[ interruptIndex ] );
  if (res < 0) {
    fprintf (stderr, "disableInterrupt, rt_intr_disable: %d (%s)\n", res, strerror(-res) );
  }

  return;    
}

/*
  Information that the interrupt has been served, and thus local interrupts should be re-enabled
*/  
void interruptServed( jint interruptIndex ) {

  int res;

  ASSERT( interruptIndex < MAX_INTERRUPT_INDEX, "interruptServed: interrupt index out of range" );

  // enableLocalInterrupts(); - this is presently called already from InterruptMonitorImpl
  
  /* this is needed because we use I_NOAUTOENA, and thus the IPIPE does not 
     call xnarch_end_irq 
     
     in fact, this might not work for some types of interrupts (but level and edge should be fine),
     because for some the xnarch_end_irq does something else
     
     this way, we should not receive more interrupt at one in rt_intr_wait
  */
  
  res = rt_intr_enable( &intr_desc[interruptIndex] );
  if (res < 0) {
    fprintf( stderr, "interruptServer, rt_intr_enable: %d (%s)\n", res, strerror(-res) );
  }
  
}  

/*
  Instructs to monitor the given interrupt.
  returns:
    J_TRUE - success: not monitored before
    J_FALSE - already monitored
*/     
/*
 this function must not return before the handler is blocked
 within call rt_intr_wait ; This should be assured by the high priority
 of the handler task
 
 with the subtle exception of other handler tasks competing with this one, 
 in case of exceptional interrupt activity. Lets' assume that the task will
 be able to reach the call within it's first alloted quantum.
*/ 
jboolean startMonitoringInterrupt( jint interruptIndex ) {  

  int rc;
  
  ASSERT( interruptIndex < MAX_INTERRUPT_INDEX, "startMonitoringInterrupt: interrupt index out of range" );
  
  if (monitoredInterruptsMask[interruptIndex]!=0) {
    return J_FALSE;
  } 
  
  monitoredInterruptsMask[interruptIndex]=1;  
  
  rc = rt_intr_create(&intr_desc[interruptIndex], NULL, interruptIndex, I_NOAUTOENA);
//  rc = rt_intr_create(&intr_desc[interruptIndex], NULL, interruptIndex, 0);
  if (rc) {
    fprintf(stderr, "startMonitoringInterrupt, rt_intr_create: %d (%s)\n", rc, strerror(-rc) );
  }
    
  rc = rt_task_create(&handler_desc[interruptIndex], NULL, 0, 99, 0);
  if (rc) {
    fprintf(stderr, "startMonitoringInterrupt, rt_task_create: %d (%s)\n", rc, strerror(-rc) );
  }
    
  rc = rt_task_start(&handler_desc[interruptIndex], &handler_task, (void *)interruptIndex);
  if (rc) {
    fprintf(stderr, "startMonitoringInterrupt, rt_task_start: %d (%s)\n", rc, strerror(-rc) );
  }
  
  rc = intr_sync_create( &hardware_interrupt[interruptIndex] );
  if (rc) {
    fprintf(stderr, "startMonitoringInterrupt, rt_sync_create: %d (%s)\n", rc, strerror(-rc) );
  }

  rc = rt_intr_enable(&intr_desc[interruptIndex]);
  if (rc) {
    fprintf(stderr, "startMonitoringInterrupt, rt_intr_enable: %d (%s)\n", rc, strerror(-rc) );
  }
  
  return J_TRUE;
}

jint getMaxInterruptIndex( void ) {
  return MAX_INTERRUPT_INDEX;
}  

/*
  Instructs to stop monitoring given interrupt.
  returns:
    J_TRUE - success: was monitored before
    J_FALSE - wasn't monitored before
    
  Note that the VM does not call any other function in parallel.
*/        
jboolean stopMonitoringInterrupt( jint interruptIndex ) {

  int rc;
  
  ASSERT( interruptIndex < MAX_INTERRUPT_INDEX, "interruptServed: interrupt index out of range" );
  
  if (monitoredInterruptsMask[interruptIndex]==0) {
    return J_FALSE;
  } 
  
  monitoredInterruptsMask[interruptIndex]=0;

  /* now cleanup if any interrupt was pending */
  
  rc = rt_intr_disable( &intr_desc[interruptIndex] );
  if (rc) {
    fprintf(stderr, "stopMonitoringInterrupt, rt_intr_disable: %d (%s)\n", rc, strerror(-rc) );
  }
  
  rc = rt_intr_delete( &intr_desc[interruptIndex] );
  if (rc) {
    fprintf(stderr, "stopMonitoringInterrupt, rt_intr_delete: %d (%s)\n", rc, strerror(-rc) );
  }  
/*
  the task exits automatically after the interrupt object is deleted
  
  rc = rt_task_delete( &handler_desc[interruptIndex] );
  if (rc) {
    fprintf(stderr, "stopMonitoringInterrupt, rt_task_delete: %d (%s)\n", rc, strerror(-rc) );
  }
*/  
  if (pendingInterruptIndex == interruptIndex ) {
    pendingInterruptIndex = -1;
  }
        
  return J_TRUE;
}

jboolean isMonitoredInterrupt( jint interruptIndex ) {

  if (monitoredInterruptsMask[interruptIndex]) {
    return J_TRUE;
  } else {
    return J_FALSE;
  }

}
#endif // xenomai or simulated interrupts

#ifdef NO_INTERRUPTS

static void notImplemented() {
  ubprintf("Interrupt handling called but not compiled in.\n");
  abort();
}  __attribute__((noreturn));

void initInterruptMonitor(void) {};
void shutdownInterruptMonitor(void) {};

jint getPendingInterrupts( jint pendingInterrupts[], jint arrayLength ) { return 0; }
jint getPendingInterrupt(void) { return -1; }

void interruptServingStarted( jint interruptIndex ) { notImplemented(); }
void interruptServed( jint interruptIndex ) { notImplemented(); }

void enableLocalInterrupts(void) { notImplemented(); }
void disableLocalInterrupts(void) { notImplemented(); }
void enableInterrupt( jint interruptIndex ) { notImplemented(); }
void disableInterrupt( jint interruptIndex ) { notImplemented(); }

jboolean startMonitoringInterrupt( jint interruptIndex ) { notImplemented(); }
jboolean stopMonitoringInterrupt( jint interruptIndex ) { notImplemented(); }
jboolean isMonitoredInterrupt( jint interruptIndex ) { return J_FALSE; }

jint getMaxInterruptIndex( void ) { return MAX_INTERRUPT_INDEX; }

#endif

void resignalInterrupt() {
  SIGNAL_EVENT();
}
