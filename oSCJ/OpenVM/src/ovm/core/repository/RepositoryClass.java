
package ovm.core.repository;

import ovm.core.repository.Mode;
import ovm.core.repository.TypeName;
import ovm.core.repository.TypeName.Scalar;
import ovm.util.ArrayList;
import ovm.util.Collections;
import ovm.util.List;
import ovm.util.OVMError;
import ovm.util.OVMException;
import s3.core.S3Base;
import ovm.core.domain.LinkageException;
import ovm.util.Iterator;

/**
 * RepositoryClass provides read-only access to the information extracted
 * from a Java ".class" file. Objects of this class that are stored in the
 * repository have been verified and are, by default, shared across all
 * domains that need a particular class.  To allow for sharing of symbolic
 * information the RepositoryClass does not include any domain-specific
 * data such as linking information, static fields, locks, JIT compiled
 * code.<p>
 *
 * RepositoryClass objects represent both classes and interfaces.  Use
 * the <code>getAccessMode().isInterface()</code>  method to differentiate.
 *
 * @author Jan Vitek, Christian Grothoff
 **/
public class RepositoryClass extends S3Base {

    // ---- Static Methods ---------------------------------------

    /**
     * Create a class with only a name (this is used for probing class tables).
     * @param scalar name of the class
     * @return       a class with only a name	 
     */
    public static RepositoryClass createClassProbe(Scalar scalar) {
	return new RepositoryClass(scalar);
    }

    // ---- Instance fields ---------------------------------------

    /**
     * An array of this class's attributes
     **/
    private Attribute[] attributes_;

    /**
     * This class's constant pool
     **/
    private ConstantPool constantPool_;

    /**
     * An array of the instance inner classes of this class
     **/
    private TypeName.Scalar[] instanceClasses_;

    /**
     * An array of the instance fields of this class
     **/
    private RepositoryMember.Field[] instanceFields_;

    /**
     * An array of the instance methods of this class
     **/
    private RepositoryMember.Method[] instanceMethods_;

    /**
     * An array of the type names of the interfaces this
     * class implements.
     **/
    private final TypeName.Scalar[] interfaces_;

    /**
     * The major version of this class's class file
     **/
    private final char majorVersion_;

    /**
     * The minor version of this class's class file
     **/
    private final char minorVersion_;

    /**
     * The object containing all of the modifiers for this class
     **/
    private final Mode.Class mode_;
    /**
      * The type name of this class
      **/
    protected final TypeName.Scalar name_;

    /**
     * The enclosing class type name for this class
     **/
    private TypeName.Scalar outer_;

    /**
     * The size of the original class file.
     **/
    private final int size_;

    /**
     * An array of the static inner classes of this class
     **/
    private TypeName.Scalar[] staticClasses_;

    /**
     * An array of this class's static fields
     **/
    private RepositoryMember.Field[] staticFields_;

    /**
     * An array of the static methods of this class
     **/
    private RepositoryMember.Method[] staticMethods_;

    /**
     * The type name of this class's superclass
     **/
    private final TypeName.Scalar super_;

	// ---- Constructors -----------------------------------------

    /**
     * Constructor - used to probe the hashtable -- package scoped.
     * @param scalar
     */
    RepositoryClass(Scalar scalar) {
        this.name_ = scalar;
        this.minorVersion_ = 0;
        this.majorVersion_ = 0;
        this.mode_ = null;
        this.size_ = 0;
        this.super_ = null;
        this.interfaces_ = null;
        this.outer_ = null;
        this.instanceMethods_ = null;
        this.instanceFields_ = null;
        this.instanceClasses_ = null;
        this.staticMethods_ = null;
        this.staticFields_ = null;
        this.staticClasses_ = null;
        this.attributes_ = null;
        this.constantPool_ = null;
    }

