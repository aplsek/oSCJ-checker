package ovm.core.domain;
import ovm.core.repository.Attribute;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.Attribute.LineNumberTable;
import ovm.core.execution.CoreServicesAccess;

import ovm.core.execution.Native;
import ovm.core.services.memory.MemoryPolicy;
import ovm.core.services.io.BasicIO;
import ovm.core.Executive;

public abstract class AttributedCode extends Code {
    static public boolean DEBUG_THROW = false;

    protected static final ExceptionHandler[] EMPTY_EXCEPTIONS =
        new ExceptionHandler[0];
    protected static final Attribute[] EMPTY_ATTRIBUTES = new Attribute[0];

    /**
     * The attributes for this code fragment.  A method with no
     * byte code attributes is guaranteed to have a  zero length
     * attribute array.
     **/
    protected Attribute[] attributes;
    /**
     * The exception table for this code fragment.  A method with no
     * exception handlers is guaranteed to have a  zero length handler
     * array.
     **/
    protected ExceptionHandler[] handlers;

    public AttributedCode(Method m,
			  Attribute[] attributes,
			  ExceptionHandler[] exceptions) {
	super(m);
        handlers = exceptions == null ? EMPTY_EXCEPTIONS : exceptions;
        this.attributes = attributes == null ? EMPTY_ATTRIBUTES : attributes;
    }

    protected void bang(AttributedCode c) {
	attributes = c.attributes;
	handlers = c.handlers;
	foreignEntry = c.foreignEntry;
    }

    public Attribute[] getAttributes() { return attributes; }
    public ExceptionHandler[] getExceptionHandlers() { return handlers; }
    public int getLineNumber(int vpc) {
	for (int i = 0; i < attributes.length; i++)
	    if (attributes[i] instanceof LineNumberTable)
		return ((LineNumberTable) attributes[i]).getLineNumber(vpc);
	return vpc;
    }
}
