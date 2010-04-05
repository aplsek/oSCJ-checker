package ovm.core.services.memory;

import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;

/**
 * Define the executive domain allocation policy.  This class tell the
 * OVM which VM_Area to use when allocated various kinds of objects.
 * Defaults are provided to allocate everything in the heap.
 **/
public abstract class MemoryPolicy {
    /**
     * Leave the current area, restoring allocation state using using
     * the result of a previous enter method.
     **/
    public abstract void leave(Object data);

    /**
     * Enter the garbage collected heap.  This method may be called
     * from NoHeapRealtimeThreads, at specific times where the VM
     * generates many temporary objects and makes no realtime
     * guarantees.
     **/
    public abstract Object enterHeap();

    /**
     * Enter an area in which we can safely throw exceptions.
     **/
    public abstract Object enterExceptionSafeArea();
    
    /**
     * The area in which to allocate the monitor record for a given
     * object.
     **/
    public abstract Object enterAreaForMonitor(Oop oop);

    /**
     * The area in which to allocate the ED shadow-self of a UD object.
     **/
    public abstract Object enterAreaForMirror(Oop oop);

    /**
     * This area holds java.lang.Class objects, shared state objects,
     * and ovm.core.domain data structures for a particular Type.Context
     * and its corresponding Classloader.
     **/
    public abstract Object enterMetaDataArea(Type.Context ctx);

    public final Object enterMetaDataArea(Domain d) {
	return enterMetaDataArea(d.getSystemTypeContext());
    }

    /**
     * This area holds strings to be interned from the constant pool.
     **/
    public abstract Object enterInternedStringArea(Domain d);

    /**
     * Return true if a string in an arbitrary domain can safely be
     * placed in the intern table.  This method's return value depends
     * a great deal on whether RTSJ NoHeapRealtimeThreads are
     * supported.
     **/
    public abstract boolean isInternable(Domain d, Oop string);
    
    /**
     * The area in which service instances live.
     */
    public abstract Object enterServiceInstanceArea();

    /**
     * The area in which static initializers are to be run.
     **/
    public abstract Object enterClinitArea(Type.Context ctx);
    
    /**
     * The area in which short-lived repository structures are
     * allocated.  If repositoryQueryArea(b) != repositoryDataArea(b),
     * a query object must be copied into the data area before it is
     * interned.  See the FIXME in scratchPadArea()
     **/
    public abstract Object enterRepositoryQueryArea();

    /**
     * This area holds interned repository symbols.<p>
     *
     * If an attempt is made to intern new repository symbols outside
     * of classfile parsing, an @see ovm.util.ReadonlyViewException
     * will be thrown.<p>
     *
     * Code that deals with unknown repository symbols, such as
     * classForName, must be prepared to deal with
     * ReadonlyViewExceptions.<p>
     * FIXME: the previous paragraph doesn't document the right method(s).
     **/
    public abstract Object enterRepositoryDataArea();

    /**
     * A per-thread scratchpad area.
     * FIXME: I think some sort of recursion count needs to be maintained.
     **/
    public abstract Object enterScratchPadArea();

    public static MemoryPolicy the() throws PragmaStitchSingleton {
	return StandaloneMemoryPolicy._;
    }
}
