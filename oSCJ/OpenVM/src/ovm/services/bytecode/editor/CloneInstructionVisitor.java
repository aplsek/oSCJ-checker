
package ovm.services.bytecode.editor;


import ovm.core.repository.TypeName;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionVisitor;
import ovm.services.bytecode.JVMConstants;
import ovm.util.ByteBuffer;

/**
 * Visits all instructions and copy them into the cursor.<p>
 * 
 * Use this class to clone parts of a method. Subclass and 
 * override methods to change the translations of a few instructions.
 *
 * @author Christian Grothoff
 * @see ovm.services.bytecode.Instruction
 **/
public class CloneInstructionVisitor extends Instruction.IVisitor implements JVMConstants {

    /**
     * The visitor will append the code for each visited instruction
     * to this Cursor. 
     **/
    protected Cursor cursor;

    /**
     * The editor that we obtain markers from.
     **/
    protected final CodeFragmentEditor editor;
    
    public CloneInstructionVisitor(InstructionBuffer code, CodeFragmentEditor editor) {
	super(code);
	this.editor = editor;
    }
    protected Marker getMarkerAtPC(int pc) {
	return editor.getMarkerAtPC(pc);
    }
    
    public void setCursor(Cursor cursor) {
	this.cursor = cursor;
    }
    public void visit(Instruction i) {
	if (i.size(buf) == 1) {
	    cursor.addSimpleInstruction(i.getOpcode());
	    return;
	}
      	if ( i instanceof Instruction.ConstantPoolRead )
	    throw new Error( "CloneInstructionVisitor requires an explicit " +
	      	      	     "visit method for " + i.getName() + ", which " +
			     "accesses the constant pool");
	byte[] seq = new byte [ i.size(buf) ];
	ByteBuffer bb = getCode();
	bb.position(getPC());
	bb.get(seq);
	cursor.addSpecialSequence( seq);
    }

