package ovm.core.domain;

import ovm.core.OVMBase;
import ovm.core.repository.Mode;
import ovm.core.repository.RepositoryClass;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.UnboundSelector;
import ovm.util.ByteBuffer;

/**
 * A <code>Type</code> contains information about a class and its structure
 * and domain. A <code>Type</code> has a reference to its
 * <code>Context</code>, which contains a reference to the
 * <code>Type's</code> domain. Thus, every <code>Type</code> belongs to
 * exactly one <code>Context</code>, and every <code>Context</code> refers
 * to exactly one <code>Domain</code>. The <code>Type</code> can then be
 * seen as a domain-specific representation of class information.
 *
 * <p>Some information within a <code>Type</code> may be resolved lazily.  
 * For example, if a class has a field which is never assigned to, the class 
 * representing that field's type may never be loaded. Since the field's type 
 * may never be resolved, the <code>Type</code> doesn't bother to encode that
 * information until the actual resolution occurs. <code>Type</code>
 * objects, then, may evolve at runtime. 
 *
 * <p>Each <code>Blueprint</code> corresponds to exactly one
 * <code>Type</code>.  However, each <code>Type</code> may have several
 * <code>Blueprints</code>.  See {@link ovm.core.domain.Blueprint
 * Blueprint} for further details on the relation between <code>Type</code>
 * and <code>Blueprint</code>.
 *
 * <p><b>N.B.: For the time being, there <i>is</i> a one-to-one
 * correspondence between <code>Type</code> objects and
 * <code>Blueprint</code> objects.  However, KP indicated that this will
 * hopefully change, at least as far as the higher-level concept is
 * concerned (i.e. implementations may choose to keep this one-to-one, but
 * this is a special case of one-to-many).</b>
 *
 * @author Krzysztof Palacz
 * @see Context
 **/
public interface Type {

    static final Type.Reference[] EMPTY_REFERENCE_ARRAY = new Type.Reference[0];
    
    /**
     * Get this type's context (which links it with its <code>Domain</code>.)
     * @return the context associated with this type object
     * @see Type.Context
     **/
    Context getContext();
    /**
     *  <code>getContext().getDomain()</code>
     */
    Domain getDomain();
    /**
     * If there is shared state for all instances of the type this Type
     * describes, return a Type describing that shared state.
     * <p>
     * This is the identity function when isSharedState() is true.
     **/
    Type.Class getSharedStateType();

    Oop getClassMirror();

    /**
     * The opposite of getSharedStateType().
     * <p>
     * This is the identity function when isSharedState() is false.
     **/
    Type getInstanceType();

    /**
     * Does this Type describe shared state?
     * @return true if this is a shared state type.
     **/
    boolean isSharedState();
    /**
      * Get the modifiers associated with this <code>Type's</code> 
      * associated class.
      * @return the modifier object for this type's class
      **/
    Mode.Class getMode();
    /**
     * Return true if this type is a subtype of its argument.
     **/
    boolean isSubtypeOf(Type other);

    State getLifecycleState();

    void setLifecycleState(State state);
    /**
     * If this <code>Type</code> is an array type, get the <i>element</i>
     * type (or innermost component type) for this type. (This is
     * the type one would get by stripping all of the <code>'['</code>
     * characters from a fully qualified name.
     * @return the innermost component type of this type if this is
     *         an array type, or null if not.
     **/
    Type getInnermostComponentType();
    /**
     * If this <code>Type</code> is an array type, get the <i>depth</i>,
     * or number of dimensions, for this type. (This is
     * the number one would get by counting all of the <code>'['</code>
     * characters from the beginning of a fully qualified name).
     * @return the number of dimensions of this <code>Type</code> 
     *         (which is 0 for non-array types)
     **/
    int getDepth();

    boolean isCompound();
    Type.Compound asCompound();

    /**
     * Determine if this <code>Type</code> object represents an array type.
     * @return true if this <code>Type</code> object represents an array type,
     *         else false
     **/
    boolean isArray();

