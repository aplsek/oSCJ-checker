/**
 * @file ovm/tools/modifiers/Mode.java
 * @author Ted Witkamp
 * @author Jan Vitek
 * @version 1.1
 **/
package ovm.core.repository;

import java.io.OutputStream;
import java.io.IOException;

import ovm.core.services.memory.MemoryPolicy;
import ovm.services.bytecode.JVMConstants;

/**
 * Contains the factory singleton for creation and retrieval of all class, 
 * interface, field, and method modes.
 * <p>
 * Modes are immutable values representing the modifiers of the
 * following Java language elements: classes, interfaces, fields and
 * methods.
 * <br>
 * We impose a partial ordering on modes such that:
 * <pre>
 *          private < default < protected < public
 *          non-final < final 
 *          non-synchronized < synchronized
 *          non-static < static
 *          non-transient < transient
 *          non-volatile < volatile
 *          non-synthetic < synthetic
 * </pre>
 * Although the default implementation uses the flyweight pattern, other
 * implementations may not exist, so use == with wild abandon.
 * <p>
 * Modes are very similar to access flags in Java bytecode, however,
 * Modes carry one extra bit of information.  {@link AllModes#isSynthetic}
 * returns true when a class or member carries a
 * <code>Synthetic</code> attribute.  (See
 * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#80128">JVMS section 4.7.6</a>
 * for details.)
 **/
public abstract class Mode extends RepositorySymbol implements JVMConstants {

    // First unused bit in our mode word
    private static final int ACC_SYNTHETIC = MAX_ACC_FLAG << 1;

    private static final int N_CLASS_MODES = ACC_SYNTHETIC << 1;
    private static final int N_METHOD_MODES = N_CLASS_MODES;
    private static final int N_FIELD_MODES = N_CLASS_MODES;

    // Ignore undefined bits in access modifier words.
    private static final int ACC_MASK = ACC_SYNTHETIC - 1;
    private static final int FIELD_MODE_MASK = (ACC_TRANSIENT << 1) - 1;
    
    /**
     * The base class for all concrete implementations of <code>Modes</code>.
     *
     * This class provides final implementations of all the basic methods
     * needed for mode object creation.
     **/
    public static abstract class AllModes extends Mode {

        /**
         * Use the <code>Builder</code> to build a mode in stages.
         * Subclasses define <code>build()</code> methods that return
         * mode objects representing the desired modes.
         **/
        public static abstract class Builder {
            /**
             * The flags that are set in the current state of this
             * builder, and will be set in all mode objects
             * built with it.
             **/
            protected int flags;
            /**
             * Make the builder ready for reuse, reinitializing to
             * remove state.
             **/
            public void reset() {
                flags = 0;
            }
            /**
             * Configure the value of the <code>final</code> flag
             * for mode objects to be created with this builder.
             * @param f true if modes created with this builder should
             *           be <code>final</code>, otherwise, false.
             **/
            public void setFinal(boolean f) {
                setFlag(f, ACC_FINAL);
            }

            /**
             * Set a particular flag or set of flags for mode objects to be 
             * built with this builder. Multiple flags can be set by
             * <code>OR</code>ing flags to form the argument mask (e.g.
             * <code>(ACC_FINAL | ACC_PRIVATE)</code>.
             * @param f true if the indicated flag or flags should be set, 
             *          false if unset
             * @param mask the set of flags that should be set
             * @see ovm.services.bytecode.JVMConstants
             **/
            protected void setFlag(boolean f, int mask) {
                if (f) {
                    flags |= mask;
                } else {
                    flags &= ~mask;
                }
            }
            /**
             * Configure the value of the <code>public</code> flag
             * for mode objects to be created with this builder.
             * @param f true if modes created with this builder should
             *          be <code>public</code>, otherwise, false.
             **/
            public void setPublic(boolean f) {
                setFlag(f, ACC_PUBLIC);
            }

        }

        /**
         * The mask which should be <code>AND</code>ed with mode
         * bits in order to get only the access mode bits.
         **/
        static final int ACCESS_MODE_MASK = 0x0007;

        // Statics -------------------------------------------------------

        /**
         * The mask which should be <code>AND</code>ed with mode
         * bits in order to get the modifiers in a mode which are
         * not access modifiers.
         **/
        static final int FLAG_BITS_MASK = 0x07f8;

        // Fields -------------------------------------------------------

        /**
         * The mode bits for this mode object
         * added protected
         **/
        protected final int mode_;

        /**
         * The <code>String</code> representation of the mode
         * bits which have been set in this mode.
         **/
        protected String toString_;

        // Constructors -------------------------------------------------------

