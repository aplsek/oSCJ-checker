package s3.services.io.clients;

import ovm.core.domain.*;
import ovm.core.execution.*;
import ovm.core.services.memory.*;
import ovm.core.services.threads.*;
import ovm.core.stitcher.*;
import ovm.services.io.async.*;
import ovm.services.io.blocking.*;
import ovm.services.io.clients.*;
import ovm.services.threads.*;
import ovm.util.*;
import ovm.core.*;
import s3.core.domain.*;
import s3.util.*;
import ovm.core.services.io.BasicIO;

/**
 * Implementation of POSIX I/O abstraction layer using the OVM
 * Async I/O and Blocking I/O services.
 *
 * @author Jason Baker
 * @author Filip Pizlo
 * @author David Holmes
 * @author Christian Grothoff
 */
public class PosixIOImpl extends PosixIO implements NativeConstants {

    private static final boolean DEBUG_READ = false;
    
    Domain dom;
    UserLevelThreadManager tm;
    BlockingManager bm;
    AsyncIOServicesFactory aiosf;
    IODescriptor[] table = new IODescriptor[NativeConstants.FD_SETSIZE];
    int[] refCountTable = new int[NativeConstants.FD_SETSIZE];
    volatile boolean inited = false;

    private PosixIOImpl(Domain dom,
			UserLevelThreadManager tm,
                        BlockingManager bm,
                        AsyncIOServicesFactory aiosf) {
	this.dom = dom;
	this.tm = tm;
        this.bm = bm;
        this.aiosf = aiosf;
    }
    
    // FIXME: This service should be initialized at a well-defined time during
    // the boostrap process so that each method doesn't have to call
    // assertInited(). It should be initialized before the event manager is
    // enabled. Note you won't need to disable rescheduling during 
    // initialization if it's done during bootstrap.
    // It should be a service instance.
    private void assertInited() {
        // quick return if already initialized
        if (inited) {
            return;
        }
        boolean enabled=tm.setReschedulingEnabled(false);
        try {
            if (inited) {
                return;
            }
            
            VM_Area prev=U.ei();
            try {
                table[0]=aiosf.getStdIOManager().getStdIn();
                table[1]=aiosf.getStdIOManager().getStdOut();
                table[2]=aiosf.getStdIOManager().getStdErr();
            } finally {
                U.l(prev);
            }

            inited=true;
        } catch (IOException e) {
            throw new ovm.util.OVMError.Internal(
                "Unable to get standard IODescriptors: "+e);
        } finally {
            tm.setReschedulingEnabled(enabled);
        }
    }
    
    public int allocateDescriptor(IODescriptor io) throws PragmaNoBarriers {
        assertInited();
        boolean enabled=tm.setReschedulingEnabled(false);
        try {
            for (int i=0;i<table.length;++i) {
                if (table[i]==null && refCountTable[i]==0) {
                    table[i]=io;
                    return i;
                }
            }
            throw Executive.panic("Ran out of IO descriptor slots");
        } finally {
            tm.setReschedulingEnabled(enabled);
        }
    }
    
    public void freeDescriptor(int iod) throws PragmaNoPollcheck {
        table[iod]=null;
    }
    
    public void forceValid(int fd) throws PragmaNoPollcheck {
        refCountTable[fd]++;
    }
    
    public void ecrofValid(int fd) throws PragmaNoPollcheck {
        refCountTable[fd]--;
    }
    
    IODescriptor getIOD(int iod) {
	assertInited();
	if (iod<0 || iod>=table.length || table[iod]==null) {
	    setErrno(NativeConstants.EBADF);
	    return null;
	}
	return table[iod];
    }
    
    private void setErrno(int errno) {
        Context ctx = tm.getCurrentThread().getContext();
        ((OVMThreadContext) ctx).setErrno(errno);
    }
    
    private void setHErrno(int h_errno) {
        Context ctx = tm.getCurrentThread().getContext();
        ((OVMThreadContext) ctx).setHErrno(h_errno);
    }
    
    private void setErrno(IOException error) {
        int errno;
        if (error instanceof IOException.System) {
            errno=((IOException.System)error).getErrno();
        } else if (error instanceof IOException.HostLookup) {
            setHErrno(((IOException.HostLookup)error).getHErrno());
            errno=0;
        } else if (error instanceof IOException.Canceled) {
            errno=NativeConstants.ECANCELED;
        } else if (error instanceof IOException.Unsupported) {
            errno=NativeConstants.ENOTSUP;
        } else if (error instanceof IOException.Unimplemented ||
                   error instanceof IOException.Internal) {
            errno=NativeConstants.ENOSYS;
        } else {
            errno=-1;
        }
        setErrno(errno);
    }
    
    private boolean check(BlockingCallback cback) {
        cback.waitOnDone();
        
        IOException error=cback.getError();
        if (error!=null) {
            setErrno(error);
            return false;
        }
        
        return true;
    }

    private int check(int ret, int errno) {
	if (ret < 0)
	    setErrno(errno);
	return ret;
    }
  
    public boolean isDirectory(Oop name) {
        return (1 == Native.is_directory(dom.getLocalizedCString(name)));
    }

    public boolean isPlainFile(Oop name) {
        return (1 == Native.is_plainfile(dom.getLocalizedCString(name)));
    }

    /**
     * Determines if verifyPointer() really does anything.
     */
    public static boolean SHOULD_VERIFY_POINTERS = false;
    
    /**
     * Verify that the given user-domain object is in fact an array of
     * primitives and that the given byteOffset and byteCount can be used
     * with getPointer() without causing a panic.
     */
    private boolean verifyPointer(Oop buf, int byteOffset, int byteCount) {
        if (!SHOULD_VERIFY_POINTERS) {
            return true;
        }
        
        if (byteOffset < 0) {
            return false;
        }
        
	if (byteCount <= 0) {
            return false;
	}
        
	Blueprint bp = buf.getBlueprint();
        
	if (!bp.getType().isArray()) {
            return false;
	}
        
	if (!(bp.getType().asArray().getComponentType().isPrimitive())) {
            return false;
	}

	// FIXME: What is this?  I think there was some sort of JIT
	// bug involved here.  Is the bug still alive?
	MemoryManager.the().pin(buf);
	try {
            Blueprint.Array abp = (Blueprint.Array) bp;
            VM_Address base = abp.addressOfElement(buf, 0);
	    int len = abp.getLength(buf);
            VM_Address end
                = VM_Address.fromObject(buf).add(abp.byteOffset(len));
            VM_Address ret = base.add(byteOffset);
            if (ret.add(byteCount).uGT(end)) {
                return false;
            }
        } finally {
	    MemoryManager.the().unpin(buf);
	}
        return true;
    }
    
    
    
