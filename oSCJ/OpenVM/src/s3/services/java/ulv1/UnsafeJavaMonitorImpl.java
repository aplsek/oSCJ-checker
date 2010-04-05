// $Header: /p/sss/cvs/OpenVM/src/s3/services/java/ulv1/UnsafeJavaMonitorImpl.java,v 1.6 2005/08/30 00:19:01 dholmes Exp $

package s3.services.java.ulv1;

import ovm.services.java.JavaMonitor;
import ovm.services.java.UnsafeJavaMonitor;
import ovm.services.monitors.Monitor;
import s3.util.PragmaAtomic;
import s3.core.domain.S3Domain;
/**
 *
 * @author Filip Pizlo
 */
public class UnsafeJavaMonitorImpl
    extends JavaMonitorImpl
    implements UnsafeJavaMonitor {
    
    public void signalOneUnsafe() throws PragmaAtomic {
        signalOne();
    }
    
    public static class Factory extends JavaMonitorImpl.Factory
        implements UnsafeJavaMonitor.Factory {
        
        public Monitor newInstance() {
            return new UnsafeJavaMonitorImpl();
        }

        public JavaMonitor newJavaMonitorInstance() {
            return new UnsafeJavaMonitorImpl();
        }

        public UnsafeJavaMonitor newUnsafeJavaMonitorInstance() {
            return new UnsafeJavaMonitorImpl();
        }
        public int monitorSize() {
            return UnsafeJavaMonitorImpl.sizeOf();
        }
    }

 
    /**
     *  Returns the actual size of an instance of this class, including the
     *  space needed for the object header and all fields, plus the space
     *  needed for creating referenced objects (and transitively the space
     *  they need to create referenced objects) during construction.
     */
    static int sizeOf() {
        return 
            S3Domain.sizeOfInstance("s3/services/java/ulv1/UnsafeJavaMonitorImpl")
            + constructionSizeOf();
    }


    /**
     * Returns the maximum space allocated during the execution of the
     * constructor of an instance of this class, and transitively the space
     * needed by any object allocation performed in this constructor.
     * Note this doesn't include "temporary" allocations like debug strings
     * etc, but it does include super constructors. Hence for any class the
     * total space needed to do "new" is the base size plus the construction
     * size.
     */
    protected static int constructionSizeOf() {
        // there is no additional allocation in this class so just return
        // whatever our super class construction requirements are
        return JavaMonitorImpl.constructionSizeOf();
    }

    
    public static Factory factory = new Factory();
}

