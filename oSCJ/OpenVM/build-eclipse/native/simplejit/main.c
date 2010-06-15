#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>

#include "globals.h"
#include "image.h"
#include "signalmanager.h"
#include "nativeScheduling.h"
#include "util.h"

/**
 * Parse command line arguments
 **/
char * parse_args(int argc, char ** argv);


/* Saved stack pointer of the main call stack though we never come back */
int main_sp;

/* I don't know what this is, but for no compilation errors */
void launchContext(void * arg) {
}

static ImageFormat *image;

void *getImageBaseAddress() {
  return image->data + 4;	   /* first word is null */
}

void *getImageEndAddress() {
  int off = image->usedMemory - 4; /* last word is magic number */
  return image->data + off;
}

const int signalsReservedByExecEngine[] = {
    /* add comma-separated list of signals that JIT  uses for aborting or
     * regular execution here, but before the 0.  see interpreter/main.c
     * for an example. */
    SIGSEGV,
    SIGBUS,
    0
};

void segvHandler(int sig, siginfo_t * info, void *_ctx);

int main(int argc, char* argv[]) {
  void* bootMethod;
  NativeCode* mainMethod;
  JITHeader* jitHeader;
  struct s3_services_simplejit_NativeContext* nativeCtx;
  char* imagefilename;
  void* arg[2];
  process_argv = argv;
  process_argc = argc;

  /*
  stack_t stack;
  memset(&stack, 0, sizeof stack);
  stack.ss_sp = (char*)malloc(SIGSTKSZ);
  stack.ss_size = SIGSTKSZ;
  if (sigaltstack(&stack, 0)) {
      perror("sigaltstack");
  }
  */

  imagefilename = parse_args(argc, argv);

  image = loadImage(imagefilename);

  fprintf(stderr, 
	  "OVM (c) 2001-2003 S3 Lab, Purdue University\n"
	  "http://www.ovmj.org/      ovm@mail.ovmj.org\n");

  jitHeader = (JITHeader*)(image->simpleJITBootImageHeader);
  mainMethod = jitHeader->mainMethod;
  printf("mainMethod = %p\n", mainMethod);
  printf("code = %p\n", mainMethod->_parent_._parent_.foreignEntry);
  bootMethod = (void*)(mainMethod->_parent_._parent_.foreignEntry);
  printf("bootMethod = %p\n", bootMethod);

  arg[0] = mainMethod;
  arg[1] = image->mainObject;

  nativeCtx = (struct s3_services_simplejit_NativeContext*)
      malloc(sizeof(struct s3_services_simplejit_NativeContext));
  nativeCtx->reflection_invocation_buffer = 
      (int)malloc(REFLECTION_INVOCATION_BUFFER_WORD_SIZE * sizeof(int));
  nativeCtx->ovm_context = image->bootContext;
  nativeCtx->cut_to_frame_set = 0;
  nativeCtx->mtb = create_mthread(bootMethod,
				  MTHREAD_DEFAULT_STACK_SIZE,
				  2,
				  arg);

  ((struct ovm_core_execution_Context*)nativeCtx->ovm_context)->
      nativeContextHandle_ = (int)nativeCtx;

  currentContext = nativeCtx;
  ncontexts++;

  EXIT_ERRNO(initSignalHandling(),"initSignalHandling");
  initializeNativeScheduling();

  struct sigaction act;
  act.sa_sigaction = segvHandler;
  sigfillset(&act.sa_mask);
  //sigemptyset(&act.sa_mask);
  act.sa_flags = SA_SIGINFO;//|SA_ONSTACK;
  if (sigaction(SIGSEGV, &act, 0) < 0)
      perror("installing SEGV handler");
  if (sigaction(SIGBUS, &act, 0) < 0)
      perror("installing BUS handler");

  mthread_context_switch(&main_sp,
			 &nativeCtx->mtb->sp);

  /* never come here */

  return 0; 
}


/**
 * print_usage_and_exit()
 *
 * Print the command line options that the interpreter expects
 * and exit the interpreter gracefully.
 **/
static void print_usage_and_exit(char* exename) {
    fprintf(stderr,"Usage : %s PATH-TO-BOOTIMAGE arguments\n", exename);
    exit(-1);
}

/**
 * check_args()
 *
 * Check whether the arguments are valid and what the interpreter expects.
 * If the arguments are invalid, print the usage and exit.
 **/
static void check_args(int argc, char* argv[]) {
  if (argc <= 1)
    print_usage_and_exit(argv[0]);
}

/**
 * Parse the argument array and look for options to the engine.
 * @return the filename of the image
 **/
char * parse_args(int argc, char ** argv) {
#ifndef BOOTBASE
    check_args(argc, argv);
    process_argv++;
    process_argc--;
    return argv[1];
#else
    return NULL;
#endif
}



