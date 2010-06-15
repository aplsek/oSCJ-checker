#include <assert.h>
#include <stdio.h>
#include <setjmp.h>
#include <string.h>
#include <signal.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>

//#define DEBUG_PRECISE 1

// dummy defs, used ONLY to test the precise code with thunkTester.cc
#ifdef THUNKTESTER_TESTING
# include "thunkTester.hh"
#else
// this is the normal alternative
# include "j2c.hh"		// pick up CONTEXT_SIZE, AppExn_t, and APP_THROW
#endif

#include "precise.hh"		// redundant (included from j2c.hh)
#include "md.h"
#include "fdutils.h"


namespace accurate {

#if defined(PRECISE_THUNK) && defined(CEXCEPTIONS)
  struct GCException GCException_singleton;
#endif


#ifdef THUNKTESTER_TESTING
# define ubprintf(...) printf(__VA_ARGS__)
# define j2c_signalEvent() do {} while(0)
#endif

    //////////////////////////////////////////
    //
    // The Global State
    //
    // uses constructor and destructor to automatically
    GlobalState theGlobalState; 

    //////////////////////////////////////////
    //
    // BackupStack encapsulates the information
    // necessary to save the stack state before
    // throwing a SaveException, and to restore
    // it afterwards.
    //
    // There is only one thread at any given
    // time that unwinds its stack in response
    // to a chain of SaveExceptions, so this
    // structure is actually unique, and
    // stored into the GlobalState
    //
    struct BackupStack {
	unsigned char bak[BAKSIZE]; // used during unwinding
	sigjmp_buf cont; // sigjmp_buf, NOT A jmp_buf !!
#ifndef SJB_SP	
	int sp;
#endif	
    };


    static void signalHandler( int arg ) {
	BackupStack *bs = theGlobalState.backupStack;
	LocalState *st  = theGlobalState.currentLocalState;
#ifndef SJB_SP
	int len = ((int)st->base) - bs->sp;
	// On linux, MINSTKSIZE may be way to small for a call to
	// printf!
	// stwu    r1,-8448(r1)
// 	printf("signalHandler(%d) [r1=%p]: "
// 		    "restore %d bytes of stack starting at %p from %p\n",
// 		    arg, __builtin_frame_address(0),
// 		    len, (void *) JMPBUF_FP(bs->cont), bs->bak);
//        assert( ((int)st->base) - JB_REG(bs->cont, SJB_FP) <= BAKSIZE );
	memcpy((void*)bs->sp,
	       bs->bak,
	       ((int)st->base) - bs->sp);
#else

// on the ARM the frame pointer can be ABOVE the frame.
// Must save up to the current stack pointer, or else the portion of stack
// that stores the locals of "do_gc()" gets corrupted.
// (Actually, saving up to the current stack pointer for *all* the
// supported architectures would be a much better idea).

	int len = ((int)st->base) - JB_REG(bs->cont, SJB_SP);
	// On linux, MINSTKSIZE may be way to small for a call to
	// printf!
	// stwu    r1,-8448(r1)
// 	printf("signalHandler(%d) [r1=%p]: "
// 		    "restore %d bytes of stack starting at %p from %p\n",
// 		    arg, __builtin_frame_address(0),
// 		    len, (void *) JMPBUF_FP(bs->cont), bs->bak);
	memcpy((void*)JB_REG(bs->cont, SJB_SP),
	       bs->bak,
	       ((int)st->base) - JB_REG(bs->cont, SJB_SP));
#endif

#if defined(SIGALTSTACK_REQUIRED)
	if (sigaltstack(&theGlobalState.altStack,0)<0) {
	    perror("sigaltstack");
	    abort();
	}
#endif
	siglongjmp(bs->cont, 1);
    }

    static inline void saveBackupStack() __attribute__((always_inline));
    // call this routine before beginning to unwind using SaveExceptions
    static inline void saveBackupStack() {
	BackupStack *bs = theGlobalState.backupStack;
	LocalState *st  = theGlobalState.currentLocalState;
#ifndef SJB_SP
#if !defined(LINUX_X86) && !defined(SOLARIS_X86) && !defined(NETBSD_X86) 
  #error "With undefined SJB_SP, stack unwinding is only implemented for x86"
#endif  
        asm("push %%esp ; pop %0" : "=r"(bs->sp));
        
	int len = ((int)st->base) - bs->sp;
	PRECISE_LOG("saveBackupStack() %d bytes of stack "
		    "starting at %p to %p\n",
		    len, bs->sp, bs->bak);
#ifndef NDEBUG
        if (len > BAKSIZE ) {
          ubprintf("Stack size is too large to save: %d (allocated buffer size is %d)\n",
            len, BAKSIZE);
          assert(!"The buffer size can be increased via constant CONTEXT_SIZE in j2c.hh\n");
        }
#endif        
         
        assert( len <= BAKSIZE );		    
	memcpy(bs->bak, (void *)(bs->sp), len);
#else
	int len = ((int)st->base) - JB_REG(bs->cont, SJB_SP);
	PRECISE_LOG("saveBackupStack() %d bytes of stack "
		    "starting at %p to %p\n",
		    len, (void *) JB_REG(bs->cont, SJB_SP), bs->bak);
	memcpy(bs->bak, (void*)JB_REG(bs->cont, SJB_SP), len);
#endif
    }