        /**
         * Constructor which initializes the mode bits and constructs
         * the <code>String</code> representation of the mode. Since
         * there is a clash between the bits for 
         * {@link ovm.services.bytecode.JVMConstants#ACC_SYNCHRONIZED ACC_SYNCHRONIZED} 
         * and {@link ovm.services.bytecode.JVMConstants#ACC_SUPER ACC_SUPER},
         * the string will not be fully built in this constructor for
         * method mode objects and must be handled in the subclass 
         * constructor.
         *
         * <p>Once the mode field has been set for this object, it
         * is immutable.</p>
         *
         * @param s the mode bits to be set for this mode object
         **/
        AllModes(int s) {
            mode_ = s;
            String st = "";

            // Build the mode string.
	    if (isSynthetic())
		st += "<synthetic> ";
            if (isFinal())
                st += "final ";
            if (isStatic())
                st += "static ";
            // Note: there is a clash between ACC_SYNCHRONIZED and ACC_SUPER.
            // the string can not be fully built for methods.
            if (isAbstract())
                st += "abstract ";
            if (isVolatile())
                st += "volatile ";
            if (isTransient())
                st += "transient ";
            if (isNative())
                st += "native ";
            if (isPrivate())
                st += "private ";
            else if (isPublic())
                st += "public ";
            else if (isProtected())
                st += "protected ";

            toString_ = st;
        }

        // Methods -------------------------------------------------------

        /**
         * Get the access mode bits of this <code>Mode</code> object.
	 * Access mode bits do <b>not</b> include an indication of
	 * whether this mode is synthetic.
         * @return the mode bits of this object
         **/
        final public int getMode() {
            return mode_ & ACC_MASK;
        }
        /**
	 * The glb returns a mode that has the most restrictive access mode,
         * and the intersection of the other modifiers. 
         * <code>this.mode_</code> will not be modified.
         * 
         * @param m the mode bits to generate the greatest lower bound
         *          from
         * @return a new set of mode bits containing the most restrictive
         *         access mode of <code>this.mode_</code> and <code>m</code>,
         *         and the intersection of the two modes' flag bits
         **/
        final protected int glb(int m) {
            if (lte_access(m))
                return m | ((m | mode_) & FLAG_BITS_MASK);
            else
                return mode_ | ((m | mode_) & FLAG_BITS_MASK);
        }

        /**
         * Returns mode object's hashcode.
         * @return the hashcode
         **/
        final public int hashCode() {
            return mode_;
        }

        /** 
         * Determines if this class mode is <code>abstract</code>. 
         * @return true if this mode is <code>abstract</code>, else false.
         **/
        final public boolean isAbstract() {
            return (mode_ & ACC_ABSTRACT) != 0;
        }

        /** 
         * Determines if this access mode is package-scoped. 
         * @return true if this mode is package-scoped, else false
         **/
        public boolean isDefault() {
            return (mode_ & ACCESS_MODE_MASK) == 0;
        }

        /** 
         * Determines if this mode is <code>final</code>.
         * @return true if this mode is <code>final</code>, else false.
         **/
        final public boolean isFinal() {
            return (mode_ & ACC_FINAL) != 0;
        }

        /** 
         * Determines if this class mode indicates an interface. 
         * @return true if this mode indicates an interface, else false.
         **/
        final public boolean isInterface() {
            return (mode_ & ACC_INTERFACE) != 0;
        }

        /** 
         * Determines if this mode is <code>native</code>.
         * @return true if this mode is <code>native</code>, else false.
         **/
        final public boolean isNative() {
            return (mode_ & ACC_NATIVE) != 0;
        }

        /** 
         * Determines if this mode is <code>private</code>.
         * @return true if this mode is <code>private</code>, else false.
         **/
        final public boolean isPrivate() {
            return (mode_ & ACC_PRIVATE) != 0;
        }

        /**
         * Determines if this mode is <code>protected</code>. 
         * @return true if this mode is <code>protected</code>, else false.
         **/
        final public boolean isProtected() {
            return (mode_ & ACC_PROTECTED) != 0;
        }

        /** 
         * Determines if this access mode is <code>public</code>. 
         * @return true if this mode is <code>public</code>, else false.
         **/
        final public boolean isPublic() {
            return (mode_ & ACC_PUBLIC) != 0;
        }

        /**
         * Determines if this mode is <code>static</code>.
         * @return true if this mode is <code>static</code>, else false.
         **/
        final public boolean isStatic() {
            return (mode_ & ACC_STATIC) != 0;
        }

        /** 
         * Determines if this mode is <code>synchronized</code>.
         * @return true if this mode is <code>synchronized</code>, else false.
         **/
        final public boolean isSynchronized() {
            return (mode_ & ACC_SYNCHRONIZED) != 0;
        }

        /** 
         * Determines if this mode is <code>transient</code>.
         * @return true if this mode is <code>transient</code>, else false.
         **/
        final public boolean isTransient() {
            return (mode_ & ACC_TRANSIENT) != 0;
        }

        /** 
         * Determines if this mode is <code>volatile</code>.
         * @return true if this mode is <code>volatile</code>, else false.
         **/
        final public boolean isVolatile() {
            return (mode_ & ACC_VOLATILE) != 0;
        }

	final public boolean isSynthetic() {
	    return (mode_ & ACC_SYNTHETIC) != 0;
	}

