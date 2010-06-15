#ifndef _OVMTIMER_H
#define _OVMTIMER_H
#include <signal.h>
#include "jtypes.h"

extern void configureTimer(jlong period,jlong multiplier);
extern void stopTimer();
extern jint realTimerPoll();
extern jlong getPeriod();
extern volatile void* getInterruptCountAddress();
void generateTimerInterrupt();
void printTimerStats();
#endif
