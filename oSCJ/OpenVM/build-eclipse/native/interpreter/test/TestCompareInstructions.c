/**
 * This file contains the test code for the 
 * interpreter related to Compare instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestCompareInstructions.c
 * @author James Liang
 */ 

#include "CUnit.h"
#include "framework.h"

extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;

void testDCMPG() {
    registerMethod("TestCompareInstructions.testDCMPG()");
    pc[0] = INSTR_DCMPG;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)1279914.331);
    PUSH_W((double)1279914.331);
    runInterpreter();
    assertEquals("equal test",POP().jint,0);
    PUSH_W((double)19914.331);
    PUSH_W((double)1279914.331);
    runInterpreter();
    assertEquals("less than test",POP().jint,-1);
    PUSH_W(1279914.331);
    PUSH_W(12914.331);
    runInterpreter();
    assertEquals("greater than test",POP().jint,1);
}


void testFCMPL() {
  registerMethod("TestCompareInstructions.testFCMPL()");
  pc[0] = INSTR_FCMPL;
  pc[1] = INSTR_BREAKPOINT;
  PUSH((float)71.331);
  PUSH((float)71.33);
  runInterpreter();assertEquals("greater than test",POP().jint,1);
  PUSH((float)71.331);
  PUSH((float)71.331);
  runInterpreter();assertEquals("equal test",POP().jint,0);
  PUSH((float)71.33);
  PUSH((float)71.331);
  runInterpreter();assertEquals("equal test",POP().jint,-1);
}


void testLCMP() {
  registerMethod("TestCompareInstructions.testLCMP()");
  pc[0] = INSTR_LCMP;
  pc[1] = INSTR_BREAKPOINT;
  PUSH_W((long long)1279914331);
  PUSH_W((long long)127991433);
  runInterpreter();assertEquals("greater than test",POP().jint,1);
  PUSH_W((long long)1279914331);
  PUSH_W((long long)1279914331);
  runInterpreter();assertEquals("equal test",POP().jint,0);
  PUSH_W((long long)127991433);
  PUSH_W((long long)1279914331);
  runInterpreter();assertEquals("equal test",POP().jint,-1);
}


void testFCMPG() {
  registerMethod("TestCompareInstructions.testFCMPG()");
  pc[0] = INSTR_FCMPG;
  pc[1] = INSTR_BREAKPOINT;
  PUSH((float)73.331);
  PUSH((float)71.33);
  runInterpreter();assertEquals("greater than test",POP().jint,1);
  PUSH((float)71.331);
  PUSH((float)71.331);
  runInterpreter();assertEquals("equal test",POP().jint,0);
  PUSH((float)71.33);
  PUSH((float)71.331);
  runInterpreter();assertEquals("equal test",POP().jint,-1);
}



int main() {
  setup();
  testDCMPG();
  testLCMP();
  testFCMPG();
  testFCMPL();
  teardown();
  return 0;
}
