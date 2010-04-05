package s3.core.domain;

import ovm.core.domain.Blueprint;
import ovm.core.domain.ConstantPool;
import ovm.core.domain.Domain;
import ovm.core.domain.Field;
import ovm.core.domain.JavaDomain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Method;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Oop;
import ovm.core.domain.Type;
import ovm.core.domain.Method.Iterator;
import ovm.core.execution.CoreServicesAccess;
import ovm.core.repository.Attribute;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.Mode;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.RepositoryUtils;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.core.repository.Attribute.SourceFile;
import ovm.core.services.format.JavaFormat;
import ovm.core.services.io.BasicIO;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.memory.VM_Address;
import ovm.core.services.threads.OVMThread;
import ovm.services.bytecode.JVMConstants;
import ovm.services.monitors.Monitor;
import ovm.services.monitors.MonitorMapper;
import ovm.util.Arrays;
import ovm.util.Comparator;
import ovm.util.OVMError;
import ovm.util.OVMRuntimeException;
import s3.core.S3Base;
import s3.services.transactions.Transaction;
import s3.util.EphemeralBureau;
import s3.util.PragmaAtomic;
import s3.util.PragmaMayNotLink;
import s3.util.PragmaTransformCallsiteIR;
import s3.services.bytecode.ovmify.IRewriter;
import ovm.core.Executive;
import ovm.core.services.memory.MemoryManager;

/**
 * Represents the abstract {@link Type} implementation that should
 * be extended by all specific <code>S3Type</code> implementations.
 *
 * <p><b>FIXME:</b> Some of this stuff is Java user-domain specific. Should it
 * be in a separate JavaType class?? - DH
 **/
public abstract class S3Type extends S3Base implements Type {
    /**
     * The context for this type; this is a reference that allows us to
     * find the correct domain and thus the right repository bundles to
     * search in order to find the correct <code>RepositoryClass</code>
     * for this type. A type object can have one and only one context,
     * which connects to one and only one domain.
     * <p>
     * Package-private to expose S3TypeContext fields within to members.
     **/
    final S3TypeContext context_;
    /**
     * This array is used to caches the complete list of intefaces
     * implemented by this class. It remains null if the information is not
     * needed.
     **/
    Type.Interface[] allIfcs_;

    protected SharedStateClass sharedStateType;

    /**
     * Reference to the thread that is initializing this type. Used
     * by the class initialization protocol defined in the JVMS.
     */
    protected volatile OVMThread initializer = null;

    private static final boolean debug = false;
    // only turn on debugging for runtime, not build time, by setting in boot_

    static void boot_() {
        // debug = true; // - remove 'final' above when you want to do this
    }

    /**
     * Return the name of the class and its context in a slightly less wordy
     * way than toString().
     */
    public String toShortString() {
         return JavaFormat._.format(getUnrefinedName()) + " " + 
             context_.getDomain();
    }
    
    public String toString() {
        return "Type{" + JavaFormat._.format(getUnrefinedName()) 
                + "," + context_ + "}";
    }

    /**
     * Mark the given thread as the initializer of this type if initialization
     * is not already in progres.
     * @return the initializer thread, which is either <tt>t</tt> if no
     * initialization was in progress, or some other thread that is doing
     * the initialization.
     */
    public OVMThread setInitializerThread(OVMThread t) 
        throws ovm.core.services.memory.PragmaNoBarriers {
        if (initializer == null) {
            initializer = t;
        }
        return initializer;
    }

    /**
     * Constructor, which assigns a context to the new S3Type object
     * @param context this type's context
     **/
    public S3Type(S3TypeContext context) {
        assert context != null : "null context";
        context_ = context;
    }
    public Domain getDomain() {
        return context_.getDomain();
    }
    public Type getInnermostComponentType() {
        return null;
    }
    public int getDepth() {
        return 0;
    }
    public boolean isArray() {
        return false;
    }
    public boolean isScalar() {
        return false;
    }
    public Type.Context getContext() {
        return context_;
    }
    public Type.Scalar asScalar() {
        throw new OVMError.ClassCast(this +" is not a scalar");
    }
    public Type.Array asArray() {
        throw new OVMError.ClassCast(this +" is not an array");
    }
    public boolean isCompound() {
        return false;
    }
    public Type.Compound asCompound() {
        throw new OVMError.ClassCast(this +" is not a compound type");
    }
    public boolean isInterface() {
        return false;
    }
    public boolean isPrimitive() {
        return false;
    }
    public boolean isWidePrimitive() {
        return false;
    }

    // Overridden in Type.SharedStateClass only
    public Type getInstanceType() {
	return this;
    }
    /** If empty return S3Type.Interface.EMPTY_ARRAY */
    public Type.Interface[] getInterfaces() {
        return S3Type.Interface.EMPTY_ARRAY;
    }
    /** Default is no parent. */
    public Type.Class getSuperclass() {
        return null;
    }
    public Type getAncestor( TypeName tn) {
        for ( Type t = this; null != t; t = t.getSuperclass() )
            if ( t.getUnrefinedName() == tn )
                return t;
	return null;
        //throw new OVMError.ClassCast( this + " has no ancestor " + tn);
    }

    /** A root of the hierarchy has no super class. Note that there
     *  can be multiple roots (e.g. SharedSates do not have parents) */
    public boolean isRoot() {
        return getSuperclass() == null;
    }
    public boolean isSharedState() {
        return false;
    }
    public boolean isClass() {
        return false;
    }
    
    public Type.Class asClass() {
        throw new OVMError.ClassCast(this.getUnrefinedName().toString());
    }
    private boolean isSubtypeOfInterface(Type ifc) {
        Type.Interface[] ifcs = getAllInterfaces();
        for (int i = 0; i < ifcs.length; i++)
            if (ifcs[i] == ifc)
                return true;

        return false;
    }

    /**
     * Given two reference types - return the least common super
     * class. For interfaces and array types this is always the
     * root of the hierarchy in Java.
     * @param other
     */
    public Type.Class getLeastCommonSuperclass(Type other ) {
        if (this.isPrimitive() || other.isPrimitive())
            throw new OVMError.IllegalArgument("Not a reference type");
        if (this == other) return (Type.Class) this;
        if (this.isSubtypeOf(other))
            return (Type.Class) other;
        if (other.isSubtypeOf(this))
            return (Type.Class) this;
        if (this.isArray() || other.isArray())
            return getDomain().getHierarchyRoot();
        if (this.isInterface() || other.isInterface())
            return getDomain().getHierarchyRoot();
        
        return getLeastCommonSuperclass(other.getSuperclass());     
    }
    
