/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/include/waitmanager.h,v 1.3 2004/10/09 21:43:04 pizlofj Exp $
 *
 * waitmanager.h -- native code for asynchronously dealing with waitpid
 * by Filip Pizlo, 2004
 */

#ifndef WAITMANAGER_H
#define WAITMANAGER_H

#include "jtypes.h"

void registerPid(jint pid);
void unregisterPid(jint pid);
jboolean getDeadPid(jint *pid,jint *status);

#endif

