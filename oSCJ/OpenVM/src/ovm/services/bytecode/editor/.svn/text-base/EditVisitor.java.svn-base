package ovm.services.bytecode.editor;

import ovm.core.repository.Mode;
import ovm.core.repository.Bytecode;
import ovm.core.repository.CloneClassVisitor;
import ovm.core.repository.ConstantPoolBuilder;
import ovm.core.repository.DiffConstantPoolBuilder;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.UnboundSelector;

/**
 * Visitor that walks over a class and modifies methods.  This visitor
 * is a basic driver for rewriting classes.  A typical use would look
 * like this:<p>
 *
 * <code>
 * RepositoryClass rcOld = ...;
 * EditVisitor.Controller gen = new MyEditController();
 * EditVisitor v = new EditVisitor(pke, gen, false);
 * v.visitClass(rcOld);
 * RepositoryClass rcNew = v.commit();
 * </code><p>
 *
 * Here, <tt>MyEditController</tt> is a class that has a method
 * <tt>run</tt> which is invoked to perform the actual editing of the
 * code using the editing framework.<p>
 *
 * ConstantPools deserve special attention since a single ConstantPool
 * exists per class. The EditVisitor preserves the entries of the
 * existing constant pool that are referenced after the rewriting and
 * extends it by entries that are added to any of the methods.<p>
 * Thus we end up creating per-codefragment constant pools, use the
 * ClassCleaner to fix that (see also S3Dumper for auto-reconcilliation
 * of constant pools at dump time).
 * 
 * @author Christian Grothoff
 **/
public class EditVisitor
    extends CloneClassVisitor {
    
    /**
     * The visitor used to update the bytecode.
     **/
    protected final Controller controller;

    /**
     * Number of arguments (incl. this, double & long count twice) of the
     * currently visited method (valid only at visitCodeFragment!).  This
     * represents the <i>minimum</i> number of local variables in the
     * current code fragment due to parameters. The actual number of local
     * variables may be higher.
     **/
    protected char minLocals;

    /**
     * Should the stack height (and number of local variables) be inferred?
     **/
    protected boolean inferStackHeight_;

    /**
     * The name of the currently visited method.
     **/
    protected UnboundSelector.Method meSel;
   
    /**
     * Construct a visitor that infers stack height.
     **/
    public EditVisitor(Controller c) {
	this( c, true);
    }
   
    /**
     * Constructor. 
     * @param inferStack true if stack height should be inferred, false
     *                   otherwise
     **/
    public EditVisitor(Controller c, 
		       boolean inferStack) {
	this.controller = c;
	this.inferStackHeight_ = inferStack;
    }

    /**
     * Visits a method in the class to be cloned and declare this method
     * in the class builder.
     * @param x the cloned method to be declared in the class builder
     **/
    public void visitMethod(RepositoryMember.Method x) {
	this.meSel = x.getUnboundSelector();
	minLocals = (char) ( x.getDescriptor().getArgumentLength()/4);
	super.visitMethod(x);
    }
   
    /**
     * Visits the currently visited method's modifiers and sets
     * these in the method builder that will be used to build this method
     * for the class builder. Also initializes values that will be used if
     * stack height is to be inferred (0 is this is a static method, 1
     * if it is not)
     * @param x the currently visited methods's modifiers object
     **/
    public void visitMethodMode(Mode.Method x) {
	rMethodInfoBuilder.setMode(x);
	if (! x.isStatic())
	    minLocals++;
    }

    /**
     **/
    public void visitByteCodeFragment(Bytecode x) {
	ConstantPoolBuilder prev = rConstantPoolBuilder;
	// KP: isn't this broken? we create per-bytecode fragment constant pools?
	// CG: Yes, this is ok, we can fix it later if we need to.
	rConstantPoolBuilder = new DiffConstantPoolBuilder(x.getConstantPool());
	CodeFragmentEditor cfe = new CodeFragmentEditor(x,
							rConstantPoolBuilder);
	controller.run(cfe);
	Bytecode.Builder fragBuilder = new Bytecode.Builder();
	if (inferStackHeight_)
	    cfe.commit(fragBuilder,
		       minLocals);
	else
	    cfe.commit(fragBuilder,
		       x.getMaxStack(), 
		       x.getMaxLocals());
	fragBuilder.setConstantPool(rConstantPoolBuilder.build());
	super.visitByteCodeFragment(fragBuilder.build());
	rConstantPoolBuilder = prev; // restore
    }

    /**
     * Controller that is invoked to perform the actual editing
     * for each codefragment.<p>
     * 
     * Implementations will typically use an InstructionVisitor,
     * so the arguments to run conveniently support the creation
     * of one.
     * 
     * @see LinearPassController
     * @author Christian Grothoff
     **/
    public interface Controller {
	/**
	 * This run method is invoked for each codefragment that
	 * is edited. The Controller is then responsible for
	 * performing the appropriate editing.
	 * @param cfe null is passed for native methods!
	 **/
	public void run(CodeFragmentEditor cfe);

    } // end of Controller

} // end of EditVisitor
