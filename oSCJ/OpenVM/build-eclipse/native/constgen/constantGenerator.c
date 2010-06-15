/*
 * $Header: /p/sss/cvs/OpenVM/src/native/constgen/constantGenerator.c,v 1.28 2006/09/24 06:31:53 cunei Exp $
 */

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/select.h>
#include "autodefs.h"

#if defined(RTEMS_BUILD)
#include <bsp.h>
#include <stdlib.h>
#endif

#if defined(HAVE_SYS_MMAN_H)
  #include <sys/mman.h>
#endif

#if defined(RTEMS_BUILD)
  #include <sys/param.h>
#endif  


#define CONST_GEN
#include "blockingio.h"

#if !defined(CG_DEFAULT_PKG)
  #define CG_DEFAULT_PKG "ovm.core.execution"
#endif

#if !defined(CG_DEFAULT_CLS)
  #define CG_DEFAULT_CLS "NativeConstants"
#endif    

#if !defined(CG_DEFAULT_NOCT)
  #define CG_DEFAULT_NOCT 0
#endif    

/**
 * Generates a Java class that defines constants which match various
 * C level constants (#define values) on the current platform.
 * @author Jan Vitek
 * @author David Holmes
 **/
 
/*
With RTEMS, the command line arguments have to be supported at compile time,
i.e. -DCG_DEFAULT_PKG=XXX -DCG_DEFAULT_CLS=XXX -DCG_DEFAULT_NOCT=XXX

And the output is written to stdout. If run in simulator, something the output may 
have to be filtered to strip off the simulator's messages
*/

struct namepair {
    const char* name;
    int value;
};

#define C(name) { #name, name }
struct namepair constants[] = {
    /* file I/O flags */
    C(O_RDONLY),
    C(O_WRONLY),
    C(O_RDWR),
    C(O_APPEND),
    C(O_CREAT),
    C(O_TRUNC),
    C(O_EXCL),
    C(O_NONBLOCK),
#ifdef O_SYNC
    C(O_SYNC),
#else
    {"O_SYNC",0},
#endif
    C(EOF),
    C(SEEK_SET),
    C(SEEK_CUR),
    C(SEEK_END),
    C(AF_INET),
    C(AF_UNIX),
    C(PF_INET),
    C(MSG_DONTWAIT),
#ifdef MSG_NOSIGNAL
    C(MSG_NOSIGNAL),
#else
    {"MSG_NOSIGNAL",0},
#endif
    C(SOCK_STREAM),
    C(SOCK_DGRAM),
    C(R_OK),
    C(W_OK),
    C(X_OK),
    C(F_OK),
    C(S_IRWXU),
    C(S_IRUSR),
    C(S_IREAD),
    C(S_IWUSR),
    C(S_IWRITE),
    C(S_IXUSR),
    C(S_IEXEC),    
    C(S_IRWXG),
    C(S_IRGRP),
    C(S_IWGRP),
    C(S_IXGRP),
    C(S_IRWXO),
    C(S_IROTH),
    C(S_IWOTH),
    C(S_IXOTH),
    C(_IONBF),
    C(_IOLBF),
    C(_IOFBF),

    C(S_IFMT),
    C(S_IFIFO),
    C(S_IFCHR),
    C(S_IFDIR),
    C(S_IFBLK),
    C(S_IFREG),
    C(S_IFLNK),
    C(S_IFSOCK),


    /* special fake values for host VM use */
    { "STDOUT", 0 }, 
    { "STDERR", 1},
    { "STDIN", 2 },

    /* Possibly values of BYTE_ORDER, which is computed in main() */
    { "LITTLE_ENDIAN", 0x86 },
    { "BIG_ENDIAN", 1 },
    { "LITTLE_ENDIAN_DOUBLES_SWAPPED", 'A'*65536L+'R'*256L+'M' }, /* see below */