    /**
     * Return this <code>Type</code> as a <code>Type.Array</code>. If this
     * <code>Type</code> doesn't represent an array, an exception should be 
     * thrown.
     * @return this type as the more specific <code>Array</code> type object.
     **/
    Type.Array asArray();
    /**
     * Determine if this <code>Type</code> object represents a scalar type.
     * @return true if this <code>Type</code> objects represents a scalar type,
     *         else false.
     **/
    boolean isScalar();
    /**
     * Return this <code>Type</code> as a <code>Type.Scalar</code>. If this
     * <code>Type</code> doesn't represent a scalar type, an exception should 
     * be thrown.
     * @return this type as the more specific <code>Scalar</code> type object.
     **/
    Type.Scalar asScalar();
    /**
     * Determine if this <code>Type</code> object represents an interface type.
     * @return true if this <code>Type</code> object represents an interface 
     *         type, else false
     **/
    boolean isInterface();
    /**
     * Determine if this <code>object</code> represents a primitive type.
     * @return true if this <code>Type</code> object represents a primitive 
     *         type, else false
     **/
    boolean isPrimitive();
    /**
     * Determine if this <code>object</code> represents a wide primitive type.
     * @return true if this <code>Type</code> object represents a wide 
     *         primitive type, else false
     **/
    boolean isWidePrimitive();
    /**
     * Get all of the interface <code>Type</code> objects of interfaces 
     * directly implemented by this <code>Type's</code> class
     * @return an array of implemented interface <code>Type</code> objects
     **/
    Type.Interface[] getInterfaces();
    /**
     * Get <em>every</em> interface this type's class implements, directly
     * or indirectly; this means finding all of the superinterfaces of this
     * type's directly implemented interfaces, as well as all ancestors'
     * directly and indirectly implemented interfaces.
     * @return the type objects for every interface this type's class
     *         implements, either directly or indirectly
     **/
    Type.Interface[] getAllInterfaces();
    /**
     * Get the <code>Type</code> object for this class's superclass,
     * or null if there is no superclass
     * @return this class's superclass <code>Type</code> object,
     *        or null for the root object (e.g. <code>java.lang.Object</code>)
     **/
    Type.Class getSuperclass();
    /**
     * Returns true if this type has no parent type, i.e. is the root of
     * the hierarchy. (NB: there is an implicit assumption here that the
     * hierarchy has a single root)
     * @return true if no supertype
     **/
    boolean isRoot();

    /**
     * Factored out common operation in getField and getMethod: walk up
     * from this Type to the named ancestor and return that Type (most
     * often this one, probably).
     * @param tn Name of the ancestor desired
     * @return The named ancestor (this Type or a supertype)
     * @throws OVMError.ClassCast if this Type has no such ancestor
     */
    Type getAncestor(TypeName tn);
    /**
     * Obtain the <code>TypeName</code> of this <code>Type</code>.
     * Java lacks covariance, thus the 'getUnrefinedName' is used for
     * the generic <code>TypeName</code> return value. Use 'getName'
     * if you statically know the subtype of <code>Type</code>, it
     * will return a more precisely typed (refined)
     * <code>TypeName</code>.
     * @return the generic <code>TypeName</code> object associated with this
     *         <code>Type</code>
     **/
    TypeName getUnrefinedName();
    Field getField(UnboundSelector.Field usf);
    /**
     * Given the <code>Selector</code> for a Field, retrieve
     * its <code>Field</code> object.
     * @param selector the bound selector for the desired field
     * @return the <code>Field</code> associated with this selector,
     *         or <code>null</code> if not found.
     * @deprecated Use {@link #getAncestor(TypeName) getAncestor()} and
     * {@link #getField(UnboundSelector.Field)}.
     * Should getAncestor be exposed here?
     **/
    Field getField(Selector.Field selector);
    /**
     * Get the iterator over all fields in this type (including
     * inherited fields) 
     **/
    Field.Iterator fieldIterator();
    /**
     * Get the iterator over the fields in this type (excluding
     * inherited fields) 
     **/
    Field.Iterator localFieldIterator();
    /**
     * Return the local defintion of the method usm, or null if it is
     * inherited or undefined.  Equvialent to
     * {@code getMethod(usm, false)}
     **/
    Method getMethod(UnboundSelector.Method usm);

