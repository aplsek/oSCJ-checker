/* Simple RapiTime Instrumentation Library Example */

#include "jtypes.h"


/*
 * This implemtation outputs a simple incrementing integer
 * for each timestamp, it does not measure real times. This
 * library is only intended for use as a host-based example.
 * A specific board support package for each target would
 * normally be required.
 */


typedef unsigned short rapita_t;

#define RAPITA_BASE 0x80000800
#define RAPITA_OUT (*(rapita_t *) (RAPITA_BASE + 0x4))
#define RAPITA_MASK (*(rapita_t *) (RAPITA_BASE + 0x8))
#define RAPITA_INTERRUPT (*(rapita_t *) (RAPITA_BASE + 0xc))


/* Initialise the Instrumentation Library*/
void RPT_Init        (void);


/* An instrumentation point. An IPoint would normally be fast, efficient code
 * possibly using a #define. Care must be taken if a #define is used in this
 * file, contact Rapita Systems for further examples. */
void RPT_Output_Trace();

void RPT_Ipoint (int x);