    // simple wrapper around the setjmp
#define setjmpBackupStack() sigsetjmp(theGlobalState.backupStack->cont, 1)

    // after the first scan, restore the stack and
    // prepare for the actual gc() and the second phase

    static void rewind () __attribute__ ((noreturn));
    static void rewind() {
        PRECISE_LOG("In rewind...\n");
	DIAG_PRECISE
	    (struct sigaltstack oss;
	     struct sigaction oact;
	     sigset_t set;
	     sigset_t oset;

	     if (sigaltstack(0, &oss)<0) {
		 perror("sigaltstack");
		 abort();
	     }
	     assert(!(oss.ss_flags & (SS_DISABLE|SS_ONSTACK)));
	     if (sigaction(PRECISE_RESERVED_SIGNAL, 0, &oact) < 0) {
		 perror("sigaction");
		 abort();
	     }
	     assert(oact.sa_handler == signalHandler
		    && (oact.sa_flags & SA_ONSTACK));
	     sigemptyset(&set);
	     if (sigprocmask(SIG_UNBLOCK, &set, &oset) < 0) {
		 perror("sigprocmask");
		 abort();
	     }
	     if (sigismember(&oset, PRECISE_RESERVED_SIGNAL)) {
		 puts("rewind: PRECISE_RESERVED_SIGNAL was blocked");
	     });
	raise(PRECISE_RESERVED_SIGNAL);
	assert(!"continue after PRECISE_RESERVED_SIGNAL");
        abort();
    }

#ifndef PRECISE_THUNK
    unsigned gStackHeight;
    unsigned gAuxStackHeight;
#endif

#ifndef PRECISE_THUNK
    AuxFrame *AuxStack::push(int n) {
	int oldfree_ = free_;
	free_ += n+sizeof(AuxFrame)/sizeof(void*);
	assert(!(free_ > sp));
	AuxFrame *a=(AuxFrame*)(stack+oldfree_);
	a->setup(n,1);
	return a;
    }
#else
    AuxFrame *AuxStack::push(void *realFrame, int n) {
	int oldfree_ = free_;
	free_ += n+sizeof(AuxFrame)/sizeof(void*);
	assert(!(free_ > sp));
	AuxFrame *a=(AuxFrame*)(stack+oldfree_);
	a->setup(realFrame, n);
	return a;
    }
#endif

    int AuxStack::empty() {
        return ((sp==AUXSIZE)&&(free_==0));
    }

    AuxFrame *AuxStack::top() {
        if (empty()) return 0; else return (AuxFrame*)(stack+sp);
    }

    AuxFrame *AuxStack::pop() {
      assert(sp < AUXSIZE);
      AuxFrame *ret = top();
      sp+=(sizeof(*ret)/sizeof(void*)) + ret->size;
      return ret;
    }
    AuxFrame *AuxStack::next(AuxFrame *a) {
	AuxFrame *nextFrame=(AuxFrame*)
            (((void**)a)+a->size+sizeof(AuxFrame)/sizeof(void*));
	int newSP=(void**)nextFrame-stack;
	if (newSP==AUXSIZE) return 0;
	if (newSP>AUXSIZE||newSP<0) {
	    puts("Wrong argument or stack corrupted in AuxStack::next()");
	}
	return nextFrame;
    }

#ifdef PRECISE_THUNK
    static void *getThunk() __attribute__((noinline));

    /////////////


    /*
    Each frame stores the returnAddressAddress and the returnAddress
    TO IT, not from it.  That is, if f() creates a frame, it stores in
    the frame the address to which control returns when f()
    terminates.
    */

    void AuxStack::thunkifyStack() {
	// thunkifies all the new frames It loops until it finds an
	// AuxFrame that is already thunkified, or it gets to the
	// bottom.
	PRECISE_LOG("thunkifyStack called\n");
	void *th=getThunk();
	AuxFrame *f=top();
	void *callee = getCurrentActivation(0);
	while (f) {
	    // A new frame should have an unknown PC location
	    while (getCaller(callee) != f->realFrame)
		callee = getCaller(callee);
	    if (getCallerPC(callee) == th)
	      break;
	    assert(!f->returnAddressAddress);
	    f->returnAddressAddress = getCallerPCLoc(callee);
	    f->returnAddress = *f->returnAddressAddress;
	    callee = getCaller(callee);
	    PRECISE_LOG("Thunkify   %p: return address was %p\n",
			f->realFrame, f->returnAddress);
	    assert(f->returnAddress);
	    *f->returnAddressAddress = th;
	    f = next(f);
	}
	// Either we did the whole stack, or we stopped at the first
	// thunk.
	assert(!f || *f->returnAddressAddress == th);
    }

