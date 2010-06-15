/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This file defines helper functions for the native call interface. If
 * we want a native call that doesn't map directly to a library/system call
 * then we define a helper here to do the real call, and declare the helper
 * in ovm.core.execution.Native, or declare it via a LibraryGlue method from
 * the user domain.
 *
 * @author David Holmes
 * @author Krzysztof Palacz
 **/
 
#include "autodefs.h"

#include <unistd.h>
#include <time.h>
#include <signal.h>
#include <stdio.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>

#ifdef RTEMS_BUILD
  #include <sys/times.h>
#else  
  #include <sys/resource.h>
#endif  

#include <string.h>

#if defined(HAVE_STRINGS_H)
  #include <strings.h>
#endif
  
#include <errno.h>
#include <assert.h>
#include <fcntl.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <sys/ioctl.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <utime.h>
#include <netdb.h>
#include <dirent.h>

#if defined(HAVE_SYS_FILIO_H)
# include <sys/filio.h>
#endif
#include "native_helpers.h"
#include "fdutils.h"
#include "config.h"
#include "engine.h"
#include "util.h"
#include "jtypes.h"
#include <sys/time.h>
#include <sys/resource.h>

#if defined(HAVE_SYS_IO_H)
  #include <sys/io.h>
#endif  


#ifndef RUSAGE_SELF
#define RUSAGE_SELF 0
#endif


#ifndef SO_TIMEOUT
#ifndef SO_RCVTIMEO
#warning Neither SO_TIMEOUT or SO_RCVTIMEO are defined!
#warning This will cause all get/setOption calls with that value to throw an exception
#else
#define SO_TIMEOUT SO_RCVTIMEO
#endif /* not SO_RCVTIMEO */
#endif /* not SO_TIMEOUT */

#ifndef SOL_TCP
#define SOL_TCP 6   // hack!
#endif

#define DUMMY_PORT_IO

#ifdef OVM_XENOMAI

#include <native/task.h>
#include <rtdk.h>

#endif

jint getrusageSelf(jlong *data) {

#ifndef RTEMS_BUILD
    struct rusage ru;
    if (getrusage(RUSAGE_SELF,&ru)<0) {
	return -1;
    }
    data[0]=((jlong)1000)*ru.ru_utime.tv_usec+((jlong)1000000000)*ru.ru_utime.tv_sec;
    data[1]=((jlong)1000)*ru.ru_stime.tv_usec+((jlong)1000000000)*ru.ru_stime.tv_sec;
    data[2]=ru.ru_maxrss;
    data[3]=ru.ru_ixrss;
    data[4]=ru.ru_idrss;
    data[5]=ru.ru_isrss;
    data[6]=ru.ru_minflt;
    data[7]=ru.ru_majflt;
    data[8]=ru.ru_nswap;
    data[9]=ru.ru_inblock;
    data[10]=ru.ru_oublock;
    data[11]=ru.ru_msgsnd;
    data[12]=ru.ru_msgrcv;
    data[13]=ru.ru_nsignals;
    data[14]=ru.ru_nvcsw;
    data[15]=ru.ru_nivcsw;
    return 0;
#else
  struct tms tm;
  
  if(times(&tm) < 0) {
    return -1;
  }
  
  data[0] = ((jlong)1000000000LL)*tm.tms_utime / ((jlong)1000000000LL)*sysconf(_SC_CLK_TCK);
  data[1] = ((jlong)1000000000LL)*tm.tms_stime / ((jlong)1000000000LL)*sysconf(_SC_CLK_TCK);
  data[2] = -1;
  data[3] = -1;
  data[4] = -1;
  data[5] = -1;
  data[6] = -1;
  data[7] = -1;
  data[8] = -1;
  data[9] = -1;
  data[10] = -1;
  data[11] = -1;
  data[12] = -1;
  data[13] = -1;
  data[14] = -1;
  data[15] = -1;

  return 0;
#endif      
}

#ifndef NDEBUG
  extern void print_ustring( void * ptr );
#endif

void print_ustring_at_address( void * ptr ) {
#ifndef NDEBUG
  print_ustring(ptr);
#endif  
}

// these low-level print functions are useful when you don't
// trust the string conversion routines or you are debugging the
// low level I/O or string conversion routines. Or you want to
// avoid the allocation and general overhead of turning strings into
// byte arrays at the java level.
// The FILE is forced into blocking mode for these print functions

