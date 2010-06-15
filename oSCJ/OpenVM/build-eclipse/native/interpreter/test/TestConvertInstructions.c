/**
 * This file contains the test code for the 
 * interpreter related to Convert instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestConvertInstructions.c
 * @author James Liang
 */ 

#include "CUnit.h"
#include "framework.h"

extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;

void testWideningInstructions() {
    registerMethod("TestConvertInstructions.testNarrowingInstructions()");
    pc[0] = INSTR_I2L;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(1397);
    runInterpreter();assertMsg("test I2L", isEqual(POP_W().jlong,1397));
    pc[0] = INSTR_I2D;
    PUSH(1397);
    runInterpreter();assertMsg("test I2D", isEqual(POP_W().jdouble,1397));
    pc[0] = INSTR_F2L;
    PUSH((float)1397.781);
    runInterpreter();assertMsg("test F2L", isEqual(POP_W().jlong,1397));
    pc[0] = INSTR_F2D;
    PUSH((float)1397.7781);
    runInterpreter();assertMsg("test F2D", isEqual(POP_W().jdouble,1397.7781));
}

void testSameSizeConversionInstructions() {
  registerMethod("TestConvertInstructions.testSameSizeConversionInstructions()");
  /** 32 bit **/
  pc[0] = INSTR_I2F;
  pc[1] = INSTR_BREAKPOINT;
  PUSH(1397);
  runInterpreter();assertMsg("test I2F", isEqual(POP().jfloat,1397));

  pc[0] = INSTR_F2I;
  PUSH((float)1397.971);
  runInterpreter();assertMsg("test F2I", isEqual(POP().jint,1397));
  

  /** 64 bit **/
  pc[0] = INSTR_L2D;
  PUSH_W((long long)139791);
  runInterpreter();assertMsg("test L2D", isEqual(POP_W().jdouble,139791));
  pc[0] = INSTR_D2L;
  PUSH_W(139791.112);
  runInterpreter();assertMsg("test D2L", isEqual(POP_W().jlong,139791));

}
void testNarrowingInstructions() {
  registerMethod("TestConvertInstructions.testNarrowingInstructions()");
  pc[0] = INSTR_D2F;
  pc[1] = INSTR_BREAKPOINT;
  PUSH_W((double)1397.12);
  runInterpreter();assertMsg("test D2F", isEqual(POP().jfloat,1397.12));
  pc[0] = INSTR_L2I;
  PUSH_W((long long)139712L);
  runInterpreter();assertMsg("test L2I", isEqual(POP().jint,139712));
  pc[0] = INSTR_L2F;
  PUSH_W((long long )139712L);
  runInterpreter();assertMsg("test L2F", isEqual(POP().jfloat,139712));
  pc[0] = INSTR_D2I;
  PUSH_W((double)1397.12);
  runInterpreter();assertMsg("test D2I", isEqual(POP().jint,1397));  
  pc[0] = INSTR_I2B;
  PUSH(1397);
  runInterpreter();assertMsg("test I2B", isEqual((jbyte)POP().jint,117));
  pc[0] = INSTR_I2C;
  PUSH(59781397 );
  runInterpreter();assertMsg("test I2C", isEqual((jchar)POP().jint,12565));
  pc[0] = INSTR_I2S;
  PUSH(5781397);
  runInterpreter();assertMsg("test I2S",isEqual((jshort)POP().jint,14229));
}

int main() {
  setup();
  testNarrowingInstructions();
  testWideningInstructions();
  testSameSizeConversionInstructions();  
  teardown();
  return 0;
}
