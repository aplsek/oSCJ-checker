package ovm.services.io.async;

import ovm.core.services.memory.*;
import ovm.core.*;

/**
 * Object given to an async IO client so that the client can complete
 * the operation in thread context.  This interface will have sub-interfaces
 * for various IO operations.  For example, <code>RWIODescriptor</code> 
 * extends this interface to include a method for retrieving the number
 * of bytes actually read.
 * @author Filip Pizlo
 */
public interface AsyncFinalizer {
    
    /**
     * Call this to finish the operation.  You should not do anything
     * else with this object until this returns <code>true</code>.  Note
     * that this interface along with the <code>AsyncCallback</code> supports
     * the concept of <emph>finalizer chaining</emph>, where <code>finish()</code> returns
     * <code>false</code>, while <emph>at the same time<emph> calling
     * <code>AsyncCallback.ready()</code> with a <emph>new</emph> instance of
     * <code>AsyncFinalizer</code>.
     * <p>
     * Note furthermore that <code>finish()</code> should <emph>never</emph> be
     * called in an interrupt handler.
     * <p>
     * Oh, and one other thing: you may call <code>finish()</code> from any memory
     * area, as it is <code>finish()</code>'s responsibility to place itself into
     * whatever memory area it needs to be in (which is actually almost never the
     * receiver's memory area).
     * @return <code>true</code> if the operation actually finished.  If
     *         <code>false</code> is returned, this means that
     *         <code>AsyncCallback.ready()</code> will be called again
     *         at some point.
     */
    public boolean finish();
    
    /**
     * Returns the error that occurred.  Call this before casting
     * this object to anything other than <code>AsyncFinalizer</code>,
     * and don't do any such casting unless this method returns
     * <code>null</code>.
     * <p>
     * Do not call this until
     * after you've called <code>finish()</code> and it has returned
     * <code>true</code>.
     * @return null if no error occurred, or the error that occurred
     *         otherwise.
     */
    public IOException getError();
    
    // some basic re-usable AsyncFinalizers.  these are here instead of being
    // in the S3 namespace because there is only one way to implement them, and
    // they really should be reusable by all implementations.
    
    /** A finalizer that reports that the operation is not complete.  It is
     * effectively a NOP finalizer. */
    public static class NotFinished
        implements AsyncFinalizer {
        private NotFinished() {}
        public boolean finish() {
            return false;
        }
        public IOException getError() {
            throw Executive.panic(
                "AsyncFinalizer.NotFinished.getError() called, which should "+
                "not have happened since our finish() method always returns "+
                "false!");
        }
        
        private static NotFinished singleton;
        
        /** Call this method to get your allocated-in-immortal NotFinished
         * finalizer. */
        public static NotFinished getInstance() {
            if (singleton==null) {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.
                VM_Area prev=U.ei();
                try { singleton=new NotFinished(); } finally { U.l(prev); }
            }
            return singleton;
        }
    }
    
    /** A finalizer that indicates that there was an error. */
    public static class Error
        implements AsyncFinalizer {
        private IOException error_;
        public Error(IOException error) {
            this.error_=error;
        }
        public boolean finish() {
            return true;
        }
        public IOException getError() {
            return error_;
        }
        
        private static Error canceled,
                             unsupported,
                             unimplemented,
                             internal,
                             badIod;
        
        private static Error[] system,
                               hostLookup;
        
        /** Call this to make yourself an Error finalizer.  Note that the only
         * Error finalizers that are available are the ones for which we have
         * <code>IOException</code> subclass that is also an inner class of
         * <code>IOException</code>.  In other words, it is not possible to create
         * your own <code>IOException</code> subclass and pass it into this method.
         * You'll get a panic if you do that. */
        public static Error make(IOException error) {
            // note: we know that the area of the error parameter is immortal since
            // construction of IOExceptions only happens in immortal (we ensure this
            // by having the constructors be private).
            
            VM_Area prev=U.ei();
            try {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.

                if (error instanceof IOException.Canceled) {
                    if (canceled==null) {
                        canceled=new Error(IOException.Canceled.getInstance());
                    }
                    return canceled;
                } else if (error instanceof IOException.Unsupported) {
                    if (unsupported==null) {
                        unsupported=new Error(IOException.Unsupported.getInstance());
                    }
                    return unsupported;
                } else if (error instanceof IOException.Unimplemented) {
                    if (unimplemented==null) {
                        unimplemented=new Error(IOException.Unimplemented.getInstance());
                    }
                    return unimplemented;
                } else if (error instanceof IOException.Internal) {
                    if (internal==null) {
                        internal=new Error(error);
                    }
                    return internal;
                } else if (error instanceof IOException.System) {
                    int errno=((IOException.System)error).getErrno();
                    if (system==null) {
                        int size=1024;
                        if (errno>=size) {
                            size=errno+1;
                        }
                        system=new Error[size];
                    } else if (errno>=system.length) {
                        Error[] newArray=new Error[errno+1];
                        System.arraycopy(system, 0,
                                         newArray, 0,
                                         system.length);
                        system=newArray;
                    }
                    
                    if (system[errno]==null) {
                        system[errno]=new Error(error);
                    }
                    
                    return system[errno];
                } else if (error instanceof IOException.HostLookup) {
                    int errno=((IOException.HostLookup)error).getHErrno();
                    if (hostLookup==null) {
                        int size=10;
                        if (errno>=size) {
                            size=errno+1;
                        }
                        hostLookup=new Error[size];
                    } else if (errno>=hostLookup.length) {
                        Error[] newArray=new Error[errno+1];
                        System.arraycopy(hostLookup, 0,
                                         newArray, 0,
                                         hostLookup.length);
                        hostLookup=newArray;
                    }
                    
                    if (hostLookup[errno]==null) {
                        hostLookup[errno]=new Error(error);
                    }
                    
                    return hostLookup[errno];
                } else {
                    throw Executive.panic(
                        "Got an IOException that is not of a type that I know about");
                }
            } finally {
                U.l(prev);
            }
        }
    }
    
    /** A finalizer that represents success without having any return value. */
    public static class Success
        implements AsyncFinalizer {
        protected Success() {}
        public boolean finish() {
            return true;
        }
        public IOException getError() {
            return null;
        }
        
        private static Success singleton;
        
        /** Call this to get your allocated-in-immortal successful finalizer. */
        public static Success getInstance() {
            if (singleton==null) {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.
                VM_Area prev=U.ei();
                try { singleton=new Success(); } finally { U.l(prev); }
            }
            return singleton;
        }
    }
    
    public static class Delegate
        implements AsyncFinalizer {
        
        private AsyncFinalizer target_;
        
        public Delegate(AsyncFinalizer fin) {
            this.target_=fin;
        }
	
	public Delegate() {}
        
        public AsyncFinalizer getTarget() {
            return target_;
        }
	
	public void setTarget(AsyncFinalizer fin) {
	    this.target_=fin;
	}
        
        public boolean finish() {
            return getTarget().finish();
        }
        
        public IOException getError() {
            return getTarget().getError();
        }
    }
}

