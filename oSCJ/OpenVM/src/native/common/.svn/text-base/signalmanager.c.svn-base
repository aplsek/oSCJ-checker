
/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/signalmanager.c,v 1.30 2006/11/21 15:20:56 baker29 Exp $
 * signalmanager.c -- implementation of bunch of function that centralise
 *                    OVM's signal management.
 * by Filip Pizlo, David Holmes, 2003
 */

#include "signalmanager.h"
#include "signalmapper.h"
#include "util.h"
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>

#if HAVE_PTHREAD
#include <pthread.h>
#endif
extern volatile jboolean eventsEnabled;
/*
 * The signals that are always reserved and which cannot have a handler
 * registered for them. This list is zero-terminated.
 *
 */
const int reservedSignals[] = {
    /* These can't be caught or ignored */
    SIGKILL, 
    SIGSTOP, 
    /* These have undefined behaviour if a signal handler for them
       returns normally - as per IEEE 1003.1 2003
       Plus we use SIGSEGV internally for null pointer traps in j2c
       (not anymore in C version !)
    */
    SIGSEGV,
    SIGBUS,
    SIGILL,
    SIGFPE,  /* trying to play with this one wreaks havoc on Mac OSX */

    /* if we install a handler for these job-control signals then we lose
       the default behaviour
    */
    SIGTSTP,
    SIGCONT,

    /* Is there a legitimate purpose for defining a SIGTRAP handler?
       AFAIK, it is only used to stop a child process so that a
       debugger can do something.
    */
    SIGTRAP,

    0  /* zero-terminated list */
};

/* holds the set of handlers for a given signal, together with the number
   of handlers and the old sigaction for that signal so that we can chain
   handlers installed outside of the signalmanager.
*/
struct SignalHandlers {
    ovmSignalHandler_t *handlers;
    jint numHandlers;
    
    struct sigaction oldsa;
};

/* The array of SignalHandlers. The array is indexed by the actual signal
   number and so may have empty entries.
*/
static struct SignalHandlers *signals=NULL;

/**
 * This is the main signal handler that gets installed. It's job is to
 * invoke the registered signal handlers for the given signal and then to
 * chain to an external signal handler that already existed.
 */
static void realSignalHandler(int sig, siginfo_t* info, void* context) {
    jint i;
    void (*plainHandler)(int);
    void (*sigInfoHandler)(int, siginfo_t*, void*);

    if (0) fprintf(stderr, "Entered realSignalHandler for signal %s\nEvent are currently %sabled\n", 
                   getSignalName(sig), (eventsEnabled? "en" : "dis") );
    if (sig < 0 || sig >= signalMapSize) {
        fprintf(stderr,"Error: signal %d is outside the range [1, %d)\n",
                sig, signalMapSize);
        return;
    }

    if (0) fprintf(stderr, "There are %d handlers to process\n", 
                   signals[sig].numHandlers);
    for (i = 0; i < signals[sig].numHandlers; ++i) {
        if (signals[sig].handlers == NULL) {
            fprintf(stderr, "Null handler list when numHandlers > 0\n");
            continue;
        }
        if (0) fprintf(stderr, "About to invoke handler[%d]\n", i);
        signals[sig].handlers[i](sig, info, context);
        if (0) fprintf(stderr, "Returned from handler[%d]\n", i);
    }
    
    /* now do chain */
    if (signals[sig].oldsa.sa_flags&SA_SIGINFO) {
        sigInfoHandler = signals[sig].oldsa.sa_sigaction;
        if (0) fprintf(stderr, "Chaining to SIGINFO style handler\n");
        sigInfoHandler(sig, info, context);
        if (0) fprintf(stderr, "Returned from chained handler\n");
    }
    else {
        plainHandler = signals[sig].oldsa.sa_handler;
        if (plainHandler == SIG_IGN || plainHandler == SIG_DFL) {
            if (0) fprintf(stderr, "Ignoring chain to SIG_IGN/DFL\n");
        } else {
            if (0) fprintf(stderr, "Chaining to plain handler\n");
            plainHandler(sig);
            if (0) fprintf(stderr, "Returned from plain handler\n");
        }
    }
    if (0) fprintf(stderr, "Returning from realSignalHandler\n");
}

