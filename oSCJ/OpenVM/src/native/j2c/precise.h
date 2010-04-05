#ifndef PRECISE_HH
#define PRECISE_HH

/*
 * TODO:
 *
 * Here:
 * 1. get rid of PACKCALL macro, it is no longer needed
 * 2. replace setjmp/raise/longjmp with VM-level context switches
 * 3. make LHS a parameter to RCALL
 * 4. reduce number of blocks executed when restoring pointers
 *
 * On the j2c side:
 * 1. get rid of PACKCALL macro, it is no longer needed
 * 2. make LHS a parameter to RCALL
 * 3. elminate GCException/e_java_lang_Throwable handler nesting
 */
#include <signal.h>
#define PRECISE_RESERVED_SIGNAL SIGUSR2

//////////////////////////////////////////
//
// Constants
//


// Print diagnostic messages
//

#ifdef DEBUG_PRECISE
//#define DEBUG_PRECISE 1
#undef DEBUG_PRECISE
#endif

#if DEBUG_PRECISE
// do NOT add a ; after DIAG_PRECISE (makes debugging easier)
#define DIAG_PRECISE(x) {x;}
#define PRECISE_LOG(...) ubprintf(__VA_ARGS__)
#else
#define DIAG_PRECISE(x)
#define PRECISE_LOG(...)
#endif

// Maximum size in bytes of each per-thread actual stack,
//
#ifndef BAKSIZE
#define BAKSIZE 0x20000
#endif

// auxsize is the number of pointers or ints in the auxiliary stack (number of bytes / 4)
//
#ifndef AUXSIZE
#define AUXSIZE (BAKSIZE/4)
#endif

// altsize is the size in bytes of the little stack used by the signal handler
// must be at least MINSIGSTKSZ
#ifndef ALTSIZE
#define ALTSIZE (MINSIGSTKSZ+0x2000)
#endif


namespace accurate {
  struct BackupStack;
  struct LocalState;

  struct GlobalState {
    // backup for the real stack,
    // used when unwinding before gc
    BackupStack *backupStack;

    // alternate stack structure for the signal handler
    stack_t altStack;

    // the current local state must ALWAYS
    // be reachable from here
    LocalState *currentLocalState;

    GlobalState();
    ~GlobalState();
  };

  // needed?
  extern GlobalState theGlobalState;

  //////////////////////////////////////////
  //
  // The definition of the Auxiliary Stack
  //
  // Each thread has one.
  //

  //
  // AuxStack contains AuxFrames. AuxStack grows downwards.
  // The AuxFrames are actually allocated within the
  // body of the AuxStack itself

  struct AuxFrame {
      int size; // data[size]

#ifndef PRECISE_THUNK
      unsigned save;
#else /* PRECISE_THUNK */
	void *realFrame;
	// see the comments in the implementation file for important info
	void **returnAddressAddress;
	void *returnAddress;
#endif /* PRECISE_THUNK */

      void *data[0];
#ifndef PRECISE_THUNK
      void setup(int n,unsigned save) {
	  size=n;
	  this->save=save;
      }
#else /* PRECISE_THUNK */
      void setup(void *frame,int n) { // no constructors, the memory used for
	    realFrame = frame;	// AuxFrames is actually an array of bytes
	    size = n;		// within AuxStack
	    returnAddress = 0;
	    returnAddressAddress = 0;
      }
#endif /* PRECISE_THUNK */
  };

