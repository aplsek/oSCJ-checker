/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 * @file src/interpreter/frame.c
 * @author Jan Vitek, James Liang
 *
 * Code to manipulate call stack frames.
 *
 * The C interpreter maintains a separate call stack for interpreted code,
 * this stack is composed of two data structures NativeContexts and
 * Frames. Furthermore, local variables are allocated off a data structure
 * called a VarArea, where base points to an array of jvalue whose last
 * usable element is max-1, and tag points to an array of char holding the
 * same number of elements.
 *
 * A call stack is split in two units of contiguous storage: contexts and
 * varareas. 
 *
 * A NativeContext is a sequence of frames, where a frame is a fixed size data
 * structure with two pointers into the VarArea, one of them points to the
 * operand stack, while the other points to the local variables of the
 * frame.
 *
 *   NativeContext: 
 *   ---------------       /--------->|---------|
 *   | max_frame   |------/           |top frame|
 *   |-------------|                  |---------|
 *   | var_area    |                  |         |
 *   |-------------|                  |         |
 *   |current_frame|-------\          |         |
 *   |-------------|        \         |---------|
 *   |   current_  |         \------->| frame n |
 *   |   opstack   |                  ...       ...
 *   |-------------|                  |         |
 *   |   catch_    |                  |---------|
 *   |   all_frame |-----------\      | frame 0 |
 *   |-------------|            \---->|---------|
 *
 * Invariants:
 *   - catch_all_frame < max_frame
 *   - catch_all_frame =< current_frame 
 *   - next_frame < max_frame
 *
 *
 *  VarArea:
 *                                       |      |
 *                                       |  s0  |<----stacktop
 *                                       |------|
 *   |      |<--- stacktop               | ...  |
 *   |  s1  |                            |  l1  |
 *   |  s0  |                            |  l0  |
 *   |______|             ===call===>    |------|<---locals (old stacktop)
 *   | ...  |                            | ...  |
 *   |  l1  |                            |  l1  |
 *   |  l0  |                            |  l0  |
 *   |______|<--- locals                 |______|           (stored locals)
 *
 *  Invariant: stacktop points to the unused slot that will be filled by
 *             the next push. The top-of-stack element is stacktop[-1].
 **/

/** NOTE: A moving GC implementation must ensure that the pointer from the
          native context to its OVM Context object is always kept up-to-date
*/

#define IN_FRAME_C

#include <stdlib.h>
#include <string.h>
#include "config.h"
#include "engine.h"
#include "frame.h"
#include "interpreter_defs.h"
#include "instruction_names.h"
#include "mem.h"
#include "exception.h"

#define INVALID_FRAME  ((Frame*)-1)
#define INVALID_LOCALS ((jvalue*)-1)
#define INVALID_STKTOP ((jvalue*)-1)

/**
 * Returns a pointer to the byte code.
 */
inline byte* get_code(ByteCode* bc) {
  return (byte*) &(bc->bytes->values);
}

/**
 * Creates a context.
 *
 * @param initial_size- initial size of the context in number of frames.
 *                      A context may either grow or simply throw an
 *                      exception when no memory is left.
 * @param catch_code -  a fragment of code to execute when there are
 *                      no more frames left in the context.
 * @return a pointer to the new context, or null if it could not be allocated
 */
