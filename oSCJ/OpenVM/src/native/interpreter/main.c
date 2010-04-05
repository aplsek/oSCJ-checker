/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This is the main file that coordinates loading the boot image,
 * relocating the objects inside, and beginning execution.
 *
 * @author Christian Grothoff
 * @author Jan Vitek
 * @author James Liang
 * @author Ben L. Titzer
 **/

#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>
#include <math.h>
#include "config.h"
#include "boot.h"
#include "frame.h"
#include "interpreter_defs.h"
#include "yesno.h"
#include "debugging.h"
#include "globals.h"
#include "interpreter.h"
#include "nativeScheduling.h"
#include "mthread.h"
#include "exception.h"
#include "signalmanager.h"
#include "engine.h"
/**
 * Parse command line arguments
 **/
char * parse_args(int argc, char ** argv);

/**
 * Initialise signal handling state
 **/
void initialiseSignalHandling();

static NativeContext** context_for_debugging = NULL;

NativeContext* current_native_context = NULL;

/**
 * Defines the set of signals for which the crash handler should be
 * installed. This handler tries to report useful information about
 * the state of the VM when a fatal error occurs, or the vm is
 * terminated externally.
 */
static const int signalsUsedByExecEngine[] = {
    SIGABRT,
    SIGSEGV,
    SIGBUS,
    SIGQUIT,
    0
};

/**
 * Defines the set of signals reserved by the VM and which the signal
 * manager will leave alone. Just because a signal is used by the VM
 * it doesn't mean that it must be reserved. Signal handlers are chained
 * by the signal manager.
 */
const int signalsReservedByExecEngine[] = {
    SIGABRT,
    0
};


/**
 * Entrypoint of the interpreter. Loads the bootimage, resolves
 * objects, and begins execution.
 **/
int main(int argc, char* argv[]) {
  NativeContext* boot_current_context;
  PFX_DECLARE_CONTEXT_CACHE(boot_);
  void* mainObject;
  ByteCode* mainMethod;
  jref bootContext;
  char* filename;
#if USE_MTHREAD == YES
  struct mthread_block* mb;
#endif
  process_argv = argv;
  process_argc = argc;
  setvbuf(stdout,(char*)NULL, _IOLBF, 0);
  filename = parse_args(argc, argv);

  /* phase 1: load image  */
  /*  init_names(); - repository_names.h for debugging.  */
  if (SYSERR == load_image(filename,
			   &mainObject,
			   &mainMethod, 
			   &executiveCSA,
			   &utf8Array,
			   &global_catch_code,
			   &bootContext)) {
    errexit("FATAL: Failed in image loading\n");
  }

  print("# OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University\n");
  print("# http://www.ovmj.org/                  ovm@mail.ovmj.org\n");

  initialiseSignalHandling(); 
  initializeNativeScheduling();

  csavtbl = (struct ovm_core_execution_CoreServicesAccess_VTBL *)
    (HEADER_BLUEPRINT(executiveCSA)->vTable);

  if (csavtbl == NULL) {
    fprintf(stderr, "FATAL: csavtbl is null\n");
    abort();
  }

  /* Create the catch-all method: has only one instruction */
  boot_current_context = create_context(MAX_METHOD_DEPTH,
					global_catch_code,
					bootContext);
  ((struct ovm_core_execution_Context *)
   bootContext)->nativeContextHandle_ = (jint)boot_current_context;    
  context_for_debugging = &boot_current_context;
  current_native_context = boot_current_context;
  
  /* get information from the frame */
  PFX_LOAD_CONTEXTCACHE(boot_);
  
  /* create a frame for the main method. */

  create_frame(PFX_CONTEXT_CACHE_INOUT(boot_), 
	       mainMethod, HEADER_BLUEPRINT(mainObject), 0);

  PFX_SAVE_CONTEXT(boot_);
  
  /* Set up the "this" pointer for the main method. */
  set_local_ref(boot_current_context, boot_local_vars, 0,
		(jvalue)(mainObject));
  
#if USE_MTHREAD == YES
  mb = create_mthread(run_interpreter,
		      MTHREAD_DEFAULT_STACK_SIZE,
		      boot_current_context);
  boot_current_context->mthread = mb;
  mb->ctx = boot_current_context;
  mthread_context_switch(&main_sp, &mb->sp);
#else
  launchContext(bootContext);
#endif /* USE_MTHREAD */
  return 0; 
}

/* ************************* Helper methods ***************** */

/**
 * print_usage_and_exit()
 *
 * Print the command line options that the interpreter expects
 * and exit the interpreter gracefully.
 **/
static void print_usage_and_exit(char* exename) {
    errexit("Usage : %s PATH-TO-BOOTIMAGE argument*\n", exename);
}


/**
 * Parse the argument array and look for options to the interpreter.
 * @return the filename of the image
 **/
char * parse_args(int argc, char ** argv) {
  char *ret;
  
#ifndef BOOTBASE
  process_argv++;
  process_argc--;
  ret = argv[1];
#else
  ret = NULL;
#endif

  if (process_argv[1] && process_argv[1][0] == '-'
      && (process_argv[1][1] == 'v' || process_argv[1][1] == 'V')
      && process_argv[1][2] == '\0') {
    verbose_mode = 1 + (process_argv[1][1] == 'V');
    process_argv++;
    process_argc--;
  }
  
  return ret;
}

/**
 * This is the crash handler for fatal fault signals, or for
 * externally triggered termination. Currently the action is just
 * to print a message and exit; if debugging is turned on, a
 * stack trace is also printed.
 **/
static void sig_crash(int flag) {
  fprintf(stderr,"OVM interpreter crash (%s).\n", getSignalName(flag));
  if (current_native_context != NULL) {
    fprintf(stderr, "Stack trace:\n");
    stacktrace(current_native_context);
  }
  //printInterpreterState(mmbase);
  fflush(stdout);
  exit(1);
}

/**
 * Initialise signal handling in the OVM.
 * - Install the crash handler for the fatal signals.
 * - initialise the signal manager
 */
void initialiseSignalHandling() {
  struct sigaction sa;
  int i;
  
  sa.sa_flags = SA_RESTART;
  sa.sa_handler = sig_crash;
  sigemptyset(&sa.sa_mask);
  for (i = 0; signalsUsedByExecEngine[i]; i++) {
      EXIT_ERRNO(sigaction(signalsUsedByExecEngine[i], &sa, NULL), "sigaction");
  }
  
  EXIT_ERRNO(initSignalHandling(),"initSignalHandling");
}


/* end of main.c */


