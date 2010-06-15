package ovm.core.repository;

import ovm.core.OVMBase;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.util.ArrayList;
import ovm.util.ByteBuffer;
import ovm.util.Collections;
import ovm.util.List;

/**
 * Describes a bytecode (JVM spec or OVMIR) method.
 * 
 * Contains a complete method consisting of access flags, a reference
 * to a defining method, an array of byte code, a starting offset and
 * ending offset, limits on stack size and local variables, and
 * finally exceptions.<p>
 *
 * The code arguments point to the first instruction of the code
 * fragment.  In some implementations the code array may be shared
 * between different objects, therefore the code is not guaranteed to
 * start at offset 0 in the array.  A code fragment may have no
 * exception table if there are no handlers.
 *
 * @author Christian Grothoff, Jan Vitek, Krzysztof Palacz
 **/
public class Bytecode extends OVMBase {

    public static final Bytecode DEAD_CODE = new Bytecode();
    /**
     * The attributes for this fragment
     **/
    private final Attribute[] attributes_;

    /**
     * The byte code array for this byte code fragment.
     **/
    protected final byte[] code_;

    /**
     * The constant pool to be used by this fragment
     **/
    private final ConstantPool constantPool_;

    /**
     * The exception table for this byte code fragment (can
     * be null).
     * @see ovm.core.repository.ExceptionHandler
     **/
    private final ExceptionHandler[] exceptions_;

    // KLB: ask and explain more?
    /**
     * The number of local variables for this fragment
     **/
    protected final char maxLocals_;

    /**
     * The maximum stack height for this fragment
     **/
    protected final char maxStack_;

    /**
     * The bound selector of the method associated with this 
     * <code>Bytecode</code> object
     **/
    private final Selector.Method selector_;

    /**
     * Create a new code fragment.
     *
     * @param code a reference to an array containing byte code.
     * @param constantPool the constant pool referred from <code>code</code>
     * @param maxStack the limit on the height of the stack.
     * @param maxLocals the number of locals.
     * @param exceptions the exception table, or null if none.
     * @param selector the method's selector
     * @param attributes the method's attributes
     **/
    Bytecode(
        byte[] code,
        ConstantPool constantPool,
        char maxStack,
        char maxLocals,
        ExceptionHandler[] exceptions,
        Selector.Method selector,
        Attribute[] attributes) {
 
        this.code_ = code;
        this.constantPool_ = constantPool;
        this.maxStack_ = maxStack;
        this.maxLocals_ = maxLocals;
        this.exceptions_ = exceptions;
        this.selector_ = selector;
        this.attributes_ = attributes;
    }

    private Bytecode() { // Used to build DEAD_CODE only

        this.code_ = null;
        this.constantPool_ = null;
        this.maxStack_ = 0;
        this.maxLocals_ = 0;
        this.exceptions_ = null;
        this.selector_ = null;
        this.attributes_ = null;
    }
    /**
     * A visitor pattern accept method.
     * @param v visitor.
     */
    public void accept(RepositoryProcessor v) {
        v.visitByteCodeFragment(this);
    }

    /**
     * Get the attributes associated with this <code>Bytecode</code> object
     * @return Attribute[]
     */
    public Attribute[] getAttributes() {
        return attributes_;
    }

    /**
     * Get a builder which can build <code>Bytecode</code> objects modeled on
     * this object.
     * @param copyAttrs determines whether or not the attributes contained in
     *                  the original object should also be contained in the
     *                  objects built by the new builder
     * @return Bytecode.Builder a builder which can produce copies of this
     *                          Bytecode objects
     */
    public Bytecode.Builder getBuilder(boolean copyAttrs) {
        return new Bytecode.Builder(this, copyAttrs);
    }

    /**
     * Get the array of bytecode associated with this <code>Bytecode</code> object
     * @return byte[] a byte array containing this object's bytecode
     */
    public byte[] getCode() {
        return code_;
    }

