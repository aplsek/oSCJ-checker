package ovm.services.bytecode;


import ovm.core.OVMBase;
import ovm.core.domain.Blueprint;
import ovm.core.domain.Domain;
import ovm.core.domain.LinkageException;
import ovm.core.domain.ResolvedConstant;
import ovm.core.domain.Type;
import ovm.core.repository.Descriptor;
import ovm.core.repository.Selector;
import ovm.core.repository.TypeName;
import ovm.core.repository.ConstantPool;
import ovm.core.repository.JavaNames;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.ConstantFieldref;
import ovm.core.repository.ConstantMethodref;
import ovm.core.repository.ConstantClass;
import ovm.core.repository.Constants;
import ovm.core.services.io.BasicIO;
import ovm.services.bytecode.SpecificationIR.*;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import s3.util.PragmaTransformCallsiteIR.BCdead;
import ovm.util.ByteBuffer;
import ovm.util.NumberRanges;
import ovm.core.services.memory.MemoryManager;

/**
 * Defines the generic interface of bytecode intructions. The generic
 * interface is further extended by a number of nested interfaces. The
 * naming scheme used here is <b>all-caps</b> names stand for JVM
 * instructions, and <b>mixed caps</b> for instruction categories.<p>
 *
 * There is an ongoing effort to make the instruction hierarchy
 * compatible with the hierarchy used in
 * <a href="http://jakarta.apache.org/bcel">BCEL</a>, and support
 * BCEL-style bytecode editing.  100% compatibility will probably
 * never be achieved for a number of reasons.
 * <ul>
 * <li> Ovm supports instructions that BCEL, and Java, does not.
 * <li> Ovm uses instruction definitions to derive an interpreter as
 *      well as directly executable C++ code.
 * <li> Ovm's analysis framework allows both linked
 *      {@link InstructionList}s and <code>byte[]</code>-based
 *      {@link InstructionBuffer}s to be examined.
 * <li> Ovm adopts a more general visitor strategy
 * </ul>
 *
 * <code>Instruction</code> is the most fundemental class in Ovm.  The
 * correctness of the system as a whole depends on a correct
 * definition of each instruction's effect on the stack, a correct
 * definition of each instruction's exception-throwing behavior, and a
 * correct definition of each method in
 * {@link ovm.services.bytecode.Instruction.Visitor}.
 *
 * @see ovm.services.bytecode.Instruction.IVisitor
 * @see InstructionVisitor
 * @see MethodInformation
 **/
