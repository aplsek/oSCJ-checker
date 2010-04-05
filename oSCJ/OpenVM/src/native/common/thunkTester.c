/*
  use this file on cthulhu/dragon/reptilicus as:

/p/sss/project/x86_64/gcc4/bin/g++ -MMD -m32 -ffloat-store -fno-schedule-insns2 \
  -D_XOPEN_SOURCE=600 -D_BSD_SOURCE=1 -DHAVE_CONFIG_H -DTHUNKTESTER_TESTING \
  -DPRECISE_SAFE_POINTS -DPRECISE_THUNK \
  <source>/src/native/common/thunkTester.cc -I<source>/src/native/common/include \
  -I/<source>/src/native/j2c -I<build>/src/native/common/include -o thunkTester \
  <source>/src/native/j2c/precise.cc -fexceptions -fnon-call-exceptions \
  -fno-strict-aliasing -Wno-pmf-conversions -g

On the arm add: -fno-omit-frame-pointer
*/

#include <stdio.h>
#include <setjmp.h>
#include <string.h>
#include <signal.h>
#include <stdlib.h>

#if defined(PRECISE_COUNTER) || defined(PRECISE_THUNK)
# include "thunkTester.h"
# include "precise.h"
#else
# define CHCKRET()
# define CATCHPREPARE()
# define CATCHRESTORE(...)
# define CALL0(E)     E
# define RCALL0(T, E) E
# define CALL(E, N, ...) E
# define RCALL(E,N,...) E
# define APP_THROW(X) throw(X)
# define ACCURATE_R
# error Testing what? Must define either -DPRECISE_THUNK or -DPRECISE_COUNTER
#endif

#ifndef THUNKTESTER_TESTING
#error "You must define THUNKTESTER_TESTING"
#endif

#include "md.h"
#include "fdutils.h"
// simple, but essential observation:
// user code MUST NOT use "catch(...)", or the whole code dies :)
// SaveException and RestoreException are caught as well!


// if you switch threads, remember to use
// localStateN.makeCurrent(); before resuming execution.
// OTOH prepGC() can be called directly.


// you can traverse the stack either directly,
// using the DO_GC macro, or indirectly, if
// you have a jmp_buf of a suspended thread.
// In this second case, you can call
// prepGC(buf,localState) to traverse the
// stack of that particular thread.

//////////////////////////////////////////////////////////
//
//  This is the user-defined gc routine
//
//
// when you want this code to be called,
// invoke DO_GC()
//

void do_gc(accurate::AuxStack *as) {
 printf("GC running...\n");
 accurate::AuxFrame *a=as->top();
 while (a!=NULL) {
  printf("Scanning frame\n");
  int i;
  for (i=0;i<a->size;i++) {
   printf("Ptr %d is %08lX\n",i,a->data[i]);
   a->data[i]=(void*)((int)a->data[i]&0xF0F0F0F0);
  }
  a=as->next(a);
 }
}


//////////////////////////////////////////////////////////
//
// This section contains the user program
// The CALLn() macros should add no overhead,
//  with respect to a plain call, if no GC is requested
//
// Use CALLn(fun,p1,p2,..pn) to call fun(p1,...,pn)
//
// Use x=RCALLn(retType,fun,p1,...,pn) if fun returns a value of type retType
//
// Returning structures or unions is not currently supported, although it
// probably could be implemented with additional effort.
//
///////////
//
// Update: use the following format instead:
//
// CALL(PACKCALL(fun,j,a1,..,aj),n,p1,..,pn)
// x=RCALL(PACKCALL(fun,j,a1,..,aj),n,p1,..,pn)
//

accurate::LocalState myState;

#if 0
jmp_buf jb;


void testRemote(int arg) {
 printf("serving USR2, current frame is %08lX, testing prepGC()...\n",__builtin_frame_address(0));
 prepGC(jb,&myState);
 printf("after testing prepGC(), ready to return to your scheduled programme. Longjmp!...\n");
 if (sigaltstack(&accurate::theGlobalState.altStack,0)<0){
  perror("sigaltstack");
  exit(1);
 }
 longjmp(jb,1);
}


// in order to test the uberTrick from a different
// "thread" with a different stack, I call it from
// a signal handler, therefore from the secondary stack.
// (sigaltstack is called elsewhere)

void setUberTrickTester() {
 struct sigaction sa;

 // Set up the custom signal handler
 sa.sa_handler = &testRemote;
 sa.sa_flags = SA_ONSTACK;
 sigemptyset( &sa.sa_mask );
 sigaction( SIGUSR2, &sa, 0 );
}
#endif





static void antiLeaf() __attribute__((noinline));
static void antiLeaf() {}

int testExceptions=0;

void ho2() {
 antiLeaf(); // must make it non-leaf
 if (testExceptions) APP_THROW(4);
}

void ho1() {
#undef ACCURATE_R
#define ACCURATE_R void
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE void

 CALL(PACKCALL(ho2,0),0);
}

void ho() {
#undef ACCURATE_R
#define ACCURATE_R void
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE void

 CALL(PACKCALL(ho1,0),0);
}