    // arraylet support 
    
    /*
    
    static private Oop getReadOnlyContiguousCopy(Oop buf, int byteOffset, int byteCount)  {
      Oop res = MemoryManager.the().allocateContinuousByteArray( byteCount );
      MemoryManager.the().copyArrayElements(buf, byteOffset, res, 0, byteCount );
    }
    
    static private void releaseReadOnlyContiguousCopy(Oop copy, Oop buf, int byteOffset, int byteCount)  {
      // nothing to be done
      // btw - what if the mutator is writing to the array from another thread ?
    };
    
    static private Oop getContiguousCopy(Oop buf, int byteOffset, int byteCount)  {
    }
    
    static private void releaseContiguousCopy(Oop buf, int byteOffset, int byteCount)  {  
    }
    */
    
    /**
     * Given a user-domain array of primitives, an offset and a count,
     * verify all arguments and return a displaced pointer into the
     * array's values
     * 
     * @param buf        any primitive array type
     * @param byteOffset offset from address of first elt in bytes
     * @param byteCount  number of bytes we shall access
     *
     * @return the displaced pointer.
     *
     */
     
    // with arraylets - null means that the requested part of the buffer is not contiguous 
    // includes pointer forwarding (implicitly)
    
    static private VM_Address getPointer(Oop buf, int byteOffset, int byteCount) throws PragmaNoReadBarriers {
    

        if (byteOffset < 0) {
            ovm.core.Executive.panic("in getPointer(): byteOffset is negative");
        }
	if (byteCount <= 0) {
            ovm.core.Executive.panic("in getPointer(): byteCount is negative or zero");
	}
        
	Blueprint bp = buf.getBlueprint();
	if (!bp.getType().isArray()) {
            ovm.core.Executive.panic("in getPointer(): Oop buf is not an array");
	}
	if (!(bp.getType().asArray().getComponentType().isPrimitive())) {
            ovm.core.Executive.panic("in getPointer(): Oop buf is not an array of primitives");
	}

	Blueprint.Array abp = (Blueprint.Array) bp;
	int len = abp.getLength(buf);
	
	VM_Address ret = null;
	if (MemoryManager.the().usesArraylets()) {
	
	  int componentSize = abp.getComponentSize();
	  int dataSize = componentSize * len;
	  
          int lastByte = byteOffset + byteCount -1;
          
	  if (lastByte >= dataSize) {
	    ovm.core.Executive.panic("in getPointer(): byte count exceedes array");
	  }
	  
	  VM_Address aptr = VM_Address.fromObjectNB(buf).add( ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD );
	  VM_Address alet = aptr.getAddress();

// NO!!!!
//          if (alet.diff(VM_Address.fromObjectNB(buf)).uLT( VM_Word.fromInt(MemoryManager.the().sizeOfContinuousArray(abp, len)) )) {
          if (alet == VM_Address.fromObjectNB(buf).add( MemoryManager.the().continuousArrayBytesToData(abp,len) )) {
	    // the array is continuous, so it's easy

            //Native.print_string("getPointer: called on contiguous array\n");

	    ret = alet.add(byteOffset);
	    if (true) {
	      MemoryManager.the().checkAccess( ret );
	      MemoryManager.the().checkAccess( ret.add(byteCount-1) );
	    }
	    return ret;
	  } 
	  
	  // we know the array is not continuous, but maybe... just maybe
	  //  the requested portion of it is in a single arraylet
	  
	  int arrayletSize = MemoryManager.the().arrayletSize();
	  int startingArraylet = byteOffset / arrayletSize;
	  int endingArraylet = lastByte / arrayletSize;
	  
	  if (startingArraylet == endingArraylet) {
	    // we are very lucky today
            //Native.print_string("getPointer: called on contiguous part of an array\n");
            
            int innerOffset = byteOffset % arrayletSize;
	    ret = aptr.add( startingArraylet * MachineSizes.BYTES_IN_ADDRESS ).getAddress().add(innerOffset);
            if (true) {
	      MemoryManager.the().checkAccess( ret );
	      MemoryManager.the().checkAccess( ret.add(byteCount-1) );
	    }
	    return ret;  
	  }
          // ovm.core.Executive.panic("in getPointer(): the requested part of the array is not contiguous");	  
          // create a contiguous copy / space for the array
          
          //Native.print_string("getPointer: returning null, because called on non-contiguous fragment\n");
          
          return null;
          
	} else {
    	  VM_Address base = abp.addressOfElement(buf, 0);
    	  VM_Address end
	    = VM_Address.fromObject(buf).add(abp.byteOffset(len));
          ret = base.add(byteOffset);
          if (ret.add(byteCount).uGT(end)) {
            
            Native.print_string("buf: ");
            Native.print_ptr(VM_Address.fromObject(buf));
            Native.print_string("\n");
            
            Native.print_string("base: ");
            Native.print_ptr(base);
            Native.print_string("\n");
            
            Native.print_string("ret: ");
            Native.print_ptr(ret);
            Native.print_string("\n");

            Native.print_string("end: ");
            Native.print_ptr(end);
            Native.print_string("\n");
            
            Native.print_string("byteOffset: ");
            Native.print_int(byteOffset);
            Native.print_string("\n");

            Native.print_string("byteCount: ");
            Native.print_int(byteCount);
            Native.print_string("\n");
            
            Native.print_string("getVariableSize(): ");
            Native.print_int(abp.getVariableSize(buf));
            Native.print_string("\n");
	    
            ovm.core.Executive.panic("in getPointer(): ret + byteCount > end");
          }
        }
        
	return ret;
    }
    

    static final class Nat implements NativeInterface {
        static native void memcpy(VM_Address to, VM_Address from, int nb);
    }
        
    // does nothing except for replicating compaction without arraylets
    // in that case, it should be called after read operation
    //	ptr - displaced pointer to read data (gotten using getPointer(buf, byteOffset, byteCount))
    //  byteCount - can be reduced to the number of bytes really modified (read)
    
