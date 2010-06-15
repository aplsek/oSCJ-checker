
package s3.core.services.memory;

import ovm.core.Executive;
import ovm.core.domain.Oop;
import ovm.core.execution.Native;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Area;
import ovm.util.CommandLine;
import s3.util.PragmaAtomic;
import s3.util.PragmaNoPollcheck;

/**
 * A minimal implementation of {@link MemoryManager}. This one just allocates
 * (by pointer bumping) from a single large memory range allocated in its
 * entirety at boot time, has no collector and no support for freeing, and so
 * never reclaims anything.
 * @author jv
 **/
public class S3MinimalMemoryManager extends MemoryManager {

    /**
     * Amount of memory this trivial allocator can handle. In bytes.
     **/
    private final int MAX_MEM_SIZE;
    
    /**
     * @param heapSize size of the memory area to obtain from the OS at boot
     * time; see {@link CommandLine#parseSize(String) parseSize} for syntax.
     **/
    public S3MinimalMemoryManager(String heapSize) {
      	MAX_MEM_SIZE = CommandLine.parseSize( heapSize);
    }

    // Pin object at build time
    public void pin(Oop o) { VM_Address.fromObject(o).asInt(); }
    public void unpin(Oop _) { }
    
    public boolean supportsGC() { return false; }
    public void garbageCollect() {
	BasicIO.out.println("MinimalMemoryManager: ignoring request "
			    + "for garbage collection");
    }
    public void doGC() {
	Executive.panic("who started a garbage collection?");
    }

    private static OutOfMemoryError oome;
    private VM_Address memory_;
    private int pos_;

    public final void boot(boolean useImageBarrier) {
        this.memory_ = Native.getmem(MAX_MEM_SIZE);
        oome = new OutOfMemoryError();
    }

    private VM_Area heapArea = new VM_Area() {
	    public int size() { return MAX_MEM_SIZE; }
	    public int memoryConsumed() { return pos_; }
	    public VM_Address getBaseAddress() {
		return memory_;
	    }
	    public VM_Address getMem(int size) {
		return S3MinimalMemoryManager.this.getMem(size);
	    }
            public String toString()throws PragmaNoPollcheck { return "Heap Area"; }
	    public Oop revive(Destructor d) {
		throw Executive.panic("destructors run without GC");
	    }
	    public void addDestructor(Destructor _) { }
	    public void removeDestructor(Destructor _) { }
	    public int destructorCount(int kind) { return 0; }
	};

    public VM_Area getHeapArea() {
	return heapArea;
    }

    /**
     * Allocate a chunk of memory.
     * <p>Atomicity is ensured by using {@link PragmaAtomic}.
     * @param size the number of bytes to allocate
     * @return the address of the allocated chunk
     */
    protected VM_Address getMem(int size) throws PragmaAtomic {
        int current = pos_;
        if (current + size <= MAX_MEM_SIZE) {
            pos_ += size;
            return memory_.add(current);
        } else
            throw oome;    
    }
    
}