NativeContext*  create_context(int initial_size, 
			       ByteCode* catch_code,
			       jref ovm_context) {

  Frame* frame  = (Frame*) malloc(sizeof(Frame) * initial_size);
  int locals_sz = sizeof(jvalue) * AVERAGE_LOCAL_STACK_MAX * initial_size; 
  NativeContext* result = (NativeContext*) malloc(sizeof(NativeContext));
  
  /* check allocation was successful, if not clean-up and throw OutOfMemory*/
  if (frame == NULL) {
      return NULL;
  }
  if (result == NULL) {
      free(frame);
      return NULL;
  }

  result->processing_stack_overflow = NO;
  result->var_area.base =
    (jvalue*) malloc(locals_sz * (sizeof(char) + sizeof(jvalue)));

  if (result->var_area.base == NULL) {
      free(frame);
      free(result);
      return NULL;
  }

  result->var_area.max = result->var_area.base + locals_sz;
  result->var_area.tag = (char *)result->var_area.max;
  memset(result->var_area.tag, TAG_EMPTY, sizeof(char)*locals_sz);

  result->catch_all_frame = frame;
  result->current_frame   = frame; 
  result->max_frame       = frame + initial_size;
  result->current_opstack = result->var_area.base + get_max_locals(catch_code);
  result->ovm_context     = ovm_context;
  IFDEBUG_DO(result->frame_depth = 0);
  frame->current_method   = catch_code;
  /* this is the wrong blueprint, but it lives in the executive
   * domain, hence current_bp->myCSA works.
   */
  frame->current_bp       = HEADER_BLUEPRINT(catch_code);
  frame->locals_base      = result->var_area.base;
  frame->pc               = get_code(catch_code);

  return result;
}


void destroy_context(NativeContext * context) {
#if USE_MTHREAD == YES
  destroy_mthread(context->mthread);
#endif
  free(context->catch_all_frame); /* NB this frees the whole array of frames */
  free(context->var_area.base);
  free(context);
}


/**
 * Saves the current context.  Called before operation such as context
 * switching.
 *
 * @param context      - the context to be saved
 * @param locals_base  - base pointer for local variables
 * @param top_of_stack - the top of the current operand stack.
 * @param pc           - the current program counter
 */
inline void save_context(NativeContext* context, 
			 jvalue* locals_base,
			 jvalue* top_of_stack, 
			 byte* pc) {

  context->current_frame->pc = pc;
  context->current_opstack   = top_of_stack; 
  ASSERT(locals_base == context->current_frame->locals_base, "hmm");

  /*  IFDEBUG_DO(check_invariants(context, 
			      context->current_frame, 
			      locals_base, 
			      top_of_stack, 
			      pc));*/
}

/**
 * Creates a frame in the given NativeContext and set the pointers to the 
 * pc, stack, and locals
 *
 * @param context      - the context that we are working with.
 * @param code_fragment- ovm data structure containing the code of 
 *                       to invoke
 * @param bp           - Blueprint for the receiver Oop
 * @param locals_base  - doubly-indirect pointer to the base of the local
 *    	      	      	 variables section that will be modifed to point to
 *                       the locals base of the new frame
 * @param top_of_stack - doubly-indirect pointer to the top of stack that will
 *                       be modified to point to the top of the stack of the
 *                       new frame.
 * @param pc           - doubly-indirect pointer to the program counter that
 *                       will be modifed to point to the pc of the new frame
 * @param total_param_size - The number of local slots in the new frame that
 *                       are to be supplied by copy or overlap from the operand
 *                       stack in the current frame. If this parameter is less
 *                       than the number of parameters for the method associated
 *    	      	      	 with the new frame, then the remaining parameters must
 *                       be set in the locals area of the new frame after this
 *                       function returns.
 *
 * @return the number of frame slots that remain available. If this drops
 * below a specific minimum then a stack overflow is deemed to have occurred.
 */