    /**
     * Find the most-specific common supertypes of two types T1 and T2. 
     * The procedure is implemented as follows:<p>
     * <pre>
     * Case T1 == Primitive:
     *      if T2 == Primitive find the LCS according to the primitive 
     *      hierarchy rules otherwise return the empty set. 
     *      <NB: This is currently not implemented. We implicitly assume that 
     *           this method is only used for reference types.>
     * 
     * Case T1 & T2  == Scalar:
     *      Find their common superclass P, compute the set S of all interfaces
     *      implemented by both T1 and T2, and not by P.  Extend S with the 
     *      super class P, and eliminate from the set any class for which a 
     *      subclass belongs to the set.
     *      
     * Case T1 is Scalar and T2 is Array: the LCS is the hierarchy root.
     * 
     * Case T1 and T2 are array types of dimensions d1 and d2:
     *      let T1' and T2' be the arrays stripped by min(d1,d2) dimensions.
     *      Let S be the computed LCS of T1' and T2', then return
     *      the set S' that consists of elements of S with min(d1,d2)
     *      array dimensions added.
     * </pre>
     * @param other type to unify
     * @return an array of all supertypes of this and other s.t. no
     * array element is a subtype of any other 
     */
    public Type.Reference[] getLeastCommonSupertypes(Type other) {
        if (this == other) 
            return new Type.Reference[] { (Type.Reference) this};
        if (this.isPrimitive() || other.isPrimitive()) 
            return Type.EMPTY_REFERENCE_ARRAY;
        if (this.isSubtypeOf(other)) 
            return new Type.Reference[] {(Type.Reference) other};
        if (other.isSubtypeOf(this)) 
            return new Type.Reference[] {(Type.Reference) this};
        if ((this.isArray() && !other.isArray())  ||  
            (!this.isArray() && other.isArray()))
            return new Type.Reference[] { getDomain().getHierarchyRoot() };

        if (this.isArray() && other.isArray())
            return getArrayLeastCommonSupertypes(this, other);
        
        Type.Class parent = getLeastCommonSuperclass(other);
        Type.Interface[] ip = parent.getAllInterfaces();
        Type.Interface[] it = this.getAllInterfaces();
        Type.Interface[] io = other.getAllInterfaces();
        
        Type.Interface[] intersect = new Type.Interface[it.length];
        int cnt = 0;
        for (int i = 0; i < it.length; i++) {
            Type.Interface iti = it[i];
            boolean found = false;
            for (int j = 0; j < io.length; j++) {
                Type.Interface ito = io[j];
                if (iti == ito) {
                    found = true;
                    break;
                }
            }
            if (found)
                intersect[cnt++] = iti;
        }
        int retained =cnt;
        for (int i = 0; i < ip.length; i++) {
            Type.Interface ipi = ip[i];
            for (int j = 0; j < cnt; j++) {
                Type.Interface inter = intersect[j];
                if (inter == ipi) {
                    intersect[j] = null;
                    retained--;
                    break;
                }
            }
        }
          
        Type.Reference[] res = new Type.Reference[retained+1];
        int rcnt=0;
        res[rcnt++] = parent;
        for (int i = 0; i < intersect.length; i++) {
            Type.Reference reference = intersect[i];
            if (reference != null) 
                res[rcnt++] = reference;
        }
        int remain = res.length;
        for (int i = 0; i < res.length; i++) {
            Type.Reference t = res[i];
            for (int j = i + 1; j < res.length; j++) {
                Type.Reference o = res[j];
                if (o == null)
                    continue;
                if (t.isSubtypeOf(o)) {
                    res[j] = null;
                    remain--;
                } else if (o.isSubtypeOf(t)) {
                    res[i] = null;
                    remain--;
                    break;
                }
            }
        }
        if (remain != res.length) {
            Type.Reference[] tmp = new Type.Reference[remain];
            for (int i = 0, j=0; i < res.length; i++) 
                if (res[i]!=null) tmp[j++]=res[i];
            res=tmp;
        }
        return res;
    }
    
     private Type.Reference[] getArrayLeastCommonSupertypes(Type a, Type b) {
        int dim_to_strip = Math.min(a.getDepth(), b.getDepth());
        Type inner_a = a;
        Type inner_b = b;
        for (int i = 0; i < dim_to_strip; i++ ) {
            // the castsassert the expected type
            inner_a = ((Type.Array)inner_a).getComponentType(); 
            inner_b = ((Type.Array)inner_b).getComponentType(); 
        }
        Type.Reference[] tmp = 
            ((S3Type)inner_a).getLeastCommonSupertypes(inner_b);
	try {
	    if (tmp.length == 0)
		return new Type.Reference[] { getDomain().getHierarchyRoot() };
	    for (int i = 0; i < tmp.length; i++) 
		tmp[i] = makeArr(tmp[i], dim_to_strip, a.getContext());        
	    return tmp;
	} catch (LinkageException e) {
		return new Type.Reference[] { getDomain().getHierarchyRoot() };
	}
    }
    
    private static Type.Array makeArr(Type element, int dim, Type.Context env)
	throws LinkageException
    {
        assert dim > 0 : "dim <=0";
        Type innermost = element;
        if (element.isArray()) {
            innermost = element.getInnermostComponentType();
            dim += element.getDepth();
        }
        TypeName.Array tn = 
            TypeName.Array.make(innermost.getUnrefinedName(), dim);
	return (Type.Array) env.typeFor(tn);
    }

    /**
     * Return the <tt>java.lang.Class</tt> object for this type, creating it
     * if it does not yet exist.
     * @param csa the CoreServicesAccess object to use to allocate the Class
     * object
     * @param sharedState the sharedState object for this type
     **/
    public Oop getClassMirror(CoreServicesAccess csa, Oop sharedState) {
        return sharedState;
    }

    /**
     * Return the <tt>java.lang.Class</tt> object for this type, creating it
     * if it does not yet exist.
     **/
    public Oop getClassMirror() {
	return getSharedStateType().getSingleton();
    }

    public Type.Class getSharedStateType() {
	if (sharedStateType == null)
	    throw new Error("shared state of " + this + " accessed early");
	return sharedStateType;
    }

    /* The implementation assumes that the majority of subtype tests
     * are checking against a class (ie. other is not an interface).
     */
    public boolean isSubtypeOf(Type other) {
        Type current = this;
        while (current != null && current != other)
            current = current.getSuperclass();
        if (current == other)
            return true;
        if (other.isInterface())
            return isSubtypeOfInterface(other);
        else
            return false;
    }
//FIXME

