package ovm.core.repository;

import ovm.util.ByteBuffer;

/**
 * API changed by builders of bytecode representations.
 *
 * @author Christian Grothoff
 */
public interface CodeBuilder {
 
    /**
     * Declare the maximum stack height and number of local variables for
     * <code>Bytecode</code> objects to be built with this builder.
     * @param maxStack the maximum stack height
     * @param maxLocals the number of local variables.
     */
    public void declareTemporaries(char maxStack, 
				   char maxLocals);
    
    /**
     * Set the array of bytecode for <code>Bytecode</code> objects to be built
     * with this builder
     * @param buf the buffer with code to be set.
     */
    public void setCode(ByteBuffer buf);

    public void setExceptionHandlers(ExceptionHandler[] ex);
    
    /**
     * Declare a new exception handler for Bytecode objects built with this
     * builder, adding the new exception handler to the existing list
     * of exception handlers if there is one, and if not, creating a new list
     * of exception handlers.
     * @param re an exception handler to be declared in this builder
     */
    public void declareExceptionHandler(ExceptionHandler re);

    /**
     * Declare third-party attributes which should be set for 
     * objects built with this builder. Will fail if attributes 
     * are frozen.
     * @see Attribute
     * @param attribute the attribute
     */
    public void declareAttribute(Attribute attribute);

    public void replaceAttribute(Attribute old,
				 Attribute newAttr);
	
    public void removeAttribute(Attribute attribute);

    /**
     * Build the corresponding code object.
     * FIXME: currently used only for a "toString()",
     * we may want to refine the return type...
     */
    public Object unrefinedBuild();

    /**
     * Set the constant pool (unrefined variant, MUST throw class cast
     * exception if the CP has the wrong type.
     */
    public void setUnrefinedConstantPool(Constants c);


} // end of CodeBuilder
