#ifndef _MD_H
#define _MD_H

#include "config.h"
#include <setjmp.h>
#include <signal.h>
#ifndef RTEMS_BUILD
#include <ucontext.h>
#endif
#include <stdio.h>
#include <stdlib.h>

// used ONLY when testing precise.c with thunkTester.c
#ifdef THUNKTESTER_TESTING
# include "cjtypes.h"
#else
// normal alternative
# include "jtypes.h"
#endif

#ifdef __cplusplus
extern "C" {
#if 0
} /* fool emacs */
#endif
#endif

/*
 * This file defines functions that can be used to walk thread stacks,
 * functions that provide compare-and-swap primitives, and macros that
 * operate on jmpbuf and ucontext_t structures. 
 *
 * stackGrowsUp is defined here since it's definition is shared by all
 * architectures we are likely to encounter.
 */

// This can be defined in terms of __builtin_frame_address(0) and
// getCaller, but it can't be #define'd to __builtin_frame_address,
// since skipCount will not be a compile-time constant.  If we need a
// an md.c file for any other reason, getCurrentActivation should move
// there

static void *getCurrentActivation(int skipCount) __attribute__((noinline));
static inline void *getCaller(void *frame);
static inline void **getCallerPCLoc(void *frame);

static inline void *getCallerPC(void *frame) {
  return *getCallerPCLoc(frame);
}
static inline void setCallerPC(void *frame, void *absPC) {
  getCallerPCLoc(frame)[0] = absPC;
}


/*
 * Conservative-GC specific: this method must be called on an
 * frame that does SAVE_REGISTERS(), and it returns a pointer to
 * the frame's callee-save area.
 */
static inline void *callerLocalsEnd(void *frameHandle);
static inline jboolean stackGrowsUp() { return 0; }

static inline jboolean CAS32(volatile void *, jint, jint);
static inline jboolean CAS64(volatile void *, jlong, jlong);

/*
 * UC_REG(ucontext_t, reg-name) retrieves a register from a ucontext_t
 * The following names are defined for all architectures:
 *    UC_FP
 *    UC_PC
 *
 * UC_SIZE returns the size of a ucontext_t structure, or at least the
 * size up to and including the last general-purpose register value.
 * It can be used to scan a ucontext_t during conservative stack
 * walking.
 *
 * JB_REG(jmpbuf, reg-name) retrieves a register from a jmp_buf
 * The following names are defined for all supported architectures:
 *    JB_FP
 *
 * On some architectures the layout of a sigjmp_buf is different:
 *    SJB_FP is the offset to FP within a sigjmp_buf
 *
 * JB_PC might be useful, but hasn't been needed so far.
 */

#ifdef OVM_X86

static void *getCurrentActivation(int skipCount) {
  void *ret = __builtin_frame_address(1);
  while (skipCount--)
    ret = getCaller(ret);
  return ret;
}

static inline void *getCaller(void *frameHandle) {
  return *(void **)frameHandle;
}

void **getCallerPCLoc(void *frameHandle) {
  return ((void **) frameHandle) + 1;
}

static inline void *callerLocalsEnd(void *frameHandle) {
  return ((char *)frameHandle) - 16;
}

# if !(defined(UNIPROCESSOR) && defined(SAFE_POINTS))
#  ifdef UNIPROCESSOR
#   define LOCK
#  elif defined(OSX_BUILD)
     /* http://lists.apple.com/archives/darwin-dev/2005/Jun/msg00099.html */
#    define LOCK "lock;"
#  else
#   define LOCK "LOCK"
#  endif
/*
 * Perform an atomic compare-and-swap of a 32-bit value
 * @param target the target address
 * @param oldval the value to compare against
 * @param newval the value to set if the target has oldval
 *
 * @return 1 if the comparison succeeded and the newvalue was set
 *         0 if the comparison failed
 */
