/**
 * This file contains the test code for the 
 * interpreter related to arithmetic instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestArithmeticInstructions.c
 * @author James Liang
 */ 

#include "CUnit.h"
#include "framework.h"
#include <math.h>

extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;
extern NativeContext* current_context;

/**
 * See documentation in file header.
 */
void testIINC() {
    registerMethod("TestArithmeticInstructions.testIINC()");
    pc[0] = INSTR_IINC; mark("??");
    pc[1] = 1; mark("??");
    pc[2] = 3; mark("??");
    pc[3] = INSTR_BREAKPOINT; mark("??");
    SETLOCAL(1,7); mark("??");
    mark("??");
    runInterpreter();//,0,0);
    assertEquals("basic test",GETLOCAL(1).jint,10);
}

void testWIINC() {
    registerMethod("TestArithmeticInstructions.testWIDE_IINC()");
    pc[0] = INSTR_WIDE;
    pc[1] = INSTR_IINC;
    *((unsigned short int*)(&pc[2])) = htons(8);
    *((unsigned short int*)(&pc[4])) = htons(1792);
    pc[6] = INSTR_BREAKPOINT;
    SETLOCAL(8,14);
    runInterpreter();//,0,0);
    assertEquals("basic test",GETLOCAL(8).jint , 1806);
}

void testIADD() {
    registerMethod("TestArithmeticInstructions.testIADD()");
    pc[0] = INSTR_IADD;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(100);
    PUSH(127);
    runInterpreter();//,0,0);
    assertEquals("basic test",POP().jint,227);
}

void testIAND() {
    registerMethod("TestArithmeticInstructions.testIAND()");
    pc[0] = INSTR_IAND;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(1);
    PUSH(1);
    runInterpreter();//,0,0);
    assertEquals("basic test11",POP().jint,1);
    PUSH(0);
    PUSH(1);
    runInterpreter();//,0,0);
    assertEquals("basic test01",POP().jint,0);
    PUSH(1);
    PUSH(0);
    runInterpreter();//,0,0);
    assertEquals("basic test10",POP().jint,0);
    PUSH(0);
    PUSH(0);
    runInterpreter();//,0,0);
    assertEquals("basic test00",POP().jint,0);
}

void testFADD() {
    registerMethod("TestArithmeticInstructions.testFADD()");
    pc[0] = INSTR_FADD;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)100.3);
    PUSH((float)127.4);
    runInterpreter();//,0,0);
    assertMsg("basic test",isEqual(POP().jfloat,(float)227.7));
}


void testDADD() {
    registerMethod("TestArithmeticInstructions.testDADD()");
    pc[0] = INSTR_DADD;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)100.1);
    PUSH_W((double)127.127);
    runInterpreter();//,0,0);
    assertMsg("basic test",isEqual(POP_W().jdouble,(double)227.227));
}


void testLADD() {
    registerMethod("TestArithmeticInstructions.testLADD()");
    pc[0] = INSTR_LADD;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)100);
    PUSH_W((long long)127);
    runInterpreter();//,0,0);
    assertMsg("basic test",POP_W().jlong==(long long)227);
}




void testISUB() {
    registerMethod("TestArithmeticInstructions.testISUB()");
    pc[0] = INSTR_ISUB;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(100);
    PUSH(127);
    runInterpreter();//,0,0);
    assertEquals("basic test",POP().jint,-27);
}


void testFSUB() {
    registerMethod("TestArithmeticInstructions.testFSUB()");
    pc[0] = INSTR_FSUB;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)100.3);
    PUSH((float)127.4);
    runInterpreter();//,0,0);
      assertMsg("basic test",isEqual(POP().jfloat,(float)-27.1));
}


void testDSUB() {
    registerMethod("TestArithmeticInstructions.testDSUB()");
    pc[0] = INSTR_DSUB;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)100.1);
    PUSH_W((double)127.127);
    runInterpreter();//,0,0);
    assertMsg("basic test",isEqual(POP_W().jdouble,(double)-27.027));
}


void testLSUB() {
    registerMethod("TestArithmeticInstructions.testLSUB()");
    pc[0] = INSTR_LSUB;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)100);
    PUSH_W((long long)127);
    runInterpreter();//,0,0);
    assertMsg("basic test",POP_W().jlong==(long long)-27);
}