inline int create_frame(NativeContext* context,
			jvalue** locals_base,
			jvalue** top_of_stack, 
			byte** pc,
			CoreServicesAccess **csaTHIS,
			ByteCode* code_fragment,
			Blueprint * bp,
			int total_param_size) {

  int max_locals = get_max_locals(code_fragment);
  int locals_idx = *top_of_stack - context->var_area.base;

  struct ovm_core_domain_Method *m = code_fragment->_parent_._parent_.itsMethod;
  struct ovm_core_domain_Type_Compound *t =
    ((struct s3_core_domain_S3Method *) m)->type_;
  struct s3_core_domain_S3Type_Scalar *st =
    (struct s3_core_domain_S3Type_Scalar *) t;
  /* Saving old frame information */
  Frame* new_frame;
  Frame* frame  =  context->current_frame;
  frame->pc     =  *pc;
  ASSERT(max_locals > 0, "No space for this pointer in the frame?");

  *locals_base        = *top_of_stack - total_param_size;
  frame->top_of_stack = *locals_base;
  memset(context->var_area.tag+locals_idx, TAG_EMPTY, 
	 sizeof(char)*(max_locals - total_param_size));

  *top_of_stack       = *locals_base + max_locals;
  *pc                 = get_code(code_fragment);
  *csaTHIS	      = bp->myCSA;

  new_frame                 = ++(context->current_frame);
  new_frame->current_method = code_fragment;   // storing in new frame
  new_frame->current_cp = st->constants;
  new_frame->current_cp_values = st->constants->constants;
  new_frame->current_cp_tags = st->constants->tags_;
  new_frame->current_bp     = bp;
  new_frame->locals_base    = *locals_base;
  new_frame->pc_adjustment  = 0;

  if (context->current_frame >= context->max_frame) {
      printf("About to cause frame overflow: ");
      if (context->processing_stack_overflow == YES)
          printf("stackoverflow processing in progress\n");
      else
          printf("normal processing\n");
  }
 
  ASSERT(context->current_frame < context->max_frame,
	 "ERROR: frame overflow\n");
  ASSERT((*top_of_stack < context->var_area.max), 
	 "ERROR: stack overflow\n");
  IFDEBUG_DO(frameDepth++);
  return context->max_frame - context->current_frame;
}

DEBUGVAR(int frameDepth = 0);

/**
 * Pop a frame of the context and set the pointers to the pc, stack, and
 * locals
 *
 * @param context      - the context that we are working with.
 * @param locals_base  - doubly-indirect pointer to the base of the local
 *                       variables section that will be modifed to point to
 *                       the locals base of the restored frame
 * @param top_of_stack - doubly-indirect pointer to the top of stack that will
 *                       be modified to point to the top of the stack of the
 *                       restored frame.
 * @param pc           - doubly-indirect pointer to the program counter that
 *                       will be modifed to point to the pc of the restored
 *			 frame
 */
inline void pop_frame(NativeContext* context,
		      jvalue** locals_base,
		      jvalue** top_of_stack, 
		      byte** pc,
		      CoreServicesAccess **csaTHIS) {

  Frame* restored_frame  = --(context->current_frame);
  *top_of_stack          = restored_frame->top_of_stack;
  /* *top_of_stack = *locals_base;*/
  *locals_base           = restored_frame->locals_base;
  *pc                    = restored_frame->pc + restored_frame->pc_adjustment;
  *csaTHIS		 = restored_frame->current_bp->myCSA;
  restored_frame->pc_adjustment = 0;
  
  IFDEBUG_DO(check_invariants(context, context->current_frame, *locals_base, 
			      *top_of_stack, *pc));
  IFDEBUG_DO(frameDepth--);
}

/**
 * Set the pointers to the pc, stack, and locals to that of the current
 * frame.  Used to initialize local variables when, e.g., resuming 
 * from a context switch.
 *
 * @param context      - The context that we are working with.
 * @param locals_base  - doubly-indirect pointer to the base of the local
 *                       variables section that will be modifed to point to
 *                       the locals base of the restored frame
 * @param top_of_stack - doubly_indirect pointer to the top of stack that will
 *                       be modified to point to the top of the stack of the
 *                       restored frame.
 * @param pc           - doubly-indirect pointer to the program counter that
 *                       will be modifed to point to the pc of the restored
			 frame
 */
inline void get_frame_values(NativeContext* context,
			     jvalue** locals_base,
			     jvalue** top_of_stack, 
			     byte** pc,
			     CoreServicesAccess **csaTHIS) {
  Frame * frame   = context->current_frame;
  *locals_base    = frame->locals_base;
  *top_of_stack   = context->current_opstack;
  *pc             = frame->pc;
  *csaTHIS	  = frame->current_bp->myCSA;

  IFDEBUG_DO(check_invariants(context,frame,*locals_base, *top_of_stack, *pc));
}

