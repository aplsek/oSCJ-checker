#ifndef _SIGNALMONITOR_H
#define _SIGNALMONITOR_H

#include "jtypes.h"

jint initSignalMonitor();
void releaseSignalMonitor();
jboolean registerSignal(jint ovmSig);
jboolean registerSignalVector(jint ovmSig[], jint len);
void unregisterSignal(jint ovmSig);
void unregisterSignalVector(jint ovmSig[], jint len);
jboolean getFiredSignals(jint sigs[], jint len);
jboolean canMonitorSignal(jint sig);
#endif
