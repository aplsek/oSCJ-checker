/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/waitmanager.c,v 1.3 2004/10/09 21:43:04 pizlofj Exp $
 *
 * waitmanager.c -- native code for asynchronously dealing with waitpid
 * by Filip Pizlo, 2004
 */

#include "waitmanager.h"
#include "eventmanager.h"
#include "signalmanager.h"
#include "util.h"

#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>

#if HAVE_PTHREAD
#include <pthread.h>
#endif

#define SIZE 1024

static volatile jint waitPids[SIZE],
                     deadPids[SIZE],
                     deadStati[SIZE];    /* stati is the plural of status! */

static volatile jint numWaitPids=0, numDeadPids=0;

static jboolean inited=0;

static void sigCHLDHandler(jint sig,siginfo_t *info) {
    jint i;
    for (i=0;i<numWaitPids;++i) {
        for (;;) {
            int status;
            jint res=waitpid(waitPids[i],
                             &status,
                             WNOHANG);
            if (res<0) {
                if (errno==EINTR) {
                    continue;
                } else if (errno==ECHILD) {
                    return;
                }
            } else if (res==0) {
                return;
            }
            deadStati[numDeadPids]=status;
            deadPids[numDeadPids++]=res;
            break;
        }
        waitPids[i--]=waitPids[--numWaitPids];
    }
}

void registerPid(jint pid) {
    sigset_t set,oldSet;
    
    if (!inited) {
        if (registerSignalHandler(SIGCHLD,sigCHLDHandler)<0) {
            fprintf(stderr,"Error registering SIGCHLD handler: %s\n",
                           strerror(errno));
            abort();
        }
        inited=1;
    }
    
    sigemptyset(&set);
    sigaddset(&set,SIGCHLD);
#if HAVE_PTHREAD
    pthread_sigmask(SIG_BLOCK,&set,&oldSet);
#else
    sigprocmask(SIG_BLOCK,&set,&oldSet);
#endif
    
    waitPids[numWaitPids++]=pid;
    
    sigCHLDHandler(0,NULL); /* total hack */
    
#if HAVE_PTHREAD
    pthread_sigmask(SIG_SETMASK,&oldSet,NULL);
#else
    sigprocmask(SIG_SETMASK,&oldSet,NULL);
#endif
}

void unregisterPid(jint pid) {
    sigset_t set,oldSet;
    jint i;

    sigemptyset(&set);
    sigaddset(&set,SIGCHLD);
#if HAVE_PTHREAD
    pthread_sigmask(SIG_BLOCK,&set,&oldSet);
#else
    sigprocmask(SIG_BLOCK,&set,&oldSet);
#endif
    
    for (i=0;i<numWaitPids;++i) {
	if (waitPids[i]==pid) {
	    waitPids[i]=waitPids[--numWaitPids];
	    break;
	}
    }
    
#if HAVE_PTHREAD
    pthread_sigmask(SIG_SETMASK,&oldSet,NULL);
#else
    sigprocmask(SIG_SETMASK,&oldSet,NULL);
#endif
}

jboolean getDeadPid(jint *pid,jint *status) {
    sigset_t set,oldSet;
    jboolean ret=0;
    
    if (!numDeadPids) {
        return 0;
    }
    
    sigemptyset(&set);
    sigaddset(&set,SIGCHLD);
#if HAVE_PTHREAD
    pthread_sigmask(SIG_BLOCK,&set,&oldSet);
#else
    sigprocmask(SIG_BLOCK,&set,&oldSet);
#endif
    
    if (numDeadPids) {
        ret=1;
        --numDeadPids;
        *pid=deadPids[numDeadPids];
        *status=deadStati[numDeadPids];
    }
    
#if HAVE_PTHREAD
    pthread_sigmask(SIG_SETMASK,&oldSet,NULL);
#else
    sigprocmask(SIG_SETMASK,&oldSet,NULL);
#endif

    return ret;
}