  struct AuxStack {
     // AuxFrames are stored in this array, used as a plain memory block
    void *stack[AUXSIZE];
    int sp;   // 0..AUXSIZE
    int free_; // 0..AUXSIZE

#ifndef PRECISE_THUNK
    AuxFrame *push(int);
#else
    AuxFrame *push(void *, int);
#endif
    /*

    This "stack" is somewhat of a "kcats", in that it
    grows in the wrong directions, from the innermost
    routine to the outermost routine.

    Since part of the innermost frames can be
    released, and more allocated (again in the
    wrong direction), the structure becomes a bit
    tricky.

    The implementation below, possibly not the
    most efficient ever, works as follows:

    Initially the stack is empty:

    +-+-+-+-+-+-+-+-+-+-+
    | | | | | | | | | | |   => growing addresses
    +-+-+-+-+-+-+-+-+-+-+
    ^                   ^
    offs 0              offs SIZE

    We traverse the frames from the innermost
    to the outermost, allocating them from the
    bottom going up:

    +-+-+-+-+-+-+-+-+-+-+
    |#| | | | | | | | | |
    +-+-+-+-+-+-+-+-+-+-+
    ^ ^                 ^
    0 free              SIZE

    +-+-+-+-+-+-+-+-+-+-+
    |#|#| | | | | | | | |
    +-+-+-+-+-+-+-+-+-+-+
    ^   ^               ^
    0   free            SIZE

    +-+-+-+-+-+-+-+-+-+-+
    |#|#|#| | | | | | | |
    +-+-+-+-+-+-+-+-+-+-+
    ^     ^             ^
    0     free          SIZE

    Now the outermost is the one at the highest
    address. Once the traversal is complete, the
    auxiliary stack is moved near the top:

    +-+-+-+-+-+-+-+-+-+-+
    | | | | | | | |#|#|#|
    +-+-+-+-+-+-+-+-+-+-+
    ^             ^     ^
    0             sp    SIZE
    free                   

    Traversal during the second stage is done
    from the innermost frame (just below
    offset free) towards the bottom.
    Let's assume that only one frame is copied back
    and freed:

    +-+-+-+-+-+-+-+-+-+-+
    | | | | | | | | |#|#|
    +-+-+-+-+-+-+-+-+-+-+
    ^               ^   ^
    0               sp  SIZE
    free

    and a few new routines are called, triggering
    a new gc(). The newly allocated routines are
    traversed as before, leading to:

    +-+-+-+-+-+-+-+-+-+-+
    |#|#|#|#| | | | |#|#|
    +-+-+-+-+-+-+-+-+-+-+
    ^       ^       ^   ^
    0       free    sp  SIZE

    Once the newly allocated frames are scanned,
    compaction is performed again:

    +-+-+-+-+-+-+-+-+-+-+
    | | | | |#|#|#|#|#|#|
    +-+-+-+-+-+-+-+-+-+-+
    ^       ^           ^
    0       sp          SIZE
    free


    and so on.

    ---------------------------------------------------

    To push new frames, after a compaction or at the
    beginning, call multiple times:

    AuxFrame *a=aux.push(realFrame,size);

    after the first traversal:

    aux.compact();

    ----------

    After compaction, the second traversal can be done with:

    AuxFrame *a=aux.top(); // the innermost
    aux.pop(); // removes the topmost frame

    or

    AuxFrame *a=aux.next(a);

    and

    if (aux.empty()) ...
 
    The data in each Auxframe is accessed as:

    a->size; // == number of ptrs
    a->data[0]; a->data[1]; ... a->data[a->size-1];

    */
    void compact() {
      memcpy(stack+sp-free_,stack,sizeof(void*)*free_);
      sp-=free_;
      free_=0;
    }

    AuxFrame *pop();
    int empty();
    AuxFrame *top();

    AuxFrame *next(AuxFrame *a);
    AuxStack():sp(AUXSIZE),free_(0) {}
    ~AuxStack() {}

#ifdef PRECISE_THUNK
    void dethunkifyTop(void *expectedFrame);
    void thunkifyStack();
#endif /* PRECISE_THUNK */
  };

#ifdef PRECISE_THUNK
    //////////////////////////////////////////
    //
    // the restore clause of the call wrapper   
    // is now a catch(GCException)
    // This enum helps us detecting whether
    // we are saving pointers, restoring pointers
    // on a normal return, or restoring pointers
    // on an exceptional return
    enum ExceptionKind {
	save,
	plainRestore,
	userException
    };


    struct GCException {
	ExceptionKind kind;
    };

#ifdef CEXCEPTIONS
    extern struct GCException GCException_singleton;
#endif

    //////////////////////////////////////////
    //
    // the return value is no longer stored
    // in the exception. It ends up into
    // this structure instead.
    //
    struct RetPack {
	int r3,r4;
	double f1;
	void setRetPack(int a,int b,double f) {r3=a;r4=b;f1=f;}
    };
// in x86 r3 holds eax, r4 holds edx, f holds the x87 stack top
// on the ARM r3 gets r0, r4 gets r1, f gets fp0


#endif /* PRECISE_THUNK */

