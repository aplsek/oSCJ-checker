/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This file contains the code that the main loop of the C code
 * interpreter. This file includes a generated file that contains C
 * code implementations of the instructions that is generated
 * automatically from a description of the bytecodes.
 * @author Grothoff, Vitek, Titzer, Palacz
 **/
#include "config.h"
#include "interpreter.h"
#include "frame.h"
#include "instruction_names.h"
#include "types.h"
#include "interpreter_defs.h"
#include "print.h"
#include "globals.h"
#include "debugging.h"
#include "engine.h"
#include "exception.h"
#include "mem.h"
#include "jvm_constants/opcodes.h"
#include "clock.h"
#include "atomicops.h"
#include "timer.h"
#include "mthread.h"
#include "blockingio.h"
#include "sigiomanager.h"
#include "native_helpers.h"
#include "eventmanager.h"
#include "waitmanager.h"
#include "systemproperties.h"
#include "signalmonitor.h"
#include "doselect.h"
#include "nativeScheduling.h"

/* Note the angle quotes.  They ensure that we start searching from
 * the start of the include path
 */
#include <app_native_calls.h>

#include <math.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <time.h>
#include <sys/time.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <netdb.h>
#include <sys/stat.h>
#include <sys/types.h>


extern NativeContext* current_native_context;

/**
 * core of the instruction lookupswitch.
 * @param offset pointer to the instruction AFTER the padding
 * @param key key to look for, popped from the stack
 * @return the offset of the next instruction
 ***/
static int lookup_switch(int def, int npairs, int* list,  int key) {
  int lowerBound = 0;
  int upperBound = npairs - 1;
  
  /*  printf("Looking for key 0x%08X with %d pairs\n",
       key, npairs);*/
  while (lowerBound <= upperBound) {
    int currentPos = (lowerBound + upperBound) / 2;
    int currentKey = htonl(list[currentPos * 2]);
    /*    printf("\t%d is list[0] currentPos %d\n",list[0],currentPos);
	  printf("\tCurrent key: 0x%08X / %d\n",currentKey, currentKey);*/
    if (key < currentKey) {
      upperBound = currentPos - 1;
    } else if (key > currentKey) {
      lowerBound = currentPos + 1;
    } else { //  key == currentKey 
      return htonl(list[currentPos * 2 + 1]);
    }
  }
  return def; /* NO htonl here, was converted by caller! */
} /* end of lookupswitch */


// dummy function to call to allow easier setting of breakpoints
void breakpoint() {
/*      printf("Reached breakpoint\n"); */
}

inline static int is_array(Blueprint* bpt) {
    return bpt->arrayDepth_ > 0;
}

Blueprint* strip_array_blueprint(Blueprint* bpt, int howManyTimes) {
    int i;
    ASSERT(howManyTimes <= bpt->arrayDepth_,  "whoops");
    for (i = 0; i < howManyTimes; i++) {
	bpt = ((struct s3_core_domain_S3Blueprint_Array*)bpt)->componentBlueprint_;
    }
    return bpt;
}

int is_subtype_of(Blueprint* client, Blueprint* provider) {
    if (client == provider) { /* equality should work for primitives */
	return YES;
    }

    if (provider->typeBucket_ >= client->typeDisplay_->length)
      return NO;

    ASSERT(client->typeDisplay_ != NULL, "whoops");
    if (client->typeDisplay_->values[provider->typeBucket_] 
	== provider->typeBucketID_) {
	return YES;
    } 
    /*	printf("failed display[%d] = %d != %d\n",
 	provider->typeBucket_,
 	client->typeDisplay_->values[provider->typeBucket_]
 	provider->typeBucketID_);
    */
    
    if (!is_array(provider)) {
	return NO; /* the test was subsumed */ 
    } else { /* provider is an array, test doesn't matter */
	if (!is_array(client)) {
	    return NO;
	} else {
	    int cdepth = client->arrayDepth_;
	    int pdepth = provider->arrayDepth_;
	    if (pdepth > cdepth) { 
		return NO;
	    } else { /* pdepth <= cdepth */
		return is_subtype_of(strip_array_blueprint(client, pdepth),
				     strip_array_blueprint(provider, pdepth));
	    }
	}
    } 
}