inline void push_ref(NativeContext* context, jvalue** jstop, jvalue val) {  
  **jstop = val;
  context->var_area.tag[*jstop - context->var_area.base] = TAG_REF;
  *jstop += 1;
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME, INVALID_LOCALS,
			      *jstop, (byte*)-1));
}

inline void push_prim(NativeContext* context, jvalue** jstop, jvalue val) {  
  **jstop = val;
  context->var_area.tag[*jstop - context->var_area.base] = TAG_PRIM;
  *jstop += 1;
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME, INVALID_LOCALS,
			      *jstop, (byte*)-1));
}

inline void push_wide(NativeContext* context, jwide** jstop, jwide val) {
  char* tagtop =
    ((jvalue*)*jstop) - context->var_area.base + context->var_area.tag;
  tagtop[0] = tagtop[1] = TAG_PRIM;
  **jstop = val; *jstop += 1; 
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME, INVALID_LOCALS,
			      (jvalue*) *jstop, (byte*)-1));
}

inline jvalue pop(NativeContext* context, jvalue** jstop) {
  *jstop -= 1; 
  context->var_area.tag[*jstop - context->var_area.base] = TAG_EMPTY;
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME, INVALID_LOCALS,
			      *jstop, (byte*)-1));
  return **jstop; 
}

inline jwide pop_wide(NativeContext* context, jwide** jstop) {
  char* tagtop;
  *jstop -= 1; 
  tagtop= ((jvalue*)*jstop) - context->var_area.base + context->var_area.tag;
  tagtop[0] = tagtop[1] = TAG_EMPTY;
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME, INVALID_LOCALS,
			      (jvalue*)*jstop, (byte*)-1));
  return **jstop;  
}

inline jvalue get_local(NativeContext* context, jvalue* base, int index) {
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME,  base+index,
			      INVALID_LOCALS, (byte*)-1));
  return *(base + index);
}

inline jwide get_local_wide(NativeContext* context, jvalue* base, int index ) {
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME,  base+index,
			      INVALID_STKTOP, (byte*)-1));
  return *(jwide*)(base + index);
}

inline void set_local_prim(NativeContext* context, jvalue* base, 
		      int index, jvalue value) {
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME, base+index,
			      INVALID_STKTOP, (byte*)-1));
  base[index] = value;
  context->var_area.tag[index + base - context->var_area.base] = TAG_PRIM;
}

inline void set_local_ref(NativeContext* context, jvalue* base, 
		      int index, jvalue value) {
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME, base+index,
			      INVALID_STKTOP, (byte*)-1));
  base[index] = value;
  context->var_area.tag[index + base - context->var_area.base] = TAG_REF;
}

inline void set_local_wide(NativeContext* context, jvalue* base, 
			   int index, jwide value) {
  char* tagtop =
    base - context->var_area.base + context->var_area.tag + index;
  tagtop[0] = tagtop[1] = TAG_PRIM;
  IFDEBUG_DO(check_invariants(context, INVALID_FRAME, base+index,
			      INVALID_STKTOP, (byte*)-1));
  *(jwide*)(base + index) = value;
}

inline jvalue peek_from_top(NativeContext* context, jvalue* jstop, int index) {
  jvalue* js = jstop - index - 1;
  /*  IFDEBUG_DO(check_invariants(context, INVALID_FRAME,  INVALID_LOCALS, 
			      jstop - index - 1, (byte*)-1));
  */
  return *js;
}

inline jwide peek_wide_from_top(NativeContext* context, jwide* jstop, int index) {
  jvalue* js = ((jvalue*)jstop) - index - 1;
  /*  IFDEBUG_DO(check_invariants(context, INVALID_FRAME,  INVALID_LOCALS, 
			     (jvalue*)(jstop - index - 1), (byte*)-1));
  */
  return *(jwide*)js;
}

inline void pick( NativeContext *context, jvalue **stacktop, int n) {
  char *tag_top = context->var_area.tag + (*stacktop - context->var_area.base);
  n = -1 - n;
  (*stacktop)[0] = (*stacktop)[n];
  tag_top    [0] = tag_top    [n];
  ++ *stacktop;
}