        /**
         * Determines if the access modifiers of this object
         * are less restrictive than those in the mode in the argument.
         * <p>
         * This is determined according to the following partial order: 
         * <pre>     
         *    public > protected > default > private
         * </pre>
         * @param m the mode bits to compare against
         * @return true if this.mode_ less restrictive than m, else
         *         false
         **/
        final protected boolean lte_access(int m) {
            int m1 = mode_ & ACCESS_MODE_MASK;
            int m2 = m & ACCESS_MODE_MASK;
            if (m1 == ACC_DEFAULT)
                m1 = 3;
            else if (m1 == ACC_PUBLIC)
                m1 = 5;
            if (m2 == ACC_DEFAULT)
                m2 = 3;
            else if (m2 == ACC_PUBLIC)
                m2 = 5;
            return m1 >= m2;
        }

        /** 
             * The lub returns a mode that has the least restrictive access mode 
         * and the union of the other modifiers. <code>this.mode_</code> will 
         * not be modified.
         *
         * @param m the mode bits to generate the least upper bound
         *          from
         * @return a new set of mode bits containing the least restrictive
         *         access mode of <code>this.mode_</code> and <code>m</code>,
         *         and a union of the two modes' flag bits
         **/
        final protected int lub(int m) {
            if (lte_access(m))
                return mode_ | ((m | mode_) & FLAG_BITS_MASK);
            else
                return m | ((m | mode_) & FLAG_BITS_MASK);
        }
        /**
         * Return a new set of mode bits with fewer flags. Any flag that
         * is set in the argument is removed from this. Thus:
         * <pre>
         *    makeSynchronized().lub(makeFinal()).restrict(makeFinal())
         * </pre>
         * is equal to
         * <pre>
         *    makeSynchronized()
         * </pre>
         * (where <code>makeX</code> has the obvious meaning)
         * For access modifiers, this method returns the most restrictive:
         * <pre>
         *    makePublic().restrict(makeDefault()) == makeDefault()
         *    makeDefault().restrict(makePublic()) == makeDefault()
         * </pre>
         * @param m the mode bits with which to generate 
         *          the restricted class mode
         * @return the new restricted mode bits
         **/
        final protected int restrict(int m) {
            return (glb(m) & ACCESS_MODE_MASK)
                | ((FLAG_BITS_MASK & mode_) & ~m);
        }

        /**
         * Return the <code>String</code> representation of this mode's
         * modifier bits
         * @return the <code>String</code> representation of the modifiers
         *         represented in this mode
         **/
        final public String toString() {
            return toString_;
        }

	/**
	 * Write this mode in 16 bit big-endian form.
	 **/
	public void write(OutputStream str) throws IOException {
	    str.write(mode_ >> 8);
	    str.write(mode_ & 0xff);
	}
    }

    // ******************************************************************

    /**
     * The concrete implementation of <code>Method</code> modifier objects
     * FIXME made public for Jamit's JMode.java
     **/
    public static class Class extends AllModes {
        /**
         * Builder to be used to build class mode objects in
         * stages. Flags can be individually set in the builder
         * and will be then set in the objects produced until
         * <code>reset()</code> is called to reinitialize.
         **/
        public static class Builder extends AllModes.Builder {
            public Mode.Class build() {
                return makeClassMode(flags);
            }
            public void setAbstract(boolean flag) {
                setFlag(flag, ACC_ABSTRACT);
            }

            public void setInterface(boolean flag) {
                setFlag(flag, ACC_INTERFACE);
            }

        }
        /**
         * The hashtable that stores class modifier object flyweights
         **/
        private final static Mode.Class[] modes
	    = new Mode.Class[N_CLASS_MODES];

        /**
             * Returns a unique <code>Mode.Class</code> object with mode set 
         * to the argument.
             * The flyweight pattern is used to ensure that modes are unique.
         * @param s the mode bits of the desired class modifier object
         * @return a class modifier object that represents the
         *         desired modifiers
         **/
        public static Mode.Class makeClassMode(int s) {
            // Impl Note: ACC_SUPER is a flag included for backwards 
            // compatibility, this flag is ignored by S3 code.
            s = s & ~ACC_SUPER;
	    s &= ACC_MASK;

            /* Interfaces are implicitely abstract, the flag should be set
             * according to the JVM specification.  */
            if ((s & ACC_INTERFACE) != 0)
                s |= ACC_ABSTRACT;

            if (((s & ACC_ABSTRACT) != 0) && ((s & ACC_FINAL) != 0))
                fail("Class can't be both final and abstract");

	    if (modes[s] == null) {
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try { modes[s] = new Mode.Class(s); }
		finally { MemoryPolicy.the().leave(r); }
	    }
	    return modes[s];
        }

	public Mode.Class makeSynthetic() {
	    if (isSynthetic())
		return this;
	    int s = mode_ | ACC_SYNTHETIC;
	    if (modes[s] == null) {
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try { modes[s] = new Mode.Class(s); }
		finally { MemoryPolicy.the().leave(r); }
	    }
	    return modes[s];
	}

