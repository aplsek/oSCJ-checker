/**
 * This file contains the test code for the 
 * interpreter related to Store instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestStoreInstructions.c
 * @author James Liang
 */ 

#include "CUnit.h"
#include "framework.h"


extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;


void testISTORE() {
    registerMethod("TestStoreInstructions.testISTORE()");
    pc[0] = INSTR_ISTORE;
    pc[1] = 3;
    pc[2] = INSTR_BREAKPOINT;
    PUSH(127931);
    runInterpreter();
    assertMsg("basic test",GETLOCAL(3).jint==127931);
}



void testWISTORE() {
    registerMethod("TestStoreInstructions.testWIDE_ISTORE()");
    pc[0] = INSTR_WIDE; 
    pc[1] = INSTR_ISTORE;
    *((unsigned short *)(&(pc[2]))) = htons(301); 
    pc[4] = INSTR_BREAKPOINT;
    PUSH(127931);
    runInterpreter();
    assertMsg("basic test",GETLOCAL(301).jint==127931);
}


void testWFSTORE() {
    registerMethod("TestStoreInstructions.testWIDE_FSTORE()");
    pc[0] = INSTR_WIDE; 
    pc[1] = INSTR_FSTORE;
    *((unsigned short *)(&(pc[2]))) = htons(311) ;
    pc[4] = INSTR_BREAKPOINT;
    PUSH((float)127.931);
    runInterpreter();
    assertMsg("basic test",GETLOCAL(311).jfloat==(float)127.931);
}


void testWDSTORE() {
    registerMethod("TestStoreInstructions.testWIDE_DSTORE()");
    pc[0] = INSTR_WIDE; 
    pc[1] = INSTR_DSTORE;
    *((unsigned short *)(&(pc[2]))) = htons(298); 
    pc[4] = INSTR_BREAKPOINT;
    PUSH_W(1279914.331);
    runInterpreter();
    assertMsg("basic test",GETLOCAL_W(298).jdouble==1279914.331);
}

void testFSTORE() {
    registerMethod("TestStoreInstructions.testFSTORE()");
    pc[0] = INSTR_FSTORE;
    pc[1] = 3;
    pc[2] = INSTR_BREAKPOINT;
    PUSH((float)127.931);
    runInterpreter();
    assertMsg("basic test",GETLOCAL(3).jfloat==(float)127.931);
}

void testDSTORE() {
    registerMethod("TestStoreInstructions.testDSTORE()");
    pc[0] = INSTR_DSTORE;
    pc[1] = 3;
    pc[2] = INSTR_BREAKPOINT;
    PUSH_W(1279914.331);
    runInterpreter();
    assertMsg("basic test",GETLOCAL_W(3).jdouble==1279914.331);
}



void testWLSTORE() {
    registerMethod("TestStoreInstructions.testWIDE_LSTORE()");
    pc[0] = INSTR_WIDE; 
    pc[1] = INSTR_DSTORE;
    *((unsigned short *)(&(pc[2]))) = htons(298); 
    pc[4] = INSTR_BREAKPOINT;
    PUSH_W((long long)1279914331);
    runInterpreter();
    assertMsg("basic test",GETLOCAL_W(298).jlong==1279914331);
}

void testLSTORE() {
    registerMethod("TestStoreInstructions.testLSTORE()");
    pc[0] = INSTR_LSTORE;
    pc[1] = 3;
    pc[2] = INSTR_BREAKPOINT;
    PUSH_W((long long)1279914331);
    runInterpreter();
    assertMsg("basic test",GETLOCAL_W(3).jlong==1279914331);
}


void testISTORE_X() {
    registerMethod("TestStoreInstructions.testISTORE_X()");
    pc[0] = INSTR_ISTORE_0;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(1127931);
    runInterpreter();
    assertMsg("testing istore_0",GETLOCAL(0).jint==1127931);
  pc[0] = INSTR_ISTORE_1;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(2127931);
    runInterpreter();
    assertMsg("testing istore_1",GETLOCAL(1).jint==2127931);
  pc[0] = INSTR_ISTORE_2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(3127931);
    runInterpreter();
    assertMsg("testing istore_2",GETLOCAL(2).jint==3127931);
  pc[0] = INSTR_ISTORE_3;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(1275931);
    runInterpreter();
    assertMsg("testing istore_3",GETLOCAL(3).jint==1275931);
}


