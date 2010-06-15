#ifndef __APPLE_CC__
/* Original behavior of gcc-3.3 libffi */
# define EMIT_EH_SECTION
# define EH_SECTION __TEXT,__eh_frame
# define EH_SYMBOL
#elif __GNUC_MAJOR__ >= 3 && __GNUC_MINOR__ >= 3
/* I don't think apple's new exception handling mechanism needs unwind
   info. */
# undef EMIT_EH_SECTION
#else
/* Match behavior of apple's gcc3 with -fexceptions */
# define EMIT_EH_SECTION
# define EH_SECTION .section __TEXT,__eh_frame,coalesced,no_toc+strip_static_syms
# define EH_SYMBOL _EH_unwind_info:
#endif
