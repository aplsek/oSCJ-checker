/*
 * fdutils.c -- useful stuff for dealing with file descriptors
 * by Filip Pizlo, 2004
 */

#include "autodefs.h"

#include "util.h"
#include "fdutils.h"

#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <stdarg.h>

#ifdef RTEMS_BUILD
#include <termios.h>
#endif

int get_flags(int fd) {
    return fcntl(fd, F_GETFL, 0);
}

int set_flags(int fd, int flags) {
    return fcntl(fd, F_SETFL, flags);
}

int set_fl(int fd,int flags) {
    int res;
    res=fcntl(fd,F_GETFL,0);
    if (res<0) return -1;
    return fcntl(fd,F_SETFL,res|flags);
}

int clr_fl(int fd,int flags) {
    int res;
    res=fcntl(fd,F_GETFL,0);
    if (res<0) return -1;
    return fcntl(fd,F_SETFL,res&(~flags));
}

int makeNonBlocking(int fd) {
#ifdef RTEMS_BUILD
    struct termios flags;
    
    if ( tcgetattr( fd, &flags ) < 0 ) {
      /* TODO: Check this fallback works, for sockets, files... */
      return set_fl(fd,O_NONBLOCK);
    }
    
    flags.c_lflag &= ~( ICANON | ECHO | ECHONL | ECHOK | ECHOE | ECHOPRT | ECHOCTL );
    flags.c_cc[VMIN] = 0;
    flags.c_cc[VTIME] = 0;
    
    return tcsetattr( fd, TCSADRAIN, &flags );
#else
    return set_fl(fd,O_NONBLOCK);
#endif
}

int makeBlocking(int fd) {
#ifdef RTEMS_BUILD
    struct termios flags;
    
    flags.c_iflag = BRKINT | ICRNL | IXON | IMAXBEL;
    flags.c_lflag =  ISIG | ICANON | ECHO | ECHONL | ECHOK | ECHOE | ECHOPRT | ECHOCTL | IEXTEN;
    
    if (tcsetattr( fd, TCSADRAIN, &flags )) {
      /* TODO: Check this fallback works, for sockets, files... */
      
      return clr_fl(fd,O_NONBLOCK);
    } else {
    
      return 0;
    }
#else
    return clr_fl(fd,O_NONBLOCK);
#endif
}

int SBE_fd(int fd,int enabled) {
    int res=fcntl(fd,F_GETFL,0);
    if (res<0) {
        fprintf(stderr,"fcntl(%d,F_GETFL,0) failed in SBE(%s): %s\n",
                       fd,
                       enabled?"true":"false",
                       strerror(errno));
        abort();
    }
    
    if (enabled && (res&O_NONBLOCK)!=0) {
        /* we want to enable blocking but non-blocking flag is set.  so reset it. */
        if (fcntl(fd,F_SETFL,res&~O_NONBLOCK)<0) {
            fprintf(stderr,"fcntl(%d,F_SETFL,%x&~O_NONBLOCK) failed in SBE(%s): %s\n",
                           fd,
                           res,
                           enabled?"true":"false",
                           strerror(errno));
            abort();
        }
    }
    else
    if (!enabled && (res&O_NONBLOCK)==0) {
        /* we want to disable blocking but non-blocking flag is NOT set.  so set it. */
        if (fcntl(fd,F_SETFL,res|O_NONBLOCK)<0) {
            fprintf(stderr,"fcntl(%d,F_SETFL,%x|O_NONBLOCK) failed in SBE(%s): %s\n",
                           fd,
                           res,
                           enabled?"true":"false",
                           strerror(errno));
            abort();
        }
    }
    
    return (res&O_NONBLOCK)==0;
}


#ifdef RTEMS_BUILD

int SBE(int fd,int enabled) {

    struct termios flags;
    
    if ( tcgetattr( fd, &flags ) < 0 ) {
      /* TODO: Check this fallback works, for sockets, files... */
      return SBE_fd(fd,enabled);
    }
    
  if ((!enabled) && (flags.c_lflag&ICANON)) {
    flags.c_lflag &= ~( ICANON | ECHO | ECHONL | ECHOK | ECHOE | ECHOPRT | ECHOCTL );
    flags.c_cc[VMIN] = 0;
    flags.c_cc[VTIME] = 0;
    
    if (tcsetattr( fd, TCSADRAIN, &flags )) {
      fprintf(stderr, "tcsetattr failed in SBE on fd %d at line %d: %s\n",
        fd, __LINE__, strerror(errno));
      abort();
    };
  }
  else
  if (enabled && (!(flags.c_lflag&ICANON))) {
    flags.c_iflag = BRKINT | ICRNL | IXON | IMAXBEL;
    flags.c_lflag =  ISIG | ICANON | ECHO | ECHONL | ECHOK | ECHOE | ECHOPRT | ECHOCTL | IEXTEN;
    
    if (tcsetattr( fd, TCSADRAIN, &flags )) {
      fprintf(stderr, "tcsetattr failed in SBE on fd %d at line %d: %s\n",
        fd, __LINE__, strerror(errno));
      abort();
    };
  }
  
  return 0;
}  

#else
#define SBE	SBE_fd
#endif

int fSBE(FILE *file,int enabled) {
    int res;
    res=SBE(file->FILE_FD_FIELD,enabled);
    return res;
}

void ubprintf(const char *msg,...) {
    va_list lst;
    jboolean e=fSBE(stderr,1);
    va_start(lst,msg);
    vfprintf(stderr,msg,lst);
    va_end(lst);
    fSBE(stderr,e);
}