    void AuxStack::dethunkifyTop(void *expectedFrame) {
    	PRECISE_LOG("dethunkifyTop called\n");
	AuxFrame *f = top();
	assert(f->realFrame == expectedFrame);
	PRECISE_LOG("Dethunkify %p: return address back to %p\n",
		    f->realFrame, f->returnAddress);
	*f->returnAddressAddress = f->returnAddress;
    }

    //////////////////////////////////////////
    //
    // Set up the signal handler for USR1
    //
    ////
    //
    //  It is impossible to unwind the stack and then use
    //  a longjmp to a jmp_buf recorded from an inner function.
    //
    //  To get around that problem, we allocate an extra stack,
    //  and call longjmp from there, after restoring the
    //  content of the stack, as it was at the time of the
    //  setjmp.
    //
    //  In order to allocate and use an extra stack, using
    //  a longjmp from there, in a portable way, 
    //  we allocate an altstack for a signal handler,
    //  send ourselves a signal, and jump from there.
    //  SigHandler code originally taken from:
    //  http://evanjones.ca/software/threading.html

    // Since there isn't much more than setting up
    // the signal handler that needs to be done
    // while setting up the GlobalState, we do
    // the whole initialization here
#endif /* PRECISE_THUNK */

    GlobalState::GlobalState():	currentLocalState(0) {
	struct sigaction sa;

	// Create the new stack
	altStack.ss_flags = 0;
	altStack.ss_size = ALTSIZE;

	if (!(altStack.ss_sp=(char*)malloc(ALTSIZE))) {
	    perror("Could not allocate signal handler stack");
	    abort();
	}
	if (sigaltstack(&altStack,0)<0){
	    perror("sigaltstack");
	    abort();
	}
	
	// Set up the custom signal handler
	sa.sa_handler = &signalHandler;
	sa.sa_flags = SA_ONSTACK;
	sigemptyset( &sa.sa_mask );
	sigaction( PRECISE_RESERVED_SIGNAL, &sa, 0 );

	// prepare the BackupStack
	if (!(backupStack=new BackupStack)) {
	    fprintf(stderr,"Could not allocate BackupStack\n");
	    abort();
	}
    }

    GlobalState::~GlobalState() {
	free((char*)altStack.ss_sp);
	delete backupStack;
    }

#ifndef PRECISE_THUNK
    static unsigned gRealAuxStackHeight;
    static unsigned gNumPushesToAuxStack;

    // fascinating observations:
    // - if this is called with events disabled, then we do not need the
    //   CAS loop.
    // - if this is called when we know that a signal occurred, then we do
    //   not need the CAS loop either, since in that case we know that the
    //   aux height has HALFMAX added to it, and so if a second signal occurs
    //   it won't change the aux height, until after we bring the aux height
    //   back down.
    // - what is most interesting is that we only ever call this function
    //   if either of the above conditions hold.  so maybe we can simplify
    //   this.
    extern "C" void resetCounterSignal() {
	if (ACC_POLLCHECK) {
	    unsigned oldVal,newVal;
	    do {
		oldVal=newVal=accurate::gAuxStackHeight;
		if (oldVal==ACC_DOSAVE) {
		    // can't do anything
		} else {
		    newVal&=~ACC_SIGBIT;
		}
	    } while (!CAS32(&accurate::gAuxStackHeight,
			    (jint)oldVal,
			    (jint)newVal));
	}
    }
    
    extern "C" void setCounterSignalIfSignaled() {
	if (ACC_POLLCHECK) {
	    unsigned oldVal,newVal;
	    do {
		oldVal=newVal=gAuxStackHeight;
		if (CHECK_EVENTS()) {
		    //ubprintf("CHECK_EVENTS says YES!  in setCounterSignalIfSignaled\n");
		    if (oldVal==ACC_DOSAVE) {
			// can't do anything, though I want to
		    } else {
			newVal|=ACC_SIGBIT;
		    }
		}
	    } while (!CAS32(&gAuxStackHeight,(jint)oldVal,(jint)newVal));
	}
    }
    
    void initCounterSignalEvent() {
	if (ACC_POLLCHECK) {
	    PRECISE_LOG("Initializing counter exit pollcheck.\n");
	    engine_signal_event=setCounterSignalIfSignaled;
	    engine_events_enabled=setCounterSignalIfSignaled;
	    engine_events_disabled=resetCounterSignal;
	}
    }
    
    void counterSetException() {

	assert(gAuxStackHeight!=ACC_DOSAVE);
	gAuxStackHeight|=ACC_EXCBIT;
    }

    void counterClearException() {

	assert(gAuxStackHeight!=ACC_DOSAVE);
	gAuxStackHeight&=~ACC_EXCBIT;
    }

    // runs with events disabled
    void LocalState::saveCounters() {
//	resetCounterSignal();
	stackHeight=gStackHeight;
	auxStackHeight=gAuxStackHeight;
    }
    
    // runs with events disabled
    void LocalState::restoreCounters() {
	gStackHeight=stackHeight;
	gAuxStackHeight=auxStackHeight;
    }
#else
    extern "C" void resetCounterSignal() {
	// no-op
    }
#endif /* PRECISE_THUNK */