// NOTE: print_string*(String s) is rewritten to expose its internal byte[]
// to these methods asa jbyte* and a length.

jint print_string(const jbyte* bytes, jint len) {
    return print_string_on(stderr, bytes, len);
}

jint print_bytearr_len(const jbyte* bytes, jint len) {
    return print_string_on(stderr,bytes,len);
}

jint print_substring(const jbyte* bytes, jint len, jint offset, jint nbytes) {
    return print_substring_on(stderr, bytes, len, offset, nbytes);
}

jint print_substring_on(FILE* fp,
			const jbyte* bytes, 
			jint len, 
			jint offset, 
			jint nbytes) {
    jint res;
    jboolean enabled=fSBE(fp,1);
    assert(offset+nbytes <= len);
    res=fprintf(fp, "%.*s", nbytes, (bytes+offset) );
    fSBE(fp,enabled);
    return res;
}

jint print_string_on(FILE* fp, 
		     const jbyte* bytes, 
		     jint len) {
    jint res;
    jboolean enabled=fSBE(fp,1);
    res=fprintf(fp, "%.*s", len, bytes);
    fSBE(fp,enabled);
    return res;
}

jint print_char(jint val) {
    return print_char_on(stdout, val);
}

jint print_char_on(FILE* fp, jint val) {
    jint res;
    jboolean enabled=fSBE(fp,1);
    res=fputc(val, fp);
    fSBE(fp,enabled);
    return res;
}

jint print_formatted_int_on(FILE* fp, jint val, char* format) {
    jint res;
    jboolean enabled=fSBE(fp,1);
    res=fprintf(fp, format, val);
    fSBE(fp,enabled);
    return res;
}

jint print_int_on(FILE* fp, jint val) {
    return print_formatted_int_on(fp, val, "%d");
}

jint print_int(jint val) {
    return print_formatted_int_on(stderr, val, "%d");
}

jint print_int_as_unsigned(jint val) {
    return print_formatted_int_on(stderr, val, "%u");
}

jint print_hex_int(jint val) {
    return print_formatted_int_on(stderr, val, "%x");
}


#if SIZEOF_LONG == 8
#define JLONGFORMAT "%ld"
#define JLONGXFORMAT "%lx"
#else
#define JLONGFORMAT "%lld"
#define JLONGXFORMAT "%llx"
#endif

jint print_formatted_long_on(FILE* fp, jlong val, char* format) {
    jint res;
    jboolean enabled=fSBE(fp,1);
    res=fprintf(fp, format, val);
    fSBE(fp,enabled);
    return res;
}
jint print_long_on(FILE* fp, jlong val) {
    return print_formatted_long_on(fp, val, JLONGFORMAT);
}
jint print_long(jlong val) {
    return print_formatted_long_on(stderr, val, JLONGFORMAT);
}
jint print_hex_long(jlong val) {
    return print_formatted_long_on(stderr, val, JLONGXFORMAT);
}

jint print_ptr(void *ptr) {
    jint res;
    jboolean enabled=fSBE(stderr,1);
    res=fprintf(stderr, "%p", ptr);
    fSBE(stderr,enabled);
    return res;
}

jint print_boolean(jint val) {
    jint res;
    jboolean enabled=fSBE(stderr,1);
    res=fprintf(stderr, "%s", val ? "true" : "false");
    fSBE(stderr,enabled);
    return res;
}

/* note that a float arg is promoted to double before being passed */
jint print_float_on(FILE* fp, jdouble val) {
    jint res;
    jboolean enabled=fSBE(fp,1);
    res=fprintf(fp, "%f", val);
    fSBE(fp,enabled);
    return res;
}

jint print_double(jdouble val) {
    return print_double_on(stderr, val);
}

jint print_double_on(FILE* fp, jdouble val) {
    jint res;
    jboolean enabled=fSBE(fp,1);
    res=fprintf(fp, "%f", val);
    fSBE(fp,enabled);
    return res;
}

/* These provide access to the C level I/O streams for use by the BasicIO and
   other low-level "raw" services.
*/
jint getStdOut(void) { 
    return (jint)stdout; 
}
jint getStdErr(void) { 
    return (jint)stderr; 
}
jint getStdIn(void) { 
    return (jint)stdin; 
}

jint is_directory(char * name) {
  struct stat buf;
  int ret;

  ret = stat(name, &buf);
  if (ret != 0)
    return (jint) -1;
  if (S_ISDIR(buf.st_mode))
    return (jint) 1;
  else
    return (jint) 0;
}

