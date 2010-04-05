/**
 * These functions define the way that the VM will allocate
 * deallocate, and access memory
 * @author James Liang
 * @author Christian Grothoff
 **/

#include <stdlib.h>
#include <stdio.h>

#include "mem.h"

#include "autodefs.h"

#if MEMORY_TRACKING
struct memtracking_block {
  void * start;
  void * end;
  struct memtracking_block * next;
};

static struct memtracking_block * first = NULL;
#endif

/**
 * Allocate a block of memory from the OS.
 * @param size size of the block in bytes
 * @return a reference to the memory
 **/
void * getmem(int size) {
  void * retVal = malloc(size);
  if (retVal == NULL) {
      fprintf(stderr, "ERROR: failed to allocate memory");
      fprintf(stderr," when attempted to allocate %d bytes\n", size);
      
      for(;;) {
        size-=1024;
        if (size<=0) break;
        retVal = malloc(size);
        if (retVal != NULL) {
          free(retVal);
          fprintf(stderr, "Maximum allocation possible (stepping down by 1kB) was %d\n", size);
          break;
        }
      }
      
      abort();
  }

/*
  fprintf(stderr,"Allocated %d bytes by malloc\n", size);
  
  size=8*1024*1024;
  void *dummy;
      for(;;) {
        size-=1024;
        if (size<=0) break;
         dummy = malloc(size);
        if (dummy != NULL) {
          free(dummy);
          fprintf(stderr, "next maximum allocation possible (stepping down by 1kB) was %d\n", size);
          break;
        }
      }
*/  
  registerMem(retVal,size);
  return retVal;
}

void *getheap(int size) {

  return getmem(size+getpagesize());
}


/**
 * Return a block of memory to the OS
 * @param ptr the reference to the block as obtained from getmem
 **/
void freemem(void * ptr) {
  unregisterMem(ptr);
  free(ptr);
}

/**
 * Lets the memory object know about a block of memory that
 * is considered accessible by the interpreted code.
 *
 * Used mostly for the mapped bootimage.  
 * Most other forms of memory accessible by the
 * interpreted code with be retrieved by getmem() which
 * calles registerMem.
 **/ 
void registerMem(void * begin, int length) {
#if MEMORY_TRACKING
  struct memtracking_block * block = (struct memtracking_block *)
    malloc(sizeof(struct memtracking_block));
    
  if (block==NULL) {
    fprintf(stderr, "Cannot allocate block in registerMem\n");
    abort();
  }
  block->start = begin;
  block->end = begin+length;
  block->next = first;
  first = block;
#endif
}

/**
 * Unregister a block of memory at a certain beginning position.
 * If memory registration is active and the block was never
 * registered, an error message is printed.
 **/ 
void unregisterMem(void * begin) {
#if MEMORY_TRACKING
  struct memtracking_block **to_go = &first;

  if (begin==NULL) 
    return;
  while (*to_go) {
    if ((*to_go)->start == begin) {
      struct memtracking_block *dead = *to_go;
      *to_go = dead->next;
      free(dead);
      return;
    }
    to_go = &(*to_go)->next;
  }
  fprintf(stderr,
	  "ERROR: cannot unregister block of memory that was never registered in the first place\n");
#endif
}

/**
 * Verify that pointer is a valid address in memory, 
 * prints a warning if the address is invalid. Can do 
 * nothing if debugging memory is turned off.
 *
 * @param pointer the address to verify
 * @return 0 on error, 1 on ok
 **/
int checkPointer(void * pointer) {
#if MEMORY_TRACKING
  struct memtracking_block * current=first;
  while (current!=NULL) {
    if (current->start<=pointer && current->end >=pointer)
      return 1;
    current = current->next;
  }
  return 0;
#else
  return 1;
#endif
}