        /**
         * Constructor - should only be called from within
         * {@link #makeClassMode}. Calls the superconstructor 
         * to initialize mode bits and the mode string.
         * Addition of the <code>interface</code> keyword
         * to the <code>String</code> representation of the mode
         * bits is delayed until this call, if it is necessary at all.
         * @param s the mode bits of this class modifier object
         **/
        private Class(int s) {
            super(s);
        }
        /**
         * The accept method for the visitor pattern.
         * @param visitor the visitor object to be accepted.
         **/
        public void accept(Mode.ModeVisitor visitor) {
            visitor.visitClassMode(this);
        }

        /**
         * The greatest lower bound (glb) returns a <code>Mode.Class</code>
         * object that has the most restrictive access mode,
         * and the intersection of the two modes' other modifiers
         * according to the partial ordering described for <code>Mode</code>
         * objects. Neither the receiver mode nor the argument mode will
         * be modified.
         *
         * @param m the class modifier object with which the greatest
         *          lower bound should be computed.
         * @return a class modifier object containing the most restrictive
         *         access mode of <code>this</code> and <code>m</code>,
         *         and the intersection of the two modes' other modifiers
         *         according to the partial ordering described for
         *         <code>Mode</code> objects.
         * @see Mode
         **/
        public Mode.Class glb(Mode.Class m) {
            int mM = m.getMode();
            int res = glb(mM);
            return (res == mode_)
                ? this
                : ((res == mM) ? m : makeClassMode(res));
        }

        /**
         * Determines if this class node indicates is an inner static
         * class.
         * @return true if this mode indicates an inner static class, else
         *         false.
         **/
        public boolean isInnerStatic() {
            return isStatic();
        }

        /**
         * The least upper bound (lub) returns a <code>Mode.Class</code> object
         * that has the least restrictive access mode
         * and the union of the two modes' other modifiers
         * according to the partial ordering described for <code>Mode</code>
         * objects. Neither the receiver mode nor the argument mode will be
         * modified.
         *
         * @param m the class modifier object with which the least
         *          upper bound should be computed.
         * @return a class modifier object containing the least restrictive
         *         access mode of <code>this</code> and <code>m</code>,
         *         and the union of the two modes' other modifiers
         *         according to the partial ordering described for
         *         <code>Mode</code> objects.
         * @see Mode
         **/
        public Mode.Class lub(Mode.Class m) {
            int mM = m.getMode();
            int res = lub(mM);
            if (res == mode_)
                return this;
            else if (res == mM)
                return m;
            return makeClassMode(res);
        }
        /**
         * Return a new <code>Mode.Class</code> with fewer flags. Any flag that
         * is set in the argument is removed from this. Thus:
         * <pre>
         *    makeAbstract().lub(makeFinal()).restrict(makeFinal())
         * </pre>
         * is equal to
         * <pre>
         *    makeAbstract()
         * </pre>
         * (where <code>makeX</code> has the obvious meaning)
         * For access modifiers, this method returns the most restrictive:
         * <pre>
         *    makePublic().restrict(makeDefault()) == makeDefault()
         * </pre>
         * @param m the <code>Mode.Class</code> with which to generate
         *          the restricted class mode
         * @return the new restricted class mode
         **/
        public Mode.Class restrict(Mode.Class m) {
            return makeClassMode(restrict(m.getMode()));
        }

    }

    /**
     * The interface for modifier object implementations for
     * <code>Members</code> (Fields or Methods).
     *
     * FIXME: all the methods that where defined in the member
     * interface are now defined in the AllModes class.  Is this a
     * problem?  Is a class not a member?
     **/
    public static abstract class Member extends AllModes {
	protected Member(int s) { super(s); }
    }

    /**
     * Modifiers for Fields.
     **/
    static public class Field extends Member {

        /**
         * Builder to be used to build field mode objects in
         * stages. Flags can be individually set in the builder
         * and will be then set in the objects produced until
         * <code>reset()</code> is called to reinitialize.
         **/
	static public class Builder extends AllModes.Builder {
            public Mode.Field build() {
                return makeFieldMode(flags);
            }
            public void setPrivate(boolean flag) {
                setFlag(flag, ACC_PRIVATE);
            }
            public void setProtected(boolean flag) {
                setFlag(flag, ACC_PROTECTED);
            }
            public void setStatic(boolean flag) {
                setFlag(flag, ACC_STATIC);
            }
            public void setTransient(boolean flag) {
                setFlag(flag, ACC_TRANSIENT);
            }
            public void setVolatile(boolean flag) {
                setFlag(flag, ACC_VOLATILE);
            }

        }

        // FIXME is that thread safe?
	final private static Field[] modes = new Field[N_FIELD_MODES];
        /**
	 * Returns a unique <code>Mode.Field</code> object with mode set 
         * to the argument.
             * The flyweight pattern is used to ensure that modes are unique.
         * @param s the mode bits of the desired field modifier object
         * @return a field modifier object that represents the
         *         desired modifiers
         **/
        public static Mode.Field makeFieldMode(int s) {
	    s &= FIELD_MODE_MASK;
            Mode.Field mode = modes[s];
            if (mode == null) {
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try { mode = modes[s] = new Field(s); }
		finally { MemoryPolicy.the().leave(r); }
	    }
	    return mode;
        }

