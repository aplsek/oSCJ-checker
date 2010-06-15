/**
 * This file contains the test code for the 
 * Stack
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the function that is being tested
 * and that function is tested using CUnit. 
 * @file src/Stack/test/TestStack.c
 * @author James Liang
 */
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <ovm/util.h>
#include <ovm/types.h>
#include <ovm/interpreter_defs.h>
#include <CUnit.h>
#include "framework.h" /* unless there's a good reason not to */

Primitive p;
struct StackArea *area;
struct StackFrame *frame;

void setup() {
  initializeCUnit();
  setBehaviour(CUNIT_REPORT_ALL | CUNIT_PROGRESS_DOT);
  area = newArea(65536);
  frame = newFrame(&area,5,5,0);
}

void teardown() {
    printf("done\n");
  /** free up memory that was allocated */
}

void testGrowStack() {
  int i;
  registerMethod("TestStack.testGrowStack");
  for (i = 0; i<900;i++) {
    mark("Storing value");
    push(frame,(Primitive)69);
  }
  for (i = 0; i<900;i++)
    assert("checking popped value",pop(frame).i==69);
}

/**
 * See file header
 */
void testPopPush() {
 
  registerMethod("TestStack.testPopPush()");
  p.i = 2;push(frame,p); 
  assert("Simple integer",pop(frame).i==2); 
  
  p.f = 2.997;
  push(frame,p);

  assert("Simple float",pop(frame).f-2.997<.00001);
  
  p.i=9;push(frame,p);
  p.f=5.999;push(frame,p);
  p.i=1;push(frame,p);
  
  assert("Extensive Testing",pop(frame).i==1);
  assert("Simple float",(float)pop(frame).f-5.999<.00001);
  assert("Extensive Testing",pop(frame).i==9);
}
   
   /**
    * See file header
    */
  void testPushPopw() {
    registerMethod("TestStack.testPopPushw()");
    pushw(frame,(WidePrimitive)257924805ll); 
    assert("testing long",popw(frame).l==257924805L); 
    pushw(frame,(WidePrimitive)-5435242.997);
    assert("testing double",popw(frame).d-5435242.997<.00001);
  }
  /**
   * See file header
   */
  void testSetGetLocal() {
    registerMethod("TestStack.testSetGetLocal()");
    mark("setting local variables");
    p.i=1;setLocal(frame,0,p); 
    p.i=-7;setLocal(frame,1,p); 
    p.f=-.99;setLocal(frame,2,p); 
    p.f=1.441;setLocal(frame,3,p); 
    mark("Done setting local variables");
    assert("Positive int",getLocal(frame,0).i==1);
    assert("Negative int",getLocal(frame,1).i==-7);
    assert("Positive float",getLocal(frame,3).f-1.441<.00001); 
    assert("Positive float",abs(getLocal(frame,2).f+.99) <0.0001); 
}
/**
 * See file header
 */
void testSetGetLocalw() {  
  registerMethod("TestStack.testSetGetLocalw()");
  setLocalw(frame,0,(WidePrimitive)(long long)458390479);
  setLocalw(frame,3,(WidePrimitive)545244.54526654);
  
  assert("testing long",getLocalw(frame,0).l==458390479);
  assert("testing double",abs(getLocalw(frame,3).d-545244.54526654)<0.00001);
}
/**
 * main- drives tests for the Stack
 */
int main() {
  setup();
  testPopPush();
  testPushPopw();
  testSetGetLocal();
  testSetGetLocalw();
  testGrowStack();
  teardown();
  return 0;
} 


