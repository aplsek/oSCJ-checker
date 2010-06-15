/**
 * Testing framework
 * @author Christian Grothoff
 * @author James Liang
 **/

#include "framework.h"

CodeFragment * dummyFragment;
NativeContext* current_context;
jvalue*  local_vars;
jvalue*  stack_top;
byte*    pc;
byte*    endingPC;

#define MAKEFRAME() create_frame(current_context,dummyFragment,NULL,&local_vars,&stack_top,&pc,0)

void setup() {
  initializeCUnit(); 
  setBehaviour(CUNIT_REPORT_ALL | CUNIT_PROGRESS_DOT);

  dummyFragment = (CodeFragment *)malloc(sizeof(dummyFragment));
  dummyFragment->code_ = (struct arr_jbyte *)malloc(70000);
  // YES, we need this much
  pc = (byte*)dummyFragment->code_;
  dummyFragment->code_->length = 65535;/* not 70000*/
  dummyFragment->maxStack_ = 18;
  dummyFragment->maxLocals_ = 350;

  current_context = create_context(100, dummyFragment); 
  get_frame_values(current_context, &local_vars, &stack_top, &pc);
  MAKEFRAME();
  save_context(current_context, stack_top, local_vars, pc);

  /*  print_context(current_context);*/
}

void runInterpreter() {
  save_context(current_context, stack_top, local_vars, pc);
  run_interpreter(current_context);
  get_frame_values(current_context, &local_vars, &stack_top, &endingPC);
}

void cleanup() {
  //  clear_stack(&stack_top,context);
}

void teardown() {
    /** dealocate memory **/
  
  /* printf("\n");
     print_cunit_summary();*/
  printf("\n");
}
