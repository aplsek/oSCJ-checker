package ovm.services.bytecode.editor;

import ovm.core.domain.Method;
import ovm.core.domain.ConstantPool;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import s3.core.domain.S3ByteCode;

/**
 * An InstructionVisitor that is called upon per-method by an appropriate
 * EditVisitor.Controller. The most common use of this class is via the
 * LinearPassController.
 * @author Filip Pizlo
 */
public abstract class InstructionEditVisitor extends Instruction.IVisitor {
    /**
     * The editor for the current method.
     */
    protected CodeFragmentEditor cfe;

    public InstructionEditVisitor() {
        super(null);
    }

    public void run(Method m) {
	S3ByteCode code = m.getByteCode();
	S3ByteCode.Builder builder = new S3ByteCode.Builder(m.getByteCode());
	ConstantPool rcpb = code.getConstantPool();
	CodeFragmentEditor cfe = new CodeFragmentEditor(code, rcpb);
	InstructionBuffer codeBuf = cfe.getOriginalCode();
	codeBuf.rewind();
	beginEditing(codeBuf, cfe);
	try {
	    while (codeBuf.hasRemaining()) {
		visitAppropriate(codeBuf.get());
	    }
	} finally {
	    endEditing();
	}
	if (cfe.wasEdited()) {
	    cfe.commit(builder, builder.getMaxStack(), builder.getMaxLocals());
	    code.bang(builder.build());
	}
    }
	
    public void beginEditing(InstructionBuffer buf_, CodeFragmentEditor cfe_) {
        this.buf = buf_;
	this.cp = buf.getConstantPool();
        this.cfe = cfe_;
    }

    public void endEditing() {
        this.buf = null;
        this.cfe = null;
    }
}
