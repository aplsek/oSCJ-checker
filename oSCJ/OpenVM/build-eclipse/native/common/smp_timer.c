#include "config.h"

#include "util.h"
#include "timer.h"
#include "eventmanager.h"
#include "signalmanager.h"
#include "fdutils.h"
#include "native_helpers.h"
#include <signal.h>
#include <time.h>
#include <sys/time.h>
#include <stdlib.h>
#include <errno.h>
#include <pthread.h>

#if HAVE_PTHREAD

static jlong period;

static void *timerThread(void *arg) {
    jlong last=getCurrentTimeR();
    for (;;) {
	jlong cur=getCurrentTimeR();
	if ((cur-last)>=period) {
	    SIGNAL_EVENT_FROM_THREAD();
	    last=cur;
	}
    }
}

jint initSMPTimer(jlong period_) {
    pthread_t t;
    period=period_;
    pthread_create(&t,NULL,timerThread,NULL);
    return 1;
}

#else

jint initSMPTimer(jlong period) { return 0; }

#endif