    /**
     * Return the local defintion of the method usm, or null if it is
     * inherited or undefined.  
     * @param usm          the name and signature to search for
     * @param externalName true if we are searching for a user-visible
     *                     method definition that may be implemented
     *                     with a different signature internally
     * @see Method#getSelector
     * @see Method#getExternalSelector
     **/
    Method getMethod(UnboundSelector.Method usm, boolean externalName);
    /**
     * Given the <code>Selector</code> for a method, retrieve
     * its <code>Method</code> object.
     * @param selector the bound selector for the desired method
     * @return the <code>Method</code> associated with this selector,
     *         or <code>null</code> if not found.
     * @deprecated Use {@link #getAncestor(TypeName) getAncestor()} and
     * {@link #getMethod(UnboundSelector.Method)}.
     **/
    Method getMethod(Selector.Method selector);
    /**
     * Get the iterator over all methods in this type (including
     * inherited methods) 
     **/
    Method.Iterator methodIterator();
    /**
     * Get the iterator over the methods in this type (excluding
     * inherited methods) 
     **/
    Method.Iterator localMethodIterator();

    /**
     * The <code>Type</code> interface for built-in and primitive types
     **/
    interface Primitive extends Type {
        /**
         * Get the <code>TypeName</code> associated with this
         * primitive type
         * @return the primitive typename associated with this type
         **/
        TypeName.Primitive getName();
    }

    /**
     * The <code>Type</code> interface for wide (2-word) built-in 
     * and primitive types
     **/
    interface WidePrimitive extends Primitive {
    }

    /**
     * The <code>Type</code> interface for non-primitive objects
     **/
    interface Compound extends Type {
        /**
         * Get the <code>TypeName</code> associated with this type
         * @return the typename associated with this type
         * @throws RepositoryUnavailableException if there is an error in
         *         accessing the repository in order to retrieve the typename.
         **/
        TypeName.Compound getName() throws RepositoryUnavailableException;

	/**
	 * Return the UTF8 index of this type's source file, or 0 if
	 * no source file is present.
	 **/
	int getSourceFileNameIndex();

        /**
         * Get the iterator over all fields in this type (including
         * inherited fields) 
         **/
        Field.Iterator fieldIterator();
        /**
         * Get the iterator over the fields in this type (excluding
         * inherited fields) 
         **/
        Field.Iterator localFieldIterator();
        /**
         * Get the iterator over all methods in this type (including
         **/
        Method.Iterator methodIterator();
        /**
         * Get the iterator over the methods in this type (excluding
         * inherited fields)
         **/
        Method.Iterator localMethodIterator();
    }

    /**
     * The <code>Type</code> for references (i.e. all data types which can
     * be allocated in the heap such as Objects, Arrays, ...)
     **/
    interface Reference extends Compound {
        /**
         * Get the <code>Types</code> of the interfaces implemented
         * by this <code>Type</code>
         * @return the array of interface types
         * maybe an iterator would be better
         **/
        Interface[] getInterfaces();
        /** 
         * Get the superclass <code>Type</code> of this <code>Type</code>. 
         **/
        Type.Class getSuperclass(); // arrays' superclass is ROOT -- in java
        /**
         * Get the <code>TypeName</code> associated with this <code>Type</code>
         * @return the typename object
         * @throws RepositoryUnavailableException if there is an error in
         * accessing the repository in order to retrieve the typename.
         **/
        TypeName.Compound getName() throws RepositoryUnavailableException;
    } // end of Type.Reference

    /**
     * Scalar types represent all reference types which are not arrays
     * (this means Objects).
     **/
    interface Scalar extends Reference {
	/**
	 * Return this type's constant pool
	 **/
	ConstantPool getConstantPool();
	
        /**
         * Get the <code>TypeName</code> of this type.
         **/
        TypeName.Compound getName() throws RepositoryUnavailableException;
        
        /**
         * Get the name of the enclosing Type, if this type is nested
         * @return  typename or null
         * @throws RepositoryUnavailableException
         */
        TypeName.Compound getOuterName() throws RepositoryUnavailableException;

        /**
         * Return the enclosing type for this type.
         * @return a class or interface, or null if the type isn't
         * an inner class.
         */
        Type.Scalar getOuterType() throws LinkageException;

	/**
	 * Ensure that this type's code is valid.  This method's
	 * behavior depends on {@link #getLifecycleState}:
	 * <dl>
	 * <dt> {@link Type.State#LOADED} </dt>
	 * <dd> Attempt verification.  Move to
	 * {@link Type.State#VERIFIED} on success, or throw an
	 * exception and move to state {@link
	 * Type.State#ERRONEOUS}</dd>
	 * <dt> {@link Type.State#ERRONEOUS} </dt>
	 * <dd> Immediately thow an exception </dd>
	 * <dt> Other States </dt>
	 * <dd> Do nothing.  Other states imply that verification is
	 * complete. </dd>
	 * </dl>
	 **/
	void verify() throws LinkageException;
    }

