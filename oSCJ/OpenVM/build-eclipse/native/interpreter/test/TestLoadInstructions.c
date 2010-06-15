/**
 * This file contains the test code for the 
 * interpreter related to Load instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestLoadInstructions.c
 * @author James Liang
 */ 

#include "CUnit.h"
#include "framework.h"

extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;


void testDLOAD() {
    registerMethod("TestLoadInstructions.testDLOAD()");
    pc[0] = INSTR_DLOAD;
    pc[1] = 3;
    pc[2] = INSTR_BREAKPOINT;
    SETLOCAL_W(3,1279914.331);
    runInterpreter();
    assertMsg("basic test",isEqual(POP_W().jdouble,1279914.331));
}

void testFLOAD() {
    registerMethod("TestLoadInstructions.testFLOAD()");
    pc[0] = INSTR_FLOAD;
    pc[1] = 2;
    pc[2] = INSTR_BREAKPOINT;
    SETLOCAL(2,(float)914.331);
    runInterpreter();
    assertMsg("basic test",isEqual(POP().jfloat,914.331));
}


void testALOAD() {
    registerMethod("TestLoadInstructions.testALOAD()");
    pc[0] = INSTR_ALOAD;
    pc[1] = 2;
    pc[2] = INSTR_BREAKPOINT;
    SETLOCAL(2,12799);
    runInterpreter();
    assertMsg("basic test",POP().jint==12799);
}

void testILOAD() {
    registerMethod("TestLoadInstructions.testILOAD()");
    pc[0] = INSTR_ILOAD;
    pc[1] = 2;
    pc[2] = INSTR_BREAKPOINT;
    SETLOCAL(2,12799);
    runInterpreter();
    assertMsg("basic test",POP().jint==12799);
}


void testLLOAD() {
    registerMethod("TestLoadInstructions.testLLOAD()");
    pc[0] = INSTR_LLOAD;
    pc[1] = 3;
    pc[2] = INSTR_BREAKPOINT;
    SETLOCAL_W(3,(long long)127991433);
    runInterpreter();
    assertMsg("basic test",POP_W().jlong==(long long)127991433);
}


void testDLOAD_X() {
    registerMethod("TestLoadInstructions.testDLOAD_X()");
    pc[0] = INSTR_DLOAD_0;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL_W(0,1279914.331);
    runInterpreter();
    assertMsg("dload0",isEqual(POP_W().jdouble,1279914.331));
    pc[0] = INSTR_DLOAD_1;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL_W(1,12799114.331);
    runInterpreter();
    assertMsg("dload1",isEqual(POP_W().jdouble,12799114.331));
    pc[0] = INSTR_DLOAD_2;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL_W(2,21279914.331);
    runInterpreter();
    assertMsg("dload2",isEqual(POP_W().jdouble,21279914.331));
    pc[0] = INSTR_DLOAD_3;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL_W(3,1279914.3331);
    runInterpreter();
    assertMsg("dload3",isEqual(POP_W().jdouble,1279914.3331));
}


void testILOAD_X() {
    registerMethod("TestLoadInstructions.testILOAD_X()");
    pc[0] = INSTR_ILOAD_0;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(0,12799140);
    runInterpreter();
    assertMsg("iload0",POP().jint==12799140);
    pc[0] = INSTR_ILOAD_1;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(1,127991141);
    runInterpreter();
    assertMsg("iload1",POP().jint==127991141);
    pc[0] = INSTR_ILOAD_2;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(2,212799142);
    runInterpreter();
    assertMsg("iload2",POP().jint==212799142);
    pc[0] = INSTR_ILOAD_3;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(3,12799143);
    runInterpreter();
    assertMsg("iload3",POP().jint==12799143);
}


void testALOAD_X() {
    registerMethod("TestLoadInstructions.testALOAD_X()");
    pc[0] = INSTR_ALOAD_0;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(0,12799140);
    runInterpreter();
    assertMsg("aload0",POP().jint==12799140);
    pc[0] = INSTR_ALOAD_1;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(1,127991141);
    runInterpreter();
    assertMsg("aload1",POP().jint==127991141);
    pc[0] = INSTR_ALOAD_2;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(2,212799142);
    runInterpreter();
    assertMsg("aload2",POP().jint==212799142);
    pc[0] = INSTR_ALOAD_3;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(3,12799143);
    runInterpreter();
    assertMsg("aload3",POP().jint==12799143);
}

