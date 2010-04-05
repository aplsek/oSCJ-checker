/*
 * doselect.c -- implements the doSelect() native call
 * by Filip Pizlo, 2004
 */

#include "util.h"
#include "doselect.h"
#include "fdutils.h"
#include "eventmanager.h"

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/select.h>
#include <sys/time.h>
#include <errno.h>

static fd_set reads,writes,excepts;
static jint maxReads,maxWrites,maxExcepts;

int selectPipe[2] = { -1, -1 };

static void select_based_signal_event() {
    char c;
    if (selectPipe[1] >= 0) {
        if (write(selectPipe[1],&c,1)<1 &&
            errno != EAGAIN &&
            errno != EWOULDBLOCK &&
            errno != EINTR) {
            fprintf(stderr,"Failed to write to selectPipe[1] = %d (%s)\n",
		    selectPipe[1],strerror(errno));
        }
    }
    SIGNAL_EVENT_FOR_POLL_CHECK();
}

jint initSelect() {
    signal_event_from_thread=signal_event=select_based_signal_event;
    
    FD_ZERO(&reads);
    FD_ZERO(&writes);
    FD_ZERO(&excepts);
    
    maxReads = -1;
    maxWrites = -1;
    maxExcepts = -1;
    
    if (pipe(selectPipe)<0) {
        return -1;
    }
    if (makeNonBlocking(selectPipe[0])<0) {
        return -1;
    }
    if (makeNonBlocking(selectPipe[1])<0) {
        return -1;
    }

    /* fprintf(stderr,"selectPipe: %d, %d\n",selectPipe[0],selectPipe[1]); */
    
    return 0;
}

jint resetPipeBecauseOfFork() {
    /*printf("%d: resetPipeBecauseOfFork\n",getpid());*/
    
    close(selectPipe[0]);
    close(selectPipe[1]);
    if (pipe(selectPipe)<0) {
        return -1;
    }
    if (makeNonBlocking(selectPipe[0])<0) {
        return -1;
    }
    if (makeNonBlocking(selectPipe[1])<0) {
        return -1;
    }
    return 0;
}

jint doneSelect() {
    FD_ZERO(&reads);
    FD_ZERO(&writes);
    FD_ZERO(&excepts);
    
    close(selectPipe[0]);
    close(selectPipe[1]);
    
    selectPipe[0] = -1;
    selectPipe[1] = -1;
    
    return 0;
}