    /**
     * Create a new RepositoryClass.
     *
     * @param nm           class name
     * @param minV         minor version of class file format
     * @param majV         major version of class file format
     * @param access       modifiers
     * @param supr         the superclass  name
     * @param intf         non-null array of interface type names
     * @param outer        outer class or null if not an inner class
     * @param iMethods     non-null array of instance methods
     * @param iFields      non-null array of instance fields
     * @param iClasses     non-null array of instance classes
     * @param sMethods     non-null array of static methods
     * @param sFields      non-null array of static fields
     * @param sClasses     non-null array of static classes
     * @param attributes   non-null array of attribute types
     * @param constantPool the constant pool
     * @param size         the size of the original class file
     **/
    RepositoryClass(
        TypeName.Scalar nm,
        char minV,
        char majV,
        Mode.Class access,
        TypeName.Scalar supr,
        TypeName.Scalar[] intf,
        TypeName.Scalar outer,
        RepositoryMember.Method[] iMethods,
        RepositoryMember.Field[] iFields,
        TypeName.Scalar[] iClasses,
        RepositoryMember.Method[] sMethods,
        RepositoryMember.Field[] sFields,
        TypeName.Scalar[] sClasses,
        Attribute[] attributes,
        ConstantPool constantPool,
        int size) {
        this.name_ = nm;

        if (intf != null)
            for (int i = 0; i < intf.length; i++)
                assert(intf[i] != null);

        this.minorVersion_ = minV;
        this.majorVersion_ = majV;
        this.mode_ = access;
        this.size_ = size;
	// FIXME: Why is any of this bullshit done.  
        if (supr == null || access.isInterface()) {
            this.super_ = JavaNames.java_lang_Object;
        } else {
            this.super_ = supr;
        }
        this.interfaces_ = intf;
        this.outer_ = outer;
        this.instanceMethods_ = iMethods;
        this.instanceFields_ = iFields;
        this.instanceClasses_ = iClasses;
        this.staticMethods_ = sMethods;
        this.staticFields_ = sFields;
        this.staticClasses_ = sClasses;
        this.attributes_ = attributes;
        this.constantPool_ = constantPool;
    }

    // ---- Methods -----------------------------------------------------

    public RepositoryClass createClassProbe() {
        return null;
    } //FIXME

    /**
     * Returns this class's name.
     * @return the name of this class
     **/
    public TypeName.Scalar getName() {
	// TODO Auto-generated method stub
	return name_;
    }

    /**
     * Returns the super class' name. <p> If this class is the root of
     * the hierarchy, we have <code>getName()==getSuper()</code>.
     * @return the name of this class's super class
     */
    public TypeName.Scalar getSuper() {
	return super_;
    }	

    /**
     * Returns the type name of the outer class for inner classes or null
     * for top level classes.
     * @return name of the outer class of this class, or <code>null
     *         </code> if this is not an inner class
     */
    public TypeName.Scalar getOuter() {
	return outer_;
    }
	
    /**
     * Returns the constant pool for this class
     * @return the constant pool
     */
    public ConstantPool getConstantPool() {
	return constantPool_;
    }

    /**
     * Return the mode for this class.
     * KP: shouldn't it be called getModifiers ?
     * @return the access modifiers object for this class
     */
    public Mode.Class getAccessMode() {
	return mode_;
    }

    /**
     * Return an array of the attributes declared for this class. This
     * array is not null.
     * @return the array of declared attributes for this class
     */
    public Attribute[] getAttributes() {
	return attributes_;
    }

    /**
     * Return the major version of the class file format.
     * @return the major version of the class file format
     */
    public int getMajorVersion() {
	return majorVersion_;
    }

    /**
     * Return the minor version of the class file format.
     * @return the minor version of the class file format
     */
    public int getMinorVersion() {
	return minorVersion_;
    }

    /**
     * Return the size of the original class file (in bytes)
     * @return the size of the original class file
     */
    public int getSize() {
	return size_;
    }

    /**
     * Return  the static fields of this class.
     * @return the array of static fields for this class
     */
    public RepositoryMember.Field[] getStaticFields() {
	return staticFields_;
    }
	
    /**
     * Return the static inner classes of this class.
     * @return an array of the type names of the static inner classes declared
     *         by this class
     */	
    public TypeName.Scalar[] getStaticInnerClasses() {
	return staticClasses_;
    }