    public Field getField(Selector.Field selector) {
        Type t = getAncestor(selector.getDefiningClass());
        return t == null ? null : t.getField(selector.getUnboundSelector());
    }
    
    public Field getField(UnboundSelector.Field usf) {
        throw new LinkageException.UndefinedMember(getUnrefinedName(),usf)
                  .unchecked();
    }
    public Field.Iterator fieldIterator() {
        return Field.Iterator.EMPTY_INSTANCE;
    }
    public ovm.core.domain.Field.Iterator localFieldIterator() {
        return Field.Iterator.EMPTY_INSTANCE;
    }
    public Method getMethod(Selector.Method selector) {
        Type t = getAncestor(selector.getDefiningClass());
        return t == null ? null : t.getMethod(selector.getUnboundSelector());
    }
    public Method getMethod(UnboundSelector.Method usm, boolean externalName) {
        throw new LinkageException.UndefinedMember(getUnrefinedName(),usm)
                  .unchecked();
    }
    public Method getMethod(UnboundSelector.Method usm) {
	return getMethod(usm, false);
    }
    public Method.Iterator methodIterator() {
        return Method.Iterator.EMPTY_INSTANCE;
    }
    public Iterator localMethodIterator() {
        return Method.Iterator.EMPTY_INSTANCE;
    }
    /**
     * Get <em>every</em> interface this type's class implements, directly
     * or indirectly; this means finding all of the superinterfaces of this
     * type's directly implemented interfaces, as well as all ancestors'
     * directly and indirectly implemented interfaces.
     * @return the type objects for every interface this type's class
     *         implements, either directly or indirectly
     **/
    public Type.Interface[] getAllInterfaces()
	// This code MUST run in the heap, hence it should be marked
	// as badalloc.
        // throws BCbadalloc
    {
        if (allIfcs_ != null)         return allIfcs_;
        // true of Object and primitive types
        if (getSuperclass() == null)  return allIfcs_ = S3Type.Interface.EMPTY_ARRAY;     

        Type.Interface[] direct = getInterfaces();
        Type.Interface[] parent_ifcs = getSuperclass().getAllInterfaces();
        
        // speed up the common case where we inherit our parent's  ifcs.
	// FIXME: This line demonstrates j2c's array pointer comparison bug.
        //if (direct == S3Type.Interface.EMPTY_ARRAY) return allIfcs_
        //= parent_ifcs;
	if (direct.length == 0) return allIfcs_ = parent_ifcs;
           
         Type.Interface[][] allSup = new Type.Interface[direct.length + 2][];
        // Find all interfaces implemented by our direct supertypes,  and keep track of 
        // the total number (this is an upper bound on the length of allIfcs)

        allSup[0] = direct;
        allSup[1] = parent_ifcs;
        int max = allSup[0].length + allSup[1].length;
        for (int i = 0; i < direct.length; i++) {
            allSup[i + 2] = direct[i].getAllInterfaces();
            max += allSup[i + 2].length;
        }

        int nIfcs = 0;
        Type.Interface[] tmp = new Type.Interface[max];
        for (int i = 0; i < allSup.length; i++) {
            // check whether each interface in allSup[i] has been
            // added to tmp[] by a previous supertype
            int checkTo = nIfcs;
            addOne : for (int j = 0; j < allSup[i].length; j++) {
                for (int k = 0; k < checkTo; k++)
                    if (allSup[i][j] == tmp[k])
                        continue addOne;
                tmp[nIfcs++] = allSup[i][j];
            }
        }
        allIfcs_ = new Type.Interface[nIfcs];
        for (int i = 0; i < nIfcs; i++)
            allIfcs_[i] = tmp[i];

        return allIfcs_;
    }

    //----------------------------------------------------------

    public static class Primitive extends S3Type implements Type.Primitive {

        final TypeName.Primitive name_;
	static final Mode.Class primMode
	    = Mode.Class.makeClassMode(JVMConstants.ACC_PUBLIC +
				       JVMConstants.ACC_FINAL);

        // do not create any other primitives than these
        Primitive(TypeName.Primitive name, S3TypeContext ctx) {
            super(ctx);
            name_ = name;
	    ctx.bootTypes.add(this);
        }
        public String toString() {
            return name_.toString();
        }
	// We use pointer-equality for comparison, but delegate
	// hashCode to our TypeName.  This code assumes that a
	// TypeName's hashCode never changes.
        public int hashCode() {
            return name_.hashCode();
        }
        public TypeName.Primitive getName() {
            return name_;
        }
        public boolean isPrimitive() {
            return true;
        }
        public boolean isWidePrimitive() {
            return false;
        }
        public TypeName getUnrefinedName() {
            return name_;
        }
        public State getLifecycleState() {
            return State.INITIALIZED;
        }
        public void setLifecycleState(State state) {
        }
        // FIXME this currently means java.lang.Object
        public boolean isRoot() {
            return false;
        }
        public Mode.Class getMode() {
            return primMode;
        }
    } // end of Type.Primitive

    public static class WidePrimitive
        extends Primitive
        implements Type.WidePrimitive {

        WidePrimitive(TypeName.WidePrimitive name, S3TypeContext ctx) {
            super(name, ctx);
        }
        public boolean isWidePrimitive() {
            return true;
        }
    }

    abstract static class Reference extends S3Type implements Type.Reference {

        protected S3Method[] methods = S3Method.EMPTY_ARRAY;
        // unnecessary here but added for simplicity:
        protected S3Field[] fields = S3Field.EMPTY_ARRAY;

        // volatile to allow correct reading/writing without synchronization
        protected volatile State state = State.LOADED;

        protected Reference(S3TypeContext env) {
            super(env);
        }

	// We use pointer-equality for comparison, but delegate
	// hashCode to our TypeName.  This code assumes that a
	// TypeName's hashCode never changes, and a RepositoryClass's
	// TypeName never changes.
        public int hashCode() {
            // ClassLoader puns can cause collisions.  If this becomes
            // a problem, we can use Type.Context.getUID().
            return getName().hashCode();
        }
   
        public TypeName getUnrefinedName() {
            return getName();
        }
        public State getLifecycleState() {
            return state;
        }
        public void setLifecycleState(State state) {
            this.state = state;
        }

