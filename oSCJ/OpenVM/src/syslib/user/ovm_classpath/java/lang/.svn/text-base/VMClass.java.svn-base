/* VMClass.java -- VM Specific Class methods
   Copyright (C) 2003, 2004 Free Software Foundation

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package java.lang;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;

import org.ovmj.java.Opaque;
/*
 * This class is a reference version, mainly for compiling a class library
 * jar.  It is likely that VM implementers replace this with their own
 * version that can communicate effectively with the VM.
 */

/**
 *
 * @author Etienne Gagnon <etienne.gagnon@uqam.ca>
 * @author Archie Cobbs <archie@dellroad.org>
 * @author C. Brian Jones <cbj@gnu.org>
 */
final class VMClass 
{
    private final Class vmType;
    
    /** The name of the Class represented by this. (lazily initialized) */
    private volatile String name;

    /* For caching of Method, Constructor, and Field objects, used for 
       reflection. Only heap and immortal allocated objects are cached.
       Scope usage of these methods will reallocate each time.
    */
    private volatile transient Method      publicMethods[];
    private volatile transient Method      declaredMethods[];
    private volatile transient Constructor publicConstructors[];
    private volatile transient Constructor declaredConstructors[];
    private volatile transient Field       publicFields[];
    private volatile transient Field       declaredFields[];

    /** stores the methods provided by this class
     * @see #initMethodHash */
    private HashMap methodHashMap;
    /** stores the constructorsprovided by this class
     * @see #initMethodHash */
    private HashMap constructorHashMap;
    
    /* just to avoid having many copies of the "<init>" string around */
    private static final String constructorName = "<init>";

  /**
   * Discover whether an Object is an instance of this Class.  Think of it
   * as almost like <code>o instanceof (this class)</code>.
   *
   * @param klass the Class object that's calling us
   * @param o the Object to check
   * @return whether o is an instance of this class
   * @since 1.1
   */
  static native boolean isInstance(Class klass, Object o);

  /**
   * Discover whether an instance of the Class parameter would be an
   * instance of this Class as well.  Think of doing
   * <code>isInstance(c.newInstance())</code> or even
   * <code>c.newInstance() instanceof (this class)</code>. While this
   * checks widening conversions for objects, it must be exact for primitive
   * types.
   *
   * @param klass the Class object that's calling us
   * @param c the class to check
   * @return whether an instance of c would be an instance of this class
   *         as well
   * @throws NullPointerException if c is null
   * @since 1.1
   */
  static native boolean isAssignableFrom(Class klass, Class c);

  /**
   * Check whether this class is an interface or not.  Array types are not
   * interfaces.
   *
   * @param klass the Class object that's calling us
   * @return whether this class is an interface or not
   */
  static native boolean isInterface(Class klass);

  /**
   * Return whether this class is a primitive type.  A primitive type class
   * is a class representing a kind of "placeholder" for the various
   * primitive types, or void.  You can access the various primitive type
   * classes through java.lang.Boolean.TYPE, java.lang.Integer.TYPE, etc.,
   * or through boolean.class, int.class, etc.
   *
   * @param klass the Class object that's calling us
   * @return whether this class is a primitive type
   * @see Boolean#TYPE
   * @see Byte#TYPE
   * @see Character#TYPE
   * @see Short#TYPE
   * @see Integer#TYPE
   * @see Long#TYPE
   * @see Float#TYPE
   * @see Double#TYPE
   * @see Void#TYPE
   * @since 1.1
   */
  static native boolean isPrimitive(Class klass);

  /**
   * Get the name of this class, separated by dots for package separators.
   * Primitive types and arrays are encoded as:
   * <pre>
   * boolean             Z
   * byte                B
   * char                C
   * short               S
   * int                 I
   * long                J
   * float               F
   * double              D
   * void                V
   * array type          [<em>element type</em>
   * class or interface, alone: &lt;dotted name&gt;
   * class or interface, as element type: L&lt;dotted name&gt;;
   *
   * @param klass the Class object that's calling us
   * @return the name of this class
   */
    static String getName(Class klass) {
	VMClass c = get(klass);
	if (c.name == null)
	    c.name = LibraryImports.nameForClass(klass);
	return c.name;
    }