jint is_plainfile(char * name) {
  struct stat buf;
  
  return ( (stat(name,&buf)==0) && 
	   (S_ISREG(buf.st_mode)) );
}

jlong file_size(jint fd) {
    struct stat st;
    if (fstat(fd, &st)<0) {
	return (jlong)-1;
    }
    // make it signed
    return (jlong)st.st_size;
}

jlong get_last_modified(char * filename) {
  struct stat __statBuffer;
  if (stat(filename,&__statBuffer)==0) 
    return ((long long)__statBuffer.st_mtime) * 1000;
  else 
    return -1;  
}

jint getmod(char * filename) {
  struct stat statBuffer;
  if (stat(filename, &statBuffer) == 0) 
    return (jint) statBuffer.st_mode;
  else 
    return -1;  
}

jint set_last_modified(char * filename, jlong time) {
  struct stat statBuffer;
  struct utimbuf utimeBuffer; 
  if (stat(filename,&statBuffer)==0) {
    utimeBuffer.actime  = statBuffer.st_atime; 
    utimeBuffer.modtime = (time_t) (time / 1000);
    if (0 == utime(filename, &utimeBuffer))
      return 0;
    else 
      return -1;
  } else 
    return -1;  
}

jint estimate_directory(char * name) {
  DIR * handle;
  struct dirent * ent;
  jint needed = 0;

  handle = opendir(name);
  if (handle == NULL)
    return -1;
  do {
    ent = readdir(handle);
    if (ent != NULL && strcmp(".", ent->d_name) && strcmp("..", ent->d_name))
      needed += strlen(ent->d_name) + 1;
  } while (ent != NULL);
  closedir(handle); 
  return needed;
}

jint list_directory(char * name,
		    char * buf, 
		    jint max) {
  DIR * handle;
  struct dirent * ent;
  jint pos = 0;

  handle = opendir(name);
  if (handle == NULL)
    return -1; /* open failed */
  do {
    ent = readdir(handle);
    if (ent != NULL) {
      int slen = strlen(ent->d_name) + 1;
      if (!strcmp(".", ent->d_name) || !strcmp("..", ent->d_name))
	continue;
      if (pos + slen <= max) {
	memcpy(&buf[pos],
	       ent->d_name,
	       slen);
	pos += slen;
      } else {
	closedir(handle);
	return -2; /* max too small, retry! */
      }
    }
  } while (ent != NULL);
  closedir(handle); 
  return pos;
}

jlong file_size_path(char *name) {
    struct stat st;
    if (stat(name, &st) < 0)
      return (jlong) -1;
    return (jlong)st.st_size;
}

jint mylock(jint fd,
	    jlong start,
	    jlong len,
	    jint shared) {
  struct flock fl;
  int ret;

  fl.l_type = (shared == 1) ? F_RDLCK : F_WRLCK;
  fl.l_whence = SEEK_SET;
  fl.l_start = start;
  fl.l_len = len;
  fl.l_pid = getpid();
  ret = fcntl(fd, F_SETLK, &fl);
  if (ret == -1) {
    if ( (errno == EACCES) ||
	 (errno == EAGAIN) )
      return 1;
  }
  return ret;
}

jint myunlock(jint fd,
	      jlong start,
	      jlong len) {
  struct flock fl;

  fl.l_type = F_UNLCK;
  fl.l_whence = SEEK_SET;
  fl.l_start = start;
  fl.l_len = len;
  fl.l_pid = getpid();
  return fcntl(fd, F_SETLK, &fl);
}

jint myrecvfrom(jint sock,
		char * buf,
		jint blen,
		jint flags,
		jint * addr) {
  struct sockaddr_in sa;
  socklen_t tolen = sizeof(sa);
  size_t ret;

  memset(&sa, 0, sizeof(sa)); 
  ret = recvfrom(sock,
		 buf,
		 blen,
		 flags,
		 (struct sockaddr*) &sa,
		 &tolen);
  addr[0] = ntohl(sa.sin_addr.s_addr);
  addr[1] = ntohs(sa.sin_port);
  return ret;
}