    public void visit(Instruction.NOP  i) {	
	cursor.addSimpleInstruction(Opcodes.NOP);
    }
    public void visit(Instruction.LABEL i) {
	cursor.addLABEL(i.get(buf));
    }
    public void visit(Instruction.GETFIELD i) {
	cursor.addGETFIELD(i.getConstantFieldref(buf,cp));//       	getSelector(buf, cp));getSelector(buf, cp));
    }
    public void visit(Instruction.PUTFIELD i) {
        cursor.addPUTFIELD(i.getConstantFieldref(buf,cp));//       	getSelector(buf, cp));
    }
    public void visit(Instruction.PUTFIELD_WITH_BARRIER_REF i) {
        cursor.addPUTFIELD_WITH_BARRIER_REF(i.getConstantFieldref(buf,cp));//       	getSelector(buf, cp));getSelector(buf, cp));
    }
    public void visit(Instruction.AASTORE_WITH_BARRIER i) {
     cursor.addSimpleInstruction(Opcodes.AASTORE_WITH_BARRIER);
    }
    public void visit(Instruction.READ_BARRIER i) {
     cursor.addSimpleInstruction(Opcodes.READ_BARRIER);
    } 
    public void visit(Instruction.CHECKING_TRANSLATING_READ_BARRIER i) {
     cursor.addSimpleInstruction(Opcodes.CHECKING_TRANSLATING_READ_BARRIER);
    }
    public void visit(Instruction.NONCHECKING_TRANSLATING_READ_BARRIER i) {
     cursor.addSimpleInstruction(Opcodes.NONCHECKING_TRANSLATING_READ_BARRIER);
    }    
    public void visit(Instruction.GETSTATIC i) {
 	cursor.addGETSTATIC(i.getConstantFieldref(buf, cp));
    }
    public void visit(Instruction.PUTSTATIC i) {
	cursor.addPUTSTATIC(i.getConstantFieldref(buf, cp));
    }
    public void visit(Instruction.PUTSTATIC_WITH_BARRIER_REF i) {
 	cursor.addPUTSTATIC_WITH_BARRIER_REF(i.getConstantFieldref(buf, cp));
    }
    public void visit(Instruction.INVOKESTATIC i) {
	cursor.addINVOKESTATIC(i.getConstantMethodref(buf, cp));
    }
    public void visit(Instruction.INVOKEVIRTUAL i) {
 	cursor.addINVOKEVIRTUAL(i.getConstantMethodref(buf, cp));
    }
    public void visit(Instruction.INVOKEINTERFACE i) {
	cursor.addINVOKEINTERFACE(i.getConstantMethodref(buf, cp));
    }
    public void visit(Instruction.INVOKESPECIAL i) {
	cursor.addINVOKESPECIAL(i.getConstantMethodref(buf, cp));
    }
    public void visit(Instruction.NEW  i) {
	cursor.addNEW(i.getResultType(buf, cp));
    }
    public void visit(Instruction.NEWARRAY i) {
	cursor.addNewArray(i.getPrimitiveType(buf));
    }
    public void visit(Instruction.ANEWARRAY i) {
	cursor.addNewArray(TypeName.Array.make(i.getClassName(buf, cp), 1), 1);
    }
    public void visit(Instruction.SINGLEANEWARRAY i) {
	cursor.addSINGLEANEWARRAY(i.getResultType(buf, cp));
    }
    public void visit(Instruction.LOAD_SHST_FIELD i) {
	cursor.addLOAD_SHST_FIELD(i.getConstantFieldref(buf, cp));
    }
    public void visit(Instruction.LOAD_SHST_METHOD i) {
	cursor.addLOAD_SHST_METHOD(i.getConstantMethodref(buf, cp));
    }
    public void visit(Instruction.LOAD_SHST_FIELD_QUICK i) {
	cursor.addLOAD_SHST_FIELD_QUICK(i.getConstantFieldref(buf, cp));
    }
    public void visit(Instruction.LOAD_SHST_METHOD_QUICK i) {
	cursor.addLOAD_SHST_METHOD_QUICK(i.getConstantMethodref(buf, cp));
    }
    public void visit(Instruction.MULTIANEWARRAY i) {
	cursor.addNewArray(i.getResultType(buf, cp), i.getDimensions(buf));
    }
    public void visit(Instruction.ARRAYLENGTH i) {
	cursor.addSimpleInstruction(Opcodes.ARRAYLENGTH);
    }
    public void visit(Instruction.CHECKCAST i) {
	cursor.addCHECKCAST(i.getResultType(buf, cp));
    }
    public void visit(Instruction.INSTANCEOF i) {
	cursor.addINSTANCEOF(i.getConstantClass(buf, cp));
    }
    public void visit(Instruction.AFIAT i) {
	cursor.addFiat(i.getConstantClass(buf, cp));
    }
    public void visit(Instruction.IFIAT i) {
	cursor.addFiat(i.getResultTypeName(buf, cp));
    }
    public void visit(Instruction.FFIAT i) {
	cursor.addFiat(i.getResultTypeName(buf, cp));
    }
    public void visit(Instruction.LFIAT i) {
	cursor.addFiat(i.getResultTypeName(buf, cp));
    }
    public void visit(Instruction.DFIAT i) {
	cursor.addFiat(i.getResultTypeName(buf, cp));
    }
    public void visit(Instruction.MONITORENTER  i) {
	cursor.addSimpleInstruction(Opcodes.MONITORENTER);
    }
    public void visit(Instruction.MONITOREXIT  i) {
	cursor.addSimpleInstruction(Opcodes.MONITOREXIT);
    }
    public void visit(Instruction.AALOAD i) {
	cursor.addSimpleInstruction(Opcodes.AALOAD);
    }
    public void visit(Instruction.IALOAD i) {
	cursor.addSimpleInstruction(Opcodes.IALOAD);
    }
    public void visit(Instruction.FALOAD i) {
 	cursor.addSimpleInstruction(Opcodes.FALOAD);
    }
    public void visit(Instruction.DALOAD i) {
	cursor.addSimpleInstruction(Opcodes.DALOAD);
    }
    public void visit(Instruction.LALOAD i) {
	cursor.addSimpleInstruction(Opcodes.LALOAD);
    }
    public void visit(Instruction.BALOAD i) {
	cursor.addSimpleInstruction(Opcodes.BALOAD);
    }
    public void visit(Instruction.CALOAD i) {
	cursor.addSimpleInstruction(Opcodes.CALOAD);
    }
    public void visit(Instruction.SALOAD i) {
	cursor.addSimpleInstruction(Opcodes.SALOAD);
    }
    public void visit(Instruction.AASTORE i) {
	cursor.addSimpleInstruction(Opcodes.AASTORE);
    }
    public void visit(Instruction.UNCHECKED_AASTORE i) {
	cursor.addSimpleInstruction(Opcodes.UNCHECKED_AASTORE);
    }
    public void visit(Instruction.IASTORE i) {
	cursor.addSimpleInstruction(Opcodes.IASTORE);
    }
    public void visit(Instruction.FASTORE i) {
	cursor.addSimpleInstruction(Opcodes.FASTORE);
    }
    public void visit(Instruction.DASTORE i) {
	cursor.addSimpleInstruction(Opcodes.DASTORE);
    }
    public void visit(Instruction.LASTORE i) {
	cursor.addSimpleInstruction(Opcodes.LASTORE);
    }
    public void visit(Instruction.BASTORE i) {
	cursor.addSimpleInstruction(Opcodes.BASTORE);
    }
    public void visit(Instruction.CASTORE i) {
	cursor.addSimpleInstruction(Opcodes.CASTORE);
    }
    public void visit(Instruction.SASTORE i) {
	cursor.addSimpleInstruction(Opcodes.SASTORE);
    }
    public void visit(Instruction.POLLCHECK i) {
	cursor.addSimpleInstruction(Opcodes.POLLCHECK);
    }
    public void visit(Instruction.NULLCHECK i) {
	cursor.addSimpleInstruction(Opcodes.NULLCHECK);
    }

