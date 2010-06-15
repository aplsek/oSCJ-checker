package ovm.core.services.memory;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Oop;
import ovm.core.execution.NativeConstants;

import s3.util.PragmaNoPollcheck;

public abstract class ExtentWalker {
    static public abstract class Scanner extends ExtentWalker {
	protected abstract void walk(Oop object, Blueprint type);

	public int getVariableSize(Blueprint bp, Oop o) {
	  return bp.getVariableSize(o);
	}

	protected void pollWalking() {
	} // put pollcheck here if needed to be incremental

	public VM_Address walkRet(VM_Address start, VM_Address end) throws PragmaNoPollcheck {
	    while (start.uLT(end)) {
//		Oop o = start.asOop();
		Oop o = start.asOopUnchecked();
		Blueprint bp = o.getBlueprint();
		if (bp == null) {
		    // FIXME: VM_Word vs. int. Changing it wakes up j2c bugs.
		    int next = start.asInt() + NativeConstants.PAGE_SIZE;
		    start = VM_Address.fromInt(next & -NativeConstants.PAGE_SIZE);
		} else {
		    start = start.add(getVariableSize(bp,o));
		    walk(o, bp);
		}
		pollWalking();
	    }
	    return start;
	}
	
	public void walk(VM_Address start, VM_Address end) throws PragmaNoPollcheck {
	    while (start.uLT(end)) {
//		Oop o = start.asOop();
		Oop o = start.asOopUnchecked();
		Blueprint bp = o.getBlueprint();
		if (bp == null) {
		    // FIXME: VM_Word vs. int. Changing it wakes up j2c bugs.
		    int next = start.asInt() + NativeConstants.PAGE_SIZE;
		    start = VM_Address.fromInt(next & -NativeConstants.PAGE_SIZE);
		} else {
		    start = start.add(getVariableSize(bp,o));
		    walk(o, bp);
		}
		pollWalking();
	    }
	}
	
    }

    public abstract void walk(VM_Address start, VM_Address end);
    public abstract VM_Address walkRet(VM_Address start, VM_Address end);
}
