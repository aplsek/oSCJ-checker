package ovm.core.services.memory;

import ovm.core.execution.Context;
import ovm.core.services.threads.OVMThreadContext;
import ovm.core.services.threads.OVMThread;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.Component;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;

/**
 * Associate Manager and Policy specific information with a thread
 * context.  This class is typically used with memory managers that
 * support the VM_Area interface and a memory policy that makes use of
 * the per-thread scratchpad area.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 **/
public class ScopedMemoryContext extends OVMThreadContext {
    // the current memory context can be an inner scope to the scope
    // in which this ScopedMemoryContext was allocated. Hence we need
    // do disable scope checks when setting this.
    private Object memoryContext;
    private VM_Area scratchPadArea;

    public static Object getMemoryContext() {
	return ((ScopedMemoryContext) getCurrentContext()).memoryContext;
    }

    public static void setMemoryContext(Object mc) throws PragmaNoBarriers {
	((ScopedMemoryContext) getCurrentContext()).memoryContext = mc;
    }

    public static VM_Area getScratchPadArea() {
	return ((ScopedMemoryContext) getCurrentContext()).scratchPadArea;
    }

    protected ScopedMemoryContext(OVMThread thisThread)
	throws PragmaNoBarriers
    {
	super(thisThread);
	memoryContext = MemoryManager.the().makeMemoryContext();
	scratchPadArea = MemoryManager.the().getCurrentArea();
	// FIXME: scratchpad may end up being one block too large.  I
	// don't understand why that happens.
// 	assert (scratchPadArea.size()
// 		== ((ScopedMemoryPolicy) MemoryPolicy.the()).SCRATCH_PAD_SIZE);
    }

    /**
     * Destroy this context.  Free the scratcpad memory assoicated
     * with this context.  A context cannot be referenced at all once
     * destroy has been called, since destroy <b>deallocates</b> the
     * Context itself.  Must not be invoked on the current context.
     **/
    public void destroy() {
	super.destroy();
	scratchPadArea.destroy();
    }

    protected ScopedMemoryContext() {
	memoryContext = MemoryManager.the().makeMemoryContext();
	scratchPadArea = null;
    }

    public void bootPrimordialContext() throws PragmaNoBarriers {
	super.bootPrimordialContext();
	scratchPadArea =
	    ScopedMemoryPolicy.theScopedPolicy().makeScratchPadArea();
    }
    
    public static class Factory extends OVMThreadContext.Factory
	implements Component
    {
	/**
	 * Verify that we are configured with a {@link ScopedMemoryPolicy}.
	 * We are responsible for initialize a thread's scratchpad
	 * area, but ScopedMemoryPolicy is the thing that builds
	 * them.  What is going on here, exactly?  We could certainly
	 * construct scratch pad areas here directly, but if we are
	 * using the default memory policy,
	 * {@link MemoryPolicy#enterScratchPadArea} will not work.<p>
	 *
	 * Suddenly, the advantadges of {@link
	 * ovm.core.stitcher.ServiceConfiguratorBase
	 * ServiceConfigurators} become apparent.
	 */
	public void initialize() {
	    // Explicitly call singletonFor, because the() returns
	    // something else.
	    if (!(InvisibleStitcher.singletonFor(MemoryPolicy.class)
		  instanceof ScopedMemoryPolicy))
		throw new MisconfiguredException("cannot use scoped memory " +
						 "without RTSJ memory policy");
	}
	public Context make() {
	    return new ScopedMemoryContext();
	}
	public OVMThreadContext make(OVMThread thisThread) {
	    return new ScopedMemoryContext(thisThread);
	}
    }
}