void testIMUL() {
    registerMethod("TestArithmeticInstructions.testIMUL()");
    pc[0] = INSTR_IMUL;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(100);
    PUSH(127);
    mark("Going to run_interpreter");
    runInterpreter();//,0,0);
    mark("Finished run_interpreterning");
    assertEquals("basic test",POP().jint,12700);
}


void testFMUL() {
    registerMethod("TestArithmeticInstructions.testFMUL()");
    pc[0] = INSTR_FMUL;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)100.3);
    PUSH((float)127.4);
    runInterpreter();//,0,0);
    assertMsg("basic test",isEqual(POP().jfloat,(float)12778.22));
}


void testDMUL() {
    registerMethod("TestArithmeticInstructions.testDMUL()");
    pc[0] = INSTR_DMUL;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)100.1);
    PUSH_W((double)127.127);
    runInterpreter();//,0,0);
    assertMsg("basic test",isEqual(POP_W().jdouble,(double)12725.413));
}


void testLMUL() {
    registerMethod("TestArithmeticInstructions.testLMUL()");
    pc[0] = INSTR_LMUL;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)100);
    PUSH_W((long long)127);
    runInterpreter();//,0,0);
    assertMsg("basic test",POP_W().jlong==(long long)12700);
}


void testIDIV() {
    registerMethod("TestArithmeticInstructions.testIDIV()");
    pc[0] = INSTR_IDIV;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(100);
    PUSH(127);
    runInterpreter();//,0,0);
    assertEquals("basic test",POP().jint,0);
}


void testFDIV() {
    registerMethod("TestArithmeticInstructions.testFDIV()");
    pc[0] = INSTR_FDIV;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)100.3);
    PUSH((float)127.4);
    runInterpreter();//,0,0);
    assertMsg("basic test",isEqual(POP().jfloat,(float)0.78728414));
}


void testDDIV() {
    registerMethod("TestArithmeticInstructions.testDDIV()");
    pc[0] = INSTR_DDIV;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)100.1);
    PUSH_W((double)127.127);
    runInterpreter();//,0,0);
    assertMsg("basic test",isEqual(POP_W().jdouble,(double)0.78740157));
}


void testLDIV() {
    registerMethod("TestArithmeticInstructions.testLDIV()");
    pc[0] = INSTR_LDIV;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)100);
    PUSH_W((long long)1);
    runInterpreter();//,0,0);
    assertMsg("basic test",POP_W().jlong==(long long)100);
}


void testLAND() {
    registerMethod("TestArithmeticInstructions.testLAND()");
    pc[0] = INSTR_LAND;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)1);
    PUSH_W((long long)1);
    runInterpreter();//,0,0);
    assertMsg("basic test11",POP_W().jlong==(long long)1);
    PUSH_W((long long)0);
    PUSH_W((long long)1);
    runInterpreter();//,0,0);
    assertMsg("basic test01",POP_W().jlong==(long long)0);
    PUSH_W((long long)1);
    PUSH_W((long long)0);
    runInterpreter();//,0,0);
    assertMsg("basic test10",POP_W().jlong==(long long)0);
    PUSH_W((long long)0);
    PUSH_W((long long)0);
    runInterpreter();//,0,0);
    assertMsg("basic test00",POP_W().jlong==(long long)0);
}


void testIOR() {
    registerMethod("TestArithmeticInstructions.testIOR()");
    pc[0] = INSTR_IOR;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(1);
    PUSH(1);
    runInterpreter();//,0,0);
    assertEquals("basic test11",POP().jint,1);
    PUSH(0);
    PUSH(1);
    runInterpreter();//,0,0);
    assertEquals("basic test01",POP().jint,1);
    PUSH(1);
    PUSH(0);
    runInterpreter();//,0,0);
    assertEquals("basic test10",POP().jint,1);
    PUSH(0);
    PUSH(0);
    runInterpreter();//,0,0);
    assertEquals("basic test00",POP().jint,0);
}


void testIXOR() {
    registerMethod("TestArithmeticInstructions.testIXOR()");
    pc[0] = INSTR_IXOR;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(1);
    PUSH(1);
    runInterpreter();//,0,0);
    assertEquals("basic test11",POP().jint,0);
    PUSH(0);
    PUSH(1);
    runInterpreter();//,0,0);
    assertEquals("basic test01",POP().jint,1);
    PUSH(1);
    PUSH(0);
    runInterpreter();//,0,0);
    assertEquals("basic test10",POP().jint,1);
    PUSH(0);
    PUSH(0);
    runInterpreter();//,0,0);
    assertEquals("basic test00",POP().jint,0);
}


