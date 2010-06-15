/**
 * These functions define the way that the VM will allocate
 * deallocate, and access memory
 * @author James Liang
 * @author Christian Grothoff
 **/

#ifndef MEM_H
#define MEM_H

#include <stdlib.h>

/* do we support memory tracking? */
#define MEMORY_TRACKING 0

/**
 * Allocate a block of memory from the OS.
 * @param size size of the block in bytes
 * @return a reference to the memory
 **/
void * getmem(int size);

void * getheap(int size);

/**
 * Return a block of memory to the OS
 * @param ptr the reference to the block as obtained from getmem
 **/
void freemem(void * ptr);

/**
 * Lets the memory object know about a block of memory that
 * is considered accessible by the interpreted code.
 *
 * Used mostly for the mapped bootimage.  
 * Most other forms of memory accessible by the
 * interpreted code with be retrieved by getmem() which
 * calles registerMem.
 **/ 
void registerMem(void * begin, int length);

/**
 * Unregister a block of memory at a certain beginning position.
 * If memory registration is active and the block was never
 * registered, an error message is printed.
 **/ 
void unregisterMem(void * begin);

/**
 * Verify that pointer is a valid address in memory, 
 * prints a warning if the address is invalid. Can do 
 * nothing if debugging memory is turned off.
 *
 * @param pointer the address to verify
 * @return 0 on error, 1 on ok
 **/
int checkPointer(void * pointer);

#endif
/* end of mem.h */