  struct LocalState {
      void *base;
      AuxStack auxStack;
      bool running;
      
#ifdef PRECISE_THUNK
	RetPack     ret;
	AppExn_t    appExn;
#ifdef CEXCEPTIONS
	int appExnDom;
#endif /* CEXCEPTIONS */
	GCException exn;
#else
        unsigned stackHeight;
        unsigned auxStackHeight;
#endif /* PRECISE_THUNK */

      LocalState():
	  running(false)
#if !defined(PRECISE_THUNK)
	  , stackHeight(0), auxStackHeight(0)
#endif
      {}

#ifndef PRECISE_THUNK
       void saveCounters();
       void restoreCounters();
#endif

      // call everythime there is a
      // thread switch:
      void makeCurrent();
      
      // use this method to start execution
      // of the user program
      void run(void(*userMain)(),void (*do_gc)(AuxStack *)); // begin execution

      // XXX: not used by ovm
      void (*userGC)(AuxStack *); 
      
      // I must be certain that this routine is not inlined,
      // and that it calls at least one routine that is not inlined
      // (so that it is not a leaf routine)
      void gc();   // request gc
  };

  extern "C" void resetCounterSignal();

#ifndef PRECISE_THUNK
    // the counter implementation's barriers double as a pollcheck.
    // here's how that works:

    // SIGNALING:
    // In addition to manipulating the one true flag, the auxStackHeight
    // is modified to be greater than the stackHeight (auxStackHeight+=HALFMAX)

    // TRIGGERING ON TRADITIONAL POLLCHECK:
    // the j2c_signalEvent function is called, and the auxStackHeight is
    // returned to an ordinary value
    
    // TRIGGERING ON COUNTER BARRIER POLLCHECK:
    // the j2c_signalEvent function is called, which resets the auxStackHeight
    // and possibly runs the event processing code (based on the one true
    // word).  note that this means that the auxStackHeight signal may be
    // lost when events are disabled.

    // DOING STACK WALKING:
    // the auxStackHeight is restored to a normal value, and then after
    // walking is done, it might be set back to auxStackHeight+HALFMAX if
    // the one true flag demands it.
    
    // WHEN EVENTS ARE ENABLED:
    // the auxStackHeight is updated to reflect the value of the one true
    // flag
    
    // FIXME: there is something missing here.  in the eventmanager code,
    // great care was taken to ensure that the events enabled property
    // is handled correctly.  I don't see those elements here, which makes
    // me feel awfully uncomfortable.
    // NOTE: the reason why the eventmanager code is so cautious is because
    // it has to be able to handle signals from other threads.  for the
    // purposes of this stuff, we'll pretend that that doesn't happen.
    
  extern "C" void setCounterSignalIfSignaled();
  void initCounterSignalEvent();
    
  void counterSetException();
  void counterClearException();
    
  bool XCALL_chckret();
  AuxFrame *XCALL_popFrame(unsigned nptrs,unsigned oldVal);
  AuxFrame *XCALL_getFrame(unsigned nptrs);

  extern unsigned gStackHeight;
  extern unsigned gAuxStackHeight;
#else
  extern AuxFrame *getFrameX(int nptrs);
  extern void resume();

    template<typename T>
    inline T restoreRetVal(RetPack *rev);

    // default: int (char, short, unsigned int, void*, int*,...)
    // every simple type that fits in 32bit, that is
    template<typename T>
    inline T restoreRetVal(RetPack *rev) {
	return (T)rev->r3;
    }

    template<typename K>
    inline K restoreLL(RetPack *rev) {
	union transfer {
	    struct {int h;int l;} a;
	    K b;
	} zz;
	zz.a.h=rev->r3;
	zz.a.l=rev->r4;
	return zz.b;
    }
    template<>
    inline long long restoreRetVal<long long>(RetPack *rev) {
	return restoreLL<long long>(rev);
    }
    template<>
    inline unsigned long long restoreRetVal<unsigned long long>(RetPack *rev) {
	return restoreLL<unsigned long long>(rev);
    }