public abstract class Instruction extends OVMBase
    implements JVMConstants.Throwables, Cloneable
{
    // If true, skip all bounds checks
    static final boolean NO_BOUND_CHECK = false;

    private static final Value[] nv = new Value[0];
    private static final Temp[] nt = new Temp[0];
    private static final StreamableValue[] niv = new StreamableValue[0];
    
    // In both stackIns and stackOuts 0 is the top of the stack, hence
    // stackIn[0] is the first thing popped, and stackOut[0] is the
    // last thing pushed
    public StreamableValue[] istreamIns = niv;
    public Value[] stackIns = nv;
    public Temp[] temps = nt;
    public Value[] evals = nv;
    public Value[] stackOuts = nv;

    protected final int opcode_; // from Opcodes*

    public Instruction(int opcode) {
	this.opcode_ = opcode;
    }

    public boolean isPrototype() {
	return this == InstructionSet.SINGLETON.set[getOpcode()];
    }

    public Object clone() {
	try {
	    return super.clone();
	} catch (CloneNotSupportedException e) {
	    throw new Error("is so supported!"/*, e*/);
	}
    }
	    
    /**
     * A factory method for non singleton Instruction objects.
     * @param iv
     *            the bytecode stream from which the concrete parameters are
     *            decoded
     * @return a clone of this Instruction with concrete in-stream parameter
     *         values, or this if this has no in-stream parameters.
     */
    public Instruction concretize(MethodInformation iv) {
        if (istreamIns.length == 0)
            return this;
        if (!isPrototype())
            throw new Error("Trying to concretize concrete Instruction " + this); 
        Instruction clone = (Instruction) clone();
        clone.istreamIns = new StreamableValue[this.istreamIns.length];
        int parameterOffset = 1;
        if (this instanceof Switch) {
            throw new Error("Switch should override this method.");
        } else if (this instanceof WIDE) {
            throw new Error("WIDE should override this method.");
        } else if ((getOpcode() & JVMConstants.Opcodes.WIDE_OFFSET) != 0) {
            parameterOffset = 2;
        }
        int streamOffset = parameterOffset; 
        try {
            for (int i = 0; i < this.istreamIns.length; i++) {
                clone.istreamIns[i] = istreamIns[i].concretize(iv, streamOffset);
                streamOffset += istreamIns[i].bytestreamSize();
            }
        } catch (Exception e) {
            BasicIO.err.println("An error in dealing with " + clone.toString());
            throw new Error();
        }
        return clone;
    }
    
    /**
     * Compute the size of the concrete Instruction.
     * @param position
     *            the bytecode position where the instruction starts
     * @return the size
     */
    public int size(int position) {
        int size = 1;
        if (this instanceof Switch) {
            throw new Error("Switch should override this method.");
        } else if (this instanceof WIDE) {
            throw new Error("WIDE should not appear here because of the specialized Instructions");
        } else if ((getOpcode() & JVMConstants.Opcodes.WIDE_OFFSET) != 0) {
            size++;
        }
        try {
            for (int i = 0; i < this.istreamIns.length; i++) {
                // An InstructionHandle itself does not know its size 
                // (when encoded as a branch offset) because 
                // the size depends on the Instruction pointing to it.  
                size += istreamIns[i] instanceof InstructionHandle
                        ? ((InstructionHandle)istreamIns[i]).bytestreamSize((BranchInstruction)this)
                        : istreamIns[i].bytestreamSize();
            }
        } catch (Exception e) {
            BasicIO.err.println("An error in dealing with " + toString());
            throw new Error();
        }
        return size;
    }
    
    /**
     * @return true if the local offset or the branch offset in 'widenable'
     *         concrete Instructions (LocalRead, LocalWrite, IINC, RET, GOTO, JSR) does
     *         not fit in the slot, otherwise false.
     */
    public boolean doesNeedWidening() {
        return false;
    }

    /**
     * @return the widened version of this concrete Instruction. 
     * 
     * Only valid for non-wide LocalRead, non-wide LocalWrite, IINC, RET, GOTO, JSR.
     */
    public Instruction widen() {
        throw new Error("Can't widen " + toString());
    }
    
    /**
     * Encode the concrete Instruction into the byte buffer.
     * @param code
     */
    public void encode(ByteBuffer code) {
        int startOfInstruction = code.position();
        encodeOpcode(code);
        encodeIstreamIns(code, startOfInstruction);
    }

    protected void encodeOpcode(ByteBuffer code) {
        if ((opcode_ & JVMConstants.Opcodes.WIDE_OFFSET) == 0)
            code.put(NumberRanges.checkUnsignedByte(opcode_));
        else {
            code.put((byte)JVMConstants.Opcodes.WIDE);
            code.put((byte)(opcode_ & 0xFF));
        }
    }
    
    protected void encodeIstreamIns(ByteBuffer code, int startOfInstruction) {
        for(int i = 0; i < istreamIns.length; i++) {
            StreamableValue in = istreamIns[i];
            if (in instanceof InstructionHandle) {
                if (this instanceof BranchInstruction) {
                    if (((BranchInstruction)this).isBranchOffsetShort()) {
                        ((InstructionHandle)in).encode(code, TypeCodes.SHORT, startOfInstruction);
                    } else {
                        ((InstructionHandle)in).encode(code, TypeCodes.INT, startOfInstruction);
                    }
                } else
                    throw new Error();
            } else if (in instanceof ConcretePCValue) {
                ((ConcretePCValue)in).encode(code, startOfInstruction);
            } else if (in instanceof ConcretePCValueList) {
                ((ConcretePCValueList)in).encode(code, startOfInstruction);
            } else if (in instanceof ConcreteIntPCValuePairList) {
                ((ConcreteIntPCValuePairList)in).encode(code, startOfInstruction);
            } else if (in instanceof ConcreteStreamableValue) {
                ((ConcreteStreamableValue)in).encode(code);
            } else {
                throw new Error("Not concrete");
            }
        }
    }

    private int known_size = -1;
    /**
     * Get the size of the instruction
     **/
    public int size(MethodInformation _) {
	if (known_size == -1) {
	    int ret = ((opcode_ & JVMConstants.Opcodes.WIDE_OFFSET) == 0 ? 1 : 2);
	    for (int i = 0; i < istreamIns.length; i++)
		ret += istreamIns[i].bytestreamSize();
	    known_size = ret;
	}
	return known_size;
    }

    /**
     * Get the numeric value of the opcode (must be >= 0).
     **/
    public int getOpcode() {
	return opcode_;
    }

    public abstract void accept(Instruction.Visitor v);	

    protected final boolean isExtendedSetSpec() {
	return getName().startsWith("WIDE_");
    }

    public String getName() /*throws BCdead*/ {
        String n = getClass().getName();
        return n.substring(n.lastIndexOf('$') + 1);
    }
    
    public String toString() {
        return getName();
    }

    public String toString(MethodInformation _) {
	return this.toString();
    }

    public String toString(MethodInformation mi, Constants _) {
	return this.toString(mi);
    }

    public String toString(Constants cp) {
        return toString();
    }
    
    static abstract class ValueFactory {
	abstract Value make(ValueSource s);
	Value make() { return make(null); }
    }
    static ValueFactory cvtFactory(final ValueFactory f1)
    {
	return new ValueFactory() {
		Value make(ValueSource s) {
		    return new IntValue(new ConversionExp(f1.make(s)));
		}
	    };
    }
    static ValueFactory subwordFactory(final char code)
    {
	return new ValueFactory() {
		Value make(ValueSource s) {
		    return new IntValue(code, s);
		}
	    };
    }

    static final ValueFactory refFactory = new ValueFactory() {
	    Value make(ValueSource s) {
		return new RefValue(s);
	    }
	};
    static final ValueFactory intFactory = new ValueFactory() {
	    Value make(ValueSource s) {
		return new IntValue(s);
	    }
	};
    static final ValueFactory uintFactory = subwordFactory(TypeCodes.UINT);
    static final ValueFactory shortFactory = subwordFactory(TypeCodes.SHORT);
    static final ValueFactory cvtShortFactory = cvtFactory(shortFactory);
    static final ValueFactory charFactory = subwordFactory(TypeCodes.CHAR);
    static final ValueFactory cvtCharFactory = cvtFactory(charFactory);
    static final ValueFactory byteFactory = subwordFactory(TypeCodes.BYTE);
    static final ValueFactory cvtByteFactory = cvtFactory(byteFactory);

    static final ValueFactory longFactory = new ValueFactory() {
	    Value make(ValueSource s) {
		return new LongValue(s);
	    }
	};
    static final ValueFactory ulongFactory = new ValueFactory() {
	    Value make(ValueSource s) {
		return new LongValue(TypeCodes.ULONG, s);
	    }
	};
    static final ValueFactory floatFactory = new ValueFactory() {
	    Value make(ValueSource s) {
		return new FloatValue(s);
	    }
	};
    static final ValueFactory doubleFactory = new ValueFactory() {
	    Value make(ValueSource s) {
		return new DoubleValue(s);
	    }
	};

    /* ********************* Interfaces ************************** */


    // first: top-level interfaces that extend Instruction

    /**
     * Instructions that may throw exceptions.<p>
     *
     * {@link #getThrowables} returns a list exceptions that may
     * consist soley of <code>java.lang.Throwable</code>, or may
     * include <code>java.lang.Error</code> and zero or more subtypes
     * of <code>java.lang.RuntimeException</code>.
     **/
    public interface ExceptionThrower {
	/**
	 * Which types of exceptions may be thrown?
	 * May list common supertype(s) if exact
	 * types are not known.
	 **/
	public TypeName.Scalar[] getThrowables(); 

    }

    static abstract class ExceptionThrowerImpl extends Instruction
	implements ExceptionThrower
    {
	TypeName.Scalar[] ex;
	
	ExceptionThrowerImpl(int opcode, TypeName.Scalar[] ex) {
	    super(opcode);
	    this.ex = ex;
	}
	// Map exception codes defined in Throwables to TypeNames.
	// This is the preferred constructor, since it ensures that
	// exceptions thrown directly by CSA calls are listed in the
	// set of exceptions that can be implicitly new'ed.
	ExceptionThrowerImpl(int opcode, int[] exCode) {
	    this(opcode, mayThrow(exCode));
	}
	public TypeName.Scalar[] getThrowables() { return ex; }
    }

    /* ******************** Abstract classes ********************* */

    /* Classes on this page represent the new decompostion into
     * polymorphic logical instructions.  Classes in the next page
     * represent the old decomposition that emphasized concrete
     * instruction layout over types and types over operations.  I
     * take the opposite approach, which seems better for the library
     * client.
     */

    /**
     * Instructions that access local variables.
     **/
    public static abstract class LocalAccess extends Instruction {
	IntValue offset;

	LocalAccess(int opcode) { super(opcode); }

	LocalAccess(int opcode, IntValue offset) {
	    super(opcode);
	    this.offset = offset;

	    // offset is an immediate -- either embedded in the opcode
	    // byte or in some following bytes
	    if (!(offset instanceof ConcreteIntValue))
		istreamIns = imm(offset);
	}

    public abstract char getTypeCode();
    
	public int getLocalVariableOffset(MethodInformation iv)	{
	    if (offset instanceof ConcreteIntValue)
		return offset.intValue();
	    else {
		// offset of offset
		int offoff =
		    ((getOpcode() & JVMConstants.Opcodes.WIDE_OFFSET) == 0
		     ? 1
		     : 2);
		return offset.decodeInt(iv, offoff);
	    }
	}

    // The version for concrete Instruction
    public int getLocalVariableOffset() {
        if (offset instanceof ConcreteIntValue)
            return offset.intValue();
        else if (istreamIns[0] instanceof ConcreteIntValue) // concrete
            return ((ConcreteIntValue)istreamIns[0]).intValue();
        else // abstract
            throw new Error("Not concrete");
    }

    public void setLocalVariableOffset(int index) {
        if (offset instanceof ConcreteIntValue)
            throw new Error("Tried to modify the index in " + getName() + ". Instead try to make a new Instruction object with a new index");
        else if (istreamIns[0] instanceof ConcreteIntValue)
            istreamIns[0] = new ConcreteIntValue(index, TypeCodes.UBYTE);
        else
            throw new Error("Not concrete");
    }
    
    public boolean doesNeedWidening() {
        if (offset instanceof ConcreteIntValue) { // ALOAD_0, etc
            int localIndex = offset.intValue();
            switch(localIndex) {
            case 0:
            case 1:
            case 2:
            case 3:
                return false;
            default:
                return true;
            }
        } else { // ALOAD, etc
            int localIndex = getLocalVariableOffset();
            return ! NumberRanges.isUnsignedByte(localIndex);
        }
    }
    }

    public static abstract class LocalRead extends LocalAccess {
        public static LocalRead make(char typeCode, int localIndex) {
            switch(typeCode) {
            case TypeCodes.REFERENCE:
                return ALOAD.make(localIndex);
            case TypeCodes.INT:
                return ILOAD.make(localIndex);
            case TypeCodes.FLOAT:
                return FLOAD.make(localIndex);
            case TypeCodes.LONG:
                return LLOAD.make(localIndex);
            case TypeCodes.DOUBLE:
                return DLOAD.make(localIndex);
            default:
                throw new Error();
            }
        }
	LocalRead(int opcode, IntValue offset, ValueFactory f) {
	    super(opcode, offset);
	    stackOuts = stack(f.make(new LocalExp(offset)));
	}
    public char getTypeCode() {
        return SpecificationIR.toTypeCode(stackOuts[0]);
    }
	public String toString(MethodInformation iv) {
	    return getName() + " " + getLocalVariableOffset(iv);
	}

	public String toString() {
	    return (isPrototype()
		    ? getName()
		    : (getName() + " " + getLocalVariableOffset()));
	}
    }

    public static abstract class LocalWrite extends LocalAccess {
        public static LocalWrite make(char typeCode, int localIndex) {
            switch(typeCode) {
            case TypeCodes.REFERENCE:
                return ASTORE.make(localIndex);
            case TypeCodes.INT:
                return ISTORE.make(localIndex);
            case TypeCodes.FLOAT:
                return FSTORE.make(localIndex);
            case TypeCodes.LONG:
                return LSTORE.make(localIndex);
            case TypeCodes.DOUBLE:
                return DSTORE.make(localIndex);
            default:
                throw new Error();
            }
        }

	LocalWrite(int opcode, IntValue offset, ValueFactory f) {
	    super(opcode, offset);
	    stackIns = stack(f.make());
	    evals = eval(new LocalStore(offset, stackIns[0]));
	}
    public char getTypeCode() {
        return SpecificationIR.toTypeCode(stackIns[0]);
    }

	public String toString(MethodInformation iv) {
	    return getName() + " " + getLocalVariableOffset(iv);
	}

	public String toString() {
	    return (isPrototype()
		    ? getName()
		    : (getName() + " " + getLocalVariableOffset()));
	}
    }

    /**
     * Instructions that read or write arrays extend this class. 
     * It declares the exceptions "NullPointerException" and
     * "ArrayIndexOutOfBoundsException" as possible  events.
     **/
     public static abstract class ArrayAccess extends ExceptionThrowerImpl {
	public ArrayAccess(int opcode) {
	    super(opcode, new int[] {
		    NULL_POINTER_EXCEPTION,
		    ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION,
		    ERROR
		});
	}


	ValueSource boundCheck(NonnulArrayRefValue arr, IntValue idx) {
	    CondExp c = new CondExp(new IntValue(TypeCodes.UINT,
						 new ConversionExp(idx)),
				    ">=",
				    new IntValue(TypeCodes.UINT,
                                                 new ConversionExp(new IntValue(new ArrayLengthExp(arr)))));
                                                 

              
	    IfCmd ic =  new IfCmd(new Value(c),
			     throwException(ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION,
					    idx));
            return new CallExp( "CHECK_BOUNDS_IF_ENABLED", new Value(ic) );
	}

/*
	ValueSource boundCheck(NonnulArrayRefValue arr, IntValue idx, IntValue barrierReturn) {
	    CondExp c = new CondExp( barrierReturn,
				    "!=",
				     ConcreteIntValue.ZERO);
	    return new IfCmd(new Value(c),
			     throwException(ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION,
					    idx));
	}	*/
    }

    public static abstract class ArrayLoad extends ArrayAccess {
	public ArrayLoad(int opcode, ValueFactory fact, String barrier) {
	    super(opcode);
	    NonnulArrayRefValue arr = new NonnulArrayRefValue();
	    IntValue idx = new IntValue();
	    stackIns = stack(idx, arr);
	    if (!NO_BOUND_CHECK)
		evals = eval(boundCheck(arr, idx)); 
	
	        // the null barrier is just for debugging, feel free to kill it	
            if (MemoryManager.the().needsArrayAccessBarriers() && barrier!=null) { 
              this.stackOuts = stack(fact.make( 
                new CSACallExp(barrier,
                      arr,
                      idx
              )));            
            } else {
  	      this.stackOuts = stack(fact.make(new ArrayAccessExp(arr, idx)));
            }
	}
    }

    public static abstract class ArrayStore extends ArrayAccess {
	public ArrayStore(int opcode, 
			  ValueFactory stackType,
			  ValueFactory heapType,
			  String barrier)
	{
	    super(opcode);
	    NonnulArrayRefValue arr = new NonnulArrayRefValue();
	    IntValue idx = new IntValue();
	    stackIns = stack(stackType.make(), idx, arr);            	    

	    if (MemoryManager.the().needsArrayAccessBarriers()) {
              evals = eval(new CSACallExp(barrier, arr, idx, stackIns[0]));	    
	    } else {
  	      evals = eval(new AssignmentExp(heapType.make(new ArrayAccessExp(arr, idx, true)),
					   stackIns[0]));
            }
            
	    if (!NO_BOUND_CHECK)
		pushEval(boundCheck(arr, idx));
	    if (this instanceof AASTORE) {
		pushEval(new IfCmd
			 (new IntValue(new CallExp("ARRAY_STORE_INVALID",
						   arr, stackIns[0])),
			  throwException(ARRAY_STORE_EXCEPTION)));
	    }
	}
    }

    public static abstract class UncheckedArrayStore extends ArrayAccess {
    	public UncheckedArrayStore(int opcode) {
    	  super(opcode);
    	}
    	
	public UncheckedArrayStore(int opcode, ValueFactory stackType, ValueFactory heapType) {
	    super(opcode);
	    RefValue arr = new RefValue();
	    IntValue idx = new IntValue();
	    stackIns = stack(stackType.make(), idx, arr);
	    evals = eval(new AssignmentExp(heapType.make(
		    new ArrayAccessExp(arr, idx, true)), stackIns[0]));
	}
    }

    public abstract static class FieldAccess_Quick extends ExceptionThrowerImpl
    {
	FieldAccess_Quick(int opcode) {
	    super(opcode, new int[] { NULL_POINTER_EXCEPTION, ERROR });
	    istreamIns = imm(TypeCodes.USHORT);
	}
	public int getOffset(MethodInformation iv) {
	    return istreamIns[0].decodeInt(iv, 1);
	}
	ValueSource refField() {
	    return new MemExp(stackIns[stackIns.length-1], (Value)istreamIns[0]);
	}

	public String toString(MethodInformation iv) {
	    return getName() + " " + getOffset(iv);
	}

	public String toString() {
	    return (isPrototype()
		    ? getName()
		    : (getName() + " " + getOffset(null)));
	}
    }

    public static abstract class FieldGet_Quick extends FieldAccess_Quick {
	FieldGet_Quick(int opcode, ValueFactory fact) {
	    super(opcode);
	    stackIns = stack(new NonnulRefValue());
	    stackOuts = stack(fact.make(refField()));
	}
    }

    public static abstract class FieldPut_Quick extends FieldAccess_Quick {
	FieldPut_Quick(int opcode, ValueFactory fact) {
	    super(opcode);
	    stackIns = stack(fact.make(), new NonnulRefValue());
	    evals = eval(new AssignmentExp(fact.make(refField()), stackIns[0]));
	}
    }

    
    public static abstract class ReturnInstruction extends FlowEnd 
           implements ExceptionThrower {
        protected ReturnInstruction(int opcode) {
            super(opcode);
        }
        TypeName.Scalar[] ex = mayThrow(ILLEGAL_MONITOR_STATE_EXCEPTION,
					ERROR);
        public TypeName.Scalar[] getThrowables() { return ex; }

    }
    
    public static abstract class ReturnValue extends ReturnInstruction {
	ReturnValue(int opcode, ValueFactory fact) {
	    super(opcode);
	    stackIns = stack(fact.make());
	    // FIXME S3Generator uses this spec to push our return
	    // value onto the caller's stack.
	    stackOuts = stack(stackIns[0]);
	}
    }

    public static abstract class ArithmeticInstruction extends Instruction {
        protected ArithmeticInstruction(int opcode) {
            super(opcode);
        }
    }
    
    // AsymetricBinOp not needed at the moment, because shifts get
    // special handling.
    public static abstract class AsymetricBinOp extends ArithmeticInstruction {
	public Value getRhs() { return stackIns[0]; }
	public Value getLhs() {
	    return ((stackIns[0] instanceof WideValue)
		    ? stackIns[2]
		    : stackIns[1]);
	}
	AsymetricBinOp(int opcode){
	    super(opcode);
	}

	AsymetricBinOp(int opcode, String op,
		       ValueFactory lhs, ValueFactory rhs,
		       ValueFactory result)
	{
	    super(opcode);
	    stackIns = stack(rhs.make(), lhs.make());
	    stackOuts = stack(result.make(new BinExp(getLhs(), op, getRhs())));
	}
    }
    public static abstract class BinOp extends AsymetricBinOp {
	BinOp(int opcode, String op, ValueFactory type) {
	    super(opcode, op, type, type, type);
	}
    }
    public static abstract class IntegerDivision extends BinOp
	implements ExceptionThrower
    {
	private final TypeName.Scalar[] exceptions_;

	IntegerDivision(int opcode, String op, ValueFactory type) {
	    super(opcode, op, type);
	    evals = eval
		(new IfCmd(new IntValue(new CondExp(getRhs(), "==",
						    ConcreteIntValue.ZERO)),
			   throwException(ARITHMETIC_EXCEPTION)));
	    exceptions_ = mayThrow(ARITHMETIC_EXCEPTION, ERROR);
	}

	public TypeName.Scalar[] getThrowables() {
	    return exceptions_;
	}
    }
    public static abstract class Negation extends ArithmeticInstruction {
	Negation(int opcode, ValueFactory t) {
	    super(opcode);
	    stackIns = stack(t.make());
	    stackOuts = stack(t.make(new UnaryExp("-", stackIns[0])));
	}
    }
    public abstract static class Conversion extends Instruction {
	public Conversion(int opcode, ValueFactory from, ValueFactory to) {
	    super(opcode);
	    stackIns = stack(from.make());
	    stackOuts = stack(to.make(new ConversionExp(stackIns[0])));
	}
    }

    public static abstract class Synchronization extends ExceptionThrowerImpl
    {
	Synchronization(int opcode, String csa, TypeName.Scalar[] ex) {
	    super(opcode, ex);
	    stackIns = stack(new NonnulRefValue());
	    evals = eval(new CSACallExp(csa, stackIns[0]));
	}
    }

    static final int VIRTUAL = 0;
    static final int NONVIRTUAL = 1;
    static final int INTERFACE = 2;
    static final String[] tables = new String[] {
	    "vTable", "nvTable", "ifTable"
    };

    // We need to know the number of arguments for two reasons:
    // First, to find the `receiver' object, and to pass the count
    // including the receiver to INVOKE.  We also need to look up a
    // blueprint object from either the receiver or the linkset.  This
    // object will be used twice as well.  We can store the blueprint
    // in a temporary, but storing the length expression in a
    // temporary is a bit more complicated.  Duplicated funky
    // arithmetic expressions should be OK -- the compiler should
    // catch them for us.
    public static abstract class Invocation_Quick extends ExceptionThrowerImpl {
	IntValue getField(IntValue source, int shift, int mask) {
	    return new IntValue(new BitFieldExp(source, shift, mask));
	}
	IntValue computeCount(IntValue nCount, IntValue wCount)	{
	    return new IntValue(new BinExp(nCount, "+", wCount));
	}
	RefValue computeStackIn(IntValue count) {
	    RefValue recv = new NonnulRefValue();
	    stackIns = stack(new ValueList(count), recv);
	    return recv;
	}
	RefValue findBlueprint(ValueSource exp) {
	    temps = temp(new Temp("Blueprint *", "bp", new RefValue(exp)));
	    return new RefValue(temps[0]);
	}


	int computeSize() {
	    int count = 1;
	    for (int j = 0; j < istreamIns.length; j++) {
		if (!(istreamIns[j] instanceof IntValueList)) {
		    count += istreamIns[j].bytestreamSize();
		} else {
		    assert(false);
		}
	    }
	    return count;
	}
	void invoke(RefValue recv, RefValue bp, int kind,
		    IntValue methodIndex, IntValue count)
	{
	    Value cf = new Value(new LookupExp(bp, tables[kind], methodIndex));
	    Value cnt = new Value(new BinExp(count, "+",
					     ConcreteIntValue.ONE));
	    ConcreteIntValue instrSize = new ConcreteIntValue(computeSize());
	    evals = eval(new CallExp("INVOKE", recv, cf, bp, cnt, instrSize));
	    // evals = eval(new CallExp("INVOKE", recv, cf, bp, cnt));
	    
	}
	Invocation_Quick(int opcode) {
	    super(opcode, new int[] { THROWABLE });
	}
	public abstract int getMethodTableIndex(MethodInformation iv);

    public abstract int getMethodTableIndex();
    public abstract void setMethodTableIndex(int index);
	/**
	 * @return the argument count
	 **/
	public abstract int getArgumentCount(MethodInformation iv);
	/**
	 * @return the wide argument count
	 **/
	public abstract int getWideArgumentCount(MethodInformation iv);
	/**
	 * @return the argument length in words
	 **/
	public int getArgumentLengthInWords(MethodInformation iv) {
	    return getArgumentCount(iv) + getWideArgumentCount(iv);
	}

    
	public abstract Selector.Method getSelector(MethodInformation iv,
						    Constants cp);

    public abstract int getArgumentCount();
    public abstract int getWideArgumentCount();
    public int getArgumentLengthInWords() {
        return getArgumentCount() + getWideArgumentCount();
    }
    public abstract Selector.Method getSelector(Constants constantPool);
    
    public String toString(MethodInformation iv, Constants cp) {
        Selector.Method sel = getSelector(iv, cp);
        return getName() + " " + sel + " " + getMethodTableIndex(iv);
    }
    public String toString(Constants constantPool) {
        Selector.Method sel = getSelector(constantPool);
        return getName() + " " + sel + " " + getMethodTableIndex();
    }
    }

    /**
     * This instruction puts a value onto the stack. 
     **/
    public abstract static class ConstantLoad extends Instruction {
	public final Object builtinConcreteValue;
	public ConstantLoad(int opcode, boolean hasStreamableValue, StreamableValue v) {
	    super(opcode);
        Value V = (Value)v;        
        if (hasStreamableValue) {
            istreamIns = imm(v);
            builtinConcreteValue = null;
        } else {
            if (V.concreteValue() != null) {
                builtinConcreteValue = V.concreteValue();
            } else
                throw new Error("Does not have a streamable value but a concrete value, either");
        }
		stackOuts = stack(V);
	}
	/**
	 * Return the value of the constant loaded. If the
	 * constant is the NULL pointer, the value is 0.
	 * @return the value as an integer. This may require
	 *          conversion, but no precision should be lost.
	 **/
	public int getValue(MethodInformation iv) {
        if (builtinConcreteValue != null) {
            if (builtinConcreteValue instanceof Number)
                return ((Number)builtinConcreteValue).intValue();
            else if (builtinConcreteValue == NullRefValue.INSTANCE)
                return 0;
            else
                throw new Error("builtinConcreteValue : " + builtinConcreteValue.getClass() + ", "+ builtinConcreteValue.toString());
        } else {
            return istreamIns[0].decodeInt(iv, 1);            
        }
	}
    public int getValue() {
        if (builtinConcreteValue != null) {
            if (builtinConcreteValue instanceof Number)
                return ((Number)builtinConcreteValue).intValue();
            else if (builtinConcreteValue == NullRefValue.INSTANCE)
                return 0;
            else
                throw new Error();
        } else {
            if (istreamIns[0] instanceof ConcreteIntValue)
                return ((ConcreteIntValue)istreamIns[0]).intValue();
            else
                throw new Error("Not concrete");    
        }
    }
    }

    public abstract static class IConstantLoad extends ConstantLoad {
        public static IConstantLoad makeIConstantLoad(int value) {
            switch(value) {
            case -1:
                return ICONST_M1.make();
            case 0:
                return ICONST_0.make();
            case 1:
                return ICONST_1.make();
            case 2:
                return ICONST_2.make();
            case 3:
                return ICONST_3.make();
            case 4:
                return ICONST_4.make();
            case 5:
                return ICONST_5.make();
            default:
                if (NumberRanges.isByte(value))
                    return BIPUSH.make((byte)value);
                else if (NumberRanges.isShort(value))
                    return SIPUSH.make((short)value);
                else
                    return LDC_INT_QUICK.make(value);
            }
        }
	public IConstantLoad(int opcode, boolean hasStreamableValue, IntValue v) {
	    super(opcode, hasStreamableValue, v);
	}
    }

    // FIXME: BCEL does not define DConstantLoad and friends.  Why do we?
    public abstract static class DConstantLoad extends ConstantLoad {
	 public DConstantLoad(int opcode, boolean hasStreamableValue, DoubleValue v) {
	     super(opcode, hasStreamableValue, v);
	 }
	public abstract double getDValue(MethodInformation iv);
	public abstract double getDValue();
    }
    
    public abstract static class FConstantLoad extends ConstantLoad {
	 public FConstantLoad(int opcode, boolean hasStreamableValue, FloatValue v) {
	     super(opcode, hasStreamableValue, v);
	 }
	public abstract float getFValue(MethodInformation iv);
	public abstract float getFValue();
    }
    
    public abstract static class LConstantLoad extends ConstantLoad {
	 public LConstantLoad(int opcode, boolean hasStreamableValue, LongValue v) {
	     super(opcode, hasStreamableValue, v);
	 }
	public abstract long getLValue(MethodInformation iv);
	public abstract long getLValue();
    }
    
    public abstract static class If extends ConditionalJump {
	public If(int opcode, String op, Value v1, Value v2) {
	    super(opcode);
/*	    if (this instanceof IF_ACMPEQ) {
	      controlValue = new IntValue(			
	       new CSACallExp(CSACallExp.Names.acmpeqBarrier, v1, v2)
	      );
	    } else if (this instanceof IF_ACMPNE) {
	      controlValue = new IntValue(			
	       new CSACallExp(CSACallExp.Names.acmpneBarrier, v1, v2)
	      );
	    } else {  */
    	      controlValue = new IntValue(new CondExp(v1, op, v2));
//            }
	}
    public If(int opcode, String op, Value v1, Value v2, int relOffset) {
        super(opcode, relOffset);
        controlValue = new IntValue(new CondExp(v1, op, v2));
    }
    public InstructionHandle getTargetHandle() {
        return (InstructionHandle)istreamIns[0];
    }
    public void setTargetHandle(InstructionHandle ih) {
	((InstructionHandle)istreamIns[0]).removeTargeter(this);
	ih.addTargeter(this);
        istreamIns[0] = ih;
    }
    }
    public abstract static class IfZ extends If {
	public IfZ(int opcode, String op, Value v1, Value v2) {
	    super(opcode, op, v1, v2);
	    stackIns = stack(v1);
	}
    public IfZ(int opcode, String op, Value v1, Value v2, int relOffset) {
        super(opcode, op, v1, v2, relOffset);
        stackIns = stack(v1);
    }
    }
    public abstract static class IfCmp extends If {
	public IfCmp(int opcode, String op, Value v1, Value v2) {
	    super(opcode, op, v1, v2);
	    stackIns = stack(v2, v1);
	}
	public IfCmp(int opcode, String op, ValueFactory f) {
	    this(opcode, op, f.make(), f.make());
	}
    public IfCmp(int opcode, String op, Value v1, Value v2, int relOffset) {
        super(opcode, op, v1, v2, relOffset);
        stackIns = stack(v2, v1);
    }
    public IfCmp(int opcode, String op, ValueFactory f, int relOffset) {
        this(opcode, op, f.make(), f.make(), relOffset);
    }
    }

    /**
     * Instructions that have no effect but to reorder values alreeady
     * on the stack.
     **/
    public abstract static class StackManipulation extends Instruction {
	public StackManipulation(int opcode) { super(opcode); }
    }

    /**
     * Template for instructions that load data from the constant
     * pool and store the index (char) immediately after the opcode.
     **/
    public static abstract class ConstantPoolRead
	extends Instruction {
	public ConstantPoolRead(int opcode) {
	    super(opcode);
	}
	/**
	 * Which index of the Constant Pool is accessed by this instruction?
	 **/
	public int getCPIndex(MethodInformation iv) {
	    int i = istreamIns[0].decodeInt(iv, 1);
	    if (i == 0)
		throw new Error(istreamIns[0].getClass().toString());
	    return istreamIns[0].decodeInt(iv, 1);
	}
	/**
	 * Like <code>getCPIndex(null)<code> this method returns the
	 * index used by a concrete instruction object.
	 * @deprecated
	 **/
	public int getCPIndex() { return getCPIndex(null); }
	public String toString(MethodInformation iv, Constants cp) {
	    Object c = cp.getConstantAt(getCPIndex(iv));
	    return (getName() + "\t" +
		    	    (c instanceof String ? "\"" + c + "\"" : c.toString()));
	}
    public void setCPIndex(int index) {
        if (istreamIns[0] instanceof ConcreteCPIndexValue) {
            istreamIns[0] = new ConcreteCPIndexValue(index);
        } else {
            throw new Error("Tried to set the CP index on an abstract Instruction");
        }
    }
	public String toString(Constants cp) {
	    Object c = cp.getConstantAt(getCPIndex());
	    return (getName() + "\t" +
		    	  (c instanceof String ? "\"" + c + "\"" : c.toString()));
	}
    }

    
    public static abstract class Resolution extends ConstantPoolRead 
	implements Instruction.ExceptionThrower
    {
	static final TypeName.Scalar[] loadingErrors =
	    mayThrow(ERROR);
	protected TypeName.Scalar[] ex;

	public boolean isResolved(MethodInformation iv, Constants cp) {
	    // getConstantAt does not always return a constant:
	    // RepositoryString is not a Constant, Number is not a
	    // Constant, and a user-domain string sure as hell is not
	    // a constant.  (It isn't an object either...)
	    Object c  = cp.getConstantAt(getCPIndex(iv));
	    return  c instanceof ResolvedConstant;
	}

	/**
	 * Like <code>isResolved(null, cp)</code>, this method will
	 * return true if the constant this concrete instruction
	 * refers to has been resolved in cp.  The two-argument form
	 * of isResolved is preferred.
	 * @deprecated
	 **/
	public boolean isResolved(Constants cp) {
	    return isResolved(null, cp);
	}

	Resolution(int opcode, TypeName.Scalar[] ex) {
	    super(opcode);
	    this.ex = ex;
	}

	public TypeName.Scalar[] getThrowables() { return ex; }
    }
    /**
     * Instructions where the first immediate operand is a constant
     * pool reference that names the result type.  These instructions
     * include most (but not all) allocation instructions and
     * cast-like instructions.<p>
     *
     * FIXME: {@link Instruction.AFIAT} is not a <code>ConstantPoolPush</code> 
     * because <code>ConstantPoolPush</code> implements
     * {@link Instruction.ExceptionThrower}.
     **/
    public static abstract class ConstantPoolPush extends Resolution {
	public ConstantPoolPush(int opcode) {
	    super(opcode, null);
	}

	/** The type pushed by this instruction **/
	public ConstantClass getResultType(MethodInformation iv,
					   Constants c) {
	    return c.getClassAt(getCPIndex(iv));
	}

	public TypeName.Compound getResultTypeName(MethodInformation iv,
						   Constants c) {
	    return getResultType(iv, c).asTypeName().asCompound();
	}
    }

    public static interface Allocation {
	ConstantClass getResultType(MethodInformation iv, Constants c);
    }

    /**
     * Interface that any kind of invocation implements.
     **/
    public static abstract class Invocation
	extends Resolution {

	Invocation(int opcode) {
	    super(opcode, mayThrow(THROWABLE));
	    istreamIns = imm(new CPIndexValue(TypeCodes.USHORT));
	}
	/**
	 * Obtain the number of arguments. "this" in non-static invocations
	 * also counts as an argument, the "class" pointer for static
	 * invocations (see: JNI) does NOT count.  Double and long
	 * arguments <em>do not</em> count twice.<p> 
	 * 
	 * Thus this number gives the actual number of arguments, not slots,
	 * obtained from the stack. To obtain the number of <em>slots</em>,
	 * add to this the value returned by
	 * {@link #getWideArgumentCount}.<p> 
	 *
	 * Rationale: this number can not be obtained from the selector
	 * only, as the presence of the 'this' pointer is determined by the
	 * fact that this is a call on an object.<p> This super-method does
	 * take the this-pointer into account. For static methods, override
	 * and subtract 1.
	 * @return the number of arguments
	 **/
	public int getArgumentCount(MethodInformation iv, Constants cp) {
	    Selector.Method s = getSelector(iv, cp);
	    return 1 + s.getDescriptor().getArgumentCount();
	    // add "this" ptr
	}
	
	/**
	 * Obtain the number of wide arguments (longs and doubles, those that
	 * occupy two slots).
	 * @return the number of wide arguments
	 **/
	public int getWideArgumentCount(MethodInformation iv, Constants cp) {
	    Selector.Method s = getSelector(iv, cp);
	    return s.getDescriptor().getWideArgumentCount();
	}


	public int getArgumentLengthInWords(MethodInformation iv, Constants cp) {
	    return getArgumentCount(iv, cp) + getWideArgumentCount(iv, cp);
	}

	/**
	 * Get the selector describing the method called.
	 **/
	public Selector.Method getSelector(MethodInformation iv,
					   Constants c) {
	    return getConstantMethodref(iv, c).asSelector();
	}
    
	public ConstantMethodref getConstantMethodref(MethodInformation iv,
						      Constants c) {
	    return c.getMethodrefAt(getCPIndex(iv));
	}
    } // end of Invocation!

    /**
     * Interface that any kind of control-flow changing operation
     * implements (returns, gotos, jumps, switches)
     **/
    public interface FlowChange {
	 public PCValue getJumpTarget();
	 public abstract int getTargetCount(MethodInformation iv);
    }

/* FIXME: Runabout's ambiguity rules do not let us use FlowChange as 
 * a dispatch target in ovmp. Thus we had to make FlowChangeImpl
 * public. This should be changed if the Runabout is ever 
 * modified to match the Walkabout semantics. --jv, jt  May, 1, 03
 */ 
    public static abstract class FlowChangeImpl extends Instruction
	 implements FlowChange
    {
        PCValue jumpTarget;
	 public FlowChangeImpl(int opcode) {
	     super(opcode);
	 }
	 public PCValue getJumpTarget() {
	     return jumpTarget;
     }
	 public abstract int getTargetCount(MethodInformation iv);
     }

    public static abstract class BranchInstruction extends FlowChangeImpl 
        implements InstructionTargeter {
        protected BranchInstruction(int opcode) {
            super(opcode);
        }
        
        // Used by InstructionList to replace ConcretePCValues with InstructionHandles
        public abstract ConcretePCValue[] getAllTargets();
        public abstract void setAllTargets(ConcretePCValue[] targets);
        
        public abstract boolean isBranchOffsetShort();
        public abstract boolean containsTarget(InstructionHandle ih);
        public abstract void updateTarget(InstructionHandle old_ih,  InstructionHandle new_ih);
        public abstract InstructionHandle getTargetHandle();
    }
    
    /**
     * Common interface of all unconditional jumps with
     * fixed target, thus including "JSR" but not "RET" (!).
     **/
    public static abstract class UnconditionalJump 
	extends BranchInstruction {
	public UnconditionalJump(int opcode) {
	    super(opcode);
	}
	public int getTargetCount(MethodInformation iv) {
	    return 1;
	}
    public ConcretePCValue[] getAllTargets() { 
        return new ConcretePCValue[] { (ConcretePCValue)istreamIns[0] }; 
    }
    public void setAllTargets(ConcretePCValue[] targets) {
        if (targets.length != 1) throw new IllegalArgumentException();
        istreamIns[0] = targets[0];
    }
    public boolean containsTarget(InstructionHandle ih) {
        return istreamIns[0] == ih;
    }
    public void updateTarget(InstructionHandle old_ih,  InstructionHandle new_ih) {
        if (istreamIns[0] == old_ih) {
            istreamIns[0] = new_ih;
		old_ih.removeTargeter(this);
		if (new_ih != null)
			new_ih.addTargeter(this);
        } else
            throw new Error();
    }
    
	/**
	 * What is the target of the jump?
	 * @return the relative offset into the bytecode
	 **/
    public abstract int getTarget(MethodInformation iv);
    public abstract int getTarget();
    public String toString(MethodInformation iv) {
        return getName() + "\t" + (iv.getPC() + getTarget(iv));
    }
    public String toString() {
        return (isPrototype()
		? getName()
		: (getName() + "\t" + getTarget()));
    }
    }

    /**
     * Common parent of all conditional jump instructions.
     * The method "getBranchTarget" implemented here assumes
     * that the branchTarget is given as a short after
     * the opcode. Override if this is not the case. In
     * that case, you may also want to override size().<p>
     * One of the targets of the conditional jump MUST
     * be pc+size() [fallthrough].
     **/
    public static abstract class ConditionalJump
	extends BranchInstruction {
	protected Value controlValue;

	public ConditionalJump(int opcode) {
	    super(opcode);
	    PCValue target = new PCValue(TypeCodes.SHORT);
	    istreamIns = imm(target);
	    jumpTarget = target;
	}
    public ConditionalJump(int opcode, int relOffset) {
        this(opcode);
	jumpTarget = new ConcretePCValue(relOffset, TypeCodes.SHORT);
        istreamIns[0] = jumpTarget;
    }

    public boolean isBranchOffsetShort() { return true; }

    public ConcretePCValue[] getAllTargets() { 
        return new ConcretePCValue[] { (ConcretePCValue)istreamIns[0] }; 
    }
    public void setAllTargets(ConcretePCValue[] targets) {
        if (targets.length != 1) throw new IllegalArgumentException();
        istreamIns[0] = targets[0];
	jumpTarget = targets[0];
    }
    public boolean containsTarget(InstructionHandle ih) {
        return istreamIns[0] == ih;
    }
    public void updateTarget(InstructionHandle old_ih,  InstructionHandle new_ih) {
        if (istreamIns[0] == old_ih) {
            istreamIns[0] = new_ih;
	    jumpTarget = new_ih;
		old_ih.removeTargeter(this);
		if (new_ih != null)
			new_ih.addTargeter(this);
	} else
            throw new Error();
    }
    
	/**
	 * Get the target of the jump that is NOT fallthrough.
	 * @return the target relative (!) to the current pc
	 **/
	public int getBranchTarget(MethodInformation iv) {
	    return jumpTarget.decodeInt(iv, 1);
	}
 	/**
	 * How many different next PCs can occur after this
	 * FlowChange?
	 **/
	public int getTargetCount(MethodInformation iv) {
	    return 2;
	}
	public Value getControlValue() {
	    return controlValue;
	}
	public String toString(MethodInformation iv) {
	    return getName() + " " + (iv.getPC() + getBranchTarget(iv));
	}
	public String toString() {
	    return (isPrototype()
		    ? getName()
		    : (getName() + " " + getBranchTarget(null)));
	}
     }


    /**
     * Common class of all switches (n-target branches)
     **/
    public static abstract class Switch
	extends BranchInstruction {
	public Switch(int opcode) {
	    super(opcode);
	}
	/**
	 * Get the number of bytes padding that preceeds this
	 * instruction (0-3).
	 **/
	public int getPadding(MethodInformation iv) {
	    int pad = 0;
	    int pc = iv.getPC()+1;
	    while ((pc & 3) != 0) {
		pc++;
		pad++;
	    }
	    return pad;
	}

    public boolean isBranchOffsetShort() { return false; }
    
	/**
	 * Get the default target of the switch.
	 **/
	public abstract  int getDefaultTarget(MethodInformation iv);

	/**
	 * Get the value for each of the non-default
	 * targets.
	 **/
	public abstract  int[] getIndexForTargets(MethodInformation iv);

	/**
	 * Get all other possible targets (list), not including
	 * the default.
	 **/
	public abstract int[] getTargets(MethodInformation iv);

    public abstract  int getDefaultTarget();
    public abstract  int[] getIndexForTargets();
    public abstract int[] getTargets();
    
    public abstract InstructionHandle[] getTargetHandles(); 
                                      
	public int getArgumentsStartPosition(MethodInformation iv) {
	    int pc = iv.getPC() + 1;
	    while ((pc & 3) != 0)
		pc++;
	    return pc;
	}

    public int getPadding(int position) {
        int pc = position + 1;
        int pad = 0;
        while ((pc & 3) != 0) {
            pc++;
            pad++;
        }
        return pad;
    }

	public String toString(MethodInformation iv) {
	    int defaultTarget = getDefaultTarget(iv);
	    int[] indices = getIndexForTargets(iv);
	    int[] targets = getTargets(iv);
	    int pc = iv.getPC();
	    StringBuffer buf = new StringBuffer();
	    buf.append(getName());
	    buf.append("\n");
	    buf.append("\tdefault: ");
	    buf.append(pc + defaultTarget);
        buf.append("\n");
	    for(int i = 0; i < indices.length; i++) {
		buf.append("\t");
		buf.append(indices[i]);
		buf.append(" : ");
		buf.append(pc + targets[i] + "\n");
	    }
	    return buf.toString();
	}
    
    public String toString(Constants _) {
        int defaultTarget = getDefaultTarget();
        int[] indices = getIndexForTargets();
        int[] targets = getTargets();
        StringBuffer buf = new StringBuffer();
        buf.append(getName());
        buf.append("\n");
        buf.append("\tdefault: [relative]");
        buf.append(defaultTarget);
        buf.append("\n");
        for(int i = 0; i < indices.length; i++) {
        buf.append("\t");
        buf.append(indices[i]);
        buf.append(" : [relative]");
        buf.append(targets[i] + "\n");
        }
        return buf.toString();
    }

    }

    /**
     * Instructions that read or write fields
     * extend this class.
     **/
    public static abstract class FieldAccess extends Resolution {
	static final TypeName.Scalar[] instanceAccessErrors
	    = mayThrow(NULL_POINTER_EXCEPTION, ERROR);
	static final TypeName.Scalar[] staticAccessErrors
	    = mayThrow(ERROR);

	public FieldAccess(int opcode,
			   TypeName.Scalar[] ex) {
	    super(opcode, ex);
	    istreamIns = imm(TypeCodes.USHORT);
	}

    public FieldAccess(int opcode,
            TypeName.Scalar[] ex,
            int cpindex) {
        this(opcode, ex);
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	/**
	 * @return the selector of the location accessed.
	 **/
	public Selector.Field getSelector(MethodInformation iv,
					  Constants c) {
	    return getConstantFieldref(iv, c).asSelector();
	}

	public ConstantFieldref getConstantFieldref(MethodInformation iv,
						    Constants c) {
	    return c.getFieldrefAt(getCPIndex(iv));
	}
    }

    /**
     * FIXME Why isn't this a subtype of ConstantLoad?  OK, it is
     * harder to come up with an integer equivalent to a random string
     * than it is for null, but why does ACONST_NULL.getValue() return
     * 0 anyway?
     *
     * This class was renamed from 'Ldc' to avoid a conflict with
     * 'LDC' on case-preserving case-insensitive file
     * systems
     **/
    public static abstract class ConstantPoolLoad 
	extends ConstantPoolRead { 	
	ConstantPoolLoad(int opcode) {
	    super(opcode);
	}
	/**
	 * Obtain the type of the constant load.
	 * @return CONSTANT_XXXX value
	 **/
	public byte getCType(MethodInformation iv, Constants c) {
	    return c.getTagAt(getCPIndex(iv));
	}
	/**
	 * Get the value of the constant, wrapped as an
	 * object if required.
	 **/
	public Object getValue(MethodInformation iv, Constants c) {
	    return c.getConstantAt(getCPIndex(iv));
	}
    }


    /**
     * Common superclass of all return instructions.
     **/
    public static abstract class FlowEnd
	extends Instruction {
	FlowEnd(int opcode) {
	    super(opcode);
	}
    }
    
    /* ******************** concrete instructions ******************** */


    /**
     * @author janvitek, pizlo
     */
    public static  class POLLCHECK extends Instruction  {
	public static final int opcode = JVMConstants.Opcodes.POLLCHECK;
	public static final POLLCHECK singleton = new POLLCHECK();
	public POLLCHECK(){
	     super(opcode);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }


    public static class INB extends Instruction {
	public static final int opcode = JVMConstants.Opcodes.INB;
	public static final INB singleton = new INB();
	public INB(){
          super(opcode);
          this.stackIns = new IntValue[] {new IntValue(TypeCodes.UINT)}; 
          this.stackOuts = new IntValue[] {new IntValue(TypeCodes.UBYTE)}; 
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }    
    }

    public static class OUTB extends Instruction {
	public static final int opcode = JVMConstants.Opcodes.OUTB;
	public static final OUTB singleton = new OUTB();
	public OUTB(){
          super(opcode);
          this.stackIns = new IntValue[] {new IntValue(TypeCodes.UBYTE), new IntValue(TypeCodes.UINT)}; 	     
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }    
    }


    /**
     *  takes one argument from stack, which should be 
     *	objectref or arrayref
     *	checks that it is not null ; inserted by OVM to implement
     *  NullPointerExceptions
     */
    public static  class NULLCHECK extends StackManipulation implements ExceptionThrower {
	public static final int opcode = JVMConstants.Opcodes.NULLCHECK;
	public static final NULLCHECK singleton = new NULLCHECK();
	public NULLCHECK(){
	     super(opcode);
	     
	    this.stackIns = new Value[] {new Value()}; 
	    this.stackOuts = new Value[] {this.stackIns[0]};   	     
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
	
	TypeName.Scalar[] ex = mayThrow(NULL_POINTER_EXCEPTION);
        public TypeName.Scalar[] getThrowables() { return ex; }

    }

    public static  class INCREMENT_COUNTER extends StackManipulation {
	public static final int opcode = JVMConstants.Opcodes.INCREMENT_COUNTER;
	public static final INCREMENT_COUNTER singleton = new INCREMENT_COUNTER();
	public INCREMENT_COUNTER(){
	     super(opcode);
	     
	    this.stackIns = new Value[] {new IntValue()}; 
	    this.stackOuts = new Value[] {};   	     
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
	
    }

    /**
     * TODO: this is not a singleton as we can have one instance per opcode....
     */
    public static class UNIMPLEMENTED 
	extends Instruction { 
        public static final UNIMPLEMENTED singleton = new UNIMPLEMENTED(512); // TODO -- yuck!
	public UNIMPLEMENTED(int opcode) {
	    super(opcode);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
	

    /**
     * This instruction loads a reference from the local variables. The index
     * into the local variables is the unsigned byte following the opcode.
     * <p>
     * <ul>
     * <li>
     **/
    public static class ALOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ALOAD;
        public static final ALOAD singleton = new ALOAD();
        public static ALOAD make(int localIndex) {
            ALOAD clone = (ALOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }
	public ALOAD() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), refFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_ALOAD.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an int from the local variables. The index into
     * the local variables is the unsigned byte following the opcode.
     **/
    public static class ILOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ILOAD;
        public static final ILOAD singleton = new ILOAD();
        public static ILOAD make(int localIndex) {
            ILOAD clone = (ILOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }

	public ILOAD() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), intFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_ILOAD.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
				   
    /**
     * This instruction loads a float from the local variables. The index into
     * the local variables is the unsigned byte following the opcode.
     **/
    public static class FLOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.FLOAD;
        public static final FLOAD singleton = new FLOAD();
        public static FLOAD make(int localIndex) {
            FLOAD clone = (FLOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }

	public FLOAD() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), floatFactory);
 	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_FLOAD.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads a double from the local variables. The index into
     * the local variables is the unsigned byte following the opcode.
     **/
    public static class DLOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.DLOAD;
        public static final DLOAD singleton = new DLOAD();
        public static DLOAD make(int localIndex) {
            DLOAD clone = (DLOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }

	public DLOAD() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), doubleFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_DLOAD.make(localIndex);
    }
    
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads a double from
     * the local variables. The index into the local
     * variables is the unsigned byte following the
     * opcode.
     **/
    public static class LLOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.LLOAD;
        public static final LLOAD singleton = new LLOAD();
        public static LLOAD make(int localIndex) {
            LLOAD clone = (LLOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }

	public LLOAD() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), longFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_LLOAD.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class ALOAD_0 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ALOAD_0;
        public static final ALOAD_0 singleton = new ALOAD_0();
	public ALOAD_0() {
	    super(opcode, ConcreteIntValue.ZERO, refFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return ALOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_ALOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class ALOAD_1 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ALOAD_1;
        public static final ALOAD_1 singleton = new ALOAD_1();
	public ALOAD_1 () {
	    super(opcode, ConcreteIntValue.ONE, refFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return ALOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_ALOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class ALOAD_2	extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ALOAD_2;
        public static final ALOAD_2 singleton = new ALOAD_2();
	public ALOAD_2() {
	    super(opcode, ConcreteIntValue.TWO, refFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return ALOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_ALOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class ALOAD_3 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ALOAD_3;
        public static final ALOAD_3 singleton = new ALOAD_3();
	public ALOAD_3 () {
	    super(opcode, ConcreteIntValue.THREE, refFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return ALOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_ALOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class ILOAD_0 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ILOAD_0;
        public static final ILOAD_0 singleton = new ILOAD_0();
	public ILOAD_0() {
	    super(opcode, ConcreteIntValue.ZERO, intFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return ILOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_ILOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class ILOAD_1 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ILOAD_1;
        public static final ILOAD_1 singleton = new ILOAD_1();
	public ILOAD_1 () {
	    super(opcode, ConcreteIntValue.ONE, intFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return ILOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_ILOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class ILOAD_2 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ILOAD_2;
        public static final ILOAD_2 singleton = new ILOAD_2();
	public ILOAD_2() {
	    super(opcode, ConcreteIntValue.TWO, intFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return ILOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_ILOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class ILOAD_3 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.ILOAD_3;
        public static final ILOAD_3 singleton = new ILOAD_3();
	public ILOAD_3 () {
	    super(opcode, ConcreteIntValue.THREE, intFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return ILOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_ILOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    
    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class FLOAD_0 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.FLOAD_0;
        public static final FLOAD_0 singleton = new FLOAD_0();
	public FLOAD_0() {
	    super(opcode, ConcreteIntValue.ZERO, floatFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return FLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_FLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class FLOAD_1 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.FLOAD_1;
        public static final FLOAD_1 singleton = new FLOAD_1();
	public FLOAD_1 () {
	    super(opcode, ConcreteIntValue.ONE, floatFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return FLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_FLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class FLOAD_2 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.FLOAD_2;
        public static final FLOAD_2 singleton = new FLOAD_2();
	public FLOAD_2() {
	    super(opcode, ConcreteIntValue.TWO, floatFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return FLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_FLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class FLOAD_3 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.FLOAD_3;
        public static final FLOAD_3 singleton = new FLOAD_3();
	public FLOAD_3 () {
	    super(opcode, ConcreteIntValue.THREE, floatFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return FLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_FLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class DLOAD_0 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.DLOAD_0;
        public static final DLOAD_0 singleton = new DLOAD_0();
	public DLOAD_0() {
	    super(opcode, ConcreteIntValue.ZERO, doubleFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return DLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_DLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class DLOAD_1 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.DLOAD_1;
        public static final DLOAD_1 singleton = new DLOAD_1();
	public DLOAD_1 () {
	    super(opcode, ConcreteIntValue.ONE, doubleFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return DLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_DLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class DLOAD_2 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.DLOAD_2;
        public static final DLOAD_2 singleton = new DLOAD_2();
	public DLOAD_2() {
	    super(opcode, ConcreteIntValue.TWO, doubleFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return DLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_DLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class DLOAD_3 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.DLOAD_3;
        public static final DLOAD_3 singleton = new DLOAD_3();
	public DLOAD_3 () {
	    super(opcode, ConcreteIntValue.THREE, doubleFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return DLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_DLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class LLOAD_0 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.LLOAD_0;
        public static final LLOAD_0 singleton = new LLOAD_0();
	public LLOAD_0() {
	    super(opcode, ConcreteIntValue.ZERO, longFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return LLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_LLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class LLOAD_1 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.LLOAD_1;
        public static final LLOAD_1 singleton = new LLOAD_1();
	public LLOAD_1 () {
	    super(opcode, ConcreteIntValue.ONE, longFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return LLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_LLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class LLOAD_2 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.LLOAD_2;
        public static final LLOAD_2 singleton = new LLOAD_2();
	public LLOAD_2() {
	    super(opcode, ConcreteIntValue.TWO, longFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return LLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_LLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction loads an arbitrary constant from
     * the local variables. The index into the local
     * variables is determined by the opcode of the
     * instruction and passed to the constructor
     * when the instruction is created.
     **/
    public static class LLOAD_3 extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.LLOAD_3;
        public static final LLOAD_3 singleton = new LLOAD_3();
	public LLOAD_3 () {
	    super(opcode, ConcreteIntValue.THREE, longFactory);
	}
    public Instruction widen() {
        int localIndex = offset.intValue();
        if (NumberRanges.isUnsignedByte(localIndex)) {
            return LLOAD.make(localIndex);
        } else {
            char clocalIndex = NumberRanges.checkUnsignedShort(localIndex);
            return WIDE_LLOAD.make(clocalIndex);
        }
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class RETURN extends ReturnInstruction { 
        public static final int opcode = JVMConstants.Opcodes.RETURN;
        public static final RETURN singleton = new RETURN();
	public RETURN() { super(opcode); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class IRETURN extends ReturnValue { 
        public static final int opcode = JVMConstants.Opcodes.IRETURN;
        public static final IRETURN singleton = new IRETURN();
	public IRETURN() { super(opcode, intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    
    public static class LRETURN extends ReturnValue { 
        public static final int opcode = JVMConstants.Opcodes.LRETURN;
        public static final LRETURN singleton = new LRETURN();
	public LRETURN() { super(opcode, longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
   
    public static class DRETURN extends ReturnValue { 
        public static final int opcode = JVMConstants.Opcodes.DRETURN;
        public static final DRETURN singleton = new DRETURN();
	public DRETURN() { super(opcode, doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class FRETURN extends ReturnValue { 
        public static final int opcode = JVMConstants.Opcodes.FRETURN;
        public static final FRETURN singleton = new FRETURN();
	public FRETURN() { super(opcode, floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class ARETURN extends ReturnValue { 
        public static final int opcode = JVMConstants.Opcodes.ARETURN;
        public static final ARETURN singleton = new ARETURN();
	public ARETURN() { super(opcode, refFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in an unsigned
     * short (char) after the instruction.
     **/
    public static class ASTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.ASTORE;
        public static final ASTORE singleton = new ASTORE();
        public static ASTORE make(int localIndex) {
            ASTORE clone = (ASTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }

	public ASTORE() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), refFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_ASTORE.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop an int from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in an unsigned
     * short (char) after the instruction.
     **/
    public static class ISTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.ISTORE;
        public static final ISTORE singleton = new ISTORE();
        public static ISTORE make(int localIndex) {
            ISTORE clone = (ISTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }
	public ISTORE() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), intFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_ISTORE.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a float from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in an unsigned
     * short (char) after the instruction.
     **/
    public static class FSTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.FSTORE;
        public static final FSTORE singleton = new FSTORE();
        public static FSTORE make(int localIndex) {
            FSTORE clone = (FSTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }
	public FSTORE() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), floatFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_FSTORE.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a double from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in an unsigned
     * short (char) after the instruction.
     **/
    public static class DSTORE 
	extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.DSTORE;
        public static final DSTORE singleton = new DSTORE();
        public static DSTORE make(int localIndex) {
            DSTORE clone = (DSTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }
	public DSTORE() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), doubleFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_DSTORE.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a long from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in an unsigned
     * short (char) after the instruction.
     **/
    public static class LSTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.LSTORE;
        public static final LSTORE singleton = new LSTORE();
        public static LSTORE make(int localIndex) {
            LSTORE clone = (LSTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(localIndex, TypeCodes.UBYTE));
            return clone;
        }
	public LSTORE() {
	    super(opcode, new IntValue(TypeCodes.UBYTE), longFactory);
	}
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_LSTORE.make(localIndex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class ASTORE_0 extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.ASTORE_0;
        public static final ASTORE_0 singleton = new ASTORE_0();
	public ASTORE_0() {
	    super(opcode, ConcreteIntValue.ZERO, refFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class ASTORE_1 extends LocalWrite   { 
        public static final int opcode = JVMConstants.Opcodes.ASTORE_1;
        public static final ASTORE_1 singleton = new ASTORE_1();
	public ASTORE_1() {
	    super(opcode, ConcreteIntValue.ONE, refFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

      /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class ASTORE_2 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.ASTORE_2;
        public static final ASTORE_2 singleton = new ASTORE_2();
	public ASTORE_2() {
	    super(opcode, ConcreteIntValue.TWO, refFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class ASTORE_3 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.ASTORE_3;
        public static final ASTORE_3 singleton = new ASTORE_3();
	public ASTORE_3() {
	    super(opcode, ConcreteIntValue.THREE, refFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }

    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class ISTORE_0 extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.ISTORE_0;
        public static final ISTORE_0 singleton = new ISTORE_0();
	public ISTORE_0() {
	    super(opcode, ConcreteIntValue.ZERO, intFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class ISTORE_1 extends LocalWrite   { 
        public static final int opcode = JVMConstants.Opcodes.ISTORE_1;
        public static final ISTORE_1 singleton = new ISTORE_1();
	public ISTORE_1() {
	    super(opcode, ConcreteIntValue.ONE, intFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

      /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class ISTORE_2 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.ISTORE_2;
        public static final ISTORE_2 singleton = new ISTORE_2();
	public ISTORE_2() {
	    super(opcode, ConcreteIntValue.TWO, intFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }

    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class ISTORE_3 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.ISTORE_3;
        public static final ISTORE_3 singleton = new ISTORE_3();
	public ISTORE_3() {
	    super(opcode, ConcreteIntValue.THREE, intFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class FSTORE_0 extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.FSTORE_0;
        public static final FSTORE_0 singleton = new FSTORE_0();
	public FSTORE_0() {
	    super(opcode, ConcreteIntValue.ZERO, floatFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class FSTORE_1 extends LocalWrite   { 
        public static final int opcode = JVMConstants.Opcodes.FSTORE_1;
        public static final FSTORE_1 singleton = new FSTORE_1();
	public FSTORE_1() {
	    super(opcode, ConcreteIntValue.ONE, floatFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

      /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class FSTORE_2 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.FSTORE_2;
        public static final FSTORE_2 singleton = new FSTORE_2();
	public FSTORE_2() {
	    super(opcode, ConcreteIntValue.TWO, floatFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class FSTORE_3 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.FSTORE_3;
        public static final FSTORE_3 singleton = new FSTORE_3();
	public FSTORE_3() {
	    super(opcode, ConcreteIntValue.THREE, floatFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class LSTORE_0 extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.LSTORE_0;
        public static final LSTORE_0 singleton = new LSTORE_0();
	public LSTORE_0() {
	    super(opcode, ConcreteIntValue.ZERO, longFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class LSTORE_1 extends LocalWrite   { 
        public static final int opcode = JVMConstants.Opcodes.LSTORE_1;
        public static final LSTORE_1 singleton = new LSTORE_1();
	public LSTORE_1() {
	    super(opcode, ConcreteIntValue.ONE, longFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    
    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class LSTORE_2 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.LSTORE_2;
        public static final LSTORE_2 singleton = new LSTORE_2();
	public LSTORE_2() {
	    super(opcode, ConcreteIntValue.TWO, longFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class LSTORE_3 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.LSTORE_3;
        public static final LSTORE_3 singleton = new LSTORE_3();
	public LSTORE_3() {
	    super(opcode, ConcreteIntValue.THREE, longFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class DSTORE_0 extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.DSTORE_0;
        public static final DSTORE_0 singleton = new DSTORE_0();
	public DSTORE_0() {
	    super(opcode, ConcreteIntValue.ZERO, doubleFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class DSTORE_1 extends LocalWrite   { 
        public static final int opcode = JVMConstants.Opcodes.DSTORE_1;
        public static final DSTORE_1 singleton = new DSTORE_1();
	public DSTORE_1() {
	    super(opcode, ConcreteIntValue.ONE, doubleFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class DSTORE_2 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.DSTORE_2;
        public static final DSTORE_2 singleton = new DSTORE_2();
	public DSTORE_2() {
	    super(opcode, ConcreteIntValue.TWO, doubleFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop a reference from the top of the stack and store it in a local
     * variable. The index of the local variable is specified in the
     * constructor of the instruction.<p>
     **/
    public static class DSTORE_3 extends LocalWrite  { 
        public static final int opcode = JVMConstants.Opcodes.DSTORE_3;
        public static final DSTORE_3 singleton = new DSTORE_3();
	public DSTORE_3() {
	    super(opcode, ConcreteIntValue.THREE, doubleFactory);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: index, arrayref<p>
     * Read entry arrayref[index] and push it onto the stack.<p>
     **/
    public static class AALOAD  extends ArrayLoad { 
        public static final int opcode = JVMConstants.Opcodes.AALOAD;
        public static final AALOAD singleton = new AALOAD();
    public AALOAD() { this(opcode); }
    public AALOAD(int oc) { 
      super(oc, refFactory, CSACallExp.Names.aaloadBarrier); 
    //  super(oc, refFactory, null); 
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    
    public static class READ_BARRIER
	extends ExceptionThrowerImpl { 
        public static final int opcode = JVMConstants.Opcodes.READ_BARRIER;
        public static final READ_BARRIER singleton = new READ_BARRIER();
    	public READ_BARRIER()  {
	    // FIXME: THROWABLE should be MEMORY_ACCESS_ERROR, but the
	    // latter constant has been removed
 	    super(opcode, mayThrow(THROWABLE));
 	    RefValue val = new RefValue();
	    stackIns = stack(val);
	    evals = eval(new CSACallExp("readBarrier",val));
    	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }


    //this is non-nullchecking version

    public static class NONCHECKING_TRANSLATING_READ_BARRIER
	extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.NONCHECKING_TRANSLATING_READ_BARRIER;
        public static final NONCHECKING_TRANSLATING_READ_BARRIER singleton = new NONCHECKING_TRANSLATING_READ_BARRIER();
    	public NONCHECKING_TRANSLATING_READ_BARRIER()  {
 	    super(opcode);
 	    RefValue val = new RefValue();
	    this.stackIns = new Value[] { new RefValue() } ;
            // this is probably ignored. why ? (the used code is in SpecInstantiation.java)
	    this.stackOuts = new Value[] {  
	      new RefValue(new CSACallExp("lazyTranslatingReadBarrier",stackIns[0]))
            };
    	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class CHECKING_TRANSLATING_READ_BARRIER
	extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.CHECKING_TRANSLATING_READ_BARRIER;
        public static final CHECKING_TRANSLATING_READ_BARRIER singleton = new CHECKING_TRANSLATING_READ_BARRIER();
    	public CHECKING_TRANSLATING_READ_BARRIER()  {
 	    super(opcode);
 	    RefValue val = new RefValue();
	    this.stackIns = new Value[] { new RefValue() } ;
            // this is probably ignored. why ? (the used code is in SpecInstantiation.java)
	    this.stackOuts = new Value[] {  
	      new RefValue(new CSACallExp("eagerTranslatingReadBarrier",stackIns[0]))
            };
    	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

/*
  null-checking version
  
    public static class TRANSLATING_READ_BARRIER
	extends ExceptionThrower { 
        public static final int opcode = JVMConstants.Opcodes.TRANSLATING_READ_BARRIER;
        public static final TRANSLATING_READ_BARRIER singleton = new TRANSLATING_READ_BARRIER();
    	public TRANSLATING_READ_BARRIER()  {
 	    super(opcode);
 	    RefValue val = new RefValue();
	    this.stackIns = new Value[] { new RefValue() } ;
	    this.stackOuts = new Value[] { new NonnullRefValue() };
            };
    	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
*/
    /**
     * on the stack: index, arrayref<p>
     * Read entry arrayref[index] and push it onto the stack.<p>
     **/
    public static class IALOAD  extends ArrayLoad { 
        public static final int opcode = JVMConstants.Opcodes.IALOAD;
        public static final IALOAD singleton = new IALOAD();
  	public IALOAD() { super(opcode, intFactory, CSACallExp.Names.ialoadBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: index, arrayref<p>
     * Read entry arrayref[index] and push it onto the stack.<p>
     **/
    public static class FALOAD extends ArrayLoad { 
        public static final int opcode = JVMConstants.Opcodes.FALOAD;
        public static final FALOAD singleton = new FALOAD();
  	public FALOAD() { super(opcode, floatFactory, CSACallExp.Names.faloadBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: index, arrayref<p>
     * Read entry arrayref[index] and push it onto the stack.<p>
     **/
    public static class DALOAD  extends ArrayLoad { 
        public static final int opcode = JVMConstants.Opcodes.DALOAD;
        public static final DALOAD singleton = new DALOAD();
	public DALOAD() { super(opcode, doubleFactory, CSACallExp.Names.daloadBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
   }

    /**
     * on the stack: index, arrayref<p>
     * Read entry arrayref[index] and push it onto the stack.<p>
     **/
    public static class BALOAD  extends ArrayLoad { 
        public static final int opcode = JVMConstants.Opcodes.BALOAD;
        public static final BALOAD singleton = new BALOAD();
	public BALOAD() { super(opcode, cvtByteFactory, CSACallExp.Names.baloadBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: index, arrayref<p>
     * Read entry arrayref[index] and push it onto the stack.<p>
     **/
    public static class CALOAD  extends ArrayLoad { 
        public static final int opcode = JVMConstants.Opcodes.CALOAD;
        public static final CALOAD singleton = new CALOAD();
	public CALOAD() { super(opcode, cvtCharFactory, CSACallExp.Names.caloadBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: index, arrayref<p>
     * Read entry arrayref[index] and push it onto the stack.<p>
     **/
    public static class SALOAD extends ArrayLoad { 
        public static final int opcode = JVMConstants.Opcodes.SALOAD;
        public static final SALOAD singleton = new SALOAD();
	public SALOAD() { super(opcode, cvtShortFactory, CSACallExp.Names.saloadBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: index, arrayref<p>
     * Read entry arrayref[index] and push it onto the stack.<p>
     **/
    public static class LALOAD extends ArrayLoad { 
        public static final int opcode = JVMConstants.Opcodes.LALOAD;
        public static final LALOAD singleton = new LALOAD();
 	public LALOAD() { super(opcode, longFactory, CSACallExp.Names.laloadBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: arrayref, index, value<p>
     * Set entry arrayref[index]=value, pop all 3 parameters.<p>
     * Throws NullPointerException and ArrayIndexOutOfBoundsException.<br>
     **/
    public static class AASTORE extends ArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.AASTORE;
        public static final AASTORE singleton = new AASTORE();
	public AASTORE()  {
	    this(opcode);
	}
	public AASTORE(int oc)  {
	    super(oc, refFactory, refFactory, CSACallExp.Names.aastoreBarrier);
	    // FIXME: whatever
	    ex = mayThrow(NULL_POINTER_EXCEPTION,
			  ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION,
			  ARRAY_STORE_EXCEPTION,
			  ERROR);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: arrayref, index, value<p>
     * Set entry arrayref[index]=value, pop all 3 parameters.<p>
     * Throws NullPointerException and ArrayIndexOutOfBoundsException.<br>
     **/
    public static class AASTORE_WITH_BARRIER
	extends AASTORE { 
        public static final int opcode = JVMConstants.Opcodes.AASTORE_WITH_BARRIER;
        public static final AASTORE_WITH_BARRIER singleton = new AASTORE_WITH_BARRIER();
	public AASTORE_WITH_BARRIER()  {
	    super(opcode);
	    NonnulArrayRefValue arr = new NonnulArrayRefValue();
	    IntValue idx = new IntValue();
	    stackIns = stack(refFactory.make(), // value
			     idx,
			     arr);
	    Value offset =
		new IntValue(
                   new BinExp(
                       new IntValue(
                           new ConversionExp(
                               new RefValue(
                                   new UnaryExp(
                                       "&",
                                       new Value(
                                                 new ArrayAccessExp(arr, idx, true)))))),
                       "-",
                       new IntValue(new ConversionExp(arr))));
	    evals = eval(new IfCmd(new IntValue
				   (new CallExp("ARRAY_STORE_INVALID",
						arr, stackIns[0])),
				   throwException(ARRAY_STORE_EXCEPTION)),
//			 new CSACallExp("aastoreBarrier",
			 new CSACallExp(CSACallExp.Names.aastoreBarrier,
					arr,
					offset,
					stackIns[0]));
	    if (!NO_BOUND_CHECK)
		pushEval(boundCheck(arr, idx));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * on the stack: arrayref, index, value<p>
     * Set entry arrayref[index]=value, pop all 3 parameters.<p>
     * Throws NullPointerException.<br>
     **/
     
    // FIXME: refactor the base class (it is only "used" by this class, but bypassed) 
    public static class UNCHECKED_AASTORE 	extends UncheckedArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.UNCHECKED_AASTORE;
        public static final UNCHECKED_AASTORE singleton = new UNCHECKED_AASTORE();
	public UNCHECKED_AASTORE()  {

	    super(opcode);
	    NonnulArrayRefValue arr = new NonnulArrayRefValue();
	    IntValue idx = new IntValue();
	    stackIns = stack(refFactory.make(), // value
			     idx,
			     arr);
			     
            if (MemoryManager.the().needsArrayAccessBarriers()) {
              evals = eval(
			 new CSACallExp(CSACallExp.Names.aastoreBarrier,
					arr,
					idx,
					stackIns[0]));            
            } else {
  	      Value offset =
		new IntValue(
                   new BinExp(
                       new IntValue(
                           new ConversionExp(
                               new RefValue(
                                   new UnaryExp(
                                       "&",
                                       new Value(
                                                 new ArrayAccessExp(arr, idx, true)))))),
                       "-",
                       new IntValue(new ConversionExp(arr))));
              evals = eval(
			 new CSACallExp(CSACallExp.Names.aastoreBarrier,
					arr,
					offset,
					stackIns[0]));
            }
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class BASTORE extends ArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.BASTORE;
        public static final BASTORE singleton = new BASTORE();
	public BASTORE()  { super(opcode, intFactory, byteFactory, CSACallExp.Names.bastoreBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class CASTORE extends ArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.CASTORE;
        public static final CASTORE singleton = new CASTORE();
	public CASTORE()  { super(opcode, intFactory, charFactory, CSACallExp.Names.castoreBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    
    public static class DASTORE extends ArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.DASTORE;
        public static final DASTORE singleton = new DASTORE();
	public DASTORE()  { super(opcode, doubleFactory, doubleFactory, CSACallExp.Names.dastoreBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }   

    public static class FASTORE extends ArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.FASTORE;
        public static final FASTORE singleton = new FASTORE();
	public FASTORE()  { super(opcode, floatFactory, floatFactory, CSACallExp.Names.fastoreBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class IASTORE extends ArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.IASTORE;
        public static final IASTORE singleton = new IASTORE();
	public IASTORE()  { super(opcode, intFactory, intFactory, CSACallExp.Names.iastoreBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class LASTORE extends ArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.LASTORE;
        public static final LASTORE singleton = new LASTORE();
	public LASTORE()  { super(opcode, longFactory, longFactory, CSACallExp.Names.lastoreBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class SASTORE extends ArrayStore { 
        public static final int opcode = JVMConstants.Opcodes.SASTORE;
        public static final SASTORE singleton = new SASTORE();
	public SASTORE()  { super(opcode, intFactory, shortFactory, CSACallExp.Names.sastoreBarrier); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction puts a null pointer onto the stack.
     **/
    public static class ACONST_NULL extends ConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.ACONST_NULL;
        public static final ACONST_NULL singleton = new ACONST_NULL();
	public ACONST_NULL() {
	    super(opcode, false, NullRefValue.INSTANCE);
	}	
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction puts an integer onto the stack.
     * The value is specified by the signed byte following
     * the instruction opcode. (yes, a signed BYTE, not
     * a signed int!)
     **/
    public static class BIPUSH extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.BIPUSH;
        public static final BIPUSH singleton = new BIPUSH();
        public static BIPUSH make(byte value) {
            BIPUSH clone = (BIPUSH)singleton.clone();
            clone.istreamIns =
		imm(new ConcreteIntValue((int)value, TypeCodes.BYTE));
            return clone;
        }
	public BIPUSH() {
	    super(opcode, true, new IntValue(TypeCodes.BYTE));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Push a short (internally represented as an integer)
     * onto the stack. The value of the short follows
     * the opcode in the byteCode.
     **/
    public static class SIPUSH extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.SIPUSH;
        public static final SIPUSH singleton = new SIPUSH();
        public static SIPUSH make(short value) {
            SIPUSH clone = (SIPUSH)singleton.clone();
            clone.istreamIns =
		imm(new ConcreteIntValue((int)value, TypeCodes.SHORT));
            return clone;
        }        
        public SIPUSH() { super(opcode, true, new IntValue(TypeCodes.SHORT)); }
        public SIPUSH(short value) { super(opcode, true, 
                new ConcreteIntValue((int)value, TypeCodes.SHORT)); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction puts a double onto the stack.
     **/
    public static class DCONST_0 extends DConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.DCONST_0;
        public static final DCONST_0 singleton = new DCONST_0();
	public DCONST_0() { super(opcode, false, ConcreteDoubleValue.ZERO); }
    public double getDValue(MethodInformation iv) { return 0.0d; }
    public double getDValue() { return 0.0d; }    
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction puts a double onto the stack.
     **/
    public static class DCONST_1 extends DConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.DCONST_1;
        public static final DCONST_1 singleton = new DCONST_1();
	public DCONST_1() { super(opcode, false, ConcreteDoubleValue.ONE); }
	public double getDValue(MethodInformation iv) { return 1.0d; }
    public double getDValue() { return 1.0d; }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction puts a float onto the stack.
     **/
    public static class FCONST_0 extends FConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.FCONST_0;
        public static final FCONST_0 singleton = new FCONST_0();
	public FCONST_0() { super(opcode, false, ConcreteFloatValue.ZERO); }
    public float getFValue(MethodInformation iv) { return 0.0f; }
    public float getFValue() { return 0.0f; }    
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction puts a float onto the stack.
     **/
    public static class FCONST_1 extends FConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.FCONST_1;
        public static final FCONST_1 singleton = new FCONST_1();
	public FCONST_1() { super(opcode, false, ConcreteFloatValue.ONE); }
	public float getFValue(MethodInformation iv) { return 1.0f; }
    public float getFValue() { return 1.0f; }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction puts a float onto the stack.
     **/
    public static class FCONST_2 extends FConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.FCONST_2;
        public static final FCONST_2 singleton = new FCONST_2();
	public FCONST_2() { super(opcode, false, ConcreteFloatValue.TWO); }
	public float getFValue(MethodInformation iv) { return 2.0f; }
    public float getFValue() { return 2.0f; }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    
    /**
     * This instruction puts an int onto the stack.
     **/
    public static class ICONST_M1 extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.ICONST_M1;
        public static final ICONST_M1 singleton = new ICONST_M1();
        public static ICONST_M1 make() { return singleton; }

	public ICONST_M1() { super(opcode, false, ConcreteIntValue.MINUSONE); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class ICONST_0 extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.ICONST_0;
        public static final ICONST_0 singleton = new ICONST_0();
        public static ICONST_0 make() { return singleton; }
	public ICONST_0() { super(opcode, false, ConcreteIntValue.ZERO); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class ICONST_1 extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.ICONST_1;
        public static final ICONST_1 singleton = new ICONST_1();
        public static ICONST_1 make() { return singleton; }

	public ICONST_1() { super(opcode, false, ConcreteIntValue.ONE); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class ICONST_2 extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.ICONST_2;
        public static final ICONST_2 singleton = new ICONST_2();
        public static ICONST_2 make() { return singleton; }

	public ICONST_2() { super(opcode, false, ConcreteIntValue.TWO); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class ICONST_3 extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.ICONST_3;
        public static final ICONST_3 singleton = new ICONST_3();
        public static ICONST_3 make() { return singleton; }

	public ICONST_3() { super(opcode, false, ConcreteIntValue.THREE); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class ICONST_4 extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.ICONST_4;
        public static final ICONST_4 singleton = new ICONST_4();
        public static ICONST_4 make() { return singleton; }

	public ICONST_4() { super(opcode, false, ConcreteIntValue.FOUR); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class ICONST_5 extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.ICONST_5;
        public static final ICONST_5 singleton = new ICONST_5();
        public static ICONST_5 make() { return singleton; }

	public ICONST_5() { super(opcode, false, ConcreteIntValue.FIVE); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }


    /**
     * This instruction puts a long onto the stack.
     **/
    public static class LCONST_0 extends LConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.LCONST_0;
        public static final LCONST_0 singleton = new LCONST_0();
	public LCONST_0() { super(opcode, false, ConcreteLongValue.ZERO); }
    public long getLValue(MethodInformation iv) { return 0L; }
    public long getLValue() { return 0L; }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * This instruction puts a long onto the stack.
     **/
    public static class LCONST_1 extends LConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.LCONST_1;
        public static final LCONST_1 singleton = new LCONST_1();
	public LCONST_1() { super(opcode, false, ConcreteLongValue.ONE); }
    public long getLValue(MethodInformation iv) { return 1L; }
    public long getLValue() { return 1L; }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a constant from the contant pool and push it onto the
     * stack.  The index is specified as an unsigned byte after the
     * opcode. The constant may be a String!
     *
     * This is the slow variant of LDC that does a CSA upcall
     * to resolve the String/SharedState.
     **/
    public static class LDC
	extends ConstantPoolLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC;
        public static final LDC singleton = new LDC();
	LDC() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(CPIndexValue.CONSTANT_Any,
						TypeCodes.UBYTE);	    
	    istreamIns = imm(val);
	    this.stackOuts =
		/* ok, RefValue is not quite right for the general LDC
		   opcode, but it is the right type for quickified
		   OVM IR and it is "close enough" for bytecode */
		stack(new RefValue
		      (new CSACallExp("resolveLDC",  
				      new IntValue(new ConversionExp((Value)istreamIns[0])),
				      new RefValue(new CurrentConstantPool())))
		      );
	}
    public LDC(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex, TypeCodes.UBYTE);
    }
	public boolean doesNeedWidening() {
	    int cpIndex = istreamIns[0].decodeInt(null, -1);
	    return ! NumberRanges.isUnsignedByte(cpIndex);
	}
	public Instruction widen() {
	    return new LDC_W(istreamIns[0].decodeInt(null, -1));
	}
	public void setCPIndex(int index) {
	    if (istreamIns[0] instanceof ConcreteCPIndexValue) {
		istreamIns[0] = new ConcreteCPIndexValue(index, TypeCodes.UBYTE);
	    } else {
		throw new Error("Tried to set the CP index on an abstract Instruction");
	    }
	}

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a non-wide primitive constant from the contant pool and
     * push it onto the stack.  The index is specified as an unsigned
     * byte after the opcode. The constant may NOT be a String! No
     * resolution is required.
     **/
    public static class LDC_REF_QUICK
	extends ConstantPoolLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC_REF_QUICK;
        public static final LDC_REF_QUICK singleton = new LDC_REF_QUICK();
	LDC_REF_QUICK() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Reference,
						TypeCodes.UBYTE);
	    istreamIns = imm(val);
	    stackOuts = stack(new RefValue(new CPAccessExp(val)));
	}
    public LDC_REF_QUICK(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex, TypeCodes.UBYTE);
    }
	public boolean doesNeedWidening() {
	    int cpIndex = istreamIns[0].decodeInt(null, -1);
	    return ! NumberRanges.isUnsignedByte(cpIndex);
	}
	public Instruction widen() {
	    return new LDC_W_REF_QUICK(istreamIns[0].decodeInt(null, -1));
	}
	public void setCPIndex(int index) {
	    if (istreamIns[0] instanceof ConcreteCPIndexValue) {
		istreamIns[0] = new ConcreteCPIndexValue(index, TypeCodes.UBYTE);
	    } else {
		throw new Error("Tried to set the CP index on an abstract Instruction");
	    }
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a non-wide primitive constant from the contant pool and
     * push it onto the stack.  The index is specified as an unsigned
     * byte after the opcode. The constant may NOT be a String! No
     * resolution is required.
     **/
    public static class LDC_INT_QUICK
	extends IConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC_INT_QUICK;
        public static final LDC_INT_QUICK singleton = new LDC_INT_QUICK();
        public static LDC_INT_QUICK make(int value) {
            LDC_INT_QUICK clone = (LDC_INT_QUICK)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue((int)value, TypeCodes.INT));
            return clone;
        }
        public LDC_INT_QUICK() { super(opcode, true, new IntValue(TypeCodes.INT)); }
	public Object getObjectValue(MethodInformation iv) {
	    return new java.lang.Integer(getValue(iv));
	}
	public String toString(MethodInformation iv) {
	    return "LDC_INT_QUICK " + getObjectValue(iv).toString();
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a non-wide primitive constant from the contant pool and
     * push it onto the stack.  The index is specified as an unsigned
     * byte after the opcode. The constant may NOT be a String! No
     * resolution is required.
     **/
    public static class LDC_FLOAT_QUICK
	extends FConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC_FLOAT_QUICK;
        public static final LDC_FLOAT_QUICK singleton = new LDC_FLOAT_QUICK();
        public LDC_FLOAT_QUICK() { super(opcode, true, new FloatValue()); }
        public LDC_FLOAT_QUICK(float value) { super(opcode, true, new ConcreteFloatValue(value)); }
	public Object getObjectValue(MethodInformation iv) {
	    return new java.lang.Float(getFValue(iv));
	}
	public float getFValue(MethodInformation iv) {
	    return iv.getCode().getFloat(iv.getPC()+1);
	}
    public float getFValue() {
        if (istreamIns[0] instanceof ConcreteFloatValue)
            return ((Float)((ConcreteFloatValue)istreamIns[0]).concreteValue()).floatValue();
        else
            throw new Error("Not concrete");
    }
	public String toString(MethodInformation iv) {
	    return "LDC_FLOAT_QUICK " + getObjectValue(iv).toString();
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a constant from the contant pool and push
     * it onto the stack.
     * The index is specified as a char after
     * the opcode. The constant may be a String!
     **/
    public static class LDC_W 
	extends ConstantPoolLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC_W;
        public static final LDC_W singleton = new LDC_W();
	LDC_W() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(CPIndexValue.CONSTANT_Any,
						TypeCodes.USHORT);
	    this.istreamIns = imm(val);
	    this.stackOuts =
		/* ok, RefValue is not quite right for the general LDC
		   opcode, but it is the right type for quickified
		   OVM IR and it is "close enough" for bytecode */
		stack(new RefValue
		      (new CSACallExp("resolveLDC",
				      new IntValue(new ConversionExp((Value)istreamIns[0])),
				      new RefValue(new CurrentConstantPool()))
		       ));
	}
    LDC_W(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex, TypeCodes.USHORT);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a non-wide primitive constant from the contant pool and push
     * it onto the stack.
     * The index is specified as a char after
     * the opcode.  The constant may NOT be a String! No
     * resolution is required.
     **/
    public static class LDC_W_REF_QUICK
	extends ConstantPoolLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC_W_REF_QUICK;
        public static final LDC_W_REF_QUICK singleton = new LDC_W_REF_QUICK();
	LDC_W_REF_QUICK() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Reference,
						TypeCodes.USHORT);
	    this.istreamIns = imm(val);
	    this.stackOuts = stack(new RefValue(new CPAccessExp(val)));
	}
    LDC_W_REF_QUICK(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex, TypeCodes.USHORT);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a WIDE primitive constant from the contant pool and push
     * it onto the stack.  The index is specified as an unsigned short
     * after the opcode. The constant can not be a String, so one
     * extra check goes away. Thus an extra instruction.
     **/
    public static class LDC2_W 
	extends ConstantPoolLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC2_W;
        public static final LDC2_W singleton = new LDC2_W();
	LDC2_W() {
	    super(opcode);
	    CPIndexValue index = new CPIndexValue();
	    istreamIns = imm(index);
	    this.stackOuts = stack(new LongValue(new CPAccessExp(index, true)));
	}
    LDC2_W(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex, TypeCodes.USHORT);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
     
    /**
     * Load a non-wide primitive constant from the contant pool and
     * push it onto the stack.  The index is specified as an unsigned
     * byte after the opcode. The constant may NOT be a String! No
     * resolution is required.
     **/
    public static class LDC_LONG_QUICK
	extends LConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC_LONG_QUICK;
        public static final LDC_LONG_QUICK singleton = new LDC_LONG_QUICK();
        public LDC_LONG_QUICK() { super(opcode, true, new LongValue()); }
        public LDC_LONG_QUICK(long value) { super(opcode, true, 
                new ConcreteLongValue(value)); }
	public Object getObjectValue(MethodInformation iv) {
	    return new java.lang.Long(getLValue(iv));
	}
	public long getLValue(MethodInformation iv) {
	    return iv.getCode().getLong(iv.getPC()+1);
	}
    public long getLValue() {
        if (istreamIns[0] instanceof ConcreteLongValue)
            return ((Long)((ConcreteLongValue)istreamIns[0]).concreteValue()).longValue();
        else
            throw new Error("Not concrete");
    }
	public String toString(MethodInformation iv) {
	    return "LDC_LONG_QUICK " + getObjectValue(iv).toString();
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
   
    /**
     * Load a non-wide primitive constant from the contant pool and
     * push it onto the stack.  The index is specified as an unsigned
     * byte after the opcode. The constant may NOT be a String! No
     * resolution is required.
     **/
    public static class LDC_DOUBLE_QUICK
	extends DConstantLoad { 
        public static final int opcode = JVMConstants.Opcodes.LDC_DOUBLE_QUICK;
        public static final LDC_DOUBLE_QUICK singleton = new LDC_DOUBLE_QUICK();
        public LDC_DOUBLE_QUICK() { super(opcode, true, new DoubleValue()); }
        public LDC_DOUBLE_QUICK(double value) { super(opcode, true, new ConcreteDoubleValue(value)); }        
	public Object getObjectValue(MethodInformation iv) {
	    return new java.lang.Double(getDValue(iv));
	}
	public double getDValue(MethodInformation iv) {
	    return iv.getCode().getDouble(iv.getPC()+1);
	}
    public double getDValue() {
        if (istreamIns[0] instanceof ConcreteDoubleValue)
            return ((Double)((ConcreteDoubleValue)istreamIns[0]).concreteValue()).doubleValue();
        else
            throw new Error("Not concrete");
    }
	public String toString(MethodInformation iv) {
	    return "LDC_DOUBLE_QUICK " + getObjectValue(iv).toString();
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }


    static abstract class LoadSharedState extends ConstantPoolLoad
	implements ExceptionThrower
    {
	final TypeName.Scalar[] ex = mayThrow(ERROR);
	LoadSharedState(int opcode) { super(opcode); }
	public TypeName.Scalar[] getThrowables() { return ex; }
    }

    /**
     * Load a shared state from the constant pool entry of type
     * CONSTANT_ResolvedStaticField and push it onto the stack.  The
     * index is specified as a char after the opcode. No resolution is
     * required.
     *
     * FIXME: If we call initializeBlueprint, isn't this an ExceptionThrower?
     **/
    public static class LOAD_SHST_FIELD extends LoadSharedState { 
        public static final int opcode = JVMConstants.Opcodes.LOAD_SHST_FIELD;
        public static final LOAD_SHST_FIELD singleton = new LOAD_SHST_FIELD();
	LOAD_SHST_FIELD() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Fieldref,
						TypeCodes.USHORT);
	    this.istreamIns = imm(val);
	    this.stackOuts =
		stack(new RefValue
		      (new CSACallExp
		       ("initializeBlueprint", 
			new RefValue(new CallExp
				     ("GET_CONSTANT_SHST_RESOLVED_STATIC_FIELDREF", 
				      val)))));
	}
    public LOAD_SHST_FIELD(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public ConstantFieldref getConstantFieldref(MethodInformation iv,
						    Constants c) {
	    try {
		char index = iv.getCode().getChar(iv.getPC() + 1);
		return c.getFieldrefAt(index);
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
    public ConstantFieldref getConstantFieldref(Constants cp) {
        return cp.getFieldrefAt(getCPIndex());
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a shared state from the constant pool entry of type
     * CONSTANT_ResolvedStaticMethod and push it onto the stack.  The
     * index is specified as a char after the opcode. No resolution is
     * required.
     **/
    public static class LOAD_SHST_METHOD extends LoadSharedState { 
        public static final int opcode = JVMConstants.Opcodes.LOAD_SHST_METHOD;
        public static final LOAD_SHST_METHOD singleton = new LOAD_SHST_METHOD();
	LOAD_SHST_METHOD() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Methodref,
						TypeCodes.USHORT);
	    this.istreamIns = imm(val);
	    this.stackOuts =
		stack(new RefValue
		      (new CSACallExp("initializeBlueprint",
				      new RefValue(new CallExp
						   ("GET_CONSTANT_SHST_RESOLVED_STATIC_METHODREF", 
						    val)))));
	}
    public LOAD_SHST_METHOD(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public ConstantMethodref getConstantMethodref(MethodInformation iv,
						      Constants c) {
	    try {
		char index = iv.getCode().getChar(iv.getPC() + 1);
		return c.getMethodrefAt(index);
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
	public ConstantMethodref getConstantMethodref(Constants cp) {
	    return cp.getMethodrefAt(getCPIndex());
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }


    /**
     * Load a shared state from the constant pool entry of type
     * CONSTANT_ResolvedStaticField and push it onto the stack.  The
     * index is specified as a char after the opcode. No resolution is
     * required.
     **/
    public static class LOAD_SHST_FIELD_QUICK extends LoadSharedState {
        public static final int opcode = JVMConstants.Opcodes.LOAD_SHST_FIELD_QUICK;
        public static final LOAD_SHST_FIELD_QUICK singleton = new LOAD_SHST_FIELD_QUICK();
	LOAD_SHST_FIELD_QUICK() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Fieldref,
						TypeCodes.USHORT);
	    this.istreamIns = imm(val);
	    this.stackOuts =
		stack(new RefValue
		      (new CallExp("GET_CONSTANT_SHST_RESOLVED_STATIC_FIELDREF", 
				   val)));
	}
    public LOAD_SHST_FIELD_QUICK(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public ConstantFieldref getConstantFieldref(MethodInformation iv,
						    Constants c) {
	    try {
		char index = iv.getCode().getChar(iv.getPC() + 1);
		return c.getFieldrefAt(index);
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
	public ConstantFieldref getConstantFieldref(Constants cp) {
	    return cp.getFieldrefAt(getCPIndex());
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Load a shared state from the constant pool entry of type
     * CONSTANT_ResolvedStaticMethod and push it onto the stack.  The
     * index is specified as a char after the opcode. No resolution is
     * required.
     **/
    public static class LOAD_SHST_METHOD_QUICK extends LoadSharedState { 
        public static final int opcode = JVMConstants.Opcodes.LOAD_SHST_METHOD_QUICK;
        public static final LOAD_SHST_METHOD_QUICK singleton = new LOAD_SHST_METHOD_QUICK();
	LOAD_SHST_METHOD_QUICK() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Methodref,
						TypeCodes.USHORT);
	    this.istreamIns = imm(val);
	    this.stackOuts = 
		stack(new RefValue
		      (new CallExp("GET_CONSTANT_SHST_RESOLVED_STATIC_METHODREF", 
				   val)));
	}
    public LOAD_SHST_METHOD_QUICK(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public ConstantMethodref getConstantMethodref(MethodInformation iv,
						      Constants c) {
	    try {
		char index = iv.getCode().getChar(iv.getPC() + 1);
		return c.getMethodrefAt(index);
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
    public ConstantMethodref getConstantMethodref(Constants cp) {
        return cp.getMethodrefAt(getCPIndex());
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

   
    /**
     * An Ovm specific bytecode which
     * creates a new array. It is a variant of ANEWARRAY and NEWARRAY,
     * but the main and only difference is that the array type is
     * stored in the constant pool entry, unlike them.
     **/
    public static class SINGLEANEWARRAY
	extends ConstantPoolPush implements Allocation {
        public static final int opcode = JVMConstants.Opcodes.SINGLEANEWARRAY;
        public static final SINGLEANEWARRAY singleton = new SINGLEANEWARRAY();
	public SINGLEANEWARRAY() {
	    super(opcode);
	    ex = mayThrow(NEGATIVE_ARRAY_SIZE_EXCEPTION, ERROR);
	    istreamIns = imm(new CPIndexValue(TypeCodes.USHORT));
	    stackIns = stack(new IntValue());
	    stackOuts = stack(new NonnulRefValue());
	}
    public SINGLEANEWARRAY(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public TypeName.Array getArrayName(MethodInformation iv,
					   Constants c) {
	    return getResultType(iv, c).asTypeName().asArray();
	}
	public TypeName.Array getArrayName(Constants cp) {
	    return getArrayName(null, cp);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Create a new array. The type is specified in an index into the constant pool 
     * following the opcode.  The length of the array is given as an integer on
     * the stack. If the length is negative, a NegativeArraySizeException is thrown.
     **/
    public static class ANEWARRAY 
	extends Resolution implements Allocation { 
        public static final int opcode = JVMConstants.Opcodes.ANEWARRAY;
        public static final ANEWARRAY singleton = new ANEWARRAY();
	public ANEWARRAY() {
	    super(opcode, null);
	    ex = mayThrow(NEGATIVE_ARRAY_SIZE_EXCEPTION, ERROR);
	    istreamIns = imm(new CPIndexValue(TypeCodes.USHORT));
	    stackIns = stack(new IntValue());
	    stackOuts = stack(new NonnulRefValue());
	}
	ANEWARRAY(int cpindex) {
	    this();
	    istreamIns[0] = new ConcreteCPIndexValue(cpindex);
	}
	public TypeName.Array getArrayName(MethodInformation mi, Constants cp) {
 	    return getResultType(mi, cp).asTypeName().asArray();
	}
	public ConstantClass getResultType(MethodInformation iv, Constants cp) {
	    ConstantClass inner = getConstantClass(iv, cp);
	    TypeName innerTN = inner.asTypeName();
	    TypeName.Array outerTN = TypeName.Array.make(innerTN, 1);
	    if (inner instanceof Blueprint) {
		Type innerT = ((Blueprint) inner).getType();
		if (innerT.isArray())
		    innerT = ((Type.Array) innerT).getInnermostComponentType();
		Domain d = innerT.getDomain();
		Type outerT = ((S3Domain) d).makeType(innerT, outerTN.getDepth());
		return (S3Blueprint) d.blueprintFor(outerT);
	    } else
		return outerTN;
	}
	public TypeName.Compound getClassName(MethodInformation iv, Constants c) {
	    return getConstantClass(iv, c).asTypeName().asCompound();
	}
	public ConstantClass getConstantClass(MethodInformation iv, Constants cp) {
	    return cp.getClassAt(getCPIndex(iv));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    } // end of ANEWARRAY

    /**
     * Get the length of an array. A reference to an array is on
     * the stack, pop the reference and push the length
     * of the array.
     **/
    public static class ARRAYLENGTH extends ExceptionThrowerImpl { 
        public static final int opcode = JVMConstants.Opcodes.ARRAYLENGTH;
        public static final ARRAYLENGTH singleton = new ARRAYLENGTH();
	public ARRAYLENGTH() {
	    super(opcode, mayThrow(NULL_POINTER_EXCEPTION, ERROR));
	    NonnulArrayRefValue obj = new NonnulArrayRefValue();
	    this.stackIns = stack(obj);
	    this.stackOuts = stack(new IntValue(new ArrayLengthExp(obj)));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Create a new multidimensional array. The type is specified via an
     * index into the constant pool following the opcode.  The number of
     * dimensions follows the type as an unsigned byte. The dimensions can
     * be found in reverse order on the stack. The instruction returns a
     * reference to the array on the stack.
     **/
    public static class MULTIANEWARRAY 
	extends ConstantPoolPush implements Allocation { 
        public static final int opcode = JVMConstants.Opcodes.MULTIANEWARRAY;
        public static final MULTIANEWARRAY singleton = new MULTIANEWARRAY();
	public MULTIANEWARRAY() {
	    super(opcode);
	    ex = mayThrow(NEGATIVE_ARRAY_SIZE_EXCEPTION, ERROR);
	    IntValue dimensions = new IntValue(TypeCodes.UBYTE);
	    istreamIns = imm(new CPIndexValue(CPIndexValue.CONSTANT_Any),
			     dimensions);
	    this.stackIns = stack(new IntValueList(dimensions));
	    this.stackOuts = stack(new NonnulRefValue());
 	}
    public MULTIANEWARRAY(int cpindex, int dimensions) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
        istreamIns[1] = new ConcreteIntValue(dimensions, TypeCodes.UBYTE);
    }
	// FIXME: rename to getArrayName for consistency! -- CG
	public TypeName.Array getClassName(MethodInformation iv,
					   Constants cp) {
	    try {
		return cp.getClassAt(getCPIndex(iv)).asTypeName().asArray();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
    public TypeName.Array getClassName(Constants cp) {
        return cp.getClassAt(getCPIndex()).asTypeName().asArray();
    }
	public int getDimensions(MethodInformation iv) {
	    return iv.getCode().get(iv.getPC() + 3);
	}
    public int getDimensions() {
        if (istreamIns[1] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Create a new array. The type of the array is encoded in the byteCode
     * afte the instruction. The length of the array is popped from the
     * stack. A reference to the array is pushed onto the stack. If the length
     * is negative, a NegativeArraySizeException is thrown.
     **/
    public static class NEWARRAY extends ExceptionThrowerImpl implements Allocation { 
        public static final int opcode = JVMConstants.Opcodes.NEWARRAY;
        public static final NEWARRAY singleton = new NEWARRAY();
	public NEWARRAY() {
	    super(opcode, mayThrow(NEGATIVE_ARRAY_SIZE_EXCEPTION, ERROR));
	    istreamIns = imm(TypeCodes.UBYTE);
	    stackIns = stack(new IntValue());
	    stackOuts = stack(new NonnulRefValue());
	}
	public NEWARRAY(int type) {
	    this();
	    istreamIns[0] = new ConcreteIntValue(type, TypeCodes.UBYTE);
	}
	public byte getPrimitiveType(MethodInformation iv) {
	    return iv.getCode().get(iv.getPC() + 1);
	}
	public TypeName.Array getArrayName(MethodInformation iv) {
	    TypeName.Primitive component = null;
	    // YUCK YUCK YUCK YUCK
	    switch (getPrimitiveType(iv)) {
	    case 4:
		component = TypeName.BOOLEAN;
		break;
	    case 5:
		component = TypeName.CHAR;
		break;
	    case 6:
		component = TypeName.FLOAT;
		break;
	    case 7:
		component = TypeName.DOUBLE;
		break;
	    case 8:
		component = TypeName.BYTE;
		break;
	    case 9:
		component = TypeName.SHORT;
		break;
	    case 10:
		component = TypeName.INT;
		break;
	    case 11:
		component = TypeName.LONG;
		break;
	    default:
		throw new Error("NEWARRAY: Tag " + getPrimitiveType(iv) + " not known/supported!");
	    }
	    return TypeName.Array.make(component, 1);
	}
	public ConstantClass getResultType(MethodInformation iv,
					   Constants c) {
	    TypeName.Array tn = getArrayName(iv);
	    if (c instanceof s3.core.domain.S3Constants)
		try {
		    Type dt = ((s3.core.domain.S3Constants) c).getType();
		    Domain d = dt.getDomain();
		    Type.Context ctx = d.getSystemTypeContext();
		    Type at = ctx.typeFor(tn);
		    return (S3Blueprint) d.blueprintFor(at);
		} catch (LinkageException e) {
		    throw e.fatal("can't find primitive array BP");
		}
	    else
		return tn;
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
   }

    /**
     * Pop a double from the stack and push it back as
     * a float.
     **/
    public static class D2F extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.D2F;
        public static final D2F singleton = new D2F();
	public D2F() { super(opcode, doubleFactory, floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop a double from the stack and push it back as
     * an int.
     *
     * On x86 GCC does not convert NaN, +Infinity, and -Infinity
     * to ints/longs in the same way that Java needs. To avoid
     * troubles I make the check explicitly. An autoconf test
     * might be added to add the whole sequence only if GCC on
     * the platform in use does not work as expected. If it
     * does, the whole block "stackOuts=..." can be removed
     * altogether.
     * 
     * The sequence works by checking whether the number in input
     * is equal to itself; if it isn't then it's Not A Number,
     * and the result is zero. Then +Infinity and -Infinity
     * are checked, and finally, if the floating point value
     * is just a regular number, a plain conversion is used.
     **/
    public static class D2I extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.D2I;
        public static final D2I singleton = new D2I();
        public D2I() {
	    super(opcode, doubleFactory, intFactory);
	    stackOuts = stack(new IntValue(new IfExp(new CondExp(stackIns[0], "==", stackIns[0]),
						     new IntValue(new IfExp(new CondExp(stackIns[0], "==", ConcreteDoubleValue.POSITIVE_INFINITY),
									    ConcreteIntValue.MAX_VALUE,
									    new IntValue(new IfExp(new CondExp(stackIns[0], "==", ConcreteDoubleValue.NEGATIVE_INFINITY),
												   ConcreteIntValue.MIN_VALUE,
												   new IntValue(new ConversionExp(stackIns[0]))
												   ))
									    )),
						     ConcreteIntValue.ZERO)));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop a double from the stack and push it back as
     * a long.
     **/
    public static class D2L extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.D2L;
        public static final D2L singleton = new D2L();
	public D2L() {
	    super(opcode, doubleFactory, longFactory);
	    stackOuts = stack(new LongValue(new IfExp(new CondExp(stackIns[0], "==", stackIns[0]),
						      new LongValue(new IfExp(new CondExp(stackIns[0], "==", ConcreteDoubleValue.POSITIVE_INFINITY),
									      ConcreteLongValue.MAX_VALUE,
									      new LongValue(new IfExp(new CondExp(stackIns[0], "==", ConcreteDoubleValue.NEGATIVE_INFINITY),
												      ConcreteLongValue.MIN_VALUE,
												      new LongValue(new ConversionExp(stackIns[0]))
												      ))
									      )),
						      ConcreteLongValue.ZERO)));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop a float from the stack and push it back as
     * a double.
     **/
    public static class F2D extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.F2D;
        public static final F2D singleton = new F2D();
 	public F2D() { super(opcode, floatFactory, doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop a float from the stack and push it back as
     * a long.
     **/
    public static class F2L extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.F2L;
        public static final F2L singleton = new F2L();
 	public F2L() {
	    super(opcode, floatFactory, longFactory); 
	    stackOuts = stack(new LongValue(new IfExp(new CondExp(stackIns[0], "==", stackIns[0]),
						      new LongValue(new IfExp(new CondExp(stackIns[0], "==", ConcreteFloatValue.POSITIVE_INFINITY),
									      ConcreteLongValue.MAX_VALUE,
									      new LongValue(new IfExp(new CondExp(stackIns[0], "==", ConcreteFloatValue.NEGATIVE_INFINITY),
												      ConcreteLongValue.MIN_VALUE,
												      new LongValue(new ConversionExp(stackIns[0]))
												      ))
									      )),
						      ConcreteLongValue.ZERO)));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop a float from the stack and push it back as
     * an int.
     **/
    public static class F2I extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.F2I;
        public static final F2I singleton = new F2I();
  	public F2I() {
	    super(opcode, floatFactory, intFactory);
	    stackOuts = stack(new IntValue(new IfExp(new CondExp(stackIns[0], "==", stackIns[0]),
						     new IntValue(new IfExp(new CondExp(stackIns[0], "==", ConcreteFloatValue.POSITIVE_INFINITY),
									    ConcreteIntValue.MAX_VALUE,
									    new IntValue(new IfExp(new CondExp(stackIns[0], "==", ConcreteFloatValue.NEGATIVE_INFINITY),
												   ConcreteIntValue.MIN_VALUE,
												   new IntValue(new ConversionExp(stackIns[0]))
												   ))
									    )),
						     ConcreteIntValue.ZERO)));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop an int from the stack and push it back as
     * a byte.
     **/
    public static class I2B extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.I2B;
        public static final I2B singleton = new I2B();
	public I2B() { super(opcode, intFactory, byteFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }

    }
    
    /**
     * Pop an int from the stack and push it back as
     * a char.
     **/
    public static class I2C extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.I2C;
        public static final I2C singleton = new I2C();
 	public I2C() { super(opcode, intFactory, charFactory); }    
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop an int from the stack and push it back as
     * a double.
     **/
    public static class I2D extends Conversion  { 
        public static final int opcode = JVMConstants.Opcodes.I2D;
        public static final I2D singleton = new I2D();
	public I2D() { super(opcode, intFactory, doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop an int from the stack and push it back as
     * a long.
     **/
    public static class I2L extends Conversion  { 
        public static final int opcode = JVMConstants.Opcodes.I2L;
        public static final I2L singleton = new I2L();
 	public I2L() { super(opcode, intFactory, longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
	
    /**
     * Pop an int from the stack and push it back as
     * a float.
     **/
    public static class I2F extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.I2F;
        public static final I2F singleton = new I2F();
 	public I2F() { super(opcode, intFactory, floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop an int from the stack and push it back as
     * a short.
     **/
    public static class I2S extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.I2S;
        public static final I2S singleton = new I2S();
  	public I2S() { super(opcode, intFactory, shortFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop a long from the stack and push it back as
     * an int.
     **/
    public static class L2I extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.L2I;
        public static final L2I singleton = new L2I();
  	public L2I() { super(opcode, longFactory, intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop a long from the stack and push it back as
     * a double.
     **/
    public static class L2D extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.L2D;
        public static final L2D singleton = new L2D();
 	public L2D() { super(opcode, longFactory, doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop a long from the stack and push it back as
     * a float.
     **/
    public static class L2F extends Conversion { 
        public static final int opcode = JVMConstants.Opcodes.L2F;
        public static final L2F singleton = new L2F();
 	public L2F() { super(opcode, longFactory, floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Get the value of a field. A reference to the object is on the stack.
     * If it is null, a NullPointerException is thrown.
     * The name of the field (and the class, and the type)
     * are given by an index into the constant pool following
     * the opcode. The value of the field is pushed onto the
     * stack.
     **/
    public static class GETFIELD 
	extends FieldAccess { 
        public static final int opcode = JVMConstants.Opcodes.GETFIELD;
        public static final GETFIELD singleton = new GETFIELD();
    	public GETFIELD() {
	    super(opcode, instanceAccessErrors);
	    stackIns   = stack(new NonnulRefValue());
	    stackOuts  = stack(new Value());
	    istreamIns = imm(new CPIndexValue(CPIndexValue.CONSTANT_Any));
	}
    public GETFIELD(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Get the value of a static field.
     * The name of the field (and the class, and the type)
     * are given by an index into the constant pool following
     * the opcode. The value of the field is pushed onto the
     * stack.
     **/
    public static class GETSTATIC 
	extends FieldAccess { 
        public static final int opcode = JVMConstants.Opcodes.GETSTATIC;
        public static final GETSTATIC singleton = new GETSTATIC();
	public GETSTATIC() {
	    super(opcode, staticAccessErrors);
	    stackOuts = stack(new Value());
	    istreamIns = imm(new CPIndexValue(CPIndexValue.CONSTANT_Any));
	}
	public GETSTATIC(int cpindex) {
	    this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Set the value of a field. On the top of the stack
     * is the value, a reference to the object is below.
     * If it is null, a NullPointerException is thrown.
     * The name of the field (and the class, and the type)
     * are given by an index into the constant pool following
     * the opcode.
     **/
    public static class PUTFIELD 
	extends FieldAccess { 
        public static final int opcode = JVMConstants.Opcodes.PUTFIELD;
        public static final PUTFIELD singleton = new PUTFIELD();
	public PUTFIELD() {
        this(opcode, instanceAccessErrors);
    }
    protected PUTFIELD(int oc, TypeName.Scalar[] ex) {
	    super(oc, ex);
	    this.stackIns = new Value[] { new Value(), new NonnulRefValue()};
	    istreamIns = imm(new CPIndexValue(CPIndexValue.CONSTANT_Any));
	}
    public PUTFIELD(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Set the value of a field. On the top of the stack
     * is the value, a reference to the object is below.
     * If it is null, a NullPointerException is thrown.
     * The name of the field (and the class, and the type)
     * are given by an index into the constant pool following
     * the opcode.
     **/
    public static class PUTFIELD_WITH_BARRIER_REF
	extends PUTFIELD { 
        public static final int opcode = JVMConstants.Opcodes.PUTFIELD_WITH_BARRIER_REF;
        public static final PUTFIELD_WITH_BARRIER_REF singleton = new PUTFIELD_WITH_BARRIER_REF();
	public PUTFIELD_WITH_BARRIER_REF() {
	    super(opcode, instanceAccessErrors);
	}
    public PUTFIELD_WITH_BARRIER_REF(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Set the value of a static field. On the top of the stack
     * is the value.
     * The name of the field (and the class, and the type)
     * are given by an index into the constant pool following
     * the opcode.
     **/
    public static class PUTSTATIC 
	extends FieldAccess { 
        public static final int opcode = JVMConstants.Opcodes.PUTSTATIC;
        public static final PUTSTATIC singleton = new PUTSTATIC();
	public PUTSTATIC() {
	    this(opcode, staticAccessErrors);
	}
	protected PUTSTATIC(int oc, TypeName.Scalar[] ex) {
	    super(oc, ex);
	    this.stackIns = new Value[] { new Value()};
	    istreamIns = imm(new CPIndexValue(CPIndexValue.CONSTANT_Any));
	}
    public PUTSTATIC(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Set the value of a static field. On the top of the stack
     * is the value.
     * The name of the field (and the class, and the type)
     * are given by an index into the constant pool following
     * the opcode.
     **/
    public static class PUTSTATIC_WITH_BARRIER_REF
 	extends PUTSTATIC { 
	public static final int opcode = JVMConstants.Opcodes.PUTSTATIC_WITH_BARRIER_REF;
	public static final PUTSTATIC_WITH_BARRIER_REF singleton = new PUTSTATIC_WITH_BARRIER_REF();
	public PUTSTATIC_WITH_BARRIER_REF() {
	    // FIXME: missing MemoryAccessError
	    super(opcode, staticAccessErrors);
	}
    public PUTSTATIC_WITH_BARRIER_REF(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    
    /**
     * Stack manipulation: <br>
     * before: <bf>... a</bf> <br>
     * after: <bf>... a a</bf><br>
     **/
    public static class DUP extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.DUP;
        public static final DUP singleton = new DUP();
	public DUP() {
	    super(opcode);
	    this.stackIns = new Value[] {new Value()};
	    this.stackOuts = new Value[] {this.stackIns[0], this.stackIns[0]};   
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Stack manipulation: <br>
     * before: <bf>... b a</bf> <br>
     * after: <bf>... a b a</bf><br>
     **/
    public static class DUP_X1 extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.DUP_X1;
        public static final DUP_X1 singleton = new DUP_X1();
	public DUP_X1() {
	    super(opcode);
	    this.stackIns = new Value[] {new Value(), new Value()};
	    this.stackOuts = new Value[] {this.stackIns[0], this.stackIns[1], this.stackIns[0]};    
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Stack manipulation: <br>
     * before: <bf>... c b a</bf> <br>
     * after: <bf>... a c b a</bf><br>
     **/
    public static class DUP_X2 extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.DUP_X2;
        public static final DUP_X2 singleton = new DUP_X2();
	public DUP_X2() {
	    super(opcode);
 	this.stackIns = new Value[] {new Value(), new Value(), new Value()};
	this.stackOuts = new Value[] {this.stackIns[0], this.stackIns[1], 
				 this.stackIns[2], this.stackIns[0]};
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Stack manipulation: <br>
     * before: <bf>... b a</bf> <br>
     * after: <bf>... b a b a</bf><br>
     **/
    public static class DUP2 extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.DUP2;
        public static final DUP2 singleton = new DUP2();
	public DUP2() {
	    super(opcode);
	    this.stackIns = new Value[] {new Value(), new Value() };
	    this.stackOuts = new Value[] {this.stackIns[0], this.stackIns[1], 
				 this.stackIns[0], this.stackIns[1]};	    
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Stack manipulation: <br>
     * before: <bf>... c b a</bf> <br>
     * after: <bf>... b a c b a</bf><br>
     **/
    public static class DUP2_X1 extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.DUP2_X1;
        public static final DUP2_X1 singleton = new DUP2_X1();
	public DUP2_X1() {
	    super(opcode);
	    this.stackIns = new Value[] {new Value(), new Value(), new Value() };
	    this.stackOuts = new Value[] {this.stackIns[0], this.stackIns[1], 
				     this.stackIns[2], this.stackIns[0], this.stackIns[1]};	    
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Stack manipulation: <br>
     * before: <bf>... d c b a</bf> <br>
     * after: <bf>... b a d c b a</bf><br>
     **/
    public static class DUP2_X2 extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.DUP2_X2;
        public static final DUP2_X2 singleton = new DUP2_X2();
 	public DUP2_X2() {
	    super(opcode);
	    this.stackIns = new Value[] {new Value(), new Value(), 
				    new Value(), new Value()};
	    this.stackOuts = new Value[] {this.stackIns[0], this.stackIns[1], 
				     this.stackIns[2], this.stackIns[3], 
				     this.stackIns[0], this.stackIns[1]};
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Stack manipulation: <br>
     * before: <bf>... a</bf> <br>
     * after: <bf>...</bf><br>
     **/
    public static class POP extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.POP;
        public static final POP singleton = new POP();
        public static POP make() {
            return singleton;
        }
	public POP() {
	    super(opcode);
	    this.stackIns = new Value[] {new Value()};
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Stack manipulation: <br>
     * before: <bf>... a b</bf> <br>
     * after: <bf>...</bf><br>
     **/
    public static class POP2 extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.POP2;
        public static final POP2 singleton = new POP2();
        public static POP2 make() {
            return singleton;
        }
 	public POP2() {
	    super(opcode);
	    stackIns = new Value[] {new Value(), new Value()};
	}     
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Stack manipulation: <br>
     * before: <bf>... b a</bf> <br>
     * after: <bf>... a b</bf><br>
     **/
    public static class SWAP extends StackManipulation { 
        public static final int opcode = JVMConstants.Opcodes.SWAP;
        public static final SWAP singleton = new SWAP();
	public SWAP() {
	    super(opcode);
	    stackIns = new Value[] {new Value(), new Value()};
	    stackOuts = new Value[] {stackIns[1], stackIns[0]};
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }



    /**
     * Pop two doubles from the stack and push the sum back
     **/
    public static class DADD extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.DADD;
        public static final DADD singleton = new DADD();
	public DADD(){ super(opcode, "+", doubleFactory);	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two doubles from the stack and push the difference back
     **/
    public static class DSUB extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.DSUB;
        public static final DSUB singleton = new DSUB();
 	public DSUB(){ super(opcode, "-", doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
   }

    /**
     * Pop two doubles from the stack and push the product back
     **/
    public static class DMUL extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.DMUL;
        public static final DMUL singleton = new DMUL();
 	public DMUL(){ super(opcode, "*", doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
   }

    /**
     * Pop two doubles from the stack and push the division result back
     **/
    public static class DDIV extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.DDIV;
        public static final DDIV singleton = new DDIV();
  	public DDIV(){ super(opcode, "/", doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two doubles from the stack and push the reminder back
     **/
    public static class DREM extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.DREM;
        public static final DREM singleton = new DREM();
   	public DREM(){
	    super(opcode, "%", 
	      doubleFactory);// FIXME no such operation in C?
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two longs from the stack and push the sum back
     **/
    public static class LADD extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.LADD;
        public static final LADD singleton = new LADD();
   	public LADD(){ super(opcode, "+", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
     }

    /**
     * Pop two longs from the stack and push the difference back
     **/
    public static class LSUB extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.LSUB;
        public static final LSUB singleton = new LSUB();
   	public LSUB(){ super(opcode, "-", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two longs from the stack and push the product back
     **/
    public static class LMUL extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.LMUL;
        public static final LMUL singleton = new LMUL();
    	public LMUL(){ super(opcode, "*", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
   }

    /**
     * Pop two longs from the stack and push the division result back
     **/
    public static class LDIV extends IntegerDivision { 
        public static final int opcode = JVMConstants.Opcodes.LDIV;
        public static final LDIV singleton = new LDIV();
	public LDIV() { super(opcode, "/", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two longs from the stack and push the reminder back
     **/
    public static class LREM extends IntegerDivision { 
        public static final int opcode = JVMConstants.Opcodes.LREM;
        public static final LREM singleton = new LREM();
	public LREM() { super(opcode, "%", 
	  longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two longs from the stack and or them
     **/
    public static class LOR extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.LOR;
        public static final LOR singleton = new LOR();
    	public LOR(){ super(opcode, "|", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two longs from the stack and "and" them.
     **/
    public static class LAND extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.LAND;
        public static final LAND singleton = new LAND();
   	public LAND(){ super(opcode, "&", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
     }

    /**
     * Pop two longs from the stack and xor them
     **/
    public static class LXOR extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.LXOR;
        public static final LXOR singleton = new LXOR();
    	public LXOR(){ super(opcode, "^", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two ints from the stack and push the sum back
     **/
    public static class IADD extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.IADD;
        public static final IADD singleton = new IADD();
        public static IADD make() { return singleton; }
      	public IADD(){ super(opcode, "+", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two ints from the stack and push the difference back
     **/
    public static class ISUB extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.ISUB;
        public static final ISUB singleton = new ISUB();
      	public ISUB(){ super(opcode, "-", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
     }

    /**
     * Pop two ints from the stack and push the product back
     **/
    public static class IMUL extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.IMUL;
        public static final IMUL singleton = new IMUL();
     	public IMUL(){ super(opcode, "*", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
      }

    /**
     * Pop two ints from the stack and push the division result back
     **/
    public static class IDIV extends IntegerDivision { 
        public static final int opcode = JVMConstants.Opcodes.IDIV;
        public static final IDIV singleton = new IDIV();
	public IDIV() { super(opcode, "/", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two ints from the stack and push the reminder back
     **/
    public static class IREM extends IntegerDivision { 
        public static final int opcode = JVMConstants.Opcodes.IREM;
        public static final IREM singleton = new IREM();
	public IREM() { super(opcode, "%", 
	  intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two ints from the stack and or them
     **/
    public static class IOR extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.IOR;
        public static final IOR singleton = new IOR();
    	public IOR(){ super(opcode, "|", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two ints from the stack and "and" them.
     **/
    public static class IAND extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.IAND;
        public static final IAND singleton = new IAND();
    	public IAND(){ super(opcode, "&", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two ints from the stack and xor them
     **/
    public static class IXOR extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.IXOR;
        public static final IXOR singleton = new IXOR();
    	public IXOR(){ super(opcode, "^", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two floats from the stack and push the sum back
     **/
    public static class FADD extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.FADD;
        public static final FADD singleton = new FADD();
     	public FADD(){ super(opcode, "+", floatFactory); }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two floats from the stack and push the difference back
     **/
    public static class FSUB extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.FSUB;
        public static final FSUB singleton = new FSUB();
    	public FSUB(){ super(opcode, "-", floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
     }
    
    /**
     * Pop two floats from the stack and push the product back
     **/
    public static class FMUL extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.FMUL;
        public static final FMUL singleton = new FMUL();
    	public FMUL(){ super(opcode, "*", floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
     }

    /**
     * Pop two floats from the stack and push the division result back
     **/
    public static class FDIV extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.FDIV;
        public static final FDIV singleton = new FDIV();
    	public FDIV(){ super(opcode, "/", floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
     }

    /**
     * Pop two floats from the stack and push the reminder back
     **/
    public static class FREM extends BinOp { 
        public static final int opcode = JVMConstants.Opcodes.FREM;
        public static final FREM singleton = new FREM();
     	public FREM(){ super(opcode, "%", 
     	  floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two floats from the stack and push an int back
     **/
    public static class FCMPG extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.FCMPG;
        public static final FCMPG singleton = new FCMPG();
 	public FCMPG() {
	    super(opcode);
	    stackIns = new Value[] {new FloatValue(), new FloatValue()};
	    IfExp exp = buildCMPG(stackIns[1],  stackIns[0]);
	    stackOuts = new Value[] {new IntValue(exp)};
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop two floats from the stack and push an int back
     **/
    public static class FCMPL extends Instruction  { 
        public static final int opcode = JVMConstants.Opcodes.FCMPL;
        public static final FCMPL singleton = new FCMPL();
	public FCMPL() {
	    super(opcode);
	    stackIns = new Value[] {new FloatValue(), new FloatValue()};
	    IfExp exp = buildCMP(stackIns[1],  stackIns[0]);
	    stackOuts = new Value[] {new IntValue(exp)};
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop value stack and push the negative value back
     **/
    public static class FNEG extends Negation { 
        public static final int opcode = JVMConstants.Opcodes.FNEG;
        public static final FNEG singleton = new FNEG();
 	public FNEG() { super(opcode, floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
   }

    /**
     * Pop value stack and push the negative value back
     **/
    public static class INEG extends Negation { 
        public static final int opcode = JVMConstants.Opcodes.INEG;
        public static final INEG singleton = new INEG();
	public INEG() { super(opcode, intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop value stack and push the negative value back
     **/
    public static class DNEG extends Negation { 
        public static final int opcode = JVMConstants.Opcodes.DNEG;
        public static final DNEG singleton = new DNEG();
	public DNEG() { super(opcode, doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Pop value stack and push the negative value back
     **/
    public static class LNEG extends Negation { 
        public static final int opcode = JVMConstants.Opcodes.LNEG;
        public static final LNEG singleton = new LNEG();
	public LNEG() { super(opcode, longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * add constant to local (int) value
     **/
    public static class IINC extends LocalAccess { 
        public static final int opcode = JVMConstants.Opcodes.IINC;
        public static final IINC singleton = new IINC();
        public static IINC make(int index, int delta) {
            IINC clone = (IINC)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.UBYTE), 
                    new ConcreteIntValue(delta, TypeCodes.BYTE));
            return clone;
        }
	public IINC() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.UBYTE, TypeCodes.BYTE);
	    offset = (IntValue) istreamIns[0];

	    IntValue orig = new IntValue(new LocalExp(offset));
	    BinExp sum = new BinExp(orig, "+", (Value) istreamIns[1]);

	    evals = eval(new LocalStore(offset, new IntValue(sum)));
	}

	protected IINC(int opcode) {
	    super(opcode);
	}

    public char getTypeCode() { return TypeCodes.INT; }
    
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        byte delta = NumberRanges.
            checkByte(((ConcreteIntValue)istreamIns[1]).intValue());
        return WIDE_IINC.make(localIndex, delta);
    }
	/**
	 * Return delta
	 **/
	public int getValue(MethodInformation iv) {
	    return iv.getCode().get(iv.getPC() + 2);
	}
    public int getValue() {
        if (istreamIns[1] instanceof ConcreteIntValue) {
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        } else 
            throw new Error("Not concrete");
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }

	public String toString(MethodInformation iv) {
	    return getName() + " " + getLocalVariableOffset(iv) + ", " 
		+ getValue(iv);
	}

	public String toString() {
	    return (isPrototype()
		    ? getName()
		    : (getName() + " " + getLocalVariableOffset()
		       + getValue()));
	}
     }

    /**
     * pop 2 doubles from the stack, compare, push result of
     * comparison (-1,0,1, Integer) back on the stack.
     **/
    public static class DCMPG extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.DCMPG;
        public static final DCMPG singleton = new DCMPG();
	public DCMPG() {
	    super(opcode);
	    DoubleValue in0 = new DoubleValue();
	    DoubleValue in1 = new DoubleValue();
	    stackIns = new Value[]  { in0, in0.getSecondHalf(),
				      in1, in1.getSecondHalf()};
	    IfExp exp = buildCMPG(stackIns[2], stackIns[0]);
	    stackOuts = new Value[] { new IntValue(exp) };
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop 2 doubles from the stack, compare, push result of
     * comparison (-1,0,1, Integer) back on the stack.
     **/
    public static class DCMPL extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.DCMPL;
        public static final DCMPL singleton = new DCMPL();
	public DCMPL() {
	    super(opcode);
	    DoubleValue in0 = new DoubleValue();
	    DoubleValue in1 = new DoubleValue();
	    stackIns = new Value[]  { in0, in0.getSecondHalf(),
				      in1, in1.getSecondHalf()};
	    IfExp exp = buildCMP(stackIns[2], stackIns[0]);
	    stackOuts = new Value[] { new IntValue(exp) };
	}   
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * pop 2 longs from the stack, compare, push result of
     * comparison (-1,0,1, Integer) back on the stack.
     **/
    public static class LCMP extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.LCMP;
        public static final LCMP singleton = new LCMP();
 	public LCMP() {
	    super(opcode);
	    LongValue in0 = new LongValue();
	    LongValue in1 = new LongValue();
	    stackIns = new Value[] {in0, in0.getSecondHalf(),
				    in1, in1.getSecondHalf()};
	    IfExp exp = buildCMP(stackIns[2],  stackIns[0]);
	    stackOuts = new Value[] {new IntValue(exp)};
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class ShiftOp extends AsymetricBinOp { 
	//    public static class ShiftOp extends Instruction { public static final class singleton = new class();
	ShiftOp(int opcode, String op, ValueFactory lhs, ValueFactory ret) {
	    super(opcode);
	    stackIns = stack(new IntValue(), lhs.make());
	    Value masked
		= new IntValue(new ShiftMaskExp(stackIns[0], stackIns[1]));
	    stackOuts = stack(ret.make(new BinExp(stackIns[1], op, masked)));
	}
	ShiftOp(int opcode, String op, ValueFactory lhs) {
	    this(opcode, op, lhs, lhs);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }	
    }
		      
    public static class ISHL extends ShiftOp { 
        public static final int opcode = JVMConstants.Opcodes.ISHL;
        public static final ISHL singleton = new ISHL();
	public ISHL() { super(opcode, "<<", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }	
    }
    public static class ISHR extends ShiftOp { 
        public static final int opcode = JVMConstants.Opcodes.ISHR;
        public static final ISHR singleton = new ISHR();
 	public ISHR() { super(opcode, ">>", intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }	
    }
    public static class IUSHR extends ShiftOp { 
        public static final int opcode = JVMConstants.Opcodes.IUSHR;
        public static final IUSHR singleton = new IUSHR();
	public IUSHR() { super(opcode, ">>", uintFactory, intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }	
     }
    public static class LUSHR extends ShiftOp { 
        public static final int opcode = JVMConstants.Opcodes.LUSHR;
        public static final LUSHR singleton = new LUSHR();
 	public LUSHR() { super(opcode, ">>", ulongFactory, longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }	
     }
    public static class LSHR extends ShiftOp { 
        public static final int opcode = JVMConstants.Opcodes.LSHR;
        public static final LSHR singleton = new LSHR();
 	public LSHR() { super(opcode, ">>", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }	
    }
    public static class LSHL extends ShiftOp { 
        public static final int opcode = JVMConstants.Opcodes.LSHL;
        public static final LSHL singleton = new LSHL();
	public LSHL() { super(opcode, "<<", longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }	
    }

	
    /* *********************** Jumps ************************* **/

    public abstract static class GotoInstruction
	extends UnconditionalJump {
	public GotoInstruction(int opcode) {
	    super(opcode);
	}
    public InstructionHandle getTargetHandle() {
        return (InstructionHandle)istreamIns[0];
    }
    public void setTargetHandle(InstructionHandle ih) {
	((InstructionHandle)istreamIns[0]).removeTargeter(this);
	ih.addTargeter(this);
        istreamIns[0] = ih;
    }
    }

    /**
     * Branch always
     **/
    public static class GOTO 
	extends GotoInstruction { 
        public static final int opcode = JVMConstants.Opcodes.GOTO;
        public static final GOTO singleton = new GOTO();
        public static GOTO make(InstructionHandle target) {
            GOTO clone = (GOTO)singleton.clone();
            clone.istreamIns = imm(target);
            target.addTargeter(clone);
            return clone;
        }

	public GOTO() {
	    super(opcode);
	    PCValue target = new PCValue(TypeCodes.SHORT);
	    istreamIns = imm(target);
        jumpTarget = target;
 	}

    public boolean isBranchOffsetShort() { return true; }

    public boolean doesNeedWidening() {
        int branchOffset = getTarget();
        return ! NumberRanges.isShort(branchOffset);
    }
    public Instruction widen() {
        return GOTO_W.make(getTargetHandle());
    }
  	/**
	 * What is the target of the jump?
	 **/
	public int getTarget(MethodInformation iv) {
	    return iv.getCode().getShort(iv.getPC() + 1);
	}
    public int getTarget() {
        if (istreamIns[0] instanceof ConcretePCValue)
            return ((ConcretePCValue)istreamIns[0]).intValue();
        else
            throw new Error("Not concrete");
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch always (wideindex)
     **/
    public static class GOTO_W 
	extends GotoInstruction { 
        public static final int opcode = JVMConstants.Opcodes.GOTO_W;
        public static final GOTO_W singleton = new GOTO_W();
        public static GOTO_W make(InstructionHandle target) {
            GOTO_W clone = (GOTO_W)singleton.clone();
            clone.istreamIns = imm(target);
            target.addTargeter(clone);
            return clone;
        }
	public GOTO_W() {
	    super(opcode);
	    PCValue target = new PCValue();
	    istreamIns = imm(target);
        jumpTarget = target;
	}
       public boolean isBranchOffsetShort() { return false; }
  	/**
	 * What is the target of the jump?
	 **/
	public int getTarget(MethodInformation iv) {
	    return iv.getCode().getInt(iv.getPC() + 1);
	}
    public int getTarget() {
        if (istreamIns[0] instanceof ConcretePCValue)
            return ((ConcretePCValue)istreamIns[0]).intValue();
        else
            throw new Error("Not concrete");
    }

    public InstructionHandle getTargetHandle() {
        return (InstructionHandle)istreamIns[0];
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if reference comparison succeeds
     * <ul>
     * <li> eq succeeds if and only if reference1 == reference2
     * <li> ne succeeds if and only if reference1 != reference2
     * </ul>
     **/
    public static class IF_ACMPEQ extends IfCmp { 
        public static final int opcode = JVMConstants.Opcodes.IF_ACMPEQ;
        public static final IF_ACMPEQ singleton = new IF_ACMPEQ();
	public IF_ACMPEQ() { super(opcode, "==", refFactory); }
    public IF_ACMPEQ(int relOffset) { super(opcode, "==", refFactory, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if reference comparison succeeds
     * <ul>
     * <li> eq succeeds if and only if reference1 == reference2
     * <li> ne succeeds if and only if reference1 != reference2
     * </ul>
     **/
    public static class IF_ACMPNE extends IfCmp { 
        public static final int opcode = JVMConstants.Opcodes.IF_ACMPNE;
        public static final IF_ACMPNE singleton = new IF_ACMPNE();
        public IF_ACMPNE() { super(opcode, "!=", refFactory); }
        public IF_ACMPNE(int relOffset) { super(opcode, "!=", refFactory, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison succeeds
     * <ul>
     * <li> eq succeeds if and only if value1 == value2
     * </ul>
     **/
    public static class IF_ICMPEQ extends IfCmp { 
        public static final int opcode = JVMConstants.Opcodes.IF_ICMPEQ;
        public static final IF_ICMPEQ singleton = new IF_ICMPEQ();
        public IF_ICMPEQ() { super(opcode, "==", intFactory); }
        public IF_ICMPEQ(int relOffset) { super(opcode, "==", intFactory, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison succeeds
     * <ul>
     * <li> ne succeeds if and only if value1 != value2
     * </ul>
     **/
    public static class IF_ICMPNE extends IfCmp { 
        public static final int opcode = JVMConstants.Opcodes.IF_ICMPNE;
        public static final IF_ICMPNE singleton = new IF_ICMPNE();
        public IF_ICMPNE() { super(opcode, "!=", intFactory); }
        public IF_ICMPNE(int relOffset) { super(opcode, "!=", intFactory, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison succeeds
     * <ul>
     * <li> lt succeeds if and only if value1 <  value2
     * </ul>
     **/
    public static class IF_ICMPLT extends IfCmp { 
        public static final int opcode = JVMConstants.Opcodes.IF_ICMPLT;
        public static final IF_ICMPLT singleton = new IF_ICMPLT();
        public IF_ICMPLT() { super(opcode, "<", intFactory); }
        public IF_ICMPLT(int relOffset) { super(opcode, "<", intFactory, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison succeeds
     * <ul>
     * <li> le succeeds if and only if value1 <= value2
     * </ul>
     **/
    public static class IF_ICMPLE extends IfCmp { 
        public static final int opcode = JVMConstants.Opcodes.IF_ICMPLE;
        public static final IF_ICMPLE singleton = new IF_ICMPLE();
        public IF_ICMPLE() { super(opcode, "<=", intFactory); }
        public IF_ICMPLE(int relOffset) { super(opcode, "<=", intFactory, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison succeeds
     * <ul>
     * <li> gt succeeds if and only if value1 >  value2
     * </ul>
     **/
    public static class IF_ICMPGT extends IfCmp { 
        public static final int opcode = JVMConstants.Opcodes.IF_ICMPGT;
        public static final IF_ICMPGT singleton = new IF_ICMPGT();
        public IF_ICMPGT() { super(opcode, ">", intFactory); }
        public IF_ICMPGT(int relOffset) { super(opcode, ">", intFactory, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison succeeds
     * <ul>
     * <li> ge succeeds if and only if value1 >= value2
     * </ul>
     **/
    public static class IF_ICMPGE extends IfCmp { 
        public static final int opcode = JVMConstants.Opcodes.IF_ICMPGE;
        public static final IF_ICMPGE singleton = new IF_ICMPGE();
        public IF_ICMPGE() { super(opcode, ">=", intFactory); }
        public IF_ICMPGE(int relOffset) { super(opcode, ">=", intFactory, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison with zero succeeds.
     **/
    public static class IFEQ extends IfZ { 
        public static final int opcode = JVMConstants.Opcodes.IFEQ;
        public static final IFEQ singleton = new IFEQ();
        public IFEQ() { super(opcode, "==", new IntValue(), ConcreteIntValue.ZERO); }
        public IFEQ(int relOffset) { super(opcode, "==", new IntValue(), ConcreteIntValue.ZERO, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison with zero fails.
     **/
    public static class IFNE extends IfZ { 
        public static final int opcode = JVMConstants.Opcodes.IFNE;
        public static final IFNE singleton = new IFNE();
        public IFNE() { super(opcode, "!=", new IntValue(), ConcreteIntValue.ZERO); }
        public IFNE(int relOffset) { super(opcode, "!=", new IntValue(), ConcreteIntValue.ZERO, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison with zero succeeds.
     * <li> lt succeeds if and only if value <  0
     **/
    public static class IFLT extends IfZ { 
        public static final int opcode = JVMConstants.Opcodes.IFLT;
        public static final IFLT singleton = new IFLT();
        public IFLT() { super(opcode, "<", new IntValue(), ConcreteIntValue.ZERO); }
        public IFLT(int relOffset) { super(opcode, "<", new IntValue(), ConcreteIntValue.ZERO, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison with zero succeeds.
     * <ul>
     * <li> le succeeds if and only if value <= 0
     * </ul>
     **/
    public static class IFLE extends IfZ { 
        public static final int opcode = JVMConstants.Opcodes.IFLE;
        public static final IFLE singleton = new IFLE();
        public IFLE() { super(opcode, "<=", new IntValue(), ConcreteIntValue.ZERO); }
        public IFLE(int relOffset) { super(opcode, "<=", new IntValue(), ConcreteIntValue.ZERO, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison with zero succeeds.
     * <ul>
     * <li> gt succeeds if and only if value >  0
     * </ul>
     **/
    public static class IFGT extends IfZ { 
        public static final int opcode = JVMConstants.Opcodes.IFGT;
        public static final IFGT singleton = new IFGT();
        public IFGT() { super(opcode, ">", new IntValue(), ConcreteIntValue.ZERO); }
        public IFGT(int relOffset) { super(opcode, ">", new IntValue(), ConcreteIntValue.ZERO, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if int comparison with zero succeeds.
     * <ul>
     * <li> ge succeeds if and only if value >= 0
     * </ul>
     **/
    public static class IFGE extends IfZ { 
        public static final int opcode = JVMConstants.Opcodes.IFGE;
        public static final IFGE singleton = new IFGE();
        public IFGE() { super(opcode, ">=", new IntValue(), ConcreteIntValue.ZERO); }
        public IFGE(int relOffset) { super(opcode, ">=", new IntValue(), ConcreteIntValue.ZERO, relOffset); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if reference is not null
     **/
    public static class IFNONNULL extends IfZ { 
        public static final int opcode = JVMConstants.Opcodes.IFNONNULL;
        public static final IFNONNULL singleton = new IFNONNULL();
	public IFNONNULL() {
	    super(opcode, "!=", new RefValue(), NullRefValue.INSTANCE);
	}
    public IFNONNULL(int relOffset) {
        super(opcode, "!=", new RefValue(), NullRefValue.INSTANCE, relOffset);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Branch if reference is null
     **/
    public static class IFNULL extends IfZ { 
        public static final int opcode = JVMConstants.Opcodes.IFNULL;
        public static final IFNULL singleton = new IFNULL();
	public IFNULL() {
	    super(opcode, "==", new RefValue(), NullRefValue.INSTANCE);
	}
    public IFNULL(int relOffset) {
        super(opcode, "==", new RefValue(), NullRefValue.INSTANCE, relOffset);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public abstract static class JsrInstruction
	extends UnconditionalJump {
	public JsrInstruction(int opcode) {
	    super(opcode);
	}
    }

    /**
     * Jump subroutine
     **/
    public static class JSR 
	extends JsrInstruction { 
        public static final int opcode = JVMConstants.Opcodes.JSR;
        public static final JSR singleton = new JSR();
        public static JSR make(InstructionHandle target) {
            JSR clone = (JSR)singleton.clone();
            clone.istreamIns = imm(target);
            target.addTargeter(clone);
            return clone;
        }
	public JSR() {
	    super(opcode);
	    PCValue t = new PCValue(TypeCodes.SHORT);
	    istreamIns = imm(t);
	    stackOuts = new Value[] { new PCValue(new CurrentPC(), false)};
	    jumpTarget = t;
	}
    public boolean isBranchOffsetShort() { return true; }

    public boolean doesNeedWidening() {
        int branchOffset = getTarget();
        return ! NumberRanges.isShort(branchOffset);
    }
    public Instruction widen() {
        return JSR_W.make(getTargetHandle());
    }
    public InstructionHandle getTargetHandle() {
        return (InstructionHandle)istreamIns[0];
    }
	public int getTarget(MethodInformation iv) {
	    return iv.getCode().getShort(iv.getPC() + 1);
	}
    public int getTarget() {
        if (istreamIns[0] instanceof ConcretePCValue)
            return ((ConcretePCValue)istreamIns[0]).intValue();
        else
            throw new Error("Not concrete");
    }


	public void accept(Instruction.Visitor v) { v.visit(this); }
   }

    /**
     * Jump subroutine (wide index)
     **/
    public static class JSR_W
	extends JsrInstruction { 
        public static final int opcode = JVMConstants.Opcodes.JSR_W;
        public static final JSR_W singleton = new JSR_W();
        public static JSR_W make(InstructionHandle target) {
            JSR_W clone = (JSR_W)singleton.clone();
            clone.istreamIns = imm(target);
            target.addTargeter(clone);
            return clone;
        }
	public JSR_W() {
	    super(opcode);
	    PCValue t = new PCValue();
	    istreamIns = imm(t);
	    stackOuts = new Value[] { new PCValue(new CurrentPC(), false)}; 
	    jumpTarget = t;
	}
       public boolean isBranchOffsetShort() { return false; }
    public InstructionHandle getTargetHandle() {
        return (InstructionHandle)istreamIns[0];
    }
 	public int getTarget(MethodInformation iv) {
	    return iv.getCode().getInt(iv.getPC() + 1);
	}
    public int getTarget() {
        if (istreamIns[0] instanceof ConcretePCValue)
            return ((ConcretePCValue)istreamIns[0]).intValue();
        else
            throw new Error("Not concrete");
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }

    }

    /**
     * Returns from subroutine
     **/
    public static class RET extends LocalAccess
	implements FlowChange
    { 
        public static final int opcode = JVMConstants.Opcodes.RET;
        public static final RET singleton = new RET();
        public static RET make(char index) {
            RET clone = (RET)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.UBYTE));
            return clone;
        }
	PCValue jumpTarget;
	public RET() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.UBYTE);
	    offset = (IntValue) istreamIns[0];
	    jumpTarget = new PCValue(new LocalExp(offset), false);
	}
    public RET(int index) {
        this();
        istreamIns[0] = new ConcreteIntValue(index, TypeCodes.UBYTE);
    }
	public int getTargetCount(MethodInformation iv) {
	    return 1;
	}
    public char getTypeCode() { return TypeCodes.RETURNADDRESS; }
    public Instruction widen() {
        char localIndex = NumberRanges.
            checkUnsignedShort(((ConcreteIntValue)istreamIns[0]).intValue());
        return WIDE_RET.make(localIndex);
    }
	public PCValue getJumpTarget() { return jumpTarget; }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
          
    /**
     * Access jump table by key match and jump.
     * <p>
     * Notes :
     * <ul>
     * <li>There is a 0-3 byte pad before the arguments start.
     * <li>They must be aligned to a position that's a multiple of four of
     *     the start of the method.
     * <li>The start of the method is at 14.
     * </ul>
     **/
    public static class LOOKUPSWITCH extends Switch { 
        public static final int opcode = JVMConstants.Opcodes.LOOKUPSWITCH;
        public static final LOOKUPSWITCH singleton = new LOOKUPSWITCH();
	public LOOKUPSWITCH() {
	    super(opcode);
	    stackIns = new Value [] {new IntValue()};
	    Padding padding = new Padding();
	    PCValue def = new PCValue();
	    IntValue npairs = new IntValue(TypeCodes.INT);
	    BinExp lengthExp = new BinExp(npairs, 
					  "*", 
					  ConcreteIntValue.EIGHT);
	    IntPCValuePairList list = new IntPCValuePairList(new IntValue(lengthExp));
	    istreamIns = imm(padding, def, npairs, list);
	    jumpTarget = new PCValue(new CallExp("lookup_switch", 
						 def, 
						 npairs, 
						 list, 
						 stackIns[0])); 
	}
	public LOOKUPSWITCH(int defaultRelOffset, int[] indices, int[] relOffsets) {
	    this();
        int npairs = indices.length;
        istreamIns[1] = new ConcreteIntValue(defaultRelOffset);
        istreamIns[2] = new ConcreteIntValue(npairs);
        ConcreteIntValue[] indexValues = new ConcreteIntValue[npairs];
        ConcretePCValue[] targetValues = new ConcretePCValue[npairs];
        for(int i = 0; i < indices.length; i++) {
            indexValues[i] = new ConcreteIntValue(indices[i]);
            targetValues[i] = new ConcretePCValue(relOffsets[i]);
        }
        istreamIns[3] = new ConcreteIntPCValuePairList(indexValues, targetValues);
    }
    public ConcretePCValue[] getAllTargets() {
        int npairs = ((ConcreteIntValue)istreamIns[2]).intValue();
        ConcretePCValue[] values = new ConcretePCValue[1 + npairs];
        values[0] = (ConcretePCValue)istreamIns[1]; // def
        ConcreteIntPCValuePairList list = (ConcreteIntPCValuePairList)istreamIns[3];
        ConcretePCValue[] pcValues = list.getPCValues();
        for(int i = 0; i < pcValues.length; i++) {
            values[i + 1] = pcValues[i]; 
        }
        return values;
    }
    public void setAllTargets(ConcretePCValue[] targets) {
        int npairs = ((ConcreteIntValue)istreamIns[2]).intValue();        
        if (targets.length != 1 + npairs) throw new IllegalArgumentException();
        istreamIns[1] = targets[0]; // def
        ConcreteIntPCValuePairList list = (ConcreteIntPCValuePairList)istreamIns[3];
        ConcretePCValue[] pcValues = list.getPCValues();
        for(int i = 0; i < pcValues.length; i++) {
            pcValues[i] = targets[i + 1];
        }
    }
    public boolean containsTarget(InstructionHandle ih) {
        if (istreamIns[1] == ih) return true;
        ConcreteIntPCValuePairList list = (ConcreteIntPCValuePairList)istreamIns[3];
        ConcretePCValue[] pcValues = list.getPCValues();
        for(int i = 0; i < pcValues.length; i++) {
            if (ih == pcValues[i])
                return true;
        }
        return false;
    }
    public void updateTarget(InstructionHandle old_ih,  InstructionHandle new_ih) {
        if (istreamIns[1] == old_ih) {
            istreamIns[1] = new_ih;
		old_ih.removeTargeter(this);
		if (new_ih != null)
			new_ih.addTargeter(this);
            return;
        }
        ConcreteIntPCValuePairList list = (ConcreteIntPCValuePairList)istreamIns[3];
        ConcretePCValue[] pcValues = list.getPCValues();
        for(int i = 0; i < pcValues.length; i++) {
            if (old_ih == pcValues[i]) {
                pcValues[i] = new_ih;
		old_ih.removeTargeter(this);
		if (new_ih != null)
			new_ih.addTargeter(this);
                return;
            }
        }
        throw new Error();
    }
    // The default target
    public InstructionHandle getTargetHandle() {
        return (InstructionHandle)istreamIns[1];
    }
    // The other targets
    public InstructionHandle[] getTargetHandles() {
        int npairs = ((ConcreteIntValue)istreamIns[2]).intValue();
        InstructionHandle[] values = new InstructionHandle[npairs];
        ConcreteIntPCValuePairList list = (ConcreteIntPCValuePairList)istreamIns[3];
        ConcretePCValue[] pcValues = list.getPCValues();
        for(int i = 0; i < pcValues.length; i++) {
            values[i] = (InstructionHandle)pcValues[i]; 
        }
        return values;
    }
    
    public Instruction concretize(MethodInformation iv) {
        Instruction clone = (Instruction)this.clone();  
        clone.istreamIns = new StreamableValue[this.istreamIns.length];
        int parameterOffset = 1 + ((Switch)this).getPadding(iv);
        int streamOffset = parameterOffset;
        // padding
        clone.istreamIns[0] = new ConcretePadding();
        // def
        clone.istreamIns[1] = istreamIns[1].concretize(iv, streamOffset);
        streamOffset += istreamIns[1].bytestreamSize();
        // npairs
        ConcreteIntValue npairs = (ConcreteIntValue)istreamIns[2].concretize(iv, streamOffset);
        clone.istreamIns[2] = npairs;
        streamOffset += istreamIns[2].bytestreamSize();
        // list
        clone.istreamIns[3] = ((IntPCValuePairList)istreamIns[3]).concretize(iv, streamOffset, npairs.intValue());
        streamOffset += istreamIns[3].bytestreamSize();
        return clone;
    }

    public int size(int position) {
        int padding = getPadding(position);
        int npairs = ((ConcreteIntValue)istreamIns[2]).intValue();
        return 1 + padding + 4 + 4 + 8 * npairs;
    }
	public int size(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    int count = iv.getCode().getInt(start + 4);
	    int retval = start + 8 + 8 * count - iv.getPC();
	    //BasicIO.out.println("size() " + retval);
	    return retval;
	}
 	public int getTargetCount(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    int count = iv.getCode().getInt(start + 4);
	    //BasicIO.out.println("getTargetCount: " + count+1);
	    return count+1; // + default target!
	}
    public int getTargetCount() {
        if (istreamIns[2] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[2]).intValue() + 1;
        else
            throw new Error("Not concrete");
    }
	public int getDefaultTarget(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    return iv.getCode().getInt(start);
	}
    public int getDefaultTarget() {
        if (istreamIns[1] instanceof ConcretePCValue)
            return ((ConcretePCValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    }
 	public int[] getTargets(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    int count = getTargetCount(iv)-1;
	    int[] ret = new int[count];
	    for (int i = 0; i < count; i++)
		ret[i] = iv.getCode().getInt(start + 12 + 8 * i);
	    return ret;
	}
    public int[] getTargets() {
        if (istreamIns[3] instanceof ConcreteIntPCValuePairList
                && istreamIns[2] instanceof ConcreteIntValue) {
            int npairs = ((ConcreteIntValue)istreamIns[2]).intValue();
            ConcreteIntPCValuePairList list = (ConcreteIntPCValuePairList)istreamIns[3];
            ConcretePCValue[] pcs = list.getPCValues();
            int[] targets = new int[npairs];
            for(int i = 0; i < npairs; i++) {
                targets[i] = pcs[i].intValue();
            }
            return targets;
        } else
            throw new Error("Not concrete");
    }
  	public int[] getIndexForTargets(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    int count = getTargetCount(iv)-1;
	    int[] ret = new int[count];
	    for (int i = 0; i < count; i++)
		ret[i] = iv.getCode().getInt(start + 8 + 8 * i);
	    return ret;
	}
    public int[] getIndexForTargets() {
        if (istreamIns[3] instanceof ConcreteIntPCValuePairList
                && istreamIns[2] instanceof ConcreteIntValue) {
            int npairs = ((ConcreteIntValue)istreamIns[2]).intValue();
            ConcreteIntPCValuePairList list = (ConcreteIntPCValuePairList)istreamIns[3];
            ConcreteIntValue[] ints = list.getIntValues();
            //ConcretePCValue[] pcs = list.getPCValues();
            int[] indices = new int[npairs];
            for(int i = 0; i < npairs; i++) {
                indices[i] = ints[i].intValue();
            }
            return indices;
        } else
            throw new Error("Not concrete");
    }
    
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Access jump table by access and jump.
     * <p>
     * Notes :
     * <ul>
     * <li>There is a 0-3 byte pad before the arguments start.
     * <li>They must be aligned to a position that's a multiple of four of
     *     the start of the method.
     * <li>The start of the method is at 14.
     * </ul>
     **/
    public static class TABLESWITCH 
	extends Switch { 
        public static final int opcode = JVMConstants.Opcodes.TABLESWITCH;
        public static final TABLESWITCH singleton = new TABLESWITCH();
	public TABLESWITCH() {
	    super(opcode);
	    stackIns = new Value[] {new IntValue()};
	    Padding padding = new Padding();
	    PCValue def = new PCValue(TypeCodes.INT);
	    IntValue low = new IntValue(TypeCodes.INT);
	    IntValue high = new IntValue(TypeCodes.INT);
	    BinExp lengthExp = new BinExp(high, 
					  "-", 
					  new IntValue(new BinExp(low, 
								  "+", 
								  ConcreteIntValue.ONE)));
	    IntValue length = new IntValue(lengthExp);
	    PCValueList list = new PCValueList(length);
	    
	    istreamIns = imm(padding, def, low, high, list);
	    BinExp index = new BinExp(stackIns[0], "-", low);
	    IntValue nondefault = new IntValue(new ListElementExp(list,
								  new IntValue(index)));
	    CondExp check1 = new CondExp(stackIns[0], ">", high);
	    CondExp check2 = new CondExp(stackIns[0], "<", low);

	    jumpTarget = 
		new PCValue(new IfExp(check1, 
				      def, 
				      new IntValue(new IfExp(check2, 
							     def, 
							     nondefault))));
	}
    public int size(int position) {
        int padding = getPadding(position);
        int high = getHigh();
        int low = getLow();
        return 1 + padding + 4 + 8 + 4 * (high - low + 1); 
    }
	public int size(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    int lowByte  = iv.getCode().getInt(start + 4);
	    int highByte = iv.getCode().getInt(start + 8);
	    int retval = start + 16 + 4 * (highByte - lowByte) - iv.getPC();
	    /*
	      d("size " + retval + " start " + start + " hi - lo "
	      + (highByte - lowByte) + " hi " + highByte + " lo " + lowByte);
	      d("PC is " + iv.getPC());
	    */
	    assert(retval > 0);
	    
	    return retval;
	}
	public int getHigh(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    return iv.getCode().getInt(start + 8);
	}
	public int getLow(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    return iv.getCode().getInt(start + 4);
	}
    public int getHigh() {
        if (istreamIns[3] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[3]).intValue();
        else
            throw new Error("Not concrete");
    }
    public int getLow() {
        if (istreamIns[2] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[2]).intValue();
        else
            throw new Error("Not concrete");
    }
  	public int getTargetCount(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    int lowByte  = iv.getCode().getInt(start + 4);
	    int highByte = iv.getCode().getInt(start + 8);
	    return highByte-lowByte+1+1; // include default target!
	}
 	public int getDefaultTarget(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    return iv.getCode().getInt(start);
	}
    public int getDefaultTarget() {
        if (istreamIns[1] instanceof ConcretePCValue)
            return ((ConcretePCValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    }
	public int[] getTargets(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    // int count = iv.getCode().getInt(start + 4);
	    int count = getTargetCount(iv)-1; // not default!
	    int[] ret = new int[count];
	    for (int i = 0; i < count; i++)
		ret[i] = iv.getCode().getInt(start + 12 + 4 * i);
	    return ret;
	}
    public int[] getTargets() {
        if (istreamIns[4] instanceof ConcretePCValueList) {
            ConcretePCValueList list = (ConcretePCValueList)istreamIns[4];
            ConcretePCValue[] elems = list.getElements();
            int[] targets = new int[elems.length];
            for(int i = 0; i < elems.length; i++) {
                targets[i] = elems[i].intValue();
            }
            return targets;
        } else
            throw new Error("Not concrete");
    }
	public int[] getIndexForTargets(MethodInformation iv) {
	    int start = getArgumentsStartPosition(iv);
	    int count = getTargetCount(iv)-1;// not default!
	    int[] ret = new int[count];
	    int lowByte  = iv.getCode().getInt(start + 4);
	    int highByte = iv.getCode().getInt(start + 8);
	    int j=0;
	    for (int i=lowByte;i<=highByte;i++)
		ret[j++] = i;
	    if (j != count)
		throw new Error("TableSwitch format error.");
	    return ret;
	}
    public int[] getIndexForTargets() {
        int low = getLow();
        int high = getHigh();
        int[] indices = new int[high - low + 1];
        int j = 0;
        for(int i = low; i <= high; i++)
            indices[j++] = i;
        return indices;
    }
    public ConcretePCValue[] getAllTargets() {
        int low = getLow();
        int high = getHigh();
        ConcretePCValue[] values = new ConcretePCValue[1 + high - low + 1];
        try {
        values[0] = (ConcretePCValue)istreamIns[1]; // def
        } catch (ClassCastException e) {
            BasicIO.out.println("istreamIns[1] was " + istreamIns[1].getClass());
            throw new Error();
        }
        ConcretePCValueList list = (ConcretePCValueList)istreamIns[4];
        ConcretePCValue[] pcValues = list.getElements();
        for(int i = 0; i < pcValues.length; i++) {
            values[i + 1] = pcValues[i]; 
        }
        return values;
    }
    public void setAllTargets(ConcretePCValue[] targets) {
        int low = getLow();
        int high = getHigh();
        if (targets.length != 1 + high - low + 1) throw new IllegalArgumentException();
        istreamIns[1] = targets[0]; // def
        ConcretePCValueList list = (ConcretePCValueList)istreamIns[4];
        ConcretePCValue[] pcValues = list.getElements();
        for(int i = 0; i < pcValues.length; i++) {
            pcValues[i] = targets[i + 1];
        }
    }
    public boolean containsTarget(InstructionHandle ih) {
        if (istreamIns[1] == ih) return true;
        ConcretePCValueList list = (ConcretePCValueList)istreamIns[4];
        ConcretePCValue[] pcValues = list.getElements();
        for(int i = 0; i < pcValues.length; i++) {
            if (pcValues[i] == ih)
                return true;
        }
        return false;
    }
    public void updateTarget(InstructionHandle old_ih,  InstructionHandle new_ih) {
        if (istreamIns[1] == old_ih) {
            istreamIns[1] = new_ih;
		old_ih.removeTargeter(this);
		if (new_ih != null)
			new_ih.addTargeter(this);
            return;
        }
        ConcretePCValueList list = (ConcretePCValueList)istreamIns[4];
        ConcretePCValue[] pcValues = list.getElements();
        for(int i = 0; i < pcValues.length; i++) {
            if (pcValues[i] == old_ih) {
                pcValues[i] = new_ih;
		old_ih.removeTargeter(this);
		if (new_ih != null)
			new_ih.addTargeter(this);
                return;
            }
        }
        throw new Error();
    }
    // The default target
    public InstructionHandle getTargetHandle() {
        return (InstructionHandle)istreamIns[1];
    }
    // The other targets
    public InstructionHandle[] getTargetHandles() {
        int low = getLow();
        int high = getHigh();
        InstructionHandle[] values = new InstructionHandle[1 + high - low];
        ConcretePCValueList list = (ConcretePCValueList)istreamIns[4];
        ConcretePCValue[] pcValues = list.getElements();
        for(int i = 0; i < pcValues.length; i++) {
            values[i] = (InstructionHandle)pcValues[i]; 
        }
        return values;
    }
    public Instruction concretize(MethodInformation iv) {
        Instruction clone = (Instruction)this.clone();  
        clone.istreamIns = new StreamableValue[this.istreamIns.length];
        int parameterOffset = 1 + ((Switch)this).getPadding(iv);
        int streamOffset = parameterOffset;
        // padding
        clone.istreamIns[0] = new ConcretePadding();
        // def
        clone.istreamIns[1] = istreamIns[1].concretize(iv, streamOffset);
        streamOffset += istreamIns[1].bytestreamSize();
        // low
        ConcreteIntValue low = (ConcreteIntValue)istreamIns[2].concretize(iv, streamOffset);
        clone.istreamIns[2] = low;
        streamOffset += istreamIns[2].bytestreamSize();
        // high
        ConcreteIntValue high = (ConcreteIntValue)istreamIns[3].concretize(iv, streamOffset);
        clone.istreamIns[3] = high;
        streamOffset += istreamIns[3].bytestreamSize();
        // list
        clone.istreamIns[4] = ((PCValueList)istreamIns[4]).concretize(iv, streamOffset, high.intValue() - low.intValue() + 1);
        streamOffset += istreamIns[4].bytestreamSize();
        return clone;
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class INVOKEINTERFACE 
	extends Invocation { 
        public static final int opcode = JVMConstants.Opcodes.INVOKEINTERFACE;
        public static final INVOKEINTERFACE singleton = new INVOKEINTERFACE();
	public INVOKEINTERFACE() {
	    super(opcode);
	    IntValue count = new IntValue(TypeCodes.UBYTE);
	    stackIns = new Value[] {new ValueList(count), new NonnulRefValue()};
	    istreamIns = imm(new CPIndexValue(CPIndexValue.CONSTANT_Any),
			     count,
			     new IntValue(TypeCodes.UBYTE));	    
 	}	
    public INVOKEINTERFACE(int cpindex, int count) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
        istreamIns[1] = new ConcreteIntValue(count, TypeCodes.UBYTE);
        istreamIns[2] = new ConcreteIntValue(0, TypeCodes.UBYTE);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class INVOKESPECIAL extends Invocation { 
        public static final int opcode = JVMConstants.Opcodes.INVOKESPECIAL;
        public static final INVOKESPECIAL singleton = new INVOKESPECIAL();
 	public INVOKESPECIAL() {
	    super(opcode);
	}
    public INVOKESPECIAL(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
   }

    public static class INVOKEVIRTUAL extends Invocation { 
        public static final int opcode = JVMConstants.Opcodes.INVOKEVIRTUAL;
        public static final INVOKEVIRTUAL singleton = new INVOKEVIRTUAL();
	public INVOKEVIRTUAL() {
	    super(opcode);
	}
    public INVOKEVIRTUAL(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class INVOKESTATIC extends Invocation { 
        public static final int opcode = JVMConstants.Opcodes.INVOKESTATIC;
        public static final INVOKESTATIC singleton = new INVOKESTATIC();
	public INVOKESTATIC() {
	    super(opcode);
	}
    public INVOKESTATIC(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	/**
	 * Obtain the number of arguments. The "class" pointer for
	 * static invocations (see: JNI) does NOT count.
	 * Double and long arguments count twice.
	 * <p>
	 * Thus this number gives the actual number
	 * of arguments obtained from the stack.
	 * @return the number of arguments
	 **/
	public int getArgumentCount(MethodInformation iv, Constants cp) {
	    return super.getArgumentCount(iv, cp) - 1;
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Enter monitor for object
     **/
    public static class MONITORENTER extends Synchronization { 
        public static final int opcode = JVMConstants.Opcodes.MONITORENTER;
        public static final MONITORENTER singleton = new MONITORENTER();
	public MONITORENTER() {
	    super(opcode, "monitorEnter",
		  mayThrow(NULL_POINTER_EXCEPTION, ERROR));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Exit monitor for object
     **/
    public static class MONITOREXIT extends Synchronization { 
        public static final int opcode = JVMConstants.Opcodes.MONITOREXIT;
        public static final MONITOREXIT singleton = new MONITOREXIT();
	public MONITOREXIT() {
	    super(opcode, "monitorExit",
		  mayThrow(NULL_POINTER_EXCEPTION,
			   ILLEGAL_MONITOR_STATE_EXCEPTION,
			   ERROR));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Throws an exception. A pointer to the exception is
     * popped from the stack. If it is the this-pointer,
     * this is a violation of anonymity.
     **/
    public static class ATHROW extends FlowEnd
	implements ExceptionThrower
    { 
        public static final int opcode = JVMConstants.Opcodes.ATHROW;
        public static final ATHROW singleton = new ATHROW();
	public ATHROW() {
	    super(opcode);
	    stackIns = stack(new NonnulRefValue());
	    evals = eval(new CSACallExp("processThrowable", stackIns[0]));
	}
	private final TypeName.Scalar[] exceptions_ = mayThrow(THROWABLE);
	public TypeName.Scalar[] getThrowables() {
	    return exceptions_;
	}
	public int getTargetCount(MethodInformation iv) { 
	    return 0; 
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
     }

    /**
     * This instruction does nothing (but advancing
     * the pc and wasting memory & time).
     **/
    public static class NOP extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.NOP;
        public static final NOP singleton = new NOP();
        public static NOP make() { return singleton; }
	public NOP() { super(opcode); }
	protected NOP(int opcode) { super(opcode); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * An unintepretted 32 bit label within byte code.  Labels are
     * typically used to annotate other instructions or to annotate
     * basic blocks.  Labels should not be moved or eliminated by
     * bytecode transformations, so that it is possible to recover
     * labels inserted before the transformation was done.
     **/
    public static class LABEL extends NOP {
	public static final int opcode = JVMConstants.Opcodes.LABEL;
	public static final LABEL singleton = new LABEL();
	public LABEL() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.INT);
	}
	/** The 32 bit label of this instruction. **/
	public int get(MethodInformation iv) {
	    return istreamIns[0].decodeInt(iv, 1);
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
	    
    public static class INSTANCEOF
	extends Resolution { 
        public static final int opcode = JVMConstants.Opcodes.INSTANCEOF;
        public static final INSTANCEOF singleton = new INSTANCEOF();
	public INSTANCEOF() {
	    super(opcode, loadingErrors);
	    istreamIns = imm(new CPIndexValue(CPIndexValue.CONSTANT_Any));
	    stackIns = new Value[] { new RefValue() };
	    stackOuts = new Value[] { new IntValue() };
	}
	public INSTANCEOF(int cpindex) {
	    this();
	    istreamIns[0] = new ConcreteCPIndexValue(cpindex);
	}
 	public TypeName.Compound getTypeName(MethodInformation iv,
					     Constants cp) {
	    try {
		return cp.getClassAt(getCPIndex(iv)).asTypeName().asCompound();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
	public TypeName.Compound getTypeName(Constants cp) {
	    return cp.getClassAt(getCPIndex()).asTypeName().asCompound();
	}
	public ConstantClass getConstantClass(MethodInformation iv,
					      Constants cp) {
	    try {
		return cp.getClassAt(getCPIndex(iv));
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
	public ConstantClass getConstantClass(Constants cp) {
	    return cp.getClassAt(getCPIndex());
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class CHECKCAST
	extends ConstantPoolPush { 
        public static final int opcode = JVMConstants.Opcodes.CHECKCAST;
        public static final CHECKCAST singleton = new CHECKCAST();
	public CHECKCAST() {
	    super(opcode);
	    ex = loadingErrors;
	    istreamIns = imm(new CPIndexValue(CPIndexValue.CONSTANT_Any));
	    // KP: shouldn't the refs be the same?
	    stackIns = new Value[] { new RefValue() };
	    stackOuts = new Value[] { new RefValue() };
	}
	
    public CHECKCAST(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public TypeName.Compound getTypeName(MethodInformation iv,
					     Constants cp) {
	    try {
		return cp.getClassAt(getCPIndex(iv)).asTypeName().asCompound();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
	public TypeName.Compound getTypeName(Constants cp) {
	    return cp.getClassAt(getCPIndex()).asTypeName().asCompound();
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class NEW extends ConstantPoolPush implements Allocation { 
        public static final int opcode = JVMConstants.Opcodes.NEW;
        public static final NEW singleton = new NEW();
	public NEW() {
	    super(opcode);
	    ex = loadingErrors;	// loadingErrors include out of memory
	    CPIndexValue val = new CPIndexValue(CPIndexValue.CONSTANT_Any,
						TypeCodes.USHORT);	    
	    istreamIns = imm(val);
	    this.stackOuts = new Value[] {
	    new RefValue(new CSACallExp("resolveNEW",  
					    new IntValue(new ConversionExp((Value)istreamIns[0])),
					    new RefValue(new CurrentConstantPool())))
        };
	}
    public NEW(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public TypeName.Scalar getClassName(MethodInformation iv,
					    Constants cp) {
	    try {
		return cp.getClassAt(getCPIndex(iv)).asTypeName().asScalar();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
    public TypeName.Scalar getClassName(Constants cp) {
        return cp.getClassAt(getCPIndex()).asTypeName().asScalar();
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
					        
    public static class WIDE 
	extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.WIDE;
        public static final WIDE singleton = new WIDE();
	public WIDE() {
	    super(opcode);
	}
    public Instruction concretize(MethodInformation iv) {
        return specialize(iv).concretize(iv);
    }
	public int size(MethodInformation iv) {
	    return specialize(iv).size(iv);
	}
	public String toString(MethodInformation iv) {
	    return "WIDE: " + specialize(iv).toString(iv);
	}
	public int getType(MethodInformation iv) {
	    return (0xFF & (iv.getCode().get(iv.getPC() + 1)+0x100));
	}
	/**
	 * Return a specialized instruction object that depends on
	 * the opcode that the current WIDE instruction is parametrized with.
	 * The returned instruction will behave like the matching IINC,
	 * XLOADN or XSTOREN instruction, except that it uses a variant
	 * that respects the semantics of WIDE.<p>
	 * Example:<br>
	 * If the wide-opcode in the current bytestream is
	 * describing a WIDE-IINC, the specialize method will
	 * return an object that implements IINC. The difference
	 * to the normal IINC implementation will be, that this
	 * object will read a 2-byte index into the local
	 * variables.<p>
	 * This way, the application code does not have to care about
	 * wide instructions by using
	 * <pre>
	 * i.specialize().accept(this);
	 * </pre>
	 * in the <tt>visitWide(Instruction.WIDE i)</tt> method of
	 * the visitor.
	 **/
	public Instruction specialize(MethodInformation iv) {
	    // FIXME We are using singletons / InstructionSet index, but there
            // should be some way to get the InstructionSet
	    // from our MethodInformation (or maybe our home IS should
	    // be passed in as it is being populated.
	    InstructionSet myIs = InstructionSet.SINGLETON;
	    Instruction ret = myIs.getInstructions()[
	    	JVMConstants.Opcodes.WIDE_OFFSET + getType(iv)];
	    if (ret instanceof UNIMPLEMENTED)
		throw new Error("WIDE parametrized with invalid opcode: "
				+ getType(iv));
	    return ret;
	}

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    
    public static class WIDE_IINC extends IINC { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.IINC;
        public static final WIDE_IINC singleton = new WIDE_IINC();
        public static WIDE_IINC make(char index, byte delta) {
            WIDE_IINC clone = (WIDE_IINC)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT),
                    new ConcreteIntValue(delta, TypeCodes.SHORT));
            return clone;
        }
	// FIXME copied from IINC()
	WIDE_IINC() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.USHORT, TypeCodes.SHORT);
	    offset = (IntValue) istreamIns[0];

	    IntValue orig = new IntValue(new LocalExp(offset));
	    BinExp sum = new BinExp(orig, "+", (Value)istreamIns[1]);

	    evals = eval(new LocalStore(offset, new IntValue(sum)));
	}

 	/**
	 * Return delta
	 **/
	public int getValue(MethodInformation iv) {
	    return iv.getCode().getShort(iv.getPC() + 4);
	}
	public int getLocalVariableOffset(MethodInformation iv)	{
	    return offset.decodeInt(iv, 2);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_ILOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.ILOAD;
        public static final WIDE_ILOAD singleton = new WIDE_ILOAD();
        public static WIDE_ILOAD make(char index) {
            WIDE_ILOAD clone = (WIDE_ILOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_ILOAD() {
	    super(opcode, new IntValue(TypeCodes.USHORT), intFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_FLOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.FLOAD;
        public static final WIDE_FLOAD singleton = new WIDE_FLOAD();
        public static WIDE_FLOAD make(char index) {
            WIDE_FLOAD clone = (WIDE_FLOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_FLOAD() {
	    super(opcode, new IntValue(TypeCodes.USHORT), floatFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_ALOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.ALOAD;
        public static final WIDE_ALOAD singleton = new WIDE_ALOAD();
        public static WIDE_ALOAD make(char index) {
            WIDE_ALOAD clone = (WIDE_ALOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_ALOAD() {
	    super(opcode, new IntValue(TypeCodes.USHORT), refFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_LLOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.LLOAD;
        public static final WIDE_LLOAD singleton = new WIDE_LLOAD();
        public static WIDE_LLOAD make(char index) {
            WIDE_LLOAD clone = (WIDE_LLOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_LLOAD() {
	    super(opcode, new IntValue(TypeCodes.USHORT), longFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_DLOAD extends LocalRead { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.DLOAD;
        public static final WIDE_DLOAD singleton = new WIDE_DLOAD();
        public static WIDE_DLOAD make(char index) {
            WIDE_DLOAD clone = (WIDE_DLOAD)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_DLOAD() {
	    super(opcode, new IntValue(TypeCodes.USHORT), doubleFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_ISTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.ISTORE;
        public static final WIDE_ISTORE singleton = new WIDE_ISTORE();
        public static WIDE_ISTORE make(char index) {
            WIDE_ISTORE clone = (WIDE_ISTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_ISTORE() {
	    super(opcode, new IntValue(TypeCodes.USHORT), intFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_FSTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.FSTORE;
        public static final WIDE_FSTORE singleton = new WIDE_FSTORE();
        public static WIDE_FSTORE make(char index) {
            WIDE_FSTORE clone = (WIDE_FSTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_FSTORE() {
	    super(opcode, new IntValue(TypeCodes.USHORT), floatFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_ASTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.ASTORE;
        public static final WIDE_ASTORE singleton = new WIDE_ASTORE();
        public static WIDE_ASTORE make(char index) {
            WIDE_ASTORE clone = (WIDE_ASTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_ASTORE() {
	    super(opcode, new IntValue(TypeCodes.USHORT), refFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_DSTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.DSTORE;
        public static final WIDE_DSTORE singleton = new WIDE_DSTORE();
        public static WIDE_DSTORE make(char index) {
            WIDE_DSTORE clone = (WIDE_DSTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }

	WIDE_DSTORE() {
	    super(opcode, new IntValue(TypeCodes.USHORT), doubleFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_LSTORE extends LocalWrite { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.LSTORE;
        public static final WIDE_LSTORE singleton = new WIDE_LSTORE();
        public static WIDE_LSTORE make(char index) {
            WIDE_LSTORE clone = (WIDE_LSTORE)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	WIDE_LSTORE() {
	    super(opcode, new IntValue(TypeCodes.USHORT), longFactory);
	}
    public boolean doesNeedWidening() {
        return false;
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class WIDE_RET extends LocalAccess implements FlowChange { 
        public static final int opcode = JVMConstants.Opcodes.WIDE_OFFSET +
                                     JVMConstants.Opcodes.RET;
        public static final WIDE_RET singleton = new WIDE_RET();
        public static WIDE_RET make(char index) {
            WIDE_RET clone = (WIDE_RET)singleton.clone();
            clone.istreamIns = imm(new ConcreteIntValue(index, TypeCodes.USHORT));
            return clone;
        }
	// FIXME copied from RET
	PCValue jumpTarget;
 	WIDE_RET() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.USHORT);
	    offset = (IntValue) istreamIns[0];
	    jumpTarget = new PCValue(new LocalExp(offset), false);
	}
    public WIDE_RET(int index) {
        this();
        istreamIns[0] = new ConcreteIntValue(index, TypeCodes.USHORT);
    }
    public char getTypeCode() { return TypeCodes.RETURNADDRESS; }
    public boolean doesNeedWidening() {
        return false;
    }

	public int getTargetCount(MethodInformation iv) {
	    return 1;
	}
	public PCValue getJumpTarget() { return jumpTarget; }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /* ************* quick instructions ******************* */

    public static class ANEWARRAY_QUICK extends ConstantPoolRead { 
        public static final int opcode = JVMConstants.Opcodes.ANEWARRAY_QUICK;
        public static final ANEWARRAY_QUICK singleton = new ANEWARRAY_QUICK();
	ANEWARRAY_QUICK() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Reference,
						TypeCodes.USHORT);
	    stackIns = stack(new IntValue());
	    istreamIns = imm(val);
	    
	    stackOuts = new Value[] {
		new RefValue
		(
                new CSACallExp
		 ("allocateArray", 
		  new RefValue(new CPAccessExp(val)), stackIns[0]
                )
                )
	    };
	    
	    if (false) {
	      // enable this to trace where arrays are allocated
	      // (or anywhere else for that matter)
  	      evals = eval( new CallExp( "STORE_LINE_NUMBER") );
            }
	}
	
    ANEWARRAY_QUICK(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class CHECKCAST_QUICK
	extends ConstantPoolRead implements ExceptionThrower
    { 
        public static final int opcode = JVMConstants.Opcodes.CHECKCAST_QUICK;
        public static final CHECKCAST_QUICK singleton = new CHECKCAST_QUICK();
	TypeName.Scalar[] ex = mayThrow(CLASS_CAST_EXCEPTION, ERROR);
	public TypeName.Scalar[] getThrowables() { return ex; }

	CHECKCAST_QUICK() {
	    super(opcode);
	    RefValue ptr = new RefValue();
	    stackIns = stack(ptr);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Reference,
						TypeCodes.USHORT);
	    istreamIns = imm(val);
	    ValueSource expr
		= new IfExp(ptr,
			    instanceTest(ptr, val),
			    ConcreteIntValue.ONE);
	    evals = eval(new IfCmd(new IntValue(new UnaryExp("!", new IntValue(expr))),
			   throwException(CLASS_CAST_EXCEPTION)));
	    stackOuts = stack(ptr);
	}
    
    CHECKCAST_QUICK(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }

    }

    public static class INSTANCEOF_QUICK extends ConstantPoolRead
    { 
        public static final int opcode = JVMConstants.Opcodes.INSTANCEOF_QUICK;
        public static final INSTANCEOF_QUICK singleton = new INSTANCEOF_QUICK();
	INSTANCEOF_QUICK() {
	    super(opcode);
	    RefValue ptr = new RefValue();
	    stackIns = stack(ptr);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Reference,
						TypeCodes.USHORT);
	    istreamIns = imm(val);
	    ValueSource expr = new IfExp(ptr,
					 instanceTest(ptr, val),
					 ConcreteIntValue.ZERO);
	    stackOuts = stack(new IntValue(expr));
	}
    INSTANCEOF_QUICK(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class NEW_QUICK extends ConstantPoolRead { 
        public static final int opcode = JVMConstants.Opcodes.NEW_QUICK;
        public static final NEW_QUICK singleton = new NEW_QUICK();
	NEW_QUICK() {
	    super(opcode);
	    CPIndexValue val = new CPIndexValue(JVMConstants.CONSTANT_Reference,
						TypeCodes.USHORT);
	    istreamIns = imm(val);
	    stackOuts = new Value[] {
		new NonnulRefValue
		(new CSACallExp("allocateObject", 
				new RefValue(new CPAccessExp(val))))
	    };
	}
    NEW_QUICK(int cpindex) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }


    public static class MULTIANEWARRAY_QUICK 
	extends ConstantPoolRead { 
        public static final int opcode = JVMConstants.Opcodes.MULTIANEWARRAY_QUICK;
        public static final MULTIANEWARRAY_QUICK singleton = new MULTIANEWARRAY_QUICK();
	MULTIANEWARRAY_QUICK() {
	    super(opcode);
        CPIndexValue bpindex = new CPIndexValue(TypeCodes.USHORT);
        IntValue dim = new IntValue(TypeCodes.UBYTE);
	    istreamIns = imm(bpindex, dim);
	    stackIns = stack(new IntValueList((IntValue)istreamIns[1]));
	    stackOuts = stack(new NonnulRefValue());
	}
	public int getDimensions(MethodInformation iv) {
	    return iv.getCode().get(iv.getPC() + 3);
	}
    public int getDimensions() {
        if (istreamIns[1] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    }
    MULTIANEWARRAY_QUICK(int cpindex, int dimensions) {
        this();
        istreamIns[0] = new ConcreteCPIndexValue(cpindex);
        istreamIns[1] = new ConcreteIntValue(dimensions, TypeCodes.UBYTE);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class GETFIELD_QUICK extends FieldGet_Quick { 
        public static final int opcode = JVMConstants.Opcodes.GETFIELD_QUICK;
        public static final GETFIELD_QUICK singleton = new GETFIELD_QUICK();
    GETFIELD_QUICK() { super(opcode, intFactory); }
    GETFIELD_QUICK(int offset) {
        this();
        istreamIns[0] = new ConcreteIntValue(offset, TypeCodes.USHORT);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class REF_GETFIELD_QUICK extends FieldGet_Quick { 
        public static final int opcode = JVMConstants.Opcodes.REF_GETFIELD_QUICK;
        public static final REF_GETFIELD_QUICK singleton = new REF_GETFIELD_QUICK();
    REF_GETFIELD_QUICK() { super(opcode, refFactory); }
    public REF_GETFIELD_QUICK(int offset) {
        this();
        istreamIns[0] = new ConcreteIntValue(offset, TypeCodes.USHORT);
    }
    	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class GETFIELD2_QUICK extends FieldGet_Quick { 
        public static final int opcode = JVMConstants.Opcodes.GETFIELD2_QUICK;
        public static final GETFIELD2_QUICK singleton = new GETFIELD2_QUICK();
	GETFIELD2_QUICK() { super(opcode, longFactory); }
    GETFIELD2_QUICK(int offset) {
        this();
        istreamIns[0] = new ConcreteIntValue(offset, TypeCodes.USHORT);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class PUTFIELD_QUICK extends FieldPut_Quick { 
        public static final int opcode = JVMConstants.Opcodes.PUTFIELD_QUICK;
        public static final PUTFIELD_QUICK singleton = new PUTFIELD_QUICK();
	PUTFIELD_QUICK() { this(opcode, intFactory); }
	PUTFIELD_QUICK(int oc, ValueFactory vf) { super(oc, vf); }
    PUTFIELD_QUICK(int offset) {
        this();
        istreamIns[0] = new ConcreteIntValue(offset, TypeCodes.USHORT);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class PUTFIELD2_QUICK extends FieldPut_Quick { 
        public static final int opcode = JVMConstants.Opcodes.PUTFIELD2_QUICK;
        public static final PUTFIELD2_QUICK singleton = new PUTFIELD2_QUICK();
	PUTFIELD2_QUICK() { super(opcode, longFactory); }
    PUTFIELD2_QUICK(int offset) {
        this();
        istreamIns[0] = new ConcreteIntValue(offset, TypeCodes.USHORT);
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public static class PUTFIELD_QUICK_WITH_BARRIER_REF
	extends PUTFIELD_QUICK { 
        public static final int opcode = JVMConstants.Opcodes.PUTFIELD_QUICK_WITH_BARRIER_REF;
        public static final PUTFIELD_QUICK_WITH_BARRIER_REF singleton = new PUTFIELD_QUICK_WITH_BARRIER_REF();
	PUTFIELD_QUICK_WITH_BARRIER_REF() {
	    super(opcode, intFactory);
/*          
this is now handled in SpecInstantiation.java

	    IntValue offset
		= new IntValue(new ConversionExp((Value)istreamIns[0]));
	    stackIns = stack(new RefValue(),
			     new NonnulRefValue());    
	    evals = eval(new CSACallExp("putFieldBarrier",
					stackIns[1],
					offset,
					stackIns[0]));
*/
	}
    PUTFIELD_QUICK_WITH_BARRIER_REF(int offset) {
        this();
        istreamIns[0] = new ConcreteIntValue(offset, TypeCodes.USHORT);
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    } 
    
    /**
     * FORMAT:
     *
     * INVOKEVIRTUAL_QUICK {
     *     byte ivq_opcode;  (= INVOKEVIRTUAL_QUICK opcode)
     *     char vtable_index;
     *     byte argument_count;
     *     byte wide_argument_count;
     *     char selector_cpindex;
     * };
     **/
    public static class INVOKEVIRTUAL_QUICK 
	extends Invocation_Quick { 
        public static final int opcode = JVMConstants.Opcodes.INVOKEVIRTUAL_QUICK;
        public static final INVOKEVIRTUAL_QUICK singleton = new INVOKEVIRTUAL_QUICK();
	INVOKEVIRTUAL_QUICK() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.USHORT, TypeCodes.UBYTE, 
			     TypeCodes.UBYTE, TypeCodes.USHORT);
	    IntValue index = (IntValue) istreamIns[0];
	    IntValue count = computeCount((IntValue)istreamIns[1],
					  (IntValue)istreamIns[2]);
	    RefValue recv = computeStackIn(count);
	    RefValue bp = findBlueprint(new BlueprintAccessExp(recv));
	    invoke(recv, bp, VIRTUAL, index, count);
	}
        public INVOKEVIRTUAL_QUICK(int vtable_index, int selCPIndex, Constants constantPool) {
            this();
            Descriptor.Method desc = constantPool.getMethodrefAt(selCPIndex)
            .asSelector().getDescriptor();
            istreamIns = imm(new ConcreteIntValue(vtable_index, TypeCodes.USHORT),
                    new ConcreteIntValue(desc.getArgumentCount(), TypeCodes.UBYTE),
                    new ConcreteIntValue(desc.getWideArgumentCount(), TypeCodes.UBYTE),
                    new ConcreteIntValue(selCPIndex, TypeCodes.USHORT));
        }
	public int getMethodTableIndex(MethodInformation iv) {
	    return iv.getCode().getChar(iv.getPC() + 1);
	}
    public int getMethodTableIndex() {    
        if (istreamIns[0] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[0]).intValue();
        else
            throw new Error("Not concrete");
    }
    public void setMethodTableIndex(int index) {
        if (istreamIns[0] instanceof ConcreteIntValue)
            istreamIns[0] = new ConcreteIntValue(index, TypeCodes.USHORT);
        else
            throw new Error("Not concrete");
    }
	/**
	 * @return the argument count
	 **/
	public int getArgumentCount(MethodInformation iv) {
	    return (0xFF & iv.getCode().get(iv.getPC() + 3) + 0x100);
	} 
    public int getArgumentCount() {
        if (istreamIns[1] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    } 
	/**
	 * @return the wide argument count
	 **/
	public int getWideArgumentCount(MethodInformation iv) {
	    return (0xFF & iv.getCode().get(iv.getPC() + 4) + 0x100);
	}
    public int getWideArgumentCount() {
        if (istreamIns[2] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[2]).intValue();
        else
            throw new Error("Not concrete");
    }

	public Selector.Method getSelector(MethodInformation iv,
					   Constants cp) {
	    try {
		return cp.getMethodrefAt(iv.getCode().getChar(iv.getPC()+5))
		    .asSelector();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
    public Selector.Method getSelector(Constants constantPool) {
        if (istreamIns[3] instanceof ConcreteIntValue) {
            int cpIndex = ((ConcreteIntValue)istreamIns[3]).intValue();
            try {
                return constantPool.getMethodrefAt(cpIndex).asSelector();
            } catch (ConstantPool.AccessException ae) {
                throw ae.fatal();
            }
        } else
            throw new Error("Not concrete");
    }

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * FORMAT:
     *
     * INVOKENONVIRTUAL_QUICK {
     *     byte opcode; (= INVOKENONVIRTUAL_QUICK opcode)
     *     char nvtable_index;
     *     byte argument_count;
     *     byte wide_argument_count;
     *     char selector_cpindex;
     * };
     **/
    public static class INVOKENONVIRTUAL_QUICK 
	extends Invocation_Quick { 
        public static final int opcode = JVMConstants.Opcodes.INVOKENONVIRTUAL_QUICK;
        public static final INVOKENONVIRTUAL_QUICK singleton = new INVOKENONVIRTUAL_QUICK();
	INVOKENONVIRTUAL_QUICK() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.USHORT, TypeCodes.UBYTE, 
			     TypeCodes.UBYTE, TypeCodes.USHORT);
	    IntValue index = (IntValue) istreamIns[0];
	    IntValue count = computeCount((IntValue)istreamIns[1], 
					  (IntValue)istreamIns[2]);
	    RefValue recv = computeStackIn(count);
	    RefValue bp = findBlueprint(new BlueprintAccessExp(recv));
	    invoke(recv, bp, NONVIRTUAL, index, count);
	}
    public INVOKENONVIRTUAL_QUICK(int nvtable_index, int selCPIndex, Constants constantPool) {
        this();
        Descriptor.Method desc = constantPool.getMethodrefAt(selCPIndex).asSelector().getDescriptor();
        istreamIns = imm(new ConcreteIntValue(nvtable_index, TypeCodes.USHORT),
                new ConcreteIntValue(desc.getArgumentCount(), TypeCodes.UBYTE),
                new ConcreteIntValue(desc.getWideArgumentCount(), TypeCodes.UBYTE),
                new ConcreteIntValue(selCPIndex, TypeCodes.USHORT));
    }
	public int getMethodTableIndex(MethodInformation iv) {
	    return iv.getCode().getChar(iv.getPC() + 1);
	}
    public int getMethodTableIndex() {    
        if (istreamIns[0] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[0]).intValue();
        else
            throw new Error("Not concrete");
    }
    public void setMethodTableIndex(int index) {
        if (istreamIns[0] instanceof ConcreteIntValue)
            istreamIns[0] = new ConcreteIntValue(index, TypeCodes.USHORT);
        else
            throw new Error("Not concrete");
    }

	/**
	 * @return the argument count
	 **/
	public int getArgumentCount(MethodInformation iv) {
	    return (0xFF & iv.getCode().get(iv.getPC() + 3) + 0x100);
	} 
    public int getArgumentCount() {
        if (istreamIns[1] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    } 
	/**
	 * @return the wide argument count
	 **/
	public int getWideArgumentCount(MethodInformation iv) {
	    return (0xFF & iv.getCode().get(iv.getPC() + 4) + 0x100);
	} 
    public int getWideArgumentCount() {
        if (istreamIns[2] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[2]).intValue();
        else    
            throw new Error("Not concrete");
    }

	public Selector.Method getSelector(MethodInformation iv,
					   Constants cp) {
	    try {
		return cp.getMethodrefAt(iv.getCode().getChar(iv.getPC()+5))
		    .asSelector();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}

    public Selector.Method getSelector(Constants constantPool) {
        if (istreamIns[3] instanceof ConcreteIntValue) {
            int cpIndex = ((ConcreteIntValue)istreamIns[3]).intValue();
            try {
                return constantPool.getMethodrefAt(cpIndex).asSelector();
            } catch (ConstantPool.AccessException ae) {
                throw ae.fatal();
            }
        } else
            throw new Error("Not concrete");
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * FORMAT:
     *
     * INVOKENONVIRTUAL2_QUICK {
     *     byte opcode; (= INVOKENONVIRTUAL2_QUICK opcode)
     *     char cpindex;
     *     char nvtable_index;
     *     byte argument_count;
     *     byte wide_argument_count;
     *     char selector_cpindex;
     * };
     **/
    public static class INVOKENONVIRTUAL2_QUICK 
	extends Invocation_Quick { 
        public static final int opcode = JVMConstants.Opcodes.INVOKENONVIRTUAL2_QUICK;
        public static final INVOKENONVIRTUAL2_QUICK singleton = new INVOKENONVIRTUAL2_QUICK();
	INVOKENONVIRTUAL2_QUICK() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.USHORT, TypeCodes.USHORT,
		TypeCodes.UBYTE, TypeCodes.UBYTE, TypeCodes.USHORT);
	    IntValue index = (IntValue) istreamIns[1];
	    IntValue count = computeCount((IntValue)istreamIns[2], 
					  (IntValue)istreamIns[3]);
	    RefValue recv = computeStackIn(count);
	    RefValue bp = findBlueprint
		(new CallExp("GET_CONSTANT_BP_RESOLVED_INSTANCE_METHODREF",
			     (Value)istreamIns[0]));
	    invoke(recv, bp, NONVIRTUAL, index, count);
	}
    public INVOKENONVIRTUAL2_QUICK(int cpIndex, int nvtable_index, int selCPIndex, 
            Constants constantPool) {
        this();
        Descriptor.Method desc = constantPool.getMethodrefAt(selCPIndex)
        .asSelector().getDescriptor();
        istreamIns = imm(new ConcreteIntValue(cpIndex, TypeCodes.USHORT),
                new ConcreteIntValue(nvtable_index, TypeCodes.USHORT),
                new ConcreteIntValue(desc.getArgumentCount(), TypeCodes.UBYTE),
                new ConcreteIntValue(desc.getWideArgumentCount(), TypeCodes.UBYTE),
                new ConcreteIntValue(selCPIndex, TypeCodes.USHORT));
    }
	/**
	 * Which index of the Constant Pool is accessed by this instruction?
	 **/
	public int getCPIndex(MethodInformation iv) {
	    return istreamIns[0].decodeInt(iv, 1);
	}
	/**
	 * Like <code>getCPIndex(null)<code> this method returns the
	 * index used by a concrete instruction object.
	 * @deprecated
	 **/
	public int getCPIndex() { return getCPIndex(null); }
    public void setCPIndex(int index) {
        if (istreamIns[0] instanceof ConcreteIntValue)
            istreamIns[0] = new ConcreteIntValue(index, TypeCodes.USHORT);
        else
            throw new Error("Not concrete");
    }
	public int getMethodTableIndex(MethodInformation iv) {
	    return iv.getCode().getChar(iv.getPC() + 3);
	}
    public int getMethodTableIndex() {    
        if (istreamIns[1] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    }
    public void setMethodTableIndex(int index) {
        if (istreamIns[1] instanceof ConcreteIntValue)
            istreamIns[1] = new ConcreteIntValue(index, TypeCodes.USHORT);
        else
            throw new Error("Not concrete");
    }

	/**
	 * @return the argument count
	 **/
	public int getArgumentCount(MethodInformation iv) {
	    return (0xFF & iv.getCode().get(iv.getPC() + 5) + 0x100);
	} 
    public int getArgumentCount() {
        if (istreamIns[2] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[2]).intValue();
        else
            throw new Error("Not concrete");
    } 
	/**
	 * @return the wide argument count
	 **/
	public int getWideArgumentCount(MethodInformation iv) {
	    return (0xFF & iv.getCode().get(iv.getPC() + 6) + 0x100);
	} 
    public int getWideArgumentCount() {
        if (istreamIns[3] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[3]).intValue();
        else    
            throw new Error("Not concrete");
    }
	public Selector.Method getSelector(MethodInformation iv,
					   Constants cp) {
	    try {
		return cp.getMethodrefAt(iv.getCode().getChar(iv.getPC()+7))
		    .asSelector();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
    public Selector.Method getSelector(Constants constantPool) {
        if (istreamIns[4] instanceof ConcreteIntValue) {
            int cpIndex = ((ConcreteIntValue)istreamIns[4]).intValue();
            try {
                return constantPool.getMethodrefAt(cpIndex).asSelector();
            } catch (ConstantPool.AccessException ae) {
                throw ae.fatal();
            }
        } else
            throw new Error("Not concrete");
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * FORMAT:
     *
     * INVOKESUPER_QUICK {
     *     byte opcode; (= INVOKESUPER_QUICK opcode)
     *     char cpindex;
     *     char vtable_index;
     *     byte argument_count;
     *     byte wide_argument_count;
     *     char selector_cpindex;
     * };
     **/
    public static class INVOKESUPER_QUICK 
	extends Invocation_Quick { 
        public static final int opcode = JVMConstants.Opcodes.INVOKESUPER_QUICK;
        public static final INVOKESUPER_QUICK singleton = new INVOKESUPER_QUICK();
	INVOKESUPER_QUICK() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.USHORT, TypeCodes.USHORT,
		TypeCodes.UBYTE, TypeCodes.UBYTE, TypeCodes.USHORT);
	    IntValue index = (IntValue) istreamIns[1];
	    IntValue count = computeCount((IntValue)istreamIns[2],
					  (IntValue)istreamIns[3]);
	    RefValue recv = computeStackIn(count);
	    RefValue bp = findBlueprint
		(new CallExp("GET_CONSTANT_BP_RESOLVED_INSTANCE_METHODREF",
			     (Value)istreamIns[0]));
	    invoke(recv, bp, VIRTUAL, index, count);
	}
    public INVOKESUPER_QUICK(int cpIndex, int vtable_index, int selCPIndex, 
            Constants constantPool) {
        this();
        Descriptor.Method desc = constantPool.getMethodrefAt(selCPIndex)
        .asSelector().getDescriptor();
        istreamIns = imm(new ConcreteIntValue(cpIndex, TypeCodes.USHORT),
                new ConcreteIntValue(vtable_index, TypeCodes.USHORT),
                new ConcreteIntValue(desc.getArgumentCount(), TypeCodes.UBYTE),
                new ConcreteIntValue(desc.getWideArgumentCount(), TypeCodes.UBYTE),
                new ConcreteIntValue(selCPIndex, TypeCodes.USHORT));
    }
	/**
	 * Which index of the Constant Pool is accessed by this instruction?
	 **/
	public int getCPIndex(MethodInformation iv) {
	    return istreamIns[0].decodeInt(iv, 1);
	}
	/**
	 * Like <code>getCPIndex(null)<code> this method returns the
	 * index used by a concrete instruction object.
	 * @deprecated
	 **/
	public int getCPIndex() { return getCPIndex(null); }
    public void setCPIndex(int index) {
        if (istreamIns[0] instanceof ConcreteIntValue)
            istreamIns[0] = new ConcreteIntValue(index, TypeCodes.USHORT);
        else
            throw new Error("Not concrete");
    }
	public int getMethodTableIndex(MethodInformation iv) {
	    return iv.getCode().getChar(iv.getPC() + 3);
	}
    public int getMethodTableIndex() {    
        if (istreamIns[1] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    }
    public void setMethodTableIndex(int index) {
        if (istreamIns[1] instanceof ConcreteIntValue)
            istreamIns[1] = new ConcreteIntValue(index, TypeCodes.USHORT);
        else
            throw new Error("Not concrete");
    }
	/**
	 * @return the argument count
	 **/
	public int getArgumentCount(MethodInformation iv) {
	    return (0xFF & iv.getCode().get(iv.getPC() + 5) + 0x100);
	} 
    public int getArgumentCount() {
        if (istreamIns[2] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[2]).intValue();
        else
            throw new Error("Not concrete");
    } 
	/**
	 * @return the wide argument count
	 **/
	public int getWideArgumentCount(MethodInformation iv) {
	    return (0xFF & iv.getCode().get(iv.getPC() + 6) + 0x100);
	} 
    public int getWideArgumentCount() {
        if (istreamIns[3] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[3]).intValue();
        else    
            throw new Error("Not concrete");
    }
	public Selector.Method getSelector(MethodInformation iv,
					   Constants cp) {
	    try {
		return cp.getMethodrefAt(iv.getCode().getChar(iv.getPC()+7))
		    .asSelector();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
    public Selector.Method getSelector(Constants constantPool) {
        if (istreamIns[4] instanceof ConcreteIntValue) {
            int cpIndex = ((ConcreteIntValue)istreamIns[4]).intValue();
            try {
                return constantPool.getMethodrefAt(cpIndex).asSelector();
            } catch (ConstantPool.AccessException ae) {
                throw ae.fatal();
            }
        } else
            throw new Error("Not concrete");
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * FORMAT:
     * INVOKEINTERFACE_QUICK {
     *     byte opcode; (= INVOKEINTERFACE_QUICK opcode)
     *     char iftable_index;
     *     byte argCount;
     *     byte wideArgCount;
     *     char selector_cpindex;
     **/
    public static class INVOKEINTERFACE_QUICK 
	extends Invocation_Quick { 
        public static final int opcode = JVMConstants.Opcodes.INVOKEINTERFACE_QUICK;
        public static final INVOKEINTERFACE_QUICK singleton = new INVOKEINTERFACE_QUICK();
	INVOKEINTERFACE_QUICK() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.USHORT, TypeCodes.UBYTE, 
			     TypeCodes.UBYTE, TypeCodes.USHORT);
	    IntValue index = (IntValue) istreamIns[0];
	    IntValue count = computeCount((IntValue)istreamIns[1], 
					  (IntValue)istreamIns[2]);
	    RefValue recv = computeStackIn(count);
	    RefValue bp = findBlueprint(new BlueprintAccessExp(recv));
	    invoke(recv, bp, INTERFACE, index, count);
	}
    public INVOKEINTERFACE_QUICK(int iftable_index, int selCPIndex, 
            Constants constantPool) {
        this();
        Descriptor.Method desc = constantPool.getMethodrefAt(selCPIndex)
        .asSelector().getDescriptor();
        istreamIns = imm(new ConcreteIntValue(iftable_index, TypeCodes.USHORT),
                new ConcreteIntValue(desc.getArgumentCount(), TypeCodes.UBYTE),
                new ConcreteIntValue(desc.getWideArgumentCount(), TypeCodes.UBYTE),
                new ConcreteIntValue(selCPIndex, TypeCodes.USHORT));
    }
	public int getMethodTableIndex(MethodInformation iv) {
	    return iv.getCode().getChar(iv.getPC() + 1);
	}
    public int getMethodTableIndex() {    
        if (istreamIns[0] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[0]).intValue();
        else
            throw new Error("Not concrete");
    }
    public void setMethodTableIndex(int index) {
        if (istreamIns[0] instanceof ConcreteIntValue)
            istreamIns[0] = new ConcreteIntValue(index, TypeCodes.USHORT);
        else
            throw new Error("Not concrete");
    }
	public int getArgumentCount(MethodInformation iv) {
	    return iv.getCode().get(iv.getPC() + 3);
	}
    public int getArgumentCount() {
        if (istreamIns[1] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        else
            throw new Error("Not concrete");
    } 
	public int getWideArgumentCount(MethodInformation iv) {
	    return iv.getCode().get(iv.getPC() + 4);
	} 
    public int getWideArgumentCount() {
        if (istreamIns[2] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[2]).intValue();
        else    
            throw new Error("Not concrete");
    }

	public Selector.Method getSelector(MethodInformation iv,
					   Constants cp) {
	    try {
		return cp.getMethodrefAt(iv.getCode().getChar(iv.getPC()+5))
		    .asSelector();
	    } catch (ConstantPool.AccessException ae) {
		throw ae.fatal();
	    }
	}
    public Selector.Method getSelector(Constants constantPool) {
        if (istreamIns[3] instanceof ConcreteIntValue) {
            int cpIndex = ((ConcreteIntValue)istreamIns[3]).intValue();
            try {
                return constantPool.getMethodrefAt(cpIndex).asSelector();
            } catch (ConstantPool.AccessException ae) {
                throw ae.fatal();
            }
        } else
            throw new Error("Not concrete");
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    /**
     * ROLL as defined in Postscript Language Reference Manual, Chapter 8,
     * where <code>span</code> is <em>n</em> and <code>count</code> is
     * <em>j</em>. Direction reminder: positive <em>j</em> repeatedly
     * (<em>j</em> times) rolls top element below deepest; negative <em>j</em>
     * repeatedly (<em>-j</em> times) rolls deepest element to top.<p>
     *
     * FIXME: Why is this not a {@link ovm.services.bytecode.Instruction.StackManipulation}?
     **/
    public static class ROLL
	extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.ROLL;
        public static final ROLL singleton = new ROLL();
	ROLL() {
	    super(opcode);
	    istreamIns = imm(new IntValue(TypeCodes.USHORT),
			     new IntValue(TypeCodes.BYTE));
	    // FIXME stackIns/stackOuts depend on ROLL arguments.  Is this
	    // actually used?
	    evals = eval(new CallExp("ROLL", (Value)istreamIns[0], (Value) istreamIns[1]));
	}
    ROLL(int span, int count) {
        this();
        istreamIns[0] = new ConcreteIntValue(span, TypeCodes.USHORT);
        istreamIns[1] = new ConcreteIntValue(count, TypeCodes.BYTE);
    }
	public char getSpan(MethodInformation iv) {
	    return  iv.getCode().getChar(iv.getPC() + 1);
	}
	public int getCount(MethodInformation iv) {
	    return  iv.getCode().get(iv.getPC() + 3);
	}
    public char getSpan() {
        if (istreamIns[0] instanceof ConcreteIntValue) {
            return (char)((ConcreteIntValue)istreamIns[0]).intValue();
        } else
            throw new Error("Not concrete");
    }
    public int getCount() {
        if (istreamIns[1] instanceof ConcreteIntValue) {
            return ((ConcreteIntValue)istreamIns[1]).intValue();
        } else
            throw new Error("Not concrete");
    }
    public String toString(MethodInformation iv) {
        return "ROLL " + (int)getSpan(iv) + ", " + getCount(iv);
    }
    public String toString(Constants _) {
        return "ROLL " + (int)getSpan() + ", " + getCount();
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /*
      Copy n bytes on top of stack. It is the responsibility of the caller
      that wide objects on the stack would not be broken - that whole wide
      objects will be copied.
    */
    public static class COPY
	extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.COPY;
        public static final COPY singleton = new COPY();
	COPY() {
	    super(opcode);
	    istreamIns = imm(new IntValue(TypeCodes.BYTE));
	    // FIXME stackIns/stackOuts depend on ROLL arguments.  Is this
	    // actually used?
	    evals = eval(new CallExp("COPY", (Value)istreamIns[0]));
	}

        COPY(int offset) {
          this();
          istreamIns[0] = new ConcreteIntValue(offset, TypeCodes.BYTE);
        }
	public byte getOffset(MethodInformation iv) {
	    return  iv.getCode().get(iv.getPC() + 1);
	}

	public byte getOffset() {
          if (istreamIns[0] instanceof ConcreteIntValue) {
            return (byte)((ConcreteIntValue)istreamIns[0]).intValue();
          } else
            throw new Error("Not concrete");
        }
    
        public String toString(MethodInformation iv) {
          return "COPY " + (int)getOffset(iv);
        }
        
        public String toString(Constants _) {
          return "COPY " + (int)getOffset();
        }
        
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Invoke a builtin routine.
     *
     * @see ovm.core.execution.Interpreter
     * @see ovm.core.services.memory.VM_Address
     * @see ovm.core.services.memory.VM_Word
     * @see ovm.services.bytecode.JVMConstants.InvokeSystemArguments
     * @see ovm.services.bytecode.JVMConstants.WordOps
     * @see ovm.services.bytecode.JVMConstants.DereferenceOps
     **/
    public static class INVOKE_SYSTEM
	extends ExceptionThrowerImpl { 
        public static final int opcode = JVMConstants.Opcodes.INVOKE_SYSTEM;
        public static final INVOKE_SYSTEM singleton = new INVOKE_SYSTEM();
	INVOKE_SYSTEM() {
	    super(opcode, new TypeName.Scalar[] {
		// Should this be here?  I guess it is needed.
		JavaNames.ovm_core_domain_WildcardException
	    });
	    istreamIns = imm(TypeCodes.UBYTE, TypeCodes.UBYTE);
	}
	INVOKE_SYSTEM(int methodIndex, int optype) {
	    this();
	    istreamIns[0] = new ConcreteIntValue(methodIndex, TypeCodes.UBYTE);
	    istreamIns[1] = new ConcreteIntValue(optype, TypeCodes.UBYTE);
	}
	public int getMethodIndex(MethodInformation iv) {
	    return (isPrototype()
		    ? iv.getCode().get(iv.getPC() + 1) & 0xff
		    : ((ConcreteIntValue)istreamIns[0]).intValue());
	}
	public int getOpType(MethodInformation iv) {
	    return (isPrototype()
		    ? iv.getCode().get(iv.getPC() + 2) & 0xff
		    : ((ConcreteIntValue)istreamIns[1]).intValue());
	}
	public int getMethodIndex() {
	    return getMethodIndex(null);
	}
	public int getOpType() {
	    return getOpType(null);
	}

	public String toString(MethodInformation iv, Constants cp) {
	    return (super.toString() + "\t"
		    + Integer.toHexString(getMethodIndex(iv)) + ", "
		    + Integer.toHexString(getOpType(iv)));
	}

	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Invoke a C function.
     *
     * @see ovm.core.execution.Native
     * @see s3.services.bytecode.ovmify.NativeCallGenerator
     **/
    public static class INVOKE_NATIVE
	extends Instruction { 
        public static final int opcode = JVMConstants.Opcodes.INVOKE_NATIVE;
        public static final INVOKE_NATIVE singleton = new INVOKE_NATIVE();
	INVOKE_NATIVE() {
	    super(opcode);
	    istreamIns = imm(TypeCodes.UBYTE);
	}
    INVOKE_NATIVE(int methodIndex) {
        this();
        istreamIns[0] = new ConcreteIntValue(methodIndex, TypeCodes.UBYTE);
    }
	public int getMethodIndex(MethodInformation iv) {
	    return  iv.getCode().get(iv.getPC() + 1) & 0xff;
	}
    public int getMethodIndex() {
        if (istreamIns[0] instanceof ConcreteIntValue)
            return ((ConcreteIntValue)istreamIns[0]).intValue();
        else
            throw new Error("Not concrete");
    }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    /**
     * Unsafe type conversions.<p>
     *
     * FIXME: {@link #getResultTypeName} should really be called
     * <code>getResultType</code>, but sadly, ConstantClass is not a
     * subtype of TypeName.
     **/
    public interface Fiat {
        public TypeName getResultTypeName(MethodInformation iv, Constants cp);
    }
    
    /**
     * Fiat instructions consume values of arbitrary types and produce
     * values of new types with the same bitwise representation.
     * Expressing this in the specification IR is only tricky in that
     * we need to assign a type to the operand.  If we choose a random
     * type of the appropriate width, the interpreter and j2c will
     * both be happy.  (Actually, for primitive conversions, the
     * interpreter is most happy popping exactly the same type it will
     * push).
     **/
    public static class AFIAT extends ConstantPoolRead implements Fiat { 
        public static final int opcode = JVMConstants.Opcodes.AFIAT;
        public static final AFIAT singleton = new AFIAT();
        public AFIAT() {
            super(opcode);
            istreamIns = imm(new CPIndexValue(JVMConstants.CONSTANT_Class));
            // KP: shouldn't the refs be the same?
            stackIns = stack( new IntValue());
            stackOuts = stack( new RefValue(new ReinterpretExp(stackIns[0])));
        }
        public AFIAT(int cpindex) {
            this();
            istreamIns[0] = new ConcreteCPIndexValue(cpindex);
        }
        
        public TypeName getResultTypeName(MethodInformation iv,
					  Constants cp) {
	    return cp.getClassAt(getCPIndex(iv)).asTypeName().asCompound();
        }

	public ConstantClass getConstantClass(MethodInformation iv,
					      Constants cp) {
	    return cp.getClassAt(getCPIndex(iv));
	}
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }

    public abstract static class PrimFiat extends Instruction implements Fiat {
	public PrimFiat(int opcode, ValueFactory vf) {
	    super(opcode);
	    stackIns = stack(vf.make(null));
	    stackOuts = stack(vf.make(new ReinterpretExp(stackIns[0])));
	}
	public TypeName getResultTypeName(MethodInformation iv,
					  Constants cp) {
	    return TypeName.Primitive.make(((StreamableValue) stackOuts[0])
					   .getType());
	}
    }
	
    public abstract static class WidePrimFiat extends Instruction implements Fiat {
	public WidePrimFiat(int opcode, ValueFactory vf) {
	    super(opcode);
	    stackIns = stack(vf.make(null));
	    stackOuts = stack(vf.make(new ReinterpretExp(stackIns[0])));
	}
	public TypeName getResultTypeName(MethodInformation iv,
					  Constants cp) {
	    return TypeName.Primitive.make(((StreamableValue) stackOuts[0])
					   .getType());
	}
    }
	
    public static class IFIAT extends PrimFiat { 
        public static final int opcode = JVMConstants.Opcodes.IFIAT;
        public static final IFIAT singleton = new IFIAT();
        public IFIAT() { super(opcode, intFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class FFIAT extends PrimFiat { 
        public static final int opcode = JVMConstants.Opcodes.FFIAT;
        public static final FFIAT singleton = new FFIAT();
        public FFIAT() { super(opcode, floatFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class LFIAT extends WidePrimFiat { 
        public static final int opcode = JVMConstants.Opcodes.LFIAT;
        public static final LFIAT singleton = new LFIAT();
        public LFIAT() { super(opcode, longFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }
    public static class DFIAT extends WidePrimFiat { 
        public static final int opcode = JVMConstants.Opcodes.DFIAT;
        public static final DFIAT singleton = new DFIAT();
        public DFIAT() { super(opcode, doubleFactory); }
	public void accept(Instruction.Visitor v) { v.visit(this); }
    }


    /* ************** helper methods ************ */

    public  static  IfExp buildCMP(Value lhs, Value rhs) {
	return new IfExp(new CondExp(lhs, ">", rhs),
			 ConcreteIntValue.ONE,
			 new IntValue(new IfExp(new CondExp(lhs, "==", rhs),
						ConcreteIntValue.ZERO,
						ConcreteIntValue.MINUSONE)));
    }

    /*  only for FCMPG/DCMPG, if one of the operands is NaN, 1 is pushed.
        In the case of FCMPL/DCMPL the general buildCMP() works OK. */
    public  static  IfExp buildCMPG(Value lhs, Value rhs) {
	return new IfExp(new CondExp(lhs, "<", rhs),
			 ConcreteIntValue.MINUSONE,
			 new IntValue(new IfExp(new CondExp(lhs, "==", rhs),
						ConcreteIntValue.ZERO,
						ConcreteIntValue.ONE)));
    }

    public static Value instanceTest(RefValue ptr, IntValue link)
    {
	return new IntValue(new CallExp("is_subtype_of",
					new Value(new BlueprintAccessExp(ptr)),
					new Value(new CPAccessExp(link))));
    }

    public static Value throwException(int code, IntValue meta) {
	return new Value(new CSACallExp("generateThrowable",
					ConcreteIntValue.make(code),
					meta));
    }
    public static Value throwException(int code) {
	return throwException(code, ConcreteIntValue.ZERO);
    }
	
    static Value[] stack(Value v1, Value v2, Value v3) {
	return ((v1 instanceof WideValue)
		? ((v2 instanceof WideValue)
		   ? ((v3 instanceof WideValue)
		      ? new Value[] { v1, ((WideValue) v1).getSecondHalf(),
				      v2, ((WideValue) v2).getSecondHalf(),
				      v3, ((WideValue) v3).getSecondHalf() }
		      : new Value[] { v1, ((WideValue) v1).getSecondHalf(),
				      v2, ((WideValue) v2).getSecondHalf(),
				      v3 })
		   : ((v3 instanceof WideValue)
		      ? new Value[] { v1, ((WideValue) v1).getSecondHalf(),
				      v2,
				      v3, ((WideValue) v3).getSecondHalf() }
		      : new Value[] { v1, ((WideValue) v1).getSecondHalf(),
				      v2,
				      v3 }))
		: ((v2 instanceof WideValue)
		   ? ((v3 instanceof WideValue)
		      ? new Value[] { v1,
				      v2, ((WideValue) v2).getSecondHalf(),
				      v3, ((WideValue) v3).getSecondHalf() }
		      : new Value[] { v1, ((WideValue) v1).getSecondHalf(),
				      v2, ((WideValue) v2).getSecondHalf(),
				      v3 })
		   : ((v3 instanceof WideValue)
		      ? new Value[] { v1,
				      v2,
				      v3, ((WideValue) v3).getSecondHalf() }
		      : new Value[] { v1,
				      v2,
				      v3 })));
    }

    static Value[] stack(Value v1, Value v2) {
	return ((v1 instanceof WideValue)
		? ((v2 instanceof WideValue)
		   ? new Value[] { v1, ((WideValue) v1).getSecondHalf(),
				   v2, ((WideValue) v2).getSecondHalf() }
		   : new Value[] { v1, ((WideValue) v1).getSecondHalf(),
				   v2  })
		: ((v2 instanceof WideValue)
		   ? new Value[] { v1,
				   v2, ((WideValue) v2).getSecondHalf() }
		   : new Value[] { v1, v2 }));
    }

    static Value[] stack(Value v1) {
	return ((v1 instanceof WideValue)
		? new Value[] { v1, ((WideValue) v1).getSecondHalf() }
		: new Value[] { v1 });
    }

    static Value[] eval(ValueSource e1, ValueSource e2, ValueSource e3) {
	return new Value[] { new Value(e1), new Value(e2), new Value(e3) };
    }

    static Value[] eval(ValueSource e1, ValueSource e2) {
	return new Value[] { new Value(e1), new Value(e2) };
    }

    static Value[] eval(ValueSource e1) {
	return new Value[] { new Value(e1) };
    }

    /**
     * Evaluate a new side-effecting expression before all existing
     * ones.
     * 
     * @param e the expression to add
     *
     * <!-- void -->
     *
     */
    public void pushEval(ValueSource e) {
	Value[] nevals = new Value[evals.length + 1];
	nevals[0] = new Value(e);
	System.arraycopy(evals, 0, nevals, 1, evals.length);
	evals = nevals;
    }

    static StreamableValue[] imm(StreamableValue v1, StreamableValue v2,
				 StreamableValue v3, StreamableValue v4,
				 StreamableValue v5) {
	return new StreamableValue[] { v1, v2, v3, v4, v5 };
    }
    static StreamableValue[] imm(StreamableValue v1, StreamableValue v2,
				 StreamableValue v3, StreamableValue v4) {
	return new StreamableValue[] { v1, v2, v3, v4 };
    }
    static StreamableValue[] imm(StreamableValue v1, StreamableValue v2,
				 StreamableValue v3) {
	return new StreamableValue[] { v1, v2, v3 };
    }
    static StreamableValue[] imm(StreamableValue v1, StreamableValue v2) {
	return new StreamableValue[] { v1, v2 };
    }
    static StreamableValue[] imm(StreamableValue v1) {
	return new StreamableValue[] { v1 };
    }
    static StreamableValue[] imm(char t1, char t2, char t3, char t4) {
	return imm(new IntValue(t1), new IntValue(t2),
		   new IntValue(t3), new IntValue(t4));
    }
    static StreamableValue[] imm(char t1, char t2, char t3, char t4, char t5) {
	return imm(new IntValue(t1), new IntValue(t2),
		   new IntValue(t3), new IntValue(t4), new IntValue(t5));
    }
    static StreamableValue[] imm(char t1, char t2, char t3) {
	return imm(new IntValue(t1), new IntValue(t2), new IntValue(t3));
    }
    static StreamableValue[] imm(char t1, char t2)
    { return imm(new IntValue(t1), new IntValue(t2)); }
    static StreamableValue[] imm(char t1) { return imm(new IntValue(t1)); }

    static Temp[] temp(Temp t1) { return new Temp[] { t1 }; }

    static TypeName.Scalar[] mayThrow(int[] exCode) {
	TypeName.Scalar[]  ex = new TypeName.Scalar[exCode.length];
	for (int i = 0; i < ex.length; i++)
	    ex[i] = JavaNames.throwables[exCode[i]];
	return ex;
    }
    static TypeName.Scalar[] mayThrow(int ex1) {
	return mayThrow(new int[] { ex1 });
    }
    static TypeName.Scalar[] mayThrow(int ex1, int ex2) {
	return mayThrow(new int[] { ex1, ex2 });
    }
    static TypeName.Scalar[] mayThrow(int ex1, int ex2, int ex3) {
	return mayThrow(new int[] { ex1, ex2, ex3 });
    }
    static TypeName.Scalar[] mayThrow(int ex1, int ex2, int ex3, int ex4) {
	return mayThrow(new int[] { ex1, ex2, ex3, ex4 });
    }
	
// FIXME NaN handling       

    public static interface Iterator {
	
	/**
	 * Get the next instruction to visit, null for none.
	 **/
	public Instruction next();
    }

    /**
     * The base class for instruction visitors.  The behavior if this
     * class is much similar to that of {@link InstructionVisitor}
     * than BCEL's <code>Visitor</code> and
     * <code>EmptyVisitor</code>.  This visitor class works more
     * reliably than the <code>Runabout</code>-based
     * <code>InstructionVisitor</code> and subsumes the behavior of
     * BCEL-style instruction visitors.<p>
     *
     * In this implemenation, {@link Instruction#accept} calls the
     * most-specific <code>visit</code> method, and the default
     * <code>visit</code> implementation recursively calls
     * <code>visit</code> on the directy supertypes of it's argument
     * types.<p>
     *
     * In contrast, BCEL's <code>accept</code> methods call all
     * applicable <code>visit</code> methods starting with the most
     * general, and ending with the most specific.  The same behavior
     * can be achieved with <cod>Visitor</code> adding
     * <code>super.visit(instruction)</code> to the start of each
     * <code>visit</code> method in a concrete visitor type.
     * <code>Visitor</code> generalizes BCEL's vist pattern in that a
     * <code>visit</code> method is given the choice of if and when
     * more general <code>visit</code> methods should be run.<p>
     *
     * <code>Runabout</code> and <code>InstructionVisitor</code>
     * provide similar functionality, in that they dispatch to the
     * most-specific visit method based on an argument type.  However,
     * in a class hiearchy derived from <code>Runabout</code> there is
     * no way to call the next most specific method, whereas with
     * <code>Visitor</code> a normal <code>super</code> suffices.<p>
     *
     * <b>Note</b> this code is very brittle.  Whenever the instruction
     * hierarchy changes in <b>any</b> way, <code>Visitor</code>
     * <b>must</b> be updated to match the behavior described in the
     * second paragraph.  Errors caused by broken double-dispatch can
     * be extremely hard to track down.  At some point, the
     * <code>Visitor</code> class may be replaced by a custom
     * dispatching policy using PolyD.
     **/
    public abstract static class Visitor {
	/** By default, do nothing **/
	public void visit(Instruction i)       { }
	/** By default, do nothing **/
	public void visit(Allocation i)        { }
	/** By default, do nothing **/
	public void visit(ExceptionThrower i)  { }
	/** By default, do nothing **/
	public void visit(FlowChange i)        { }
	/** By default, do nothing **/
	public void visit(Fiat i)              { }
	public void visit(ExceptionThrowerImpl i) {
	    visit((Instruction) i);
	    visit((ExceptionThrower) i);
	}
	public void visit(LocalAccess i)       { visit((Instruction) i); }
	public void visit(LocalRead i)         { visit((LocalAccess) i); }
	public void visit(LocalWrite i)        { visit((LocalAccess) i); }
	public void visit(ArrayAccess i) { visit((ExceptionThrowerImpl) i); }
	public void visit(ArrayLoad i)         { visit((ArrayAccess) i); }
	public void visit(ArrayStore i)        { visit((ArrayAccess) i); }
	public void visit(FieldAccess_Quick i) {
	    visit((ExceptionThrowerImpl) i);
	}
	public void visit(FieldGet_Quick i)    { visit((FieldAccess_Quick) i); }
	public void visit(FieldPut_Quick i)    { visit((FieldAccess_Quick) i); }
	public void visit(ReturnValue i)       { visit((ReturnInstruction) i); }
        public void visit(ReturnInstruction i) {
	    visit((FlowEnd) i);
	    visit((ExceptionThrower) i);
	}
	public void visit(FlowEnd i)           { visit((Instruction) i); }
	public void visit(ArithmeticInstruction i) { visit((Instruction) i); }
	public void visit(AsymetricBinOp i)    {
	    visit((ArithmeticInstruction) i);
	}
	public void visit(BinOp i)             { visit((AsymetricBinOp) i); }
	public void visit(IntegerDivision i)   { visit((BinOp) i); }
	public void visit(Negation i){ visit((ArithmeticInstruction) i); }
	public void visit(Conversion i)        { visit((Instruction) i); }
	public void visit(Synchronization i)   {
	    visit((ExceptionThrowerImpl) i);
	}
	public void visit(Invocation_Quick i)  {
	    visit((ExceptionThrowerImpl) i);
	}
	public void visit(ConstantLoad i)      { visit((Instruction) i); }
	public void visit(IConstantLoad i)     { visit((ConstantLoad) i); }
	public void visit(DConstantLoad i)     { visit((ConstantLoad) i); }
	public void visit(FConstantLoad i)     { visit((ConstantLoad) i); }
	public void visit(LConstantLoad i)     { visit((ConstantLoad) i); }
	public void visit(FlowChangeImpl i)    {
	    visit((Instruction) i);
	    visit((FlowChange) i);
	}
	public void visit(BranchInstruction i) { visit((FlowChangeImpl) i); }
	public void visit(ConditionalJump i)   { visit((BranchInstruction) i); }
	public void visit(If i)                { visit((ConditionalJump) i); }
	public void visit(IfZ i)               { visit((If) i); }
	public void visit(IfCmp i)             { visit((If) i); }
	public void visit(StackManipulation i) { visit((Instruction) i); }
	public void visit(ConstantPoolRead i)  { visit((Instruction) i); }
	public void visit(Resolution i)        {
	    visit((ConstantPoolRead) i);
	    visit((ExceptionThrower) i);
	}
	public void visit(ConstantPoolPush i)  { visit((Resolution) i); }
	public void visit(Invocation i)        { visit((Resolution) i); }
	public void visit(FieldAccess i)       { visit((Resolution) i); }
	public void visit(UnconditionalJump i) { visit((BranchInstruction) i); }
	public void visit(ConstantPoolLoad i)  { visit((ConstantPoolRead) i); }
	public void visit(POLLCHECK i)         { visit((Instruction) i); }
	public void visit(INB i)               { visit((Instruction) i); }
	public void visit(OUTB i)              { visit((Instruction) i); }	
	public void visit(NULLCHECK i)         { 
	  visit((StackManipulation) i);
	  visit((ExceptionThrower) i);
	}
        public void visit(INCREMENT_COUNTER i)         { 
	  visit((StackManipulation) i);
	}
	public void visit(UNIMPLEMENTED i)     { visit((Instruction) i); }
	public void visit(ALOAD i)             { visit((LocalRead) i); }
	public void visit(ILOAD i)             { visit((LocalRead) i); }
	public void visit(FLOAD i)             { visit((LocalRead) i); }
	public void visit(DLOAD i)             { visit((LocalRead) i); }
	public void visit(LLOAD i)             { visit((LocalRead) i); }
	public void visit(ALOAD_0 i)           { visit((LocalRead) i); }
	public void visit(ALOAD_1 i)           { visit((LocalRead) i); }
	public void visit(ALOAD_2 i)           { visit((LocalRead) i); }
	public void visit(ALOAD_3 i)           { visit((LocalRead) i); }
	public void visit(ILOAD_0 i)           { visit((LocalRead) i); }
	public void visit(ILOAD_1 i)           { visit((LocalRead) i); }
	public void visit(ILOAD_2 i)           { visit((LocalRead) i); }
	public void visit(ILOAD_3 i)           { visit((LocalRead) i); }
	public void visit(FLOAD_0 i)           { visit((LocalRead) i); }
	public void visit(FLOAD_1 i)           { visit((LocalRead) i); }
	public void visit(FLOAD_2 i)           { visit((LocalRead) i); }
	public void visit(FLOAD_3 i)           { visit((LocalRead) i); }
	public void visit(DLOAD_0 i)           { visit((LocalRead) i); }
	public void visit(DLOAD_1 i)           { visit((LocalRead) i); }
	public void visit(DLOAD_2 i)           { visit((LocalRead) i); }
	public void visit(DLOAD_3 i)           { visit((LocalRead) i); }
	public void visit(LLOAD_0 i)           { visit((LocalRead) i); }
	public void visit(LLOAD_1 i)           { visit((LocalRead) i); }
	public void visit(LLOAD_2 i)           { visit((LocalRead) i); }
	public void visit(LLOAD_3 i)           { visit((LocalRead) i); }
	public void visit(RETURN i)            { visit((ReturnInstruction) i); }
	public void visit(IRETURN i)           { visit((ReturnValue) i); }
	public void visit(ARETURN i)           { visit((ReturnValue) i); }
	public void visit(FRETURN i)           { visit((ReturnValue) i); }
	public void visit(DRETURN i)           { visit((ReturnValue) i); }
	public void visit(LRETURN i)           { visit((ReturnValue) i); }
	public void visit(ASTORE i)            { visit((LocalWrite) i); }
	public void visit(ISTORE i)            { visit((LocalWrite) i); }
	public void visit(LSTORE i)            { visit((LocalWrite) i); }
	public void visit(FSTORE i)            { visit((LocalWrite) i); }
	public void visit(DSTORE i)            { visit((LocalWrite) i); }
	public void visit(ASTORE_0 i)          { visit((LocalWrite) i); }
	public void visit(ASTORE_1 i)          { visit((LocalWrite) i); }
	public void visit(ASTORE_2 i)          { visit((LocalWrite) i); }
	public void visit(ASTORE_3 i)          { visit((LocalWrite) i); }
	public void visit(ISTORE_0 i)          { visit((LocalWrite) i); }
	public void visit(ISTORE_1 i)          { visit((LocalWrite) i); }
	public void visit(ISTORE_2 i)          { visit((LocalWrite) i); }
	public void visit(ISTORE_3 i)          { visit((LocalWrite) i); }
	public void visit(LSTORE_0 i)          { visit((LocalWrite) i); }
	public void visit(LSTORE_1 i)          { visit((LocalWrite) i); }
	public void visit(LSTORE_2 i)          { visit((LocalWrite) i); }
	public void visit(LSTORE_3 i)          { visit((LocalWrite) i); }
	public void visit(FSTORE_0 i)          { visit((LocalWrite) i); }
	public void visit(FSTORE_1 i)          { visit((LocalWrite) i); }
	public void visit(FSTORE_2 i)          { visit((LocalWrite) i); }
	public void visit(FSTORE_3 i)          { visit((LocalWrite) i); }
	public void visit(DSTORE_0 i)          { visit((LocalWrite) i); }
	public void visit(DSTORE_1 i)          { visit((LocalWrite) i); }
	public void visit(DSTORE_2 i)          { visit((LocalWrite) i); }
	public void visit(DSTORE_3 i)          { visit((LocalWrite) i); }
	public void visit(AALOAD i)            { visit((ArrayLoad) i); }
	public void visit(IALOAD i)            { visit((ArrayLoad) i); }
	public void visit(FALOAD i)            { visit((ArrayLoad) i); }
	public void visit(DALOAD i)            { visit((ArrayLoad) i); }
	public void visit(LALOAD i)            { visit((ArrayLoad) i); }
	public void visit(BALOAD i)            { visit((ArrayLoad) i); }
	public void visit(CALOAD i)            { visit((ArrayLoad) i); }
	public void visit(SALOAD i)            { visit((ArrayLoad) i); }
	public void visit(READ_BARRIER i) { visit((ExceptionThrowerImpl) i); }
        public void visit(NONCHECKING_TRANSLATING_READ_BARRIER i) { visit((Instruction) i); }
        public void visit(CHECKING_TRANSLATING_READ_BARRIER i) { visit((Instruction) i); }        
	public void visit(AASTORE i)           { visit((ArrayStore) i); }
	public void visit(AASTORE_WITH_BARRIER i) { visit((AASTORE) i); }
	public void visit(UNCHECKED_AASTORE i) { visit((UncheckedArrayStore) i); }
	public void visit(IASTORE i)           { visit((ArrayStore) i); }
	public void visit(FASTORE i)           { visit((ArrayStore) i); }
	public void visit(DASTORE i)           { visit((ArrayStore) i); }
	public void visit(LASTORE i)           { visit((ArrayStore) i); }
	public void visit(BASTORE i)           { visit((ArrayStore) i); }
	public void visit(SASTORE i)           { visit((ArrayStore) i); }
	public void visit(CASTORE i)           { visit((ArrayStore) i); }
	public void visit(ACONST_NULL i)       { visit((ConstantLoad) i); }
	public void visit(ICONST_M1 i)         { visit((IConstantLoad) i); }
	public void visit(ICONST_0 i)          { visit((IConstantLoad) i); }
	public void visit(ICONST_1 i)          { visit((IConstantLoad) i); }
	public void visit(ICONST_2 i)          { visit((IConstantLoad) i); }
	public void visit(ICONST_3 i)          { visit((IConstantLoad) i); }
	public void visit(ICONST_4 i)          { visit((IConstantLoad) i); }
	public void visit(ICONST_5 i)          { visit((IConstantLoad) i); }
	public void visit(BIPUSH i)            { visit((IConstantLoad) i); }
	public void visit(SIPUSH i)            { visit((IConstantLoad) i); }
	public void visit(LDC_INT_QUICK i)     { visit((IConstantLoad) i); }
	public void visit(DCONST_0 i)          { visit((DConstantLoad) i); }
	public void visit(DCONST_1 i)          { visit((DConstantLoad) i); }
	public void visit(LDC_DOUBLE_QUICK i)  { visit((DConstantLoad) i); }
	public void visit(FCONST_0 i)          { visit((FConstantLoad) i); }
	public void visit(FCONST_1 i)          { visit((FConstantLoad) i); }
	public void visit(FCONST_2 i)          { visit((FConstantLoad) i); }
	public void visit(LDC_FLOAT_QUICK i)   { visit((FConstantLoad) i); }
	public void visit(LCONST_0 i)          { visit((LConstantLoad) i); }
	public void visit(LCONST_1 i)          { visit((LConstantLoad) i); }
	public void visit(LDC_LONG_QUICK i)    { visit((LConstantLoad) i); }
	public void visit(LDC i)               { visit((ConstantPoolLoad) i); }
	public void visit(LDC_REF_QUICK i)     { visit((ConstantPoolLoad) i); }
	public void visit(LDC_W i)             { visit((ConstantPoolLoad) i); }
	public void visit(LDC_W_REF_QUICK i)   { visit((ConstantPoolLoad) i); }
	public void visit(LDC2_W i)            { visit((ConstantPoolLoad) i); }
	public void visit(LOAD_SHST_FIELD i)   { visit((ConstantPoolLoad) i); }
	public void visit(LOAD_SHST_METHOD i)  { visit((ConstantPoolLoad) i); }
	public void visit(LOAD_SHST_FIELD_QUICK i) {
	    visit((ConstantPoolLoad) i);
	}
	public void visit(LOAD_SHST_METHOD_QUICK i) {
	    visit((ConstantPoolLoad) i);
	}
	public void visit(SINGLEANEWARRAY i)   {
	    visit((Allocation) i);
	    visit((ConstantPoolPush) i);
	}
	public void visit(ANEWARRAY i)         {
	    visit((Allocation) i);
	    visit((Resolution) i);
	}
	public void visit(ARRAYLENGTH i) { visit((ExceptionThrowerImpl) i); }
	public void visit(MULTIANEWARRAY i)    {
	    visit((Allocation) i);
	    visit((ConstantPoolPush) i);
	}
	public void visit(NEWARRAY i)	       {
	    visit((Allocation) i);
	    visit((ExceptionThrowerImpl) i);
	}
	public void visit(D2F i)               { visit((Conversion) i); }
	public void visit(D2I i)               { visit((Conversion) i); }
	public void visit(D2L i)               { visit((Conversion) i); }
	public void visit(F2D i)               { visit((Conversion) i); }
	public void visit(F2L i)               { visit((Conversion) i); }
	public void visit(F2I i)               { visit((Conversion) i); }
	public void visit(I2B i)               { visit((Conversion) i); }
	public void visit(I2C i)               { visit((Conversion) i); }
	public void visit(I2S i)               { visit((Conversion) i); }
	public void visit(I2L i)               { visit((Conversion) i); }
	public void visit(I2F i)               { visit((Conversion) i); }
	public void visit(I2D i)               { visit((Conversion) i); }
	public void visit(L2I i)               { visit((Conversion) i); }
	public void visit(L2D i)               { visit((Conversion) i); }
	public void visit(L2F i)               { visit((Conversion) i); }
	public void visit(GETFIELD i)          { visit((FieldAccess) i); }
	public void visit(GETSTATIC i)         { visit((FieldAccess) i); }
	public void visit(PUTFIELD i)          { visit((FieldAccess) i); }
	public void visit(PUTFIELD_WITH_BARRIER_REF i) { visit((PUTFIELD) i); }
	public void visit(PUTSTATIC i)         { visit((FieldAccess) i); }
	public void visit(PUTSTATIC_WITH_BARRIER_REF i){ visit((PUTSTATIC) i); }
	public void visit(DUP i)               { visit((StackManipulation) i); }
	public void visit(DUP_X1 i)            { visit((StackManipulation) i); }
	public void visit(DUP_X2 i)            { visit((StackManipulation) i); }
	public void visit(DUP2 i)              { visit((StackManipulation) i); }
	public void visit(DUP2_X1 i)           { visit((StackManipulation) i); }
	public void visit(DUP2_X2 i)           { visit((StackManipulation) i); }
	public void visit(POP i)               { visit((StackManipulation) i); }
	public void visit(POP2 i)              { visit((StackManipulation) i); }
	public void visit(SWAP i)              { visit((StackManipulation) i); }
	public void visit(ROLL i)              { visit((Instruction) i); }
	public void visit(COPY i)              { visit((Instruction) i); }	
	public void visit(DADD i)              { visit((BinOp) i); }
	public void visit(DSUB i)              { visit((BinOp) i); }
	public void visit(DMUL i)              { visit((BinOp) i); }
	public void visit(DDIV i)              { visit((BinOp) i); }
	public void visit(DREM i)              { visit((BinOp) i); }
	public void visit(IADD i)              { visit((BinOp) i); }
	public void visit(ISUB i)              { visit((BinOp) i); }
	public void visit(IMUL i)              { visit((BinOp) i); }
	public void visit(IDIV i)              { visit((IntegerDivision) i); }
	public void visit(IREM i)              { visit((IntegerDivision) i); }
	public void visit(FADD i)              { visit((BinOp) i); }
	public void visit(FSUB i)              { visit((BinOp) i); }
	public void visit(FMUL i)              { visit((BinOp) i); }
	public void visit(FDIV i)              { visit((BinOp) i); }
	public void visit(FREM i)              { visit((BinOp) i); }
	public void visit(LADD i)              { visit((BinOp) i); }
	public void visit(LSUB i)              { visit((BinOp) i); }
	public void visit(LMUL i)              { visit((BinOp) i); }
	public void visit(LDIV i)              { visit((IntegerDivision) i); }
	public void visit(LREM i)              { visit((IntegerDivision) i); }
	public void visit(LOR i)               { visit((BinOp) i); }
	public void visit(LAND i)              { visit((BinOp) i); }
	public void visit(LXOR i)              { visit((BinOp) i); }
	public void visit(IOR i)               { visit((BinOp) i); }
	public void visit(IAND i)              { visit((BinOp) i); }
	public void visit(IXOR i)              { visit((BinOp) i); }
	public void visit(FCMPG i)             { visit((Instruction) i); }
	public void visit(FCMPL i)             { visit((Instruction) i); }
	public void visit(DCMPG i)             { visit((Instruction) i); }
	public void visit(DCMPL i)             { visit((Instruction) i); }
	public void visit(LCMP i)              { visit((Instruction) i); }
	public void visit(INEG i)              { visit((Negation) i); }
	public void visit(FNEG i)              { visit((Negation) i); }
	public void visit(LNEG i)              { visit((Negation) i); }
	public void visit(DNEG i)              { visit((Negation) i); }
	public void visit(IINC i)              { visit((LocalAccess) i); }
	public void visit(ShiftOp i)           { visit((AsymetricBinOp) i); }
	public void visit(ISHL i)              { visit((ShiftOp) i); }
	public void visit(ISHR i)              { visit((ShiftOp) i); }
	public void visit(IUSHR i)             { visit((ShiftOp) i); }
	public void visit(LSHL i)              { visit((ShiftOp) i); }
	public void visit(LSHR i)              { visit((ShiftOp) i); }
	public void visit(LUSHR i)             { visit((ShiftOp) i); }
	public void visit(GotoInstruction i)   { visit((UnconditionalJump) i);}
	public void visit(GOTO i)              { visit((GotoInstruction) i); }
	public void visit(GOTO_W i)            { visit((GotoInstruction) i); }
	public void visit(IF_ACMPEQ i)         { visit((IfCmp) i); }
	public void visit(IF_ACMPNE i)         { visit((IfCmp) i); }
	public void visit(IF_ICMPEQ i)         { visit((IfCmp) i); }
	public void visit(IF_ICMPNE i)         { visit((IfCmp) i); }
	public void visit(IF_ICMPLE i)         { visit((IfCmp) i); }
	public void visit(IF_ICMPLT i)         { visit((IfCmp) i); }
	public void visit(IF_ICMPGE i)         { visit((IfCmp) i); }
	public void visit(IF_ICMPGT i)         { visit((IfCmp) i); }
	public void visit(IFEQ i)              { visit((IfZ) i); }
	public void visit(IFNE i)              { visit((IfZ) i); }
	public void visit(IFLE i)              { visit((IfZ) i); }
	public void visit(IFLT i)              { visit((IfZ) i); }
	public void visit(IFGE i)              { visit((IfZ) i); }
	public void visit(IFGT i)              { visit((IfZ) i); }
	public void visit(IFNONNULL i)         { visit((IfZ) i); }
	public void visit(IFNULL i)            { visit((IfZ) i); }
	public void visit(JsrInstruction i)    { visit((UnconditionalJump) i);}
	public void visit(JSR i)               { visit((JsrInstruction) i); }
	public void visit(JSR_W i)             { visit((JsrInstruction) i); }
	public void visit(RET i)               {
	    visit((LocalAccess) i);
	    visit((FlowChange) i);
	}
	public void visit(Switch i)            { visit((BranchInstruction) i); }
	public void visit(LOOKUPSWITCH i)      { visit((Switch) i); }
	public void visit(TABLESWITCH i)       { visit((Switch) i); }
	public void visit(INVOKEINTERFACE i)   { visit((Invocation) i); }
	public void visit(INVOKESPECIAL i)     { visit((Invocation) i); }
	public void visit(INVOKEVIRTUAL i)     { visit((Invocation) i); }
	public void visit(INVOKESTATIC i)      { visit((Invocation) i); }
	public void visit(MONITORENTER i)      { /*visit((Synchronization) i); */}
	public void visit(MONITOREXIT i)       { /*visit((Synchronization) i); */}
	public void visit(ATHROW i)            {
	    visit((FlowEnd) i);
	    visit((ExceptionThrower) i);
	}
	public void visit(NOP i)               { visit((Instruction) i); }
	public void visit(LABEL i)             { visit((NOP) i); }
	public void visit(INSTANCEOF i)        { visit((Resolution) i); }
	public void visit(CHECKCAST i)         { visit((Resolution) i); }
	public void visit(NEW i)               {
	    visit((Allocation) i);
	    visit((ConstantPoolPush) i);
	}
	/** Signal an error, since WIDE is not a complete instruction. **/
	public void visit(WIDE i) { throw new Error("wide not specialized"); }
	public void visit(WIDE_IINC i)         { visit((IINC) i); }
	public void visit(WIDE_ILOAD i)        { visit((LocalRead) i); }
	public void visit(WIDE_FLOAD i)        { visit((LocalRead) i); }
	public void visit(WIDE_ALOAD i)        { visit((LocalRead) i); }
	public void visit(WIDE_DLOAD i)        { visit((LocalRead) i); }
	public void visit(WIDE_LLOAD i)        { visit((LocalRead) i); }
	public void visit(WIDE_ISTORE i)       { visit((LocalWrite) i); }
	public void visit(WIDE_FSTORE i)       { visit((LocalWrite) i); }
	public void visit(WIDE_ASTORE i)       { visit((LocalWrite) i); }
	public void visit(WIDE_DSTORE i)       { visit((LocalWrite) i); }
	public void visit(WIDE_LSTORE i)       { visit((LocalWrite) i); }
	public void visit(WIDE_RET i)          {
	    visit((LocalAccess) i);
	    visit((FlowChange) i);
	}
	public void visit(ANEWARRAY_QUICK i)   { visit((ConstantPoolRead) i); }
	public void visit(NEW_QUICK i)         { visit((ConstantPoolRead) i); }
	public void visit(MULTIANEWARRAY_QUICK i){ visit((ConstantPoolRead) i);}
	public void visit(INSTANCEOF_QUICK i)  { visit((ConstantPoolRead) i); }
	public void visit(CHECKCAST_QUICK i)   {
	    visit((ConstantPoolRead) i);
	    visit((ExceptionThrower) i);
	}
	public void visit(GETFIELD_QUICK i)    { visit((FieldGet_Quick) i); }
	public void visit(GETFIELD2_QUICK i)   { visit((FieldGet_Quick) i); }
	public void visit(REF_GETFIELD_QUICK i){ visit((FieldGet_Quick) i); }
	public void visit(PUTFIELD_QUICK i)    { visit((FieldPut_Quick) i); }
	public void visit(PUTFIELD2_QUICK i)   { visit((FieldPut_Quick) i); }
	public void visit(PUTFIELD_QUICK_WITH_BARRIER_REF i) {
	    visit((FieldPut_Quick) i);
	}
	public void visit(INVOKEINTERFACE_QUICK i) {visit((Invocation_Quick)i);}
	public void visit(INVOKENONVIRTUAL_QUICK i){visit((Invocation_Quick)i);}
	public void visit(INVOKENONVIRTUAL2_QUICK i){visit((Invocation_Quick)i);}
	public void visit(INVOKESUPER_QUICK i)     {visit((Invocation_Quick)i);}
	public void visit(INVOKEVIRTUAL_QUICK i)   {visit((Invocation_Quick)i);}
	public void visit(INVOKE_SYSTEM i)  { visit((ExceptionThrowerImpl) i); }
	public void visit(INVOKE_NATIVE i)     { visit((Instruction) i); }
	public void visit(AFIAT i)             {
	    visit((ConstantPoolRead) i);
	    visit((Fiat) i);
	}
	public void visit(PrimFiat i)          {
	    visit((Instruction) i);
	    visit((Fiat) i);
	}
	public void visit(WidePrimFiat i)      {
	    visit((Instruction) i);
	    visit((Fiat) i);
	}
	public void visit(IFIAT i)             { visit((PrimFiat) i); }
	public void visit(FFIAT i)             { visit((PrimFiat) i); }
	public void visit(LFIAT i)             { visit((WidePrimFiat) i); }
	public void visit(DFIAT i)             { visit((WidePrimFiat) i); }

    }    

    /**
     * The instruction visitor ready for use with InstructionBuffer. This
     * corresponds to InstructionVisitor in the Runabout scenario.
     */
    public static class IVisitor extends Visitor {
	protected InstructionBuffer buf;
	protected Constants cp;
	public IVisitor(InstructionBuffer buf) {
            this.buf = buf;
	    if (buf != null)
		this.cp = buf.getConstantPool();
	}

	public MethodInformation getInstructionBuffer() {
	    return buf;
	}

	public ByteBuffer getCode() {
            return buf.getCode();
	}

	public Constants getConstantPool() {
            return buf.getConstantPool();
	}
	public Selector.Method getSelector() {
            return buf.getSelector();
	}
        public int getPC() {
    	    return buf.getPC();
	}
	public void visitAppropriate(Instruction i) {
	    i.accept(this);
	}
	public void visit(Instruction.WIDE i) {
	    i.specialize(buf).accept(this);
	}
    }

} // End of Instruction