        /**
         * Get a method implemented in this type (but not supertypes)
         * identified by some integer index. Both the method and the meaning
         * of the index is internal to the implementation. Terminology
         * ("local") borrowed from HotSpot.
         **/
        S3Method getLocalMethod(int index) {
            return methods[index];
        }
        /**
         * Get a field implemented in this type (but not supertypes)
         * identified by some integer index. Both the method and the meaning
         * of the index is internal to the implementation. 
         **/
        S3Field getLocalField(int index) {
            return fields[index];
        }
        int localMethodCount() {
            return methods.length;
        }
        int localFieldCount() {
            return fields.length;
        }
        public Method.Iterator methodIterator() {
            return new MethodIterator(this);
        }
        public Method.Iterator localMethodIterator() {
            return new LocalMethodIterator();
        }
        public Field.Iterator fieldIterator() {
            return new FieldIterator(this);
        }
        public Field.Iterator localFieldIterator() {
            return new LocalFieldIterator();
        }

        public boolean isCompound() {
            return true;
        }
        public Type.Compound asCompound() {
            return this;
        }
        private class LocalFieldIterator implements Field.Iterator {
            int pos = 0;
            public Field next() {
                return getLocalField(pos++);
            }
            public boolean hasNext() {
                return pos < fields.length;
            }
        }
        private class LocalMethodIterator implements Method.Iterator {
            private int pos_ = 0;
            public Method next() {
		return getLocalMethod(pos_++);
            }
            public boolean hasNext() {
                return pos_ < methods.length;
            }
        }
    } // End of Reference

    // keep in sync with FieldIterator since it's almost identical
    private static class MethodIterator implements Method.Iterator {
        private S3Type.Reference type_;
        private int nextix_;
        public MethodIterator(S3Type.Reference startType) {
            this.type_ = startType;
            this.nextix_ = 0; 
        }

        public Method next() {
            if (nextix_ < type_.localMethodCount()) {
                return type_.getLocalMethod(nextix_++);
            } else {
		nextix_ = 0;
		do type_ = (S3Type.Reference) type_.getSuperclass();
		while (type_.localMethodCount() == 0);
                return next();
            }
        }
        public boolean hasNext() {
            S3Type.Reference t = type_;
            int index = nextix_;
            while (t != null) {
                if (index < t.localMethodCount()) {
                    return true;
                }
                index = 0;
                t = (S3Type.Reference) t.getSuperclass();
            }
            return false;
        }
    }

    // keep in sync with MethodIterator since it's almost identical
    private static class FieldIterator implements Field.Iterator {
        private S3Type.Reference type_;
        private int nextix_;
        public FieldIterator(S3Type.Reference startType) {
            this.type_ = startType;
            this.nextix_ = 0;
        }

        public Field next() {
	    while (nextix_ >= type_.localFieldCount()) {
		type_ = (S3Type.Reference) type_.getSuperclass();
		nextix_ = 0;
		if (type_ == null) {
		    throw new OVMRuntimeException("no more fields");
		}
	    }
	    return type_.getLocalField(nextix_++);
	}

        public boolean hasNext() {
            S3Type.Reference t = type_;
            int index = nextix_;
            while (t != null) {
                if (index < t.localFieldCount()) {
                    return true;
                }
                index = 0;
                t = (S3Type.Reference) t.getSuperclass();
            }
            return false;
        }
    }

    static public final int[] EMPTY_INT_ARRAY   = new int[0];

