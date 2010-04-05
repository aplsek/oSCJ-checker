/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * Alternative frame manipulation header.  It will be used for debugging
 * purposes.
 *
 * @file interpreter/include/ovm/frame.h
 * @author Jan Vitek, James Liang
 **/

#ifndef _FRAME_H_
#define _FRAME_H_

#include "md.h"			/* big time hackery follows */
/*
 * md.h is going to be included somewhere somehow no matter what we
 * do.  It defines certain functions in terms of the physical
 * architecture that we would rather define in terms of the JVM.  So,
 * we let it use the real names of those functions, and make sure that
 * those names are never used again.
 */
#define stackGrowsUp    intrp_stackGrowsUp
#define callerLocalsEnd intrp_callerLocalsEnd

#include "util.h"  /* config.h included from here */
#include "jtypes.h"
#include "types.h"
#include <mthread.h>

/* Our expectation for the average local variable stack height */
#define AVERAGE_LOCAL_STACK_MAX 100

/* how many frames do we HAVE to have left before we generate
   a stack overflow exception? (we need some left to generate
   the exception!)  -- and yes, we need that many, 20 is NOT enough!*/
#define MIN_FREE_FRAMES 50

/**
 * struct Frame
 *
 * This structure represents a portion of stack (including local
 * variables) that is needed for the execution of a method.
 **/
typedef struct {
  final ByteCode*     current_method;
  final Blueprint*    current_bp;
  final struct s3_core_domain_S3Constants *current_cp;
  final struct arr_java_lang_Object* current_cp_values;
  final struct arr_jbyte* current_cp_tags;
  final jvalue*       locals_base; /* base pointer for locals */
  final jvalue*       top_of_stack;
  short               pc_adjustment;     /* adjust after invoke     */
  short               padding;  /* may be useful at some point */

  /*  final int               max_locals; -- number of jany slots
      There seems to be little need for the above field -- jv */
  byte*                   pc;         // program counter
  byte*               previous_pc;
} Frame;


/**
 * Points to the chunk of memory allocated for local variables and operand
 * stacks.
 **/
typedef struct  {
  final char* tag;
  final jvalue* max;
  final jvalue* base;
} VarArea;

#define TAG_EMPTY 2
#define TAG_REF  1
#define TAG_PRIM 0

/**
 * struct NativeContext
 *
 * This struct contains a reference to the OVM-level Context, which describes
 * the execution context for a thread, including a collection of Activations
 * needed to execute its methods. The rest of this struct is merely
 * implementation detail for that collection of Activations (where they are
 * allocated, etc.) and does not have anything else to do with the OVM Context.
 * (Dissent exists as to whether the name NativeContext is felicitous.)
 * This could be thought of as a bit of ContextLocal state that happens to be
 * needed when manipulating Activations.
 **/
typedef struct {
  int processing_stack_overflow;  /* flag to indicate if we are currenlty processing a stack overflow */
  final VarArea     var_area;
  final Frame*      catch_all_frame;      /* bottom frame in the context  */
  final Frame*      max_frame;            /* upper bound		  */
  Frame*            current_frame;        /* current frame		  */
  jvalue*           current_opstack;      /* operand stack pointer	  */
  jref              ovm_context;          /* reference to the OVM Context */
#if DEBUGGING
  int               frame_depth;
#endif
#if USE_MTHREAD == YES
    struct mthread_block * mthread; /* A mthread tied to this native context */
#endif
} NativeContext;


/** Documentation in frame.c **/
NativeContext*  create_context(int initial_size, 
			       ByteCode* catch_code,
			       jref ovm_context);

void destroy_context(NativeContext* context);

inline void save_context(NativeContext* context, 
			 jvalue* locals_base,
			 jvalue* top_of_stack, 
			 byte* pc);
#if DEBUGGING
void print_context(NativeContext* this);

void print_frame(Frame* this);
#endif

inline int create_frame(NativeContext* context,
			jvalue** locals_base,
			jvalue** top_of_stack, 
			byte** pc,
			CoreServicesAccess **csaTHIS,
			ByteCode* code_fragment,
			Blueprint * bp,
			int total_param_size);

inline void pop_frame(NativeContext* context,
		      jvalue** locals_base,
		      jvalue** top_of_stack, 
		      byte** pc,
		      CoreServicesAccess **csaTHIS);