    /**
     * Return the static methods of this class.
     * @return the array of static methods for this class
     */
    public RepositoryMember.Method[] getStaticMethods() {
	return staticMethods_;
    }

    /**
     * Return the instance fields of this class.
     * @return the array of instance fields for this class
     */
    public RepositoryMember.Field[] getInstanceFields() {
	return instanceFields_;
    }
	
    /**
     * Return the instance inner classes of this class
     * @return an array of the type names of the instance inner classes 
     *         declared by this class
     */
    public TypeName.Scalar[] getInstanceInnerClasses() {
	return instanceClasses_;
    }

    /**
     * Return  the instance methods of this class.
     * @return the array of instance methods for this class
     */
    public RepositoryMember.Method[] getInstanceMethods() {
	return instanceMethods_;
    }

    /**
     * Returns a non-null array of interface names.  FIXME? change
     * name to <code>getLocalIntefaces()</code> to avoid confusion
     * (the array does not include parent interfaces)
     * @return the array of type names of interfaces directly implemented by 
     *         this class
     */
    public TypeName.Scalar[] getInterfaces() {
	return interfaces_;
    }

    /**
     * Return the field corresponding to the given string.
     * @param fieldSelectorString field name String.
     * @return                    the field or <code>null</code>.
     */
    public RepositoryMember.Field getField(String fieldSelectorString) {
	UnboundSelector sel =
	    RepositoryUtils.makeUnboundSelector(fieldSelectorString);
	return getField(sel.asField());
    }

    /**
     * Returns the field described by the unbound selector argument.    
     * <p>
     * Linear search for the <code>RepositoryMember.Field</code>
     * using pointer equality of <code>RepositoryUnboundSelectors</code>
     * </p>
     * @param field the unbound selector for this field
     * @return the field or <code>null</code>.     
     **/
    public RepositoryMember.Field getField(UnboundSelector.Field field) {
	for (int i = 0; i < instanceFields_.length; i++)
	    if (instanceFields_[i].getUnboundSelector() == field)
		return instanceFields_[i];
	for (int i = 0; i < staticFields_.length; i++)
	    if (staticFields_[i].getUnboundSelector() == field)
		return staticFields_[i];
	return null;
    }

    /**     
     * Returns the method corresponding to the input <code>String</code>.
     * 
     * @param methodSelectorString name of method to be retrieved
     * @return <code>null</code> if method not found in this class; 
     *         otherwise return the method.
     */
    public RepositoryMember.Method getMethod(String methodSelectorString) {
	UnboundSelector sel =
	    RepositoryUtils.makeUnboundSelector(methodSelectorString);
	return getMethod(sel.asMethod());
    }
    
    /**     
     * Given an unbound selector, return the method defined in this class.
     * <p>
     * Linear search for the <code>RepositoryMember.Method</code>
     * using pointer equality of <code>RepositorySelectors<code>.
     *
     * @param selector the unbound selector for this method
     * @return <code>null</code> if not found in this class;
     *         otherwise return the method.   
     */
    public RepositoryMember.Method getMethod(UnboundSelector.Method selector) {
	for (int i = 0; i < instanceMethods_.length; i++) {
	    if (instanceMethods_[i].getUnboundSelector() == selector)
		return instanceMethods_[i];
	}
	for (int i = 0; i < staticMethods_.length; i++) {
	    if (staticMethods_[i].getUnboundSelector() == selector)
		return staticMethods_[i];
	}
	return null;
    }

    /**
     * Returns true if this class has a parent and false if this class
     * is the root of hierarchy.
     * @return true if this class is not the root of the hierarchy, else
     *         false
     */	
    public boolean hasSuper() {
	return getSuper() != getName();
    }

    /**
     * Determine if this class is an inner class
     * @return true if this is an inner class, else false
     */
    public boolean isInner() {
	return name_.toString().indexOf("$") != -1;
    }

    /**
     * Return this class's hashcode
     * @return the hashcode for this class
     **/
    public int hashCode() {
        return name_.hashCode();
    }