    // XXX: should not be public
    abstract public static class Scalar
	extends Reference implements Type.Scalar
    {
	private final int sourceFileIndex;

        private final Type.Interface[] ifcs_;
	private final Mode.Class mode;
	private final TypeName.Scalar name;
	private final TypeName.Scalar outerName;
        S3Constants constants;

        public Scalar(RepositoryClass cls,
		      Type.Interface[] ifcs,
		      S3TypeContext env) {
            super(env);
	    mode = cls.getAccessMode();
	    if (this instanceof SharedStateClass)
		name = cls.getName().getGemeinsamTypeName();
	    else
		name = cls.getName();
	    outerName = cls.getOuter();
	    ifcs_ = ifcs;
	    Attribute[] attrs = cls.getAttributes();
	    int si = 0;
	    for (int i=0;i<attrs.length;i++)
		if (attrs[i] instanceof SourceFile) {
		    SourceFile att = (SourceFile) attrs[i];
		    si = att.getSourceFileNameIndex();
		    break;
		}
	    sourceFileIndex = si;

	    constants = new S3Constants(this, cls.getConstantPool());

	    RepositoryMember.Field[] rf = getRepositoryFields(cls);
            int count = rf.length;
            if ( count != 0 ) {
                fields = new S3Field [ count ];
		for (int i = 0; i < count; i++)
		    fields[i] = makeField(rf[i]);
            }
	    RepositoryMember.Method[] rm = getRepositoryMethods(cls);
            count = rm.length;
            if ( count > 0 ) {
                methods = new S3Method [ count ];
		for (int i = 0; i < count; i++)
		    methods[i] = S3Method.makeMethod(rm[i], this);
	    }
        }
	/**
	 * This constructor is used to initialize the shared-state
	 * types for primitive and array types.  These types have
	 * never supported any number of methods
	 **/
 	Scalar(TypeName.Scalar name, S3TypeContext env) {
 	    super(env);
	    this.name = name;
	    mode = null;
	    outerName = null;
 	    sourceFileIndex = 0;
 	    ifcs_ = S3Type.Interface.EMPTY_ARRAY;
 	}

	abstract int getLogicalEnd();

	public int getSourceFileNameIndex() { return sourceFileIndex; }

	public ConstantPool getConstantPool() { return constants; }

        public Type.Interface[] getInterfaces() {
            return ifcs_;
        }
        public Mode.Class getMode() {
            return mode;
        }
        public TypeName.Compound getName() {
            return name;
        }
        public TypeName.Compound getOuterName() {
            return outerName;
        }
        public Type.Scalar getOuterType() throws LinkageException {
	    return context_.typeFor(outerName).asScalar();
        }
        public boolean isScalar() {
            return true;
        }
        public Type.Scalar asScalar() {
            return this;
        }

	/**
	 * This is a partial implementation of Java bytecode
	 * verification as defined in JVMS 4.9.1 (passes 2 and 3).
	 * Missing features include:
	 * <ul>
	 * <li> <a href="https://www.ovmj.org/~mantis/bug_view_page.php?bug_id=836">
	 *      tests for explicit superclass</a>
	 * <li> <a href="https://www.ovmj.org/~mantis/bug_view_page.php?bug_id=837">
	 *      constant pool parsing</a>
	 * <li> tests for final method overriding
	 * <li> Possibly, other tests that are not explicitly
	 *      mentioned in JVMS.  The JVMS does not state whether
	 *      catch types are checked in pass 3 or pass 4 -- 4.9.1
	 *      does not mention catch types at all.  Who knows what
	 *      else we should check...
	 * <li> bytecode verification
	 * </ul>
	 *
	 * Implemented features include, most significantly, loading
	 * and typechecking each exception handler.  Without this step
	 * exception dispatch may not terminate.
	 **/
	public synchronized void verify()
	    throws LinkageException, PragmaMayNotLink
	{
	    if (state == State.ERRONEOUS) throw new LinkageException("access to invalid class " + getName());
	    if (state != State.LOADED) return;
	    try {
		if (getSuperclass() == null) {
		    if (this != getDomain().getHierarchyRoot() && !isSharedState())
			throw new LinkageException.Verification(getName() + " has no superclass");
		} else if (!isSharedState()) {
		    getSuperclass().verify();
		    if (getSuperclass().getMode().isFinal()) throw new LinkageException.Verification(getName()
			    + " extends final class");
		}
		if (isInterface())
		// I suppose we should check that all methods are
		// either public-abstract-instance or <clinit>
		return;

		Type throwable = ((JavaDomain) getDomain()).commonTypes().java_lang_Throwable;
		for (Method.Iterator it = this.localMethodIterator(); it.hasNext();) {
		    Method m = it.next();
		    if (m.getMode().isAbstract()) continue;
		    S3ByteCode c = m.getByteCode();
		    // Check that all caught types are Throwable.  As
		    // a side effect, this check loads caught
		    // exception into the current context, and prevent
		    // recursive exceptions inside dispatch
		    assert c != null: ("null bytecode verifying " + m +
				       " in " + this);
		    ExceptionHandler[] eh = c.getExceptionHandlers();
		    for (int i = 0; i < eh.length; i++) {
			Type t = getContext().typeFor(eh[i].getCatchTypeName());
//PARBEGIN
			if (t.asScalar().getName() == Transaction.ABORTED_EXCEPTION ||
				t.asScalar().getName() == Transaction.ED_ABORTED_EXCEPTION)    continue;
//PAREND
			if (!t.isSubtypeOf(throwable)) throw new LinkageException.Verification(m.getSelector()
				+ " catches non-throwable: " + t.getUnrefinedName());
		    }
		}

		if (!isSharedState()) getSharedStateType().verify();
	    } catch (LinkageException e) {
		this.setLifecycleState(State.ERRONEOUS);
		throw e;
	    }
	}

	S3Type.Scalar makeSharedStateType(RepositoryClass cls,
				       S3TypeContext env) {
	    if (sharedStateType != null)
		throw new Error("shared state created twice");
	    sharedStateType = new SharedStateClass(cls, this, env);
	    return sharedStateType;
	}
	
        // Overriden in SharedStateClass
        protected RepositoryMember.Method[] getRepositoryMethods(RepositoryClass cls) {
            if (!Transaction.the().transactionalMode())
        		return cls.getInstanceMethods();
            else
		return Transaction.the().initMethods(this, cls.getInstanceMethods(), null);
         }
        
        // Overriden in SharedStateClass
        protected RepositoryMember.Field[] getRepositoryFields(RepositoryClass cls) {
            return cls.getInstanceFields();
        }
	private S3Field makeField(RepositoryMember.Field rmf) {
	    TypeName tn = rmf.getDescriptor().getType();
	    S3Field f;

	    if ( ! tn.isPrimitive() ) {
		f = (S3Field)    EphemeralBureau.fieldFor( rmf, this);
		if ( null == f ) f = new S3Field.Reference( rmf, this);
	    }
	    else switch ( tn.getTypeTag() ) {
		case TypeCodes.BOOLEAN:
		    f = new S3Field.  Boolean( rmf, this); break; 
		case TypeCodes.BYTE:
		    f = new S3Field.     Byte( rmf, this); break; 
		case TypeCodes.SHORT:
		    f = new S3Field.    Short( rmf, this); break; 
		case TypeCodes.CHAR:
		    f = new S3Field.Character( rmf, this); break; 
		case TypeCodes.INT:
		    f = new S3Field.  Integer( rmf, this); break; 
		case TypeCodes.LONG:
		    f = new S3Field.     Long( rmf, this); break; 
		case TypeCodes.FLOAT:
		    f = new S3Field.    Float( rmf, this); break; 
		case TypeCodes.DOUBLE:
		    f = new S3Field.   Double( rmf, this); break;
		default:
		    throw failure( "No Field subtype for type " + tn); 
		}
	    return f;
        }

        public Method getMethod( UnboundSelector.Method usel,
				 boolean externalName ) {
	    for (int i = 0; i < methods.length; i++) {
		Selector.Method key = (externalName
				       ? methods[i].getExternalSelector()
				       : methods[i].getSelector());
		if (key.getUnboundSelector()  == usel)
		    return methods[i];
	    }
	    return null;
	}
		
        public Field getField(UnboundSelector.Field usel) {
            for (int i = 0; i < fields.length; i++)
		if (fields[i].getSelector().getUnboundSelector() == usel)
		    return fields[i];
            return null;
        }

        int createLayout(RepositoryMember.Field[] thefields) {
            Type.Class sup;
            int offset;

            if ( null != ( sup = getSuperclass() ) )
                offset = ((S3Type.Class)sup).getLogicalEnd();
            else
                offset = ObjectModel.getObjectModel().headerSkipBytes();  // FIXME 547

            for ( int i = 0; i < thefields.length; i++ ) {
                // The following line should be regarded with some awe at its majestic
                // subtlety.  It encodes major design choices about our object layouts,
                // such as padding every field to a word boundary, which assumption
                // is also built into the whole idea of descriptor.wordSize().
                // Note that field layout is being done here rather than in Blueprint.
                // This follows from a JV comment (and no strong objection) in a
                // meeting, that Blueprints differing in field layout for the same
                // Type are asking for trouble.  Later on, if we *want* trouble, we
                // can think about going there. FIXME 547
                // The obvious extension will be that a Field is associated (per Type)
                // with a small integer, that gets you an offset by indexing an array
                // in any appropriate Blueprint.  But tomorrow is another day.

                UnboundSelector.Field sel = thefields[i].getUnboundSelector();
		int size=sel.getDescriptor().wordSize();

		// align longs to a 2-WORD boundary
		if (size>1)
		    offset = (offset + MachineSizes.BYTES_IN_WORD*2 -1) & ~ (MachineSizes.BYTES_IN_WORD*2 -1);

		fields[i].offset = offset;
		offset += MachineSizes.BYTES_IN_WORD * size;
            }
            return offset;
        }
        
        /** This may exist, if not in exactly this form, at the completion of work
         *  on bug 417; it returns an opaque integer representing the field in
         *  question, which can be translated to an actual offset by any Blueprint
         *  representing this Type. The method is package private and intended for
         *  S3Field's use only.
         * <p />
         *  In this implementation, where offsets are constrained not to vary by
         *  Blueprint, the integer returned simply <em>is</em> the offset, and the
         *  translation by a Blueprint is identity. FIXME 547
         * @return an opaque integer representing the field.
         * It might be the offset.  Shhh.
         **/
        int bug417fieldID( Field f) {
	    return ((S3Field) f).getOffset();
        }
        
        /** This is a worse hack existing for now to support work on bug 417; it
         *  returns the offset of the field in question, without reference to any
         *  Blueprint, so the very existence of this method screams FIXME 547.
         **/
        int bug417fieldOffset( Field f) {
            return bug417fieldID( f);
        }
    } // End of Scalar

