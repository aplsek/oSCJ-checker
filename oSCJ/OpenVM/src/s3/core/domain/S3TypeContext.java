package s3.core.domain;
import ovm.core.Executive;
import ovm.core.OVMBase;
import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.Type;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.TypeName;
import ovm.core.services.memory.MemoryPolicy;
import ovm.services.bytecode.reader.Parser;
import ovm.util.ArrayList;
import ovm.util.ByteBuffer;
import ovm.util.Collections;
import ovm.util.IdentityHashMap;
import ovm.util.Iterator;
import ovm.util.Map;
import ovm.util.ReadSafeHashMap;
import s3.services.transactions.Transaction;
import ovm.core.services.memory.VM_Area;
import ovm.util.HashMap;
import ovm.util.BitSet;
import ovm.core.services.io.BasicIO;
import ovm.core.execution.Native;

public class S3TypeContext extends OVMBase implements Type.Context {

    public static final boolean REFLECTION_DEBUGGING = false;

    private final S3Domain domain;
    private Type.Loader loader;
    private final int id;

    // Maps a TypeName n to a Type t if typeFor(n) has successfully
    // returned t.
    private ReadSafeHashMap initiatedTypes = new ReadSafeHashMap(2047);

    // Maps a TypeName to either a Type (if it has been defined), or
    // Boolean.FALSE (if we are currently resolving its ancestors)
    // FIXME: defineType() synchronizes on this variable.  Is there a
    // potential for deadlock when two loaders delegate cyclically?
    private Map definedTypes = new IdentityHashMap(2047);

    private boolean frozen = false;

    // Why are we using an OVMIR parser rather than a plain bytecode
    // parser?
//     private static final Parser parser = (ovm
// 					  .services
// 					  .bytecode
// 					  .reader
// 					  .Services
// 					  .getServices()
// 					  .getOVMIRParserFactory()
// 					  .makeParser());

    // Bypass package.Services stuff, and create the object we actually
    // want!
    private static Parser parser = 
	new s3.services.bytecode.reader.S3Parser();
    /**
     * Maps {@link S3Field}s to constant values.
     * @see S3Field#getConstantValue
     **/
    HashMap constantValues = new HashMap();

    /**
     * The set of methods that are constructors.  Indexed by
     * {@link S3Method#number}.
     **/
    BitSet ctors = new BitSet();
    /**
     * The set of &lt;clinit&gt; methods.  Indexed by
     * {@link S3Method#number}.
     **/
    BitSet clinits = new BitSet();

    /**
     * Maps {#link S3Method}s to non-empty throws clauses
     * ({@link ovm.core.repository.TypeName.Scalar}[]).
     * @see S3Method#getThrownType
     **/
    HashMap methodThrows = new HashMap();

    /**
     * The source-level selector for every method that has been
     * renamed.
     * @see S3Method#setSelector
     * @see S3Method#getExternalSelector
     **/
    HashMap origSelectors = new HashMap();

    /**
     * The offsets of synthetic parameters within the argument lists
     * of all methods that take synthetic parameters
     * @see S3Method#getSyntheticParameterOffsets
     * @see S3Method#markParameterAsSynthetic
     **/
    HashMap syntheticParams = new HashMap();
    
    public S3TypeContext(S3Domain dom) {
	this(dom, false);
    }

    public S3TypeContext(S3Domain dom, boolean root) {
        domain = dom;
        id = S3DomainDirectory.registerContext( this);
	if (root)
	    bootTypes = new ArrayList();
    }

    private int blueprintCount;
    private int methodCount;

    synchronized void addBlueprint(S3Blueprint bp) {
	bp.number = (char) blueprintCount++;
	bp.ctxNumber = (char) id;
    }

    synchronized void addMethod(S3Method meth) {
	meth.number = methodCount++;
	meth.ctxNumber = id;
    }
    
