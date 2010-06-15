/**
 **/
package ovm.services.bytecode;

import java.lang.reflect.Modifier;

import ovm.core.OVMBase;
import s3.util.PragmaTransformCallsiteIR.BCbootTime;

/**
 * This file provides the generic InstructionSet.
 * <p>
 * @see ovm.services.bytecode.Instruction
 * @author Christian Grothoff
 **/
public class InstructionSet extends OVMBase
    implements JVMConstants.Opcodes {

    /**
     * The only InstructionSet there is.
     **/
    public static final InstructionSet SINGLETON = new InstructionSet();
    
    /**
     * The array of instructions.
     **/
    public final Instruction set[] = new Instruction[512];
    
    /**
     * Constructor to set up the array
     */
    private InstructionSet() {
	set[AALOAD         ] = Instruction.AALOAD.singleton;
	set[AASTORE        ] = Instruction.AASTORE.singleton;
	set[ACONST_NULL    ] = Instruction.ACONST_NULL.singleton;
	set[ALOAD          ] = Instruction.ALOAD.singleton;
	set[ALOAD_0        ] = Instruction.ALOAD_0.singleton;
	set[ALOAD_1        ] = Instruction.ALOAD_1.singleton;
	set[ALOAD_2        ] = Instruction.ALOAD_2.singleton;
	set[ALOAD_3        ] = Instruction.ALOAD_3.singleton;
	set[ANEWARRAY      ] = Instruction.ANEWARRAY.singleton;
	set[ARETURN        ] = Instruction.ARETURN.singleton;
	set[ARRAYLENGTH    ] = Instruction.ARRAYLENGTH.singleton;
	set[ASTORE         ] = Instruction.ASTORE.singleton;
	set[ASTORE_0       ] = Instruction.ASTORE_0.singleton;
	set[ASTORE_1       ] = Instruction.ASTORE_1.singleton;
	set[ASTORE_2       ] = Instruction.ASTORE_2.singleton;
	set[ASTORE_3       ] = Instruction.ASTORE_3.singleton;
	set[ATHROW         ] = Instruction.ATHROW.singleton;
	set[BALOAD         ] = Instruction.BALOAD.singleton;
	set[BASTORE        ] = Instruction.BASTORE.singleton;
	set[BIPUSH         ] = Instruction.BIPUSH.singleton;
	set[CALOAD         ] = Instruction.CALOAD.singleton;
	set[CASTORE        ] = Instruction.CASTORE.singleton;
	set[CHECKCAST      ] = Instruction.CHECKCAST.singleton;
	set[D2F            ] = Instruction.D2F.singleton;
	set[D2I            ] = Instruction.D2I.singleton;
	set[D2L            ] = Instruction.D2L.singleton;
	set[DADD           ] = Instruction.DADD.singleton;
	set[DALOAD         ] = Instruction.DALOAD.singleton;
	set[DASTORE        ] = Instruction.DASTORE.singleton;
	set[DCMPG          ] = Instruction.DCMPG.singleton;
	set[DCMPL          ] = Instruction.DCMPL.singleton;
	set[DCONST_0       ] = Instruction.DCONST_0.singleton;
	set[DCONST_1       ] = Instruction.DCONST_1.singleton;
	set[DDIV           ] = Instruction.DDIV.singleton;
	set[DRETURN        ] = Instruction.DRETURN.singleton;
	set[DLOAD          ] = Instruction.DLOAD.singleton;
	set[DLOAD_0        ] = Instruction.DLOAD_0.singleton;
	set[DLOAD_1        ] = Instruction.DLOAD_1.singleton;
	set[DLOAD_2        ] = Instruction.DLOAD_2.singleton;
	set[DLOAD_3        ] = Instruction.DLOAD_3.singleton;
	set[DMUL           ] = Instruction.DMUL.singleton;
	set[DNEG           ] = Instruction.DNEG.singleton;
	set[DREM           ] = Instruction.DREM.singleton;
	set[DSTORE         ] = Instruction.DSTORE.singleton;
	set[DSTORE_0       ] = Instruction.DSTORE_0.singleton;
	set[DSTORE_1       ] = Instruction.DSTORE_1.singleton;
	set[DSTORE_2       ] = Instruction.DSTORE_2.singleton;
	set[DSTORE_3       ] = Instruction.DSTORE_3.singleton;
	set[DSUB           ] = Instruction.DSUB.singleton;
	set[DUP            ] = Instruction.DUP.singleton;
	set[DUP_X1         ] = Instruction.DUP_X1.singleton;
	set[DUP_X2         ] = Instruction.DUP_X2.singleton;
	set[DUP2           ] = Instruction.DUP2.singleton;
	set[DUP2_X1        ] = Instruction.DUP2_X1.singleton;
	set[DUP2_X2        ] = Instruction.DUP2_X2.singleton;
	set[F2D            ] = Instruction.F2D.singleton;
	set[F2I            ] = Instruction.F2I.singleton;
	set[F2L            ] = Instruction.F2L.singleton;
	set[FADD           ] = Instruction.FADD.singleton;
	set[FALOAD         ] = Instruction.FALOAD.singleton;
	set[FASTORE        ] = Instruction.FASTORE.singleton;
	set[FCMPG          ] = Instruction.FCMPG.singleton;
	set[FCMPL          ] = Instruction.FCMPL.singleton;
	set[FCONST_0       ] = Instruction.FCONST_0.singleton;
	set[FCONST_1       ] = Instruction.FCONST_1.singleton;
	set[FCONST_2       ] = Instruction.FCONST_2.singleton;
	set[FDIV           ] = Instruction.FDIV.singleton;
	set[FLOAD          ] = Instruction.FLOAD.singleton;
	set[FLOAD_0        ] = Instruction.FLOAD_0.singleton;
	set[FLOAD_1        ] = Instruction.FLOAD_1.singleton;
	set[FLOAD_2        ] = Instruction.FLOAD_2.singleton;
	set[FLOAD_3        ] = Instruction.FLOAD_3.singleton;
	set[FMUL           ] = Instruction.FMUL.singleton;
	set[FNEG           ] = Instruction.FNEG.singleton;
	set[FREM           ] = Instruction.FREM.singleton;
	set[FRETURN        ] = Instruction.FRETURN.singleton;
	set[FSTORE         ] = Instruction.FSTORE.singleton;
	set[FSTORE_0       ] = Instruction.FSTORE_0.singleton;
	set[FSTORE_1       ] = Instruction.FSTORE_1.singleton;
	set[FSTORE_2       ] = Instruction.FSTORE_2.singleton;
	set[FSTORE_3       ] = Instruction.FSTORE_3.singleton;
	set[FSUB           ] = Instruction.FSUB.singleton;
	set[GETFIELD       ] = Instruction.GETFIELD.singleton;
	set[GETSTATIC      ] = Instruction.GETSTATIC.singleton;
	set[GOTO           ] = Instruction.GOTO.singleton;
	set[GOTO_W         ] = Instruction.GOTO_W.singleton;
	set[I2B            ] = Instruction.I2B.singleton;
	set[I2C            ] = Instruction.I2C.singleton;
	set[I2D            ] = Instruction.I2D.singleton;
	set[I2F            ] = Instruction.I2F.singleton;
	set[I2L            ] = Instruction.I2L.singleton;
	set[I2S            ] = Instruction.I2S.singleton;
	set[IADD           ] = Instruction.IADD.singleton;
	set[IALOAD         ] = Instruction.IALOAD.singleton;
	set[IAND           ] = Instruction.IAND.singleton;
	set[IASTORE        ] = Instruction.IASTORE.singleton;
	set[ICONST_M1      ] = Instruction.ICONST_M1.singleton;
	set[ICONST_0       ] = Instruction.ICONST_0.singleton;
	set[ICONST_1       ] = Instruction.ICONST_1.singleton;
	set[ICONST_2       ] = Instruction.ICONST_2.singleton;
	set[ICONST_3       ] = Instruction.ICONST_3.singleton;
	set[ICONST_4       ] = Instruction.ICONST_4.singleton;
	set[ICONST_5       ] = Instruction.ICONST_5.singleton;
	set[IDIV           ] = Instruction.IDIV.singleton;
	set[IF_ACMPEQ      ] = Instruction.IF_ACMPEQ.singleton;
	set[IF_ACMPNE      ] = Instruction.IF_ACMPNE.singleton;
	set[IF_ICMPEQ      ] = Instruction.IF_ICMPEQ.singleton;
	set[IF_ICMPGE      ] = Instruction.IF_ICMPGE.singleton;
	set[IF_ICMPGT      ] = Instruction.IF_ICMPGT.singleton;
	set[IF_ICMPLE      ] = Instruction.IF_ICMPLE.singleton;
	set[IF_ICMPLT      ] = Instruction.IF_ICMPLT.singleton;
	set[IF_ICMPNE      ] = Instruction.IF_ICMPNE.singleton;
	set[IFEQ           ] = Instruction.IFEQ.singleton;
	set[IFGE           ] = Instruction.IFGE.singleton;
	set[IFGT           ] = Instruction.IFGT.singleton;
	set[IFLE           ] = Instruction.IFLE.singleton;
	set[IFLT           ] = Instruction.IFLT.singleton;
	set[IFNE           ] = Instruction.IFNE.singleton;
	set[IFNONNULL      ] = Instruction.IFNONNULL.singleton;
	set[IFNULL         ] = Instruction.IFNULL.singleton;
	set[IINC           ] = Instruction.IINC.singleton;
	set[ILOAD          ] = Instruction.ILOAD.singleton;
	set[ILOAD_0        ] = Instruction.ILOAD_0.singleton;
	set[ILOAD_1        ] = Instruction.ILOAD_1.singleton;
	set[ILOAD_2        ] = Instruction.ILOAD_2.singleton;
	set[ILOAD_3        ] = Instruction.ILOAD_3.singleton;
	set[IMUL           ] = Instruction.IMUL.singleton;
	set[INEG           ] = Instruction.INEG.singleton;
	set[INSTANCEOF     ] = Instruction.INSTANCEOF.singleton;
	set[INVOKEINTERFACE] = Instruction.INVOKEINTERFACE.singleton;
	set[INVOKESPECIAL  ] = Instruction.INVOKESPECIAL.singleton;
	set[INVOKESTATIC   ] = Instruction.INVOKESTATIC.singleton;
	set[INVOKEVIRTUAL  ] = Instruction.INVOKEVIRTUAL.singleton;
	set[IOR            ] = Instruction.IOR.singleton;
	set[IREM           ] = Instruction.IREM.singleton;
	set[IRETURN        ] = Instruction.IRETURN.singleton;
	set[ISHL           ] = Instruction.ISHL.singleton;
	set[ISHR           ] = Instruction.ISHR.singleton;
	set[ISTORE         ] = Instruction.ISTORE.singleton;
	set[ISTORE_0       ] = Instruction.ISTORE_0.singleton;
	set[ISTORE_1       ] = Instruction.ISTORE_1.singleton;
	set[ISTORE_2       ] = Instruction.ISTORE_2.singleton;
	set[ISTORE_3       ] = Instruction.ISTORE_3.singleton;
	set[ISUB           ] = Instruction.ISUB.singleton;
	set[IUSHR          ] = Instruction.IUSHR.singleton;
	set[IXOR           ] = Instruction.IXOR.singleton;
	set[JSR            ] = Instruction.JSR.singleton;
	set[JSR_W          ] = Instruction.JSR_W.singleton;
	set[L2D            ] = Instruction.L2D.singleton;
	set[L2F            ] = Instruction.L2F.singleton;
	set[L2I            ] = Instruction.L2I.singleton;
	set[LADD           ] = Instruction.LADD.singleton;
	set[LALOAD         ] = Instruction.LALOAD.singleton;
	set[LAND           ] = Instruction.LAND.singleton;
	set[LASTORE        ] = Instruction.LASTORE.singleton;
	set[LCMP           ] = Instruction.LCMP.singleton;
	set[LCONST_0       ] = Instruction.LCONST_0.singleton;
	set[LCONST_1       ] = Instruction.LCONST_1.singleton;
	set[LDC            ] = Instruction.LDC.singleton;
	set[LDC_W          ] = Instruction.LDC_W.singleton;
	set[LDC2_W         ] = Instruction.LDC2_W.singleton;
	set[LDIV           ] = Instruction.LDIV.singleton;
	set[LLOAD          ] = Instruction.LLOAD.singleton;
	set[LLOAD_0        ] = Instruction.LLOAD_0.singleton;
	set[LLOAD_1        ] = Instruction.LLOAD_1.singleton;
	set[LLOAD_2        ] = Instruction.LLOAD_2.singleton;
	set[LLOAD_3        ] = Instruction.LLOAD_3.singleton;
	set[LMUL           ] = Instruction.LMUL.singleton;
	set[LNEG           ] = Instruction.LNEG.singleton;
	set[LOOKUPSWITCH   ] = Instruction.LOOKUPSWITCH.singleton;
	set[LOR            ] = Instruction.LOR.singleton;
	set[LREM           ] = Instruction.LREM.singleton;
	set[LRETURN        ] = Instruction.LRETURN.singleton;
	set[LSHL           ] = Instruction.LSHL.singleton;
	set[LSHR           ] = Instruction.LSHR.singleton;
	set[LSTORE         ] = Instruction.LSTORE.singleton;
	set[LSTORE_0       ] = Instruction.LSTORE_0.singleton;
	set[LSTORE_1       ] = Instruction.LSTORE_1.singleton;
	set[LSTORE_2       ] = Instruction.LSTORE_2.singleton;
	set[LSTORE_3       ] = Instruction.LSTORE_3.singleton;
	set[LSUB           ] = Instruction.LSUB.singleton;
	set[LUSHR          ] = Instruction.LUSHR.singleton;
	set[LXOR           ] = Instruction.LXOR.singleton;
	set[MONITORENTER   ] = Instruction.MONITORENTER.singleton;
	set[MONITOREXIT    ] = Instruction.MONITOREXIT.singleton;
	set[MULTIANEWARRAY ] = Instruction.MULTIANEWARRAY.singleton;
	set[NEW            ] = Instruction.NEW.singleton;
	set[NEWARRAY       ] = Instruction.NEWARRAY.singleton;
	set[NOP            ] = Instruction.NOP.singleton;
	set[POP            ] = Instruction.POP.singleton;
	set[POP2           ] = Instruction.POP2.singleton;
	set[PUTFIELD       ] = Instruction.PUTFIELD.singleton;
	set[PUTSTATIC      ] = Instruction.PUTSTATIC.singleton;
	set[RET            ] = Instruction.RET.singleton;
	set[RETURN         ] = Instruction.RETURN.singleton;
	set[SALOAD         ] = Instruction.SALOAD.singleton;
	set[SASTORE        ] = Instruction.SASTORE.singleton;
	set[SIPUSH         ] = Instruction.SIPUSH.singleton;
	set[SWAP           ] = Instruction.SWAP.singleton;
	set[TABLESWITCH    ] = Instruction.TABLESWITCH.singleton;
	set[WIDE           ] = Instruction.WIDE.singleton;

	// quick opcodes
	set[LDC_INT_QUICK            ] = Instruction.LDC_INT_QUICK.singleton;
	set[LDC_FLOAT_QUICK          ] = Instruction.LDC_FLOAT_QUICK.singleton;
	set[LDC_LONG_QUICK           ] = Instruction.LDC_LONG_QUICK.singleton;
	set[LDC_DOUBLE_QUICK         ] = Instruction.LDC_DOUBLE_QUICK.singleton;
	set[LDC_REF_QUICK            ] = Instruction.LDC_REF_QUICK.singleton;
	set[LDC_W_REF_QUICK          ] = Instruction.LDC_W_REF_QUICK.singleton;

	set[AFIAT                    ] = Instruction.AFIAT.singleton;
	set[IFIAT                    ] = Instruction.IFIAT.singleton;
	set[FFIAT                    ] = Instruction.FFIAT.singleton;
	set[LFIAT                    ] = Instruction.LFIAT.singleton;
	set[DFIAT                    ] = Instruction.DFIAT.singleton;

	set[SINGLEANEWARRAY          ] = Instruction.SINGLEANEWARRAY.singleton;

	set[LOAD_SHST_FIELD          ] = Instruction.LOAD_SHST_FIELD.singleton;
	set[LOAD_SHST_METHOD         ] = Instruction.LOAD_SHST_METHOD.singleton;
	set[LOAD_SHST_FIELD_QUICK    ] = Instruction.LOAD_SHST_FIELD_QUICK.singleton;
	set[LOAD_SHST_METHOD_QUICK   ] = Instruction.LOAD_SHST_METHOD_QUICK.singleton;

	set[ANEWARRAY_QUICK          ] = Instruction.ANEWARRAY_QUICK.singleton;
	set[CHECKCAST_QUICK          ] = Instruction.CHECKCAST_QUICK.singleton;
	set[INSTANCEOF_QUICK         ] = Instruction.INSTANCEOF_QUICK.singleton;
	set[GETFIELD_QUICK           ] = Instruction.GETFIELD_QUICK.singleton;
	set[GETFIELD2_QUICK          ] = Instruction.GETFIELD2_QUICK.singleton;
	set[INVOKENONVIRTUAL_QUICK   ] = Instruction.INVOKENONVIRTUAL_QUICK.singleton;
	set[INVOKESUPER_QUICK        ] = Instruction.INVOKESUPER_QUICK.singleton;
	set[INVOKENONVIRTUAL2_QUICK  ] = Instruction.INVOKENONVIRTUAL2_QUICK.singleton;
	set[INVOKEINTERFACE_QUICK    ] = Instruction.INVOKEINTERFACE_QUICK.singleton;
	set[UNCHECKED_AASTORE        ] = Instruction.UNCHECKED_AASTORE.singleton;
	set[NEW_QUICK                ] = Instruction.NEW_QUICK.singleton;
	set[MULTIANEWARRAY_QUICK     ] = Instruction.MULTIANEWARRAY_QUICK.singleton;
	set[INVOKEVIRTUAL_QUICK      ] = Instruction.INVOKEVIRTUAL_QUICK.singleton;
	set[PUTFIELD_QUICK           ] = Instruction.PUTFIELD_QUICK.singleton;
	set[PUTFIELD2_QUICK          ] = Instruction.PUTFIELD2_QUICK.singleton;
	set[PUTFIELD_WITH_BARRIER_REF    ] = Instruction.PUTFIELD_WITH_BARRIER_REF.singleton;
	set[AASTORE_WITH_BARRIER     ] = Instruction.AASTORE_WITH_BARRIER.singleton;
	set[PUTFIELD_QUICK_WITH_BARRIER_REF ] = Instruction.PUTFIELD_QUICK_WITH_BARRIER_REF.singleton;
	set[READ_BARRIER     ] = Instruction.READ_BARRIER.singleton;
	set[CHECKING_TRANSLATING_READ_BARRIER     ] = Instruction.CHECKING_TRANSLATING_READ_BARRIER.singleton;	
        set[NONCHECKING_TRANSLATING_READ_BARRIER     ] = Instruction.NONCHECKING_TRANSLATING_READ_BARRIER.singleton;	
	set[PUTSTATIC_WITH_BARRIER_REF    ] = Instruction.PUTSTATIC_WITH_BARRIER_REF.singleton;
 
	// ovm specific opcodes
	set[INVOKE_SYSTEM            ] = Instruction.INVOKE_SYSTEM.singleton;
	set[INVOKE_NATIVE            ] = Instruction.INVOKE_NATIVE.singleton;
	set[ROLL                     ] = Instruction.ROLL.singleton;
	set[COPY                     ] = Instruction.COPY.singleton;
	set[REF_GETFIELD_QUICK       ] = Instruction.REF_GETFIELD_QUICK.singleton;
        set[POLLCHECK                ] = Instruction.POLLCHECK.singleton;
        set[NULLCHECK                ] = Instruction.NULLCHECK.singleton;
        set[INB			     ] = Instruction.INB.singleton;
        set[OUTB		     ] = Instruction.OUTB.singleton;
        set[INCREMENT_COUNTER        ] = Instruction.INCREMENT_COUNTER.singleton;

	// wide instructions
	set[WIDE_OFFSET + ILOAD      ] = Instruction.WIDE_ILOAD.singleton;
	set[WIDE_OFFSET + LLOAD      ] = Instruction.WIDE_LLOAD.singleton;
	set[WIDE_OFFSET + FLOAD      ] = Instruction.WIDE_FLOAD.singleton;
	set[WIDE_OFFSET + DLOAD      ] = Instruction.WIDE_DLOAD.singleton;
	set[WIDE_OFFSET + ALOAD      ] = Instruction.WIDE_ALOAD.singleton;
	set[WIDE_OFFSET + ISTORE     ] = Instruction.WIDE_ISTORE.singleton;
	set[WIDE_OFFSET + LSTORE     ] = Instruction.WIDE_LSTORE.singleton;
	set[WIDE_OFFSET + FSTORE     ] = Instruction.WIDE_FSTORE.singleton;
	set[WIDE_OFFSET + DSTORE     ] = Instruction.WIDE_DSTORE.singleton;
	set[WIDE_OFFSET + ASTORE     ] = Instruction.WIDE_ASTORE.singleton;
	set[WIDE_OFFSET + IINC       ] = Instruction.WIDE_IINC.singleton;
	set[WIDE_OFFSET + RET        ] = Instruction.WIDE_RET.singleton;

	// fill the instruction set with unimplemented instructions
	for (int i = 0; i<set.length; i++) 
	    if (set[i] == null) 
		set[i] = new Instruction.UNIMPLEMENTED(i);
    } 

    public Instruction[] getInstructions() {
	return set;
    }

    /**
     * Resurrected for reference. Won't run without some tweaking (e.g,
     * the instruction names should be in sync with opcodes but are not).
     **/
    public static void buildReflectively() throws BCbootTime {
	Class insClass = Instruction.class;
	Class[] innerClasses = insClass.getDeclaredClasses();
	for (int i = 0; i < innerClasses.length; i++) {
	    if (insClass.isAssignableFrom(innerClasses[i])
		&& !Modifier.isAbstract(innerClasses[i].getModifiers())) {
		try {
		    Instruction ins = 
			(Instruction) innerClasses[i].newInstance();
		    int opcode = ins.getOpcode();
		    if (opcode < 0) {
			d("can't find " + innerClasses[i] + ". ignoring");
			continue;
		    }
		    if (SINGLETON.set[opcode] != null) {
			throw new Error("number already taken");
		    }
		    SINGLETON.set[opcode] = ins;
		} catch (Exception e) {
		    throw OVMBase.failure(e);
		}
	    }
	}
    }

} // End of InstructionSet.java