static inline 
jboolean CAS32(volatile void *_target, jint oldval, jint newval) {
    volatile jint *target = (volatile jint *) _target;
    char ret;  // must be a single byte
    jint readval;
    
    __asm__ __volatile__ 
        ( LOCK" cmpxchgl %3, %1\n\t"  /* do the cas */
          "sete %b0\n\t"  /* set the return value */
          : /* outputs */
          "=q" (ret), "+m" (*target), "=a" (readval)
          : /* inputs */
          "r" (newval),  "a" (oldval)
          : /* clobbers */
          "memory", "cc");

    return (jboolean)ret;
}


/*
 * Perform an atomic compare-and-swap of a 64-bit value
 * @param retval the value at target when the comparison is made will be
 *               stored at this address
 * @param target the target address
 * @param cmp the value to compare against
 * @param xchg_1 the low 32-bits of the new value to set
 * @param xchg_2 the high 32-bits of the new value to set
 *
 * @return 1 if the comparison succeeded and the new value was set
 *         0 if the comparison failed
 */
/* 
This code came from Joe Seigh in a posting in comp.lang.asm.x86. 
The original code used 'and' to zero out the upper three bytes of ret
after sete wrote 0 or 1 to the first byte. However, Matt Young, reported
that the use of a byte-access op followed by a word-access op would cause
a stall unless the CPU knew the value was zero - hence the 'and' was changed
to a movzbl - move byte to word with zero extension.
Of course you could use a byte for ret instead of an int, but seemingly this
might restrict the compiler too much and the compiler will probably use it
as an int anyway ... note that we use a byte for CAS32 so it might suffer too.
Someone should profile this.
*/
static inline 
int cas64_(jlong *retval, 
           volatile jlong *target,
           jlong cmp,
           long xchg_1,
           long xchg_2) {
    int ret;

    __asm__ __volatile__
        (
            LOCK" cmpxchg8b %2\n\t"      /* do the CAS */
            "sete %b0\n\t"          /* set bit 0 of ret if it succeeded */
            "movzbl %b0, %0\n\t"    /* zero-extend result - avoiding stall */
            : /* output */
            "=q" (ret),
            "=A" (*retval)
            : /* input */
            "m"  (*target),  /* target */
            "A"  (cmp),      /* comparand */
            "b"  (xchg_1),   /* exchange-lo */
            "c"  (xchg_2)    /* exchange-hi */
            : /* clobber list */  
            "memory", "cc"
            );

    return ret;
}

/*
 * Perform an atomic compare-and-swap of a 64-bit value
 * @param target the target address
 * @param oldval the value to compare against
 * @param newval the value to set if the target has oldval
 *
 * @return 1 if the comparison succeeded and the new value was set
 *         0 if the comparison failed
 */
inline static jboolean CAS64(volatile void* _target,
			     jlong oldval, jlong newval) {
    volatile jlong *target = (jlong *) _target;
    jlong retval; /* we don't use this */
    jlong tmp = newval;
    return cas64_(&retval, target, oldval, (&tmp)[0], (&tmp)[1]);
}
# endif

# if defined(LINUX_BUILD)
#   define UC_FP  REG_EBP
#   define UC_PC  REG_EIP

// It used to be
// # define JB_FP 4
// but I tested the asm code and I can assert
// that the correct offset is in fact 3 -- Toni
#   define JB_FP 3
#   define SJB_FP 3
#   define JB_REG(BUF, N) (BUF)->__jmpbuf[N]
#   define UC_REG(CTX, N) (CTX).uc_mcontext.gregs[N]
// It used to be defined as:
// # define UC_SIZE (8 * sizeof(size_t))
// but the expansion of ucontext_t shows that
// the general registers are well beyond 8 ptrs
// into the structure
#   define UC_SIZE sizeof(ucontext_t)
# elif defined(NETBSD_BUILD)
#   define UC_FP  _REG_EBP
#   define UC_PC  _REG_EIP

// the structure of jmp_buf in NetBSD is
// fiercely private, and the layout is not
// made explicit anywhere.
// An objdump of a sample program, linked statically, revealed:
/*
 80483d2:       89 61 08                mov    %esp,0x8(%ecx)
 80483d5:       89 69 0c                mov    %ebp,0xc(%ecx)
 80483d8:       89 71 10                mov    %esi,0x10(%ecx)
*/
// so the offset for %ebp is 12
#   define JB_SP  2
#   define SJB_SP 2
#   define JB_FP  3
#   define SJB_FP 3
#   define JB_REG(BUF, N) (BUF)[N]
#   define UC_REG(CTX, N) (CTX).uc_mcontext.__gregs[N]
#   define UC_SIZE sizeof(ucontext_t)
# elif defined(SOLARIS_BUILD)
#   define UC_FP  REG_FP
#   define UC_PC  REG_PC

