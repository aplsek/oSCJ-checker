package s3.core.domain;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.repository.Mode;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.util.OVMError;
import s3.core.S3Base;


//FIXME REAFACTOR
public abstract class S3MemberResolver extends S3Base {


    // FIXME access site
    public static Method resolveStaticMethod(Type.Compound target, UnboundSelector.Method usel, Type accessSite) {
	Type.Scalar instanceType, shstType;
	if (target.isSharedState()) {
	    instanceType = (Type.Scalar) target.getInstanceType();
	    shstType = (Type.Scalar) target;
	} else {
	    instanceType = (Type.Scalar) target;
	    shstType = target.getSharedStateType();
	}
	for (int count = 0; count < 100; ++count) {
	    Method answer = shstType.getMethod(usel);
	    if (answer != null) { return answer; }
	    instanceType = instanceType.getSuperclass();
	    if (null == instanceType) return null;
	    shstType = instanceType.getSharedStateType();
	}
	throw new OVMError("loop count limit exceeded");
    }
    /**
     * ignore the accesibility rules, intentionally long name
     **/
    static int resolveVTableOffsetNoAccessibility(S3Blueprint target, UnboundSelector.Method sel) {
	int offset = target.getVTableOffsets().get(sel);
	if (offset != -1) return offset;
	Blueprint parent = target.getParentBlueprint();
	return parent != null ? resolveVTableOffsetNoAccessibility((S3Blueprint) parent, sel) : -1;
    }


    // FIXME deal with accessSite
    public static Field resolveStaticField(Type.Class target, UnboundSelector.Field sel, Type accessSite) {
	Type.Scalar instanceType, shstType;
	if (target.isSharedState()) {
	    shstType = (Type.Scalar) target;
	    instanceType = (Type.Scalar) shstType.getInstanceType();
	} else {
	    instanceType = (Type.Scalar) target;
	    shstType = target.getSharedStateType();
	}
	for (int count = 0; count < 100; count++) {
	    Field fld = shstType.getField(sel);
	    if (null != fld) return fld;
	    Type.Interface[] ifcs = instanceType.getAllInterfaces();
	    for(int i=ifcs.length; i --> 0;) {
		fld = resolveStaticField(ifcs[i].getSharedStateType(),sel,accessSite);
		if (fld!=null)return fld;
	    }
	    instanceType = instanceType.getSuperclass();
	    if (null == instanceType) return null;
	    shstType = instanceType.getSharedStateType();
	}
	throw new OVMError("loop count limit exceeded"); // FIXME arbitrary limit!
    }

    static private boolean isAccessible(Mode.Member modifier, Type.Compound what, Type.Compound fromWhere) {
	if (modifier.isPrivate()) {
	    return what == fromWhere;
	    // Private methods are not in the vtable!
	} else if (modifier.isProtected()) {
	    // FIXME: Bug 506 - subtypeof is incorrect test for package access
	    // it will allow some illegal access and prevent legal access.
	    // "protected" is much more subtle than simply "is-subtype-of"
	    // As a partial fix we check to see if the call-site is a G-type
	    // and if so convert to an L-type and then do subtype-of - DH
	    TypeName.Compound fromWhereName = fromWhere.getName();
	    if (fromWhereName.isGemeinsam() && !what.getName().isGemeinsam()) {
		TypeName.Gemeinsam gname = (TypeName.Gemeinsam) fromWhereName;
		TypeName instTypeName = gname.getInstanceTypeName();
		try {
		    Type instType = fromWhere.getContext().typeFor(instTypeName);
		    boolean result = instType.isSubtypeOf(what);
		    return result
			    || (what.getName().getPackageNameIndex() == fromWhere.getName().getPackageNameIndex());
		} catch (LinkageException ex) {
		    throw new Error("Stuffed up gemeinsam conversion");
		}
	    } else {
		return fromWhere.isSubtypeOf(what)
			|| (what.getName().getPackageNameIndex() == fromWhere.getName().getPackageNameIndex());
	    }
	} else if (modifier.isPublic()) {
	    return true;
	} else { // Default
	    return what.getName().getPackageNameIndex() == fromWhere.getName().getPackageNameIndex();
	}
    }