    /**
     * Get the constant pool referred to by this <code>Bytecode</code> object's array
     * of bytecode.
     * @return the referenced constant pool
     */
    public ConstantPool getConstantPool() {
        return constantPool_;
    }

    /**
     * Return the descriptor associated with this <code>Bytecode</code> 
     * object's method.
     * @return the descriptor associated with this method
     **/
    public Descriptor.Method getDescriptor() {
        return selector_.getDescriptor();
    }

    /**
     * Return the exception table of this <code>Bytecode</code> object
     * @return ExceptionHandler[] the exception handler table
     */
    public ExceptionHandler[] getExceptionHandlers() {
        return exceptions_;
    }

    /**
     * Return the number of local variable associated with this <code>Bytecode</code>
     * object
     * @return int the number of local variables
     */
    public char getMaxLocals() {
        return maxLocals_;
    }

    /**
     * Get the maximum stack height associated with this <code>Bytecode</code> object
     * @return int the maximum stack height
     */
    public char getMaxStack() {
        return maxStack_;
    }

    /**
     * Get the selector of the method associated with this <code>Bytecode</code>
     * object
     * @return Selector.Method this bytecode fragment's bound selector
     */
    public Selector.Method getSelector() {
        return selector_;
    }

    /**
     * Return the <code>String</code> representation of the Selector for
     * this fragment.
     * @return the <code>String</code> representation of this
     *         fragment's selector.
     **/

    public String toString() {
        return selector_.toString();
    }

    /**
     * Visit the attributes of this <code>Bytecode</code> object.
     */
    public void visitAttributes(RepositoryProcessor x) {
        for (int i = 0; i < attributes_.length; i++)
            attributes_[i].accept(x);
    }
    public void visitComponents(RepositoryProcessor x) {
        visitExceptions(x);
        visitAttributes(x);
    }

    /**
     * Visit the exceptions in the exception table of this Code fragment.
     */
    public void visitExceptions(RepositoryProcessor v) {
        for (int i = 0; i < exceptions_.length; i++)
            exceptions_[i].accept(v);
    }