jint mysendto(jint sock,
	      char * buf,
	      jint blen,
	      jint flags,
	      jint ip,
	      jint port) {
  struct sockaddr_in sa;
  int ipnbo;
  socklen_t tolen = sizeof(sa);

  ipnbo = htonl(ip);
  memset(&sa, 0, sizeof(sa)); 
  sa.sin_family      = AF_INET;  
  sa.sin_addr.s_addr = ipnbo;
  sa.sin_port        = htons(port);

  return sendto(sock,
		buf,
		blen,
		flags,
		(struct sockaddr*) &sa,
		tolen);
}

jdouble strtod_helper(char* str) {
    char* endptr;
    jdouble val = 0.0;
    val = strtod(str, &endptr);
    return val;
}

jfloat intBitsToFloat(jint i) {
    jvalue t;
    t.jint = i;
    return t.jfloat;
}

jdouble longBitsToDouble(jlong l) {
    jwide t;
    t.jlong = l;
#if defined(OVM_ARM) && !defined(WORDS_BIGENDIAN)
    jint z=t.swap.x;
    t.swap.x=t.swap.y;
    t.swap.y=z;
#endif
    return t.jdouble;
}

jint floatToRawIntBits(jfloat f) {
    jvalue t;
    t.jfloat = f;
    return t.jint;
}

jlong doubleToRawLongBits(jdouble d) {
    jwide t;
    t.jdouble = d;
#if defined(OVM_ARM) && !defined(WORDS_BIGENDIAN)
    jint z=t.swap.x;
    t.swap.x=t.swap.y;
    t.swap.y=z;
#endif
    return t.jlong;
}

#ifdef NO_GETHOST
/* fake a host entry for localhost */
static char locaddr[] = { 127, 0, 0, 1 };
static char* addresses[] = { locaddr, NULL };
static struct hostent fake = {
    "localhost",
    NULL,
    2,
    4,
    addresses
};
#endif

jlong get_host_by_name(char * name,
		       char * ret,
		       jint retlen) {
  struct hostent * he = NULL;
  int i;
  jint p;
#ifndef NO_GETHOST
  he = gethostbyname(name);
#else
  if (strcmp(name, "localhost") == 0)
      he = &fake;
#endif
  if (he == NULL) {
    return (jlong)-1;
  }
  i = 0;
  p = 0;
  while ( (he->h_addr_list[i] != NULL) &&
	  (p+he->h_length < retlen) ) {
    memcpy(&ret[p],
	   he->h_addr_list[i],
	   he->h_length);
    i++;
    p+= he->h_length;
  }
  while (he->h_addr_list[i] != NULL)
    i++;
  return ((jlong) i | ((jlong)he->h_length << 32));
}

jint get_host_by_addr(char * ip, 
		      int iplen,
		      int af,
		      char * buf,
		      int buflen) {
  struct hostent * hostEntry = NULL; 
#ifndef NO_GETHOST 
  hostEntry = gethostbyaddr(ip, iplen, af); 
#endif
  if (hostEntry != NULL) { 
    strncpy(buf,
	    hostEntry->h_name,
	    buflen-1); 
    buf[buflen]='\0';
    return 0;
  } else 
    return -1;
}

jboolean is_file_valid(int fd) {
    struct stat st;
    int ret = fstat(fd, &st);
    /*   if (ret != 0) { */
    /*       fprintf(stderr, "fstat of %d failed: %s\n", fd, strerror(errno)); */
    /*   } */
    return ret == 0;
}

jint getErrno() { return errno; }
jint getHErrno() { return h_errno; }

jint get_error_string(char* buf, jint len) {
    return (jint)snprintf(buf, len, "%s", strerror(errno));
}

jint get_specific_error_string(jint err, char* buf, jint len) {
    return (jint)snprintf(buf, len, "%s", strerror(err));
}

jint get_specific_h_error_string(jint err, char* buf, jint len) {
    return (jint)snprintf(buf, len, "%s", hstrerror(err));
}

void dumpNativeProfileHisto(void);
void dumpCExceptionsStat(void);

#ifdef OVM_XENOMAI

// global variable read from jrt.c - not nice, yes
int exit_process_code = -211;
RT_TASK main_ovm_task;

#endif

uint64_t customCounters [MAX_CUSTOM_COUNTERS];