	public Mode.Field makeSynthetic() {
	    if (isSynthetic())
		return this;
	    int s = mode_ | ACC_SYNTHETIC;
	    if (modes[s] == null) {
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try { modes[s] = new Mode.Field(s); }
		finally { MemoryPolicy.the().leave(r); }
	    }
	    return modes[s];
	}

        // This constructor only call from within makeFieldMode
        /**
         * Constructor - should only be called from within
         * {@link #makeFieldMode}. Calls the superconstructor 
         * to initialize mode bits and the mode string.
         * @param s the mode bits of this field modifier object
         * @see AllModes
         **/
        private Field(int s) {
            super(s);
        }

        public void accept(Mode.ModeVisitor visitor) {
            visitor.visitFieldMode(this);
        }

        /**
         * The greatest lower bound (glb) returns a <code>Mode.Field</code>
         * object that has the most restrictive access mode,
         * and the intersection of the two modes' other modifiers
         * according to the partial ordering described for <code>Mode</code>
         * objects. Neither the receiver mode nor the argument mode will
         * be modified.
         *
         * @param m the field modifier object with which the greatest
         *          lower bound should be computed.
         * @return a field modifier object containing the most restrictive
         *         access mode of <code>this</code> and <code>m</code>,
         *         and the intersection of the two modes' other modifiers
         *         according to the partial ordering described for
         *         <code>Mode</code> objects.
         * @see Mode
         **/
        public Mode.Field glb(Mode.Field m) {
            int mM = m.getMode();
            int res = glb(mM);
            if (res == mode_)
                return this;
            else if (res == mM)
                return m;
            return makeFieldMode(res);
        }

        /**
         * The least upper bound (lub) returns a <code>Mode.Field</code> object
         * that has the least restrictive access mode
         * and the union of the two modes' other modifiers
         * according to the partial ordering described for <code>Mode</code>
         * objects. Neither the receiver mode nor the argument mode will be
         * modified.
         *
         * @param m the field modifier object with which the least
         *          upper bound should be computed.
         * @return a field modifier object containing the least restrictive
         *         access mode of <code>this</code> and <code>m</code>,
         *         and the union of the two modes' other modifiers
         *         according to the partial ordering described for
         *         <code>Mode</code> objects.
         * @see Mode
         **/
        public Mode.Field lub(Mode.Field m) {
            int mM = m.getMode();
            int res = lub(mM);
            if (res == mode_)
                return this;
            else if (res == mM)
                return m;
            return makeFieldMode(res);
        }
        /**
         * Return a new <code>Mode.Field</code> with fewer flags.
         * Any flag that is set in the argument is removed from this. Thus:
         * <pre>
         *    makeAbstract().lub(makeFinal()).restrict(makeFinal())
         * </pre>
         * is equal to
         * <pre>
         *    makeAbstract()
         * </pre>
	 * <i>Yes, abstract fields, I use them all the time!</i>
         * (where <code>makeX</code> has the obvious meaning)
         * For access modifiers, this method returns the most restrictive:
         * <pre>
         *    makePublic().restrict(makeDefault()) == makeDefault()
         * </pre>
         * @param m the <code>Mode.Field</code> with which to generate
         *          the restricted field mode
         * @return the new restricted field mode
         **/
        public Mode.Field restrict(Mode.Field m) {
            int mM = m.getMode();
            int res = restrict(mM);
            return (res == mode_) ? this : makeFieldMode(res);
        }
    }

    /**
     * Modifier object implementations for <code>Methods</code>
     **/
    static public class Method extends Member {

        /**
         * Builder to be used to build method mode objects in
         * stages. Flags can be individually set in the builder
         * and will be then set in the objects produced until
         * <code>reset()</code> is called to reinitialize.
         * FIXME made public for Jamit's JMode
         **/
        static public class Builder extends AllModes.Builder {
            public Mode.Method build() {
                return makeMethodMode(flags);
            }
            public void setAbstract(boolean flag) {
                setFlag(flag, ACC_ABSTRACT);
            }
            public void setNative(boolean flag) {
                setFlag(flag, ACC_NATIVE);
            }
            public void setPrivate(boolean flag) {
                setFlag(flag, ACC_PRIVATE);
            }
            public void setProtected(boolean flag) {
                setFlag(flag, ACC_PROTECTED);
            }
            public void setStatic(boolean flag) {
                setFlag(flag, ACC_STATIC);
            }
            public void setSynchronized(boolean flag) {
                setFlag(flag, ACC_SYNCHRONIZED);
            }

        }
        /**
         * The array that stores method modifier object flyweights
         **/
	static private final Method[] modes
	    = new Method[N_METHOD_MODES];

        /**
	 * Returns a unique <code>Mode.Method</code> object with mode set 
         * to the argument.
	 * The flyweight pattern is used to ensure that modes are unique.
         * @param s the mode bits of the desired method modifier object
         * @return a method modifier object that represents the
         *         desired modifiers
         **/
        static Mode.Method makeMethodMode(int s) {
	    s &= ACC_MASK;
	    Method mode = modes[s];
            if (mode == null) {
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try { mode = modes[s] = new Method(s); }
		finally { MemoryPolicy.the().leave(r); }
            }
            return mode;
        }