void testLOR() {
    registerMethod("TestArithmeticInstructions.testLOR()");
    pc[0] = INSTR_LOR;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)1);
    PUSH_W((long long)1);
    runInterpreter();//,0,0);
    assertMsg("basic test11",POP_W().jlong==(long long)1);
    PUSH_W((long long)0);
    PUSH_W((long long)1);
    runInterpreter();//,0,0);
    assertMsg("basic test01",POP_W().jlong==(long long)1);
    PUSH_W((long long)1);
    PUSH_W((long long)0);
    runInterpreter();//,0,0);
    assertMsg("basic test10",POP_W().jlong==(long long)1);
    PUSH_W((long long)0);
    PUSH_W((long long)0);
    runInterpreter();//,0,0);
    assertMsg("basic test00",POP_W().jlong==(long long)0);
}


void testLXOR() {
    registerMethod("TestArithmeticInstructions.testLXOR()");
    pc[0] = INSTR_LXOR;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)1);
    PUSH_W((long long)1);
    runInterpreter();//,0,0);
    assertMsg("basic test11",POP_W().jlong==(long long)0);
    PUSH_W((long long)0);
    PUSH_W((long long)1);
    runInterpreter();//,0,0);
    assertMsg("basic test01",POP_W().jlong==(long long)1);
    PUSH_W((long long)1);
    PUSH_W((long long)0);
    runInterpreter();//,0,0);
    assertMsg("basic test10",POP_W().jlong==(long long)1);
    PUSH_W((long long)0);
    PUSH_W((long long)0);
    runInterpreter();//,0,0);
    assertMsg("basic test00",POP_W().jlong==(long long)0);
}


void testINEG() {
    registerMethod("TestArithmeticInstructions.testINEG()");
    pc[0] = INSTR_INEG;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(127);
    runInterpreter();//,0,0);
    assertEquals("basic test",POP().jint,-127);
}


void testFNEG() {
    registerMethod("TestArithmeticInstructions.testFNEG()");
    pc[0] = INSTR_FNEG;
    pc[1] = INSTR_BREAKPOINT;
    PUSH((float)12.7);
    runInterpreter();//,0,0);
    assertMsg("basic test",POP().jfloat==(float)-12.7);
}


void testLNEG() {
    registerMethod("TestArithmeticInstructions.testLNEG()");
    pc[0] = INSTR_LNEG;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)199927);
    runInterpreter();//,0,0);
    assertMsg("basic test",POP_W().jlong==(long long)-199927);
}


void testDNEG() {
    registerMethod("TestArithmeticInstructions.testDNEG()");
    pc[0] = INSTR_DNEG;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)127.145663);
    runInterpreter();//,0,0);
    assertMsg("basic test",POP_W().jdouble==(double)-127.145663);
}


void testIREM() {
  registerMethod("TestArithmeticInstructions.testIREM()");
    pc[0] = INSTR_IREM;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(11);
    PUSH(7);
    runInterpreter();//,0,0);
    assertEquals("basic test",POP().jint,4);
}


void testLREM() {
  registerMethod("TestArithmeticInstructions.testLREM()");
    pc[0] = INSTR_LREM;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((long long)11);
    PUSH_W((long long)7);
    runInterpreter();//,0,0);
    assertMsg("basic test",POP_W().jlong==(long long)4);
}


void testDREM() {
  registerMethod("TestArithmeticInstructions.testDREM()");
    pc[0] = INSTR_DREM;
    pc[1] = INSTR_BREAKPOINT;
    PUSH_W((double)11.00);
    PUSH_W((double)7.00);
    runInterpreter();//,0,0);
    assertMsg("basic test",isEqual(POP_W().jdouble,(double)4));
}



void testFREM() {
  
  registerMethod("TestArithmeticInstructions.testFREM()");
  pc[0] = INSTR_FREM;
  pc[1] = INSTR_BREAKPOINT;
  PUSH((float)11.0);
  PUSH((float)7.0);
  runInterpreter();//,0,0);
  assertMsg("basic test",isEqual(POP().jfloat,4));
}

