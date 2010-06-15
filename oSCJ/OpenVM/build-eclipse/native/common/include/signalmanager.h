/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/include/signalmanager.h,v 1.10 2006/10/18 08:52:46 cunei Exp $
 * signalmanager.h -- bunch of functions to centralise OVM's signal management
 * by Filip Pizlo, 2003
 */

#ifndef _SIGNALMANAGER_H
#define _SIGNALMANAGER_H

#include <signal.h>
#include <sys/types.h>
#include <unistd.h>
#include "autodefs.h"
#include "jtypes.h"

/* Zero-terminated array of signals reserved by execution engine.
 * The execution engine must actually define this.  
 * The preferred way of querying this array from outside of the
 * execution engine is to use the signalReserved() function,
 * which is declared below.
 *
 * Some signals are automatically defined as reserved by the signal
 * manager. These are listed in reservedSignals. Such signals include
 * those for which a handler can not be installed (SIGKILL, SIGSTOP)
 * and those for which an application level handler is infeasible -
 * such as SIGSEGV and SIGBUS.
 *
 * This little array gives the execution engine a tremendous
 * amount of power in that it can prevent the rest of the VM
 * from receiving any arbitrary signal.  This feature should
 * be used sparringly, however. below is a list of uses that
 * are most probably appropriate:
 *
 * -> SIGABRT - the unrecoverable signals.  Even if you managed to register 
 *    a signal handler for one of these, you couldn't really do much
 *    in it, because the OVM would exit shortly thereafter anyway.
 *
 * -> some special signal that gets reserved by the host
 *    system.  LinuxThreads, for example, uses signals to
 *    unblock threads that are waiting on a mutex.  If the
 *    execution engine has knowledge that some signal is
 *    reserved by the system in such a way, it should place
 *    it in this array.
 */
extern const int signalsReservedByExecEngine[];

/*
 * The zero-terminated list of always reserved signals defined by the 
 * Signal Manager
*/
extern const int reservedSignals[];

/* our idea of a signal handling function. */
typedef void (*ovmSignalHandler_t)(jint sig,
				   siginfo_t *info,
				   void *ctx);

/* returns true if the signal is reserved by the execution
 * engine, or is a signal for which installing a signal handler
 * is either not possible, or not practical and so cannot be used 
 * by other code.  
 * Any signal for which this function returns true cannot be used
 * with registerSignalHandler(); any such attempt will
 * result in EINVAL. */
jboolean signalReserved(jint sig);

/* register a signal handler.  returns 0 on success and -1
 * on error. */
jint registerSignalHandler(jint sig,ovmSignalHandler_t newHandler);

/* remove a signal handler from all signals.  returns the
 * number of times the handler was removed. */
jint removeSignalHandler(ovmSignalHandler_t handler);

/* remove a signal handler from a signal.
 * Returns the number of time the handler was removed
 */
jint unregisterSignalHandler(jint sig,ovmSignalHandler_t handler);

/* initialize the signal handling infrastructure. returns 0
 * on success and -1 on error. this function should be called
 * from the execution engine's startup code. */
jint initSignalHandling();

/* returns a constant string name for the given signal,
 * such as "SIGHUP" for SIGHUP, or "UNKNOWN" if we don't
 * know about that particular signal. */
inline const char *getSignalName(jint sig);

#endif

