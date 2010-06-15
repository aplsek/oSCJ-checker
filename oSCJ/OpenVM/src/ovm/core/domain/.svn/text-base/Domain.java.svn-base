package ovm.core.domain;

import ovm.core.execution.CoreServicesAccess;
import ovm.core.execution.RuntimeExports;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.util.Iterator;
import ovm.util.UnicodeBuffer;
import s3.services.bytecode.ovmify.IRewriter;
import ovm.core.services.memory.VM_Address;

/**
 * <code>Domain</code> is a container and name-space for types and blueprints.
 * It is essentially an application space; applications running in different
 * domains have (conceptually) no interaction via shared objects. Two
 * different domains might well contain two classes with the same name but
 * different implementations (for example, one domain may contain a class
 * that uses one version of <code>java.lang.String</code>, whereas another
 * application may expect a different implementation of this class). The
 * domain with which the application is associated will determine where
 * classes will be searched for at resolution time.
 * <p>The methods of a domain fall into three main categories:
 * <ul>
 * <li>Those that describe the type system: such as the root type for
 * the domain, the class that forms the entry point for executing the
 * domain, and the interfaces that array types support. The intent is to
 * be language neutral, but there are some obvious ties to the Java type
 * system.
 * </li>
 * <li>Lifecycle methods to startup and shutdown a domain. This lifecycle
 * aspect needs further refinement.
 * </li>
 * <li>Query methods that expose the main interface objects of a domain:
 * the core-services-access (CSA) object defines the interface from the 
 * bytecode execution to the domain, runtime-exports defines the API entry
 * points back into the domain, and we can determine the type contexts of
 * the domain.
 * </li>
 * </ul>
 * @author Krzysztof Palacz, James Liang
 * @see Type
 * @see Blueprint
 **/
public interface Domain extends Blueprint.Factory {

    // The type system methods

    /**
     * Gets the <code>Type</code> object for the root of the inheritance
     * hierarchy (e.g., <code>java.lang.Object</code>).
     * @return the type object for the root object
     **/
     Type.Class getHierarchyRoot();

    /**
     * Return the type of type objects, which is also the direct
     * superclass of all shared-state types (e.g.,<code>java.lang.Class</code>).
     **/
    Type.Class getMetaClass();
    
    /**
     * Gets the <code>Type</code> objects for the interfaces implemented
     * by array types (for example, <code>Serializable</code> and
     * <code>Cloneable</code>).
     * @return an array of types representing the interfaces implemented by
     * arrays.
     **/
     Type.Interface[] getArrayInterfaces();


    // The lifecycle methods

    /**
     * Load core classes such as the {@link #getHierarchyRoot root class}
     * and the {@link #getMetaClass meta class}.  This method must be
     * called before any attempt is made to load code into the domain,
     * but after a {@link Type.Loader} has been associated with
     * each of the domain's builtin {@link Type.Context Type.Contexts}.
     **/
    void bootstrap() throws LinkageException;

    /**
     * Suspend classloading until a subsequent call to
     * {@link #startup}, or forever.  This method is called during VM
     * generation, after the static analysis has finished.  Attempts
     * to load additional classes after this method is called indicate
     * a bug in the static analysis.
     *
     * @see s3.services.bootimage.DomainSprout#importCode
     **/
    void freezeClassLoading(boolean permanently);

    /**
     * Drop references to data structures that are only needed at
     * compile-time.
     *
     * @param allCallsSeen is true if we will no longer need to
     * resolve method calls into this domain
     * 
     * @see s3.services.bootimage.Driver#genAuxFiles
     **/
    void dropCompileTimeData(boolean allCallsSeen);

    /**
     * Do what is needed to startup this domain
     */
     void startup();

    /**
     * Shutdown this domain. This might do things like shutdown domain-specific
     * services, close file descriptors used by the domain and other domain
     * level shutdown hooks.
     * <p><b>FIXME:</b> Domain shutdown should hook into an Executive lifecycle
     * method so that the shutting down of the last domain terminates the
     * OVM. At present OVM lifecycle management is hardcoded in S3Executive
     * and affected by direct calls like Runtime.halt from the user-domain.
     */
     void shutdown();

    // Query methods

    /**
     * Return a small integer that uniquely identifies this domain
     **/
    public char getUID();

    /**
     * Retrieve the core services access instance for this domain.
     * When this domain's main class is executing its bytecode is
     * processed using this domains CSA instance.
     */
    CoreServicesAccess getCoreServicesAccess();

    /**
     * Retrieve the run-time exports object associated with this domain.
     * The RTE exposes the executive domain API needed by the code
     * running in this domain.
     */
    RuntimeExports getRuntimeExports();

    /**
     * Retrieve the IRewriter object used to convert standard java
     * bytecode to OvmIR in this domain.
     */
    IRewriter getRewriter();

    /**
     * Return the <code>Type.Context</code> for system types in this domain.
     * Every domain has at least one type context: that for the system types 
     * in that domain.
     * @return the system type context
     */
    Type.Context getSystemTypeContext();