/**
 * As defined in the PostScript Language Reference Manual, chapter 8.
 **/
void roll( NativeContext *context, jvalue *stacktop, int n, int j) {
  jvalue *rollbase = stacktop - n;
  char *tagbase = context->var_area.tag + (rollbase - context->var_area.base);
  
  j = ((j % n) + n) % n; /* Agnostic about -j % n in C */
  
  ASSERT( stacktop + j <= context->var_area.max, "No room to roll!");
  
  memmove( rollbase + j, rollbase,     n * sizeof *rollbase);
  memmove( rollbase,     rollbase + n, j * sizeof *rollbase);
  
  memmove( tagbase  + j, tagbase,      n * sizeof *tagbase );
  memmove( tagbase,      tagbase + n,  j * sizeof *tagbase );
}

/* just roll(2,1) but that's a heavyweight way to swap */
inline void swap( NativeContext *context, jvalue *stacktop) {
  char *tag_top = context->var_area.tag + (--stacktop - context->var_area.base);
  jvalue tmp = stacktop[0];
  char   tag = tag_top [0];
  stacktop[ 0] = stacktop[-1];
  tag_top [ 0] = tag_top [-1];
  stacktop[-1] = tmp;
  tag_top [-1] = tag;
}

//---------------------------------------------
//-------------DBG SUPPORT---------------------
//---------------------------------------------

int current_stack_depth(NativeContext* this, jvalue* opstack) {

  Frame*  frame   = this->current_frame;
  jvalue* stkbase = frame->locals_base + get_max_locals(frame->current_method);
  return  opstack - stkbase; 
}



#if DEBUGGING
void print_operand_stack(NativeContext* this, jvalue* opstack) {
  int i, depth = current_stack_depth(this, opstack);

  if (depth == 0) return;

  for (i = 1; i < frameDepth; i++) 
    printf("   ");

  printf("  OpStack[%i]: ", depth);

  if (depth > 150) {
    printf("ERROR: Stack height %d. \n\tExiting", depth);
    abort();
  }

  for (i = 0; i < depth; i++) {
    printf("0x%08X ",opstack[(-i)-1].jint);
#if MEMORY_TRACKING
    if (checkPointer((void*) opstack[(-i)-1].jref)) {
      char * typeName;
      Blueprint * bp = HEADER_BLUEPRINT(opstack[(-i)-1].jref);
      if (checkPointer(bp) && 
	  checkPointer(bp->dbg_string) && 
	  checkPointer(&bp->dbg_string->values[0])) {
	typeName = malloc(bp->dbg_string->length+1);
	if (typeName == NULL) {
	  printf("<invalid BP?> ");
	} else {
	  memcpy(typeName,
		 &bp->dbg_string->values[0],
		 bp->dbg_string->length);
	  typeName[bp->dbg_string->length] = 0;
	  printf("<%s>  ", typeName);
	  free(typeName);
	}
      } else
	printf("<invalid BP?> ");
    }
#else
    printf(" ");
#endif
  }
  
  printf("\n");
}

void print_frame(Frame* this) {
  printf(
  "Printing frame %d: code_frag= %d, locals_base= %d pc= %d\n",
	 (int)this,
	 (int)this->current_method,
	 (int)this->locals_base,
	 (int)this->pc);
}

void print_context(NativeContext* this) {
  Frame* fr = this->current_frame;
  printf("======NativeContext %d======\n", (int)this);
  while( fr >= this->catch_all_frame) 
    print_frame(fr--);
}

#endif

inline void dump_frame(Frame *fr, int count) {
    ByteCode* meth     = fr->current_method;
    struct arr_jbyte* code = fr->current_method->bytes;
    int pc                 = fr->previous_pc - (byte*)(code->values);
    const char* name =
	(pc < 0) ? "" : instr_name[(byte) (code->values[pc])];
    struct arr_jbyte* sel = NULL;
    if (meth != NULL) {
        sel = meth->dbg_string;
    }
    if (sel != NULL) {
      printf("# %d: %.*s %d %s\n", count, sel->length, sel->values, pc, name);
    } else {
      printf("# %d: NULL! %d (%s)\n", count, pc, name);
    }
}

