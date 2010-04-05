
/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This file defines helper functions for the native call interface. If
 * we want a native call that doesn't map directly to a library/system call
 * then we define a helper here to do the real call, and declare the helper
 * in ovm.core.execution.Native
 *
 * @author David Holmes
 * @author Krzysztof Palacz
 **/
#ifndef _NATIVE_HELPERS_
#define _NATIVE_HELPERS_
#include <stdio.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <stdint.h>
#include "jtypes.h"

jint getrusageSelf(jlong *data);

void turnOffBuffering();
void print_ustring_at_address( void * ptr );

jint print_bytearr_len(const jbyte* bytes, jint len);
jint print_string_on(FILE* fp, const jbyte* bytes, jint len);
jint print_string(const jbyte* bytes, jint len);
#define print(bytes, len) print_string((bytes),(len))
jint print_substring(const jbyte* bytes, jint len, jint offset, jint nbytes);
jint print_substring_on(FILE* fp, const jbyte* bytes, jint len, jint offset, jint nbytes);
jint print_char_on(FILE* fp, jint val);
jint print_char(jint val);
jint print_int_on(FILE* fp, jint val);
jint print_int(jint val);
jint print_int_as_unsigned(jint val);
jint print_hex_int(jint val);
jint print_ptr(void* ptr);
jint print_boolean(jint val);
jint print_long_on(FILE* fp, jlong val);
jint print_long(jlong val);
jint print_hex_long(jlong val);
jint print_double(jdouble val);
jint print_double_on(FILE* fp, jdouble val);
/* note that float arg is promoted to double before call */
jint print_float_on(FILE* fp, jdouble val);


jint getStdOut(void);
jint getStdErr(void);
jint getStdIn(void);

// stat/fstat wrappers
jlong file_size(jint fd);
jlong file_size_path(char* name);
jboolean is_file_valid(jint fd);

jboolean file_mkdir(char *name);

jdouble strtod_helper(char *str);
jfloat intBitsToFloat(jint i);
jdouble longBitsToDouble(jlong l);
jint floatToRawIntBits(jfloat f);
jlong doubleToRawLongBits(jdouble d);

void exit_process(jint code);

void abortDelayed(jint seconds);

jint getErrno();
jint getHErrno();
jint get_error_string(char* buf, jint len);
jint get_specific_error_string(jint err, char* buf, jint len);
jint get_specific_h_error_string(jint err, char* buf, jint len);

jint readInt();
jlong readLong();

long long getCurrentTimeR(void);
long long getCurrentTime(void);
jint getClockResolution(void);
jboolean supportsPOSIXTimers(void);

jint get_process_arg(jint index, char* buf, jint buf_len);
jint get_process_arg_count();


jint mylock(jint fd, jlong start, jlong len, jint shared);
jint myunlock(jint fd,jlong start, jlong len);

jint myPipe(jint *fds);
jint mySocketpair(jint *sv);

jint myInetBind(jint sock,jint ipAddress,jint port);
jint myInetConnect(jint sock,jint ipAddress,jint port);
jint myInetAccept(jint sock,jint *ipAddress,jint *port);
jint myInetGetSockName(jint sock,jint *ipAddress,jint *port);
jint myInetGetPeerName(jint sock,jint *ipAddress,jint *port);
jint getSoError(jint sock);

jint setSoReuseAddr(jint sock,jboolean reuseAddr);
jint getSoReuseAddr(jint sock);
jint setSoKeepAlive(jint sock,jboolean keepAlive);
jint getSoKeepAlive(jint sock);
jint setSoLinger(jint sock,jboolean onoff,jint linger);
jint getSoLinger(jint sock,jboolean *onoff,jint *linger);
jint setSoTimeout(jint sock,jint timeout);
jint getSoTimeout(jint sock);
jint setSoOOBInline(jint sock,jboolean oobInline);
jint getSoOOBInline(jint sock);
jint setTcpNoDelay(jint sock,jboolean noDelay);
jint getTcpNoDelay(jint sock);

jint availableForRead(jint fd);

jint makeNonBlocking(jint fd);
jint makeBlocking(jint fd);

jint mysendto(jint sock,
	      char * buf,
	      jint blen,
	      jint flags,
	      jint ip,
	      jint port);


jint myrecvfrom(jint sock,
		char * buf,
		jint blen,
		jint flags,
		jint * addr);

jint list_directory(char * name,
		    char * buf, 
		    jint max);

jint estimate_directory(char * name);

jint getmod(char * filename);
 
 
jint rewindFd(jint fd);

jint getFdMode(jint fd);

jint get_host_by_addr(char * ip, int iplen, int af, char * buf, int buflen);

jint is_directory(char * name);
jlong get_last_modified(char * filename);
jint is_plainfile(char * name);
jint set_last_modified(char * filename, jlong time);
jlong get_host_by_name(char * name,
		       char * ret,
		       int retlen);

void dumpProfileHisto(char *name,
		      jint nameSize,
		      jlong *histo,
		      jint histoSize,
		      jlong overflow);
jlong RUsage_getSysTime();
jlong RUsage_getUserTime();

void ovm_outb( int value, int address );
int ovm_inb( int address );
jlong getTimeStamp(void);

#define MAX_CUSTOM_COUNTERS 5
extern uint64_t customCounters[MAX_CUSTOM_COUNTERS];

static inline void incrementCounter(int code) {
  assert( code<MAX_CUSTOM_COUNTERS );
  customCounters[code]++;
}


#endif