  /**
   * Get the direct superclass of this class.  If this is an interface,
   * Object, a primitive type, or void, it will return null. If this is an
   * array type, it will return Object.
   *
   * @param klass the Class object that's calling us
   * @return the direct superclass of this class
   */
  static native Class getSuperclass(Class klass);

  /**
   * Get the interfaces this class <EM>directly</EM> implements, in the
   * order that they were declared. This returns an empty array, not null,
   * for Object, primitives, void, and classes or interfaces with no direct
   * superinterface. Array types return Cloneable and Serializable.
   *
   * @param klass the Class object that's calling us
   * @return the interfaces this class directly implements
   */
  static native Class[] getInterfaces(Class klass);

  /**
   * If this is an array, get the Class representing the type of array.
   * Examples: "[[Ljava.lang.String;" would return "[Ljava.lang.String;", and
   * calling getComponentType on that would give "java.lang.String".  If
   * this is not an array, returns null.
   *
   * @param klass the Class object that's calling us
   * @return the array type of this class, or null
   * @see Array
   * @since 1.1
   */
  static native Class getComponentType(Class klass);

  /**
   * Get the modifiers of this class.  These can be decoded using Modifier,
   * and is limited to one of public, protected, or private, and any of
   * final, static, abstract, or interface. An array class has the same
   * public, protected, or private modifier as its component type, and is
   * marked final but not an interface. Primitive types and void are marked
   * public and final, but not an interface.
   *
   * @param klass the Class object that's calling us
   * @param ignoreInnerClassesAttrib if set, return the real modifiers, not
   * the ones specified in the InnerClasses attribute.
   * @return the modifiers of this class
   * @see Modifier
   * @since 1.1
   */
  static native int getModifiers(Class klass, boolean ignoreInnerClassesAttrib);

  /**
   * If this is a nested or inner class, return the class that declared it.
   * If not, return null.
   *
   * @param klass the Class object that's calling us
   * @return the declaring class of this class
   * @since 1.1
   */
  static native Class getDeclaringClass(Class klass);

  /**
   * Like <code>getDeclaredClasses()</code> but without the security checks.
   *
   * @param klass the Class object that's calling us
   * @param publicOnly Only public classes should be returned
   */
  static native Class[] getDeclaredClasses(Class klass, boolean publicOnly);

  /**
   * Like <code>getDeclaredFields()</code> but without the security checks.
   *
   * @param klass the Class object that's calling us
   * @param publicOnly Only public fields should be returned
   */
  static native Field[] getDeclaredFields(Class klass, boolean publicOnly);

  /**
   * Like <code>getDeclaredMethods()</code> but without the security checks.
   *
   * @param klass the Class object that's calling us
   * @param publicOnly Only public methods should be returned
   */
  static native Method[] getDeclaredMethods(Class klass, boolean publicOnly);

  /**
   * Like <code>getDeclaredConstructors()</code> but without
   * the security checks.
   *
   * @param klass the Class object that's calling us
   * @param publicOnly Only public constructors should be returned
   */
  static native Constructor[] getDeclaredConstructors(Class klass, boolean publicOnly);

  /**
   * Return the class loader of this class.
   *
   * @param klass the Class object that's calling us
   * @return the class loader
   */
    static ClassLoader getClassLoader(Class klass) {
	// ClassLoader.getClassLoader() is the first method to execute
	// with static initialization enabled (see
	// JavaVirtualMachine).  If VMClassLoader.bootClassLoader is
	// null, we can be sure that no application-level classes are
	// in use.
	ClassLoader boot = VMClassLoader.bootClassLoader;
	if (boot == null)
	    return null;

	ClassLoader ret = LibraryImports.getClassLoader(klass);
	return (ret == boot ? null : ret);
    }

  /**
   * Load the requested class and record the specified loader as the
   * initiating class loader.
   *
   * @param name the name of the class to find
   * @param initialize should the class initializer be run?
   * @param loader the class loader to use (or null for the bootstrap loader)
   * @return the Class object representing the class or null for noop
   * @throws ClassNotFoundException if the class was not found by the
   *         class loader
   * @throws LinkageError if linking the class fails
   * @throws ExceptionInInitializerError if the class loads, but an exception
   *         occurs during initialization
   */
    static Class forName(String name, boolean initialize,
			 ClassLoader loader)
	throws ClassNotFoundException
    {
	if (loader == null)
	    loader = VMClassLoader.bootClassLoader;
	try {
	    Class c = LibraryImports.initiateLoading(loader, name.intern());
	    if (initialize)
		LibraryImports.initializeClass(c);
	    return c;
	} catch (NoClassDefFoundError e) {
	    throw new ClassNotFoundException(name, e);
	}
    }

