/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/blockingio.c,v 1.22 2003/09/04 04:10:35 dholmes Exp $
 * blockingio.c -- implementation of checkIfBlock
 * by Filip Pizlo, 2003
 */

#include "blockingio.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/time.h>
#include <unistd.h>
#include <errno.h>
#include <assert.h>
#include <string.h>

#ifndef BIO_DEBUG
#define BIO_DEBUG 0
#endif 

#if BIO_DEBUG == 1
#define d(exp) (exp)
#else
#define d(exp) 
#endif

/**
 * Test if an operation on a file_descriptor would block
 * @param file_descriptor the file descriptor to check
 * @param mode which operations should be tested for 
 * @return the mask of operations that would block. If zero then no
 * operations would block. If an error occurs then zero is returned as the
 * operation would not block, but would report an error
 *  (only bits of operations that were specified in mode may still be set)
 */
jint checkIfBlock(jint file_descriptor, 
		  jint mode) {
    fd_set reads;
    fd_set writes;
    fd_set excepts;
    jint res,ret;
    struct timeval tv;

    FD_ZERO(&reads);
    FD_ZERO(&writes);
    FD_ZERO(&excepts);
	
    if (mode&BLOCKINGIO_READ) {
        FD_SET(file_descriptor,&reads);
    }
	
    if (mode&BLOCKINGIO_WRITE) {
        FD_SET(file_descriptor,&writes);
    }
	
    if (mode&BLOCKINGIO_EXCEPT) {
        FD_SET(file_descriptor,&excepts);
    }
	
    tv.tv_sec  = 0;
    tv.tv_usec = 0;

    while(1) {
        res = select(file_descriptor+1,&reads,&writes,&excepts,&tv);

        /* there are three possible error cases:
           - EBADF - in which case we return zero as the op will also
                     return EBADF
           - EINTR  - in which case we retry the operation (seems unlikely
                      in this non-blocking case)
           - EINVAL - in which case we've got a bug
        */
        if (res<0) {
            d(fprintf(stderr,
                      "checkIfBlock(%d,%d) returning 0 due to error: %s\n",
                      file_descriptor,mode, strerror(errno)));
            switch(errno) {
            case EBADF: 
                return 0;
            case EINTR: 
                break;
            case EINVAL:
                fprintf(stderr, "EINVAL from select in checkIfBlock\n");
                assert(0);
            default: 
                fprintf(stderr, 
                        "Unexpected error (%s) from select in checkIfBlock\n", 
                        strerror(errno));
                abort();
            }
        }
        else break; /* no error so get out of loop */
    }
	
    if (res==0) {
        d(fprintf(stderr, "checkIfBlock(%d,%d) returning %d.\n",
                  file_descriptor,mode,mode));
        return mode;
    }
	
    ret=mode;
	
    if (FD_ISSET(file_descriptor,&reads)) {
        ret&=~BLOCKINGIO_READ;
    }
	
    if (FD_ISSET(file_descriptor,&writes)) {
        ret&=~BLOCKINGIO_WRITE;
    }
	
    if (FD_ISSET(file_descriptor,&excepts)) {
        ret&=~BLOCKINGIO_EXCEPT;
    }
    
    d(fprintf(stderr, "checkIfBlock(%d,%d) returning %d.\n",
    	     file_descriptor,mode,ret));
    
    return ret;
}
