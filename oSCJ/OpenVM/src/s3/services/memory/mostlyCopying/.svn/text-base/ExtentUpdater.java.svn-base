package s3.services.memory.mostlyCopying;
import ovm.core.domain.Blueprint;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.services.memory.ExtentWalker;
import ovm.core.services.memory.MovingGC;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.memory.VM_Word;
import ovm.core.services.memory.PragmaNoBarriers;
import ovm.util.NumberRanges;
import ovm.core.Executive;

/**
 * A base class for implementing Chenney scanning garbage collection.
 * Essentially, this class provides support for scanning root objects
 * and grey objects.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 **/
public abstract class ExtentUpdater extends ExtentWalker.Scanner {
    /**
     * true if <code>oop</code> either has been or should be moved in
     * this GC cycle.
     **/
    public abstract boolean needsUpdate(MovingGC oop);
    /**
     * Called to move an object to a new location.  That location
     * should be returned, and recorded as the forwarding address of
     * oop.
     **/
    public abstract Oop updateReference(MovingGC oop);

    /**
     * This method takes the address of an object reference, and
     * updates that location in memory.  This method is implemented in
     * terms of {@link #needsUpdate}, {@link #updateReference}, and
     * {@link ovm.core.services.memory.MovingGC#getForwardAddress}.
     **/
    public void updateLoc(VM_Address loc) {
	MovingGC oop = (MovingGC) loc.getAddress().asAnyOop();
	if (needsUpdate(oop)) {
	    Oop newVal = (oop.isForwarded()
			  ? oop.getForwardAddress().asOop()
			  : updateReference(oop));
	    loc.setAddress(VM_Address.fromObject(newVal));
	}
    }
    
    public void updateHeader(Oop.WithUpdate oop) {
	for (int i = 0;
	     i < ObjectModel.getObjectModel().maxReferences();
	     i++) {
	    MovingGC r = (MovingGC) oop.getReference(i).asAnyOop();
	    if (needsUpdate(r)) {
		Oop newVal = (r.isForwarded()
			      ? r.getForwardAddress().asOop()
			      : updateReference(r));
		oop.updateReference(i, newVal);
	    }
	}
    }
    
    public void walk(Oop _oop, Blueprint bp) {
	MovingGC oop = (MovingGC) _oop.asAnyOop();
	updateHeader(oop);

	if (bp.isArray()) {
	    Blueprint.Array abp = bp.asArray();
	    if (abp.getComponentBlueprint().isReference()) {
		VM_Address p = abp.addressOfElement(oop, 0);
		VM_Address pend = p.add(VM_Word.widthInBytes()
					* abp.getLength(oop));
		while (p.uLT(pend)) {
		    updateLoc(p);
		    p = p.add(VM_Word.widthInBytes());
		}
	    }
	}
	else {
	    VM_Address base = VM_Address.fromObject(oop);
	    int[] offset = bp.getRefMap();
	    for (int i = 0; i < offset.length; i++)
		updateLoc(base.add(offset[i]));
	}
    }
}
	    
	    

    