    public int getBlueprintCount()     { return blueprintCount; }
    public int getMethodCount()        { return methodCount;  }    
    public Domain getDomain()          { return domain;    }
    public int getUID()                { return id;        }
    // FIXME: this isn't part of the interface
    public Map getTypes()              { return definedTypes; }

    public Type.Loader getLoader()     { return loader; }
    public void setLoader(Type.Loader loader) {
	this.loader = loader;
    }

    /** Return the type matching the argument, throw a fatal exception if not found. 
     */
    public Type typeForKnown(TypeName name) {
        try {  return typeFor(name); }
        catch (LinkageException e) { throw e.fatal(); }
    }

    public Type typeFor(TypeName name) throws LinkageException {
	Type ret = (Type) initiatedTypes.get(name);
	if (ret != null)
	    return ret;
	Object r = MemoryPolicy.the().enterMetaDataArea(this);
	try { 
	    Object _ret = definedTypes.get(name);
	    // FIXME: Should probably store current thread, rather
	    // than FALSE, and use wait/notify when another thread
	    // tries to load
	    if (_ret == Boolean.FALSE)
		throw new LinkageException.CyclicInheritance(name.asScalar());

	    if (name.isGemeinsam()) {
		Type t = typeFor(name.asScalar().getInstanceTypeName());
		ret = t.getSharedStateType();
	    } else if (name.isScalar()) {
		if (loader == null) { // no dynamic loading possible here
		    assert _ret == null: ("type " + name +
					  " defined but not initiated");
// 		    BasicIO.err.println("can't find " + name +
// 					" from " + this);
		    throw new LinkageException.NoClassDef(name);
		}
		// in RTSJ-1.1 loadClass is called from the MemoryArea
		// where the classloader is defined, this is also the
		// MetaDataArea of the corresponding Context.
		ret = loader.loadType(name.asScalar());
		if (ret == null) {
		    // initiatedTypes.put(name, ERROR_TYPE)?
		    throw new LinkageException.NoClassDef(name);
		}
	    } else if (name.isArray()) {
		TypeName.Array aname = name.asArray();
		Type comp = typeFor(aname.getInnermostComponentTypeName());
		Type.Context ctx = comp.getContext();
		ret = ((S3TypeContext) ctx).makeArrayType(comp, aname);
	    } else {
		assert(name.isPrimitive());
		S3TypeContext sys = (S3TypeContext)  domain.getSystemTypeContext();
		ret = (Type) sys.definedTypes.get(name);
		assert(ret != null);
	    }
	    initiate(name, ret);
	} finally { MemoryPolicy.the().leave(r); }

	return ret;
    }

    private void initiate(TypeName n, Type t) throws LinkageException {
	synchronized (initiatedTypes) {
	    Type existing = (Type) initiatedTypes.get(n);
	    if (existing != null && existing != t)
		throw new LinkageException("duplicate class definition: "
					   + t);
	    else if (existing == null)
		initiatedTypes.put(n, t);
	}
    }

    /**
     * This is the executive domain equivalent of
     * ClassLoader.findLoadedClass.  JavaDoc claims that
     * findLoadedClass will return null unless we are an initiating
     * loader for this name.  Experiments with jdk-1.4.2 indicate that
     * it also find classes that are defined but not used locally.
     * (Classes that we are the defining loader, but not an initiating
     * loader for.)  We do the same, but initiate loading as well.
     * <p>
     * Remember, definedTypes contains Boolean.FALSE while supertypes
     * are loaded
     **/
    public Type loadedTypeFor(TypeName name) {
	Type ret = (Type) initiatedTypes.get(name);
	if (ret == null && definedTypes.get(name) instanceof Type)
	    // Oops, we're an initiating loader now
	    try { return typeFor(name); }
	    catch (LinkageException e) { Executive.panicOnException(e); }
	return ret;
    }

