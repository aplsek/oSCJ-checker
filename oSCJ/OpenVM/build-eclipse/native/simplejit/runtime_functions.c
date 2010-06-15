#include "runtime_functions.h"

#include "runtime.h"
#include "gc.h"
#include "timer.h"
#include "globals.h"
#include "mthread.h"
#include "md.h"
#include "engine.h"
#include "eventmanager.h"
#include "signalmanager.h"
#include "timer.h"
#include "native_calls.h"

#include <assert.h>
#include <stdio.h>
#include <sys/stat.h>
#include <errno.h>
#include <math.h>
#include <fcntl.h>
#include <sys/ucontext.h>
#include <signal.h>

typedef struct s3_services_simplejit_NativeContext NativeContext;

void ovmrt_fflush_out() {
    fflush(stdout);
}

inline static int ovmrt_is_array(Blueprint* bpt) {
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

int ovmrt_is_subtype_of(Blueprint* client, Blueprint* provider) {
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
    if (!ovmrt_is_array(provider)) {
	return NO; /* the test was subsumed */ 
    } else { /* provider is an array, test doesn't matter */
	if (!ovmrt_is_array(client)) {
	    return NO;
	} else {
	    int cdepth = client->arrayDepth_;
	    int pdepth = provider->arrayDepth_;
	    if (pdepth > cdepth) { 
		return NO;
	    } else { /* pdepth <= cdepth */
		return ovmrt_is_subtype_of(strip_array_blueprint(client, pdepth),
					   strip_array_blueprint(provider, pdepth));
	    }
	}
    } 
}

jlong ovmrt_ldiv(jlong a, jlong b) {
    return a / b;
}

jlong ovmrt_lrem(jlong a, jlong b) {
    return a % b;
}

jref ovmrt_getCurrentOVMContext(int processorID) {
    if ( processorID != 0 ) { /* FIXME for more than 1 processor */
      	fprintf(stderr, "Can't handle processor %d yet!\n", processorID);
	   fflush(stderr);
	   abort();
    }
    return currentContext->ovm_context;
}

jref ovmrt_newNativeContext(struct ovm_core_execution_Context* ovm_context) {
    struct s3_services_simplejit_NativeContext* ctx =
	   (struct s3_services_simplejit_NativeContext*)
	malloc(sizeof(struct s3_services_simplejit_NativeContext));
    ctx->ovm_context = ovm_context;
    ctx->processing_stack_overflow = NO;
    ctx->reflection_invocation_buffer = 
	   (int)malloc(REFLECTION_INVOCATION_BUFFER_WORD_SIZE * sizeof(int));
    ncontexts++;
    return (jref)ctx;
}

void ovmrt_destroyNativeContext(struct s3_services_simplejit_NativeContext* nc) {
    free((void*)nc->reflection_invocation_buffer);
    free(nc);
    ncontexts--;
}

/* 
 * Initialize the given native context with the given entry method.
 * Notice that the given native context must not be the one which is
 * running currently. This is not an interpreter frame, but a native
 * one. Interpreter.makeActivation looks like you can make an
 * activation to arbitrary context. But here only the initial
 * activation of a context can be made.
 *
 * Current restrictions:
 * The given native context must be a newly created context
 * The given bytecode must be a S3ByteCode and it must have a pointer to its S3NativeCode
 */
jint ovmrt_makeActivation(struct s3_services_simplejit_NativeContext* nc, 
			  NativeCode *ncode, 
			  struct ovm_core_execution_InvocationMessage* msg) {

    ValueUnion* value;
    int i;
    int wordSize = 0;

    if (ncode == NULL) {
	   fprintf(stderr, "Fatal Error: null ByteCode in ovmrt_makeActivation\n");
	   fflush(stderr);
    }

    for(i = 0; i < msg->inArgs->length; i++) {
	   value = msg->inArgs->values[i];
	   switch (value->tag) {
	   case 'L': case 'G':case '[':
	        wordSize++;
	       break;
	   case 'D': case 'J':
	        wordSize += 2;
	       break;
	   case 'V':
	        break;
	   default:
	        wordSize++;
	   }
    }

    {
	   int arg_i;
	   void* arg[wordSize + 2 /* for method and receiver */];
	   arg[0] = ncode;
	   arg[1] = msg->receiver;
	   for(i = 0, arg_i = 2; i < msg->inArgs->length; i++) {
	        value = msg->inArgs->values[i];
	       switch (value->tag) {
	       case 'L': case 'G': case '[':
		      arg[arg_i++] = value->reference;
		      break;
	       case 'D': case 'J': {
		      jwide w;
		      w.jlong = value->widePrimitive;
		      ((jwide*)arg)[arg_i] = w;
		      arg_i += 2;
              break;
	       }
	       case 'V':
		      break;
	       default:
		      arg[arg_i++] = (void*)value->primitive;
	       }
	   }
	   nc->mtb = create_mthread(ncode->_parent_._parent_.foreignEntry,
				    MTHREAD_DEFAULT_STACK_SIZE,
				    arg_i,
				    arg);
    }
    
    if (nc->mtb == NULL)
	   fprintf(stderr, "Fatal Error: Can't make a new mthread block\n");

    nc->mtb->ctx = nc;
    return 0;
}

void* nc2fp(NativeCode* nc) {
    return (void*)nc->_parent_._parent_.foreignEntry;
}

#if defined(OVM_X86)
void ovmrt_invoke(NativeCode * ncode, 
		  struct ovm_core_execution_InvocationMessage* msg) {
    int i, arg_length, arg_ptr = REFLECTION_INVOCATION_NONARG_WORD_SIZE;
    void* fp;
    ValueUnion* value;
    void** buf; // see s3.services.simplejit.NativeContext for layout
    int edi, esi, ebx;

    
    // want to save original edi, esi, ebx!

    __asm__ volatile("mov -4(%%ebp),  %0 \n"
		     "mov -8(%%ebp),  %1 \n"
#ifndef OSX_BUILD
		     "mov -12(%%ebp), %2 \n"
#endif
		     : "=r"(edi),
		       "=r"(esi)
#ifndef OSX_BUILD
		     , "=r"(ebx)
#endif
		     : : "eax", "edx", "ecx");


    buf = (void**)currentContext->reflection_invocation_buffer;
    __asm__ volatile("mov 4(%%ebp), %0 \n" : "=r"(buf[5]));

    buf[2] = (void*)edi;
    buf[3] = (void*)esi;
    buf[4] = (void*)ebx;

    fp = ncode->_parent_._parent_.foreignEntry;

    buf[0] = ncode;
    buf[1] = fp;
    buf[arg_ptr++] = (void*)msg->receiver;

    if (msg->inArgs->length + 1
	   > (REFLECTION_INVOCATION_BUFFER_WORD_SIZE
	   - REFLECTION_INVOCATION_NONARG_WORD_SIZE)) {
	   fprintf(stderr, "reflection args buffer is too small\n");
	   abort();
    }
    for(i = 0; i < msg->inArgs->length; i++) {
	   value = msg->inArgs->values[i];
	   switch (value->tag) {
	   case 'L': 
	   case 'G':
	   case '[': {
	        buf[arg_ptr++] = value->reference;
	       break;
	   }
	   case 'D':
	   case 'J': {
	        jwide w;
	       w.jlong = value->widePrimitive;
	       *((jwide*)&buf[arg_ptr]) = w;
	       arg_ptr += 2;
	       break;
	   }
	   case 'V':
	        break;
	   default:
	        buf[arg_ptr++] = (void*)value->primitive;
	   }
    }

    arg_length = arg_ptr - REFLECTION_INVOCATION_NONARG_WORD_SIZE;
    buf[6] = (void*)arg_length;

    /*
     * Note "$12" (3 words) in the following code should be the
     * byte-size of the arguments and the return address for
     * ovmrt_invoke
     */
    __asm__ volatile("                  mov %0, %%eax         \n" // EAX = buffer
                     "                  leave                 \n"
                     "                  add $12, %%esp        \n" // Skip arguments and return address
	           	     "                  mov 24(%%eax), %%edx  \n" // EDX = arg length
	           	     "                  lea 28(%%eax), %%ecx  \n" // ECX = arg 0
                     "ovmrt_invoke_L1:                        \n" // Push arguments
	           	     "                  dec %%edx             \n"
            		     "                  cmp $0, %%edx         \n"
            		     "                  jl  ovmrt_invoke_L2   \n"
                     "                  push (%%ecx, %%edx, 4)\n"
                     "                  jmp ovmrt_invoke_L1   \n"
                     "ovmrt_invoke_L2:                        \n"
            		     "                  push 0(%%eax)         \n" // Push native code
                     "                  push 20(%%eax)        \n" // Push return address
            		     "                  mov  8(%%eax), %%edi  \n" // Copy old EDI
            		    "                  mov  12(%%eax), %%esi \n" // Copy old ESI
		             "                  mov  16(%%eax), %%ebx \n" // Copy old EBX
                     "                  jmp *4(%%eax)" : : 
		     "m"(buf));
}
#elif defined(OVM_PPC)
void* msg2receiver(struct ovm_core_execution_InvocationMessage* msg) {
    return (void*)msg->receiver;
}
void layoutParametersOnMemory(
    struct ovm_core_execution_InvocationMessage* msg,
    int* parameterArea,
    char* gprParameterInfoArea,
    char* fprParameterInfoArea) {
    int i;
    int gprInfoAreaOffset = 5;
    int fprInfoAreaOffset = 1;
    int arg_ptr = 2;
    ValueUnion* value;
    for(i = 0; i < 16; i++) {
        gprParameterInfoArea[i] = (char)0;
        fprParameterInfoArea[i] = (char)0;
    }
    for(i = 0; i < msg->inArgs->length; i++) {
       value = msg->inArgs->values[i];
       switch (value->tag) {
       case 'L': 
       case 'G':
       case '[': {
            if (gprInfoAreaOffset <= 10) {
                gprParameterInfoArea[gprInfoAreaOffset++] = (char)arg_ptr;
            }
            parameterArea[arg_ptr++] = (int)value->reference;
           break;
       }
       case 'D': {
            jwide w;
            w.jlong = value->widePrimitive;
            *((jwide*)&parameterArea[arg_ptr]) = w;
            if (fprInfoAreaOffset <= 13) {
                fprParameterInfoArea[fprInfoAreaOffset++] = (char)(0x80 | arg_ptr);

            }
            arg_ptr += 2;
            break;
       }
       case 'J': {
            jwide w;
            w.jlong = value->widePrimitive;
            *((jwide*)&parameterArea[arg_ptr]) = w;
            if (gprInfoAreaOffset <= 10) {
                gprParameterInfoArea[gprInfoAreaOffset++] = (char)arg_ptr;
            }
            arg_ptr++;
            if (gprInfoAreaOffset <= 10) {
                gprParameterInfoArea[gprInfoAreaOffset++] = (char)arg_ptr;
            }
            arg_ptr++;
            break;
       }
       case 'V':
            break;
       case 'F':
            if (fprInfoAreaOffset <= 13) {
                fprParameterInfoArea[fprInfoAreaOffset++] = (char)arg_ptr;
            }
            parameterArea[arg_ptr++] = (int)value->primitive;
            break;
       default:
            if (gprInfoAreaOffset <= 10) {
                gprParameterInfoArea[gprInfoAreaOffset++] = (char)arg_ptr;
            }
            parameterArea[arg_ptr++] = (int)value->primitive;
            break;
       }
    }
}

void ovmrt_invoke(struct s3_core_domain_S3ByteCode* bc, 
          struct ovm_core_execution_InvocationMessage* msg);
__asm__(".globl _ovmrt_invoke\n"
        "_ovmrt_invoke:\n"
        "   mflr r0\n"
        "   stw  r0, 8(r1)\n"
        "   stwu r1, -60(r1)\n" // allocate 60 (12 for linkage area + 8 for parameters + 40 for locals) bytes for stackframe
        "   stw  r4, 56(r1)\n" // save msg as a local, keep bc in r3
        "   stw  r3, 72(r1)\n" // save nc as the arg-1
        "   bl   _nc2fp\n"     // code pointer = nc2fp(nc)
        "   stw  r3, 52(r1)\n" // save code pointer as a local

        "   lwz  r3, 56(r1)\n" // load msg
        "   bl   _msg2receiver\n" // msg2receiver(msg)
        "   stw  r3, 76(r1)\n" // save receiver as arg0
        "   lwz  r3, 56(r1)\n" // load msg
        "   addi  r4, r1, 72\n" // compute the pointer to parameterArea
        "   addi  r5, r1, 20\n" // compute the address of the gprParameterInfoArea (16 byte long)
        "   addi  r6, r1, 36\n" // compute the address of the fprParameterInfoArea (16 byte long)
        "   bl   _layoutParametersOnMemory\n" // place the rest of the args on memory
        "   addi  r30, r1, 20\n" // r30 = gprParameterInfoArea
        "   addi  r29, r1, 36\n" // r29 = fprParameterInfoArea
        "   addi  r28, r1, 72\n" // r28 = parameterArea
        
        "   lbz   r27, 5(r30)\n" // R5
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_r5\n"
        "   slwi  r27, r27, 2\n"
        "   lwzx  r5,  r28, r27\n"
        "ovmrt_invoke_r5:\n"
        
        "   lbz   r27, 6(r30)\n" // R6
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_r6\n"
        "   slwi  r27, r27, 2\n"
        "   lwzx  r6,  r28, r27\n"
        "ovmrt_invoke_r6:\n"
        
        "   lbz   r27, 7(r30)\n" // R7
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_r7\n"
        "   slwi  r27, r27, 2\n"
        "   lwzx  r7,  r28, r27\n"
        "ovmrt_invoke_r7:\n"
        
        "   lbz   r27, 8(r30)\n" // R8
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_r8\n"
        "   slwi  r27, r27, 2\n"
        "   lwzx  r8,  r28, r27\n"
        "ovmrt_invoke_r8:\n"
        
        "   lbz   r27, 9(r30)\n" // R9
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_r9\n"
        "   slwi  r27, r27, 2\n"
        "   lwzx  r9,  r28, r27\n"
        "ovmrt_invoke_r9:\n"
        
        "   lbz   r27, 10(r30)\n" // R6
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_r10\n"
        "   slwi  r27, r27, 2\n"
        "   lwzx  r10,  r28, r27\n"
        "ovmrt_invoke_r10:\n"
        
        "   lbz   r27, 1(r29)\n" // F1
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f1_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f1_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f1, r28, r27\n"
        "   b     ovmrt_invoke_f1_1\n"
        "ovmrt_invoke_f1_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f1, r28, r27\n"
        "ovmrt_invoke_f1_1:\n"
        
        "   lbz   r27, 2(r29)\n" // F2
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f2_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f2_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f2, r28, r27\n"
        "   b     ovmrt_invoke_f2_1\n"
        "ovmrt_invoke_f2_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f2, r28, r27\n"
        "ovmrt_invoke_f2_1:\n"
        
        "   lbz   r27, 3(r29)\n" // F3
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f3_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f3_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f3, r28, r27\n"
        "   b     ovmrt_invoke_f3_1\n"
        "ovmrt_invoke_f3_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f3, r28, r27\n"
        "ovmrt_invoke_f3_1:\n"
        
        "   lbz   r27, 4(r29)\n" // F4
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f4_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f4_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f4, r28, r27\n"
        "   b     ovmrt_invoke_f4_1\n"
        "ovmrt_invoke_f4_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f4, r28, r27\n"
        "ovmrt_invoke_f4_1:\n"

        "   lbz   r27, 5(r29)\n" // F5
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f5_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f5_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f5, r28, r27\n"
        "   b     ovmrt_invoke_f5_1\n"
        "ovmrt_invoke_f5_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f5, r28, r27\n"
        "ovmrt_invoke_f5_1:\n"
        
        "   lbz   r27, 6(r29)\n" // F6
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f6_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f6_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f6, r28, r27\n"
        "   b     ovmrt_invoke_f6_1\n"
        "ovmrt_invoke_f6_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f6, r28, r27\n"
        "ovmrt_invoke_f6_1:\n"
        
        "   lbz   r27, 7(r29)\n" // F7
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f7_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f7_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f7, r28, r27\n"
        "   b     ovmrt_invoke_f7_1\n"
        "ovmrt_invoke_f7_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f7, r28, r27\n"
        "ovmrt_invoke_f7_1:\n"
        
        "   lbz   r27, 8(r29)\n" // F8
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f8_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f8_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f8, r28, r27\n"
        "   b     ovmrt_invoke_f8_1\n"
        "ovmrt_invoke_f8_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f8, r28, r27\n"
        "ovmrt_invoke_f8_1:\n"
        
        
        "   lbz   r27, 9(r29)\n" // F9
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f9_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f9_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f9, r28, r27\n"
        "   b     ovmrt_invoke_f9_1\n"
        "ovmrt_invoke_f9_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f9, r28, r27\n"
        "ovmrt_invoke_f9_1:\n"

        "   lbz   r27, 10(r29)\n" // F10
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f10_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f10_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f10, r28, r27\n"
        "   b     ovmrt_invoke_f10_1\n"
        "ovmrt_invoke_f10_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f10, r28, r27\n"
        "ovmrt_invoke_f10_1:\n"
        
        "   lbz   r27, 6(r29)\n" // F11
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f11_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f11_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f11, r28, r27\n"
        "   b     ovmrt_invoke_f11_1\n"
        "ovmrt_invoke_f11_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f11, r28, r27\n"
        "ovmrt_invoke_f11_1:\n"
        
        "   lbz   r27, 7(r29)\n" // F12
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f12_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f12_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f12, r28, r27\n"
        "   b     ovmrt_invoke_f12_1\n"
        "ovmrt_invoke_f12_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f12, r28, r27\n"
        "ovmrt_invoke_f12_1:\n"
        
        "   lbz   r27, 13(r29)\n" // F13
        "   cmpwi cr7, r27, 0\n"
        "   beq   cr7, ovmrt_invoke_f13_1\n"
        "   andi.  r26, r27, 0x80\n"
        "   cmpwi cr7, r26, 0\n"
        "   bne   cr7, ovmrt_invoke_f13_2\n"
        "   slwi  r27, r27, 2\n"
        "   lfsx  f13, r28, r27\n"
        "   b     ovmrt_invoke_f13_1\n"
        "ovmrt_invoke_f13_2:\n"
        "   andi. r27, r27, 0x7F\n"
        "   slwi  r27, r27, 2\n"
        "   lfdx  f13, r28, r27\n"
        "ovmrt_invoke_f13_1:\n"

        "   lwz  r3, 72(r1)\n"    // code object
        "   lwz  r4, 76(r1)\n"    // receiver
        "   lwz  r25, 52(r1)\n" // code pointer
        "   mtctr r25\n"        
        "   addi  r1, r1, 60\n"
        "   lwz  r0, 8(r1)\n" // restore LR
        "   mtlr r0\n"
        "   bctr\n"
);
#endif

		  
void ovmrt_start_tracing(jint a) {
    fprintf(stderr, "Start Tracing\n");
}

void ovmrt_stop_tracing() {
    fprintf(stderr, "Stop Tracing\n");
}

/*
 * GC support
 */
void *getOVMContextLoc(jint _nc) {
    struct s3_services_simplejit_NativeContext *nc = (struct s3_services_simplejit_NativeContext *) _nc;
    return &nc->ovm_context;
}

void *getEngineSpecificPtrLoc(jint nc) {
    return NULL;
}

/*
 * Conservative GC support.
 */
void *getStackBase(jint _nc) {
    NativeContext *nc = (NativeContext *) _nc;
    return (void*) nc->mtb->bottom_frame_pointer;
}

void *getStackTop(jint _nc) {
    NativeContext *nc = (NativeContext *) _nc;
    return (void*)nc->mtb->sp;
}

// In mthreads, all thread state is saved on the stack
void *getSavedRegisters(jint nc) { return 0; }
jint getSavedRegisterSize() { return 0; }

void *jit_topFrame(int _nc) {
  NativeContext *nc = (NativeContext *) _nc;
  if (nc == currentContext)
    // Skip invoke_native(), jit_topFrame()
    return getCurrentActivation(2);
  else
    return (void *) mthread_top_frame(nc->mtb);
}

void *jit_bottomFrame(int nc) {
  return (void *)((NativeContext *) nc)->mtb->bottom_frame_pointer;
}

void* jit_getCode(void *frameHandle) {
  return *(((void **) frameHandle) + getStackFrameCodeOffset());
}

void *jit_getLocalAddress(jint _nc, void * _frameHandle, jint index) {
    void** frameHandle = (void **) _frameHandle;
    NativeCode* cf;
    int maxLocals, argLen, localOffset;

    cf = (NativeCode *) jit_getCode(_frameHandle);
    maxLocals = cf->maxLocals_;
    argLen = cf->argLen_;
    localOffset = getStackFrameLocalOffset(maxLocals, argLen, index);
    return frameHandle + localOffset;
}

void *jit_getOperandAddress(jint _nc, void * _frameHandle, jint index) {
    int maxLocals, argLen, stackOffset;
    void ** frameHandle = (void **) _frameHandle;
    NativeCode* cf = jit_getCode(_frameHandle);

    maxLocals = cf->maxLocals_;
    argLen = cf->argLen_;
    stackOffset = getStackFrameOperandStackOffset(maxLocals, argLen, index);
    return (frameHandle + stackOffset);
}

void jit_setOpstackDepth(jint _nc, void * _frameHandle, jint depth) {
    struct s3_services_simplejit_NativeContext* nc = (struct s3_services_simplejit_NativeContext *) _nc;
    int* frameHandle = (int *) _frameHandle;
    nc->cut_to_frame = (int)frameHandle;
    nc->new_stack_depth = depth;
    nc->cut_to_frame_set = 1;
}

void jit_cutToActivation(int _nc, void *_calleeFrame) {
    NativeContext *nc = (NativeContext *) _nc;
    int *calleeFrame = (int *) _calleeFrame;
    int *frameHandle = (int *) getCaller(_calleeFrame);
    NativeCode* cf;
    int stackOffset;
    register int* pc;
#if defined(OVM_X86)
    register int* esp;
    register int* ebp;
#elif defined(OVM_PPC)
    register int* sp;
    register int* opsp;

    //fprintf(stderr, "jit_cutToActivation called on PPC\n");
#endif

    pc = (int*)getCallerPC(calleeFrame);
    cf = (NativeCode*)*(frameHandle + getStackFrameCodeOffset());

    nc->processing_stack_overflow = NO;

    if (! nc->cut_to_frame_set) {
	fprintf(stderr, "Error in cut_to_activation() flag not set\n");
    }

    nc->cut_to_frame_set = 0;

    stackOffset = getStackFrameOperandStackOffset(cf->maxLocals_,
						  cf->argLen_,
						  nc->new_stack_depth - 1);

    /*
    fprintf(stderr, "maxLocals = %x\n", cf->maxLocals_);
    fprintf(stderr, "argLen = %x\n", cf->argLen_);
    fprintf(stderr, "frameHandle = %x\n", (int)frameHandle);
    fprintf(stderr, "nc->cut_to_frame = %x\n", nc->cut_to_frame);
    fprintf(stderr, "nc->new_stack_depth = %x\n", nc->new_stack_depth);
    fprintf(stderr, "stackOffset = %x\n", stackOffset);
    fprintf(stderr, "ebp = %x\n", (int)frameHandle);
    fprintf(stderr, "esp = %x\n", (int)(frameHandle + stackOffset));
    fprintf(stderr, "pc = %x\n", (int)(pc));
    */

#if defined(OVM_X86)

    ebp = frameHandle;
    esp = (frameHandle + stackOffset);

    __asm__ volatile("mov %0, %%esp \n"
                     "mov %1, %%ebp \n"
                     "mov -4(%%ebp), %%edi \n"
                     "mov -8(%%ebp), %%esi \n"
                     "mov -12(%%ebp), %%ebx \n"
                     "jmp *%2" : : "r"(esp), "r"(ebp), "r"(pc) );
#elif defined(OVM_PPC)
    sp = calleeFrame;
    opsp = (frameHandle + stackOffset);
    
    __asm__ volatile("mr r31, %0 \n" // assume r31 is used as the operand stack pointer for unoptimized simplejit
                     "mr r1,  %1 \n"
                     "mr r0,  %2 \n"
                     "mtctr r0   \n"
                     "bctr       \n" : : "r"(opsp), "r"(sp), "r"(pc));

#endif
}

jint ovmrt_get_process_arg_count() {
    return (jint)process_argc;
}

void ovmrt_run(int processorHandle, struct s3_services_simplejit_NativeContext* to_nc) {
    struct s3_services_simplejit_NativeContext* from_nc;
    if ( processorHandle != 0 ) { /* FIXME for more than 1 processor */
      	fprintf(stderr, "Can't handle processor %d yet!\n", processorHandle);
	abort();
    }
    from_nc = currentContext;
    currentContext = to_nc;
    mthread_context_switch(&from_nc->mtb->sp, &to_nc->mtb->sp);
}


jboolean check_stack_overflow() {
    int current_frame_pointer = (int)getCurrentActivation(0/*(int)currentContext*/);
    int space_left = current_frame_pointer - (int)currentContext->mtb->stack_top;
    if (currentContext->processing_stack_overflow == NO &&
	MTHREAD_ROOM_FOR_STACK_OVERFLOW_PROCESSING > space_left) {
	currentContext->processing_stack_overflow = YES;
	return 1;
    }
    return 0;
}

void hit_unrewritten_code() {
    fprintf(stderr, "Hit an unrewritten instruction...\n");
    //abort();
}

void memDebugHook(int lineNumber, jbyte* methodName) {
    // Filip, fill me in!
}

#ifdef removed_on_branch_changed_on_head
static inline jboolean CAS32(void *_target, jint oldval, jint newval) {
  jint *target = (jint *) _target; /* C++ compatiblity */
  jboolean same = *target == oldval;
  if (same)
    *target = newval;
  return same;
}
#endif

#if defined(OVM_X86)

static char* segvHandler_esp;
static int* segvHandler_ebp;
static char* segvHandler_edi;
static char* segvHandler_esi;
static char* segvHandler_ebx;
static char* segvHandler_eip;

#elif defined(OVM_PPC)
int call_sigprocmask(int how, const sigset_t *set, sigset_t *oset) {
    return sigprocmask(how, set, oset);
}
void print1() {
    fprintf(stderr, "print1\n");
    fflush(stderr);
}

static int* segvHandler_gpr1; // stack pointer
static unsigned int segvHandler_pc;

static int ppc_gpr[32];
#endif

static int segvHandler_exceptionCode;
static int segvHandler_offsetCodeInStack;
static NativeCode* segvHandler_nativeCode;
static struct ovm_core_execution_CoreServicesAccess* segvHandler_csaObject;
static struct s3_core_domain_S3Blueprint* segvHandler_csaBlueprint;
static struct arr_ovm_core_domain_Code* segvHandler_vtable;
static NativeCode* segvHandler_generateThrowableNativeCode;
static char* segvHandler_generateThrowableCodeEntry;
sigset_t normalMask;
int sig_setmask = SIG_SETMASK;

int _dummy0;
void _dummy(unsigned int _) { _dummy0 = _; }

void segvHandler(int sig, siginfo_t * info, void *_ctx) {
    //    fprintf(stderr, "segvHandler(%d, %p, %p)\n", sig, info, _ctx);
    //    fflush(stderr);

    int pgsize = getpagesize();

    ucontext_t* ctx = (ucontext_t*)_ctx;
    normalMask = ctx->uc_sigmask;
    char *faultAddr = (char *) FAULT_ADDR(info, _ctx);
    //    fprintf(stderr, "fault address = 0x%x\n", (int)faultAddr);
    //    fflush(stderr);
    if (faultAddr >= (char*)pgsize) {
    	abort();
    }

    segvHandler_exceptionCode = NULL_POINTER_EXCEPTION;
#if defined(OVM_X86) && defined(LINUX_BUILD)
    int *gregs = ctx->uc_mcontext.gregs;
    segvHandler_esp = gregs[7/*REG_ESP*/];
    segvHandler_ebp = gregs[6/*REG_EBP*/];
    segvHandler_edi = gregs[4/*REG_EDI*/];
    segvHandler_esi = gregs[5/*REG_ESI*/];
    segvHandler_ebx = gregs[8/*REG_EBX*/];
    segvHandler_eip = gregs[14/*REG_EIP*/];
#elif defined(OVM_PPC)

    unsigned int spc = ctx->uc_mcontext->ss.srr0;// + 4;
    //    fprintf(stderr, "spc 0x%x\n", spc);
    //    fflush(stderr);

    segvHandler_pc = spc;
    //		fprintf(stderr, "segvHandler_pc 0x%x\n", segvHandler_pc);
    //		fflush(stderr);
    // some voodoo for famine
    _dummy(segvHandler_pc);

    segvHandler_gpr1 = (int*)ctx->uc_mcontext->ss.r1;


#else
#warning "simplejit segv handler not supported"
#endif
    segvHandler_offsetCodeInStack = getStackFrameCodeOffset();

#if defined(OVM_X86)
    segvHandler_nativeCode = *(NativeCode**) (segvHandler_ebp + segvHandler_offsetCodeInStack);
#elif defined(OVM_PPC)
    segvHandler_nativeCode = *(NativeCode**) ((int*)*segvHandler_gpr1 + segvHandler_offsetCodeInStack);
#endif
    segvHandler_csaObject = segvHandler_nativeCode->myCSA_;
    segvHandler_csaBlueprint = HEADER_BLUEPRINT(((struct HEADER*)segvHandler_csaObject));
    segvHandler_vtable = segvHandler_csaBlueprint->vTable;
    segvHandler_generateThrowableNativeCode = segvHandler_vtable->values[csa_generateThrowable_index];
    segvHandler_generateThrowableCodeEntry = (char*)nc2fp(segvHandler_generateThrowableNativeCode);

    //    fprintf(stderr, "generateThrowableCodeEntry = 0x%x\n", (int)segvHandler_generateThrowableCodeEntry);
    //    fflush(stderr);

//    fprintf(stderr, "segvHandler2(%d, %p, %p)\n", sig, info, _ctx);
//    fflush(stderr);

#if defined(OVM_X86) && defined(LINUX_BUILD)
    asm("movl %0, %%edi\n"
        "movl %1, %%esi\n"
        "movl %2, %%ebx\n"
        "movl %3, %%ebp\n"
        "movl %4, %%esp\n"
        "pushl $0\n" // zero
        "pushl %5\n" // exception ID
        "pushl %6\n" // CSA object
        "pushl %7\n" // CSA method object
        "pushl %8\n"
        "pushl $0\n"  // push 0
        "pushl %9\n" // push &normalMask
        "pushl %10\n" // push SIG_SETMASK
        "call sigprocmask\n" // call sigprocmask
        "add  $12, %%esp\n"  // restore sp
        "jmp *%11\n"  // call generateThrowable
        : :
        "m"(segvHandler_edi), "m"(segvHandler_esi), "m"(segvHandler_ebx),
        "m"(segvHandler_ebp), "m"(segvHandler_esp), "m"(segvHandler_exceptionCode),
        "m"(segvHandler_csaObject), "m"(segvHandler_generateThrowableNativeCode),
        "m"(segvHandler_eip), 
        "p"(&normalMask),
        "i"(SIG_SETMASK),
        "m"(segvHandler_generateThrowableCodeEntry));
#elif defined(OVM_PPC)
    // Restore GPR13-31, FPR14-31, GPR1 (SP)
    struct ppc_thread_state *ss = &ctx->uc_mcontext->ss;
    struct ppc_float_state *fs = &ctx->uc_mcontext->fs;
    asm(
	"mr  r4, %0\n"
	"mr  r1, %1\n"
	"lwz  r2, 0(r1)\n"

	"lwz  r3, 16(r4)\n\t" // r17 // See s3/services/simplejit/powerpc/StackLayoutImpl.java for the stack layout
	"stw  r3, -60(r2)\n"
	"lwz  r3, 20(r4)\n\t" // r18
	"stw  r3, -56(r2)\n"
	"lwz  r3, 24(r4)\n\t" // r19
	"stw  r3, -52(r2)\n"
	"lwz  r3, 28(r4)\n\t" // r20
	"stw  r3, -48(r2)\n"
	"lwz  r3, 32(r4)\n\t" // r21
	"stw  r3, -44(r2)\n"
	"lwz  r3, 36(r4)\n\t" // r22
	"stw  r3, -40(r2)\n"
	"lwz  r3, 40(r4)\n\t" // r23
	"stw  r3, -36(r2)\n"
	"lwz  r3, 44(r4)\n\t" // r24
	"stw  r3, -32(r2)\n"
	"lwz  r3, 48(r4)\n\t" // r25
	"stw  r3, -28(r2)\n"
	"lwz  r3, 52(r4)\n\t" // r26
	"stw  r3, -24(r2)\n"
	"lwz  r3, 56(r4)\n\t" // r27
	"stw  r3, -20(r2)\n"
	"lwz  r3, 60(r4)\n\t" // r28
	"stw  r3, -16(r2)\n"
	"lwz  r3, 64(r4)\n\t" // r29
	"stw  r3, -12(r2)\n"
	"lwz  r3, 68(r4)\n\t" // r30
	"stw  r3, -8(r2)\n"
	"lwz  r3, 72(r4)\n\t" // r31
	"stw  r3, -4(r2)\n"

	"mr   r4, %10\n"     // &fp->fpregs[14]
	/*
	"lfd  f1, 0(r4)\n"    // f14
	"stfd f1, -272(r2)\n"
	"lfd  f1, 8(r4)\n"    // f15
	"stfd f1, -264(r2)\n"
	"lfd  f1, 16(r4)\n"   // f16
	"stfd f1, -256(r2)\n"
	*/
	"lfd  f1, 24(r4)\n"   // f17
	"stfd f1, -248(r2)\n"
	"lfd  f1, 32(r4)\n"   // f18
	"stfd f1, -240(r2)\n"
	"lfd  f1, 40(r4)\n"   // f19
	"stfd f1, -232(r2)\n"
	"lfd  f1, 48(r4)\n"   // f20
	"stfd f1, -224(r2)\n"
	"lfd  f1, 56(r4)\n"   // f21
	"stfd f1, -216(r2)\n"
	"lfd  f1, 64(r4)\n"   // f22
	"stfd f1, -208(r2)\n"
	"lfd  f1, 72(r4)\n"   // f23
	"stfd f1, -200(r2)\n"
	"lfd  f1, 80(r4)\n"   // f24
	"stfd f1, -192(r2)\n"
	"lfd  f1, 88(r4)\n"   // f25
	"stfd f1, -184(r2)\n"
	"lfd  f1, 96(r4)\n"   // f26
	"stfd f1, -176(r2)\n"
	"lfd  f1, 104(r4)\n"  // f27
	"stfd f1, -168(r2)\n"
	"lfd  f1, 112(r4)\n"  // f28
	"stfd f1, -160(r2)\n"
	"lfd  f1, 104(r4)\n"  // f29
	"stfd f1, -152(r2)\n"
	"lfd  f1, 112(r4)\n"  // f30
	"stfd f1, -144(r2)\n"
	"lfd  f1, 120(r4)\n"  // f31
	"stfd f1, -136(r2)\n"

	"li   r5, 0\n"     // push 0
	"stw  r5, 20(r1)\n"
	"mr  r4, %2\n"   // push &normalMask
	"stw  r4, 16(r1)\n"
	"mr  r3, %3\n"   // push SIG_SETMASK
	"stw  r3, 12(r1)\n"
	"mr   r13, %9\n"
	"stw  r0,  0(r13)\n"
	"stw  r2,  8(r13)\n"
	"stw  r7,  28(r13)\n"
	"stw  r8,  32(r13)\n"
	"stw  r9,  36(r13)\n"
	"stw  r10, 40(r13)\n"
	"stw  r11, 44(r13)\n"
	"stw  r12, 48(r13)\n"
	"bl _call_sigprocmask\n" // call sigprocmask
	"lwz  r0,  0(r13)\n"
	"lwz  r2,  8(r13)\n"
	"lwz  r7,  28(r13)\n"
	"lwz  r8,  32(r13)\n"
	"lwz  r9,  36(r13)\n"
	"lwz  r10, 40(r13)\n"
	"lwz  r11, 44(r13)\n"
	"lwz  r12, 48(r13)\n"
	"mr  r4, %5\n"  // CSA object
	"mr  r3, %6\n"  // CSA method object
	"li   r6, 0\n"    // zero
	//	"mr  r5, %4\n"  // exception ID
	"li  r5, 0\n"
	"mtlr %7\n" // PC
	"mtctr %8\n" // call generateThrowable
	"bctr\n"
	: :
	"r"(&ss->r13),
	"r"(ss->r1),
        "r"(&normalMask),
        "r"(SIG_SETMASK),
	"r"(segvHandler_exceptionCode),
        "r"(segvHandler_csaObject), 
	"r"(segvHandler_generateThrowableNativeCode),
	"r"(ss->srr0),
	"r"(segvHandler_generateThrowableNativeCode->_parent_._parent_.foreignEntry),
	"r"(&ppc_gpr[0]),
	"r"(&fs->fpregs[14])
    	: "r0", "r1", "r2", "r3", "r4", "r5", "r6", //"r13", "r14", "r15", "r16",
    	  "r17", "r18", "r19", "r20", "r21", "r22", "r23", "r24", 
    	  "r25", "r26", "r27", "r28", "r29", "r30", "r31");
#endif
}


union dl {
	double d;
	long long l;
};
void toStringHook(double d) {
	union dl dl;
	dl.d = d;
	printf("toStringHook : %f (%llx)\n", d, dl.l);
}

jint fcmpg(float f1, float f2) {
	jvalue v1 = (jvalue) f1;
	jvalue v2 = (jvalue) f2;
    if (v1.jint == 0x7fc00000 || v2.jint == 0x7fc00000) { // NaNf
    	return 1;
    } else if (f1 == f2) {
    	return 0;
    } else if (f1 > f2) {
    	return 1;
    } else {
    	return -1;
    }	
}

jdouble f2d(float f) { 
    jvalue v = (jvalue) f;
    jwide wv;
    if (v.jint == 0x7fc00000) { // NaNf
    	wv.jlong = 0x7ff8000000000000LL;
    	return wv.jdouble;
    } else if (v.jint == 0x7f800000) { // Float.POSITIVE_INFINITY
     	wv.jlong = 0x7ff0000000000000LL;
    	return wv.jdouble;  
    } else if (v.jint == 0xff800000) { // Float.NEGATIVE_INFINITY
     	wv.jlong = 0xfff0000000000000LL;
    	return wv.jdouble;
    }
    return (jdouble)f; 
}
jlong f2l(float f) { 
    jvalue v = (jvalue) f;
    return f != f  
	? (jlong) 0 // NaN
	: v.jint == 0x7f800000 // Float.POSITIVE_INFINITY
	? (jlong) 0x7fffffffffffffffLL
	: v.jint == 0xff800000 // Float.NEGATIVE_INFINITY
	? (jlong) 0x8000000000000000LL
	: (jlong) f; 
}
jlong d2l(double d) { 
    jwide v = (jwide) d;
    return d != d
	? (jlong) 0 // NaN
	: v.jlong == 0x7ff0000000000000LL
	? (jlong) 0x7fffffffffffffffLL
	: v.jlong == 0xfff0000000000000LL
	? (jlong) 0x8000000000000000LL
	: (jlong) d; 
}
jint f2i(float f) { 
    jvalue v = (jvalue) f;
    return f != f  
	? (jint) 0 // NaN
	: v.jint == 0x7f800000 // Float.POSITIVE_INFINITY
	? (jint) 0x7fffffff
	: v.jint == 0xff800000 // Float.NEGATIVE_INFINITY
	? (jint) 0x80000000
	: (jint) f; 
}
jint d2i(double d) { 
    jwide v = (jwide) d;
    return d != d
	? (jint) 0 // NaN
	: v.jlong == 0x7ff0000000000000LL
	? (jint) 0x7fffffff
	: v.jlong == 0xfff0000000000000LL
	? (jint) 0x80000000
	: (jint) d; 
}
float l2f(jlong l) { return (float) l; }
double l2d(jlong l) { return (double) l; }

void defineRuntimeFunctionTable() {
    OVM_printf = printf;
    OVM_fflush_out = ovmrt_fflush_out;
    OVM_is_subtype_of = ovmrt_is_subtype_of;
    OVM_ldiv = ovmrt_ldiv;
    OVM_lrem = ovmrt_lrem;
    OVM_fmod = fmod;
    OVM_f2l = f2l;
    OVM_d2l = d2l;
    OVM_f2i = f2i;
    OVM_d2i = d2i;
    OVM_l2f = l2f;
    OVM_l2d = l2d;
    OVM_f2d = f2d;
    OVM_fcmpg = fcmpg;
    OVM_memmove = memmove;
    OVM_check_stack_overflow = check_stack_overflow;

    OVM_SYSTEM_NEW_CONTEXT = ovmrt_newNativeContext;
    OVM_SYSTEM_GET_CONTEXT = ovmrt_getCurrentOVMContext;
    OVM_SYSTEM_GET_ACTIVATION = getCurrentActivation;
    OVM_SYSTEM_MAKE_ACTIVATION = ovmrt_makeActivation;
    OVM_SYSTEM_INVOKE = ovmrt_invoke;
    OVM_SYSTEM_START_TRACING = ovmrt_start_tracing;
    OVM_SYSTEM_STOP_TRACING = ovmrt_stop_tracing;
    OVM_SYSTEM_DESTROY_NATIVE_CONTEXT = ovmrt_destroyNativeContext;
    OVM_SYSTEM_RUN = ovmrt_run;

    OVM_eventPollcheck = eventPollcheck;

    OVM_invoke_native = invoke_native;

    OVM_hit_unrewritten_code = hit_unrewritten_code;

    OVM_eventUnion = &eventUnion;

    OVM_memDebugHook = memDebugHook;

    OVM_currentContext = (void*)&currentContext;

    OVM_CAS32 = CAS32;

    installRuntimeFunctionTable(); // this has to be the last statement
}