inline void get_frame_values(NativeContext* context,
			     jvalue** locals_base,
			     jvalue** top_of_stack, 
			     byte** pc,
			     CoreServicesAccess **csaTHIS);

#if DEBUGGING
void check_invariants(NativeContext* context,
		      Frame* frame,
		      jvalue* locals_base,
		      jvalue* top_of_stack, 
		      byte* pc);

void print_operand_stack(NativeContext * context, 
			 jvalue *topofStack);

#endif

void dump_frame(Frame *f, int count);
int stacktrace(NativeContext* context);

#ifdef IN_FRAME_C
# define FRAME_INLINE
#else 
# define FRAME_INLINE static inline
#endif

extern void *getOVMContextLoc(int nc);

// These guys are inlined with or without fast to match the old
// INVOKE_SYSTEM behavior.
FRAME_INLINE void *interp_getPC(jint _act) {
  return ((Frame *) _act)->pc;
}

FRAME_INLINE void interp_setPC(jint _act, void *pc) {
  ((Frame *) _act)->pc = pc;
}

FRAME_INLINE void *interp_getCode(jint _act) {
  return ((Frame *) _act)->current_method;
}

FRAME_INLINE jint interp_getCaller(jint _context, jint _act) {
  NativeContext *context = (NativeContext *) _context;
  Frame *frame = (Frame *) _act;
  return (context->catch_all_frame < frame
	  ? (jint) --frame /* not an Oop! */
	  : (jint) NULL_REFERENCE);
}

FRAME_INLINE int interp_topActivation(int nc) {
  NativeContext *ctxHandle = (NativeContext *)nc;
  return (int) ctxHandle->current_frame;
}

inline void    push_ref(NativeContext* ctx, jvalue** jstop, jvalue val);
inline void    push_prim(NativeContext* ctx, jvalue** jstop, jvalue val);
inline void    push_wide(NativeContext* ctx,  jwide** jstop, jwide val);
inline jvalue  pop(NativeContext* ctx, jvalue** jstop);
inline jwide   pop_wide(NativeContext* ctx, jwide** jstop);
inline jvalue  peek_from_top(NativeContext* ctx, jvalue* jstop, int index);
inline jwide   peek_wide_from_top(NativeContext* ctx, jwide* jstop, int index);
inline jvalue  get_local(NativeContext* ctx, jvalue* base, int index);
inline jwide   get_local_wide(NativeContext* ctx, jvalue* base, int index);
inline void    set_local_ref(NativeContext* ctx, jvalue* base, int index, 
			 jvalue value);
inline void    set_local_prim(NativeContext* ctx, jvalue* base, int index, 
			 jvalue value);
inline void    set_local_wide(NativeContext* ctx, jvalue* base, int index, 
			      jwide value);
inline void    pick(NativeContext* ctx, jvalue** jstop, int index);
       void    roll(NativeContext* ctx, jvalue* jstop, int n, int j);
inline void    swap(NativeContext* ctx, jvalue* jstop);


/*
 * The next four methods are used in conservative GC
 */
void *getStackBase(int _nc);
void *getStackTop(int _nc);
void *callerLocalsEnd(int _f);
FRAME_INLINE jboolean stackGrowsUp() { return 1; }
// interpreter threads have java locals and stack slots, but no
// hardware registers
FRAME_INLINE void *getSavedRegisters(jint nc) { return 0; }
FRAME_INLINE jint getSavedRegisterSize() { return 0; }

/*
 * The next four methods are used by InterpreterLocalReferenceIterator
 * for precise GC
 */
FRAME_INLINE void *getValueStack(int _nc) {
  return ((NativeContext *) _nc)->var_area.base;
}
FRAME_INLINE void *getTagStack(int _nc) {
  return ((NativeContext *) _nc)->var_area.tag;
}
FRAME_INLINE int getStackDepth(int _nc, int _f) {
  return ((Frame *) _f)->top_of_stack - (jvalue *)getValueStack(_nc);
}


/*
 * Other methods are used both in precise GC and in exception dispatch
 */
inline void *interp_getLocalAddress(int _context,
			     int _frame,
                                    int index);

inline void *interp_getOperandAddress(int _context,
			       int _frame,
                                      int index);

