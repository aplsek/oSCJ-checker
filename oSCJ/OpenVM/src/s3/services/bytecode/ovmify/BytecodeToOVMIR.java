package s3.services.bytecode.ovmify;

import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.editor.Cursor;
import ovm.services.bytecode.editor.InstructionEditVisitor;
import s3.core.S3Base;

public class BytecodeToOVMIR extends S3Base {
    
    /**
     * This visitor performs a linear pass over the code, fixing up static accesses 
     * and translating array references.
     * @author Christian Grothoff
     **/
    public static class Converter extends InstructionEditVisitor 
    	implements JVMConstants.Opcodes {
        
        public Converter() {}

	public void visit(Instruction.NEWARRAY i) {
            Cursor c = replaceInstruction();
	    c.addSINGLEANEWARRAY(i.getArrayName(buf));
	}
	public void visit(Instruction.ANEWARRAY i) {
            Cursor c = replaceInstruction();
	    c.addSINGLEANEWARRAY(i.getArrayName(buf, cp));
	}
	// I'm not sure why javac sometimes generates MULTIANEWARRAY 1
	// instructions, but CodeFragmentEditor will optimize them to
	// ANEWARRAY instructions when editing is finished.
	public void visit(Instruction.MULTIANEWARRAY i) {
	    if (i.getDimensions(buf) == 1) {
		Cursor c = replaceInstruction();
		c.addSINGLEANEWARRAY(i.getClassName(buf, cp));
	    }
	}

	public void visit(Instruction.GETSTATIC i) {
	    Selector.Field sel = i.getSelector(buf, cp);           
            TypeName.Scalar tns = sel.getDefiningClass().asScalar();
            Cursor c = replaceInstruction();
	    TypeName.Gemeinsam tng = tns.getGemeinsamTypeName();
            sel = Selector.Field.make(sel.getUnboundSelector(), tng);
	    c.addGETSTATIC(sel);
	}

	public void visit(Instruction.PUTSTATIC i) {
	    Selector.Field sel = i.getSelector(buf, cp);            
            TypeName.Scalar tns = sel.getDefiningClass().asScalar();
            Cursor c = replaceInstruction();
  	    TypeName.Gemeinsam tng = tns.getGemeinsamTypeName();
            sel = Selector.Field.make( sel.getUnboundSelector(), tng);
            c.addPUTSTATIC(sel);
	}

	public void visit(Instruction.INVOKESTATIC i) {
            Selector.Method sel = i.getSelector(buf, cp);            
	    TypeName.Scalar tns = sel.getDefiningClass().asScalar();
            Cursor c = replaceInstruction();
            TypeName.Gemeinsam tng = tns.getGemeinsamTypeName();
	    sel = Selector.Method.make(sel.getUnboundSelector(), tng);
	    c.addINVOKESTATIC(sel);
        }

	// Replace LDC CONSTANT_Class with LDC CONSTANT_SharedState.
	// Leave CONSTANT_Class entries in place, in case the same
	// entry is used for NEW, CHECKCAST, &c.
	public void visit(Instruction.ConstantPoolLoad i) {
	    int idx = i.getCPIndex(buf);
	    switch (cp.getTagAt(idx)) {
	    case JVMConstants.CONSTANT_Class:
	    case JVMConstants.CONSTANT_ResolvedClass:
		TypeName tn = cp.getClassAt(idx).asTypeName();
		Cursor c = cfe.replaceInstruction();
		c.addLdc(tn.getGemeinsamTypeName());
		break;
	    }
	}
	private Cursor replaceInstruction() {
	    int pc = buf.getPC();
	    cfe.removeInstruction(pc);
	    return cfe.getCursorAfterMarker(pc);
	}
    } 
} 