    /**
     * Determines whether or not this class object is equal to
     * another object by name comparison.
     * @return true if the input object is a class object with the
     *         same name; otherwise, false.
     **/
    public boolean equals(Object cl) {
	if (cl == null)
	    return false;
	if (cl == this)
	    return true;
	if (cl instanceof RepositoryClass)
	    return name_ == ((RepositoryClass) cl).getName();
	else
	    return false;
    }

    /**
     * Returns the <code>String</code> representation of the class
     * name.
     * @return the <code>String</code> representation of this
     *         class's name.
     * @see ovm.core.repository.TypeName#toString
     **/
    public String toString() {
        return name_.toString();
    }

    /**
     * Accept method for the visitor pattern
     * @param visitor a visitor object to be used to visit this object
     */
    public void accept(RepositoryProcessor visitor) {
	visitor.visitClass(this);
    }

    /**
     * Visit the attributes of this class
     * @param x a visitor
     */
    public void visitAttributes(RepositoryProcessor x) {
        for (int i = 0; i < attributes_.length; i++)
            attributes_[i].accept(x);
    }

    /**
     * Visit the header of this class: class name, versions,
     * superclass name, outer class name, class access modifiers,
     * and interfaces
     * @param x a visitor
     */
    public void visitHeader(RepositoryProcessor x) {
        x.visitClassName(name_);
        x.visitVersions(minorVersion_, majorVersion_);
        x.visitSuperName(super_);
        x.visitOuterName(outer_);
        x.visitClassMode(mode_);
        visitInterfaces(x);
    }
    /**
     * Visit instance fields of this RepositoryClass.
     * @param x a visitor for this RepositoryClass.
     **/
    public void visitInstanceFields(RepositoryProcessor x) {
        for (int i = 0; i < instanceFields_.length; i++)
            instanceFields_[i].accept(x);
    }

    /**
     * Visit instance inner class type names of this RepositoryClass
     * @param x a visitor for this RepositoryClass.
     **/
    public void visitInstanceInnerClasses(RepositoryProcessor x) {
        for (int i = 0; i < instanceClasses_.length; i++)
            x.visitInstanceInnerClass(instanceClasses_[i]);
    }
    /**
     * Visit instance methods of this RepositoryClass.
     * @param x a visitor for this RepositoryClass.
     **/
    public void visitInstanceMethods(RepositoryProcessor x) {
        for (int i = 0; i < instanceMethods_.length; i++)
            instanceMethods_[i].accept(x);
    }

    /**
     * Visit type names of the directly implemented
     * interfaces of this RepositoryClass.
     * @param x a visitor for this RepositoryClass.
     **/
    public void visitInterfaces(RepositoryProcessor x) {
        for (int i = 0; i < interfaces_.length; i++)
            x.visitInterface(interfaces_[i]);
    }

    /**
     * Visit (instance and static) fields and methods of this class.
     * @param x a visitor
     */        
    public void visitMembers(RepositoryProcessor x) {
        visitInstanceMethods(x);
        visitStaticMethods(x);
        visitStaticFields(x);
        visitInstanceFields(x);
        visitStaticInnerClasses(x);
        visitInstanceInnerClasses(x);
    }
    /**
     * Visit static fields of this RepositoryClass.
     * @param x a visitor for this RepositoryClass.
     **/
    public void visitStaticFields(RepositoryProcessor x) {
        for (int i = 0; i < staticFields_.length; i++)
            staticFields_[i].accept(x);
    }

    /**
     * Visit static inner class type names of this RepositoryClass
     * @param x a visitor for this RepositoryClass.
     **/
    public void visitStaticInnerClasses(RepositoryProcessor x) {
        for (int i = 0; i < staticClasses_.length; i++)
            x.visitStaticInnerClass(staticClasses_[i]);
    }
    /**
     * Visit static methods of this RepositoryClass.
     * @param x a visitor for this RepositoryClass.
     **/
    public void visitStaticMethods(RepositoryProcessor x) {
        for (int i = 0; i < staticMethods_.length; i++)
            staticMethods_[i].accept(x);
    }

    // --------------------------------------------------------------------
    // ------------ Inner Classes and Interfaces
    // --------------------------------------------------------------------
    