    // this function has no pollchecks, so buf needs not be pinned because of this function
    static public void handleReplication(Oop buf, int byteOffset, int byteCount, VM_Address ptr) {
    
      if (!MemoryManager.the().hasArrayReplicas()) {
        return ;
      }
      // no arraylets here, because arraylets don't need replication
      
      Oop otherBuf = MemoryManager.the().getOtherReplica(buf);
      
      VM_Address bufPtr = VM_Address.fromObjectNB(buf);
      VM_Address otherBufPtr = VM_Address.fromObjectNB(otherBuf);
      
      if (bufPtr == otherBufPtr) {
        return; // there is in fact no replica of this buffer
      }
      
      int offset = ObjectModel.getObjectModel().headerSkipBytes() + MachineSizes.BYTES_IN_WORD + byteOffset ;
      
      VM_Address dPtr = bufPtr.add(offset);
      VM_Address otherDPtr = otherBufPtr.add(offset);
      
      if (dPtr == ptr) {
        Nat.memcpy( otherDPtr, ptr, byteCount );
      } else {
        assert( otherDPtr == ptr );
        Nat.memcpy( dPtr, ptr, byteCount );      
      }
    }
    
/*    
    private int getIntFromIntArray(Oop buf,int index) {
        return ((Blueprint.Array)buf.getBlueprint()).addressOfElement(buf,index).getInt();
    }
*/    
/*
    private void setIntInIntArray(Oop buf,int index,int value) {
        ((Blueprint.Array)buf.getBlueprint()).addressOfElement(buf,index).setInt(value);
    }
*/
    public int getErrno() {
	Context ctx = tm.getCurrentThread().getContext();
	return ((OVMThreadContext) ctx).getErrno();
    }

    public int getHErrno() {
	Context ctx = tm.getCurrentThread().getContext();
	return ((OVMThreadContext) ctx).getHErrno();
    }

    /**
     * creates a user-domain array type from a user-domain type    
     */
    private Type.Array _makeUserArray(Type memberType){
	Type.Array arrayType = ((S3Domain)dom).makeType(memberType, 1);
	return arrayType;
    }

    private Oop _allocateUserArray(Type.Array arrayType, int size){
	S3Blueprint.Array arrayBP;
	arrayBP = (S3Blueprint.Array)dom.
	    blueprintFor(arrayType).asArray();
	return dom.getCoreServicesAccess().allocateArray(arrayBP, size);
    }
    
    public Oop getHostByAddr(Oop ipOop,int af) {
        if (af!=NativeConstants.AF_INET) {
            setErrno(ENOTSUP);
            return null;
        }
            
        byte[] ip = new byte[4];
        MemoryManager.the().copyArrayElements(ipOop,0,
                                    VM_Address.fromObject(ip).asOop(),0,
                                    4);
        
        InetAddress addr=new IPv4Address(ip);
        
        //Native.print_string("address: "+addr+"\n");
        
        BlockingCallback bc=new BlockingCallback(bm,tm);
        aiosf.getHostLookupManager().getHostByAddr(addr,bc);
        if (!check(bc)) {
            return null;
        }
        byte[] hostname=
            ((HostLookupManager.GetHostByAddrFinalizer)bc.getFinalizer()).
            getHostname();
	// Host names should always be 7 bit, either plain ascii or
	// punycode (RFC 3492).  Should we try to translate punycode
	// to proper UTF8 or UTF16?
        return dom.makeString(UnicodeBuffer.factory().wrap(hostname,0,hostname.length-1));
    }

    public Oop getHostByName(Oop hostname) {
	// Host names should always be 7 bit, either plain ascii or
	// punycode (RFC 3492).  Should we translate non-ascii names
	// to punycode?
	byte[] bytes=dom.getLocalizedCString(hostname);
        BlockingCallback bc=new BlockingCallback(bm,tm);
        aiosf.getHostLookupManager().getHostByName(bytes,bc);
        if (!check(bc)) {
            return null;
        }
        InetAddress[] addresses=
            ((HostLookupManager.GetHostByNameFinalizer)bc.getFinalizer()).
            getAddresses();
	Type.Array byteArray
	    = ((S3Domain)dom).commonTypes().arr_byte; 
	Type.Array byteArrayArray
	    = _makeUserArray(byteArray);
	S3Blueprint.Array arrayarrayBP = (S3Blueprint.Array)dom.
	    blueprintFor(byteArrayArray).asArray();
	Oop theDarnArrayArray = _allocateUserArray(byteArrayArray, addresses.length);
	
	// fill it
	for (int i=0; i < addresses.length; i++) { 
	    byte[] address=addresses[i].getIPAddress();
                
	    dom. blueprintFor(byteArray).asArray();// used ?  (this has side effect
	    // so I am loath to remove it. --jv
	    Oop theDarnArray = _allocateUserArray(byteArray, address.length);
                
	    MemoryManager.the().copyArrayElements(VM_Address.fromObject(address).asOop(), 0,
					theDarnArray, 0,
					address.length);
                
	    //arrayarrayBP.addressOfElement(theDarnArrayArray, i)
	//	.setAddress(VM_Address.fromObject(theDarnArray));
	
	    MemoryManager.the().setReferenceArrayElement(theDarnArrayArray, i, theDarnArray);
	    }
	return theDarnArrayArray;
    }

    public boolean access(Oop name,
			  int mode) {
	return (0 == Native.access(dom.getLocalizedCString(name),
				   mode));
    }   
  
    public int mkdir(Oop name,
		     int mode) {
        return check(Native.mkdir(dom.getLocalizedCString(name),
				  mode),
		     Native.getErrno());
    }    

    public int renameTo(Oop oldpath,
			Oop newpath) {
	return check(Native.rename(dom.getLocalizedCString(oldpath),
				   dom.getLocalizedCString(newpath)),
		     Native.getErrno());
    }    
 
    public long getLastModified(Oop name) {
	return Native.get_last_modified(dom.getLocalizedCString(name));
    }
    
    public int setLastModified(Oop name,
			       long time) {
	return Native.set_last_modified(dom.getLocalizedCString(name), time);
    }
  
