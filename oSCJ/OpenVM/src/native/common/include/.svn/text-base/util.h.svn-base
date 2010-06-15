 /**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This is a general purpose header file that contains useful macros
 * and functions to both clean up code and increase its unit
 * testability.
 *
 * @file include/ovm/main.c
 * @author Jan Vitek
 * @author Ben L. Titzer
 * @author David Holmes
 **/

/**
 C Coding standards: 

 Struct names follow the same convention as Java class names with a leading
 upper case char. Variables and functions are all lower case with
 underscores to separate words. Macros in upper case with underscores.

 Insert spaces between arithmetic operators, assignment, comparison.  No
 space before or after parenthesis (function calls). Space after commas.

 Lines length, max 78 chars.

 Use emacs autoformat for comments.
**/

#ifndef _UTIL_H_
#define _UTIL_H_

#include "config.h"
#include "jtypes.h"
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
/* on systems with both prefer the GNU libc error function */
#if HAVE_ERROR_H == 1
  #include <error.h>
#elif HAVE_ERR_H == 1 || defined(OSX_BUILD)
  #include <err.h>
#else
  #define err(eval,fmt,args...) do { fprintf(stderr,fmt,##args); fprintf(stderr,"\n"); exit(eval); } while(0)
  #define warn(fmt,args...) do { fprintf(stderr,fmt,##args); fprintf(stderr,"\n"); } while(0)
  #define warnx(fmt,args...) do { fprintf(stderr,fmt,##args); fprintf(stderr,"\n"); } while(0)
#endif
#include <string.h>

/**-----------------------------------------------------------------------
 * Error checking macros are used to perform sanity checks of system
 * and library calls. 
 * By defaults all calls are checked, this can be turned off by
 * defining UNCHECKED_SYSCALLS == YES (1)
 *
 * These macros should only be used for system/library calls that on 
 * success return zero, and on failure return non-zero AND set errno.
 *
 *       WARN_ERRNO( F , M )
 *       EXIT_ERRNO( F , M )
 *       ABORT_ERRNO(F , M )
 *
 * Each macro is given a function expression F and a message M. It will
 * evaluate F and in case it yields a non zero result will print M (WARN), 
 * print M and exit (EXIT) or print M and abort (ABORT). In each case the
 * the standard error string for the current value of errno will also be
 * printed.
 *
 * These macros should be used for system/library calls that on success
 * return zero, and on failure return an error code (without setting errno)
 *
 *       WARN_RETVAL( F , M )
 *       EXIT_RETVAL( F , M )
 *       ABORT_RETVAL(F , M )
 *
 * Each macro is given a function expression F and a message M. It will
 * evaluate F and in case it yields a non zero result will print M (WARN), 
 * print M and exit (EXIT) or print M and abort (ABORT). In each case the
 * the standard error string for the value returned from F will also be
 * printed.
 *
 **/
#if UNCHECKED_SYSCALLS == YES  /*------------------------------------------*/

#define WARN_ERRNO( exp, msg)  do{ if (exp); } while (0)
#define EXIT_ERRNO( exp, msg)  do{ if (exp); } while (0)
#define ABORT_ERRNO( exp, msg) do{ if (exp); } while (0)

#define WARN_RETVAL( exp, msg)  do{ if (exp); } while (0)
#define EXIT_RETVAL( exp, msg)  do{ if (exp); } while (0)
#define ABORT_RETVAL( exp, msg) do{ if (exp); } while (0)

#else /*---------------- UNCHECKED_SYSCALLS --------------------------------*/


#define DO( STMT )   do{ STMT; }while(0)

#define ON_ERROR( F , A )  DO(int res = (F); if (res != 0) {A;})

#ifdef HAVE_ERROR_H
  #define WARN_ERRNO(exp, msg)  ON_ERROR( exp, error(0, errno, msg))
  #define EXIT_ERRNO(exp, msg)  ON_ERROR( exp, error(1, errno, msg))
  #define ABORT_ERRNO(exp, msg) ON_ERROR( exp, error(0, errno, msg);abort())

  #define WARN_RETVAL(exp, msg)  ON_ERROR( exp, error(0, res, msg))
  #define EXIT_RETVAL(exp, msg)  ON_ERROR( exp, error(1, res, msg))
  #define ABORT_RETVAL(exp, msg) ON_ERROR( exp, error(0, res, msg);abort())

#else /* assumes only two possibilities - if that changes, so must this */
  #define WARN_ERRNO(exp, msg)  ON_ERROR( exp, warn(msg))
  #define EXIT_ERRNO(exp, msg)  ON_ERROR( exp, err(1, msg))
  #define ABORT_ERRNO(exp, msg) ON_ERROR( exp, warn(msg);abort())

  #define WARN_RETVAL(exp, msg)  ON_ERROR( exp, warnx("%s: %s",msg, strerror(res)))
  #define EXIT_RETVAL(exp, msg)  ON_ERROR( exp, err(1, "%s: %s", msg, strerror(res)))
  #define ABORT_RETVAL(exp, msg) ON_ERROR( exp, warnx("%s: %s",msg, strerror(res));abort())

#endif  

#endif /*------------ End of UNCHECKED_SYSCALLS ---------------------------*/


#define  EXIT(i) {  fflush(stderr);  fflush(stdout);  exit(i); }
/*------------------------------------------------------------*/
#define  PRINT_EXIT(M) { fprintf(stderr, "Exiting(on line %d from %s): %s\n",\
	      __LINE__,__FILE__,M); fflush(stderr); fflush(stdout); abort(); }

/* This macros can be turned off when debugging mode is off.
 * Right they should stay on... --jv */
#if DEBUGGING
#define IFDEBUG_PRINT_ERR( S ) fprintf(stderr, "Error: %s\n",S)
#define IFDEBUG_PRINT_WARN( S ) fprintf(stderr, "Error: %s\n",S)
#define IFDEBUG_DO( E ) { E; }
#define DEBUGVAR(var) var
#define IFDEBUG(F) F
#define IFDEBUG_ASSERT( E, S ) ASSERT( E, S)

#else
#define IFDEBUG_PRINT_ERR( S )  {}
#define IFDEBUG_PRINT_WARN( S ) {}
#define IFDEBUG_DO( E ) { }
#define DEBUGVAR(var) 
#define IFDEBUG(F) {}
#define IFDEBUG_ASSERT( E, S ) {}
#endif /* DEBUGGING */

#define ASSERT_ALLOC( V ) EXIT_ON_ERROR( V != 0, "Out of memory.");

#define WARN( E )  IFDEBUG_DO( if ( (E) == 0 ) \
                   IFDEBUG_PRINT_ERR("Assertion violation.") )

#define ASSERT( E , S )  IFDEBUG_DO( if ( (E) == 0 ) PRINT_EXIT( S ) )

/* Document that the value is not supposed to change. */
#define final 

DEBUGVAR(extern int frameDepth);
int  print_address(int a);

#define MASK_SHIFT_32(X) ((X) & 31)
#define MASK_SHIFT_64(X) ((X) & 63)

#ifndef TRUE
#define TRUE 1
#endif
#ifndef FALSE
#define FALSE 0
#endif
#define J_TRUE ((jboolean)TRUE)
#define J_FALSE ((jboolean)FALSE)

#endif  /*------------ UTIL_H ------------------------------------------------*/