void exit_process(jint code) {

    int i;
#ifdef OVM_XENOMAI
    int rc;
#endif    
    
    fSBE(stdout,1);
    fSBE(stderr,1);
    
    dumpNativeProfileHisto();
    dumpCExceptionsStat();
    
#if 0
    fprintf(stderr,"\n Custom counters: \n");    
    for(i=0; i<MAX_CUSTOM_COUNTERS; i++) {
      fprintf(stderr, "%d %lld\n", i, customCounters[i]);
    }
    fprintf(stderr, "\n");
#endif
    
    fprintf(stderr,"# OVM exits.\n");

    /* print stack traces from somewhere else
    if (nativeContextHandle != 0) {
	fprintf(stderr, "# Stack trace:\n");
	stacktrace((NativeContext*)nativeContextHandle);
    }
    */
    fflush(stderr);
    fflush(stdout);

#ifndef OVM_XENOMAI
    exit(code);
#else    
    exit_process_code = code;

    rc=rt_task_resume( &main_ovm_task );
    if (rc) {
    	fprintf(stderr, "rt_task_resume: %d (%s)\n", rc, strerror(-rc) );
    }
    fprintf(stderr, "Resumed main task, suspending self.\n");    

    rc=rt_task_suspend( rt_task_self() );
    if (rc) {
    	fprintf(stderr, "rt_task_suspend: %d (%s)\n", rc, strerror(-rc) );
    }    	
    
    fprintf(stderr, "WARNING: Main task resume failed, exiting by crashing.\n");
    	// yes, this causes a crash, but most likely only because of a Xenomai bug
    rt_task_delete(rt_task_self()); // this will cause crash, should not be reached
#endif    
}

void abortDelayed(jint time) {
  sigset_t blockem;
  
  fprintf(stderr, "ovm process %d aborting in %d seconds, attach to gdb now\n",
	  getpid(), time);
  fflush(stderr);
  sigfillset(&blockem);
  sigdelset(&blockem, SIGTRAP);
  sigprocmask(SIG_BLOCK, &blockem, 0);
  sleep(time);
  abort();
}

#define BUFLEN 80
jlong readLong() {
    char buf[BUFLEN];
    jboolean inEnabled=fSBE(stdin,1);
    jlong retval = 0x8000000000000000LL;
    if (fgets(buf, BUFLEN, stdin) != buf) {
        jboolean outEnabled=fSBE(stdout,1);
        fprintf(stdout, "Error reading new value\n");
        fSBE(stdout,outEnabled);
    }
    else {
        if (sscanf(buf, "%lld*\n", &retval) != 1) {
            jboolean outEnabled=fSBE(stdout,1);
            fprintf(stdout, "Error converting input value\n");
            fSBE(stdout,outEnabled);
        }
        else {
/*              fprintf(stdout, "New value is: %lld\n", retval); */
        }
    }
    fSBE(stdin,inEnabled);
    return retval;
}

jint readInt() {
    char buf[BUFLEN];
    jboolean inEnabled=fSBE(stdin,1);
    jint retval = 0x80000000;
    if (fgets(buf, BUFLEN, stdin) != buf) {
        jboolean outEnabled=fSBE(stdout,1);
        fprintf(stdout, "Error reading new value\n");
        fSBE(stdout,outEnabled);
    }
    else {
        if (sscanf(buf, "%d*\n", &retval) != 1) {
            jboolean outEnabled=fSBE(stdout,1);
            fprintf(stdout, "Error converting input value\n");
            fSBE(stdout,outEnabled);
        }
        else {
/*              fprintf(stdout, "New value is: %d\n", retval); */
        }
    }
    fSBE(stdin,inEnabled);
    return retval;
}

/* return current time as nanoseconds since the EPOCH */
#if _POSIX_TIMERS > 0
jlong getCurrentTimeR(void) {
    struct timespec tm;
    int status = clock_gettime(CLOCK_REALTIME, &tm);
    jlong nanos_per_sec = 1000L * 1000 * 1000;
    jlong now = tm.tv_sec * nanos_per_sec + tm.tv_nsec;
    if (status != 0) {
        ASSERT(status == -1 && errno == EINVAL, "unexpected error from clock_getttime");
        return status;
    }
/*      printf("Using clock_gettime()\n");  */
    return now;
}

jint getClockResolution(void) {
    struct timespec tm;
/*      printf("Using clock_getres()\n");  */
    int status = clock_getres(CLOCK_REALTIME, &tm);
    if (status != 0) {
        ASSERT(status == -1, "wrong status returned from clock_getres");
        return status;
    }
    /* resolution should always be less than 1 second */
    ASSERT(tm.tv_sec == 0, "resolution greater than one second!!!!");
    return (jint) tm.tv_nsec;
}
jboolean supportsPOSIXTimers(void) {
/*      printf("returning true\n"); */
    return 1;
}
#else
jlong getCurrentTimeR(void) {
     struct timeval now;
     jlong micros_per_sec = 1000000;
     jlong nowNanos = 0;
     gettimeofday(&now, NULL);
/*      printf("Using gettimeofday()\n");  */
     nowNanos = (now.tv_usec + now.tv_sec * micros_per_sec) * 1000;

     return nowNanos;
}