    public void visit(Instruction.ACONST_NULL i) {
	cursor.addSimpleInstruction(Opcodes.ACONST_NULL);
    }
    public void visit(Instruction.ICONST_M1 i) {
 	cursor.addLoadConstant(-1);
    }
    public void visit(Instruction.ICONST_0 i) {
	cursor.addLoadConstant(0);
    }
    public void visit(Instruction.ICONST_1 i) {
	cursor.addLoadConstant(1);
    }
    public void visit(Instruction.ICONST_2 i) {
	cursor.addLoadConstant(2);
    }
    public void visit(Instruction.ICONST_3 i) {
	cursor.addLoadConstant(3);
    }
    public void visit(Instruction.ICONST_4 i) {
	cursor.addLoadConstant(4);
    }
    public void visit(Instruction.ICONST_5 i) {
 	cursor.addLoadConstant(5);
    }
    public void visit(Instruction.LCONST_0 i) {
	cursor.addLoadConstant(0L);
    }
    public void visit(Instruction.LCONST_1 i) {
	cursor.addLoadConstant(1L);
    }
    public void visit(Instruction.FCONST_0 i) {
 	cursor.addLoadConstant(0.0f);
    }
    public void visit(Instruction.FCONST_1 i) {
	cursor.addLoadConstant(1.0f);
    }
    public void visit(Instruction.FCONST_2 i) {
	cursor.addLoadConstant(2.0f);
    }
    public void visit(Instruction.DCONST_0 i) {
	cursor.addLoadConstant(0.0d);
    }
    public void visit(Instruction.DCONST_1 i) {
 	cursor.addLoadConstant(1.0d);
    }
    public void visit(Instruction.SIPUSH i) {
 	cursor.addLoadConstant(i.getValue(buf));
    }
    public void visit(Instruction.BIPUSH i) {
	cursor.addLoadConstant(i.getValue(buf));
    }
    public void visit(Instruction.LDC i) {
	Object val = i.getValue(buf, cp);
	if (val == null) throw new Error("LDC has constant value NULL!? " + getSelector());
	cursor.addLdc(val);
    }
    public void visit(Instruction.LDC_W i) {
	cursor.addLdc(i.getValue(buf, cp));
    }
    public void visit(Instruction.LDC2_W i) {
	cursor.addLdc(i.getValue(buf, cp));
    }
    public void visit(Instruction.LDC_INT_QUICK i) {
	cursor.addQuickLoadConstant(i.getValue(buf));
    }
    public void visit(Instruction.LDC_FLOAT_QUICK i) {
	cursor.addQuickLoadConstant(i.getFValue(buf));
    }
    public void visit(Instruction.LDC_LONG_QUICK i) {
	cursor.addQuickLoadConstant(i.getLValue(buf));
    }
    public void visit(Instruction.LDC_DOUBLE_QUICK i) {
	cursor.addQuickLoadConstant(i.getDValue(buf));
    }
    public void visit(Instruction.LDC_REF_QUICK i) {
	cursor.addQuickLdc(i.getValue(buf, cp));
    }
    public void visit(Instruction.LDC_W_REF_QUICK i) {
	cursor.addQuickLdc(i.getValue(buf, cp));
    }
//     public void visit(Instruction.ASTORE_0 i) {
// 	cursor.addSimpleInstruction(Opcodes.ASTORE_0);
//     }
//     public void visit(Instruction.ASTORE_1 i) { 
// 	cursor.addSimpleInstruction(Opcodes.ASTORE_1);
//     }
//     public void visit(Instruction.ASTORE_2 i) { 
// 	cursor.addSimpleInstruction(Opcodes.ASTORE_2); 
//     }
//     public void visit(Instruction.ASTORE_3 i) {
// 	cursor.addSimpleInstruction(Opcodes.ASTORE_3);
//     }
    public void visit(Instruction.ASTORE i) { 
	cursor.addAStore((char)i.getLocalVariableOffset(buf)); 
    }
//     public void visit(Instruction.ISTORE_0 i) {
// 	cursor.addSimpleInstruction(Opcodes.ISTORE_0);
//     }
//     public void visit(Instruction.ISTORE_1 i) {
// 	cursor.addSimpleInstruction(Opcodes.ISTORE_1);
//     }
//     public void visit(Instruction.ISTORE_2 i) { 
// 	cursor.addSimpleInstruction(Opcodes.ISTORE_2); 
//     }
//     public void visit(Instruction.ISTORE_3 i) {
// 	cursor.addSimpleInstruction(Opcodes.ISTORE_3); 
//     }
    public void visit(Instruction.ISTORE i) {
 	cursor.addIStore((char)i.getLocalVariableOffset(buf)); 
    }
//     public void visit(Instruction.FSTORE_0 i) {
// 	cursor.addSimpleInstruction(Opcodes.FSTORE_0);
//     }
//     public void visit(Instruction.FSTORE_1 i) {
// 	cursor.addSimpleInstruction(Opcodes.FSTORE_1);
//     }
//     public void visit(Instruction.FSTORE_2 i) {
// 	cursor.addSimpleInstruction(Opcodes.FSTORE_2);
//     }
//     public void visit(Instruction.FSTORE_3 i) {
// 	cursor.addSimpleInstruction(Opcodes.FSTORE_3);
//     }
    public void visit(Instruction.FSTORE i) { 
	cursor.addFStore((char)i.getLocalVariableOffset(buf)); 
    }
//     public void visit(Instruction.DSTORE_0 i) {
// 	cursor.addSimpleInstruction(Opcodes.DSTORE_0); 
//     }
//     public void visit(Instruction.DSTORE_1 i) {
// 	cursor.addSimpleInstruction(Opcodes.DSTORE_1);
//     }
//     public void visit(Instruction.DSTORE_2 i) {
// 	cursor.addSimpleInstruction(Opcodes.DSTORE_2);
//     }
//     public void visit(Instruction.DSTORE_3 i) { 
// 	cursor.addSimpleInstruction(Opcodes.DSTORE_3); 
//     }
    public void visit(Instruction.DSTORE i) { 
 	cursor.addDStore((char)i.getLocalVariableOffset(buf)); 
    }
//     public void visit(Instruction.LSTORE_0 i) {
// 	cursor.addSimpleInstruction(Opcodes.LSTORE_0);
//     }
//     public void visit(Instruction.LSTORE_1 i) {
// 	cursor.addSimpleInstruction(Opcodes.LSTORE_1);
//     }
//     public void visit(Instruction.LSTORE_2 i) {
// 	cursor.addSimpleInstruction(Opcodes.LSTORE_2); 
//     }
//     public void visit(Instruction.LSTORE_3 i) {
// 	cursor.addSimpleInstruction(Opcodes.LSTORE_3); 
//     }
    public void visit(Instruction.LSTORE i) {
	cursor.addLStore((char)i.getLocalVariableOffset(buf)); 
    } 
//     public void visit(Instruction.ALOAD_0 i) {
// 	cursor.addSimpleInstruction(Opcodes.ALOAD_0);
//     }
//     public void visit(Instruction.ALOAD_1 i) {
// 	cursor.addSimpleInstruction(Opcodes.ALOAD_1); 
//     }
//     public void visit(Instruction.ALOAD_2 i) {
// 	cursor.addSimpleInstruction(Opcodes.ALOAD_2);
//     }
//     public void visit(Instruction.ALOAD_3 i) { 	
// 	cursor.addSimpleInstruction(Opcodes.ALOAD_3);
//     }
    public void visit(Instruction.ALOAD i) { 
	cursor.addALoad((char)i.getLocalVariableOffset(buf)); 
    }
//     public void visit(Instruction.ILOAD_0 i) { 
// 	cursor.addSimpleInstruction(Opcodes.ILOAD_0);
//     }
//     public void visit(Instruction.ILOAD_1 i) { 
// 	cursor.addSimpleInstruction(Opcodes.ILOAD_1); 
//     }
//     public void visit(Instruction.ILOAD_2 i) {
// 	cursor.addSimpleInstruction(Opcodes.ILOAD_2);
//     }
//     public void visit(Instruction.ILOAD_3 i) { 
// 	cursor.addSimpleInstruction(Opcodes.ILOAD_3);
//     }
    public void visit(Instruction.ILOAD i) {
	cursor.addILoad((char)i.getLocalVariableOffset(buf)); 
    }
//     public void visit(Instruction.FLOAD_0 i) { 
// 	cursor.addSimpleInstruction(Opcodes.FLOAD_0); 
//     }
//     public void visit(Instruction.FLOAD_1 i) { 
// 	cursor.addSimpleInstruction(Opcodes.FLOAD_1);
//     }
//     public void visit(Instruction.FLOAD_2 i) { 
// 	cursor.addSimpleInstruction(Opcodes.FLOAD_2); 
//     }
//     public void visit(Instruction.FLOAD_3 i) {
// 	cursor.addSimpleInstruction(Opcodes.FLOAD_3);
//     }
    public void visit(Instruction.FLOAD i) {
	cursor.addFLoad((char)i.getLocalVariableOffset(buf)); 
    }
//     public void visit(Instruction.DLOAD_0 i) {
// 	cursor.addSimpleInstruction(Opcodes.DLOAD_0);
//     }
//     public void visit(Instruction.DLOAD_1 i) {
// 	cursor.addSimpleInstruction(Opcodes.DLOAD_1);
//     }
//     public void visit(Instruction.DLOAD_2 i) {
// 	cursor.addSimpleInstruction(Opcodes.DLOAD_2);
//     }
//     public void visit(Instruction.DLOAD_3 i) { 
// 	cursor.addSimpleInstruction(Opcodes.DLOAD_3);
//     }
    public void visit(Instruction.DLOAD i) { 
	cursor.addDLoad((char)i.getLocalVariableOffset(buf)); 
    }
//     public void visit(Instruction.LLOAD_0 i) { 
// 	cursor.addSimpleInstruction(Opcodes.LLOAD_0); 
//     }    
//     public void visit(Instruction.LLOAD_1 i) {
// 	cursor.addSimpleInstruction(Opcodes.LLOAD_1);
//     }
//     public void visit(Instruction.LLOAD_2 i) {
// 	cursor.addSimpleInstruction(Opcodes.LLOAD_2);
//     }
//     public void visit(Instruction.LLOAD_3 i) {
// 	cursor.addSimpleInstruction(Opcodes.LLOAD_3); 
//     }
    public void visit(Instruction.LLOAD i) {
	cursor.addLLoad((char)i.getLocalVariableOffset(buf)); 
    } 
    public void visit(Instruction.IADD i) { 
	cursor.addSimpleInstruction(Opcodes.IADD);
    }
    public void visit(Instruction.ISUB i) { 
	cursor.addSimpleInstruction(Opcodes.ISUB);
    }
    public void visit(Instruction.IMUL i) { 
	cursor.addSimpleInstruction(Opcodes.IMUL);
    }
    public void visit(Instruction.IDIV i) { 
	cursor.addSimpleInstruction(Opcodes.IDIV);
    }
    public void visit(Instruction.IOR i)   {
	cursor.addSimpleInstruction(Opcodes.IOR);
    }
    public void visit(Instruction.IAND i) { 
	cursor.addSimpleInstruction(Opcodes.IAND);
    }
    public void visit(Instruction.IREM i) {
	cursor.addSimpleInstruction(Opcodes.IREM);
    }
    public void visit(Instruction.ISHL i) { 
	cursor.addSimpleInstruction(Opcodes.ISHL);
    }
    public void visit(Instruction.IUSHR i) { 
	cursor.addSimpleInstruction(Opcodes.IUSHR);
    }  
    public void visit(Instruction.IXOR i) {
	cursor.addSimpleInstruction(Opcodes.IXOR); 
    }
    public void visit(Instruction.ISHR i) { 
	cursor.addSimpleInstruction(Opcodes.ISHR); 
    }
    public void visit(Instruction.IINC i) { 
	cursor.addIInc((char)i.getLocalVariableOffset(buf),
		       (short)i.getValue(buf));
    }
    public void visit(Instruction.INEG i) {
	cursor.addSimpleInstruction(Opcodes.INEG);
    }
    public void visit(Instruction.LADD i) {
	cursor.addSimpleInstruction(Opcodes.LADD);
    }
    public void visit(Instruction.LSUB i) {
	cursor.addSimpleInstruction(Opcodes.LSUB); 
    }
    public void visit(Instruction.LMUL i) {
	cursor.addSimpleInstruction(Opcodes.LMUL); 
    }
    public void visit(Instruction.LDIV i) {
	cursor.addSimpleInstruction(Opcodes.LDIV); 
    }
    public void visit(Instruction.LOR i) {
	cursor.addSimpleInstruction(Opcodes.LOR); 
    }
    public void visit(Instruction.LAND i) {
	cursor.addSimpleInstruction(Opcodes.LAND); 
    }
    public void visit(Instruction.LREM i) {
	cursor.addSimpleInstruction(Opcodes.LREM);
    }
    public void visit(Instruction.LSHL i) {
	cursor.addSimpleInstruction(Opcodes.LSHL);
    }
    public void visit(Instruction.LUSHR i) {
	cursor.addSimpleInstruction(Opcodes.LUSHR);
    }  
    public void visit(Instruction.LXOR i) { 
	cursor.addSimpleInstruction(Opcodes.LXOR); 
    }
    public void visit(Instruction.LSHR i) { 
	cursor.addSimpleInstruction(Opcodes.LSHR); 
    }
    public void visit(Instruction.LNEG i) {
	cursor.addSimpleInstruction(Opcodes.LNEG);
    }
    public void visit(Instruction.FADD i) {
	cursor.addSimpleInstruction(Opcodes.FADD);
    }
    public void visit(Instruction.FSUB i) {
	cursor.addSimpleInstruction(Opcodes.FSUB);
    }
    public void visit(Instruction.FMUL i) {
	cursor.addSimpleInstruction(Opcodes.FMUL);
    }
    public void visit(Instruction.FDIV i) {
	cursor.addSimpleInstruction(Opcodes.FDIV); 
    }
    public void visit(Instruction.FREM i) {
	cursor.addSimpleInstruction(Opcodes.FREM);
    }
    public void visit(Instruction.FNEG i) {
	cursor.addSimpleInstruction(Opcodes.FNEG);
    }
    public void visit(Instruction.DADD i) {
	cursor.addSimpleInstruction(Opcodes.DADD); 
    }
    public void visit(Instruction.DSUB i) {
	cursor.addSimpleInstruction(Opcodes.DSUB);
    }
    public void visit(Instruction.DMUL i) {
	cursor.addSimpleInstruction(Opcodes.DMUL);
    }
    public void visit(Instruction.DDIV i) { 
	cursor.addSimpleInstruction(Opcodes.DDIV);
    }
    public void visit(Instruction.DREM i) {
	cursor.addSimpleInstruction(Opcodes.DREM); 
    }
    public void visit(Instruction.DNEG i) {
	cursor.addSimpleInstruction(Opcodes.DNEG);
    }
    public void visit(Instruction.I2F i) {
	cursor.addSimpleInstruction(Opcodes.I2F);
    }
    public void visit(Instruction.I2L i) {
	cursor.addSimpleInstruction(Opcodes.I2L);
    }
    public void visit(Instruction.I2D i) {
	cursor.addSimpleInstruction(Opcodes.I2D);
    }
    public void visit(Instruction.I2B i) {
	cursor.addSimpleInstruction(Opcodes.I2B);
    }
    public void visit(Instruction.I2C i) {
	cursor.addSimpleInstruction(Opcodes.I2C); 
    }
    public void visit(Instruction.I2S i) {
	cursor.addSimpleInstruction(Opcodes.I2S);
    }
    public void visit(Instruction.L2I i) {
	cursor.addSimpleInstruction(Opcodes.L2I);
    }
    public void visit(Instruction.L2F i) {
	cursor.addSimpleInstruction(Opcodes.L2F); 
    }
    public void visit(Instruction.L2D i) {
	cursor.addSimpleInstruction(Opcodes.L2D);
    }
    public void visit(Instruction.D2I i) {
	cursor.addSimpleInstruction(Opcodes.D2I); 
    }
    public void visit(Instruction.D2L i) {
	cursor.addSimpleInstruction(Opcodes.D2L); 
    }
    public void visit(Instruction.D2F i) {
	cursor.addSimpleInstruction(Opcodes.D2F); 
    }    
    public void visit(Instruction.F2D i) {
	cursor.addSimpleInstruction(Opcodes.F2D);
    }
    public void visit(Instruction.F2I i) { 
	cursor.addSimpleInstruction(Opcodes.F2I); 
    }
    public void visit(Instruction.F2L i) {
	cursor.addSimpleInstruction(Opcodes.F2L); 
    }
    public void visit(Instruction.LCMP i) {
 	cursor.addSimpleInstruction(Opcodes.LCMP);
    }
    public void visit(Instruction.DCMPL i) {
	cursor.addSimpleInstruction(Opcodes.DCMPL);
    }
    public void visit(Instruction.FCMPL i) {
	cursor.addSimpleInstruction(Opcodes.FCMPL);
    }
    public void visit(Instruction.DCMPG i) {
	cursor.addSimpleInstruction(Opcodes.DCMPG);
    }
    public void visit(Instruction.FCMPG i) {
	cursor.addSimpleInstruction(Opcodes.FCMPG);
    }
//     public void visit(Instruction.IRETURN i) {
// 	cursor.addSimpleInstruction(Opcodes.IRETURN);
//     }
//     public void visit(Instruction.FRETURN i) {
// 	cursor.addSimpleInstruction(Opcodes.FRETURN);
//     }
//     public void visit(Instruction.DRETURN i) {
// 	cursor.addSimpleInstruction(Opcodes.DRETURN);
//     }
//     public void visit(Instruction.LRETURN i) {
// 	cursor.addSimpleInstruction(Opcodes.LRETURN);
//     }
//     public void visit(Instruction.ARETURN i) {
// 	cursor.addSimpleInstruction(Opcodes.ARETURN);
//     }
//     public void visit(Instruction.RETURN i) {
// 	cursor.addSimpleInstruction(Opcodes.RETURN);
//     }
    public void visit(Instruction.GOTO i) {
	Marker m = getMarkerAtPC(i.getTarget(buf)+getPC());
	cursor.addGoto(m);
    }
    public void visit(Instruction.GOTO_W i) {
	Marker m = getMarkerAtPC(i.getTarget(buf)+getPC());
	cursor.addGoto(m);
    }
    public void visit(Instruction.JSR  i) {
	// CHECK: is the JSR target RELATIVE???
	Marker m = getMarkerAtPC(i.getTarget(buf)+getPC());
	cursor.addJsr(m);
    }
    public void visit(Instruction.JSR_W i) {
	// CHECK: is the JSR target RELATIVE???
	Marker m = getMarkerAtPC(i.getTarget(buf)+getPC());
	cursor.addJsr(m);
    }
    public void visit(Instruction.RET i) {
 	cursor.addRet((char)i.getLocalVariableOffset(buf));
    }
    public void visit(Instruction.TABLESWITCH i) {
	int pc = getPC();
	Marker def = getMarkerAtPC(i.getDefaultTarget(buf) + pc);
	int[] targets = i.getTargets(buf);
	Marker[] tar = new Marker[targets.length];
	for (int j = 0; j < targets.length; j++)
	    tar[j] = getMarkerAtPC(targets[j] + pc);
	cursor.addSwitch(def, i.getIndexForTargets(buf), tar, Opcodes.TABLESWITCH);
    }

