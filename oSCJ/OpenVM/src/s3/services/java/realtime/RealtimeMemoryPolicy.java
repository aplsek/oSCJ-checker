package s3.services.java.realtime;

import ovm.core.OVMBase;
import ovm.core.domain.Domain;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.execution.Context;
import ovm.core.services.memory.MemoryManager;
import ovm.core.services.memory.ScopedMemoryPolicy;
import ovm.core.services.memory.VM_Area;
import ovm.services.realtime.NoHeapRealtimeOVMThread;

import ovm.core.stitcher.JavaServicesFactory;

import ovm.services.java.JavaDispatcher;
import ovm.core.stitcher.ThreadServiceConfigurator;

public class RealtimeMemoryPolicy extends ScopedMemoryPolicy {
    /**
     * This class is currently unused, but it is a nice way to
     * reserve a large chunk of memory that must be shared between
     * threads.  At one point, there was a lot of temporary allocation
     * when dealing with utf8 string.<p>
     *
     * These allocations could not be performed in the per-thread
     * scratchpad, because their allocation bound is too high.  In a
     * non-realtime JVM, these operations can allocate temporaries on
     * the heap, but we don't have that option.  AtomicArea provides
     * mutually exclusive, non-reentrant, access to an area across
     * multiple threads.<p>
     *
     * @author <a href="mailto://baker29@cs.purdue.edu"> Jason Baker </a>
     **/
    protected static class AtomicArea {
	VM_Area area;
	Context owner;
	AtomicArea(VM_Area a) { area = a; }

	synchronized Object enter() {
	    assert owner != Context.getCurrentContext():
		"recursive entry to atomic area";
	    while (owner != null) {
		try { wait(); }
		catch (InterruptedException _) { }
	    }
	    owner = Context.getCurrentContext();
	    VM_Area outer = MemoryManager.the().setCurrentArea(area);
	    return new Cleanup(outer) {
		    public void leave() {
			// we need to refer to the outer this after
			// calling areaOf(inner this).reset(), so we
			// copy it into a local
			AtomicArea aa = AtomicArea.this;
			synchronized (aa) {
			    super.leave();
			    assert(owner == Context.getCurrentContext());
			    aa.owner = null;
			    aa.area.reset();
			    aa.notify();
			}
		    }
		};
	}
    }

    public RealtimeMemoryPolicy(String scSize) {
	super(scSize);
    }

    public Object enterMetaDataArea(Type.Context _) {
	return enterImmortal();
    }

    public Object enterInternedStringArea(Domain _) {
	return enterImmortal();
    }

    /**
     * FIXME: what about aliasing? An immortal string may well have a
     * heap- or even scope- allocated char[].
     **/
    public boolean isInternable(Domain _, Oop str) {
	return (MemoryManager.the().areaOf(str)	==
		MemoryManager.the().getImmortalArea());
    }

    public Object enterClinitArea(Type.Context ctx) {
	return enterImmortal();
    }

    public Object enterRepositoryDataArea() {
	return enterImmortal();
    }

    // Java dispatcher
    private static final JavaDispatcher jd;
    static  {
         JavaServicesFactory jsf = (JavaServicesFactory)
            ThreadServiceConfigurator.config.getServiceFactory(
                JavaServicesFactory.name);
	 jd = jsf.getJavaDispatcher();
    }

    public Object enterHeap() {
	VM_Area heap = MemoryManager.the().getHeapArea();
	VM_Area outer = MemoryManager.the().setCurrentArea(heap);

	if (MemoryManager.the().readBarriersEnabled()) {
	    final NoHeapRealtimeOVMThread th =
		(NoHeapRealtimeOVMThread) jd.getCurrentVMThread();
	    th.enableHeapChecks(false);
	    MemoryManager.the().enableReadBarriers(false);
	    // Allocate cleanup object in heap to avoid leaks
	    return new Cleanup(outer) {
		public void leave() {
		    super.leave();
		    // After enableHeapChecks and enableReadBarriers,
		    // we can no longer read from this.
		    th.enableHeapChecks(true);
		    MemoryManager.the().enableReadBarriers(true);
		}
	    };
	} else {
	    return outer;
	}
    }
}
	
