package s3.core.domain;

import ovm.core.domain.Code;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.domain.Type.Class;
import ovm.core.repository.Bytecode;
import ovm.core.repository.Descriptor;
import ovm.core.repository.Mode;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.util.ArrayList;
import ovm.util.OVMError;
import s3.core.S3Base;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.Executive;
import s3.util.PragmaMayNotLink;

/**
 * @author KP, Christian Grothoff
 **/
public abstract class S3Method 
    extends S3Base 
    implements Method {

    public static S3Method[] EMPTY_ARRAY = new S3Method[0];
    
    public static S3Method makeMethod( RepositoryMember.Method rmm,
                                       S3Type.Reference type) {
        Mode.Method mode = rmm.getMode();
        if ( mode.isStatic() ) {
            if ( type.isSharedState() )
                return new NonVirtual( rmm, type);
            return new Static( rmm, type);
        }
        if ( rmm.isConstructor()  ||  mode.isPrivate() )
            return new NonVirtual( rmm, type);
	if (type.isInterface())
	    return new Interface( rmm, type);
        return new Virtual( rmm, type);
    }

    int number;
    int ctxNumber;

    private Selector.Method selector;
    private Mode.Method mode;

    static private final int[] NO_SYNTHETIC_PARAMS = new int[0];

    private final S3Type.Reference type_; // "type_" must match string in C code!

    /**
     * The code representations for this method (of various kinds).
     * *Not* final since the list may change over time.
     */
    protected Code code;

    public boolean toCompile;
    
    S3Method(RepositoryMember.Method info,
	     S3Type.Reference type) throws PragmaMayNotLink {
	mode = info.getMode();
	selector = info.getSelector();
	this.type_ = type;
	type.context_.addMethod(this);
        if ( null != info ) {
	    Bytecode org_bc = info.getCodeFragment();
	    if (org_bc == null)
		// These dummy S3ByteCode objects are needed to build
		// dispatch tables
		this.code = new S3ByteCode(this, null, 
                                           (char) 0, (char) 0,
					   null, null, 
					   getMode().isSynchronized());
	    else
		this.code = new S3ByteCode(this,
					   org_bc.getCode(),
					   org_bc.getMaxStack(),
					   org_bc.getMaxLocals(),
					   org_bc.getExceptionHandlers(),
					   org_bc.getAttributes(),
					   getMode().isSynchronized());
	}
	TypeName.Scalar[] thrown = info.getThrownExceptions();
	if (thrown.length > 0)
	    type.context_.methodThrows.put(this, thrown);
	if (selector.isConstructor())
	    type.context_.ctors.set(number);
	if (selector.isClassInit())
	    type.context_.clinits.set(number);
        if (getMode().isStatic() && !getDeclaringType().isSharedState())
            fail("Congratulations! You win an exciting instance of bug 484!");
    }

    S3Method(S3Method m, S3Type.Reference type) {
	mode = m.mode;
	selector = m.selector;
	type_ = type;
	type.context_.addMethod(this);
	if (type.context_.methodThrows.get(m) != null)
	    type.context_.methodThrows.put(this,
					  type.context_.methodThrows.get(m));
	if (m.isConstructor())
	    type.context_.ctors.set(number);
	if (m.isClassInit())
	    type.context_.clinits.set(number);
        if (getMode().isStatic() && !getDeclaringType().isSharedState())
            fail("Congratulations! You win an exciting instance of bug 484!");
    }

    public final int getUID() { 
	return number; 
    }
    public final int getCID() {
	return ctxNumber;
    }

    public int hashCode() {
	// should we use ctxNumber at all?
	return number ^ (ctxNumber << 13);
    }

    public Type.Compound getDeclaringType() {
        return type_;
    }

    public boolean isConstructor() {
        return type_.context_.ctors.get(number);
    }
    public boolean isClassInit() {
        return type_.context_.clinits.get(number);
    }
    public boolean isNonVirtual() {
        return false;
    }
    public boolean isVirtual() { return false; }
    public boolean isInterface() { return false; }

    public Selector.Method getSelector() {
        return selector;
    }

    public void setSelector(Selector.Method selector) {
	if (type_.context_.origSelectors.get(this) == null)
	    type_.context_.origSelectors.put(this, this.selector);
	this.selector = selector;
    }

    public Selector.Method getExternalSelector() {
	Selector.Method orig = (Selector.Method) 
	    type_.context_.origSelectors.get(this);
	return orig == null ? selector : orig;
    }
    public int getExternalNameIndex() {
	return getExternalSelector().getNameIndex();
    }

    public int[] getSyntheticParameterOffsets() {
	int[] ret = (int[]) type_.context_.syntheticParams.get(this);
	return ret == null ? NO_SYNTHETIC_PARAMS : ret;
    }

    public void markParameterAsSynthetic(int offset) {
	int[] syntheticParams = getSyntheticParameterOffsets();
	int[] newSP = new int[syntheticParams.length + 1];
	for (int i = 0; i < syntheticParams.length; i++) {
	    assert(syntheticParams[i] != offset);
	    newSP[i] = syntheticParams[i];
	}
	newSP[syntheticParams.length] = offset;
	type_.context_.syntheticParams.put(this, newSP);
    }

    public void appendSyntheticParameter(TypeName t) {
	Descriptor.Method desc = getSelector().getDescriptor();
	int oldArgCount = desc.getArgumentCount();
	ArrayList newArgs = new ArrayList(oldArgCount + 1);
	for (int i = 0; i < oldArgCount; i++) 
	    newArgs.add(desc.getArgumentType(i));
	newArgs.add(t);
	desc = Descriptor.Method.make(newArgs, desc.getType());
	UnboundSelector.Method ubs =
	    UnboundSelector.Method.make(getSelector().getNameIndex(),
					desc);
	setSelector(Selector.Method.make(ubs,
					 getSelector().getDefiningClass()));
	markParameterAsSynthetic(oldArgCount);
    }

    // FIXME haven't thought about how correct this could possibly be ...
    protected Type resolve(TypeName tn) throws LinkageException {
        return getDeclaringType().getContext().typeFor(tn);
    }

    public Mode.Method getMode() {
        return mode;
    }

    public Code getCode() {
	return code;
    }

    public S3ByteCode getByteCode() throws PragmaMayNotLink {
	return  (S3ByteCode) getCode(S3ByteCode.KIND);
    }

    public Code getCode(Code.Kind kind) {
        for (Code c = code; c != null; c = c.next)
            if (c.getKind().equals(kind))
                return c;
        return null;
    }

    /**
     * Replace the current default code object for this method with a
     * new and improved version.  Update all dispatch tables.
     **/
    protected abstract void installCompiledCode(Code cd);
    
    /**
     * Add a new piece of code for this method.  If code of the
     * same kind already exists, it is REPLACED.
     */
    public void addCode(Code cd) {
        for (Code curr = this.code; curr != null; curr = curr.next) {
            if (curr.getKind().equals(cd.getKind())) {
		curr.bang(cd);
		return;
            }
        }
	assert (cd.next == null);
	if (this.code != null)
	    installCompiledCode(cd);
	else
	    this.code = cd;
    }

    public Code removeCode(Code.Kind kind) {
        for (Code prev = null, cur = code;
	     cur != null;
	     prev = cur, cur = cur.next) {
            if (cur.getKind().equals(kind)) {
		if (prev == null)
		    code = cur.next;
		else
		    prev.next = cur.next;
		cur.next = null;
		return cur;
            }
        }
	return null;
    }

    public Type getReturnType() throws LinkageException {
        return resolve(getSelector().getDescriptor().getType());
    }

    private static final TypeName.Scalar[] NO_THROWS = new TypeName.Scalar[0];

    private TypeName.Scalar[] getThrownExceptions() {
	TypeName.Scalar[] ret = (TypeName.Scalar[])
	     type_.context_.methodThrows.get(this);
	return ret == null ? NO_THROWS : ret;
    }

    public Type.Class getThrownType(int i) throws LinkageException {
	    return (Type.Class) resolve(getThrownExceptions()[i]);
    }

    public int getThrownTypeCount() {
        return getThrownExceptions().length;
    }

    public Type getArgumentType(int i) throws LinkageException {
        return resolve(getSelector().getDescriptor().getArgumentType(i));
    }

    public int getArgumentCount() {
        return getSelector().getDescriptor().getArgumentCount();
    }

    public int getArgumentLength() {
        return getSelector().getDescriptor().getArgumentLength();
    }

    public String toString() {
        return /* getDeclaringType() + "::" + */ getSelector().toString();
    }

    public static class Virtual extends S3Method {
        Virtual( RepositoryMember.Method rmm, S3Type.Reference declarer) {
            super( rmm, declarer);
        }
	public boolean isVirtual() { return true; }

	/**
	 * Update dispatch tables in the type hierarchy.  Replace all
	 * occurrence of old with cd in virtual and interface dispatch
	 * tables.  This method recurses over subclasses and iterates
	 * over sibling classes.  Recursion stops when the method is
	 * overriden, or a leaf class is reached.
	 *
	 * @param bp
	 * The blueprint to update.  If bp not the declaring
	 * blueprint, we also update it's siblings.
	 * @param old
	 * The previous Code definition for this method
	 * @param cd
	 * The new Code definition for this method
	 * @param vtableOffset
	 * Offset of this method in vtables
	 * @param iftableOffset
	 * Offset of this method in interface tables, or -1 if it does
	 * not appear in <b>any</b> interface table.
	 * @param parentVTable
	 * A pointer to a parent vtable we have already updated, or
	 * null if we are starting with the declaring blueprint.  This
	 * code is written with the assumptionthat vtables may be
	 * shared between parents and children
	 **/
	private void updateBlueprint(Blueprint bp,
				     Code old, Code cd,
				     int vtableOffset, int iftableOffset,
				     Code[] parentVTable) {
	    do {
		Code[] vtable = ((S3Blueprint) bp).getVTable();
		if (vtable != parentVTable && vtable[vtableOffset] != old) {
		    bp = bp.nextSiblingBlueprint();
		    continue;
		}
		if (vtable != parentVTable) {
		    vtable[vtableOffset] = cd;
// 		    System.out.println("install compiled code " + cd
// 				       + " in vtable for " + bp);
		}
		Code[] ifTable = ((S3Blueprint) bp).getIFTable();
		if (ifTable != null && iftableOffset != -1
		    && ifTable.length > iftableOffset
		    && ifTable[iftableOffset] == old)
		    ifTable[iftableOffset] = cd;
		if (bp.firstChildBlueprint() != null)
		    updateBlueprint(bp.firstChildBlueprint(),
				    old, cd,
				    vtableOffset, iftableOffset,
				    vtable);
		bp = bp.nextSiblingBlueprint();
		// avoid updating siblings of declaring class
	    } while (bp != null && parentVTable != null);
	}

	protected void installCompiledCode(Code cd) {
	    Type t = getDeclaringType();
	    Domain d = t.getDomain();
	    Blueprint bp = d.blueprintFor(t);
	    DispatchBuilder db = ((S3Domain) d).getDispatchBuilder();
	    UnboundSelector.Method usel = getExternalSelector().getUnboundSelector();
	    int vtableOffset = ((S3Blueprint) bp).getVirtualMethodOffset(this);
	    int iftableOffset = db.getExistingIFTableOffset(usel);
	    updateBlueprint(bp, code, cd, vtableOffset, iftableOffset, null);
	    cd.next = code;
	    code = cd;
	}
    }

    public static class NonVirtual extends S3Method {
        NonVirtual( RepositoryMember.Method rmm, S3Type.Reference declarer) {
            super( rmm, declarer);
        }
        public boolean isNonVirtual() {
            return true;
        }

	protected void installCompiledCode(Code cd) {
	    Type t = getDeclaringType();
	    Domain d = t.getDomain();
	    Blueprint bp = d.blueprintFor(t);
	    Selector.Method sel = getSelector();
	    int offset = ((S3Blueprint) bp).getNonVTableOffsets().get(sel);
	    Code[] nvtable = ((S3Blueprint) bp).getNVTable();
	    nvtable[offset] = cd;
	    cd.next = code;
	    code = cd;
	}
    }

    public static class Static extends NonVirtual {
        Static( RepositoryMember.Method rmm, S3Type.Reference declarer) {
            super( rmm, declarer);
        }
    }

    public static class Interface extends S3Method {
        /** If I read the JLS properly, this is the only correct and possible mode
         * for an interface method--it has entropy zero.
         **/
        public static final Mode.Method mode = Mode.makeAbstractMethod();
        Interface( RepositoryMember.Method rmm, S3Type.Reference declarer) {
            super( rmm, declarer);
        }
	Interface(S3Method from, S3Type.Reference declarer) {
	    super(from, declarer);
	}
	public boolean isInterface() { return true; }

	protected void installCompiledCode(Code _) {
	    Executive.panic("someone compiled an interface method");
	}
    }

    /**
     * A multiple method occurs when a class inherits/implements multiple methods
     * of the same signature.
     * 
     * @author Chap Flack
     */
    // While implementing 1.2 support, I found that we need to return something meaningfull
    // for the getCode here. The problem was that we could find an invocation
    // to a Multiple and j2c expected to get a S3ByteCode object in the iFtable.
    // So -- we will randomly pick one... is this the right thing to do? Who knows. --jv
    
    public static class Multiple extends Interface {
        
        protected final S3Method[] ancestors;  // the inherited methods
       
        Multiple(S3Type.Reference declarer, Selector.Method selector, S3Method[] bag, int from, int len) {
        
            super(bag[from], declarer); // here pick a random one; see comment above--jv
            assert( 0 < len);
            ancestors = new S3Method [ len ];
            System.arraycopy( bag, from, ancestors, 0, len);
        }

        public void addCode(Code cd) {
            throw blooey();
        }


        /** Asking for the thrown types of a multiply-inherited method is asking
         * for trouble.  The JLS doesn't impose requirements on throws-clause
         * compatibility among methods with the same signature inherited by an 
         * interface.  (It does require, if the interface *declares* the method,
         * that the declaration be throws-compatible with every inherited method
         * that it overrides.)  So what would be the "throws clause" of an
         * S3Method.Multiple, which represents a group of methods with the same
         * signature, inherited by an interface and not overridden by a local
         * declaration?  The least arbitrary choice, I suppose, would be as much
         * of the union as would be permissible if the interface did override the
         *  methods; i.e., the union of their throws clauses, minus every
         *  *checked* exception incompatible with any of them.  The union should
         *  be computed without having supertypes subsume subtypes.  But I'm lazy
         *  enough to avoid implementing that computation here for the moment, in
         *  the hope that client code will not ask for thrown types of
         * S3Method.Multiple, but will have specific questions it can directly
         * ask by visiting the ancestors.  Of course if a client site ever turns
         * out to have to implement the full union/culling computation just
         *  described, that really belongs here. --- Chap<p>
         * 
         * I ran into such a case and it took me a bit of time to track the error. --jv
         */
        public Class getThrownType(int i) throws LinkageException {
              throw unimp();
        }

        public int getThrownTypeCount() {
            for (int i = 0; i < ancestors.length; i++)    // a cheap hack--make it work as
                if (ancestors[i].getThrownTypeCount() != 0)  // long as we don't have exceptions                  
                    throw unimp();
            return 0;
        }

	// Oh the glory of S3Method.Multiple!
	public Code removeCode(Code.Kind kind) {
	    for (int i = 0; i < ancestors.length; i++)
		ancestors[i].removeCode(kind);
	    return super.removeCode(kind);
	}

        public String toString() {
            Selector.Method sm = ancestors[0].getSelector();
            UnboundSelector.Method usm = sm.getUnboundSelector();
            StringBuffer sb;
            sb = new StringBuffer( getDeclaringType().toString()).append( "::{");
            sb = new StringBuffer( "{");
            sb.append( sm.getDefiningClass().toString());
            for ( int i = 1; i < ancestors.length; ++ i ) {
                sm = ancestors[i].getSelector();
                assert( sm.getUnboundSelector() == usm); // sanity check
                sb.append( ',').append( sm.getDefiningClass().toString());
            }
            sb.append( "}.").append( usm.toString());
            return sb.toString();
        }

        private static OVMError.Unimplemented unimp() {
            return new OVMError.Unimplemented();
        }
        
        private static OVMError.UnsupportedOperation blooey() {
            return new OVMError.UnsupportedOperation();
        }
    }
} 