/** Returns true if the given signal is reserved for use by the execution
    engine.
*/
jboolean signalReserved(jint sig) {
    const jint *cur;
    int i;
    for (i=0, cur = reservedSignals; *cur; ++cur, i++) {
        if (*cur == sig) {
            if (0) printf("%s reserved at %d\n", getSignalName(sig), i);
            return 1;
        }
    }

    for (cur = signalsReservedByExecEngine; *cur; ++cur) {
        if (*cur == sig) {
            if (0) printf("%s reserved by engine\n", getSignalName(sig));
            return 1;
        }
    }
    return 0;
}

/* unprotected versions of these functions */
static jint registerSignalHandlerImpl(jint sig,ovmSignalHandler_t newHandler);
static jint removeSignalHandlerImpl(ovmSignalHandler_t handler);
static jint unregisterSignalHandlerImpl(jint sig,ovmSignalHandler_t handler);

/** Adds the given signal handler to the list of handlers for the given
    signal.
    @return 0 on success and -1 on failure and errno is set. Errno is set
    to EINVAL is the requested signal is reserved by the execution engine.
*/
static jint registerSignalHandlerImpl(jint sig,ovmSignalHandler_t newHandler) {
    /* can't register for special signals */
    if (signalReserved(sig)) {
        errno = EINVAL;
        return -1;
    }

    
    /* We never de-register our actual signal handler once it is
       installed. So it is possible for a signal to have had its 
       handlers removed but for it to still actually be registered with us.  
       in such a case numHandlers will be 0, but handlers will be
       non-NULL.  handlers is only NULL if no handlers were ever
       added to it
    */
    if (signals[sig].handlers == NULL) {
        /* must now actually register the handler. */

        struct sigaction sa;

        /* fill in our sigaction struct with the current settings so we
           preserve anything we don't explicitly override - eg SA_ONSTACK.
        */
        if (sigaction(sig, 0, &sa) < 0) {
            perror("Query of existing sigaction failed");
            /* just continue at this stage. If there is another failure
               it will be reported to the caller.
            */
        }        

        sa.sa_sigaction = realSignalHandler;

        /* Allow all other signals whilst we're handling this one.
           In particular allow SIGINT, SIGQUIT, SIGTSTP, SIGCONT and other
           control signals. Also we must allow all the synchronous hardware
           related signals.
           The current signal will be masked by the signal mechanism
        */
        sigemptyset(&sa.sa_mask);

#ifdef RTEMS_BUILD
        sa.sa_flags = SA_SIGINFO; 
#else
        sa.sa_flags = SA_RESTART|SA_SIGINFO; 
#endif
        
        if (sigaction(sig, &sa, &signals[sig].oldsa) < 0) {
            return -1;
        }

        /* sanity check */
        /* Why do we do this check? So what if they want to use SIGINFO? -DH*/
        if (signals[sig].oldsa.sa_flags & SA_SIGINFO) {
            fprintf(stderr, "WARNING: pre-installed %s handler wants to use SIGINFO\n", getSignalName(sig));
        }
    }

    /* if no current handlers, and never allocated space then 
       allocate space for one handler
    */
    if (signals[sig].handlers == NULL) {
        signals[sig].handlers = malloc(sizeof(ovmSignalHandler_t));
        if (signals[sig].handlers == NULL) {
            return -1;
        }
    } else {
        /* allocate space for all existing handlers plus the new one
           copying over all the existing handler info. This will grow
           the array if it was full, or else shrink it if it has surplus
           room.
        */
        ovmSignalHandler_t* newHandlers = 
            realloc(signals[sig].handlers,
                    sizeof(ovmSignalHandler_t)*(signals[sig].numHandlers+1));
        if (newHandlers == NULL) {
            return -1;
        }
        
        signals[sig].handlers = newHandlers;
    }

    signals[sig].handlers[signals[sig].numHandlers++] = newHandler;
    
    return 0;
}

/**
 * Removes the given signal handler from all signals, if it happens to be
 * registered with them. This is just a convenience function.
 */
