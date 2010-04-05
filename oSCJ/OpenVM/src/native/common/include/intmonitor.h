#ifndef _INTMONITOR_H
#define _INTMONITOR_H

#include "jtypes.h"

void initInterruptMonitor();
void shutdownInterruptMonitor(void);

jint getPendingInterrupts( jint pendingInterrupts[], jint arrayLength );
jint getPendingInterrupt(void);

void interruptServingStarted( jint interruptIndex );
void interruptServed( jint interruptIndex );

void enableLocalInterrupts(void);
void disableLocalInterrupts(void);
void enableInterrupt( jint interruptIndex );
void disableInterrupt( jint interruptIndex );

jboolean startMonitoringInterrupt( jint interruptIndex );
jboolean stopMonitoringInterrupt( jint interruptIndex );
jboolean isMonitoredInterrupt( jint interruptIndex );

jint getMaxInterruptIndex( void );

void resignalInterrupt( void );

#endif