void dump_iftable_and_die(Blueprint* bp) {
  int cntr;
  print_arr_jbyte(bp->dbg_string); 
  printf(" - Null iftable entry, not good\n");
  for (cntr = 0; cntr < bp->ifTable->length; cntr++) {
    ByteCode * fr = (ByteCode *)((bp->ifTable->values[cntr]));
    printf("%d: ", cntr);
    if (fr != NULL) {
      print_arr_jbyte(fr->dbg_string);
    }
    else {
      printf(" <null> ");
    }
    printf("\n");
  }
  abort();
}


#if DEBUGGING

void debug_opcode(NativeContext* current_context, 
		  byte* pc,
		  jvalue* local_vars,
		  jvalue* stack_top) {
    int current_opcode;
    int i;
    if (override_tracing) {
        return;
    }
    if (pc == NULL) {
	printf("pc == NULL?\n");
	return;
    }
    current_context->current_frame->previous_pc = pc;
    current_opcode = *pc;
    if (verbose_mode < 1) {
	return;
    }
    if (verbose_mode > 1) {
	print_operand_stack(current_context, stack_top);
    }
    for (i = 1; i < frameDepth; i++) {
	printf("   ");
    }
    printf("code[%d]=", pc - 
        (byte*)(current_context->current_frame->current_method->bytes->values));
    switch(current_opcode) {
#include "disassembler.gen"
	
    case INSTR_BREAKPOINT:
	printf("BREAKPOINT\n");
	break;
    case OPCODE_INVOKE_NATIVE: {
	int syscallnum = ISTREAM_GET_UBYTE_AT(1);
	printf("INVOKE_NATIVE %d\n", syscallnum);
	break;
    }
    case INVOKE_SYSTEM: {
	int syscallnum = ISTREAM_GET_UBYTE_AT(1);
	printf("INVOKE_SYSTEM ");
	switch (syscallnum) {
	case SYSTEM_ACTIVE_CONTEXT: {
	    printf("ACTIVE_CONTEXT\n");
	    break;
	}
	case SYSTEM_SETPOLLCOUNT: {
	    printf("SETPOLLCOUNT\n");
	    break;
	}
	case SYSTEM_DESTROY_NATIVE_CONTEXT: {
	    printf("DESTROY_NATIVE_CONTEXT\n");
	    break;
	}
	case SYSTEM_WORD_OP: {
	    int wopnum = ISTREAM_GET_UBYTE_AT(2);
	    printf( "WORD_OP ");
	    switch ( wopnum ) {
#define PRINTSYM(pfx,name) case pfx##name: puts( #name); break;
	    PRINTSYM(WORD_OP_,SCMP)
	    PRINTSYM(WORD_OP_,UCMP)
	    PRINTSYM(WORD_OP_,ULT)
	    PRINTSYM(WORD_OP_,ULE)
	    PRINTSYM(WORD_OP_,UGE)
	    PRINTSYM(WORD_OP_,UGT)
	    PRINTSYM(WORD_OP_,UI2L)
	    default:
	      	printf("Unsupported WordOp: %d\n", wopnum);
	    } /* end switch ( wopnum ) */
	    break;
	}
	case SYSTEM_DEREFERENCE: {
	    int derefnum = ISTREAM_GET_UBYTE_AT(2);
	    printf( "DEREFERENCE ");
	    switch ( derefnum ) {
	    PRINTSYM(DEREFERENCE_,GETBYTE)
	    PRINTSYM(DEREFERENCE_,GETSHORT)
	    PRINTSYM(DEREFERENCE_,SETBYTE)
	    PRINTSYM(DEREFERENCE_,SETSHORT)
	    PRINTSYM(DEREFERENCE_,SETBLOCK)
	    default:
	      	printf("Unsupported Dereference: %d\n", derefnum);
	    } /* end switch ( derefnum ) */
	    break;
	}
	case SYSTEM_EMPTY_CSA_CALL: {
	    printf("EMPTY_CSA_CALL\n");
	    break;
	}
	case SYSTEM_GET_CONTEXT:
	    printf("GET_CONTEXT\n");
	    break;
	case SYSTEM_MAKE_ACTIVATION:
	    printf("MAKE_ACTIVATION\n");
	    break;
	case SYSTEM_CUT_TO_ACTIVATION:
	    printf("CUT_TO_ACTIVATION\n");
	    break;
	case SYSTEM_RUN: 
	    printf("RUN\n");
	    break;    
	case SYSTEM_GET_ACTIVATION: 
	    printf("GET_ACTIVATION\n");
	    break;
	case SYSTEM_NEW_CONTEXT: 
	    printf("NEW_CONTEXT\n");
	    break;
	case SYSTEM_START_TRACING: 
	    printf("START_TRACING\n");
	    break;
	case SYSTEM_STOP_TRACING: 
	    printf("STOP_TRACING\n");
	    break;
        case SYSTEM_BEGIN_OVERRIDE_TRACING:
            printf("SYSTEM_DISABLE_TRACING\n");
            break;
        case SYSTEM_END_OVERRIDE_TRACING:
            printf("SYSTEM_RETURN_TRACING\n");
            break;
	case SYSTEM_BREAKPOINT: 
	    printf("BREAKPOINT\n");
	    break;
	case SYSTEM_INVOKE:
	  printf("INVOKE\n");
	  break;
	case SYSTEM_CAS32: {
	    printf("CAS32\n");
	    break;
	}
	case SYSTEM_CAS64: {
	    printf("CAS64\n");
	    break;
	}
	default:
	    printf("Unsupported SYSCALL: %d\n", syscallnum);
	} /* end switch(syscallnum) */
	break;
    } /* end case INVOKE_SYSTEM: */
    default: 
	printf("UNKNOWN[%d/0x%x]\n", current_opcode, current_opcode);
    } /* end switch (current_opcode) */
}
#endif