// jmp_buf is also private in Solaris.
// an objdump -d /lib/libc.so reveals:
/*
   2473a:       89 78 08                mov    %edi,0x8(%eax)
   2473d:       89 68 0c                mov    %ebp,0xc(%eax)
   24740:       5a                      pop    %edx
*/
#   define JB_FP 3
#   define SJB_FP 15
#   define JB_REG(BUF, N) (BUF)[N]
#   define UC_REG(CTX, N) (CTX).uc_mcontext.gregs[N]
#   define UC_SIZE sizeof(ucontext_t)
# elif defined(OSX_BUILD)
/* This is kind of a weird one (%ecx is the jmp_buf):
0x90001244 <_setjmp+20>:        mov    %ebp,32(%ecx)
0x90001247 <_setjmp+23>:        mov    (%esp),%eax
0x9000124a <_setjmp+26>:        mov    %eax,48(%ecx)
0x9000124d <_setjmp+29>:        mov    %esp,%eax
0x9000124f <_setjmp+31>:        add    $0x4,%eax
0x90001252 <_setjmp+34>:        mov    %eax,36(%ecx)

  Then, at the end it pops %eax
 */
#   define JB_FP  8
#   define SJB_FP 8
#   define JB_SP  9
#   define SJB_SP 9
#   define JB_REG(BUF, N) (BUF)[N]

#   define UC_FP ebp
#   define UC_PC eip
#   define UC_REG(CTX, R) (CTX)->uc_mcontext->ss.R
#   define UC_SIZE sizeof(ucontext_t)
# elif defined(RTEMS_BUILD)
/*
00115d80 <setjmp>:
  115d80:       55                      push   %ebp
  115d81:       89 e5                   mov    %esp,%ebp
  115d83:       57                      push   %edi
  115d84:       8b 7d 08                mov    0x8(%ebp),%edi
  115d87:       89 07                   mov    %eax,(%edi)
  115d89:       89 5f 04                mov    %ebx,0x4(%edi)
  115d8c:       89 4f 08                mov    %ecx,0x8(%edi)
  115d8f:       89 57 0c                mov    %edx,0xc(%edi)
  115d92:       89 77 10                mov    %esi,0x10(%edi)
  115d95:       8b 45 fc                mov    -0x4(%ebp),%eax
  115d98:       89 47 14                mov    %eax,0x14(%edi)
  115d9b:       8b 45 00                mov    0x0(%ebp),%eax
  115d9e:       89 47 18                mov    %eax,0x18(%edi)
  115da1:       89 e0                   mov    %esp,%eax
  115da3:       83 c0 0c                add    $0xc,%eax
  115da6:       89 47 1c                mov    %eax,0x1c(%edi)
  115da9:       8b 45 04                mov    0x4(%ebp),%eax
  115dac:       89 47 20                mov    %eax,0x20(%edi)
  115daf:       5f                      pop    %edi
  115db0:       b8 00 00 00 00          mov    $0x0,%eax
  115db5:       c9                      leave  
  115db6:       c3                      ret    
*/

// #  define JB_SP 0 ??
#  define JB_SP 7
#  define JB_FP 6
#  define SJB_FP 6
//#  define JB_PC 3
#  define JB_REG(BUF, N) (((int*)(BUF))[N])

# else
#   warning "unknown ucontext_t and jmpbuf layout"
# endif
#elif defined(OVM_PPC)

/**
 * The following would work at least as well, but there is currently
 * no place to define it globally.  (I'm not sure about all the
 * optional suffixes on bdnz, or the order of operands for mr
 *
 * getCurrentActivation:
 *      mtctr r3
 *      mr    r3, r1
 * 0:   lwz   r3, 0(r3)
 *      bdnz  0b
 *      blr
 **/
