// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/HostLookupUtil.java,v 1.2 2004/10/01 02:52:11 pizlofj Exp $

package ovm.services.io.async;

import ovm.core.execution.*;
import ovm.core.*;
import ovm.core.domain.Oop;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;

/**
 * Implements stalling hostname lookup.  This then gets used by all of
 * the current ED implementations of hostname lookup.
 * @author Filip Pizlo
 */
public class HostLookupUtil {
    public static class RawResult {
        public int addrLen;
        public int numEntries;
        public byte[] data;
        public RawResult(int addrLen,
                         int numEntries,
                         byte[] data) {
            this.addrLen=addrLen;
            this.numEntries=numEntries;
            this.data=data;
        }
    }
    
    public static RawResult getHostByNameRaw(byte[] str) throws IOException {
        long retVal;
        int addrlen;
        int ret;
        //byte[] retbuf = new byte[32];
        byte[] retbuf = MemoryManager.the().allocateContinuousByteArray(32);
        
        Oop bufOop = VM_Address.fromObject(retbuf).asOop();
        MemoryManager.the().pin(bufOop);
        
        try {
        while (true) {
            while (IOException.HostLookup.checkRepeat(
                       retVal = Native.get_host_by_name(str,
                                                        retbuf,
                                                        retbuf.length),
                       Native.getErrno(),
                       Native.getHErrno())) {
                // loop until we don't get an EINTR
            }
            
            addrlen = (int) (retVal >> 32);
            ret = (int) retVal;
            if (addrlen * ret > retbuf.length) { // number of entries to few!
                retbuf = MemoryManager.the().allocateContinuousByteArray(addrlen * ret);
                continue;
            }
            if (ret == 0) {
                throw Executive.panic("ret == 0 even though we didn't get an error");
            }
            break; // wow, no error :-)
        }
        } finally {
          MemoryManager.the().unpin(bufOop);
        }
        return new RawResult(addrlen,ret,retbuf);
    }
    
    public static InetAddress[] fromRawResult(RawResult res)
	throws IOException {
        if (res.addrLen!=4) {
            throw IOException.Unsupported.getInstance();
        }
        
        IPv4Address[] result=new IPv4Address[res.numEntries];
        
        for (int i=0;i<res.numEntries;++i) {
            result[i]=new IPv4Address(res.data,i*4);
        }
        
        return result;
    }
                                        
    public static InetAddress[] getHostByNameNow(byte[] name)
	throws IOException {
        return fromRawResult(getHostByNameRaw(name));
    }
    
    public static byte[] getHostByAddrNow(InetAddress addr)
	throws IOException {
//        byte[] hostname=new byte[1024];
        byte[] hostname = MemoryManager.the().allocateContinuousByteArray(1024);
        Oop hostOop = VM_Address.fromObject(hostname).asOop();
        MemoryManager.the().pin(hostOop);
        
        byte[] ip=addr.getIPAddress();

        try {
        while (IOException.HostLookup.checkRepeat(
		   Native.get_host_by_addr(ip,ip.length,
					   addr.getAddressFamily(),
					   hostname,hostname.length),
		   Native.getErrno(),
		   Native.getHErrno())) {
            /* loop until we don't get an EINTR */
        }
        } finally {
          MemoryManager.the().unpin(hostOop);
        }
        for (int i=0;i<hostname.length;++i) {
            if (hostname[i]==0) {
                byte[] result=new byte[i+1];
                System.arraycopy(hostname, 0,
                                 result, 0,
                                 i);
                result[i]=0;
                return result;
            }
        }
        throw IOException.Internal.getInstance(
            "nul terminator not found in hostname in "+
            "HostLookupUtil.getHostByAddrNow()");
    }
}