    /**
     * Concrete builder class for <code>Class</code> objects.
     * @see ovm.core.repository.RepositoryBuilder
     **/
    static public class Builder extends RepositoryBuilder {
	
	
	/**
	 * The default access modifiers for the class to be built
	 **/
	final static Mode.Class DEFAULT_ACCESS_MODE =
	    Mode.makePublicClass().lub(
						 Mode.makeFinalClass());
	
	/**
	 * The default major version of the class file of the class to be built
	 **/
	final static char DEFAULT_MAJOR_VERSION = '\u0000';
	
	/**
	 * The default minor version of the class file of the class to be built
	 **/
	final static char DEFAULT_MINOR_VERSION = '\u0000';
	
        /**
         * The size of the original class file (in bytes)
         */
	private int classSize;
	
	/**
		 * Constant pool for the class to be built
		 **/
	private ConstantPool constantPool;
	
	/**
	 * List of <code>RepositoryMember.Field</code> objects of the class 
	 * to be built
	 **/
	private List fieldInfos;
	
	/**
	 *  List of unbound selectors for fields of the class to be built
	 **/
	private List fieldSelectors;
	
	/**
	 * List of type names of the instance inner classes of the class to be 
	 * built
	 **/
	private List instanceClasses;
	
	// ----------- MEMBER INFO ----------------------------
	/**
	 * The list of type names of interfaces implemented by the class to be
	 * built
	 **/
	private List interfaces;
	
	/**
	 * Major class file version of the class to be built
	 **/
	private char majorVersion;
	
	/**
	 * Hash table mapping the unbound selectors of methods in the class to 
	 * be built to their <code>RepositoryMember.Method</code> objects
	 **/
	private HTUnboundSel2Method methods;
	
	// ----------- CLASS INFO ----------------------------
	/**
	 * Minor class file version of the class to be built
	 **/
	private char minorVersion;
	
	/**
	 * Access modifiers of the class to be built
	 **/
	private Mode.Class mode;
	
	/**
	 * Type name of the class to be built
	 **/
	private TypeName.Scalar name;
	
	/**
	 * Outer class type name of the class to be built
	 **/
	private TypeName.Scalar outerName;
	
	/**
	 * List of type names of the static inner classes of the class to be built
	 **/
	private List staticClasses;
	
	/**
	 * Super class type name of the class to be built
	 **/
	private TypeName.Scalar superName = JavaNames.java_lang_Object;
	
	// -------------- END FIELDS --------------------    
	
	/**
	 * Initialize this class builder by calling its {@link #reset} method
	 **/
	public Builder() {
	    reset();
	}
	
	/**
	 * Initialize the class builder with an existing class so that this
	 * builder will build copies of the input class
	 * @param cl the class to use as a template for this builder
	 **/
	public Builder(RepositoryClass cl) {
	    // remove all state
	    reset();
	    
	    // Grab all of the class information from the input class and
	    // declare it in this builder
	    // FIXME ...
	    try {
		majorVersion = (char) cl.getMinorVersion();
		minorVersion = (char) cl.getMajorVersion();
		classSize = cl.getSize();
		mode = cl.getAccessMode();
		name = cl.getName();
		superName = cl.getSuper();
		TypeName.Scalar[] s = cl.getInterfaces();
		int i;
		for (i = 0; i < s.length; i++) {
		    declareInterface(s[i]);
		}
		RepositoryMember.Method[] m = cl.getInstanceMethods();
		for (i = 0; i < m.length; i++) {
		    declareMethod(m[i]);
		}
		m = cl.getStaticMethods();
		for (i = 0; i < m.length; i++)
		    declareMethod(m[i]);
		RepositoryMember.Field[] f = cl.getInstanceFields();
		for (i = 0; i < f.length; i++)
		    declareField(f[i]);
		f = cl.getStaticFields();
		for (i = 0; i < f.length; i++)
		    declareField(f[i]);
		Attribute[] a = cl.getAttributes();
		for (i = 0; i < a.length; i++)
		    declareAttribute(a[i]);
		TypeName.Scalar[] classes = cl.getInstanceInnerClasses();
		for (i = 0; i < classes.length; i++) {
		    declareInstanceInnerClass(classes[i]);
		}
		classes = cl.getStaticInnerClasses();
		for (i = 0; i < classes.length; i++) {
		    declareStaticInnerClass(classes[i]);
		}
	    } catch (LinkageException e) {
		// impossible!
		throw e.fatal();
	    }
	    
	}
	
