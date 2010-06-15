package s3.services.bytecode.analysis;

import ovm.core.repository.Descriptor;
import ovm.core.repository.RepositoryMember;
import ovm.services.bytecode.Instruction;
import ovm.services.bytecode.InstructionBuffer;
import ovm.services.bytecode.InstructionVisitor;
import ovm.services.bytecode.SpecificationIR.Value;
import ovm.services.bytecode.SpecificationIR.ValueList;
import ovm.services.bytecode.SpecificationIR.WideValue;
import ovm.services.bytecode.analysis.AbstractExecutionError;
import ovm.services.bytecode.analysis.AbstractValue;
import ovm.services.bytecode.analysis.AbstractValueError;
import ovm.services.bytecode.analysis.Frame;
import ovm.services.bytecode.MethodInformation;

/**
 * AbstractInterpreter for OVM. This class implements abstract execution.<p>
 *
 * An analysis based on an abstract interpreter is characterized
 * by the following properties:
 * <ul>
 *  <li>the abstract domain, that is, what is abstracted. The
 *      AbstractValue and the Frame interfaces provides a basic
 *      abstraction. A different abstraction 
 *      can be specified by giving the AbstractInterpreter the
 *      appropriate Frame.
 *
 *      Potentially visit() methods of the AbstractInterpreter must
 *      be overriden or provided to ensure proper handling of the new 
 *      abstraction.
 *  </li>
 *  <li>the execution strategy, in particular when to terminate and
 *      additional visitors/analysis operations to perform. 
 *      See FixpointIterator for a possible
 *      implementation.</li>
 * </ul>
 *
 * @author Christian Grothoff
 **/