void launchContext(void *arg) {
    struct ovm_core_execution_Context* ctx = (struct ovm_core_execution_Context*) arg;
    run_interpreter((NativeContext*)ctx->nativeContextHandle_);
}  

jref inspectable;    


static void shove_invocation(NativeContext* context, 
			    jvalue* param_base,
			    InvocationMessage* msg) {
  int i,j;
  jvalue arg;
  arg.jref = msg->receiver;
  set_local_ref(context, param_base, 0, arg);

  for (i = 0, j=1; i < msg->inArgs->length; i++) {
    ValueUnion* value = msg->inArgs->values[i];
    switch (value->tag) {
    case 'L': 
    case 'G':
    case '[': {
      arg.jref = value->reference;
      set_local_ref(context, param_base, j++, arg);
      break;
    }
    case 'D':
    case 'J': {
      jwide argwide;
      argwide.jlong = value->widePrimitive;
      set_local_wide(context, param_base, j, argwide); 
      j +=2;
      break;
    }
    case 'V':
      break;
    default:
      arg.jint = value->primitive;
      set_local_prim(context, param_base, j++, arg);
    }
  }
}

static void profileNative(int _) {}

void dumpNativeProfileHisto(void) {}

/**
 * Execute the top frame of this context.
 **/
void run_interpreter(NativeContext * current_context) {
#include "instruction_dispatch.gen"
    
#define STATS 0
#if STATS
    unsigned int stats[512];
    int i;
#endif
    /* this is the poll event counter: see interpreter_defs.h */
    jlong counter = 100;  /* actual counter */
    jlong pollcount = 100;  /* current threshhold */
    
    int current_opcode;
    DECLARE_CONTEXT_CACHE(); /* pc, local_vars, stack_top */ 
    
    LOAD_CONTEXTCACHE();
    
#if STATS
    for (i = 0; i < 512; i++) {
	stats[i] = 0;
    }
#endif
    
    NEXT_INSTRUCTION;

#include "java_instructions_threaded.gen"

INSTR_UNKNOWN: {
  current_opcode = *pc;
  goto INTERPRETER_LOOP;

}


    INTERPRETER_LOOP:
    current_opcode = *pc;
#if STATS
    stats[current_opcode]++;
#endif
    

  switch (current_opcode) {

  case INSTR_BREAKPOINT:
    SAVE_CONTEXT();
    return;

  case OPCODE_INVOKE_NATIVE: {
    int code = ISTREAM_GET_UBYTE_AT(1);
#define DONE_HOOK 0
#include "native_calls.gen"
    INCPC(2);
    NEXT_INSTRUCTION;
  }

  case OPCODE_INVOKE_SYSTEM: {
    int syscallnum = ISTREAM_GET_UBYTE_AT(1);
    switch (syscallnum) {
    case SYSTEM_SETPOLLCOUNT: {
      jlong old = pollcount;
      counter = POP_W().jlong;
      pollcount = counter;
      printf("New poll count: %lld\n",counter);
      PUSH_W(old);
      break;
    }
    case SYSTEM_DESTROY_NATIVE_CONTEXT: {
      NativeContext * ctx = (NativeContext*) POP().jref;
      if ( ctx == current_context ) {
      	fputs( "Ouch! Don't do that to the current context!\n", stderr);
	abort();
      }
      destroy_context(ctx);
      break;
    }
    case SYSTEM_WORD_OP: {
      int wopnum = ISTREAM_GET_UBYTE_AT(2);

	jint rhs = POP().jint; /* FIXME assumes 32 bit */
	jint lhs; /* only initialize this if not a unary op */
	jint result;
	if ( wopnum == WORD_OP_UI2L ) {
	    PUSH_W( (jlong)(unsigned int)rhs);
	    break; /* case SYSTEM_WORD_OP */
	}
	lhs = POP().jint; /* FIXME too */
	switch ( wopnum ) {
	case WORD_OP_SCMP: {
	    result = ( lhs == rhs ) ? 0
	             : ( (signed int)lhs < (signed int)rhs ) ? -1 : 1;
	    break;
	}
	case WORD_OP_UCMP: {
	    result = ( lhs == rhs ) ? 0
	           : ( (unsigned int)lhs < (unsigned int)rhs ) ? -1 : 1;
	    break;
	}
	case WORD_OP_ULT: {
	    result = (unsigned int)lhs <  (unsigned int)rhs;
	    break;
	}
	case WORD_OP_ULE: {
	    result = (unsigned int)lhs <= (unsigned int)rhs;
	    break;
	}
	case WORD_OP_UGE: {
	    result = (unsigned int)lhs >= (unsigned int)rhs;
	    break;
	}
	case WORD_OP_UGT: {
	    result = (unsigned int)lhs >  (unsigned int)rhs;
	    break;
	}
      	default:
      	    printf( "INTERPRETER ERROR: Word op instruction "
	    	    "0x%02X (%d) invalid\n", wopnum, wopnum);
      	    exit(1);
	} /* end switch ( wopnum ) */

	PUSH_P(result);
	break;
    }
    case SYSTEM_DEREFERENCE: {
      int derefnum = ISTREAM_GET_UBYTE_AT(2);
      
	switch ( derefnum ) {
	case DEREFERENCE_GETBYTE: {
	    PUSH_P((jint) *(jbyte *)POP().jref);
	    break;
	}
	case DEREFERENCE_GETSHORT: {
	    PUSH_P((jint) *(jshort *)POP().jref);
	    break;
	}
	case DEREFERENCE_GETCHAR: {
	    PUSH_P((jint) *(jchar *)POP().jref);
	    break;
	}
	case DEREFERENCE_SETBYTE: {
	    jbyte  val = (jbyte) POP().jint;
	    jbyte *loc = (jbyte *)POP().jref;
	    *loc = val;
	    break;
	}
	case DEREFERENCE_SETSHORT: {
            jshort val = (jshort) POP().jint;
	    jshort *loc = (jshort *)POP().jref;
	    *loc = val;
	    break;
	}
	case DEREFERENCE_SETBLOCK: {
      	    size_t bytes = POP().jint;
	    void const *src = POP().jref;
	    void *dst = POP().jref;
	    memmove( dst, src, bytes);
	    break;
	}
      	default:
      	    printf( "INTERPRETER ERROR: Dereference instruction "
	    	    "0x%02X (%d) invalid\n", derefnum, derefnum);
      	    exit(1);
	} /* end switch ( derefnum ) */
	break;
    }
    case SYSTEM_EMPTY_CSA_CALL: {
	PUSH_R((struct java_lang_Object *)csaTHIS);
	INVOKE_CSA(emptyCall, 1, 0);
	ASSERT(0, "not reached");
    }
    case SYSTEM_MAKE_ACTIVATION: {
      InvocationMessage* msg =	(InvocationMessage*)POP().jref;
      ByteCode *rcf = (ByteCode*)POP().jref;
      Blueprint *bp = HEADER_BLUEPRINT(msg->receiver);
      NativeContext* given_current_context = (NativeContext*) POP().jref;
      if ( given_current_context == current_context ) {
	create_frame(CONTEXT_CACHE_INOUT(), rcf, bp, 0); /* chap style */
	shove_invocation(current_context, local_vars, msg);
      } else {         
        PFX_DECLARE_CONTEXT_CACHE(given_);
	PFX_LOAD_CONTEXTCACHE(given_);
	INCPC(3);
        create_frame(PFX_CONTEXT_CACHE_INOUT(given_), rcf, bp, 0);
	PFX_SAVE_CONTEXT(given_);
	shove_invocation(given_current_context, given_local_vars, msg);
      }
      PUSH_P((jint)given_current_context->current_frame);
      NEXT_INSTRUCTION;
      ASSERT(0, "not reached");
      break;
    }
    case SYSTEM_INVOKE: {
      InvocationMessage* targetMsg = (InvocationMessage*)POP().jref;
      ByteCode* targetCode = (ByteCode*)POP().jref;
      Blueprint* bp = HEADER_BLUEPRINT(targetMsg->receiver);
      current_context->current_frame->pc_adjustment = 3;
      create_frame(CONTEXT_CACHE_INOUT(), targetCode, bp, 0); 
      shove_invocation(current_context, local_vars, targetMsg);
      NEXT_INSTRUCTION;
      ASSERT(0, "not reached");
      break;
    }
    case SYSTEM_CUT_TO_ACTIVATION: {
      Frame * frame = (Frame*) POP().jref;
      NativeContext * context = (NativeContext*) POP().jref;
      
      IFDEBUG_DO(frameDepth-= (context->current_frame - frame));
      INCPC(3);
      if (0 && context->processing_stack_overflow == YES) {
          printf("CUT-TO_ACTIVATION turning off overflow processing."
                 " Recovered %d frames, leaving %d\n",
                 (context->current_frame - frame),
                 (context->max_frame - frame));
      }
      context->processing_stack_overflow = NO; /* whenever we're at a catch, we're no longer processing a stack overflow */
      context->current_frame = frame;
      /* I am done if I have cut to a new frame in any context other than
       * the current one. But if it is the current context, I am not quite
       * done yet: must update cached variables.
       */
      context->current_opstack = frame->top_of_stack;
      if ( context == current_context ) {
      	LOAD_CONTEXTCACHE();
      }
      NEXT_INSTRUCTION;
      ASSERT(0, "not reached");
      break;
    }
    case SYSTEM_RUN: {
      NativeContext *ctxHandle = (NativeContext *)POP().jref;
      int processorHandle = POP().jint;
      if ( processorHandle != 0 ) { /* FIXME for more than 1 processor */
      	printf("Can't handle processor %d yet!\n", processorHandle);
	exit( 1);
      }
      IFDEBUG_DO(current_context->frame_depth = frameDepth);
      INCPC(3);
      SAVE_CONTEXT();
      if (0) printf("Switching from context %p locals %p stacktop %p pc %p &pc %p\n", 
	      current_context, local_vars, stack_top, pc, &pc);
#if USE_MTHREAD == YES
      mthread_context_switch(&current_context->mthread->sp,
			     &ctxHandle->mthread->sp);
#else
      current_context = ctxHandle;
      current_native_context = ctxHandle;
#endif /* USE_MTHREAD */
      IFDEBUG_DO(frameDepth = current_context->frame_depth);
      LOAD_CONTEXTCACHE();
      if (0) printf("Switching to context %p locals %p stacktop %p pc %p &pc %p\n",  
  	      current_context, local_vars, stack_top, pc, &pc); 
      NEXT_INSTRUCTION;
      ASSERT(0, "not reached");
      break;
    }
    case SYSTEM_GET_CONTEXT: {
      int processorHandle = POP().jint;
      if ( processorHandle != 0 ) { /* FIXME for more than 1 processor */
      	printf("Can't handle processor %d yet!\n", processorHandle);
	exit( 1);
      }
      PUSH_R((jref)current_context->ovm_context); /* this IS an Oop! */
      break;
    }
    case SYSTEM_NEW_CONTEXT: {
      jref appCtx = POP().jref;
      NativeContext *newCtx = create_context( MAX_METHOD_DEPTH,
      	      	      	      	      	global_catch_code,
					appCtx);
      /* fprintf(stderr, "created native context 0x%x with catch code 0x%x\n",
	 newCtx, global_catch_code); */
      if (newCtx == NULL) { /* out of memory */
          THROW_EXCEPTION(OUT_OF_MEMORY_ERROR, 0);
      }
#if USE_MTHREAD == YES
      newCtx->mthread = create_mthread(run_interpreter,
				       MTHREAD_DEFAULT_STACK_SIZE,
				       newCtx);
      if (newCtx->mthread == NULL) {
          THROW_EXCEPTION(OUT_OF_MEMORY_ERROR, 0);
      }
      newCtx->mthread->ctx = newCtx;
#endif
      PUSH_P((jint)newCtx); /* not an Oop! */
/*        printf("new context 0x%x\n", newCtx); */
      break;
    }
    case SYSTEM_START_TRACING: {
      jint printStacks = POP().jint;
      fprintf(stderr, "instruction tracing started\n");
      if (printStacks) {
	verbose_mode = 2;
      } else {
	verbose_mode = 1;
      }
      break;
    }
    case SYSTEM_STOP_TRACING: {
      verbose_mode = 0;
      fprintf(stderr, "instruction tracing stopped\n");

      break;
    }
    case SYSTEM_BEGIN_OVERRIDE_TRACING: {
        ++override_tracing;
        break;
    }
    case SYSTEM_END_OVERRIDE_TRACING: {
        --override_tracing;
        break;
    }
    case SYSTEM_BREAKPOINT: {
      inspectable = POP().jref;
      breakpoint(); 
      break;
    }

    case SYSTEM_CAS32: {
	  jint update = POP().jint;
	  jint expected = POP().jint;
	  jint* target = (jint*)POP().jint;
	  jint result = CAS32(target, expected, update);	
	  PUSH_P(result);
	  break;
    }
    case SYSTEM_CAS64: {
	  jlong update = POP_W().jlong;
	  jlong expected = POP_W().jlong;
	  jlong* target = (jlong*)POP().jint; 
	  jint result = CAS64(target, expected, update);
	  PUSH_P(result);	
	  break;
    }
    default:
      printf("INTERPRETER ERROR: System instruction 0x%02X (%d) invalid\n",
	     syscallnum, syscallnum);
      exit(1);
    }
    /* note: some case statements do an explicit NEXT_INSTRUCTION 
       so they don't return here. */
    INCPC(3);
    NEXT_INSTRUCTION;
  }
    break;
    
    
    /* unknown instruction */
  default:
    printf("INTERPRETER ERROR: Instruction %s [0x%02X,%d] not implemented\n",
	   instr_name[current_opcode],
	   current_opcode, 
	   current_opcode);
    stacktrace(current_context);
    exit(1); 
    
    
  };
  goto INTERPRETER_LOOP;
}

/* End of interpreter */