	/**
	 * Create a <code>RepositoryClass</code> according to the 
	 * specifications of this builder.
	 * @return a <code>RepositoryClass</code> object made according
	 *         to this builder's specifications
	 * @throws ClassFormatError if the class format is invalid
	 */
	public RepositoryClass build() throws ClassFormatError {
	    int approxSize = methods.size() / 2;
	    List instanceMethods = new ArrayList(approxSize);
	    List staticMethods = new ArrayList(approxSize);
	    
	    for (UnboundSelector.Method.Iterator i = methods.getIterator();
		 i.hasNext();
		 ) {
		RepositoryMember.Method m = methods.get(i.next());
		if (m.getMode().isStatic()) {
		    staticMethods.add(m);
		} else {
		    instanceMethods.add(m);
		}
	    }
	    approxSize = fieldSelectors.size() / 2;
	    List instanceFields = new ArrayList(approxSize);
	    List staticFields = new ArrayList(approxSize);
	    for (Iterator i = fieldInfos.iterator();
		 i.hasNext();
		 ) {
		RepositoryMember.Field f = (RepositoryMember.Field) i.next();
		if (f.getMode().isStatic()) {
		    staticFields.add(f);
		} else {
		    instanceFields.add(f);
		}
	    }
	    
	    RepositoryMember.Field[] staticFieldsArr;
	    int size = staticFields.size();
	    if (size > 0) {
		staticFieldsArr =
		    (RepositoryMember.Field[]) staticFields.toArray(
								    new RepositoryMember.Field[size]);
	    } else {
		staticFieldsArr = RepositoryMember.Field.EMPTY_ARRAY;
	    }
	    
	    TypeName.Scalar[] staticClassesArr;
	    size = staticClasses.size();
	    if (size > 0) {
		staticClassesArr = new TypeName.Scalar[size];
		staticClasses.toArray(staticClassesArr);
	    } else {
		staticClassesArr = TypeName.Scalar.EMPTY_SARRAY;
	    }
	    
	    TypeName.Scalar[] instanceClassesArr;
	    size = instanceClasses.size();
	    if (size > 0) {
		instanceClassesArr = new TypeName.Scalar[size];
		instanceClasses.toArray(instanceClassesArr);
	    } else {
		instanceClassesArr = TypeName.Scalar.EMPTY_SARRAY;
	    }
	    
	    TypeName.Scalar[] interfacesArr;
	    size = interfaces.size();
	    if (size > 0) {
		interfacesArr =
		    (TypeName.Scalar[]) interfaces.toArray(
						new TypeName.Scalar[size]);
	    } else {
		interfacesArr = TypeName.Scalar.EMPTY_SARRAY;
	    }
	    
	    RepositoryMember.Field[] instanceFieldsArr;
	    size = instanceFields.size();
	    if (size > 0) {
		instanceFieldsArr =
		    (RepositoryMember.Field[]) instanceFields.toArray(
								      new RepositoryMember.Field[size]);
	    } else {
		instanceFieldsArr = RepositoryMember.Field.EMPTY_ARRAY;
	    }
	    
	    RepositoryMember.Method[] instanceMethodsArr;
	    size = instanceMethods.size();
	    if (size > 0) {
		instanceMethodsArr =
		    (RepositoryMember.Method[]) instanceMethods.toArray(
									new RepositoryMember.Method[size]);
	    } else {
		instanceMethodsArr = RepositoryMember.Method.EMPTY_ARRAY;
	    }
	    
	    RepositoryMember.Method[] staticMethodsArr;
	    size = staticMethods.size();
	    if (size > 0) {
		staticMethodsArr =
		    (RepositoryMember.Method[]) staticMethods.toArray(
								      new RepositoryMember.Method[size]);
	    } else {
		staticMethodsArr = RepositoryMember.Method.EMPTY_ARRAY;
	    }
	    
	    if (mode == null) {
		mode = DEFAULT_ACCESS_MODE;
	    }
	    RepositoryClass cls =
		new RepositoryClass(
					name,
					minorVersion,
					majorVersion,
					mode,
					superName,
					interfacesArr,
					outerName,
					instanceMethodsArr,
					instanceFieldsArr,
					instanceClassesArr,
					staticMethodsArr,
					staticFieldsArr,
					staticClassesArr,
					super.getAttributes(),
					constantPool,
					classSize);
			return cls;
		}

