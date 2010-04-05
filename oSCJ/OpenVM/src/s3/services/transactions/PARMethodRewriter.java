package s3.services.transactions;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeName;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.editor.CodeFragmentEditor;
import ovm.services.bytecode.editor.Cursor;
import ovm.services.bytecode.editor.Marker;
import ovm.services.bytecode.editor.CodeFragmentEditor.ExceptionHandlerList;
import s3.core.domain.S3Blueprint;
import ovm.core.domain.ConstantPool;
import s3.services.bytecode.ovmify.IRewriter;

/**
 * This rewriter takes a method annotated as Preemptible Atomic and adds
 * a prologue that starts a transaction and an epilogue the commits it.
 */
public class PARMethodRewriter extends IRewriter.RewriteVisitor {

    public PARMethodRewriter(IRewriter ir, S3Blueprint bp,  CodeFragmentEditor c,
			     ConstantPool rcpb) {
	ir.super(bp, false, false); cfe = c;
	_rcpb = rcpb;
    }

    ConstantPool _rcpb;
    Marker startofmeth;
    Marker endofmeth;
    int lastPC;
    Marker startofexception;
    Instruction.ReturnInstruction returnI;
    /**
     * Returns are transformed into gotos to the epilogue.
     */
    public void visit(Instruction.ReturnInstruction r) {
	cfe.replaceInstruction().addGoto(endofmeth);
	returnI = r;
    }
    /**
     * Insert a Transaction.start() before the first instruction of the method.
     * The jump target Marker is before the method call, the exception range
     * starts after.
     */
    public void beginEditing(InstructionBuffer buf, CodeFragmentEditor cfe) {
	super.beginEditing(buf, cfe);
	// FIXME: there has to be a better way to get the last PC
	while (buf.hasRemaining()) buf.get();
	lastPC = buf.getPC();
	buf.rewind();
	Cursor c = cfe.getCursorBeforeMarker(0);
	startofmeth = c.addMarker();
	// if (isSystemTypeContext_) {
	// ovm.core.execution.Native.print_string("adding " + Transaction.kernel_start + " to " + buf.getSelector() + "\n");
	// } 

	ConstantMethodref transstart =
	    this.isExecutive_?Transaction.kernel_start:Transaction.Transaction_start;
	
	c.addINVOKESTATIC(transstart);
	_rcpb.addMethodref(transstart);

	startofexception = c.addMarker();
	endcursor = cfe.getCursorAfterMarker(lastPC);	
	endofmeth = endcursor.makeUnboundMarker();
    }
    Cursor endcursor;
    /**
     * Add a backbranch, followed by a call to commit and a return of the 
     * proper kind. The jump is the handler for any aborted exception.     
     */
    public void endEditing() {
	Cursor c = endcursor;
	Marker jump = c.addMarker();
	c.addSimpleInstruction(POP);
	c.addGoto(startofmeth);	
	Marker finallyMark = c.addMarker();

	ConstantMethodref transstart =
	    this.isExecutive_?Transaction.kernel_start:Transaction.Transaction_start;
	ConstantMethodref transcommit =
	    this.isExecutive_?Transaction.kernel_commit:Transaction.Transaction_commit;
	TypeName.Scalar transexception = 
	    this.isExecutive_?Transaction.ED_ABORTED_EXCEPTION:Transaction.ABORTED_EXCEPTION;
	
	
	c.addINVOKESTATIC(transcommit);	
	_rcpb.addMethodref(transstart);
	c.addSimpleInstruction(ATHROW);
	c.bindMarker(endofmeth);
	c.addINVOKESTATIC(transcommit);	
	c.addSimpleInstruction((returnI!=null)?returnI.getOpcode():RETURN);	
	
	ExceptionHandlerList handlers = cfe.getExceptionHandlers();
	handlers.insertBefore(startofexception, jump, jump, transexception);
	handlers.insertBefore(startofexception, jump, finallyMark, JavaNames.java_lang_Throwable);
	super.endEditing();
    }
}