static void getCurrentActivation_1() __attribute__((noinline));
static void getCurrentActivation_1() { }

// Because neither getCurrentActivation nor getCurrentActivation_1 are
// inlineable, getCurrentActivation is never compiled with
// leaf-procedure optimizations, and __builtin_frame_address(1) will
// reliably return the caller's AP.  It would be nice if
// __builtin_frame_address where aware of leaf-procedure
// optimizations, but it does not seem to be.
static void *getCurrentActivation(int skipCount) {
  void *ret = __builtin_frame_address(2);
  // asm("lwz " R(1) " %0" : "=r"(ret));
  getCurrentActivation_1();
  while (skipCount--)
    ret = getCaller(ret);
  return ret;
}

static inline void *getCaller(void *frameHandle) {
  return *(void **)frameHandle;
}

/*
 * AIX and MacOS refer to the stack pointer as `r1', while Linux and
 * SysV refer to it as `%r1'.  OSX_BUILD is the wrong macro to test,
 * but it gets the job done.
 */
static inline void **getCallerPCLoc(void *frameHandle) {
#if 0
# ifdef OSX_BUILD
  return ((void **) getCaller(frameHandle)) + 2;
# else
  return ((void **) getCaller(frameHandle)) + 1;
# endif
#else
# ifdef OSX_BUILD
  return ((void **) frameHandle) + 2;
# else
  return ((void **) frameHandle) + 1;
# endif
#endif

}

static inline void *callerLocalsEnd(void *frameHandle) {
  return ((char *)frameHandle) - 76;
}

# if !(defined(UNIPROCESSOR) && defined(SAFE_POINTS))
#  include <stdio.h>
#  include <stdlib.h>
/*
 * Perform a "weak" atomic compare-and-swap of a 32-bit value.
 * This is a "weak" version because the store-conditional could fail
 * if another address in the reservation granule was written to.
 * @param target the target address
 * @param oldval the value to compare against
 * @param newval the value to set if the target has oldval
 *
 * @return 1 if the comparison succeeded and the newvalue was set
 *         0 if the comparison failed or the reservation was lost
 */
static inline
jboolean CAS32_WEAK(volatile jint *target,jint oldval, jint newval) {
    // %0 = result
    // %2 = expected old value 
    // %3 = target address 
    // %4 = new value
    // %5 = tmp
    jboolean result;
    int tmp;
    __asm__ __volatile__ (
        "lwarx  %5,0,%3\n"  // load *target with reservation
        "cmpw   %2,%5\n"    // compare with oldval
        "bne-   0f\n"       // goto result=0 if != oldval
        "stwcx. %4,0,%3\n"  // conditionally store newval
        "bne-   0f\n"       // goto result=0 if store failed
        "li     %0,1\n"     // result = 1
        "b      1f\n"       // goto exit
        "0:\n"
        "li     %0,0\n"     // result = 0
        "1:\n"
        /* outputs */
        : "=r"(result), "=r"(target)
        /* inputs */
        : "r"(oldval), "1"(target), "r"(newval), "r"(tmp)
        /* clobbers */
        : "cr0", "memory");
    return result;
}

/*
 * Perform an atomic compare-and-swap of a 32-bit value.
 * @param target the target address
 * @param oldval the value to compare against
 * @param newval the value to set if the target has oldval
 *
 * @return 1 if the comparison succeeded and the newvalue was set
 *         0 if the comparison failed.
 */
static inline
jboolean CAS32(volatile void *_target,jint oldval, jint newval) {
    // %0 = result
    // %2 = expected old value 
    // %3 = target address 
    // %4 = new value
    // %5 = tmp
    volatile jint *target = (volatile jint *) _target;
    jboolean result;
    int tmp = 0; // gcc warns about uninitialized use - though that should not be so
    __asm__ __volatile__ (
        "0:\n"
        "lwarx  %5,0,%3\n"  // load *target with reservation
        "cmpw   %2,%5\n"    // compare with oldval
        "bne-   1f\n"       // goto result=0 if != oldval
        "stwcx. %4,0,%3\n"  // conditionally store newval
        "bne-   0b\n"       // loop if lost reservation
        "li     %0,1\n"     // result = 1
        "b      2f\n"       // goto exit
        "1:\n"
        "li     %0,0\n"     // result = 0
        "2:\n"              // exit
        /* outputs */
        : "=r"(result), "=r"(target)
        /* inputs */
        : "r"(oldval), "1"(target), "r"(newval), "r"(tmp)
        /* clobbers */
        : "cr0", "memory");

    return result;
}