    public byte[] list_directory(Oop name) {
	byte[] dname = dom.getLocalizedCString(name);
	int ret = -2;
	byte[] buf = null;
	while (ret == -2) {
	    // note that due to concurrent modification the estimate
	    // might be wrong, so we go again until we succeed.
	    int size = Native.estimate_directory(dname);
	    buf = null;
	    if (MemoryManager.the().usesArraylets()) {
	      buf = MemoryManager.the().allocateContinuousByteArray(size);
	    } else {
	      buf = new byte[size];
            }
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
	byte[] cname = dom.getLocalizedCString(name);
        if (Native.unlink(cname) != 0) {
		return -1;
	} else {
	    return 0;
	}
    }
    
    public int rmdir(Oop name) {
	byte[] cname = dom.getLocalizedCString(name);
        if (Native.rmdir(cname) != 0) {
		return -1;
	} else {
	    return 0;
	}
    }
    
    public long length(Oop name) {
	return Native.file_size_path(dom.getLocalizedCString(name));
    }
    
    public boolean isValid(int fd) {
	return getIOD(fd)!=null;
    }
    
    public int pipe(Oop fds) {
        MemoryManager.the().setPrimitiveArrayElement(fds,0,(int)-1);
        MemoryManager.the().setPrimitiveArrayElement(fds,1,(int)-1);
        
        IODescriptor[] ds=new IODescriptor[2];
        try {
            aiosf.getPipeManager().pipe(ds);
        } catch (IOException e) {
            setErrno(e);
            return -1;
        }

        MemoryManager.the().setPrimitiveArrayElement(fds,0,(int)allocateDescriptor(ds[0]));
        MemoryManager.the().setPrimitiveArrayElement(fds,1,(int)allocateDescriptor(ds[1]));

        return 0;
    }

    public int socketpair(int domain,int type,int protocol,Oop sv) {
        MemoryManager.the().setPrimitiveArrayElement(sv,0,(int)-1);
        MemoryManager.the().setPrimitiveArrayElement(sv,1,(int)-1);
        
        IODescriptor[] ds=new IODescriptor[2];
        try {
            aiosf.getSocketManager().socketpair(domain,type,protocol,ds);
        } catch (IOException e) {
            setErrno(e);
            return -1;
        }
        
        MemoryManager.the().setPrimitiveArrayElement(sv,0,(int)allocateDescriptor(ds[0]));
        MemoryManager.the().setPrimitiveArrayElement(sv,1,(int)allocateDescriptor(ds[1]));

        return 0;
    }
    
    public int socket(int domain,int type,int protocol) {
        try {
            return allocateDescriptor(aiosf.getSocketManager().socket(domain,type,protocol));
        } catch (IOException e) {
            setErrno(e);
            return -1;
        }
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
    
    public int bind(int sock,Oop address) {
	IODescriptor io=getIOD(sock);
	if (io==null) return -1;
        
        try {
            SocketAddress addr
		= parseUserSocketAddress(address);
            if (addr==null) {
                return -1;
            }
            ((SocketIODescriptor)io).bind(addr);
        } catch (ClassCastException e) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        } catch (IOException e) {
            setErrno(e);
            return -1;
        }
        
        return 0;
    }
    
    public int listen(int sock,int queueSize) {
	IODescriptor io=getIOD(sock);
	if (io==null) return -1;
        
        try {
            ((SocketIODescriptor)io).listen(queueSize);
        } catch (ClassCastException e) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        } catch (IOException e) {
            setErrno(e);
            return -1;
        }
        
        return 0;
    }
    
    private SocketIODescriptor asSock(int sock) throws IOException.System {
	IODescriptor io=getIOD(sock);
	if (io==null) {
	    // total hack
	    throw IOException.System.make(getErrno());
	}
	return (SocketIODescriptor)io;
    }

    private int sockError(Exception t) {
        if (t instanceof ClassCastException) {
            setErrno(NativeConstants.EINVAL);
        } else if (t instanceof IOException) {
            setErrno((IOException)t);
        } else {
            throw new OVMError("Unexpected bad thing happened: "+t);
        }
        return -1;
    }
    
