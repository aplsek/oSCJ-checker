// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/SeekableIODescriptor.java,v 1.5 2004/10/09 21:43:04 pizlofj Exp $

package ovm.services.io.async;

/**
 * Interface for seeking operations.  Operations whose effect is changing the current
 * offset into the file are asynchronous.  It is guaranteed that these operations only
 * affect the offset for read and write operations enqueued after them.
 * <p>
 * There are at least two possible implementations of the <code>seekSet()</code>,
 * <code>seekEnd()</code>, and <code>seekCur()</code>.
 * <ol>
 * <li>Enqueue seek operations along with read and write operations.
 * <li>Store a virtual file offset and have the seek operations change that offset.
 *     Read and write operations are enqueued as pread and pwrite (using an
 *     absolute offset) using the last available virtual file offset.  Once enqueued,
 *     these operations render the virtual file offset invalid, meaning that any
 *     operations that wish to use the virtual file offset must block until these
 *     operations complete.  If serializability is to be guaranteed, then this may
 *     cause all I/O operations to wait until the virtual file offset is in a valid
 *     state again (OUCH!).
 * </ol>
 * For those who have been paying attention, the first option is doable only when we
 * are in charge of the asynchrony (either via polling or a thread pool).  Linux AIO
 * as well as POSIX AIO only give us pread and pwrite operations, so the second option
 * will have to be used there.  (Actually, I'm not yet sure about Linux AIO.  The
 * documentation and papers that I have read so far give me no insight into this matter.
 * What is perhaps evidence that Linux AIO does in fact support relative read and write
 * operations is that Capriccio passes -1 as the offset in <code>io_submit()</code>
 * when servicing a relative read/write call.  Totally weird.)
 *
 * @author Filip Pizlo
 */
public interface SeekableIODescriptor extends IODescriptor {
    
    /** tells you the current offset within the file. */
    public long tell() throws IOException;
    
    /** tells you the largest possible offset in the file. */
    public long getSize() throws IOException;

    /** seeks to an absolute location. */
    public AsyncHandle seekSet(long offset,
			       AsyncCallback cback);
    
    /** seeks to an offset from the end of the file.
     * @return the new absolute location */
    public AsyncHandle seekEnd(long offset,
			       AsyncCallback cback);
    
    /** seeks to an offset from the current location.  doing <code>seekCur(0)</code>
     * is a good way of finding out the current offset.  it may not be as good as
     * doing <code>tell()</code>, since <code>tell()</code> doesn't block.
     * @return the new absolute location */
    public AsyncHandle seekCur(long offset,
			       AsyncCallback cback);
    
    public static interface SeekFinalizer extends AsyncFinalizer {
	/**
	 * @return the new absolute offset
	 */
	public long getAbsoluteOffset();
	
	/**
	 * @return the change in offset at the time of the operation
	 */
	public long getRelativeOffset();
    }
}

