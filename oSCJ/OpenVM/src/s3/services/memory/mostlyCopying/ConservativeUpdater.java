package s3.services.memory.mostlyCopying;

import ovm.core.execution.Activation;
import ovm.core.execution.Context;
import ovm.core.execution.NativeInterface;
import ovm.core.execution.Native;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import ovm.core.services.memory.LocalReferenceIterator;
import ovm.util.Iterator;

/**
 * A base class for conservative, moving GC.  This class inherits
 * scanning support from {@link ExtentUpdater} and adds support for
 * conservative stack walking.<p>
 *
 * FIXME:  This inheritence relationship seems wrong.  It exists
 * because each thread state contains one precies root: the pointer
 * from a native context to it's java peer.  This root must be walked
 * with {@link ExtentUpdater#updateLoc} or some equivalent for a
 * non-moving collector.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 */
public abstract class ConservativeUpdater extends ExtentUpdater {
    /**
     * This method is called on every word in a thread stack.  If the
     * word may be an object reference, the appropriate action should
     * be taken.
     **/
    public abstract void checkConservativeRoot(VM_Word w);

    /**
     * This method calls {@link #checkConservativeRoot} on every
     * potential pointer in the system.  It iterates over the
     * register set and stack of all threads other than the current
     * one, and iterates over the part of the current thread stack
     * below <code>curThreadSkip</code>.
     * 
     * @param curThreadSkip the activation of the main GC loop.  The
     * current thread stack is walked starting with this activation's
     * caller,
     **/
    public void walkThreadStacks(Activation curThreadSkip) {
	LocalReferenceIterator lri=LocalReferenceIterator.the();
	
	lri.walkCurrentContexts(LocalReferenceIterator.CONSERVATIVE);
	while (lri.hasNext()) {
	    checkConservativeRoot(lri.next().getWord());
	}
	
	lri.walkCurrentContexts(LocalReferenceIterator.PRECISE);
	while (lri.hasNext()) {
	    updateLoc(lri.next());
	}
    }
}

    
