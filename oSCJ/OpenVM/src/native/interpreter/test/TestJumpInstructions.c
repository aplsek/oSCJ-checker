/**
 * This file contains the test code for the 
 * interpreter related to Jump instructions
 *
 * All of the methods in this file that begin with the word 
 * test take no parameters and return void.  The second part
 * of the name designates the instruction that is being tested
 * and that instruction is tested using CUnit. 
 *
 * There was really a limit to how much CUnit tests would
 * be able to fully determine the correctness of the conditional
 * branches.  Each instruction was tested to see when
 * the branch would and would not occur.  Closer examination of
 * the actual instruction implementation would be more efficient
 * than exhaustive testing of the possible inputs to the 
 * conditional jumps
 *
 * @file src/interpreter/test/TestJumpInstructions.c
 * @author James Liang
 *
 *
 */ 

#include "CUnit.h"
#include "framework.h"
#include "globals.h"
#include <math.h>

extern jvalue* local_vars;
extern jvalue* stack_top;
extern byte * pc;

void testGOTO() {
    int i;
    registerMethod("TestJumpInstructions.testGOTO()");
    for (i = 3;i<30;i++) 
        pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_GOTO;
    *((signed short int*)(&pc[3])) = htons(10);
       pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10);
    PUSH(20);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("Simple test",POP().jint,30);
}

void testLOOKUPSWITCH() {
  int i;
  byte * oldPC;
  registerMethod("TestJumpInstructions.testLOOKUPSWITCH");
  for (i = 0;i<50;i++) 
    pc[i] = INSTR_BREAKPOINT;
  pc[0] = INSTR_IADD; // this is the instruction we will be attempting to jump to the second time
  PUSH(14); // parameters for the IADD
  PUSH(23);
  pc[3] = INSTR_LOOKUPSWITCH;
  *((signed int*)(&pc[4])) = htonl(37); /* default */
  *((signed int*)(&pc[8])) = htonl(3);  /* npairs */
  *((signed int*)(&pc[12])) = htonl(3); /* match1 */
  *((signed int*)(&pc[16])) = htonl(-34325); /* offset1 */
  *((signed int*)(&pc[20])) = htonl(6488); /* match2*/
  *((signed int*)(&pc[24])) = htonl(-3); /* offset2*/
  *((signed int*)(&pc[28])) = htonl(99927); /* match3*/
  *((signed int*)(&pc[32])) = htonl(693); /* offset3*/
  pc[40] = INSTR_ISUB; // this is the instruction we will be attempting to jump to the first time
  PUSH(6488); // on the second execution,we will use this match
  PUSH(19); // parameters for the ISUB
  PUSH(24);
  PUSH(69); // this match will not be found
  /** tweaking staring position**/
  oldPC = pc;
  pc = &(pc[3]);//-----  *getPcOfFrame(frame)=&(pc[3]); // will start execution here
  
  runInterpreter();//(frame,&area,0,0);
  assertEquals("using default will jump to subtraction",POP().jint,-5);
  mark("Passed the first lookupswitch test");
  pc = &(oldPC[3]);//-----  *getPcOfFrame(frame)=&(pc[3]); // will start execution here
  runInterpreter();//(frame,&area,0,0);
  assertEquals("using offset pair 2 will jump to addition",POP().jint,37);
  /** resetting starting instruction to normal **/

  pc = oldPC;//---------  *getPcOfFrame(frame)=pc;
}

void testTABLESWITCH() {
  int i;
  byte * oldBC= pc;
  registerMethod("TestJumpInstructions.testTABLESWITCH");
  //  failure("NOT YET IMPLEMENTED: will fix testcase when instruction has been implemented\n");
  //  return;
  for (i = 0;i<50;i++) 
    pc[i] = INSTR_BREAKPOINT;
  pc[0] = INSTR_IADD; // this is the instruction we will be attempting to jump to the second time
  PUSH(14); // parameters for the IADD
  PUSH(23);
  pc[3] = INSTR_TABLESWITCH;
  *((signed int*)(&pc[4])) = htonl(37); /* default*/
  *((signed int*)(&pc[8])) = htonl(1);  /* low*/
  *((signed int*)(&pc[12])) = htonl(3); /* high*/
  *((signed int*)(&pc[16])) = htonl(-34325); /* My jump table*/
  *((signed int*)(&pc[20])) = htonl(-3); /* My jump table*/
  *((signed int*)(&pc[24])) = htonl(697633); /* My jump table*/
  pc[40] = INSTR_ISUB; // this is the instruction we will be attempting to jump to the first time
  PUSH(2); // on the second execution, this will go to index - low or 2-1 = 1 
  PUSH(19); // parameters for the ISUB
  PUSH(24);
  PUSH(69); // this will cause it to jump to default
  /** tweaking staring position**/

  pc = &(pc[3]);//-------  *getPcOfFrame(frame)=&(pc[3]); // will start execution here
  
  runInterpreter();//(frame,&area,0,0);
  assertEquals("using default will jump to subtraction",POP().jint,-5);
  pc = &(oldBC[3]);
  runInterpreter();//(frame,&area,0,0);
  assertEquals("using jump table will jump to addition",POP().jint,37);
  /** resetting starting instruction to normal **/
  pc = oldBC;//----  *getPcOfFrame(frame)=pc;
}