jint getClockResolution(void) {
/*      printf("returning 10ms \n");  */
    return 10L * 1000 * 1000; /* 10ms UNIX default in nanoseconds */
}

jboolean supportsPOSIXTimers(void) {
    /*printf("returning false\n"); */
    return 0;
}
#endif /* _POSIX_TIMERS */

/* the last time that was recorded. We use this to watch for problems
   in the time values returned from the system calls. This is mainly a
   VMWare issue, but could also be a problem on some Linuxes when run on
   SMP's due to different TSC values at each CPU.
*/
static jlong lastTime = 0;

// a hack to make the values nice and small
extern jlong baseTime;

jlong getCurrentTime(void) {
    jlong now=getCurrentTimeR();
    if (now <= lastTime) {
        now = lastTime+1;  /* don't let time jump backwards */
    }
    else {
        lastTime = now;
    }
    
    // a hack to make the values nice and small
    
/*
    if (baseTime==-1) {
      baseTime = now;
      now = 0;
    } else {
      now -= baseTime;
    }
*/    
    return now - baseTime;
}

#ifndef MIN
#define MIN(a,b) (((a)<(b))?(a):(b))
#endif

jint get_process_arg(jint index, char* buf, jint buf_len) {
    if (index >= process_argc) {
	return -1;
    }
    strncpy(buf, process_argv[index], buf_len);
    return MIN(strlen(process_argv[index]), (unsigned)buf_len);
}

jint get_process_arg_count() {
    return (jint)process_argc;
}

jint myPipe(jint *fds) {
    int _fds[2];
    jint result=pipe(_fds);
    fds[0]=_fds[0];
    fds[1]=_fds[1];
    return result;
}

jint mySocketpair(jint *sv) {
#ifdef RTEMS_BUILD
    return -1;
#else
    int _sv[2];
    jint result=socketpair(AF_UNIX,SOCK_STREAM,0,_sv);
    sv[0]=_sv[0];
    sv[1]=_sv[1];
    return result;
#endif
}

jint myInetBind(jint sock,jint ipAddress,jint port) {
    struct sockaddr_in addr;
    int value = 1;
    unsigned addr_len=sizeof(addr);
    setsockopt(sock, 
	       SOL_SOCKET,
	       SO_REUSEADDR,
	       &value,
	       sizeof(int));
    bzero(&addr,addr_len);
    addr.sin_family=AF_INET;
    addr.sin_port=htons(port);
    addr.sin_addr.s_addr=htonl(ipAddress);
    return bind(sock,(struct sockaddr*)&addr,addr_len);
}

jint myInetConnect(jint sock,jint ipAddress,jint port) {
    struct sockaddr_in addr;
    unsigned addr_len=sizeof(addr);
    bzero(&addr,addr_len);
    addr.sin_family=AF_INET;
    addr.sin_port=htons(port);
    addr.sin_addr.s_addr=htonl(ipAddress);
    return connect(sock,(struct sockaddr*)&addr,addr_len);
}

jint myInetAccept(jint sock,jint *ipAddress,jint *port) {
    struct sockaddr_in addr;
    jint res;
    socklen_t addr_len=sizeof(addr);
    bzero(&addr,addr_len);
    addr.sin_family=AF_INET;
    res=accept(sock,(struct sockaddr*)&addr,&addr_len);
    if (res>=0) {
        *ipAddress=htonl(addr.sin_addr.s_addr);
        *port=ntohs(addr.sin_port);
    }
    return res;
}

jint myInetGetSockName(jint sock,jint *ipAddress,jint *port) {
    struct sockaddr_in addr;
    jint res;
    socklen_t addr_len=sizeof(addr);
    bzero(&addr,addr_len);
    addr.sin_family=AF_INET;
    res=getsockname(sock,(struct sockaddr*)&addr,&addr_len);
    if (res==0) {
        *ipAddress=htonl(addr.sin_addr.s_addr);
        *port=ntohs(addr.sin_port);
    }
    return res;
}