void testISHR() {
  registerMethod("TestArithmeticInstructions.testISHR()");
  pc[0] = INSTR_ISHR;
  pc[1] = INSTR_BREAKPOINT;
  PUSH(54326);
  PUSH(34); //100010 in binary.  The low
  // five bits equal 2

  runInterpreter();//,0,0);
  assertMsg("basic test",isEqual(54326.0/pow(2.0,34&0x1f),
			      POP().jint));
}

void testISHL() {
  registerMethod("TestArithmeticInstructions.testISHL()");
  pc[0] = INSTR_ISHL;
  pc[1] = INSTR_BREAKPOINT;
  PUSH(1); 
  PUSH(39); // lower bits represent 7
  runInterpreter();//,0,0);
  assertEquals("basic test",128,POP().jint);
}

void testLSHR() {
  registerMethod("TestArithmeticInstructions.testLSHR()");
  pc[0] = INSTR_LSHR;
  pc[1] = INSTR_BREAKPOINT;
  PUSH_W((long long)11254326); 
  PUSH(68); //1000100 in binary.  The low
  // 6 bits represents 4
 
  runInterpreter();//,0,0);
  assertMsg("basic test",isEqual(11254326.0/pow(2.0,68&0x3f),
			      POP_W().jlong));
}
void testLUSHR() {
  int s;
  registerMethod("TestArithmeticInstructions.testLUSHR()");
  pc[0] = INSTR_LUSHR;
  pc[1] = INSTR_BREAKPOINT;
  PUSH_W((long long)11254326); 
  PUSH(68); //1000100 in binary.  The low
  // 6 bits represents 4
 
  runInterpreter();//,0,0);
  assertMsg("positive test",isEqual((11254326L>>(long long)(68&0x3f)),
			      POP_W().jlong));
  
   PUSH_W((long long)-11254326); 
   PUSH(68); //1000100 in binary.  The low
  // 6 bits represents 4
   runInterpreter();//,0,0);
   s = 68 & 0x3f;
   assertMsg("positive test",
	     ( (-11254326 >> s)+ (2L<<~(s)) ) ==
  			      POP_W().jlong);
}

void testIUSHR() {
  int s;
  registerMethod("TestArithmeticInstructions.testIUSHR()");
  pc[0] = INSTR_IUSHR;
  pc[1] = INSTR_BREAKPOINT;
  PUSH(54326);
  PUSH(34); //100010 in binary.  The low
  // five bits equal 2

  runInterpreter();//,0,0);
  assertMsg("positive test",isEqual((54326>>(34&0x1f)),POP().jint));
  PUSH(-151326);
  PUSH(134); //100010 in binary.  The low
  // five bits equal 2

  runInterpreter();//,0,0);
  s = 134&0x1f;
  assertEquals("negative test",POP().jint,
	       (-151326>>s)+
	       (2<<~s));
}


void testLSHL() {
  registerMethod("TestArithmeticInstructions.testLSHL()");
  pc[0] = INSTR_LSHL;
  pc[1] = INSTR_BREAKPOINT;
  PUSH_W((long long)4); 
  PUSH(97);// lower 6 bits represent 33
  runInterpreter();//,0,0);
  pc[0] = INSTR_LSHR; // shift it back
  PUSH(97);// lower 6 bits represent 33
  runInterpreter();//,0,0);
  assertMsg("basic test",4L==POP_W().jlong);
 
}

int main() {
  setup();
  testIINC();
  testIADD();
  testFADD();
  testDADD();
  testLADD();
  testISUB();
  testFSUB();
  testDSUB();
  testLSUB();
  testIMUL();
  testFMUL();
  testDMUL();
  testLMUL();
  testIDIV();
  testFDIV();
  testDDIV();
  testLDIV();
  testIAND();
  testLAND();
  testIOR();
  testLOR();
  testIXOR();
  testLOR();
  testLXOR();
  testINEG();
  testFNEG();
  testDNEG();
  testLNEG();
  testIREM();
  testFREM();
  testLREM();
  testDREM();
  testISHR(); 
  testIUSHR();
  testISHL();
  testLSHR();  
  testLUSHR();
  testLSHL();

  /**** Wide **/
  testWIINC(); 

  teardown();
  return 0;
}



