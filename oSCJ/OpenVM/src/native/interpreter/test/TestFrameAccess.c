/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This file contains unit tests for the frame access code.
 *
 * @file test/TestFrameAccess.c
 * @author Ben L. Titzer
 **/

#include <ovm/frame.h>
#include <stdlib.h>
#include <CUnit.h>
#include "framework.h"


void testCall(int depth);

CodeFragment* dummyFragment=NULL;

#define TEST_MAKE_CONTEXT   \
  current_context = create_context(4, dummyFragment); \
  assertMsg("make_context test", current_context != NULL); \
  get_frame_values(current_context, &local_vars, &stack_top, &pc); \
  assertMsg("get_frame_values: ", local_vars != NULL); \
  assertMsg("get_frame_values: ", stack_top != NULL); \
  assertMsg("get_frame_values: ", pc != NULL);

#define TEST_SETLOCAL(x, y) set_local(current_context, \
                                      local_vars, x, (jvalue) (y));
#define TEST_GETLOCAL(x, y) assertEquals("get_local test failed", \
                           get_local(current_context, local_vars, x).jint,(y))
#define TEST_PUSH(x) push(current_context, &stack_top, (jvalue)(x));
#define TEST_POP(x) assertEquals("pop test failed", \
                           pop(current_context, &stack_top).jint, (x)) 

#define STD_LOCALS

static NativeContext* current_context;
static jvalue*  local_vars;
static jvalue*  stack_top;
static byte*  pc;

/**
 * TestContextCreation()
 *
 * Test that a context can be created correctly without errors.
 **/
void TestContextCreation() {

  STD_LOCALS;

  registerMethod("TestFrameAccess.testFrameCreation()");
  TEST_MAKE_CONTEXT;

}

/**
 * TestPushPop()
 *
 * Test that a context can be created and that stack frames in that
 * context can push and pop correctly.
 **/
void TestPushPop() {

  STD_LOCALS;

  registerMethod("TestFrameAccess.testPushPop()");

  TEST_MAKE_CONTEXT;

  TEST_PUSH(10);
  TEST_PUSH(11);
  TEST_PUSH(12);
  TEST_POP(12);
  TEST_POP(11);
  TEST_POP(10);
}

/**
 * TestLocalAccess()
 *
 * Test that a context and a frame in that context can be created
 * correctly and that local variables can be accessed correctly.
 * Intersperse pushes and pops to make sure they do not break
 * the local variable access by overwriting them.
 **/
void TestLocalAccess() {

  STD_LOCALS;

  registerMethod("TestFrameAccess.testLocalAccess()");

  TEST_MAKE_CONTEXT;
  TEST_SETLOCAL(0,100);
  TEST_GETLOCAL(0,100);
  TEST_SETLOCAL(1,101);
  TEST_GETLOCAL(1,101);
  
  TEST_SETLOCAL(2,102);
  TEST_GETLOCAL(2,102);

  TEST_GETLOCAL(2,102);
  TEST_GETLOCAL(1,101);
  TEST_GETLOCAL(0,100);

  TEST_SETLOCAL(0,200);
  TEST_SETLOCAL(1,201);
  TEST_SETLOCAL(2,202);

  TEST_GETLOCAL(2,202);
  TEST_GETLOCAL(1,201);
  TEST_GETLOCAL(0,200);

}

/**
 * TestMethodCall()
 * 
 * Test that a "fake" method call can be placed. Create a new frame
 * and insert things into it. Make sure the old frame works correctly
 * after the "fake" invocation.
 **/
void TestMethodCall() {
  jvalue * topofStack;
  STD_LOCALS;

  registerMethod("TestFrameAccess.testMethodCall()");

  TEST_MAKE_CONTEXT;
  
  // getting ready to make a call
 
  TEST_SETLOCAL(0,200);
  TEST_SETLOCAL(1,201);
  TEST_PUSH(20);
  TEST_PUSH(21);
  TEST_PUSH(22);
  TEST_GETLOCAL(0,200);
  TEST_GETLOCAL(1,201);
  TEST_POP(22);
  TEST_POP(21);

  topofStack = stack_top;
  
  create_frame(current_context, 
	       dummyFragment, 
	       NULL, /* blueprint of main object */
	       &local_vars, 
	       &stack_top, 
	       &pc,
	       0);
  
  TEST_PUSH(30);
  TEST_PUSH(31);
  TEST_SETLOCAL(0,300);
  TEST_SETLOCAL(1,301);
  TEST_POP(31);
  TEST_POP(30);
  TEST_GETLOCAL(0,300);
  TEST_GETLOCAL(1,301);

  /*
  curr_frame = prepare_frame(current_context, &local_vars, &stack_top, &pc);
  
  init_new_frame(curr_frame, dummyFragment,
		 2, stack_top, local_vars, pc);
  // coming back from a call
  */  
  assert(local_vars == topofStack);
  pop_frame(current_context, &local_vars, &stack_top, &pc);

  //  printf("\n\nrestored FRAME info %d %d,%d\n",(int)local_vars,(int)stack_top,(int)pc);
  

  TEST_GETLOCAL(0,200);
  TEST_GETLOCAL(1,201);
  TEST_POP(20);


}

/**
 * TestDeepStackAccess()
 *
 * Push lots of things onto the stack and make sure everything works
 * correctly.
 **/
void TestDeepStackAccess() {
  int cntr;
  STD_LOCALS;

  registerMethod("TestFrameAccess.testMultipleMethodCalls()");

  TEST_MAKE_CONTEXT;

  for ( cntr = 50; cntr < 100; cntr++ )
    { TEST_PUSH(cntr); }

  for ( cntr = 99; cntr >= 50; cntr-- )
    { TEST_POP(cntr); }

  for ( cntr = 500; cntr < 700; cntr++ )
    { TEST_PUSH(cntr); }

  for ( cntr = 699; cntr >= 500; cntr-- )
    { TEST_POP(cntr); }

}

/* these variables are used in the calls to testCall() */
STD_LOCALS;

/**
 * TestMultipleMethodCalls()
 *
 * Test multiple stack frame allocations and make sure they work
 * as advertised.
 **/
void TestMultipleMethodCalls() {
  int cntr;

  registerMethod("TestFrameAccess.testMultipleMethodCalls()");

  TEST_MAKE_CONTEXT;

  TEST_PUSH(40);

  for ( cntr = 0; cntr < 3; cntr++ )
    testCall(cntr);

  TEST_POP(40);


}

void testCall(int val) {
  jvalue *topofStack;

  // here is the new frame
  topofStack = stack_top;
  create_frame(current_context, 
	       dummyFragment, 
	       NULL, /* blueprint! */
	       &local_vars, 
	       &stack_top, 
	       &pc, 
	       0);

  TEST_PUSH(30*val);
  TEST_PUSH(31*val);
  TEST_SETLOCAL(0,300*val);
  TEST_SETLOCAL(1,301*val);
  TEST_POP(31*val);
  TEST_POP(30*val);
  TEST_GETLOCAL(0,300*val);
  TEST_GETLOCAL(1,301*val);

  assert(local_vars == topofStack);
  pop_frame(current_context, &local_vars, &stack_top, &pc);

}


int main() {
  setup();
  TestDeepStackAccess();
  TestMultipleMethodCalls(); 
  teardown();
  return 0;
}