public abstract class S3AbstractInterpreter
    extends InstructionVisitor {

    /**
     * The current PC in the analysis.
     **/
    protected int pc_;

    /**
     * The current frame.
     **/
    protected Frame frame_;

    /**
     * Create a new AbstractInterpreter
     * @param me the method to interpret
     **/
    public S3AbstractInterpreter(RepositoryMember.Method me) {
	super(InstructionBuffer.wrap(me));
    }

    /**
     * Create a new AbstractInterpreter
     * @param ib the code to interpret
     **/
    public S3AbstractInterpreter(InstructionBuffer ib) {
	super(ib);
    }

    public MethodInformation getPosition() {
	return buf;
    }

    public int getPC() {
        return pc_;
    }

    public Frame getFrame() {
        return frame_;
    }

    public void setFrameAndPC(Frame frame, int pc) {
        this.frame_ = frame;
        this.pc_ = pc;
    }


    /* ******************* Abstract Machine implementation ************ */

    /* *********** the input/output registers ************* */

    /**
     * Four input registers to store input operand to a JVM instruction.
     **/
    protected AbstractValue[] inputRegister = new AbstractValue[4];
    /**
     * Six output register to store results of an instruction's execution.
     **/
    protected AbstractValue[] outputRegister = new AbstractValue[6];

    /* ************** argument registers ****************** */

    /**
     * The variable size method argument register array.
     **/
    private int argumentRegisterCount;
    /**
     * The arguments.
     **/
    protected AbstractValue[] argumentRegisters = new AbstractValue[10];

    public final AbstractValue getInputRegister1() {
        return inputRegister[0];
    }
    public final void setInputRegister1(AbstractValue val) {
        inputRegister[0] = val;
    }
    public final AbstractValue getInputRegister2() {
        return inputRegister[1];
    }
    public final void setInputRegister2(AbstractValue val) {
        inputRegister[1] = val;
    }
    public final AbstractValue getInputRegister3() {
        return inputRegister[2];
    }
    public final void setInputRegister3(AbstractValue val) {
        inputRegister[2] = val;
    }
    public final AbstractValue getInputRegister4() {
        return inputRegister[3];
    }
    public final void setInputRegister4(AbstractValue val) {
        inputRegister[3] = val;
    }
    public final AbstractValue getOutputRegister1() {
        return outputRegister[0];
    }
    public final void setOutputRegister1(AbstractValue val) {
        outputRegister[0] = val;
    }
    public final AbstractValue getOutputRegister2() {
        return outputRegister[1];
    }
    public final void setOutputRegister2(AbstractValue val) {
        outputRegister[1] = val;
    }
    public final AbstractValue getOutputRegister3() {
        return outputRegister[2];
    }
    public final void setOutputRegister3(AbstractValue val) {
        outputRegister[2] = val;
    }
    public final AbstractValue getOutputRegister4() {
        return outputRegister[3];
    }
    public final void setOutputRegister4(AbstractValue val) {
        outputRegister[3] = val;
    }
    public final AbstractValue getOutputRegister5() {
        return outputRegister[4];
    }
    public final void setOutputRegister5(AbstractValue val) {
        outputRegister[4] = val;
    }
    public final AbstractValue getOutputRegister6() {
        return outputRegister[5];
    }
    public final void setOutputRegister6(AbstractValue val) {
        outputRegister[5] = val;
    }

    public final AbstractValue getArgumentRegisterAt(int i) {
        return argumentRegisters[i];
    }
    public final void setArgumentRegisterAt(int i, AbstractValue val) {
        argumentRegisters[i] = val;
    }
    public final int getArgumentRegisterCount() {
        return argumentRegisterCount;
    }
    public final void setArgumentRegisterCount(int i) {
        if (i >= argumentRegisters.length)
            argumentRegisters = new AbstractValue[i];
        argumentRegisterCount = i;
    }

    /* **************** and now: visit methods! ************** */

    protected void popInputs(Instruction i) {
        int ridx = 0;

        Value[] vals = i.stackIns;
        for (int vidx = 0; vidx < vals.length; vidx++, ridx++) {
            if (vals[vidx] instanceof ValueList) {
                throw new Error("instruction has complex inputs");
            } else if (vals[vidx] instanceof WideValue) {
                inputRegister[ridx] = frame_.popWide();
                vidx++;
            } else {
                inputRegister[ridx] = frame_.pop();
            }
        }
    }

    /**
     * When processing outputs, what AbstractValue does
     * the given input value correspond to?
     */
    protected abstract AbstractValue convertSpecification(Value v);

    protected void pushOutputs(Instruction i) {
        int ridx = 0;
        Value[] vals = i.stackOuts;

        // FIXME we are processing pushes backwards.  This code would fail if
        // anything other than a StackManipulation actually returned multiple
        // values.
        for (int vidx=0; vidx<vals.length; vidx++, ridx++) {
            AbstractValue desc 
		= convertSpecification(vals[vidx]);
            if (desc == null)
                throw new Error(i + " lacks type information");
            outputRegister[ridx] = desc;
            if (desc.isWidePrimitive()) {
                frame_.pushWide(desc);
                vidx++;
            } else {
                frame_.push(desc);
            }
        }
    }

    // This method properly handles all instructions that take a fixed
    // number of inputs, and push primitive types.  It also handles
    // aconst_null.
    public void visit(Instruction i) {
        popInputs(i);
        pushOutputs(i);
    }

    public void visit(Instruction.PUTFIELD i) {
        //pop the value to be assigned
        Descriptor.Field desc = i.getSelector(buf, cp).getDescriptor();
        if (desc.isWidePrimitive())
            inputRegister[0] = frame_.popWide();
        else
            inputRegister[0] = frame_.pop();
        //pop the object reference
        inputRegister[1] = frame_.pop();
    }

    public void visit(Instruction.PUTSTATIC i) {
        //pop the value to be assigned
        Descriptor.Field desc = i.getSelector(buf, cp).getDescriptor();
        if (desc.isWidePrimitive())
            inputRegister[0] = frame_.popWide();
        else
            inputRegister[0] = frame_.pop();
    }
    
    /* subclasses should redefine these methods; but
       thanks to the runabout, they may not have to 
       redefine all of these methods exactly (as long
       as they are covered).  Thus we do NOT declare
       them explictly abstract. */
    /*
      public abstract void visit(Instruction.GETFIELD i);
      public abstract void visit(Instruction.GETSTATIC i);
      public abstract void visit(Instruction.INVOKESTATIC i);
      public abstract void visit(Instruction.INVOKEVIRTUAL i);  
      public abstract void visit(Instruction.INVOKEINTERFACE i);
      public abstract void visit(Instruction.INVOKESPECIAL i);
      public abstract void visit(Instruction.NEW i);
      public abstract void visit(Instruction.NEWARRAY i);
      public abstract void visit(Instruction.ANEWARRAY i);
      public abstract void visit(Instruction.MULTIANEWARRAY i);
      public abstract void visit(Instruction.CHECKCAST i);
      public abstract void visit(Instruction.LDC i);
      public abstract void visit(Instruction.LDC2 i);
      public abstract void visit(Instruction.LDC2_W i);
      public abstract void visit(Instruction.JSR i);
      public abstract void visit(Instruction.JSR_W i);
    */

    public void visit(Instruction.AALOAD i) {
        inputRegister[0] = frame_.pop();
        inputRegister[1] = frame_.pop();
        try {
            AbstractValue.Array arr =
                inputRegister[1].getReference().getArray();
            outputRegister[0] = arr.getComponentType();
        } catch (AbstractValueError ave) {
            if (inputRegister[1].getReference().isNull()) {
                outputRegister[0] = inputRegister[1];
                /* null[i] = invalid, but we can continue the simulation assuming it is null */
            } else {
                throw new AbstractExecutionError(
					    "Array or null expected, got: " + inputRegister[1]);
            }
        }
        frame_.push(outputRegister[0]);
    }

    public void visit(Instruction.LocalWrite i) {
        inputRegister[0] =
            (i.stackIns.length > 1 ? frame_.popWide() : frame_.pop());
        outputRegister[0] = inputRegister[0];
        frame_.store(i.getLocalVariableOffset(buf), outputRegister[0]);
    }
    public void visit(Instruction.LocalRead i) {
        inputRegister[0] = frame_.load(i.getLocalVariableOffset(buf));
        outputRegister[0] = inputRegister[0];
        if (i.stackOuts.length > 1)
            frame_.pushWide(outputRegister[0]);
        else
            frame_.push(outputRegister[0]);
    }

    public void visit(Instruction.RET i) {
        inputRegister[0] = frame_.load(i.getLocalVariableOffset(buf));
    }

    public void visit(Instruction.FlowEnd i) {
        // FIXME see Instruction.ReturnValue, S3Generator
        popInputs(i);
    }

    // working around a javac method resolution problem triggered in SpecInstantiation
    public void visit(Instruction.WIDE i) {
	super.visit(i);
    }

    public void visit(Instruction.StackManipulation i) {
        popInputs(i);
        Value[] in = i.stackIns;
        Value[] out = i.stackOuts;
        outer : for (int oidx = out.length - 1; oidx >= 0; oidx--) {
            for (int iidx = 0; iidx < in.length; iidx++) {
                if (out[oidx] == in[iidx]) {
                    outputRegister[oidx] = inputRegister[iidx];
                    frame_.push(outputRegister[oidx]);
                    continue outer;
                }
            }
            throw new Error(i + ": output " + oidx + " not found in inputs");
        }
    }

} // end of S3AbstractInterpreter
