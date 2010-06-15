/*
 * eventmanager.c -- C-land code for the event manager.  this here code
 *                   helps facilitate our fast poll check that happens once
 *                   every couple instructions.  it also helps the VM
 *                   block for events whenever it needs to do so.
 *                   if you wish to learn how the Java code works that
 *                   actually makes this code useful, look at
 *                   ovm.services.events.EventManager and
 *                   s3.services.events.EventManagerImpl.
 * by Filip and David.
 */

#ifndef _EVENTMANAGER_H
#define _EVENTMANAGER_H

#include "jtypes.h"
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>


void startProfilingEvents(jint histo_size,
			  jint trace_buf_size,
			  jboolean skip_pa);
void profileEventsBegin(void);
void profileEventsEnd(void);
void profileEventsReset(void);
void profileEventsResetOnSetEnabled(jboolean enabled);

typedef void (*VOID_FPTR)(void);
extern volatile VOID_FPTR signal_event;
extern volatile VOID_FPTR signal_event_from_thread;

/* these might be set by the engine */
extern volatile VOID_FPTR engine_signal_event;
extern volatile VOID_FPTR engine_signal_event_from_thread;
extern volatile VOID_FPTR engine_events_enabled;
extern volatile VOID_FPTR engine_events_disabled;

typedef union {
    struct {
        /* these flags are 'reversed' to ensure that the pollcheck involves
           a compare-to-zero, which can be easier to do on some platforms. */
        volatile jshort notSignaled;
        volatile jshort notEnabled; /* disabled using eventsSetEnabled or using eventsSetInterruptDisabled */
            /* bit 0 ... events disabled 
               bit 1 ... interrupts disabled
              */
    } _;
    volatile jint oneTrueWord;
} eventUnion_t;

#define EU_EVENTS_MASK	1
#define	EU_INTERRUPTS_MASK (1 << 1)

extern eventUnion_t eventUnion;

extern jshort eventCount;

#define CHECK_EVENTS() (!eventUnion.oneTrueWord)
#define CHECK_EVENT_SIGNALED() (!eventUnion._.notSignaled)
#define CHECK_EVENT_ENABLED() (!eventUnion._.notEnabled)
#define SIGNAL_EVENT_FOR_POLL_CHECK() (profileEventsBegin(), eventUnion._.notSignaled=0)
#define RESET_EVENTS() (profileEventsReset(), eventUnion._.notSignaled=1)
#define EVENTS_SET_ENABLED(value) do {\
       profileEventsResetOnSetEnabled(value);\
       if (value) { \
         eventUnion._.notEnabled &= ~EU_EVENTS_MASK; \
       } else { \
         eventUnion._.notEnabled |= EU_EVENTS_MASK; \
       } \
       if (value && engine_events_enabled!=NULL) engine_events_enabled();\
       if (!value && engine_events_disabled!=NULL) engine_events_disabled();\
   } while (0)

#define INTERRUPTS_SET_ENABLED(value) do {\
       if (value) { \
         eventUnion._.notEnabled &= ~EU_INTERRUPTS_MASK; \
       } else { \
         eventUnion._.notEnabled |= EU_INTERRUPTS_MASK; \
         if (CHECK_EVENT_SIGNALED()) SIGNAL_EVENT(); \
       } \
   } while (0)

#define SIGNAL_EVENT() do {\
    if (signal_event!=NULL) {\
        signal_event();\
    }\
    if (engine_signal_event!=NULL) {\
        engine_signal_event();\
    }\
} while (0)

#define SIGNAL_EVENT_FROM_THREAD() do {\
    if (signal_event_from_thread!=NULL) {\
        signal_event_from_thread();\
    }\
    if (engine_signal_event_from_thread!=NULL) {\
        engine_signal_event_from_thread();\
    }\
} while (0)

#ifdef IN_EVENTMANAGER_C
# define EVENTMANAGER_INLINE
#else 
# define EVENTMANAGER_INLINE static inline
#endif

/** enable/disable event processing */
/* Warning: this is hand-inlined in SimpleJIT. If you change this
 * function, turn off the hand inlinig or update SimpleJIT */
EVENTMANAGER_INLINE void eventsSetEnabled(jboolean enabled) {
    EVENTS_SET_ENABLED(enabled);
}

EVENTMANAGER_INLINE void interruptsSetEnabled(jboolean enabled) {
    INTERRUPTS_SET_ENABLED(enabled);
}

/* Warning: this is hand-inlined in SimpleJIT. If you change this
 * function, turn off the hand inlinig or update SimpleJIT */
EVENTMANAGER_INLINE jboolean eventPollcheck() {
    if (CHECK_EVENTS()) {
	profileEventsEnd();
        RESET_EVENTS();
        eventsSetEnabled(0);
        return 1;
    }
    return 0;
}

jboolean eventPollcheckTimer();

void initPollcheckTimer(jshort maxCount,
			jlong period);
void initPollcheckTimerProf(jint trace_buf_size);

/* wait for events to happen.  will not return until there
 * is an event. */
void waitForEvents();

/* functions for setting up the two trivial signal event implementations */
void makeSignalEventSimple();
void makeSignalEventBroken();
void makeSignalEventFromThreadSimple();
void makeSignalEventFromThreadBroken();
void makeSignalEventFromThreadProper();

#endif