    public int setSoReuseAddr(int sock,boolean reuseAddr) {
        try { asSock(sock).setSoReuseAddr(reuseAddr); return 0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public int setSoKeepAlive(int sock,boolean keepAlive) {
        try { asSock(sock).setSoKeepAlive(keepAlive); return 0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public int setSoLinger(int sock,Oop linger) {
        Linger l=parseUserLinger(linger);
        if (l==null) {
            return -1;
        }
        try { asSock(sock).setSoLinger(l); return 0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public int setSoOOBInline(int sock,boolean oobInline) {
        try { asSock(sock).setSoOOBInline(oobInline); return 0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public int setTcpNoDelay(int sock,boolean noDelay) {
        try { asSock(sock).setTcpNoDelay(noDelay); return 0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public int getSoReuseAddr(int sock) {
        try { return asSock(sock).getSoReuseAddr()?1:0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public int getSoKeepAlive(int sock) {
        try { return asSock(sock).getSoKeepAlive()?1:0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public int getSoLinger(int sock,Oop linger) {
        Linger l=new Linger();
        try { asSock(sock).getSoLinger(l); }
        catch (Exception t) { return sockError(t); }
        if (!populateUserLinger(linger,l)) {
            return -1;
        }
        return 0;
    }



    public int receive(int sock,
		       Oop source_addr,
		       Oop buf,
		       int byteCount,
		       boolean blocking) {
        if (byteCount==0) {
            return 0;
        }
        if (!verifyPointer(buf,0,byteCount)) {
            setErrno(NativeConstants.EFAULT);
            return -1;
        }
	IODescriptor io=getIOD(sock);
	if (io==null) return -1;
        if (!(io instanceof SocketIODescriptor)) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        }        
	int numBytes;
	SocketAddress isa;
	if (blocking) {
	    BlockingCallback bc=new BlockingCallback(bm,tm);
	    ((SocketIODescriptor)io)
		.receive(new ForReadMemoryCallback(buf,0,byteCount),
			 byteCount,
			 bc);        
	    if (!check(bc)) {
		return -1;
	    }      
	    numBytes = ((RWIODescriptor.ReadFinalizer)
			bc.getFinalizer()).getNumBytes();
	    isa = ((SocketIODescriptor.ReceiveFinalizer)
		   bc.getFinalizer()).getRemoteSocketAddress();
	} else {
	    SocketAddress[] isaCell=new SocketAddress[1];
	    try {
	        numBytes=receiveFastPath( (SocketIODescriptor)io, buf, 0, byteCount, isaCell );

	    } catch (IOException e) {
		setErrno(e);
		return -1;
	    }
	    if (numBytes<0) {
		setErrno(NativeConstants.EWOULDBLOCK);
		return -1;
	    }
	    isa=isaCell[0];
	}
	if (! populateUserSocketAddress(source_addr,
					isa))
	    return -1;	
	return numBytes;
    }
    
    public int sendto(Oop address,
		      Oop buf,
		      int off,
		      int len) {
	SocketAddress sa
	    = parseUserSocketAddress(address);
	if (sa==null) {
	    return -1;
	}
	SocketIODescriptor io = null;
	try {
	    io = aiosf.getSocketManager().socket(NativeConstants.AF_INET,
						NativeConstants.SOCK_DGRAM,
						17 /* UDP */);
	    return 
		io.sendto(sa,
			  new ForWriteMemoryCallback(buf, off, len),
			  len);
	} catch (IOException ioe) {
	    setErrno(ioe);
	    return -1;
	} finally {
	    if (io != null)
                io.close();
	}
    }
    
    public int getSoOOBInline(int sock) {
        try { return asSock(sock).getSoOOBInline()?1:0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public int getTcpNoDelay(int sock) {
        try { return asSock(sock).getTcpNoDelay()?1:0; }
        catch (Exception t) { return sockError(t); }
    }
    
    public long lseek(int fd,
		      long offset,
		      int whence) {
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
	
	if (!(io instanceof SeekableIODescriptor)) {
	    setErrno(NativeConstants.EINVAL);
	    return -1;
	}
	
	if (whence==NativeConstants.SEEK_CUR &&
	    offset==0) {
	    try {
		return ((SeekableIODescriptor)io).tell();
	    } catch (IOException e) {
		setErrno(e);
		return -1;
	    }
	}
	
        BlockingCallback bc=new BlockingCallback(bm,tm);
	
	switch (whence) {
	    case NativeConstants.SEEK_SET:
		((SeekableIODescriptor)io).seekSet(offset,bc);
		break;
	    case NativeConstants.SEEK_CUR:
		((SeekableIODescriptor)io).seekCur(offset,bc);
		break;
	    case NativeConstants.SEEK_END:
		((SeekableIODescriptor)io).seekEnd(offset,bc);
		break;
	    default:
		setErrno(NativeConstants.EINVAL);
		return -1;
	}
	
	if (!check(bc)) {
	    return -1;
	}
	
	return ((SeekableIODescriptor.SeekFinalizer)bc.getFinalizer()).getAbsoluteOffset();
    }
    
    public long length(int fd) {
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
	
	if (!(io instanceof SeekableIODescriptor)) {
	    setErrno(NativeConstants.EINVAL);
	    return -1;
	}
	
	try {
	    return ((SeekableIODescriptor)io).getSize();
	} catch (IOException e) {
	    setErrno(e);
	    return -1;
	}
    }
   
    public int ftruncate(int fd, long size) {
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
	
	if (!(io instanceof FileDescriptor)) {
	    setErrno(NativeConstants.EINVAL);
	    return -1;
	}
	
	BlockingCallback bc=new BlockingCallback(bm,tm);
	
	((FileDescriptor)io).truncate(size,bc);
	
	if (check(bc)) {
	    return 0;
	}
	return -1;
    }

    /**
     * Lock a region in a file.
     */
    public int lock(int fd,
		    long start,
		    long size,
		    boolean shared,
		    boolean wait) {
	IODescriptor io = getIOD(fd);
	if (io == null)
	    return -1;
	if (!(io instanceof FileDescriptor)) {
	    setErrno(NativeConstants.EINVAL);
	    return -1;
	}
   	int ret 
	    = ((FileDescriptor)io).trylock(start, size, shared);
	if (ret == -1)
	    setErrno(Native.getErrno());
	if ( (ret != 1) || (! wait) )
	    return ret;
	//we're supposed to block until the lock is available, but it
	//is not.  Slow path: enter busy waiting / polling!
	BlockingCallback bc
	    = new BlockingCallback(bm,tm);
   	((FileDescriptor)io).lock(start, size, shared, bc);
	if (!check(bc)) {
            return -1;        
	}
	return 0;
    }

    /**
     * Lock a region in a file.
     */
    public int unlock(int fd,
		      long start,
		      long size) {
	IODescriptor io = getIOD(fd);
	if (io == null)
	    return -1;
	if (!(io instanceof FileDescriptor)) {
	    setErrno(NativeConstants.EINVAL);
	    return -1;
	}
	int ret = ((FileDescriptor)io).unlock(start, size);       
	if (ret == -1)
	    setErrno(Native.getErrno());
	return ret;
    }
    
    public int fsync(int fd) {
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
	
	if (!(io instanceof FileDescriptor)) {
	    setErrno(NativeConstants.EINVAL);
	    return -1;
	}
	
	try {
	    ((FileDescriptor)io).bufferSyncNow();
	    return 0;
	} catch (IOException e) {
	    setErrno(e);
	    return -1;
	}
    }
    
    public int connect(int sock,Oop address) {
	IODescriptor io=getIOD(sock);
	if (io==null) return -1;
        
        if (!(io instanceof SocketIODescriptor)) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        }
        
        BlockingCallback bc=new BlockingCallback(bm,tm);
        
        ((SocketIODescriptor)io).connect(parseUserSocketAddress(address),bc);
        
        if (check(bc)) {
            return 0;
        }
        
        return -1;
    }
    
    public int accept(int sock,Oop address,boolean blocking) {
	IODescriptor io=getIOD(sock);
	if (io==null) return -1;
        
        if (!(io instanceof SocketIODescriptor)) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        }

	SocketAddress sa;
	IODescriptor newIo;

	if (blocking) {
	    BlockingCallback bc=new BlockingCallback(bm,tm);
	    ((SocketIODescriptor)io).accept(bc);
	    
	    if (!check(bc)) {
		return -1;
	    }
	    
	    SocketIODescriptor.AcceptFinalizer af=
		(SocketIODescriptor.AcceptFinalizer)bc.getFinalizer();
	    
	    newIo=af.getNewDescriptor();
	    sa=af.getRemoteSocketAddress();
	} else {
	    IODescriptor[] iodCell=new IODescriptor[1];
	    SocketAddress[] saCell=new SocketAddress[1];
	    try {
		if (((SocketIODescriptor)io).tryAcceptNow(iodCell,saCell)) {
		    newIo=iodCell[0];
		    sa=saCell[0];
		} else {
		    setErrno(NativeConstants.EWOULDBLOCK);
		    return -1;
		}
	    } catch (IOException e) {
		setErrno(e);
		return -1;
	    }
	}
        
        if (!populateUserSocketAddress(address,sa)) {
            newIo.close();
            return -1;
        }
        
        return allocateDescriptor(newIo);
    }
    
    public int getsockname(int sock,Oop address) {
        try {
            return populateUserSocketAddress(address,
                ((SocketIODescriptor)getIOD(sock)).getSockName())?0:-1;
        } catch (IOException e) {
            setErrno(e);
            return -1;
        } catch (ClassCastException e) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        }
    }

    public int getpeername(int sock,Oop address) {
        try {
            return populateUserSocketAddress(address,
                ((SocketIODescriptor)getIOD(sock)).getPeerName())?0:-1;
        } catch (IOException e) {
            setErrno(e);
            return -1;
        } catch (ClassCastException e) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        }
    }

    public int open(Oop name, int flags, int mode) {
        //Native.print_string("open(name,"+flags+","+mode+")\n");
        
        BlockingCallback bc=new BlockingCallback(bm,tm);
        aiosf.getFileManager().open(dom.getLocalizedCString(name),
                                    flags,
                                    mode,
                                    bc);
        if (!check(bc)) {
            return -1;
        }
        
        return allocateDescriptor(
            ((AsyncBuildFinalizer)bc.getFinalizer()).getNewDescriptor());
    }
    
    public int mkstemp(Oop templateRef) {
        BlockingCallback bc=new BlockingCallback(bm,tm);

	//Blueprint.Array abp = templateRef.getBlueprint().asArray();
	//Oop tmpl = abp.addressOfElement(templateRef,0).getAddress().asOop();
	
	Oop tmpl = MemoryManager.the().getReferenceArrayElement( templateRef, 0 );

	byte[] byteTemplate=dom.getLocalizedCString(tmpl);
        aiosf.getFileManager().mkstemp(byteTemplate, bc);
        if (!check(bc)) {
            return -1;
        }

	// FIXME: Do we create strings with default encoding, or
	// what?  It should be safe to use UTF8 both ways here
	// Do we really need the actual name returned?  -- jason
	// YES!  We really DO need the actual name returned.  If we
	// don't return the name then how the heck do we delete the
	// file when we're done with it?!?  -- filip
        
        int nul;
        for (nul=0;byteTemplate[nul]!=0;++nul) {}
	Oop name = dom.makeString(UnicodeBuffer.factory().wrap(byteTemplate,0,nul));
	
	//abp.addressOfElement(templateRef,0).setAddress
	//    (VM_Address.fromObject(name));
	MemoryManager.the().setReferenceArrayElement( templateRef, 0, name );

        return allocateDescriptor(
            ((AsyncBuildFinalizer)bc.getFinalizer()).getNewDescriptor());
    }
    
    public int getAvailable(int fd) {
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
        
        if (!(io instanceof RWIODescriptor)) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        }
        
        try {
            return ((RWIODescriptor)io).getAvailable();
        } catch (IOException e) {
            setErrno(e);
            return -1;
        }
    }
    
    public int read(int fd,
                    Oop buf,
                    int byteOffset,
                    int byteCount,
                    boolean block) {
        if (byteCount==0) {
            return 0;
        }
        if (!verifyPointer(buf,byteOffset,byteCount)) {
            setErrno(NativeConstants.EFAULT);
            return -1;
        }
        
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
        
        if (!(io instanceof RWIODescriptor)) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        }
        
        // try read fast path
        try {
            if (DEBUG_READ) {
              Native.print_string("In read, trying the fast path...\n");
            }
            
            int result=readFastPath((RWIODescriptor)io,
                                    buf,byteOffset,byteCount);
            if (result>=0) {
                if (DEBUG_READ) {
                  Native.print_string("In read, fast path worked...\n");

		  Native.print_string("Buffer content after sucessful operation:\n");
		  for(int off=0;off<byteCount;off++) {
  		    Native.print_char(MemoryManager.the().addressOfElement(VM_Address.fromObject(buf), off+byteOffset, 1).getByte() );
                  }
                  Native.print_string("\n----------------end of content dump----------------\n");
		}
                
                return result;
            }
        } catch (IOException e) {
            if (DEBUG_READ) {
              Native.print_string("In read, fast path revealed an error...\n");        
            }
            setErrno(e);
            return -1;
        }
        
        if (!block) {
            setErrno(NativeConstants.EWOULDBLOCK);
            return -1;
        }
        
        Object r1=MemoryPolicy.the().enterScratchPadArea();
        try {
            if (DEBUG_READ) {
              Native.print_string("In read, setting up callback...\n");        
            }
            BlockingCallback bc=new BlockingCallback(bm,tm);
            ((RWIODescriptor)io)
                .read(new ForReadMemoryCallback(buf,byteOffset,byteCount),
                      byteCount,
                      bc);
            
            if (!check(bc)) {
                return -1;
            }
            
            int res = ((RWIODescriptor.ReadFinalizer)bc.getFinalizer()).getNumBytes();
            
            if (DEBUG_READ) {
              Native.print_string("In read, returning after callback, number of bytes read is ");        
              Native.print_int(res);
              Native.print_string("\n");
            }
            return res;
        } finally {
            MemoryPolicy.the().leave(r1);
        }
    }
    
    static class SkipBucket implements AsyncMemoryCallback {
	private int length;
	private VM_Address ptr;
	private byte[] data;
	
	static SkipBucket instance_=null;
	static SkipBucket instance() {
	    if (instance_==null) {
		MemoryManager mm=MemoryManager.the();
		VM_Area old=mm.setCurrentArea(mm.getImmortalArea());
		try {
		    instance_=new SkipBucket(4096);
		} finally {
		    mm.setCurrentArea(old);
		}
	    }
	    return instance_;
	}
	
	private SkipBucket(int length) {
	    this.length=length;
	    byte[] data = null;
	    
	    if (MemoryManager.the().usesArraylets()) {
	      data = MemoryManager.the().allocateContinuousByteArray(length);
	    } else {
	      data = new byte[length];
            }
            
            this.data = data;
            //ptr = getPointer(VM_Address.fromObject(data).asOop(),0,length);
	}
	
	int length() {
	    return length;
	}
	
	public VM_Address getBufferInternal(int count,
				    boolean needPinned) {  // pinning ?
	    if (count>length) {
		ovm.core.Executive.panic("count>length in SkipBucket.getBuffer()");
	    }
	    
	    ptr = getPointer(VM_Address.fromObject(data).asOop(),0,length);
	    
            if (true) {
	      MemoryManager.the().checkAccess( ptr );
	      MemoryManager.the().checkAccess( ptr.add(count-1) );
	    }
	    
	    return ptr;
	}

	public VM_Address getBuffer(int count,
				    boolean needPinned) {
            pinBuffer();
            return getBufferInternal( count, needPinned );				    
        }


/*
	VM_Address justGiveMeTheBuffer() {
	    return ptr;
	}
*/	
	
	public void doneBuffer(VM_Address buf,
			       int count) {
          handleReplication( VM_Address.fromObject(data).asOop(), 0, count, buf ); //FIXME: refactor this that the data to be thrown away don't have to be replicated
          unpinBuffer();			       
	}
	
	public void pinBuffer() {
	  MemoryManager.the().pinNewLocation(data);
        }
        
	public void unpinBuffer() {
	  MemoryManager.the().unpin(data);
	}
    
        public VM_Address getContiguousBuffer( int length, int offset ) {
        
            if (length>this.length) {
		ovm.core.Executive.panic("requested lenght > buffer length in SkipBucket.getContiguousBuffer()");
	    }
            
            //return ptr.add(offset);
            return getBufferInternal( length, false ).add(offset);
        }
    
        public int getLastContiguousBufferLength() {
          return this.length;
        }
    }
    
    public long skip(int fd,
                     long offset,
                     boolean blocking) {
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
	
	if (io instanceof SeekableIODescriptor) {
	    BlockingCallback bc=new BlockingCallback(bm,tm);
	    ((SeekableIODescriptor)io).seekCur(offset,bc);
	    if (!check(bc)) {
		return -1;
	    }
	    return ((SeekableIODescriptor.SeekFinalizer)bc.getFinalizer()).getRelativeOffset();
	} else if (io instanceof RWIODescriptor) {
	    SkipBucket sb=SkipBucket.instance();
	    
	    int toRead = offset>sb.length()?sb.length():(int)offset;
	    
	    if (blocking) {
		BlockingCallback bc=new BlockingCallback(bm,tm);
		((RWIODescriptor)io).read(sb,toRead,bc);
		if (!check(bc)) {
		    return -1;
		}
		
		return ((RWIODescriptor.ReadFinalizer)bc.getFinalizer()).getNumBytes();
	    } else {
		int result=0;
		VM_Address buf = sb.getBuffer(toRead, false);
		try {
		    result=((RWIODescriptor)io).tryReadNow(buf,
							   toRead);
		} catch (IOException e) {
		    setErrno(e);
		    return -1;
		} finally {
		  sb.doneBuffer( buf, result<0 ? 0 : result );
		}
		
		if (result<0) {
		    setErrno(NativeConstants.EWOULDBLOCK);
		    return -1;
		}
		return result;
	    }
	} else {
            setErrno(NativeConstants.EINVAL);
            return -1;
	}
    }
    
    private int writeFastPath(RWIODescriptor io,
                              Oop buf,
                              int byteOffset,
                              int byteCount) throws PragmaAtomic,
                                                    IOException {

        VM_Address addr = getPointer(buf, byteOffset, byteCount);
        
        if (MemoryManager.the().usesArraylets()) {
          if (addr.isNull()) {
            // the requested buffer is not contiguous
            return -1;
          }
        }
        return io.tryWriteNow( addr, byteCount);
    }
    
    private int receiveFastPath(SocketIODescriptor io,
                             Oop buf,
                             int byteOffset,
                             int byteCount,
                             SocketAddress[] isaCell) throws PragmaAtomic,
                                                   IOException {
                                                   
        VM_Address addr = getPointer(buf,byteOffset,byteCount);
        
        if (MemoryManager.the().usesArraylets()) {
          if (addr.isNull()) {
            // the requested buffer is non-contiguous
            return -1;
          }
        }
        
        int res = io.tryReceiveNow( addr, byteCount, isaCell );
        
        if (res>0) {
          handleReplication(buf, byteOffset, res, addr);
        }
        return res;
    }
    
    
    private int readFastPath(RWIODescriptor io,
                             Oop buf,
                             int byteOffset,
                             int byteCount) throws PragmaAtomic,
                                                   IOException {
        VM_Address addr = getPointer(buf,byteOffset,byteCount);
        
        if (MemoryManager.the().usesArraylets()) {
          if (addr.isNull()) {
            // the requested buffer is non-contiguous
            return -1;
          }
        }
        
        int res = io.tryReadNow( addr, byteCount);
        
        if (res>0) {
          handleReplication(buf, byteOffset, res, addr);
        }
        return res;
    }
    
    public int write(int fd,
                     Oop buf,
                     int byteOffset,
                     int byteCount,
                     boolean block) {
	//Native.print_string("write("+fd+")\n");

        if (byteCount==0) {
            return 0;
        }
        if (!verifyPointer(buf,byteOffset,byteCount)) {
            setErrno(NativeConstants.EFAULT);
            return -1;
        }
    
        IODescriptor io=getIOD(fd);
	if (io==null) return -1;
		long time3 = Native.getCurrentTime();
        if (!(io instanceof RWIODescriptor)) {
            setErrno(NativeConstants.EINVAL);
            return -1;
        }
        
        // try non-blocking fast path
        try {
            int result=writeFastPath((RWIODescriptor)io,
                                     buf,byteOffset,byteCount);
            if (result>=0) {
                // fast path succeeded!
                return result;
            }
        } catch (IOException e) {
            // error on fast path
            setErrno(e);
            return -1; 
        }
        
        if (!block) {
            setErrno(NativeConstants.EWOULDBLOCK);
            return -1;
        }
        
        // go for slow path
        Object r1=MemoryPolicy.the().enterScratchPadArea();
        try {
            BlockingCallback bc=new BlockingCallback(bm,tm);
            ((RWIODescriptor)io)
                .write(new ForWriteMemoryCallback(buf,byteOffset,byteCount),
                       byteCount,
                       bc);
            if (!check(bc)) {
                return -1;
            }
            
            int result=((RWIODescriptor.WriteFinalizer)bc.getFinalizer()).getNumBytes();
            
            //Native.print_string("write(): "+result+"/"+byteCount+"\n");
            return result;
        } finally {
            MemoryPolicy.the().leave(r1);
        }
    }
    
    public int cancel(int fd) {
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
        
        io.cancel(IOException.Canceled.getInstance());
        
        return 0;
    }
    
    public int close(int fd) throws PragmaAtomic {
        //Native.print_string("close("+fd+")\n");
        
	IODescriptor io=getIOD(fd);
	if (io==null) return -1;
        
        if (io==null) {
            setErrno(EINVAL);
            return -1;
        }

//         BasicIO.out.println("Closing FD " + fd + " attached to IOD " + io);
        io.close();
        freeDescriptor(fd);
        return 0;
    }

    public static class Factory implements PosixIO.Factory {
	public PosixIO make(Domain dom) {
	    UserLevelThreadManager tm = (UserLevelThreadManager)
		((ThreadServicesFactory)ThreadServiceConfigurator.config.
		 getServiceFactory(ThreadServicesFactory.name)).getThreadManager();
	    if (tm == null) {
		throw new OVMError.Configuration("need a configured thread manager");
	    }
            
            BlockingIOServicesFactory biosf =
                (BlockingIOServicesFactory) ThreadServiceConfigurator.config
                .getServiceFactory(BlockingIOServicesFactory.name);
            if (biosf == null) {
                throw new OVMError.Configuration("need blocking io services factory");
            }
            if (biosf.getBlockingManager() == null) {
		throw new OVMError.Configuration("need a configured blocking manager");
            }
            
            AsyncIOServicesFactory aiosf =
                (AsyncIOServicesFactory) IOServiceConfigurator.config
                .getServiceFactory(AsyncIOServicesFactory.name);
            if (aiosf == null) {
                throw new OVMError.Configuration("need async io services factory");
            }
            if (aiosf.getStdIOManager() == null) {
                throw new OVMError.Configuration("need std io manager");
            }
            if (aiosf.getPipeManager() == null) {
                throw new OVMError.Configuration("need pipe manager");
            }
            if (aiosf.getFileManager() == null) {
                throw new OVMError.Configuration("need file manager");
            }
            if (aiosf.getSocketManager() == null) {
                throw new OVMError.Configuration("need socket manager");
            }
            if (aiosf.getHostLookupManager() == null) {
                throw new OVMError.Configuration("need host lookup manager");
            }

	    return new PosixIOImpl(dom, tm,
                biosf.getBlockingManager(),aiosf);
	}
    }

    private static Blueprint.Array ebp;
    static void boot_() {
	ebp = (Blueprint.Array)
	    VM_Address.fromObject(new byte[0]).asOop().getBlueprint();
    }

    private class ForWriteMemoryCallback extends RWMemoryCallbackBase
        implements AsyncMemoryCallback  {

        public ForWriteMemoryCallback(Oop byteArray,
                                     int offset,
                                     int count) {
          super( byteArray, offset, count );
        }                
    }

    private class ForReadMemoryCallback extends RWMemoryCallbackBase
        implements AsyncMemoryCallback  {
        
        public ForReadMemoryCallback(Oop byteArray,
                                     int offset,
                                     int count) {
          super( byteArray, offset, count );
        }
        
        public void doneBuffer(VM_Address buf,
                               int count) {
          handleReplication(byteArray_, offset_, count, buf);                                
	  unpinBuffer();    
        }        
    }
    
    private class RWMemoryCallbackBase
        implements AsyncMemoryCallback {
        
        protected Oop byteArray_;
        protected int offset_;
        private int count_;

        public int lastBufferLength;

        public RWMemoryCallbackBase(Oop byteArray,
                                     int offset,
                                     int count) {
            this.byteArray_=byteArray;
            this.offset_=offset;
            this.count_=count;
            
            if (MemoryManager.the().usesArraylets()) {
              this.lastBufferLength = 0;
            }
        }

	/**
	 * FIXME: second param unneeded
	 **/
        public VM_Address getBuffer(int count,
                                    boolean mayNeedBufferForAnUnboundedPeriodOfTime) {
	    pinBuffer();  
	    return getPointer(byteArray_, offset_, count_);
	}

	
	// get largest possible contiguous buffer, no bigger than count, skipping 
	// initial skip bytes of the buffer data (after offset)
	// real length of the buffer is stored in bufferLength
	
	public void pinBuffer() {
	  MemoryManager.the().pinNewLocation(byteArray_);
	}
	
	public void unpinBuffer() {
	  MemoryManager.the().unpin(byteArray_);  
	}
	
	public VM_Address getContiguousBuffer(int count, int skip) {
          
          int arrayletSize = MemoryManager.the().arrayletSize();
          
          int offset = offset_ + skip;
          int iArraylet = offset / arrayletSize;
          int innerOffset = offset % arrayletSize;
          
          lastBufferLength = arrayletSize-innerOffset;
          if (lastBufferLength > count) {
            lastBufferLength = count;
          }
          
          VM_Address ret = VM_Address.fromObjectNB(byteArray_).add( ObjectModel.getObjectModel().headerSkipBytes() + 
            MachineSizes.BYTES_IN_WORD + iArraylet*MachineSizes.BYTES_IN_ADDRESS ).getAddress().add( innerOffset );
          
          if (true) {
            MemoryManager.the().checkAccess( ret );
            MemoryManager.the().checkAccess( ret.add(lastBufferLength-1) );            
          }
          return ret;
	}
	
	public int getLastContiguousBufferLength() {
	  return lastBufferLength;
	}
	
        public void doneBuffer(VM_Address buf,
                               int count) {
	  unpinBuffer();    
        }
    }

}
