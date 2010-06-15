#ifndef RUNTIME_H
#define RUNTIME_H

#include "globals.h"
#include "types.h"
#include "mthread.h"
#include "native_interface.h"
#include <math.h>

/**********************/
/*** Native context ***/
/**********************/
#define REFLECTION_INVOCATION_BUFFER_WORD_SIZE 256  // total buffer size
#define REFLECTION_INVOCATION_NONARG_WORD_SIZE   7  // size for slots other than arguments

#define MAX_NCONTEXT 1024
extern struct s3_services_simplejit_NativeContext* currentContext;
extern struct s3_services_simplejit_NativeContext contexts[MAX_NCONTEXT];
extern int ncontexts;

#endif
