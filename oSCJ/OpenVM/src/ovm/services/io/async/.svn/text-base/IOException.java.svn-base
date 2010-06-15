// $Header: /p/sss/cvs/OpenVM/src/ovm/services/io/async/IOException.java,v 1.11 2004/10/10 03:01:16 pizlofj Exp $

package ovm.services.io.async;

import ovm.core.execution.Native;
import ovm.core.execution.NativeConstants;
import ovm.core.services.memory.*;
import ovm.core.services.memory.MemoryManager;

/**
 * Describes an IO-related error.  Note that even though this
 * an exception, it is not always used as one.
 * Instead, this is just a structure object
 * that is passed around within the IO infrastructure whenever
 * an IO-related error happens.  The OVM kernel itself should
 * interpret what to do about these errors based on subclasses
 * of IOException (in other words, by doing instanceof tests).  Otherwise,
 * the system's <code>errno</code> can always be retrieved and
 * tests can be performed on it.
 * @author Filip Pizlo
 */
public abstract class IOException extends Exception {
    
    private static final String CANCELED_CODE = "C";
    private static final String UNSUPPORTED_CODE = "U";
    private static final String UNIMPLEMENTED_CODE = "N";
    private static final String INTERNAL_CODE = "I";
    private static final String SYSTEM_CODE = "S";
    private static final String HOST_LOOKUP_CODE = "H";
    
    public IOException(String message) {
        super(message);
    }

    protected boolean shouldFillInStackTrace() {
	return false;
    }
    
    public static IOException fromCode(String code) {
        if (code.equals(CANCELED_CODE)) {
            return Canceled.getInstance();
        } else if (code.equals(UNSUPPORTED_CODE)) {
            return Unsupported.getInstance();
        } else if (code.equals(UNIMPLEMENTED_CODE)) {
            return Unimplemented.getInstance();
        } else if (code.equals(INTERNAL_CODE)) {
            return Internal.getInstance("building in IOException.fromCode()");
        } else if (code.startsWith(SYSTEM_CODE)) {
            return System.make(Integer.parseInt(code.substring(1)));
        } else if (code.startsWith(HOST_LOOKUP_CODE)) {
            return HostLookup.make(Integer.parseInt(code.substring(1)));
        } else {
            throw new ovm.util.OVMError("Unrecognized code");
        }
    }
    
    /** a nul-terminatable code that describes this error. */
    public abstract String getCode();
    
    public static class Canceled extends IOException {
        private Canceled() {
            super("Canceled");
        }
        
        public String getCode() {
            return CANCELED_CODE;
        }
        
        private static Canceled singleton;
        public static Canceled getInstance() {
            if (singleton==null) {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.
                VM_Area prev=U.ei();
                try { singleton=new Canceled(); } finally { U.l(prev); }
            }
            return singleton;
        }
    }
    
    public static class Unsupported extends IOException {
        private Unsupported() {
            super("Unsupported");
        }
        
        public String getCode() {
            return UNSUPPORTED_CODE;
        }
        
        private static Unsupported singleton;
        public static Unsupported getInstance() {
            if (singleton==null) {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.
                VM_Area prev=U.ei();
                try { singleton=new Unsupported(); } finally { U.l(prev); }
            }
            return singleton;
        }
    }
    
    public static class Unimplemented extends IOException {
        public Unimplemented() {
            super("Unimplemented");
        }
        
        public String getCode() {
            return UNIMPLEMENTED_CODE;
        }
        
        private static Unimplemented singleton;
        public static Unimplemented getInstance() {
            if (singleton==null) {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.
                VM_Area prev=U.ei();
                try { singleton=new Unimplemented(); } finally { U.l(prev); }
            }
            return singleton;
        }
    }
    
    public static class Internal extends IOException {
        public static final boolean DEBUG=false;
        
        public Internal() {
            super("Internal");
        }
        
        public String getCode() {
            return UNIMPLEMENTED_CODE;
        }
        
        private static Internal singleton;
        
        /**
         * Get the singleton instance of <code>IOException.Internal</code> and
         * print the given reason string.  If <code>IOException.Internal.DEBUG</code>
         * is <code>false</code>, the reason string is not printed.  The justification
         * for not placing the reason string into the exception object is that we
         * want to have a finite set of exception objects in the system.
         */
        public static Internal getInstance(String reason) {
            if (DEBUG) {
                Native.print_string("IOException.Internal.getInstance(");
                Native.print_string(reason);
                Native.print_string(")\n");
            }
            if (singleton==null) {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.
                VM_Area prev=U.ei();
                try { singleton=new Internal(); } finally { U.l(prev); }
            }
            return singleton;
        }
    }
    
    public static class System extends IOException {
        
