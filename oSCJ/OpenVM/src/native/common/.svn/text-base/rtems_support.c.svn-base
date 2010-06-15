#include "config.h"

#ifdef RTEMS_BUILD
#include <bsp.h>

#include <stdlib.h>
#include <stdio.h>


rtems_task ovm_task_main(rtems_task_argument _);

void create_ratemon(rtems_id *ratemon) {
    rtems_status_code res;
    printk("creating timer (in rtems_support.c)\n");
    printk("max ratemons = %d\n",_Rate_monotonic_Information.maximum);
    if ((res=rtems_rate_monotonic_create(
	     rtems_build_name('r','a','t','e'),
	     ratemon))!=0) {
	printk("could not create rate monotonic timer, res = %d!\n",res);
	exit(1);
    }
    printk("we have a timer\n");
}

rtems_task Init( rtems_task_argument ignored ) {

    rtems_task_priority myprio;
    rtems_id ovm_task;


    printk("max ratemons = %d\n",_Rate_monotonic_Information.maximum);

    if (rtems_task_set_priority(
	    RTEMS_SELF,
	    RTEMS_CURRENT_PRIORITY,
	    &myprio)!=0) {
	printf("could not get priority!\n");
	exit(1);
    }
    
    printf("creating main task with priority: %d\n", myprio);
    
    printf("[DBG] test RTEMS support !\n");
    printf("[DBG] STACK size is: %d !\n",  RTEMS_MINIMUM_STACK_SIZE*100);
    
    printf("[DBG] Stack growing test !\n");
    double *t = (double *) malloc (sizeof(double)); 
    printf("[DBG] value 1 : %d !\n", t);
    double *t2 = (double *) malloc (sizeof(double)); 
    printf("[DBG] value 1 : %d !\n", t2);
    
    printf("[DBG] checking the heap size !\n");
    unsigned int size=1;
    unsigned int *p = &size;
    while (p) {
      p = malloc(size);
      if (p) {
	free(p);
	size *= 2;
      }
    }
    printf("[DBG] Stopped at size=%u\n",size);

    
    
    
    if (rtems_task_create(
	    rtems_build_name('m','a','i','n'),
	    myprio+1,
	    RTEMS_MINIMUM_STACK_SIZE*100,
	    RTEMS_PREEMPT | RTEMS_NO_TIMESLICE | RTEMS_ASR | RTEMS_INTERRUPT_LEVEL(0),
	    RTEMS_LOCAL | RTEMS_PRIORITY | RTEMS_FLOATING_POINT,
	    &ovm_task)!=0) {
	printf("could not create ovm task!\n");
	exit(1);
    }
    
    if (rtems_task_start(
	    ovm_task,
	    ovm_task_main,
	    0)!=0) {
	printf("could not start ovm task!\n");
	exit(1);
    }
    
    printf("[DBG] test RTEMS support !\n");
    printf("[DBG] STACK size is: %d !\n",  RTEMS_MINIMUM_STACK_SIZE*100);
    
    
    rtems_task_delete(RTEMS_SELF);
    exit(1);
  
}

// TODO: check and update these options
// ? how many files ?
// ? network ?
// ...
// keep in mind that they depend on the timer as well

#define CONFIGURE_INIT

#define CONFIGURE_APPLICATION_NEEDS_CONSOLE_DRIVER
#define CONFIGURE_APPLICATION_NEEDS_CLOCK_DRIVER

#define CONFIGURE_USE_IMFS_AS_BASE_FILESYSTEM
#define CONFIGURE_LIBIO_MAXIMUM_FILE_DESCRIPTORS 6

#define CONFIGURE_MAXIMUM_TASKS 5
#define CONFIGURE_MAXIMUM_PERIODS             7
#define CONFIGURE_MAXIMUM_TIMERS             7
#define CONFIGURE_MAXIMUM_SEMAPHORES         7

#define CONFIGURE_EXTRA_TASK_STACKS           (107 * RTEMS_MINIMUM_STACK_SIZE)

#define CONFIGURE_MICROSECONDS_PER_TICK 500

// leave 1 for timer
#define CONFIGURE_INIT_TASK_PRIORITY	2

#define CONFIGURE_RTEMS_INIT_TASKS_TABLE

// Ales changes
#define STACK_CHECKER_ON
#define CONFIGURE_MINIMUM_STACK_SIZE   128*1024 /* 128 * 1024 128k :) */

#include <rtems/confdefs.h>

#else

void ovm_dummy(void) {} /* some compilers compain about empty files.  make
			   those compilers happy. */

#endif
