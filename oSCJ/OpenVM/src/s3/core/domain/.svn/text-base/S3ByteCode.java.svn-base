package s3.core.domain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import ovm.core.domain.AttributedCode;
import ovm.core.domain.Code;
import ovm.core.domain.ConstantPool;
import ovm.core.domain.Type;
import ovm.core.execution.Engine;
import ovm.core.repository.Attribute;
import ovm.core.repository.CodeBuilder;
import ovm.core.repository.Constants;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.Selector;
import ovm.core.repository.Attribute.LineNumberTable;
import ovm.services.bytecode.InstructionBuffer;
import ovm.util.ByteBuffer;
import ovm.util.OVMRuntimeException;
import s3.util.PragmaTransformCallsiteIR.BCnothing;
import ovm.core.OVMBase;

/**
 * Representation of bytecode in the LINKED, per type, domain-bound state.  
 * The S3ByteCode can be at any stage of rewriting and has a linked, S3Constants 
 * constant pool.  Thus S3ByteCode is different from the repository Bytecode, 
 * which is unlinked. The similarity between the two classes comes from the fact 
 * that they are both representing code that is expressed in terms of a stack machine.
 * @author Christian Grothoff
 */
public class S3ByteCode extends AttributedCode {

    public static final Kind KIND = new Kind() {
    };
    public static final S3ByteCode[] EMPTY_ARRAY = new S3ByteCode[0];

    /**
     * The state of S3ByteCode containing Java bytecode.
     **/
    public static final int ORIGINAL = 0;
    /**
     * The state of S3ByteCode containing OvmIR bytecode, but without
     * pollchecks, quickified instructions, or other constructs that
     * make analysis and optimization difficult.  Ovm idioms in these
     * methods have been expanded into bytecode.
     **/
    public static final int EXPANDED = 1;
    /**
     * Fully rewritten S3ByteCode.  This code contains the appropriate
     * number of pollcheck instructions, and may contain quick or
     * symbolic references to fields and methods, depending on what
     * the execution engine expects.
     **/
    public static final int REWRITTEN = 2;

    protected byte[] dbg_string;

    /**The byte code array for this byte code fragment.*/
    private byte[] bytes;
    // ATTN: interpreter uses this field, MUST MATCH TEXT "bytes"

    /** The state of this S3ByteCode object. **/
    private int state;

    /** The maximum stack height for this fragment */
    private char maxStack_;

    /** The number of local variables for this fragment*/
    private char maxLocals_;

    private ExceptionHandler[] exceptions_;

    /** The attributes for this fragment*/
    private Attribute[] attributes_;

    /** The flag that indicates if this is synchronized
     * This field is here because the interpreter needs to know that a
     * method is synchronized in order to call monitor enter and so on.                     
     */
    private boolean isSynchronized_;