jint myInetGetPeerName(jint sock,jint *ipAddress,jint *port) {
    struct sockaddr_in addr;
    jint res;
    socklen_t addr_len=sizeof(addr);
    bzero(&addr,addr_len);
    addr.sin_family=AF_INET;
    res=getpeername(sock,(struct sockaddr*)&addr,&addr_len);
    if (res==0) {
        *ipAddress=htonl(addr.sin_addr.s_addr);
        *port=ntohs(addr.sin_port);
    }
    return res;
}

jint getSoError(jint sock) {
    jint so_error;
    socklen_t len=sizeof(so_error);
    if (getsockopt(sock,
                   SOL_SOCKET,
                   SO_ERROR,
                   &so_error,
                   &len)<0) {
        return errno;
    }
    return so_error;
}

jint setSoReuseAddr(jint sock,jboolean reuseAddr) {
    int on=reuseAddr?1:0;   // probably unnecessary - just playing it safe
    return setsockopt(sock,
                      SOL_SOCKET,
                      SO_REUSEADDR,
                      &on,
                      sizeof(on));
}

jint getSoReuseAddr(jint sock) {
    int on;
    socklen_t len=sizeof(on);
    if (getsockopt(sock,
                   SOL_SOCKET,
                   SO_REUSEADDR,
                   &on,
                   &len)<0) {
        return -1;
    }
    return on?1:0;  // canonicalize
}

jint setSoKeepAlive(jint sock,jboolean keepAlive) {
    int on=keepAlive?1:0;   // probably unnecessary - just playing it safe
    return setsockopt(sock,
                      SOL_SOCKET,
                      SO_KEEPALIVE,
                      &on,
                      sizeof(on));
}

jint getSoKeepAlive(jint sock) {
    int on;
    socklen_t len=sizeof(on);
    if (getsockopt(sock,
                   SOL_SOCKET,
                   SO_KEEPALIVE,
                   &on,
                   &len)<0) {
        return -1;
    }
    return on?1:0;  // canonicalize
}

jint setSoLinger(jint sock,jboolean onoff,jint linger) {
    struct linger l;
    l.l_onoff=onoff?1:0;
    l.l_linger=linger;
    return setsockopt(sock,
                      SOL_SOCKET,
                      SO_LINGER,
                      &l,
                      sizeof(l));
}

jint getSoLinger(jint sock,jboolean *onoff,jint *linger) {
    struct linger l;
    socklen_t len=sizeof(l);
    if (getsockopt(sock,
                   SOL_SOCKET,
                   SO_LINGER,
                   &l,
                   &len)<0) {
        return -1;
    }
    *onoff=l.l_onoff;
    *linger=l.l_linger;
    return 0;
}

jint setSoTimeout(jint sock,jint timeout) {
#ifdef SO_TIMEOUT
    int val=timeout;    // just in case sizeof(jint)!=sizeof(int)
    return setsockopt(sock,
                      SOL_SOCKET,
                      SO_TIMEOUT,
                      &val,
                      sizeof(val));
#else
    errno=EINVAL;
    return -1;
#endif
}

jint getSoTimeout(jint sock) {
#ifdef SO_TIMEOUT
    int val;
    socklen_t len=sizeof(val);
    if (getsockopt(sock,
                   SOL_SOCKET,
                   SO_TIMEOUT,
                   &val,
                   &len)<0) {
        return -1;
    }
    return val;
#else
    errno=EINVAL;
    return -1;
#endif
}

jint setSoOOBInline(jint sock,jboolean oobInline) {
    int on=oobInline?1:0;   // probably unnecessary - just playing it safe
    return setsockopt(sock,
                      SOL_SOCKET,
                      SO_OOBINLINE,
                      &on,
                      sizeof(on));
}

jint getSoOOBInline(jint sock) {
    int on;
    socklen_t len=sizeof(on);
    if (getsockopt(sock,
                   SOL_SOCKET,
                   SO_OOBINLINE,
                   &on,
                   &len)<0) {
        return -1;
    }
    return on?1:0;  // canonicalize
}

jint setTcpNoDelay(jint sock,jboolean noDelay) {
    int on=noDelay?1:0;   // probably unnecessary - just playing it safe
    return setsockopt(sock,
                      SOL_TCP,
                      TCP_NODELAY,
                      &on,
                      sizeof(on));
}

jint getTcpNoDelay(jint sock) {
    int on;
    socklen_t len=sizeof(on);
    if (getsockopt(sock,
                   SOL_TCP,
                   TCP_NODELAY,
                   &on,
                   &len)<0) {
        return -1;
    }
    return on?1:0;  // canonicalize
}

