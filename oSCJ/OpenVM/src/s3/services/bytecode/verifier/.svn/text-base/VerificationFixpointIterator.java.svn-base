package s3.services.bytecode.verifier;

import ovm.core.domain.Method;
import ovm.core.repository.TypeName;
import ovm.core.repository.Descriptor;
import ovm.core.repository.ExceptionHandler;
import ovm.core.repository.RepositoryMember;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.Frame;
import ovm.util.OVMError;
import s3.services.bytecode.analysis.FixpointIterator;
import s3.services.bytecode.analysis.S3AbstractInterpreter;
import s3.services.bytecode.analysis.S3Frame;
import s3.services.bytecode.analysis.S3Heap;
import s3.core.domain.S3ByteCode;

/**
 * This class implements a fixpoint iteration for the
 * AbstractInterpreter as implemented by S3AbstractInterpreter.  The
 * Iterator iterates until all possible abstract states are
 * found. Note that the implementation of the AbstractValue determines
 * which states are considered different.<p>
 * 
 * @see S3AbstractInterpreter
 * @author Christian Grothoff, jv
 **/
public class VerificationFixpointIterator
    extends FixpointIterator {

    /**
     * The Factory to create AbstractValues.
     */
    protected final AbstractValue.Factory avf;

    /**
     * Create a fixpoint iterator for the abstract execution of bytecode
     * with the S3AbstractInterpreter.
     **/
    public VerificationFixpointIterator(
        AbstractValue.Factory avf,
        S3AbstractInterpreter ai,
        Method me) {
	super(ai, me);
	this.avf = avf;
    }

    public VerificationFixpointIterator(
        AbstractValue.Factory avf,
        S3AbstractInterpreter ai,
        RepositoryMember.Method me) {
	super(ai, null);
	this.avf = avf;
	throw new OVMError.Unimplemented("constructor no longer supported");
    }

    public VerificationFixpointIterator(
        AbstractValue.Factory avf,
        S3AbstractInterpreter ai,
        Method me,
        InstructionBuffer code) {
	super(ai, me, code);
	this.avf = avf;
    }

    /**
     * Initialize frame with the abstract values.
     */
    public Frame makeInitialFrame() {	
        S3ByteCode rbcf 
	    = method.getByteCode();
        Frame frame 
	    = new S3Frame.Factory
	    (new S3Heap.Factory())
	    .makeFrame(rbcf.getMaxStack(),
		       rbcf.getMaxLocals());
        Descriptor.Method desc 
	    = sel.getDescriptor();

        int pos = 0, cnt = 0, argCount = desc.getArgumentCount();
        if (!method.getMode().isStatic())
            frame.store(pos++,
			avf.typeName2AbstractValue(sel.getDefiningClass()));
        while (cnt < argCount) {
            TypeName tn = desc.getArgumentType(cnt);
            frame.store(pos++, 
			avf.typeName2AbstractValue(tn));
            if (desc.isArgumentWidePrimitive(cnt++))
                frame.store(pos++, null);
            // we should probably use null, not invalid for these!
        }
        return frame;
    }

    /**
     * Create the abstract value that should be pushed
     * on the stack on entry to the specified
     * execption handler.
     */
    protected AbstractValue makeExceptionHandlerAV(ExceptionHandler eh) {
	return avf.makeReference(eh.getCatchTypeName());
    }

} // end of VerificationFixpointIterator