  /**
   * Return whether this class is an array type.
   *
   * @param klass the Class object that's calling us
   * @return true if this class is an array type
   * operation
   */
  static native boolean isArray(Class klass);

  /**
   * Throw a checked exception without declaring it.
   */
  static native void throwException(Throwable t);

    static VMClass get(Class c) {
	if (c.vmdata == null) {
	    Opaque r = LibraryImports.enterAreaForMirror(c);
	    try {
		c.vmdata = new VMClass(c);
	    } finally {
		LibraryImports.leaveArea(r);
	    }
	}
	return (VMClass) c.vmdata;
    }

    private VMClass(Class c) {
	this.vmType = c;
    }

    Constructor getConstructor(Class[] args)
	throws NoSuchMethodException
    {
	if (publicConstructors == null)
	    _initPublicConstructors();
	MethodKey mkey = 
	    new MethodKey(constructorName, args);
	Object foundEntry = constructorHashMap.get(mkey);
	if (foundEntry != null) {
	    /* it's faster to look straight in the hash table, but 
	     * we then have to check the modifier */
	    if (Modifier.isPublic(((Constructor)foundEntry).getModifiers()))
		return (Constructor) foundEntry;
	}

	throw new NoSuchMethodException("(constructor) " + formatSignature(simpleTypeName(vmType.getName()), args));
    }

    Constructor[] getConstructors(){
	if(publicConstructors == null)
	    _initPublicConstructors();
	return publicConstructors;
    }
    
    Constructor getDeclaredConstructor(Class[] args)
	throws NoSuchMethodException
    {
	if(declaredConstructors == null)
	    _initDeclaredConstructors();

	MethodKey mkey = 
	    new MethodKey(constructorName, args);
	Object foundEntry = constructorHashMap.get(mkey);
	if (foundEntry != null) {
	    return (Constructor) foundEntry;
        }

	throw new NoSuchMethodException("(constructor) " + formatSignature(simpleTypeName(vmType.getName()), args));
    }

    Constructor[] getDeclaredConstructors(){
	if(declaredConstructors == null) {
	    _initDeclaredConstructors();
	}
	return declaredConstructors;
    }
    
    Field[] getDeclaredFields() {
        if (declaredFields == null) {
	    Opaque r = LibraryImports.enterAreaForMirror(vmType);
	    try {
		declaredFields = LibraryImports.getDeclaredFields(vmType);
	    } finally {
		LibraryImports.leaveArea(r);
	    }
        }
        return declaredFields;
    }
    
    Method getDeclaredMethod(String name, Class[] args)
        throws NoSuchMethodException{
	if (declaredMethods == null)
	    _initDeclaredMethods();

	MethodKey mkey = 
	    new MethodKey(name, args);
	// System.out.println(vmType + " get " + mkey);
	Object foundEntry = methodHashMap.get(mkey);
	if (foundEntry != null){
	
	        Method m = (Method) foundEntry;
                if (m.getDeclaringClass() == vmType)
  		  return (Method) foundEntry;
	} 
	
	throw new NoSuchMethodException(formatSignature(name, args));
    }
    
    Method[] getDeclaredMethods(){
	if(declaredMethods == null){
	    _initDeclaredMethods();
	}
	return declaredMethods;
    }

    Field[] getFields() {
	if (publicFields == null) {
	    Opaque r = LibraryImports.enterAreaForMirror(vmType);
	    try {
		publicFields = LibraryImports.getFields(vmType);
	    } finally {
		LibraryImports.leaveArea(r);
	    }
        }
	return publicFields;
    }

    Method getMethod(String name, Class[] args)
        throws NoSuchMethodException{
	if (publicMethods == null)
	    _initPublicMethods();

	MethodKey mkey = 
	    new MethodKey(name, args);
	Object foundEntry = methodHashMap.get(mkey);
	if (foundEntry != null){
	    /* it's faster to look straight in the hash table, but 
	     * we then have to check the modifier */
	    if (Modifier.isPublic(((Method)foundEntry).getModifiers()))
		return (Method) foundEntry;
	} 
	
	throw new NoSuchMethodException(formatSignature(name, args));	       
    }