void testGOTO_backwards() {
  int i;
  byte * oldBC = pc;
  registerMethod("TestJumpInstructions.testGOTO_backwards()");
  for (i = 0;i<30;i++) 
    pc[i] = INSTR_BREAKPOINT;
  pc[10] = INSTR_GOTO;
  *((signed short int*)(&pc[11])) = htons(-10); 
  //    pc[11] = 0;
  // pc[12] = -10; // parameters of GOTO
  pc[0] = INSTR_IADD;
  pc[1] = INSTR_BREAKPOINT;
  PUSH(10);
  PUSH(20);
  /** tweaking staring position**/
  pc = &(pc[10]);//*getPcOfFrame(frame)=&(pc[10]);
  runInterpreter();//(frame,&area,0,0);
  assertEquals("test jumping backwards",POP().jint,30);
  /** resetting starting instruction to normal **/
  pc = oldBC;//*getPcOfFrame(frame)=pc;
}

void testJSR_W() {
 int i;
  registerMethod("TestJumpInstructions.testJSR()_W");
  for (i = 3;i<66500;i++) 
	pc[i] = INSTR_BREAKPOINT;
  pc[0] = INSTR_NOP;
  pc[1] = INSTR_NOP;
  pc[2] = INSTR_JSR_W;
  *((signed int*)(&pc[3])) =  htonl(65541);
  pc[65543] = INSTR_IADD;
  PUSH(10);

  /** Weird Test 
      The program will jump to the add instruction and add the
      return address to 10.  If either the pushed return address or
      the jumping is wrong, the result will show in my test.
  **/
  runInterpreter();//(frame,&area,0,0);
  // printf("Expected %d and got %d.\n",17+*getPcOfFrame(frame),POP().jint);
  assertEquals("Jumped position",POP().jint,(int)(17+pc));
						//*getPcOfFrame(frame)));
}

void testJSR() {
  int i;
  registerMethod("TestJumpInstructions.testJSR()");
  for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
  pc[0] = INSTR_NOP;
  pc[1] = INSTR_NOP;
  pc[2] = INSTR_JSR;
  *((signed short int*)(&pc[3])) = htons(10);

  pc[12] = INSTR_IADD;
  pc[13] = INSTR_BREAKPOINT;
  PUSH(10);

  /** Weird Test 
      The program will jump to the add instruction and add the
      return address to 10.  If either the pushed return address or
      the jumping is wrong, the result will show in my test.
  **/

  runInterpreter();//(frame,&area,0,0);
  assertEquals("Jumped position",POP().jint,(int)(15+pc));
						//*getPcOfFrame(frame)));
}


void testGOTO_W() {
    int i;
    registerMethod("TestJumpInstructions.testGOTO_W()");
    for (i = 3;i<66500;i++) 
	pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_GOTO_W;
    *((signed int*)(&pc[3])) = htonl(65541);
    //    pc[3] = 0; /* parameter of INSTR_GOTO_W */
    // pc[4] = 1; /*1 parameter of INSTR_GOTO_W */
    // pc[5] = 0; /* parameter of INSTR_GOTO_W */
    //pc[6] = 5; /*5 parameter of INSTR_GOTO_W */
    pc[65543] = INSTR_IADD;
    PUSH(13);
    PUSH(29);
    runInterpreter();//(frame,&area,0,0);
    mark("Finished executing");
    assertEquals("Simple test",POP().jint,42);
}


void testIFEQ() {
    int i;
    registerMethod("TestJumpInstructions.testIFEQ()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IFEQ;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(0); // branch that will jump to the add
    PUSH(122);// arbitrary number
    PUSH(1); // branch fail that will exit
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30);
}


