/*
 * This file is intentionally left empty.
 * Execution engines #include the file app_native_calls.h when
 * compiling the native call dispatcher.  If an application programmer
 * wishes to call C code using Ovm's LibraryGlue interface, he or she
 * should declare all the called C functions in a file called
 * app_native_calls.h, and place that file in the directory where
 * gen-ovm is run.
 *
 * If the application programmer does not use the LibraryGlue native
 * interface, this file empty will be included in place of the
 * application-specific one.
 */


/* Simple RapiTime Instrumentation Library Example */

#include "jtypes.h"


/*
 * This implemtation outputs a simple incrementing integer
 * for each timestamp, it does not measure real times. This
 * library is only intended for use as a host-based example.
 * A specific board support package for each target would
 * normally be required.
 */


/* Initialise the Instrumentation Library*/
void RPT_Init        (void);


/* An instrumentation point. An IPoint would normally be fast, efficient code
 * possibly using a #define. Care must be taken if a #define is used in this
 * file, contact Rapita Systems for further examples. */
void RPT_Output_Trace();

void RPT_Ipoint (int x);