    // this should probably be part of
    // globalstate, but I do not want
    // to expose the global state to the
    // user, only instances of localstate
    void LocalState::makeCurrent() {
#ifndef PRECISE_THUNK
	if (theGlobalState.currentLocalState) {
	    PRECISE_LOG("makeCurrent: switching out %p, height = %u, auxHeight = %u\n",
			theGlobalState.currentLocalState,gStackHeight,gAuxStackHeight);
	    theGlobalState.currentLocalState->saveCounters();
	}
	theGlobalState.currentLocalState=this;
	restoreCounters();
	PRECISE_LOG("makeCurrent: switching to %p, height = %u, auxHeight = %u\n",
		    this,gStackHeight,gAuxStackHeight);
#else
	theGlobalState.currentLocalState=this;
#endif
    }

    // XXX: Used outside of PRECISE_THUNK config?
    static void nothing() __attribute__((noinline));
    static void nothing() {}

#ifdef PRECISE_THUNK

    // check this page for info on the PPC stack layout:
    // http://developer.apple.com/documentation/mac/runtimehtml/RTArch-59.html
    //
    // (page does not refer to OSX, but the ideas are roughly the same)
    //
    //////////////////////////////////////////
    //
    /*

    When we're in the thunk, we might have a return value from the routine we
    have just returned from. Such return value must be saved, so that it can be
    properly assigned within the caller, after the RestoreException
    has been served.

    From the "Mac OS Runtime Architectures":

    Function Return

    In the PowerPC runtime environment, floating-point function values are
    returned in register FPR1 (or FPR1 and FPR2 for long double values).
    Other values are returned in GPR3 as follows:

    * Functions returning simple values smaller than 4 bytes (such as
    type SInt8, Boolean, or SInt16) place the return value in the least
    significant byte or bytes of GPR3. The most significant bytes in GPR3
    are undefined.

    * Functions returning 4-byte values (such as pointers, including array
    pointers, or types SInt32 and UInt32) return them normally in GPR3.

    * If a function returns a composite value (for example, a struct or
    union data type) or a value larger than 4 bytes, a pointer must be
    passed as an implicit left-most parameter before passing all the
    user-visible arguments (that is, the address is passed in GPR3, and
    the actual parameters begin with GPR4). The address of the pointer
    must be a memory location large enough to hold the function return
    value. Since GPR3 is treated as a parameter in this case, its value is
    not guaranteed on return.

    The book "PowerPC Runtime Architecture Guide" helpfully adds:

    * Functions returning long long values place the return value in GPR3
    (the 4 high-order bytes) and GPR4 (the 4 low-order bytes)

    ---

    So:
    - If something is returned in registers, it is returned in r3,r4,f1,f2
    - if a larger structure is filled in as a return value,
    the value of r3 (and r4) does not need to be saved.

    (Note: I've experimented a bit, and even structures of size <=4 are
    not passed around in registers, but through an indirect pointer)

    Now, the size of long double on GCC/PPC/64 is 16 bytes, so the return
    value ends up in f1 and f2. However, on GCC/PPC/32 it is just 8 bytes.
    Hence, we do not actually need to keep track of f2.

    r3,r4,f1 also happen to be the regs in which the first arguments are passed.

    In order to catch floating point return values, and long long,
    the thunk is declared as:

    void thunk(int retVal3,int retVal4,double floatRetVal1)

    where retVal3 ends in r3, retVal4 in r4, floatRetVal1 in f1
    */

    //////////////////////////////////////////
    //
    // The thunk!
    //
    // I stuff the regs in the RetPack and throw that
    //

    /*
      To restore the saved return address:
      If this is a leaf:      __asm__("mtlr %0":: "r" (savedReturn));
      If this is not a leaf:  ((void**)BUILTIN_FRAME_ADDRESS_1())[2]=(void*)savedReturn;
    */


#if defined(OVM_PPC) || defined (OVM_ARM)
  /* on the ARM retVal3 gets r0, retVal4 gets r1, floatRetVal1 gets fp0 */
    void thunk(int retVal3,int retVal4,double floatRetVal1)
	__attribute__((used));
    void thunk(int retVal3,int retVal4,double floatRetVal1) {
	// Restore the saved return address:
	LocalState *st = theGlobalState.currentLocalState;
	st->auxStack.dethunkifyTop(getCurrentActivation(1));
	st->ret.setRetPack(retVal3, retVal4, floatRetVal1);
	st->exn.kind = plainRestore;
	throw st->exn;
    }

#else /* OVM_X86 */

// int is 32 bits on i686. Should use a different def in 64-bit mode

