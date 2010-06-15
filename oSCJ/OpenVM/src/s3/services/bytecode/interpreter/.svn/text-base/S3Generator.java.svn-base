package s3.services.bytecode.interpreter;

import java.io.IOException;
import java.io.Writer;

import ovm.core.repository.TypeCodes;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionSet;
import ovm.services.bytecode.JVMConstants;
import ovm.services.bytecode.SpecificationIR.ArrayAccessExp;
import ovm.services.bytecode.SpecificationIR.ArrayLengthExp;
import ovm.services.bytecode.SpecificationIR.AssignmentExp;
import ovm.services.bytecode.SpecificationIR.BinExp;
import ovm.services.bytecode.SpecificationIR.BitFieldExp;
import ovm.services.bytecode.SpecificationIR.BlueprintAccessExp;
import ovm.services.bytecode.SpecificationIR.CPAccessExp;
import ovm.services.bytecode.SpecificationIR.CSACallExp;
import ovm.services.bytecode.SpecificationIR.CallExp;
import ovm.services.bytecode.SpecificationIR.ConcreteIntValue;
import ovm.services.bytecode.SpecificationIR.CondExp;
import ovm.services.bytecode.SpecificationIR.ConversionExp;
import ovm.services.bytecode.SpecificationIR.CurrentConstantPool;
import ovm.services.bytecode.SpecificationIR.CurrentPC;
import ovm.services.bytecode.SpecificationIR.DoubleValue;
import ovm.services.bytecode.SpecificationIR.EmptyValueSourceVisitor;
import ovm.services.bytecode.SpecificationIR.FloatValue;
import ovm.services.bytecode.SpecificationIR.IfCmd;
import ovm.services.bytecode.SpecificationIR.IfExp;
import ovm.services.bytecode.SpecificationIR.IntValue;
import ovm.services.bytecode.SpecificationIR.LinkSetAccessExp;
import ovm.services.bytecode.SpecificationIR.ListElementExp;
import ovm.services.bytecode.SpecificationIR.LocalExp;
import ovm.services.bytecode.SpecificationIR.LocalStore;
import ovm.services.bytecode.SpecificationIR.LookupExp;
import ovm.services.bytecode.SpecificationIR.MemExp;
import ovm.services.bytecode.SpecificationIR.NonnulRefValue;
import ovm.services.bytecode.SpecificationIR.PCValue;
import ovm.services.bytecode.SpecificationIR.Padding;
import ovm.services.bytecode.SpecificationIR.RefValue;
import ovm.services.bytecode.SpecificationIR.ReinterpretExp;
import ovm.services.bytecode.SpecificationIR.SecondHalf;
import ovm.services.bytecode.SpecificationIR.ShiftMaskExp;
import ovm.services.bytecode.SpecificationIR.StreamableValue;
import ovm.services.bytecode.SpecificationIR.SymbolicConstant;
import ovm.services.bytecode.SpecificationIR.Temp;
import ovm.services.bytecode.SpecificationIR.UnaryExp;
import ovm.services.bytecode.SpecificationIR.Value;
import ovm.services.bytecode.SpecificationIR.ValueList;
import ovm.services.bytecode.SpecificationIR.ValueSource;
import ovm.services.bytecode.SpecificationIR.WideValue;
import ovm.util.CommandLine;
import ovm.util.OVMError;
import s3.services.bytecode.SpecificationProcessor;
import java.io.PrintWriter;
import java.io.FileOutputStream;