    private Type.Array makeArrayType(Type innerMost, TypeName.Array name)
	throws LinkageException
    {
	Type inner = innerMost;
	TypeName innerName = inner.getUnrefinedName();
	int depth = 1;
	while (innerName != name) {
	    TypeName.Array outerName = TypeName.Array.make(innerName, 1);
	    synchronized (definedTypes) {
		Type.Array outer = (Type.Array) definedTypes.get(outerName);
		if (outer == null) {
		    if (frozen) {
			throw new LinkageException.DomainFrozen(outerName);
		    }
		    outer = new S3Type.Array(innerMost, depth, inner, this);
		    definedTypes.put(outerName, outer);
		    
		    if ( (REFLECTION_DEBUGGING || true) && !OVMBase.isBuildTime()) { // this is safe to do since we do not have interpreter anymore
		      Native.print_string("makeArrayType called for type name "+name.toString()+", the system will crash. try_to_add_class: ");
		      Native.print_string(outerName.toString()+"\n");
		    }
                    domain.newTypeHook(outer);
		    if (bootTypes == null)
			domain.newTypeHook(outer.getSharedStateType());
		}
		inner = outer;
		innerName = outerName;
		depth++;
	    }
	}
	return inner.asArray();
    }

    private ArrayList bootRC = null;
    ArrayList bootTypes;
    
    /**
     * defineType implements JVMspec section 5.3.5, and performs the
     * work of ClassLoader.defineClass().  The LinkageExceptions this
     * method throws are can be converted to Java LinkageErrors
     * suitable for return from defineClass.
     *
     * Bootstrapping is tricky because we must define
     * Ljava/lang/Object and Ljava/lang/Class before we can create
     * Gjava/lang/Object, but in general we must create a shared state
     * type before we can define the corresponding instance type.
     **/
    public Type.Scalar defineType(TypeName.Scalar name, ByteBuffer bytes)
	throws LinkageException
    {
	//System.err.println("defining " + name);
	if (frozen || parser == null)
	    throw new LinkageException.DomainFrozen(name);
	synchronized (definedTypes) {
	    Object r = MemoryPolicy.the().enterMetaDataArea(this);
	    try {
		RepositoryClass rclass;
		rclass = parser.parse(bytes, bytes.remaining());
		if (name != null && rclass.getName() != name) {
		    throw new LinkageException.NoClassDef("bad classfile: expected "
							  + name + " but found "
							  + rclass.getName());
		}
		// FIXME: check version #
		name = rclass.getName();
		if (definedTypes.get(name) != null
		    || initiatedTypes.get(name) != null)
		    throw new LinkageException("duplicate class definition: "
					       + name);
		// We use Boolean.FALSE to detect cyclic inheritence,
		// see typeFor FIXME: should store current thread and
		// notifyAll when complete
		definedTypes.put(name, Boolean.FALSE);
		S3Type.Scalar ret = null;
		try {
		    Type.Class parent = null;
		    if (name == Transaction.ABORTED_EXCEPTION
			|| name == Transaction.ED_ABORTED_EXCEPTION) {
			parent = (Type.Class) typeFor( JavaNames.java_lang_Object);
		    } else if (name != JavaNames.java_lang_Object) {
			try {
			    parent =  (Type.Class) typeFor( rclass.getSuper());
			} catch (ClassCastException e) {
			    throw new LinkageException.ClassChange(name + " extends "
								   + rclass.getSuper()
								   + ", which is not a class");
			}
		    }
		    TypeName.Scalar[] ifn = rclass.getInterfaces();
		    Type.Interface[] ifc = new Type.Interface[ifn.length];
		    for (int i = 0; i < ifc.length; i++) 
			try { ifc[i] = (Type.Interface) typeFor(ifn[i]); }
			catch (ClassCastException e) {
			    throw new LinkageException.ClassChange(name + " implements " + ifn[i] + ", which is not an interface");
			}

		    ret =  (rclass.getAccessMode().isInterface()
			    ? (S3Type.Scalar) new S3Type.Interface( rclass,ifc, this)
			    : (S3Type.Scalar) new S3Type.Class( rclass, parent, ifc, this));
		    if (parent == null) 
			domain.ROOT_TYPE = ret.asClass();
		    else if (name == JavaNames.java_lang_Class
			     && this == domain.getSystemTypeContext())
			domain.CLASS_TYPE = ret.asClass();
		    try {
			S3Type.Scalar shSt = null;
			if (domain.CLASS_TYPE != null)
			    shSt = ret.makeSharedStateType(rclass, this);
			else
			    bootRC.add(rclass);
			domain.newTypeHook(ret);
			definedTypes.put(name, ret);
			initiate(name, ret);
			if (shSt != null)
			    domain.newTypeHook(shSt);
		    }
		    catch (LinkageException e) {
			// FIXME: I that errors are impossible when
			// creating and registering the shared-state
			// type.  If not, this code is buggy.
			ret = null;
			throw e;
		    }
		} finally {
		    // If we took an exception, put(name, null) correctly
		    // allows later attempt to load the type.
		    definedTypes.put(name, ret);
		}
		return ret;
	    } finally {
		MemoryPolicy.the().leave(r);
	    }
	}
    }

