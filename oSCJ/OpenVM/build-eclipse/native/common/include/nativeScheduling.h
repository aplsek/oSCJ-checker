/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/include/nativeScheduling.h,v 1.1 2004/02/02 07:03:02 dholmes Exp $
 *
 */
#ifndef _NATIVESCHEDULING_H
#define _NATIVESCHEDULING_H

#include <unistd.h>
#include <sched.h>
#include "jtypes.h"



/* private to the main method this initialises the scheduling policy and
   parameters
*/
void initializeNativeScheduling();

/* return the minimum priority of the native scheduler */
jint getMinPriority();

/* return the maximum priority of the native scheduler */
jint getMaxPriority();

/* set the current process priority to the given value */
jint setPriority(jint prio);

#endif
