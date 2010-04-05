package ovm.services.bytecode;

/**
 * An interface to represent some entity that points to an InstructionHandle
 * @author yamauchi
 */
public interface InstructionTargeter {
    boolean containsTarget(InstructionHandle ih); 
    void updateTarget(InstructionHandle old_ih,  InstructionHandle new_ih); 
}
