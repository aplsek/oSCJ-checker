/**
 * This file contains the test code for the 
 * interpreter related to Pop instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestPopInstructions.c
 * @author James Liang
 */ 

#include "CUnit.h"
#include "framework.h"



extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;

void testPOP() {
    registerMethod("TestPopInstructions.testPOP()");
    pc[0] = INSTR_POP;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(69);
    PUSH(73);
    runInterpreter();//frame,&area,0,0);
    assertEquals("basic test",POP().jint,69);
}


void testPOP2() {
    registerMethod("TestPopInstructions.testPOP2()");
    pc[0] = INSTR_POP2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(69); // expecting this value
    PUSH(321);
    PUSH(73);
    PUSH(25); // expecting this value
    PUSH_W((long long)5729480);
    runInterpreter();//frame,&area,0,0);
    assertEquals("category 2 test",POP().jint,25);
    runInterpreter();//frame,&area,0,0);
    assertEquals("category 1 test",POP().jint,69);
}

int main() {
  setup();
  testPOP();
  testPOP2();
  teardown();
  return 0;
}
