// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/IPv4Address.java,v 1.1 2004/04/08 18:39:58 pizlofj Exp $

package ovm.services.io.async;

import ovm.core.execution.*;

/**
 *
 * @author Filip Pizlo
 */
public class IPv4Address implements InetAddress {
    
    private int ipAddress_;
    
    public IPv4Address(int ipAddress) {
        this.ipAddress_ = ipAddress;
    }
    
    public IPv4Address(byte[] ipAddress) {
        this(ipAddress,0);
    }
    
    // helper constructor used pretty much exclusively by HostLookupUtil.
    public IPv4Address(byte[] ipAddress,int offset) {
        this.ipAddress_ = ((ipAddress[offset+0]&0xff) << 24)
                        | ((ipAddress[offset+1]&0xff) << 16)
                        | ((ipAddress[offset+2]&0xff) << 8)
                        | (ipAddress[offset+3]&0xff);
    }
    
    public IPv4Address(IPv4Address ipAddress) {
        this(ipAddress.getIPv4Address());
    }
    
    /**
     * @return the IPv4 address in host ordering.  it is really a 32-bit
     *         unsigned integer.
     */
    public int getIPv4Address() {
        return ipAddress_;
    }
    
    /**
     * @return the IPv4 address in network ordering.
     */
    public byte[] getIPAddress() {
        return new byte[]{(byte)((ipAddress_>>24)&0xff),
                          (byte)((ipAddress_>>16)&0xff),
                          (byte)((ipAddress_>>8)&0xff),
                          (byte)(ipAddress_&0xff)};
    }
    
    public int getAddressFamily() {
        return NativeConstants.AF_INET;
    }
    
    public String toString() {
        return ""+((ipAddress_>>24)&0xff)+
              "."+((ipAddress_>>16)&0xff)+
              "."+((ipAddress_>>8)&0xff)+
              "."+(ipAddress_&0xff);
    }
}