void testIFNE() {
    int i;
    registerMethod("TestJumpInstructions.testIFNE()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IFNE;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(1); // branch that will jump to the add
    PUSH(122);// arbitrary number
    PUSH(0); // branch fail that will exit
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30);
}


void testIFLT() {
    int i;
    registerMethod("TestJumpInstructions.testIFLT()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IFLT;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(-1); // branch that will jump to the add
    PUSH(122);// arbitrary number
    PUSH(1); // branch fail that will exit
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30);
}


void testIFLE() {
    int i;
    registerMethod("TestJumpInstructions.testIFLE()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IFLE;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(0); // branch that will jump to the add
    PUSH(40); // this and the next number will be added
    PUSH(20); 
    PUSH(-7); // branch that will jump to the add
    PUSH(122);// arbitrary number
    PUSH(2); // branch fail that will exit
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,60); // less than zero branch
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30); // equal to zero branch
}


void testIFGT() {
    int i;
    registerMethod("TestJumpInstructions.testIFGT()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IFGT;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(1); // branch that will jump to the add
    PUSH(122);// arbitrary number
    PUSH(-1); // branch fail that will exit
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30);
}


void testIFGE() {
    int i;
    registerMethod("TestJumpInstructions.testIFGE()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IFGE;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(0); // branch that will jump to the add
    PUSH(40); // this and the next number will be added
    PUSH(20); 
    PUSH(7); // branch that will jump to the add
    PUSH(122);// arbitrary number
    PUSH(-2); // branch fail that will exit
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,60); // less than zero branch
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30); // equal to zero branch
}


void testIFNULL() {
    int i;
    registerMethod("TestJumpInstructions.testIFNULL()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IFNULL;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(0); // branch that will jump to the add
    PUSH(122);// arbitrary number
    PUSH(1); // branch fail that will exit
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30);
}

void testIFNONNULL() {
    int i;
    registerMethod("TestJumpInstructions.testIFNONNULL()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IFNONNULL;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(4324); // some arbitrary non-null
    PUSH(122);// arbitrary number
    PUSH(0); // branch fail that will exit
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30);
}


void testIF_ACMPEQ() {
    int i;
    registerMethod("TestJumpInstructions.testIFEQ()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IF_ACMPEQ;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(4324); // these are equal and will jump
    PUSH(4324);
    PUSH(122);// arbitrary number
    PUSH(11); // these are not equal fail and
    PUSH(10); // branch will fail 
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30);
}

