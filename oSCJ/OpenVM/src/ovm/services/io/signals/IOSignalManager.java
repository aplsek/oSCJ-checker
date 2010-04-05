
package ovm.services.io.signals;

/**
 * Manages signaling you to tell you that IO is ready on an fd.
 * @author Filip Pizlo
 */
public interface IOSignalManager
    extends ovm.services.ServiceInstance {
    
    /**
     * Add a callback for a particular file descriptor for reading.  It should
     * always be possible to call this method, even if you are inside
     * Callback.signal().
     * <p>
     * You should never register the same Callback object more than once.
     */
    public void addCallbackForRead(int fd,Callback cback);
    
    /**
     * Add a callback for a particular file descriptor for writing.  It should
     * always be possible to call this method, even if you are inside
     * Callback.signal().
     * <p>
     * You should never register the same Callback object more than once.
     */
    public void addCallbackForWrite(int fd,Callback cback);
    
    /**
     * Add a callback for a particular file descriptor for exceptional conditions.  It should
     * always be possible to call this method, even if you are inside
     * Callback.signal().
     * <p>
     * You should never register the same Callback object more than once.
     */
    public void addCallbackForExcept(int fd,Callback cback);
    
    /**
     * Remove the callback from a particular FD.  Do not call this
     * from Callback.signal().
     */
    public void removeCallbackFromFD(int fd,
				     Callback cback,
				     Object byWhat);
    
    /**
     * Remove the callback from all FDs.  Do not call this
     * from Callback.signal().
     * <p>
     * You should use this instead of removeCallbackFromFD() if you do not
     * know what FD the Callback object corresponds to.  Note that this
     * method will almost certainly take a lot longer than removeCallbackFromFD().
     */
    public void removeCallback(Callback cback,
			       Object byWhat);
    
    /**
     * Remove all callbacks on a particular FD.  Do not call this
     * from Callback.signal().
     */
    public void removeFD(int fd,
			 Object byWhat);
    
    public static interface Callback {
	/** Special value of the byWhat parameter, for when signal()
	 * returns false. */
	public static final Object BY_SIGNAL = new Object();
	
        /**
         * Called when an edge trigger happens on <code>fd</code>, or in some
         * cases when it is believed that an edge trigger happened on said
         * file descriptor.  (So you may want to do your own check to see if
         * your desired state has been reached.)
         * @param certain is <code>true</code> if the <code>IOSignalManager</code>
         *                most certainly knows that that the file descriptor is
         *                ready for the operation in question.
         * @return <code>true</code> if you wish to be triggered for the same
         *         file descriptor in the future, <code>false</code> otherwise.
         */
        public boolean signal(boolean certain);
        
        /**
         * Called when the callback is removed.
         * @param byWhat tells you why you got removed.
         */
        public void removed(Object byWhat);
    }
    
}