    public static class Interface extends Scalar implements Type.Interface {

        static final Type.Interface[] EMPTY_ARRAY = new S3Type.Interface[0];

        private S3Method[] flattenedMethods;

        Interface(RepositoryClass cls,
		  Type.Interface[] ifcs,
		  S3TypeContext env)
	    throws LinkageException
	{
            super(cls, ifcs, env);
	    // FIXME: It would be nice if all Type.Scalars had a
            // method list like flattenedMethods, but they don't.

            // java.lang.reflect code needs to use
            // localMethodIterator(), so we need to keep the array of
            // declared methods around.  
            // methods = null;
            flattenedMethods = collectMethods(cls); // side effect: fills in ifcs_
        }
        /**  The superclass of all interfaces is java.lang.Object in Java
         **/
        public Type.Class getSuperclass() {
            return getContext().getDomain().getHierarchyRoot();
        }

	protected int getLogicalEnd() {
	    return ((S3Type.Class) getSuperclass()).getLogicalEnd();
	}

        public boolean isInterface() {
            return true;
        }

        // as long as we insist on early aggressive gemeinsamifizieren, these
        // methods should be safely dead.  If we go to a lazier strategy, just
        // forward to the shared state.
        public Field getField(UnboundSelector.Field selector) {
            throw failure( "Interface-fieldref nicht gemeinsamifiziert"); // can you think of a dumber error message?
        }
        S3Field getLocalField(int index) {
            throw failure( "Interface-fieldref nicht gemeinsamifiziert");
        }
        
        private S3Method[] collectMethods(RepositoryClass cls) throws LinkageException {
            RepositoryMember.Method[] rmms = getRepositoryMethods(cls);
            int localCount = rmms.length;
            int totalCount = localCount;
            S3TypeContext tc = context_;
          
        	
            // the superclass constructor has allocated, but not populated, our
            // array of superinterfaces. populate it here as a side effect while
            // walking it to estimate the bag size needed to hold all inherited
            // methods. that's an upper bound on the size of the set that will
            // result from culling duplicates from the bag.
            
           Type.Interface[] ifcs = this.getInterfaces();
            
            // for each superinterface, add its method count to the estimate.
            // by construction, each interface's method array includes all of its
            // (declared and inherited) methods, so only direct superinterfaces
            // need to be consulted. a slot in a parent's method array could
            // contain an S3Method.Multiple for a multiply-inherited method, but
            // by construction Multiple is flattened - it will not contain other
            // Multiples as entries. therefore we need only one step of
            // flattening here to preserve that property: when a method inherited
            // from a direct superinterface is a Multiple, we just add the
            // *contents* of that Multiple to our bag.
            for ( int k = ifcs.length; k --> 0; ) {
                S3Type.Interface ti = (S3Type.Interface) ifcs[k];
                S3Method[] inherited = ti.flattenedMethods;
                for ( int j = inherited.length; j --> 0; ) {
                    S3Method m = inherited [ j ];
                    totalCount += m instanceof S3Method.Multiple
                                ? ((S3Method.Multiple)m).ancestors.length
                                : 1;
                }
            }
            S3Method[] bag;
            
            // now allocate the bag and put the inherited methods in it.
            // first inherit the public instance methods of the class hierarchy
            // root, only if there is no superinterface (JLS ?9.2 bullet 3).
            if ( ifcs.length == 0 ) {
                S3Method[] baseMethods =
                    ((S3Type.Reference)getDomain().getHierarchyRoot()).methods;
                baseMethods = restrictToPublicInstance( baseMethods);
                if ( 0 == totalCount )
                    return baseMethods;
                totalCount += baseMethods.length;
                bag = new S3Method [ totalCount ];
                System.arraycopy( baseMethods, 0,
                                  bag, localCount, baseMethods.length);
            } else
                bag = new S3Method [ totalCount ];
            
            // now methods inherited from superinterfaces, if any
            for ( int k = ifcs.length, j = totalCount; k --> 0; ) {
                S3Type.Interface ti = (S3Type.Interface) ifcs [ k ];
                S3Method[] inherited = ti.flattenedMethods;
                for ( int i = inherited.length; i --> 0; ) {
                    S3Method m = inherited [ i ];
                    if ( m instanceof S3Method.Multiple ) {
                        S3Method[] ancestors = ((S3Method.Multiple)m).ancestors;
                        j -= ancestors.length;
                        System.arraycopy( ancestors, 0, bag, j, ancestors.length);
                    }
                    else
                        bag [ --j ] = m;
                }
            }
            
            // now put the locally declared methods in the bag
            
	    for ( int k = localCount; k --> 0; )
		bag [ k ] = methods [ k ];

            // and sort the bag
            
            Comparator c = new Comparator() {
                public int compare( Object a, Object b) {
                    Method ma = (Method)a, mb = (Method)b;
                    
                    // Methods should be interned, only equal if they're ==
                    
                    if ( ma == mb  &&  null != ma ) // Comparator contract
                        return 0;                   // requires NPE if arg is null
                    
                    // Major sort order: the unbound selector
                    
                    Selector.Method
                        sa = ma.getSelector(),
                        sb = mb.getSelector();
                    UnboundSelector.Method
                        usa = sa.getUnboundSelector(),
                        usb = sb.getUnboundSelector();
                    int rslt = usa.toString().compareTo( usb.toString());
                    if ( 0 != rslt )
                        return rslt;
                    
                    // Within unbound selector: defining class
                    
                    TypeName.Compound
                        dca = sa.getDefiningClass(),
                        dcb = sb.getDefiningClass();
                    
                    // local method precedes anything inherited
                    if ( getName() == dca ) return -1;
                    if ( getName() == dcb ) return  1;
                    // they can't both be local, they'd be ==
                    
                    // if we were not flattening Method.Multiple while populating
                    // the bag, here would be the place to ensure they sort ahead
                    // of non-Multiples. but there won't be any in the bag,
                    // so there.
                    
                    // otherwise, natural order on defining class name
                    rslt = dca.toString().compareTo( dcb.toString());
                    assert( 0 != rslt); // us and dc both equal but Methods !=?
                    return rslt;
                }
            };
            
            Arrays.sort( bag, c);
            
            // now process the bag, eliminating duplicates, and collecting
            // multiply-inherited methods (which are now consecutive).
            
            UnboundSelector.Method mrs = null;
            Method mrm = null;
            boolean skip = false;
            
            int lead, mid, follow;
            for ( lead = 0, mid = 0, follow = 0; lead < bag.length; ++ lead) {
                S3Method m = bag [ lead ];
                if ( m == mrm )
                    continue;
                mrm = m;
                Selector.Method sm = m.getSelector();
                UnboundSelector.Method usm = sm.getUnboundSelector();
                if ( usm == mrs ) {
                    if ( skip )
                        continue;
                    bag [ mid++ ] = m; // assume flat bag (m cannot be Multiple)
                    continue;
                }
                // encountered a new selector. from follow to mid lie n >=1 Method
                // objects for the previous selector. deal with them, leaving
                // follow advanced and mid == follow. The only exception is the
                // first new selector encountered; n == 0 then.
                int n = mid - follow;
                if ( 1 < n ) {
                    Object r = MemoryPolicy.the().enterMetaDataArea( getContext());
                    try {
			bag[follow] = new S3Method.Multiple(this, Selector.Method.make(mrs, getName()), bag, follow, n);
                    } finally { MemoryPolicy.the().leave( r); }
                }
                if ( 0 != n )   // skip this, the first time
                    mid = ++ follow;
                // this begins processing the new selector, by storing it at
                // follow (== mid) and advancing mid. if this is a locally
                // method (that's why they sort ahead of others), just set skip
                // so any inherited entries will be ignored. otherwise, mid will
                // be advanced for every entry with the same selector, leaving
                // n >=1 inherited Method objects to be dealt with as above.
                mrs = usm;
                bag [ mid++ ] = m;
                skip = this == m.getDeclaringType();
            }
            int n = mid - follow;
            assert( 0 < n); // can't exit loop without something to do
            if ( 1 < n ) {
                Object r = MemoryPolicy.the().enterMetaDataArea( getContext());
                try {
		    bag[follow] = new S3Method.Multiple(this, Selector.Method.make(mrs, getName()), bag, follow, n);
                } finally {
                    MemoryPolicy.the().leave( r);
                }
            }
            ++ follow;
            
            // there, wasn't that easy?
            
//            if ( bag.length == follow )       metaDataArea complicates this
//                return bag;                   optimization
            S3Method[] rslt;
            Object r = MemoryPolicy.the().enterMetaDataArea( getContext());
            try {
                rslt = new S3Method [ follow ];
            } finally { MemoryPolicy.the().leave( r); }
            System.arraycopy( bag, 0, rslt, 0, follow);
            return rslt;
        }
        
