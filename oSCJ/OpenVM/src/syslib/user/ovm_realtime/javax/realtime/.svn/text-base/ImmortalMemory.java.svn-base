/*
 * $Header: /p/sss/cvs/OpenVM/src/syslib/user/ovm_realtime/javax/realtime/ImmortalMemory.java,v 1.2 2005/06/09 02:29:57 dholmes Exp $
 */
package javax.realtime;

/** 
 * <code>ImmortalMemory</code> is a memory resource that is shared among all 
 * threads. Objects allocated in the immortal memory live until the end of 
 * the application. Objects in immortal memory are never subject to garbage 
 * collection, although some GC algorithms may require a scan of the immortal 
 * memory. An immortal object may only contain references to other immortal 
 * objects or to heap objects. Unlike standard Java heap objects, immortal 
 * objects continue to exist even after there are no other references to them.
 */
public class ImmortalMemory extends MemoryArea {

    /** the singleton instance representing immortal memory */
    protected static final ImmortalMemory instance = new ImmortalMemory();

    /** private, trivial constructor */
    private ImmortalMemory() {
	super(LibraryImports.getImmortalArea());
    }

    /**
     * Returns a pointer to the singleton <code>ImmortalMemory</code>
     * space.
     * @return The singleton <code>ImmortalMemory</code> object.
     */
    public static ImmortalMemory instance() { 
        return instance; 
    }
}
