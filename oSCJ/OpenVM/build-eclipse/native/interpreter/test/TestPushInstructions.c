/**
 * This file contains the test code for the 
 * interpreter related to Push instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestPushInstructions.c
 * @author James Liang
 */ 

#include "CUnit.h"
#include "framework.h"


extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;

void testICONST_X() {
  registerMethod("TestPushInstructions.testICONST_X()");
  pc[0] = INSTR_ICONST_M1;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing iconst_m1",POP().jint==-1);
  pc[0] = INSTR_ICONST_0;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing iconst_0",POP().jint==0);
  pc[0] = INSTR_ICONST_1;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing iconst_1",POP().jint==1);
  pc[0] = INSTR_ICONST_2;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing iconst_2",POP().jint==2);
  pc[0] = INSTR_ICONST_3;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing iconst_3",POP().jint==3);
  pc[0] = INSTR_ICONST_4;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing iconst_4",POP().jint==4);
  pc[0] = INSTR_ICONST_5;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing iconst_5",POP().jint==5);
}

void testLCONST_X() {
  registerMethod("TestPushInstructions.testLCONST_X()");
  pc[0] = INSTR_LCONST_1;
  pc[1] = INSTR_BREAKPOINT;
  runInterpreter();assertMsg("testing lconst_1",POP_W().jlong==1);
  pc[0] = INSTR_LCONST_0;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing lconst_0",POP_W().jlong==0);
}

void testFCONST_X() {
  registerMethod("TestPushInstructions.testFCONST_X()");
  pc[0] = INSTR_FCONST_0;
  pc[1] = INSTR_BREAKPOINT;
  runInterpreter();assertMsg("testing fconst_0",isEqual(POP().jfloat,0));
  pc[0] = INSTR_FCONST_1;
  pc[1] = INSTR_BREAKPOINT;
  runInterpreter();assertMsg("testing fconst_1",isEqual(POP().jfloat,1));
  pc[0] = INSTR_FCONST_2;
  pc[1] = INSTR_BREAKPOINT;
  runInterpreter();assertMsg("testing fconst_2",isEqual(POP().jfloat,2));
}



void testDCONST_X() {
  registerMethod("TestPushInstructions.testDCONST_X()");
  pc[0] = INSTR_DCONST_0;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing dconst_0",isEqual(POP_W().jdouble,0));
  pc[0] = INSTR_DCONST_1;
  pc[1] = INSTR_BREAKPOINT;
   runInterpreter();assertMsg("testing dconst_1",isEqual(POP_W().jdouble,1));
}


void testACONST_NULL() {
  registerMethod("TestPushInstructions.testACONST_NULL");
  pc[0] = INSTR_ACONST_NULL;
  pc[1] = INSTR_BREAKPOINT;
  runInterpreter();
  assertMsg("testing iconst_m1",POP().jint==0);
}


void testBIPUSH() {
  registerMethod("TestPushInstructions.testBIPUSH");
  pc[0] = INSTR_BIPUSH;
  pc[1] = -125;
  pc[2] = INSTR_BREAKPOINT;
  runInterpreter(); 
  assertMsg("testing",POP().jint==-125);
}


void testSIPUSH() {
  registerMethod("TestPushInstructions.testSIPUSH");
  pc[0] = INSTR_SIPUSH;
  *((signed short int *)&pc[1]) = htons(-1250);
  pc[3] = INSTR_BREAKPOINT;
  runInterpreter(); 
  assertMsg("testing",POP().jint==-1250);
}


int main() {
  setup();
  testDCONST_X();
  testFCONST_X();
  testICONST_X();
  testLCONST_X();
  testACONST_NULL();
  testBIPUSH();
  testSIPUSH(); 
  teardown();
  return 0;
}