static jint removeSignalHandlerImpl(ovmSignalHandler_t handler) {
    jint ret = 0;
    jint i;
    for (i = 0; i < signalMapSize; ++i) {
        ret += unregisterSignalHandlerImpl(i, handler);
    }
    return ret;
}

/**
 * Unregisters the given handler from the given signal.
 * @return the number of times the handler was removed.
 */
static jint unregisterSignalHandlerImpl(jint sig, ovmSignalHandler_t handler) {
    jint ret = 0;
    jint i;
    
    if (sig < 0 || sig >= signalMapSize ) { /* out of range */
        return 0;
    }
    
    for (i = 0; i < signals[sig].numHandlers; ++i) {
        if (signals[sig].handlers[i] != handler) {
            continue;
        }
        
        /* we wish to remove the element at i, so we move the
         * last element in the list to the element at i,
         * make sure we decrement the list size, and then decrement
         * i so that we do not miss this new element that is
         * now at i the next time we loop around. */
        signals[sig].handlers[i--]=
            signals[sig].handlers[--signals[sig].numHandlers];
        ++ret;
    }
    
    return ret;
}

#if HAVE_PTHREAD
#define doSigmask pthread_sigmask
#else
#define doSigmask sigprocmask
#endif

/**
 * Takes a function defined with an Impl suffix and defines
 * a new function, one with a name that lacks the Impl suffix,
 * that first disables signals using either pthread_sigmask
 * or sigprocmask (whichever is more appropriate), then calls
 * the original function that has the Impl suffix, and then
 * re-enabled signals to whatever they were before.
 *
 * @param type the type that the function we are wrapping
 *             returns.  We end up returning the same type.
 *             The type may not be void.
 * @param name the name of the function, minus the Impl
 *             suffix.  For example, if name is Blah, then
 *             this macro expects that there is already a
 *             function called BlahImpl and then defines
 *             a new function called Blah.
 * @param pts The paranthesized C++-style parameter declaration.
 *            For example, if Blah takes a single int called
 *            x, pts should be (int x).
 * @param pns The paranthesized parameter list, without types.
 *            If Blah takes a simple int called x, pns would
 *            just be (x).
 */
#define SIGNAL_PROTECT(type,name,pts,pns)                   \
type name pts {                                             \
    sigset_t set,oldSet;                                    \
    type result;                                            \
    sigfillset(&set);                                       \
    EXIT_ERRNO(doSigmask(SIG_BLOCK,&set,&oldSet),           \
               "doSigmask failed");                         \
    result=name##Impl pns;                                  \
    EXIT_ERRNO(doSigmask(SIG_SETMASK,&oldSet,NULL),         \
               "doSigmask failed");                         \
    return result;                                          \
}

SIGNAL_PROTECT(jint,
               registerSignalHandler,
               (jint sig,ovmSignalHandler_t newHandler),
               (sig,newHandler))

SIGNAL_PROTECT(jint,
               removeSignalHandler,
               (ovmSignalHandler_t newHandler),
               (newHandler))

SIGNAL_PROTECT(jint,
               unregisterSignalHandler,
               (jint sig,ovmSignalHandler_t newHandler),
               (sig,newHandler))


/**
 * Initialises the signal manager. This must be called as part of the early
 * bootstrapping of the VM native code.
 */
jint initSignalHandling() {

    struct sigaction sa;

    /* initialise the signal map */
    if (initSignalMaps() != 0)
        return -1;

    /* now create the signals array */
    if ( (signals = calloc(signalMapSize,sizeof(struct SignalHandlers))) == NULL)
        return -1;
    
    /* we never, ever want to find ourselves getting killed by a SIGPIPE.
     * we instead wish to handle the EPIPE error returned from the
     * offending syscall.  so, we make a point of ignoring SIGPIPE. */
    
    sa.sa_handler=SIG_IGN;
    sigfillset(&sa.sa_mask);
#ifdef RTEMS_BUILD
    sa.sa_flags=0;
#else
    sa.sa_flags=SA_RESTART/*|SA_ONSTACK*/;
#endif
    return sigaction(SIGPIPE,&sa,NULL);
}

inline const char *getSignalName(jint sig) {
    return cSig2ovmSig[sig].sigName;
}


