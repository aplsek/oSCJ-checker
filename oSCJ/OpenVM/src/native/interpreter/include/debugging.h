/**
 * Debugging helper methods. 
 * @author Christian Grothoff
 **/

#ifndef DEBUGGING_H
#define DEBUGGING_H

#include "yesno.h"

/* where to print with the 'print' method? */
#define PRINTTO stdout

/* should we check asserts? */
#define CHECK_ASSERTS NO

/**
 * print a debug message
 * @param format the string describing the error message
 **/
void print(const char *format, ...);

/**
 * errexit - print an error message and exit
 * @param format the string describing the error message
 **/
void errexit(const char *format, ...);

/**
 * Useful debugging function for looking at memory.
 **/
void dump_memory(void * addr, int num);

 
#endif
