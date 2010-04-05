package java.net;


final class LibraryImports {

    static native String getLocalHostname();
    
    /**
     * Note that OVM does NOT do the endianess conversion that
     * classpath does on the IP.  The reason is that we want to
     * support non-32 bit IPs using this interface!
     */
    static native String getHostByAddr (byte[] ip,
					int af);

    static native byte[][] getHostByName (String hostname);

    //static native Vector getRealNetworkInterfaces ();

}
