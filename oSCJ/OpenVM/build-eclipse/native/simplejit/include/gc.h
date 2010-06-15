#ifndef __GC_H
#define  __GC_H

#include "jtypes.h"

/* GC interface functions */

/*
 * GC support
 */
void *getOVMContextLoc(jint _nc);

/*
 * Conservative GC support.
 */

void *getStackBase(jint _nc) ;
void *getStackTop(jint _nc) ;
#endif