static inline
jboolean CAS64_WEAK(volatile jlong* target, jlong oldval, jlong newval) {
/* there is no extended CAS support on PPC. So CAS64 is only valid
   when on a 64-bit architecture
*/
#if SIZEOF_INT == 8
    // %0 = result
    // %2 = expected old value
    // %3 = target address
    // %4 = new value
    // %5 = temp
    jboolean result;
    int tmp;
    __asm__ __volatile__ (
        "ldarx  %5,0,%3\n"   // load dword *target with reservation
        "cmpd   %2,%5\n"     // compare with old value
        "bne-   0f\n"        // goto result=0 if != oldval
        "stdcx. %4,0,%3\n"   // conditionally store newval
        "bne-   0f\n"        // goto result=0 if lost reservation
        "li     %0,1\n"      // result = 1
        "b      1f\n"        // goto exit
        "0:\n"
        "li     %0,0\n"      // result = 0
        "1:\n"               // exit
        /* outputs */
        : "=r"(result), "=r"(target)
        /* inputs */
        : "r"(oldval),  "1"(target), "r"(newval), "r"(tmp)
        /* clobbers */
        : "cr0", "memory");

    return result;
#else
    fprintf(stderr, "ERROR: attempt to invoke CAS64_WEAK from a 32-bit PPC machine\n");
    exit(-1);
#endif
}

static inline
jboolean CAS64(volatile void* _target, jlong oldval, jlong newval) {
/* there is no extended CAS support on PPC. So CAS64 is only valid
   when on a 64-bit architecture
*/
    volatile jlong *target = (volatile jlong *) _target;  
#if SIZEOF_INT == 8
    // %0 = result
    // %2 = expected old value
    // %3 = target address
    // %4 = new value
    // %5 = temp
    jboolean result;
    int tmp;
    __asm__ __volatile__ (
        "0:\n"
        "ldarx  %5,0,%3\n"   // load dword *target with reservation
        "cmpd   %2,%5\n"     // compare with old value
        "bne-   1f\n"        // goto result=0 if != oldval
        "stdcx. %4,0,%3\n"   // conditionally store newval
        "bne-   0b\n"        // loop if lost reservation
        "li     %0,1\n"      // result = 1
        "b      2f\n"        // goto exit
        "1:\n"
        "li     %0,0\n"      // result = 0
        "2:\n"               // exit
        /* outputs */
        : "=r"(result), "=r"(target)
        /* inputs */
        : "r"(oldval),  "1"(target), "r"(newval), "r"(tmp)
        /* clobbers */
        : "cr0", "memory");

    return result;
#else
    fprintf(stderr, "ERROR: attempt to invoke CAS64 from a 32-bit PPC machine\n");
    exit(-1);
#endif
}
# endif // !(UNIPROCESSOR && SAFEPOINTS)

# if defined(LINUX_BUILD)
#  define UC_R1  1
#  define UC_R14 14
#  define UC_PC  32
#  define UC_FP  UC_R1
#  define JB_FP 0
#  define SJB_FP 0
#  define JB_REG(BUF, N) (BUF)->__jmpbuf[N]
#  define UC_REG(CTX, N) (CTX).uc_mcontext.uc_regs->gregs[N]
#  define UC_SIZE (32 * sizeof(size_t))
# elif defined(OSX_BUILD)
#  define JB_FP 0
#  define SJB_FP 0
#  define JB_REG(BUF, N) (BUF)[N]
# endif


#elif defined(OVM_ARM)

#if !defined (ARM_THUMB)

