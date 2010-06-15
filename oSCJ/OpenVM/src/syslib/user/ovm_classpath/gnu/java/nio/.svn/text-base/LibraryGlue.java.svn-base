package gnu.java.nio;

import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;
import org.ovmj.java.Opaque;

/**
 * Native methods for gnu.java.nio
 *
 * @author Christian Grothoff
 */
final class LibraryGlue {

    private static IOException die(int reason) {
	return new IOException(getErrorMessage(reason));
    }

    // native helpers
    static native int get_specific_error_string(int errno,
						byte[] buf,
						int len);

    /**
     * Poll value of errno and build the appropriate error message.
     */
    private static String getErrorMessage(int reason) {	
	byte[] buf = new byte[128];
	int len = get_specific_error_string(reason, buf, buf.length);
	return new String(buf, 0, len);
    }

   /* ************** methods from NIOServerSocket *************** */
    
/*    static PlainSocketImpl getPlainSocketImpl(NIOServerSocket _) {
	throw new Error("Not implemented");
    }*/
    private static int check(int ret) throws IOException {
	if (ret < 0) 
	    throw die(LibraryImports.getErrno());
	return ret;
    }

    private static long check(long ret) throws IOException {
	if (ret < 0) 
	    throw die(LibraryImports.getErrno());
	return ret;
    }

    /* ************** methods from PipeImpl *************** */

    static int readImpl (PipeImpl.SourceChannelImpl _,
			 int fd,
			 boolean blocking,
			 byte[] dst,
			 int off,
			 int len)
      throws IOException
    {
	return check(LibraryImports.read(fd, 
					 dst,
					 off, 
					 len,
					 blocking));
    }

    static int writeImpl(PipeImpl.SinkChannelImpl _,
			 int fd,
			 boolean blocking,
			 byte[] dst,
			 int off,
			 int len)
      throws IOException
    {
	return check(LibraryImports.write(fd, 
					  dst,
					  off, 
					  len,
					  blocking));
    }

    static void implClose(PipeImpl.SinkChannelImpl _,
			  int fd) throws IOException {
	check(LibraryImports.close(fd));
    }

    static void nativeInit (PipeImpl _,
			    SelectorProvider provider)
	throws IOException {
	int[] p = new int[2];
	int ok = LibraryImports.pipe(p);
	if (ok != 0) 
	    throw new IOException(getErrorMessage(LibraryImports.getErrno()));
	_.sink = new PipeImpl.SinkChannelImpl(provider,
					      p[0]);
	_.source = new PipeImpl.SourceChannelImpl(provider,
						  p[1]);	
    }

    /* ************** methods form SelectorImpl ************ */

    static void selectorImplInit(SelectorImpl _) {
	_.nativeCookie_ = LibraryImports.createSelectCookie();
    }

    static void selectorImplDone(SelectorImpl _) {
	LibraryImports.releaseSelectCookie((Opaque)_.nativeCookie_);
    }

    static void registerNative(SelectorImpl _,
			       SelectionKeyImpl key) {
	int fd = key.getNativeFD();
	int ovmops = 0;
	int cpops = key.interestOps();

	if ( (cpops & SelectionKey.OP_ACCEPT) > 0)
	    ovmops |= 1;
	if ( (cpops & SelectionKey.OP_READ) > 0)
	    ovmops |= 1;
	if ( (cpops & SelectionKey.OP_WRITE) > 0)
	    ovmops |= 2;
	if ( (cpops & SelectionKey.OP_CONNECT) > 0)
	    ovmops |= 2;
	
	LibraryImports.registerSelector((Opaque)_.nativeCookie_,
					fd,
					ovmops,
					key);
    }

    static void unregisterNative(SelectorImpl _,
				 SelectionKeyImpl key) {
	int fd = key.getNativeFD();
	LibraryImports.unregisterSelector((Opaque)_.nativeCookie_,
					  fd,
					  key);
    }

    /**
     * 
     * @param timeout A timeout value of 0 means block forever.
     */
    static int VMSelector_select (SelectorImpl _, long timeout)
	throws IOException
    {
	_.selected.clear();
	// Note: an optimized implementation should
	// cache rs instances in SelectorImpl and
	// avoid allocation of rs and rs.readySet.
	LibraryImports.ResultSet rs
	    = new LibraryImports.ResultSet();
	int sret
	    = LibraryImports.select((Opaque)_.nativeCookie_,
				    timeout,
				    rs);
	if (sret == -1) 
	    throw die(LibraryImports.getErrno());
	Set keys = _.keys();
	for (int i=rs.readyCount-1;i>=0;i--) {
	    // We must here update 'key.readyOps'!  
	    SelectionKeyImpl key
		= (SelectionKeyImpl) rs.readyCpCookieSet[i];
	    int ops = 0;
	    if ((rs.readyType[i] & 1) > 0) {
                if (key.channel() instanceof ServerSocketChannelImpl) {
		    ops = ops | SelectionKey.OP_ACCEPT;
		} else {
		    ops = ops | SelectionKey.OP_READ;
		}
	    }
	    if ((rs.readyType[i] & 2) > 0) {
		ops = ops | SelectionKey.OP_WRITE;
		// hmm, classpath had this commented out,
		// but without saying why.  Strange...
		//if (key.channel().isConnected()) {
		//    ops = ops | SelectionKey.OP_WRITE;
		//} else {
		//    ops = ops | SelectionKey.OP_CONNECT;
		//}
		
	    }
	    if ((rs.readyType[i] & 4) > 0) {
		// not supported by classpath yet...
	    }	       
	    key.readyOps(key.interestOps () & ops);	    
	    _.selected.add(key);
	}
	return sret;
    }

}

 