	/**
	 * Declare the constant pool for class objects to be built with this
	 * builder
	 * @param cp the constant pool of the class to be built
	 */
	public void declareConstantPool(ConstantPool cp) {
	    this.constantPool = cp;
	}

        // Declare a deprecated attribute for this builder
		public final void declareDeprecated() {
		    declareAttribute(Attribute.deprecatedClass);
		}

		/**
		 * Declare a member field for classes to be built with this builder 
		 * object
		 * @param field a field to be declared in this builder
		 */
		public void declareField(RepositoryMember.Field field) {
			UnboundSelector.Field selector = field.getUnboundSelector();
			assert !fieldSelectors.contains(selector)
			    : "redeclaring field " + selector;
			fieldSelectors.add(selector);
			fieldInfos.add(field);
		}


	/**
	 * Declare an inner classes attribute for the class which will
	 * be built with this builder
	 * @param innerClass the array of inner class typenames for
	 *                   the inner classes declared in this attribute
	 * @param outerClass the array of enclosing class typenames for
	 *                   the inner classes declared in this attribute
	 * @param shortNameIndex the array of indices into the repository
	 *                       for the utf8string simple names (as
	 *                       declared in the class file) for the
	 *                       inner classes declared in this attribute
	 * @param m the array of modifiers for the inner classes
	 *          declared in this attribute
	 */
	public void declareInnerClasses(TypeName.Scalar[] innerClass,
					TypeName.Scalar[] outerClass,
					int[] shortNameIndex,
					Mode.Class[] m) {
	    declareAttribute(new Attribute.InnerClasses(innerClass,
							outerClass,
							shortNameIndex,
							m));
	}

		/**
		 * Declare an instance inner class of the class which will
		 * be built with this builder
		 * @param cls the type name of the inner class of the class to
		 *            be built
		 */
		public void declareInstanceInnerClass(TypeName.Scalar cls) {
			assert !instanceClasses.contains(cls)
			    : "redeclaring class " + cls;
			instanceClasses.add(cls);
		}

		/**
		 * Declare a static inner class of the class which will be 
		 * built with this builder
		 * @param cls the type name of the inner class of the class to be
		 *            built
		 */
		public void declareStaticInnerClass(TypeName.Scalar cls) {
			assert !staticClasses.contains(cls)
			    : "redeclaring class " + cls;
			staticClasses.add(cls);
		}

		/**
		 * Declare an interface to be implemented by classes to be built with
		 * this builder
		 * @param iface the type name of the interface implemented by
		 *              the class to be built
		 */
		public void declareInterface(TypeName.Scalar iface) {
			if (interfaces == Collections.EMPTY_LIST)
				interfaces = new ArrayList();
			interfaces.add(iface);
		}

	/**
	 * Add a new method <code>newMethod</code>.
	 * @param method the method to be declared in this builder
	 * @throws RedeclarationError if a method with
	 * the same selector as <code>newMethod</code>'s
	 * UnboundSelector has already been declared
	 */
	public void declareMethod(RepositoryMember.Method method)
	    throws LinkageException.ClassFormat
	{
	    UnboundSelector.Method selector = method.getUnboundSelector();
	    if (methods.get(selector) != null) {
		throw new LinkageException.ClassFormat
				    (name + ": duplicate method :" + selector);
			}
			redeclareMethod(method);
		}

        // Declare a synthetic attribute for this builder. 
		public final void declareSynthetic() {
			if (freeze_attributes_)
				fail("Attribute declaration on frozen attribute set");
			declareAttribute(Attribute.Synthetic.Class.SINGLETON);
		}

