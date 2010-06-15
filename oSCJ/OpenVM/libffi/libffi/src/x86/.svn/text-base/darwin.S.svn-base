/* -----------------------------------------------------------------------
   sysv.S - Copyright (c) 1996, 1998, 2001, 2002  Cygnus Solutions
   
   X86 Foreign Function Interface 

   Permission is hereby granted, free of charge, to any person obtaining
   a copy of this software and associated documentation files (the
   ``Software''), to deal in the Software without restriction, including
   without limitation the rights to use, copy, modify, merge, publish,
   distribute, sublicense, and/or sell copies of the Software, and to
   permit persons to whom the Software is furnished to do so, subject to
   the following conditions:

   The above copyright notice and this permission notice shall be included
   in all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED ``AS IS'', WITHOUT WARRANTY OF ANY KIND, EXPRESS
   OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
   IN NO EVENT SHALL CYGNUS SOLUTIONS BE LIABLE FOR ANY CLAIM, DAMAGES OR
   OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
   ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
   OTHER DEALINGS IN THE SOFTWARE.
   ----------------------------------------------------------------------- */

#ifndef __x86_64__

#define LIBFFI_ASM	
#include <fficonfig.h>
#include <ffi.h>

.text

.globl _ffi_prep_args

	.align 1,0x90
.globl _ffi_call_DARWIN
_ffi_call_DARWIN:
LFB1:
        pushl %ebp
LCFI0:
        movl  %esp,%ebp
LCFI1:
	/* Make room for all of the new args */
	movl  16(%ebp),%ecx
	subl  %ecx,%esp
	/* Align down to 16 byte boundary */
	andl  $-16, %esp

	movl  %esp,%eax

	/* align ffi_prep_args too */
	subl  $8, %esp
	
	/* Place all of the ffi_prep_args in position  */
	pushl 12(%ebp)
	pushl %eax
	call  *8(%ebp)

	/* Return stack to previous state and call the function  */
	addl  $16,%esp	

	call  *28(%ebp)

	/* Load %ecx with the return type code  */
	movl  20(%ebp),%ecx	

	/* If the return value pointer is NULL, assume no return value.  */
	cmpl  $0,24(%ebp)
	jne   retint

	/* Even if there is no space for the return value, we are 
	   obliged to handle floating-point values.  */
	cmpl  $FFI_TYPE_FLOAT,%ecx
	jne   noretval
	fstp  %st(0)

        jmp   epilogue

retint:
	cmpl  $FFI_TYPE_INT,%ecx
	jne   retfloat
	/* Load %ecx with the pointer to storage for the return value  */
	movl  24(%ebp),%ecx	
	movl  %eax,0(%ecx)
	jmp   epilogue

retfloat:
	cmpl  $FFI_TYPE_FLOAT,%ecx
	jne   retdouble
	/* Load %ecx with the pointer to storage for the return value  */
	movl  24(%ebp),%ecx	
	fstps (%ecx)
	jmp   epilogue

retdouble:
	cmpl  $FFI_TYPE_DOUBLE,%ecx
	jne   retlongdouble
	/* Load %ecx with the pointer to storage for the return value  */
	movl  24(%ebp),%ecx	
	fstpl (%ecx)
	jmp   epilogue

retlongdouble:
	cmpl  $FFI_TYPE_LONGDOUBLE,%ecx
	jne   retint64
	/* Load %ecx with the pointer to storage for the return value  */
	movl  24(%ebp),%ecx	
	fstpt (%ecx)
	jmp   epilogue
	
retint64:	
	cmpl  $FFI_TYPE_SINT64,%ecx
        jne   retstruct
	/* Load %ecx with the pointer to storage for the return value  */
	movl  24(%ebp),%ecx	
	movl  %eax,0(%ecx)
	movl  %edx,4(%ecx)
	
retstruct:
	/* Nothing to do!  */

noretval:
epilogue:
	/* The next two instructions are equivalent to leave */
        movl %ebp,%esp
        popl %ebp
        ret
LFE1:

	/* The following was copied from a test file compile with
	 * gcc -O2 -fnon-call-exceptions -S
	 * I do not have the slightest idea what it all means.
	 */
        .section __TEXT,__eh_frame,coalesced,no_toc+strip_static_syms+live_support
EH_frame1:
        .set L$set$0,LECIE1-LSCIE1
        .long L$set$0
LSCIE1:
        .long   0x0
        .byte   0x1
        .ascii "zR\0"
        .byte   0x1
        .byte   0x7c
        .byte   0x8
        .byte   0x1
        .byte   0x10
        .byte   0xc
        .byte   0x5
        .byte   0x4
        .byte   0x88
        .byte   0x1
        .align 2
LECIE1:
        .globl _ffi_call_DARWIN.eh
_ffi_call_DARWIN.eh:
LSFDE1:
        .set L$set$1,LEFDE1-LASFDE1
        .long L$set$1
LASFDE1:
        .long   LASFDE1-EH_frame1
        .long   LFB1-.
        .set L$set$2,LFE1-LFB1
        .long L$set$2
        .byte   0x0
        .byte   0x4
        .set L$set$3,LCFI0-LFB1
        .long L$set$3
        .byte   0xe
        .byte   0x8
        .byte   0x84
        .byte   0x2
        .byte   0x4
        .set L$set$4,LCFI1-LCFI0
        .long L$set$4
        .byte   0xd
        .byte   0x4
        .align 2
LEFDE1:

#endif /* ifndef __x86_64__ */