        private S3Method[] restrictToPublicInstance( S3Method[] _methods) {
            int src = 0, dst = 0;
            S3Method m;
            Mode.Method mode;
            for ( ; src < _methods.length ; ++ src ) {
                m = _methods[src];
                if ( m.isConstructor() )
                    continue;
                mode = m.getMode();
                if ( mode.isStatic()  ||  ! mode.isPublic() )
                    continue;
                ++dst;
            }
            S3Method[] rslt = new S3Method[dst];
            while ( src --> 0 ) {
                m = _methods[src];
                if ( m.isConstructor() )
                    continue;
                mode = m.getMode();
                if ( mode.isStatic()  ||  ! mode.isPublic() )
                    continue;
                rslt[--dst] = m;
            }
            return rslt;
        }

        public Method getMethod(UnboundSelector.Method usm,
				boolean externalName) {
            for ( int i = flattenedMethods.length; i --> 0; ) {
		Selector.Method key = (externalName
				       ? flattenedMethods[i].getExternalSelector()
				       : flattenedMethods[i].getSelector());
                if ( usm != key.getUnboundSelector() )
                    continue;
                return flattenedMethods[i];
            }
            return null; // what, we don't believe in exceptions?
        }

        public Iterator methodIterator() {
            final Method[] meth = flattenedMethods;
            return new Method.Iterator() {
                private int i = 0;
                public boolean hasNext() {
                    return i < meth.length;
                }
                public Method next() {
                    return meth [ i++ ];
                }
            };
        }

    } // End of Interface

    public static class Class extends S3Type.Scalar implements Type.Class {

        private final Type.Class super_;

 
        private final int logicalEnd_;
        
        protected int getLogicalEnd() {
            return logicalEnd_; 
        }

        public Class(
            RepositoryClass cl,
            Type.Class supercl,
	    Type.Interface[] ifcs,
            S3TypeContext env) {

            super(cl, ifcs, env);
            super_ = supercl;
            logicalEnd_ = createLayout( getRepositoryFields(cl));
        }
        public Type.Class getSuperclass() {
            return super_;
        }
        public boolean isClass() {
            return true;
        }
	public Type.Class asClass() {
	    return this;
	}
        
        public Oop getSingleton() { return null; }

    } // End of Class

    /**
     * These classes are not put directly into the domain but are
     * reachable from the classes describing instances.
     **/
    public static class SharedStateClass extends S3Type.Scalar implements Type.Class {
        private Type instanceType_;
        private Oop singleton_;
	private int logicalEnd_;

        SharedStateClass(
            RepositoryClass cls,
            Type.Scalar instanceType,
            S3TypeContext env) {

            super(cls, S3Type.Interface.EMPTY_ARRAY, env);
	    sharedStateType = this;
            instanceType_ = instanceType;
            constants = ((S3Type.Scalar) instanceType).constants;
            logicalEnd_ = createLayout( getRepositoryFields(cls));
        }

	SharedStateClass(Type instanceType, S3TypeContext env) {
	    super(instanceType.getUnrefinedName().getGemeinsamTypeName(), env);
	    sharedStateType = this;
	    instanceType_ = instanceType;
	    logicalEnd_ = ((S3Type.Class) getSuperclass()).getLogicalEnd();
	}

	protected int getLogicalEnd() { return logicalEnd_; }

	public Type getInstanceType() {
	    return instanceType_;
	}

