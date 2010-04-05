/**
 * @file ovm/services/bytecode/verifier/AbstractMachine.java
 **/  
package ovm.services.bytecode.analysis;

/**
 * The verification framework provides an inteface to bytecode analysis and
 * optimization services.  These services operate by abstractly executing
 * Java Virtual Machine instruction on an AbstractMachine consisting of a
 * current program counter (thisPC), a set of possible next program
 * counters (nextPC), four input registers (IR1 -- IR4), six output
 * registers (OR1 -- OR6), a variable length method argument register
 * array, along with methods for structured access to the code fragment
 * being executed.
 *
 * @author Chrislain Razafimahefa
 * @author Michel Pawlak
 * @author Jan Vitek
 * @author Christian Grothoff
 **/
public interface AbstractMachine {

    /**
     * Interface for the control flow manipulation.
     * @author Christian Grothoff
     **/
    public interface ControlFlow {
	public int getNextPCAt(int i);
	public int getNextPCCount();
	public void setNextPCAt(int i , int PC);
	public void setNextPCCount(int i);
    } // end of AbstractMachine.ControlFlow

    public AbstractValue getArgumentRegisterAt(int i);
    public int getArgumentRegisterCount();
    public AbstractValue getInputRegister1();
    public AbstractValue getInputRegister2();
    public AbstractValue getInputRegister3();
    public AbstractValue getInputRegister4();
    public AbstractValue getOutputRegister1();
    public AbstractValue getOutputRegister2();
    public AbstractValue getOutputRegister3();
    public AbstractValue getOutputRegister4();
    public AbstractValue getOutputRegister5();
    public AbstractValue getOutputRegister6();

    public void setArgumentRegisterAt(int i , AbstractValue val);
    public void setArgumentRegisterCount(int i);
    public void setInputRegister1(AbstractValue val);
    public void setInputRegister2(AbstractValue val);
    public void setInputRegister3(AbstractValue val);
    public void setInputRegister4(AbstractValue val);
    public void setOutputRegister1(AbstractValue val);
    public void setOutputRegister2(AbstractValue val);
    public void setOutputRegister3(AbstractValue val);
    public void setOutputRegister4(AbstractValue val);
    public void setOutputRegister5(AbstractValue val);
    public void setOutputRegister6(AbstractValue val);

} // End of AbstractMachine






