/*
 * The mthread routines
 * @author Hiroshi Yamauchi
 */
#include "runtime.h"
#include "mthread.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>

int main_sp;


void return_point() {
    fprintf(stderr, 
	    "Error : A mthread returned to the bottom of the native call stack\n");
    fflush(stderr);
    exit(1);
}

void* get_return_point_address() {
    return (void*)return_point;
}

void dummy_entry_point() {
    fprintf(stdout, 
        "Ok: dummy_entry_point() was called. Exit\n");
    fflush(stdout);
    exit(0);
}

typedef struct s3_services_simplejit_MThreadBlock mthread_block;

/* Limitation: only integers and references are allowed in the argument 'arg'.
 * This is because the type information of the arguments are not available */
mthread_block *create_mthread(void* entry_point,
			      int stack_size,
			      int argWordSize,
			      void* arg[]) {
    int* call_stack, *sp, *org_sp;
    struct s3_services_simplejit_MThreadBlock* mtb;
    int pagesize;
    int i;
#if defined(OVM_X86)
    int *prev_fp, *fp;
#elif defined(OVM_PPC)
    int *prev_sp;
    
    //    fprintf(stderr, "create_mthread called on PPC\n");
#elif defined(OVM_ARM) || defined (OVM_SPARC)
#error "simplejit is not available in this configuration."
#endif

    if (entry_point == NULL || stack_size < 0)
		return NULL;

    if (stack_size < MTHREAD_MINIMUM_STACK_SIZE)
		stack_size = MTHREAD_MINIMUM_STACK_SIZE;

    /* round stack size up to a page boundary and room for an
     * unreadable page at the bottom. */
    pagesize = getpagesize();
    stack_size += 2*pagesize - 1;
    stack_size &= ~(pagesize - 1);

#if defined(OVM_X86)    
    call_stack = (int*)mmap(0, stack_size, PROT_READ|PROT_WRITE,
			    MAP_PRIVATE|MAP_ANON, -1, 0);
#elif defined(OVM_PPC)
    call_stack = (int*)malloc(stack_size);
#endif

    if (call_stack == (void *) -1) {
		perror("can't map thread stack");
		return NULL;
    }

    /* catch overflow */
    mprotect(call_stack, pagesize, PROT_NONE);
    
    mtb = (struct s3_services_simplejit_MThreadBlock*)
	malloc(sizeof(struct s3_services_simplejit_MThreadBlock));
    if (!mtb) {
#if defined(OVM_X86)
		munmap(call_stack, stack_size);
#elif defined(OVM_PPC)
        free(call_stack);
#endif
		return NULL;
    }

    /* set up the stack for the architecture */
	sp = (int*)((char*)call_stack + stack_size);
#if defined(OVM_X86)
    prev_fp = sp;
    for(i = argWordSize - 1; i >= 0; i--)
		*--sp = (int)arg[i];
    *--sp = (int)return_point;
    *--sp = (int)entry_point;
    *--sp = (int)prev_fp;
    fp = sp;
    org_sp = sp;
    *--sp = 0; /* eax */
    *--sp = 0; /* ecx */
    *--sp = 0; /* edx */
    *--sp = 0; /* ebx */    
    *--sp = (int)org_sp; /* esp */
    *--sp = (int)fp; /* ebp*/
    *--sp = 0; /* esi */
    *--sp = 0; /* edi */
#elif defined(OVM_PPC)
    sp--;
    sp--;
    sp--;
    sp--;
    prev_sp = sp;
	/*prev_sp = sp;*/
	/* Note we only allow integers and references which are to be stored in GPRs */
    //if (argWordSize > 8) {
        for(i = argWordSize - 1; i >= 0; i--) {
            *--sp = (int)arg[i];
        }
    //}
    *--sp = 0; /* LR */
    *--sp = 0; /* CR */
    *--sp = (int)prev_sp;
    //prev_sp = sp;
    //org_sp = sp;
    /* Lay out the dummy values as if an stmw saved them */
    for(i = 31; i >= 0; i--)
        *--sp = 0; /* ri */
    /* r0 = entry_point */
    *sp = (int)entry_point;
    //*sp = (int)dummy_entry_point; // TEMPORARY until the code generator can generate code
    
    /* Write arg into the area where r3-r10 are saved in mthread_context_switch. */
    for(i = 0; i <= 7; i++)
        *(sp + i + 3) = (int)arg[i];
    /* Layout the dummy fp register values (8 x 32 bytes) */
    for(i = 0; i < 64; i++)
        *--sp = 0;
    // allocate two words in case mthread_context_switch calls a function
    *--sp = 0;
    *--sp = 0;
#endif

    mtb->stack_top = (int)call_stack;
    mtb->redzone = (int)(call_stack) + MTHREAD_ROOM_FOR_STACK_OVERFLOW_PROCESSING;
    mtb->stack_size = stack_size;
    mtb->sp = (int)sp;

#if defined(OVM_X86)
    mtb->bottom_frame_pointer = (int)prev_fp;
#elif defined(OVM_PPC)
    mtb->bottom_frame_pointer = (int)prev_sp;
#endif

    return mtb;
}

void destroy_mthread(mthread_block *mtb) {
#if defined(OVM_X86)
    munmap((void*)mtb->stack_top, mtb->stack_size);
#elif defined(OVM_PPC)
    fprintf(stderr, "destroy_mthread called on PPC\n");
    free(mtb->stack_top);
#endif
    free(mtb);
}

 /* Return the frame pointer of the frame under the mthread_context_switch frame */
