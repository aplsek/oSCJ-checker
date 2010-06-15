package test.runtime;
/**
 * @author Filip Pizlo
 */
final class LibraryImports {
    
    static final native int getErrno();
    static final native int getHErrno();
    static final native int pipe(int[] fds);
    static final native int socketpair(int domain,int type,int protocol,int[] sv);
    static final native int open(String name,int flags,int mode);
    static final native int mkstemp(String[] template);
    static final native int read(int fd,
				 byte[] buf,
				 int byteOffset,
				 int byteCount,
				 boolean blocking);
    static final native int write(int fd,
				  byte[] buf,
				  int byteOffset,
				  int byteCount,
				  boolean blocking);
    static final native int close(int fd);
    static final native int cancel(int fd);
    static final native int socket(int domain,int type,int protocol);
    static final native int bind(int sock,Object addr);
    static final native int listen(int sock,int queueSize);
    static final native int connect(int sock,Object addr);
    static final native int accept(int sock,
				   Object addr,
				   boolean blocking);
    static final native int setSoReuseAddr(int sock,boolean reuseAddr);
    static final native long length(String name);
    static final native int unlink(String name);
    static final native int rewind(int fd);
    static final native byte[][] getHostByName(String name);
    static final native String getHostByAddr(byte[] ip,int af);
    
 }