void testFSTORE_X() {
    registerMethod("TestStoreInstructions.testFSTORE_X()");
    pc[0] = INSTR_FSTORE_0;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)  1.127931);
    runInterpreter();
    assertMsg("testing fstore_0",GETLOCAL(0).jfloat==(float)1.127931);
  pc[0] = INSTR_FSTORE_1;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)  212793.1);
    runInterpreter();
    assertMsg("testing fstore_1",GETLOCAL(1).jfloat==(float)212793.1);
  pc[0] = INSTR_FSTORE_2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)  3127.931);
    runInterpreter();
    assertMsg("testing fstore_2",GETLOCAL(2).jfloat==(float)3127.931);
  pc[0] = INSTR_FSTORE_3;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)  127.5931);
    runInterpreter();
    assertMsg("testing fstore_3",GETLOCAL(3).jfloat==(float)127.5931);
}


void testDSTORE_X() {
    registerMethod("TestStoreInstructions.testDSTORE_X()");
    pc[0] = INSTR_DSTORE_0;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)  1.127931);
    runInterpreter();
    assertMsg("testing dstore_0",GETLOCAL_W(0).jdouble==(double)1.127931);
  pc[0] = INSTR_DSTORE_1;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)  212793.1);
    runInterpreter();
    assertMsg("testing dstore_1",GETLOCAL_W(1).jdouble==(double)212793.1);
  pc[0] = INSTR_DSTORE_2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)  3127.931);
    runInterpreter();
    assertMsg("testing dstore_2",GETLOCAL_W(2).jdouble==(double)3127.931);
  pc[0] = INSTR_DSTORE_3;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)  127.5931);
    runInterpreter();
    assertMsg("testing dstore_3",GETLOCAL_W(3).jdouble==(double)127.5931);
}


void testLSTORE_X() {
    registerMethod("TestStoreInstructions.testLSTORE_X()");
    pc[0] = INSTR_LSTORE_0;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)  1127931);
    runInterpreter();
    assertMsg("testing lstore_0",GETLOCAL_W(0).jlong==(long long)1127931);
  pc[0] = INSTR_LSTORE_1;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)  2127931);
    runInterpreter();
    assertMsg("testing lstore_1",GETLOCAL_W(1).jlong==(long long)2127931);
  pc[0] = INSTR_LSTORE_2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)  3127931);
    runInterpreter();
    assertMsg("testing lstore_2",GETLOCAL_W(2).jlong==(long long)3127931);
  pc[0] = INSTR_LSTORE_3;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)  1275931);
    runInterpreter();
    assertMsg("testing lstore_3",GETLOCAL_W(3).jlong==(long long)1275931);
}


void testASTORE() {
    registerMethod("TestStoreInstructions.testASTORE()");
    pc[0] = INSTR_ASTORE;
    pc[1] = 3;
    pc[2] = INSTR_BREAKPOINT;
    PUSH(127931);
    runInterpreter();
    assertMsg("basic test",GETLOCAL(3).jint==127931);
}


void testASTORE_X() {
    registerMethod("TestStoreInstructions.testASTORE_X()");
    pc[0] = INSTR_ASTORE_0;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(1127931);
    runInterpreter();
    assertEquals("testing astore_0",GETLOCAL(0).jint,1127931);
  pc[0] = INSTR_ASTORE_1;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(2127931);
    runInterpreter();
    assertEquals("testing astore_1",GETLOCAL(1).jint,2127931);
  pc[0] = INSTR_ASTORE_2;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(3127931);
    runInterpreter();
    assertEquals("testing astore_2",GETLOCAL(2).jint,3127931);
  pc[0] = INSTR_ASTORE_3;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(1275931);
    runInterpreter();
    assertEquals("testing astore_3",GETLOCAL(3).jint,1275931);
}


int main() {
  setup();
  testISTORE();
  testWISTORE();
  testWFSTORE();
  testWDSTORE();
  testWLSTORE(); 
  
  testFSTORE();
  testDSTORE();
  testLSTORE();
  testASTORE();
  testDSTORE_X();
  testLSTORE_X();
  testFSTORE_X();
  testISTORE_X();
  testASTORE_X();
  teardown();
  return 0;
}

