package s3.services.bytecode.verifier;

import ovm.core.repository.Descriptor;
import ovm.core.repository.JavaNames;
import ovm.core.repository.RepositoryMember;
import ovm.core.repository.TypeCodes;
import ovm.core.repository.TypeName;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.SpecificationIR.DoubleValue;
import ovm.services.bytecode.SpecificationIR.FloatValue;
import ovm.services.bytecode.SpecificationIR.IntValue;
import ovm.services.bytecode.SpecificationIR.LongValue;
import ovm.services.bytecode.SpecificationIR.NullRefValue;
import ovm.services.bytecode.SpecificationIR.Value;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.util.OVMError;
import org.ovmj.util.Runabout;
import s3.services.bytecode.analysis.S3AbstractInterpreter;

/**
 * AbstractInterpreter for bytecode verification.
 * @author Christian Grothoff
 **/
public class VerificationInterpreter
    extends S3AbstractInterpreter {

    /**
     * AbstractValueFactory.
     **/
    protected final AbstractValue.Factory avf;

    // cached repository primitives (for speed)
    protected final TypeName.Primitive Repository_INT;
    protected final TypeName.Primitive Repository_FLOAT;
    protected final TypeName.Primitive Repository_DOUBLE;
    protected final TypeName.Primitive Repository_LONG;

    // pre-generated abstract values - for speed
    protected AbstractValue NULL_POINTER;
    protected AbstractValue.Primitive PRIMITIVE_BOOL;
    protected AbstractValue.Primitive PRIMITIVE_CHAR;
    protected AbstractValue.Primitive PRIMITIVE_BYTE;
    protected AbstractValue.Primitive PRIMITIVE_SHORT;
    protected AbstractValue.Primitive PRIMITIVE_INT;
    protected AbstractValue.Primitive PRIMITIVE_FLOAT;
    protected AbstractValue.WidePrimitive PRIMITIVE_DOUBLE;
    protected AbstractValue.WidePrimitive PRIMITIVE_LONG;
    protected AbstractValue.Array PRIMITIVE_BOOL_ARRAY;
    protected AbstractValue.Array PRIMITIVE_SHORT_ARRAY;
    protected AbstractValue.Array PRIMITIVE_BYTE_ARRAY;
    protected AbstractValue.Array PRIMITIVE_CHAR_ARRAY;
    protected AbstractValue.Array PRIMITIVE_INT_ARRAY;
    protected AbstractValue.Array PRIMITIVE_FLOAT_ARRAY;
    protected AbstractValue.Array PRIMITIVE_DOUBLE_ARRAY;
    protected AbstractValue.Array PRIMITIVE_LONG_ARRAY;
    protected AbstractValue.Reference JAVA_LANG_STRING;
    protected AbstractValue.Reference JAVA_LANG_THROWABLE;

    public class Canonicalizer extends Runabout {

        protected AbstractValue result;
        public void visit(IntValue v) {
            switch (v.getType()) {
                case TypeCodes.BYTE :
                case TypeCodes.UBYTE :
                    result = PRIMITIVE_BYTE;
                    break;
                case TypeCodes.CHAR :
                case TypeCodes.USHORT :
                    result = PRIMITIVE_CHAR;
                    break;
                case TypeCodes.SHORT :
                    result = PRIMITIVE_SHORT;
                    break;
                case TypeCodes.INT :
                case TypeCodes.UINT :
                    result = PRIMITIVE_INT;
                    break;
                default :
                    throw new OVMError("Unknown type: " + v.getType());
            }
        }
        public void visit(LongValue v) {
            result = PRIMITIVE_LONG;
        }
        public void visit(FloatValue v) {
            result = PRIMITIVE_FLOAT;
        }
        public void visit(DoubleValue v) {
            result = PRIMITIVE_DOUBLE;
        }
        public void visit(NullRefValue v) {
            result = NULL_POINTER;
        }
        public void visit(Value v) {
            result = null;
        }
        public AbstractValue convert(Value v) {
            visitAppropriate(v);
            return result;
        }
        public void visitDefault(Object o) { 
 	    throw new OVMError("Unknown case: " + o.getClass());
        }
    }

    private final Canonicalizer specMapper	= makeCanonicalizer();
    
    protected Canonicalizer makeCanonicalizer() {
	return new Canonicalizer();
    }

    protected AbstractValue convertSpecification(Value v) {
	return specMapper.convert(v);
    }

    /**
     * Create a new AbstractInterpreter
     **/
    public VerificationInterpreter(RepositoryMember.Method me,
				   AbstractValue.Factory avf) {
	this(InstructionBuffer.wrap(me), avf);
    }

    /**
     * Create a new AbstractInterpreter
     **/
    public VerificationInterpreter(InstructionBuffer ib,
				   AbstractValue.Factory avf) {
	super(ib);
        this.avf = avf;

        Repository_INT = TypeName.INT;
        Repository_FLOAT = TypeName.FLOAT;
        Repository_DOUBLE = TypeName.DOUBLE;
        Repository_LONG = TypeName.LONG;

        NULL_POINTER = avf.makeNull();
        PRIMITIVE_BYTE = avf.makePrimitiveByte();
        PRIMITIVE_CHAR = avf.makePrimitiveChar();
        PRIMITIVE_SHORT = avf.makePrimitiveShort();
        PRIMITIVE_BOOL = avf.makePrimitiveBool();
        PRIMITIVE_INT = avf.makePrimitiveInt();
        PRIMITIVE_FLOAT = avf.makePrimitiveFloat();
        PRIMITIVE_DOUBLE = avf.makePrimitiveDouble();
        PRIMITIVE_LONG = avf.makePrimitiveLong();

        PRIMITIVE_BYTE_ARRAY = avf.makeArray(PRIMITIVE_BYTE);
        PRIMITIVE_CHAR_ARRAY = avf.makeArray(PRIMITIVE_CHAR);
        PRIMITIVE_SHORT_ARRAY = avf.makeArray(PRIMITIVE_SHORT);
        PRIMITIVE_BOOL_ARRAY = avf.makeArray(PRIMITIVE_BOOL);
        PRIMITIVE_INT_ARRAY = avf.makeArray(PRIMITIVE_INT);
        PRIMITIVE_FLOAT_ARRAY = avf.makeArray(PRIMITIVE_FLOAT);
        PRIMITIVE_DOUBLE_ARRAY = avf.makeArray(PRIMITIVE_DOUBLE);
        PRIMITIVE_LONG_ARRAY = avf.makeArray(PRIMITIVE_LONG);
        JAVA_LANG_STRING = avf.makeReference(JavaNames.java_lang_String);
        JAVA_LANG_THROWABLE = avf.makeReference(JavaNames.java_lang_Throwable);
    }


    /* **************** and now: visit methods! ************** */

    public void visit(Instruction.GETFIELD i) {
        Descriptor.Field desc = i.getSelector(buf, cp).getDescriptor();
        TypeName type = desc.getType();
        inputRegister[0] = frame_.pop();
        outputRegister[0] = avf.typeName2AbstractValue(type);
        if (desc.isWidePrimitive())
            frame_.pushWide(outputRegister[0]);
        else
            frame_.push(outputRegister[0]);
    }

    public void visit(Instruction.GETSTATIC i) {
        Descriptor.Field desc = i.getSelector(buf, cp).getDescriptor();
        TypeName type = desc.getType();
        outputRegister[0] = avf.typeName2AbstractValue(type);
        if (desc.isWidePrimitive())
            frame_.pushWide(outputRegister[0]);
        else
            frame_.push(outputRegister[0]);
    }

    public void visit(Instruction.INVOKESTATIC i) {
        Descriptor.Method des = i.getSelector(buf, cp).getDescriptor();
        //pop args
        int argCount = des.getArgumentCount();
        setArgumentRegisterCount(argCount);
        AbstractValue[] arg = argumentRegisters;
        for (int j = argCount - 1; j >= 0; j--) {
            if (des.isArgumentWidePrimitive(j))
                arg[j] = frame_.popWide();
            else
                arg[j] = frame_.pop();
        }
        //push return value
        TypeName type = des.getType();
	if (type.getTypeTag() == TypeCodes.VOID)
	    return; // nothing to push!
         outputRegister[0] = avf.typeName2AbstractValue(type);
        if (des.isReturnValueWidePrimitive())
            frame_.pushWide(outputRegister[0]);
        else
            frame_.push(outputRegister[0]);
    }
    protected void visitInvocation(Instruction.Invocation i) {
        Descriptor.Method des = i.getSelector(buf, cp).getDescriptor();

        //pop args
        int argCount = des.getArgumentCount();
        setArgumentRegisterCount(argCount);
        AbstractValue[] arg = argumentRegisters;
        for (int j = argCount - 1; j >= 0; j--)
            if (des.isArgumentWidePrimitive(j))
                arg[j] = frame_.popWide();
            else
                arg[j] = frame_.pop();
        //pop obj ref (this method is NOT for invokestatic!)
        inputRegister[0] = frame_.pop();

        //push return value
        TypeName type = des.getType();
 	if (type.getTypeTag() == TypeCodes.VOID)
	    return; // nothing to push!
        outputRegister[0] = avf.typeName2AbstractValue(type);
        if (des.isReturnValueWidePrimitive())
            frame_.pushWide(outputRegister[0]);
        else
            frame_.push(outputRegister[0]);
    }
    public void visit(Instruction.INVOKEVIRTUAL i) {
        visitInvocation(i);
    }
    public void visit(Instruction.INVOKEINTERFACE i) {
        visitInvocation(i);
    }
    public void visit(Instruction.INVOKESPECIAL i) {
        visitInvocation(i);
    }
    public void visit(Instruction.NEW i) {
        frame_.push(avf.makeReference(i.getClassName(buf, cp)));
    }
    public void visit(Instruction.NEWARRAY i) {
        inputRegister[0] = frame_.pop();
        switch (i.getPrimitiveType(buf)) {
            case 4 :
                //typeName = Repository._INT;
                outputRegister[0] = PRIMITIVE_BOOL_ARRAY;
                break;
            case 5 :
                //typeName = Repository._CHAR;
                outputRegister[0] = PRIMITIVE_CHAR_ARRAY;
                break;
            case 6 :
                //typeName = Repository._FLOAT;
                outputRegister[0] = PRIMITIVE_FLOAT_ARRAY;
                break;
            case 7 :
                //typeName = Repository._DOUBLE;
                outputRegister[0] = PRIMITIVE_DOUBLE_ARRAY;
                break;
            case 8 :
                //typeName = Repository._BYTE;
                outputRegister[0] = PRIMITIVE_BYTE_ARRAY;
                break;
            case 9 :
                //typeName = Repository._SHORT;
                outputRegister[0] = PRIMITIVE_SHORT_ARRAY;
                break;
            case 10 :
                //typeName = Repository._INT;
                outputRegister[0] = PRIMITIVE_INT_ARRAY;
                break;
            case 11 :
                //typeName = Repository._LONG;
                outputRegister[0] = PRIMITIVE_LONG_ARRAY;
                break;
            default :
                throw new Error(
                    "NEWARRAY: Tag "
                        + i.getPrimitiveType(buf)
                        + " not known!");
        }
        frame_.push(outputRegister[0]);
    }
    public void visit(Instruction.ANEWARRAY i) {
        inputRegister[0] = frame_.pop();
        frame_.push(
            avf.makeArray(avf.typeName2AbstractValue(i.getClassName(buf, cp))));
    }
    public void visit(Instruction.MULTIANEWARRAY i) {
        outputRegister[0] =
            inputRegister[0] = avf.typeName2AbstractValue(i.getClassName(buf, cp));
        int count = i.getDimensions(buf);
        setArgumentRegisterCount(count);
        for (int j = 0; j < count; j++) {
            argumentRegisters[j] = frame_.pop();
        }
        frame_.push(outputRegister[0]);
    }
    public void visit(Instruction.CHECKCAST i) {
        inputRegister[0] = frame_.pop();
        frame_.push(avf.typeName2AbstractValue(i.getResultTypeName(buf, cp)));
    }
/* unused ? 
    private void visitCL(Instruction.ConstantLoad i, AbstractValue type) {
        outputRegister[0] = type;
        frame.push(outputRegister[0]);
    }
    private void visitCLW(Instruction.ConstantLoad i, AbstractValue type) {
        outputRegister[0] = type;
        frame.pushWide(outputRegister[0]);
    }
    */
    public void visit(Instruction.ConstantPoolLoad i) {
        byte cType = i.getCType(buf, cp);
        switch (cType) {
            case JVMConstants.CONSTANT_String :
                outputRegister[0] = JAVA_LANG_STRING;
                break;
            case JVMConstants.CONSTANT_Float :
                outputRegister[0] = PRIMITIVE_FLOAT;
                break;
            case JVMConstants.CONSTANT_Integer :
                outputRegister[0] = PRIMITIVE_INT;
                break;
            case JVMConstants.CONSTANT_Double :
                outputRegister[0] = PRIMITIVE_DOUBLE;
                break;
            case JVMConstants.CONSTANT_Long :
                outputRegister[0] = PRIMITIVE_LONG;
                break;
        }
        if (outputRegister[0].isWidePrimitive())
            frame_.pushWide(outputRegister[0]);
        else
            frame_.push(outputRegister[0]);
    }
    public void visit(Instruction.JSR i) {
        outputRegister[0] = avf.makeJumpTarget(pc_ + i.size(buf));
        frame_.push(outputRegister[0]);
    }
    public void visit(Instruction.JSR_W i) {
        outputRegister[0] = avf.makeJumpTarget(pc_ + i.size(buf));
        frame_.push(outputRegister[0]);
    }

} // end of VerificationInterpreter
