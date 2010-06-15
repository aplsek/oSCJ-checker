#include "util.h"
#include "signalmonitor.h"
#include "signalmapper.h"
#include "signalmanager.h"
#include "eventmanager.h"
#include "atomicops.h"
#include <stdlib.h>
#include <errno.h>
#include <string.h>

/**
 The signalmonitor maintains an array of counters for each signal of interest 
 that is defined in the system, and when requested starts monitoring 
 occurrences of those signals and reporting them to the event manager.
 These signals are in native signal order.
*/
static jint* signals;

/**
 The set of registered signals. When the count reaches zero we really
 deregister it.
 These signals are in OVM signal order.
*/
static jint* registered;


/**
 Single flag that tells us if our signal handler fired. This prevents us from
 checking every signal on every event when the event manager invokes our
 Java-level event processor.
*/
static int signalled = 0;

/**
 * This is the common signal handler that gets installed. It's job is to
 * increment the count, atomically, for the given signal and indicate that
 * an event has occurred.
 */
static void signalCounter(int sig, siginfo_t* info) {
/*     printf("Signal counter called for %d\n", sig); */
    /* this assert could fail given this is a signal handler, but we need to
       check for this anyway
    */
    ASSERT(sig < signalMapSize, "out of range signal");

    atomic_get_and_inc_int(&signals[sig]);
    atomic_set_int(&signalled, 1);
    SIGNAL_EVENT();
}


/** initialize by allocating the data structures
 * @return 0 on success and -1 on failure
 */
jint initSignalMonitor() {

    signals = calloc(signalMapSize, sizeof(jint));
    if (signals == NULL)
        return -1;

    registered = calloc(ovmSignalCount, sizeof(jint));
    if (registered == NULL) {
        free(signals);
        return -1;
    }

    return 0;
}


/** Remove our signal handler from all signals and free the data structures
 */
void releaseSignalMonitor() {
    removeSignalHandler(signalCounter);
    free(signals);
    free(registered);
}


/**
 * Registers the signal handler for the given signal, if not already
 * registered
 * @param ovmSig the OVM signal number
 * @return true if the signal was registered and false if it couldn't
 * be registered
 */
jboolean registerSignal(jint ovmSig) {
    int cSig = ovmSig2cSig[ovmSig];
/*      printf("Registering ovmSig %d which maps to %d\n",  */
/*               ovmSig, cSig);  */
    if (cSig < 0) {
        return J_FALSE;
    }
    if (registered[ovmSig]++ == 0) {
        if (registerSignalHandler(cSig, signalCounter) != 0) {
/*               printf("Error registering signal handler for %d: %s\n",   */
/*                        ovmSig, strerror(errno));   */
            registered[ovmSig]--;
            return J_FALSE;
        }
        else {
	  /* printf("Registered handler for %d\n", ovmSig);  */
        }
    }
    else {
      /*      printf("Wasn't a zero count\n"); */
    }
    return J_TRUE;
}

/**
 * Registers the signal handler for the given set of signals, if not already
 * registered.
 * @param ovmSigs the OVM signal number vector
 * @return TRUE if all signals were registered and FALSE if any signal could
 * not be registered. The ovmSig array is set to all zero entries except for
 * the errant signal entry which is set to one.
 */
jboolean registerSignalVector(jint ovmSig[], jint len) {
    int i, j;
    for (i = 0; i < len; i++) {
        if (ovmSig[i] == 1) {
            if (!registerSignal(i)) {
                /* we failed to register so unregister all the
                   signals we did manage to register
                */
                for ( j = i-1; j >= 0; j--) {
                    if (ovmSig[j] == 1) {
                        unregisterSignal(j);
                    }
                }
                /* zero the array except for the first errant signal */
                memset(ovmSig,'\0', len*sizeof(jint));
                ovmSig[i] = 1;
                return J_FALSE;
            }
        }
    }
    return J_TRUE;
}

/**
 * Unregisters the signal handler for the given signal.
 * @param ovmSig the OVM signal number
 */
void unregisterSignal(jint ovmSig) {
    int cSig = ovmSig2cSig[ovmSig];
/*     printf("Unregistering ovmSig %d which maps to %d\n",  */
/*            ovmSig, cSig);  */
    if (--registered[ovmSig] == 0) {
/*         printf("Actually removing handler for %d\n", cSig); */
        unregisterSignalHandler(cSig, signalCounter);
    }
}


/**
 * Unregisters the signal handler for the given signal set.
 * @param ovmSig the OVM signal number vector
 */
void unregisterSignalVector(jint ovmSig[], jint len) {
    int i;
    for (i= 0; i < len; i++) {
        if (ovmSig[i] == 1) {
            unregisterSignal(i);
        }
    }
}

/**
 * This is the event processor downcall. It's job is to clear all counts
 * atomically, and report to the VM which signals fired and how
 * many times
 * @param sigs an array to hold the count for each fired signal.
 * Note that the array expects the counts in OVM signal order NOT C signal
 * value order, so we convert.
 */
jboolean getFiredSignals(jint sigs[], jint len) {
    int i;
    if (atomic_get_and_set_int(&signalled, 0) == 1) {
        /* we atomically read and clear the signal count. By the time this
           information gets back to the VM the signal count may be non-zero
           again but we'll catch that one next time through.
        */
        for (i = 0; i < signalMapSize; i++) {
            int ovmSigNum = cSig2ovmSig[i].ovmSigNum;
            if (ovmSigNum != -1) {
                jint count = atomic_get_and_set_int(&signals[i], 0);
/*                 if (count > 0) {  */
/*                     printf("Reporting count of %d for ovmSigNum %d\n",   */
/*                            count, ovmSigNum);   */
/*                 }  */
                ASSERT(ovmSigNum < len, "ovmSigNum out of range");
                sigs[ovmSigNum] = count;
            }
        }
        return J_TRUE;
    }
    else {
/*         printf("No signals have fired\n"); */
        return J_FALSE;
    }
}


/** check if the given OVM signal is one we can wait upon on this platform.
   That means it exists and is not reserved.
   @param sig the OVM signal value (already range checked)
   @return true if you can wait on it and false otherwise
*/
jboolean canMonitorSignal(int sig) {
    int cSig = ovmSig2cSig[sig];
    return (cSig != -1 && !signalReserved(cSig));
}
