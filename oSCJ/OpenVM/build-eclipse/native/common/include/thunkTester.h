#ifndef THUNKTESTER_HH
#define THUNKTESTER_HH

typedef int AppExn_t;
#define APP_THROW throw
#define CONTEXT_SIZE (2 * 56 * 1024)
#define BAKSIZE CONTEXT_SIZE/2
#define CHECK_EVENTS() (false)
#define THROW(x) throw x

extern void (*engine_signal_event)();
extern void (*engine_events_enabled)();
extern void (*engine_events_disabled)();
extern void j2c_signalEvent();

#endif /* THUNKTESTER_HH */

