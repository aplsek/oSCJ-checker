/*
 * blockingio.h -- code to query the blocking state of an fd.
 * by Filip Pizlo, 2003
 */

#ifndef BLOCKINGIO_H
#define BLOCKINGIO_H

#include "jtypes.h"

/*
 * Stuff for native interface to kernel
 */

enum BlockingIOEnum {
	BLOCKINGIO_READ   = 1<<0,
	BLOCKINGIO_WRITE  = 1<<1,
	BLOCKINGIO_EXCEPT = 1<<2
};

/* returns a mask of what the fd would block on. */
jint checkIfBlock(jint file_descriptor,jint mode);

#endif



