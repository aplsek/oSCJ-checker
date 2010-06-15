/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This is the configuration file for most of the interpreter.
 * All the parameters that configure the behavior of the
 * interpreter should be put in here. These should all be overridable
 * at compile-time using -D
 *
 * @file include/ovm/config.h
 **/

#ifndef _CONFIG_H_
#define _CONFIG_H_

#include "autodefs.h"
#include "yesno.h"

/* FIXME: what does this mean? */
#ifndef GCCLIBS
  #define GCCLIBS NO           
#endif

/* FIXME: what does this mean ? */
#ifndef MAX_METHOD_DEPTH
  #define MAX_METHOD_DEPTH 200
#endif

/* Controls whether the return values of system call need not be checked.
   Set to NO for normal safe checking of system calls.
   Set to YES when you are 100% certain that system calls won't fail. Or, is
   so unlikely that you want to remove the check for performance reasons
*/
#ifndef UNCHECKED_SYSCALLS
  #define UNCHECKED_SYSCALLS NO
#endif

/* Controls whether reaching a polling point actually executes anything.
   The interpreter loop has polling points inserted after various control
   flow instructions. The polling invokes the event manager's checkEvents
   method and if the result is non-zero performs a CSA upcall to the
   evet hook.
   Set to YES to perform this polling and execution - THE NORMAL SETTING
   Set to NO to do nothing
*/
#ifndef EXECUTE_POLLING_HOOK
  #define EXECUTE_POLLING_HOOK YES
#endif

/* Controls whether the timer interrupt code keeps track of the number
   of polls that occur between actual interrupts.
   Set to YES to do this
   Set to NO to do nothing
*/
#ifndef PROFILE_TIMER_INTERRUPT
  #define PROFILE_TIMER_INTERRUPT NO
#endif

/* Control whether signalling handling for the timer/alarm is performed in 
   the main thread, or in a separate dedicated thread. 
   A seperate thread makes some aspects of the JIT easier to deal with.
*/
#ifndef USE_TIMER_SIGNAL_THREAD
   #define USE_TIMER_SIGNAL_THREAD NO
#endif

#ifndef USE_MTHREAD
   #define USE_MTHREAD NO
#endif

/*
 * Control whether the timer interrupt code keeps the initial series of
 * interrupt times and reports them when the timer is shutdown - see timer.c
 */
#ifndef REPORT_TIMER_INTERRUPT_RATE
   #define REPORT_TIMER_INTERRUPT_RATE NO
 //  #define REPORT_TIMER_INTERRUPT_RATE YES
#endif


#endif /* _CONFIG_H_ */
/* end of config.h */
