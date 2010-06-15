/**
 * Debugging helper methods. 
 * @author Christian Grothoff
 **/

#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include "debugging.h"
#include "types.h"

/**
 * print a debug message
 * @param format the string describing the error message
 **/
void print(const char *format, ...) {
  va_list       args;
  va_start(args, format);
  vfprintf(PRINTTO, format, args);
  va_end(args);
}

/**
 * errexit - print an error message and exit
 * @param format the string describing the error message
 **/
void errexit(const char *format, ...) {
  va_list args;

  va_start(args, format);
  vfprintf(stderr, format, args);
  va_end(args);
  exit(1);
}

/**
 * Useful debugging function for looking at memory.
 **/
void dump_memory(void * addr, int num) {
  int cntr;
  int * ptr;
  
  ptr = addr;
  printf("Memory dump (%d bytes) of base address 0x%0X:\n",
	 num, (int) addr);
  for ( cntr = 0; cntr < num; cntr++ ) {
    printf(" offset %d ", cntr);
    printf("= 0x%08X = %d\n", ptr[cntr], ptr[cntr]);
  }
}

void pSTE(StackTraceElement* ste) {
  fprintf(stderr, "%.*s.%.*s[%.*s:%d]\n",
	  ste->className->data->length,
	  ste->className->data->values,
	  ste->methodName->data->length,
	  ste->methodName->data->values,
	  ste->fileName->data->length,
	  ste->fileName->data->values,
	  ste->lineNumber);
}
