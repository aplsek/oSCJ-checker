/*
 * doselect.h -- implements the doSelect() native call (and its friends)
 * by Filip Pizlo, 2004
 */

#ifndef DOSELECT_H
#define DOSELECT_H

#include "jtypes.h"

jint initSelect();
jint doneSelect();
jint resetPipeBecauseOfFork();

jint doSelect(jboolean block,
              jint *reads,
              jint *writes,
              jint *excepts);

jint addSelectRead(jint fd);
jint delSelectRead(jint fd);

jint addSelectWrite(jint fd);
jint delSelectWrite(jint fd);

jint addSelectExcept(jint fd);
jint delSelectExcept(jint fd);

void dumpSelectBits();

#endif