  // on the ARM there are no less than three different
  // calling conventions concerning floating point
  // values. For us, only two variations are actually
  // relevant: when in thumb mode all floating point
  // values are returned in r0 (float) or in r0/r1
  // (double or long double).
  // When using the standard instruction set, and
  // the FPA calling convention (the default for us)
  // all floating point results are returned in f0

#if defined(OVM_ARM) && defined(ARM_THUMB)
    // return r0/r1 as double (maybe the order should be swapped? Likely)
    template<>
    inline double restoreRetVal<double>(RetPack *rev) {
	return restoreLL<double>(rev);
    }
    // return r0/r1 as long double (maybe the order should be swapped? Likely)
    template<>
    inline long double restoreRetVal<long double>(RetPack *rev) {
	return restoreLL<long double>(rev);
    }
    // return r0 as a float
    template<>
    inline float restoreRetVal<float>(RetPack *rev) {
	union transfer {
	    int r;
	    float b;
	} zz;
	zz.r=rev->r3;
	return zz.b;
    }
#else
    // restore f1
    template<>
    inline double restoreRetVal<double>(RetPack *rev) {
	return rev->f1;
    }
    // long double is be longer in PPC/64!
    template<>
    inline long double restoreRetVal<long double>(RetPack *rev) {
	return rev->f1;
    }
    // PPC always stores doubles in floating point registers.
    // therefore float -> double is a nop
    // and double -> float is a rounding (fsrp)
    // but the format stays the same
    template<>
    inline float restoreRetVal<float>(RetPack *rev) {
	return rev->f1;
    }
#endif

    template<typename T> T restore() __attribute__((noinline));
    template<typename T> T restore() {
	LocalState *st = theGlobalState.currentLocalState;
	return restoreRetVal<T>(&st->ret);
    }
#endif

/*
 * Implement basic macros:
 *    CALL(E, N, var1, ... varN)      -- evalutate call E, preserving var0...
 *    CALL0(E)                        -- CALL(E, 0) but potentially faster
 *    RCALL(E, N, var1, ... varN)     -- CALL with return type T
 *    RCALL0(E)                       -- RCALL(T, E, 0) but potentially faster
 *    CATCHPREPARE()                  -- this function may use try/catch
 *    CATCHRESTORE(N, var1, ... varN) -- restore var0... after exception
 *    CHKRET()                        -- equivalent to CALL0?
 *
 * where E represents a function call, written as:
 *
 *    PACKCALL(fun, N, arg1, ... argN)
 *
 * The R?CALL0? family of macros are defined in terms of a family
 * of templates or macros, defined below. Their definition relies on
 * a number of sub-template and macros, defined and in saverestore.hh.
 */

#ifdef PRECISE_THUNK

#ifdef CEXCEPTIONS
#define CEXCEPTION_CHECK \
if (cur_exc) { \
	if (cur_exc==(HEADER *)&accurate::GCException_singleton) { \
		PRECISE_LOG("Leaving method due to GC  exception %x, line %d...\n",cur_exc,__LINE__); \
		CEXCEPTION_METHOD_LEAVE; \
	} else { \
		PRECISE_LOG("Jumping to dispatch code on application exception %x, line %d...\n",cur_exc,__LINE__); \
		CEXCEPTION_JUMP; \
	} \
}
#define CEXCEPTION_PRECISE_GCE_CHECK \
	if (cur_exc==(HEADER *)&accurate::GCException_singleton) { \
		PRECISE_LOG("At GCE_CHECK, leaving method due to GC exception %x, line %d...\n",cur_exc,__LINE__); \
		CEXCEPTION_METHOD_LEAVE; \
	}
#define CEXCEPTION_INIT \
cur_exc = 0;
#else 
#define CEXCEPTION_CHECK
#define CEXCEPTION_INIT
#endif /* CEXCEPTIONS */


// thunking implementation
#include "saverestore.hh"

#define SAVE(ptr,offs)    {						\
  FRAME->data[offs] = ptr;						\
}
#define RESTORE(ptr,offs) {						\
  ptr = (typeof(ptr)) FRAME->data[offs];				\
}

// for this wrapping style, PACKCALL is
// equivalent to a plain call.
#define PACKCALL(CALL,N,...) CALL(__VA_ARGS__)

#define CHCKRET()
#define CATCHPREPARE()
#define CATCHRESTORE(...)