    Method[] getMethods(){
	if(publicMethods == null)
	    _initPublicMethods();
	return publicMethods;
    }

    private String formatSignature(String name, Class[] args) {
        StringBuffer buf = new StringBuffer(32);
        buf.append(name);
        buf.append('(');
        int arglen = args == null ? 0 : args.length;
        for (int i = 0; i < arglen; i++) {
            buf.append(typeName(args[i].getName()));
            if (i < arglen-1)
                buf.append(", ");
        }
        buf.append(')');
        return buf.toString();
    }

    /**
     * Convert a fully qualified typename a.b.c to the simple name c 
     * (used to get the name of a constructor)
     */
    private String simpleTypeName(String name) {
        int lastDot = name.lastIndexOf('.');
        return name.substring(lastDot+1);
    }

    /**
     * Convert a type name, as returned by Class.getName() into the
     * expected readable form eg I -> int, [I -> int[], 
     * [Lfoo.bar.Baz; -> foo.bar.Baz[]
     */
    private String typeName(String name) {
        if (name.charAt(0) == '[') // array
            return arrayComponentName(name.substring(1)) + "[]";
        else
            return name;
    }

    private String arrayComponentName(String name) {
        if (name.charAt(0) == '[') // array
            return arrayComponentName(name.substring(1)) + "[]";

        if (name.length() == 1) { // primitive encoded in an array type
            switch( name.charAt(0)) {
            case 'Z': return "boolean";
            case 'B': return "byte";
            case 'C': return "char";
            case 'D': return "double";
            case 'F': return "float";
            case 'I': return "int";
            case 'J': return "long";
            case 'S': return "short";
            default: throw new Error("Unexpected type name " + name);
            }
        }
        // else it is an array component name L?????; 
        if (name.charAt(0) == 'L') {
            // strip the L and the ; 
            return name.substring(1, name.length()-1);
        }
        else {
            throw new Error("Unexpected array component name " + name);
        }
    }
    
    /* methods to deal with j.l.r.Method and j.l.r.Constructor */
    private void _initDeclaredMethods() {
	Opaque r = LibraryImports.enterAreaForMirror(vmType);
	try {
	    int nb = 0;
	    for (Iterator iter = _getMethodHashMapIterator();
		 iter.hasNext(); )
	    {
		Method m = (Method) iter.next();
		if (m.getDeclaringClass() == vmType)
		    nb++;
	    }
	    Method[] methods = new Method[nb];
	    nb = 0;
	    for (Iterator iter = _getMethodHashMapIterator();
		 iter.hasNext(); )
	    {
		Method m = (Method) iter.next();
		if (m.getDeclaringClass() == vmType)
		    methods[nb++] = m;
	    }
	    declaredMethods = methods;
	} finally {
	    LibraryImports.leaveArea(r);
	}
    }
    private void _initPublicMethods() {
	Opaque r = LibraryImports.enterAreaForMirror(vmType);
	try {
	    int nb = 0;
	    for(Iterator iter = _getMethodHashMapIterator(); iter.hasNext();){
		Method m = (Method) iter.next();
		if(Modifier.isPublic(m.getModifiers()))
		    nb++;
	    }
	    Method[] methods = new Method[nb];
	    nb = 0;
	    for(Iterator iter = _getMethodHashMapIterator(); iter.hasNext();){
		Method m = (Method) iter.next();
		if(Modifier.isPublic(m.getModifiers()))
		    methods[nb++] = m;
	    }
	    publicMethods = methods;
	} finally {
	    LibraryImports.leaveArea(r);
	}
    }
    private void _initDeclaredConstructors() {
	Opaque r = LibraryImports.enterAreaForMirror(vmType);
	try {
	    int nb = 0;
	    for(Iterator iter = _getConstructorHashMapIterator();
		iter.hasNext();)
	    {
		Constructor m = (Constructor) iter.next();
		nb++;
	    }
	    Constructor[] constructors = new Constructor[nb];
	    nb = 0;
	    for(Iterator iter = _getConstructorHashMapIterator();
		iter.hasNext();)
	    {
		Constructor m = (Constructor) iter.next();
		constructors[nb++] = m;
	    }
	    declaredConstructors = constructors;
	} finally {
	    LibraryImports.leaveArea(r);
	}
    }
    private void _initPublicConstructors() {
	Opaque r = LibraryImports.enterAreaForMirror(vmType);
	try {
	    int nb = 0;
	    for(Iterator iter = _getConstructorHashMapIterator();
		iter.hasNext();)
	    {
		Constructor m = (Constructor) iter.next();
		if (Modifier.isPublic(m.getModifiers()))
		    nb++;
	    }
	    Constructor[] constructors = new Constructor[nb];
	    nb = 0;
	    for(Iterator iter = _getConstructorHashMapIterator();
		iter.hasNext();)
	    {
		Constructor m = (Constructor) iter.next();
		if(Modifier.isPublic(m.getModifiers()))
		    constructors[nb++] = m;
	    }
	    publicConstructors = constructors;
	} finally {
	    LibraryImports.leaveArea(r);
	}
    }