	public Mode.Method makeSynthetic() {
	    if (isSynthetic())
		return this;
	    int s = mode_ | ACC_SYNTHETIC;
	    if (modes[s] == null) {
		Object r = MemoryPolicy.the().enterRepositoryDataArea();
		try { modes[s] = new Mode.Method(s); }
		finally { MemoryPolicy.the().leave(r); }
	    }
	    return modes[s];
	}


        /**
         * Constructor - should only be called from within
         * {@link #makeMethodMode}. Calls the superconstructor 
         * to initialize mode bits and the mode string. Because
         * of a clash between ACC_SUPER and ACC_SYNCHRONIZED,
         * the addition of the <code>synchronized</code> keyword
         * to the <code>String</code> representation of the mode
         * bits is delayed until this call, if it is necessary at all.
         * @param s the mode bits of this field modifier object
         * @see ovm.services.bytecode.JVMConstants#ACC_SUPER
         * @see ovm.services.bytecode.JVMConstants#ACC_SYNCHRONIZED
         **/
	 private Method(int s) {
	     super(s);
	     // Since ACC_SUPER==ACC_Synchronized, toString is delayed until
	     // now...
	     if (isSynchronized())
		 toString_ += "synchronized ";
	 }

	 public void accept(Mode.ModeVisitor visitor) {
	     visitor.visitMethodMode(this);
	 }

	 /**
	  * The greatest lower bound (glb) returns a <code>Mode.Method</code>
	  * object that has the most restrictive access mode,
	  * and the intersection of the two modes' other modifiers
	  * according to the partial ordering described for <code>Mode</code>
	  * objects. Neither the receiver mode nor the argument mode will
	  * be modified.
	  *
	  * @param m the method modifier object with which the greatest
	  *          lower bound should be computed.
	  * @return a method modifier object containing the most restrictive
	  *         access mode of <code>this</code> and <code>m</code>,
	  *         and the intersection of the two modes' other modifiers
	  *         according to the partial ordering described for
	  *         <code>Mode</code> objects.
	  * @see Mode
	  **/
	 public Mode.Method glb(Mode.Method m) {
	     int mM = m.getMode();
	     int res = glb(mM);
	     return (res == mode_)
		 ? this
		 : ((res == mM) ? m : makeMethodMode(res));
	 }

	 /**
	  * The least upper bound (lub) returns a <code>Mode.Method</code>
	  * object that has the least restrictive access mode
	  * and the union of the two modes' other modifiers
	  * according to the partial ordering described for <code>Mode</code>
	  * objects. Neither the receiver mode nor the argument mode will be
	  * modified.
	  *
	  * @param m the method modifier object with which the least
	  *          upper bound should be computed.
	  * @return a method modifier object containing the least restrictive
	  *         access mode of <code>this</code> and <code>m</code>,
	  *         and the union of the two modes' other modifiers
	  *         according to the partial ordering described for
	  *         <code>Mode</code> objects.
	  * @see Mode
	  **/
	 public Mode.Method lub(Mode.Method m) {
	     int mM = m.getMode();
	     int res = lub(mM);
	     return (res == mode_)
		 ? this
		 : ((res == mM) ? m : makeMethodMode(res));
	 }
	 /**
	  * Return a new <code>Mode.Method</code> with fewer flags.
	  * Any flag that is set in the argument is removed from this. Thus:
	  * <pre>
	  *    makeAbstract().lub(makeFinal()).restrict(makeFinal())
	  * </pre>
	  * is equal to
	  * <pre>
	  *    makeAbstract()
	  * </pre>
	  * (where <code>makeX</code> has the obvious meaning)
	  * For access modifiers, this method returns the most restrictive:
	  * <pre>
	  *    makePublic().restrict(makeDefault()) == makeDefault()
	  * </pre>
	  * @param m the <code>Mode.Method</code> with which to generate
	  *          the restricted method mode
	  * @return the new restricted method mode
	  **/
	 public Mode.Method restrict(Mode.Method m) {
	     int mM = m.getMode();
	     int res = restrict(mM);
	     if (res == mode_)
		 return this;
	     else
		 return makeMethodMode(res);
	 }

     }

