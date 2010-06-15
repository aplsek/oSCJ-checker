/*
 * The interface that an execution engine must provide to the common
 * native library.
 */
#ifndef _ENGINE_H
#define _ENGINE_H
#include "jtypes.h"

extern int    process_argc;
extern char **process_argv;
extern void   launchContext(void *ctx);

// It would be nice if the image loading code where shared
extern void *getImageBaseAddress();
extern void *getImageEndAddress();

/**
 * An engine-specific primitive operation.  Force the calling
 * method to save all call-preserved registers.  This method
 * may have an empty implementation in engines that do not
 * perform register allocation.
 */
#define SAVE_REGISTERS()

#endif /* _ENGINE_H */