#define DEFINE_SELECT_FUNCS(lcAction,ucAction)\
jint addSelect##ucAction(jint fd) {\
    /* printf("adding for %s: %d\n",#lcAction,fd); */ \
    FD_SET(fd,&lcAction##s);\
    if (fd>max##ucAction##s) {\
        max##ucAction##s=fd;\
    }\
    return 0;\
}\
jint delSelect##ucAction(jint fd) {\
    /* printf("removing for %s: %d\n",#lcAction,fd); */ \
    FD_CLR(fd,&lcAction##s);\
    if (fd>max##ucAction##s) {\
        fprintf(stderr,"%d is greater than the max for fd_set %ss\n",\
                       fd,#lcAction);\
        abort();\
    }\
    if (fd==max##ucAction##s) {\
        while (max##ucAction##s >= 0 &&\
               !FD_ISSET(max##ucAction##s,&lcAction##s)) {\
            max##ucAction##s--;\
        }\
    }\
    return 0;\
}

DEFINE_SELECT_FUNCS(read,Read)
DEFINE_SELECT_FUNCS(write,Write)
DEFINE_SELECT_FUNCS(except,Except)

#define myMax(a,b) ((a)>(b)?(a):(b))

jint doSelect(jboolean block,
              jint *hitReads,
              jint *hitWrites,
              jint *hitExcepts) {
    fd_set myReads,
           myWrites,
           myExcepts;
    
    int res,i,max;
    
    struct timeval tv;
    
    char c;
    
    int save_errno;

    tv.tv_sec  = 0;
    tv.tv_usec = 0;

    memcpy(&myReads,&reads,sizeof(reads));
    memcpy(&myWrites,&writes,sizeof(writes));
    memcpy(&myExcepts,&excepts,sizeof(excepts));
    
    FD_SET(selectPipe[0],&myReads);
    
    max = myMax(myMax(selectPipe[0],maxReads),
                myMax(maxWrites,maxExcepts));
    
    /* printf("maxReads = %d, maxWrites = %d, maxExcepts = %d, max = %d\n",
           maxReads, maxWrites, maxExcepts, max); */
    
    /*for (i=0;
         i<max+1;
         ++i) {
        if (FD_ISSET(i,&myReads) ||
            FD_ISSET(i,&myWrites) ||
            FD_ISSET(i,&myExcepts)) {
            printf("%d: fd = %d:%s%s%s\n",
                   getpid(),
                   i,
                   FD_ISSET(i,&myReads)?" read":"",
                   FD_ISSET(i,&myWrites)?" write":"",
                   FD_ISSET(i,&myExcepts)?" except":"");
        }
    }*/
    
    res=select(max + 1,
               &myReads,
               &myWrites,
               &myExcepts,
               block?NULL:&tv);
    
    /* clear the flag */
    RESET_EVENTS();
    
    save_errno=errno;
    
    /* clear out the pipe.  we do this without checking FD_ISSET(selectPipe[0]) because
     * res could have been negative indicating an error but there may still be data
     * in the pipe anyway.  besides, this call is probably not so expensive that checking
     * FD_ISSET(selectPipe[0]) buys you all that much...  anyway, this may be something
     * to revisit once we've profiled this thing. */
    while (read(selectPipe[0],&c,1)==1) {
        /* fprintf(stderr,"Read from pipe.\n"); */
    }
    
    if (res<0) {
        if (save_errno==EINTR) {
            goto done;
        }
        fprintf(stderr,"Error in doSelect(): %s\n",strerror(save_errno));
        abort();
    }
    
    if (res==0 ||
        (res==1 && FD_ISSET(selectPipe[0],&myReads))) {
        goto done;
    }
    
    for (i=0;i<=maxReads;++i) {
        if (i != selectPipe[0] &&
            FD_ISSET(i,&myReads)) {
            //printf("hit read: %d\n",i);
            *hitReads++=i;
        }
    }
    
    for (i=0;i<=maxWrites;++i) {
        if (FD_ISSET(i,&myWrites)) {
            //printf("hit write: %d\n",i);
            *hitWrites++=i;
        }
    }
    
    for (i=0;i<=maxExcepts;++i) {
        if (FD_ISSET(i,&myExcepts)) {
            //printf("hit except: %d\n",i);
            *hitExcepts++=i;
        }
    }
    
done:
    *hitReads=-1;
    *hitWrites=-1;
    *hitExcepts=-1;
    
    return 0;
}

static void dumpBits(fd_set *set) {
    unsigned i;
    for (i=0;
         i<FD_SETSIZE;
         ++i) {
        fprintf(stderr,"%c",FD_ISSET(i,set)?'1':'0');
    }
}

void dumpSelectBits() {
    jboolean e=fSBE(stderr,1);
    
    fprintf(stderr,"C-land Select Bits:\n");
    fprintf(stderr,"selectPipe = [%d, %d]\n",
                   selectPipe[0], selectPipe[1]);
    fprintf(stderr,"maxReads = %d, maxWrites = %d, maxExcepts = %d\n",
                   maxReads, maxWrites,maxExcepts);
    fprintf(stderr,"reads: ");
    dumpBits(&reads);
    fprintf(stderr,"\nwrites: ");
    dumpBits(&writes);
    fprintf(stderr,"\nexcepts: ");
    dumpBits(&excepts);
    fprintf(stderr,"\n");
    
    fSBE(stderr,e);
}