     /**
      * Get an <code>abstract</code> class mode object
      * @return a <code>abstract</code> modifier object
      *         for classes.
      **/
     public static Mode.Class makeAbstractClass() {
	 return makeClass(ACC_ABSTRACT | ACC_PUBLIC);
     }
     /**
      * Get an <code>abstract</code> method mode object
      * @return a <code>abstract</code> modifier object
      *         for methods.
      **/
     public static Mode.Method makeAbstractMethod() {
	 return makeMethod(ACC_ABSTRACT | ACC_PUBLIC);
     }
     /**
      * Return the class modifier object which represents
      * <i>bottom</i>, according to the partial ordering
      * described in {@link Mode}.
      * <p>KLB: ask</p>
      * @return a class modifier object representing
      *         <i>bottom</i>
      **/
     public static Mode.Class makeBottomClass() {
	 return makeClass(ACC_DEFAULT);
     }
     /**
      * Return the field modifier object which represents
      * <i>bottom</i>, according to the partial ordering
      * described in {@link Mode}.
      * <p>KLB: ask</p>
      * @return a field modifier object representing
      *         <i>bottom</i>
      **/
     public static Mode.Field makeBottomField() {
	 return makeField(ACC_PRIVATE);
     }
     /**
      * Return a method modifier object which represents
      * <i>bottom</i>, according to the partial ordering
      * described in {@link Mode}.
      * <p>KLB: ask</p>
      * @return a method modifier object representing
      *         <i>bottom</i>
      **/
     public static Mode.Method makeBottomMethod() {
	 return makeMethod(ACC_PRIVATE);
     }
     /**
      * Retrieve (and possibly create) a class modifier
      * object corresponding to a modifier constant
      * argument.
      * @return a corresponding class modifier object
      * @see ovm.services.bytecode.JVMConstants
      **/
     public static Mode.Class makeClass(int s) {
	 return Mode.Class.makeClassMode(s);
     }
     /**
      * Make a new <code>Mode</code> builder for classes.
      * @return a new class modifer object builder object
      **/
     public static Mode.Class.Builder makeClassBuilder() {
	 return new Mode.Class.Builder();
     }
     /**
      * Get a <code>default</code> (in other words, package-scoped)
      * class access mode object
      * @return a <code>default</code> access modifier object
      *         for classes.
      **/
     public static Mode.Class makeDefaultClass() {
	 return makeClass(ACC_DEFAULT);
     }
     /**
      * Get a <code>default</code> (in other words, package-scoped)
      * field access mode object
      * @return a <code>default</code> access modifier object
      *         for fields.
      **/
     public static Mode.Field makeDefaultField() {
	 return makeField(ACC_DEFAULT);
     }
     /**
      * Get a <code>default</code> (in other words, package-scoped)
      * method access mode object
      * (meaning, in this case, package scoped)
      * @return a <code>default</code> access modifier object
      *         for methods.
      **/
     public static Mode.Method makeDefaultMethod() {
	 return makeMethod(ACC_DEFAULT);
     }
     /**
      * Retrieve (and possibly create) a field modifier
      * object corresponding to an modifier constant
      * argument.
      * @return a corresponding field modifier object
      * @see ovm.services.bytecode.JVMConstants
      **/
     public static Mode.Field makeField(int s) {
	 return Field.makeFieldMode(s);
     }
     /**
      * Make a new <code>Mode</code> builder for fields.
      * @return a new field modifer object builder object
      **/
     public static Mode.Field.Builder makeFieldBuilder() {
	 return new Field.Builder();
     }
     /**
      * Get a <code>final</code> class mode object
      * @return a <code>final</code> modifier object
      *         for classes.
      **/
     public static Mode.Class makeFinalClass() {
	 return makeClass(ACC_FINAL | ACC_PUBLIC);
     }
     /**
      * Get a <code>final</code> field mode object
      * @return a <code>final</code> modifier object for fields.
      **/
     public static Mode.Field makeFinalField() {
	 return makeField(ACC_FINAL | ACC_PUBLIC);
     }
     /**
      * Get a <code>final</code> method mode object
      * @return a <code>final</code> modifier object
      *         for methods.
      **/
     public static Mode.Method makeFinalMethod() {
	 return makeMethod(ACC_FINAL | ACC_PUBLIC);
     }
     /**
      * Retrieve (and possibly create) a method modifier
      * object corresponding to an modifier constant
      * argument.
      * @return a corresponding method modifier object
      * @see ovm.services.bytecode.JVMConstants
      **/
     public static Mode.Method makeMethod(int s) {
	 return Method.makeMethodMode(s);
     }
     // -----------------------------------------------------

     // ---------- modifier object builders ----------
     /**
      * Make a new <code>Mode</code> builder for methods.
      * @return a new method modifer object builder object
      **/
     public static Mode.Method.Builder makeMethodBuilder() {
	 return new Method.Builder();
     }
     /**
      * Get a <code>native</code> method mode object
      * @return a <code>native</code> modifier object
      *         for methods.
      **/
     public static Mode.Method makeNativeMethod() {
	 return makeMethod(ACC_NATIVE | ACC_PUBLIC);
     }
     /**
      * Get a <code>private</code> field access mode object
      * @return a <code>private</code> access modifier object
      *         for fields.
      **/
     public static Mode.Field makePrivateField() {
	 return makeField(ACC_PRIVATE);
     }
     /**
      * Get a <code>private final</code> field mode object
      * @return a <code>private final</code> modifier object
      *         for fields.
      **/
     public static Mode.Field makePrivateFinalField() {
	 return makeField(ACC_FINAL | ACC_PRIVATE);
     }
     /**
      * Get a <code>private final</code> method mode object
      * @return a <code>private final</code> modifier object
      *         for methods.
      **/
     public static Mode.Method makePrivateFinalMethod() {
	 return makeMethod(ACC_FINAL | ACC_PRIVATE);
     }
     /**
      * Get a <code>private</code> method access mode object
      * @return a <code>private</code> access modifier object
      *         for methods.
      **/
     public static Mode.Method makePrivateMethod() {
	 return makeMethod(ACC_PRIVATE);
     }
     /**
      * Get a <code>protected</code> field access mode object
      * @return a <code>protected</code> access modifier object
      *         for fields.
      **/
     public static Mode.Field makeProtectedField() {
	 return makeField(ACC_PROTECTED);
     }
     /**
      * Get a <code>protected</code> method access mode object
      * @return a <code>protected</code> access modifier object
      *         for methods.
      **/
     public static Mode.Method makeProtectedMethod() {
	 return makeMethod(ACC_PROTECTED);
     }
     // --------------------------------------------------------