jint availableForRead(jint fd) {
    jint ret;
    jint res=ioctl(fd,FIONREAD,&ret);
    if (res<0) {
	return -1;
    }
    return ret;
}

jint rewindFd(jint fd) {
    if (lseek(fd,0,SEEK_SET)==0) {
        return 0;
    }
    return -1;
}

jint getFdMode(jint fd) {
    struct stat st;
    if (fstat(fd,&st)<0) {
        return -1;
    }
    return st.st_mode;
}

int myWrite(int fd, void *buf, size_t cnt) {
    printf("myWrite(%d, %p, %u)\n",
    	   fd,buf,cnt);
    return write(fd,buf,cnt);
}

void turnOffBuffering() {
    setvbuf(stdout,NULL,_IONBF,0);
    setvbuf(stderr,NULL,_IONBF,0);
}

void dumpProfileHisto(char *name,
		      jint nameSize,
		      jlong *histo,
		      jint histoSize,
		      jlong overflow) {
    char buf[256];
    if (nameSize<256) {
	memcpy(buf,name,nameSize);
	buf[nameSize]=0;
    } else {
	memcpy(buf,name,255);
	buf[255]=0;
    }
    FILE *flout=fopen(buf,"w");
    if (flout!=NULL) {
	jint i;
	fprintf(flout,"Filename: %s\n",buf);
	fprintf(flout,"Overflow: %qd\n",overflow);
	fprintf(flout,"Bins:\n");
	for (i=0;i<histoSize;++i) {
	    fprintf(flout,"%8d %8qd\n",i,histo[i]);
	}
	fclose(flout);
    }
}

jlong RUsage_getSysTime() {

#ifndef RTEMS_BUILD
  struct rusage usage;
  
  if (getrusage(RUSAGE_SELF, &usage) < 0)
    return -1;
  else
    return (((jlong) usage.ru_stime.tv_sec) * 1000000LL
	    + usage.ru_stime.tv_usec);
#else
  struct tms tm;
  
  if(times(&tm) < 0) {
    return -1;
  }
  
  return ((jlong)1000000LL)*tm.tms_stime / ((jlong)1000000LL)*sysconf(_SC_CLK_TCK);
#endif	    
}

jlong RUsage_getUserTime() {

#ifndef RTEMS_BUILD
  struct rusage usage;

  if (getrusage(RUSAGE_SELF, &usage) < 0)
    return -1;
  else
    return (((jlong) usage.ru_utime.tv_sec) * 1000000LL
	    + usage.ru_utime.tv_usec);
#else
  struct tms tm;
  
  if(times(&tm) < 0) {
    return -1;
  }
  
  return ((jlong)1000000LL)*tm.tms_utime / ((jlong)1000000LL)*sysconf(_SC_CLK_TCK);
#endif	    
}

#ifndef DUMMY_PORT_IO

void ovm_outb( int value, int address ) {

	if (!address) {
		/* initialization of a hardware object generates write to address zero */
		return;
	}
	
//	fprintf(stderr, "***Port write to address %x value %x (char %c) \n", address, value, value );
	outb(value,address);

}


int ovm_inb( int address ) {

	return inb(address);
}

#else

char dummy_inb_counter=0;

int ovm_inb( int address ) {

	dummy_inb_counter++;
	dummy_inb_counter%=256;
	
	fprintf(stderr, "***Port read from address %x (returning value %d) ***\n", address, dummy_inb_counter );
	return dummy_inb_counter;
}

void ovm_outb( int value, int address ) {

	if (!address) {
		/* initialization of a hardware object generates write to address zero */
		return;
	}
	
	fprintf(stderr, "***Port write to address %x value %x (char %c) \n", address, value, value );

}

#endif

#ifdef OVM_X86

jlong getTimeStamp(void) {

  uint32_t lo, hi;
  __asm__ __volatile__ (      // serialize
    "xorl %%eax,%%eax \n        cpuid" ::: "%rax", "%rbx", "%rcx", "%rdx");
    
   /* We cannot use "=A", since this would use %rax on x86_64 */
   __asm__ __volatile__ ("rdtsc" : "=a" (lo), "=d" (hi));
   return (uint64_t)hi << 32 | lo;
}

#else

jlong getTimeStamp(void) {
 return 0;
}

#endif
