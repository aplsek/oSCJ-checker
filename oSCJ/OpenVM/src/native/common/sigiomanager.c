/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/sigiomanager.c,v 1.7 2006/10/09 10:28:48 cunei Exp $
 * sigiomanager.c -- implementation of SIGIO code
 * by Filip Pizlo, 2003
 *
 *-------------------------
 * O_ASYNC is not available under Solaris. An alternate mechanism is used instead,
 * which relies on SIGPOLL and ioctl rather than SIGIO and O_ASYNC.
 * --Toni
 */

#include "sigiomanager.h"
#include "eventmanager.h"
#include "signalmanager.h"

#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>

#if defined(SOLARIS_BUILD)
# include <stropts.h>
#endif

static volatile sig_atomic_t triggered=0;

static void sigIOHandler(jint sig,siginfo_t *info,void *ctx) {
    //write(1,"SIGIO!\n",strlen("SIGIO!\n"));
    triggered=1;
    SIGNAL_EVENT();
}

static int fcntlSetFlag(int fd,int flag) {
	int flags=fcntl(fd,F_GETFL,0);
	if (flags<0) {
		return -1;
	}
	return fcntl(fd,F_SETFL,flags|flag);
}

static int fcntlResetFlag(int fd,int flag) {
	int flags=fcntl(fd,F_GETFL,0);
	if (flags<0) {
		return -1;
	}
	return fcntl(fd,F_SETFL,flags&(~flag));
}

jint enableIOSignalManager() {
#if defined(SOLARIS_BUILD)
    if (registerSignalHandler(SIGPOLL,sigIOHandler)<0) {
        return errno;
    }
#else
    if (registerSignalHandler(SIGIO,sigIOHandler)<0) {
        return errno;
    }
#endif    
    if (registerSignalHandler(SIGURG,sigIOHandler)<0) {
        removeSignalHandler(sigIOHandler);
        return errno;
    }
    
    return 0;
}

jint disableIOSignalManager() {
    removeSignalHandler(sigIOHandler);
    return 0;
}

jint enableIOSignal(jint fd) {

#if defined(SOLARIS_BUILD)
    if (ioctl(fd, I_SETSIG, S_OUTPUT | S_INPUT | S_HIPRI)<0) {
        printf("enableIOSignal: error setting I_SETSIG on %d: %s\n",
               fd, strerror(errno));
        return errno;
    }
#elif defined(O_ASYNC)
    if (fcntlSetFlag(fd, O_ASYNC)<0) {
        printf("enableIOSignal: error setting O_ASYNC flag on %d: %s\n",
               fd, strerror(errno));
        return errno;
    }
/*     printf("enableIOSignal: getpid() == %d\n", getpid()); */
    if (fcntl(fd, F_SETOWN, getpid())<0) {
        printf("enableIOSignal: error fcntl(%d, F_SETOWN, %d): %s\n",fd, getpid(), strerror(errno));
        return errno;
    }
    return 0;
#else
    printf("WARNING: O_ASYNC not supported.\n");
    return EINVAL;
#endif
}

jint disableIOSignal(jint fd) {

#if defined(SOLARIS_BUILD)
    int current;
    int res=ioctl(fd, I_GETSIG, &current);
    int er=errno;
// for unidentified reasons, the I_GETSIG above will often complain saying EPIPE.
// However, calling it will allow the I_SETSIG below to operate properly.
// FIXME: find out what's really going on

    if (ioctl(fd, I_SETSIG, 0)<0) {
        printf("disableIOSignal: error clearing I_SETSIG on %d: %s - current mask is %d\n",
               fd, strerror(errno),current);
        return errno;
    }
#elif defined(O_ASYNC)
    if (fcntlResetFlag(fd, O_ASYNC)<0) {
        return errno;
    }
    return 0;
#else
    printf("WARNING: O_ASYNC not supported.\n");
    return EINVAL;
#endif
}

jint getKnownSignaledFDs(void *fds) {
    if (triggered) {
        triggered=0;
        //printf("getKnownSignaledFDs: -1\n");
        return -1;
    }
    //printf("getKnownSignaledFDs: 0\n");
    return 0;
    //return -1;
}