        /**
         * The system's errno.
         */
        private int errno_;
        
        protected static String makeMessage(int errno) {
//            byte[] data=new byte[1024];
            byte[] data = MemoryManager.the().allocateContinuousByteArray(1024);
            int result=Native.get_specific_error_string(errno,data,data.length);
            return new String(data,0,result);
        }
        
        // use make() method instead of constructor
        private System(int errno) {
            super(makeMessage(errno));
            this.errno_=errno;
        }
        
        public String getCode() {
            return SYSTEM_CODE+errno_;
        }
        
        public int getErrno() {
            return errno_;
        }
        
        private static System[] systemErrors=null;
        
        public static System make(int errno) {
            VM_Area prev=U.ei();
            try {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.

                if (systemErrors==null) {
                    int size=1024;
                    if (errno>=size) {
                        size=errno+1;
                    }
                    systemErrors=new System[size];
                } else if (errno>=systemErrors.length) {
                    System[] newArray=new System[errno+1];
                    java.lang.System.arraycopy(systemErrors, 0,
                                               newArray, 0,
                                               systemErrors.length);
                    systemErrors=newArray;
                }
                
                if (systemErrors[errno]==null) {
                    systemErrors[errno]=new System(errno);
                }
                
                return systemErrors[errno];
            } finally {
                U.l(prev);
            }
        }
        
        public static int check(int result,
				int errno)
            throws IOException {
            if (result<0) {
                throw System.make(errno);
            }
	    return result;
        }
        
	/** use this instead of check(int,int) with stuff like lseek() that returns
	 * a long. */
        public static long check(long result,
                                 int errno)
            throws IOException {
            if (result<0) {
                throw System.make(errno);
            }
	    return result;
        }
        
        public static boolean check(int result,
                                    int errno,
                                    AsyncCallback cback) {
            if (result<0) {
                cback.ready(AsyncFinalizer.Error.make(System.make(errno)));
                return false;
            }
            return true;
        }
        
        public static boolean check(long result,
                                    int errno,
                                    AsyncCallback cback) {
            if (result<0) {
                cback.ready(AsyncFinalizer.Error.make(System.make(errno)));
                return false;
            }
            return true;
        }
        
        public static boolean checkRepeat(int result,
                                          int errno)
            throws IOException {
            if (result<0 && (errno==NativeConstants.EWOULDBLOCK ||
                             errno==NativeConstants.EAGAIN)) {
                return true;
            }
            check(result,errno);
            return false;
        }
    }
    
    public static class HostLookup extends IOException {
        
        /**
         * The system's errno.
         */
        private int h_errno_;
        
        protected static String makeMessage(int h_errno) {
//            byte[] data=new byte[1024];
            byte[] data = MemoryManager.the().allocateContinuousByteArray(1024);           
            int result=Native.get_specific_h_error_string(h_errno,data,data.length);
            return new String(data,0,result);
        }
        
        // use make() method instead of constructor
        private HostLookup(int h_errno) {
            super(makeMessage(h_errno));
            this.h_errno_=h_errno;
        }
        
        public String getCode() {
            return HOST_LOOKUP_CODE+h_errno_;
        }
        
        public int getHErrno() {
            return h_errno_;
        }
        
        private static HostLookup[] hostErrors=null;
        
        public static HostLookup make(int h_errno) {
            VM_Area prev=U.ei();
            try {
                // amount of allocation that happens in here is bounded by the
                // number of threads, which is fine by me.

                if (hostErrors==null) {
                    int size=10;
                    if (h_errno>=size) {
                        size=h_errno+1;
                    }
                    hostErrors=new HostLookup[size];
                } else if (h_errno>=hostErrors.length) {
                    HostLookup[] newArray=new HostLookup[h_errno+1];
                    java.lang.System.arraycopy(hostErrors, 0,
                                               newArray, 0,
                                               hostErrors.length);
                    hostErrors=newArray;
                }
                
                if (hostErrors[h_errno]==null) {
                    hostErrors[h_errno]=new HostLookup(h_errno);
                }
                
                return hostErrors[h_errno];
            } finally {
                U.l(prev);
            }
        }
        
        public static long check(long result,
                                 int errno,
				 int h_errno)
            throws IOException {
            if (result<0) {
                if (h_errno<0) {
                    throw System.make(errno);
                } else {
                    throw HostLookup.make(h_errno);
                }
            }
	    return result;
        }
        
        public static boolean checkRepeat(long result,
                                          int errno,
                                          int h_errno)
            throws IOException {
            if (result<0 && h_errno<0 && (errno==NativeConstants.EINTR)) {
                return true;
            }
            check(result,errno,h_errno);
            return false;
        }
        
    }
}

