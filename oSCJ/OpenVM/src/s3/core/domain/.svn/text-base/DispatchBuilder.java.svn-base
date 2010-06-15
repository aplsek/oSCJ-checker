package s3.core.domain;

import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.repository.Bytecode;
import ovm.core.repository.Mode;
import ovm.core.repository.RepositoryException;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.util.ArrayList;
import ovm.util.List;
import s3.core.S3Base;
import ovm.core.domain.Code;

/**
 * @author Jan Vitek
 * @author K. Palacz
 * @author Christian Grothoff
 * @author Jason Baker 
 */
public class DispatchBuilder extends S3Base {

    private final static boolean saveSpaceOnFinals  = false; 
    private HTUnboundSel2int vtableOffsets_;
    private HTSelector_Method2int nvtableOffsets_;

    private S3Blueprint.Scalar bpt;
    private S3Type.Scalar type;
 
    // Hmm, should this be here or not ...
    private HTUnboundSel2int ifaceOffsets = new HTUnboundSel2int();

    private int lastGlobalInterfaceMethodIndex = -1;

    private UnboundSelector.Method[] offset2iface;

    
    public DispatchBuilder() {
	reset();
    }

    /**
     * Given a class or interface type and an IFTable index, return
     * the selector for the sole interface method that may appear at
     * that index, or null.
     *
     * FIXME Shouldn't be a selector!
     *
     * @param t     receiver type, in case multiple interface methods
     *               share an IFtable slot
     * @param index  into iftable
     * @return the method located at the IFTable index for the type
     */
    public UnboundSelector.Method getInterfaceMethod(Type t, int index) {
	if (offset2iface == null) {
	    offset2iface = new UnboundSelector.Method[ifaceOffsets.size()];
	    HTUnboundSel2int.Iterator it = ifaceOffsets.getIterator();
	    while (it.hasNext()) {
		UnboundSelector.Method m = it.next();
		offset2iface[ifaceOffsets.get(m)] = m;
	    }
	}
	return offset2iface[index];
    }

    public void reset() {
	vtableOffsets_ = HTUnboundSel2int.EMPTY;
	nvtableOffsets_ = HTSelector_Method2int.EMPTY;
	bpt = null;
    }

 
    public void setBlueprint(S3Blueprint.Scalar bpt) {
	boolean prealloc = (type == null);
	this.bpt = bpt;
	this.type = (S3Type.Scalar)bpt.getType().asScalar();
	if (prealloc) {
	    allocObjectMethods();
	}
    }
   
    /**
     * Create a vtable for the <code>Blueprint</code> associated with this
     * dispatch object. The entries in the vtbl can be null if we are
     * dealing with a static or abstract method.
     * FIXME change this to a singleton representing an arbitrary static method. --jv
     * @return an array of bytecode fragments representing this
     * dispatch object's vtable. The vtable is non-null by definition
     * since there are at least some method inherited from the root of
     * the hierarchy.  FIXME if class is abstract, allocate VTBL
     * entries for all abstractly implemented interface methods.
     **/
    public Code[] createVTable() {
	S3Blueprint.Scalar parentBp =  bpt.getParentBlueprint();
    
	Code[] parentVTBL = ((parentBp == null) ? Code.EMPTY_ARRAY
			     : parentBp.getVTable());

	if ( type.isInterface() || 0 == type.localMethodCount()) // FIXME ifc special 
	    return parentVTBL; // Returning parentVTBL depends on the assumption vtbl arrays are immutable.

	int guessChildVTBLSize = parentVTBL.length * 2;
	
	List slots = new ArrayList(guessChildVTBLSize);
	
	for (int i = 0; i < parentVTBL.length; i++) {
	    slots.add(parentVTBL[i]);
	}
	int localMethodCount = type.localMethodCount();
	for (int i = 0; i < localMethodCount; i++) {
	    Method method = type.getLocalMethod(i);
	    Mode.Method mode = method.getMode();

	    if (method.isConstructor() || mode.isPrivate()) {
		continue;
	    }

	    S3ByteCode cf = method.getByteCode();
	    if (cf.getBytes() == null) checkNullCode(method);

	    if (vtableOffsets_ == HTUnboundSel2int.EMPTY) {
		vtableOffsets_ = new HTUnboundSel2int(localMethodCount);
	    }
	    // NB: we tolerate collisions in vtableOffsets_ on the assumption
	    // that access speed is not primordial

	    UnboundSelector.Method sel = method.getSelector().getUnboundSelector();
	    int old = HTUnboundSel2int.NOTFOUND;

	    // FIXME: see bug #513: this is broken since we must take
	    // visibility of the inherited methods
	    // (access modifiers [default]) into account!
	    if (parentBp != null)old = S3MemberResolver.resolveVTableOffset(parentBp, sel, type);

	    if (old != HTUnboundSel2int.NOTFOUND) { // overides parent method
		vtableOffsets_.put(sel, old);
		slots.set(old, method.getCode());
	    } else if (saveSpaceOnFinals && mode.isFinal()) {
		// a final method that does not override an existing method
		// doesn't need a vtable slot
		continue;
	    } else { // does not overide
		vtableOffsets_.put(sel, slots.size()); 
		slots.add(method.getCode());
	    }
	}
	Code[] vt = new Code[slots.size()];
	slots.toArray(vt);
	return vt;
    }

