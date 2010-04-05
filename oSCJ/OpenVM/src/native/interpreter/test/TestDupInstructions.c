/**
 * This file contains the test code for the 
 * interpreter related to Dup instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestDupInstructions.c
 * @author James Liang
 */ 

#include "CUnit.h"
#include "framework.h"

extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;


void testDUP() {
    registerMethod("TestDupInstructions.testDUP()");
    pc[0] = INSTR_DUP;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(69);
    runInterpreter();
    assertEquals("basic test p1",POP().jint,69);
    assertEquals("basic test p2",POP().jint,69);
}


void testDUPx1() {
    registerMethod("TestDupInstructions.testDUP_X1()");
    pc[0] = INSTR_DUP_X1;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(69);
    PUSH(155);
    runInterpreter();
    assertEquals("basic test p1",POP().jint,155);
    assertEquals("basic test p2",POP().jint,69);
    assertEquals("basic test p3",POP().jint,155);
}


void testDUPx2() {
    registerMethod("TestDupInstructions.testDUP_X2()");
    pc[0] = INSTR_DUP_X2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(69);
    PUSH(155);
    PUSH(255);
    runInterpreter();
    assertEquals("basic test p1",POP().jint,255);
    assertEquals("basic test p2",POP().jint,155);
    assertEquals("basic test p3",POP().jint,69);
    assertEquals("basic test p4",POP().jint,255);
}


void testDUP2() {
    registerMethod("TestDup2Instructions.testDUP2()");
    pc[0] = INSTR_DUP2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)69);
    PUSH_W((long long)169);
    runInterpreter();
    assertEquals("basic test p1",POP_W().jlong,169);
    assertEquals("basic test p2",POP_W().jlong,169);

}


void testDUP2_X1() {
    registerMethod("TestDup2Instructions.testDUP2()_X1");
    pc[0] = INSTR_DUP2_X1;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(69);
    PUSH_W((long long)169);
    runInterpreter();
    assertEquals("basic test p1",POP_W().jlong,169);
    assertEquals("basic test p2",POP().jint,69);
    assertEquals("basic test p2",POP_W().jlong,169);
}


void testDUP2_X2() {
    registerMethod("TestDup2Instructions.testDUP2()_X2");
    pc[0] = INSTR_DUP2_X2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)69);
    PUSH_W((long long)169);
    runInterpreter();
    assertMsg("basic test p1",POP_W().jlong==169);
    assertMsg("basic test p2",POP_W().jlong==69);
    assertMsg("basic test p2",POP_W().jlong==169);
}


void testSWAP() {
    registerMethod("TestDup2Instructions.testSWAP");
    pc[0] = INSTR_SWAP;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(69);
    PUSH(169);
    runInterpreter();
    assertEquals("basic test p2",POP().jint,69);
    assertEquals("basic test p2",POP().jint,169);

}

int main() {
  setup();
  testDUP();
  testDUP2();
  testDUP2_X1();
  testDUP2_X2();
  testDUPx1();
  testDUPx2();
  testSWAP();
  teardown();
  return 0;
}