/* __builtin_frame_address() on the ARM is totally
   and utterly broken. Using __builtin_frame_address(0)
   it returns a "soft" frame pointer that refers to
   the local area. When using __builtin_frame_address(N)
   with N>0 it returns total garbage.
   As we are interested in the "real" frame pointer,
   as a mean to follow a frame chain and to find the
   return addresses, some asm is used here instead.
   The routine below can only work if the frame pointer
   register is always used, though, while GCC on the
   ARM tries to get rid of it whenever possible.
   Code that uses getCurrentActivation() *must* be
   compiled with -fno-omit-frame-pointer. (The flags
   -fno-optimize-sibling-calls and -fno-inline may
   also be of use, depending on whether the code
   needs a wiew of the frames as they were in the
   source code, or just a view of the frames in the
   code that GCC produced).
   
   I have not written a suitable implementation
   for the Thumb as it is not obvious how the
   location of the previous frame can be obtained.
   Maybe there's a way, but I have not found an
   obvious approach so far. Possibly using
   `-mtpcs-frame' might force frames that are
   easier to decode.
*/

// the definition below really works, reading from the fp register.
#define BUILTIN_FRAME_ADDRESS_1() ({   register void** fp asm("fp"); void *ret=*(fp-3); ret; })

// frameHandle must be the "hard" fp register and
// not the "soft" fp as returned by __builtin_frame_address
// next fp is at [fp, #-12]

static inline void *getCaller(void *frameHandle) {
  return *(((void **)(frameHandle)) -3);
}

static inline void *callerLocalsEnd(void *frameHandle) {
  // should be ok when saving r4..r11 and f4..f7
  // -56 bytes
  //
  // when in softfloat mode f4-f7 are *not* saved.

#if __JMP_BUF_SP == 20
// fpa mode
  return ((char *)frameHandle) - 56;
#elif __JMP_BUF_SP == 8
// softfloat mode
  return ((char *)frameHandle) - 44;
#else
# error "Unknown ARM jmp_buf layout"
#endif
}

// frameHandle must be the "hard" fp register and
// not the "soft" fp as returned by __builtin_frame_address
// caller pc (prevoius lr) is stored at [fp, #-4]
void **getCallerPCLoc(void *frameHandle) {
  return ((void **)(frameHandle)) -1;
}

#else

#define BUILTIN_FRAME_ADDRESS_1() ({ void *ret=NULL;                          \
  fprintf(stderr,"BUILTIN_FRAME_ADDRESS_1() unsupported in ARM Thumb mode."); \
  exit(-1);                                                                   \
  ret; })

static inline void *getCaller(void *frameHandle) {
  fprintf(stderr,"getCaller() unsupported in ARM Thumb mode.");
  exit(-1);
}
static inline void *callerLocalsEnd(void *frameHandle) {
  fprintf(stderr,"callerLocalsEnd() unsupported in ARM Thumb mode.");
  exit(-1);
  // it *seems* that the soft frame pointer refers
  // exactly to the location we need. Not clear how to
  // get that from the real fp (stored in r7) for a
  // general frame, though.
}
void **getCallerPCLoc(void *frameHandle) {
  fprintf(stderr,"getCallerPCLoc() unsupported in ARM Thumb mode.");
  exit(-1);
}

#endif


// force getCurrentActivation to be non-leaf
static void getCurrentActivation_1() __attribute__((noinline));
static void getCurrentActivation_1() { }

// Because neither getCurrentActivation nor getCurrentActivation_1 are
// inlineable, getCurrentActivation is never compiled with
// leaf-procedure optimizations, and __builtin_frame_address(1) will
// reliably return the caller's AP.  It would be nice if
// __builtin_frame_address where aware of leaf-procedure
// optimizations, but it does not seem to be.
static void *getCurrentActivation(int skipCount) {
  void *ret = BUILTIN_FRAME_ADDRESS_1(); // or should it be (2) ??
  getCurrentActivation_1();
  while (skipCount--)
    ret = getCaller(ret);
  return ret;
}


# if !(defined(UNIPROCESSOR) && defined(SAFE_POINTS))

/*
 * Perform an atomic compare-and-swap of a 32-bit value
 * @param target the target address
 * @param oldval the value to compare against
 * @param newval the value to set if the target has oldval
 *
 * @return 1 if the comparison succeeded and the newvalue was set
 *         0 if the comparison failed
 */