    /* all of the possible error codes  as defined by POSIX 
       Because some systems may not be fully compliant we check
       the existence of each value and warn if it doesn't exist
     */
#ifdef E2BIG
C(E2BIG),
#else
#warning "E2BIG not defined in this system"
#endif
#ifdef EACCES
C(EACCES),
#else
#warning "EACCES not defined in this system"
#endif
#ifdef EADDRINUSE
C(EADDRINUSE),
#else
#warning "EADDRINUSE not defined in this system"
#endif
#ifdef EADDRNOTAVAIL
C(EADDRNOTAVAIL),
#else
#warning "EADDRNOTAVAIL not defined in this system"
#endif
#ifdef EAFNOSUPPORT
C(EAFNOSUPPORT),
#else
#warning "EAFNOSUPPORT not defined in this system"
#endif
#ifdef EAGAIN
C(EAGAIN),
#else
#warning "EAGAIN not defined in this system"
#endif
#ifdef EALREADY
C(EALREADY),
#else
#warning "EALREADY not defined in this system"
#endif
#ifdef EBADF
C(EBADF),
#else
#warning "EBADF not defined in this system"
#endif
#ifdef EBADMSG
C(EBADMSG),
#else
#warning "EBADMSG not defined in this system"
#endif
#ifdef EBUSY
C(EBUSY),
#else
#warning "EBUSY not defined in this system"
#endif
#ifdef ECANCELED
C(ECANCELED),
#else
#warning "ECANCELED not defined in this system"
#endif
#ifdef ECHILD
C(ECHILD),
#else
#warning "ECHILD not defined in this system"
#endif
#ifdef ECONNABORTED
C(ECONNABORTED),
#else
#warning "ECONNABORTED not defined in this system"
#endif
#ifdef ECONNREFUSED
C(ECONNREFUSED),
#else
#warning "ECONNREFUSED not defined in this system"
#endif
#ifdef ECONNRESET
C(ECONNRESET),
#else
#warning "ECONNRESET not defined in this system"
#endif
#ifdef EDEADLK
C(EDEADLK),
#else
#warning "EDEADLK not defined in this system"
#endif
#ifdef EDESTADDRREQ
C(EDESTADDRREQ),
#else
#warning "EDESTADDRREQ not defined in this system"
#endif
#ifdef EDOM
C(EDOM),
#else
#warning "EDOM not defined in this system"
#endif
#ifdef EDQUOT
C(EDQUOT),
#else
#warning "EDQUOT not defined in this system"
#endif
#ifdef EEXIST
C(EEXIST),
#else
#warning "EEXIST not defined in this system"
#endif
#ifdef EFAULT
C(EFAULT),
#else
#warning "EFAULT not defined in this system"
#endif
#ifdef EFBIG
C(EFBIG),
#else
#warning "EFBIG not defined in this system"
#endif
#ifdef EHOSTUNREACH
C(EHOSTUNREACH),
#else
#warning "EHOSTUNREACH not defined in this system"
#endif
#ifdef EIDRM
C(EIDRM),
#else
#warning "EIDRM not defined in this system"
#endif
#ifdef EILSEQ
C(EILSEQ),
#else
#warning "EILSEQ not defined in this system"
#endif
#ifdef EINPROGRESS
C(EINPROGRESS),
#else
#warning "EINPROGRESS not defined in this system"
#endif
#ifdef EINTR
C(EINTR),
#else
#warning "EINTR not defined in this system"
#endif
#ifdef EINVAL
C(EINVAL),
#else
#warning "EINVAL not defined in this system"
#endif
#ifdef EIO
C(EIO),
#else
#warning "EIO not defined in this system"
#endif
#ifdef EISCONN
C(EISCONN),
#else
#warning "EISCONN not defined in this system"
#endif
#ifdef EISDIR
C(EISDIR),
#else
#warning "EISDIR not defined in this system"
#endif
#ifdef ELOOP
C(ELOOP),
#else
#warning "ELOOP not defined in this system"
#endif
#ifdef EMFILE
C(EMFILE),
#else
#warning "EMFILE not defined in this system"
#endif
#ifdef EMLINK
C(EMLINK),
#else
#warning "EMLINK not defined in this system"
#endif
#ifdef EMSGSIZE
C(EMSGSIZE),
#else
#warning "EMSGSIZE not defined in this system"
#endif
#ifdef EMULTIHOP
C(EMULTIHOP),
#else
#warning "EMULTIHOP not defined in this system"
#endif
#ifdef ENAMETOOLONG
C(ENAMETOOLONG),
#else
#warning "ENAMETOOLONG not defined in this system"
#endif
#ifdef ENETDOWN
C(ENETDOWN),
#else
#warning "ENETDOWN not defined in this system"
#endif
#ifdef ENETRESET
C(ENETRESET),
#else
#warning "ENETRESET not defined in this system"
#endif
#ifdef ENETUNREACH
C(ENETUNREACH),
#else
#warning "ENETUNREACH not defined in this system"
#endif
#ifdef ENFILE
C(ENFILE),
#else
#warning "ENFILE not defined in this system"
#endif
#ifdef ENOBUFS
C(ENOBUFS),
#else
#warning "ENOBUFS not defined in this system"
#endif
#ifdef ENODATA
C(ENODATA),
#else
#warning "ENODATA not defined in this system"
#endif
#ifdef ENODEV
C(ENODEV),
#else
#warning "ENODEV not defined in this system"
#endif
#ifdef ENOENT
C(ENOENT),
#else
#warning "ENOENT not defined in this system"
#endif
#ifdef ENOEXEC
C(ENOEXEC),
#else
#warning "ENOEXEC not defined in this system"
#endif
#ifdef ENOLCK
C(ENOLCK),
#else
#warning "ENOLCK not defined in this system"
#endif
#ifdef ENOLINK
C(ENOLINK),
#else
#warning "ENOLINK not defined in this system"
#endif
#ifdef ENOMEM
C(ENOMEM),
#else
#warning "ENOMEM not defined in this system"
#endif
#ifdef ENOMSG
C(ENOMSG),
#else
#warning "ENOMSG not defined in this system"
#endif
#ifdef ENOPROTOOPT
C(ENOPROTOOPT),
#else
#warning "ENOPROTOOPT not defined in this system"
#endif
#ifdef ENOSPC
C(ENOSPC),
#else
#warning "ENOSPC not defined in this system"
#endif
#ifdef ENOSR
C(ENOSR),
#else
#warning "ENOSR not defined in this system"
#endif
#ifdef ENOSTR
C(ENOSTR),
#else
#warning "ENOSTR not defined in this system"
#endif
#ifdef ENOSYS
C(ENOSYS),
#else
#warning "ENOSYS not defined in this system"
#endif
#ifdef ENOTCONN
C(ENOTCONN),
#else
#warning "ENOTCONN not defined in this system"
#endif
#ifdef ENOTDIR
C(ENOTDIR),
#else
#warning "ENOTDIR not defined in this system"
#endif
#ifdef ENOTEMPTY
C(ENOTEMPTY),
#else
#warning "ENOTEMPTY not defined in this system"
#endif
#ifdef ENOTSOCK
C(ENOTSOCK),
#else
#warning "ENOTSOCK not defined in this system"
#endif
#ifdef ENOTSUP
C(ENOTSUP),
#else
#warning "ENOTSUP not defined in this system"
#endif
#ifdef ENOTTY
C(ENOTTY),
#else
#warning "ENOTTY not defined in this system"
#endif
#ifdef ENXIO
C(ENXIO),
#else
#warning "ENXIO not defined in this system"
#endif
#ifdef EOPNOTSUPP
C(EOPNOTSUPP),
#else
#warning "EOPNOTSUPP not defined in this system"
#endif
#ifdef EOVERFLOW
C(EOVERFLOW),
#else
#warning "EOVERFLOW not defined in this system"
#endif
#ifdef EPERM
C(EPERM),
#else
#warning "EPERM not defined in this system"
#endif
#ifdef EPIPE
C(EPIPE),
#else
#warning "EPIPE not defined in this system"
#endif
#ifdef EPROTO
C(EPROTO),
#else
#warning "EPROTO not defined in this system"
#endif
#ifdef EPROTONOSUPPORT
C(EPROTONOSUPPORT),
#else
#warning "EPROTONOSUPPORT not defined in this system"
#endif
#ifdef EPROTOTYPE
C(EPROTOTYPE),
#else
#warning "EPROTOTYPE not defined in this system"
#endif
#ifdef ERANGE
C(ERANGE),
#else
#warning "ERANGE not defined in this system"
#endif
#ifdef EROFS
C(EROFS),
#else
#warning "EROFS not defined in this system"
#endif
#ifdef ESPIPE
C(ESPIPE),
#else
#warning "ESPIPE not defined in this system"
#endif
#ifdef ESRCH
C(ESRCH),
#else
#warning "ESRCH not defined in this system"
#endif
#ifdef ESTALE
C(ESTALE),
#else
#warning "ESTALE not defined in this system"
#endif
#ifdef ETIME
C(ETIME),
#else
#warning "ETIME not defined in this system"
#endif
#ifdef ETIMEDOUT
C(ETIMEDOUT),
#else
#warning "ETIMEDOUT not defined in this system"
#endif
#ifdef ETXTBSY
C(ETXTBSY),
#else
#warning "ETXTBSY not defined in this system"
#endif
#ifdef EWOULDBLOCK
C(EWOULDBLOCK),
#else
#warning "EWOULDBLOCK not defined in this system"
#endif
#ifdef EXDEV
C(EXDEV),
#else
#warning "EXDEV not defined in this system"
#endif

C(BLOCKINGIO_READ),
C(BLOCKINGIO_WRITE),
C(BLOCKINGIO_EXCEPT),
C(FD_SETSIZE),
#if defined(HAVE_SYS_MMAN_H)
C(PROT_READ),
C(PROT_WRITE),
C(PROT_EXEC),
C(PROT_NONE),
C(MAP_ANON),
C(MAP_FIXED),
C(MAP_SHARED),
C(MAP_PRIVATE),
#else
 {"PROT_READ",0},
 {"PROT_WRITE",0}, 
 {"PROT_EXEC",0}, 
 {"PROT_NONE",0}, 
 {"MAP_ANON",0},
 {"MAP_FIXED",0}, 
 {"MAP_SHARED",0}, 
 {"MAP_PRIVATE",0},
#endif
#ifdef FORCE_IMAGE_LOCATION
  C(FORCE_IMAGE_LOCATION),
#else
  { "FORCE_IMAGE_LOCATION", 0 },
#endif
};