    void thunk(int eax,int edx)
	__attribute__((used,regparm(2)));
    void thunk(int eax,int edx) {
#ifdef CEXCEPTIONS
        if (cur_exc) {
          PRECISE_LOG("Throwing exception through thunk\n");
          return;
        }
#endif    
	// Restore the saved return address:
	LocalState *st = theGlobalState.currentLocalState;
	st->auxStack.dethunkifyTop(getCurrentActivation(1));
	// retrieve the floating point return value from
	// the top of the x87 stack (if present)
	volatile long double floatPark=0.0;
        uint16_t flags;
	__asm__ __volatile__ ("fnstsw %w0":"=a"(flags));
	// top will be 0 if x87 stack is empty, 7 if 1 word on the stack,
	// 6 if 2 words on the stack and so on.
	int top=(flags>>11)&7;
	if (top>0) { // there is a floating point result, get it out
	    __asm__ __volatile__ ("fstpt %0":"=m"(floatPark));
	}
	st->ret.setRetPack(eax, edx, floatPark);
	st->exn.kind = plainRestore;
#ifdef CEXCEPTIONS
        GCException_singleton.kind = plainRestore;
        cur_exc = (HEADER *)&GCException_singleton;
//        cur_exc_dom = 1;
        PRECISE_LOG("In thunk, throwing exception...\n");
        return ;
#else
    	throw st->exn;
#endif    	
    }

#endif



    // thunkCatcher is called when control ends up in the thunk
    // because of an application-level exception.  The proper course of
    // action is to save the exception, and throw the GCException.
    // This will force the caller's frame to be updated, and the
    // AppExn will be rethrown on the way out of XCALL.
    //
    // on x86, thunkCatcher must take its argument in eax, rather than
    // on the stack.  If any arguments are pushed on the stack, the
    // location of the top frame's return pc changes, and then we are
    // screwed.
#if defined(OVM_PPC) || defined(OVM_ARM)
    void thunkCatcher(AppExn_t appExn) __attribute__((noinline));
#else /* OVM_X86 */
    void thunkCatcher(AppExn_t appExn) __attribute__((noinline,regparm(1)));
#endif
    void thunkCatcher(AppExn_t appExn) {
	PRECISE_LOG("Exception caught by thunkCatcher()\n");
	// restore_original_return_address_in_this_frame
	LocalState *st = theGlobalState.currentLocalState;
	st->auxStack.dethunkifyTop(getCurrentActivation(1));
	st->exn.kind = userException;
	st->appExn = appExn;
#ifdef CEXCEPTIONS
        st->appExnDom = cur_exc_dom;
        GCException_singleton.kind = userException;
        cur_exc = (HEADER *)&GCException_singleton;
//        cur_exc_dom = 1;
        PRECISE_LOG("In thunkCatcher, throwing exception...\n");
        return ;
#else        	
	throw st->exn;
#endif	
    }


// see below for details on the ARM version.

#if !defined (OVM_ARM)


    // when jumping directly to restart, below, the
    // prologue has NOT been executed. That happens
    // upon return from a function, while falling
    // into the thunk. Control goes to restart,
    // and then to the thunk, that will save the
    // registers and continue normally.

    // when arriving here because of an exception,
    // the exception unwinding code will see this
    // return address (restart) and jump to the
    // catch handler. At this point, the frame
    // and all the registers will be again those
    // of the caller of the function from which
    // this would have been the thunk. Every action
    // besides jumping might corrupt registers,
    // so we must be careful.
    // rewind() causes no problems, since it will
    // overwrite the stack completely anyway, and
    // use a longjmp afterwards.
    // thunkCatcher() will create its own frame,
    // so we're safe.
    // NOTE: the exception handler will always
    // begin with an automatic call to "__cxa_begin_catch",
    // that grabs the exception object. It can't be avoided.


    // GCC 4.0 optimizes too much the calculation of
    // exception ranges. In particular, even if the
    // flag -fnon-call-exceptions is used, it seems
    // not to care about the fact that asm blocks
    // might throw exceptions, even if memory is clobbered.
    // In order to convince it to cooperate, the
    // following hack is used, informing gcc 4.0 that
    // yes, there really is a chance that an exception
    // will be generated. If at least one call to
    // forceExceptionHandling() is present, an
    // exception handling range will be generated
    // for the asm as well, otherwise the asm block
    // is not covered. This smells like a gcc bug
    // to me. Anyway, with the call it works.
    
#ifndef CEXCEPTIONS
    
    void forceExceptionHandling() __attribute__((noinline));
    void forceExceptionHandling() {
	volatile static int h=0;
	if (h==0) throw (AppExn_t) 0;
	else if (h==1) throw theGlobalState.currentLocalState->exn;
    }
#endif

#ifdef CEXCEPTIONS

