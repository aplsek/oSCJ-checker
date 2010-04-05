/**
 * Global variables in the interpreter.
 **/

#include "globals.h"

/* globals as declared in globals.h */
struct ovm_core_execution_CoreServicesAccess * executiveCSA;
struct arr_jbyte* utf8Array;
struct ovm_core_execution_CoreServicesAccess_VTBL * csavtbl;
int verbose_mode = 0;
int override_tracing = 0;
ByteCode *global_catch_code;
char** process_argv;
int process_argc;