#ifdef CEXCEPTIONS
#define CALL0(e)     ({CEXCEPTION_INIT; e; CEXCEPTION_CHECK; })
#define RCALL0(e) ({CEXCEPTION_INIT ; typeof(e) RET = e; CEXCEPTION_CHECK; RET;})
#else
#define CALL0(E)     E
#define RCALL0(E)    E
#endif

#ifdef CEXCEPTIONS
#define XCALL(PRE,PRE2,MID,POST,call,nptrs,...)                         \
({                                                                      \
    PRE;                                                                \
    cur_exc = 0;                                                        \
    PRE2 call;   	\
    if (cur_exc) {							\
	if (cur_exc==(HEADER *)&accurate::GCException_singleton) {             \
            	accurate::AuxFrame *FRAME=accurate::getFrameX(nptrs);       \
		PRECISE_LOG("Caught GC exception in application\n"); \
		if (accurate::GCException_singleton.kind == accurate::save) {			\
			PRECISE_LOG("GC exception is save, saving pointers...\n"); \
          		XNEST(SAVE,nptrs,##__VA_ARGS__);                            \
		} else {                                                        \
			PRECISE_LOG("GC exception is restore, restoring pointers...\n"); \
            		XNEST(RESTORE,nptrs,##__VA_ARGS__);                         \
            		MID;                                                        \
		}								\
		PRECISE_LOG("GC exception handled, calling resume at line %d...\n",__LINE__); \
	        accurate::resume();                                             \
		CEXCEPTION_CHECK; \
		PRECISE_LOG("Resume finished without exception at line %d...\n",__LINE__); \
        } \
	else {							\
	    PRECISE_LOG("jumping to dispatch call of user exception...\n"); \
		CEXCEPTION_JUMP;					\
	}                                                              \
									\
    } \
    POST;                                                               \
})
#else
#define XCALL(PRE,PRE2,MID,POST,call,nptrs,...)                         \
({                                                                      \
    PRE;                                                                \
    try{                                                                \
        PRE2 call;                                                      \
    } catch(accurate::GCException EX) {                                 \
        if (EX.kind == accurate::save) {                                \
            accurate::AuxFrame *FRAME=accurate::getFrameX(nptrs);       \
            XNEST(SAVE,nptrs,##__VA_ARGS__);                            \
        } else {                                                        \
            accurate::AuxFrame *FRAME=accurate::getFrameX(nptrs);       \
            XNEST(RESTORE,nptrs,##__VA_ARGS__);                         \
            MID;                                                        \
        }                                                               \
        accurate::resume();                                             \
    }                                                                   \
    POST;                                                               \
})
#endif

#define CALL(E, N, ...) XCALL(,,,, E, N,## __VA_ARGS__)
#define RCALL(E,N,...)							\
  XCALL(typeof(E) RET,							\
	RET = ,								\
        RET = accurate::restore<typeof(E)>(),				\
	RET,								\
        E, N,## __VA_ARGS__)

#else

// Counting implementation
#include "saverestore.hh"

#define SAVE(ptr,offs)    {						\
  FRAME->data[offs] = ptr;						\
}
#define RESTORE(ptr,offs) {						\
  ptr = (typeof(ptr)) FRAME->data[offs];				\
}

#ifndef CEXCEPTIONS

#define ACCURATE_R void
#define ACCURATE_R_TYPE void // the actual return type

#endif

#define BEGIN_OUTER_BARRIER
#define END_OUTER_BARRIER
#define BEGIN_INNER_BARRIER
#define END_INNER_BARRIER

#ifndef CEXCEPTIONS
    // change this to be the appropriate action in case of an exception
#define EXCEPTION_JUMP abort()
#endif

#define ACC_SIGBIT 0x80000000lu
#define ACC_EXCBIT 0x40000000lu

    // if the aux height is >= this, then something special happened
#define ACC_SPECIAL_THRESHOLD ACC_EXCBIT

#define ACC_DOSAVE (((unsigned)-1)&~ACC_EXCBIT)

#ifndef CEXCEPTIONS
    // the goal is to get a tailcall
#define ACC_CHECK_RETURN_void j2c_signalEvent(); return
#define ACC_CHECK_RETURN_val return (ACCURATE_R_TYPE)j2c_signalEvent()

#define ACC_RETURN_void return
#define ACC_RETURN_val return 0

#define ACC_CONCAT(a,b) a##b
#endif

#if defined(COUNTER_POLLCHECK) || defined(COUNTER_EXCEPTIONS)
#define COUNTER_SPECIALS
#endif

#ifdef COUNTER_SPECIALS
#define ACC_SPECIALS true
#else
#define ACC_SPECIALS false
#endif

#ifdef COUNTER_POLLCHECK
#error "Counter pollcheck is not supported in the present code base."
#define ACC_POLLCHECK true
#else
#define ACC_POLLCHECK false
#endif

#ifdef COUNTER_EXCEPTIONS
#error "Counter exceptions are not supported in the present code base."
#define ACC_EXCEPTIONS true
#else
#define ACC_EXCEPTIONS false
#endif

    // this should work even if the aux stack height is being modified from
    // a signal handler
#ifndef CEXCEPTIONS

#define CHCKRET_HACK(hack) do {			\
        BEGIN_OUTER_BARRIER;                    \
        unsigned tmp=accurate::gAuxStackHeight; \
	if (tmp>=ACC_SPECIAL_THRESHOLD) {       \
            if (ACC_EXCEPTIONS && (tmp&ACC_EXCBIT)) { \
                EXCEPTION_JUMP;                 \
            } else if (!ACC_POLLCHECK || accurate::XCALL_chckret()) { \
                ACC_CONCAT(ACC_RETURN_,hack);   \
	    }					\
	}					\
        END_OUTER_BARRIER;                      \
    } while (0)
#define CHCKRET() CHCKRET_HACK(ACCURATE_R)

#else /*CEXCEPTIONS*/

#define CEXCEPTION_PRECISE_GCE_CHECK do {			\
        BEGIN_OUTER_BARRIER;                    \
	if (accurate::gAuxStackHeight>=ACC_SPECIAL_THRESHOLD) {       \
	    PRECISE_LOG("Detected exception at line %d in GCE_CHECK\n",__LINE__); \
            if (accurate::gAuxStackHeight&ACC_EXCBIT) { \
		PRECISE_LOG("The exception is user, ignoring, at line %d in GCE_CHECK\n",__LINE__); \
            } else { \
		PRECISE_LOG("The exception is NOT user, leaving the method at line %d in GCE_CHECK\n",__LINE__); \
                CEXCEPTION_METHOD_LEAVE;   \
	    }					\
	}					\
        END_OUTER_BARRIER;                      \
    } while (0)


#define CHCKRET() do {			\
        BEGIN_OUTER_BARRIER;                    \
	if (accurate::gAuxStackHeight>=ACC_SPECIAL_THRESHOLD) {       \
	    PRECISE_LOG("Detected exception at line %d in CHCKRET\n",__LINE__); \
            if (accurate::gAuxStackHeight&ACC_EXCBIT) { \
		PRECISE_LOG("The exception is user, jumping to dispatch code at line %d in CHCKRET\n",__LINE__); \
                CEXCEPTION_JUMP;                 \
            } else { \
		PRECISE_LOG("The exception is NOT user, leaving the method at line %d in CHCKRET\n",__LINE__); \
                CEXCEPTION_METHOD_LEAVE;   \
	    }					\
	}					\
        END_OUTER_BARRIER;                      \
    } while (0)

