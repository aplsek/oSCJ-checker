/* Simple RapiTime Instrumentation Library Example */

#include "rpt.h"
#include <stdio.h>

//typedef unsigned short rapita_t;

#define RAPITA_BASE 0x80000800
#define RAPITA_OUT (*(rapita_t *) (RAPITA_BASE + 0x4))
#define RAPITA_MASK (*(rapita_t *) (RAPITA_BASE + 0x8))
#define RAPITA_INTERRUPT (*(rapita_t *) (RAPITA_BASE + 0xc))

void RPT_Init (void)
{
  //  printf("rpt init !!!!!!\n");
  RAPITA_MASK = 0xffff; // output port mask
  RAPITA_INTERRUPT = 0;
  RAPITA_OUT = 0;
}

void RPT_Ipoint (int x)
{
  //printf("%u\n", x);
  RAPITA_OUT = x & 0xffff;
}

void RPT_Output_Trace(){
}