void testIF_ACMPNE() {
    int i;
    registerMethod("TestJumpInstructions.testIF_ACMPNE()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IF_ACMPNE;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(434); // these are not equal and will jump
    PUSH(4324);
    PUSH(122);// arbitrary number
    PUSH(11); // these are same  fail and
    PUSH(11); // branch will fail 
    runInterpreter();//(frame,&area,0,0);
    assertEquals("false test",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test true",POP().jint,30);
}

void testIF_ICMPEQ() {
    int i;
    registerMethod("TestJumpInstructions.testIF_ICMPEQ()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IF_ICMPEQ;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(124); // these are equal and will branch
    PUSH(124);
    PUSH(40); // some arbitrary number
    PUSH(20); // this is greater than the next number 
    PUSH(-7); 
    PUSH(122);// arbitrary number
    PUSH(2); // less than the next number
    PUSH(6); // and will not branch
    runInterpreter();//(frame,&area,0,0);
    assertEquals("less than(won't jump)",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("greater than(won't jump)",POP().jint,40);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("equal to(will jump)",POP().jint,30); 
}

void testIF_ICMPGT() {
    int i;
    registerMethod("TestJumpInstructions.testIF_ICMPGT()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IF_ICMPGT;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(124); // these are equal and will branch
    PUSH(123);
    PUSH(40); // some arbitrary number
    PUSH(10); 
    PUSH(10); 
    PUSH(122);// arbitrary number
    PUSH(2); 
    PUSH(6); 
    runInterpreter();//(frame,&area,0,0);
    assertEquals("less than(won't jump)",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("equal to (won't jump)",POP().jint,40);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("greater than(will jump)",POP().jint,30); 
}



void testIF_ICMPLT() {
    int i;
    registerMethod("TestJumpInstructions.testIF_ICMPLT()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IF_ICMPLT;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(14); 
    PUSH(124);
    PUSH(40); // some arbitrary number
    PUSH(-7); 
    PUSH(-7); 
    PUSH(122);// arbitrary number
    PUSH(22); // less than the next number
    PUSH(6); // and will not branch
    runInterpreter();//(frame,&area,0,0);
    assertEquals("greater than(won't jump)",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("equal to(won't jump)",POP().jint,40);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("less than(will jump)",POP().jint,30); 
}

void testIF_ICMPNE() {
    int i;
    registerMethod("TestJumpInstructions.testIF_ICMPNE()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IF_ICMPNE;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(12); // value1 < value2 and will branch
    PUSH(124);
    PUSH(40); // This and next number will be added
    PUSH(94);
    PUSH(20); // value1 > value2 and will branch  
    PUSH(-7); 
    PUSH(122);// arbitrary number
    PUSH(6); // value1 == value2 and will NOT branch
    PUSH(6); // 
    runInterpreter();//(frame,&area,0,0);
    assertEquals("equal to(won't jump)",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("greater than(will jump)",POP().jint,134);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("less than(will jump)",POP().jint,30); 
}


void testIF_ICMPGE() {
    int i;
    registerMethod("TestJumpInstructions.testIF_ICMPGE()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IF_ICMPGE;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(121); // value1 > value2 and will branch
    PUSH(12);
    PUSH(40); // This and next number will be added
    PUSH(94);
    PUSH(20); // value1 > value2 and will branch  
    PUSH(20); 
    PUSH(122);// arbitrary number
    PUSH(1); 
    PUSH(6); 
    runInterpreter();//(frame,&area,0,0);
    assertEquals("less than(won't jump)",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("equal to(will jump)",POP().jint,134);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("greater than(will jump)",POP().jint,30); 
}



void testIF_ICMPLE() {
    int i;
    registerMethod("TestJumpInstructions.testIF_ICMPLE()");
    for (i = 3;i<30;i++) 
      pc[i] = INSTR_BREAKPOINT;
    pc[0] = INSTR_NOP;
    pc[1] = INSTR_NOP;
    pc[2] = INSTR_IF_ICMPLE;
    *((signed short int*)(&pc[3])) = htons(10);
    pc[12] = INSTR_IADD;
    pc[13] = INSTR_BREAKPOINT;
    PUSH(10); // this and the next number will be added
    PUSH(20); 
    PUSH(12); // value1 < value2 and will branch
    PUSH(124);
    PUSH(40); // This and next number will be added
    PUSH(94);
    PUSH(-7); // value1 == value2 and will branch  
    PUSH(-7); 
    PUSH(122);// arbitrary number
    PUSH(6); // value1 == value2 and will NOT branch
    PUSH(1); // 
    runInterpreter();//(frame,&area,0,0);
    assertEquals("greater than (won't jump)",POP().jint,122);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("eqaul to(will jump)",POP().jint,134);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("less than(will jump)",POP().jint,30); 
}

void testRET() {
    int i;
    byte * oldBC = pc;
    registerMethod("TestJumpInstructions.testRET()");
    //    failure("waiting for KP to fix interpreter generator\n");
    for (i = 0;i<30;i++) 
	pc[i] = INSTR_BREAKPOINT;
    pc[10] = INSTR_RET;
    pc[11] = 3; // index where return address is
    SETLOCAL(3, (int)pc);//*getPcOfFrame(frame)); 
    // will return to this spot
    pc[0] = INSTR_IADD;
    pc[1] = INSTR_BREAKPOINT;
    PUSH(10);
    PUSH(20);
    /** tweaking staring position**/
    pc=&(pc[10]);//*getPcOfFrame(frame)=&(pc[10]);
    //printf("Runn interpreter at %d(%d...:)\n",(int)pc,oldBC[0]);
    runInterpreter();//(frame,&area,0,0);
    assertEquals("test jumping backwards",POP().jint,30);
    /** resetting starting instruction to normal **/
    
    pc = oldBC;//*getPcOfFrame(frame)=pc;
}

int main() {
  setup();
  testLOOKUPSWITCH();  
  testTABLESWITCH();
  testGOTO();
  
  testGOTO_W();
  testJSR(); 
  testJSR_W();
  testGOTO_backwards();
  testIFEQ();
  testIFNE();
  testIFLT();
  testIFLE();
  testIFGT();
  testIFGE();
  testIFNULL();
  testIFNONNULL();
  testIF_ACMPEQ();
  testIF_ACMPNE();
  testIF_ICMPEQ();
  testIF_ICMPGT();
  testIF_ICMPLT();
  testIF_ICMPNE(); 
  testRET();
  teardown();
  return 0;
}

