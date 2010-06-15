/**
 * Testing framework
 * @author Christian Grothoff
 * @author James Liang
 **/

#ifndef FRAMEWORK_H
#define FRAMEWORK_H

#include "CUnit.h"
#include <math.h>
#include <instructions.h>
#include <ovm/frame.h>
#include <stdlib.h>
#include <CUnit.h>
#include <math.h>
#include <netinet/in.h>
#include <ovm/interpreter.h>


extern CodeFragment * dummyFragment;
extern NativeContext* current_context;
extern jvalue*  local_vars;
extern jvalue*  stack_top;
extern byte*    pc;
extern byte*    endingPC;

void setup();

void runInterpreter();

void teardown();

#define isEqual(val1,val2)  (0.001>abs(val1-val2)) 

#define PUSH PUSH_P
#define SETLOCAL SETLOCAL_P
#define POKE POKE_P

#define push push_prim
#define set_local set_local_prim
#define poke_from_top poke_prim_from_top

#endif