inline void interp_setOperandReference(int _context,
				       int _frame,
				       int index,
				       int reference);

void interp_setOpstackDepth(jint _context, jint _act, jint d);


/**
 * Returns the max locals value.
 */
#define /*inline int*/ get_max_locals(/*ByteCode* */ cf) \
  /* {  return */  ((cf)->maxLocals_) /*; }*/


/**
 * ++WARNING++  The following macros require the following variables to
 * +++++++++++   be in scope: current_context, local_vars, stack_top, pc
 *
 * The macros that accept a pfx argument (normally empty) will, if a nonempty
 * pfx is given, prepend pfx to the names.
 * E.g. CONTEXT_CACHE_INOUT(new_) will expand to
 * new_current_context, &new_local_vars, &new_stack_top, &new_pc
 *
 * We lied a little. The first of these macros does not require these
 * variables to be in scope; it declares them (all but current_context).
 */
#define PFX_DECLARE_CONTEXT_CACHE(pfx) byte *pfx##pc; \
      	      	      	      	   jvalue *pfx##local_vars, *pfx##stack_top; \
				   CoreServicesAccess *pfx##csaTHIS
#define PFX_CONTEXT_CACHE_INOUT(pfx)  pfx##current_context, \
                                  &pfx##local_vars, &pfx##stack_top, &pfx##pc, \
				  &pfx##csaTHIS
#define PFX_CONTEXT_CACHE_INONLY(pfx) pfx##current_context, \
                                  pfx##local_vars, pfx##stack_top, pfx##pc \
				  
#define PFX_POPFRAME(pfx)    pop_frame(PFX_CONTEXT_CACHE_INOUT(pfx));
				  
#define PFX_LOAD_CONTEXTCACHE(pfx)    get_frame_values(PFX_CONTEXT_CACHE_INOUT(pfx));

#define PFX_SAVE_CONTEXT(pfx)    save_context(PFX_CONTEXT_CACHE_INONLY(pfx));

#define DECLARE_CONTEXT_CACHE() byte *pc; \
      	      	      	      	jvalue *local_vars, *stack_top; \
				CoreServicesAccess *csaTHIS
				   
#define CONTEXT_CACHE_INOUT()  current_context, \
                                &local_vars, &stack_top, &pc, \
				&csaTHIS
#define CONTEXT_CACHE_INONLY() current_context, \
                               local_vars, stack_top, pc
				  
#define POPFRAME()    pop_frame(CONTEXT_CACHE_INOUT());
				  
#define LOAD_CONTEXTCACHE()    get_frame_values(CONTEXT_CACHE_INOUT());

#define SAVE_CONTEXT()    save_context(CONTEXT_CACHE_INONLY());



#define GETLOCAL(i)            get_local(current_context, local_vars, i)
#define GETLOCAL_W(i)          get_local_wide(current_context, local_vars, i)

#define SETLOCAL_P(index,val)  set_local_prim(current_context, local_vars, \
      	      	      	      	      	 index, (jvalue)(val))
#define SETLOCAL_R(index,val)  set_local_ref(current_context, local_vars, \
      	      	      	      	      	 index, (jvalue)(val))
#define SETLOCAL_W(index,val)  set_local_wide(current_context, local_vars, \
                                              index, (jwide)(val))
					      
#define POP()                  pop(current_context, &stack_top)
#define POP_W()                pop_wide(current_context, (jwide **)(void*)&stack_top)

#define PUSH_P(value)          push_prim(current_context, &stack_top, \
                                    (jvalue)((value)))
#define PUSH_R(value)          push_ref(current_context, &stack_top, \
                                    (jvalue)((value)))
#define PUSH_W(value)          push_wide(current_context, \
                                        (jwide **)(void*)&stack_top, (jwide)((value)))

#define PEEK(index)            peek_from_top(current_context, stack_top, \
                                            (index))

#define POKE_P(index, value)     poke_prim_from_top(current_context, stack_top,\
                                            (index), (value))
#define POKE_R(index, value)     poke_ref_from_top(current_context, stack_top,\
                                            (index), (value))

#define PICK(index)            pick(current_context, &stack_top, (index))
#define ROLL(n,j)              roll(current_context, stack_top, (n), (j))
#define SWAP()                 swap(current_context, stack_top)
/** End of Macros **/
#endif /* FRAME_H */
