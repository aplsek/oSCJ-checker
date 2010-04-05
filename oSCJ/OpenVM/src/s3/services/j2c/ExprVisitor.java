package s3.services.j2c;
import ovm.services.bytecode.SpecificationIR.*;
import s3.services.j2c.J2cValue.*;
import s3.services.j2c.BBSpec.*;
import org.ovmj.util.Runabout;

/**
 * Walk the syntax tree for a {@link Expr}.
 * There are many subtypes of
 * {@link ValueSource} that are
 * not used in the {@link BBSpec} representation.
 * Visit methods for these types are not defined.
 **/
public abstract class ExprVisitor extends Runabout {
    public void visit(Expr e) {
	throw new Error("visit not implemented for " + e);
    }

    public void visit(IRExpr e) {
	visitAppropriate(e.source);
    }

    public void visit(BCExpr e) {
	for (int i = 0; i < e.inputs.length; i++)
	    visitAppropriate(e.inputs[i]);
    }

    public void visit(ValueSource vs) {
	throw new Error("visit not implemented for " + vs);
    }

    public void visit(RootsArrayBaseAccessExp e) {
    }
    

    public void visit(RootsArrayOffsetExp e) {
    }

    public void visit(JValueExp e) {
	visitAppropriate(e.source);
    }

    public void visit(SeqExp e) {
	for (int i = 0; i < e.v.length; i++)
	    visitAppropriate(e.v[i]);
    }

    public void visit(ClinitExp e) {
	visitAppropriate(e.csaCall);
    }

    public void visit(InvocationExp e) {
	visitAppropriate(e.target);
	for (int i = 0; i < e.args.length; i++)
	    if (e.args[i] != null)
		// Watch out for wide arguments (dammit)
		visitAppropriate(e.args[i]);
    }

    public void visit(J2cFieldAccessExp e) {
	visitAppropriate(e.obj);
    }

    public void visit(ValueAccessExp e) {
	visitAppropriate(e.v);
    }

    public void visit(J2cLookupExp e) {
	visitAppropriate(e.bp);
	visitAppropriate(e.index);
    }

    public void visit(DimensionArrayExp e) {
	for (int i = 0; i < e.dims.length; i++)
	    visitAppropriate(e.dims[i]);
    }

    public void visit(CCastExp e) {
	visitAppropriate(e.exp);
    }

    public void visit(BinExp e) {
	visitAppropriate(e.lhs);
	visitAppropriate(e.rhs);
    }

    public void visit(UnaryExp e) {
	visitAppropriate(e.arg);
    }

    public void visit(ConversionExp e) {
	visitAppropriate(e.before);
    }

    public void visit(AssignmentExp e) {
	visitAppropriate(e.dest);
	visitAppropriate(e.src);
    }

    public void visit(CondExp e) {
	visitAppropriate(e.lhs);
	visitAppropriate(e.rhs);
    }

    public void visit(IfExp e) {
	visitAppropriate(e.cond);
	visitAppropriate(e.ifTrue);
	if (e.ifFalse != null)
	    visitAppropriate(e.ifFalse);
    }

    public void visit(MemExp e) {
	visitAppropriate(e.addr);
	visitAppropriate(e.offset);
    }

    public void visit(ArrayAccessExp e) {
	visitAppropriate(e.arr);
	visitAppropriate(e.index);
    }

    public void visit(ArrayLengthExp e) {
	visitAppropriate(e.arr);
    }

    public void visit(BlueprintAccessExp e) {
	visitAppropriate(e.ref);
    }
    
    public void visit(ShiftMaskExp e) {
	visitAppropriate(e.exponent);
	visitAppropriate(e.sizeType);
    }
    
    public void visit(SymbolicConstant e) { }

    public void visit(ReinterpretExp e) {
	visitAppropriate(e.before);
    }

    public void visit(PCRefExp e) { }

    public void visit(J2cValue v) {
	if (!v.isConcrete() && v.name == null && v.source != null)
	    visitAppropriate(v.source);
    }
}
