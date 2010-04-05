package ovm.core.domain;
/**
 * A <tt>JavaUserDomain</tt> is a user-domain that supports the execution
 * of a Java Virtual Machine. It provides services and facilities, like
 * reflection, that pertain to the semantics of the Java programming
 * language and platform API's.
 * <p>For example, a JVM typically has three classloaders defined by
 * default: the boostrap or system class loader, the extensions classloader
 * and the application class loader. These are reflected in the three
 * pre-defined type contexts within the JavaUserDomain: the system 
 * extension and application type context.
 */
public interface JavaUserDomain extends UserDomain, JavaDomain {

    /**
     * Return the type context that holds standard library classes.
     **/
    Type.Context getBootTypeContext();

    /**
     * Return the search path to be used with the boot Type.Context
     **/
    String getBootClassPath();
    
    /**
     * Return the application type context for this Java user domain.
     * This type context corresponds to the normal application classpath
     * provided to the JVM
     */
     Type.Context getApplicationTypeContext();

    /**
     * Return the serach path to be used with the application Type.Context.
     **/
    String getClassPath();
    /**
     * Return the extensions type context for this Java user domain.
     * This type context corresponds to the extensions classpath
     * provided to the JVM. Standard extensions are treated as trusted
     * types by the JVM - as if they were loaded by the bootstrap or system
     * class loader.
     */
    Type.Context getExtensionsTypeContext();

    /**
     * Construct a fresh classloading context within this domain.
     **/
    Type.Context makeTypeContext();

    /**
     * Return a Type.Loader that forwards requests to the user-level
     * java.lang.ClassLoader given as an argument.
     **/
    Type.Loader makeTypeLoader(Oop classLoader);
    
    /**
     * Create a new object instance using the constructor object and
     * arguments given.
     * @param constructor the constructor object as an 
     * @param argArray the arguments to the constructor
     * @param callerClass the optional hidden parameter created by
     *        {@link s3.services.bootimage.CallingClassTransform}
     * @return the new instance
     * @throws Any exception thrown by the instantiation process, or the
     * execution of the constructor.
     */
    Oop newInstance(Method constructor, Oop argArray, Oop callerClass);

    /**
     * Perform a reflective invocation in this domain.
     * @param receiver the object upon which <tt>theMethod</tt> is to be
     * invoked. If the method is a static method then the receiver argument
     * is ignored.
     * @param theMethod the method to be invoked. It is assumed that the
     * receiver is of a type that defines the given method. This should be
     * checked in the user-domain JVM reflection code.
     * @param argArray the arguments to pass to the method invocation. This
     * array should match the number and types of the parameters declared by 
     * the given method, except that for primitive parameters, passed via
     * wrappers, widening conversions are applied (eg. a Byte can be used to
     * set a byte, short, int, long, float or double).
     * @param callerClass the optional hidden parameter created by
     *        {@link s3.services.bootimage.CallingClassTransform}
     * @return the result of the method invocation, or <tt>null</tt> for a
     * void method.
     */
    Oop invokeMethod(Oop receiver, Method theMethod,
		     Oop argArray, Oop callerClass);

    /**
     * This method should be called before the outermost constructor
     * of any object that may override <code>Object.finalize()</code>.
     * When finalizable objects are explicitly allocated from
     * user-level code, a call to this method is inserted at the
     * appropriate point.  Because this method explicitly checks
     * whether an object is finalizable, executive domain code that
     * allocates user-level objects can call this method without
     * long-term overhead.
     **/
    void registerFinalizer(Oop oop);

    /**
     * This method does not return, and it should be called once, from
     * a domain's finalizer thread.
     **/
    void runFinalizers();
}