#endif /*CEXCEPTIONS*/

#ifndef CEXCEPTIONS
#define CATCHPREPARE()						\
    unsigned preciseCounterStackHeight = accurate::gStackHeight

#define CATCHRESTORE(n,...)                                             \
    do {                                                                \
        accurate::gStackHeight = preciseCounterStackHeight;             \
        unsigned myAuxStackHeight=accurate::gAuxStackHeight;            \
        if (accurate::gStackHeight < myAuxStackHeight) {                \
            accurate::AuxFrame *FRAME=                                  \
                accurate::XCALL_popFrame(n,myAuxStackHeight);           \
            if (!ACC_SPECIALS || FRAME!=NULL) {                         \
                XNEST(RESTORE,n,##__VA_ARGS__);                         \
            }                                                           \
        }                                                               \
    } while (0)
#endif /*CEXCEPTIONS*/

#ifndef CEXCEPTIONS
#define XCALL_HACK(hack,PRE,POST,call,nptrs,...)			\
({									\
  BEGIN_OUTER_BARRIER;                                                  \
  accurate::gStackHeight++;						\
  BEGIN_INNER_BARRIER;                                                  \
  PRE call;								\
  END_INNER_BARRIER;                                                    \
  accurate::gStackHeight--;						\
  if (accurate::gStackHeight < accurate::gAuxStackHeight) {		\
    accurate::AuxFrame *FRAME=					        \
      accurate::XCALL_getFrame(nptrs);				        \
    if (!ACC_SPECIALS || FRAME!=NULL) {                                 \
      if (accurate::gAuxStackHeight==ACC_DOSAVE) {			\
        XNEST(SAVE,nptrs,##__VA_ARGS__);				\
        ACC_CONCAT(ACC_RETURN_,hack);                                   \
      } else {                                     			\
        XNEST(RESTORE,nptrs,##__VA_ARGS__);				\
      }								        \
    }                                                                   \
    if (ACC_EXCEPTIONS && (accurate::gAuxStackHeight&ACC_EXCBIT)) {     \
      EXCEPTION_JUMP;                                                   \
    }                                                                   \
  }									\
  END_OUTER_BARRIER;							\
  POST;									\
})
#define XCALL(PRE,POST,call,nptrs,...)					\
  XCALL_HACK(ACCURATE_R,PRE,POST,call,nptrs,##__VA_ARGS__)

#else /* CEXCEPTIONS */

#define XCALL(PRE,POST,call,nptrs,...)			\
({									\
  BEGIN_OUTER_BARRIER;                                                  \
  accurate::gStackHeight++;						\
  accurate::counterClearException();					\
  BEGIN_INNER_BARRIER;                                                  \
  PRE call;								\
  END_INNER_BARRIER;                                                    \
  accurate::gStackHeight--;						\
  if (accurate::gStackHeight < accurate::gAuxStackHeight) {		\
    PRECISE_LOG("Detected GC exception in XCALL at line %d\n",__LINE__);   \
    accurate::AuxFrame *FRAME=					        \
      accurate::XCALL_getFrame(nptrs);				        \
    if (FRAME!=NULL) {                                 \
      if (accurate::gAuxStackHeight==ACC_DOSAVE) {			\
   	PRECISE_LOG("Exception is SAVE at line %d\n",__LINE__);   \
        XNEST(SAVE,nptrs,##__VA_ARGS__);				\
   	PRECISE_LOG("Saved, leaving method, at line %d\n",__LINE__);   \
        CEXCEPTION_METHOD_LEAVE;                                        \
      } else {                                     			\
   	PRECISE_LOG("Exception is RESTORE, FRAME is %x, at line %d\n",FRAME, __LINE__);   \
        XNEST(RESTORE,nptrs,##__VA_ARGS__);				\
   	PRECISE_LOG("Restored, at line %d\n",__LINE__);   \
      }								        \
    }                                                                   \
    if (accurate::gAuxStackHeight&ACC_EXCBIT) {     \
	PRECISE_LOG("Detected user exception %x in XCALL, jumping to dispatch code, at line %d\n",cur_exc,__LINE__);   \
      CEXCEPTION_JUMP;                                                   \
    }                                                                   \
  }									\
  END_OUTER_BARRIER;							\
  POST;									\
})

#endif /*CEXCEPTIONS*/

#define CHECK_EXCEPTION() (accurate::gAuxStackHeight&ACC_EXCBIT)

#ifdef CEXCEPTIONS
#define CEXCEPTION_INIT accurate::counterClearException()
#else
#define CEXCEPTION_INIT
#endif /* CEXCEPTIONS */

// for this wrapping style, PACKCALL is
// equivalent to a plain call.
#define PACKCALL(CALL,N,...) CALL(__VA_ARGS__)

#define CALL(E, N, ...) XCALL(,,E,N,##__VA_ARGS__)
#define RCALL(E, N, ...) XCALL(typeof(E) RET = , RET, E, N,##__VA_ARGS__)
#define CALL0(e)     ({ CEXCEPTION_INIT ; e; CHCKRET(); })
#define RCALL0(e) ({ CEXCEPTION_INIT ; typeof(e) RET=e; CHCKRET(); RET; })


#endif /* !PRECISE_THUNK */

// close HERE namespace accurate
};

#endif /* !PRECISE_HH */