    public void visit(Instruction.LOOKUPSWITCH i) {
	int pc = getPC();
	Marker def = getMarkerAtPC(i.getDefaultTarget(buf) + pc);
	int[] targets = i.getTargets(buf);
	Marker[] tar = new Marker[targets.length];
	for (int j = 0; j < targets.length; j++)
	    tar[j] = getMarkerAtPC(targets[j] + pc);
	cursor.addSwitch(def, i.getIndexForTargets(buf), tar, Opcodes.LOOKUPSWITCH);
    }
    
    public void visit(Instruction.IFEQ i) {
	cursor.addIf(Opcodes.IFEQ,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IFNE i) {
 	cursor.addIf(Opcodes.IFNE,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IFLT i) {
 	cursor.addIf(Opcodes.IFLT,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IFGE i) {
 	cursor.addIf(Opcodes.IFGE,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IFGT i) {
	cursor.addIf(Opcodes.IFGT,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IFLE i) {
	cursor.addIf(Opcodes.IFLE,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IFNONNULL i) {
	cursor.addIf(Opcodes.IFNONNULL,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IFNULL i) {
	cursor.addIf(Opcodes.IFNULL,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IF_ACMPEQ i) {
	cursor.addIf(Opcodes.IF_ACMPEQ,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IF_ACMPNE i) {
	cursor.addIf(Opcodes.IF_ACMPNE,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IF_ICMPEQ i) {
 	cursor.addIf(Opcodes.IF_ICMPEQ,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IF_ICMPNE i) {
 	cursor.addIf(Opcodes.IF_ICMPNE,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IF_ICMPLT i) {
 	cursor.addIf(Opcodes.IF_ICMPLT,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IF_ICMPGE i) {
 	cursor.addIf(Opcodes.IF_ICMPGE,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IF_ICMPGT i) {
	cursor.addIf(Opcodes.IF_ICMPGT,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.IF_ICMPLE i) {
	cursor.addIf(Opcodes.IF_ICMPLE,
		     getMarkerAtPC(getPC()+i.getBranchTarget(buf)));
    }
    public void visit(Instruction.POP i) {
	cursor.addSimpleInstruction(Opcodes.POP);
    }
    public void visit(Instruction.POP2 i) {
 	cursor.addSimpleInstruction(Opcodes.POP2);
    }
    public void visit(Instruction.DUP i) {
 	cursor.addSimpleInstruction(Opcodes.DUP);
    }
    public void visit(Instruction.DUP_X1 i) {
	cursor.addSimpleInstruction(Opcodes.DUP_X1);
    }
    public void visit(Instruction.DUP_X2 i) {
	cursor.addSimpleInstruction(Opcodes.DUP_X2);
    }
    public void visit(Instruction.DUP2 i) {
	cursor.addSimpleInstruction(Opcodes.DUP2);
    }
    public void visit(Instruction.DUP2_X1 i) {
	cursor.addSimpleInstruction(Opcodes.DUP2_X1);
    }
    public void visit(Instruction.DUP2_X2 i) {
	cursor.addSimpleInstruction(Opcodes.DUP2_X2);
    }
    public void visit(Instruction.SWAP i) {
 	cursor.addSimpleInstruction(Opcodes.SWAP);
    }
    public void visit(Instruction.ATHROW i) {
 	cursor.addSimpleInstruction(Opcodes.ATHROW);
    }
    public void visit(Instruction.WIDE i) {
	this.visitAppropriate(i.specialize(buf));
    }   

    /* ************* visitors for special OVM opcodes as needed *************/
 
    public void visit(Instruction.ANEWARRAY_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.CHECKCAST_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.INSTANCEOF_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.NEW_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.MULTIANEWARRAY_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.GETFIELD_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.REF_GETFIELD_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.GETFIELD2_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.PUTFIELD_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.PUTFIELD2_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.PUTFIELD_QUICK_WITH_BARRIER_REF i) {
 	visit((Instruction)i);
     }
    public void visit(Instruction.INVOKEVIRTUAL_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.INVOKENONVIRTUAL_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.INVOKENONVIRTUAL2_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.INVOKESUPER_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.INVOKEINTERFACE_QUICK i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.ROLL i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.COPY i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.INVOKE_SYSTEM i) {
	visit((Instruction)i);
    }
    public void visit(Instruction.INVOKE_NATIVE i) {
	visit((Instruction)i);
    }



} // end of CloneInstructionVisitor

