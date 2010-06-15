/*
 * The mthread routines
 * @author Hiroshi Yamauchi
 */
#include "config.h"
#include "mthread.h"
#include <stdio.h>
#include <sys/mman.h>

#if USE_MTHREAD == YES

int main_sp;

void return_point() {
    fprintf(stderr, 
	    "Error : A mthread returned to the bottom of the native call stack\n");
    fflush(stderr);
    exit(1);
}

struct mthread_block* create_mthread(void* entry_point,
				     int stack_size,
				     void* arg) {
    int* call_stack, *sp, *prev_bp, *bp, *org_sp;
    struct mthread_block* mtb;
    int pagesize;

    if (entry_point == NULL || stack_size < 0)
	return NULL;

    if (stack_size < MTHREAD_MINIMUM_STACK_SIZE)
	stack_size = MTHREAD_MINIMUM_STACK_SIZE;

    /* round stack size up to a page boundary and room for an
     * unreadable page at the bottom.
     */
    pagesize = getpagesize();
    stack_size += 2*pagesize - 1;
    stack_size &= ~(pagesize - 1);
    
    call_stack = (int*)mmap(0, stack_size, PROT_READ|PROT_WRITE,
			    MAP_PRIVATE|MAP_ANON, -1, 0);
    if (call_stack == (void *) -1) {
	perror("can't map thread stack");
	return NULL;
    }

    /* catch overflow */
    mprotect(call_stack, pagesize, PROT_NONE);
    
    mtb = (struct mthread_block*)malloc(sizeof(struct mthread_block));
    if (!mtb) {
	munmap(call_stack, stack_size);
	return NULL;
    }

    /* set up the stack for the Intel architecture */
    sp = (int*)((char*)call_stack + stack_size);
    prev_bp = sp;
    *--sp = (int)arg;
    *--sp = (int)return_point;
    *--sp = (int)entry_point;
    *--sp = (int)prev_bp;
    bp = sp;
    org_sp = sp;
    *--sp = 0; /* eax */
    *--sp = 0; /* ecx */
    *--sp = 0; /* edx */
    *--sp = 0; /* ebx */
    *--sp = (int)org_sp; /* esp */
    *--sp = (int)bp; /* ebp*/
    *--sp = 0; /* esi */
    *--sp = 0; /* edi */

    mtb->stack_bottom = (void*)call_stack;
    mtb->stack_size = stack_size;
    mtb->sp = sp;

    return mtb;
}

void destroy_mthread(struct mthread_block *mtb) {
    munmap(mtb->stack_bottom, mtb->stack_size);
    free(mtb);
}

/* 
 * mthread_context_switch
 */
__asm__(".global mthread_context_switch\n"
	"mthread_context_switch:\n"
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

#endif /* USE_MTHREAD */