    /**
     * Return the leaf type context in this domain. Due to the hierarchical
     * relationship between type contexts, the leaf type context will allow
     * you to find all types in the domain. The name is a historical remnant
     * of the fact that statically, for Java domains, the application type
     * context is the leaf type context.
     * <p>Note that we preclude the potential for a tree of type contexts - 
     * again this is related to the Java heritage.
     */
    Type.Context getApplicationTypeContext();

    /**
     * Map a {@link s3.util.PragmaException}'s name to a
     * domain-specific exception type.  This method may return null of
     * the pragma does not exist in this domain.<p>
     *
     * The entire pragma mechanism: marker exceptions, and rewriting
     * code exists in the executive domain.  It is possible, however,
     * to use pragmas within other domains.  The only thing that is
     * required is the way to translate the real (ED) exception type's
     * name into to a per-domain exception type.  In ovm, we translate
     * exceptions by replacing the package name with org.ovmj.util.<p>
     *
     * If you wish to use a pragma in the user domain, you simply need
     * to define a peer exception type in org.ovmj.util, and start
     * declaring methods that throw that exception.
     **/
    Type getPragma(TypeName.Scalar exc);

    Iterator getBlueprintIterator();

    /**
     * Convenience method that should be unnecessary
     */
    boolean isExecutive();

    // String manipulation/conversion methods. Maybe move into JavaDomain?

    /**
     * Make a domain specific String object from the given Unicode buffer.
     */
    public abstract Oop makeString(UnicodeBuffer contents);
    
    /**
     * Extract the Unicode buffer from the given domain String object
     */
    public abstract UnicodeBuffer getString(Oop dString);

    /**
     * Extract the contents of the domain string as a byte[] encoded using
     * the default encoding.
     */
    public abstract byte[] getLocalizedCString(Oop dString);

    /**
     * Convert a C string (a zero-terminated byte array using the
     * system's default charset) into this domain's string
     * representation.
     **/
    public Oop stringFromLocalizedCString(VM_Address cstring);

    /**
     * Executive domain strings that come from literals or image-build
     * time values are UTF-8, but executive domain strings that come
     * from the system (process arguments &amp;c) are encoded in the
     * system's default charset.  When converting an ED string to the
     * domain's representation, we must first decide whether it is
     * UTF-8, or in the default encoding.<p>
     *
     * UTF-8 ED strings can be converted to user-domain strings with
     * {@link #makeString} and {@link ovm.util.UnicodeBuffer.Factory#wrap(String)}.
     * But, default-encoded ED strings should be converted to UD
     * strings with this method.<p>
     *
     * As with other methods that operate on localized C strings, this
     * method can only be called after a Java domain's system properties
     * have been initialized (or at least <code>file.encoding</code>).
     **/
    public Oop stringFromLocalizedCString(String edString);

    /**
     * Extract the contents of the domain string as a byte[] encoded using
     * UTF8
     */
    public abstract byte[] getUTF8CString(Oop dString);

    /**
     * If a string equal to this one is already in the intern table,
     * return it, otherwise, add the given string to the intern table
     * and return it.
     **/ 
    public abstract Oop internString(Oop dString);

    /**
     * Return the interned string whose contents are equal to this
     * character sequence.  Create said string if needed.
     **/
    public abstract Oop internString(UnicodeBuffer b);

    /**
     * Similar to
     * <pre>internString(UTF8Store._.getUtf8(utf8Index))</pre>, but
     * more careful about temporary storage allocation.
     **/
    public abstract Oop internString(int utf8Index);

    /**
     * Register tn as a root type.  Maybe this method should take a
     * Name,Context pair, but in the current scheme of things, the
     * name is enough.  After this call, tn will be included in the
     * array returned by getRoots.
     * <p>
     * FIXME: should this method exist, and if so, should it take a
     * Type.Context?
     **/
    void registerRoot(TypeName.Compound tn);

    /**
     * Regster tn as being the target of a reflective allocation.  The
     * type corresponding to this name will subsequently be returned
     * by getReflectiveNews.
     * <p>
     * FIXME: should this method take a Type.Context?
     **/
    void registerNew(TypeName.Compound tn);

    /**
     * Register fsel as being the target of a reflective field
     * access.  The field corresponding to this selector will be
     * returned by getReflectiveFields
     **/
    void registerField(Selector.Field fsel);
    
    /**
     * Register msel as being the target of a reflective invocation.
     * If this method is called on a constructor, the constructor's
     * declaring class will appear in getReflectivelyAllocatedTypes.
     * Whether or not msel names a constructor, the corresponding
     * method will be returned by getReflectiveCalls.
     * <p>
     * FIXME: should this method take a Type.Context?
     **/
    void registerCall(Selector.Method msel);

    /**
     * Register msel as being the target of a reflective virtual or
     * interface invocation.  The corresponding method will be
     * returned by getReflectiveCalls.  <p>
     * FIXME: should this method take a Type.Context?
     **/
    void registerVirtualCall(Selector.Method msel);

    TypeName.Compound[] getRoots();
    Type[] getRootTypes();
    Method[] getReflectiveCalls();
    Method[] getReflectiveVirtualCalls();
    Field[] getReflectiveFields();
    Type[]  getReflectiveNews();
 } // end of Domain






