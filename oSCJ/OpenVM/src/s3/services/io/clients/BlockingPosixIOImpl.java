
// FIXME:!!!! not hacked to work with arraylets

package s3.services.io.clients;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Field;
import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.execution.Context;
import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.stitcher.ThreadServiceConfigurator;
import ovm.core.stitcher.ThreadServicesFactory;
import ovm.services.io.async.IPv4SocketAddress;
import ovm.services.io.async.SocketAddress;
import ovm.services.io.async.Linger;
import ovm.services.io.clients.PosixIO;
import ovm.services.threads.UserLevelThreadManager;
import ovm.util.OVMError;

/**
 * Implementation of POSIX I/O abstraction layer using the blocking
 * system calls.
 *
 * @author Jason Baker
 * @author Filip Pizlo
 * @author David Holmes
 * @author Christian Grothoff
 */
public class BlockingPosixIOImpl extends PosixIO implements NativeConstants {
    
    Domain dom;
    UserLevelThreadManager tm;

    private BlockingPosixIOImpl(Domain dom_) {
	this.dom = dom_;
	tm = (UserLevelThreadManager)
	    ((ThreadServicesFactory)ThreadServiceConfigurator.config.
	     getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
	if (tm == null) {
	    throw new OVMError.Configuration("need a configured thread manager");
	}
    }
    
    private void setErrno(int errno) {
        Context ctx = tm.getCurrentThread().getContext();
        ((OVMThreadContext) ctx).setErrno(errno);
    }
    
    private int check(int ret, int errno) {
	if (ret < 0)
	    setErrno(errno);
	return ret;
    }

    private long check(long ret, int errno) {
	if (ret < 0)
	    setErrno(errno);
	return ret;
    }
    
    /**
     * Given a user-domain array of primitives, an offset and a count,
     * verify all arguments and return a displaced pointer into the
     * array's values
     * 
     * @param buf        any primitive array type
     * @param byteOffset offset from address of first elt in bytes
     * @param byteCount  number of bytes we shall access
     *
     * @return the displaced pointer or the null address if the
     * arguments are invalid.  If null is returned, we set the current
     * thread's errno to EFAULT.
     *
     */
    private VM_Address getPointer(Oop buf, int byteOffset, int byteCount) {
	if (byteOffset < 0 || byteCount <= 0) {
	    setErrno(EFAULT);
	    return VM_Address.fromObject(null);
	}
	Blueprint bp = buf.getBlueprint();
	if (!bp.getType().isArray()) {
	    setErrno(EFAULT);
	    return VM_Address.fromObject(null);
	}
	if (!(bp.getType().asArray().getComponentType().isPrimitive())) {
	    setErrno(EFAULT);
	    return VM_Address.fromObject(null);
	}

	Blueprint.Array abp = (Blueprint.Array) bp;
	VM_Address base = abp.addressOfElement(buf, 0);
	int len = abp.getLength(buf);
	VM_Address end
	    = VM_Address.fromObject(buf).add(abp.byteOffset(len));
	VM_Address ret = base.add(byteOffset);
	if (ret.add(byteCount).uGT(end)) {
	    setErrno(EFAULT);
	    return VM_Address.fromObject(null);
	}
	return ret;
    }
    
   // private int getIntFromIntArray(Oop buf,int index) {
   //     return ((Blueprint.Array)buf.getBlueprint()).addressOfElement(buf,index).getInt();
   // }

//    private void setIntInIntArray(Oop buf,int index,int value) {
//        ((Blueprint.Array)buf.getBlueprint()).addressOfElement(buf,index).setInt(value);
//    }

    public int getErrno() {
	Context ctx = tm.getCurrentThread().getContext();
	return ((OVMThreadContext) ctx).getErrno();
    }
    
    public int getHErrno() {
        throw new Error("implement me!");
    }
    
    public Oop getHostByName(Oop name) {
        throw new Error("implement me!");
    }
    
    public Oop getHostByAddr(Oop ip,int af) {
        throw new Error("implement me!");
    }

    public boolean access(Oop name,
			  int mode) {
	return (0 == Native.access(dom.getLocalizedCString(name),
				   mode));
    }   
   
    public int mkdir(Oop name,
		     int mode) {
        return Native.mkdir(dom.getLocalizedCString(name),
			    mode);
    }
    
    public int renameTo(Oop oldpath,
			Oop newpath) {
	return Native.rename(dom.getLocalizedCString(oldpath),
			     dom.getLocalizedCString(newpath));
    }    
 
    public long getLastModified(Oop name) {
	return Native.get_last_modified(dom.getLocalizedCString(name));
    }
    
    public int setLastModified(Oop name,
			       long time) {
	return Native.set_last_modified(dom.getLocalizedCString(name), time);
    }
  
    public boolean isPlainFile(Oop name) {
        return (1 == Native.is_plainfile(dom.getLocalizedCString(name)));
    }

    public boolean isDirectory(Oop name) {
        return (1 == Native.is_directory(dom.getLocalizedCString(name)));
    }      
   
    public byte[] list_directory(Oop name) {
	byte[] dname = dom.getLocalizedCString(name);
	int ret = -2;
	byte[] buf = null;
	while (ret == -2) {
	    // note that due to concurrent modification the estimate
	    // might be wrong, so we go again until we succeed.
	    int size = Native.estimate_directory(dname);
	    //buf = new byte[size];
	    buf = MemoryManager.the().allocateContinuousByteArray(size);
	    ret = Native.list_directory(dname,
					buf,
					buf.length);	    
	}
	if (ret == -1)
	    return null;
	return buf;
    }

    public int getmod(Oop name) {
	return Native.getmod(dom.getLocalizedCString(name)); 	
    }

    public int chmod(Oop name, int mode) {
	return Native.chmod(dom.getLocalizedCString(name), mode);
    }

    public int unlink(Oop name) {
        return Native.unlink(dom.getLocalizedCString(name));
    }
    
    public int rmdir(Oop name) {
        return Native.rmdir(dom.getLocalizedCString(name));
    }
    
    public long length(Oop name) {
	return Native.file_size_path(dom.getLocalizedCString(name));
    }
    
    public int pipe(Oop fds) {
	int[] ds = new int[2];
        MemoryManager.the().setPrimitiveArrayElement(fds,0,(int) -1);
        MemoryManager.the().setPrimitiveArrayElement(fds,1,(int) -1);

        if (check(Native.pipe(ds), Native.getErrno()) < 0)
	    return -1;
	
        MemoryManager.the().setPrimitiveArrayElement(fds,0,(int) ds[0]);
        MemoryManager.the().setPrimitiveArrayElement(fds,1,(int) ds[1]);

        return 0;
    }

    public int socketpair(int domain,int type,int protocol,Oop sv) {
        MemoryManager.the().setPrimitiveArrayElement(sv,0,(int)-1);
        MemoryManager.the().setPrimitiveArrayElement(sv,1,(int)-1);
        
        int[] ds=new int[2];
	if (check(Native.socketpair(domain,type,protocol,ds),
		  Native.getErrno()) < 0)
	    return -1;

        MemoryManager.the().setPrimitiveArrayElement(sv,0,(int)ds[0]);
        MemoryManager.the().setPrimitiveArrayElement(sv,1,(int)ds[1]);

        return 0;
    }
    
    public int socket(int domain,int type,int protocol) {
	return check(Native.socket(domain,type,protocol),
		     Native.getErrno());
    }
    
    private Field findFieldByName(Type.Compound tc,
                                  String name) {
        // FIXME: this is a brain-dead didn't-know-any-better implementation.
        // please fix it if you know better.
        
        Field.Iterator iter=tc.fieldIterator();
        while (iter.hasNext()) {
            Field f=iter.next();
            if (f.getSelector().getName().equals(name)) {
                return f;
            }
        }
        
        return null;
    }
    
    private SocketAddress parseUserSocketAddress(Oop userSa) {
        Type t=userSa.getBlueprint().getType();
        if (!t.isCompound()) {
            setErrno(NativeConstants.EINVAL);
            return null;
        }
        Type.Compound tc=t.asCompound();
        try {
            if (((Field.Integer)findFieldByName(tc,"family")).get(userSa)
                != NativeConstants.AF_INET) {
                setErrno(NativeConstants.ENOTSUP);
                return null;
            }
            return new IPv4SocketAddress(((Field.Integer)findFieldByName(tc,"addr")).get(userSa),
                                         ((Field.Integer)findFieldByName(tc,"port")).get(userSa));
        } catch (ClassCastException e) {
            setErrno(NativeConstants.EINVAL);
            return null;
        } catch (NullPointerException e) {
            setErrno(NativeConstants.EINVAL);
            return null;
        }
    }
    
    private boolean populateUserSocketAddress(Oop userSa,
                                              SocketAddress execSa) {
        IPv4SocketAddress inetSa=(IPv4SocketAddress)execSa;
        Type t=userSa.getBlueprint().getType();
        if (!t.isCompound()) {
            setErrno(NativeConstants.EINVAL);
            return false;
        }
        Type.Compound tc=t.asCompound();
        try {
            ((Field.Integer)findFieldByName(tc,"family")).set(userSa,NativeConstants.AF_INET);
            ((Field.Integer)findFieldByName(tc,"addr")).set(userSa,inetSa.getIPv4Address());
            ((Field.Integer)findFieldByName(tc,"port")).set(userSa,inetSa.getPort());
            return true;
        } catch (ClassCastException e) {
            setErrno(NativeConstants.EINVAL);
            return false;
        } catch (NullPointerException e) {
            setErrno(NativeConstants.EINVAL);
            return false;
        }
    }
    
    private Linger parseUserLinger(Oop userLinger) {
        Type t=userLinger.getBlueprint().getType();
        if (!t.isCompound()) {
            setErrno(NativeConstants.EINVAL);
            return null;
        }
        Type.Compound tc=t.asCompound();
        try {
            return new Linger(((Field.Boolean)findFieldByName(tc,"onoff")).get(userLinger),
                              ((Field.Integer)findFieldByName(tc,"linger")).get(userLinger));
        } catch (ClassCastException e) {
            setErrno(NativeConstants.EINVAL);
            return null;
        } catch (NullPointerException e) {
            setErrno(NativeConstants.EINVAL);
            return null;
        }
    }
    
    private boolean populateUserLinger(Oop userLinger,
                                       Linger execLinger) {
        Type t=userLinger.getBlueprint().getType();
        if (!t.isCompound()) {
            setErrno(NativeConstants.EINVAL);
            return false;
        }
        Type.Compound tc=t.asCompound();
        try {
            ((Field.Boolean)findFieldByName(tc,"onoff")).set(userLinger,execLinger.onoff);
            ((Field.Integer)findFieldByName(tc,"linger")).set(userLinger,execLinger.linger);
            return true;
        } catch (ClassCastException e) {
            setErrno(NativeConstants.EINVAL);
            return false;
        } catch (NullPointerException e) {
            setErrno(NativeConstants.EINVAL);
            return false;
        }
    }
    
    public int getsockname(int sock,Oop address) {
        int[] ipAddress=new int[1];
        int[] port=new int[1];
        if (check(Native.myInetGetSockName(sock,ipAddress,port),
                  Native.getErrno())<0) {
            return -1;
        }
        return populateUserSocketAddress(address,
            new IPv4SocketAddress(ipAddress[0],port[0]))?0:-1;
    }
    
    public int getpeername(int sock,Oop address) {
        int[] ipAddress=new int[1];
        int[] port=new int[1];
        if (check(Native.myInetGetPeerName(sock,ipAddress,port),
                  Native.getErrno())<0) {
            return -1;
        }
        return populateUserSocketAddress(address,
            new IPv4SocketAddress(ipAddress[0],port[0]))?0:-1;
    }
    
    public int bind(int sock,Oop address) {
	SocketAddress addr=parseUserSocketAddress(address);
	if (addr==null) {
	    return -1;
	}
	throw new OVMError.Unimplemented();
	// return check(Native.bind(sock, addr), Native.getErrno());
    }
    
    public int listen(int sock,int queueSize) {
	return check(Native.listen(sock, queueSize), Native.getErrno());
    }
    
    public int setSoReuseAddr(int sock,boolean reuseAddr) {
        return check(Native.setSoReuseAddr(sock,reuseAddr),
                     Native.getErrno());
    }

    public int getSoReuseAddr(int sock) {
        return check(Native.getSoReuseAddr(sock),
                     Native.getErrno());
    }
    
    public int setSoKeepAlive(int sock,boolean keepAlive) {
        return check(Native.setSoKeepAlive(sock,keepAlive),
                     Native.getErrno());
    }
    
    public int getSoKeepAlive(int sock) {
        return check(Native.getSoKeepAlive(sock),
                     Native.getErrno());
    }
    
    public int setSoLinger(int sock,Oop linger) {
        Linger l=parseUserLinger(linger);
        if (l==null) {
            return -1;
        }
        return check(Native.setSoLinger(sock,l.onoff,l.linger),
                     Native.getErrno());
    }
    
    public int getSoLinger(int sock,Oop l) {
        boolean[] onoff=new boolean[1];
        int[] linger=new int[1];
        int res=check(Native.getSoLinger(sock,onoff,linger),
                      Native.getErrno());
        if (res<0) {
            return -1;
        }
        return populateUserLinger(l,new Linger(onoff[0],linger[0]))?0:-1;
    }
    
    public int setSoTimeout(int sock,int timeout) {
        return check(Native.setSoTimeout(sock,timeout),
                     Native.getErrno());
    }
    
    public int getSoTimeout(int sock) {
        return check(Native.getSoTimeout(sock),
                     Native.getErrno());
    }
    
    public int setSoOOBInline(int sock,boolean oobInline) {
        return check(Native.setSoOOBInline(sock,oobInline),
                     Native.getErrno());
    }
    
    public int getSoOOBInline(int sock) {
        return check(Native.getSoOOBInline(sock),
                     Native.getErrno());
    }
    
    public int setTcpNoDelay(int sock,boolean noDelay) {
        return check(Native.setTcpNoDelay(sock,noDelay),
                     Native.getErrno());
    }
    
    public int getTcpNoDelay(int sock) {
        return check(Native.getTcpNoDelay(sock),
                     Native.getErrno());
    }

    public boolean isValid(int fd) {
	return Native.is_file_valid(fd);
    }
    
    public void forceValid(int fd) {}
    public void ecrofValid(int fd) {}

    public int receive(int sock,
		       Oop source_addr,
		       Oop dstBuf,
		       int len,
		       boolean blocking) {
	if (!blocking) {
	    setErrno(NativeConstants.ENOSYS);
	    return -1;
	}
	int eno = EAGAIN;
	int rlen = -1;
	int[] addr = new int[2];
	while (eno == EAGAIN) {
	    rlen = Native.myrecvfrom(sock,
				    getPointer(dstBuf, 0, len),
				    len,
				    NativeConstants.MSG_DONTWAIT,
				    addr);
	    if (rlen == -1)
		eno = Native.getErrno();
	    else
		break;
	} 
	if (rlen == -1)
	    return -1;
	IPv4SocketAddress isa
	    = new IPv4SocketAddress(addr[0],
				    addr[1]);
	if (! populateUserSocketAddress(source_addr,
					isa))
	    return -1;
	return rlen;
    }
    
    public int sendto(Oop address,
		      Oop buf,
		      int off,
		      int len) {
	SocketAddress addr
	    = parseUserSocketAddress(address);
	if (addr == null) {
	    return -1;
	}
	boolean old
	    = tm.setReschedulingEnabled(false);
	int sock = -1;
	try {
	    final IPv4SocketAddress ta
		= (IPv4SocketAddress)address;

	    sock = Native.socket(AF_INET,
				 SOCK_DGRAM,
				 17 /* udp */);
	    if (sock == -1)
		return -1;
	    VM_Address bptr 
		= getPointer(buf, off, len);
	    if (bptr.isNull())
		return -1;
	    return check(Native.mysendto(sock,
					 bptr,
					 len,
					 NativeConstants.MSG_NOSIGNAL,
					 ta.getIPv4Address(),
					 ta.getPort()),
			 Native.getErrno());
	} catch (ClassCastException cce) {
	    setErrno(NativeConstants.EPROTONOSUPPORT);
	    return -1;
	} finally {	
	    tm.setReschedulingEnabled(old);    
	    if (sock != -1)
		Native.close(sock);
	}      	
    }
    
    public long lseek(int fd,
		      long offset,
		      int whence) {
	return check(Native.lseek(fd,offset,whence),
		     Native.getErrno());
    }
    
    public long length(int fd) {
	return check(Native.file_size(fd),
		     Native.getErrno());
    }    
   
    public int getAvailable(int fd) {
        return check(Native.availableForRead(fd),Native.getErrno());
    }

    public int ftruncate(int fd, long size) {
	return check(Native.ftruncate(fd, size),
		     Native.getErrno());
    }

    /**
     * Lock a region in a file.
     */
    public int lock(int fd,
		    long start,
		    long size,
		    boolean shared,
		    boolean wait) {
	throw new Error("not implemented");
    }

    /**
     * Lock a region in a file.
     */
    public int unlock(int fd,
		      long start,
		      long size) {
	throw new Error("not implemented");
    }
    
    public int fsync(int fd) {
	return check(Native.fsync(fd),
		     Native.getErrno());
    }

    public int connect(int sock,
		       Oop address) {
	if (sock == -1)
	    return -1;
	SocketAddress addr
	    = parseUserSocketAddress(address);
	if (addr == null) 
	    return -1;	
	boolean old
	    = tm.setReschedulingEnabled(false);
	try {
	    final IPv4SocketAddress ta
		= (IPv4SocketAddress)address;
	    return check(Native.myInetConnect(sock,
					      ta.getIPv4Address(),
					      ta.getPort()),
			 Native.getErrno());
	} catch (ClassCastException cce) {
	    setErrno(NativeConstants.EPROTONOSUPPORT);
	    return -1;
	} finally {	
	    tm.setReschedulingEnabled(old);    
	}      	
    }
    
    public int accept(int sock, Oop address, boolean blocking) {
	if (!blocking) {
	    setErrno(NativeConstants.ENOSYS);
	    return -1;
	}
	boolean old
	    = tm.setReschedulingEnabled(false);
	try {
	    int[] ipaddress = new int[1];
	    int[] port = new int[1];
	    int as = Native.myInetAccept(sock,
					 ipaddress,
					 port);	    
	    if (as == -1) {
		return check(as,
			     Native.getErrno());
	    }
	    IPv4SocketAddress isa
		= new IPv4SocketAddress(ipaddress[0],
					port[0]);
	    if (! populateUserSocketAddress(address,
					    isa)) {
		Native.close(as);
		return -1;
	    }
	    return as;
	} catch (ClassCastException cce) {
	    setErrno(NativeConstants.EPROTONOSUPPORT);
	    return -1;
	} finally {	
	    tm.setReschedulingEnabled(old);    
	}      	
    }

    public int open(Oop _name, int flags, int mode) {
	byte[] name = dom.getLocalizedCString(_name);
	return check(Native.open(name, flags, mode),
		     Native.getErrno());
    }
    
    public int mkstemp(Oop templateRef) {
	throw new OVMError.Unimplemented();
	// we must build a C string and copy the updated value back
	// into the template.  What is the template?  A byte[]?
    }
    
    public int read(int fd,
                    Oop buf,
                    int byteOffset,
                    int byteCount,
                    boolean blocking) {
	if (byteCount==0) {
	    return 0;
	}
	boolean old = tm.setReschedulingEnabled(false);
	try {
	    VM_Address addr = getPointer(buf, byteOffset, byteCount);
	    if (addr.isNull()) {
		return -1;
            }
            if (check(blocking?Native.makeBlocking(fd)
		              :Native.makeNonBlocking(fd),
                      Native.getErrno())<0) {
                return -1;
            }
	    return check(Native.read(fd, addr, byteCount),
			 Native.getErrno());
	} finally {
	    tm.setReschedulingEnabled(old);
	}
    }
    
    private static byte[] skipBucket = new byte[4096];
    public long skip(int fd, long offset, boolean blocking) {
	if (!blocking) {
	    setErrno(NativeConstants.ENOSYS);
	    return -1;
	}

	int toRead = offset>skipBucket.length?skipBucket.length:(int)offset;
	int res    = Native.read(fd, skipBucket, toRead);
	int errno  = Native.getErrno();
	
	return check(res,errno);
    }
    
    public int write(int fd,
		     Oop buf,
		     int byteOffset,
		     int byteCount,
		     boolean blocking) {
	if (!blocking) {
	    setErrno(NativeConstants.ENOSYS);
	    return -1;
	}
	if (byteCount==0) {
	    return 0;
	}
	boolean old = tm.setReschedulingEnabled(false);
	try {
	    VM_Address addr = getPointer(buf, byteOffset, byteCount);
	    if (addr.isNull())
		return -1;
	    return check(Native.write(fd, addr, byteCount),
			 Native.getErrno());
	} finally {
	    tm.setReschedulingEnabled(old);
	}
    }
    
    public int cancel(int fd) {
	throw new OVMError.Unimplemented();
    }
    
    public int close(int fd) {
	return check(Native.close(fd), Native.getErrno());
    }

    public static class Factory implements PosixIO.Factory {
	public PosixIO make(Domain dom) {
	    return new BlockingPosixIOImpl(dom);
	}
    }
}