    /**
     * The <code>Type</code> for interfaces/classes which have not yet
     * been resolved (in other words, those that have not yet been
     * loaded in the VM).
     * FIXME nobody uses this. Don't start without reading the bugnotes for 417.
     **/
    interface Unresolved extends Scalar {
    }
    /**
     * The <code>Type</code> for interface objects
     **/
    interface Interface extends Scalar {
    }

    /**
     * The <code>Type</code> for resolved classes
     **/
    interface Class extends Scalar {
        /*
         * When Type.Class is instantiated (result of resolution), its
         * implemented interfaces are not required to be
         * resolved. Resolution may happen as a result of <code>cls
         * instanceof iface</code> or during <code>invokeinterface</code>.
         * That's when this method should be called.
	 *
	 * Not according to JVMS2 section 5.3.5.4.
         */
        // void addInterface(Interface iface);

        /**
         * If this Type is a known singleton type (type for which only one instance
         * can exist), return that instance; in any other case, return null.
         * FIXME we could enforce things, like such types have no <code>new</code>s
         * and no subtypes, but for now we do not.
         * For the moment, shared state types are the only reason this method exists.
         * @return singleton instance described by this Type, if this Type represents
         * a singleton; <code>null</code> otherwise.
         */
        Oop getSingleton();
    }

    /**
     * The <code>Type</code> for arrays
     **/
    interface Array extends Reference {
        /**
         * Get the innermost component (or <i>element</i>) <code>Type</code> of
         * this array
         * @return the innermost component type of this array type
         **/
        Type getInnermostComponentType();

        /**
         * Get the <i>component</i> <code>Type</code> of this array; note that
         * this refers to contents of the array, meaning that the
         * <code>Type</code> returned by this method is that of the
         * objects at a dimension that is one less than that of an array.
         * A 1-dimensional <code>int</code> array would return a corresponding
         * <code>int</code> type, but a 3-dimensional <code>int</code>
         * array would return a correponding 2-dimensional <code>int</code>
         * array type.
         * @return the component type of this array
         **/
        Type getComponentType();
        /**
         * Get the number of dimensions in this array.
         * @return the number of dimensions contains in this array
         **/
        int getDepth();
    }

    /**
     * Factory methods for <code>Type</code> objects.
     **/
    interface Factory {
	// what else is needed here ?
	/**
	 * Make a <code>Type</code> object for a given
	 * <code>TypeName</code> and a <code>Context</code>
	 * referring to its domain.
	 * @param typeName the type name of the class this type
	 *                 will correspond to. If a gemeinsam type name is supplied,
         *                 a shared state type will be returned (corresponding to
         *                 the instance type named by the corresponding non-
         *                 gemeinsam type name.
	 * @param context the context this type should be associated
	 *                with
	 * @return the desired <code>Type</code> object
	 * @throws LinkageException 
	 * @see Type.Context
	 **/
	Type makeType(TypeName typeName, Type.Context context)
	    throws LinkageException;

	Type.Array makeType(Type innermost, int depth);
    }


    /** 
     * A type belongs to exactly one <code>Context</code> and each
     * <code>Context</code> belongs to exactly one {@link Domain}. A
     * <code>Context</code> contains {@link Type}s, and defines the
     * namespace in which these types are defined.  <code>Type</code>s
     * defined within a <code>Context</code> may refer to
     * <code>Type</code>s defined in other <code>Context</code>s, but
     * every {@link TypeName} used within a <code>Context</code> must
     * refer to the same type.
     * <p>
     * A <code>Context</code> then, is the VM-internal equivalent to a
     * Java classloader.  This interface is, in fact, designed to be
     * compatible with JVMS section 5 classloading semantics.  But,
     * rather than calling <code>ClassLoader.loadClass</code> to find
     * the definition of a <code>TypeName</code>, a
     * <code>Context</code> should call
     * {@link Type.Loader#loadType(TypeName.Scalar)}.
     *
     * @see JavaUserDomain#makeTypeLoader(Oop)
     */
    interface Context {
	/**
	 * Return an integer that uniquely identifies this type context.
	 **/
	public int getUID();

	/**
	 * Return a number greater than the highest value
	 * Blueprint.getUID() for any blueprint in this Type.Context
	 * FIXME: In the current implemenation, this number may be
	 * higher than the total number of blueprints in this context
	 **/
	public int getBlueprintCount();
	