    // FIXME access site
    public static Method resolveInstanceMethod(Type.Compound target, UnboundSelector.Method usel, Type accessSite) {
	assert(!target.isSharedState());
	for (Type.Compound t = target; t != null; t = t.getSuperclass()) {
	    Method answer = t.getMethod(usel); 
	    if (answer != null) return answer;
	    Type.Scalar shstType = target.getSharedStateType();
	    if (shstType.getMethod(usel) != null)
		throw new OVMError("Static method found where instance method was expected");
	
	}
	return null;
    }

    public static Method resolveInterfaceMethod(Type.Scalar receiver, UnboundSelector.Method usel, Type accessSite) {
	Method result = resolveInstanceMethod(receiver, usel, accessSite);
	if (result != null) return result;

	for (Type.Compound t = receiver; t != null; t = t.getSuperclass()) {
	    Type.Interface[] ifcs = t.getAllInterfaces();
	    for (int i = ifcs.length; i-- > 0;) {
		Method answer = ifcs[i].getMethod(usel);
		if (answer != null) return answer;
	    }
	}
	return null;
    }

    public static Field resolveField(Type.Class target, UnboundSelector.Field sel, Type.Compound callSite) {
	do {
	    Field fld = target.getField(sel);
	    // FIXME callSite
	    if (null != fld) return fld;
	    target = target.getSuperclass();
	} while (null != target);
	return null;
    }

    /**
     * Get vtable offset for a any method. Requires the callSite to
     * deal correctly with protection modifiers. If the callSite is null we expect the 
     * method to be public.
     * @throws unchecked exception if method not found 
     **/
    public static int resolveVTableOffset(S3Blueprint target, UnboundSelector.Method sel, Type.Compound callSite) {

	int offset = target.getVTableOffsets().get(sel);
	if (offset != -1) {
	    Type.Scalar thistype = target.getType().asScalar();
	    Mode.Method mode = thistype.getMethod(sel).getMode();
	    if (callSite == null) return mode.isPublic() ? offset : -1;
	    else if (isAccessible(mode, thistype, callSite)) return offset;
	    else throw new LinkageException(sel + " not accessible from " + callSite).unchecked();
	}
	S3Blueprint parent = target.getParentBlueprint();
	return (parent != null) ? resolveVTableOffset(parent, sel, callSite) : offset; // may be -1
    }

    // FIXME deal with accessSite
    public static int resolveNonVTableOffset(Blueprint target, UnboundSelector.Method sel, Type accessSite) {
	S3Blueprint instanceBpt, shstBpt;
	// it is now possible (by Phase1) that 'target' already is the shared state
	// blueprint. Handle both cases.
	// FIXME once Phase1 is de rigueur, old case (target not shst) should go
	if (target.isSharedState()) {
	    shstBpt = (S3Blueprint) target;
	    instanceBpt = (S3Blueprint) target.getInstanceBlueprint();
	} else {
	    shstBpt = (S3Blueprint) target.getSharedState().getBlueprint();
	    instanceBpt = (S3Blueprint) target;
	}
	for (int count = 0; count < 100; ++count) {
	    // FIXME here we assume that the selector can not occur in the root of the hierarchy.
	    assert shstBpt.isSharedState()
		: "not shared state " + shstBpt + " started at " + target;

	    int idx = shstBpt.getNonVTableOffsets().get(Selector.Method.make(sel, shstBpt.getName().asCompound()));

	    Oop shst = instanceBpt.getSharedState();
	    if (idx >= 0 && shst != null) { return idx; }
	    if (instanceBpt.getType().isRoot()) { return -1; }
	    instanceBpt = instanceBpt.getParentBlueprint();
	    shstBpt = (S3Blueprint) instanceBpt.getSharedState().getBlueprint();
	}
	throw new OVMError("loop count limit exceeded");
    }

}