    S3ByteCode(
        S3Method method,
        byte[] bytes,
        char maxStack,
        char maxLocals,
        ExceptionHandler[] exceptions,
        Attribute[] attributes,
        boolean isSynchronized) {
        super(method, attributes, exceptions);
        this.bytes = bytes;
        maxStack_ = maxStack;
        maxLocals_ = maxLocals;
        isSynchronized_ = isSynchronized;

        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            getMethod().getSelector().write(b);
            b.write(0);
	    dbg_string = b.toByteArray();
        } catch (IOException e) {
            throw new OVMRuntimeException(e);
        }
	ensureTrampoline();
    }

    public void ensureTrampoline() {
	// FIXME: can this ever be called with foreignEntry null?

	// Don't attempt to create trampolines at build time.
	// Trampolines for but not compiled methods will be generated
	// at startup.
	if (foreignEntry == null && !OVMBase.isBuildTime())
	    foreignEntry = Engine.getTrampoline(this);
    }

    public void bang(Code c) {
        bang((S3ByteCode) c);
    }

    public void bang(S3ByteCode c) {
	bang((AttributedCode) c);
        this.bytes = c.bytes;
        this.maxStack_ = c.maxStack_;
        this.maxLocals_ = c.maxLocals_;
        this.isSynchronized_ = c.isSynchronized_;
    }

    public void dumpAscii(String domainName, PrintWriter pw) {
        InstructionBuffer ib =
            InstructionBuffer.wrap(
                ByteBuffer.wrap(bytes),
                getSelector(),
                getConstantPool());
	pw.println(domainName + ";" + itsMethod.toString());
	pw.println("Stack = " + (int)maxStack_ + " Locals = " + (int)maxLocals_);
	pw.println(ib.toString());
	String handlers = "";
	boolean atLeastOneHandler = false;
	for(int i = 0; i < exceptions_.length; i++) {
	    atLeastOneHandler = true;
	    handlers += "\t" + exceptions_[i] + "\n";
	}
	if (atLeastOneHandler) {
	    pw.println("Exception handlers:\n" + handlers);
	}
	for(int i = 0; i < attributes_.length; i++) {
	    pw.println(attributes_[i]);
	}
    }

    // FIXME: does InstructionBuffer.toString work at runtime?
    public String toString() throws BCnothing {
	if (bytes == null)
	    return "S3ByteCode{" + itsMethod + "#null}";
        InstructionBuffer ib =
            InstructionBuffer.wrap(
                ByteBuffer.wrap(bytes),
                getSelector(),
                getConstantPool());
        return "S3ByteCode{"
            + itsMethod
            + "#"
            + ib.toString()
            + " "
            + getSelector()
            + "}";
    }

    public Kind getKind() {
        return KIND;
    }
    public boolean isSynchronized() {
        return isSynchronized_;
    }
    public ConstantPool getConstantPool() {
        return ((Type.Scalar) getMethod().getDeclaringType()).getConstantPool();
    }
    public byte[] getBytes() {
        return bytes;
    }
    /**
     * Return the state of this S3ByteCode object.  The state
     * indicates how much rewriting has been performed.  ByteCode
     * objects start life in the {@link #ORIGINAL} state.  Later they
     * are {@link #EXPANDED}.  And finally, they are fully
     * {@link #REWRITTEN}.
     *
     * @see s3.services.bytecode.ovmify.IRewriter#ensureState
     **/
    public int getState() {
	return state;
    }
    public void setState(int state) {
	this.state = state;
    }
    public char getMaxStack() {
        return maxStack_;
    }
    public char getMaxLocals() {
        return maxLocals_;
    }

    /** @author Christian Grothoff */
    public static class Builder implements CodeBuilder {

        /** The byte code array for this byte code fragment. */
        private byte[] bytes_;
        /**The maximum stack height for this fragment*/
        private char maxStack;
        /** The number of local variables for this fragment*/
        private char maxLocals;
        /**The exception table for this byte code fragment (can be null).*/
        private ExceptionHandler[] exceptions;
        /** The attributes for this fragment*/
        private Attribute[] attributes;
        /**The flag that indicates if this is synchronized*/
        private boolean isSynchronized;
        private final S3Method method_;

        public Builder(S3Method method) {
            this.method_ = method;
        }

        public Builder(S3ByteCode bc) {
            this.method_ = (S3Method) bc.getMethod();
            this.bytes_ = bc.getBytes();
            this.maxStack = bc.getMaxStack();
            this.maxLocals = bc.getMaxLocals();
            this.exceptions = bc.getExceptionHandlers();
            this.attributes = bc.getAttributes();
            this.isSynchronized = bc.isSynchronized();
        }

        public Builder(
            S3Method method,
            byte[] bytes,
            char maxStack,
            char maxLocals,
            ExceptionHandler[] exceptions,
            Attribute[] attributes,
            boolean isSynchronized) {
            this.method_ = method;
            this.bytes_ = bytes;
            this.maxStack = maxStack;
            this.maxLocals = maxLocals;
            this.exceptions = exceptions;
            this.attributes = attributes;
            this.isSynchronized = isSynchronized;
        }

        public Object unrefinedBuild() {
            return build();
        }

	/**
	 * This is a weird one.  It is called by the bytecode editing
	 * framework, and it should only be called with the
	 * ConstantPool for this method's class.
	 */
	public void setUnrefinedConstantPool(Constants c) {
	    assert(c == ((Type.Scalar) method_.getDeclaringType()).getConstantPool());
	}

        public S3ByteCode build() {
            return new S3ByteCode(
                method_,
                bytes_,
                maxStack,
                maxLocals,
                exceptions,
                attributes,
                isSynchronized);
        }

        public void declareTemporaries(char _maxStack, char _maxLocals) {
            setMaxStack(_maxStack);
            setMaxLocals(_maxLocals);
        }

        public void setCode(ByteBuffer buf) {
            setBytes(buf.array());
        }
        private void setBytes(byte[] code) {
            this.bytes_ = code;
        }
        public void setAttributes(Attribute[] attr) {
            this.attributes = attr;
        }
        public void setMaxStack(char ms) {
            this.maxStack = ms;
        }
        public void setMaxLocals(char ml) {
            this.maxLocals = ml;
        }
        public void setSynchronized(boolean value) {
            this.isSynchronized = value;
        }
        public char getMaxStack() {
            return maxStack;
        }
        public char getMaxLocals() {
            return maxLocals;
        }
        public void setExceptionHandlers(ExceptionHandler[] ex) {
            this.exceptions = ex;
        }

        public void declareExceptionHandler(ExceptionHandler re) {
            int len = exceptions == null ? 1 : exceptions.length + 1;
            ExceptionHandler[] dst = new ExceptionHandler[len];
            if (exceptions != null)
                System.arraycopy(exceptions, 0, dst, 0, exceptions.length);
            dst[len - 1] = re;
            this.exceptions = dst;
        }

        public void removeAttribute(Attribute attribute) {
            if (attributes == null)
                return;
            int match = -1;
            int len = attributes.length;
            for (int i = 0; i < len; i++)
                if (attributes[i] == attribute) {
                    match = i;
                    break;
                }
            if (match == -1)
                return; // warn? throw error?
            Attribute[] dst = new Attribute[len - 1];
	    if (match != 0)
		System.arraycopy(attributes, 0, dst, 0, match - 1);
	    if (match != len - 1)
		System.arraycopy(
				 attributes,
				 match + 1,
				 dst,
				 match,
				 len - match - 1);
            this.attributes = dst;
        }

        public void replaceAttribute(Attribute old, Attribute newAttr) {
            if (attributes == null) {
                declareAttribute(newAttr);
                return;
            }
            int len = attributes.length;
            for (int i = 0; i < len; i++)
                if (attributes[i] == old) {
                    attributes[i] = newAttr;
                    return;
                }
            declareAttribute(newAttr);
        }

        public void declareAttribute(Attribute attribute) {
            int len = attributes == null ? 1 : attributes.length + 1;
            Attribute[] dst = new Attribute[len];
            if (attributes != null)
                System.arraycopy(attributes, 0, dst, 0, attributes.length);
            dst[len - 1] = attribute;
            this.attributes = dst;
        }
    }
}