void testFLOAD_X() {
    registerMethod("TestLoadInstructions.testFLOAD_X()");
    pc[0] = INSTR_FLOAD_0;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(0,(float)1279.9140);
    runInterpreter();
    assertMsg("fload0",isEqual(POP().jfloat,1279.9140));
    pc[0] = INSTR_FLOAD_1;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(1,(float)1279911.41);
    runInterpreter();
    assertMsg("fload1",isEqual(POP().jfloat,1279911.41));
    pc[0] = INSTR_FLOAD_2;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(2,(float)21279914.2);
    runInterpreter();
    assertMsg("fload2",isEqual(POP().jfloat,21279914.2));
    pc[0] = INSTR_FLOAD_3;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL(3,(float)127.99143);
    runInterpreter();
    assertMsg("fload3",isEqual(POP().jfloat,127.99143));
}


void testLLOAD_X() {
    registerMethod("TestLoadInstructions.testLLOAD_X()");
    pc[0] = INSTR_LLOAD_0;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL_W(0,(long long)10279914331L);
    runInterpreter();
    assertMsg("lload0",isEqual(POP_W().jlong,10279914331L));
    pc[0] = INSTR_LLOAD_1;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL_W(1,(long long)112799114331L);
    runInterpreter();
    assertMsg("lload1",isEqual(POP_W().jlong,112799114331L));
    pc[0] = INSTR_LLOAD_2;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL_W(2,(long long)221279914331L);
    runInterpreter();
    assertMsg("lload2",isEqual(POP_W().jlong,221279914331L));
    pc[0] = INSTR_LLOAD_3;
    pc[1] = INSTR_BREAKPOINT;
    SETLOCAL_W(3,(long long)12799143331L);
    runInterpreter();
    assertMsg("lload3",isEqual(POP_W().jlong,12799143331L));
}


void testWDLOAD() {
    registerMethod("TestLoadInstructions.testWIDE_DLOAD()");
    pc[0] = INSTR_WIDE;
    pc[1] = INSTR_DLOAD;
    *((unsigned short int*)(&pc[2])) = htons(293);
    // pc[1] = 3;
    pc[4] = INSTR_BREAKPOINT;
    SETLOCAL_W(293,1279914.331);
    runInterpreter();
    assertMsg("basic test",isEqual(POP_W().jdouble,1279914.331));
}

void testWFLOAD() {
    registerMethod("TestLoadInstructions.testWIDE_FLOAD()");
    pc[0] = INSTR_WIDE;
    pc[1] = INSTR_FLOAD;
    *((unsigned short int*)(&pc[2])) = htons(291);
    pc[4] = INSTR_BREAKPOINT;
    SETLOCAL(291,(float)914.331);
    runInterpreter();
    assertMsg("basic test",isEqual(POP().jfloat,914.331));
}


void testWALOAD() {
    registerMethod("TestLoadInstructions.testWIDE_ALOAD()");
    pc[0] = INSTR_WIDE;
    pc[1] = INSTR_ALOAD;
    *((unsigned short int*)(&pc[2])) = htons(312);
    pc[4] = INSTR_BREAKPOINT;
    SETLOCAL(312,12799);
    runInterpreter();
    assertMsg("basic test",POP().jint==12799);
}

void testWILOAD() {
    registerMethod("TestLoadInstructions.testWIDE_ILOAD()");
    pc[0] = INSTR_WIDE;
    pc[1] = INSTR_ILOAD;
    *((unsigned short int*)(&pc[2])) = htons(299);
    pc[4] = INSTR_BREAKPOINT;
    SETLOCAL(299,12799);
    runInterpreter();
    assertMsg("basic test",POP().jint==12799);
}


void testWLLOAD() {
    registerMethod("TestLoadInstructions.testWIDE_LLOAD()");
    pc[0] = INSTR_WIDE;
    pc[1] = INSTR_LLOAD;
    *((unsigned short int*)(&pc[2])) = htons(311);
    pc[4] = INSTR_BREAKPOINT;
    SETLOCAL_W(311,(long long)127991433);
    runInterpreter();
    assertMsg("basic test",POP_W().jlong==127991433L);
}


int main() {
  setup();
    testLLOAD();
    testFLOAD();
    testILOAD();
    testDLOAD();
    testALOAD();
    /* Wide instructions*/
    testWLLOAD(); 
    testWFLOAD();
    testWILOAD();
    testWDLOAD();
    testWALOAD(); 
        
    testFLOAD_X();
    testLLOAD_X();
    testILOAD_X();
    testDLOAD_X();
    testALOAD_X();
    teardown();
    return 0;
}