public class S3Generator 
    extends SpecificationProcessor.CSourceGenerator {
    // Why call this S3 Generator if it's under the S3 package?
    // seems like it would be similar to have java.lang.JavaString instead
    // of java.lang.String or javax.swing.JButton instead of, hm,...
    // never mind.

    protected final static String STACK_OUT_PREFIX = "stack_out_";
    protected final static String STACK_IN_PREFIX = "stack_in_";
    protected final static String LOCAL_PREFIX = "local_";
    protected final static String STREAM_IN_PREFIX = "stream_in_";
    protected final static String GET_LOCAL = "GETLOCAL";
    protected final static String SET_LOCAL_P = "SETLOCAL_P";
    protected final static String SET_LOCAL_R = "SETLOCAL_R";
    protected final static String GET_LOCAL_W = "GETLOCAL_W";
    protected final static String SET_LOCAL_W = "SETLOCAL_W";
    protected final static String CURRENT_PC = "GETPC()";

    public S3Generator(Writer out, InstructionSet is) {
        super(out, is);
    }

    protected boolean shouldProcess(Instruction spec) {
        // unimplemented opcodes and skip unquickened instructions 
        return !(
            spec instanceof Instruction.UNIMPLEMENTED
            || (spec instanceof Instruction.INVOKE_NATIVE)
            || (spec instanceof Instruction.INVOKE_SYSTEM)
            || (spec instanceof Instruction.NEWARRAY) // anewarray_quick
	    || ((spec instanceof Instruction.ConstantPoolRead)
		&& !(spec instanceof Instruction.ConstantPoolLoad
		     || spec instanceof Instruction.AFIAT)	        
		&& !(spec instanceof Instruction.NEW
		     || spec instanceof Instruction.NEW_QUICK
		     || spec instanceof Instruction.ANEWARRAY_QUICK
		     || spec instanceof Instruction.CHECKCAST_QUICK
		     || spec instanceof Instruction.INSTANCEOF_QUICK
		     || spec instanceof Instruction.MULTIANEWARRAY_QUICK))
	    );
    }

    private IntValue sizeValue(Instruction i) {
        int count = 1;
        IntValue retval = null;
        for (int j = 0; j < i.istreamIns.length; j++) {
            if (!(i.istreamIns[j] instanceof ValueList)) {
                count += i.istreamIns[j].bytestreamSize();
            } else {
                ValueList list = (ValueList) i.istreamIns[j];
                retval =
                    new IntValue(
                        new BinExp(
                            new ConcreteIntValue(count),
                            "+",
                            list.sizeValue()));
                count = 0;
            }
        }
        if (retval == null) {
            return new ConcreteIntValue(count);
        } else {
            return new IntValue(
                new BinExp(retval, "+", new ConcreteIntValue(count)));
        }
    }

    private void declareIstreamIns(Instruction spec) throws IOException {

        int stream_offset = OPCODE_SIZE;
        boolean hasPadding = false;
        for (int i = 0; i < spec.istreamIns.length; i++) {
            StreamableValue v = spec.istreamIns[i];
            if (v instanceof Padding) {
                if (!hasPadding) {
                    writeln(
                        "int padding = PAD4(GETPC() + " + stream_offset + ");");
                    hasPadding = true;
                }
                continue;
            }
            String macroName;
	    
            switch (v.getType()) {
	    case TypeCodes.UBYTE :
		macroName = "ISTREAM_GET_UBYTE_AT";
		break;
	    case TypeCodes.BYTE :
		macroName = "ISTREAM_GET_SBYTE_AT";
		break;
	    case TypeCodes.CHAR :
	    case TypeCodes.USHORT :
		macroName = "ISTREAM_GET_USHORT_AT";
		break;
	    case TypeCodes.SHORT :
		macroName = "ISTREAM_GET_SSHORT_AT";
		break;
	    case TypeCodes.UINT:
		macroName = "ISTREAM_GET_UUINT_AT";
		    break;
	    case TypeCodes.INT :
		if (v instanceof ValueList) {
		    macroName = "ISTREAM_GET_SINT_ADDRESS_AT";
		} else {
		    macroName = "ISTREAM_GET_SINT_AT";
		}
		break;
   	    case TypeCodes.FLOAT:
		macroName = "ISTREAM_GET_FLOAT_AT";
		break;
	    case TypeCodes.DOUBLE:
		macroName = "ISTREAM_GET_DOUBLE_AT";
		break;
	    case TypeCodes.LONG:
		macroName = "ISTREAM_GET_LONG_AT";
		break;
             default :
		 throw new OVMError("cannot deal with type " + v.getType());
            }
            writeln(
                getCType((Value)v)
                    + ' '
                    + istreamInName(i)
                    + " = "
                    + macroName
                    + '('
                    + stream_offset
                    + (hasPadding ? " + padding" : "")
                    + ");");
            stream_offset += v.bytestreamSize();
        }
    }

    private boolean hasVariableStackOuts(Instruction i) {
        for (int j = 0; j < i.stackOuts.length; j++) {
            if (i.stackOuts[j] instanceof ValueList) {
                return true;
            }
        }
        return false;
    }

    private boolean isStackManipulation(Instruction i) {
        return i instanceof Instruction.StackManipulation;
    }

    Stringifier str = new Stringifier();
    private boolean optimizeStackManipulation(Instruction spec)
        throws IOException {
        if (!isStackManipulation(spec)) {
            return false;
        }
        if (spec.stackOuts.length == 0) {
            for (int i = 0; i < spec.stackIns.length; i++) {
                writeln("(void) POP().jint;");
            }
        } else if (spec.stackOuts.length > spec.stackIns.length) {
            int commonPrefixLen = 0;
            for (int i = 0; i < spec.stackIns.length; i++) {
                if (spec.stackIns[i] == spec.stackOuts[i]) {
                    commonPrefixLen++;
                } else {
                    break;
                }
            }
            int count = 0;
            for (int i = commonPrefixLen; i < spec.stackOuts.length; i++) {
                if (spec.stackOuts[i] == spec.stackIns[i - commonPrefixLen]) {
                    count++;
                } else {
                    return false; // couldn't recognize
                }
            }
            for (int i = 0; i < count; i++) {
                writeln("PICK(" + (count - 1) + ");");
            }
            if (!isRollIdempotent(spec.stackOuts,
                spec.stackOuts.length - commonPrefixLen)) {

                writeln(
                    "ROLL("
                        + spec.stackOuts.length
                        + ","
                        + (spec.stackOuts.length - commonPrefixLen)
                        + ");");
            }
        } else if (
            spec.stackOuts.length == spec.stackIns.length
                && (spec.stackIns.length == 2)) {
            writeln("SWAP();");
        } else {
            return false; // couldn't recognize the pattern
        }
        return true;

    }
    private static boolean isRollIdempotent(Object[] arr, int count) {
        Object[] other = new Object[arr.length];
        if (count > 0) {
            System.arraycopy(arr, count, other, 0, arr.length - count);
            System.arraycopy(arr, 0, other, arr.length - count, count);
        } else {
            throw new UnsupportedOperationException("FIXME");
        }
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != other[i]) {
                return false;
            }
        }
        return true;
    }
    private boolean isReturn(Instruction spec) {
        return spec instanceof Instruction.FlowEnd
            && !(spec instanceof Instruction.ATHROW);
    }

    protected void emitBody(Instruction spec) throws IOException {

        str.initWithSpec(spec);
        if (isReturn(spec)) {
            writeln("jref obj = GETLOCAL(0).jref;");
            writeln("zint synch = current_context->current_frame->current_method->isSynchronized_;");
            declareStackOuts(spec);
            emitPops(spec);
            processOuts(spec);
            writeln("POPFRAME();");
            emitPushes(spec);
            writeln("if (synch) {");
            writeln("CALL_MONITOREXIT(obj);");
            writeln("}");
            writeln("NEXT_INSTRUCTION;");
        } else if (spec instanceof Instruction.MULTIANEWARRAY_QUICK) {
            writeln("struct arr_jint_255 dim;");
            writeln("unsigned short stream_in_0 = ISTREAM_GET_USHORT_AT(1);");
            writeln("unsigned char dimensions = ISTREAM_GET_UBYTE_AT(3);");
            writeln("dim.header.length = dimensions;");
            writeln("while (dimensions > 0)  {");
            writeln("    dim.header.values[--dimensions] = POP().jint; ");
            writeln("}");
            writeln("PUSH_R((jref)csaTHIS);");
            writeln("PUSH_R((jref)get_constant(current_context, stream_in_0));");
            writeln("PUSH_P(0);");
            writeln("/* not heap alloc'd & can contain no reference */");
            writeln("PUSH_P((jref)&dim.header); ");
            writeln("/* this pointer and 3 arguments: bp, 0, dim */");
            writeln("/* NOTE: INVOKE_CSA never returns */");
            writeln("INVOKE_CSA(allocateMultiArray, 4, 4);");
        } else {

            if (!optimizeStackManipulation(spec)) {
                declareStackOuts(spec);
                declareTemps(spec);
                declareIstreamIns(spec);
                emitPops(spec);
                initTemps(spec);

		if (spec instanceof Instruction.LDC) {
		    // quicken!
		    writeln("ISTREAM_SET_UBYTE_AT("+0+", " + JVMConstants.Opcodes.LDC_REF_QUICK +");");
		}
		if (spec instanceof Instruction.LDC_W) {
		    writeln("ISTREAM_SET_UBYTE_AT("+0+", " + JVMConstants.Opcodes.LDC_W_REF_QUICK +");");
		}

		if (spec instanceof Instruction.LOAD_SHST_FIELD) {
		    writeln("ISTREAM_SET_UBYTE_AT("+0+", " + JVMConstants.Opcodes.LOAD_SHST_FIELD_QUICK +");");
		}

		if (spec instanceof Instruction.LOAD_SHST_METHOD) {
		    writeln("ISTREAM_SET_UBYTE_AT("+0+", " + JVMConstants.Opcodes.LOAD_SHST_METHOD_QUICK +");");
		}

		emitEvals(spec);
                processOuts(spec);
                emitPushes(spec);
            }
            // insert the right control transfer instructions, unless this
            // instruction ends with a CSA call
            if (!hasCSATailCall(spec)) {
                doControlTransfer(spec);
            }
        }
    }
    
    private void emitPops(Instruction spec) throws IOException {
        ValueList valueList = null;
        for (int i = 0; i < spec.stackIns.length; i++) {
            Value v = spec.stackIns[i];
            if (v instanceof ValueList) {
                if (valueList == null) {
                    valueList = (ValueList) v;
                } else {
                    throw new OVMError("can't deal with multiple value lists");
                }
                continue;
            }
            if (valueList == null) {
                emitPop(v, i);
            } else {
                emitPeek(spec, valueList.valueCount, i);
            }

            if (v.isWide()) {
                i++;
            }
        }
        // emit nonnull checks
        for (int i = 0; i < spec.stackIns.length; i++) {
            // No need to perform explicit check for ArrayRefValues
            // XXX: split out CHECK_ARRAY_BOUNDS/CHECK_NONNULL after
            // the spec starts getting generated properly
            if (spec.stackIns[i] instanceof NonnulRefValue) {
                emitNonnullCheck(i);
            }
        }

    }

    void declareTemps(Instruction spec) throws IOException {
        for (int i = 0; i < spec.temps.length; i++)
            write(spec.temps[i].type + " " + spec.temps[i].name + ";");
    }

    void initTemps(Instruction spec) throws IOException {
        for (int i = 0; i < spec.temps.length; i++)
            writeln(
                spec.temps[i].name
                    + " = "
                    + boundName(spec.temps[i].init)
                    + ";");
    }

    String getAssignment(Value lhs) {
        if (lhs instanceof ValueList) {
            return " = (int*)";
        } else {
            return " = ";
        }
    }

    void emitPeek(Instruction spec, IntValue valueCount, int i)
        throws IOException {
        Value v = spec.stackIns[i];
        write(getCType(v) + ' ' + stackInName(i) + getAssignment(v));
        Stringifier s = new Stringifier();
        s.initWithSpec(spec);
        s.visitValue(valueCount);
        // count - 1 + i 
        int delta = i - 1;
        String deltaStr = "";
        if (delta > 0) {
            deltaStr += " + " + delta;
        }
        writeln("PEEK(" + s.buf + deltaStr + ")" + getUnionField(v) + ";");

    }
    void emitNonnullCheck(int i) throws IOException {
        writeln("CHECK_NONNULL(" + stackInName(i) + ");");
    }

    void emitPop(Value v, int i) throws IOException {
        write(getCType(v) + ' ' + stackInName(i) + getAssignment(v));
        // write(" + getCType(v) + ")");
        if (v.isWide()) {
            writeln("POP_W()" + getUnionField(v) + ";");
        } else {
            /* if (v instanceof NonnulRefValue) {
               writeln("CHECK_NONNULL(POP()" + getUnionField(v) + ");");
               } else */
            writeln("POP()" + getUnionField(v) + ";");
        }
    }

    private void emitPush(Value v, int i) throws IOException {
        if (v instanceof SecondHalf) {
            assert(false);
        } else if (v instanceof WideValue) {
            emitPushWide(i);
        } else if ((v instanceof PCValue) && !((PCValue) v).isRelative()) {
            // FIXME For moving collectors, shouldn't the PC value be
            // relative?  Displaced pointers are bad and PUSH_P screws
            // up the tag stack.  Or, as the handwritten interpreter
            // said, "not an Oop!"
            writeln("PUSH_P((jref)" + stackOutName(i) + ");");
        } else if (v instanceof RefValue) {
            emitPushRef(i);
        } else if (v instanceof IntValue) {
            emitPushIntRef(i, ((IntValue) v).getType());
        } else {
            emitPushNonref(i);
        }
    }

    private void emitPushes(Instruction spec) throws IOException {
        if (hasVariableStackOuts(spec)) {
            return;

        }
        for (int i = spec.stackOuts.length - 1; i >= 0; i--) {
            Value v = spec.stackOuts[i];
            if (v instanceof SecondHalf) {
                continue;
            }
            if (v.source instanceof CSACallExp) {
                continue;
            }
            emitPush(v, i);
        }
    }

    protected void emitPushRef(int i) throws IOException {
        writeln("PUSH_R(" + stackOutName(i) + ");");
    }

    protected void emitPushNonref(int i) throws IOException {
        writeln("PUSH_P(" + stackOutName(i) + ");");
    }

    protected void emitPushIntRef(int i, char type) throws IOException {
        if (type == TypeCodes.BYTE
            || type == TypeCodes.SHORT
            || type == TypeCodes.CHAR) {
            writeln("PUSH_P((jint)" + stackOutName(i) + ");");
        } else {
            emitPushNonref(i);
        }
    }

    protected void emitPushWide(int i) throws IOException {
        writeln("PUSH_W(" + stackOutName(i) + ");");
    }
    protected void emitPCUpdate(String expr) throws IOException {
        writeln("INCPC(" + expr + ");");
    }

    protected void emitPCUpdate(Instruction spec) throws IOException {
        writeln("INCPC(" + boundName(sizeValue(spec)) + ");");
    }

    protected void doControlTransfer(Instruction spec) throws IOException {
        if (spec instanceof Instruction.FlowEnd)
            return;
        else if (spec instanceof Instruction.POLLCHECK) {
            emitPCUpdate(spec);
            writeln("NEXT_INSTRUCTION_WITH_POLL_CHECK;");
        } else if (spec instanceof Instruction.FlowChange) {
            Instruction.FlowChange i = (Instruction.FlowChange) spec;
            if (!(i instanceof Instruction.ConditionalJump)
                && (i.getJumpTarget() != null)) {
                PCValue v = i.getJumpTarget();
                if (v.isRelative()) {
                    emitPCUpdate(boundName(i.getJumpTarget()));
                } else {
                    writeln(
                        "SETPC((byte*)"
                            + boundName(i.getJumpTarget())
                            + ");");
                }
            } else if (i instanceof Instruction.ConditionalJump) {
                Instruction.ConditionalJump cspec =
                    (Instruction.ConditionalJump) i;
                writeln("if (" + boundName(cspec.getControlValue()) + ')');

                emitPCUpdate(boundName(cspec.getJumpTarget()));
                writeln("else");
                emitPCUpdate(spec);
            }
            writeln("NEXT_INSTRUCTION;");
        } else if (spec instanceof Instruction.Invocation_Quick) {
            // no emitPCUpdate(spec)
        } else if (spec instanceof Instruction.Synchronization) {
            // currently no synchronization instructions will reach this
            writeln("NEXT_INSTRUCTION;");
        } else if (
            spec instanceof Instruction.CHECKCAST_QUICK
                || (spec instanceof Instruction.INSTANCEOF_QUICK)) {
            emitPCUpdate(spec);
            writeln("NEXT_INSTRUCTION;");
        } else {
            emitPCUpdate(spec);
            writeln("NEXT_INSTRUCTION;");
        }
    }

    private void emitEvals(Instruction spec) throws IOException {
        for (int i = 0; i < spec.evals.length; i++) {
            write(boundName(spec.evals[i]));
            writeln(";");
        }
    }

    /** Look for a CSA call expression. As a call to the CSA never returns
        we don't need any additional control flow logic after it - nor
        should anything appear after it as it can't get executed.
    */
    private boolean hasCSATailCall(Instruction spec) {
        for (int i = 0; i < spec.stackOuts.length; i++) {
            if (spec.stackOuts[i].source instanceof CSACallExp) {
                return true;
            }
        }
        for (int i = 0; i < spec.evals.length; i++) {
            if (spec.evals[i].source instanceof CSACallExp) {
                return true;
            }
        }
        return false;
    }

    private void processOuts(Instruction spec) throws IOException {
        for (int i = 0; i < spec.stackOuts.length; i++) {
            if (spec.stackOuts[i].source instanceof CSACallExp) {
                writeln(boundName(spec.stackOuts[i]) + ';');
            } else {
                // S3Base.d("processing OUT" + i);
                String rep = boundName(spec.stackOuts[i]);
                if (rep != null) {
                    writeln(
                        stackOutName(i)
                            + getAssignment(spec.stackOuts[i])
                            + rep
                            + ';');
                } else {
                    // S3Base.d("no binding for OUT" + i);
                }
            }
            if (spec.stackOuts[i].isWide()) {
                i++;
            }
        }
    }

    private void declareStackOuts(Instruction spec) throws IOException {
        for (int i = 0; i < spec.stackOuts.length; i++) {
            if (!(spec.stackOuts[i].source instanceof CSACallExp)) {
                writeln(
                    getCType(spec.stackOuts[i]) + ' ' + stackOutName(i) + ';');
            }
            if (spec.stackOuts[i].isWide()) {
                i++;
            }
        }
    }

    protected String stackInName(int inslot) {
        return STACK_IN_PREFIX + inslot;
    }

    protected String istreamInName(int inslot) {
        return STREAM_IN_PREFIX + inslot;
    }

    protected String localsInName(int inslot) {
        return LOCAL_PREFIX + inslot;
    }

    protected String stackOutName(int inslot) {
        return STACK_OUT_PREFIX + inslot;
    }

    protected String localsAccess(Value outval, IntValue index) {
        String rep = "";
        if (outval.isWide()) {
            rep += GET_LOCAL_W;
        } else {
            rep += GET_LOCAL;
        }
        rep += '(' + boundName(index) + ')' + getUnionField(outval);
        return rep;
    }

    protected boolean isValueFromSlot(Value value, IntValue slotNumber) {
        LocalExp slot = value.localsSlot();
        if (slot == null) {
            return false;
        }
        return slotNumber == slot.number;
    }

    public class Stringifier extends EmptyValueSourceVisitor {
        StringBuffer buf;
        Value currentValue;
        Instruction spec;

        public void initWithSpec(Instruction s) {
            this.spec = s;
            reset();
        }

        public void reset() {
            buf = new StringBuffer();
        }

        private Value save(Value newValue) {
            Value v = currentValue;
            assert(newValue != null);
            currentValue = newValue;
            return v;
        }
        private void restore(Value v) {
            currentValue = v;
        }

        public void visit(ValueSource vs) {
            throw new Error("match not exhaustive: " + vs);
        }

        public void visit(Temp t) {
            buf.append(t.name);
        }

	public void visit(CurrentConstantPool ccp) {
	    buf.append("(jref)current_context->current_frame->current_cp");
	}

        public void visit(LocalExp localsSlot) {

            if (currentValue.isWide()) {
                buf.append(GET_LOCAL_W);
            } else {
                buf.append(GET_LOCAL);
            }
            buf.append('(');
            visitValue(localsSlot.number);
            buf.append(')');
            buf.append(getUnionField(currentValue));

        }
        public void visit(LocalStore store) {
            if (store.value.isWide()) {
                buf.append(SET_LOCAL_W);
            } else if (store.value.isReference()) {
                buf.append(SET_LOCAL_R);
            } else {
                buf.append(SET_LOCAL_P);
            }
            buf.append('(');
            visitValue(store.index);
            buf.append(", ");
            visitValue(store.value);
            buf.append(')');
        }

        public void visit(ArrayAccessExp exp) {
            boolean extraCast = currentValue.isReference() && !exp.isStore;
            if (extraCast) {
                buf.append('(');
                buf.append(getCType(currentValue));
                buf.append(')');
                buf.append('(');
            }

            buf.append("(");
            if (currentValue instanceof RefValue) {
                buf.append("(struct arr_java_lang_Object*)");
            } else {
                buf.append("(struct arr_");
                buf.append(getCType(currentValue));
                buf.append("*)");
            }
            visitValue(exp.arr);
            buf.append(')');
            buf.append("->values[");
            visitValue(exp.index);
            buf.append(']');
            if (extraCast) {
                buf.append(')');
            }
        }

        public void visit(BlueprintAccessExp exp) {
            buf.append("HEADER_BLUEPRINT(");
            visitValue(exp.ref);
            buf.append(")");
        }

        public void visit(ArrayLengthExp exp) {
            buf.append("((struct Array*)");
            visitValue(exp.arr);
            buf.append(")->length");
        }

        public void visit(LookupExp exp) {
            buf.append("((ByteCode*)(");
            visitValue(exp.bp);
            buf.append("->");
            buf.append(exp.tableName);
            buf.append("->values[");
            visitValue(exp.index);
            buf.append("]))");
        }

        public void visit(ConversionExp conversion) {
            buf.append('(');
            buf.append(getCType(currentValue));
            buf.append(')');
            visitValue(conversion.before);
        }

	public void visit(ReinterpretExp conversion) {
	    // Don't generate a cast.
	    visitValue(conversion.before);
	}

        public void visit(CallExp exp) {
            buf.append(exp.fname);
            buf.append('(');
            for (int i = 0; i < exp.args.length; i++) {
                visitValue(exp.args[i]);
                if (i < exp.args.length - 1) {
                    buf.append(", ");
                }
            }
            buf.append(')');
        }

        public void visit(CSACallExp exp) {
            buf.append("PUSH_R((jref)csaTHIS);\n");
            for (int i = 0; i < exp.args.length; i++) {
                if (exp.args[i] instanceof RefValue) {
                    buf.append("PUSH_R(");
                } else {
                    buf.append("PUSH_P(");
                }
                visitValue(exp.args[i]);
                buf.append(");\n");
            }
            buf.append("/* NOTE: INVOKE_CSA never returns! */\n");
            buf.append("INVOKE_CSA(");
            buf.append(exp.fname);
            // buf.append(", " + (exp.args.length + 1) + ")");
	    ConcreteIntValue iv = (ConcreteIntValue)sizeValue(spec);
	    
            buf.append(", " + (exp.args.length + 1) + ", " 
		       + iv.intValue() + ")");
        }

        public void visit(CurrentPC currentPc) {
            buf.append(CURRENT_PC + " + ");
            visitValue(sizeValue(spec));
        }

        public void visit(SymbolicConstant constant) {
            buf.append(constant.name);
        }

        public void visit(CPAccessExp cpAccess) {
            if (cpAccess.isWide) {
                buf.append("get_constant_wide(current_context, ");
            } else {
                buf.append("get_constant(current_context, ");
            }
            visitValue(cpAccess.value);
            buf.append(')');
        }

        public void visit(LinkSetAccessExp lsetAccess) {
            buf.append("(jref)GET_LINKSET_ENTRY(");
            visitValue(lsetAccess.index);
            buf.append(')');
        }
        static final int WORD_SIZE = 4;
        public void visit(MemExp memExp) {
            if (false // What is this intended to accomplish?
                && currentValue.isReference()
                && memExp.offset instanceof ConcreteIntValue) {
                int offset = ((ConcreteIntValue) memExp.offset).intValue();
                assert(offset % WORD_SIZE == 0);
                buf.append("*((int*)");
                visitValue(memExp.addr);
                buf.append(" + " + (offset / WORD_SIZE) + ')');
            } else {
                buf.append("*(" + getCType(currentValue) + "*)");
                buf.append('(');
                buf.append("(byte*)");
                visitValue(memExp.addr);
                buf.append(" + ");
                visitValue(memExp.offset);
                buf.append(')');
            }
        }

        public void visit(BinExp binexp) {
            String operator = binexp.operator;
            if (operator == "+") { // FIXME operator precedence
                buf.append("(");
            }
            // Question: Why don't we mask the low bits of normal
            // shift instructions?  Is 1 << 32 == 1?
            if ((operator == "%")
                && ((binexp.lhs instanceof FloatValue)
                    || (binexp.lhs instanceof DoubleValue))) {
                buf.append("fmod(");
                visitValue(binexp.lhs);
                buf.append(", ");
                visitValue(binexp.rhs);
                buf.append(")");
            } else {
                visitValue(binexp.lhs);
                buf.append(' ' + binexp.operator + ' ');
                visitValue(binexp.rhs);
            }
            if (operator == "+") { // FIXME operator precedence
                buf.append(")");
            }

        }

        public void visit(UnaryExp unaryExp) {
	    buf.append("(");
            buf.append(unaryExp.operator + ' ');
            visitValue(unaryExp.arg);
	    buf.append(")");
        }

        public void visit(ShiftMaskExp exp) {
            buf.append(
                (exp.sizeType instanceof WideValue)
                    ? "MASK_SHIFT_64("
                    : "MASK_SHIFT_32(");
            visitValue(exp.exponent);
            buf.append(')');
        }

        public void visit(IfExp ifExp) {
            buf.append("(");
            visitValue(ifExp.cond);
            buf.append(" ? ");
            visitValue(ifExp.ifTrue);
            buf.append(" : ");
            visitValue(ifExp.ifFalse);
            buf.append(")");
        }
        public void visit(IfCmd ifCmd) {
            buf.append("if (");
            visitValue(ifCmd.cond);
            buf.append("){\n");
            visitValue(ifCmd.ifTrue);
            if (ifCmd.ifFalse != null) {
                buf.append(";\n} else {\n");
                visitValue(ifCmd.ifFalse);
            }
            buf.append(";\n}");
        }

        public void visit(CondExp condExp) {
            visitValue(condExp.lhs);
            buf.append(' ' + condExp.operator + ' ');
            visitValue(condExp.rhs);
        }

        public void visit(ListElementExp listElement) {
            buf.append("htonl("); // hmm, not too general ...
            visitValue(listElement.list);
            buf.append('[');
            visitValue(listElement.index);
            buf.append(']');
            buf.append(')');

        }

        public void visit(AssignmentExp a) {
            visitValue(a.dest);
            buf.append(" = ");
            visitValue(a.src);
        }

        public void visit(BitFieldExp b) {
            buf.append("(");
            visitValue(b.word);
            if (b.shift != 0)
                buf.append(" >> " + b.shift);
            if (b.mask != -1)
                buf.append(" & " + b.mask);
            buf.append(")");
        }

        public void visitValue(Value v) {
            Value sv = save(v);
            try {
                if (v.concreteValue() != null) {
                    buf.append(v.toString());
                    return;
                }
                if (v.source != null) {
                    visitAppropriate(v.source);
                    return;
                }

                for (int j = 0; j < spec.stackIns.length; j++) {
                    Value inval = spec.stackIns[j];
                    if (v == inval) {
                        buf.append(stackInName(j));
                        return;
                    }
                }

                for (int j = 0; j < spec.istreamIns.length; j++) {
                    StreamableValue inval = spec.istreamIns[j];
                    if (v == inval) {
                        buf.append(istreamInName(j));
                        return;
                    } else if (isValueFromSlot(v, (IntValue)inval)) {
                        buf.append(localsAccess(v, (IntValue)inval));
                        return;
                    }
                }

            } finally {
                restore(sv);
            }
        }
    }

    /**
     * Compute textual representation of <code>outval</code> if it can
     * be related to <code>context.stackIns</code> and
     * <code>context.streamStackIns</code>, and return it, otherwise
     * return null.
     **/
    protected String boundName(Value outval) {
        str.reset();
        str.visitValue(outval);
        String s = str.buf.toString();
        if (s == null || s.equals("")) {
            return null;
        } else {
            return s;
        }
    }

    public void generate() throws IOException {
	super.generate();

	InstructionSet is = InstructionSet.SINGLETON;
	PrintWriter w =
	    new PrintWriter(new FileOutputStream("instruction_names.c"));
	w.println("#include \"instruction_names.h\"");
	w.println();
	w.println("/* GENERATED FILE -- DO NOT EDIT */");
	w.println();
	w.println("const char *instr_name[] = {");
	for (int i = 0; i < is.set.length; i++) {
	    w.print("  \"");
	    w.print(is.set[i] == null
		    ? "UNUSED/RESERVED"
		    : is.set[i].getName());
	    w.println("\",");
	}
	w.println("};");
	w.close();

	w = new PrintWriter(new FileOutputStream("instruction_dispatch.gen"));
	w.println("static void *instruction_dispatch[] = {");
	for (int i = 0; i < is.set.length; i++) {
	    w.print("  &&INSTR_");
	    w.print(is.set[i] != null && shouldProcess(is.set[i])
		    ? is.set[i].getName()
		    : "UNKNOWN");
	    w.println(",");
	}
	w.println("};");
	w.close();
    }

    public static void main(String[] args) throws IOException {
        CommandLine cmdl = new CommandLine( args);
        Writer out = buildOutputStream( cmdl.getArgument(0));
        try {
            new S3Generator(out, InstructionSet.SINGLETON).generate();
        } finally {
            out.flush();
            out.close();
        }
    }
} // End of S3Generator
