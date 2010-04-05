package ovm.core.services.memory;

import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;

/**
 * Define the executive domain allocation policy.  This class tell the
 * OVM which VM_Area to use when allocated various kinds of objects.
 * Defaults are provided to allocate everything in the heap.
 **/
public class DefaultMemoryPolicy extends MemoryPolicy {
    protected final VM_Area enter(VM_Area a) {
	return MemoryManager.the().setCurrentArea(a);
    }

    protected final VM_Area enterImmortal() {
	return enter(MemoryManager.the().getImmortalArea());
    }
    
    /**
     * This method assumes that enter* always returns the outer
     * VM_Area, and that the only cleanup needed is to restore it
     * @param data 
     **/
    public void leave(Object data) {
	MemoryManager.the().setCurrentArea((VM_Area) data);
    }

    /**
     * 
     **/
    public Object enterHeap() {
	return enter(MemoryManager.the().getHeapArea());
    }

    /**
     * We don't want to generate an exception in an area that will
     * die before it is caught, but we don't want exceptions to hang
     * around forever either.
     **/
    public Object enterExceptionSafeArea() {
	return enterHeap();
    }
    
    /**
     * Monitors should always be allocated in the same area as their
     * corresponding objects.  This method makes
     * MemoryManager.the().areaOf(oop) current.
     * @param oop 
     **/
    public Object enterAreaForMonitor(Oop oop) {
	return enter(MemoryManager.the().areaOf(oop));
    }

    /**
     * VM_Areas should be allocated in the same area as the
     * corresponding javax.realtime.MemoryArea.
     * java.lang.reflect.Methods should be allocated in the same area
     * as the corresponding ovm.core.domain.Method, and so on.  This
     * method makes MemoryManager.the().areaOf(oop) current.
     * @param oop 
     **/
    public Object enterAreaForMirror(Oop oop) {
	return enter(MemoryManager.the().areaOf(oop));
    }

    public Object enterMetaDataArea(Type.Context ctx) {
	return enterHeap();
    }

    public Object enterInternedStringArea(Domain d) {
	return enterHeap();
    }
    
    public Object enterServiceInstanceArea() {
	return enterHeap();
    }

    public boolean isInternable(Domain d, Oop str)	{ return true; }

    public Object enterClinitArea(Type.Context ctx) {
	return enterHeap();
    }

    /**
     * By default, this method equivalent to enterScratchPadArea, but
     * by default, the scratch-pad area is the heap
     **/
    public Object enterRepositoryQueryArea() {
	return enterScratchPadArea();
    }
    public Object enterRepositoryDataArea() {
	// The member resolver can end up creating Selectors that have
	// no equivalent in bytecode (there are no calls through this
	// type, and there is no definition in this type).
	return enterHeap();
	// enterExceptionSafeArea();
	// throw new ReadonlyViewException();
    }
    public Object enterScratchPadArea() {
	return enterHeap();
    }
}
