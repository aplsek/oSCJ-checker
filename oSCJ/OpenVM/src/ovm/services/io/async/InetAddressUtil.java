// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/InetAddressUtil.java,v 1.1 2004/04/08 18:39:58 pizlofj Exp $

package ovm.services.io.async;

/**
 *
 * @author Filip Pizlo
 */
public class InetAddressUtil {
    public static byte[][] toByteArrays(InetAddress[] addresses) {
        byte[][] result=new byte[addresses.length][];
        for (int i=0;i<addresses.length;++i) {
            result[i]=addresses[i].getIPAddress();
        }
        return result;
    }
}