int mthread_top_frame(mthread_block *mtb) {
  int *sp = (int *) mtb->sp;
#if defined(OVM_X86)
  return sp[8];
#elif defined(OVM_PPC)
    fprintf(stderr, "mthread_top_frame called on PPC\n");
  return sp[392/4];
#endif
}
  
/* 
 * mthread_context_switch
 */
#if defined(OVM_X86)
__asm__(
#ifdef OSX_BUILD
	".globl _mthread_context_switch\n"
	"_mthread_context_switch:\n"
#else
	".global mthread_context_switch\n"
	"mthread_context_switch:\n"
#endif
	"    push %ebp\n"
	"    mov %esp, %ebp\n"
	"    pusha\n"
	"    mov 8(%ebp), %ebx\n"
	"    mov %esp, (%ebx) \n"
	"    mov 12(%ebp), %ebx\n"
	"    mov (%ebx), %esp\n"
	"    popa\n"
	"    leave\n"
	"    ret\n");
#elif defined(OVM_PPC)
__asm__(".globl _mthread_context_switch\n"
	"_mthread_context_switch:\n" /* assume the two arguments are on r3 and r4 */
    "   mflr r0\n"
    "   stw  r0, -128(r1)\n" /* save LR as r0 */
    "   stmw r2, -120(r1)\n" /* do not save r0 and r1 */
    "   stfd f31, -136(r1)\n"
    "   stfd f30, -144(r1)\n"
    "   stfd f29, -152(r1)\n"
    "   stfd f28, -160(r1)\n"
    "   stfd f27, -168(r1)\n"
    "   stfd f26, -176(r1)\n"
    "   stfd f25, -184(r1)\n"
    "   stfd f24, -192(r1)\n"
    "   stfd f23, -200(r1)\n"
    "   stfd f22, -208(r1)\n"
    "   stfd f21, -216(r1)\n"
    "   stfd f20, -224(r1)\n"
    "   stfd f19, -232(r1)\n"
    "   stfd f18, -240(r1)\n"
    "   stfd f17, -248(r1)\n"
    "   stfd f16, -256(r1)\n"
    "   stfd f15, -264(r1)\n"
    "   stfd f14, -272(r1)\n"
    "   stfd f13, -280(r1)\n"
    "   stfd f12, -288(r1)\n"
    "   stfd f11, -296(r1)\n"
    "   stfd f10, -304(r1)\n"
    "   stfd  f9, -312(r1)\n"
    "   stfd  f8, -320(r1)\n"
    "   stfd  f7, -328(r1)\n"
    "   stfd  f6, -336(r1)\n"
    "   stfd  f5, -344(r1)\n"
    "   stfd  f4, -352(r1)\n"
    "   stfd  f3, -360(r1)\n"
    "   stfd  f2, -368(r1)\n"
    "   stfd  f1, -376(r1)\n"
    "   stfd  f0, -384(r1)\n"
    "   addi  r1, r1, -392\n"
    "   stw   r1, 0(r3)\n"
    "   lwz   r1, 0(r4)\n"
#if 1
    /* put the address of return_point() so that 
     * we can detect a return from the bottom of the mthread stack */
    "   bl     _get_return_point_address\n"
    "   mtlr   r3\n"
#elif
    /* put some garbage into LR so that we can detect 
     * a return from the bottom of the mthread stack */
//    "   lis    r31, 0xCAFE\n"
//    "   ori    r31, r31, 0xBABE\n"
//    "   mtlr  r31\n"
#endif
    "   addi  r1, r1, 392\n"
    "   lmw   r2, -120(r1)\n"
    "   lwz   r0, -128(r1)\n"
    "   lfd f31, -136(r1)\n"
    "   lfd f30, -144(r1)\n"
    "   lfd f29, -152(r1)\n"
    "   lfd f28, -160(r1)\n"
    "   lfd f27, -168(r1)\n"
    "   lfd f26, -176(r1)\n"
    "   lfd f25, -184(r1)\n"
    "   lfd f24, -192(r1)\n"
    "   lfd f23, -200(r1)\n"
    "   lfd f22, -208(r1)\n"
    "   lfd f21, -216(r1)\n"
    "   lfd f20, -224(r1)\n"
    "   lfd f19, -232(r1)\n"
    "   lfd f18, -240(r1)\n"
    "   lfd f17, -248(r1)\n"
    "   lfd f16, -256(r1)\n"
    "   lfd f15, -264(r1)\n"
    "   lfd f14, -272(r1)\n"
    "   lfd f13, -280(r1)\n"
    "   lfd f12, -288(r1)\n"
    "   lfd f11, -296(r1)\n"
    "   lfd f10, -304(r1)\n"
    "   lfd  f9, -312(r1)\n"
    "   lfd  f8, -320(r1)\n"
    "   lfd  f7, -328(r1)\n"
    "   lfd  f6, -336(r1)\n"
    "   lfd  f5, -344(r1)\n"
    "   lfd  f4, -352(r1)\n"
    "   lfd  f3, -360(r1)\n"
    "   lfd  f2, -368(r1)\n"
    "   lfd  f1, -376(r1)\n"
    "   lfd  f0, -384(r1)\n"
    "   mtctr r0\n"
	"   bctr\n"
    );
#endif
 