	/**
	 * Return a number greater than the highest value
	 * Method.getUID() for any method in this Type.Context
	 * FIXME: In the current implemenation, this number may be
	 * higher than the total number of methods in this context
	 **/
	public int getMethodCount();
	
        /**
         * Get the <code>Domain</code> associated with this 
         * <code>Context</code>
         * @return the domain associated with this context
         **/
        Domain getDomain();

	/**
	 * Change the {@link Type.Loader} that we ask for {@link Type}s.
	 **/
	void setLoader(Loader loader);
	/**
	 * Return the {@link Type.Loader} that we ask for {@link Type}s.
	 **/
	Loader getLoader();
	
	/**
         * Return the Type definition given the type name.  If we have
         * already resolved this name to a type, return it.
         * Otherwise, forward to the {@link Type.Loader} defined by
         * the last call to {@link #setLoader(Type.Loader)}.
         **/
	Type typeFor(TypeName tn) throws LinkageException;
    
        /**
         * The same as <tt>typeFor</tt> but throws <tt>OVMError</tt> instead 
         * of checked exceptions.
         * @param tn name of the type to find
         * @return the type 
	 */
	Type typeForKnown(TypeName tn);

	/**
	 * Return the Type definition given the type name, if we are
	 * an initiating loader.  Otherwise, return null.
	 *
	 * @param tn name of the type to find
	 * @return the type, or null if the type was not already defined
	 */
	Type loadedTypeFor(TypeName tn);

	/**
	 * Define a new type from classfile contents.
	 */
	Type.Scalar defineType(TypeName.Scalar name, ByteBuffer bytes)
	    throws LinkageException;

    } // end of Context

    /**
     * A <code>Loader</code> is responsible for finding the
     * {@link Type} corresponding to a {@link TypeName} in a
     * particular {@link Type.Context}.  It may create a new
     * <code>Type</code> using {@link
     * Type.Context#defineType(TypeName.Scalar, ByteBuffer)}, or it may
     * delegate to another <code>Loader</code>.  At runtime, a
     * <code>Loader</code> is a proxy for a user-domain object such as
     * a <code>ClassLoader</code>, but in the OVM build process, this
     * interface may be used to simulate a <code>ClassLoader</code>.
     **/
    interface Loader {
	/**
	 * If a type of this name is defined, load it.  Otherwise,
	 * throw a LinkageException.  This method should never return
	 * null.
	 **/
	Type loadType(TypeName.Scalar name) throws LinkageException;
	/**
	 * Return the user-level object this loader acts as a proxy
	 * for, or null.
	 **/
	Oop getMirror();
    }

    public final class State extends OVMBase {
	/**
	 * A valid classfile for this type exists, and all supertypes
	 * are <code>LOADED</code>.
	 **/
	public final static State LOADED = new State("LOADED");
	/**
	 * All supertypes are <code>VERIFIED</code> and non-final, and
	 * other types have been loaded as dictated by bytecode
	 * verification rules.  Most importantly: catch types have
	 * been loaded and verified to extend <code>Throwable</code>.<p>
	 *
	 * <b>NOTE</b>: A {@link Type} should not be created for a
	 * class that extends an interface, but should be created for
	 * a class that extends a final class.  This <code>Type</code> will make
	 * the transition from <code>LOADED</code> to
	 * <code>ERRONEOUS</code>, and prevent redefinition of the
	 * same name in the same context.  
	 **/
	public final static State VERIFIED = new State("VERIFIED");
	/** Shared state allocated. **/
        public final static State PREPARED = new State("PREPARED");
	/** Static initialization in progress. **/
        public final static State INITIALIZING = new State("INITIALIZING");
	/** Fully usable. **/
        public final static State INITIALIZED = new State("INITIALIZED");
	/** Unusable. **/
        public final static State ERRONEOUS = new State("ERRONEOUS");
        private final String name;
     
        private State(String name) {
            this.name = name;
          }
    
        public String toString() {
            return name;
        }
    }

    /**
     * return true if this type is a Class.
     * FIXME: Used? Why? How?
     **/
    boolean isClass();
    
    /**
     * NB was added so that we do not have to use instanceof within
     * domain/Subtyping --jv
     * FIXME: Used? Why? How?
     **/
    Class asClass();
    
} // end of Type