    /**
     * Builder methods for ByteCode objects
     **/
    public static class Builder
        extends RepositoryBuilder
        implements CodeBuilder {

        /**
         * Factory interface for <code>ByteCode.Builder</code>
         * objects.
         **/
        public interface Factory {
            /**
             * Get a fresh <code>ByteCode.Builder</code>
             * @return a fresh builder object for bytecode fragments
             **/
            public Bytecode.Builder getBuilder();

        } // end Bytecode.Builder.Factory

        /**
         * The byte code array for <code>Bytecode</code> objects 
         * to be built with this builder.
         **/
        protected byte[] code;

        /**
         * The constant pool to be associated with 
         * <code>Bytecode</code> objects to be built with this builder.
         **/
        protected ConstantPool constantPool;

        /**
         * The <code>TypeName</code> of the class which defined the method
         * associated with the <code>Bytecode</code> object to be built with
         * this builder.
         **/
        protected TypeName.Scalar definingClass;

        /**
         * The <code>Descriptor</code> for the method which is associated
         * with the <code>Bytecode</code> object to be built with
         * this builder.
         **/
        protected Descriptor.Method descriptor;

        private List handlers;

        /**
         * The number of local variables for this fragment
         **/
        protected char maxLocals;

        /**
         * The maximum stack height for <code>Bytecode</code> objects
         * to be built with this builder.
         **/
        protected char maxStack;
        /**
         * The utf8 index of the name of the method associated with the
         * <code>Bytecode</code> objects to be built with this builder. 
         **/
        protected int nameIndex;

        /**
         * Create a new builder from scratch.
         **/
        public Builder() {
        }

        /**
         * Create a new <code>Bytecode</code> builder, given all
         * of the necessary information about the <code>Bytecode</code>
         * object to be built with this builder.
         * 
         * @param code the byte array containing the bytecode
         * @param constantPool the constant pool referred from <code>code</code>
         * @param maxStack the maximum stack height for this bytecode fragment
         * @param maxLocals the number of local variables for this fragment
         * @param sel the selector of the method associated with this 
         *            bytecode fragment
         * @param attr the attributes to be associated with this method
         * @param rex the exception table, or null if none
         */
        public Builder(
            byte[] code,
            ConstantPool constantPool,
            char maxStack,
            char maxLocals,
            ExceptionHandler[] rex,
            Selector.Method sel,
            Attribute[] attr) {
            this.code = code;
            this.constantPool = constantPool;
            this.maxStack = maxStack;
            this.maxLocals = maxLocals;
            this.descriptor = sel.getDescriptor();
            this.definingClass = (TypeName.Scalar) sel.getDefiningClass();
            this.nameIndex = sel.getNameIndex();

            if (attr != null)
                for (int i = 0; i < attr.length; i++) {
                    declareAttribute(attr[i]);
                }
            for (int i = 0; i < rex.length; i++) {
                declareExceptionHandler(rex[i]);
            }
        }

        /**
         * Given a <code>Bytecode</code> object, create a new
         * builder based on that object.
         * @param rbcf the object from which to create the builder
         * @param copyAttributes true if the attributes of the
         *        Bytecode object should be copied, else false.
         */
        public Builder(Bytecode rbcf, boolean copyAttributes) {
            this(
                rbcf.getCode(),
                rbcf.getConstantPool(),
                rbcf.getMaxStack(),
                rbcf.getMaxLocals(),
                rbcf.getExceptionHandlers(),
                rbcf.getSelector(),
                copyAttributes ? rbcf.getAttributes() : null);
        }

        public Object unrefinedBuild() {
            return build();
        }

        /**
         * Create a new <code>Bytecode</code> object based on this
         * <code>Builder</code>'s configuration.
         * @return Bytecode the new Bytecode object
         */
        public Bytecode build() {
            UnboundSelector.Method usel =
                UnboundSelector.Method.make(nameIndex, descriptor);
            Selector.Method sel =
		Selector.Method.make(usel, definingClass);
            Bytecode bcf =
                new Bytecode(
                    code,
                    constantPool,
                    maxStack,
                    maxLocals,
                    makeHandlersArray(),
                    sel,
                    super.getAttributes());

            return bcf;
        }

        /**
         * Declare the Bytecode objects built with this builder deprecated. 
         * This will fail, since Bytecode objects cannot be deprecated.
         */
        public final void declareDeprecated() {
            fail("S3ByteCodeFragmentBuilder: a ByteCodeFragment can't be deprecated");
        }

        /**
         * Declare a synthetic attribute for this builder.
         * <p>
         * Note that if {@link #getAttributes} has already been called, the 
         * attribute set is considered frozen and this will fail.
         * </p>
         */
        public final void declareSynthetic() {
            declareAttribute(Attribute.Synthetic.Bytecode.SINGLETON);
        }

        /**
         * Declare a new exception handler for Bytecode objects built with this
         * builder, adding the new exception handler to the existing list
         * of exception handlers if there is one, and if not, creating a new list
         * of exception handlers.
         * @param re an exception handler to be declared in this builder
         */
        public void declareExceptionHandler(ExceptionHandler re) {
            if (handlers == Collections.EMPTY_LIST) {
                handlers = new ArrayList();
            }
            handlers.add(re);
        }

        /**
         * Declare the selector for the method associated with the
         * <code>Bytecode</code> object to be built with this builder.
         * @param _definingClass the class which defines this Bytecode object's method
         * @param _nameIndex the repository utf8 string index of the method name
         * @param _descriptor the descriptor for the method
         */
        public void declareSelector(
            TypeName.Scalar _definingClass,
            int _nameIndex,
            Descriptor.Method _descriptor) {
            this.definingClass = _definingClass;
            this.nameIndex = _nameIndex;
            this.descriptor = _descriptor;
        }

        public void setSelector(Selector.Method sel) {
            this.definingClass = sel.getDefiningClass().asScalar();
            this.nameIndex = sel.getNameIndex();
            this.descriptor = sel.getDescriptor();
        }

        /**
         * Declare the maximum stack height and number of local variables for
         * <code>Bytecode</code> objects to be built with this builder.
         * @param _maxStack the maximum stack height
         * @param _maxLocals the number of local variables.
         */
        public void declareTemporaries(char _maxStack, char _maxLocals) {
            this.maxStack = _maxStack;
            this.maxLocals = _maxLocals;
        }

        /**
         * Return a read-only list of over already declared exception handlers.
         */
        public List exceptionHandlerList() {
            return Collections.unmodifiableList(handlers);
        }

        /**
         * Get the array of bytecode for <code>Bytecode</code> to be built
         * with this builder
         * @return a byte array containing the bytecode
         **/
        public byte[] getCode() {
            return code;
        }

        /**
         * Get the constant pool referred to by the bytecode associated with
         * the <code>Bytecode</code> object to be built by this builder. This
         * is whatever was passed as argument to the last setConstantPool()
         * call.
         */
        public ConstantPool getConstantPool() {
            return this.constantPool;
        }

        /**
         * Get the descriptor of the method with which this
         * <code>Bytecode</code> object is associated.
         * @return Descriptor.Method the method descriptor
         */
        public Descriptor.Method getDescriptor() {
            return descriptor;
        }

        /**
         * Get the number of local variables associated with <code>Bytecode</code>
         * objects to be built by this builder.
         * @see Attribute.LocalVariableTable
         * @return int the number of local variables associated with this 
         *             object
         */
        public int getMaxLocals() {
            return maxLocals;
        }

        public void setExceptionHandlers(ExceptionHandler[] ex) {
            handlers = Collections.EMPTY_LIST;
            if (ex != null)
                for (int i = ex.length - 1; i >= 0; i--)
                    declareExceptionHandler(ex[i]);
        }

        /**
             * Get the maximum stack height for the <code>Bytecode</code> object
             * to be built by this builder.
             * @see Attribute.LocalVariableTable
             * @return int
             */
        public int getMaxStack() {
            return maxStack;
        }

        /**
         * Create a list of exception handlers. If the exception handler list
         * for this builder is empty, return an empty array. Otherwise, create
         * an array of exception handlers from the exception handler list 
         * associated with this builder and return that.
         * @return ExceptionHandler[] an array corresponding to the list of
         *                            exception handlers associated with this
         *                            builder
         */
        protected ExceptionHandler[] makeHandlersArray() {
            ExceptionHandler[] exceptionsArr;
            if (handlers == Collections.EMPTY_LIST) {
                exceptionsArr = ExceptionHandler.EMPTY_ARRAY;
            } else {
                exceptionsArr =
                    (ExceptionHandler[]) handlers.toArray(
                        new ExceptionHandler[handlers.size()]);
            }
            return exceptionsArr;
        }

        /**
         * Reset this builder to its default values, clearing any information
         * added through previous uses.
         */
        public void reset() {
            super.reset();
            code = null;
            constantPool = null;
            maxStack = 0;
            maxLocals = 0;
            handlers = Collections.EMPTY_LIST;
            descriptor = null;
            nameIndex = 0;
            definingClass = null;
        }

        /**
         * Set the array of bytecode for <code>Bytecode</code> objects to be built
         * with this builder
         * @param buf the buffer with code to be set.
         */
        public void setCode(ByteBuffer buf) {
            this.code = buf.array();
        }

        /**
         * Set the constant pool for the <code>Bytecode</code> object
         * to be built with this builder.
         * @param constantPool the constant pool referenced by the bytecode
         *                     associated with this Bytecode object
         */
        public void setConstantPool(ConstantPool constantPool) {
            this.constantPool = constantPool;
        }

        public void setUnrefinedConstantPool(Constants constantPool) {
            setConstantPool((ConstantPool) constantPool);
        }

        /**
         * Remove all exception handlers from the exception handler list
         */
        public void unsetAllExceptionHandlers() {
            handlers = Collections.EMPTY_LIST;
        }
    }
}