// this implementation taken from glibc/sysdeps/arm/atomicity.h
// with the following important caveat:

// These sequences are not actually atomic!!! There can be a race
// condition in certain cases. However, they are the best thing
// that can be obtained on pre-ARMv6 implementations without
// using OS support, and it's the implementation currently
// present in glibc.

// It's the first time that I find an "atomic-but-not-quite"
// implementation of an atomic operation. However, ARMv6 and
// following DO have ldrex/strex, which can be used instead. Not sure
// if XScale has it, but it seems likely. I have not found
// an implementation of CAS that uses ldrex/strex, but it shouldn't
// be exceedingly difficult to put one together. However, as
// current Ovm code always uses a single processor and always
// uses safe points (hence the code below is not used anyway),
// the availability of a perfect CAS implementation is, at
// present, not particularly crucial.
static inline
jboolean CAS32(volatile void *_target, jint oldval, jint newval) {
  volatile jint *target = (volatile jint *) _target;
  jboolean result;
  jint tmp;
  __asm__ __volatile(
           "0:\tldr\t%1,[%2]\n\t"
           "mov\t%0,#0\n\t"
           "cmp\t%1,%4\n\t"
           "bne\t1f\n\t"
           "swp\t%0,%3,[%2]\n\t"
           "cmp\t%1,%0\n\t"
           "swpne\t%1,%0,[%2]\n\t"
           "bne\t0b\n\t"
           "mov\t%0,#1\n"
           "1:"
           : "=&r" (result), "=&r" (tmp)
           : "r" (target), "r" (newval), "r" (oldval)
           : "cc", "memory");
  return result;
}

// no CAS64 (one could possibly be implemented by using
// ldrex/strex)
static inline
jboolean CAS64(volatile void* _target, jlong oldval, jlong newval) {
    fprintf(stderr, "ERROR: attempt to invoke CAS64 from an ARM machine\n");
    exit(-1);
}
# endif // !(UNIPROCESSOR && SAFEPOINTS)                                                                                                       

# ifdef LINUX_BUILD

/* the size and layout of a jmp_buf on the arm
   *changes* depending on whether the compiler is
   working in softfloat or hardfloat mode.

   The saved stack pointer is, in any case, in:

   __jmp_buf[__JMP_BUF_SP].

   The other registers can be found relative to it:

   fp(r11) is __jmp_buf[__JMP_BUF_SP-1]
   pc      is __jmp_buf[__JMP_BUF_SP+1]

   in thumb mode the fp is in r7 rather than r11:

   fp(r7) is __jmp_buf[__JMP_BUF_SP-5]
*/

#  if defined(ARM_THUMB)
#   define JB_FP __JMP_BUF_SP-5
#  else
#   define JB_FP __JMP_BUF_SP-1
#  endif
#  define JB_REG(BUF, N) (BUF)->__jmpbuf[N]
# else
#   warning "unknown ucontext_t and jmpbuf layout"
# endif

#elif defined(OVM_SPARC)

// force getCurrentActivation to be non-leaf
static void getCurrentActivation_1() __attribute__((noinline));
static void getCurrentActivation_1() { }

// I do not return the actual fp, but rather sp,
// as the return address (in %o7) is stored
// relative to sp (sp points to reg window on
// the top of stack, which is the backing store
// for %l and %i regs. %i7 is the %o7 of the
// caller, that is the return address.
// so 4(sp) is the return address.

static void *getCurrentActivation(int skipCount) {
  __asm__ __volatile__ ("ta 3");
  void *ret = __builtin_frame_address(1);
  getCurrentActivation_1();
  while (skipCount--)
    ret = getCaller(ret);
  return ret;
}

/*

  The SPARC frame (?):

 ...
 Caller's Frame <- %fp (old %sp)
 ----
 Local Variables
 alloca() space
 Memory Temps &
 Saved FP Registers
 Parms past 6
 Parms 1 to 6
 (memory home)
 Address of return value (minus 8)
 Register Window Overflow (16 words) <-%sp


 The space for the registers is only written to
 when the register windows are flushed (instruction "ta 3").

*/
// BEWARE: the SPARC saves the return address-8 


