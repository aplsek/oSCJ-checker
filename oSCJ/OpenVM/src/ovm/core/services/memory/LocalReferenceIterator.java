package ovm.core.services.memory;
import ovm.core.execution.Context;
import ovm.core.stitcher.InvisibleStitcher;
import ovm.core.stitcher.InvisibleStitcher.Component;
import ovm.core.stitcher.InvisibleStitcher.MisconfiguredException;
import ovm.core.stitcher.InvisibleStitcher.PragmaStitchSingleton;
import ovm.util.Iterator;
import ovm.core.execution.Native;
import ovm.util.OVMError;
import s3.services.j2c.J2cImageCompiler;
import s3.services.bootimage.ImageObserver;
import s3.util.PragmaTransformCallsiteIR.BCdead;


/** Accurately iterates over references on the stack.  The procedure
    for using this class is as follows:
    <ul>
    <li>Start by calling prepareForGC() and passing a callback.</li>
    <li>Within the callback you use the other instance methods.  You
        cannot use any of the instance methods other than prepareForGC()
	unless you are in the callback.</li>
    <li>Once you are ready to walk the stacks, call either
        walkTheseContexts() or walkCurrentContexts().  Note that
	walkCurrentContexts() allocates memory.
    <li>Then use hasNext()/next() to walk the references.
    </ul>
    It should be noted that if the GC has its own thread, it may call
    prepareForGC() once in the whole lifetime of the system.
*/
public abstract class LocalReferenceIterator implements Component {
    public static final int PRECISE = 1;
    public static final int CONSERVATIVE = 2;

    protected abstract boolean moreReferencesInThread();
    protected abstract VM_Address nextReferenceInThread();
    protected abstract void setThread(Context thread);

    private Iterator contexts;
    private int mode_;
    
    /** a bitmask of supported modes */
    public int supportedModes() { return PRECISE; }
    /** the current mode (note: we can only be in one mode at a time) */
    public int mode() { return mode_; }

    /** are we currently walking conservative roots? */
    public boolean conservative() { return mode_==CONSERVATIVE; }
    /** are we currently walking precise roots? */
    public boolean precise() { return mode_==PRECISE; }
    
    /** is choosing a given mode going to yield a non-empty set? */
    public boolean modeSupported(int mode) { return (mode&supportedModes())!=0; }
    public boolean modeSupported() { return modeSupported(mode_); }

    /** will walking precise roots yield a non-empty set? */
    public boolean preciseSupported() { return (supportedModes()&PRECISE)!=0; }
    /** will walking conservative roots yield a non-empty set?  translation: if this
	returns true then your collector must be able to deal with conservative roots.
	if this returns false, your collector can be accurate. */
    public boolean conservativeSupported() { return (supportedModes()&CONSERVATIVE)!=0; }
    
    public void forcePrecise() {
	if (conservativeSupported()) {
	    throw new OVMError("Mismatch between stack walking and memory management configuration: the memory manager requires precise stack walking, but the local reference iterator claims that it may return ambiguous roots.");
	}
    }
    
    /** does the engine need to do something special for this?  default is true, since the iterator for which it is false are in the minority. */
    public boolean expectsEngineSupport() {
	return true;
    }

    /**
     * If this LocalReferenceIterator expects support from {@link
     * J2cImageCompiler j2c}, verify that {@code -engine=j2c} was
     * configured.  In the future, there may be LocalReferenceIterators
     * that depend on support from other compilers.  They will have to
     * override this method.
     **/
    public void initialize() throws BCdead {
	if (expectsEngineSupport() &&
	    !(ImageObserver.the() instanceof J2cImageCompiler))
	    throw new MisconfiguredException("LocalReferenceIterator requires "+
					     "j2c");
    }

    protected void nextThread() {
	setThread((Context) contexts.next());
    }

    public boolean hasNext() {
	return modeSupported() && (moreReferencesInThread() || contexts.hasNext());
    }

    public VM_Address next() {
	if (!moreReferencesInThread())
	    nextThread();
        return nextReferenceInThread();
    }
    
    protected void prepareForGCHook() {}
    
    public void prepareForGC() {
	prepareForGCHook();
	MemoryManager.the().doGC();
    }
    
    protected void aboutToWalkHook() {}
    
    protected void callNextThreadForFirstThread() {
	nextThread();
    }
    
    // you can use this method to force the iterator to walk one thread at a time,
    // for example.
    public void walkTheseContexts(Iterator contexts,int mode) {
	mode_=mode;
	if (modeSupported()) {
	    aboutToWalkHook();
	    this.contexts=contexts;
	    callNextThreadForFirstThread();
	}
    }
    
    public void walkCurrentContexts(int mode) {
	walkTheseContexts(Context.iterator(),mode);
    }
    
    public void walkTheseContexts(Iterator contexts) {
	walkTheseContexts(contexts,PRECISE);
    }
    
    public void walkCurrentContexts() {
	walkCurrentContexts(PRECISE);
    }
    
    protected LocalReferenceIterator() {}
    
    public static LocalReferenceIterator the() throws PragmaStitchSingleton {
	return (LocalReferenceIterator)InvisibleStitcher.singletonFor(LocalReferenceIterator.class);
    }
}
