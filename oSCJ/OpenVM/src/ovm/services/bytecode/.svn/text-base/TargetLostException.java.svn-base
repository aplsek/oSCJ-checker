package ovm.services.bytecode;

/**
 * Thrown if an InstructionHandle with targeters is deleted.
 * The following code shows a convention to handle this Exception. 
 * <pre>
     try {
        il.delete(start_ih, end_ih);
     } catch(TargetLostException e) {
       InstructionHandle[] targets = e.getTargets();
         for(int i=0; i < targets.length; i++) {
           InstructionTargeter[] targeters = targets[i].getTargeters();
     
           for(int j=0; j < targeters.length; j++)
             targeters[j].updateTarget(targets[i], new_target);
       }
     }
   </pre>     
 * @author yamauchi
 */
public class TargetLostException extends Exception {

    private InstructionHandle[] targets;
    TargetLostException(InstructionHandle[] targets) {
        this.targets = targets;
    }
    public InstructionHandle[] getTargets() {
        return targets;
    }
}