static inline void *getCaller(void *frameHandle) {
  return *(((void **) frameHandle) +14);
}

void **getCallerPCLoc(void *frameHandle) {
  return ((void **) frameHandle) +15;
}

static inline void *callerLocalsEnd(void *frameHandle) {
  // All call-preserved regs are automatically
  // saved in register windows on the sparc
  return ((char *)frameHandle); 
}

# if !(defined(UNIPROCESSOR) && defined(SAFE_POINTS))

// SPARC CAS32 as used in uClib, glibc, kaffe
static inline
jboolean CAS32(volatile void *p, jint oldval, jint newval)
{
  static unsigned char lock;
  jboolean ret; jint tmp;

  __asm__ __volatile__("1:	ldstub	[%1], %0\n\t"
		       "	cmp	%0, 0\n\t"
		       "	bne	1b\n\t"
		       "	 nop"
		       : "=&r" (tmp)
		       : "r" (&lock)
		       : "memory");
  if (*(volatile jint*)p != oldval)
    ret = 0;
  else
    {
      *(volatile jint*)p = newval;
      ret = 1;
    }
  __asm__ __volatile__("stb	%%g0, [%0]"
		       : /* no outputs */
		       : "r" (&lock)
		       : "memory");

  return ret;
}

// no CAS64 (no SPARC v9 support yet)
static inline
jboolean CAS64(volatile void* _target, jlong oldval, jlong newval) {
    fprintf(stderr, "ERROR: attempt to invoke CAS64 from a SPARC machine\n");
    exit(-1);
}
# endif // !(UNIPROCESSOR && SAFEPOINTS)

# ifdef LINUX_BUILD
#  define JB_SP  0
#  define JB_FP  1
#  define SJB_FP  1
#  define JB_PC  2
#  define JB_REG(BUF, N) (BUF)->__jmpbuf[N]

# elif defined(SOLARIS_BUILD)
// OpenSolaris tells me everything! :)
// http://cvs.opensolaris.org/source/xref/on/usr/src/lib/libc/inc/sigjmp_struct.h#sigjmp_struct_t

#  define JB_SP  1
#  define JB_FP  3
#  define SJB_FP  3
#  define JB_PC  2
#  define JB_REG(BUF, N) (BUF)[N]

# elif defined(RTEMS_BUILD)

#  define JB_SP 0
#  define JB_FP 2
#  define SJB_FP 2
#  define JB_PC 3
#  define JB_REG(BUF, N) (((int*)(BUF))[N])

# else
#   warning "unknown ucontext_t and jmpbuf layout"
# endif

#else // !OVM_PPC, !OVM_ARM, !OVM_X86, !OVM_SPARC
# error "no machine dependent functions defined on this architecture"
#endif


#if defined(SAFE_POINTS) && defined(UNIPROCESSOR)
/*
 * Note that SAFE_POINTS and UNIPROCESSOR are only defined when
 * compiling the ovm itself.  Code in libnative.a will be compiled
 * with SMP-safe CAS primitives.
 */

/*
 * On the x86: gcc does a poor job of compiling either of these
 * functions for a branch.  It seems to do slightly better with the
 * first version: it uses the same number of instructions, but one
 * fewer registers.
 */
# if 1
static inline jboolean CAS32(volatile void *_target,
			     jint oldval, jint newval) {
  jint *target = (jint *) _target; /* C++ compatiblity */
  jboolean same = *target == oldval;
  if (same)
    *target = newval;
  return same;
}
# else
static inline jboolean CAS32(volatile void *_target,
			     jint oldval, jint newval) {
  jint *target = (jint *) _target; /* C++ compatibility */
  if (*target == oldval) {
    *target = newval;
    return 1;
  } else
    return 0;
}
# endif

static inline jboolean CAS64(volatile void *_target,
			     jlong oldval, jlong newval) {
  jlong *target = (jlong *) _target; /* C++ compatibility */
  jboolean same = *target == oldval;
  if (same)
    *target = newval;
  return same;
}
#endif

#ifdef __cplusplus
}
#endif
#endif // _MD_H