    /* returns an iterator on the hash map of methods ; initializes the
     * the method hash map if needed */
    private Iterator _getMethodHashMapIterator(){
	if(methodHashMap == null)
	    _initMethodHash();
	return methodHashMap.values().iterator();
    }
    private Iterator _getConstructorHashMapIterator(){
	if(constructorHashMap == null)
	    _initConstructorHash();
	return constructorHashMap.values().iterator();
    }

    /**
     * Initializes the hash table that stores methods provided by this class,
     * either locally declared or inherited.
     * Overriden methods are found only once in the hash table : we only
     * store the overriding one.
     */    
    private void _initMethodHash(){
 	Class parent = vmType.getSuperclass();

	/* if we have a super class, get its method hash map */
	if(parent == null)
	    methodHashMap = new HashMap();
	else
	    methodHashMap = get(parent).getMethodHashMap();	

	/* get our declared methods */
	Method[] myDeclaredMethods = 
	    LibraryImports.getDeclaredMethods(vmType);

	/* merge the methods in the hash table 
	 * Since the hash key works on method name and args, this will
	 * take care of overriding at the same time */
	for(int i=0; i<myDeclaredMethods.length; i++){
	    MethodKey mkey = 
		new MethodKey(myDeclaredMethods[i].getName(),
			      myDeclaredMethods[i].getParameterTypes());
	    // System.out.println(vmType + " declares " + mkey);
	    methodHashMap.put(mkey, myDeclaredMethods[i]);
	}
    }

    private void _initConstructorHash() {
	constructorHashMap = new HashMap();

	/* get our declared constructors */
	Constructor[] myDeclaredConstructors = 
	    LibraryImports.getDeclaredConstructors(vmType);

	/* put the constructors in the hash table 
	 * Since the hash key works on constructor name and args, this will
	 * take care of overriding at the same time */
	for(int i=0; i<myDeclaredConstructors.length; i++){
	    MethodKey mkey = 
		new MethodKey(constructorName,
			      myDeclaredConstructors[i].getParameterTypes());
	    constructorHashMap.put(mkey, myDeclaredConstructors[i]);
	}
    }
	

    /**
     * @return a copy of the method hash table from this class ; this is 
     * used by classes to get learn what methods they inherit from their
     * super class
     * @see initMethodHash()
     */
    private HashMap getMethodHashMap(){
	if(methodHashMap == null)
	    _initMethodHash();
	return (HashMap) methodHashMap.clone();
    }
    
    /**
     * The key we use to hash methods in the method hash table
     */
    private static class MethodKey{
        static final Class[] EMPTY_ARRAY = new Class[0];
	Class[] args;
	String name;
	
	public MethodKey(String name, Class[] args){
	    this.name = name;
	    this.args = (args != null ? args : EMPTY_ARRAY);
	}

	public boolean equals(Object o){
	    return (hashCode() == o.hashCode());
	}

	public int hashCode(){
	    int c = name.hashCode();
            // methods/constructors that have certain parameter combinations
            // will yield the same hash. For example for any method of the
            // form meth(X x1, X x2) will have the same hash as the no arg
            // constructor. Similarly meth(X x, Y y) and meth(Y y, X x) will
            // has the same. So we shift the hash of the arg type by the 
            // position of the arg in an attempt to fix this.
            // This hash may still not be unique - FIXME
	    for(int i=0; i<args.length; i++)
		c ^= (args[i].hashCode() << i);
	    return c;
	}

        public String toString() {
            return name + ":" + args.length + "@" + hashCode();
        }
    }
} // class VMClass