struct namepair boolean_constants[] = {
#ifdef ASM_NEEDS_UNDERSCORE
  C(ASM_NEEDS_UNDERSCORE),
#else
  { "ASM_NEEDS_UNDERSCORE", 0 },
#endif
  C(SHIFTS_NEED_MASK),
#ifdef OVM_X86
  C(OVM_X86),
#else
  { "OVM_X86", 0 },
#endif
#ifdef OVM_PPC
  C(OVM_PPC),
#else
  { "OVM_PPC", 0 },
#endif
#ifdef OVM_ARM
  C(OVM_ARM),
#else
  { "OVM_ARM", 0 },
#endif
#ifdef OVM_SPARC
  C(OVM_SPARC),
#else
  { "OVM_SPARC", 0 },
#endif
#ifdef WORDS_BIGENDIAN
  C(WORDS_BIGENDIAN),
#else
  { "WORDS_BIGENDIAN", 0 },
#endif
#ifdef OSX_BUILD
  C(OSX_BUILD),
#else
  { "OSX_BUILD", 0 },
#endif  
#ifdef SOLARIS_BUILD
  C(SOLARIS_BUILD),
#else
  { "SOLARIS_BUILD", 0 },
#endif  
#ifdef NETBSD_BUILD
  C(NETBSD_BUILD),
#else
  { "NETBSD_BUILD", 0 },
#endif  
#ifdef LINUX_BUILD
  C(LINUX_BUILD),
#else
  { "LINUX_BUILD", 0 },
#endif  
#ifdef RTEMS_BUILD
  C(RTEMS_BUILD),
#else
  { "RTEMS_BUILD", 0 },
#endif  

};