     // ----- Class mode object creation methods --------
     /**
      * Get a <code>public</code> class access mode object
      * @return a <code>public</code> access modifier object
      *         for classes.
      **/
      public static Mode.Class makePublicClass() {
	 return makeClass(ACC_PUBLIC);
     }

     // ----- Field mode object creation methods --------

     /**
      * Get a <code>public</code> field access mode object
      * @return a <code>public</code> access modifier object
      *         for fields.
      **/
     public static Mode.Field makePublicField() {
	 return makeField(ACC_PUBLIC);
     }
     // ---------------------------------------------------------

     // ----- Method mode object creation methods --------
     /**
      * Get a <code>public</code> method access mode object
      * @return a <code>public</code> access modifier object
      *         for methods.
      **/
     public static Mode.Method makePublicMethod() {
	 return makeMethod(ACC_PUBLIC);
     }
     /**
      * Get a <code>static</code> field mode object
      * @return a <code>static</code> modifier object for fields.
      **/
     public static Mode.Field makeStaticField() {
	 return makeField(ACC_STATIC | ACC_PUBLIC);
     }
     /**
      * Get a <code>static</code> method mode object
      * @return a <code>static</code> modifier object
      *         for methods.
      **/
     public static Mode.Method makeStaticMethod() {
	 return makeMethod(ACC_STATIC | ACC_PUBLIC);
     }
     /**
      * Get a <code>synchronized</code> method mode object
      * @return a <code>synchronized</code> modifier object
      *         for methods.
      **/
     public static Mode.Method makeSynchronizedMethod() {
	 return makeMethod(ACC_SYNCHRONIZED | ACC_PRIVATE);
     }
     /**
      * Return a class modifier object which represents
      * <i>top</i>, according to the partial ordering
      * described in {@link Mode}.
      * <p>KLB: ask</p>
      * @return a class modifier object representing
      *         <i>top</i>
      **/
     public static Mode.Class makeTopClass() {
	return makeClass(ACC_PUBLIC | ACC_STATIC | ACC_FINAL | ACC_ABSTRACT);
    }
    /**
     * Return a field modifier object which represents
     * <i>top</i>, according to the partial ordering
     * described in {@link Mode}.
     * <p>KLB: ask</p>
     * @return a field modifier object representing
     *         <i>top</i>
     **/
    public static Mode.Field makeTopField() {
	return makeField(
			 ACC_PUBLIC
			 | ACC_FINAL
			 | ACC_STATIC
			 | ACC_TRANSIENT
			 | ACC_VOLATILE);
    }
    /**
     * Return a method modifier object which represents
     * <i>top</i>, according to the partial ordering
     * described in {@link Mode}.
     * <p>KLB: ask</p>
     * @return a method modifier object representing
     *         <i>top</i>
     **/
    public static Mode.Method makeTopMethod() {
	return makeMethod(
			  ACC_PUBLIC
			  | ACC_FINAL
			  | ACC_STATIC
			  | ACC_SYNCHRONIZED
			  | ACC_NATIVE
			  | ACC_ABSTRACT);
    }
    /**
     * Get a <code>transient</code> field mode object
     * @return a <code>transient</code> modifier object for
     * fields.
     **/
    public static Mode.Field makeTransientField() {
	return makeField(ACC_VOLATILE | ACC_PUBLIC);
    }
    /**
     * Get a <code>volatile</code> field mode object
     * @return a <code>volatile</code> modifier object
     *         for fields.
     **/
    public static Mode.Field makeVolatileField() {
	return makeField(ACC_TRANSIENT | ACC_PUBLIC);
    }

    /**
     * The interface for visitors to <code>Mode</code> objects.
     **/
    public interface ModeVisitor {

        /**
         * Visit a class modifiers object
         * @param m the <code>Mode.Class</code> object to be visited
         **/
        public void visitClassMode(Class m);

        /**
         * Visit a field modifiers object
         * @param m the <code>Mode.Field</code> object to be visited
         **/
        public void visitFieldMode(Field m);

        /**
         * Visit a method modifiers object
         * @param m the <code>Mode.Method</code> object to be visited
         **/
        public void visitMethodMode(Method m);

    } // End of Mode.ModeVisitor

} // End of S3Mode