    public Code[] createNVTable() {
        if ( type.isInterface() )
            return Code.EMPTY_ARRAY;
	ArrayList list = new ArrayList();
	int localMethodCount = type.localMethodCount();
	if (bpt.isSharedState()) {
	    for (int i = 0; i < localMethodCount; i ++) {
		Method method = type.getLocalMethod(i);
		Code cf = method.getCode(); 
		if (nvtableOffsets_ == HTSelector_Method2int.EMPTY) {
		    nvtableOffsets_ = new HTSelector_Method2int();
		}
		nvtableOffsets_.put(method.getSelector(), i);
		list.add(cf);
	    }
	    
	} else {
	    for (int i = 0; i < localMethodCount; i ++) {
		Method method = type.getLocalMethod(i);
		if (method.isConstructor() || method.getMode().isPrivate()) {
		    Code code = method.getCode();
		    if (nvtableOffsets_ == HTSelector_Method2int.EMPTY) {
			nvtableOffsets_ = new HTSelector_Method2int();
		    }
		    assert(nvtableOffsets_.get(method.getSelector()) == -1); 
		    nvtableOffsets_.put(method.getSelector(), list.size());
		    list.add(code);
		}
	    }
	}
	Code[] nvtbl = new Code[list.size()];
	return (Code[])list.toArray(nvtbl);
    }
    
    // called twice, once to compute max (with table=null), and the second time
    // to fill table.
    private int createIFTable_(Type.Interface[] ifaces, Code[] table) throws LinkageException {
	int max = -1;

	for (int i = 0; i < ifaces.length; i++) {
	    Method.Iterator iter = ifaces[i].methodIterator();
	    while (iter.hasNext()) {
		Method ifmethod = iter.next();
		// intefaces inherit all Object methods; don't put them in the iftable
		if (!ifmethod.getMode().isAbstract()) continue;

		UnboundSelector.Method sel = ifmethod.getExternalSelector().getUnboundSelector();
		int index = getIFTableOffset(sel);
		max = Math.max(index, max);
		if (table==null) continue; // first time around table==null to compute max.
		
		int vtblIndex = S3MemberResolver.resolveVTableOffset(bpt, sel, type); 
		
		// if we have an implementation of the method in a vtable, use that.
		if (vtblIndex == HTUnboundSel2int.NOTFOUND)  {
		    Method meth = S3MemberResolver.resolveInterfaceMethod(type, ifmethod.getExternalSelector().getUnboundSelector(),null);
		    table[index] =  meth.getCode();
		} else {
		    Code[] vTable = bpt.getVTable();
		    table[index] = vTable[vtblIndex];	
		}		
	    }
	}
	return max;
    }

    /** Build an interface table. 
     * @return a (possibly emtpy) array of bytecode.
     * @throws LinkageException
     */
    public Code[] createIFTable() throws LinkageException {
// 	    try { forceInterfaceLoad(type); } 
// 	    catch (RepositoryException e) { throw e.fatal(); }
	    Type.Interface[] ifaces = type.getAllInterfaces();		
	    int max = createIFTable_(ifaces, null);
	    if (max < 0) return Code.EMPTY_ARRAY;
	    Code[] itbl = new Code[max + 1];
	    createIFTable_(ifaces, itbl);
	    return itbl;	
    }    

    int getExistingIFTableOffset(UnboundSelector.Method sel) {
	synchronized (ifaceOffsets) {
	    int offset = ifaceOffsets.get(sel);
	    return offset == HTUnboundSel2int.NOTFOUND ? -1 : offset;
	}
    }

    int getIFTableOffset(UnboundSelector.Method sel) {
        synchronized (ifaceOffsets) {
            int offset = ifaceOffsets.get(sel);
            if (offset == HTUnboundSel2int.NOTFOUND) {
                lastGlobalInterfaceMethodIndex++;
                ifaceOffsets.put(sel, lastGlobalInterfaceMethodIndex);
                return lastGlobalInterfaceMethodIndex;
            }
            return offset;
        }
    }

    private void allocObjectMethods() {
	Method.Iterator iter = getDomain().getHierarchyRoot().methodIterator();
	while (iter.hasNext()) {
	    Method method = iter.next();
	    if (method.isConstructor())
		continue; 
		//interfaces inherit all object methods except the Object constructor 
	    UnboundSelector.Method sel = method.getExternalSelector().getUnboundSelector();
	    getIFTableOffset(sel);
	}
    }
	

    HTUnboundSel2int getVTableOffsets() {
	return vtableOffsets_;
    }

    HTSelector_Method2int getNonVTableOffsets() {
	return nvtableOffsets_;
    }

    private S3Domain getDomain() {
	return (S3Domain)type.getDomain();
    }


    private static void checkNullCode(Method method) {
	Mode.Method mode = method.getMode();
	if (mode.isNative()) {
	    //Logger.global.fine("Native stub " + cf + " for:" + methods[i]);
	} else if (mode.isAbstract()) {
	    // ok
	} else fail("Null selector for " + method);
    }		
} 