		/**
		 * Get the type name for the class being built with this builder object
		 */
		public TypeName.Scalar getName() {
			if (name == null) {
				throw new OVMError("name not set yet");
			}
			return this.name;
		}

		/**
		 * Add a new method <code>method</code> if a method with
		 * such the same UnboundSelector has not been declared,
		 * otherwise replace the existing method with
		 * <code>method</code>.
		 * @param method the method to be declared in this builder
		 */
		public void redeclareMethod(RepositoryMember.Method method) {
			methods.put(method.getUnboundSelector(), method);
		}

		/**
		 * Remove the method specified by <code>selector</code>.
		 * @param selector the unbound selector of the method being
		 *                 removed
		 * @return the method info being removed or <code>null</code>
		 *        if no method with such selector was declared.
		 */
		public RepositoryMember.Method removeMethod(
			UnboundSelector.Method selector) {
			RepositoryMember.Method res = methods.get(selector);
			methods.remove(selector);
			return res;
		}

		// remove all state and reinitialize
		public void reset() {
		    super.reset();
		    majorVersion = DEFAULT_MAJOR_VERSION;
		    minorVersion = DEFAULT_MINOR_VERSION;
		    classSize = 0;
		    mode = null;
		    name = null;
		    superName = JavaNames.java_lang_Object;
		    interfaces = Collections.EMPTY_LIST;
		    outerName = null;
		    methods = new HTUnboundSel2Method();
		    fieldSelectors = new ArrayList();
		    fieldInfos = new ArrayList();
		    staticClasses = new ArrayList();
		    instanceClasses = new ArrayList();
		}
		
		/**
		 * Set the access modifiers object of classes to be built with this
		 * class
		 * @param mode the access modifiers object of the class to be built
		 */		
		public void setAccessMode(Mode.Class mode) {
			this.mode = mode;
		}

	public Mode.Class getAccessMode() { return mode; }

		/**
		 * Set the major class file version number for this class
		 * @param major the major version number
		 */		
		public void setMajorVersion(int major) {
			majorVersion = (char) major;
		}
		
		/**
		 * Set the minor class file version number for this class
		 * @param minor the minor version number
		 */		
		public void setMinorVersion(int minor) {
			minorVersion = (char) minor;
		}

		/**
		 * Set the type name for classes to be built with this builder object
		 * @param name the type name for classes to be built with this builder
		 */
		public void setName(TypeName.Scalar name) {
			this.name = name;
			//	Iterator i = methods.getIterator();
		}

	/**
	 * Set the outer class type name for class objects to be built
	 * with this builder
	 * @param outerName the outer class type name
	 * @throws RedeclarationError if the outer class type has already
	 *                            been declared
	 */
	public void setOuterClassName(TypeName.Scalar outerName)
	    throws LinkageException.ClassFormat
	{
	    if (this.outerName != null) {
		throw new LinkageException.ClassFormat
		    (name + ": duplicate inner class record");
	    }
	    //	d("outer name is " + outerName);
	    this.outerName = outerName;
	}
		
		/**
		 * Set the size of the class file to be size bytes.
		 * @param size the size to set the class file size to
		 **/		
		public void setSize(int size) {
			this.classSize = size;
		}

		/**
		 * Set the super class type name for the class objects to be
		 * built with this builder -
		 * ATTENTION: this will eventually require updating all the
		 * super() calls!
		 * @param superName the super class type name
		 */
		public void setSuperClassName(TypeName.Scalar superName) {
			this.superName = superName;
		}

		/**
		 * Set the class file versions for this class
		 * @param major the major version number
		 * @param minor the minor version number
		 */
		public void setVersions(int major, int minor) {
			majorVersion = (char) major;
			minorVersion = (char) minor;

		}

	} // END RepositoryClass.Builder

	public interface Action {
		/**
		 * Do something with a repository class.
		 * @param repoClass the class to process.
		 */
		public void process(RepositoryClass repoClass) throws OVMException;   
        
	} // END RepositoryClass.Action


	
} // END RepositoryClass