    void *getThunk() {
	volatile static int really=69;
	int doRewind = 0;

	PRECISE_LOG("getThunk called...\n");
        if (really)
		return &&restart;
	restart:
	    // jump to the normal thunk
#if defined(OVM_PPC)
 #if defined(ASM_NEEDS_UNDERSCORE)
	    __asm__ __volatile__ ("b __ZN8accurate5thunkEiid");
 #else
	    __asm__ __volatile__ ("b _ZN8accurate5thunkEiid");
 #endif
#elif defined(OVM_X86)
 #if defined(ASM_NEEDS_UNDERSCORE)
	    __asm__ __volatile__ ("call __ZN8accurate5thunkEii");
 #else
	    __asm__ __volatile__ ("call _ZN8accurate5thunkEii");
 #endif
#else
#error "Unsupported architecture"
#endif

        PRECISE_LOG("In getThunk thunk, after real thunk...\n");
        assert(cur_exc);
        if (cur_exc) {
          if (cur_exc==(HEADER *)&GCException_singleton) {
                PRECISE_LOG("In getThunk thunk, received GC exception\n");
	    // Don't rewind the stack while this exception is active
	        doRewind = 1;
	        cur_exc = 0;
          } else {
            PRECISE_LOG("In getThunk thunk, received user exception\n");
	    thunkCatcher((AppExn_t)cur_exc);
	    return 0;
	    assert(!"thunkCatcher returned");
          }
	}
	PRECISE_LOG("In getThunk thunk, calling rewind...\n");
	assert(doRewind == 1);
	if (doRewind)
	    rewind();
	return 0;
    }

#else
    // proto is above: void *getThunk() __attribute__((noinline));
    void *getThunk() {
	volatile static int really=69;
	int doRewind = 0;
	try {
	    if (really)
		return &&restart;
	    forceExceptionHandling();
	restart:
	    // jump to the normal thunk
#if defined(OVM_PPC)
 #if defined(ASM_NEEDS_UNDERSCORE)
	    __asm__ __volatile__ ("b __ZN8accurate5thunkEiid");
 #else
	    __asm__ __volatile__ ("b _ZN8accurate5thunkEiid");
 #endif
#elif defined(OVM_X86)
 #if defined(ASM_NEEDS_UNDERSCORE)
	    __asm__ __volatile__ ("call __ZN8accurate5thunkEii");
 #else
	    __asm__ __volatile__ ("call _ZN8accurate5thunkEii");
 #endif
#else
#error "Unsupported architecture"
#endif
	    forceExceptionHandling();
	} catch (GCException _) {
	    // Don't rewind the stack while this exception is active
	    doRewind = 1;
	} catch (AppExn_t appExn) {
	    thunkCatcher(appExn);
	    assert(!"thunkCatcher returned");
	}
	assert(doRewind == 1);
	if (doRewind)
	    rewind();
	return 0;
    }
#endif //CEXCEPTIONS

#else   // ARM version


# error "Thunking is not supported on the ARM (and probably never will be)."


    // on the ARM things are rather different. First of all,
    // exception handling works in a very different way, and
    // each time an exception handler is used there are
    // additional routines inserted everywhere. Those routines
    // are executed as part of the normal execution, therefore
    // jumping in the middle of the "try" block without executing
    // the normal code would not work at all.

    // Basically, the "try" (or wherever GCC decides that the code
    // that can throw exceptions begins) is converted into a
    // farily long and complicated sequence that creates a record
    // on the stack, and fills it with a copy of some registers
    // and pointers to various complex internal tables.

    // When an exception occurs, the unwinder detects those tables
    // acts accordingly. In the case of thunking, such a record
    // would have to be added "retroactively" to each routine
    // currently suspended on the stack, so that when an exception
    // is thrown the unwinder correctly falls into the alternate
    // exception handler.

    // Creating the appropriate records is virtually impossible.
    // Even recycling the existing record created by the original
    // try/catch around the call that is later thunkified, things
    // get very hairy and fragile, extremely impractical.

    // Finally, the strong aspect of thunking is usually the
    // speed gain that it allows the code to achieve, as
    // exceptions normally have a cost only when taken. But on
    // the ARM, following the brain-dead ABIs that they have
    // very arbitrarily enacted, each try/catch has a very
    // significant cost even if no exception is ever thrown.

    // Therefore, even if it were possible to implement the
    // whole mechanism correctly, the performance would be
    // abysmal, defying the whole point.

    // Consequently, thunking has not been, and probably
    // never will be, implemented on the ARM (unless
    // someome wisens up and decides to write a new ABI
    // for the ARM for exclusive GCC use, that does away
    // with the existing brain-dead ABIs).

    //-----------------------------------------------------
    //-----------------------------------------------------

    // below is some code that was originally written
    // for the thunk handling for the arm. The rest of the
    // of the code has been removed, the following bit
    // is the jump sequence:

/*

#if defined(ARM_THUMB)
#define ARM_THUMB_BIT 1
#define ARM_THUMB_BIT_STRING "1"
#else
#define ARM_THUMB_BIT 0
#define ARM_THUMB_BIT_STRING "0"
#endif

	    __asm__ __volatile__ ("ldr r2,.Lthunk_indirect_vector\n\t"
				  "bx r2\n"
  ".Lthunk_indirect_vector:\n\t.word _ZN8accurate5thunkEiid + " ARM_THUMB_BIT_STRING  "\n\t"

*/

    // Other considerations of interest:


    // There is a mismatch in the handling of floating point return values.
    // They are returned in f0, but passed as arguments in r0..r3 (or stack).
    // In consequence, the thunk will never see a floating point (float/double)
    // where it should be if it were passed as an argument. The thunk should
    // "magically" extract the return floating point value from "f0"
    // first thing in the code.