int gen_main(int c,char **v) {
    char *pkg = CG_DEFAULT_PKG;
    char *cls = CG_DEFAULT_CLS;
    int callId = CG_DEFAULT_NOCT;
    int i;
    int len = sizeof(constants)/sizeof(struct namepair);
    FILE* fout = stdout;
    union {
      char bytes[4];
      int  word;
    } testOrder;
    union {
      int words[2];
      double d;
    } testDouble;
    char *order;

/*
    endianness is tricky on the ARM: int words can be either
    little endian or big endian (although Linux uses it in
    little endian mode). Doubles are little endian within
    each of the two 32-bit words, but the two words are
    ordered in a BIG-ENDIAN order, so they are *swapped*
    with respect to the x86.
*/

    strncpy(testOrder.bytes, "\001\002\003\004", 4);
    testDouble.d=0.41;

    if (testOrder.word == 0x01020304) {
      order = "BIG_ENDIAN";
    } else if (testOrder.word == 0x04030201 && testDouble.words[0] == 0xA3D70A3D) {
      order = "LITTLE_ENDIAN";
    } else if (testOrder.word == 0x04030201 && testDouble.words[0] == 0x3FDA3D70) {
      order = "LITTLE_ENDIAN_DOUBLES_SWAPPED";
    } else {
      fprintf(stderr, "strange endianness %#x\n", testOrder.word);
      order = "-1";
    }
      
#ifndef RTEMS_BUILD      
    if (c<2) {
      fprintf(stderr,
	      "usage: %s [-noct] [package-name [interface-name]] output-file\n",
	      v[0]);
      return 1;
    }

    i = 1;
    if (i + 1 < c && !strcmp(v[i], "-noct")) {
      callId = 1;
      i++;
    }
    if (i + 1 < c)
      pkg = v[i++];
    if (i + 1 < c)
      cls = v[i++];
    
    fout = fopen(v[i],"w");
#endif
    
    if (fout==NULL) {
      fprintf(stderr,"error openning %s: %s\n",v[1],strerror(errno));
      return 1;
    }
    fprintf(fout,
           "/* DO NOT EDIT! - This file is auto-generated by the program\n"
           " * defined by constantGenerator.c\n"
           " * \n"
           " */\n"
           "package %s;\n"
           "/**\n"
           " * Defines Java level constants for a range of C level constants.\n"
           " * These include:\n"
           " * <ul>\n"
           " * <li>File I/O flags such as <code>O_RDONLY</code></li>\n"
           " * <li>All possible <code>errno</code> values</li>\n"
           " * <li>Special constants for use by a host VM, such as <code>STDOUT</code></li>\n"
           " * </ul>\n"
           " * @author constantGenerator.c\n"
           " */\n"
           "\n"
	    "public interface %s {\n",
	    pkg, cls
        );
    for (i = 0; i < len; i++) {
	fprintf(fout,
		(callId
		 ? "\tpublic static final int %s = Id.id(0x%x);\n"
		 : "\tpublic static final int %s = 0x%x;\n"),
	       constants[i].name,
	       constants[i].value);
    }
    len = sizeof(boolean_constants)/sizeof(struct namepair);
    for (i = 0; i < len; i++) {
    fprintf(fout,
	    (callId
	     ? "\tpublic static final boolean %s = Id.id(%s);\n"
	     : "\tpublic static final boolean %s = %s;\n"),
	    boolean_constants[i].name,
	    boolean_constants[i].value ? "true" : "false");
    }

    fprintf(fout,
	    (callId
	     ? "\tpublic static final int BYTE_ORDER = Id.id(%s);\n"
	     : "\tpublic static final int BYTE_ORDER = %s;\n"),
	    order);

    fprintf(fout,
	    (callId
	     ? "\tpublic static final int PAGE_SIZE = Id.id(%d);\n"
	     : "\tpublic static final int PAGE_SIZE = %d;\n"),
#ifdef RTEMS_BUILD
/*
FIXME: why this doesn't work in RTEMS ?
            (int) sysconf(_SC_PAGESIZE)
*/             
            PAGE_SIZE            
#else            
	    getpagesize()
#endif	    
    );

    fprintf(fout,"};\n");

#ifndef RTEMS_BUILD
    fclose(fout);
#endif    

    return 0;
}


#ifndef RTEMS_BUILD
int main(int argc, char **argv) {
  return gen_main(argc,argv);
}
#else

rtems_task Init(
  rtems_task_argument ignored
)
{
  int r;

  printf("----PROGRAM-STARTS-HERE----\n");  
  r=gen_main(0,(char **)0);
  printf("----PROGRAM-ENDS-HERE---- %d\n",r);    
  exit( r ); /* this does NOT get propagated through emulator, anyway */
}

/* configuration information */

#define CONFIGURE_APPLICATION_NEEDS_CONSOLE_DRIVER

#define CONFIGURE_RTEMS_INIT_TASKS_TABLE

#define CONFIGURE_MAXIMUM_TASKS 1

#define CONFIGURE_INIT

#define CONFIGURE_APPLICATION_DOES_NOT_NEED_CLOCK_DRIVER 

#include <rtems/confdefs.h>

#endif
