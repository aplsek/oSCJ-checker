package java.net;

import java.util.Vector;
import org.ovmj.java.NativeConstants;
//import org.xbill.DNS.Address;

/**
 * @author Christian Grothoff
 */
public final class LibraryGlue 
    implements NativeConstants {
    
    /**
     * Use XBILL.DNS library for DNS lookups?
     * The real question is whether we allow XBILL into the image at
     * all.  If you want to set this to true, you need to uncomment
     * the code that checks it, and add -xbill-targets to -udtargets
     * in config/engine/j2c.
     */
    private static final boolean XBILL = false;

     public static void setAddress(SocketImpl sock,
 				  int port,
 				  byte[] addr) {
 	sock.port = port;
 	sock.address = new InetAddress(addr, null);
     }

    /**
     * This native method looks up the hostname of the local machine
     * we are on.  If the actual hostname cannot be determined, then the
     * value "localhost" will be used.  This native method wrappers the
     * "gethostname" function.
     *
     * @return The local hostname.
     */
    static String getLocalHostname() {
	String hn = LibraryImports.getLocalHostname();
	if (hn == null) {
            //System.err.println("LI.getLocalHostname() returned null");
	    return "localhost";
	} else {
	    return hn;
        }
    }

    /**
     * Returns the value of the special address INADDR_ANY
     */
    static byte[] lookupInaddrAny() throws UnknownHostException {
	return new byte[4]; // classpath goes crazy here, but effectively does the same thing,
	// so let's keep it simple until someone really needs something else here.
    }
 
    /**
     * This method returns the hostname for a given IP address.  It will
     * throw an UnknownHostException if the hostname cannot be determined.
     *
     * @param ip The IP address as a int array
     * 
     * @return The hostname
     *
     * @exception UnknownHostException If the reverse lookup fails
     */
    static String getHostByAddr(byte[] ip)
	throws UnknownHostException {
// 	if (XBILL) {
// 	    InetAddress ia 
// 		= new InetAddress(ip);
// 	    return Address.getHostName(ia);
// 	} else {
	    String hn = LibraryImports.getHostByAddr(ip, AF_INET);
	    if (hn == null)
		throw new UnknownHostException();   // stupid stupid stupid
	    else
		return hn;
//	}
    }

    /**
     * Returns a list of all IP addresses for a given hostname.  Will throw
     * an UnknownHostException if the hostname cannot be resolved.
     */
    static byte[][] getHostByName(String hostname)
	throws UnknownHostException {
// 	if (XBILL) {
// 	    InetAddress[] iret
// 		= Address.getAllByName(hostname);
// 	    byte[][] ret = new byte[iret.length][];
// 	    for (int i=ret.length-1;i>=0;i--)
// 		ret[i] = iret[i].getAddress();
// 	    return ret;
// 	} else {
	    byte[][] ret = LibraryImports.getHostByName(hostname);
	    if ( (ret == null) ||
		 (ret.length == 0) )
		throw new UnknownHostException();   // stupid stupid stupid
	    return ret;
// 	}
    }


   /* *********** NetworkInterface ********** */

    /**
     * Returns Vector<NetworkInterface>!  Little problem (one among
     * many: NetworkInterface constructor is private!)
     */
    static Vector getRealNetworkInterfaces ()
	throws SocketException {
	return new Vector(0); // FIXME
    }
    

}
