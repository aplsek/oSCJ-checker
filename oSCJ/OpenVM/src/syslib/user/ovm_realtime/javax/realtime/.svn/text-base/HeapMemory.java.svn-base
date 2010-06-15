/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/HeapMemory.java,v 1.2 2005/06/09 02:29:57 dholmes Exp $
 */
package javax.realtime;

/**
 * The <code>HeapMemory</code> class is a singleton object that allows logic 
 * within other memory areas to allocate objects in the Java heap.
 *
 */
public class HeapMemory extends MemoryArea {

    /** the singleton instance representing the heap */
    protected static final HeapMemory instance = new HeapMemory();
    
    /** private, trivial constructor */
    private HeapMemory() {
	super(LibraryImports.getHeapArea());
    }

    /**
     * Returns a pointer to the singleton instance of <code>HeapMemory</code> 
     * representing the Java heap.
     * @return The singleton <code>HeapMemory</code> object.
     */
    public static HeapMemory instance() { 
        return instance; 
    }
}