    public void bootstrap() {
	try {
	    ArrayList bootRC  = this.bootRC = new ArrayList();
	    ArrayList bootTypes = this.bootTypes;
	    // getMetaClass will create
	    // Ljava/lang/Object;
	    // Ljava/io/Serializable;
	    // Ljava/lang/Class;
	    // Gjava/lang/Class;
	    //
	    // Gjava/lang/Class is needed to define Class's interface
	    // table, but the other g-types are not yet needed.  This
	    // is good, because they could not be created before
	    // Ljava/lang/Class was defined.  However, the
	    // RepositoryClass definitions of java.lang.Object and
	    // java.io.Serializable have been saved in bootRC, and we
	    // can safely create shared states from them.
	    //
	    // We also need Ljava/lang/Class to define the
	    // shared-state types for array and primitve types.  These
	    // non-Scalar instance types are stored in bootTypes
	    // pending bootstrapping.
	    domain.getMetaClass();
	    this.bootRC = null;
	    this.bootTypes = null;
	    for (Iterator it = bootRC.iterator(); it.hasNext(); ) {
		RepositoryClass rc = (RepositoryClass) it.next();
		S3Type.Scalar instType =
		    (S3Type.Scalar) definedTypes.get(rc.getName());
		Type.Scalar staticType = instType.makeSharedStateType(rc, this);
		domain.newTypeHook(staticType);
	    }
	    for (Iterator it = bootTypes.iterator(); it.hasNext(); ) {
		S3Type t = (S3Type) it.next();
		t.sharedStateType = new S3Type.SharedStateClass(t, this);
		domain.newTypeHook(t.sharedStateType);
	    }
	} catch (LinkageException e) {
	    Executive.panicOnException(e, "cannot load core classes");
	}
    }

    public boolean bootstrapping() { return bootRC != null; }

   /** Unlock the context to allow the addition of new types. */
   public void thaw() {
       definedTypes = new IdentityHashMap(definedTypes);
       frozen = false;
   }

   /** Lock the context to prevent addition of new types. */
   public void freeze(boolean permanently) {
       frozen = true;
       definedTypes = Collections.unmodifiableMap(definedTypes);
       //initiatedTypes = Collections.unmodifiableMap(initiatedTypes);

       // As it turns out, we don't much care whether or not the
       // freeze on loading is permanent.
       // * We can't null out parser, because there is no way to know
       //   whether loading is still enabled in another context,
       // * We can't null out loader, because we may still need to
       //   initiate loading for a class that already exists in the
       //   parent context.

       // Actually, there is no good reason that we should initiate
       // new lookups after freezing!
       loader = null;
   }
   
   public String toString() {
       return domain.toString() + id;
   }

   /* testing utility     */
   protected final boolean contains(TypeName name) {
       return definedTypes.containsKey(name);
   }
}