        /**
         * Shared state classes are not subclasses of anything.
         **/
        public Type.Class getSuperclass() {
            return getContext().getDomain().getMetaClass();
        }
        public boolean isSharedState() {
            return true;
        }
    
/*
        public Field getField(UnboundSelector.Field usel) {
	    Field answer = super.getField(usel);   

	    // if (true) return answer;
	    if (answer != null) return answer;

	    Type.Class superclass = instanceType_.getSuperclass();
	    if (superclass == null) return null;

	    return superclass.getSharedStateType().getField(usel);
	}
*/
        
        protected RepositoryMember.Method[] getRepositoryMethods(RepositoryClass cls) {
            if (!Transaction.the().transactionalMode())
		return cls.getStaticMethods();
            else
		return Transaction.the().initMethods(this, cls.getStaticMethods(), null);

        }
        protected RepositoryMember.Field[] getRepositoryFields(RepositoryClass cls) {
            return cls.getStaticFields();
        }

        public Type.Class asClass() {
            return this;
        }
        
        public Oop getSingleton() {
	    if (singleton_ == null) {
		CoreServicesAccess csa = getDomain().getCoreServicesAccess();
		Blueprint bp;
		bp = getDomain().blueprintFor(this);
		Object r = MemoryPolicy.the().enterMetaDataArea(getContext());
		try {
		    singleton_ = internalSharedStateOop(csa, bp);
		    if (!isBuildTime() && MemoryManager.the().shouldPinCrazily())
			MemoryManager.the().pin(singleton_);
		} finally {
		    MemoryPolicy.the().leave(r);
		}

		// FIXME: We are allocating shared-state objects for
		// types that fail verification.  Is that a good
		// thing?  Does it really matter?
		try {
		    Type instType = getInstanceType();
		    if (instType.isScalar()) {
			boolean wasLoaded = (instType.getLifecycleState() == State.LOADED);
			((Type.Scalar) instType).verify();
			instType.setLifecycleState(State.PREPARED);
		    }
		}
		catch (LinkageException e) {
		    BasicIO.err.println("verify of " + this + " failed: " + e);
		}
	    }
	    return singleton_;
	}
        
	/*
	 * This method should be called exactly once on every shared state
	 * blueprint in the system.  It allocates the corresponding
	 * shared-state object
	 */
	private static Oop internalSharedStateOop(CoreServicesAccess csa,
						  Blueprint bp)
	    throws BCnew
	{
	    return VM_Address
		.fromObject( new SharedStatePlaceHolder(), bp)
		.asOop();
	}
        
	private static class SharedStatePlaceHolder { }

	private static final Selector.Method allocateObject
	    = RepositoryUtils.methodSelectorFor
	    ("Lovm/core/execution/CoreServicesAccess;",
	     "allocateObject:(Lovm/core/domain/Blueprint$Scalar;)"
	     + "Lovm/core/domain/Oop;");
	/**
	 * Call CoreServicesAccess.allocateObject()
	 */
	private static class BCnew extends PragmaTransformCallsiteIR
	    implements JVMConstants.Opcodes
	{
	    static {
		register(BCnew.class.getName(),
			 new Rewriter() {
			     protected boolean rewrite() {
				 cursor.addINVOKEVIRTUAL(allocateObject);
				 return true;
			     }
			 });
	    }
	}
    } // end of SharedStateClass

    public static class Array extends Reference implements Type.Array {

        private final Type innermostType_;
        private final int dimension_;
        private final Type componentType_;

	public int getSourceFileNameIndex() { return 0; }

        public Type.Interface[] getAllInterfaces() {
            if (allIfcs_ != null)
                return allIfcs_;
            return allIfcs_ = getContext().getDomain().getArrayInterfaces();
        }
        
        /* The innermostElemType should not be an Array type. Also:
         * innermostElemType == componentType.getComponentType(). ... getComponentType()
         * where getComponentType() is repeated dimension - 1 times.  */
        Array(Type innermostElemType, int dimension, Type componentType, S3TypeContext env) {
            super(env);
            assert(!innermostElemType.isArray()); 
            innermostType_ = innermostElemType;
            dimension_ = dimension;
            componentType_ = componentType;
	    if (env.bootTypes != null)
		env.bootTypes.add(this);
	    else
		sharedStateType = new SharedStateClass(this, env);
        }

	// We use pointer-equality for comparison, but delegate hashCode to our TypeName. 
        public int hashCode() {
            return innermostType_.hashCode() + dimension_; 
        }
        public Type getComponentType() {
            return componentType_;
        }

        public Type getInnermostComponentType() {
            return innermostType_;
        }

        public TypeName.Compound getName() {
            return TypeName.Array.make(innermostType_.getUnrefinedName(), dimension_);
        }
        public Mode.Class getMode() {
            // array access mode == access mode of component type
            int m = innermostType_.getMode().getMode();
	    m &= ~JVMConstants.ACC_INTERFACE; // must clear
	    m |= JVMConstants.ACC_FINAL;      // must set
	    m &= ~JVMConstants.ACC_ABSTRACT;  // must clear as FINAL set
            // anything else is undefined
	    return Mode.Class.makeClassMode(m);
        }
        public Type.Interface[] getInterfaces() {
            return getContext().getDomain().getArrayInterfaces();
        }
        public Type.Class getSuperclass() {
            return getContext().getDomain().getHierarchyRoot();
        }
        public Method getMethod(UnboundSelector.Method selector,
				boolean externalName) {
            // FIXME the S3Method for Object.clone can't be shared with arrays
            // since the access mode and thrown exceptions differ
            return getSuperclass().getMethod(selector, externalName);
            // return (Method)methods_.get(selector);
        }
        public boolean isArray() {
            return true;
        }
        public Type.Array asArray() {
            return this;
        }
        public int getDepth() {
            return dimension_;
        }
        public boolean isSubtypeOf(Type other) {

            if (super.isSubtypeOf(other))
                return true;

            if (other.isArray()) {
                Type thisComp = getInnermostComponentType();
                Type otherComp = other.getInnermostComponentType();
                int thisDim = getDepth();
                int otherDim = other.getDepth();

                if (thisComp.isSubtypeOf(otherComp)) {
                    if (thisDim == otherDim) {
                        return true;
                    } else if (thisDim > otherDim) {
                        return otherComp.isRoot();
                    }
                } else if (thisComp.isPrimitive()) {
                    return otherComp.isRoot() && (thisDim > otherDim);
                }
            }
            return false;
        }
        public Method.Iterator methodIterator() {
            return getSuperclass().methodIterator();
        }

    } //  End of Array
}