    // Also: on the ARM, GCC allows a "naked"
    // function attribute, that does not add prologue and
    // epilogue to functions. That might be useful for
    // some tricky bits of code.


    
#endif /* OVM_ARM */




#endif /* PRECISE_THUNK */

    /////////////////////////////////////////
    //
    // A wrapper around the user-defined gc
    // operation that ends up being executed
    // after the first stack traversal
    // each time the macro DO_GC() is used
    //

    // We need to be certain about the stack
    // depth from the gc() call in user code to the point
    // in which we use __builtin_frame_address().
    //
    // if you switch threads, remember to use
    // localState.makeCurrent(); before resuming execution.
#ifdef PRECISE_THUNK
    void LocalState::gc() {
        PRECISE_LOG("LocalState::gc called\n");
	assert(theGlobalState.currentLocalState == this);
	nothing(); // make sure gc() is not a leaf routine

	if (!setjmpBackupStack()) {
	    saveBackupStack();
	    assert(theGlobalState.currentLocalState == this);
	    PRECISE_LOG("About to traverse the stack\n");
	    exn.kind = save;
#ifdef CEXCEPTIONS
            GCException_singleton.kind = save;
            cur_exc = (HEADER *)&GCException_singleton;
//            cur_exc_dom = 1;
            return ;
#else            
	    throw exn;
#endif	    
	} else {
	    assert(theGlobalState.currentLocalState == this);
	    PRECISE_LOG("After longjmp, in gc()\n");
	    auxStack.compact();
	    auxStack.thunkifyStack();
	    userGC(&auxStack);
	}
    }
#else
    void LocalState::gc() {
	// runs with events disabled, so we can clear the HALFMAX from the
	// aux height without having to set it back
	assert(theGlobalState.currentLocalState == this);
	nothing(); // make sure gc() is not a leaf routine
	saveCounters();

	if (!setjmpBackupStack()) {
	    saveBackupStack();
	    assert(theGlobalState.currentLocalState == this);
	    PRECISE_LOG("About to traverse the stack\n");
	    resetCounterSignal();
	    gRealAuxStackHeight=gAuxStackHeight;
	    gNumPushesToAuxStack=0;
	    gAuxStackHeight=ACC_DOSAVE;
	} else {
	    assert(theGlobalState.currentLocalState == this);
	    PRECISE_LOG("After longjmp, in gc()\n");
	    restoreCounters();
	    gAuxStackHeight=gRealAuxStackHeight+gNumPushesToAuxStack;
	    auxStack.compact();
	    userGC(&auxStack);
	}
    }
#endif

    /////////////////////////////////////////
    //
    // A wrapper around the main routine of
    // the user code, the lowest level that
    // will be scanned before a gc
#ifdef PRECISE_THUNK
    void LocalState::run(void(*userMain)(),void (*do_gc)(AuxStack *)) {
	running=true;
	makeCurrent();
	base=getCurrentActivation(1);
	userGC=do_gc;
	int normal = -1;
#ifdef CEXCEPTIONS	
        cur_exc = 0;
#else
	try {
#endif	
	    userMain();

#ifdef CEXCEPTIONS
        if (cur_exc==(HEADER *)&GCException_singleton) {
          normal = 0;
          cur_exc = 0; //? is this correct ? probably yes
        } else {
	    normal = 1;        
        }
#else	    
	    normal = 1;
	} catch (GCException exn) {
	    normal = 0;
	}
#endif	
	assert(normal == 0 || normal == 1);
	if (!normal)
	    rewind();
	running=false;
    }
#else
    // this _should_ be running with events disabled, though I am not 100% sure
    void LocalState::run(void(*userMain)(),void (*do_gc)(AuxStack *)) {
	running=true;
	makeCurrent();
	base=getCurrentActivation(1);
	userGC=do_gc;
	gStackHeight=0;
	gAuxStackHeight=0;
	userMain();
	if (accurate::gAuxStackHeight==ACC_DOSAVE) {
	    rewind();
	}
	running=false;
    }
#endif

#ifndef PRECISE_THUNK
    static void debugCounters(const char *where) {
	PRECISE_LOG("counters in %s: height = %u, auxHeight = %u\n",
		    where,
		    accurate::gStackHeight,
		    accurate::gAuxStackHeight);
    }
    
    bool XCALL_chckret() {
#ifdef DEBUG_PRECISE    
	debugCounters("XCALL_chckret");
#endif	
	if (accurate::gAuxStackHeight==ACC_DOSAVE ||
	    (accurate::gAuxStackHeight&ACC_EXCBIT)) {
	    return true;
	} else {
	    j2c_signalEvent();
	    return false;
	}
    }
    
