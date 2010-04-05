// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/RWIODescriptor.java,v 1.7 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

import ovm.core.services.memory.*;

/**
 * An <code>IODescriptor</code> that supports the <code>read()</code> and
 * <code>write()</code> async syscalls.  In the future, this interface may
 * also support <code>readv()</code> and <code>writev()</code>.
 * <p>
 * An object implementing this interface does not necessarily support either
 * reading or writing, since reading and writing are abilities that can
 * be added or go away depending on state (example: a socket may be neither
 * readable nor writable before being connected).
 *
 * @author Filip Pizlo
 */
public interface RWIODescriptor extends IODescriptor {
    
    /**
     * Determine how much data can be read from the descriptor.  You may want to
     * watch out for IOException.Unsupported on this one, which would indicate
     * that this particular descriptor type cannot accurately answer this question.
     * @return how many bytes of data can be read
     */
    public int getAvailable() throws IOException;
    
    /**
     * Initiate an asynchronous read.  To learn about how asynchronous
     * operations work, look at <code>AsyncCallback</code> and
     * <code>AsyncFinalizer</code>.  To learn
     * about why you need to pass in an <code>AsyncMemoryCallback</code>
     * rather than just a <code>VM_Address</code> or <code>byte[]</code>,
     * look at that class's documentation.
     * <p>
     * @param data an <code>AsyncMemoryCallback</code> that will return,
     *             when asked to do so, a pointer to a buffer of size
     *             <code>maxBytes</code>, into which the data will be read.
     * @param maxBytes the maximum number of bytes to read.  This operation
     *                 will only complete once at least one byte is read.
     *                 As such, <code>maxByte</code> being 0 will have
     *                 unknown consequences.
     * @param cback the callback that is called when this operation is ready
     *              to complete.  The <code>AsyncFinalizer</code> passed to
     *              <code>AsyncCallback.read()</code> is guaranteed to be
     *              an instance of <code>RWIODescriptor.ReadFinalizer</code>.
     */
    public AsyncHandle read(AsyncMemoryCallback data,
			    int maxBytes,
			    AsyncCallback cback);
    
    /**
     * Attempt a non-blocking read right now.  Can only be called from an
     * atomic region.  This method will never block, stall, or schedule
     * anything to happen after it returns.  This method does not allocate
     * any objects (even the exceptions it throws are singletons).
     * <p>
     * As a user of RWIODescriptors, call this method before calling the
     * read() method to get a faster fast path.  As an implementor of
     * RWIODescriptors, try to make this method as fast as possible.  Note
     * that simply returning -1 every time that this method is called is
     * incorrect.  A valid implementation must ensure that a program that
     * calls tryReadNow() after data becomes available on the descriptor
     * without any other mechanism (such as an asynchronous read()) claiming
     * that data will get completion.
     *
     * @return non-negative integer if the read completed, -1 if the read
     *         did not complete or was not attempted.  note that -1 does
     *         not indicate an error.  errors are indicated with exceptions.
     */
    public int tryReadNow(VM_Address address,
			  int maxBytes) throws IOException;
    
    // if we wish to support readv(), for example to efficiently support
    // the java.nio.ScatteringByteChannel interface, then we should place it
    // into this interface.
    
    public static interface ReadFinalizer extends AsyncFinalizer {
        /**
         * @return the number of bytes read.
         */
        public int getNumBytes();
    }
    
    public AsyncHandle write(AsyncMemoryCallback data,
			     int maxBytes,
			     AsyncCallback cback);
    
    /**
     * Attempt a non-blocking write right now.  Can only be called from an
     * atomic region.  This method will never block, stall, or schedule
     * anything to happen after it returns.  This method does not allocate
     * any objects (even the exceptions it throws are singletons).
     * <p>
     * As a user of RWIODescriptors, call this method before calling the
     * write() method to get a faster fast path.  As an implementor of
     * RWIODescriptors, try to make this method as fast as possible.  Note
     * that simply returning -1 every time that this method is called is
     * incorrect.  A valid implementation must ensure that a program that
     * calls tryWriteNow() after the descriptor becomes ready for write
     * without any other mechanism (such as an asynchronous write()) claiming
     * that readyness state will get completion.
     *
     * @return non-negative integer if the write completed, -1 if the write
     *         did not complete or was not attempted.  note that -1 does
     *         not indicate an error.  errors are indicated with exceptions.
     */
    public int tryWriteNow(VM_Address address,
			   int maxBytes) throws IOException;
    
    // if we wish to support writev(), for example to efficiently support
    // the java.nio.GatheringByteChannel interface, then we should place it
    // into this interface.
    
    public static interface WriteFinalizer extends AsyncFinalizer {
        /**
         * @return the number of bytes written.
         */
        public int getNumBytes();
    }
    
}