int stacktrace(NativeContext* this) {
     Frame* fr = this->current_frame;
     int count = 0;
     while (fr > this->catch_all_frame) {
       dump_frame(fr--, count++);
     }
     return 0;
}

#if DEBUGGING
void check_invariants(NativeContext* this,
		      Frame* new_frame,
		      jvalue* new_locals,
		      jvalue* new_stack_top,
		      byte* new_pc) {
  
  Frame* current_frame = this->current_frame;
  int max_locals       = get_max_locals(current_frame->current_method);
  jvalue* stack_bottom = current_frame->locals_base + max_locals;

  /*   printf("check_invariants(%d,%d,%d,NST: %d,NPC: %d,SB: %d, MAX:%d)\n",
	  this,new_frame,new_locals,
	  new_stack_top,new_pc,stack_bottom,this->var_area.max);*/

  if (new_frame != INVALID_FRAME) {
    ASSERT(new_frame >= this->catch_all_frame &&
	   new_frame < this->max_frame, "Frame overflow");
    ASSERT(new_frame != NULL, "out of frames");
  }

  if (new_locals != INVALID_LOCALS) {
    ASSERT(new_locals >= current_frame->locals_base,
	   "Local variable underflow");
    ASSERT(new_locals <= stack_bottom, /*KP: was < Not sure ... */
	   "Local variable overflow");
  }
  if (new_stack_top != INVALID_STKTOP) {
    ASSERT(new_stack_top < this->var_area.max, "Operand stack overflow");

    if (new_stack_top < stack_bottom)
      printf("OOPS: stack top: %p stack bottom: %p\n",
	     new_stack_top, stack_bottom);
    ASSERT(new_stack_top >= stack_bottom, "Operand stack underflow");
  }
}
#endif

/*
 * The following functions are called from Java code to inspect and
 * manipulate stack frames.
 */


/*
 * This method is used to update each thread's GCable back-pointer
 */
inline void *getOVMContextLoc(int nc) {
  return &((NativeContext *) nc)->ovm_context;
}

inline void *getEngineSpecificPtrLoc(int nc) {
  return NULL;
}

/*
 * The next four methods are used in conservative GC
 */
inline void *getStackBase(int _nc) {
  NativeContext *nc = (NativeContext *) _nc;
  return nc->var_area.base;
}

inline void *getStackTop(int _nc) {
  NativeContext *nc = (NativeContext *) _nc;
  return nc->current_opstack;
}

inline void *callerLocalsEnd(int _f) {
  Frame *f = (Frame *) _f;
  return f->locals_base;
}

/*
 * Other methods are used both in precise GC and in exception dispatch
 */
inline void *interp_getLocalAddress(int _context,
			     int _frame,
			     int index) {
  Frame *frame = (Frame *) _frame;
  return frame->locals_base + index;
}

inline void *interp_getOperandAddress(int _context,
			       int _frame,
			       int index) {
  Frame *frame = (Frame *) _frame;
  int offset = get_max_locals(frame->current_method) + index;
  return frame->locals_base + offset;
}

inline void interp_setOperandReference(int _context,
				       int _frame,
				       int index,
				       int reference) {
  NativeContext *context = (NativeContext *) _context;
  Frame *frame = (Frame *) _frame;
  char *tagbase = (frame->locals_base -
		   context->var_area.base +
		   context->var_area.tag);
  int offset = get_max_locals(frame->current_method) + index;
  tagbase[offset] = reference ? TAG_REF : TAG_PRIM;
}

void interp_setOpstackDepth(jint _context, jint _act, jint d) {
  Frame *act = (Frame *) _act;
  act->top_of_stack =
    act->locals_base + get_max_locals( act->current_method) + d;
}