    AuxFrame *XCALL_popFrame(unsigned nptrs,
			     unsigned oldVal /*old value of aux stack height*/) {
#ifdef DEBUG_PRECISE			     
	debugCounters("XCALL_popFrame");
#endif	
	unsigned goodVal=oldVal;
	unsigned sub=0;
/*	
	if (goodVal&ACC_SIGBIT) {
	    j2c_signalEvent();
	    goodVal&=~ACC_SIGBIT;
	}
*/	
	if (goodVal&ACC_EXCBIT) {
	    goodVal&=~ACC_EXCBIT;
	}
	PRECISE_LOG("in XCALL_popFrame(), nptrs = %u, oldVal = %u, goodVal = %u\n",
		    nptrs,oldVal,goodVal);
	// at this point in the code, provided that no other signals have happened
	// since slightly before entry into this function, gAuxStackHeight will
	// not have HALFMAX.  (of course, there is a good chance that there was
	// a signal somewhere in there, so gAuxStackHeight could have HALFMAX.)
	if (accurate::gStackHeight >= goodVal ||
	    nptrs==0) {
	    PRECISE_LOG("returning NULL");
	    return NULL;
	}
	sub++;
	while (accurate::gStackHeight < (goodVal-sub)) {
	    PRECISE_LOG("in pop loop: gStackHeight = %u, goodVal = %u, sub = %u\n",
			accurate::gStackHeight,goodVal,sub);
	    sub++;
	    accurate::theGlobalState.currentLocalState->auxStack.pop();
	}
	AuxFrame *FRAME=
	    accurate::theGlobalState.currentLocalState->auxStack.top();
	accurate::theGlobalState.currentLocalState->auxStack.pop();
#ifndef NDEBUG	
	if (FRAME->size!=nptrs) {
	    ubprintf("In popFrame(): size = %u but nptrs = %u\n",
		     FRAME->size,nptrs);
	    abort();
	}
#endif	
        accurate::gAuxStackHeight = oldVal-sub;
	FRAME->save=0;
	return FRAME;
    }
    
    AuxFrame *XCALL_getFrame(unsigned nptrs) {
#ifdef DEBUG_PRECISE    
	debugCounters("XCALL_getFrame");
#endif	
	PRECISE_LOG("nptrs = %u\n",nptrs);
#ifndef NDEBUG	
	if (nptrs==0) {
	    ubprintf("In getFrame(): nptrs = 0!\n");
	    abort();
	}
#endif	
	if (gAuxStackHeight==ACC_DOSAVE) {
	    PRECISE_LOG("real aux stack height = %u\n", gRealAuxStackHeight);
	    if (gStackHeight < gRealAuxStackHeight) {
		PRECISE_LOG("XCALL_getFrame(): rewinding!\n");
		rewind();
		abort(); // tell gcc that this function doesn't return
	    } else {
		PRECISE_LOG("XCALL_getFrame(): pushing new frame!\n");
		gNumPushesToAuxStack++;
		AuxFrame *result =
		    theGlobalState.currentLocalState->auxStack.push(nptrs);
		return result;
	    }
	}
	
	PRECISE_LOG("XCALL_getFrame(): going into popFrame()\n");
	return XCALL_popFrame(nptrs,gAuxStackHeight);
    }
#else /* PRECISE_THUNK */
    void resume() {
	LocalState *st = theGlobalState.currentLocalState;
#ifdef CEXCEPTIONS
        cur_exc = 0;
#endif        		

	if (st->exn.kind == save) {
	    PRECISE_LOG("In resume, save - propagating...\n");
	    // We are propagating st->exn up the stack until we hit
	    // either a thunk, or the bottom frame.  Keep going.
#ifdef CEXCEPTIONS
            GCException_singleton.kind = save;
            cur_exc = (HEADER *)&GCException_singleton;
//            cur_exc_dom = 1;
            return;
#else            	    
	    throw;
#endif	    
	} else if (st->exn.kind == userException) {
	    PRECISE_LOG("In resume, propagating real user exception\n");
	    // We have restored the caller's frame, and it is safe to
	    // allow the application-level exception to flow into it
#ifdef CEXCEPTIONS
            cur_exc = (HEADER *)st->appExn;
            cur_exc_dom = st->appExnDom;
            return;
#else    	    
	    APP_THROW(st->appExn);
#endif	    
	} else {
	    PRECISE_LOG("In resume, resuming using real function's return value\n");
	    // We have restored local variables and the function's
	    // return value.  It is safe to proceed normally.
	}
    }



    //////////////////////////////////////////////////////////////////
    //
    // getFrameX is an auxiliary routine used to obtain the frame used
    // for saving and restoring pointers in the caller wrapper.  It
    // should ONLY be used from there.
    //
    // When the thread is saving pointer, getFrame allocates an
    // AuxFrame for the calling function and returns it.  Otherwise,
    // getFrame obtains the calling function's frame from the top of
    // the AuxStack.
    AuxFrame *getFrameX(int nptrs) {
	LocalState *st = theGlobalState.currentLocalState;
#ifdef CEXCEPTIONS
        cur_exc = 0;
#endif        		

	if (st->exn.kind == save) {
	  return st->auxStack.push(getCurrentActivation(1),  nptrs);
	} else {
	    AuxFrame *ret = st->auxStack.pop();
	    ret->realFrame == getCurrentActivation(1);
	    assert(ret->realFrame);
	    return ret;
	}
    }
#endif /* PRECISE_THUNK */
}