void h() {
#undef ACCURATE_R
#define ACCURATE_R void
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE void

  CATCHPREPARE();
  void *p1=(void*)0xDEADBEEF;
  void *p2=(void*)0xB4C0FFEE;
  int a,b;

  try {
    CALL(PACKCALL(ho,0), 2, p1, p2);
  } catch (int x) {
    CATCHRESTORE(2, p1, p2);
    if (x == 4)
      puts("throw before GC works");
    else
      printf("SHIT: expected to catch 4, but got %d\n", x);
  }
#if defined(PRECISE_COUNTER) || defined(PRECISE_THUNK)
//
// Calls to bound members must be converted, only
// plain calls can be used with the current templates.
//
// That is relatively easy to do, the implicit receiver
// can be made explicit (using GCC).
//
// For instance, if I have an object myState of class
// LocalState and I want to use its member function gc(),
// I would normally write:
//
//  myState.gc();
//
// That can be made explicit as follows:
//
//  ((void(*)(LocalState*))&LocalState::gc)(&myState);
//
// at which point the expression can be treated as a
// normal (constant) function with one argument.
//
// In the case in which the call is made through
// a pointer-to-member, the form becomes:
//
//  void (LocalState::*p)() = &LocalState::gc;
//  ((void(*)(LocalState*))(myState.*p))(&myState);
//
// and similarly for virtual calls.
// Note that this conversion requires the flag -Wno-pmf-conversions
//

  CALL(PACKCALL(((void(*)(accurate::LocalState*))&accurate::LocalState::gc),1,&myState),2,p1,p2);
//
// The asm for the call above, excluding the exception handler,
// is quite simply:
//
//        movl    $myState, (%esp)
//        call    _ZN8accurate10LocalState2gcEv
//
//

#endif
#if 0
  asm volatile ("; BEFORE RCALL");
  int ret=RCALL(PACKCALL(setjmp,1,jb),2,p1,p2);
  asm volatile ("; AFTER RCALL");

  if (!ret) {
   puts("about to test the jmp_buf uberTrick");
   raise(SIGUSR2);
  } else {
   puts("re-entered");
  }
#endif
  printf("\n...doing things...\n");
  printf("h().p1=%08lX\n",p1);
  printf("h().p2=%08lX\n",p2);
  puts("");
  CALL(PACKCALL(ho,0),0);
}

#if USING_UNRELIABLE_GOTO_HACK
inline long long h3()  __attribute__((always_inline));
#endif

short h4() {
#undef ACCURATE_R
#define ACCURATE_R val
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE short

 CALL(PACKCALL(h,0),0);
 return 0x1EE7;
}

long long h3() {
#undef ACCURATE_R
#define ACCURATE_R val
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE long long

 printf("SHORT: %04hx\n",RCALL(PACKCALL(h4,0),0));
 return 0x1122334455667788ll;
}

float h2(int a,int b) {
#undef ACCURATE_R
#define ACCURATE_R val
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE float

 printf("LONG LONG: %016llX\n",RCALL(PACKCALL(h3,0),0));
 return 2.31111;
}

#if USING_UNRELIABLE_GOTO_HACK
inline int g()  __attribute__((always_inline));
#endif


int g() {
#undef ACCURATE_R
#define ACCURATE_R val
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE int

 static int r=0x11112222;
 printf("FLOAT: %f\n",RCALL(PACKCALL(h2,2,3,4),0));
 puts("in g() between calls to h2()");
 printf("FLOAT: %f\n",RCALL(PACKCALL(h2,2,5,6),0));
 return r++;
}


void f() {
#undef ACCURATE_R
#define ACCURATE_R void
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE void

 CATCHPREPARE();
 void *p3=(void*)0x12345678;
 int k;
try {
 asm volatile ("# BEFORE RCALL");
 k=RCALL(PACKCALL(g,0),1,p3);
 asm volatile ("# AFTER RCALL");
 printf("k received value %08lX\n",k);
 puts("in f() between calls to g()");
 printf("g().p3=%08lX\n",p3);
 p3=(void*)0x12345678;
 k=RCALL(PACKCALL(g,0),1,p3);
 printf("k received value %08lX\n",k);
 printf("g().p3=%08lX\n",p3);
} catch(int a) {
 CATCHRESTORE(1,p3);
 printf("Oho! Exception caught!\n");
#ifdef PRECISE_COUNTER
printf("right after catching: gStackHeight is %d\n",accurate::gStackHeight);
printf("right after catching: gAuxStackHeight is %d\n",accurate::gAuxStackHeight);
#endif
 printf("ALERT: p3 is: %08lX\n",p3);
 p3=(void*)0xFEDCBA98;
 printf("ALERT: p3 before gc() is: %08lX\n",p3);
#if defined(PRECISE_COUNTER) || defined(PRECISE_THUNK)
  CALL(PACKCALL(((void(*)(accurate::LocalState*))&accurate::LocalState::gc),1,&myState),1,p3);
#endif
 printf("ALERT: p3 after  gc() is: %08lX\n",p3);
}
}


void userMain() {
#undef ACCURATE_R
#define ACCURATE_R void
#undef ACCURATE_R_TYPE
#define ACCURATE_R_TYPE void

 printf("Starting...\n");
 CALL(PACKCALL(f,0),0);
 printf("First run completed. Testing exceptions...\n");
 testExceptions=1;
 CALL(PACKCALL(f,0),0);

 printf("The end!\n");
 exit(0);
}


//
//
///////////////////////////////////////////////////////////

int main()
{
  // setUberTrickTester();

 // userMain() cannot return:
 // the counter implementation would not be happy.

 // if you do not use the DO_GC() macro,
 // the do_gc param can be NULL
#if defined(PRECISE_COUNTER) || defined(PRECISE_THUNK)
  myState.run(userMain, do_gc);
#else
  userMain();
#endif
}
