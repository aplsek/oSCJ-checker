package ovm.services.bytecode;

import ovm.core.domain.ConstantPool;
import ovm.core.domain.Method;
import ovm.core.domain.Type;
import ovm.core.repository.TypeName;
import ovm.core.repository.UTF8Store;
import s3.core.domain.S3ByteCode;
import s3.util.PragmaMayNotLink;

/**
 * An {@link Instruction.Visitor} that can be used to visit each
 * instruction in a {@link Method} in turn.  Typically, a subclass is
 * defined with visit methods, and instatiated exactly once.
 * {@link #run} can be called repeatedly to scan any number of methods.
 **/
public abstract class MethodCodeScanner extends Instruction.Visitor {
    protected InstructionBuffer buf;
    protected ConstantPool cp;
    protected S3ByteCode code;
    /** True unless warnings suppressed by {@link s3.util.PragmaMayNotLink} **/
    protected boolean warnMissingEnabled;

    protected int getPC() { return buf.getPC(); }

    /**
     * Iterate over a method.
     **/
    public void run(Method m) {
	code = m.getByteCode();
	Type.Scalar t = (Type.Scalar) m.getDeclaringType();
	cp = t.getConstantPool();
	buf = InstructionBuffer.wrap(code);
	warnMissingEnabled =
	    !PragmaMayNotLink.declaredBy(m.getSelector(),
				       t.getDomain().blueprintFor(t));

	while (buf.hasRemaining()) {
	    Instruction i = buf.get();
	    //System.err.println(getPC() + i.getName());
	    if (i == Instruction.WIDE.singleton)
		((Instruction.WIDE) i).specialize(buf).accept(this);
	    else
		i.accept(this);
	}
    }

    /**
     * Print a message with the current source location.
     * The message will be of the form
     * <blockquote>
     * <code><i>filename</i>: <i>lineno</i>: in <i>method</i>: <i>s</i><code>
     * </blockquote>
     * These message can almost always be parsed by programs like
     * eclipse and emacs.
     **/
    public void message(String s) {
	Type.Scalar dt = code.getMethod().getDeclaringType().asScalar();
	TypeName tn = dt.getName().getInstanceTypeName();
	int dir = tn.getPackageNameIndex();
	int file = dt.getSourceFileNameIndex();
	int line = code.getLineNumber(getPC());

	if (file == 0)
	    file = tn.getShortNameIndex();

	System.err.println((dir == 0
			    ? ""
			    : UTF8Store._.getUtf8(dir)  + "/") +
			   UTF8Store._.getUtf8(file) + ":" +  line +
			   ": in " + code.getSelector().getName() +
			   ": "  + s);
    }

    /**
     * Generate a message (with file and line), warning that <i>s</i>
     * is not found.
     **/
    public void warnMissing(String s) {
	if (warnMissingEnabled)
	    message("warning: " + s + " not found");
    }
}

