/**
 * This file contains the test code for the 
 * interpreter related to method invocation and return instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * @file src/interpreter/test/TestMethodInstructions.c
 * @author James Liang
 */ 
#include <CUnit.h>
#include "framework.h"

extern jvalue*  local_vars;
extern jvalue*  stack_top;
extern byte*    pc;
extern NativeContext* current_context;
CodeFragment*   methodFragment;
byte*           otherPC;

#define MAKEFRAME() create_frame(current_context,methodFragment,NULL,&local_vars,&stack_top,&pc,0)

void testRETURN() {
  byte* oldBC = pc;
  registerMethod("testMethodInstructions.testRETURN");
  PUSH(15);
  PUSH(19);
  pc[0]   = INSTR_IADD;
  pc[1]   = INSTR_BREAKPOINT;
  otherPC = (byte*)(&(methodFragment->code_->values));
  otherPC[0] = INSTR_RETURN;
  MAKEFRAME();
  runInterpreter();
  assertEquals("Testing that we return to the correct place",
	       34, POP().jint);
  pc = oldBC;
}

void testIRETURN() {
  byte* oldBC = pc;
  registerMethod("testMethodInstructions.testIRETURN");
  pc[0] = INSTR_BREAKPOINT;
  otherPC = (byte*)(&(methodFragment->code_->values));
  otherPC[0] = INSTR_ICONST_5;
  otherPC[1] = INSTR_ICONST_4;
  otherPC[2] = INSTR_IMUL;
  otherPC[3] = INSTR_IRETURN;
  MAKEFRAME();
  runInterpreter();
  assertEquals("testing",POP().jint,20);
  pc = oldBC;
}

void testFRETURN() {
  byte* oldBC = pc;
  registerMethod("testMethodInstructions.testFRETURN");
  pc[0] = INSTR_BREAKPOINT;
  otherPC = (byte*)(&(methodFragment->code_->values));
  otherPC[0] = INSTR_FCONST_1;
  otherPC[1] = INSTR_FCONST_2;
  otherPC[2] = INSTR_FDIV;
  otherPC[3] = INSTR_FRETURN;
  MAKEFRAME();
  runInterpreter();
  assertMsg("testing",isEqual(POP().jfloat,.5));
  pc = oldBC;
}

void testDRETURN() {
  byte* oldBC = pc;
  registerMethod("testMethodInstructions.testDRETURN");
  pc[0] = INSTR_BREAKPOINT;
  otherPC = (byte*)(&(methodFragment->code_->values));
  otherPC[0] = INSTR_DCONST_1;
  otherPC[1] = INSTR_DCONST_1;
  otherPC[2] = INSTR_DDIV;
  otherPC[3] = INSTR_DRETURN;
  MAKEFRAME();
  runInterpreter();
  assertMsg("testing",isEqual(POP_W().jdouble,1));
  pc = oldBC;
}

void testLRETURN() {
  byte* oldBC = pc; 
  registerMethod("testMethodInstructions.testLRETURN");
  pc[0] = INSTR_BREAKPOINT;
  otherPC = (byte*)(&(methodFragment->code_->values));
  otherPC[0] = INSTR_LCONST_1;
  otherPC[1] = INSTR_LCONST_1;
  otherPC[2] = INSTR_LADD;
  otherPC[3] = INSTR_LRETURN;
  MAKEFRAME();
  runInterpreter();
  assertMsg("testing", isEqual(POP_W().jlong, 2));
  pc = oldBC;
}

void testARETURN() {
  byte* oldBC = pc;
  registerMethod("testMethodInstructions.testARETURN");
  pc[0] = INSTR_BREAKPOINT;
  otherPC = (byte*)(&(methodFragment->code_->values));
  otherPC[0] = INSTR_ICONST_5;
  otherPC[1] = INSTR_ICONST_4;
  otherPC[2] = INSTR_IMUL;
  otherPC[3] = INSTR_ARETURN;
  MAKEFRAME();
  runInterpreter();
  assertEquals("testing",POP().jint,20);
  //  printf("PC is %d\n",pc);
  pc = oldBC;
}

int main() {
  struct arr_jbyte * code__ = (struct arr_jbyte *)
    malloc(sizeof(struct arr_jbyte)+10);
  code__->length = 10;
  methodFragment = (CodeFragment *)malloc(sizeof(CodeFragment));
  methodFragment->code_ = code__;
  methodFragment->maxStack_ = 18;
  methodFragment->maxLocals_ = 18;
  
  setup();
  testRETURN();
  testIRETURN();
  testFRETURN();
  testDRETURN();
  testLRETURN();
  testARETURN();
  teardown();
  return 0;
}

